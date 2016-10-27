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

import java.util.Set;
import java.util.HashSet;
import java.util.Locale;
import java.lang.reflect.Field;
import javax.measure.Unit;
import javax.measure.format.ParserException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link UnitFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn({SystemUnitTest.class, ConventionalUnitTest.class})
public final strictfp class UnitFormatTest extends TestCase {
    /**
     * Verifies all constants defined in {@link Units} class. This method verifies:
     *
     * <ul>
     *   <li>The string representation of the dimension, which indirectly tests {@link UnitFormat}
     *       since {@link UnitDimension} delegates to to {@code UnitFormat}.</li>
     *   <li>The unit symbol as given by {@link UnitFormat#format(Object)} using the system-wide instance.</li>
     * </ul>
     */
    @Test
    public void verifyUnitConstants() {
        final Set<String> declared = new HashSet<>(64);
        for (final Field f : Units.class.getFields()) {
            if (Unit.class.isAssignableFrom(f.getType())) {
                declared.add(f.getName());
            }
        }
        verify(declared, "NANOMETRE",           "L",        "nm",    Units.NANOMETRE);
        verify(declared, "MILLIMETRE",          "L",        "mm",    Units.MILLIMETRE);
        verify(declared, "CENTIMETRE",          "L",        "cm",    Units.CENTIMETRE);
        verify(declared, "METRE",               "L",        "m",     Units.METRE);
        verify(declared, "KILOMETRE",           "L",        "km",    Units.KILOMETRE);
        verify(declared, "NAUTICAL_MILE",       "L",        "M",     Units.NAUTICAL_MILE);
        verify(declared, "STATUTE_MILE",        "L",        "mi",    Units.STATUTE_MILE);
        verify(declared, "US_SURVEY_FOOT",      "L",        "ft_US", Units.US_SURVEY_FOOT);
        verify(declared, "FOOT",                "L",        "ft",    Units.FOOT);
        verify(declared, "INCH",                "L",        "in",    Units.INCH);
        verify(declared, "POINT",               "L",        "pt",    Units.POINT);
        verify(declared, "RADIAN",              "",         "rad",   Units.RADIAN);
        verify(declared, "GRAD",                "",         "grad",  Units.GRAD);
        verify(declared, "DEGREE",              "",         "°",     Units.DEGREE);
        verify(declared, "ARC_MINUTE",          "",         "′",     Units.ARC_MINUTE);
        verify(declared, "ARC_SECOND",          "",         "″",     Units.ARC_SECOND);
        verify(declared, "MICRORADIAN",         "",         "µrad",  Units.MICRORADIAN);
        verify(declared, "MILLISECOND",         "T",        "ms",    Units.MILLISECOND);
        verify(declared, "SECOND",              "T",        "s",     Units.SECOND);
        verify(declared, "MINUTE",              "T",        "min",   Units.MINUTE);
        verify(declared, "HOUR",                "T",        "h",     Units.HOUR);
        verify(declared, "DAY",                 "T",        "d",     Units.DAY);
        verify(declared, "WEEK",                "T",        "wk",    Units.WEEK);
        verify(declared, "TROPICAL_YEAR",       "T",        "a",     Units.TROPICAL_YEAR);
        verify(declared, "HERTZ",               "∕T",       "Hz",    Units.HERTZ);
        verify(declared, "PASCAL",              "M∕(L⋅T²)", "Pa",    Units.PASCAL);
        verify(declared, "HECTOPASCAL",         "M∕(L⋅T²)", "hPa",   Units.HECTOPASCAL);
        verify(declared, "HECTARE",             "L²",       "ha",    Units.HECTARE);
        verify(declared, "SQUARE_METRE",        "L²",       "m²",    Units.SQUARE_METRE);
        verify(declared, "CUBIC_METRE",         "L³",       "m³",    Units.CUBIC_METRE);
        verify(declared, "METRES_PER_SECOND",   "L∕T",      "m∕s",   Units.METRES_PER_SECOND);
        verify(declared, "KILOMETRES_PER_HOUR", "L∕T",      "km∕h",  Units.KILOMETRES_PER_HOUR);
        verify(declared, "KILOGRAM",            "M",        "kg",    Units.KILOGRAM);
        verify(declared, "AMPERE",              "I",        "A",     Units.AMPERE);
        verify(declared, "NEWTON",              "M⋅L∕T²",   "N",     Units.NEWTON);
        verify(declared, "JOULE",               "M⋅L²∕T²",  "J",     Units.JOULE);
        verify(declared, "WATT",                "M⋅L²∕T³",  "W",     Units.WATT);
        verify(declared, "KELVIN",              "Θ",        "K",     Units.KELVIN);
        verify(declared, "CELSIUS",             "Θ",        "°C",    Units.CELSIUS);
        verify(declared, "CANDELA",             "J",        "cd",    Units.CANDELA);
        verify(declared, "MOLE",                "N",        "mol",   Units.MOLE);
        verify(declared, "UNITY",               "",         "",      Units.UNITY);
        verify(declared, "PERCENT",             "",         "%",     Units.PERCENT);
        verify(declared, "PPM",                 "",         "ppm",   Units.PPM);
        verify(declared, "PSU",                 "",         "psu",   Units.PSU);
        verify(declared, "PIXEL",               "",         "px",    Units.PIXEL);
        assertTrue("Missing units in test:" + declared, declared.isEmpty());
    }

    /**
     * Verifies one of the constants declared in the {@link Unit} class.
     *
     * @param declared   a map from which to remove the {@code field} value, for verifying that we didn't forgot an element.
     * @param field      the name of the constant to be verified.
     * @param dimension  the expected string representation of the unit dimension.
     * @param symbol     the expected string representation of the unit.
     * @param unit       the unit to verify.
     */
    private static void verify(final Set<String> declared, final String field, final String dimension, final String symbol, final Unit<?> unit) {
        assertEquals(field, dimension, String.valueOf(unit.getDimension()));
        assertEquals(field, symbol,    UnitFormat.INSTANCE.format(unit));
        declared.remove(field);
    }

    /**
     * Tests the formatting of a dimension having rational powers.
     */
    @Test
    @DependsOnMethod("verifyUnitConstants")
    public void testRationalPower() {
        assertEquals("T^(5⁄2)∕(M⋅L)", UnitDimensionTest.specificDetectivity().toString());
    }

    /**
     * Tests {@link UnitFormat#label(Unit, String)}.
     */
    @Test
    public void testLabel() {
        final UnitFormat f = new UnitFormat(Locale.ENGLISH);
        f.label(Units.METRE,  "mFoo");
        f.label(Units.SECOND, "sFoo");
        assertEquals("mFoo", f.format(Units.METRE));
        assertEquals("sFoo", f.format(Units.SECOND));
        assertSame(Units.METRE,  f.parse("mFoo"));
        assertSame(Units.SECOND, f.parse("sFoo"));
        /*
         * Overwriting previous value should remove the assignment from "mFoo" to Units.METRE.
         */
        f.label(Units.METRE, "mètre");
        assertEquals("mètre", f.format(Units.METRE));
        assertEquals("sFoo",  f.format(Units.SECOND));
        assertSame(Units.METRE, f.parse("mètre"));
        try {
            f.parse("mFoo");
            fail("“mFoo” should not be assigned to unit anymore.");
        } catch (ParserException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("mFoo"));
        }
        /*
         * Verify that we can not specify invalid unit label.
         */
        try {
            f.label(Units.METRE, "m¹");
            fail("Should not accept labels ending with a digit.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("m¹"));
        }
    }

    /**
     * Tests unit formatting with {@link UnitFormat.Style#UCUM}.
     */
    @Test
    public void testFormatUCUM() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        f.setStyle(UnitFormat.Style.UCUM);
        assertEquals("m",   f.format(Units.METRE));
        assertEquals("km",  f.format(Units.KILOMETRE));
        assertEquals("s",   f.format(Units.SECOND));
        assertEquals("min", f.format(Units.MINUTE));
        assertEquals("m2",  f.format(Units.SQUARE_METRE));
        assertEquals("Cel", f.format(Units.CELSIUS));
        assertEquals("K",   f.format(Units.KELVIN));
    }

    /**
     * Tests unit formatting with {@link UnitFormat.Style#NAME}.
     */
    @Test
    public void testFormatName() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        f.setStyle(UnitFormat.Style.NAME);
        assertEquals("metre",        f.format(Units.METRE));
        assertEquals("kilometre",    f.format(Units.KILOMETRE));
        assertEquals("second",       f.format(Units.SECOND));
        assertEquals("minute",       f.format(Units.MINUTE));
        assertEquals("square metre", f.format(Units.SQUARE_METRE));
        assertEquals("Celsius",      f.format(Units.CELSIUS));          // Really upper-case "C" - this is a SI exception.

        f.setLocale(Locale.US);
        assertEquals("meter",        f.format(Units.METRE));
        assertEquals("kilometer",    f.format(Units.KILOMETRE));
        assertEquals("second",       f.format(Units.SECOND));
        assertEquals("square meter", f.format(Units.SQUARE_METRE));
        assertEquals("Celsius",      f.format(Units.CELSIUS));

        f.setLocale(Locale.FRANCE);
        assertEquals("mètre",        f.format(Units.METRE));
        assertEquals("kilomètre",    f.format(Units.KILOMETRE));
        assertEquals("seconde",      f.format(Units.SECOND));
        assertEquals("mètre carré",  f.format(Units.SQUARE_METRE));
        assertEquals("Celsius",      f.format(Units.CELSIUS));
    }

    /**
     * Tests parsing of names.
     */
    @Test
    public void testParseName() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        f.setStyle(UnitFormat.Style.NAME);                          // As a matter of principle, but actually ignored.
        assertSame(Units.METRE,         f.parse("metre"));
        assertSame(Units.METRE,         f.parse("metres"));
        assertSame(Units.METRE,         f.parse("meter"));
        assertSame(Units.METRE,         f.parse("meters"));
        assertSame(Units.KILOMETRE,     f.parse("kilometre"));
        assertSame(Units.KILOMETRE,     f.parse("kilometer"));
        assertSame(Units.KILOMETRE,     f.parse("kilometres"));
        assertSame(Units.KILOMETRE,     f.parse("kilometers"));
        assertSame(Units.SQUARE_METRE,  f.parse("square metre"));
        assertSame(Units.SQUARE_METRE,  f.parse("square_meters"));
        assertSame(Units.DEGREE,        f.parse("degree"));
        assertSame(Units.DEGREE,        f.parse("degrees"));
        assertSame(Units.DEGREE,        f.parse("decimal degrees"));
        assertSame(Units.DEGREE,        f.parse("degree north"));
        assertSame(Units.DEGREE,        f.parse("degree_east"));
        assertSame(Units.DEGREE,        f.parse("Degree West"));
        assertSame(Units.KELVIN,        f.parse("degree Kelvin"));
        assertSame(Units.CELSIUS,       f.parse("degree Celsius"));
        assertSame(Units.WATT,          f.parse("watt"));
        try {
            f.parse("degree foo");
            fail("Should not accept unknown unit.");
        } catch (ParserException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("foo"));
        }
        // Tests with localisation.
        try {
            f.parse("mètre cube");
            fail("Should not accept localized unit unless requested.");
        } catch (ParserException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("mètre cube"));
        }
        f.setLocale(Locale.FRANCE);
        assertSame(Units.CUBIC_METRE, f.parse("mètre cube"));
    }

    /**
     * Tests parsing a unit defined by a URI in OGC namespace.
     * Example: {@code "urn:ogc:def:uom:EPSG::1026"} is for metres per second.
     */
    @Test
    public void testParseEPSG() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        assertSame(Units.METRE,             f.parse("urn:ogc:def:uom:EPSG::9001"));
        assertSame(Units.METRES_PER_SECOND, f.parse("urn:ogc:def:uom:EPSG::1026"));
        assertSame(Units.METRE, f.parse("http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"));
    }

    /**
     * Tests parsing of symbols without arithmetic operations other than exponent.
     */
    @Test
    public void testParseSymbol() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        assertSame(Units.METRE,         f.parse("m"));
        assertSame(Units.UNITY,         f.parse("m⁰"));
        assertSame(Units.METRE,         f.parse("m¹"));
        assertSame(Units.SQUARE_METRE,  f.parse("m²"));
        assertSame(Units.CUBIC_METRE,   f.parse("m³"));
        assertSame(Units.UNITY,         f.parse("m-0"));
        assertSame(Units.METRE,         f.parse("m01"));
        assertSame(Units.SQUARE_METRE,  f.parse("m2"));
        assertSame(Units.CUBIC_METRE,   f.parse("m3"));
        assertSame(Units.HERTZ,         f.parse("s-1"));
    }

    /**
     * Tests parsing of symbols with SI prefix.
     * Note that the "da" prefix needs to be handled in a special way because it is the only two-letters long prefix.
     */
    @Test
    @DependsOnMethod("testParseSymbol")
    public void testParsePrefix() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        ConventionalUnitTest.verify(Units.JOULE,  f.parse("kJ"),   "kJ",  1E+3);
        ConventionalUnitTest.verify(Units.HERTZ,  f.parse("MHz"),  "MHz", 1E+6);
        ConventionalUnitTest.verify(Units.PASCAL, f.parse("daPa"), "daPa",  10);
        /*
         * Verify that prefix are not accepted for conventional units. It would either be illegal prefix duplication
         * (for example we should not accept "kkm" as if it was "k" + "km") or confusing (for example "a" stands for
         * the tropical year, "ha" could be understood as 100 tropical years but is actually used for hectare).
         */
        assertSame(Units.TROPICAL_YEAR, f.parse("a"));
        try {
            f.parse("ka");
            fail("Should not accept prefix in ConventionalUnit.");
        } catch (ParserException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("ka"));
        }
    }

    /**
     * Tests parsing of symbols composed of terms combined by arithmetic operations (e.g. "m/s").
     */
    @Test
    @DependsOnMethod("testParsePrefix")
    public void testParseTerms() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        assertSame(Units.SQUARE_METRE,      f.parse("m⋅m"));
        assertSame(Units.CUBIC_METRE,       f.parse("m⋅m⋅m"));
        assertSame(Units.CUBIC_METRE,       f.parse("m²⋅m"));
        assertSame(Units.CUBIC_METRE,       f.parse("m2.m"));
        assertSame(Units.METRES_PER_SECOND, f.parse("m∕s"));
        assertSame(Units.HERTZ,             f.parse("1/s"));
    }

    /**
     * Tests parsing of symbols composed of terms combined by arithmetic operations (e.g. "m/s").
     */
    @Test
    @DependsOnMethod("testParseTerms")
    public void testParseMultiplier() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        assertSame(Units.MILLIMETRE, f.parse("m/1000"));
        ConventionalUnitTest.verify(Units.METRE, f.parse("10*-6⋅m"),   "µm", 1E-6);
        ConventionalUnitTest.verify(Units.METRE, f.parse("10*-6.m"),   "µm", 1E-6);
        ConventionalUnitTest.verify(Units.METRE, f.parse( "1000*m"),   "km", 1E+3);
        ConventionalUnitTest.verify(Units.METRE, f.parse( "1000.0*m"), "km", 1E+3);
    }
}
