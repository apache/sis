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

import java.util.Locale;
import java.util.EnumMap;
import java.awt.Graphics2D;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.ImageRenderer;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.gui.map.MapCanvas;
import org.apache.sis.gui.map.MapCanvasAWT;
import org.apache.sis.internal.gui.Resources;


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
     * The {@code RenderedImage} to draw together with transform from pixel coordinates to display coordinates.
     * Shall never be {@code null} but may be {@linkplain RenderingData#isEmpty() empty}. This instance shall be
     * read and modified in JavaFX thread only and cloned if those data needed by a background thread.
     *
     * @see Worker
     */
    private RenderingData data;

    /**
     * The {@link #data} resampled to a CRS which can easily be mapped to {@linkplain #getDisplayCRS() display CRS}.
     * The different values are slight variants of the values associated to {@link Stretching#NONE}, with only the
     * color map changed.
     */
    private final EnumMap<Stretching,RenderedImage> resampledImages;

    /**
     * Creates a new two-dimensional canvas for {@link RenderedImage}.
     */
    public CoverageCanvas() {
        super(Locale.getDefault());
        coverageProperty    = new SimpleObjectProperty<>(this, "coverage");
        sliceExtentProperty = new SimpleObjectProperty<>(this, "sliceExtent");
        resampledImages     = new EnumMap<>(Stretching.class);
        data                = new RenderingData();
        coverageProperty   .addListener((p,o,n) -> onImageSpecified());
        sliceExtentProperty.addListener((p,o,n) -> onImageSpecified());
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
     * Sets the background, as a color for now but more patterns may be allowed in a future version.
     */
    final void setBackground(final Color color) {
        fixedPane.setBackground(new Background(new BackgroundFill(color, null, null)));
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
                    final RenderedImage image = coverage.render(sliceExtent);
                    final Object value = image.getProperty(PlanarImage.GRID_GEOMETRY_KEY);
                    imageGeometry = (value instanceof GridGeometry) ? (GridGeometry) value
                                  : new ImageRenderer(coverage, sliceExtent).getImageGeometry(BIDIMENSIONAL);
                    return image;
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
                 * Invoked in JavaFX thread for setting the image to the instance we juste fetched.
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
        requestRepaint();   // Cause `Worker` class to be executed.
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
         * The resampled image after stretching.
         */
        private RenderedImage stretchedImage;

        /**
         * Conversion from {@link #resampledImage} (also {@link #stretchedImage}) pixel coordinates
         * to display coordinates.
         */
        private AffineTransform resampledToDisplay;

        /**
         * Creates a new renderer.
         */
        Worker(final CoverageCanvas canvas) {
            data               = canvas.data.clone();
            objectiveCRS       = canvas.getObjectiveCRS();
            objectiveToDisplay = canvas.getObjectiveToDisplay();
            resampledImage     = canvas.resampledImages.get(Stretching.NONE);
            stretchedImage     = canvas.resampledImages.get(data.selectedStretching);
        }

        /**
         * Invoked in background thread for resampling the image or stretching the color ramp.
         * This method performs some of the steps documented in class Javadoc, with possibility
         * to skip the first step if the required source image is already resampled.
         */
        @Override
        @SuppressWarnings("PointlessBitwiseExpression")
        protected void render() throws TransformException {
            boolean isResampled = (resampledImage != null);
            if (isResampled) {
                resampledToDisplay = data.getTransform(objectiveToDisplay);
                // Recompute if anything else than identity or translation.
                isResampled = (resampledToDisplay.getType()
                        & ~(AffineTransform.TYPE_IDENTITY | AffineTransform.TYPE_TRANSLATION)) == 0;
            }
            if (!isResampled) {
                stretchedImage = null;
                resampledImage = data.resample(objectiveCRS, objectiveToDisplay);
                resampledToDisplay = data.getTransform(objectiveToDisplay);
            }
            if (stretchedImage == null) {
                stretchedImage = data.stretch(resampledImage);
            }
        }

        /**
         * Draws the image in a background buffer after {@link #render()} finished to prepare data.
         */
        @Override
        protected void paint(final Graphics2D gr) {
            gr.drawRenderedImage(stretchedImage, resampledToDisplay);
        }

        /**
         * Invoked in JavaFX thread after {@link #paint(Graphics2D)} completion. This method stores
         * the computation results.
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
            resampledImages.clear();
            resampledImages.put(Stretching.NONE, newValue);
        }
        resampledImages.put(data.selectedStretching, worker.stretchedImage);
    }

    /**
     * Invoked when the user selected a new color stretching mode.
     */
    final void setStretching(final Stretching type) {
        data.selectedStretching = type;
        requestRepaint();
    }

    /**
     * Sets the Coordinate Reference System in which all data are transformed before displaying.
     * The new CRS must be compatible with the previous CRS, i.e. a coordinate operation between
     * the two CRSs shall exist. If the CRS can not be set to the specified value, then an error
     * message is shown in the status bar.
     *
     * @param  crs  the new Coordinate Reference System in which to transform all data before displaying.
     */
    @Override
    public void setObjectiveCRS(final CoordinateReferenceSystem crs) {
        resampledImages.clear();
        data.clearCRS();
        try {
            super.setObjectiveCRS(crs);
        } catch (Exception e) {
            errorOccurred(e);
            final Locale locale = getLocale();
            final Resources i18n = Resources.forLocale(locale);
            ExceptionReporter.show(null, i18n.getString(Resources.Keys.CanNotUseRefSys_1,
                    IdentifiedObjects.getDisplayName(crs, locale)), e);
        }
        requestRepaint();
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
