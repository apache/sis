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
import java.io.IOException;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.test.TestCase;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.mock.VerticalCRSMock;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;


/**
 * Tests the {@link CoordinateFormat} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Michael Hausegger
 *
 * @see org.apache.sis.measure.AngleFormatTest
 */
@SuppressWarnings("exports")
public final class CoordinateFormatTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CoordinateFormatTest() {
    }

    /**
     * Compares coordinate values from the given positions.
     */
    private static void assertPositionEquals(final DirectPosition expected, final DirectPosition actual) {
        assertNotSame(expected, actual);
        assertArrayEquals(expected.getCoordinates(), actual.getCoordinates());
    }

    /**
     * Tests formatting a coordinate in unknown CRS.
     * The coordinate values are expected to be formatted as ordinary numbers.
     */
    @Test
    public void testFormatUnknownCRS() {
        final var format = new CoordinateFormat(null, null);
        var position = new GeneralDirectPosition(23.78, -12.74, 127.9, 3.25);
        assertEquals("23.78 -12.74 127.9 3.25", format.format(position));
        /*
         * Try another point having a different number of position
         * for verifying that no cached values are causing problem.
         */
        position = new GeneralDirectPosition(4.64, 10.25, -3.12);
        assertEquals("4.64 10.25 -3.12", format.format(position));
        /*
         * Try again with a different separator.
         */
        format.setSeparator("; ");
        assertEquals("; ", format.getSeparator());
        assertEquals("4.64; 10.25; -3.12", format.format(position));
    }

    /**
     * Tests parsing a coordinate in unknown CRS.
     * The coordinate values are formatted as ordinary numbers.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testParseUnknownCRS() throws ParseException {
        final var format = new CoordinateFormat(null, null);
        final var charPos = new ParsePosition(0);
        DirectPosition position = format.parse("23.78 -12.74 127.9 3.25", charPos);
        assertArrayEquals(new double[] {23.78, -12.74, 127.9, 3.25}, position.getCoordinates());
        assertEquals(-1, charPos.getErrorIndex());
        assertEquals(23, charPos.getIndex());
        /*
         * Try another point having a different number of position
         * for verifying that no cached values are causing problem.
         */
        charPos.setIndex(0);
        position = format.parse("4.64 10.25 -3.12", charPos);
        assertArrayEquals(new double[] {4.64, 10.25, -3.12}, position.getCoordinates());
        assertEquals(-1, charPos.getErrorIndex());
        assertEquals(16, charPos.getIndex());
        /*
         * Try again with a different separator. Also put or remove some spaces
         * around the separator for testing UnitFormat capabilities to ignore them.
         */
        format.setSeparator("; ");
        assertEquals("; ", format.getSeparator());
        charPos.setIndex(0);
        position = format.parse("4.64;10.25 ;  -3.12", charPos);
        assertArrayEquals(new double[] {4.64, 10.25, -3.12}, position.getCoordinates());
        assertEquals(-1, charPos.getErrorIndex());
        assertEquals(19, charPos.getIndex());
    }

    /**
     * Tests formatting a single vertical coordinate.
     */
    @Test
    public void testFormatVertical() {
        final var format = new CoordinateFormat(Locale.US, null);
        format.setDefaultCRS(VerticalCRSMock.HEIGHT);
        var position = new DirectPosition1D(100);
        assertEquals("100 m", format.format(position));

        position.setCoordinateReferenceSystem(VerticalCRSMock.HEIGHT_ft);
        assertEquals("100 ft", format.format(position));

        position.setCoordinateReferenceSystem(VerticalCRSMock.DEPTH);
        assertEquals("100 m", format.format(position));
    }

    /**
     * Tests formatting 2-dimensional projected coordinates.
     */
    @Test
    public void testFormatProjected() {
        final var format = new CoordinateFormat(Locale.US, null);
        format.setDefaultCRS(HardCodedConversions.mercator());
        assertEquals("100 m W 300 m N", format.format(new DirectPosition2D(-100, 300)));
        assertEquals("200 m E 100 m S", format.format(new DirectPosition2D(200, -100)));
    }

    /**
     * Tests parsing 2-dimensional projected coordinates.
     * This method is the converse of {@link #testFormatProjected()}.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testParseProjected() throws ParseException {
        final var format = new CoordinateFormat(Locale.US, null);
        format.setDefaultCRS(HardCodedConversions.mercator());
        DirectPosition pos = format.parse("100 m W 300 m N", new ParsePosition(0));
        assertArrayEquals(new double[] {-100, 300}, pos.getCoordinates());
        pos = format.parse("200 m E 100 m S", new ParsePosition(0));
        assertArrayEquals(new double[] {200, -100}, pos.getCoordinates());
    }

    /**
     * Tests formatting 4-dimensional geographic coordinates.
     */
    @Test
    public void testFormatGeographic4D() {
        /*
         * For a 4-dimensional coordinate with a temporal CRS.
         * Use a fixed timezone and date pattern for portability.
         * Epoch is November 17, 1858 at 00:00 UTC.
         */
        final var format = new CoordinateFormat(Locale.FRANCE, TimeZone.getTimeZone("GMT+01:00"));
        final String anglePattern = "DD°MM.m′";
        final String  datePattern = "dd-MM-yyyy HH:mm";
        format.applyPattern(Angle.class,  anglePattern);
        format.applyPattern(Date.class,    datePattern);
        assertEquals(anglePattern, format.getPattern(Angle.class));
        assertEquals( datePattern, format.getPattern(Date .class));
        final var position = new GeneralDirectPosition(23.78, -12.74, 127.9, 54000.25);
        position.setCoordinateReferenceSystem(HardCodedCRS.GEOID_4D);
        assertEquals("23°46,8′E 12°44,4′S 127,9 m 22-09-2006 07:00", format.format(position));
        /*
         * Try a null CRS. Should format everything as numbers.
         */
        position.setCoordinateReferenceSystem(null);
        assertEquals(anglePattern, format.getPattern(Angle.class));
        assertEquals( datePattern, format.getPattern(Date .class));
        assertEquals("23,78 -12,74 127,9 54 000,25",    format.format(position));
        /*
         * Try again with the original CRS, but different separator.
         */
        format.setSeparator("; ");
        assertEquals("; ", format.getSeparator());
        position.setCoordinateReferenceSystem(HardCodedCRS.GEOID_4D);
        assertEquals(anglePattern, format.getPattern(Angle.class));
        assertEquals( datePattern, format.getPattern(Date .class));
        assertEquals("23°46,8′E; 12°44,4′S; 127,9 m; 22-09-2006 07:00", format.format(position));
    }

    /**
     * Tests parsing 4-dimensional geographic coordinates.
     * This method is the converse of {@link #testFormatGeographic4D()}.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testParseGeographic4D() throws ParseException {
        final var format = new CoordinateFormat(Locale.FRANCE, TimeZone.getTimeZone("GMT+01:00"));
        format.applyPattern(Date.class, "dd-MM-yyyy HH:mm");
        format.setDefaultCRS(HardCodedCRS.GEOID_4D);
        final ParsePosition charPos = new ParsePosition(11);
        final DirectPosition pos = format.parse("(to skip); 23°46,8′E 12°44,4′S 127,9 m 22-09-2006 07:00 (ignore)", charPos);
        assertArrayEquals(new double[] {23.78, -12.74, 127.90, 54000.25}, pos.getCoordinates());
        assertEquals(-1, charPos.getErrorIndex());
        assertEquals(55, charPos.getIndex());
        /*
         * Tests error message when parsing the same string but with unknown units of measurement.
         */
        charPos.setIndex(11);
        var e = assertThrows(ParseException.class,
                () -> format.parse("(to skip); 23°46,8′E 12°44,4′S 127,9 Foo 22-09-2006 07:00", charPos),
                "Should not have parsed a coordinate with unknown units.");
        assertEquals(  11, charPos.getIndex());
        assertEquals(  37, charPos.getErrorIndex());
        assertEquals(37, e.getErrorOffset());
        assertEquals("Les caractères « Foo » après « 23°46,8′E 12°44,4′S 127,9 » sont inattendus.",
                     e.getLocalizedMessage());  // In the language specified at CoordinateFormat construction time.
    }

    /**
     * Tests formatting a coordinate in default locale, then parsing the result. This test verifies that the
     * parsing is consistent with formatting in whatever locale used by the platform. This test does not verify
     * if the formatted string is equal to any expected value since it is locale-dependent.
     *
     * @throws IOException    should never happen since we format into a {@link StringBuffer}.
     * @throws ParseException if {@code CoordinateFormat} fails to parse the value that it formatted.
     */
    @Test
    public void testParseInDefaultLocale() throws IOException, ParseException {
        var format = new CoordinateFormat();
        var buffer = new StringBuffer();
        format.format(new DirectPosition2D(-3, 4), buffer);

        ParsePosition  charPos  = new ParsePosition(0);
        DirectPosition position = format.parse(buffer, charPos);
        assertEquals(buffer.length(), charPos.getIndex(), "Should have parsed the whole text.");
        assertEquals(2, position.getDimension(), "DirectPosition.getDimension()");
        assertArrayEquals(new double[] {-3, 4}, position.getCoordinates());
    }

    /**
     * Tests parsing from a position different then the beginning of the string.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testParseFromOffset() throws ParseException {
        var coordinateFormat = new CoordinateFormat(Locale.CANADA, null);
        coordinateFormat.setDefaultCRS(VerticalCRSMock.BAROMETRIC_HEIGHT);
        ParsePosition  charPos  = new ParsePosition(7);
        DirectPosition position = coordinateFormat.parse("[skip] 12", charPos);
        assertEquals(9, charPos.getIndex(), "Should have parsed the whole text.");
        assertEquals(1, position.getDimension(), "DirectPosition.getDimension()");
        assertArrayEquals(new double[] {12}, position.getCoordinates());
    }

    /**
     * Verifies the pattern returned by {@link CoordinateFormat#getPattern(Class)}. This includes verifying that
     * the method returns {@code null} when invoked for an unknown type, or a type that does not support pattern.
     */
    @Test
    public void testGetPattern() {
        var coordinateFormat = new CoordinateFormat(Locale.UK, null);
        assertEquals("#,##0.###", coordinateFormat.getPattern(Float.class));
        assertNull(coordinateFormat.getPattern(Object.class));
        assertNull(coordinateFormat.getPattern(Class.class));
    }

    /**
     * Verifies that {@link CoordinateFormat#applyPattern(Class, String)} when
     * invoked for an unknown type, or for a type that does not support patterns.
     */
    @Test
    public void testApplyPattern() {
        var format = new CoordinateFormat();
        assertFalse(format.applyPattern(Object.class, "A dummy pattern"));
        assertFalse(format.applyPattern(Class.class,  "A dummy pattern"));
    }

    /**
     * Tests {@link CoordinateFormat#setGroundPrecision(Quantity)}.
     */
    @Test
    public void testSetGroundPrecision() {
        final var format = new CoordinateFormat(Locale.FRANCE, null);
        final var pos = new DirectPosition2D(40.123456789, 9.87654321);
        format.setDefaultCRS(HardCodedCRS.WGS84_LATITUDE_FIRST);
        format.setGroundPrecision(Quantities.create(0.01, Units.GRAD));
        assertEquals("40°07,4′N 9°52,6′E", format.format(pos));
        format.setGroundPrecision(Quantities.create(0.01, Units.METRE));
        assertEquals("40°07′24,4444″N 9°52′35,5556″E", format.format(pos));
    }

    /**
     * Tests {@link CoordinateFormat#setPrecisions(double...)} followed by
     * {@link CoordinateFormat#getPrecisions()}
     */
    @Test
    public void testSetPrecisions() {
        final var format = new CoordinateFormat(Locale.FRANCE, null);
        final var pos = new DirectPosition2D(40.123456789, 9.87654321);
        format.setDefaultCRS(HardCodedCRS.WGS84_LATITUDE_FIRST);
        format.setPrecisions(0.05, 0.0001);
        assertEquals("40°07′N 9°52′35,6″E", format.format(pos));
        assertArrayEquals(new double[] {1.0/60, 0.1/3600}, format.getPrecisions(), 1E-15);

        format.setPrecisions(0.0005, 0.01);
        assertEquals("40°07′24″N 9°52,6′E", format.format(pos));
        assertArrayEquals(new double[] {1.0/3600, 0.1/60}, format.getPrecisions(), 1E-15);
    }

    /**
     * Tests {@link CoordinateFormat#setGroundAccuracy(Quantity)}.
     *
     * @throws ParseException if parsing failed.
     */
    @Test
    public void testSetGroundAccuracy() throws ParseException {
        final var format = new CoordinateFormat(Locale.FRANCE, null);
        final var pos = new DirectPosition2D(40.123456789, 9.87654321);
        format.setDefaultCRS(HardCodedCRS.WGS84_LATITUDE_FIRST);
        format.setPrecisions(0.05, 0.0001);
        format.setGroundAccuracy(Quantities.create(3, Units.KILOMETRE));
        assertEquals("40°07′N 9°52′35,6″E ± 3 km", format.format(pos));

        final DirectPosition p = format.parseObject("40°07′N 9°52′35,6″E ± 3 km");
        assertArrayEquals(new double[] {40.1166, 9.8765}, p.getCoordinates(), 0.0001);
    }

    /**
     * Tests the automatic change of units from "m" to "km" when the precision is low.
     *
     * @throws ParseException if parsing failed.
     */
    @Test
    public void testAutomaticChangeOfUnits() throws ParseException {
        final var format = new CoordinateFormat(Locale.CANADA, null);
        final var pos = new DirectPosition2D(400000, -600000);
        format.setDefaultCRS(HardCodedConversions.mercator());
        /*
         * Test with a precision larger than 1 km, which instruct
         * CoordinateFormat to switch unit.
         */
        format.setPrecisions(1000, 2000);
        assertEquals("400 km E 600 km S", format.format(pos));
        assertPositionEquals(pos, format.parseObject("400 km E 600 km S"));
        assertPositionEquals(pos, format.parseObject("400,000 m E 600,000 m S"));
        /*
         * Scaled units but with a fraction digit.
         */
        format.setPrecisions(100, 200);
        assertEquals("400.0 km E 600.0 km S", format.format(pos));
        assertPositionEquals(pos, format.parseObject("400,000 m E 600,000 m S"));
        assertPositionEquals(pos, format.parseObject("400 km E 600 km S"));
        /*
         * Test reverting back to unscaled units.
         */
        format.setPrecisions(1, 2);
        assertEquals("400,000 m E 600,000 m S", format.format(pos));
        assertPositionEquals(pos, format.parseObject("400,000 m E 600,000 m S"));
        assertPositionEquals(pos, format.parseObject("400 km E 600 km S"));
    }

    /**
     * Tests {@link CoordinateFormat#clone()}, then verifies that the clone has the same configuration
     * than the original object.
     */
    @Test
    public void testClone() {
        CoordinateFormat format = new CoordinateFormat(Locale.CANADA, null);
        CoordinateFormat clone  = format.clone();
        assertNotSame(clone, format);
        assertEquals(format.getSeparator(),  clone.getSeparator());
        assertEquals(format.getDefaultCRS(), clone.getDefaultCRS());
    }
}
