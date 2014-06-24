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
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;


/**
 * A one dimensional, logarithmic transform. This transform is the inverse of {@link ExponentialTransform1D}.
 * Input values <var>x</var> are converted into output values <var>y</var> using the following equation:
 *
 * <blockquote><table class="compact" summary="y = offset + log(x)">
 *   <tr><td><var>y</var></td><td> = </td><td>{@linkplain #offset} + log<sub>{@linkplain #base}</sub>(<var>x</var>)</td></tr>
 *   <tr><td>            </td><td> = </td><td>{@linkplain #offset} + ln(<var>x</var>) / ln({@linkplain #base})</td></tr>
 * </table></blockquote>
 *
 * {@section Serialization}
 * Serialized instances of this class are not guaranteed to be compatible with future SIS versions.
 * Serialization should be used only for short term storage or RMI between applications running the
 * same SIS version.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5 (derived from geotk-2.0)
 * @version 0.5
 * @module
 */
class LogarithmicTransform1D extends AbstractMathTransform1D implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1535101265352133948L;

    /**
     * The logarithm base.
     */
    protected final double base;

    /**
     * Natural logarithm of {@link #base}, used for {@link #derivative(double)} computation.
     */
    final double lnBase;

    /**
     * The offset to add to the logarithm.
     *
     * <span class="note"><b>Note:</b> the offset could be handled by a concatenation with {@link LinearTransform1D}.
     * instead than an explicit field in this class. However the <var>offset</var> + log<sub>base</sub>(<var>x</var>)
     * formula is extensively used as a <cite>transfer function</cite> in grid coverages. Consequently we keep this
     * explicit field for performance reasons.</span>
     */
    protected final double offset;

    /**
     * The inverse of this transform. Created only when first needed. Serialized in order to avoid
     * rounding error if this transform is actually the one which was created from the inverse.
     */
    private MathTransform1D inverse;

    /**
     * Constructs a new logarithmic transform which is the
     * inverse of the supplied exponential transform.
     *
     * @see #create(ExponentialTransform1D)
     */
    private LogarithmicTransform1D(final ExponentialTransform1D inverse) {
        this.base    = inverse.base;
        this.lnBase  = inverse.lnBase;
        this.offset  = -Math.log(inverse.scale) / lnBase;
        this.inverse = inverse;
    }

    /**
     * Constructs a new logarithmic transform. This constructor is provided for subclasses only.
     * Instances should be created using the {@linkplain #create(double, double) factory method},
     * which may return optimized implementations for some particular argument values.
     *
     * @param base    The base of the logarithm (typically 10).
     * @param offset  The offset to add to the logarithm.
     */
    protected LogarithmicTransform1D(final double base, final double offset) {
        ArgumentChecks.ensureStrictlyPositive("base", base);
        this.base    = base;
        this.offset  = offset;
        this.lnBase  = Math.log(base);
    }

    /**
     * Constructs a new logarithmic transform which include the given offset after the logarithm.
     *
     * @param  base    The base of the logarithm (typically 10).
     * @param  offset  The offset to add to the logarithm.
     * @return The math transform.
     */
    public static MathTransform1D create(final double base, final double offset) {
        if (base == 10) {
            return (offset == 0) ? Base10.INSTANCE : new Base10(offset);
        }
        if (base == 0 || base == Double.POSITIVE_INFINITY) {
            /*
             * offset + ln(x) / ln(0)   =   offset + ln(x) / -∞   =   offset + -0   for 0 < x < ∞
             * offset + ln(x) / ln(∞)   =   offset + ln(x) / +∞   =   offset +  0   for 0 < x < ∞
             */
            return LinearTransform1D.create(0, offset);
        }
        return new LogarithmicTransform1D(base, offset);
    }

    /**
     * Constructs a new logarithmic transform which is the inverse of the supplied exponential transform.
     */
    static LogarithmicTransform1D create(final ExponentialTransform1D inverse) {
        if (inverse.base == 10) {
            return new Base10(inverse);
        }
        return new LogarithmicTransform1D(inverse);
    }

    /**
     * Returns the inverse of this transform.
     */
    @Override
    public synchronized MathTransform1D inverse() {
        if (inverse == null) {
            inverse = new ExponentialTransform1D(this);
            // Above constructor will set ExponentialTransform1D.inverse = this.
        }
        return inverse;
    }

    /**
     * Gets the derivative of this function at a value.
     */
    @Override
    public double derivative(final double value) {
        return 1 / (lnBase * value);
    }

    /**
     * Returns the logarithm of the given value in the base given to this transform constructor.
     * This method is similar to {@link #transform(double)} except that the offset is not added.
     *
     * @param  value The value for which to compute the log.
     * @return The log of the given value in the base used by this transform.
     */
    double log(final double value) {
        return Math.log(value) / lnBase;
    }

    /**
     * Transforms the specified value.
     */
    @Override
    public double transform(final double value) {
        return Math.log(value) / lnBase + offset;
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = Math.log(srcPts[srcOff++]) / lnBase + offset;
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = Math.log(srcPts[--srcOff]) / lnBase + offset;
            }
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (Math.log(srcPts[srcOff++]) / lnBase + offset);
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = (float) (Math.log(srcPts[--srcOff]) / lnBase + offset);
            }
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            dstPts[dstOff++] = (float) (Math.log(srcPts[srcOff++]) / lnBase + offset);
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            dstPts[dstOff++] = Math.log(srcPts[srcOff++]) / lnBase + offset;
        }
    }

    /**
     * Special case for base 10 taking advantage of extra precision provided by {@link Math#log10(double)}.
     */
    private static final class Base10 extends LogarithmicTransform1D {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -5435804027536647558L;

        /** Commonly used instance. */
        static LogarithmicTransform1D INSTANCE = new Base10(0);

        /** Constructs the inverse of the supplied exponential transform. */
        Base10(final ExponentialTransform1D inverse) {
            super(inverse);
        }

        /** Creates a new instance with the given offset. */
        protected Base10(final double offset) {
            super(10, offset);
        }

        /** {@inheritDoc} */
        @Override
        double log(final double value) {
            return Math.log10(value);
        }

        /** {@inheritDoc} */
        @Override
        public double transform(final double value) {
            return Math.log10(value) + offset;
        }

        /** {@inheritDoc} */
        @Override
        public void transform(final double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
            if (srcPts != dstPts || srcOff >= dstOff) {
                while (--numPts >= 0) {
                    dstPts[dstOff++] = Math.log10(srcPts[srcOff++]) + offset;
                }
            } else {
                srcOff += numPts;
                dstOff += numPts;
                while (--numPts >= 0) {
                    dstPts[--dstOff] = Math.log10(srcPts[srcOff++]) + offset;
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void transform(final float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
            if (srcPts != dstPts || srcOff >= dstOff) {
                while (--numPts >= 0) {
                    dstPts[dstOff++] = (float) (Math.log10(srcPts[srcOff++]) + offset);
                }
            } else {
                srcOff += numPts;
                dstOff += numPts;
                while (--numPts >= 0) {
                    dstPts[--dstOff] = (float) (Math.log10(srcPts[srcOff++]) + offset);
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (Math.log10(srcPts[srcOff++]) + offset);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void transform(final float[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = Math.log10(srcPts[srcOff++]) + offset;
            }
        }
    }

    /**
     * Concatenates in an optimized way a {@link MathTransform} {@code other} to this
     * {@code MathTransform}. This implementation can optimize some concatenation with
     * {@link LinearTransform1D} and {@link ExponentialTransform1D}.
     *
     * @param  other The math transform to apply.
     * @param  applyOtherFirst {@code true} if the transformation order is {@code other}
     *         followed by {@code this}, or {@code false} if the transformation order is
     *         {@code this} followed by {@code other}.
     * @return The combined math transform, or {@code null} if no optimized combined
     *         transform is available.
     */
    @Override
    final MathTransform concatenate(final MathTransform other, final boolean applyOtherFirst) {
        if (other instanceof LinearTransform) {
            final LinearTransform1D linear = (LinearTransform1D) other;
            if (applyOtherFirst) {
                if (linear.offset == 0 && linear.scale > 0) {
                    return create(base, Math.log(linear.scale) / lnBase + offset);
                }
            } else {
                final double newBase = Math.pow(base, 1 / linear.scale);
                if (!Double.isNaN(newBase)) {
                    return create(newBase, linear.scale * offset + linear.offset);
                }
            }
        } else if (other instanceof ExponentialTransform1D) {
            return ((ExponentialTransform1D) other).concatenateLog(this, !applyOtherFirst);
        }
        return super.concatenate(other, applyOtherFirst);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        return Numerics.hashCode(Double.doubleToLongBits(base) +
                            31 * Double.doubleToLongBits(offset)) ^ super.computeHashCode();
    }

    /**
     * Compares the specified object with this math transform for equality.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;  // Optimization for a common case.
        }
        if (super.equals(object, mode)) {
            final LogarithmicTransform1D that = (LogarithmicTransform1D) object;
            return Numerics.equals(this.base,   that.base) &&
                   Numerics.equals(this.offset, that.offset);
        }
        return false;
    }
}
