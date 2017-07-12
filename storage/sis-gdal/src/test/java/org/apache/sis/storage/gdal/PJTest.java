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
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.measure.Units;
import org.apache.sis.test.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.*;


/**
 * Tests the {@link PJ} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class PJTest extends TestCase {
    /**
     * If the Proj4 library has been successfully initialized, an empty string.
     * Otherwise, the reason why the library is not available.
     */
    private static String status;

    /**
     * Verifies if the Proj4 library is available.
     */
    @BeforeClass
    public static synchronized void verifyNativeLibraryAvailability() {
        if (status == null) try {
            out.println("Proj.4 version: " + PJ.getVersion());
            status = "";
        } catch (UnsatisfiedLinkError e) {
            status = e.toString();
        }
        assumeTrue(status, status.isEmpty());
    }

    /**
     * Ensures that the given object is the WGS84 definition.
     */
    private static void assertIsWGS84(final PJ pj) {
        assertEquals("+proj=latlong +datum=WGS84 +ellps=WGS84 +towgs84=0,0,0", pj.getDefinition().trim());
        assertEquals("Lat/long (Geodetic alias)", pj.toString().trim());
        assertEquals(PJ.Type.GEOGRAPHIC, pj.getType());
        assertEquals("WGS84",            pj.getName().getCode());
        assertEquals(6378137.0,          pj.getSemiMajorAxis(),         1E-9);
        assertEquals(6356752.314245179,  pj.getSemiMinorAxis(),         1E-9);
        assertEquals(298.257223563,      pj.getInverseFlattening(),     1E-9);
        assertEquals(0.0,                pj.getGreenwichLongitude(),    STRICT);
        assertEquals(1.0,                pj.getLinearUnitToMetre(true), STRICT);
        assertSame  (Units.METRE,        pj.getAxisUnit());
        assertFalse(pj.isSphere());
        assertArrayEquals(new char[] {'e', 'n', 'u'}, pj.getAxisDirections());
    }

    /**
     * Tests the creation of a simple WGS84 object.
     *
     * @throws InvalidGeodeticParameterException if the Proj4 definition string used in this test is invalid.
     */
    @Test
    public void testWGS84() throws InvalidGeodeticParameterException {
        final PJ pj = new PJ(null, "+proj=latlong +datum=WGS84");
        assertIsWGS84(pj);
    }

    /**
     * Tests the creation of the EPSG:3395 projected CRS.
     *
     * @throws InvalidGeodeticParameterException if the Proj4 definition string used in this test is invalid.
     */
    @Test
    public void testEPSG3395() throws InvalidGeodeticParameterException {
        final PJ pj = new PJ(null, "+init=epsg:3395");
        assertEquals(PJ.Type.PROJECTED, pj.getType());
        assertArrayEquals(new char[] {'e', 'n', 'u'}, pj.getAxisDirections());
        assertEquals(1.0, pj.getLinearUnitToMetre(true), STRICT);
        assertIsWGS84(new PJ(pj));
    }

    /**
     * Tests the {@link PJ#getLinearUnit(boolean)} method.
     *
     * @throws InvalidGeodeticParameterException if the Proj4 definition string used in this test is invalid.
     */
    @Test
    public void testGetLinearUnit() throws InvalidGeodeticParameterException {
        PJ pj;
        pj = new PJ(null, "+proj=merc +to_meter=1");
        assertSame(Units.METRE, pj.getLinearUnit(false));
        pj = new PJ(null, "+proj=merc +to_meter=0.001");
        assertSame(Units.KILOMETRE, pj.getLinearUnit(false));
    }

    /**
     * Ensures that the native code correctly detects the case of null pointers.
     * This is important in order to ensure that we don't have a JVM crash.
     *
     * @throws InvalidGeodeticParameterException if the Proj4 definition string used in this test is invalid.
     * @throws TransformException should never happen.
     */
    @Test
    public void testNullPointerException() throws InvalidGeodeticParameterException, TransformException {
        final PJ pj = new PJ(null, "+proj=latlong +datum=WGS84");
        try {
            pj.transform(null, 2, null, 0, 1);
            fail("Expected an exception to be thrown.");
        } catch (NullPointerException e) {
            // This is the expected exception.
        }
    }

    /**
     * Ensures that the native code correctly detects the case of index out of bounds.
     * This is important in order to ensure that we don't have a JVM crash.
     *
     * @throws InvalidGeodeticParameterException if the Proj4 definition string used in this test is invalid.
     * @throws TransformException should never happen.
     */
    @Test
    public void testIndexOutOfBoundsException() throws InvalidGeodeticParameterException, TransformException {
        final PJ pj = new PJ(null, "+proj=latlong +datum=WGS84");
        try {
            pj.transform(pj, 2, new double[5], 2, 2);
            fail("Expected an exception to be thrown.");
        } catch (IndexOutOfBoundsException e) {
            // This is the expected exception.
        }
    }

    /**
     * Tests a method that returns NaN. The native code is expected to returns the
     * {@link java.lang.Double#NaN} constant, because not all C/C++ compiler define
     * a {@code NAN} constant.
     *
     * @throws InvalidGeodeticParameterException if the Proj4 definition string used in this test is invalid.
     */
    @Test
    @SuppressWarnings("FinalizeCalledExplicitly")
    public void testNaN() throws InvalidGeodeticParameterException {
        final PJ pj = new PJ(null, "+proj=latlong +datum=WGS84");
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
