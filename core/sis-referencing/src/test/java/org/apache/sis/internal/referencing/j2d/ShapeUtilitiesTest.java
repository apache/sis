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

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link ShapeUtilities} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-3.20)
 * @version 0.5
 * @module
 */
public final strictfp class ShapeUtilitiesTest extends TestCase {
    /**
     * Tolerance factor for the tests in this class.
     */
    private static final double EPS = 1E-8;

    /**
     * Tests {@link ShapeUtilities#cubicCurveExtremum(double, double, double, double, double, double)}.
     */
    @Test
    public void testCubicCurveExtremum() {
        final Point2D.Double P1 = new Point2D.Double();
        final Point2D.Double P2 = new Point2D.Double();
        double dy1, dy2;
        Line2D extremums;

        P1.x =  0; P1.y =  0; dy1 =   7;
        P2.x = -4; P2.y =  0; dy2 = -12;
        extremums = ShapeUtilities.cubicCurveExtremum(P1.x, P1.y, dy1, P2.x, P2.y, dy2);
        assertEquals("X1",   3.31741507, extremums.getX1(), EPS);
        assertEquals("Y1",  17.31547745, extremums.getY1(), EPS);
        assertEquals("X2",  -2.25074840, extremums.getX2(), EPS);
        assertEquals("Y2",  -9.65918115, extremums.getY2(), EPS);

        P1.x = 0; P1.y =  0; dy1 = 5;
        P2.x = 5; P2.y = 20; dy2 = 1;
        extremums = ShapeUtilities.cubicCurveExtremum(P1.x, P1.y, dy1, P2.x, P2.y, dy2);
        assertEquals("X1",   5.47313697, extremums.getX1(), EPS);
        assertEquals("Y1",  20.24080512, extremums.getY1(), EPS);
        assertEquals("X2",  -3.80647030, extremums.getX2(), EPS);
        assertEquals("Y2", -11.72228660, extremums.getY2(), EPS);
    }
}
