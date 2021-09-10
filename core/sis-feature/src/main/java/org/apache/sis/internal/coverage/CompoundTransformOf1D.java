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
package org.apache.sis.internal.coverage;

import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.IterationStrategy;


/**
 * A transform composed of an arbitrary amount of juxtaposed one-dimensional transforms.
 * This is an optimization for a common case when using transforms as transfer functions
 * in grid coverages.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
final class CompoundTransformOf1D extends CompoundTransform {
    /**
     * The transforms to juxtapose for defining a new transform.
     * The length of this array should be greater then 1.
     */
    private final MathTransform1D[] components;

    /**
     * Creates a new compound transforms with the given components.
     */
    CompoundTransformOf1D(final MathTransform1D[] components) {
        this.components = components;
    }

    /**
     * Returns the components of this compound transform.
     * This is a direct reference to internal array; callers shall not modify.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final MathTransform[] components() {
        return components;
    }

    /**
     * Returns the number of source dimensions of this compound transform.
     * This is the sum of the number of source dimensions of all components.
     */
    @Override
    public int getSourceDimensions() {
        return components.length;
    }

    /**
     * Returns the number of target dimensions of this compound transform.
     * This is the sum of the number of target dimensions of all components.
     */
    @Override
    public int getTargetDimensions() {
        return components.length;
    }

    /**
     * Transforms a single coordinate point in an array, and optionally computes the transform derivative
     * at that location.
     */
    @Override
    public Matrix transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, final boolean derivate)
            throws TransformException
    {
        /*
         * If the arrays may overlap, we need to protect the source coordinates
         * before we start writing destination coordinates.
         */
        if (srcPts == dstPts && dstOff > srcOff) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, getSourceDimensions());
            srcPts = dstPts;
            srcOff = dstOff;
        }
        if (!derivate) {
            for (final MathTransform1D c : components) {
                dstPts[dstOff++] = c.transform(srcPts[srcOff++]);
            }
            return null;
        } else {
            final int n = components.length;
            final MatrixSIS m = Matrices.createZero(n, n);
            for (int i=0; i<n; i++) {
                final MathTransform1D c = components[i];
                final double x = srcPts[srcOff++];
                dstPts[dstOff++] = c.transform(x);
                m.setElement(i, i, c.derivative(x));
            }
            return m;
        }
    }

    /**
     * Transforms a list of coordinate points.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        final int n = components.length;
        if (IterationStrategy.suggest(srcOff, n, dstOff, n, numPts) != IterationStrategy.ASCENDING) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * n);
            srcPts = dstPts;
            srcOff = dstOff;
        }
        while (--numPts >= 0) {
            for (final MathTransform1D c : components) {
                dstPts[dstOff++] = c.transform(srcPts[srcOff++]);
            }
        }
    }

    /**
     * Transforms a list of coordinate points.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        final int n = components.length;
        if (IterationStrategy.suggest(srcOff, n, dstOff, n, numPts) != IterationStrategy.ASCENDING) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * n);
            srcPts = dstPts;
            srcOff = dstOff;
        }
        while (--numPts >= 0) {
            for (final MathTransform1D c : components) {
                dstPts[dstOff++] = (float) c.transform(srcPts[srcOff++]);
            }
        }
    }

    /**
     * Transforms a list of coordinate points.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        while (--numPts >= 0) {
            for (final MathTransform1D c : components) {
                dstPts[dstOff++] = (float) c.transform(srcPts[srcOff++]);
            }
        }
    }

    /**
     * Transforms a list of coordinate points.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        while (--numPts >= 0) {
            for (final MathTransform1D c : components) {
                dstPts[dstOff++] = c.transform(srcPts[srcOff++]);
            }
        }
    }
}
