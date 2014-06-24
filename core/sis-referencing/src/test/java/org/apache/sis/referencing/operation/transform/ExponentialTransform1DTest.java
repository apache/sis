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

import java.util.EnumSet;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import static java.lang.StrictMath.*;

// Test imports
import org.junit.Test;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import static org.opengis.test.Assert.*;


/**
 * Tests the {@link ExponentialTransform1D} class. This test case will also tests
 * indirectly the {@link LogarithmicTransform1D} class since it is the inverse of
 * the exponential transform.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-3.17)
 * @version 0.5
 * @module
 */
@DependsOn({
    CoordinateDomainTest.class,
    LinearTransformTest.class
})
public final strictfp class ExponentialTransform1DTest extends MathTransformTestCase {
    /**
     * Arbitrary parameters of the exponential transform to be tested.
     */
    private static final double BASE = 10, SCALE = 2;

    /**
     * Arbitrary coefficients of a linear transform to be concatenated to the exponential transform.
     */
    private static final double C0 = -3, C1 = 0.25;

    /**
     * The random values to use as input, and the expected transformed values.
     */
    private double[] values, expected;

    /**
     * Generates random values for input coordinates, and allocates (but do not compute values)
     * array for the expected transformed coordinates.
     *
     * @param mt The math transform which will be tested.
     */
    private void initialize(final MathTransform1D mt) {
        transform         = mt; // Must be set before generateRandomCoordinates(…).
        tolerance         = 1E-5; // Tolerance is much smaller on other branches.
//      toleranceModifier = ToleranceModifier.RELATIVE; // Not available on GeoAPI 3.0.
        values            = generateRandomCoordinates(CoordinateDomain.RANGE_10, 0);
        expected          = new double[values.length];
    }

    /**
     * Tests the current transform using the {@link #values} as input points, and comparing with
     * the {@link #expected} values.
     */
    private void run(final Class<? extends MathTransform1D> expectedType) throws TransformException {
        assertInstanceOf("Expected the use of mathematical identities.", expectedType, transform);
        assertFalse(transform.isIdentity());
        validate();
        verifyTransform(values, expected);
    }

    /**
     * A single (non-concatenated) test case.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSingle() throws TransformException {
        initialize(ExponentialTransform1D.create(BASE, SCALE));
        for (int i=0; i<values.length; i++) {
            expected[i] = SCALE * pow(BASE, values[i]);
        }
        run(ExponentialTransform1D.class);
    }

    /**
     * Tests the concatenation of a linear operation before the exponential one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingle")
    public void testAffinePreConcatenation() throws TransformException {
        initialize(MathTransforms.concatenate(
                   LinearTransform1D.create(C1, C0),
                   ExponentialTransform1D.create(BASE, SCALE)));
        for (int i=0; i<values.length; i++) {
            expected[i] = SCALE * pow(BASE, C0 + C1 * values[i]);
        }
        run(ExponentialTransform1D.class);
        /*
         * Find back the original linear coefficients as documented in the ExponentialTransform1D class javadoc.
         */
        final double lnBase =  log(BASE);
        final double offset = -log(SCALE) / lnBase;
        final MathTransform1D log = LogarithmicTransform1D.create(BASE, offset);
        for (int i=0; i<values.length; i++) {
            expected[i] = log(expected[i]) / lnBase + offset;
        }
        transform = (LinearTransform1D) MathTransforms.concatenate(transform, log);
        run(LinearTransform1D.class);
        assertEquals(C1, ((LinearTransform1D) transform).scale,  1E-12);
        assertEquals(C0, ((LinearTransform1D) transform).offset, 1E-12);
    }

    /**
     * Tests the concatenation of a linear operation after the exponential one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingle")
    public void testAffinePostConcatenation() throws TransformException {
        initialize(MathTransforms.concatenate(
                   ExponentialTransform1D.create(BASE, SCALE),
                   LinearTransform1D.create(C1, C0)));
        for (int i=0; i<values.length; i++) {
            expected[i] = C0 + C1 * (SCALE * pow(BASE, values[i]));
        }
        run(ConcatenatedTransformDirect1D.class);
    }

    /**
     * Tests the concatenation of a logarithmic operation with the exponential one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testSingle")
    public void testLogarithmicConcatenation() throws TransformException {
        final double base   = 8; // Must be different than BASE.
        final double lnBase = log(base);
        initialize(MathTransforms.concatenate(
                   LogarithmicTransform1D.create(base, C0),
                   ExponentialTransform1D.create(BASE, SCALE)));
        for (int i=0; i<values.length; i++) {
            values[i] = abs(values[i]) + 0.001;
            expected[i] = SCALE * pow(BASE, log(values[i]) / lnBase + C0);
        }
        run(ConcatenatedTransformDirect1D.class);
    }
}
