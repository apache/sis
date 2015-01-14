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

import java.util.EnumSet;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import static java.lang.StrictMath.*;

// Test imports
import org.junit.Test;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import static org.opengis.test.Assert.*;

// Branch-dependent imports
import org.opengis.test.CalculationType;
import org.opengis.test.ToleranceModifier;
import org.opengis.test.ToleranceModifiers;


/**
 * Tests the {@link ExponentialTransform1D} class. Note that this is closely related to
 * {@link LogarithmicTransform1DTest}, since one transform is the inverse of the other.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-3.17)
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
        tolerance         = 1E-14;
        toleranceModifier = ToleranceModifier.RELATIVE;
        derivativeDeltas  = new double[] {0.001};
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
        assertFalse(transform.isIdentity());
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
        verifyDerivative(2.5); // Test at a hard-coded point.
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
        /*
         * The inverse transforms in this test case have high rounding errors.
         * Those errors are low for values close to zero, and increase fast for higher values.
         * We scale the default tolerance (1E-14) by 1E+8, which give us a tolerance of 1E-6.
         */
        toleranceModifier = ToleranceModifiers.concatenate(toleranceModifier,
                ToleranceModifiers.scale(EnumSet.of(CalculationType.INVERSE_TRANSFORM), 1E+8));
        run(ConcatenatedTransformDirect1D.class, base, SCALE, false, true);
    }

    /**
     * Implementation of {@link #testAffineConcatenations()} for the given base.
     */
    private void testAffineConcatenations(final double base) throws TransformException {
        final LinearTransform1D linear = LinearTransform1D.create(C1, C0);
        transform = MathTransforms.concatenate(linear, ExponentialTransform1D.create(base, SCALE), linear);

        // See testAffinePostConcatenation for an explanation about why we relax tolerance.
        toleranceModifier = ToleranceModifiers.concatenate(toleranceModifier,
                ToleranceModifiers.scale(EnumSet.of(CalculationType.INVERSE_TRANSFORM), 1E+8));
        run(ConcatenatedTransformDirect1D.class, base, SCALE, true, true);
    }

    /**
     * A single (non-concatenated) test case without scale.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSingle() throws TransformException {
        messageOnFailure = "Exponential transform in base 10";
        testSingle(10, 1);
        messageOnFailure = "Exponential transform in base E";
        testSingle(E, 1);
        messageOnFailure = "Exponential transform in base 8.4"; // Arbitrary base.
        testSingle(8.4, 1);
    }

    /**
     * A single (non-concatenated) test case with a scale.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingle")
    public void testSingleWithScale() throws TransformException {
        messageOnFailure = "Exponential transform in base 10";
        testSingle(10, SCALE);
        messageOnFailure = "Exponential transform in base E";
        testSingle(E, SCALE);
        messageOnFailure = "Exponential transform in base 8.4"; // Arbitrary base.
        testSingle(8.4, SCALE);
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
        messageOnFailure = "Affine + exponential transform in base 10";
        testAffinePreConcatenation(10);
        messageOnFailure = "Affine + exponential transform in base E";
        testAffinePreConcatenation(E);
        messageOnFailure = "Affine + exponential transform in base 8.4"; // Arbitrary base.
        testAffinePreConcatenation(8.4);
    }

    /**
     * Tests the concatenation of a linear operation after the exponential one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingleWithScale")
    public void testAffinePostConcatenation() throws TransformException {
        messageOnFailure = "Exponential transform in base 10 + affine";
        testAffinePostConcatenation(10);
        messageOnFailure = "Exponential transform in base E + affine";
        testAffinePostConcatenation(E);
        messageOnFailure = "Exponential transform in base 8.4 + affine"; // Arbitrary base.
        testAffinePostConcatenation(8.4);
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
        messageOnFailure = "Affine + exponential transform in base 10 + affine";
        testAffineConcatenations(10);
        messageOnFailure = "Affine + exponential transform in base E + affine";
        testAffineConcatenations(E);
        messageOnFailure = "Affine + exponential transform in base 8.4 + affine"; // Arbitrary base.
        testAffineConcatenations(8.4);
    }
}
