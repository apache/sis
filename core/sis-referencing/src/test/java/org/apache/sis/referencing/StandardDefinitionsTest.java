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

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.util.Constants;

// Test dependencies
import org.apache.sis.referencing.datum.GeodeticDatumMock;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.opengis.test.Validators;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.cs.HardCodedAxes;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link StandardDefinitions} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.crs.DefaultGeographicCRSTest.class,
    org.apache.sis.internal.referencing.provider.TransverseMercatorTest.class
})
public final strictfp class StandardDefinitionsTest extends TestCase {
    /**
     * Tests {@link StandardDefinitions#createUTM(int, GeographicCRS, double, boolean, CartesianCS)}.
     *
     * @since 0.7
     */
    @Test
    @DependsOnMethod("testCreateGeographicCRS")
    public void testCreateUTM() {
        final ProjectedCRS crs = StandardDefinitions.createUTM(32610, HardCodedCRS.WGS84, 15, -122, HardCodedCS.PROJECTED);
        assertEquals("name", "WGS 84 / UTM zone 10N", crs.getName().getCode());
        final ParameterValueGroup pg = crs.getConversionFromBase().getParameterValues();
        assertEquals(Constants.LATITUDE_OF_ORIGIN, -123, pg.parameter(Constants.CENTRAL_MERIDIAN).doubleValue(), STRICT);
        assertEquals(Constants.FALSE_NORTHING,        0, pg.parameter(Constants.FALSE_NORTHING).doubleValue(),   STRICT);
    }

    /**
     * Compares the values created by {@code StandardDefinitions} against hard-coded constants.
     * This method tests the following methods:
     *
     * <ul>
     *   <li>{@link StandardDefinitions#createEllipsoid(short)}</li>
     *   <li>{@link StandardDefinitions#createGeodeticDatum(short, Ellipsoid, PrimeMeridian)}</li>
     *   <li>{@link StandardDefinitions#createGeographicCRS(short, GeodeticDatum, EllipsoidalCS)}</li>
     * </ul>
     *
     * The geodetic objects are compared against the {@link HardCodedCRS}, {@link HardCodedDatum} and
     * {@link GeodeticDatumMock} constants. Actually this is more a test of the above-cited constants
     * than a {@code StandardDefinitions} one - in case of test failure, any of those classes could be
     * at fault.
     */
    @Test
    @DependsOnMethod("testCreateAxis")
    public void testCreateGeographicCRS() {
        final PrimeMeridian pm = StandardDefinitions.primeMeridian();
        final EllipsoidalCS cs = (EllipsoidalCS) StandardDefinitions.createCoordinateSystem((short) 6422);
        for (final CommonCRS e : CommonCRS.values()) {
            final Ellipsoid ellipsoid = StandardDefinitions.createEllipsoid(e.ellipsoid);
            switch (e) {
                case WGS84:  compare(GeodeticDatumMock.WGS84 .getEllipsoid(), ellipsoid); break;
                case WGS72:  compare(GeodeticDatumMock.WGS72 .getEllipsoid(), ellipsoid); break;
                case NAD83:  compare(GeodeticDatumMock.NAD83 .getEllipsoid(), ellipsoid); break;
                case NAD27:  compare(GeodeticDatumMock.NAD27 .getEllipsoid(), ellipsoid); break;
                case SPHERE: compare(GeodeticDatumMock.SPHERE.getEllipsoid(), ellipsoid); break;
            }
            final GeodeticDatum datum = StandardDefinitions.createGeodeticDatum(e.datum, ellipsoid, pm);
            switch (e) {
                case WGS84:  compare(HardCodedDatum.WGS84,  datum); break;
                case WGS72:  compare(HardCodedDatum.WGS72,  datum); break;
                case SPHERE: compare(HardCodedDatum.SPHERE, datum); break;
            }
            final GeographicCRS crs = StandardDefinitions.createGeographicCRS(e.geographic, datum, cs);
            Validators.validate(crs);
            switch (e) {
                case WGS84:  compare(HardCodedCRS.WGS84, crs); break;
            }
            Validators.validate(crs);
        }
    }

    /**
     * Compares only the properties which are known to be defined in {@link StandardDefinitions}.
     */
    private static void compare(final GeographicCRS expected, final GeographicCRS actual) {
        assertEquals("name", expected.getName().getCode(), actual.getName().getCode());
    }

    /**
     * Compares only the properties which are known to be defined in {@link StandardDefinitions}.
     */
    private static void compare(final GeodeticDatum expected, final GeodeticDatum actual) {
        assertEquals("name", expected.getName().getCode(), actual.getName().getCode());
    }

    /**
     * Compares only the properties which are known to be defined in {@link StandardDefinitions}.
     */
    private static void compare(final Ellipsoid expected, final Ellipsoid actual) {
        assertEquals("semiMajorAxis",     expected.getSemiMajorAxis(),     actual.getSemiMajorAxis(), STRICT);
        assertEquals("semiMinorAxis",     expected.getSemiMinorAxis(),     actual.getSemiMinorAxis(), STRICT);
        assertEquals("inverseFlattening", expected.getInverseFlattening(), actual.getInverseFlattening(), expected.isIvfDefinitive() ? STRICT : 1E-11);
        assertEquals("isIvfDefinitive",   expected.isIvfDefinitive(),      actual.isIvfDefinitive());
        assertEquals("isSphere",          expected.isSphere(),             actual.isSphere());
    }

    /**
     * Compares the values created by {@link StandardDefinitions#createAxis(short)} against the {@link HardCodedAxes}
     * constants. Actually this is more a {@code HardCodedAxes} test than a {@code StandardDefinitions} one - in case
     * of test failure, both classes could be at fault.
     */
    @Test
    public void testCreateAxis() {
        for (final short code : new short[] {1, 2, 60, 61, 62, 106, 107, 110, 114, 113}) {
            final CoordinateSystemAxis actual = StandardDefinitions.createAxis(code);
            Validators.validate(actual);
            switch (code) {
                case   1: compare(HardCodedAxes.EASTING,                actual); break;
                case   2: compare(HardCodedAxes.NORTHING,               actual); break;
                case  60: compare(HardCodedAxes.SPHERICAL_LATITUDE,     actual); break;
                case  61: compare(HardCodedAxes.SPHERICAL_LONGITUDE,    actual); break;
                case  62: compare(HardCodedAxes.GEOCENTRIC_RADIUS,      actual); break;
                case 106: compare(HardCodedAxes.GEODETIC_LATITUDE,      actual); break;
                case 107: compare(HardCodedAxes.GEODETIC_LONGITUDE,     actual); break;
                case 110: compare(HardCodedAxes.ELLIPSOIDAL_HEIGHT,     actual); break;
                case 114: compare(HardCodedAxes.GRAVITY_RELATED_HEIGHT, actual); break;
                case 113: compare(HardCodedAxes.DEPTH,                  actual); break;
                default:  throw new AssertionError(code);
            }
        }
    }

    /**
     * Compares only the properties which are known to be defined in {@link StandardDefinitions}.
     */
    private static void compare(final CoordinateSystemAxis expected, final CoordinateSystemAxis actual) {
        assertEquals("name",         expected.getName().getCode(), actual.getName().getCode());
        assertEquals("abbreviation", expected.getAbbreviation(),   actual.getAbbreviation());
        assertEquals("unit",         expected.getUnit(),           actual.getUnit());
        assertEquals("direction",    expected.getDirection(),      actual.getDirection());
        assertEquals("minimumValue", expected.getMinimumValue(),   actual.getMinimumValue(), STRICT);
        assertEquals("maximumValue", expected.getMaximumValue(),   actual.getMaximumValue(), STRICT);
        assertEquals("rangeMeaning", expected.getRangeMeaning(),   actual.getRangeMeaning());
    }

    /**
     * Tests the creation of vertical CRS.
     *
     * @since 0.7
     */
    @Test
    @DependsOnMethod("testCreateAxis")
    public void testCreateVerticalCRS() {
        VerticalDatum datum;
        VerticalCRS crs;

        datum = StandardDefinitions.createVerticalDatum(CommonCRS.Vertical.NAVD88.datum);
        crs = StandardDefinitions.createVerticalCRS(CommonCRS.Vertical.NAVD88.crs, datum);
        assertEquals("name", "NAVD88 height", crs.getName().getCode());
        assertEquals("identifier", "5703", IdentifiedObjects.getIdentifier(crs, Citations.EPSG).getCode());
        assertEquals("identifier",   "88", IdentifiedObjects.getIdentifier(crs, Citations.WMS ).getCode());
        assertEquals("direction", AxisDirection.UP, crs.getCoordinateSystem().getAxis(0).getDirection());

        datum = StandardDefinitions.createVerticalDatum(CommonCRS.Vertical.MEAN_SEA_LEVEL.datum);
        crs = StandardDefinitions.createVerticalCRS(CommonCRS.Vertical.MEAN_SEA_LEVEL.crs, datum);
        assertEquals("name", "MSL height", crs.getName().getCode());
        assertEquals("identifier", "5714", IdentifiedObjects.getIdentifier(crs, Citations.EPSG).getCode());
        assertNull  ("identifier", IdentifiedObjects.getIdentifier(crs, Citations.OGC));
        assertEquals("direction", AxisDirection.UP, crs.getCoordinateSystem().getAxis(0).getDirection());

        datum = StandardDefinitions.createVerticalDatum(CommonCRS.Vertical.DEPTH.datum);
        crs = StandardDefinitions.createVerticalCRS(CommonCRS.Vertical.DEPTH.crs, datum);
        assertEquals("name", "MSL depth", crs.getName().getCode());
        assertEquals("identifier", "5715", IdentifiedObjects.getIdentifier(crs, Citations.EPSG).getCode());
        assertNull  ("identifier", IdentifiedObjects.getIdentifier(crs, Citations.OGC));
        assertEquals("direction", AxisDirection.DOWN, crs.getCoordinateSystem().getAxis(0).getDirection());
    }
}
