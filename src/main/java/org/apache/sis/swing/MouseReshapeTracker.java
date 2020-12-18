/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.swing;

import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Component;
import javax.swing.JSpinner;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.JFormattedTextField;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.event.MouseInputAdapter;

import java.util.Date;
import java.util.Arrays;
import java.text.Format;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.Classes;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;


/**
 * Controls the position and size of a rectangle which the user can move
 * with their mouse. For example, this class can be used as follows:
 *
 * {@preformat java
 *     public class MyClass extends JPanel {
 *         private final MouseReshapeTracker slider = new MouseReshapeTracker() {
 *             protected void clipChangeRequested(double xmin, double xmax, double ymin, double ymax) {
 *                 // Indicates what must be done if the user tries to move the
 *                 // rectangle outside the permitted limits.
 *                 // This method is optional.
 *             }
 *
 *             protected void stateChanged(boolean isAdjusting) {
 *                 // Method automatically invoked each time the user
 *                 // changes the position of the rectangle.
 *                 // Code here what it should do in this case.
 *             }
 *         };
 *
 *         private final AffineTransform transform = AffineTransform.getScaleInstance(10, 10);
 *
 *         public MyClass() {
 *             slider.setFrame(0, 0, 1, 1);
 *             slider.setClip(0, 100, 0, 1);
 *             slider.setTransform(transform);
 *             addMouseMotionListener(slider);
 *             addMouseListener(slider);
 *         }
 *
 *         public void paintComponent(Graphics graphics) {
 *             AffineTransform tr=...
 *             Graphics2D g = (Graphics2D) graphics;
 *             g.transform(transform);
 *             g.setColor(new Color(128, 64, 92, 64));
 *             g.fill(slider);
 *         }
 *     }
 * }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
class MouseReshapeTracker extends MouseInputAdapter implements Shape {
    /**
     * Minimum width the rectangle should have, in pixels.
     */
    private static final int MIN_WIDTH = 12;

    /**
     * Minimum height the rectangle should have, in pixels.
     */
    private static final int MIN_HEIGHT = 12;

    /**
     * If user moves the mouse at a position less than {@code RESIZE_POS} from shape border,
     * then we assume the user wants to resize rather than to move the rectangle.
     * This distance is measured in pixels from one of the rectangle's edges.
     */
    private static final int RESIZE_POS = 4;

    /**
     * Minimum value of the <code>(clipped rectangle size)/(full rectangle size)</code> ratio.
     * This minimum value will only be taken into account when the user modifies the rectangle's
     * position using the values entered in the fields. This number must be greater than or equal to 1.
     */
    private static final double MINSIZE_RATIO = 1.25;

    /**
     * Minimum <var>x</var> coordinate permitted for the rectangle.
     * The default value is {@link java.lang.Double#NEGATIVE_INFINITY}.
     */
    private double xmin = Double.NEGATIVE_INFINITY;

    /**
     * Minimum <var>y</var> coordinate permitted for the rectangle.
     * The default value is {@link java.lang.Double#NEGATIVE_INFINITY}.
     */
    private double ymin = Double.NEGATIVE_INFINITY;

    /**
     * Maximum <var>x</var> coordinate permitted for the rectangle.
     * The default value is {@link java.lang.Double#POSITIVE_INFINITY}.
     */
    private double xmax = Double.POSITIVE_INFINITY;

    /**
     * Maximum <var>y</var> coordinate permitted for the rectangle.
     * The default value is {@link java.lang.Double#POSITIVE_INFINITY}.
     */
    private double ymax = Double.POSITIVE_INFINITY;

    /**
     * The rectangle to control.  The coordinates of this rectangle must be logical coordinates
     * (for example, coordinates in metres), and not screen pixel coordinates.
     * An empty rectangle means that no region is currently selected.
     */
    private final RectangularShape logicalShape;

    /**
     * Rectangle to be drawn in the component. This rectangle can be different to {@link #logicalShape}
     * and the latter is so small that it is preferable to draw it a little bit bigger than the user has requested.
     * In this case, {@code drawnShape} will serve as a temporary rectangle with extended coordinates.
     *
     * <p><b>Note:</b> this rectangle should be read only, except in the case
     * of {@link #update} which is the only method permitted to update it.</p>
     */
    private transient RectangularShape drawnShape;

    /**
     * Affine transform which changes logical coordinates into pixel coordinates.
     * It is guaranteed that no method except {@link #setTransform} will modify this transformation.
     */
    private final AffineTransform transform = new AffineTransform();

    /**
     * Last <em>relative</em> mouse coordinates. This information is expressed in logical
     * coordinates (according to the {@link #transform} inverse affine transform).
     * The coordinates are relative to (<var>x</var>,<var>y</var>) corner of the rectangle.
     */
    private transient double mouseDX, mouseDY;

    /**
     * {@code x}, {@code y}, {@code width} and {@code height} coordinates of a box which completely
     * encloses {@link #drawnShape}. These coordinates must be expressed in <strong>pixels</strong>.
     * If need be, the affine transform {@link #transform} can be used to change pixel coordinates
     * into logical coordinates and vice versa.
     */
    private transient int x, y, width, height;

    /**
     * Indicates whether the mouse pointer is over the rectangle.
     */
    private transient boolean mouseOverRect;

    /**
     * Point used internally by certain calculations in order to avoid
     * the frequent creation of several temporary {@link Point2D} objects.
     */
    private final transient Point2D.Double tmp = new Point2D.Double();

    /**
     * Indicates if the user is currently dragging the rectangle.
     * For this field to become {@code true}, the mouse must have
     * been over the rectangle as the user pressed the mouse button.
     */
    private transient boolean isDragging;

    /**
     * Indicates which edges the user is currently adjusting with the mouse.
     * This field is often identical to {@link #adjustingSides}.
     * However, unlike {@link #adjustingSides}, it designates an edge of the shape {@link #logicalShape}
     * and not an edge of the shape in pixels appearing on the screen. It is different, for example,
     * if the affine transform {@link #transform} contains a 90° rotation.
     */
    private transient int adjustingLogicalSides;

    /**
     * Indicates which edges the user is currently adjusting with the mouse.
     * Permitted values are binary combinations of {@link #NORTH}, {@link #SOUTH}, {@link #EAST} and {@link #WEST}.
     */
    private transient int adjustingSides;

    /**
     * Indicates which edges are allowed to be adjusted.
     * Permitted values are binary combinations of {@link #NORTH}, {@link #SOUTH}, {@link #EAST} and {@link #WEST}.
     */
    private int adjustableSides;

    /**
     * Indicates if the geometric shape can be moved.
     */
    private boolean moveable = true;

    /**
     * When the position of the left or right-hand edge of the rectangle is manually edited,
     * this indicates whether the position of the opposite edge should be automatically adjusted.
     * The default value is {@code false}.
     */
    private boolean synchronizeX;

    /**
     * When the position of the top or bottom edge of the rectangle is manually edited,
     * this indicates whether the position of the opposite edge should be automatically adjusted.
     * The default value is {@code false}.
     */
    private boolean synchronizeY;

    /** Bit representing north. */ private static final int NORTH = 1;
    /** Bit representing south. */ private static final int SOUTH = 2;
    /** Bit representing east.  */ private static final int EAST  = 4;
    /** Bit representing west.  */ private static final int WEST  = 8;

    /**
     * Cursor codes corresponding to a given {@link adjustingSides} value.
     */
    private static final int[] CURSORS = new int[] {
        Cursor.     MOVE_CURSOR,     // 0000 =       |      |       |
        Cursor. N_RESIZE_CURSOR,     // 0001 =       |      |       | NORTH
        Cursor. S_RESIZE_CURSOR,     // 0010 =       |      | SOUTH |
        Cursor.  DEFAULT_CURSOR,     // 0011 =       |      | SOUTH | NORTH
        Cursor. E_RESIZE_CURSOR,     // 0100 =       | EAST |       |
        Cursor.NE_RESIZE_CURSOR,     // 0101 =       | EAST |       | NORTH
        Cursor.SE_RESIZE_CURSOR,     // 0110 =       | EAST | SOUTH |
        Cursor.  DEFAULT_CURSOR,     // 0111 =       | EAST | SOUTH | NORTH
        Cursor. W_RESIZE_CURSOR,     // 1000 =  WEST |      |       |
        Cursor.NW_RESIZE_CURSOR,     // 1001 =  WEST |      |       | NORTH
        Cursor.SW_RESIZE_CURSOR      // 1010 =  WEST |      | SOUTH |
    };

    /**
     * Lookup table which converts <i>Swing</i> constants into combinations of {@link #NORTH},
     * {@link #SOUTH}, {@link #EAST} and {@link #WEST} constants. We cannot use <i>Swing</i>
     * constants directly because, unfortunately, they do not correspond to the binary
     * combinations of the four cardinal corners.
     */
    private static final int[] SWING_TO_CUSTOM = new int[] {
        SwingConstants.NORTH,      NORTH,
        SwingConstants.SOUTH,      SOUTH,
        SwingConstants.EAST,       EAST,
        SwingConstants.WEST,       WEST,
        SwingConstants.NORTH_EAST, NORTH | EAST,
        SwingConstants.SOUTH_EAST, SOUTH | EAST,
        SwingConstants.NORTH_WEST, NORTH | WEST,
        SwingConstants.SOUTH_WEST, SOUTH | WEST
    };

    /**
     * List of text fields which represent the coordinates of the rectangle's edges.
     */
    private Control[] editors;

    /**
     * Constructs an object capable of moving and resizing a rectangular shape through mouse movements.
     * The rectangle will be positioned, by default at the coordinates (0,0).
     * Its width and height will be null.
     */
    public MouseReshapeTracker() {
        this(new Rectangle2D.Double());
    }

    /**
     * Constructs an object capable of moving and resizing a rectangular shape through mouse movements.
     * The given shape does not have to be a rectangle. It could, for example, be a circle.
     * The coordinates of this shape will be the initial coordinates of the visor.
     * They are logical coordinates and not pixel coordinates.
     *
     * <p>Note that the constructor retains a direct reference to this shape, without creating a clone.
     * As a consequence, any modification carried out on the geometric shape will have repercussions
     * for this {@code MouseReshapeTracker} object and vice versa.</p>
     *
     * @param  shape  rectangular geometric shape to be resized by mouse movements.
     */
    public MouseReshapeTracker(final RectangularShape shape) {
        this.logicalShape = shape;
        this.drawnShape   = shape;
        update();
    }

    /**
     * Invoked after reading this object in order to finish the construction of certain fields.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        drawnShape = logicalShape;
        update();
    }

    /**
     * Updates the internal fields of this object. The adjusted fields will be:
     *
     * <ul>
     *   <li>{@link #drawnShape} for the rectangle to be drawn.</li>
     *   <li>{@link #x}, {@link #y}, {@link #width} and {@link #height}
     *       for the pixel coordinates of {@link #drawnShape}.</li>
     * </ul>
     */
    private void update() {
        /*
         * Takes into account cases where the affine transform
         * contains a rotation of 90° or any other.
         */
        adjustingLogicalSides = inverseTransform(adjustingSides);
        /*
         * Obtains the geometric shape to draw. Normally it will be a {@link #logicalShape},
         * except if the latter is so small that we have considered it preferable to create
         * a temporary shape which will be slightly bigger.
         */
        tmp.x = logicalShape.getWidth();
        tmp.y = logicalShape.getHeight();
        transform.deltaTransform(tmp, tmp);
        if (Math.abs(tmp.x) < MIN_WIDTH || Math.abs(tmp.y) < MIN_HEIGHT) {
            if (Math.abs(tmp.x) < MIN_WIDTH ) tmp.x = (tmp.x < 0) ? -MIN_WIDTH  : MIN_WIDTH;
            if (Math.abs(tmp.y) < MIN_HEIGHT) tmp.y = (tmp.y < 0) ? -MIN_HEIGHT : MIN_HEIGHT;
            try {
                AffineTransforms2D.inverseDeltaTransform(transform, tmp, tmp);
                double x = logicalShape.getX();
                double y = logicalShape.getY();
                if ((adjustingLogicalSides & WEST) != 0) {
                    x += logicalShape.getWidth() - tmp.x;
                }
                if ((adjustingLogicalSides & NORTH) != 0) {
                    y += logicalShape.getHeight() - tmp.y;
                }
                if (drawnShape == logicalShape) {
                    drawnShape = (RectangularShape) logicalShape.clone();
                }
                drawnShape.setFrame(x, y, tmp.x, tmp.y);
            } catch (NoninvertibleTransformException exception) {
                unexpectedException("update", exception);
                drawnShape = logicalShape;
            }
        } else {
            drawnShape = logicalShape;
        }
        /*
         * NOTE: the condition 'drawnShape==logicalShape' indicates that it has not been necessary
         *       to modify the shape. The method `mouseDragged` will use this information.
         *
         * Now retains the pixel coordinates of the new position of the rectangle.
         */
        double xmin = Double.POSITIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            tmp.x = (i&1) == 0 ? drawnShape.getMinX() : drawnShape.getMaxX();
            tmp.y = (i&2) == 0 ? drawnShape.getMinY() : drawnShape.getMaxY();
            transform.transform(tmp, tmp);
            if (tmp.x < xmin) xmin = tmp.x;
            if (tmp.x > xmax) xmax = tmp.x;
            if (tmp.y < ymin) ymin = tmp.y;
            if (tmp.y > ymax) ymax = tmp.y;
        }
        x      = (int) Math.floor(xmin)      -1;
        y      = (int) Math.floor(ymin)      -1;
        width  = (int) Math.ceil (xmax-xmin) +2;
        height = (int) Math.ceil (ymax-ymin) +2;
    }

    /**
     * Returns the transform of {@code adjusting}.
     *
     * @param  adjusting  flags to transform (generally {@link #adjustingSides}).
     */
    private int inverseTransform(int adjusting) {
        switch (adjusting & (WEST | EAST)) {
            case WEST: tmp.x=-1; break;
            case EAST: tmp.x=+1; break;
            default  : tmp.x= 0; break;
        }
        switch (adjusting & (NORTH | SOUTH)) {
            case NORTH: tmp.y=-1; break;
            case SOUTH: tmp.y=+1; break;
            default   : tmp.y= 0; break;
        }
        try {
            AffineTransforms2D.inverseDeltaTransform(transform, tmp, tmp);
            final double normalize = 0.25 * Math.hypot(tmp.x, tmp.y);
            tmp.x /= normalize;
            tmp.y /= normalize;
            adjusting = 0;
            switch ((int) Math.signum(Math.rint(tmp.x))) {
                case -1: adjusting |= WEST; break;
                case +1: adjusting |= EAST; break;
            }
            switch ((int) Math.signum(Math.rint(tmp.y))) {
                case -1: adjusting |= NORTH; break;
                case +1: adjusting |= SOUTH; break;
            }
            return adjusting;
        } catch (NoninvertibleTransformException exception) {
            unexpectedException("inverseTransform", exception);
            return adjusting;
        }
    }

    /**
     * Declares the affine transform which will transform the logical coordinates into pixel coordinates.
     * This is the affine transform specified in {@link java.awt.Graphics2D#transform} at the moment that
     * {@code this} is drawn. The information contained in this affine transform is necessary for several
     * of this class's methods to work. It is the programmer's responsibility to ensure that this information
     * is always up-to-date.  By default, {@code MouseReshapeTracker} assumes an identity transform.
     *
     * @param  newValue  the transform from logical to pixel coordinates.
     */
    public void setTransform(final AffineTransform newValue) {
        if (!transform.equals(newValue)) {
            fireStateWillChange();
            transform.setTransform(newValue);
            update();
            fireStateChanged();
        }
    }

    /**
     * Returns the position and the size of the rectangular shape. These bounds can be slightly
     * bigger than those returned by {@link #getFrame} since {@code getBounds2D()} returns the
     * bounds of the rectangle visible on screen, which may have a minimal size.
     *
     * @return the position and size of the rectangular shape, in logical units.
     */
    @Override
    public Rectangle getBounds() {
        return drawnShape.getBounds();
    }

    /**
     * Returns the position and the size of the rectangular shape. These bounds can be slightly
     * bigger than those returned by {@link #getFrame} since {@code getBounds2D()} returns the
     * bounds of the rectangle visible on screen, which may have a minimal size.
     *
     * @return the position and size of the rectangular shape, in logical units.
     */
    @Override
    public Rectangle2D getBounds2D() {
        return drawnShape.getBounds2D();
    }

    /**
     * Returns the position and the size of the rectangular shape.
     * This information is expressed in logical coordinates.
     *
     * @return the position and size of the rectangular shape, in logical units.
     *
     * @see #getCenterX()
     * @see #getCenterY()
     * @see #getMinX()
     * @see #getMaxX()
     * @see #getMinY()
     * @see #getMaxY()
     */
    public Rectangle2D getFrame() {
        return logicalShape.getFrame();
    }

    /**
     * Defines a new position and bounds for the rectangular shape. The coordinates passed to this
     * method should be logical coordinates rather than pixel coordinates. If the range of values
     * covered by the rectangular shape is limited by a call to {@link #setClip}, then the shape
     * will be moved and resized as needed to fit into the permitted region.
     *
     * @param  frame  the new position and size of the rectangular shape.
     * @return {@code true} if the rectangle's coordinates have changed.
     *
     * @see #getFrame()
     */
    public final boolean setFrame(final Rectangle2D frame) {
        return setFrame(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight());
    }

    /**
     * Defines a new position and bounds for the rectangular shape. The coordinates passed to this
     * method should be logical coordinates rather than pixel coordinates. If the range of values
     * covered by the rectangular shape is limited by a call to {@link #setClip}, then the shape
     * will be moved and resized as needed to fit into the permitted region.
     *
     * @param  x       the new horizontal position.
     * @param  y       the new vertical position.
     * @param  width   the new width.
     * @param  height  the new height.
     * @return {@code true} if the rectangle's coordinates have changed.
     *
     * @see #setX(double, double)
     * @see #setY(double, double)
     */
    public boolean setFrame(double x, double y, double width, double height) {
        final double oldX = logicalShape.getX();
        final double oldY = logicalShape.getY();
        final double oldW = logicalShape.getWidth();
        final double oldH = logicalShape.getHeight();
        if (x < xmin) x = xmin;
        if (y < ymin) y = ymin;
        if (x + width > xmax) {
            x = Math.max(xmin, xmax - width);
            width = xmax - x;
        }
        if (y + height > ymax) {
            y = Math.max(ymin, ymax - height);
            height = ymax - y;
        }
        fireStateWillChange();
        logicalShape.setFrame(x, y, width, height);
        if (oldX != logicalShape.getX()     ||
            oldY != logicalShape.getY()     ||
            oldW != logicalShape.getWidth() ||
            oldH != logicalShape.getHeight())
        {
            update();
            fireStateChanged();
            return true;
        }
        return false;
    }

    /**
     * Defines the new range of values covered by the rectangle according to the <var>x</var> axis.
     * The values covered along the <var>y</var> axis will not be changed.
     * The values must be expressed in logical coordinates.
     *
     * @param  min  the new minimal value.
     * @param  max  the new maximal value.
     *
     * @see #getMinX()
     * @see #getMaxX()
     * @see #getCenterX()
     */
    public final void setX(final double min, final double max) {
        setFrame(Math.min(min,max), logicalShape.getY(),
                 Math.abs(max-min), logicalShape.getHeight());
    }

    /**
     * Defines the new range of values covered by the rectangle according to the <var>y</var> axis.
     * The values covered along the <var>x</var> axis will not be changed.
     * The values must be expressed in logical coordinates.
     *
     * @param  min  the new minimal value.
     * @param  max  the new maximal value.
     *
     * @see #getMinY()
     * @see #getMaxY()
     * @see #getCenterY()
     */
    public final void setY(final double min, final double max) {
        setFrame(logicalShape.getX(), Math.min(min, max),
                 logicalShape.getWidth(), Math.abs(max - min));
    }

    /**
     * Returns the minimum <var>x</var> coordinate of the rectangle
     * (the logical coordinate, not the pixel coordinate).
     *
     * @return the current minimal value on the horizontal axis.
     */
    public double getMinX() {
        return logicalShape.getMinX();
    }

    /**
     * Returns the minimum <var>y</var> coordinate of the rectangle
     * (the logical coordinate, not the pixel coordinate).
     *
     * @return the current minimal value on the vertical axis.
     */
    public double getMinY() {
        return logicalShape.getMinY();
    }

    /**
     * Returns the maximum <var>x</var> coordinate of the rectangle
     * (the logical coordinate, not the pixel coordinate).
     *
     * @return the current maximal value on the horizontal axis.
     */
    public double getMaxX() {
        return logicalShape.getMaxX();
    }

    /**
     * Returns the maximum <var>y</var> coordinate of the rectangle
     * (the logical coordinate, not the pixel coordinate).
     *
     * @return the current maximal value on the vertical axis.
     */
    public double getMaxY() {
        return logicalShape.getMaxY();
    }

    /**
     * Returns the width of the rectangle.
     * This width is expressed in logical coordinates, not pixel coordinates.
     *
     * @return the current width.
     */
    public double getWidth() {
        return logicalShape.getWidth();
    }

    /**
     * Returns the height of the rectangle.
     * This height is expressed in logical coordinates, not pixel coordinates.
     *
     * @return the current height.
     */
    public double getHeight() {
        return logicalShape.getHeight();
    }

    /**
     * Returns the <var>x</var> coordinate of the centre of the rectangle
     * (logical coordinate, not pixel coordinate).
     *
     * @return the current median value on the horizontal axis.
     */
    public double getCenterX() {
        return logicalShape.getCenterX();
    }

    /**
     * Returns the <var>y</var> coordinate of the centre of the rectangle
     * (logical coordinate, not pixel coordinate).
     *
     * @return the current median value on the vertical axis.
     */
    public double getCenterY() {
        return logicalShape.getCenterY();
    }

    /**
     * Indicates whether the rectangle is empty.
     * This will be the case if the width and / or height is null.
     *
     * @return {@code true} if the rectangular shape is empty.
     */
    public boolean isEmpty() {
        return logicalShape.isEmpty();
    }

    /**
     * Indicates whether the rectangular shape contains the specified point.
     * This point should be expressed in logical coordinates.
     *
     * @param  point  the point to test for inclusion.
     * @return {@code true} if the given point is included in the rectangular shape.
     */
    @Override
    public boolean contains(final Point2D point) {
        return logicalShape.contains(point);
    }

    /**
     * Indicates whether the rectangular shape contains the specified point.
     * This point should be expressed in logical coordinates.
     *
     * @param  x  the <var>x</var> value of the point to test for inclusion.
     * @param  y  the <var>y</var> value of the point to test for inclusion.
     * @return {@code true} if the given point is included in the rectangular shape.
     */
    @Override
    public boolean contains(final double x, final double y) {
        return logicalShape.contains(x, y);
    }

    /**
     * Indicates whether the rectangular shape contains the specified rectangle.
     * This rectangle should be expressed in logical coordinates. This method may
     * conservatively return {@code false} as permitted by the {@link Shape} specification.
     *
     * @param rect  the rectangle to test for inclusion.
     * @return {@code true} if the given rectangle is included in the rectangular shape.
     */
    @Override
    public boolean contains(final Rectangle2D rect) {
        return logicalShape.contains(rect);
    }

    /**
     * Indicates whether the rectangular shape contains the specified rectangle.
     * This rectangle must be expressed in logical coordinates. This method may
     * conservatively return {@code false} as permitted by the {@link Shape} specification.
     *
     * @param  x       the <var>x</var> location of the rectangle to test for inclusion.
     * @param  y       the <var>y</var> location of the rectangle to test for inclusion.
     * @param  width   the width of the rectangle to test for inclusion.
     * @param  height  the height of the rectangle to test for inclusion.
     * @return {@code true} if the given rectangle is included in the rectangular shape.
     */
    @Override
    public boolean contains(double x, double y, double width, double height) {
        return logicalShape.contains(x, y, width, height);
    }

    /**
     * Indicates whether the rectangular shape intersects the specified rectangle.
     * This rectangle must be expressed in logical coordinates. This method may
     * conservatively return {@code true} as permitted by the {@link Shape} specification.
     *
     * @param  rect  the rectangle to test for intersection.
     * @return {@code true} if the given rectangle intersects the rectangular shape.
     */
    @Override
    public boolean intersects(final Rectangle2D rect) {
        return drawnShape.intersects(rect);
    }

    /**
     * Indicates whether the rectangular shape intersects the specified rectangle.
     * This rectangle must be expressed in logical coordinates. This method may
     * conservatively return {@code true} as permitted by the {@link Shape} specification.
     *
     * @param  x The <var>x</var> location of the rectangle to test for intersection.
     * @param  y The <var>y</var> location of the rectangle to test for intersection.
     * @param  width The width of the rectangle to test for intersection.
     * @param  height The height of the rectangle to test for intersection.
     * @return {@code true} if the given rectangle intersects the rectangular shape.
     */
    @Override
    public boolean intersects(double x, double y, double width, double height) {
        return drawnShape.intersects(x, y, width, height);
    }

    /**
     * Returns a path iterator for the rectangular shape to be drawn.
     *
     * @param  transform  the transform to be applied on coordinates to be returned by the iterator.
     * @return An iterator over the coordinates of the rectangular shape.
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform transform) {
        return drawnShape.getPathIterator(transform);
    }

    /**
     * Returns a path iterator for the rectangular shape to be drawn.
     *
     * @param  transform  the transform to be applied on coordinates to be returned by the iterator.
     * @param  flatness   the flateness factor for converting curves to straight lines.
     * @return An iterator over the coordinates of the rectangular shape.
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform transform, final double flatness) {
        return drawnShape.getPathIterator(transform, flatness);
    }

    /**
     * Returns the bounds between which the rectangular shape can move.
     * These bounds are specified in logical coordinates.
     *
     * @return the current clip area.
     */
    public Rectangle2D getClip() {
        return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    /**
     * Defines the bounds between which the rectangular shape can move. This method manages
     * infinities correctly if the specified rectangle has redefined its {@code getMaxX()}
     * and {@code getMaxY()} methods correctly.
     *
     * @param  rect  the new clip area.
     *
     * @see #setClipMinMax(double, double, double, double)
     */
    public final void setClip(final Rectangle2D rect) {
        setClipMinMax(rect.getMinX(), rect.getMaxX(), rect.getMinY(), rect.getMaxY());
    }

    /**
     * Defines the bounds between which the rectangular shape can move. This method simply calls
     * {@link #setClipMinMax setClipMinMax(...)} with the appropriate parameters.
     * It is defined in order to avoid confusion among programmers used to <i>Java2D</i> conventions.
     * If you want to specify infinite values (in order to widen the visor's bounds to all possible values along
     * certain axes), you <u>must</u> use {@link #setClipMinMax setClipMinMax(...)} rather than {@code setClip(...)}.
     *
     * @param  x       the <var>x</var> location of the clip area.
     * @param  y       the <var>y</var> location of the clip area.
     * @param  width   the width of the clip area.
     * @param  height  the height of the clip area.
     */
    public final void setClip(final double x, final double y, final double width, final double height) {
        setClipMinMax(x, x + width, y, y + height);
    }

    /**
     * Defines the bounds between which the rectangle can move. This method's arguments define the
     * minimum and maximum values that the logical coordinates of the rectangle can take. The values
     * {@link java.lang.Double#NEGATIVE_INFINITY} and {@link java.lang.Double#POSITIVE_INFINITY} are
     * valid for indicating that the visor can extend across all values according to certain axes.
     * The value {@link java.lang.Double#NaN} for a given argument indicates that we want to keep the old value.
     *
     * <p>If the visor doesn't fit completely within the new bounds,
     * it will be moved and resized as needed in order to make it fit.</p>
     */
    public void setClipMinMax(double xmin, double xmax, double ymin, double ymax) {
        if (xmin > xmax) {
            final double tmp = xmin;
            xmin = xmax; xmax = tmp;
        }
        if (ymin > ymax) {
            final double tmp = ymin;
            ymin = ymax; ymax = tmp;
        }
        if (!Double.isNaN(xmin)) this.xmin = xmin;
        if (!Double.isNaN(xmax)) this.xmax = xmax;
        if (!Double.isNaN(ymin)) this.ymin = ymin;
        if (!Double.isNaN(ymax)) this.ymax = ymax;
        setFrame(logicalShape.getX(), logicalShape.getY(), logicalShape.getWidth(), logicalShape.getHeight());
    }

    /**
     * Invoked when a change in the clip is required. This method can be invoked,
     * for example, when the user manually edits the position of the rectangle in a text field, and
     * the new position falls outside the current clip.  This method does <u>not</u> have to accept
     * a clip change. It can do nothing, which is the same as refusing any change. It can also
     * unconditionally accept any change by calling {@link #setClipMinMax}. Finally, it can reach a
     * compromise solution by imposing certain conditions on the changes. The default implementation
     * does nothing, which means that no automatic change in the clip will be authorized.
     */
    protected void clipChangeRequested(double xmin, double xmax, double ymin, double ymax) {
    }

    /**
     * Indicates whether the rectangle can be moved with the mouse.
     * By default, it can be moved but not resized.
     */
    public boolean isMoveable() {
        return moveable;
    }

    /**
     * Specifies whether the rectangle can be moved with the mouse.
     * The value {@code false} indicates that the rectangle cannot be moved, but can still
     * be resized if {@link #setAdjustable} has been invoked with the appropriate parameters.
     */
    public void setMoveable(final boolean moveable) {
        this.moveable = moveable;
    }

    /**
     * Indicates whether the size of a rectangle can be modified using a specified edge.
     * The specified edge must be one of the following constants:
     *
     * <table class="sis">
     *   <tr>
     *     <td>{@link SwingConstants#NORTH_WEST}</td>
     *     <td>{@link SwingConstants#NORTH}</td>
     *     <td>{@link SwingConstants#NORTH_EAST}</td>
     *   </tr><tr>
     *     <td>{@link SwingConstants#WEST}</td>
     *     <td></td>
     *     <td>{@link SwingConstants#EAST}</td>
     *   </tr><tr>
     *     <td>{@link SwingConstants#SOUTH_WEST}</td>
     *     <td>{@link SwingConstants#SOUTH}</td>
     *     <td>{@link SwingConstants#SOUTH_EAST}</td>
     *   </tr>
     * </table>
     *
     * These constants designate the edge which is visible on screen. For example, {@code NORTH} always designates
     * the top edge on the screen. However, this could correspond to another edge of the logical shape {@code this}
     * depending on the affine transform which was specified during the last call to {@link #setTransform(jAffineTransform)}.
     * For example, {@code AffineTransform.getScaleInstance(+1,-1)} has the effect of inverting the <var>y</var> axis so
     * that the <var>y</var><sub>max</sub> values appear to the North rather than the <var>y</var><sub>min</sub> values.
     */
    public boolean isAdjustable(int side) {
        side = convertSwingConstant(side);
        return (adjustableSides & side) == side;
    }

    /**
     * Specifies whether the size of the rectangle can be modified using the specified edge.
     * The specified edge must be one of the following constants:
     *
     * <table class="sis">
     *   <tr>
     *     <td>{@link SwingConstants#NORTH_WEST}</td>
     *     <td>{@link SwingConstants#NORTH}</td>
     *     <td>{@link SwingConstants#NORTH_EAST}</td>
     *   </tr><tr>
     *     <td>{@link SwingConstants#WEST}</td>
     *     <td></td>
     *     <td>{@link SwingConstants#EAST}</td>
     *   </tr><tr>
     *     <td>{@link SwingConstants#SOUTH_WEST}</td>
     *     <td>{@link SwingConstants#SOUTH}</td>
     *     <td>{@link SwingConstants#SOUTH_EAST}</td>
     *   </tr>
     * </table>
     *
     * These constants designate the edge which is visible on screen. For example, {@code NORTH} always designates
     * the top edge on the screen. However, this could correspond to another edge of the logical shape {@code this}
     * depending on the affine transform which was specified during the last call to {@link #setTransform(AffineTransform)}.
     * For example, {@code AffineTransform.getScaleInstance(+1,-1)} has the effect of inverting the <var>y</var> axis so
     * that the <var>y</var><sub>max</sub> values appear to the North rather than the <var>y</var><sub>min</sub> values.
     */
    public void setAdjustable(int side, final boolean adjustable) {
        side = convertSwingConstant(side);
        if (adjustable) {
            adjustableSides |=  side;
        } else {
            adjustableSides &= ~side;
        }
    }

    /**
     * Converts a Swing edge constant to system used by this package. We cannot use <i>Swing</i>
     * constants directly because, unfortunately, they do not correspond to the binary combinations
     * of the four cardinal corners.
     */
    private int convertSwingConstant(final int side) {
        for (int i = 0; i < SWING_TO_CUSTOM.length; i += 2) {
            if (SWING_TO_CUSTOM[i] == side) {
                return SWING_TO_CUSTOM[i + 1];
            }
        }
        throw new IllegalArgumentException(String.valueOf(side));
    }

    /**
     * Invoked during mouse movements. The default implementation checks whether the cursor
     * is inside the rectangle or on one of its edges, and adjusts the mouse pointer icon accordingly.
     */
    @Override
    public void mouseMoved(final MouseEvent event) {
        if (!isDragging) {
            final Component source = event.getComponent();
            if (source != null) {
                int ex = event.getX(); tmp.x = ex;
                int ey = event.getY(); tmp.y = ey;
                final boolean isMouseOver;
                try {
                    isMouseOver = drawnShape.contains(transform.inverseTransform(tmp, tmp));
                } catch (NoninvertibleTransformException exception) {
                    unexpectedException("mouseMoved", exception);
                    return;
                }
                final boolean mouseOverRectChanged = (isMouseOver != mouseOverRect);
                if (isMouseOver) {
                    /*
                     * We do not use "adjustingLogicalSides" because we are working
                     * with pixel coordinates and not logical coordinates.
                     */
                    final int old = adjustingSides;
                    adjustingSides = 0;
                    if (Math.abs(ex -= x)      <= RESIZE_POS) adjustingSides |= WEST;
                    if (Math.abs(ey -= y)      <= RESIZE_POS) adjustingSides |= NORTH;
                    if (Math.abs(ex -  width)  <= RESIZE_POS) adjustingSides |= EAST;
                    if (Math.abs(ey -  height) <= RESIZE_POS) adjustingSides |= SOUTH;
                    adjustingSides &= adjustableSides;
                    if (adjustingSides != old || mouseOverRectChanged) {
                        if (adjustingSides == 0 && !moveable) {
                            source.setCursor(null);
                        } else {
                            adjustingLogicalSides = inverseTransform(adjustingSides);
                            source.setCursor(Cursor.getPredefinedCursor(adjustingSides < CURSORS.length
                                    ? CURSORS[adjustingSides] : Cursor.DEFAULT_CURSOR));
                        }
                    }
                    if (mouseOverRectChanged) {
                        mouseOverRect = isMouseOver;
                    }
                } else if (mouseOverRectChanged) {
                    adjustingSides = 0;
                    source.setCursor(null);
                    mouseOverRect = isMouseOver;
                }
            }
        }
    }

    /**
     * Invoked when the user presses a mouse button anywhere within the component.
     * The default implementation checks if the button was pressed whilst the mouse cursor was within the rectangle.
     * If so, this object will track the mouse drags to move or resize the rectangle.
     */
    @Override
    public void mousePressed(final MouseEvent e) {
        if (!e.isConsumed() && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK)!= 0) {
            if (adjustingSides != 0 || moveable) {
                tmp.x = e.getX();
                tmp.y = e.getY();
                try {
                    if (drawnShape.contains(transform.inverseTransform(tmp, tmp))) {
                        mouseDX = tmp.x - drawnShape.getX();
                        mouseDY = tmp.y - drawnShape.getY();
                        isDragging = true;
                        e.consume();
                    }
                } catch (NoninvertibleTransformException exception) {
                    unexpectedException("mousePressed", exception);
                }
            }
        }
    }

    /**
     * Invoked during mouse drags. Default implementation moves the rectangle and notifies
     * the component where the event come from that it needs repaint.
     */
    @Override
    public void mouseDragged(final MouseEvent e) {
        if (isDragging) {
            final int adjustingLogicalSides = this.adjustingLogicalSides;
            final Component source = e.getComponent();
            if (source != null) try {
                tmp.x = e.getX();
                tmp.y = e.getY();
                transform.inverseTransform(tmp, tmp);
                /*
                 * Calculates the (x0,y0) coordinates of the corner of the rectangle. The (mouseDX, mouseDY)
                 * coordinates represent the position of the mouse at the moment the button is pressed and
                 * should not change (except during some adjustments). In determining (mouseDX, mouseDY),
                 * we calculate as if user began to drag the rectangle at the very corner,
                 * though in reality (s)he could have clicked anywhere.
                 */
                double x0 = tmp.x - mouseDX;
                double y0 = tmp.y - mouseDY;
                double dx = drawnShape.getWidth();
                double dy = drawnShape.getHeight();
                final double oldWidth  = dx;
                final double oldHeight = dy;
                /*
                 * Deals with cases where, instead of dragging the rectangle,
                 * the user is in the process of resizing it.
                 */
                switch (adjustingLogicalSides & (EAST | WEST)) {
                    case WEST: {
                        if (x0 < xmin) {
                            x0 = xmin;
                        }
                        dx += drawnShape.getX() - x0;
                        if (!(dx > 0)) {
                            dx = drawnShape.getWidth();
                            x0 = drawnShape.getX();
                        }
                        break;
                    }
                    case EAST: {
                        dx += x0 - (x0 = drawnShape.getX());
                        final double limit = xmax - x0;
                        if (dx > limit) {
                            dx = limit;
                        }
                        if (!(dx > 0)) {
                            dx = drawnShape.getWidth();
                            x0 = drawnShape.getX();
                        }
                        break;
                    }
                }
                switch (adjustingLogicalSides & (NORTH | SOUTH)) {
                    case NORTH: {
                        if (y0 < ymin) {
                            y0 = ymin;
                        }
                        dy += drawnShape.getY() - y0;
                        if (!(dy > 0)) {
                            dy = drawnShape.getHeight();
                            y0 = drawnShape.getY();
                        }
                        break;
                    }
                    case SOUTH: {
                        dy += y0 - (y0 = drawnShape.getY());
                        final double limit = ymax - y0;
                        if (dy > limit) dy = limit;
                        if (!(dy > 0)) {
                            dy = drawnShape.getHeight();
                            y0 = drawnShape.getY();
                        }
                        break;
                    }
                }
                /*
                 * The (x0, y0, dx, dy) coordinates now give the new position and size of the rectangle.
                 * But before making the change, check whether only one edge was being adjusted. If so,
                 * we cancel the changes with respect to the other edge (otherwise the user could move
                 * the rectangle vertically at the same time as adjusting its right or left edge,
                 * which is not convenient).
                 */
                if ((adjustingLogicalSides & (NORTH | SOUTH)) != 0 &&
                    (adjustingLogicalSides & (EAST  |  WEST)) == 0)
                {
                    x0 = drawnShape.getX();
                    dx = drawnShape.getWidth();
                }
                if ((adjustingLogicalSides & (NORTH | SOUTH)) == 0 &&
                    (adjustingLogicalSides & (EAST  |  WEST)) != 0)
                {
                    y0 = drawnShape.getY();
                    dy = drawnShape.getHeight();
                }
                /*
                 * If the user didn't adjusted any side, then make sure that the logical size
                 * is conserved (i.e. discard the "drawing" size if it was different).
                 */
                if (adjustingLogicalSides == 0) {
                    final double old_dx = logicalShape.getWidth();
                    final double old_dy = logicalShape.getHeight();
                    x0 += (dx - old_dx) / 2;
                    y0 += (dy - old_dy) / 2;
                    dx = old_dx;
                    dy = old_dy;
                }
                /*
                 * Modifies the rectangle coordinates and notifies that the component needs redrawing.
                 * Note: `repaint` should be invoked before and after `setFrame` because the coordinates changed.
                 */
                source.repaint(x, y, width, height);
                try {
                    setFrame(x0, y0, dx, dy);
                } catch (RuntimeException exception) {
                    unexpectedException("mouseDragged", exception);
                }
                source.repaint(x, y, width, height);
                /*
                 * Adjustment for special cases.
                 */
                if ((adjustingLogicalSides & EAST) != 0) {
                    mouseDX += (drawnShape.getWidth() - oldWidth);
                }
                if ((adjustingLogicalSides & SOUTH) != 0) {
                    mouseDY += (drawnShape.getHeight() - oldHeight);
                }
            } catch (NoninvertibleTransformException exception) {
                unexpectedException("mouseDragged", exception);
            }
        }
    }

    /**
     * Invoked when the user releases the mouse button. The default implementation invokes
     * {@link #stateChanged(boolean)} with {@code false} argument value, in order to notify
     * subclasses that changes are finished.
     */
    @Override
    public void mouseReleased(final MouseEvent event) {
        if (isDragging && (event.getButton() == MouseEvent.BUTTON1)) {
            isDragging = false;
            final Component source = event.getComponent();
            try {
                tmp.x = event.getX();
                tmp.y = event.getY();
                mouseOverRect = drawnShape.contains(transform.inverseTransform(tmp, tmp));
                if (!mouseOverRect && source != null) source.setCursor(null);
                event.consume();
            } catch (NoninvertibleTransformException exception) {
                unexpectedException("mouseReleased", exception);
            } try {
                // It is essential that `isDragging == false`.
                fireStateChanged();
            } catch (RuntimeException exception) {
                ExceptionMonitor.show(source, exception);
            }
        }
    }

    /**
     * Invoked <strong>before</strong> the position or the size of the visor has changed.
     * A call to {@code stateWillChange} is normally followed by a call to {@link #stateChanged}, <u>except</u>
     * if the expected change didn't ultimately occur. The derived classes can redefine this method to take the
     * necessary actions when a change is on the point of being actioned. They must not, however, call any method
     * which risks modifying the state of this object.  The default implementation does nothing.
     *
     * @param isAdjusting {@code true} if the user is still modifying the position of the visor,
     *        {@code false} if they have released the mouse button.
     */
    protected void stateWillChange(final boolean isAdjusting) {
    }

    /**
     * Invoked <strong>after</strong> the position and size of the visor has changed.
     * The call to {@code stateChanged} must have been preceded by a call to {@link #stateWillChange}.
     * The derived classes can redefine this method to take the necessary actions when a change has just been actioned.
     * They must not, however, call any method which risks modifying the state of this object.
     * The default implementation does nothing.
     *
     * @param isAdjusting {@code true} if the user is still modifying the position of the visor,
     *        {@code false} if they have released the mouse button.
     */
    protected void stateChanged(final boolean isAdjusting) {
    }

    /**
     * Invoked before the position or the size of the visor has changed.
     */
    private void fireStateWillChange() {
        stateWillChange(isDragging);
    }

    /**
     * Invoked after the position or the size of the visor has changed.
     */
    private void fireStateChanged() {
        updateEditors();
        stateChanged(isDragging);
    }

    /**
     * Updates the text in the editors. Each editor added by the method {@link #addEditor} will have its text
     * reformatted. This method can be invoked, for example, after changing the format used by the editors.
     * It is not necessary to call this method each time the mouse moves; it is done automatically.
     */
    public void updateEditors() {
        if (editors != null) {
            for (int i = 0; i < editors.length; i++) {
                editors[i].updateText();
            }
        }
    }

    /**
     * Adds an editor in which the user can explicitly specify the coordinates of one of the edges of the rectangle.
     * Each time the user drags the rectangle, the text appearing in this editor will automatically be updated.
     * If the user explicitly enters a new value in this editor, the position of the rectangle will be adjusted.
     *
     * @param  format  format to use for parsing and formatting the values in the editor.
     * @param  side    edge of the rectangle whose coordinates will be controlled by the editor.
     *                 It should be one of the following constants:
     *
     * <table class="sis">
     *   <tr>
     *     <td>{@link SwingConstants#NORTH_WEST}</td>
     *     <td>{@link SwingConstants#NORTH}</td>
     *     <td>{@link SwingConstants#NORTH_EAST}</td>
     *   </tr><tr>
     *     <td>{@link SwingConstants#WEST}</td>
     *     <td></td>
     *     <td>{@link SwingConstants#EAST}</td>
     *   </tr><tr>
     *     <td>{@link SwingConstants#SOUTH_WEST}</td>
     *     <td>{@link SwingConstants#SOUTH}</td>
     *     <td>{@link SwingConstants#SOUTH_EAST}</td>
     *   </tr></table>
     *
     * @param  toRepaint  component to repaint after a field has been edited, or {@code null} if there isn't one.
     * @return an editor in which the user can specify the position of one of the edges of the geometric shape.
     * @throws IllegalArgumentException if {@code side} isn't one of the recognized codes.
     */
    public synchronized JComponent addEditor(final Format format, final int side, Component toRepaint)
            throws IllegalArgumentException
    {
        final JComponent       component;
        final JFormattedTextField editor;
        if (format instanceof DecimalFormat) {
            final SpinnerNumberModel   model = new SpinnerNumberModel();
            final JSpinner           spinner = new JSpinner(model);
            final JSpinner.NumberEditor sedt = (JSpinner.NumberEditor) spinner.getEditor();
            final DecimalFormat targetFormat = sedt.getFormat();
            final DecimalFormat sourceFormat = (DecimalFormat) format;
            // TODO: Next line would be much more efficient if only we had a
            // NumberEditor.setFormat(NumberFormat) method (See RFE #4520587)
            targetFormat.setDecimalFormatSymbols(sourceFormat.getDecimalFormatSymbols());
            targetFormat.applyPattern(sourceFormat.toPattern());
            editor = sedt.getTextField();
            component = spinner;
        } else if (format instanceof SimpleDateFormat) {
            final SpinnerDateModel        model = new SpinnerDateModel();
            final JSpinner              spinner = new JSpinner(model);
            final JSpinner.DateEditor      sedt = (JSpinner.DateEditor) spinner.getEditor();
            final SimpleDateFormat targetFormat = sedt.getFormat();
            final SimpleDateFormat sourceFormat = (SimpleDateFormat) format;
            // TODO: Next line would be much more efficient if only we had a
            // DateEditor.setFormat(DateFormat) method... (See RFE #4520587)
            targetFormat.setDateFormatSymbols(sourceFormat.getDateFormatSymbols());
            targetFormat.applyPattern(sourceFormat.toPattern());
            editor = sedt.getTextField();
            component = spinner;
        } else {
            component = editor = new JFormattedTextField(format);
        }
        /*
         * "9" is the default width of text fields. These widths are expressed in number of columns.
         * Swing does not appear to measure these widths very accurately; it seems to provide more
         * than requested. For that reason, we specify a narrower width.
         */
        editor.setColumns(5);
        editor.setHorizontalAlignment(JTextField.RIGHT);
        Insets insets = editor.getMargin();
        insets.right += 2;
        editor.setMargin(insets);
        /*
         * Adds the editor to the list of editors to control. Increasing the `editors` array length
         * each time is not a very efficient strategy, but it will do because it is unlikely that we
         * will ever add more than 4 editors.
         */
        final Control control = new Control(editor, (format instanceof DateFormat),
                                            convertSwingConstant(side), toRepaint);
        if (editors == null) {
            editors = new Control[1];
        } else {
            editors = Arrays.copyOf(editors, editors.length + 1);
        }
        editors[editors.length - 1] = control;
        return component;
    }

    /**
     * Removes an editor from the list of component showing coordinates.
     *
     * @param  editor  editor to remove.
     */
    public synchronized void removeEditor(final JComponent editor) {
        if (editors != null) {
            for (int i = 0; i < editors.length; i++) {
                if (editors[i].editor == editor) {
                    editors = ArraysExt.remove(editors, i, 1);
                    /*
                     * In principle, there should be no more objects to remove from the table.
                     * But we let the loop continue anyway, just in case.
                     */
                }
            }
            if (editors.length == 0) {
                editors = null;
            }
        }
    }

    /**
     * When the position of one of the rectangle's edges is edited manually, specifies whether
     * the opposite edge should also be adjusted. By default, the edges are not synchronized.
     *
     * @param  axis {@link SwingConstants#HORIZONTAL} to change the synchronization of the left
     *         and right edges, or {@link SwingConstants#VERTICAL} to change the synchronization
     *         of the top and bottom edges.
     * @param  state {@code true} to synchronize the edges, or {@code false} to desynchronize.
     * @throws IllegalArgumentException if {@code axis} isn't one of the valid codes.
     */
    public void setEditorsSynchronized(final int axis, final boolean state)
            throws IllegalArgumentException
    {
        switch (axis) {
            case SwingConstants.HORIZONTAL: synchronizeX = state; break;
            case SwingConstants.VERTICAL:   synchronizeY = state; break;
            default: throw new IllegalArgumentException();
        }
    }

    /**
     * When the position of one of the rectangle's edges is edited manually, specifies whether
     * the opposite edge should also be adjusted. By default, the edges are not synchronized.
     *
     * @param  axis {@link SwingConstants#HORIZONTAL} to determine the synchronization of the left
     *         and right edges, or {@link SwingConstants#VERTICAL} to determine the synchronization
     *         of the top and bottom edges.
     * @return {@code true} if the specified edges are synchronized, or {@code false} if not
     * @throws IllegalArgumentException if {@code axis} isn't one of the valid codes.
     */
    public boolean isEditorsSynchronized(final int axis) throws IllegalArgumentException {
        switch (axis) {
            case SwingConstants.HORIZONTAL: return synchronizeX;
            case SwingConstants.VERTICAL:   return synchronizeY;
            default: throw new IllegalArgumentException();
        }
    }

    /**
     * Returns a character string representing this object.
     */
    @Override
    public String toString() {
        return Classes.getShortClassName(this) + '[' + Classes.getShortClassName(logicalShape) + ']';
    }

    /**
     * Invoked when an unexpected exception occurs.
     * Current implementation logs the stack trace.
     *
     * @param  methodName  the caller's method name.
     * @param  exception   the exception to log.
     */
    private static void unexpectedException(String methodName, Exception exception) {
        Logging.unexpectedException(null, MouseReshapeTracker.class, methodName, exception);
    }

    /**
     * Synchronizes one of the rectangle's edges with a text field. Each time the visor moves, the text will be
     * updated. If, on the contrary, it is the text which is manually edited, the visor will be repositioned.
     */
    private final class Control implements PropertyChangeListener {
        /**
         * Text field representing the coordinate of one of the visor's edges.
         */
        public final JFormattedTextField editor;

        /**
         * {@code true} if the field {@link #editor} formats dates,
         * or {@code false} if it formats numbers.
         */
        private final boolean isDate;

        /**
         * Side of the rectangle to be controlled. This field designates the edge which is visible on screen.
         * For example, {@code NORTH} always designates the top edge on the screen. However, this could correspond
         * to another edge of the logical shape {@link MouseReshapeTracker} depending on the affine transform that
         * was specified during the last call to {@link MouseReshapeTracker#setTransform}.
         * For example, {@code AffineTransform.getScaleInstance(+1,-1)} has the effect of inverting
         * the <var>y</var> axis so that the <var>y</var><sub>max</sub> values appear to the North
         * rather than the <var>y</var><sub>min</sub> values.
         */
        private final int side;

        /**
         * Component to repaint after the field is edited, or {@code null} if there isn't one.
         */
        private final Component toRepaint;

        /**
         * Constructs an object which will control one of the rectangle's edges.
         *
         * @param editor     field which will contain the coordinate of the rectangle's edge.
         * @param isDate     {@code true} if the field {@link #editor} formats dates, or {@code false} if it formats numbers.
         * @param side       edge of the rectangle to control. This argument designates the edge visible on screen.
         * @param toRepaint  component to repaint after the field has been edited, or {@code null} if there isn't one.
         */
        public Control(final JFormattedTextField editor, final boolean isDate,
                       final int side, final Component toRepaint)
        {
            this.editor    = editor;
            this.isDate    = isDate;
            this.side      = side;
            this.toRepaint = toRepaint;
            updateText(editor);
            editor.addPropertyChangeListener("value", this);
        }

        /**
         * Invoked each time the value in the editor changes.
         */
        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            final Object source = event.getSource();
            if (source instanceof JFormattedTextField) {
                final JFormattedTextField editor = (JFormattedTextField) source;
                final Object value = editor.getValue();
                if (value != null) {
                    final double v = (value instanceof Date)       ?
                                     ((Date) value).getTime()      :
                                     ((Number) value).doubleValue();
                    if (!Double.isNaN(v)) {
                        /*
                         * Obtains the new coordinates of the rectangle, taking into account the coordinates
                         * changed by the user as well as the old coordinates which have not changed.
                         */
                        final int side = inverseTransform(this.side);
                        double Vxmin = (side &  WEST) == 0 ? logicalShape.getMinX() : v;
                        double Vxmax = (side &  EAST) == 0 ? logicalShape.getMaxX() : v;
                        double Vymin = (side & NORTH) == 0 ? logicalShape.getMinY() : v;
                        double Vymax = (side & SOUTH) == 0 ? logicalShape.getMaxY() : v;
                        if (synchronizeX || Vxmin > Vxmax) {
                            final double dx = logicalShape.getWidth();
                            if ((side & WEST) != 0) Vxmax = Vxmin + dx;
                            if ((side & EAST) != 0) Vxmin = Vxmax - dx;
                        }
                        if (synchronizeY || Vymin > Vymax) {
                            final double dy = logicalShape.getHeight();
                            if ((side & NORTH) != 0) Vymax = Vymin + dy;
                            if ((side & SOUTH) != 0) Vymin = Vymax - dy;
                        }
                        /*
                         * Checks whether the new coordinates need a clip adjustment. If so, we ask
                         * the method clipChangeRequested(...) to make the change. That method doesn't
                         * have to accept the change. The rest of the code will be correct even if
                         * the clip hasn't changed - in that case the position of the rectangle will
                         * still be adjusted by setFrame(...).
                         */
                        if (Vxmin < xmin) {
                            final double dx = Math.max(xmax - xmin, MINSIZE_RATIO * (Vxmax - Vxmin));
                            final double margin = Vxmax + dx * ((MINSIZE_RATIO - 1) * 0.5);
                            clipChangeRequested(margin - dx, margin, ymin, ymax);
                        } else if (Vxmax > xmax) {
                            final double dx = Math.max(xmax - xmin, MINSIZE_RATIO * (Vxmax - Vxmin));
                            final double margin = Vxmin-dx * ((MINSIZE_RATIO - 1) * 0.5);
                            clipChangeRequested(margin, margin + dx, ymin, ymax);
                        }
                        if (Vymin < ymin) {
                            final double dy = Math.max(ymax - ymin, MINSIZE_RATIO * (Vymax - Vymin));
                            final double margin = Vymax + dy * ((MINSIZE_RATIO - 1) * 0.5);
                            clipChangeRequested(xmin, xmax, margin - dy, margin);
                        } else if (Vymax > ymax) {
                            final double dy = Math.max(ymax - ymin, MINSIZE_RATIO * (Vymax - Vymin));
                            final double margin = Vymin - dy * ((MINSIZE_RATIO - 1) * 0.5);
                            clipChangeRequested(xmin, xmax, margin, margin + dy);
                        }
                        /*
                         * Repositions the rectangle based on the new coordinates.
                         */
                        if (setFrame(Vxmin, Vymin, Vxmax - Vxmin, Vymax - Vymin)) {
                            if (toRepaint != null) toRepaint.repaint();
                        }
                    }
                }
                updateText(editor);
            }
        }

        /**
         * Invoked when the rectangle position is adjusted. This method adjusts
         * the value shown in the text field based on the position of the rectangle.
         */
        private void updateText(final JFormattedTextField editor) {
            String text;
            if (!logicalShape.isEmpty() ||
                ((text = editor.getText()) != null && !text.trim().isEmpty()))
            {
                double value;
                switch (inverseTransform(side)) {
                    case NORTH: value = logicalShape.getMinY(); break;
                    case SOUTH: value = logicalShape.getMaxY(); break;
                    case  WEST: value = logicalShape.getMinX(); break;
                    case  EAST: value = logicalShape.getMaxX(); break;
                    default   : return;
                }
                editor.setValue(isDate ? new Date(Math.round(value)) : Double.valueOf(value));
            }
        }

        /**
         * Updates the text which appears in {@link #editor}
         * based on the current position of the rectangle.
         */
        public void updateText() {
            updateText(editor);
        }
    }
}
