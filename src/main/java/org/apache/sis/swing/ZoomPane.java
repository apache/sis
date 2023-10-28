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

import java.util.EventListener;
import java.io.Serializable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BoundedRangeModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.sis.util.logging.Logging;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.swing.internal.Resources;

import static java.lang.Math.abs;
import static java.lang.Math.rint;


/**
 * Base class for widget with a zoomable content. User can perform zooms using keyboard, menu or mouse.
 * Subclasses must provide the content to be paint with the following methods, which need to be overridden:
 *
 * <ul class="verbose">
 *   <li>{@link #getArea()}, which must return a bounding box for the content to paint.
 *   This area can be expressed in arbitrary units. For example, an object wanting to display
 *   a geographic map with a content ranging from 10° to 15°E and 40° to 45°N should override
 *   this method as follows:
 *
 * {@snippet lang="java" :
 *     public Rectangle2D getArea() {
 *         return new Rectangle2D.Double(10, 40, 15-10, 45-40);
 *     }
 * }</li>
 *
 *   <li>{@link #paintComponent(Graphics2D)}, which must paint the widget content. Implementations
 *   must invoke <code>graphics.transform({@linkplain #zoom})</code> somewhere in their code in order
 *   to perform the zoom. Note that, by default, the {@linkplain #zoom} is initialized in such a way
 *   that the <var>y</var> axis points upwards, like the convention in geometry. This is opposed to
 *   the default Java2D axis orientation, where the <var>y</var> axis points downwards. The Java2D
 *   convention is appropriate for text rendering - consequently implementations wanting to paint
 *   text should use the default transform (the one provided by {@link Graphics2D}) for that purpose.
 *   Example:
 *
 * {@snippet lang="java" :
 *     protected void paintComponent(final Graphics2D graphics) {
 *         graphics.clip(getZoomableBounds(null));
 *         final AffineTransform textTr = graphics.getTransform();
 *         graphics.transform(zoom);
 *         // Paint the widget here, using logical coordinates.
 *         // The coordinate system is the same as getArea()'s one.
 *         graphics.setTransform(textTr);
 *         // Paint any text here, in pixel coordinates.
 *     }
 * }</li>
 *
 *   <li>{@link #reset()}, which sets up the initial {@linkplain #zoom}.
 *   Overriding this method is optional since the default implementation is appropriate in many cases.
 *   This default implementation setups the initial zoom in such a way that the following relation
 *   approximately hold: <cite>Logical coordinates provided by {@link #getPreferredArea()},
 *   after an affine transform described by {@link #zoom}, match pixel coordinates provided
 *   by {@link #getZoomableBounds(Rectangle)}.</cite></li>
 * </ul>
 *
 * The "preferred area" is initially the same as {@link #getArea()}.
 * The user can specify a different preferred area with {@link #setPreferredArea(Rectangle2D)}.
 * The user can also reduce zoomable bounds by inserting an empty border around the widget, e.g.:
 *
 * {@snippet lang="java" :
 *     setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
 *     }
 *
 * <h2>Zoom actions</h2>
 * Whatever action is performed by the user, all zoom commands are translated as calls to
 * {@link #transform(AffineTransform)}. Derived classes can redefine this method if they want
 * to take particular actions during zooms, for example, modifying the minimum and maximum of
 * a graph's axes. The table below shows the keyboard presses assigned to each zoom:
 *
 * <table class="sis">
 *   <caption>Key events</caption>
 *   <tr><th>Key</th>             <th>Purpose</th>                 <th>{@link Action} name</th></tr>
 *   <tr><td>↑ (up)</td>          <td>Scroll up</td>               <td>{@code "Up"}</td></tr>
 *   <tr><td>↓ (down)</td>        <td>Scroll down</td>             <td>{@code "Down"}</td></tr>
 *   <tr><td>← (left)</td>        <td>Scroll left</td>             <td>{@code "Left"}</td></tr>
 *   <tr><td>→ (right)</td>       <td>Scroll right</td>            <td>{@code "Right"}</td></tr>
 *   <tr><td>⎘ (page down)</td>   <td>Zoom in</td>                 <td>{@code "ZoomIn"}</td></tr>
 *   <tr><td>⎗ (page up)</td>     <td>Zoom out</td>                <td>{@code "ZoomOut"}</td></tr>
 *   <tr><td>end</td>             <td>Maximal zoom</td>            <td>{@code "Zoom"}</td></tr>
 *   <tr><td>home</td>            <td>Default zoom</td>            <td>{@code "Reset"}</td></tr>
 *   <tr><td>Ctrl + left</td>     <td>Anti-clockwise rotation</td> <td>{@code "RotateLeft"}</td></tr>
 *   <tr><td>Ctrl + right</td>    <td>Clockwise rotation</td>      <td>{@code "RotateRight"}</td></tr>
 * </table>
 *
 * In above table, the last column gives the {@link String}s that identify the different actions
 * which manage the zooms. For example, to get action for zoom in, we can write
 * <code>{@linkplain #getActionMap() getActionMap()}.get("ZoomIn")</code>.
 *
 * <h2>Scroll pane</h2>
 * <strong>{@link javax.swing.JScrollPane} objects are not suitable for adding scrollbars
 * to a {@code ZoomPane} object.</strong> Instead, use {@link #createScrollPane()}.
 * Like other actions, all movements performed by user through the scrollbars
 * will be translated in calls to {@link #transform(AffineTransform)}.
 *
 * <img src="doc-files/ZoomPane.png" alt="ZoomPane screenshot">
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
@SuppressWarnings("serial")
public abstract class ZoomPane extends JComponent implements DeformableViewer {
    /**
     * Whether to print debug messages.
     *
     * @see #debug(String, Rectangle2D)
     */
    private static final boolean DEBUG = false;

    /**
     * Minimum width and height of this component.
     */
    private static final int MINIMUM_SIZE = 40;

    /**
     * Default width and height of this component.
     */
    private static final int DEFAULT_SIZE = 400;

    /**
     * Default width and height of the magnifying glass.
     */
    private static final int DEFAULT_MAGNIFIER_SIZE = 250;

    /**
     * Default color with which to tint magnifying glass.
     */
    private static final Paint DEFAULT_MAGNIFIER_GLASS = new Color(209, 225, 243);

    /**
     * Default color of the magnifying glass border.
     */
    private static final Paint DEFAULT_MAGNIFIER_BORDER = new Color(110, 129, 177);

    /**
     * Small number for floating point comparisons.
     */
    private static final double EPS = 1E-6;

    /**
     * Constant indicating scale changes on the <var>x</var> axis.
     */
    public static final int SCALE_X = 1;

    /**
     * Constant indicating scale changes on the <var>y</var> axis.
     */
    public static final int SCALE_Y = (1 << 1);

    /**
     * Constant indicating scale changes by the same value on both the <var>x</var> and <var>y</var> axes.
     * This flag combines {@link #SCALE_X} and {@link #SCALE_Y}.
     * <b>Note:</b> the converse (<code>{@linkplain #SCALE_X}|{@linkplain #SCALE_Y}</code>)
     * does not necessarily imply {@code UNIFORM_SCALE}.
     */
    public static final int UNIFORM_SCALE = SCALE_X | SCALE_Y | (1 << 2);

    /**
     * Constant indicating translations on the <var>x</var> axis.
     */
    public static final int TRANSLATE_X = (1 << 3);

    /**
     * Constant indicating translations on the <var>y</var> axis.
     */
    public static final int TRANSLATE_Y = (1 << 4);

    /**
     * Constant indicating rotations.
     */
    public static final int ROTATE  = (1 << 5);

    /**
     * Constant indicating the resetting of scale, rotation and translation to default values.
     * Those default values ensure that the content is fully contained in the window.
     * This action is implemented by a call to {@link #reset()}.
     */
    public static final int RESET = (1 << 6);

    /**
     * Constant indicating default zoom close to the maximum permitted zoom.
     * This zoom should allow details of the graphic to be seen without being overly big.
     */
    public static final int DEFAULT_ZOOM = (1 << 7);

    /**
     * Combination of all permitted flags.
     */
    private static final int MASK = SCALE_X | SCALE_Y | UNIFORM_SCALE | TRANSLATE_X | TRANSLATE_Y |
                                    ROTATE | RESET | DEFAULT_ZOOM;

    /**
     * Number of pixels by which to move the {@code ZoomPane} content during translations.
     */
    private static final double AMOUNT_TRANSLATE = 10;

    /**
     * Zoom factor (must be greater than 1).
     */
    private static final double AMOUNT_SCALE = 1.03125;

    /**
     * Rotation angle in radians.
     */
    private static final double AMOUNT_ROTATE = Math.PI / 90;

    /**
     * Multiplication factor to apply on {@link #ACTION_AMOUNT} numbers when the "Shift" key is kept pressed.
     */
    private static final double ENHANCEMENT_FACTOR = 7.5;

    /**
     * Enumeration value indicating that a paint is in progress.
     *
     * @see #renderingType
     */
    private static final int IS_PAINTING = 0;

    /**
     * Enumeration value indicating that a paint of the magnifying glass is in progress.
     *
     * @see #renderingType
     */
    private static final int IS_PAINTING_MAGNIFIER = 1;

    /**
     * Enumeration value indicating that a print is in progress.
     *
     * @see #renderingType
     */
    private static final int IS_PRINTING = 2;

    /**
     * List of keys identifying zoom actions.
     */
    private static final String[] ACTION_ID = {
        /*[0] Left        */ "Left",
        /*[1] Right       */ "Right",
        /*[2] Up          */ "Up",
        /*[3] Down        */ "Down",
        /*[4] ZoomIn      */ "ZoomIn",
        /*[5] ZoomOut     */ "ZoomOut",
        /*[6] ZoomMax     */ "ZoomMax",
        /*[7] Reset       */ "Reset",
        /*[8] RotateLeft  */ "RotateLeft",
        /*[9] RotateRight */ "RotateRight"
    };

    /**
     * List of resource keys for building menus in user's language.
     * Must be in same order than {@link #ACTION_ID}.
     */
    private static final short[] RESOURCE_ID = {
        /*[0] Left        */ Resources.Keys.Left,
        /*[1] Right       */ Resources.Keys.Right,
        /*[2] Up          */ Resources.Keys.Up,
        /*[3] Down        */ Resources.Keys.Down,
        /*[4] ZoomIn      */ Resources.Keys.ZoomIn,
        /*[5] ZoomOut     */ Resources.Keys.ZoomOut,
        /*[6] ZoomMax     */ Resources.Keys.ZoomMax,
        /*[7] Reset       */ Resources.Keys.Reset,
        /*[8] RotateLeft  */ Resources.Keys.RotateLeft,
        /*[9] RotateRight */ Resources.Keys.RotateRight
    };

    /**
     * List of default keystrokes performing zooms. Elements in this table go in pairs:
     * elements at even indices are keystroke whilst elements at odd indices are modifier
     * (CTRL or SHIFT). To obtain the {@link KeyStroke} object for action <var>i</var>,
     * we can use the following code:
     *
     * {@snippet lang="java" :
     *     final int key = DEFAULT_KEYBOARD[(i << 1)+0];
     *     final int mdf = DEFAULT_KEYBOARD[(i << 1)+1];
     *     KeyStroke stroke = KeyStroke.getKeyStroke(key, mdf);
     *     }
     */
    private static final int[] ACTION_KEY = {
        /*[0] Left        */ KeyEvent.VK_LEFT,      0,
        /*[1] Right       */ KeyEvent.VK_RIGHT,     0,
        /*[2] Up          */ KeyEvent.VK_UP,        0,
        /*[3] Down        */ KeyEvent.VK_DOWN,      0,
        /*[4] ZoomIn      */ KeyEvent.VK_PAGE_UP,   0,
        /*[5] ZoomOut     */ KeyEvent.VK_PAGE_DOWN, 0,
        /*[6] ZoomMax     */ KeyEvent.VK_END,       0,
        /*[7] Reset       */ KeyEvent.VK_HOME,      0,
        /*[8] RotateLeft  */ KeyEvent.VK_LEFT,      KeyEvent.CTRL_DOWN_MASK,
        /*[9] RotateRight */ KeyEvent.VK_RIGHT,     KeyEvent.CTRL_DOWN_MASK
    };

    /**
     * Constants indicating the type of action to apply: translation, zoom or rotation.
     */
    private static final short[] ACTION_TYPE = {
        /*[0] Left        */ (short) TRANSLATE_X,
        /*[1] Right       */ (short) TRANSLATE_X,
        /*[2] Up          */ (short) TRANSLATE_Y,
        /*[3] Down        */ (short) TRANSLATE_Y,
        /*[4] ZoomIn      */ (short) SCALE_X | SCALE_Y,
        /*[5] ZoomOut     */ (short) SCALE_X | SCALE_Y,
        /*[6] ZoomMax     */ (short) DEFAULT_ZOOM,
        /*[7] Reset       */ (short) RESET,
        /*[8] RotateLeft  */ (short) ROTATE,
        /*[9] RotateRight */ (short) ROTATE
    };

    /**
     * Amounts by which to translate, zoom or rotate the window content.
     */
    private static final double[] ACTION_AMOUNT = {
        /*[0] Left        */  +AMOUNT_TRANSLATE,
        /*[1] Right       */  -AMOUNT_TRANSLATE,
        /*[2] Up          */  +AMOUNT_TRANSLATE,
        /*[3] Down        */  -AMOUNT_TRANSLATE,
        /*[4] ZoomIn      */   AMOUNT_SCALE,
        /*[5] ZoomOut     */ 1/AMOUNT_SCALE,
        /*[6] ZoomMax     */   Double.NaN,
        /*[7] Reset       */   Double.NaN,
        /*[8] RotateLeft  */  -AMOUNT_ROTATE,
        /*[9] RotateRight */  +AMOUNT_ROTATE
    };

    /**
     * List of operation types forming a group in the contextual menu.
     * Group will be separated by a menu separator.
     */
    private static final int[] GROUP = {
        TRANSLATE_X | TRANSLATE_Y,
        SCALE_X | SCALE_Y | DEFAULT_ZOOM | RESET,
        ROTATE
    };

    /**
     * {@code ComponentUI} object in charge of obtaining the preferred
     * size of a {@code ZoomPane} object as well as drawing it.
     */
    private static final ComponentUI UI = new ComponentUI() {
        /**
         * Returns a default minimum size.
         */
        @Override
        public Dimension getMinimumSize(final JComponent c) {
            return new Dimension(MINIMUM_SIZE, MINIMUM_SIZE);
        }

        /**
         * Returns the maximum size. We use the preferred size as a default maximum size.
         */
        @Override
        public Dimension getMaximumSize(final JComponent c) {
            return getPreferredSize(c);
        }

        /**
         * Returns the default preferred size. User can override this preferred size
         * by invoking {@link JComponent#setPreferredSize(Dimension)}.
         */
        @Override
        public Dimension getPreferredSize(final JComponent c) {
            return ((ZoomPane) c).getDefaultSize();
        }

        /**
         * Overrides in order to handle painting of magnifying glass, which is a special case.
         * Since the magnifying glass is painted just after the normal component, we do not want
         * to clear the background before painting it.
         */
        @Override
        public void update(final Graphics g, final JComponent c) {
            switch (((ZoomPane) c).renderingType) {
                case IS_PAINTING_MAGNIFIER: paint(g, c); break;     // Avoid background clearing
                default: super.update(g, c); break;
            }
        }

        /**
         * Paints the component. This method basically delegates the
         * work to {@link ZoomPane#paintComponent(Graphics2D)}.
         */
        @Override
        public void paint(final Graphics g, final JComponent c) {
            final ZoomPane pane = (ZoomPane)   c;
            final Graphics2D gr = (Graphics2D) g;
            switch (pane.renderingType) {
                case IS_PAINTING:           pane.paintComponent(gr); break;
                case IS_PAINTING_MAGNIFIER: pane.paintMagnifier(gr); break;
                case IS_PRINTING:           pane.printComponent(gr); break;
                default: throw new IllegalStateException(Integer.toString(pane.renderingType));
            }
        }
    };

    /**
     * Object in charge of drawing a box representing the user's selection.
     */
    private final MouseListener mouseSelectionTracker = new MouseSelectionTracker() {
        /**
         * Returns the selection shape. This is usually a rectangle, but could also be an ellipse or other kind
         * of geometric shape. This method gets the shape from {@link ZoomPane#getMouseSelectionShape(Point2D)}.
         */
        @Override
        protected Shape getModel(final MouseEvent event) {
            final Point2D point = new Point2D.Double(event.getX(), event.getY());
            if (getZoomableBounds().contains(point)) try {
                return getMouseSelectionShape(zoom.inverseTransform(point, point));
            } catch (NoninvertibleTransformException exception) {
                unexpectedException("getModel", exception);
            }
            return null;
        }

        /**
         * Invoked when the user finished his/her selection. This method delegates the action to
         * {@link ZoomPane#mouseSelectionPerformed(Shape)}, which default implementation performs a zoom.
         */
        @Override
        protected void selectionPerformed(int ox, int oy, int px, int py) {
            try {
                final Shape selection = getSelectedArea(zoom);
                if (selection != null) {
                    mouseSelectionPerformed(selection);
                }
            } catch (NoninvertibleTransformException exception) {
                unexpectedException("selectionPerformed", exception);
            }
        }
    };

    /**
     * Group of listeners for various events of interest for {@link ZoomPane}.
     * Its includes mouse clicks in order to eventually claim focus or show contextual menu.
     * Also listen for changes of component size (to adjust the zoom), <i>etc.</i>
     */
    @SuppressWarnings("serial")
    private final class Listeners extends MouseAdapter implements MouseWheelListener, ComponentListener, Serializable {
        @Override public void mouseWheelMoved (final MouseWheelEvent event) {ZoomPane.this.mouseWheelMoved (event);}
        @Override public void mousePressed    (final MouseEvent      event) {ZoomPane.this.mayShowPopupMenu(event);}
        @Override public void mouseReleased   (final MouseEvent      event) {ZoomPane.this.mayShowPopupMenu(event);}
        @Override public void componentResized(final ComponentEvent  event) {ZoomPane.this.processSizeEvent(event);}
        @Override public void componentMoved  (final ComponentEvent  event) {}
        @Override public void componentShown  (final ComponentEvent  event) {}
        @Override public void componentHidden (final ComponentEvent  event) {}
    }

    /**
     * Affine transform containing zoom factors, translations and rotations.
     * During component painting, this affine transform should be applied with a call to
     * <code>{@linkplain Graphics2D#transform(AffineTransform) Graphics2D.transform}(zoom)</code>.
     */
    protected final AffineTransform zoom = new AffineTransform();

    /**
     * Indicates whether the zoom is the result of a {@link #reset()} operation.
     * This is used in order to determine which behavior to replicate when the widget is resized.
     */
    private boolean zoomIsReset = true;

    /**
     * {@code true} if calls to {@link #repaint()} should be temporarily disabled.
     */
    private boolean disableRepaint;

    /**
     * Types of zoom permitted. Values can be combinations of {@link #SCALE_X}, {@link #SCALE_Y},
     * {@link #TRANSLATE_X}, {@link #TRANSLATE_Y}, {@link #ROTATE}, {@link #RESET} or {@link #DEFAULT_ZOOM}.
     */
    private final int allowedActions;

    /**
     * Controls how to calculate the initial affine transform. The {@code true} value specifies that
     * content should fill the entire panel, even if it implies losing some content close to the edges.
     * The {@code false} value specifies to display the entire content, even if it means leaving blank
     * spaces in the panel.
     */
    private boolean fillPanel;

    /**
     * Logical coordinates of visible region. This information is used for keeping the same region when
     * the component size or position changes. This rectangle is initially empty and get values only when
     * {@link #reset()} is invoked while {@link #getPreferredArea()} and {@link #getZoomableBounds()} can
     * both return valid coordinates.
     *
     * @see #getVisibleArea()
     * @see #setVisibleArea(Rectangle2D)
     */
    private final Rectangle2D visibleArea = new Rectangle2D.Double();

    /**
     * Logical coordinates of the initial region to display, the first time that the window is shown.
     * A {@code null} value indicates a call to {@link #getArea()}.
     *
     * @see #getPreferredArea()
     * @see #setPreferredArea(Rectangle2D)
     */
    private Rectangle2D preferredArea;

    /**
     * Menu to show on mouse right click. This menu will contain navigation options.
     *
     * @see #getPopupMenu(MouseEvent)
     */
    private transient PointPopupMenu navigationPopupMenu;

    /**
     * Enumeration value indicating which kind of painting is in progress. Permitted values are
     * {@link #IS_PAINTING}, {@link #IS_PAINTING_MAGNIFIER} and {@link #IS_PRINTING}.
     */
    private transient int renderingType;

    /**
     * Indicates if this {@code ZoomPane} should be repainted when the user adjusts the scrollbars.
     * The default value is {@code true}.
     *
     * @see #isPaintingWhileAdjusting()
     * @see #setPaintingWhileAdjusting(boolean)
     */
    private boolean paintingWhileAdjusting = true;

    /**
     * Object in which to write coordinates computed by {@link #getZoomableBounds()}.
     * Used for reducing the amount of object allocations.
     */
    private transient Rectangle cachedBounds;

    /**
     * Object in which to write values computed by {@link #getInsets()}.
     * Used for reducing the amount of object allocations.
     */
    private transient Insets cachedInsets;

    /**
     * Indicates whether the user is authorized to show the magnifying glass.
     * The default value is {@code true}.
     */
    private boolean magnifierEnabled = true;

    /**
     * Magnification factor inside the magnifying glass. This factor must be greater than 1.
     */
    private double magnifierPower = 4;

    /**
     * Boundaries of the region to magnify. Coordinates of this shape are in pixels.
     * The {@code null} value means that no magnifying glass is drawn.
     */
    private transient MouseReshapeTracker magnifier;

    /**
     * Color with which to tint magnifying glass interior.
     */
    private Paint magnifierGlass = DEFAULT_MAGNIFIER_GLASS;

    /**
     * Color of the magnifying glass border.
     */
    private Paint magnifierBorder = DEFAULT_MAGNIFIER_BORDER;

    /**
     * Creates a new zoom pane allowing all actions.
     */
    public ZoomPane() {
        this(UNIFORM_SCALE | ROTATE | TRANSLATE_X | TRANSLATE_Y | RESET | DEFAULT_ZOOM);
    }

    /**
     * Constructs a {@code ZoomPane}.
     *
     * @param allowedActions
     *             allowed zoom actions. It can be a bitwise combination of the following constants:
     *             {@link #SCALE_X}, {@link #SCALE_Y}, {@link #UNIFORM_SCALE}, {@link #TRANSLATE_X},
     *             {@link #TRANSLATE_Y}, {@link #ROTATE}, {@link #RESET} and {@link #DEFAULT_ZOOM}.
     * @throws IllegalArgumentException if {@code type} is invalid.
     */
    public ZoomPane(final int allowedActions) throws IllegalArgumentException {
        if ((allowedActions & ~MASK) != 0) {
            throw new IllegalArgumentException();
        }
        this.allowedActions = allowedActions;
        final Resources resources = Resources.forLocale(null);
        final InputMap   inputMap = super.getInputMap();
        final ActionMap actionMap = super.getActionMap();
        for (int i = 0; i < ACTION_ID.length; i++) {
            final short actionType = ACTION_TYPE[i];
            if ((actionType & allowedActions) != 0) {
                final String  actionID = ACTION_ID[i];
                final double    amount = ACTION_AMOUNT[i];
                final int     keyboard = ACTION_KEY[(i << 1) + 0];
                final int     modifier = ACTION_KEY[(i << 1) + 1];
                final KeyStroke stroke = KeyStroke.getKeyStroke(keyboard, modifier);
                final Action    action = new AbstractAction() {
                    /*
                     * Action to perform when a key has been pressed or the mouse clicked.
                     */
                    @Override
                    public void actionPerformed(final ActionEvent event) {
                        Point point = null;
                        final Object  source = event.getSource();
                        final boolean button = (source instanceof AbstractButton);
                        if (button) {
                            for (Container c = (Container) source; c != null; c = c.getParent()) {
                                if (c instanceof PointPopupMenu) {
                                    point = ((PointPopupMenu) c).point;
                                    break;
                                }
                            }
                        }
                        double m = amount;
                        if (button || (event.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                            if ((actionType & UNIFORM_SCALE) != 0) {
                                m = (m >= 1) ? 2.0 : 0.5;
                            }
                            else {
                                m *= ENHANCEMENT_FACTOR;
                            }
                        }
                        transform(actionType & allowedActions, m, point);
                    }
                };
                action.putValue(Action.NAME, resources.getString(RESOURCE_ID[i]));
                action.putValue(Action.ACTION_COMMAND_KEY, actionID);
                action.putValue(Action.ACCELERATOR_KEY, stroke);
                actionMap.put(actionID, action);
                inputMap .put(stroke, actionID);
                inputMap .put(KeyStroke.getKeyStroke(keyboard, modifier | KeyEvent.SHIFT_DOWN_MASK), actionID);
            }
        }
        /*
         * Adds a listeners for mouse clicks and resizing events.
         */
        final Listeners listeners = new Listeners();
        super.addComponentListener(listeners);
        super.addMouseListener(listeners);
        if ((allowedActions & (SCALE_X | SCALE_Y)) != 0) {
            super.addMouseWheelListener(listeners);
        }
        super.addMouseListener(mouseSelectionTracker);
        super.setBackground(Color.WHITE);
        super.setAutoscrolls(true);
        super.setFocusable(true);
        super.setOpaque(true);
        super.setUI(UI);
    }

    /**
     * Reinitializes the {@link #zoom} affine transform in order to cancel any zoom, rotation or translation.
     * Default implementation makes the <var>y</var> axis orientation upwards and makes the entire content to be
     * visible in the {@link #getPreferredArea()} logical coordinates.
     *
     * <h4>Implementation note</h4>
     * {@code reset()} is <u>the only</u> {@code ZoomPane} method which does not delegate
     * to {@link #transform(AffineTransform)} method for modifying the zoom.
     * This exception is necessary for avoiding an infinite loop.
     */
    public void reset() {
        reset(getZoomableBounds(), true);
    }

    /**
     * Reinitializes the {@link #zoom} affine transform in order to cancel any zoom, rotation or translation.
     * The {@code yAxisUpward} argument indicates whether the <var>y</var> axis should point upwards.
     * A {@code false} value lets it point downwards. This is a convenience method for subclasses which want
     * to override {@link #reset()}.
     *
     * @param zoomableBounds  pixel coordinates of the region where to draw. Typical value is
     *        <code>{@linkplain #getZoomableBounds(Rectangle) getZoomableBounds}(null)</code>.
     * @param yAxisUpward {@code true} if the <var>y</var> axis should point upwards rather than downwards.
     */
    protected void reset(final Rectangle zoomableBounds, final boolean yAxisUpward) {
        if (!zoomableBounds.isEmpty()) {
            final Rectangle2D area = getPreferredArea();
            if (isValid(area)) {
                final AffineTransform change;
                try {
                    change = zoom.createInverse();
                } catch (NoninvertibleTransformException exception) {
                    unexpectedException("reset", exception);
                    return;
                }
                if (yAxisUpward) {
                    zoom.setToScale(+1, -1);
                } else {
                    zoom.setToIdentity();
                }
                final AffineTransform transform = setVisibleArea(area, zoomableBounds,
                                        SCALE_X | SCALE_Y | TRANSLATE_X | TRANSLATE_Y);
                change.concatenate(zoom);
                zoom  .concatenate(transform);
                change.concatenate(transform);
                getVisibleArea(zoomableBounds);         // Force update of `visibleArea`
                /*
                 * The three private versions `fireZoomPane0`, `getVisibleArea` and `setVisibleArea`
                 * avoid invoking other `ZoomPane` methods in order to avoid an infinite loop.
                 */
                if (!change.isIdentity()) {
                    fireZoomChanged0(change);
                    if (!disableRepaint) {
                        repaint(zoomableBounds);
                    }
                }
                zoomIsReset = true;
                debug("reset", visibleArea);
            }
        }
    }

    /**
     * Indicates whether the zoom is the result of a {@link #reset()} operation.
     */
    final boolean zoomIsReset() {
        return zoomIsReset;
    }

    /**
     * Sets the policy for the zoom when the content is initially drawn or when the user resets the zoom.
     * Value {@code true} means that the panel should initially be completely filled, even if the content
     * partially falls outside the panel's bounds. Value {@code false} means that the full content should
     * appear in the panel, even if some space is not used. Default value is {@code false}.
     *
     * @param fill {@code true} if the panel should be initially completely filled.
     */
    protected void setResetPolicy(final boolean fill) {
        fillPanel = fill;
    }

    /**
     * Returns a bounding box that contains the logical coordinates of all data that may be displayed
     * in this {@code ZoomPane}. For example if this {@code ZoomPane} is to display a geographic map,
     * then this method should return the map's bounds in degrees of latitude and longitude (if the
     * underlying CRS is {@linkplain org.opengis.referencing.crs.GeographicCRS geographic}), in metres
     * (if the underlying CRS is {@linkplain org.opengis.referencing.crs.ProjectedCRS projected}) or
     * some other geodetic units. This bounding box is completely independent of any current zoom
     * setting and will change only if the content changes.
     *
     * @return a bounding box for the logical coordinates of all contents that are going to be
     *         drawn in this {@code ZoomPane}. If this bounding box is unknown, then this method
     *         can return {@code null} (but this is not recommended).
     */
    public abstract Rectangle2D getArea();

    /**
     * Indicates whether the logical coordinates of a region have been defined. This method returns
     * {@code true} if {@link #setPreferredArea(Rectangle2D)} has been invoked with a non null argument.
     *
     * @return {@code true} if a preferred area has been set.
     */
    public final boolean hasPreferredArea() {
        return preferredArea != null;
    }

    /**
     * Returns the logical coordinates of the region to display the first time that {@code ZoomPane} is shown.
     * This region will also be displayed each time the method {@link #reset()} is invoked.
     * The default implementation goes as follows:
     *
     * <ul>
     *   <li>If a region has already been defined by a call to {@link #setPreferredArea(Rectangle2D)},
     *       this region will be returned.</li>
     *   <li>If not, the whole region {@link #getArea()} will be returned.</li>
     * </ul>
     *
     * @return the logical coordinates of the region to be initially displayed,
     *         or {@code null} if these coordinates are unknown.
     */
    public final Rectangle2D getPreferredArea() {
        return (preferredArea != null) ? (Rectangle2D) preferredArea.clone() : getArea();
    }

    /**
     * Specifies the logical coordinates of the region to display the first time that {@code ZoomPane} is shown.
     * This region will also be displayed when {@link #reset()} method is invoked.
     *
     * @param  area  the logical coordinates of the region to be initially displayed, or {@code null}.
     */
    public final void setPreferredArea(final Rectangle2D area) {
        if (area == null) {
            preferredArea = null;
        } else if (isValid(area)) {
            final Object oldArea;
            if (preferredArea == null) {
                oldArea = null;
                preferredArea = new Rectangle2D.Double();
            }
            else oldArea = preferredArea.clone();
            preferredArea.setRect(area);
            firePropertyChange("preferredArea", oldArea, area);
            debug("setPreferredArea", area);
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "area"));
        }
    }

    /**
     * Returns the logical coordinates of the region currently shown. In the case of a map,
     * the logical coordinates can be expressed in degrees of latitude/longitude or in metres
     * if a cartographic projection has been applied.
     *
     * @return the region currently shown, in logical coordinates.
     */
    public final Rectangle2D getVisibleArea() {
        return getVisibleArea(getZoomableBounds());
    }

    /**
     * Implementation of {@link #getVisibleArea()}.
     */
    private Rectangle2D getVisibleArea(final Rectangle zoomableBounds) {
        if (zoomableBounds.isEmpty()) {
            return (Rectangle2D) visibleArea.clone();
        }
        Rectangle2D visible;
        try {
            visible = AffineTransforms2D.inverseTransform(zoom, zoomableBounds, null);
        } catch (NoninvertibleTransformException exception) {
            unexpectedException("getVisibleArea", exception);
            visible = new Rectangle2D.Double(zoomableBounds.getCenterX(),
                                             zoomableBounds.getCenterY(), 0, 0);
        }
        visibleArea.setRect(visible);
        return visible;
    }

    /**
     * Zooms to a given region specified in logical coordinates.
     * This method modifies the zoom and the translation in order to display the specified region.
     * If {@link #zoom} contains a rotation, this rotation will not be modified.
     *
     * @param  logicalBounds  logical coordinates of the region to be shown.
     * @throws IllegalArgumentException if {@code source} is empty.
     */
    public void setVisibleArea(final Rectangle2D logicalBounds) throws IllegalArgumentException {
        debug("setVisibleArea", logicalBounds);
        transform(setVisibleArea(logicalBounds, getZoomableBounds(), 0));
    }

    /**
     * Implementation of {@link #setVisibleArea(Rectangle2D)}.
     *
     * @param  source  logical coordinates of the region to be shown.
     * @param  dest    pixel coordinates of the window region where to draw (usually {@link #getZoomableBounds()}).
     * @param  mask    a mask to combine with the {@link #allowedActions} for determining which transformations are
     *                 allowed. The {@link #allowedActions} is not modified.
     * @return change to apply to the {@link #zoom} affine transform.
     * @throws IllegalArgumentException if {@code source} is empty.
     */
    private AffineTransform setVisibleArea(Rectangle2D source, Rectangle2D dest, int mask) throws IllegalArgumentException {
        /*
         * Reject invalid source rectangle, but be more flexible for destination
         * rectangle because the window could have been resized by the user.
         */
        if (!isValid(source)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "source"));
        }
        if (!isValid(dest)) {
            return new AffineTransform();
        }
        /*
         * Converts the destination into logical coordinates, then apply
         * a zoom and a translation mapping `source` into `dest`.
         */
        try {
            dest = AffineTransforms2D.inverseTransform(zoom, dest, null);
        } catch (NoninvertibleTransformException exception) {
            unexpectedException("setVisibleArea", exception);
            return new AffineTransform();
        }
        final double sourceWidth  = source.getWidth ();
        final double sourceHeight = source.getHeight();
        final double destWidth    =   dest.getWidth ();
        final double destHeight   =   dest.getHeight();
        double sx = destWidth / sourceWidth;
        double sy = destHeight / sourceHeight;
        /*
         * Uniformize the horizontal and vertical scales,
         * if such a uniformization has been requested.
         */
        mask |= allowedActions;
        if ((mask & UNIFORM_SCALE) == UNIFORM_SCALE) {
            if (fillPanel) {
                if (sy * sourceWidth  > destWidth ) {
                    sx = sy;
                } else if (sx * sourceHeight > destHeight) {
                    sy = sx;
                }
            } else {
                if (sy * sourceWidth  < destWidth ) {
                    sx = sy;
                } else if (sx * sourceHeight < destHeight) {
                    sy = sx;
                }
            }
        }
        final AffineTransform change = AffineTransform.getTranslateInstance(
                         (mask & TRANSLATE_X) != 0 ? dest.getCenterX()    : 0,
                         (mask & TRANSLATE_Y) != 0 ? dest.getCenterY()    : 0);
        change.scale    ((mask & SCALE_X    ) != 0 ? sx                   : 1,
                         (mask & SCALE_Y    ) != 0 ? sy                   : 1);
        change.translate((mask & TRANSLATE_X) != 0 ? -source.getCenterX() : 0,
                         (mask & TRANSLATE_Y) != 0 ? -source.getCenterY() : 0);
        roundIfAlmostInteger(change);
        return change;
    }

    /**
     * Returns the bounding box (in pixel coordinates) of the zoomable area.
     * <strong>For performance reasons, this method reuses an internal cache.
     * Never modify the returned rectangle!</strong>. This internal method is
     * invoked by every method looking for the {@code ZoomPane} dimension.
     *
     * @return the bounding box of the zoomable area, in pixel coordinates relative to this {@code ZoomPane} widget.
     *         <strong>Do not change the returned rectangle!</strong>
     */
    private Rectangle getZoomableBounds() {
        return cachedBounds = getZoomableBounds(cachedBounds);
    }

    /**
     * Returns the bounding box (in pixel coordinates) of the zoomable area. This method is similar
     * to {@link #getBounds(Rectangle)}, except that the zoomable area may be smaller than the whole
     * widget area. For example, a chart needs to keep some space for axes around the zoomable area.
     * Another difference is that pixel coordinates are relative to the widget, i.e. the (0,0)
     * coordinate lies on the {@code ZoomPane} upper left corner, no matter the location on screen.
     *
     * <p>{@code ZoomPane} invokes {@code getZoomableBounds(…)} when it needs to set up an initial {@link #zoom} value.
     * Subclasses should also set the clip area to this bounding box in their {@link #paintComponent(Graphics2D)}
     * method <em>before</em> setting the graphics transform. For example:</p>
     *
     * {@snippet lang="java" :
     *     graphics.clip(getZoomableBounds(null));
     *     graphics.transform(zoom);
     *     }
     *
     * @param  bounds  an optional pre-allocated rectangle, or {@code null} to create a new one.
     * @return the bounding box of the zoomable area, in pixel coordinates relative to this {@code ZoomPane} widget.
     */
    protected Rectangle getZoomableBounds(Rectangle bounds) {
        Insets insets;
        bounds = getBounds(bounds); insets = cachedInsets;
        insets = getInsets(insets); cachedInsets = insets;
        if (bounds.isEmpty()) {
            final Dimension size = getPreferredSize();
            bounds.width  = size.width;
            bounds.height = size.height;
        }
        bounds.x       =  insets.left;
        bounds.y       =  insets.top;
        bounds.width  -= (insets.left + insets.right);
        bounds.height -= (insets.top + insets.bottom);
        return bounds;
    }

    /**
     * Returns the default size for this component. This is the size returned by {@link #getPreferredSize()}
     * if no preferred size has been explicitly set with {@link #setPreferredSize(Dimension)}.
     *
     * @return the default size for this component.
     */
    protected Dimension getDefaultSize() {
        return getViewSize();
    }

    /**
     * Returns the preferred pixel size for a close zoom. For image rendering, the preferred pixel size
     * is the image's pixel size in logical units. For other kinds of rendering, this "pixel" size should
     * be some reasonable resolution. The default implementation computes a default value from {@link #getArea()}.
     *
     * @return the preferred pixel size for a close zoom, in logical units.
     */
    protected Dimension2D getPreferredPixelSize() {
        final Rectangle2D area = getArea();
        if (isValid(area)) {
            final double sx = area.getWidth () / (10 * getWidth ());
            final double sy = area.getHeight() / (10 * getHeight());
            return new DoubleDimension2D(sx, sy);
        } else {
            return new Dimension(1, 1);
        }
    }

    /**
     * Returns the current {@link #zoom} scale factor. For example, a value of 1/100 means that 100 metres are
     * displayed as 1 pixel (assuming that the logical coordinates of {@link #getArea()} are expressed in metres).
     *
     * <p>This method combines scale along both axes, which is correct if this {@code ZoomPane} has
     * been constructed with the {@link #UNIFORM_SCALE} type.</p>
     *
     * @return the current scale factor calculated from the {@link #zoom} affine transform.
     */
    public double getScaleFactor() {
        return AffineTransforms2D.getScale(zoom);
    }

    /**
     * Returns a clone of the current {@link #zoom} transform.
     *
     * @return a clone of the current transform.
     */
    public AffineTransform getTransform() {
        return new AffineTransform(zoom);
    }

    /**
     * Sets the {@link #zoom} transform to the given value. The default implementation computes an affine transform
     * which is the change needed for going from the current {@linkplain #zoom} to the given transform, then calls
     * {@link #transform(AffineTransform)} with that change. This is done that way for giving listeners a chance to
     * track the changes.
     *
     * @param  tr  the new transform.
     */
    public void setTransform(final AffineTransform tr) {
        final AffineTransform change;
        try {
            change = zoom.createInverse();
        } catch (NoninvertibleTransformException exception) {
            /*
             * Invoke the static method because we will not be able to invoke fireZoomChanged(…).
             * This is because we cannot compute the change.
             */
            unexpectedException("setTransform", (Exception) exception);
            zoom.setTransform(tr);
            return;
        }
        change.concatenate(tr);
        roundIfAlmostInteger(change);
        transform(change);
    }

    /**
     * Changes the {@linkplain #zoom} by applying an affine transform. The {@code change} transform
     * must express a change in logical units, for example, a translation in metres.
     * This method is conceptually similar to the following code:
     *
     * {@snippet lang="java" :
     *     zoom.concatenate(change);
     *     fireZoomChanged(change);
     *     repaint(getZoomableBounds(null));
     *     }
     *
     * If {@code change} is the identity transform, then this method does nothing and listeners are not notified.
     *
     * @param  change  the zoom change as an affine transform in logical coordinates.
     */
    public void transform(final AffineTransform change) {
        if (!change.isIdentity()) {
            zoom.concatenate(change);
            roundIfAlmostInteger(zoom);
            fireZoomChanged(change);
            if (!disableRepaint) {
                repaint(getZoomableBounds());
            }
            zoomIsReset = false;
        }
    }

    /**
     * Changes the {@linkplain #zoom} by applying an affine transform. The {@code change} transform
     * must express a change in pixel units, for example a scrolling of 6 pixels toward right.
     * This method is conceptually similar to the following code:
     *
     * {@snippet lang="java" :
     *     zoom.preConcatenate(change);
     *     // Converts the change from pixel to logical units
     *     AffineTransform logical = zoom.createInverse();
     *     logical.concatenate(change);
     *     logical.concatenate(zoom);
     *     fireZoomChanged(logical);
     *     repaint(getZoomableBounds(null));
     *     }
     *
     * If {@code change} is the identity transform, then this method does nothing and listeners are not notified.
     *
     * @param  change  the zoom change, as an affine transform in pixel coordinates.
     */
    public void transformPixels(final AffineTransform change) {
        if (!change.isIdentity()) {
            final AffineTransform logical;
            try {
                logical = zoom.createInverse();
            } catch (NoninvertibleTransformException exception) {
                throw new IllegalStateException(exception);
            }
            logical.concatenate(change);
            logical.concatenate(zoom);
            roundIfAlmostInteger(logical);
            transform(logical);
        }
    }

    /**
     * Applies a zoom, translation or rotation on the {@code ZoomPane} content.
     * The type of operation depends on the {@code operation} argument:
     *
     * <ul>
     *   <li>{@link #TRANSLATE_X} applies a translation along the <var>x</var> axis.
     *       The {@code amount} argument specifies the translation in number of pixels.
     *       A negative value moves to the left whilst a positive value moves to the right.</li>
     *   <li>{@link #TRANSLATE_Y} applies a translation along the <var>y</var> axis.
     *       The {@code amount} argument specifies the translation in number of pixels.
     *       A negative value moves upwards whilst a positive value moves downwards.</li>
     *   <li>{@link #UNIFORM_SCALE} applies a zoom. The {@code amount} argument specifies the
     *       type of zoom to perform. A value greater than 1 will perform a zoom in whilst a value
     *       between 0 and 1 will perform a zoom out.</li>
     *   <li>{@link #ROTATE} carries out a rotation.
     *       The {@code amount} argument specifies the rotation angle in radians.</li>
     *   <li>{@link #RESET} restore the zoom to a default scale, rotation and translation.
     *       This operation displays all, or almost all, the contents of {@code ZoomPane}.</li>
     *   <li>{@link #DEFAULT_ZOOM} applies a default zoom, close to the maximum zoom, which shows
     *       the details of the contents of {@code ZoomPane} but without enlarging them too much.</li>
     * </ul>
     *
     * @param  operation  type of operation to perform.
     * @param  amount     ({@link #TRANSLATE_X} and {@link #TRANSLATE_Y}) translation in pixels,
     *                    ({@link #SCALE_X} and {@link #SCALE_Y}) scale factor or
     *                    ({@link #ROTATE}) rotation angle in radians.
     *                    In other cases, this argument is ignored and can be {@link Double#NaN}.
     * @param  center     zoom center ({@link #SCALE_X} and {@link #SCALE_Y}) or
     *                    rotation center ({@link #ROTATE}), in pixel coordinates.
     *                    The {@code null} value indicates a default value, more often the window center.
     * @throws UnsupportedOperationException if the {@code operation} argument is not recognized.
     */
    private void transform(final int operation, final double amount, final Point2D center)
            throws UnsupportedOperationException
    {
        if ((operation & (RESET)) != 0) {
            /////////////////////
            ////    RESET    ////
            /////////////////////
            if ((operation & ~(RESET)) != 0) {
                throw new UnsupportedOperationException();
            }
            reset();
            return;
        }
        final AffineTransform change;
        try {
            change = zoom.createInverse();
        } catch (NoninvertibleTransformException exception) {
            unexpectedException("transform", exception);
            return;
        }
        if ((operation & (TRANSLATE_X | TRANSLATE_Y)) != 0) {
            /////////////////////////
            ////    TRANSLATE    ////
            /////////////////////////
            if ((operation & ~(TRANSLATE_X | TRANSLATE_Y)) != 0) {
                throw new UnsupportedOperationException();
            }
            change.translate(((operation & TRANSLATE_X) != 0) ? amount : 0,
                             ((operation & TRANSLATE_Y) != 0) ? amount : 0);
        } else {
            /*
             * Gets the coordinates (in pixels) of the rotation or zoom center.
             */
            final double centerX;
            final double centerY;
            if (center != null) {
                centerX = center.getX();
                centerY = center.getY();
            } else {
                final Rectangle bounds = getZoomableBounds();
                if (bounds.width >= 0 && bounds.height >= 0) {
                    centerX = bounds.getCenterX();
                    centerY = bounds.getCenterY();
                } else {
                    return;
                }
                /*
                 * Zero lengths and widths are accepted. however if the rectangle is not valid
                 * (negative length or width) then the method will end without doing anything.
                 * No zoom will be performed.
                 */
            }
            if ((operation & (ROTATE)) != 0) {
                //////////////////////
                ////    ROTATE    ////
                //////////////////////
                if ((operation & ~(ROTATE)) != 0) {
                    throw new UnsupportedOperationException();
                }
                change.rotate(amount, centerX, centerY);
            } else if ((operation & (SCALE_X | SCALE_Y)) != 0) {
                /////////////////////
                ////    SCALE    ////
                /////////////////////
                if ((operation & ~(UNIFORM_SCALE)) != 0) {
                    throw new UnsupportedOperationException();
                }
                change.translate(+centerX, +centerY);
                change.scale(((operation & SCALE_X) != 0) ? amount : 1,
                             ((operation & SCALE_Y) != 0) ? amount : 1);
                change.translate(-centerX, -centerY);
            } else if ((operation & (DEFAULT_ZOOM)) != 0) {
                ////////////////////////////
                ////    DEFAULT_ZOOM    ////
                ////////////////////////////
                if ((operation & ~(DEFAULT_ZOOM)) != 0) {
                    throw new UnsupportedOperationException();
                }
                final Dimension2D size = getPreferredPixelSize();
                double sx = 1 / (size.getWidth()  * AffineTransforms2D.getScaleX0(zoom));
                double sy = 1 / (size.getHeight() * AffineTransforms2D.getScaleY0(zoom));
                if ((allowedActions & UNIFORM_SCALE) == UNIFORM_SCALE) {
                    if (sx > sy) sx = sy;
                    if (sy > sx) sy = sx;
                }
                if ((allowedActions & SCALE_X) == 0) sx = 1;
                if ((allowedActions & SCALE_Y) == 0) sy = 1;
                change.translate(+centerX, +centerY);
                change.scale    ( sx,       sy     );
                change.translate(-centerX, -centerY);
            } else {
                throw new UnsupportedOperationException();
            }
        }
        change.concatenate(zoom);
        roundIfAlmostInteger(change);
        transform(change);
    }

    /**
     * Adds an object to the list of objects interested in being notified about zoom changes.
     *
     * @param  listener  the change listener to add.
     */
    public void addZoomChangeListener(final ZoomChangeListener listener) {
        listenerList.add(ZoomChangeListener.class, listener);
    }

    /**
     * Removes an object from the list of objects interested in being notified about zoom changes.
     *
     * @param  listener  the change listener to remove.
     */
    public void removeZoomChangeListener(final ZoomChangeListener listener) {
        listenerList.remove(ZoomChangeListener.class, listener);
    }

    /**
     * Adds an object to the list of objects interested in being notified about mouse events.
     *
     * @param  listener  the mouse listener to add.
     */
    @Override
    public void addMouseListener(final MouseListener listener) {
        super.removeMouseListener(mouseSelectionTracker);
        super.addMouseListener   (listener);
        super.addMouseListener   (mouseSelectionTracker);               // MUST be last!
    }

    /**
     * Notifies all registered {@code ZoomListener}s that a zoom change occurred.
     * If {@code oldZoom} and {@code newZoom} are the affine transforms of the old and new zoom respectively,
     * the change can be computed in such a way that the following relation hold within rounding errors:
     *
     * {@snippet lang="java" :
     *     newZoom = oldZoom.concatenate(change)
     *     }
     *
     * <strong>Note: This method may modify the given {@code change} transform</strong> to combine several
     * consecutive {@code fireZoomChanged(…)} calls in a single transformation.
     *
     * @param change  affine transform which represents the change in the zoom.
     *                The given instance may be modified by this method call.
     */
    protected void fireZoomChanged(final AffineTransform change) {
        visibleArea.setRect(getVisibleArea());
        fireZoomChanged0(change);
    }

    /**
     * Notifies all registered {@code ZoomListener}s that a zoom change occurred.
     * Unlike the protected {@link #fireZoomChanged(AffineTransform)} method, this private method does not modify any
     * internal field and does not attempt to call other {@code ZoomPane} methods such as {@link #getVisibleArea()}.
     * This restriction avoid an infinite loop when this method is invoked by {@link #reset()}.
     */
    private void fireZoomChanged0(final AffineTransform change) {
        /*
         * Note: the event must be fired even if the transformation is the identity matrix,
         *       because some classes use it for updating scrollbars.
         */
        if (change == null) {
            throw new NullPointerException();
        }
        ZoomChangeEvent event = null;
        final Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length; (i -= 2) >= 0;) {
            if (listeners[i] == ZoomChangeListener.class) {
                if (event == null) {
                    event = new ZoomChangeEvent(this, change);
                }
                try {
                    ((ZoomChangeListener) listeners[i+1]).zoomChanged(event);
                } catch (RuntimeException exception) {
                    unexpectedException("fireZoomChanged", exception);
                }
            }
        }
    }

    /**
     * Invoked when user selected an area with the mouse.
     * The default implementation zooms to the selected {@code area}.
     * Subclasses can override this method in order to perform another action.
     *
     * @param  area  area selected by the user, in logical coordinates.
     */
    protected void mouseSelectionPerformed(final Shape area) {
        final Rectangle2D rect = (area instanceof Rectangle2D) ? (Rectangle2D) area : area.getBounds2D();
        if (isValid(rect)) {
            setVisibleArea(rect);
        }
    }

    /**
     * Returns the geometric shape to draw when user is delimitating an area. The shape is often a {@link Rectangle2D}
     * but could also be an {@link java.awt.geom.Ellipse2D} or some other kinds of shape. The important aspect is the
     * shape class and parameters not related to its position (e.g. arc size in a {@link RoundRectangle2D}).
     * The width, height, <var>x</var> and <var>y</var> coordinates will be ignored and overwritten.
     *
     * <p>The returned shape should be either an instance of {@link java.awt.geom.RectangularShape} or
     * {@link java.awt.geom.Line2D}. Other classes may cause a {@link ClassCastException} to be thrown.</p>
     *
     * <p>The default implementation returns a {@link Rectangle2D} instance.</p>
     *
     * @param  point  logical coordinates of the mouse at the moment the button is pressed.
     * @return shape as an instance of {@link java.awt.geom.RectangularShape} or {@link java.awt.geom.Line2D},
     *         or {@code null} for disabling selection by area.
     */
    protected Shape getMouseSelectionShape(final Point2D point) {
        return new Rectangle2D.Float();
    }

    /**
     * Indicates whether the magnifying glass is allowed to be shown on this component.
     * By default, it is allowed.
     *
     * @return {@code true} if the magnifying glass is allowed to be shown.
     */
    public boolean isMagnifierEnabled() {
        return magnifierEnabled;
    }

    /**
     * Specifies whether the magnifying glass is allowed to be shown on this component.
     * A {@code false} value hides the magnifying glass, removes the "Display magnifying glass"
     * choice from the contextual menu and causes
     * <code>{@linkplain #setMagnifierVisible setMagnifierVisible}(true)</code>
     * calls to be ignored.
     *
     * @param  enabled  whether magnifying glass is allowed to be show.
     */
    public void setMagnifierEnabled(final boolean enabled) {
        magnifierEnabled = enabled;
        navigationPopupMenu = null;
        if (!enabled) {
            setMagnifierVisible(false);
        }
    }

    /**
     * Indicates whether the magnifying glass is currently shown. By default, it is not visible.
     * Invoke {@link #setMagnifierVisible(boolean)} to make it appear.
     *
     * @return whether the magnifying glass is currently shown.
     */
    public boolean isMagnifierVisible() {
        return magnifier != null;
    }

    /**
     * Shows or hides the magnifying glass. If the magnifying glass is not yet shown and this method is invoked
     * with the {@code true} argument value, then the magnifying glass will appear at the window center.
     *
     * @param  visible  whether to show the magnifying glass.
     */
    public void setMagnifierVisible(final boolean visible) {
        setMagnifierVisible(visible, null);
    }

    /**
     * Returns the color with which to tint magnifying glass interior.
     *
     * @return the current color of magnifying glass interior.
     */
    public Paint getMagnifierGlass() {
        return magnifierGlass;
    }

    /**
     * Sets the color with which to tint magnifying glass interior.
     *
     * @param  color  the new color of magnifying glass interior.
     */
    public void setMagnifierGlass(final Paint color) {
        final Paint old = magnifierGlass;
        magnifierGlass = color;
        firePropertyChange("magnifierGlass", old, color);
    }

    /**
     * Returns the color of the magnifying glass border.
     *
     * @return the current color of the magnifying glass border.
     */
    public Paint getMagnifierBorder() {
        return magnifierBorder;
    }

    /**
     * Sets the color of the magnifying glass border.
     *
     * @param  color  the new color of the magnifying glass border.
     */
    public void setMagnifierBorder(final Paint color) {
        final Paint old = magnifierBorder;
        magnifierBorder = color;
        firePropertyChange("magnifierBorder", old, color);
    }

    /**
     * Returns the scale factor that has been applied on the {@link Graphics2D} before invoking
     * {@link #paintComponent(Graphics2D)}. This is always 1, except when painting the content
     * inside magnifier glass.
     */
    final double getGraphicsScale() {
        return (renderingType == IS_PAINTING_MAGNIFIER) ? magnifierPower : 1;
    }

    /**
     * Corrects a pixel coordinates for removing the effect of the magnifying glass. Without this
     * method, transformations from pixels to geographic coordinates would not give accurate results
     * for pixels inside the magnifying glass because the glass moves the apparent pixel position.
     * Invoking this method removes deformation effects using the following steps:
     *
     * <ul>
     *   <li>If the given pixel coordinates are outside the magnifying glass,
     *       then this method do nothing.</li>
     *   <li>Otherwise, this method update {@code point} in such a way that it contains the position
     *       that the same pixel would have in the absence of magnifying glass.</li>
     * </ul>
     *
     * @param point  on input, a pixel coordinates as it appears on the screen. On output, the
     *        coordinates that the same pixel would have if the magnifying glass was not presents.
     */
    @Override
    public void correctApparentPixelPosition(final Point2D point) {
        if (magnifier != null && magnifier.contains(point)) {
            final double centerX = magnifier.getCenterX();
            final double centerY = magnifier.getCenterY();
            /*
             * The following code is equivalent to the following transformations, which
             * must be identical to those which are applied in paintMagnifier(...).
             *
             *     translate(+centerX, +centerY);
             *     scale    (magnifierPower, magnifierPower);
             *     translate(-centerX, -centerY);
             *     inverseTransform(point, point);
             */
            point.setLocation((point.getX() - centerX) / magnifierPower + centerX,
                              (point.getY() - centerY) / magnifierPower + centerY);
        }
    }

    /**
     * Shows or hides the magnifying glass. The magnifying glass will be shown centered on the
     * specified coordinate if non-null, or in the screen center if {@code center} is null.
     *
     * @param visible  {@code true} to show the magnifying glass or {@code false} to hide it.
     * @param center   central coordinate for the magnifying glass.
     */
    private void setMagnifierVisible(final boolean visible, final Point center) {
        MouseReshapeTracker magnifier = this.magnifier;
        if (visible && magnifierEnabled) {
            if (magnifier == null) {
                Rectangle bounds = getZoomableBounds(); // Do not modify the Rectangle!
                if (bounds.isEmpty()) bounds = new Rectangle(0, 0, DEFAULT_SIZE, DEFAULT_SIZE);
                final int size = Math.min(Math.min(bounds.width, bounds.height), DEFAULT_MAGNIFIER_SIZE);
                final int x, y;
                if (center != null) {
                    x = center.x - size / 2;
                    y = center.y - size / 2;
                } else {
                    x = bounds.x + (bounds.width  - size) / 2;
                    y = bounds.y + (bounds.height - size) / 2;
                }
                this.magnifier = magnifier = new MouseReshapeTracker(new RoundRectangle2D.Float(x, y, size, size, 24, 24)) {
                    @Override protected void stateWillChange(final boolean isAdjusting) {repaintMagnifier();}
                    @Override protected void stateChanged   (final boolean isAdjusting) {repaintMagnifier();}
                };
                magnifier.setClip(bounds);
                magnifier.setAdjustable(SwingConstants.NORTH, true);
                magnifier.setAdjustable(SwingConstants.SOUTH, true);
                magnifier.setAdjustable(SwingConstants.EAST,  true);
                magnifier.setAdjustable(SwingConstants.WEST,  true);

                addMouseListener      (magnifier);
                addMouseMotionListener(magnifier);
                firePropertyChange("magnifierVisible", Boolean.FALSE, Boolean.TRUE);
                repaintMagnifier();
            } else if (center != null) {
                final Rectangle2D frame = magnifier.getFrame();
                final double width  = frame.getWidth();
                final double height = frame.getHeight();
                magnifier.setFrame(center.x - 0.5 * width,
                                   center.y - 0.5 * height, width, height);
            }
        } else if (magnifier != null) {
            repaintMagnifier();
            removeMouseMotionListener(magnifier);
            removeMouseListener      (magnifier);
            setCursor(null);
            this.magnifier = null;
            firePropertyChange("magnifierVisible", Boolean.TRUE, Boolean.FALSE);
        }
    }

    /**
     * Inserts navigation options to the specified menu. Default implementation adds menu items
     * such as "Zoom in" and "Zoom out" together with associated short-cut keys.
     *
     * @param  menu  the menu in which to add navigation options.
     */
    public void buildNavigationMenu(final JMenu menu) {
        buildNavigationMenu(menu, null);
    }

    /**
     * Implementation of {@link #buildNavigationMenu(JMenu)}.
     */
    private void buildNavigationMenu(final JMenu menu, final JPopupMenu popup) {
        int groupIndex = 0;
        boolean firstMenu = true;
        final ActionMap actionMap = getActionMap();
        for (int i=0; i<ACTION_ID.length; i++) {
            final Action action = actionMap.get(ACTION_ID[i]);
            if (action!=null && action.getValue(Action.NAME)!=null) {
                /*
                 * Checks whether the next item belongs to a new group.
                 * If this is the case, it will be necessary to add a separator before the next menu.
                 */
                final int lastGroupIndex = groupIndex;
                while ((ACTION_TYPE[i] & GROUP[groupIndex]) == 0) {
                    groupIndex = (groupIndex+1) % GROUP.length;
                    if (groupIndex == lastGroupIndex) {
                        break;
                    }
                }
                /*
                 * Adds an item to the menu.
                 */
                if (menu != null) {
                    if (groupIndex!=lastGroupIndex && !firstMenu) {
                        menu.addSeparator();
                    }
                    final JMenuItem item = new JMenuItem(action);
                    item.setAccelerator((KeyStroke) action.getValue(Action.ACCELERATOR_KEY));
                    menu.add(item);
                }
                if (popup != null) {
                    if (groupIndex!=lastGroupIndex && !firstMenu) {
                        popup.addSeparator();
                    }
                    final JMenuItem item = new JMenuItem(action);
                    item.setAccelerator((KeyStroke) action.getValue(Action.ACCELERATOR_KEY));
                    popup.add(item);
                }
                firstMenu = false;
            }
        }
    }

    /**
     * Menu with mouse coordinates where user clicked when this menu has been shown.
     */
    @SuppressWarnings("serial")
    private static final class PointPopupMenu extends JPopupMenu {
        /**
         * Coordinates of the point where user clicked.
         */
        public final Point point;

        /**
         * Creates a menu associated to the given mouse coordinates.
         */
        public PointPopupMenu(final Point point) {
            this.point = point;
        }
    }

    /**
     * Invoked when user clicks on the right mouse button.
     * The default implementation shows a contextual menu containing navigation options.
     *
     * @param  event  mouse event containing mouse coordinates in geographic coordinates
     *                together with pixel coordinates.
     * @return the contextual menu to show, or {@code null} if none.
     */
    protected JPopupMenu getPopupMenu(final MouseEvent event) {
        if (getZoomableBounds().contains(event.getX(), event.getY())) {
            if (navigationPopupMenu == null) {
                navigationPopupMenu = new PointPopupMenu(event.getPoint());
                if (magnifierEnabled) {
                    final Resources resources = Resources.forLocale(getLocale());
                    final JMenuItem item = new JMenuItem(
                            resources.getString(Resources.Keys.ShowMagnifier));
                    item.addActionListener((final ActionEvent event1) ->
                            setMagnifierVisible(true, navigationPopupMenu.point));
                    navigationPopupMenu.add(item);
                    navigationPopupMenu.addSeparator();
                }
                buildNavigationMenu(null, navigationPopupMenu);
            } else {
                navigationPopupMenu.point.x = event.getX();
                navigationPopupMenu.point.y = event.getY();
            }
            return navigationPopupMenu;
        } else {
            return null;
        }
    }

    /**
     * Invoked when user clicks on the right mouse button inside the magnifying glass.
     * The default implementation shows a contextual menu which contains magnifying glass options.
     *
     * @param  event  mouse event containing mouse coordinates in geographic coordinates
     *                together with pixel coordinates.
     * @return the contextual menu to show, or {@code null} if none.
     */
    protected JPopupMenu getMagnifierMenu(final MouseEvent event) {
        final Resources resources = Resources.forLocale(getLocale());
        final JPopupMenu menu = new JPopupMenu(resources.getString(Resources.Keys.Magnifier));
        final JMenuItem  item = new JMenuItem (resources.getString(Resources.Keys.Hide));
        item.addActionListener((final ActionEvent event1) -> setMagnifierVisible(false));
        menu.add(item);
        return menu;
    }

    /**
     * Shows the navigation contextual menu, provided the mouse event described the expected action.
     */
    private void mayShowPopupMenu(final MouseEvent event) {
        if (event.getID() == MouseEvent.MOUSE_PRESSED &&
                (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0)
        {
            requestFocus();
        }
        if (event.isPopupTrigger()) {
            final Point point      = event.getPoint();
            final JPopupMenu popup = (magnifier != null && magnifier.contains(point)) ?
                    getMagnifierMenu(event) : getPopupMenu(event);
            if (popup != null) {
                final Component source  = event.getComponent();
                final Window    window  = SwingUtilities.getWindowAncestor(source);
                if (window != null) {
                    final Toolkit   toolkit = source.getToolkit();
                    final Insets    insets  = toolkit.getScreenInsets(window.getGraphicsConfiguration());
                    final Dimension screen  = toolkit.getScreenSize();
                    final Dimension size    = popup.getPreferredSize();
                    SwingUtilities.convertPointToScreen(point, source);
                    screen.width  -= (size.width  + insets.right);
                    screen.height -= (size.height + insets.bottom);
                    if (point.x > screen.width)  point.x = screen.width;
                    if (point.y > screen.height) point.y = screen.height;
                    if (point.x < insets.left)   point.x = insets.left;
                    if (point.y < insets.top)    point.y = insets.top;
                    SwingUtilities.convertPointFromScreen(point, source);
                    popup.show(source, point.x, point.y);
                }
            }
        }
    }

    /**
     * Invoked when user moves the mouse wheel.
     * This method performs a zoom centered on the mouse position.
     */
    private void mouseWheelMoved(final MouseWheelEvent event) {
        if (event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int rotation  = event.getUnitsToScroll();
            double scale  = 1 + (AMOUNT_SCALE - 1) * Math.abs(rotation);
            Point2D point = new Point2D.Double(event.getX(), event.getY());
            if (rotation > 0) {
                scale = 1 / scale;
            }
            if (magnifier != null && magnifier.contains(point)) {
                magnifierPower *= scale;
                repaintMagnifier();
            } else {
                correctApparentPixelPosition(point);
                transform(UNIFORM_SCALE & allowedActions, scale, point);
            }
            event.consume();
        }
    }

    /**
     * Invoked when component size or position changed.
     * The {@link #repaint()} method is not invoked because there is already a repaint command in the queue.
     * The {@link #transform(AffineTransform)} method is not invoked neither because the zoom has not really changed;
     * However, we still need to adjust the scrollbars.
     */
    private void processSizeEvent(final ComponentEvent event) {
        if (zoomIsReset || !isValid(visibleArea)) {
            disableRepaint = true;
            try {
                reset();
            } finally {
                disableRepaint = false;
            }
        }
        if (magnifier != null) {
            magnifier.setClip(getZoomableBounds());
        }
        final Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length; (i -= 2) >= 0;) {
            if (listeners[i] == ZoomChangeListener.class) {
                if (listeners[i + 1] instanceof Synchronizer) try {
                    ((ZoomChangeListener) listeners[i + 1]).zoomChanged(null);
                } catch (RuntimeException exception) {
                    unexpectedException("processSizeEvent", exception);
                }
            }
        }
    }

    /**
     * Returns an {@code ZoomPane} embedded in a component with scrollbars.
     *
     * @return a swing component showing this {@code ZoomPane} together with scrollbars.
     */
    public JComponent createScrollPane() {
        return new ScrollPane();
    }

    /**
     * Convenience method for getting a scrollbar model. Should actually be declared inside {@link ScrollPane},
     * but we are not allowed to declare static methods in non-static inner classes.
     */
    private static BoundedRangeModel getModel(final JScrollBar bar) {
        return (bar != null) ? bar.getModel() : null;
    }

    /**
     * The scroll panel for {@link ZoomPane}. The standard {@link javax.swing.JScrollPane}
     * class is not used because it is difficult to get {@link javax.swing.JViewport} to
     * interact with transformations already handled by {@link ZoomPane#zoom}.
     */
    @SuppressWarnings("serial")
    private final class ScrollPane extends JComponent implements PropertyChangeListener {
        /**
         * The horizontal scrollbar, or {@code null} if none.
         */
        private final JScrollBar scrollbarX;

        /**
         * The vertical scrollbar, or {@code null} if none.
         */
        private final JScrollBar scrollbarY;

        /**
         * Creates a scroll pane for the enclosing {@link ZoomPane}.
         */
        public ScrollPane() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            /*
             * Sets up the scrollbars.
             */
            if ((allowedActions & TRANSLATE_X) != 0) {
                scrollbarX = new JScrollBar(JScrollBar.HORIZONTAL);
                scrollbarX.setUnitIncrement ((int) (AMOUNT_TRANSLATE));
                scrollbarX.setBlockIncrement((int) (AMOUNT_TRANSLATE * ENHANCEMENT_FACTOR));
            } else {
                scrollbarX  = null;
            }
            if ((allowedActions & TRANSLATE_Y) != 0) {
                scrollbarY = new JScrollBar(JScrollBar.VERTICAL);
                scrollbarY.setUnitIncrement ((int) (AMOUNT_TRANSLATE));
                scrollbarY.setBlockIncrement((int) (AMOUNT_TRANSLATE * ENHANCEMENT_FACTOR));
            } else {
                scrollbarY  = null;
            }
            /*
             * Adds the scrollbars in the scroll pane.
             */
            final GridBagConstraints c = new GridBagConstraints();
            if (scrollbarX != null) {
                c.gridx = 0; c.weightx = 1;
                c.gridy = 1; c.weighty = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                add(scrollbarX, c);
            }
            if (scrollbarY != null) {
                c.gridx = 1; c.weightx = 0;
                c.gridy = 0; c.weighty = 1;
                c.fill = GridBagConstraints.VERTICAL;
                add(scrollbarY, c);
            }
            if (scrollbarX != null && scrollbarY != null) {
                final JComponent corner = new JPanel(false);
                c.gridx = 1; c.weightx = 0;
                c.gridy = 1; c.weighty = 0;
                c.fill = GridBagConstraints.BOTH;
                add(corner, c);
            }
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0; c.weightx = 1;
            c.gridy = 0; c.weighty = 1;
            add(ZoomPane.this, c);
        }

        /**
         * Invoked when this {@code ScrollPane} is added in a {@link Container}.
         * This method registers all required listeners.
         */
        @Override
        public void addNotify() {
            super.addNotify();
            tieModels(getModel(scrollbarX), getModel(scrollbarY));
            ZoomPane.this.addPropertyChangeListener("zoom.insets", this);
        }

        /**
         * Invoked when this {@code ScrollPane} is removed from a {@link Container}.
         * This method unregisters all listeners.
         */
        @Override
        public void removeNotify() {
            ZoomPane.this.removePropertyChangeListener("zoom.insets", this);
            untieModels(getModel(scrollbarX), getModel(scrollbarY));
            super.removeNotify();
        }

        /**
         * Invoked when the zoomable area changes.
         * This method adjust scrollbar insets for keeping scrollbars aligned with zoomable area.
         */
        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            final Insets old    = (Insets) event.getOldValue();
            final Insets insets = (Insets) event.getNewValue();
            final GridBagLayout layout = (GridBagLayout) getLayout();
            if (scrollbarX != null && (old.left != insets.left || old.right != insets.right)) {
                final GridBagConstraints c = layout.getConstraints(scrollbarX);
                c.insets.left  = insets.left;
                c.insets.right = insets.right;
                layout.setConstraints(scrollbarX, c);
                scrollbarX.invalidate();
            }
            if (scrollbarY != null && (old.top != insets.top || old.bottom != insets.bottom)) {
                final GridBagConstraints c = layout.getConstraints(scrollbarY);
                c.insets.top    = insets.top;
                c.insets.bottom = insets.bottom;
                layout.setConstraints(scrollbarY, c);
                scrollbarY.invalidate();
            }
        }
    }

    /**
     * Synchronizes the position and range of given models with the zoom position.
     * The <var>x</var> and <var>y</var> models are associated with horizontal and vertical scrollbars.
     * When a scrollbar position is adjusted, the zoom is adjusted accordingly.
     * Conversely when the zoom is modified, the scrollbars position and range are adjusted accordingly.
     *
     * @param  x  model of the horizontal scrollbar or {@code null} if none.
     * @param  y  model of the vertical scrollbar or {@code null} if none.
     */
    public void tieModels(final BoundedRangeModel x, final BoundedRangeModel y) {
        if (x != null || y != null) {
            final Synchronizer listener = new Synchronizer(x, y);
            addZoomChangeListener(listener);
            if (x != null) x.addChangeListener(listener);
            if (y != null) y.addChangeListener(listener);
        }
    }

    /**
     * Removes synchronization between specified <var>x</var> and <var>y</var> models and enclosing {@code ZoomPane}.
     * The {@link ChangeListener} and {@link ZoomChangeListener} objects that were created are deleted.
     *
     * @param  x  model of the horizontal scrollbar or {@code null} if none.
     * @param  y  model of the vertical scrollbar or {@code null} if none.
     */
    public void untieModels(final BoundedRangeModel x, final BoundedRangeModel y) {
        final EventListener[] listeners = getListeners(ZoomChangeListener.class);
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] instanceof Synchronizer) {
                final Synchronizer s = (Synchronizer) listeners[i];
                if (s.xm == x && s.ym == y) {
                    removeZoomChangeListener(s);
                    if (x != null) x.removeChangeListener(s);
                    if (y != null) y.removeChangeListener(s);
                }
            }
        }
    }

    /**
     * Object responsible for synchronizing a {@link javax.swing.JScrollPane} object with scrollbars.
     * Whilst not generally useful, it would be possible to synchronize several pairs of
     * {@link BoundedRangeModel} objects on one {@code ZoomPane} object.
     */
    private final class Synchronizer implements ChangeListener, ZoomChangeListener {
        /**
         * Model to synchronize with {@link ZoomPane}.
         */
        public final BoundedRangeModel xm, ym;

        /**
         * Indicates whether the scrollbars are being adjusted in response to {@link #zoomChanged}.
         * If this is the case, {@link #stateChanged} must not make any other adjustments.
         */
        private transient boolean isAdjusting;

        /**
         * Cached {@code ZoomPane} bounds. Used in order to avoid too many object allocations on the heap.
         */
        private transient Rectangle bounds;

        /**
         * Constructs an object which synchronizes a pair of {@link BoundedRangeModel} with {@link ZoomPane}.
         */
        public Synchronizer(final BoundedRangeModel xm, final BoundedRangeModel ym) {
            this.xm = xm;
            this.ym = ym;
        }

        /**
         * Invoked when the position of a scrollbars changed.
         */
        @Override
        public void stateChanged(final ChangeEvent event) {
            if (!isAdjusting) {
                final boolean valueIsAdjusting = ((BoundedRangeModel) event.getSource()).getValueIsAdjusting();
                if (paintingWhileAdjusting || !valueIsAdjusting) {
                    /*
                     * Scroll view coordinates are computed using the following steps:
                     *
                     *   1) Get the logical coordinates for the whole area.
                     *   2) Transform to pixel space using current zoom.
                     *   3) Clip to the scrollbar's position (in pixels).
                     *   4) Transform back to the logical space.
                     *   5) Set the visible area to the resulting rectangle.
                     */
                    Rectangle2D area = getArea();
                    if (isValid(area)) {
                        area = AffineTransforms2D.transform(zoom, area, null);
                        double x = area.getX();
                        double y = area.getY();
                        double width, height;
                        if (xm != null) {
                            x    += xm.getValue();
                            width = xm.getExtent();
                        } else {
                            width = area.getWidth();
                        }
                        if (ym != null) {
                            y     += ym.getValue();
                            height = ym.getExtent();
                        } else {
                            height = area.getHeight();
                        }
                        area.setRect(x, y, width, height);
                        bounds = getBounds(bounds);
                        try {
                            area = AffineTransforms2D.inverseTransform(zoom, area, area);
                            try {
                                isAdjusting = true;
                                transform(setVisibleArea(area, bounds=getBounds(bounds), 0));
                            } finally {
                                isAdjusting = false;
                            }
                        } catch (NoninvertibleTransformException exception) {
                            unexpectedException("stateChanged", exception);
                        }
                    }
                }
                if (!valueIsAdjusting) {
                    zoomChanged(null);
                }
            }
        }

        /**
         * Invoked when the zoom changes.
         *
         * @param  change  ignored. Can be null.
         */
        @Override
        public void zoomChanged(final ZoomChangeEvent change) {
            if (!isAdjusting) {
                Rectangle2D area = getArea();
                if (isValid(area)) {
                    area = AffineTransforms2D.transform(zoom, area, null);
                    try {
                        isAdjusting = true;
                        setRangeProperties(xm, area.getX(), getWidth(),  area.getWidth());
                        setRangeProperties(ym, area.getY(), getHeight(), area.getHeight());
                    }
                    finally {
                        isAdjusting = false;
                    }
                }
            }
        }
    }

    /**
     * Adjusts the values of a model. The minimums and maximum values are adjusted as needed in order to include
     * the given value and its range. This adjustment is necessary for avoiding chaotic behavior when suer drags
     * the shape whilst a part of the graphic is outside the region initially specified by {@link #getArea()}.
     */
    private static void setRangeProperties(final BoundedRangeModel model,
            final double value, final int extent, final double max)
    {
        if (model != null) {
            final int pos = (int) Math.round(-value);
            model.setRangeProperties(pos, extent, Math.min(0, pos),
                    Math.max((int) Math.round(max), pos + extent), false);
        }
    }

    /**
     * Modifies the position in pixels of the {@code ZoomPane} visible part. {@code viewSize} is the size (in pixels)
     * that {@code ZoomPane} would have if its visible area covered the whole region given by {@link #getArea()} with
     * current zoom (Note: {@code viewSize} can be obtained by {@link #getPreferredSize()}
     * if {@link #setPreferredSize(Dimension)} has not been invoked with a non-null value).
     * Therefore, by definition, the conversion in pixel space of the region given by {@link #getArea()}
     * would be <code>bounds = Rectangle(0, 0, viewSize.width, viewSize.height)</code>.
     *
     * <p>This {@code scrollRectToVisible(…)} method allows us to define the {@code bounds} sub-region
     * to show in the {@code ZoomPane} window.</p>
     *
     * @param  rect  the region to be made visible.
     */
    @Override
    public void scrollRectToVisible(final Rectangle rect) {
        Rectangle2D area = getArea();
        if (isValid(area)) {
            area = AffineTransforms2D.transform(zoom, area, null);
            area.setRect(area.getX() + rect.getX(), area.getY() + rect.getY(),
                         rect.getWidth(), rect.getHeight());
            try {
                setVisibleArea(AffineTransforms2D.inverseTransform(zoom, area, area));
            } catch (NoninvertibleTransformException exception) {
                unexpectedException("scrollRectToVisible", exception);
            }
        }
    }

    /**
     * Indicates whether this {@code ZoomPane} should be repainted when the user is still adjusting scrollbar slider.
     * The scrollbars (or other models) are those which have been synchronized with this {@code ZoomPane} object by a
     * call to the {@link #tieModels(BoundedRangeModel, BoundedRangeModel)} method. The default value is {@code true},
     *
     * @return {@code true} if the zoom pane is painted while the user is scrolling.
     */
    public boolean isPaintingWhileAdjusting() {
        return paintingWhileAdjusting;
    }

    /**
     * Defines whether this {@code ZoomPane} should repaint its content when the user moves the scrollbar slider.
     * A fast computer is recommended if this flag is to be set to {@code true}.
     *
     * @param  flag  {@code true} if the zoom pane should be painted while the user is scrolling.
     */
    public void setPaintingWhileAdjusting(final boolean flag) {
        paintingWhileAdjusting = flag;
    }

    /**
     * Notifies that a part of this pane needs to be repainted. This method overrides the method
     * of the parent class for taking in account the case where the magnifying glass is shown.
     */
    @Override
    public void repaint(final long tm, final int x, final int y, final int width, final int height) {
        super.repaint(tm, x, y, width, height);
        if (magnifier != null && magnifier.intersects(x, y, width, height)) {
            /*
             * If the part to paint is inside the magnifying glass, the zoom applied by the
             * glass implies that we have to repaint a little more than that was requested.
             */
            repaintMagnifier();
        }
    }

    /**
     * Notifies that the magnifying glass needs to be repainted. A {@link #repaint()} action is performed
     * with the bounds of the magnifying glass as coordinates (taking into account its outline).
     */
    private void repaintMagnifier() {
        final Rectangle bounds = magnifier.getBounds();
        bounds.x      -= 4;
        bounds.y      -= 4;
        bounds.width  += 8;
        bounds.height += 8;
        super.repaint(0, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Paints the magnifying glass. This method is invoked after {@link #paintComponent(Graphics2D)}
     * if a magnifying glass is visible.
     *
     * @param  graphics  the graphics where to paint the magnifying glass.
     */
    protected void paintMagnifier(final Graphics2D graphics) {
        final double centerX = magnifier.getCenterX();
        final double centerY = magnifier.getCenterY();
        final Stroke  stroke =  graphics.getStroke();
        final Paint    paint =  graphics.getPaint();
        graphics.setStroke(new BasicStroke(6));
        graphics.setPaint (magnifierBorder);
        graphics.draw     (magnifier);
        graphics.setStroke(stroke);
        graphics.clip     (magnifier);                  // Coordinates in pixels.
        graphics.setPaint (magnifierGlass);
        graphics.fill     (magnifier.getBounds2D());
        graphics.setPaint (paint);
        graphics.translate(+centerX, +centerY);
        graphics.scale    (magnifierPower, magnifierPower);
        graphics.translate(-centerX, -centerY);
        /*
         * Note: the transformations performed here must be identical to those performed in pixelToLogical(…).
         */
        paintComponent(graphics);
    }

    /**
     * Paints this component. Subclass must override this method in order to draw the {@code ZoomPane} content.
     * For most implementations, the first line in this method will be <code>graphics.transform({@linkplain #zoom})</code>.
     *
     * @param  graphics  the graphics where to paint this component.
     */
    protected abstract void paintComponent(final Graphics2D graphics);

    /**
     * Prints this component. The default implementation invokes {@link #paintComponent(Graphics2D)}.
     *
     * @param  graphics  the graphics where to print this component.
     */
    protected void printComponent(final Graphics2D graphics) {
        paintComponent(graphics);
    }

    /**
     * Paints this component. This method is declared final in order to avoid unintentional overriding.
     * Override {@link #paintComponent(Graphics2D)} instead.
     *
     * @param  graphics  the graphics where to paint this component.
     */
    @Override
    protected final void paintComponent(final Graphics graphics) {
        renderingType = IS_PAINTING;
        super.paintComponent(graphics);
        /*
         * The JComponent.paintComponent(…) method creates a temporary Graphics2D object,
         * then calls ComponentUI.update(…) with that graphic as a parameter. This method
         * clears the screen background then calls ComponentUI.paint(…). This last method
         * has been redefined above (our {@link #UI} object) in such a way that it calls
         * itself paintComponent(Graphics2D).
         */
        if (magnifier != null) {
            renderingType = IS_PAINTING_MAGNIFIER;
            super.paintComponent(graphics);
        }
    }

    /**
     * Prints this component. This method is declared final in order to avoid unintentional overriding.
     * Override {@link #printComponent(Graphics2D)} instead.
     *
     * @param  graphics  the graphics where to print this component.
     */
    @Override
    protected final void printComponent(final Graphics graphics) {
        renderingType = IS_PRINTING;
        super.paintComponent(graphics);
        /*
         * Do not invoke `super.printComponent(…)` because we do not want above `paintComponent(…)` to be invoked.
         */
    }

    /**
     * Returns the size (in pixels) that {@code ZoomPane} would have if it was showing the whole region
     * specified by {@link #getArea()} with the current zoom ({@link #zoom}). This method is useful for
     * determining the maximum values to assign to the scrollbars.
     * For example, the horizontal bar could cover the {@code [0..viewSize.width]} range
     * whilst the vertical bar could cover the {@code [0..viewSize.height]} range.
     */
    private Dimension getViewSize() {
        if (!visibleArea.isEmpty()) {
            Rectangle2D area = getArea();
            if (isValid(area)) {
                area = AffineTransforms2D.transform(zoom, area, null);
                return new Dimension((int) Math.rint(area.getWidth()),
                                     (int) Math.rint(area.getHeight()));
            }
            return getSize();
        }
        return new Dimension(DEFAULT_SIZE, DEFAULT_SIZE);
    }

    /**
     * Returns the insets of this component.
     * If different insets are desired, override {@link #getInsets(Insets)} instead of this method.
     */
    @Override
    public final Insets getInsets() {
        return getInsets(null);
    }

    /**
     * Notifies this {@code ZoomPane} that the GUI has changed. Users should not call this method directly.
     */
    @Override
    public void updateUI() {
        navigationPopupMenu = null;
        super.updateUI();
        setUI(UI);
    }

    /**
     * Invoked when an affine transform cannot be inverted.
     * Current implementation logs the stack trace and resets the zoom.
     *
     * @param  methodName  the caller method name.
     * @param  exception   the exception to log.
     */
    private void unexpectedException(String methodName, NoninvertibleTransformException exception) {
        zoom.setToIdentity();
        unexpectedException(methodName, (Exception) exception);
    }

    /**
     * Invoked when an unexpected exception occurs.
     * Current implementation logs the stack trace.
     *
     * @param  methodName  the caller's method name.
     * @param  exception   the exception to log.
     */
    private static void unexpectedException(String methodName, Exception exception) {
        Logging.unexpectedException(null, ZoomPane.class, methodName, exception);
    }

    /**
     * Prints a message saying "Area:" with coordinates of given rectangle.
     * This is used for debugging purposes only.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void debug(final String methodName, final Rectangle2D area) {
        if (DEBUG) {
            System.out.println(methodName + " area: "
                    + "x=[" + area.getMinX() + " … " + area.getMaxX() + "], "
                    + "y=[" + area.getMinY() + " … " + area.getMaxY() + ']');
        }
    }

    /**
     * Verifies whether the given rectangle is valid. The rectangle is considered invalid if
     * its length or width is less than or equals to 0, or if a coordinate is infinite or NaN.
     */
    private static boolean isValid(final Rectangle2D rect) {
        if (rect == null) {
            return false;
        }
        final double x = rect.getX();
        final double y = rect.getY();
        final double w = rect.getWidth();
        final double h = rect.getHeight();
        return (x > Double.NEGATIVE_INFINITY && x < Double.POSITIVE_INFINITY &&
                y > Double.NEGATIVE_INFINITY && y < Double.POSITIVE_INFINITY &&
                w > 0                        && w < Double.POSITIVE_INFINITY &&
                h > 0                        && h < Double.POSITIVE_INFINITY);
    }

    /**
     * If scale and shear coefficients are close to integers, replaces their current values by their rounded values.
     * The scale and shear coefficients are handled in a "all or nothing" way; either all of them or none are rounded.
     * The translation terms are handled separately, provided that the scale and shear coefficients have been rounded.
     *
     * <p>This rounding up is useful for example in order to speedup image renderings.</p>
     *
     * @param  tr  the transform to round. Rounding will be applied in place.
     */
    private static void roundIfAlmostInteger(final AffineTransform tr) {
        double r;
        final double m00, m01, m10, m11;
        if (abs((m00 = rint(r=tr.getScaleX())) - r) <= EPS &&
            abs((m01 = rint(r=tr.getShearX())) - r) <= EPS &&
            abs((m11 = rint(r=tr.getScaleY())) - r) <= EPS &&
            abs((m10 = rint(r=tr.getShearY())) - r) <= EPS)
        {
            /*
             * At this point the scale and shear coefficients can be rounded to integers.
             * Continue only if this rounding does not make the transform non-invertible.
             */
            if ((m00!=0 || m01!=0) && (m10!=0 || m11!=0)) {
                double m02, m12;
                if (abs((r = rint(m02=tr.getTranslateX())) - m02) <= EPS) m02=r;
                if (abs((r = rint(m12=tr.getTranslateY())) - m12) <= EPS) m12=r;
                tr.setTransform(m00, m10, m01, m11, m02, m12);
            }
        }
    }
}
