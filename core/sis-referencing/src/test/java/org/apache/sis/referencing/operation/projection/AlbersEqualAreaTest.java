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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.parameter.Parameters;

import static java.lang.StrictMath.*;

// Test dependencies
import org.opengis.test.ToleranceModifier;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link AlbersEqualArea} class. We test using various values of standard parallels.
 * We do not test with various values of the latitude of origin, because its only effect is to
 * modify the translation term on the <var>y</var> axis.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(CylindricalEqualAreaTest.class)
public final strictfp class AlbersEqualAreaTest extends MapProjectionTestCase {
    /**
     * Creates a new map projection. See the class javadoc for an explanation about
     * why we ask only for the standard parallels and not the latitude of origin.
     *
     * @param  a   semi-major axis length.
     * @param  b   semi-minor axis length.
     * @param  φ1  first standard parallel.
     * @param  φ2  second standard parallel.
     * @return newly created projection.
     * @throws FactoryException if an error occurred while creating the map projection.
     */
    private static MathTransform create(final double a, final double b, double φ1, double φ2) throws FactoryException {
        final MapProjection method = new org.apache.sis.internal.referencing.provider.AlbersEqualArea();
        final Parameters values = Parameters.castOrWrap(method.getParameters().createValue());
        values.parameter("semi_major").setValue(a);
        values.parameter("semi_minor").setValue(b);
        values.parameter("standard_parallel_1").setValue(φ1);
        values.parameter("standard_parallel_2").setValue(φ2);
        return method.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
    }

    /**
     * Returns whether the given projection is the spherical implementation.
     */
    private static boolean isSpherical(final AlbersEqualArea transform) {
        return transform instanceof AlbersEqualArea.Spherical;
    }

    /**
     * Tests the unitary projection on a sphere.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSphere() throws FactoryException, TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;                  // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        toleranceModifier = ToleranceModifier.PROJECTION;
        tolerance = Formulas.LINEAR_TOLERANCE;
        transform = create(6370997, 6370997, 29.5, 45.5);                   // Standard parallels from Synder table 15.
        final AlbersEqualArea kernel = (AlbersEqualArea) getKernel();
        assertTrue("isSpherical", isSpherical(kernel));
        assertEquals("n", 0.6028370, kernel.nm, 0.5E-7);                    // Expected 'n' value from Synder table 15.
        /*
         * When stepping into the AlbersEqualArea.Sphere.transform(…) method with a debugger, the
         * expected value of 6370997*ρ/n is 6910941 (value taken from ρ column in Synder table 15).
         */
        verifyTransform(new double[] {0, 50}, new double[] {0, 5373933.180});
        /*
         * Expect 6370997*ρ/n  ≈  8022413   (can be verified only with the debugger)
         */
        verifyTransform(new double[] {0, 40}, new double[] {0, 4262461.266});
        /*
         * Expect 6370997*ρ/n  ≈  9695749   (can be verified only with the debugger)
         */
        verifyTransform(new double[] {0, 25}, new double[] {0, 2589125.654});
        /*
         * Verify consistency with random points.
         */
        verifyInDomain(new double[] {-20, 20}, new double[] {20, 50}, new int[] {5, 5},
                TestUtilities.createRandomNumberGenerator());
    }

    /**
     * Tests the unitary projection on an ellipse.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    @DependsOnMethod("testSphere")
    public void testEllipse() throws FactoryException, TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;                  // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        toleranceModifier = ToleranceModifier.PROJECTION;
        tolerance = Formulas.LINEAR_TOLERANCE;
        transform = create(6378206.4, 6356583.8, 29.5, 45.5);               // Standard parallels from Synder table 15.
        final AlbersEqualArea kernel = (AlbersEqualArea) getKernel();
        assertFalse("isSpherical", isSpherical(kernel));
        /*
         * Expected 'n' value from Synder table 15. The division by (1-ℯ²) is because Apache SIS omits this factor
         * in its calculation of n (we rather take it in account in (de)normalization matrices and elsewhere).
         */
        assertEquals("n", 0.6029035, kernel.nm / (1 - kernel.eccentricitySquared), 0.5E-7);
        /*
         * When stepping into the AlbersEqualArea.Sphere.transform(…) method with a debugger, the expected
         * value of 6378206.4*ρ/(nm/(1-ℯ²)) is 6931335 (value taken from ρ column in Synder table 15).
         */
        verifyTransform(new double[] {0, 50}, new double[] {0, 5356698.435});
        /*
         * Expect 6378206.4*ρ/(nm/(1-ℯ²))  ≈  8042164   (can be verified only with the debugger)
         */
        verifyTransform(new double[] {0, 40}, new double[] {0, 4245869.390});
        /*
         * Expect 6378206.4*ρ/(nm/(1-ℯ²))  ≈  9710969   (can be verified only with the debugger)
         */
        verifyTransform(new double[] {0, 25}, new double[] {0, 2577064.350});
        /*
         * Verify consistency with random points.
         */
        verifyInDomain(new double[] {-20, 20}, new double[] {20, 50}, new int[] {5, 5},
                TestUtilities.createRandomNumberGenerator());
    }

    /**
     * Uses Proj.4 test point has a reference.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    @DependsOnMethod("testEllipse")
    public void compareWithProj4() throws FactoryException, TransformException {
        toleranceModifier = ToleranceModifier.PROJECTION;
        tolerance = Formulas.LINEAR_TOLERANCE;

        // Spherical case
        transform = create(6400000, 6400000, 0, 2);
        verifyTransform(new double[] {2, 1}, new double[] {223334.085, 111780.432});

        // Ellipsoidal case
        transform = create(6378137, 6356752.314140347, 0, 2);
        verifyTransform(new double[] {2, 1}, new double[] {222571.609, 110653.327});
    }
}
