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

import java.util.Set;
import java.text.ParseException;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.UnformattableObjectException;
import org.apache.sis.util.iso.DefaultNameSpace;

// Test dependencies
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.apache.sis.test.TestCase;


/**
 * Performs consistency checks on all CRS given by {@link CRS#getAuthorityFactory(String)}.
 * The consistency checks include:
 *
 * <ul>
 *   <li>Format in WKT, parse, reformat again and verify that we get the same WKT string.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ConsistencyTest extends TestCase {
    /**
     * Codes to exclude for now.
     */
    private static final Set<String> EXCLUDES = Set.of(
        "CRS:1",            // Computer display: WKT parser alters the (i,j) axis names.
        "EPSG:5819",        // EPSG topocentric example A: DerivedCRS wrongly handled as a ProjectedCRS. See SIS-518.
        "AUTO2:42001",      // This projection requires parameters, but we provide none.
        "AUTO2:42002",      // This projection requires parameters, but we provide none.
        "AUTO2:42003",      // This projection requires parameters, but we provide none.
        "AUTO2:42004",      // This projection requires parameters, but we provide none.
        "AUTO2:42005");     // This projection requires parameters, but we provide none.

    /**
     * Width of the code columns in the warnings formatted by {@link #print(String, String, Object)}.
     * We begin with an arbitrary width and will expand if necessary.
     */
    private int codeWidth = 15;

    /**
     * Creates a new test case.
     */
    public ConsistencyTest() {
    }

    /**
     * Specialization of {@link #testCoordinateReferenceSystems()} for specific cases that were known to fail.
     * This is used for debugging purposes only; not included in normal test execution because it is redundant
     * with {@link #testCoordinateReferenceSystems()}.
     *
     * @throws FactoryException if the coordinate reference system cannot be created.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-433">SIS-433</a>
     * @see <a href="https://issues.apache.org/jira/browse/SIS-434">SIS-434</a>
     */
    public void debug() throws FactoryException {
        final String code = "EPSG::29871";
        final CoordinateReferenceSystem crs = CRS.forCode(code);
        final var format = new WKTFormat();
        format.setConvention(Convention.WKT2);
        lookup(parseAndFormat(format, code, crs), crs);
    }

    /**
     * Verifies the WKT consistency of all CRS instances.
     *
     * @throws FactoryException if an error other than "unsupported operation method" occurred.
     */
    @Test
    @Tag(TAG_SLOW)
    public void testCoordinateReferenceSystems() throws FactoryException {
        assumeTrue(RUN_EXTENSIVE_TESTS, "Extensive tests not enabled.");
        final var v1  = new WKTFormat();
        final var v1c = new WKTFormat();
        final var v2  = new WKTFormat();
        final var v2s = new WKTFormat();
        v1 .setConvention(Convention.WKT1);
        v1c.setConvention(Convention.WKT1_COMMON_UNITS);
        v2 .setConvention(Convention.WKT2);
        v2s.setConvention(Convention.WKT2_SIMPLIFIED);
        for (final String code : CRS.getAuthorityFactory(null).getAuthorityCodes(CoordinateReferenceSystem.class)) {
            if (!EXCLUDES.contains(code) && !code.startsWith(Constants.PROJ4 + DefaultNameSpace.DEFAULT_SEPARATOR)) {
                final CoordinateReferenceSystem crs;
                try {
                    crs = CRS.forCode(code);
                } catch (UnavailableFactoryException | NoSuchIdentifierException | FactoryDataException e) {
                    print(code, "WARNING", e.getLocalizedMessage());
                    continue;
                }
                lookup(parseAndFormat(v2,  code, crs), crs);
                lookup(parseAndFormat(v2s, code, crs), crs);
                /*
                 * There is more information lost in WKT 1 than in WKT 2, so we cannot test everything.
                 * For example, we cannot format fully three-dimensional geographic CRS because the unit
                 * is not the same for all axes. We cannot format neither some axis directions.
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
    private void print(final String code, final String word, final Object message) {
        final int currentWidth = code.length();
        if (currentWidth >= codeWidth) {
            codeWidth = currentWidth + 1;
        }
        out.print(code);
        out.print(CharSequences.spaces(codeWidth - currentWidth));
        out.print(word);
        out.print(": ");
        out.println(message);
    }

    /**
     * Formats the given CRS using the given formatter, parses it and reformats again.
     * Then the two WKT are compared.
     *
     * @param  f     the formatter to use.
     * @param  code  the authority code, used only in case of errors.
     * @param  crs   the CRS to test.
     * @return the parsed CRS.
     */
    private CoordinateReferenceSystem parseAndFormat(final WKTFormat f,
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
            print(code, "ERROR", "Cannot parse the WKT below.");
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
                    assertEquals(',', line.charAt(--p), code);
                    assertEquals(']', line.charAt(--p), code);
                    assertEquals(']', line.charAt(--p), code);
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
                assertEquals(expectedLines[i], actualLines[i], code);
            }
        } catch (AssertionError e) {
            print(code, "ERROR", "WKT are not equal.");
            final var table = new TableAppender();
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
        assertEquals(expectedLines.length, actualLines.length);
        return parsed;
    }

    /**
     * Verifies that {@code IdentifiedObjects.lookupURN(…)} on the parsed CRS can find back the original CRS.
     */
    private void lookup(final CoordinateReferenceSystem parsed, final CoordinateReferenceSystem crs) throws FactoryException {
        final Identifier id = IdentifiedObjects.getIdentifier(crs, null);
        final String urn = IdentifiedObjects.toURN(crs.getClass(), id);
        assertNotNull(urn, crs.getName().getCode());
        /*
         * Lookup operation is not going to work if the CRS are not approximately equal.
         * However, in current Apache SIS implementation, we can perform this check only
         * if the scale factor of units of measurement have the exact same value.
         *
         * This check can be removed after the following issue is resolved:
         * https://issues.apache.org/jira/browse/SIS-433
         */
        if (toStandardUnit(crs   .getCoordinateSystem().getAxis(0).getUnit()).equals(
            toStandardUnit(parsed.getCoordinateSystem().getAxis(0).getUnit())))
        {
            assertTrue(Utilities.deepEquals(crs, parsed, ComparisonMode.DEBUG), urn);
            /*
             * Now test the lookup operation. Since the parsed CRS has an identifier,
             * that lookup operation should not do a lot of work actually.
             */
            final String lookup = IdentifiedObjects.lookupURN(parsed, null);
            assertEquals(urn, lookup, "Failed to lookup the parsed CRS.");
        } else {
            print(id.getCode(), "SKIPPED", "Unit conversion factors differ.");
        }
    }

    /**
     * Returns the converter to standard unit.
     */
    private static <Q extends Quantity<Q>> UnitConverter toStandardUnit(final Unit<Q> unit) {
        return unit.getConverterTo(unit.getSystemUnit());
    }
}
