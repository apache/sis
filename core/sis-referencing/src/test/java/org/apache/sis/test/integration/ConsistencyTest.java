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
package org.apache.sis.test.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.text.ParseException;
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.wkt.UnformattableObjectException;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Utilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assume.*;
import static org.junit.Assert.*;


/**
 * Performs consistency checks on all CRS given by {@link CRS#getAuthorityFactory(String)}.
 * The consistency checks include:
 *
 * <ul>
 *   <li>Format in WKT, parse, reformat again and verify that we get the same WKT string.</li>
 * </ul>
 *
 * This test is executed only of {@link #RUN_EXTENSIVE_TESTS} is {@code true}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class ConsistencyTest extends TestCase {
    /**
     * Codes to exclude for now.
     */
    private static final Set<String> EXCLUDES = new HashSet<String>(Arrays.asList(
            "CRS:1",            // Computer display
            "EPSG:5819"         // EPSG topocentric example A
    ));

    /**
     * Codes to exclude from the {@link #lookup(CoordinateReferenceSystem, CoordinateReferenceSystem)} test.
     * The reason why those tests are excluded now is related to the way JSR-275 parse units. We may resolve
     * those issues when we will replace JSR-275 by another library.
     */
    private static final Set<String> LOOKUP_EXCLUDES = Collections.singleton(
            "EPSG:5754"         // Poolbeg height. Uses British foot (1936).
    );

    /**
     * Verifies the WKT consistency of all CRS instances.
     *
     * @throws FactoryException if an error other than "unsupported operation method" occurred.
     */
    @Test
    public void testCoordinateReferenceSystems() throws FactoryException {
        assumeTrue(RUN_EXTENSIVE_TESTS);
        final WKTFormat v1  = new WKTFormat(null, null);
        final WKTFormat v1c = new WKTFormat(null, null);
        final WKTFormat v2  = new WKTFormat(null, null);
        final WKTFormat v2s = new WKTFormat(null, null);
        v1 .setConvention(Convention.WKT1);
        v1c.setConvention(Convention.WKT1_COMMON_UNITS);
        v2 .setConvention(Convention.WKT2);
        v2s.setConvention(Convention.WKT2_SIMPLIFIED);
        for (final String code : CRS.getAuthorityFactory(null).getAuthorityCodes(CoordinateReferenceSystem.class)) {
            if (!EXCLUDES.contains(code)) {
                final CoordinateReferenceSystem crs;
                try {
                    crs = CRS.forCode(code);
                } catch (NoSuchIdentifierException e) {
                    print(code, "WARNING", e.getLocalizedMessage());
                    continue;
                } catch (FactoryDataException e) {
                    print(code, "WARNING", e.getLocalizedMessage());
                    continue;
                }
                lookup(parseAndFormat(v2,  code, crs), crs);
                lookup(parseAndFormat(v2s, code, crs), crs);
                /*
                 * There is more information lost in WKT 1 than in WKT 2, so we can not test everything.
                 * For example we can not format fully three-dimensional geographic CRS because the unit
                 * is not the same for all axes. We can not format neither some axis directions.
                 */
                try {
                    parseAndFormat(v1, code, crs);
                } catch (UnformattableObjectException e) {
                    print(code, "WARNING", e.getLocalizedMessage());
                    continue;
                }
                parseAndFormat(v1c, code, crs);
            }
        }
    }

    /**
     * Prints the given code followed by spaces and the given {@code "ERROR"} or {@code "WARNING"} word,
     * then the given message.
     */
    private static void print(final String code, final String word, final Object message) {
        out.print(code);
        out.print(CharSequences.spaces(15 - code.length()));
        out.print(word);
        out.print(": ");
        out.println(message);
    }

    /**
     * Formats the given CRS using the given formatter, parses it and reformat again.
     * Then the two WKT are compared.
     *
     * @param  f    The formatter to use.
     * @param  code The authority code, used only in case of errors.
     * @param  crs  The CRS to test.
     * @return The parsed CRS.
     */
    private static CoordinateReferenceSystem parseAndFormat(final WKTFormat f,
            final String code, final CoordinateReferenceSystem crs)
    {
        String wkt = f.format(crs);
        final Warnings warnings = f.getWarnings();
        if (warnings != null && !warnings.getExceptions().isEmpty()) {
            print(code, "WARNING", warnings.getException(0));
        }
        final CoordinateReferenceSystem parsed;
        try {
            parsed = (CoordinateReferenceSystem) f.parseObject(wkt);
        } catch (ParseException e) {
            print(code, "ERROR", "Can not parse the WKT below.");
            out.println(wkt);
            out.println();
            e.printStackTrace(out);
            fail(e.getLocalizedMessage());
            return null;
        }
        final String again = f.format(parsed);
        final CharSequence[] expectedLines = CharSequences.splitOnEOL(wkt);
        final CharSequence[] actualLines   = CharSequences.splitOnEOL(again);
        /*
         * WKT 2 contains a line like below:
         *
         *   METHOD["Transverse Mercator", ID["EPSG", 9807, "8.9"]]
         *
         * But after parsing, the version number disaspear:
         *
         *   METHOD["Transverse Mercator", ID["EPSG", 9807]]
         *
         * This is a side effect of the fact that operation method are hard-coded in Java code.
         * This is normal for our implementation, so remove the version number from the expected lines.
         */
        if (f.getConvention().majorVersion() >= 2) {
            for (int i=0; i < expectedLines.length; i++) {
                final CharSequence line = expectedLines[i];
                int p = line.length();
                int s = CharSequences.skipLeadingWhitespaces(line, 0, p);
                if (CharSequences.regionMatches(line, s, "METHOD[\"")) {
                    assertEquals(code, ',', line.charAt(--p));
                    assertEquals(code, ']', line.charAt(--p));
                    assertEquals(code, ']', line.charAt(--p));
                    if (line.charAt(--p) == '"') {
                        p = CharSequences.lastIndexOf(line, ',', 0, p);
                        expectedLines[i] = line.subSequence(0, p) + "]],";
                    }
                }
            }
        }
        /*
         * Now compare the WKT line-by-line.
         */
        final int length = StrictMath.min(expectedLines.length, actualLines.length);
        try {
            for (int i=0; i<length; i++) {
                assertEquals(code, expectedLines[i], actualLines[i]);
            }
        } catch (AssertionError e) {
            print(code, "ERROR", "WKT are not equal.");
            final TableAppender table = new TableAppender();
            table.nextLine('═');
            table.setMultiLinesCells(true);
            table.append("Original WKT:");
            table.nextColumn();
            table.append("CRS parsed from the WKT:");
            table.nextLine();
            table.appendHorizontalSeparator();
            table.append(wkt);
            table.nextColumn();
            table.append(again);
            table.nextLine();
            table.nextLine('═');
            out.println(table);
            throw e;
        }
        assertEquals("Unexpected number of lines.", expectedLines.length, actualLines.length);
        return parsed;
    }

    /**
     * Verifies that {@code IdentifiedObjects.lookupURN(…)} on the parsed CRS can find back the original CRS.
     */
    private static void lookup(final CoordinateReferenceSystem parsed, final CoordinateReferenceSystem crs)
            throws FactoryException
    {
        final Identifier id = IdentifiedObjects.getIdentifier(crs, null);
        if (LOOKUP_EXCLUDES.contains(IdentifiedObjects.toString(id))) {
            return;
        }
        /*
         * Lookup operation is not going to work if the CRS are not approximatively equal.
         */
        final String urn = IdentifiedObjects.toURN(crs.getClass(), id);
        assertNotNull(crs.getName().getCode(), urn);
        assertTrue(urn, Utilities.deepEquals(crs, parsed, ComparisonMode.DEBUG));
        /*
         * Now test the lookup operation. Since the parsed CRS has an identifier,
         * that lookup operation should not do a lot of work actually.
         */
        final String lookup = IdentifiedObjects.lookupURN(parsed, null);
        assertEquals("Failed to lookup the parsed CRS.", urn, lookup);
    }
}
