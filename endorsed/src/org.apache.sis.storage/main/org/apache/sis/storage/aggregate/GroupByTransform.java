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
package org.apache.sis.storage.aggregate;

import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.text.NumberFormat;
import java.text.FieldPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A container for a list of slices grouped by their "grid to CRS" transform, ignoring integer translations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GroupByTransform extends Group<GridSlice> {
    /**
     * Geometry of the grid coverage or resource. This is copied from the first
     * {@link GridSlice#geometry} found in iteration order for this group.
     * It may be a somewhat random grid geometry, unless {@link GridSlice}
     * instances are sorted before processing.
     */
    private final GridGeometry geometry;

    /**
     * Value of {@code geometry.getGridToCRS(GridSlice.CELL_ANCHOR)}.
     * All {@linkplain #members} of this group use this transform,
     * possibly with integer differences in translation terms.
     */
    private final MathTransform gridToCRS;

    /**
     * Algorithm to apply when more than one grid coverage can be found at the same grid index.
     * This is set at construction time and usually keep the same value after that point,
     * unless {@link CoverageAggregator#setMergeStrategy(MergeStrategy)} is invoked again.
     */
    MergeStrategy strategy;

    /**
     * Whether the members of this group are the tiles of a mosaic.
     * This is {@code true} if all dimensions have tiles of the same size with an increment equal to that size.
     *
     * @see DimensionSelector#isMosaic
     */
    private boolean isMosaic;

    /**
     * Creates a new group of objects associated to the given transform.
     *
     * @param  geometry   geometry of the grid coverage or resource.
     * @param  gridToCRS  value of {@code geometry.getGridToCRS(GridSlice.CELL_ANCHOR)}.
     * @param  strategy   algorithm to apply when more than one grid coverage can be found at the same grid index.
     */
    GroupByTransform(final GridGeometry geometry, final MathTransform gridToCRS, final MergeStrategy strategy) {
        this.geometry  = geometry;
        this.gridToCRS = gridToCRS;
        this.strategy  = strategy;
        this.isMosaic  = true;
    }

    /**
     * Creates a name for this group for use in metadata (not a persistent identifier).
     * This is used as the resource name if an aggregated resource needs to be created.
     * Current implementation assumes that the main reason why many groups may exist is
     * that they differ by their resolution.
     */
    @Override
    final String createName(final Locale locale) {
        final Vocabulary    v = Vocabulary.forLocale(locale);
        final StringBuffer  b = new StringBuffer(v.getLabel(Vocabulary.Keys.Resolution));
        final NumberFormat  f = NumberFormat.getIntegerInstance(v.getLocale());
        final FieldPosition p = new FieldPosition(0);
        String separator = "";
        for (final double r : geometry.getResolution(true)) {     // Should not fail when `gridToCRS` exists.
            f.setMaximumFractionDigits(Math.max(DecimalFunctions.fractionDigitsForDelta(r / 100, false), 0));
            f.format(r, b.append(separator), p);
            separator = " × ";
        }
        return b.toString();
    }

    /**
     * Returns the conversion of pixel coordinates from this group to a slice if that conversion is linear.
     *
     * @param  crsToGrid  the "CRS to slice" transform.
     * @return the conversion as an affine transform, or {@code null} if null or if the conversion is non-linear.
     */
    final Matrix linearTransform(final MathTransform crsToGrid) {
        if (gridToCRS.getTargetDimensions() == crsToGrid.getSourceDimensions()) {
            return MathTransforms.getMatrix(MathTransforms.concatenate(gridToCRS, crsToGrid));
        }
        return null;
    }

    /**
     * Returns grid dimensions to aggregate, in order of recommendation.
     * Aggregations should use the first dimension in the returned list.
     * This method opportunistically updates {@link #isMosaic}.
     *
     * @todo A future version should add {@code findMosaicDimensions()}, which should be tested first.
     */
    private int[] findConcatenatedDimensions() {
        final DimensionSelector[] selects;
        synchronized (members) {                // Should no longer be needed at this step, but we are paranoiac.
            int sliceIndex = members.size();
            selects = new DimensionSelector[geometry.getDimension()];
            for (int dimension = selects.length; --dimension >= 0;) {
                selects[dimension] = new DimensionSelector(dimension, sliceIndex);
            }
            while (--sliceIndex >= 0) {
                members.get(sliceIndex).getGridExtent(sliceIndex, selects);
            }
        }
        /*
         * The above block collected information about all slices in this group.
         * The following code computes the increment along each grid dimension,
         * then finds which axis is the one on which the members are slices.
         */
        Arrays.stream(selects).parallel().forEach(DimensionSelector::finish);
        Arrays.sort(selects);       // Contains usually less than 5 elements.
        final var dimensions = new int[selects.length];
        int count = 0;
        for (int dimension = selects.length; --dimension >= 0;) {
            final DimensionSelector select = selects[dimension];
            if (select.isConstantPosition) break;
            dimensions[count++] = select.dimension;
            isMosaic &= select.isMosaic;
        }
        return ArraysExt.resize(dimensions, count);
    }

    /**
     * Sorts the slices in increasing order of low grid coordinates in the concatenated dimension.
     * Then builds a concatenated grid coverage resource capable to perform binary searches along that dimension.
     *
     * @param  parentListeners   listeners of the parent resource, or {@code null} if none.
     * @param  sampleDimensions  the sample dimensions of the resource to build.
     * @return the concatenated resource.
     */
    final Resource createResource(final StoreListeners parentListeners, final List<SampleDimension> ranges) {
        final int count = members.size();
        if (count == 1) {
            return members.get(0).resource;
        }
        final var slices = new GridCoverageResource[count];
        final String name = getName(parentListeners);
        final int[] dimensions = findConcatenatedDimensions();
        if (isMosaic) {
            if (strategy == null) {
                /*
                 * We can safely default to the "overlay" merge strategy.
                 * There is no ambiguity, because no tile should overlap.
                 * The "overlay" operation should be able to share tile
                 * references without copying pixel values in the common
                 * case where all tiles use the same `SampleModel`.
                 */
                strategy = MergeStrategy.opaqueOverlay(null);
            }
        } else if (dimensions.length == 0) {
            // Unable to group the slices in a multi-dimensional cube.
            for (int i=0; i<count; i++) slices[i] = members.get(i).resource;
            return new GroupAggregate(parentListeners, name, slices, ranges);
        }
        // The following constructor fills itself the `slices` array content.
        final var locator = new GridSliceLocator(members, dimensions[0], slices);
        final GridGeometry domain = locator.union(geometry, members, GridSlice::getGridExtent);
        return new ConcatenatedGridResource(name, parentListeners, domain, ranges, slices, locator, strategy);
    }
}
