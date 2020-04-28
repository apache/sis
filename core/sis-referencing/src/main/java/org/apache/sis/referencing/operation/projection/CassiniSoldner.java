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
        final MatrixSIS denormalize = getContextualParameters().getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(1, null, -distance(φ0, sin(φ0), cos(φ0)));
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
        final double rν   = sqrt(1 - eccentricitySquared*(sinφ*sinφ));
        if (dstPts != null) {
            dstPts[dstOff  ]  = (A - T*A3/6 - (8 - T + 8*C)*T*(A3*A2) / 120) / rν;
            dstPts[dstOff+1]  = distance(φ, sinφ, cosφ) + tanφ*(A2/2 + (5 - T + 6*C)*(A2*A2) / 24) / rν;
        }
        return null;
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
}
