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

import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.operation.provider.Affine;
import org.apache.sis.referencing.operation.matrix.Matrices;


/**
 * The identity transform. The data are only copied without any transformation. Instance of this
 * class are created for identity transform of dimension greater than 2. For 1D and 2D identity
 * transforms, {@link LinearTransform1D} and {@link AffineTransform2D} already provide their own
 * optimizations.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5 (derived from geotk-2.0)
 * @version 0.5
 * @module
 */
final class IdentityTransform extends AbstractMathTransform implements LinearTransform, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5339040282922138164L;

    /**
     * Identity transforms for dimensions ranging from to 0 to 7.
     * Elements in this array will be created only when first requested.
     *
     * @see #identity(int)
     */
    private static final LinearTransform[] IDENTITIES = new LinearTransform[8];

    /**
     * The input and output dimension.
     */
    private final int dimension;

    /**
     * Constructs an identity transform of the specified dimension.
     *
     * @param dimension The dimension of the transform to be created.
     *
     * @see MathTransforms#identity(int)
     */
    private IdentityTransform(final int dimension) {
        this.dimension = dimension;
    }

    /**
     * Returns an identity transform of the specified dimension. In the special case of
     * dimension 1 and 2, this method returns instances of {@link LinearTransform1D} or
     * {@link AffineTransform2D} respectively.
     *
     * @param dimension The dimension of the transform to be returned.
     * @return An identity transform of the specified dimension.
     *
     * @see MathTransforms#identity(int)
     */
    public static LinearTransform create(final int dimension) {
        LinearTransform candidate;
        synchronized (IDENTITIES) {
            if (dimension < IDENTITIES.length) {
                candidate = IDENTITIES[dimension];
                if (candidate != null) {
                    return candidate;
                }
            }
            switch (dimension) {
                case 1:  candidate = IdentityTransform1D.INSTANCE;     break;
                case 2:  candidate = new AffineTransform2D();          break;
                default: candidate = new IdentityTransform(dimension); break;
            }
            if (dimension < IDENTITIES.length) {
                IDENTITIES[dimension] = candidate;
            }
        }
        return candidate;
    }

    /**
     * Returns {@code true} since this transform does not move any points.
     */
    @Override
    public boolean isIdentity() {
        return true;
    }

    /**
     * Gets the dimension of input points.
     */
    @Override
    public int getSourceDimensions() {
        return dimension;
    }

    /**
     * Gets the dimension of output points.
     */
    @Override
    public int getTargetDimensions() {
        return dimension;
    }

    /**
     * Returns the parameter descriptors for this math transform.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Affine.PARAMETERS;
    }

    /**
     * Returns the matrix elements as a group of parameters values.
     *
     * @return A copy of the parameter values for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return ProjectiveTransform.getParameterValues(getMatrix());
    }

    /**
     * Returns a copy of the identity matrix.
     */
    @Override
    public Matrix getMatrix() {
        return Matrices.createIdentity(dimension + 1);
    }

    /**
     * Gets the derivative of this transform at a point. For an identity transform,
     * the derivative is the same everywhere.
     */
    @Override
    public Matrix derivative(final DirectPosition point) {
        return Matrices.createIdentity(dimension);
    }

    /**
     * Copies the values from {@code ptSrc} to {@code ptDst}.
     * Overrides the super-class method for performance reason.
     */
    @Override
    public DirectPosition transform(final DirectPosition ptSrc, final DirectPosition ptDst) {
        ArgumentChecks.ensureDimensionMatches("ptSrc", dimension, ptSrc);
        if (ptDst == null) {
            return new GeneralDirectPosition(ptSrc);
        }
        ArgumentChecks.ensureDimensionMatches("ptDst", dimension, ptDst);
        for (int i=0; i<dimension; i++) {
            ptDst.setOrdinate(i, ptSrc.getOrdinate(i));
        }
        return ptDst;
    }

    /**
     * Transforms a single coordinate in a list of ordinal values,
     * and optionally returns the derivative at that location.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate)
    {
        if (dstPts != null) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, dimension);
        }
        return derivate ? derivative((DirectPosition) null) : null;
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
    {
        System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * dimension);
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff,
                          final float[] dstPts, int dstOff, int numPts)
    {
        System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * dimension);
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final float [] dstPts, int dstOff, int numPts)
    {
        numPts *= dimension;
        while (--numPts >= 0) {
            dstPts[dstOff++] = (float) srcPts[srcOff++];
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final float [] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
    {
        numPts *= dimension;
        while (--numPts >= 0) {
            dstPts[dstOff++] = srcPts[srcOff++];
        }
    }

    /**
     * Returns the inverse transform of this object, which is this transform itself
     */
    @Override
    public MathTransform inverse() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) { // Slight optimization
            return true;
        }
        if (mode != ComparisonMode.STRICT) {
            return equals(this, object, mode);
        }
        if (super.equals(object, mode)) {
            final IdentityTransform that = (IdentityTransform) object;
            return this.dimension == that.dimension;
        }
        return false;
    }
}
