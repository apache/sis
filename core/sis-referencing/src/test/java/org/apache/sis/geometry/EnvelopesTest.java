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

import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.opengis.test.Validators.validate;


/**
 * Tests the {@link Envelopes} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn({
    GeneralEnvelopeTest.class,
    CurveExtremumTest.class
})
public final strictfp class EnvelopesTest extends TestCase {

    /*
     * Tests of the 'transform' methods are not yet ported because they need more MathTransform
     * implementations. Those tests will be ported in a future Apache SIS version.
     */

    /**
     * Tests {@link Envelopes#fromWKT(CharSequence)}. This test is provided as a matter of principle,
     * but the real test is done by {@link GeneralEnvelopeTest#testWktParsing()}.
     *
     * @throws FactoryException Should never happen.
     */
    @Test
    public void testFromWKT() throws FactoryException {
        final Envelope envelope = Envelopes.fromWKT("BOX(-100 -80, 70 40)");
        assertEquals(2, envelope.getDimension());
        assertEquals(-100, envelope.getMinimum(0), 0);
        assertEquals(  70, envelope.getMaximum(0), 0);
        assertEquals( -80, envelope.getMinimum(1), 0);
        assertEquals(  40, envelope.getMaximum(1), 0);
        validate(envelope);
    }

    /**
     * Tests {@link Envelopes#toString(Envelope)}.
     */
    @Test
    public void testToString() {
        final GeneralEnvelope envelope = new GeneralEnvelope(2);
        envelope.setRange(0, 40, 50);
        envelope.setRange(1, 20, 25);
        assertEquals("BOX(40 20, 50 25)", Envelopes.toString(envelope));
    }

    /**
     * Tests {@link Envelopes#toPolygonWKT(Envelope)}.
     */
    @Test
    public void testToPolygonWKT() {
        final GeneralEnvelope envelope = new GeneralEnvelope(2);
        envelope.setRange(0, 40, 50);
        envelope.setRange(1, 20, 25);
        assertEquals("POLYGON((40 20, 40 25, 50 25, 50 20, 40 20))", Envelopes.toPolygonWKT(envelope));
    }
}
