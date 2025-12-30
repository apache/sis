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
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.util.Workaround;
import static org.apache.sis.referencing.operation.provider.Orthographic.*;


/**
 * <cite>Orthographic</cite> projection (EPSG:9840).
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Orthographic_projection_in_cartography">Orthographic projection on Wikipedia</a></li>
 *   <li><a href="https://mathworld.wolfram.com/OrthographicProjection.html">Orthographic projection on MathWorld</a></li>
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
 */
public class Orthographic extends NormalizedProjection {
    /**
     * For compatibility with different versions during deserialization.
     */
    private static final long serialVersionUID = -6140156868989213344L;

    /**
     * Sine and cosine of latitude of origin.
     */
    private final double sinφ0, cosφ0;

    /**
     * Value of (1 – ℯ²)⋅cosφ₀.
     */
    private final double mℯ2_cosφ0;

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="8", fixed="25")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);
        return new Initializer(method, parameters, roles, null);
    }

    /**
     * Creates an orthographic projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Orthographic</q>.</li>
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
    @Workaround(library="JDK", version="8", fixed="25")
    private Orthographic(final Initializer initializer) {
        super(initializer, null);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        sinφ0 = sin(φ0);
        cosφ0 = cos(φ0);
        mℯ2_cosφ0 = (1 - eccentricitySquared)*cosφ0;
        /*
         * For ellipsoidal formulas, equation given by EPSG guidance note contains:
         *
         *     N = FN + ν⋅[sinφ⋅cosφ₀ – cosφ⋅sinφ₀⋅cos(λ – λ₀)] + ℯ²⋅(ν₀⋅sinφ₀ – ν⋅sinφ)⋅cosφ₀
         *
         * In addition of false northing (FN), another constant term is ℯ²⋅(ν₀⋅sinφ₀)⋅cosφ₀
         * which we factor out below. Note that we do not really need the "if" statement
         * since the computed value below is zero in the spherical case.
         */
        if (eccentricity != 0) {
            final double ν0_cosφ0 = initializer.scaleAtφ(sinφ0, cosφ0);
            final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
            denormalize.convertBefore(1, null, eccentricitySquared * ν0_cosφ0 * sinφ0);
        }
    }

    /**
     * Returns the names of additional internal parameters which need to be taken in account when
     * comparing two {@code Orthographic} projections or formatting them in debug mode.
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
     * comparing two {@code Orthographic} projections or formatting them in debug mode.
     */
    @Override
    final double[] getInternalParameterValues() {
        return new double[] {(cosφ0 < PI/4) ? acos(cosφ0) : asin(sinφ0)};
    }

    /**
     * Projects the specified (λ,φ) coordinates and stores the result in {@code dstPts}.
     * The units of measurement are implementation-specific (see super-class javadoc).
     * The results must be multiplied by the denormalization matrix before to get linear distances.
     *
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinates cannot be converted.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws ProjectionException
    {
        final Matrix2 derivative = derivate ? new Matrix2() : null;
        transform(srcPts, srcOff, dstPts, dstOff, derivative);
        return derivative;
    }

    /**
     * Implementation of {@link #transform(double[], int, double[], int, boolean)}
     * with possibility to recycle an existing matrix instance.
     *
     * <h4>Implementation note</h4>
     * in other map projections, we use a different class for ellipsoidal formulas.
     * But the orthographic projection is a bit different; for this one it is more
     * convenient to use {@code if} statements.
     *
     * @param  derivative  where to store the Jacobian matrix, or {@code null} if none.
     *         If this matrix is an {@link Inverter} instance, we take that as a flag
     *         meaning to not set out-of-range results to NaN.
     * @return {@code cos(φ)}, useful for error tolerance check.
     */
    final double transform(final double[] srcPts, final int srcOff,
                           final double[] dstPts, final int dstOff,
                           final Matrix2 derivative)
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
        double x, y;
        /*
         * c is the distance from the center of orthographic projection.
         * If that distance is greater than 90° (identied by cos(c) < 0)
         * then the point is on the opposite hemisphere and should be clipped.
         */
        double rν;
        if (cosc >= 0 || derivative instanceof Inverter) {
            x  = cosφ * sinλ;                           // Snyder (20-3)
            y  = mℯ2_cosφ0*sinφ - sinφ0*cosφ_cosλ;      // Snyder (20-4) × (1 – ℯ²)
            rν = 1;
            if (eccentricity != 0) {
                /*
                 * EPSG equations without the  ℯ²⋅(ν₀⋅sinφ₀ – ν⋅sinφ)⋅cosφ₀  additional term in y;
                 * the  ℯ²⋅(ν₀⋅sinφ₀)⋅cosφ₀  part is applied by the denormalization matrix and the
                 * remaining was applied by the (1 - ℯ²) multiplication factor above.
                 */
                final double ℯsinφ = eccentricity * sinφ;
                rν = sqrt(1 - ℯsinφ * ℯsinφ);
                x /= rν;
                y /= rν;
            }
        } else {
            x  = Double.NaN;
            y  = Double.NaN;
            rν = Double.NaN;
        }
        if (dstPts != null) {
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
        }
        if (derivative != null) {
            final double ρ = (1 - eccentricitySquared) / (rν*rν*rν);
            derivative.m00 =  cosφ_cosλ / rν;                           // ∂E/∂λ
            derivative.m01 = -sinφ*sinλ * ρ;                            // ∂E/∂φ
            derivative.m10 =  sinφ0 * x;                                // ∂N/∂λ
            derivative.m11 = (cosφ0*cosφ + sinφ0_sinφ*cosλ) * ρ;        // ∂N/∂φ
        }
        return cosφ;
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
        if (eccentricity != 0) {
            /*
             * In the ellipsoidal case, there are no reverse formulas. Instead we take a first estimation of (λ,φ),
             * compute the forward projection (E′,N′) for that estimation, compute the errors compared to specified
             * (E,N) values, convert that (ΔE,ΔN) error into a (Δλ,Δφ) error using the inverse of Jacobian matrix,
             * correct (λ,φ) and continue iteratively until the error is small enough. This algorithm described in
             * EPSG guidance note could be applied to any map projection, not only Orthographic.
             *
             * See https://issues.apache.org/jira/browse/SIS-478
             */
            final Inverter j = new Inverter();                      // Jacobian matrix.
            j.inverseTransform(this, x, y, dstPts, dstOff);
            dstPts[dstOff] = IEEEremainder(dstPts[dstOff], PI);
        }
    }
}
