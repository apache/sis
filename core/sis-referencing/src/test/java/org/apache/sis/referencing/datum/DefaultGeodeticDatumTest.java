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
package org.apache.sis.referencing.datum;

import java.util.Map;
import java.util.HashMap;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.opengis.test.Assert.*;
import static org.apache.sis.test.mock.GeodeticDatumMock.*;


/**
 * Tests the {@link DefaultGeodeticDatum} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({DefaultEllipsoidTest.class, BursaWolfParametersTest.class})
public final strictfp class DefaultGeodeticDatumTest extends TestCase {
    /**
     * Tests {@link DefaultGeodeticDatum#getPositionVectorTransformation(GeodeticDatum, Extent)}.
     */
    @Test
    public void testGetPositionVectorTransformation() {
        final Map<String,Object> properties = new HashMap<>();
        assertNull(properties.put(DefaultGeodeticDatum.NAME_KEY, "Invalid dummy datum"));
        /*
         * Associate two BursaWolfParameters, one valid only in a local area and the other one
         * valid globaly.  Note that we are building an invalid set of parameters, because the
         * source datum are not the same in both case. But for this test we are not interrested
         * in datum consistency - we only want any Bursa-Wolf parameters having different area
         * of validity.
         */
        final BursaWolfParameters local  = BursaWolfParametersTest.createED87_to_WGS84();   // Local area (North Sea)
        final BursaWolfParameters global = BursaWolfParametersTest.createWGS72_to_WGS84();  // Global area (World)
        assertNull(properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, new BursaWolfParameters[] {local, global}));
        /*
         * Build the datum using WGS 72 ellipsoid (so at least one of the BursaWolfParameters is real).
         */
        final DefaultGeodeticDatum datum = new DefaultGeodeticDatum(properties,
                WGS72.getEllipsoid(), WGS72.getPrimeMeridian());
        /*
         * Search for BursaWolfParameters around the North Sea area.
         */
        final DefaultGeographicBoundingBox areaOfInterest = new DefaultGeographicBoundingBox(-2, 8, 55, 60);
        final DefaultExtent extent = new DefaultExtent("Around the North Sea", areaOfInterest, null, null);
        Matrix matrix = datum.getPositionVectorTransformation(NAD83, extent);
        assertNull("No BursaWolfParameters for NAD83", matrix);
        matrix = datum.getPositionVectorTransformation(WGS84, extent);
        assertNotNull("BursaWolfParameters for WGS84", matrix);
        checkTransformationSignature(local, matrix);
        /*
         * Expand the area of interest to something greater than North Sea, and test again.
         */
        areaOfInterest.setWestBoundLongitude(-8);
        matrix = datum.getPositionVectorTransformation(WGS84, extent);
        assertNotNull("BursaWolfParameters for WGS84", matrix);
        checkTransformationSignature(global, matrix);
    }

    /**
     * Verifies if the given matrix is for the expected Position Vector transformation.
     * The easiest way to verify that is to check the translation terms (last matrix column),
     * which should have been copied verbatim from the {@code BursaWolfParameters} to the matrix.
     * Other terms in the matrix are modified compared to the {@code BursaWolfParameters} ones.
     */
    private static void checkTransformationSignature(final BursaWolfParameters expected, final Matrix actual) {
        assertEquals("tX", expected.tX, actual.getElement(0, 3), 0);
        assertEquals("tY", expected.tY, actual.getElement(1, 3), 0);
        assertEquals("tZ", expected.tZ, actual.getElement(2, 3), 0);
    }
}
