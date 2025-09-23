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
import java.util.List;
import java.util.Map;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.measure.Range;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.WraparoundTransform;
import org.apache.sis.referencing.operation.transform.MathTransformWrapper;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.referencing.EPSGDependentTestCase;
import org.apache.sis.referencing.operation.HardCodedConversions;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.referencing.Assertions.assertEnvelopeEquals;


/**
 * Tests the {@link Envelopes} class.
 * This class inherits the test methods defined in {@link TransformTestCase}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class EnvelopesTest extends TransformTestCase<GeneralEnvelope> {
    /**
     * Creates a new test case.
     */
    public EnvelopesTest() {
    }

    /**
     * Forces the check of whether of EPSG database exists before to start any tests.
     * This is done for avoiding race conditions logging the same message many times.
     */
    @BeforeAll
    public static void forceCheckForEPSG() {
        EPSGDependentTestCase.forceCheckForEPSG();
    }

    /**
     * Creates an envelope for the given CRS and coordinate values.
     */
    @Override
    GeneralEnvelope createFromExtremums(CoordinateReferenceSystem crs, double xmin, double ymin, double xmax, double ymax) {
        final var env = new GeneralEnvelope(crs);
        env.setRange(0, xmin, xmax);
        env.setRange(1, ymin, ymax);
        return env;
    }

    /**
     * Transforms an envelope using the given math transform.
     * This transformation cannot handle poles.
     *
     * <p>This method wraps the math transform into an opaque object for hiding the fact that the given
     * transform implements the {@link MathTransform2D} interface. The intent is to disable optimization
     * paths (if any), in order to test the generic path.</p>
     */
    @Override
    GeneralEnvelope transform(CoordinateReferenceSystem targetCRS, MathTransform2D transform, GeneralEnvelope envelope) throws TransformException {
        final GeneralEnvelope env = Envelopes.transform(new MathTransformWrapper(transform), envelope);
        env.setCoordinateReferenceSystem(targetCRS);
        return env;
    }

    /**
     * Transforms an envelope using the given operation.
     * This transformation can handle poles.
     */
    @Override
    GeneralEnvelope transform(CoordinateOperation operation, GeneralEnvelope envelope) throws TransformException {
        return Envelopes.transform(operation, envelope);
    }

    /**
     * Returns {@code true} if the outer envelope contains the inner one.
     */
    @Override
    boolean contains(GeneralEnvelope outer, GeneralEnvelope inner) {
        return outer.contains(inner);
    }

    /**
     * Asserts that the given envelope is equal to the expected value.
     */
    @Override
    void assertGeometryEquals(GeneralEnvelope expected, GeneralEnvelope actual, double tolx, double toly) {
        assertEnvelopeEquals(expected, actual, tolx, toly);
    }

    /**
     * Tests {@link Envelopes#fromWKT(CharSequence)}. This test is provided as a matter of principle,
     * but the real test is done by {@link GeneralEnvelopeTest#testWktParsing()}.
     *
     * @throws FactoryException if an error occurred during WKT parsing.
     */
    @Test
    public void testFromWKT() throws FactoryException {
        final Envelope envelope = Envelopes.fromWKT("BOX(-100 -80, 70 40)");
        assertEquals(2, envelope.getDimension());
        assertEquals(-100, envelope.getMinimum(0));
        assertEquals(  70, envelope.getMaximum(0));
        assertEquals( -80, envelope.getMinimum(1));
        assertEquals(  40, envelope.getMaximum(1));
        validate(envelope);
    }

    /**
     * Tests {@link Envelopes#toString(Envelope)}.
     */
    @Test
    public void testToString() {
        final var envelope = new GeneralEnvelope(2);
        envelope.setRange(0, 40, 50);
        envelope.setRange(1, 20, 25);
        assertEquals("BOX(40 20, 50 25)", Envelopes.toString(envelope));
    }

    /**
     * Tests {@link Envelopes#toPolygonWKT(Envelope)}.
     */
    @Test
    public void testToPolygonWKT() {
        final var envelope = new GeneralEnvelope(2);
        envelope.setRange(0, 40, 50);
        envelope.setRange(1, 20, 25);
        assertEquals("POLYGON((40 20, 40 25, 50 25, 50 20, 40 20))", Envelopes.toPolygonWKT(envelope));
    }

    /**
     * Tests the transformation of an envelope from a 4D CRS to a 2D CRS
     * where the coordinates in one dimension are NaN.
     *
     * @throws TransformException if an error occurred while transforming the envelope.
     */
    @Test
    public void testTransform4to2D() throws TransformException {
        final CoordinateReferenceSystem targetCRS = HardCodedCRS.WGS84;
        final CoordinateReferenceSystem sourceCRS = new DefaultCompoundCRS(
                Map.of(DefaultCompoundCRS.NAME_KEY, "4D CRS"),
                HardCodedCRS.WGS84,
                HardCodedCRS.GRAVITY_RELATED_HEIGHT,
                HardCodedCRS.TIME);

        final GeneralEnvelope env = new GeneralEnvelope(sourceCRS);
        env.setRange(0, -170, 170);
        env.setRange(1, -80,   80);
        env.setRange(2, -50,  -50);
        env.setRange(3, Double.NaN, Double.NaN);
        assertFalse(env.isAllNaN());        // Opportunist test (not really the topic of this method).
        assertTrue (env.isEmpty());         // Opportunist test (not really the topic of this method).
        /*
         * If the referencing framework has selected the CopyTransform implementation
         * as expected, then the envelope coordinates should not be NaN.
         */
        final Envelope env2D = Envelopes.transform(env, targetCRS);
        assertEquals(-170, env2D.getMinimum(0));
        assertEquals( 170, env2D.getMaximum(0));
        assertEquals( -80, env2D.getMinimum(1));
        assertEquals(  80, env2D.getMaximum(1));
    }

    /**
     * Tests a transformation from a three-dimensional CRS where the range of longitude axis is changed.
     * This is the same test as {@link #testAxisRangeChange()} but using a compound CRS as the source.
     * Internally, this results in the concatenation of transforms. We want to ensure that information
     * about axis range changes are not lost in this process.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     * @throws TransformException if an error occurred while transforming the envelope.
     */
    @Test
    public void testAxisRangeChange3D() throws FactoryException, TransformException {
        testAxisRangeChange3D(HardCodedCRS.WGS84);
    }

    /**
     * Tests a change of longitude axis range together with change of ellipsoid. This is the same test
     * than {@link #testAxisRangeChange3D()} with an additional complexity: a change of ellipsoid.
     * This causes the execution of different code branches in {@code ConcatenatedOperation} creation.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     * @throws TransformException if an error occurred while transforming the envelope.
     */
    @Test
    public void testAxisRangeChangeWithDatumShift() throws FactoryException, TransformException {
        testAxisRangeChange3D(HardCodedCRS.SPHERE);
    }

    /**
     * Implementation of {@link #testAxisRangeChange3D()} and {@link #testAxisRangeChangeWithDatumShift()}.
     */
    private void testAxisRangeChange3D(final GeographicCRS targetCRS) throws FactoryException, TransformException {
        final var envelope  = new GeneralEnvelope(new double[] { -0.5, -90, 1000},
                                                  new double[] {354.5, +90, 1002});
        envelope.setCoordinateReferenceSystem(CRS.compound(
                HardCodedCRS.WGS84.forConvention(AxesConvention.POSITIVE_RANGE), HardCodedCRS.TIME));
        final GeneralEnvelope expected = createFromExtremums(targetCRS, -0.5, -90, -5.5, 90);
        assertEnvelopeEquals(expected, Envelopes.transform(envelope, targetCRS), STRICT, STRICT);
        /*
         * When the envelope to transform span the full longitude range,
         * target envelope should unconditionally be [-180 … +180]°.
         */
        envelope.setRange(0, -0.5, 359.5);
        expected.setRange(0, -180, 180);
        assertEnvelopeEquals(expected, Envelopes.transform(envelope, targetCRS), STRICT, STRICT);
    }

    /**
     * Tests {@link Envelopes#findOperation(Envelope, Envelope)}.
     *
     * @throws FactoryException if an error occurred while searching the operation.
     */
    @Test
    public void testFindOperation() throws FactoryException {
        final var source = new GeneralEnvelope(HardCodedCRS.WGS84);
        final var target = new GeneralEnvelope(HardCodedCRS.GEOCENTRIC);
        source.setRange(0, 20, 30);
        source.setRange(1, 40, 45);
        target.setRange(0, 6000, 8000);
        target.setRange(1, 7000, 9000);
        target.setRange(2, 4000, 5000);
        CoordinateOperation op = Envelopes.findOperation(source, target);
        assertInstanceOf(Conversion.class, op);
        assertEquals("Geographic/geocentric conversions", ((Conversion) op).getMethod().getName().getCode());
    }

    /**
     * Test {@link Envelopes#compound(Envelope...)} method.
     *
     * @throws FactoryException if an error occurred while creating the compound CRS.
     */
    @Test
    public void testCompound() throws FactoryException {
        final var element0 = new GeneralEnvelope(HardCodedCRS.WGS84);
        final var element1 = new GeneralEnvelope(HardCodedCRS.TIME);
        final var expected = new GeneralEnvelope(3);
        element0.setRange(0,   20,   30);
        expected.setRange(0,   20,   30);
        element0.setRange(1,   40,   45);
        expected.setRange(1,   40,   45);
        element1.setRange(0, 1000, 1010);
        expected.setRange(2, 1000, 1010);
        Envelope env = Envelopes.compound(element0, element1);
        final List<SingleCRS> crs = CRS.getSingleComponents(env.getCoordinateReferenceSystem());
        assertEnvelopeEquals(expected, env);
        assertEquals(2, crs.size());
        assertSame(HardCodedCRS.WGS84, crs.get(0));
        assertSame(HardCodedCRS.TIME,  crs.get(1));
        /*
         * Try again without CRS in the second component.
         * The compound envelope shall not have CRS anymore.
         */
        element1.setCoordinateReferenceSystem(null);
        env = Envelopes.compound(element0, element1);
        assertEnvelopeEquals(expected, env);
        assertNull(env.getCoordinateReferenceSystem());
    }

    /**
     * Tests {@link Envelopes#toTimeRange(Envelope)}.
     *
     * @see GeneralEnvelopeTest#testTimeRange()
     */
    @Test
    public void testToTimeRange() {
        final var envelope = new GeneralEnvelope(HardCodedCRS.WGS84_WITH_TIME);
        envelope.setToNaN();
        envelope.setRange(2, 58840, 59000.75);
        final Range<Instant> range = Envelopes.toTimeRange(envelope).get();
        assertEquals(Instant.parse("2019-12-23T00:00:00Z"), range.getMinValue());
        assertEquals(Instant.parse("2020-05-31T18:00:00Z"), range.getMaxValue());
    }

    /**
     * Tests {@link Envelopes#transformWithWraparound(MathTransform, Envelope)}.
     *
     * @throws TransformException if a coordinate transformation failed.
     */
    @Test
    public void testWraparound() throws TransformException {
        final var envelope = new GeneralEnvelope(HardCodedCRS.WGS84);
        envelope.setRange(0, -200, -100);
        envelope.setRange(1, 5, 9);
        final MathTransform tr = WraparoundTransform.create(2, 0, 360, -180, 0);
        assertTrue(tr instanceof WraparoundTransform);
        final Map<Parameters, GeneralEnvelope> results = Envelopes.transformWithWraparound(tr, envelope);
        assertEquals(2, results.size());
        final GeneralEnvelope[] envelopes = results.values().toArray(GeneralEnvelope[]::new);
        assertEnvelopeEquals(new GeneralEnvelope(new double[] {-200, 5}, new double[] {-100, 9}), envelopes[0]);
        assertEnvelopeEquals(new GeneralEnvelope(new double[] { 160, 5}, new double[] { 260, 9}), envelopes[1]);
    }

    /**
     * Tests with an envelope slightly outside the domain of validity of Mercator projection.
     * This tests simulates the conversion of grid indices to CRS coordinates in a way similar
     * to what {@link org.apache.sis.coverage.grid.GridGeometry} does.
     * An envelope slightly outside the -180° … 180° longitude range is used.
     * A key goal of this test is to ensure that no unexpected longitude wraparound is applied.
     *
     * <p>This test uses a combination of the following factors:</p>
     * <ul>
     *   <li>A source envelope whose origin is zero.</li>
     *   <li>An intermediate geographic transform involving a wrap-around.</li>
     *   <li>A Mercator CRS destination.</li>
     * </ul>
     *
     * @throws TransformException if a coordinate transformation failed.
     */
    @Test
    public void testOutsideMercatorDomain() throws TransformException {
        final Envelope gridExtent = new Envelope2D(HardCodedCRS.IMAGE, 0, 0, 1024, 512);
        final ProjectedCRS targetCRS = HardCodedConversions.mercator();
        MathTransform gridToCRS = new AffineTransform2D(0.3515625, 0, 0, -0.3515625, -180.00001, 90);
        gridToCRS = MathTransforms.concatenate(gridToCRS, targetCRS.getConversionFromBase().getMathTransform());

        final GeneralEnvelope targetEnvelope = Envelopes.transform(gridToCRS, gridExtent);
        final double expected = targetCRS.getDatum().getEllipsoid().getSemiMajorAxis() * Math.PI;
        assertEquals(-expected, targetEnvelope.getMinimum(0), 2);
        assertEquals(+expected, targetEnvelope.getMaximum(0), 2);
    }
}
