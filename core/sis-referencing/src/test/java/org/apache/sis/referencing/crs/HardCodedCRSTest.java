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
package org.apache.sis.referencing.crs;

import org.opengis.test.ValidatorContainer;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.referencing.crs.HardCodedCRS.*;


/**
 * Validates the {@link HardCodedCRS} definitions.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.cs.HardCodedCSTest.class,
    DefaultGeographicCRSTest.class
})
public final strictfp class HardCodedCRSTest extends TestCase {
    /**
     * Validates constants.
     *
     * <p>Note: ISO specification does not allow ellipsoidal height, so we have to relax
     * the check for the {@code DefaultVerticalCRS.ELLIPSOIDAL_HEIGHT} constant.</p>
     */
    @Test
    public void validate() {
        final ValidatorContainer validators = new ValidatorContainer();
        validators.validate(WGS84);
        validators.validate(WGS84_3D);           validators.crs.enforceStandardNames = false;
        validators.validate(ELLIPSOIDAL_HEIGHT); validators.crs.enforceStandardNames = true;
        validators.validate(GRAVITY_RELATED_HEIGHT);
        validators.validate(TIME);
        validators.validate(SPHERICAL);
        validators.validate(GEOCENTRIC);
        validators.validate(CARTESIAN_2D);
        validators.validate(CARTESIAN_3D);
    }

    /**
     * Tests dimension of constants.
     */
    @Test
    public void testDimensions() {
        assertEquals("WGS84 2D", 2, WGS84   .getCoordinateSystem().getDimension());
        assertEquals("WGS84 3D", 3, WGS84_3D.getCoordinateSystem().getDimension());
    }

    /**
     * Tests WKT formatting.
     */
    @Test
    public void testWKT() {
        assertWktEquals(Convention.WKT1,
                "GEOGCS[“WGS 84”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "  PRIMEM[“Greenwich”, 0.0],\n" +
                "  UNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Longitude”, EAST],\n" +
                "  AXIS[“Latitude”, NORTH]]",
                WGS84);

        assertWktEquals(Convention.WKT2,
                "GEOGCS[“WGS 84”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "  PRIMEM[“Greenwich”, 0.0],\n" +
                "  ANGLEUNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Longitude (λ)”, EAST],\n" +
                "  AXIS[“Latitude (φ)”, NORTH],\n" +
                "  AREA[“World”],\n" +
                "  BBOX[-90.00, -180.00, 90.00, 180.00]]",
                WGS84);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "GEOGCS[“WGS 84”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "  PRIMEM[“Greenwich”, 0.0],\n" +
                "  UNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Longitude (λ)”, EAST],\n" +
                "  AXIS[“Latitude (φ)”, NORTH],\n" +
                "  AREA[“World”],\n" +
                "  BBOX[-90.00, -180.00, 90.00, 180.00]]",
                WGS84);

        assertWktEquals(Convention.INTERNAL,
                "GEOGCS[“WGS 84”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    SPHEROID[“WGS84”, 6378137.0, 298.257223563],\n" +
                "    ID[“EPSG”, 6326]],\n" +
                "  PRIMEM[“Greenwich”, 0.0, ID[“EPSG”, 8901]],\n" +
                "  UNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Geodetic longitude (λ)”, EAST],\n" +
                "  AXIS[“Geodetic latitude (φ)”, NORTH],\n" +
                "  AREA[“World”],\n" +
                "  BBOX[-90.00, -180.00, 90.00, 180.00]]",
                WGS84);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(WGS84);
        assertSerializedEquals(WGS84_3D);
    }
}
