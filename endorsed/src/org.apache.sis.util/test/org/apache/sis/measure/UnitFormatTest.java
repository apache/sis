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
import javax.measure.quantity.Length;
import javax.measure.format.MeasurementParseException;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Characters;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;


/**
 * Tests the {@link UnitFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class UnitFormatTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public UnitFormatTest() {
    }

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
        final var declared = new HashSet<String>(64);
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
        verify(declared, "NANOSECOND",          "T",            "ns",    "nanosecond",              Units.NANOSECOND);
        verify(declared, "MILLISECOND",         "T",            "ms",    "millisecond",             Units.MILLISECOND);
        verify(declared, "SECOND",              "T",            "s",     "second",                  Units.SECOND);
        verify(declared, "MINUTE",              "T",            "min",   "minute",                  Units.MINUTE);
        verify(declared, "HOUR",                "T",            "h",     "hour",                    Units.HOUR);
        verify(declared, "DAY",                 "T",            "d",     "day",                     Units.DAY);
        verify(declared, "WEEK",                "T",            "wk",    "week",                    Units.WEEK);
        verify(declared, "TROPICAL_YEAR",       "T",            "a",     "year",                    Units.TROPICAL_YEAR);
        verify(declared, "CURIE",               "∕T",           "Ci",    "curie",                   Units.CURIE);
        verify(declared, "BECQUEREL",           "∕T",           "Bq",    "becquerel",               Units.BECQUEREL);
        verify(declared, "HERTZ",               "∕T",           "Hz",    "hertz",                   Units.HERTZ);
        verify(declared, "RADIANS_PER_SECOND",  "∕T",           "rad∕s", "radians per second",      Units.RADIANS_PER_SECOND);
        verify(declared, "METRES_PER_SECOND",   "L∕T",          "m∕s",   "metres per second",       Units.METRES_PER_SECOND);
        verify(declared, "KILOMETRES_PER_HOUR", "L∕T",          "km∕h",  "kilometres per hour",     Units.KILOMETRES_PER_HOUR);
        verify(declared, "KNOT",                "L∕T",          "kn",    "knot",                    Units.KNOT);
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
        verify(declared, "TONNE",               "M",            "t",     "tonne",                   Units.TONNE);
        verify(declared, "MOLE",                "N",            "mol",   "mole",                    Units.MOLE);
        verify(declared, "UNITY",               "",             "",       null,                     Units.UNITY);
        verify(declared, "PERCENT",             "",             "%",     "percent",                 Units.PERCENT);
        verify(declared, "PPM",                 "",             "ppm",   "parts per million",       Units.PPM);
        verify(declared, "PSU",                 "",             "psu",   "practical salinity unit", Units.PSU);
        verify(declared, "PIXEL",               "",             "px",    "pixel",                   Units.PIXEL);
        verify(declared, "DECIBEL",             "",             "dB",    "decibel",                 Units.DECIBEL);
        verify(declared, "GAL",                 "L∕T²",         "Gal",   "gal",                     Units.GAL);
        verify(declared, "METRES_PER_SECOND_SQUARED", "L∕T²",   "m∕s²",  "metres per second squared", Units.METRES_PER_SECOND_SQUARED);
        assertTrue(declared.isEmpty(), () -> "Missing units in test:" + declared);
    }

    /**
     * Verifies one of the constants declared in the {@link Units} class.
     *
     * @param declared   a map from which to remove the {@code field} value, for verifying that we didn't forgot an element.
     * @param field      the name of the constant to be verified.
     * @param dimension  the expected string representation of the unit dimension.
     * @param symbol     the expected string representation of the unit.
     * @param name       the expected name, or {@code null} for skipping this test.
     * @param unit       the unit to verify.
     */
    private static void verify(final Set<String> declared, final String field, final String dimension, final String symbol, final String name, final Unit<?> unit) {
        assertEquals(dimension, String.valueOf(unit.getDimension()), field);
        assertEquals(symbol, UnitFormat.INSTANCE.format(unit), field);
        if (name != null) {
            assertEquals(name, UnitFormat.getBundle(Locale.UK).getString(symbol), field);
            for (int i=0; i<name.length();) {
                final int c = name.codePointAt(i);
                assertTrue(AbstractUnit.isSymbolChar(c) || Character.isWhitespace(c), name);
                i += Character.charCount(c);
            }
        }
        for (int i=0; i<symbol.length();) {
            final int c = symbol.codePointAt(i);
            assertTrue(AbstractUnit.isSymbolChar(c) || Characters.isSuperScript(c) || c == '∕', symbol);
            i += Character.charCount(c);
        }
        declared.remove(field);
    }

    /**
     * Tests the formatting of a dimension having rational powers.
     */
    @Test
    public void testRationalPower() {
        assertEquals("T^(5⁄2)∕(M⋅L)", UnitDimensionTest.specificDetectivity().toString());
    }

    /**
     * Tests {@link UnitFormat#label(Unit, String)}.
     */
    @Test
    public void testLabel() {
        final var f = new UnitFormat(Locale.ENGLISH);
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

        // “mFoo” should not be assigned to unit anymore.
        RuntimeException exception;
        exception = assertThrows(MeasurementParseException.class, () -> f.parse("mFoo"));
        assertMessageContains(exception, "mFoo");

        // Should not accept labels ending with a digit.
        exception = assertThrows(IllegalArgumentException.class, () -> f.label(Units.METRE, "m¹"));
        assertMessageContains(exception, "m¹");
    }

    /**
     * Tests the assignation of two labels on the same unit.
     */
    @Test
    public void testDuplicatedLabels() {
        final var f = new UnitFormat(Locale.ENGLISH);
        f.label(Units.DEGREE, "deg");
        f.label(Units.DEGREE, "dd");        // For "decimal degrees"
        roundtrip(f, "dd", "dd");
    }

    /**
     * Tests unit formatting with {@link UnitFormat.Style#UCUM}.
     */
    @Test
    public void testFormatUCUM() {
        final var f = new UnitFormat(Locale.UK);
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
        final var f = new UnitFormat(Locale.UK);
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
     * Tests the formatting of units that are derived from existing units by a multiplication factor.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-382">SIS-382</a>
     */
    @Test
    public void testFormatScaled() {
        final var f = new UnitFormat(Locale.UK);
        f.setStyle(UnitFormat.Style.SYMBOL);
        assertEquals("Mm",      f.format(Units.KILOMETRE .multiply(1000)));
        assertEquals("10⁵⋅m",   f.format(Units.KILOMETRE .multiply( 100)));
        assertEquals("10⁻⁴⋅m",  f.format(Units.MILLIMETRE.divide  (  10)));
        assertEquals("mg",      f.format(Units.KILOGRAM  .divide  (1E+6)));
        assertEquals("mg",      f.format(Units.GRAM      .divide  (1E+3)));
        assertEquals("µg",      f.format(Units.KILOGRAM  .multiply(1E-9)));
        assertEquals("cg",      f.format(Units.GRAM      .divide  ( 100)));
        assertEquals("10⁻⁷⋅kg", f.format(Units.GRAM      .divide  (1E+4)));
    }

    /**
     * Tests formatting of units raised to some powers.
     */
    @Test
    public void testFormatPower() {
        final var f = new UnitFormat(Locale.UK);
        f.setStyle(UnitFormat.Style.SYMBOL);
        assertEquals("m²",  f.format(Units.METRE     .pow(2)));
        assertEquals("cm²", f.format(Units.CENTIMETRE.pow(2)));
        assertEquals("in²", f.format(Units.INCH      .pow(2)));
    }

    /**
     * Tests formatting ratio of units.
     */
    @Test
    public void testFormatRatio() {
        final var f = new UnitFormat(Locale.UK);
        f.setStyle(UnitFormat.Style.SYMBOL);
        assertEquals( "m∕h", f.format(Units.METRE.divide(Units.HOUR)));
        assertEquals("mm∕h", f.format(Units.MILLIMETRE.divide(Units.HOUR)));
    }

    /**
     * Tests formatting of some more unusual units. The units tested by this method are artificial
     * and somewhat convolved. The intent is to verify that unit formatting is still robust.
     */
    @Test
    public void testFormatUnusual() {
        final var f = new UnitFormat(Locale.UK);
        final Unit<?> u1 = Units.SECOND.pow(-1).multiply(3);
        assertEquals("3∕s",        f.format(u1));
        assertEquals("3⋅m∕s",      f.format(Units.METRE.multiply(u1)));
        assertEquals("m^⅔",        f.format(Units.METRE.pow(2).root(3)));
        assertEquals("km²∕(s⋅kg)", f.format(Units.SQUARE_METRE.divide(Units.SECOND).divide(Units.KILOGRAM).multiply(1E+6)));
    }

    /**
     * Tests formatting of units when the numerator is unity.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-414">SIS-414</a>
     */
    @Test
    public void testUnity() {
        final var f = new UnitFormat(Locale.UK);
        assertEquals(   "1∕m²", f.format(Units.UNITY.divide(Units.SQUARE_METRE)));
        assertEquals("10⁻²∕m²", f.format(Units.UNITY.divide(100).divide(Units.SQUARE_METRE)));
        assertEquals("%∕m²",    f.format(Units.PERCENT.divide(Units.SQUARE_METRE)));
    }

    /**
     * Tests parsing of names, for example {@code "meter"}.
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

        // Should not accept unknown unit.
        MeasurementParseException exception;
        exception = assertThrows(MeasurementParseException.class, () -> f.parse("degree foo"));
        assertMessageContains(exception, "degree", "foo");

        // Should not accept localized unit unless requested.
        exception = assertThrows(MeasurementParseException.class, () -> f.parse("mètre cube"));
        assertMessageContains(exception, "mètre", "cube");

        f.setLocale(Locale.FRANCE);
        assertSame(Units.CUBIC_METRE, f.parse("mètre cube"));
    }

    /**
     * Tests parsing of names such "metres per second".
     */
    @Test
    public void testParseNameWithRatio() {
        final var f = new UnitFormat(Locale.UK);
        assertSame(Units.METRES_PER_SECOND, f.parse("metres per second"));
        assertSame(Units.METRES_PER_SECOND, f.parse("metres per seconds"));     // Mispelling sometime encoutered.
    }

    /**
     * Tests parsing of names raised to some power, for example {@code "meter2"}.
     */
    @Test
    public void testParseNameRaisedToPower() {
        final var f = new UnitFormat(Locale.UK);
        assertSame(Units.SQUARE_METRE, f.parse("meter2"));
        assertSame(Units.HERTZ,        f.parse("second-1"));
    }

    /**
     * Tests parsing a unit defined by a URI in OGC namespace.
     * Example: {@code "urn:ogc:def:uom:EPSG::1026"} is for metres per second.
     */
    @Test
    public void testParseEPSG() {
        final var f = new UnitFormat(Locale.UK);
        assertSame(Units.METRE,             f.parse("EPSG:9001"));
        assertSame(Units.METRE,             f.parse("urn:ogc:def:uom:EPSG::9001"));
        assertSame(Units.METRES_PER_SECOND, f.parse("urn:ogc:def:uom:EPSG::1026"));
    }

    /**
     * Tests parsing a unit defined by a URL.
     */
    @Test
    public void testParseURL() {
        final var f = new UnitFormat(Locale.UK);
        assertSame(Units.METRE, f.parse("http://www.opengis.net/def/uom/EPSG/0/9001"));
        assertSame(Units.DAY,   f.parse("http://www.opengis.net/def/uom/UCUM/0/d"));
    }

    /**
     * Tests parsing of symbols without arithmetic operations other than exponent.
     */
    @Test
    public void testParseSymbol() {
        final var f = new UnitFormat(Locale.UK);
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
    public void testParsePrefix() {
        final var f = new UnitFormat(Locale.UK);
        ConventionalUnitTest.verify(Units.JOULE,       f.parse("kJ"),   "kJ",   1E+3);
        ConventionalUnitTest.verify(Units.HERTZ,       f.parse("MHz"),  "MHz",  1E+6);
        ConventionalUnitTest.verify(Units.PASCAL,      f.parse("daPa"), "daPa", 1E+1);
        ConventionalUnitTest.verify(Units.CUBIC_METRE, f.parse("mL"),   "mL",   1E-6);
        ConventionalUnitTest.verify(Units.CUBIC_METRE, f.parse("ml"),   "mL",   1E-6);
        ConventionalUnitTest.verify(Units.KILOGRAM,    f.parse("kg"),   "kg",   1E+0);
        ConventionalUnitTest.verify(Units.KILOGRAM,    f.parse("g"),    "g",    1E-3);
        ConventionalUnitTest.verify(Units.KILOGRAM,    f.parse("mg"),   "mg",   1E-6);
        /*
         * When the unit contain an exponent, the conversion factor shall be raised
         * to that exponent too.
         */
        assertEquals(1E+6, Units.toStandardUnit(f.parse("km²")), "km²");
        assertEquals(1E+6, Units.toStandardUnit(f.parse("kJ²")), "kJ²");
        /*
         * Verify that prefix are not accepted for conventional units. It would either be illegal prefix duplication
         * (for example we should not accept "kkm" as if it was "k" + "km") or confusing (for example "a" stands for
         * the tropical year, "ha" could be understood as 100 tropical years but is actually used for hectare).
         */
        assertSame(Units.TROPICAL_YEAR, f.parse("a"));

        // Should not accept prefix in ConventionalUnit.
        MeasurementParseException exception;
        exception = assertThrows(MeasurementParseException.class, () -> f.parse("ka"));
        assertMessageContains(exception, "ka");
    }

    /**
     * Tests parsing of symbols composed of terms combined by arithmetic operations (e.g. "m/s").
     */
    @Test
    public void testParseTerms() {
        final var f = new UnitFormat(Locale.UK);
        assertSame(Units.SQUARE_METRE,      f.parse("m⋅m"));
        assertSame(Units.CUBIC_METRE,       f.parse("m⋅m⋅m"));
        assertSame(Units.CUBIC_METRE,       f.parse("m²⋅m"));
        assertSame(Units.CUBIC_METRE,       f.parse("m2.m"));
        assertSame(Units.CUBIC_METRE,       f.parse("m^3"));
        assertSame(Units.METRES_PER_SECOND, f.parse("m∕s"));
        assertSame(Units.HERTZ,             f.parse("1/s"));
    }

    /**
     * Tests parsing of symbols containing terms separated by spaces.
     * This is valid only when using {@link UnitFormat#parse(CharSequence)}.
     */
    @Test
    public void testParseTermsSeparatedBySpace() {
        final var f = new UnitFormat(Locale.UK);
        assertSame(Units.METRES_PER_SECOND, f.parse("m s**-1"));
        assertEqualsIgnoreSymbol(Units.KILOGRAM.divide(Units.SQUARE_METRE), f.parse("kg m**-2"));

        // Should not accept unknown sentence even if each individual word is known.
        MeasurementParseException exception;
        exception = assertThrows(MeasurementParseException.class, () -> f.parse("degree minute"));
        assertMessageContains(exception, "degree", "minute");
    }

    /**
     * Tests parsing of symbols composed of terms combined by arithmetic operations (e.g. "m/s").
     */
    @Test
    public void testParseMultiplier() {
        final var f = new UnitFormat(Locale.UK);
        assertSame(Units.MILLIMETRE, f.parse("m/1000"));
        assertSame(Units.KILOMETRE,  f.parse( "1000*m"));
        assertSame(Units.KILOMETRE,  f.parse( "1000.0*m"));
        ConventionalUnitTest.verify(Units.METRE,    f.parse("10*-6⋅m"),   "µm", 1E-6);
        ConventionalUnitTest.verify(Units.METRE,    f.parse("10*-6.m"),   "µm", 1E-6);
        ConventionalUnitTest.verify(Units.METRE,    f.parse("10^-3.m"),   "mm", 1E-3);
        ConventionalUnitTest.verify(Units.METRE,    f.parse("10⁻⁴.m"),    null, 1E-4);
        ConventionalUnitTest.verify(Units.METRE,    f.parse( "100 feet"), null, 30.48);
        ConventionalUnitTest.verify(Units.KILOGRAM, f.parse("10*3.kg"),   "Mg", 1E+3);
        ConventionalUnitTest.verify(Units.KILOGRAM, f.parse("10⋅mg"),     "cg", 1E-5);
        ConventionalUnitTest.verify(Units.KILOGRAM, f.parse("10^-6.kg"),  "mg", 1E-6);
    }

    /**
     * Tests parsing of symbols composed of terms containing kilogram.
     * The management of SI prefixes need to make a special case for kilogram.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-382">SIS-382</a>
     * @see ConventionalUnitTest#testKilogram()
     */
    @Test
    public void testParseKilogram() {
        final var f = new UnitFormat(Locale.UK);
        /*
         * Kilograms should be identified even if they appear in an expression.
         * Current implementation creates a symbol early when it detects such case.
         */
        assertEquals("mg∕m",  f.parse("10^-6.kg/m").getSymbol());
        assertEquals("µg∕m³", f.parse("μg.m-3").getSymbol());
    }

    /**
     * Tests the parsing of {@code "1/l"}.
     */
    @Test
    public void testParseInverseL() {
        final var f = new UnitFormat(Locale.UK);
        final Unit<?> u = f.parse("1/l");
        assertEquals("1∕L", u.toString());
    }

    /**
     * Tests parsing of symbols containing an explicit exponentiation operation.
     * Usually the exponentiation is implicit, as in {@code "m*s-1"}.
     * However, some formats write it explicitly, as in {@code "m*s^-1"}.
     */
    @Test
    public void testParseExponentiation() {
        final var f = new UnitFormat(Locale.UK);
        assertSame(Units.HERTZ,             f.parse("s^-1"));
        assertSame(Units.HERTZ,             f.parse("s**-1"));
        assertSame(Units.METRES_PER_SECOND, f.parse("m*s^-1"));
        assertSame(Units.METRES_PER_SECOND, f.parse("m*s**-1"));
    }

    /**
     * Tests parsing expressions containing parenthesis.
     */
    @Test
    public void testParseWithParenthesis() {
        final var f = new UnitFormat(Locale.UK);
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
    public void testParsePosition() {
        final var f = new UnitFormat(Locale.UK);
        final var pos = new ParsePosition(4);
        assertSame(Units.CENTIMETRE, f.parse("ABC cm foo", pos));
        assertEquals( 6, pos.getIndex(), "ParsePosition.getIndex()");
        assertEquals(-1, pos.getErrorIndex(), "ParsePosition.getErrorIndex()");
        /*
         * Adding "cm DEF" as a unit label should allow UnitFormat to recognize those characters.
         * We associate a random unit to that label, just for testing purpose.
         */
        pos.setIndex(4);
        f.label(Units.HECTARE, "cm foo");
        assertEqualsIgnoreSymbol(Units.HECTARE, f.parse("ABC cm foo", pos));
        assertEquals(10, pos.getIndex(), "ParsePosition.getIndex()");
        assertEquals(-1, pos.getErrorIndex(), "ParsePosition.getErrorIndex()");
    }

    /**
     * Tests {@link UnitFormat#clone()}.
     */
    @Test
    public void testClone() {
        final var f1 = new UnitFormat(Locale.FRANCE);
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

    /**
     * Tests parsing of miscellaneous symbols, followed by formatting.
     * This test uses some units defined by World Meteorological Organisation (WMO).
     * The lines with <q>Too aggressive simplification bug (SIS-378)</q> comment are actually bugs,
     * but they are tested anyway (despite the bogus "expected" value) for tracking progresses on SIS-378.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-378">SIS-378</a>
     */
    @Test
    public void testParseAndFormat() {
        final var f = new UnitFormat(Locale.UK);
        roundtrip(f, "K.m2.kg-1.s-1",    "K⋅m²∕(kg⋅s)");
        roundtrip(f, "m.m6.m-3",         "m⋅m⁶∕m³");
        roundtrip(f, "Pa.s-1",           "Pa∕s");
        roundtrip(f, "S.m-1",            "S∕m");
        roundtrip(f, "m2/3.s-1",         "m^⅔∕s");
        roundtrip(f, "J.kg-1",           "J∕kg");
        roundtrip(f, "mol.mol-1",        "mol∕mol");
        roundtrip(f, "mol.s-1",          "mol∕s");
        roundtrip(f, "K.s-1",            "K∕s");
        roundtrip(f, "m.s-1",            "m∕s");
        roundtrip(f, "m.s-2",            "m∕s²");
        roundtrip(f, "Pa.m",             "Pa⋅m");
        roundtrip(f, "m3.s-1",           "m³∕s");
        roundtrip(f, "kg.m-2.s-1",       "kg∕(m²⋅s)");
        roundtrip(f, "μg.m-2",           "µg∕m²");
        roundtrip(f, "K.m-1",            "K∕m");
        roundtrip(f, "W.m-2",            "W∕m²");
        roundtrip(f, "W.m-2.Hz-1",       "kg∕s²");          // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "W.sr-1.m-2",       "kg∕s³");          // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "W.m-1.sr-1",       "W∕m");            // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "W.m-3.sr-1",       "W∕m³");           // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "N.m-1",            "N∕m");
        roundtrip(f, "N.m-2",            "Pa");
        roundtrip(f, "kg.m-2",           "kg∕m²");
        roundtrip(f, "kg.m-3",           "kg∕m³");
        roundtrip(f, "K*m.s-1",          "K⋅m∕s");
        roundtrip(f, "N.m-2.s",          "Pa⋅s");
        roundtrip(f, "K*m/s",            "K⋅m∕s");
        roundtrip(f, "kg/kg*Pa/s",       "Pa∕s");           // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "kg/kg*m/s",        "m∕s");            // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "kg.kg-1.m.s-1",    "m∕s");            // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "kg/kg*kg/kg",      "kg∕kg");          // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "day",              "d");
        roundtrip(f, "µg.m-3",           "µg∕m³");
        roundtrip(f, "Pa*Pa",            "Pa²");
        roundtrip(f, "m-2.s-1",          "1∕(m²⋅s)");
        roundtrip(f, "m-2.s.rad-1",      "s∕m²");           // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "°",                "°");
        roundtrip(f, "K*Pa/s",           "K⋅Pa∕s");
        roundtrip(f, "kg.kg-1",          "kg∕kg");
        roundtrip(f, "m3.m-3",           "m³∕m³");
        roundtrip(f, "m3.s-1.m-1",       "m²∕s");           // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "s.m-1",            "s∕m");
        roundtrip(f, "V.m-1",            "V∕m");
        roundtrip(f, "m2.s-2",           "m²∕s²");
        roundtrip(f, "m2.s-1",           "m²∕s");
        roundtrip(f, "mol.m-3",          "mol∕m³");
        roundtrip(f, "J.m-2",            "J∕m²");
        roundtrip(f, "psu",              "psu");
        roundtrip(f, "kg-2.s-1",         "1∕(kg²⋅s)");
        roundtrip(f, "K*K",              "K²");
        roundtrip(f, "kg.m-3.s-1",       "kg∕(m³⋅s)");
        roundtrip(f, "m.rad-1",          "m∕rad");
        roundtrip(f, "rad.s-1",          "rad∕s");
        roundtrip(f, "(m2.s)^-1",        "1∕(m²⋅s)");
        roundtrip(f, "(m2.s)-1",         "1∕(m²⋅s)");
        roundtrip(f, "(m2.s.sr)-1",      "1∕(m²⋅s)");       // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "(kg.m-3).(m.s-1)", "kg∕(m²⋅s)");      // Too aggressive simplification bug (SIS-378)
        roundtrip(f, "cm/day",           "cm∕d");
        roundtrip(f, "W.m-2.nm-1",       "10⁹⋅kg∕(s³⋅m)");  // Too aggressive simplification bug (SIS-378)
    }

    /**
     * Tests parsing and formatting of custom symbol.
     */
    @Test
    public void testParseAndFormatLabel() {
        final Unit<Length> yard  = Units.METRE.multiply(0.9144);
        final Unit<?>      yard2 = yard.pow(2);
        final var f = new UnitFormat(Locale.ENGLISH);
        f.label(yard, "yd");
        roundtrip(f, "yd",    "yd",  yard);
        roundtrip(f, "yd**2", "yd²", yard2);
        roundtrip(f, "yd^2",  "yd²", yard2);
        roundtrip(f, "yd2",   "yd²", yard2);
        roundtrip(f, "yd²",   "yd²", yard2);
    }

    /**
     * Reminder for units parsing and formatting that still need improvement.
     * The "expected" values checked in this method are not really what we expect,
     * but they reflect the current behavior of Apache SIS units library. We keep
     * those tests as a reminder of work to do, but they should be modified if SIS
     * support of those units is improved in a future version.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-378">SIS-378</a>
     */
    @Test
    public void needForImprovements() {
        final var f = new UnitFormat(Locale.UK);
        roundtrip(f, "kg.kg-1.m.s-1",    "m∕s");
        roundtrip(f, "(m2.s.sr)-1",      "1∕(m²⋅s)");
        roundtrip(f, "m-2.s.rad-1",      "s∕m²");
        roundtrip(f, "kg.kg-1.s-1",      "Hz");
        roundtrip(f, "kg/kg*kg/kg",      "kg∕kg");
        roundtrip(f, "W.m-2.Hz-1",       "kg∕s²");
        roundtrip(f, "W.sr-1.m-2",       "kg∕s³");
        roundtrip(f, "W.m-1.sr-1",       "W∕m");
        roundtrip(f, "W.m-3.sr-1",       "W∕m³");
        roundtrip(f, "m3.s-1.m-1",       "m²∕s");
        roundtrip(f, "(kg.m-3)*(m.s-1)", "kg∕(m²⋅s)");
        roundtrip(f, "W.m-2.nm-1",       "10⁹⋅kg∕(s³⋅m)");
    }

    /**
     * Parses the given symbol, then reformat the unit and compares with expected symbol.
     */
    private static void roundtrip(final UnitFormat f, final String symbol, final String expected) {
        final Unit<?> unit = f.parse(symbol);
        final String actual = f.format(unit);
        assertEquals(expected, actual);
    }

    /**
     * Sames as {@link #roundtrip(UnitFormat, String, String)}, but also compare with the given units ignoring symbol.
     */
    private static void roundtrip(final UnitFormat f, final String symbol, final String expected, final Unit<?> reference) {
        final Unit<?> unit = f.parse(symbol);
        assertEqualsIgnoreSymbol(reference, unit);
        final String actual = f.format(unit);
        assertEquals(expected, actual);
    }

    /**
     * Asserts that the given units are equal, ignoring symbol.
     */
    private static void assertEqualsIgnoreSymbol(final Unit<?> actual, final Unit<?> expected) {
        assertTrue(((AbstractUnit<?>) expected).equals(actual, ComparisonMode.DEBUG));
    }
}
