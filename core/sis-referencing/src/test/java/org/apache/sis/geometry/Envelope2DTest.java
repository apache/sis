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

import java.awt.geom.Rectangle2D;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Validators.*;
import static org.apache.sis.test.ReferencingAssert.*;
import static org.apache.sis.geometry.AbstractEnvelopeTest.WGS84;


/**
 * Tests the {@link Envelope2D} class.
 * Most tests are actually performed by {@link AbstractEnvelopeTest}, which compare
 * {@link GeneralEnvelope} results with {@code Envelope2D} results for ensuring consistency.
 * This class adds only some tests that are specific to {@code Envelope2D} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Ross Laidlaw
 * @since   0.3
 * @version 0.4
 * @module
 */
@DependsOn(AbstractEnvelopeTest.class)
public final strictfp class Envelope2DTest extends TestCase {
    /**
     * Tests {@code Envelope2D} serialization.
     */
    @Test
    public void testSerialization() {
        final Envelope2D e1 = new Envelope2D(WGS84, -20, -10, 40, 20);
        final Envelope2D e2 = assertSerializedEquals(e1);
        assertNotSame(e1, e2);
        validate(e2);
    }

    /**
     * Tests {@link Envelope2D#toRectangles()} on an empty envelope.
     *
     * @since 0.4
     */
    @Test
    public void testToRectanglesOnEmptyEnvelope() {
        final Envelope2D envelope = new Envelope2D(WGS84, 0, 0, 0, 0);
        assertEquals(0, envelope.toRectangles().length);
    }

    /**
     * Tests {@link Envelope2D#toRectangles()} on a simple envelope having no wraparound axis.
     *
     * @since 0.4
     */
    @Test
    public void testToRectanglesOnSimpleEnvelope() {
        final Envelope2D envelope = new Envelope2D(WGS84, -20, -10, 50, 40);
        final Rectangle2D[] rectangles = envelope.toRectangles();
        assertEquals(1, rectangles.length);
        final Rectangle2D r = rectangles[0];
        assertNotSame("toRectangles() shall copy the envelope.", envelope, r);
        assertEquals(-20.0, r.getX(),      STRICT);
        assertEquals(-10.0, r.getY(),      STRICT);
        assertEquals( 50.0, r.getWidth(),  STRICT);
        assertEquals( 40.0, r.getHeight(), STRICT);
    }

    /**
     * Tests {@link Envelope2D#toRectangles()} on an envelope crossing the anti-meridian.
     * The longitude range in this test is [155 … -150].
     *
     * @since 0.4
     */
    @Test
    @DependsOnMethod("testToRectanglesOnSimpleEnvelope")
    public void testToRectanglesOverAntiMeridian() {
        final Envelope2D envelope = new Envelope2D(WGS84, 155, 0, -150 - 155, 50);
        final Rectangle2D[] rectangles = envelope.toRectangles();
        assertEquals(2, rectangles.length);
        final Rectangle2D r0 = rectangles[0];
        final Rectangle2D r1 = rectangles[1];

        assertEquals( 155.0, r0.getX(),      STRICT);
        assertEquals(   0.0, r0.getY(),      STRICT);
        assertEquals(  25.0, r0.getWidth(),  STRICT);
        assertEquals(  50.0, r0.getHeight(), STRICT);

        assertEquals(-180.0, r1.getX(),      STRICT);
        assertEquals(   0.0, r1.getY(),      STRICT);
        assertEquals(  30.0, r1.getWidth(),  STRICT);
        assertEquals(  50.0, r1.getHeight(), STRICT);
    }
}
