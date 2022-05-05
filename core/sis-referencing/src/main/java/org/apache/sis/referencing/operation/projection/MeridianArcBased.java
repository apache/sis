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

import org.apache.sis.internal.util.DoubleDouble;

import static java.lang.Math.*;


/**
 * Base class of map projections based on distance along the meridian from equator to latitude φ.
 * Except for some cases like "Sinusoidal equal area", those projections are neither conformal nor equal-area.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @version 1.0
 *
 * @see <a href="https://en.wikipedia.org/wiki/Meridian_arc">Meridian arc on Wikipedia</a>
 *
 * @since 1.0
 * @module
 */
abstract class MeridianArcBased extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6105123637473385555L;

    /**
     * Coefficients for the forward transform implemented by the {@link #distance(double, double, double)} method.
     * Values are computed by the constructor and depend on the form of equation implemented by {@code distance(…)}.
     * We do not use the formulas in the form published by EPSG or Snyder since a few algebraic operations allow to
     * replace the sin(2φ), sin(4φ), sin(6φ) and sin(8φ) terms by sin²(φ), which is faster to calculate.
     * See comments in constructor body for more information.
     */
    private final double cf0, cf1, cf2, cf3, cf4;

    /**
     * Coefficients used in inverse transform. Same comment than for {@link #cf0}… applies.
     */
    private final double ci1, ci2, ci3, ci4;

    /**
     * Denominator of <cite>rectifying latitude</cite> equation. The rectifying latitude is computed by
     * µ = M/(1 – ℯ²/4 – 3ℯ⁴/64 – 5ℯ⁶/256 – …)  (Snyder 7-19 with a=1).
     */
    private final double rµ;

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer.
     */
    MeridianArcBased(final Initializer initializer) {
        super(initializer, null);
        final double e2  = eccentricitySquared;
        final double e4  = e2*e2;
        final double e6  = e2*e4;
        final double e8  = e4*e4;
        final double e10 = e2*e8;
        /*
         * Snyder 3-21 and EPSG guidance notes #7 part 2 (February 2019) give us the following equation:
         *
         *   M = a[(1 – ℯ²/4 – 3ℯ⁴/64  –  5ℯ⁶/256  – …)⋅φ
         *          – (3ℯ²/8 + 3ℯ⁴/32  + 45ℯ⁶/1024 + …)⋅sin2φ
         *                 + (15ℯ⁴/256 + 45ℯ⁶/1024 + …)⋅sin4φ
         *                            – (35ℯ⁶/3072 + …)⋅sin6φ
         *                                         + …]
         *
         * But we do not use this equation as-is. Instead, we start from a series providing two more terms (ℯ⁸ and ℯ¹⁰)
         * from the following source (also available with less terms on https://en.wikipedia.org/wiki/Meridian_arc):
         *
         *   Kawase, Kazushige (2011). A General Formula for Calculating Meridian Arc Length and its Application to Coordinate
         *   Conversion in the Gauss-Krüger Projection. Bulletin of the Geospatial Information Authority of Japan, Vol.59.
         *
         * That more accurate formula is implemented in MeridianArcTest for comparison purposes.
         * Then we transform that formula as below:
         *
         *    1) Multiply by b²/a = (1 - ℯ²). This is done by combining some terms. For example (1 - ℯ²)⋅(1 + ¾ℯ²) =
         *       (1 + ¾ℯ²) - (1ℯ² + ¾ℯ⁴) = 1 - ¼ℯ² - ¾ℯ⁴. Note that we get the first two terms of EPSG formula, which
         *       already include the multiplication by (1 - ℯ²).
         *
         *    2) Application of trigonometric identities:
         *
         *       replace:  f(φ) = A⋅sin(2φ) + B⋅sin(4φ) + C⋅sin(6φ) + D⋅sin(8φ)                             Snyder 3-34
         *       by:       f(φ) = sin(2φ)⋅(A′ + cos(2φ)⋅(B′ + cos(2φ)⋅(C′ + cos(2φ)⋅D′)))                   Snyder 3-35
         *       with:       A′ = A - C
         *                   B′ = 2B - 4D
         *                   C′ = 4C
         *                   D′ = 8D
         *
         *    3) Replacement of  sin2φ  by  2⋅sinφ⋅cosφ  and  cos2φ  by  1 - 2sin²φ:
         *
         *                 f(φ) = sinφ⋅cosφ⋅(C₁ + sin²φ⋅(C₂ + sin²φ⋅(C₃ + sin²φ⋅C₄)))
         *       with:      C₁ = 2⋅(A′ + B′ + C′ + D′)
         *                  C₂ = 2⋅(−2B′ − 4C′ − 6D′)
         *                  C₃ = 2⋅(4C′ + 12D′)
         *                  C₄ = 2⋅(-8D′)
         *
         * They are the coefficients computed below (the C₀ coefficient is not affected by trigonometric identities).
         * We add the terms from smallest values to largest values for accuracy reasons. Most denominators are power
         * of 2, which result in exact representations in IEEE 754 double type. Derivation from Kawase formula can be
         * viewed at: https://svn.apache.org/repos/asf/sis/analysis/Map%20projection%20formulas.ods
         */
        cf0 =  -441./0x10000 * e10  +  -175./0x4000  * e8  +    -5./0x100   * e6  +   -3./0x40 * e4  +  -1./4 * e2  +  1;
        cf1 =  1575./0x20000 * e10  +   175./0x4000  * e8  +     5./0x100   * e6  +    3./0x40 * e4  +  -3./4 * e2;
        cf2 = -2625./0x8000  * e10  +   175./0x6000  * e8  +  5120./0x60000 * e6  +  -15./0x20 * e4;
        cf3 =   735./0x800   * e10  +  2240./0x60000 * e8  +   -35./0x60    * e6;
        cf4 = -2205./0x1000  * e10  +  -315./0x400   * e8;
     // cf5 =   693./0x20000 * e10  omitted for now (not yet used).
        /*
         * Coefficients for inverse transform derived from Snyder 3-26 and EPSG guidance notes:
         *
         *   φ = µ + (3ℯ₁ /  2 – 27ℯ₁³/ 32 + …)⋅sin2µ
         *        + (21ℯ₁²/ 16 – 55ℯ₁⁴/ 32 + …)⋅sin4µ
         *                   + (151ℯ₁³/ 96 + …)⋅sin6µ
         *                  + (1097ℯ₁⁴/512 – …)⋅sin8µ
         *                                 + …
         *
         *   where  ℯ₁ = [1 - √(1 - ℯ²)] / [1 + √(1 - ℯ²)]                          Snyder 3-24
         *          µ  = M/(1 – ℯ²/4 – 3ℯ⁴/64 – 5ℯ⁶/256 – …)                        Snyder 7-19 with a=1
         *
         * µ is the rectifying latitude.
         * Derivation of coefficients used by this class are provided in the above-cited spreadsheet.
         */
        rµ = (1 - 1./4*e2 - 3./64*e4 - 5./256*e6);        // Part of Snyder 7-19 for computing rectifying latitude.
        DoubleDouble e1 = initializer.axisLengthRatio();
        e1.ratio_1m_1p();
        final double ei  = e1.doubleValue();              // Equivalent to [1 - √(1 - ℯ²)] / [1 + √(1 - ℯ²)]   (Snyder 3-24).
        final double ei2 = ei*ei;
        final double ei3 = ei*ei2;
        final double ei4 = ei2*ei2;
        ci1 =   657./0x40 * ei4  +    31./4 * ei3  +   21./4 * ei2  +  3./1 * ei;
        ci2 = -5045./0x20 * ei4  +  -151./3 * ei3  +  -21./2 * ei2;
        ci3 =  3291./0x08 * ei4  +   151./3 * ei3;
        ci4 = -1097./0x04 * ei4;
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    MeridianArcBased(final MeridianArcBased other) {
        super(null, other);
        cf0 = other.cf0;
        cf1 = other.cf1;
        cf2 = other.cf2;
        cf3 = other.cf3;
        cf4 = other.cf4;
        ci1 = other.ci1;
        ci2 = other.ci2;
        ci3 = other.ci3;
        ci4 = other.ci4;
        rµ  = other.rµ;
    }

    /**
     * Computes the distance (M) along meridian arc from equator to a given latitude φ.
     * Special cases:
     *
     * <ul>
     *   <li>If φ is 0°, then this method returns 0.</li>
     *   <li>If φ=+π/2, then this method returns a value slightly smaller than +π/2, depending on the eccentricity.</li>
     *   <li>If φ=-π/2, then this method returns a value slightly greater than -π/2, depending on the eccentricity.</li>
     * </ul>
     *
     * This value is related to <cite>rectifying latitude</cite> by µ = (π/2)⋅(M/{@linkplain #cf0}) (derived from Snyder 3-20).
     *
     * @param  φ     latitude for which to compute the distance, in radians.
     * @param  sinφ  value of sin(φ).
     * @param  cosφ  value of cos(φ).
     * @return distance for the given latitude on an ellipsoid of semi-major axis of 1.
     */
    final double distance(final double φ, final double sinφ, final double cosφ) {
        final double sinφ2 = sinφ * sinφ;
        return cf0*φ + cosφ*sinφ*(cf1 + sinφ2*(cf2 + sinφ2*(cf3 + sinφ2*cf4)));     // TODO: use Math.fma with JDK9.
    }

    /**
     * Gets the derivative of this {@link #distance(double, double, double)} method.
     *
     * @return the derivative at the specified latitude.
     */
    final double dM_dφ(final double sinφ2) {
        return ((((7 - 8*sinφ2)*cf4 - 6*cf3) * sinφ2
                            + 5*cf3 - 4*cf2) * sinφ2
                            + 3*cf2 - 2*cf1) * sinφ2
                            +   cf1
                            +   cf0;
    }

    /**
     * Computes latitude φ from a meridian distance <var>M</var>.
     *
     * @param  distance  meridian distance for which to compute the latitude.
     * @return the latitude of given meridian distance, in radians.
     */
    final double latitude(final double distance) {
        double    φ  = distance / rµ;             // Rectifying latitude µ used as first approximation.
        double sinφ  = sin(φ);
        double sinφ2 = sinφ * sinφ;
        φ += cos(φ)*sinφ*(ci1 + sinφ2*(ci2 + sinφ2*(ci3 + sinφ2*ci4)));                 // Snyder 3-26.
        /*
         * We could improve accuracy by continuing from here with Newton's iterative method
         * (see MeridianArcTest.inverse(…) for implementation). However those iterations requires
         * calls to distance(double, …), which is itself an approximation based on series expansion.
         * Consequently the accuracy of iterative method can not be better than distance(…) accuracy.
         */
        return φ;
    }
}
