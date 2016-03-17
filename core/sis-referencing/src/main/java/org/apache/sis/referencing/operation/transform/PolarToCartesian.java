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
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.internal.util.DoubleDouble;

import static java.lang.Math.*;


/**
 * Conversions from polar coordinates to two-dimensional Cartesian coordinates.
 * This conversion assumes that there is no datum change. Source axis order is:
 *
 * <ul>
 *   <li>Radius (r)</li>
 *   <li>Angle  (θ)</li>
 *   <li>Height (z) in the cylindrical case (see below)</li>
 * </ul>
 *
 * Target axis order is:
 *
 * <ul>
 *   <li><var>x</var> in the direction of θ = 0°</li>
 *   <li><var>y</var> in the direction of θ = 90°</li>
 *   <li><var>z</var> in the same direction than the source (cylindrical case only - see below)</li>
 * </ul>
 *
 * Axis order shall match the order defined by {@code Normalizer} in {@link org.apache.sis.referencing.cs} package.
 *
 * <div class="note"><b>Note:</b>
 * We do not provide explicit {@code CylindricalToCartesian} implementation.  Instead, the cylindrical case is
 * implemented by the polar case with a {@link PassThroughTransform} for the height. This allows Apache SIS to
 * use the optimization implemented by {@code PassThroughTransform} when for example a concatenated transform
 * is dropping the <var>z</var> axis.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class PolarToCartesian extends CoordinateSystemTransform implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8026743046661603837L;

    /**
     * The singleton instance expecting input coordinates in radians.
     * For the instance expecting input coordinates in degrees, use {@link #completeTransform()} instead.
     */
    static final PolarToCartesian INSTANCE = new PolarToCartesian();

    /**
     * Returns the singleton instance on deserialization.
     */
    private Object readResolve() {
        return INSTANCE;
    }

    /**
     * Creates the singleton instance.
     * Input coordinates are in radians.
     */
    private PolarToCartesian() {
        super("Polar to Cartesian", 2);
        context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION)
               .convertBefore(1, DoubleDouble.createDegreesToRadians(), null);
    }

    /**
     * Returns the inverse of this transform.
     */
    @Override
    public MathTransform inverse() {
        return CartesianToPolar.INSTANCE;
    }

    /**
     * Converts a single coordinate and optionally computes the derivative.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate)
    {
        final double r = srcPts[srcOff  ];
        final double θ = srcPts[srcOff+1];
        final double cosθ = cos(θ);
        final double sinθ = sin(θ);
        if (dstPts != null) {
            dstPts[dstOff  ] = r*cosθ;
            dstPts[dstOff+1] = r*sinθ;
        }
        if (!derivate) {
            return null;
        }
        return new Matrix2(cosθ, -r*sinθ,
                           sinθ,  r*cosθ);
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
            switch (IterationStrategy.suggest(srcOff, 2, dstOff, 2, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += 2 * (numPts - 1);
                    dstOff += 2 * (numPts - 1);
                    srcInc = -4;
                    dstInc = -4;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*2);
                    srcOff = 0;
                    break;
                }
            }
        }
        while (--numPts >= 0) {
            final double r = srcPts[srcOff++];
            final double θ = srcPts[srcOff++];
            dstPts[dstOff++] = r*cos(θ);
            dstPts[dstOff++] = r*sin(θ);
            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /*
     * NOTE: we do not bother to override the methods expecting a 'float' array because those methods should
     *       be rarely invoked. Since there is usually LinearTransforms before and after this transform, the
     *       conversion between float and double will be handled by those LinearTransforms.  If nevertheless
     *       this PolarToCartesian is at the beginning or the end of a transformation chain,
     *       the methods inherited from the subclass will work (but may be slightly slower).
     */
}
