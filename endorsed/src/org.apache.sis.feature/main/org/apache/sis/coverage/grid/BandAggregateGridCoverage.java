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
package org.apache.sis.coverage.grid;

import java.util.Map;
import java.util.TreeMap;
import java.awt.image.RenderedImage;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.image.DataType;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.coverage.internal.shared.BandAggregateArgument;
import org.apache.sis.util.internal.shared.CollectionsExt;


/**
 * A grid coverage where each band (sample dimension) is taken from a selection of bands
 * in a sequence of source coverages.
 *
 * <h2>Restrictions</h2>
 * <ul>
 *   <li>All coverages shall have the same {@linkplain GridCoverage#getGridGeometry() domain}, except for
 *       the grid extent and the translation terms which can vary by integer amounts of grid cells.</li>
 *   <li>All grid extents shall intersect and the intersection area shall be non-empty.</li>
 *   <li>If coverage data are stored in {@link RenderedImage} instances,
 *       then all images shall use the same data type.</li>
 * </ul>
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class BandAggregateGridCoverage extends GridCoverage {
    /**
     * The source grid coverages.
     */
    private final GridCoverage[] sources;

    /**
     * The sample dimensions to use for each source coverage, in order.
     * The length of this array is always equal to {@link #sources} array length.
     * This array may contain {@code null} elements but never contain empty element.
     */
    private final int[][] bandsPerSource;

    /**
     * Total number of bands.
     */
    private final int numBands;

    /**
     * Translations in units of grid cells to apply on an extent of this grid coverage
     * for getting a "grid to CRS" transform compatible with each source.
     */
    private final long[][] gridTranslations;

    /**
     * Index of a sources having the same "grid to CRS" than this grid coverage, or -1 if none.
     */
    private final int sourceOfGridToCRS;

    /**
     * The data type identifying the primitive type used for storing sample values in each band.
     */
    private final DataType dataType;

    /**
     * The processor to use for creating images.
     * The processor {@linkplain ImageProcessor#getColorizer() colorizer}
     * will determine the color model applied on the aggregated images.
     */
    private final ImageProcessor processor;

    /**
     * Creates a new band aggregated coverage from the given sources.
     *
     * @param  aggregate  the source grid coverages together with bands to select.
     * @param  processor  the processor to use for creating images.
     * @throws IllegalArgumentException if there is an incompatibility between some source coverages
     *         or if some band indices are duplicated or outside their range of validity.
     */
    BandAggregateGridCoverage(final BandAggregateArgument<GridCoverage> aggregate, final ImageProcessor processor) {
        super(aggregate.domain(GridCoverage::getGridGeometry), aggregate.ranges());
        this.sources           = aggregate.sources();
        this.bandsPerSource    = aggregate.bandsPerSource(true);
        this.numBands          = aggregate.numBands();
        this.gridTranslations  = aggregate.gridTranslations();
        this.sourceOfGridToCRS = aggregate.sourceOfGridToCRS();
        this.processor         = processor;
        this.dataType          = sources[0].getBandType();
        for (int i=1; i < sources.length; i++) {
            final GridCoverage source = sources[i];
            final DataType type = source.getBandType();
            if (!dataType.equals(type)) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedDataType));
            }
        }
    }

    /**
     * Returns potentially deeper sources than the user supplied coverage.
     * This method unwraps {@link BandAggregateGridCoverage} for making possible to detect that
     * two consecutive coverages are actually the same coverage, with only different bands selected.
     *
     * @param  unwrapper  a handler where to supply the result of an aggregate decomposition.
     */
    static void unwrap(final BandAggregateArgument<GridCoverage>.Unwrapper unwrapper) {
        if (unwrapper.source instanceof BandAggregateGridCoverage) {
            final var aggregate = (BandAggregateGridCoverage) unwrapper.source;
            unwrapper.applySubset(aggregate.sources, aggregate.bandsPerSource, GridCoverage::getSampleDimensions);
        }
    }

    /**
     * Returns the data type identifying the primitive type used for storing sample values in each band.
     */
    @Override
    DataType getBandType() {
        return dataType;
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This operation is potentially costly if the {@code sliceExtent} argument changes often because
     * the previously computed images are unlikely to be reused when the coordinate systems are different.
     * It may result in the same bands being copied may times in different {@link RenderedImage} instances.
     *
     * <h4>Implementation note</h4>
     * We do not compute the rendered image in advance (which would have produced better caching) because
     * the image to cache depends on {@code sliceExtent} if this coverage has more than two dimensions.
     *
     * @param  sliceExtent  a subspace of this grid coverage where all dimensions except two have a size of 1 cell.
     * @return the grid slice as a rendered image. Image location is relative to {@code sliceExtent}.
     */
    @Override
    public RenderedImage render(GridExtent sliceExtent) {
        /*
         * We need a non-null extent for making sure that all rendered images
         * will use coordinates relative to the same extent.
         */
        if (sliceExtent == null) {
            sliceExtent = gridGeometry.getExtent();
        }
        final var images = new RenderedImage[sources.length];
        for (int i=0; i<images.length; i++) {
            images[i] = sources[i].render(sliceExtent.translate(gridTranslations[i]));
        }
        return processor.aggregateBands(images, bandsPerSource);
    }

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     */
    @Override
    public Evaluator evaluator() {
        return new CombinedEvaluator(sources);
    }

    /**
     * An evaluator which delegates to the evaluators of all source grid coverages.
     * This class is not thread-safe.
     *
     * <h2>Implementation note</h2>
     * We need this specialized class instead of relying on the default implementation inherited
     * from {@link GridCoverage} because the latter may invoke {@link #render(GridExtent)} often,
     * which is potentially costly. Furthermore, it may not work well with coverages having more
     * than two dimensions.
     */
    private final class CombinedEvaluator implements Evaluator {
        /**
         * Evaluators from all source coverages.
         */
        private final Evaluator[] sources;

        /**
         * The union of slice coordinates from all sources, or {@code null} if not yet computed.
         * All coverages should have the same values, but we nevertheless compute union in case.
         */
        private Map<Integer, Long> slices;

        /**
         * Result of combining the bands, recycled on each invocation of this method.
         */
        private final double[] aggregate;

        /**
         * Creates a new evaluator which will delegate to the evaluators of all given sources.
         */
        CombinedEvaluator(final GridCoverage[] coverages) {
            sources = new Evaluator[coverages.length];
            for (int i=0; i < coverages.length; i++) {
                sources[i] = coverages[i].evaluator();
            }
            aggregate = new double[numBands];
        }

        /**
         * Returns the coverage that created this evaluator.
         */
        @Override
        public GridCoverage getCoverage() {
            return BandAggregateGridCoverage.this;
        }

        /**
         * Returns the default slice where to perform evaluation, or an empty map if unspecified.
         */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        public Map<Integer, Long> getDefaultSlice() {
            if (slices == null) {
                final var c = new TreeMap<Integer, Long>();
                for (final Evaluator source : sources) {
                    c.putAll(source.getDefaultSlice());
                }
                slices = CollectionsExt.unmodifiableOrCopy(c);
            }
            return slices;
        }

        /**
         * Sets the default slice where to perform evaluation, or an empty map if unspecified.
         */
        @Override
        public void setDefaultSlice(final Map<Integer, Long> slice) {
            slices = null;
            for (final Evaluator source : sources) {
                source.setDefaultSlice(slice);
            }
        }

        /**
         * Returns whether to return {@code null} instead of throwing an exception if a point is outside coverage bounds.
         */
        @Override
        public boolean isNullIfOutside() {
            for (final Evaluator source : sources) {
                if (!source.isNullIfOutside()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Sets whether to return {@code null} instead of throwing an exception if a point is outside coverage bounds.
         */
        @Override
        public void setNullIfOutside(final boolean flag) {
            for (final Evaluator source : sources) {
                source.setNullIfOutside(flag);
            }
        }

        /**
         * Returns {@code true} if this evaluator is allowed to wraparound coordinates that are outside the coverage.
         */
        @Override
        public boolean isWraparoundEnabled() {
            for (final Evaluator source : sources) {
                if (!source.isWraparoundEnabled()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Sets whether this evaluator is allowed to wraparound coordinates that are outside the coverage.
         */
        @Override
        public void setWraparoundEnabled(final boolean allow) {
            for (final Evaluator source : sources) {
                source.setWraparoundEnabled(allow);
            }
        }

        /**
         * Returns a sequence of double values for a given point in the coverage.
         * This method delegates to all source evaluators and merge the results.
         */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        public double[] apply(final DirectPosition point) {
            int offset = 0;
            for (int i=0; i < sources.length; i++) {
                final double[] values = sources[i].apply(point);
                final int[] bands = bandsPerSource[i];
                if (bands == null) {
                    System.arraycopy(values, 0, aggregate, offset, values.length);
                    offset += values.length;
                } else {
                    for (int b : bands) {
                        aggregate[offset++] = values[b];
                    }
                }
            }
            return aggregate;
        }

        /**
         * Converts the specified geospatial position to grid coordinates.
         */
        @Override
        public FractionalGridCoordinates toGridCoordinates(DirectPosition point) throws TransformException {
            if (sourceOfGridToCRS >= 0) {
                return sources[sourceOfGridToCRS].toGridCoordinates(point);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }
}
