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

import java.util.Objects;
import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix1;
import org.apache.sis.referencing.internal.Arithmetic;
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.operation.provider.Affine;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.internal.shared.DoubleDouble;


/*
 * We really want to use doubleToRawLongBits, not doubleToLongBits, because the
 * coverage module needs the raw bits for differentiating various NaN values.
 */
import static java.lang.Double.doubleToRawLongBits;


/**
 * A one dimensional, linear transform.
 * Input values <var>x</var> are converted into output values <var>y</var> using the following equation:
 *
 * <blockquote><var>y</var>  =  <var>x</var> × {@linkplain #scale} + {@linkplain #offset}</blockquote>
 *
 * This class is the same as a 2×2 affine transform. However, this specialized {@code LinearTransform1D} class
 * is faster. This kind of transform is extensively used by {@code org.apache.sis.coverage.grid.GridCoverage2D}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see LogarithmicTransform1D
 * @see ExponentialTransform1D
 */
@SuppressWarnings("CloneInNonCloneableClass")
class LinearTransform1D extends AbstractMathTransform1D implements LinearTransform, ExtendedPrecisionMatrix, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4507255773105057855L;

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
     * The {@link #scale} and {@link #offset} values as {@code Number} objects, or {@code null} if zero.
     * For performance reasons, those terms are not used in {@code transform(…)} methods.
     * They are used in {@link #getMatrix()} for making possible to use extended precision
     * during the creation of concatenated or inverse transforms.
     */
    private final Number scaleNumber, offsetNumber;

    /**
     * The inverse of this transform. Created only when first needed.
     *
     * @see #inverse()
     */
    private transient LinearTransform1D inverse;

    /**
     * Constructs a new linear transform. This constructor is provided for subclasses only.
     * Instances should be created using the {@linkplain #create(double, double) factory method},
     * which may returns optimized implementations for some particular argument values.
     *
     * @param scale   the {@code scale}  term in the linear equation.
     * @param offset  the {@code offset} term in the linear equation.
     *
     * @see #create(double, double)
     */
    LinearTransform1D(final Number scale, final Number offset) {
        this.scale        = scale .doubleValue();
        this.offset       = offset.doubleValue();                   // Unconditional because we want to preserve -0.
        this.scaleNumber  = (this.scale  == 0) ? null : scale;      // SHALL be null instead of zero.
        this.offsetNumber = (this.offset == 0) ? null : offset;
    }

    /**
     * Constructs a new linear transform.
     *
     * @param  scale   the {@code scale}  term in the linear equation.
     * @param  offset  the {@code offset} term in the linear equation.
     * @return the linear transform for the given scale and offset.
     *
     * @see MathTransforms#linear(double, double)
     */
    static LinearTransform1D create(final Number scale, Number offset) {
        if (ExtendedPrecisionMatrix.isZero(scale)) {
            if (ExtendedPrecisionMatrix.isZero(offset)) {
                return ConstantTransform1D.ZERO;
            } else if (Arithmetic.isOne(offset)) {
                return ConstantTransform1D.ONE;
            } else {
                return new ConstantTransform1D(offset);
            }
        } else if (ExtendedPrecisionMatrix.isZero(offset)) {
            final double s = scale.doubleValue();
            if (s == +1) return IdentityTransform1D.INSTANCE;
            if (s == -1) return NEGATE;
            if (offset == null) offset = 0;
        }
        return new LinearTransform1D(scale, offset);
    }

    /**
     * Creates a constant function having value <var>y</var>, and for which the inverse is <var>x</var>.
     */
    static LinearTransform1D constant(final double x, final double y) {
        final LinearTransform1D tr = create(null, y);
        if (!Double.isNaN(x)) {
            tr.inverse = create(null, x);
        }
        return tr;
    }

    /**
     * Implementation of Matrix API. No need for clone because this matrix is immutable.
     */
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override public final Matrix clone()     {return this;}
    @Override public final Matrix getMatrix() {return this;}
    @Override public final int    getNumRow() {return 2;}
    @Override public final int    getNumCol() {return 2;}

    /**
     * Retrieves the value at the specified row and column of the matrix.
     * Row and column indices can be only 0 or 1.
     * If the value is zero, then this method <em>shall</em> return {@code null}.
     */
    @Override
    public final Number getElementOrNull(final int row, final int column) {
        if (((row | column) & ~1) == 0) {
            switch ((row << 1) | column) {
                case 0: return scaleNumber;
                case 1: return offsetNumber;
                case 2: return null;
                case 3: return 1;
            }
        }
        throw new IndexOutOfBoundsException();
    }

    /**
     * Returns a copy of all matrix elements in a flat, row-major array.
     * Zero values <em>shall</em> be null. Callers can write in the returned array.
     */
    @Override
    public final Number[] getElementAsNumbers(final boolean writable) {
        return new Number[] {scaleNumber, offsetNumber, null, 1};
    }

    /**
     * Returns the parameter descriptors for this math transform.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Affine.provider(1, 1, true).getParameters();
    }

    /**
     * Returns the matrix elements as a group of parameter values. The number of parameters
     * depends on the matrix size. Only matrix elements different from their default value
     * will be included in this group.
     *
     * @return the parameter values for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return Affine.parameters(getMatrix());
    }

    /**
     * Creates the inverse transform of this object.
     */
    @Override
    public synchronized LinearTransform1D inverse() throws NoninvertibleTransformException {
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
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final LinearTransform1D inverse;
                if (DoubleDouble.DISABLED) {
                    inverse = create(1/scale, -offset/scale);
                } else {
                    final Number n = Arithmetic.negate(offsetNumber);
                    inverse = create(Arithmetic.inverse(scaleNumber), Arithmetic.divide(n, scaleNumber));
                    inverse.inverse = this;
                }
                this.inverse = inverse;
            } else {
                inverse = (LinearTransform1D) super.inverse();              // Throws NoninvertibleTransformException
            }
        }
        return inverse;
    }

    /**
     * Returns {@code true} since this transform is affine.
     */
    @Override
    public final boolean isAffine() {
        return true;
    }

    /**
     * Tests whether this transform does not move any points.
     * This method should always returns {@code false}, because
     * {@code MathTransforms.linear(…)} should have created specialized implementations for identity cases.
     * Nevertheless we perform the full check as a safety, in case someone instantiated this class directly
     * instead of using a factory method.
     */
    @Override
    public final boolean isIdentity() {
       return offset == 0 && scale == 1;
    }

    /**
     * Gets the derivative of this transform at a point.
     *
     * @param  point  ignored for a linear transform. Can be null.
     * @return the derivative at the given point.
     */
    @Override
    public final Matrix derivative(final DirectPosition point) throws TransformException {
        return new Matrix1(scale);
    }

    /**
     * Gets the derivative of this function at a value.
     *
     * @param  value  ignored for a linear transform. Can be {@link Double#NaN NaN}.
     * @return the derivative at the given point.
     */
    @Override
    public final double derivative(final double value) {
        return scale;
    }

    /**
     * Transforms the specified value.
     */
    @Override
    public double transform(final double value) {
        if (Formulas.USE_FMA) {
            return Math.fma(value, scale, offset);
        } else {
            return offset + scale * value;
        }
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
            if (Formulas.USE_FMA) {
                dstPts[dstOff] = Math.fma(srcPts[srcOff], scale, offset);
            } else {
                dstPts[dstOff] = offset + scale*srcPts[srcOff];
            }
        }
        return derivate ? new Matrix1(scale) : null;
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples. The default implementation
     * computes the values from the {@link #scale} and {@link #offset} coefficients.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
    {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                if (Formulas.USE_FMA) {
                    dstPts[dstOff++] = Math.fma(srcPts[srcOff++], scale, offset);
                } else {
                    dstPts[dstOff++] = offset + scale * srcPts[srcOff++];
                }
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                if (Formulas.USE_FMA) {
                    dstPts[--dstOff] = Math.fma(srcPts[--srcOff], scale, offset);
                } else {
                    dstPts[--dstOff] = offset + scale * srcPts[--srcOff];
                }
            }
        }
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples. The default implementation
     * computes the values from the {@link #scale} and {@link #offset} coefficients using
     * the {@code double} precision, then casts the result to the {@code float} type.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff,
                          final float[] dstPts, int dstOff, int numPts)
    {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                if (Formulas.USE_FMA) {
                    dstPts[dstOff++] = (float) Math.fma(srcPts[srcOff++], scale, offset);
                } else {
                    dstPts[dstOff++] = (float) (offset + scale * srcPts[srcOff++]);
                }
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                if (Formulas.USE_FMA) {
                    dstPts[--dstOff] = (float) Math.fma(srcPts[--srcOff], scale, offset);
                } else {
                    dstPts[--dstOff] = (float) (offset + scale * srcPts[--srcOff]);
                }
            }
        }
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples. The default implementation
     * computes the values from the {@link #scale} and {@link #offset} coefficients using
     * the {@code double} precision, then casts the result to the {@code float} type.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final float [] dstPts, int dstOff, int numPts)
    {
        while (--numPts >= 0) {
            if (Formulas.USE_FMA) {
                dstPts[dstOff++] = (float) Math.fma(srcPts[srcOff++], scale, offset);
            } else {
                dstPts[dstOff++] = (float) (offset + scale * srcPts[srcOff++]);
            }
        }
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples. The default implementation
     * computes the values from the {@link #scale} and {@link #offset} coefficients.
     */
    @Override
    public void transform(final float [] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
    {
        while (--numPts >= 0) {
            if (Formulas.USE_FMA) {
                dstPts[dstOff++] = Math.fma(srcPts[srcOff++], scale, offset);
            } else {
                dstPts[dstOff++] = offset + scale * srcPts[srcOff++];
            }
        }
    }

    /**
     * Transforms many distance vectors in a sequence of coordinate tuples.
     * The default implementation computes the values from the {@link #scale} coefficient only.
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
     * @hidden because nothing new to said.
     */
    @Override
    protected int computeHashCode() {
        return Long.hashCode(super.computeHashCode() ^
               (doubleToRawLongBits(offset)         +
                doubleToRawLongBits(scale)     * 31 +
                Objects.hashCode(offsetNumber) * 37 +
                Objects.hashCode(scaleNumber)  * 7));
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {                                               // Slight optimization
            return true;
        }
        if (mode != ComparisonMode.STRICT) {
            if (object instanceof LinearTransform) {
                return Matrices.equals(getMatrix(), ((LinearTransform) object).getMatrix(), mode);
            }
        } else if (super.equals(object, mode)) {
            final var that = (LinearTransform1D) object;
            return doubleToRawLongBits(scale)  == doubleToRawLongBits(that.scale)  &&
                   doubleToRawLongBits(offset) == doubleToRawLongBits(that.offset) &&
                   Objects.equals(scaleNumber,  that.scaleNumber) &&
                   Objects.equals(offsetNumber, that.offsetNumber);
            /*
             * NOTE: `LinearTransform1D` and `ConstantTransform1D` are extensively used by `SampleDimension`
             * in `org.apache.sis.coverage` package. It is essential for sample dimensions to differentiate
             * various NaN values. Because `equals(…)` is used by `WeakHashSet.unique(Object)` in turn used
             * by `DefaultMathTransformFactory`, equality tests cannot use the non-raw `doubleToLongBits(…)`
             * method because it collapse all NaN into a single canonical value.
             * The `doubleToRawLongBits(…)` method instead provides the needed functionality.
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
