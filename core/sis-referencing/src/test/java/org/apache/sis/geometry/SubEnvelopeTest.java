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
package org.apache.sis.geometry;

import org.junit.Test;
import org.apache.sis.test.DependsOn;

import static java.lang.Double.NaN;
import static org.junit.Assert.*;
import static org.opengis.test.Validators.validate;
import static org.apache.sis.referencing.crs.HardCodedCRS.WGS84;


/**
 * Tests the {@link SubEnvelope} class. This method leverage the tests written for
 * {@link GeneralEnvelope}, but using a sub-envelope instead of a full envelope.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 0.3
 * @since   0.3
 * @module
 */
@DependsOn(GeneralEnvelopeTest.class)
public final strictfp class SubEnvelopeTest extends GeneralEnvelopeTest {
    /**
     * Creates a new sub-envelope envelope for the given coordinate values.
     */
    @Override
    GeneralEnvelope create(final double xmin, final double ymin, final double xmax, final double ymax) {
        final GeneralEnvelope envelope = new GeneralEnvelope(5);
        envelope.setEnvelope(1, 4, xmin, ymin, 5,
                             2, 7, xmax, ymax, 9);
        if (!skipValidation) {
            validate(envelope);
        }
        final GeneralEnvelope sub = envelope.subEnvelope(2, 4);
        sub.setCoordinateReferenceSystem(WGS84);
        if (!skipValidation) {
            validate(sub);
        }
        return sub;
    }

    /**
     * Verifies invariants for the given envelope after each test.
     */
    @Override
    void verifyInvariants(final GeneralEnvelope envelope) {
        super.verifyInvariants(envelope);
        final double[] coordinates = envelope.coordinates;
        assertEquals(1, coordinates[0], STRICT);
        assertEquals(4, coordinates[1], STRICT);
        assertEquals(5, coordinates[4], STRICT);
        assertEquals(2, coordinates[5], STRICT);
        assertEquals(7, coordinates[6], STRICT);
        assertEquals(9, coordinates[9], STRICT);
    }

    /**
     * Skips this test, since {@code SubEnvelope} provides nothing new compared to the super-class.
     */
    @Override
    public void testWktParsing() {
    }

    /**
     * Tests {@link SubEnvelope#setToNaN()}.
     */
    @Test
    public void testSetToNaN() {
        final GeneralEnvelope e = create(-40, -20, +60, +30);
        e.setToNaN();
        assertArrayEquals(new double[] {1, 4, NaN, NaN, 5, 2, 7, NaN, NaN, 9}, e.coordinates, STRICT);
        validate(e);
    }

    /**
     * Tests two distinct sub-envelopes for equality.
     */
    @Test
    @Override
    public void testEquals() {
        final GeneralEnvelope e1 = create(-40, -20, +60, +30);
        final GeneralEnvelope e2 = create(-40, -20, +60, +30);
        assertEquals(e1, e2);
        e2.coordinates[0] = -1;
        e2.coordinates[1] = -4;
        e2.coordinates[9] = -9;
        assertEquals(e1, e2);
        e2.coordinates[2] = -41;
        assertFalse(e1.equals(e2));
    }

    /**
     * Tests the {@link SubEnvelope#clone()} method.
     */
    @Test
    @Override
    public void testClone() {
        final GeneralEnvelope e1 = create(-40, -20, +60, +30);
        final GeneralEnvelope e2 = e1.clone();
        assertArrayEquals(new double[] {1, 4, -40, -20, 5, 2, 7, +60, +30, 9}, e1.coordinates, STRICT);
        assertArrayEquals(new double[] {      -40, -20,          +60, +30   }, e2.coordinates, STRICT);
        validate(e2);
    }
}
