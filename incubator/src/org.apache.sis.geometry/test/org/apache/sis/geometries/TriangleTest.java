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
package org.apache.sis.geometries;

import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TriangleTest {

    /**
     * LinearRing constructor test.
     */
    @Test
    public void constructorTest() {

        final Array positions = NDArrays.of(CommonCRS.WGS84.normalizedGeographic(), new double[]{0,0, 1,0, 0,1, 0,0});
        final PointSequence points = GeometryFactory.createSequence(positions);
        final LinearRing exterior = GeometryFactory.createLinearRing(points);
        final Triangle triangle = GeometryFactory.createTriangle(exterior);

        assertNotNull(triangle.getExteriorRing());
        assertTrue(triangle.getInteriorRings().isEmpty());
        assertEquals(exterior, triangle.getExteriorRing());
    }

    /**
     * Barycentric coordinate tests.
     */
    @Test
    public void barycentreTest() {

        double x1,y1,x2,y2,x3,y3,x,y,epsilon;
        double tolerance = 0.0;
        double[] b;

        {
            x1 = 0;
            y1 = 0;
            x2 = 1;
            y2 = 0;
            x3 = 0;
            y3 = 1;
            epsilon = 0;
            /*
             *   3 +
             *     |\
             *     | \
             *   1 o--+ 2
             */
            x = 0;
            y = 0;
            b = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y, epsilon, true);
            assertNotNull(b);
            assertArrayEquals(new double[]{1,0,0}, b, tolerance);
            /*
             *   3 +
             *     |\
             *     | \
             *   1 +--o 2
             */
            x = 1;
            y = 0;
            b = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y, epsilon, true);
            assertNotNull(b);
            assertArrayEquals(new double[]{0,1,0}, b, tolerance);
            /*
             *   3 o
             *     |\
             *     | \
             *   1 +--+ 2
             */
            x = 0;
            y = 1;
            b = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y, epsilon, true);
            assertNotNull(b);
            assertArrayEquals(new double[]{0,0,1}, b, tolerance);
            /*
             *   3 +  o
             *     |\
             *     | \
             *   1 +--+ 2
             */
            x = 1;
            y = 1;
            b = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y, epsilon, true);
            assertNull(b);
            /*
             *   3 +
             *     |\
             *     | o
             *     |  \
             *   1 +---+ 2
             */
            x = 0.5;
            y = 0.5;
            b = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y, epsilon, true);
            assertNotNull(b);
            assertArrayEquals(new double[]{0,0.5,0.5}, b, tolerance);
            /*
             *   3 +
             *     |\
             *     | \
             *     |  \
             *   1 +-o-+ 2
             */
            x = 0.5;
            y = 0;
            b = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y, epsilon, true);
            assertNotNull(b);
            assertArrayEquals(new double[]{0.5,0.5,0}, b, tolerance);
            /*
             *   3 +
             *     |\
             *     o \
             *     |  \
             *   1 +---+ 2
             */
            x = 0;
            y = 0.5;
            b = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y, epsilon, true);
            assertNotNull(b);
            assertArrayEquals(new double[]{0.5,0,0.5}, b, tolerance);
            /*
             *   3 +
             *     |\
             *     | \
             *     |o \
             *   1 +---+ 2
             */
            x = 0.3;
            y = 0.3;
            tolerance = 0.1;
            b = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y, epsilon, true);
            assertNotNull(b);
            assertArrayEquals(new double[]{0.4,0.3,0.3}, b, tolerance);
        }

    }

}
