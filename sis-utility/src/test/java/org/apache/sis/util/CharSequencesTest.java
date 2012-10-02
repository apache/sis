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
package org.apache.sis.util;

import java.util.Arrays;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.Dependency;
import org.apache.sis.util.type.SimpleInternationalString;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.util.CharSequences.*;


/**
 * Tests {@link CharSequences} methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final strictfp class CharSequencesTest extends TestCase {
    /**
     * Tests {@link CharSequences#spaces(int)}.
     */
    @Test
    public void testSpaces() {
        assertEquals("",         spaces(0));
        assertEquals(" ",        spaces(1));
        assertEquals("        ", spaces(8));
        assertEquals("",         spaces(-2));
    }

    /**
     * Tests {@link CharSequences#length(CharSequence)}.
     */
    @Test
    public void testLength() {
        assertEquals(3, length("ABC"));
        assertEquals(0, length(null));
    }

    /**
     * Tests {@link CharSequences#count(CharSequence, String)} and its variants.
     */
    @Test
    public void testCount() {
        assertEquals(0, count("An ordinary sentence.",   '-'));
        assertEquals(4, count("- this one has -dashs--", '-'));
        assertEquals(2, count("An ordinary sentence.",  "en"));
        assertEquals(0, count(new StringBuilder("An ordinary sentence."),   '-'));
        assertEquals(4, count(new StringBuilder("- this one has -dashs--"), '-'));
        assertEquals(2, count(new StringBuilder("An ordinary sentence."),  "en"));
    }

    /**
     * Tests the {@link CharSequences#indexOf(CharSequence, CharSequence, int)} method.
     * We test four time with different kind of character sequences.
     */
    @Test
    public void testIndexOf() {
        for (int i=0; i<3; i++) {
            CharSequence string = "An ordinary sentence.";
            switch (i) {
                case 0:  /* Test directly on the String instance. */              break;
                case 1:  string = new StringBuilder            ((String) string); break;
                case 2:  string = new StringBuffer             ((String) string); break;
                case 3:  string = new SimpleInternationalString((String) string); break;
                default: throw new AssertionError(i);
            }
            assertEquals(-1, indexOf(string, "dummy",        0));
            assertEquals( 0, indexOf(string, "An",           0));
            assertEquals(-1, indexOf(string, "An",           1));
            assertEquals(12, indexOf(string, "sentence.",    0));
            assertEquals(-1, indexOf(string, "sentence;",    0));
        }
    }

    /**
     * Tests the {@link CharSequences#indexOfLineStart(CharSequence, int, int)} method.
     */
    @Test
    public void testIndexOfLineStart() {
        assertEquals("Forward search: expected the begining of \"Third\"", 15,
                indexOfLineStart("\nFirst\r\nSecond\nThird\r4\n5", 2, 3));
        assertEquals("Current line: expected the begining of \"Third\"", 15,
                indexOfLineStart("\nFirst\r\nSecond\nThird\r4\n5", 0, 17));
        assertEquals("Backward search: expected the begining of \"Second\"", 8,
                indexOfLineStart("\nFirst\r\nSecond\nThird\r4\n5", -1, 17));
        assertEquals("Backward search: expected the begining of \"First\"", 1,
                indexOfLineStart("\nFirst\r\nSecond\nThird\r4\n5", -2, 17));
    }

    /**
     * Tests {@link CharSequences#split(CharSequence, char)}.
     */
    @Test
    public void testSplit() {
        assertArrayEquals(new String[] {"lundi", "mardi", "", "mercredi"}, split("lundi , mardi,,mercredi ", ','));
        assertArrayEquals(new String[] {"lundi", "mardi", "", "mercredi"}, split("lundi \n mardi\r\n\nmercredi ", '\n'));
    }

    /**
     * Tests the {@link CharSequences#splitOnEOL(CharSequence)} method.
     */
    @Test
    public void testSplitOnEOL() {
        final CharSequence[] splitted = splitOnEOL("\nOne\r\nTwo\rThree \rFour\n Five\n\r Six \n");
        assertArrayEquals(new String[] {
            "", "One", "Two", "Three ", "Four", " Five", "", " Six ", ""
        }, splitted);
    }

    /**
     * Tests {@link CharSequences#parseDoubles(CharSequence, char)}.
     */
    @Test
    @Dependency("testSplit")
    public void testParseDoubles() {
        assertArrayEquals(new double[] {5, 1.5, Double.NaN, -8}, parseDoubles("5 , 1.5,, -8 ", ','), 0.0);
    }

    /**
     * Tests {@link CharSequences#parseFloats(CharSequence, char)}.
     */
    @Test
    @Dependency("testSplit")
    public void testParseFloats() {
        assertArrayEquals(new float[] {5, 1.5f, Float.NaN, -8}, parseFloats("5 , 1.5,, -8 ", ','), 0f);
    }

    /**
     * Tests {@link CharSequences#parseLongs(CharSequence, char, int)}.
     */
    @Test
    @Dependency("testSplit")
    public void testParseLongs() {
        assertArrayEquals(new long[] {5, 2, -8}, parseLongs("5 , 2, -8 ", ',', 10));
    }

    /**
     * Tests {@link CharSequences#parseInts(CharSequence, char, int)}.
     */
    @Test
    @Dependency("testSplit")
    public void testParseInts() {
        assertArrayEquals(new int[] {5, 2, -8}, parseInts("5 , 2, -8 ", ',', 10));
    }

    /**
     * Tests {@link CharSequences#parseShorts(CharSequence, char, int)}.
     */
    @Test
    @Dependency("testSplit")
    public void testParseShorts() {
        assertArrayEquals(new short[] {5, 2, -8}, parseShorts("5 , 2, -8 ", ',', 10));
    }

    /**
     * Tests {@link CharSequences#parseBytes(CharSequence, char, int)}.
     */
    @Test
    @Dependency("testSplit")
    public void testParseBytes() {
        assertArrayEquals(new byte[] {5, 2, -8}, parseBytes("5 , 2, -8 ", ',', 10));
    }

    /**
     * Tests the {@link CharSequences#toString(Iterable, String)} method.
     */
    @Test
    public void testToString() {
        assertEquals("4, 8, 12, 9", CharSequences.toString(Arrays.asList(4, 8, 12, 9), ", "));
        assertSame  ("singleton",   CharSequences.toString(Arrays.asList("singleton"), ", "));
    }

    /**
     * Tests the {@link CharSequences#toASCII(CharSequence)} method.
     */
    @Test
    public void testToASCII() {
        final String metre = "metre";
        assertSame  (metre, toASCII(metre));
        assertEquals(metre, toASCII("mètre").toString());
    }

    /**
     * Tests the {@link CharSequences#trimWhitespaces(CharSequence)} method.
     */
    @Test
    public void testTrimWhitespaces() {
        assertEquals("A text.", trimWhitespaces("  A text. "));
    }

    /**
     * Tests the {@link CharSequences#trimFractionalPart(CharSequence)} method.
     */
    @Test
    public void testTrimFractionalPart() {
        assertEquals("4",    trimFractionalPart("4"));
        assertEquals("4",    trimFractionalPart("4."));
        assertEquals("4",    trimFractionalPart("4.0"));
        assertEquals("4",    trimFractionalPart("4.00"));
        assertEquals("4.10", trimFractionalPart("4.10"));
    }

    /**
     * Tests the {@link CharSequences#shortSentence(CharSequence, int)} method.
     */
    @Test
    public void testShortSentence() {
        assertEquals("This sentence given (…) in a short name.", String.valueOf(
                shortSentence("This sentence given as an example is way too long to be included in a short name.", 40)));
    }

    /**
     * Tests the {@link CharSequences#camelCaseToWords(CharSequence, boolean)} method.
     */
    @Test
    public void testCamelCaseToWords() {
        final CharSequence convert = camelCaseToWords("PixelInterleavedSampleModel", true);
        assertEquals("Pixel interleaved sample model", convert.toString());
    }

    /**
     * Tests the {@link CharSequences#camelCaseToAcronym(CharSequence)} method.
     */
    @Test
    public void testCamelCaseToAcronym() {
        assertEquals("OGC", camelCaseToAcronym("OGC").toString());
        assertEquals("OGC", camelCaseToAcronym("Open Geospatial Consortium").toString());
        assertEquals("E",   camelCaseToAcronym("East").toString());
        assertEquals("NE",  camelCaseToAcronym("North-East").toString());
        assertEquals("NE",  camelCaseToAcronym("NORTH_EAST").toString());
        assertEquals("NE",  camelCaseToAcronym("northEast").toString());
    }

    /**
     * Tests the {@link CharSequences#isAcronymForWords(CharSequence, CharSequence)} method.
     */
    @Test
    public void testIsAcronymForWords() {
        /*
         * Following should be accepted as acronyms...
         */
        assertTrue(isAcronymForWords("OGC",                        "Open Geospatial Consortium"));
        assertTrue(isAcronymForWords("O.G.C.",                     "Open Geospatial Consortium"));
        assertTrue(isAcronymForWords("OpGeoCon",                   "Open Geospatial Consortium"));
        assertTrue(isAcronymForWords("Open Geospatial Consortium", "Open Geospatial Consortium"));
        assertTrue(isAcronymForWords("ogc",                        "Open Geospatial Consortium"));
        /*
         * Following should be rejected...
         */
        assertFalse(isAcronymForWords("ORC",    "Open Geospatial Consortium"));
        assertFalse(isAcronymForWords("O.C.G.", "Open Geospatial Consortium"));
        assertFalse(isAcronymForWords("OGC2",   "Open Geospatial Consortium"));
        assertFalse(isAcronymForWords("OG",     "Open Geospatial Consortium"));
        assertFalse(isAcronymForWords("GC",     "Open Geospatial Consortium"));
        /*
         * Following are mapping of EPSG table names from MS-Access to ANSI SQL.
         * All those items must be recognized as acroynms - this is requred by DirectEpsgFactory.
         */
        assertTrue(isAcronymForWords("alias",                     "[Alias]"));
        assertTrue(isAcronymForWords("area",                      "[Area]"));
        assertTrue(isAcronymForWords("coordinateaxis",            "[Coordinate Axis]"));
        assertTrue(isAcronymForWords("coordinateaxisname",        "[Coordinate Axis Name]"));
        assertTrue(isAcronymForWords("coordoperation",            "[Coordinate_Operation]"));
        assertTrue(isAcronymForWords("coordoperationmethod",      "[Coordinate_Operation Method]"));
        assertTrue(isAcronymForWords("coordoperationparam",       "[Coordinate_Operation Parameter]"));
        assertTrue(isAcronymForWords("coordoperationparamusage",  "[Coordinate_Operation Parameter Usage]"));
        assertTrue(isAcronymForWords("coordoperationparamvalue",  "[Coordinate_Operation Parameter Value]"));
        assertTrue(isAcronymForWords("coordoperationpath",        "[Coordinate_Operation Path]"));
        assertTrue(isAcronymForWords("coordinatereferencesystem", "[Coordinate Reference System]"));
        assertTrue(isAcronymForWords("coordinatesystem",          "[Coordinate System]"));
        assertTrue(isAcronymForWords("datum",                     "[Datum]"));
        assertTrue(isAcronymForWords("ellipsoid",                 "[Ellipsoid]"));
        assertTrue(isAcronymForWords("namingsystem",              "[Naming System]"));
        assertTrue(isAcronymForWords("primemeridian",             "[Prime Meridian]"));
        assertTrue(isAcronymForWords("supersession",              "[Supersession]"));
        assertTrue(isAcronymForWords("unitofmeasure",             "[Unit of Measure]"));
        assertTrue(isAcronymForWords("versionhistory",            "[Version History]"));
        assertTrue(isAcronymForWords("change",                    "[Change]"));
        assertTrue(isAcronymForWords("deprecation",               "[Deprecation]"));
        /*
         * It is important the the following is not recognized as an acronym,
         * otherwise it leads to a confusion in DirectEpsgFactory.
         */
        assertFalse(isAcronymForWords("coordoperation", "[Coordinate_Operation Method]"));
    }

    /**
     * Tests the {@link CharSequences#isJavaIdentifier(CharSequence)} method.
     */
    @Test
    public void testIsJavaIdentifier() {
        assertTrue ("A123", isJavaIdentifier("A123"));
        assertFalse("123A", isJavaIdentifier("123A"));
    }

    /**
     * Tests the {@link CharSequences#isUpperCase(CharSequence)} method.
     */
    @Test
    public void testIsUpperCase() {
        assertTrue ("ABC", isUpperCase("ABC"));
        assertFalse("AbC", isUpperCase("AbC"));
        assertFalse("A2C", isUpperCase("A2C")); // TODO: actually an unspecified behavior; we can change that.
    }

    /**
     * Tests the {@link CharSequences#equalsIgnoreCase(CharSequence, CharSequence)} method.
     */
    @Test
    public void testEqualsIgnoreCase() {
        assertTrue (equalsIgnoreCase("Test", "TEST"));
        assertTrue (equalsIgnoreCase("Test", new StringBuilder("TEST")));
        assertFalse(equalsIgnoreCase("Test1", "Test2"));
    }

    /**
     * Tests the {@link CharSequences#equals(CharSequence, CharSequence)} method.
     */
    @Test
    public void testEquals() {
        assertTrue (CharSequences.equals("Test", new StringBuilder("Test")));
        assertFalse(CharSequences.equals("Test1", "Test2"));
    }

    /**
     * Tests the {@link CharSequences#regionMatches(CharSequence, int, CharSequence)} method.
     */
    @Test
    public void testRegionMatches() {
        assertTrue (regionMatches(new StringBuilder("Un chasseur sachant chasser sans son chien"), 12, "sachant"));
        assertFalse(regionMatches(new StringBuilder("Un chasseur sachant chasser sans son chien"), 12, "sacHant"));
    }

    /**
     * Tests the {@link CharSequences#startsWith(CharSequence, CharSequence, boolean)} method.
     */
    @Test
    public void testStartsWith() {
        assertTrue (startsWith(new StringBuilder("Un chasseur sachant chasser sans son chien"), "un chasseur", true));
        assertFalse(startsWith(new StringBuilder("Un chasseur sachant chasser sans son chien"), "un chasseur", false));
    }

    /**
     * Tests the {@link CharSequences#endsWith(CharSequence, CharSequence, boolean)} method.
     */
    @Test
    public void testEndsWith() {
        assertTrue (endsWith(new StringBuilder("Un chasseur sachant chasser sans son chien"), "Son chien", true));
        assertFalse(endsWith(new StringBuilder("Un chasseur sachant chasser sans son chien"), "Son chien", false));
    }

    /**
     * Tests the {@link CharSequences#commonPrefix(CharSequence, CharSequence)} method.
     */
    @Test
    public void testCommonPrefix() {
        assertEquals("testCommon", commonPrefix(new StringBuilder("testCommonPrefix()"), "testCommonSuffix()"));
    }

    /**
     * Tests the {@link CharSequences#commonSuffix(CharSequence, CharSequence)} method.
     */
    @Test
    public void testCommonSuffix() {
        assertEquals("fix()", commonSuffix(new StringBuilder("testCommonPrefix()"), "testCommonSuffix()"));
    }

    /**
     * Tests the {@link CharSequences#token(CharSequence, int)} method.
     */
    @Test
    public void testToken() {
        assertEquals("Id4", token("..Id4  56B..", 2));
        assertEquals("56",  token("..Id4  56B..", 6));
    }
}
