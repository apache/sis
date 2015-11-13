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
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.measure.Latitude;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.internal.referencing.provider.LambertConformal1SP;
import org.apache.sis.internal.referencing.provider.LambertConformal2SP;
import org.apache.sis.internal.referencing.provider.LambertConformalWest;
import org.apache.sis.internal.referencing.provider.LambertConformalBelgium;
import org.apache.sis.internal.referencing.provider.LambertConformalMichigan;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static java.lang.Double.*;
import static org.apache.sis.math.MathFunctions.isPositive;
import static org.apache.sis.internal.util.DoubleDouble.verbatim;


/**
 * <cite>Lambert Conic Conformal</cite> projection (EPSG codes 9801, 9802, 9803, 9826, 1051).
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
public class LambertConicConformal extends ConformalProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2067358524298002016L;

    /**
     * Codes for variants of Lambert Conical Conformal projection. Those variants modify the way the projections are
     * constructed (e.g. in the way parameters are interpreted), but formulas are basically the same after construction.
     * Those variants are not exactly the same than variants 1SP and 2SP used by EPSG, but they are closely related.
     *
     * <p>We do not provide such codes in public API because they duplicate the functionality of
     * {@link OperationMethod} instances. We use them only for constructors convenience.</p>
     *
     * <p><b>CONVENTION:</b> Codes for SP1 case must be odd, and codes for SP2 case must be even.
     *
     * @see #getVariant(OperationMethod)
     */
    private static final byte SP1  = 1,  WEST    = 3,                   // Must be odd
                              SP2  = 2,  BELGIUM = 4,  MICHIGAN = 6;    // Must be even

    /**
     * Returns the type of the projection based on the name and identifier of the given operation method.
     * If this method can not identify the type, then the parameters should be considered as a 2SP case.
     */
    private static byte getVariant(final OperationMethod method) {
        if (identMatch(method, "(?i).*\\bBelgium\\b.*",  LambertConformalBelgium .IDENTIFIER)) return BELGIUM;
        if (identMatch(method, "(?i).*\\bMichigan\\b.*", LambertConformalMichigan.IDENTIFIER)) return MICHIGAN;
        if (identMatch(method, "(?i).*\\bWest\\b.*",     LambertConformalWest    .IDENTIFIER)) return WEST;
        if (identMatch(method, "(?i).*\\b2SP\\b.*",      LambertConformal2SP     .IDENTIFIER)) return SP2;
        if (identMatch(method, "(?i).*\\b1SP\\b.*",      LambertConformal1SP     .IDENTIFIER)) return SP1;
        return 0; // Unidentified case, to be considered as 2SP.
    }

    /**
     * Constant for the Belgium 2SP case. Defined as 29.2985 seconds, given here in radians.
     * Use double-double arithmetic not for map projection accuracy, but for consistency with
     * the normalization matrix which use that precision for "degrees to radians" conversion.
     * The goal is to have cleaner results after matrix inversions and multiplications.
     *
     * <div class="note"><b>Tip:</b> how to verify the value:
     * {@preformat java
     *     BigDecimal a = new BigDecimal(BELGE_A.value);
     *     a = a.add     (new BigDecimal(BELGE_A.error));
     *     a = a.multiply(new BigDecimal("57.29577951308232087679815481410517"));
     *     a = a.multiply(new BigDecimal(60 * 60));
     *     System.out.println(a);
     * }
     * </div>
     */
    static Number belgeA() {
        return new DoubleDouble(-1.420431363598774E-4, -1.1777378450498224E-20);
    }

    /**
     * Internal coefficients for computation, depending only on values of standards parallels.
     * This is defined as {@literal n = (ln m₁ – ln m₂) / (ln t₁ – ln t₂)} in §1.3.1.1 of
     * IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
     *
     * <p><b>Note:</b></p>
     * <ul>
     *   <li>If φ₁ = -φ₂, then the cone become a cylinder and this {@code n} value become 0.
     *       This limiting case is the Mercator projection, but we can not use this class because
     *       {@code n=0} causes indetermination like 0 × ∞ in the equations of this class.</li>
     *   <li>If φ₁ = φ₂ = ±90°, then this {@code n} value become ±1. The formulas in the transform and
     *       inverse transform methods become basically the same than the ones in {@link PolarStereographic},
     *       but (de)normalization matrices contain NaN values.</li>
     *   <li>Depending on how the formulas are written, <var>n</var> may be positive in the South hemisphere and
     *       negative in the North hemisphere (or conversely). However Apache SIS adjusts the coefficients of the
     *       (de)normalization matrices in order to keep <var>n</var> positive, because the formulas are slightly
     *       more accurate for positive <var>n</var> values. However this adjustment is optional and can be disabled
     *       in the constructor.</li>
     * </ul>
     */
    final double n;

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
    public LambertConicConformal(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @SuppressWarnings("fallthrough")
    @Workaround(library="JDK", version="1.7")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final byte variant = getVariant(method);
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles =
                new EnumMap<ParameterRole, ParameterDescriptor<Double>>(ParameterRole.class);
        /*
         * "Scale factor" is not formally a "Lambert Conformal (2SP)" argument, but we accept it
         * anyway for all Lambert projections since it may be used in some Well Known Text (WKT).
         */
        ParameterDescriptor<Double> scaleFactor = LambertConformal1SP.SCALE_FACTOR;
        ParameterRole eastingDirection = ParameterRole.FALSE_EASTING;
        switch (variant) {
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
                roles.put(ParameterRole.CENTRAL_MERIDIAN, LambertConformal1SP.LONGITUDE_OF_ORIGIN);
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
            default: throw new AssertionError(variant);
        }
        roles.put(ParameterRole.SCALE_FACTOR, scaleFactor);
        return new Initializer(method, parameters, roles, variant);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private LambertConicConformal(final Initializer initializer) {
        super(initializer);
        super.computeCoefficients();
        double φ0 = initializer.getAndStore(((initializer.variant & 1) != 0) ?  // Odd 'type' are SP1, even 'type' are SP2.
                LambertConformal1SP.LATITUDE_OF_ORIGIN : LambertConformal2SP.LATITUDE_OF_FALSE_ORIGIN);
        /*
         * Standard parallels (SP) are defined only for the 2SP case, but we look for them unconditionally
         * in case the user gave us non-standard parameters. For the 1SP case, or for the 2SP case left to
         * their default values, EPSG says that we shall use the latitude of origin as the SP.
         */
        double φ1 = initializer.getAndStore(LambertConformal2SP.STANDARD_PARALLEL_1, φ0);
        double φ2 = initializer.getAndStore(LambertConformal2SP.STANDARD_PARALLEL_2, φ1);
        if (abs(φ1 + φ2) < Formulas.ANGULAR_TOLERANCE) {
            /*
             * We can not allow that because if φ1 = -φ2, then n = 0 and the equations
             * in this class break down with indetermination like 0 × ∞.
             * The caller should use the Mercator projection instead for such cases.
             */
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
         * Compute constants. We do not need to use special formulas for the spherical case below,
         * since rν(sinφ) = 1 and expOfNorthing(φ) = tan(π/4 + φ/2) when the eccentricity is zero.
         * However we need special formulas for φ1 ≈ φ2 in the calculation of n, otherwise we got
         * a 0/0 indetermination.
         */
        final double sinφ1 = sin(φ1);
        final double m1 = initializer.scaleAtφ(sinφ1, cos(φ1));
        final double t1 = expOfNorthing(φ1, eccentricity*sinφ1);
        /*
         * Compute n = (ln m₁ – ln m₂) / (ln t₁ – ln t₂), which we rewrite as ln(m₁/m₂) / ln(t₁/t₂)
         * for reducing the amount of calls to the logarithmic function. Note that this equation
         * tends toward 0/0 if φ₁ ≈ φ₂, which force us to do a special check for the SP1 case.
         */
        if (abs(φ1 - φ2) >= ANGULAR_TOLERANCE) {  // Should be 'true' for 2SP case.
            final double sinφ2 = sin(φ2);
            final double m2 = initializer.scaleAtφ(sinφ2, cos(φ2));
            final double t2 = expOfNorthing(φ2, eccentricity*sinφ2);
            n = log(m1/m2) / log(t1/t2);    // Tend toward 0/0 if φ1 ≈ φ2.
        } else {
            n = -sinφ1;
        }
        /*
         * Compute F = m₁/(n⋅t₁ⁿ) from Geomatics Guidance Note number 7.
         * Following constants will be stored in the denormalization matrix, to be applied
         * after the non-linear formulas implemented by this LambertConicConformal class.
         * Opportunistically use double-double arithmetic since the matrix coefficients will
         * be stored in that format anyway. This makes a change in the 2 or 3 last digits.
         */
        final DoubleDouble F = verbatim(n);
        F.multiply(pow(t1, n), 0);
        F.inverseDivide(m1);
        if (!isNorth) {
            F.negate();
        }
        /*
         * Compute the radius of the parallel of latitude of the false origin.
         * This is related to the "ρ₀" term in Snyder. From EPG guide:
         *
         *    r = a⋅F⋅tⁿ     where (in our case) a=1 and t is our 'expOfNorthing' function.
         *
         * EPSG uses this term in the computation of  y = FN + rF – r⋅cos(θ).
         */
        DoubleDouble rF = null;
        if (φ0 != copySign(PI/2, -n)) {    // For reducing the rounding error documented in expOfNorthing(+π/2).
            rF = new DoubleDouble(F);
            rF.multiply(pow(expOfNorthing(φ0, eccentricity*sin(φ0)), n), 0);
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
        DoubleDouble sλ = verbatim(n);
        DoubleDouble sφ = null;
        if (isNorth) {
            // Reverse the sign of either longitude or latitude values before map projection.
            sφ = verbatim(-1);
        } else {
            sλ.negate();
        }
        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        normalize  .convertAfter(0, sλ, (initializer.variant == BELGIUM) ? belgeA() : null);
        normalize  .convertAfter(1, sφ, null);
        denormalize.convertBefore(0, F, null); F.negate();
        denormalize.convertBefore(1, F, rF);
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    LambertConicConformal(final LambertConicConformal other) {
        super(other);
        n = other.n;
    }

    /**
     * Returns the names of additional internal parameters which need to be taken in account when
     * comparing two {@code LambertConicConformal} projections or formatting them in debug mode.
     */
    @Override
    final String[] getInternalParameterNames() {
        return new String[] {"n"};
    }

    /**
     * Returns the values of additional internal parameters which need to be taken in account when
     * comparing two {@code LambertConicConformal} projections or formatting them in debug mode.
     */
    @Override
    final double[] getInternalParameterValues() {
        return new double[] {n};
    }

    /**
     * Returns the sequence of <cite>normalization</cite> → {@code this} → <cite>denormalization</cite> transforms
     * as a whole. The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>)
     * coordinates in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     *
     * <p>The non-linear part of the returned transform will be {@code this} transform, except if the ellipsoid
     * is spherical. In the later case, {@code this} transform will be replaced by a simplified implementation.</p>
     *
     * @param  factory The factory to use for creating the transform.
     * @return The map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMapProjection(final MathTransformFactory factory) throws FactoryException {
        LambertConicConformal kernel = this;
        if (eccentricity == 0) {
            kernel = new Spherical(this);
        }
        return context.completeTransform(factory, kernel);
    }

    /**
     * Converts the specified (θ,φ) coordinate (units in radians) and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
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
        final double θ    = srcPts[srcOff  ];     // θ = λ⋅n  (ignoring longitude of origin)
        final double φ    = srcPts[srcOff+1];     // Sign may be reversed
        final double absφ = abs(φ);
        final double sinθ = sin(θ);
        final double cosθ = cos(θ);
        final double sinφ;
        final double ρ;     // EPSG guide uses "r", but we keep the symbol from Snyder p. 108 for consistency with PolarStereographic.
        if (absφ < PI/2) {
            sinφ = sin(φ);
            ρ = pow(expOfNorthing(φ, eccentricity*sinφ), n);
        } else if (absφ < PI/2 + ANGULAR_TOLERANCE) {
            sinφ = 1;
            ρ = (φ*n >= 0) ? POSITIVE_INFINITY : 0;
        } else {
            ρ = sinφ = NaN;
        }
        final double x = ρ * sinθ;
        final double y = ρ * cosθ;
        if (dstPts != null) {
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
        }
        if (!derivate) {
            return null;
        }
        /*
         * End of map projection. Now compute the derivative.
         */
        final double dρ;
        if (sinφ != 1) {
            dρ = n * dy_dφ(sinφ, cos(φ)) * ρ;
        } else {
            dρ = ρ;
        }
        return new Matrix2(y, dρ*sinθ,      // ∂x/∂λ , ∂x/∂φ
                          -x, dρ*cosθ);     // ∂y/∂λ , ∂y/∂φ
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates and stores the (θ,φ) result in {@code dstPts}.
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
        dstPts[dstOff  ] = atan2(x, y);                 // Really (x,y), not (y,x)
        dstPts[dstOff+1] = -φ(pow(hypot(x, y), 1/n));   // Equivalent to φ(pow(hypot(x,y), -1/n)) but more accurate for n>0.
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
    static final class Spherical extends LambertConicConformal {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -7005092237343502956L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param other The other projection (usually ellipsoidal) from which to copy the parameters.
         */
        protected Spherical(final LambertConicConformal other) {
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
            final double θ    = srcPts[srcOff  ];       // θ = λ⋅n
            final double φ    = srcPts[srcOff+1];       // Sign may be reversed
            final double absφ = abs(φ);
            final double sinθ = sin(θ);
            final double cosθ = cos(θ);
            final double ρ;   // EPSG guide uses "r", but we keep the symbol from Snyder p. 108 for consistency with PolarStereographic.
            if (absφ < PI/2) {
                ρ = pow(tan(PI/4 + 0.5*φ), n);
            } else if (absφ < PI/2 + ANGULAR_TOLERANCE) {
                ρ = (φ*n >= 0) ? POSITIVE_INFINITY : 0;
            } else {
                ρ = NaN;
            }
            final double x = ρ * sinθ;
            final double y = ρ * cosθ;
            if (dstPts != null) {
                dstPts[dstOff  ] = x;
                dstPts[dstOff+1] = y;
            }
            if (!derivate) {
                return null;
            }
            final double dρ;
            if (absφ < PI/2) {
                dρ = n*ρ / cos(φ);
            } else {
                dρ = NaN;
            }
            return new Matrix2(y, dρ*sinθ,    // ∂x/∂λ , ∂x/∂φ
                              -x, dρ*cosθ);   // ∂y/∂λ , ∂y/∂φ
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
            dstPts[dstOff  ] = atan2(x, y);  // Really (x,y), not (y,x);
            dstPts[dstOff+1] = PI/2 - 2*atan(pow(1/ρ, 1/n));
        }
    }
}
