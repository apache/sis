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

import javax.measure.Unit;
import org.apache.sis.geometry.GeneralEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.util.Utilities;
import org.apache.sis.measure.Units;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;
import static org.apache.sis.internal.referencing.ReferencingUtilities.*;


/**
 * Tests {@link ReferencingUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.4
 * @module
 */
public final strictfp class ReferencingUtilitiesTest extends TestCase {
    /**
     * Tests {@link ReferencingUtilities#getGreenwichLongitude(PrimeMeridian, Unit)}.
     */
    @Test
    public void testGetGreenwichLongitude() {
        assertEquals(0,          getGreenwichLongitude(HardCodedDatum.GREENWICH, Units.DEGREE), STRICT);
        assertEquals(0,          getGreenwichLongitude(HardCodedDatum.GREENWICH, Units.GRAD),   STRICT);
        assertEquals(2.5969213,  getGreenwichLongitude(HardCodedDatum.PARIS,     Units.GRAD),   STRICT);
        assertEquals(2.33722917, getGreenwichLongitude(HardCodedDatum.PARIS,     Units.DEGREE), 1E-12);
        assertEquals(2.33720833, getGreenwichLongitude(HardCodedDatum.PARIS_RGS, Units.DEGREE), 1E-8);
        assertEquals(2.596898,   getGreenwichLongitude(HardCodedDatum.PARIS_RGS, Units.GRAD),   1E-6);
    }

    /**
     * Tests {@link ReferencingUtilities#getWraparoundRange(CoordinateSystem, int)}.
     */
    @Test
    public void testGetWraparoundRange() {
        assertTrue  (Double.isNaN(getWraparoundRange(HardCodedCS.GEODETIC_φλ, 0)));
        assertEquals(360, getWraparoundRange(HardCodedCS.GEODETIC_φλ, 1), STRICT);
        assertEquals(400, getWraparoundRange(HardCodedCS.ELLIPSOIDAL_gon, 0), STRICT);
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
     * Tests {@link ReferencingUtilities#getPropertiesForModifiedCRS(IdentifiedObject)}.
     *
     * @since 0.7
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
     *
     * @since 0.6
     */
    @Test
    public void testToPropertyName() {
        assertEquals("coordinateSystem", toPropertyName(CoordinateSystem.class, CoordinateSystem.class).toString());
        assertEquals("affineCS",         toPropertyName(CoordinateSystem.class, AffineCS        .class).toString());
        assertEquals("cartesianCS",      toPropertyName(CoordinateSystem.class, CartesianCS     .class).toString());
        assertEquals("cylindricalCS",    toPropertyName(CoordinateSystem.class, CylindricalCS   .class).toString());
        assertEquals("ellipsoidalCS",    toPropertyName(CoordinateSystem.class, EllipsoidalCS   .class).toString());
        assertEquals("linearCS",         toPropertyName(CoordinateSystem.class, LinearCS        .class).toString());
//      assertEquals("parametricCS",     toPropertyName(CoordinateSystem.class, ParametricCS    .class).toString());
        assertEquals("polarCS",          toPropertyName(CoordinateSystem.class, PolarCS         .class).toString());
        assertEquals("sphericalCS",      toPropertyName(CoordinateSystem.class, SphericalCS     .class).toString());
        assertEquals("timeCS",           toPropertyName(CoordinateSystem.class, TimeCS          .class).toString());
        assertEquals("verticalCS",       toPropertyName(CoordinateSystem.class, VerticalCS      .class).toString());
    }

    /**
     * Tests {@link ReferencingUtilities#adjustWraparoundAxes(Envelope, Envelope, CoordinateOperation)}
     * with an envelope crossing the anti-meridian.
     *
     * @throws TransformException should never happen since this test does not transform coordinates.
     *
     * @since 1.0
     */
    @Test
    public void testAdjustWraparoundAxesOverAntiMeridian() throws TransformException {
        final GeneralEnvelope domainOfValidity = new GeneralEnvelope(HardCodedCRS.WGS84);
        domainOfValidity.setRange(0,  80, 280);
        domainOfValidity.setRange(1, -90, +90);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0, 140, -179);                 // Cross anti-meridian.
        areaOfInterest.setRange(1, -90,   90);

        final GeneralEnvelope expected = new GeneralEnvelope(HardCodedCRS.WGS84);
        expected.setRange(0, 140, 181);
        expected.setRange(1, -90, +90);

        final Envelope actual = ReferencingUtilities.adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);
    }

    /**
     * Tests {@link ReferencingUtilities#adjustWraparoundAxes(Envelope, Envelope, CoordinateOperation)}
     * with an envelope shifted by 360° before or after the grid valid area.
     *
     * @throws TransformException should never happen since this test does not transform coordinates.
     *
     * @since 1.0
     */
    @Test
    public void testAdjustWraparoundAxesWithShiftedAOI() throws TransformException {
        final GeneralEnvelope domainOfValidity = new GeneralEnvelope(HardCodedCRS.WGS84);
        domainOfValidity.setRange(0,  80, 100);
        domainOfValidity.setRange(1, -70, +70);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0,  70, 90);
        areaOfInterest.setRange(1, -80, 60);

        final GeneralEnvelope expected = new GeneralEnvelope(areaOfInterest);

        Envelope actual = ReferencingUtilities.adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);

        areaOfInterest.setRange(0, -290, -270);                    // [70 … 90] - 360
        actual = ReferencingUtilities.adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);

        areaOfInterest.setRange(0, 430, 450);                      // [70 … 90] + 360
        actual = ReferencingUtilities.adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);
    }

    /**
     * Tests {@link ReferencingUtilities#adjustWraparoundAxes(Envelope, Envelope, CoordinateOperation)}
     * with an envelope that cause the method to expand the area of interest. Illustration:
     *
     * {@preformat text
     *                  ┌────────────────────────────────────────────┐
     *                  │             Domain of validity             │
     *                  └────────────────────────────────────────────┘
     *   ┌────────────────────┐                                ┌─────
     *   │  Area of interest  │                                │  AOI
     *   └────────────────────┘                                └─────
     *    ↖………………………………………………………360° period……………………………………………………↗︎
     * }
     *
     * @throws TransformException should never happen since this test does not transform coordinates.
     *
     * @since 1.0
     */
    @Test
    public void testAdjustWraparoundAxesCausingExpansion() throws TransformException {
        final GeneralEnvelope domainOfValidity = new GeneralEnvelope(HardCodedCRS.WGS84);
        domainOfValidity.setRange(0,   5, 345);
        domainOfValidity.setRange(1, -70, +70);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0, -30,  40);
        areaOfInterest.setRange(1, -60,  60);

        final GeneralEnvelope expected = new GeneralEnvelope(HardCodedCRS.WGS84);
        expected.setRange(0, -30, 400);
        expected.setRange(1, -60,  60);

        final Envelope actual = ReferencingUtilities.adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);
    }
}
