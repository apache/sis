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

import javax.measure.IncommensurableException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Power;
import javax.measure.quantity.Volume;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link ConventionalUnit} class. This class tests also the {@link SystemUnit#multiply(double)} and
 * {@link SystemUnit#divide(double)} methods since they are used for creating {@code ConventionalUnit} instances,
 * but those methods just delegate to {@link ConventionalUnit#create(AbstractUnit, UnitConverter)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ConventionalUnitTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ConventionalUnitTest() {
    }

    /**
     * Verifies the properties in the given unit.
     *
     * @param  system  the expected system unit.
     * @param  unit    the conventional unit to verify.
     * @param  symbol  the expected symbol.
     * @param  scale   the expected scale factor.
     */
    static void verify(final Unit<?> system, final Unit<?> unit, final String symbol, final double scale) {
        assertSame  (system, unit.getSystemUnit());
        assertEquals(symbol, unit.getSymbol());
        assertEquals(scale,  Units.toStandardUnit(unit));
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
        verify(Units.METRES_PER_SECOND, Units.KILOMETRES_PER_HOUR, "km∕h",  1/3.6);
        verify(Units.CUBIC_METRE,       Units.LITRE,                  "L",  1E-3);
        verify(Units.KILOGRAM,          Units.KILOGRAM,              "kg",     1);
        verify(Units.KILOGRAM,          Units.GRAM,                   "g",  1E-3);
        verify(Units.UNITY,             Units.UNITY,                   "",     1);
        verify(Units.UNITY,             Units.PERCENT,                "%",  1E-2);
        verify(Units.UNITY,             Units.PPM,                  "ppm",  1E-6);
    }

    /**
     * Tests {@link ConventionalUnit#power(String)}.
     */
    @Test
    public void testPower() {
        assertEquals(1, ConventionalUnit.power("m"));
        assertEquals(2, ConventionalUnit.power("m²"));
        assertEquals(2, ConventionalUnit.power("m2"));
        assertEquals(1, ConventionalUnit.power("m₂"));      // Because the "2" is in indice.
        assertEquals(3, ConventionalUnit.power("m³"));
        assertEquals(1, ConventionalUnit.power("m/s²"));
        assertEquals(1, ConventionalUnit.power("km/h"));
        assertEquals(3, ConventionalUnit.power("m³/s"));
        assertEquals(0, ConventionalUnit.power("m³s"));     // Illegal symbol.
    }

    /**
     * Tests {@link SystemUnit#multiply(double)} and {@link SystemUnit#divide(double)} on fundamental units.
     * All those methods are implemented by calls to {@link SystemUnit#transform(UnitConverter)}.
     */
    @Test
    public void testTransformFundamentalUnit() {
        assertSame(Units.METRE,      Units.METRE.multiply(   1));
        assertSame(Units.KILOMETRE,  Units.METRE.multiply(1000));
        assertSame(Units.MILLIMETRE, Units.METRE.divide  (1000));
        assertSame(Units.CENTIMETRE, Units.METRE.divide  ( 100));
        assertSame(Units.NANOMETRE,  Units.METRE.divide  (1E+9));
        assertSame(Units.NANOMETRE,  Units.METRE.multiply(1E-9));
        verify    (Units.METRE,      Units.METRE.multiply( 100), "hm", 100);
        verify    (Units.METRE,      Units.METRE.multiply(  10), "dam", 10);

        assertSame(Units.HOUR,        Units.SECOND.multiply(3600));
        assertSame(Units.DEGREE,      Units.RADIAN.multiply(StrictMath.PI/180));
        assertSame(Units.GRAD,        Units.RADIAN.multiply(StrictMath.PI/200));
        assertSame(Units.ARC_SECOND,  Units.RADIAN.multiply(StrictMath.PI / (180*60*60)));
        assertSame(Units.MICRORADIAN, Units.RADIAN.divide(1E6));
    }

    /**
     * Tests the same methods as {@link #testTransformFundamentalUnit()}, but applied on other system units than the
     * fundamental ones. All tested methods are implemented by calls to {@link SystemUnit#transform(UnitConverter)}.
     *
     * @see <a href="https://en.wikipedia.org/wiki/SI_derived_unit">Derived units on Wikipedia</a>
     */
    @Test
    public void testTransformDerivedUnit() {
        assertSame(Units.METRES_PER_SECOND, Units.METRES_PER_SECOND.multiply(   1));
        verify    (Units.METRES_PER_SECOND, Units.METRES_PER_SECOND.multiply(1000), "km∕s", 1E+3);
        verify    (Units.METRES_PER_SECOND, Units.METRES_PER_SECOND.divide  (1000), "mm∕s", 1E-3);

        assertSame(Units.SQUARE_METRE, Units.SQUARE_METRE.multiply(   1));
        assertSame(Units.HECTARE,      Units.SQUARE_METRE.multiply(1E+4));
        assertSame(Units.CUBIC_METRE,  Units.CUBIC_METRE .multiply(   1));
        verify    (Units.CUBIC_METRE,  Units.CUBIC_METRE .multiply(1E+9), "km³", 1E+9);
        verify    (Units.CUBIC_METRE,  Units.CUBIC_METRE .divide  (1E+9), "mm³", 1E-9);
    }

    /**
     * Tests {@link ConventionalUnit#multiply(double)} and {@link ConventionalUnit#divide(double)}.
     * Both are implemented by calls to {@link ConventionalUnit#transform(UnitConverter)}.
     */
    @Test
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
     * Tests operations on kilogram.
     * The management of SI prefixes need to make a special case for kilogram.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-382">SIS-382</a>
     * @see UnitFormatTest#testParseKilogram()
     */
    @Test
    public void testKilogram() {
        assertSame(Units.GRAM, Units.KILOGRAM.divide(1E+3));
        verify(Units.KILOGRAM, Units.KILOGRAM.divide(1E+6), "mg", 1E-6);

        Unit<?> unit = Units.KILOGRAM.divide(1E+9);
        assertEquals("µg", unit.getSymbol());
        unit = unit.divide(Units.METRE);
        assertEquals("µg∕m", unit.getSymbol());
        unit = unit.multiply(1E+3);
        assertEquals("mg∕m", unit.getSymbol());
    }

    /**
     * Tests operations on Celsius units.
     */
    @Test
    public void testCelsius() {
        assertSame(Units.CELSIUS, Units.KELVIN.shift(+273.15));
        assertSame(Units.KELVIN, Units.CELSIUS.shift(-273.15));
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
        assertEquals(180,        c.convert(200));
        assertEquals(2.33722917, c.convert(2.5969213));
    }

    /**
     * Tests conversion of a temperature value between two conventional units.
     */
    @Test
    public void testConvertTemperature() {
        final UnitConverter c = Units.FAHRENHEIT.getConverterTo(Units.CELSIUS);
        assertEquals( 10, c.convert(50),          "50°F");
        assertEquals(-15, c.convert(5),           "5°F");
        assertEquals( 32, c.inverse().convert(0), "0°C");
    }

    /**
     * Verifies that the given units derived from litres ({@code u1}) is equivalent to the given units derived
     * from cubic metres ({@code u2}). The conversion between those two units is expected to be identity.
     */
    private static void assertEquivalent(final String s1, final Unit<Volume> u1,
                                         final String s2, final Unit<Volume> u2)
            throws IncommensurableException
    {
        assertEquals(s1, u1.getSymbol());
        assertEquals(s2, u2.getSymbol());
        assertTrue(u1.getConverterTo(u2).isIdentity());
        assertTrue(u2.getConverterTo(u1).isIdentity());
        assertTrue(u1.getConverterToAny(u2).isIdentity());
        assertTrue(u2.getConverterToAny(u1).isIdentity());
    }

    /**
     * Tests the equivalence between litres and cubic metres.
     * The litre unit is handled as a special case, since it is not a SI unit but can have SI prefix.
     *
     * @throws IncommensurableException if {@link Unit#getConverterToAny(Unit)} failed.
     */
    @Test
    public void testVolumeEquivalences() throws IncommensurableException {
        assertEquivalent(  "L", Units.LITRE.divide  (1E+00),  "dm³", Units.CUBIC_METRE.divide  (1E+03));
        assertEquivalent( "mL", Units.LITRE.divide  (1E+03),  "cm³", Units.CUBIC_METRE.divide  (1E+06));
        assertEquivalent( "µL", Units.LITRE.divide  (1E+06),  "mm³", Units.CUBIC_METRE.divide  (1E+09));
        assertEquivalent( "fL", Units.LITRE.divide  (1E+15),  "µm³", Units.CUBIC_METRE.divide  (1E+18));
        assertEquivalent( "yL", Units.LITRE.divide  (1E+24),  "nm³", Units.CUBIC_METRE.divide  (1E+27));
        assertEquivalent( "kL", Units.LITRE.multiply(1E+03),   "m³", Units.CUBIC_METRE.divide  (1E+00));
        assertEquivalent( "ML", Units.LITRE.multiply(1E+06), "dam³", Units.CUBIC_METRE.multiply(1E+03));
        assertEquivalent( "GL", Units.LITRE.multiply(1E+09),  "hm³", Units.CUBIC_METRE.multiply(1E+06));
        assertEquivalent( "TL", Units.LITRE.multiply(1E+12),  "km³", Units.CUBIC_METRE.multiply(1E+09));
        assertEquivalent( "ZL", Units.LITRE.multiply(1E+21),  "Mm³", Units.CUBIC_METRE.multiply(1E+18));
        assertEquals    ( "dL", Units.LITRE.divide  (1E+01).getSymbol());
        assertEquals    ( "cL", Units.LITRE.divide  (1E+02).getSymbol());
        assertEquals    ( "nL", Units.LITRE.divide  (1E+09).getSymbol());
        assertEquals    ( "pL", Units.LITRE.divide  (1E+12).getSymbol());
        assertEquals    ( "aL", Units.LITRE.divide  (1E+18).getSymbol());
        assertEquals    ( "zL", Units.LITRE.divide  (1E+21).getSymbol());
        assertEquals    ("daL", Units.LITRE.multiply(1E+01).getSymbol());
        assertEquals    ( "hL", Units.LITRE.multiply(1E+02).getSymbol());
        assertEquals    ( "PL", Units.LITRE.multiply(1E+15).getSymbol());
        assertEquals    ( "EL", Units.LITRE.multiply(1E+18).getSymbol());
        assertEquals    ( "YL", Units.LITRE.multiply(1E+24).getSymbol());
    }

    /**
     * Tests conversion between litres and cubic metres.
     */
    @Test
    public void testVolumeConversions() {
        final Unit<Volume>  l  = Units.LITRE;
        final Unit<Volume> cl  = Units.LITRE.divide(100);
        final Unit<Volume> ml  = Units.LITRE.divide(1000);
        final Unit<Volume> cm3 = Units.CUBIC_METRE.divide(1E+06);
        assertEquals(4000,  l.getConverterTo(ml) .convert(4), "4 L to ml");
        assertEquals(40, cl.getConverterTo(cm3).convert(4), "4 cL to cm³");
    }

    /**
     * Tests the creation of a unit of measurement defined by a logarithm.
     *
     * @see <a href="https://en.wikipedia.org/wiki/DBm">Decibel-milliwatts on Wikipedia</a>
     */
    @Test
    public void testDecibelWatt() {
        final Unit<Power> dBm = Units.logarithm(Units.WATT.divide(1000)).divide(10);
        final UnitConverter c = dBm.getConverterTo(Units.WATT);
        assertEquals(100000, c.convert(80));
        assertEquals(  1000, c.convert(60));
        assertEquals(    10, c.convert(40));
        assertEquals(     1, c.convert(30));
        assertEquals(0.3162, c.convert(25), 0.0001);
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
