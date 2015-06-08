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
import org.opengis.geometry.DirectPosition;
import org.apache.sis.internal.metadata.AxisNames;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.opengis.test.Validators.*;
import static org.apache.sis.test.ReferencingAssert.*;
import static org.apache.sis.geometry.AbstractEnvelopeTest.WGS84;


/**
 * Tests the {@link GeneralEnvelope} class. The {@link Envelope2D} class will also be tested as a
 * side effect, because it is used for comparison purpose. Note that {@link AbstractEnvelopeTest}
 * already tested {@code contains} and {@code intersects} methods, so this test file will focus on
 * other methods.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(AbstractEnvelopeTest.class)
public strictfp class GeneralEnvelopeTest extends TestCase {
    /**
     * Tolerance threshold for floating point comparisons.
     */
    private static final double EPS = 1E-4;

    /**
     * {@code false} if {@link #create(double, double, double, double)} can validate the envelope.
     * This is set to {@code true} only when we intentionally want to create an invalid envelope,
     * for example in order to test normalization.
     */
    boolean skipValidation = !PENDING_NEXT_GEOAPI_RELEASE;

    /**
     * Creates a new geographic envelope for the given ordinate values.
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
     * Asserts that the given two-dimensional envelope is equals to the given rectangle.
     * The {@code xLower} and {@code xUpper} arguments are the <var>x</var> ordinate values
     * for the lower and upper corners respectively. The actual {@code xmin} and {@code ymin}
     * values will be inferred from those corners.
     *
     * <p>This method assumes that only the <var>x</var> axis may be a wraparound axis.</p>
     *
     * @param test   The actual envelope to verify.
     * @param xLower The expected first   <var>x</var> ordinate value. May  be greater than {@code xUpper}.
     * @param xUpper The expected last    <var>x</var> ordinate value. May  be less    than {@code xLower}.
     * @param ymin   The expected minimal <var>y</var> ordinate value. Must be less    than {@code ymax}.
     * @param ymax   The expected maximal <var>y</var> ordinate value. Must be greater than {@code ymax}.
     */
    private static void assertEnvelopeEquals(final Envelope test,
            final double xLower, final double ymin, final double xUpper, final double ymax)
    {
        final double xmin, xmax;
        if (MathFunctions.isNegative(xUpper - xLower)) { // Check for anti-meridian spanning.
            xmin = -180;
            xmax = +180;
        } else {
            xmin = xLower;
            xmax = xUpper;
        }
        final DirectPosition lower = test.getLowerCorner();
        final DirectPosition upper = test.getUpperCorner();
        assertEquals("lower", xLower, lower.getOrdinate(0), STRICT);
        assertEquals("upper", xUpper, upper.getOrdinate(0), STRICT);
        assertEquals("xmin",  xmin,   test .getMinimum (0), STRICT);
        assertEquals("xmax",  xmax,   test .getMaximum (0), STRICT);
        assertEquals("ymin",  ymin,   test .getMinimum (1), STRICT);
        assertEquals("ymax",  ymax,   test .getMaximum (1), STRICT);
        assertEquals("ymin",  ymin,   lower.getOrdinate(1), STRICT);
        assertEquals("ymax",  ymax,   upper.getOrdinate(1), STRICT);
        if (test instanceof Envelope2D) {
            final Envelope2D ri = (Envelope2D) test;
            assertEquals("xmin", xmin, ri.getMinX(), STRICT);
            assertEquals("xmax", xmax, ri.getMaxX(), STRICT);
            assertEquals("ymin", ymin, ri.getMinY(), STRICT);
            assertEquals("ymax", ymax, ri.getMaxY(), STRICT);
        }
    }

    /**
     * Asserts that the intersection of the two following envelopes is equals to the given rectangle.
     * First, this method tests using the {@link Envelope2D} implementation. Then, it tests using the
     * {@link GeneralEnvelope} implementation.
     */
    private static void assertIntersectEquals(final GeneralEnvelope e1, final GeneralEnvelope e2,
            final double xmin, final double ymin, final double xmax, final double ymax)
    {
        final boolean isEmpty = !(((xmax - xmin) * (ymax - ymin)) != 0); // Use ! for catching NaN.
        final Envelope2D r1 = new Envelope2D(e1);
        final Envelope2D r2 = new Envelope2D(e2);
        final Envelope2D ri = r1.createIntersection(r2);
        assertEquals("isEmpty", isEmpty, r1.isEmpty());
        assertEnvelopeEquals(ri, xmin, ymin, xmax, ymax);
        assertEquals("Interchanged arguments.", ri, r2.createIntersection(r1));

        // Compares with GeneralEnvelope.
        final GeneralEnvelope ei = new GeneralEnvelope(e1);
        ei.intersect(e2);
        assertEquals("isEmpty", isEmpty, e1.isEmpty());
        assertEnvelopeEquals(ei, xmin, ymin, xmax, ymax);
        assertTrue("Using GeneralEnvelope.", ei.equals(ri, STRICT, false));

        // Interchanges arguments.
        ei.setEnvelope(e2);
        ei.intersect(e1);
        assertEquals("isEmpty", isEmpty, e1.isEmpty());
        assertEnvelopeEquals(ei, xmin, ymin, xmax, ymax);
        assertTrue("Using GeneralEnvelope.", ei.equals(ri, STRICT, false));
    }

    /**
     * Asserts that the union of the two following envelopes is equals to the given rectangle.
     * First, this method tests using the {@link Envelope2D} implementation. Then, it tests
     * using the {@link GeneralEnvelope} implementation.
     *
     * @param inf {@code true} if the range after union is infinite. The handling of such case
     *        is different for {@link GeneralEnvelope} than for {@link Envelope2D} because we
     *        can not store infinite values in a reliable way in a {@link Rectangle2D} object,
     *        so we use NaN instead.
     * @param exactlyOneAntiMeridianSpan {@code true} if one envelope spans the anti-meridian
     *        and the other does not.
     */
    private static void assertUnionEquals(final GeneralEnvelope e1, final GeneralEnvelope e2,
            final double xmin, final double ymin, final double xmax, final double ymax,
            final boolean inf, final boolean exactlyOneAntiMeridianSpan)
    {
        final Envelope2D r1 = new Envelope2D(e1);
        final Envelope2D r2 = new Envelope2D(e2);
        final Envelope2D ri = r1.createUnion(r2);
        assertEnvelopeEquals(ri, inf ? NaN : xmin, ymin, inf ? NaN : xmax, ymax);
        assertEquals("Interchanged arguments.", ri, r2.createUnion(r1));

        // Compares with GeneralEnvelope.
        final GeneralEnvelope ei = new GeneralEnvelope(e1);
        ei.add(e2);
        assertEnvelopeEquals(ei, xmin, ymin, xmax, ymax);
        if (!inf) {
            assertTrue("Using GeneralEnvelope.", ei.equals(ri, STRICT, false));
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
            assertTrue("Using GeneralEnvelope.", ei.equals(ri, STRICT, false));
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
        assertTrue("Using GeneralEnvelope.", ec.equals(r, STRICT, false));
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
        final double ymin=-10, ymax=12; // Will not change anymore
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
        final double ymin=-20, ymax=30; // Will not change anymore
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
     * Tests the {@link GeneralEnvelope#add(DirectPosition)} and
     * {@link Envelope2D#add(Point2D)} methods.
     */
    @Test
    public void testAddPoint() {
        final double ymin=-20, ymax=30; // Will not change anymore
        final GeneralEnvelope  e = create(20, ymin,  80, ymax);
        final DirectPosition2D p = new DirectPosition2D(40, 15);
        assertAddEquals(e, p, 20, ymin, 80, ymax);

        p.x = 100; // Add on the right side.
        assertAddEquals(e, p, 20, ymin, 100, ymax);

        p.x = -10; // Add on the left side.
        assertAddEquals(e, p, -10, ymin, 80, ymax);

        e.setRange(0,  80, 20);
        p.x = 100; // No change expected.
        assertAddEquals(e, p, 80, ymin, 20, ymax);

        p.x = 70; // Add on the right side.
        assertAddEquals(e, p, 70, ymin, 20, ymax);

        p.x = 30; // Add on the left side.
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
        assertTrue("Expect positive zero", MathFunctions.isPositiveZero(e.getLower(0)));
        assertTrue("Expect negative zero", MathFunctions.isNegativeZero(e.getUpper(0)));
        verifyInvariants(e);
    }

    /**
     * Tests the {@link GeneralEnvelope#normalize()} method
     * with an envelope having more then 360° of longitude.
     */
    @Test
    public void testNormalizeWorld() {
        GeneralEnvelope e = create(-195, -90, +170, +90); // -195° is equivalent to 165°
        assertTrue(e.normalize());
        assertEnvelopeEquals(e, -180, -90, +180, +90);
        verifyInvariants(e);
    }

    /**
     * Tests the {@link GeneralEnvelope#simplify()}.
     */
    @Test
    public void testSimplify() {
        // Normal envelope: no change expected.
        GeneralEnvelope e = create(-100, -10, +100, +10);
        assertFalse(e.simplify());
        assertEnvelopeEquals(e, -100, -10, +100, +10);

        // Anti-meridian spanning: should substitute [-180 … 180]°
        e = create(30, -10, -60, 10);
        assertTrue(e.simplify());
        assertEnvelopeEquals(e, -180, -10, 180, 10);

        // Anti-meridian spanning using positive and negative zero.
        e = create(0.0, -10, -0.0, 10);
        assertTrue(e.simplify());
        assertEnvelopeEquals(e, -180, -10, 180, 10);
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
        try {
            e.setEnvelope(1, -10, 2, -20);
            fail("Invalid range shall not be allowed.");
        } catch (IllegalArgumentException ex) {
            // This is the expected exception.
            final String message = ex.getMessage();
            assertTrue(message, message.contains(AxisNames.GEODETIC_LATITUDE));
        }
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
        try {
            e.setRange(1, -10, -20);
            fail("Invalid range shall not be allowed.");
        } catch (IllegalArgumentException ex) {
            // This is the expected exception.
            final String message = ex.getMessage();
            assertTrue(message, message.contains(AxisNames.GEODETIC_LATITUDE));
        }
        // Verify that the envelope still have the old values.
        assertEnvelopeEquals(e, 2, -5, 3, 2);
        verifyInvariants(e);
    }

    /**
     * Tests {@link GeneralEnvelope#setCoordinateReferenceSystem(CoordinateReferenceSystem)}.
     */
    @Test
    @DependsOnMethod("testSetRange")
    public void testSetCoordinateReferenceSystem() {
        final GeneralEnvelope e = create(2, -4, 3, -3);
        e.setCoordinateReferenceSystem(null);
        /*
         * Set an invalid latitude range, but the Envelope can not known that fact without CRS.
         * Only when we will specify the CRS, the envelope will realize that it contains an
         * invalid range.
         */
        e.setRange(1, -10, -20);
        try {
            e.setCoordinateReferenceSystem(WGS84);
            fail("Invalid range shall not be allowed.");
        } catch (IllegalStateException ex) {
            // This is the expected exception.
            final String message = ex.getMessage();
            assertTrue(message, message.contains(AxisNames.GEODETIC_LATITUDE));
        }
        /*
         * Verify that the envelope values are unchanged.
         * Then fix the range and try again to set the CRS.
         */
        assertEquals(  2, e.getLower(0), STRICT);
        assertEquals(-10, e.getLower(1), STRICT);
        assertEquals(  3, e.getUpper(0), STRICT);
        assertEquals(-20, e.getUpper(1), STRICT);
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
        e.getLowerCorner().setOrdinate(0,  1);
        e.getUpperCorner().setOrdinate(1, -1);
        assertEquals( 1, e.getLower(0), STRICT);
        assertEquals(-4, e.getLower(1), STRICT);
        assertEquals( 3, e.getUpper(0), STRICT);
        assertEquals(-1, e.getUpper(1), STRICT);
        verifyInvariants(e);
    }

    /**
     * Tests {@link GeneralEnvelope#translate(double...)}.
     *
     * @since 0.5
     */
    @Test
    public void testTranslate() {
        final GeneralEnvelope envelope = new GeneralEnvelope(new double[] {4, 5}, new double[] {8, 7});
        envelope.translate(2, -4);
        assertEnvelopeEquals(envelope, 6, 1, 10, 3);
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
    @DependsOnMethod("testToString")
    public void testWktParsing() {
        GeneralEnvelope envelope = new GeneralEnvelope("BOX(-180 -90,180 90)");
        assertEquals(2, envelope.getDimension());
        assertEquals(-180, envelope.getLower(0), STRICT);
        assertEquals( 180, envelope.getUpper(0), STRICT);
        assertEquals( -90, envelope.getLower(1), STRICT);
        assertEquals(  90, envelope.getUpper(1), STRICT);
        validate(envelope);

        envelope = new GeneralEnvelope("BOX3D(-180 -90 10, 180 90 30)");
        assertEquals(3, envelope.getDimension());
        assertEquals(-180, envelope.getLower(0), STRICT);
        assertEquals( 180, envelope.getUpper(0), STRICT);
        assertEquals( -90, envelope.getLower(1), STRICT);
        assertEquals(  90, envelope.getUpper(1), STRICT);
        assertEquals(  10, envelope.getLower(2), STRICT);
        assertEquals(  30, envelope.getUpper(2), STRICT);
        validate(envelope);

        envelope = new GeneralEnvelope("POLYGON((-80 -30,-100 40,80 40,100 -40,-80 -30))");
        assertEquals(-100, envelope.getLower(0), STRICT);
        assertEquals( 100, envelope.getUpper(0), STRICT);
        assertEquals( -40, envelope.getLower(1), STRICT);
        assertEquals(  40, envelope.getUpper(1), STRICT);
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
        assertFalse(e1.getLowerCorner().equals(e1.getUpperCorner()));
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
        assertFalse(e1.hashCode() == e2.hashCode());
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
        assertFalse(e1.hashCode() == e2.hashCode());
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
        assertNotSame("Expected a new instance.",           e1, e2);
        assertEquals ("The two instances should be equal.", e1, e2);
        e1.setRange(0, -40, +61);
        assertFalse("Ordinates array should have been cloned.", e1.equals(e2));
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
