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
package org.apache.sis.io.wkt;

import java.util.Set;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link WKTDictionary}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@DependsOn(WKTFormatTest.class)
public final strictfp class WKTDictionaryTest extends TestCase {
    /**
     * Tests {@link WKTDictionary#load(BufferedReader)}.
     *
     * @throws IOException if an error occurred while reading the test file.
     * @throws FactoryException if an error occurred while parsing a WKT.
     */
    @Test
    public void testLoad() throws IOException, FactoryException {
        final WKTDictionary factory = new WKTDictionary(null);
        try (BufferedReader source = new BufferedReader(new InputStreamReader(
                WKTFormatTest.class.getResourceAsStream("ExtraCRS.txt"), "UTF-8")))
        {
            factory.load(source);
        }
        /*
         * ESRI code space should be fist because it is the most frequently used
         * in the test file. The authority should be "ESRI" for the same reason.
         * Codes can be in any order.
         */
        assertArrayEquals("getCodeSpaces()", new String[] {"ESRI", "MyCodeSpace"}, factory.getCodeSpaces().toArray());
        assertSame("getAuthority()", Citations.ESRI, factory.getAuthority());
        Set<String> codes = factory.getAuthorityCodes(IdentifiedObject.class);
        assertSame(codes, factory.getAuthorityCodes(IdentifiedObject.class));       // Test caching.
        assertSame(codes, factory.getAuthorityCodes(SingleCRS.class));              // Test sharing.
        assertSetEquals(Arrays.asList("102018", "ESRI::102021", "MyCodeSpace::102021", "MyCodeSpace:v2:102021"), codes);
        assertSetEquals(Arrays.asList("102018", "ESRI::102021"), factory.getAuthorityCodes(ProjectedCRS.class));
        codes = factory.getAuthorityCodes(GeographicCRS.class);
        assertSetEquals(Arrays.asList("MyCodeSpace::102021", "MyCodeSpace:v2:102021"), codes);
        assertSame(codes, factory.getAuthorityCodes(GeodeticCRS.class));            // Test sharing.
        assertSame(codes, factory.getAuthorityCodes(GeographicCRS.class));          // Test caching.
        /*
         * Tests CRS creation.
         */
        verifyCRS(factory.createProjectedCRS(     "102018"), "North_Pole_Stereographic", +90);
        verifyCRS(factory.createProjectedCRS("ESRI:102021"), "South_Pole_Stereographic", -90);
        verifyCRS(factory.createGeographicCRS("MyCodeSpace::102021"), "Anguilla 1957");
        verifyCRS(factory.createGeographicCRS("MyCodeSpace:v2:102021"), "Anguilla 1957 (bis)");
    }

    /**
     * Verifies a projected CRS.
     *
     * @param crs   the CRS to verify.
     * @param name  expected CRS name.
     * @param φ0    expected latitude of origin.
     */
    private static void verifyCRS(final ProjectedCRS crs, final String name, final double φ0) {
        assertEquals("name", name, crs.getName().getCode());
        assertAxisDirectionsEqual(name, crs.getCoordinateSystem(),
                                  AxisDirection.EAST, AxisDirection.NORTH);
        assertEquals("φ0", φ0, crs.getConversionFromBase().getParameterValues()
                                  .parameter("Latitude of natural origin").doubleValue(), STRICT);
    }

    /**
     * Verifies a geographic CRS.
     *
     * @param crs   the CRS to verify.
     * @param name  expected CRS name.
     */
    private static void verifyCRS(final GeographicCRS crs, final String name) {
        assertEquals("name", name, crs.getName().getCode());
        assertAxisDirectionsEqual(name, crs.getCoordinateSystem(),
                                  AxisDirection.NORTH, AxisDirection.EAST);
    }
}
