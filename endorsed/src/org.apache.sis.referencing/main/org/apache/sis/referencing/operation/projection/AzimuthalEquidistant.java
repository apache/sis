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
import static java.lang.Math.*;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Workaround;
import static org.apache.sis.referencing.operation.provider.ModifiedAzimuthalEquidistant.*;


/**
 * <cite>Azimuthal Equidistant (Spherical)</cite> projection.
 * This projection method has no EPSG code.
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Azimuthal_equidistant_projection">Azimuthal equidistant projection on Wikipedia</a></li>
 *   <li><a href="https://mathworld.wolfram.com/AzimuthalEquidistantProjection.html">Azimuthal Equidistant Projection on MathWorld</a></li>
 * </ul>
 *
 * Current implementation supports only the spherical case.
 * For ellipsoidal formulas, the {@link ModifiedAzimuthalEquidistant} class provides an approximation
 * valid under 800 kilometres of the projection centre.
 *
 * <h2>Note of projection variants</h2>
 * Formulas for this map projection have been published by Snyder (1987) in the following forms:
 * <ul>
 *   <li><cite>Azimuthal Equidistant projection for the sphere.</cite>
 *     This form has no EPSG code. It is implemented in Apache SIS as "Azimuthal Equidistant (Spherical)".</li>
 *   <li><cite>Polar aspect of ellipsoidal Azimuthal Equidistant.</cite>
 *     This form has no EPSG code. It is not yet implemented in Apache SIS.</li>
 *   <li><cite>Oblique and equatorial aspects of ellipsoidal Azimuthal Equidistant:</cite>
 *     <ul>
 *       <li><cite>Nearly rigorous sets of formulas.</cite>
 *         The EPSG name is "Modified Azimuthal Equidistant" (EPSG:9832).
 *         This projection is implemented by {@link ModifiedAzimuthalEquidistant}.</li>
 *       <li><cite>Approximate sets of formulas.</cite>
 *         The EPSG name is "Guam projection" (EPSG:9831).
 *         This projection is not yet implemented in Apache SIS.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * This base class is aimed to provide the general case valid for all distances;
 * the fact that current version uses spherical formulas should be considered as an implementation limitation
 * that may change in future version. Subclasses are specializations for more restricted areas.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Maxime Gavens (Geomatys)
 *
 * @see ModifiedAzimuthalEquidistant
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
    @Workaround(library="JDK", version="8", fixed="25")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.LATITUDE_OF_CONFORMAL_SPHERE_RADIUS, LATITUDE_OF_ORIGIN);
        roles.put(ParameterRole.CENTRAL_MERIDIAN,                    LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.FALSE_EASTING,                       FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,                      FALSE_NORTHING);
        return new Initializer(method, parameters, roles, null);
    }

    /**
     * Creates a Azimuthal Equidistant projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Azimuthal Equidistant (Spherical)</q>.</li>
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
        super(initializer, null);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        cosφ0 = cos(φ0);
        sinφ0 = sin(φ0);
    }

    /**
     * Creates a new projection initialized to the same parameters as the given one.
     */
    AzimuthalEquidistant(final AzimuthalEquidistant other) {
        super(null, other);
        cosφ0 = other.cosφ0;
        sinφ0 = other.sinφ0;
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
     * comparing two {@code AzimuthalEquidistant} projections or formatting them in debug mode.
     */
    @Override
    final double[] getInternalParameterValues() {
        return new double[] {(cosφ0 < PI/4) ? acos(cosφ0) : asin(sinφ0)};
    }

    /**
     * Projects the specified (λ,φ) coordinates (units in radians) and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     * The results must be multiplied by the denormalization matrix before to get linear distances.
     *
     * @param  srcPts    source point coordinates, as (<var>longitude</var>, <var>latitude</var>) in radians.
     * @param  srcOff    the offset of the single coordinate tuple to be converted in the source array.
     * @param  dstPts    the array into which the converted coordinates is returned (may be the same as {@code srcPts}).
     * @param  dstOff    the offset of the location of the converted coordinates that is stored in the destination array.
     * @param  derivate  {@code true} for computing the derivative, or {@code false} if not needed.
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinates cannot be converted.
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
        final double  cosc  = min(1, max(-1, sinφ0*sinφ + cosφ0*cosφ*cosλ));
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
     * @param  srcPts  the array containing the source point coordinates, as linear distance on a unit sphere or ellipse.
     * @param  srcOff  the offset of the point to be converted in the source array.
     * @param  dstPts  the array into which the converted point coordinates is returned (may be the same as {@code srcPts}).
     * @param  dstOff  the offset of the location of the converted point that is stored in the destination array.
     * @throws ProjectionException if the point cannot be converted.
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
        dstPts[dstOff+1]  = asin(D == 0 ? sinφ0 : sinφ0*cosD + cosφ0*sinD*(y/D));
        /*
         * Checking for strict equality (D == 0) is okay because even a very small value
         * is sufficient for avoiding NaN. We get (y/D) ≤ 1 and sin(D) ≈ D, so the right
         * term become close to zero.
         */
    }
}
