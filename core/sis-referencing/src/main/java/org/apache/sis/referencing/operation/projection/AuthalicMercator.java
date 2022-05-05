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
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix2;

import static java.lang.Math.*;
import static org.apache.sis.math.MathFunctions.atanh;


/**
 * Spherical Mercator projection after conversion of geodetic latitudes to authalic latitudes.
 * This is used for implementation of ESRI "Mercator Auxiliary Sphere type 3" projection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class AuthalicMercator extends AuthalicConversion {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3546152970105798436L;

    /**
     * The type value for this map projection in ESRI <cite>Auxiliary sphere type</cite> parameter.
     */
    static final int TYPE = 3;

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer.
     *
     * @param other  the other projection from which to compute parameters.
     */
    AuthalicMercator(final Mercator other) {
        super(null, other);
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}
     * (linear distance on a unit sphere). In addition, opportunistically computes the projection derivative
     * if {@code derivate} is {@code true}.
     *
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate)
    {
        final double φ = srcPts[srcOff+1];
        final double sinβ = sinβ(sin(φ));
        if (dstPts != null) {
            dstPts[dstOff  ] = srcPts[srcOff];
            dstPts[dstOff+1] = atanh(sinβ);
        }
        return derivate ? new Matrix2(1, 0, 0, 1/sqrt(1 - sinβ*sinβ)) : null;
    }

    /**
     * Converts a list of coordinate points. This method performs the same calculation than above
     * {@link #transform(double[], int, double[], int, boolean)} method, but is overridden for efficiency.
     *
     * @throws TransformException if a point can not be converted.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        if (srcPts != dstPts || srcOff != dstOff) {
            super.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        } else {
            dstOff--;
            while (--numPts >= 0) {
                final double φ = dstPts[dstOff += DIMENSION];       // Same as srcPts[srcOff + 1].
                dstPts[dstOff] = atanh(sinβ(sin(φ)));
            }
        }
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double y = srcPts[srcOff+1];          // Must be before writing x.
        dstPts[dstOff  ] = srcPts[srcOff];          // Must be before writing y.
        dstPts[dstOff+1] = φ(tanh(y));
    }
}
