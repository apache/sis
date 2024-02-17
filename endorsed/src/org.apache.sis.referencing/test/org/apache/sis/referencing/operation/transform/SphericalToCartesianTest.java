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
package org.apache.sis.referencing.operation.transform;

import java.util.List;
import static java.lang.StrictMath.*;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestUtilities;


/**
 * Tests {@link SphericalToCartesian}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SphericalToCartesianTest extends TransformTestCase {
    /**
     * Creates a new test case.
     */
    public SphericalToCartesianTest() {
    }

    /**
     * Tolerance factor for verifying the conversion results.
     */
    static final double TOLERANCE = 1E-12;

    /**
     * Returns coordinate tuples in spherical coordinates and their equivalent in Cartesian coordinates.
     */
    static double[][] testData() {
        final double r     = 1000;
        final double cos30 = r*sqrt(0.75);      // cos(30°) = √¾
        final double cos60 = r/2;               // cos(60°) =  ½
        final double sin60 = cos30;
        final double sin30 = cos60;
        return new double[][] {
            new double[] {                      // (θ,Ω,r) coordinates
                 0,       0,       0,
                 0,       0,       r,
                90,       0,       r,
               -90,       0,       r,
                 0,      90,       r,
                 0,     -90,       r,
                 0,      60,       r,
                60,       0,       r,
                30,       0,       r,
                30,      60,       r
            }, new double[] {                   // (X,Y,Z) coordinates
                 0,       0,       0,
                 r,       0,       0,
                 0,       r,       0,
                 0,      -r,       0,
                 0,       0,       r,
                 0,       0,      -r,
                 cos60,   0,       sin60,
                 cos60,   sin60,   0,
                 cos30,   sin30,   0,
                 cos30/2, sin30/2, sin60
            }
        };
    }

    /**
     * Multiples all elements of the given array by the given scale factor.
     * This is used when the test data are modified for testing concatenation.
     */
    static void multiply(final double[] data, final double scale) {
        for (int i=0; i<data.length; i++) {
            data[i] *= scale;
        }
    }

    /**
     * Creates the transform instance to test.
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @BeforeEach
    public void createInstance() throws FactoryException {
        transform = SphericalToCartesian.INSTANCE.completeTransform(DefaultMathTransformFactory.provider());
    }

    /**
     * Tests coordinate conversions.
     *
     * @throws TransformException if a coordinate cannot be transformed.
     */
    @Test
    public void testConversion() throws TransformException {
        tolerance = TOLERANCE;
        final double[][] data = testData();
        verifyTransform(data[0], data[1]);
    }

    /**
     * Tests calculation of a transform derivative.
     *
     * @throws TransformException if a coordinate cannot be transformed.
     */
    @Test
    public void testDerivative() throws TransformException {
        derivativeDeltas = new double[] {1E-6, 1E-6, 1E-6};
        tolerance = 1E-7;
        verifyDerivative(30, 60, 100);
    }

    /**
     * Tests calculation of a transform derivative.
     *
     * @throws TransformException if a coordinate cannot be transformed.
     */
    @Test
    public void testConsistency() throws TransformException {
        derivativeDeltas = new double[] {1E-6, 1E-6, 1E-6};
        tolerance = 1E-7;
        verifyInDomain(new double[] {-180, -90,   0},       // Minimal coordinates
                       new double[] {+180, +90, 100},       // Maximal coordinates
                       new int[]    {  10,  10,  10},
                       TestUtilities.createRandomNumberGenerator());
    }

    /**
     * Tests concatenation.
     *
     * @throws TransformException if a coordinate cannot be transformed.
     */
    @Test
    public void testConcatenation() throws TransformException {
        final double scale = 1000;
        transform = MathTransforms.concatenate(transform, MathTransforms.scale(scale, scale, scale));
        List<MathTransform> steps = MathTransforms.getSteps(transform);
        assertEquals(2, steps.size());
        assertTrue(steps.get(0) instanceof LinearTransform);
        assertSame(SphericalToCartesian.INSTANCE, steps.get(1));

        double[][] data = testData();
        multiply(data[1], scale);
        tolerance = TOLERANCE * scale;
        verifyTransform(data[0], data[1]);
    }
}
