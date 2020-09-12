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
package org.apache.sis.internal.storage;

import java.util.List;
import java.awt.image.RenderedImage;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.ArgumentChecks;


/**
 * A {@link org.apache.sis.storage.GridCoverageResource} in memory.
 * This resource wraps an arbitrary {@link GridCoverage} specified at construction time.
 * Metadata can be specified by overriding {@link #createMetadata(MetadataBuilder)}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class MemoryGridResource extends AbstractGridResource {
    /**
     * The grid coverage specified at construction time.
     */
    private final GridCoverage coverage;

    /**
     * Creates a new coverage stored in memory.
     *
     * @param  parent    listeners of the parent resource, or {@code null} if none.
     * @param  coverage  stored coverage retained as-is (not copied). Can not be null.
     */
    public MemoryGridResource(final StoreListeners parent, final GridCoverage coverage) {
        super(parent);
        ArgumentChecks.ensureNonNull("coverage", coverage);
        this.coverage = coverage;
    }

    /**
     * Returns information about the <cite>domain</cite> of wrapped grid coverage.
     *
     * @return extent of grid coordinates together with their mapping to "real world" coordinates.
     */
    @Override
    public GridGeometry getGridGeometry() {
        return coverage.getGridGeometry();
    }

    /**
     * Returns information about the <cite>range</cite> of wrapped grid coverage.
     *
     * @return ranges of sample values together with their mapping to "real values".
     */
    @Override
    public List<SampleDimension> getSampleDimensions() {
        return coverage.getSampleDimensions();
    }

    /**
     * Returns a subset of the wrapped grid coverage. If a non-null grid geometry is specified, then
     * this method tries to return a grid coverage matching the given grid geometry on a best-effort basis.
     * In current implementation this is either a {@link org.apache.sis.coverage.grid.GridCoverage2D} or
     * the original grid coverage.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and range.
     */
    @Override
    public GridCoverage read(GridGeometry domain, final int... range) {
        List<SampleDimension> bands = coverage.getSampleDimensions();
        final RangeArgument rangeIndices = validateRangeArgument(bands.size(), range);
        /*
         * The given `domain` may use arbitrary `gridToCRS` and `CRS` properties.
         * For this simple implementation we need the same `gridToCRS` and `CRS`
         * than the wrapped coverage; only domain `extent` is allowed to differ.
         * Subsampling is ignored for now because it is an expensive operation.
         * Clipping and range selection are light and do not copy any data.
         *
         * TODO: a future implementation may apply subsampling efficiently,
         *       by adjusting the pixel stride in SampleModel.
         */
        GridExtent intersection = null;
        final GridGeometry source = coverage.getGridGeometry();
        if (domain == null) {
            domain = source;
        } else {
            intersection = source.derive()
                    .rounding(GridRoundingMode.ENCLOSING)
                    .subgrid(domain).getIntersection();             // Take in account the change of CRS if needed.
            if (intersection.equals(source.getExtent())) {
                intersection = null;                                // Will request the whole image.
                domain = source;
            }
        }
        /*
         * Quick check before to invoke the potentially costly `coverage.render(…)` method.
         */
        final boolean sameBands = rangeIndices.isIdentity();
        if (sameBands && intersection == null) {
            return coverage;
        }
        /*
         * After `render(…)` execution, the (minX, minY) image coordinates are the differences between
         * the extent that we requested and the one that we got. If that differences is not zero, then
         * we need to translate the `GridExtent` in order to make it matches what we got.
         */
        RenderedImage data = coverage.render(intersection);
        if (intersection != null) {
            final int[]  sd      = intersection.getSubspaceDimensions(2);
            final int    dimX    = sd[0];
            final int    dimY    = sd[1];
            final long[] changes = new long[Math.max(dimX, dimY) + 1];
            changes[dimX] = data.getMinX();
            changes[dimY] = data.getMinY();
            intersection  = intersection.translate(changes);
            changes[dimX] = Math.subtractExact(data.getWidth(),  intersection.getSize(dimX));
            changes[dimY] = Math.subtractExact(data.getHeight(), intersection.getSize(dimY));
            intersection  = intersection.expand(null, changes);
            if (intersection.equals(source.getExtent())) {
                if (sameBands) {
                    return coverage;
                }
                domain = source;
            } else {
                domain = new GridGeometry(intersection, PixelInCell.CELL_CORNER,
                        source.getGridToCRS(PixelInCell.CELL_CORNER),
                        source.getCoordinateReferenceSystem());
            }
        }
        if (!sameBands) {
            data  = new ImageProcessor().selectBands(data, range);
            bands = UnmodifiableArrayList.wrap(rangeIndices.select(bands));
        }
        return new GridCoverageBuilder()
                .setValues(data)
                .setRanges(bands)
                .setDomain(domain).build();
    }
}
