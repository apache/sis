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
 * Conversions from spherical coordinates to three-dimensional Cartesian coordinates.
 * This conversion assumes that there is no datum change. Axis order is:
 *
 * <ul>
 *   <li>Spherical longitude (θ), also noted Ω or λ.</li>
 *   <li>Spherical latitude (Ω), also noted θ or φ′ (confusing).</li>
 *   <li>Spherical radius (r), also noted <var>r</var> in ISO 19111.</li>
 * </ul>
 * <div class="note"><b>Note:</b>
 * the spherical latitude is related to geodetic latitude φ by {@literal Ω(φ) = atan((1-ℯ²)⋅tan(φ))}.</div>
 *
 * This order matches the {@link EllipsoidToCentricTransform} axis order.
 * It shall also match the order defined by {@code Normalizer} in {@link org.apache.sis.referencing.cs} package.
 * Note that this is <strong>not</strong> the convention used neither in physics (ISO 80000-2:2009) or in mathematics.
 *
 * <div class="note"><b>Relationship with the convention used in physics</b>
 * The ISO 80000-2 convention is (r,Ω,φ) where φ is like the spherical longitude, and Ω is measured from
 * the Z axis (North pole) instead than from the equator plane. The consequence in the formulas is that
 * {@code sin(Ω)} needs to be replaced by {@code cos(Ω)} and conversely.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see CartesianToSpherical
 * @see EllipsoidToCentricTransform
 * @see <a href="https://en.wikipedia.org/wiki/Spherical_coordinate_system">Spherical coordinate system on Wikipedia</a>
 */
final class SphericalToCartesian extends CoordinateSystemTransform implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8001536207920751506L;

    /**
     * The singleton instance expecting input coordinates in radians.
     * For the instance expecting input coordinates in degrees, use {@link #completeTransform()} instead.
     */
    static final SphericalToCartesian INSTANCE = new SphericalToCartesian();

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
    private SphericalToCartesian() {
        super("Spherical to Cartesian", 3);
        context.normalizeGeographicInputs(0);                   // Convert (θ,Ω) from degrees to radians.
    }

    /**
     * Returns the inverse of this transform.
     */
    @Override
    public MathTransform inverse() {
        return CartesianToSpherical.INSTANCE;
    }

    /**
     * Converts a single coordinate and optionally computes the derivative.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate)
    {
        final double θ = srcPts[srcOff  ];          // Spherical longitude
        final double Ω = srcPts[srcOff+1];          // Spherical latitude
        final double r = srcPts[srcOff+2];          // Spherical radius
        final double cosθ = cos(θ);
        final double sinθ = sin(θ);
        final double cosΩ = cos(Ω);
        final double sinΩ = sin(Ω);
        final double rsinΩ = r * sinΩ;
        final double rcosΩ = r * cosΩ;
        if (dstPts != null) {
            dstPts[dstOff  ] = rcosΩ * cosθ;        // X: Toward prime meridian
            dstPts[dstOff+1] = rcosΩ * sinθ;        // Y: Toward 90° east
            dstPts[dstOff+2] = rsinΩ;               // Z: Toward north pole
        }
        if (!derivate) {
            return null;
        }
        final double dX_dr = cosΩ * cosθ;
        final double dY_dr = cosΩ * sinθ;
        return new Matrix3(-r*dY_dr, -rsinΩ*cosθ, dX_dr,       // ∂X/∂θ, ∂X/∂Ω, ∂X/∂r
                            r*dX_dr, -rsinΩ*sinθ, dY_dr,       // ∂Y/∂θ, ∂Y/∂Ω, ∂Y/∂r
                                  0,  rcosΩ,      sinΩ);       // ∂Z/∂θ, ∂Z/∂Ω, ∂Z/∂r
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
            final double θ = srcPts[srcOff++];          // Spherical longitude
            final double Ω = srcPts[srcOff++];          // Spherical latitude
            final double r = srcPts[srcOff++];          // Spherical radius
            final double rcosΩ = r * cos(Ω);
            dstPts[dstOff++] = rcosΩ * cos(θ);          // X: Toward prime meridian
            dstPts[dstOff++] = rcosΩ * sin(θ);          // Y: Toward 90° east
            dstPts[dstOff++] = r * sin(Ω);              // Z: Toward north pole
            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /*
     * NOTE: we do not bother to override the methods expecting a 'float' array because those methods should
     *       be rarely invoked. Since there is usually LinearTransforms before and after this transform, the
     *       conversion between float and double will be handled by those LinearTransforms.  If nevertheless
     *       this SphericalToCartesian is at the beginning or the end of a transformation chain,
     *       the methods inherited from the subclass will work (but may be slightly slower).
     */
}
