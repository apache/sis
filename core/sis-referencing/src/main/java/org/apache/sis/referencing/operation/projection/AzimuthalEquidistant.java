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
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.provider.ModifiedAzimuthalEquidistant.*;


/**
 * <cite>Azimuthal Equidistant (Spherical)</cite> projection.
 * This projection method has no EPSG code.
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Azimuthal_equidistant_projection">Azimuthal equidistant projection</a></li>
 *   <li><a href="https://mathworld.wolfram.com/AzimuthalEquidistantProjection.html">Azimuthal Equidistant Projection</a></li>
 * </ul>
 *
 * Current implementation supports only the spherical case.
 * For ellipsoidal formulas, the {@link ModifiedAzimuthalEquidistant} provides an approximation
 * valid under 800 kilometres of the projection centre.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see ModifiedAzimuthalEquidistant
 *
 * @since 1.1
 * @module
 */
public class AzimuthalEquidistant extends NormalizedProjection {
    /**
     * For compatibility with different versions during deserialization.
     */
    private static final long serialVersionUID = -6969752149232210847L;

    /**
     * Sine and cosine of the latitude of origin φ₀.
     */
    final double sinφ0, cosφ0;

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.LATITUDE_OF_CONFORMAL_SPHERE_RADIUS, LATITUDE_OF_ORIGIN);
        roles.put(ParameterRole.CENTRAL_MERIDIAN,                    LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.FALSE_EASTING,                       FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,                      FALSE_NORTHING);
        return new Initializer(method, parameters, roles, (byte) 0);
    }

    /**
     * Creates a Azimuthal Equidistant projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Azimuthal Equidistant (Spherical)"</cite>.</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public AzimuthalEquidistant(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer.
     *
     * @param initializer  the initializer for computing map projection internal parameters.
     */
    AzimuthalEquidistant(final Initializer initializer) {
        super(initializer);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        cosφ0 = cos(φ0);
        sinφ0 = sin(φ0);
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
        final double c    = acos(sinφ0*sinφ + cosφ0*cosφ*cosλ);
        final double k    = abs(c) >= ANGULAR_TOLERANCE ? c/sin(c) : 1;
        if (dstPts != null) {
            dstPts[dstOff  ] = k * cosφ*sinλ;
            dstPts[dstOff+1] = k * (cosφ0*sinφ - sinφ0*cosφ*cosλ);
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
        final double x    = srcPts[srcOff  ];
        final double y    = srcPts[srcOff+1];
        final double D    = hypot(x, y);
        final double sinD = sin(D);
        final double cosD = cos(D);
        dstPts[dstOff  ]  = atan2(x*sinD, (cosφ0*cosD*D - sinφ0*sinD*y));
        dstPts[dstOff+1]  = asin(cosD*sinφ0 + sinD*cosφ0*y/D);
    }

    /**
     * Returns the names of additional internal parameters which need to be taken in account when
     * comparing two {@code AzimuthalEquidistant} projections or formatting them in debug mode.
     *
     * <p>We could report any of the internal parameters. But since they are all derived from φ₀ and
     * the {@linkplain #eccentricity eccentricity} and since the eccentricity is already reported by
     * the super-class, we report only φ₀ as a representative of the internal parameters.</p>
     */
    @Override
    final String[] getInternalParameterNames() {
        return new String[] {"φ₀"};
    }

    /**
     * Returns the values of additional internal parameters which need to be taken in account when
     * comparing two {@code ObliqueStereographic} projections or formatting them in debug mode.
     */
    @Override
    final double[] getInternalParameterValues() {
        return new double[] {(cosφ0 < PI/4) ? acos(cosφ0) : asin(sinφ0)};
    }
}
