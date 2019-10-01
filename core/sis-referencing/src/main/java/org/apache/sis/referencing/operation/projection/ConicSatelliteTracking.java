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
import org.apache.sis.util.Workaround;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.parameter.Parameters;

import static java.lang.Math.*;
import static java.lang.Double.NaN;
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
 * @version 1.1
 * @since   1.1
 * @module
 */
public class ConicSatelliteTracking extends NormalizedProjection {
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
    final double cos_i, sin_i, cos2_i;

    /**
     * Cosine of first standard parallel.
     *
     * @see org.apache.sis.internal.referencing.provider.SatelliteTracking#STANDARD_PARALLEL_1
     */
    final double cos_φ1;

    /**
     * Ratio of satellite orbital period (P2) over ascending node period (P1).
     *
     * @see org.apache.sis.internal.referencing.provider.SatelliteTracking#SATELLITE_ORBITAL_PERIOD
     * @see org.apache.sis.internal.referencing.provider.SatelliteTracking#ASCENDING_NODE_PERIOD
     */
    final double p2_on_p1;

    /**
     * Projection Cone's constant.
     */
    private final double n;

    /**
     * Approximation of the minimum latitude at infinite radius.
     * This is the limiting latitude at which the {@code L} coefficient takes the {@code -s0/n} value.
     * In such a situation the coefficient ρ computed for transformations is infinite.
     */
    private final double latitudeLimit;

    /**
     * Coefficients for the Conic Satellite-Tracking Projection.
     * {@code cosφ1_sinF1_n} = cos(φ₁) × sin(F₁) / n.
     */
    private final double cosφ1_sinF1_n, s0;

    /**
     * Work around for RFE #4093999 in Sun's bug database ("Relax constraint on
     * placement of this()/super() call in constructors").
     */
    @Workaround(library = "JDK", version = "1.8")
    static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<NormalizedProjection.ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(NormalizedProjection.ParameterRole.class);
        roles.put(NormalizedProjection.ParameterRole.CENTRAL_MERIDIAN, CENTRAL_MERIDIAN);
        return new Initializer(method, parameters, roles, (byte) 0);
    }

    /**
     * Creates a Satellite Tracking projection from the given parameters.
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public ConicSatelliteTracking(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    ConicSatelliteTracking(final Initializer initializer) {
        super(initializer);

        //======================================================================
        // Common for both cylindrical and conic sattelite tracking projections :
        //======================================================================
        final double i = toRadians(initializer.getAndStore(SATELLITE_ORBIT_INCLINATION));
        cos_i    = cos(i);
        sin_i    = sin(i);
        cos2_i   = cos_i * cos_i;
        p2_on_p1 = initializer.getAndStore(SATELLITE_ORBITAL_PERIOD) / initializer.getAndStore(ASCENDING_NODE_PERIOD);

        final double φ1      = toRadians(initializer.getAndStore(STANDARD_PARALLEL_1));  //appropriated use of toRadians??
        cos_φ1               = cos(φ1);
        final double cos2_φ1 = cos_φ1 * cos_φ1;

        //======================================================================
        // For conic projection :
        //=======================
        if (!(this instanceof CylindricalSatelliteTracking)) {
            final double sin_φ1 = sin(φ1);
            final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));   //appropriated use of toRadians??
            final double cos_φ0 = cos(φ0);
            final double cos2_φ0 = cos_φ0 * cos_φ0;
            final double sin_φ0 = sin(φ0);
            final double φ2 = toRadians(initializer.getAndStore(STANDARD_PARALLEL_2));   //appropriated use of toRadians??

//            final DoubleUnaryOperator computeFn = (cos2_φn) -> atan((p2_on_p1 * cos2_φn - cos_i) / sqrt(cos2_φn - cos2_i)); // eq.28-9 in Snyder
//            final double F0 = computeFn.applyAsDouble(cos2_φ0);
//            final double F1 = computeFn.applyAsDouble(cos2_φ1);
//            final DoubleUnaryOperator computedλn = (sin_φn) -> -asin(sin_φn / sin_i); // eq.28-2a in Snyder
//            final double dλ0 = computedλn.applyAsDouble(sin_φ0);
//            final double dλ1 = computedλn.applyAsDouble(sin_φ1);
//            final DoubleUnaryOperator computeλtn = (dλn) -> atan(tan(dλn) * cos_i); // eq.28-3a in Snyder
//            final double λt0 = computeλtn.applyAsDouble(dλ0);
//            final double λt1 = computeλtn.applyAsDouble(dλ1);

            final double F0 = computeFn(cos2_φ0);
            /*
             * Inclination of the groundtrack to the meridian at latitude φ1
             */
            final double F1 = computeFn(cos2_φ1);

            final double dλ0 = computedλn(sin_φ0);
            final double dλ1 = computedλn(sin_φ1);

            final double λt0 = computeλtn(dλ0);
            final double λt1 = computeλtn(dλ1);

            final double L0 = λt0 - p2_on_p1 * dλ0;
            final double L1 = λt1 - p2_on_p1 * dλ1;

            if (φ1 == PI - i) { //tracking limit computed as 180 - i from Snyder's manual p.238
                final double factor = (p2_on_p1 * cos_i - 1);
                final double factor2 = factor * factor;
                n = sin_i / (factor2); //eq. 28-18 in Snyder
            } else if (φ2 != φ1) {
                final double cos_φ2 = cos(φ2);
                final double cos2_φ2 = cos_φ2 * cos_φ2;
                final double sin_φ2 = sin(φ2);
//                final double dλ2 = computedλn.applyAsDouble(sin_φ2);
                final double dλ2 = computedλn(sin_φ2);
                final double λt2 = computeλtn(dλ2);
                final double L2 = λt2 - p2_on_p1 * dλ2;
//                final double F2 = computeFn.applyAsDouble(cos2_φ2);
                final double F2 = computeFn(cos2_φ2);
                n = (F2 - F1) / (L2 - L1);
            } else {
                n = sin_φ1 * (p2_on_p1 * (2 * cos2_i - cos2_φ1) - cos_i) / (p2_on_p1 * cos2_φ1 - cos_i); //eq. 28-17 in Snyder
            }
            cosφ1_sinF1_n = cos_φ1 * sin(F1)/n;
            s0 = F1 - n * L1;
            final double ρ0 = cosφ1_sinF1_n / sin(n * L0 + s0); // *R in eq.28-12 in Snyder

            //======================== Unsure ======================================
            // Aim to assess the limit latitude associated with -s0/n L-value.
            //
            latitudeLimit = latitudeFromNewtonMethod(-s0 / n);
            //======================================================================

//            //Additionally we can compute the radius of the circle to which groundtracks
//            //are tangent on the map :
//            ρs = cos_φ1xsin_F1 / n; //*R
            //======================================================================
            /*
         * At this point, all parameters have been processed. Now process to their
         * validation and the initialization of (de)normalize affine transforms.
             */
            final MatrixSIS normalize = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
            normalize.convertAfter(0, n, null);  //For conic tracking

            final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
            denormalize.convertBefore(1, 1, ρ0);  //For conic tracking
        } else {
            n = latitudeLimit = cosφ1_sinF1_n = s0 = NaN;
        }
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}
     * (linear distance on a unit sphere). In addition, opportunistically computes the projection derivative
     * if {@code derivate}
     * is {@code true}.
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
        final double λ = srcPts[srcOff];
        final double φ = srcPts[srcOff + 1];
        /*
         * According to the Snyder (page 236) in those cases cannot or should not be plotted.
         */
        if ( ((n >= 0) && (φ<=latitudeLimit)) || ((!(n >= 0)) && (φ>=latitudeLimit)) ){
            throw new ProjectionException(Resources.format(Resources.Keys.CanNotTransformCoordinates_2));
        }

        // compute an double array with {L} or {L, dL/dφ} if derivate recquired.
        final double[] vector_L = computeLanddLdφForDirectTransform(φ, derivate);
        final double L = vector_L[0];
        /*
         * As {@code latitudeLimit} is an approximation we repeat the test here.
         */
        if ( ((n >= 0) && (L<=-s0/n)) || ((!(n >= 0)) && (L>=-s0/n)) ){
            //TODO if usefull, could we add :
            // latitudeLimit = φ;
            throw new ProjectionException(Resources.format(Resources.Keys.CanNotTransformCoordinates_2));
        }

        final double nLandS0     = n*L+s0;
        final double sin_nLandS0 = sin(nLandS0);
        final double ρ           = cosφ1_sinF1_n/sin_nLandS0;

        final double sinλ = sin(λ);
        final double cosλ = cos(λ);
        if (dstPts != null) {
            dstPts[dstOff    ] =   ρ * sinλ;   // x
            dstPts[dstOff + 1] = - ρ * cosλ;   // y
        }

         if (!derivate) {
            return null;
        }

        //=========================To check the resolution =====================
        final double dρ_dφ = cosφ1_sinF1_n  * (-1 / (sin_nLandS0*sin_nLandS0)) *cos(nLandS0)  * n
                * vector_L[1]; // dL/dφ

        final double dx_dλ = ρ*cosλ;
        final double dx_dφ = sinλ * dρ_dφ;

        final double dy_dλ = ρ*sinλ;
        final double dy_dφ = -cosλ * dρ_dφ;
        //======================================================================

        return new Matrix2(dx_dλ, dx_dφ,
                           dy_dλ, dy_dφ);
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

        /* =====================================================================
        * Uncomputed scale factors :
        *===========================
        * k = ρ*n/cos(φ); // /R
        * h = k*tan(F)/tan(n*L+s0);
        ===================================================================== */
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

        final double x   = srcPts[srcOff];
        final double y   = srcPts[srcOff + 1];

        if ((x== 0) && (y == 0)) {
            //TODO : which values does it imply?
            throw new UnsupportedOperationException("Not supported yet for those coordinates."); //To change body of generated methods, choose Tools | Templates.
        }
        final double ρ = copySign(hypot(x,y), n);
        final double θ = atan(x/(-y) ); //undefined if x=y=0
        final double L = (asin(cosφ1_sinF1_n/ρ) -s0)/n; //undefined if x=y=0  //eq.28-26 in Snyder with R=1

        dstPts[dstOff  ] =  θ ;//λ
        dstPts[dstOff+1] = latitudeFromNewtonMethod(L); //φ
    }

    /**
     * Return an approximation of the latitude associated with L coefficient
     * by applying Newton-Raphson method.
     *
     * Eq. 28-24, 28-25 and then 28-22 in Snyder's Manual.
     *
     * @param l the L coefficient used in (cylindrical and conic)
     * satellite-tracking projections.
     * @return Approximation of the associated latitude.
     */
    protected final double latitudeFromNewtonMethod(final double l){
        double dλ = -PI/2;
        double dλn = Double.MIN_VALUE;
        double A, A2, Δdλ;

        int count=0;
        int maxIteration = 100; //TODO : check the value.

        while ((count==0) || abs(dλ-dλn) >= (0.01*PI/180)){ //TODO : check the condition. It is considered here that convergence is reached when improvement is lower than 0.01°.
            if (count >= maxIteration){
                throw new RuntimeException(Resources.format(Resources.Keys.NoConvergence));
            }
            dλn = dλ;

            // Alternative calculation with Snyder's eq.  28-20 and 28-21
//            λt = l + p2_on_p1 * dλ_n;
//            dλ = atan(tan(λt) / cos_i);

            A = tan(l + p2_on_p1 * dλn) / cos_i;
            A2=A*A;
            Δdλ = -(dλn-atan(A)) / (1- (A2 + 1/cos2_i) * (p2_on_p1*cos_i/(A2+1)) );
            dλ = dλn + Δdλ ;

            count++;
        }
        final double sin_dλ = sin(dλ);
        return -asin(sin_dλ * sin_i);
    }

    /**
     * Method to compute the φn coefficient according to equation 28-9
     * in Snyder's Map Projections manual.
     *
     * @param cos2_φn : square of the φn 's cosinus.
     * @return Fn  coefficient associated with the φn latittude.
     */
    private double computeFn(final double cos2_φn) {
        return atan((p2_on_p1 * cos2_φn - cos_i) / sqrt(cos2_φn - cos2_i)); // eq.28-9 in Snyder
    }
    /**
     * Method to compute the φn coefficient according to equation 28-2a
     * in Snyder's Map Projections manual.
     *
     * @param sin_φn : φn 's sinus.
     * @return dλn  coefficient associated with the φn latittude.
     */
    private double computedλn(final double sin_φn) {
        return -asin(sin_φn / sin_i); // eq.28-2a in Snyder
    }
    /**
     * Method to compute the φn coefficient according to equation 28-3a
     * in Snyder's Map Projections manual.
     *
     * @param dλn  coefficient associated with the φn latittude.
     * @return λtn  coefficient associated with the φn latittude.
     */
    private double computeλtn(final double dλn) {
        return atan(tan(dλn) * cos_i); // eq.28-3a in Snyder
    }

    /**
     * Method returning the L coefficient used for the direct transformation of
     * the Satellite-tracking projections and its partial derivate dL_dφ if
     * queried by a true value of the input parameter derivate.
     *
     * This method is used By :
     *     {@link ConicSatelliteTracking#transform(double[], int, double[], int, boolean) }
     * and {@link CylindricalSatelliteTracking#transform(double[], int, double[], int, boolean) }
     *
     * @param φ : input latitude of the projection's (direct)  method.
     * @param derivate : boolean value indicating if the partial derivate dL_dφ
     *                  must be computed.
     * @return a double array. It contains :
     *        - if derivate is false :  only the L coefficient value.
     *        - if derivate is true :  the L coefficient value and the partial
     *          derivate dL_dφ respectively at the at index 0 and 1 of the
     *          resulting array.
     */
    double[] computeLanddLdφForDirectTransform(final double φ, final boolean derivate){
        final double sinφ_sini = sin(φ) / sin_i;
        final double dλ        = -asin(sinφ_sini);
        final double tan_dλ    = tan(dλ);
        final double λt        = atan(tan_dλ * cos_i);
        final double L         = λt - p2_on_p1 * dλ;

        if (derivate){
            final double dL_dφ = ((cos(φ) / sin_i) *(1/sqrt(1-sinφ_sini*sinφ_sini)))  // first computed term associated with derivative of (-dλ/dφ)
                               * ( p2_on_p1 - ((1+tan_dλ*tan_dλ)*cos_i/(1+λt*λt) ) );
            return new double[] {L, dL_dφ};
        } else {
            return new double[] {L};
        }
    }
}
