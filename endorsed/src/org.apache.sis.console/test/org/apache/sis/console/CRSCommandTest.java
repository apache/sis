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
package org.apache.sis.console;

import org.apache.sis.util.CharSequences;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link CRSCommand} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class CRSCommandTest extends TestCase {
    /**
     * The Well Known Text for EPSG:4326 as a regular expression.
     * This string uses the native line separator. Consequently, test cases comparing
     * against this pattern are expected to use that line separator for every lines.
     */
    private String WGS84;

    /**
     * Creates a new test.
     */
    public CRSCommandTest() {
        final String name = "\"WGS\\E\\s?(?:19)?\\Q84\"";                                       // Accept "WGS 84" or "WGS 1984".
        WGS84 = "(?m)\\Q" +                                                                     // Multilines.
            "GeographicCRS[" + name + ",\n" +
            "  Ensemble[\"World Geodetic System 1984\\E\\s?\\w*\\Q\",\n" +                      // Ignore any suffix in the name.
            "\\E(?:    Member\\[\".+\"\\],\n)+\\Q" +                                            // At least one MEMBER[…].
            "    Ellipsoid[" + name + ", 6378137.0, 298.257223563],\n" +
            "    EnsembleAccuracy[2.0]],\n" +
            "  CS[ellipsoidal, 2],\n" +
            "    Axis[\"Latitude (B)\", north],\n" +
            "    Axis[\"Longitude (L)\", east],\n" +
            "    Unit[\"degree\", 0.017453292519943295],\n" +
            "  Usage[\n" +
            "\\E(?:    Scope\\[\".+\"\\],\n)?\\Q" +                                             // Ignore SCOPE[…] if present.
            "    Area[\"\\E.*\\Q\"],\n" +                                                       // Language may vary because of SIS localization.
            "    BBox[-90.00, -180.00, 90.00, 180.00]],\n" +
            "  Id[\"EPSG\", 4326,\\E.*\\Q URI[\"urn:ogc:def:crs:EPSG:\\E.*\\Q:4326\"]]" +       // Version number of EPSG dataset may vary.
            "\\E(?:,\n  Remark\\[\".+\"\\])?\\]\n";                                             // Ignore trailing REMARK[…] if present.
        /*
         * Insert the native line separator in the expected string. We modify the expected string
         * instead of modifying the `test.outputBuffer` result because we want to verify that the
         * native line separator appears in every output lines.
         */
        WGS84 = CharSequences.replace(WGS84, "\n", System.lineSeparator()).toString();
    }

    /**
     * Tests with the given code. This method formats the output, but check for a match
     * only if the <abbr>EPSG</abbr> database is present. We recognize this situation by
     * the fact that the {@code CommonCRS} fallback defines the datum in the old way.
     *
     * @param  code  the code to test.
     * @throws Exception if an error occurred while creating the command.
     */
    private void test(final String code) throws Exception {
        var test = new CRSCommand(0, new String[] {CommandRunner.TEST, code});
        test.run();
        String wkt = test.outputBuffer.toString();
        if (wkt.contains("Datum[\"World Geodetic System 1984\"")) {
            if (REQUIRE_EPSG_DATABASE) {
                fail("EPSG database is absent or too old.");
            }
        } else {
            assertTrue(wkt.matches(WGS84), wkt);
        }
    }

    /**
     * Tests fetching the CRS from a simple code ({@code "EPSG:4326"}).
     *
     * @throws Exception if an error occurred while creating the command.
     */
    @Test
    public void testCode() throws Exception {
        test("EPSG:4326");
    }

    /**
     * Tests fetching the CRS from a URN ({@code "urn:ogc:def:crs:epsg::4326"}).
     *
     * @throws Exception if an error occurred while creating the command.
     */
    @Test
    public void testURN() throws Exception {
        test("urn:ogc:def:crs:epsg::4326");
    }

    /**
     * Tests fetching the CRS from a HTTP code ({@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}).
     *
     * @throws Exception if an error occurred while creating the command.
     */
    @Test
    public void testHTTP() throws Exception {
        test("http://www.opengis.net/gml/srs/epsg.xml#4326");
    }
}
