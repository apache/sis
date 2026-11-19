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
import java.util.List;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.referencing.CommonCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link FeatureGeometry} and {@link DisplayGeometry}.
 * The scene is 100×100 pixels over [-50 … 50]° in CRS:84, so that (0,0) is at the display center.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class FeatureGeometryTest {
    /**
     * The CRS of the scene and of the geometries.
     */
    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();

    /**
     * The scene where the geometries are projected.
     */
    private final Scene2D scene;

    /**
     * Creates a new test case.
     *
     * @throws Exception if the scene cannot be created.
     */
    public FeatureGeometryTest() throws Exception {
        final GridGeometry grid = new GridGeometry(new GridExtent(100, 100),
                new Envelope2D(CRS84, -50, -50, 100, 100), GridOrientation.REFLECTION_Y);
        scene = new Scene2D(grid, new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics());
    }

    /**
     * Tests a polygon of Apache SIS: dimension, projection and centroid.
     */
    @Test
    public void testSISPolygon() {
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        final FeatureGeometry geometry = new FeatureGeometry(factory.createPolygon(factory.createLinearRing(
                factory.createDataPoints(CRS84, -10, -10, 10, -10, 10, 10, -10, 10, -10, -10)), List.of()));
        assertEquals(2, geometry.getDimension());
        assertNotNull(geometry.toGeometry());
        final DisplayGeometry display = geometry.project(scene);
        assertEquals(2, display.getDimension());
        assertTrue(display.getPolygons().contains(50, 50));
        assertFalse(display.getPolygons().contains(30, 50));
        final GraphicPlacement centroid = display.getMarkerPositions().get(0);
        assertEquals(50, centroid.x(), 1E-9);
        assertEquals(50, centroid.y(), 1E-9);
    }

    /**
     * Tests a line of JTS, without CRS: the CRS of the scene is assumed.
     */
    @Test
    public void testJTSLine() {
        final org.locationtech.jts.geom.LineString jts = new org.locationtech.jts.geom.GeometryFactory().createLineString(
                new Coordinate[] {new Coordinate(-20, 0), new Coordinate(20, 0)});
        final FeatureGeometry geometry = new FeatureGeometry(jts);
        assertEquals(1, geometry.getDimension());
        final LineString line = assertInstanceOf(LineString.class, geometry.toGeometry());
        assertEquals(2, line.getNumPoints());
        final DisplayGeometry display = geometry.project(scene);
        assertEquals(1, display.getDimension());
        assertEquals(40, display.getOutline().getBounds2D().getWidth(), 1E-9);
        assertEquals(2, display.getMarkerPositions().size(), "One marker per vertex.");
    }

    /**
     * Tests a point with a missing height in a three-dimensional CRS: the height is replaced by zero.
     */
    @Test
    public void testMissingHeight() {
        final FeatureGeometry geometry = new FeatureGeometry(GeometryFactory.DEFAULT.createPoint(
                CommonCRS.WGS84.geographic3D(), 0, 0, Double.NaN));
        final DisplayGeometry display = geometry.project(scene);
        assertNotNull(display);
        final GraphicPlacement point = display.getMarkerPositions().get(0);
        assertEquals(50, point.x(), 1E-9);
        assertEquals(50, point.y(), 1E-9);
    }

    /**
     * Tests values which are not supported geometries, and geometries outside the scene.
     */
    @Test
    public void testUnsupported() {
        for (final Object value : new Object[] {null, "text", new Envelope(0, 1, 0, 1)}) {
            final FeatureGeometry geometry = new FeatureGeometry(value);
            assertEquals(-1, geometry.getDimension());
            assertNull(geometry.toGeometry());
            assertNull(geometry.project(scene));
        }
        final FeatureGeometry far = new FeatureGeometry(GeometryFactory.DEFAULT.createPoint(CRS84, 170, 80));
        assertEquals(0, far.getDimension());
        assertNull(far.project(scene), "Outside the scene.");
    }
}
