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

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.test.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assume.*;
import static org.apache.sis.test.Assert.*;


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
     * If the {@literal Proj.4} library has been successfully initialized, an empty string.
     * Otherwise, the reason why the library is not available.
     */
    private static String status;

    /**
     * Verifies if the {@literal Proj.4} library is available.
     */
    @BeforeClass
    public static synchronized void verifyNativeLibraryAvailability() {
        if (status == null) try {
            out.println("Proj.4 version: " + PJ.getRelease());
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
        assertEquals("+proj=latlong +datum=WGS84 +ellps=WGS84 +towgs84=0,0,0", pj.getCode().trim());
        assertEquals("Proj4",            pj.getCodeSpace());
        assertSame  (Citations.PROJ4,    pj.getAuthority());
        assertTrue  (Character.isDigit(  pj.getVersion().codePointAt(0)));
        assertEquals(PJ.Type.GEOGRAPHIC, pj.getType());
        final double[] ellps = pj.getEllipsoidDefinition();
        assertEquals(2, ellps.length);
        ellps[1] = 1 / (1 - StrictMath.sqrt(1 - ellps[1]));     // Replace eccentricity squared by inverse flattening.
        assertArrayEquals(new double[] {6378137.0, 298.257223563}, ellps, 1E-9);
    }

    /**
     * Tests the creation of a simple WGS84 object.
     *
     * @throws FactoryException if the Proj.4 definition string used in this test is invalid.
     */
    @Test
    public void testWGS84() throws FactoryException {
        final PJ pj = new PJ("+proj=latlong +datum=WGS84");
        assertIsWGS84(pj);
    }

    /**
     * Tests the creation of the EPSG:3395 projected CRS.
     *
     * @throws FactoryException if the Proj.4 definition string used in this test is invalid.
     */
    @Test
    public void testEPSG3395() throws FactoryException {
        final PJ pj = new PJ("+init=epsg:3395");
        assertEquals(PJ.Type.PROJECTED, pj.getType());
        final String definition = pj.getCode();
        assertTrue(definition, definition.contains("+proj=merc"));
        assertTrue(definition, definition.contains("+datum=WGS84"));
        assertTrue(definition, definition.contains("+units=m"));
        assertIsWGS84(new PJ(pj));
    }

    /**
     * Tests the units of measurement.
     *
     * @throws FactoryException if the Proj.4 definition string used in this test is invalid.
     */
    @Test
    public void testGetLinearUnit() throws FactoryException {
        PJ pj;
        pj = new PJ("+proj=merc +to_meter=1");
        String definition = pj.getCode();
        assertTrue(definition, definition.contains("+to_meter=1"));
        pj = new PJ("+proj=merc +to_meter=0.001");
        definition = pj.getCode();
        assertTrue(definition, definition.contains("+to_meter=0.001"));
    }

    /**
     * Ensures that the native code correctly detects the case of null pointers.
     * This is important in order to ensure that we don't have a JVM crash.
     *
     * @throws FactoryException if the Proj.4 definition string used in this test is invalid.
     * @throws TransformException should never happen.
     */
    @Test
    public void testNullPointerException() throws FactoryException, TransformException {
        final PJ pj = new PJ("+proj=latlong +datum=WGS84");
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
     * @throws FactoryException if the Proj.4 definition string used in this test is invalid.
     * @throws TransformException should never happen.
     */
    @Test
    public void testIndexOutOfBoundsException() throws FactoryException, TransformException {
        final PJ pj = new PJ("+proj=latlong +datum=WGS84");
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
     * @throws FactoryException if the Proj.4 definition string used in this test is invalid.
     */
    @Test
    @SuppressWarnings("FinalizeCalledExplicitly")
    public void testNaN() throws FactoryException {
        final PJ pj = new PJ("+proj=latlong +datum=WGS84");
        pj.finalize();              // This cause the disposal of the internal PJ structure.
        assertNull(pj.getCode());
        assertNull(pj.getType());
        assertNull(pj.getEllipsoidDefinition());
    }

    /**
     * Tests serialization. Since we can not serialize native resources, {@link PJ} is expected
     * to serialize the Proj.4 definition string instead.
     *
     * @throws FactoryException if the Proj.4 definition string used in this test is invalid.
     */
    @Test
    public void testSerialization() throws FactoryException {
        final PJ pj = new PJ("+proj=latlong +datum=WGS84");
        assertNotSame(pj, assertSerializedEquals(pj));
    }
}
