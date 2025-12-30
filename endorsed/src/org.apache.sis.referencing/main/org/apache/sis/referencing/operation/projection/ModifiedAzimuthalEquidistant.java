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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.util.Workaround;
import static org.apache.sis.referencing.operation.provider.ModifiedAzimuthalEquidistant.*;


/**
 * <cite>Modified Azimuthal Equidistant</cite> projection (EPSG:9832).
 * This is an approximation of the oblique form of the <cite>Azimuthal Equidistant</cite> projection.
 * For distances under 800 kilometres this modification introduces no significant error.
 *
 * <h2>Limitation</h2>
 * This class does not support derivative (Jacobian matrix) yet.
 * See <a href="https://issues.apache.org/jira/browse/SIS-237">SIS-237 on issues tracker</a>.
 *
 * @todo Add Jacobian matrix formulas.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Maxime Gavens (Geomatys)
 *
 * @see AzimuthalEquidistant
 */
public class ModifiedAzimuthalEquidistant extends AzimuthalEquidistant {
    /**
     * For compatibility with different versions during deserialization.
     */
    private static final long serialVersionUID = 96569177715708509L;

    /**
     * A term involving radius of curvature ν₀, the latitude of origin φ₀ and the eccentricity.
     * The semi-major axis length <var>a</var> is omitted since it is handled outside this class.
     */
    private final double ℯ2_ν0_sinφ0;

    /**
     * The ℯ⋅sin(φ₀)/√(1 − ℯ²) term, used in direct projection.
     */
    private final double G;

    /**
     * The ℯ⋅cos(φ₀)/√(1 − ℯ²) term. This is the <var>H</var> term in EPSG guidance notes
     * but without the cos(α) term (omitted because α depends on the point to project).
     *
     * <p>Note that during reverse projection, EPSG guidance notes has a <var>A</var> as:
     * −ℯ²⋅cos²φ₀/(1 − ℯ²)⋅cos²α. We opportunistically use Hp² for that purpose.</p>
     */
    private final double Hp;

    /**
     * The 3⋅ℯ²⋅sin(φ₀)⋅cos(φ₀)/(1 − ℯ²) term. This is the <var>B</var> term in EPSG guidance notes
     * for reverse projection but without the terms that depend on coordinates of transformed point.
     */
    private final double Bp;

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
     * Creates a Modified Azimuthal Equidistant projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Modified Azimuthal Equidistant</q>.</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public ModifiedAzimuthalEquidistant(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="8", fixed="25")
    private ModifiedAzimuthalEquidistant(final Initializer initializer) {
        super(initializer);
        var ν0      = initializer.rν2(sinφ0).sqrt().inverse();
        ℯ2_ν0_sinφ0 = initializer.eccentricitySquared.multiply(ν0).doubleValue() * sinφ0;
        double f    = eccentricity / initializer.axisLengthRatio().doubleValue();           // √(1 - ℯ²) = b/a
        G           = f * sinφ0;
        Hp          = f * cosφ0;
        Bp          = 3*eccentricitySquared * (sinφ0*cosφ0) / (1 - eccentricitySquared);

        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(0, ν0, null);
        denormalize.convertBefore(1, ν0, null);
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
        AzimuthalEquidistant kernel = this;
        if (eccentricity == 0) {
            kernel = new AzimuthalEquidistant(this);
        }
        return context.completeTransform(parameters.getFactory(), kernel);
    }

    /**
     * Projects the specified (λ,φ) coordinates (units in radians)
     * and stores the (<var>x</var>,<var>y</var>) result in {@code dstPts}.
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
        final double λ     = srcPts[srcOff  ];
        final double φ     = srcPts[srcOff+1];
        final double cosλ  = cos(λ);
        final double sinλ  = sin(λ);
        final double cosφ  = cos(φ);
        final double sinφ  = sin(φ);
        final double rν    = sqrt(1 - eccentricitySquared*(sinφ*sinφ));
        final double tanΨ  = ((1 - eccentricitySquared)*sinφ + ℯ2_ν0_sinφ0*rν) / cosφ;
        final double rcosΨ = sqrt(1 + tanΨ*tanΨ);
        final double α     = atan2(sinλ, cosφ0*tanΨ - sinφ0*cosλ);
        final double sinα  = sin(α);
        final double cosα  = cos(α);
        final double H     = cosα * Hp;
        /*
         * Equations are:    s  =  asin(cos(φ₀)⋅sin(Ψ) − sin(φ₀)⋅cos(Ψ)) ⋅ signum(cos(α))     for small α
         *                   s  =  asin(sin(λ)⋅cos(Ψ) / sin(α))                               for other α
         *
         * Using identity:   sin(atan(x))  =  x / √(1 + x²)
         * Rewrite as:       sin(Ψ)  =   tan(Ψ) / √(1 + tan²Ψ)
         */
        double c;
        if (abs(sinα) < ANGULAR_TOLERANCE) {
            c = (cosφ0*tanΨ - sinφ0) / rcosΨ;
            if (cosα < 0) c = -c;
        } else {
            c = sinλ / (rcosΨ * sinα);
        }
        c = asin(c);                    // After this line this is the `s` value in EPSG guidance notes.
        final double s2 = c  * c;
        final double s3 = s2 * c;
        final double s4 = s2 * s2;
        final double s5 = s4 * c;
        final double H2 = H*H;
        final double GH = G*H;
        c *= 1 - (s2/6   *  H2*(1 -   H2))
               + (s3/8   *  GH*(1 - 2*H2))
               + (s4/120 * (H2*(4 - 7*H2) - 3*(G*G)*(1 - 7*H2)))
               - (s5/48  * GH);

        if (dstPts != null) {
            dstPts[dstOff  ] = c * sinα;
            dstPts[dstOff+1] = c * cosα;
        }
        if (!derivate) {
            return null;
        }
        /*
         * If we want to give another try in supporting this,
         * the following formulas may be used as a starting point:
         * https://svn.apache.org/repos/asf/sis/analysis/Modified%20Azimuthal%20Equidistant.wxmx
         */
        throw new ProjectionException("Derivative not yet implemented.");
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
        final double x  = srcPts[srcOff  ];
        final double y  = srcPts[srcOff+1];
        final double D2 = x*x + y*y;
        final double D  = sqrt(D2);                     // D = c′/ν₀, but division by ν₀ is already done here.
        /*
         * From ESPG guidance note:
         *
         *     α′    =  atan2(x, y)                     // x and y interchanged compared to usual atan2(y, x).
         *     sinα  =  sin(α′)
         *     cosα  =  cos(α′)
         *
         * But we rewrite in a way that avoid the use of trigonometric functions. We test (D != 0)
         * exactly (without epsilon) because even a very small value is sufficient for avoiding NaN:
         * Since D ≥ max(|x|,|y|) we get x/D and y/D close to zero.
         *
         * Note: the D ≥ max(|x|,|y|) assumption may not be always true (see `Formulas.fastHypot(…)`).
         * Consequently, sin(α) or cos(α) may be slightly greater than 1. However, they are multiplied by terms
         * involving eccentricity, which are smaller than 1. An empirical verification is done with cos(φ₀) = 1
         * in AzimuthalEquidistantTest.testValuesNearZero().
         */
        double sinα = 0;
        double cosα = 0;
        if (D != 0) {
            sinα = x / D;                               // x and y interchanged compared to usual atan2(y, x).
            cosα = y / D;
        }
              double negA = Hp * cosα; negA *= negA;    // negA = −A  compared to EPSG guidance note.
        final double B    = Bp * (1 + negA) * cosα;
        final double J    = D + (negA*(1 -   negA)*(D2*D )/6)
                              - (   B*(1 - 3*negA)*(D2*D2)/24);
        final double J2   = J*J;
        final double K    = 1 + (negA*J2/2) - (B*(J2*J)/6);
        final double sinJ = sin(J);
        final double sinΨ = sinφ0*cos(J) + cosφ0*sinJ*cosα;
        final double cosΨ = sqrt(1 - sinΨ*sinΨ);
        dstPts[dstOff  ]  = asin(sinα*sinJ / cosΨ);
        dstPts[dstOff+1]  = atan((1 - eccentricitySquared*sinφ0*K / sinΨ) * (sinΨ/cosΨ)
                               / (1 - eccentricitySquared));
    }
}
