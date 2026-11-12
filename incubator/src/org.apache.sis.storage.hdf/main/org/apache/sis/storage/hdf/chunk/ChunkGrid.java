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
package org.apache.sis.storage.hdf.chunk;

import java.util.Arrays;
import org.apache.sis.coverage.grid.GridExtent;

/**
 * Geometry of the chunk grid of a chunked dataset.
 * <p>
 * Chunk positions are expressed in <em>scaled</em> coordinates, that is the
 * element coordinates divided by the chunk dimensions. All the position
 * addressed indexes (single chunk, implicit, fixed array, extensible array)
 * locate a chunk from a linear index computed from those coordinates.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ChunkGrid {

    private final long[] datasetDimensions;
    private final long[] chunkDimensions;
    private final long[] chunkCounts;

    /**
     * Creates the chunk grid of a dataset.
     *
     * @param datasetDimensions size of the dataset in elements, per dimension
     * @param chunkDimensions size of a chunk in elements, per dimension. When longer
     *        than the dataset rank, the extra trailing values are ignored : version 1
     *        and 2 layout messages store the element size as an additional dimension.
     * @throws IllegalArgumentException if a chunk dimension is missing or not strictly positive.
     */
    public ChunkGrid(int[] datasetDimensions, int[] chunkDimensions) {
        final int rank = datasetDimensions.length;
        if (chunkDimensions.length < rank) {
            throw new IllegalArgumentException("Chunk has " + chunkDimensions.length
                    + " dimensions but dataset has " + rank);
        }
        this.datasetDimensions = new long[rank];
        this.chunkDimensions = new long[rank];
        this.chunkCounts = new long[rank];
        for (int i = 0; i < rank; i++) {
            this.datasetDimensions[i] = datasetDimensions[i];
            this.chunkDimensions[i] = chunkDimensions[i];
            if (chunkDimensions[i] <= 0) {
                throw new IllegalArgumentException("Chunk dimension " + i + " is " + chunkDimensions[i]);
            }
            //number of chunks needed to cover the dataset, edge chunks included
            this.chunkCounts[i] = (datasetDimensions[i] + chunkDimensions[i] - 1) / chunkDimensions[i];
        }
    }

    /**
     * Returns the number of dimensions of the grid, which is the dataset rank.
     *
     * @return the number of dimensions of the grid, which is the dataset rank
     */
    public int getDimension() {
        return chunkDimensions.length;
    }

    /**
     * Returns a copy of the chunk size in elements, per dimension.
     *
     * @return a copy of the chunk size in elements, per dimension
     */
    public long[] getChunkDimensions() {
        return chunkDimensions.clone();
    }

    /**
     * Returns a copy of the number of chunks covering the dataset, per dimension, partial edge chunks
     * included.
     *
     * @return a copy of the number of chunks covering the dataset, per dimension, partial edge chunks
     *         included
     */
    public long[] getChunkCounts() {
        return chunkCounts.clone();
    }

    /**
     * Returns the number of elements in a full chunk.
     *
     * @return the number of elements in a full chunk
     */
    public long getChunkElementCount() {
        long count = 1;
        for (long d : chunkDimensions) {
            count *= d;
        }
        return count;
    }

    /**
     * Returns the linear index of a chunk, row major, dimension 0 being the slowest changing one.
     * <p>
     * Note the count of dimension 0 is never used as a factor, which is what lets
     * this formula stay valid when that dimension is unlimited, as it is for a
     * dataset indexed by an extensible array.
     *
     * @param scaled chunk coordinates in the chunk grid
     * @return the linear index of a chunk, row major, dimension 0 being the slowest changing one
     */
    public long linearIndex(long[] scaled) {
        long index = 0;
        for (int i = 0; i < scaled.length; i++) {
            index = index * chunkCounts[i] + scaled[i];
        }
        return index;
    }

    /**
     * Returns the chunk coordinates in the chunk grid.
     * Inverse of {@link #linearIndex(long[])}.
     *
     * @param linearIndex linear index of a chunk
     * @return the chunk coordinates in the chunk grid
     */
    public long[] scaledOf(long linearIndex) {
        final long[] scaled = new long[chunkCounts.length];
        for (int i = scaled.length - 1; i >= 0; i--) {
            if (i == 0) {
                scaled[i] = linearIndex;
            } else {
                scaled[i] = linearIndex % chunkCounts[i];
                linearIndex /= chunkCounts[i];
            }
        }
        return scaled;
    }

    /**
     * Returns the scaled coordinates of the chunk containing the given element.
     *
     * @param element element coordinates in the dataset
     * @return the scaled coordinates of the chunk containing the given element
     */
    public long[] scaledOfElement(long[] element) {
        final long[] scaled = new long[chunkDimensions.length];
        for (int i = 0; i < scaled.length; i++) {
            scaled[i] = element[i] / chunkDimensions[i];
        }
        return scaled;
    }

    /**
     * Returns the extent covered by the given chunk, in element coordinates.
     * <p>
     * The extent is <em>not</em> clipped to the dataset : edge chunks are stored
     * at full size, and the caller intersects with the queried extent anyway.
     *
     * @param scaled chunk coordinates in the chunk grid
     * @return the extent covered by the given chunk, in element coordinates
     */
    public GridExtent extentOf(long[] scaled) {
        final long[] low = new long[scaled.length];
        final long[] high = new long[scaled.length];
        for (int i = 0; i < scaled.length; i++) {
            low[i] = scaled[i] * chunkDimensions[i];
            high[i] = low[i] + chunkDimensions[i];
        }
        return new GridExtent(null, low, high, false);
    }

    /**
     * Returns the scaled coordinates of every chunk intersecting the given extent, in row major order.
     *
     * @param extent extent in element coordinates
     * @return the scaled coordinates of every chunk intersecting the given extent, in row major order
     */
    public long[][] intersecting(GridExtent extent) {
        final int rank = chunkDimensions.length;
        final long[] first = new long[rank];
        final long[] last = new long[rank];
        long total = 1;
        for (int i = 0; i < rank; i++) {
            first[i] = extent.getLow(i) / chunkDimensions[i];
            last[i] = extent.getHigh(i) / chunkDimensions[i];
            total *= (last[i] - first[i] + 1);
        }
        final long[][] all = new long[Math.toIntExact(total)][];
        final long[] current = first.clone();
        for (int n = 0; n < all.length; n++) {
            all[n] = current.clone();
            //increment the fastest changing dimension first
            for (int i = rank - 1; i >= 0; i--) {
                if (++current[i] <= last[i]) break;
                current[i] = first[i];
            }
        }
        return all;
    }

    @Override
    public String toString() {
        return "ChunkGrid{dataset:" + Arrays.toString(datasetDimensions)
                + ",chunk:" + Arrays.toString(chunkDimensions)
                + ",counts:" + Arrays.toString(chunkCounts) + "}";
    }
}
