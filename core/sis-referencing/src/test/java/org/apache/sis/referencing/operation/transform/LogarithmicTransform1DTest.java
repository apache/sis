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
 * Tests the {@link LogarithmicTransform1D} class. Note that this is closely related to
 * {@link ExponentialTransform1DTest}, since one transform is the inverse of the other.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
@DependsOn(ExponentialTransform1DTest.class)
public final strictfp class LogarithmicTransform1DTest extends MathTransformTestCase {
    /**
     * Arbitrary parameter of the logarithmic transform to be tested.
     */
    private static final double OFFSET = 1.5;

    /**
     * Arbitrary coefficients of a linear transform to be concatenated to the exponential transform.
     */
    private static final double C0 = 0.75, C1 = 0.25;

    /**
     * Creates a new test case.
     */
    public LogarithmicTransform1DTest() {
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
     * @param base         The exponent base given to the {@link LogarithmicTransform1D} constructor.
     * @param offset       The offset given to the {@link LogarithmicTransform1D} constructor.
     * @param preAffine    {@code true} for applying an additional affine transform before the transform.
     * @param postAffine   {@code true} for applying an additional affine transform after the transform.
     */
    private void run(final Class<? extends MathTransform1D> expectedType, final double base, final double offset,
            final boolean preAffine, final boolean postAffine) throws TransformException
    {
        assertInstanceOf("Expected the use of mathematical identities.", expectedType, transform);
        assertIsNotIdentity(transform);
        validate();

        final double[] values = generateRandomCoordinates(CoordinateDomain.RANGE_10, 0);
        final double[] expected = new double[values.length];
        final double lnBase = log(base);
        for (int i=0; i<values.length; i++) {
            double value = abs(values[i]) + 0.001; // Makes the values valid for logarithms.
            values[i] = value;
            if (preAffine) {
                value = C0 + C1*value;
            }
            value = log(value) / lnBase + offset;
            if (postAffine) {
                value = C0 + C1*value;
            }
            expected[i] = value;
        }
        verifyTransform(values, expected);
    }

    /**
     * Implementation of {@link #testSingle()} and {@link #testSingleWithOffset()} for the given base.
     */
    private void testSingle(final double base, final double offset,
            final Class<? extends MathTransform1D> expectedType) throws TransformException
    {
        transform = LogarithmicTransform1D.create(base, offset);
        run(expectedType, base, offset, false, false);
    }

    /**
     * Implementation of {@link #testAffinePreConcatenation()} for the given base.
     */
    private void testAffinePreConcatenation(final double base) throws TransformException {
        transform = MathTransforms.concatenate(LinearTransform1D.create(C1, C0),
                LogarithmicTransform1D.create(base, OFFSET));
        run(ConcatenatedTransformDirect1D.class, base, OFFSET, true, false);
    }

    /**
     * Implementation of {@link #testAffinePostConcatenation()} for the given base.
     */
    private void testAffinePostConcatenation(final double base) throws TransformException {
        transform = MathTransforms.concatenate(LogarithmicTransform1D.create(base, OFFSET),
                LinearTransform1D.create(C1, C0));
        run(ConcatenatedTransformDirect1D.class, base, OFFSET, false, true);
    }

    /**
     * Implementation of {@link #testAffineConcatenations()} for the given base.
     */
    private void testAffineConcatenations(final double base) throws TransformException {
        final MathTransform1D linear = LinearTransform1D.create(C1, C0);
        transform = MathTransforms.concatenate(linear, LogarithmicTransform1D.create(base, OFFSET), linear);
        run(ConcatenatedTransformDirect1D.class, base, OFFSET, true, true);
    }

    /**
     * A single (non-concatenated) test case without offset.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSingle() throws TransformException {
        testSingle( 10, 0, LogarithmicTransform1D.class);         // Logarithmic transform in base 10
        testSingle(  E, 0, LogarithmicTransform1D.class);         // Logarithmic transform in base E
        testSingle(8.4, 0, ConcatenatedTransformDirect1D.class);  // Logarithmic transform in base 8.4 (arbitrary base)
    }

    /**
     * A single (non-concatenated) test case with an offset.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingle")
    public void testSingleWithOffset() throws TransformException {
        testSingle( 10, 0.25, LogarithmicTransform1D.class);         // Logarithmic transform in base 10
        testSingle(  E, 0.25, ConcatenatedTransformDirect1D.class);  // Logarithmic transform in base E
        testSingle(8.4, 0.25, ConcatenatedTransformDirect1D.class);  // Logarithmic transform in base 8.4 (arbitrary base)
    }

    /**
     * Tests the concatenation of a linear operation before the logarithmic one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingleWithOffset")
    public void testAffinePreConcatenation() throws TransformException {
        testAffinePreConcatenation(10);   // Affine + logarithmic transform in base 10
        testAffinePreConcatenation(E);    // Affine + logarithmic transform in base E
        testAffinePreConcatenation(8.4);  // Affine + logarithmic transform in base 8.4 (arbitrary base)
    }

    /**
     * Tests the concatenation of a linear operation after the logarithmic one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingleWithOffset")
    public void testAffinePostConcatenation() throws TransformException {
        testAffinePostConcatenation(10);    // Logarithmic transform in base 10  + affine
        testAffinePostConcatenation(E);     // Logarithmic transform in base E   + affine
        testAffinePostConcatenation(8.4);   // Logarithmic transform in base 8.4 + affine (arbitrary base)
    }

    /**
     * Tests the concatenation of a linear operation before and after the logarithmic one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod({
        "testAffinePreConcatenation",
        "testAffinePostConcatenation"
    })
    public void testAffineConcatenations() throws TransformException {
        testAffineConcatenations(10);   // Affine + logarithmic transform in base 10  + affine
        testAffineConcatenations(E);    // Affine + logarithmic transform in base E   + affine
        testAffineConcatenations(8.4);  // Affine + logarithmic transform in base 8.4 + affine
    }

    /**
     * Tests the concatenation of a logarithmic operation with the exponential one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingleWithOffset")
    public void testLogarithmicConcatenation() throws TransformException {
        transform = MathTransforms.concatenate(
                LogarithmicTransform1D.create(8, C0),
                ExponentialTransform1D.create(10, ExponentialTransform1DTest.SCALE));
        validate();

        final double lnBase = log(8);
        final double[] values = generateRandomCoordinates(CoordinateDomain.RANGE_10, 0);
        final double[] expected = new double[values.length];
        for (int i=0; i<values.length; i++) {
            final double value = abs(values[i]) + 0.001; // Makes the values valid for logarithms.
            values[i] = value;
            expected[i] = ExponentialTransform1DTest.SCALE * pow(10, log(value) / lnBase + C0);
        }
        verifyTransform(values, expected);
    }
}
