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
import java.util.Locale;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.lang.ref.Reference;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.ImageRenderer;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.Interpolation;
import org.apache.sis.gui.map.MapCanvas;
import org.apache.sis.gui.map.MapCanvasAWT;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.portrayal.RenderException;
import org.apache.sis.internal.gui.GUIUtilities;
import org.apache.sis.internal.gui.LogHandler;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.Resource;


/**
 * A canvas for {@link RenderedImage} provided by a {@link GridCoverage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see CoverageExplorer
 *
 * @since 1.1
 * @module
 */
@DefaultProperty("coverage")
public class CoverageCanvas extends MapCanvasAWT {
    /**
     * The data shown in this canvas. Note that setting this property to a non-null value may not
     * modify the canvas content immediately. Instead, a background process will request the tiles.
     *
     * <p>Current implementation is restricted to {@link GridCoverage} instances, but a future
     * implementation may generalize to {@link org.opengis.coverage.Coverage} instances.</p>
     *
     * @see #getCoverage()
     * @see #setCoverage(GridCoverage)
     */
    public final ObjectProperty<GridCoverage> coverageProperty;

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
     * Shall never be {@code null} but may be {@linkplain RenderingData#isEmpty() empty}. This instance shall be
     * read and modified in JavaFX thread only and cloned if those data are needed by a background thread.
     *
     * @see Worker
     */
    private RenderingData data;

    /**
     * The {@link #data} resampled to a CRS which can easily be mapped to {@linkplain #getDisplayCRS() display CRS}.
     * The different values are variants with color ramp changed.
     */
    private final Map<Stretching,RenderedImage> resampledImages;

    /**
     * The explorer to notify when the image shown in this canvas has changed.
     * This is non-null only if this {@link CoverageCanvas} is used together with {@link CoverageControls}.
     *
     * <p>Consider as final after {@link #createPropertyExplorer()} invocation.
     * This field may be removed in a future version if we revisit this API before making public.</p>
     *
     * @see #createPropertyExplorer()
     */
    private ImagePropertyExplorer imageProperty;

    /**
     * The status bar associated to this {@code MapCanvas}.
     * This is non-null only if this {@link CoverageCanvas} is used together with {@link CoverageControls}.
     */
    StatusBar statusBar;

    /**
     * The resource from which the data has been read, or {@code null} if unknown.
     * This is used only for determining a target window for logging records.
     *
     * @see #setOriginator(Reference)
     */
    private Reference<Resource> originator;

    /**
     * Creates a new two-dimensional canvas for {@link RenderedImage}.
     */
    public CoverageCanvas() {
        this(Locale.getDefault());
    }

    /**
     * Creates a new two-dimensional canvas using the given locale.
     *
     * @param  locale  the locale to use for labels and some messages, or {@code null} for default.
     */
    CoverageCanvas(final Locale locale) {
        super(locale);
        data                  = new RenderingData();
        resampledImages       = new EnumMap<>(Stretching.class);
        coverageProperty      = new SimpleObjectProperty<>(this, "coverage");
        sliceExtentProperty   = new SimpleObjectProperty<>(this, "sliceExtent");
        interpolationProperty = new SimpleObjectProperty<>(this, "interpolation", data.processor.getInterpolation());
        coverageProperty     .addListener((p,o,n) -> onImageSpecified());
        sliceExtentProperty  .addListener((p,o,n) -> onImageSpecified());
        interpolationProperty.addListener((p,o,n) -> onInterpolationSpecified(n));
    }

    /**
     * Completes initialization of this canvas for use with the returned property explorer.
     * The intent is to be notified when the image used for showing the coverage changed.
     * This method may be removed in a future SIS version if we revisit this API before
     * to make public.
     */
    final ImagePropertyExplorer createPropertyExplorer() {
        imageProperty = new ImagePropertyExplorer(getLocale(), fixedPane.backgroundProperty());
        imageProperty.setImage(resampledImages.get(data.selectedDerivative), getVisibleImageBounds());
        return imageProperty;
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
     * Sets the resource from which the data has been read.
     * This is used only for determining a target window for logging records.
     *
     * @param  originator  the resource from which the data has been read, or {@code null} if unknown.
     */
    final void setOriginator(final Reference<Resource> originator) {
        this.originator = originator;
    }

    /**
     * Returns the source of image for this viewer.
     * This method, like all other methods in this class, shall be invoked from the JavaFX thread.
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
     * @param  coverage  the data to show in this viewer, or {@code null} if none.
     *
     * @see #coverageProperty
     */
    public final void setCoverage(final GridCoverage coverage) {
        assert Platform.isFxApplicationThread();
        coverageProperty.set(coverage);
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
     *
     * @param  sliceExtent  subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @see #sliceExtentProperty
     * @see GridCoverage#render(GridExtent)
     */
    public final void setSliceExtent(final GridExtent sliceExtent) {
        sliceExtentProperty.set(sliceExtent);
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
     * Sets the background, as a color for now but more patterns may be allowed in a future version.
     */
    final void setBackground(final Color color) {
        fixedPane.setBackground(new Background(new BackgroundFill(color, null, null)));
    }

    /**
     * Sets the Coordinate Reference System in which the coverage is resampled before displaying.
     * The new CRS must be compatible with the previous CRS, i.e. a coordinate operation between
     * the two CRSs shall exist.
     *
     * @param  newValue  the new Coordinate Reference System in which to resample the coverage before displaying.
     * @param  anchor    the point to keep at fixed display coordinates, expressed in any compatible CRS.
     *                   If {@code null}, defaults to {@linkplain #getPointOfInterest() point of interest}.
     *                   If non-null, the anchor must be associated to a CRS.
     * @throws RenderException if the objective CRS can not be set to the given value.
     *
     * @hidden
     */
    @Override
    public void setObjectiveCRS(final CoordinateReferenceSystem newValue, DirectPosition anchor) throws RenderException {
        final Long id = LogHandler.loadingStart(originator);
        try {
            super.setObjectiveCRS(newValue, anchor);
        } finally {
            LogHandler.loadingStop(id);
        }
    }

    /**
     * Sets canvas properties from the given grid geometry.
     *
     * @param  newValue  the grid geometry from which to get new canvas properties.
     * @throws RenderException if the given grid geometry can not be converted to canvas properties.
     *
     * @hidden
     */
    @Override
    public void setGridGeometry(final GridGeometry newValue) throws RenderException {
        final Long id = LogHandler.loadingStart(originator);
        try {
            super.setGridGeometry(newValue);
        } finally {
            LogHandler.loadingStop(id);
        }
    }

    /**
     * Invoked when a new coverage has been specified or when the slice extent changed.
     * This method fetches the image (which may imply data loading) in a background thread.
     */
    private void onImageSpecified() {
        final GridCoverage coverage = getCoverage();
        if (coverage == null) {
            clear();
        } else {
            final GridExtent sliceExtent = getSliceExtent();
            execute(new Task<RenderedImage>() {
                /**
                 * The coverage geometry reduced to two dimensions and with a translation taking in account
                 * the {@code sliceExtent}. That value will be stored in {@link CoverageCanvas#dataGeometry}.
                 */
                private GridGeometry imageGeometry;

                /**
                 * Invoked in a background thread for fetching the image and computing its geometry. The image
                 * geometry should be provided by {@value PlanarImage#GRID_GEOMETRY_KEY} property. But if that
                 * property is not provided, {@link ImageRenderer} is used as a fallback for computing it.
                 */
                @Override protected RenderedImage call() throws FactoryException {
                    final Long id = LogHandler.loadingStart(originator);
                    try {
                        final RenderedImage image = coverage.render(sliceExtent);
                        final Object value = image.getProperty(PlanarImage.GRID_GEOMETRY_KEY);
                        imageGeometry = (value instanceof GridGeometry) ? (GridGeometry) value
                                      : new ImageRenderer(coverage, sliceExtent).getImageGeometry(BIDIMENSIONAL);
                        return image;
                    } finally {
                        LogHandler.loadingStop(id);
                    }
                }

                /**
                 * Invoked when an error occurred while loading an image or processing it.
                 * This method popups the dialog box immediately because it is considered
                 * an important error.
                 */
                @Override protected void failed() {
                    final Throwable ex = getException();
                    errorOccurred(ex);
                    ExceptionReporter.canNotUseResource(ex);
                }

                /**
                 * Invoked in JavaFX thread for setting the image to the instance we just fetched.
                 */
                @Override protected void succeeded() {
                    setRawImage(getValue(), imageGeometry);
                }
            });
        }
    }

    /**
     * Invoked when a new image has been successfully loaded. The given image must be the "raw" image,
     * without resampling and without color ramp stretching. The call to this method is followed by a
     * a repaint event, which will cause the image to be resampled in a background thread.
     */
    private void setRawImage(final RenderedImage image, final GridGeometry imageGeometry) {
        resampledImages.clear();
        data.setImage(image, imageGeometry);
        Envelope bounds = null;
        if (imageGeometry != null && imageGeometry.isDefined(GridGeometry.ENVELOPE)) {
            bounds = imageGeometry.getEnvelope();
        }
        setObjectiveBounds(bounds);
        requestRepaint();                       // Cause `Worker` class to be executed.
    }

    /**
     * Invoked when a new interpolation has been specified.
     */
    private void onInterpolationSpecified(final Interpolation newValue) {
        data.processor.setInterpolation(newValue);
        resampledImages.clear();
        requestRepaint();
    }

    /**
     * Invoked in JavaFX thread for creating a renderer to be executed in a background thread.
     * This method prepares the information needed but does not start the rendering itself.
     * The rendering will be done later by a call to {@link Renderer#paint(Graphics2D)}.
     */
    @Override
    protected Renderer createRenderer() {
        return data.isEmpty() ? null : new Worker(this);
    }

    /**
     * Resample and paint image in the canvas. This class performs some or all of the following tasks, in order.
     * It is possible to skip the first tasks if they are already done, but after the work started at some point
     * all remaining points are executed:
     *
     * <ol>
     *   <li>Compute statistics on sample values (if needed).</li>
     *   <li>Resample the image (if needed).</li>
     *   <li>Paint the image.</li>
     * </ol>
     */
    private static final class Worker extends Renderer {
        /**
         * Value of {@link CoverageCanvas#data} at the time this worker has been initialized.
         */
        private final RenderingData data;

        /**
         * The coordinate reference system in which to reproject the data.
         */
        private final CoordinateReferenceSystem objectiveCRS;

        /**
         * The conversion from {@link #objectiveCRS} to the canvas display CRS.
         */
        private final LinearTransform objectiveToDisplay;

        /**
         * The source image after resampling.
         */
        private RenderedImage resampledImage;

        /**
         * The resampled image after color ramp stretching and/or index color model applied.
         */
        private RenderedImage recoloredImage;

        /**
         * The filtered image with tiles computed in advance. The set of prefetched
         * tiles may differ at each rendering event. This image should not be cached
         * after rendering operation is completed.
         */
        private RenderedImage prefetchedImage;

        /**
         * Conversion from {@link #resampledImage} (also {@link #prefetchedImage})
         * pixel coordinates to display coordinates.
         */
        private AffineTransform resampledToDisplay;

        /**
         * Size and location of the display device, in pixel units.
         */
        private final Envelope2D displayBounds;

        /**
         * The resource from which the data has been read, or {@code null} if unknown.
         * This is used only for determining a target window for logging records.
         */
        private final Reference<Resource> originator;

        /**
         * Creates a new renderer.
         */
        Worker(final CoverageCanvas canvas) {
            originator         = canvas.originator;
            data               = canvas.data.clone();
            objectiveCRS       = canvas.getObjectiveCRS();
            objectiveToDisplay = canvas.getObjectiveToDisplay();
            displayBounds      = canvas.getDisplayBounds();
            if (data.validateCRS(objectiveCRS)) {
                resampledImage = canvas.resampledImages.get(Stretching.NONE);
                recoloredImage = canvas.resampledImages.get(data.selectedDerivative);
            }
        }

        /**
         * Returns the bounds of the image part which is currently shown. This method can be invoked
         * only after {@link #render()}. It returns {@code null} if the visible bounds are unknown.
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
         * Invoked in background thread for resampling the image or stretching the color ramp.
         * This method performs some of the steps documented in class Javadoc, with possibility
         * to skip the first step if the required source image is already resampled.
         */
        @Override
        @SuppressWarnings("PointlessBitwiseExpression")
        protected void render() throws TransformException {
            final Long id = LogHandler.loadingStart(originator);
            try {
                boolean isResampled = (resampledImage != null);
                if (isResampled) {
                    resampledToDisplay = data.getTransform(objectiveToDisplay);
                    // Recompute if anything else than identity or translation.
                    isResampled = (resampledToDisplay.getType()
                            & ~(AffineTransform.TYPE_IDENTITY | AffineTransform.TYPE_TRANSLATION)) == 0;
                }
                if (!isResampled) {
                    recoloredImage = null;
                    resampledImage = data.resample(objectiveCRS, objectiveToDisplay);
                    resampledToDisplay = data.getTransform(objectiveToDisplay);
                }
                if (recoloredImage == null) {
                    recoloredImage = data.recolor(resampledImage);
                }
                prefetchedImage = data.prefetch(recoloredImage, resampledToDisplay, displayBounds);
            } finally {
                LogHandler.loadingStop(id);
            }
        }

        /**
         * Draws the image in a background buffer after {@link #render()} finished to prepare data.
         */
        @Override
        protected void paint(final Graphics2D gr) {
            gr.drawRenderedImage(prefetchedImage, resampledToDisplay);
        }

        /**
         * Invoked in JavaFX thread after successful {@link #paint(Graphics2D)} completion.
         * This method stores the computation results.
         */
        @Override
        protected boolean commit(final MapCanvas canvas) {
            ((CoverageCanvas) canvas).cacheRenderingData(this);
            return super.commit(canvas);
        }
    }

    /**
     * Invoked after a paint event for caching rendering data.
     * If the resampled image changed, all previously cached images are discarded.
     */
    private void cacheRenderingData(final Worker worker) {
        data = worker.data;
        final RenderedImage newValue = worker.resampledImage;
        final RenderedImage oldValue = resampledImages.put(Stretching.NONE, newValue);
        if (oldValue != newValue && oldValue != null) {
            /*
             * If resampled image changed, then all derivative images (with stretched color ramp
             * or other operation applied) are not valid anymore. We need to empty the cache.
             */
            resampledImages.clear();
            resampledImages.put(Stretching.NONE, newValue);
        }
        resampledImages.put(data.selectedDerivative, worker.recoloredImage);
        /*
         * Notify the "Image properties" tab that the image changed.
         */
        if (imageProperty != null) {
            imageProperty.setImage(worker.recoloredImage, worker.getVisibleImageBounds());
        }
        if (statusBar != null) {
            final Object value = worker.recoloredImage.getProperty(PlanarImage.POSITIONAL_ACCURACY_KEY);
            Quantity<Length> accuracy = null;
            if (value instanceof Quantity<?>[]) {
                for (final Quantity<?> q : (Quantity<?>[]) value) {
                    if (Units.isLinear(q.getUnit())) {
                        accuracy = q.asType(Length.class);
                        accuracy = GUIUtilities.shorter(accuracy, accuracy.getUnit().getConverterTo(Units.METRE)
                                                                    .convert(accuracy.getValue().doubleValue()));
                        break;
                    }
                }
            }
            statusBar.setLowestAccuracy(accuracy);
        }
    }

    /**
     * Returns the bounds of the image part which is currently shown. This method performs the same work
     * than {@link Worker#getVisibleImageBounds()} is a less efficient way. It is used when no worker is
     * available.
     *
     * @see Worker#getVisibleImageBounds()
     */
    private Rectangle getVisibleImageBounds() {
        final Envelope2D displayBounds = getDisplayBounds();
        final AffineTransform resampledToDisplay = data.getTransform(getObjectiveToDisplay());
        try {
            return (Rectangle) AffineTransforms2D.inverseTransform(resampledToDisplay, displayBounds, new Rectangle());
        } catch (NoninvertibleTransformException e) {
            unexpectedException(e);                     // Should never happen.
        }
        return null;
    }

    /**
     * Invoked by {@link CoverageControls} when the user selected a new color stretching mode.
     * The sample values are assumed the same; only the image appearance is modified.
     */
    final void setStyling(final Stretching selection) {
        if (data.selectedDerivative != selection) {
            data.selectedDerivative = selection;
            requestRepaint();
        }
    }

    /**
     * Invoked when an exception occurred while computing a transform but the painting process can continue.
     */
    private static void unexpectedException(final Exception e) {
        Logging.unexpectedException(Logging.getLogger(Modules.APPLICATION), CoverageCanvas.class, "render", e);
    }

    /**
     * Removes the image shown and releases memory.
     */
    @Override
    protected void clear() {
        setRawImage(null, null);
        super.clear();
    }
}
