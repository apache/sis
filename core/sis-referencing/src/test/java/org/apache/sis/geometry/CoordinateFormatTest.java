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
package org.apache.sis.geometry;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.ParsePosition;
import java.text.ParseException;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.measure.Angle;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.mock.VerticalCRSMock;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link CoordinateFormat} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 *
 * @see org.apache.sis.measure.CoordinateFormatTest
 *
 * @since 0.8
 * @module
 */
public final strictfp class CoordinateFormatTest extends TestCase {
    /**
     * Tests formatting a coordinate in unknown CRS.
     * The ordinate values are expected to be formatted as ordinary numbers.
     */
    @Test
    public void testFormatUnknownCRS() {
        final CoordinateFormat format = new CoordinateFormat(null, null);
        GeneralDirectPosition position = new GeneralDirectPosition(23.78, -12.74, 127.9, 3.25);
        assertEquals("23.78 -12.74 127.9 3.25", format.format(position));
        /*
         * Try another point having a different number of position
         * for verifying that no cached values are causing problem.
         */
        position = new GeneralDirectPosition(4.64, 10.25, -3.12);
        assertEquals("4.64 10.25 -3.12", format.format(position));
        /*
         * Try again with a different separator.
         */
        format.setSeparator("; ");
        assertEquals("4.64; 10.25; -3.12", format.format(position));
    }

    /**
     * Tests parsing a coordinate in unknown CRS.
     * The ordinate values are formatted as ordinary numbers.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testParseUnknownCRS() throws ParseException {
        final CoordinateFormat format = new CoordinateFormat(null, null);
        final ParsePosition index = new ParsePosition(0);
        DirectPosition position = format.parse("23.78 -12.74 127.9 3.25", index);
        assertArrayEquals(new double[] {23.78, -12.74, 127.9, 3.25}, position.getCoordinate(), STRICT);
        assertEquals("ParsePosition.getErrorIndex()", -1, index.getErrorIndex());
        assertEquals("ParsePosition.getIndex()",      23, index.getIndex());
        /*
         * Try another point having a different number of position
         * for verifying that no cached values are causing problem.
         */
        index.setIndex(0);
        position = format.parse("4.64 10.25 -3.12", index);
        assertArrayEquals(new double[] {4.64, 10.25, -3.12}, position.getCoordinate(), STRICT);
        assertEquals("ParsePosition.getErrorIndex()", -1, index.getErrorIndex());
        assertEquals("ParsePosition.getIndex()",      16, index.getIndex());
        /*
         * Try again with a different separator. Also put or remove some spaces
         * around the separator for testing UnitFormat capabilities to ignore them.
         */
        format.setSeparator("; ");
        index.setIndex(0);
        position = format.parse("4.64;10.25 ;  -3.12", index);
        assertArrayEquals(new double[] {4.64, 10.25, -3.12}, position.getCoordinate(), STRICT);
        assertEquals("ParsePosition.getErrorIndex()", -1, index.getErrorIndex());
        assertEquals("ParsePosition.getIndex()",      19, index.getIndex());
    }

    /**
     * Tests formatting a single vertical coordinate.
     */
    @Test
    public void testFormatVertical() {
        final CoordinateFormat format = new CoordinateFormat(Locale.US, null);
        format.setDefaultCRS(VerticalCRSMock.HEIGHT);
        DirectPosition1D position = new DirectPosition1D(100);
        assertEquals("100 m", format.format(position));

        position.setCoordinateReferenceSystem(VerticalCRSMock.HEIGHT_ft);
        assertEquals("100 ft", format.format(position));

        position.setCoordinateReferenceSystem(VerticalCRSMock.DEPTH);
        assertEquals("100 m", format.format(position));
    }

    /**
     * Tests formatting a 4-dimensional geographic coordinate.
     */
    @Test
    @DependsOnMethod("testFormatUnknownCRS")
    public void testFormatGeographic4D() {
        /*
         * For a 4-dimensional coordinate with a temporal CRS.
         * Use a fixed timezone and date pattern for portability.
         * Epoch is November 17, 1858 at 00:00 UTC.
         */
        final CoordinateFormat format = new CoordinateFormat(Locale.FRANCE, TimeZone.getTimeZone("GMT+01:00"));
        final String anglePattern = "DD°MM.m′";
        final String  datePattern = "dd-MM-yyyy HH:mm";
        format.applyPattern(Angle.class,  anglePattern);
        format.applyPattern(Date.class,    datePattern);
        assertEquals("getPattern(Angle)", anglePattern, format.getPattern(Angle.class));
        assertEquals("getPattern(Date)",   datePattern, format.getPattern(Date .class));
        final GeneralDirectPosition position = new GeneralDirectPosition(23.78, -12.74, 127.9, 54000.25);
        position.setCoordinateReferenceSystem(HardCodedCRS.GEOID_4D);
        assertEquals("23°46,8′E 12°44,4′S 127,9 m 22-09-2006 07:00", format.format(position));
        /*
         * Try a null CRS. Should format everything as numbers.
         */
        position.setCoordinateReferenceSystem(null);
        assertEquals("getPattern(Angle)", anglePattern, format.getPattern(Angle.class));
        assertEquals("getPattern(Date)",   datePattern, format.getPattern(Date .class));
        assertEquals("23,78 -12,74 127,9 54 000,25",    format.format(position));
        /*
         * Try again with the original CRS, but different separator.
         */
        format.setSeparator("; ");
        position.setCoordinateReferenceSystem(HardCodedCRS.GEOID_4D);
        assertEquals("getPattern(Angle)", anglePattern, format.getPattern(Angle.class));
        assertEquals("getPattern(Date)",   datePattern, format.getPattern(Date .class));
        assertEquals("23°46,8′E; 12°44,4′S; 127,9 m; 22-09-2006 07:00", format.format(position));
    }

    /**
     * Tests parsing a 4-dimensional geographic coordinate.
     * This method is the converse of {@link #testFormatGeographic4D()}.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testParseUnknownCRS")
    public void testParseGeographic4D() throws ParseException {
        final CoordinateFormat format = new CoordinateFormat(Locale.FRANCE, TimeZone.getTimeZone("GMT+01:00"));
        format.applyPattern(Date.class, "dd-MM-yyyy HH:mm");
        format.setDefaultCRS(HardCodedCRS.GEOID_4D);
        final ParsePosition index = new ParsePosition(11);
        final DirectPosition pos = format.parse("(to skip); 23°46,8′E 12°44,4′S 127,9 m 22-09-2006 07:00 (ignore)", index);
        assertArrayEquals(new double[] {23.78, -12.74, 127.90, 54000.25}, pos.getCoordinate(), STRICT);
        assertEquals("ParsePosition.getErrorIndex()", -1, index.getErrorIndex());
        assertEquals("ParsePosition.getIndex()",      55, index.getIndex());
        /*
         * Tests error message when parsing the same string but with unknown units of measurement.
         */
        index.setIndex(11);
        try {
            format.parse("(to skip); 23°46,8′E 12°44,4′S 127,9 Foo 22-09-2006 07:00", index);
            fail("Should not have parsed a coordinate with unknown units.");
        } catch (ParseException e) {
            assertEquals("ParsePosition.getIndex()",        11, index.getIndex());
            assertEquals("ParsePosition.getErrorIndex()",   37, index.getErrorIndex());
            assertEquals("ParseException.getErrorOffset()", 37, e.getErrorOffset());
            assertEquals("Les caractères « Foo » après « 23°46,8′E 12°44,4′S 127,9 » sont inattendus.",
                         e.getLocalizedMessage());  // In the language specified at CoordinateFormat construction time.
        }
    }
}
