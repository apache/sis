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
import java.util.Arrays;
import java.awt.image.WritableRaster;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.collection.WeakValueHashMap;


/**
 * Base class of grid coverage resource storing data in tiles.
 * Word "tile" is used for simplicity but can be understood as "chunk"
 * in a <var>n</var>-dimensional generalization.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class TiledGridResource extends AbstractGridResource {
    /**
     * All tiles loaded by any {@link TiledGridCoverage} created from this resource.
     * Keys are tile indices in a row-major array of tiles.
     * For each value, the {@link WritableRaster#getMinX()} and {@code minY} values
     * can be anything, depending which {@link TiledGridResource} was first to load the tile.
     *
     * @see TiledGridCoverage#rasters
     * @see TiledGridCoverage.AOI#getCachedTile()
     */
    private final WeakValueHashMap<Integer, WritableRaster> rasters;

    /**
     * Creates a new resource.
     *
     * @param  parent  listeners of the parent resource, or {@code null} if none.
     */
    protected TiledGridResource(final StoreListeners parent) {
        super(parent);
        rasters = new WeakValueHashMap<>(Integer.class);
    }

    /**
     * Returns the size of tiles in this resource.
     * The length of the returned array is the number of dimensions.
     *
     * @return the size of tiles in this resource.
     */
    protected abstract int[] getTileSize();

    /**
     * Parameters that describe the resource subset to be accepted by the {@link TiledGridCoverage} constructor.
     * This is a temporary class used only for transferring information from {@link TiledGridResource}.
     * This class does not perform I/O operations.
     */
    public static final class Subset {
        /**
         * The full size of the coverage in the enclosing {@link TiledGridResource}.
         */
        final GridExtent sourceExtent;

        /**
         * The area to read in unit of the full coverage (without subsampling).
         * This is the intersection between user-specified domain and enclosing
         * {@link TiledGridResource} domain, expanded to an integer number of tiles.
         */
        final GridExtent readExtent;

        /**
         * The sub-region extent, CRS and conversion from cell indices to CRS.
         * This is the domain of the grid coverage to create.
         */
        final GridGeometry domain;

        /**
         * Sample dimensions for each image band.
         * This is the range of the grid coverage to create.
         */
        final List<? extends SampleDimension> ranges;

        /**
         * Indices of {@link TiledGridResource} bands which have been retained
         * for inclusion in the {@link TiledGridCoverage} to construct.
         * This is {@code null} if all bands shall be included.
         */
        final int[] selectedBands;

        /**
         * Coordinate conversion from subsampled grid to the grid at full resolution.
         * This array contains the factors by which to divide {@link TiledGridResource}
         * cell coordinates in order to obtain {@link TiledGridCoverage} cell coordinates.
         */
        final int[] subsampling;

        /**
         * Remainder of the divisions of {@link TiledGridResource} cell coordinates by subsampling factors.
         */
        final int[] subsamplingOffsets;

        /**
         * Size of tiles in each dimension.
         */
        final int[] tileSize;

        /**
         * Cache to use for tiles loaded by the {@link TiledGridCoverage}.
         * It is a reference to {@link TiledGridResource#rasters} if shareable.
         */
        final WeakValueHashMap<Integer, WritableRaster> cache;

        /**
         * Creates parameters for the given domain and range.
         *
         * @param  caller  the resource for which a subset is created.
         * @param  domain  the domain argument specified by user in a call to {@code GridCoverageResource.read(…)}.
         * @param  range   the range argument specified by user in a call to {@code GridCoverageResource.read(…)}.
         * @throws ArithmeticException if pixel indices exceed 64 bits integer capacity.
         * @throws DataStoreException if a call to {@link TiledGridResource} method failed.
         */
        public Subset(final TiledGridResource caller, GridGeometry domain, final int[] range) throws DataStoreException {
            List<SampleDimension> bands        = caller.getSampleDimensions();
            final RangeArgument   rangeIndices = caller.validateRangeArgument(bands.size(), range);
            final GridGeometry    gridGeometry = caller.getGridGeometry();
            sourceExtent = gridGeometry.getExtent();
            tileSize = caller.getTileSize();
            boolean sharedCache = true;
            if (domain == null) {
                domain             = gridGeometry;
                readExtent         = sourceExtent;
                subsamplingOffsets = new int[gridGeometry.getDimension()];
                subsampling        = new int[subsamplingOffsets.length];
                Arrays.fill(subsampling, 1);
            } else {
                /*
                 * If an area of interest has been specified, we may need to expand it to an integer amount of tiles.
                 * But we do not need to do that if the image is untiled; it is okay to read only a sub-region of the
                 * single tile. We disable the "integer amount of tiles" restriction by setting the tile size to 1.
                 * Note that it is possible to disable this restriction in a single dimension, typically the X one
                 * when reading a TIFF image using strips instead of tiles.
                 */
                int tileWidth  = tileSize[0];
                int tileHeight = tileSize[1];
                if (tileWidth  >= sourceExtent.getSize(0)) {tileWidth  = 1; sharedCache = false;}
                if (tileHeight >= sourceExtent.getSize(1)) {tileHeight = 1; sharedCache = false;}
                final GridDerivation target = gridGeometry.derive().chunkSize(tileWidth, tileHeight)
                                              .rounding(GridRoundingMode.ENCLOSING).subgrid(domain);
                domain             = target.build();
                readExtent         = target.getIntersection();
                subsampling        = target.getSubsampling();
                subsamplingOffsets = target.getSubsamplingOffsets();
                if (sharedCache) {
                    for (final int s : subsampling) {
                        if (s != 1) {
                            sharedCache = false;
                            break;
                        }
                    }
                }
            }
            int[] selectedBands = null;
            if (!rangeIndices.isIdentity()) {
                bands = Arrays.asList(rangeIndices.select(bands));
                selectedBands = new int[rangeIndices.getNumBands()];
                for (int i=0; i<selectedBands.length; i++) {
                    selectedBands[rangeIndices.getTargetIndex(i)] = rangeIndices.getSourceIndex(i);
                }
            }
            this.domain        = domain;
            this.ranges        = bands;
            this.selectedBands = selectedBands;
            /*
             * All `TiledGridCoverage` instances can share the same cache if they read all tiles fully.
             * If they read only sub-regions or apply subsampling, then they will need their own cache.
             */
            cache = sharedCache ? caller.rasters : new WeakValueHashMap<>(Integer.class);
        }
    }
}
