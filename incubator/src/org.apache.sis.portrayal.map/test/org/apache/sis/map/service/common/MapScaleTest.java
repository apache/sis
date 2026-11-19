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

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link MapScale} and the scale denominators of {@link Scene2D}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MapScaleTest {
    /**
     * Metres per degree on the equator of the WGS84 ellipsoid.
     */
    private static final double METRES_PER_DEGREE = 6378137.0 * 2.0 * Math.PI / 360.0;

    /**
     * Nominal scale denominator of a grid of one degree per pixel, at 90 pixels per inch.
     */
    private static final double NOMINAL_ONE_DEGREE = METRES_PER_DEGREE * 90 / 0.0254;

    /**
     * The geographic CRS of the grids.
     */
    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();

    /**
     * Creates a new test case.
     */
    public MapScaleTest() {
    }

    /**
     * Returns a grid of 140×140 pixels of one degree, centered on the given latitude.
     */
    private static GridGeometry degrees(final double latitude) {
        return new GridGeometry(new GridExtent(140, 140),
                new Envelope2D(CRS84, -70, latitude - 70, 140, 140), GridOrientation.REFLECTION_Y);
    }

    /**
     * Returns a scene on the given grid.
     */
    private static Scene2D scene(final GridGeometry grid) {
        return new Scene2D(grid, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics());
    }

    /**
     * Tests a grid of one degree per pixel on the equator, where both formulas agree within 1%.
     */
    @Test
    public void testEquator() throws Exception {
        final MapScale scale = new MapScale(degrees(0));
        assertEquals(METRES_PER_DEGREE, scale.metresPerPixel(), 1);
        assertEquals(NOMINAL_ONE_DEGREE, scale.nominalDenominator(), 1);

        final Scene2D scene = scene(degrees(0));
        assertEquals(METRES_PER_DEGREE / 0.00028, scene.getScaleDenominator(), 1E3);
        assertEquals(scale.metresPerPixel() / MapScale.STANDARD_PIXEL_SIZE, scene.getScaleDenominator(), 1E-6);
        assertEquals(scale.nominalDenominator(), scene.getNominalScaleDenominator(), 1E-6);
    }

    /**
     * Tests a grid centered at 60° of latitude: the ground scale is about half the nominal scale.
     */
    @Test
    public void testLatitude() throws Exception {
        final MapScale scale = new MapScale(degrees(60));
        assertEquals(NOMINAL_ONE_DEGREE, scale.nominalDenominator(), 1, "Independent of the latitude.");
        assertEquals(0.5, scene(degrees(60)).getScaleDenominator() / (METRES_PER_DEGREE / 0.00028), 0.01, "Parallel at 60°.");
    }

    /**
     * Tests a projected grid of 10 metres per pixel.
     */
    @Test
    public void testProjected() throws Exception {
        final GridGeometry grid = new GridGeometry(new GridExtent(100, 100),
                new Envelope2D(CommonCRS.WGS84.universal(0, 3), 500000, 0, 1000, 1000), GridOrientation.REFLECTION_Y);
        final MapScale scale = new MapScale(grid);
        assertEquals(10, scale.metresPerPixel(), 1E-9);
        assertEquals(10 / 0.00028, scene(grid).getScaleDenominator(), 1E-6);
        assertEquals(10 * 90 / 0.0254, scale.nominalDenominator(), 1E-6);
    }

    /**
     * Tests a rotated grid of one degree per pixel: the nominal formula uses the scale of the transform.
     */
    @Test
    public void testRotated() throws Exception {
        final AffineTransform gridToCRS = AffineTransform.getRotateInstance(Math.toRadians(30));
        gridToCRS.scale(1, -1);
        final GridGeometry grid = new GridGeometry(new GridExtent(140, 140), PixelInCell.CELL_CENTER,
                MathTransforms.linear(AffineTransforms2D.toMatrix(gridToCRS)), CRS84);
        assertEquals(NOMINAL_ONE_DEGREE, new MapScale(grid).nominalDenominator(), 1);
    }
}
