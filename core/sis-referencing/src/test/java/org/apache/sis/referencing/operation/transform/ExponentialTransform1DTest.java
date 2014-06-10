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

import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.StrictMath.*;


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
@DependsOn(LinearTransformTest.class)
public final strictfp class ExponentialTransform1DTest extends MathTransformTestCase {
    /**
     * Arbitrary coefficients used for this test.
     */
    private static final double BASE = 10, SCALE = 2, C0 = -3, C1 = 0.25;

    /**
     * A simple (non-concatenated) test case.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSimple() throws TransformException {
        transform = ExponentialTransform1D.create(BASE, SCALE);
        validate();
        assertFalse(transform.isIdentity());
        final double[] source = generateRandomCoordinates(CoordinateDomain.GAUSSIAN, 0);
        final double[] target = new double[source.length];
        for (int i=0; i<source.length; i++) {
            target[i] = SCALE * pow(BASE, source[i]);
        }
        tolerance = 1E-12;
        verifyTransform(source, target);
        /*
         * Tests the derivative at a single point.
         */
        tolerance = 0.002;
        derivativeDeltas = new double[] {0.001};
        verifyDerivative(2.5);
    }

    /**
     * Tests the concatenation of a linear operation before the exponential one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testAffinePreConcatenation() throws TransformException {
        transform = MathTransforms.concatenate(
                LinearTransform1D.create(C1, C0),
                ExponentialTransform1D.create(BASE, SCALE));
        validate();
        assertFalse(transform.isIdentity());
        assertTrue("Expected mathematical identities.", transform instanceof ExponentialTransform1D);
        final double[] source = generateRandomCoordinates(CoordinateDomain.GAUSSIAN, 0);
        final double[] target = new double[source.length];
        for (int i=0; i<source.length; i++) {
            target[i] = SCALE * pow(BASE, C0 + C1 * source[i]);
        }
        tolerance = 1E-14;
        verifyTransform(source, target);
        /*
         * Tests the derivative at a single point.
         */
        tolerance = 1E-9;
        derivativeDeltas = new double[] {0.001};
        verifyDerivative(2.5);
        /*
         * Find back the original linear coefficients as documented in the ExpentionalTransform1D
         * class javadoc. Then check that the transform results are the expected ones.
         */
        final double lnBase =  log(BASE);
        final double offset = -log(SCALE) / lnBase;
        final MathTransform1D log = LogarithmicTransform1D.create(BASE, offset);
        transform = (LinearTransform1D) MathTransforms.concatenate(transform, log);
        assertTrue("Expected mathematical identities.", transform instanceof LinearTransform1D);
        assertEquals(C1, ((LinearTransform1D) transform).scale,  1E-12);
        assertEquals(C0, ((LinearTransform1D) transform).offset, 1E-12);
        for (int i=0; i<source.length; i++) {
            target[i] = log(target[i]) / lnBase + offset;
        }
        tolerance = 1E-14;
        verifyTransform(source, target);
    }

    /**
     * Tests the concatenation of a linear operation after the exponential one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testAffinePostConcatenation() throws TransformException {
        transform = MathTransforms.concatenate(
                ExponentialTransform1D.create(BASE, SCALE),
                LinearTransform1D.create(C1, C0));
        validate();
        assertFalse(transform.isIdentity());
        final double[] source = generateRandomCoordinates(CoordinateDomain.GAUSSIAN, 0);
        final double[] target = new double[source.length];
        for (int i=0; i<source.length; i++) {
            target[i] = C0 + C1 * (SCALE * pow(BASE, source[i]));
        }
        tolerance = 1E-12;
        verifyTransform(source, target);
        /*
         * Tests the derivative at a single point.
         */
        tolerance = 0.01;
        derivativeDeltas = new double[] {0.001};
        verifyDerivative(2.5);
    }

    /**
     * Tests the concatenation of a logarithmic operation with the exponential one.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testLogarithmicConcatenation() throws TransformException {
        final double offset = -3;
        final double base   = 8;
        final double lnBase = log(base);
        transform = MathTransforms.concatenate(
                LogarithmicTransform1D.create(base, offset),
                ExponentialTransform1D.create(BASE, SCALE));
        validate();
        assertFalse(transform.isIdentity());
        final double[] source = generateRandomCoordinates(CoordinateDomain.GAUSSIAN, 0);
        final double[] target = new double[source.length];
        for (int i=0; i<source.length; i++) {
            source[i] = abs(source[i]) + 0.001;
            target[i] = SCALE * pow(BASE, log(source[i]) / lnBase + offset);
        }
        tolerance = 1E-14;
        verifyTransform(source, target);
        /*
         * Tests the derivative at a single point.
         */
        tolerance = 1E-10;
        derivativeDeltas = new double[] {0.001};
        verifyDerivative(2.5);
    }
}
