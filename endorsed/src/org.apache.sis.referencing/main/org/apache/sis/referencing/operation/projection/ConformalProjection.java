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
import org.apache.sis.referencing.internal.shared.Formulas;


/**
 * Base class of {@link LambertConicConformal}, {@link Mercator} and {@link PolarStereographic} projections.
 * Those projections have in common the property of being <dfn>conformal</dfn>, i.e. they preserve angles locally.
 * However, we do not put this base class in public API because not all conformal projections extend this base class.
 * For example, the {@link TransverseMercator} projection, despite being conformal, uses very different formulas.
 *
 * <p>Note that no projection can be both conformal and equal-area. So the formulas in this class are usually
 * mutually exclusive with formulas in {@link AuthalicConversion} class (used for equal-area projections).</p>
 *
 * <p>This base class can be seen as a generalization of <cite>Lambert Conic Conformal</cite> projection,
 * which includes some other projections like Mercator and Polar Stereographic as special cases.
 * For this base class, the Mercator projection is considered as <q>a special limiting case of the
 * Lambert Conic Conformal map projection with the equator as the single standard parallel.</q>
 * (Source: §1.3.3 in IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015).
 * Indeed, those two projections have some equation in commons which are provided in this base class.</p>
 *
 * <p>The Polar Stereographic projection is not documented as a special case of Lambert Conic Conformal,
 * but the equations in the {@code PolarStereographic.transform(…)} and {@code inverseTransform(…)} methods
 * appear to be the same with the <var>n</var> factor fixed to 1 or -1, so we leverage the code provided by
 * this base class. This class hierarchy is only an implementation convenience and not part of public API.</p>
 *
 * <p>The Transverse Mercator projection is also conformal, but does not use the formulas provided in this class.
 * It will instead compute the coefficients itself and use its own, more complex, formulas with those coefficients.
 * However, the formulas provided in this {@code ConformalProjection} class can be seen as a special case of
 * Transverse Mercator formulas for <var>x</var> = 0.</p>
 *
 * <h2>Reference</h2>
 * “Lambert developed the regular Conformal Conic as the oblique aspect of a family containing the previously
 * known polar Stereographic and regular Mercator projections. (…) If the standard parallels are symmetrical
 * about the Equator, the regular Mercator results (although formulas must be revised). If the only standard
 * parallel is a pole, the polar Stereographic results.” (Snyder, page 105).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class ConformalProjection extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -292755890184138414L;

    /**
     * The threshold value of {@link #eccentricity} at which we consider the accuracy of the
     * series expansion insufficient. This threshold is determined empirically with the help
     * of the {@code MercatorMethodComparison} class in the test directory.
     * We choose the value where:
     *
     * <ul>
     *   <li>the average error of series expansion become greater than {@link NormalizedProjection#ITERATION_TOLERANCE},</li>
     *   <li>the maximal error of series expansion become greater than {@link NormalizedProjection#ANGULAR_TOLERANCE}.</li>
     * </ul>
     */
    static final double ECCENTRICITY_THRESHOLD = 0.16;

    /**
     * {@code true} if the {@link #eccentricity} value is greater than or equals to {@link #ECCENTRICITY_THRESHOLD},
     * in which case the {@link #φ(double)} method will need to use an iterative method.
     */
    private final boolean useIterations;

    /**
     * Coefficients of the first terms in the series expansion of the reverse projection.
     * Values of those coefficients depend only on {@linkplain #eccentricity eccentricity} value.
     * The series expansion is published under the following form, where χ is the <i>conformal latitude</i>:
     *
     *     <blockquote>c₂⋅sin(2χ) + c₄⋅sin(4χ) + c₆⋅sin(6χ) + c₈⋅sin(8χ)</blockquote>
     *
     * However, we rewrite above series expansion for taking advantage of trigonometric identities.
     * The equation become (with different <var>c</var> values):
     *
     *     <blockquote>sin(2χ)⋅(c₂ + cos(2χ)⋅(c₄ + cos(2χ)⋅(c₆ + cos(2χ)⋅c₈)))</blockquote>
     *
     * <h4>Serialization note</h4>
     * we do not strictly need to serialize those fields because they could be computed after deserialization.
     * But we serialize them anyway in order to simplify a little bit this class (it allows us to keep those
     * fields final) and because different version of SIS computes those coefficients in slightly different ways.
     */
    private final double c2χ, c4χ, c6χ, c8χ;

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer.
     * This constructor computes the coefficients in the series expansion from the {@link #eccentricitySquared} value.
     *
     * @param  initializer  the initializer for computing map projection internal parameters.
     */
    ConformalProjection(final Initializer initializer) {
        super(initializer, null);
        useIterations = (eccentricity >= ECCENTRICITY_THRESHOLD);
        final double e2 = eccentricitySquared;
        final double e4 = e2 * e2;
        final double e6 = e2 * e4;
        final double e8 = e4 * e4;
        /*
         * Published equation is of the following form:
         *
         *     φ  =  χ + A⋅sin(2χ) + B⋅sin(4χ) + C⋅sin(6χ) + D⋅sin(8χ)                      (Snyder 3-34)
         *
         * We rewrite as:
         *
         *     φ  =  χ + sin(2χ)⋅(A′ + cos(2χ)⋅(B′ + cos(2χ)⋅(C′ + cos(2χ)⋅D′)))            (Snyder 3-35)
         *     A′ =  A - C
         *     B′ =  2B - 4D
         *     C′ =  4C
         *     D′ =  8D
         *
         * Published coefficients are:
         *
         *     A  =    13/360   ⋅ℯ⁸  +   1/12 ⋅ℯ⁶  +  5/24⋅ℯ⁴  +  ℯ²/2
         *     B  =   811/11520 ⋅ℯ⁸  +  29/240⋅ℯ⁶  +  7/48⋅ℯ⁴
         *     C  =    81/1120  ⋅ℯ⁸  +   7/120⋅ℯ⁶
         *     D  =  4279/161280⋅ℯ⁸
         *
         * We replace A B C D by A′ B′ C′ D′ using Snyder 3-35. The result are coefficients below.
         * For each line, we add the smallest values first in order to reduce rounding errors.
         * The smallest values are the ones using the eccentricity raised to the highest power.
         */
        c2χ = fma(e2, 1./2, fma(e4, 5./24, fma(e6,  1./40,  e8*( -73./2016))));
        c4χ =               fma(e4, 7./24, fma(e6, 29./120, e8*( 233./6720)));
        c6χ =                              fma(e6,  7./30,  e8*(  81./280));
        c8χ =                                               e8*(4279./20160);
    }

    /**
     * Creates a new projection initialized to the values of the given one. This constructor may be invoked after
     * we determined that the default implementation can be replaced by another one, for example using spherical
     * formulas instead of the ellipsoidal ones. This constructor allows to transfer all parameters to the new
     * instance without recomputing them.
     */
    ConformalProjection(final ConformalProjection other) {
        super(null, other);
        useIterations = other.useIterations;
        c2χ = other.c2χ;
        c4χ = other.c4χ;
        c6χ = other.c6χ;
        c8χ = other.c8χ;
    }

    /**
     * Computes the latitude for a value closely related to the <var>y</var> value of a Mercator projection.
     * This formula is also part of other projections, since Mercator can be considered as a special case of
     * Lambert Conic Conformal for instance.
     *
     * <p>This function is <em>almost</em> the converse of the {@link #expΨ(double, double)} function.
     * In a Mercator reverse projection, the value of the {@code rexpΨ} argument is {@code exp(-Ψ)}.</p>
     *
     * <p>The input should be a positive number, otherwise the result will be either outside
     * the [-π/2 … π/2] range, or will be NaN. Its behavior at some particular points is:</p>
     *
     * <ul>
     *   <li>φ(0)   =   π/2</li>
     *   <li>φ(1)   =   0</li>
     *   <li>φ(∞)   =  -π/2</li>
     * </ul>
     *
     * <b>Note:</b> §1.3.3 in Geomatics Guidance Note number 7 part 2 (April 2015) uses a series expansion
     * while USGS used an iterative method. The series expansion is twice faster than the iterative method
     * for the same precision, but this precision is achieved "only" for relatively small eccentricity like
     * the Earth's one. See the {@code MercatorMethodComparison} class in the test package for more discussion.
     *
     * @param  rexpΨ  the <em>reciprocal</em> of the value returned by {@link #expΨ}.
     * @return the latitude in radians.
     * @throws ProjectionException if the iteration does not converge.
     *
     * @see #expΨ(double, double)
     * @see #dy_dφ(double, double)
     */
    final double φ(final double rexpΨ) throws ProjectionException {
        /*
         * Get a first approximation of φ from Snyder (7-11). The result below would be exact if the
         * ellipsoid was actually a sphere. But if the eccentricity is different than 0, then we will
         * need to add a correction.
         *
         * Note that the φ value computed by the line below is called χ in EPSG guide.
         * We name it φ in our code because we will modify that value in-place in order to get φ.
         */
        double φ = (PI/2) - 2*atan(rexpΨ);                  // at this point == χ (conformal latitude).
        /*
         * Add a correction for the flattened shape of the Earth. The correction can be represented by an
         * infinite series. Here, we apply only the first 4 terms. Those terms are given by §1.3.3 in the
         * EPSG guidance note and modified by application of Snyder 3-35. We add those terms in reverse order,
         * beginning with the smallest values, for reducing rounding errors due to IEEE 754 arithmetic.
         *
         * For the original formula as published by EPSG, see MercatorMethodComparison.bySeriesExpansion()
         * in the test suite.
         */
        final double sin_2φ = sin(2*φ);
        final double cos_2φ = cos(2*φ);
        if (Formulas.USE_FMA) {
            φ = fma(sin_2φ, fma(cos_2φ, fma(cos_2φ, fma(cos_2φ, c8χ, c6χ), c4χ), c2χ), φ);
        } else {
            φ += sin_2φ * (c2χ + cos_2φ * (c4χ + cos_2φ * (c6χ + cos_2φ * c8χ)));
        }
        /*
         * Note: a previous version checked if the value of the smallest term c8χ⋅sin(8φ) was smaller than
         * the iteration tolerance. But this was not reliable enough. We use now a hard coded threshold
         * determined empirically by MercatorMethodComparison.
         */
        if (!useIterations) {
            return φ;
        }
        /*
         * We should never reach this point for map projections on Earth. But if the ellipsoid is for some
         * other planet having a high eccentricity, then the above series expansion may not be sufficient.
         * Try to improve by iteratively solving equation (7-9) from Snyder. However, instead of using
         * Snyder (7-11) as the starting point, we take the result of above calculation as the initial φ.
         * Assuming that it is closer to the real φ value, this save us some iteration loops and usually
         * gives us more accurate results (according MercatorMethodComparison tests).
         */
        final double hℯ = 0.5 * eccentricity;
        for (int it=0; it<MAXIMUM_ITERATIONS; it++) {
            final double ℯsinφ = eccentricity * sin(φ);
            final double Δφ = φ - (φ = PI/2 - 2*atan(rexpΨ * pow((1 - ℯsinφ)/(1 + ℯsinφ), hℯ)));
            if (!(abs(Δφ) > ITERATION_TOLERANCE)) {                 // Use '!' for accepting NaN.
                return φ;
            }
        }
        throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
    }

    /**
     * Computes <code>{@linkplain Math#exp(double) exp}(Ψ)</code> where Ψ is the isometric latitude.
     * This is part of the Mercator projection for the given latitude. This formula is also part of
     * Lambert Conic Conformal projection, since Mercator can be considered as a special case of that
     * Lambert projection with the equator as the single standard parallel.
     *
     * <p>The isometric latitude is given by the {@linkplain Math#log(double) natural logarithm} of the value returned
     * by this method. The <em>reciprocal</em> of this function {@code 1/expΨ(φ)} is the converse of {@link #φ(double)}.
     * Isometric latitude Ψ is related to conformal latitude χ by {@literal χ(φ) = gd(Ψ(φ))} where {@literal gd(x)} is the
     * Gudermannian function. There is many representations of that function, e.g. {@literal gd(Ψ) = 2⋅atan(expΨ) - π/2}.</p>
     *
     * <p>In IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015,
     * a function closely related to this method has the letter <var>t</var>.</p>
     *
     *
     * <h4>Properties</h4>
     * This function is used with φ values in the [-π/2 … π/2] range and has a periodicity of 2π.
     * The result is always a positive number when the φ argument is inside the above-cited range.
     * If, after removal of any 2π periodicity, φ is still outside the [-π/2 … π/2] range, then the
     * result is a negative number. In a Mercator projection, such negative number will result in NaN.
     *
     * <p>Some values are:</p>
     * <ul>
     *   <li>expΨ(NaN)    =  NaN</li>
     *   <li>expΨ(±∞)     =  NaN</li>
     *   <li>expΨ(-π/2)   =   0</li>
     *   <li>expΨ( 0  )   =   1</li>
     *   <li>expΨ(+π/2)   →   ∞  (actually some large value like 1.633E+16)</li>
     *   <li>expΨ(-φ)     =  1 / expΨ(φ)</li>
     * </ul>
     *
     *
     * <h4>The π/2 special case</h4>
     * The value at {@code Math.PI/2} is not exactly infinity because there is no exact representation of π/2.
     * However, since the conversion of 90° to radians gives {@code Math.PI/2}, we can presume that the user was
     * expecting infinity. The caller should check for the PI/2 special case himself if desired, as this method
     * does nothing special about it.
     *
     * <p>Note that the result for the φ value after {@code Math.PI/2} (as given by {@link Math#nextUp(double)})
     * is still positive, maybe because {@literal PI/2 < π/2 < nextUp(PI/2)}. Only the {@code nextUp(nextUp(PI/2))}
     * value become negative. Callers may need to take this behavior in account: special check for {@code Math.PI/2}
     * is not sufficient, the check needs to include at least the {@code nextUp(Math.PI/2)} case.</p>
     *
     *
     * <h4>Relationship with Snyder</h4>
     * This function is related to the following functions from Snyder:
     *
     * <ul>
     *   <li>(7-7) in the <cite>Mercator projection</cite> chapter.</li>
     *   <li>Reciprocal of (9-13) in the <cite>Oblique Mercator projection</cite> chapter.</li>
     *   <li>Reciprocal of (15-9) in the <cite>Lambert Conformal Conic projection</cite> chapter.</li>
     * </ul>
     *
     * @param  φ      the latitude in radians.
     * @param  ℯsinφ  the sine of the φ argument multiplied by {@link #eccentricity}.
     * @return {@code Math.exp(Ψ)} where Ψ is the isometric latitude.
     *
     * @see #φ(double)
     * @see #dy_dφ(double, double)
     * @see <a href="https://en.wikipedia.org/wiki/Latitude#Isometric_latitude">Isometric latitude on Wikipedia</a>
     */
    final double expΨ(final double φ, final double ℯsinφ) {
        /*
         * Note:   tan(π/4 - φ/2)  =  1 / tan(π/4 + φ/2)
         *
         * A + sign in the equation favorises slightly the accuracy in South hemisphere, while a - sign
         * favorises slightly the North hemisphere (but the differences are very small). In Apache SIS,
         * we handle that by changing the sign of some terms in the (de)normalisation matrices.
         */
        return tan(PI/4 + 0.5*φ) * pow((1 - ℯsinφ) / (1 + ℯsinφ), 0.5*eccentricity);
    }

    /**
     * Computes the partial derivative of a Mercator projection at the given latitude. This formula is also part of
     * other projections, since Mercator can be considered as a special case of Lambert Conic Conformal for instance.
     *
     * <p>In order to get the derivative of the {@link #expΨ(double, double)} function, caller can multiply
     * the returned value by {@code expΨ}.</p>
     *
     * @param  sinφ  the sine of latitude.
     * @param  cosφ  the cosine of latitude.
     * @return the partial derivative of a Mercator projection at the given latitude.
     *
     * @see #expΨ(double, double)
     * @see #φ(double)
     */
    final double dy_dφ(final double sinφ, final double cosφ) {
        return (1 / cosφ)  -  eccentricitySquared * cosφ / (1 - eccentricitySquared * (sinφ*sinφ));
    }
}
