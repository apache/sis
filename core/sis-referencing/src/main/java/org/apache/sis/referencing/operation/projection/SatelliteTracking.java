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
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.provider.SatelliteTracking.*;


/**
 * <cite>Satellite-Tracking</cite> projection.
 * This projection has been developed in 1977 by Snyder and has no associated EPSG code.
 * This projection is neither conformal or equal-area, but has the property that ground tracks
 * for satellites orbiting the Earth with the same orbital parameters are shown as straight lines
 * on the map. Other properties are (Snyder 1987):
 *
 * <ul>
 *   <li>All meridians are equally spaced straight lines.
 *       They are parallel on cylindrical form and converging to a common point on conical form.</li>
 *   <li>All parallels are straight but unequally spaced.
 *       They are parallel on cylindrical form and are concentric circular arcs on conical form.</li>
 *   <li>Conformality occurs along two chosen parallels. Scale is correct along one of these parameters
 *       on the conical form and along both on the cylindrical form.</li>
 * </ul>
 *
 * <div class="section">Limitations</div>
 * This map projection supports only circular orbits. The Earth is assumed spherical.
 * Areas close to poles can not be mapped.
 *
 * <div class="section">References</div>
 * John P. Snyder., 1987. <u>Map Projections - A Working Manual</u>
 * chapter 28: <cite>Satellite-tracking projections</cite>.
 *
 * @author  Matthieu Bastianelli (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class SatelliteTracking extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 859940667477896653L;

    /**
     * Sines and cosines of inclination between the plane of the Earth's Equator and the plane
     * of the satellite orbit. The angle variable name is <var>i</var> in Snyder's book.
     *
     * @see org.apache.sis.internal.referencing.provider.SatelliteTracking#SATELLITE_ORBIT_INCLINATION
     */
    private final double cos_i, sin_i, cos2_i;

    /**
     * Ratio of satellite orbital period (P₂) over ascending node period (P₁).
     *
     * @see org.apache.sis.internal.referencing.provider.SatelliteTracking#SATELLITE_ORBITAL_PERIOD
     * @see org.apache.sis.internal.referencing.provider.SatelliteTracking#ASCENDING_NODE_PERIOD
     */
    private final double p2_on_p1;

    /**
     * Coefficients for the Conic Satellite-Tracking Projection.
     * Those values are {@link Double#NaN} in the cylindrical case.
     */
    private final double n, s0;

    /**
     * {@code true} if this projection is conic, or {@code false} if cylindrical or unknown.
     */
    private final boolean isConic;

    /**
     * Work around for RFE #4093999 in Sun's bug database ("Relax constraint on
     * placement of this()/super() call in constructors").
     */
    @Workaround(library = "JDK", version = "1.8")
    static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<NormalizedProjection.ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(NormalizedProjection.ParameterRole.class);
        roles.put(NormalizedProjection.ParameterRole.CENTRAL_MERIDIAN, CENTRAL_MERIDIAN);
        roles.put(ParameterRole.LATITUDE_OF_CONFORMAL_SPHERE_RADIUS, LATITUDE_OF_ORIGIN);
        return new Initializer(method, parameters, roles, (byte) 0);
    }

    /**
     * Creates a Satellite Tracking projection from the given parameters.
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public SatelliteTracking(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private SatelliteTracking(final Initializer initializer) {
        super(initializer);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        final double φ1 = toRadians(initializer.getAndStore(STANDARD_PARALLEL_1));
        final double φ2 = toRadians(initializer.getAndStore(STANDARD_PARALLEL_2));
        isConic = Math.abs(φ2 + φ1) > ANGULAR_TOLERANCE;
        /*
         * Common for both cylindrical and conic sattelite tracking projections.
         * Symbols from Snyder:
         *
         *   i  =  angle of inclination between the plane of Earth Equator and the plane of satellite orbit.
         *   P₁ =  length of Earth's rotation with respect to precessed ascending node.
         *   P₂ =  time required for revolution of the satellite.
         *   φ₁ =  standard parallel (North and South in cylindrical case).
         *   φ₂ =  second standard parallel (conic case only).
         */
        final double i = toRadians(initializer.getAndStore(SATELLITE_ORBIT_INCLINATION));
        cos_i    = cos(i);
        sin_i    = sin(i);
        cos2_i   = cos_i * cos_i;
        p2_on_p1 = initializer.getAndStore(SATELLITE_ORBITAL_PERIOD) / initializer.getAndStore(ASCENDING_NODE_PERIOD);
        final double cos_φ1  = cos(φ1);
        final double cos2_φ1 = cos_φ1 * cos_φ1;
        /*
         *
         */
        double scale;
        final Double ρ0;
        if (isConic) {
            final double sin_φ1 = sin(φ1);
            /*
             * Conic projection case.
             * Inclination of the groundtrack to the meridian at latitude φ1
             */
            final double F1  = computeFn(cos2_φ1);
            final double dλ0 = -asin(sin(φ0) / sin_i);       // eq.28-2a in Snyder
            final double dλ1 = -asin(sin_φ1  / sin_i);
            final double λt0 = computeλtn(dλ0);
            final double λt1 = computeλtn(dλ1);
            final double L0  = λt0 - p2_on_p1 * dλ0;
            final double L1  = λt1 - p2_on_p1 * dλ1;

            //tracking limit computed as 180 - i from Snyder's manual p.238
            if (φ1 == PI - i) {
                final double factor = (p2_on_p1 * cos_i - 1);
                n = sin_i / (factor * factor); //eq. 28-18 in Snyder
            } else if (φ2 != φ1) {
                final double cos_φ2 = cos(φ2);
                final double dλ2 = -asin(sin(φ2) / sin_i);
                final double λt2 = computeλtn(dλ2);
                final double L2 = λt2 - p2_on_p1 * dλ2;
                final double F2 = computeFn(cos_φ2 * cos_φ2);
                n = (F2 - F1) / (L2 - L1);
            } else {
                n = sin_φ1 * (p2_on_p1 * (2 * cos2_i - cos2_φ1) - cos_i) / (p2_on_p1 * cos2_φ1 - cos_i); //eq. 28-17 in Snyder
            }
            // cos(φ₁) × sin(F₁) / n
            scale = cos_φ1 * sin(F1) / n;
            s0 = F1 - n * L1;
            ρ0 = scale / sin(n * L0 + s0); // *R in eq.28-12 in Snyder
            scale = -scale;
        } else {
            /*
             * Cylindrical projection case.
             */
            n = s0 = Double.NaN;
            ρ0 = null;
            scale = sqrt(cos2_φ1 - cos2_i) * cos_φ1 / (p2_on_p1 * cos2_φ1 - cos_i);
        }
        /*
         * At this point, all parameters have been processed. Now process to their
         * validation and the initialization of (de)normalize affine transforms.
         */
        final MatrixSIS normalize = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        normalize.convertAfter(0, isConic ? n : cos_φ1, null);

        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(1, scale, ρ0);
        if (isConic) {
            denormalize.convertBefore(0, -scale, null);
        }
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}
     * (linear distance on a unit sphere). In addition, opportunistically computes the projection derivative
     * if {@code derivate} is {@code true}.
     *
     * <cite> The Yaxis lies along the central meridian λ0, y increasing
     * northerly, and X axis intersects perpendicularly at LATITUDE_OF_ORIGIN
     * φ0, x increasing easterly.
     * </cite>
     *
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinates can not be converted.
     */
    @Override
    public Matrix transform(double[] srcPts, int srcOff,
                            double[] dstPts, int dstOff,
                            boolean derivate) throws ProjectionException
    {
        final double φ         = srcPts[srcOff + 1];
        final double sinφ_sini = sin(φ) / sin_i;
        final double dλ        = -asin(sinφ_sini);
        final double tan_dλ    = tan(dλ);
        final double λt        = atan(tan_dλ * cos_i);
        double x     = srcPts[srcOff];          // In cylindrical case, x=λ (assuming Earth radius of 1).
        double y     = λt - p2_on_p1 * dλ;      // In cylindrical case, y=L. Otherwise will be adjusted.
        double dx_dL = 0;       // Part of dx_dφ
        double dy_dL = 1;       // Part of dy_dφ
        if (isConic) {
            double nλs0 = n*y + s0;
            if (nλs0 * n < 0) {
                /*
                 * if nλs0 does not have the sign than n, the (x,y) values computed below would suddenly
                 * change their sign. The y values lower or greater (depending of n sign) than -s0/n can
                 * not be plotted. Snyder suggests to use another projection if cosmetic output is wanted.
                 * For now, we just set the results to NaN (meaning "no result", which is not the same than
                 * TransformException which means that a result exists but can not be computed).
                 */
                nλs0 = Double.NaN;
            }
            final double iρ = sin(nλs0);        // Inverse of ρ.
            y = cos(x) / iρ;
            x = sin(x) / iρ;                    // Must be last.
            if (derivate) {
                final double dρ_dφ = -n / tan(nλs0);
                dx_dL = x * dρ_dφ;
                dy_dL = y * dρ_dφ;
            }
        }
        if (dstPts != null) {
            dstPts[dstOff    ] = x;
            dstPts[dstOff + 1] = y;
        }
        if (!derivate) {
            return null;
        }
        // first computed term associated with derivative of (-dλ/dφ)
        final double dL_dφ = ((cos(φ) / sin_i) / sqrt(1 - sinφ_sini*sinφ_sini))
                           * (p2_on_p1 - ((1 + tan_dλ*tan_dλ)*cos_i/(1 + λt*λt)));

        return new Matrix2(isConic ? +y : 1,    dx_dL * dL_dφ,
                           isConic ? -x : 0,    dy_dL * dL_dφ);
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the coordinates can not be converted.
     */
    @Override
    protected void inverseTransform(double[] srcPts, int srcOff,
                                    double[] dstPts, int dstOff)
                                    throws ProjectionException {

        double x = srcPts[srcOff];
        double L = srcPts[srcOff + 1];
        if (isConic) {
            final double ρ = copySign(hypot(x,L), n);
            x = atan(x / L);                            // Undefined if x = y = 0.
            L = (asin(1 / ρ) - s0) / n;                 // Equation 28-26 in Snyder with R=1.
        }
        /*
         * Approximation of the latitude associated with L coefficient by applying Newton-Raphson method.
         * Equations 28-24, 28-25 and then 28-22 in Snyder's book.
         */
        double Δdλ, dλ = -PI/2;
        int iter = Formulas.MAXIMUM_ITERATIONS;
        do {
            if (--iter < 0) {
                throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
            }
            final double dλn = dλ;

            // Alternative calculation with Snyder's eq.  28-20 and 28-21
//            λt = l + p2_on_p1 * dλ_n;
//            dλ = atan(tan(λt) / cos_i);

            final double A   = tan(L + p2_on_p1 * dλn) / cos_i;
            final double A2  = A*A;
            Δdλ = (atan(A) - dλn) / (1 - (A2 + 1/cos2_i) * (p2_on_p1*cos_i/(A2 + 1)));
            dλ  = dλn + Δdλ;
        } while (abs(Δdλ) >= ANGULAR_TOLERANCE);
        dstPts[dstOff  ] = x;
        dstPts[dstOff+1] = -asin(sin(dλ) * sin_i);
    }

    /**
     * Method to compute the φn coefficient according to equation 28-9
     * in Snyder's Map Projections manual.
     *
     * @param cos2_φn : square of the φn 's cosine.
     * @return Fn  coefficient associated with the φn latitude.
     */
    private double computeFn(final double cos2_φn) {
        return atan((p2_on_p1 * cos2_φn - cos_i) / sqrt(cos2_φn - cos2_i)); // eq.28-9 in Snyder
    }

    /**
     * Method to compute the φn coefficient according to equation 28-3a
     * in Snyder's Map Projections manual.
     *
     * @param dλn  coefficient associated with the φn latitude.
     * @return λtn  coefficient associated with the φn latitude.
     */
    private double computeλtn(final double dλn) {
        return atan(tan(dλn) * cos_i); // eq.28-3a in Snyder
    }
}
