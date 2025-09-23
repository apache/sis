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
import org.apache.sis.referencing.internal.shared.Formulas;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the {@link Mollweide} projection.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MollweideTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public MollweideTest() {
    }

    /**
     * Creates a new instance of {@link Mollweide} concatenated with the (de)normalization matrices.
     * The new instance is stored in the inherited {@link #transform} field.
     *
     * @param  ellipse  {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     */
    private void createProjection(final boolean ellipse) throws FactoryException {
        createCompleteProjection(new org.apache.sis.referencing.operation.provider.Mollweide(),
                WGS84_A, ellipse ? WGS84_B : WGS84_A,
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
    public void testTransform() throws FactoryException, TransformException {
        createProjection(false);
        assertTrue(isInverseTransformSupported);
        verifyTransform(
            new double[] {          // (λ,φ) coordinates in degrees to project.
                  0,      0,        // At (0,0) point should be unchanged.
                  0,    +90,        // At (0,±90) north/south poles singularity.
                  0,    -90,
                  0,     89,        // At (0,~90) point near north pole singularity should be close to ~9000000.
                 12,     50,        // Other random points.
               -150,    -70,
               -179.9999, 0
            },
            new double[] {          // Expected (x,y) results in metres.
                       0.0,           0.0,
                       0.0,     9020047.848,
                       0.0,    -9020047.848,
                       0.0,     8997266.899,
                  912759.823,   5873471.956,
                -7622861.357,  -7774469.608,
               -18040085.674,         0.0
            });
    }

    /**
     * Tests the projection of a few points on a sphere computed from authalic radius.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testOnAuthalicRadius() throws FactoryException, TransformException {
        createProjection(true);
        assertTrue(isInverseTransformSupported);
        verifyTransform(
            new double[] {          // (λ,φ) coordinates in degrees to project.
                 12,     50,        // Other random points.
               -150,    -70,
               -179.9999, 0
            },
            new double[] {          // Expected (x,y) results in metres.
                  911739.492,    5866906.278,
                -7614340.119,   -7765778.894,
               -18019919.511,          0.0
            });
    }

    /**
     * Tests reverse projection of a point outside domain of validity.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testTransformOutsideDomain() throws FactoryException, TransformException {
        createProjection(false);
        final double[] in  = new double[] {-180.0001,     0.0};
        final double[] out = new double[] {-18040105.718, 0.0};
        isInverseTransformSupported = false;
        verifyTransform(in, out);

        // Outside of validity area, should have NaN with the inverse transform.
        transform = transform.inverse();
        tolerance = 0;
        in[0] = Double.NaN;
        verifyTransform(out, in);
    }

    /**
     * Tests the inverse derivatives at a few points. This method compares the derivatives computed by
     * the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-428">SIS-428</a>
     */
    @Test
    public void testInverseDerivative() throws FactoryException, TransformException {
        createProjection(false);
        transform = transform.inverse();
        derivativeDeltas = new double[] {100, 100};             // Approximately 100 metres.
        tolerance = Formulas.ANGULAR_TOLERANCE;
        verifyDerivative(  912759.823,  5873471.956);
        verifyDerivative(-7622861.357, -7774469.608);
    }

    /**
     * Tests the derivatives at a few points. This method compares the derivatives computed by
     * the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testDerivative() throws FactoryException, TransformException {
        createProjection(false);
        final double delta = (100.0 / 60) / 1852;               // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 1E-6;                                       // More severe than Formulas.LINEAR_TOLERANCE.
        verifyDerivative(15,  30);
        verifyDerivative(10, -60);
    }
}
