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
import org.apache.sis.referencing.operation.matrix.Matrix2;
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
     * @param  srcPts    source point coordinate, as (<var>longitude</var>, <var>latitude</var>) in radians.
     * @param  srcOff    the offset of the single coordinate to be converted in the source array.
     * @param  dstPts    the array into which the converted coordinate is returned (may be the same than {@code srcPts}).
     * @param  dstOff    the offset of the location of the converted coordinate that is stored in the destination array.
     * @param  derivate  {@code true} for computing the derivative, or {@code false} if not needed.
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinate can not be converted.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws ProjectionException
    {
        final double  λ     = srcPts[srcOff  ];
        final double  φ     = srcPts[srcOff+1];
        final double  cosλ  = cos(λ);
        final double  sinλ  = sin(λ);
        final double  cosφ  = cos(φ);
        final double  sinφ  = sin(φ);
        final double  cosc  = sinφ0*sinφ + cosφ0*cosφ*cosλ;
        final double  c     = acos(cosc);
        final boolean ind   = abs(c) < ANGULAR_TOLERANCE;
        final double  k     = ind ? 1 : c/sin(c);
        final double  cφcλ  = cosφ * cosλ;
        final double  cφsλ  = cosφ * sinλ;
        final double  x     = k * cφsλ;
        final double  y     = k * (cosφ0*sinφ - sinφ0*cφcλ);
        if (dstPts != null) {
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
        }
        if (!derivate) {
            return null;
        }
        /*
         * Formulas below can be verified with Maxima.
         *
         * https://svn.apache.org/repos/asf/sis/analysis/Azimuthal%20Equidistant%20(Spherical).wxmx
         */
        final double t    = ind ? 1./3 : (1/k - cosc) / (1 - cosc*cosc);
        final double sφcλ = sinφ * cosλ;
        final double tλ   = cφsλ * cosφ0*t;
        final double tφ   = (cosφ0*sφcλ - sinφ0*cosφ) * t;
        return new Matrix2(x*tλ + k*cφcλ,                           // ∂x/∂λ
                           x*tφ - k*sinλ*sinφ,                      // ∂x/∂φ
                           y*tλ + x*sinφ0,                          // ∂y/∂λ
                           y*tφ + k*(sinφ0*sφcλ + cosφ0*cosφ));     // ∂y/∂φ
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @param  srcPts  the array containing the source point coordinate, as linear distance on a unit sphere or ellipse.
     * @param  srcOff  the offset of the point to be converted in the source array.
     * @param  dstPts  the array into which the converted point coordinate is returned (may be the same than {@code srcPts}).
     * @param  dstOff  the offset of the location of the converted point that is stored in the destination array.
     * @throws ProjectionException if the point can not be converted.
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
