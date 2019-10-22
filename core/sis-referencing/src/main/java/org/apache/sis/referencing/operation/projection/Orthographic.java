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
import org.apache.sis.internal.util.Numerics;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.provider.Orthographic.*;


/**
 * <cite>Orthographic</cite> projection (EPSG:9840).
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Orthographic_projection_in_cartography">Orthographic projection on Wikipedia</a></li>
 *   <li><a href="http://mathworld.wolfram.com/OrthographicProjection.html">Orthographic projection on MathWorld</a></li>
 * </ul>
 *
 * <h2>Description</h2>
 * This is a perspective azimuthal (planar) projection that is neither conformal nor equal-area.
 * It resembles a globe viewed from a point of perspective at infinite distance.
 * Only one hemisphere can be seen at a time.
 * While not useful for accurate measurements, this projection is useful for pictorial views of the world.
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class Orthographic extends NormalizedProjection {
    /**
     * For compatibility with different versions during deserialization.
     */
    private static final long serialVersionUID = -321305496803338120L;

    /**
     * Sine and cosine of latitude of origin.
     */
    private final double sinφ0, cosφ0;

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
        return new Initializer(method, parameters, roles, Initializer.AUTHALIC_RADIUS);
    }

    /**
     * Creates an orthographic projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Orthographic"</cite>.</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public Orthographic(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private Orthographic(final Initializer initializer) {
        super(initializer);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        sinφ0 = sin(φ0);
        cosφ0 = cos(φ0);
    }

    /**
     * Converts the specified (λ,φ) coordinate and stores the (<var>x</var>,<var>y</var>) result in {@code dstPts}.
     * The units of measurement are implementation-specific (see subclass javadoc).
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
        final double cosφ_cosλ = cosφ*cosλ;
        final double sinφ0_sinφ = sinφ0*sinφ;                   // Note: φ₀ here is φ₁ in Snyder.
        final double cosc = sinφ0_sinφ + cosφ0*cosφ_cosλ;       // Snyder (5-3) page 149
        final double x, y;
        /*
         * c is the distance from the center of orthographic projection.
         * If that distance is greater than 90° (identied by cos(c) < 0)
         * then the point is on the opposite hemisphere and should be clipped.
         */
        if (cosc >= 0) {
            x = cosφ * sinλ;                        // Snyder (20-3)
            y = cosφ0*sinφ - sinφ0*cosφ_cosλ;       // Snyder (20-4)
        } else {
            x = Double.NaN;
            y = Double.NaN;
        }
        if (dstPts != null) {
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
        }
        if (!derivate) {
            return null;
        }
        return new Matrix2(cosφ_cosλ,
                          -sinφ*sinλ,
                           sinφ0 * x,
                           cosφ0 * cosφ + sinφ0_sinφ*cosλ);
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
        /*
         * Note: Synder said that equations become undetermined if ρ=0.
         * But setting the radius R to 1 allows simplifications to emerge.
         * In particular sin(c) = ρ, so terms like sin(c)/ρ disappear.
         */
        final double x    = srcPts[srcOff  ];
        final double y    = srcPts[srcOff+1];
        final double ρ2   = x*x + y*y;                          // sin(c) = ρ/R, but in this method R=1.
        final double cosc = sqrt(1 - ρ2);                       // NaN if ρ > 1.
        dstPts[dstOff  ]  = atan2(x, cosc*cosφ0 - y*sinφ0);     // Synder (20-15) with ρ = sin(c)
        dstPts[dstOff+1]  = asin(cosc*sinφ0 + y*cosφ0);         // Synder (20-14) where y⋅sin(c)/ρ = y/R with R=1.
    }

    /**
     * Compares the given object with this transform for equivalence.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final Orthographic that = (Orthographic) object;
            return Numerics.epsilonEqual(sinφ0, that.sinφ0, mode) &&
                   Numerics.epsilonEqual(cosφ0, that.cosφ0, mode);
        }
        return false;
    }
}
