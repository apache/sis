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
package org.apache.sis.measure;

import java.lang.reflect.Field;
import org.apache.sis.util.ArraysExt;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link Prefixes} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class PrefixesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public PrefixesTest() {
    }

    /**
     * Ensures that the characters in the {@link Prefixes#PREFIXES} array are in strictly increasing order,
     * and that {@link Prefixes#POWERS} has the same length.
     * Those two arrays form a map used by {@link Prefixes#converter(char)}.
     *
     * @throws ReflectiveOperationException if this test cannot access the private fields of {@link LinearConverter}.
     */
    @Test
    public void verifyConverterMap() throws ReflectiveOperationException {
        Field f = Prefixes.class.getDeclaredField("PREFIXES");
        f.setAccessible(true);
        final char[] prefixes = (char[]) f.get(null);
        assertTrue(ArraysExt.isSorted(prefixes, true));

        f = Prefixes.class.getDeclaredField("POWERS");
        f.setAccessible(true);
        assertEquals(prefixes.length, ((byte[]) f.get(null)).length);
    }

    /**
     * Ensures that the characters in the {@link Prefixes#ENUM} array match
     * the prefixes recognized by {@link Prefixes#converter(char)}.
     * This array forms a list used by {@link Prefixes#converter(char)}.
     *
     * @throws ReflectiveOperationException if this test cannot access the private fields of {@link LinearConverter}.
     */
    @Test
    public void verifySymbolList() throws ReflectiveOperationException {
        Field f = Prefixes.class.getDeclaredField("ENUM");
        f.setAccessible(true);
        double previousScale = StrictMath.pow(1000, -(Prefixes.MAX_POWER + 1));
        for (final char prefix : (char[]) f.get(null)) {
            final LinearConverter lc = Prefixes.converter(prefix);
            final String asString = String.valueOf(prefix);
            assertNotNull(lc, asString);
            /*
             * Ratio of previous scale with current scale shall be a power of 10.
             */
            final double scale = lc.derivative(0);
            final double power = StrictMath.log10(scale / previousScale);
            assertTrue(power >= 1, asString);
            assertEquals(0, power % 1, asString);
            /*
             * At this point we got the LinearConverter to use for the test,
             * and we know the expected prefix. Verify that we get that value.
             */
            assertEquals(asString, String.valueOf(Prefixes.symbol(scale, 1)));
            previousScale = scale;
        }
    }

    /**
     * Tests the {@link Prefixes#converter(char)} method. This also indirectly tests the
     * {@link LinearConverter#scale(double, double)} and {@link LinearConverter#coefficients()}
     * methods.
     */
    @Test
    public void testConverter() {
        LinearConverterTest.assertScale(1000000,    1, Prefixes.converter('M'));
        LinearConverterTest.assertScale(   1000,    1, Prefixes.converter('k'));
        LinearConverterTest.assertScale(      1,  100, Prefixes.converter('c'));
        LinearConverterTest.assertScale(      1, 1000, Prefixes.converter('m'));
    }

    /**
     * Tests {@link Prefixes#symbol(double, int)}.
     */
    @Test
    public void testSymbol() {
        assertEquals( 0 , Prefixes.symbol(1E-27, 1));
        assertEquals( 0 , Prefixes.symbol(1E-25, 1));
        assertEquals('y', Prefixes.symbol(1E-24, 1));
        assertEquals( 0 , Prefixes.symbol(1E-23, 1));
        assertEquals('n', Prefixes.symbol(1E-09, 1));
        assertEquals( 0 , Prefixes.symbol(1E-08, 1));
        assertEquals( 0 , Prefixes.symbol(1E-04, 1));
        assertEquals('m', Prefixes.symbol(1E-03, 1));
        assertEquals('c', Prefixes.symbol(1E-02, 1));
        assertEquals('d', Prefixes.symbol(1E-01, 1));
        assertEquals( 0 , Prefixes.symbol(    1, 1));
        assertEquals( 0 , Prefixes.symbol(    0, 1));
        assertEquals( 0 , Prefixes.symbol(  -10, 1));
        assertEquals('㍲', Prefixes.symbol(   10, 1));
        assertEquals('h', Prefixes.symbol(  100, 1));
        assertEquals('k', Prefixes.symbol( 1000, 1));
        assertEquals( 0 , Prefixes.symbol(1E+04, 1));
        assertEquals('G', Prefixes.symbol(1E+09, 1));
        assertEquals('Y', Prefixes.symbol(1E+24, 1));
        assertEquals( 0 , Prefixes.symbol(1E+25, 1));
        assertEquals( 0 , Prefixes.symbol(1E+27, 1));
        assertEquals( 0 , Prefixes.symbol(1E+25, 1));
    }

    /**
     * Tests {@link Prefixes#getUnit(String)}.
     */
    @Test
    public void testGetUnit() {
        assertEquals(1,    Units.toStandardUnit(Prefixes.getUnit( "m" )),  "m" );
        assertEquals(1,    Units.toStandardUnit(Prefixes.getUnit( "m²")),  "m²");
        assertEquals(1E+3, Units.toStandardUnit(Prefixes.getUnit("km" )), "km");
        assertEquals(1E+6, Units.toStandardUnit(Prefixes.getUnit("km²")), "km²");
    }
}
