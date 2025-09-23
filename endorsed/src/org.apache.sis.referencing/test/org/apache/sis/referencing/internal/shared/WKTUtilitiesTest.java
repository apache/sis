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

import java.util.function.Function;
import org.opengis.referencing.cs.*;
import org.apache.sis.math.Vector;
import static org.apache.sis.referencing.internal.shared.WKTUtilities.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Tests {@link WKTUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WKTUtilitiesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public WKTUtilitiesTest() {
    }

    /**
     * Tests {@link WKTUtilities#toType(Class, Class)}.
     *
     * @see ReferencingUtilitiesTest#testToPropertyName()
     */
    @Test
    public void testToType() {
        assertNull  (                         toType(CoordinateSystem.class, CoordinateSystem.class));
        assertEquals(WKTKeywords.affine,      toType(CoordinateSystem.class, AffineCS        .class));
        assertEquals(WKTKeywords.Cartesian,   toType(CoordinateSystem.class, CartesianCS     .class));
        assertEquals(WKTKeywords.cylindrical, toType(CoordinateSystem.class, CylindricalCS   .class));
        assertEquals(WKTKeywords.ellipsoidal, toType(CoordinateSystem.class, EllipsoidalCS   .class));
        assertEquals(WKTKeywords.linear,      toType(CoordinateSystem.class, LinearCS        .class));
//      assertEquals(WKTKeywords.parametric,  toType(CoordinateSystem.class, ParametricCS    .class));
        assertEquals(WKTKeywords.polar,       toType(CoordinateSystem.class, PolarCS         .class));
        assertEquals(WKTKeywords.spherical,   toType(CoordinateSystem.class, SphericalCS     .class));
        assertEquals(WKTKeywords.temporal,    toType(CoordinateSystem.class, TimeCS          .class));
        assertEquals(WKTKeywords.vertical,    toType(CoordinateSystem.class, VerticalCS      .class));
    }

    /**
     * Tests {@link WKTUtilities#suggestFractionDigits(Vector[])}.
     */
    @Test
    public void testSuggestFractionDigitsFromVectors() {
        final int[] f = WKTUtilities.suggestFractionDigits(new Vector[] {
            Vector.create(new double[] {10, 100,   0.1, 1000.1}),
            Vector.create(new double[] {15, 140.1, 0.4, Double.NaN}),
            Vector.create(new double[] {20, 400,   0.5, Double.NaN})});
        assertArrayEquals(new int[] {0, 0, 3, 3}, f);
    }

    /**
     * Tests {@link WKTUtilities#suggestFractionDigits(CoordinateReferenceSystem, Vector[])}.
     */
    @Test
    public void testSuggestFractionDigits() {
        final Vector[] points = {
            Vector.create(new double[] {40, -10}),
            Vector.create(new double[] {50, -10.000000001})
        };
        assertArrayEquals(new int[] {8, 9}, WKTUtilities.suggestFractionDigits(HardCodedCRS.WGS84, points));
    }

    /**
     * Tests {@link WKTUtilities#cornersAndCenter(Function, int[], int)}.
     */
    @Test
    public void testCornersAndCenter() {
        final Object[] values = cornersAndCenter((p) -> 100 * p[1] + p[0], new int[] {12, 15}, 3);
        assertArrayEquals(new Number[] {   0,    1,    2, null,    9,   10,   11}, (Number[]) values[0]);
        assertArrayEquals(new Number[] { 100,  101,  102, null,  109,  110,  111}, (Number[]) values[1]);
        assertArrayEquals(new Number[] { 200,  201,  202, null,  209,  210,  211}, (Number[]) values[2]);
        assertArrayEquals(new Number[] {null, null, null,  706, null, null, null}, (Number[]) values[3]);
        assertArrayEquals(new Number[] {1200, 1201, 1202, null, 1209, 1210, 1211}, (Number[]) values[4]);
        assertArrayEquals(new Number[] {1300, 1301, 1302, null, 1309, 1310, 1311}, (Number[]) values[5]);
        assertArrayEquals(new Number[] {1400, 1401, 1402, null, 1409, 1410, 1411}, (Number[]) values[6]);
        assertEquals(7, values.length);
    }
}
