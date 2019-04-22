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
package org.apache.sis.referencing.operation.projection;

import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Sinusoidal} projection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@DependsOn(MeridionalDistanceTest.class)
public final strictfp class SinusoidalTest extends MapProjectionTestCase {
    /**
     * Creates a new instance of {@link Sinusoidal} concatenated with the (de)normalization matrices.
     * The new instance is stored in the inherited {@link #transform} field.
     *
     * @param  ellipse  {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     */
    private void createProjection(final boolean ellipse) throws FactoryException {
        createCompleteProjection(new org.apache.sis.internal.referencing.provider.Sinusoidal(),
                ellipse ? WGS84_A : 6400000,
                ellipse ? WGS84_B : 6400000,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        tolerance = Formulas.LINEAR_TOLERANCE;  // Not NORMALIZED_TOLERANCE since this is not a NormalizedProjection.
    }

    /**
     * Tests the projection of a few points on a sphere.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSpherical() throws FactoryException, TransformException {
        createProjection(false);
        assertTrue(isInverseTransformSupported);
        verifyTransform(
            new double[] {              // (λ,φ) coordinates in degrees to project.
                  2,      1
            },
            new double[] {              // Expected (x,y) results in metres.
               223368.12, 111701.07     // Values taken from PROJ.4.
            });
    }

    /**
     * Tests the projection of a few points on an ellipsoid.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    @org.junit.Ignore("Ellipsoidal formula not yet implemented.")
    public void testEllipsoidal() throws FactoryException, TransformException {
        createProjection(true);
        assertTrue(isInverseTransformSupported);
        verifyTransform(
            new double[] {              // (λ,φ) coordinates in degrees to project.
                  2,      1
            },
            new double[] {              // Expected (x,y) results in metres.
               222605.30, 110574.39     // Values taken from PROJ.4.
            });
    }

    /**
     * Tests the derivatives at a few points. This method compares the derivatives computed by
     * the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    @DependsOnMethod("testInverseDerivative")
    public void testDerivative() throws FactoryException, TransformException {
        createProjection(false);
        final double delta = (100.0 / 60) / 1852;               // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 1E-6;                                       // More severe than Formulas.LINEAR_TOLERANCE.
        verifyDerivative(15,  30);
        verifyDerivative(10, -60);
    }
}
