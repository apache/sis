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
import java.text.ParsePosition;
import java.lang.reflect.Field;
import javax.measure.Unit;
import javax.measure.format.ParserException;
import org.apache.sis.util.Characters;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link UnitFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
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
        verify(declared, "NANOMETRE",           "L",            "nm",    "nanometre",               Units.NANOMETRE);
        verify(declared, "MILLIMETRE",          "L",            "mm",    "millimetre",              Units.MILLIMETRE);
        verify(declared, "CENTIMETRE",          "L",            "cm",    "centimetre",              Units.CENTIMETRE);
        verify(declared, "METRE",               "L",            "m",     "metre",                   Units.METRE);
        verify(declared, "KILOMETRE",           "L",            "km",    "kilometre",               Units.KILOMETRE);
        verify(declared, "NAUTICAL_MILE",       "L",            "M",     "nautical mile",           Units.NAUTICAL_MILE);
        verify(declared, "STATUTE_MILE",        "L",            "mi",    "statute mile",            Units.STATUTE_MILE);
        verify(declared, "US_SURVEY_FOOT",      "L",            "ftUS",  "US survey foot",          Units.US_SURVEY_FOOT);
        verify(declared, "CLARKE_FOOT",         "L",            "ftCla", "Clarke’s foot",           Units.CLARKE_FOOT);
        verify(declared, "FOOT",                "L",            "ft",    "foot",                    Units.FOOT);
        verify(declared, "INCH",                "L",            "in",    "inch",                    Units.INCH);
        verify(declared, "POINT",               "L",            "pt",    "point",                   Units.POINT);
        verify(declared, "SQUARE_METRE",        "L²",           "m²",    "square metre",            Units.SQUARE_METRE);
        verify(declared, "HECTARE",             "L²",           "ha",    "hectare",                 Units.HECTARE);
        verify(declared, "CUBIC_METRE",         "L³",           "m³",    "cubic metre",             Units.CUBIC_METRE);
        verify(declared, "LITRE",               "L³",           "L",     "litre",                   Units.LITRE);
        verify(declared, "STERADIAN",           "",             "sr",    "steradian",               Units.STERADIAN);
        verify(declared, "MICRORADIAN",         "",             "µrad",  "microradian",             Units.MICRORADIAN);
        verify(declared, "RADIAN",              "",             "rad",   "radian",                  Units.RADIAN);
        verify(declared, "DEGREE",              "",             "°",     "degree",                  Units.DEGREE);
        verify(declared, "ARC_MINUTE",          "",             "′",     "arc-minute",              Units.ARC_MINUTE);
        verify(declared, "ARC_SECOND",          "",             "″",     "arc-second",              Units.ARC_SECOND);
        verify(declared, "GRAD",                "",             "grad",  "grad",                    Units.GRAD);
        verify(declared, "MILLISECOND",         "T",            "ms",    "millisecond",             Units.MILLISECOND);
        verify(declared, "SECOND",              "T",            "s",     "second",                  Units.SECOND);
        verify(declared, "MINUTE",              "T",            "min",   "minute",                  Units.MINUTE);
        verify(declared, "HOUR",                "T",            "h",     "hour",                    Units.HOUR);
        verify(declared, "DAY",                 "T",            "d",     "day",                     Units.DAY);
        verify(declared, "WEEK",                "T",            "wk",    "week",                    Units.WEEK);
        verify(declared, "TROPICAL_YEAR",       "T",            "a",     "year",                    Units.TROPICAL_YEAR);
        verify(declared, "HERTZ",               "∕T",           "Hz",    "hertz",                   Units.HERTZ);
        verify(declared, "METRES_PER_SECOND",   "L∕T",          "m∕s",   "metres per second",       Units.METRES_PER_SECOND);
        verify(declared, "KILOMETRES_PER_HOUR", "L∕T",          "km∕h",  "kilometres per hour",     Units.KILOMETRES_PER_HOUR);
        verify(declared, "PASCAL",              "M∕(L⋅T²)",     "Pa",    "pascal",                  Units.PASCAL);
        verify(declared, "HECTOPASCAL",         "M∕(L⋅T²)",     "hPa",   "hectopascal",             Units.HECTOPASCAL);
        verify(declared, "DECIBAR",             "M∕(L⋅T²)",     "dbar",  "decibar",                 Units.DECIBAR);
        verify(declared, "BAR",                 "M∕(L⋅T²)",     "bar",    null,                     Units.BAR);
        verify(declared, "ATMOSPHERE",          "M∕(L⋅T²)",     "atm",   "atmosphere",              Units.ATMOSPHERE);
        verify(declared, "NEWTON",              "M⋅L∕T²",       "N",     "newton",                  Units.NEWTON);
        verify(declared, "JOULE",               "M⋅L²∕T²",      "J",     "joule",                   Units.JOULE);
        verify(declared, "WATT",                "M⋅L²∕T³",      "W",     "watt",                    Units.WATT);
        verify(declared, "VOLT",                "M⋅L²∕(T³⋅I)",  "V",     "volt",                    Units.VOLT);
        verify(declared, "AMPERE",              "I",            "A",     "ampere",                  Units.AMPERE);
        verify(declared, "COULOMB",             "I⋅T",          "C",     "coulomb",                 Units.COULOMB);
        verify(declared, "FARAD",               "I²⋅T⁴∕(M⋅L²)", "F",     "farad",                   Units.FARAD);
        verify(declared, "OHM",                 "M⋅L²∕(T³⋅I²)", "Ω",     "ohm",                     Units.OHM);
        verify(declared, "SIEMENS",             "I²⋅T³∕(M⋅L²)", "S",     "siemens",                 Units.SIEMENS);
        verify(declared, "WEBER",               "M⋅L²∕(T²⋅I)",  "Wb",    "weber",                   Units.WEBER);
        verify(declared, "TESLA",               "M∕(T²⋅I)",     "T",     "tesla",                   Units.TESLA);
        verify(declared, "HENRY",               "M⋅L²∕(T²⋅I²)", "H",     "henry",                   Units.HENRY);
        verify(declared, "KELVIN",              "Θ",            "K",     "kelvin",                  Units.KELVIN);
        verify(declared, "CELSIUS",             "Θ",            "°C",    "Celsius",                 Units.CELSIUS);
        verify(declared, "FAHRENHEIT",          "Θ",            "°F",    "Fahrenheit",              Units.FAHRENHEIT);
        verify(declared, "CANDELA",             "J",            "cd",    "candela",                 Units.CANDELA);
        verify(declared, "LUMEN",               "J",            "lm",    "lumen",                   Units.LUMEN);
        verify(declared, "LUX",                 "J∕L²",         "lx",    "lux",                     Units.LUX);
        verify(declared, "KILOGRAM",            "M",            "kg",    "kilogram",                Units.KILOGRAM);
        verify(declared, "GRAM",                "M",            "g",     "gram",                    Units.GRAM);
        verify(declared, "MOLE",                "N",            "mol",   "mole",                    Units.MOLE);
        verify(declared, "UNITY",               "",             "",       null,                     Units.UNITY);
        verify(declared, "PERCENT",             "",             "%",     "percentage",              Units.PERCENT);
        verify(declared, "PPM",                 "",             "ppm",   "parts per million",       Units.PPM);
        verify(declared, "PSU",                 "",             "psu",   "practical salinity unit", Units.PSU);
        verify(declared, "PIXEL",               "",             "px",    "pixel",                   Units.PIXEL);
        assertTrue("Missing units in test:" + declared, declared.isEmpty());
    }

    /**
     * Verifies one of the constants declared in the {@link Unit} class.
     *
     * @param declared   a map from which to remove the {@code field} value, for verifying that we didn't forgot an element.
     * @param field      the name of the constant to be verified.
     * @param dimension  the expected string representation of the unit dimension.
     * @param symbol     the expected string representation of the unit.
     * @param name       the expected name, or {@code null} for skipping this test.
     * @param unit       the unit to verify.
     */
    private static void verify(final Set<String> declared, final String field, final String dimension, final String symbol, final String name, final Unit<?> unit) {
        assertEquals(field, dimension, String.valueOf(unit.getDimension()));
        assertEquals(field, symbol,    UnitFormat.INSTANCE.format(unit));
        if (name != null) {
            assertEquals(field, name, UnitFormat.getBundle(Locale.UK).getString(symbol));
            for (int i=0; i<name.length();) {
                final int c = name.codePointAt(i);
                assertTrue(name, AbstractUnit.isSymbolChar(c) || Character.isWhitespace(c));
                i += Character.charCount(c);
            }
        }
        for (int i=0; i<symbol.length();) {
            final int c = symbol.codePointAt(i);
            assertTrue(symbol, AbstractUnit.isSymbolChar(c) || Characters.isSuperScript(c) || c == '∕');
            i += Character.charCount(c);
        }
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
        assertEquals("g",   f.format(Units.GRAM));
        assertEquals("kg",  f.format(Units.KILOGRAM));
        assertEquals("s",   f.format(Units.SECOND));
        assertEquals("min", f.format(Units.MINUTE));
        assertEquals("m2",  f.format(Units.SQUARE_METRE));
        assertEquals("m3",  f.format(Units.CUBIC_METRE));
        assertEquals("L",   f.format(Units.LITRE));
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
        assertEquals("gram",         f.format(Units.GRAM));
        assertEquals("kilogram",     f.format(Units.KILOGRAM));
        assertEquals("second",       f.format(Units.SECOND));
        assertEquals("minute",       f.format(Units.MINUTE));
        assertEquals("square metre", f.format(Units.SQUARE_METRE));
        assertEquals("cubic metre",  f.format(Units.CUBIC_METRE));
        assertEquals("litre",        f.format(Units.LITRE));
        assertEquals("Celsius",      f.format(Units.CELSIUS));          // Really upper-case "C" - this is a SI exception.

        f.setLocale(Locale.US);
        assertEquals("meter",        f.format(Units.METRE));
        assertEquals("kilometer",    f.format(Units.KILOMETRE));
        assertEquals("second",       f.format(Units.SECOND));
        assertEquals("square meter", f.format(Units.SQUARE_METRE));
        assertEquals("cubic meter",  f.format(Units.CUBIC_METRE));
        assertEquals("liter",        f.format(Units.LITRE));
        assertEquals("Celsius",      f.format(Units.CELSIUS));

        f.setLocale(Locale.FRANCE);
        assertEquals("mètre",        f.format(Units.METRE));
        assertEquals("kilomètre",    f.format(Units.KILOMETRE));
        assertEquals("seconde",      f.format(Units.SECOND));
        assertEquals("mètre carré",  f.format(Units.SQUARE_METRE));
        assertEquals("mètre cube",   f.format(Units.CUBIC_METRE));
        assertEquals("litre",        f.format(Units.LITRE));
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
        assertSame(Units.CUBIC_METRE,   f.parse("cubic_metres"));
        assertSame(Units.LITRE,         f.parse("litre"));
        assertSame(Units.LITRE,         f.parse("liters"));
        assertSame(Units.GRAM,          f.parse("gram"));
        assertSame(Units.GRAM,          f.parse("grams"));
        assertSame(Units.KILOGRAM,      f.parse("kilogram"));
        assertSame(Units.KILOGRAM,      f.parse("kilograms"));
        assertSame(Units.DEGREE,        f.parse("degree"));
        assertSame(Units.DEGREE,        f.parse("degrees"));
        assertSame(Units.DEGREE,        f.parse("decimal degrees"));
        assertSame(Units.DEGREE,        f.parse("Degrees North"));
        assertSame(Units.DEGREE,        f.parse("degrees north"));
        assertSame(Units.DEGREE,        f.parse("degree north"));
        assertSame(Units.DEGREE,        f.parse("degrees_east"));
        assertSame(Units.DEGREE,        f.parse("degree_east"));
        assertSame(Units.DEGREE,        f.parse("Degree West"));
        assertSame(Units.DEGREE,        f.parse("degrees N"));
        assertSame(Units.DEGREE,        f.parse("degE"));
        assertSame(Units.DEGREE,        f.parse("Deg_E"));
        assertSame(Units.KELVIN,        f.parse("degree Kelvin"));
        assertSame(Units.CELSIUS,       f.parse("degree Celsius"));
        assertSame(Units.CELSIUS,       f.parse("degrees C"));
        assertSame(Units.KELVIN,        f.parse("degK"));
        assertSame(Units.CELSIUS,       f.parse("degC"));
        assertSame(Units.CELSIUS,       f.parse("deg C"));
        assertSame(Units.WATT,          f.parse("watt"));
        assertSame(Units.UNITY,         f.parse("unity"));
        try {
            f.parse("degree foo");
            fail("Should not accept unknown unit.");
        } catch (ParserException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("degree"));
            assertTrue(message, message.contains("foo"));
        }
        // Tests with localisation.
        try {
            f.parse("mètre cube");
            fail("Should not accept localized unit unless requested.");
        } catch (ParserException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("mètre"));
            assertTrue(message, message.contains("cube"));
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
        assertSame(Units.LITRE,         f.parse("L"));
        assertSame(Units.LITRE,         f.parse("l"));
        assertSame(Units.LITRE,         f.parse("ℓ"));
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
        ConventionalUnitTest.verify(Units.JOULE,       f.parse("kJ"),   "kJ",   1E+3);
        ConventionalUnitTest.verify(Units.HERTZ,       f.parse("MHz"),  "MHz",  1E+6);
        ConventionalUnitTest.verify(Units.PASCAL,      f.parse("daPa"), "daPa", 1E+1);
        ConventionalUnitTest.verify(Units.CUBIC_METRE, f.parse("mL"),   "mL",   1E-6);
        ConventionalUnitTest.verify(Units.CUBIC_METRE, f.parse("ml"),   "mL",   1E-6);
        ConventionalUnitTest.verify(Units.KILOGRAM,    f.parse("kg"),   "kg",   1E+0);
        ConventionalUnitTest.verify(Units.KILOGRAM,    f.parse("g"),    "g",    1E-3);
        ConventionalUnitTest.verify(Units.KILOGRAM,    f.parse("mg"),   "mg",   1E-6);
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
        assertSame(Units.KILOMETRE,  f.parse( "1000*m"));
        assertSame(Units.KILOMETRE,  f.parse( "1000.0*m"));
        ConventionalUnitTest.verify(Units.METRE, f.parse("10*-6⋅m"),   "µm", 1E-6);
        ConventionalUnitTest.verify(Units.METRE, f.parse("10*-6.m"),   "µm", 1E-6);
        ConventionalUnitTest.verify(Units.METRE, f.parse("10^-3.m"),   "mm", 1E-3);
        ConventionalUnitTest.verify(Units.METRE, f.parse( "100 feet"), null, 30.48);
    }

    /**
     * Tests parsing of symbols containing an explicit exponentiation operation.
     * Usually the exponentiation is implicit, as in {@code "m*s-1"}.
     * However some formats write it explicitely, as in {@code "m*s^-1"}.
     */
    @Test
    @DependsOnMethod("testParseMultiplier")
    public void testParseExponentiation() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        assertSame(Units.HERTZ,             f.parse("s^-1"));
        assertSame(Units.HERTZ,             f.parse("s**-1"));
        assertSame(Units.METRES_PER_SECOND, f.parse("m*s^-1"));
        assertSame(Units.METRES_PER_SECOND, f.parse("m*s**-1"));
    }

    /**
     * Tests parsing expressions containing parenthesis.
     */
    @Test
    @DependsOnMethod("testParseMultiplier")
    public void testParseWithParenthesis() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        assertSame(Units.PASCAL, f.parse("kg∕(m⋅s²)"));
        assertSame(Units.PASCAL, f.parse("(kg)∕m∕s²"));
        assertSame(Units.VOLT,   f.parse("kg⋅m²∕(s³⋅A)"));
        assertSame(Units.VOLT,   f.parse("(kg)m²∕(s³⋅A)"));
    }

    /**
     * Tests parsing a unit from another position than zero and verifies that {@code UnitFormat} detects
     * correctly where the unit symbol ends.
     */
    @Test
    @DependsOnMethod("testParseSymbol")
    public void testParsePosition() {
        final UnitFormat f = new UnitFormat(Locale.UK);
        final ParsePosition pos = new ParsePosition(4);
        assertSame(Units.CENTIMETRE, f.parse("ABC cm DEF", pos));
        assertEquals("ParsePosition.getIndex()", 6, pos.getIndex());
        assertEquals("ParsePosition.getErrorIndex()", -1, pos.getErrorIndex());
        /*
         * Adding "cm DEF" as a unit label should allow UnitFormat to recognize those characters.
         * We associate a random unit to that label, just for testing purpose.
         */
        pos.setIndex(4);
        f.label(Units.HECTARE, "cm DEF");
        assertSame(Units.HECTARE, f.parse("ABC cm DEF", pos));
        assertEquals("ParsePosition.getIndex()", 10, pos.getIndex());
        assertEquals("ParsePosition.getErrorIndex()", -1, pos.getErrorIndex());
    }

    /**
     * Tests {@link UnitFormat#clone()}.
     */
    @Test
    public void testClone() {
        final UnitFormat f1 = new UnitFormat(Locale.FRANCE);
        f1.label(Units.METRE,  "myMeterLabel");
        f1.label(Units.SECOND, "mySecondLabel");
        final UnitFormat f2 = f1.clone();
        f2.label(Units.METRE, "otherMeterLabel");
        assertSame  (Locale.FRANCE,     f1.getLocale());
        assertSame  (Locale.FRANCE,     f2.getLocale());
        assertEquals("myMeterLabel",    f1.format(Units.METRE));
        assertEquals("mySecondLabel",   f1.format(Units.SECOND));
        assertEquals("otherMeterLabel", f2.format(Units.METRE));
        assertEquals("mySecondLabel",   f2.format(Units.SECOND));
    }
}
