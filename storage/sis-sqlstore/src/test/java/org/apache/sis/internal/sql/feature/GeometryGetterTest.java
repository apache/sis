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

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.feature.GeometryWithCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.test.TestCase;
import org.junit.Test;

// Optional dependencies
import org.locationtech.jts.geom.Geometry;

import static org.junit.Assert.*;


/**
 * Tests the parsing of geometries encoded in Well-Known Binary (WKB) format.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public strictfp final class GeometryGetterTest extends TestCase {
    /**
     * The factory to use for creating geometric objects.
     */
    private Geometries<?> GF;

    /**
     * Creates a new test case.
     */
    public GeometryGetterTest() {
    }

    /**
     * Creates a reader to use for testing.
     *
     * @param  library   the geometry implementation to use (JTS or ESRI).
     * @param  encoding  the way binary data are encoded (raw or hexadecimal).
     */
    @SuppressWarnings("unchecked")
    private GeometryGetter<?,?> createReader(final GeometryLibrary library, final BinaryEncoding encoding) {
        GF = Geometries.implementation(library);
        return new GeometryGetter<>(GF, (Class) GF.rootClass, HardCodedCRS.WGS84, encoding);
    }

    /**
     * Tests the decoding of a geometry from a byte array using JTS library.
     * The array does not use any encoding.
     *
     * @throws Exception if an error occurred while decoding the WKB.
     */
    @Test
    public void testBinaryWithJTS() throws Exception {
        testBinary(GeometryLibrary.JTS);
    }

    /**
     * Tests the decoding of a geometry from a byte array using ESRI library.
     * The array does not use any encoding.
     *
     * @throws Exception if an error occurred while decoding the WKB.
     */
    @Test
    public void testBinaryWithESRI() throws Exception {
        testBinary(GeometryLibrary.ESRI);
    }

    /**
     * Implementation of {@link #testBinaryWithJTS()} and {@link #testBinaryWithESRI()} methods.
     */
    private void testBinary(final GeometryLibrary library) throws Exception {
        final ByteBuffer point = ByteBuffer.allocate(21);
        point.put((byte) 0);    // XDR mode.

        // Create a 2D point.
        point.putInt(1);
        point.putDouble(42.2);
        point.putDouble(43.3);

        // Read the point.
        point.rewind();
        final ResultSet r = ResultSetMock.create(point.array());
        final Object geometry = createReader(library, BinaryEncoding.RAW).getValue(null, r, 1);
        assertEquals(GF.createPoint(42.2, 43.3), geometry);
        final GeometryWrapper<?> wrapper = Geometries.implementation(library).castOrWrap(geometry);
        /*
         * If the wrapper is an instance of `GeometryWithCRS`, then the CRS is stored
         * with the wrapper instead of the geometry implementation. In such case, the
         * CRS is lost on `GeometryWrapper.implementation()` and can not be tested.
         */
        if (!(wrapper instanceof GeometryWithCRS)) {
            assertSame(HardCodedCRS.WGS84, wrapper.getCoordinateReferenceSystem());
        }
    }

    /**
     * Compares WKB with WKT parsing using the {@code features."Geometries"} view of test database.
     * This test is <em>not</em> executed by this {@code GeometryGetterTest} class. This is a method
     * to be invoked from {@linkplain org.apache.sis.internal.sql.postgis.PostgresTest#testGeometryGetter
     * another test class} having a connection to a database.
     *
     * @param  connection     connection to the database.
     * @param  fromSridToCRS  the resolver of Spatial Reference Identifier (SRID) to CRS, or {@code null}.
     * @param  encoding       the way binary data are encoded (raw or hexadecimal).
     * @throws Exception if an error occurred while querying the database or parsing the WKT or WKB.
     */
    public void testFromDatabase(final Connection connection, final InfoStatements fromSridToCRS,
            final BinaryEncoding encoding) throws Exception
    {
        final GeometryGetter<?,?> reader = createReader(GeometryLibrary.JTS, encoding);
        try (Statement stmt = connection.createStatement();
             ResultSet results = stmt.executeQuery("SELECT \"WKT\",\"WKB\",\"SRID\" FROM features.\"Geometries\""))
        {
            while (results.next()) {
                final String wkt = results.getString(1);
                final Geometry geometry = (Geometry) reader.getValue(fromSridToCRS, results, 2);
                final GeometryWrapper<?> expected = GF.parseWKT(wkt);
                assertEquals("WKT and WKB parsings gave different results.", expected.implementation(), geometry);
                assertSame("SRID", getExpectedCRS(results.getInt(3)),
                           GF.castOrWrap(geometry).getCoordinateReferenceSystem());
            }
        }
    }

    /**
     * Returns the expected CRS for the given SRID. Note that a SRID is not necessary an EPSG code.
     * This method accepts only the SRID used in the {@code "SpatialFeatures"} test schema.
     * The mapping from SRID to CRS is hard-coded to the same mapping than the spatial database
     * used for the test. Other databases may have different mapping.
     *
     * @param  srid  the SRID for which to get the CRS.
     * @return the CRS for the given SRID.
     * @throws FactoryException if an error occurred while fetching the CRS.
     */
    public static CoordinateReferenceSystem getExpectedCRS(final int srid) throws FactoryException {
        switch (srid) {
            case 3395: return CRS.forCode("EPSG:3395");
            case 4326: return CommonCRS.WGS84.normalizedGeographic();
            default:   throw new AssertionError(srid);
        }
    }
}
