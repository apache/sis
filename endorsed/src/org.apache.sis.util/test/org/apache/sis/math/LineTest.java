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
package org.apache.sis.math;

import java.util.Random;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link Line} class.
 *
 * @author  Martin Desruisseaux (MPO, IRD)
 */
@DependsOn(org.apache.sis.util.internal.DoubleDoubleTest.class)
public final class LineTest extends TestCase {
    /**
     * Tolerance factor for comparisons for floating point values.
     */
    private static final double EPS = 1E-8;

    /**
     * Creates a new test case.
     */
    public LineTest() {
    }

    /**
     * Tests {@link Line#setFromPoints(double, double, double, double)}.
     */
    @Test
    public void testSetFromPoints() {
        final Line line = new Line();
        line.setFromPoints(-2, 2,  8, 22);
        assertEquals( 2, line.slope(), EPS, "slope");
        assertEquals(-3, line.x0(),    EPS, "x₀");
        assertEquals( 6, line.y0(),    EPS, "y₀");

        // Horizontal line
        line.setFromPoints(-2, 2,  8, 2);
        assertEquals(0, line.slope(), EPS, "slope");
        assertTrue(Double.isInfinite(line.x0()), "x₀");
        assertEquals(2, line.y0(), EPS, "y₀");

        // Vertical line
        line.setFromPoints(-2, 2,  -2, 22);
        assertTrue(Double.isInfinite(line.slope()), "slope");
        assertEquals(-2, line.x0(), EPS, "x₀");
        assertTrue(Double.isInfinite(line.y0()), "y₀");

        // Horizontal line on the x axis
        line.setFromPoints(-2, 0, 8, 0);
        assertEquals(0, line.slope(), EPS, "slope");
        assertTrue(Double.isInfinite(line.x0()), "x₀");
        assertEquals(0, line.y0(), EPS, "y₀");

        // Vertical line on the y axis
        line.setFromPoints(0, 2, 0, 22);
        assertTrue(Double.isInfinite(line.slope()), "slope");
        assertEquals(0, line.x0(), EPS, "x₀");
        assertTrue(Double.isInfinite(line.y0()), "y₀");
    }

    /**
     * Tests {@link Line#fit(double[], double[])}.
     */
    @Test
    public void testFit() {
        final int    n = 10000;
        final double slope = 5;
        final double offset = 10;
        final double[] x = new double[n];
        final double[] y = new double[n];
        final Random random = new Random(888576070);
        for (int i=0; i<n; i++) {
            final double xi = random.nextDouble() * (20*n) - 10*n;
            final double yi = random.nextGaussian() * 100 + (slope * xi + offset);
            x[i] = xi;
            y[i] = yi;
        }
        final Line line = new Line();
        final double correlation = line.fit(x, y);
        assertEquals( slope,        line.slope(), 1E-6, "slope");
        assertEquals( offset,       line.y0(),    0.5,  "x₀");
        assertEquals(-offset/slope, line.x0(),    0.1,  "y₀");
        assertEquals( 1.0,          correlation,  1E-6, "corr");
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final Line local = new Line(9.5, -3.7);
        assertNotSame(local, assertSerializedEquals(local));
    }
}
