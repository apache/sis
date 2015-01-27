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

import java.util.Arrays;
import java.util.Random;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.ArraysExt;

// Test imports
import org.junit.Test;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOn;
import static org.junit.Assert.*;

// Branch-dependent imports
// (all imports removed)


/**
 * Tests {@link PassThroughTransform}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn({
    CoordinateDomainTest.class,
    LinearTransformTest.class,
    ExponentialTransform1DTest.class
})
public final strictfp class PassThroughTransformTest extends MathTransformTestCase {
    /**
     * The random number generator to be used in this test.
     */
    private Random random;

    /**
     * Verifies argument validation performed by {@link PassThroughTransform#create(int, MathTransform, int)}.
     */
    @Test
    public void testIllegalArgument() {
        final MathTransform subTransform = MathTransforms.identity(1);
        try {
            PassThroughTransform.create(-1, subTransform, 0);
            fail("An illegal argument should have been detected");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("firstAffectedOrdinate"));
        }
        try {
            PassThroughTransform.create(0, subTransform, -1);
            fail("An illegal argument should have been detected");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("numTrailingOrdinates"));
        }

    }

    /**
     * Tests the pass through transform using an identity transform.
     * The "pass-through" of such transform shall be itself the identity transform.
     *
     * @throws TransformException Should never happen.
     */
    @Test
    public void testIdentity() throws TransformException {
        final Matrix matrix = new Matrix3();
        runTest(MathTransforms.linear(matrix), IdentityTransform.class);
    }

    /**
     * Tests the pass-through transform using an affine transform.
     * The "pass-through" of such transforms are optimized using matrix arithmetic.
     *
     * @throws TransformException Should never happen.
     */
    @Test
    public void testLinear() throws TransformException {
        final Matrix matrix = new Matrix3(
                4, 0, 0,
                0, 3, 0,
                0, 0, 1);
        runTest(MathTransforms.linear(matrix), LinearTransform.class);
    }

    /**
     * Tests the general pass-through transform.
     * This test uses a non-linear sub-transform for preventing the factory method to optimize.
     *
     * @throws TransformException Should never happen.
     */
    @Test
    public void testPassthrough() throws TransformException {
        runTest(ExponentialTransform1D.create(10, 2), PassThroughTransform.class);
    }

    /**
     * Tests a pass-through transform built using the given sub-transform.
     *
     * @param  subTransform  The sub-transform to use for building pass-through transform.
     * @param  expectedClass The expected implementation class of pass-through transforms.
     * @throws TransformException If a transform failed.
     */
    private void runTest(final MathTransform subTransform, final Class<? extends MathTransform> expectedClass)
            throws TransformException
    {
        random = TestUtilities.createRandomNumberGenerator();
        /*
         * Test many combinations of "first affected ordinate" and "number of trailing ordinates" parameters.
         * For each combination we create a passthrough transform, test it with the 'verifyTransform' method.
         */
        for (int firstAffectedOrdinate=0; firstAffectedOrdinate<=3; firstAffectedOrdinate++) {
            for (int numTrailingOrdinates=0; numTrailingOrdinates<=3; numTrailingOrdinates++) {
                final int numAdditionalOrdinates = firstAffectedOrdinate + numTrailingOrdinates;
                transform = PassThroughTransform.create(firstAffectedOrdinate, subTransform, numTrailingOrdinates);
                if (numAdditionalOrdinates == 0) {
                    assertSame("Failed to recognize that no passthrough was needed.", subTransform, transform);
                    continue;
                }
                assertNotSame(subTransform, transform);
                assertTrue   ("Wrong transform class.", expectedClass.isInstance(transform));
                assertEquals ("Wrong number of source dimensions.",
                        subTransform.getSourceDimensions() + numAdditionalOrdinates, transform.getSourceDimensions());
                assertEquals ("Wrong number of target dimensions.",
                        subTransform.getTargetDimensions() + numAdditionalOrdinates, transform.getTargetDimensions());
                verifyTransform(subTransform, firstAffectedOrdinate);
            }
        }
    }

    /**
     * Tests the current {@linkplain #transform transform} using an array of random coordinate values,
     * and compares the result against the expected ones. This method computes itself the expected results.
     *
     * @param  subTransform The sub transform used by the current pass-through transform.
     * @param  firstAffectedOrdinate First input/output dimension used by {@code subTransform}.
     * @throws TransformException If a transform failed.
     */
    private void verifyTransform(final MathTransform subTransform, final int firstAffectedOrdinate)
            throws TransformException
    {
        validate();
        /*
         * Prepare two arrays:
         *   - passthrough data, to be given to the transform to be tested.
         *   - sub-transform data, which we will use internally for verifying the pass-through work.
         */
        final int      passthroughDim   = transform.getSourceDimensions();
        final int      subTransformDim  = subTransform.getSourceDimensions();
        final int      numPts           = ORDINATE_COUNT / passthroughDim;
        final double[] passthroughData  = CoordinateDomain.RANGE_10.generateRandomInput(random, passthroughDim, numPts);
        final double[] subTransformData = new double[numPts * subTransformDim];
        Arrays.fill(subTransformData, Double.NaN);
        for (int i=0; i<numPts; i++) {
            System.arraycopy(passthroughData, firstAffectedOrdinate + i*passthroughDim,
                             subTransformData, i*subTransformDim, subTransformDim);
        }
        assertFalse("Error building test arrays.", ArraysExt.hasNaN(subTransformData));
        /*
         * Build the array of expected data by applying ourself the sub-transform.
         */
        subTransform.transform(subTransformData, 0, subTransformData, 0, numPts);
        final double[] expectedData = passthroughData.clone();
        for (int i=0; i<numPts; i++) {
            System.arraycopy(subTransformData, i*subTransformDim, expectedData,
                             firstAffectedOrdinate + i*passthroughDim, subTransformDim);
        }
        assertEquals("Error building test arrays.", subTransform.isIdentity(),
                Arrays.equals(passthroughData, expectedData));
        /*
         * Now process to the transform and compares the results with the expected ones.
         */
        tolerance = 0; // Results should be strictly identical because we used the same inputs.
        final double[] transformedData = new double[expectedData.length];
        transform.transform(passthroughData, 0, transformedData, 0, numPts);
        assertCoordinatesEqual("Direct transform.", passthroughDim,
                expectedData, 0, transformedData, 0, numPts, false);
        /*
         * Test inverse transform.
         */
        tolerance = 1E-8;
        Arrays.fill(transformedData, Double.NaN);
        transform.inverse().transform(expectedData, 0, transformedData, 0, numPts);
        assertCoordinatesEqual("Inverse transform.", passthroughDim,
                passthroughData, 0, transformedData, 0, numPts, false);
        /*
         * Verify the consistency between different 'transform(…)' methods.
         */
        final float[] sourceAsFloat = Numerics.copyAsFloats(passthroughData);
        final float[] targetAsFloat = verifyConsistency(sourceAsFloat);
        assertEquals("Unexpected length of transformed array.", expectedData.length, targetAsFloat.length);
    }
}
