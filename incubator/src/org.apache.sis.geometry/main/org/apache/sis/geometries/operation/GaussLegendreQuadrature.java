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
package org.apache.sis.geometries.operation;

import java.util.function.DoubleUnaryOperator;


/**
 * Numerical integration of a function of one variable by the Gauss-Legendre quadrature.
 *
 * <p>A rule of <var>n</var> points evaluates the function at the roots of the Legendre polynomial of
 * degree <var>n</var>, mapped to the integration interval, and is exact for polynomials of degree up
 * to 2<var>n</var> − 1. For smooth functions the error decreases very fast with <var>n</var>. For
 * functions which are not smooth everywhere, the {@linkplain #integrate(DoubleUnaryOperator, double,
 * double, double, int, IntervalConsumer) adaptive integration} halves the interval where needed.</p>
 *
 * <p>Instances are immutable and thread-safe.</p>
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://en.wikipedia.org/wiki/Gauss%E2%80%93Legendre_quadrature">Gauss–Legendre quadrature</a>
 */
public final class GaussLegendreQuadrature {

    /**
     * Receives the sub-intervals over which an adaptive integration has converged,
     * in increasing order of the integration variable.
     */
    @FunctionalInterface
    public interface IntervalConsumer {
        /**
         * Receives a sub-interval and the integral of the function over it.
         *
         * @param start     start of the sub-interval.
         * @param end       end of the sub-interval.
         * @param integral  integral of the function from {@code start} to {@code end}.
         */
        void accept(double start, double end, double integral);
    }

    /**
     * Abscissas of the rule in the [−1 … +1] interval, in increasing order.
     */
    private final double[] nodes;

    /**
     * Weight of each abscissa.
     */
    private final double[] weights;

    /**
     * Creates a rule of the given number of points.
     *
     * @param  n  number of points, at least 1.
     * @throws IllegalArgumentException if the number of points is less than 1.
     */
    public GaussLegendreQuadrature(final int n) {
        if (n < 1) {
            throw new IllegalArgumentException("The number of points must be at least 1, but got " + n + '.');
        }
        nodes   = new double[n];
        weights = new double[n];
        /*
         * The roots are symmetric around zero, so only the positive ones are computed, by Newton
         * iterations starting from an asymptotic approximation of the root. The Legendre polynomial
         * and its derivative are evaluated by the three-term recurrence.
         */
        for (int i = 0; i < (n + 1) / 2; i++) {
            double z = Math.cos(Math.PI * (i + 0.75) / (n + 0.5));
            double derivative;
            for (int iteration = 0; ; iteration++) {
                double p1 = 1, p2 = 0;
                for (int j = 1; j <= n; j++) {
                    final double p3 = p2;
                    p2 = p1;
                    p1 = ((2*j - 1) * z * p2 - (j - 1) * p3) / j;
                }
                derivative = n * (z * p1 - p2) / (z * z - 1);
                final double previous = z;
                z -= p1 / derivative;
                if (Math.abs(z - previous) <= Math.ulp(1.0) || iteration >= 100) break;
            }
            final double weight = 2 / ((1 - z * z) * derivative * derivative);
            nodes[i]         = -z;
            nodes[n - 1 - i] =  z;
            weights[i]         = weight;
            weights[n - 1 - i] = weight;
        }
        if ((n & 1) != 0) {
            nodes[n / 2] = 0;               // Exact zero instead of a value close to it.
        }
    }

    /**
     * Returns the number of points of this rule.
     *
     * @return number of points.
     */
    public int getNumPoints() {
        return nodes.length;
    }

    /**
     * Returns the abscissas of this rule in the [−1 … +1] interval, in increasing order.
     *
     * @return a copy of the abscissas.
     */
    public double[] getNodes() {
        return nodes.clone();
    }

    /**
     * Returns the weights of this rule, in the order of the {@linkplain #getNodes() abscissas}.
     *
     * @return a copy of the weights.
     */
    public double[] getWeights() {
        return weights.clone();
    }

    /**
     * Integrates the given function from {@code a} to {@code b} by a single application of this rule.
     * The result is negative if {@code b} is less than {@code a} and the function is positive.
     *
     * @param  f  the function to integrate.
     * @param  a  lower bound of the integral.
     * @param  b  upper bound of the integral.
     * @return integral of the function from {@code a} to {@code b}.
     */
    public double integrate(final DoubleUnaryOperator f, final double a, final double b) {
        final double half = (b - a) / 2;
        final double mid  = (a + b) / 2;
        double sum = 0;
        for (int i = 0; i < nodes.length; i++) {
            sum += weights[i] * f.applyAsDouble(mid + half * nodes[i]);
        }
        return sum * half;
    }

    /**
     * Integrates the given function from {@code a} to {@code b}, halving the interval until the
     * integrals over the two halves agree with the integral over the whole interval within the given
     * relative tolerance. Each sub-interval is halved independently, so the subdivision is finer only
     * where the function needs it.
     *
     * <p>If the function cannot be integrated within the tolerance, for example because its integral
     * is zero or because it is not continuous, the halving stops after {@code maxDepth} levels.</p>
     *
     * @param  f          the function to integrate.
     * @param  a          lower bound of the integral.
     * @param  b          upper bound of the integral.
     * @param  tolerance  relative tolerance on the integral of each sub-interval.
     * @param  maxDepth   maximal number of interval halvings.
     * @param  consumer   receives the sub-intervals and their integrals in order, or {@code null} if none.
     * @return integral of the function from {@code a} to {@code b}.
     */
    public double integrate(final DoubleUnaryOperator f, final double a, final double b,
                            final double tolerance, final int maxDepth, final IntervalConsumer consumer)
    {
        return adaptive(f, a, b, integrate(f, a, b), tolerance, maxDepth, consumer);
    }

    /**
     * Recursive part of the adaptive integration, given the integral over the whole interval.
     */
    private double adaptive(final DoubleUnaryOperator f, final double a, final double b, final double whole,
                            final double tolerance, final int depth, final IntervalConsumer consumer)
    {
        final double m     = (a + b) / 2;
        final double left  = integrate(f, a, m);
        final double right = integrate(f, m, b);
        final double sum   = left + right;
        if (depth <= 0 || Math.abs(sum - whole) <= tolerance * Math.abs(sum)) {
            if (consumer != null) {
                consumer.accept(a, m, left);
                consumer.accept(m, b, right);
            }
            return sum;
        }
        return adaptive(f, a, m, left,  tolerance, depth - 1, consumer)
             + adaptive(f, m, b, right, tolerance, depth - 1, consumer);
    }
}
