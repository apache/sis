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
package org.apache.sis.internal.coverage;

import java.util.Arrays;
import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.ImageRenderer;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.ImageCombiner;
import org.apache.sis.image.Interpolation;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.round;
import static org.apache.sis.internal.util.Numerics.saturatingAdd;
import static org.apache.sis.internal.util.Numerics.saturatingSubtract;


/**
 * Combines an arbitrary amount of coverages into a single one.
 * The combined coverages may use different coordinate systems.
 * The workflow is as below:
 *
 * <ol>
 *   <li>Creates a {@code CoverageCombiner} with the destination coverage where to write.</li>
 *   <li>Configure with methods such as {@link #setInterpolation setInterpolation(…)}.</li>
 *   <li>Invoke {@link #apply apply(…)} methods for each list of coverages to combine.</li>
 *   <li>Get the combined coverage with {@link #result()}.</li>
 * </ol>
 *
 * Coverages are combined in the order they are specified.
 *
 * <h2>Limitations</h2>
 * Current implementation does not apply interpolations except in the two dimensions
 * specified at construction time. For all other dimensions, data are taken from the
 * nearest neighbor two-dimensional slice.
 *
 * <p>In addition, current implementation does not verify if sample dimensions are in the same order,
 * and does not expand the destination coverage for accommodating data in given coverages that would
 * be outside the bounds of destination coverage.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see ImageCombiner
 *
 * @since 1.2
 * @module
 */
public final class CoverageCombiner {
    /**
     * The {@value} value for identifying code expecting exactly 2 dimensions.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * The image processor for resampling operation.
     * The same processor is used for all slices.
     */
    private final ImageProcessor processor;

    /**
     * The destination coverage where to write the coverages given to this {@code CoverageCombiner}.
     */
    private final GridCoverage destination;

    /**
     * The dimension to extract as {@link RenderedImage}s.
     * This is usually 0 for <var>x</var> and 1 for <var>y</var>.
     */
    private final int xdim, ydim;

    /**
     * Creates a coverage combiner which will write in the given coverage.
     * The coverage is not cleared; cells that are not overwritten by calls
     * to the {@code accept(…)} method will be left unchanged.
     *
     * @param  destination  the destination coverage where to combine source coverages.
     * @param  xdim         the dimension to extract as {@link RenderedImage} <var>x</var> axis. This is usually 0.
     * @param  ydim         the dimension to extract as {@link RenderedImage} <var>y</var> axis. This is usually 1.
     */
    public CoverageCombiner(final GridCoverage destination, final int xdim, final int ydim) {
        ArgumentChecks.ensureNonNull("destination", destination);
        this.destination = destination;
        final int dimension = destination.getGridGeometry().getDimension();
        ArgumentChecks.ensureBetween("xdim", 0, dimension-1, xdim);
        ArgumentChecks.ensureBetween("ydim", 0, dimension-1, ydim);
        if (xdim == ydim) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedNumber_1, xdim));
        }
        this.xdim = xdim;
        this.ydim = ydim;
        processor = new ImageProcessor();
    }

    /**
     * Returns the interpolation method to use during resample operations.
     *
     * <h4>Limitations</h4>
     * In current version, the interpolation is applied only in the {@code xdim} and {@code ydim} dimensions
     * specified at construction time. For all other dimensions, nearest neighbor interpolation is applied.
     *
     * @return interpolation method to use during resample operations.
     */
    public Interpolation getInterpolation() {
        return processor.getInterpolation();
    }

    /**
     * Sets the interpolation method to use during resample operations.
     *
     * <h4>Limitations</h4>
     * In current version, the interpolation is applied only in the {@code xdim} and {@code ydim} dimensions
     * specified at construction time. For all other dimensions, nearest neighbor interpolation is applied.
     *
     * @param  method  interpolation method to use during resample operations.
     */
    public void setInterpolation(final Interpolation method) {
        processor.setInterpolation(method);
    }

    /**
     * Returns information about conversion from pixel coordinates to "real world" coordinates.
     * This is taken from {@link PlanarImage#GRID_GEOMETRY_KEY} if available, or computed otherwise.
     *
     * @param  image     the image from which to get the conversion.
     * @param  coverage  the coverage to use as a fallback if the information is not provided with the image.
     * @param  slice     identification of the slice to read in the coverage.
     * @return information about conversion from pixel to "real world" coordinates.
     */
    private static GridGeometry getGridGeometry(final RenderedImage image,
            final GridCoverage coverage, final GridExtent slice)
    {
        final Object value = image.getProperty(PlanarImage.GRID_GEOMETRY_KEY);
        if (value instanceof GridGeometry) {
            return (GridGeometry) value;
        }
        return new ImageRenderer(coverage, slice).getImageGeometry(BIDIMENSIONAL);
    }

    /**
     * Writes the given coverages on top of the destination coverage.
     * The given coverages are resampled to the grid geometry of the destination coverage.
     * Coverages that do not intercept with the destination coverage are silently ignored.
     *
     * @param  sources  the coverages to write on top of destination coverage.
     * @return {@code true} on success, or {@code false} if at least one slice
     *         in the destination coverage is not writable.
     * @throws TransformException if the coordinates of a given coverage can not be transformed
     *         to the coordinates of destination coverage.
     */
    public boolean apply(GridCoverage... sources) throws TransformException {
        ArgumentChecks.ensureNonNull("sources", sources);
        sources = sources.clone();
        final GridGeometry    targetGG            = destination.getGridGeometry();
        final GridExtent      targetEx            = targetGG.getExtent();
        final int             dimension           = targetEx.getDimension();
        final long[]          minIndices          = new long[dimension]; Arrays.fill(minIndices, Long.MAX_VALUE);
        final long[]          maxIndices          = new long[dimension]; Arrays.fill(maxIndices, Long.MIN_VALUE);
        final MathTransform[] toSourceSliceCorner = new MathTransform[sources.length];
        final MathTransform[] toSourceSliceCenter = new MathTransform[sources.length];
        /*
         * Compute the intersection between `source` and `destination`, in units of destination cell indices.
         * If a coverage does not intersect the destination, the corresponding element in the `sources` array
         * will be set to null.
         */
next:   for (int j=0; j<sources.length; j++) {
            final GridCoverage source = sources[j];
            ArgumentChecks.ensureNonNullElement("sources", j, source);
            final GridGeometry  sourceGG = source.getGridGeometry();
            final GridExtent    sourceEx = sourceGG.getExtent();
            final MathTransform toSource = targetGG.createTransformTo(sourceGG, PixelInCell.CELL_CORNER);
            final Envelope      env      = sourceEx.toEnvelope(toSource.inverse());
            final long[]        min      = new long[dimension];
            final long[]        max      = new long[dimension];
            for (int i=0; i<dimension; i++) {
                /*
                 * The conversion from `double` to `long` may loose precision. The -1 (for making the maximum value
                 * inclusive) is done on the floating point value instead of the integer value in order to have the
                 * same rounding when the minimum and maximum values are close to each other.
                 * The goal is to avoid spurious "disjoint extent" exceptions.
                 */
                min[i] = Math.max(targetEx.getLow (i), round(env.getMinimum(i)));
                max[i] = Math.min(targetEx.getHigh(i), round(env.getMaximum(i) - 1));
                if (min[i] > max[i]) {
                    sources[j] = null;
                    continue next;
                }
            }
            /*
             * Expand the destination extent only if the source intersects it.
             * It can be done only after we tested all dimensions.
             */
            for (int i=0; i<dimension; i++) {
                minIndices[i] = Math.min(minIndices[i], min[i]);
                maxIndices[i] = Math.max(maxIndices[i], max[i]);
            }
            toSourceSliceCenter[j] = targetGG.createTransformTo(sourceGG, PixelInCell.CELL_CENTER);
            toSourceSliceCorner[j] = toSource;
        }
        if (ArraysExt.allEquals(sources, null)) {
            return true;                                // No intersection. We "successfully" wrote nothing.
        }
        /*
         * Now apply `ImageCombiner` for each two-dimensional slice. We will iterate on all destination slices
         * in the intersection area, and locate the corresponding source slices (this is a strategy similar to
         * the resampling of pixel values in rasters).
         */
        final long[] minSliceIndices = minIndices.clone();
        final long[] maxSliceIndices = maxIndices.clone();
        final double[] centerIndices = targetEx.getPointOfInterest(PixelInCell.CELL_CENTER);
        final Dimension margin = processor.getInterpolation().getSupportSize();
        margin.width  = ((margin.width  + 1) >> 1) + 1;
        margin.height = ((margin.height + 1) >> 1) + 1;
        boolean success = true;
next:   for (;;) {
            /*
             * Get the image for the current slice to write.
             * If the image is not writable, we skip that slice and try the next one.
             * A flag will report to the user that at least one slice was non-writable.
             */
            final GridExtent targetSliceExtent = new GridExtent(null, minSliceIndices, maxSliceIndices, true);
            final RenderedImage targetSlice = destination.render(targetSliceExtent);
            if (targetSlice instanceof WritableRenderedImage) {
                final ImageCombiner combiner = new ImageCombiner((WritableRenderedImage) targetSlice, processor);
                for (int j=0; j<sources.length; j++) {
                    final GridCoverage source = sources[j];
                    if (source == null) {
                        continue;
                    }
                    /*
                     * Compute the bounds of the source image to load (with a margin for rounding and interpolations).
                     * For all dimensions other than the slice dimensions, we take the center of the slice to read.
                     */
                    final int      srcDim = source.getGridGeometry().getDimension();
                    final long[]   minSourceIndices    = new long  [srcDim];
                    final long[]   maxSourceIndices    = new long  [srcDim];
                    final double[] centerSourceIndices = new double[srcDim];
                    toSourceSliceCenter[j].transform(centerIndices, 0, centerSourceIndices, 0, 1);
                    final Envelope env = targetSliceExtent.toEnvelope(toSourceSliceCorner[j]);
                    for (int i=0; i<srcDim; i++) {
                        if (i == xdim || i == ydim) {
                            final int m = (i == xdim) ? margin.width : margin.height;
                            minSourceIndices[i] = saturatingSubtract(round(env.getMinimum(i)), m  );
                            maxSourceIndices[i] = saturatingAdd     (round(env.getMaximum(i)), m-1);
                        } else {
                            minSourceIndices[i] = round(centerSourceIndices[i]);
                            maxSourceIndices[i] = minSourceIndices[i];
                        }
                    }
                    /*
                     * Get the source image and combine with the corresponding slice of destination coverage.
                     */
                    GridExtent sourceSliceExtent = new GridExtent(null, minSourceIndices, maxSourceIndices, true);
                    RenderedImage sourceSlice = source.render(sourceSliceExtent);
                    MathTransform toSource =
                            getGridGeometry(targetSlice, destination, targetSliceExtent).createTransformTo(
                            getGridGeometry(sourceSlice, source,      sourceSliceExtent), PixelInCell.CELL_CENTER);
                    combiner.resample(sourceSlice, null, toSource);
                }
            } else {
                success = false;
            }
            /*
             * Increment indices to the next slice.
             */
            for (int i=0; i<dimension; i++) {
                if (i != xdim && i != ydim) {
                    long index = minSliceIndices[i];
                    boolean done = index++ <= maxIndices[i];
                    if (!done)     index    = minIndices[i];
                    maxSliceIndices[i] = minSliceIndices[i] = index;
                    if (done) {
                        continue next;
                    }
                }
            }
            break;
        }
        return success;
    }
}
