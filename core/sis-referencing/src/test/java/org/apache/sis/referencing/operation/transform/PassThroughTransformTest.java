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

import java.util.List;
import java.util.Arrays;
import java.util.Random;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.util.ArraysExt;

// Test imports
import org.junit.Test;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOn;
import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.test.CalculationType;
import org.opengis.test.ToleranceModifier;


/**
 * Tests {@link PassThroughTransform}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.5
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
     * Verifies argument validation performed by {@link MathTransforms#passThrough(int, MathTransform, int)}.
     */
    @Test
    public void testIllegalArgument() {
        final MathTransform subTransform = MathTransforms.identity(1);
        try {
            MathTransforms.passThrough(-1, subTransform, 0);
            fail("An illegal argument should have been detected");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("firstAffectedCoordinate"));
        }
        try {
            MathTransforms.passThrough(0, subTransform, -1);
            fail("An illegal argument should have been detected");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("numTrailingCoordinates"));
        }
    }

    /**
     * Tests the pass through transform using an identity transform.
     * The "pass-through" of such transform shall be itself the identity transform.
     *
     * @throws TransformException should never happen.
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
     * @throws TransformException should never happen.
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
     * @throws TransformException should never happen.
     */
    @Test
    public void testPassthrough() throws TransformException {
        runTest(ExponentialTransform1D.create(10, 2), PassThroughTransform.class);
    }

    /**
     * Tests the general pass-through transform with a sub-transform going from 3D to 2D points.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testDimensionDecrease() throws TransformException {
        isInverseTransformSupported = false;
        runTest(new PseudoTransform(3, 2), PassThroughTransform.class);
    }

    /**
     * Tests the general pass-through transform with a sub-transform going from 2D to 3D points.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testDimensionIncrease() throws TransformException {
        isInverseTransformSupported = false;
        runTest(new PseudoTransform(2, 3), PassThroughTransform.class);
    }

    /**
     * Tests a pass-through transform built using the given sub-transform.
     *
     * @param  subTransform   the sub-transform to use for building pass-through transform.
     * @param  expectedClass  the expected implementation class of pass-through transforms.
     * @throws TransformException if a transform failed.
     */
    private void runTest(final MathTransform subTransform, final Class<? extends MathTransform> expectedClass)
            throws TransformException
    {
        random = TestUtilities.createRandomNumberGenerator();
        /*
         * Test many combinations of "first affected coordinate" and "number of trailing coordinates" parameters.
         * For each combination we create a passthrough transform, test it with the 'verifyTransform' method.
         */
        for (int firstAffectedCoordinate=0; firstAffectedCoordinate<=3; firstAffectedCoordinate++) {
            for (int numTrailingCoordinates=0; numTrailingCoordinates<=3; numTrailingCoordinates++) {
                final int numAdditionalOrdinates = firstAffectedCoordinate + numTrailingCoordinates;
                transform = MathTransforms.passThrough(firstAffectedCoordinate, subTransform, numTrailingCoordinates);
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
                verifyTransform(subTransform, firstAffectedCoordinate);
            }
        }
    }

    /**
     * Tests the current {@linkplain #transform transform} using an array of random coordinate values,
     * and compares the result against the expected ones. This method computes itself the expected results.
     *
     * @param  subTransform           the sub transform used by the current pass-through transform.
     * @param  firstAffectedCoordinate  first input/output dimension used by {@code subTransform}.
     * @throws TransformException if a transform failed.
     */
    private void verifyTransform(final MathTransform subTransform, final int firstAffectedCoordinate) throws TransformException {
        validate();
        /*
         * Prepare two arrays:
         *   - passthrough data, to be given to the transform to be tested.
         *   - sub-transform data, which we will use internally for verifying the pass-through work.
         */
        final int      sourceDim        = transform.getSourceDimensions();
        final int      targetDim        = transform.getTargetDimensions();
        final int      subSrcDim        = subTransform.getSourceDimensions();
        final int      subTgtDim        = subTransform.getTargetDimensions();
        final int      numPts           = ORDINATE_COUNT / sourceDim;
        final double[] passthroughData  = CoordinateDomain.RANGE_10.generateRandomInput(random, sourceDim, numPts);
        final double[] subTransformData = new double[numPts * StrictMath.max(subSrcDim, subTgtDim)];
        Arrays.fill(subTransformData, Double.NaN);
        for (int i=0; i<numPts; i++) {
            System.arraycopy(passthroughData, firstAffectedCoordinate + i*sourceDim,
                             subTransformData, i*subSrcDim, subSrcDim);
        }
        subTransform.transform(subTransformData, 0, subTransformData, 0, numPts);
        assertFalse(ArraysExt.hasNaN(subTransformData));
        /*
         * Build the array of expected data by copying ourself the sub-transform results.
         */
        final int numTrailingCoordinates = targetDim - subTgtDim - firstAffectedCoordinate;
        final double[] expectedData = new double[targetDim * numPts];
        for (int i=0; i<numPts; i++) {
            int srcOffset = i * sourceDim;
            int dstOffset = i * targetDim;
            final int s = firstAffectedCoordinate + subSrcDim;
            System.arraycopy(passthroughData,  srcOffset,   expectedData, dstOffset,   firstAffectedCoordinate);
            System.arraycopy(subTransformData, i*subTgtDim, expectedData, dstOffset += firstAffectedCoordinate, subTgtDim);
            System.arraycopy(passthroughData,  srcOffset+s, expectedData, dstOffset + subTgtDim, numTrailingCoordinates);
        }
        assertEquals(subTransform.isIdentity(), Arrays.equals(passthroughData, expectedData));
        /*
         * Now process to the transform and compares the results with the expected ones.
         */
        tolerance         = 0;          // Results should be strictly identical because we used the same inputs.
        toleranceModifier = null;
        final double[] transformedData = new double[StrictMath.max(sourceDim, targetDim) * numPts];
        transform.transform(passthroughData, 0, transformedData, 0, numPts);
        assertCoordinatesEqual("PassThroughTransform results do not match the results computed by this test.",
                targetDim, expectedData, 0, transformedData, 0, numPts, CalculationType.DIRECT_TRANSFORM);
        /*
         * Test inverse transform.
         */
        if (isInverseTransformSupported) {
            tolerance         = 1E-8;
            toleranceModifier = ToleranceModifier.RELATIVE;
            Arrays.fill(transformedData, Double.NaN);
            transform.inverse().transform(expectedData, 0, transformedData, 0, numPts);
            assertCoordinatesEqual("Inverse of PassThroughTransform do not give back the original data.",
                    sourceDim, passthroughData, 0, transformedData, 0, numPts, CalculationType.INVERSE_TRANSFORM);
        }
        /*
         * Verify the consistency between different 'transform(…)' methods.
         */
        final float[] sourceAsFloat = ArraysExt.copyAsFloats(passthroughData);
        final float[] targetAsFloat = verifyConsistency(sourceAsFloat);
        assertEquals("Unexpected length of transformed array.", expectedData.length, targetAsFloat.length);
        /*
         * We use a relatively high tolerance threshold because result are
         * computed using inputs stored as float values.
         */
        if (transform instanceof LinearTransform) {
            tolerance         = 1E-4;
            toleranceModifier = ToleranceModifier.RELATIVE;
            assertCoordinatesEqual("PassThroughTransform.transform(…) variants produce inconsistent results.",
                    sourceDim, expectedData, 0, targetAsFloat, 0, numPts, CalculationType.DIRECT_TRANSFORM);
        }
    }

    /**
     * Tests {@link PassThroughTransform#tryConcatenate(boolean, MathTransform, MathTransformFactory)}.
     * This tests creates a non-linear transform of 6→7 dimensions, then applies a filter keeping only
     * target dimensions 1, 4 and 6 (corresponding to source dimensions 1 and 5).
     *
     * @throws FactoryException if an error occurred while combining the transforms.
     */
    @Test
    public void testTryConcatenate() throws FactoryException {
        PassThroughTransform ps = new PassThroughTransform(2, new PseudoTransform(2, 3), 2);
        MathTransform c = ps.tryConcatenate(false, MathTransforms.linear(Matrices.create(4, 8, new double[] {
                0, 1, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 1, 0,
                0, 0, 0, 0, 0, 0, 0, 1})), null);

        final List<MathTransform> steps = MathTransforms.getSteps(c);
        assertEquals("Number of steps", 3, steps.size());
        /*
         * We need to remove source dimensions 0, 2, 3 and 4. We can not remove dimensions 2 and 3 before
         * pass-through because they are used by the sub-transform. It leaves us dimensions 0 and 4 which
         * can be removed here.
         */
        assertMatrixEquals("Expected removal of dimensions 0 and 4 before pass-through", Matrices.create(5, 7, new double[] {
                0, 1, 0, 0, 0, 0, 0,
                0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 1, 0,
                0, 0, 0, 0, 0, 0, 1}), MathTransforms.getMatrix(steps.get(0)), null);
        /*
         * The number of pass-through dimensions have decreased from 2 to 1 on both sides of the sub-transform.
         */
        final PassThroughTransform reduced = (PassThroughTransform) steps.get(1);
        assertEquals("firstAffectedCoordinate", 1, reduced.firstAffectedCoordinate);
        assertEquals("numTrailingCoordinates",  1, reduced.numTrailingCoordinates);
        assertSame  ("subTransform", ps.subTransform, reduced.subTransform);
        /*
         * We still have to remove source dimensions 2 and 3. Since we removed dimension 0 in previous step,
         * the indices of dimensions to removed have shifted to 1 and 2.
         */
        assertMatrixEquals("Expected removal of dimensions 1 and 2 after pass-through", Matrices.create(4, 6, new double[] {
                1, 0, 0, 0, 0, 0,
                0, 0, 0, 1, 0, 0,
                0, 0, 0, 0, 1, 0,
                0, 0, 0, 0, 0, 1}), MathTransforms.getMatrix(steps.get(2)), null);
    }
}
