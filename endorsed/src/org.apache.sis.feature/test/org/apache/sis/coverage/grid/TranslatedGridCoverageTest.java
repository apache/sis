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

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.image.internal.shared.RasterFactory;
import org.apache.sis.geometry.DirectPosition2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Tests {@link TranslatedGridCoverage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TranslatedGridCoverageTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TranslatedGridCoverageTest() {
    }

    /**
     * Creates a test coverage with grid coordinates starting at (-20, -10).
     * Envelope is BOX(-80 -20, -72 -16).
     */
    private static GridCoverage createCoverage() {
        final int imageSize = 2;
        final GridExtent     extent = new GridExtent(imageSize, imageSize).translate(-20, -10);
        final GridGeometry   domain = new GridGeometry(extent, PixelInCell.CELL_CORNER, MathTransforms.scale(4, 2), HardCodedCRS.WGS84);
        final BufferedImage  image  = RasterFactory.createGrayScaleImage(DataBuffer.TYPE_BYTE, imageSize, imageSize, 1, 0, 10, 24);
        final WritableRaster raster = image.getRaster();
        raster.setSample(0, 0, 0, 10);
        raster.setSample(1, 0, 0, 16);
        raster.setSample(0, 1, 0, 20);
        raster.setSample(1, 1, 0, 24);
        return new GridCoverageBuilder().setDomain(domain).setValues(image).build();
    }

    /**
     * Verifies that the given two-dimensional extent has the given low coordinates.
     */
    private static void assertExtentStarts(final GridExtent extent, final long low0, final long low1) {
        assertEquals(2, extent.getDimension());
        assertEquals(low0, extent.getLow(0));
        assertEquals(low1, extent.getLow(1));
    }

    /**
     * Tests using {@link GridCoverageProcessor}.
     */
    @Test
    public void testUsingProcessor() {
        final var processor = new GridCoverageProcessor();
        final GridCoverage source = createCoverage();
        final GridCoverage target = processor.shiftGrid(source, 30, -5);
        assertExtentStarts(source.getGridGeometry().getExtent(), -20, -10);
        assertExtentStarts(target.getGridGeometry().getExtent(),  10, -15);
        /*
         * The result for identical "real world" coordinates should be the same for both coverages.
         */
        final var p = new DirectPosition2D(HardCodedCRS.WGS84, -75, -18);
        assertArrayEquals(source.evaluator().apply(p),
                          target.evaluator().apply(p));
    }
}
