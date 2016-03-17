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
import org.apache.sis.internal.util.DoubleDouble;

import static java.lang.Math.*;


/**
 * Conversions from three-dimensional Cartesian coordinates to spherical coordinates.
 * This conversion assumes that there is no datum change.
 *
 * <p>See {@link CylindricalToCartesian} for explanation on axes convention.
 * Axis order shall match the order defined by {@code Normalizer} in {@link org.apache.sis.referencing.cs} package.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class CartesianToCylindrical extends CoordinateSystemTransform implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2619855017534519721L;

    /**
     * The singleton instance computing output coordinates are in radians.
     * For the instance computing output coordinates in degrees, use {@link #completeTransform()} instead.
     */
    static final CartesianToCylindrical INSTANCE = new CartesianToCylindrical();

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
    private CartesianToCylindrical() {
        super("Cartesian to cylindrical", 3);
        context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION)
               .convertAfter(1, DoubleDouble.createRadiansToDegrees(), null);
    }

    /**
     * Returns the inverse of this transform.
     */
    @Override
    public MathTransform inverse() {
        return CylindricalToCartesian.INSTANCE;
    }

    /**
     * Converts a single coordinate and optionally computes the derivative.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate)
    {
        final double x  = srcPts[srcOff  ];
        final double y  = srcPts[srcOff+1];
        final double z  = srcPts[srcOff+2];
        final double r  = hypot(x, y);
        if (dstPts != null) {
            dstPts[dstOff  ] = r;
            dstPts[dstOff+1] = atan2(y, x);
            dstPts[dstOff+2] = z;
        }
        if (!derivate) {
            return null;
        }
        final double r2 = r*r;
        return new Matrix3(x/r,   y/r,   0,
                          -y/r2,  x/r2,  0,
                           0,     0,     1);
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
            final double x  = srcPts[srcOff++];
            final double y  = srcPts[srcOff++];
            final double z  = srcPts[srcOff++];
            dstPts[dstOff++] = hypot(x, y);
            dstPts[dstOff++] = atan2(y, x);
            dstPts[dstOff++] = z;
            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /*
     * NOTE: we do not bother to override the methods expecting a 'float' array because those methods should
     *       be rarely invoked. Since there is usually LinearTransforms before and after this transform, the
     *       conversion between float and double will be handled by those LinearTransforms.  If nevertheless
     *       this CartesianToCylindrical is at the beginning or the end of a transformation chain,
     *       the methods inherited from the subclass will work (but may be slightly slower).
     */
}
