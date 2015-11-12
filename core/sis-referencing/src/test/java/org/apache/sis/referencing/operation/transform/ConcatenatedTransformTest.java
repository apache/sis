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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the {@link ConcatenatedTransform} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 */
@DependsOn(ProjectiveTransformTest.class)
public final strictfp class ConcatenatedTransformTest extends MathTransformTestCase {
    /**
     * Tests the concatenation of two affine transforms than can be represented
     * as a {@link ConcatenatedTransformDirect2D}.
     *
     * @throws TransformException Should never happen.
     */
    @Test
    public void testDirect2D() throws TransformException {
        final AffineTransform2D first  = new AffineTransform2D(1, 0, 0, 1, 2.00, 4.00);    // translate(2, 4)
        final AffineTransform2D second = new AffineTransform2D(1, 0, 0, 1, 0.25, 0.75);    // translate(0.25, 0.75);

        // Direct for 2D case.
        tolerance = 1E-10;
        transform = new ConcatenatedTransformDirect2D(first, second);
        validate();
        final double[] source = generateRandomCoordinates(CoordinateDomain.PROJECTED, 0);
        final double[] target = new double[source.length];
        first .transform(source, 0, target, 0, source.length/2);
        second.transform(target, 0, target, 0, target.length/2);
        verifyTransform(source, target);

        // Non-direct for 2D case.
        transform = new ConcatenatedTransform2D(first, second);
        validate();
        verifyTransform(source, target);

        // Direct for general case - can't be validated.
        transform = new ConcatenatedTransformDirect(first, second);
        verifyTransform(source, target);

        // Most general case - can't be validated.
        transform = new ConcatenatedTransform(first, second);
        verifyTransform(source, target);

        // Optimized case.
        transform = MathTransforms.concatenate(first, second);
        assertInstanceOf("Expected optimized concatenation through matrix multiplication.", AffineTransform2D.class, transform);
        validate();
        verifyTransform(source, target);
    }

    /**
     * Tests the concatenation of two affine transforms than can not be represented as a
     * {@link ConcatenatedTransformDirect}. The slower {@link ConcatenatedTransform} shall be used.
     *
     * @throws FactoryException Should never happen.
     * @throws TransformException Should never happen.
     */
    @Test
    @org.junit.Ignore("Missing implementation of DimensionFilter.")
    public void testGeneric() throws FactoryException, TransformException {
        final MathTransform first = null; //MathTransforms.dimensionFilter(4, new int[] {1,3});

        final AffineTransform2D second = new AffineTransform2D(0.5, 0, 0, 0.25, 0, 0);  // scale(0.5, 0.25);

        transform = new ConcatenatedTransform(first, second);
        isInverseTransformSupported = false;
        validate();
        final double[] source = generateRandomCoordinates(CoordinateDomain.PROJECTED, 0);
        final double[] target = new double[source.length / 2]; // Going from 4 to 2 dimensions.
        first .transform(source, 0, target, 0, target.length/2);
        second.transform(target, 0, target, 0, target.length/2);
        verifyTransform(source, target);

        // Optimized case.
        transform = ConcatenatedTransform.create(first, second, null);
        assertInstanceOf("Expected optimized concatenation through matrix multiplication.", ProjectiveTransform.class, transform);
        validate();
        verifyTransform(source, target);
    }

    /**
     * Tests the concatenation of a 3D affine transform with a pass-through transform.
     * The {@link ConcatenatedTransform#create(MathTransform, MathTransform)} method
     * should optimize this case.
     *
     * @throws FactoryException Should never happen.
     */
    @Test
    public void testPassthrough() throws FactoryException {
        final MathTransform kernel = new PseudoTransform(2, 3); // Any non-linear transform.
        final MathTransform passth = PassThroughTransform.create(0, kernel, 1);
        final Matrix4 matrix = new Matrix4();
        transform = ConcatenatedTransform.create(MathTransforms.linear(matrix), passth, null);
        assertSame("Identity transform should be ignored.", passth, transform);
        assertEquals("Source dimensions", 3, transform.getSourceDimensions());
        assertEquals("Target dimensions", 4, transform.getTargetDimensions());
        /*
         * Put scale or offset factors only in the dimension to be processed by the sub-transform.
         * The matrix should be concatenated to the sub-transform rather than to the passthrough
         * transform.
         */
        matrix.m00 = 3;
        matrix.m13 = 2;
        transform = ConcatenatedTransform.create(MathTransforms.linear(matrix), passth, null);
        assertInstanceOf("Expected a new passthrough transform.", PassThroughTransform.class, transform);
        final MathTransform subTransform = ((PassThroughTransform) transform).subTransform;
        assertInstanceOf("Expected a new concatenated transform.", ConcatenatedTransform.class, subTransform);
        assertSame(kernel, ((ConcatenatedTransform) subTransform).transform2);
        assertEquals("Source dimensions", 3, transform.getSourceDimensions());
        assertEquals("Target dimensions", 4, transform.getTargetDimensions());
        /*
         * Put scale or offset factors is a passthrough dimension. Now, the affine transform
         * can not anymore be concatenated with the sub-transform.
         */
        matrix.m22 = 4;
        transform = ConcatenatedTransform.create(MathTransforms.linear(matrix), passth, null);
        assertInstanceOf("Expected a new concatenated transform.", ConcatenatedTransform.class, transform);
        assertSame(passth, ((ConcatenatedTransform) transform).transform2);
        assertEquals("Source dimensions", 3, transform.getSourceDimensions());
        assertEquals("Target dimensions", 4, transform.getTargetDimensions());
    }
}
