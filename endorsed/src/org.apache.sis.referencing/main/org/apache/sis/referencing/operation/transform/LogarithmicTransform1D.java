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
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.math.MathFunctions;


/**
 * A one dimensional, logarithmic transform. This transform is the inverse of {@link ExponentialTransform1D}.
 * The default implementation computes the natural logarithm of input values using {@link Math#log(double)}.
 * Subclasses compute alternate logarithms, for example in base 10 computed by {@link Math#log10(double)}.
 *
 * <p>Logarithms in bases other than <var>e</var> or 10 are computed by concatenating a linear transform,
 * using the following mathematical identity:</p>
 *
 * <blockquote>log<sub>base</sub>(<var>x</var>) = ln(<var>x</var>) / ln(base)</blockquote>
 *
 * <h2>Serialization</h2>
 * Serialized instances of this class are not guaranteed to be compatible with future SIS versions.
 * Serialization should be used only for short term storage or RMI between applications running the
 * same SIS version.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
class LogarithmicTransform1D extends AbstractMathTransform1D implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1535101265352133948L;

    /**
     * The unique instance of the natural logarithmic transform.
     */
    private static final LogarithmicTransform1D NATURAL = new LogarithmicTransform1D();

    /**
     * The inverse of this transform. Created only when first needed. Serialized in order to avoid
     * rounding error if this transform is actually the one which was created from the inverse.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private MathTransform1D inverse;

    /**
     * Constructs a new logarithmic transform.
     *
     * @see #create(double, double)
     */
    LogarithmicTransform1D() {
    }

    /**
     * Constructs a new logarithmic transform which add the given offset after the logarithm.
     *
     * @param  base    the base of the logarithm (typically 10).
     * @param  offset  the offset to add to the logarithm.
     * @return the math transform.
     */
    public static MathTransform1D create(final double base, final double offset) {
        ArgumentChecks.ensureStrictlyPositive("base", base);
        if (base == 10) {
            return Base10.create(offset);
        } else {
            return NATURAL.concatenate(1 / Math.log(base), offset);
        }
    }

    /**
     * Constructs a new logarithmic transform which is the inverse of the supplied exponential transform.
     */
    static MathTransform1D create(final ExponentialTransform1D inverse) {
        if (inverse.base == 10) {
            return Base10.create(-Math.log10(inverse.scale));
        } else {
            return NATURAL.concatenate(1 / inverse.lnBase, -Math.log(inverse.scale) / inverse.lnBase);
        }
    }

    /**
     * Returns the concatenation of this transform by the given scale and offset.
     * This method does not check if a simplification is possible.
     */
    private MathTransform1D concatenate(final double scale, final double offset) {
        final LinearTransform1D t = LinearTransform1D.create(scale, offset);
        return t.isIdentity() ? this : new ConcatenatedTransformDirect1D(this, t);
    }

    /**
     * Concatenates in an optimized way a neighbor math transform with this transform.
     * This implementation does special cases for {@link LinearTransform1D} and {@link ExponentialTransform1D}.
     */
    @Override
    protected void tryConcatenate(final TransformJoiner context) throws FactoryException {
        int relativeIndex = +1;
        do {
            final MathTransform concatenation;
            final MathTransform other = context.getTransform(relativeIndex).orElse(null);
            if (other instanceof LinearTransform1D) {
                final var linear = (LinearTransform1D) other;
                if (relativeIndex < 0 && linear.offset == 0 && linear.scale > 0) {
                    // Replace the affine transform which is before by an affine transform after.
                    concatenation = create(base(), transform(linear.scale));
                } else {
                    // Note: a previous version was handling the `relativeIndex > 0` case,
                    // but it was useless because the result was always the same transform.
                    continue;
                }
            } else if (other instanceof ExponentialTransform1D) {
                concatenation = ((ExponentialTransform1D) other).concatenateLog(this, -relativeIndex);
            } else {
                continue;
            }
            if (context.replace(relativeIndex, concatenation)) {
                return;
            }
        } while ((relativeIndex = -relativeIndex) < 0);
        super.tryConcatenate(context);
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
     * Returns the base of this logarithmic function.
     */
    double base() {
        return Math.E;
    }

    /**
     * Returns the natural logarithm of the base of this logarithmic function.
     * More specifically, returns <code>{@linkplain Math#log(double) Math.log}({@link #base()})</code>.
     */
    double lnBase() {
        return 1;
    }

    /**
     * Returns the offset applied after this logarithmic function.
     */
    double offset() {
        return 0;
    }

    /**
     * Gets the derivative of this function at a value.
     */
    @Override
    public double derivative(final double value) {
        return 1 / value;
    }

    /**
     * Returns the base of this logarithmic transform raised to the given power.
     *
     * @param  value  the power to raise the base.
     * @return the base of this transform raised to the given power.
     */
    double pow(final double value) {
        return Math.exp(value);
    }

    /**
     * Returns the logarithm of the given value in the base of this logarithmic transform.
     * This method is similar to {@link #transform(double)} except that the offset is not added.
     *
     * @param  value  the value for which to compute the log.
     * @return the log of the given value in the base used by this transform.
     */
    double log(final double value) {
        return Math.log(value);
    }

    /**
     * Transforms the specified value.
     */
    @Override
    public double transform(final double value) {
        return Math.log(value);
    }

    /**
     * Transforms many positions in a list of coordinate values.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = Math.log(srcPts[srcOff++]);
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = Math.log(srcPts[--srcOff]);
            }
        }
    }

    /**
     * Transforms many positions in a list of coordinate values.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        if (srcPts != dstPts || srcOff >= dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) Math.log(srcPts[srcOff++]);
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = (float) Math.log(srcPts[--srcOff]);
            }
        }
    }

    /**
     * Transforms many positions in a list of coordinate values.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            dstPts[dstOff++] = (float) Math.log(srcPts[srcOff++]);
        }
    }

    /**
     * Transforms many positions in a list of coordinate values.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            dstPts[dstOff++] = Math.log(srcPts[srcOff++]);
        }
    }

    /**
     * Special case for base 10 taking advantage of extra precision provided by {@link Math#log10(double)}.
     */
    static final class Base10 extends LogarithmicTransform1D {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5435804027536647558L;

        /**
         * The natural logarithm of 10.
         */
        private static final double LOG_10 = 2.302585092994045684;

        /**
         * Commonly used instance with no offset.
         */
        static final Base10 INSTANCE = new Base10(0);

        /**
         * The offset to add to the logarithm.
         *
         * <h4>Implementation note</h4>
         * The offset could be handled by a concatenation with {@link LinearTransform1D}.
         * instead of an explicit field in this class. However, the <var>offset</var> + log<sub>base</sub>(<var>x</var>)
         * formula is extensively used as a <i>transfer function</i> in grid coverages. Consequently, we keep this
         * explicit field for performance reasons.
         */
        private final double offset;

        /**
         * Creates a new instance with the given offset.
         *
         * @see #create(double)
         */
        private Base10(final double offset) {
            this.offset = offset;
        }

        /**
         * Creates a new instance with the given offset.
         */
        public static Base10 create(final double offset) {
            return (offset == 0) ? INSTANCE : new Base10(offset);
        }

        /** {@inheritDoc} */
        @Override
        double base() {
            return 10;
        }

        /** {@inheritDoc} */
        @Override
        double lnBase() {
            return LOG_10;
        }

        /** {@inheritDoc} */
        @Override
        double offset() {
            return offset;
        }

        /** {@inheritDoc} */
        @Override
        public double derivative(final double value) {
            return (1 / LOG_10) / value;
        }

        /** {@inheritDoc} */
        @Override
        double pow(final double value) {
            return MathFunctions.pow10(value);
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
                    dstPts[--dstOff] = Math.log10(srcPts[--srcOff]) + offset;
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
                    dstPts[--dstOff] = (float) (Math.log10(srcPts[--srcOff]) + offset);
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

        /** {@inheritDoc} */
        @Override
        protected int computeHashCode() {
            return Double.hashCode(offset) ^ super.computeHashCode();
        }

        /** Compares the specified object with this math transform for equality. */
        @Override
        public boolean equals(final Object object, final ComparisonMode mode) {
            if (object == this) {
                return true;  // Optimization for a common case.
            }
            if (super.equals(object, mode)) {
                return Numerics.equals(offset, ((Base10) object).offset);
            }
            return false;
        }
    }
}
