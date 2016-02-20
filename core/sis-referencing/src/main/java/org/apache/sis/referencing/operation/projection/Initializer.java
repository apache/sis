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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.projection.NormalizedProjection.ParameterRole;

import static java.lang.Math.*;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.internal.util.DoubleDouble.verbatim;


/**
 * Helper class for map projection constructions, providing formulas normally needed only at construction time.
 * Since map projection constructions should not happen very often, we afford using some double-double arithmetic.
 * The main intend is not to provide more accurate coordinate conversions (while it may be a nice side-effect),
 * but to improve the result of matrix multiplications when the map projection is part of a more complex chain
 * of transformations. More specifically we want to be able:
 *
 * <ul>
 *   <li>To convert degrees to radians, than back to degrees and find the original value.</li>
 *   <li>To convert axis length (optionally with flattening factor) to eccentricity, then back
 *       to axis length and find the original value.</li>
 * </ul>
 *
 * This has visible effects on WKT formatting among others, but also in our capability to detect simplification
 * opportunities in relatively complex chains of transformations.
 *
 * <p>As a general rule, we stop storing result with double-double precision after the point where we need
 * transcendental functions (sine, logarithm, <i>etc.</i>), since we do not have double-double versions of
 * those functions. Digits after the {@code double} part are usually not significant in such cases, except
 * in some relatively rare scenarios like 1 ± x where <var>x</var> is much smaller than 1.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Marechal (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
final class Initializer {
    /**
     * The parameters used for creating the map projection.
     * This object will be stored in the map projection.
     *
     * @see NormalizedProjection#getContextualParameters()
     */
    final ContextualParameters context;

    /**
     * The user-supplied parameters, stored temporarily while we transfer the information to {@link #context}.
     */
    final Parameters parameters;

    /**
     * The square of eccentricity: ℯ² = (a²-b²)/a² where
     * <var>ℯ</var> is the {@linkplain #eccentricity eccentricity},
     * <var>a</var> is the <cite>semi-major</cite> axis length and
     * <var>b</var> is the <cite>semi-minor</cite> axis length.
     *
     * <p>This is stored as a double-double value because this parameter is sometime used for computing back
     * the semi-minor axis length or the inverse flattening factor. In such case we wish to find the original
     * {@code double} parameter value without rounding errors. This wish usually do not apply to other internal
     * {@link NormalizedProjection} parameters.</p>
     */
    final DoubleDouble eccentricitySquared;

    /**
     * Map projection variant. This is a convenience field left at
     * the discretion of {@link NormalizedProjection} subclasses.
     */
    final byte variant;

    /**
     * Creates a new initializer. The parameters are described in
     * {@link NormalizedProjection#NormalizedProjection(OperationMethod, Parameters, Map)}.
     *
     * @param method     Description of the map projection parameters.
     * @param parameters The parameters of the projection to be created.
     * @param roles Parameters to look for <cite>central meridian</cite>, <cite>scale factor</cite>,
     *        <cite>false easting</cite>, <cite>false northing</cite> and other values.
     */
    Initializer(final OperationMethod method, final Parameters parameters,
            final Map<ParameterRole, ? extends ParameterDescriptor<? extends Number>> roles,
            final byte variant)
    {
        ensureNonNull("method",     method);
        ensureNonNull("parameters", parameters);
        ensureNonNull("roles",      roles);
        this.context    = new ContextualParameters(method);
        this.parameters = parameters;
        this.variant    = variant;
        /*
         * Note: we do not use Map.getOrDefault(K,V) below because the user could have explicitly associated
         * a null value to keys (we are paranoiac...) and because it conflicts with the "? extends" part of
         * in this constructor signature.
         */
        ParameterDescriptor<? extends Number> semiMajor = roles.get(ParameterRole.SEMI_MAJOR);
        ParameterDescriptor<? extends Number> semiMinor = roles.get(ParameterRole.SEMI_MINOR);
        if (semiMajor == null) semiMajor = MapProjection.SEMI_MAJOR;
        if (semiMinor == null) semiMinor = MapProjection.SEMI_MINOR;

        final double a  = getAndStore(semiMajor);
        final double b  = getAndStore(semiMinor);
        final double λ0 = getAndStore(roles.get(ParameterRole.CENTRAL_MERIDIAN));
        final double fe = getAndStore(roles.get(ParameterRole.FALSE_EASTING))
                        - getAndStore(roles.get(ParameterRole.FALSE_WESTING));
        final double fn = getAndStore(roles.get(ParameterRole.FALSE_NORTHING))
                        - getAndStore(roles.get(ParameterRole.FALSE_SOUTHING));

        eccentricitySquared = new DoubleDouble();
        DoubleDouble k = new DoubleDouble(a);  // The value by which to multiply all results of normalized projection.
        if (a != b) {
            /*
             * (1) Using axis lengths:  ℯ² = 1 - (b/a)²
             * (2) Using flattening;    ℯ² = 2f - f²     where f is the (NOT inverse) flattening factor.
             *
             * If the inverse flattening factor is the definitive factor for the ellipsoid, we use (2).
             * Otherwise use (1). With double-double arithmetic, this makes a difference in the 3 last
             * digits for the WGS84 ellipsoid.
             */
            boolean isIvfDefinitive;
            try {
                isIvfDefinitive = parameters.parameter(Constants.IS_IVF_DEFINITIVE).booleanValue();
            } catch (ParameterNotFoundException e) {
                /*
                 * Should never happen with Apache SIS implementation, but may happen if the given parameters come
                 * from another implementation. We can safely abandon our attempt to get the inverse flattening value,
                 * since it was redundant with semi-minor axis length.
                 */
                isIvfDefinitive = false;
            }
            /*
             * The ellipsoid parameters (a, b or ivf) are assumed accurate in base 10 rather than in base 2,
             * because they are defined by authorities. For example the semi-major axis length of the WGS84
             * ellipsoid is equal to exactly 6378137 metres by definition of that ellipsoid. The DoubleDouble
             * constructor applies corrections for making those values more accurate in base 10 rather than 2.
             */
            if (isIvfDefinitive) {
                final DoubleDouble f = new DoubleDouble(parameters.parameter(Constants.INVERSE_FLATTENING).doubleValue());
                f.inverseDivide(1,0);
                eccentricitySquared.setFrom(f);
                eccentricitySquared.multiply(2,0);
                f.square();
                eccentricitySquared.subtract(f);
            } else {
                final DoubleDouble rs = new DoubleDouble(b);
                rs.divide(k);    // rs = b/a
                rs.square();
                eccentricitySquared.value = 1;
                eccentricitySquared.subtract(rs);
            }
            final ParameterDescriptor<? extends Number> radius = roles.get(ParameterRole.LATITUDE_OF_CONFORMAL_SPHERE_RADIUS);
            if (radius != null) {
                /*
                 * EPSG said: R is the radius of the sphere and will normally be one of the CRS parameters.
                 * If the figure of the earth used is an ellipsoid rather than a sphere then R should be calculated
                 * as the radius of the conformal sphere at the projection origin at latitude φ₀ using the formula
                 * for Rc given in section 1.2, table 3.
                 *
                 * Table 3 gives:
                 * Radius of conformal sphere Rc = a √(1 – ℯ²) / (1 – ℯ²⋅sin²φ)
                 *
                 * Using √(1 – ℯ²) = b/a we rewrite as: Rc = b / (1 – ℯ²⋅sin²φ)
                 *
                 * Equivalent Java code:
                 *
                 *     final double sinφ = sin(toRadians(parameters.doubleValue(radius)));
                 *     k = b / (1 - eccentricitySquared * (sinφ*sinφ));
                 */
                k = rν2(sin(toRadians(parameters.doubleValue(radius))));
                k.inverseDivide(b, 0);
            }
        }
        /*
         * Scale factor is assumed more accurate in base 10 than in base 2 for the same reason than for the
         * ellipsoid parameters (i.e. is a value given by authority as part of map projection definition).
         * Again, DoubleDouble constructor will take care of computing a correction.
         */
        final ParameterDescriptor<? extends Number> scaleFactor = roles.get(ParameterRole.SCALE_FACTOR);
        if (scaleFactor != null) {
            k.multiply(getAndStore(scaleFactor));
        }
        /*
         * Set meridian rotation, scale factor, false easting and false northing parameter values
         * in the (de)normalization matrices.
         */
        context.normalizeGeographicInputs(λ0);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertAfter(0, k, new DoubleDouble(fe));
        denormalize.convertAfter(1, k, new DoubleDouble(fn));
    }

    /**
     * Gets a parameter value identified by the given descriptor and stores it in the {@link #context}.
     * A "contextual parameter" is a parameter that apply to the normalize → {@code this} → denormalize
     * chain as a whole. It does not really apply to a {@code NormalizedProjection} instance taken alone.
     *
     * <p>This method performs the following actions:</p>
     * <ul>
     *   <li>Convert the value to the units specified by the descriptor.</li>
     *   <li>Ensure that the value is contained in the range specified by the descriptor.</li>
     *   <li>Store the value only if different than the default value.</li>
     * </ul>
     */
    final double getAndStore(final ParameterDescriptor<? extends Number> descriptor) {
        if (descriptor == null) {
            return 0;                           // Default value for all parameters except scale factor.
        }
        /*
         * Get the parameter value, or its default value if the parameter was not set. That default value
         * (which is specified by the descriptor of the user-supplied parameters) is not necessarily the
         * same than the default value of the map projection implementation (which is specified by the
         * descriptor given in argument to this method).
         */
        final double value = parameters.doubleValue(descriptor);    // Apply a unit conversion if needed.
        final Number defaultValue = descriptor.getDefaultValue();
        if (defaultValue == null || !defaultValue.equals(value)) {
            MapProjection.validate(descriptor, value);
            context.parameter(descriptor.getName().getCode()).setValue(value);
        }
        return value;
    }

    /**
     * Same as {@link #getAndStore(ParameterDescriptor)}, but returns the given default value if the parameter
     * is not specified.  This method shall be used only for parameters having a default value more complex than
     * what we can represent in {@link ParameterDescriptor#getDefaultValue()}.
     */
    final double getAndStore(final ParameterDescriptor<Double> descriptor, final double defaultValue) {
        final Double value = parameters.getValue(descriptor);   // Apply a unit conversion if needed.
        if (value == null) {
            return defaultValue;
        }
        MapProjection.validate(descriptor, value);
        context.parameter(descriptor.getName().getCode()).setValue(value);
        return value;
    }

    /**
     * Returns {@code b/a} where {@code a} is the semi-major axis length and {@code b} the semi-minor axis length.
     * We retrieve this value from the eccentricity with {@code b/a = sqrt(1-ℯ²)}.
     */
    final DoubleDouble axisLengthRatio() {
        final DoubleDouble b = new DoubleDouble(1,0);
        b.subtract(eccentricitySquared);
        b.sqrt();
        return b;
    }

    /**
     * Computes the square of the reciprocal of the radius of curvature of the ellipsoid
     * perpendicular to the meridian at latitude φ. That radius of curvature is:
     *
     * <blockquote>ν = 1 / √(1 - ℯ²⋅sin²φ)</blockquote>
     *
     * This method returns 1/ν².
     *
     * <div class="section">Relationship with Snyder</div>
     * This is related to functions (14-15) from Snyder (used for computation of scale factors
     * at the true scale latitude) as below:
     *
     * <blockquote>m = cosφ / sqrt(rν²)</blockquote>
     *
     * Special cases:
     * <ul>
     *   <li>If φ is 0°, then <var>m</var> is 1.</li>
     *   <li>If φ is ±90°, then <var>m</var> is 0 provided that we are not in the spherical case
     *       (otherwise we get {@link Double#NaN}).</li>
     * </ul>
     *
     * @param  sinφ The sine of the φ latitude.
     * @return Reciprocal squared of the radius of curvature of the ellipsoid
     *         perpendicular to the meridian at latitude φ.
     */
    private DoubleDouble rν2(final double sinφ) {
        if (DoubleDouble.DISABLED) {
            return verbatim(1 - eccentricitySquared.value * (sinφ*sinφ));
        }
        final DoubleDouble t = verbatim(sinφ);
        t.square();
        t.multiply(eccentricitySquared);

        // Compute 1 - ℯ²⋅sin²φ.  Since  ℯ²⋅sin²φ  may be small,
        // this is where double-double arithmetic has more value.
        t.negate();
        t.add(1,0);
        return t;
    }

    /**
     * Returns the radius of the conformal sphere (assuming a semi-major axis length of 1) at a given latitude.
     * The radius of conformal sphere is computed from ρ, which is the radius of curvature in the meridian at
     * latitude φ, and ν which is the radius of curvature in the prime vertical, as below:
     *
     * <blockquote>Rc = √(ρ⋅ν) = √(1 – ℯ²) / (1 – ℯ²sin²φ)</blockquote>
     *
     * This is a function of latitude and therefore not constant. When used for spherical projections
     * the use of φ₀ (or φ₁ as relevant to method) for φ is suggested, except if the projection is
     * equal area when the radius of authalic sphere should be used.
     *
     * @param  sinφ The sine of the φ latitude.
     * @return Radius of the conformal sphere at latitude φ.
     */
    final double radiusOfConformalSphere(final double sinφ) {
        final DoubleDouble Rc = verbatim(1);
        Rc.subtract(eccentricitySquared);       //  1 - ℯ²
        Rc.sqrt();                              //  √(1 - ℯ²)
        Rc.divide(rν2(sinφ));                   //  √(1 - ℯ²) / (1 - ℯ²sin²φ)
        return Rc.value;
    }

    /**
     * Returns the scale factor at latitude φ. This is computed as:
     *
     * <blockquote>cosφ / sqrt(rν2(sinφ))</blockquote>
     *
     * The result is returned as a {@code double} because the limited precision of {@code sinφ} and {@code cosφ}
     * makes the error term meaningless. We use double-double arithmetic only for intermediate calculation.
     *
     * @param  sinφ The sine of the φ latitude.
     * @param  cosφ The cosine of the φ latitude.
     * @return Scale factor at latitude φ.
     */
    final double scaleAtφ(final double sinφ, final double cosφ) {
        final DoubleDouble s = rν2(sinφ);
        s.sqrt();
        s.inverseDivide(cosφ, 0);
        return s.value;
    }
}
