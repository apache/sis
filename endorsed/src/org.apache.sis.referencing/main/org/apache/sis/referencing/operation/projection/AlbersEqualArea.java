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
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.measure.Latitude;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import static org.apache.sis.referencing.operation.provider.AlbersEqualArea.*;


/**
 * <cite>Albers Equal Area</cite> projection (EPSG code 9822).
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Albers_projection">Albers projection on Wikipedia</a></li>
 *   <li><a href="https://mathworld.wolfram.com/AlbersEqual-AreaConicProjection.html">Albers Equal-Area Conic projection on MathWorld</a></li>
 * </ul>
 *
 * <p>The {@code "standard_parallel_2"} parameter is optional and will be given the same value as
 * {@code "standard_parallel_1"} if not set.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 */
public class AlbersEqualArea extends AuthalicConversion {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3024658742514888646L;

    /**
     * Internal coefficients for computation, depending only on eccentricity and values of standards parallels.
     * This is defined as {@literal n = (m₁² – m₂²) / (α₂ – α₁)} in §1.3.13 of IOGP Publication 373-7-2 (April 2015).
     *
     * <p>In Apache SIS implementation, we use modified formulas in which the (1 - ℯ²) factor is omitted in
     * {@link #qm(double)} calculation. Consequently, what we get is a modified value <var>nm</var> which is
     * related to Snyder's <var>n</var> value by {@literal n = nm / (1 - ℯ²)}.  The omitted (1 - ℯ²) factor
     * is either taken in account by the (de)normalization matrix, or cancels with other (1 - ℯ²) factors
     * when we develop the formulas.</p>
     *
     * <p>Note that in the spherical case, <var>nm</var> = Snyder's <var>n</var>.</p>
     */
    final double nm;

    /**
     * Internal coefficients for computation, depending only on values of standards parallels.
     * This is defined as {@literal C = m₁² + (n⋅α₁)} in §1.3.13 of IOGP Publication 373-7-2 –
     * Geomatics Guidance Note number 7, part 2 – April 2015.
     */
    final double C;

    /**
     * Creates an Albers Equal Area projection from the given parameters.
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public AlbersEqualArea(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @SuppressWarnings("fallthrough")
    @Workaround(library="JDK", version="1.7")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.FALSE_EASTING,    EASTING_AT_FALSE_ORIGIN);
        roles.put(ParameterRole.FALSE_NORTHING,   NORTHING_AT_FALSE_ORIGIN);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, LONGITUDE_OF_FALSE_ORIGIN);
        return new Initializer(method, parameters, roles, null);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private AlbersEqualArea(final Initializer initializer) {
        super(initializer, null);
        double φ0 = initializer.getAndStore(LATITUDE_OF_FALSE_ORIGIN);
        double φ1 = initializer.getAndStore(STANDARD_PARALLEL_1, φ0);
        double φ2 = initializer.getAndStore(STANDARD_PARALLEL_2, φ1);
        if (abs(φ1 + φ2) < Formulas.ANGULAR_TOLERANCE) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.LatitudesAreOpposite_2,
                    new Latitude(φ1), new Latitude(φ2)));
        }
        final boolean secant = (abs(φ1 - φ2) >= Formulas.ANGULAR_TOLERANCE);
        φ0 = toRadians(φ0);
        φ1 = toRadians(φ1);
        φ2 = toRadians(φ2);
        final double sinφ0 = sin(φ0);
        final double sinφ1 = sin(φ1);
        final double cosφ1 = cos(φ1);
        final double sinφ2 = sin(φ2);
        final double cosφ2 = cos(φ2);
        final double m1    = initializer.scaleAtφ(sinφ1, cosφ1);        // (cos(φ₁) / √(1 – ℯ²sin²φ₁))²
        final double α1    = qm(sinφ1);                                 // Omitted ×(1-ℯ²) (see below)
        if (secant) {
            final double m2 = initializer.scaleAtφ(sinφ2, cosφ2);       // (cos(φ₂) / √(1 – ℯ²sin²φ₂))²
            final double α2 = qm(sinφ2);                                // Omitted ×(1-ℯ²) (see below)
            nm = (m1*m1 - m2*m2) / (α2 - α1);                           // n = nm / (1-ℯ²)
        } else {
            nm = sinφ1;
        }
        C = m1*m1 + nm*α1;                  // Omitted (1-ℯ²) term in nm cancels with omitted (1-ℯ²) term in α₁.
        /*
         * Compute rn = (1-ℯ²)/nm, which is the reciprocal of the "real" n used in Snyder and EPSG guidance note.
         * We opportunistically use double-double arithmetic since the MatrixSIS operations use them anyway, but
         * we do not really have that accuracy because of the limited precision of `nm`. The intent is rather to
         * increase the chances that term cancellations happen during concatenation of coordinate operations.
         */
        final DoubleDouble rn = DoubleDouble.ONE.subtract(initializer.eccentricitySquared).divide(nm, false);
        /*
         * Compute  ρ₀ = √(C - n⋅q(sinφ₀))/n  with multiplication by `a` omitted because already taken in account
         * by the denormalization matrix. Omitted (1-ℯ²) term in `nm` cancels with omitted (1-ℯ²) term in qm(…).
         * See above note about double-double arithmetic usage.
         */
        final var ρ0 = DoubleDouble.sum(C, -nm*qm(sinφ0)).sqrt().multiply(rn);
        /*
         * At this point, all parameters have been processed. Now process to their
         * validation and the initialization of (de)normalize affine transforms.
         */
        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(0, rn, null);
        denormalize.convertBefore(1, rn.negate(), ρ0);
        normalize  .convertAfter (0, rn.inverse(), null);
    }

    /**
     * Creates a new projection initialized to the same parameters as the given one.
     */
    AlbersEqualArea(final AlbersEqualArea other) {
        super(other);
        nm = other.nm;
        C  = other.C;
    }

    /**
     * Returns the names of additional internal parameters which need to be taken in account when
     * comparing two {@code AlbersEqualArea} projections or formatting them in debug mode.
     */
    @Override
    final String[] getInternalParameterNames() {
        return new String[] {"n", "C"};
    }

    /**
     * Returns the values of additional internal parameters which need to be taken in account when
     * comparing two {@code AlbersEqualArea} projections or formatting them in debug mode.
     */
    @Override
    final double[] getInternalParameterValues() {
        return new double[] {nm / (1 - eccentricitySquared), C};
    }

    /**
     * Returns the sequence of <i>normalization</i> → {@code this} → <i>denormalization</i> transforms as a whole.
     * The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>) coordinates
     * in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     * The non-linear part of the returned transform will be {@code this} transform, except if the ellipsoid
     * is spherical. In the latter case, {@code this} transform is replaced by a simplified implementation.
     *
     * @param  parameters  parameters and the factory to use for creating the transform.
     * @return the map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMapProjection(final MathTransformProvider.Context parameters) throws FactoryException {
        AlbersEqualArea kernel = this;
        if (eccentricity == 0) {
            kernel = new Spherical(this);
        }
        return kernel.completeWithWraparound(parameters);
    }

    /**
     * Projects the specified (θ,φ) coordinates and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
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
        final double θ = srcPts[srcOff  ];      // θ = n⋅λ  reduced to  [−n⋅π … n⋅π]  range.
        final double φ = srcPts[srcOff+1];
        final double cosθ = cos(θ);
        final double sinθ = sin(θ);
        final double sinφ = sin(φ);
        final double ρ = sqrt(C - nm*qm(sinφ));
        if (dstPts != null) {
            dstPts[dstOff  ] = ρ * sinθ;
            dstPts[dstOff+1] = ρ * cosθ;
        }
        if (!derivate) {
            return null;
        }
        /*
         * End of map projection. Now compute the derivative.
         */
        final double ome = 1 - eccentricitySquared;
        final double dρ_dφ = -0.5 * nm*dqm_dφ(sinφ, cos(φ)*ome) / (ome*ρ);
        return new Matrix2(cosθ*ρ, dρ_dφ*sinθ,          // ∂x/∂λ, ∂x/∂φ
                          -sinθ*ρ, dρ_dφ*cosθ);         // ∂y/∂λ, ∂y/∂φ
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates and stores the (θ,φ) result in {@code dstPts}.
     *
     * @throws ProjectionException if the point cannot be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double x = srcPts[srcOff  ];
        final double y = srcPts[srcOff+1];
        /*
         * Note: Snyder suggests to reverse the sign of x, y and ρ₀ if n is negative. It should not be done
         * in SIS implementation because (x,y) are premultiplied by n (by the normalization affine transform)
         * before to enter in this method, so if n was negative those values have already their sign reverted.
         */
        dstPts[dstOff  ] = atan2(x, y);
        dstPts[dstOff+1] = φ((C - (x*x + y*y)) / (nm*qmPolar));
        /*
         * Note: Snyder 14-19 gives  q = (C - ρ²n²/a²)/n  where  ρ = √(x² + (ρ₀ - y)²).
         * But in Apache SIS implementation, ρ₀ has already been subtracted by the matrix before we reach this point.
         * So we can simplify by ρ² = x² + y². Furthermore, the matrix also divided x and y by a (the semi-major axis
         * length) before this method, and multiplied by n. so what we have is actually (ρ⋅n/a)² = x² + y².
         * So the formula become:
         *
         *      q  =  (C - (x² + y²)) / n
         *
         * We divide by nm instead of n, so a (1-ℯ²) term is missing. But that missing term will be cancelled with
         * the missing (1-ℯ²) term in `qmPolar`. The division by `qmPolar` is for converting y to sin(β).
         */
    }


    /**
     * Provides the transform equations for the spherical case of the Albers Equal Area projection.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @author  Rémi Maréchal (Geomatys)
     */
    static final class Spherical extends AlbersEqualArea {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -7238296545347764989L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param other the other projection (usually ellipsoidal) from which to copy the parameters.
         */
        protected Spherical(final AlbersEqualArea other) {
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
            final double θ = srcPts[srcOff  ];              // θ = n⋅λ  reduced to  [−n⋅π … n⋅π]  range.
            final double φ = srcPts[srcOff+1];
            final double cosθ = cos(θ);
            final double sinθ = sin(θ);
            final double sinφ = sin(φ);
            final double ρ = sqrt(C - 2*nm*sinφ);           // Snyder 14-3 with radius and division by n omitted.
            if (dstPts != null) {
                dstPts[dstOff  ] = ρ * sinθ;                // Snyder 14-1
                dstPts[dstOff+1] = ρ * cosθ;                // Snyder 14-2
            }
            if (!derivate) {
                return null;
            }
            final double dρ_dφ = -nm*cos(φ) / ρ;
            return new Matrix2(cosθ*ρ, dρ_dφ*sinθ,          // ∂x/∂λ, ∂x/∂φ
                              -sinθ*ρ, dρ_dφ*cosθ);         // ∂y/∂λ, ∂y/∂φ
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void inverseTransform(final double[] srcPts, final int srcOff,
                                        final double[] dstPts, final int dstOff)
        {
            final double x = srcPts[srcOff];
            final double y = srcPts[srcOff + 1];
            dstPts[dstOff  ] = atan2(x, y);                         // Part of Snyder 14-11
            dstPts[dstOff+1] = asin((C - (x*x + y*y)) / (nm*2));    // Snyder 14-8 modified
        }
    }
}
