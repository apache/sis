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
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import javax.measure.unit.SI;
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;

import static org.junit.Assert.*;
import static java.lang.StrictMath.*;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.NEGATIVE_INFINITY;


/**
 * Tests parsing and formatting done by the {@link RangeFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.06)
 * @version 0.3
 * @module
 */
@DependsOn(MeasurementRangeTest.class)
public final strictfp class RangeFormatTest extends TestCase {
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
     * Formats the given range.
     */
    private String format(final Range<?> range) {
        final String s1 = format.format(range, new StringBuffer(), minPos).toString();
        final String s2 = format.format(range, new StringBuffer(), maxPos).toString();
        assertEquals("Two consecutive formats produced different results.", s1, s2);
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
        assertEquals("Index position shall be modified on parse success, and only parse success.", parsePos.getIndex()      == 0, range == null);
        assertEquals("Error position shall be defined on parse failure, and only parse failure",   parsePos.getErrorIndex() >= 0, range == null);
        if (range != null) {
            int i = parsePos.getIndex();
            assertTrue("The parse method parsed a character that it shouldn't have accepted.", text.lastIndexOf('_', i-1) < 0);
            while (i < text.length()) {
                final char c = text.charAt(i++);
                assertTrue("Looks like that the parse method didn't parsed everything.",
                           Character.isWhitespace(c) || c == '_');
            }
        }
        return range;
    }

    /**
     * Tests the {@link RangeFormat#format} method with numbers.
     */
    @Test
    public void testFormatNumber() {
        format = new RangeFormat(Locale.CANADA);
        minPos = new FieldPosition(RangeFormat.MIN_VALUE_FIELD | NumberFormat.INTEGER_FIELD);
        maxPos = new FieldPosition(RangeFormat.MAX_VALUE_FIELD | NumberFormat.INTEGER_FIELD);

        // Closed range
        assertEquals("[-10 … 20]", format(NumberRange.create(-10, 20)));
        assertEquals("minPos.beginIndex", 2, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   4, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 7, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   9, maxPos.getEndIndex());

        // Open range
        assertEquals("(-3 … 4)", format(NumberRange.create(-3, false, 4, false)));
        assertEquals("minPos.beginIndex", 2, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   3, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 6, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   7, maxPos.getEndIndex());

        // Half-open range
        assertEquals("[2 … 8)", format(NumberRange.create(2, true, 8, false)));
        assertEquals("minPos.beginIndex", 1, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   2, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 5, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   6, maxPos.getEndIndex());

        // Half-open range
        assertEquals("(40 … 90]", format(NumberRange.create(40, false, 90, true)));
        assertEquals("minPos.beginIndex", 1, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   3, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 6, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   8, maxPos.getEndIndex());

        // Single value
        assertEquals("300", format(NumberRange.create(300, 300)));
        assertEquals("minPos.beginIndex", 0, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   3, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 0, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   3, maxPos.getEndIndex());

        // Empty range
        assertEquals("[]", format(NumberRange.create(300, true, 300, false)));
        assertEquals("minPos.beginIndex", 1, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   1, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 1, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   1, maxPos.getEndIndex());

        // Negative infinity
        assertEquals("(-∞ … 30]", format(NumberRange.create(Double.NEGATIVE_INFINITY, 30)));
        assertEquals("minPos.beginIndex", 2, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   3, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 6, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   8, maxPos.getEndIndex());

        // Positive infinity
        assertEquals("[50 … ∞)", format(NumberRange.create(50, Double.POSITIVE_INFINITY)));
        assertEquals("minPos.beginIndex", 1, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   3, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 6, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   7, maxPos.getEndIndex());

        // Positive infinities
        assertEquals("(-∞ … ∞)", format(NumberRange.create(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)));
        assertEquals("minPos.beginIndex", 2, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   3, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 6, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   7, maxPos.getEndIndex());

        // Positive infinity with integers
        assertEquals("[50 … ∞)", format(new NumberRange<>(Integer.class, 50, null)));
        assertEquals("minPos.beginIndex", 1, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   3, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 6, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   7, maxPos.getEndIndex());

        // Negative infinity with integers
        assertEquals("(-∞ … 40]", format(new NumberRange<>(Integer.class, null, 40)));
        assertEquals("minPos.beginIndex", 2, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   3, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 6, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   8, maxPos.getEndIndex());

        // Measurement
        assertEquals("[-10 … 20] m", format(MeasurementRange.create(-10, 20, SI.METRE)));
        assertEquals("minPos.beginIndex", 2, minPos.getBeginIndex());
        assertEquals("minPos.endIndex",   4, minPos.getEndIndex());
        assertEquals("maxPos.beginIndex", 7, maxPos.getBeginIndex());
        assertEquals("maxPos.endIndex",   9, maxPos.getEndIndex());

        maxPos = new FieldPosition(RangeFormat.UNIT_FIELD);
        assertEquals("[-1 … 2] km", format(MeasurementRange.create(-1, 2, SI.KILOMETRE)));
        assertEquals("unitPos.beginIndex", 9, maxPos.getBeginIndex());
        assertEquals("unitPos.endIndex",  11, maxPos.getEndIndex());
    }

    /**
     * Tests the parsing method on ranges of numbers. This test fixes the type to
     * {@code Integer.class}.  A different test will let the parser determine the
     * type itself.
     */
    @Test
    public void testParseInteger() {
        format   = new RangeFormat(Locale.CANADA, Integer.class);
        parsePos = new ParsePosition(0);

        assertEquals(NumberRange.create(-10,         20       ), parse("[-10 … 20]" ));
        assertEquals(NumberRange.create( -3, false,   4, false), parse("( -3 …  4) "));
        assertEquals(NumberRange.create(  2, true,    8, false), parse("  [2 …  8) _"));
        assertEquals(NumberRange.create( 40, false,  90, true ), parse(" (40 … 90]_"));
        assertEquals(NumberRange.create(300,        300       ), parse(" 300_"));
        assertEquals(NumberRange.create(300,        300       ), parse("[300]"));
        assertEquals(NumberRange.create(300, false, 300, false), parse("(300)"));
        assertEquals(NumberRange.create(  0, true,    0, false), parse("[]"));
    }

    /**
     * Tests the parsing method on ranges of numbers. This test fixes the type to
     * {@code Double.class}.   A different test will let the parser determine the
     * type itself.
     */
    @Test
    public void testParseDouble() {
        format   = new RangeFormat(Locale.CANADA, Double.class);
        parsePos = new ParsePosition(0);

        assertEquals(NumberRange.create(-10.0,             20.0), parse("[-10 … 20]" ));
        assertEquals(NumberRange.create(NEGATIVE_INFINITY, 30.0), parse("[-∞ … 30]"));
        assertEquals(NumberRange.create(50.0, POSITIVE_INFINITY), parse("[50 … ∞]"));
    }

    /**
     * Tests the parsing method on ranges of numbers where the type is inferred automatically.
     */
    @Test
    @SuppressWarnings("cast")
    public void testParseAuto() {
        format   = new RangeFormat(Locale.CANADA);
        parsePos = new ParsePosition(0);

        assertEquals(NumberRange.create((byte)    -10, (byte)    20), parse("[  -10 …    20]" ));
        assertEquals(NumberRange.create((short) -1000, (short) 2000), parse("[-1000 …  2000]" ));
        assertEquals(NumberRange.create((int)      10, (int)  40000), parse("[   10 … 40000]" ));
        assertEquals(NumberRange.create((int)       1, (int)  50000), parse("[ 1.00 … 50000]" ));
        assertEquals(NumberRange.create((float)   8.5, (float)    4), parse("[ 8.50 …     4]" ));
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
        try {
            assertNull(format.parse("[10 … TB]"));
            fail("Parsing should have failed.");
        } catch (ParseException e) {
            // This is the expected exception.
            assertEquals(6, e.getErrorOffset());
        }
    }

    /**
     * Tests formatting and parsing with dates.
     */
    @Test
    public void testDateRange() {
        format   = new RangeFormat(Locale.FRANCE, TimeZone.getTimeZone("UTC"));
        minPos   = new FieldPosition(RangeFormat.MIN_VALUE_FIELD | DateFormat.YEAR_FIELD);
        maxPos   = new FieldPosition(RangeFormat.MAX_VALUE_FIELD | DateFormat.YEAR_FIELD);
        parsePos = new ParsePosition(0);

        final long HOUR = 60L * 60 * 1000;
        final long DAY  = 24L * HOUR;
        final long YEAR = round(365.25 * DAY);

        DateRange range = new DateRange(new Date(15*DAY + 18*HOUR), new Date(20*YEAR + 15*DAY + 9*HOUR));
        String text =  format(range);
        assertEquals("[16/01/70 18:00 … 16/01/90 09:00]", text);
        assertEquals(range, parse(text));
        /*
         * Following is for a visual check of the default toString() method,
         * but is not part of the test suite because it may vary.
         */
        if (false) {
            System.out.println(range);
        }
        /*
         * Try again with the infinity symbol in one bounds.
         */
        range = new DateRange((Date) null, new Date(20*YEAR));
        text  = format(range);
        assertEquals("(-∞ … 01/01/90 00:00]", text);
        assertEquals(range, parse(text));

        range = new DateRange(new Date(20*YEAR), (Date) null);
        text  = format(range);
        assertEquals("[01/01/90 00:00 … ∞)", text);
        assertEquals(range, parse(text));
    }
}
