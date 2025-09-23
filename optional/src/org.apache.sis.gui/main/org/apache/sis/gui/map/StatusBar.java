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

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Orientation;
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
import javafx.scene.control.Separator;
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
import javafx.concurrent.Task;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.portrayal.RenderException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.gui.Widget;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.gui.internal.GUIUtilities;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.gui.internal.Styles;
import org.apache.sis.referencing.gazetteer.ReferencingByIdentifiers;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;

// Specific to the main branch:
import org.opengis.geometry.MismatchedDimensionException;


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
 * Alternatively, users can omit some or all above listener registrations and invoke
 * {@link #setLocalCoordinates(double, double)} explicitly instead.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.1
 */
public class StatusBar extends Widget implements EventHandler<MouseEvent> {
    /**
     * The {@value} value, for identifying code that assume two-dimensional objects.
     *
     * @see #getXYDimensions()
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
     * Minimal size of the text field where sample values are shown.
     */
    private static final int MINIMAL_VALUES_SIZE = 60;

    /**
     * The container of controls making the status bar.
     * Contains {@link #message}, {@link #position} and {@link #sampleValues}.
     *
     * @see #getView()
     */
    private final HBox view;

    /**
     * Message to write in the middle of the status bar.
     * This component usually has nothing to show; it is used mostly for error messages.
     * It takes all the space before {@link #position}.
     *
     * @see #getMessage()
     */
    private final Label message;

    /**
     * Local coordinates currently formatted in the {@link #position} field.
     * This is used for detecting if coordinate values changed since last formatting.
     * If the mouse moved outside the canvas, then those coordinates are set to NaN.
     * Otherwise those coordinates are usually integer values.
     *
     * @see #getLocalCoordinates()
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
    private MapCanvas canvas;

    /**
     * The manager of reference systems chosen by user, or {@code null} if none.
     * The {@link RecentReferenceSystems#areaOfInterest} property is used for
     * computing {@link #objectiveToPositionCRS}.
     */
    private final RecentReferenceSystems systemChooser;

    /**
     * The selected reference system, or {@code null} if there is no such property. This property is provided
     * by {@link RecentReferenceSystems}. It usually has the same value as {@link #positionReferenceSystem},
     * but the values may temporarily differ between the time a CRS is selected and when it became applied.
     *
     * @see #positionReferenceSystem
     */
    private final ObjectProperty<ReferenceSystem> selectedSystem;

    /**
     * The reference system used for rendering the data for which this status bar is providing cursor coordinates.
     * This is the "{@linkplain RecentReferenceSystems#setPreferred(boolean, ReferenceSystem) preferred}" or native
     * data CRS. It may be different than the CRS of coordinates actually shown in the status bar.
     *
     * @see MapCanvas#getObjectiveCRS()
     */
    private CoordinateReferenceSystem objectiveCRS;

    /**
     * The transform from <i>objective CRS</i> to the CRS of coordinates shown in this status bar.
     * The {@linkplain CoordinateOperation#getSourceCRS() source CRS} is {@link #objectiveCRS} and
     * the {@linkplain CoordinateOperation#getTargetCRS() target CRS} is {@link CoordinateFormat#getDefaultCRS()}.
     * This transform may be null if there is no CRS change to apply
     * (in which case {@link #localToPositionCRS} is the same instance as {@link #localToObjectiveCRS})
     * or if the target is not a CRS (for example it may be a Military Grid Reference System (MGRS) code).
     *
     * @see #localToObjectiveCRS
     * @see #localToPositionCRS
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
     * (ignoring rounding error). This transform is normally the inverse of {@linkplain #canvas}
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
     * <h4>API note</h4>
     * We do not provide getter/setter for this property; use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     *
     * @see MapCanvas#getObjectiveCRS()
     * @see MapCanvas#getObjectiveToDisplay()
     */
    public final ObjectProperty<MathTransform> localToObjectiveCRS;

    /**
     * The reference systems used by the coordinates shown in this status bar.
     * This is initially the <i>objective CRS</i>, but may become different
     * if the user selects another reference system through contextual menu.
     *
     * <h4>API note</h4>
     * We do not provide getter method for this property; use {@link ReadOnlyObjectProperty#get()}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     *
     * @see #position
     */
    public final ReadOnlyObjectProperty<ReferenceSystem> positionReferenceSystem;

    /**
     * Transform from local coordinates to geographic or projected coordinates shown in this status bar.
     * This is the concatenation of {@link #localToObjectiveCRS} with {@link #objectiveToPositionCRS} transform.
     * The result is a transform to the user-selected CRS for coordinates shown in the status bar.
     * That transform target CRS shall correspond to {@link CoordinateFormat#getDefaultCRS()}.
     * This transform shall never be null but may be the identity transform.
     * It is usually non-affine if the display CRS is not the same as the objective CRS.
     * This transform may have a {@linkplain CoordinateOperation#getCoordinateOperationAccuracy() limited accuracy}.
     *
     * @see #localToObjectiveCRS
     * @see #getPositionCRS()
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
     * Indices where to assign the values of the <var>x</var> and <var>y</var> arguments in {@link #sourceCoordinates}.
     * They are usually 0 for <var>x</var> and 1 for <var>y</var>.
     *
     * @see #BIDIMENSIONAL
     */
    private int xDimension, yDimension;

    /**
     * The source local indices before conversion to geospatial coordinates (never {@code null}).
     * The number of dimensions is often {@value #BIDIMENSIONAL}. May be the same array as
     * <code>{@linkplain #targetCoordinates}.coordinates</code> if both are two-dimensional
     * (if more than 2 dimensions, we need to avoid overwriting values in extra dimensions).
     *
     * @see #targetCoordinates
     * @see #position
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
     * It is the argument to be given to {@link CoordinateFormat#setPrecisions(double...)}.
     *
     * @see CoordinateFormat#setPrecisions(double...)
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
     * The unit of measurement for {@link #precisions}.
     * This is the unit of measurement of the first coordinate system axis.
     */
    private Unit<?> precisionUnit;

    /**
     * Number of elements in {@link #precisions} having the same unit of measurement than {@link #precisionUnit}.
     * This value shall be between 1 and {@code precisions.length} inclusive, or 0 if {@link #precisionUnit} is null.
     */
    private int compatiblePrecisionCount;

    /**
     * Specifies a minimal uncertainty to append as "± <var>accuracy</var>" after the coordinate values.
     * This uncertainty can be caused for example by a coordinate transformation applied on data before
     * rendering in the canvas.
     *
     * <p>Note that {@code StatusBar} maintains also its own uncertainty, which can be caused by transformation
     * from objective CRS to the {@linkplain #positionReferenceSystem reference system used in this status bar}.
     * Such transformations happen when users select a CRS on the status bar (e.g. using the contextual menu)
     * which is different than the canvas {@linkplain MapCanvas#getObjectiveCRS() objective CRS}.
     * In such case we have two sources of stochastic errors: one internal to this status bar and one having
     * causes external to this status bar. This {@code lowestAccuracy} property is for specifying the latter.</p>
     *
     * <p>The accuracy actually shown by {@code StatusBar} will be the greatest value between the accuracy
     * specified in this property and the accuracy computed internally by {@code StatusBar}.
     * Note that the "± <var>accuracy</var>" text may be shown or hidden depending on the zoom level.
     * If pixels on screen are larger than the accuracy, then the accuracy text is hidden.</p>
     *
     * @see CoordinateFormat#setGroundAccuracy(Quantity)
     *
     * @since 1.3
     */
    public final ObjectProperty<Quantity<Length>> lowestAccuracy;

    /**
     * The object to use for formatting coordinate values.
     * This reference shall not be null because it is the instance to use most of the time.
     * In the rarer cases where {@link #formatAsIdentifiers} is non-null, the latter has precedence.
     */
    private final CoordinateFormat format;

    /**
     * The object to use for formatting coordinate values as identifiers (MGRS, GeoHash…).
     * The null/non-null state tells whether to format coordinates as identifiers or not;
     * a {@code null} values mean that coordinates shall be formatted using {@link #format} instead.
     *
     * <p>If non-null, then {@link #getPositionCRS()} should be the {@link #objectiveCRS}
     * and {@link #objectiveToPositionCRS} should be null.</p>
     */
    private ReferencingByIdentifiers.Coder formatAsIdentifiers;

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
     * <h4>API note</h4>
     * We do not provide getter/setter for this property; use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
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
     * The background task under execution, or {@code null} if none. This is used for cancellation.
     *
     * @see #cancelWorker()
     * @see #terminated(Task)
     */
    private Task<?> worker;

    /**
     * Creates a new status bar for showing coordinates of mouse cursor position in a canvas.
     * If {@link #track(Canvas)} is invoked, then this {@code StatusBar} will show coordinates
     * (usually geographic or projected) of mouse cursor position when the mouse is over that canvas.
     * Note that in such case, the {@link #localToObjectiveCRS} property value will be overwritten
     * at any time (for example every time that a gesture event such as pan, zoom or rotation happens).
     *
     * <p>If the {@code systemChooser} argument is non-null, user will be able to select different CRS
     * using the contextual menu on the status bar.</p>
     *
     * <h4>Limitations</h4>
     * This constructor registers numerous listeners on {@code canvas} and {@code systemChooser}.
     * There is currently no unregistration mechanism. The {@code StatusBar} instance is expected
     * to exist as long as the {@code MapCanvas} and {@code RecentReferenceSystems} instances
     * given to this constructor.
     *
     * @param  systemChooser  the manager of reference systems chosen by user, or {@code null} if none.
     */
    @SuppressWarnings("this-escape")    // `this` appears in a cyclic graph.
    public StatusBar(final RecentReferenceSystems systemChooser) {
        positionReferenceSystem = new PositionSystem();
        localToObjectiveCRS     = new LocalToObjective();
        localToPositionCRS      = localToObjectiveCRS.get();
        targetCoordinates       = new GeneralDirectPosition(BIDIMENSIONAL);
        sourceCoordinates       = targetCoordinates.coordinates;
        lastX = lastY           = Double.NaN;
        yDimension              = 1;
        format                  = new CoordinateFormat();
        lowestAccuracy          = new SimpleObjectProperty<>(this, "lowestAccuracy");

        message = new Label();
        message.setVisible(false);                      // Waiting for getting a message to display.
        message.setMaxWidth(Double.POSITIVE_INFINITY);
        HBox.setHgrow(message, Priority.ALWAYS);

        position = new Label();
        position.setAlignment(Pos.CENTER_RIGHT);
        position.setTextAlignment(TextAlignment.RIGHT);

        view = new HBox(6, message, position);
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
            final Menu choices = systemChooser.createMenuItems(false, (property, oldValue, newValue) -> {
                if (newValue instanceof CoordinateReferenceSystem) {
                    setPositionCRS((CoordinateReferenceSystem) newValue);
                } else if (newValue instanceof ReferencingByIdentifiers) {
                    setPositionRID((ReferencingByIdentifiers) newValue);
                } else {
                    setPositionCRS(null);       // Default to `objectiveCRS`.
                }
            });
            selectedSystem = RecentReferenceSystems.getSelectedProperty(choices);
            menu.getItems().add(choices);
            /*
             * Ensure (as much as possible) that the CRS of coordinates formatted in this status bar is one
             * of the CRSs in the list of choices offered to the user.  It happens often that the CRS given
             * to `applyCanvasGeometry(GridGeometry)` has (λ,φ) axis order but the CRS offered to user have
             * (φ,λ) axis order (because we try to comply with definitions following geographers practice).
             * In such case we will replace (λ,φ) by (φ,λ). Since we use the list of choices as the source
             * of desired CRS, we have to listen to new elements added to that list. This is necessary because
             * the list is often empty at construction time and filled later after a background thread task.
             */
            systemChooser.getItems().addListener((ListChangeListener.Change<? extends ReferenceSystem> change) -> {
                if (formatAsIdentifiers == null) {
                    while (change.next()) {
                        if (change.wasAdded() || change.wasReplaced()) {
                            setReplaceablePositionCRS(getPositionCRS());
                            break;
                        }
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
            if (n != null) items.add(1, n.valueChoices);
            setSampleValuesVisible(n != null);
        });
    }

    /**
     * Registers listeners on the specified canvas for tracking mouse movements.
     * After this method call, this {@code StatusBar} will show coordinates (usually geographic or projected)
     * of mouse cursor position when the mouse is over that canvas. The {@link #localToObjectiveCRS} property
     * value may be overwritten at any time, for example after each gesture event such as pan, zoom or rotation.
     *
     * <h4>Limitations</h4>
     * Current implementation accepts only zero or one {@code MapCanvas}. A future implementation
     * may accept a larger number of canvas for tracking many views with a single status bar
     * (for example images over the same area but at different times).
     *
     * @param  canvas  the canvas that this status bar is tracking.
     *
     * @since 1.3
     */
    public void track(final MapCanvas canvas) {
        if (this.canvas != null) {
            throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.TooManyCollectionElements_3, "canvas", 1, 2));
        }
        /*
         * If a canvas is specified, register listeners for mouse position, rendering events, errors, etc.
         * We do not allow the canvas to be changed after construction because of the added complexity
         * (e.g. we would have to remember all registered listeners so we can unregister them).
         */
        this.canvas = Objects.requireNonNull(canvas);
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
             * After the canvas has been initialized, it cannot be null anymore.
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
        xDimension = 0;
        yDimension = 1;
        apply(geometry);
    }

    /**
     * Configures this status bar for showing coordinates of a slice of a grid coverage.
     * This method is useful for tracking the pixel coordinates of an image obtained by
     * a call to {@link GridCoverage#render(GridExtent)}.
     * By {@code render(GridExtent)} contract, the {@link RenderedImage} pixel coordinates
     * are relative to the requested {@link GridExtent}. Consequently, we need to translate
     * the grid coordinates so that the request coordinates start at zero.
     * This method handles that translation.
     *
     * @param  geometry     geometry of the coverage which produced the {@link RenderedImage} to track, or {@code null}.
     * @param  sliceExtent  the extent specified in call to {@link GridCoverage#render(GridExtent)} (can be {@code null}).
     * @param  xdim         the grid dimension where to assign the values of <var>x</var> pixel coordinates.
     * @param  ydim         the grid dimension where to assign the values of <var>y</var> pixel coordinates.
     *
     * @since 1.3
     */
    public void applyCanvasGeometry(GridGeometry geometry, GridExtent sliceExtent, final int xdim, final int ydim) {
        position.setText(null);
        if (geometry != null) {
            final int dimension = geometry.getDimension();
            ArgumentChecks.ensureBetween("xdim", 0,      dimension-1, xdim);
            ArgumentChecks.ensureBetween("ydim", xdim+1, dimension-1, ydim);
            xDimension = xdim;
            yDimension = ydim;                      // Shall be assigned before call to `getXYDimensions()` below.
            if (sliceExtent != null) {
                final long[] offset = new long[dimension];
                for (final int i : getXYDimensions()) {
                    offset[i] = sliceExtent.getLow(i);
                }
                sliceExtent = sliceExtent.translate(offset, true);
                geometry = geometry.shiftGrid(offset, true);        // Does not change the "real world" envelope.
                try {
                    geometry = geometry.relocate(sliceExtent);      // Changes the "real world" envelope.
                } catch (TransformException e) {
                    setErrorMessage(null, e);
                }
            }
        }
        apply(geometry);
    }

    /**
     * Implementation of {@link #applyCanvasGeometry(GridGeometry)} without changing {@link #position} visibility state.
     * Invoking this method usually invalidate the coordinates shown in this status bar. The new coordinates cannot be
     * easily recomputed because the {@link #lastX} and {@link #lastY} values may not be valid anymore, as a result of
     * possible changes in JavaFX local coordinate system. Consequently, the coordinates should be temporarily hidden
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
        double[] pointOfInterest = ArraysExt.EMPTY_DOUBLE;
        double[] inflate = null;
        double   resolution = 1;
        Unit<?>  unit = Units.PIXEL;
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
             * This will be used as a fallback if we cannot compute the precision specific
             * to a coordinate, for example if we cannot compute the derivative.
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
                pointOfInterest = extent.getPointOfInterest(PixelInCell.CELL_CENTER);
            }
        }
        /*
         * If the objective CRS stay unchanged, then we will try to keep the same position CRS
         * (which may be different), which implies keeping the same `objectiveToPositionCRS`.
         */
        final boolean clear = (fullOperationSearchRequired != null) && fullOperationSearchRequired.test(canvas);
        final boolean sameCRS = !clear && CRS.equivalent(objectiveCRS, crs);
        if (localToCRS == null) {
            localToCRS = MathTransforms.identity(BIDIMENSIONAL);
        }
        if (sameCRS && objectiveToPositionCRS != null) {
            localToCRS = MathTransforms.concatenate(localToCRS, objectiveToPositionCRS);
        }
        final int srcDim = Math.max(localToCRS.getSourceDimensions(), BIDIMENSIONAL);
        final int tgtDim = localToCRS.getTargetDimensions();
        /*
         * Remaining code should not fail, so we can start modifying the `StatusBar` fields.
         * The buffers for source and target coordinates are recreated because the number of
         * dimensions may have changed. The `lastX` and `lastY` coordinates are local to the
         * JavaFX view and considered invalid  because they depend on the transforms applied
         * on JavaFX node, which may have changed together with `localToObjectiveCRS` change.
         * So we cannot use those values for updating the coordinates shown in status bar.
         * Instead, we will wait for the next mouse event to provide new local coordinates.
         */
        ((LocalToObjective) localToObjectiveCRS).setNoCheck(localToCRS);
        sourceCoordinates   = Arrays.copyOf(pointOfInterest, srcDim);
        targetCoordinates   = new GeneralDirectPosition(tgtDim);
        objectiveCRS        = crs;
        localToPositionCRS  = localToCRS;
        inflatePrecisions   = inflate;
        precisions          = null;
        lastX = lastY       = Double.NaN;           // Not valid anymove — see above block comment.
        /*
         * If the objective CRS is unchanged, keep the same position CRS (the CRS selected
         * by user for formatting coordinates; it may be different than the objective CRS).
         * Otherwise we reset the formatter to the CRS specified in the grid geometry.
         */
        if (sameCRS) {
            crs = getPositionCRS();
            targetCoordinates.setCoordinateReferenceSystem(crs);
        } else {
            objectiveToPositionCRS = null;
            setFormatCRS(crs, null);                            // Should be invoked before to set ground precision.
            crs = OperationFinder.toGeospatial(crs, canvas);
            crs = setReplaceablePositionCRS(crs);               // May invoke setFormatCRS(…) after background work.
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
     * Sets the CRS of the position shown in this status bar after replacement by one of the available CRS
     * if a match is found. This method compares the given CRS with the list of choices before to delegate
     * to {@link #setPositionCRS(CoordinateReferenceSystem)} possibly with different axis order. A typical
     * scenario is {@link #apply(GridGeometry)} invoked with (<var>longitude</var>, <var>latitude</var>)
     * axis order, and this method swapping axes to standard (<var>latitude</var>, <var>longitude</var>)
     * axis order for coordinates display purpose.
     *
     * <h4>Prerequisite</h4>
     * This method should be invoked only when {@link #formatAsIdentifiers} is null. This is not verified.
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
        if (crs != getPositionCRS()) {
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
     *
     * @see #setPositionRID(ReferencingByIdentifiers)
     * @see #getPositionCRS()
     */
    private void setPositionCRS(final CoordinateReferenceSystem crs) {
        cancelWorker();
        if (crs != null && objectiveCRS != null && objectiveCRS != crs) {
            position.setTextFill(Styles.OUTDATED_TEXT);
            /*
             * Take snapshots of references to all objects that the background thread will use.
             * The background thread shall not read StatusBar fields directly since they may be
             * in the middle of changes at any time. All objects are assumed immutable.
             */
            final Envelope aoi = (systemChooser != null) ? systemChooser.areaOfInterest.get() : null;
            BackgroundThreads.execute(worker = new OperationFinder(canvas, aoi, objectiveCRS, crs) {
                /**
                 * The accuracy to show on the status bar, or {@code null} if none.
                 * This is computed after {@link CoordinateOperation} has been determined.
                 *
                 * @see StatusBar#lowestAccuracy
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
                    terminated(this);
                    setPositionCRS(this, accuracy);
                }

                /**
                 * Invoked in JavaFX thread on failure. The previous CRS is kept unchanged but
                 * the coordinates will appear in red for telling user that there is a problem.
                 */
                @Override protected void failed() {
                    terminated(this);
                    setReferenceSystemError(crs, getException());
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
     *
     * @see #setPositionRID(ReferencingByIdentifiers.Coder, String, DirectPosition)
     */
    private void setPositionCRS(final OperationFinder finder, final Quantity<Length> accuracy) {
        worker = null;
        setErrorMessage(null, null);
        fullOperationSearchRequired = finder.fullOperationSearchRequired();
        localToPositionCRS = localToObjectiveCRS.get();
        objectiveToPositionCRS = finder.getValue();
        if (objectiveToPositionCRS != null) {
            localToPositionCRS = MathTransforms.concatenate(localToPositionCRS, objectiveToPositionCRS);
        }
        setFormatCRS(finder.getTargetCRS(), accuracy);
        rewritePosition(null);
    }

    /**
     * Invoked after a new reference system has been set. This method rewrites the coordinates
     * on the assumption that {@link #lastX} and {@link #lastY} are still valid.
     *
     * @param  current  the local coordinates used for current text, or {@code null} if not valid.
     */
    private void rewritePosition(final DirectPosition current) {
        position.setTextFill(Styles.NORMAL_TEXT);
        position.setMinWidth(0);
        maximalPositionLength = 0;
        if (isPositionVisible()) {
            final double x = lastX;
            final double y = lastY;
            lastX = lastY = Double.NaN;
            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                if (current == null || current.getOrdinate(0) != x || current.getOrdinate(1) != y) {
                    setLocalCoordinates(x, y);
                }
            }
        }
    }

    /**
     * Invoked in JavaFX thread when a background task finished its work, either successfully or on error.
     */
    private void terminated(final Task<?> caller) {
        if (caller == worker) {
            worker = null;
        }
    }

    /**
     * If a background task was in progress, cancels it. This is invoked before a new background task is launched.
     */
    private void cancelWorker() {
        if (worker != null) {
            worker.cancel();
            worker = null;
        }
    }

    /**
     * Sets the {@link CoordinateFormat} default CRS together with the tool tip text.
     * Caller is responsible to setup transforms ({@link #localToPositionCRS}, <i>etc</i>).
     * For method that applies required changes on transforms before to set the format CRS,
     * see {@link #setPositionCRS(CoordinateReferenceSystem)}.
     *
     * @param  crs       the new {@link #format} reference system.
     * @param  accuracy  positional accuracy of the transformation from local coordinates to the given CRS,
     *         or {@code null} if none. This is an accuracy computed by this {@code StatusBar} class,
     *         as opposed to {@link #lowestAccuracy} which has causes external to {@code StatusBar}.
     *
     * @see #positionReferenceSystem
     */
    @SuppressWarnings("StringEquality")
    private void setFormatCRS(final CoordinateReferenceSystem crs, final Quantity<Length> accuracy) {
        int dimension = localToPositionCRS.getTargetDimensions();
        GeneralDirectPosition target = targetCoordinates;
        if (dimension != target.getDimension()) {
            target = new GeneralDirectPosition(dimension);
            precisions = null;
        }
        target.setCoordinateReferenceSystem(crs);
        format.setDefaultCRS(crs);
        targetCoordinates = target;         // Assign only after above succeed.
        formatAsIdentifiers = null;
        format.setGroundAccuracy(Quantities.max(accuracy, lowestAccuracy.get()));
        setTooltip(crs);
        /*
         * Prepare the text to show when the mouse is outside the canvas area.
         * We will write axis abbreviations, for example "(φ, λ)".
         * Also fetch the unit of measurement of first axes.
         */
        compatiblePrecisionCount = 0;
        precisionUnit = null;
        String text = null;
        if (crs != null) {
            final var b = new StringBuilder().append('(');
            final CoordinateSystem cs = crs.getCoordinateSystem();
            dimension = (cs != null) ? cs.getDimension() : 0;       // Paranoiac check (should never be null).
            for (int i=0; i<dimension; i++) {
                if (i != 0) b.append(", ");
                final CoordinateSystemAxis axis = cs.getAxis(i);
                if (axis != null) {                                 // Paranoiac check (should never be null).
                    if (i == compatiblePrecisionCount) {            // Require consecutive axes for unit test.
                        final Unit<?> unit = axis.getUnit();
                        if (i == 0 || Objects.equals(precisionUnit, unit)) {
                            compatiblePrecisionCount = i+1;
                            precisionUnit = unit;
                        }
                    }
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
        /*
         * If the mouse is already outside canvas area, update the `position` text now.
         * Otherwise `position` is probably showing coordinates, which we leave unchanged for now.
         */
        if (position.getText() == outsideText) {          // Identity comparison is okay for this value.
            position.setText(text);
        }
        outsideText = text;
        ((PositionSystem) positionReferenceSystem).fireValueChangedEvent();
    }

    /**
     * Implementation of {@link #positionReferenceSystem} property.
     */
    private final class PositionSystem extends ReadOnlyObjectPropertyBase<ReferenceSystem> {
        @Override public Object getBean() {return StatusBar.this;}
        @Override public String getName() {return "positionReferenceSystem";}
        @Override public ReferenceSystem get() {
            final ReferencingByIdentifiers.Coder f = formatAsIdentifiers;
            return (f != null) ? f.getReferenceSystem() : getPositionCRS();
        }
        @Override protected void fireValueChangedEvent() {super.fireValueChangedEvent();}
    }

    /**
     * Returns the coordinate reference system of the position shown in this status bar.
     * This is valid only if {@link #formatAsIdentifiers} is null.
     *
     * @see #setPositionCRS(CoordinateReferenceSystem)
     */
    private CoordinateReferenceSystem getPositionCRS() {
        return format.getDefaultCRS();
    }

    /**
     * Resets {@link #localToPositionCRS} to its default value. This is invoked either when the
     * target CRS is {@link #objectiveCRS}, or when an attempt to use another CRS failed.
     *
     * @param  textFill  the color to assign to position text. It depends on the reason why we reset the position.
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
         * may be temporarily mismatched. This method does not update {@link #localToPositionCRS};
         * that update must be done by the caller when ready.
         */
        final void setNoCheck(final MathTransform newValue) {
            super.set(newValue);
        }

        /**
         * Sets the conversion from local coordinates to geographic or projected coordinates of rendered data.
         *
         * @param  newValue  the new conversion from local coordinates to "real world" coordinates of rendered data.
         * @throws MismatchedDimensionException if the number of dimensions is not the same as previous conversion.
         */
        @Override public void set(MathTransform newValue) {
            final MathTransform oldValue = get();
            ArgumentChecks.ensureDimensionsMatch("newValue",
                    oldValue.getSourceDimensions(),
                    oldValue.getTargetDimensions(),
                    Objects.requireNonNull(newValue));
            final MathTransform tr = objectiveToPositionCRS;
            if (tr != null) {
                newValue = MathTransforms.concatenate(newValue, tr);
            }
            localToPositionCRS = newValue;
            super.set(newValue);
        }
    }

    /**
     * Sets the reference system of the position shown in this status bar.
     * This is similar to {@link #setPositionCRS(CoordinateReferenceSystem)} but for referencing by identifiers.
     * This method tries to format the current position in a background thread because the first invocation of
     * {@link ReferencingByIdentifiers.Coder#encode(DirectPosition)} may require an access to the EPSG database.
     *
     * @param  system  the new reference system (shall not be {@code null}).
     */
    private void setPositionRID(final ReferencingByIdentifiers system) {
        resetPositionCRS(Styles.OUTDATED_TEXT);
        final DirectPosition poi;
        final MathTransform toObjective;
        final CoordinateReferenceSystem crs = objectiveCRS;
        final Quantity<Length> accuracy = lowestAccuracy.get();
        if (Double.isFinite(lastX) && Double.isFinite(lastY)) {
            poi = new DirectPosition2D(lastX, lastY);
            toObjective = localToPositionCRS;
        } else {
            poi = (canvas != null) ? canvas.getPointOfInterest(true) : null;
            toObjective = null;
        }
        cancelWorker();
        BackgroundThreads.execute(worker = new Task<String>() {
            /**
             * The object to use for formatting identifiers. This is the value to assign
             * to {@link StatusBar#formatAsIdentifiers} after successful task completion.
             */
            private ReferencingByIdentifiers.Coder coder;

            /**
             * Invoked in a background thread for formatting the identifier. The point to format is transformed
             * from local coordinates to objective CRS. The CRS needs to be specified for allowing the coder to
             * transform again the point to whatever internal CRS it needs for encoding purpose.
             */
            @Override protected String call() {
                coder = system.createCoder();
                try {
                    DirectPosition p = poi;
                    if (p != null && toObjective != null) {
                        p = toObjective.transform(p, new GeneralDirectPosition(crs));
                    }
                    if (accuracy != null) {
                        coder.setPrecision(accuracy, p);
                    }
                    if (p != null) {
                        return coder.encode(p);
                    }
                } catch (IncommensurableException | TransformException e) {
                    recoverableException("setPositionRID", e);
                }
                return null;
            }

            /**
             * Invoked in JavaFX thread for reporting a failure.
             * The reference system in use stay the previous one.
             */
            @Override protected void failed() {
                terminated(this);
                setReferenceSystemError(system, getException());
            }

            /** Invoked in JavaFX thread on success for applying the actual reference system change. */
            @Override protected void succeeded() {
                terminated(this);
                setPositionRID(coder, getValue(), (toObjective != null) ? poi : null);
            }
        });
    }

    /**
     * Invoked after the background thread prepared the new reference system.
     * The identifier formatted by the background thread is written, but needs to be rewritten
     * again if {@link #lastX} or {@link #lastY} changed since background task execution.
     *
     * @param  coder       the coder to use for formatting identifiers.
     * @param  identifier  identifier formatted using mouse position.
     * @param  current     the local coordinates used for current text, or {@code null} if not valid.
     *
     * @see #setPositionCRS(OperationFinder, Quantity)
     */
    private void setPositionRID(final ReferencingByIdentifiers.Coder coder, final String identifier, final DirectPosition current) {
        formatAsIdentifiers = coder;
        fullOperationSearchRequired = null;
        outsideText = null;
        setErrorMessage(null, null);
        setTooltip(coder.getReferenceSystem());
        position.setText(identifier);
        ((PositionSystem) positionReferenceSystem).fireValueChangedEvent();
        rewritePosition(current);
    }

    /**
     * Returns the indices of <var>x</var> and <var>y</var> coordinate values in a grid coordinate tuple.
     * They are the indices where to assign the values of the <var>x</var> and <var>y</var> arguments in
     * calls to <code>{@linkplain #setLocalCoordinates(double, double) setLocalCoordinates}(x,y)</code>.
     *
     * <p>The default value is {0,1}, i.e. the 2 first dimensions in a coordinate tuple.
     * The value can be changed by call to {@link #applyCanvasGeometry(GridGeometry, GridExtent, int, int)}.</p>
     *
     * @return indices of <var>x</var> and <var>y</var> coordinate values in a grid coordinate tuple.
     *
     * @since 1.3
     */
    public final int[] getXYDimensions() {
        return new int[] {xDimension, yDimension};
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
     * <h4>Supplemental dimensions</h4>
     * If local coordinates have more than 2 dimensions, then the given (x,y) values will be assigned
     * to the dimensions specified by {@link #getXYDimensions()}. Coordinates in all other dimensions
     * will have the values given by {@link GridExtent#getPointOfInterest(PixelInCell)} from the extent
     * of the grid geometry given to {@link #applyCanvasGeometry(GridGeometry)}.
     *
     * @param  x  the <var>x</var> coordinate local to the view.
     * @param  y  the <var>y</var> coordinate local to the view.
     *
     * @see #handle(MouseEvent)
     */
    public void setLocalCoordinates(final double x, final double y) {
        if (x != lastX || y != lastY) {
            if (isSampleValuesVisible) {
                sampleValuesProvider.get().evaluateLater(targetCoordinates);        // Work in a background thread.
            }
            final String text = formatLocalCoordinates(lastX = x, lastY = y);
            position.setText(text);
            /*
             * Make sure that there is enough space for keeping the coordinates always visible.
             * This is needed if there is an error message on the left which may be long.
             */
            if (text.length() > maximalPositionLength) {
                maximalPositionLength = text.length();
                position.setMinWidth(Math.min(view.getWidth() / 2, Math.ceil(position.prefWidth(position.getHeight()))));
            }
        }
    }

    /**
     * Unconditionally converts and formats the given local coordinates, but without modifying any control.
     * It is caller's responsibility to either change the text shown in the status bar, or to use the returned
     * text for something else (for example for copying in the clipboard).
     *
     * @param  x  the <var>x</var> coordinate local to the view.
     * @param  y  the <var>y</var> coordinate local to the view.
     * @return string representation of coordinates or an error message.
     */
    private String formatLocalCoordinates(final double x, final double y) {
        sourceCoordinates[xDimension] = x;
        sourceCoordinates[yDimension] = y;
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
            try {
                localToPositionCRS.transform(sourceCoordinates, 0, targetCoordinates.coordinates, 0, 1);
            } catch (TransformException e) {
                return cause(e);
            }
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
        targetCoordinates.normalize();
        /*
         * Format as an identifier or as a coordinate tuple, depending on the type of the reference system.
         * The precision is determined by the size of a pixel on screen and controls the number of fraction
         * digits to print. Precision should not be confused with accuracy, which depends on transformation
         * applied on coordinate values and determines the "± accuracy" text shown after coordinates.
         */
        try {
            if (formatAsIdentifiers != null) {
                double precision = 0;
                for (int i = compatiblePrecisionCount; --i >= 0;) {
                    final double p = precisions[i];
                    if (p > precision) precision = p;
                }
                return formatAsIdentifiers.encode(targetCoordinates,
                        (precision > 0) ? Quantities.create(precision, precisionUnit) : null);
            } else {
                format.setPrecisions(precisions);
                return format.format(targetCoordinates);
            }
        } catch (Exception e) {
            return cause(e);
        }
    }

    /**
     * Converts and formats the given local coordinates, but without modifying text shown in this status bar.
     * This is used for copying the coordinates somewhere else, for example on the clipboard.
     *
     * @param  x  the <var>x</var> coordinate local to the view.
     * @param  y  the <var>y</var> coordinate local to the view.
     */
    final String formatTabSeparatedCoordinates(final double x, final double y) throws TransformException {
        final String separator = format.getSeparator();
        try {
            format.setSeparator("\t");
            return formatLocalCoordinates(x, y);
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
            sampleValuesProvider.get().evaluateLater(null);
        }
    }

    /**
     * Sets the result of formatting sample values under cursor position.
     */
    final void setSampleValues(final String text) {
        sampleValues.setText(text);
    }

    /**
     * Returns {@code true} if the position contains a valid coordinates.
     */
    @SuppressWarnings("StringEquality")
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
        Label sampleView = sampleValues;
        if (visible) {
            if (sampleView == null) {
                sampleView = new Label();
                sampleView.setAlignment(Pos.CENTER_RIGHT);
                sampleView.setTextAlignment(TextAlignment.RIGHT);
                sampleView.setMinWidth(Label.USE_PREF_SIZE);
                sampleView.setMaxWidth(Label.USE_PREF_SIZE);
                sampleValues = sampleView;
            }
            if (c.lastIndexOf(sampleView) < 0) {
                final Separator separator = new Separator(Orientation.VERTICAL);
                c.addAll(separator, sampleView);
            }
        } else if (sampleView != null) {
            sampleView.setText(null);
            int i = c.lastIndexOf(sampleView);
            if (i >= 0) {
                c.remove(i);
                if (--i >= 0) {
                    final Node last = c.remove(i);
                    assert last instanceof Separator : last;
                }
            }
        }
        isSampleValuesVisible = visible;
    }

    /**
     * Given the longest expected text for values under the cursor, computes the {@link #sampleValues} minimal width.
     * If {@code prototype} is empty, then no sample values are expected and the {@link #sampleValues} label will be
     * hidden.
     *
     * @param  prototype  an example of longest normal text that we expect, or {@code null} or empty for hiding.
     * @param  others     some other texts that may appear, such as labels for missing data.
     * @return {@code true} on success, or {@code false} if this method should be invoked again.
     *
     * @see ValuesUnderCursor#prototype(String, Iterable)
     */
    final boolean computeSizeOfSampleValues(final String prototype, final Iterable<String> others) {
        setSampleValuesVisible(prototype != null && !prototype.isEmpty());
        if (isSampleValuesVisible) {
            final Label sampleView = sampleValues;
            sampleView.setText(prototype);
            sampleView.setPrefWidth(Label.USE_COMPUTED_SIZE);               // Enable `prefWidth(…)` computation.
            double width = sampleView.prefWidth(sampleView.getHeight());
            final double max = Math.max(width * 1.25, 200);                 // Arbitrary limit.
            for (final String other : others) {
                sampleView.setText(other);
                final double cw = sampleView.prefWidth(sampleView.getHeight());
                if (cw > width) {
                    width = cw;
                    if (width > max) {
                        width = max;
                        break;
                    }
                }
            }
            sampleView.setText(null);
            if (!(width > 0)) {                 // May be 0 if canvas is not yet added to scene graph.
                return false;
            }
            sampleView.setPrefWidth(Math.max(width + VALUES_PADDING, MINIMAL_VALUES_SIZE));
        }
        return true;
    }

    /**
     * Sets the tooltip text to show when the mouse cursor is over the coordinate values.
     */
    private void setTooltip(final ReferenceSystem crs) {
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
    }

    /**
     * Returns the message currently shown. It may be an error message or an informative message.
     *
     * @return the current message, or an empty value if none.
     *
     * @since 1.3
     */
    public Optional<String> getMessage() {
        return Optional.ofNullable(message.getText());
    }

    /**
     * Shows or hides an informative message on the status bar.
     * The message should be temporary, for example for telling that a loading is in progress.
     *
     * @param  text  the message to show, or {@code null} if none.
     *
     * @since 1.3
     */
    public void setInfoMessage(String text) {
        text = Strings.trimOrNull(text);
        message.setVisible(text != null);
        message.setGraphic(null);
        message.setText(text);
        message.setTextFill(Styles.LOADING_TEXT);
    }

    /**
     * Shows or hides an error message on the status bar, optionally with a button showing details in a dialog box.
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
            final String alert = (text != null) ? text : cause(details);
            more = new Button(Styles.ERROR_DETAILS_ICON);
            more.setOnAction((e) -> ExceptionReporter.show(getView(),
                    Resources.forLocale(getLocale()).getString(Resources.Keys.ErrorDetails), alert, details));
        }
        message.setVisible(text != null);
        message.setGraphic(more);
        message.setText(text);
        message.setTextFill(Styles.ERROR_TEXT);
    }

    /**
     * Shows an error message for a reference system that cannot be set.
     * The previous reference system is kept unchanged but the coordinates
     * will appear in red for telling user that there is a problem.
     *
     * @param system     the reference system that we failed to set.
     * @param exception  the exception that occurred while attempting to set the CRS.
     */
    private void setReferenceSystemError(final ReferenceSystem system, final Throwable exception) {
        final Locale locale = getLocale();
        setErrorMessage(Resources.forLocale(locale).getString(Resources.Keys.CanNotUseRefSys_1,
                        IdentifiedObjects.getDisplayName(system, locale)), exception);
        if (selectedSystem != null) {
            selectedSystem.set(positionReferenceSystem.get());
        }
        resetPositionCRS(Styles.ERROR_TEXT);
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

    /**
     * Returns a string representation of the message of the given exception.
     * If the exception is a wrapper, the exception cause is taken.
     * If there is no message, the exception class name is returned.
     *
     * @param  e  the exception.
     * @return the exception message or class name.
     */
    final String cause(Throwable e) {
        if (e instanceof Exception) {
            e = Exceptions.unwrap((Exception) e);
        }
        String text = Exceptions.getLocalizedMessage(e, getLocale());
        if (text == null) {
            text = Classes.getShortClassName(e);
        }
        return text;
    }

    /**
     * Logs an error considered too minor for reporting on the status bar.
     */
    private static void recoverableException(final String caller, final Exception e) {
        Logging.recoverableException(LOGGER, StatusBar.class, caller, e);
    }
}
