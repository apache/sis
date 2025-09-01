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
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.referencing.datum.GeodeticDatumMock;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.cs.HardCodedAxes;
import org.apache.sis.referencing.cs.HardCodedCS;


/**
 * Tests the {@link StandardDefinitions} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class StandardDefinitionsTest extends EPSGDependentTestCase {
    /**
     * Creates a new test case.
     */
    public StandardDefinitionsTest() {
    }

    /**
     * Verifies value of the {@link StandardDefinitions#GREENWICH} code.
     * This method is for ensuring consistency between hard-coded constants.
     */
    @Test
    public void verifyGreenwichCode() {
        assertEquals(String.valueOf(Constants.EPSG_GREENWICH), StandardDefinitions.GREENWICH);
    }

    /**
     * Tests {@link StandardDefinitions#createCoordinateSystem(short, boolean)}.
     */
    @Test
    public void testCreateCoordinateSystem() {
        CoordinateSystem cs = StandardDefinitions.createCoordinateSystem((short) 4400, true);
        assertInstanceOf(CartesianCS.class, cs);
        assertEquals(2, cs.getDimension());
        assertEquals(Units.METRE,         cs.getAxis(0).getUnit());
        assertEquals(Units.METRE,         cs.getAxis(1).getUnit());
        assertEquals(AxisDirection.EAST,  cs.getAxis(0).getDirection());
        assertEquals(AxisDirection.NORTH, cs.getAxis(1).getDirection());
    }

    /**
     * Tests {@link StandardDefinitions#createMercator(int, GeographicCRS, boolean)} for World Mercator.
     */
    @Test
    public void testCreateWorldMercator() {
        final ProjectedCRS crs = StandardDefinitions.createMercator(3395, HardCodedCRS.WGS84, false);
        assertEquals("WGS 84 / World Mercator", crs.getName().getCode());
        final ParameterValueGroup pg = crs.getConversionFromBase().getParameterValues();
        assertEquals(0, pg.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue(), Constants.LATITUDE_OF_ORIGIN);
        assertEquals(0, pg.parameter(Constants.CENTRAL_MERIDIAN)  .doubleValue(), Constants.CENTRAL_MERIDIAN);
        assertEquals(1, pg.parameter(Constants.SCALE_FACTOR)      .doubleValue(), Constants.SCALE_FACTOR);
        assertEquals(0, pg.parameter(Constants.FALSE_EASTING)     .doubleValue(), Constants.FALSE_EASTING);
        assertEquals(0, pg.parameter(Constants.FALSE_NORTHING)    .doubleValue(), Constants.FALSE_NORTHING);
    }

    /**
     * Tests {@link StandardDefinitions#createMercator(int, GeographicCRS, boolean)} for pseudo-Mercator.
     */
    @Test
    public void testCreatePseudoMercator() {
        final ProjectedCRS crs = StandardDefinitions.createMercator(3857, HardCodedCRS.WGS84, true);
        assertEquals("WGS 84 / Pseudo-Mercator", crs.getName().getCode());
        final ParameterValueGroup pg = crs.getConversionFromBase().getParameterValues();
        assertEquals(0, pg.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue(), Constants.LATITUDE_OF_ORIGIN);
        assertEquals(0, pg.parameter(Constants.CENTRAL_MERIDIAN)  .doubleValue(), Constants.CENTRAL_MERIDIAN);
        assertEquals(0, pg.parameter(Constants.FALSE_EASTING)     .doubleValue(), Constants.FALSE_EASTING);
        assertEquals(0, pg.parameter(Constants.FALSE_NORTHING)    .doubleValue(), Constants.FALSE_NORTHING);
    }

    /**
     * Tests {@link StandardDefinitions#createUniversal(int, GeographicCRS, boolean, double, double, CartesianCS)}
     * for a Universal Transverse Mercator (UTM) projection.
     */
    @Test
    public void testCreateUTM() {
        final ProjectedCRS crs = StandardDefinitions.createUniversal(32610, HardCodedCRS.WGS84, true, 15, -122, HardCodedCS.PROJECTED);
        assertEquals("WGS 84 / UTM zone 10N", crs.getName().getCode());
        final ParameterValueGroup pg = crs.getConversionFromBase().getParameterValues();
        assertEquals(     0, pg.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue(), Constants.LATITUDE_OF_ORIGIN);
        assertEquals(  -123, pg.parameter(Constants.CENTRAL_MERIDIAN)  .doubleValue(), Constants.CENTRAL_MERIDIAN);
        assertEquals(0.9996, pg.parameter(Constants.SCALE_FACTOR)      .doubleValue(), Constants.SCALE_FACTOR);
        assertEquals(500000, pg.parameter(Constants.FALSE_EASTING)     .doubleValue(), Constants.FALSE_EASTING);
        assertEquals(     0, pg.parameter(Constants.FALSE_NORTHING)    .doubleValue(), Constants.FALSE_NORTHING);
    }

    /**
     * Tests {@link StandardDefinitions#createUniversal(int, GeographicCRS, boolean, double, double, CartesianCS)}
     * for a Universal Polar Stereographic (UPS) projection. This test cheats a little bit on the coordinate system
     * by laziness; we are more interested in the projection parameters.
     */
    @Test
    public void testCreateUPS() {
        final ProjectedCRS crs = StandardDefinitions.createUniversal(5041, HardCodedCRS.WGS84, false, 90, -122, HardCodedCS.PROJECTED);
        assertEquals("WGS 84 / Universal Polar Stereographic North", crs.getName().getCode());
        final ParameterValueGroup pg = crs.getConversionFromBase().getParameterValues();
        assertEquals(     90, pg.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue(), Constants.LATITUDE_OF_ORIGIN);
        assertEquals(      0, pg.parameter(Constants.CENTRAL_MERIDIAN)  .doubleValue(), Constants.CENTRAL_MERIDIAN);
        assertEquals(  0.994, pg.parameter(Constants.SCALE_FACTOR)      .doubleValue(), Constants.SCALE_FACTOR);
        assertEquals(2000000, pg.parameter(Constants.FALSE_EASTING)     .doubleValue(), Constants.FALSE_EASTING);
        assertEquals(2000000, pg.parameter(Constants.FALSE_NORTHING)    .doubleValue(), Constants.FALSE_NORTHING);
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
    public void testCreateGeographicCRS() {
        final PrimeMeridian pm = StandardDefinitions.primeMeridian();
        final EllipsoidalCS cs = (EllipsoidalCS) StandardDefinitions.createCoordinateSystem((short) 6422, true);
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
            final GeographicCRS crs = StandardDefinitions.createGeographicCRS(e.geographic, datum, null, cs);
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
        assertEquals(expected.getName().getCode(), actual.getName().getCode(), "name");
    }

    /**
     * Compares only the properties which are known to be defined in {@link StandardDefinitions}.
     */
    private static void compare(final GeodeticDatum expected, final GeodeticDatum actual) {
        assertEquals(expected.getName().getCode(), actual.getName().getCode(), "name");
    }

    /**
     * Compares only the properties which are known to be defined in {@link StandardDefinitions}.
     */
    private static void compare(final Ellipsoid expected, final Ellipsoid actual) {
        assertEquals(expected.getSemiMajorAxis(),     actual.getSemiMajorAxis());
        assertEquals(expected.getSemiMinorAxis(),     actual.getSemiMinorAxis());
        assertEquals(expected.getInverseFlattening(), actual.getInverseFlattening(), expected.isIvfDefinitive() ? STRICT : 1E-11);
        assertEquals(expected.isIvfDefinitive(),      actual.isIvfDefinitive());
        assertEquals(expected.isSphere(),             actual.isSphere());
    }

    /**
     * Compares the values created by {@link StandardDefinitions#createAxis(short, boolean)} against the {@link HardCodedAxes}
     * constants. Actually this is more a {@code HardCodedAxes} test than a {@code StandardDefinitions} one - in case of test
     * failure, both classes could be at fault.
     */
    @Test
    public void testCreateAxis() {
        for (final short code : new short[] {1, 2, 60, 61, 62, 106, 107, 110, 114, 113}) {
            final CoordinateSystemAxis actual = StandardDefinitions.createAxis(code, true);
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
        assertEquals(expected.getName().getCode(), actual.getName().getCode());
        assertEquals(expected.getAbbreviation(),   actual.getAbbreviation());
        assertEquals(expected.getUnit(),           actual.getUnit());
        assertEquals(expected.getDirection(),      actual.getDirection());
        assertEquals(expected.getMinimumValue(),   actual.getMinimumValue());
        assertEquals(expected.getMaximumValue(),   actual.getMaximumValue());
        assertEquals(expected.getRangeMeaning(),   actual.getRangeMeaning());
    }

    /**
     * Tests the creation of vertical CRS.
     */
    @Test
    public void testCreateVerticalCRS() {
        VerticalDatum datum;
        VerticalCRS crs;

        datum = StandardDefinitions.createVerticalDatum(CommonCRS.Vertical.NAVD88.datum);
        crs = StandardDefinitions.createVerticalCRS(CommonCRS.Vertical.NAVD88.crs, datum);
        assertEquals("NAVD88 height", crs.getName().getCode());
        assertEquals("5703", IdentifiedObjects.getIdentifier(crs, Citations.EPSG).getCode());
        assertEquals(  "88", IdentifiedObjects.getIdentifier(crs, Citations.WMS ).getCode());
        assertEquals(AxisDirection.UP, crs.getCoordinateSystem().getAxis(0).getDirection());

        datum = StandardDefinitions.createVerticalDatum(CommonCRS.Vertical.MEAN_SEA_LEVEL.datum);
        crs = StandardDefinitions.createVerticalCRS(CommonCRS.Vertical.MEAN_SEA_LEVEL.crs, datum);
        assertEquals("MSL height", crs.getName().getCode());
        assertEquals("5714", IdentifiedObjects.getIdentifier(crs, Citations.EPSG).getCode());
        assertNull  (IdentifiedObjects.getIdentifier(crs, Citations.OGC));
        assertEquals(AxisDirection.UP, crs.getCoordinateSystem().getAxis(0).getDirection());

        datum = StandardDefinitions.createVerticalDatum(CommonCRS.Vertical.DEPTH.datum);
        crs = StandardDefinitions.createVerticalCRS(CommonCRS.Vertical.DEPTH.crs, datum);
        assertEquals("MSL depth", crs.getName().getCode());
        assertEquals("5715", IdentifiedObjects.getIdentifier(crs, Citations.EPSG).getCode());
        assertNull  (IdentifiedObjects.getIdentifier(crs, Citations.OGC));
        assertEquals(AxisDirection.DOWN, crs.getCoordinateSystem().getAxis(0).getDirection());
    }
}
