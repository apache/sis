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
import org.apache.sis.parameter.Parameters;
import org.apache.sis.math.Fraction;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import static org.apache.sis.referencing.operation.provider.Robinson.*;


/**
 * <cite>Robinson</cite> projection.
 * This projection is different than other projections in that it computes
 * by interpolations of tabulated values instead of analytic function.
 * The table is indexed by latitude at a constant interval.
 *
 * <h2>Note on possible code reuse</h2>
 * While the current implementation supports only the Robinson projection,
 * it could be generalized to any projection using interpolations in a similar way.
 * For example, the "Natural Earth" projection was initially defined by interpolations.
 * See {@link Variant} for a note about how to generalize.
 *
 * <h2>References</h2>
 * <p>Snyder, J. P. (1990). <u>The Robinson projection: A computation algorithm.</u>
 * Cartography and Geographic Information Systems, 17 (4), p. 301-305.</p>
 *
 * <h3>Changes compared to the reference</h3>
 * The Snyder's article gives a program in the ANSI Fortran language. That program has been translated
 * to Java in this class and adapted for SIS architecture (constants moved to normalization matrices).
 * Robinson did not specify a particular interpolation method, but Snyder's program uses the Stirling's
 * central-difference formula, while PROJ uses cubic splices. Some other software use Aitken interpolation.
 * The Fortran program translated to Java did not converged well for the inverse projection of points near
 * a pole (maybe it is the reason why PROJ uses cubic splines instead). We modified the algorithm with the
 * use of derivative (∂y/∂φ) for faster convergence.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://en.wikipedia.org/wiki/Robinson_projection">Robinson projection on Wikipedia</a>
 */
public class Robinson extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2998244461334786203L;

    /**
     * The projection variants supported by the enclosing class.
     *
     * <h2>Future evolution</h2>
     * If there is a need to support other interpolated projection than Robinson, then the {@link #TABLE},
     * {@link #LAST_INDEX} and {@link #LATITUDE_INCREMENT} constants should move in this enumeration.
     */
    private enum Variant implements ProjectionVariant {
        /** The Robinson projection. */
        ROBINSON;

        /** Requests the use of authalic radius. */
        @Override public boolean useAuthalicRadius() {
            return true;
        }

        /** Requests the use of degrees. */
        @Override public boolean useRadians() {
            return false;
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
        return new Initializer(method, parameters, roles, Variant.ROBINSON);
    }

    /**
     * Increment in degrees between two rows of the interpolation table.
     *
     * @see #TABLE
     */
    private static final int LATITUDE_INCREMENT = 5;

    /**
     * Conversion factor from latitude in degrees to index in the interpolation table.
     */
    private static final Fraction TO_INDEX = new Fraction(1, LATITUDE_INCREMENT);

    /**
     * Multiplication factors of <var>x</var> and <var>y</var> axes after projection.
     */
    private static final Fraction XF = new Fraction( 8487, 10000),      // 0.8487
                                  YF = new Fraction(13523, 10000);      // 1.3523

    /**
     * Creates a Robinson projection from the given parameters.
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public Robinson(final OperationMethod method, final Parameters parameters) {
        super(initializer(method, parameters), null);
        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        normalize  .convertAfter (0, DoubleDouble.DEGREES_TO_RADIANS, null);
        normalize  .convertAfter (1, TO_INDEX, null);
        denormalize.convertBefore(0, XF, null);
        denormalize.convertBefore(1, YF, null);
    }

    /**
     * Interpolation table from 5°S to 90°N inclusive with a step of 5° of latitude.
     * The first column (XLR) is the ratio of the length of the parallel to the length of the equator.
     * The second column (PR) is proportional to the distance of from the equator to the parallel
     * divided by the equator length.
     */
    private static final double[] TABLE = {
        // XLR   PR
        0.9986, -0.0620,  // -5°
        1.0000,  0.0000,  //  0°
        0.9986,  0.0620,  //  5°
        0.9954,  0.1240,  // 10°
        0.9900,  0.1860,  // 15°
        0.9822,  0.2480,  // 20°
        0.9730,  0.3100,  // 25°
        0.9600,  0.3720,  // 30°
        0.9427,  0.4340,  // 35°
        0.9216,  0.4958,  // 40°
        0.8962,  0.5571,  // 45°
        0.8679,  0.6176,  // 50°
        0.8350,  0.6769,  // 55°
        0.7986,  0.7346,  // 60°
        0.7597,  0.7903,  // 65°
        0.7186,  0.8435,  // 70°
        0.6732,  0.8936,  // 75°
        0.6213,  0.9394,  // 80°
        0.5722,  0.9761,  // 85°
        0.5322,  1.0000   // 90°
    };

    /**
     * Highest valid value for the index of the latitude.
     * Assertion: {@code (LAST_INDEX << 1) + 5 == TABLE.length - 1}.
     */
    private static final int LAST_INDEX = 17;

    /**
     * Projects the specified (Λ,φ) coordinates and stores the (<var>x</var>,<var>y</var>) result in {@code dstPts}.
     * The units of measurement are implementation-specific (see super-class javadoc).
     * The results must be multiplied by the denormalization matrix before to get linear distances.
     *
     * <p>Robinson did not specify a particular interpolation method. This class uses the
     * Stirling's central-difference formula as published in Snyder (1990) article.</p>
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
        final double λ  = srcPts[srcOff  ];
        final double φm = srcPts[srcOff+1];     // In multiple of `LATITUDE_INCREMENT`.
        final double φa = abs(φm);
        int i = Math.min((int) φa, LAST_INDEX);
        final double p  = φa - i;
        double tb, t0, t1;

        tb = TABLE[i <<= 1];    // Value of XLR for the range of latitudes before current range.
        t0 = TABLE[i+2];        // Value of XLR for the lower bound of current range of latitudes.
        t1 = TABLE[i+4];        // Value of XLR for the upper bound of current range of latitudes.
        final double xp1 = t1 - tb;
        final double xp2 = p*(t1 - 2*t0 + tb);
        final double xr  = t0 + p*(xp1 + xp2)/2;

        tb = TABLE[i+1];        // Value of PR for the range of latitudes before current range.
        t0 = TABLE[i+3];        // Value of PR for the lower bound of current range of latitudes.
        t1 = TABLE[i+5];        // Value of PR for the upper bound of current range of latitudes.
        final double yp1 = t1 - tb;
        final double yp2 = p*(t1 - 2*t0 + tb);
        final double ya  = t0 + p*(yp1 + yp2)/2;

        if (dstPts != null) {
            dstPts[dstOff  ] = xr * λ;
            dstPts[dstOff+1] = copySign(ya, φm);
        }
        if (!derivate) return null;
        return new Matrix2(xr, (xp1/2 + xp2)*abs(λ),
                            0, (yp1/2 + yp2));
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * <p>Snyder's algorithm has been modified in this method with the use of derivative (∂y/∂φ)
     * for faster convergence. It is particularly important for points near the poles.</p>
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double x  = srcPts[srcOff  ];
        final double y  = srcPts[srcOff+1];
        final double ya = abs(y);
        int i = Math.min((int) (ya*(90/LATITUDE_INCREMENT)), LAST_INDEX);    // First estimation.
        double p;
        do {
            int ti = (i << 1) | 1;
            double ym = TABLE[ti  ];
            double y0 = TABLE[ti+2];
            double y1 = TABLE[ti+4];
            double u  = y1 - ym;
            double t  = 2*(ya - y0) / u;
            double c  = t*(y1 - 2*y0 + ym) / u;
            p = t*(1 - c*(1 - 2*c));
        } while (p < 0 && --i >= 0);      // Recompute if the first estimation was too high.
        i = max(i, 0);
        /*
         * Above loop computed an estimated position for the latitude band that contains φ.
         * Refine the result for the actual latitude value (not only the band containing it).
         */
        int nbIter = MAXIMUM_ITERATIONS;
        double φm = p + i;                                  // In multiple of `LATITUDE_INCREMENT`.
        do {
            i <<= 1;
            double tb, t0, t1;
            tb = TABLE[i+1];
            t0 = TABLE[i+3];
            t1 = TABLE[i+5];
            final double yp1 = t1 - tb;                     // Same formulas as the forward case.
            final double yp2 = p*(t1 - 2*t0 + tb);
            final double err = t0 + p*(yp1 + yp2)/2 - ya;   // Difference between interpolated y and desired y.
            final double dy  = (yp1/2 + yp2);               // Derivative ∂y/∂φ (last term in the Jacobian matrix).
            φm -= err / dy;                                 // Convert the error in y to an error in φ.
            if (!(abs(err) > ITERATION_TOLERANCE)) {        // Use `!` for accepting NaN.
                tb = TABLE[i+0];
                t0 = TABLE[i+2];
                t1 = TABLE[i+4];
                dstPts[dstOff] = x / (t0 + p*(t1 - tb + p*(t1 - 2*t0 + tb))/2);
                dstPts[dstOff+1] = copySign(φm, y);
                return;
            }
            i = Math.min((int) φm, LAST_INDEX);
            p = φm - i;
        } while (--nbIter >= 0);
        throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
    }
}
