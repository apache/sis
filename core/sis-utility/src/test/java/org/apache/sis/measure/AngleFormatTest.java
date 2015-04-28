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
import java.text.FieldPosition;
import java.text.AttributedCharacterIterator;
import java.text.ParseException;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.*;


/**
 * Tests parsing and formatting done by the {@link AngleFormat} class.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@DependsOn({
  FormattedCharacterIteratorTest.class,
  org.apache.sis.math.MathFunctionsTest.class
})
public final strictfp class AngleFormatTest extends TestCase {
    /**
     * Tests a pattern with illegal usage of D, M and S symbols.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalPattern() {
        final AngleFormat f = new AngleFormat(Locale.CANADA);
        f.applyPattern("DD°SS′MM″");
    }

    /**
     * Tests an illegal pattern with illegal symbols for the fraction part.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFractionPattern() {
        final AngleFormat f = new AngleFormat(Locale.CANADA);
        f.applyPattern("DD°MM′SS.m″");
    }

    /**
     * Tests a {@code '?'} symbol without suffix.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalOptionalField() {
        final AngleFormat f = new AngleFormat(Locale.CANADA);
        f.applyPattern("DD°MM?SS.m″");
    }

    /**
     * Tests a {@code '?'} symbol without suffix.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalOptionalLastField() {
        final AngleFormat f = new AngleFormat(Locale.CANADA);
        f.applyPattern("DD°MM?");
    }

    /**
     * Tests using {@link Locale#CANADA}.
     */
    @Test
    public void testCanadaLocale() {
        final AngleFormat f = new AngleFormat("DD.ddd°", Locale.CANADA);
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
        final AngleFormat f = new AngleFormat("DD.ddd°", Locale.FRANCE);
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
        final AngleFormat f = new AngleFormat("DDddd", Locale.CANADA);
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
    @DependsOnMethod("testCanadaLocale")
    public void testDegreeMinutes() {
        final AngleFormat f = new AngleFormat("DD°MM.m", Locale.CANADA);
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
    @DependsOnMethod("testDegreeMinutes")
    public void testDegreeMinutesSeconds() {
        final AngleFormat f = new AngleFormat("DD°MM′SS.sss″", Locale.CANADA);
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
    @DependsOnMethod("testDegreeMinutesSeconds")
    public void testRounding() {
        final AngleFormat f = new AngleFormat("DD°MM′SS.sss″", Locale.CANADA);
        assertEquals( "01°00′00.000″", f.format(new Angle(+(59 + (59.9999 / 60)) / 60)));
        assertEquals("-01°00′00.000″", f.format(new Angle(-(59 + (59.9999 / 60)) / 60)));
        assertEquals("-00°59′59.999″", f.format(new Angle(-(59 + (59.9988 / 60)) / 60)));
    }

    /**
     * Tests with optional minutes and seconds fields.
     */
    @Test
    @DependsOnMethod("testDegreeMinutesSeconds")
    public void testOptionalFields() {
        final AngleFormat f = new AngleFormat(Locale.CANADA);
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
     * @throws ParseException If a string can not be parsed.
     */
    @Test
    @DependsOnMethod("testOptionalFields")
    public void testJavadocExamples() throws ParseException {
        final AngleFormat f = new AngleFormat(Locale.CANADA);
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
     * @param f       The angle format to test.
     * @param pattern The pattern to apply for the test.
     * @param e1      The expected string value of 48.5.
     * @param e2      The expected string value of -12.53125.
     * @param eps     The tolerance for comparing the parsed value of {@code e2}.
     */
    private static void testExample(final AngleFormat f, final String pattern, final String e1, final String e2,
            final double eps) throws ParseException
    {
        f.applyPattern(pattern);
        assertEquals("toPattern()", pattern, f.toPattern());
        assertEquals("format(double)", e1,       f.format(48.5));
        assertEquals("format(double)", e2,       f.format(-12.53125));
        assertEquals("parse(String)",  48.5,     f.parse(e1).degrees(), 0.0);
        assertEquals("parse(String)", -12.53125, f.parse(e2).degrees(), eps);
    }

    /**
     * Tests with optional digits.
     */
    @Test
    @DependsOnMethod("testDegreeMinutesSeconds")
    public void testOptionalFractionDigits() {
        final AngleFormat f = new AngleFormat("DD°MM′SS.s##″", Locale.CANADA);
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
    @DependsOnMethod("testOptionalFractionDigits")
    public void testSetMaximumWidth() {
        final AngleFormat f = new AngleFormat("D°MM′SS.################″", Locale.CANADA);
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
     * Tests the field position while formatting an angle.
     */
    @Test
    public void testFieldPosition() {
        final Latitude latitude = new Latitude(FormattedCharacterIteratorTest.LATITUDE_VALUE);
        final AngleFormat f = new AngleFormat("DD°MM′SS.s″", Locale.CANADA);
        final StringBuffer buffer = new StringBuffer();
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
            final FieldPosition pos = new FieldPosition(field);
            assertEquals(FormattedCharacterIteratorTest.LATITUDE_STRING, f.format(latitude, buffer, pos).toString());
            assertSame  ("getFieldAttribute", field, pos.getFieldAttribute());
            assertEquals("getBeginIndex",     start, pos.getBeginIndex());
            assertEquals("getEndIndex",       limit, pos.getEndIndex());
            buffer.setLength(0);
        }
    }

    /**
     * Tests the {@link AngleFormat#formatToCharacterIterator(Object)} method.
     */
    @Test
    @DependsOnMethod("testFieldPosition")
    public void testFormatToCharacterIterator() {
        final Latitude latitude = new Latitude(FormattedCharacterIteratorTest.LATITUDE_VALUE);
        final AngleFormat f = new AngleFormat("DD°MM′SS.s″", Locale.CANADA);
        final AttributedCharacterIterator it = f.formatToCharacterIterator(latitude);
        assertEquals(FormattedCharacterIteratorTest.LATITUDE_STRING, it.toString());
        FormattedCharacterIteratorTest.testAttributes(it, true);
    }
}
