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
package org.apache.sis.geometries.surface;

import javax.measure.Quantity;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link Polygon}.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class PolygonTest extends GeometryTest {
    /**
     * The ring of a square of 10 × 10, therefore of area 100 and of perimeter 40,
     * with its centroid at (5 5).
     */
    private static final double[] SQUARE = {0,0, 10,0, 10,10, 0,10, 0,0};

    /**
     * A square hole of 2 × 2 at the center of {@link #SQUARE}, therefore of area 4
     * and of perimeter 8.
     */
    private static final double[] HOLE = {4,4, 6,4, 6,6, 4,6, 4,4};

    /**
     * A ring crossing itself at (5 5), which makes the surface it bounds invalid.
     */
    private static final double[] BOWTIE = {0,0, 10,10, 10,0, 0,10, 0,0};

    protected PolygonTest() {
    }

    /**
     * Creates a surface bounded by the given rings in the given coordinate reference system.
     * The caller is responsible for repeating the first position of each ring at its end.
     *
     * @param  crs       the coordinate reference system of the surface to create, not null.
     * @param  exterior  the coordinates of the exterior ring, in the axis order of the given system.
     * @param  holes     the coordinates of the rings bounding the holes, if any.
     * @return a new surface bounded by the given rings, never null.
     */
    protected abstract Polygon createPolygon(CoordinateReferenceSystem crs, double[] exterior, double[]... holes);

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetAttributesType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetInterpolation() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetInteriorRings() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetExteriorRing() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumInteriorRing() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetInteriorRingN() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetSpanningSurface() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetTopologicDimension() {
    }

    /**
     * The area of a surface with a hole excludes that hole, and is measured in the units of the
     * coordinate system axes: stating it in square metres would claim a measurement on the
     * reference surface, which is not what is computed.
     */
    @Test
    public void testGetArea() {
        final Quantity<?> area = createPolygon(CRS_2D, SQUARE).getArea();
        assertEquals(100, area.getValue().doubleValue(), TOLERANCE, "A square of 10 × 10.");
        assertEquals(Units.DEGREE.multiply(Units.DEGREE), area.getUnit(),
                     "The area shall use the square of the unit of the axes.");
        assertEquals(96, createPolygon(CRS_2D, SQUARE, HOLE).getArea().getValue().doubleValue(),
                     TOLERANCE, "A square of 10 × 10 less a hole of 2 × 2.");
    }

    /**
     * The centroid of a surface is weighted by area. The hole being at the center of the square, it
     * removes as much area on one side of the center as on the other and therefore leaves the
     * centroid where it was. That position is then in the hole, and is therefore not on the
     * surface: a centroid is not required to lie on the geometry it summarizes.
     */
    @Test
    public void testGetCentroid() {
        assertPositionEquals(5, 5, createPolygon(CRS_2D, SQUARE).getCentroid());
        assertPositionEquals(5, 5, createPolygon(CRS_2D, SQUARE, HOLE).getCentroid());
    }

    /**
     * Contrarily to the centroid, the returned position shall lie on the surface, but which
     * position is returned is left to the implementation. On the surface with a hole, lying on it
     * means being inside the exterior ring and outside the hole, so the centroid would not be an
     * acceptable answer.
     */
    @Test
    public void testGetPointOnSurface() {
        double[] position = createPolygon(CRS_2D, SQUARE).getPointOnSurface().getPosition().toArrayDouble();
        assertTrue(position[0] > 0 && position[0] < 10 && position[1] > 0 && position[1] < 10,
                   "The position shall be interior to the surface.");

        position = createPolygon(CRS_2D, SQUARE, HOLE).getPointOnSurface().getPosition().toArrayDouble();
        assertTrue(position[0] > 0 && position[0] < 10 && position[1] > 0 && position[1] < 10,
                   "The position shall be inside the exterior ring.");
        assertFalse(position[0] > 4 && position[0] < 6 && position[1] > 4 && position[1] < 6,
                    "The position shall be outside the hole.");
    }

    /**
     * The boundary of a surface is the set of rings bounding it, so its length is the perimeter of
     * that surface.
     */
    @Test
    public void testGetBoundary() {
        final Geometry boundary = createPolygon(CRS_2D, SQUARE).getBoundary();
        assertFalse(boundary.isEmpty(), "A surface is bounded by at least one ring.");
        assertEquals(1, boundary.getTopologicDimension(), "The boundary of a surface is made of curves.");
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumDerivativesBoundary() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumDerivativeInterior() {
    }

    /**
     * The perimeter of a surface with a hole includes the length of the ring bounding that hole.
     */
    @Test
    public void testGetPerimeter() {
        final Quantity<?> perimeter = createPolygon(CRS_2D, SQUARE).getPerimeter();
        assertEquals(40, perimeter.getValue().doubleValue(), TOLERANCE, "The four edges of a square of 10 × 10.");
        assertEquals(Units.DEGREE, perimeter.getUnit(), "The perimeter shall use the unit of the axes.");
        assertEquals(48, createPolygon(CRS_2D, SQUARE, HOLE).getPerimeter().getValue().doubleValue(),
                     TOLERANCE, "The four edges of the square, plus the four edges of the hole.");
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDataPoints() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetControlPoints() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetKnots() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testUpNormal() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetOrientationSign() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetProxy() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetPrimitive() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetReverse() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetBoundaryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDimension() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetSegments() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetCoordinateReferenceSystem() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSetCoordinateReferenceSystem() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetMetadata() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDimension_DirectPosition() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIs3D() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetSpatialDimension() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType2() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEnvelope() {
    }

    /**
     * The representative position of ISO 19107 is the position which OGC Simple Feature
     * Access calls the position on the surface.
     */
    @Test
    public void testGetRepresentativePoint() {
        final double[] position = createPolygon(CRS_2D, SQUARE).getRepresentativePoint()
                                                               .getPosition().toArrayDouble();
        assertTrue(position[0] > 0 && position[0] < 10 && position[1] > 0 && position[1] < 10,
                   "The position shall be interior to the surface.");
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetClosure() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetMaximalComplex() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsEmpty() {
    }

    /**
     * A hole is not an anomalous position.
     */
    @Test
    public void testIsSimple() {
        assertTrue(createPolygon(CRS_2D, SQUARE).isSimple(),       "A square has no anomalous position.");
        assertTrue(createPolygon(CRS_2D, SQUARE, HOLE).isSimple(), "A hole is not an anomalous position.");
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsCycle() {
    }

    /**
     * Contrarily to a curve, the ring of a surface is not allowed to cross itself.
     */
    @Test
    public void testIsValid() {
        assertTrue (createPolygon(CRS_2D, SQUARE).isValid(),       "A square is a valid surface.");
        assertTrue (createPolygon(CRS_2D, SQUARE, HOLE).isValid(), "A hole inside the exterior ring is valid.");
        assertFalse(createPolygon(CRS_2D, BOWTIE).isValid(),       "A ring crossing itself is not valid.");
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testUserProperties() {
    }
}
