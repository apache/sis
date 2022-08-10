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
package org.apache.sis.openoffice;

import org.opengis.util.FactoryException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;


/**
 * Tests {@link Transformer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class TransformerTest extends TestCase {
    /**
     * The instance to use for testing purpose.
     */
    private static ReferencingFunctions caller;

    /**
     * Creates a {@link ReferencingFunctions} instance to use for all tests.
     */
    @BeforeClass
    public static void createReferencingInstance() {
        caller = new ReferencingFunctions(null);
        caller.setLocale(new com.sun.star.lang.Locale("en", "US", null));
    }

    /**
     * Disposes the {@link ReferencingFunctions} instance after all tests completed.
     */
    @AfterClass
    public static void disposeReferencingInstance() {
        caller = null;
    }

    /**
     * Asserts that the transformation result is equal to the expected result.
     */
    static void assertPointsEqual(final double[][] expected, final double[][] actual, final double tolerance) {
        assertNotSame("transform", expected, actual);
        assertEquals("transform.length", expected.length, actual.length);
        for (int i=0; i<expected.length; i++) {
            assertArrayEquals(expected[i], actual[i], tolerance);
        }
    }

    /**
     * Tests a trivial identity operation. The main purpose of this method is to test the constructor.
     *
     * @throws FactoryException if an error occurred while creating the object.
     * @throws DataStoreException if an error occurred while reading a data file.
     */
    @Test
    public void testIdentity() throws FactoryException, DataStoreException {
        final double[][] points = {
            new double[] {30,  20},
            new double[] {34,  17},
            new double[] {27, -12},
            new double[] {32,  23}
        };
        final Transformer tr = new Transformer(caller, CommonCRS.WGS84.geographic(), "EPSG:4326", points);
        assumeTrue(tr.hasAreaOfInterest());     // False if there is no EPSG geodetic dataset installed.
        final GeographicBoundingBox bbox = tr.getAreaOfInterest();
        assertEquals("eastBoundLongitude",  23, bbox.getEastBoundLongitude(), STRICT);
        assertEquals("westBoundLongitude", -12, bbox.getWestBoundLongitude(), STRICT);
        assertEquals("northBoundLatitude",  34, bbox.getNorthBoundLatitude(), STRICT);
        assertEquals("southBoundLatitude",  27, bbox.getSouthBoundLatitude(), STRICT);
        assertPointsEqual(points, tr.transform(points), STRICT);
    }

    /**
     * Same test than {@link #testIdentity()} except that the height values are dropped.
     *
     * @throws FactoryException if an error occurred while creating the object.
     * @throws DataStoreException if an error occurred while reading a data file.
     */
    @Test
    @DependsOnMethod("testIdentity")
    public void test3D_to_2D() throws FactoryException, DataStoreException {
        final double[][] points = {
            new double[] {30,  20,  4},
            new double[] {34,  17, -3},
            new double[] {27, -12, 12},
            new double[] {32,  23, -1}
        };
        final double[][] result = {
            new double[] {30,  20},
            new double[] {34,  17},
            new double[] {27, -12},
            new double[] {32,  23}
        };
        final Transformer tr = new Transformer(caller, CommonCRS.WGS84.geographic3D(), "EPSG:4326", points);
        assumeTrue(tr.hasAreaOfInterest());     // False if there is no EPSG geodetic dataset installed.
        final GeographicBoundingBox bbox = tr.getAreaOfInterest();
        assertEquals("eastBoundLongitude",  23, bbox.getEastBoundLongitude(), STRICT);
        assertEquals("westBoundLongitude", -12, bbox.getWestBoundLongitude(), STRICT);
        assertEquals("northBoundLatitude",  34, bbox.getNorthBoundLatitude(), STRICT);
        assertEquals("southBoundLatitude",  27, bbox.getSouthBoundLatitude(), STRICT);
        assertPointsEqual(result, tr.transform(points), STRICT);
    }
}
