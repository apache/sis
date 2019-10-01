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

import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;

import static java.lang.Math.*;


/**
 * Special case of <cite>Satellite-Tracking</cite> projection when the standard parallels are opposite.
 *
 * @author  Matthieu Bastianelli (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class CylindricalSatelliteTracking extends ConicSatelliteTracking {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5972958777067525602L;

    /**
     * Create a Cylindrical Satellite Tracking Projection from the given parameters.
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
        final double cosφ1_dF1 = sqrt(cos2_φ1 - cos2_i) *cos_φ1 / (p2_on_p1 * cos2_φ1 - cos_i);

        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        normalize  .convertAfter (0, cos_φ1, null);  //For conic tracking

        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(1, cosφ1_dF1,     null);

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

        // compute an double array with {L} or {L, dL/dφ} if derivate recquired.
        final double[] vector_L = computeLanddLdφForDirectTransform(φ, derivate);

        if (dstPts != null) {
            dstPts[dstOff    ] = λ;   // In eq. Snyder 28-5 : R(λ - λ0) cos_φ1
            dstPts[dstOff + 1] = vector_L[0];   // In eq. Snyder 28-6 : R L cos(φ1)/F'1
        }

        /* =====================================================================
        * Uncomputed scale factors :
        *===========================
        * F' : tangente of the angle on the globe between the groundtrack and
        * the meridian at latitude φ
        * final double dF = (p2_on_p1 * cos2_φ - cos_i) / sqrt(cos2_φ - cos2_i);
        * k = cos_φ1/cos_φ;   // Parallel eq. Snyder 28-7
        * h = k* dF / cosφ1_dF1;    // Meridian eq. Snyder 28-8
        ===================================================================== */
        if (!derivate) {
            return null;
        }

        //=========================To check the resolution =====================
        final double dx_dλ = 1; //*R
        final double dx_dφ = 0;

        final double dy_dλ = 0;
        final double dy_dφ = vector_L[1]; //dL/dφ
        //======================================================================

        return new Matrix2(dx_dλ, dx_dφ,
                           dy_dλ, dy_dφ);
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
        final double y   = srcPts[srcOff + 1]; // In eq. Snyder 28-19 : y = yinit * cosφ1_dF1 / R . cos_φ1

        dstPts[dstOff  ] =  x;
        dstPts[dstOff+1] = latitudeFromNewtonMethod(y); //φ
    }
}
