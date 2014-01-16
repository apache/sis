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
package org.apache.sis.referencing;

import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.util.FactoryException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link CRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({
    GeodeticObjectsTest.class
})
public final strictfp class CRSTest extends TestCase {
    /**
     * Asserts that the result of {@link CRS#forCode(String)} is the given CRS.
     */
    private static void verifyForCode(final GeographicCRS expected, final String code) throws FactoryException {
        assertSame(code, expected, CRS.forCode(code));
    }

    /**
     * Tests {@link CRS#forCode(String)} with EPSG codes.
     *
     * @throws FactoryException If a CRS can not be constructed.
     */
    @Test
    public void testForEpsgCode() throws FactoryException {
        verifyForCode(GeodeticObjects.WGS84 .geographic(), "EPSG:4326");
        verifyForCode(GeodeticObjects.WGS84 .geographic(), "urn:ogc:def:crs:EPSG::4326");
        verifyForCode(GeodeticObjects.WGS84 .geographic(), "http://www.opengis.net/gml/srs/epsg.xml#4326");
        verifyForCode(GeodeticObjects.WGS72 .geographic(), "EPSG:4322");
        verifyForCode(GeodeticObjects.SPHERE.geographic(), "EPSG:4047");
        verifyForCode(GeodeticObjects.NAD83 .geographic(), "EPSG:4269");
        verifyForCode(GeodeticObjects.NAD27 .geographic(), "EPSG:4267");
        verifyForCode(GeodeticObjects.ETRS89.geographic(), "EPSG:4258");
        verifyForCode(GeodeticObjects.ED50  .geographic(), "EPSG:4230");
    }

    /**
     * Tests {@link CRS#forCode(String)} with CRS codes.
     *
     * @throws FactoryException If a CRS can not be constructed.
     */
    @Test
    @DependsOnMethod("testForEpsgCode")
    public void testForCrsCode() throws FactoryException {
        verifyForCode(GeodeticObjects.WGS84.normalizedGeographic(), "CRS:84");
        verifyForCode(GeodeticObjects.NAD83.normalizedGeographic(), "CRS:83");
        verifyForCode(GeodeticObjects.NAD27.normalizedGeographic(), "CRS:27");
    }
}
