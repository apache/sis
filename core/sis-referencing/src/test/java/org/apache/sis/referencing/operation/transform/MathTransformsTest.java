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
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.opengis.test.Assert.assertMatrixEquals;


/**
 * Tests {@link MathTransforms}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.5
 */
@DependsOn(org.apache.sis.referencing.operation.matrix.MatricesTest.class)
public final class MathTransformsTest extends TestCase {
    /**
     * Creates a dummy transform for testing purpose.
     * The transform has the following properties:
     *
     * <ul>
     *   <li>The source and target dimensions are 3.</li>
     *   <li>The transform contains 3 step.</li>
     *   <li>The second step is a {@link PassThroughTransform}.</li>
     *   <li>The transform in the middle (at dimension 1) is non-linear.</li>
     * </ul>
     *
     * @return the dummy math transform.
     */
    public static MathTransform createConcatenateAndPassThrough() {
        return createConcatenateAndPassThrough(new Matrix4(), new Matrix4());
    }

    /**
     * Creates a dummy transform for testing purpose.
     * The {@code scale} and {@code scap} arguments are initially identity matrices,
     * and will be written by this method with dummy coefficient values for testing purpose.
     *
     * <p><b>Implementation note:</b> we do not use {@code MathTransforms.concatenate(…)} for
     * preventing the optimization performed for the {@link PassThroughTransform} special case.</p>
     *
     * @param  scale  the matrix applying a scale along at least one axis.
     * @param  swap   the matrix swapping two matrices.
     */
    private static MathTransform createConcatenateAndPassThrough(final Matrix4 scale, final Matrix4 swap) {
        scale.m11 = 3;
        swap.m00 = 0; swap.m01 = 1;
        swap.m10 = 1; swap.m11 = 0;
        MathTransform tr = ExponentialTransform1D.create(10, 1);
        tr = MathTransforms.passThrough(1, tr, 1);
        tr = new ConcatenatedTransformDirect(MathTransforms.linear(scale), tr);     // See "implementation note" above.
        tr = new ConcatenatedTransformDirect(tr, MathTransforms.linear(swap));
        return tr;
    }

    /**
     * Tests {@link MathTransforms#getSteps(MathTransform)}.
     */
    @Test
    public void testGetSteps() {
        final Matrix4 scale = new Matrix4();                    // Scales a value.
        final Matrix4 swap  = new Matrix4();                    // Swaps two dimensions.
        final List<MathTransform> steps = MathTransforms.getSteps(createConcatenateAndPassThrough(scale, swap));
        assertEquals(3, steps.size());
        assertMatrixEquals("Step 1", scale, MathTransforms.getMatrix(steps.get(0)), STRICT);
        assertMatrixEquals("Step 3", swap,  MathTransforms.getMatrix(steps.get(2)), STRICT);
        assertInstanceOf  ("Step 2", PassThroughTransform.class, steps.get(1));
    }

    /**
     * Tests {@link MathTransforms#compound(MathTransform...)}.
     * This test uses linear transforms because they are easy to test, but the
     * {@code MathTransforms.compound(…)} method should work with any transforms.
     */
    @Test
    public void testCompound() {
        final MathTransform t1 = MathTransforms.linear(new Matrix2(
            3, -1,                                                          // Random numbers (no real meaning)
            0,  1));
        final MathTransform t2 = MathTransforms.linear(new Matrix4(
            0,  8,  0,  9,
            5,  0,  0, -7,
            0,  0,  2,  0,
            0,  0,  0,  1));
        final MathTransform t3 = MathTransforms.linear(new Matrix3(
            0, -5, -3,
            7,  0, -9,
            0,  0,  1));
        final MathTransform r = MathTransforms.compound(t1, t2, t3);
        assertMatrixEquals("compound", Matrices.create(7, 7, new double[] {
            3,  0,  0,  0,  0,  0, -1,
            0,  0,  8,  0,  0,  0,  9,
            0,  5,  0,  0,  0,  0, -7,
            0,  0,  0,  2,  0,  0,  0,
            0,  0,  0,  0,  0, -5, -3,
            0,  0,  0,  0,  7,  0, -9,
            0,  0,  0,  0,  0,  0,  1
        }), MathTransforms.getMatrix(r), STRICT);
    }

    /**
     * Returns a three-dimensional transform which is non-linear in the second dimension.
     * A sample source point is (x, 1.5, y), which interpolates to (x, 8, y) where 8 is
     * the mid-point between 6 and 14. The transform can compute derivatives.
     */
    private static MathTransform nonLinear3D() {
        MathTransform tr = MathTransforms.interpolate(null, new double[] {2, 6, 14, 15});
        tr = MathTransforms.passThrough(1, tr, 1);
        return tr;
    }

    /**
     * Tests {@link MathTransforms#getMatrix(MathTransform, DirectPosition)}.
     *
     * @throws TransformException if an error occurred while computing the derivative.
     */
    @Test
    public void testGetMatrix() throws TransformException {
        MathTransform tr = MathTransforms.concatenate(nonLinear3D(), MathTransforms.linear(new Matrix4(
            5,  0,  0,  9,
            0,  1,  0,  0,      // Non-linear transform will be concatenated at this dimension.
            0,  0,  2, -7,
            0,  0,  0,  1)));

        // In the following position, only 1.5 matter because only dimension 1 is non-linear.
        final DirectPosition pos = new GeneralDirectPosition(3, 1.5, 6);
        final Matrix affine = MathTransforms.getMatrix(tr, pos);
        assertMatrixEquals("Affine approximation", new Matrix4(
            5,  0,  0,  9,
            0,  8,  0, -2,      // Non-linear transform shall be the only one with different coefficients.
            0,  0,  2, -7,
            0,  0,  0,  1), affine, STRICT);
        /*
         * Transformation using above approximation shall produce the same result than the original
         * transform if we do the comparison at the position where the approximation has been computed.
         */
        DirectPosition expected = tr.transform(pos, null);
        DirectPosition actual = MathTransforms.linear(affine).transform(pos, null);
        assertEquals(expected, actual);
    }

    /**
     * Tests {@link MathTransforms#tangent(MathTransform, DirectPosition)} of a linear transform.
     *
     * @throws TransformException should never happen since this test uses a linear transform.
     */
    @Test
    @DependsOnMethod("testGetMatrix")
    public void testTangentOfLinear() throws TransformException {
        /*
         * The random values in Matrix and DirectPosition below does not matter; we will just verify
         * that we get the same values in final result. In particular the `tangentPoint` coordinates
         * are ignored since we use a linear transform for this test.
         */
        final Matrix expected = Matrices.create(3, 4, new double[] {
            -4, 5, 7, 2,
             3, 4, 2, 9,
             0, 0, 0, 1,
        });
        final DirectPosition tangentPoint = new GeneralDirectPosition(3, 8, 7);
        MathTransform transform = MathTransforms.linear(expected);
        assertSame(transform, MathTransforms.tangent(transform, tangentPoint));
        /*
         * Above test returned the transform directly because it found that it was already an instance
         * of `LinearTransform`. For a real test, we need to hide that fact to the `tangent` method.
         */
        transform = new MathTransformWrapper(transform);
        final LinearTransform result = MathTransforms.tangent(transform, tangentPoint);
        assertNotSame(transform, result);
        assertMatrixEquals("tangent", expected, result.getMatrix(), STRICT);
    }

    /**
     * Tests {@link MathTransforms#tangent(MathTransform, DirectPosition)} of a non-linear transform.
     *
     * @throws TransformException if an error occurred while computing the derivative.
     */
    @Test
    @DependsOnMethod("testTangentOfLinear")
    public void testTangent() throws TransformException {
        final DirectPosition pos = new GeneralDirectPosition(3, 1.5, 6);
        MathTransform tr = MathTransforms.linear(new Matrix4(
            0,  5,  0,  9,
            1,  0,  0,  0,      // Non-linear transform will be concatenated at this dimension.
            0,  0, 12, -3,
            0,  0,  0,  1));

        LinearTransform linear = MathTransforms.tangent(tr, pos);
        assertSame("Linear transform shall be returned unchanged.", tr, linear);

        tr = MathTransforms.concatenate(nonLinear3D(), tr);
        linear = MathTransforms.tangent(tr, pos);
        assertNotSame(tr, linear);
        /*
         * Transformation using above approximation shall produce the same result than the original
         * transform if we do the comparison at the position where the approximation has been computed.
         */
        DirectPosition expected = tr.transform(pos, null);
        DirectPosition actual = linear.transform(pos, null);
        assertEquals(expected, actual);
    }

    /**
     * Tests the interfaces implemented by the transforms returned by {@link MathTransforms#translation(double...)}.
     */
    @Test
    public void testTranslation() {
        MathTransform tr = MathTransforms.translation(4);
        assertInstanceOf("1D", MathTransform1D.class, tr);
        assertFalse("isIdentity", tr.isIdentity());

        tr = MathTransforms.translation(4, 7);
        assertInstanceOf("2D", MathTransform2D.class, tr);
        assertFalse("isIdentity", tr.isIdentity());
    }
}
