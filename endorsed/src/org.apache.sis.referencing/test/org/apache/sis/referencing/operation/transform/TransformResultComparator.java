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
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;
import static org.opengis.test.Assertions.assertMatrixEquals;


/**
 * Compares the results of two {@link MathTransform} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TransformResultComparator implements MathTransform {
    /**
     * The transform to be used as the reference.
     */
    final MathTransform reference;

    /**
     * The transform to be compared with the {@link #reference} one.
     */
    final MathTransform tested;

    /**
     * The tolerance threshold.
     */
    private final double tolerance;

    /**
     * Creates a transform which will compare the results of the two given transforms.
     */
    TransformResultComparator(final MathTransform reference, final MathTransform tested, final double tolerance) {
        this.reference = reference;
        this.tested    = tested;
        this.tolerance = tolerance;
    }

    /**
     * Delegates to the tested implementation and verifies that the value is equal
     * to the one provided by the reference implementation.
     */
    @Override
    public int getSourceDimensions() {
        final int value = tested.getSourceDimensions();
        assertEquals(reference.getSourceDimensions(), value);
        return value;
    }

    /**
     * Delegates to the tested implementation and verifies that the value is equal
     * to the one provided by the reference implementation.
     */
    @Override
    public int getTargetDimensions() {
        final int value = tested.getTargetDimensions();
        assertEquals(reference.getTargetDimensions(), value);
        return value;
    }

    /**
     * Delegates to the tested implementation and verifies that the value is equal
     * to the one provided by the reference implementation.
     */
    @Override
    public boolean isIdentity() {
        final boolean value = tested.isIdentity();
        assertEquals(reference.isIdentity(), value);
        return value;
    }

    /**
     * Delegates to the tested implementation and verifies that the value is equal
     * to the one provided by the reference implementation.
     */
    @Override
    public Matrix derivative(DirectPosition point) throws MismatchedDimensionException, TransformException {
        final Matrix value = tested.derivative(point);
        assertMatrixEquals(reference.derivative(point), value, tolerance, "derivative");
        return value;
    }

    /**
     * Delegates to the tested implementation and verifies that the value is equal
     * to the one provided by the reference implementation.
     */
    @Override
    public DirectPosition transform(DirectPosition ptSrc, DirectPosition ptDst) throws TransformException {
        final double[] expected = reference.transform(ptSrc, ptDst).getCoordinates();
        final DirectPosition value = tested.transform(ptSrc, ptDst);
        assertArrayEquals(expected, value.getCoordinates(), tolerance);
        return value;
    }

    /**
     * Delegates to the tested implementation and verifies that the values are equal
     * to the ones provided by the reference implementation.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        final double[] expected = new double[numPts * reference.getTargetDimensions()];
        reference.transform(srcPts, srcOff, expected, 0, numPts);
        tested.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        assertArrayEquals(expected, Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts * tested.getTargetDimensions()), tolerance);
    }

    /**
     * Delegates to the tested implementation and verifies that the values are equal
     * to the ones provided by the reference implementation.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        final float[] expected = new float[numPts * reference.getTargetDimensions()];
        reference.transform(srcPts, srcOff, expected, 0, numPts);
        tested.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        assertArrayEquals(expected, Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts * tested.getTargetDimensions()), (float) tolerance);
    }

    /**
     * Delegates to the tested implementation and verifies that the values are equal
     * to the ones provided by the reference implementation.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        final double[] expected = new double[numPts * reference.getTargetDimensions()];
        reference.transform(srcPts, srcOff, expected, 0, numPts);
        tested.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        assertArrayEquals(expected, Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts * tested.getTargetDimensions()), tolerance);
    }

    /**
     * Delegates to the tested implementation and verifies that the values are equal
     * to the ones provided by the reference implementation.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        final float[] expected = new float[numPts * reference.getTargetDimensions()];
        reference.transform(srcPts, srcOff, expected, 0, numPts);
        tested.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        assertArrayEquals(expected, Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts * tested.getTargetDimensions()), (float) tolerance);
    }

    /**
     * Returns the inverse of this transform.
     */
    @Override
    public MathTransform inverse() throws NoninvertibleTransformException {
        return new TransformResultComparator(reference.inverse(), tested.inverse(), tolerance);
    }

    /**
     * Delegates to the tested implementation. No comparison is done by this method.
     */
    @Override
    public String toWKT() {
        return tested.toWKT();
    }

    /**
     * Concatenates two transforms that may be comparators.
     * Instances of {@code TransformResultComparator} are unwrapped before concatenation,
     * then the concatenation result is re-wrapped in {@code TransformResultComparator}.
     *
     * @param  tr1      the first math transform.
     * @param  tr2      the second math transform.
     * @param  factory  the factory which is (indirectly) invoking this method, or {@code null} if none.
     * @return the concatenated transform.
     */
    static MathTransform concatenate(final MathTransform tr1,
                                     final MathTransform tr2,
                                     final MathTransformFactory factory) throws FactoryException
    {
        double tolerance;
        final MathTransform t1, r1, t2, r2;
        if (tr1 instanceof TransformResultComparator c) {
            t1 = c.tested;
            r1 = c.reference;
            tolerance = c.tolerance;
        } else {
            t1 = r1 = tr1;
            tolerance = 0;
        }
        if (tr2 instanceof TransformResultComparator c) {
            t2 = c.tested;
            r2 = c.reference;
            if (c.tolerance > tolerance) {
                tolerance = c.tolerance;
            }
        } else {
            t2 = r2 = tr2;
        }
        final MathTransform tested = ConcatenatedTransform.create(factory, t1, t2);
        if (r1 == t1 && r2 == t2) {
            return tested;
        }
        final MathTransform reference = ConcatenatedTransform.create(factory, r1, r2);
        return new TransformResultComparator(reference, tested, tolerance);
    }
}
