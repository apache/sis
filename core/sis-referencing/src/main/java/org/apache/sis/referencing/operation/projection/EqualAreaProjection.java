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

import org.apache.sis.internal.referencing.Resources;

import static java.lang.Math.*;
import static org.apache.sis.math.MathFunctions.atanh;


/**
 * Base class of {@link AlbersEqualArea} and {@link CylindricalEqualArea} projections.
 * Those projections have in common the property of being <cite>equal-area</cite>.
 * However we do not put this base class in public API because not all equal-area projections extend this base class.
 * For example the {@link Sinusoidal} projection, despite being equal-area, uses different formulas.
 *
 * <p>Note that no projection can be both conformal and equal-area. This restriction is implemented in class
 * hierarchy with {@link ConformalProjection} and {@link EqualAreaProjection} being two distinct classes.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
abstract class EqualAreaProjection extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2221537810038553082L;

    /**
     * The threshold value of {@link #eccentricity} at which we consider the accuracy of the
     * series expansion insufficient. This threshold is determined empirically with the help
     * of the {@code EqualAreaProjectionTest.searchThreshold()} method in the test directory.
     *
     * <p>Note: WGS84 eccentricity is about 0.082.</p>
     */
    private static final double ECCENTRICITY_THRESHOLD = 0.1;

    /**
     * Coefficients of the first terms in the series expansion of the inverse projection.
     * Values of those coefficients depend only on {@linkplain #eccentricity eccentricity} value.
     * The series expansion is published under the following form, where β is the <cite>authalic latitude</cite>:
     *
     *     <blockquote>φ = c₂⋅sin(2β) + c₄⋅sin(4β) + c₈⋅sin(6β)</blockquote>
     *
     * However we rewrite above series expansion for taking advantage of trigonometric identities.
     * The equation become (with different <var>c</var> values):
     *
     *     <blockquote>sin(2β)⋅(c₂ + cos(2β)⋅(c₄ + cos(2β)⋅c₆))</blockquote>
     *
     * <div class="note"><b>Serialization note:</b>
     * we do not strictly need to serialize those fields since they could be computed after deserialization.
     * Bu we serialize them anyway in order to simplify a little bit this class (it allows us to keep those
     * fields final) and because values computed after deserialization could be slightly different than the
     * ones computed after construction if a future version of the constructor uses the double-double values
     * provided by {@link Initializer}.</div>
     */
    private final double c2β, c4β, c6β;

    /**
     * Value of {@link #qm(double)} function (part of Snyder equation (3-12)) at pole (sinφ = 1).
     */
    private final double qmPolar;

    /**
     * {@code true} if the {@link #eccentricity} value is greater than or equals to the eccentricity threshold,
     * in which case the {@link #φ(double)} method will need to use an iterative method.
     */
    private final boolean useIterations;

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer.
     *
     * @param  initializer  the initializer for computing map projection internal parameters.
     */
    EqualAreaProjection(final Initializer initializer) {
        super(initializer);
        final double e2 = eccentricitySquared;
        final double e4 = e2 * e2;
        final double e6 = e2 * e4;
        /*
         * Published equation is of the following form:
         *
         *     φ  =  β + A⋅sin(2β) + B⋅sin(4β) + C⋅sin(6β)                  (part of Snyder 3-34)
         *
         * We rewrite as:
         *
         *     φ  =  β + sin(2β)⋅(A′ + cos(2β)⋅(B′ + cos(2β)⋅C′))           (part of Snyder 3-35)
         *     A′ =  A - C
         *     B′ =  2B - 4D
         *     C′ =  4C
         *
         * Published coefficients are:
         *
         *     A  =  517/5040  ⋅ℯ⁶  +  31/180 ⋅ℯ⁴  +  ℯ²/3
         *     B  =  251/3780  ⋅ℯ⁶  +  23/360 ⋅ℯ⁴
         *     C  =  761/45360 ⋅ℯ⁶
         *
         * We replace A B C D by A′ B′ C′ D′ using Snyder 3-35, then replace sin2β by 2sinβ⋅cosβ
         * and cos2β by 1 – 2sin²β. The result are coefficients below. For each line, we add the
         * smallest values first in order to reduce rounding errors. The algebraic changes are
         * in https://svn.apache.org/repos/asf/sis/analysis/Map%20projection%20formulas.ods.
         */
        c2β     =      4./7    * e6  +    3./5  * e4  +  2./3 * e2;
        c4β     =  -3028./2835 * e6  +  -23./45 * e4;
        c6β     =   1522./2835 * e6;
        qmPolar = qm(1);
        useIterations = (eccentricity >= ECCENTRICITY_THRESHOLD);
    }

    /**
     * Creates a new projection initialized to the values of the given one. This constructor may be invoked after
     * we determined that the default implementation can be replaced by an other one, for example using spherical
     * formulas instead than the ellipsoidal ones. This constructor allows to transfer all parameters to the new
     * instance without recomputing them.
     */
    EqualAreaProjection(final EqualAreaProjection other) {
        super(other);
        c2β     = other.c2β;
        c4β     = other.c4β;
        c6β     = other.c6β;
        qmPolar = other.qmPolar;
        useIterations = other.useIterations;
    }

    /**
     * Calculates <strong>part</strong> of <var>q</var> from Snyder equation (3-12).
     * In order to get the <var>q</var> function, this method output must be multiplied
     * by <code>(1 - {@linkplain #eccentricitySquared})</code>.
     *
     * <p>The <var>q</var> variable is named <var>α</var> in EPSG guidance notes.</p>
     *
     * <p>This equation has the following properties:</p>
     *
     * <ul>
     *   <li>Input in the [-1 … +1] range</li>
     *   <li>Output multiplied by {@code (1 - ℯ²)} in the [-2 … +2] range</li>
     *   <li>Output of the same sign than input</li>
     *   <li>q(-sinφ) = -q(sinφ)</li>
     *   <li>q(0) = 0</li>
     * </ul>
     *
     * In the spherical case, <var>q</var> = 2⋅sinφ.
     *
     * @param  sinφ  the sine of the latitude <var>q</var> is calculated for.
     * @return <var>q</var> from Snyder equation (3-12).
     */
    final double qm(final double sinφ) {
        /*
         * Check for zero eccentricity is required because qm_ellipsoid(sinφ) would
         * simplify to sinφ + atanh(0) / 0 == sinφ + 0/0, thus producing NaN.
         */
        return (eccentricity == 0) ? 2*sinφ : qm_ellipsoid(sinφ);
    }

    /**
     * Same as {@link #qm(double)} but without check about whether the map projection is a spherical case.
     * It is caller responsibility to ensure that this method is not invoked in the spherical case, since
     * this implementation does not work in such case.
     *
     * @param  sinφ  the sine of the latitude <var>q</var> is calculated for.
     * @return <var>q</var> from Snyder equation (3-12).
     */
    final double qm_ellipsoid(final double sinφ) {
        final double ℯsinφ = eccentricity * sinφ;
        return sinφ / (1 - ℯsinφ*ℯsinφ) + atanh(ℯsinφ) / eccentricity;
    }

    /**
     * Gets the derivative of the {@link #qm(double)} method.
     * Callers must multiply the returned value by <code>(1 - {@linkplain #eccentricitySquared})</code>
     * in order to get the derivative of Snyder equation (3-12).
     *
     * @param  sinφ  the sine of latitude.
     * @param  cosφ  the cosines of latitude.
     * @return the {@code qm} derivative at the specified latitude.
     */
    final double dqm_dφ(final double sinφ, final double cosφ) {
        final double t = 1 - eccentricitySquared*(sinφ*sinφ);
        return 2*cosφ / (t*t);
    }

    /**
     * Computes the latitude using equation 3-18 from Snyder, followed by iterative resolution of Snyder 3-16.
     * In theory, the series expansion given by equation 3-18 (φ ≈ c₂⋅sin(2β) + c₄⋅sin(4β) + c₈⋅sin(8β)) should
     * be used in replacement of the iterative method. However in practice the series expansion seems to not
     * have a sufficient amount of terms for achieving the centimetric precision, so we "finish" it by the
     * iterative method. The series expansion is nevertheless useful for reducing the number of iterations.
     *
     * @param  y  in the cylindrical case, this is northing on the normalized ellipsoid.
     * @return the latitude in radians.
     */
    final double φ(final double y) throws ProjectionException {
        final double sinβ = y / qmPolar;
        final double sinβ2 = sinβ * sinβ;
        final double β = asin(sinβ);
        /*
         * Snyder 3-18, but rewriten using trigonometric identities in order to avoid
         * multiple calls to sin(double) method.
         */
        double φ = β + cos(β)*sinβ*(c2β + sinβ2*(c4β + sinβ2*c6β));
        if (useIterations) {
            /*
             * Mathematical note: Snyder 3-16 gives q/(1-ℯ²) instead of y in the calculation of Δφ below.
             * For Cylindrical Equal Area projection, Snyder 10-17 gives  q = (qPolar⋅sinβ), which simplifies
             * as y.
             *
             * For Albers Equal Area projection, Snyder 14-19 gives  q = (C - ρ²n²/a²)/n,  which we rewrite
             * as  q = (C - ρ²)/n  (see comment in AlbersEqualArea.inverseTransform(…) for the mathematic).
             * The y value given to this method is y = (C - ρ²) / (n⋅(1-ℯ²)) = q/(1-ℯ²), the desired value.
             */
            for (int i=0; i<MAXIMUM_ITERATIONS; i++) {
                final double sinφ  = sin(φ);
                final double cosφ  = cos(φ);
                final double ℯsinφ = eccentricity * sinφ;
                final double ome   = 1 - ℯsinφ*ℯsinφ;
                final double Δφ    = ome*ome/(2*cosφ) * (y - sinφ/ome - atanh(ℯsinφ)/eccentricity);
                φ += Δφ;
                if (abs(Δφ) <= ITERATION_TOLERANCE) {
                    return φ;
                }
            }
        } else if (!Double.isNaN(φ)) {
            return φ;
        }
        /*
         * In the Albers Equal Area discussion, Snyder said that above algorithm does not converge if
         *
         *   q = ±(1 - (1-ℯ²)/(2ℯ) ⋅ ln((1-ℯ)/(1+ℯ)))
         *
         * which we rewrite as
         *
         *   q = ±(1 + (1-ℯ²)⋅atanh(ℯ)/ℯ)
         *
         * Given that y = q/(1-ℯ²)  (see above comment), we rewrite as
         *
         *   y  =  ±(1/(1-ℯ²) + atanh(ℯ)/ℯ)  =  ±qmPolar
         *
         * which implies  sinβ = ±1. This is consistent with Snyder discussion of Cylndrical Equal Area
         * projection, where he said exactly that about the same formula (that it does not converge for
         * β = ±90°). In both case, Snyder said that the result is φ = β, with the same sign.
         */
        final double as = abs(sinβ);
        if (abs(as - 1) < ANGULAR_TOLERANCE) {
            return copySign(PI/2, y);               // Value is at a pole.
        }
        if (as >= 1 || Double.isNaN(y)) {
            return Double.NaN;                      // Value "after" the pole.
        }
        // Value should have converged but did not.
        throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
    }
}
