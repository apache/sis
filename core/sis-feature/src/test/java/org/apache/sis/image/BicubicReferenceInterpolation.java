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

import static org.apache.sis.image.BicubicInterpolation.SUPPORT;


/**
 * A {@link BicubicInterpolation} to be used as a reference implementation.
 * This class implements the bicubic convolution algorithm as documented on
 * <a href="https://en.wikipedia.org/wiki/Bicubic_interpolation#Bicubic_convolution_algorithm">Wikipedia</a>,
 * also found in Java Advanced Imaging (JAI) as below:
 *
 * {@preformat text
 *   W(x)  =  (a+2)|x|³ − (a+3)|x|²         + 1       for 0 ≤ |x| ≤ 1
 *   W(x)  =      a|x|³ −    5a|x|² + 8a|x| − 4a      for 1 < |x| < 2
 *   W(x)  =  0                                       otherwise
 * }
 *
 * The <var>a</var> value is typically −0.5 or −1, as recommended by Rifman and Keys respectively (Reference:
 * Digital Image Warping, George Wolberg, 1990, pp 129-131, IEEE Computer Society Press, ISBN 0-8186-8944-7).
 * The −1 value often (not always) produces sharper results than the −0.5 value.
 *
 * <p>The {@link BicubicInterpolation} class develops coefficients for the <var>a</var> = −0.5 case.
 * This class can be used for comparing {@link BicubicInterpolation} results with results calculated
 * from original formulas.</p>
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class BicubicReferenceInterpolation implements Interpolation {
    /**
     * The <var>a</var> coefficient used in bicubic convolution algorithm.
     * Typical values are −0.5, −0.75 or −1.
     */
    private final double a;

    /**
     * Creates a new interpolation.
     */
    BicubicReferenceInterpolation(final double a) {
        this.a = a;
    }

    /**
     * Interpolation name for debugging purpose.
     */
    @Override
    public String toString() {
        return "BicubicReference[a=" + a + ']';
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
            final double xfrac, final double yfrac, final double[] writeTo, int writeToOffset)
    {
        for (int b=0; b<numBands; b++) {
            double sum = 0;
            int p = source.position() + b;
            for (int j=0; j<SUPPORT; j++) {
                double s = 0;
                for (int i=0; i<SUPPORT; i++) {
                    s += convolutionValue(xfrac - i) * source.get(p);
                    p += numBands;
                }
                sum += convolutionValue(yfrac - j) * s;
            }
            writeTo[writeToOffset++] = sum;
        }
        return true;
    }

    /**
     * Compute a value of kernel filter to apply on current pixel value.
     *
     * @param  t  difference between interpolation position and pixel position.
     */
    private double convolutionValue(double t) {
        t = StrictMath.abs(t);
        if (t <= 1) {
            return ((a+2)*t - (a+3))*(t*t) + 1;     // (a+2)|x|³ − (a+3)|x|² + 1
        } else if (t < 2) {
            return (((t - 5)*t + 8)*t - 4)*a;       // a|x|³ − 5a|x|² + 8a|x| − 4a
        } else {
            return 0;
        }
    }
}
