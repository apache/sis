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

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.AttributedCharacterIterator;
import static java.lang.StrictMath.*;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.NEGATIVE_INFINITY;
import static org.apache.sis.util.internal.shared.Constants.UTC;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests parsing and formatting done by the {@link RangeFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class RangeFormatTest extends TestCase {
    /**
     * The format being tested.
     */
    private RangeFormat format;

    /**
     * The position of the minimal value and maximal value fields.
     */
    private FieldPosition minPos, maxPos;

    /**
     * The position during parsing.
     */
    private ParsePosition parsePos;

    /**
     * Creates a new test case.
     */
    public RangeFormatTest() {
    }

    /**
     * Formats the given range.
     */
    private String format(final Range<?> range) {
        final String s1 = format.format(range, new StringBuffer(), minPos).toString();
        final String s2 = format.format(range, new StringBuffer(), maxPos).toString();
        assertEquals(s1, s2, "Two consecutive formats produced different results.");
        return s1;
    }

    /**
     * Parses the given text and ensure that there is only whitespace or underscore after the last
     * parse position. Also verifies that there is no underscore before the parse position. This
     * assume that the underscore is not allowed to appears in the valid portion of the text.
     */
    private Range<?> parse(final String text) {
        parsePos.setIndex(0);
        final Range<?> range = format.parse(text, parsePos);
        assertEquals(parsePos.getIndex()      == 0, range == null, "Index position shall be modified on parse success, and only parse success.");
        assertEquals(parsePos.getErrorIndex() >= 0, range == null, "Error position shall be defined on parse failure, and only parse failure");
        if (range != null) {
            int i = parsePos.getIndex();
            assertTrue(text.lastIndexOf('_', i-1) < 0, "The parse method parsed a character that it shouldn't have accepted.");
            while (i < text.length()) {
                final char c = text.charAt(i++);
                assertTrue(Character.isWhitespace(c) || c == '_', "Looks like that the parse method didn't parsed everything.");
            }
        }
        return range;
    }

    /**
     * Tests the {@link RangeFormat#format(Object, StringBuffer, FieldPosition)} method with numbers.
     */
    @Test
    public void testFormatNumbers() {
        format = new RangeFormat(Locale.CANADA);
        minPos = new FieldPosition(RangeFormat.Field.MIN_VALUE);
        maxPos = new FieldPosition(RangeFormat.Field.MAX_VALUE);

        // Closed range
        assertEquals("[-10 … 20]", format(NumberRange.create(-10, true, 20, true)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(4, minPos.getEndIndex());
        assertEquals(7, maxPos.getBeginIndex());
        assertEquals(9, maxPos.getEndIndex());

        // Open range
        assertEquals("(-3 … 4)", format(NumberRange.create(-3, false, 4, false)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(3, minPos.getEndIndex());
        assertEquals(6, maxPos.getBeginIndex());
        assertEquals(7, maxPos.getEndIndex());

        // Half-open range
        assertEquals("[2 … 8)", format(NumberRange.create(2, true, 8, false)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(2, minPos.getEndIndex());
        assertEquals(5, maxPos.getBeginIndex());
        assertEquals(6, maxPos.getEndIndex());

        // Half-open range
        assertEquals("(40 … 90]", format(NumberRange.create(40, false, 90, true)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(3, minPos.getEndIndex());
        assertEquals(6, maxPos.getBeginIndex());
        assertEquals(8, maxPos.getEndIndex());

        // Single value
        assertEquals("{300}", format(NumberRange.create(300, true, 300, true)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(4, minPos.getEndIndex());
        assertEquals(1, maxPos.getBeginIndex());
        assertEquals(4, maxPos.getEndIndex());

        // Empty range
        assertEquals("{}", format(NumberRange.create(300, true, 300, false)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(1, minPos.getEndIndex());
        assertEquals(1, maxPos.getBeginIndex());
        assertEquals(1, maxPos.getEndIndex());

        // Negative infinity
        assertEquals("(−∞ … 30]", format(NumberRange.create(Double.NEGATIVE_INFINITY, true, 30, true)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(3, minPos.getEndIndex());
        assertEquals(6, maxPos.getBeginIndex());
        assertEquals(8, maxPos.getEndIndex());

        // Positive infinity
        assertEquals("[50 … ∞)", format(NumberRange.create(50, true, Double.POSITIVE_INFINITY, true)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(3, minPos.getEndIndex());
        assertEquals(6, maxPos.getBeginIndex());
        assertEquals(7, maxPos.getEndIndex());

        // Positive infinities
        assertEquals("(−∞ … ∞)", format(NumberRange.create(Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(3, minPos.getEndIndex());
        assertEquals(6, maxPos.getBeginIndex());
        assertEquals(7, maxPos.getEndIndex());

        // Positive infinity with integers
        assertEquals("[50 … ∞)", format(new NumberRange<>(Integer.class, 50, true, null, true)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(3, minPos.getEndIndex());
        assertEquals(6, maxPos.getBeginIndex());
        assertEquals(7, maxPos.getEndIndex());

        // Negative infinity with integers
        assertEquals("(−∞ … 40]", format(new NumberRange<>(Integer.class, null, true, 40, true)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(3, minPos.getEndIndex());
        assertEquals(6, maxPos.getBeginIndex());
        assertEquals(8, maxPos.getEndIndex());

        // Measurement
        assertEquals("[-10 … 20] m", format(MeasurementRange.create(-10, true, 20, true, Units.METRE)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(4, minPos.getEndIndex());
        assertEquals(7, maxPos.getBeginIndex());
        assertEquals(9, maxPos.getEndIndex());

        assertEquals("[-10 … 20]°", format(MeasurementRange.create(-10, true, 20, true, Units.DEGREE)));
        assertEquals(1, minPos.getBeginIndex());
        assertEquals(4, minPos.getEndIndex());
        assertEquals(7, maxPos.getBeginIndex());
        assertEquals(9, maxPos.getEndIndex());

        maxPos = new FieldPosition(RangeFormat.Field.UNIT);
        assertEquals("[-1 … 2] km", format(MeasurementRange.create(-1, true, 2, true, Units.KILOMETRE)));
        assertEquals( 9, maxPos.getBeginIndex());
        assertEquals(11, maxPos.getEndIndex());
    }

    /**
     * Tests the {@link RangeFormat#format(Object, StringBuffer, FieldPosition)} method
     * using the alternate format.
     */
    @Test
    public void testAlternateFormat() {
        format = new RangeFormat(Locale.CANADA);
        minPos = new FieldPosition(RangeFormat.Field.MIN_VALUE);
        maxPos = new FieldPosition(RangeFormat.Field.MAX_VALUE);
        format.setAlternateForm(true);

        assertEquals("[-10 … 20]", format(NumberRange.create(-10, true, 20, true)));
        assertEquals("]-3 … 4[",   format(NumberRange.create( -3, false, 4, false)));
        assertEquals("[2 … 8[",    format(NumberRange.create(  2, true,  8, false)));
    }

    /**
     * Tests the parsing method on ranges of numbers. This test fixes the type to
     * {@code Integer.class}.  A different test will let the parser determine the
     * type itself.
     */
    @Test
    public void testParseIntegers() {
        format   = new RangeFormat(Locale.CANADA, Integer.class);
        parsePos = new ParsePosition(0);

        assertEquals(NumberRange.create(-10, true,   20, true ), parse("[-10 … 20]" ));
        assertEquals(NumberRange.create( -3, false,   4, false), parse("( -3 …  4) "));
        assertEquals(NumberRange.create(  2, true,    8, false), parse("  [2 …  8) "));
        assertEquals(NumberRange.create( 40, false,  90, true ), parse(" (40 … 90]"));
        assertEquals(NumberRange.create(300, true,  300, true ), parse(" 300 "));
        assertEquals(NumberRange.create(300, true,  300, true ), parse("[300]"));
        assertEquals(NumberRange.create(300, false, 300, false), parse("(300)"));
        assertEquals(NumberRange.create(300, true,  300, true ), parse("{300}"));
        assertEquals(NumberRange.create(  0, true,    0, false), parse("[]"));
        assertEquals(NumberRange.create(  0, true,    0, false), parse("{}"));
    }

    /**
     * Tests the parsing method on ranges of numbers. This test fixes the type to
     * {@code Double.class}.   A different test will let the parser determine the
     * type itself.
     */
    @Test
    public void testParseDoubles() {
        format   = new RangeFormat(Locale.CANADA, Double.class);
        parsePos = new ParsePosition(0);

        assertEquals(NumberRange.create(-10.0, true,             20.0, true), parse("[-10 … 20]" ));
        assertEquals(NumberRange.create(NEGATIVE_INFINITY, true, 30.0, true), parse("[-∞ … 30]"));
        assertEquals(NumberRange.create(NEGATIVE_INFINITY, true, 30.0, true), parse("[−∞ … 30]"));
        assertEquals(NumberRange.create(50.0, true, POSITIVE_INFINITY, true), parse("[50 … ∞]"));
    }

    /**
     * Tests the parsing method on ranges of numbers where the type is inferred automatically.
     */
    @Test
    @SuppressWarnings("cast")
    public void testParseAuto() {
        format   = new RangeFormat(Locale.CANADA);
        parsePos = new ParsePosition(0);

        assertEquals(NumberRange.create((byte)    -10, true, (byte)    20, true), parse("[  -10 …    20]" ));
        assertEquals(NumberRange.create((short) -1000, true, (short) 2000, true), parse("[-1000 …  2000]" ));
        assertEquals(NumberRange.create((int)      10, true, (int)  40000, true), parse("[   10 … 40000]" ));
        assertEquals(NumberRange.create((int)       1, true, (int)  50000, true), parse("[ 1.00 … 50000]" ));
        assertEquals(NumberRange.create((float)   8.5, true, (float)    4, true), parse("[ 8.50 …     4]" ));
    }

    /**
     * Tests the parsing of invalid ranges.
     */
    @Test
    public void testParseFailure() {
        format   = new RangeFormat(Locale.CANADA);
        parsePos = new ParsePosition(0);

        assertNull(parse("[-A … 20]")); assertEquals(1, parsePos.getErrorIndex());
        assertNull(parse("[10 … TB]")); assertEquals(6, parsePos.getErrorIndex());
        assertNull(parse("[10 x 20]")); assertEquals(4, parsePos.getErrorIndex());
        assertNull(parse("[10 … 20" )); assertEquals(8, parsePos.getErrorIndex());
        var e = assertThrows(ParseException.class, () -> format.parse("[10 … TB]"));
        assertEquals(6, e.getErrorOffset());
    }

    /**
     * Stores the field indices of the years in the {@link #minPos} and {@link #maxPos} fields.
     */
    private static void findYears(final AttributedCharacterIterator it,
            final RangeFormat.Field field, final FieldPosition pos)
    {
        it.setIndex(it.getRunLimit(field));
        it.setIndex(it.getRunLimit(DateFormat.Field.YEAR));
        pos.setBeginIndex(it.getIndex());
        it.setIndex(it.getRunLimit(DateFormat.Field.YEAR));
        pos.setEndIndex(it.getIndex());
    }

    /**
     * Tests formatting a range of {@link LocalDate} objects.
     */
    @Test
    public void testFormatLocalDate() {
        format = new RangeFormat(Locale.CANADA_FRENCH, LocalDate.class);
        final Range<LocalDate> range = new Range<>(LocalDate.class,
                LocalDate.of(2019, 12, 23), true,
                LocalDate.of(2020,  5, 31), true);
        /*
         * Expected output is "[2019-12-23 … 2020-05-31]" but be robust
         * to small variation in output format (years may be on 2 digits).
         */
        final String result = format.format(range);
        assertTrue(result.matches("\\[(20)?19-12-23 … (20)?20-05-31\\]"), result);
    }

    /**
     * Tests formatting a range of {@link LocalTime} objects.
     */
    @Test
    public void testFormatLocalTime() {
        format = new RangeFormat(Locale.FRANCE, LocalTime.class);
        final Range<LocalTime> range = new Range<>(LocalTime.class,
                LocalTime.of( 6, 0), true,
                LocalTime.of(18, 0), true);

        assertEquals("[06:00 … 18:00]", format.format(range));
    }

    /**
     * Tests formatting a range of {@link Instant} objects.
     *
     * @see RangeTest#testFormatInstantTo()
     */
    @Test
    public void testFormatInstant() {
        format = new RangeFormat(Locale.FRANCE, Instant.class);
        ((DateFormat) format.elementFormat).setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        final Range<Instant> range = new Range<>(Instant.class,
                Instant.parse("2019-12-23T06:00:00Z"), true,
                Instant.parse("2020-05-31T18:00:00Z"), true);

        assertEquals("[23/12/2019 07:00 … 31/05/2020 20:00]", format.format(range));
    }

    /**
     * Tests the {@link RangeFormat#formatToCharacterIterator(Object)} method with dates.
     */
    @Test
    public void testFormatDatesToCharacterIterator() {
        format   = new RangeFormat(Locale.FRANCE, TimeZone.getTimeZone(UTC));
        minPos   = new FieldPosition(RangeFormat.Field.MIN_VALUE);
        maxPos   = new FieldPosition(RangeFormat.Field.MAX_VALUE);
        parsePos = new ParsePosition(0);

        final long HOUR = 60L * 60 * 1000;
        final long DAY  = 24L * HOUR;
        final long YEAR = round(365.25 * DAY);

        Range<Date> range = new Range<>(Date.class,
                new Date(15*DAY + 18*HOUR), true,
                new Date(20*YEAR + 15*DAY + 9*HOUR), true);
        AttributedCharacterIterator it = format.formatToCharacterIterator(range);
        String text = it.toString();
        findYears(it, RangeFormat.Field.MIN_VALUE, minPos);
        findYears(it, RangeFormat.Field.MAX_VALUE, maxPos);
        assertEquals("[16/01/1970 18:00 … 16/01/1990 09:00]", text);
        assertEquals( 7, minPos.getBeginIndex());
        assertEquals(11, minPos.getEndIndex());
        assertEquals(26, maxPos.getBeginIndex());
        assertEquals(30, maxPos.getEndIndex());
        assertEquals(range, parse(text));
        /*
         * Try again with the infinity symbol in one endpoint.
         */
        range = new Range<>(Date.class, (Date) null, true, new Date(20*YEAR), true);
        it    = format.formatToCharacterIterator(range);
        text  = it.toString();
        findYears(it, RangeFormat.Field.MAX_VALUE, maxPos);
        assertEquals("(−∞ … 01/01/1990 00:00]", text);
        assertEquals(12, maxPos.getBeginIndex());
        assertEquals(16, maxPos.getEndIndex());
        assertEquals(range, parse(text));

        range = new Range<>(Date.class, new Date(20*YEAR), true, (Date) null, true);
        it    = format.formatToCharacterIterator(range);
        text  = it.toString();
        findYears(it, RangeFormat.Field.MIN_VALUE, minPos);
        assertEquals("[01/01/1990 00:00 … ∞)", text);
        assertEquals( 7, minPos.getBeginIndex());
        assertEquals(11, minPos.getEndIndex());
        assertEquals(range, parse(text));
    }

    /**
     * Tests {@link RangeFormat#clone()}.
     */
    @Test
    public void testClone() {
        final RangeFormat f1 = new RangeFormat(Locale.FRANCE);
        f1.setElementPattern("#0.###", false);
        final RangeFormat f2 = f1.clone();
        f2.setElementPattern("#0.00#", false);
        assertEquals("#0.###", f1.getElementPattern(false));
        assertEquals("#0.00#", f2.getElementPattern(false));
    }
}
