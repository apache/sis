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
package org.apache.sis.internal.referencing;

import java.util.List;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link WraparoundTransform}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class WraparoundTransformTest extends TestCase {
    /**
     * Tests {@link WraparoundTransform} cache.
     */
    @Test
    public void testCache() {
        final WraparoundTransform t1, t2, t3, t4;
        assertSame   (WraparoundTransform.create(3, 0), t1 = WraparoundTransform.create(3, 0));
        assertNotSame(WraparoundTransform.create(3, 0), t2 = WraparoundTransform.create(3, 1));
        assertNotSame(WraparoundTransform.create(3, 0), t3 = WraparoundTransform.create(2, 0));
        assertNotSame(WraparoundTransform.create(3, 0), t4 = WraparoundTransform.create(3, 2));
        assertEquals(3, t1.getSourceDimensions());
        assertEquals(3, t2.getSourceDimensions());
        assertEquals(2, t3.getSourceDimensions());
        assertEquals(3, t4.getSourceDimensions());
        assertEquals(0, t1.wraparoundDimension);
        assertEquals(1, t2.wraparoundDimension);
        assertEquals(0, t3.wraparoundDimension);
        assertEquals(2, t4.wraparoundDimension);
    }

    /**
     * Tests wraparound on one axis.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws NoninvertibleMatrixException if the expected matrix can not be inverted.
     */
    @Test
    public void testOneAxis() throws FactoryException, NoninvertibleMatrixException {
        final AbstractCoordinateOperation op = new AbstractCoordinateOperation(
                Collections.singletonMap(AbstractCoordinateOperation.NAME_KEY, "Wrapper"),
                HardCodedCRS.WGS84_φλ.forConvention(AxesConvention.POSITIVE_RANGE),
                HardCodedCRS.WGS84_φλ, null, MathTransforms.scale(3, 5));
        /*
         * Transform should be  [scale & normalization]  →  [wraparound]  →  [denormalization].
         * The wraparound is applied on target coordinates, which is why it appears after [scale].
         * Wrararound is often (but not always) unnecessary on source coordinates if the operation
         * uses trigonometric functions.
         */
        final MathTransform wt = WraparoundTransform.forTargetCRS(DefaultFactories.forClass(MathTransformFactory.class), op);
        final List<MathTransform> steps = MathTransforms.getSteps(wt);
        assertEquals(3, steps.size());
        assertEquals(1, ((WraparoundTransform) steps.get(1)).wraparoundDimension);
        /*
         * Wraparound outputs are in [0 … 1) range (0 inclusive and 1 exclusive), so we expect a
         * multiplication by the span of each axis for getting the final range.
         */
        assertMatrixEquals("denormalize", new Matrix3(
                1,   0,    0,                           // Latitude (no wrap around)
                0, 360, -180,                           // Longitude in [-180 … 180) range.
                0,   0,    1),
                MathTransforms.getMatrix(steps.get(2)), STRICT);
        /*
         * The normalization is the inverse of above matrix (when source and target axes have the same span).
         * But we expect the normalization matrix to be concatenated with the (3, 2, 5) scale operation.
         */
        assertMatrixEquals("normalize", new Matrix3(
                3,      0,            0,                // 3 is a factor in MathTransforms.scale(…).
                0, 5./360,  -(-180./360),               // 5 is (idem).
                0,      0,            1),
                MathTransforms.getMatrix(steps.get(0)), 1E-15);
    }

    /**
     * Tests wraparound on two axes. We expects two instances of {@link WraparoundTransform} without linear
     * transform between them. The absence of separation between the two {@link WraparoundTransform}s is an
     * indirect test of {@link WraparoundTransform#tryConcatenate(boolean, MathTransform, MathTransformFactory)}.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws NoninvertibleMatrixException if the expected matrix can not be inverted.
     */
    @Test
    public void testTwoAxes() throws FactoryException, NoninvertibleMatrixException {
        final AbstractCoordinateOperation op = new AbstractCoordinateOperation(
                Collections.singletonMap(AbstractCoordinateOperation.NAME_KEY, "Wrapper"),
                HardCodedCRS.WGS84_3D_TIME.forConvention(AxesConvention.POSITIVE_RANGE),
                HardCodedCRS.WGS84_3D_TIME_CYCLIC, null, MathTransforms.scale(3, 2, 5));
        /*
         * Transform should be  [scale & normalization]  →  [wraparound 1]  →  [wraparound 2]  →  [denormalization].
         * At first an affine transform existed between the two [wraparound] operations, but that affine transform
         * should have been moved by `WraparoundTransform.tryConcatenate(…)` in order to combine them with initial
         * [normalization} and final {denormalization].
         */
        final MathTransform wt = WraparoundTransform.forTargetCRS(DefaultFactories.forClass(MathTransformFactory.class), op);
        final List<MathTransform> steps = MathTransforms.getSteps(wt);
        assertEquals(4, steps.size());
        assertEquals(0, ((WraparoundTransform) steps.get(1)).wraparoundDimension);
        assertEquals(2, ((WraparoundTransform) steps.get(2)).wraparoundDimension);
        /*
         * Wraparound outputs are in [0 … 1) range (0 inclusive and 1 exclusive), so we expect a
         * multiplication by the span of each axis for getting the final range.
         */
        assertMatrixEquals("denormalize", new Matrix4(
                360,   0,   0, -180,                        // Longitude in [-180 … 180) range.
                  0,   1,   0,    0,                        // Latitude (no wrap around)
                  0,   0, 365,    1,                        // Day of year in [1 … 366) range.
                  0,   0,   0,    1),
                MathTransforms.getMatrix(steps.get(3)), STRICT);
        /*
         * The normalization is the inverse of above matrix (when source and target axes have the same span).
         * But we expect the normalization matrix to be concatenated with the (3, 2, 5) scale operation.
         */
        assertMatrixEquals("normalize", new Matrix4(
                3./360,  0,       0,  -(-180./360),         // 3 is a factor in MathTransforms.scale(…).
                    0,   2,       0,            0,          // 2 is (idem).
                    0,   0,  5./365,     -(1./365),         // 5 is (idem).
                    0,   0,       0,            1),
                MathTransforms.getMatrix(steps.get(0)), 1E-15);
    }
}
