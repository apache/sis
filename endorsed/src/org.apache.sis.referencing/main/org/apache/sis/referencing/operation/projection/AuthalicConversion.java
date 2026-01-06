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

import static java.lang.Math.*;
import org.apache.sis.referencing.internal.Resources;
import static org.apache.sis.math.MathFunctions.atanh;


/**
 * Base class of projections doing conversions between <i>geodetic</i> latitude and <i>authalic</i> latitude.
 * This is used by <i>equal-area</i> projections such as {@link AlbersEqualArea} and {@link CylindricalEqualArea}.
 * However, not all equal-area projections extend this base class, and conversely not all sub-classes are equal-area.
 * For example, the {@link Sinusoidal} projection, despite being equal-area, uses different formulas.
 *
 * <p>Note that no projection can be both conformal and equal-area. So the formulas in this class
 * are usually mutually exclusive with formulas in {@link ConformalProjection} class.</p>
 *
 * <h2>Note on class naming</h2>
 * Lee (1944) defines an <dfn>authalic map projection</dfn> to be one in which at any point the scales in
 * two orthogonal directions are inversely proportional. Those map projections have a constant areal scale.
 * However, this {@code AuthalicConversion} is <strong>not</strong> necessarily an authalic projection.
 * Subclasses may want to use the latitude conversion formulas for other purposes.
 *
 * <h2>References</h2>
 * <p>Lee, L. P. "The Nomenclature and Classification of Map Projections." Empire Survey Review 7, 190-200, 1944.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class AuthalicConversion extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5880625564193782957L;

    /**
     * The threshold value of {@link #eccentricity} at which we consider the accuracy of the
     * series expansion insufficient. This threshold is determined empirically with the help
     * of the {@code EqualAreaProjectionTest.searchThreshold()} method in the test directory.
     *
     * <p>Note: WGS84 eccentricity is about 0.082.</p>
     */
    private static final double ECCENTRICITY_THRESHOLD = 0.1;

    /**
     * Coefficients of the first terms in the series expansion of the reverse projection.
     * Values of those coefficients depend only on {@linkplain #eccentricity eccentricity} value.
     * The series expansion is published under the following form, where β is the <i>authalic latitude</i>:
     *
     *     <blockquote>φ = c₂⋅sin(2β) + c₄⋅sin(4β) + c₈⋅sin(6β)</blockquote>
     *
     * However, we rewrite above series expansion for taking advantage of trigonometric identities.
     * The equation become (with different <var>c</var> values):
     *
     *     <blockquote>sin(2β)⋅(c₂ + cos(2β)⋅(c₄ + cos(2β)⋅c₆))</blockquote>
     *
     * <h4>Serialization note</h4>
     * We do not strictly need to serialize those fields since they could be computed after deserialization.
     * But we serialize them anyway in order to simplify a little bit this class (it allows us to keep those
     * fields final) and because values computed after deserialization could be slightly different than the
     * ones computed after construction if a future version of the constructor uses the double-double values
     * provided by {@link Initializer}.
     */
    private final double c2β, c4β, c6β;

    /**
     * Value of {@link #qm(double)} function (part of Snyder equation (3-12)) at pole (sinφ = 1).
     * The <var>q</var><sub>p</sub> constant is defined by EPSG guidance note as:
     *
     * <blockquote>(1 – ℯ²)⋅([1 / (1 – ℯ²)] – {[1/(2ℯ)] ln [(1 – ℯ) / (1 + ℯ)]})</blockquote>
     *
     * But in this class, we omit the (1 – ℯ²) factor.
     *
     * <h4>Spherical case</h4>
     * In the spherical case (ℯ=0), the value is exactly 2.
     */
    final double qmPolar;

    /**
     * {@code true} if {@link #eccentricity} is zero.
     */
    final boolean isSpherical;

    /**
     * {@code true} if the {@link #eccentricity} value is greater than or equals to the eccentricity threshold,
     * in which case the {@link #φ(double)} method will need to use an iterative method.
     */
    private final boolean useIterations;

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer,
     * or from the parameters already computed by another projection.
     * Exactly one of {@code initializer} or {@code other} shall be non-null.
     *
     * @param initializer  the initializer for computing map projection internal parameters, or {@code null}.
     * @param other        the other projection from which to compute parameters, or {@code null}.
     */
    AuthalicConversion(final Initializer initializer, final NormalizedProjection other) {
        super(initializer, other);
        isSpherical = (eccentricitySquared == 0);
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
        c2β     = fma(e2, 2./3, fma(e4,   3./5,  e6*(    4./7)));
        c4β     =               fma(e4, -23./45, e6*(-3028./2835));
        c6β     =                                e6*( 1522./2835);
        qmPolar = qm(1);
        useIterations = (eccentricity >= ECCENTRICITY_THRESHOLD);
    }

    /**
     * Creates a new projection initialized to the values of the given one. This constructor may be invoked after
     * we determined that the default implementation can be replaced by another one, for example using spherical
     * formulas instead of the ellipsoidal ones. This constructor allows to transfer all parameters to the new
     * instance without recomputing them.
     */
    AuthalicConversion(final AuthalicConversion other) {
        super(null, other);
        c2β           = other.c2β;
        c4β           = other.c4β;
        c6β           = other.c6β;
        qmPolar       = other.qmPolar;
        isSpherical   = other.isSpherical;
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
     *   <li>Output of the same sign as input</li>
     *   <li>q(-sinφ) = -q(sinφ)</li>
     *   <li>q(0) = 0</li>
     * </ul>
     *
     * <h4>Spherical case</h4>
     * In the spherical case, <var>q</var> = 2⋅sinφ.
     * We pay the cost of checking for the spherical case in each method invocation because otherwise,
     * users creating their own map projection subclasses could get a non-working implementation.
     *
     * @param  sinφ  the sine of the geodetic latitude for which <var>q</var> is calculated.
     * @return <var>q</var> from Snyder equation (3-12).
     */
    final double qm(final double sinφ) {
        /*
         * Check for zero eccentricity is required because `qm(sinφ)` would
         * simplify to sinφ + atanh(0) / 0 == sinφ + 0/0, thus producing NaN.
         */
        if (isSpherical) return 2*sinφ;
        final double ℯsinφ = eccentricity * sinφ;
        return sinφ/(1 - ℯsinφ*ℯsinφ) + atanh(ℯsinφ)/eccentricity;
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
     * Converts the sine of geodetic latitude to the sin of authalic latitude.
     * This is defined by {@code qm(sinφ) / qmPolar}.
     *
     * @param  sinφ  the sine of the geodetic latitude.
     * @return the sine of the authalic latitude.
     */
    final double sinβ(double sinφ) {
        // Edited copy of `qm(double)` method.
        if (isSpherical) return sinφ;
        sinφ *= eccentricity;           // Become `ℯsinφ` from here.
        return (sinφ/(1 - sinφ*sinφ) + atanh(sinφ)) / (eccentricity * qmPolar);
    }

    /**
     * Computes the latitude using equation 3-18 from Snyder, followed by iterative resolution of Snyder 3-16.
     * In theory, the series expansion given by equation 3-18 (φ ≈ c₂⋅sin(2β) + c₄⋅sin(4β) + c₈⋅sin(8β)) should
     * be used in replacement of the iterative method. However, in practice the series expansion seems to not
     * have a sufficient number of terms for achieving the centimetric precision, so we "finish" it by the
     * iterative method. The series expansion is nevertheless useful for reducing the number of iterations.
     *
     * <h4>Relationship with northing</h4>
     * The simplest projection using this formula is the {@link CylindricalEqualArea} projection.
     * In that case, sin(β) = <var>y</var> / {@link #qmPolar}.
     *
     * <h4>Spherical case</h4>
     * In the spherical case, this method returns {@code β = asin(sinβ)}. This method does not check for
     * that simplification for the spherical case. This optimization is left to the caller if desired.
     *
     * @param  sinβ  sine of the authalic latitude.
     * @return the geodetic latitude in radians.
     */
    final double φ(final double sinβ) throws ProjectionException {
        final double sinβ2 = sinβ * sinβ;
        final double β = asin(sinβ);
        /*
         * Snyder 3-18, but rewriten using trigonometric identities
         * in order to avoid multiple calls to sin(double) method.
         *
         *   φ = β + cos(β)*sinβ*(c2β + sinβ2*(c4β + sinβ2*c6β))
         */
        double φ = fma(fma(fma(sinβ2, c6β, c4β), sinβ2, c2β), cos(β)*sinβ, β);
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
            final double y = qmPolar * sinβ;
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
            final double y = qmPolar * sinβ;        // Do not use β because it may be NaN.
            return copySign(PI/2, y);               // Value is at a pole.
        }
        if (!(as < 1)) {                            // Use `!` for catching NaN.
            return Double.NaN;                      // Value "after" the pole.
        }
        // Value should have converged but did not.
        throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
    }
}
