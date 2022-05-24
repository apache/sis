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
package org.apache.sis.gui.map;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.measure.Unit;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextAlignment;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.portrayal.RenderException;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.gui.Widget;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.GUIUtilities;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;


/**
 * A status bar showing geographic or projected coordinates under mouse cursor.
 * The number of fraction digits is adjusted according pixel resolution for each coordinate to format.
 * Other components such as error message may also be shown.
 *
 * <p>Since the main {@code StatusBar} job is to listen to mouse events for updating coordinates,
 * this class implements {@link EventHandler} directly. {@code StatusBar} can be registered as a listener
 * using the following methods:</p>
 *
 * <ul>
 *   <li>{@link javafx.scene.Node#setOnMouseEntered(EventHandler)} for showing the coordinate values
 *     when the mouse enters the region of interest.</li>
 *   <li>{@link javafx.scene.Node#setOnMouseExited(EventHandler)} for hiding the coordinate values
 *     when the mouse exits the region of interest.</li>
 *   <li>{@link javafx.scene.Node#setOnMouseMoved(EventHandler)} for updating the coordinate values
 *     when the mouse moves inside the region of interest.</li>
 * </ul>
 *
 * Alternatively users can omit some or all above listener registrations and invoke
 * {@link #setLocalCoordinates(double, double)} explicitly instead.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
public class StatusBar extends Widget implements EventHandler<MouseEvent> {
    /**
     * The {@value} value, for identifying code that assume two-dimensional objects.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * Some spaces to add around the status bar.
     */
    private static final Insets PADDING = new Insets(5, Styles.SCROLLBAR_WIDTH, 6, 9);

    /**
     * An arbitrary increase in size of the text field where sample values are shown.
     * This is in case {@link #computeSizeOfSampleValues(String, Iterable)} underestimates the required size.
     */
    private static final int VALUES_PADDING = 9;

    /**
     * The container of controls making the status bar.
     */
    private final HBox view;

    /**
     * Message to write in the middle of the status bar.
     * This component usually has nothing to show; it is used mostly for error messages.
     * It takes all the space before {@link #position}.
     */
    private final Label message;

    /**
     * Local coordinates currently formatted in the {@link #position} field.
     * This is used for detecting if coordinate values changed since last formatting.
     * Those coordinates are often integer values.
     */
    private double lastX, lastY;

    /**
     * The canvas that this status bar is tracking. The value is {@code null} if this status bar is not associated
     * to a canvas. If non-null, this {@code StatusBar} will show coordinates (usually geographic or projected) of
     * mouse cursor position when the mouse is over that canvas.
     *
     * <p>Note that if this field is non-null, then the {@link #localToObjectiveCRS} property value may be overwritten
     * at any time, for example every time that a gesture event such as pan, zoom or rotation happens.</p>
     */
    private final MapCanvas canvas;

    /**
     * The manager of reference systems chosen by user, or {@code null} if none.
     * The {@link RecentReferenceSystems#areaOfInterest} property is used for
     * computing {@link #objectiveToPositionCRS}.
     */
    private final RecentReferenceSystems systemChooser;

    /**
     * The selected reference system, or {@code null} if there is no such property. This property is provided
     * by {@link RecentReferenceSystems}. It usually has the same value than {@link #positionReferenceSystem},
     * but the values may temporarily differ between the time a CRS is selected and when it became applied.
     *
     * @see #positionReferenceSystem
     */
    private final ObjectProperty<ReferenceSystem> selectedSystem;

    /**
     * The reference system used for rendering the data for which this status bar is providing cursor coordinates.
     * This is the "{@linkplain RecentReferenceSystems#setPreferred(boolean, ReferenceSystem) preferred}" or native
     * data CRS. It may not be the same than the CRS of coordinates actually shown in the status bar.
     *
     * @see MapCanvas#getObjectiveCRS()
     */
    private CoordinateReferenceSystem objectiveCRS;

    /**
     * The transform from <cite>objective CRS</cite> to the CRS of coordinates shown in this status bar.
     * The {@linkplain CoordinateOperation#getSourceCRS() source CRS} is {@link #objectiveCRS} and
     * the {@linkplain CoordinateOperation#getTargetCRS() target CRS} is {@link CoordinateFormat#getDefaultCRS()}.
     * This transform may be null if there is no CRS change to apply
     * (in which case {@link #localToPositionCRS} is the same instance than {@link #localToObjectiveCRS})
     * or if the target is not a CRS (for example it may be a Military Grid Reference System (MGRS) code).
     *
     * @see #updateLocalToPositionCRS()
     */
    private MathTransform objectiveToPositionCRS;

    /**
     * Conversion from local coordinates to geographic or projected coordinates of rendered data.
     * The local coordinates are the coordinates of the JavaFX view, as given for example in {@link MouseEvent}
     * The objective coordinates are geographic or projected coordinates of rendered data, ignoring all CRS changes
     * that may result from user selecting a different CRS in the contextual menu. Consequently while this transform
     * is often the conversion from pixel coordinates to the coordinates shown in this status bar,
     * this is not always the case.
     *
     * <p>This transform shall never be null. It is initially an identity transform and is modified by
     * {@link #applyCanvasGeometry(GridGeometry)}. The transform is usually (but not necessarily) affine
     * and should have no {@linkplain CoordinateOperation#getCoordinateOperationAccuracy() inaccuracy}
     * (ignoring rounding error). This is normally the inverse of {@linkplain #canvas}
     * {@linkplain MapCanvas#getObjectiveToDisplay() objective to display} transform,
     * but temporary mismatches may exist during gesture events such as pans, zooms and rotations.</p>
     *
     * <p>If this transform is set to a new value, the given transform must have the same number of source
     * and target dimensions than the previous value (if a change in the number of dimension is desired,
     * use {@link #applyCanvasGeometry(GridGeometry)} instead). The status bar is updated as if the new
     * conversion was applied <em>before</em> any CRS changes resulting from user selecting a different
     * CRS in the contextual menu. Note however that any specified transform may be overwritten if some
     * {@linkplain #canvas} gesture events happen later; setting an explicit transform is more useful
     * when this {@code StatusBar} is <em>not</em> associated to a {@link MapCanvas}
     * (for example it may be used with a {@link org.apache.sis.gui.coverage.GridView} instead).</p>
     *
     * <div class="note"><b>API note:</b>
     * We do not provide getter/setter for this property; use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.</div>
     *
     * @see MapCanvas#getObjectiveCRS()
     * @see MapCanvas#getObjectiveToDisplay()
     */
    public final ObjectProperty<MathTransform> localToObjectiveCRS;

    /**
     * The reference systems used by the coordinates shown in this status bar.
     * This is initially the <cite>objective CRS</cite>, but may become different
     * if the user selects another reference system through contextual menu.
     *
     * <div class="note"><b>API note:</b>
     * We do not provide getter method for this property; use {@link ReadOnlyObjectProperty#get()}
     * directly instead. We omit the "Property" suffix for making this operation more natural.</div>
     *
     * @see #position
     */
    public final ReadOnlyObjectProperty<ReferenceSystem> positionReferenceSystem;

    /**
     * Conversion from local coordinates to geographic or projected coordinates shown in this status bar.
     * This is the concatenation of {@link #localToObjectiveCRS} with {@link #objectiveToPositionCRS} transform.
     * The result is a transform to the user-selected CRS for coordinates shown in the status bar.
     * This conversion shall never be null but may be the identity transform.
     * It is usually non-affine if the display CRS is not the same than the objective CRS.
     * This transform may have a {@linkplain CoordinateOperation#getCoordinateOperationAccuracy() limited accuracy}.
     *
     * <p>The target CRS can be obtained by {@link CoordinateOperation#getTargetCRS()} on
     * {@link #objectiveToPositionCRS} or by {@link CoordinateFormat#getDefaultCRS()}.</p>
     *
     * @see #updateLocalToPositionCRS()
     */
    private MathTransform localToPositionCRS;

    /**
     * If non-null, determines if {@link #apply(GridGeometry)} needs to update {@link #localToPositionCRS} with a
     * potentially costly search for coordinate operation even in context where it would normally not be required.
     * An explanation of the context when it may happen is given in {@link OperationFinder#dataGeometry}.
     * This is rarely needed for most data (i.e. this field is almost always {@code null}).
     *
     * @see OperationFinder#fullOperationSearchRequired()
     */
    private Predicate<MapCanvas> fullOperationSearchRequired;

    /**
     * The source local indices before conversion to geospatial coordinates (never {@code null}).
     * The number of dimensions is often {@value #BIDIMENSIONAL}. May be the same array than
     * <code>{@linkplain #targetCoordinates}.coordinates</code> if both are two-dimensional
     * (if more than 2 dimensions, we need to avoid overwriting values in extra dimensions).
     *
     * @see #targetCoordinates
     * @see #position
     * @see #setTargetCRS(CoordinateReferenceSystem)
     */
    private double[] sourceCoordinates;

    /**
     * Coordinates after conversion to the CRS. The number of dimensions depends on the target CRS.
     * This object is reused during each coordinate transformation. Shall never be {@code null}.
     *
     * @see #sourceCoordinates
     * @see #position
     * @see #setPositionCRS(CoordinateReferenceSystem)
     */
    private GeneralDirectPosition targetCoordinates;

    /**
     * The desired precisions for each dimension in the {@link #targetCoordinates} to format.
     * It may vary for each position if the {@link #localToPositionCRS} transform is non-linear.
     * This array is initially {@code null} and created when first needed.
     */
    private double[] precisions;

    /**
     * A multiplication factory slightly greater than 1 applied on {@link #precisions}.
     * The intent is to avoid that a precision like 0.09999 is interpreted as requiring
     * two decimal digits instead of one. For avoiding that, we add a small value to the
     * precision: <var>precision</var> += <var>precision</var> × ε, which we compact as
     * <var>precision</var> *= (1 + ε). The ε value is chosen to represent an increase
     * of no more than 0.5 pixel between the lower and upper indices of the grid.
     * This array may be {@code null} if it has not been computed.
     */
    private double[] inflatePrecisions;

    /**
     * The declared accuracy on ground, or {@code null} if unspecified.
     *
     * @see #getLowestAccuracy()
     * @see #setLowestAccuracy(Quantity)
     */
    private Quantity<Length> lowestAccuracy;

    /**
     * The object to use for formatting coordinate values.
     */
    private final CoordinateFormat format;

    /**
     * The label where to format the cursor position, either as coordinate values or other representations.
     * The text is usually the result of formatting coordinate values as numerical values,
     * but may also be other representations such as Military Grid Reference System (MGRS) codes.
     *
     * @see #positionReferenceSystem
     */
    protected final Label position;

    /**
     * Maximal length of {@linkplain #position} text found so far. This is used for detecting when
     * to compute a minimal {@linkplain #position} width for making sure that the coordinates stay
     * visible even when an error message is shown on the left.
     */
    private int maximalPositionLength;

    /**
     * The {@link #position} text to show when the mouse is outside the canvas area.
     * This text is set to the axis abbreviations, for example "(φ, λ)".
     *
     * @see #setFormatCRS(CoordinateReferenceSystem, Quantity)
     */
    private String outsideText;

    /**
     * The label where to format the sample value(s) below cursor position, or {@code null} if none.
     *
     * @see #isSampleValuesVisible
     * @see #setSampleValuesVisible(boolean)
     */
    private Label sampleValues;

    /**
     * The object providing sample values under cursor position.
     * The property value may be {@code null} if there are no sample values to format.
     * If non-null, the text provided by this object will appear at the right of the coordinates.
     *
     * <div class="note"><b>API note:</b>
     * We do not provide getter/setter for this property; use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.</div>
     */
    public final ObjectProperty<ValuesUnderCursor> sampleValuesProvider;

    /**
     * Whether the {@link #sampleValues} are visible.
     * This field is {@code true} only if all following conditions are met:
     *
     * <ul>
     *   <li>{@link #sampleValues} is non-null (it is created by {@link #setSampleValuesVisible(boolean)}).</li>
     *   <li>{@link #sampleValuesProvider} property value is non-null (it is set by user).</li>
     *   <li>{@link ValuesUnderCursor#isEmpty()} is {@code false}.</li>
     * </ul>
     *
     * @see #setSampleValuesVisible(boolean)
     */
    private boolean isSampleValuesVisible;

    /**
     * Creates a new status bar for showing coordinates of mouse cursor position in a canvas.
     * If the {@code canvas} argument is non-empty, this {@code StatusBar} will show coordinates
     * (usually geographic or projected) of mouse cursor position when the mouse is over that canvas.
     * Note that in such case, the {@link #localToObjectiveCRS} property value will be overwritten
     * at any time (for example every time that a gesture event such as pan, zoom or rotation happens).
     *
     * <p>If the {@code choices} argument is non-null, user will be able to select different CRS
     * using the contextual menu on the status bar.</p>
     *
     * <h4>Limitations</h4>
     * This constructor registers numerous listeners on {@code canvas} and {@code systemChooser}.
     * There is currently no unregistration mechanism. The {@code StatusBar} instance is expected
     * to exist as long as the {@code MapCanvas} and {@code RecentReferenceSystems} instances
     * given to this constructor.
     *
     * <p>Current implementation accepts only zero or one {@code MapCanvas}. A future implementation
     * may accept a larger amount of canvas for tracking many views with a single status bar
     * (for example images over the same area but at different times).</p>
     *
     * @param  systemChooser  the manager of reference systems chosen by user, or {@code null} if none.
     * @param  toTrack        the canvas that this status bar is tracking.
     *                        Currently restricted to an array of length 0 or 1.
     */
    public StatusBar(final RecentReferenceSystems systemChooser, final MapCanvas... toTrack) {
        positionReferenceSystem = new PositionSystem();
        localToObjectiveCRS     = new LocalToObjective();
        localToPositionCRS      = localToObjectiveCRS.get();
        targetCoordinates       = new GeneralDirectPosition(BIDIMENSIONAL);
        sourceCoordinates       = targetCoordinates.coordinates;
        lastX = lastY           = Double.NaN;
        format                  = new CoordinateFormat();

        message = new Label();
        message.setVisible(false);                      // Waiting for getting a message to display.
        message.setTextFill(Styles.ERROR_TEXT);
        message.setMaxWidth(Double.POSITIVE_INFINITY);
        HBox.setHgrow(message, Priority.ALWAYS);

        position = new Label();
        position.setAlignment(Pos.CENTER_RIGHT);
        position.setTextAlignment(TextAlignment.RIGHT);

        view = new HBox(18, message, position);
        view.setPadding(PADDING);
        view.setAlignment(Pos.CENTER_RIGHT);
        /*
         * Contextual menu can be invoked anywhere on the HBox; we do not register this menu
         * on `position` or `sampleValues` labels because those regions are relatively small.
         */
        final ContextMenu menu = new ContextMenu();
        view.setOnMousePressed((event) -> {
            if (event.isSecondaryButtonDown() && !menu.getItems().isEmpty()) {
                menu.show((HBox) event.getSource(), event.getScreenX(), event.getScreenY());
                event.consume();
            } else {
                menu.hide();
            }
        });
        /*
         * Create a contextual menu offering to user a choice of CRS in which to display the coordinates.
         * The CRS choices are controlled by `RecentReferenceSystems`. Selection of a new CRS causes the
         * `setPositionCRS(…)` method to be invoked.
         */
        this.systemChooser = systemChooser;
        if (systemChooser == null) {
            selectedSystem = null;
        } else {
            final Menu choices = systemChooser.createMenuItems((property, oldValue, newValue) -> {
                setPositionCRS(newValue instanceof CoordinateReferenceSystem ? (CoordinateReferenceSystem) newValue : null);
            });
            selectedSystem = RecentReferenceSystems.getSelectedProperty(choices);
            menu.getItems().add(choices);
            /*
             * Ensure (as much as possible) that the CRS of coordinates formatted in this status bar is one
             * of the CRSs in the list of choices offered to the user.  It happens often that the CRS given
             * to `applyCanvasGeometry(GridGeometry)` has (λ,φ) axis order but the CRS offered to user have
             * (φ,λ) axis order (because we try to comply with definitions following geographers practice).
             * In such case we will replace (λ,φ) by (φ,λ). Since we use the list of choices as the source
             * of desired CRS, we have to listen to new elements added to that list. This is necessary since
             * the list of often empty at construction time and filled later after a background thread task.
             */
            systemChooser.getItems().addListener((ListChangeListener.Change<? extends ReferenceSystem> change) -> {
                while (change.next()) {
                    if (change.wasAdded() || change.wasReplaced()) {
                        setReplaceablePositionCRS(format.getDefaultCRS());
                        break;
                    }
                }
            });
        }
        /*
         * Configure the property controlling the values shown on the right of cursor coordinates.
         * A default value will be provided if `canvas` is non-null. Changing this property causes
         * a contextual menu item to be added or removed.
         */
        final ObservableList<MenuItem> items = menu.getItems();
        sampleValuesProvider = new SimpleObjectProperty<>(this, "valueProvider");
        sampleValuesProvider.addListener((p,o,n) -> {
            ValuesUnderCursor.update(this, o, n);
            if (o != null) items.remove(o.valueChoices);
            if (n != null) items.add(0, n.valueChoices);
            setSampleValuesVisible(n != null && !n.isEmpty());
        });
        /*
         * If a canvas is specified, register listeners for mouse position, rendering events, errors, etc.
         * We do not allow the canvas to be changed after construction because of the added complexity
         * (e.g. we would have to remember all registered listeners so we can unregister them).
         */
        if (toTrack != null && toTrack.length != 0) {
            if (toTrack.length != 1) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.TooManyCollectionElements_3, "toTrack", 1, toTrack.length));
            }
            canvas = toTrack[0];
        } else {
            canvas = null;
        }
        if (canvas != null) {
            sampleValuesProvider.set(ValuesUnderCursor.create(canvas));
            canvas.errorProperty().addListener((p,o,n) -> setRenderingError(n));
            canvas.renderingProperty().addListener((p,o,n) -> {if (!n) applyCanvasGeometry();});
            applyCanvasGeometry();
            if (canvas.getObjectiveCRS() != null) {
                registerMouseListeners();
            } else {
                /*
                 * Wait for objective CRS to be known before to register listeners.
                 * The canvas "objective CRS" is null only for unitialized canvas.
                 * After the canvas has been initialized, it can not be null anymore.
                 * We delay listeners registration because if listeners were enabled
                 * on uninitialized canvas, the status bar would show irrelevant coordinates.
                 */
                canvas.addPropertyChangeListener(MapCanvas.OBJECTIVE_CRS_PROPERTY, new PropertyChangeListener() {
                    @Override public void propertyChange(final PropertyChangeEvent event) {
                        canvas.removePropertyChangeListener(MapCanvas.OBJECTIVE_CRS_PROPERTY, this);
                        registerMouseListeners();
                    }
                });
            }
        }
    }

    /**
     * Registers mouse listeners for the canvas after the objective CRS became known.
     * This method is invoked only once for the {@link StatusBar} instance.
     */
    private void registerMouseListeners() {
        final Pane floatingPane = canvas.floatingPane;
        floatingPane.addEventHandler(MouseEvent.MOUSE_ENTERED, this);
        floatingPane.addEventHandler(MouseEvent.MOUSE_EXITED,  this);
        floatingPane.addEventHandler(MouseEvent.MOUSE_MOVED,   this);
    }

    /**
     * Returns the node to add to the scene graph for showing the status bar.
     */
    @Override
    public final Region getView() {
        return view;
    }

    /**
     * Invoked when {@link MapCanvas} completed its rendering. This method sets
     * {@link StatusBar#localToObjectiveCRS} to the inverse of {@link MapCanvas#objectiveToDisplay}.
     * It assumes that even if the JavaFX local coordinates and {@link #localToPositionCRS} transform
     * changed, the "real world" coordinates under the mouse cursor is still the same. This assumption
     * should be true if this listener is notified as a result of zoom, translation or rotation events.
     */
    private void applyCanvasGeometry() {
        try {
            apply(canvas.getGridGeometry());
            /*
             * Do not hide `position` since we assume that "real world" coordinates are still valid.
             * Do not try to rewrite position neither since `lastX` and `lastY` are not valid anymore.
             */
        } catch (RenderException e) {
            setRenderingError(e);
        }
    }

    /**
     * Configures this status bar for showing coordinates in the CRS and with the resolution given
     * by the specified grid geometry. The geometry properties are applied as below:
     *
     * <ul>
     *   <li>{@link GridGeometry#getCoordinateReferenceSystem()} defines the CRS of the coordinates to format.</li>
     *   <li>{@link GridGeometry#getGridToCRS(PixelInCell) GridGeometry.getGridToCRS(PixelInCell.CELL_CENTER)}
     *       defines the conversion from coordinate values local to the canvas to coordinate values in the CRS
     *       (the {@linkplain #localToObjectiveCRS local to objective CRS} conversion).</li>
     *   <li>{@link GridGeometry#getExtent()} provides the view size in pixels, used for estimating a resolution.</li>
     *   <li>{@link GridGeometry#getResolution(boolean)} is also used for estimating a resolution.</li>
     * </ul>
     *
     * All above properties are optional. The "local to objective CRS" conversion can be updated
     * after this method call by setting the {@link #localToObjectiveCRS} property.
     *
     * @param  geometry  geometry of the coverage shown in {@link MapCanvas}, or {@code null}.
     *
     * @see MapCanvas#getGridGeometry()
     */
    public void applyCanvasGeometry(final GridGeometry geometry) {
        position.setText(null);
        apply(geometry);
    }

    /**
     * Implementation of {@link #applyCanvasGeometry(GridGeometry)} without changing {@link #position} visibility state.
     * Invoking this method usually invalidate the coordinates shown in this status bar. The new coordinates can not be
     * easily recomputed because the {@link #lastX} and {@link #lastY} values may not be valid anymore, as a result of
     * possible changes in JavaFX local coordinate system. Consequently the coordinates should be temporarily hidden
     * until a new {@link MouseEvent} gives us the new local coordinates, unless this method is invoked in a context
     * where we know that the "real world" coordinates should be the same even if local coordinates changed.
     *
     * @param  geometry  geometry of the coverage shown in {@link MapCanvas}, or {@code null}.
     */
    private void apply(final GridGeometry geometry) {
        /*
         * Compute values in local variables without modifying `StatusBar` fields for now.
         * The fields will be updated only after we know that this operation is successful.
         */
        MathTransform localToCRS = null;
        CoordinateReferenceSystem crs = null;
        double resolution = 1;
        double[] inflate = null;
        Unit<?> unit = Units.PIXEL;
        if (geometry != null) {
            if (geometry.isDefined(GridGeometry.CRS)) {
                crs = geometry.getCoordinateReferenceSystem();
            }
            if (geometry.isDefined(GridGeometry.GRID_TO_CRS)) {
                localToCRS = geometry.getGridToCRS(PixelInCell.CELL_CENTER);
            }
            /*
             * Compute the precision of coordinates to format. We use the finest resolution,
             * looking only at axes having the same units of measurement than the first axis.
             * This will be used as a fallback if we can not compute the precision specific
             * to a coordinate, for example if we can not compute the derivative.
             */
            if (geometry.isDefined(GridGeometry.RESOLUTION)) {
                double[] resolutions = geometry.getResolution(true);
                if (crs != null && resolutions.length != 0) {
                    final CoordinateSystem cs = crs.getCoordinateSystem();
                    unit = cs.getAxis(0).getUnit();
                    for (int i=0; i<resolutions.length; i++) {
                        if (unit.equals(cs.getAxis(i).getUnit())) {
                            final double r = resolutions[i];
                            if (r < resolution) resolution = r;
                        }
                    }
                }
            }
            /*
             * Add a tolerance factor of ½ pixel when computing the number of significant
             * fraction digits to show in coordinates.
             */
            if (geometry.isDefined(GridGeometry.EXTENT)) {
                final GridExtent extent = geometry.getExtent();
                final int n = extent.getDimension();
                inflate = new double[n];
                for (int i=0; i<n; i++) {
                    inflate[i] = (0.5 / extent.getSize(i)) + 1;
                }
            }
        }
        final boolean sameCRS = Utilities.equalsIgnoreMetadata(objectiveCRS, crs);
        if (localToCRS == null) {
            localToCRS = MathTransforms.identity(BIDIMENSIONAL);
        }
        final int srcDim = Math.max(localToCRS.getSourceDimensions(), BIDIMENSIONAL);
        final int tgtDim = localToCRS.getTargetDimensions();
        /*
         * Remaining code should not fail, so we can start modifying the `StatusBar` fields.
         * The buffers for source and target coordinates are recreated because the number of
         * dimensions may have changed. The `lastX` and `lastY` coordinates are local to the
         * JavaFX view and considered invalid  because they depend on the transforms applied
         * on JavaFX node, which may have changed together with `localToObjectiveCRS` change.
         * So we can not use those values for updating the coordinates shown in status bar.
         * Instead we will wait for the next mouse event to provide new local coordinates.
         */
        ((LocalToObjective) localToObjectiveCRS).setNoCheck(localToCRS);
        targetCoordinates   = new GeneralDirectPosition(tgtDim);
        sourceCoordinates   = new double[srcDim];
        objectiveCRS        = crs;
        localToPositionCRS  = localToCRS;                           // May be updated again below.
        inflatePrecisions   = inflate;
        precisions          = null;
        lastX = lastY       = Double.NaN;                           // Not valid anymove — see above block comment.
        if (sameCRS) {
            updateLocalToPositionCRS();
            // Keep the format CRS unchanged since we made `localToPositionCRS` consistent with its value.
            if (fullOperationSearchRequired != null && fullOperationSearchRequired.test(canvas)) {
                setPositionCRS(format.getDefaultCRS());
            }
        } else {
            objectiveToPositionCRS = null;
            setFormatCRS(crs, null);                                // Should be invoked before to set precision.
            crs = OperationFinder.toGeospatial(crs, canvas);
            crs = setReplaceablePositionCRS(crs);                   // May invoke setFormatCRS(…) after background work.
        }
        format.setGroundPrecision(Quantities.create(resolution, unit));
        /*
         * If the CRS changed, we may need to update the selected menu item. It happens when this method
         * is invoked because new data have been loaded, as opposed to this method being invoked after a
         * gesture event (zoom, pan, rotation).
         */
        if (!sameCRS && selectedSystem != null) {
            selectedSystem.set(crs);
        }
    }

    /**
     * Computes {@link #localToPositionCRS} after a change of {@link #localToObjectiveCRS}.
     * Other properties, in particular {@link #objectiveToPositionCRS}, must be valid.
     */
    private void updateLocalToPositionCRS() {
        if (objectiveToPositionCRS != null) {
            localToPositionCRS = MathTransforms.concatenate(localToObjectiveCRS.get(), objectiveToPositionCRS);
        }
        setTargetCRS(format.getDefaultCRS());
    }

    /**
     * Sets the CRS of {@link #targetCoordinates}.
     * This method creates a new position if the number of dimensions changed.
     */
    private void setTargetCRS(final CoordinateReferenceSystem crs) {
        final int tgtDim = ReferencingUtilities.getDimension(crs);
        if (tgtDim != 0 && tgtDim != targetCoordinates.getDimension()) {
            precisions = null;
            targetCoordinates = new GeneralDirectPosition(tgtDim);
        }
        targetCoordinates.setCoordinateReferenceSystem(crs);
    }

    /**
     * Sets the CRS of the position shown in this status bar after replacement by one of the available CRS
     * if a match is found. This method compares the given CRS with the list of choices before to delegate
     * to {@link #setPositionCRS(CoordinateReferenceSystem)} possibly with different axis order. A typical
     * scenario is {@link #apply(GridGeometry)} invoked with (<var>longitude</var>, <var>latitude</var>)
     * axis order, and this method swapping axes to standard (<var>latitude</var>, <var>longitude</var>)
     * axis order for coordinates display purpose.
     *
     * @param  crs  the new CRS (ignoring axis order), or {@code null} for {@link #objectiveCRS}.
     * @return the reference system actually used for formatting coordinates. It may have different axis order
     *         and units than the specified CRS. This is the CRS that {@link CoordinateFormat#getDefaultCRS()}
     *         will return a little bit later (pending completion of a background task).
     */
    private CoordinateReferenceSystem setReplaceablePositionCRS(CoordinateReferenceSystem crs) {
        if (crs != null && systemChooser != null) {
            final ComparisonMode mode = systemChooser.duplicationCriterion.get();
            for (final ReferenceSystem system : systemChooser.getItems()) {
                if (Utilities.deepEquals(crs, system, mode)) {
                    crs = (CoordinateReferenceSystem) system;       // Same CRS but possibly different axis order.
                    break;
                }
            }
        }
        if (crs != format.getDefaultCRS()) {
            setPositionCRS(crs);
        }
        return crs;
    }

    /**
     * Sets the coordinate reference system of the position shown in this status bar.
     * The change may not appear immediately after method return; this method may use a
     * background thread for computing the coordinate operation.  That task may be long
     * the first time that it is executed, but should be fast on subsequent invocations.
     *
     * @param  crs  the new CRS, or {@code null} for {@link #objectiveCRS}.
     */
    private void setPositionCRS(final CoordinateReferenceSystem crs) {
        if (crs != null && objectiveCRS != null && objectiveCRS != crs) {
            position.setTextFill(Styles.OUTDATED_TEXT);
            /*
             * Take snapshots of references to all objects that the background thread will use.
             * The background thread shall not read StatusBar fields directly since they may be
             * in the middle of changes at any time. All objects are assumed immutable.
             */
            final Envelope aoi = (systemChooser != null) ? systemChooser.areaOfInterest.get() : null;
            BackgroundThreads.execute(new OperationFinder(canvas, aoi, objectiveCRS, crs) {
                /**
                 * The accuracy to show on the status bar, or {@code null} if none.
                 * This is computed after {@link CoordinateOperation} has been determined.
                 */
                private Quantity<Length> accuracy;

                /**
                 * Invoked in a background thread for fetching transformation to target CRS.
                 * The potentially costly part is {@code CRS.findOperation(…)} in super.call().
                 */
                @Override protected MathTransform call() throws Exception {
                    final MathTransform value = super.call();
                    double a = CRS.getLinearAccuracy(getOperation());
                    if (a > 0) {
                        accuracy = GUIUtilities.shorter(null, a);
                    }
                    return value;
                }
                /**
                 * Invoked in JavaFX thread on success. The {@link StatusBar#localToPositionCRS} transform
                 * is set to the transform that we computed in background and the {@link CoordinateFormat}
                 * is configured with auxiliary information such as positional accuracy.
                 */
                @Override protected void succeeded() {
                    setPositionCRS(this, accuracy);
                }

                /**
                 * Invoked in JavaFX thread on failure. The previous CRS is kept unchanged but
                 * the coordinates will appear in red for telling user that there is a problem.
                 */
                @Override protected void failed() {
                    final Locale locale = getLocale();
                    setErrorMessage(Resources.forLocale(locale).getString(Resources.Keys.CanNotUseRefSys_1,
                                    IdentifiedObjects.getDisplayName(crs, locale)), getException());
                    selectedSystem.set(format.getDefaultCRS());
                    resetPositionCRS(Styles.ERROR_TEXT);
                }

                /** For logging purpose if a non-fatal error occurs. */
                @Override protected Class<?> getCallerClass()  {return StatusBar.class;}
                @Override protected String   getCallerMethod() {return "setPositionCRS";}
            });
        } else {
            /*
             * If the requested CRS is the objective CRS, avoid above costly operation.
             * The work that we need to do is to cancel the effect of `localToPositionCRS`.
             * As a special case if `objectiveCRS` was unknown before this method call,
             * set it to the given value. This is needed for initializing the format CRS
             * to the first reference system listed in `RecentReferenceSystems` choices.
             * We could not do this work at construction time because the CRS choices may
             * be computed in a background thread, in which case it became known only a
             * little bit later and given to `StatusBar` through listeners.
             */
            if (objectiveCRS == null) {
                objectiveCRS = crs;
            }
            position.setMinWidth(0);
            maximalPositionLength = 0;
            resetPositionCRS(Styles.NORMAL_TEXT);
        }
    }

    /**
     * Invoked after the background thread computed the new coordinate operation.
     * This method rewrites the coordinates on the assumption that {@link #lastX}
     * and {@link #lastY} are still valid. This assumption should be correct when
     * only the format CRS has been updated and not {@link #localToObjectiveCRS}.
     *
     * @param  finder    the completed task with the new {@link #objectiveToPositionCRS}.
     * @param  accuracy  the accuracy to show on the status bar, or {@code null} if none.
     */
    private void setPositionCRS(final OperationFinder finder, final Quantity<Length> accuracy) {
        setErrorMessage(null, null);
        setFormatCRS(finder.getTargetCRS(), accuracy);
        objectiveToPositionCRS = finder.getValue();
        fullOperationSearchRequired = finder.fullOperationSearchRequired();
        updateLocalToPositionCRS();
        position.setTextFill(Styles.NORMAL_TEXT);
        position.setMinWidth(0);
        maximalPositionLength = 0;
        if (isPositionVisible()) {
            final double x = lastX;
            final double y = lastY;
            lastX = lastY = Double.NaN;
            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                setLocalCoordinates(x, y);
            }
        }
    }

    /**
     * Sets the {@link CoordinateFormat} default CRS together with the tool tip text.
     * Caller is responsible to setup transforms ({@link #localToPositionCRS}, <i>etc</i>).
     * For the method that apply required changes on transforms before to set the format CRS,
     * see {@link #setPositionCRS(CoordinateReferenceSystem)}.
     *
     * @param  crs       the new {@link #format} reference system.
     * @param  accuracy  positional accuracy in the given CRS, or {@code null} if none.
     *
     * @see #positionReferenceSystem
     */
    private void setFormatCRS(final CoordinateReferenceSystem crs, final Quantity<Length> accuracy) {
        format.setDefaultCRS(crs);
        format.setGroundAccuracy(Quantities.max(accuracy, lowestAccuracy));
        String text = IdentifiedObjects.getDisplayName(crs, getLocale());
        Tooltip tp = null;
        if (text != null) {
            tp = position.getTooltip();
            if (tp == null) {
                tp = new Tooltip(text);
            } else {
                tp.setText(text);
            }
        }
        position.setTooltip(tp);
        /*
         * Prepare the text to show when the mouse is outside the canvas area.
         * We will write axis abbreviations, for example "(φ, λ)".
         */
        text = null;
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs != null) {                                               // Paranoiac check (should never be null).
                final int dimension = cs.getDimension();
                if (dimension > 0) {                                        // Paranoiac check (should never be zero).
                    final StringBuilder b = new StringBuilder().append('(');
                    for (int i=0; i<dimension; i++) {
                        if (i != 0) b.append(", ");
                        final CoordinateSystemAxis axis = cs.getAxis(i);
                        if (axis != null) {                                 // Paranoiac check (should never be null).
                            final String abbr = Strings.trimOrNull(axis.getAbbreviation());
                            if (abbr != null) {
                                b.append(abbr);
                                continue;
                            }
                        }
                        b.append('?');
                    }
                    b.append(')');
                    format.getGroundAccuracyText().ifPresent(b::append);
                    text = b.toString();
                }
            }
        }
        /*
         * If the mouse is already outside canvas area, update the `position` text now.
         * Otherwise `position` is probably showing coordinates, which we leave unchanged for now.
         */
        if (position.getText() == outsideText) {          // Identity comparison is okay for this value.
            position.setText(text);
        }
        outsideText = text;
        setTargetCRS(crs);
        ((PositionSystem) positionReferenceSystem).fireValueChangedEvent();
    }

    /**
     * Implementation of {@link #positionReferenceSystem} property.
     */
    private final class PositionSystem extends ReadOnlyObjectPropertyBase<ReferenceSystem> {
        @Override public Object          getBean()       {return StatusBar.this;}
        @Override public String          getName()       {return "positionReferenceSystem";}
        @Override public ReferenceSystem get()           {return format.getDefaultCRS();}
        @Override protected void fireValueChangedEvent() {super.fireValueChangedEvent();}
    }

    /**
     * Resets {@link #localToPositionCRS} to its default value. This is invoked either when the
     * target CRS is {@link #objectiveCRS}, or when an attempt to use another CRS failed.
     */
    private void resetPositionCRS(final Color textFill) {
        objectiveToPositionCRS = null;
        localToPositionCRS = localToObjectiveCRS.get();
        setFormatCRS(objectiveCRS, null);
        position.setTextFill(textFill);
    }

    /**
     * Implementation of {@link #localToObjectiveCRS} property performing argument check before to change its value.
     * When a new value is set, the given transform must have the same number of source and target dimensions than
     * the previous value. The status bar is updated as if the new conversion was applied <em>before</em> any CRS
     * changes resulting from user selecting a different CRS in the contextual menu.
     *
     * @see #localToObjectiveCRS
     */
    private final class LocalToObjective extends ObjectPropertyBase<MathTransform> {
        LocalToObjective() {super(MathTransforms.identity(BIDIMENSIONAL));}

        @Override public Object getBean() {return StatusBar.this;}
        @Override public String getName() {return "localToObjectiveCRS";}

        /**
         * Overwrite previous value without any check. This method is invoked when the {@link #objectiveCRS}
         * is changed at the same time that the {@link #localToObjectiveCRS} transform, so the number of dimensions
         * may be temporarily mismatched. This method does not invoke {@link #updateLocalToPositionCRS()};
         * that call must be done by the caller when ready.
         */
        final void setNoCheck(final MathTransform newValue) {
            super.set(newValue);
        }

        /**
         * Sets the conversion from local coordinates to geographic or projected coordinates of rendered data.
         *
         * @param  newValue  the new conversion from local coordinates to "real world" coordinates of rendered data.
         * @throws MismatchedDimensionException if the number of dimensions is not the same than previous conversion.
         */
        @Override public void set(final MathTransform newValue) {
            ArgumentChecks.ensureNonNull("newValue", newValue);
            final MathTransform oldValue = get();
            ArgumentChecks.ensureDimensionsMatch("newValue",
                    oldValue.getSourceDimensions(),
                    oldValue.getTargetDimensions(), newValue);
            super.set(newValue);
            updateLocalToPositionCRS();
        }
    }

    /**
     * Returns the lowest value appended as "± <var>accuracy</var>" after the coordinate values.
     * This is the last value specified to {@link #setLowestAccuracy(Quantity)}.
     *
     * @return the lowest accuracy to append after the coordinate values, or {@code null} if none.
     *
     * @see CoordinateFormat#getGroundAccuracy()
     */
    public Quantity<Length> getLowestAccuracy() {
        return lowestAccuracy;
    }

    /**
     * Specifies an uncertainty to append as "± <var>accuracy</var>" after the coordinate values.
     * If user has selected (e.g. by contextual menu) a CRS causing the use of a coordinate transformation,
     * then the accuracy actually shown by {@code StatusBar} will be the greatest value between the accuracy
     * specified to this method and the coordinate transformation accuracy.
     *
     * <p>Note that the "± <var>accuracy</var>" text may be shown or hidden depending on the zoom level.
     * If pixels on screen are larger than the accuracy, then the accuracy text is hidden.</p>
     *
     * @param  accuracy  the lowest accuracy to append after the coordinate values, or {@code null} if none.
     *
     * @see CoordinateFormat#setGroundAccuracy(Quantity)
     */
    public void setLowestAccuracy(final Quantity<Length> accuracy) {
        lowestAccuracy = accuracy;
    }

    /**
     * Returns the coordinates given to the last call to {@link #setLocalCoordinates(double, double)},
     * or an empty value if those coordinates are not visible.
     *
     * @return the local coordinates currently shown in the status bar.
     */
    public Optional<Point2D> getLocalCoordinates() {
        if (isPositionVisible() && !Double.isNaN(lastX) && !Double.isNaN(lastY)) {
            return Optional.of(new Point2D(lastX, lastY));
        }
        return Optional.empty();
    }

    /**
     * Converts and formats the given pixel coordinates. Those coordinates will be automatically
     * converted to geographic or projected coordinates if a "local to CRS" conversion is available.
     *
     * @param  x  the <var>x</var> coordinate local to the view.
     * @param  y  the <var>y</var> coordinate local to the view.
     *
     * @see #handle(MouseEvent)
     */
    public void setLocalCoordinates(final double x, final double y) {
        if (x != lastX || y != lastY) {
            sourceCoordinates[0] = lastX = x;
            sourceCoordinates[1] = lastY = y;
            String text, values = null;
            try {
                convertCoordinates();
                if (isSampleValuesVisible) {
                    values = sampleValuesProvider.get().evaluate(targetCoordinates);
                }
                targetCoordinates.normalize();
                text = format.format(targetCoordinates);
            } catch (TransformException | RuntimeException e) {
                Throwable cause = Exceptions.unwrap(e);
                text = cause.getLocalizedMessage();
                if (text == null) {
                    text = Classes.getShortClassName(cause);
                }
                values = null;
            }
            position.setText(text);
            if (isSampleValuesVisible) {
                sampleValues.setText(values);
            }
            /*
             * Make sure that there is enough space for keeping the coordinates always visible.
             * This is the needed if there is an error message on the left which may be long.
             */
            if (text.length() > maximalPositionLength) {
                maximalPositionLength = text.length();
                position.setMinWidth(Math.min(view.getWidth() / 2, Math.ceil(position.prefWidth(position.getHeight()))));
            }
        }
    }

    /**
     * Converts the local coordinates currently stored in {@link #sourceCoordinates} array.
     * The conversion result is stored in {@link #targetCoordinates} and the {@link #format}
     * is configured with suggested precision. Callers can use this method as below:
     *
     * {@preformat java
     *     convertCoordinates();
     *     targetCoordinates.normalize();
     *     String text = format.format(targetCoordinates);
     * }
     */
    private void convertCoordinates() throws TransformException {
        Matrix derivative;
        try {
            derivative = MathTransforms.derivativeAndTransform(localToPositionCRS,
                    sourceCoordinates, 0, targetCoordinates.coordinates, 0);
        } catch (TransformException ignore) {
            /*
             * If above operation failed, it may be because the MathTransform does not support
             * derivative calculation. Try again without derivative (the precision will be set
             * to the default resolution computed in `setCanvasGeometry(…)`).
             */
            localToPositionCRS.transform(sourceCoordinates, 0, targetCoordinates.coordinates, 0, 1);
            derivative = null;
        }
        if (derivative == null) {
            precisions = null;
        } else {
            if (precisions == null) {
                precisions = new double[targetCoordinates.getDimension()];
            }
            /*
             * Estimate the precision by looking at the maximal displacement in the CRS caused by
             * a displacement of one cell (i.e. when moving by one row or column). We search for
             * maximal displacement because we expect the displacement to be zero along some axes
             * (e.g. one row down does not change longitude value in a Plate Carrée projection).
             */
            for (int j=derivative.getNumRow(); --j >= 0;) {
                double p = 0;
                for (int i=derivative.getNumCol(); --i >= 0;) {
                    double e = Math.abs(derivative.getElement(j, i));
                    if (inflatePrecisions != null) {
                        e *= inflatePrecisions[i];
                    }
                    if (e > p) p = e;
                }
                precisions[j] = p;
            }
        }
        format.setPrecisions(precisions);
    }

    /**
     * Converts and formats the given local coordinates, but without modifying text shown in this status bar.
     *
     * @param  x  the <var>x</var> coordinate local to the view.
     * @param  y  the <var>y</var> coordinate local to the view.
     */
    final String formatCoordinates(final double x, final double y) throws TransformException {
        sourceCoordinates[0] = x;
        sourceCoordinates[1] = y;
        final String separator = format.getSeparator();
        try {
            format.setSeparator("\t");
            convertCoordinates();
            return format.format(targetCoordinates);
        } finally {
            format.setSeparator(separator);
        }
    }

    /**
     * Updates the coordinates shown in the status bar with the value given by the mouse event.
     * This method handles the following events:
     *
     * <ul>
     *   <li>{@link MouseEvent#MOUSE_ENTERED}: show the coordinates.</li>
     *   <li>{@link MouseEvent#MOUSE_EXITED}:  hide the coordinates.</li>
     *   <li>{@link MouseEvent#MOUSE_MOVED}:   delegate to {@link #setLocalCoordinates(double, double)}.</li>
     * </ul>
     *
     * @param  event  the enter, exit or move event. For the convenience of programmatic calls,
     *                a null value is synonymous to a mouse exit event.
     */
    @Override
    public void handle(final MouseEvent event) {
        if (event != null) {
            final EventType<? extends MouseEvent> type = event.getEventType();
            if (type == MouseEvent.MOUSE_MOVED || type == MouseEvent.MOUSE_ENTERED) {
                setLocalCoordinates(event.getX(), event.getY());
                return;
            }
            if (type != MouseEvent.MOUSE_EXITED) {
                return;
            }
        }
        /*
         * Mouse exited the canvas. Use substitution texts.
         * Do not use `position.setVisible(false)` because
         * we want the Tooltip to continue to be available.
         */
        lastX = lastY = Double.NaN;
        position.setText(outsideText);
        if (isSampleValuesVisible) {
            sampleValues.setText(sampleValuesProvider.get().evaluate(null));
        }
    }

    /**
     * Returns {@code true} if the position contains a valid coordinates.
     */
    private boolean isPositionVisible() {
        if (position.isVisible()) {
            final String text = position.getText();
            return text != null && text != outsideText;           // Identity comparison is okay for that value.
        }
        return false;
    }

    /**
     * Sets whether to show or hide the label for values under the cursor.
     * This method is invoked when {@link #sampleValuesProvider} changed.
     *
     * @see #sampleValuesProvider
     * @see #sampleValues
     */
    private void setSampleValuesVisible(final boolean visible) {
        final ObservableList<Node> c = view.getChildren();
        if (visible) {
            if (sampleValues == null) {
                sampleValues = new Label();
                sampleValues.setAlignment(Pos.CENTER_RIGHT);
                sampleValues.setTextAlignment(TextAlignment.RIGHT);
                sampleValues.setMinWidth(Label.USE_PREF_SIZE);
                sampleValues.setMaxWidth(Label.USE_PREF_SIZE);
            }
            if (c.lastIndexOf(sampleValues) < 0) {
                c.add(sampleValues);
            }
        } else if (sampleValues != null) {
            sampleValues.setText(null);
            c.remove(sampleValues);
        }
        isSampleValuesVisible = visible;
    }

    /**
     * Given the longest expected text for values under the cursor, computes the {@link #sampleValues} minimal width.
     * If {@code prototype} is empty, then no sample values are expected and the {@link #sampleValues} label will be
     * hidden.
     *
     * @param  prototype  an example of longest normal text that we expect.
     * @param  others     some other texts that may appear, such as labels for missing data.
     * @return {@code true} on success, or {@code false} if this method should be invoked again.
     *
     * @see ValuesUnderCursor#prototype(String, Iterable)
     */
    final boolean computeSizeOfSampleValues(final String prototype, final Iterable<String> others) {
        setSampleValuesVisible(prototype != null && !prototype.isEmpty());
        if (isSampleValuesVisible) {
            sampleValues.setText(prototype);
            sampleValues.setPrefWidth(Label.USE_COMPUTED_SIZE);                 // Enable `prefWidth(…)` computation.
            double width = sampleValues.prefWidth(sampleValues.getHeight());
            final double max = Math.max(width * 1.25, 200);                     // Arbitrary limit.
            for (final String other : others) {
                sampleValues.setText(other);
                final double cw = sampleValues.prefWidth(sampleValues.getHeight());
                if (cw > width) {
                    width = cw;
                    if (width > max) {
                        width = max;
                        break;
                    }
                }
            }
            sampleValues.setText(null);
            if (!(width > 0)) {                 // May be 0 if canvas is not yet added to scene graph.
                return false;
            }
            sampleValues.setPrefWidth(width + VALUES_PADDING);
        }
        return true;
    }

    /**
     * Returns the error message currently shown.
     *
     * @return the current error message, or an empty value if none.
     */
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(message.getText());
    }

    /**
     * Show or hide an error message on the status bar, optionally with a button showing details in a dialog box.
     * The {@code text} argument specifies the message to show on the status bar.
     * If {@code text} is null, the message will be taken from the {@code details} if non-null.
     * If {@code details} is also null, then the error message will be hidden.
     *
     * @param  text     the error message to show, or {@code null} if none.
     * @param  details  the exception that caused the error, or {@code null} if none.
     */
    public void setErrorMessage(String text, final Throwable details) {
        text = Strings.trimOrNull(text);
        Button more = null;
        if (details != null) {
            final Locale locale = getLocale();
            if (text == null) {
                text = Exceptions.getLocalizedMessage(details, locale);
                if (text == null) {
                    text = details.getClass().getSimpleName();
                }
            }
            final String alert = text;
            more = new Button(Styles.ERROR_DETAILS_ICON);
            more.setOnAction((e) -> ExceptionReporter.show(getView(),
                    Resources.forLocale(locale).getString(Resources.Keys.ErrorDetails), alert, details));
        }
        message.setVisible(text != null);
        message.setGraphic(more);
        message.setText(text);
        message.setTextFill(Styles.ERROR_TEXT);
    }

    /**
     * Shown an error message that occurred in the context of rendering the {@link #canvas} content.
     * This method should not be invoked for other context like an error during transformation of
     * coordinates shown is the status bar.
     */
    private void setRenderingError(final Throwable details) {
        String text = null;
        if (details != null) {
            text = Resources.forLocale(getLocale()).getString(Resources.Keys.CanNotRender);
        }
        setErrorMessage(text, details);
    }
}
