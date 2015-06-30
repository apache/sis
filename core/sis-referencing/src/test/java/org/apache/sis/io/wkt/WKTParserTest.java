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
package org.apache.sis.io.wkt;

import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.util.FactoryException;
import org.opengis.test.wkt.CRSParserTest;
import org.apache.sis.internal.metadata.AxisNames;
import org.apache.sis.test.DependsOn;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;


/**
 * Tests Well-Known Text parser using the tests defined in GeoAPI. Those tests use the
 * {@link org.apache.sis.referencing.factory.GeodeticObjectFactory#createFromWKT(String)} method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@RunWith(JUnit4.class)
@DependsOn(GeodeticObjectParserTest.class)
public class WKTParserTest extends CRSParserTest {
    /**
     * Creates a new test case using the default {@code CRSFactory} implementation.
     */
    public WKTParserTest() {
        super(org.apache.sis.internal.system.DefaultFactories.forClass(CRSFactory.class));
    }

    /**
     * Verifies the axis names of a geographic CRS. This method is invoked when the parsed object is
     * expected to have <cite>"Geodetic latitude"</cite> and <cite>"Geodetic longitude"</cite> names.
     */
    @SuppressWarnings("fallthrough")
    private void verifyEllipsoidalCS() {
        final CoordinateSystem cs = object.getCoordinateSystem();
        switch (cs.getDimension()) {
            default: assertEquals("name", AxisNames.ELLIPSOIDAL_HEIGHT, cs.getAxis(2).getName().getCode());
            case 2:  assertEquals("name", AxisNames.GEODETIC_LONGITUDE, cs.getAxis(1).getName().getCode());
            case 1:  assertEquals("name", AxisNames.GEODETIC_LATITUDE,  cs.getAxis(0).getName().getCode());
            case 0:  break;
        }
        switch (cs.getDimension()) {
            default: assertEquals("abbreviation", "h", cs.getAxis(2).getAbbreviation());
            case 2:  assertEquals("abbreviation", "λ", cs.getAxis(1).getAbbreviation());
            case 1:  assertEquals("abbreviation", "φ", cs.getAxis(0).getAbbreviation());
            case 0:  break;
        }
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographic() throws FactoryException {
        super.testGeographic();
        verifyEllipsoidalCS();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographicWithRemark() throws FactoryException {
        super.testGeographicWithRemark();
        verifyEllipsoidalCS();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographicWithId() throws FactoryException {
        super.testGeographicWithId();
        verifyEllipsoidalCS();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographicWithGradUnits() throws FactoryException {
        super.testGeographicWithGradUnits();
        verifyEllipsoidalCS();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeocentric() throws FactoryException {
        super.testGeocentric();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.GEOCENTRIC_X, cs.getAxis(0).getName().getCode());
        assertEquals(AxisNames.GEOCENTRIC_Y, cs.getAxis(1).getName().getCode());
        assertEquals(AxisNames.GEOCENTRIC_Z, cs.getAxis(2).getName().getCode());
    }

    /**
     * Ignored for now, because the Lambert Azimuthal Equal Area projection method is not yet implemented.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    @Ignore("Lambert Azimuthal Equal Area projection method not yet implemented.")
    public void testProjected() throws FactoryException {
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testProjectedWithFootUnits() throws FactoryException {
        super.testProjectedWithFootUnits();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.EASTING,  cs.getAxis(0).getName().getCode());
        assertEquals(AxisNames.NORTHING, cs.getAxis(1).getName().getCode());
    }

    /**
     * Ignored for now, because the Transverse Mercator projection method is not yet implemented.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    @Ignore("Transverse Mercator projection method not yet implemented.")
    public void testProjectedWithImplicitUnits() throws FactoryException {
    }
}
