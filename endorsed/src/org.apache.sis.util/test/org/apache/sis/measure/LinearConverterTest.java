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

import java.math.BigDecimal;
import javax.measure.UnitConverter;
import org.apache.sis.math.Fraction;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link LinearConverter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class LinearConverterTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public LinearConverterTest() {
    }

    /**
     * Asserts that the given converter is a linear converter with the given scale factor and no offset.
     * The scale factor is given by the ratio of the given numerator and denominator.
     *
     * @param  numerator    the expected numerator in the conversion factor.
     * @param  denominator  the expected denominator in the conversion factor.
     * @param  converter    the converter to verify.
     */
    static void assertScale(final int numerator, final int denominator, final AbstractConverter converter) {
        final double derivative = numerator / (double) denominator;
        final Number[] coefficients = converter.coefficients();
        assertEquals(2, coefficients.length, "coefficients.length");
        assertEquals(0, coefficients[0].doubleValue(), 0, "offset");        // Δ=0 for ignoring the sign of ±0.
        assertEquals(derivative, coefficients[1].doubleValue(), "scale");
        if (denominator != 1) {
            assertInstanceOf(Fraction.class, coefficients[1], "coefficients[1]");
            final Fraction f = (Fraction) coefficients[1];
            assertEquals(numerator,   f.numerator,   "numerator");
            assertEquals(denominator, f.denominator, "denominator");
        }
        assertEquals(derivative, converter.derivative(0), "derivative");
    }

    /**
     * Tests {@link LinearConverter#create(Number, Number)}.
     */
    @Test
    public void testCreate() {
        assertTrue(LinearConverter.create(null, null).isIdentity());
        assertScale(3, 1, LinearConverter.create(3, 0));
        assertScale(3, 2, LinearConverter.create(new Fraction(3, 2), null));
    }

    /**
     * Tests {@link LinearConverter#pow(UnitConverter, int, boolean)}.
     */
    @Test
    public void testPow() {
        LinearConverter c = LinearConverter.scale(10, 3);
        assertScale( 100,  9, LinearConverter.pow(c, 2, false));
        assertScale(1000, 27, LinearConverter.pow(c, 3, false));

        c = LinearConverter.scale(1000, 27);
        assertScale(10, 3, LinearConverter.pow(c, 3, true));
    }

    /**
     * Tests the {@link LinearConverter#isIdentity()} and {@link LinearConverter#isLinear()} methods.
     * This also indirectly test the {@link LinearConverter#offset(double, double)} and
     * {@link LinearConverter#coefficients()} methods.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testIsIdentityAndLinear() {
        AbstractConverter c = IdentityConverter.INSTANCE;
        assertTrue(c.isIdentity());
        assertTrue(c.isLinear());
        assertEquals(0, c.coefficients().length);

        c = LinearConverter.scale(100, 100);
        assertTrue(c.isIdentity());
        assertTrue(c.isLinear());
        assertEquals(0, c.coefficients().length);

        c = LinearConverter.scale(254, 100);
        assertFalse(c.isIdentity());
        assertTrue (c.isLinear());
        assertScale(254, 100, c);
    }

    /**
     * Tests {@link LinearConverter#convert(double)}. This method tests also the pertinence of
     * representing the conversion factor by a ratio instead of a single {@code double} value.
     */
    @Test
    public void testConvertDouble() {
        LinearConverter c = LinearConverter.scale(254, 100);            // inches to centimetres
        assertEquals(1143, c.convert(450));
        /*
         * Below is an example of case giving a different result depending on whether we use the straightforward
         * y = (x⋅scale + offset)  equation or the longer  y = (x⋅scale + offset) ∕ divisor  equation.  This test
         * uses the US survey foot, which is defined as exactly 1200∕3937 metres.  That conversion factor has no
         * exact representation in the 'double' type. Converting 200 metres to US survey foot gives 656.1666666…
         * Converting that value back to US survey foot with (x⋅scale + offset) gives 199.99999999999997 metres,
         * but the longer equation used by UnitConverter gives exactly 200 metres as expected.
         *
         * Reminder: we usually don't care much about those rounding errors as they are unavoidable when doing
         * floating point arithmetic and the scale factor are usually no more accurate in base 10 than base 2.
         * But unit conversions are special cases since the conversion factors are exact in base 10 by definition.
         */
        c = LinearConverter.scale(1200, 3937);                  // US survey feet to metres
        assertEquals(200, c.convert(656.16666666666667));       // Really want exact comparison; see above comment
        /*
         * Test conversion from degrees Celsius to Kelvin. The straightforward equation gives 300.15999999999997
         * while the longer equation used by UnitConverter gives 300.16 as expected.
         */
        c = LinearConverter.offset(27315, 100);                 // Celsius to kelvin
        assertEquals(300.16, c.convert(27.01));                 // Really want exact comparison; see above comment
    }

    /**
     * Tests {@link LinearConverter#convert(Number)} with a value of type {@link Float}.
     * This method indirectly tests {@link org.apache.sis.math.DecimalFunctions#floatToDouble(float)}.
     */
    @Test
    public void testConvertFloat() {
        LinearConverter c = LinearConverter.offset(27315, 100);
        final Number n = c.convert(Float.valueOf(27.01f));
        assertInstanceOf(Double.class, n);
        assertEquals(300.16, n.doubleValue());          // Really want exact comparison; see testConvertDouble()
    }

    /**
     * Tests {@link LinearConverter#convert(Number)} with a value of type {@link BigDecimal}.
     */
    @Test
    public void testConvertBigDecimal() {
        LinearConverter c = LinearConverter.offset(27315, 100);
        final Number n = c.convert(new BigDecimal("27.01"));
        assertInstanceOf(BigDecimal.class, n);
        assertEquals(new BigDecimal("300.16"), n);
    }

    /**
     * Tests {@link LinearConverter#inverse()}.
     */
    @Test
    public void testInverse() {
        LinearConverter c = LinearConverter.scale(254, 100);
        LinearConverter inv = (LinearConverter) c.inverse();
        assertScale(254, 100, c);
        assertScale(100, 254, inv);
        assertEquals(12.3, c.convert(inv.convert(12.3)));
        /*
         * Following is an example of case where our effort regarding preserving accuracy in base 10 does not work.
         * However, the concatenation of those two UnitConverter gives the identity converter, as expected.
         * That concatenation is not verified here because it is not the purpose of this test case.
         */
        c = LinearConverter.offset(27315, 100);
        inv = (LinearConverter) c.inverse();
        assertEquals(12.3, c.convert(inv.convert(12.3)), 1E-13);
    }

    /**
     * Tests {@link LinearConverter#concatenate(UnitConverter)}.
     */
    @Test
    public void testConcatenate() {
        AbstractConverter c = LinearConverter.scale(254, 100);                      // inches to centimetres
        assertScale(254, 100, c);
        c = (AbstractConverter) c.concatenate(LinearConverter.scale(10, 1));        // centimetres to millimetres
        assertScale(254, 10, c);
        c = (AbstractConverter) c.concatenate(LinearConverter.scale(1, 1000));      // millimetres to metres
        assertScale(254, 10000, c);

        c = LinearConverter.offset(27315, 100);                                     // Celsius to kelvin
        c = (AbstractConverter) c.concatenate(LinearConverter.offset(-54630, 200));
        assertTrue(c.isIdentity());
    }

    /**
     * Tests {@link LinearConverter#equals(Object)} and {@link LinearConverter#hashCode()}.
     */
    @Test
    public void testEquals() {
        final LinearConverter c1 = LinearConverter.scale(254, 100);
        final LinearConverter c2 = LinearConverter.scale( 25, 100);
        final LinearConverter c3 = LinearConverter.scale(254, 100);
        assertFalse(c1.equals(c2));
        assertTrue (c1.equals(c3));
        assertFalse(c2.equals(c3));
        assertNotEquals(c1.hashCode(), c2.hashCode());
        assertEquals   (c1.hashCode(), c3.hashCode());
        assertNotEquals(c2.hashCode(), c3.hashCode());
    }

    /**
     * Tests serialization of a {@link UnitConverter}.
     */
    @Test
    public void testSerialization() {
        LinearConverter c = LinearConverter.scale(254, 100);
        assertNotSame(c, assertSerializedEquals(c));
    }

    /**
     * Tests {@link LinearConverter#toString()}, mostly for debugging purpose.
     */
    @Test
    public void testToString() {
        assertEquals("y = x",                   IdentityConverter.INSTANCE        .toString());
        assertEquals("y = 100⋅x",               LinearConverter.scale (  100,   1).toString());
        assertEquals("y = x∕100",               LinearConverter.scale (    1, 100).toString());
        assertEquals("y = 254⋅x∕100",           LinearConverter.scale (  254, 100).toString());
        assertEquals("y = (100⋅x + 27315)∕100", LinearConverter.offset(27315, 100).toString());
    }
}
