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
package org.apache.sis.internal.sql.feature;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.opengis.referencing.crs.GeographicCRS;

import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.test.TestCase;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Tests the parsing of geometries encoded in Extended Well Known Binary (EWKB) format.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 *
 * @todo Run the same test for all supported geometry implementations (ESRI and JTS).
 */
public strictfp class EWKBTest extends TestCase {
    /**
     * The factory to use for creating geometric objects.
     * It requires a geometry implementation to be available on the classpath.
     */
    private final Geometries<?> GF;

    /**
     * Creates a new test using JTS geometry implementation.
     */
    public EWKBTest() {
        GF = Geometries.implementation(GeometryLibrary.JTS);
    }

    /**
     * Decodes a geometry encoded in EWKB format and compares with the geometry specified in WKT format.
     *
     * @param  wkt        WKT representation of the geometry. This is used as the reference value.
     * @param  hexEWKB    EWKB representation of the same geometry. This is the value to test.
     * @throws Exception  if an error occurred while decoding one of the given string.
     */
    public void decodeHexadecimal(final String wkt, final String hexEWKB) throws Exception {
        final GeographicCRS expectedCrs = CommonCRS.defaultGeographic();
        final EWKBReader reader = new EWKBReader(GF).forCrs(expectedCrs);
        assertEquals("WKT and hexadecimal EWKB representation don't match",
                GF.parseWKT(wkt).implementation(), reader.readHexa(hexEWKB));
    }

    /**
     * Tests the decoding of a geometry from a byte stream. The purpose of this test is not to check complex geometries,
     * which are validated by {@link #decodeHexadecimal(String, String)}. This test only ensures that decoding directly
     * a byte stream behaves in the same way than through hexadecimal.
     */
    @Test
    public void testBinary() {
        final ByteBuffer point = ByteBuffer.allocate(21);
        point.put((byte) 0);    // XDR mode.

        // Create a 2D point.
        point.putInt(1);
        point.putDouble(42.2);
        point.putDouble(43.3);

        // Read the point.
        point.rewind();
        final Object read = new EWKBReader(GeometryLibrary.JTS).read(point);
        assertEquals(GF.createPoint(42.2, 43.3), read);
    }

    /**
     * Temporary test for simulating JUnit 5 execution of {@link #decodeHexadecimal(String, String)}
     * as a parameterized test. To be removed after migration to JUnit 5.
     *
     * @throws Exception if test file can not be decoded.
     */
    @Test
    public void testDecodeHexadecimal() throws Exception {
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(
                EWKBTest.class.getResourceAsStream("hexa_ewkb_4326.csv"), StandardCharsets.UTF_8)))
        {
            String line;
            int numLinesToSkip = 1;
            while ((line = in.readLine()) != null) {
                if (!(line = line.trim()).isEmpty() && line.charAt(0) != '#' && --numLinesToSkip < 0) {
                    final String[] columns = line.split("\t");
                    assertEquals(2, columns.length);
                    decodeHexadecimal(columns[0], columns[1]);
                }
            }
        }
    }
}
