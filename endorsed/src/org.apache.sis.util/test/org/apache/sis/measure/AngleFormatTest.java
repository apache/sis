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

import java.util.Locale;
import java.math.RoundingMode;
import java.text.FieldPosition;
import java.text.AttributedCharacterIterator;
import java.text.ParseException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests parsing and formatting done by the {@link AngleFormat} class.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 */
@SuppressWarnings("exports")
public final class AngleFormatTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AngleFormatTest() {
    }

    /**
     * Formats the given value using the given formatter, and parses the text back to its value.
     * If the parsed value is not equal to the original one, an {@link AssertionError} is thrown.
     *
     * @param  formatter  the formatter to use for formatting and parsing.
     * @param  value      the value to format.
     * @return the formatted value.
     */
    private static String formatAndParse(final AngleFormat formatter, final Object value) {
        final String text = formatter.format(value);
        final Object parsed;
        try {
            parsed = formatter.parseObject(text);
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
        assertEquals(value, parsed, "Parsed text not equal to the original value");
        return text;
    }

    /**
     * Tests a pattern with illegal usage of D, M and S symbols.
     */
    @Test
    public void testIllegalPattern() {
        final var f = new AngleFormat(Locale.CANADA);
        var e = assertThrows(IllegalArgumentException.class, () -> f.applyPattern("DD°SS′MM″"));
        assertMessageContains(e);
    }

    /**
     * Tests an illegal pattern with illegal symbols for the fraction part.
     */
    public void testIllegalFractionPattern() {
        final var f = new AngleFormat(Locale.CANADA);
        var e = assertThrows(IllegalArgumentException.class, () -> f.applyPattern("DD°MM′SS.m″"));
        assertMessageContains(e);
    }

    /**
     * Tests a {@code '?'} symbol without suffix.
     */
    @Test
    public void testIllegalOptionalField() {
        final var f = new AngleFormat(Locale.CANADA);
        var e = assertThrows(IllegalArgumentException.class, () -> f.applyPattern("DD°MM?SS.m″"));
        assertMessageContains(e);
    }

    /**
     * Tests a {@code '?'} symbol without suffix.
     */
    @Test
    public void testIllegalOptionalLastField() {
        final var f = new AngleFormat(Locale.CANADA);
        var e = assertThrows(IllegalArgumentException.class, () -> f.applyPattern("DD°MM?"));
        assertMessageContains(e);
    }

    /**
     * Tests using {@link Locale#CANADA}.
     */
    @Test
    public void testCanadaLocale() {
        final var f = new AngleFormat("DD.ddd°", Locale.CANADA);
        assertEquals(3, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DD.ddd°",  f.toPattern());
        assertEquals( "20.000°",  formatAndParse(f, new Angle   ( 20.000)));
        assertEquals( "20.749°",  formatAndParse(f, new Angle   ( 20.749)));
        assertEquals("-12.247°",  formatAndParse(f, new Angle   (-12.247)));
        assertEquals( "13.214°N", formatAndParse(f, new Latitude( 13.214)));
        assertEquals( "12.782°S", formatAndParse(f, new Latitude(-12.782)));
        assertEquals("-00.010°",  formatAndParse(f, new Angle   (-0.01)));
    }

    /**
     * Tests using {@link Locale#FRANCE}.
     */
    @Test
    public void testFranceLocale() {
        final var f = new AngleFormat("DD.ddd°", Locale.FRANCE);
        assertEquals(3, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DD.ddd°",  f.toPattern());
        assertEquals( "19,457°E", formatAndParse(f, new Longitude( 19.457)));
        assertEquals( "78,124°S", formatAndParse(f, new Latitude (-78.124)));
        assertEquals("-00,010°",  formatAndParse(f, new Angle    (-0.01)));
    }

    /**
     * Tests with no decimal separator.
     */
    @Test
    public void testNoSeparator() {
        final var f = new AngleFormat("DDddd", Locale.CANADA);
        assertEquals(3, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DDddd",  f.toPattern());
        assertEquals( "19457E", formatAndParse(f, new Longitude( 19.457)));
        assertEquals( "78124S", formatAndParse(f, new Latitude (-78.124)));
        assertEquals("-00010",  formatAndParse(f, new Angle    (-0.01)));
    }

    /**
     * Tests with a minute fields.
     */
    @Test
    public void testDegreeMinutes() {
        final var f = new AngleFormat("DD°MM.m", Locale.CANADA);
        assertEquals(1, f.getMinimumFractionDigits());
        assertEquals(1, f.getMaximumFractionDigits());
        assertEquals( "DD°MM.m", f.toPattern());
        assertEquals( "12°30.0", formatAndParse(f, new Angle( 12.50)));
        assertEquals("-10°15.0", formatAndParse(f, new Angle(-10.25)));
        assertEquals("-00°00.6", formatAndParse(f, new Angle( -0.01)));
        assertEquals( "89°01.0", formatAndParse(f, new Angle( 89.01666666666667)));
    }

    /**
     * Tests with a seconds fields.
     */
    @Test
    public void testDegreeMinutesSeconds() {
        final var f = new AngleFormat("DD°MM′SS.sss″", Locale.CANADA);
        assertEquals(3, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DD°MM′SS.sss″", f.toPattern());
        assertEquals( "12°30′56.250″", formatAndParse(f, new Angle( 12.515625)));
        assertEquals("-12°30′56.250″", formatAndParse(f, new Angle(-12.515625)));
        assertEquals("-00°00′36.000″", formatAndParse(f, new Angle( -0.01)));
        assertEquals( "89°01′00.000″", formatAndParse(f, new Angle( 89.01666666666667)));
    }

    /**
     * Tests values that have to be rounded, especially the values near zero.
     */
    @Test
    public void testRounding() {
        final var f = new AngleFormat("DD°MM′SS.sss″", Locale.CANADA);
        assertEquals( "01°00′00.000″", f.format(new Angle(+(59 + (59.9999 / 60)) / 60)));
        assertEquals("-01°00′00.000″", f.format(new Angle(-(59 + (59.9999 / 60)) / 60)));
        assertEquals("-00°59′59.999″", f.format(new Angle(-(59 + (59.9988 / 60)) / 60)));
    }

    /**
     * Tests with optional minutes and seconds fields.
     */
    @Test
    public void testOptionalFields() {
        final var f = new AngleFormat(Locale.CANADA);
        assertEquals("D°?MM′?SS.################″?", f.toPattern());
        assertEquals("12°",          formatAndParse(f, new Angle(12)));
        assertEquals("12°30′",       formatAndParse(f, new Angle(12.5)));
        assertEquals("12°36″",       formatAndParse(f, new Angle(12.01)));
        assertEquals("12°00′36″N",   formatAndParse(f, new Latitude(12.01)));
        assertEquals("12°30′56.25″", formatAndParse(f, new Angle(12.515625)));
        assertEquals("-36″",         formatAndParse(f, new Angle(-0.01)));
        assertEquals("0°00′36″S",    formatAndParse(f, new Latitude(-0.01)));
    }

    /**
     * Tests the example provided in the {@link AngleFormat} javadoc.
     *
     * @throws ParseException if a string cannot be parsed.
     */
    @Test
    public void testJavadocExamples() throws ParseException {
        final var f = new AngleFormat(Locale.CANADA);
        testExample(f, "DD°MM′SS.#″",   "48°30′00″", "-12°31′52.5″", 0.000);
        testExample(f, "DD°MM′",        "48°30′",    "-12°32′",      0.003);
        testExample(f, "DD.ddd",        "48.500",    "-12.531",      2.500);
        testExample(f, "DD.###",        "48.5",      "-12.531",      2.500);
        testExample(f, "DDMM",          "4830",      "-1232",        0.003);
        testExample(f, "DDMMSSs",       "4830000",   "-1231525",     0.000);
        testExample(f, "DD°MM′?SS.s″?", "48°30′",    "-12°31′52.5″", 0.000);
    }

    /**
     * Tests a single line of Javadoc examples.
     *
     * @param f        the angle format to test.
     * @param pattern  the pattern to apply for the test.
     * @param e1       the expected string value of 48.5.
     * @param e2       the expected string value of -12.53125.
     * @param eps      the tolerance for comparing the parsed value of {@code e2}.
     */
    private static void testExample(final AngleFormat f, final String pattern, final String e1, final String e2,
            final double eps) throws ParseException
    {
        f.applyPattern(pattern);
        assertEquals(pattern,   f.toPattern());
        assertEquals( e1,       f.format(48.5));
        assertEquals( e2,       f.format(-12.53125));
        assertEquals( 48.5,     f.parse(e1).degrees());
        assertEquals(-12.53125, f.parse(e2).degrees(), eps);
    }

    /**
     * Tests formatting the same value with different rounding modes.
     */
    @Test
    public void testRoundingMode() {
        final var f = new AngleFormat("DD°MM′SS″", Locale.CANADA);
        Angle angle = new Angle(12.515625);
        f.setRoundingMode(RoundingMode.DOWN);      assertEquals("12°30′56″", f.format(angle));
        f.setRoundingMode(RoundingMode.UP);        assertEquals("12°30′57″", f.format(angle));
        f.setRoundingMode(RoundingMode.FLOOR);     assertEquals("12°30′56″", f.format(angle));
        f.setRoundingMode(RoundingMode.CEILING);   assertEquals("12°30′57″", f.format(angle));
        f.setRoundingMode(RoundingMode.HALF_EVEN); assertEquals("12°30′56″", f.format(angle));

        angle = new Angle(-12.515625);
        f.setRoundingMode(RoundingMode.DOWN);      assertEquals("-12°30′56″", f.format(angle));
        f.setRoundingMode(RoundingMode.UP);        assertEquals("-12°30′57″", f.format(angle));
        f.setRoundingMode(RoundingMode.FLOOR);     assertEquals("-12°30′57″", f.format(angle));
        f.setRoundingMode(RoundingMode.CEILING);   assertEquals("-12°30′56″", f.format(angle));
        f.setRoundingMode(RoundingMode.HALF_EVEN); assertEquals("-12°30′56″", f.format(angle));
    }

    /**
     * Tests with optional digits.
     */
    @Test
    public void testOptionalFractionDigits() {
        final var f = new AngleFormat("DD°MM′SS.s##″", Locale.CANADA);
        assertEquals(1, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DD°MM′SS.s##″", f.toPattern());
        assertEquals( "12°30′56.25″",  formatAndParse(f, new Angle( 12.515625)));
        assertEquals("-12°30′56.25″",  formatAndParse(f, new Angle(-12.515625)));
        assertEquals( "12°31′52.5″",   formatAndParse(f, new Angle( 12.53125 )));
        assertEquals("-12°31′52.5″",   formatAndParse(f, new Angle(-12.53125 )));
        assertEquals( "12°33′45.0″",   formatAndParse(f, new Angle( 12.5625  )));
        assertEquals("-12°33′45.0″",   formatAndParse(f, new Angle(-12.5625  )));

        f.applyPattern("DD°MM′SS.###″");
        assertEquals(0, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DD°MM′SS.###″", f.toPattern());
        assertEquals( "12°33′45″", formatAndParse(f, new Angle( 12.5625)));
        assertEquals("-12°33′45″", formatAndParse(f, new Angle(-12.5625)));
    }

    /**
     * Tests the {@link AngleFormat#setMaximumWidth(int)} method.
     */
    @Test
    public void testSetMaximumWidth() {
        final var f = new AngleFormat("D°MM′SS.################″", Locale.CANADA);
        assertEquals("D°MM′SS.################″", f.toPattern());

        f.setMaximumWidth(12);
        assertEquals("D°MM′SS.###″", f.toPattern());
        assertEquals("8°07′24.442″", f.format(new Angle( 8.123456)));
        assertEquals("20°07′24.44″", f.format(new Angle(20.123456)));

        f.setMaximumWidth(10);
        assertEquals("D°MM′SS.#″", f.toPattern());
        assertEquals("8°07′24.4″", f.format(new Angle( 8.123456)));
        assertEquals("20°07′24″",  f.format(new Angle(20.123456)));

        f.setMaximumWidth(9);
        assertEquals("D°MM′SS″", f.toPattern());
        f.setMaximumWidth(8);
        assertEquals("D°MM′SS″", f.toPattern());

        // Test the drop of seconds field.
        f.setMaximumFractionDigits(6);
        f.setMaximumWidth(7);
        assertEquals("D°MM.#′",  f.toPattern());
        assertEquals("8°07.4′",  f.format(new Angle( 8.123456)));
        assertEquals("20°07.4′", f.format(new Angle(20.123456)));

        f.setMaximumWidth(6);
        assertEquals("D°MM′",  f.toPattern());
        assertEquals("8°07′",  f.format(new Angle( 8.123456)));
        assertEquals("20°07′", f.format(new Angle(20.123456)));

        // Test the drop of minutes field.
        f.setMaximumFractionDigits(6);
        f.setMaximumWidth(4);
        assertEquals("D.#°", f.toPattern());
        assertEquals("8.1°", f.format(new Angle( 8.123456)));
        assertEquals("20°",  f.format(new Angle(20.123456)));

        f.setMaximumWidth(3);
        assertEquals("D°",  f.toPattern());
        assertEquals("8°",  f.format(new Angle( 8.123456)));
        assertEquals("20°", f.format(new Angle(20.123456)));
    }

    /**
     * Tests {@link AngleFormat#setPrecision(double, boolean)}.
     */
    @Test
    public void testSetPrecision() {
        final var f = new AngleFormat(Locale.CANADA);
        f.setPrecision(1,        true); assertEquals("D°",         f.toPattern());
        f.setPrecision(1./10,    true); assertEquals("D.d°",       f.toPattern());
        f.setPrecision(1./60,    true); assertEquals("D°MM′",      f.toPattern());
        f.setPrecision(1./600,   true); assertEquals("D°MM.m′",    f.toPattern());
        f.setPrecision(1./3600,  true); assertEquals("D°MM′SS″",   f.toPattern());
        f.setPrecision(1./4000,  true); assertEquals("D°MM′SS.s″", f.toPattern());
        f.setPrecision(1./100,   true); assertEquals("D°MM.m′",    f.toPattern());
        f.setPrecision(1./8000, false); assertEquals("D°MM.mmm′",  f.toPattern());
        f.setPrecision(1./1000, false); assertEquals("D°MM.mm′",   f.toPattern());
        f.setPrecision(10,       true); assertEquals("D°",         f.toPattern());
        f.setPrecision(1./1000, false); assertEquals("D.ddd°",     f.toPattern());
        f.setPrecision(1./1001, false); assertEquals("D.dddd°",    f.toPattern());
    }

    /**
     * Tests {@link AngleFormat#getPrecision()}.
     */
    @Test
    public void testGetPrecision() {
        final var f = new AngleFormat(Locale.CANADA);
        f.applyPattern("D°");         assertEquals(     1,   f.getPrecision());
        f.applyPattern("D.dd°");      assertEquals(  0.01,   f.getPrecision(),  1E-16);
        f.applyPattern("D°MM′");      assertEquals(1.0/60,   f.getPrecision(),  1E-16);
        f.applyPattern("D°MM′SS.s″"); assertEquals(0.1/3600, f.getPrecision(),  1E-16);
    }

    /**
     * Tests the field position while formatting an angle.
     */
    @Test
    public void testFieldPosition() {
        final var latitude = new Latitude(FormattedCharacterIteratorTest.LATITUDE_VALUE);
        final var f = new AngleFormat("DD°MM′SS.s″", Locale.CANADA);
        final var buffer = new StringBuffer();
        for (int i=AngleFormat.DEGREES_FIELD; i<=AngleFormat.HEMISPHERE_FIELD; i++) {
            final AngleFormat.Field field;
            final int start, limit;
            switch (i) {
                case AngleFormat.DEGREES_FIELD:    field = AngleFormat.Field.DEGREES;    start= 0; limit= 3;  break;
                case AngleFormat.MINUTES_FIELD:    field = AngleFormat.Field.MINUTES;    start= 3; limit= 6;  break;
                case AngleFormat.SECONDS_FIELD:    field = AngleFormat.Field.SECONDS;    start= 6; limit=11; break;
                case AngleFormat.HEMISPHERE_FIELD: field = AngleFormat.Field.HEMISPHERE; start=11; limit=12; break;
                default: continue; // Skip the fraction field.
            }
            final var pos = new FieldPosition(field);
            assertEquals(FormattedCharacterIteratorTest.LATITUDE_STRING, f.format(latitude, buffer, pos).toString());
            assertSame  (field, pos.getFieldAttribute());
            assertEquals(start, pos.getBeginIndex());
            assertEquals(limit, pos.getEndIndex());
            buffer.setLength(0);
        }
    }

    /**
     * Tests the {@link AngleFormat#formatToCharacterIterator(Object)} method.
     */
    @Test
    public void testFormatToCharacterIterator() {
        final var latitude = new Latitude(FormattedCharacterIteratorTest.LATITUDE_VALUE);
        final var f = new AngleFormat("DD°MM′SS.s″", Locale.CANADA);
        final AttributedCharacterIterator it = f.formatToCharacterIterator(latitude);
        assertEquals(FormattedCharacterIteratorTest.LATITUDE_STRING, it.toString());
        FormattedCharacterIteratorTest.testAttributes(it, true);
    }
}
