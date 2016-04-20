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
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix1;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.ComparisonMode;

// We really want to use doubleToRawLongBits, not doubleToLongBits, because the
// coverage module needs the raw bits for differentiating various NaN values.
import static java.lang.Double.doubleToRawLongBits;


/**
 * A one dimensional, linear transform.
 * Input values <var>x</var> are converted into output values <var>y</var> using the following equation:
 *
 * <blockquote><var>y</var>  =  <var>x</var> × {@linkplain #scale} + {@linkplain #offset}</blockquote>
 *
 * This class is the same as a 2×2 affine transform. However, this specialized {@code LinearTransform1D} class
 * is faster. This kind of transform is extensively used by {@link org.apache.sis.coverage.grid.GridCoverage2D}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 *
 * @see LogarithmicTransform1D
 * @see ExponentialTransform1D
 */
class LinearTransform1D extends AbstractMathTransform1D implements LinearTransform, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7595037195668813000L;

    /**
     * A transform that just reverse the sign of input values.
     */
    static final LinearTransform1D NEGATE = new LinearTransform1D(-1, 0);

    /**
     * The value which is multiplied to input values.
     */
    final double scale;

    /**
     * The value to add to input values.
     */
    final double offset;

    /**
     * The inverse of this transform. Created only when first needed.
     */
    private transient MathTransform1D inverse;

    /**
     * Constructs a new linear transform. This constructor is provided for subclasses only.
     * Instances should be created using the {@linkplain #create(double, double) factory method},
     * which may returns optimized implementations for some particular argument values.
     *
     * @param scale  The {@code scale}  term in the linear equation.
     * @param offset The {@code offset} term in the linear equation.
     *
     * @see #create(double, double)
     */
    protected LinearTransform1D(final double scale, final double offset) {
        this.scale  = scale;
        this.offset = offset;
    }

    /**
     * Constructs a new linear transform.
     *
     * @param  scale  The {@code scale}  term in the linear equation.
     * @param  offset The {@code offset} term in the linear equation.
     * @return The linear transform for the given scale and offset.
     *
     * @see MathTransforms#linear(double, double)
     */
    public static LinearTransform1D create(final double scale, final double offset) {
        if (offset == 0) {
            if (scale == +1) return IdentityTransform1D.INSTANCE;
            if (scale == -1) return NEGATE;
        }
        if (scale == 0) {
            if (offset == 0) return ConstantTransform1D.ZERO;
            if (offset == 1) return ConstantTransform1D.ONE;
            return new ConstantTransform1D(offset);
        }
        return new LinearTransform1D(scale, offset);
    }

    /**
     * Creates a constant function having value <var>y</var>, and for which the inverse is <var>x</var>.
     *
     * @since 0.7
     */
    static LinearTransform1D constant(final double x, final double y) {
        final LinearTransform1D tr = create(0, y);
        if (!Double.isNaN(x)) {
            tr.inverse = create(0, x);
        }
        return tr;
    }

    /**
     * Returns the parameter descriptors for this math transform.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Affine.getProvider(1, 1, true).getParameters();
    }

    /**
     * Returns the matrix elements as a group of parameters values. The number of parameters
     * depends on the matrix size. Only matrix elements different from their default value
     * will be included in this group.
     *
     * @return The parameter values for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return Affine.parameters(getMatrix());
    }

    /**
     * Returns this transform as an affine transform matrix.
     */
    @Override
    public Matrix getMatrix() {
        return new Matrix2(scale, offset, 0, 1);
    }

    /**
     * Creates the inverse transform of this object.
     */
    @Override
    public LinearTransform1D inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            /*
             * Note: we do not perform the following optimization, because MathTransforms.linear(…)
             *       should never instantiate this class in the identity case.
             *
             *       if (isIdentity()) {
             *           inverse = this;
             *       } else { ... }
             */
            if (scale != 0) {
                final LinearTransform1D inverse;
                inverse = create(1/scale, -offset/scale);
                inverse.inverse = this;
                this.inverse = inverse;
            } else {
                inverse = super.inverse();      // Throws NoninvertibleTransformException
            }
        }
        return (LinearTransform1D) inverse;
    }

    /**
     * Returns {@code true} since this transform is affine.
     */
    @Override
    public boolean isAffine() {
        return true;
    }

    /**
     * Tests whether this transform does not move any points.
     *
     * <div class="note"><b>Note:</b> this method should always returns {@code false}, since
     * {@code MathTransforms.linear(…)} should have created specialized implementations for identity cases.
     * Nevertheless we perform the full check as a safety, in case someone instantiated this class directly
     * instead than using a factory method.</div>
     */
    @Override
    public boolean isIdentity() {
       return offset == 0 && scale == 1;
    }

    /**
     * Gets the derivative of this transform at a point.
     *
     * @param point Ignored for a linear transform. Can be null.
     * @return The derivative at the given point.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        return new Matrix1(scale);
    }

    /**
     * Gets the derivative of this function at a value.
     *
     * @param  value Ignored for a linear transform. Can be {@link Double#NaN NaN}.
     * @return The derivative at the given point.
     */
    @Override
    public double derivative(final double value) {
        return scale;
    }

    /**
     * Transforms the specified value.
     */
    @Override
    public double transform(double value) {
        return offset + scale * value;
    }

    /**
     * Transforms a single point in the given array and opportunistically computes its derivative
     * if requested. The default implementation computes all those values from the {@link #scale}
     * and {@link #offset} coefficients.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate)
    {
        if (dstPts != null) {
            dstPts[dstOff] = offset + scale*srcPts[srcOff];
        }
        return derivate ? new Matrix1(scale) : null;
    }

    /**
     * Transforms many coordinates in a list of ordinal values. The default implementation
     * computes the values from the {@link #scale} and {@link #offset} coefficients.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
    {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = offset + scale * srcPts[srcOff++];
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = offset + scale * srcPts[--srcOff];
            }
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values. The default implementation
     * computes the values from the {@link #scale} and {@link #offset} coefficients using
     * the {@code double} precision, then casts the result to the {@code float} type.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff,
                          final float[] dstPts, int dstOff, int numPts)
    {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (offset + scale * srcPts[srcOff++]);
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = (float) (offset + scale * srcPts[--srcOff]);
            }
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values. The default implementation
     * computes the values from the {@link #scale} and {@link #offset} coefficients using
     * the {@code double} precision, then casts the result to the {@code float} type.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final float [] dstPts, int dstOff, int numPts)
    {
        while (--numPts >= 0) {
            dstPts[dstOff++] = (float) (offset + scale * srcPts[srcOff++]);
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values. The default implementation
     * computes the values from the {@link #scale} and {@link #offset} coefficients.
     */
    @Override
    public void transform(final float [] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
    {
        while (--numPts >= 0) {
            dstPts[dstOff++] = offset + scale * srcPts[srcOff++];
        }
    }

    /**
     * Transforms many distance vectors in a list of ordinal values.
     * The default implementation computes the values from the {@link #scale} coefficient only.
     *
     * @since 0.7
     */
    @Override
    public void deltaTransform(final double[] srcPts, int srcOff,
                               final double[] dstPts, int dstOff, int numPts)
    {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = scale * srcPts[srcOff++];
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = scale * srcPts[--srcOff];
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        return Numerics.hashCode(doubleToRawLongBits(offset) + 31*doubleToRawLongBits(scale)) ^ super.computeHashCode();
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
            if (object instanceof LinearTransform) {
                return Matrices.equals(getMatrix(), ((LinearTransform) object).getMatrix(), mode);
            }
        } else if (super.equals(object, mode)) {
            final LinearTransform1D that = (LinearTransform1D) object;
            return doubleToRawLongBits(this.scale)  == doubleToRawLongBits(that.scale) &&
                   doubleToRawLongBits(this.offset) == doubleToRawLongBits(that.offset);
            /*
             * NOTE: 'LinearTransform1D' and 'ConstantTransform1D' are heavily used by 'Category'
             * from 'org.apache.sis.coverage' package. It is essential for Cateory to differenciate
             * various NaN values. Because 'equals' is used by WeakHashSet.unique(Object) (which
             * is used by 'DefaultMathTransformFactory'), test for equality can't use the non-raw
             * doubleToLongBits method because it collapse all NaN into a single canonical value.
             * The 'doubleToRawLongBits' method instead provides the needed functionality.
             */
        }
        return false;
    }

    /**
     * Returns a string representation of this transform as a matrix, for consistency with other
     * {@link LinearTransform} implementations in Apache SIS.
     */
    @Override
    public String toString() {
        return Matrices.toString(getMatrix());
    }
}
