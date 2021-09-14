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
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link CRSCommand} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
@DependsOn(CommandRunnerTest.class)
public final strictfp class CRSCommandTest extends TestCase {
    /**
     * The Well Known Text for EPSG:4326 as a regular expression.
     * This string uses the native line separator. Consequently test cases comparing
     * against this pattern are expected to use that line separator for every lines.
     */
    private String WGS84;

    /**
     * Creates a new test.
     */
    public CRSCommandTest() {
        WGS84 =
            "\\QGeodeticCRS[\"WGS 84\",\n" +
            "  Datum[\"World Geodetic System 1984\",\n" +
            "    Ellipsoid[\"WGS 84\", 6378137.0, 298.257223563]],\n" +
            "  CS[ellipsoidal, 2],\n" +
            "    Axis[\"Latitude (B)\", north],\n" +
            "    Axis[\"Longitude (L)\", east],\n" +
            "    Unit[\"degree\", 0.017453292519943295],\n" +
            "  Scope[\"Horizontal component of 3D system.\\E.*\\Q\"],\n" +                      // EPSG geodetic dataset provides more details here.
            "  Area[\"\\E.*\\Q\"],\n" +                                                         // Language may vary because of SIS localization.
            "  BBox[-90.00, -180.00, 90.00, 180.00],\n" +
            "  Id[\"EPSG\", 4326,\\E.*\\Q URI[\"urn:ogc:def:crs:EPSG:\\E.*\\Q:4326\"]]]\n\\E";  // Version number of EPSG dataset may vary.
        /*
         * Insert the native line separator in the expected string. We modify the expected string
         * instead of modifying the `test.outputBuffer` result because we want to verify that the
         * native line separator appears in every output lines.
         */
        WGS84 = CharSequences.replace(WGS84, "\n", System.lineSeparator()).toString();
    }

    /**
     * Tests fetching the CRS from a simple code ({@code "EPSG:4326"}).
     *
     * @throws Exception if an error occurred while creating the command.
     */
    @Test
    public void testCode() throws Exception {
        final CRSCommand test = new CRSCommand(0, CommandRunner.TEST, "EPSG:4326");
        test.run();
        final String wkt = test.outputBuffer.toString();
        assertTrue(wkt, wkt.matches(WGS84));
    }

    /**
     * Tests fetching the CRS from a URN ({@code "urn:ogc:def:crs:epsg::4326"}).
     *
     * @throws Exception if an error occurred while creating the command.
     */
    @Test
    @DependsOnMethod("testCode")
    public void testURN() throws Exception {
        final CRSCommand test = new CRSCommand(0, CommandRunner.TEST, "urn:ogc:def:crs:epsg::4326");
        test.run();
        final String wkt = test.outputBuffer.toString();
        assertTrue(wkt, wkt.matches(WGS84));
    }

    /**
     * Tests fetching the CRS from a HTTP code ({@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}).
     *
     * @throws Exception if an error occurred while creating the command.
     */
    @Test
    @DependsOnMethod("testURN")
    public void testHTTP() throws Exception {
        final CRSCommand test = new CRSCommand(0, CommandRunner.TEST, "http://www.opengis.net/gml/srs/epsg.xml#4326");
        test.run();
        final String wkt = test.outputBuffer.toString();
        assertTrue(wkt, wkt.matches(WGS84));
    }
}
