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
import java.io.Serializable;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrix1;
import org.apache.sis.referencing.operation.provider.Interpolation1D;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.resources.Errors;


/**
 * A transform that performs linear interpolation between values.
 * The transform is invertible if, and only if, the values are in increasing order.
 *
 * <p>If desired values in decreasing order can be supported by inverting the sign of all values,
 * then concatenating this transform with a transform that multiply all output values by -1.</p>
 *
 * <h2>Extrapolation</h2>
 * If an input value is outside the expected range of values, this class extrapolates using the
 * slope defined by the two first points if the requested value is before, or the slope defined
 * by the two last points if the requested value is after.   In other words, extrapolations are
 * computed using only values at the extremum where extrapolation happen. This rule causes less
 * surprising behavior when computing a data cube envelope, which may need extrapolation by 0.5
 * pixel before the first value or after the last value.
 *
 * <h3>Example</h3>
 * If a vertical dimension is made of slices at y₀=5, y₁=10, y₂=100 and y₃=250 meters, then linear
 * interpolation at 0.5 is 7.5 meters and extrapolation at -0.5 is expected to give 2.5 meters.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see MathTransforms#interpolate(double[], double[])
 */
final class LinearInterpolator1D extends AbstractMathTransform1D implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5025693608589996896L;

    /**
     * The sequence values specified at construction time.
     * Must contain at least 2 values.
     */
    private final double[] values;

    /**
     * If the transform is invertible, the inverse. Otherwise {@code null}.
     * The transform is invertible only if values are in increasing order.
     */
    @SuppressWarnings("serial")                         // Most SIS implementations are serializable.
    private final MathTransform1D inverse;

    /**
     * Creates a new transform which will interpolate in the given table of values.
     * The inputs are {0, 1, … , <var>N</var>} where <var>N</var> is length of output values.
     *
     * <p>This constructor assumes that the {@code values} array has already been cloned,
     * so it will not clone it again. That array shall contain at least two values.</p>
     *
     * @param values  the <var>y</var> values in <var>y=f(x)</var> where <var>x</var> = {0, 1, … , {@code values.length-1}}.
     */
    private LinearInterpolator1D(final double[] values) {
        this.values = values;                           // Cloning this array is caller's responsibility.
        double last = values[0];
        for (int i=1; i<values.length; i++) {
            if (!(last <= (last = values[i]))) {        // Use `!` for catching NaN values.
                inverse = null;                         // Transform is not reversible.
                return;
            }
        }
        inverse = new Inverse(this);
    }

    /**
     * Creates a transform for the given values. This method returns an affine transform instead of an
     * interpolator if the given values form a series with a constant increment. The given array shall
     * contain at least two values.
     *
     * @param  values  a <strong>copy</strong> of the user-provided values. This array may be modified.
     */
    private static MathTransform1D create(final double[] values) {
        final int n = values.length - 1;
        final double offset = values[0];
        final double slope = (values[n] - offset) / n;
        final double as = Math.abs(slope);
        /*
         * If the increment between values is constant (with a small tolerance factor),
         * return a one-dimensional affine transform instead of an interpolator.
         * We need to perform this check before the sign reversal applied after this loop.
         */
        double value, tolerance;
        int i = 0;
        do {
            if (++i >= n) {
                return LinearTransform1D.create(slope, offset);
            }
            value = values[i];
            tolerance = Math.max(Math.abs(value), as) * Numerics.COMPARISON_THRESHOLD;
        } while (Numerics.epsilonEqual(value, Math.fma(i, slope, offset), tolerance));
        /*
         * If the values are in decreasing order, reverse their sign so we get increasing order.
         * We will multiply the results by -1 after the transformation.
         */
        final boolean isReverted = (slope < 0);
        if (isReverted) {
            for (i=0; i <= n; i++) {
                values[i] = -values[i];
            }
        }
        MathTransform1D tr = new LinearInterpolator1D(values);
        if (isReverted) {
            tr = new ConcatenatedTransformDirect1D(tr, LinearTransform1D.NEGATE);
        }
        return tr;
    }

    /**
     * Creates a <i>y=f(x)</i> transform for the given preimage (<var>x</var>) and values (<var>y</var>).
     * See {@link MathTransforms#interpolate(double[], double[])} javadoc for more information.
     */
    static MathTransform1D create(final double[] preimage, final double[] values) {
        final int length;
        if (preimage == null) {
            if (values == null) {
                return IdentityTransform1D.INSTANCE;
            }
            length = values.length;
        } else {
            length = preimage.length;
            if (values != null && values.length != length) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedArrayLengths));
            }
        }
        switch (length) {
            case 0: throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, (preimage != null) ? "preimage" : "values"));
            case 1: return LinearTransform1D.constant((preimage != null) ? preimage[0] : Double.NaN, (values != null) ? values[0] : Double.NaN);
        }
        /*
         * A common usage of this 'create' method is for creating a "gridToCRS" transform from grid coordinates
         * to something else, in which case the preimage array is null. In the less frequent case where preimage
         * is non-null, we first convert from preimage to indices, then from indices to y values.
         */
        MathTransform1D tr = null;
        if (values != null) {
            tr = create(values.clone());
        }
        if (preimage != null) {
            final MathTransform1D indexToValues = tr;
            try {
                tr = create(preimage.clone()).inverse();    // Transform from `preimage` to index used as input.
            } catch (NoninvertibleTransformException e) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.NonMonotonicSequence_1, "preimage"), e);
            }
            if (indexToValues != null) {
                tr = MathTransforms.concatenate(tr, indexToValues);
            }
        }
        return tr;
    }

    /**
     * Returns the parameter descriptors for this math transform.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Interpolation1D.PARAMETERS;
    }

    /**
     * Returns the parameter values for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        final ParameterValueGroup p = getParameterDescriptors().createValue();
        p.parameter("values").setValue(values);
        return p;
    }

    /**
     * Returns {@code true} if this transform is the identity transform. This method should never returns {@code true}
     * since we verified the inputs in the {@code create(…)} method. We nevertheless verify as a paranoiac safety.
     */
    @Override
    public boolean isIdentity() {
        for (int i=0; i<values.length; i++) {
            if (values[i] != i) return false;
        }
        return true;
    }

    /**
     * Combines {@link #transform(double)}, {@link #derivative(double)} in a single method call.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        double x = srcPts[srcOff];
        final double y, d;
        if (x >= 0) {
            final int i = (int) x;
            final int n = values.length - 1;
            if (i < n) {
                x -= i;
                final double y0 = values[i  ];
                final double y1 = values[i+1];
                y = y0 * (1-x) + y1 * x;
                d = y1 - y0;
            } else {
                // x is after the last available value.
                final double y1 = values[n];
                d = y1 - values[n-1];
                y = (x - n) * d + y1;
            }
        } else {
            // x is before the first available value.
            final double y0 = values[0];
            d = values[1] - y0;
            y = x * d + y0;
        }
        if (dstPts != null) {
            dstPts[dstOff] = y;
        }
        return derivate ? new Matrix1(d) : null;
    }

    /**
     * Interpolates a <var>y</var> values for the given <var>x</var>.
     * The given <var>x</var> value should be between 0 to {@code values.length - 1} inclusive.
     * If the given input value is outside that range, then the output value will be extrapolated.
     */
    @Override
    public double transform(double x) {
        if (x >= 0) {
            final int i = (int) x;
            final int n = values.length - 1;
            if (i < n) {
                x -= i;
                return values[i] * (1-x) + values[i+1] * x;
            } else {
                // x is after the last available value.
                final double y1 = values[n];
                return (x - n) * (y1 - values[n-1]) + y1;
            }
        } else {
            // x is before the first available value.
            final double y0 = values[0];
            return x * (values[1] - y0) + y0;
        }
    }

    /**
     * Returns the derivative of <var>y</var> for the given <var>x</var>.
     * Note: for each segment, the derivative is considered constant between
     * <var>x</var> inclusive and <var>x+1</var> exclusive.
     */
    @Override
    public double derivative(final double x) {
        final int i = Math.max(0, Math.min(values.length - 2, (int) x));
        return values[i+1] - values[i];
    }

    /**
     * Returns the inverse of this transform, or throw an exception if there is no inverse.
     */
    @Override
    public MathTransform1D inverse() throws NoninvertibleTransformException {
        if (inverse != null) {
            return inverse;
        }
        throw new NoninvertibleTransformException(Resources.format(Resources.Keys.NonInvertibleBecauseUnordered));
    }

    /**
     * The inverse of the enclosing {@link LinearInterpolator1D}. Given a <var>y</var> value, this class performs
     * a bilinear search for locating the lower and upper <var>x</var> values as integers, then interpolates the
     * <var>x</var> real value.
     */
    private static final class Inverse extends AbstractMathTransform1D.Inverse implements MathTransform1D, Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5112948223332095009L;

        /**
         * The enclosing transform.
         */
        private final LinearInterpolator1D forward;

        /**
         * Creates a new inverse transform.
         */
        Inverse(final LinearInterpolator1D forward) {
            this.forward = forward;
        }

        /**
         * Returns the inverse of this math transform.
         */
        @Override
        public MathTransform1D inverse() {
            return forward;
        }

        /**
         * Combines {@link #transform(double)}, {@link #derivative(double)} in a single method call.
         * The intent is to avoid to call {@link Arrays#binarySearch(double[], double)} twice for the
         * same value.
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final boolean derivate) throws TransformException
        {
            final double d, x, y = srcPts[srcOff];
            final double[] values = forward.values;
            int i = Arrays.binarySearch(values, y);
            if (i >= 0) {
                x = i;
                i = Math.max(1, Math.min(values.length - 1, i));
                d = values[i] - values[i-1];
            } else {
                i = ~i;
                if (i >= 1) {
                    if (i < values.length) {
                        final double y0 = values[i-1];
                        x = (y - y0) / (d = values[i] - y0) + (i-1);
                    } else {
                        // y is after the last available value.
                        final int n = values.length - 1;
                        final double y1 = values[n];
                        x = (y - y1) / (d = y1 - values[n-1]) + n;
                    }
                } else {
                    // y is before the first available value.
                    final double y0 = values[0];
                    x = (y - y0) / (d = values[1] - y0);
                }
            }
            if (dstPts != null) {
                dstPts[dstOff] = x;
            }
            return derivate ? new Matrix1(1/d) : null;
        }

        /**
         * Locates by bilinear search and interpolates the <var>x</var> value for the given <var>y</var>.
         */
        @Override
        public double transform(final double y) {
            final double[] values = forward.values;
            int i = Arrays.binarySearch(values, y);
            if (i >= 0) {
                return i;
            } else {
                i = ~i;
                if (i >= 1) {
                    if (i < values.length) {
                        final double y0 = values[i-1];
                        return (y - y0) / (values[i] - y0) + (i-1);
                    } else {
                        // y is after the last available value.
                        final int n = values.length - 1;
                        final double y1 = values[n];
                        return (y - y1) / (y1 - values[n-1]) + n;
                    }
                } else {
                    // y is before the first available value.
                    final double y0 = values[0];
                    return (y - y0) / (values[1] - y0);
                }
            }
        }

        /**
         * Returns the derivative at the given <var>y</var> value.
         */
        @Override
        public double derivative(final double y) {
            final double[] values = forward.values;
            int i = Arrays.binarySearch(values, y);
            if (i < 0) {
                i = ~i;
            }
            i = Math.max(1, Math.min(values.length - 1, i));
            return 1 / (values[i] - values[i-1]);
        }
    }

    /**
     * Computes a hash code value for this transform.
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() ^ Arrays.hashCode(values);
    }

    /**
     * Compares this transform with the given object for equality.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            // No need to compare 'slope' because it is computed from 'values'.
            return Arrays.equals(values, ((LinearInterpolator1D) object).values);
        }
        return false;
    }
}
