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

import java.util.ArrayList;
import java.util.List;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link GaussLegendreQuadrature}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GaussLegendreQuadratureTest {
    /**
     * Tolerance on the computed abscissas and weights.
     */
    private static final double EPS = 1E-15;

    @Test
    public void testInvalidNumberOfPoints() {
        assertThrows(IllegalArgumentException.class, () -> new GaussLegendreQuadrature(0));
        assertThrows(IllegalArgumentException.class, () -> new GaussLegendreQuadrature(-1));
    }

    /**
     * The rules of 1 and 2 points have simple closed forms.
     */
    @Test
    public void testSmallRules() {
        final GaussLegendreQuadrature one = new GaussLegendreQuadrature(1);
        assertEquals(1, one.getNumPoints());
        assertArrayEquals(new double[] {0}, one.getNodes(),   EPS);
        assertArrayEquals(new double[] {2}, one.getWeights(), EPS);

        final GaussLegendreQuadrature two = new GaussLegendreQuadrature(2);
        final double x = 1 / Math.sqrt(3);
        assertArrayEquals(new double[] {-x, x}, two.getNodes(),   EPS);
        assertArrayEquals(new double[] { 1, 1}, two.getWeights(), EPS);
    }

    /**
     * Compares the rule of 5 points with its closed form.
     */
    @Test
    public void testFivePoints() {
        final GaussLegendreQuadrature rule = new GaussLegendreQuadrature(5);
        final double x1 = Math.sqrt(5 - 2 * Math.sqrt(10.0 / 7)) / 3;
        final double x2 = Math.sqrt(5 + 2 * Math.sqrt(10.0 / 7)) / 3;
        final double w1 = (322 + 13 * Math.sqrt(70)) / 900;
        final double w2 = (322 - 13 * Math.sqrt(70)) / 900;
        assertArrayEquals(new double[] {-x2, -x1, 0, x1, x2}, rule.getNodes(), EPS);
        assertArrayEquals(new double[] {w2, w1, 128.0 / 225, w1, w2}, rule.getWeights(), EPS);
    }

    /**
     * Compares the rule of 10 points with the values tabulated in the literature.
     */
    @Test
    public void testTenPoints() {
        final GaussLegendreQuadrature rule = new GaussLegendreQuadrature(10);
        final double[] x = {0.1488743389816312, 0.4333953941292472, 0.6794095682990244, 0.8650633666889845, 0.9739065285171717};
        final double[] w = {0.2955242247147529, 0.2692667193099963, 0.2190863625159820, 0.1494513491505806, 0.0666713443086881};
        final double[] nodes   = rule.getNodes();
        final double[] weights = rule.getWeights();
        for (int i = 0; i < x.length; i++) {
            assertEquals( x[i], nodes  [5 + i], EPS);
            assertEquals(-x[i], nodes  [4 - i], EPS);
            assertEquals( w[i], weights[5 + i], EPS);
            assertEquals( w[i], weights[4 - i], EPS);
        }
    }

    /**
     * For any number of points, the abscissas are increasing and symmetric around zero,
     * and the weights are positive and sum to 2, the length of the [−1 … +1] interval.
     */
    @Test
    public void testNodesAndWeightsProperties() {
        for (int n = 1; n <= 64; n++) {
            final GaussLegendreQuadrature rule = new GaussLegendreQuadrature(n);
            final double[] nodes   = rule.getNodes();
            final double[] weights = rule.getWeights();
            double sum = 0;
            for (int i = 0; i < n; i++) {
                if (i > 0) {
                    assertTrue(nodes[i] > nodes[i-1], "Abscissas shall be increasing.");
                }
                assertTrue(nodes[i] > -1 && nodes[i] < 1, "Abscissas shall be inside the interval.");
                assertEquals(-nodes[i], nodes[n - 1 - i], EPS, "Abscissas shall be symmetric.");
                assertTrue(weights[i] > 0, "Weights shall be positive.");
                sum += weights[i];
            }
            assertEquals(2, sum, 1E-13, "Weights shall sum to 2.");
        }
    }

    /**
     * A rule of <var>n</var> points is exact for polynomials of degree up to 2<var>n</var> − 1,
     * but not beyond.
     */
    @Test
    public void testPolynomialExactness() {
        for (int n = 1; n <= 12; n++) {
            final GaussLegendreQuadrature rule = new GaussLegendreQuadrature(n);
            for (int k = 0; k <= 2*n - 1; k++) {
                final int degree = k;
                assertEquals(1.0 / (k + 1), rule.integrate((x) -> Math.pow(x, degree), 0, 1), 1E-14,
                             () -> "x^" + degree + " with " + rule.getNumPoints() + " points.");
            }
        }
        final GaussLegendreQuadrature rule = new GaussLegendreQuadrature(2);
        assertTrue(Math.abs(rule.integrate((x) -> x*x*x*x, 0, 1) - 0.2) > 1E-3,
                   "A rule of 2 points is not exact for a polynomial of degree 4.");
    }

    /**
     * A smooth function is integrated to the floating point accuracy with a few points.
     */
    @Test
    public void testSmoothFunction() {
        final GaussLegendreQuadrature rule = new GaussLegendreQuadrature(10);
        assertEquals(2, rule.integrate(Math::sin, 0, Math.PI), 1E-14);
        assertEquals(Math.E - 1, rule.integrate(Math::exp, 0, 1), 1E-14);
    }

    /**
     * Exchanging the bounds changes the sign of the integral, and an empty interval gives zero.
     */
    @Test
    public void testBounds() {
        final GaussLegendreQuadrature rule = new GaussLegendreQuadrature(10);
        assertEquals(-2, rule.integrate(Math::sin, Math.PI, 0), 1E-14);
        assertEquals( 0, rule.integrate(Math::sin, 1, 1), 0);
    }

    /**
     * The square root has an unbounded derivative at zero, so a single application of the rule is
     * inaccurate. The adaptive integration halves the intervals near zero only, until the result is
     * accurate. The sub-intervals reported to the consumer cover the interval without gap or overlap.
     */
    @Test
    public void testAdaptive() {
        final GaussLegendreQuadrature rule = new GaussLegendreQuadrature(10);
        final double expected = 2.0 / 3;
        assertTrue(Math.abs(rule.integrate(Math::sqrt, 0, 1) - expected) > 1E-8,
                   "A single application of the rule is not accurate near the singularity.");

        final List<double[]> intervals = new ArrayList<>();
        final double result = rule.integrate(Math::sqrt, 0, 1, 1E-13, 40,
                (start, end, integral) -> intervals.add(new double[] {start, end, integral}));
        assertEquals(expected, result, 1E-12);

        double previousEnd = 0, sum = 0;
        for (final double[] interval : intervals) {
            assertEquals(previousEnd, interval[0], 0, "Sub-intervals shall be contiguous and in order.");
            assertTrue(interval[1] > interval[0], "Sub-intervals shall not be empty.");
            previousEnd = interval[1];
            sum += interval[2];
        }
        assertEquals(1, previousEnd, 0, "Sub-intervals shall cover the whole interval.");
        assertEquals(result, sum, 1E-15, "The integrals of the sub-intervals shall sum to the result.");
        assertTrue(intervals.size() < 200, "Only the intervals near the singularity shall be halved.");
    }

    /**
     * Without halving, the adaptive integration integrates the two halves of the interval.
     * The consumer is optional.
     */
    @Test
    public void testAdaptiveWithoutHalving() {
        final GaussLegendreQuadrature rule = new GaussLegendreQuadrature(4);
        final double halves = rule.integrate(Math::sqrt, 0, 0.5) + rule.integrate(Math::sqrt, 0.5, 1);
        assertEquals(halves, rule.integrate(Math::sqrt, 0, 1, 1E-13, 0, null), 0);
    }
}
