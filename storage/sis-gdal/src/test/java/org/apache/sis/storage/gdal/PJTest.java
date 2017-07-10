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
package org.apache.sis.storage.gdal;

import org.opengis.referencing.operation.TransformException;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link PJ} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class PJTest {
    /**
     * Ensures that the given object is the WGS84 definition.
     */
    private static void assertIsWGS84(final PJ pj) {
        assertEquals("+proj=latlong +datum=WGS84 +ellps=WGS84 +towgs84=0,0,0", pj.getDefinition().trim());
        assertEquals("Lat/long (Geodetic alias)", pj.toString().trim());
        assertEquals(PJ.Type.GEOGRAPHIC, pj.getType());
        assertEquals(6378137.0,          pj.getSemiMajorAxis(),         1E-9);
        assertEquals(6356752.314245179,  pj.getSemiMinorAxis(),         1E-9);
        assertEquals(0.0,                pj.getGreenwichLongitude(),    0.0);
        assertEquals(1.0,                pj.getLinearUnitToMetre(true), 0.0);
        assertArrayEquals(new char[] {'e', 'n', 'u'}, pj.getAxisDirections());
    }

    /**
     * Tests the creation of a simple WGS84 object.
     */
    @Test
    public void testWGS84() {
        final PJ pj = new PJ("+proj=latlong +datum=WGS84");
        assertIsWGS84(pj);
    }

    /**
     * Tests the creation of the EPSG:3395 projected CRS
     */
    @Test
    public void testEPSG3395() {
        final PJ pj = new PJ("+init=epsg:3395");
        assertEquals(PJ.Type.PROJECTED, pj.getType());
        assertArrayEquals(new char[] {'e', 'n', 'u'}, pj.getAxisDirections());
        assertEquals(1.0, pj.getLinearUnitToMetre(true), 0.0);
        assertIsWGS84(new PJ(pj));
    }

    /**
     * Ensures that the native code correctly detects the case of null pointers.
     * This is important in order to ensure that we don't have a JVM crash.
     *
     * @throws TransformException should never happen.
     */
    @Test(expected = NullPointerException.class)
    public void testNullPointerException() throws TransformException {
        final PJ pj = new PJ("+proj=latlong +datum=WGS84");
        pj.transform(null, 2, null, 0, 1);
    }

    /**
     * Ensures that the native code correctly detects the case of index out of bounds.
     * This is important in order to ensure that we don't have a JVM crash.
     *
     * @throws TransformException should never happen.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testIndexOutOfBoundsException() throws TransformException {
        final PJ pj = new PJ("+proj=latlong +datum=WGS84");
        pj.transform(pj, 2, new double[5], 2, 2);
    }

    /**
     * Tests a method that returns NaN. The native code is expected to returns the
     * {@link java.lang.Double#NaN} constant, because not all C/C++ compiler define
     * a {@code NAN} constant.
     */
    @Test
    @SuppressWarnings("FinalizeCalledExplicitly")
    public void testNaN() {
        final PJ pj = new PJ("+proj=latlong +datum=WGS84");
        pj.finalize();              // This cause the disposal of the internal PJ structure.
        assertNull(pj.getType());
        assertNaN(pj.getSemiMajorAxis());
        assertNaN(pj.getSemiMinorAxis());
        assertNaN(pj.getEccentricitySquared());
        assertNaN(pj.getGreenwichLongitude());
        assertNaN(pj.getLinearUnitToMetre(false));
        assertNaN(pj.getLinearUnitToMetre(true));
    }

    /**
     * Asserts that the bits pattern of the given value is strictly identical to the bits
     * pattern of the {@link java.lang.Double#NaN} constant.
     */
    private static void assertNaN(final double value) {
        assertEquals(Double.doubleToRawLongBits(Double.NaN), Double.doubleToRawLongBits(value));
    }
}
