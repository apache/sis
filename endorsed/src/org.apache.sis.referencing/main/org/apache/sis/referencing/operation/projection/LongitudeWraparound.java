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
package org.apache.sis.referencing.operation.projection;

import java.util.Optional;
import java.io.Serializable;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform2D;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.DomainDefinition;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.measure.Longitude;
import org.apache.sis.referencing.ExportableTransform;


/**
 * If the scaled longitude θ=n⋅λ is outside the [−n⋅π … n⋅π] range, maybe shifts θ to that range.
 * This transform intentionally does <strong>not</strong> force θ to be inside that range in all cases.
 * We avoid explicit wraparounds as much as possible (as opposed to implicit wraparounds performed by
 * trigonometric functions) because they tend to introduce discontinuities. We perform wraparounds only
 * when necessary for the problem of area crossing the anti-meridian (±180°).
 *
 * <div class="note"><b>Example:</b>
 * a CRS for Alaska may have the central meridian at λ₀=−154° of longitude. If the point to project is
 * at λ=177° of longitude, calculations will be performed with Δλ=331° while the correct value that we
 * need to use is Δλ=−29°.</div>
 *
 * In order to avoid wraparound operations as much as possible, we test only the bound where anti-meridian
 * problem may happen; no wraparound will be applied for the opposite bound. Furthermore, we add or subtract
 * 360° only once. Even if the point did many turns around the Earth, the 360° shift will still be applied
 * at most once. The desire to apply the minimal number of shifts is the reason why we do not use
 * {@link Math#IEEEremainder(double, double)}.
 *
 * <h2>When to use</h2>
 * Many map projections implicitly wraparound longitude values through the use of trigonometric functions
 * ({@code sin(λ)}, {@code cos(λ)}, <i>etc</i>). For those map projections, the wraparound is unconditional
 * and this {@code LongitudeWraparound} class is not needed. This class is used only when the wraparound is
 * not implicitly done and the central meridian is not zero. The latter condition is because subtraction of
 * central meridian may cause longitude values to go outside the −180° … +180° range.
 *
 * <p>This transform is hidden in WKT (it does not appear as a concatenation).</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.referencing.operation.transform.WraparoundTransform
 * @see <a href="https://issues.apache.org/jira/browse/SIS-486">SIS-486</a>
 */
final class LongitudeWraparound extends AbstractMathTransform2D implements Serializable, ExportableTransform {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4658152274068444690L;

    /**
     * The actual map projection to execute after the longitude wraparound.
     */
    final NormalizedProjection projection;

    /**
     * A bound of the [−n⋅π … n⋅π] range which, if exceeded, should cause wraparound.
     * Some (not all) θ = n⋅λ values need to be shifted inside that range before to
     * use them in trigonometric functions.
     *
     * <p>The sign is significant. A negative value means that the wraparound is applied
     * only on longitudes less than −180°. A positive value means that the wraparound is
     * applied only on longitudes greater than +180°.</p>
     */
    final double bound;

    /**
     * Whether the bound is on the side of negative longitudes. This is {@code bound < 0}.
     * This field is trivial in {@code LongitudeWraparound} case, but more important in
     * {@link Inverse} case because {@code rotation - bound} may cancel to zero.
     */
    final boolean negative;

    /**
     * The inverse of this operation.
     *
     * @see #inverse()
     */
    private final Inverse inverse;

    /**
     * Creates a new transform for wrapping around the longitude values before a map projection.
     *
     * @param  projection  the actual map projection to execute after the longitude wraparound.
     * @param  bound       one bound of the [−n⋅π … n⋅π] range, on the side where wraparound needs to be applied.
     * @param  rotation    longitude rotation applied by the normalization matrix after conversion to projection domain.
     */
    LongitudeWraparound(final NormalizedProjection projection, final double bound, final double rotation) {
        this.projection = projection;
        this.bound = bound;
        negative = bound < 0;
        inverse = new Inverse(this, rotation - bound);
    }

    /**
     * Returns a bound of the [−n⋅π … n⋅π] domain where <var>n</var> is a map projection dependent factor.
     * The factor is inferred from the {@link NormalizedProjection#context}.
     *
     * @param  normalize  the normalization matrix of the projection for which to get a bound.
     * @param  negative   {@code true} for the −180° bound, or {@code false} for the +180° bound.
     * @return a bound of the [−n⋅π … n⋅π] range.
     */
    static double boundOfScaledLongitude(final MatrixSIS normalize, final boolean negative) {
        DoubleDouble bound = DoubleDouble.of(normalize.getNumber(0, 0), true);
        bound = bound.multiply(negative ? Longitude.MIN_VALUE : Longitude.MAX_VALUE, false);
        return bound.doubleValue();
    }

    /**
     * Returns the ranges of coordinate values which can be used as inputs.
     * The {@link #projection} domain is used verbatim, without wraparound adjustment.
     *
     * @param  criteria  controls the definition of transform domain.
     * @return estimation of a domain where this transform is considered numerically applicable.
     */
    @Override
    public Optional<Envelope> getDomain(DomainDefinition criteria) throws TransformException {
        return projection.getDomain(criteria);
    }

    /**
     * Returns the parameter descriptors for this math transform, or {@code null} if unknown.
     * Delegates to {@link #projection} since this {@code LongitudeWraparound} is hidden.
     * This is used by WKT formatting.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return projection.getParameterDescriptors();
    }

    /**
     * Returns the parameter values for this math transform, or {@code null} if unknown.
     * Delegates to {@link #projection} since this {@code LongitudeWraparound} is hidden.
     * This is used by WKT formatting.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return projection.getParameterValues();
    }

    /**
     * Returns the parameters for a sequence of <i>normalize</i> → {@code this} → <i>denormalize</i>.
     * Delegates to {@link #projection} since this {@code LongitudeWraparound} is hidden.
     * This is used by WKT formatting.
     */
    @Override
    protected ContextualParameters getContextualParameters() {
        return projection.getContextualParameters();
    }

    /**
     * Transforms a single coordinate tuple in an array, and optionally computes the transform derivative.
     * The wraparound is applied, if needed, on the longitude value before to delegate to {@link #projection}.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff, double[] dstPts, int dstOff, final boolean derivate)
            throws TransformException
    {
        final double λ = srcPts[srcOff];
        if (negative ? λ < bound : λ > bound) {
            if (dstPts == null) {
                dstPts = new double[DIMENSION];
                dstOff = 0;
            }
            dstPts[dstOff+1] = srcPts[srcOff+1];        // Must be first.
            dstPts[dstOff  ] = λ - 2*bound;
            return projection.transform(dstPts, dstOff, dstPts, dstOff, derivate);
        } else {
            return projection.transform(srcPts, srcOff, dstPts, dstOff, derivate);
        }
    }

    /**
     * Transforms a list of coordinate tuples. This method is provided for efficiently transforming many points.
     * Wraparound is applied on all longitude values before to delegate to {@link #projection}.
     */
    @Override
    public void transform(final double[] srcPts, final int srcOff,
                          final double[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        if (srcPts != dstPts || srcOff != dstOff) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * DIMENSION);
        }
        final double period = 2*bound;
        final int stop = dstOff + numPts * DIMENSION;
        for (int i=dstOff; i<stop; i+=DIMENSION) {
            final double λ = dstPts[i];
            if (negative ? λ < bound : λ > bound) {
                dstPts[i] = λ - period;
            }
        }
        projection.transform(dstPts, dstOff, dstPts, dstOff, numPts);
    }

    /**
     * Returns the inverse transform of this object.
     */
    @Override
    public MathTransform2D inverse() throws NoninvertibleTransformException {
        return inverse;
    }

    @Override
    public String toECMAScript() throws UnsupportedOperationException {
        return projection.toECMAScript();
    }

    /**
     * Longitude wraparound applied on reverse projection.
     * This is a copy of {@code NormalizedProjection.Inverse} with longitude wraparound added after conversion.
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    private static final class Inverse extends AbstractMathTransform2D.Inverse implements Serializable, ExportableTransform {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -543869926271003589L;

        /**
         * The projection to reverse, which is the enclosing transform.
         */
        private final LongitudeWraparound forward;

        /**
         * {@link LongitudeWraparound#bound} with opposite sign and translated by the longitude rotation.
         * This is the bound that the reverse projection needs to use before longitude rotation.
         * This bound <strong>shall not</strong> be used for period computation.
         */
        private final double bound;

        /**
         * Creates a reverse projection for the given forward projection.
         */
        Inverse(final LongitudeWraparound forward, final double bound) {
            this.forward = forward;
            this.bound = bound;
        }

        /**
         * Returns the inverse of this math transform, which is the forward projection.
         */
        @Override
        public MathTransform2D inverse() {
            return forward;
        }

        /**
         * Reverse projects the specified {@code srcPts} and stores the result in {@code dstPts}.
         * If the derivative has been requested, then this method will delegate the derivative
         * calculation to the enclosing class and inverts the resulting matrix.
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                      double[] dstPts,       int dstOff,
                                final boolean derivate) throws TransformException
        {
            if (derivate && dstPts == null) {
                dstPts = new double[DIMENSION];
                dstOff = 0;
            }
            forward.projection.inverseTransform(srcPts, srcOff, dstPts, dstOff);
            final Matrix d = derivate ? Matrices.inverse(forward.transform(dstPts, dstOff, null, 0, true)) : null;
            final double λ = dstPts[dstOff];
            if (forward.negative ? λ > bound : λ < bound) {          // Interpretation of `negative` is inversed.
                dstPts[dstOff] = λ + 2*forward.bound;
            }
            return d;
        }

        /**
         * Inverse transforms an arbitrary number of coordinate tuples. This method optimizes the
         * case where conversions can be applied by a loop with indices in increasing order.
         */
        @Override
        public void transform(final double[] srcPts, int srcOff,
                              final double[] dstPts, int dstOff, int numPts) throws TransformException
        {
            if (srcPts == dstPts && srcOff < dstOff) {
                super.transform(srcPts, srcOff, dstPts, dstOff, numPts);
            } else {
                final double period = 2 * forward.bound;
                while (--numPts >= 0) {
                    forward.projection.inverseTransform(srcPts, srcOff, dstPts, dstOff);
                    final double λ = dstPts[dstOff];
                    if (forward.negative ? λ > bound : λ < bound) {     // Interpretation of `negative` is inversed.
                        dstPts[dstOff] = λ + period;
                    }
                    srcOff += DIMENSION;
                    dstOff += DIMENSION;
                }
            }
        }

        @Override
        public String toECMAScript() throws UnsupportedOperationException {
            return forward.projection.toECMAScript(true);
        }
    }

    /*
     * We do not implement `tryConcatenate` yet because the result of invoking `projection.tryConcatenate(…)`
     * is either null or a linear transform. In the latter case, the linear transform cannot be wrapped by
     * this `longitudeWraparound` class. Even if we could, it would block concatenation with surrounding
     * affine transforms. We have no easy solution for now.
     */

    /**
     * Computes a hash value for this transform.
     */
    @Override
    protected int computeHashCode() {
        return projection.hashCode() + Double.hashCode(bound);
    }

    /**
     * Compares the specified object with this math transform for equality.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object instanceof LongitudeWraparound) {
            final LongitudeWraparound that = (LongitudeWraparound) object;
            return Numerics.epsilonEqual(bound, that.bound, mode)
                      && projection.equals(that.projection, mode);
        }
        return false;
    }
}
