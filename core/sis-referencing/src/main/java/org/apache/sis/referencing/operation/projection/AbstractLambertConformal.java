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
 * Base class of {@link LambertConformal} and {@link Mercator} projections.
 * For this base class, the Mercator projection is considered as <cite>"a special limiting case of the
 * Lambert Conic Conformal map projection with the equator as the single standard parallel."</cite>
 * (Source: §1.3.3 in IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
abstract class AbstractLambertConformal extends NormalizedProjection {
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
    protected AbstractLambertConformal(final OperationMethod method, final Parameters parameters,
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
    }

    /**
     * Creates a new projection initialized to the values of the given one. This constructor may be invoked after
     * we determined that the default implementation can be replaced by an other one, for example using spherical
     * formulas instead than the ellipsoidal ones. This constructor allows to transfer all parameters to the new
     * instance without recomputing them.
     */
    AbstractLambertConformal(final AbstractLambertConformal other) {
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
        φ += c8χ * sin(8*φ)
           + c6χ * sin(6*φ)
           + c4χ * sin(4*φ)
           + c2χ * sin(2*φ);
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
     * Restores transient fields after deserialization.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initialize();
    }
}
