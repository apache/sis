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

import java.util.Arrays;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the {@link Proj4Factory} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@DependsOn({PJTest.class, Proj4ParserTest.class})
public final strictfp class Proj4FactoryTest extends TestCase {
    /**
     * Verifies if the {@literal Proj.4} library is available.
     */
    @BeforeClass
    public static void verifyNativeLibraryAvailability() {
        PJTest.verifyNativeLibraryAvailability();
    }

    /**
     * Tests {@link Proj4Factory#getAuthorityCodes(Class)}.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testGetAuthorityCodes() throws FactoryException {
        final Proj4Factory factory = Proj4Factory.INSTANCE;
        assertTrue(factory.getAuthorityCodes(GeographicCRS.class).containsAll(Arrays.asList("+init=", "+proj=latlon")));
        assertTrue(factory.getAuthorityCodes(CoordinateReferenceSystem.class).containsAll(Arrays.asList("+init=", "+proj=")));
    }

    /**
     * Tests the creation of the {@code "+init=epsg:4326"} geographic CRS.
     * Note that the axis order is not the same than standard EPSG:4326.
     *
     * @throws FactoryException if an error occurred while creating the CRS objects.
     */
    @Test
    public void test4326() throws FactoryException {
        final Proj4Factory factory = Proj4Factory.INSTANCE;
        final GeographicCRS crs = factory.createGeographicCRS("+init=epsg:4326");
        /*
         * Use Proj.4 specific API to check axis order.
         */
        final PJ pj = (PJ) TestUtilities.getSingleton(crs.getIdentifiers());
        assertEquals(PJ.Type.GEOGRAPHIC, pj.getType());
        assertArrayEquals(new char[] {'e', 'n', 'u'}, pj.getAxisDirections());
    }

    /**
     * Tests the creation of the {@code "+init=epsg:3395"} projected CRS.
     *
     * @throws FactoryException if an error occurred while creating the CRS objects.
     */
    @Test
    public void test3395() throws FactoryException {
        final Proj4Factory factory = Proj4Factory.INSTANCE;
        final ProjectedCRS crs = factory.createProjectedCRS("+init=epsg:3395");
        /*
         * Use Proj.4 specific API to check axis order.
         */
        final PJ pj = (PJ) TestUtilities.getSingleton(crs.getIdentifiers());
        assertEquals(PJ.Type.PROJECTED, pj.getType());
        assertArrayEquals(new char[] {'e', 'n', 'u'}, pj.getAxisDirections());
    }

    /**
     * Tests the transformation from {@code "+init=epsg:4326"} to {@code "+init=epsg:3395"}.
     *
     * @throws FactoryException if an error occurred while creating the CRS objects.
     * @throws TransformException if an error occurred while projecting a test point.
     */
    @Test
    public void testTransform() throws FactoryException, TransformException {
        final Proj4Factory  factory   = Proj4Factory.INSTANCE;
        final GeographicCRS sourceCRS = factory.createGeographicCRS("+init=epsg:4326");
        final ProjectedCRS  targetCRS = factory.createProjectedCRS("+init=epsg:3395");
        final CoordinateOperation op  = factory.createOperation(sourceCRS, targetCRS);
        assertInstanceOf("createOperation", Conversion.class, op);

        final MathTransform mt = op.getMathTransform();
        DirectPosition pt = new DirectPosition2D(20, 40);
        pt = mt.transform(pt, pt);
        assertEquals("Easting",  2226389.816, pt.getOrdinate(0), 0.01);
        assertEquals("Northing", 4838471.398, pt.getOrdinate(1), 0.01);
    }
}
