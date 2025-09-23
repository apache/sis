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
package org.apache.sis.referencing;

import static java.lang.Math.*;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.Debug;


/**
 * Performs geodetic calculations on an ellipsoid. This class overrides the spherical
 * formulas implemented in the parent class, replacing them by ellipsoidal formulas.
 * The methods for direct and inverse geodesic problem use the formulas described in
 * the following publication:
 *
 * <blockquote>
 * Charles F. F. Karney, 2013.
 * <a href="https://doi.org/10.1007/s00190-012-0578-z">Algorithms for geodesics</a>, SRI International.
 * </blockquote>
 *
 * The following symbols are used with the same meaning as in Karney's article,
 * except λ₁₂ which is represented by ∆λ:
 *
 * <ul>
 *   <li><var>a</var>:  equatorial radius of the ellipsoid of revolution.</li>
 *   <li><var>b</var>:  polar semi-axis of the ellipsoid of revolution.</li>
 *   <li><var>ℯ</var>:  first  eccentricity: ℯ  = √[(a²-b²)/a²].</li>
 *   <li><var>ℯ′</var>: second eccentricity: ℯ′ = √[(a²-b²)/b²].</li>
 *   <li><var>n</var>:  third flattening:    n  = (a-b)/(a+b).</li>
 *   <li><var>E</var>:  the point at which the geodesic crosses the equator in the northward direction.</li>
 *   <li><var>P</var>:  the start point (P₁) or end point (P₂).</li>
 *   <li><var>∆λ</var>: longitude difference between start point and end point (λ₁₂ in Karney).</li>
 *   <li><var>β</var>:  reduced latitude, related to φ geodetic latitude on the ellipsoid.</li>
 *   <li><var>ω</var>:  spherical longitude, related to λ geodetic longitude on the ellipsoid.</li>
 *   <li><var>σ</var>:  spherical arc length, related to distance <var>s</var> on the ellipsoid.</li>
 * </ul>
 *
 * Suffix 1 in variable names denotes values computed using starting point (P₁) and starting azimuth (α₁)
 * while suffix 2 denotes values computed using ending point (P₂) and ending azimuth (α₂).
 * All angular values stored in this class are in radians.
 *
 * <h2>Limitations</h2>
 * Current implementation is still unable to compute the geodesics in some cases.
 * In particular, calculation may fail for antipodal points.
 * See <a href="https://issues.apache.org/jira/browse/SIS-467">SIS-467</a>.
 *
 * <p>If the following cases where more than one geodesics exist, current implementation returns an arbitrary one:</p>
 * <ul>
 *   <li>Coincident points (distance is zero but azimuths can be anything).</li>
 *   <li>Starting point and ending points are at opposite poles (there are infinitely many geodesics).</li>
 *   <li>φ₁ = -φ₂ and ∆λ is close to 180° (two geodesics may exist).</li>
 *   <li>∆λ = ±180° (two geodesics may exist).</li>
 * </ul>
 *
 * @author  Matthieu Bastianelli (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
class GeodesicsOnEllipsoid extends GeodeticCalculator {
    /**
     * Whether to include code used for JUnit tests only. This field can be set to
     * {@code true} during debugging and should be set to {@code false} in releases.
     *
     * @see #snapshot()
     */
    @Debug
    static final boolean STORE_LOCAL_VARIABLES = false;

    /**
     * Accuracy threshold for iterative computations, in radians.
     * This accuracy must be at least {@value Formulas#ANGULAR_TOLERANCE} degrees (converted to radians) for
     * conformance with the accuracy reported in {@link GeodeticCalculator} class javadoc. Actually we take
     * a finer accuracy than above value in order to met the accuracy of numbers published in Karney (2013),
     * but this extra accuracy is not guaranteed because it is hard to achieve in all cases.
     *
     * <p><b>Note:</b> when the iteration loop detects that it reached this requested accuracy, the loop
     * completes the iteration step which was in progress. Consequently, the final accuracy is one iteration
     * better than the accuracy computed from this value.</p>
     *
     * <h4>Maintenance</h4>
     * If this value is modified, the effect can be verified by executing the {@code GeodesicsOnEllipsoidTest}
     * methods that compare computed values against Karney's tables. The {@link GeodeticCalculator} javadoc may
     * need to be edited accordingly.
     */
    static final double ITERATION_TOLERANCE = (Formulas.ANGULAR_TOLERANCE / 20) * (PI/180);

    /**
     * Difference between ending point and antipode of starting point for considering them as nearly antipodal.
     * This is used only for finding an approximate position before to start iteration using Newton's method,
     * so it is okay if the antipodal approximation has some inaccuracy.
     */
    private static final double NEARLY_ANTIPODAL_Δλ = 0.25 * (PI/180);

    /**
     * The square of eccentricity: ℯ² = (a²-b²)/a² where
     * <var>ℯ</var> is the eccentricity,
     * <var>a</var> is the <i>semi-major</i> axis length and
     * <var>b</var> is the <i>semi-minor</i> axis length.
     */
    final double eccentricitySquared;

    /**
     * The square of the second eccentricity: ℯ′² = (a²-b²)/b².
     */
    final double secondEccentricitySquared;

    /**
     * Third flattening of the ellipsoid: n = (a-b)/(a+b).
     * Not to be confused with the third eccentricity squared ℯ″² = (a²-b²)/(a²+b²) which is not used in this class.
     */
    final double thirdFlattening;

    /**
     * Ration between the semi-minor and semi-major axis b/a.
     * This is related to flattening <var>f</var> = 1 - b/a.
     */
    final double axisRatio;

    /**
     * Length of semi-minor axis.
     *
     * @see org.opengis.referencing.datum.Ellipsoid#getSemiMinorAxis()
     */
    private double semiMinorAxis() {
        return semiMajorAxis * axisRatio;
    }

    /**
     * The α value computed from the starting point and starting azimuth.
     * We use the sine and cosine instead of the angles because those components are more frequently used than angles.
     * Those values can be kept constant when computing many end points and end azimuths at different geodesic distances.
     * The {@link #COEFFICIENTS_FOR_START_POINT} flag specifies whether those fields need to be recomputed.
     */
    private double sinα0, cosα0;

    /**
     * Longitude angle from the equatorial point <var>E</var> to starting point P₁ on the ellipsoid.
     * This longitude is computed from the ω₁ longitude on auxiliary sphere, which is itself computed
     * from α₀, α₁, β₁ and ω₁ values computed from the starting point and starting azimuth.
     * The {@link #COEFFICIENTS_FOR_START_POINT} flag specifies whether this field needs to be recomputed.
     *
     * @see #sphericalToGeodeticLongitude(double, double)
     */
    private double λ1E;

    /**
     * Ellipsoidal arc length <var>s₁</var>/<var>b</var> computed from the spherical arc length <var>σ₁</var>.
     * The <var>σ₁</var> value is an arc length on the auxiliary sphere between equatorial point <var>E</var>
     * (the point on equator with forward direction toward azimuth α₀) and starting point P₁. This is computed
     * by I₁(σ₁) in Karney equation 15 and is saved for reuse when the starting point and azimuth do not change.
     * The {@link #COEFFICIENTS_FOR_START_POINT} flag specifies whether this field needs to be recomputed.
     *
     * @see #sphericalToEllipsoidalAngle(double, boolean)
     */
    private double I1_σ1;

    /**
     * The term to be raised to powers (ε⁰, ε¹, ε², ε³, …) in series expansions. Defined in Karney equation 16 as
     * ε = (√[k²+1] - 1) / (√[k²+1] + 1) where k² = {@linkplain #secondEccentricitySquared ℯ′²}⋅cos²α₀ (Karney 9).
     * The {@link #COEFFICIENTS_FOR_START_POINT} flag specifies whether this field needs to be recomputed.
     */
    private double ε;

    /**
     * Coefficients in series expansion of the form A × (σ + ∑Cₓ⋅sin(…⋅σ)).
     * There is 3 series expansions used in this class:
     *
     * <ul>
     *   <li><var>A₁</var> and <var>C₁ₓ</var> (Karney 15) are used for arc length conversions from auxiliary sphere to ellipsoid.</li>
     *   <li><var>A₂</var> and <var>C₂ₓ</var> (Karney 41) are used in calculation of reduced length.</li>
     *   <li><var>A₃</var> and <var>C₃ₓ</var> (Karney 23) are used for calculation of geodetic longitude from spherical angles.</li>
     * </ul>
     *
     * The <var>Cₓₓ</var> coefficients are hard-coded, except the <var>C₃ₓ</var> coefficients
     * (used together with <var>A₃</var>) which depend on {@link #sinα0} and {@link #cosα0}.
     * Note that the <var>C</var> coefficients differ from the ones published by Karney because
     * they have been combined using Clenshaw summation.
     *
     * <p>All those coefficients must be recomputed when the starting point or starting azimuth change.
     * The {@link #COEFFICIENTS_FOR_START_POINT} flag specifies whether those fields need to be recomputed.</p>
     *
     * @see #computeSeriesExpansionCoefficients()
     */
    private double A1, A2, A3, C31, C32, C33, C34, C35;

    /**
     * Coefficients for Rhumb line calculation from Bennett (1996) equation 2, modified with Clenshaw summation.
     */
    private final double R0, R2, R4, R6;

    /**
     * Constructs a new geodetic calculator expecting coordinates in the supplied CRS.
     *
     * @param  crs         the referencing system for the {@link DirectPosition} arguments and return values.
     * @param  ellipsoid   ellipsoid associated to the geodetic component of given CRS.
     */
    GeodesicsOnEllipsoid(final CoordinateReferenceSystem crs, final Ellipsoid ellipsoid) {
        super(crs, ellipsoid);
        final double a = semiMajorAxis;
        final double b = ellipsoid.getSemiMinorAxis();
        final double Δ2 = a*a - b*b;
        eccentricitySquared = Δ2 / (a*a);
        secondEccentricitySquared = Δ2 / (b*b);
        thirdFlattening = (a - b) / (a + b);
        axisRatio = b / a;
        /*
         * For rhumb-line calculation from Bennett (1996) equation 2, modified with Clenshaw summation.
         */
        double fe;
        R0 = 1  -  (eccentricitySquared)*(  1./4   + eccentricitySquared*( 3./64 + eccentricitySquared*( 5./256)));
        R2 = (fe  = eccentricitySquared)*( -3./8   - eccentricitySquared*( 3./32 + eccentricitySquared*(25./768)));
        R4 = (fe *= eccentricitySquared)*( 15./128 + eccentricitySquared*(45./512));
        R6 = (fe *  eccentricitySquared)*(-35./768);
    }

    /**
     * Computes series expansions coefficients.
     *
     * <p><b>Preconditions:</b> The {@link #sinα0} and {@link #cosα0} fields shall be set before to invoke this method.
     * It is caller's responsibility to ensure that sin(α₀)² + cos(α₀)² ≈ 1 (this is verified in assertion).</p>
     *
     * <p><b>Post-conditions:</b> this method sets the {@link #ε}, {@link #A1}, {@link #A2}, {@link #A3},
     * {@link #C31}, {@link #C32}, {@link #C33}, {@link #C34}, {@link #C35} fields.</p>
     *
     * It is caller's responsibility to invoke {@code setValid(COEFFICIENTS_FOR_START_POINT)} after it has updated
     * {@link #λ1E} and {@link #I1_σ1}.
     *
     * @return k² = ℯ′²⋅cos²α₀ term (Karney 9).
     */
    private double computeSeriesExpansionCoefficients() {
        assert abs(sinα0*sinα0 + cosα0*cosα0 - 1) <= 1E-10;                     // Arbitrary threshold.
        final double k2 = secondEccentricitySquared * (cosα0 * cosα0);          // (Karney 9)
        final double h = sqrt(1 + k2);
        ε = (h - 1) / (h + 1);                                                  // (Karney 16)
        final double ε2 = ε*ε;
        A1 = (((( 1./256)*ε2 + 1./64)*ε2 + 1./4)*ε2 + 1) / (1 - ε);             // (Karney 17)
        A2 = ((((25./256)*ε2 + 9./64)*ε2 + 1./4)*ε2 + 1) * (1 - ε);             // (Karney 42)
        /*
         * Compute A₃ from Karney equation 24 with signs redistributed in a different but equivalent way.
         * We use + or - operations in a way where multiplication factors of n are positive. By increasing
         * the number of times where the same value is used (for example a single 1/8 instead of two values
         * +1/8 and -1/8), our hope is that the compiler can reuse more often values that are already loaded
         * in registers. Maybe some compilers could even detect when the same multiplication is done two times.
         *
         * Note: for each line below, we could compute at construction time the parts at the right of ε.
         * However, since they are only a few additions and multiplications, it may not be worth to vastly
         * increase the number of fields in `GeodesicOnEllipsoid` for that purpose, especially if compiler
         * can optimize itself those duplicated multiplications.
         */
        final double n = thirdFlattening;
        A3 = 1 - ε*(1./2    -   n*(1./2)
               + ε*(1./4    +   n*(1./8    -   n*(3./8 ))
               + ε*(1./16   +   n*(3./16   +   n*(1./16))
               + ε*(3./64   +   n*(1./32)
               + ε*(3./128)))));
        /*
         * Karney equation 25 with fε = εⁿ where n = 1, 2, 3, …. The coefficients below differ from
         * the ones published by Karney because they have been combined using Clenshaw summation
         * (see sphericalToEllipsoidalAngle(…) below for a simpler example for Clenshaw summation).
         */
        double fε;
        C31 = (fε  = ε) * ( 1./4     -   n*(1./4)
                   + ε  * ( 1./8     -   n*(            n*(1./8 ))
                   + ε  * ( 1./48    +   n*(3./32   -   n*(1./24))
                   + ε  * ( 1./64    +   n*(1./24)
                   + ε  * (23./1280)))));
        C32 = (fε *= ε) * ( 1./8     -   n*(3./16   -   n*(1./16))
                   + ε  * ( 3./32    -   n*(1./16   +   n*(3./32))
                   + ε  * (-1./128   +   n*(1./8)
                   + ε  * (-1./64))));
        C33 = (fε *= ε) * ( 5./48    -   n*(3./16   -   n*(5./48))
                   + ε  * ( 3./32    -   n*(5./48)
                   + ε  * (-7./160)));
        C34 = (fε *= ε) * ( 7./64    -   n*(7./32)
                   + ε  * ( 7./64));
        C35 = (fε *  ε) * (21./160);
        return k2;
    }

    /**
     * Computes a term in calculation of ellipsoidal arc length <var>s</var> from the given spherical arc length <var>σ</var>.
     * The <var>σ</var> value is an arc length on the auxiliary sphere between equatorial point <var>E</var>
     * (the point on equator with forward direction toward azimuth α₀) and another point.
     *
     * <p>If the {@code minusI2} argument is {@code false}, then this method computes the ellipsoidal arc length <var>s</var>
     * from the given spherical arc length <var>σ</var>. This value is computed by Karney's equation 7:
     *
     * <blockquote>s/b = I₁(σ)</blockquote>
     *
     * If the {@code minusI2} argument is {@code true}, then this method is modified for computing
     * Karney's equation 40 instead (a term in calculation of reduced length <var>m</var>):
     *
     * <blockquote>J(σ) = I₁(σ) - I₂(σ)</blockquote>
     *
     * <p><b>Precondition:</b> {@link #computeSeriesExpansionCoefficients()} must have been invoked before this method.</p>
     *
     * @param  σ        arc length on the auxiliary sphere since equatorial point <var>E</var>.
     * @param  minusI2  whether to subtract I₂(σ) after calculation of I₁(σ).
     * @return I₁(σ) if {@code minusI2} is false, or J(σ) if {@code minusI2} is true.
     */
    private double sphericalToEllipsoidalAngle(final double σ, final boolean minusI2) {
        /*
         * Derived from Karney (2013) equation 18 with θ = 2σ:
         *
         *   ∑Cₓ⋅sin(x⋅θ)  ≈  (-1/2 ⋅ε   +  3/16 ⋅ε³  +  -1/32  ⋅ε⁵)⋅sin(1θ) +                  // C₁₁
         *                    (-1/16⋅ε²  +  1/32 ⋅ε⁴  +  -9/2048⋅ε⁶)⋅sin(2θ) +                  // C₁₂
         *                    (            -1/48 ⋅ε³  +   3/256 ⋅ε⁵)⋅sin(3θ) +                  // C₁₃
         *                    (            -5/512⋅ε⁴  +   3/512 ⋅ε⁶)⋅sin(4θ) +                  // C₁₄
         *                    (                          -7/1280⋅ε⁵)⋅sin(5θ) +                  // C₁₅
         *                    (                          -7/2048⋅ε⁶)⋅sin(6θ)                    // C₁₆
         *
         * For performance and simplicity, we rewrite the equations using trigonometric identities
         * as described in Snyder 3-34 and 3-35, expanded with two more terms. Given:
         *
         *   s  =  A⋅sin(θ) + B⋅sin(2θ) + C⋅sin(3θ) + D⋅sin(4θ) + E⋅sin(5θ) + F⋅sin(6θ)
         *
         * We rewrite as:
         *
         *   s  =  sinθ⋅(A′ + cosθ⋅(B′ + cosθ⋅(C′ + cosθ⋅(D′ + cosθ⋅(E′ + cosθ⋅F′)))))
         *   A′ =  A - C + E
         *   B′ =  2B - 4D + 6F
         *   C′ =  4C - 12E
         *   D′ =  8D - 32F
         *   E′ = 16E
         *   F′ = 32F
         *
         * See https://svn.apache.org/repos/asf/sis/analysis/Map%20projection%20formulas.ods for calculations.
         * Additions are done with smallest values first for better precision.
         *
         * See Karney 2011, Geodesics on an ellipsoid of revolution, https://arxiv.org/pdf/1102.1215.pdf
         * if more coefficients (up to ε⁸) are desired. Those coefficients may be needed in a future SIS
         * version if we want to support celestial bodies with higher flattening factor.
         * Issue tracker: https://issues.apache.org/jira/browse/SIS-465
         */
        final double ε2 = ε*ε;
        final double ε3 = ε*ε2;
        final double ε4 = ε2*ε2;
        final double ε5 = ε2*ε3;
        final double ε6 = ε3*ε3;
        final double θ  = σ * 2;
        final double sinθ = sin(θ);
        final double cosθ = cos(θ);
        double m = A1 * (σ + sinθ * (-31./640 * ε5   +    5./24  * ε3   +   -1./2 * ε
                           + cosθ * (-27./512 * ε6   +   13./128 * ε4   +   -1./8 * ε2
                           + cosθ * (  9./80  * ε5   +   -1./12  * ε3
                           + cosθ * (  5./32  * ε6   +   -5./64  * ε4
                           + cosθ * ( -7./80  * ε5
                           + cosθ * ( -7./64  * ε6)))))));
        if (minusI2) {
            m  -=  A2 * (σ + sinθ * ( 39./640 * ε5   +   -1./24  * ε3   +    1./2 * ε
                           + cosθ * (105./512 * ε6   +  -27./128 * ε4   +    3./8 * ε2
                           + cosθ * ( -41./80 * ε5   +    5./12  * ε3
                           + cosθ * ( -35./32 * ε6   +   35./64  * ε4
                           + cosθ * (  63./80 * ε5
                           + cosθ * (  77./64 * ε6)))))));
        }
        return m;
    }

    /**
     * Computes the spherical arc length <var>σ</var> from the an ellipsoidal arc length <var>s</var>.
     * This method is the converse of {@link #sphericalToEllipsoidalAngle(double, boolean)}.
     * Input is τ = s/(b⋅A₁).
     *
     * <p><b>Precondition:</b> {@link #computeSeriesExpansionCoefficients()} must have been invoked before this method.</p>
     *
     * @param  τ  arc length on the ellipsoid since equatorial point <var>E</var> divided by {@link #A1}.
     * @return arc length <var>σ</var> on the auxiliary sphere.
     *
     * @see #sphericalToEllipsoidalAngle(double, boolean)
     */
    private double ellipsoidalToSphericalAngle(final double τ) {
        assert !isInvalid(COEFFICIENTS_FOR_START_POINT);
        /*
         * Derived from Karney (2013) equation 21 with θ = 2τ:
         *
         *   ∑C′ₓ⋅sin(x⋅θ)  ≈  (1/2 ⋅ε   +   -9/32  ⋅ε³  +    205/1536 ⋅ε⁵)⋅sin(1θ) +           // C′₁₁
         *                     (5/16⋅ε²  +  -37/96  ⋅ε⁴  +   1335/4096 ⋅ε⁶)⋅sin(2θ) +           // C′₁₂
         *                     (             29/96  ⋅ε³  +    -75/128  ⋅ε⁵)⋅sin(3θ) +           // C′₁₃
         *                     (            539/1536⋅ε⁴  +  -2391/2560 ⋅ε⁶)⋅sin(4θ) +           // C′₁₄
         *                     (                             3467/7680 ⋅ε⁵)⋅sin(5θ) +           // C′₁₅
         *                     (                            38081/61440⋅ε⁶)⋅sin(6θ)             // C′₁₆
         *
         * For performance and simplicity, we rewrite the equations using trigonometric identities
         * using the same technic as the one documented above in sphericalToEllipsoidalAngle(σ).
         */
        final double ε2 = ε*ε;
        final double ε3 = ε*ε2;
        final double ε4 = ε2*ε2;
        final double ε5 = ε2*ε3;
        final double ε6 = ε3*ε3;
        final double θ  = τ * 2;
        final double cosθ = cos(θ);
        return τ + sin(θ) * (   281./240  * ε5  +    -7./12  * ε3  +  1./2 * ε
                 + cosθ   * ( 20753./2560 * ε6  +  -835./384 * ε4  +  5./8 * ε2
                 + cosθ   * ( -4967./640  * ε5  +    29./24  * ε3
                 + cosθ   * (-52427./1920 * ε6  +   539./192 * ε4
                 + cosθ   * (  3467./480  * ε5
                 + cosθ   * ( 38081./1920 * ε6))))));
    }

    /**
     * Computes the longitude angle λ from the equatorial point <var>E</var> to a point on the ellipsoid
     * specified by the ω longitude on auxiliary sphere.
     *
     * <p><b>Precondition:</b> {@link #computeSeriesExpansionCoefficients()} must have been invoked before this method.</p>
     *
     * @param  ω  longitude on the auxiliary sphere.
     * @param  σ  spherical arc length from equatorial point E to point on auxiliary sphere
     *            along a geodesic with azimuth α₀ at equator.
     * @return geodetic longitude λ from the equatorial point E to the point.
     */
    private double sphericalToGeodeticLongitude(final double ω, final double σ) {
        /*
         * Derived from Karney (2013) equation 23:
         *
         *   I₃(σ) = A₃⋅(σ + ∑C₃ₓ⋅sin(x⋅θ))
         *
         * but with the sum of sine terms replaced by Clenshaw summation.
         */
        final double θ    = σ * 2;
        final double cosθ = cos(θ);
        final double I3   = A3*(sin(θ)*(C31 + cosθ*(C32 + cosθ*(C33 + cosθ*(C34 + cosθ*C35)))) + σ);
        if (STORE_LOCAL_VARIABLES) store("I₃(σ)", I3);
        return ω - sinα0 * I3 * (1 - axisRatio);
    }

    /**
     * Sets the {@link #sinα0} and {@link #cosα0} terms. Note that the {@link #msinα2} field is always set
     * to the {@code sinα0} value in this class. But those two fields may nevertheless have different values
     * if {@code msinα2} has been set independently, for example by {@link #createGeodesicPath2D(double)}.
     */
    private void α0(final double sinα1, final double cosα1, final double sinβ1, final double cosβ1) {
        sinα0 = sinα1 * cosβ1;                  // Azimuth at equator (Karney 5) as geographic angle.
        cosα0 = hypot(cosα1, sinα1*sinβ1);      // = sqrt(1 - sinα0*sinα0) with less rounding errors.
    }

    /**
     * Computes the end point from the start point, the azimuth and the geodesic distance.
     * This method should be invoked if the end point or ending azimuth is requested while
     * {@link #END_POINT} validity flag is not set.
     *
     * <p>This implementation computes {@link #φ2}, {@link #λ2} and azimuths using the method
     * described in Karney (2013) <cite>Algorithms for geodesics</cite>. The coefficients of
     * Fourier and Taylor series expansions are given by equations 15 and 17.</p>
     *
     * @throws IllegalStateException if the start point, azimuth or distance has not been set.
     */
    @Override
    final void computeEndPoint() {
        canComputeEndPoint();                                       // May throw IllegalStateException.
        if (isInvalid(COEFFICIENTS_FOR_START_POINT)) {
            final double m     = hypot(msinα1, mcosα1);
            final double sinα1 = msinα1 / m;                        // α is a geographic (not arithmetic) angle.
            final double cosα1 = mcosα1 / m;
            final double tanβ1 = axisRatio * tan(φ1);               // β₁ is reduced latitude (Karney 6).
            final double cosβ1 = 1 / sqrt(1 + tanβ1*tanβ1);
            final double sinβ1 = tanβ1 * cosβ1;
            α0(sinα1, cosα1, sinβ1, cosβ1);
            /*
             * Note:  Karney said that for equatorial geodesics (φ₁=0 and α₁=π/2), calculation of σ₁ is indeterminate
             * and σ₁=0 should be taken. The indetermination appears as atan2(0,0). However, this expression evaluates
             * to 0 in Java, as required by Math.atan2(y,x) specification, which is what we want. So there is no need
             * for a special case.
             */
            final double cosα1_cosβ1 = cosα1*cosβ1;
            final double σ1 = atan2(sinβ1,       cosα1_cosβ1);      // Arc length on great circle (Karney 11).
            final double ω1 = atan2(sinβ1*sinα0, cosα1_cosβ1);
            final double k2 = computeSeriesExpansionCoefficients();
            λ1E   = sphericalToGeodeticLongitude(ω1, σ1);
            I1_σ1 = sphericalToEllipsoidalAngle(σ1, false);
            setValid(COEFFICIENTS_FOR_START_POINT);
            if (STORE_LOCAL_VARIABLES) {                            // For comparing values with Karney table 2.
                store("β₁", atan2(sinβ1, cosβ1));
                store("σ₁", σ1);
                store("ω₁", ω1);
                store("k²", k2);
                snapshot();
            }
        }
        /*
         * Distance from equatorial point E to ending point P₂ along the geodesic: s₂ = s₁ + ∆s.
         */
        final double s2b = I1_σ1 + geodesicDistance / semiMinorAxis();      // (Karney 18) + ∆s/b
        final double σ2  = ellipsoidalToSphericalAngle(s2b / A1);           // (Karney 21)
        final double sinσ2 = sin(σ2);
        final double cosσ2 = cos(σ2);
        /*
         * Azimuth at ending point α₂ = atan2(sinα₀, cosα₀⋅cosσ₂)    (Karney 14 adapted for ending point).
         * We do not need atan2(y,x) because we keep x and y components separated. It is not necessary to
         * normalize to a vector of length 1.
         */
        msinα2 = sinα0;
        mcosα2 = cosα0 * cosσ2;
        /*
         * Ending point coordinates on auxiliary sphere: Latitude β is given by Karney equation 13:
         *
         *   β₂ = atan2(cos(α₀)⋅sin(σ₂), hypot(cos(α₀)⋅cos(σ₂), sin(α₀))
         *
         * We replace cos(α₀)⋅cos(σ₂) by mcosα2 since we computed it above. Then we avoid the call to
         * atan2(y,x) by storing directly the y and x values. Note that `sinβ2` and `cosβ2` are not
         * really sine and cosine since we do not normalize them to sin² + cos² = 1. We do not need
         * to normalize because we use either a ratio of those 2 quantities or give them to atan2(…).
         */
        final double sinβ2 = cosα0 * sinσ2;
        final double cosβ2 = hypot(msinα2, mcosα2);                             // m⋅sin(α₂) = sin(α₀) in this class.
        final double ω2    = atan2(sinα0*sinσ2, cosσ2);                         // (Karney 12).
        /*
         * Convert reduced longitude ω and latitude β on auxiliary sphere
         * to geodetic longitude λ and latitude φ.
         */
        final double λ2E = sphericalToGeodeticLongitude(ω2, σ2);
        λ2 = IEEEremainder(λ2E - λ1E + λ1, 2*PI);
        φ2 = atan(sinβ2 / (cosβ2 * axisRatio));                                 // (Karney 6).
        setValid(END_POINT | ENDING_AZIMUTH);
        if (STORE_LOCAL_VARIABLES) {                // For comparing values with Karney table 2.
            store("s₂", s2b * semiMinorAxis());
            store("τ₂", s2b / A1);
            store("σ₂", σ2);
            store("α₂", atan2(msinα2, mcosα2));
            store("β₂", atan2(sinβ2, cosβ2));
            store("ω₂", ω2);
            store("λ₂", λ2E);
            store("Δλ", λ2E - λ1E);
        }
    }

    /**
     * Computes the geodesic distance and azimuths from the start point and end point.
     * This method should be invoked if the distance or an azimuth is requested while
     * {@link #STARTING_AZIMUTH}, {@link #ENDING_AZIMUTH} or {@link #GEODESIC_DISTANCE}
     * validity flag is not set.
     *
     * <p>Reminder: given <var>P₁</var> the starting point and <var>E</var> the intersection of the geodesic with equator:</p>
     * <ul>
     *   <li><var>α₁</var> is the azimuth (0° oriented North) of the geodesic from <var>E</var> to <var>P₁</var>.</li>
     *   <li><var>σ₁</var> is the spherical arc length (in radians) between <var>E</var> and <var>P₁</var>.</li>
     *   <li><var>ω₁</var> is the spherical longitude (in radians) on the auxiliary sphere at <var>P₁</var>.
     *       Spherical longitude is the angle formed by the meridian of <var>E</var> and the meridian of <var>P₁</var>.</li>
     * </ul>
     *
     * @throws IllegalStateException if the distance or azimuth has not been set.
     * @throws GeodeticException if an azimuth or the distance cannot be computed.
     */
    @Override
    final void computeDistance() {
        canComputeDistance();
        /*
         * The algorithm in this method requires the following canonical configuration:
         *
         *   Negative latitude of starting point:         φ₁ ≤ 0
         *   Ending point latitude smaller in magnitude:  φ₁ ≤ φ₂ ≤ -φ₁
         *   Positive longitude difference:               0 ≤ ∆λ ≤ π
         *   (Consequence of above):                      0 ≤ α₀ ≤ π/2
         *
         * If the given points do not met above conditions, then we need to swap start and end points or to
         * swap coordinate signs. We apply those changes on local variables only, not on the class fields.
         * We will need to apply the converse of those changes in the final results.
         */
        double φ1 = this.φ1;
        double φ2 = this.φ2;
        double Δλ = IEEEremainder(λ2 - λ1, 2*PI);           // In [-π … +π] range.
        final boolean swapPoints = abs(φ2) > abs(φ1);
        if (swapPoints) {
            φ1 = this.φ2;
            φ2 = this.φ1;
            Δλ = -Δλ;
        }
        final boolean inverseLatitudeSigns = φ1 > 0;
        if (inverseLatitudeSigns) {
            φ1 = -φ1;
            φ2 = -φ2;
        }
        final boolean inverseLongitudeSigns = Δλ < 0;
        if (inverseLongitudeSigns) {
            Δλ = -Δλ;
        }
        /*
         * Compute an approximation of the azimuth α₁ at starting point. This estimation will be refined by iteration
         * in the loop later, but that iteration will not converge if the first α₁ estimation is not good enough. The
         * general formula does not give good α₁ initial value for antipodal points, so we need to check for special
         * cases first:
         *
         *   1) Nearly antipodal points with φ₁ = -φ₂.
         *   2) Nearly antipodal points with φ₁ slightly different than -φ₂.
         *   3) Equatorial case: φ₁ = φ₂ = 0 but restricted to ∆λ ≤ (1-f)⋅π.
         *   4) Meridional case: ∆λ = 0 or ∆λ = π (handled by general case in this method).
         */
        if (φ1 > -LATITUDE_THRESHOLD) {                         // Sufficient test because φ₁ ≤ 0 and |φ₂| ≤ φ₁
            /*
             * Points on equator but not nearly anti-podal. The geodesic is an arc on equator and the azimuths
             * are α₁ = α₂ = ±90°. We need this special case because when φ = 0, the general case get sinβ = 0
             * then σ = atan2(sinβ, cosα⋅cosβ) = 0, which result in a distance of 0. I have not yet understood
             * how to use the general formulas in such case. This code is a workaround in the meantime.
             *
             * See https://issues.apache.org/jira/browse/SIS-467
             */
            if (Δλ > axisRatio * PI) {
                // Karney's special case documented before equation 45.
                throw new GeodeticException("Cannot compute geodesics for antipodal points on equator.");
            }
            super.computeDistance();
            return;
        }
        /*
         * Reduced latitudes β (Karney 6). Actually we don't need the β angles
         * (except for a special case), but rather their sine and cosine values.
         */
        final double tanβ1 = axisRatio * tan(φ1);
        final double tanβ2 = axisRatio * tan(φ2);
        final double cosβ1 = 1 / sqrt(1 + tanβ1*tanβ1);
        final double cosβ2 = 1 / sqrt(1 + tanβ2*tanβ2);
        final double sinβ1 = tanβ1 * cosβ1;
        final double sinβ2 = tanβ2 * cosβ2;
        double α1;
        if (Δλ >= PI - NEARLY_ANTIPODAL_Δλ && abs(φ1 + φ2) <= NEARLY_ANTIPODAL_Δλ) {
            /*
             * Nearly antipodal points. Karney's equations 53 are reproduced on the left side
             * (with f = 1 - b/a), which we replace by the equations on the right side below:
             *
             *   Δ  = f⋅a⋅π⋅cos²β₁              ┃      Δ₁ = f⋅π⋅cosβ₁
             *   ∆λ = π + Δ/(a⋅cosβ₁)⋅x         ┃      ∆λ = π + Δ₁⋅x
             *   β₂ = -β₁ + (Δ/a)⋅y             ┃      β₂ = -β₁ + (Δ₁⋅cosβ₁)⋅y
             */
            final double β1 = atan2(sinβ1, cosβ1);
            final double β2 = atan2(sinβ2, cosβ2);
            final double Δ1  = (1 - axisRatio) * PI * cosβ1;                // Differ from Karney by a⋅cosβ₁ factor.
            final double y  = (β2 + β1) / (Δ1*cosβ1);
            final double x  = (PI - Δλ) / Δ1;                               // Opposite sign of Karney. We have x ≥ 0.
            final double x2 = x*x;
            final double y2 = y*y;
            if (STORE_LOCAL_VARIABLES) {                        // For comparing values with Karney table 4.
                store("x", -x);                                 // Sign used by Karney.
                store("y",  y);
            }
            if (y2 < 1E-12) {                                   // Empirical threshold. See μ(…) for more information.
                α1 = (x2 > 1) ? PI/2 : atan(x / sqrt(1 - x2));  // (Karney 57) with opposite sign of x. Result in α₁ ≥ 0.
            } else {
                final double μ = μ(x2, y2);
                α1 = atan2(x*μ, (y*(1+μ)));                     // (Karney 56) with opposite sign of x.
                if (STORE_LOCAL_VARIABLES) {
                    store("μ", μ);
                }
            }
        } else {
            /*
             * Usual case (non-antipodal). Estimation is based on variation of geodetic longitude λ
             * and spherical longitude ω on the auxiliary sphere. Karney makes a special case for
             * Δλ = 0 and Δλ = π by defining α₁ = Δλ. However, formulas below produce the same result.
             */
            double w = (cosβ1 + cosβ2) / 2;
            w = sqrt(1 - eccentricitySquared * (w*w));
            final double Δω = Δλ / w;
            final double cω = cos(Δω);
            final double sω = sin(Δω);
            final double αx = cosβ1*sinβ2 - sinβ1*cosβ2*cω;
            final double αy = cosβ2*sω;
            α1 = atan2(αy, αx);                                                                 // (Karney 49)
            if (STORE_LOCAL_VARIABLES) {        // For comparing values with Karney table 3.
                store("ωb", w);
                store("Δω", Δω);
                store("α₂", atan2(cosβ1*sω,      cosβ1*cω*sinβ2 - sinβ1*cosβ2));                // (Karney 50)
                store("Δσ", atan2(hypot(αy, αx), cosβ1*cω*cosβ2 + sinβ1*sinβ2));                // (Karney 51)
                // For small distances we could stop here. But it is easier to let iteration does its work.
            }
        }
        /*
         * Stores locale variables for comparison against Karney tables 4, 5 and 6. Values β₁ and β₂ are kept
         * constant during all this method. Value α₁ is a first estimation and will be updated during iteration.
         * Note that because Karney separate calculation of α₁ and remaining calculation in two separated tables,
         * we need to truncate α₁ to the same number of digits than Karney in order to get the same numbers in
         * the rest of this method.
         */
        if (STORE_LOCAL_VARIABLES) {
            store("β₁", atan(tanβ1));
            store("β₂", atan(tanβ2));
            store("α₁", α1);
            α1 = computedToGiven(α1);
        }
        /*
         * Refine α₁ using Newton's method. Karney proposes an hybrid approach: approximate α₁,
         * then compute σ₁, σ₂, ω₁ and ω₂ (actually the sine and cosine of those angles in our
         * implementation).
         */
        double σ1, σ2;
        int moreRefinements = Formulas.MAXIMUM_ITERATIONS;
        do {
            α0(msinα1 = sin(α1), mcosα1 = cos(α1), sinβ1, cosβ1);
            final double k2 = computeSeriesExpansionCoefficients();
            /*
             * Compute α₂ from Karney equation 5: sin(α₀) = sin(α₁)⋅cos(β₁) = sin(α₂)⋅cos(β₂)
             * The cos(α₂) term could be computed from sin(α₂), but Karnay recommends instead
             * equation 45. An older publication (Karney 2010) went one step further with the
             * replacement of (cos²β₂ - cos²β₁) by:
             *
             *     (cosβ₂ - cosβ₁)⋅(cosβ₂ + cosβ₁)  if β₁ < -π/4        (Reminder: β₁ is always negative)
             *     (sinβ₁ - sinβ₂)⋅(sinβ₁ + sinβ₂)  otherwise.
             *
             * Actually we don't need α values directly, but instead cos(α)⋅cos(β).
             * Note that cos(α₁) can be negative because α₁ ∈ [0…2π].
             */
            final double cosα1_cosβ1 = mcosα1 * cosβ1;
            final double cosα2_cosβ2 = sqrt(cosα1_cosβ1*cosα1_cosβ1 +
                    (cosβ1 <= 1/MathFunctions.SQRT_2 ? (cosβ2 - cosβ1)*(cosβ2 + cosβ1)
                                                     : (sinβ1 - sinβ2)*(sinβ1 + sinβ2)));
            msinα2 = sinα0;
            mcosα2 = cosα2_cosβ2;
            /*
             * Karney gives the following formulas:
             *
             *   σ  =  atan2(sinβ, cosα⋅cosβ)        —     spherical arc length.
             *   ω  =  atan2(sin(α₀)⋅sinσ, cosσ)     —     spherical longitude.
             *
             * We perform the following substitutions where c is an unknown constant:
             * That unknown coefficient will disaspear in atan2(c⋅y, c⋅x) expressions:
             *
             *   sin(σ) = c⋅sinβ
             *   cos(σ) = c⋅cosα⋅cosβ
             */
            final double ω1, ω2;
            σ1 = atan2(sinβ1,       cosα1_cosβ1);               // (Karney 11)
            σ2 = atan2(sinβ2,       cosα2_cosβ2);
            ω1 = atan2(sinβ1*sinα0, cosα1_cosβ1);               // (Karney 12) with above-cited substitutions.
            ω2 = atan2(sinβ2*sinα0, cosα2_cosβ2);
            /*
             * Compute the difference in longitude using the current start point and end point.
             * If this difference is close enough to the requested accuracy, we are almost done.
             * Otherwise refine the results. Note that if we detect that accuracy is good enough,
             * we still complete the computation in order to not waste what we have computed so
             * far in current iteration.
             */
            final double λ2E;
            λ1E = sphericalToGeodeticLongitude(ω1, σ1);
            λ2E = sphericalToGeodeticLongitude(ω2, σ2);
            final double Δλ_error = IEEEremainder(λ2E - λ1E - Δλ, 2*PI);
            if (abs(Δλ_error) <= ITERATION_TOLERANCE) {
                moreRefinements = 0;
            } else if (--moreRefinements == 0) {
                throw new GeodeticException(Resources.format(Resources.Keys.NoConvergence));
            }
            /*
             * Special case for α₁ = π/2 and β₂ = ±β₁ (Karney's equation 47). We replace the β₂ = ±β₁
             * condition by |β₂| - |β₁| ≈ 0. Assuming tan(θ) ≈ θ for small angles we take the tangent
             * of above difference and use tan(β₂ - β₁) = (tanβ₂ - tanβ₁)/(1 + tanβ₂⋅tanβ₁) identity.
             * Note that tanβ₁ ≤ 0 and |tanβ₂| ≤ |tanβ₁| in this method.
             */
            final double dΔλ_dα1;
            if (abs(mcosα1) < LATITUDE_THRESHOLD && (-tanβ1 - abs(tanβ2)) < (1 + abs(tanβ1*tanβ2)) * LATITUDE_THRESHOLD) {
                dΔλ_dα1 = -2 * sqrt(1 - eccentricitySquared * (cosβ1*cosβ1)) / sinβ1;
            } else {
                /*
                 * Karney's equation 38 combined with equation 46. The substitutions
                 * for sin(σ) and cos(σ) described above are applied again here.
                 */
                final double h1 = hypot(sinβ1, cosα1_cosβ1);
                final double h2 = hypot(sinβ2, cosα2_cosβ2);
                final double sinσ1 = sinβ1 / h1;
                final double sinσ2 = sinβ2 / h2;
                final double cosσ1 = cosα1_cosβ1 / h1;
                final double cosσ2 = cosα2_cosβ2 / h2;
                final double J2 = sphericalToEllipsoidalAngle(σ2, true);
                final double J1 = sphericalToEllipsoidalAngle(σ1, true);
                final double Δm = (sqrt(1 + k2*(sinσ2*sinσ2))*cosσ1*sinσ2
                                - sqrt(1 + k2*(sinσ1*sinσ1))*sinσ1*cosσ2
                                - cosσ1*cosσ2*(J2 - J1));
                dΔλ_dα1 = Δm * axisRatio / cosα2_cosβ2;
                if (STORE_LOCAL_VARIABLES) {
                    store("J(σ₁)", J1);
                    store("J(σ₂)", J2);
                    store("Δm",    Δm * semiMinorAxis());
                }
            }
            final double dα1 = Δλ_error / dΔλ_dα1;                  // Opposite sign of Karney δα₁ term.
            /*
             * We need to compute α₁ -= dα₁ then iterate again. But sometimes the subtraction has no effect
             * because dα₁ ≪ α₁ and iteration continues with unchanged α₁ value until no convergence error.
             * If we detect this situation, assume that we have the best accuracy that we can get.
             *
             * Note: we tried Kahan summation algorithm but it didn't solved the problem.
             * No convergence were still happening, but in more indirect ways (after a cycle in iterations).
             */
            if (α1 == (α1 -= dα1)) {
                moreRefinements = 0;
                if (STORE_LOCAL_VARIABLES) {
                    store("dα₁ ≪ α₁", dα1);                         // Flag for `iterationReachedPrecisionLimit` in tests.
                }
            }
            if (STORE_LOCAL_VARIABLES) {                            // For comparing values against Karney table 5 and 6.
                final double I1_σ2;
                I1_σ1 = sphericalToEllipsoidalAngle(σ1, false);     // Required for computation of s₁ in `snapshot()`.
                I1_σ2 = sphericalToEllipsoidalAngle(σ2, false);
                snapshot();
                store("k²",    k2);
                store("σ₁",    σ1);
                store("σ₂",    σ2);
                store("ω₁",    ω1);
                store("ω₂",    ω2);
                store("α₂",    atan2(msinα2, mcosα2));
                store("λ₂",    λ2E);
                store("Δλ",    λ2E - λ1E);
                store("δλ",    Δλ_error);
                store("dλ/dα", dΔλ_dα1);
                store("δσ₁",  -dα1);
                store("α₁",    α1);
                store("s₂",    I1_σ2 * semiMinorAxis());
                store("Δs",    (I1_σ2 - I1_σ1) * semiMinorAxis());
            }
        } while (moreRefinements != 0);
        final double I1_σ2;
        I1_σ1 = sphericalToEllipsoidalAngle(σ1, false);
        I1_σ2 = sphericalToEllipsoidalAngle(σ2, false);
        geodesicDistance = (I1_σ2 - I1_σ1) * semiMinorAxis();
        /*
         * Restore the coordinate sign and order to the original configuration.
         */
        if (swapPoints) {
            double t;
            t = msinα1; msinα1 = msinα2; msinα2 = t;
            t = mcosα1; mcosα1 = mcosα2; mcosα2 = t;
        }
        if (inverseLongitudeSigns ^ swapPoints) {
            msinα1 = -msinα1;
            msinα2 = -msinα2;
        }
        if (inverseLatitudeSigns ^ swapPoints) {
            mcosα1 = -mcosα1;
            mcosα2 = -mcosα2;
        }
        setValid(STARTING_AZIMUTH | ENDING_AZIMUTH | GEODESIC_DISTANCE);
        if (!(swapPoints | inverseLongitudeSigns | inverseLatitudeSigns)) {
            setValid(COEFFICIENTS_FOR_START_POINT);
        }
    }

    /**
     * Computes the positive root of quartic equation for estimation of α₁ in nearly antipodal case.
     * Formula is given in appendix B of <a href="https://arxiv.org/pdf/1102.1215.pdf">C.F.F Karney (2011)</a>
     * given <var>x</var> and <var>y</var> the coordinates on a plane coordinate system centered on the antipodal point:
     *
     * <blockquote>
     * μ⁴ + 2μ³ + (1−x²-y²)μ² − 2y²μ - y² = 0
     * </blockquote>
     *
     * The results should have only one positive root {@literal (μ > 0)}.
     *
     * <h4>Condition on <var>y</var> value</h4>
     * This method is indeterminate when <var>y</var> → 0 (it returns {@link Double#NaN}). For values too close to zero,
     * the result may be non-significative because of rounding errors. For choosing a threshold value for <var>y</var>,
     * {@code GeodesicsOnEllipsoidTest.Calculator} compares the value computed by this method against the value computed
     * by {@link org.apache.sis.math.MathFunctions#polynomialRoots(double...)}. If the values differ too much, we presume
     * that they are mostly noise caused by a <var>y</var> value too low.
     *
     * @param  x2  the square of <var>x</var>.
     * @param  y2  the square of <var>y</var>.
     */
    private static double μ(final double x2, final double y2) {
        final double r  = (x2 + y2 - 1)/6;
        final double r3 = r*r*r;
        final double S  = x2*y2/4;
        final double d  = S*(S + 2*r3);
        final double u;
        if (d < 0) {
            u = r*(1 + 2*cos(atan2(sqrt(-d), -(S + r3))/3));
        } else {
            final double T = cbrt(S + r3 + copySign(sqrt(d), S + r3));
            u = (T == 0) ? 0 : r + T + r*r/T;
        }
        final double v = sqrt(u*u + y2);
        final double w = (v + u - y2) / (2*v);
        return (v + u) / (sqrt(v + u + w*w) + w);
    }

    /**
     * Computes (∂y/∂φ)⁻¹ at the given latitude on an ellipsoid with semi-major axis length of 1.
     * This derivative is close to cos(φ) for a slightly flattened sphere.
     *
     * @see org.apache.sis.referencing.operation.projection.ConformalProjection#dy_dφ
     */
    @Override
    double dφ_dy(final double φ) {
        final double sinφ = sin(φ);
        final double cosφ = cos(φ);
        return cosφ/(1  -  eccentricitySquared * (cosφ*cosφ) / (1 - eccentricitySquared * (sinφ*sinφ)));
    }

    /**
     * Takes a snapshot of the current fields in this class. This is used for JUnit tests only. During development phases,
     * {@link #STORE_LOCAL_VARIABLES} should be {@code true} for allowing {@code GeodesicsOnEllipsoidTest} to verify the
     * values of a large range of local variables. But when the storage of locale variables is disabled, this method allows
     * {@code GeodesicsOnEllipsoidTest} to still verify a few variables.
     */
    final void snapshot() {
        store("ε",      ε);
        store("A₁",     A1);
        store("A₂",     A2);
        store("A₃",     A3);
        store("α₀",     atan2(sinα0, cosα0));
        store("I₁(σ₁)", I1_σ1);
        store("s₁",     I1_σ1 * semiMinorAxis());
        store("λ₁",     λ1E);
    }

    /**
     * Stores the value of a local variable or a field. This is used in JUnit tests only.
     *
     * @param  name   name of the local variable.
     * @param  value  value of the local variable.
     */
    void store(String name, double value) {
    }

    /**
     * Replaces a computed value by the value given in Karney table. This is used when the result published in Karney
     * table 4 is used as input for Karney table 5. We need to truncate the value to the same numbers of digits than
     * Karney, otherwise computation results will differ.
     */
    double computedToGiven(double α1) {
        return α1;
    }

    /**
     * Computes rhumb line using series expansion.
     *
     * <p><b>Source:</b> G.G. Bennett, 1996. <a href="https://doi.org/10.1017/S0373463300013151">
     * Practical Rhumb Line Calculations on the Spheroid</a>. J. Navigation 49(1), 112-119.</p>
     */
    @Override
    final void computeRhumbLine() {
        canComputeDistance();
        /*
         * Bennett (1996) equation 1 computes isometric latitudes Ψ for given geodetic latitudes φ:
         *
         *     Ψ(φ) = log(tan(PI/4 + φ/2) * pow((1 - ℯsinφ) / (1 + ℯsinφ), ℯ/2));
         *
         * (ℯ is the eccentricity, not squared). However, we need only the isometric latitudes difference:
         *
         *     ΔΨ = Ψ(φ₂) - Ψ(φ₁)
         *
         * We rewrite the equation using log(Ψ₁) - log(Ψ₂) = log(Ψ₁/Ψ₂) and other identities:
         *
         *     ΔΨ = log(tan(PI/4 + φ₂/2)) + log(pow((1 - ℯsinφ₂) / (1 + ℯsinφ₂), ℯ/2))
         *        - log(tan(PI/4 + φ₁/2)) - log(pow((1 - ℯsinφ₁) / (1 + ℯsinφ₁), ℯ/2))
         *
         *        = log(tan(PI/4 + φ₂/2) /
         *              tan(PI/4 + φ₁/2) *
         *              pow(((1 - ℯsinφ₂)*(1 + ℯsinφ₁)) /
         *                  ((1 + ℯsinφ₂)*(1 - ℯsinφ₁)), ℯ/2))
         *
         * The code below combines ℯsinφ terms otherwise. Note that we could also use product-to-sum
         * identities for rewriting the  tan(¼π + ½φ₂) / tan(¼π + ½φ₁)  expression as  (a + b) / (a - b)
         * where  a = cos((φ₂+φ₁)/2)  and  b = sin((φ₂-φ₁)/2), but the number of trigonometric method calls
         * would be about the same and result may be less accurate.
         */
        final double eccentricity = sqrt(eccentricitySquared);      // TODO: avoid computing on each invocation.
        final double sinφ1 = sin(φ1);
        final double sinφ2 = sin(φ2);
        final double sd = eccentricity * (sinφ1 - sinφ2);
        final double sm = 1 - eccentricitySquared * (sinφ1 * sinφ2);
        final double ΔΨ = log(tan(PI/4 + φ2/2) / tan(PI/4 + φ1/2) * pow((sm+sd)/(sm-sd), eccentricity/2));
        final double Δλ = IEEEremainder(λ2 - λ1, 2*PI);
        final double h  = hypot(Δλ, ΔΨ);
        final double S;
        if (abs(φ1 - φ2) < LATITUDE_THRESHOLD) {
            final double φm = (φ1 + φ2)/2;
            final double sinφ = sin(φm);
            S = cos(φm) / sqrt(1 - eccentricitySquared*(sinφ*sinφ));        // Bennett equation 4 with sin(α) = Δλ/h.
        } else {
            final double m1 = m(φ1, sinφ1);
            final double m2 = m(φ2, sinφ2);
            S = (m2 - m1) / ΔΨ;                                             // Bennett (1996) with cos(α) = ΔΨ/h.
            if (STORE_LOCAL_VARIABLES) {
                store("m₁", m1);
                store("m₂", m2);
                store("Δm", m2 - m1);
            }
        }
        rhumblineLength = S * h * semiMajorAxis;
        rhumblineAzimuth = atan2(Δλ, ΔΨ);
        if (STORE_LOCAL_VARIABLES) {
            store("Δλ", Δλ);
            store("ΔΨ", ΔΨ);
        }
    }

    /**
     * Computes Bennett (1996) equation 2 modified with Clenshaw summation.
     */
    private double m(final double φ, final double sinφ) {
        final double cosφ = cos(φ);
        final double sinθ = 2*sinφ*cosφ;                        // sin(2φ)
        final double cosθ = (cosφ + sinφ) * (cosφ - sinφ);      // cos(2φ)  =  cos²φ - sin²φ
        return R0*φ + sinθ*(R2 + cosθ*(R4 + cosθ*R6));
    }

    /**
     * The operation method to use for creating a map projection. For the ellipsoidal case we use EPSG::9832.
     * According EPSG documentation the precision is acceptable withing 800 km of projection natural origin.
     */
    @Override
    final String getProjectionMethod() {
        return "Modified Azimuthal Equidistant";
    }
}
