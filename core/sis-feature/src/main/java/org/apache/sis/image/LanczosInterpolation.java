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
package org.apache.sis.image;

import java.awt.Dimension;
import java.nio.DoubleBuffer;


/**
 * Lanczos interpolation of arbitrary size. This interpolation is good for photographic images but
 * not recommended for images that may contain NaN values. The Lanczos reconstruction kernel is:
 *
 * <blockquote>
 * <var>L</var>(<var>x</var>) = <var>a</var>⋅sin(π⋅<var>x</var>)⋅sin(π⋅<var>x</var>/<var>a</var>)/(π⋅<var>x</var>)²
 * for |<var>x</var>| ≤ lanczos window size
 * </blockquote>
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see <a href="https://en.wikipedia.org/wiki/Lanczos_resampling">Lanczos resampling on Wikipedia</a>
 *
 * @since 1.1
 * @module
 */
final class LanczosInterpolation extends Interpolation {
    /**
     * The Lanczos window size. This is denoted <var>a</var> in this class javadoc.
     */
    private final double a;

    /**
     * Width of the interpolation support region.
     */
    private final int span;

    /**
     * Creates a new interpolation.
     *
     * @param  a  the Lanczos window size.
     */
    LanczosInterpolation(final int a) {
        this.a = a;
        span = 2*a;
    }

    /**
     * Interpolation name for debugging purpose.
     */
    @Override
    public String toString() {
        return "LANCZOS";
    }

    /**
     * Size of the area over which to provide values.
     */
    @Override
    public Dimension getSupportSize() {
        return new Dimension(span, span);
    }

    /**
     * Applies Lanczos interpolation. Interpolation results may be outside
     * the expected range of pixel values; caller may need to clamp.
     *
     * @param  xfrac  the X subsample position, in the [0 … 1) range except on image border.
     * @param  yfrac  the Y subsample position, in the [0 … 1) range except on image border.
     */
    @Override
    public void interpolate(final DoubleBuffer source, final int numBands,
            final double xfrac, final double yfrac, final double[] writeTo, final int writeToOffset)
    {
        source.mark();
        /*
         * Lanczos kernel coefficients, pre-computed because reused many times. We store them in a temporary array
         * created in this method; we do not reuse a cached array because this method will be invoked concurrently
         * in many threads. In the particular case of the last dimension (y in our case), the coefficients will be
         * used only once, so we can compute them on-the-fly and avoid the creation of one array.
         */
        final double[] kx = new double[span];
        double ky;
        /*
         * The kernel values could be computed in two lines such as:
         *
         *     x = (xfrac - (i+1 - a)) * PI;
         *     kx[i] = sin(x)/x * sin(x/a)/(x/a)          for x ≠ 0
         *
         * But we can take advantage that, for integer values of i,
         * sin(x) will always have the same value except for the sign.
         */
        double siny;
        {   // Block for keeping variables in local scope.
            double sinx;
            double xp = (xfrac - (1 - a)) * Math.PI;
            double yp = (yfrac - (1 - a)) * Math.PI;
            sinx = Math.sin(xp);
            siny = Math.sin(yp);
            kx[0] = kernel(xp, sinx);
            ky    = kernel(yp, siny);
            /*
             * Multiply pixel values by Lanczos kernel coefficients. This is the first
             * iteration of the loop after this block, unrolled for using the existing
             * `ky` value and for overwritten existing values in the destination array.
             */
            double k = kx[0] * ky;
            for (int b=0; b<numBands; b++) {
                writeTo[writeToOffset + b] = k * source.get();
            }
            for (int i=1; i<span;) {
                /*
                 * kernel(x) must be invoked with −a ≤ x ≤ +a.
                 *
                 *   Given   x  =  xfrac − (i+1 - a)
                 *   with    i  ∈  [0 … 2a-1]
                 *   we get  x  ∈  [xfrac − a  …  xfrac + a-1]
                 *
                 * The condition is met for xfrac ∈ [0 … 1].
                 */
                kx[i] = k = kernel((xfrac - (++i - a)) * Math.PI, sinx = -sinx);
                /*
                 * Multiply pixel values by Lanczos kernel coefficients
                 * (continuing the unrollled first iteration over `j`).
                 */
                k *= ky;
                for (int b=0; b<numBands; b++) {
                    writeTo[writeToOffset + b] += k * source.get();
                }
            }
        }
        /*
         * Multiply pixel values by Lanczos kernel coefficients,
         * continuing the iteration for all rows after `j = 0`.
         */
        for (int j=1; j<span;) {
            ky = kernel((yfrac - (++j - a)) * Math.PI, siny = -siny);
            for (int i=0; i<span; i++) {
                final double k = kx[i] * ky;
                for (int b=0; b<numBands; b++) {
                    writeTo[writeToOffset + b] += k * source.get();
                }
            }
        }
        source.reset();
    }

    /**
     * Computes a value of the Lanczos reconstruction kernel L(x).
     * This is a component of Lanczos filter's kernel in two dimensions, which is L(x,y) = L(x)L(y).
     * In this implementation, the given <var>x</var> value must be pre-multiplied by {@link Math#PI}.
     *
     * <div class="note"><b>Note:</b> the multiplication by π is a normalization that causes
     * the definite integral of the function over the real numbers to equal 1.</div>
     *
     * @param  x     must be between −{@link #a}⋅π and +{@link #a}⋅π inclusive.
     * @param  sinx  value of {@code Math.sin(x)}.
     */
    private double kernel(double x, final double sinx) {
        final double y = sinx * Math.sin(x/a)*a / (x*x);
        return (y <= 1) ? y : 1;    // Do not use Math.min(…) because we want to replace NaN by 1.
    }
}
