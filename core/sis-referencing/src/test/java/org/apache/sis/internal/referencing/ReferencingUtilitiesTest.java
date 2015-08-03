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
package org.apache.sis.internal.referencing;

import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.util.Utilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.internal.referencing.ReferencingUtilities.*;


/**
 * Tests {@link ReferencingUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from 0.4)
 * @version 0.5
 * @module
 */
public final strictfp class ReferencingUtilitiesTest extends TestCase {
    /**
     * Tests {@link ReferencingUtilities#getGreenwichLongitude(PrimeMeridian, Unit)}.
     */
    @Test
    public void testGetGreenwichLongitude() {
        assertEquals(0,          getGreenwichLongitude(HardCodedDatum.GREENWICH, NonSI.DEGREE_ANGLE), STRICT);
        assertEquals(0,          getGreenwichLongitude(HardCodedDatum.GREENWICH, NonSI.GRADE),        STRICT);
        assertEquals(2.5969213,  getGreenwichLongitude(HardCodedDatum.PARIS,     NonSI.GRADE),        STRICT);
        assertEquals(2.33722917, getGreenwichLongitude(HardCodedDatum.PARIS,     NonSI.DEGREE_ANGLE), 1E-12);
        assertEquals(2.33720833, getGreenwichLongitude(HardCodedDatum.PARIS_RGS, NonSI.DEGREE_ANGLE), 1E-8);
        assertEquals(2.596898,   getGreenwichLongitude(HardCodedDatum.PARIS_RGS, NonSI.GRADE),        1E-6);
    }

    /**
     * Asserts that normalization of the given CRS produces {@link HardCodedCRS#WGS84} (ignoring metadata).
     *
     * @param message The message to show in case of failure.
     * @param createExpected {@code true} if we expect normalization to create a new CRS object.
     * @param crs The CRS for which to test normalization.
     */
    private static void assertNormalizedEqualsWGS84(final String message, final boolean createExpected,
            final CoordinateReferenceSystem crs)
    {
        final GeographicCRS normalizedCRS = toNormalizedGeographicCRS(crs);
        assertTrue(message, Utilities.equalsIgnoreMetadata(HardCodedCRS.WGS84, normalizedCRS));
        assertEquals("New CRS instance expected:", createExpected, normalizedCRS != HardCodedCRS.WGS84);
    }

    /**
     * Tests {@link ReferencingUtilities#toNormalizedGeographicCRS(CoordinateReferenceSystem)}.
     */
    @Test
    public void testToNormalizedGeographicCRS() {
        assertNormalizedEqualsWGS84("Expected identity operation.",    false, HardCodedCRS.WGS84);
        assertNormalizedEqualsWGS84("Shall extract the 2D component.", false, HardCodedCRS.GEOID_3D);
        assertNormalizedEqualsWGS84("Shall extract the 2D component.", false, HardCodedCRS.GEOID_4D);
        assertNormalizedEqualsWGS84("Shall build a the 2D component.", true,  HardCodedCRS.WGS84_3D);
        assertNormalizedEqualsWGS84("Shall normalize axis order.",     true,  HardCodedCRS.WGS84_φλ);
        assertNull(toNormalizedGeographicCRS(null));
    }

    /**
     * Tests {@link ReferencingUtilities#toWKTType(Class, Class)}.
     */
    @Test
    public void testType() {
        assertNull  (                         toWKTType(CoordinateSystem.class, CoordinateSystem.class));
        assertEquals(WKTKeywords.affine,      toWKTType(CoordinateSystem.class, AffineCS        .class));
        assertEquals(WKTKeywords.Cartesian,   toWKTType(CoordinateSystem.class, CartesianCS     .class));
        assertEquals(WKTKeywords.cylindrical, toWKTType(CoordinateSystem.class, CylindricalCS   .class));
        assertEquals(WKTKeywords.ellipsoidal, toWKTType(CoordinateSystem.class, EllipsoidalCS   .class));
        assertEquals(WKTKeywords.linear,      toWKTType(CoordinateSystem.class, LinearCS        .class));
//      assertEquals(WKTKeywords.parametric,  toWKTType(CoordinateSystem.class, ParametricCS    .class));
        assertEquals(WKTKeywords.polar,       toWKTType(CoordinateSystem.class, PolarCS         .class));
        assertEquals(WKTKeywords.spherical,   toWKTType(CoordinateSystem.class, SphericalCS     .class));
        assertEquals(WKTKeywords.temporal,    toWKTType(CoordinateSystem.class, TimeCS          .class));
        assertEquals(WKTKeywords.vertical,    toWKTType(CoordinateSystem.class, VerticalCS      .class));
    }
}
