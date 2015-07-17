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
import java.util.EnumMap;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.internal.referencing.provider.TransverseMercatorSouth;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;
import static org.apache.sis.math.MathFunctions.asinh;
import static org.apache.sis.math.MathFunctions.atanh;


/**
 * <cite>Transverse Mercator</cite> projection (EPSG codes 9807).
 * This class implements the "JHS formulas" reproduced in
 * IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
 *
 * <div class="section">Description</div>
 * This is a cylindrical projection, in which the cylinder has been rotated 90°.
 * Instead of being tangent to the equator (or to an other standard latitude), it is tangent to a central meridian.
 * Deformation are more important as we are going further from the central meridian.
 * The Transverse Mercator projection is appropriate for region which have a greater extent north-south than east-west.
 *
 * <p>There are a number of versions of the Transverse Mercator projection including the Universal (UTM)
 * and Modified (MTM) Transverses Mercator projections. In these cases the earth is divided into zones.
 * For the UTM the zones are 6 degrees wide, numbered from 1 to 60 proceeding east from 180 degrees longitude,
 * and between latitude 84 degrees North and 80 degrees South. The central meridian is taken as the center of the zone
 * and the latitude of origin is the equator. A scale factor of 0.9996 and false easting of 500000 metres is used for
 * all zones and a false northing of 10000000 metres is used for zones in the southern hemisphere.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see Mercator
 * @see ObliqueMercator
 */
public class TransverseMercator extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4717976245811852528L;

    /**
     * Internal coefficients for computation, depending only on values of standards parallels.
     * Defined in §1.3.5.1 of IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
     */
    private final double h1, h2, h3, h4, ih1, ih2, ih3, ih4;

    /**
     * Returns the (<var>role</var> → <var>parameter</var>) associations for a Transverse Mercator projection.
     *
     * @return The roles map to give to super-class constructor.
     */
    private static Map<ParameterRole, ParameterDescriptor<Double>> roles(final boolean isSouth) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        ParameterRole xOffset = ParameterRole.FALSE_EASTING;
        ParameterRole yOffset = ParameterRole.FALSE_NORTHING;
        if (isSouth) {
            xOffset = ParameterRole.FALSE_WESTING;
            yOffset = ParameterRole.FALSE_SOUTHING;
        }
        roles.put(ParameterRole.CENTRAL_MERIDIAN, org.apache.sis.internal.referencing.provider.TransverseMercator.LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.SCALE_FACTOR,     org.apache.sis.internal.referencing.provider.TransverseMercator.SCALE_FACTOR);
        roles.put(xOffset,                        org.apache.sis.internal.referencing.provider.TransverseMercator.FALSE_EASTING);
        roles.put(yOffset,                        org.apache.sis.internal.referencing.provider.TransverseMercator.FALSE_NORTHING);
        return roles;
    }

    /**
     * Returns the type of the projection based on the name and identifier of the given parameter group.
     */
    private static boolean isSouth(final ParameterDescriptorGroup parameters) {
        return identMatch(parameters, "(?i).*\\bSouth\\b.*", TransverseMercatorSouth.IDENTIFIER);
    }

    /**
     * Creates a Transverse Mercator projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Transverse Mercator"</cite>.</li>
     * </ul>
     *
     * @param method     Description of the projection parameters.
     * @param parameters The parameter values of the projection to create.
     */
    public TransverseMercator(final OperationMethod method, final Parameters parameters) {
        super(method, parameters, roles(isSouth(parameters.getDescriptor())));
        final double φ0 = toRadians(getAndStore(parameters,
                org.apache.sis.internal.referencing.provider.TransverseMercator.LATITUDE_OF_ORIGIN));
        final double rs = parameters.doubleValue(MapProjection.SEMI_MINOR)
                        / parameters.doubleValue(MapProjection.SEMI_MAJOR);

        final double n  = (1 - rs) / (1 + rs);       // Rewrite of n = f / (2-f)
        final double n2 = n  * n;
        final double n3 = n2 * n;
        final double n4 = n2 * n2;
        /*
         * Compute B  =  (n4/64 + n2/4 + 1) / (n + 1)
         * Opportunistically uses double-double arithmetic since we use it anyway for denormalization matrix.
         */
        final DoubleDouble B = new DoubleDouble(n);
        B.add(1);
        B.inverseDivide(1, n4/64 + n2/4);
        /*
         * Coefficients for direct projection.
         * Add the smallest values first in order to reduce rounding errors.
         */
        h1 = (   41. /    180)*n4  +  ( 5. /  16)*n3  +  (-2. /  3)*n2  +  n/2;
        h2 = (  557. /   1440)*n4  +  (-3. /   5)*n3  +  (13. / 48)*n2;
        h3 = ( -103. /    140)*n4  +  (61. / 240)*n3;
        h4 = (49561. / 161280)*n4;
        /*
         * Coefficients for inverse projection.
         * Add the smallest values first in order to reduce rounding errors.
         */
        ih1 = (  -1. /    360)*n4  +  (37. /  96)*n3  +  (-2. /  3)*n2  +  n/2;
        ih2 = (-437. /   1440)*n4  +  ( 1. /  15)*n3  +  ( 1. / 48)*n2;
        ih3 = ( -37. /    840)*n4  +  (17. / 480)*n3;
        ih4 = (4397. / 161280)*n4;
        /*
         * Compute M₀ = B⋅(ξ₁ + ξ₂ + ξ₃ + ξ₄) and negate in anticipation for what will be needed
         * in the denormalization matrix. We opportunistically use double-double arithmetic, but
         * the precision is actually not better than double (in current SIS version) because of
         * the precision of trigonometric functions. We may improve on that in the future if it
         * seems useful.
         *
         * NOTE: the EPSG documentation makes special cases for φ₀ = 0 or ±π/2. This is not
         * needed here; we verified that the code below produces naturally the expected values.
         */
        final double Q = asinh(tan(φ0)) - excentricity * atanh(excentricity * sin(φ0));
        final double β = atan(sinh(Q));
        final DoubleDouble M0 = new DoubleDouble(β, 0);
        M0.add(h1 * sin(2*β), 0);
        M0.add(h2 * sin(4*β), 0);
        M0.add(h3 * sin(6*β), 0);
        M0.add(h4 * sin(8*β), 0);
        M0.multiply(B);
        M0.negate();
        /*
         * At this point, all parameters have been processed. Now store
         * the linear operations in the (de)normalize affine transforms:
         *
         * Normalization:
         *   - Subtract central meridian to longitudes (done by the super-class constructor).
         *   - Convert longitudes and latitudes from degrees to radians (done by the super-class constructor)
         *
         * Denormalization
         *   - Scale x and y by B.
         *   - Subtract M0 to the northing.
         *   - Multiply by the scale factor (done by the super-class constructor).
         *   - Add false easting and false northing (done by the super-class constructor).
         */
        final MatrixSIS denormalize = context.getMatrix(false);
        denormalize.convertBefore(0, B, null);
        denormalize.convertBefore(1, B, M0);
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     *
     * @return The matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinate can not be converted.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws ProjectionException
    {
        final double λ  = srcPts[srcOff];
        final double φ  = srcPts[srcOff + 1];
        final double Q  = asinh(tan(φ)) - atanh(sin(φ)*excentricity)*excentricity;
        final double β  = atan(sinh(Q));

        // TODO: sin(atan(x)) = x / sqrt(1+x²)
        //       cos(atan(x)) = 1 / sqrt(1+x²)
        final double η0 = atanh(cos(β) * sin(λ));
        final double ξ0 = asin(sin(β) * cosh(η0));

        // TODO: use trigonometric identities.
        // See AbstractLambertConformal for example.
        final double ξ = h4 * sin(8*ξ0) * cosh(8*η0)
                       + h3 * sin(6*ξ0) * cosh(6*η0)
                       + h2 * sin(4*ξ0) * cosh(4*η0)
                       + h1 * sin(2*ξ0) * cosh(2*η0)
                       + ξ0;

        final double η = h4 * cos(8*ξ0) * sinh(8*η0)
                       + h3 * cos(6*ξ0) * sinh(6*η0)
                       + h2 * cos(4*ξ0) * sinh(4*η0)
                       + h1 * cos(2*ξ0) * sinh(2*η0)
                       + η0;

        if (dstPts != null) {
            dstPts[dstOff    ] = η;
            dstPts[dstOff + 1] = ξ;
        }
        if (!derivate) {
            return null;
        }

        // TODO: compute projection derivative.
        return null;
    }

    /**
     * Transforms the specified (η,ξ) coordinates and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double η = srcPts[srcOff    ];
        final double ξ = srcPts[srcOff + 1];

        // TODO: use trigonometric identities.
        // See AbstractLambertConformal for example.
        final double ξ0 = ξ - (ih4 * sin(8*ξ) * cosh(8*η)
                             + ih3 * sin(6*ξ) * cosh(6*η)
                             + ih2 * sin(4*ξ) * cosh(4*η)
                             + ih1 * sin(2*ξ) * cosh(2*η));

        final double η0 = η - (ih4 * cos(8*ξ) * sinh(8*η)
                             + ih3 * cos(6*ξ) * sinh(6*η)
                             + ih2 * cos(4*ξ) * sinh(4*η)
                             + ih1 * cos(2*ξ) * sinh(2*η));

        final double β = asin(sin(ξ0) / cosh(η0));
        final double Q = asinh(tan(β));
        /*
         * Following usually converges in 4 iterations.
         */
        double Qp = Q, p = 0;
        for (int i=0; i<MAXIMUM_ITERATIONS; i++) {
            final double c = excentricity * atanh(excentricity * tanh(Qp));
            Qp = Q + c;
            if (abs(c - p) <= ITERATION_TOLERANCE) {
                dstPts[dstOff    ] = asin(tanh(η0) / cos(β));
                dstPts[dstOff + 1] = atan(sinh(Qp));
                return;
            }
            p = c;
        }
        throw new ProjectionException(Errors.Keys.NoConvergence);
    }
}
