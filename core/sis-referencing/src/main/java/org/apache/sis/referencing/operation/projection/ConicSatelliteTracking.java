/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sis.referencing.operation.projection;

import static java.lang.Double.NaN;
import java.util.EnumMap;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Workaround;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;

import static java.lang.Math.*;
import org.apache.sis.internal.referencing.Resources;
import static org.apache.sis.internal.referencing.provider.SatelliteTracking.*;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;

/**
 * <cite>Cylindrical Satellite-Tracking projections</cite>.
 * 
 * <cite>
 * - All groundtracks for satellites orbiting the Earth with the same orbital
 * parameters are shown as straight lines on the map.
 *
 * - Cylindrical {@link CylindricalSatelliteTracking}
 * or conical {@link ConicSatelliteTracking} form available. 
 *
 * - Neither conformal nor equal-area.
 *
 * - All meridians are equally spaced straight lines, parallel on cylindrical
 * form and converging to a common point on conical form.
 *
 * - All parallels are straight and parallel on cylindrical form and are
 * concentric circular arcs on conical form. Parallels are unequally spaced.
 *
 * - Conformality occurs along two chosen parallels. Scale is correct along one
 * of these parameters on the conical form and along both on the cylindrical
 * form.
 *
 * Developed 1977 by Snyder
 * </cite>
 *
 * <cite> These formulas are confined to circular orbits and the SPHERICAL
 * Earth.</cite>
 *
 * <cite>The ascending and descending groundtracks meet at the northern an
 * southern tracking limits, lats. 80.9°N and S for landsat 1, 2 and 3. The map
 * Projection does not extend closer to the poles.</cite>
 *
 * This projection method has no associated EPSG code.
 *
 * Earth radius is normalized. Its value is 1 and is'nt an input parameter.
 *
 * =============================================================================
 * REMARK : The parameters associated with the satellite (and its orbit) could
 * be aggregate in class of the kind : Satellite or SatelliteOrbit.
 * =============================================================================
 *
 * @see <cite>Map Projections - A Working Manual</cite> By John P. Snyder
 * @author Matthieu Bastianelli (Geomatys)
 * @version 1.0
 */
public class ConicSatelliteTracking extends NormalizedProjection{
    
    /**
     * {@code SATELLITE_ORBIT_INCLINATION}
     */
    final double i;
    
    /**
     * Coefficients for both cylindrical and conic Satellite-Tracking Projection.
     */
    final double cos_i, sin_i, cos2_i, cos_φ1, p2_on_p1;
    
//    /**
//     * Radius of the circle radius of the circle to which groundtracks
//     * are tangent on the map.
//     */
//    private final double ρs;
    
    /**
     * Projection Cone's constant.
     */
    private final double n;
    
    /**
     * Approximation of the Minimum latitude at infinite radius. 
     * 
     * Limiting latitude to which the {@code L coefficient}
     * is associated with a particular case {@code L equals -s0/n}.
     */
    private final double latitudeLimit;
    /**
     * Boolean attribute indicating if the projection cone's constant is positive.
     * */
    private final boolean positiveN;
    /**
     * Coefficients for the Conic Satellite-Tracking Projection.
     */
    private final double cos_φ1xsin_F1, s0, ρ0;

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
     * Create a Cylindrical Satellite Tracking Projection from the given
     * parameters.
     *
     * The parameters are described in <cite>Map Projections - A Working
     * Manual</cite> By John P. Snyder.
     *
     * @param method : description of the projection parameters.
     * @param parameters : the parameter values of the projection to create.
     */
    public ConicSatelliteTracking(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Constructor for ConicSatelliteTracking.
     * 
     * Calculation are based on <cite>28 .SATTELITE-TRACKING PROJECTIONS </cite> 
     * in <cite> Map Projections - A Working Manual</cite> By John P. Snyder.
     * 
     * @param initializer 
     */
    ConicSatelliteTracking(final Initializer initializer) {
        super(initializer);

        //======================================================================
        // Common for both cylindrical and conic sattelite tracking projections :
        //======================================================================
        i        = toRadians(initializer.getAndStore(SATELLITE_ORBIT_INCLINATION)); // Radian input value.
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
        if(!(this instanceof CylindricalSatelliteTracking)) {
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

            cos_φ1xsin_F1 = cos_φ1 * sin(F1);

            final double dλ0 = computedλn(sin_φ0);
            final double dλ1 = computedλn(sin_φ1);

            final double λt0 = computeλtn(dλ0);
            final double λt1 = computeλtn(dλ1);

            final double L0 = λt0 - p2_on_p1 * dλ0;
            final double L1 = λt1 - p2_on_p1 * dλ1;

            if (φ1 == PI / 2 - i) { //tracking limit
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
            s0 = F1 - n * L1;
            ρ0 = cos_φ1xsin_F1 / (n * sin(n * L0 + s0)); // *R in eq.28-12 in Snyder 

            //======================== Unsure ======================================
            // Aim to assess the limit latitude associated with -s0/n L-value.
            //
            latitudeLimit = latitudeFromNewtonMethod(-s0 / n);
            positiveN = (n >= 0);
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
        }else{
            n = latitudeLimit = cos_φ1xsin_F1 = s0 = ρ0 = NaN;
            positiveN = false;
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
                            boolean derivate) throws ProjectionException {
        
        final double λ      = srcPts[srcOff];
        final double φ      = srcPts[srcOff + 1];
        
        /*
         * According to the Snyder (page 236) in those cases cannot or should not be plotted.
         */
        if( ((positiveN)&&(φ<=latitudeLimit)) || ((!positiveN)&&(φ>=latitudeLimit)) ){
            throw new ProjectionException(Resources.format(Resources.Keys.CanNotTransformCoordinates_2));
        }
        
        final double sin_φ  = sin(φ);
        final double dλ     = -asin(sin_φ / sin_i);
        final double λt     = atan(tan(dλ) * cos_i);
        final double L      = λt - p2_on_p1 * dλ;
        
        /*
         * As {@code latitudeLimit} is an approximation we repeat the test here.
         */
        if( ((positiveN)&&(L<=-s0/n)) || ((!positiveN)&&(L>=-s0/n)) ){
            //TODO if usefull, could we add :
            // latitudeLimit = φ;
            throw new ProjectionException(Resources.format(Resources.Keys.CanNotTransformCoordinates_2));
        }
        
        final double ρ      = cos_φ1xsin_F1/(n*sin(n*L+s0)); 
        final double θ      = λ;      // extracted n
        
        final double sinθ = sin(θ);
        final double cosθ = cos(θ);
        if (dstPts != null) {
            dstPts[dstOff    ] = ρ * sinθ;   // x
            dstPts[dstOff + 1] = ρ0 - ρ*cosθ;  // y       //TODO : extract ρ0 when ensuring : λ = λ - λ0;
        }
        
         if (!derivate) {
            return null;
        }

        //=========================TO Resolve =================================
//        final double dx_dλ = ρ*n*cosθ; 
//        final double dx_dφ =?;
//
//        final double dy_dλ =  ρ*n*sinθ;
//        final double dy_dφ = ?;
        //======================================================================
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        //Additionally we can compute the scale factors :
        // k = ρ*n/cos(φ); // /R
        // h = k*tan(F)/tan(n*L+s0);
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
        final double y   = srcPts[srcOff + 1] - ρ0;
        
        //TODO : extract - ρ0 : MatrixSIS convertBefore and convertAfter??
        
        if((x== 0) && (y == 0)) {
            //TODO : which values does it imply?
            throw new UnsupportedOperationException("Not supported yet for those coordinates."); //To change body of generated methods, choose Tools | Templates.
        }
               
        final double ρ = positiveN? hypot(x,y) : -hypot(x,y);
        final double θ = atan(x/(-y) ); //undefined if x=y=0
        final double L = (asin(cos_φ1xsin_F1/(ρ*n)) -s0)/n; //undefined if x=y=0  //eq.28-26 in Snyder with R=1
        
        //TODO ensure that λ0 will be added. ;  In eq. Snyder 28-23 : λ0 + θ/n
        dstPts[dstOff  ] =  θ ;//λ 
        dstPts[dstOff+1] = latitudeFromNewtonMethod(L); //φ
    }
    
    protected final double latitudeFromNewtonMethod(final double l){
        double dλ = -PI/2;
        double dλn = Double.MIN_VALUE;
        double A, A2, Δdλ;
        
        int count=0;
        int maxIteration = 100; //TODO : check the value.
                
        while((count==0) || dλ-dλn >= (0.1*PI/180)){ //TODO : check the condition. It is considered here that convergence is reached when improvement is lower than 0.1°.
            if(count >= maxIteration){
                throw new RuntimeException(Resources.format(Resources.Keys.NoConvergence));
            }
            dλn = dλ;

//            λt = l + p2_on_p1 * dλ_n; 
//            dλ = atan(tan(λt) / cos_i);

            A = tan(l + p2_on_p1*dλn) / cos_i;
            A2=A*A;
            Δdλ = -(dλn-atan(A)) / (1- (A2 + 1/cos2_i) * (p2_on_p1*cos_i/(A2+1)) );
            dλ = dλn + Δdλ ;

            count++;
        }
//        λt = L + p2_on_p1 * dλ;
        final double sin_dλ = sin(dλ);
        return -asin(sin_dλ * sin_i); 
    }
    
    
    public double getLatitudeLimit(){
        return latitudeLimit;
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
//    /**
//     * Radius of the circle radius of the circle to which groundtracks
//     * are tangent on the map.
//     * 
//     * @return radius ρs.
//     */
//    public double getRadiusOfTangencyCircle(){
//        return ρs;
//    }
}
