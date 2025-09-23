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
package org.apache.sis.coverage;

import java.util.Arrays;
import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import static java.lang.Math.round;
import javax.measure.IncommensurableException;
import javax.measure.Unit;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.ImageRenderer;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.ImageCombiner;
import org.apache.sis.image.Interpolation;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.coverage.internal.shared.SampleDimensions;
import static org.apache.sis.util.internal.shared.Numerics.saturatingAdd;
import static org.apache.sis.util.internal.shared.Numerics.saturatingSubtract;


/**
 * Combines an arbitrary number of coverages into a single one.
 * The combined coverages may use different coordinate systems.
 * The workflow is as below:
 *
 * <ol>
 *   <li>Creates a {@code CoverageCombiner} with the destination coverage where to write.</li>
 *   <li>Configure with methods such as {@link #setInterpolation setInterpolation(…)}.</li>
 *   <li>Invoke {@link #acceptAll acceptAll(…)} methods for each list of coverages to combine.</li>
 *   <li>Get the combined coverage with {@link #result()}.</li>
 * </ol>
 *
 * Coverage domains can have any number of dimensions.
 * Coverages are combined in the order they are specified.
 * For each coverage, sample dimensions are combined in the order they appear, regardless their names.
 * For each sample dimension, values are converted to the unit of measurement of the destination coverage.
 *
 * <h2>Limitations</h2>
 * The current implementation has the following limitations.
 * Those restrictions may be resolved progressively in future Apache SIS versions.
 *
 * <ul>
 *   <li>Supports only {@link GridCoverage} instances, not yet more generic coverages.</li>
 *   <li>No interpolation except in the two dimensions having the largest size (usually the 2 first).
 *       For all other dimensions, data are taken from the nearest neighbor two-dimensional slice.</li>
 *   <li>No expansion of the destination coverage for accommodating data of source coverages
 *       that are outside the destination coverage bounds.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see ImageCombiner
 *
 * @since 1.4
 */
public class CoverageCombiner {
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
     * The other dimensions can have any size (not restricted to 1 cell).
     */
    private final int xdim, ydim;

    /**
     * Whether the {@linkplain #destination} uses converted values.
     */
    private final boolean isConverted;

    /**
     * Creates a coverage combiner which will write in the given coverage.
     * The coverage is not cleared: cells that are not overwritten by calls
     * to the {@code accept(…)} method will be left unchanged.
     *
     * @param  destination  the destination coverage where to combine source coverages.
     * @throws CannotEvaluateException if the coverage does not have at least 2 dimensions.
     */
    public CoverageCombiner(final GridCoverage destination) {
        this.destination = destination.forConvertedValues(true);
        isConverted = (this.destination == destination);
        final int[] dim = destination.getGridGeometry().getExtent().getLargestDimensions(BIDIMENSIONAL);
        xdim = dim[0];
        ydim = dim[1];
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
     * @return information about conversion from pixel coordinates to "real world" coordinates.
     */
    private static GridGeometry getImageGeometry(final RenderedImage image,
            final GridCoverage coverage, final GridExtent slice)
    {
        final Object value = image.getProperty(PlanarImage.GRID_GEOMETRY_KEY);
        if (value instanceof GridGeometry) {
            return (GridGeometry) value;
        }
        return new ImageRenderer(coverage, slice).getImageGeometry(BIDIMENSIONAL);
    }

    /**
     * Returns the conversions from source units to target units.
     * Conversion is fetched for each pair of units at the same index.
     *
     * @param  sources  the source units. May contain null elements.
     * @param  targets  the target units. May contain null elements.
     * @return converters, or {@code null} if none. May contain null elements.
     * @throws IncommensurableException if a pair of units are not convertible.
     */
    private static MathTransform1D[] createUnitConverters(final Unit<?>[] sources, final Unit<?>[] targets)
            throws IncommensurableException
    {
        MathTransform1D[] converters = null;
        final int n = Math.min(sources.length, targets.length);
        for (int i=0; i<n; i++) {
            final Unit<?> source = sources[i];
            final Unit<?> target = targets[i];
            if (source != null && target != null) {
                final MathTransform1D c = MathTransforms.convert(source.getConverterToAny(target));
                if (!c.isIdentity()) {
                    if (converters == null) {
                        converters = new MathTransform1D[n];
                        Arrays.fill(converters, MathTransforms.identity(1));
                    }
                    converters[i] = c;
                }
            }
        }
        return converters;
    }

    /**
     * Writes the given coverages on top of the destination coverage.
     * The given coverages are resampled to the grid geometry of the destination coverage.
     * Coverages that do not intercept with the destination coverage are silently ignored.
     *
     * <h4>Performance note</h4>
     * If there is many coverages to write, they should be specified in a single
     * call to {@code acceptAll(…)} instead of invoking this method multiple times.
     * Bulk operations can reduce the number of calls to {@link GridCoverage#render(GridExtent)}.
     *
     * @param  sources  the coverages to write on top of destination coverage.
     * @return {@code true} on success, or {@code false} if at least one slice
     *         in the destination coverage is not writable.
     * @throws TransformException if the coordinates of a given coverage cannot be transformed
     *         to the coordinates of destination coverage.
     * @throws IncommensurableException if the unit of measurement of at least one source sample dimension
     *         is not convertible to the unit of measurement of the corresponding target sample dimension.
     */
    public boolean acceptAll(GridCoverage... sources) throws TransformException, IncommensurableException {
        sources = sources.clone();
        final GridGeometry        targetGG            = destination.getGridGeometry();
        final GridExtent          targetEx            = targetGG.getExtent();
        final int                 dimension           = targetEx.getDimension();
        final long[]              minIndices          = new long[dimension]; Arrays.fill(minIndices, Long.MAX_VALUE);
        final long[]              maxIndices          = new long[dimension]; Arrays.fill(maxIndices, Long.MIN_VALUE);
        final MathTransform[]     toSourceSliceCorner = new MathTransform  [sources.length];
        final MathTransform[]     toSourceSliceCenter = new MathTransform  [sources.length];
        final MathTransform1D[][] unitConverters      = new MathTransform1D[sources.length][];
        final NumberRange<?>[][]  sourceRanges        = new NumberRange<?> [sources.length][];
        final Unit<?>[]           destinationUnits    = SampleDimensions.units(destination);
        /*
         * Compute the intersection between `source` and `destination`, in units of destination cell indices.
         * If a coverage does not intersect the destination, it will be discarded.
         */
        int numSources = 0;
next:   for (int j=0; j<sources.length; j++) {
            GridCoverage source = sources[j];
            ArgumentChecks.ensureNonNullElement("sources", j, source);
            source = source.forConvertedValues(true);
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
            toSourceSliceCenter[numSources] = targetGG.createTransformTo(sourceGG, PixelInCell.CELL_CENTER);
            toSourceSliceCorner[numSources] = toSource;
            sources            [numSources] = source;
            unitConverters     [numSources] = createUnitConverters(SampleDimensions.units(source), destinationUnits);
            sourceRanges       [numSources] = SampleDimensions.ranges(source);
            numSources++;
        }
        Arrays.fill(sources, numSources, sources.length, null);
        if (numSources == 0) {
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
                for (int j=0; j<numSources; j++) {
                    final GridCoverage source = sources[j];
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
                            minSourceIndices[i] = saturatingSubtract(round(env.getMinimum(i)), m   );
                            maxSourceIndices[i] = saturatingAdd     (round(env.getMaximum(i)), m-1L);
                        } else {
                            minSourceIndices[i] = round(centerSourceIndices[i]);
                            maxSourceIndices[i] = minSourceIndices[i];
                        }
                    }
                    /*
                     * Get the source image and combine with the corresponding slice of destination coverage.
                     * Data are converted to the destination units before the resampling is applied.
                     */
                    GridExtent sourceSliceExtent = new GridExtent(null, minSourceIndices, maxSourceIndices, true);
                    RenderedImage sourceSlice = source.render(sourceSliceExtent);
                    MathTransform1D[] converters = unitConverters[j];
                    if (converters != null) {
                        sourceSlice = processor.convert(sourceSlice, sourceRanges[j], converters, combiner.getBandType());
                    }
                    MathTransform toSource =
                            getImageGeometry(targetSlice, destination, targetSliceExtent).createTransformTo(
                            getImageGeometry(sourceSlice, source,      sourceSliceExtent), PixelInCell.CELL_CENTER);
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

    /**
     * Returns the combination of destination coverage with all coverages specified to {@code CoverageCombiner} methods.
     * This may be the destination coverage specified at construction time, but may also be a larger coverage if the
     * destination has been dynamically expanded for accommodating larger sources.
     *
     * <p><b>Note:</b> dynamic expansion is not yet implemented in current version.
     * If a future version implements it, we shall guarantee that the coordinate of each cell is unchanged
     * (i.e. the grid extent {@code minX} and {@code minY} may become negative, but the cell identified by
     * coordinates (0,0) for instance will stay the same cell.)</p>
     *
     * @return the combination of destination coverage with all source coverages.
     */
    public GridCoverage result() {
        return destination.forConvertedValues(isConverted);
    }
}
