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
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;


/**
 * Base class of {@link LambertConicConformal}, {@link Mercator} and {@link PolarStereographic} projections.
 * All those projections have in common the property of being <cite>conformal</cite>, i.e. they preserve
 * angles locally. However we do not put this base class in public API because we do not (yet) guarantee
 * than all conformal projections will extend this base class.
 *
 * <p>This base class can been seen as a generalization of <cite>Lambert Conic Conformal</cite> projection,
 * which includes some other projections like Mercator and Polar Stereographic as special cases.
 * For this base class, the Mercator projection is considered as <cite>"a special limiting case of the
 * Lambert Conic Conformal map projection with the equator as the single standard parallel."</cite>
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
 * However the formulas provided in this {@code ConformalProjection} class can be seen as a special case of
 * Transverse Mercator formulas for <var>x</var> = 0.</p>
 *
 * <div class="note"><b>Reference:</b>
 * “Lambert developed the regular Conformal Conic as the oblique aspect of a family containing the previously
 * known polar Stereographic and regular Mercator projections. (…) If the standard parallels are symmetrical
 * about the Equator, the regular Mercator results (although formulas must be revised). If the only standard
 * parallel is a pole, the polar Stereographic results.” (Snyder, page 105)</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
abstract class ConformalProjection extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 458860570536642265L;

    /**
     * {@code false} for using the original formulas as published by EPSG, or {@code true} for using formulas
     * modified using trigonometric identities. The use of trigonometric identities is for reducing the amount
     * of calls to the {@link Math#sin(double)} and similar methods. Some identities used are:
     *
     * <ul>
     *   <li>sin(2θ) = 2⋅sinθ⋅cosθ</li>
     *   <li>cos(2θ) = cos²θ - sin²θ</li>
     *   <li>sin(3θ) = (3 - 4⋅sin²θ)⋅sinθ</li>
     *   <li>cos(3θ) = (4⋅cos³θ) - 3⋅cosθ</li>
     *   <li>sin(4θ) = (4 - 8⋅sin²θ)⋅sinθ⋅cosθ</li>
     *   <li>cos(4θ) = (8⋅cos⁴θ) - (8⋅cos²θ) + 1</li>
     * </ul>
     *
     * Hyperbolic formulas (used in Transverse Mercator projection):
     *
     * <ul>
     *   <li>sinh(2θ) = 2⋅sinhθ⋅coshθ</li>
     *   <li>cosh(2θ) = cosh²θ + sinh²θ   =   2⋅cosh²θ - 1   =   1 + 2⋅sinh²θ</li>
     *   <li>sinh(3θ) = (3 + 4⋅sinh²θ)⋅sinhθ</li>
     *   <li>cosh(3θ) = ((4⋅cosh²θ) - 3)⋅coshθ</li>
     *   <li>sinh(4θ) = (1 + 2⋅sinh²θ)⋅4.sinhθ⋅coshθ
     *                = 4.cosh(2θ).sinhθ⋅coshθ</li>
     *   <li>cosh(4θ) = (8⋅cosh⁴θ) - (8⋅cosh²θ) + 1
     *                = 8⋅cosh²(θ) ⋅ (cosh²θ - 1) + 1
     *                = 8⋅cosh²(θ) ⋅ sinh²(θ) + 1
     *                = 2⋅sinh²(2θ) + 1</li>
     * </ul>
     *
     * Note that since this boolean is static final, the compiler should exclude the code in the branch that is never
     * executed (no need to comment-out that code).
     *
     * @see #identityEquals(double, double)
     */
    static final boolean ALLOW_TRIGONOMETRIC_IDENTITIES = true;

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
     *
     * <p><strong>Consider this field as final!</strong>
     * It is not final only for the purpose of {@link #readObject(ObjectInputStream)}.
     * This field is not serialized because its value may depend on the version of this
     * {@code ConformalProjection} class.</p>
     */
    private transient boolean useIterations;

    /**
     * Coefficients in the series expansion of the inverse projection,
     * depending only on {@linkplain #eccentricity eccentricity} value.
     * The series expansion is of the following form, where ｆ(θ) is typically sin(θ):
     *
     *     <blockquote>ci₂⋅ｆ(2θ) + ci₄⋅ｆ(4θ) + ci₆⋅ｆ(6θ) + ci₈⋅ｆ(8θ)</blockquote>
     *
     * This {@code ConformalProjection} class uses those coefficients in {@link #φ(double)}.
     * However some subclasses may compute those coefficients differently and use them in a
     * different series expansion (but for the same purpose).
     *
     * <p><strong>Consider those fields as final!</strong> They are not final only for sub-class
     * constructors convenience and for the purpose of {@link #readObject(ObjectInputStream)}.</p>
     *
     * @see #computeCoefficients()
     */
    transient double ci2, ci4, ci6, ci8;

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer.
     *
     * <p>It is sub-classes responsibility to invoke {@code super.computeCoefficients()} in their
     * constructor when ready, or to compute the coefficients themselves.</p>
     *
     * @param initializer The initializer for computing map projection internal parameters.
     */
    ConformalProjection(final Initializer initializer) {
        super(initializer);
    }

    /**
     * Computes the coefficients in the series expansions from the {@link #eccentricitySquared} value.
     * This method shall be invoked after {@code ConformalProjection} construction or deserialization.
     */
    void computeCoefficients() {
        useIterations = (eccentricity >= ECCENTRICITY_THRESHOLD);
        final double e2 = eccentricitySquared;
        final double e4 = e2 * e2;
        final double e6 = e2 * e4;
        final double e8 = e4 * e4;
        /*
         * For each line below, add the smallest values first in order to reduce rounding errors.
         * The smallest values are the one using the eccentricity raised to the highest power.
         */
        ci2  =    13/   360.* e8  +   1/ 12.* e6  +  5/24.* e4  +  e2/2;
        ci4  =   811/ 11520.* e8  +  29/240.* e6  +  7/48.* e4;
        ci6  =    81/  1120.* e8  +   7/120.* e6;
        ci8  =  4279/161280.* e8;
        /*
         * When rewriting equations using trigonometric identities, some constants appear.
         * For example sin(2θ) = 2⋅sinθ⋅cosθ, so we can factor out the 2 constant into the
         * corresponding 'c' field.
         */
        if (ALLOW_TRIGONOMETRIC_IDENTITIES) {
            // Multiplication by powers of 2 does not bring any additional rounding error.
            ci4 *= 2;
            ci6 *= 4;
            ci8 *= 8;
        }
    }

    /**
     * Creates a new projection initialized to the values of the given one. This constructor may be invoked after
     * we determined that the default implementation can be replaced by an other one, for example using spherical
     * formulas instead than the ellipsoidal ones. This constructor allows to transfer all parameters to the new
     * instance without recomputing them.
     */
    ConformalProjection(final ConformalProjection other) {
        super(other);
        useIterations = other.useIterations;
        ci2 = other.ci2;
        ci4 = other.ci4;
        ci6 = other.ci6;
        ci8 = other.ci8;
    }

    /**
     * Computes the latitude for a value closely related to the <var>y</var> value of a Mercator projection.
     * This formula is also part of other projections, since Mercator can be considered as a special case of
     * Lambert Conic Conformal for instance.
     *
     * <div class="note"><b>Warning:</b>
     * this method is valid only if the series expansion coefficients where computed by the
     * {@link #computeCoefficients()} method defined in this class. This is not the case of
     * {@link TransverseMercator} for instance. Note however that even in the later case,
     * this method can still be seen as a special case of {@code TransverseMercator} formulas.</div>
     *
     * <p>This function is <em>almost</em> the converse of the {@link #expOfNorthing(double, double)} function.
     * In a Mercator inverse projection, the value of the {@code expOfSouthing} argument is {@code exp(-y)}.</p>
     *
     * <p>The input should be a positive number, otherwise the result will be either outside
     * the [-π/2 … π/2] range, or will be NaN. Its behavior at some particular points is:</p>
     *
     * <ul>
     *   <li>φ(0)   =   π/2</li>
     *   <li>φ(1)   =   0</li>
     *   <li>φ(∞)   =  -π/2.</li>
     * </ul>
     *
     * <b>Note:</b> §1.3.3 in Geomatics Guidance Note number 7 part 2 (April 2015) uses a series expansion
     * while USGS used an iterative method. The series expansion is twice faster than the iterative method
     * for the same precision, but this precision is achieved "only" for relatively small eccentricity like
     * the Earth's one. See the {@code MercatorMethodComparison} class in the test package for more discussion.
     *
     * @param  expOfSouthing The <em>reciprocal</em> of the value returned by {@link #expOfNorthing}.
     * @return The latitude in radians.
     * @throws ProjectionException if the iteration does not converge.
     *
     * @see #expOfNorthing(double, double)
     * @see #dy_dφ(double, double)
     */
    final double φ(final double expOfSouthing) throws ProjectionException {
        /*
         * Get a first approximation of φ from Snyder (7-11). The result below would be exact if the
         * ellipsoid was actually a sphere. But if the eccentricity is different than 0, then we will
         * need to add a correction.
         *
         * Note that the φ value computed by the line below is called χ in EPSG guide.
         * We name it φ in our code because we will modify that value in-place in order to get φ.
         */
        double φ = (PI/2) - 2*atan(expOfSouthing);          // at this point == χ
        /*
         * Add a correction for the flattened shape of the Earth. The correction can be represented by an
         * infinite series. Here, we apply only the first 4 terms. Those terms are given by §1.3.3 in the
         * EPSG guidance note. Note that we add those terms in reverse order, beginning with the smallest
         * values, for reducing rounding errors due to IEEE 754 arithmetic.
         */
        if (!ALLOW_TRIGONOMETRIC_IDENTITIES) {
            φ += ci8 * sin(8*φ)
               + ci6 * sin(6*φ)
               + ci4 * sin(4*φ)
               + ci2 * sin(2*φ);
        } else {
            /*
             * Same formula than above, be rewriten using trigonometric identities in order to have only two
             * calls to Math.sin/cos instead than 5. The performance gain is twice faster on tested machine.
             */
            final double sin_2φ = sin(2*φ);
            final double sin2 = sin_2φ * sin_2φ;
            φ += ((ci4 + ci8 * (0.50 - sin2)) * cos(2*φ)
                + (ci2 + ci6 * (0.75 - sin2))) * sin_2φ;
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
         * Try to improve by iteratively solving equation (7-9) from Snyder. However instead than using
         * Snyder (7-11) as the starting point, we take the result of above calculation as the initial φ.
         * Assuming that it is closer to the real φ value, this save us some iteration loops and usually
         * gives us more accurate results (according MercatorMethodComparison tests).
         */
        final double hℯ = 0.5 * eccentricity;
        for (int it=0; it<MAXIMUM_ITERATIONS; it++) {
            final double ℯsinφ = eccentricity * sin(φ);
            final double Δφ = φ - (φ = PI/2 - 2*atan(expOfSouthing * pow((1 - ℯsinφ)/(1 + ℯsinφ), hℯ)));
            if (!(abs(Δφ) > ITERATION_TOLERANCE)) {     // Use '!' for accepting NaN.
                return φ;
            }
        }
        throw new ProjectionException(Errors.Keys.NoConvergence);
    }

    /**
     * Computes part of the Mercator projection for the given latitude. This formula is also part of
     * Lambert Conic Conformal projection, since Mercator can be considered as a special case of that
     * Lambert projection with the equator as the single standard parallel.
     *
     * <p>The Mercator projection is given by the {@linkplain Math#log(double) natural logarithm}
     * of the value returned by this method. This function is <em>almost</em> the converse of
     * {@link #φ(double)}.
     *
     * <p>In IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015,
     * a function closely related to this one has the letter <var>t</var>.</p>
     *
     *
     * <div class="section">Properties</div>
     * This function is used with φ values in the [-π/2 … π/2] range and has a periodicity of 2π.
     * The result is always a positive number when the φ argument is inside the above-cited range.
     * If, after removal of any 2π periodicity, φ is still outside the [-π/2 … π/2] range, then the
     * result is a negative number. In a Mercator projection, such negative number will result in NaN.
     *
     * <p>Some values are:</p>
     * <ul>
     *   <li>expOfNorthing(NaN)    =  NaN</li>
     *   <li>expOfNorthing(±∞)     =  NaN</li>
     *   <li>expOfNorthing(-π/2)   =   0</li>
     *   <li>expOfNorthing( 0  )   =   1</li>
     *   <li>expOfNorthing(+π/2)   →   ∞  (actually some large value like 1.633E+16)</li>
     *   <li>expOfNorthing(-φ)     =  1 / expOfNorthing(φ)</li>
     * </ul>
     *
     *
     * <div class="section">The π/2 special case</div>
     * The value at {@code Math.PI/2} is not exactly infinity because there is no exact representation of π/2.
     * However since the conversion of 90° to radians gives {@code Math.PI/2}, we can presume that the user was
     * expecting infinity. The caller should check for the PI/2 special case himself if desired, as this method
     * does nothing special about it.
     *
     * <p>Note that the result for the φ value after {@code Math.PI/2} (as given by {@link Math#nextUp(double)})
     * is still positive, maybe because {@literal PI/2 < π/2 < nextUp(PI/2)}. Only the {@code nextUp(nextUp(PI/2))}
     * value become negative. Callers may need to take this behavior in account: special check for {@code Math.PI/2}
     * is not sufficient, the check needs to include at least the {@code nextUp(Math.PI/2)} case.</p>
     *
     *
     * <div class="section">Relationship with Snyder</div>
     * This function is related to the following functions from Snyder:
     *
     * <ul>
     *   <li>(7-7) in the <cite>Mercator projection</cite> chapter.</li>
     *   <li>Reciprocal of (9-13) in the <cite>Oblique Mercator projection</cite> chapter.</li>
     *   <li>Reciprocal of (15-9) in the <cite>Lambert Conformal Conic projection</cite> chapter.</li>
     * </ul>
     *
     * @param  φ     The latitude in radians.
     * @param  ℯsinφ The sine of the φ argument multiplied by {@link #eccentricity}.
     * @return {@code Math.exp} of the Mercator projection of the given latitude.
     *
     * @see #φ(double)
     * @see #dy_dφ(double, double)
     */
    final double expOfNorthing(final double φ, final double ℯsinφ) {
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
     * <p>In order to get the derivative of the {@link #expOfNorthing(double, double)} function, call can multiply
     * the returned value by by {@code expOfNorthing}.</p>
     *
     * @param  sinφ the sine of latitude.
     * @param  cosφ The cosine of latitude.
     * @return The partial derivative of a Mercator projection at the given latitude.
     *
     * @see #expOfNorthing(double, double)
     * @see #φ(double)
     */
    final double dy_dφ(final double sinφ, final double cosφ) {
        return (1 / cosφ)  -  eccentricitySquared * cosφ / (1 - eccentricitySquared * (sinφ*sinφ));
    }

    /**
     * Restores transient fields after deserialization.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        computeCoefficients();
    }

    /**
     * Verifies if a trigonometric identity produced the expected value. This method is used in assertions only,
     * for values close to the [-1 … +1] range. The tolerance threshold is approximatively 1.5E-12 (note that it
     * still about 7000 time greater than {@code Math.ulp(1.0)}).
     *
     * @see #ALLOW_TRIGONOMETRIC_IDENTITIES
     */
    static boolean identityEquals(final double actual, final double expected) {
        return !(abs(actual - expected) >                               // Use !(a > b) instead of (a <= b) in order to tolerate NaN.
                (ANGULAR_TOLERANCE / 1000) * max(1, abs(expected)));    // Increase tolerance for values outside the [-1 … +1] range.
    }
}
