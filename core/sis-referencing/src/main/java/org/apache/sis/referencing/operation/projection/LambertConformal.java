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

import java.util.Map;
import java.util.EnumMap;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.measure.Latitude;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.provider.LambertConformal1SP;
import org.apache.sis.internal.referencing.provider.LambertConformal2SP;
import org.apache.sis.internal.referencing.provider.LambertConformalWest;
import org.apache.sis.internal.referencing.provider.LambertConformalBelgium;
import org.apache.sis.internal.referencing.provider.LambertConformalMichigan;
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
import static org.apache.sis.math.MathFunctions.isPositive;


/**
 * <cite>Lambert Conical Conformal</cite> projection (EPSG codes 9801, 9802, 9803, 9826, 1051).
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
public class LambertConformal extends AbstractLambertConformal {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2067358524298002016L;

    /**
     * Codes for special kinds of Lambert projection. We do not provide such codes in public API because
     * they duplicate the functionality of {@link OperationMethod} instances. We use them only for convenience.
     *
     * <p>Codes for SP1 case must be odd, and codes for SP2 case must be even.</p>
     *
     * @see #getType(ParameterDescriptorGroup)
     */
    private static final byte SP1  = 1,   SP2      = 2,
                              WEST = 3,   BELGIUM  = 4,
                                          MICHIGAN = 6;

    /**
     * Constant for the Belgium 2SP case. This is 29.2985 seconds, given here in radians.
     */
    private static final double BELGE_A = 0.00014204313635987700;

    /**
     * Internal coefficients for computation, depending only on values of standards parallels.
     * This is defined as {@literal n = (ln m₁ – ln m₂) / (ln t₁ – ln t₂)} in §1.3.1.1 of
     * IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
     */
    final double n;

    /**
     * Returns the (<var>role</var> → <var>parameter</var>) associations for a Lambert projection of the given type.
     *
     * @param  type One of {@link #SP1}, {@link #SP2}, {@link #WEST}, {@link #BELGIUM} and {@link #MICHIGAN} constants.
     * @return The roles map to give to super-class constructor.
     */
    @SuppressWarnings("fallthrough")
    private static Map<ParameterRole, ParameterDescriptor<Double>> roles(final byte type) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        /*
         * "Scale factor" is not formally a "Lambert Conformal (2SP)" argument, but we accept it
         * anyway for all Lambert projections since it may be used in some Well Known Text (WKT).
         */
        ParameterDescriptor<Double> scaleFactor = LambertConformal1SP.SCALE_FACTOR;
        ParameterRole eastingDirection = ParameterRole.FALSE_EASTING;
        switch (type) {
            case WEST: {
                /*
                 * For "Lambert Conic Conformal (West Orientated)" projection, the "false easting" parameter is
                 * effectively a "false westing" (Geomatics Guidance Note number 7, part 2 – April 2015, §1.3.1.3)
                 */
                eastingDirection = ParameterRole.FALSE_WESTING;
                // Fallthrough
            }
            case SP1: {
                roles.put(eastingDirection,               LambertConformal1SP.FALSE_EASTING);
                roles.put(ParameterRole.FALSE_NORTHING,   LambertConformal1SP.FALSE_NORTHING);
                roles.put(ParameterRole.CENTRAL_MERIDIAN, LambertConformal1SP.CENTRAL_MERIDIAN);
                break;
            }
            case MICHIGAN: {
                scaleFactor = LambertConformalMichigan.SCALE_FACTOR;    // Ellipsoid scaling factor (EPSG:1038)
                // Fallthrough
            }
            case BELGIUM:
            case SP2: {
                roles.put(eastingDirection,               LambertConformal2SP.EASTING_AT_FALSE_ORIGIN);
                roles.put(ParameterRole.FALSE_NORTHING,   LambertConformal2SP.NORTHING_AT_FALSE_ORIGIN);
                roles.put(ParameterRole.CENTRAL_MERIDIAN, LambertConformal2SP.LONGITUDE_OF_FALSE_ORIGIN);
                break;
            }
            default: throw new AssertionError(type);
        }
        roles.put(ParameterRole.SCALE_FACTOR, scaleFactor);
        return roles;
    }

    /**
     * Creates a Lambert projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Lambert Conic Conformal (1SP)"</cite>.</li>
     *   <li><cite>"Lambert Conic Conformal (West Orientated)"</cite>.</li>
     *   <li><cite>"Lambert Conic Conformal (2SP)"</cite>.</li>
     *   <li><cite>"Lambert Conic Conformal (2SP Belgium)"</cite>.</li>
     *   <li><cite>"Lambert Conic Conformal (2SP Michigan)"</cite>.</li>
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
        super(method, parameters, roles(type));
        double φ0 = getAndStore(parameters, ((type & 1) != 0) ?  // Odd 'type' are SP1, even 'type' are SP2.
                LambertConformal1SP.LATITUDE_OF_ORIGIN : LambertConformal2SP.LATITUDE_OF_FALSE_ORIGIN);
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
        /*
         * Whether to favorize precision in the North hemisphere (true) or in the South hemisphere (false).
         * The IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015 uses the
         * following formula:
         *
         *     t = tan(π/4 – φ/2) / [(1 – ℯ⋅sinφ)/(1 + ℯ⋅sinφ)] ^ (ℯ/2)
         *
         * while our 'expOfNorthing' function is defined like above, but with   tan(π/4 + φ/2)   instead of
         * tan(π/4 - φ/2). Those two expressions are the reciprocal of each other if we reverse the sign of
         * φ (see 'expOfNorthing' for trigonometric identities), but their accuracies are not equivalent:
         * the hemisphere having values closer to zero is favorized. The EPSG formulas favorize the North
         * hemisphere.
         *
         * Since Apache SIS's formula uses the + operator instead of -, we need to reverse the sign of φ
         * values in order to match the EPSG formulas, but we will do that only if the map projection is
         * for the North hemisphere.
         *
         * TEST: whether 'isNorth' is true of false does not change the formulas "correctness": it is only
         * a small accuracy improvement. One can safely force this boolean value to 'true' or 'false' for
         * testing purpose.
         */
        final boolean isNorth = isPositive(φ0);
        if (isNorth) {
            φ0 = -φ0;
            φ1 = -φ1;
            φ2 = -φ2;
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
        /*
         * Computes n = (ln m₁ – ln m₂) / (ln t₁ – ln t₂), which we rewrite as ln(m₁/m₂) / ln(t₁/t₂)
         * since division is less at risk of precision lost than subtraction. Note that this equation
         * tends toward 0/0 if φ₁ ≈ φ₂, which force us to do a special check for the SP1 case.
         */
        if (abs(φ1 - φ2) >= ANGULAR_TOLERANCE) {  // Should be 'true' for 2SP case.
            final double sinφ2 = sin(φ2);
            final double m2 = cos(φ2) / rν(sinφ2);
            final double t2 = expOfNorthing(φ2, excentricity*sinφ2);
            n = log(m1/m2) / log(t1/t2);    // Tend toward 0/0 if φ1 ≈ φ2.
        } else {
            n = -sinφ1;
        }
        /*
         * Computes F = m₁/(n⋅t₁ⁿ) from Geomatics Guidance Note number 7.
         * Following constants will be stored in the denormalization matrix, to be applied after
         * the non-linear formulas implemented by this LambertConformal class. Opportunistically
         * use double-double arithmetic since we the matrix coefficients will be stored in this
         * format anyway. This makes a change in the 2 or 3 last digits.
         */
        final DoubleDouble F = new DoubleDouble(n, 0);
        F.multiply(pow(t1, n), 0);
        F.inverseDivide(m1, 0);
        if (!isNorth) {
            F.negate();
        }
        /*
         * Compute  r = a⋅F⋅tⁿ  from EPSG notes where (in our case) a=1 and t is our 'expOfNorthing' function.
         * Note that Snyder calls this term "ρ0".
         */
        final DoubleDouble r0 = new DoubleDouble();    // Initialized to zero.
        if (φ0 != copySign(PI/2, -n)) {    // For avoiding the rounding error documented in expOfNorthing(+π/2).
            r0.value = pow(expOfNorthing(φ0, excentricity*sin(φ0)), n);
            r0.multiply(F);
        }
        /*
         * At this point, all parameters have been processed. Now store
         * the linear operations in the (de)normalize affine transforms:
         *
         * Normalization:
         *   - Subtract central meridian to longitudes (done by the super-class constructor).
         *   - Convert longitudes and latitudes from degrees to radians (done by the super-class constructor)
         *   - Multiply longitude by 'n'.
         *   - In the Belgium case only, subtract BELGE_A to the scaled longitude.
         *
         * Denormalization
         *   - Revert the sign of y (by negating the factor F).
         *   - Scale x and y by F.
         *   - Translate y by ρ0.
         *   - Multiply by the scale factor (done by the super-class constructor).
         *   - Add false easting and false northing (done by the super-class constructor).
         */
        final MatrixSIS normalize = context.getMatrix(true);
        normalize.convertAfter(0, new DoubleDouble(isNorth ? n : -n, 0),    // Multiplication factor for longitudes.
                (type == BELGIUM) ? new DoubleDouble(-BELGE_A, 0) : null);  // Longitude translation for Belgium.
        if (isNorth) {
            normalize.convertAfter(1, new DoubleDouble(-1, 0), null);
        }
        final MatrixSIS denormalize = context.getMatrix(false);
        denormalize.convertBefore(0, F, null);
        F.negate();
        denormalize.convertBefore(1, F, r0);
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
        if (identMatch(parameters, "(?i).*\\bBelgium\\b.*",  LambertConformalBelgium .IDENTIFIER)) return BELGIUM;
        if (identMatch(parameters, "(?i).*\\bMichigan\\b.*", LambertConformalMichigan.IDENTIFIER)) return MICHIGAN;
        if (identMatch(parameters, "(?i).*\\bWest\\b.*",     LambertConformalWest    .IDENTIFIER)) return WEST;
        if (identMatch(parameters, "(?i).*\\b2SP\\b.*",      LambertConformal2SP     .IDENTIFIER)) return SP2;
        if (identMatch(parameters, "(?i).*\\b1SP\\b.*",      LambertConformal1SP     .IDENTIFIER)) return SP1;
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
     * Converts the specified (θ,φ) coordinate (units in radians) and stores the result in {@code dstPts}
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
        final double θ    = srcPts[srcOff];         // θ = λ⋅n
        final double φ    = srcPts[srcOff + 1];     // Sign may be reversed
        final double absφ = abs(φ);
        final double sinθ = sin(θ);
        final double cosθ = cos(θ);
        final double sinφ;
        final double r;     // From EPSG guide. Note that Snyder p. 108 calls this term "ρ".
        if (absφ < PI/2) {
            sinφ = sin(φ);
            r = pow(expOfNorthing(φ, excentricity*sinφ), n);
        } else if (absφ < PI/2 + ANGULAR_TOLERANCE) {
            sinφ = 1;
            r = (φ*n >= 0) ? POSITIVE_INFINITY : 0;
        } else {
            r = sinφ = NaN;
        }
        final double x = r * sinθ;
        final double y = r * cosθ;
        if (dstPts != null) {
            dstPts[dstOff    ] = x;
            dstPts[dstOff + 1] = y;
        }
        if (!derivate) {
            return null;
        }
        //
        // End of map projection. Now compute the derivative.
        //
        final double dρ;
        if (sinφ != 1) {
            dρ = n * dy_dφ(sinφ, cos(φ)) * r;
        } else {
            dρ = r;
        }
        return new Matrix2(y, dρ*sinθ,      // ∂x/∂λ , ∂x/∂φ
                          -x, dρ*cosθ);     // ∂y/∂λ , ∂y/∂φ
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
        final double x = srcPts[srcOff    ];
        final double y = srcPts[srcOff + 1];
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
            final double θ    = srcPts[srcOff];         // θ = λ⋅n
            final double φ    = srcPts[srcOff + 1];     // Sign may be reversed
            final double absφ = abs(φ);
            final double sinθ = sin(θ);
            final double cosθ = cos(θ);
            final double r;   // Snyder p. 108 calls this term "ρ", but we use "r" for consistency with EPSG guide.
            if (absφ < PI/2) {
                r = pow(tan(PI/4 + 0.5*φ), n);
            } else if (absφ < PI/2 + ANGULAR_TOLERANCE) {
                r = (φ*n >= 0) ? POSITIVE_INFINITY : 0;
            } else {
                r = NaN;
            }
            final double x = r * sinθ;
            final double y = r * cosθ;
            Matrix derivative = null;
            if (derivate) {
                final double dρ;
                if (absφ < PI/2) {
                    dρ = n*r / cos(φ);
                } else {
                    dρ = NaN;
                }
                derivative = new Matrix2(y, dρ*sinθ,    // ∂x/∂λ , ∂x/∂φ
                                        -x, dρ*cosθ);   // ∂y/∂λ , ∂y/∂φ
            }
            // Following part is common to all spherical projections: verify, store and return.
            assert Assertions.checkDerivative(derivative, super.transform(srcPts, srcOff, dstPts, dstOff, derivate))
                && Assertions.checkTransform(dstPts, dstOff, x, y);     // dstPts = result from ellipsoidal formulas.
            if (dstPts != null) {
                dstPts[dstOff    ] = x;
                dstPts[dstOff + 1] = y;
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
            y = 2*atan(pow(1/ρ, -1/n)) - PI/2;
            assert checkInverseTransform(srcPts, srcOff, dstPts, dstOff, x, y);
            dstPts[dstOff    ] = x;
            dstPts[dstOff + 1] = y;
        }

        /**
         * Computes using ellipsoidal formulas and compare with the
         * result from spherical formulas. Used in assertions only.
         */
        private boolean checkInverseTransform(final double[] srcPts, final int srcOff,
                                              final double[] dstPts, final int dstOff,
                                              final double θ, final double φ)
                throws ProjectionException
        {
            super.inverseTransform(srcPts, srcOff, dstPts, dstOff);
            return Assertions.checkInverseTransform(dstPts, dstOff, θ, φ);
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
