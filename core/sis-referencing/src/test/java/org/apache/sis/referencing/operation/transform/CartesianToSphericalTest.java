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

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;

// Test dependencies
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;


/**
 * Tests {@link CartesianToSpherical}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(SphericalToCartesianTest.class)
public final strictfp class CartesianToSphericalTest extends TransformTestCase {
    /**
     * Tests coordinate conversions.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if a coordinate can not be transformed.
     */
    @Test
    public void testConversion() throws FactoryException, TransformException {
        transform = CartesianToSpherical.INSTANCE.completeTransform(SphericalToCartesianTest.factory());
        tolerance = 1E-12;
        final double[][] data = SphericalToCartesianTest.testData();
        verifyTransform(data[1], data[0]);
    }

    /**
     * Tests calculation of a transform derivative.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if a coordinate can not be transformed.
     */
    @Test
    public void testDerivative() throws FactoryException, TransformException {
        transform = CartesianToSpherical.INSTANCE.completeTransform(SphericalToCartesianTest.factory());
        derivativeDeltas = new double[] {1E-6, 1E-6, 1E-6};
        tolerance = 1E-7;
        verifyDerivative(30, 60, 100);
    }

    /**
     * Tests calculation of a transform derivative.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if a coordinate can not be transformed.
     */
    @Test
    @DependsOnMethod({"testConversion", "testDerivative"})
    public void testConsistency() throws FactoryException, TransformException {
        transform = CartesianToSpherical.INSTANCE.completeTransform(SphericalToCartesianTest.factory());
        derivativeDeltas = new double[] {1E-6, 1E-6, 1E-6};
        tolerance = 2E-6;
        verifyInDomain(new double[] {-100, -100, -100},      // Minimal coordinates
                       new double[] {+100, +100, +100},      // Maximal coordinates
                       new int[]    {  10,   10,   10},
                       TestUtilities.createRandomNumberGenerator());
    }
}
