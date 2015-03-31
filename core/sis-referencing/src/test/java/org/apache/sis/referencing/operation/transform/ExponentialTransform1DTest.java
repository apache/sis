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
package org.apache.sis.referencing.operation.transform;

import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import static java.lang.StrictMath.*;

// Test imports
import org.junit.Test;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests the {@link ExponentialTransform1D} class. Note that this is closely related to
 * {@link LogarithmicTransform1DTest}, since one transform is the inverse of the other.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn({
    CoordinateDomainTest.class,
    LinearTransformTest.class
})
public final strictfp class ExponentialTransform1DTest extends MathTransformTestCase {
    /**
     * Arbitrary parameter of the exponential transform to be tested.
     */
    static final double SCALE = 2;

    /**
     * Arbitrary coefficients of a linear transform to be concatenated to the exponential transform.
     */
    private static final double C0 = -3, C1 = 0.25;

    /**
     * Tolerance factor for comparison of coefficients (not coordinates).
     */
    private static final double EPS = 1E-12;

    /**
     * Creates a new test case.
     */
    public ExponentialTransform1DTest() {
        tolerance         = 1E-5; // Tolerance is much smaller on other branches.
//      toleranceModifier = ToleranceModifier.RELATIVE; // Not available on GeoAPI 3.0.
    }

    /**
     * Tests the current transform using random values as input points, and
     * comparing with the expected values computed using the given coefficients.
     *
     * The {@link #transform} field must be set before to invoke this method.
     *
     * @param expectedType The expected base type of the math transform.
     * @param base         The exponent base given to the {@link ExponentialTransform1D} constructor.
     * @param scale        The scale factor given to the {@link ExponentialTransform1D} constructor.
     * @param preAffine    {@code true} for applying an additional affine transform before the transform.
     * @param postAffine   {@code true} for applying an additional affine transform after the transform.
     */
    private void run(final Class<? extends MathTransform1D> expectedType, final double base, final double scale,
            final boolean preAffine, final boolean postAffine) throws TransformException
    {
        assertInstanceOf("Expected the use of mathematical identities.", expectedType, transform);
        assertIsNotIdentity(transform);
        validate();

        final double[] values = generateRandomCoordinates(CoordinateDomain.RANGE_10, 0);
        final double[] expected = new double[values.length];
        for (int i=0; i<values.length; i++) {
            double value = values[i];
            if (preAffine) {
                value = C0 + C1*value;
            }
            value = scale * pow(base, value);
            if (postAffine) {
                value = C0 + C1*value;
            }
            expected[i] = value;
        }
        verifyTransform(values, expected);
    }

    /**
     * Implementation of {@link #testSingle()} and {@link #testSingleWithScale()} for the given base.
     */
    private void testSingle(final double base, final double scale) throws TransformException {
        transform = ExponentialTransform1D.create(base, scale);
        run(ExponentialTransform1D.class, base, scale, false, false);
    }

    /**
     * Implementation of {@link #testAffinePreConcatenation()} for the given base.
     */
    private void testAffinePreConcatenation(final double base) throws TransformException {
        transform = MathTransforms.concatenate(LinearTransform1D.create(C1, C0),
                ExponentialTransform1D.create(base, SCALE));
        run(ExponentialTransform1D.class, base, SCALE, true, false);
        /*
         * Find back the original linear coefficients as documented in the ExponentialTransform1D class javadoc.
         */
        final double offset = -log(SCALE) / log(base);
        final MathTransform1D log = LogarithmicTransform1D.create(base, offset);
        transform = (LinearTransform1D) MathTransforms.concatenate(transform, log);
        assertEquals("C1", C1, ((LinearTransform1D) transform).scale,  EPS);
        assertEquals("C0", C0, ((LinearTransform1D) transform).offset, EPS);
    }

    /**
     * Implementation of {@link #testAffinePostConcatenation()} for the given base.
     */
    private void testAffinePostConcatenation(final double base) throws TransformException {
        transform = MathTransforms.concatenate(ExponentialTransform1D.create(base, SCALE),
                LinearTransform1D.create(C1, C0));
        run(ConcatenatedTransformDirect1D.class, base, SCALE, false, true);
    }

    /**
     * Implementation of {@link #testAffineConcatenations()} for the given base.
     */
    private void testAffineConcatenations(final double base) throws TransformException {
        final LinearTransform1D linear = LinearTransform1D.create(C1, C0);
        transform = MathTransforms.concatenate(linear, ExponentialTransform1D.create(base, SCALE), linear);
        run(ConcatenatedTransformDirect1D.class, base, SCALE, true, true);
    }

    /**
     * A single (non-concatenated) test case without scale.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSingle() throws TransformException {
        testSingle( 10, 1);  // Exponential transform in base 10
        testSingle(  E, 1);  // Exponential transform in base E
        testSingle(8.4, 1);  // Exponential transform in base 8.4 (arbitrary base)
    }

    /**
     * A single (non-concatenated) test case with a scale.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingle")
    public void testSingleWithScale() throws TransformException {
        testSingle( 10, SCALE);  // Exponential transform in base 10
        testSingle(  E, SCALE);  // Exponential transform in base E
        testSingle(8.4, SCALE);  // Exponential transform in base 8.4 (arbitrary base)
    }

    /**
     * Tests the concatenation of a linear operation before the exponential one. This test also
     * opportunistically verifies that the technic documented in {@link ExponentialTransform1D}
     * javadoc for finding back the original coefficients works.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingleWithScale")
    public void testAffinePreConcatenation() throws TransformException {
        testAffinePreConcatenation( 10);  // Affine + exponential transform in base 10
        testAffinePreConcatenation(  E);  // Affine + exponential transform in base E
        testAffinePreConcatenation(8.4);  // Affine + exponential transform in base 8.4 (arbitrary base)
    }

    /**
     * Tests the concatenation of a linear operation after the exponential one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingleWithScale")
    public void testAffinePostConcatenation() throws TransformException {
        testAffinePostConcatenation( 10);  // Exponential transform in base 10  + affine
        testAffinePostConcatenation(  E);  // Exponential transform in base E   + affine
        testAffinePostConcatenation(8.4);  // Exponential transform in base 8.4 + affine (arbitrary base)
    }

    /**
     * Tests the concatenation of a linear operation before and after the exponential one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod({
        "testAffinePreConcatenation",
        "testAffinePostConcatenation"
    })
    public void testAffineConcatenations() throws TransformException {
        testAffineConcatenations( 10);  // Affine + exponential transform in base 10  + affine
        testAffineConcatenations(  E);  // Affine + exponential transform in base E   + affine
        testAffineConcatenations(8.4);  // Affine + exponential transform in base 8.4 + affine (arbitrary base)
    }
}
