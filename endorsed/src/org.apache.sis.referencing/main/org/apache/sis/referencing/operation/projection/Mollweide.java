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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.util.Workaround;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.internal.Resources;
import static org.apache.sis.math.MathFunctions.SQRT_2;
import static org.apache.sis.referencing.operation.provider.Mollweide.*;


/**
 * <cite>Mollweide</cite> projection.
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Mollweide_projection">Mollweide projection on Wikipedia</a></li>
 *   <li><a href="https://mathworld.wolfram.com/MollweideProjection.html">Mollweide projection on MathWorld</a></li>
 * </ul>
 *
 * @todo This projection is not {@link org.apache.sis.math.FunctionProperty#SURJECTIVE surjective}.
 *       Consequently, {@link org.apache.sis.referencing.CRS#suggestCommonTarget CRS.suggestCommonTarget(…)}
 *       may not work correctly if a CRS uses this projection.
 *       See <a href="https://issues.apache.org/jira/browse/SIS-427">SIS-427</a>.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class Mollweide extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 712275000459795291L;

    /**
     * Allowed projection variants. Current implementation supports only spherical formulas.
     * We do not yet use this enumeration for detecting variants from the operation name.
     */
    private enum Variant implements ProjectionVariant {
        /** The spherical case. */
        SPHERICAL;

        /** Requests the use of authalic radius. */
        @Override public boolean useAuthalicRadius() {
            return true;
        }
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="8", fixed="25")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, CENTRAL_MERIDIAN);
        roles.put(ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);
        return new Initializer(method, parameters, roles, Variant.SPHERICAL);
    }

    /**
     * Creates a Mollweide projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Mollweide</q>, also known as
     *       <q>Homalographic</q> or <q>Homolographic</q>.</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public Mollweide(final OperationMethod method, final Parameters parameters) {
        super(initializer(method, parameters), null);
        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        normalize  .convertAfter (0, 2*SQRT_2, null);
        denormalize.convertBefore(0, 1/PI,     null);
        denormalize.convertBefore(1, SQRT_2,   null);
    }

    /**
     * Projects the specified (Λ,φ) coordinates and stores the (<var>x</var>,<var>y</var>) result in {@code dstPts}.
     * The units of measurement are implementation-specific (see super-class javadoc).
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
        final double λ = srcPts[srcOff];            // Scaled by 2√2.
        final double φ = srcPts[srcOff + 1];
        final double sinφ = sin(φ);
        double θp = 2 * asin(φ * (2/PI));           // θ′ in Snyder formulas.
        /*
         * If sin(φ) is 1 or -1 we are on a pole.
         * Iteration would produce NaN values.
         */
        if (abs(sinφ) != 1) {
            final double πsinφ = PI * sinφ;
            int nbIter = MAXIMUM_ITERATIONS;
            double Δθ;
            do {
                if (--nbIter < 0) {
                    throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
                }
                Δθ = (θp + sin(θp) - πsinφ) / (1 + cos(θp));
                θp -= Δθ;
            } while (abs(Δθ) > 2*ITERATION_TOLERANCE);          // *2 because θ′ is twice the desired angle.
        }
        final double θ = θp * 0.5;
        final double x = cos(θ) * λ;
        final double y = sin(θ);
        if (dstPts != null) {
            dstPts[dstOff    ] = x;
            dstPts[dstOff + 1] = y;
        }
        if (!derivate) {
            return null;
        }
        try {
            // TODO: see https://issues.apache.org/jira/browse/SIS-428
            return Matrices.inverse(inverseDerivate(x, y, θp, sinφ));
        } catch (NoninvertibleMatrixException e) {
            throw new ProjectionException(e);
        }
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
    {
        final double x  = srcPts[srcOff];
        final double y  = srcPts[srcOff + 1];
        final double θ  = asin(y);
        final double θp = 2 * θ;
        final double φ  = asin((θp + sin(θp)) * (1/PI));
        double λ = x / cos(θ);
        if (abs(λ) > 2*SQRT_2*PI) {
            λ = Double.NaN;
        }
        dstPts[dstOff]   = λ;
        dstPts[dstOff+1] = φ;
    }

    /**
     * Computes the inverse of projection derivative.
     *
     * @param  θp    {@code 2 * asin(y)}
     * @param  sinφ  {@code (θp + sin(θp)) / PI}
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-428">SIS-428</a>
     */
    private static Matrix inverseDerivate(final double x, final double y, final double θp, final double sinφ) {
        final double cosφ  = sqrt(1 - (sinφ * sinφ));
        final double ym1   = 1 - y*y;
        final double dλ_dx = 1 / sqrt(ym1);
        final double dλ_dy = (x*y) * dλ_dx / ym1;
        final double dφ_dy = 2*dλ_dx*(1 + cos(θp)) / (PI*cosφ);
        return new Matrix2(dλ_dx, dλ_dy, 0, dφ_dy);
    }
}
