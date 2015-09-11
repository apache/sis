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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.provider.TransverseMercatorSouth;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static org.apache.sis.math.MathFunctions.asinh;
import static org.apache.sis.math.MathFunctions.atanh;


/**
 * <cite>Transverse Mercator</cite> projection (EPSG codes 9807).
 * This class implements the "JHS formulas" reproduced in
 * IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
 *
 * <div class="section">Description</div>
 * This is a cylindrical projection, in which the cylinder has been rotated 90°.
 * Instead of being tangent to the equator (or to an other standard latitude), it is tangent to a central meridian.
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
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see Mercator
 * @see ObliqueMercator
 */
public class TransverseMercator extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4717976245811852528L;

    /**
     * Whether to use the original formulas a published by EPSG, or their form modified using trigonometric identities.
     * The modified form uses trigonometric identifies for reducing the amount of calls to the {@link Math#sin(double)}
     * and similar method. The identities used are:
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
     * <ul>
     *   <li>sinh(2θ) = 2⋅sinhθ⋅coshθ</li>
     *   <li>cosh(2θ) = cosh²θ + sinh²θ =  2cosh²θ - 1 = 1 + 2sinh²θ</li>
     *   <li>sinh(3θ) = (3 + 4⋅sinh²θ)⋅sinhθ</li>
     *   <li>cosh(3θ) = ((4⋅cosh²θ) - 3)coshθ</li>
     *   <li>sinh(4θ) = (1 + 2⋅sinh²θ)⋅4.sinhθ⋅coshθ
     *                = 4.cosh(2θ).sinhθ⋅coshθ</li>
     *   <li>cosh(4θ) = (8⋅cosh⁴θ) - (8⋅cosh²θ) + 1
     *                = 8.cosh²θ(cosh²θ - 1) + 1
     *                = 8.cosh²(θ).sinh²(θ) + 1
     *                = 2.sinh²(2θ) + 1</li>
     * </ul>
     *
     * Note that since this boolean is static final, the compiler should exclude the code in the branch that is never
     * executed (no need to comment-out that code).
     */
    private static final boolean ORIGINAL_FORMULA = false;

    /**
     * Internal coefficients for computation, depending only on value of excentricity.
     * Defined in §1.3.5.1 of IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
     */
    private final double h1, h2, h3, h4, ih1, ih2, ih3, ih4;

    /**
     * Creates a Transverse Mercator projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Transverse Mercator"</cite>.</li>
     *   <li><cite>"Transverse Mercator (South Orientated)"</cite>.</li>
     * </ul>
     *
     * @param method     Description of the projection parameters.
     * @param parameters The parameter values of the projection to create.
     */
    public TransverseMercator(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @SuppressWarnings("fallthrough")
    @Workaround(library="JDK", version="1.7")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final boolean isSouth = identMatch(method, "(?i).*\\bSouth\\b.*", TransverseMercatorSouth.IDENTIFIER);
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles =
                new EnumMap<ParameterRole, ParameterDescriptor<Double>>(ParameterRole.class);
        ParameterRole xOffset = ParameterRole.FALSE_EASTING;
        ParameterRole yOffset = ParameterRole.FALSE_NORTHING;
        if (isSouth) {
            xOffset = ParameterRole.FALSE_WESTING;
            yOffset = ParameterRole.FALSE_SOUTHING;
        }
        roles.put(ParameterRole.CENTRAL_MERIDIAN, org.apache.sis.internal.referencing.provider.TransverseMercator.LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.SCALE_FACTOR,     org.apache.sis.internal.referencing.provider.TransverseMercator.SCALE_FACTOR);
        roles.put(xOffset,                        org.apache.sis.internal.referencing.provider.TransverseMercator.FALSE_EASTING);
        roles.put(yOffset,                        org.apache.sis.internal.referencing.provider.TransverseMercator.FALSE_NORTHING);
        return new Initializer(method, parameters, roles, isSouth ? (byte) 1 : (byte) 0);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private TransverseMercator(final Initializer initializer) {
        super(initializer);
        final double φ0 = toRadians(initializer.getAndStore(
                org.apache.sis.internal.referencing.provider.TransverseMercator.LATITUDE_OF_ORIGIN));
        /*
         * Opportunistically use double-double arithmetic for computation of B since we will store
         * it in the denormalization matrix, and there is no sine/cosine functions involved here.
         */
        final double n;
        final DoubleDouble B;
        {   // For keeping the 't' variable locale.
            /*
             * EPSG gives:      n  =  f / (2-f)
             * We rewrite as:   n  =  (1 - b/a) / (1 + b/a)
             */
            final DoubleDouble t = initializer.axisLengthRatio();   // t  =  b/a
            t.ratio_1m_1p();                                        // t  =  (1 - t) / (1 + t)
            n = t.doubleValue();
            /*
             * Compute B  =  (1 + n²/4 + n⁴/64) / (1 + n)
             */
            B = new DoubleDouble(t);        // B  =  n
            B.square();
            B.series(1, 0.25, 1./64);       // B  =  (1 + n²/4 + n⁴/64)
            t.add(1,0);
            B.divide(t);                    // B  =  (1 + n²/4 + n⁴/64) / (1 + n)
        }
        final double n2 = n  * n;
        final double n3 = n2 * n;
        final double n4 = n2 * n2;
        /*
         * Coefficients for direct projection.
         * Add the smallest values first in order to reduce rounding errors.
         */
        h1 = (   41. /    180)*n4  +  ( 5. /  16)*n3  +  (-2. /  3)*n2  +  n/2;
        h2 = (  557. /   1440)*n4  +  (-3. /   5)*n3  +  (13. / 48)*n2;
        h3 = ( -103. /    140)*n4  +  (61. / 240)*n3;
        h4 = (49561. / 161280)*n4;
        /*
         * Coefficients for inverse projection.
         * Add the smallest values first in order to reduce rounding errors.
         */
        ih1 = (  -1. /    360)*n4  +  (37. /  96)*n3  +  (-2. /  3)*n2  +  n/2;
        ih2 = (-437. /   1440)*n4  +  ( 1. /  15)*n3  +  ( 1. / 48)*n2;
        ih3 = ( -37. /    840)*n4  +  (17. / 480)*n3;
        ih4 = (4397. / 161280)*n4;
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
        final double Q = asinh(tan(φ0)) - excentricity * atanh(excentricity * sin(φ0));
        final double β = atan(sinh(Q));
        final DoubleDouble M0 = new DoubleDouble();
        M0.value = h4 * sin(8*β)
                 + h3 * sin(6*β)
                 + h2 * sin(4*β)
                 + h1 * sin(2*β)
                 + β;
        M0.multiply(B);
        M0.negate();
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
         *   - Subtract M0 to the northing.
         *   - Multiply by the scale factor (done by the super-class constructor).
         *   - Add false easting and false northing (done by the super-class constructor).
         */
        final MatrixSIS denormalize = context.getMatrix(false);
        denormalize.convertBefore(0, B, null);
        denormalize.convertBefore(1, B, M0);
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    TransverseMercator(final TransverseMercator other) {
        super(other);
        h1  = other. h1;
        h2  = other. h2;
        h3  = other. h3;
        h4  = other. h4;
        ih1 = other.ih1;
        ih2 = other.ih2;
        ih3 = other.ih3;
        ih4 = other.ih4;
    }

    /**
     * Returns the sequence of <cite>normalization</cite> → {@code this} → <cite>denormalization</cite> transforms
     * as a whole. The transform returned by this method except (<var>longitude</var>, <var>latitude</var>)
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
        TransverseMercator kernel = this;
        if (excentricity == 0) {
            kernel = new Spherical(this);
        }
        return context.completeTransform(factory, kernel);
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}.
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
        final double λ     = srcPts[srcOff];
        final double φ     = srcPts[srcOff + 1];

        final double ℯsinφ = excentricity * sin(φ);
        final double Q     = asinh(tan(φ)) - atanh(ℯsinφ) * excentricity;
        final double sinλ  = sin(λ);
        final double coshQ = cosh(Q);
        final double η0    = atanh(sinλ / coshQ);

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
        final double sin_2ξ0, sin_4ξ0, sin_6ξ0, sin_8ξ0,
                     cos_2ξ0, cos_4ξ0, cos_6ξ0, cos_8ξ0;
        if (ORIGINAL_FORMULA) {
            sin_2ξ0 = sin(2*ξ0);
            cos_2ξ0 = cos(2*ξ0);
            sin_4ξ0 = sin(4*ξ0);
            cos_4ξ0 = cos(4*ξ0);
            sin_6ξ0 = sin(6*ξ0);
            cos_6ξ0 = cos(6*ξ0);
            sin_8ξ0 = sin(8*ξ0);
            cos_8ξ0 = cos(8*ξ0);
        } else {
            sin_2ξ0 = sin(2*ξ0);                              // sin(2⋅ξ₀);
            cos_2ξ0 = cos(2*ξ0);                              // cos(2⋅ξ₀)
            final double sin2 = sin_2ξ0 * sin_2ξ0;
            final double cos2 = cos_2ξ0 * cos_2ξ0;
            sin_4ξ0 = 2 * sin_2ξ0 * cos_2ξ0;                  // sin(4⋅ξ₀)
            cos_4ξ0 = cos2 - sin2;                            // cos(4⋅ξ₀)
            sin_6ξ0 = (3 - 4*sin2) * sin_2ξ0;                 // sin(6⋅ξ₀)
            cos_6ξ0 = (4*cos2 - 3) * cos_2ξ0;                 // cos(6⋅ξ₀)
            sin_8ξ0 = 4*cos_4ξ0 * (sin_2ξ0 * cos_2ξ0);        // sin(8⋅ξ₀)
            cos_8ξ0 = 1 - 2*sin_4ξ0*sin_4ξ0;                  // cos(8⋅ξ₀)
        }

        /*
         * Compute sinh(2⋅ξ₀), sinh(4⋅ξ₀), sinh(6⋅ξ₀), sinh(8⋅ξ₀) and same for cosh, but using the following
         * hyperbolic identities in order to reduce the number of calls to Math.sinh and cosh methods.
         * Note that the formulas are very similar to the above ones, with only some signs reversed.
         */
        final double sinh_2η0, sinh_4η0, sinh_6η0, sinh_8η0,
                     cosh_2η0, cosh_4η0, cosh_6η0, cosh_8η0;
        if (ORIGINAL_FORMULA) {
            sinh_2η0 = sinh(2*η0);
            cosh_2η0 = cosh(2*η0);
            sinh_4η0 = sinh(4*η0);
            cosh_4η0 = cosh(4*η0);
            sinh_6η0 = sinh(6*η0);
            cosh_6η0 = cosh(6*η0);
            sinh_8η0 = sinh(8*η0);
            cosh_8η0 = cosh(8*η0);
        } else {
            sinh_2η0 = sinh(2*η0);                              // sinh(2⋅η₀);
            cosh_2η0 = cosh(2*η0);                              // cosh(2⋅η₀)
            final double sinh2 = sinh_2η0 * sinh_2η0;
            final double cosh2 = cosh_2η0 * cosh_2η0;
            sinh_4η0 = 2 * sinh_2η0 * cosh_2η0;                 // sinh(4⋅η₀)
            cosh_4η0 = cosh2 + sinh2;                           // cosh(4⋅η₀)
            sinh_6η0 = (3 + 4*sinh2) * sinh_2η0;                // sinh(6⋅η₀)
            cosh_6η0 = (4*cosh2 - 3) * cosh_2η0;                // cosh(6⋅η₀)
            sinh_8η0 = 4*cosh_4η0 * (sinh_2η0 * cosh_2η0);      // sinh(8⋅η₀)
            cosh_8η0 = 1 + 2*sinh_4η0*sinh_4η0;                 // cosh(8⋅η₀)
        }

        /*
         * Assuming that (λ, φ) ↦ Proj((λ, φ))
         * where Proj is defined by: Proj((λ, φ)) : (η(λ, φ), ξ(λ, φ)).
         *
         * => (λ, φ) ↦ (η(λ, φ), ξ(λ, φ)).
         */
        //-- ξ(λ, φ)
        final double ξ = h4 * sin_8ξ0 * cosh_8η0
                       + h3 * sin_6ξ0 * cosh_6η0
                       + h2 * sin_4ξ0 * cosh_4η0
                       + h1 * sin_2ξ0 * cosh_2η0
                       + ξ0;

        //-- η(λ, φ)
        final double η = h4 * cos_8ξ0 * sinh_8η0
                       + h3 * cos_6ξ0 * sinh_6η0
                       + h2 * cos_4ξ0 * sinh_4η0
                       + h1 * cos_2ξ0 * sinh_2η0
                       + η0;

        if (dstPts != null) {
            dstPts[dstOff    ] = η;
            dstPts[dstOff + 1] = ξ;
        }

        if (!derivate) {
            return null;
        }

        final double cosλ          = cos(λ);                                     //-- λ
        final double cosφ          = cos(φ);                                     //-- φ
        final double cosh2Q        = coshQ * coshQ;                              //-- Q
        final double sinhQ         = sinh(Q);
        final double tanhQ         = tanh(Q);
        final double cosh2Q_sin2λ  = cosh2Q - sinλ * sinλ;                       //-- Qλ
        final double sinhη0        = sinh(η0);                                   //-- η0
        final double sqrt1_thQchη0 = sqrt(1 - tanhQ * tanhQ * coshη0 * coshη0);  //-- Qη0

        //-- dQ_dλ = 0;
        final double dQ_dφ  = 1 / cosφ - excentricitySquared * cosφ / (1 - ℯsinφ * ℯsinφ);

        final double dη0_dλ =   cosλ * coshQ         / cosh2Q_sin2λ;
        final double dη0_dφ = - dQ_dφ * sinλ * sinhQ / cosh2Q_sin2λ;

        final double dξ0_dλ = sinhQ * sinhη0 * cosλ / (cosh2Q_sin2λ * sqrt1_thQchη0);
        final double dξ0_dφ = (dQ_dφ * coshη0 / cosh2Q + dη0_dφ * sinhη0 * tanhQ) / sqrt1_thQchη0;

        /*
         * Assuming that Jac(Proj((λ, φ))) is the Jacobian matrix of Proj((λ, φ)) function.
         *
         * So derivative Proj((λ, φ)) is defined by:
         *                    ┌                              ┐
         *                    │ dη(λ, φ) / dλ, dη(λ, φ) / dφ │
         * Jac              = │                              │
         *    (Proj(λ, φ))    │ dξ(λ, φ) / dλ, dξ(λ, φ) / dφ │
         *                    └                              ┘
         */
        //-- dξ(λ, φ) / dλ
        final double dξ_dλ = dξ0_dλ
                           + 2 * (h1 * (dξ0_dλ * cos_2ξ0 * cosh_2η0 + dη0_dλ * sinh_2η0 * sin_2ξ0)
                           + 3 *  h3 * (dξ0_dλ * cos_6ξ0 * cosh_6η0 + dη0_dλ * sinh_6η0 * sin_6ξ0)
                           + 2 * (h2 * (dξ0_dλ * cos_4ξ0 * cosh_4η0 + dη0_dλ * sinh_4η0 * sin_4ξ0)
                           + 2 *  h4 * (dξ0_dλ * cos_8ξ0 * cosh_8η0 + dη0_dλ * sinh_8η0 * sin_8ξ0)));

        //-- dξ(λ, φ) / dφ
        final double dξ_dφ = dξ0_dφ
                           + 2 * (h1 * (dξ0_dφ * cos_2ξ0 * cosh_2η0 + dη0_dφ * sinh_2η0 * sin_2ξ0)
                           + 3 *  h3 * (dξ0_dφ * cos_6ξ0 * cosh_6η0 + dη0_dφ * sinh_6η0 * sin_6ξ0)
                           + 2 * (h2 * (dξ0_dφ * cos_4ξ0 * cosh_4η0 + dη0_dφ * sinh_4η0 * sin_4ξ0)
                           + 2 *  h4 * (dξ0_dφ * cos_8ξ0 * cosh_8η0 + dη0_dφ * sinh_8η0 * sin_8ξ0)));

        //-- dη(λ, φ) / dλ
        final double dη_dλ = dη0_dλ
                           + 2 * (h1 * (dη0_dλ * cosh_2η0 * cos_2ξ0 - dξ0_dλ * sin_2ξ0 * sinh_2η0)
                           + 3 *  h3 * (dη0_dλ * cosh_6η0 * cos_6ξ0 - dξ0_dλ * sin_6ξ0 * sinh_6η0)
                           + 2 * (h2 * (dη0_dλ * cosh_4η0 * cos_4ξ0 - dξ0_dλ * sin_4ξ0 * sinh_4η0)
                           + 2 *  h4 * (dη0_dλ * cosh_8η0 * cos_8ξ0 - dξ0_dλ * sin_8ξ0 * sinh_8η0)));

        //-- dη(λ, φ) / dφ
        final double dη_dφ = dη0_dφ
                           + 2 * (h1 * (dη0_dφ * cosh_2η0 * cos_2ξ0 - dξ0_dφ * sin_2ξ0 * sinh_2η0)
                           + 3 *  h3 * (dη0_dφ * cosh_6η0 * cos_6ξ0 - dξ0_dφ * sin_6ξ0 * sinh_6η0)
                           + 2 * (h2 * (dη0_dφ * cosh_4η0 * cos_4ξ0 - dξ0_dφ * sin_4ξ0 * sinh_4η0)
                           + 2 *  h4 * (dη0_dφ * cosh_8η0 * cos_8ξ0 - dξ0_dφ * sin_8ξ0 * sinh_8η0)));

        return new Matrix2(dη_dλ, dη_dφ,
                           dξ_dλ, dξ_dφ);
    }

    /**
     * Transforms the specified (η, ξ) coordinates and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double η = srcPts[srcOff    ];
        final double ξ = srcPts[srcOff + 1];
        /*
         * Following calculation of sin_2ξ, sin_4ξ, etc. is basically a copy-and-paste of the code in transform(…).
         * Its purpose is the same than for transform(…): reduce the amount of calls to Math.sin(double) and other
         * methods.
         */
        final double sin_2ξ,  sin_4ξ,  sin_6ξ,  sin_8ξ,
                     cos_2ξ,  cos_4ξ,  cos_6ξ,  cos_8ξ,
                     sinh_2η, sinh_4η, sinh_6η, sinh_8η,
                     cosh_2η, cosh_4η, cosh_6η, cosh_8η;
        if (ORIGINAL_FORMULA) {
            sin_2ξ = sin(2*ξ);
            cos_2ξ = cos(2*ξ);
            sin_4ξ = sin(4*ξ);
            cos_4ξ = cos(4*ξ);
            sin_6ξ = sin(6*ξ);
            cos_6ξ = cos(6*ξ);
            sin_8ξ = sin(8*ξ);
            cos_8ξ = cos(8*ξ);

            cosh_2η = cosh(2*η);
            sinh_4η = sinh(4*η);
            cosh_4η = cosh(4*η);
            sinh_6η = sinh(6*η);
            cosh_6η = cosh(6*η);
            sinh_8η = sinh(8*η);
            cosh_8η = cosh(8*η);
        } else {
            sin_2ξ = sin(2*ξ);
            cos_2ξ = cos(2*ξ);
            final double sin2 = sin_2ξ * sin_2ξ;
            final double cos2 = cos_2ξ * cos_2ξ;
            sin_4ξ = 2 * sin_2ξ * cos_2ξ;
            cos_4ξ = cos2 - sin2;
            sin_6ξ = (3 - 4*sin2) * sin_2ξ;
            cos_6ξ = (4*cos2 - 3) * cos_2ξ;
            sin_8ξ = 4*cos_4ξ * (sin_2ξ * cos_2ξ);
            cos_8ξ = 1 - 2*sin_4ξ*sin_4ξ;

            sinh_2η = sinh(2*η);
            cosh_2η = cosh(2*η);
            final double sinh2 = sinh_2η * sinh_2η;
            final double cosh2 = cosh_2η * cosh_2η;
            sinh_4η = 2 * sinh_2η * cosh_2η;
            cosh_4η = cosh2 + sinh2;
            sinh_6η = (3 + 4*sinh2) * sinh_2η;
            cosh_6η = (4*cosh2 - 3) * cosh_2η;
            sinh_8η = 4*cosh_4η * (sinh_2η * cosh_2η);
            cosh_8η = 1 + 2*sinh_4η*sinh_4η;
        }
        /*
         * The actual inverse transform.
         */
        final double ξ0 = ξ - (ih4 * sin_8ξ * cosh_8η
                             + ih3 * sin_6ξ * cosh_6η
                             + ih2 * sin_4ξ * cosh_4η
                             + ih1 * sin_2ξ * cosh_2η);

        final double η0 = η - (ih4 * cos_8ξ * sinh_8η
                             + ih3 * cos_6ξ * sinh_6η
                             + ih2 * cos_4ξ * sinh_4η
                             + ih1 * cos_2ξ * sinh_2η);

        final double β = asin(sin(ξ0) / cosh(η0));
        final double Q = asinh(tan(β));
        /*
         * Following usually converges in 4 iterations.
         */
        double Qp = Q, p = 0;
        for (int i=0; i<MAXIMUM_ITERATIONS; i++) {
            final double c = excentricity * atanh(excentricity * tanh(Qp));
            Qp = Q + c;
            if (abs(c - p) <= ITERATION_TOLERANCE) {
                dstPts[dstOff    ] = asin(tanh(η0) / cos(β));
                dstPts[dstOff + 1] = atan(sinh(Qp));
                return;
            }
            p = c;
        }
        throw new ProjectionException(Errors.Keys.NoConvergence);
    }


    /**
     * Provides the transform equations for the spherical case of the Transverse Mercator projection.
     *
     * @author  André Gosselin (MPO)
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @author  Rueben Schulz (UBC)
     * @since   0.6
     * @version 0.6
     * @module
     */
    private static final class Spherical extends TransverseMercator {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 8903592710452235162L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param other The other projection (usually ellipsoidal) from which to copy the parameters.
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
            final double λ    = srcPts[srcOff];
            final double φ    = srcPts[srcOff + 1];
            final double sinλ = sin(λ);
            final double cosλ = cos(λ);
            final double sinφ = sin(φ);
            final double cosφ = cos(φ);
            final double tanφ = sinφ / cosφ;
            final double B    = cosφ * sinλ;
            /*
             * Using Snyder's equation for calculating y, instead of the one used in Proj4.
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
                throws ProjectionException
        {
            final double x = srcPts[srcOff  ];
            final double y = srcPts[srcOff+1];
            final double sinhx = sinh(x);
            final double cosy  = cos(y);
            // 'copySign' corrects for the fact that we made everything positive using sqrt(…)
            dstPts[dstOff  ] = atan2(sinhx, cosy);
            dstPts[dstOff+1] = copySign(asin(sqrt((1 - cosy*cosy) / (1 + sinhx*sinhx))), y);
        }
    }
}
