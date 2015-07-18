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

import java.util.Map;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.parameter.Parameters;

import static java.lang.Math.*;


/**
 * Base class of {@link LambertConformal}, {@link Mercator} and {@link PolarStereographic} projections.
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
 * <p>The polar stereographic projection is not documented as a special case of Lambert Conic Conformal,
 * but the equations in the {@code PolarStereographic.transform(…)} and {@code inverseTransform(…)} methods
 * appear to be the same with the <var>n</var> factor fixed to 1 or -1, so we leverage the code provided by
 * this base class. This class hierarchy is only an implementation convenience and not part of public API.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
abstract class ConformalProjection extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 458860570536642265L;

    /**
     * The threshold value of {@link #excentricity} at which we consider the accuracy of the
     * series expansion insufficient. This threshold is determined empirically with the help
     * of the {@code MercatorMethodComparison} class in the test directory.
     * We choose the value where:
     *
     * <ul>
     *   <li>the average error of series expansion become greater than {@link NormalizedProjection#ITERATION_TOLERANCE},</li>
     *   <li>the maximal error of series expansion become greater than {@link NormalizedProjection#ANGULAR_TOLERANCE}.</li>
     * </ul>
     */
    static final double EXCENTRICITY_THRESHOLD = 0.16;

    /**
     * Whether to use the original formulas a published by EPSG, or their form modified using trigonometric identities.
     * The modified form uses trigonometric identifies for reducing the amount of calls to the {@link Math#sin(double)}
     * method. The identities used are:
     *
     * <ul>
     *   <li>sin(2⋅x) = 2⋅sin(x)⋅cos(x)</li>
     *   <li>sin(3⋅x) = (3 - 4⋅sin²(x))⋅sin(x)</li>
     *   <li>sin(4⋅x) = (4 - 8⋅sin²(x))⋅sin(x)⋅cos(x)</li>
     * </ul>
     *
     * Note that since this boolean is static final, the compiler should exclude the code in the branch that is never
     * executed (no need to comment-out that code).
     */
    private static final boolean ORIGINAL_FORMULA = false;

    /**
     * Coefficients in the series expansion used by {@link #φ(double)}.
     *
     * <p>Consider those fields as final. They are not only of the purpose of {@link #readObject(ObjectInputStream)}.</p>
     */
    private transient double c2χ, c4χ, c6χ, c8χ;

    /**
     * {@code true} if the {@link #excentricity} value is greater than or equals to {@link #EXCENTRICITY_THRESHOLD},
     * in which case the {@link #φ(double)} method will need to use an iterative method.
     *
     * <p>Consider this field as final. It is not only of the purpose of {@link #readObject(ObjectInputStream)}.</p>
     */
    private transient boolean useIterations;

    /**
     * Constructs a new map projection from the supplied parameters.
     *
     * @param method     Description of the map projection parameters.
     * @param parameters The parameters of the projection to be created.
     * @param roles Parameters to look for <cite>central meridian</cite>, <cite>scale factor</cite>,
     *        <cite>false easting</cite>, <cite>false northing</cite> and other values.
     */
    protected ConformalProjection(final OperationMethod method, final Parameters parameters,
            final Map<ParameterRole, ? extends ParameterDescriptor<Double>> roles)
    {
        super(method, parameters, roles);
        initialize();
    }

    /**
     * Computes the transient fields after construction or deserialization.
     */
    private void initialize() {
        useIterations = (excentricity >= EXCENTRICITY_THRESHOLD);
        final double e2 = excentricitySquared;
        final double e4 = e2 * e2;
        final double e6 = e2 * e4;
        final double e8 = e4 * e4;
        /*
         * For each line below, add the smallest values first in order to reduce rounding errors.
         * The smallest values are the one using the excentricity raised to the highest power.
         */
        c2χ  =    13/   360.* e8  +   1/ 12.* e6  +  5/24.* e4  +  e2/2;
        c4χ  =   811/ 11520.* e8  +  29/240.* e6  +  7/48.* e4;
        c6χ  =    81/  1120.* e8  +   7/120.* e6;
        c8χ  =  4279/161280.* e8;
        if (!ORIGINAL_FORMULA) {
            c4χ *= 2;
            c6χ *= 4;
            c8χ *= 8;
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
     * for the same precision, but this precision is achieved "only" for relatively small excentricity like
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
         * Get a first approximation of φ. The result below is exact if the ellipsoid is actually a sphere.
         * But if the excentricity is different than 0, then we will need to add a correction.
         */
        double φ = (PI/2) - 2*atan(expOfSouthing);          // Snyder (7-11)
        /*
         * Add a correction for the flattened shape of the Earth. The correction can be represented by an
         * infinite series. Here, we apply only the first 4 terms. Those terms are given by §1.3.3 in the
         * EPSG guidance note. Note that we add those terms in reverse order, beginning with the smallest
         * values, for reducing rounding errors due to IEEE 754 arithmetic.
         */
        if (ORIGINAL_FORMULA) {
            φ += c8χ * sin(8*φ)
               + c6χ * sin(6*φ)
               + c4χ * sin(4*φ)
               + c2χ * sin(2*φ);
        } else {
            /*
             * Same formula than above, be rewriten using trigonometric identities in order to have only two
             * calls to Math.sin/cos instead than 5. The performance gain is twice faster on some machines.
             */
            final double sin2χ     = sin(2*φ);
            final double sin_cos2χ = cos(2*φ) * sin2χ;
            final double sin_sin2χ = sin2χ * sin2χ;
            φ += c8χ * (0.50 - sin_sin2χ)*sin_cos2χ     // ÷8 compared to original formula
               + c6χ * (0.75 - sin_sin2χ)*sin2χ         // ÷4 compared to original formula
               + c4χ * (       sin_cos2χ)               // ÷2 compared to original formula
               + c2χ * sin2χ;
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
         * other planet having a high excentricity, then the above series expansion may not be sufficient.
         * Try to improve by iteratively solving equation (7-9) from Snyder. However instead than using
         * Snyder (7-11) as the starting point, we take the result of above calculation as the initial φ.
         * Assuming that it is closer to the real φ value, this save us some iteration loops and usually
         * gives us more accurate results (according MercatorMethodComparison tests).
         */
        final double hℯ = 0.5 * excentricity;
        for (int i=0; i<MAXIMUM_ITERATIONS; i++) {
            final double ℯsinφ = excentricity * sin(φ);
            double ε = abs(φ - (φ = PI/2 - 2*atan(expOfSouthing * pow((1 - ℯsinφ)/(1 + ℯsinφ), hℯ))));
            if (ε <= ITERATION_TOLERANCE) {
                return φ;
            }
        }
        if (Double.isNaN(expOfSouthing)) {
            return Double.NaN;
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
     * @param  ℯsinφ The sine of the φ argument multiplied by {@link #excentricity}.
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
        return tan(PI/4 + 0.5*φ) * pow((1 - ℯsinφ) / (1 + ℯsinφ), 0.5*excentricity);
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
        return (1 / cosφ)  -  excentricitySquared * cosφ / (1 - excentricitySquared * (sinφ*sinφ));
    }

    /**
     * Restores transient fields after deserialization.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initialize();
    }
}
