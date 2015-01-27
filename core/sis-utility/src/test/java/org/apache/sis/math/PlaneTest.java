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
import org.opengis.geometry.DirectPosition;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Plane} class.
 *
 * @author  Martin Desruisseaux (MPO, IRD)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(org.apache.sis.internal.util.DoubleDoubleTest.class)
public final strictfp class PlaneTest extends TestCase {
    /**
     * Invokes {@link Plane#fit(DirectPosition[])} with the given arrays,
     * and compares the fitted values against the original values.
     *
     * This method also verifies that the Pearson coefficient is close to 1.
     *
     * @param tolerance The maximal difference allowed between the fitted and the original values.
     */
    private static Plane assertFitEquals(final double tolerance, final double[] x, final double[] y, final double[] z) {
        final Plane plan = new Plane();
        final double pearson = plan.fit(x, y, z);
        assertEquals("Pearson correlation coefficient", 1, pearson, 0.01);
        for (int i = 0; i < z.length; i++) {
            assertEquals("z(x,y)", z[i], plan.z(x[i], y[i]), tolerance);
        }
        return plan;
    }

    /**
     * Returns an array of the given length filled with random values.
     *
     * @param  rd     The random number generator to use.
     * @param  length The desired array length.
     * @param  offset The minimal value allowed in the returned array.
     * @param  scale  The difference between the minimal and maximal allowed values.
     * @return The array of random values.
     */
    private static double[] random(final Random rd, final int length, final double offset, final double scale) {
        final double[] x = new double[length];
        for (int i=0; i<length; i++) {
            x[i] = offset + scale * rd.nextDouble();
        }
        return x;
    }

    /**
     * Tests {@link Plane#fit(DirectPosition[])} with 3 points.
     * The solution is expected to be exact.
     */
    @Test
    public void testFit3Points() {
        // We fix the random seed in order to avoid colinear points.
        final Random rd = new Random(4762038814364076443L);
        for (int n=0; n<10; n++) {
            assertFitEquals(1E-9, // We expect close to exact matches.
                    random(rd, 3, 100, -25),
                    random(rd, 3,  80, -20),
                    random(rd, 3, 150, -40));
        }
    }

    /**
     * Tests {@link Plane#fit(double[], double[], double[])} with 4000 points.
     */
    @Test
    @DependsOnMethod("testFit3Points")
    public void testFitManyPoints() {
        final Random  rd = new Random(7241997054993322309L);
        final double x[] = random(rd, 4000, 100, -25);
        final double y[] = random(rd, 4000,  80, -20);
        final double z[] = random(rd, 4000, 150, -40);
        final Plane plan = new Plane(2, 3, -4);
        for (int i=0; i<z.length; i++) {
            // Compute points with random displacements above or below a known plane.
            z[i] = plan.z(x[i], y[i]) + (10 * rd.nextDouble() - 5);
        }
        final Plane fitted = assertFitEquals(6, x, y, z);
        assertEquals("sx", plan.slopeX(), fitted.slopeX(), 0.01);
        assertEquals("sy", plan.slopeY(), fitted.slopeY(), 0.01);
        assertEquals("z₀", plan.z0(),     fitted.z0(),     1);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final Plane local = new Plane(3.7, 9.3, -1.8);
        assertNotSame(local, assertSerializedEquals(local));
    }
}
