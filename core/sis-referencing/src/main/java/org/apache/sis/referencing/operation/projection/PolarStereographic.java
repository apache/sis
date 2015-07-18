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
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.provider.PolarStereographicA;
import org.apache.sis.internal.referencing.provider.PolarStereographicB;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.Latitude;

import static java.lang.Math.*;


/**
 * <cite>Polar Stereographic</cite> projection (EPSG codes 9810, 9829, 9830).
 *
 * @author  Gerald Evenden (USGS)
 * @author  André Gosselin (MPO)
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see EquatorialStereographic
 * @see ObliqueStereographic
 */
public class PolarStereographic extends ConformalProjection {  // Seen as a special case of LambertConformal.
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6635298308431138524L;

    /**
     * Codes for variants.
     *
     * @see #getVariant(ParameterDescriptorGroup)
     */
    private static final byte A = 1, B = 2, C = 3, NORTH = 4, SOUTH = 5;

    /**
     * Returns the (<var>role</var> → <var>parameter</var>) associations for a Polar Stereographic projection.
     *
     * @return The roles map to give to super-class constructor.
     */
    private static Map<ParameterRole, ParameterDescriptor<Double>> roles() {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, PolarStereographicA.LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.SCALE_FACTOR,     PolarStereographicA.SCALE_FACTOR);
        roles.put(ParameterRole.FALSE_EASTING,    PolarStereographicA.FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   PolarStereographicA.FALSE_NORTHING);
        return roles;
    }

    /**
     * Returns the type of the projection based on the name and identifier of the given parameter group.
     * If this method can not identify the type, then the parameters should be considered as a 2SP case.
     */
    private static byte getVariant(final ParameterDescriptorGroup parameters) {
        if (identMatch(parameters, "(?i).*\\bA\\b.*",  PolarStereographicA.IDENTIFIER)) return A;
        if (identMatch(parameters, "(?i).*\\bB\\b.*",  PolarStereographicB.IDENTIFIER)) return B;
//      if (identMatch(parameters, "(?i).*\\bC\\b.*",  PolarStereographicC.IDENTIFIER)) return C;
        if (identMatch(parameters, "(?i).*\\bNorth\\b.*",  null)) return NORTH;
        if (identMatch(parameters, "(?i).*\\bSouth\\b.*",  null)) return SOUTH;
        return 0; // Unidentified case, to be considered as variant A.
    }

    /**
     * Creates a Polar Stereographic projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Polar Stereographic (Variant A)"</cite>.</li>
     *   <li><cite>"Polar Stereographic (Variant B)"</cite>.</li>
     *   <li><cite>"Polar Stereographic (Variant C)"</cite>.</li>
     * </ul>
     *
     * @param method     Description of the projection parameters.
     * @param parameters The parameter values of the projection to create.
     */
    public PolarStereographic(final OperationMethod method, final Parameters parameters) {
        super(method, parameters, roles());
        final byte variant = getVariant(parameters.getDescriptor());
        /*
         * "Standard parallel" and "Latitude of origin" should be mutually exclusive,
         * but this is not a strict requirement for the constructor.
         * Expected parameter:
         *
         *   ┌───────────────────────────────────┬────────────────────┬─────────────┐
         *   │ Projection                        │ Parameter          │ Pole        │
         *   ├───────────────────────────────────┼────────────────────┼─────────────┤
         *   │ Polar Stereographic (variant A)   │ Latitude of origin │ auto detect │
         *   │ Polar Stereographic (variant B)   │ Standard Parallel  │ auto detect │
         *   │ Polar Stereographic (variant C)   │ Standard Parallel  │ auto detect │
         *   │ Stereographic North Pole          │ Standard Parallel  │ North pole  │
         *   │ Stereographic South Pole          │ Standard Parallel  │ South pole  │
         *   └───────────────────────────────────┴────────────────────┴─────────────┘
         */
        double φ0;
        if (variant == A) {
            φ0 = getAndStore(parameters, PolarStereographicA.LATITUDE_OF_ORIGIN);   // Mandatory
        } else {
            φ0 = getAndStore(parameters, PolarStereographicA.LATITUDE_OF_ORIGIN,    // Optional (should not be present)
                    (variant == NORTH) ? Latitude.MAX_VALUE :
                    (variant == SOUTH) ? Latitude.MIN_VALUE : Double.NaN);
        }
        if (abs(abs(φ0) - Latitude.MAX_VALUE) > Formulas.ANGULAR_TOLERANCE) {       // Can be only -90°, +90° or NaN
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalParameterValue_2,
                    PolarStereographicA.LATITUDE_OF_ORIGIN.getName(), φ0));
        }
        double φ1;
        if (variant == B || variant == C || Double.isNaN(φ0)) {
            φ1 = getAndStore(parameters, PolarStereographicB.STANDARD_PARALLEL);        // Mandatory
        } else {
            φ1 = getAndStore(parameters, PolarStereographicB.STANDARD_PARALLEL, φ0);    // Optional
        }
        /*
         * At this point we should ensure that the sign of φ0 is the same than the sign of φ1,
         * since opposite signs produce too large deformations. But we do not verify because
         * it is normally not possible to specify both φ0 and φ1 with the SIS parameter descriptors.
         * It may be possible to specify φ0 and φ1 if the caller used his own parameter descriptor,
         * in which case maybe he really wanted different sign (e.g. for testing purpose).
         */
        φ1 = toRadians(abs(φ1));  // May be anything in [0 … π/2] range.
        final double k0;
        if (abs(φ1 - PI/2) < ANGULAR_TOLERANCE) {
            /*
             * True scale at pole (part of Synder 21-33).
             * In the spherical case, should give k0 == 2.
             */
            k0 = 2 / sqrt(pow(1+excentricity, 1+excentricity) * pow(1-excentricity, 1-excentricity));
        } else {
            /*
             * Derived from Synder 21-32 and 21-33.
             * In the spherical case, should give k0 = 1 + sinφ1   (Synder 21-7 and 21-11).
             */
            final double sinφ1 = sin(φ1);
            k0 = cos(φ1) * expOfNorthing(φ1, sinφ1) / rν(sinφ1);
        }
        /*
         * At this point, all parameters have been processed. Now process to their
         * validation and the initialization of (de)normalize affine transforms.
         */
        final MatrixSIS denormalize = context.getMatrix(false);
        denormalize.convertBefore(0, k0, null);
        denormalize.convertBefore(1, k0, null);
        if (φ0 >= 0) {  // North pole.
            context.getMatrix(true).convertAfter(1, -1, null);
        } else {
            denormalize.convertBefore(1, -1, null);
        }
    }

    /**
     * Converts the specified (θ,φ) coordinate (units in radians) and stores the result in {@code dstPts}.
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
        /*
         * Note: formulas below are very similar to LambertConformal.transform(…) with n = -1.
         */
        final double θ    = srcPts[srcOff  ];   // θ = λ - λ₀
        final double φ    = srcPts[srcOff+1];   // Sign may be reversed
        final double sinθ = sin(θ);
        final double cosθ = cos(θ);
        final double sinφ = sin(φ);
        final double ρ    = expOfNorthing(φ, excentricity*sinφ);
        final double x    = ρ * sinθ;
        final double y    = ρ * cosθ;
        if (dstPts != null) {
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
        }
        if (!derivate) {
            return null;
        }
        //
        // End of map projection. Now compute the derivative.
        //
        final double dρ = ρ * dy_dφ(sinφ, cos(φ));
        return new Matrix2(y, dρ*sinθ,   // ∂x/∂λ , ∂x/∂φ
                          -x, dρ*cosθ);  // ∂y/∂λ , ∂y/∂φ
    }

    /**
     * Transforms the specified (x,y) coordinates and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double x = srcPts[srcOff  ];
        final double y = srcPts[srcOff+1];
        dstPts[dstOff  ] = atan2(x, y);     // Really (x,y), not (y,x)
        dstPts[dstOff+1] = -φ(hypot(x, y));
    }
}
