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
package org.apache.sis.geometries.operation.triangulate;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link HoleToExterior}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class HoleToExteriorTest {

    /**
     * Name of the attribute holding {@code x + 10*y} at each position, for checking that it is kept.
     */
    private static final String ATT_TEST = "TEST";

    /**
     * Creates a ring of the given positions in the given reference system, with a {@value #ATT_TEST} attribute.
     */
    private static LinearRing ring(final CoordinateReferenceSystem crs, final double... coordinates) {
        final int dimension = crs.getCoordinateSystem().getDimension();
        final double[] values = new double[coordinates.length / dimension];
        for (int i = 0; i < values.length; i++) {
            values[i] = coordinates[i * dimension] + 10 * coordinates[i * dimension + 1];
        }
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(crs, coordinates));
        attributes.put(ATT_TEST, NDArrays.of(1, values));
        return GeometryFactory.DEFAULT.createLinearRing(GeometryFactory.DEFAULT.createDataPoints(attributes));
    }

    /**
     * Returns the area enclosed by the given ring in the plane of the two given axes.
     * A cut followed in both directions does not change the area.
     */
    private static double area(final DataPoints ring, final int h0, final int h1) {
        double area = 0;
        for (int i = 1; i < ring.size(); i++) {
            final double[] p = ring.getPosition(i-1).toArrayDouble();
            final double[] q = ring.getPosition(i).toArrayDouble();
            area += p[h0] * q[h1] - q[h0] * p[h1];
        }
        return Math.abs(area) / 2;
    }

    /**
     * Verifies that the {@value #ATT_TEST} attribute of each position of the given ring is {@code x + 10*y}.
     */
    private static void assertAttribute(final DataPoints ring) {
        for (int i = 0; i < ring.size(); i++) {
            final double[] p = ring.getPosition(i).toArrayDouble();
            assertEquals(p[0] + 10 * p[1], ring.getAttribute(i, ATT_TEST).get(0), 1E-9);
        }
    }

    /**
     * A polygon without hole is returned unchanged.
     */
    @Test
    public void testWithoutHole() {
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(
                ring(CommonCRS.WGS84.normalizedGeographic(), 0,0, 2,0, 2,2, 0,2, 0,0), null);
        assertSame(polygon, HoleToExterior.apply(polygon));
    }

    /**
     * Verifies that each position of the given ring is a position of the given polygon.
     */
    private static void assertNoAddedPosition(final Polygon polygon, final DataPoints ring) {
        final DataPoints positions = polygon.getDataPoints();
        next: for (int i = 0; i < ring.size(); i++) {
            final double[] p = ring.getPosition(i).toArrayDouble();
            for (int j = 0; j < positions.size(); j++) {
                if (Arrays.equals(p, positions.getPosition(j).toArrayDouble())) {
                    continue next;
                }
            }
            fail("Added position: " + Arrays.toString(p));
        }
    }

    /**
     * The holes are joined to the exterior ring, which keeps the area of the polygon and the attributes
     * of its positions. The cuts join vertices of the polygon, so no position is added. The second hole
     * turns in the same direction as the exterior ring, so it shall be reversed.
     */
    @Test
    public void testWithHoles() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(ring(crs, 0,0, 10,0, 10,10, 0,10, 0,0), List.of(
                ring(crs, 2,3, 2,5, 4,5, 4,3, 2,3),
                ring(crs, 6,6, 8,6, 8,8, 6,8, 6,6)));
        final Polygon joined = HoleToExterior.apply(polygon);
        assertTrue(joined.getInteriorRings().isEmpty());
        final DataPoints ring = joined.getExteriorRing().getDataPoints();
        assertEquals(5 + 2 * (5 + 1), ring.size(), "Each hole adds its positions, its first position again and the ring vertex again.");
        assertArrayEquals(ring.getPosition(0).toArrayDouble(), ring.getPosition(ring.size() - 1).toArrayDouble(), "The ring shall stay closed.");
        assertEquals(100 - 4 - 4, area(ring, 0, 1), 1E-9);
        assertNoAddedPosition(polygon, ring);
        assertAttribute(ring);
    }

    /**
     * A reflex vertex of the exterior ring hides the end of the segment reached by the ray from the hole,
     * so the cut shall go to that reflex vertex.
     */
    @Test
    public void testHiddenVertex() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
        //the ray from the hole vertex (4,5) reaches the segment from (10,0) to (10,10), but the spike
        //of the exterior ring up to (7,4) hides the vertex (10,0) from the hole.
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(
                ring(crs, 0,0, 7,0, 7,4, 8,0, 10,0, 10,10, 0,10, 0,0),
                List.of(ring(crs, 2,4, 2,6, 4,5, 2,4)));
        final DataPoints ring = HoleToExterior.apply(polygon).getExteriorRing().getDataPoints();
        assertEquals(8 + 4 + 1, ring.size(), "The hole adds its positions, its first position again and the ring vertex again.");
        assertEquals(area(polygon.getExteriorRing().getDataPoints(), 0, 1) - 2, area(ring, 0, 1), 1E-9);
        assertNoAddedPosition(polygon, ring);
        boolean found = false;
        for (int i = 1; i < ring.size(); i++) {
            final double[] p = ring.getPosition(i - 1).toArrayDouble();
            final double[] q = ring.getPosition(i).toArrayDouble();
            if (Arrays.equals(p, new double[] {7, 4}) && Arrays.equals(q, new double[] {4, 5})) {
                found = true;
            }
        }
        assertTrue(found, "The cut shall start from the reflex vertex (7,4).");
    }

    /**
     * The hole of a vertical polygon is joined in the plane of the polygon.
     */
    @Test
    public void testVerticalPolygon() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(ring(crs, 0,0,0, 6,0,0, 6,0,4, 0,0,4, 0,0,0),
                List.of(ring(crs, 2,0,1, 2,0,3, 4,0,3, 4,0,1, 2,0,1)));
        final DataPoints ring = HoleToExterior.apply(polygon).getExteriorRing().getDataPoints();
        assertEquals(24 - 4, area(ring, 0, 2), 1E-9);
        for (int i = 0; i < ring.size(); i++) {
            assertEquals(0, ring.getPosition(i).get(1), "y shall stay in the wall plane.");
        }
        assertAttribute(ring);
    }
}
