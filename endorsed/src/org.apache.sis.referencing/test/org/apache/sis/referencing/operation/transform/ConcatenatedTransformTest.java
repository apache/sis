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

import java.util.List;
import java.util.Map;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.TestCase.STRICT;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.Assertions;


/**
 * Tests the {@link ConcatenatedTransform} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class ConcatenatedTransformTest extends MathTransformTestCase {
    /**
     * Creates a new test case.
     */
    public ConcatenatedTransformTest() {
    }

    /**
     * Tests the concatenation of two affine transforms that can be represented
     * as a {@link ConcatenatedTransformDirect2D}.
     *
     * @throws TransformException if an error occurred while transforming the test coordinate.
     */
    @Test
    public void testDirect2D() throws TransformException {
        final var first  = new AffineTransform2D(1, 0, 0, 1, 2.00, 4.00);    // translate(2, 4)
        final var second = new AffineTransform2D(1, 0, 0, 1, 0.25, 0.75);    // translate(0.25, 0.75);

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

        // Direct for general case - cannot be validated.
        transform = new ConcatenatedTransformDirect(first, second);
        verifyTransform(source, target);

        // Most general case - cannot be validated.
        transform = new ConcatenatedTransform(first, second);
        verifyTransform(source, target);

        // Optimized case.
        transform = MathTransforms.concatenate(first, second);
        assertInstanceOf(AffineTransform2D.class, transform, "Expected optimized concatenation through matrix multiplication.");
        validate();
        verifyTransform(source, target);
    }

    /**
     * Tests the concatenation of two affine transforms than cannot be represented as a
     * {@link ConcatenatedTransformDirect}. The slower {@link ConcatenatedTransform} shall be used.
     *
     * @throws FactoryException if an error occurred while creating the math transform to test.
     * @throws TransformException if an error occurred while transforming the test coordinate.
     */
    @Test
    public void testGeneric() throws FactoryException, TransformException {
        final MathTransform first = MathTransforms.linear(Matrices.createDimensionSelect(4, new int[] {1,3}));
        final var second = new AffineTransform2D(0.5, 0, 0, 0.25, 0, 0);    // scale(0.5, 0.25);
        transform = new ConcatenatedTransform(first, second);
        isInverseTransformSupported = false;
        validate();
        final double[] source = generateRandomCoordinates(CoordinateDomain.PROJECTED, 0);
        final double[] target = new double[source.length / 2];                  // Going from 4 to 2 dimensions.
        first .transform(source, 0, target, 0, target.length/2);
        second.transform(target, 0, target, 0, target.length/2);
        verifyTransform(source, target);

        // Optimized case.
        transform = MathTransforms.concatenate(first, second);
        assertInstanceOf(ProjectiveTransform.class, transform, "Expected optimized concatenation through matrix multiplication.");
        validate();
        verifyTransform(source, target);
    }

    /**
     * Tests the concatenation of a 3D affine transform with a pass-through transform.
     * The {@link ConcatenatedTransform#create(MathTransformFactory, MathTransform...)}
     * method should optimize this case.
     *
     * @throws FactoryException if an error occurred while creating the math transform to test.
     */
    @Test
    public void testPassthrough() throws FactoryException {
        final MathTransform kernel = new PseudoTransform(2, 3);                     // Any non-linear transform.
        final MathTransform passth = MathTransforms.passThrough(0, kernel, 1);
        final Matrix4 matrix = new Matrix4();
        transform = MathTransforms.concatenate(MathTransforms.linear(matrix), passth);
        assertSame(passth, transform, "Identity transform should be ignored.");
        assertEquals(3, transform.getSourceDimensions());
        assertEquals(4, transform.getTargetDimensions());
        /*
         * Put scale or offset factors only in the dimension to be processed by the sub-transform.
         * The matrix should be concatenated to the sub-transform rather than to the passthrough
         * transform.
         */
        matrix.m00 = 3;
        matrix.m13 = 2;
        transform = MathTransforms.concatenate(MathTransforms.linear(matrix), passth);
        MathTransform subTransform;
        subTransform = assertInstanceOf(PassThroughTransform.class, transform).subTransform;
        subTransform = assertInstanceOf(ConcatenatedTransform.class, subTransform).transform2;
        assertSame(kernel, subTransform);
        assertEquals(3, transform.getSourceDimensions());
        assertEquals(4, transform.getTargetDimensions());
        /*
         * Put scale or offset factors is a passthrough dimension. Now, the affine transform
         * cannot anymore be concatenated with the sub-transform.
         */
        matrix.m22 = 4;
        transform = MathTransforms.concatenate(MathTransforms.linear(matrix), passth);
        final var concat = assertInstanceOf(ConcatenatedTransform.class, transform);
        assertSame(passth, concat.transform2);
        assertEquals(3, transform.getSourceDimensions());
        assertEquals(4, transform.getTargetDimensions());
    }

    /**
     * Test pass-through which is build-in the transform.
     *
     * @throws FactoryException if an error occurred while creating the math transform to test.
     */
    @Test
    public void testBuildinPassthrough() throws FactoryException {
        final var kernel = new PseudoTransform(2, 2) {
            /** Whether the pass-through optimization has been applied. */
            private boolean done;

            /** Tries to apply the pass-through optimization. */
            @Override protected void tryConcatenate(final TransformJoiner context) throws FactoryException {
                if (context.replacePassThrough(Map.of(1, 1))) {
                    assertFalse(done);
                    done = true;
                }
            }
        };
        final Matrix3 before, after;
        before = new Matrix3(0.25,  0,    -45,
                             0,    -0.25,  90,
                             0,     0,      1);

        after  = new Matrix3(1,     0,     45,
                             0,    -1,     90,
                             0,     0,      1);

        transform = MathTransforms.concatenate(MathTransforms.linear(before), kernel, MathTransforms.linear(after));
        final List<MathTransform> steps = MathTransforms.getSteps(transform);
        assertEquals(3, steps.size());
        assertSame(kernel, steps.get(1));
        /*
         * The optimization should have moved the -1 factor from `after` to `before`
         * in order to simplify `after` to a matrix doing only a translation.
         */
        after .m11 = -after .m11;    after .m12 = 0;
        before.m11 = -before.m11;    before.m12 = 0;
        assertMatrixEquals(before, MathTransforms.getMatrix(steps.get(0)), null, "before");
        assertMatrixEquals(after,  MathTransforms.getMatrix(steps.get(2)), null, "after");
    }

    /**
     * Tests concatenation of transforms built from non-square matrices. The transforms are invertible
     * when taken separately, but the transform resulting from concatenation cannot be inverted unless
     * {@link TransformJoiner#simplify(List)} prepares in advance the inverse transform
     * using the inverse of original transforms.
     *
     * @throws NoninvertibleTransformException if a transform cannot be inverted.
     */
    @Test
    public void testNonSquares() throws NoninvertibleTransformException {
        final LinearTransform tr1 = MathTransforms.scale(8, 6, 0.5);
        final LinearTransform tr2 = MathTransforms.linear(Matrices.create(4, 3, new double[] {
            2, 0, 0,        // Scale first dimension.
            0, 3, 0,        // Scale second dimension.
            0, 0, 5,        // Set third dimension to a constant.
            0, 0, 1}));     // Usual row in affine transforms.
        /*
         * Request for a transform going from 3D points to 2D points.
         * Dropping a dimension is not a problem.
         */
        final MathTransform c = MathTransforms.concatenate(tr1, tr2.inverse());
        Assertions.assertMatrixEquals(Matrices.create(3, 4, new double[] {
                    4, 0, 0, 0,     // scale = 8/2
                    0, 2, 0, 0,     // scale = 6/3
                    0, 0, 0, 1
                }), MathTransforms.getMatrix(c), STRICT, "Forward");
        /*
         * Following test is the interesting part. By inverting the transform, we ask for a conversion
         * from 2D points to 3D points. Without contextual information we would not know which value to
         * put in the third dimension (that value would fallback on NaN). But with the knowledge that
         * this concatenation was built from a transform which was putting value 5 in third dimension,
         * we can complete the matrix as below with value 10 in third dimension.
         */
        Assertions.assertMatrixEquals(Matrices.create(4, 3, new double[] {
                    0.25, 0,    0,
                    0,    0.5,  0,
                    0,    0,   10,   // Having value 10 instead of NaN is the main purpose of this test.
                    0,    0,    1
                }), MathTransforms.getMatrix(c.inverse()), STRICT, "Inverse");
    }

    /**
     * Tests a concatenation between transforms having (indirectly) infinite coefficients.
     * This test uses a transform with a coefficient close enough to zero for causing the
     * inverse matrix to have infinite values. If the coefficient was strictly zero, a
     * {@link org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException}
     * would have been thrown. But with non-zero coefficient small enough, the exception
     * is not thrown and infinite values may confuse {@link ConcatenatedTransform} if not
     * properly handled.
     */
    @Test
    public void testInfinities() {
        final MathTransform tr1 = MathTransforms.linear(new Matrix2(4.9E-324, -5387, 0, 1));
        final MathTransform tr2 = MathTransforms.linear(new Matrix2(-1, 0, 0, 1));
        final MathTransform c   = MathTransforms.concatenate(tr1, tr2);
        final Matrix m          = ((LinearTransform) c).getMatrix();
        Assertions.assertMatrixEquals(new Matrix2(-4.9E-324, 5387, 0, 1), m, STRICT, "Concatenate");
    }
}
