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
package org.apache.sis.internal.referencing.j2d;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.CubicCurve2D;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the {@link ShapeUtilities} class.
 * Values in this test were determined empirically by running {@link ShapeUtilitiesViewer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class ShapeUtilitiesTest extends TestCase {
    /**
     * Tolerance factor for the tests in this class.
     */
    private static final double EPS = 1E-12;

    /**
     * Asserts that the given point is equals to the given value.
     */
    private static void assertPointEquals(final double x, final double y, final Point2D point) {
        assertEquals(x, point.getX(), EPS);
        assertEquals(y, point.getY(), EPS);
    }

    /**
     * Tests {@link ShapeUtilities#intersectionPoint(double, double, double, double, double, double, double, double)}.
     */
    @Test
    public void testIntersectionPoint() {
        assertPointEquals(373.0459349536288,  153.4272907665677, ShapeUtilities.intersectionPoint(164, 261, 541,  67, 475, 214, 135,  12));
        assertPointEquals(164.2967949656081,  147.2610859066296, ShapeUtilities.intersectionPoint(  6, 259, 227, 103, 328, 254,  32,  61));
        assertPointEquals(206.18415613599478, 276.5596260282143, ShapeUtilities.intersectionPoint(549, 309, 158, 272, 495, 138, 174, 292));

        assertNull("Segments do not intersect.", ShapeUtilities.intersectionPoint( 52, 61, 419, 209, 419, 130, 529, 303));
        assertNull("Segments do not intersect.", ShapeUtilities.intersectionPoint( 53, 62, 222, 221, 494, 158, 382, 174));
        assertNull("Segments do not intersect.", ShapeUtilities.intersectionPoint(566, 296, 386, 305, 553, 51, 408, 291));
    }

    /**
     * Tests {@link ShapeUtilities#nearestColinearPoint(double, double, double, double, double, double)}.
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
     */
    @Test
    public void testColinearPoint() {
        assertPointEquals(292.1838668370446,  278.764084678759,   ShapeUtilities.colinearPoint(214, 297, 587, 210, 104, 77, 275.902));
        assertPointEquals(151.57330058770097, 162.19277228964654, ShapeUtilities.colinearPoint(187,  93, 123, 218, 204, 16, 155.309));
        assertPointEquals(568.6671514383643,  274.6199927862288,  ShapeUtilities.colinearPoint(232,  84, 587, 285, 469, 31, 263.219));

        assertNull("No point at the given distance.", ShapeUtilities.colinearPoint(415, 112, 21,  269, 223, 270, 341.434));
        assertNull("No point at the given distance.", ShapeUtilities.colinearPoint(353, 235, 233, 104, 423,  81, 558.129));
    }

    /**
     * Invokes {@code ShapeUtilities.fitParabol(x1, y1, px, py, x2, y2, horizontal)}, then verifies that the control
     * point of the returned curve is equals to {@code (cx, cy)}.
     */
    private static void assertParabolEquals(final double cx, final double cy,
                                            final double x1, final double y1,
                                            final double px, final double py,
                                            final double x2, final double y2,
                                            final boolean horizontal)
    {
        final QuadCurve2D p = ShapeUtilities.fitParabol(x1, y1, px, py, x2, y2, horizontal);
        assertPointEquals(x1, y1, p.getP1());
        assertPointEquals(x2, y2, p.getP2());
        assertPointEquals(cx, cy, p.getCtrlPt());
    }

    /**
     * Tests {@link ShapeUtilities#fitParabol(double, double, double, double, double, double, boolean)}
     * with a {@code false} boolean argument.
     */
    @Test
    public void testFitParabol() {
        assertParabolEquals(203.09937404322247, 298.52149034018106, 188,  25, 367, 282, 477, 294, false);
        assertParabolEquals(440.2165208525737,  147.92614458270768, 342, 193, 503, 182, 537, 196, false);
        assertParabolEquals(688.8232271997849,  117.2311838864974,  488, 241, 578, 134, 455,  86, false);
    }

    /**
     * Tests {@link ShapeUtilities#fitParabol(double, double, double, double, double, double, boolean)}
     * with a {@code true} boolean argument.
     */
    @Test
    public void testFitHorizontalParabol() {
        assertParabolEquals(327.0, 272.41465201465195, 538, 197, 473, 213, 116, 43, true);
    }

    /**
     * Tests {@link ShapeUtilities#circleCentre(double, double, double, double, double, double)}.
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
        assertInstanceOf("toPrimitive", Line2D.class, p);
        assertEquals("P1", new Point2D.Double(4, 5), ((Line2D) p).getP1());
        assertEquals("P2", new Point2D.Double(7, 9), ((Line2D) p).getP2());

        path.reset();
        path.moveTo(4, 5);
        path.quadTo(6, 7, 8, 5);
        p = ShapeUtilities.toPrimitive(path);
        assertInstanceOf("toPrimitive", QuadCurve2D.class, p);
        assertEquals("P1",     new Point2D.Double(4, 5), ((QuadCurve2D) p).getP1());
        assertEquals("CtrlPt", new Point2D.Double(6, 7), ((QuadCurve2D) p).getCtrlPt());
        assertEquals("P2",     new Point2D.Double(8, 5), ((QuadCurve2D) p).getP2());

        path.reset();
        path.moveTo(4, 5);
        path.curveTo(6, 7, 8, 6, 9, 4);
        p = ShapeUtilities.toPrimitive(path);
        assertInstanceOf("toPrimitive", CubicCurve2D.class, p);
        assertEquals("P1",     new Point2D.Double(4, 5), ((CubicCurve2D) p).getP1());
        assertEquals("CtrlP1", new Point2D.Double(6, 7), ((CubicCurve2D) p).getCtrlP1());
        assertEquals("CtrlP2", new Point2D.Double(8, 6), ((CubicCurve2D) p).getCtrlP2());
        assertEquals("P2",     new Point2D.Double(9, 4), ((CubicCurve2D) p).getP2());
    }
}
