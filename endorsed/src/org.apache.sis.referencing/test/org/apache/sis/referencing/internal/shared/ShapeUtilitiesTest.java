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
package org.apache.sis.referencing.internal.shared;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.CubicCurve2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link ShapeUtilities} class.
 * Values in this test were determined empirically by running {@link ShapeUtilitiesViewer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ShapeUtilitiesTest extends TestCase {
    /**
     * Tolerance factor for the tests in this class.
     */
    private static final double EPS = 1E-12;

    /**
     * Asserts that the given point is equal to the given value.
     */
    private static void assertPointEquals(final double x, final double y, final Point2D point) {
        assertEquals(x, point.getX(), EPS);
        assertEquals(y, point.getY(), EPS);
    }

    /**
     * Creates a new test case.
     */
    public ShapeUtilitiesTest() {
    }

    /**
     * Tests {@link ShapeUtilities#intersectionPoint(double, double, double, double, double, double, double, double)}.
     * This is an anti-regression test with values computed by {@link ShapeUtilitiesViewer}.
     */
    @Test
    public void testIntersectionPoint() {
        assertPointEquals(373.0459349536288,  153.4272907665677, ShapeUtilities.intersectionPoint(164, 261, 541,  67, 475, 214, 135,  12));
        assertPointEquals(164.2967949656081,  147.2610859066296, ShapeUtilities.intersectionPoint(  6, 259, 227, 103, 328, 254,  32,  61));
        assertPointEquals(206.18415613599478, 276.5596260282143, ShapeUtilities.intersectionPoint(549, 309, 158, 272, 495, 138, 174, 292));

        assertNull(ShapeUtilities.intersectionPoint( 52, 61, 419, 209, 419, 130, 529, 303));
        assertNull(ShapeUtilities.intersectionPoint( 53, 62, 222, 221, 494, 158, 382, 174));
        assertNull(ShapeUtilities.intersectionPoint(566, 296, 386, 305, 553, 51, 408, 291));
    }

    /**
     * Tests {@link ShapeUtilities#nearestColinearPoint(double, double, double, double, double, double)}.
     * This is an anti-regression test with values computed by {@link ShapeUtilitiesViewer}.
     */
    @Test
    public void testNearestColinearPoint() {
        assertPointEquals(250.3762957286331,   41.53513088371716, ShapeUtilities.nearestColinearPoint(251,  41,  82, 186, 334, 139));
        assertPointEquals(339.88188039274024, 107.30764653376968, ShapeUtilities.nearestColinearPoint(318, 189, 363,  21, 167,  61));
        assertPointEquals(120.45221035554958, 270.19778288495337, ShapeUtilities.nearestColinearPoint(100, 279, 574,  75, 135, 304));
        assertPointEquals(141.0,              203.0,              ShapeUtilities.nearestColinearPoint(351,  17, 141, 203,  55, 221));
        assertPointEquals(333.6115520537628,  160.28938068478067, ShapeUtilities.nearestColinearPoint(289,  25, 381, 304,  10, 267));
    }

    /**
     * Tests {@link ShapeUtilities#colinearPoint(double, double, double, double, double, double, double)}.
     * This is an anti-regression test with values computed by {@link ShapeUtilitiesViewer}.
     */
    @Test
    public void testColinearPoint() {
        assertPointEquals(292.1838668370446,  278.764084678759,   ShapeUtilities.colinearPoint(214, 297, 587, 210, 104, 77, 275.902));
        assertPointEquals(151.57330058770097, 162.19277228964654, ShapeUtilities.colinearPoint(187,  93, 123, 218, 204, 16, 155.309));
        assertPointEquals(568.6671514383643,  274.6199927862288,  ShapeUtilities.colinearPoint(232,  84, 587, 285, 469, 31, 263.219));

        assertNull(ShapeUtilities.colinearPoint(415, 112, 21,  269, 223, 270, 341.434));
        assertNull(ShapeUtilities.colinearPoint(353, 235, 233, 104, 423,  81, 558.129));
    }

    /**
     * Invokes {@code ShapeUtilities.fitParabol(x1, y1, px, py, x2, y2)},
     * then verifies that the control point of the returned curve is equal to {@code (cx, cy)}.
     */
    private static void assertParabolEquals(final double cx, final double cy,
                                            final double x1, final double y1,
                                            final double px, final double py,
                                            final double x2, final double y2)
    {
        final QuadCurve2D p = ShapeUtilities.fitParabol(x1, y1, px, py, x2, y2);
        assertPointEquals(x1, y1, p.getP1());
        assertPointEquals(x2, y2, p.getP2());
        assertPointEquals(cx, cy, p.getCtrlPt());
    }

    /**
     * Tests {@link ShapeUtilities#fitParabol(double, double, double, double, double, double, boolean)}
     * with a {@code false} boolean argument.
     * This is an anti-regression test with values computed by {@link ShapeUtilitiesViewer}.
     */
    @Test
    public void testFitParabol() {
        assertParabolEquals(203.09937404322247, 298.52149034018106, 188,  25, 367, 282, 477, 294);
        assertParabolEquals(440.2165208525737,  147.92614458270768, 342, 193, 503, 182, 537, 196);
        assertParabolEquals(688.8232271997849,  117.2311838864974,  488, 241, 578, 134, 455,  86);
    }

    /**
     * Invokes {@code ShapeUtilities.fitCubicCurve(x1, y1, xm, ym, x2, y2, α1, α2)}, then verifies that
     * the control points of the returned curve are equal to {@code (cx1, cy1)} and {@code (cx2, cy2)}.
     */
    private static void assertCubicCurveEquals(final double cx1, final double cy1,
                                               final double cx2, final double cy2,
                                               final double x1,  final double y1,
                                               final double xm,  final double ym,
                                               final double x2,  final double y2,
                                               final double α1,  final double α2)
    {
        final CubicCurve2D p = (CubicCurve2D) ShapeUtilitiesExt.bezier(x1, y1, xm, ym, x2, y2, α1, α2, 1, 1);
        assertPointEquals( x1,  y1, p.getP1());
        assertPointEquals( x2,  y2, p.getP2());
        assertPointEquals(cx1, cy1, p.getCtrlP1());
        assertPointEquals(cx2, cy2, p.getCtrlP2());
    }

    /**
     * Tests {@link ShapeUtilitiesExt#bezier(double, double, double, double, double, double, double, double, double, double)}.
     * This is an anti-regression test with values computed by {@link ShapeUtilitiesViewer}.
     */
    @Test
    public void testBezier() {
        /*
         * Case when the curve can be simplified to a straight line.
         * This test uses a line starting from (100,200) with a slope of 1.3333…
         */
        final Line2D c1 = (Line2D) ShapeUtilitiesExt.bezier(
                100, 200,                           // Start point:  P1
                175, 300,                           // Midle point:  Pm = P1 + (75,100)
                250, 400,                           // End point:    P2 = Pm + (75,100)
                100./75, 100./75,                   // Slope
                1, 1);                              // Tolerance
        assertPointEquals(100, 200, c1.getP1());
        assertPointEquals(250, 400, c1.getP2());
        /*
         * Case when the curve can be simplified to a quadratic curve. First we build a quadratic curve at points
         *
         *     P₁=(100,200), Q=(400.0, 300), P₂=(550,600)   where    Q is the control point (not the midway point).
         *                  Pm=(362.5, 350)                 where   Pm is the midway point B(½) = ¼P₁ + ½Q + ¼P₂
         *
         * Derivatives are:
         *
         *     α₁  =  2(Q - P₁)  =  (600, 200)  = 1/3
         *     α₂  =  2(P₂ - Q)  =  (300, 600)  = 2
         *
         * The control point of a cubic curve are below (can be verified in the debugger):
         *
         *     C₁  =  ⅓P₁ + ⅔Q  = (300, 266.666…)
         *     C₂  =  ⅓P₂ + ⅔Q  = (450, 400.0)
         */
        final QuadCurve2D c2 = (QuadCurve2D) ShapeUtilitiesExt.bezier(
                100,   200,                         // Start point
                362.5, 350,                         // Midway point
                550,   600,                         // End point
                1./3,    2,                         // Derivatives
                  1,     1);                        // Tolerance
        assertPointEquals(100, 200, c2.getP1());
        assertPointEquals(550, 600, c2.getP2());
        assertPointEquals(400, 300, c2.getCtrlPt());
        /*
         * Cubic case (empirical values from ShapeUtilitiesViewer).
         */
        assertCubicCurveEquals(886.54566341452,   354.9913859188133,
                               635.1210032521466, 438.6752807478533,                        // Expected control points.
                               1143, 62, 739, 345, 204, 317, -1.14247, 0.282230);
    }

    /**
     * Tests {@link ShapeUtilities#circleCentre(double, double, double, double, double, double)}.
     * This is an anti-regression test with values computed by {@link ShapeUtilitiesViewer}.
     */
    @Test
    public void testCircleCentre() {
        assertPointEquals(117.40902595856156, 151.49785663253124, ShapeUtilities.circleCentre(182, 103, 50, 107, 124, 232));
    }

    /**
     * Tests {@link ShapeUtilities#toPrimitive(Shape)}.
     */
    @Test
    public void testToPrimitive() {
        final Path2D path = new Path2D.Double();
        path.moveTo(4, 5);
        path.lineTo(7, 9);
        Shape p = ShapeUtilities.toPrimitive(path);
        var line = assertInstanceOf(Line2D.class, p);
        assertEquals(new Point2D.Double(4, 5), line.getP1());
        assertEquals(new Point2D.Double(7, 9), line.getP2());

        path.reset();
        path.moveTo(4, 5);
        path.quadTo(6, 7, 8, 5);
        p = ShapeUtilities.toPrimitive(path);
        var quad = assertInstanceOf(QuadCurve2D.class, p);
        assertEquals(new Point2D.Double(4, 5), quad.getP1());
        assertEquals(new Point2D.Double(6, 7), quad.getCtrlPt());
        assertEquals(new Point2D.Double(8, 5), quad.getP2());

        path.reset();
        path.moveTo(4, 5);
        path.curveTo(6, 7, 8, 6, 9, 4);
        p = ShapeUtilities.toPrimitive(path);
        var cubic = assertInstanceOf(CubicCurve2D.class, p);
        assertEquals(new Point2D.Double(4, 5), cubic.getP1());
        assertEquals(new Point2D.Double(6, 7), cubic.getCtrlP1());
        assertEquals(new Point2D.Double(8, 6), cubic.getCtrlP2());
        assertEquals(new Point2D.Double(9, 4), cubic.getP2());
    }
}
