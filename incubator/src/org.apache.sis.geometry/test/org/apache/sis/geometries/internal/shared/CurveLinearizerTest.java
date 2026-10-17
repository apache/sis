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
package org.apache.sis.geometries.internal.shared;

import javax.measure.Quantity;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.Arc;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link Curve#asLine(Quantity, Quantity)} on the curves supported by {@link CurveLinearizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CurveLinearizerTest extends GeometryTest {

    private static final GeometryFactory F = GeometryFactory.DEFAULT;

    /**
     * Returns a distance in the unit of the axes of {@link #CRS_2D}.
     */
    private static Quantity<?> degrees(final double value) {
        return Quantities.create(value, Units.DEGREE);
    }

    /**
     * Returns the positions of the given line string.
     */
    private static double[][] positions(final LineString line) {
        final DataPoints points = line.getDataPoints();
        final double[][] positions = new double[points.size()][];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = points.getPosition(i).toArrayDouble();
        }
        return positions;
    }

    private static void assertPosition(final double x, final double y, final double[] position) {
        assertEquals(x, position[0], TOLERANCE, "x");
        assertEquals(y, position[1], TOLERANCE, "y");
    }

    /**
     * Verifies that the points are on the circle of the given centre and radius, and that the polyline meets
     * the given criteria. The arc length and the distance to the arc of each chord are computed exactly.
     *
     * @return the number of points.
     */
    private static int assertOnCircle(final double[][] points, final double cx, final double cy, final double r,
                                      final double spacing, final double offset)
    {
        for (final double[] p : points) {
            assertEquals(r, Math.hypot(p[0] - cx, p[1] - cy), TOLERANCE, "Point not on the circle.");
        }
        for (int i = 1; i < points.length; i++) {
            final double chord = Math.hypot(points[i][0] - points[i-1][0], points[i][1] - points[i-1][1]);
            if (spacing > 0) {
                assertTrue(2 * r * Math.asin(chord / (2*r)) <= spacing * (1 + 1E-6), "Arc longer than the spacing.");
            }
            if (offset > 0) {
                assertTrue(r - Math.sqrt(r*r - chord*chord/4) <= offset * (1 + 1E-9), "Chord farther than the offset.");
            }
        }
        return points.length;
    }

    @Test
    public void testArcByCenterPoint() {
        final var arc = F.createArcByCenterPoint(F.createPoint(CRS_2D, 10, 20), 5, null, 0, 90);
        double[][] points = positions(arc.asLine(null, null));
        assertPosition(15, 20, points[0]);
        assertPosition(10, 25, points[points.length - 1]);
        assertTrue(assertOnCircle(points, 10, 20, 5, 0, 0) > 5, "Default precision too coarse.");
        for (int i = 1; i < points.length; i++) {
            assertTrue(points[i][0] < points[i-1][0], "Points not in their order along the arc.");
        }
        // Offset: the chords are at most at 0.01 from the arc, which needs at least 8 chords for a quarter of circle.
        points = positions(arc.asLine(null, degrees(0.01)));
        final int count = assertOnCircle(points, 10, 20, 5, 0, 0.01);
        assertTrue(count >= 9 && count <= 17, "Unexpected number of points: " + count);
        // Spacing: the arcs between points are at most 0.5 long.
        points = positions(arc.asLine(degrees(0.5), null));
        assertOnCircle(points, 10, 20, 5, 0.5, 0);
        /*
         * Spacing slightly less than one eighth of the arc. The sum of the chords underestimates the length of
         * each eighth by about 10⁻⁴ of it, so the arc length shall be extrapolated for detecting the excess.
         */
        final double eighth = Math.PI * 5 / 2 / 8 * (1 - 5E-5);
        assertTrue(assertOnCircle(positions(arc.asLine(degrees(eighth), null)), 10, 20, 5, eighth, 0) > 9);
        // Spacing in percent of the arc length.
        points = positions(arc.asLine(Quantities.create(10, Units.PERCENT), null));
        assertOnCircle(points, 10, 20, 5, 0.1 * Math.PI * 5 / 2, 0);
        // Clockwise arc.
        points = positions(F.createArcByCenterPoint(F.createPoint(CRS_2D, 10, 20), 5, null, 90, 0).asLine(null, null));
        assertPosition(10, 25, points[0]);
        assertPosition(15, 20, points[points.length - 1]);
    }

    @Test
    public void testCircularString() {
        final var curve = F.createCircularString(F.createDataPoints(CRS_2D, 0,0, 5,5, 10,0, 5,-5, 0,0));
        final double[][] points = positions(curve.asLine(null, degrees(0.05)));
        assertOnCircle(points, 5, 0, 5, 0, 0.05);
        assertPosition(0, 0, points[0]);
        assertPosition(0, 0, points[points.length - 1]);
        // The data points are in the polyline, in their order.
        int next = 1;
        final double[][] data = {{5,5}, {10,0}, {5,-5}};
        for (final double[] p : points) {
            if (next <= data.length && Math.abs(p[0] - data[next-1][0]) < TOLERANCE && Math.abs(p[1] - data[next-1][1]) < TOLERANCE) {
                next++;
            }
        }
        assertEquals(data.length + 1, next, "Data point missing in the polyline.");
        // Full circle given by identical start and end points.
        assertOnCircle(positions(F.createCircularString(F.createDataPoints(CRS_2D, 0,0, 10,0, 0,0)).asLine(null, null)), 5, 0, 5, 0, 0);
    }

    @Test
    public void testArcByBulge() {
        final Vector<?> normal = Vectors.createDouble(2).set(new double[] {0, 1});
        final var arc = F.createArcByBulge(F.createDataPoints(CRS_2D, 0,0, 10,0), 2, normal);
        final double[][] points = positions(arc.asLine(degrees(1), null));
        assertOnCircle(points, 5, -5.25, 7.25, 1, 0);
        assertPosition(0, 0, points[0]);
        assertPosition(10, 0, points[points.length - 1]);
        for (final double[] p : points) {
            assertTrue(p[1] >= -TOLERANCE && p[1] <= 2 + TOLERANCE, "Point outside the arc.");
        }
        // A null bulge is a straight segment.
        assertEquals(2, positions(F.createArcByBulge(F.createDataPoints(CRS_2D, 0,0, 10,0), 0, normal).asLine(null, null)).length);
    }

    @Test
    public void testArc() {
        final Arc arc = F.createArc(F.createDataPoints(CRS_2D, 5,0, 0,5, -5,0),
                NDArrays.of(CRS_2D, new double[] {0,0, 0,0}), null, false);
        final double[][] points = positions(arc.asLine(null, degrees(0.01)));
        assertOnCircle(points, 0, 0, 5, 0, 0.01);
        for (final double[] p : points) {
            assertTrue(p[1] >= -TOLERANCE, "Point on the wrong side of the circle.");
        }
        // The envelope of an arc uses its centres, not the five points of a general conic.
        final var envelope = arc.getEnvelope();
        assertEquals(-5, envelope.getMinimum(0), TOLERANCE);
        assertEquals( 0, envelope.getMinimum(1), TOLERANCE);
        assertEquals( 5, envelope.getMaximum(0), TOLERANCE);
        assertEquals( 5, envelope.getMaximum(1), TOLERANCE);
    }

    @Test
    public void testConic() {
        final double[] t = {-0.3, 0.4, 1.2, 2.0, 2.6};
        final double[] coordinates = new double[10];
        for (int i = 0; i < t.length; i++) {
            coordinates[2*i]     = 2 * Math.cos(t[i]);
            coordinates[2*i + 1] = Math.sin(t[i]);
        }
        final var conic = F.createConic(F.createDataPoints(CRS_2D, coordinates), null, false);
        final double[][] points = positions(conic.asLine(null, degrees(0.001)));
        double previous = Double.NEGATIVE_INFINITY;
        for (final double[] p : points) {
            assertEquals(1, p[0]*p[0]/4 + p[1]*p[1], 1E-9, "Point not on the ellipse.");
            final double angle = Math.atan2(p[1], p[0] / 2);
            assertTrue(angle > previous, "Points not in their order along the ellipse.");
            previous = angle;
        }
        assertPosition(coordinates[0], coordinates[1], points[0]);
        assertPosition(coordinates[8], coordinates[9], points[points.length - 1]);
        assertTrue(points.length > 20, "Offset not met.");
    }

    @Test
    public void testNurbCurve() {
        // A quarter of the unit circle, as a rational quadratic curve.
        final var curve = F.createNurbCurve(F.createDataPoints(CRS_2D, 1,0, 1,1, 0,1),
                new double[] {1, Math.sqrt(0.5), 1}, new double[] {0, 0, 0, 1, 1, 1}, 2);
        final double[][] points = positions(curve.asLine(degrees(0.1), degrees(0.001)));
        assertOnCircle(points, 0, 0, 1, 0.1, 0.001);
        assertPosition(1, 0, points[0]);
        assertPosition(0, 1, points[points.length - 1]);
    }

    @Test
    public void testBezier() {
        // Quadratic Bézier curve on the parabola y = x².
        final var curve = F.createBezier(F.createDataPoints(CRS_2D, -1,1, 1,1),
                NDArrays.of(CRS_2D, new double[] {-1,1, 0,-1, 1,1}), new double[] {0, 1}, 2, null, null, null, null);
        final double[][] points = positions(curve.asLine(null, null));
        assertTrue(points.length > 5);
        for (final double[] p : points) {
            assertEquals(p[0] * p[0], p[1], TOLERANCE, "Point not on the parabola.");
        }
        assertPosition(-1, 1, points[0]);
        assertPosition( 1, 1, points[points.length - 1]);
    }

    @Test
    public void testCompoundAndReversed() {
        final var line = F.createLineString(F.createDataPoints(CRS_2D, 0,0, 5,0));
        final var arc  = F.createCircularString(F.createDataPoints(CRS_2D, 5,0, 10,5, 15,0));
        final var compound = F.createCompoundCurve(line, arc);
        double[][] points = positions(compound.asLine(degrees(1), null));
        assertPosition(0, 0, points[0]);
        assertPosition(15, 0, points[points.length - 1]);
        for (int i = 1; i < points.length; i++) {
            final double step = Math.hypot(points[i][0] - points[i-1][0], points[i][1] - points[i-1][1]);
            assertTrue(step > TOLERANCE, "Repeated point.");
            assertTrue(step <= 1 + TOLERANCE, "Points farther than the spacing.");
        }
        // The reversed curve has the same points in reverse order.
        final double[][] reversed = positions(F.createReversed(compound).asLine(degrees(1), null));
        assertEquals(points.length, reversed.length);
        for (int i = 0; i < points.length; i++) {
            assertArrayEquals(points[i], reversed[reversed.length - 1 - i], TOLERANCE);
        }
    }

    @Test
    public void testLineString() {
        final var line = F.createLineString(F.createDataPoints(CRS_2D, 0,0, 10,0));
        assertSame(line, line.asLine(null, null));
        assertSame(line, line.asLine(null, degrees(1)));
        final double[][] points = positions(line.asLine(degrees(3), null));
        assertEquals(5, points.length);
        for (int i = 0; i < points.length; i++) {
            assertPosition(2.5 * i, 0, points[i]);
        }
    }

    @Test
    public void testInvalid() {
        final var arc = F.createArcByCenterPoint(F.createPoint(CRS_2D, 10, 20), 5, null, 0, 90);
        assertThrows(IllegalArgumentException.class, () -> arc.asLine(degrees(-1), null));
        final Vector<?> origin = Vectors.createDouble(CRS_2D).set(new double[] {0, 0});
        final Vector<?> direction = Vectors.createDouble(CRS_2D).set(new double[] {1, 0});
        assertThrows(UnsupportedOperationException.class, () -> F.createRay(origin, direction).asLine(null, null));
    }
}
