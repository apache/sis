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
package org.apache.sis.geometries.operation.spatialanalysis2d;

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LineString;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link Centerline}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CenterlineTest {

    /**
     * A long rectangle gives a single polyline along its middle.
     * <pre>
     *  10 ■----------------------------------■
     *     |  ============================    |
     *   0 ■----------------------------------■
     *     0                                 100
     * </pre>
     */
    @Test
    public void testRectangle() {
        final List<double[]> lines = compute(0,0, 100,0, 100,10, 0,10, 0,0);
        assertEquals(1, lines.size());
        final double[] line = lines.get(0);
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < line.length; i += 2) {
            if (line[i] > 10 && line[i] < 90) {
                //ends may bend toward a corner
                assertEquals(5, line[i+1], 1E-9, "Centerline shall be in the middle of the rectangle.");
            }
            minX = Math.min(minX, line[i]);
            maxX = Math.max(maxX, line[i]);
        }
        assertTrue(minX < 10, "Centerline shall reach the left end.");
        assertTrue(maxX > 90, "Centerline shall reach the right end.");
    }

    /**
     * A L shape gives a single polyline with a bend.
     * <pre>
     * 100          ■--■
     *              |  |
     *  10 ■--------■  |
     *   0 ■-----------■
     *     0          90 100
     * </pre>
     */
    @Test
    public void testLShape() {
        final List<double[]> lines = compute(0,0, 100,0, 100,100, 90,100, 90,10, 0,10, 0,0);
        assertEquals(1, lines.size());
        assertTrue(reaches(lines, (x, y) -> x < 50 && Math.abs(y - 5) < 2), "Centerline shall follow the horizontal branch.");
        assertTrue(reaches(lines, (x, y) -> y > 50 && Math.abs(x - 95) < 2), "Centerline shall follow the vertical branch.");
    }

    /**
     * A T shape gives branches reaching the three ends.
     * <pre>
     * 100 ■-----------------■
     *  90 ■------■   ■------■
     *            |   |
     *   0        ■---■
     *     0     45   55    100
     * </pre>
     */
    @Test
    public void testTShape() {
        final List<double[]> lines = compute(45,0, 55,0, 55,90, 100,90, 100,100, 0,100, 0,90, 45,90, 45,0);
        assertTrue(lines.size() >= 3, "Three branches expected.");
        assertTrue(reaches(lines, (x, y) -> x < 15), "Centerline shall reach the left end.");
        assertTrue(reaches(lines, (x, y) -> x > 85), "Centerline shall reach the right end.");
        assertTrue(reaches(lines, (x, y) -> y < 15), "Centerline shall reach the bottom end.");
    }

    private interface Condition {
        boolean test(double x, double y);
    }

    /**
     * Returns whether a point of the lines satisfies the condition.
     */
    private static boolean reaches(final List<double[]> lines, final Condition condition) {
        for (double[] line : lines) {
            for (int i = 0; i < line.length; i += 2) {
                if (condition.test(line[i], line[i+1])) return true;
            }
        }
        return false;
    }

    /**
     * Computes the centerline of a polygon without hole, with a densification of 2 and branches of 10,
     * and returns the coordinates of each polyline.
     */
    private static List<double[]> compute(final double... exterior) {
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        final Centerline centerline = new Centerline(2);
        centerline.setMinimumBranchLength(10);
        final GeometryCollection<LineString> result = centerline.compute(factory.createPolygon(
                factory.createLinearRing(factory.createDataPoints(Geometries.getUndefinedCRS(2), exterior)), List.of()));
        final List<double[]> lines = new ArrayList<>();
        for (int i = 0; i < result.getNumGeometries(); i++) {
            lines.add(result.getGeometryN(i).getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble());
        }
        return lines;
    }
}
