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
import java.util.regex.Pattern;
import static java.lang.Math.*;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.measure.Angle;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole;
import org.apache.sis.referencing.operation.provider.ObliqueMercatorTwoPoints;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.util.Workaround;
import static org.apache.sis.math.MathFunctions.atanh;
import static org.apache.sis.referencing.internal.shared.Formulas.fastHypot;
import static org.apache.sis.referencing.operation.provider.ObliqueMercatorCenter.*;


/**
 * <cite>Oblique Mercator</cite> projection (EPSG codes 9812, 9815).
 * The Oblique Mercator projection can be seen as a generalization of {@link Mercator} and {@link TransverseMercator}
 * projections when the central line is not along the equator or a meridian, respectively.
 * This class covers also the <cite>Rectified Skew Orthomorphic</cite> (RSO) case.
 *
 * <p>There is different ways to specify the projection parameters:</p>
 * <ul>
 *   <li>Variant A (EPSG:9812) uses false easting/northing defined at the natural origin of the coordinate system.</li>
 *   <li>Variant B (EPSG:9815) uses false easting/northing defined at the projection center.</li>
 *   <li>ESRI "Two Points" variant defines the central line with two points instead of with an azimuth angle.</li>
 * </ul>
 *
 * Azimuth values of 0 and ±90 degrees are allowed, but for such cases the {@link Mercator} and
 * {@link TransverseMercator} projections should be preferred, both for performance and accuracy reasons.
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Emmanuel Giasson (Thales)
 *
 * @see Mercator
 * @see TransverseMercator
 */
public class ObliqueMercator extends ConformalProjection {
    /**
     * For compatibility with different versions during deserialization.
     */
    private static final long serialVersionUID = -2194259651400234956L;

    /**
     * Variants of Oblique Mercator projection.
     */
    private enum Variant implements ProjectionVariant {
        /** The <cite>Hotine Oblique Mercator (variant A)</cite> projection. */
        DEFAULT(".*\\bvariant\\s*A\\b.*", IDENTIFIER_OF_BASE, false, false),

        /** The <cite>Hotine Oblique Mercator (variant B)</cite> projection. */
        CENTER(".*\\bvariant\\s*B\\b.*", IDENTIFIER, false, true),

        /** The "<cite>Oblique Mercator</cite>" projection specified by two points on the central line. */
        TWO_POINTS(".*\\bTwo\\s*Point\\s*Natural\\b.*", null, true, false),

        /** The "<cite>Oblique Mercator</cite>" projection specified by two points on the central line. */
        TWO_POINTS_CENTER(".*\\bTwo\\s*Point\\s*Center\\b.*", null, true, true);

        /** Name pattern for this variant.    */ private final Pattern operationName;
        /** EPSG identifier for this variant. */ private final String  identifier;

        /**
         * Whether the projection has its central line defined by two points instead of an azimuth angle.
         * The two points variants are used by ESRI.
         */
        final boolean twoPoints;

        /**
         * Whether easting/northing values are defined at projection
         * center instead of at coordinate system natural origin.
         * This is {@code false} for <cite>Hotine Oblique Mercator (variant A)</cite>
         * and {@code true} for <cite>Hotine Oblique Mercator (variant B)</cite>.
         */
        final boolean center;

        /** Creates a new enumeration value.  */
        private Variant(final String operationName, final String identifier, final boolean twoPoints, final boolean center) {
            this.operationName = Pattern.compile(operationName, Pattern.CASE_INSENSITIVE);
            this.identifier    = identifier;
            this.twoPoints     = twoPoints;
            this.center        = center;
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
     * Constants used in the transformation.
     * Those coefficients depend only on the latitude of center.
     */
    private final double B, H;

    /**
     * Sine and Cosine values for {@code γ0} (the angle between the meridian and central line
     * at the intersection between the central line and the Earth equator on aposphere).
     */
    private final double sinγ0, cosγ0;

    /**
     * Creates an Oblique Mercator projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Hotine Oblique Mercator (variant A)</q>.</li>
     *   <li><q>Hotine Oblique Mercator (variant B)</q>.</li>
     *   <li><q>Hotine Oblique Mercator two point center</q> (from ESRI).</li>
     *   <li><q>Hotine Oblique Mercator two point natural origin</q> (from ESRI).</li>
     *   <li><q>Rectified Skew Orthomorphic</q>.</li>
     * </ul>
     *
     * @param  method      description of the projection parameters.
     * @param  parameters  the parameter values of the projection to create.
     */
    public ObliqueMercator(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final Variant variant = variant(method, Variant.values(), Variant.DEFAULT);
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
//      ParameterRole.CENTRAL_MERIDIAN intentionally excluded. It will be handled in the constructor instead.
        roles.put(ParameterRole.SCALE_FACTOR,   SCALE_FACTOR);
        roles.put(ParameterRole.FALSE_EASTING,  variant.center ? EASTING_AT_CENTRE  : FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING, variant.center ? NORTHING_AT_CENTRE : FALSE_NORTHING);
        return new Initializer(method, parameters, roles, variant);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private ObliqueMercator(final Initializer initializer) {
        super(initializer);
        final Variant variant = (Variant) initializer.variant;
        final double λc = toRadians(initializer.getAndStore(LONGITUDE_OF_CENTRE));
        final double φc = toRadians(initializer.getAndStore(LATITUDE_OF_CENTRE));
        final double sinφ  = sin(φc);
        final double cosφ  = cos(φc);
        final double cos2φ = cosφ * cosφ;
        /*
         * From EPSG guidance notes:
         *
         *      B  =  √{1+[ℯ²cos⁴φc / (1 – ℯ²)]}
         *      A  =  a⋅B⋅kc⋅√(1–ℯ²) / (1 – ℯ²sin²φc)
         *      t₀ =  tan(π/4 – φc/2) / [(1 – ℯsinφc) / (1 + ℯ⋅sinφc)]^(e/2)
         *      D  =  B⋅√(1–ℯ²) / [cos(φc)⋅√(1 – ℯ²sin²φc)]
         *      F  =  D + √(D² – 1)⋅SIGN(φc)                        — if D<1 use D²=1
         *      H  =  F⋅(t₀^B)
         *      G  =  (F – 1/F) / 2
         *      γ₀ =  asin[sin(αc) / D]
         *      λ₀ =  λc – [asin(G⋅tan(γ₀))] / B
         */
        B = sqrt(1 + eccentricitySquared*(cos2φ*cos2φ) / (1 - eccentricitySquared));
        final double Br  = B * initializer.axisLengthRatio().doubleValue();             // Br = B⋅√(1 - ℯ²)
        final double rν2 = initializer.rν2(sinφ).doubleValue();                         // rν² = (1 - ℯ²⋅sin²φc)
        final double A   = Br / rν2;                                                    // a and kc handled later.
        final double D   = Br / (cosφ * sqrt(rν2));
        final double sD1 = sqrt(max(D*D - 1, 0));
        final double F   = D + copySign(sD1, φc);
        H = F * pow(expΨ(φc, eccentricity*sinφ), -B);                                   // expΨ(…) = 1/t
        /*
         * Next coefficients depend on whether the user specified azimuth or two points.
         * Only the azimuth case is described in EPSG guidance notes.
         */
        double αc, γc, γ0, λ0;
        if (!variant.twoPoints) {
            αc = initializer.getAndStore(AZIMUTH);                                      // Convert to radians later.
            γc = toRadians(initializer.getAndStore(RECTIFIED_GRID_ANGLE, αc));
            αc = toRadians(αc);
            γ0 = asin(sin(αc) / D);
            double Gt = (F - 1/F) * 0.5 * tan(γ0);                                      // G⋅tan(γ₀)
            final double aGt = abs(Gt);
            if (aGt > 1 && aGt <= 1 + ANGULAR_TOLERANCE) {                              // Accept asin(±1.000…1)
                Gt = copySign(1, Gt);
            }
            λ0 = λc - asin(Gt) / B;
            if (Double.isNaN(λ0)) {
                final String name = AZIMUTH.getName().getCode();
                final Angle value = new Angle(toDegrees(αc));
                throw new InvalidParameterValueException(Resources.format(
                        Resources.Keys.IllegalParameterValue_2, name, value), name, value);
            }
        } else {
            /*
             * From Snyder equations 9-16 to 9-24 (pages 72-73):
             *
             *      E  =  [D ± √(D² - 1)]⋅(t₀^B)        — =H in EPSG formulas
             *      H  =  t₁^B                          — where t is the tsfn function with φ = φ₁
             *      L  =  t₂^B                          — where t is the tsfn function with φ = φ₂
             *      F  =  E/H                           — not the same F as from EPSG guidance notes.
             *      G  = (F - 1/F) / 2
             *      J  = (E² - L⋅H) / (E² + L⋅H)
             *      P  = (L - H) / (L + H)
             *      λ₀ = (λ₁ + λ₂)/2 + atan[J⋅tan(B⋅(λ₁ - λ₂)/2) / P] / B
             *      γ₀ = atan(sin[B⋅(λ₁ - λ₀)] / G)     —  Snyder warns to not use atan2(…) here.
             *      αc = asin(D⋅sin(γ₀))
             */
            final double φ1 = toRadians(initializer.getAndStore(ObliqueMercatorTwoPoints.LAT_OF_1ST_POINT));
            final double φ2 = toRadians(initializer.getAndStore(ObliqueMercatorTwoPoints.LAT_OF_2ND_POINT));
            final double λ1 = toRadians(initializer.getAndStore(ObliqueMercatorTwoPoints.LONG_OF_1ST_POINT));
                  double λ2 = toRadians(initializer.getAndStore(ObliqueMercatorTwoPoints.LONG_OF_2ND_POINT));
            final double H1 = pow(expΨ(φ1, sin(eccentricity*φ1)), -B);
            final double L  = pow(expΨ(φ2, sin(eccentricity*φ2)), -B);
            final double E2 = H * H;
            final double LH = L * H1;
            final double J  = (E2 - LH) / (E2 + LH);
            final double P  = (L - H1) / (L + H1);
            double Δλ = λ1 - λ2;
            if (abs(Δλ) > PI) {
                λ2 += copySign(2*PI, Δλ);                           // Adjustment recommended by Snyder.
                Δλ = λ1 - λ2;
            }
            λ0 = (λ1 + λ2)/2 - atan(J * tan(B*Δλ/2) / P) / B;
            Δλ = λ1 - λ0;
            if (abs(Δλ) > PI) {
                λ0 += copySign(2*PI, Δλ);                           // Adjustment recommended by Snyder.
                Δλ = λ1 - λ0;
            }
            γ0 = atan(2 * sin(B * Δλ) / (H/H1 - H1/H));             // Do not use atan2(…) here.
            αc = γc = asin(D * sin(γ0));
        }
        sinγ0 = sin(γ0);
        cosγ0 = cos(γ0);
        /*
         * At this point, all parameters have been processed. Now process to their
         * validation and the initialization of (de)normalize affine transforms.
         */
        context.getMatrix(MatrixRole.NORMALIZATION).convertAfter(0, null, -λ0);
        final MatrixSIS denormalize = getContextualParameters().getMatrix(MatrixRole.DENORMALIZATION);
        final Matrix3 rotation = new Matrix3();
        rotation.m00 =   rotation.m11 = cos(γc);
        rotation.m10 = -(rotation.m01 = sin(γc));
        denormalize.setMatrix(denormalize.multiply(rotation));
        /*
         * For variant B only, an additional (uc, vc) translation is applied here.
         * Note that the general form of uc works even in  αc = 90°  special case,
         * so we could omit the latter. But we find a difference varying from zero
         * to 0.2 metres between the two forms; we assume they are rounding errors.
         *
         *      vc = 0
         *      uc = (A / B)⋅atan[√(D² – 1) / cos(αc)]⋅SIGN(φc)
         *         = A⋅(λc – λ₀)  if  αc = 90°.
         */
        final double ArB = A / B;
        if (variant.center) {
            final double uc;
            if (abs(abs(αc) - PI/2) < ANGULAR_TOLERANCE) {
                uc = A * (λc - λ0);
            } else {
                uc = ArB * atan2(sD1, cos(αc));
            }
            denormalize.convertBefore(1, null, -copySign(uc, φc));
        }
        denormalize.convertBefore(0, ArB, null);
        denormalize.convertBefore(1, ArB, null);
    }

    /**
     * Returns the names of additional internal parameters which need to be taken in account when
     * comparing two {@code ObliqueMercator} projections or formatting them in debug mode.
     */
    @Override
    final String[] getInternalParameterNames() {
        return new String[] {"B", "H", "γ₀"};
    }

    /**
     * Returns the values of additional internal parameters which need to be taken in account when
     * comparing two {@code ObliqueMercator} projections or formatting them in debug mode.
     */
    @Override
    final double[] getInternalParameterValues() {
        return new double[] {B, H, γ0()};
    }

    /**
     * Returns the value used for computing {@link #sinγ0} and {@link #cosγ0}.
     * We did not stored that value because it is not used often.
     */
    private double γ0() {
        return (abs(sinγ0) < abs(cosγ0)) ? asin(sinγ0) : acos(cosγ0);
    }

    /**
     * Projects the specified (λ,φ) coordinates (units in radians) and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
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
        final double λ = srcPts[srcOff];
        final double φ = srcPts[srcOff + 1];
        /*
         * From EPSG guidance notes:
         *
         *      t  =  tan(π/4 – φ/2) / [(1 – ℯ⋅sinφ) / (1 + ℯ⋅sinφ)]^(ℯ/2)
         *      Q  =  H / t^B                                                   — note: t = 1/expΨ
         *      S  =  (Q – 1/Q) / 2
         *      T  =  (Q + 1/Q) / 2
         *      V  =  sin(B⋅(λ – λ₀))
         *      U  =  (–V⋅cos(γ₀) + S⋅sin(γ₀)) / T
         *      v  =  A⋅ln[(1 – U) / (1 + U)] / (2⋅B)
         *      u  =  A⋅atan{(S⋅cos(γ₀) + V⋅sin(γ₀)) / cos[B⋅(λ – λ₀)]} / B
         *
         * Code below computes (x,y) instead of (v,u) because A, B and λ₀ constants are factored out.
         *
         *      x  =  v⋅A/(2⋅B)
         *      y  =  u/A
         */
        final double sinφ  = sin(φ);
        final double Q     = H * pow(expΨ(φ, eccentricity*sinφ), B);
        final double iQ    = 1 / Q;
        final double S     = (Q - iQ) * 0.5;
        final double T     = (Q + iQ) * 0.5;
        final double V     = sin(B * λ);
        final double U     = (S*sinγ0 - V*cosγ0) / T;
        final double dV_dλ = cos(B * λ);
        if (dstPts != null) {
            dstPts[dstOff  ] = atanh(-U);                           // = 0.5 * log((1-U) / (1+U));;
            dstPts[dstOff+1] = atan2((S*cosγ0 + V*sinγ0), dV_dλ);
        }
        if (!derivate) {
            return null;
        }
        /*                   ┌              ┐
         *                   │ ∂x/∂λ, ∂x/∂φ │
         * Jacobian        = │              │
         *    (Proj(λ,φ))    │ ∂y/∂λ, ∂y/∂φ │
         *                   └              ┘
         */
        final double dQ_dφ =  B * Q * dy_dφ(sinφ, cos(φ));
        final double dU_dλ = -B * (cosγ0 / T) * dV_dλ;
        final double dU_dφ = dQ_dφ * (sinγ0 + (sinγ0 + U)/(Q*Q) - U) / (2*T);
        final double dS_dφ = 0.5*dQ_dφ * (1 + 1/(Q*Q));
        final double M   = (S*cosγ0 + V*sinγ0);
        final double L   = fastHypot(dV_dλ, M);
        final double M_L = M / L;
        final double P   = L + dV_dλ;
        final double D   = (P*P + M*M);
        final double dy_dλ = 2 * B * (dV_dλ * (sinγ0*P + (V - sinγ0*M) * M_L) + V*M) / D;    // Y = atan2(M, T)
        final double dy_dφ = 2 * cosγ0 * dS_dφ * (P - M*M_L) / D;
        final double R = U*U - 1;
        return new Matrix2(dU_dλ/R, dU_dφ/R,
                           dy_dλ,   dy_dφ);
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates and stores the result in {@code dstPts}
     * (angles in radians).
     *
     * @throws ProjectionException if the point cannot be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        /*
         * From EPSG guidance notes:
         *
         *      Q′ =  exp(-(B⋅v′/A))
         *      S′ =  (Q′ – 1/Q′) / 2
         *      T′ =  (Q′ + 1/Q′) / 2
         *      V′ =  sin(Bu′/A)
         *      U′ = (V′⋅cos(γ₀) + S′⋅sin(γ₀)) / T′
         *      t′ = {H / √[(1+U′) / (1–U′)]} ^ (1/B)
         *      χ  = π/2 – 2⋅atan(t′)
         *      λ  = λ₀ – atan[(S′⋅cos(γ₀) – V′⋅sin(γ₀)) / cos(B⋅u′/A)] / B
         *      φ  = χ + sin(2χ)⋅(e⁸⋅13/360     + e⁶⋅1/12   + e⁴⋅5/24 + e²⋅1/2)
         *             + sin(4χ)⋅(e⁸⋅811/11520  + e⁶⋅29/240 + e⁴⋅7/48)
         *             + sin(6χ)⋅(e⁸⋅81/1120    + e⁶⋅7/120)
         *             + sin(8χ)⋅(e⁸⋅4279/161280)
         *
         * The calculation of χ and its use in serie expansion is performed by the φ(t) function.
         *
         * Note: U shall be in the [−1 … +1] range, otherwise we get φ = NaN. At the limit U = ±1 we get φ = ±π/2
         * (division by zero is okay: infinite intermediate values produce the correct final result with φ(∞)=-π/2).
         * Empirical tests suggest that U is inside valid range even with points far outside the projection domain.
         * We found out-of-range values only at short distances (about 0.01 meter) from pole. In those cases U was
         * out-of-range only by 1 ULP. The (abs(U) >= 1) check is for handling those cases. Note that if U is NaN,
         * then we want to use the general formula for propagating the NaN.
         */
        final double x  = srcPts[srcOff  ];
        final double y  = srcPts[srcOff+1];
        final double Q  = exp(-x);
        final double Qi = 1 / Q;
        final double S  = (Q - Qi) * 0.5;
        final double T  = (Q + Qi) * 0.5;
        final double V  = sin(y);
        final double U  = (V*cosγ0 + S*sinγ0) / T;
        final double λ  = -atan2((S*cosγ0 - V*sinγ0), cos(y)) / B;
        final double φ  = (abs(U) >= 1) ? copySign(PI/2, U) : φ(pow(H / sqrt((1 + U) / (1 - U)), 1/B));
        dstPts[dstOff  ] = λ;
        dstPts[dstOff+1] = φ;
    }
}
