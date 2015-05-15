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
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.opengis.test.Validators.*;
import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests the methods defined in the {@link AbstractEnvelope} class.
 * Various implementations are used for each test.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@DependsOn(GeneralDirectPositionTest.class)
public final strictfp class AbstractEnvelopeTest extends TestCase {
    /**
     * Enumeration of implementations to be tested.
     * The {@code LAST} constant is for stopping the loops.
     */
    private static final int GENERAL=0, IMMUTABLE=1, RECTANGLE=2, SUBENVELOPE=3, LAST=4;

    /**
     * The coordinate reference system used for the tests.
     */
    static final GeographicCRS WGS84 = CommonCRS.WGS84.normalizedGeographic();

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
                final GeneralEnvelope ge = new GeneralEnvelope(2);
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
                GeneralEnvelope ge = new GeneralEnvelope(5);
                ge.setRange(1, xmin, xmax);
                ge.setRange(2, ymin, ymax);
                ge.setRange(0, 2, 3); // Following values will be verified in verifyInvariants(…)
                ge.setRange(3, 4, 6);
                ge.setRange(4, 8, 9);
                ge = ge.subEnvelope(1, 3);
                ge.setCoordinateReferenceSystem(WGS84);
                envelope = ge;
                break;
            }
            default: throw new IllegalArgumentException(String.valueOf(type));
        }
        if (PENDING_NEXT_GEOAPI_RELEASE) {
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
                final double[] ordinates = ((SubEnvelope) envelope).ordinates;
                assertEquals(2, ordinates[0], STRICT);
                assertEquals(3, ordinates[5], STRICT);
                assertEquals(4, ordinates[3], STRICT);
                assertEquals(6, ordinates[8], STRICT);
                assertEquals(8, ordinates[4], STRICT);
                assertEquals(9, ordinates[9], STRICT);
                break;
            }
        }
    }

    /**
     * Tests the simple case (no anti-meridian crossing).
     *
     * {@preformat text
     *     ┌─────────────┐
     *     │  ┌───────┐  │
     *     │  └───────┘  │
     *     └─────────────┘
     * }
     */
    @Test
    public void testSimpleEnvelope() {
        final DirectPosition2D inside  = new DirectPosition2D( 3, 32);
        final DirectPosition2D outside = new DirectPosition2D(-5, 32);
        final Envelope2D contained = (Envelope2D) create(RECTANGLE, -2, 10, 35, 40);
        final Envelope2D intersect = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final Envelope2D disjoint  = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, -4, 12, 30, 50);
            assertEquals(label, 30, envelope.getMinimum(1), STRICT);
            assertEquals(label, 50, envelope.getMaximum(1), STRICT);
            assertEquals(label, 40, envelope.getMedian (1), STRICT);
            assertEquals(label, 20, envelope.getSpan   (1), STRICT);
            assertEquals(label, -4, envelope.getMinimum(0), STRICT);
            assertEquals(label, 12, envelope.getMaximum(0), STRICT);
            assertEquals(label,  4, envelope.getMedian (0), STRICT);
            assertEquals(label, 16, envelope.getSpan   (0), STRICT);
            switch (type) {
                default: {
                    final AbstractEnvelope ext = (AbstractEnvelope) envelope;
                    assertTrue (label, ext.contains  (inside));
                    assertFalse(label, ext.contains  (outside));
                    assertFalse(label, ext.contains  (intersect, false));
                    assertTrue (label, ext.intersects(intersect, false));
                    assertDisjoint(ext, disjoint);
                    assertContains(ext, contained);
                    break;
                }
                case RECTANGLE: {
                    final Rectangle2D ext = (Rectangle2D) envelope;
                    assertTrue (label, ext.contains  (inside));
                    assertFalse(label, ext.contains  (outside));
                    assertFalse(label, ext.contains  (intersect));
                    assertTrue (label, ext.intersects(intersect));
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
     * {@preformat text
     *      ─────┐  ┌─────────              ─────┐      ┌─────
     *           │  │  ┌────┐       or      ──┐  │      │  ┌──
     *           │  │  └────┘               ──┘  │      │  └──
     *      ─────┘  └─────────              ─────┘      └─────
     * }
     */
    @Test
    public void testCrossingAntiMeridian() {
        final DirectPosition2D inside  = new DirectPosition2D(18, 32);
        final DirectPosition2D outside = new DirectPosition2D( 3, 32);
        final Envelope2D contained  = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        final Envelope2D intersect  = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final Envelope2D disjoint   = (Envelope2D) create(RECTANGLE, -2, 10, 35, 40);
        final Envelope2D spanning   = (Envelope2D) create(RECTANGLE, 16, -8, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, 12, -4, 30, 50);
            final DirectPosition lower = envelope.getLowerCorner();
            final DirectPosition upper = envelope.getUpperCorner();
            assertEquals(label,   30, envelope.getMinimum (1), STRICT);
            assertEquals(label,   50, envelope.getMaximum (1), STRICT);
            assertEquals(label,   40, envelope.getMedian  (1), STRICT);
            assertEquals(label,   20, envelope.getSpan    (1), STRICT);
            assertEquals(label,   12, lower   .getOrdinate(0), STRICT);
            assertEquals(label, -180, envelope.getMinimum (0), STRICT);
            assertEquals(label,   -4, upper   .getOrdinate(0), STRICT);
            assertEquals(label, +180, envelope.getMaximum (0), STRICT);
            assertEquals(label, -176, envelope.getMedian  (0), STRICT);
            assertEquals(label,  344, envelope.getSpan    (0), STRICT); // 360° - testSimpleEnvelope()
            switch (type) {
                default: {
                    final AbstractEnvelope ext = (AbstractEnvelope) envelope;
                    assertTrue (label, ext.contains  (inside));
                    assertFalse(label, ext.contains  (outside));
                    assertFalse(label, ext.contains  (intersect, false));
                    assertTrue (label, ext.intersects(intersect, false));
                    assertDisjoint(ext, disjoint);
                    assertContains(ext, contained);
                    assertContains(ext, spanning);
                    break;
                }
                case RECTANGLE: {
                    final Rectangle2D ext = (Rectangle2D) envelope;
                    assertTrue (label, ext.contains  (inside));
                    assertFalse(label, ext.contains  (outside));
                    assertFalse(label, ext.contains  (intersect));
                    assertTrue (label, ext.intersects(intersect));
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
     * {@preformat text
     *      ───┐    ┌─────────              ───┐      ┌─────
     *         │    │  ┌────┐       or      ───┼──┐   │  ┌──
     *         │    │  └────┘               ───┼──┘   │  └──
     *      ───┘    └─────────              ───┘      └─────
     * }
     */
    @Test
    public void testCrossingAntiMeridianTwice() {
        final DirectPosition2D inside  = new DirectPosition2D(18, 32);
        final DirectPosition2D outside = new DirectPosition2D( 3, 32);
        final Envelope2D contained  = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        final Envelope2D intersect  = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final Envelope2D disjoint   = (Envelope2D) create(RECTANGLE, -2, 10, 35, 40);
        final Envelope2D spanning   = (Envelope2D) create(RECTANGLE, 16, -8, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, 12, -364, 30, 50);
            final DirectPosition lower = envelope.getLowerCorner();
            final DirectPosition upper = envelope.getUpperCorner();
            assertEquals(label,   30, envelope.getMinimum (1), STRICT);
            assertEquals(label,   50, envelope.getMaximum (1), STRICT);
            assertEquals(label,   40, envelope.getMedian  (1), STRICT);
            assertEquals(label,   20, envelope.getSpan    (1), STRICT);
            assertEquals(label,   12, lower   .getOrdinate(0), STRICT);
            assertEquals(label, -180, envelope.getMinimum (0), STRICT);
            assertEquals(label, -364, upper   .getOrdinate(0), STRICT);
            assertEquals(label, +180, envelope.getMaximum (0), STRICT);
            assertEquals(label,    4, envelope.getMedian  (0), STRICT); // Note the alternance with the previous test methods.
            assertEquals(label,  NaN, envelope.getSpan    (0), STRICT); // testCrossingAntiMeridian() + 360°.
            if (envelope instanceof AbstractEnvelope) {
                final AbstractEnvelope ext = (AbstractEnvelope) envelope;
                assertTrue (label, ext.contains  (inside));
                assertFalse(label, ext.contains  (outside));
                assertFalse(label, ext.contains  (intersect, false));
                assertTrue (label, ext.intersects(intersect, false));
                assertFalse(label, ext.contains  (spanning,  false));
                assertTrue (label, ext.intersects(spanning,  false));
                assertDisjoint(ext, disjoint);
                assertContains(ext, contained);
                break;
            }
            if (envelope instanceof Rectangle2D) {
                final Rectangle2D ext = (Rectangle2D) envelope;
                assertTrue (label, ext.contains  (inside));
                assertFalse(label, ext.contains  (outside));
                assertFalse(label, ext.contains  (intersect));
                assertTrue (label, ext.intersects(intersect));
                assertFalse(label, ext.contains  (spanning));
                assertTrue (label, ext.intersects(spanning));
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
        final DirectPosition2D wasInside = new DirectPosition2D(18, 32);
        final DirectPosition2D outside   = new DirectPosition2D( 3, 32);
        final Envelope2D wasContained = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        final Envelope2D wasIntersect = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final Envelope2D disjoint     = (Envelope2D) create(RECTANGLE, -2, 10, 35, 40);
        final Envelope2D spanning     = (Envelope2D) create(RECTANGLE, 16, -8, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, 372, -364, 30, 50);
            final DirectPosition lower = envelope.getLowerCorner();
            final DirectPosition upper = envelope.getUpperCorner();
            assertEquals(label,   30, envelope.getMinimum (1), STRICT);
            assertEquals(label,   50, envelope.getMaximum (1), STRICT);
            assertEquals(label,   40, envelope.getMedian  (1), STRICT);
            assertEquals(label,   20, envelope.getSpan    (1), STRICT);
            assertEquals(label,  372, lower   .getOrdinate(0), STRICT);
            assertEquals(label, -180, envelope.getMinimum (0), STRICT);
            assertEquals(label, -364, upper   .getOrdinate(0), STRICT);
            assertEquals(label, +180, envelope.getMaximum (0), STRICT);
            assertEquals(label, -176, envelope.getMedian  (0), STRICT); // Note the alternance with the previous test methods.
            assertEquals(label,  NaN, envelope.getSpan    (0), STRICT); // testCrossingAntiMeridianTwice() + 360°.
            switch (type) {
                default: {
                    final AbstractEnvelope ext = (AbstractEnvelope) envelope;
                    assertFalse(label, ext.contains  (wasInside));
                    assertFalse(label, ext.contains  (outside));
                    assertFalse(label, ext.contains  (spanning,  false));
                    assertTrue (label, ext.intersects(spanning,  false));
                    assertDisjoint(ext, wasIntersect);
                    assertDisjoint(ext, disjoint);
                    assertDisjoint(ext, wasContained);
                    break;
                }
                case RECTANGLE: {
                    final Rectangle2D ext = (Rectangle2D) envelope;
                    assertFalse(label, ext.contains  (wasInside));
                    assertFalse(label, ext.contains  (outside));
                    assertFalse(label, ext.contains  (spanning));
                    assertTrue (label, ext.intersects(spanning));
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
        final DirectPosition2D wasInside = new DirectPosition2D(18, 32);
        final DirectPosition2D outside   = new DirectPosition2D( 3, 32);
        final Envelope2D wasContained = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        final Envelope2D intersect    = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final Envelope2D spanning     = (Envelope2D) create(RECTANGLE, 16, -8, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, -0.0, 0.0, 30, 50);
            assertEquals(label,   30, envelope.getMinimum(1), STRICT);
            assertEquals(label,   50, envelope.getMaximum(1), STRICT);
            assertEquals(label,   40, envelope.getMedian (1), STRICT);
            assertEquals(label,   20, envelope.getSpan   (1), STRICT);
            assertEquals(label, -0.0, envelope.getMinimum(0), STRICT);
            assertEquals(label,  0.0, envelope.getMaximum(0), STRICT);
            assertEquals(label,    0, envelope.getMedian (0), STRICT);
            assertEquals(label,    0, envelope.getSpan   (0), STRICT);
            switch (type) {
                default: {
                    final AbstractEnvelope ext = (AbstractEnvelope) envelope;
                    assertFalse(label, ext.contains  (wasInside));
                    assertFalse(label, ext.contains  (outside));
                    assertFalse(label, ext.contains  (intersect, false));
                    assertTrue (label, ext.intersects(intersect, false));
                    assertFalse(label, ext.contains  (spanning,  false));
                    assertFalse(label, ext.intersects(spanning,  false));
                    assertFalse(label, ext.intersects(spanning,  false));
                    assertDisjoint(ext, wasContained);
                    break;
                }
                case RECTANGLE: {
                    final Rectangle2D ext = (Rectangle2D) envelope;
                    assertFalse(label, ext.contains  (wasInside));
                    assertFalse(label, ext.contains  (outside));
                    assertFalse(label, ext.contains  (intersect));
                    assertTrue (label, ext.intersects(intersect));
                    assertFalse(label, ext.contains  (spanning));
                    assertFalse(label, ext.intersects(spanning));
                    assertFalse(label, ext.intersects(spanning));
                    assertDisjoint(ext, wasContained);
                    break;
                }
            }
            verifyInvariants(type, envelope);
        }
    }

    /**
     * Tests a case crossing the anti-meridian crossing, from 0° to -0°.
     */
    @Test
    public void testRange360() {
        final DirectPosition2D inside     = new DirectPosition2D(18, 32);
        final DirectPosition2D wasOutside = new DirectPosition2D( 3, 32);
        final Envelope2D contained = (Envelope2D) create(RECTANGLE, 14, 16, 35, 40);
        final Envelope2D intersect = (Envelope2D) create(RECTANGLE, -2, 16, 35, 40);
        final Envelope2D spanning  = (Envelope2D) create(RECTANGLE, 16, -8, 35, 40);
        for (int type=0; type<LAST; type++) {
            final String label = "Type " + type;
            final Envelope envelope = create(type, 0.0, -0.0, 30, 50);
            final DirectPosition lower = envelope.getLowerCorner();
            final DirectPosition upper = envelope.getUpperCorner();
            assertEquals(label,   30, envelope.getMinimum (1), STRICT);
            assertEquals(label,   50, envelope.getMaximum (1), STRICT);
            assertEquals(label,   40, envelope.getMedian  (1), STRICT);
            assertEquals(label,   20, envelope.getSpan    (1), STRICT);
            assertEquals(label,  0.0, lower   .getOrdinate(0), STRICT);
            assertEquals(label, -180, envelope.getMinimum (0), STRICT);
            assertEquals(label, -0.0, upper   .getOrdinate(0), STRICT);
            assertEquals(label, +180, envelope.getMaximum (0), STRICT);
            assertEquals(label,  180, envelope.getMedian  (0), STRICT);
            assertEquals(label,  360, envelope.getSpan    (0), STRICT);
            switch (type) {
                default: {
                    final AbstractEnvelope ext = (AbstractEnvelope) envelope;
                    assertTrue(label, ext.contains(inside));
                    assertTrue(label, ext.contains(wasOutside));
                    assertTrue(label, ext.intersects(intersect, false));
                    assertContains(ext, contained);
                    assertContains(ext, spanning);
                    break;
                }
                case RECTANGLE: {
                    final Rectangle2D ext = (Rectangle2D) envelope;
                    assertTrue(label, ext.contains(inside));
                    assertTrue(label, ext.contains(wasOutside));
                    assertTrue(label, ext.intersects(intersect));
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
     *
     * @since 0.4
     */
    @Test
    public void testToSimpleEnvelopesOnEmptyEnvelope() {
        for (int type=0; type<LAST; type++) {
            if (type != RECTANGLE) {
                final AbstractEnvelope envelope = (AbstractEnvelope) create(type, 0, 0, 0, 0);
                assertEquals(0, envelope.toSimpleEnvelopes().length);
            }
        }
    }

    /**
     * Tests {@link AbstractEnvelope#toSimpleEnvelopes()} on a simple envelope having no wraparound axis.
     *
     * @since 0.4
     */
    @Test
    @DependsOnMethod("testToSimpleEnvelopesOnEmptyEnvelope")
    public void testToSimpleEnvelopesOnSimpleEnvelope() {
        for (int type=0; type<LAST; type++) {
            if (type != RECTANGLE) {
                final AbstractEnvelope envelope = (AbstractEnvelope) create(type, -20, 30, -10, 15);
                final Envelope[] simples = envelope.toSimpleEnvelopes();
                assertEquals(1, simples.length);
                assertSame(envelope, simples[0]);
            }
        }
    }

    /**
     * Tests {@link AbstractEnvelope#toSimpleEnvelopes()} on a simple envelope having no wraparound axis.
     *
     * @since 0.4
     */
    @Test
    @DependsOnMethod("testToSimpleEnvelopesOnEmptyEnvelope")
    public void testToSimpleEnvelopesOverAntiMeridian() {
        for (int type=0; type<LAST; type++) {
            if (type != RECTANGLE) {
                final AbstractEnvelope envelope = (AbstractEnvelope) create(type, 155, -150, 0, 50);
                final Envelope[] simples = envelope.toSimpleEnvelopes();
                assertEquals(2, simples.length);
                final AbstractEnvelope e0 = (AbstractEnvelope) simples[0];
                final AbstractEnvelope e1 = (AbstractEnvelope) simples[1];

                assertEquals( 155.0, e0.getLower(0), STRICT);
                assertEquals(   0.0, e0.getLower(1), STRICT);
                assertEquals( 180.0, e0.getUpper(0), STRICT);
                assertEquals(  50.0, e0.getUpper(1), STRICT);
                assertEquals(  25.0, e0.getSpan (0), STRICT);
                assertEquals(  50.0, e0.getSpan (1), STRICT);

                assertEquals(-180.0, e1.getLower(0), STRICT);
                assertEquals(   0.0, e1.getLower(1), STRICT);
                assertEquals(-150.0, e1.getUpper(0), STRICT);
                assertEquals(  50.0, e1.getUpper(1), STRICT);
                assertEquals(  30.0, e1.getSpan (0), STRICT);
                assertEquals(  50.0, e1.getSpan (1), STRICT);
            }
        }
    }
}
