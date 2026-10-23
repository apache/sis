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
package org.apache.sis.maths;


/**
 * Accurate sums of products, used by the vector methods having an {@code Accurate} suffix
 * such as {@link Vector#dotAccurate(Vector)}, {@link Vector#crossAccurate(Vector)}
 * and {@link Vector#lerpAccurate(Vector, double)}.
 *
 * <h2>Why accurate methods are provided</h2>
 * A plain sum of products such as {@code a1*b1 + a2*b2} rounds each product before adding them.
 * When the terms nearly cancel, for example in the dot product of nearly orthogonal vectors or in
 * the cross product of nearly parallel vectors, the rounding errors of the products can be as large
 * as the result itself and all significant digits are lost. For example the plain dot product of
 * (1e8, 1, -1e8) and (1e8, 1, 1e8) is 0 while the exact result is 1.
 *
 * <p>The methods of this class compute each product exactly with {@link Math#fma(double, double, double)}
 * and accumulate the rounding errors of the sums (algorithm <cite>Dot2</cite> of Ogita, Rump and Oishi,
 * <cite>Accurate sum and dot product</cite>, SIAM Journal on Scientific Computing, 2005).
 * The result is as accurate as if it was computed with twice the {@code double} precision,
 * then rounded once to {@code double}: it is the correctly rounded exact value unless the terms
 * cancel by more than about 16 significant digits, in which case the relative error stays near 1E-16
 * times the cancellation ratio instead of losing all digits.
 * This is the same accuracy as the vector classes of Apache Commons Geometry.</p>
 *
 * <p>This accuracy has a cost: those methods are about 3 to 5 times slower than plain sums of products.
 * For this reason the default vector methods ({@code dot}, {@code cross}, {@code _lerp}…) use plain
 * arithmetic, which is fast and gives the same results as common libraries such as gl-matrix.
 * The accurate methods are for the cases where terms may nearly cancel.</p>
 *
 * <h2>Special values</h2>
 * When a product or a partial sum overflows, or when a value is NaN, the error terms are meaningless.
 * In such cases the plain sum of products is returned, so infinities and NaN follow the usual IEEE 754 rules.
 *
 * @author Johann Sorel (Geomatys)
 */
final class LinearCombination {

    private LinearCombination() {
    }

    /**
     * Computes {@code a1*b1 + a2*b2} accurately.
     *
     * @return the sum of products
     */
    static double of(final double a1, final double b1, final double a2, final double b2) {
        final double p1 = a1 * b1;
        final double r1 = Math.fma(a1, b1, -p1);
        final double h  = a2 * b2;
        final double r2 = Math.fma(a2, b2, -h);
        final double p  = p1 + h;
        if (!Double.isFinite(p)) return p;
        final double z  = p - p1;
        return p + (((p1 - (p - z)) + (h - z)) + r1 + r2);
    }

    /**
     * Computes {@code a1*b1 + a2*b2 + a3*b3} accurately.
     *
     * @return the sum of products
     */
    static double of(final double a1, final double b1, final double a2, final double b2,
                     final double a3, final double b3)
    {
        double p = a1 * b1;
        double s = Math.fma(a1, b1, -p);
        double h, q, z;
        h = a2 * b2; q = p + h; z = q - p; s += ((p - (q - z)) + (h - z)) + Math.fma(a2, b2, -h); p = q;
        h = a3 * b3; q = p + h; z = q - p; s += ((p - (q - z)) + (h - z)) + Math.fma(a3, b3, -h); p = q;
        return Double.isFinite(p) ? p + s : p;
    }

    /**
     * Computes {@code a1*b1 + a2*b2 + a3*b3 + a4*b4} accurately.
     *
     * @return the sum of products
     */
    static double of(final double a1, final double b1, final double a2, final double b2,
                     final double a3, final double b3, final double a4, final double b4)
    {
        double p = a1 * b1;
        double s = Math.fma(a1, b1, -p);
        double h, q, z;
        h = a2 * b2; q = p + h; z = q - p; s += ((p - (q - z)) + (h - z)) + Math.fma(a2, b2, -h); p = q;
        h = a3 * b3; q = p + h; z = q - p; s += ((p - (q - z)) + (h - z)) + Math.fma(a3, b3, -h); p = q;
        h = a4 * b4; q = p + h; z = q - p; s += ((p - (q - z)) + (h - z)) + Math.fma(a4, b4, -h); p = q;
        return Double.isFinite(p) ? p + s : p;
    }

    /**
     * Computes the dot product of two vectors of the same dimension accurately.
     *
     * @param  a  first vector
     * @param  b  second vector
     * @return the dot product
     */
    static double dot(final Vector<?> a, final Vector<?> b) {
        final int n = a.getDimension();
        if (n == 0) return 0;
        double ai = a.get(0), bi = b.get(0);
        double p = ai * bi;
        double s = Math.fma(ai, bi, -p);
        for (int i = 1; i < n; i++) {
            ai = a.get(i); bi = b.get(i);
            final double h = ai * bi;
            final double q = p + h;
            final double z = q - p;
            s += ((p - (q - z)) + (h - z)) + Math.fma(ai, bi, -h);
            p = q;
        }
        return Double.isFinite(p) ? p + s : p;
    }

    /**
     * Computes the dot product of an array and a vector of the same dimension accurately.
     *
     * @param  a  values of the first vector
     * @param  b  second vector
     * @return the dot product
     */
    static double dot(final double[] a, final Vector<?> b) {
        if (a.length == 0) return 0;
        double ai = a[0], bi = b.get(0);
        double p = ai * bi;
        double s = Math.fma(ai, bi, -p);
        for (int i = 1; i < a.length; i++) {
            ai = a[i]; bi = b.get(i);
            final double h = ai * bi;
            final double q = p + h;
            final double z = q - p;
            s += ((p - (q - z)) + (h - z)) + Math.fma(ai, bi, -h);
            p = q;
        }
        return Double.isFinite(p) ? p + s : p;
    }

    /**
     * Computes the dot product of an array and a vector of the same dimension accurately.
     *
     * @param  a  values of the first vector
     * @param  b  second vector
     * @return the dot product
     */
    static double dot(final float[] a, final Vector<?> b) {
        if (a.length == 0) return 0;
        double ai = a[0], bi = b.get(0);
        double p = ai * bi;
        double s = Math.fma(ai, bi, -p);
        for (int i = 1; i < a.length; i++) {
            ai = a[i]; bi = b.get(i);
            final double h = ai * bi;
            final double q = p + h;
            final double z = q - p;
            s += ((p - (q - z)) + (h - z)) + Math.fma(ai, bi, -h);
            p = q;
        }
        return Double.isFinite(p) ? p + s : p;
    }
}
