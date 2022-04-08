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
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.ImageCombiner;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.multiplyExact;
import static java.lang.Math.toIntExact;
import static java.lang.Math.round;
import static java.lang.Math.min;
import static java.lang.Math.max;
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
 *   <li>Invoke {@link #accept accept(…)} or {@link #resample resample(…)}
 *       methods for each coverage to combine.</li>
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
 * @version 1.2
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
     * The image combiners to use for combining two-dimensional slices.
     * There is one {@link ImageCombiner} instance per slice, created when first needed.
     */
    private final ImageCombiner[] sliceCombiners;

    /**
     * Number of dimensions of the destination grid coverage.
     */
    private final int dimension;

    /**
     * The dimension to extract as {@link RenderedImage}s.
     * This is usually 0 for <var>x</var> and 1 for <var>y</var>.
     */
    private final int xdim, ydim;

    /**
     * The offset to subtract to grid index before to compute the index of a slice.
     */
    private final long[] sliceIndexOffsets;

    /**
     * The multiplication factor to apply of grid coordinates (after subtracting the offset)
     * for computing slice coordinates.
     */
    private final int[] sliceIndexSpans;

    /**
     * Creates a coverage combiner which will write in the given coverage.
     * The coverage is not cleared; cells that are not overwritten by calls
     * to the {@code accept(…)} method will be left unchanged.
     *
     * @param  destination  the coverage where to combine coverages.
     * @param  xdim         the dimension to extract as {@link RenderedImage} <var>x</var> axis. This is usually 0.
     * @param  ydim         the dimension to extract as {@link RenderedImage} <var>y</var> axis. This is usually 1.
     */
    public CoverageCombiner(final GridCoverage destination, final int xdim, final int ydim) {
        this.destination = destination;
        final GridExtent extent = destination.getGridGeometry().getExtent();
        dimension = extent.getDimension();
        ArgumentChecks.ensureBetween("xdim", 0, dimension-1, xdim);
        ArgumentChecks.ensureBetween("ydim", 0, dimension-1, ydim);
        if (xdim == ydim) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedNumber_1, xdim));
        }
        this.xdim = xdim;
        this.ydim = ydim;
        sliceIndexOffsets = new long[dimension - BIDIMENSIONAL];
        sliceIndexSpans   = new int [dimension - BIDIMENSIONAL];
        int sliceCount    = 1;
        for (int j=0,i=0; i<dimension; i++) {
            if (i != xdim && i != ydim) {
                final int span;
                sliceIndexOffsets[j] = extent.getLow(i);
                sliceIndexSpans[j++] = span = toIntExact(extent.getSize(i));
                sliceCount = multiplyExact(sliceCount, span);
            }
        }
        sliceCombiners = new ImageCombiner[sliceCount];
        processor = new ImageProcessor();
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
     * Writes the given coverage on top of destination coverage.
     * The given coverage is resampled to the grid geometry of the destination coverage.
     *
     * @param  source  the coverage to write on top of destination coverage.
     * @return {@code true} on success, or {@code false} if at least one slice
     *         in the destination coverage is not writable.
     * @throws TransformException if the coordinates of given coverage can not be transformed
     *         to the coordinates of destination coverage.
     */
    public boolean accept(final GridCoverage source) throws TransformException {
        final Dimension margin = processor.getInterpolation().getSupportSize();
        margin.width  = ((margin.width  + 1) >> 1) + 1;
        margin.height = ((margin.height + 1) >> 1) + 1;
        final long[] minIndices = new long[dimension];      // Will be expanded by above margin.
        final long[] maxIndices = new long[dimension];      // Inclusive.
        /*
         * Compute the intersection between `source` and `destination`, in units
         * of destination cell indices. A margin is added for interpolations.
         * This block also verifies that the intersection exists.
         */
        final double[] centerIndices;
        final double[] centerSourceIndices;
        final MathTransform toSourceSliceCorner;
        final MathTransform toSourceSliceCenter;
        {   // For keeping following variables in a local scope.
            final GridGeometry targetGG = destination.getGridGeometry();
            final GridGeometry sourceGG = source.getGridGeometry();
            final GridExtent   sourceEx = sourceGG.getExtent();
            final GridExtent   targetEx = targetGG.getExtent();
            centerIndices       = targetEx.getPointOfInterest();
            centerSourceIndices = new double[sourceEx.getDimension()];
            toSourceSliceCorner = targetGG.createTransformTo(sourceGG, PixelInCell.CELL_CORNER);
            toSourceSliceCenter = targetGG.createTransformTo(sourceGG, PixelInCell.CELL_CENTER);
            final Envelope env  = sourceEx.toEnvelope(toSourceSliceCorner.inverse());
            for (int i=0; i<dimension; i++) {
                minIndices[i] = max(targetEx.getLow (i), round(env.getMinimum(i)));
                maxIndices[i] = min(targetEx.getHigh(i), round(env.getMaximum(i) - 1));
                if (minIndices[i] >= maxIndices[i]) {
                    throw new DisjointExtentException();
                }
            }
        }
        /*
         * Now apply `ImageCombiner` for each two-dimensional slice. We will iterate on all destination slices
         * in the intersection area, and locate the corresponding source slices (this is a strategy similar to
         * the resampling of pixel values in rasters).
         */
        final long[] minSliceIndices  = minIndices.clone();
        final long[] maxSliceIndices  = maxIndices.clone();
        final long[] minSourceIndices = new long[centerSourceIndices.length];
        final long[] maxSourceIndices = new long[centerSourceIndices.length];
        boolean success = true;
next:   for (;;) {
            /*
             * Compute the index in `sliceCombiners` array for the two-dimensional
             * slice identified by the current value of the `slice` coordinates.
             */
            int sliceIndex = 0;
            for (int j=0,i=0; i<dimension; i++) {
                if (i != xdim && i != ydim) {
                    maxSliceIndices[i] = minSliceIndices[i];
                    int offset = toIntExact(subtractExact(minSliceIndices[i], sliceIndexOffsets[j]));
                    offset = multiplyExact(offset, sliceIndexSpans[j++]);
                    sliceIndex = addExact(sliceIndex, offset);
                }
            }
            /*
             * Get the image for the current slice. It may be the result of a previous combination.
             * If the image is not writable, we skip that slice and try the next one.
             * A flag will report to the user that at least one slice was non-writable.
             */
            final GridExtent targetSliceExtent = new GridExtent(null, minSliceIndices, maxSliceIndices, true);
            final RenderedImage targetSlice;
            ImageCombiner combiner = sliceCombiners[sliceIndex];
            if (combiner != null) {
                targetSlice = combiner.result();
            } else {
                targetSlice = destination.render(targetSliceExtent);
                if (targetSlice instanceof WritableRenderedImage) {
                    combiner = new ImageCombiner((WritableRenderedImage) targetSlice, processor);
                    sliceCombiners[sliceIndex] = combiner;
                } else {
                    success = false;
                }
            }
            /*
             * Compute the bounds of the source image to load (with a margin for rounding and interpolations).
             * For all dimensions other than the slice dimensions, we take the center of the slice to read.
             */
            if (combiner != null) {
                toSourceSliceCenter.transform(centerIndices, 0, centerSourceIndices, 0, 1);
                final Envelope sourceArea = targetSliceExtent.toEnvelope(toSourceSliceCorner);
                for (int i=0; i<minSourceIndices.length; i++) {
                    if (i == xdim || i == ydim) {
                        final int m = (i == xdim) ? margin.width : margin.height;
                        minSourceIndices[i] = saturatingSubtract(round(sourceArea.getMinimum(i)), m  );
                        maxSourceIndices[i] = saturatingAdd     (round(sourceArea.getMaximum(i)), m-1);
                    } else {
                        minSourceIndices[i] = round(centerSourceIndices[i]);
                        maxSourceIndices[i] = minSourceIndices[i];
                    }
                }
                GridExtent sourceSliceExtent = new GridExtent(null, minSourceIndices, maxSourceIndices, true);
                /*
                 * Get the source image and combine with the corresponding slice of destination coverage.
                 */
                RenderedImage sourceSlice = source.render(sourceSliceExtent);
                MathTransform toSource =
                        getGridGeometry(targetSlice, destination, targetSliceExtent).createTransformTo(
                        getGridGeometry(sourceSlice, source,      sourceSliceExtent), PixelInCell.CELL_CENTER);
                combiner.resample(sourceSlice, null, toSource);
            }
            /*
             * Increment indices to the next slice.
             */
            for (int i=0; i<dimension; i++) {
                if (i != xdim && i != ydim) {
                    if (minSliceIndices[i]++ <= maxIndices[i]) {
                        continue next;
                    }
                    minSliceIndices[i] = minIndices[i];
                }
            }
            break;
        }
        return success;
    }
}
