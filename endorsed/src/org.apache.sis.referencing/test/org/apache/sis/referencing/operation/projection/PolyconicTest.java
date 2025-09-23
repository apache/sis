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

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.Formulas;

// Test dependencies
import org.junit.jupiter.api.Test;


/**
 * Tests the {@link Polyconic} class.
 *
 * @author  Simon Reynard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 */
public final class PolyconicTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public PolyconicTest() {
    }

    /**
     * Creates a new instance of {@link Polyconic} concatenated with the (de)normalization matrices.
     * The new instance is stored in the inherited {@link #transform} field.
     *
     * @param  ellipsoidal  {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     */
    private void createProjection(final boolean ellipsoidal) throws FactoryException {
        createCompleteProjection(new org.apache.sis.referencing.operation.provider.Polyconic(),
                ellipsoidal ? CLARKE_A : RADIUS,        // Semi-major axis (Clarke 1866)
                ellipsoidal ? CLARKE_B : RADIUS,        // Semi-minor axis (Clarke 1866)
                -96,                                    // Central meridian
                 30,                                    // Latitude of origin
                Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        tolerance = Formulas.LINEAR_TOLERANCE;  // Not NORMALIZED_TOLERANCE since this is not a NormalizedProjection.
    }

    /**
     * Tests the projection of a few points on a sphere. The first point in this test is provided
     * by Snyder at page 304. The Snyder example gives intermediate values at different step,
     * which may be verified by executing this code in the debugger.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSpherical() throws FactoryException, TransformException {
        createProjection(false);
        verifyTransform(
            new double[] {                  // (λ,φ) coordinates in degrees to project.
                -75, 40,                    // Snyder example is relative to λ₀ = 96°W.
                -75,  0
            },
            new double[] {                  // Expected (x,y) results in metres.
                1780350.84,  1327706.12,    // Values derived from Snyder page 303 with R=6400000 metres.
                2345722.51, -3351032.16
            });
    }

    /**
     * Tests the projection of a few points on an ellipsoid. The first point in this test is provided
     * by Snyder at page 304. The Snyder example gives intermediate values at different step, which may
     * be verified by executing this code in the debugger. In particular during reverse projection,
     * values of φ should be as below during each iteration steps:
     *
     * <ol>
     *   <li>0.6967280</li>
     *   <li>0.6981286</li>
     *   <li>0.6981317</li>
     * </ol>
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testEllipsoidal() throws FactoryException, TransformException {
        createProjection(true);
        verifyTransform(
            new double[] {                  // (λ,φ) coordinates in degrees to project.
                -75, 40,                    // Snyder example is relative to λ₀ = 96°W.
                -75,  0
            },
            new double[] {                  // Expected (x,y) results in metres.
                1776774.54,  1319657.78,    // Values derived from Snyder page 304.
                2337734.74, -3319933.30
            });
    }

    /**
     * Tests the derivatives at a few points on a sphere. This method compares the derivatives computed
     * by the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testDerivativeOnSphere() throws FactoryException, TransformException {
        createProjection(false);
        final double delta = (1.0 / 60) / 1852;                 // Approximately 1 metre.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = Formulas.LINEAR_TOLERANCE / 10;
        verifyDerivative(-100,  3);
        verifyDerivative( -56, 50);
        verifyDerivative( -20, 47);
    }

    /**
     * Tests the derivatives at a few points on an ellipsoid. This method compares the derivatives computed
     * by the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testDerivativeOnEllipsoid() throws FactoryException, TransformException {
        createProjection(true);
        final double delta = (1.0 / 60) / 1852;                 // Approximately 1 metre.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = Formulas.LINEAR_TOLERANCE / 10;
        verifyDerivative(-100,  3);
        verifyDerivative( -56, 50);
        verifyDerivative( -20, 47);
    }
}
