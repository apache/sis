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
import java.util.Optional;
import java.util.regex.Pattern;
import static java.lang.Math.*;
import static java.lang.Double.*;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.measure.Latitude;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.DomainDefinition;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.provider.LambertConformal1SP;
import org.apache.sis.referencing.operation.provider.LambertConformal2SP;
import org.apache.sis.referencing.operation.provider.LambertConformalWest;
import org.apache.sis.referencing.operation.provider.LambertConformalBelgium;
import org.apache.sis.referencing.operation.provider.LambertConformalMichigan;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.privy.DoubleDouble;
import static org.apache.sis.math.MathFunctions.isPositive;
import static org.apache.sis.referencing.privy.Formulas.fastHypot;


/**
 * <cite>Lambert Conic Conformal</cite> projection (EPSG codes 9801, 9802, 9803, 9826, 1051).
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Lambert_conformal_conic_projection">Lambert Conformal Conic projection on Wikipedia</a></li>
 *   <li><a href="https://mathworld.wolfram.com/LambertConformalConicProjection.html">Lambert Conformal Conic projection on MathWorld</a></li>
 * </ul>
 *
 * <h2>Description</h2>
 * Areas and shapes are deformed as one moves away from standard parallels.
 * The angles are true in a limited area.
 * This projection is used for the charts of North America and some European countries.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  André Gosselin (MPO)
 * @author  Rueben Schulz (UBC)
 * @author  Rémi Maréchal (Geomatys)
 */
public class LambertConicConformal extends ConformalProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2067358524298002016L;

    /**
     * Variants of Lambert Conical Conformal projection. Those variants modify the way the projections are constructed
     * (e.g. in the way parameters are interpreted), but formulas are basically the same after construction.
     * Those variants are not exactly the same as variants 1SP and 2SP used by EPSG, but they are closely related.
     *
     * <p>We do not provide such codes in public API because they duplicate the functionality of
     * {@link OperationMethod} instances. We use them only for constructors convenience.</p>
     */
    private enum Variant implements ProjectionVariant {
        // Declaration order matter. Patterns are matched in that order.

        /** The <q>Lambert Conic Conformal (2SP Belgium)</q> projection. */
        BELGIUM(".*\\bBelgium\\b.*", LambertConformalBelgium.IDENTIFIER, false),

        /** The <q>Lambert Conic Conformal (2SP Michigan)</q> projection. */
        MICHIGAN(".*\\bMichigan\\b.*", LambertConformalMichigan.IDENTIFIER, false),

        /** The <q>Lambert Conic Conformal (West Orientated)</q> projection. */
        WEST(".*\\bWest\\b.*", LambertConformalWest.IDENTIFIER, true),

        /** The <q>Lambert Conic Conformal (1SP)</q> projection. */
        ONE_PARALLEL(".*\\b1SP\\b.*", LambertConformal1SP.IDENTIFIER, true),

        /** The  <q>Lambert Conic Conformal (2SP)</q> projection. */
        TWO_PARALLELS(".*\\b2SP\\b.*", LambertConformal2SP.IDENTIFIER, false);

        /** Name pattern for this variant.    */ private final Pattern operationName;
        /** EPSG identifier for this variant. */ private final String identifier;
        /** Number of standard parallels.     */ final boolean is1SP;
        /** Creates a new enumeration value.  */
        private Variant(final String operationName, final String identifier, final boolean is1SP) {
            this.operationName = Pattern.compile(operationName, Pattern.CASE_INSENSITIVE);
            this.identifier    = identifier;
            this.is1SP         = is1SP;
        }

        /** The expected name pattern of an operation method for this variant. */
        @Override public Pattern getOperationNamePattern() {
            return operationName;
        }

        /** EPSG identifier of an operation method for this variant. */
        @Override public String getIdentifier() {
            return identifier;
        }
    }

    /**
     * Constant for the Belgium 2SP case. Defined as 29.2985 seconds, given here in radians.
     * Use double-double arithmetic not for map projection accuracy, but for consistency with
     * the normalization matrix which use that precision for "degrees to radians" conversion.
     * The goal is to have cleaner results after matrix inversions and multiplications.
     *
     * <h4>Tip</h4>
     * How to verify the value:
     *
     * {@snippet lang="java" :
     *     BigDecimal a = new BigDecimal(BELGE_A.value);
     *     a = a.add     (new BigDecimal(BELGE_A.error));
     *     a = a.multiply(new BigDecimal("57.29577951308232087679815481410517"));
     *     a = a.multiply(new BigDecimal(60 * 60));
     *     System.out.println(a);
     *     }
     */
    static final DoubleDouble BELGE_A = DoubleDouble.of(-1.420431363598774E-4, -1.1777378450498224E-20);

    /**
     * Internal coefficients for computation, depending only on eccentricity and values of standards parallels.
     * This is defined as {@literal n = (ln m₁ – ln m₂) / (ln t₁ – ln t₂)} in §1.3.1.1 of
     * IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
     *
     * <p><b>Note:</b></p>
     * <ul>
     *   <li>If φ₁ = -φ₂, then the cone become a cylinder and this {@code n} value become 0.
     *       This limiting case is the Mercator projection, but we cannot use this class because
     *       {@code n=0} causes indetermination like 0 × ∞ in the equations of this class.</li>
     *   <li>If φ₁ = φ₂ = ±90°, then this {@code n} value become ±1. The formulas in the transform and
     *       inverse transform methods become basically the same as the ones in {@link PolarStereographic},
     *       but (de)normalization matrices contain NaN values.</li>
     *   <li>Depending on how the formulas are written, <var>n</var> may be positive in the South hemisphere and
     *       negative in the North hemisphere (or conversely). However, Apache SIS adjusts the coefficients of the
     *       (de)normalization matrices in order to keep <var>n</var> positive, because the formulas are slightly
     *       more accurate for positive <var>n</var> values. However, this adjustment is optional and can be disabled
     *       in the constructor.</li>
     * </ul>
     */
    final double n;

    /**
     * Creates a Lambert projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Lambert Conic Conformal (1SP)</q>.</li>
     *   <li><q>Lambert Conic Conformal (West Orientated)</q>.</li>
     *   <li><q>Lambert Conic Conformal (2SP)</q>.</li>
     *   <li><q>Lambert Conic Conformal (2SP Belgium)</q>.</li>
     *   <li><q>Lambert Conic Conformal (2SP Michigan)</q>.</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
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
        final Variant variant = variant(method, Variant.values(), Variant.TWO_PARALLELS);
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
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
            case ONE_PARALLEL: {
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
            case TWO_PARALLELS: {
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
        final Variant variant = (Variant) initializer.variant;
        double φ0 = initializer.getAndStore(variant.is1SP
                  ? LambertConformal1SP.LATITUDE_OF_ORIGIN
                  : LambertConformal2SP.LATITUDE_OF_FALSE_ORIGIN);
        /*
         * Standard parallels (SP) are defined only for the 2SP case, but we look for them unconditionally
         * in case the user gave us non-standard parameters. For the 1SP case, or for the 2SP case left to
         * their default values, EPSG says that we shall use the latitude of origin as the SP.
         */
        double φ1 = initializer.getAndStore(LambertConformal2SP.STANDARD_PARALLEL_1, φ0);
        double φ2 = initializer.getAndStore(LambertConformal2SP.STANDARD_PARALLEL_2, φ1);
        if (abs(φ1 + φ2) < Formulas.ANGULAR_TOLERANCE) {
            /*
             * We cannot allow that because if φ1 = -φ2, then n = 0 and the equations
             * in this class break down with indetermination like 0 × ∞.
             * The caller should use the Mercator projection instead for such cases.
             */
            throw new IllegalArgumentException(Resources.format(Resources.Keys.LatitudesAreOpposite_2,
                    new Latitude(φ1), new Latitude(φ2)));
        }
        /*
         * Whether to favorize precision in the North hemisphere (true) or in the South hemisphere (false).
         * The IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015 uses the
         * following formula:
         *
         *     t = tan(π/4 – φ/2) / [(1 – ℯ⋅sinφ)/(1 + ℯ⋅sinφ)] ^ (ℯ/2)
         *
         * while our `expΨ` function is defined like above, but with tan(π/4 + φ/2) instead of tan(π/4 - φ/2).
         * Those two expressions are the reciprocal of each other if we reverse the sign of φ (see `expΨ` for
         * trigonometric identities), but their accuracies are not equivalent: the hemisphere having values
         * closer to zero is favorized. The EPSG formulas favorize the North hemisphere.
         *
         * Since Apache SIS's formula uses the + operator instead of -, we need to reverse the sign of φ
         * values in order to match the EPSG formulas, but we will do that only if the map projection is
         * for the North hemisphere.
         *
         * TEST: whether `isNorth` is true of false does not change the formulas "correctness": it is only
         * a small accuracy improvement. One can safely force this boolean value to `true` or `false` for
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
         * since   rν(sinφ) = 1   and   expΨ(φ) = tan(π/4 + φ/2)   when the eccentricity is zero.
         * However, we need special formulas for φ1 ≈ φ2 in the calculation of n, otherwise we got
         * a 0/0 indetermination.
         */
        final double sinφ1 = sin(φ1);
        final double m1 = initializer.scaleAtφ(sinφ1, cos(φ1));
        final double t1 = expΨ(φ1, eccentricity*sinφ1);
        /*
         * Compute n = (ln m₁ – ln m₂) / (ln t₁ – ln t₂), which we rewrite as ln(m₁/m₂) / ln(t₁/t₂)
         * for reducing the number of calls to the logarithmic function. Note that this equation
         * tends toward 0/0 if φ₁ ≈ φ₂, which force us to do a special check for the SP1 case.
         */
        if (abs(φ1 - φ2) >= ANGULAR_TOLERANCE) {                    // Should be `true` for 2SP case.
            final double sinφ2 = sin(φ2);
            final double m2 = initializer.scaleAtφ(sinφ2, cos(φ2));
            final double t2 = expΨ(φ2, eccentricity*sinφ2);
            n = log(m1/m2) / log(t1/t2);                            // Tend toward 0/0 if φ1 ≈ φ2.
        } else {
            n = -sinφ1;
        }
        /*
         * Compute F = m₁/(n⋅t₁ⁿ) from Geomatics Guidance Note number 7.
         * Following constants will be stored in the denormalization matrix, to be applied
         * after the non-linear formulas implemented by this LambertConicConformal class.
         */
        double F = m1 / (n * pow(t1, n));
        if (!isNorth) {
            F = -F;
        }
        /*
         * Compute the radius of the parallel of latitude of the false origin.
         * This is related to the "ρ₀" term in Snyder. From EPG guide:
         *
         *    r = a⋅F⋅tⁿ     where (in our case) a=1 and t is our `expΨ` function.
         *
         * EPSG uses this term in the computation of  y = FN + rF – r⋅cos(θ).
         */
        Number rF = null;
        if (φ0 != copySign(PI/2, -n)) {    // For reducing the rounding error documented in expΨ(+π/2).
            rF = F * pow(expΨ(φ0, eccentricity*sin(φ0)), n);
        }
        /*
         * At this point, all parameters have been processed. Now store
         * the linear operations in the (de)normalize affine transforms:
         *
         * Normalization:
         *   - Subtract central meridian to longitudes (done by the super-class constructor).
         *   - Convert longitudes and latitudes from degrees to radians (done by the super-class constructor)
         *   - Multiply longitude by `n`.
         *   - In the Belgium case only, subtract BELGE_A to the scaled longitude.
         *
         * Denormalization
         *   - Revert the sign of y (by negating the factor F).
         *   - Scale x and y by F.
         *   - Translate y by ρ0.
         *   - Multiply by the scale factor (done by the super-class constructor).
         *   - Add false easting and false northing (done by the super-class constructor).
         */
        double sλ = n;
        Number sφ = null;
        if (isNorth) {      // Reverse the sign of either longitude or latitude values before map projection.
            sφ = -1;
        } else {
            sλ = -sλ;
        }
        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        normalize  .convertAfter (0, sλ, (variant == Variant.BELGIUM) ? BELGE_A : null);
        normalize  .convertAfter (1, sφ, null);
        denormalize.convertBefore(0,  F, null);
        denormalize.convertBefore(1, -F, rF);
    }

    /**
     * Creates a new projection initialized to the same parameters as the given one.
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
        LambertConicConformal kernel = this;
        if (eccentricity == 0) {
            kernel = new Spherical(this);
        }
        return kernel.completeWithWraparound(parameters);
    }

    /**
     * Returns the domain of input coordinates.
     * The limits defined by this method are arbitrary and may change in any future implementation.
     * Current implementation sets a longitude range of ±180° (i.e. the world) and a latitude range
     * from pole to equator in the hemisphere of the projection.
     */
    @Override
    public Optional<Envelope> getDomain(final DomainDefinition criteria) {
        final double x = getWraparoundLongitude();
        final double y = copySign(PI/2, -n);
        return Optional.of(new Envelope2D(null, -x, Math.min(y, 0), 2*x, Math.abs(y)));
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
        /*
         * NOTE: If some equation terms seem missing, this is because the linear operations applied before
         * the first non-linear one moved to the "normalize" affine transform, and the linear operations
         * applied after the last non-linear one moved to the "denormalize" affine transform.
         */
        final double θ    = srcPts[srcOff  ];           // θ = Δλ⋅n
        final double φ    = srcPts[srcOff+1];           // Sign may be reversed
        final double absφ = abs(φ);
        final double sinθ = sin(θ);
        final double cosθ = cos(θ);
        final double sinφ;
        final double ρ;     // EPSG guide uses "r", but we keep the symbol from Snyder p. 108 for consistency with PolarStereographic.
        if (absφ < PI/2) {
            sinφ = sin(φ);
            ρ = pow(expΨ(φ, eccentricity*sinφ), n);
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
         * NOTE: If some equation terms seem missing (e.g. "y = ρ0 - y"), this is because the linear operations
         * applied before the first non-linear one moved to the inverse of the "denormalize" transform, and the
         * linear operations applied after the last non-linear one moved to the inverse of the "normalize" transform.
         */
        dstPts[dstOff  ] = atan2(x, y);                     // Really (x,y), not (y,x)
        dstPts[dstOff+1] = -φ(pow(fastHypot(x, y), 1/n));   // Equivalent to φ(pow(hypot(x,y), -1/n)) but more accurate for n>0.
    }


    @Override
    protected String toECMAScript(boolean inverse) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        if (!inverse) {
            //constants
            sb.append("\t_eccentricity : ").append(Double.toString(eccentricity)).append(",\n");
            sb.append("\t_ANGULAR_TOLERANCE : ").append(Double.toString(ANGULAR_TOLERANCE)).append(",\n");
            //utils functions
            sb.append(
              "\t_expΨ : function(φ, ℯsinφ) {\n" +
              "\t\treturn Math.tan(Math.PI/4 + 0.5*φ) * Math.pow((1 - ℯsinφ) / (1 + ℯsinφ), 0.5*this._eccentricity);\n" +
              "\t},\n");

            sb.append(
                "\ttransform : function(src){\n" +
                "\t\tconst n = " + Double.toString(n) + ";\n" +
                "\t\tconst θ    = src[0];           // θ = Δλ⋅n\n" +
                "\t\tconst φ    = src[1];           // Sign may be reversed\n" +
                "\t\tconst absφ = Math.abs(φ);\n" +
                "\t\tconst sinθ = Math.sin(θ);\n" +
                "\t\tconst cosθ = Math.cos(θ);\n" +
                "\t\tlet sinφ = 0.0;\n" +
                "\t\tlet ρ = 0.0;     // EPSG guide uses \"r\", but we keep the symbol from Snyder p. 108 for consistency with PolarStereographic.\n" +
                "\t\tif (absφ < Math.PI/2) {\n" +
                "\t\t    sinφ = Math.sin(φ);\n" +
                "\t\t    ρ = Math.pow(this._expΨ(φ, this._eccentricity*sinφ), n);\n" +
                "\t\t} else if (absφ < Math.PI/2 + this._ANGULAR_TOLERANCE) {\n" +
                "\t\t    sinφ = 1;\n" +
                "\t\t    ρ = (φ*n >= 0) ? Number.POSITIVE_INFINITY : 0;\n" +
                "\t\t} else {\n" +
                "\t\t    ρ = sinφ = Number.NaN;\n" +
                "\t\t}\n" +
                "\t\tconst x = ρ * sinθ;\n" +
                "\t\tconst y = ρ * cosθ;\n" +
                "\t\treturn [x,y];\n" +
                "\t}\n");
        } else {

            //utils functions
            sb.append(
              "\t_fastHypot : function(x, y) {\n" +
              "\t\treturn Math.sqrt(x*x + y*y);\n" +
              "\t},\n");
            final double[] exp = getExpansionFirstTerms();
            sb.append(
                "\t_φ : function(rexpΨ) {\n" +
                "\t\tlet φ = (Math.PI/2) - 2*Math.atan(rexpΨ);\n" +
                "\t\tconst sin_2φ = Math.sin(2*φ);\n" +
                "\t\tconst cos_2φ = Math.cos(2*φ);\n" +
                "\t\tφ += sin_2φ * ("+Double.toString(exp[0])+" + cos_2φ * ("+Double.toString(exp[1])+" + cos_2φ * ("+Double.toString(exp[2])+" + cos_2φ * "+Double.toString(exp[3])+")));\n");

            if (!isUseIterations()) {
                sb.append(
                    "\t\treturn φ;\n" +
                    "\t},\n");
            } else {
                sb.append(
                    "\t\tconst hℯ = 0.5 * this._eccentricity;\n" +
                    "\t\tfor (let it=0; it<this._MAXIMUM_ITERATIONS; it++) {\n" +
                    "\t\t\tconst ℯsinφ = this._eccentricity * Math.sin(φ);\n" +
                    "\t\t\tconst Δφ = φ - (φ = Math.PI/2 - 2*Math.atan(rexpΨ * Math.pow((1 - ℯsinφ)/(1 + ℯsinφ), hℯ)));\n" +
                    "\t\t\tif (!(Math.abs(Δφ) > this._ITERATION_TOLERANCE)) { // Use '!' for accepting NaN.\n" +
                    "\t\t\t\treturn φ;\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t\treturn Number.NaN;\n" +
                    "\t},\n");
            }

            sb.append(
                "\ttransform : function(src){\n" +
                "\t\tconst n = " + Double.toString(n) + ";\n" +
                "\t\tconst x = src[0];\n" +
                "\t\tconst y = src[1];\n" +
                "\t\tconst d0 = Math.atan2(x, y); // Really (x,y), not (y,x)\n" +
                "\t\tconst d1 = -this._φ(Math.pow(this._fastHypot(x, y), 1.0/n)); // Equivalent to φ(pow(hypot(x,y), -1/n)) but more accurate for n>0.\n" +
                "\t\treturn [d0,d1];\n" +
                "\t}\n");
        }

        sb.append("}");
        return sb.toString();
    }
    /**
     * Provides the transform equations for the spherical case of the Lambert Conformal projection.
     *
     * <h2>Implementation note</h2>
     * this class contains explicit checks for latitude values at poles.
     * See the discussion in the {@link Mercator.Spherical} javadoc for an explanation.
     * The following is specific to the Lambert Conformal projection.
     *
     * <p>Comparison of observed behavior at poles between the spherical and ellipsoidal cases,
     * if no special checks are applied:</p>
     *
     * <pre class="text">
     *     ┌───────┬──────────────────────────┬────────────────────────┐
     *     │       │ Spherical                │ Ellipsoidal            │
     *     ├───────┼──────────────────────────┼────────────────────────┤
     *     │ North │ Approximate  (y = small) │ Exact answer (y = 0.0) │
     *     │ South │ Exact answer (y = +∞)    │ Approximate  (y = big) │
     *     └───────┴──────────────────────────┴────────────────────────┘</pre>
     *
     * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
     * @author  André Gosselin (MPO)
     * @author  Rueben Schulz (UBC)
     */
    static final class Spherical extends LambertConicConformal {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -8077690516096472987L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param  other  the other projection (usually ellipsoidal) from which to copy the parameters.
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
                                final boolean derivate)
        {
            final double θ    = srcPts[srcOff  ];       // θ = Δλ⋅n
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
        {
            double x = srcPts[srcOff  ];
            double y = srcPts[srcOff+1];
            final double ρ = hypot(x, y);
            dstPts[dstOff  ] = atan2(x, y);                     // Really (x,y), not (y,x);
            dstPts[dstOff+1] = PI/2 - 2*atan(pow(1/ρ, 1/n));
        }
    }
}
