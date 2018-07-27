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

import static java.lang.Math.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;

/**
 * <cite>Mollweide</cite> projection.
 * The Mollweide projection do not preserve angles,surfaces
 *
 * TODO : this transform causes issues with large envelopes, we need to have the
 *        information about tranform capability (bijective,subjective,...)
 *        to correctly choose an appropriate common CRS for intersection.
 *
 * @see <a href="http://mathworld.wolfram.com/MollweideProjection.html">Mathworld formulas</a>
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
public class Mollweide extends NormalizedProjection{

    private static final Map<ParameterRole, ParameterDescriptor<? extends Number>> ROLES = new HashMap<>();
    static {
        ROLES.put(ParameterRole.CENTRAL_MERIDIAN, org.apache.sis.internal.referencing.provider.Mollweide.CENTRAL_MERIDIAN);
        ROLES.put(ParameterRole.FALSE_EASTING, org.apache.sis.internal.referencing.provider.Mollweide.FALSE_EASTING);
        ROLES.put(ParameterRole.FALSE_NORTHING, org.apache.sis.internal.referencing.provider.Mollweide.FALSE_NORTHING);
    }

    private static final double SR2 = sqrt(2);
    private static final double LAMDA_LIMIT = 2*SR2*PI;

    /**
     * Constructs a new map projection from the supplied parameters.
     *
     * @param parameters The parameters of the projection to be created.
     */
    public Mollweide(final OperationMethod method, final Parameters parameters) {
        super(method, parameters, ROLES);
        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        normalize.convertBefore(0, 2*SR2, null);
        denormalize.convertBefore(0, 1/PI, null);
        denormalize.convertBefore(1, SR2, null);
    }

    @Override
    public Matrix transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, boolean derivate) throws ProjectionException {
        if (derivate) {
            //TODO
            throw new ProjectionException("Derivation not supported");
        }

        final double λ = srcPts[srcOff]; //longitude
        final double φ = srcPts[srcOff + 1]; //latitude
        double primeθ = 2 * asin( (2 * φ) / PI );

        final double sinφ = sin(φ);
        /*
        if sinφ is 1 or -1 we are on a pole.
        iteration would produce NaN values.
        */
        if (abs(sinφ) != 1) {
            final double pisinφ = PI * sinφ;
            int nbIte = MAXIMUM_ITERATIONS;
            double deltaθ = Double.MAX_VALUE;
            do {
                if (--nbIte < 0) {
                    throw new ProjectionException("Operation does not converge");
                }
                deltaθ = - (primeθ + sin(primeθ) - pisinφ) / (1 + cos(primeθ));
                primeθ += deltaθ;
            } while (abs(deltaθ) > ITERATION_TOLERANCE);
        }
        final double θ = primeθ * 0.5;

        final double x = λ * cos(θ);
        final double y = sin(θ);

        dstPts[dstOff    ] = x;
        dstPts[dstOff + 1] = y;

        Matrix matrix = null;
        return matrix;
    }

    @Override
    protected void inverseTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff) throws ProjectionException {

        final double x = srcPts[srcOff];
        final double y = srcPts[srcOff + 1];
        final double θ = asin(y);

        final double θθ = 2 * θ;
        final double φ = asin( (θθ + sin(θθ)) / PI );
        double λ = x / cos(θ);

        if (abs(λ) > LAMDA_LIMIT) {
            λ = Double.NaN;
        }

        dstPts[dstOff] = λ;
        dstPts[dstOff+1] = φ;
    }

}
