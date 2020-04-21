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
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.portrayal.RenderException;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.gui.Widget;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.Styles;


/**
 * A status bar showing geographic or projected coordinates under mouse cursor.
 * The number of fraction digits is adjusted according pixel resolution for each coordinate to format.
 * Other components such as progress bar or error message may also be shown.
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
    private static final Insets PADDING = new Insets(5, Styles.SCROLLBAR_WIDTH, 6, 0);

    /**
     * The container of controls making the status bar.
     */
    private final HBox view;

    /**
     * The progress bar, hidden by default. This bar is initialized to undetermined state.
     */
    private final ProgressBar progress;

    /**
     * Message to write in the middle of the status bar.
     * This component usually has nothing to show; it is used mostly for error messages.
     * It takes all the space between {@link #progress} and {@link #coordinates}.
     */
    private final Label message;

    /**
     * Local coordinates currently formatted in the {@link #coordinates} field.
     * This is used for detecting if coordinate values changed since last formatting.
     * Those coordinates are often integer values.
     */
    private double lastX, lastY;

    /**
     * Conversion from local coordinates to geographic or projected coordinates.
     * This conversion shall never be null but may be the identity transform.
     */
    private MathTransform localToCRS;

    /**
     * The source local indices before conversion to geospatial coordinates.
     * The number of dimensions is often {@value #BIDIMENSIONAL}.
     * Shall never be {@code null}.
     */
    private double[] sourceCoordinates;

    /**
     * Coordinates after conversion to the CRS. The number of dimensions depends on
     * the target CRS. This object is reused during each coordinate transformation.
     * Shall never be {@code null}.
     */
    private GeneralDirectPosition targetCoordinates;

    /**
     * The desired precisions for each dimension in the {@link #targetCoordinates} to format.
     * It may vary for each position if the {@link #localToCRS} transform is non-linear.
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
     * The labels where to format the coordinates.
     */
    private final Label coordinates;

    /**
     * The canvas that this status bar is tracking.
     * The property value is {@code null} if there is none.
     *
     * @see #getCanvas()
     * @see #setCanvas(MapCanvas)
     */
    public final ObjectProperty<MapCanvas> canvasProperty;

    /**
     * The listener registered on {@link MapCanvas#renderingProperty()}.
     * This reference is stored for allowed removal.
     *
     * @see #setCanvas(MapCanvas)
     */
    private ChangeListener<Boolean> renderingListener;

    /**
     * Creates a new status bar.
     *
     * @param  referenceSystems  the manager of reference systems chosen by the user, or {@code null} if none.
     */
    public StatusBar(final RecentReferenceSystems referenceSystems) {
        localToCRS        = MathTransforms.identity(BIDIMENSIONAL);
        targetCoordinates = new GeneralDirectPosition(BIDIMENSIONAL);
        sourceCoordinates = targetCoordinates.coordinates;
        lastX = lastY     = Double.NaN;
        format            = new CoordinateFormat();
        coordinates       = new Label();
        message           = new Label();
        progress          = new ProgressBar();
        progress.setVisible(false);
        message.setTextFill(Color.RED);
        message.setMaxWidth(Double.POSITIVE_INFINITY);
        HBox.setHgrow(message, Priority.ALWAYS);
        view = new HBox(12, progress, message, coordinates);
        view.setPadding(PADDING);
        canvasProperty = new SimpleObjectProperty<>(this, "canvas");
        canvasProperty.addListener(this::onCanvasSpecified);
        if (referenceSystems != null) {
            final ContextMenu menu = new ContextMenu(referenceSystems.createMenuItems((e,o,n) -> setDisplayCRS(n)));
            view.setOnMousePressed((MouseEvent event) -> {
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
     * Sets the canvas that this status bar is tracking.
     * This method register all necessary listeners.
     * A value of {@code null} unregister all listeners.
     *
     * @param  canvas  the canvas to track, or {@code null} if none.
     *
     * @see #canvasProperty
     */
    public final void setCanvas(final MapCanvas canvas) {
        canvasProperty.set(canvas);
    }

    /**
     * Invoked when a new value is set on {@link #canvasProperty}.
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
        }
        if (value != null) {
            value.floatingPane.addEventHandler(MouseEvent.MOUSE_ENTERED, this);
            value.floatingPane.addEventHandler(MouseEvent.MOUSE_EXITED,  this);
            value.floatingPane.addEventHandler(MouseEvent.MOUSE_MOVED,   this);
            value.renderingProperty().addListener(renderingListener = new RenderingListener());
        }
    }

    /**
     * Listener notified when {@link MapCanvas} completed its rendering.
     * This listener set {@link StatusBar#localToCRS} to the inverse of
     * {@link MapCanvas#objectiveToDisplay}.
     */
    private final class RenderingListener implements ChangeListener<Boolean> {
        @Override public void changed(final ObservableValue<? extends Boolean> property,
                                      final Boolean previous, final Boolean value)
        {
            progress.setVisible(value);
            if (!value) try {
                applyCanvasGeometry(getCanvas().getGridGeometry());
            } catch (RenderException e) {
                setErrorMessage(null, e);
            }
        }
    }

    /**
     * Configures this status bar for showing coordinates in the CRS and resolution given by the specified
     * grid geometry. The geometry properties are applied as below:
     *
     * <ul>
     *   <li>{@link GridGeometry#getCoordinateReferenceSystem()} defines the CRS of the coordinates to format.</li>
     *   <li>{@link GridGeometry#getGridToCRS(PixelInCell) GridGeometry.getGridToCRS(PixelInCell.CELL_CENTER)}
     *       defines the conversion from coordinate values locale to the canvas to coordinate values in the CRS
     *       (the {@linkplain #getLocalToCRS() local to CRS} conversion).</li>
     *   <li>{@link GridGeometry#getExtent()} provides the view size in pixels, used for estimating a resolution.</li>
     *   <li>{@link GridGeometry#getResolution(boolean)} is also used for estimating a resolution.</li>
     * </ul>
     *
     * All above properties are optional.
     * The "local to CRS" conversion can be updated after this method call with {@link #setLocalToCRS(MathTransform)}.
     *
     * @param  geometry  geometry of the coverage shown in {@link MapCanvas}, or {@code null}.
     */
    public void applyCanvasGeometry(final GridGeometry geometry) {
        localToCRS = null;
        precisions = null;
        inflatePrecisions = null;
        CoordinateReferenceSystem crs = null;
        double resolution = 1;
        Unit<?> unit = Units.PIXEL;
        if (geometry != null) {
            if (geometry.isDefined(GridGeometry.GRID_TO_CRS)) {
                localToCRS = geometry.getGridToCRS(PixelInCell.CELL_CENTER);
                if (geometry.isDefined(GridGeometry.CRS)) {
                    crs = geometry.getCoordinateReferenceSystem();
                }
            }
            /*
             * Computes the precision of coordinates to format. We use the finest resolution,
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
                inflatePrecisions = new double[n];
                for (int i=0; i<n; i++) {
                    inflatePrecisions[i] = (0.5 / extent.getSize(i)) + 1;
                }
            }
        }
        /*
         * Prepare objects to be reused for each coordinate transformation.
         * Configure the `CoordinateFormat` with the CRS.
         */
        if (localToCRS != null) {
            sourceCoordinates = new double[Math.max(localToCRS.getSourceDimensions(), BIDIMENSIONAL)];
            targetCoordinates = new GeneralDirectPosition(localToCRS.getTargetDimensions());
        } else {
            localToCRS        = MathTransforms.identity(BIDIMENSIONAL);
            targetCoordinates = new GeneralDirectPosition(BIDIMENSIONAL);
            sourceCoordinates = targetCoordinates.coordinates;      // Okay to share array if same dimension.
        }
        setDisplayCRS(crs);
        format.setPrecision(resolution, unit);
        lastX = lastY = Double.NaN;
    }

    /**
     * Sets the coordinate reference systems to use for representing coordinates in status bar.
     *
     * @param  crs  the coordinate reference system to use for coordinates in the status bar.
     */
    private void setDisplayCRS(final ReferenceSystem crs) {
        if (crs instanceof CoordinateReferenceSystem) {
            format.setDefaultCRS((CoordinateReferenceSystem) crs);
        } else {
            format.setDefaultCRS(null);
        }
    }

    /**
     * Returns the conversion from local coordinates to geographic or projected coordinates.
     * The local coordinates are the coordinates of the view, as given for example in {@link MouseEvent}.
     * This is initially an identity transform and can be computed by {@link #applyCanvasGeometry(GridGeometry)}.
     *
     * @return conversion from local coordinates to "real world" coordinates.
     */
    public final MathTransform getLocalToCRS() {
        return localToCRS;
    }

    /**
     * Sets the conversion from local coordinates to geographic or projected coordinates.
     * The given value must have the same number of source and target dimensions than the
     * previous value. If a change in the number of dimension is desired,
     * use {@link #applyCanvasGeometry(GridGeometry)} instead.
     *
     * @param  conversion  the new conversion from local coordinates to "real world" coordinates.
     * @throws MismatchedDimensionException if the number of dimensions is not the same than previous conversion.
     */
    public final void setLocalToCRS(final MathTransform conversion) {
        ArgumentChecks.ensureNonNull("conversion", conversion);
        int expected = localToCRS.getSourceDimensions();
        int actual   = conversion.getSourceDimensions();
        if (expected == actual) {
            expected = localToCRS.getTargetDimensions();
            actual   = conversion.getTargetDimensions();
            if (expected == actual) {
                localToCRS = conversion;
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
        if (coordinates.isVisible() && !Double.isNaN(lastX) && !Double.isNaN(lastY)) {
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
                    derivative = MathTransforms.derivativeAndTransform(localToCRS,
                            sourceCoordinates, 0, targetCoordinates.coordinates, 0);
                } catch (TransformException ignore) {
                    /*
                     * If above operation failed, it may be because the MathTransform does not support
                     * derivative calculation. Try again without derivative (the precision will be set
                     * to the default resolution computed in `setCanvasGeometry(…)`).
                     */
                    localToCRS.transform(sourceCoordinates, 0, targetCoordinates.coordinates, 0, 1);
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
            coordinates.setText(text);
            coordinates.setVisible(true);
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
        coordinates.setVisible(false);
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
