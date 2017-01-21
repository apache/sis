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

import java.util.Collections;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.transform.MathTransformWrapper;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;
import static org.opengis.test.Validators.validate;


/**
 * Tests the {@link Envelopes} class.
 * This class inherits the test methods defined in {@link TransformTestCase}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
@DependsOn({
    GeneralEnvelopeTest.class,
    CurveExtremumTest.class
})
public final strictfp class EnvelopesTest extends TransformTestCase<GeneralEnvelope> {
    /**
     * Creates an envelope for the given CRS and coordinate values.
     */
    @Override
    GeneralEnvelope createFromExtremums(CoordinateReferenceSystem crs, double xmin, double ymin, double xmax, double ymax) {
        final GeneralEnvelope env = new GeneralEnvelope(crs);
        env.setRange(0, xmin, xmax);
        env.setRange(1, ymin, ymax);
        return env;
    }

    /**
     * Transforms an envelope using the given math transform.
     * This transformation can not handle poles.
     *
     * <p>This method wraps the math transform into an opaque object for hiding the fact that the given
     * transform implement the {@link MathTransform2D} interface. The intend is to disable optimization
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
     * Asserts that the given envelope is equals to the expected value.
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

    /**
     * Tests the transformation of an envelope from a 4D CRS to a 2D CRS
     * where the ordinates in one dimension are NaN.
     *
     * @throws TransformException if an error occurred while transforming the envelope.
     *
     * @since 0.8
     */
    @Test
    public void testTransform4to2D() throws TransformException {
        final CoordinateReferenceSystem targetCRS = HardCodedCRS.WGS84;
        final CoordinateReferenceSystem sourceCRS = new DefaultCompoundCRS(
                Collections.singletonMap(DefaultCompoundCRS.NAME_KEY, "4D CRS"),
                HardCodedCRS.WGS84,
                HardCodedCRS.GRAVITY_RELATED_HEIGHT,
                HardCodedCRS.TIME);

        final GeneralEnvelope env = new GeneralEnvelope(sourceCRS);
        env.setRange(0, -170, 170);
        env.setRange(1, -80,   80);
        env.setRange(2, -50,  -50);
        env.setRange(3, Double.NaN, Double.NaN);
        assertFalse("isAllNaN", env.isAllNaN());        // Opportunist test (not really the topic of this method).
        assertTrue ("isEmpty",  env.isEmpty());         // Opportunist test (not really the topic of this method).
        /*
         * If the referencing framework has selected the CopyTransform implementation
         * as expected, then the envelope ordinates should not be NaN.
         */
        final Envelope env2D = Envelopes.transform(env, targetCRS);
        assertEquals(-170, env2D.getMinimum(0), 0);
        assertEquals( 170, env2D.getMaximum(0), 0);
        assertEquals( -80, env2D.getMinimum(1), 0);
        assertEquals(  80, env2D.getMaximum(1), 0);
    }
}
