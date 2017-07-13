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
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link EPSGFactory} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@DependsOn(PJTest.class)
public final strictfp class EPSGFactoryTest extends TestCase {
    /**
     * Verifies if the Proj4 library is available.
     */
    @BeforeClass
    public static void verifyNativeLibraryAvailability() {
        PJTest.verifyNativeLibraryAvailability();
    }

    /**
     * Tests the creation of the EPSG:4326 geographic CRS. The interesting part of this test
     * is the check for axis order. The result will depend on whether the axis orientations
     * map has been properly created or not.
     *
     * @throws FactoryException if an error occurred while creating the CRS objects.
     */
    @Test
    public void testEPSG_4326() throws FactoryException {
        final EPSGFactory factory = new EPSGFactory(true);
        final GeographicCRS crs = factory.createGeographicCRS("EPSG:4326");
        // Use Proj.4 specific API to check axis order.
        final PJ pj = (PJ) crs.getDatum();
        assertArrayEquals(new char[] {'n', 'e', 'u'}, pj.getAxisDirections());
    }
}
