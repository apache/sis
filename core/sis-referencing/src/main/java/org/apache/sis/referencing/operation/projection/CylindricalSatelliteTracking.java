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

import org.opengis.referencing.operation.Matrix;

import org.apache.sis.parameter.Parameters;
import org.opengis.referencing.operation.OperationMethod;

import static java.lang.Math.*;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;

/**
 * <cite>Cylindrical Satellite-Tracking projections</cite>.
 *
 * <cite>
 * - All groundtracks
 * for satellites orbiting the Earth with the same orbital parameters are shown
 * as straight lines on the map.
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
public class CylindricalSatelliteTracking extends ConicSatelliteTracking {


    /**
     * F1' in Snyder : tangente of the angle on both the globe and on the map between
     * the groundtrack and the meridian at latitude φ1.
     */
    final double dF1;

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
    public CylindricalSatelliteTracking(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    private CylindricalSatelliteTracking(final Initializer initializer) {
        super(initializer);

        final double cos2_φ1 = cos_φ1 * cos_φ1;
        dF1 = (p2_on_p1 * cos2_φ1 - cos_i) / sqrt(cos2_φ1 - cos2_i);

        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
//        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);

        normalize  .convertAfter (0, cos_φ1, null);  //For conic tracking

//        denormalize.convertBefore(0, 1/PI,     null);
//        denormalize.convertBefore(1, , null);

    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}
     * (linear distance on a unit sphere). In addition, opportunistically computes the projection derivative
     * if {@code derivate} is {@code true}.
     *
     * <cite> The Yaxis lies along the central meridian λ0, y increasing northerly,
     * and X axis intersects perpendicularly at O_PARALLEL φ0, x increasing easterly.
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

        final double λ = srcPts[srcOff];
        final double φ = srcPts[srcOff + 1];

        // TODO : check the condition and the thrown exception.
        if (abs(φ) > PI / 2 - abs(PI / 2 - i)) {  // Exceed tracking limit
            throw new ProjectionException(Resources.format(Resources.Keys.CanNotTransformCoordinates_2));
        }

//        final double cos_φ  = cos(φ);
//        final double cos2_φ = cos_φ * cos_φ;
        final double sin_φ  = sin(φ);

        final double dλ     = -asin(sin_φ / sin_i);
        final double λt     = atan(tan(dλ) * cos_i);
        final double L      = λt - p2_on_p1 * dλ;

        if (dstPts != null) {
            dstPts[dstOff    ] = λ;   // In eq. Snyder 28-5 : R(λ - λ0) cos_φ1
            dstPts[dstOff + 1] = L * cos_φ1 / dF1;  // In eq. Snyder 28-6 : R L cos(φ1)/F'1
        }

        /* =====================================================================
        * Uncomputed scale factors :
        *===========================
        * F' : tangente of the angle on the globe between the groundtrack and
        * the meridian at latitude φ
        * final double dF = (p2_on_p1 * cos2_φ - cos_i) / sqrt(cos2_φ - cos2_i);
        * k = cos_φ1/cos_φ;   // Parallel eq. Snyder 28-7
        * h = k* dF / dF1;    // Meridian eq. Snyder 28-8
        ===================================================================== */
        if (!derivate) {
            return null;
        }

//        final double dx_dλ = cos_φ1; //*R
//        final double dx_dφ = 0;
//
//        final double dy_dλ = 0;
//        final double dy_dφ =

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

        final double L   = y * dF1 / cos_φ1; // In eq. Snyder 28-19 : L = y * dF1 / R . cos_φ1

        dstPts[dstOff  ] =  x;
        dstPts[dstOff+1] = latitudeFromNewtonMethod(L); //φ
    }

}
