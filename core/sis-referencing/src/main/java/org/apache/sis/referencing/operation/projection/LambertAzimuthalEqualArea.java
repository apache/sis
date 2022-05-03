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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.Formulas.fastHypot;
import static org.apache.sis.internal.referencing.provider.LambertAzimuthalEqualArea.*;


/**
 * <cite>Lambert Azimuthal Equal Area</cite> projection (EPSG code 9820).
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Lambert_azimuthal_equal-area_projection">Lambert Azimuthal projection on Wikipedia</a></li>
 *   <li><a href="https://mathworld.wolfram.com/LambertAzimuthalEqual-AreaProjection.html">Lambert Azimuthal Equal-Area projection on MathWorld</a></li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public class LambertAzimuthalEqualArea extends AuthalicConversion {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7419101460426922558L;

    /**
     * Sine and cosine of authalic latitude of origin. In the spherical case,
     * the authalic latitude ß and the geodetic latitude φ are the same.
     *
     * <h4>Possible simplification</h4>
     * In equatorial case, sin(ß₀)=0 and cos(ß₀)=1. We could do special cases with simplifications in the
     * {@code transform(…)} formulas, but the result does not seem simpler enough to worth code branching.
     *
     * <p>In the polar case, sin(ß₀)=1 and cos(ß₀)=0. But the equations become indeterminate (we get 0/0)
     * and a different set of equations must be used.</p>
     */
    private final double sinß0, cosß0;

    /**
     * {@code true} if the projection is at a pole.
     * This implementation does not need to distinguish between North and South pole.
     * Formulas in this class are for the South case only, and this class handles the
     * North case by reverting the sign of φ before conversion and y after conversion.
     */
    private final boolean polar;

    /**
     * Creates a Lambert Azimuthal Equal Area projection from the given parameters.
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public LambertAzimuthalEqualArea(final OperationMethod method, final Parameters parameters) {
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
        roles.put(ParameterRole.CENTRAL_MERIDIAN, LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);
        return new Initializer(method, parameters, roles, null);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private LambertAzimuthalEqualArea(final Initializer initializer) {
        super(initializer);
        final double φ0    = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        final double sinφ0 = sin(φ0);
        final double cosφ0 = cos(φ0);
        polar = abs(sinφ0) == 1;            // sin(φ) == 1 implies a tolerance ∆φ≈1E-6°.
        sinß0 = qm(sinφ0) / qmPolar;
        cosß0 = sqrt(1 - sinß0*sinß0);
        /*
         * In the polar case we have cos(φ₀) ≈ 0 and cos(ß₀) ≈ 0, which cause D = 0/0.
         * Trying to evaluate the indeterminate with L'Hôpital's rule produce infinity.
         * Consequently a different set of formulas for the polar form must be used not
         * only here but also in the `transform(…)` and `inverseTransform(…)` methods.
         */
        final MatrixSIS denormalize = getContextualParameters().getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        if (polar) {
            final DoubleDouble c = initializer.axisLengthRatio();
            denormalize.convertBefore(0, c, null);
            denormalize.convertBefore(1, c, null);
            if (φ0 > 0) {                           // North pole case: use South pole formulas with sign of φ and y reverted.
                final MatrixSIS normalize = getContextualParameters().getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
                normalize  .convertBefore(1, -1, null);
                denormalize.convertBefore(1, -1, null);
            }
        } else {
            final double D = cosφ0 / (sqrt(1 - eccentricitySquared*(sinφ0*sinφ0)) * cosß0);
            final double Rq2 = (1 - eccentricitySquared) * qmPolar/2;
            denormalize.convertBefore(0,     D, null);
            denormalize.convertBefore(1, Rq2/D, null);
        }
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    LambertAzimuthalEqualArea(final LambertAzimuthalEqualArea other) {
        super(other);
        sinß0 = other.sinß0;
        cosß0 = other.cosß0;
        polar = other.polar;
    }

    /**
     * Returns the names of additional internal parameters which need to be taken in account when
     * comparing two {@code LambertAzimuthalEqualArea} projections or formatting them in debug mode.
     */
    @Override
    final String[] getInternalParameterNames() {
        return new String[] {"ß₀"};
    }

    /**
     * Returns the values of additional internal parameters which need to be taken in account when
     * comparing two {@code LambertAzimuthalEqualArea} projections or formatting them in debug mode.
     */
    @Override
    final double[] getInternalParameterValues() {
        return new double[] {asin(sinß0)};
    }

    /**
     * Converts the specified (λ,φ)) coordinate (units in radians) and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     *
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinate can not be converted.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws ProjectionException
    {
        final double λ    = srcPts[srcOff  ];
        final double φ    = srcPts[srcOff+1];
        final double sinλ = sin(λ);
        final double cosλ = cos(λ);
        final double sinφ = sin(φ);
        final double qm   = qm(sinφ);
        if (!polar) {
            /*
             * Note: in the spherical case, ß = φ (ß is the authalic radius).
             * We could do an optimization, but it would be a cost for the ellipsoidal
             * case without saving a lot for the spherical case. The general path is:
             *
             *     sinß   =   qm(sinφ) / qmPolar   =   2*sinφ / 2   =   sinφ
             */
            final double sinß = qm / qmPolar;
            final double cosß = sqrt(1 - sinß*sinß);
            final double c    = sinß0*sinß + cosß0*cosß*cosλ + 1;
            /*
             * The `c` factor is 0 when projecting the antipodal point of origin.
             * The antipodal point is actually all points on a circle of radius 2.
             * We can not return a single point, so NaN seems a safer value.
             */
            double B = sqrt(2 / c);
            if (B == Double.POSITIVE_INFINITY) {
                B = Double.NaN;
            }
            final double x = cosß*sinλ;                         // (x,y)×B = (E,N)
            final double y = cosß0*sinß - sinß0*cosß*cosλ;
            if (dstPts != null) {
                dstPts[dstOff  ] = B * x;
                dstPts[dstOff+1] = B * y;
            }
            /*
             * End of map projection. Compute the derivative if it was requested.
             */
            if (!derivate) {
                return null;
            }
            final double dsinß_dφ  = dqm_dφ(sinφ, cos(φ)) / qmPolar;
            final double dcosß_dφ  = -dsinß_dφ * (sinß/cosß);
            final double cosλdcosß =  dcosß_dφ * cosλ;
            final double dy_dλ     =   sinß0*x;
            final double db_dλ     =   cosß0*x / (2*c);
            final double dy_dφ     =  (cosß0*dsinß_dφ - sinß0*cosλdcosß);
            final double db_dφ     = -(sinß0*dsinß_dφ + cosß0*cosλdcosß) / (2*c);
            return new Matrix2(
                    B * (cosλ     + db_dλ*sinλ)*cosß,
                    B * (dcosß_dφ + db_dφ*cosß)*sinλ,
                    B * (dy_dλ    + db_dλ*y),
                    B * (dy_dφ    + db_dφ*y));
        }
        /*
         * Polar case needs special code because formula for the oblique case become indeterminate.
         * North pole case is handled with the same formula, but with sign of φ reversed by the normalize
         * affine transform (which cause the sign of q to be also reversed). After the transform, sign of
         * y will be reversed by the denormalize affine transform, so it should not be reversed here.
         */
        double ρ = sqrt(qmPolar + qm);
        if (sinφ == 1) {
            ρ = Double.NaN;     // Antipodal point.
        }
        final double x = ρ * sinλ;
        final double y = ρ * cosλ;
        if (dstPts != null) {
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
        }
        if (!derivate) {
            return null;
        }
        final double db_dφ = dqm_dφ(sinφ, cos(φ)) / (2*ρ);
        return new Matrix2(y, db_dφ * sinλ,
                          -x, db_dφ * cosλ);
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates and stores the (λ,φ) result in {@code dstPts}.
     *
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        double x = srcPts[srcOff  ];
        double y = srcPts[srcOff+1];
        double sinß_qp;                         // sin(ß) × qmPolar
        if (polar) {
            sinß_qp = (x*x + y*y) - qmPolar;
        } else {
            final double ρ     = fastHypot(x, y);
            final double C     = 2*asin(ρ / 2);
            final double cosC  = cos(C);
            final double sinC  = sin(C);
            final double ysinC = y*sinC;
            sinß_qp = cosC*sinß0;
            if (cosC != 1) {                    // cos(C) == 1 implies y/ρ = 0/0.
                sinß_qp += ysinC*cosß0/ρ;
            }
            sinß_qp *= qmPolar;
            y  = ρ*cosC*cosß0 - ysinC*sinß0;
            x *= sinC;
        }
        dstPts[dstOff  ] = atan2(x, y);
        dstPts[dstOff+1] = isSpherical ? asin(sinß_qp/2) : φ(sinß_qp);
    }

    /*
     * We do not provide a specialized sub-class for the spherical case because
     * simplifications are too small. We only need to skip the φ ↔ ß conversion.
     */
}
