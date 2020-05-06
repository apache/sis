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
import java.util.Objects;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.gui.ImageRenderings;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.gui.map.MapCanvasAWT;


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
     * Different ways to represent the data. The {@link #data} field shall be one value from this map.
     *
     * @see #setDerivedImage(Stretching, RenderedImage)
     */
    private final EnumMap<Stretching,RenderedImage> stretchedColorRamps;

    /**
     * Key of the currently selected alternative in {@link #stretchedColorRamps} map.
     *
     * @see #setDerivedImage(Stretching, RenderedImage)
     */
    private Stretching currentDataAlternative;

    /**
     * The data to show, or {@code null} if not yet specified. This image may be tiled,
     * and fetching tiles may require computations to be performed in background thread.
     * The size of this {@code RenderedImage} is not necessarily the {@link #image} size.
     * In particular {@code data} way cover a larger area.
     */
    private RenderedImage data;

    /**
     * The {@link GridGeometry#getGridToCRS(PixelInCell)} conversion of rendered {@linkplain #data}
     * as an affine transform. This is often an immutable instance. A null value is synonymous to
     * identity transform.
     */
    private AffineTransform gridToCRS;

    /**
     * Creates a new two-dimensional canvas for {@link RenderedImage}.
     */
    public CoverageCanvas() {
        super(Locale.getDefault());
        coverageProperty       = new SimpleObjectProperty<>(this, "coverage");
        sliceExtentProperty    = new SimpleObjectProperty<>(this, "sliceExtent");
        stretchedColorRamps    = new EnumMap<>(Stretching.class);
        currentDataAlternative = Stretching.NONE;
        coverageProperty   .addListener((p,o,n) -> onImageSpecified());
        sliceExtentProperty.addListener((p,o,n) -> onImageSpecified());
    }

    /**
     * Returns the data which are the source of all alternative images that may be stored in the
     * {@link #stretchedColorRamps} map. All alternative images are computed from this source.
     */
    private RenderedImage getSourceData() {
        return stretchedColorRamps.get(Stretching.NONE);
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
            execute(new Process(coverage, currentDataAlternative));
        }
    }

    /**
     * Invoked when the user selected a new color stretching mode. Also invoked {@linkplain #setRawImage after
     * loading a new image or a new slice} for switching the new image to the same type of range as previously
     * selected. If the image for the specified type is not already available, then this method computes the
     * image in a background thread and refreshes the view after the computation completed.
     */
    final void setStretching(final Stretching type) {
        currentDataAlternative = type;
        final RenderedImage alt = stretchedColorRamps.get(type);
        if (alt != null) {
            setDerivedImage(type, alt);
        } else {
            final RenderedImage source = getSourceData();
            if (source != null) {
                execute(new Process(source, type));
            }
        }
    }

    /**
     * Loads or resample images before to show them in the canvas. This class performs some or all of
     * the following tasks, in order. It is possible to skip the first tasks if they are already done,
     * but after the work started at some point all remaining points are executed:
     *
     * <ol>
     *   <li>Loads the image.</li>
     *   <li>Compute statistics on sample values (if needed).</li>
     *   <li>Reproject the image (if needed).</li>
     * </ol>
     */
    private final class Process extends Task<RenderedImage> {
        /**
         * The coverage from which to fetch an image, or {@code null} if the {@link #source} is already known.
         */
        private final GridCoverage coverage;

        /**
         * The {@linkplain #coverage} slice to fetch, or {@code null} if {@link #coverage} is null
         * or for loading the whole coverage extent.
         */
        private final GridExtent sliceExtent;

        /**
         * The source image, or {@code null} if it will be the result of fetching an image from
         * the {@linkplain #coverage}. If non-null then it should be {@link #getSourceData()}.
         */
        private RenderedImage source;

        /**
         * The color ramp stretching to apply, or {@link Stretching#NONE} if none.
         */
        private final Stretching stretching;

        /**
         * Creates a new process which will load data from the specified coverage.
         */
        Process(final GridCoverage coverage, final Stretching stretching) {
            this.coverage    = coverage;
            this.sliceExtent = getSliceExtent();
            this.stretching  = stretching;
        }

        /**
         * Creates a new process which will resample the given image.
         */
        Process(final RenderedImage source, final Stretching stretching) {
            this.coverage    = null;
            this.sliceExtent = null;
            this.source      = source;
            this.stretching  = stretching;
        }

        /**
         * Invoked in background thread for fetching the image, stretching the color ramp or resampling.
         * This method performs some or all steps documented in class Javadoc, with possibility to skip
         * the first step is required source image is already loaded.
         */
        @Override protected RenderedImage call() {
            if (source == null) {
                source = coverage.render(sliceExtent);
            }
            final RenderedImage derived;
            switch (stretching) {
                case VALUE_RANGE: derived = ImageRenderings.valueRangeStretching(source); break;
                case AUTOMATIC:   derived = ImageRenderings. automaticStretching(source); break;
                default:          derived = source; break;
            }
            return derived;
        }

        /**
         * Invoked in JavaFX thread on success. This method stores the computation results, provided that
         * the settings ({@link #coverage}, source image, <i>etc.</i>) are still the ones for which the
         * computation has been launched.
         */
        @Override protected void succeeded() {
            /*
             * The image is shown only if the coverage and extent did not changed during the time we were
             * loading in background thread (if they changed, another thread is probably running for them).
             * After `setRawImage(…)` execution, `getSourceData()` should return the given `source`.
             */
            if (coverage != null && coverage.equals(getCoverage()) && Objects.equals(sliceExtent, getSliceExtent())) {
                setRawImage(source, coverage.getGridGeometry(), sliceExtent);
            }
            /*
             * The stretching result is stored only if the user did not changed the image while we were computing
             * statistics in background thread. This method does not verify if user changed the stretching mode;
             * this check will be done by `setDerivedImage(…)`.
             */
            if (source.equals(getSourceData())) {
                setDerivedImage(stretching, getValue());
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
    }

    /**
     * Invoked when a new image has been successfully loaded. The given image must the the "raw" image,
     * without resampling and without color ramp stretching. The {@link #setDerivedImage} method may
     * be invoked after this method for specifying image derived from this raw image.
     *
     * @todo Needs to handle non-affine transform.
     *
     * @param  image        the image to load.
     * @param  geometry     the grid geometry of the coverage that produced the image.
     * @param  sliceExtent  the extent that was requested.
     */
    private void setRawImage(final RenderedImage image, final GridGeometry geometry, GridExtent sliceExtent) {
        data = null;
        stretchedColorRamps.clear();
        setDerivedImage(Stretching.NONE, image);
        try {
            gridToCRS = AffineTransforms2D.castOrCopy(geometry.getGridToCRS(PixelInCell.CELL_CENTER));
        } catch (RuntimeException e) {                      // Conversion not defined or not affine.
            gridToCRS = null;
            errorOccurred(e);
        }
        /*
         * If the user did not specified a sub-region, set the initial visible area to the envelope
         * of the whole coverage. The `setObjectiveBounds(…)` method will take care of computing an
         * initial "objective to display" transform from that information.
         */
        Envelope visibleArea = null;
        if (sliceExtent == null) {
            if (gridToCRS != null && geometry.isDefined(GridGeometry.ENVELOPE)) {
                // This envelope is valid only if we are able to use the `gridToCRS`.
                visibleArea = geometry.getEnvelope();
            }
            if (geometry.isDefined(GridGeometry.EXTENT)) {
                sliceExtent = geometry.getExtent();
            }
        }
        /*
         * If geospatial area declared in grid geometry can not be used, compute it from grid extent.
         * It is the case for example when only a sub-region has been fetched.
         */
        if (sliceExtent != null) {
            if (visibleArea == null) try {
                visibleArea = sliceExtent.toEnvelope((gridToCRS != null)
                                ? AffineTransforms2D.toMathTransform(gridToCRS)
                                : MathTransforms.identity(sliceExtent.getDimension()));
            } catch (TransformException e) {
                // Should never happen because we used an affine transform.
                errorOccurred(e);
            }
            /*
             * Coordinate (0,0) in the image corresponds to the lowest coordinates requested.
             * For taking that offset in account, we need to apply a translation.
             */
            if (gridToCRS != null) {
                final int[] dimensions = sliceExtent.getSubspaceDimensions(BIDIMENSIONAL);
                final long tx = sliceExtent.getLow(dimensions[0]);
                final long ty = sliceExtent.getLow(dimensions[1]);
                if ((tx | ty) != 0) {
                    gridToCRS = new AffineTransform(gridToCRS);
                    gridToCRS.translate(tx, ty);
                }
            }
        }
        setObjectiveBounds(visibleArea);
    }

    /**
     * Invoked in JavaFX thread for setting the image to show. The given image should be a slice
     * produced by current value of {@link #coverageProperty} (should be verified by the caller).
     *
     * @param  type  the type of range used for scaling the color ramp of given image.
     * @param  alt   the image or alternative image to show (can be {@code null}).
     */
    private void setDerivedImage(final Stretching type, RenderedImage alt) {
        /*
         * Store the result but do not necessarily show it because maybe the user changed the
         * `Stretching` during the time the background thread was working. If the user did not
         * changed the type, then the `alt` variable below will stay unchanged.
         */
        stretchedColorRamps.put(type, alt);
        alt = stretchedColorRamps.get(currentDataAlternative);
        if (!Objects.equals(alt, data)) {
            data = alt;
            requestRepaint();
        }
    }

    /**
     * Invoked in JavaFX thread for creating a renderer to be executed in a background thread.
     * This method prepares the information needed but does not start the rendering itself.
     * The rendering will be done later by a call to {@link Renderer#paint(Graphics2D)}.
     */
    @Override
    protected Renderer createRenderer() {
        final RenderedImage data = this.data;       // Need to copy this reference here before background task.
        if (data == null) {
            return null;
        }
        /*
         * At each rendering operation, compute the transform from `data` cell coordinates to pixel coordinates
         * of the image shown in this view. We do this computation every times because `objectiveToDisplay` may
         * vary at any time, and also because we need a new `AffineTransform` instance anyway (we can not reuse
         * an existing instance, because it needs to be stable for use by the background thread).
         */
        final AffineTransform gridToDisplay = new AffineTransform(objectiveToDisplay);
        if (gridToCRS != null) {
            gridToDisplay.concatenate(gridToCRS);
        }
        return new Renderer() {
            @Override protected void paint(final Graphics2D gr) {
                gr.drawRenderedImage(data, gridToDisplay);
            }
        };
    }

    /**
     * Removes the image shown and releases memory.
     */
    @Override
    protected void clear() {
        data = null;
        stretchedColorRamps.clear();
        super.clear();
    }
}
