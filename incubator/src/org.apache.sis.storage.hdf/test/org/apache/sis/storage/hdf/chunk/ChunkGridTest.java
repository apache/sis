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

import org.apache.sis.coverage.grid.GridExtent;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests of the chunk grid arithmetic shared by every position addressed index.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ChunkGridTest {

    @Test
    public void testChunkCounts() {
        //16 divides by 4, 17 does not and needs one more chunk
        final ChunkGrid grid = new ChunkGrid(new int[]{16, 17}, new int[]{4, 8});
        assertArrayEquals(new long[]{4, 3}, grid.getChunkCounts());
    }

    /**
     * Dimension 0 is the slowest changing one and its count is never used as a
     * factor, which is what keeps the formula valid when it is unlimited.
     */
    @Test
    public void testLinearIndex() {
        final ChunkGrid grid = new ChunkGrid(new int[]{16, 16}, new int[]{4, 4});
        assertEquals(0, grid.linearIndex(new long[]{0, 0}));
        assertEquals(1, grid.linearIndex(new long[]{0, 1}));
        assertEquals(3, grid.linearIndex(new long[]{0, 3}));
        assertEquals(4, grid.linearIndex(new long[]{1, 0}));
        assertEquals(15, grid.linearIndex(new long[]{3, 3}));
    }

    /**
     * The count of the unlimited dimension must not bound the linear index.
     */
    @Test
    public void testLinearIndexOnUnlimitedFirstDimension() {
        final ChunkGrid grid = new ChunkGrid(new int[]{8, 16}, new int[]{4, 4});
        assertArrayEquals(new long[]{2, 4}, grid.getChunkCounts());
        //a chunk beyond the current extent of dimension 0 still gets a sane index
        assertEquals(400, grid.linearIndex(new long[]{100, 0}));
        assertEquals(403, grid.linearIndex(new long[]{100, 3}));
    }

    @Test
    public void testScaledOfRoundTrip() {
        final ChunkGrid grid = new ChunkGrid(new int[]{16, 12, 8}, new int[]{4, 3, 2});
        for (long i = 0; i < 4 * 4 * 4; i++) {
            assertEquals(i, grid.linearIndex(grid.scaledOf(i)));
        }
    }

    @Test
    public void testScaledOfElement() {
        final ChunkGrid grid = new ChunkGrid(new int[]{16, 16}, new int[]{4, 8});
        assertArrayEquals(new long[]{0, 0}, grid.scaledOfElement(new long[]{3, 7}));
        assertArrayEquals(new long[]{1, 1}, grid.scaledOfElement(new long[]{4, 8}));
        assertArrayEquals(new long[]{3, 1}, grid.scaledOfElement(new long[]{15, 15}));
    }

    /**
     * Edge chunks are stored at full size, so the extent is not clipped to the
     * dataset.
     */
    @Test
    public void testExtentOfIsNotClipped() {
        final ChunkGrid grid = new ChunkGrid(new int[]{10}, new int[]{4});
        final GridExtent extent = grid.extentOf(new long[]{2});
        assertEquals(8, extent.getLow(0));
        assertEquals(11, extent.getHigh(0));
    }

    @Test
    public void testIntersecting() {
        final ChunkGrid grid = new ChunkGrid(new int[]{16, 16}, new int[]{4, 4});
        //a query inside one chunk touches only that chunk
        long[][] touched = grid.intersecting(new GridExtent(null, new long[]{1, 1}, new long[]{3, 3}, true));
        assertEquals(1, touched.length);
        assertArrayEquals(new long[]{0, 0}, touched[0]);

        //a query straddling a boundary touches four chunks
        touched = grid.intersecting(new GridExtent(null, new long[]{3, 3}, new long[]{4, 4}, true));
        assertEquals(4, touched.length);

        //the whole dataset touches every chunk
        touched = grid.intersecting(new GridExtent(null, new long[]{0, 0}, new long[]{15, 15}, true));
        assertEquals(16, touched.length);
    }

    /**
     * A chunk larger than the dataset yields a single chunk, which is how compact
     * and contiguous layouts are described.
     */
    @Test
    public void testSingleChunkGrid() {
        final ChunkGrid grid = new ChunkGrid(new int[]{20, 20}, new int[]{20, 20});
        assertArrayEquals(new long[]{1, 1}, grid.getChunkCounts());
        final long[][] touched = grid.intersecting(new GridExtent(null, new long[]{5, 5}, new long[]{12, 12}, true));
        assertEquals(1, touched.length);
        assertArrayEquals(new long[]{0, 0}, touched[0]);
        assertTrue(grid.getChunkElementCount() == 400);
    }
}
