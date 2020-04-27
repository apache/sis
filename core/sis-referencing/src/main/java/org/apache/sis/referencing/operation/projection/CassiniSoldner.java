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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
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
public class CassiniSoldner extends NormalizedProjection {

    private final double M0;

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
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.SCALE_FACTOR,     SCALE_FACTOR);
        roles.put(ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);
        return new Initializer(method, parameters, roles, (byte) 0);
    }

    /**
     * Creates a new Cassini-Soldner projection from the given initializer.
     */
    CassiniSoldner(final Initializer initializer) {
        super(initializer);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        M0 = M(φ0);
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
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
        final double cosφ = cos(φ);
        final double sinφ = sin(φ);
        final double tanφ = sinφ / cosφ;
        final double A    = λ * cosφ;
        final double A2   = A*A;
        final double A3   = A*A2;
        final double T    = tanφ * tanφ;
        final double C    = eccentricitySquared * (cosφ*cosφ) / (1 - eccentricitySquared);
        final double ν    = 1 / sqrt(1 - eccentricitySquared*(sinφ*sinφ));
        final double y    = M(φ) - M0 + ν*tanφ*(A2/2 + (5 - T + 6*C)*(A2*A2) / 24);
        final double x    = ν*(A - T*A3/6 - (8 - T + 8*C)*T*(A3*A2) / 120);
        dstPts[dstOff  ]  = x;
        dstPts[dstOff+1]  = y;
        return null;
    }

    private double M(final double φ) {
        final double e2 = eccentricitySquared;
        final double e4 = e2*e2;
        final double e6 = e4*e2;
        final double sin2φ = sin(2*φ);
        final double sin4φ = sin(4*φ);
        final double sin6φ = sin(6*φ);
        return (1  - e2/4 - 3*e4/64  -  5*e6/256 ) *     φ
                - (3*e2/8 + 3*e4/32  + 45*e6/1024) * sin2φ
                +         (15*e4/256 + 45*e6/1024) * sin4φ
                -                     (35*e6/3072) * sin6φ;
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double x      = srcPts[srcOff  ];
        final double y      = srcPts[srcOff+1];
        final double e2     = eccentricitySquared;
        final double e4     = e2*e2;
        final double e6     = e4*e2;
        final double e1     = (1 - sqrt(1 - eccentricitySquared)) / (1 + sqrt(1 - eccentricitySquared));
        final double e12    = e1*e1;
        final double e13    = e12*e1;
        final double e14    = e12*e12;
        final double M1     = M0 + y;
        final double μ1     = M1 / (1 - eccentricitySquared/4 - 3*e4/64 - 5*e6/256);
        final double sinμ1  = sin(μ1);
        final double sin2μ1 = sin(2*μ1);
        final double sin4μ1 = sin(4*μ1);
        final double sin6μ1 = sin(6*μ1);
        final double sin8μ1 = sin(8*μ1);
        final double φ1     = μ1 + (3*e1/2 - 27*e13/32) * sin2μ1 + (21*e12/16 - 55*e14/32)*sin4μ1
                                 + (151*e13/96)*sin6μ1 + (1097*e14/512)*sin8μ1;
        final double sinφ1  = sin(φ1);
        final double cosφ1  = cos(φ1);
        final double tanφ1  = sinφ1 / cosφ1;
        final double ν1     = 1/sqrt(1 - eccentricitySquared*(sinφ1*sinφ1));
        final double ρ1     = (1 - eccentricitySquared) / pow(1 - eccentricitySquared*(sinφ1*sinφ1), 1.5);
        final double D      = x/ν1;
        final double D2     = D*D;
        final double D4     = D2*D2;
        final double T1     = tanφ1 * tanφ1;
        final double φ      = φ1 - (ν1*tanφ1 / ρ1) * (D2/2 - (1 + 3*T1)*D4 / 24);
        final double λ      = (D - T1*(D2*D)/3 + (1 + 3*T1)*T1*(D4*D)/15) / cosφ1;
        dstPts[dstOff  ]    = λ;
        dstPts[dstOff+1]    = φ;
    }
}
