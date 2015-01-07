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
