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

import java.util.EnumMap;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.provider.ModifiedAzimuthalEquidistant.*;


/**
 * <cite>Modified Azimuthal Equidistant</cite> projection (EPSG:9832).
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Azimuthal_equidistant_projection">Azimuthal equidistant projection</a></li>
 *   <li><a href="https://mathworld.wolfram.com/AzimuthalEquidistantProjection.html">Azimuthal Equidistant Projection</a></li>
 * </ul>
 *
 * <h2>Description</h2>
 * An approximation of the oblique form of the <cite>Azimuthal Equidistant</cite> projection.
 * For relatively short distances (e.g. under 800 kilometres) this modification introduces no significant error.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class ModifiedAzimuthalEquidistant extends NormalizedProjection {
    /**
     * Sine and cosine of the latitude of origin φ₀.
     */
    private final double sinφ0, cosφ0;

    /**
     * The radius of curvature (assuming <var>a</var>=1) at the latitude of origin
     * multiplied by the sine of that latitude.
     */
    private final double ℯ2_ν0_sinφ0;

    /**
     * The ℯ⋅sin(φ₀)/√(1 − ℯ²) term.
     */
    private final double G;

    /**
     * The ℯ⋅cos(φ₀)/√(1 − ℯ²) term. This is the <var>H</var> term in EPSG guidance notes
     * but without the cos(α) term.
     */
    private final double Hp;

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);
        return new Initializer(method, parameters, roles, (byte) 0);
    }

    /**
     * Creates a Modified Azimuthal Equidistant projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Modified Azimuthal Equidistant"</cite>.</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public ModifiedAzimuthalEquidistant(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private ModifiedAzimuthalEquidistant(final Initializer initializer) {
        super(initializer);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        cosφ0 = cos(φ0);
        sinφ0 = sin(φ0);
        final double ν0 = initializer.radiusOfCurvature(sinφ0);
        ℯ2_ν0_sinφ0 = eccentricitySquared * ν0 * sinφ0;
        final double f = eccentricity / sqrt(1 - eccentricitySquared);
        G  = f * sinφ0;
        Hp = f * cosφ0;

        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(0, ν0, null);
        denormalize.convertBefore(1, ν0, null);
    }

    /**
     * Converts the specified (λ,φ) coordinate and stores the (<var>x</var>,<var>y</var>) result in {@code dstPts}.
     *
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinate can not be converted.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws ProjectionException
    {
        final double λ    = srcPts[srcOff  ];
        final double φ    = srcPts[srcOff+1];
        final double cosλ = cos(λ);
        final double sinλ = sin(λ);
        final double cosφ = cos(φ);
        final double sinφ = sin(φ);
        final double rν   = sqrt(1 - eccentricitySquared*(sinφ*sinφ));
        final double Ψ    = atan((1 - eccentricitySquared)*tan(φ) + ℯ2_ν0_sinφ0*rν/cosφ);
        final double α    = atan2(sinλ, cosφ0*tan(Ψ) - sinφ0*cosλ);
        final double sinα = sin(α);
        final double cosα = cos(α);
        final double H    = cosα * Hp;
        double c;
        if (abs(α) < ANGULAR_TOLERANCE) {
            c = cosφ0*sin(Ψ) - sinφ0*cos(Ψ);
            if (abs(α) > PI/2) c = -c;
        } else {
            c = sinλ*cos(Ψ) / sinα;
        }
        c = asin(c);                    // After this line this is the `s` value in EPSG guidance notes.
        final double s2 = c  * c;
        final double s3 = s2 * c;
        final double s4 = s2 * s2;
        final double s5 = s4 * c;
        final double H2 = H*H;
        final double GH = G*H;
        c *= 1 - (s2/6   *  H2*(1 -   H2))
               + (s3/8   *  GH*(1 - 2*H2))
               + (s4/120 * (H2*(4 - 7*H2) - 3*(G*G)*(1 - 7*H2)))
               - (s5/48  * GH);
        if (dstPts != null) {
            dstPts[dstOff  ] = c*sinα;
            dstPts[dstOff+1] = c*cosα;
        }
        if (!derivate) {
            return null;
        }
        throw new ProjectionException("Derivative not yet implemented.");
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        throw new ProjectionException("Inverse transform not yet implemented.");
    }

    /**
     * Compares the given object with this transform for equivalence.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final ModifiedAzimuthalEquidistant that = (ModifiedAzimuthalEquidistant) object;
            return Numerics.epsilonEqual(ℯ2_ν0_sinφ0, that.ℯ2_ν0_sinφ0, mode);
        }
        return false;
    }
}
