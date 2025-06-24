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

import org.opengis.referencing.operation.TransformException;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.referencing.Assertions.assertEnvelopeEquals;


/**
 * Tests the {@link EnvelopeReducer} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class EnvelopeReducerTest extends TestCase {
    /**
     * The envelopes on which to perform an operation.
     */
    private final GeneralEnvelope[] sources;

    /**
     * Creates an envelope for the given coordinate values.
     */
    private static GeneralEnvelope createFromExtremums(boolean latlon, double xmin, double ymin, double xmax, double ymax) {
        final GeneralEnvelope env;
        if (latlon) {
            env = new GeneralEnvelope(HardCodedCRS.WGS84_LATITUDE_FIRST);
            env.setRange(1, xmin, xmax);
            env.setRange(0, ymin, ymax);
        } else {
            env = new GeneralEnvelope(HardCodedCRS.WGS84);
            env.setRange(0, xmin, xmax);
            env.setRange(1, ymin, ymax);
        }
        return env;
    }

    /**
     * Creates a new test case.
     */
    public EnvelopeReducerTest() {
        sources = new GeneralEnvelope[] {
            createFromExtremums(true,  50, -5, 60, 15),
            createFromExtremums(false, 44,  3, 52, 16),
            createFromExtremums(true,  48, -2, 54, 12)
        };
    }

    /**
     * Test {@link Envelopes#union(Envelope...)} method.
     *
     * @throws TransformException if an error occurred while transforming an envelope.
     */
    @Test
    public void testUnion() throws TransformException {
        final GeneralEnvelope expected = createFromExtremums(true, 44, -5, 60, 16);
        final GeneralEnvelope actual = Envelopes.union(sources);
        assertEnvelopeEquals(expected, actual, STRICT);
    }

    /**
     * Test {@link Envelopes#intersect(Envelope...)} method.
     *
     * @throws TransformException if an error occurred while transforming an envelope.
     */
    @Test
    public void testIntersect() throws TransformException {
        final GeneralEnvelope expected = createFromExtremums(true, 50, 3, 52, 12);
        final GeneralEnvelope actual = Envelopes.intersect(sources);
        assertEnvelopeEquals(expected, actual, STRICT);
    }
}
