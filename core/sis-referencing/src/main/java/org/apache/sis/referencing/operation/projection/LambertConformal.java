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

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.measure.Latitude;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.provider.LambertConformal1SP;
import org.apache.sis.internal.referencing.provider.LambertConformal2SP;
import org.apache.sis.internal.referencing.provider.LambertConformalBelgium;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.Debug;

import static java.lang.Math.*;
import static java.lang.Double.*;


/**
 * <cite>Lambert Conical Conformal</cite> projection (EPSG codes 9801, 9802, 9803).
 * See the <a href="http://mathworld.wolfram.com/LambertConformalConicProjection.html">Lambert conformal
 * conic projection on MathWorld</a> for an overview.
 *
 * <div class="section">Description</div>
 * Areas and shapes are deformed as one moves away from standard parallels.
 * The angles are true in a limited area.
 * This projection is used for the charts of North America and some European countries.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  André Gosselin (MPO)
 * @author  Rueben Schulz (UBC)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public class LambertConformal extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2067358524298002016L;

    /**
     * Codes for special kinds of Lambert projection. We do not provide such codes in public API because
     * they duplicate the functionality of {@link OperationMethod} instances. We use them only for convenience.
     *
     * @see #getType(ParameterDescriptorGroup)
     */
    static final byte SP1 = 1, SP2 = 2, BELGIUM = 3;

    /**
     * Constant for the Belgium 2SP case. This is 29.2985 seconds, given here in radians.
     */
    private static final double BELGE_A = 0.00014204313635987700;

    /**
     * Internal coefficients for computation, depending only on values of standards parallels.
     */
    final double n;

    /**
     * Creates a Lambert projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Lambert Conic Conformal (1SP)"</cite>.</li>
     *   <li><cite>"Lambert Conic Conformal (2SP)"</cite>.</li>
     *   <li><cite>"Lambert Conic Conformal (2SP Belgium)"</cite>.</li>
     * </ul>
     *
     * @param method     Description of the projection parameters.
     * @param parameters The parameter values of the projection to create.
     */
    public LambertConformal(final OperationMethod method, final Parameters parameters) {
        this(method, parameters, getType(parameters.getDescriptor()));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private LambertConformal(final OperationMethod method, final Parameters parameters, final byte type) {
        super(method, parameters, null,
                (type == SP1) ? LambertConformal1SP.FALSE_EASTING  : LambertConformal2SP.EASTING_AT_FALSE_ORIGIN,
                (type == SP1) ? LambertConformal1SP.FALSE_NORTHING : LambertConformal2SP.NORTHING_AT_FALSE_ORIGIN);
        double φ0 = getAndStore(parameters,
                (type == SP1) ? LambertConformal1SP.LATITUDE_OF_ORIGIN : LambertConformal2SP.LATITUDE_OF_FALSE_ORIGIN);
        /*
         * Standard parallels (SP) are defined only for the 2SP case, but we look for them unconditionally
         * in case the user gave us non-standard parameters. For the 1SP case, or for the 2SP case left to
         * their default values, EPSG says that we shall use the latitude of origin as the SP.
         */
        double φ1 = getAndStore(parameters, LambertConformal2SP.STANDARD_PARALLEL_1, φ0);
        double φ2 = getAndStore(parameters, LambertConformal2SP.STANDARD_PARALLEL_2, φ1);
        if (abs(φ1 + φ2) < Formulas.ANGULAR_TOLERANCE) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.LatitudesAreOpposite_2,
                    new Latitude(φ1), new Latitude(φ2)));
        }
        φ0 = toRadians(φ0);
        φ1 = toRadians(φ1);
        φ2 = toRadians(φ2);
        /*
         * Computes constants. We do not need to use special formulas for the spherical case below,
         * since rν(sinφ) = 1 and expOfNorthing(φ) = tan(π/4 + φ/2) when the excentricity is zero.
         * However we need special formulas for φ1 ≈ φ2 in the calculation of n, otherwise we got
         * a 0/0 indetermination.
         */
        final double sinφ1 = sin(φ1);
        final double m1    = cos(φ1) / rν(sinφ1);
        final double t1    = expOfNorthing(φ1, excentricity*sinφ1);
        if (abs(φ1 - φ2) >= ANGULAR_TOLERANCE) {  // Should be 'true' for 2SP case.
            final double sinφ2 = sin(φ2);
            final double m2 = cos(φ2) / rν(sinφ2);
            final double t2 = expOfNorthing(φ2, excentricity*sinφ2);
            n = log(m1/m2) / log(t1/t2);    // Tend toward 0/0 if φ1 ≈ φ2.
        } else {
            n = -sinφ1;
        }
        /*
         * Following constants will be stored in the denormalization matrix, to be applied after
         * the non-linear formulas implemented by this LambertConformal class. Opportunistically
         * use double-double arithmetic since we the matrix coefficients will be stored in this
         * format anyway. This makes a change in the 2 or 3 last digits.
         */
        final DoubleDouble F = new DoubleDouble(-pow(t1, -n), 0);
        F.multiply(m1, 0);
        F.divide(n, 0);
        final DoubleDouble ρ0 = new DoubleDouble();         // Initialized to zero.
        if (abs(abs(φ0) - PI/2) >= ANGULAR_TOLERANCE) {
            ρ0.value = pow(expOfNorthing(φ0, excentricity*sin(φ0)), n);
            ρ0.multiply(F);
        }
        /*
         * At this point, all parameters have been processed. Now store
         * the linear operations in the (de)normalize affine transforms:
         *
         * Normalization:
         *   - Subtract central meridian to longitudes.
         *   - Convert longitudes and latitudes from degrees to radians
         *   - Multiply longitude by 'n'.
         *   - In the Belgium case only, subtract BELGE_A to the scaled longitude.
         *
         * Denormalization
         *   - Revert the sign of y (by negating the factor F).
         *   - Scale x and y by F.
         *   - Translate y by ρ0.
         *   - Multiply by the scale factor.
         *   - Add false easting and fasle northing (done by the super-class constructor).
         */
        context.getMatrix(true).concatenate(0, new DoubleDouble(-n, 0),      // Multiplication factor for longitudes.
                (type == BELGIUM) ? new DoubleDouble(-BELGE_A, 0) : null);  // Longitude translation for Belgium.
        context.normalizeGeographicInputs(getAndStore(parameters,
                (type == SP1) ? LambertConformal1SP.CENTRAL_MERIDIAN
                              : LambertConformal2SP.LONGITUDE_OF_FALSE_ORIGIN));

        final double k0 = getAndStore(parameters, LambertConformal1SP.SCALE_FACTOR);
        final MatrixSIS denormalize = context.scaleAndTranslate2D(false, k0, 0, 0);
        denormalize.concatenate(0, F, null);
        F.negate();
        denormalize.concatenate(1, F, ρ0);
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    LambertConformal(final LambertConformal other) {
        super(other);
        n = other.n;
    }

    /**
     * Returns the type of the projection based on the name and identifier of the given parameter group.
     * If this method can not identify the type, then the parameters should be considered as a 2SP case.
     */
    private static byte getType(final ParameterDescriptorGroup parameters) {
        if (identMatch(parameters, "(?i).*\\bBelgium\\b.*", LambertConformalBelgium.IDENTIFIER)) return BELGIUM;
        if (identMatch(parameters, "(?i).*\\b2SP\\b.*",     LambertConformal2SP    .IDENTIFIER)) return SP2;
        if (identMatch(parameters, "(?i).*\\b1SP\\b.*",     LambertConformal1SP    .IDENTIFIER)) return SP1;
        return 0; // Unidentified case, to be considered as 2SP.
    }

    /**
     * Returns a copy of the parameter values for this projection.
     * This method supplies a value only for the following parameters:
     *
     * <ul>
     *   <li>Semi-major axis length of 1</li>
     *   <li>Semi-minor axis length of <code>sqrt(1 - {@linkplain #excentricitySquared ℯ²})</code></li>
     *   <li>Only one of the following:
     *     <ul>
     *       <li>Natural origin (1SP case)</li>
     *     </ul>
     *     or, in the 2SP case:
     *     <ul>
     *       <li>Standard parallel 1</li>
     *       <li>Standard parallel 2</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * No other parameters are set because only the above-cited ones are significant for the non-linear part
     * of this projection.
     *
     * <div class="note"><b>Note:</b>
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.</div>
     *
     * @return A copy of the parameter values for this normalized projection.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        return getParameterValues(new String[] {
            Constants.SEMI_MAJOR,
            Constants.SEMI_MINOR,
            Constants.STANDARD_PARALLEL_1,
            Constants.STANDARD_PARALLEL_2,
            "latitude_of_origin"
        });
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}
     * (linear distance on a unit sphere). In addition, opportunistically computes the projection derivative
     * if {@code derivate} is {@code true}.
     *
     * @return The matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinate can not be converted.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws ProjectionException
    {
        /*
         * NOTE: If some equation terms seem missing, this is because the linear operations applied before
         * the first non-linear one moved to the "normalize" affine transform, and the linear operations
         * applied after the last non-linear one moved to the "denormalize" affine transform.
         */
        final double λ    = srcPts[srcOff];
        final double φ    = srcPts[srcOff + 1];
        final double absφ = abs(φ);
        final double sinλ = sin(λ);
        final double cosλ = cos(λ);
        final double sinφ;
        final double ρ;     // Snyder p. 108
        if (absφ < PI/2) {
            sinφ = sin(φ);
            ρ = pow(expOfNorthing(φ, excentricity*sinφ), n);
        } else if (absφ < PI/2 + ANGULAR_TOLERANCE) {
            sinφ = 1;
            ρ = (φ*n >= 0) ? POSITIVE_INFINITY : 0;
        } else {
            ρ = sinφ = NaN;
        }
        final double x = ρ * sinλ;
        final double y = ρ * cosλ;
        if (dstPts != null) {
            dstPts[dstOff]   = x;
            dstPts[dstOff+1] = y;
        }
        if (!derivate) {
            return null;
        }
        //
        // End of map projection. Now compute the derivative.
        //
        final double dρ;
        if (sinφ != 1) {
            dρ = n * dy_dφ(sinφ, cos(φ)) * ρ;
        } else {
            dρ = ρ;
        }
        return new Matrix2(y, dρ*sinλ,      // ∂x/∂λ , ∂x/∂φ
                          -x, dρ*cosλ);     // ∂y/∂λ , ∂y/∂φ
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double x = srcPts[srcOff  ];
        final double y = srcPts[srcOff+1];
        /*
         * NOTE: If some equation terms seem missing (e.g. "y = ρ0 - y"), this is because the linear operations
         * applied before the first non-linear one moved to the inverse of the "denormalize" transform, and the
         * linear operations applied after the last non-linear one moved to the inverse of the "normalize" transform.
         */
        dstPts[dstOff  ] = atan2(x, y);  // Really (x,y), not (y,x)
        dstPts[dstOff+1] = φ(pow(hypot(x, y), -1/n));
    }


    /**
     * Provides the transform equations for the spherical case of the Lambert Conformal projection.
     *
     * <div class="note"><b>Implementation note:</b>
     * this class contains explicit checks for latitude values at poles.
     * See the discussion in the {@link Mercator.Spherical} javadoc for an explanation.
     * The following is specific to the Lambert Conformal projection.
     *
     * <p>Comparison of observed behavior at poles between the spherical and ellipsoidal cases,
     * if no special checks are applied:</p>
     *
     * {@preformat text
     *     ┌───────┬─────────────────────────────┬───────────────────────────┐
     *     │       │ Spherical                   │ Ellipsoidal               │
     *     ├───────┼─────────────────────────────┼───────────────────────────┤
     *     │ North │ Approximative (y = small)   │ Exact answer  (y = 0.0)   │
     *     │ South │ Exact answer  (y = +∞)      │ Approximative (y = big)   │
     *     └───────┴─────────────────────────────┴───────────────────────────┘
     * }
     * </div>
     *
     * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
     * @author  André Gosselin (MPO)
     * @author  Rueben Schulz (UBC)
     * @since   0.6
     * @version 0.6
     * @module
     */
    static final class Spherical extends LambertConformal {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -7005092237343502956L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param other The other projection (usually ellipsoidal) from which to copy the parameters.
         */
        protected Spherical(final LambertConformal other) {
            super(other);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final boolean derivate) throws ProjectionException
        {
            final double λ    = srcPts[srcOff];
            final double φ    = srcPts[srcOff + 1];
            final double absφ = abs(φ);
            final double sinλ = sin(λ);
            final double cosλ = cos(λ);
            final double ρ;
            if (absφ < PI/2) {
                ρ = pow(tan(PI/4 + 0.5*φ), n);
            } else if (absφ < PI/2 + ANGULAR_TOLERANCE) {
                ρ = (φ*n >= 0) ? POSITIVE_INFINITY : 0;
            } else {
                ρ = NaN;
            }
            final double x = ρ * sinλ;
            final double y = ρ * cosλ;
            Matrix derivative = null;
            if (derivate) {
                final double dρ;
                if (absφ < PI/2) {
                    dρ = n*ρ / cos(φ);
                } else {
                    dρ = NaN;
                }
                derivative = new Matrix2(y, dρ*sinλ,    // ∂x/∂λ , ∂x/∂φ
                                        -x, dρ*cosλ);   // ∂y/∂λ , ∂y/∂φ
            }
            // Following part is common to all spherical projections: verify, store and return.
            assert Assertions.checkDerivative(derivative, super.transform(srcPts, srcOff, dstPts, dstOff, derivate))
                && Assertions.checkTransform(dstPts, dstOff, x, y);     // dstPts = result from ellipsoidal formulas.
            if (dstPts != null) {
                dstPts[dstOff  ] = x;
                dstPts[dstOff+1] = y;
            }
            return derivative;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void inverseTransform(final double[] srcPts, final int srcOff,
                                        final double[] dstPts, final int dstOff)
                throws ProjectionException
        {
            double x = srcPts[srcOff  ];
            double y = srcPts[srcOff+1];
            final double ρ = hypot(x, y);
            x = atan2(x, y);  // Really (x,y), not (y,x)
            y = 2 * atan(pow(1/ρ, -1/n)) - PI/2;
            assert checkInverseTransform(srcPts, srcOff, dstPts, dstOff, x, y);
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
        }

        /**
         * Computes using ellipsoidal formulas and compare with the
         * result from spherical formulas. Used in assertions only.
         */
        private boolean checkInverseTransform(final double[] srcPts, final int srcOff,
                                              final double[] dstPts, final int dstOff,
                                              final double λ, final double φ)
                throws ProjectionException
        {
            super.inverseTransform(srcPts, srcOff, dstPts, dstOff);
            return Assertions.checkInverseTransform(dstPts, dstOff, λ, φ);
        }
    }

    /**
     * Compares the given object with this transform for equivalence.
     *
     * @return {@code true} if the given object is equivalent to this map projection.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            return Numerics.epsilonEqual(n, ((LambertConformal) object).n, mode);
        }
        return false;
    }
}
