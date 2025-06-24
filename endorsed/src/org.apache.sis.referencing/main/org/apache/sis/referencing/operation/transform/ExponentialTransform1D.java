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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.util.FactoryException;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.privy.Numerics;


/**
 * A one dimensional exponential transform.
 * Input values <var>x</var> are converted into output values <var>y</var> using the following equation:
 *
 * <blockquote><var>y</var> = {@linkplain #scale}⋅{@linkplain #base}<sup><var>x</var></sup></blockquote>
 *
 * <h2>Tip</h2>
 * if a linear transform is applied before this exponential transform, then the equation can be rewritten as:
 * <var>scale</var>⋅<var>base</var><sup><var>a</var> + <var>b</var>⋅<var>x</var></sup> =
 * <var>scale</var>⋅<var>base</var><sup><var>a</var></sup>⋅(<var>base</var><sup><var>b</var></sup>)<sup><var>x</var></sup>
 *
 * It is possible to find back the coefficients of the original linear transform by
 * pre-concatenating a logarithmic transform before the exponential one, as below:
 *
 * {@snippet lang="java" :
 *     LinearTransform1D linear = MathTransforms.create(exponentialTransform,
 *             LogarithmicTransform1D.create(base, -Math.log(scale) / Math.log(base)));
 *     }
 *
 * <h2>Serialization</h2>
 * Serialized instances of this class are not guaranteed to be compatible with future SIS versions.
 * Serialization should be used only for short term storage or RMI between applications running the
 * same SIS version.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class ExponentialTransform1D extends AbstractMathTransform1D implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5331178990358868947L;

    /**
     * The base to be raised to a power.
     */
    final double base;

    /**
     * Natural logarithm of {@link #base}, used for {@link #derivative(double)} computation.
     */
    final double lnBase;

    /**
     * The scale value to be multiplied.
     *
     * <h4>Implementation note</h4>
     * The scale could be handled by a concatenation with {@link LinearTransform1D} instead of an explicit
     * field in this class. However, the <var>scale</var>⋅<var>base</var><sup><var>x</var></sup> formula
     * is extensively used as a <i>transfer function</i> in grid coverages.
     * Consequently, we keep this explicit field for performance reasons.
     */
    final double scale;

    /**
     * The inverse of this transform. Created only when first needed. Serialized in order to avoid
     * rounding error if this transform is actually the one which was created from the inverse.
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private MathTransform1D inverse;

    /**
     * Constructs a new exponential transform which is the inverse of the supplied logarithmic transform.
     */
    ExponentialTransform1D(final LogarithmicTransform1D inverse) {
        this.base    = inverse.base();
        this.lnBase  = inverse.lnBase();
        this.scale   = inverse.pow(-inverse.offset());
        this.inverse = inverse;
    }

    /**
     * Constructs a new exponential transform. This constructor is provided for subclasses only.
     * Instances should be created using the {@linkplain #create(double, double) factory method},
     * which may returns optimized implementations for some particular argument values.
     *
     * @param base   the base to be raised to a power.
     * @param scale  the scale value to be multiplied.
     */
    protected ExponentialTransform1D(final double base, final double scale) {
        this.base   = base;
        this.scale  = scale;
        this.lnBase = Math.log(base);
    }

    /**
     * Constructs a new exponential transform which include the given scale factor applied after the exponentiation.
     *
     * @param  base   the base to be raised to a power.
     * @param  scale  the scale value to be multiplied.
     * @return the math transform.
     */
    public static MathTransform1D create(final double base, final double scale) {
        if (base == 0 || scale == 0) {
            return ConstantTransform1D.ZERO;
        }
        if (base == 1) {
            return LinearTransform1D.create(scale, null);
        }
        return new ExponentialTransform1D(base, scale);
    }

    /**
     * Returns the inverse of this transform.
     */
    @Override
    public synchronized MathTransform1D inverse() {
        if (inverse == null) {
            inverse = LogarithmicTransform1D.create(this);
            // Above method will set LogarithmicTransform1D.inverse = this.
        }
        return inverse;
    }

    /**
     * Gets the derivative of this function at a value.
     */
    @Override
    public double derivative(final double value) {
        return lnBase * transform(value);
    }

    /**
     * Transforms the specified value.
     */
    @Override
    public double transform(final double value) {
        return scale * Math.pow(base, value);
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = scale * Math.pow(base, srcPts[srcOff++]);
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = scale * Math.pow(base, srcPts[--srcOff]);
            }
        }
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (scale * Math.pow(base, srcPts[srcOff++]));
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = (float) (scale * Math.pow(base, srcPts[--srcOff]));
            }
        }
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            dstPts[dstOff++] = (float) (scale * Math.pow(base, srcPts[srcOff++]));
        }
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            dstPts[dstOff++] = scale * Math.pow(base, srcPts[srcOff++]);
        }
    }

    /**
     * Concatenates in an optimized way a neighbor math transform with this transform.
     * This implementation does special cases for {@link LinearTransform1D} and {@link LogarithmicTransform1D}.
     */
    @Override
    protected void tryConcatenate(final TransformJoiner context) throws FactoryException {
        MathTransform concatenation = null;
        int relativeIndex = +1;
        do {
            final MathTransform other = context.getTransform(relativeIndex).orElse(null);
            if (other instanceof LinearTransform1D) {
                final var linear = (LinearTransform1D) other;
                if (relativeIndex < 0) {
                    final double newBase  = Math.pow(base, linear.scale);
                    final double newScale = Math.pow(base, linear.offset) * scale;
                    if (!Double.isNaN(newBase) && !Double.isNaN(newScale)) {
                        concatenation = create(newBase, newScale);
                    }
                } else {
                    if (linear.offset == 0) {
                        concatenation = create(base, scale * linear.scale);
                    }
                }
            } else if (other instanceof LogarithmicTransform1D) {
                concatenation = concatenateLog((LogarithmicTransform1D) other, relativeIndex);
            }
            if (concatenation != null && context.replace(relativeIndex, concatenation)) {
                return;
            }
        } while ((relativeIndex = -relativeIndex) < 0);
        super.tryConcatenate(context);
    }

    /**
     * Concatenates in an optimized way a {@link LogarithmicTransform1D} {@code other}
     * to this {@code ExponentialTransform1D}.
     *
     * @param  other          the math transform to apply.
     * @param  relativeIndex  -1 if the transformation order is {@code other} followed by {@code this}, or
     *                        +1 if the transformation order is {@code this} followed by {@code other}.
     * @return the combined math transform, or {@code null} if no optimized combined transform is available.
     */
    final MathTransform concatenateLog(final LogarithmicTransform1D other, final int relativeIndex) {
        final double newScale = lnBase / other.lnBase();
        if (relativeIndex < 0) {
            return MathTransforms.concatenate(PowerTransform1D.create(newScale),
                    LinearTransform1D.create(scale * Math.pow(base, other.offset()), null));
        } else {
            final double newOffset;
            if (scale > 0) {
                newOffset = other.log(scale) + other.offset();
            } else {
                /*
                 * Maybe the Math.log(double) argument will become
                 * positive if we rewrite the equation that way...
                 */
                newOffset = other.log(scale * other.offset() * other.lnBase());
            }
            if (!Double.isNaN(newOffset)) {
                return LinearTransform1D.create(newScale, newOffset);
            }
        }
        return null;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected int computeHashCode() {
        return Long.hashCode(Double.doubleToLongBits(base)
                + 31 * Double.doubleToLongBits(scale)) ^ super.computeHashCode();
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;                    // Optimization for a common case.
        }
        if (super.equals(object, mode)) {
            final ExponentialTransform1D that = (ExponentialTransform1D) object;
            return Numerics.equals(this.base,  that.base) &&
                   Numerics.equals(this.scale, that.scale);
        }
        return false;
    }
}
