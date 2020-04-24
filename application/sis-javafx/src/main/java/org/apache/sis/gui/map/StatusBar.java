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
import javax.measure.Unit;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Menu;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextAlignment;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.util.FactoryException;
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
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
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
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.gui.Widget;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.system.Modules;
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
 * @version 1.1
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
     * The area of interest, or {@code null} if none. Used for computing {@link #objectiveToFormatCRS}.
     * This field is a reference to the {@link RecentReferenceSystems#areaOfInterest} property.
     * We do not make this property public because it does not belong to this object.
     */
    private final ObjectProperty<Envelope> areaOfInterest;

    /**
     * The reference system used for rendering the data for which this status bar is providing cursor coordinates.
     * This is the "{@linkplain RecentReferenceSystems#setPreferred(boolean, ReferenceSystem) preferred}" or native
     * data CRS. It may not be the same than the CRS of coordinates actually shown in the status bar.
     *
     * @see #getObjectiveCRS()
     * @see #getFormatReferenceSystem()
     * @see MapCanvas#getObjectiveCRS()
     */
    private CoordinateReferenceSystem objectiveCRS;

    /**
     * Hold the transform from <cite>objective CRS</cite> to the CRS of coordinates shown in this status bar.
     * The {@linkplain CoordinateOperation#getSourceCRS() source CRS} is {@link #objectiveCRS} and
     * the {@linkplain CoordinateOperation#getTargetCRS() target CRS} is {@link CoordinateFormat#getDefaultCRS()}.
     * This coordinate operation may be null if there is no CRS change to apply
     * (in which case {@link #localToFormatCRS} is the same instance than {@link #localToObjectiveCRS})
     * or if the target is not a CRS (for example it may be a Military Grid Reference System (MGRS) code).
     */
    private CoordinateOperation objectiveToFormatCRS;

    /**
     * Conversion from local coordinates to geographic or projected coordinates of rendered data.
     * This is not necessarily the conversion to the coordinates shown in this status bar.
     * This conversion shall never be null but may be the identity transform.
     * It should have no {@linkplain CoordinateOperation#getCoordinateOperationAccuracy() inaccuracy}
     * (ignoring rounding error). This transform is usually (but not necessarily) affine.
     *
     * @see #getLocalToObjectiveCRS()
     * @see MapCanvas#getObjectiveToDisplay()
     */
    private MathTransform localToObjectiveCRS;

    /**
     * Conversion from local coordinates to geographic or projected coordinates shown in this status bar.
     * This is the concatenation of {@link #localToObjectiveCRS} with {@link #objectiveToFormatCRS} transform.
     * The result is a transform to the user-selected CRS for coordinates shown in the status bar.
     * This conversion shall never be null but may be the identity transform.
     * It is usually non-affine if the display CRS is not the same than the objective CRS.
     * This transform may have a {@linkplain CoordinateOperation#getCoordinateOperationAccuracy() limited accuracy}.
     *
     * <p>The target CRS can be obtained by {@link CoordinateOperation#getTargetCRS()} on
     * {@link #objectiveToFormatCRS} or by {@link CoordinateFormat#getDefaultCRS()}.</p>
     */
    private MathTransform localToFormatCRS;

    /**
     * The source local indices before conversion to geospatial coordinates.
     * The number of dimensions is often {@value #BIDIMENSIONAL}.
     * Shall never be {@code null}.
     *
     * @see #targetCoordinates
     * @see #position
     */
    private double[] sourceCoordinates;

    /**
     * Coordinates after conversion to the CRS. The number of dimensions depends on
     * the target CRS. This object is reused during each coordinate transformation.
     * Shall never be {@code null}.
     *
     * @see #sourceCoordinates
     * @see #position
     */
    private GeneralDirectPosition targetCoordinates;

    /**
     * The desired precisions for each dimension in the {@link #targetCoordinates} to format.
     * It may vary for each position if the {@link #localToFormatCRS} transform is non-linear.
     * This array is initially {@code null} and created when first needed.
     */
    private double[] precisions;

    /**
     * A multiplication factory slightly greater than 1 applied on {@link #precisions}.
     * The intent is to avoid that a precision like 0.09999 is interpreted as requiring
     * two decimal digits instead of 1. For avoiding that, we add a small value to the
     * precision: <var>precision</var> += <var>precision</var> × ε, which we compact as
     * <var>precision</var> *= (1 + ε). The ε value is chosen to represent an increase
     * of no more than 0.5 pixel between the lower and upper indices of the grid.
     * This array may be {@code null} if it has not been computed.
     */
    private double[] inflatePrecisions;

    /**
     * The object to use for formatting coordinate values.
     */
    private final CoordinateFormat format;

    /**
     * The labels where to format the cursor position, either as coordinate values or other representations.
     * The text is usually the result of formatting {@link #targetCoordinates} as numerical values,
     * but may also be other representations such as Military Grid Reference System (MGRS) codes.
     */
    private final Label position;

    /**
     * The {@link #position} text to show when the mouse is outside the canvas area.
     * This text is set to the axis abbreviations, for example "(φ, λ)".
     *
     * @see #setFormatCRS(CoordinateReferenceSystem)
     */
    private String outsideText;

    /**
     * The canvas that this status bar is tracking.
     * The property value is {@code null} if there is none.
     *
     * @see #getCanvas()
     * @see #setCanvas(MapCanvas)
     */
    public final ObjectProperty<MapCanvas> canvasProperty;

    /**
     * The listener registered on {@link MapCanvas#renderingProperty()}, or {@code null} if the
     * listener has not yet been registered. This listener is remembered for allowing removal.
     *
     * @see #setCanvas(MapCanvas)
     * @see #onCanvasSpecified(ObservableValue, MapCanvas, MapCanvas)
     */
    private ChangeListener<Boolean> renderingListener;

    /**
     * Whether the mouse listeners have been registered. Those listeners are registered the
     * first time that {@link #apply(GridGeometry)} is invoked on a newly initialized canvas.
     */
    private boolean isMouseListenerRegistered;

    /**
     * Creates a new status bar for showing coordinates of mouse cursor position in a canvas.
     * If the {@code choices} argument is non-null, user will be able to select different CRS
     * using the contextual menu on the status bar.
     *
     * @param  systemChooser  the manager of reference systems chosen by user, or {@code null} if none.
     */
    public StatusBar(final RecentReferenceSystems systemChooser) {
        localToObjectiveCRS = MathTransforms.identity(BIDIMENSIONAL);
        localToFormatCRS    = localToObjectiveCRS;
        targetCoordinates   = new GeneralDirectPosition(BIDIMENSIONAL);
        sourceCoordinates   = targetCoordinates.coordinates;
        lastX = lastY       = Double.NaN;
        format              = new CoordinateFormat();
        position            = new Label();
        message             = new Label();
        message.setVisible(false);                      // Waiting for getting a message to display.
        message.setTextFill(Styles.ERROR_TEXT);
        message.setMaxWidth(Double.POSITIVE_INFINITY);
        HBox.setHgrow(message, Priority.ALWAYS);
        view = new HBox(18, message, position);
        view.setPadding(PADDING);
        view.setAlignment(Pos.CENTER_RIGHT);
        position.setAlignment(Pos.CENTER_RIGHT);
        position.setTextAlignment(TextAlignment.RIGHT);
        canvasProperty = new SimpleObjectProperty<>(this, "canvas");
        canvasProperty.addListener(this::onCanvasSpecified);
        if (systemChooser == null) {
            areaOfInterest = null;
        } else {
            areaOfInterest = systemChooser.areaOfInterest;
            final Menu choices = systemChooser.createMenuItems((property, oldValue, newValue) -> {
                findFormatOperation(newValue instanceof CoordinateReferenceSystem ? (CoordinateReferenceSystem) newValue : null);
            });
            final ContextMenu menu = new ContextMenu(choices);
            view.setOnMousePressed((event) -> {
                if (event.isSecondaryButtonDown()) {
                    menu.show((HBox) event.getSource(), event.getScreenX(), event.getScreenY());
                } else {
                    menu.hide();
                }
            });
        }
    }

    /**
     * Returns the node to add to the scene graph for showing the status bar.
     */
    @Override
    public final Region getView() {
        return view;
    }

    /**
     * Returns the canvas that this status bar is tracking.
     *
     * @return canvas that this status bar is tracking, or {@code null} if none.
     *
     * @see #canvasProperty
     */
    public final MapCanvas getCanvas() {
        return canvasProperty.get();
    }

    /**
     * Sets the canvas that this status bar is tracking. After this method has been invoked,
     * this {@code StatusBar} will show coordinates (usually geographic or projected) below
     * mouse cursor when the mouse is over that canvas.
     *
     * @param  canvas  the canvas to track, or {@code null} if none.
     *
     * @see #canvasProperty
     */
    public final void setCanvas(final MapCanvas canvas) {
        canvasProperty.set(canvas);
    }

    /**
     * Invoked when a new value is set on {@link #canvasProperty}. Previous listeners (if any) are removed
     * but new mouse listeners may not be added immediately. Instead if the canvas seems uninitialized, we
     * will wait for the first call to {@link #apply(GridGeometry)} before to add the listener. We do that
     * for avoiding to show irrelevant coordinate values.
     */
    private void onCanvasSpecified(final ObservableValue<? extends MapCanvas> property,
                                   final MapCanvas previous, final MapCanvas value)
    {
        if (previous != null) {
            previous.floatingPane.removeEventHandler(MouseEvent.MOUSE_ENTERED, this);
            previous.floatingPane.removeEventHandler(MouseEvent.MOUSE_EXITED,  this);
            previous.floatingPane.removeEventHandler(MouseEvent.MOUSE_MOVED,   this);
            previous.renderingProperty().removeListener(renderingListener);
            renderingListener = null;
            isMouseListenerRegistered = false;
        }
        if (value != null) {
            value.renderingProperty().addListener(renderingListener = new RenderingListener());
        }
        position.setText(null);
        registerMouseListeners(value);
        try {
            apply(value != null ? value.getGridGeometry() : null);
        } catch (RenderException e) {
            setErrorMessage(null, e);
        }
    }

    /**
     * Registers mouse listeners for the given canvas if not null. It is caller responsibility to invoke
     * this method only if {@link #isMouseListenerRegistered} is {@code false} (this method does not verify).
     *
     * @see #apply(GridGeometry)
     */
    private void registerMouseListeners(final MapCanvas value) {
        /*
         * The canvas "objective to CRS" is null only for unitialized canvas.
         * After the canvas has been initialized, it can not be null anymore.
         * We use that for deciding if listener registration should be delayed.
         */
        if (value != null && value.getObjectiveCRS() != null) {
            // Set first for avoiding duplicated registrations if an exception happen.
            isMouseListenerRegistered = true;
            value.floatingPane.addEventHandler(MouseEvent.MOUSE_ENTERED, this);
            value.floatingPane.addEventHandler(MouseEvent.MOUSE_EXITED,  this);
            value.floatingPane.addEventHandler(MouseEvent.MOUSE_MOVED,   this);
        }
    }

    /**
     * Listener notified when {@link MapCanvas} completed its rendering. This listener sets
     * {@link StatusBar#localToObjectiveCRS} to the inverse of {@link MapCanvas#objectiveToDisplay}.
     * It assumes that even if the JavaFX local coordinates and {@link #localToFormatCRS} transform
     * changed, the "real world" coordinates under the mouse cursor is still the same. This assumption
     * should be true if this listener is notified as a result of zoom, translation or rotation events.
     */
    private final class RenderingListener implements ChangeListener<Boolean> {
        @Override public void changed(final ObservableValue<? extends Boolean> property,
                                      final Boolean previous, final Boolean value)
        {
            if (!value) try {
                apply(getCanvas().getGridGeometry());
                reformat();
            } catch (RenderException e) {
                setErrorMessage(null, e);
            }
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
     *       (the {@linkplain #getLocalToObjectiveCRS() local to objective CRS} conversion).</li>
     *   <li>{@link GridGeometry#getExtent()} provides the view size in pixels, used for estimating a resolution.</li>
     *   <li>{@link GridGeometry#getResolution(boolean)} is also used for estimating a resolution.</li>
     * </ul>
     *
     * All above properties are optional. The "local to objective CRS" conversion can be updated
     * after this method call with {@link #setLocalToObjectiveCRS(MathTransform)}.
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
             * fraction digits to shown in coordinates.
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
        /*
         * Remaining code should not fail, so we can start modifying the `StatusBar` fields.
         * The buffers for source and target coordinates are recreated because the number of
         * dimensions may have changed. The `lastX` and `lastY` coordinates are local to the
         * JavaFX view and considered invalid  because they depend on the transforms applied
         * on JavaFX node, which may have changed together with `localToObjectiveCRS` change.
         * So we can not use those values for updating the coordinates shown in status bar.
         * Instead we will wait for the next mouse event to provide new local coordinates.
         */
        if (localToCRS != null) {
            sourceCoordinates = new double[Math.max(localToCRS.getSourceDimensions(), BIDIMENSIONAL)];
            targetCoordinates = new GeneralDirectPosition(localToCRS.getTargetDimensions());
        } else {
            localToCRS        = MathTransforms.identity(BIDIMENSIONAL);
            targetCoordinates = new GeneralDirectPosition(BIDIMENSIONAL);
            sourceCoordinates = targetCoordinates.coordinates;      // Okay to share array if same dimension.
        }
        objectiveCRS        = crs;
        localToObjectiveCRS = localToCRS;
        localToFormatCRS    = localToCRS;                           // May be updated again below.
        inflatePrecisions   = inflate;
        precisions          = null;
        lastX = lastY       = Double.NaN;                           // Not valid anymove — see above block comment.
        CoordinateReferenceSystem restore = null;
        if (sameCRS) {
            if (objectiveToFormatCRS != null) {
                localToFormatCRS = MathTransforms.concatenate(localToCRS, objectiveToFormatCRS.getMathTransform());
            }
            // Keep the format CRS unchanged since we made `localToFormatCRS` consistent with its value.
        } else {
            objectiveToFormatCRS = null;
            restore = format.getDefaultCRS();           // CRS to restore in a background thread.
            setFormatCRS(crs);                          // Should be invoked before to set precision.
        }
        format.setGroundPrecision(Quantities.create(resolution, unit));
        if (ReferencingUtilities.getDimension(restore) == localToFormatCRS.getTargetDimensions()) {
            findFormatOperation(restore);               // Not executed if `restore` is null.
        }
        /*
         * If this is the first time that this method is invoked after `setCanvas(MapCanvas)`,
         * the listeners are not yet registered and should be added now. Listeners registration
         * was delayed because if they were added on uninitialized canvas, they would have show
         * irrelevant coordinates.
         */
        if (geometry != null && !isMouseListenerRegistered) {
            registerMouseListeners(canvasProperty.getValue());
        }
    }

    /**
     * Sets the coordinate reference system of the coordinates shown in this status bar.
     * The change may not appear immediately after method return; this method may use a
     * background thread for computing the coordinate operation.  That task may be long
     * the first time that it is executed, but should be fast on subsequent invocations.
     *
     * @param  crs  the new CRS, or {@code null} for {@link #objectiveCRS}.
     */
    private void findFormatOperation(final CoordinateReferenceSystem crs) {
        if (crs != null && objectiveCRS != null && objectiveCRS != crs) {
            position.setTextFill(Styles.OUTDATED_TEXT);
            final Envelope aoi = (areaOfInterest != null) ? areaOfInterest.get() : null;
            BackgroundThreads.execute(new Task<MathTransform>() {
                /**
                 * The new {@link StatusBar#objectiveToFormatCRS} value if successful.
                 */
                private CoordinateOperation operation;

                /**
                 * Invoked in a background thread for fetching transformation to target CRS.
                 * This operation may be long the first time that it is executed, but should
                 * be fast on subsequent invocations.
                 */
                @Override protected MathTransform call() throws FactoryException {
                    DefaultGeographicBoundingBox bbox = null;
                    if (aoi != null) try {
                        bbox = new DefaultGeographicBoundingBox();
                        bbox.setBounds(aoi);
                    } catch (TransformException e) {
                        bbox = null;
                        Logging.recoverableException(Logging.getLogger(Modules.APPLICATION),
                                StatusBar.class, "findFormatOperation", e);
                    }
                    operation = CRS.findOperation(objectiveCRS, crs, bbox);
                    return MathTransforms.concatenate(localToObjectiveCRS, operation.getMathTransform());
                }

                /**
                 * Invoked in JavaFX thread on success. The {@link StatusBar#localToFormatCRS} transform
                 * is set to the transform that we computed in background and the {@link CoordinateFormat}
                 * is configured with auxiliary information such as positional accuracy.
                 */
                @Override protected void succeeded() {
                    final CoordinateReferenceSystem targetCRS = operation.getTargetCRS();
                    setFormatCRS(targetCRS != null ? targetCRS : crs, operation, getValue());
                }

                /**
                 * Invoked in JavaFX thread on failure. The previous CRS is kept unchanged but
                 * the coordinates will appear in red for telling user that there is a problem.
                 */
                @Override protected void failed() {
                    final Locale locale = format.getLocale(Locale.Category.DISPLAY);
                    setErrorMessage(Resources.forLocale(locale).getString(Resources.Keys.CanNotUseRefSys_1,
                                    IdentifiedObjects.getDisplayName(crs, locale)), getException());
                    resetFormatCRS(Styles.ERROR_TEXT);
                }
            });
        } else {
            /*
             * If the requested CRS is the objective CRS, avoid above costly operation.
             * The work that we need to do is to cancel the effect of `localToFormatCRS`.
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
            resetFormatCRS(Styles.NORMAL_TEXT);
        }
    }

    /**
     * Invoked after the background thread computed the new coordinate operation.
     * This method rewrites the coordinates on the assumption that {@link #lastX}
     * and {@link #lastY} are still valid. This assumption should be correct when
     * only the format CRS has been updated and not {@link #localToObjectiveCRS}.
     *
     * @param  crs        the new CRS. Should not be {@code null}.
     * @param  operation  the new value to assign to {@link #objectiveToFormatCRS}
     * @param  complete   the concatenation of {@link #localToObjectiveCRS} with {@code operation}.
     */
    private void setFormatCRS(final CoordinateReferenceSystem crs,
            final CoordinateOperation operation, final MathTransform complete)
    {
        setFormatCRS(crs);
        objectiveToFormatCRS = operation;
        localToFormatCRS = complete;
//      TODO: CRS.getLinearAccuracy(op);
        position.setTextFill(Styles.NORMAL_TEXT);
        position.setMinWidth(0);
        setErrorMessage(null, null);
        reformat();
    }

    /**
     * Sets the {@link CoordinateFormat} default CRS together with the tool tip text.
     * Caller is responsible to setup transforms ({@link #localToFormatCRS}, <i>etc</i>).
     *
     * @param  crs  the new {@link #format} reference system.
     *
     * @see #getFormatReferenceSystem()
     */
    private void setFormatCRS(final CoordinateReferenceSystem crs) {
        format.setDefaultCRS(crs);
        String text = IdentifiedObjects.getDisplayName(crs, format.getLocale(Locale.Category.DISPLAY));
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
         * Prepare the text to show when the moust is outside the canvas area.
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
                    text = b.append(')').toString();
                }
            }
        }
        if (position.getText() == outsideText) {          // Identity comparison is okay for this value.
            position.setText(text);
        }
        outsideText = text;
    }

    /**
     * Reformats the coordinates shown in {@link #position} using current {@link #lastX} and {@link #lastY} values.
     * This method should be invoked only when the caller knows that those values are still valid. Note that those
     * values may be invalid if {@link javafx.scene.Node#getTransforms()} changed even if {@link #objectiveCRS} is
     * the same.
     */
    private void reformat() {
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
     * Resets {@link #localToFormatCRS} to its default value. This is invoked either when the
     * target CRS is {@link #objectiveCRS}, or when an attempt to use another CRS failed.
     */
    private void resetFormatCRS(final Color textFill) {
        objectiveToFormatCRS = null;
        localToFormatCRS = localToObjectiveCRS;
        setFormatCRS(objectiveCRS);
        position.setTextFill(textFill);
    }

    /**
     * Returns the reference systems used by the coordinates shown in this status bar.
     * This is initially the same value than {@link #getObjectiveCRS()}, but may become
     * different if the user selects another reference system through contextual menu.
     *
     * @return reference systems used by the coordinates shown in this status bar.
     */
    public final Optional<ReferenceSystem> getFormatReferenceSystem() {
        return Optional.ofNullable(format.getDefaultCRS());
    }

    /**
     * Returns the reference system used for rendering the data for which this status bar is providing cursor coordinates.
     * This is the "{@linkplain RecentReferenceSystems#setPreferred(boolean, ReferenceSystem) preferred}" or native
     * data CRS. It may not be the same than the CRS of coordinates actually shown in the status bar.
     *
     * @return the reference system used for rendering the data for which this status bar
     *         is providing cursor coordinates, or {@code null} if unknown.
     *
     * @see MapCanvas#getObjectiveCRS()
     */
    public final Optional<CoordinateReferenceSystem> getObjectiveCRS() {
        return Optional.ofNullable(objectiveCRS);
    }

    /**
     * Returns the conversion from local coordinates to geographic or projected coordinates of rendered data.
     * The local coordinates are the coordinates of the JavaFX view, as given for example in {@link MouseEvent}.
     * This is initially an identity transform and can be computed by {@link #applyCanvasGeometry(GridGeometry)}.
     * This transform ignores all CRS changes resulting from user selecting a different CRS in the contextual menu.
     * This transform is usually (but not necessarily) affine.
     *
     * @return conversion from local coordinates to "real world" coordinates of rendered data.
     *         This is not necessarily the conversion to coordinates shown in the status bar.
     *
     * @see MapCanvas#getObjectiveToDisplay()
     */
    public final MathTransform getLocalToObjectiveCRS() {
        return localToObjectiveCRS;
    }

    /**
     * Sets the conversion from local coordinates to geographic or projected coordinates of rendered data.
     * The given transform must have the same number of source and target dimensions than the previous value
     * (if a change in the number of dimension is desired, use {@link #applyCanvasGeometry(GridGeometry)} instead).
     * The conversion should have no {@linkplain CoordinateOperation#getCoordinateOperationAccuracy() inaccuracy}
     * (ignoring rounding error). The status bar is updated as if the new conversion was applied <em>before</em>
     * any CRS changes resulting from user selecting a different CRS in the contextual menu.
     *
     * @param  conversion  the new conversion from local coordinates to "real world" coordinates of rendered data.
     * @throws MismatchedDimensionException if the number of dimensions is not the same than previous conversion.
     */
    public final void setLocalToObjectiveCRS(final MathTransform conversion) {
        ArgumentChecks.ensureNonNull("conversion", conversion);
        int expected = localToObjectiveCRS.getSourceDimensions();
        int actual   = conversion.getSourceDimensions();
        if (expected == actual) {
            expected = localToObjectiveCRS.getTargetDimensions();
            actual   = conversion.getTargetDimensions();
            if (expected == actual) {
                localToObjectiveCRS = conversion;
                findFormatOperation(format.getDefaultCRS());                    // Recompute `localToFormatCRS`.
                return;
            }
        }
        throw new MismatchedDimensionException(Errors.format(
                Errors.Keys.MismatchedDimension_3, "conversion", expected, actual));
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
            String text;
            try {
                Matrix derivative;
                try {
                    derivative = MathTransforms.derivativeAndTransform(localToFormatCRS,
                            sourceCoordinates, 0, targetCoordinates.coordinates, 0);
                } catch (TransformException ignore) {
                    /*
                     * If above operation failed, it may be because the MathTransform does not support
                     * derivative calculation. Try again without derivative (the precision will be set
                     * to the default resolution computed in `setCanvasGeometry(…)`).
                     */
                    localToFormatCRS.transform(sourceCoordinates, 0, targetCoordinates.coordinates, 0, 1);
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
                     * a displacement of one cell (i.e. when moving by one row or column).  We search for
                     * maximal displacement instead than minimal because we expect the displacement to be
                     * zero along some axes (e.g. one row down does not change longitude value in a Plate
                     * Carrée projection).
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
                text = format.format(targetCoordinates);
            } catch (TransformException | RuntimeException e) {
                /*
                 * If even the fallback without derivative failed, show the error message.
                 */
                Throwable cause = Exceptions.unwrap(e);
                text = cause.getLocalizedMessage();
                if (text == null) {
                    text = Classes.getShortClassName(cause);
                }
            }
            position.setText(text);
            /*
             * Make sure that there is enough space for keeping the coordinates always visible.
             * This is the needed if there is an error message on the left which may be long.
             */
            final double width = Math.min(view.getWidth() / 2, Math.ceil(position.prefWidth(position.getHeight())));
            if (width > position.getMinWidth()) {
                position.setMinWidth(width);
            }
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
         * Do not use `position.setVisible(false)` because
         * we want the Tooltip to continue to be available.
         */
        position.setText(outsideText);
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
            final Locale locale = format.getLocale(Locale.Category.DISPLAY);
            if (text == null) {
                text = Exceptions.getLocalizedMessage(details, locale);
                if (text == null) {
                    text = details.getClass().getSimpleName();
                }
            }
            final String alert = text;
            more = new Button(Styles.ERROR_DETAILS_ICON);
            more.setOnAction((e) -> ExceptionReporter.show(
                    Resources.forLocale(locale).getString(Resources.Keys.ErrorDetails), alert, details));
        }
        message.setVisible(text != null);
        message.setGraphic(more);
        message.setText(text);
        message.setTextFill(Styles.ERROR_TEXT);
    }
}
