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
package org.apache.sis.referencing.internal.shared;

import javax.measure.Unit;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.apache.sis.util.Utilities;
import org.apache.sis.measure.Units;
import static org.apache.sis.referencing.internal.shared.ReferencingUtilities.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.datum.HardCodedDatum;


/**
 * Tests {@link ReferencingUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ReferencingUtilitiesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ReferencingUtilitiesTest() {
    }

    /**
     * Tests {@link ReferencingUtilities#getGreenwichLongitude(PrimeMeridian, Unit)}.
     */
    @Test
    public void testGetGreenwichLongitude() {
        assertEquals(0,          getGreenwichLongitude(HardCodedDatum.GREENWICH, Units.DEGREE));
        assertEquals(0,          getGreenwichLongitude(HardCodedDatum.GREENWICH, Units.GRAD));
        assertEquals(2.5969213,  getGreenwichLongitude(HardCodedDatum.PARIS,     Units.GRAD));
        assertEquals(2.33722917, getGreenwichLongitude(HardCodedDatum.PARIS,     Units.DEGREE), 1E-12);
        assertEquals(2.33720833, getGreenwichLongitude(HardCodedDatum.PARIS_RGS, Units.DEGREE), 1E-8);
        assertEquals(2.596898,   getGreenwichLongitude(HardCodedDatum.PARIS_RGS, Units.GRAD),   1E-6);
    }

    /**
     * Tests {@link ReferencingUtilities#isEllipsoidalHeight(VerticalDatum)}.
     */
    @Test
    public void testEllipsoidalHeight() {
        assertTrue(isEllipsoidalHeight(HardCodedDatum.ELLIPSOID));
    }

    /**
     * Asserts that normalization of the given CRS produces {@link HardCodedCRS#WGS84} (ignoring metadata).
     *
     * @param  message         the message to show in case of failure.
     * @param  createExpected  {@code true} if we expect normalization to create a new CRS object.
     * @param  crs             the CRS for which to test normalization.
     */
    private static void assertNormalizedEqualsWGS84(final String message, final boolean createExpected,
            final CoordinateReferenceSystem crs)
    {
        final GeographicCRS normalizedCRS = toNormalizedGeographicCRS(crs, false, false);
        assertTrue(Utilities.equalsIgnoreMetadata(HardCodedCRS.WGS84, normalizedCRS), message);
        assertEquals(createExpected, normalizedCRS != HardCodedCRS.WGS84);
    }

    /**
     * Tests {@link ReferencingUtilities#toNormalizedGeographicCRS(CoordinateReferenceSystem, boolean, boolean)}.
     */
    @Test
    public void testToNormalizedGeographicCRS() {
        assertNormalizedEqualsWGS84("Expected identity operation.",    false, HardCodedCRS.WGS84);
        assertNormalizedEqualsWGS84("Shall extract the 2D component.", false, HardCodedCRS.GEOID_3D);
        assertNormalizedEqualsWGS84("Shall extract the 2D component.", false, HardCodedCRS.GEOID_4D);
        assertNormalizedEqualsWGS84("Shall build a the 2D component.", true,  HardCodedCRS.WGS84_3D);
        assertNormalizedEqualsWGS84("Shall normalize axis order.",     true,  HardCodedCRS.WGS84_LATITUDE_FIRST);
        assertNull(toNormalizedGeographicCRS(null, false, false));
    }

    /**
     * Tests {@link ReferencingUtilities#getPropertiesForModifiedCRS(IdentifiedObject)}.
     */
    @Test
    public void testGetPropertiesForModifiedCRS() {
        assertEquals("WGS 84",      getPropertiesForModifiedCRS(HardCodedCRS.WGS84_3D).get(IdentifiedObject.NAME_KEY));
        assertEquals("WGS 84",      getPropertiesForModifiedCRS(HardCodedCRS.GEOID_4D).get(IdentifiedObject.NAME_KEY));
        assertEquals("NTF (Paris)", getPropertiesForModifiedCRS(HardCodedCRS.NTF)     .get(IdentifiedObject.NAME_KEY));
    }

    /**
     * Tests {@link ReferencingUtilities#toPropertyName(Class, Class)}.
     *
     * @see WKTUtilitiesTest#testToType()
     */
    @Test
    public void testToPropertyName() {
        assertEquals("coordinateSystem", toPropertyName(CoordinateSystem.class, CoordinateSystem.class).toString());
        assertEquals("affineCS",         toPropertyName(CoordinateSystem.class, AffineCS        .class).toString());
        assertEquals("cartesianCS",      toPropertyName(CoordinateSystem.class, CartesianCS     .class).toString());
        assertEquals("cylindricalCS",    toPropertyName(CoordinateSystem.class, CylindricalCS   .class).toString());
        assertEquals("ellipsoidalCS",    toPropertyName(CoordinateSystem.class, EllipsoidalCS   .class).toString());
        assertEquals("linearCS",         toPropertyName(CoordinateSystem.class, LinearCS        .class).toString());
        assertEquals("parametricCS",     toPropertyName(CoordinateSystem.class, ParametricCS    .class).toString());
        assertEquals("polarCS",          toPropertyName(CoordinateSystem.class, PolarCS         .class).toString());
        assertEquals("sphericalCS",      toPropertyName(CoordinateSystem.class, SphericalCS     .class).toString());
        assertEquals("temporalCS",       toPropertyName(CoordinateSystem.class, TimeCS          .class).toString());
        assertEquals("verticalCS",       toPropertyName(CoordinateSystem.class, VerticalCS      .class).toString());
    }
}
