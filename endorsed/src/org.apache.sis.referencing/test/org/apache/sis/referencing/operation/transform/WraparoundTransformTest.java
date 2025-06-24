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
import java.util.List;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.privy.WraparoundApplicator;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.cs.AxesConvention;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertMatrixEquals;


/**
 * Tests {@link WraparoundTransform}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WraparoundTransformTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public WraparoundTransformTest() {
    }

    /**
     * Tests {@link WraparoundTransform#inverse()}.
     *
     * @throws TransformException if a coordinate cannot be computed.
     */
    @Test
    public void testInverse() throws TransformException {
        /*
         * Source range: [ 20 … 380]°.
         * Target range: [-90 … 270]°
         */
        final MathTransform forward = WraparoundTransform.create(3, 1, 360, 200, 90);
        final MathTransform inverse = forward.inverse();
        assertSame(forward, inverse.inverse());
        assertSame(inverse, forward.inverse());             // Expect cached instance.
        final double[] sources = new double[] {
            45, -100, 90,
            60,  -80, 70,
            12,  300, 33
        };
        final double[] targets = new double[9];
        forward.transform(sources, 0, targets, 0, 3);
        assertArrayEquals(new double[] {
            45, -100 + 360, 90,             // Wraparound shall be applied.
            60,  -80,       70,             // No wraparound because already in [-90 … 270]° range.
            12,  300 - 360, 33              // Wraparound in opposite direction.
        }, targets);

        inverse.transform(sources, 0, targets, 0, 3);
        assertArrayEquals(new double[] {
            45, -100 + 360, 90,             // Wraparound shall be applied.
            60,  -80 + 360, 70,             // Idem.
            12,  300,       33,             // No wraparound because already in [ 20 … 380]° range.
        }, targets);
    }

    /**
     * Tests wraparound on one axis.
     *
     * @throws TransformException if a coordinate cannot be computed.
     */
    @Test
    public void testOneAxis() throws TransformException {
        final var op = new AbstractCoordinateOperation(
                Map.of(AbstractCoordinateOperation.NAME_KEY, "Wrapper"),
                HardCodedCRS.WGS84_LATITUDE_FIRST,
                HardCodedCRS.WGS84_LATITUDE_FIRST.forConvention(AxesConvention.POSITIVE_RANGE),
                null, MathTransforms.scale(3, 5));
        /*
         * Transform should be  [scale & normalization]  →  [wraparound]  →  [denormalization].
         * The wraparound is applied on target coordinates, which is why it appears after [scale].
         * Wrararound is often (but not always) unnecessary on source coordinates if the operation
         * uses trigonometric functions.
         */
        final MathTransform wt = WraparoundApplicator.forTargetCRS(op);
        final List<MathTransform> steps = MathTransforms.getSteps(wt);
        assertEquals(3, steps.size());
        assertEquals(1, ((WraparoundTransform) steps.get(1)).wraparoundDimension);
        /*
         * WraparoundTransform outputs are in [−180 … 180] range, so we expect
         * a 180° shift for getting results in the [0 … 360]° range.
         */
        assertMatrixEquals(new Matrix3(1,  0,    0,        // Latitude (no wrap around)
                                       0,  1,  180,        // Longitude in [0 … 360] range.
                                       0,  0,    1),
                MathTransforms.getMatrix(steps.get(2)), STRICT, "denormalize");
        /*
         * The normalization is the inverse of above matrix.
         * But we expect the normalization matrix to be concatenated with the (3, 5) scale operation.
         */
        assertMatrixEquals(new Matrix3(3,  0,    0,         // 3 is a factor in MathTransforms.scale(…).
                                       0,  5, -180,         // 5 is (idem).
                                       0,  0,    1),
                MathTransforms.getMatrix(steps.get(0)), STRICT, "normalize");
        /*
         * Test transforming some points.
         */
        final double[] pts = {
            2, -100/5,
            6, -200/5,
            9,  200/5,
            3,  400/5};
        wt.transform(pts, 0, pts, 0, 4);
        assertArrayEquals(new double[] {
             6, 260,
            18, 160,
            27, 200,
             9,  40}, pts);
    }

    /**
     * Tests wraparound on two axes. We expect two instances of {@link WraparoundTransform} without linear
     * transform between them. The absence of separation between the two {@link WraparoundTransform}s is an
     * indirect test of {@link WraparoundTransform#tryConcatenate(TransformJoiner)}.
     *
     * @throws TransformException if a coordinate cannot be computed.
     */
    @Test
    public void testTwoAxes() throws TransformException {
        final var op = new AbstractCoordinateOperation(
                Map.of(AbstractCoordinateOperation.NAME_KEY, "Wrapper"),
                HardCodedCRS.WGS84_WITH_TIME.forConvention(AxesConvention.POSITIVE_RANGE),
                HardCodedCRS.WGS84_WITH_CYCLIC_TIME, null, MathTransforms.scale(3, 2, 5));
        /*
         * Transform should be  [scale & normalization]  →  [wraparound 1]  →  [wraparound 2]  →  [denormalization].
         * At first an affine transform existed between the two [wraparound] operations, but that affine transform
         * should have been moved by `WraparoundTransform.tryConcatenate(…)` in order to combine them with initial
         * [normalization] and final [denormalization].
         */
        final MathTransform wt = WraparoundApplicator.forTargetCRS(op);
        final List<MathTransform> steps = MathTransforms.getSteps(wt);
        assertEquals(4, steps.size());
        assertEquals(0, ((WraparoundTransform) steps.get(1)).wraparoundDimension);
        assertEquals(2, ((WraparoundTransform) steps.get(2)).wraparoundDimension);
        /*
         * WraparoundTransform outputs are in [−180 … 180] range in longitude case,
         * so we expect a 180° shift for getting results in the [0 … 360]° range.
         */
        assertMatrixEquals(new Matrix4(1,   0,   0,   0,        // Longitude in [-180 … 180] range.
                                       0,   1,   0,   0,        // Latitude (no wrap around)
                                       0,   0,   1, 183.5,      // Day of year in [1 … 366] range.
                                       0,   0,   0,   1),
                MathTransforms.getMatrix(steps.get(3)), STRICT, "denormalize");
        /*
         * The normalization is the inverse of above matrix (when source and target axes have the same span).
         * But we expect the normalization matrix to be concatenated with the (3, 2, 5) scale operation.
         */
        assertMatrixEquals(new Matrix4(3,   0,   0,    0,       // 3 is a factor in MathTransforms.scale(…).
                                       0,   2,   0,    0,       // 2 is (idem).
                                       0,   0,   5, -183.5,     // 5 is (idem).
                                       0,   0,   0,    1),
                MathTransforms.getMatrix(steps.get(0)), 1E-15, "normalize");
    }
}
