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
import org.apache.sis.geometries.GeometryTest;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link MultiPolygon}.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class MultiPolygonTest extends GeometryTest {
    /**
     * Two disjoint squares of 10 × 10, the second one being 20 away from the first one along
     * both axes. They have the same area, so the centroid of the set is half way between theirs.
     */
    private static final double[] SQUARE_1 = {0,0, 10,0, 10,10, 0,10, 0,0};
    private static final double[] SQUARE_2 = {20,20, 30,20, 30,30, 20,30, 20,20};

    protected MultiPolygonTest() {
    }

    /**
     * Creates a set of surfaces in the given coordinate reference system. Each surface is
     * bounded by one ring and has no hole.
     *
     * @param  crs    the coordinate reference system of the set to create, not null.
     * @param  rings  the exterior ring of each surface, in the axis order of the given system.
     * @return a new set holding the given surfaces, never null.
     */
    protected abstract MultiPolygon createMultiPolygon(CoordinateReferenceSystem crs, double[]... rings);

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetElementType() {
    }

    /**
     * The area of a set of surfaces is the sum of the areas of its elements, measured in the units
     * of the coordinate system axes.
     */
    @Test
    public void testGetArea() {
        final Quantity<?> area = createMultiPolygon(CRS_2D, SQUARE_1, SQUARE_2).getArea();
        assertEquals(200, area.getValue().doubleValue(), TOLERANCE, "Two disjoint squares of 10 × 10.");
        assertEquals(Units.DEGREE.multiply(Units.DEGREE), area.getUnit(),
                     "The area shall use the square of the unit of the axes.");
    }

    /**
     * The centroid of a set of surfaces is weighted by area, so two squares of the same area give
     * the middle of their two centroids. That position lies on neither of them, a centroid not
     * being required to lie on the geometry it summarizes.
     */
    @Test
    public void testGetCentroid() {
        final double[] centroid = createMultiPolygon(CRS_2D, SQUARE_1, SQUARE_2).getCentroid()
                                                                                .getPosition().toArrayDouble();
        assertEquals(15, centroid[0], TOLERANCE);
        assertEquals(15, centroid[1], TOLERANCE);
    }

    /**
     * Contrarily to the centroid, the returned position shall lie on one of the surfaces of the
     * set.
     */
    @Test
    public void testGetPointOnSurface() {
        final double[] position = createMultiPolygon(CRS_2D, SQUARE_1, SQUARE_2).getPointOnSurface()
                                                                                .getPosition().toArrayDouble();
        final boolean inFirst  = position[0] > 0  && position[0] < 10 && position[1] > 0  && position[1] < 10;
        final boolean inSecond = position[0] > 20 && position[0] < 30 && position[1] > 20 && position[1] < 30;
        assertTrue(inFirst || inSecond, "The position shall be interior to one of the surfaces of the set.");
    }

    /**
     * The boundary of a set of surfaces is the set of the rings bounding its elements, so it is
     * made of curves.
     */
    @Test
    public void testGetBoundary() {
        final Geometry boundary = createMultiPolygon(CRS_2D, SQUARE_1, SQUARE_2).getBoundary();
        assertFalse(boundary.isEmpty(), "A set of surfaces is bounded by the rings of its elements.");
        assertEquals(1, boundary.getTopologicDimension(), "The boundary of a surface is made of curves.");
    }

    /**
     * Two disjoint surfaces are both simple and valid.
     */
    @Test
    public void testIsSimpleAndValid() {
        final MultiPolygon surfaces = createMultiPolygon(CRS_2D, SQUARE_1, SQUARE_2);
        assertTrue(surfaces.isSimple(), "Two disjoint squares are simple.");
        assertTrue(surfaces.isValid(),  "Two disjoint squares are a valid set of surfaces.");
    }

}
