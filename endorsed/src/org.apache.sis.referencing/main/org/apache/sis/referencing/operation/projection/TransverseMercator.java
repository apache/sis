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
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.DomainDefinition;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.provider.TransverseMercatorSouth;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.Debug;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.parameter.Parameters;
import static org.apache.sis.math.MathFunctions.asinh;
import static org.apache.sis.math.MathFunctions.atanh;
import static org.apache.sis.referencing.operation.provider.TransverseMercator.*;


/**
 * <cite>Transverse Mercator</cite> projection (EPSG codes 9807).
 * This class implements the "JHS formulas" reproduced in
 * IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
 *
 * <h2>Description</h2>
 * This is a cylindrical projection, in which the cylinder has been rotated 90°.
 * Instead of being tangent to the equator (or to another standard latitude), it is tangent to a central meridian.
 * Deformation are more important as we are going further from the central meridian.
 * The Transverse Mercator projection is appropriate for region which have a greater extent north-south than east-west.
 *
 * <p>There are a number of versions of the Transverse Mercator projection including the Universal (UTM)
 * and Modified (MTM) Transverses Mercator projections. In these cases the earth is divided into zones.
 * For the UTM the zones are 6 degrees wide, numbered from 1 to 60 proceeding east from 180 degrees longitude,
 * and between latitude 84 degrees North and 80 degrees South. The central meridian is taken as the center of the zone
 * and the latitude of origin is the equator. A scale factor of 0.9996 and false easting of 500000 metres is used for
 * all zones and a false northing of 10000000 metres is used for zones in the southern hemisphere.
 *
 * <h2>Domain of validity</h2>
 * The difference between longitude values λ and the central meridian λ₀ should be less than 60°.
 * Differences larger than 90° of longitude cause a {@link ProjectionException} to be thrown.
 * Differences between 60° and 90° are not rejected by Apache SIS but should be avoided.
 * See the {@linkplain #transform(double[], int, double[], int, boolean) projection method}
 * for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 *
 * @see Mercator
 * @see ObliqueMercator
 */
public class TransverseMercator extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -627685138188387835L;

    /**
     * {@code false} for using the original formulas as published by EPSG, or {@code true} for using formulas
     * modified using trigonometric identities. The use of trigonometric identities is for reducing the amount
     * of calls to the {@link Math#sin(double)} and similar methods. Some identities used are:
     *
     * <ul>
     *   <li>sin(2θ) = 2⋅sinθ⋅cosθ</li>
     *   <li>cos(2θ) = cos²θ - sin²θ</li>
     *   <li>sin(3θ) = (3 - 4⋅sin²θ)⋅sinθ</li>
     *   <li>cos(3θ) = (4⋅cos³θ) - 3⋅cosθ</li>
     *   <li>sin(4θ) = (4 - 8⋅sin²θ)⋅sinθ⋅cosθ</li>
     *   <li>cos(4θ) = (8⋅cos⁴θ) - (8⋅cos²θ) + 1</li>
     * </ul>
     *
     * Hyperbolic formulas:
     *
     * <ul>
     *   <li>sinh(2θ) = 2⋅sinhθ⋅coshθ</li>
     *   <li>cosh(2θ) = cosh²θ + sinh²θ   =   2⋅cosh²θ - 1   =   1 + 2⋅sinh²θ</li>
     *   <li>sinh(3θ) = (3 + 4⋅sinh²θ)⋅sinhθ</li>
     *   <li>cosh(3θ) = ((4⋅cosh²θ) - 3)⋅coshθ</li>
     *   <li>sinh(4θ) = (1 + 2⋅sinh²θ)⋅4.sinhθ⋅coshθ
     *                = 4.cosh(2θ).sinhθ⋅coshθ</li>
     *   <li>cosh(4θ) = (8⋅cosh⁴θ) - (8⋅cosh²θ) + 1
     *                = 8⋅cosh²(θ) ⋅ (cosh²θ - 1) + 1
     *                = 8⋅cosh²(θ) ⋅ sinh²(θ) + 1
     *                = 2⋅sinh²(2θ) + 1</li>
     * </ul>
     *
     * Note that since this boolean is static final, the compiler should exclude the code in the branch that is never
     * executed (no need to comment-out that code).
     *
     * @see #identityEquals(double, double)
     */
    @Debug
    private static final boolean ALLOW_TRIGONOMETRIC_IDENTITIES = true;

    /**
     * Variants of the map projection. Currently this is informative only
     * (the south variant is handled with {@link ParameterRole} instead).
     */
    private enum Variant implements ProjectionVariant {
        /** The "South orientated" variant of Transverse Mercator projection. */
        SOUTH_ORIENTATED(".*\\bSouth\\b.*", TransverseMercatorSouth.IDENTIFIER);

        /** Name pattern for this variant.    */ private final Pattern operationName;
        /** EPSG identifier for this variant. */ private final String  identifier;
        private Variant(final String operationName, final String identifier) {
            this.operationName = Pattern.compile(operationName, Pattern.CASE_INSENSITIVE);
            this.identifier    = identifier;
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
     * Verifies if a trigonometric identity produced the expected value. This method is used in assertions only,
     * for values close to the [-1 … +1] range. The tolerance threshold is approximately 1.5E-12 (note that it
     * still about 7000 time greater than {@code Math.ulp(1.0)}).
     *
     * @see #ALLOW_TRIGONOMETRIC_IDENTITIES
     */
    private static boolean identityEquals(final double actual, final double expected) {
        return !(abs(actual - expected) >                               // Use !(a > b) instead of (a <= b) in order to tolerate NaN.
                (ANGULAR_TOLERANCE / 1000) * max(1, abs(expected)));    // Increase tolerance for values outside the [-1 … +1] range.
    }

    /**
     * Coefficients in the series expansion of the forward projection,
     * depending only on {@linkplain #eccentricity eccentricity} value.
     * The series expansion is of the following form:
     *
     *     <blockquote>cf₂⋅ｆ(2θ) + cf₄⋅ｆ(4θ) + cf₆⋅ｆ(6θ) + cf₈⋅ｆ(8θ)</blockquote>
     *
     * Those coefficients are named h₁, h₂, h₃ and h₄ in §1.3.5.1 of
     * IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
     *
     * <h4>Serialization note</h4>
     * We do not strictly need to serialize those fields since they could be computed after deserialization.
     * Bu we serialize them anyway in order to simplify a little bit this class (it allows us to keep those
     * fields final) and because values computed after deserialization could be slightly different than the
     * ones computed after construction since the constructor uses the double-double values provided by
     * {@link Initializer}.
     */
    private final double cf2, cf4, cf6, cf8;

    /**
     * Coefficients in the series expansion of the reverse projection,
     * depending only on {@linkplain #eccentricity eccentricity} value.
     */
    private final double ci2, ci4, ci6, ci8;

    /**
     * Creates a Transverse Mercator projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Transverse Mercator</q>.</li>
     *   <li><q>Transverse Mercator (South Orientated)</q>.</li>
     * </ul>
     *
     * @param  method      description of the projection parameters.
     * @param  parameters  the parameter values of the projection to create.
     */
    public TransverseMercator(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final Variant variant = variant(method, Variant.values(), null);
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        ParameterRole xOffset = ParameterRole.FALSE_EASTING;
        ParameterRole yOffset = ParameterRole.FALSE_NORTHING;
        if (variant == Variant.SOUTH_ORIENTATED) {
            xOffset = ParameterRole.FALSE_WESTING;
            yOffset = ParameterRole.FALSE_SOUTHING;
        }
        roles.put(ParameterRole.CENTRAL_MERIDIAN, LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.SCALE_FACTOR, SCALE_FACTOR);
        roles.put(xOffset, FALSE_EASTING);
        roles.put(yOffset, FALSE_NORTHING);
        return new Initializer(method, parameters, roles, variant);
    }

    /**
     * Creates a new Transverse Mercator projection from the given initializer.
     * This constructor is used also by {@link ZonedGridSystem}.
     */
    TransverseMercator(final Initializer initializer) {
        super(initializer, null);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        /*
         * Opportunistically use double-double arithmetic for computation of B since we will store
         * it in the denormalization matrix, and there is no sine/cosine functions involved here.
         */
        double cf4, cf6, cf8, ci4, ci6, ci8;
        final DoubleDouble B;
        {   // For keeping the `t` and `n` variables locale.
            /*
             * The n parameter is defined by the EPSG guide as:
             *
             *     n = f / (2-f)
             *
             * Where f is the flattening factor (1 - b/a). This equation can be rewritten as:
             *
             *     n = (1 - b/a) / (1 + b/a)
             *
             * As much as possible, b/a should be computed from the map projection parameters.
             * However if those parameters are not available anymore, then they can be computed
             * from the eccentricity as:
             *
             *     b/a = √(1 - ℯ²)
             */
            final var    nd = initializer.axisLengthRatio().ratio_1m_1p();
            final double n  = nd.doubleValue();                      // n = f / (2-f)
            final double n2 = n  * n;
            final double n3 = n2 * n;
            final double n4 = n2 * n2;
            /*
             * Coefficients for the forward projections.
             * Add the smallest values first in order to reduce rounding errors.
             */
            cf2 = (   41. /    180)*n4  +  ( 5. /  16)*n3  +  (-2. /  3)*n2  +  n/2;
            cf4 = (  557. /   1440)*n4  +  (-3. /   5)*n3  +  (13. / 48)*n2;
            cf6 = ( -103. /    140)*n4  +  (61. / 240)*n3;
            cf8 = (49561. / 161280)*n4;
            /*
             * Coefficients for the reverse projections.
             * Add the smallest values first in order to reduce rounding errors.
             */
            ci2 = (  -1. /    360)*n4  +  (37. /  96)*n3  +  (-2. /  3)*n2  +  n/2;
            ci4 = (-437. /   1440)*n4  +  ( 1. /  15)*n3  +  ( 1. / 48)*n2;
            ci6 = ( -37. /    840)*n4  +  (17. / 480)*n3;
            ci8 = (4397. / 161280)*n4;
            /*
             * Compute B  =  (1 + n²/4 + n⁴/64) / (1 + n)
             */
            B = nd.square().series(1, 0.25, 1./64).divide(nd.add(1));
        }
        /*
         * Compute M₀ = B⋅(ξ₁ + ξ₂ + ξ₃ + ξ₄) and negate in anticipation for what will be needed
         * in the denormalization matrix. We opportunistically use double-double arithmetic but
         * only for the final multiplication by B, for consistency with the translation term to
         * be stored in the denormalization matrix. It is not worth to use double-double in the
         * sum of sine functions because the extra digits would be meaningless.
         *
         * NOTE: the EPSG documentation makes special cases for φ₀ = 0 or ±π/2. This is not
         * needed here; we verified that the code below produces naturally the expected values.
         */
        final double Q = asinh(tan(φ0)) - eccentricity * atanh(eccentricity * sin(φ0));
        final double β = atan(sinh(Q));
        DoubleDouble M0 = B.negate().multiply(
                β + fma(cf2, sin(2*β),
                    fma(cf4, sin(4*β),
                    fma(cf6, sin(6*β),
                        cf8* sin(8*β)))), false);
        /*
         * At this point, all parameters have been processed. Now store
         * the linear operations in the (de)normalize affine transforms:
         *
         * Normalization:
         *   - Subtract central meridian to longitudes (done by the super-class constructor).
         *   - Convert longitudes and latitudes from degrees to radians (done by the super-class constructor)
         *
         * Denormalization
         *   - Scale x and y by B.
         *   - Subtract M₀ to the northing.
         *   - Multiply by the scale factor (done by the super-class constructor).
         *   - Add false easting and false northing (done by the super-class constructor).
         */
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(0, B, null);
        denormalize.convertBefore(1, B, M0);
        /*
         * When rewriting equations using trigonometric identities, some constants appear.
         * For example, sin(2θ) = 2⋅sinθ⋅cosθ, so we can factor out the 2 constant into the
         * corresponding `c` field.  Note: this factorization can only be performed after
         * the constructor finished to compute other constants.
         */
        if (ALLOW_TRIGONOMETRIC_IDENTITIES) {
            // Multiplication by powers of 2 does not bring any additional rounding error.
            cf4 *=  4;   ci4 *=  4;
            cf6 *= 16;   ci6 *= 16;
            cf8 *= 64;   ci8 *= 64;
        }
        this.cf4 = cf4;
        this.cf6 = cf6;
        this.cf8 = cf8;
        this.ci4 = ci4;
        this.ci6 = ci6;
        this.ci8 = ci8;
    }

    /**
     * Creates a new projection initialized to the same parameters as the given one.
     */
    TransverseMercator(final TransverseMercator other) {
        super(null, other);
        cf2 = other.cf2;
        cf4 = other.cf4;
        cf6 = other.cf6;
        cf8 = other.cf8;
        ci2 = other.ci2;
        ci4 = other.ci4;
        ci6 = other.ci6;
        ci8 = other.ci8;
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
        return createMapProjection(parameters.getFactory());
    }

    /**
     * Creates the <i>normalization</i> → {@code this} → <i>denormalization</i> transforms.
     * This method is defined for {@link ZonedGridSystem} implementation.
     *
     * @param  factory  the factory to use for creating the transform.
     */
    final MathTransform createMapProjection(final MathTransformFactory factory) throws FactoryException {
        TransverseMercator kernel = this;
        if (eccentricity == 0) {
            kernel = new Spherical(this);
        }
        return context.completeTransform(factory, kernel);
    }

    /**
     * Returns the domain of input coordinates.
     * The limits defined by this method are arbitrary and may change in any future implementation.
     * Current implementation sets a limit at 40° of longitude on each side of the central meridian
     * (this limit is mentioned in EPSG guidance notes)
     * and a limit at 84° of latitude (same as {@link Mercator} projection).
     */
    @Override
    public Optional<Envelope> getDomain(final DomainDefinition criteria) {
        final Envelope2D domain = new Envelope2D();
        domain.x = -PI/2 * (40d/90);
        domain.y = -POLAR_AREA_LIMIT;
        domain.width  = -2 * domain.x;
        domain.height = -2 * domain.y;
        return Optional.of(domain);
    }

    /**
     * Implementation of {@link #transform(double[], int, double[], int, boolean)} for points outside domain of validity.
     * Should be invoked only when the longitude is at more than 90° from central meridian, in which case result does not
     * exist. This method should <strong>not</strong> be invoked for points at Δλ ≤ 90° that we fail to compute, because
     * in such cases a {@link ProjectionException} should be thrown instead.
     */
    private static Matrix outsideDomainOfValidity(final double[] dstPts, final int dstOff, final boolean derivate) {
        if (dstPts != null) {
            dstPts[dstOff] = dstPts[dstOff+1] = Double.NaN;
        }
        if (derivate) {
            return new Matrix2(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
        return null;
    }

    /**
     * Projects the specified (λ,φ) coordinates (units in radians) and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     * The results must be multiplied by the denormalization matrix before to get linear distances.
     *
     * <h4>Accuracy and domain of validity</h4>
     * Projection errors depend on the difference ∆λ between longitude λ and the central meridian λ₀.
     * All Universal Transverse Mercator (UTM) projections aim for ∆λ ≤ 3°, but this implementation
     * can nevertheless handle larger values. Results have been compared with values provided by
     * <a href="http://doi.org/10.5281/zenodo.32470">Karney, C.F.F. (2009).
     * Test data for the transverse Mercator projection [Data set]. Zenodo.</a>
     * On the WGS84 ellipsoid we observed the following errors compared to Karney's data:
     *
     * <ul>
     *   <li>Errors less than 1 centimetre for ∆λ &lt; 60° at all latitudes.</li>
     *   <li>At latitudes far enough from equator (|φ| ≥ 20°), the domain can be extended up to ∆λ &lt;
     *       (1 − ℯ)⋅90° (≈ 82.63627282416406551° on WGS84) with errors less than 70 centimetres.</li>
     * </ul>
     *
     * <h5>Case of 82.6…° &lt; ∆λ ≤ 90°</h5>
     * Karney (2009) uses an “extended” domain of transverse Mercator projection for ∆λ ≥ (1 − ℯ)⋅90°,
     * but Apache SIS does not support such extension. Consequently, ∆λ values between (1 − ℯ)⋅90° and 90°
     * should be considered invalid but are not rejected by Apache SIS. Note that those invalid values are
     * consistent with the {@linkplain #inverseTransform(double[], int, double[], int) reverse projection}
     * (i.e. applying a projection followed by a reverse projection gives approximately the original values).
     *
     * <h6>Rational</h6>
     * Those coordinates are accepted despite the low accuracy of projection results because they are sometimes
     * needed for expressing bounding boxes. A bounding box may have corners located in invalid projection area
     * even if all features inside the box have valid coordinates. For "contains" and "intersects" tests between
     * envelopes, we do not need accurate coordinates; a monotonic behavior of x = f(λ) can be sufficient.
     *
     * <h5>Case of ∆λ &gt; 90°</h5>
     * Longitude values at a distance greater than 90° from the central meridian are rejected.
     * A {@link ProjectionException} is thrown in that case. This limit exists because the
     * Transverse Mercator projection is conceptually a Mercator projection rotated by 90°.
     * Consequently, <var>x</var> values tend toward infinity for ∆λ close to ±90°
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
        final double λ = srcPts[srcOff];
        final double φ = srcPts[srcOff+1];
        if (abs(λ) > 90 * (PI/180)) {
            /*
             * The Transverse Mercator projection is conceptually a Mercator projection rotated by 90°.
             * In Mercator projection, y values tend toward infinity for latitudes close to ±90°.
             * In Transverse Mercator, x values tend toward infinity for longitudes close to ±90°
             * at equator and after subtraction of central meridian. After we pass the 90° limit,
             * the Transverse Mercator formulas produce the same values at (90° + Δ) than at (90° - Δ).
             *
             * Problem is that 90° is an ordinary longitude value, not even close to the limit of longitude
             * values range (±180°). So having f(π/2+Δ, φ) = f(π/2-Δ, φ) results in wrong behavior in some
             * algorithms like the one used by `Envelopes.transform(CoordinateOperation, Envelope)`.
             * Since a distance of 90° from central meridian is far outside the Transverse Mercator
             * domain of validity anyway, we do not let the user go further.
             *
             * Historical note: in a previous version, we used a limit of 70° instead of 90° because results
             * became chaotic after 85°. That limit has been removed in later version because this method now
             * behaves like a monotonic function x = f(λ) for fixed φ values. We need to project coordinates
             * even in the area where accuracy is bad because projecting those coordinates may happen during
             * envelope projections. An envelope may have corners located in invalid projection area even if
             * all features inside the envelope have valid coordinates. For "contains" and "intersects" tests
             * between envelopes, we do not need accurate coordinates; a monotonic behavior can be sufficient.
             *
             * Reminder: difference between returning NaN or throwing an exception is as below:
             *
             *    - NaN means "value does not exist or is not a real number".
             *    - ProjectionException means "value should exist but cannot be computed".
             *
             * So it is okay to return NaN for values located at Δλ > 90°, but we should throw an exception
             * for values at Δλ ≤ 90° if we cannot compute them. Previous version of this method was throwing
             * an exception for all Δλ > 70°. Now that we accept all longitudes up to 90°, we return NaN instead.
             */
            if (Math.abs(IEEEremainder(λ, 2*PI)) > 90 * (PI/180)) {         // More costly check.
                return outsideDomainOfValidity(dstPts, dstOff, derivate);
            }
        }
        final double sinλ  = sin(λ);
        final double ℯsinφ = sin(φ) * eccentricity;
        final double Q     = asinh(tan(φ)) - atanh(ℯsinφ) * eccentricity;
        final double coshQ = cosh(Q);                                       // Cannot be smaller than 1.
        final double η0    = atanh(sinλ / coshQ);                           // Tends toward ±∞ if λ → ±90°.
        /*
         * Original formula: η0 = atanh(sin(λ) * cos(β)) where
         * cos(β) = cos(atan(sinh(Q)))
         *        = 1 / sqrt(1 + sinh²(Q))
         *        = 1 / (sqrt(cosh²(Q) - sinh²(Q) + sinh²(Q)))
         *        = 1 / sqrt(cosh²(Q))
         *        = 1 / cosh(Q)
         *
         * So η0 = atanh(sin(λ) / cosh(Q))
         */
        final double coshη0 = cosh(η0);
        final double ξ0     = asin(tanh(Q) * coshη0);
        /*
         * Compute sin(2⋅ξ₀), sin(4⋅ξ₀), sin(6⋅ξ₀), sin(8⋅ξ₀) and same for cos, but using the following
         * trigonometric identities in order to reduce the number of calls to Math.sin and cos methods.
         */
        final double sin_2ξ0 = sin(2*ξ0);
        final double cos_2ξ0 = cos(2*ξ0);
        final double sin_4ξ0, sin_6ξ0, sin_8ξ0,
                     cos_4ξ0, cos_6ξ0, cos_8ξ0;
        if (!ALLOW_TRIGONOMETRIC_IDENTITIES) {
            sin_4ξ0 = sin(4*ξ0);
            cos_4ξ0 = cos(4*ξ0);
            sin_6ξ0 = sin(6*ξ0);
            cos_6ξ0 = cos(6*ξ0);
            sin_8ξ0 = sin(8*ξ0);
            cos_8ξ0 = cos(8*ξ0);
        } else {
            final double sin2 = sin_2ξ0 * sin_2ξ0;
            final double cos2 = cos_2ξ0 * cos_2ξ0;
            sin_4ξ0 = sin_2ξ0 * cos_2ξ0;                assert identityEquals(sin_4ξ0, sin(4*ξ0) / 2) : ξ0;
            cos_4ξ0 = (cos2  - sin2)   * 0.5;           assert identityEquals(cos_4ξ0, cos(4*ξ0) / 2) : ξ0;
            sin_6ξ0 = (0.75  - sin2)   * sin_2ξ0;       assert identityEquals(sin_6ξ0, sin(6*ξ0) / 4) : ξ0;
            cos_6ξ0 = (cos2  - 0.75)   * cos_2ξ0;       assert identityEquals(cos_6ξ0, cos(6*ξ0) / 4) : ξ0;
            sin_8ξ0 =          sin_4ξ0 * cos_4ξ0;       assert identityEquals(sin_8ξ0, sin(8*ξ0) / 8) : ξ0;
            cos_8ξ0 =  0.125 - sin_4ξ0 * sin_4ξ0;       assert identityEquals(cos_8ξ0, cos(8*ξ0) / 8) : ξ0;
        }
        /*
         * Compute sinh(2⋅ξ₀), sinh(4⋅ξ₀), sinh(6⋅ξ₀), sinh(8⋅ξ₀) and same for cosh, but using the following
         * hyperbolic identities in order to reduce the number of calls to Math.sinh and cosh methods.
         * Note that the formulas are very similar to the above ones, with only some signs reversed.
         */
        final double sinh_2η0 = sinh(2*η0);
        final double cosh_2η0 = cosh(2*η0);
        final double sinh_4η0, sinh_6η0, sinh_8η0,
                     cosh_4η0, cosh_6η0, cosh_8η0;
        if (!ALLOW_TRIGONOMETRIC_IDENTITIES) {
            sinh_4η0 = sinh(4*η0);
            cosh_4η0 = cosh(4*η0);
            sinh_6η0 = sinh(6*η0);
            cosh_6η0 = cosh(6*η0);
            sinh_8η0 = sinh(8*η0);
            cosh_8η0 = cosh(8*η0);
        } else {
            final double sinh2 = sinh_2η0 * sinh_2η0;
            final double cosh2 = cosh_2η0 * cosh_2η0;
            cosh_4η0 = (cosh2 + sinh2) * 0.5;           assert identityEquals(cosh_4η0, cosh(4*η0) / 2) : η0;
            sinh_4η0 = cosh_2η0 * sinh_2η0;             assert identityEquals(sinh_4η0, sinh(4*η0) / 2) : η0;
            cosh_6η0 = cosh_2η0 * (cosh2   - 0.75);     assert identityEquals(cosh_6η0, cosh(6*η0) / 4) : η0;
            sinh_6η0 = sinh_2η0 * (sinh2   + 0.75);     assert identityEquals(sinh_6η0, sinh(6*η0) / 4) : η0;
            cosh_8η0 = sinh_4η0 * sinh_4η0 + 0.125;     assert identityEquals(cosh_8η0, cosh(8*η0) / 8) : η0;
            sinh_8η0 = sinh_4η0 * cosh_4η0;             assert identityEquals(sinh_8η0, sinh(8*η0) / 8) : η0;
        }
        /*
         * The projection of (λ,φ) is given by (η⋅B, ξ⋅B+M₀) — ignoring scale factors and false easting/northing.
         * But the B and M₀ parameters have been merged by the constructor with other linear operations in the
         * "denormalization" matrix. Consequently, we only need to compute (η,ξ) below.
         */
        if (dstPts != null) {
            // η(λ,φ)
            dstPts[dstOff  ] = cf8 * cos_8ξ0 * sinh_8η0
                             + cf6 * cos_6ξ0 * sinh_6η0
                             + cf4 * cos_4ξ0 * sinh_4η0
                             + cf2 * cos_2ξ0 * sinh_2η0
                             + η0;
            // ξ(λ,φ)
            dstPts[dstOff+1] = cf8 * sin_8ξ0 * cosh_8η0
                             + cf6 * sin_6ξ0 * cosh_6η0
                             + cf4 * sin_4ξ0 * cosh_4η0
                             + cf2 * sin_2ξ0 * cosh_2η0
                             + ξ0;
        }
        if (!derivate) {
            return null;
        }
        /*
         * Now compute the derivative, if the user asked for it.
         */
        final double cosλ          = cos(λ);                                          // λ
        final double cosφ          = cos(φ);                                          // φ
        final double cosh2Q        = coshQ * coshQ;                                   // Q
        final double sinhQ         = sinh(Q);
        final double tanhQ         = tanh(Q);
        final double cosh2Q_sin2λ  = cosh2Q - (sinλ * sinλ);                          // Qλ
        final double sinhη0        = sinh(η0);                                        // η0
        final double sqrt1_thQchη0 = sqrt(1 - (tanhQ * tanhQ) * (coshη0 * coshη0));   // Qη0

        // dQ_dλ = 0;
        final double dQ_dφ  = 1 / cosφ - eccentricitySquared * cosφ / (1 - ℯsinφ * ℯsinφ);

        final double dη0_dλ =  cosλ * coshQ         / cosh2Q_sin2λ;
        final double dη0_dφ = -dQ_dφ * sinλ * sinhQ / cosh2Q_sin2λ;

        final double dξ0_dλ = sinhQ * sinhη0 * cosλ / (cosh2Q_sin2λ * sqrt1_thQchη0);
        final double dξ0_dφ = (dQ_dφ * coshη0 / cosh2Q + dη0_dφ * sinhη0 * tanhQ) / sqrt1_thQchη0;
        /*
         * Jac(Proj(λ,φ)) is the Jacobian matrix of Proj(λ,φ) function.
         * So the derivative of Proj(λ,φ) is defined by:
         *
         *                   ┌                        ┐
         *                   │ ∂η(λ,φ)/∂λ, ∂η(λ,φ)/∂φ │
         * Jac             = │                        │
         *    (Proj(λ,φ))    │ ∂ξ(λ,φ)/∂λ, ∂ξ(λ,φ)/∂φ │
         *                   └                        ┘
         */
        // dξ(λ, φ) / dλ
        final double dξ_dλ = dξ0_dλ
                           + 2 * (cf2 * (dξ0_dλ * cos_2ξ0 * cosh_2η0 + dη0_dλ * sinh_2η0 * sin_2ξ0)
                           + 3 *  cf6 * (dξ0_dλ * cos_6ξ0 * cosh_6η0 + dη0_dλ * sinh_6η0 * sin_6ξ0)
                           + 2 * (cf4 * (dξ0_dλ * cos_4ξ0 * cosh_4η0 + dη0_dλ * sinh_4η0 * sin_4ξ0)
                           + 2 *  cf8 * (dξ0_dλ * cos_8ξ0 * cosh_8η0 + dη0_dλ * sinh_8η0 * sin_8ξ0)));

        // dξ(λ, φ) / dφ
        final double dξ_dφ = dξ0_dφ
                           + 2 * (cf2 * (dξ0_dφ * cos_2ξ0 * cosh_2η0 + dη0_dφ * sinh_2η0 * sin_2ξ0)
                           + 3 *  cf6 * (dξ0_dφ * cos_6ξ0 * cosh_6η0 + dη0_dφ * sinh_6η0 * sin_6ξ0)
                           + 2 * (cf4 * (dξ0_dφ * cos_4ξ0 * cosh_4η0 + dη0_dφ * sinh_4η0 * sin_4ξ0)
                           + 2 *  cf8 * (dξ0_dφ * cos_8ξ0 * cosh_8η0 + dη0_dφ * sinh_8η0 * sin_8ξ0)));

        // dη(λ, φ) / dλ
        final double dη_dλ = dη0_dλ
                           + 2 * (cf2 * (dη0_dλ * cosh_2η0 * cos_2ξ0 - dξ0_dλ * sin_2ξ0 * sinh_2η0)
                           + 3 *  cf6 * (dη0_dλ * cosh_6η0 * cos_6ξ0 - dξ0_dλ * sin_6ξ0 * sinh_6η0)
                           + 2 * (cf4 * (dη0_dλ * cosh_4η0 * cos_4ξ0 - dξ0_dλ * sin_4ξ0 * sinh_4η0)
                           + 2 *  cf8 * (dη0_dλ * cosh_8η0 * cos_8ξ0 - dξ0_dλ * sin_8ξ0 * sinh_8η0)));

        // dη(λ, φ) / dφ
        final double dη_dφ = dη0_dφ
                           + 2 * (cf2 * (dη0_dφ * cosh_2η0 * cos_2ξ0 - dξ0_dφ * sin_2ξ0 * sinh_2η0)
                           + 3 *  cf6 * (dη0_dφ * cosh_6η0 * cos_6ξ0 - dξ0_dφ * sin_6ξ0 * sinh_6η0)
                           + 2 * (cf4 * (dη0_dφ * cosh_4η0 * cos_4ξ0 - dξ0_dφ * sin_4ξ0 * sinh_4η0)
                           + 2 *  cf8 * (dη0_dφ * cosh_8η0 * cos_8ξ0 - dξ0_dφ * sin_8ξ0 * sinh_8η0)));

        return new Matrix2(dη_dλ, dη_dφ,
                           dξ_dλ, dξ_dφ);
    }

    /**
     * Transforms the specified (η, ξ) coordinates and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point cannot be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double η = srcPts[srcOff  ];
        final double ξ = srcPts[srcOff+1];
        /*
         * Following calculation of sin_2ξ, sin_4ξ, etc. is basically a copy-and-paste of the code in transform(…).
         * Its purpose is the same as for transform(…): reduce the number of calls to Math.sin(double) and other
         * methods.
         */
        final double sin_2ξ  = sin (2*ξ);
        final double cos_2ξ  = cos (2*ξ);
        final double sinh_2η = sinh(2*η);
        final double cosh_2η = cosh(2*η);
        final double sin_4ξ,  sin_6ξ,  sin_8ξ,
                     cos_4ξ,  cos_6ξ,  cos_8ξ,
                     sinh_4η, sinh_6η, sinh_8η,
                     cosh_4η, cosh_6η, cosh_8η;
        if (!ALLOW_TRIGONOMETRIC_IDENTITIES) {
            sin_4ξ = sin(4*ξ);
            cos_4ξ = cos(4*ξ);
            sin_6ξ = sin(6*ξ);
            cos_6ξ = cos(6*ξ);
            sin_8ξ = sin(8*ξ);
            cos_8ξ = cos(8*ξ);

            sinh_4η = sinh(4*η);
            cosh_4η = cosh(4*η);
            sinh_6η = sinh(6*η);
            cosh_6η = cosh(6*η);
            sinh_8η = sinh(8*η);
            cosh_8η = cosh(8*η);
        } else {
            final double sin2 = sin_2ξ * sin_2ξ;
            final double cos2 = cos_2ξ * cos_2ξ;
            sin_4ξ = sin_2ξ * cos_2ξ;                   assert identityEquals(sin_4ξ, sin(4*ξ) / 2) : ξ;
            cos_4ξ = (cos2  - sin2)   * 0.5;            assert identityEquals(cos_4ξ, cos(4*ξ) / 2) : ξ;
            sin_6ξ = (0.75  - sin2)   * sin_2ξ;         assert identityEquals(sin_6ξ, sin(6*ξ) / 4) : ξ;
            cos_6ξ = (cos2  - 0.75)   * cos_2ξ;         assert identityEquals(cos_6ξ, cos(6*ξ) / 4) : ξ;
            sin_8ξ =          sin_4ξ * cos_4ξ;          assert identityEquals(sin_8ξ, sin(8*ξ) / 8) : ξ;
            cos_8ξ =  0.125 - sin_4ξ * sin_4ξ;          assert identityEquals(cos_8ξ, cos(8*ξ) / 8) : ξ;

            final double sinh2 = sinh_2η * sinh_2η;
            final double cosh2 = cosh_2η * cosh_2η;
            cosh_4η = (cosh2 + sinh2) * 0.5;            assert identityEquals(cosh_4η, cosh(4*η) / 2) : η;
            sinh_4η = cosh_2η * sinh_2η;                assert identityEquals(sinh_4η, sinh(4*η) / 2) : η;
            cosh_6η = cosh_2η * (cosh2   - 0.75);       assert identityEquals(cosh_6η, cosh(6*η) / 4) : η;
            sinh_6η = sinh_2η * (sinh2   + 0.75);       assert identityEquals(sinh_6η, sinh(6*η) / 4) : η;
            cosh_8η = sinh_4η * sinh_4η + 0.125;        assert identityEquals(cosh_8η, cosh(8*η) / 8) : η;
            sinh_8η = sinh_4η * cosh_4η;                assert identityEquals(sinh_8η, sinh(8*η) / 8) : η;
        }
        /*
         * The actual inverse transform.
         */
        final double ξ0 = ξ - (ci8 * sin_8ξ * cosh_8η
                             + ci6 * sin_6ξ * cosh_6η
                             + ci4 * sin_4ξ * cosh_4η
                             + ci2 * sin_2ξ * cosh_2η);

        final double η0 = η - (ci8 * cos_8ξ * sinh_8η
                             + ci6 * cos_6ξ * sinh_6η
                             + ci4 * cos_4ξ * sinh_4η
                             + ci2 * cos_2ξ * sinh_2η);

        final double β = asin(sin(ξ0) / cosh(η0));
        final double Q = asinh(tan(β));
        /*
         * Following usually converges in 4 iterations.
         * The first iteration is unrolled.
         */
        double p = eccentricity * atanh(eccentricity * tanh(Q));
        double Qp = Q + p;
        for (int it=0; it<MAXIMUM_ITERATIONS; it++) {
            final double c = eccentricity * atanh(eccentricity * tanh(Qp));
            Qp = Q + c;
            if (abs(c - p) <= ITERATION_TOLERANCE) {
                dstPts[dstOff  ] = asin(tanh(η0) / cos(β));
                dstPts[dstOff+1] = atan(sinh(Qp));
                return;
            }
            p = c;
        }
        throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
    }

    @Override
    protected String toECMAScript(boolean inverse) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        if (!inverse) {
            //constants
            sb.append("\t_eccentricity : ").append(Double.toString(eccentricity)).append(",\n");
            sb.append("\t_cf2 : ").append(Double.toString(cf2)).append(",\n");
            sb.append("\t_cf4 : ").append(Double.toString(cf4)).append(",\n");
            sb.append("\t_cf6 : ").append(Double.toString(cf6)).append(",\n");
            sb.append("\t_cf8 : ").append(Double.toString(cf8)).append(",\n");

            sb.append(
                "\ttransform : function(src){\n" +
                "\t\tlet dst = new Array("+getTargetDimensions()+");\n" +
                "\t\tconst λ = src[0];\n" +
                "\t\tconst φ = src[1];\n" +
                "\t\tif (Math.abs(λ) > 90 * (Math.PI/180) && Math.abs(Math.IEEEremainder(λ, 2*PI)) > 90 * (Math.PI/180)) {\n" +
                "\t\t\tdst[0] = Number.NaN;\n" +
                "\t\t\tdst[1] = Number.NaN;\n" +
                "\t\t} else {\n" +
                "\t\t\tconst sinλ  = Math.sin(λ);\n" +
                "\t\t\tconst ℯsinφ = Math.sin(φ) * this._eccentricity;\n" +
                "\t\t\tconst Q     = Math.asinh(Math.tan(φ)) - Math.atanh(ℯsinφ) * this._eccentricity;\n" +
                "\t\t\tconst coshQ = Math.cosh(Q);\n" +
                "\t\t\tconst η0    = Math.atanh(sinλ / coshQ);\n" +
                "\t\t\tconst coshη0 = Math.cosh(η0);\n" +
                "\t\t\tconst ξ0     = Math.asin(Math.tanh(Q) * coshη0);\n" +
                "\t\t\tconst sin_2ξ0 = Math.sin(2*ξ0);\n" +
                "\t\t\tconst cos_2ξ0 = Math.cos(2*ξ0);\n" +
                "\t\t\tconst sin2 = sin_2ξ0 * sin_2ξ0;\n" +
                "\t\t\tconst cos2 = cos_2ξ0 * cos_2ξ0;\n" +
                "\t\t\tconst sin_4ξ0 = sin_2ξ0 * cos_2ξ0;\n" +
                "\t\t\tconst cos_4ξ0 = (cos2  - sin2)   * 0.5;\n" +
                "\t\t\tconst sin_6ξ0 = (0.75  - sin2)   * sin_2ξ0;\n" +
                "\t\t\tconst cos_6ξ0 = (cos2  - 0.75)   * cos_2ξ0;\n" +
                "\t\t\tconst sin_8ξ0 =          sin_4ξ0 * cos_4ξ0;\n" +
                "\t\t\tconst cos_8ξ0 =  0.125 - sin_4ξ0 * sin_4ξ0;\n" +
                "\t\t\tconst sinh_2η0 = Math.sinh(2*η0);\n" +
                "\t\t\tconst cosh_2η0 = Math.cosh(2*η0);\n" +
                "\t\t\tconst sinh2 = sinh_2η0 * sinh_2η0;\n" +
                "\t\t\tconst cosh2 = cosh_2η0 * cosh_2η0;\n" +
                "\t\t\tconst cosh_4η0 = (cosh2 + sinh2) * 0.5;\n" +
                "\t\t\tconst sinh_4η0 = cosh_2η0 * sinh_2η0;\n" +
                "\t\t\tconst cosh_6η0 = cosh_2η0 * (cosh2   - 0.75);\n" +
                "\t\t\tconst sinh_6η0 = sinh_2η0 * (sinh2   + 0.75);\n" +
                "\t\t\tconst cosh_8η0 = sinh_4η0 * sinh_4η0 + 0.125;\n" +
                "\t\t\tconst sinh_8η0 = sinh_4η0 * cosh_4η0;\n" +
                "\t\t\t// η(λ,φ)\n" +
                "\t\t\tdst[0] = this._cf8 * cos_8ξ0 * sinh_8η0\n" +
                "\t\t\t       + this._cf6 * cos_6ξ0 * sinh_6η0\n" +
                "\t\t\t       + this._cf4 * cos_4ξ0 * sinh_4η0\n" +
                "\t\t\t       + this._cf2 * cos_2ξ0 * sinh_2η0\n" +
                "\t\t\t       + η0;\n" +
                "\t\t\t// ξ(λ,φ)\n" +
                "\t\t\tdst[1] = this._cf8 * sin_8ξ0 * cosh_8η0\n" +
                "\t\t\t       + this._cf6 * sin_6ξ0 * cosh_6η0\n" +
                "\t\t\t       + this._cf4 * sin_4ξ0 * cosh_4η0\n" +
                "\t\t\t       + this._cf2 * sin_2ξ0 * cosh_2η0\n" +
                "\t\t\t       + ξ0;\n" +
                "\t\t}\n" +
                "\t\treturn dst;\n" +
                "\t}\n"
                );
        } else {
            //constants
            sb.append("\t_ITERATION_TOLERANCE : ").append(Double.toString(ITERATION_TOLERANCE)).append(",\n");
            sb.append("\t_MAXIMUM_ITERATIONS : ").append(MAXIMUM_ITERATIONS).append(",\n");
            sb.append("\t_eccentricity : ").append(Double.toString(eccentricity)).append(",\n");
            sb.append("\t_ci2 : ").append(Double.toString(ci2)).append(",\n");
            sb.append("\t_ci4 : ").append(Double.toString(ci4)).append(",\n");
            sb.append("\t_ci6 : ").append(Double.toString(ci6)).append(",\n");
            sb.append("\t_ci8 : ").append(Double.toString(ci8)).append(",\n");

            sb.append(
                "\ttransform : function(src){\n" +
                "\t\tlet dst = new Array("+getTargetDimensions()+");\n" +
                "\t\tconst η = src[0];\n" +
                "\t\tconst ξ = src[1];\n" +
                "\t\tconst sin_2ξ  = Math.sin(2*ξ);\n" +
                "\t\tconst cos_2ξ  = Math.cos(2*ξ);\n" +
                "\t\tconst sinh_2η = Math.sinh(2*η);\n" +
                "\t\tconst cosh_2η = Math.cosh(2*η);\n" +
                "\t\tconst sin2 = sin_2ξ * sin_2ξ;\n" +
                "\t\tconst cos2 = cos_2ξ * cos_2ξ;\n" +
                "\t\tconst sin_4ξ = sin_2ξ * cos_2ξ;\n" +
                "\t\tconst cos_4ξ = (cos2  - sin2)   * 0.5;\n" +
                "\t\tconst sin_6ξ = (0.75  - sin2)   * sin_2ξ;\n" +
                "\t\tconst cos_6ξ = (cos2  - 0.75)   * cos_2ξ;\n" +
                "\t\tconst sin_8ξ =          sin_4ξ * cos_4ξ;\n" +
                "\t\tconst cos_8ξ =  0.125 - sin_4ξ * sin_4ξ;\n" +
                "\t\t\n" +
                "\t\tconst sinh2 = sinh_2η * sinh_2η;\n" +
                "\t\tconst cosh2 = cosh_2η * cosh_2η;\n" +
                "\t\tconst cosh_4η = (cosh2 + sinh2) * 0.5;\n" +
                "\t\tconst sinh_4η = cosh_2η * sinh_2η;\n" +
                "\t\tconst cosh_6η = cosh_2η * (cosh2   - 0.75);\n" +
                "\t\tconst sinh_6η = sinh_2η * (sinh2   + 0.75);\n" +
                "\t\tconst cosh_8η = sinh_4η * sinh_4η + 0.125;\n" +
                "\t\tconst sinh_8η = sinh_4η * cosh_4η;\n" +
                "\t\tconst ξ0 = ξ - (this._ci8 * sin_8ξ * cosh_8η\n" +
                "\t\t              + this._ci6 * sin_6ξ * cosh_6η\n" +
                "\t\t              + this._ci4 * sin_4ξ * cosh_4η\n" +
                "\t\t              + this._ci2 * sin_2ξ * cosh_2η);\n" +
                "\t\t\n" +
                "\t\tconst η0 = η - (this._ci8 * cos_8ξ * sinh_8η\n" +
                "\t\t              + this._ci6 * cos_6ξ * sinh_6η\n" +
                "\t\t              + this._ci4 * cos_4ξ * sinh_4η\n" +
                "\t\t              + this._ci2 * cos_2ξ * sinh_2η);\n" +
                "\t\t\n" +
                "\t\tconst β = Math.asin(Math.sin(ξ0) / Math.cosh(η0));\n" +
                "\t\tconst Q = Math.asinh(Math.tan(β));\n" +
                "\t\tlet p = this._eccentricity * Math.atanh(this._eccentricity * Math.tanh(Q));\n" +
                "\t\tlet Qp = Q + p;\n" +
                "\t\tlet found = false;\n" +
                "\t\tfor (let it=0; !found && it<this._MAXIMUM_ITERATIONS; it++) {\n" +
                "\t\t\tconst c = this._eccentricity * Math.atanh(this._eccentricity * Math.tanh(Qp));\n" +
                "\t\t\tQp = Q + c;\n" +
                "\t\t\tif (Math.abs(c - p) <= this._ITERATION_TOLERANCE) {\n" +
                "\t\t\t\tdst[0] = Math.asin(Math.tanh(η0) / Math.cos(β));\n" +
                "\t\t\t\tdst[1] = Math.atan(Math.sinh(Qp));\n" +
                "\t\t\t\tfound = true;\n" +
                "\t\t\t}\n" +
                "\t\t\tp = c;\n" +
                "\t\t}\n" +
                "\t\tif (!found) {\n" +
                "\t\t\tdst[0] = Number.NaN;\n" +
                "\t\t\tdst[1] = Number.NaN;\n" +
                "\t\t}\n" +
                "\t\treturn dst;\n" +
                "\t}\n"
                );
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Provides the transform equations for the spherical case of the Transverse Mercator projection.
     *
     * @author  André Gosselin (MPO)
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @author  Rueben Schulz (UBC)
     */
    private static final class Spherical extends TransverseMercator {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 8903592710452235162L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param  other  the other projection (usually ellipsoidal) from which to copy the parameters.
         */
        protected Spherical(final TransverseMercator other) {
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
            final double λ = srcPts[srcOff];
            final double φ = srcPts[srcOff + 1];
            final double sinλ = sin(λ);
            final double cosλ = cos(λ);
            /*
             * The Transverse Mercator projection is conceptually a Mercator projection rotated by 90°.
             * See comment in the `super.transform(…)` implementation for more information about why we
             * need to reject ∆λ > 90°. The accuracy comment about high values of ∆λ do not apply here.
             */
            if (cosλ < 0) {                     // Implies Math.abs(IEEEremainder(λ, 2*PI)) > PI/2
                return outsideDomainOfValidity(dstPts, dstOff, derivate);
            }
            final double sinφ = sin(φ);
            final double cosφ = cos(φ);
            final double tanφ = sinφ / cosφ;
            final double B    = cosφ * sinλ;
            /*
             * Using Snyder's equation for calculating y, instead of the one used in PROJ.
             * Potential problems when y and x = 90 degrees, but behaves ok in tests.
             */
            if (dstPts != null) {
                dstPts[dstOff  ] = atanh(B);            // Snyder 8-1;
                dstPts[dstOff+1] = atan2(tanφ, cosλ);   // Snyder 8-3;
            }
            if (!derivate) {
                return null;
            }
            final double Bm  = B*B - 1;
            final double sct = cosλ*cosλ + tanφ*tanφ;
            return new Matrix2(-(cosφ * cosλ) / Bm,     // ∂x/∂λ
                                (sinφ * sinλ) / Bm,     // ∂x/∂φ
                                (tanφ * sinλ) / sct,    // ∂y/∂λ
                         cosλ / (cosφ * cosφ * sct));   // ∂y/∂φ
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
            final double sinhx = sinh(x);
            final double cosy  = cos(y);
            // `copySign` corrects for the fact that we made everything positive using sqrt(…)
            dstPts[dstOff  ] = atan2(sinhx, cosy);
            dstPts[dstOff+1] = copySign(asin(sqrt((1 - cosy*cosy) / (1 + sinhx*sinhx))), y);
        }
    }
}
