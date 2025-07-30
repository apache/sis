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
package org.apache.sis.storage.base;

import java.awt.image.BufferedImage;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.privy.AffineTransform2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;


/**
 * Tests {@link MemoryGridResource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MemoryGridResourceTest extends TestCase {
    /**
     * Arbitrary size for the grid to test.
     */
    private static final int WIDTH = 31, HEIGHT = 23;

    /**
     * The coordinate reference system used by the tests.
     */
    private final CoordinateReferenceSystem crs;

    /**
     * The conversion from pixel coordinates to geographic coordinates for the coverage in this test.
     */
    private final AffineTransform2D gridToCRS;

    /**
     * The resource to be tested by each test method.
     */
    private final MemoryGridResource resource;

    /**
     * Creates a new test case.
     */
    public MemoryGridResourceTest() {
        crs = HardCodedCRS.WGS84;
        gridToCRS = new AffineTransform2D(2, 0, 0, 3, 0, 0);
        final GridGeometry grid = new GridGeometry(new GridExtent(WIDTH, HEIGHT), PixelInCell.CELL_CENTER, gridToCRS, crs);
        final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
        resource = new MemoryGridResource(null, null, new GridCoverage2D(grid, null, image), null);
    }

    /**
     * Creates an arbitrary grid geometry included inside the {@linkplain #resource} extent.
     */
    private GridGeometry createSubGrid() {
        final GridExtent extent = new GridExtent(null,
                new long[] {7, 4},
                new long[] {WIDTH - 9, HEIGHT - 11}, true);

        return new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS, crs);
    }

    /**
     * Tests {@link MemoryGridResource#read(GridGeometry, int...)}.
     */
    @Test
    public void testRead() {
        final GridGeometry request  = createSubGrid();
        final GridCoverage coverage = resource.read(request);
        /*
         * Note: following lines work only with JDK 16 or above.
         * https://bugs.openjdk.java.net/browse/JDK-8166038
         */
        assertEqualsIgnoreMetadata(request, coverage.getGridGeometry());
        assertInstanceOf(BufferedImage.class, coverage.render(null));
    }
}
