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

import org.apache.sis.geometry.GeneralEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests {@link WraparoundAdjustment}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class WraparoundAdjustmentTest extends TestCase {
    /**
     * Tests {@link WraparoundAdjustment#range(CoordinateSystem, int)}.
     */
    @Test
    public void testRange() {
        assertTrue  (Double.isNaN(WraparoundAdjustment.range(HardCodedCS.GEODETIC_φλ, 0)));
        assertEquals(360, WraparoundAdjustment.range(HardCodedCS.GEODETIC_φλ, 1), STRICT);
        assertEquals(400, WraparoundAdjustment.range(HardCodedCS.ELLIPSOIDAL_gon, 0), STRICT);
    }

    /**
     * Convenience method for the tests.
     */
    private static Envelope adjustWraparoundAxes(Envelope areaOfInterest, Envelope domainOfValidity, CoordinateOperation validToAOI)
            throws TransformException
    {
        WraparoundAdjustment adj = new WraparoundAdjustment(areaOfInterest);
        adj.shiftInto(domainOfValidity, validToAOI);
        return adj.result(MathTransforms.identity(2));
    }

    /**
     * Tests {@link WraparoundAdjustment#shiftInto(Envelope, CoordinateOperation)}
     * with an envelope crossing the anti-meridian.
     *
     * @throws TransformException should never happen since this test does not transform coordinates.
     */
    @Test
    public void testOverAntiMeridian() throws TransformException {
        final GeneralEnvelope domainOfValidity = new GeneralEnvelope(HardCodedCRS.WGS84);
        domainOfValidity.setRange(0,  80, 280);
        domainOfValidity.setRange(1, -90, +90);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0, 140, -179);                 // Cross anti-meridian.
        areaOfInterest.setRange(1, -90,   90);

        final GeneralEnvelope expected = new GeneralEnvelope(HardCodedCRS.WGS84);
        expected.setRange(0, 140, 181);
        expected.setRange(1, -90, +90);

        final Envelope actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);
    }

    /**
     * Tests {@link WraparoundAdjustment#shiftInto(Envelope, CoordinateOperation)}
     * with an envelope shifted by 360° before or after the grid valid area.
     *
     * @throws TransformException should never happen since this test does not transform coordinates.
     */
    @Test
    @DependsOnMethod("testRange")
    public void testWithShiftedAOI() throws TransformException {
        final GeneralEnvelope domainOfValidity = new GeneralEnvelope(HardCodedCRS.WGS84);
        domainOfValidity.setRange(0,  80, 100);
        domainOfValidity.setRange(1, -70, +70);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0,  70, 90);
        areaOfInterest.setRange(1, -80, 60);

        final GeneralEnvelope expected = new GeneralEnvelope(areaOfInterest);

        Envelope actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);

        areaOfInterest.setRange(0, -290, -270);                    // [70 … 90] - 360
        actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);

        areaOfInterest.setRange(0, 430, 450);                      // [70 … 90] + 360
        actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);
    }

    /**
     * Tests {@link WraparoundAdjustment#shiftInto(Envelope, CoordinateOperation)}
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
     */
    @Test
    public void testAxesCausingExpansion() throws TransformException {
        final GeneralEnvelope domainOfValidity = new GeneralEnvelope(HardCodedCRS.WGS84);
        domainOfValidity.setRange(0,   5, 345);
        domainOfValidity.setRange(1, -70, +70);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0, -30,  40);
        areaOfInterest.setRange(1, -60,  60);

        final GeneralEnvelope expected = new GeneralEnvelope(HardCodedCRS.WGS84);
        expected.setRange(0, -30, 400);
        expected.setRange(1, -60,  60);

        final Envelope actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);
    }
}
