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

import java.time.Instant;
import static java.lang.Double.NaN;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.measure.Range;
import org.apache.sis.metadata.internal.shared.AxisNames;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.io.wkt.Convention;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.referencing.EPSGDependentTestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.referencing.crs.HardCodedCRS.WGS84;
import static org.apache.sis.referencing.crs.HardCodedCRS.WGS84_LATITUDE_FIRST;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.referencing.Assertions.assertWktEquals;


/**
 * Tests the {@link GeneralEnvelope} class. The {@link Envelope2D} class will also be tested as a
 * side effect, because it is used for comparison purpose. Note that {@link AbstractEnvelopeTest}
 * already tested {@code contains} and {@code intersects} methods, so this test file will focus on
 * other methods.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public class GeneralEnvelopeTest extends EPSGDependentTestCase {
    /**
     * Tolerance threshold for floating point comparisons.
     */
    private static final double EPS = 1E-4;

    /**
     * {@code false} if {@link #create(double, double, double, double)} can validate the envelope.
     * This is set to {@code true} only when we intentionally want to create an invalid envelope,
     * for example in order to test normalization.
     */
    boolean skipValidation;

    /**
     * Creates a new test case.
     */
    public GeneralEnvelopeTest() {
    }

    /**
     * Creates a new geographic envelope for the given coordinate values.
     * The {@literal [xmin … xmax]} may span the anti-meridian.
     * This method is overridden by {@link SubEnvelopeTest}.
     */
    GeneralEnvelope create(final double xmin, final double ymin, final double xmax, final double ymax) {
        final GeneralEnvelope envelope = new GeneralEnvelope(2);
        envelope.setCoordinateReferenceSystem(WGS84);
        envelope.setEnvelope(xmin, ymin, xmax, ymax);
        if (!skipValidation) {
            validate(envelope);
        }
        return envelope;
    }

    /**
     * Verifies invariants for the given envelope after each test.
     * This method is overridden by {@link SubEnvelopeTest}.
     */
    void verifyInvariants(final GeneralEnvelope envelope) {
        assertSame(WGS84, envelope.getCoordinateReferenceSystem());
    }

    /**
     * Asserts that the given two-dimensional envelope is equal to the given rectangle.
     * The {@code xLower} and {@code xUpper} arguments are the <var>x</var> coordinate values
     * for the lower and upper corners respectively. The actual {@code xmin} and {@code ymin}
     * values will be inferred from those corners.
     *
     * <p>This method assumes that only the <var>x</var> axis may be a wraparound axis.</p>
     *
     * @param  test    the actual envelope to verify.
     * @param  xLower  the expected first   <var>x</var> coordinate value. May  be greater than {@code xUpper}.
     * @param  xUpper  the expected last    <var>x</var> coordinate value. May  be less    than {@code xLower}.
     * @param  ymin    the expected minimal <var>y</var> coordinate value. Must be less    than {@code ymax}.
     * @param  ymax    the expected maximal <var>y</var> coordinate value. Must be greater than {@code ymax}.
     */
    private static void assertEnvelopeEquals(final Envelope test,
            final double xLower, final double ymin, final double xUpper, final double ymax)
    {
        final double xmin, xmax;
        if (MathFunctions.isNegative(xUpper - xLower)) {                // Check for anti-meridian crossing.
            xmin = -180;
            xmax = +180;
        } else {
            xmin = xLower;
            xmax = xUpper;
        }
        final DirectPosition lower = test.getLowerCorner();
        final DirectPosition upper = test.getUpperCorner();
        assertEquals(xLower, lower.getCoordinate(0), "lower");
        assertEquals(xUpper, upper.getCoordinate(0), "upper");
        assertEquals(xmin,   test .getMinimum   (0), "xmin");
        assertEquals(xmax,   test .getMaximum   (0), "xmax");
        assertEquals(ymin,   test .getMinimum   (1), "ymin");
        assertEquals(ymax,   test .getMaximum   (1), "ymax");
        assertEquals(ymin,   lower.getCoordinate(1), "ymin");
        assertEquals(ymax,   upper.getCoordinate(1), "ymax");
        if (test instanceof Envelope2D ri) {
            assertEquals(xmin, ri.getMinX(), "xmin");
            assertEquals(xmax, ri.getMaxX(), "xmax");
            assertEquals(ymin, ri.getMinY(), "ymin");
            assertEquals(ymax, ri.getMaxY(), "ymax");
        }
    }

    /**
     * Asserts that the intersection of the two following envelopes is equal to the given rectangle.
     * First, this method tests using the {@link Envelope2D} implementation. Then, it tests using the
     * {@link GeneralEnvelope} implementation.
     */
    private static void assertIntersectEquals(final GeneralEnvelope e1, final GeneralEnvelope e2,
            final double xmin, final double ymin, final double xmax, final double ymax)
    {
        final Envelope2D r1 = new Envelope2D(e1);
        final Envelope2D r2 = new Envelope2D(e2);
        final Envelope2D ri = r1.createIntersection(r2);
        assertFalse(r1.isEmpty(), "isEmpty");
        assertEnvelopeEquals(ri, xmin, ymin, xmax, ymax);
        assertEquals(ri, r2.createIntersection(r1), "Interchanged arguments.");

        // Compares with GeneralEnvelope.
        final GeneralEnvelope ei = new GeneralEnvelope(e1);
        ei.intersect(e2);
        assertFalse(e1.isEmpty(), "isEmpty");
        assertEnvelopeEquals(ei, xmin, ymin, xmax, ymax);
        assertTrue(ei.equals(ri, STRICT, false), "Using GeneralEnvelope.");

        // Interchanges arguments.
        ei.setEnvelope(e2);
        ei.intersect(e1);
        assertFalse(e1.isEmpty(), "isEmpty");
        assertEnvelopeEquals(ei, xmin, ymin, xmax, ymax);
        assertTrue(ei.equals(ri, STRICT, false), "Using GeneralEnvelope.");
    }

    /**
     * Asserts that the union of the two following envelopes is equal to the given rectangle.
     * First, this method tests using the {@link Envelope2D} implementation.
     * Then, it tests using the {@link GeneralEnvelope} implementation.
     *
     * @param inf {@code true} if the range after union is infinite. The handling of such case is different for
     *        {@link GeneralEnvelope} than for {@link Envelope2D} because we cannot store infinite values in a
     *        reliable way in a {@link java.awt.geom.Rectangle2D} object, so we use NaN instead.
     * @param exactlyOneAntiMeridianSpan {@code true} if one envelope spans the anti-meridian and the other does not.
     */
    private static void assertUnionEquals(final GeneralEnvelope e1, final GeneralEnvelope e2,
            final double xmin, final double ymin, final double xmax, final double ymax,
            final boolean inf, final boolean exactlyOneAntiMeridianSpan)
    {
        final Envelope2D r1 = new Envelope2D(e1);
        final Envelope2D r2 = new Envelope2D(e2);
        final Envelope2D ri = r1.createUnion(r2);
        assertEnvelopeEquals(ri, inf ? NaN : xmin, ymin, inf ? NaN : xmax, ymax);
        assertEquals(ri, r2.createUnion(r1), "Interchanged arguments.");

        // Compares with GeneralEnvelope.
        final GeneralEnvelope ei = new GeneralEnvelope(e1);
        ei.add(e2);
        assertEnvelopeEquals(ei, xmin, ymin, xmax, ymax);
        if (!inf) {
            assertTrue(ei.equals(ri, STRICT, false), "Using GeneralEnvelope.");
        }

        // Interchanges arguments.
        ei.setEnvelope(e2);
        ei.add(e1);
        if (inf && exactlyOneAntiMeridianSpan) {
            assertEnvelopeEquals(ei, Double.NEGATIVE_INFINITY, ymin, Double.POSITIVE_INFINITY, ymax);
        } else {
            assertEnvelopeEquals(ei, xmin, ymin, xmax, ymax);
        }
        if (!inf) {
            assertTrue(ei.equals(ri, STRICT, false), "Using GeneralEnvelope.");
        }
    }

    /**
     * Asserts that adding the given point to the given envelope produces the given result.
     * First, this method tests using the {@link Envelope2D} implementation. Then, it tests
     * using the {@link GeneralEnvelope} implementation.
     */
    private static void assertAddEquals(final GeneralEnvelope e, final DirectPosition2D p,
            final double xmin, final double ymin, final double xmax, final double ymax)
    {
        final Envelope2D r = new Envelope2D(e);
        r.add(p);
        assertEnvelopeEquals(r, xmin, ymin, xmax, ymax);

        // Compares with GeneralEnvelope.
        final GeneralEnvelope ec = new GeneralEnvelope(e);
        ec.add(p);
        assertEnvelopeEquals(ec, xmin, ymin, xmax, ymax);
        assertTrue(ec.equals(r, STRICT, false), "Using GeneralEnvelope.");
    }

    /**
     * Tests the {@link GeneralEnvelope#intersect(Envelope)} and
     * {@link Envelope2D#createIntersection(Rectangle2D)} methods.
     */
    @Test
    public void testIntersection() {
        //  ┌─────────────┐
        //  │  ┌───────┐  │
        //  │  └───────┘  │
        //  └─────────────┘
        final GeneralEnvelope e1 = create(20, -20, 80, 10);
        final GeneralEnvelope e2 = create(40, -10, 62,  8);
        assertIntersectEquals(e1, e2, 40, -10, 62, 8);
        //  ┌──────────┐
        //  │  ┌───────┼──┐
        //  │  └───────┼──┘
        //  └──────────┘
        e1.setEnvelope(20, -20,  80, 12);
        e2.setEnvelope(40, -10, 100, 30);
        final double ymin=-10, ymax=12;                         // Will not change anymore
        assertIntersectEquals(e1, e2, 40, ymin, 80, ymax);
        //  ────┐  ┌────
        //  ──┐ │  │ ┌──
        //  ──┘ │  │ └──
        //  ────┘  └────
        e1.setRange(0,  80, 20);
        e2.setRange(0, 100, 18);
        assertIntersectEquals(e1, e2, 100, ymin, 18, ymax);
        //  ────┐  ┌────
        //  ────┼──┼─┐┌─
        //  ────┼──┼─┘└─
        //  ────┘  └────
        e2.setRange(0, 100, 90);
        assertIntersectEquals(e1, e2, 100, ymin, 20, ymax);
        //  ─────┐      ┌─────
        //     ┌─┼────┐ │
        //     └─┼────┘ │
        //  ─────┘      └─────
        e2.setRange(0, 10, 30);
        assertIntersectEquals(e1, e2, 10, ymin, 20, ymax);
        //  ──────────┐  ┌─────
        //    ┌────┐  │  │
        //    └────┘  │  │
        //  ──────────┘  └─────
        e2.setRange(0, 10, 16);
        assertIntersectEquals(e1, e2, 10, ymin, 16, ymax);
        //  ─────┐     ┌─────
        //       │ ┌─┐ │
        //       │ └─┘ │
        //  ─────┘     └─────
        e2.setRange(0, 40, 60);
        assertIntersectEquals(e1, e2, NaN, ymin, NaN, ymax);
        //  ─────┐     ┌─────
        //     ┌─┼─────┼─┐
        //     └─┼─────┼─┘
        //  ─────┘     └─────
        e2.setRange(0, 10, 90);
        assertIntersectEquals(e1, e2, NaN, ymin, NaN, ymax);
        //  ────────┬────────
        //        ┌─┼────┐
        //        └─┼────┘
        //  ────────┴────────
        e1.setRange(0, 0.0, -0.0);
        e2.setRange(0, -10,   30);
        assertIntersectEquals(e1, e2, -10, ymin, 30, ymax);
        //  ┌───────────────┐
        //  │               │
        //  │               │
        //  └───────────────┘
        e1.setRange(0, 0.0, -0.0);
        e2.setRange(0, 0.0, -0.0);
        assertIntersectEquals(e1, e2, 0.0, ymin, -0.0, ymax);

        // Post-test verification, mostly for SubEnvelope.
        verifyInvariants(e1);
        verifyInvariants(e2);
    }

    /**
     * Tests the {@link GeneralEnvelope#add(Envelope)} and
     * {@link Envelope2D#createUnion(Rectangle2D)} methods.
     */
    @Test
    public void testUnion() {
        //  ┌─────────────┐
        //  │  ┌───────┐  │
        //  │  └───────┘  │
        //  └─────────────┘
        final GeneralEnvelope e1 = create(20, -20, 80, 10);
        final GeneralEnvelope e2 = create(40, -10, 62,  8);
        assertUnionEquals(e1, e2, 20, -20, 80, 10, false, false);
        //  ┌──────────┐
        //  │  ┌───────┼──┐
        //  │  └───────┼──┘
        //  └──────────┘
        e1.setEnvelope(20, -20,  80, 12);
        e2.setEnvelope(40, -10, 100, 30);
        final double ymin=-20, ymax=30;     // Will not change anymore.
        assertUnionEquals(e1, e2, 20, ymin, 100, ymax, false, false);
        //  ────┐  ┌────
        //  ──┐ │  │ ┌──
        //  ──┘ │  │ └──
        //  ────┘  └────
        e1.setRange(0,  80, 20);
        e2.setRange(0, 100, 18);
        assertUnionEquals(e1, e2, 80, ymin, 20, ymax, false, false);
        //  ────┐  ┌────
        //  ────┼──┼─┐┌─
        //  ────┼──┼─┘└─
        //  ────┘  └────
        e2.setRange(0, 100, 90);
        assertUnionEquals(e1, e2, +0.0, ymin, -0.0, ymax, true, false);
        //  ─────┐      ┌─────
        //     ┌─┼────┐ │
        //     └─┼────┘ │
        //  ─────┘      └─────
        e2.setRange(0, 10, 30);
        assertUnionEquals(e1, e2, 80, ymin, 30, ymax, false, true);
        //  ──────────┐  ┌─────
        //    ┌────┐  │  │
        //    └────┘  │  │
        //  ──────────┘  └─────
        e2.setRange(0, 10, 16);
        assertUnionEquals(e1, e2, 80, ymin, 20, ymax, false, true);
        //  ─────┐     ┌─────
        //       │ ┌─┐ │
        //       │ └─┘ │
        //  ─────┘     └─────
        e2.setRange(0, 41, 60);
        assertUnionEquals(e1, e2, 41, ymin, 20, ymax, false, true);
        //  ─────┐     ┌─────
        //     ┌─┼─────┼─┐
        //     └─┼─────┼─┘
        //  ─────┘     └─────
        e2.setRange(0, 10, 90);
        assertUnionEquals(e1, e2, +0.0, ymin, -0.0, ymax, true, true);

        // Post-test verification, mostly for SubEnvelope.
        verifyInvariants(e1);
        verifyInvariants(e2);
    }

    /**
     * Tests {@link GeneralEnvelope#intersect(Envelope)} with NaN values.
     */
    @Test
    public void testIntersectionWithNaN() {
        GeneralEnvelope e1 = create(20, -20, 80, 10);
        GeneralEnvelope e2 = create(10, -30, 62, NaN);
        e1.intersect(e2); assertEnvelopeEquals(e1, 20, -20, 62, 10);    // ymin: unchanged
        e2.intersect(e1); assertEnvelopeEquals(e2, 20, -20, 62, NaN);   // ymin: -30 → -20

        // Same test but NaN on the lower value.
        e1 = create(20, -20, 80, 10);
        e2 = create(10, NaN, 62,  8);
        e1.intersect(e2); assertEnvelopeEquals(e1, 20, -20, 62, 8);     // ymax: 10 → 8
        e2.intersect(e1); assertEnvelopeEquals(e2, 20, NaN, 62, 8);     // ymax: unchanged

        // Similar test, but span anti-meridian.
        e1 = create(80, -20,  20, 10);
        e2 = create(30, -30, NaN, 15);
        e1.intersect(e2); assertEnvelopeEquals(e1, 80, -20, 20,  10);   // [x0 … x1] range unchanged.
        e2.intersect(e1); assertEnvelopeEquals(e2, 30, -20, NaN, 10);   // Idem.

        // Same test, but NaN on the lower value.
        e1 = create( 80, -20, 20, 10);
        e2 = create(NaN, -30, 62, 15);
        e1.intersect(e2); assertEnvelopeEquals(e1,  80, -20, 20, 10);   // [x0 … x1] range unchanged.
        e2.intersect(e1); assertEnvelopeEquals(e2, NaN, -20, 62, 10);   // Idem.
    }

    /**
     * Tests {@link GeneralEnvelope#add(Envelope)} with NaN values.
     */
    @Test
    public void testUnionWithNaN() {
        GeneralEnvelope e1 = create(20, -20, 80,  10);
        GeneralEnvelope e2 = create(10, -30, 62, NaN);

        // Expect ymin to be updated even if ymax is NaN.
        e1.add(e2); assertEnvelopeEquals(e1, 10, -30, 80, 10);          // ymin: -20 → -30
        e2.add(e1); assertEnvelopeEquals(e2, 10, -30, 80, NaN);         // ymin: unchanged

        // Same test but NaN on the lower value.
        e1 = create(20, -20, 80, 10);
        e2 = create(10, NaN, 62, 25);
        e1.add(e2); assertEnvelopeEquals(e1, 10, -20, 80, 25);          // ymax: 10 → 25
        e2.add(e1); assertEnvelopeEquals(e2, 10, NaN, 80, 25);          // ymax: unchanged

        // Similar test, but span anti-meridian.
        e1 = create(80, -20,  20, 10);
        e2 = create(30, -30, NaN, 15);
        e1.add(e2); assertEnvelopeEquals(e1, 80, -30, 20,  15);         // [x0 … x1] range unchanged.
        e2.add(e1); assertEnvelopeEquals(e2, 30, -30, NaN, 15);         // Idem.

        // Same test, but NaN on the lower value.
        e1 = create( 80, -20, 20, 10);
        e2 = create(NaN, -30, 62, 15);
        e1.add(e2); assertEnvelopeEquals(e1,  80, -30, 20, 15);         // [x0 … x1] range unchanged.
        e2.add(e1); assertEnvelopeEquals(e2, NaN, -30, 62, 15);         // Idem.
    }

    /**
     * Tests the {@link GeneralEnvelope#add(DirectPosition)} and
     * {@link Envelope2D#add(Point2D)} methods.
     */
    @Test
    public void testAddPoint() {
        final double ymin=-20, ymax=30;                             // Will not change anymore
        final GeneralEnvelope  e = create(20, ymin,  80, ymax);
        final DirectPosition2D p = new DirectPosition2D(40, 15);
        assertAddEquals(e, p, 20, ymin, 80, ymax);

        p.x = 100;                                                  // Add on the right side.
        assertAddEquals(e, p, 20, ymin, 100, ymax);

        p.x = -10;                                                  // Add on the left side.
        assertAddEquals(e, p, -10, ymin, 80, ymax);

        e.setRange(0,  80, 20);
        p.x = 100;                                                  // No change expected.
        assertAddEquals(e, p, 80, ymin, 20, ymax);

        p.x = 70;                                                   // Add on the right side.
        assertAddEquals(e, p, 70, ymin, 20, ymax);

        p.x = 30;                                                   // Add on the left side.
        assertAddEquals(e, p, 80, ymin, 30, ymax);

        verifyInvariants(e);
    }

    /**
     * Tests the {@link GeneralEnvelope#normalize()} method.
     */
    @Test
    public void testNormalize() {
        skipValidation = true;
        GeneralEnvelope e = create(-100, -100, +100, +100);
        assertTrue(e.normalize());
        assertEnvelopeEquals(e, -100, -90, +100, +90);

        e = create(185, 10, 190, 20);
        assertTrue(e.normalize());
        assertEnvelopeEquals(e, -175, 10, -170, 20);

        e = create(175, 10, 185, 20);
        assertTrue(e.normalize());
        assertEnvelopeEquals(e, 175, 10, -175, 20);

        e = create(0, 10, 360, 20);
        assertTrue(e.normalize());
        assertTrue(MathFunctions.isPositiveZero(e.getLower(0)), "Expect positive zero");
        assertTrue(MathFunctions.isNegativeZero(e.getUpper(0)), "Expect negative zero");
        verifyInvariants(e);
    }

    /**
     * Tests the {@link GeneralEnvelope#normalize()} method
     * with an envelope having more then 360° of longitude.
     */
    @Test
    public void testNormalizeWorld() {
        GeneralEnvelope e = create(-195, -90, +170, +90);       // -195° is equivalent to 165°
        assertTrue(e.normalize());
        assertEnvelopeEquals(e, -180, -90, +180, +90);
        verifyInvariants(e);
    }

    /**
     * Tests the {@link GeneralEnvelope#simplify()} method.
     */
    @Test
    public void testSimplify() {
        // Normal envelope: no change expected.
        GeneralEnvelope e = create(-100, -10, +100, +10);
        assertFalse(e.simplify());
        assertEnvelopeEquals(e, -100, -10, +100, +10);

        // Anti-meridian crossing: should substitute [-180 … 180]°
        e = create(30, -10, -60, 10);
        assertTrue(e.simplify());
        assertEnvelopeEquals(e, -180, -10, 180, 10);

        // Anti-meridian crossing using positive and negative zero.
        e = create(0.0, -10, -0.0, 10);
        assertTrue(e.simplify());
        assertEnvelopeEquals(e, -180, -10, 180, 10);
        verifyInvariants(e);
    }

    /**
     * Tests the {@link GeneralEnvelope#wraparound(WraparoundMethod)} method.
     */
    @Test
    public void tesWraparound() {
        GeneralEnvelope e = create(30, -10, -60, 10);
        assertTrue(e.wraparound(WraparoundMethod.CONTIGUOUS));
        assertEnvelopeEquals(e, 30, -10, 300, 10);

        e = create(30, -10, -15, 10);
        assertTrue(e.wraparound(WraparoundMethod.CONTIGUOUS));
        assertEnvelopeEquals(e, -330, -10, -15, 10);
    }

    /**
     * Tests {@link GeneralEnvelope#setEnvelope(Envelope)}.
     */
    @Test
    public void testCopy() {
        final GeneralEnvelope e = create(2, -4, 3, -3);
        e.setEnvelope(create(3, -5, -8, 2));
        assertEnvelopeEquals(e, 3, -5, -8, 2);
        verifyInvariants(e);
        /*
         * Tests with a different implementation, for testing another code path.
         * Constructor argument are (x, y, width, height).
         */
        e.setEnvelope(new Envelope2D(null, -2, 3, 8, 5));
        assertEnvelopeEquals(e, -2, 3, 6, 8);
        verifyInvariants(e);
    }

    /**
     * Tests {@link GeneralEnvelope#setEnvelope(double...)} with valid ranges,
     * then with a range which is known to be invalid.
     */
    @Test
    public void testSetEnvelope() {
        final GeneralEnvelope e = create(2, -4, 3, -3);
        e.setEnvelope(3, -5, -8, 2);
        var ex = assertThrows(IllegalArgumentException.class,
                () -> e.setEnvelope(1, -10, 2, -20),
                "Invalid range shall not be allowed.");
        assertMessageContains(ex, AxisNames.GEODETIC_LATITUDE);
        // Verify that the envelope still have the old values.
        assertEnvelopeEquals(e, 3, -5, -8, 2);
        verifyInvariants(e);
    }

    /**
     * Tests {@link GeneralEnvelope#setRange(int, double, double)} with a valid range,
     * then with a range which is known to be invalid.
     */
    @Test
    public void testSetRange() {
        final GeneralEnvelope e = create(2, -4, 3, -3);
        e.setRange(1, -5, 2);
        var ex = assertThrows(IllegalArgumentException.class,
                () -> e.setRange(1, -10, -20),
                "Invalid range shall not be allowed.");
        assertMessageContains(ex, AxisNames.GEODETIC_LATITUDE);
        // Verify that the envelope still have the old values.
        assertEnvelopeEquals(e, 2, -5, 3, 2);
        verifyInvariants(e);
    }

    /**
     * Tests {@link GeneralEnvelope#setCoordinateReferenceSystem(CoordinateReferenceSystem)}.
     */
    @Test
    public void testSetCoordinateReferenceSystem() {
        final GeneralEnvelope e = create(2, -4, 3, -3);
        e.setCoordinateReferenceSystem(null);
        /*
         * Set an invalid latitude range, but the Envelope cannot known that fact without CRS.
         * Only when we will specify the CRS, the envelope will realize that it contains an
         * invalid range.
         */
        e.setRange(1, -10, -20);
        var ex = assertThrows(IllegalStateException.class,
                () -> e.setCoordinateReferenceSystem(WGS84),
                "Invalid range shall not be allowed.");
        assertMessageContains(ex, AxisNames.GEODETIC_LATITUDE);
        /*
         * Verify that the envelope values are unchanged.
         * Then fix the range and try again to set the CRS.
         */
        assertEquals(  2, e.getLower(0));
        assertEquals(-10, e.getLower(1));
        assertEquals(  3, e.getUpper(0));
        assertEquals(-20, e.getUpper(1));
        e.setRange(1, -20, -10);
        e.setCoordinateReferenceSystem(WGS84);
        assertEnvelopeEquals(e, 2, -20, 3, -10);
        verifyInvariants(e);
    }

    /**
     * Tests modifying the corner of an envelope.
     */
    @Test
    public void testCornerModifications() {
        final GeneralEnvelope e = create(2, -4, 3, -3);
        e.getLowerCorner().setCoordinate(0,  1);
        e.getUpperCorner().setCoordinate(1, -1);
        assertEquals( 1, e.getLower(0));
        assertEquals(-4, e.getLower(1));
        assertEquals( 3, e.getUpper(0));
        assertEquals(-1, e.getUpper(1));
        verifyInvariants(e);
    }

    /**
     * Tests {@link GeneralEnvelope#translate(double...)}.
     */
    @Test
    public void testTranslate() {
        final GeneralEnvelope envelope = new GeneralEnvelope(new double[] {4, 5}, new double[] {8, 7});
        envelope.translate(2, -4);
        assertEnvelopeEquals(envelope, 6, 1, 10, 3);
    }

    /**
     * Tests {@link GeneralEnvelope#horizontal()}.
     */
    @Test
    public void testHorizontal() {
        GeneralEnvelope envelope = new GeneralEnvelope(new double[] {4, 12, 5, -8}, new double[] {8, 19, 7, -3});
        envelope.setCoordinateReferenceSystem(HardCodedCRS.GEOID_4D_MIXED_ORDER);
        envelope = envelope.horizontal();
        assertEnvelopeEquals(envelope, 5, -8, 7, -3);
        assertSame(WGS84_LATITUDE_FIRST, envelope.getCoordinateReferenceSystem());
    }

    /**
     * Tests {@link GeneralEnvelope#getTimeRange()} and {@link GeneralEnvelope#setTimeRange(Instant, Instant)}.
     * The temporal coordinates in this test are days elapsed since November 17, 1858 at 00:00 UTC.
     */
    @Test
    public void testTimeRange() {
        final GeneralEnvelope envelope = new GeneralEnvelope(HardCodedCRS.WGS84_WITH_TIME);
        envelope.setRange(0, -20, 25);
        envelope.setRange(1, -30, 12);
        envelope.setRange(2, 58840, 59000.75);
        final Range<Instant> range = envelope.getTimeRange().get();
        assertEquals(Instant.parse("2019-12-23T00:00:00Z"), range.getMinValue());
        assertEquals(Instant.parse("2020-05-31T18:00:00Z"), range.getMaxValue());

        envelope.setTimeRange(Instant.parse("2015-04-10T06:00:00Z"),
                              Instant.parse("2018-12-29T12:00:00Z"));
        assertArrayEquals(new double[] {-20, -30, 57122.25}, envelope.getLowerCorner().getCoordinates());
        assertArrayEquals(new double[] { 25,  12, 58481.50}, envelope.getUpperCorner().getCoordinates());
    }

    /**
     * Tests the {@link GeneralEnvelope#toString()} method.
     */
    @Test
    public void testToString() {
        GeneralEnvelope envelope = new GeneralEnvelope(new double[] {-180, -90}, new double[] {180, 90});
        assertEquals("BOX(-180 -90, 180 90)", envelope.toString());

        envelope = new GeneralEnvelope(3);
        envelope.setRange(0, -180, +180);
        envelope.setRange(1,  -90,  +90);
        envelope.setRange(2,   10,   30);
        assertEquals("BOX3D(-180 -90 10, 180 90 30)", envelope.toString());
    }

    /**
     * Tests the {@link GeneralEnvelope#GeneralEnvelope(CharSequence)} constructor.
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testWktParsing() {
        GeneralEnvelope envelope = new GeneralEnvelope("BOX(-180 -90,180 90)");
        assertEquals(2, envelope.getDimension());
        assertEquals(-180, envelope.getLower(0));
        assertEquals( 180, envelope.getUpper(0));
        assertEquals( -90, envelope.getLower(1));
        assertEquals(  90, envelope.getUpper(1));
        validate(envelope);

        envelope = new GeneralEnvelope("BOX3D(-180 -90 10, 180 90 30)");
        assertEquals(3, envelope.getDimension());
        assertEquals(-180, envelope.getLower(0));
        assertEquals( 180, envelope.getUpper(0));
        assertEquals( -90, envelope.getLower(1));
        assertEquals(  90, envelope.getUpper(1));
        assertEquals(  10, envelope.getLower(2));
        assertEquals(  30, envelope.getUpper(2));
        validate(envelope);

        envelope = new GeneralEnvelope("POLYGON((-80 -30,-100 40,80 40,100 -40,-80 -30))");
        assertEquals(-100, envelope.getLower(0));
        assertEquals( 100, envelope.getUpper(0));
        assertEquals( -40, envelope.getLower(1));
        assertEquals(  40, envelope.getUpper(1));
        validate(envelope);

        assertEquals("BOX(6 10, 6 10)",       new GeneralEnvelope("POINT(6 10)").toString());
        assertEquals("BOX3D(6 10 3, 6 10 3)", new GeneralEnvelope("POINT M [ 6 10 3 ] ").toString());
        assertEquals("BOX(3 4, 20 50)",       new GeneralEnvelope("LINESTRING(3 4,10 50,20 25)").toString());
        assertEquals("BOX(1 1, 6 5)",         new GeneralEnvelope(
                "MULTIPOLYGON(((1 1,5 1,5 5,1 5,1 1),(2 2, 3 2, 3 3, 2 3,2 2)),((3 3,6 2,6 4,3 3)))").toString());
        assertEquals("BOX(3 6, 7 10)", new GeneralEnvelope("GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(3 8,7 10))").toString());
        assertEquals(0, new GeneralEnvelope("BOX()").getDimension());

        try {
            new GeneralEnvelope("BOX(3 4");
            fail("Parsing should fails because of missing parenthesis.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("BOX"));
        }
        try {
            new GeneralEnvelope("LINESTRING(3 4,10 50),20 25)");
            fail("Parsing should fails because of missing parenthesis.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("LINESTRING"));
        }
    }

    /**
     * Tests {@link GeneralEnvelope#toWKT()} on a {@link GeneralEnvelope}.
     */
    @Test
    public void testWktFormatting() {
        final GeneralEnvelope envelope = new GeneralEnvelope(3);
        envelope.setRange(0,  6, 10);
        envelope.setRange(1, 16, 20);
        envelope.setRange(2, 23, 50);
        assertWktEquals(Convention.WKT2,    // Actually not in WKT2 standard.
                "BOX3D[ 6 16 23,\n" +
                "      10 20 50]", envelope);
    }

    /**
     * Tests the {@link GeneralEnvelope#equals(Object)} and
     * {@link GeneralEnvelope#equals(Envelope, double, boolean)} methods.
     */
    @Test
    public void testEquals() {
        /*
         * Initializes an empty envelope. The new envelope is empty
         * but not null because initialized to 0, not NaN.
         */
        final GeneralEnvelope e1 = new GeneralEnvelope(4);
        assertTrue  (e1.isEmpty());
        assertFalse (e1.isAllNaN());
        assertEquals(e1.getLowerCorner(), e1.getUpperCorner());
        /*
         * Initializes with arbitrary coordinate values.
         * Should not be empty anymore.
         */
        for (int i=e1.getDimension(); --i>=0;) {
            e1.setRange(i, i*5 + 2, i*6 + 5);
        }
        assertFalse(e1.isAllNaN ());
        assertFalse(e1.isEmpty());
        assertNotEquals(e1.getLowerCorner(), e1.getUpperCorner());
        /*
         * Creates a new envelope initialized with the same
         * coordinate values. The two envelope shall be equal.
         */
        final GeneralEnvelope e2 = new GeneralEnvelope(e1);
        assertPositionEquals(e1.getLowerCorner(), e2.getLowerCorner());
        assertPositionEquals(e1.getUpperCorner(), e2.getUpperCorner());
        assertTrue   (e1.contains(e2, true ));
        assertFalse  (e1.contains(e2, false));
        assertNotSame(e1, e2);
        assertEquals (e1, e2);
        assertTrue   (e1.equals(e2, EPS, true ));
        assertTrue   (e1.equals(e2, EPS, false));
        assertEquals (e1.hashCode(), e2.hashCode());
        /*
         * Offset slightly some coordinate value. Should not be equal anymore,
         * except when comparing with a tolerance value.
         */
        e2.setRange(2, e2.getLower(2) + 3E-5, e2.getUpper(2) - 3E-5);
        assertTrue (e1.contains(e2, true ));
        assertFalse(e1.contains(e2, false));
        assertFalse(e1.equals  (e2));
        assertTrue (e1.equals  (e2, EPS, true ));
        assertTrue (e1.equals  (e2, EPS, false));
        assertNotEquals(e1.hashCode(), e2.hashCode());
        /*
         * Applies a greater offset. Should not be equal,
         * even when comparing with a tolerance value.
         */
        e2.setRange(1, e2.getLower(1) + 1.5, e2.getUpper(1) - 1.5);
        assertTrue (e1.contains(e2, true ));
        assertFalse(e1.contains(e2, false));
        assertFalse(e1.equals  (e2));
        assertFalse(e1.equals  (e2, EPS, true ));
        assertFalse(e1.equals  (e2, EPS, false));
        assertNotEquals(e1.hashCode(), e2.hashCode());
    }

    /**
     * Compares the specified corners.
     */
    private static void assertPositionEquals(final DirectPosition p1, final DirectPosition p2) {
        assertNotSame(p1, p2);
        assertEquals (p1, p2);
        assertEquals (p1.hashCode(), p2.hashCode());
    }

    /**
     * Tests the {@link GeneralEnvelope#clone()} method.
     */
    @Test
    public void testClone() {
        final GeneralEnvelope e1 = new GeneralEnvelope(2);
        e1.setRange(0, -40, +60);
        e1.setRange(1, -20, +30);
        final GeneralEnvelope e2 = e1.clone();
        validate(e2);
        assertNotSame(e1, e2);
        assertEquals (e1, e2);
        e1.setRange(0, -40, +61);
        assertNotEquals(e1, e2, "Coordinates array should have been cloned.");
        e2.setRange(0, -40, +61);
        assertEquals(e1, e2);
    }

    /**
     * Tests {@code GeneralEnvelope} serialization.
     */
    @Test
    public final void testSerialization() {
        final GeneralEnvelope e1 = create(-20, -10, 20, 10);
        final GeneralEnvelope e2 = assertSerializedEquals(e1);
        assertNotSame(e1, e2);
        validate(e2);
    }
}
