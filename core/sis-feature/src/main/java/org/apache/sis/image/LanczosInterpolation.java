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

import java.util.Arrays;
import java.awt.Dimension;
import java.nio.DoubleBuffer;


/**
 * Lanczos interpolation of arbitrary size.
 * The kernel is:
 *
 * <blockquote>
 * <var>L</var>(<var>x</var>) = <var>a</var>⋅sin(π⋅<var>x</var>)⋅sin(π⋅<var>x</var>/<var>a</var>)/(π⋅<var>x</var>)²
 * for |<var>x</var>| ≤ lanczos window size
 * </blockquote>
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see <a href="https://en.wikipedia.org/wiki/Lanczos_resampling">Lanczos resampling on Wikipedia</a>
 *
 * @since 1.1
 * @module
 */
final class LanczosInterpolation implements Interpolation {
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
     * Applies Lanczos interpolation.
     */
    @Override
    public boolean interpolate(final DoubleBuffer source, final int numBands,
            final double xfrac, final double yfrac, final double[] writeTo, final int writeToOffset)
    {
        Arrays.fill(writeTo, writeToOffset, writeToOffset + numBands, 0);
        final double[] kx = new double[span];
        final double[] ky = new double[span];
        for (int i=0; i<span; i++) {
            final double offset = i - a;
            kx[i] = kernel(offset + xfrac);
            ky[i] = kernel(offset + yfrac);
        }
        source.mark();
        for (int y=0; y<span; y++) {
            for (int x=0; x<span; x++) {
                final double k = kx[x] * ky[y];
                for (int b=0; b<numBands; b++) {
                    writeTo[writeToOffset + b] += k * source.get();
                }
            }
        }
        source.reset();
        return true;
    }

    /**
     * Computes a value of the Lanczos reconstruction kernel L(x).
     */
    private double kernel(double x) {
        x *= Math.PI;
        return (x != 0) ? Math.sin(x) * Math.sin(x/a)*a / (x*x) : 1;
    }
}
