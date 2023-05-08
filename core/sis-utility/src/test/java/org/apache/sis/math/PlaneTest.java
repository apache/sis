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
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link Plane} class.
 *
 * @author  Martin Desruisseaux (MPO, IRD)
 * @version 0.8
 * @since   0.5
 */
@DependsOn(org.apache.sis.internal.util.DoubleDoubleTest.class)
public final class PlaneTest extends TestCase {
    /**
     * The Pearson coefficient computed by the last call to
     * {@link #assertFitEquals(double, double[], double[], double[])}.
     */
    private double pearson;

    /**
     * Invokes {@link Plane#fit(Iterable)} with the given arrays,
     * and compares the fitted values against the original values.
     *
     * This method also verifies that the Pearson coefficient is close to 1.
     *
     * @param  tolerance  the maximal difference allowed between the fitted and the original values.
     */
    private Plane assertFitEquals(final double tolerance, final double[] x, final double[] y, final double[] z) {
        final Plane plan = new Plane();
        pearson = plan.fit(x, y, z);
        assertEquals("Pearson correlation coefficient", 1, pearson, 0.01);
        for (int i = 0; i < z.length; i++) {
            assertEquals("z(x,y)", z[i], plan.z(x[i], y[i]), tolerance);
        }
        return plan;
    }

    /**
     * Returns an array of the given length filled with random values.
     *
     * @param  rd      the random number generator to use.
     * @param  length  the desired array length.
     * @param  scale   the difference between the minimal and maximal allowed values.
     * @param  offset  the minimal value allowed in the returned array.
     * @return the array of random values.
     */
    private static double[] random(final Random rd, final int length, final double scale, final double offset) {
        final double[] x = new double[length];
        for (int i=0; i<length; i++) {
            x[i] = offset + scale * rd.nextDouble();
        }
        return x;
    }

    /**
     * Tests {@link Plane#fit(Iterable)} with 3 points.
     * The solution is expected to be exact.
     */
    @Test
    public void testFit3Points() {
        final Random rd = new Random(4762038814364076443L);     // Fix the random seed for avoiding colinear points.
        for (int n=0; n<10; n++) {
            assertFitEquals(1E-9,                               // We expect close to exact matches.
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
        final double z[] = random(rd, 4000,  10,  -5);
        final Plane reference = new Plane(2, 3, -4);
        for (int i=0; i<z.length; i++) {
            z[i] += reference.z(x[i], y[i]);    // Compute points with random displacements above or below a known plane.
        }
        /*
         * For a random seed fixed to 7241997054993322309, the coefficients are:
         *
         *   - reference:     z(x,y) = 2.0⋅x + 3.0⋅y + -4.0
         *   - fitted:        z(x,y) = 2.001595888896693⋅x + 3.0021028196088055⋅y + -4.105960575835259
         */
        final Plane fitted = assertFitEquals(6, x, y, z);
        assertEquals("sx", reference.slopeX(), fitted.slopeX(), 0.002);
        assertEquals("sy", reference.slopeY(), fitted.slopeY(), 0.003);
        assertEquals("z₀", reference.z0(),     fitted.z0(),     0.2);
    }

    /**
     * Verifies that {@link Plane#fit(int, int, Vector)} produces the same result than
     * {@link Plane#fit(double[], double[], double[])}.
     *
     * @since 0.8
     */
    @Test
    @DependsOnMethod("testFitManyPoints")
    public void testFitGrid() {
        final int nx = 20;
        final int ny = 30;
        final Random  rd = new Random(1224444984079270867L);
        final double z[] = random(rd, nx*ny, 12, -6);
        final double x[] = new double[z.length];
        final double y[] = new double[z.length];
        final Plane reference = new Plane(-46, 17, 35);
        for (int i=0; i<z.length; i++) {
            x[i] = i % nx;
            y[i] = i / nx;
            z[i] += reference.z(x[i], y[i]);
        }
        /*
         * Opportunistically verify the result of fit(double[], double[], double[]), but it is
         * not the purpose of this test (it should have been verified by testFitManyPoints()).
         * For a random seed fixed to 7241997054993322309, the coefficients are:
         *
         *   - reference:     z(x,y) = -46.0⋅x + 17.0⋅y + 35.0
         *   - fitted:        z(x,y) = -45.96034442769177⋅x + 16.981792790278828⋅y + 34.901440258861314
         */
        final Plane fitted = assertFitEquals(7, x, y, z);
        assertEquals("sx", reference.slopeX(), fitted.slopeX(), 0.05);
        assertEquals("sy", reference.slopeY(), fitted.slopeY(), 0.02);
        assertEquals("z₀", reference.z0(),     fitted.z0(),     0.1);
        final double ep = pearson;
        /*
         * Verify that the optimized code path for a grid produces the exact same result than the
         * generic code path.
         */
        final Plane gf = new Plane();
        gf.fit(nx, ny, Vector.create(z));
        assertEquals("sx", fitted.slopeX(), gf.slopeX(), STRICT);
        assertEquals("sy", fitted.slopeY(), gf.slopeY(), STRICT);
        assertEquals("z₀", fitted.z0(),     gf.z0(),     STRICT);
        assertEquals("Pearson", ep,         pearson,     STRICT);
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
