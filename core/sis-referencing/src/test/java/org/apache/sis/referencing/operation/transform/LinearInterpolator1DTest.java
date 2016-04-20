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

import java.util.Random;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Test {@link LinearInterpolator1D} class.
 *
 * @author  Remi Marechal (Geomatys).
 * @author  Martin Desruisseaux (Geomatys).
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class LinearInterpolator1DTest extends MathTransformTestCase {
    /**
     * The values of the <i>y=f(x)</i> function to test.
     */
    private double[] preimage, values;

    /**
     * Creates a new test case.
     */
    public LinearInterpolator1DTest() {
    }

    /**
     * Tests <var>x</var> values equal to indices and <var>y</var> values in increasing order.
     *
     * @throws TransformException if an error occurred while testing a value.
     */
    @Test
    public void testIndicesToIncreasingValues() throws TransformException {
        preimage = new double[] { 0,  1,  2,  3};
        values   = new double[] {10, 12, 16, 22};
        verifyConsistency(-2, 5, -5196528645359952958L);
        assertInstanceOf("Expected y=f(i)", LinearInterpolator1D.class, transform);
    }

    /**
     * Tests <var>x</var> values equal to indices and <var>y</var> values in decreasing order.
     *
     * @throws TransformException if an error occurred while testing a value.
     */
    @Test
    public void testIndicesToDecreasingValues() throws TransformException {
        preimage = new double[] {0,   1,  2, 3};
        values   = new double[] {35, 27, 22, 5};
        verifyConsistency(-2, 5, 6445394511592290678L);
        assertInstanceOf("Expected y = -f(-i)", ConcatenatedTransformDirect1D.class, transform);
        assertInstanceOf("Expected y = -f(-i)", LinearInterpolator1D.class, ((ConcatenatedTransform) transform).transform1);
        assertSame      ("Expected y = -f(-i)", LinearTransform1D.NEGATE,   ((ConcatenatedTransform) transform).transform2);
    }

    /**
     * Tests increasing <var>x</var> values to <var>y</var> values that are equal to indices.
     *
     * @throws TransformException if an error occurred while testing a value.
     */
    @Test
    public void testIncreasingInputsToIndices() throws TransformException {
        preimage = new double[] {10, 12, 16, 22};
        values   = new double[] { 0,  1,  2,  3};
        verifyConsistency(0, 30, 6130776597146077588L);
    }

    /**
     * Tests decreasing <var>x</var> values to <var>y</var> values that are equal to indices.
     *
     * @throws TransformException if an error occurred while testing a value.
     */
    @Test
    public void testDecreasingInputsToIndices() throws TransformException {
        preimage = new double[] {35, 27, 22, 5};
        values   = new double[] {0,   1,  2, 3};
        verifyConsistency(0, 40, 4109281798631024654L);
        assertInstanceOf("Expected i = -f(-x)", ConcatenatedTransformDirect1D.class, transform);
    }

    /**
     * Tests increasing <var>x</var> values to increasing <var>y</var> values.
     *
     * @throws TransformException if an error occurred while testing a value.
     */
    @Test
    public void testIncreasingInputsToIncreasingValues() throws TransformException {
        preimage = new double[] { -207, -96, -5,   2};
        values   = new double[] {  -50, -20,  7, 105};
        verifyConsistency(-210, 5, 1941178068603334535L);
        assertInstanceOf("Expected y = f(x)", ConcatenatedTransformDirect1D.class, transform);
        assertInstanceOf("Expected y = f(x)", LinearInterpolator1D.class, ((ConcatenatedTransform) transform).transform2);
    }

    /**
     * Tests decreasing <var>x</var> values to increasing <var>y</var> values.
     *
     * @throws TransformException if an error occurred while testing a value.
     */
    @Test
    public void testDecreasingInputsToIncreasingValues() throws TransformException {
        preimage = new double[] {  2,  -5, -96, -207};
        values   = new double[] {-50, -20,  7,   105};
        verifyConsistency(-210, 5, 7360962930883142147L);
        assertInstanceOf("Expected y = -f(-x)", ConcatenatedTransformDirect1D.class, transform);
    }

    /**
     * Tests decreasing <var>x</var> values to decreasing <var>y</var> values.
     *
     * @throws TransformException if an error occurred while testing a value.
     */
    @Test
    public void testDecreasingInputsToDecreasingValues() throws TransformException {
        preimage = new double[] {  2, -5, -96, -207};
        values   = new double[] {105,  7, -19,  -43};
        verifyConsistency(-210, 5, -2463171263749789198L);
        assertInstanceOf("Expected y = -f(-x)", ConcatenatedTransformDirect1D.class, transform);
    }

    /**
     * Tests increasing <var>x</var> values to non-monotonic <var>y</var> values.
     *
     * @throws TransformException if an error occurred while testing a value.
     */
    @Test
    public void testIncreasingInputsToNonMonotonic() throws TransformException {
        preimage = new double[] {-52, -27, -13,   2};
        values   = new double[] {105, -19,   7, -43};
        isInverseTransformSupported = false;
        verifyConsistency(-60, 5, 7750310847358135291L);
    }

    /**
     * Tests decreasing <var>x</var> values to non-monotonic <var>y</var> values.
     *
     * @throws TransformException if an error occurred while testing a value.
     */
    @Test
    public void testDecreasingInputsToNonMonotonic() throws TransformException {
        preimage = new double[] {1017, 525,  24,  12};
        values   = new double[] { -43,   7, -19, 105};
        isInverseTransformSupported = false;
        verifyConsistency(0, 1020, 2060810396521686858L);
    }

    /**
     * Tests increasing <var>x</var> values to non-monotonic <var>y</var> values.
     *
     * @throws TransformException if an error occurred while testing a value.
     */
    @Test
    public void testIncreasingInputsToPercent() throws TransformException {
        preimage = new double[] {  5, 6.5,  8, 10, 25, 28, 30,  32};
        values   = new double[] {100,  66, 33,  0,  0, 33, 66, 100};
        isInverseTransformSupported = false;
        verifyConsistency(0, 40, -6588291548545974041L);
    }

    /**
     * Verifies that the factory method does not accept invalid arguments.
     */
    @Test
    public void testArgumentChecks() {
        preimage = new double[] { -43,   7, -19, 105};                         // Non-monotonic sequence.
        values   = new double[] {1017, 525,  24,  12};
        try {
            LinearInterpolator1D.create(preimage, values);
            fail("Should not have accepted the x inputs.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("preimage"));
        }

        preimage = new double[] {1017, 525,  24,  12};
        values   = new double[] {-43,    7, -19, 105};
        MathTransform1D mt = LinearInterpolator1D.create(preimage, values);
        try {
            mt.inverse();
            fail("Should not have accepted the inverse that transform.");
        } catch (NoninvertibleTransformException e) {
            final String message = e.getMessage();
            assertFalse(message, message.isEmpty());
        }

        preimage = new double[] {1017, 525,  24,  12, 45};                     // Mismatched array length.
        values   = new double[] {-43,    7, -19, 105};
        try {
            LinearInterpolator1D.create(preimage, values);
            fail("Should not have accepted the x inputs.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertFalse(message, message.isEmpty());
        }
    }

    /**
     * Transforms point and verifies that the result is consistent with the inverse transform and the derivative.
     */
    private void verifyConsistency(final double min, final double max, final long randomSeed) throws TransformException {
        transform = LinearInterpolator1D.create(preimage, values);
        tolerance = 1E-10;
        derivativeDeltas = new double[] {0.1};
        /*
         * Convert a x value to y value, then back to x.
         * This code is provided mostly as a convenience place where to step into with a debugger.
         */
        if (isInverseTransformSupported) {
            final double xm = (min + max) / 2;
            final double ym = ((MathTransform1D) transform).transform(xm);
            assertEquals(xm,  ((MathTransform1D) transform.inverse()).transform(ym), tolerance);
        }
        /*
         * The actual test: 100 random values, test all transform methods
         * (including those working on arrays), verify consistency and derivatives.
         */
        verifyInDomain(new double[] {min}, new double[] {max}, new int[] {100}, new Random(randomSeed));
    }
}
