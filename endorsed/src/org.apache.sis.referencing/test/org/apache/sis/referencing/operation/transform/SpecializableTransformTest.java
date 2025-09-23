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

import java.util.Map;
import java.util.HashMap;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.geometry.Envelope2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link SpecializableTransform}. This test use a simple affine transform that multiply
 * coordinate values by 10, except in specialized sub-areas in which case a small translation is added.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SpecializableTransformTest extends MathTransformTestCase {
    /**
     * Creates a new test case.
     */
    public SpecializableTransformTest() {
    }

    /**
     * Creates a transform scaling the coordinate values by 10, then applying the given translation.
     */
    private static MathTransform translation(final double t) {
        return new AffineTransform2D(10, 0, 0, 10, t, t);
    }

    /**
     * Creates a transform to test.
     *
     * @throws IllegalArgumentException if {@link SpecializableTransform} constructor rejects a parameter.
     */
    private static SpecializableTransform create(final boolean is2D) {
        final Map<Envelope,MathTransform> specializations = new HashMap<>(4);
        assertNull(specializations.put(new Envelope2D(null, -5, -4, 10, 7), translation(0.1)));
        assertNull(specializations.put(new Envelope2D(null, -3, -1,  5, 2), translation(0.2)));
        final MathTransform global = translation(0);
        if (is2D) {
            return new SpecializableTransform2D(global, specializations);
        }
        return new SpecializableTransform(global, specializations);
    }

    /**
     * Invokes {@link #verifyDerivative(double...)} for all points in the given array.
     */
    private void verifyDerivatives(final double[] coordinates) throws TransformException {
        tolerance = 1E-10;
        derivativeDeltas = new double[] {0.001};
        final double[] point = new double[2];
        for (int i=0; i < coordinates.length; i += 2) {
            System.arraycopy(coordinates, i, point, 0, 2);
            verifyDerivative(point);
        }
    }

    /**
     * Verifies the transform with a few hard-coded points. The point are selected in order to avoid
     * situations where two transforms could calculate the same point (because of the way we created
     * our test transform).
     *
     * @throws IllegalArgumentException if {@link SpecializableTransform} constructor rejects a parameter.
     * @throws TransformException if a transformation failed.
     */
    @Test
    public void testTransform() throws TransformException {
        verifyNonAmbiguousPoints(false);
    }

    /**
     * Verifies the transformation using two-dimensional variant.
     *
     * @throws IllegalArgumentException if {@link SpecializableTransform} constructor rejects a parameter.
     * @throws TransformException if a transformation failed.
     */
    @Test
    public void testTransform2D() throws TransformException {
        verifyNonAmbiguousPoints(true);
    }

    /**
     * Implementation of {@link #testTransform()} and {@link #testTransform2D()}.
     */
    private void verifyNonAmbiguousPoints(final boolean is2D) throws TransformException {
        //                      ┌─── global ───┐┌───── Special. 1 ──────┐┌──── Special. 2 ─────┐
        final double[] source = {8,  2,  4,  5,  3,    2,    2,    -2,    -2,    0,   1,    0,   8,  3};
        final double[] target = {80, 20, 40, 50, 30.1, 20.1, 20.1, -19.9, -19.8, 0.2, 10.2, 0.2, 80, 30};

        tolerance = 1E-14;
        transform = create(is2D);
        verifyTransform(source, target);
        verifyDerivatives(source);

        tolerance = 1E-14;
        transform = transform.inverse();
        verifyTransform(target, source);
        verifyDerivatives(target);
    }

    /**
     * Tests consistency between different {@code transform(…)} methods in forward transforms.
     * This test uses a fixed sequence of random numbers. We fix the sequence because the transform
     * used in this test is {@linkplain org.apache.sis.math.FunctionProperty#SURJECTIVE surjective}:
     * some target coordinates can be created from more than one source coordinates. We need to be
     * "lucky" enough for not testing a point in this case, otherwise the result is undetermined.
     *
     * @throws IllegalArgumentException if {@link SpecializableTransform} constructor rejects a parameter.
     * @throws TransformException if a transformation failed.
     */
    @Test
    public void testForwardConsistency() throws TransformException {
        transform = create(false);
        tolerance = 1E-14;
        isDerivativeSupported = false;          // Actually supported, but our test transform has discontinuities.
        verifyInDomain(CoordinateDomain.RANGE_10, -672445632505596619L);
    }

    /**
     * Tests consistency between different {@code transform(…)} methods in inverse transforms.
     * This test uses a fixed sequence of random numbers. We fix the sequence because the transform
     * used in this test is {@linkplain org.apache.sis.math.FunctionProperty#SURJECTIVE surjective}.
     * See {@link #testForwardConsistency()}.
     *
     * @throws IllegalArgumentException if {@link SpecializableTransform} constructor rejects a parameter.
     * @throws TransformException if a transformation failed.
     */
    @Test
    public void testInverseConsistency() throws TransformException {
        transform = create(false).inverse();
        tolerance = 1E-12;
        isDerivativeSupported = false;          // Actually supported, but our test transform has discontinuities.
        verifyInDomain(CoordinateDomain.RANGE_100, 4308397764777385180L);
    }

    /**
     * Tests the pseudo Well-Known Text formatting.
     * The format used by this transform is non-standard and may change in any future Apache SIS version.
     *
     * @throws IllegalArgumentException if {@link SpecializableTransform} constructor rejects a parameter.
     */
    @Test
    public void testWKT() {
        transform = create(false);
        assertWktEquals(
                "SPECIALIZABLE_MT[\n" +
                "  PARAM_MT[“Affine”,\n" +
                "    PARAMETER[“elt_0_0”, 10.0],\n" +
                "    PARAMETER[“elt_1_1”, 10.0]],\n" +
                "  DOMAIN[-5 -4,\n" +
                "          5  3],\n" +
                "  PARAM_MT[“Affine”,\n" +
                "    PARAMETER[“elt_0_0”, 10.0],\n" +
                "    PARAMETER[“elt_0_2”, 0.1],\n" +
                "    PARAMETER[“elt_1_1”, 10.0],\n" +
                "    PARAMETER[“elt_1_2”, 0.1]],\n" +
                "  DOMAIN[-3 -1,\n" +
                "          2  1],\n" +
                "  PARAM_MT[“Affine”,\n" +
                "    PARAMETER[“elt_0_0”, 10.0],\n" +
                "    PARAMETER[“elt_0_2”, 0.2],\n" +
                "    PARAMETER[“elt_1_1”, 10.0],\n" +
                "    PARAMETER[“elt_1_2”, 0.2]]]");
    }
}
