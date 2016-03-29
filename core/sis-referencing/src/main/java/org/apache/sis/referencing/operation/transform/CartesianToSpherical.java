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
package org.apache.sis.referencing.operation.transform;

import java.util.Arrays;
import java.io.Serializable;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.matrix.Matrix3;

import static java.lang.Math.*;


/**
 * Conversions from three-dimensional Cartesian coordinates to spherical coordinates.
 * This conversion assumes that there is no datum change.
 *
 * <p>See {@link SphericalToCartesian} for explanation on axes convention.
 * Axis order shall match the order defined by {@code Normalizer} in {@link org.apache.sis.referencing.cs} package.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class CartesianToSpherical extends CoordinateSystemTransform implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7174557821232512348L;

    /**
     * The singleton instance computing output coordinates are in radians.
     * For the instance computing output coordinates in degrees, use {@link #completeTransform()} instead.
     */
    static final CartesianToSpherical INSTANCE = new CartesianToSpherical();

    /**
     * Returns the singleton instance on deserialization.
     */
    private Object readResolve() {
        return INSTANCE;
    }

    /**
     * Creates the singleton instance.
     * Output coordinates are in radians.
     */
    private CartesianToSpherical() {
        super("Cartesian to spherical", 3);
        context.denormalizeGeographicOutputs(0);                // Convert (θ,Ω) from radians to degrees.
    }

    /**
     * Returns the inverse of this transform.
     */
    @Override
    public MathTransform inverse() {
        return SphericalToCartesian.INSTANCE;
    }

    /**
     * Converts a single coordinate and optionally computes the derivative.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate)
    {
        final double X  = srcPts[srcOff  ];
        final double Y  = srcPts[srcOff+1];
        final double Z  = srcPts[srcOff+2];
        final double ρ2 = X*X + Y*Y;
        final double r2 = Z*Z + ρ2;
        final double r  = sqrt(r2);
        if (dstPts != null) {
            dstPts[dstOff  ] = atan2(Y, X);                     // Spherical longitude (θ)
            dstPts[dstOff+1] = (r == 0) ? Z : asin(Z / r);      // Spherical latitude  (Ω). If (X,Y,Z) is (0,0,0) take the sign of Z.
            dstPts[dstOff+2] = r;
        }
        if (!derivate) {
            return null;
        }
        final double d = r2 * sqrt(r2 - Z*Z);
        return new Matrix3(-Y/ρ2,   X/ρ2,     0,        // ∂θ/∂X, ∂θ/∂Y, ∂θ/∂Z
                           -X*Z/d, -Y*Z/d, ρ2/d,        // ∂Ω/∂X, ∂Ω/∂Y, ∂Ω/∂Z
                            X/r,    Y/r,   Z/r);        // ∂r/∂X, ∂r/∂Y, ∂r/∂Z
    }

    /**
     * Converts an array of coordinates.
     * This method performs the same conversion than {@link #transform(double[], int, double[], int, boolean)},
     * but the formulas are repeated here for performance reasons.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        int srcInc = 0;
        int dstInc = 0;
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, 3, dstOff, 3, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += 3 * (numPts - 1);
                    dstOff += 3 * (numPts - 1);
                    srcInc = -6;
                    dstInc = -6;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*3);
                    srcOff = 0;
                    break;
                }
            }
        }
        while (--numPts >= 0) {
            final double X  = srcPts[srcOff++];
            final double Y  = srcPts[srcOff++];
            final double Z  = srcPts[srcOff++];
            final double r  = sqrt(X*X + Y*Y + Z*Z);
            dstPts[dstOff++] = atan2(Y, X);                     // Spherical longitude (θ)
            dstPts[dstOff++] = (r == 0) ? Z : asin(Z / r);      // Spherical latitude  (Ω). If (X,Y,Z) is (0,0,0) take the sign of Z.
            dstPts[dstOff++] = r;
            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /*
     * NOTE: we do not bother to override the methods expecting a 'float' array because those methods should
     *       be rarely invoked. Since there is usually LinearTransforms before and after this transform, the
     *       conversion between float and double will be handled by those LinearTransforms.  If nevertheless
     *       this CartesianToSpherical is at the beginning or the end of a transformation chain,
     *       the methods inherited from the subclass will work (but may be slightly slower).
     */
}
