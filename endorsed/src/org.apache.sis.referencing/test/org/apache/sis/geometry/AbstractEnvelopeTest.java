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
import static java.lang.Double.NaN;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.test.TestCase;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import static org.apache.sis.referencing.Assertions.assertContains;
import static org.apache.sis.referencing.Assertions.assertDisjoint;
import static org.apache.sis.referencing.crs.HardCodedCRS.WGS84;


/**
 * Tests the methods defined in the {@link AbstractEnvelope} class.
 * Various implementations are used for each test.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings("exports")
public final class AbstractEnvelopeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AbstractEnvelopeTest() {
    }

    /**
     * Enumeration of implementations to be tested.
     * The {@code LAST} constant is for stopping the loops.
     */
    private static final int GENERAL=0, IMMUTABLE=1, RECTANGLE=2, SUBENVELOPE=3, LAST=4;

    /**
     * Creates an envelope of the given type. The type shall be one of the
     * {@link #GENERAL}, {@link #IMMUTABLE} or {@link #RECTANGLE} constants.
     */
    private static Envelope create(final int type,
            final double xmin, final double xmax,
            final double ymin, final double ymax)
    {
        final Envelope envelope;
        switch (type) {
            case GENERAL: {
                final var ge = new GeneralEnvelope(2);
                ge.setCoordinateReferenceSystem(WGS84);
                ge.setRange(0, xmin, xmax);
                ge.setRange(1, ymin, ymax);
                envelope = ge;
                break;
            }
            case IMMUTABLE: {
                envelope = new ImmutableEnvelope(new double[] {xmin, ymin}, new double[] {xmax, ymax}, WGS84);
                break;
            }
            case RECTANGLE: {
                envelope = new Envelope2D(WGS84, xmin, ymin, xmax - xmin, ymax - ymin);
                break;
            }
            case SUBENVELOPE: {
                var ge = new GeneralEnvelope(5);
                ge.setRange(1, xmin, xmax);
                ge.setRange(2, ymin, ymax);
                ge.setRange(0, 2, 3);               // Following values will be verified in verifyInvariants(…)
                ge.setRange(3, 4, 6);
                ge.setRange(4, 8, 9);
                ge = ge.subEnvelope(1, 3);
                ge.setCoordinateReferenceSystem(WGS84);
                envelope = ge;
                break;
            }
            default: throw new IllegalArgumentException(String.valueOf(type));
        }
        if (type != RECTANGLE) {
            validate(envelope);
        }
        return envelope;
    }

    /**
     * Verifies some invariants for the given envelope of the given type.
     */
    private static void verifyInvariants(final int type, final Envelope envelope) {
        assertSame(WGS84, envelope.getCoordinateReferenceSystem());
        switch (type) {
            case SUBENVELOPE: {
                // Asserts that other dimensions in the original envelope has not been modified.
                final double[] coordinates = ((SubEnvelope) envelope).coordinates;
                assertEquals(2, coordinates[0]);
                assertEquals(3, coordinates[5]);
                assertEquals(4, coordinates[3]);
                assertEquals(6, coordinates[8]);
                assertEquals(8, coordinates[4]);
                assertEquals(9, coordinates[9]);
                break;
            }
        }
    }

    /**
     * Tests the simple case (no anti-meridian crossing).
     *
     * <pre class="text">
     *     ┌─────────────┐
     *     │  ┌───────┐  │
     *     │  └───────┘  │
     *     └─────────────┘</pre>
     */
    @Test
    public void testSimpleEnvelope() {
        final var inside    = new DirectPosition2D( 3, 32);
        final var outside   = new DirectPosition2D(-5, 32);
        final var contained = (Envelope2D) create(RECTANGLE, -2, 10, 35, 40);
        final var intersect = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final Envelope2D disjoint  = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, -4, 12, 30, 50);
            assertEquals(30, envelope.getMinimum(1), label);
            assertEquals(50, envelope.getMaximum(1), label);
            assertEquals(40, envelope.getMedian (1), label);
            assertEquals(20, envelope.getSpan   (1), label);
            assertEquals(-4, envelope.getMinimum(0), label);
            assertEquals(12, envelope.getMaximum(0), label);
            assertEquals( 4, envelope.getMedian (0), label);
            assertEquals(16, envelope.getSpan   (0), label);
            switch (type) {
                default: {
                    final var ext = (AbstractEnvelope) envelope;
                    assertTrue (ext.contains  (inside),           label);
                    assertFalse(ext.contains  (outside),          label);
                    assertFalse(ext.contains  (intersect, false), label);
                    assertTrue (ext.intersects(intersect, false), label);
                    assertDisjoint(ext, disjoint);
                    assertContains(ext, contained);
                    break;
                }
                case RECTANGLE: {
                    final var ext = (Rectangle2D) envelope;
                    assertTrue (ext.contains  (inside),    label);
                    assertFalse(ext.contains  (outside),   label);
                    assertFalse(ext.contains  (intersect), label);
                    assertTrue (ext.intersects(intersect), label);
                    assertDisjoint(ext, disjoint);
                    assertContains(ext, contained);
                    break;
                }
            }
            verifyInvariants(type, envelope);
        }
    }

    /**
     * Tests a case crossing the anti-meridian.
     *
     * <pre class="text">
     *      ─────┐  ┌─────────              ─────┐      ┌─────
     *           │  │  ┌────┐       or      ──┐  │      │  ┌──
     *           │  │  └────┘               ──┘  │      │  └──
     *      ─────┘  └─────────              ─────┘      └─────</pre>
     */
    @Test
    public void testCrossingAntiMeridian() {
        final var inside     = new DirectPosition2D(18, 32);
        final var outside    = new DirectPosition2D( 3, 32);
        final var contained  = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        final var intersect  = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final var disjoint   = (Envelope2D) create(RECTANGLE, -2, 10, 35, 40);
        final var spanning   = (Envelope2D) create(RECTANGLE, 16, -8, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, 12, -4, 30, 50);
            final DirectPosition lower = envelope.getLowerCorner();
            final DirectPosition upper = envelope.getUpperCorner();
            assertEquals(  30, envelope.getMinimum   (1), label);
            assertEquals(  50, envelope.getMaximum   (1), label);
            assertEquals(  40, envelope.getMedian    (1), label);
            assertEquals(  20, envelope.getSpan      (1), label);
            assertEquals(  12, lower   .getCoordinate(0), label);
            assertEquals(-180, envelope.getMinimum   (0), label);
            assertEquals(  -4, upper   .getCoordinate(0), label);
            assertEquals(+180, envelope.getMaximum   (0), label);
            assertEquals(-176, envelope.getMedian    (0), label);
            assertEquals( 344, envelope.getSpan      (0), label);       // 360° - testSimpleEnvelope()
            switch (type) {
                default: {
                    final var ext = (AbstractEnvelope) envelope;
                    assertTrue (ext.contains  (inside),           label);
                    assertFalse(ext.contains  (outside),          label);
                    assertFalse(ext.contains  (intersect, false), label);
                    assertTrue (ext.intersects(intersect, false), label);
                    assertDisjoint(ext, disjoint);
                    assertContains(ext, contained);
                    assertContains(ext, spanning);
                    break;
                }
                case RECTANGLE: {
                    final var ext = (Rectangle2D) envelope;
                    assertTrue (ext.contains  (inside),    label);
                    assertFalse(ext.contains  (outside),   label);
                    assertFalse(ext.contains  (intersect), label);
                    assertTrue (ext.intersects(intersect), label);
                    assertDisjoint(ext, disjoint);
                    assertContains(ext, contained);
                    assertContains(ext, spanning);
                    break;
                }
            }
            verifyInvariants(type, envelope);
        }
    }

    /**
     * Tests a the anti-meridian case with a larger empty space
     * on the left side.
     *
     * <pre class="text">
     *      ───┐    ┌─────────              ───┐      ┌─────
     *         │    │  ┌────┐       or      ───┼──┐   │  ┌──
     *         │    │  └────┘               ───┼──┘   │  └──
     *      ───┘    └─────────              ───┘      └─────</pre>
     */
    @Test
    public void testCrossingAntiMeridianTwice() {
        final var inside     = new DirectPosition2D(18, 32);
        final var outside    = new DirectPosition2D( 3, 32);
        final var contained  = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        final var intersect  = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final var disjoint   = (Envelope2D) create(RECTANGLE, -2, 10, 35, 40);
        final var spanning   = (Envelope2D) create(RECTANGLE, 16, -8, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, 12, -364, 30, 50);
            final DirectPosition lower = envelope.getLowerCorner();
            final DirectPosition upper = envelope.getUpperCorner();
            assertEquals(  30, envelope.getMinimum   (1), label);
            assertEquals(  50, envelope.getMaximum   (1), label);
            assertEquals(  40, envelope.getMedian    (1), label);
            assertEquals(  20, envelope.getSpan      (1), label);
            assertEquals(  12, lower   .getCoordinate(0), label);
            assertEquals(-180, envelope.getMinimum   (0), label);
            assertEquals(-364, upper   .getCoordinate(0), label);
            assertEquals(+180, envelope.getMaximum   (0), label);
            assertEquals(   4, envelope.getMedian    (0), label);   // Note the alternance with the previous test methods.
            assertEquals( NaN, envelope.getSpan      (0), label);   // testCrossingAntiMeridian() + 360°.
            if (envelope instanceof AbstractEnvelope ext) {
                assertTrue (ext.contains  (inside),           label);
                assertFalse(ext.contains  (outside),          label);
                assertFalse(ext.contains  (intersect, false), label);
                assertTrue (ext.intersects(intersect, false), label);
                assertFalse(ext.contains  (spanning,  false), label);
                assertTrue (ext.intersects(spanning,  false), label);
                assertDisjoint(ext, disjoint);
                assertContains(ext, contained);
                break;
            }
            if (envelope instanceof Rectangle2D ext) {
                assertTrue (ext.contains  (inside),    label);
                assertFalse(ext.contains  (outside),   label);
                assertFalse(ext.contains  (intersect), label);
                assertTrue (ext.intersects(intersect), label);
                assertFalse(ext.contains  (spanning),  label);
                assertTrue (ext.intersects(spanning),  label);
                assertDisjoint(ext, disjoint);
                assertContains(ext, contained);
                break;
            }
            verifyInvariants(type, envelope);
        }
    }

    /**
     * Tests a the anti-meridian case with a larger empty space
     * on the left and right sides.
     */
    @Test
    public void testCrossingAntiMeridianThreeTimes() {
        final var wasInside    = new DirectPosition2D(18, 32);
        final var outside      = new DirectPosition2D( 3, 32);
        final var wasContained = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        final var wasIntersect = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final var disjoint     = (Envelope2D) create(RECTANGLE, -2, 10, 35, 40);
        final var spanning     = (Envelope2D) create(RECTANGLE, 16, -8, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, 372, -364, 30, 50);
            final DirectPosition lower = envelope.getLowerCorner();
            final DirectPosition upper = envelope.getUpperCorner();
            assertEquals(  30, envelope.getMinimum   (1), label);
            assertEquals(  50, envelope.getMaximum   (1), label);
            assertEquals(  40, envelope.getMedian    (1), label);
            assertEquals(  20, envelope.getSpan      (1), label);
            assertEquals( 372, lower   .getCoordinate(0), label);
            assertEquals(-180, envelope.getMinimum   (0), label);
            assertEquals(-364, upper   .getCoordinate(0), label);
            assertEquals(+180, envelope.getMaximum   (0), label);
            assertEquals(-176, envelope.getMedian    (0), label);   // Note the alternance with the previous test methods.
            assertEquals( NaN, envelope.getSpan      (0), label);   // testCrossingAntiMeridianTwice() + 360°.
            switch (type) {
                default: {
                    final var ext = (AbstractEnvelope) envelope;
                    assertFalse(ext.contains  (wasInside),       label);
                    assertFalse(ext.contains  (outside),         label);
                    assertFalse(ext.contains  (spanning, false), label);
                    assertTrue (ext.intersects(spanning, false), label);
                    assertDisjoint(ext, wasIntersect);
                    assertDisjoint(ext, disjoint);
                    assertDisjoint(ext, wasContained);
                    break;
                }
                case RECTANGLE: {
                    final var ext = (Rectangle2D) envelope;
                    assertFalse(ext.contains  (wasInside), label);
                    assertFalse(ext.contains  (outside),   label);
                    assertFalse(ext.contains  (spanning),  label);
                    assertTrue (ext.intersects(spanning),  label);
                    assertDisjoint(ext, wasIntersect);
                    assertDisjoint(ext, disjoint);
                    assertDisjoint(ext, wasContained);
                    break;
                }
            }
            verifyInvariants(type, envelope);
        }
    }

    /**
     * Tests an empty envelope from -0 to 0°
     */
    @Test
    public void testRange0() {
        final var wasInside    = new DirectPosition2D(18, 32);
        final var outside      = new DirectPosition2D( 3, 32);
        final var wasContained = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        final var intersect    = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final var spanning     = (Envelope2D) create(RECTANGLE, 16, -8, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, -0.0, 0.0, 30, 50);
            assertEquals(  30, envelope.getMinimum(1), label);
            assertEquals(  50, envelope.getMaximum(1), label);
            assertEquals(  40, envelope.getMedian (1), label);
            assertEquals(  20, envelope.getSpan   (1), label);
            assertEquals(-0.0, envelope.getMinimum(0), label);
            assertEquals( 0.0, envelope.getMaximum(0), label);
            assertEquals(   0, envelope.getMedian (0), label);
            assertEquals(   0, envelope.getSpan   (0), label);
            switch (type) {
                default: {
                    final var ext = (AbstractEnvelope) envelope;
                    assertFalse(ext.contains  (wasInside),        label);
                    assertFalse(ext.contains  (outside),          label);
                    assertFalse(ext.contains  (intersect, false), label);
                    assertTrue (ext.intersects(intersect, false), label);
                    assertFalse(ext.contains  (spanning,  false), label);
                    assertFalse(ext.intersects(spanning,  false), label);
                    assertFalse(ext.intersects(spanning,  false), label);
                    assertDisjoint(ext, wasContained);
                    break;
                }
                case RECTANGLE: {
                    final var ext = (Rectangle2D) envelope;
                    assertFalse(ext.contains  (wasInside), label);
                    assertFalse(ext.contains  (outside),   label);
                    assertFalse(ext.contains  (intersect), label);
                    assertTrue (ext.intersects(intersect), label);
                    assertFalse(ext.contains  (spanning),  label);
                    assertFalse(ext.intersects(spanning),  label);
                    assertFalse(ext.intersects(spanning),  label);
                    assertDisjoint(ext, wasContained);
                    break;
                }
            }
            verifyInvariants(type, envelope);
        }
    }

    /**
     * Tests a case crossing the anti-meridian, from 0° to -0°.
     */
    @Test
    public void testRange360() {
        final var inside     = new DirectPosition2D(18, 32);
        final var wasOutside = new DirectPosition2D( 3, 32);
        final var contained  = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        final var intersect  = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final var spanning   = (Envelope2D) create(RECTANGLE, 16, -8, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, 0.0, -0.0, 30, 50);
            final DirectPosition lower = envelope.getLowerCorner();
            final DirectPosition upper = envelope.getUpperCorner();
            assertEquals(  30, envelope.getMinimum   (1), label);
            assertEquals(  50, envelope.getMaximum   (1), label);
            assertEquals(  40, envelope.getMedian    (1), label);
            assertEquals(  20, envelope.getSpan      (1), label);
            assertEquals( 0.0, lower   .getCoordinate(0), label);
            assertEquals(-180, envelope.getMinimum   (0), label);
            assertEquals(-0.0, upper   .getCoordinate(0), label);
            assertEquals(+180, envelope.getMaximum   (0), label);
            assertEquals( 180, envelope.getMedian    (0), label);
            assertEquals( 360, envelope.getSpan      (0), label);
            switch (type) {
                default: {
                    final var ext = (AbstractEnvelope) envelope;
                    assertTrue(ext.contains(inside), label);
                    assertTrue(ext.contains(wasOutside), label);
                    assertTrue(ext.intersects(intersect, false), label);
                    assertContains(ext, contained);
                    assertContains(ext, spanning);
                    break;
                }
                case RECTANGLE: {
                    final var ext = (Rectangle2D) envelope;
                    assertTrue(ext.contains(inside),      label);
                    assertTrue(ext.contains(wasOutside),  label);
                    assertTrue(ext.intersects(intersect), label);
                    assertContains(ext, contained);
                    assertContains(ext, spanning);
                    break;
                }
            }
            verifyInvariants(type, envelope);
        }
    }

    /**
     * Tests {@link AbstractEnvelope#toSimpleEnvelopes()} on an empty envelope.
     */
    @Test
    public void testToSimpleEnvelopesOnEmptyEnvelope() {
        for (int type=0; type<LAST; type++) {
            if (type != RECTANGLE) {
                final var envelope = (AbstractEnvelope) create(type, 0, 0, 0, 0);
                assertEquals(0, envelope.toSimpleEnvelopes().length);
            }
        }
    }

    /**
     * Tests {@link AbstractEnvelope#toSimpleEnvelopes()} on a simple envelope having no wraparound axis.
     */
    @Test
    public void testToSimpleEnvelopesOnSimpleEnvelope() {
        for (int type=0; type<LAST; type++) {
            if (type != RECTANGLE) {
                final var envelope = (AbstractEnvelope) create(type, -20, 30, -10, 15);
                final Envelope[] simples = envelope.toSimpleEnvelopes();
                assertEquals(1, simples.length);
                assertSame(envelope, simples[0]);
            }
        }
    }

    /**
     * Tests {@link AbstractEnvelope#toSimpleEnvelopes()} on a simple envelope having no wraparound axis.
     */
    @Test
    public void testToSimpleEnvelopesOverAntiMeridian() {
        for (int type=0; type<LAST; type++) {
            if (type != RECTANGLE) {
                final var envelope = (AbstractEnvelope) create(type, 155, -150, 0, 50);
                final Envelope[] simples = envelope.toSimpleEnvelopes();
                assertEquals(2, simples.length);
                final var e0 = (AbstractEnvelope) simples[0];
                final var e1 = (AbstractEnvelope) simples[1];

                assertEquals( 155.0, e0.getLower(0));
                assertEquals(   0.0, e0.getLower(1));
                assertEquals( 180.0, e0.getUpper(0));
                assertEquals(  50.0, e0.getUpper(1));
                assertEquals(  25.0, e0.getSpan (0));
                assertEquals(  50.0, e0.getSpan (1));

                assertEquals(-180.0, e1.getLower(0));
                assertEquals(   0.0, e1.getLower(1));
                assertEquals(-150.0, e1.getUpper(0));
                assertEquals(  50.0, e1.getUpper(1));
                assertEquals(  30.0, e1.getSpan (0));
                assertEquals(  50.0, e1.getSpan (1));
            }
        }
    }
}
