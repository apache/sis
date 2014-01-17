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
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.test.mock.GeodeticDatumMock;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.opengis.test.Validators;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.cs.HardCodedAxes;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link StandardDefinitions} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.crs.DefaultGeographicCRSTest.class
})
public final strictfp class StandardDefinitionsTest extends TestCase {
    /**
     * The tolerance threshold for strict comparisons of floating point values.
     */
    private static final double STRICT = 0;

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
    public void testCreateGographicCRS() {
        final PrimeMeridian pm = StandardDefinitions.primeMeridian();
        final EllipsoidalCS cs = (EllipsoidalCS) StandardDefinitions.createCoordinateSystem((short) 6422);
        for (final GeodeticObjects e : GeodeticObjects.values()) {
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
        for (final short code : new short[] {106, 107, 110, 114, 113}) {
            final CoordinateSystemAxis actual = StandardDefinitions.createAxis(code);
            Validators.validate(actual);
            switch (code) {
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
}
