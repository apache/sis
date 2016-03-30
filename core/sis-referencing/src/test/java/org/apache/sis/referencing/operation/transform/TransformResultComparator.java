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
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import static org.apache.sis.test.Assert.*;


/**
 * Compares the results of two {@link MathTransform} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final strictfp class TransformResultComparator implements MathTransform {
    /**
     * The transform to be used as the reference.
     */
    private final MathTransform reference;

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
     * Delegates to the tested implementation and verifies that the value is equals
     * to the one provided by the reference implementation.
     */
    @Override
    public int getSourceDimensions() {
        final int value = tested.getSourceDimensions();
        assertEquals("sourceDimensions", reference.getSourceDimensions(), value);
        return value;
    }

    /**
     * Delegates to the tested implementation and verifies that the value is equals
     * to the one provided by the reference implementation.
     */
    @Override
    public int getTargetDimensions() {
        final int value = tested.getTargetDimensions();
        assertEquals("targetDimensions", reference.getTargetDimensions(), value);
        return value;
    }

    /**
     * Delegates to the tested implementation and verifies that the value is equals
     * to the one provided by the reference implementation.
     */
    @Override
    public boolean isIdentity() {
        final boolean value = tested.isIdentity();
        assertEquals("isIdentity", reference.isIdentity(), value);
        return value;
    }

    /**
     * Delegates to the tested implementation and verifies that the value is equals
     * to the one provided by the reference implementation.
     */
    @Override
    public Matrix derivative(DirectPosition point) throws MismatchedDimensionException, TransformException {
        final Matrix value = tested.derivative(point);
        assertMatrixEquals("derivative", reference.derivative(point), value, tolerance);
        return value;
    }

    /**
     * Delegates to the tested implementation and verifies that the value is equals
     * to the one provided by the reference implementation.
     */
    @Override
    public DirectPosition transform(DirectPosition ptSrc, DirectPosition ptDst) throws TransformException {
        final double[] expected = reference.transform(ptSrc, ptDst).getCoordinate();
        final DirectPosition value = tested.transform(ptSrc, ptDst);
        assertArrayEquals("transform(DirectPosition, …)", expected, value.getCoordinate(), tolerance);
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
        assertArrayEquals("transform(double[], …, double[], …)", expected,
                Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts * tested.getTargetDimensions()), tolerance);
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
        assertArrayEquals("transform(float[], …, float[], …)", expected,
                Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts * tested.getTargetDimensions()), (float) tolerance);
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
        assertArrayEquals("transform(float[], …, double[], …)", expected,
                Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts * tested.getTargetDimensions()), tolerance);
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
        assertArrayEquals("transform(double[], …, float[], …)", expected,
                Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts * tested.getTargetDimensions()), (float) tolerance);
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
}
