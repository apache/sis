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
package org.apache.sis.map.service.common;

import java.awt.image.BufferedImage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link HillShader}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class HillShaderTest {
    /**
     * Length of one degree of longitude at the equator on the WGS84 ellipsoid, in metres.
     */
    private static final double DEGREE = 111319.49079327357;

    /**
     * Creates a new test case.
     */
    public HillShaderTest() {
    }

    /**
     * Returns the elevations of a plane of 5×5 cells with the given slopes, in metres per cell.
     */
    private static double[] plane(final double east, final double south) {
        final double[] z = new double[25];
        for (int i = 0; i < z.length; i++) {
            z[i] = 1000 + (i % 5) * east + (i / 5) * south;
        }
        return z;
    }

    /**
     * Tests the shade of flat and sloping terrains with the default sun in the north-west.
     */
    @Test
    public void testShade() {
        final double flat = Math.sin(Math.toRadians(HillShader.DEFAULT_ELEVATION));
        assertEquals(flat, new HillShader(plane(0, 0), 5, 5).shade(12), 1E-12, "Flat terrain.");

        final double facingWest = new HillShader(plane(1, 0), 5, 5).shade(12);
        final double facingEast = new HillShader(plane(-1, 0), 5, 5).shade(12);
        assertEquals((flat + 0.5) / Math.sqrt(2), facingWest, 1E-12, "Slope of 1 facing west.");
        assertEquals((flat - 0.5) / Math.sqrt(2), facingEast, 1E-12, "Slope of 1 facing east.");

        assertEquals(facingWest, new HillShader(plane(1, 0), 5, 5).shade(0),  1E-12, "Corner cell of a plane.");
        assertEquals(new HillShader(plane(1, 2), 5, 5).shade(12), new HillShader(plane(1, 2), 5, 5).shade(24), 1E-12,
                     "Corner cell of an oblique plane.");
        assertEquals(flat, new HillShader(plane(1, 0), 5, 5).setFactor(0).shade(12), 1E-12, "No relief exaggeration.");
        assertEquals(facingEast, new HillShader(plane(1, 0), 5, 5).setSun(45, 45).shade(12), 1E-12, "Sun in the north-east.");
        assertEquals(new HillShader(plane(2, 0), 5, 5).shade(12), new HillShader(plane(1, 0), 5, 5).setFactor(2).shade(12),
                     1E-12, "The factor multiplies the slopes.");
    }

    /**
     * Tests the extrapolation of neighbours without elevation.
     */
    @Test
    public void testMissingElevations() {
        final double[] z = plane(1, 2);
        final double expected = new HillShader(z, 5, 5).shade(12);
        z[11] = Double.NaN;             // West neighbour of the center.
        z[6]  = Double.NaN;             // North-west neighbour of the center.
        assertEquals(expected, new HillShader(z, 5, 5).shade(12), 1E-12, "The plane shall be extrapolated.");
        assertTrue(Double.isNaN(new HillShader(z, 5, 5).shade(11)), "Cell without elevation.");
        assertThrows(IllegalArgumentException.class, () -> new HillShader(z, 4, 5));
    }

    /**
     * Tests the ground distances computed from a geographic grid.
     */
    @Test
    public void testGeographicGrid() {
        final GridGeometry grid = new GridGeometry(new GridExtent(3, 3),
                new Envelope2D(CommonCRS.WGS84.normalizedGeographic(), -1.5, -1.5, 3, 3), GridOrientation.REFLECTION_Y);
        final double[] z = new double[9];
        for (int i = 0; i < z.length; i++) {
            z[i] = (i % 3) * DEGREE;    // Slope of 1 eastward at the equator.
        }
        final double flat = Math.sin(Math.toRadians(HillShader.DEFAULT_ELEVATION));
        assertEquals((flat + 0.5) / Math.sqrt(2), new HillShader(z, 3, 3).setGrid(grid).shade(4), 1E-6);
    }

    /**
     * Tests the overlay image: transparent on flat terrain, black on shadows and white on lit slopes.
     */
    @Test
    public void testOverlay() {
        assertEquals(0, new HillShader(plane(0, 0), 5, 5).createOverlay().getRGB(2, 2) >>> 24, "Flat terrain is transparent.");

        final BufferedImage lit = new HillShader(plane(1, 0), 5, 5).createOverlay();
        assertEquals(0xFFFFFF, lit.getRGB(2, 2) & 0xFFFFFF, "White highlight.");
        assertTrue((lit.getRGB(2, 2) >>> 24) > 0);

        final BufferedImage shadowed = new HillShader(plane(-1, 0), 5, 5).createOverlay();
        assertEquals(0, shadowed.getRGB(2, 2) & 0xFFFFFF, "Black shadow.");
        assertTrue((shadowed.getRGB(2, 2) >>> 24) > 0);
    }
}
