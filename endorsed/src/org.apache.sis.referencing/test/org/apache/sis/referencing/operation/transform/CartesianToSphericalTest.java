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
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

// Test dependencies
import org.junit.Test;
import org.junit.Before;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.referencing.TransformTestCase;


/**
 * Tests {@link CartesianToSpherical}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(SphericalToCartesianTest.class)
public final class CartesianToSphericalTest extends TransformTestCase {
    /**
     * Creates a new test case.
     */
    public CartesianToSphericalTest() {
    }

    /**
     * Creates the transform instance to test.
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @Before
    public void createInstance() throws FactoryException {
        transform = CartesianToSpherical.INSTANCE.completeTransform(DefaultMathTransformFactory.provider());
    }

    /**
     * Tests coordinate conversions.
     *
     * @throws TransformException if a coordinate cannot be transformed.
     */
    @Test
    public void testConversion() throws TransformException {
        tolerance = SphericalToCartesianTest.TOLERANCE;
        final double[][] data = SphericalToCartesianTest.testData();
        verifyTransform(data[1], data[0]);
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
    @DependsOnMethod({"testConversion", "testDerivative"})
    public void testConsistency() throws TransformException {
        derivativeDeltas = new double[] {1E-6, 1E-6, 1E-6};
        tolerance = 1E-5;
        verifyInDomain(new double[] {-100, -100, -100},      // Minimal coordinates
                       new double[] {+100, +100, +100},      // Maximal coordinates
                       new int[]    {  10,   10,   10},
                       TestUtilities.createRandomNumberGenerator());
    }

    /**
     * Tests concatenation.
     *
     * @throws TransformException if a coordinate cannot be transformed.
     */
    @Test
    @DependsOnMethod("testConversion")
    public void testConcatenation() throws TransformException {
        final double scale = 1000;
        transform = MathTransforms.concatenate(MathTransforms.scale(scale, scale, scale), transform);
        List<MathTransform> steps = MathTransforms.getSteps(transform);
        assertEquals(2, steps.size());
        assertSame(CartesianToSpherical.INSTANCE, steps.get(0));
        assertTrue(steps.get(1) instanceof LinearTransform);

        double[][] data = SphericalToCartesianTest.testData();
        SphericalToCartesianTest.multiply(data[1], 1/scale);
        tolerance = SphericalToCartesianTest.TOLERANCE * scale;
        verifyTransform(data[1], data[0]);
    }
}
