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
package org.apache.sis.gui.coverage;

import java.util.Map;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.LogRecord;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.util.Duration;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Shapes2D;
import org.apache.sis.image.Colorizer;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.Interpolation;
import org.apache.sis.image.processing.isoline.Isolines;
import org.apache.sis.image.internal.shared.TileErrorHandler;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.tiling.TileReadEvent;
import org.apache.sis.gui.map.MapCanvas;
import org.apache.sis.gui.map.MapCanvasAWT;
import org.apache.sis.portrayal.RenderException;
import org.apache.sis.map.coverage.RenderingWorkaround;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.gui.internal.ShapeConverter;
import org.apache.sis.gui.internal.GUIUtilities;
import org.apache.sis.gui.internal.LogHandler;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.io.TableAppender;
import org.apache.sis.measure.Units;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;


/**
 * A canvas for {@link RenderedImage} provided by a {@link GridCoverage} or a {@link GridCoverageResource}.
 * In the latter case where the source of data is specified by {@link #resourceProperty}, the grid coverage
 * instance (given by {@link #coverageProperty}) will change automatically according the zoom level.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.7
 *
 * @see CoverageExplorer
 *
 * @since 1.1
 */
@DefaultProperty("coverage")
public class CoverageCanvas extends MapCanvasAWT {
    /**
     * The default background for this canvas.
     */
    private static final Background BACKGROUND;
    static {
        Background background = null;
        try (InputStream in = CoverageCanvas.class.getResourceAsStream("Background.png")) {
            if (in != null) {
                background = new Background(new BackgroundImage(new Image(in), null, null, null, null));
            }
        } catch (IOException e) {
            Logging.unexpectedException(LOGGER, CoverageCanvas.class, "BACKGROUND", e);
        }
        BACKGROUND = background;
    }

    /**
     * An arbitrary safety margin (in number of pixels) for avoiding integer overflow situation.
     * This margin shall be larger than any reasonable widget width or height, and much smaller
     * than {@link Integer#MAX_VALUE}.
     */
    private static final int OVERFLOW_SAFETY_MARGIN = 10_000_000;

    /**
     * Maximal surface or volume (in number of cells) of the data to load.
     * If showing the full image would cause a larger amount of data to be loaded,
     * then the widget will zoom on a smaller area by default. This is based on the
     * assumption that data are tiled, and therefore zooming will reduce the amount
     * of data to load.
     */
    private static final int MAXIMAL_CELL_COUNT = 5000 * 5000;

    /**
     * Whether to print debug information. If {@code true}, we use {@link System#out} instead of logging
     * because the log messages are intercepted and rerouted to the "logging" tab in the explorer widget.
     * This field should always be {@code false} except during debugging.
     *
     * @see #trace(String, Object...)
     */
    @Debug
    static final boolean TRACE = false;

    /**
     * The source of coverage data shown in this canvas. If this property value is non-null,
     * then {@link #coverageProperty} value will change at any time (potentially many times)
     * depending on the zoom level or other user interaction. Conversely if a value is set
     * explicitly on {@link #coverageProperty}, then this {@code resourceProperty} is cleared.
     *
     * @see #getResource()
     * @see #setResource(GridCoverageResource)
     * @see CoverageExplorer#resourceProperty
     *
     * @since 1.2
     */
    public final ObjectProperty<GridCoverageResource> resourceProperty;

    /**
     * The data shown in this canvas. This property value may be set implicitly or explicitly:
     * <ul>
     *   <li>If the {@link #resourceProperty} value is non-null, then the value will change
     *       automatically at any time (potentially many times) depending on user interaction.</li>
     *   <li>Conversely if an explicit value is set on this property,
     *       then the {@link #resourceProperty} is cleared.</li>
     * </ul>
     *
     * Note that a change in this property value may not modify the canvas content immediately.
     * Instead, a background process will request the tiles and update the canvas content later,
     * when data are ready.
     *
     * <p>Current implementation is restricted to {@link GridCoverage} instances, but a future
     * implementation may generalize to {@code org.opengis.coverage.Coverage} instances.</p>
     *
     * @see #getCoverage()
     * @see #setCoverage(GridCoverage)
     * @see CoverageExplorer#coverageProperty
     */
    public final ObjectProperty<GridCoverage> coverageProperty;

    /**
     * Whether {@link #resourceProperty} or {@link #coverageProperty} is in process of being adjusted.
     * This is used for preventing never-ending loop when a change of resource causes a change of coverage
     * or conversely.
     *
     * @see #onPropertySpecified(GridCoverageResource, GridCoverageResource, GridCoverage, ObjectProperty, GridGeometry)
     */
    private boolean isCoverageAdjusting;

    /**
     * Whether at least one of {@link #coverageProperty} or {@link #resourceProperty} has a non-null value.
     */
    private boolean hasCoverageOrResource;

    /**
     * Whether to skip the rendering of the coverage.
     * OTher features such as the isolines may still be rendered.
     *
     * @see #setCoverageHidden(boolean)
     */
    private boolean isCoverageHidden;

    /**
     * A subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     * May be {@code null} if the grid coverage has only two dimensions with a size greater than 1 cell.
     *
     * @see #getSliceExtent()
     * @see #setSliceExtent(GridExtent)
     * @see GridCoverage#render(GridExtent)
     */
    public final ObjectProperty<GridExtent> sliceExtentProperty;

    /**
     * The interpolation method to use for resampling the image.
     *
     * @see #getInterpolation()
     * @see #setInterpolation(Interpolation)
     */
    public final ObjectProperty<Interpolation> interpolationProperty;

    /**
     * The {@code RenderedImage} to draw together with transform from pixel coordinates to display coordinates.
     * Shall never be {@code null} but may be empty. This instance shall be read and modified in JavaFX thread
     * only and cloned if those data are needed by a background thread.
     *
     * @see Worker
     */
    private StyledRenderingData data;

    /**
     * The {@link #data} with different operations applied on them. Currently the only supported operation is
     * color ramp stretching. The coordinate system is the one of the original image (no resampling applied).
     */
    private final Map<Stretching, RenderedImage> derivedImages;

    /**
     * Image resampled to a CRS which can easily be mapped to the {@linkplain #getDisplayCRS() display CRS}.
     * May also include conversion to integer values for usage with index color model.
     * This is the image which will be drawn in the canvas.
     */
    private RenderedImage resampledImage;

    /**
     * The explorer to notify when the image shown in this canvas has changed.
     * This is non-null only if this {@link CoverageCanvas} is used together with {@link CoverageControls}.
     *
     * <p>Consider as final after {@link #createPropertyExplorer()} invocation.
     * This field may be removed in a future version if we revisit this API before making public.</p>
     *
     * @see #createPropertyExplorer()
     */
    private ImagePropertyExplorer propertyExplorer;

    /**
     * If this canvas is associated with controls, the controls. Otherwise {@code null}.
     * This is used only for notifications; a future version may use a more generic listener.
     *
     * @see CoverageControls#notifyDataChanged(GridCoverageResource, GridCoverage)
     */
    private final CoverageControls controls;

    /**
     * If errors occurred during tile computations, details about the error. Otherwise {@code null}.
     * This field is set once by some background thread if one or more errors occurred during calls
     * to {@link RenderedImage#getTile(int, int)}. In such case, we store information about the error
     * and let the rendering process continue with a tile placeholder (by default a cross (X) in a box).
     * This field is read in JavaFX thread for transferring the error description to {@link #errorProperty()}.
     */
    private volatile LogRecord errorReport;

    /**
     * Renderer of isolines, or {@code null} if none. The presence of this field in this class may be temporary.
     * A future version may replace this field by a more complete styling framework. Note that this class holds
     * references to {@link javafx.scene.control.TableView} list of items, which are the list of isoline levels
     * with their colors.
     */
    IsolineController isolines;

    /**
     * Listener notified when tiles are read, for showing them on top of the image as translucent tiles.
     * This is {@code null} if this effect is not shown.
     */
    private TileReadListener tileReadListener;

    /**
     * Creates a new two-dimensional canvas for {@link RenderedImage}.
     */
    public CoverageCanvas() {
        this(null, Locale.getDefault());
    }

    /**
     * Creates a new two-dimensional canvas using the given locale.
     *
     * @param  controls  the controls of this canvas, or {@code null} if none.
     * @param  locale    the locale to use for labels and some messages, or {@code null} for default.
     */
    @SuppressWarnings("this-escape")
    CoverageCanvas(final CoverageControls controls, final Locale locale) {
        super(locale);
        this.controls         = controls;
        data                  = new StyledRenderingData((report) -> errorReport = report.getDescription());
        derivedImages         = new EnumMap<>(Stretching.class);
        resourceProperty      = new SimpleObjectProperty<>(this, "resource");
        coverageProperty      = new SimpleObjectProperty<>(this, "coverage");
        sliceExtentProperty   = new SimpleObjectProperty<>(this, "sliceExtent");
        interpolationProperty = new SimpleObjectProperty<>(this, "interpolation", data.processor.getInterpolation());
        resourceProperty     .addListener((p,o,n) -> onPropertySpecified(o, n, null, coverageProperty, null));
        coverageProperty     .addListener((p,o,n) -> onPropertySpecified(getResource(), null, n, resourceProperty, null));
        sliceExtentProperty  .addListener((p,o,n) -> onPropertySpecified(getResource(), getResource(), getCoverage(), null, null));
        interpolationProperty.addListener((p,o,n) -> onInterpolationSpecified(n));
        fixedPane.setBackground(BACKGROUND);
    }

    /**
     * Completes initialization of this canvas for use with the returned property explorer.
     * The intent is to be notified when the image used for showing the coverage changed.
     * This method is invoked the first time that the "Properties" section in `CoverageControls`
     * is being shown.
     */
    final ImagePropertyExplorer createPropertyExplorer() {
        propertyExplorer = new ImagePropertyExplorer(getLocale(), fixedPane.backgroundProperty());
        propertyExplorer.setImage(resampledImage, getVisibleImageBounds());
        return propertyExplorer;
    }

    /**
     * Returns the region containing the image view.
     * The subclass is implementation dependent and may change in any future version.
     *
     * @return the region to show.
     */
    final Region getView() {
        return fixedPane;
    }

    /**
     * Returns the resource to preserve when the coverage is adjusting. If the coverage is set for another reason
     * than adjustment, then the resource should be set to null as specified in {@link #resourceProperty} contract.
     */
    final GridCoverageResource getResourceIfAdjusting() {
        return isCoverageAdjusting ? getResource() : null;
    }

    /**
     * Returns the source of coverages for this viewer, or {@code null} if none.
     * This method, like all other methods in this class, shall be invoked from the JavaFX thread.
     *
     * @return the source of coverages shown in this viewer, or {@code null} if none.
     *
     * @see #resourceProperty
     *
     * @since 1.2
     */
    public final GridCoverageResource getResource() {
        return resourceProperty.get();
    }

    /**
     * Sets the source of coverages shown in this viewer.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The new data are loaded in a background thread and the {@link #coverageProperty}
     * value will be updated after an undetermined amount of time.
     *
     * @param  resource  the source of data to show in this viewer, or {@code null} if none.
     *
     * @see #resourceProperty
     *
     * @since 1.2
     */
    public final void setResource(final GridCoverageResource resource) {
        resourceProperty.set(resource);
        // Will indirectly invoke `onPropertySpecified(…)`.
    }

    /**
     * Returns the source of image for this viewer.
     * This method, like all other methods in this class, shall be invoked from the JavaFX thread.
     * Note that this value may change at any time (depending on user interaction)
     * if the {@link #resourceProperty} has a non-null value.
     *
     * @return the coverage shown in this explorer, or {@code null} if none.
     *
     * @see #coverageProperty
     */
    public final GridCoverage getCoverage() {
        return coverageProperty.get();
    }

    /**
     * Sets the coverage to show in this viewer.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The new data are loaded in a background thread and will appear after an
     * undetermined amount of time.
     *
     * <p>Invoking this method sets the {@link #resourceProperty} value to {@code null}.</p>
     *
     * @param  coverage  the data to show in this viewer, or {@code null} if none.
     *
     * @see #coverageProperty
     */
    public final void setCoverage(final GridCoverage coverage) {
        coverageProperty.set(coverage);
        // Will indirectly invoke `onPropertySpecified(…)`.
    }

    /**
     * Returns a subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @return subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @see #sliceExtentProperty
     * @see GridCoverage#render(GridExtent)
     */
    public final GridExtent getSliceExtent() {
        return sliceExtentProperty.get();
    }

    /**
     * Sets a subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     * Note that values set on this property may be overwritten at any time by user interactions if this
     * {@code CoverageCanvas} is associated with a {@link GridSliceSelector}.
     *
     * @param  sliceExtent  subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @see #sliceExtentProperty
     * @see GridCoverage#render(GridExtent)
     */
    public final void setSliceExtent(final GridExtent sliceExtent) {
        sliceExtentProperty.set(sliceExtent);
        // Will indirectly invoke `onPropertySpecified(…)`.
    }

    /**
     * Gets the interpolation method used during resample operations.
     *
     * @return the current interpolation method.
     *
     * @see #interpolationProperty
     */
    public final Interpolation getInterpolation() {
        return interpolationProperty.get();
    }

    /**
     * Sets the interpolation method to use during resample operations.
     *
     * @param  interpolation  the new interpolation method.
     *
     * @see #interpolationProperty
     */
    public final void setInterpolation(final Interpolation interpolation) {
        interpolationProperty.set(interpolation);
    }

    /**
     * Sets the colorization algorithm to apply on rendered images.
     * Should be an algorithm based on coverage categories.
     *
     * <p>{@code CoverageCanvas} cannot detect when the given colorizer changes its internal state.
     * The {@link #stylingChanged()} method should be invoked explicitly when such change occurs.</p>
     *
     * @param colorizer colorization algorithm to apply on computed image, or {@code null} for default.
     */
    final void setColorizer(final Colorizer colors) {
        data.processor.setColorizer(colors);
        stylingChanged();
    }

    /**
     * Invoked by {@link CoverageControls} when the user selected a new color stretching mode.
     * The sample values are assumed the same, only the image appearance is modified.
     */
    final void setStretching(final Stretching selection) {
        if (TRACE) {
            trace("setStretching(%s)", selection);
        }
        if (data.selectedDerivative != selection) {
            data.selectedDerivative = selection;
            stylingChanged();
        }
    }

    /**
     * Set whether to skip the rendering of the coverage.
     * OTher features such as the isolines may still be rendered.
     *
     * @param  hidden  whether to skip the rendering of the coverage.
     */
    final void setCoverageHidden(final boolean hidden) {
        if (isCoverageHidden != hidden) {
            isCoverageHidden = hidden;
            requestRepaint();
        }
    }

    /**
     * Invoked when image colors changed. Derived features such are isolines are assumed unchanged.
     * This method should be invoked explicitly when the {@link Colorizer} changes its internal state.
     *
     * @see #clearRenderedImage()
     */
    final void stylingChanged() {
        resampledImage = null;
        requestRepaint();
    }

    /**
     * Sets whether to show for a few seconds a visual indication of which tiles were read.
     * If {@code true}, tiles will be shown by a translucent shape and fade away.
     *
     * @param  enabled  whether to show for a few seconds a visual indication of which tiles were read.
     */
    final void showTileReads(final boolean enabled) {
        final GridCoverageResource resource = getResource();
        if (enabled) {
            if (tileReadListener == null) {
                tileReadListener = new TileReadListener();
                if (resource != null) {
                    resource.addListener(TileReadEvent.class, tileReadListener);
                }
            }
        } else if (tileReadListener != null) {
            if (resource != null) {
                resource.removeListener(TileReadEvent.class, tileReadListener);
            }
            tileReadListener = null;
        }
    }

    /**
     * Sets the Coordinate Reference System in which the coverage is resampled before displaying.
     * The new CRS must be compatible with the previous CRS, i.e. a coordinate operation between
     * the two CRSs shall exist.
     *
     * @param  newValue  the new Coordinate Reference System in which to resample the coverage before displaying.
     * @param  anchor    the point to keep at fixed display coordinates, expressed in any compatible CRS.
     *                   If {@code null}, defaults to {@linkplain #getPointOfInterest(boolean) point of interest}.
     *                   If non-null, the anchor must be associated to a CRS.
     * @throws RenderException if the objective CRS cannot be set to the given value.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public void setObjectiveCRS(final CoordinateReferenceSystem newValue, DirectPosition anchor) throws RenderException {
        final Long id = LogHandler.loadingStart(getResource());
        try {
            // With `LogHandler` because this call may cause searches in EPSG database.
            super.setObjectiveCRS(newValue, anchor);
        } finally {
            LogHandler.loadingStop(id);
        }
        clearIsolines();
    }

    /**
     * Sets canvas properties from the given grid geometry.
     *
     * @param  newValue  the grid geometry from which to get new canvas properties.
     * @throws RenderException if the given grid geometry cannot be converted to canvas properties.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public void setGridGeometry(final GridGeometry newValue) throws RenderException {
        final Long id = LogHandler.loadingStart(getResource());
        try {
            // With `LogHandler` because this call may cause searches in EPSG database.
            super.setGridGeometry(newValue);
        } finally {
            LogHandler.loadingStop(id);
        }
        clearIsolines();
    }

    /**
     * Sets both resource and coverage properties together. Typically only one of those properties is non-null.
     * If both are non-null, then it is caller's responsibility to ensure that they are consistent.
     *
     * @param  request  the resource or coverage to set, or {@code null} for clearing the view.
     */
    final void setImage(final ImageRequest request) {
        final GridCoverageResource resource;
        final GridCoverage coverage;
        final GridExtent sliceExtent;
        final GridGeometry visibleArea;
        if (request != null) {
            resource    = request.resource;
            coverage    = request.coverage;
            sliceExtent = request.slice;
            visibleArea = request.visibleArea;
        } else {
            resource    = null;
            coverage    = null;
            sliceExtent = null;
            visibleArea = null;
        }
        final GridCoverageResource discard = getResource();
        if (discard != resource || getCoverage() != coverage || getSliceExtent() != sliceExtent) {
            final boolean p = isCoverageAdjusting;
            try {
                isCoverageAdjusting = true;
                setResource(resource);
                setCoverage(coverage);
                setSliceExtent(sliceExtent);
            } finally {
                isCoverageAdjusting = p;
            }
            onPropertySpecified(discard, resource, coverage, null, visibleArea);
        }
    }

    /**
     * Invoked when a new value has been set on {@link #resourceProperty} or {@link #coverageProperty}.
     * This method fetches information such as the grid geometry and sample dimensions in a background thread.
     * Those information will be used for initializing "objective CRS" and "objective to display" to new values.
     * Rendering will happen in another background computation.
     *
     * <p>The {@code visibleArea} argument is used when we want to create a new canvas
     * initialized to the same viewing region and zoom level than an existing canvas.</p>
     *
     * @param  discard      the old resource, or {@code null} if none.
     * @param  resource     the new resource, or {@code null} if none.
     * @param  coverage     the new coverage, or {@code null} if none.
     * @param  toClear      the property which is an alternative to the property that has been set.
     * @param  visibleArea  initial "objective to display" transform to use, or {@code null} for automatic.
     */
    private void onPropertySpecified(
            final GridCoverageResource discard,
            final GridCoverageResource resource,
            final GridCoverage         coverage,
            final ObjectProperty<?>    toClear,
            final GridGeometry         visibleArea)
    {
        hasCoverageOrResource = (resource != null || coverage != null);
        if (isCoverageAdjusting) {
            return;
        }
        if (toClear != null) try {
            isCoverageAdjusting = true;
            toClear.set(null);
        } finally {
            isCoverageAdjusting = false;
        }
        if (discard != resource && tileReadListener != null) {
            if (discard  != null) discard.removeListener(TileReadEvent.class, tileReadListener);
            if (resource != null) resource.addListener  (TileReadEvent.class, tileReadListener);
        }
        if (resource == null && coverage == null) {
            runAfterRendering(() -> {
                clear();
                requestRepaint();
            });
        } else if (controls != null && controls.isAdjustingSlice) {
            runAfterRendering(() -> {
                clearRenderedImage();
                requestRepaint();
            });
        } else {
            BackgroundThreads.execute(new Task<GridGeometry>() {
                /** Name of the grid <abbr>CRS</abbr>, derived from the resource identifier. */
                private Identifier gridCrsName;

                /** Information about all bands. */
                private List<SampleDimension> ranges;

                /**
                 * Fetches coverage domain and range. In some {@link GridCoverageResource} implementations,
                 * fetching the grid geometry is a costly operation. So we do it in a background thread and
                 * invoke {@code setNewSource(…)} later with the result. No rendering happen here.
                 */
                @Override protected GridGeometry call() throws Exception {
                    GridGeometry domain;
                    final Long id = LogHandler.loadingStart(resource);
                    try {
                        double[] scales;
                        if (coverage != null) {
                            domain = coverage.getGridGeometry();
                            ranges = coverage.getSampleDimensions();
                            scales = null;
                        } else {
                            domain = resource.getGridGeometry();
                            ranges = resource.getSampleDimensions();
                            scales = Containers.peekFirst(resource.getAvailableResolutions());
                        }
                        gridCrsName = ImageRequest.gridCrsName(resource, domain);
                        if (domain != null) {
                            /*
                             * The domain should never be null and should always be complete (including envelope).
                             * Nevertheless we try to be safe, since `setNewSource(…)` wants a complete geometry.
                             * So if the envelope is missing but the extent is present, then the missing part was
                             * the "grid to CRS" transform. We use an identity transform with a "display CRS".
                             */
                            if (!domain.isDefined(GridGeometry.ENVELOPE) && domain.isDefined(GridGeometry.EXTENT)) {
                                final GridExtent extent = domain.getExtent();
                                final int dimension = extent.getDimension();
                                domain = new GridGeometry(extent, PixelInCell.CELL_CORNER, MathTransforms.identity(dimension),
                                                (dimension == BIDIMENSIONAL) ? CommonCRS.Engineering.DISPLAY.crs() : null);
                            }
                            /*
                             * Compute the maximum zoom out. Usually, we want to show the full image.
                             * But if the image has no pyramid, showing the full image may cause the
                             * loading of a large amount of data. We are better to limit the zoom to
                             * a small area.
                             */
                            if (domain.isDefined(GridGeometry.ENVELOPE | GridGeometry.RESOLUTION)) {
                                if (scales == null) {
                                    scales = domain.getResolution(true);
                                }
                                double ratio = MAXIMAL_CELL_COUNT;
                                final Envelope bounds = domain.getEnvelope();
                                final int dimension = Math.min(BIDIMENSIONAL, Math.min(bounds.getDimension(), scales.length));
                                for (int i=0; i<dimension; i++) {
                                    ratio *= scales[i] / bounds.getSpan(i);  // Equivalent to `ratio /= span_in_pixels`.
                                }
                                if (ratio < 1) {
                                    ratio = Math.pow(ratio, 1d / dimension);
                                    final double out = (1 - ratio) / 2;      // Fraction of bounds to take out on each side.
                                    final var zoomArea = new GeneralEnvelope(bounds);
                                    for (int i=0; i<dimension; i++) {
                                        final double margin = zoomArea.getSpan(i) * out;
                                        zoomArea.setRange(i, zoomArea.getLower(i) + margin, zoomArea.getUpper(i) - margin);
                                    }
                                    // Pretend that the data domain is smaller than reality.
                                    domain = domain.derive().subgrid(zoomArea, null).build();
                                }
                            }
                        }
                    } catch (BackingStoreException e) {
                        throw e.unwrapOrRethrow(DataStoreException.class);
                    } finally {
                        LogHandler.loadingStop(id);
                    }
                    return domain;
                }

                /**
                 * Invoked in JavaFX thread for setting the grid geometry we just fetched.
                 * This method requests a repaint, which will occur in another thread.
                 */
                @Override protected void succeeded() {
                    runAfterRendering(() -> {
                        try {
                            setNewSource(gridCrsName, getValue(), ranges, visibleArea);
                            requestRepaint();                   // Cause `Worker` class to be executed.
                        } catch (RuntimeException ex) {         // Mostly for `BackingStoreException`.
                            clear();
                            requestRepaint();
                            ExceptionReporter.canNotUseResource(fixedPane, Exceptions.unwrap(ex));
                        }
                    });
                }

                /**
                 * Invoked when an error occurred while loading an image or processing it.
                 * This method popups the dialog box immediately because it is considered
                 * an important error.
                 */
                @Override protected void failed() {
                    final Throwable ex = getException();
                    errorOccurred(ex);
                    ExceptionReporter.canNotUseResource(fixedPane, ex);
                }
            });
        }
    }

    /**
     * Clears the rendered image but keep the resource, coverage, grid geometry and sample dimensions unchanged.
     * Invoking this method alone is useful when only the selected two-dimensional slice changed.
     * If the {@link StyledRenderingData#clear()} method is not invoked, then the map projection,
     * zoom, <i>etc.</i> are preserved.
     *
     * <p>The caller is responsible for invoking {@link #requestRepaint()} or something equivalent,
     * possibly indirectly through a listener on a modified property. The {@link #requestRepaint()}
     * method is not invoked by this method because the caller will typically do more cleaning.</p>
     *
     * @see #clear()
     */
    private void clearRenderedImage() {
        clearError();
        clearIsolines();
        resampledImage = null;
        derivedImages.clear();
    }

    /**
     * Invoked when a new resource or coverage has been specified.
     * Caller should invoke {@link #requestRepaint()} after this method
     * for loading and resampling the image in a background thread.
     *
     * <p>The {@code visibleArea} argument is used when we want to create a new canvas
     * initialized to the same viewing region and zoom level than an existing canvas.
     * It should have a <abbr>CRS</abbr> compatible with the one of the data to show.</p>
     *
     * <p>All arguments can be {@code null} for clearing the canvas.
     * This method is invoked in JavaFX thread.</p>
     *
     * @param  gridCrsName  name of the grid <abbr>CRS</abbr>, derived from the resource identifier.
     * @param  domain       the multi-dimensional grid geometry, or {@code null} if there is no data.
     * @param  ranges       descriptions of bands, or {@code null} if there is no data.
     * @param  visibleArea  initial "objective to display" transform to use, or {@code null} for automatic.
     */
    private void setNewSource(final Identifier gridCrsName,
                                    GridGeometry domain,
                              final List<SampleDimension> ranges,
                              final GridGeometry visibleArea)
    {
        if (TRACE) {
            trace("setNewSource(…): the new domain of data is:%n\t%s", domain);
        }
        data.gridCrsName = null;
        clearRenderedImage();
        data.clear();
        /*
         * Configure the `GridSliceSelector`, which will compute a new slice extent as a side effect.
         * It will overwrite the previous value of `sliceExtent` property in this class, which needs
         * to be done before to start the `Worker` process in a background thread.
         *
         * Note: we do not configure that status bar here, because `StatucBar` configures itself by
         * listening to `MapCanvas` rendering events.
         */
        int[] xyDimensions;
        if (controls != null) try {
            isCoverageAdjusting = true;
            setSliceExtent(controls.configureSliceSelector(domain));
            xyDimensions = controls.sliceSelector.getXYDimensions();
        } finally {
            isCoverageAdjusting = false;
        } else {
            xyDimensions = ArraysExt.range(0, BIDIMENSIONAL);
            final GridExtent extent = getSliceExtent();
            if (extent != null) try {
                xyDimensions = extent.getSubspaceDimensions(BIDIMENSIONAL);
            } catch (SubspaceNotSpecifiedException e) {
                unexpectedException(e);                     // We can continue with dimensions {0,1}.
            }
        }
        /*
         * Notify the `RenderingData` and `MapCanvas`. All information below must be two-dimensional.
         * The objective CRS is set indirectly through the envelope. Therefore, we should try hard to
         * provide a CRS compatible with the coverage.
         */
        Envelope bounds = null;
        if (domain != null) {
            domain = domain.selectDimensions(xyDimensions);
            if (domain.isDefined(GridGeometry.ENVELOPE)) {
                bounds = domain.getEnvelope();
                if (bounds.getCoordinateReferenceSystem() == null) try {
                    final var copy = new GeneralEnvelope(bounds);
                    copy.setCoordinateReferenceSystem(domain.createGridCRS(gridCrsName, PixelInCell.CELL_CORNER));
                    bounds = copy;
                } catch (FactoryException e) {
                    unexpectedException(e);
                }
            }
        }
        data.gridCrsName = gridCrsName;
        data.setImageSpace(domain, ranges, xyDimensions);
        initialize(visibleArea);
        setObjectiveBounds(bounds);
    }

    /**
     * Return a name of the grid <abbr>CRS</abbr>, derived from the resource identifier.
     * This method returns {@code null} if no worker has read the resource identifier yet.
     * It may happen randomly depending on thread execution order.
     *
     * <p>Note: do not fallback on an artificial name if the name is {@code null}.
     * This method is used for building a grid <abbr>CRS</abbr> for cell indices.
     * If this method returns an artificial name, it would cause an unusable menu
     * item to appear in the menu that offers different <abbr>CRS</abbr>.</p>
     *
     * @see ImageRequest#gridCrsName(GridCoverageResource, GridGeometry)
     */
    final Identifier gridCrsName() {
        return data.gridCrsName;
    }

    /**
     * Clears all information that are derived from the raw image projected to objective CRS.
     * In current version this is only isolines.
     *
     * <p>The caller is responsible for invoking {@link #requestRepaint()} or something equivalent,
     * possibly indirectly through a listener on a modified property. The {@link #requestRepaint()}
     * method is not invoked by this method because the caller will typically do more cleaning.</p>
     */
    private void clearIsolines() {
        if (isolines != null) {
            isolines.clear();
        }
    }

    /**
     * Invoked when a new interpolation has been specified.
     *
     * @see #setInterpolation(Interpolation)
     */
    private void onInterpolationSpecified(final Interpolation newValue) {
        if (TRACE) {
            trace("onInterpolationSpecified(%s)", newValue);
        }
        data.processor.setInterpolation(newValue);
        stylingChanged();
    }

    /**
     * Invoked in JavaFX thread for creating a renderer to be executed in a background thread.
     * This method prepares the information needed but does not start the rendering itself.
     * The rendering will be done later by a call to {@link Renderer#paint(Graphics2D)}.
     */
    @Override
    protected Renderer createRenderer() {
        return hasCoverageOrResource ? new Worker(this) : null;
    }

    /**
     * Resample and paint image in the canvas. This class performs some or all of the following tasks, in order.
     * It is possible to skip the two first tasks if they were already done, but after the work started at some
     * point all remaining points are executed:
     *
     * <ol>
     *   <li>Read a new coverage if zoom has changed more than some threshold value.</li>
     *   <li>Compute statistics on sample values (if needed).</li>
     *   <li>Stretch the color ramp (if requested).</li>
     *   <li>Resample the image and convert to integer values.</li>
     *   <li>Paint the image.</li>
     * </ol>
     */
    private static final class Worker extends Renderer {
        /**
         * The resource from which the data has been read, or {@code null} if unknown.
         * This is used for loading a coverage if none were explicitly specified,
         * and for determining a target window for logging records.
         */
        private final GridCoverageResource resource;

        /**
         * The coverage specified by user or the coverage loaded from the {@linkplain #resource}.
         * Should never be {@code null} after successful execution of {@link #render()}.
         */
        private GridCoverage coverage;

        /**
         * Whether the value of {@link #coverage} changed since the last rendering.
         * It may happen if {@link #resource} is non-null, contains pyramided data and the pyramid level
         * used by this rendering is different than the pyramid level used during the previous rendering.
         * Note that a {@code false} value does not mean that {@link #sliceExtent} did not changed.
         */
        private boolean coverageChanged;

        /**
         * The two-dimensional slice to display. May change for the same coverage when using
         * {@link ViewAndControls#sliceSelector} for navigation in dimensions other than the
         * {@value #BIDIMENSIONAL} first dimensions.
         */
        private final GridExtent sliceExtent;

        /**
         * Value of {@link CoverageCanvas#data} at the time this worker has been initialized.
         */
        private final StyledRenderingData data;

        /**
         * Value of {@link CoverageCanvas#getObjectiveCRS()} at the time this worker has been initialized.
         * This the coordinate reference system in which to reproject the data, in "real world" units.
         */
        private final CoordinateReferenceSystem objectiveCRS;

        /**
         * Value of {@link CoverageCanvas#getObjectiveToDisplay()} at the time this worker has been initialized.
         * This is the conversion from {@link #objectiveCRS} to the canvas display <abbr>CRS</abbr>.
         * Can be thought as a conversion from "real world" units to pixel units
         * and depends on the zoom and translation events that happened before rendering.
         */
        private final LinearTransform objectiveToDisplay;

        /**
         * Value of {@link CoverageCanvas#getDisplayBounds()} at the time this worker has been initialized,
         * expanded by {@link CoverageCanvas#imageMargin}. This is the size and location of the display device
         * in pixel units, plus the margin. This value is usually constant when the widget is not resized.
         */
        private final Envelope2D displayBounds;

        /**
         * Bounds of the currently visible area (plus margin) in units of objective CRS.
         * The AOI can be used as a hint, for example in order to clip data for faster rendering.
         * This is needed only if {@link #isolines} is non-null.
         *
         * @see CoverageCanvas#getAreaOfInterest()
         */
        private Rectangle2D objectiveAOI;

        /**
         * The coordinates of the point to show typically (but not necessarily) in the center of display area.
         * The coordinate is expressed in objective CRS.
         */
        private final DirectPosition objectivePOI;

        /**
         * The {@link #data} image after color ramp stretching, before resampling is applied.
         * May be {@code null} if not yet computed, in which case it will be computed by {@link #render()}.
         */
        private RenderedImage recoloredImage;

        /**
         * The {@link #recoloredImage} after resampling is applied.
         * May be {@code null} if not yet computed, in which case it will be computed by {@link #render()}.
         * This image should not be cached after rendering operation is completed.
         */
        private RenderedImage resampledImage;

        /**
         * The resampled image with tiles computed in advance.
         * The set of prefetched tiles may differ at each rendering event.
         */
        private RenderedImage prefetchedImage;

        /**
         * Conversion from {@link #prefetchedImage} pixel coordinates to display coordinates.
         * This transform usually contains only a translation, because we do not recompute a new {@link #prefetchedImage}
         * when the only change is a translation. But this transform may also contain a rotation or scale factor during
         * a short time if the rendering happens while {@link #prefetchedImage} is in need to be recomputed.
         *
         * @see StyledRenderingData#displayToObjective
         */
        private AffineTransform resampledToDisplay;

        /**
         * Whether to skip the rendering of the coverage.
         * OTher features such as the isolines may still be rendered.
         */
        private final boolean isCoverageHidden;

        /**
         * Snapshot of information required for rendering isolines, or {@code null} if none.
         */
        private IsolineController.Snapshot[] isolines;

        /**
         * Creates a new renderer. Shall be invoked in JavaFX thread.
         */
        Worker(final CoverageCanvas canvas) {
            resource           = canvas.getResource();
            coverage           = canvas.getCoverage();
            data               = canvas.data.clone();
            sliceExtent        = canvas.getSliceExtent();
            objectiveCRS       = canvas.getObjectiveCRS();
            objectiveToDisplay = canvas.getObjectiveToDisplay();
            displayBounds      = canvas.getDisplayBounds();
            objectivePOI       = canvas.getPointOfInterest(true);
            recoloredImage     = canvas.derivedImages.get(data.selectedDerivative);
            isCoverageHidden   = canvas.isCoverageHidden;
            if (data.validateCRS(objectiveCRS)) {
                resampledImage = canvas.resampledImage;
            }
            final Insets margin = canvas.imageMargin.get();
            if (margin != null && displayBounds != null) {
                final double top  = margin.getTop();
                final double left = margin.getLeft();
                displayBounds.x      -= left;
                displayBounds.width  += left + margin.getRight();
                displayBounds.y      -= top;
                displayBounds.height += top  + margin.getBottom();
            }
            /*
             * Help for auxiliary services. They are special cases for now,
             * but should be refactored as styling services in a future version.
             */
            if (canvas.isolines != null) try {
                isolines = canvas.isolines.prepare();
                objectiveAOI = Shapes2D.transform(MathTransforms.bidimensional(objectiveToDisplay.inverse()), displayBounds, null);
            } catch (TransformException e) {
                unexpectedException(e);                     // Should never happen.
            }
        }

        /**
         * Returns the region of source image which is currently shown, in units of source coverage pixels.
         * This method can be invoked only after {@link #render()}. It returns {@code null} if the visible
         * bounds are unknown.
         *
         * @see CoverageCanvas#getVisibleImageBounds()
         */
        final Rectangle getVisibleImageBounds() {
            try {
                return (Rectangle) AffineTransforms2D.inverseTransform(resampledToDisplay, displayBounds, new Rectangle());
            } catch (NoninvertibleTransformException e) {
                unexpectedException(e);                     // Should never happen.
            }
            return null;
        }

        /**
         * Invoked in background thread for resampling the image and stretching the color ramp.
         * This method performs some of the steps documented in class Javadoc, with possibility
         * to skip some steps for example if the required source image is already resampled.
         */
        @Override
        @SuppressWarnings("PointlessBitwiseExpression")
        protected void render() throws Exception {
            final Long id = LogHandler.loadingStart(resource);
            try {
                /*
                 * Update the transform from data CRS to objective CRS if that transform needs to be computed
                 * (otherwise do nothing). After the objective CRS is set, we can compute the pyramid level.
                 * It will determine if data needs to be loaded again.
                 */
                data.setObjectiveCRS(objectiveCRS);
                if (resource != null) {
                    data.coverageLoader = MultiResolutionImageLoader.getInstance(resource, data.coverageLoader);
                    final GridCoverage loaded = data.ensureCoverageLoaded(objectiveToDisplay, objectivePOI);
                    if (coverageChanged = (loaded != null)) {
                        coverage = loaded;
                    }
                }
                if (data.ensureImageLoaded(coverage, sliceExtent, coverageChanged)) {
                    recoloredImage = null;
                }
                /*
                 * Find whether resampling to apply is different than the resampling used last time that the image
                 * has been rendered, ignoring translations. Translations do not require new resampling operations
                 * because we can manage translations by changing `RenderedImage` coordinates.
                 */
                boolean isValid = (resampledImage != null);
                if (isValid) {
                    resampledToDisplay = data.getTransform(objectiveToDisplay);
                    isValid = (resampledToDisplay.getType() &
                            ~(AffineTransform.TYPE_IDENTITY | AffineTransform.TYPE_TRANSLATION)) == 0;
                    /*
                     * If user pans the image close to integer range limit, create a new resampled image shifted to
                     * new location (i.e. force `resampleAndConvert(…)` to be invoked again). The intent is to move
                     * away from integer overflow situation.
                     */
                    if (isValid) {
                        isValid = Math.max(Math.abs(resampledToDisplay.getTranslateX()),
                                           Math.abs(resampledToDisplay.getTranslateY()))
                                  < Integer.MAX_VALUE - OVERFLOW_SAFETY_MARGIN;
                        if (TRACE && !isValid) {
                            trace("render(): new resample for avoiding overflow caused by translation.");
                        }
                    }
                }
                if (!isValid) {
                    if (recoloredImage == null) {
                        recoloredImage = data.recolor();
                        if (TRACE) {
                            trace("render(): recolor by application of %s.", data.selectedDerivative);
                        }
                    }
                    resampledImage = data.resampleAndConvert(recoloredImage, objectiveToDisplay, objectivePOI);
                    resampledToDisplay = data.getTransform(objectiveToDisplay);
                    if (TRACE) {
                        trace("render(): resampling result:%n\t%s", resampledImage);
                    }
                }
                /*
                 * Launch isolines creation if requested. We do this operation before `prefetch(…)`
                 * because it will be executed in background threads while we process the coverage.
                 * We cannot invoke it sooner because it needs some `resampleAndConvert(…)` results.
                 */
                final Future<Isolines[]> newIsolines = data.generate(isolines);
                if (!isCoverageHidden) {
                    prefetchedImage = data.prefetch(resampledImage, resampledToDisplay, displayBounds);
                }
                if (newIsolines != null) {
                    IsolineController.complete(isolines, newIsolines);
                }
            } finally {
                LogHandler.loadingStop(id);
            }
        }

        /**
         * Draws the image after {@link #render()} finished to prepare data.
         * This method is invoked in a background thread.
         */
        @Override
        protected void paint(final Graphics2D gr) {
            gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            if (prefetchedImage instanceof TileErrorHandler.Executor ex) {
                ex.execute(
                        () -> gr.drawRenderedImage(RenderingWorkaround.wrap(prefetchedImage), resampledToDisplay),
                        new TileErrorHandler(data.processor.getErrorHandler(), CoverageCanvas.class, "paint"));
            } else if (!isCoverageHidden) {
                gr.drawRenderedImage(RenderingWorkaround.wrap(prefetchedImage), resampledToDisplay);
            }
            if (isolines != null) {
                final AffineTransform at = gr.getTransform();
                final Stroke st = gr.getStroke();
                // Arbitrarily use a line tickness of 1/8 of source pixel size for making apparent when zoom is strong.
                gr.setStroke(new BasicStroke(data.getDataPixelSize(objectivePOI) / 8));
                gr.transform((AffineTransform) objectiveToDisplay);     // This cast is safe in PlanarCanvas subclass.
                for (final IsolineController.Snapshot s : isolines) {
                    s.paint(gr, objectiveAOI);
                }
                gr.setTransform(at);
                gr.setStroke(st);
            }
        }

        /**
         * Invoked in JavaFX thread after successful {@link #paint(Graphics2D)} completion.
         * This method stores the computation results.
         */
        @Override
        protected boolean commit(final MapCanvas canvas) {
            final var cc = (CoverageCanvas) canvas;
            cc.cacheRenderingData(this);
            /*
             * Help for auxiliary services. They are special cases for now,
             * but should be refactored as styling services in a future version.
             */
            final TileReadListener tileReadListener = cc.tileReadListener;
            if (tileReadListener != null) {
                tileReadListener.newStaticGraphics();
            }
            if (isolines != null) {
                for (final IsolineController.Snapshot s : isolines) {
                    s.commit();
                }
            }
            return super.commit(canvas);
        }
    }

    /**
     * Invoked in JavaFX thread after a paint event for caching rendering data.
     * If the resampled image changed, all previously cached images are discarded.
     */
    private void cacheRenderingData(final Worker worker) {
        if (TRACE && data.hasChanged(worker.data)) {
            trace("cacheRenderingData(…): new visual coverage:%n%s", worker.data);
        }
        data = worker.data;
        /*
         * Cache the recolored image. It does not consume lot of memory (mostly the memory used by the color model).
         * The recolored image are based on the original data, so the cache does not vary with zoom level except if
         * a change of zoom level caused a change of the image read from the data store.
         */
        if (worker.coverageChanged) {
            derivedImages.clear();
        }
        derivedImages.put(data.selectedDerivative, worker.recoloredImage);
        resampledImage = worker.resampledImage;
        if (TRACE) {
            trace("cacheRenderingData(…): objective bounds after rendering at %tT:%n%s", System.currentTimeMillis(), this);
        }
        /*
         * Notify the "Image properties" tab that the image changed. The `propertyExplorer` field is non-null
         * only if the "Properties" section in `CoverageControls` has been shown at least once.
         */
        if (propertyExplorer != null) {
            propertyExplorer.setImage(resampledImage, worker.getVisibleImageBounds());
            if (TRACE) {
                trace("cacheRenderingData(…): Update image property view with visible area %s.",
                      propertyExplorer.getVisibleImageBounds(resampledImage));
            }
        }
        /*
         * Adjust the accuracy of coordinates shown in the status bar.
         * The number of fraction digits depend on the zoom factor.
         */
        Quantity<Length> accuracy = null;
        if (resampledImage.getProperty(PlanarImage.POSITIONAL_ACCURACY_KEY) instanceof Quantity<?>[] values) {
            for (final Quantity<?> q : values) {
                if (Units.isLinear(q.getUnit())) {
                    accuracy = q.asType(Length.class);
                    double m = accuracy.getUnit().getConverterTo(Units.METRE).convert(accuracy.getValue().doubleValue());
                    accuracy = GUIUtilities.shorter(accuracy, m);
                    break;
                }
            }
        }
        setPositionalAccuracy(accuracy);
        /*
         * If error(s) occurred during calls to `RenderedImage.getTile(tx, ty)`, reports those errors.
         * The `errorReport` field is reset to `null` in preparation for the next rendering operation.
         */
        final LogRecord report = errorReport;
        if (report != null) {
            errorReport = null;
            errorOccurred(report.getThrown());
        }
        /*
         * If the coverage changed, notify user. The coverage may have changed because of a change
         * in the pyramid level when the underlying data store has pyramided data.
         */
        if (worker.coverageChanged) {
            if (!isCoverageAdjusting) try {
                isCoverageAdjusting = true;
                setCoverage(worker.coverage);
            } finally {
                isCoverageAdjusting = false;
            }
        }
    }

    /**
     * Returns the region of source image which is currently shown, in units of source coverage pixels.
     * This method performs the same work as {@link Worker#getVisibleImageBounds()} in a less efficient way.
     * It is used when no worker is available.
     *
     * @see Worker#getVisibleImageBounds()
     */
    private Rectangle getVisibleImageBounds() {
        final Envelope2D displayBounds = getDisplayBounds();
        if (displayBounds != null) try {
            final AffineTransform resampledToDisplay = data.getTransform(getObjectiveToDisplay());
            return (Rectangle) AffineTransforms2D.inverseTransform(resampledToDisplay, displayBounds, new Rectangle());
        } catch (NoninvertibleTransformException e) {
            unexpectedException(e);                     // Should never happen.
        }
        return null;
    }

    /**
     * Returns the bounds of the currently visible area in units of objective CRS.
     * This AOI changes when the {@linkplain #getDisplayBounds() display bounds} or
     * the {@linkplain #getObjectiveToDisplay() objective to display transform} changed.
     * The AOI can be used as a hint, for example in order to clip data for faster rendering.
     *
     * @return bounds of currently visible area in objective CRS, or {@code null} if unavailable.
     *
     * @see Worker#objectiveAOI
     */
    private Rectangle2D getAreaOfInterest() throws TransformException {
        final Envelope2D displayBounds = getDisplayBounds();
        if (displayBounds == null) {
            return null;
        }
        return Shapes2D.transform(MathTransforms.bidimensional(getObjectiveToDisplay().inverse()), displayBounds, null);
    }




    /**
     * Object notified when a tile is about to be read. The notifications can be sent from any thread,
     * typically a background thread which is reading the data. The tiles are enqueued for processing
     * in another background thread for avoiding to slow down the thread that read the data.
     */
    private final class TileReadListener implements StoreListener<TileReadEvent>, EventHandler<ActionEvent> {
        /**
         * Colors of the tiles, using different colors for different resolutions (pyramid levels).
         */
        private static final Color[] TILE_COLORS = {
            Color.VIOLET, Color.RED, Color.YELLOW, Color.CYAN, Color.PALEGREEN
        };

        /**
         * Same colors, but with transparency.
         */
        private static final Color[] FILL_COLORS = new Color[TILE_COLORS.length];
        static {
            for (int i=0; i<FILL_COLORS.length; i++) {
                final Color c = TILE_COLORS[i];
                FILL_COLORS[i] = Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.5);
            }
        }

        /**
         * Time that tiles are visible before they fade away.
         */
        private static final Duration DURATION = new Duration(4000);

        /**
         * The JavaFX shapes (usually rectangles) for highlighting the tiles.
         * This queue shall be thread-safe as it is read and written from different threads.
         */
        private final Queue<FadeTransition> tileShapes;

        /**
         * The transform from objective <abbr>CRS</abbr> to the display coordinate system of the canvas.
         * This information is updated in the JavaFX thread after each rendering, so that creations of
         * JavaFX shapes will use the information that reflects the image shown in the canvas.
         */
        volatile StaticGraphics snapshot;

        /**
         * Creates a new listener of tile read events.
         * This constructor must be invoked from the JavaFX thread.
         */
        TileReadListener() {
            tileShapes = new ConcurrentLinkedQueue<>();
            newStaticGraphics();
        }

        /**
         * Takes a snapshot of the objective <abbr>CRS</abbr> and transform to display coordinate system.
         * This method should be invoked after each rendering, so that creations of JavaFX shapes will use
         * the information that reflects the image shown in the canvas.
         */
        final void newStaticGraphics() {
            snapshot = usingFixedTransform();
        }

        /**
         * Invoked when a tile has been read. This method computes the JavaFX shape in a background thread.
         * One thread is used for each shape (we do not collect the shapes in a queue) because that thread
         * is likely to finish before the next tile has been read anyway.
         */
        @Override
        @SuppressWarnings({"UseSpecificCatch", "LocalVariableHidesMemberVariable"})
        public void eventOccured(final TileReadEvent event) {
            BackgroundThreads.EXECUTOR.execute(() -> {
                final StaticGraphics snapshot = TileReadListener.this.snapshot;
                if (snapshot.objectiveToDisplay instanceof AffineTransform objectiveToDisplay) try {
                    final Shape tile = ShapeConverter.convert(event.outline(snapshot.objectiveCRS), objectiveToDisplay);
                    final int ic = event.getPyramidLevel() % TILE_COLORS.length;
                    tile.setStroke(TILE_COLORS[ic]);
                    tile.setFill(FILL_COLORS[ic]);
                    tile.setOpacity(0.5);
                    final var transition = new FadeTransition(DURATION, tile);
                    transition.setFromValue(0.5);
                    transition.setToValue(0);
                    transition.setOnFinished(this);
                    tileShapes.add(transition);
                } catch (Exception e) {
                    Logging.recoverableException(LOGGER, TileReadListener.class, "eventOccured", e);
                }
                Platform.runLater(() -> {
                    FadeTransition transition = tileShapes.poll();
                    if (transition != null) {
                        final ObservableList<Node> children = snapshot.getChildren();
                        do {
                            children.add(transition.getNode());
                            transition.play();
                            transition = tileShapes.poll();
                        } while (transition != null);
                    }
                });
            });
        }

        /**
         * Invoked when the animation on a tile is finished.
         * This method removes the JavaFX geometry object that represented the tile outline.
         */
        @Override
        public void handle(final ActionEvent event) {
            final var transition = (FadeTransition) event.getSource();
            final Node node = transition.getNode();
            final Pane parent = (Pane) node.getParent();
            if (parent != null && parent.getChildren().remove(node) && TRACE) {
                trace("TileReadListener.removeChild");
            }
        }
    }

    /**
     * Invoked when an exception occurred while computing a transform but the painting process can continue.
     */
    private static void unexpectedException(final Exception e) {
        unexpectedException("render", e);
    }

    /**
     * Invoked when an exception occurred. The declared source method should be a public or protected method.
     */
    static void unexpectedException(final String method, final Exception e) {
        Logging.unexpectedException(LOGGER, CoverageCanvas.class, method, e);
    }

    /**
     * Removes the image which was shown and releases memory.
     * Invoking this method may help to release memory when the map is no longer shown.
     *
     * <p>Subclasses should override this method for cleaning their fields.
     * Implementations in subclasses shall invoke {@code super.clear()}.</p>
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected void clear() {
        if (TRACE) {
            trace("clear()");
        }
        showTileReads(false);
        isCoverageHidden = false;
        isCoverageAdjusting = true;
        try {
            resourceProperty.set(null);
            coverageProperty.set(null);
            sliceExtentProperty.set(null);
        } finally {
            isCoverageAdjusting = false;
        }
        setNewSource(null, null, null, null);
        super.clear();
    }

    /**
     * Prints {@code "CoverageCanvas"} followed by the given message if {@link #TRACE} is {@code true}.
     * This is used for debugging purposes only.
     *
     * @param  format     the {@code printf} format string.
     * @param  arguments  values to format.
     */
    @Debug
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void trace(final String format, final Object... arguments) {
        if (TRACE) {
            System.out.print("CoverageCanvas.");
            System.out.printf(format, arguments);
            System.out.println();
        }
    }

    /**
     * Returns a string representation for debugging purposes.
     * The string content may change in any future version.
     */
    @Override
    public String toString() {
        if (!Platform.isFxApplicationThread()) {
            return super.toString();
        }
        final String lineSeparator = System.lineSeparator();
        final var buffer = new StringBuilder(1000);
        final var table  = new TableAppender(buffer);
        table.setMultiLinesCells(true);
        try {
            table.nextLine('═');
            getGridGeometry().getGeographicExtent().ifPresent((bbox) -> {
                table.append(String.format("Canvas geographic bounding box (λ,ɸ):%n"
                             + "Max: % 10.5f°  % 10.5f°%n"
                             + "Min: % 10.5f°  % 10.5f°",
                             bbox.getEastBoundLongitude(), bbox.getNorthBoundLatitude(),
                             bbox.getWestBoundLongitude(), bbox.getSouthBoundLatitude()))
                     .appendHorizontalSeparator();
            });
            final Rectangle2D aoi = getAreaOfInterest();
            final DirectPosition poi = getPointOfInterest(true);
            if (aoi != null && poi != null) {
                table.append(String.format("Area of interest in objective CRS (x,y):%n"
                             + "Max: %, 16.4f  %, 16.4f%n"
                             + "POI: %, 16.4f  %, 16.4f%n"
                             + "Min: %, 16.4f  %, 16.4f%n",
                             aoi.getMaxX(),        aoi.getMaxY(),
                             poi.getOrdinate(0),   poi.getOrdinate(1),
                             aoi.getMinX(),        aoi.getMinY()))
                     .appendHorizontalSeparator();
            }
            final Rectangle source = data.objectiveToData(aoi);
            if (source != null) {
                table.append("Extent in source coverage:").append(lineSeparator)
                     .append(String.valueOf(new GridExtent(source))).append(lineSeparator)
                     .appendHorizontalSeparator();
            }
            table.append(super.toString()).nextLine();
            table.nextLine('═');
            table.flush();
        } catch (RenderException | TransformException | IOException e) {
            buffer.append(e).append(lineSeparator);
        }
        return buffer.toString();
    }
}
