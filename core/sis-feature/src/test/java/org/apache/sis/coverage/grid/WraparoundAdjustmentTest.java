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
package org.apache.sis.coverage.grid;

import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests {@link WraparoundAdjustment}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.0
 * @module
 */
public final strictfp class WraparoundAdjustmentTest extends TestCase {
    /**
     * Convenience method for the tests.
     */
    private static Envelope adjustWraparoundAxes(Envelope areaOfInterest, Envelope domainOfValidity, MathTransform validToAOI)
            throws TransformException
    {
        WraparoundAdjustment adj = new WraparoundAdjustment(domainOfValidity, validToAOI, null);
        return adj.shift(areaOfInterest);
    }

    /**
     * Tests {@link WraparoundAdjustment#shift(Envelope)}
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
     * Tests {@link WraparoundAdjustment#shift(Envelope)}
     * with an envelope which is outside the grid valid area.
     *
     * @throws TransformException should never happen since this test does not transform coordinates.
     */
    @Test
    public void testDisjointAOI() throws TransformException {
        final GeneralEnvelope domainOfValidity = new GeneralEnvelope(HardCodedCRS.WGS84);
        domainOfValidity.setRange(0,  80, 100);
        domainOfValidity.setRange(1, -70, +70);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0,  50, 70);
        areaOfInterest.setRange(1, -80, 60);

        Envelope actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(areaOfInterest, actual);              // Expect no change.
    }

    /**
     * Tests {@link WraparoundAdjustment#shift(Envelope)}
     * with an envelope shifted by 360° before or after the grid valid area.
     *
     * @throws TransformException should never happen since this test does not transform coordinates.
     */
    @Test
    public void testWithShiftedAOI() throws TransformException {
        final GeneralEnvelope domainOfValidity = new GeneralEnvelope(HardCodedCRS.WGS84);
        domainOfValidity.setRange(0,  80, 100);
        domainOfValidity.setRange(1, -70, +70);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0,  70, 90);
        areaOfInterest.setRange(1, -80, 60);
        /*
         * AOI intersects the domain of validity: expected result is identical to given AOI.
         */
        final GeneralEnvelope expected = new GeneralEnvelope(areaOfInterest);
        Envelope actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);
        /*
         * AOI is on the left side of domain of validity. Expect a 360° shift to the right.
         */
        areaOfInterest.setRange(0, -290, -270);                    // [70 … 90] - 360
        actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);
        /*
         * AOI is on the right side of domain of validity. Expect a 360° shift to the left.
         */
        areaOfInterest.setRange(0, 430, 450);                      // [70 … 90] + 360
        actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, null);
        assertEnvelopeEquals(expected, actual);
    }

    /**
     * Tests {@link WraparoundAdjustment#shift(Envelope)}
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

    /**
     * Tests {@link WraparoundAdjustment#shift(Envelope)} with a projected envelope.
     *
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testWithProjection() throws TransformException {
        final GeneralEnvelope domainOfValidity = new GeneralEnvelope(HardCodedCRS.WGS84);
        domainOfValidity.setRange(0,  50,  60);
        domainOfValidity.setRange(1, -70, +70);

        final DefaultProjectedCRS mercator = HardCodedConversions.mercator();
        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(mercator);
        areaOfInterest.setRange(0,   5000000,  7000000);                        // About 45°E to 63°E
        areaOfInterest.setRange(1, -10000000, 10000000);                        // About 66.6°S to 66.6°N
        /*
         * AOI intersects the domain of validity: expected result is identical to given AOI.
         */
        final GeneralEnvelope expected = new GeneralEnvelope(areaOfInterest);
        final MathTransform validToAOI = mercator.getConversionFromBase().getMathTransform();
        Envelope actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, validToAOI);
        assertEnvelopeEquals(expected, actual);
        /*
         * AOI is on the right side of domain of validity. Expect a 360° shift to the left.
         * We add 40000 km to AOI, which is approximately the Earth circumference.
         */
        areaOfInterest.setRange(0, 45000000, 47000000);
        actual = adjustWraparoundAxes(areaOfInterest, domainOfValidity, validToAOI);
        assertEnvelopeEquals(expected, actual, 1E+5, Formulas.LINEAR_TOLERANCE);
    }
}
