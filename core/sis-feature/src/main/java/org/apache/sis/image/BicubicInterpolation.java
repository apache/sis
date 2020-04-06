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
 * Bicubic interpolation.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see <a href="https://en.wikipedia.org/wiki/Bicubic_interpolation">Bicubic interpolation on Wikipedia</a>
 *
 * @since 1.1
 * @module
 */
final class BicubicInterpolation implements Interpolation {
    /**
     * The number of pixel required horizontally and vertically for performing this interpolation.
     */
    static final int SUPPORT = 4;

    /**
     * Creates a new interpolation.
     */
    BicubicInterpolation() {
    }

    /**
     * Interpolation name for debugging purpose.
     */
    @Override
    public String toString() {
        return "BICUBIC";
    }

    /**
     * Size of the area over which to provide values.
     */
    @Override
    public Dimension getSupportSize() {
        return new Dimension(SUPPORT, SUPPORT);
    }

    /**
     * Applies bicubic interpolation on a 4×4 window.
     */
    @Override
    public boolean interpolate(final DoubleBuffer source, final int numBands,
            double xfrac, double yfrac, final double[] writeTo, int writeToOffset)
    {
        xfrac++;    // TODO: update coefficients in `interpolate(…)` method instead.
        yfrac++;
        final double[] y = new double[SUPPORT];
        for (int b=0; b<numBands; b++) {
            int p = source.position() + b;
            for (int j=0; j<SUPPORT; j++) {
                y[j] = interpolate(xfrac, source.get(p            ),
                                          source.get(p += numBands),
                                          source.get(p += numBands),
                                          source.get(p += numBands));
                p += numBands;
            }
            writeTo[writeToOffset++] = interpolate(yfrac, y[0], y[1], y[2], y[3]);
        }
        return true;
    }

    /**
     * Applies a bicubic interpolation on a row or a column.
     */
    private static double interpolate(final double t, final double s0, final double s1, final double s2, final double s3) {
        final double a1 = ( 1d/3)*s3 + (-3d/2)*s2 + ( 3d  )*s1 + (-11d/6)*s0;
        final double a2 = (-1d/2)*s3 + ( 2d  )*s2 + (-5d/2)*s1 +          s0;
        final double a3 = ( 1d/6)*s3 + (-1d/2)*s2 + ( 1d/2)*s1 + ( -1d/6)*s0;
        return s0 + (a1 + (a2 + a3*t)*t)*t;
    }
}
