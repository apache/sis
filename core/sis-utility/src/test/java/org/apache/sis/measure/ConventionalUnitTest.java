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
import javax.measure.Unit;
import javax.measure.UnitConverter;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link ConventionalUnit} class. This class tests also the {@link SystemUnit#multiply(double)} and
 * {@link SystemUnit#divide(double)} methods since they are used for creating {@code ConventionalUnit} instances,
 * but those methods just delegate to {@link ConventionalUnit#create(SystemUnit, UnitConverter)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn({SystemUnitTest.class, LinearConverterTest.class})
public final strictfp class ConventionalUnitTest extends TestCase {
    /**
     * Verifies the properties if the given unit.
     *
     * @param  system  the expected system unit.
     * @param  unit    the conventional unit to verify.
     * @param  symbol  the expected symbol.
     * @param  scale   the expected scale factor.
     */
    static void verify(final Unit<?> system, final Unit<?> unit, final String symbol, final double scale) {
        assertSame  ("getSystemUnit()", system, unit.getSystemUnit());
        assertEquals("getSymbol()",     symbol, unit.getSymbol());
        assertEquals("UnitConverter",   scale,  Units.toStandardUnit(unit), STRICT);
    }

    /**
     * Verifies some of the hard-coded constants defined in the {@link Units} class.
     */
    @Test
    public void verifyConstants() {
        verify(Units.METRE,             Units.NANOMETRE,             "nm",  1E-9);
        verify(Units.METRE,             Units.MILLIMETRE,            "mm",  1E-3);
        verify(Units.METRE,             Units.CENTIMETRE,            "cm",  1E-2);
        verify(Units.METRE,             Units.METRE,                  "m",  1E+0);
        verify(Units.METRE,             Units.KILOMETRE,             "km",  1E+3);
        verify(Units.METRE,             Units.NAUTICAL_MILE,          "M",  1852);
        verify(Units.SECOND,            Units.SECOND,                 "s",     1);
        verify(Units.SECOND,            Units.MINUTE,               "min",    60);
        verify(Units.SECOND,            Units.HOUR,                   "h",  3600);
        verify(Units.PASCAL,            Units.PASCAL,                "Pa",     1);
        verify(Units.PASCAL,            Units.HECTOPASCAL,          "hPa",   100);
        verify(Units.METRES_PER_SECOND, Units.KILOMETRES_PER_HOUR, "km∕h",  0.06);
        verify(Units.KILOGRAM,          Units.KILOGRAM,              "kg",     1);
        verify(Units.UNITY,             Units.UNITY,                   "",     1);
        verify(Units.UNITY,             Units.PERCENT,                "%",  1E-2);
        verify(Units.UNITY,             Units.PPM,                  "ppm",  1E-6);
    }

    /**
     * Tests {@link ConventionalUnit#prefix(double)}.
     */
    @Test
    public void testPrefix() {
        assertEquals( 0 , ConventionalUnit.prefix(1E-27));
        assertEquals( 0 , ConventionalUnit.prefix(1E-25));
        assertEquals('y', ConventionalUnit.prefix(1E-24));
        assertEquals( 0 , ConventionalUnit.prefix(1E-23));
        assertEquals('n', ConventionalUnit.prefix(1E-09));
        assertEquals( 0 , ConventionalUnit.prefix(1E-08));
        assertEquals( 0 , ConventionalUnit.prefix(1E-04));
        assertEquals('m', ConventionalUnit.prefix(1E-03));
        assertEquals('c', ConventionalUnit.prefix(1E-02));
        assertEquals('d', ConventionalUnit.prefix(1E-01));
        assertEquals( 0 , ConventionalUnit.prefix(    1));
        assertEquals( 0 , ConventionalUnit.prefix(    0));
        assertEquals( 0 , ConventionalUnit.prefix(  -10));
        assertEquals('㍲', ConventionalUnit.prefix(   10));
        assertEquals('h', ConventionalUnit.prefix(  100));
        assertEquals('k', ConventionalUnit.prefix( 1000));
        assertEquals( 0 , ConventionalUnit.prefix(1E+04));
        assertEquals('G', ConventionalUnit.prefix(1E+09));
        assertEquals('Y', ConventionalUnit.prefix(1E+24));
        assertEquals( 0 , ConventionalUnit.prefix(1E+25));
        assertEquals( 0 , ConventionalUnit.prefix(1E+27));
        assertEquals( 0 , ConventionalUnit.prefix(1E+25));
    }

    /**
     * Tests {@link ConventionalUnit#power(String)}.
     */
    @Test
    public void testPower() {
        assertEquals("m",    1, ConventionalUnit.power("m"));
        assertEquals("m²",   2, ConventionalUnit.power("m²"));
        assertEquals("m2",   2, ConventionalUnit.power("m2"));
        assertEquals("m₂",   1, ConventionalUnit.power("m₂"));      // Because the "2" is in indice.
        assertEquals("m³",   3, ConventionalUnit.power("m³"));
        assertEquals("m/s²", 1, ConventionalUnit.power("m/s²"));
        assertEquals("km/h", 1, ConventionalUnit.power("km/h"));
        assertEquals("m³/s", 3, ConventionalUnit.power("m³/s"));
        assertEquals("m³s",  0, ConventionalUnit.power("m³s"));     // Illegal symbol.
    }

    /**
     * Ensures that the characters in the {@link ConventionalUnit#PREFIXES} array match
     * the prefixes recognized by {@link LinearConverter#forPrefix(char)}.
     *
     * @throws ReflectiveOperationException if this test can not access the private fields of {@link LinearConverter}.
     *
     * @see LinearConverterTest#verifyPrefixes()
     */
    @Test
    @DependsOnMethod("testPrefix")
    public void verifyPrefixes() throws ReflectiveOperationException {
        Field f = ConventionalUnit.class.getDeclaredField("PREFIXES");
        f.setAccessible(true);
        double previousScale = StrictMath.pow(1000, -(ConventionalUnit.MAX_POWER + 1));
        for (final char prefix : (char[]) f.get(null)) {
            final LinearConverter lc = LinearConverter.forPrefix(prefix);
            final String asString = String.valueOf(prefix);
            assertNotNull(asString, lc);
            /*
             * Ratio of previous scale with current scale shall be a power of 10.
             */
            final double scale = lc.derivative(0);
            final double power = StrictMath.log10(scale / previousScale);
            assertTrue  (asString,    power >= 1);
            assertEquals(asString, 0, power % 1, STRICT);
            /*
             * At this point we got the LinearConverter to use for the test,
             * and we know the expected prefix. Verify that we get that value.
             */
            assertEquals("ConventionalUnit.prefix(double)", asString, String.valueOf(ConventionalUnit.prefix(scale)));
            previousScale = scale;
        }
    }

    /**
     * Tests {@link SystemUnit#multiply(double)} and {@link SystemUnit#divide(double)}.
     * Both are implemented by calls to {@link SystemUnit#transform(UnitConverter)}.
     */
    @Test
    @DependsOnMethod({"verifyPrefixes","testPower"})
    public void testTransformSystemUnit() {
        assertSame(Units.METRE,      Units.METRE.multiply(   1));
        assertSame(Units.KILOMETRE,  Units.METRE.multiply(1000));
        assertSame(Units.MILLIMETRE, Units.METRE.divide  (1000));
        assertSame(Units.CENTIMETRE, Units.METRE.divide  ( 100));
        assertSame(Units.NANOMETRE,  Units.METRE.divide  (1E+9));
        assertSame(Units.NANOMETRE,  Units.METRE.multiply(1E-9));
        verify    (Units.METRE,      Units.METRE.multiply( 100), "hm", 100);
        verify    (Units.METRE,      Units.METRE.multiply(  10), "dam", 10);

        assertSame(Units.METRES_PER_SECOND, Units.METRES_PER_SECOND.multiply(   1));
        verify    (Units.METRES_PER_SECOND, Units.METRES_PER_SECOND.multiply(1000), "km∕s", 1E+3);
        verify    (Units.METRES_PER_SECOND, Units.METRES_PER_SECOND.divide  (1000), "mm∕s", 1E-3);

        assertSame(Units.SQUARE_METRE, Units.SQUARE_METRE.multiply(   1));
        assertSame(Units.HECTARE,      Units.SQUARE_METRE.multiply(1E+4));
        assertSame(Units.CUBIC_METRE,  Units.CUBIC_METRE .multiply(   1));
        verify    (Units.CUBIC_METRE,  Units.CUBIC_METRE .multiply(1E+9), "km³", 1E+9);
        verify    (Units.CUBIC_METRE,  Units.CUBIC_METRE .divide  (1E+9), "mm³", 1E-9);

        assertSame(Units.HOUR,        Units.SECOND.multiply(3600));
        assertSame(Units.DEGREE,      Units.RADIAN.multiply(Math.PI/180));
        assertSame(Units.GRAD,        Units.RADIAN.multiply(Math.PI/200));
        assertSame(Units.ARC_SECOND,  Units.RADIAN.multiply(Math.PI / (180*60*60)));
        assertSame(Units.MICRORADIAN, Units.RADIAN.divide(1E6));
    }

    /**
     * Tests {@link ConventionalUnit#multiply(double)} and {@link ConventionalUnit#divide(double)}.
     * Both are implemented by calls to {@link ConventionalUnit#transform(UnitConverter)}.
     */
    @Test
    @DependsOnMethod("testTransformSystemUnit")
    public void testTransformConventionalUnit() {
        assertSame(Units.MILLIMETRE, Units.MILLIMETRE.multiply(   1));
        assertSame(Units.CENTIMETRE, Units.MILLIMETRE.multiply(  10));
        assertSame(Units.MILLIMETRE, Units.CENTIMETRE.divide  (  10));
        assertSame(Units.MILLIMETRE, Units.CENTIMETRE.multiply( 0.1));
        assertSame(Units.KILOMETRE,  Units.MILLIMETRE.multiply(1E+6));
        assertSame(Units.NANOMETRE,  Units.KILOMETRE .divide  (1E+12));
        assertSame(Units.NANOMETRE,  Units.KILOMETRE .multiply(1E-12));

        verify(Units.SQUARE_METRE, Units.HECTARE.divide(1E+10), "mm²", 1E-6);
    }

    /**
     * Tests {@link ConventionalUnit#isCompatible(Unit)}.
     */
    @Test
    public void testIsCompatible() {
        assertTrue (Units.KILOMETRE.isCompatible(Units.METRE));
        assertFalse(Units.KILOMETRE.isCompatible(Units.SECOND));
        assertTrue (Units.DEGREE   .isCompatible(Units.GRAD));
        assertTrue (Units.DEGREE   .isCompatible(Units.PPM));       // Because those units are dimensionless.
    }

    /**
     * Tests conversion of an angular value between two conventional units.
     * The use of angular units is of special interest because of rounding errors.
     */
    @Test
    public void testConvertAngle() {
        final UnitConverter c = Units.GRAD.getConverterTo(Units.DEGREE);
        assertEquals(180,        c.convert(200),       STRICT);
        assertEquals(2.33722917, c.convert(2.5969213), STRICT);
    }

    /**
     * Tests conversion of a temperature value between two conventional units.
     */
    @Test
    public void testConvertTemperature() {
        final UnitConverter c = Units.FAHRENHEIT.getConverterTo(Units.CELSIUS);
        assertEquals("50°F",  10, c.convert(50),          STRICT);
        assertEquals("5°F",  -15, c.convert(5),           STRICT);
        assertEquals("0°C",   32, c.inverse().convert(0), STRICT);
    }

    /**
     * Serializes some units, deserializes them and verifies that we get the same instance.
     */
    @Test
    public void testSerialization() {
        assertSame(Units.KILOMETRE, assertSerializedEquals(Units.KILOMETRE));
        assertSame(Units.HECTARE,   assertSerializedEquals(Units.HECTARE));
    }
}
