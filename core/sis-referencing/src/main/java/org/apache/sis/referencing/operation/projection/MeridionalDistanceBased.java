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

import java.io.IOException;
import java.io.ObjectInputStream;
import org.apache.sis.internal.util.DoubleDouble;

import static java.lang.Math.*;


/**
 * Base class of map projections based on distance along the meridian from equator to latitude φ.
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
abstract class MeridionalDistanceBased extends NormalizedProjection {
    /**
     * {@code false} for using the original formulas as published by EPSG, or {@code true} for using formulas
     * modified using trigonometric identities. Use of trigonometric identities reduces the amount of calls to
     * {@link Math#sin(double)} and similar methods. Snyder 3-34 to 3-39 give the following identities:
     *
     * <pre>
     *     If:     f(φ) = A⋅sin(2φ) + B⋅sin(4φ) + C⋅sin(6φ) + D⋅sin(8φ)
     *     Then:   f(φ) = sin(2φ)⋅(A′ + cos(2φ)⋅(B′ + cos(2φ)⋅(C′ + D′⋅cos(2φ))))
     *     Where:  A′ = A - C
     *             B′ = 2B - 4D
     *             C′ = 4C
     *             D′ = 8D
     * </pre>
     *
     * Similar, but with cosine instead than sin and the addition of a constant:
     *
     * <pre>
     *     If:     f(φ) = A + B⋅cos(2φ) + C⋅cos(4φ) + D⋅cos(6φ) + E⋅cos(8φ)
     *     Then:   f(φ) = A′ + cos(2φ)⋅(B′ + cos(2φ)⋅(C′ + cos(2φ)⋅(D′ + E′⋅cos(2φ))))
     *     Where:  A′ = A - C + E
     *             B′ = B - 3D
     *             C′ = 2C - 8E
     *             D′ = 4D
     *             E′ = 8E
     * </pre>
     */
    private static final boolean ALLOW_TRIGONOMETRIC_IDENTITIES = true;

    /**
     * Coefficients for the formula implemented by the {@link #meridianArc(double, double, double)} method.
     * Values are computed by {@link #computeCoefficients(double)} in constructor or after deserialization.
     * The values depends on the form of equation implemented by {@code meridianArc(…)}. We do not use the
     * form commonly found in publications since a few algebraic operations allow to replace the
     * sin(2φ), sin(4φ), sin(6φ) and sin(8φ) terms by only sin²(φ), which is faster to calculate.
     * See {@link #computeCoefficients(double)} comments in method body for more information.
     */
    private transient double c0, c1, c2, c3, c4;

    /**
     * Coefficients used in inverse transform. Same comment than for {@link #c0}… applies.
     */
    private transient double ci1, ci2, ci3, ci4;

    /**
     * Denominator of <cite>rectifying latitude</cite> equation. The rectifying latitude is computed by
     * µ = M/(1 – ℯ²/4 – 3ℯ⁴/64 – 5ℯ⁶/256 – …)  (Snyder 7-19 with a=1).
     */
    private transient double rld;

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer.
     */
    MeridionalDistanceBased(final Initializer initializer) {
        super(initializer);
        DoubleDouble e1 = initializer.axisLengthRatio();
        e1.ratio_1m_1p();
        computeCoefficients(e1.doubleValue());
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    MeridionalDistanceBased(final MeridionalDistanceBased other) {
        super(other);
        c0  = other.c0;
        c1  = other.c1;
        c2  = other.c2;
        c3  = other.c3;
        c4  = other.c4;
        ci1 = other.ci1;
        ci2 = other.ci2;
        ci3 = other.ci3;
        ci4 = other.ci4;
        rld = other.rld;
    }

    /**
     * Restores transient fields after deserialization.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        double ba = sqrt(1 - eccentricitySquared);
        computeCoefficients((1 - ba) / (1 + ba));
    }

    /**
     * Computes the coefficients in the series expansions from the {@link #eccentricitySquared} value.
     * This method shall be invoked after {@code MeridionalDistanceBased} construction or deserialization.
     *
     * @param  ei  value of  [1 - √(1 - ℯ²)] / [1 + √(1 - ℯ²)]   (Snyder 3-24).
     */
    private void computeCoefficients(final double ei) {
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
         * That more accurate formula is implemented in MeridionalDistanceTest for comparison purposes.
         * Then we transform that formula as below:
         *
         *    1) Multiply by b²/a = (1 - ℯ²). This is done by combining some terms. For example (1 - ℯ²)⋅(1 + ¾ℯ²) =
         *       (1 + ¾ℯ²) - (1ℯ² + ¾ℯ⁴) = 1 - ¼ℯ² - ¾ℯ⁴. Note that we get the first two terms of EPSG formula, which
         *       already include the multiplication by (1 - ℯ²).
         *
         *    2) Application of trigonometric identities:
         *
         *       replace:  f(φ) = A⋅sin(2φ) + B⋅sin(4φ) + C⋅sin(6φ) + D⋅sin(8φ)                             Snyder 3-34
         *       by:       f(φ) = sin(2φ)⋅(A′ + cos(2φ)⋅(B′ + cos(2φ)⋅(C′ + D′⋅cos(2φ))))                   Snyder 3-35
         *       with:       A′ = A - C
         *                   B′ = 2B - 4D
         *                   C′ = 4C
         *                   D′ = 8D
         *
         *    3) Replacement of  sin2φ  by  2⋅sinφ⋅cosφ  and  cos2φ  by  1 - 2sin²φ:
         *
         *                 f(φ) = sinφ⋅cosφ⋅(C₁ + sin²φ⋅(C₂ + sin²φ⋅(C₃ + C₄⋅sin²φ)))
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
        c0 =  -441./0x10000 * e10  +  -175./0x4000  * e8  +    -5./0x100   * e6  +   -3./0x40 * e4  +  -1./4 * e2  +  1;
        c1 =  1575./0x20000 * e10  +   175./0x4000  * e8  +     5./0x100   * e6  +    3./0x40 * e4  +  -3./4 * e2;
        c2 = -2625./0x8000  * e10  +   175./0x6000  * e8  +  5120./0x60000 * e6  +  -15./0x20 * e4;
        c3 =   735./0x800   * e10  +  2240./0x60000 * e8  +   -35./0x60    * e6;
        c4 = -2205./0x1000  * e10  +  -315./0x400   * e8;
     // c6 =   693./0x20000 * e10  omitted for now (not yet used).
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
        rld = (1 - 1./4*e2 - 3./64*e4 - 5./256*e6);       // Part of Snyder 7-19 for computing rectifying latitude.
        final double ei2 = ei*ei;
        final double ei3 = ei*ei2;
        final double ei4 = ei2*ei2;
        ci1 =   657./0x40 * ei4  +    31./4 * ei3  +   21./4 * ei2  +  3./1 * ei;
        ci2 = -5045./0x20 * ei4  +  -151./3 * ei3  +  -21./2 * ei2;
        ci3 =  3291./0x08 * ei4  +   151./3 * ei3;
        ci4 = -1097./0x04 * ei4;
    }

    /**
     * Computes the meridional distance (M) from equator to a given latitude φ.
     * Special cases:
     * <ul>
     *   <li>If <var>φ</var> is 0°, then this method returns 0.</li>
     * </ul>
     *
     * @param  φ     latitude for which to compute the meridional distance, in radians.
     * @param  sinφ  value of sin(φ).
     * @param  cosφ  value of cos(φ).
     * @return meridional distance for the given latitude on an ellipsoid of semi-major axis of 1.
     */
    final double meridianArc(final double φ, final double sinφ, final double cosφ) {
        final double sinφcosφ = cosφ * sinφ;
        final double sinφ2    = sinφ * sinφ;
        return c0*φ + sinφcosφ*(c1 + sinφ2*(c2 + sinφ2*(c3 + sinφ2*c4)));      // TODO: use Math.fma with JDK9.
    }

    /**
     * Gets the derivative of this {@link #meridianArc(double, double, double)} method.
     *
     * @return the derivative at the specified latitude.
     */
    final double dM_dφ(final double sinφ2) {
        return ((((7 - 8*sinφ2)*c4 - 6*c3) * sinφ2
                            + 5*c3 - 4*c2) * sinφ2
                            + 3*c2 - 2*c1) * sinφ2
                            +   c1
                            +   c0;
    }

    /**
     * Computes latitude φ from a meridian distance <var>M</var>.
     *
     * @param  distance  meridian distance for which to compute the latitude.
     * @return the latitude of given meridian distance, in radians.
     * @throws ProjectionException if the iteration does not converge.
     */
    final double inverse(final double distance) throws ProjectionException {
        double    φ  = distance / rld;            // Rectifying latitude µ used as first approximation.
        double sinφ  = sin(φ);
        double cosφ  = cos(φ);
        double sinφ2 = sinφ * sinφ;
        φ += sinφ*cosφ*(ci1 + sinφ2*(ci2 + sinφ2*(ci3 + sinφ2*ci4)));                   // Snyder 3-26.
        return φ;
    }
}
