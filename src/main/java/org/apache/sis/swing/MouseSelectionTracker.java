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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;


/**
 * Controller which allows the user to select a region of a component.
 * The user must click on a point in the component, then drag the mouse pointer whilst keeping the button pressed.
 * During the dragging, the shape which is drawn is usually a rectangle. But other shapes can be used such as,
 * for example, an ellipse.
 * To use this class, it is necessary to create a derived class which defines the following methods:
 *
 * <ul>
 *   <li>{@link #selectionPerformed(int, int, int, int)} (mandatory)</li>
 *   <li>{@link #getModel} (optional)</li>
 * </ul>
 *
 * This controller should then be registered with one, and only one, component using the following syntax:
 *
 * {@snippet lang="java" :
 *     Component component = ...
 *     MouseSelectionTracker control = ...
 *     component.addMouseListener(control);
 *     }
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
public abstract class MouseSelectionTracker extends MouseInputAdapter {
    /**
     * Stippled rectangle representing the region which the user is currently selecting.
     * This rectangle can be empty. These coordinates are only significant in the period
     * between the user pressing the mouse button and then releasing it to outline a region.
     * Conventionally, the {@code null} value indicates that a line should be used instead
     * of a rectangular shape. The coordinates are always expressed in pixels.
     */
    private transient RectangularShape mouseSelectedArea;

    /**
     * Color to replace during XOR drawings on a graphic.
     * This color is specified in {@link Graphics2D#setColor(Color)}.
     */
    private Color backXORColor = Color.white;

    /**
     * Color to replace with during the XOR drawings on a graphic.
     * This color is specified in {@link Graphics2D#setXORMode(Color)}.
     */
    private Color lineXORColor = Color.black;

    /**
     * <var>x</var> coordinate of the mouse when the button is pressed.
     */
    private transient int ox;

    /**
     * <var>y</var> coordinate of the mouse when the button is pressed.
     */
    private transient int oy;

    /**
     * <var>x</var> coordinate of the mouse during the last drag.
     */
    private transient int px;

    /**
     * <var>y</var> coordinate of the mouse during the last drag.
     */
    private transient int py;

    /**
     * Indicates whether a selection is underway.
     */
    private transient boolean isDragging;

    /**
     * Constructs an object which will allow rectangular regions to be selected using the mouse.
     */
    public MouseSelectionTracker() {
    }

    /**
     * Specifies the colors to be used for drawing the outline of a box when the user selects a region.
     * All {@code a} colors will be replaced by {@code b} colors and vice versa.
     *
     * @param  a  the color to be replaced by color <var>b</var>.
     * @param  b  the color replacing the color <var>a</var>.
     */
    public void setXORColors(final Color a, final Color b) {
        backXORColor = a;
        lineXORColor = b;
    }

    /**
     * Returns the geometric shape to use for marking the boundaries of a region. The shape is usually a
     * rectangle but could also be an ellipse or other {@linkplain RectangularShape rectangular shapes}.
     * The coordinates of the returned shape will not be taken into account. In fact, these coordinates
     * will regularly be discarded. Only the class of the returned shape matter (for example,
     * {@link Ellipse2D} vs {@link Rectangle2D}) and their parameters which are not related to their position
     * (for example, the {@linkplain RoundRectangle2D#getArcWidth() arc size} of a rectangle with rounded corners).
     *
     * <p>The shape returned will usually be an instance of a class derived from {@link RectangularShape},
     * but could also be an instance of the {@link Line2D} class.
     * <strong>Any other class risks throwing a {@link ClassCastException} when executed</strong>.</p>
     *
     * <p>The default implementation always returns an instance of {@link Rectangle}.</p>
     *
     * @param  event  mouse coordinate when the button is pressed. This information can be used by subclasses
     *         overriding this method if the mouse location is relevant to the choice of a geometric shape.
     * @return shape from the class {@link RectangularShape} or {@link Line2D}, or {@code null}
     *         to indicate that we do not want to make a selection.
     */
    protected Shape getModel(final MouseEvent event) {
        return new Rectangle();
    }

    /**
     * Method which is automatically invoked after the user selects a region with the mouse.
     * All coordinates passed in as parameters are expressed in pixels.
     *
     * @param  ox  <var>x</var> coordinate of the mouse when the user pressed the mouse button.
     * @param  oy  <var>y</var> coordinate of the mouse when the user pressed the mouse button.
     * @param  px  <var>x</var> coordinate of the mouse when the user released the mouse button.
     * @param  py  <var>y</var> coordinate of the mouse when the user released the mouse button.
     */
    protected abstract void selectionPerformed(int ox, int oy, int px, int py);

    /**
     * Returns the geometric shape surrounding the last region to be selected by the user.
     * An optional affine transform can be specified to convert the region selected by the user
     * into logical coordinates. The class of the shape returned depends on the model returned
     * by {@link #getModel}:
     *
     * <ul>
     *   <li>If the model is an instance of {@link Line2D} (which means that this
     *       {@code MouseSelectionTracker} only draws a line between points),
     *       the object returned will belong to the {@link Line2D} class.</li>
     *   <li>Otherwise the object returned is usually (but not necessarily) an instance of the same class,
     *       usually {@link Rectangle2D}. There could be situations where the returned object is an instance
     *       of an another class, for example if the affine transform performs a rotation.</li>
     * </ul>
     *
     * @param  transform  affine transform which converts logical coordinates into pixel coordinates.
     *         This is usually the same transform than the one used for drawing in a {@link java.awt.Graphics2D} object.
     * @return a geometric shape enclosing the last region to be selected by the user,
     *         or {@code null} if no selection has yet been made.
     * @throws NoninvertibleTransformException if the affine transform cannot be inverted.
     */
    public Shape getSelectedArea(final AffineTransform transform) throws NoninvertibleTransformException {
        if (ox == px && oy == py) {
            return null;
        }
        RectangularShape shape = mouseSelectedArea;
        if (transform != null && !transform.isIdentity()) {
            if (shape == null) {
                final Point2D.Float po = new Point2D.Float(ox, oy);
                final Point2D.Float pp = new Point2D.Float(px, py);
                transform.inverseTransform(po, po);
                transform.inverseTransform(pp, pp);
                return new Line2D.Float(po, pp);
            } else {
                if (canReshape(shape, transform)) {
                    final Point2D.Double point = new Point2D.Double();
                    double xmin = Double.POSITIVE_INFINITY;
                    double ymin = Double.POSITIVE_INFINITY;
                    double xmax = Double.NEGATIVE_INFINITY;
                    double ymax = Double.NEGATIVE_INFINITY;
                    for (int i = 0; i < 4; i++) {
                        point.x = (i & 1) == 0 ? shape.getMinX() : shape.getMaxX();
                        point.y = (i & 2) == 0 ? shape.getMinY() : shape.getMaxY();
                        transform.inverseTransform(point, point);
                        if (point.x < xmin) xmin = point.x;
                        if (point.x > xmax) xmax = point.x;
                        if (point.y < ymin) ymin = point.y;
                        if (point.y > ymax) ymax = point.y;
                    }
                    if (shape instanceof Rectangle) {
                        return new Rectangle2D.Float((float) xmin,
                                                     (float) ymin,
                                                     (float) (xmax - xmin),
                                                     (float) (ymax - ymin));
                    } else {
                        shape = (RectangularShape) shape.clone();
                        shape.setFrame(xmin, ymin, xmax - xmin, ymax - ymin);
                        return shape;
                    }
                } else {
                    return transform.createInverse().createTransformedShape(shape);
                }
            }
        } else {
            return (shape != null) ? (Shape) shape.clone() : new Line2D.Float(ox, oy, px, py);
        }
    }

    /**
     * Indicates whether we can transform {@code shape} simply by calling its
     * {@code shape.setFrame(...)} method rather than by using the heavy artillery
     * that is the {@code transform.createTransformedShape(shape)} method.
     */
    private static boolean canReshape(final RectangularShape shape, final AffineTransform transform) {
        final int type=transform.getType();
        if ((type & AffineTransform.TYPE_GENERAL_TRANSFORM) != 0) return false;
        if ((type & AffineTransform.TYPE_MASK_ROTATION)     != 0) return false;
        if ((type & AffineTransform.TYPE_FLIP)              != 0) {
            if (shape instanceof Rectangle2D)      return true;
            if (shape instanceof Ellipse2D)        return true;
            if (shape instanceof RoundRectangle2D) return true;
            return false;
        }
        return true;
    }

    /**
     * Returns a {@link Graphics2D} object to be used for drawing in the specified component.
     * We must not forget to call {@link Graphics2D#dispose} when the graphics object is no longer needed.
     */
    private Graphics2D getGraphics(final Component c) {
        final Graphics2D graphics = (Graphics2D) c.getGraphics();
        graphics.setXORMode(lineXORColor);
        graphics.setColor  (backXORColor);
        return graphics;
    }

    /**
     * Notifies this controller that the mouse button has been pressed. The default implementation
     * memorizes the mouse coordinate (which will become one of the corners of the future rectangle
     * to be drawn) and prepares this {@code MouseSelectionTracker} to observe the mouse movements.
     *
     * @param  event  contains mouse coordinates where the button has been pressed.
     * @throws ClassCastException if {@link #getModel} doesn't return a shape
     *         from the class {@link RectangularShape} or {@link Line2D}.
     */
    @Override
    public void mousePressed(final MouseEvent event) throws ClassCastException {
        if (!event.isConsumed() && (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            final Component source = event.getComponent();
            if (source != null) {
                Shape model = getModel(event);
                if (model != null) {
                    isDragging = true;
                    ox = px = event.getX();
                    oy = py = event.getY();
                    if (model instanceof Line2D) {
                        model = null;
                    }
                    mouseSelectedArea = (RectangularShape) model;
                    if (mouseSelectedArea != null) {
                        mouseSelectedArea.setFrame(ox, oy, 0, 0);
                    }
                    source.addMouseMotionListener(this);
                }
                source.requestFocus();
                event.consume();
            }
        }
    }

    /**
     * Notifies this controller that the mouse has been dragged. The default implementation moves
     * a corner of the rectangle used to select the region. The other corner remains fixed at the
     * point where the mouse was at the moment it was {@linkplain #mousePressed pressed}.
     *
     * @param  event  contains mouse coordinates when the cursor is being dragged.
     */
    @Override
    public void mouseDragged(final MouseEvent event) {
        if (isDragging) {
            final Graphics2D graphics = getGraphics(event.getComponent());
            if (mouseSelectedArea == null) {
                graphics.drawLine(ox, oy, px, py);
                px = event.getX();
                py = event.getY();
                graphics.drawLine(ox, oy, px, py);
            } else {
                graphics.draw(mouseSelectedArea);
                int xmin = this.ox;
                int ymin = this.oy;
                int xmax = px = event.getX();
                int ymax = py = event.getY();
                if (xmin > xmax) {
                    final int xtmp = xmin;
                    xmin = xmax; xmax = xtmp;
                }
                if (ymin > ymax) {
                    final int ytmp = ymin;
                    ymin = ymax; ymax = ytmp;
                }
                mouseSelectedArea.setFrame(xmin, ymin, xmax - xmin, ymax - ymin);
                graphics.draw(mouseSelectedArea);
            }
            graphics.dispose();
            event.consume();
        }
    }

    /**
     * Notifies this controller that the mouse button has been released. The default implementation invokes
     * {@link #selectionPerformed(int, int, int, int)} with the bounds of the selected region as parameters.
     *
     * @param  event  contains mouse coordinates where the button has been released.
     */
    @Override
    public void mouseReleased(final MouseEvent event) {
        if (isDragging && (event.getButton() == MouseEvent.BUTTON1)) {
            isDragging = false;
            final Component component = event.getComponent();
            component.removeMouseMotionListener(this);

            final Graphics2D graphics = getGraphics(event.getComponent());
            if (mouseSelectedArea == null) {
                graphics.drawLine(ox, oy, px, py);
            } else {
                graphics.draw(mouseSelectedArea);
            }
            graphics.dispose();
            px = event.getX();
            py = event.getY();
            selectionPerformed(ox, oy, px, py);
            event.consume();
        }
    }

    /**
     * Notifies this controller that the mouse has been moved but not as a result of the user selecting a region.
     * The default implementation notifies the source component that this {@code MouseSelectionTracker} is no
     * longer interested in being informed about mouse movements.
     *
     * @param  event  contains mouse coordinates when the cursor is being moved.
     */
    @Override
    public void mouseMoved(final MouseEvent event) {
        event.getComponent().removeMouseMotionListener(this);
    }
}
