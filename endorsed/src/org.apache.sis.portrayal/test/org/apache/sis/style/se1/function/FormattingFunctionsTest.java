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
package org.apache.sis.style.se1.function;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;


/**
 * Tests the Symbology Encoding formatting functions: {@link FormatNumber}, {@link FormatDate}, {@link Substring},
 * {@link Concatenate}, {@link ChangeCase}, {@link Trim}, {@link StringPosition} and {@link StringLength}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class FormattingFunctionsTest extends TestCase {
    /**
     * The factory of filters and expressions.
     */
    private final FilterFactory<Feature,Object,Object> ff;

    /**
     * The type of features used for the tests.
     */
    private final FeatureType type;

    /**
     * Creates a new test case.
     */
    public FormattingFunctionsTest() {
        ff = DefaultFilterFactory.forFeatures();
        final var ftb = new FeatureTypeBuilder().setName("City");
        ftb.addAttribute(String.class).setName("name");
        ftb.addAttribute(Integer.class).setName("code");
        ftb.addAttribute(Double.class).setName("population");
        ftb.addAttribute(Object.class).setName("date");
        type = ftb.build();
    }

    /**
     * Creates a feature with the given property values.
     */
    private Feature city(final String name, final Integer code, final Double population, final Object date) {
        final Feature feature = type.newInstance();
        feature.setPropertyValue("name", name);
        feature.setPropertyValue("code", code);
        feature.setPropertyValue("population", population);
        feature.setPropertyValue("date", date);
        return feature;
    }

    /**
     * Creates a function by its name in the filter factory, and verifies that it can be recreated from its parameters.
     */
    @SuppressWarnings("unchecked")
    private Expression<Feature,?> function(final String name, final Expression<?,?>... parameters) {
        final Expression<Feature,?> fn = ff.function(name, (Expression<Feature,?>[]) parameters);
        assertEquals(name, fn.getFunctionName().tip().toString());
        assertEquals(fn, ff.function(name, fn.getParameters().toArray(Expression[]::new)), "Recreated from parameters.");
        return fn;
    }

    /**
     * Applies the given function on a feature without properties.
     */
    private Object apply(final Expression<Feature,?> fn) {
        return fn.apply(type.newInstance());
    }

    /**
     * Tests {@link FormatNumber}.
     */
    @Test
    public void testFormatNumber() {
        final var fn = new FormatNumber<>(ff.property("population"), ff.literal("#,##0.00"), null, null, null);
        assertEquals("1,234,567.89", fn.apply(city(null, null, 1234567.891, null)));
        assertEquals("-5.00",        fn.apply(city(null, null, -5.0, null)), "Minus sign prefixed by default.");
        assertNull(fn.apply(city(null, null, null, null)), "Missing value.");
        assertEquals("12.50", new FormatNumber<Feature>(ff.literal("12.5"), ff.literal("0.00"), null, null, null).apply(null),
                     "Conversion of string to number.");

        // Decimal point and grouping separator, which are also the characters used in the pattern as in XSLT.
        var custom = new FormatNumber<>(ff.property("population"), ff.literal("#.##0,00"), null, ff.literal(","), ff.literal("."));
        assertEquals("1.234.567,89", custom.apply(city(null, null, 1234567.891, null)));

        // Negative pattern, exponent and quoting.
        custom = new FormatNumber<>(ff.property("population"), ff.literal("0.0"), ff.literal("(0.0)"), null, null);
        assertEquals("(5.0)", custom.apply(city(null, null, -5.0, null)));
        assertEquals("1.234E3", apply(new FormatNumber<>(ff.literal(1234), ff.literal("0.###E0"), null, null, null)));
        assertEquals("#5",      apply(new FormatNumber<>(ff.literal(5),    ff.literal("'#'0"),    null, null, null)));

        // By name.
        final var byName = function("FormatNumber", ff.property("population"), ff.literal("# ##0,0"),
                                    ff.literal(""), ff.literal(","), ff.literal(" "));
        assertEquals("1 234 567,9", byName.apply(city(null, null, 1234567.891, null)));
    }

    /**
     * Tests {@link FormatDate} with the example of the specification and the other pattern elements.
     */
    @Test
    public void testFormatDate() {
        final var date = LocalDateTime.of(2006, 2, 20, 13, 5, 9);
        assertEquals("20.02.2006", format(date, "DD.MM.YYYY"));
        assertEquals("20/2/06",    format(date, "D/M/YY"));
        assertEquals("FEB 2006",   format(date, "MMM YYYY"));
        assertEquals("February",   format(date, "MMMMM"));
        assertEquals("Februar",    format(date, "MMMMMde"));
        assertEquals("DEZ",        format(LocalDateTime.of(2006, 12, 1, 0, 0), "MMMde"));
        assertEquals("13:05:09",   format(date, "hh:mm:ss"));
        assertEquals("13:5:9",     format(date, "h:m:s"), "24-hour clock without am/pm marker.");
        assertEquals("1:05 PM",    format(date, "h:mm a"));
        assertEquals("2006-02-20T13:05", format(date, "YYYY-MM-DD\\Thh:mm"));
        assertEquals("PST", format(ZonedDateTime.of(2006, 2, 20, 13, 0, 0, 0, ZoneId.of("America/Los_Angeles")), "z"));
        assertEquals("2006-02-20T13:05:09", format(date, null), "ISO 8601 without pattern.");
        assertEquals("1 a.m.", format(date.withHour(1), "h \\a.\\m."), "Quoted letters are not am/pm markers.");
        assertEquals("13 februar", format(date, "h MMMMMda").toLowerCase(java.util.Locale.ROOT), "Language code is not am/pm marker.");
    }

    /**
     * Tests {@link FormatDate} with the different kinds of date values.
     */
    @Test
    public void testFormatDateValues() {
        assertEquals("20.02.2006", format("2006-02-20", "DD.MM.YYYY"), "XML schema date.");
        assertEquals("20.02.2006 13:05", format("2006-02-20T13:05:00Z", "DD.MM.YYYY hh:mm"), "XML schema dateTime.");
        assertEquals("02/2006", format("2006-02", "MM/YYYY"), "XML schema gYearMonth.");
        assertEquals("20.02",   format("--02-20", "DD.MM"),   "XML schema gMonthDay.");
        assertEquals("20",      format("---20", "DD"),        "XML schema gDay.");
        assertEquals("FEB",     format("--02", "MMM"),        "XML schema gMonth.");
        assertEquals("2006",    format("2006", "YYYY"),       "XML schema gYear.");
        assertEquals("20.02.2006 GMT+01:00", format("2006-02-20+01:00", "DD.MM.YYYY z"), "XML schema date with time zone.");
        assertEquals("13:05 GMT", format("13:05:00Z", "hh:mm z"), "XML schema time with time zone.");
        assertEquals("2006-02",  format("2006-02", null), "Default representation of gYearMonth.");
        assertEquals("--02-20",  format("--02-20", null), "Default representation of gMonthDay.");
        assertEquals("---20",    format("---20",   null), "Default representation of gDay.");
        assertEquals("13:05:00", format("13:05:00Z", null), "Default representation of time.");
        assertEquals("2006-02-20T13:05:00", format("2006-02-20T13:05:00Z", null), "Default representation of dateTime.");
        assertEquals("1970-01-01", format(new Date(0), "YYYY-MM-DD"), "Date in UTC.");
        assertEquals("20.02.2006 00:00", format("2006-02-20", "DD.MM.YYYY hh:mm"), "Missing time fields are zero.");
        assertNull(format("not a date", "YYYY"));
        assertNull(format(null, "YYYY"));

        // By name, on a feature.
        final var fn = function("FormatDate", ff.property("date"), ff.literal("DD.MM.YYYY"));
        assertEquals("20.02.2006", fn.apply(city(null, null, null, "2006-02-20")));
    }

    /**
     * Formats the given date with the given pattern.
     */
    private String format(final Object date, final String pattern) {
        return new FormatDate<Feature>(ff.literal(date), (pattern != null) ? ff.literal(pattern) : null).apply(null);
    }

    /**
     * Tests {@link Substring}, including the friendly handling of invalid positions and lengths.
     */
    @Test
    public void testSubstring() {
        assertEquals("World", substring("Hello World", 7, null));
        assertEquals("Hello", substring("Hello World", 1, 5));
        assertEquals("Hello", substring("Hello World", null, 5), "Default position is 1.");
        assertEquals("World", substring("Hello World", 7, 100), "Existing part of a too long substring.");
        assertEquals("",      substring("Hello World", 0, 5),   "Position lower or equal to 0.");
        assertEquals("",      substring("Hello World", 2, 0),   "Length lower or equal to 0.");
        assertEquals("",      substring("Hello World", 20, 2),  "Position after the end.");
        assertEquals("23",    substring(12345, 2, 2),           "Conversion of number to string.");
        assertEquals("\uD834\uDD1Eb", substring("a\uD834\uDD1Eb", 2, 2), "Counting in code points.");
        assertNull(substring(null, 1, 1));

        final var fn = function("Substring", ff.property("name"), ff.literal(1), ff.literal(3));
        assertEquals("Par", fn.apply(city("Paris", null, null, null)));
    }

    private String substring(final Object text, final Integer position, final Integer length) {
        return new Substring<Feature>(ff.literal(text),
                (position != null) ? ff.literal(position) : null,
                (length   != null) ? ff.literal(length)   : null).apply(null);
    }

    /**
     * Tests {@link Concatenate}.
     */
    @Test
    public void testConcatenate() {
        final var fn = new Concatenate<>(List.of(ff.property("name"), ff.literal(" ("), ff.property("code"), ff.literal(")")));
        assertEquals("Paris (75)", fn.apply(city("Paris", 75, null, null)));
        assertEquals("Paris ()",   fn.apply(city("Paris", null, null, null)), "Null values are ignored.");
        assertThrows(IllegalArgumentException.class, () -> new Concatenate<Feature>(List.of()));

        final var byName = function("Concatenate", ff.property("name"), ff.literal("-"), ff.property("code"));
        assertEquals("Lyon-69", byName.apply(city("Lyon", 69, null, null)));
    }

    /**
     * Tests {@link ChangeCase}.
     */
    @Test
    public void testChangeCase() {
        assertEquals("PARIS", new ChangeCase<>(ff.property("name"), null).apply(city("Paris", null, null, null)), "Default is upper case.");
        assertEquals("paris", new ChangeCase<>(ff.property("name"), ChangeCase.Direction.TO_LOWER).apply(city("Paris", null, null, null)));
        assertNull(new ChangeCase<>(ff.property("name"), null).apply(city(null, null, null, null)));

        final var fn = function("ChangeCase", ff.property("name"), ff.literal("toLower"));
        assertEquals("lyon", fn.apply(city("LYON", null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> ff.function("ChangeCase", ff.property("name"), ff.literal("sideways")));
    }

    /**
     * Tests {@link Trim}.
     */
    @Test
    public void testTrim() {
        assertEquals("abc  ", trim("  abc  ", null, null), "Default is leading blanks.");
        assertEquals("  abc", trim("  abc  ", Trim.StripOffPosition.TRAILING, null));
        assertEquals("abc",   trim("  abc  ", Trim.StripOffPosition.BOTH, null));
        assertEquals("123",   trim("000123", null, "0"));
        assertEquals("",      trim("0000", Trim.StripOffPosition.BOTH, "0"));
        assertNull(trim(null, null, null));

        final var fn = function("Trim", ff.property("name"), ff.literal("both"), ff.literal("*"));
        assertEquals("Paris", fn.apply(city("**Paris***", null, null, null)));
    }

    private String trim(final String text, final Trim.StripOffPosition position, final String chars) {
        return new Trim<Feature>(ff.literal(text), position, (chars != null) ? ff.literal(chars) : null).apply(null);
    }

    /**
     * Tests {@link StringPosition}.
     */
    @Test
    public void testStringPosition() {
        assertEquals(5, position("o", "Hello World", null), "Counting from 1.");
        assertEquals(8, position("o", "Hello World", StringPosition.SearchDirection.BACK_TO_FRONT));
        assertEquals(7, position("World", "Hello World", null));
        assertEquals(0, position("x", "Hello World", null), "Zero in case of search failure.");
        assertEquals(0, position("x", null, null));

        final var fn = function("StringPosition", ff.literal("-"), ff.property("name"), ff.literal("backToFront"));
        assertEquals(7, fn.apply(city("Aix-en-Provence", null, null, null)));
    }

    private Integer position(final String lookup, final String text, final StringPosition.SearchDirection direction) {
        return new StringPosition<Feature>(ff.literal(lookup), ff.literal(text), direction).apply(null);
    }

    /**
     * Tests {@link StringLength}.
     */
    @Test
    public void testStringLength() {
        assertEquals(5, new StringLength<>(ff.property("name")).apply(city("Paris", null, null, null)));
        assertEquals(2, apply(new StringLength<>(ff.literal("\uD834\uDD1Ea"))), "Counting in code points.");
        assertEquals(5, apply(new StringLength<>(ff.literal(12345))), "Conversion of number to string.");
        assertNull(new StringLength<>(ff.property("name")).apply(city(null, null, null, null)));

        final var fn = function("StringLength", ff.property("name"));
        assertEquals(4, fn.apply(city("Lyon", null, null, null)));
    }

    /**
     * Tests the combination of functions, for example a label made of the upper-cased first word of a name.
     */
    @Test
    public void testCombination() {
        final var firstWord = new Substring<>(ff.property("name"), null,
                new StringPosition<>(ff.literal(" "), ff.property("name"), null));
        final var label = new Concatenate<>(List.of(new ChangeCase<>(new Trim<>(firstWord, Trim.StripOffPosition.TRAILING, null), null),
                ff.literal(": "), new FormatNumber<>(ff.property("population"), ff.literal("#,##0"), null, null, null)));
        assertEquals("SAINT: 1,234", label.apply(city("Saint Etienne", null, 1234.0, null)));
    }
}
