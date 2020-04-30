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
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.provider.CassiniSoldner.*;


/**
 * <cite>Cassini-Soldner</cite> projection (EPSG code 9806).
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Cassini_projection">Cassini projection on Wikipedia</a></li>
 *   <li><a href="https://mathworld.wolfram.com/CassiniProjection.html">Cassini projection on MathWorld</a></li>
 * </ul>
 *
 * The ellipsoidal form of this projection is suitable only within a few degrees (3° or 4° of longitude)
 * to either side of the central meridian.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class CassiniSoldner extends MeridianArcBased {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3007155786839466950L;

    /**
     * Creates a Cassini-Soldner projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Cassini-Soldner"</cite>.</li>
     * </ul>
     *
     * @param  method      description of the projection parameters.
     * @param  parameters  the parameter values of the projection to create.
     */
    public CassiniSoldner(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters, STANDARD_VARIANT));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    static Initializer initializer(final OperationMethod method, final Parameters parameters, final byte variant) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.SCALE_FACTOR,     SCALE_FACTOR);
        roles.put(ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);
        return new Initializer(method, parameters, roles, variant);
    }

    /**
     * Creates a new Cassini-Soldner projection from the given initializer.
     */
    CassiniSoldner(final Initializer initializer) {
        super(initializer);
        if (initializer.variant == STANDARD_VARIANT) {
            final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
            final MatrixSIS denormalize = getContextualParameters().getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
            denormalize.convertBefore(1, null, -distance(φ0, sin(φ0), cos(φ0)));
        }
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    CassiniSoldner(final CassiniSoldner other) {
        super(other);
    }

    /**
     * Returns the sequence of <cite>normalization</cite> → {@code this} → <cite>denormalization</cite> transforms
     * as a whole. The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>)
     * coordinates in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     * The non-linear part of the returned transform will be {@code this} transform, except if the ellipsoid
     * is spherical. In the later case, {@code this} transform will be replaced by a simplified implementation.
     *
     * @param  factory  the factory to use for creating the transform.
     * @return the map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMapProjection(final MathTransformFactory factory) throws FactoryException {
        CassiniSoldner kernel = this;
        if (eccentricity == 0) {
            kernel = new Spherical(this);
        }
        return context.completeTransform(factory, kernel);
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     *
     * @param  srcPts    the array containing the source point coordinate,
     *                   as (<var>longitude</var>, <var>latitude</var>) angles in <strong>radians</strong>.
     * @param  srcOff    the offset of the single coordinate to be converted in the source array.
     * @param  dstPts    the array into which the converted coordinate is returned (may be the same than {@code srcPts}).
     *                   Coordinates will be expressed in a dimensionless unit, as a linear distance on a unit sphere or ellipse.
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
        /*
         * Formula from IOGP Publication 373-7-2 — Geomatics Guidance Note
         * — Coordinate Conversions and Transformations including Formulas.
         *
         * Following symbols are from EPSG formulas with a=1, λ₀=0 and M₀=0:
         * λ, φ, T, A, C and rν=1/ν.
         *
         * Following symbol are specific to this implementation: Q and S.
         * Those symbols are defined because of terms reused in derivatives.
         */
        final double λ     = srcPts[srcOff  ];
        final double φ     = srcPts[srcOff+1];
        final double cosφ  = cos(φ);
        final double sinφ  = sin(φ);
        final double sinφ2 = sinφ * sinφ;
        final double cosφ2 = cosφ * cosφ;
        final double tanφ  = sinφ / cosφ;
        final double T     = tanφ * tanφ;
        final double A     = λ  * cosφ;
        final double A2    = A  * A;
        final double A3    = A2 * A;
        final double rν2   = 1 - sinφ2*eccentricitySquared;
        final double C     = cosφ2*eccentricitySquared / (1 - eccentricitySquared);
        final double Q     = T - 8*(C + 1);
        final double S     = ((5-T)/6 + C)*A2;
        final double rν    = sqrt(rν2);
        if (dstPts != null) {
            dstPts[dstOff  ] = (A - T*A3/6 + Q*T*(A3*A2) / 120) / rν;
            dstPts[dstOff+1] = distance(φ, sinφ, cosφ) + tanφ*A2*(0.5 + S/4) / rν;
        }
        if (!derivate) {
            return null;
        }
        /*
         * Following formulas have been derived with WxMaxima, then simplified by hand.
         * See: https://svn.apache.org/repos/asf/sis/analysis/README.html
         */
        final double λ2 = λ * λ;
        final double B  = λ * sinφ;                             // Like A but with sin(φ) instead of cos(φ).
        final double B2 = B*B;                                  // = T*A2
        final double D  = cosφ2*eccentricitySquared / rν2;      // Like C but with a sin²φ in denominator.
        final double V  = A2*Q/60 - 1./3;
        final double W  = B2*(Q*A2/24 - 0.5);
        return new Matrix2(
                  (cosφ/rν) * (W + 1),
                        (B) * (λ2*V - W + B2*(λ2 + 8*C*A2)/60 +(D*(B2*V/2 + 1) - 1)/rν),
                (B*cosφ/rν) * (S + 1),
                    (B2/rν) * ((S/4 + 0.5)*(D + 1/sinφ2) - (S+1 + A2*C/2 + λ2/12)) + dM_dφ(sinφ2));
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @param  srcPts  the array containing the source point coordinate, as linear distance on a unit sphere or ellipse.
     * @param  srcOff  the offset of the point to be converted in the source array.
     * @param  dstPts  the array into which the converted point coordinate is returned (may be the same than {@code srcPts}).
     *                 Coordinates will be (<var>longitude</var>, <var>latitude</var>) angles in <strong>radians</strong>.
     * @param  dstOff  the offset of the location of the converted point that is stored in the destination array.
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double x     = srcPts[srcOff  ];
        final double y     = srcPts[srcOff+1];
        final double φ1    = latitude(y);
        final double sinφ1 = sin(φ1);
        final double cosφ1 = cos(φ1);
        final double tanφ1 = sinφ1 / cosφ1;
        final double rν2   =  1 - eccentricitySquared*(sinφ1*sinφ1);    // ν₁⁻²
        final double ρ1_ν1 = (1 - eccentricitySquared) / rν2;           // ρ₁/ν₁
        final double D     = x  * sqrt(rν2);                            // x/ν₁
        final double D2    = D  * D;
        final double D4    = D2 * D2;
        final double T1    = tanφ1 * tanφ1;
        dstPts[dstOff  ]   = (D - T1*(D2*D)/3 + (1 + 3*T1)*T1*(D4*D)/15) / cosφ1;
        dstPts[dstOff+1]   = φ1 - (tanφ1 / ρ1_ν1) * (D2/2 - (1 + 3*T1)*D4 / 24);
    }


    /**
     * Provides the transform equations for the spherical case of the Cassini-Soldner projection.
     * Formulas are available on <a href="https://en.wikipedia.org/wiki/Cassini_projection">Wikipedia</a>.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @author  Rémi Maréchal (Geomatys)
     * @version 1.1
     * @since   1.1
     * @module
     */
    static final class Spherical extends CassiniSoldner {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -8131887916538379858L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param  other  the other projection (usually ellipsoidal) from which to copy the parameters.
         */
        protected Spherical(final CassiniSoldner other) {
            super(other);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final boolean derivate)
        {
            final double λ    = srcPts[srcOff  ];
            final double φ    = srcPts[srcOff+1];
            final double sinλ = sin(λ);
            final double cosλ = cos(λ);
            final double sinφ = sin(φ);
            final double cosφ = cos(φ);
            final double tanφ = sinφ / cosφ;
            if (dstPts != null) {
                dstPts[dstOff  ] = asin (cosφ * sinλ);
                dstPts[dstOff+1] = atan2(tanφ,  cosλ);
            }
            if (!derivate) {
                return null;
            }
            final double cosφ2 = cosφ * cosφ;
            final double sinλ2 = sinλ * sinλ;
            final double dxden = sqrt(1 - sinλ2*cosφ2);
            final double dyden = tanφ*tanφ + cosλ*cosλ;
            return new Matrix2(
                    cosλ*cosφ  / dxden,
                   -sinλ*sinφ  / dxden,
                    sinλ*tanφ  / dyden,
                    cosλ/cosφ2 / dyden);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void inverseTransform(final double[] srcPts, final int srcOff,
                                        final double[] dstPts, final int dstOff)
        {
            final double x = srcPts[srcOff  ];
            final double y = srcPts[srcOff+1];
            dstPts[dstOff  ] = atan2(tan(x),  cos(y));
            dstPts[dstOff+1] = asin (sin(y) * cos(x));
        }
    }
}
