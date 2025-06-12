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

import java.util.Random;
import java.util.Iterator;
import static java.lang.Double.NaN;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.geometry.GeneralDirectPosition;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.referencing.datum.HardCodedDatum;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertMatrixEquals;


/**
 * Tests {@link TransformSeparator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TransformSeparatorTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TransformSeparatorTest() {
    }

    /**
     * Verifies that the given transform is a linear transform equals to the given matrix.
     */
    private static void assertTransformEquals(final Matrix expected, final MathTransform tr) {
        assertMatrixEquals(expected, assertInstanceOf(LinearTransform.class, tr).getMatrix(), STRICT, "transform");
    }

    /**
     * Verifies the argument checks performed by the {@code add} methods.
     */
    @Test
    public void testArgumentChecks() {
        final TransformSeparator s = new TransformSeparator(MathTransforms.identity(8));
        RuntimeException e;
        /*
         * Shall not return unspecified dimensions.
         */
        e = assertThrows(IllegalStateException.class, () -> s.getSourceDimensions());
        assertMessageContains(e);

        e = assertThrows(IllegalStateException.class, () -> s.getTargetDimensions());
        assertMessageContains(e);
        /*
         * Shall not accept non-increasing values. While the values tested below are
         * in increasing order in each method call, they are not in increasing order
         * when taking in account the previous call.
         */
        s.addSourceDimensionRange(1, 4);
        s.addTargetDimensions(0, 3, 4);
        assertArrayEquals(new int[] {1, 2, 3}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 3, 4}, s.getTargetDimensions());

        e = assertThrows(IllegalArgumentException.class, () -> s.addSourceDimensions(3, 4, 5));
        assertMessageContains(e, "dimensions[0]");

        e = assertThrows(IllegalArgumentException.class, () -> s.addTargetDimensionRange(3, 5));
        assertMessageContains(e, "lower");
        /*
         * Shall not accept values out of range.
         */
        s.addSourceDimensions(4, 6);
        s.addTargetDimensionRange(6, 8);
        assertArrayEquals(new int[] {1, 2, 3, 4, 6}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 3, 4, 6, 7}, s.getTargetDimensions());

        e = assertThrows(IllegalArgumentException.class, () -> s.addSourceDimensions(8));
        assertMessageContains(e, "dimensions[0]");

        e = assertThrows(IllegalArgumentException.class, () -> s.addTargetDimensions(3, 8));
        assertMessageContains(e, "dimensions[0]");
    }

    /**
     * Tests separation of a linear transform.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     */
    @Test
    public void testLinearTransform() throws FactoryException {
        Matrix matrix = new Matrix4(
            2, 0, 0, 7,                                         // Some random values.
            0, 5, 0, 6,
            1, 0, 3, 8,
            0, 0, 0, 1
        );
        final var s = new TransformSeparator(MathTransforms.linear(matrix));
        /*
         * Trivial case: no dimension specified, we should get the transform unchanged.
         */
        assertSame(s.transform, s.separate());
        assertArrayEquals(new int[] {0, 1, 2}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1, 2}, s.getTargetDimensions());
        /*
         * Filter only target dimensions. This is the easiest non-trivial case since we just
         * need to drop some rows. There is no analysis to perform on the matrix values.
         */
        matrix = Matrices.create(3, 4, new double[] {
            2, 0, 0, 7,
            1, 0, 3, 8,
            0, 0, 0, 1
        });
        s.clear();
        s.addTargetDimensions(0, 2);
        s.addSourceDimensionRange(0, 3);
        assertTransformEquals(matrix, s.separate());
        assertArrayEquals(new int[] {0, 1, 2}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 2},    s.getTargetDimensions());
        /*
         * Filter only source dimensions. Do not specify any target dimensions for now.
         * TransformSeparator needs to examine the matrix values and drop all target dimensions
         * that depend on an excluded source dimensions.
         */
        matrix = Matrices.create(2, 3, new double[] {
            5, 0, 6,
            0, 0, 1
        });
        s.clear();
        s.addSourceDimensions(1, 2);
        assertTransformEquals(matrix, s.separate());
        assertArrayEquals(new int[] {1, 2}, s.getSourceDimensions());
        assertArrayEquals(new int[] {1},    s.getTargetDimensions());
        /*
         * Filter both source and target dimensions. Source dimensions 0 and 2 allow the target dimensions 0 and 2
         * (target dimension 1 is discarded because it depends on source dimension 1).  Then the target dimensions
         * are filtered for retaining only dimension 0.
         */
        matrix = Matrices.create(2, 3, new double[] {
            2, 0, 7,
            0, 0, 1
        });
        s.clear();
        s.addSourceDimensions(0, 2);
        s.addTargetDimensions(0);
        assertTransformEquals(matrix, s.separate());
        assertArrayEquals(new int[] {0, 2}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0},    s.getTargetDimensions());
        /*
         * Try again, but with the addition of a target dimension that TransformSeparator cannot keep.
         * It shall cause an exception to be thrown.
         */
        s.addTargetDimensions(1);
        try {
            s.separate();
            fail("Should not have been able to separate that transform.");
        } catch (FactoryException e) {
            // This is the expected exception.
            assertNotNull(e.getMessage());
        }
        /*
         * Try again, but allow TransformSeparator to expand the list of source dimensions.
         */
        s.setSourceExpandable(true);
        matrix = Matrices.create(3, 4, new double[] {
            2, 0, 0, 7,
            0, 5, 0, 6,
            0, 0, 0, 1
        });
        assertTransformEquals(matrix, s.separate());
        assertArrayEquals(new int[] {0, 1, 2}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1},    s.getTargetDimensions());
    }

    /**
     * Tests separation of a linear transform where a row contains zero values for all terms except translation.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     */
    @Test
    public void testScalelessDimension() throws FactoryException {
        Matrix matrix = new Matrix4(
            2, 0, 0, 7,
            0, 4, 0, 6,
            0, 0, 0, 5,                 // All scale factors are zero.
            0, 0, 0, 1);

        MathTransform tr = new ProjectiveTransform(matrix);
        TransformSeparator s = new TransformSeparator(tr);
        /*
         * The usually expected case, where the [0 0 0 5] row is dropped. But here, that row has been dropped
         * because we explicitly requested so. We test the usual case first before to test the less intuitive
         * case in next step.
         */
        s.addSourceDimensions(0, 1);
        s.addTargetDimensions(0, 1);
        matrix = new Matrix3(           // Expected result.
            2, 0, 7,
            0, 4, 6,
            0, 0, 1);

        tr = s.separate();
        assertArrayEquals(new int[] {0, 1}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1}, s.getTargetDimensions());
        assertTransformEquals(matrix, tr);
        /*
         * Below is the less intuitive case. When asking for the first two dimensions, we usually expect the
         * third dimension to be dropped. But if that third dimension is a constant (all scale factors at 0),
         * it does not depend on the source dimensions and can be kept. The result is a non-square matrix
         * with 2 dimensions in input and still 3 dimensions in output.
         */
        s.clear();
        s.addSourceDimensions(0, 1);
        matrix = Matrices.create(4, 3, new double[] {
            2, 0, 7,
            0, 4, 6,
            0, 0, 5,                    // All scale factors are zero.
            0, 0, 1});

        tr = s.separate();
        assertArrayEquals(new int[] {0, 1   }, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1, 2}, s.getTargetDimensions());
        assertTransformEquals(matrix, tr);
    }

    /**
     * Tests separation of a linear transform containing {@link Double#NaN} values.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     */
    @Test
    public void testIncompleteTransform() throws FactoryException {
        Matrix matrix = new Matrix4(
            1,   0,   0,   7,
            0,   0,   1,   8,
            0, NaN,   0,   6,
            0,   0,   0,   1
        );
        var s = new TransformSeparator(MathTransforms.linear(matrix));
        s.addSourceDimensions(1);
        assertTransformEquals(new Matrix2(
               NaN, 6,
                 0, 1
        ), s.separate());
        assertArrayEquals(new int[] {2}, s.getTargetDimensions());
    }

    /**
     * Tests separation of a concatenated transform.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     */
    @Test
    public void testConcatenatedTransform() throws FactoryException {
        final MathTransformFactory factory = DefaultMathTransformFactory.provider();
        final var s = new TransformSeparator(EllipsoidToCentricTransform.createGeodeticConversion(
                                    factory, HardCodedDatum.WGS84.getEllipsoid(), false), factory);

        s.addSourceDimensions(0, 1);
        s.addTargetDimensions(0, 1);
        final Iterator<MathTransform> it = MathTransforms.getSteps(s.separate()).iterator();
        assertInstanceOf(LinearTransform.class,             it.next(), "normalize");
        assertInstanceOf(EllipsoidToCentricTransform.class, it.next(), "transform");
        assertInstanceOf(LinearTransform.class,             it.next(), "denormalize");
        assertFalse(it.hasNext());
    }

    /**
     * Tests separation of a pass through transform.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     * @throws TransformException if an error occurred while transforming coordinates for comparison purpose.
     */
    @Test
    public void testPassThroughTransform() throws FactoryException, TransformException {
        /*
         * This non-linear transform increase the number of dimensions from 2 to 3.
         * In addition we let 2 dimensions passthrough before and 3 passtrough after.
         */
        final MathTransform nonLinear = new PseudoTransform(2, 3);
        final TransformSeparator s = new TransformSeparator(MathTransforms.passThrough(2, nonLinear, 3));
        /*
         * Trivial case: no dimension specified, we should get the transform unchanged.
         */
        assertSame(s.transform, s.separate());
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 6},    s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 6, 7}, s.getTargetDimensions());
        /*
         * Filter only target dimensions. If the requested indices overlap the pass-through transform,
         * TransformSeparator will just concatenate a matrix after the transform for dropping dimensions.
         */
        Matrix expected = Matrices.create(4, 9, new double[] {
            0, 1, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 1, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 1
        });
        s.clear();
        s.addTargetDimensions(1, 2, 7);
        s.addSourceDimensionRange(0, 7);
        MathTransform result = s.separate();
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 6}, s.getSourceDimensions());
        assertArrayEquals(new int[] {1, 2, 7}, s.getTargetDimensions());
        final Random random = TestUtilities.createRandomNumberGenerator();
        compare(s.transform, MathTransforms.linear(expected), result, random);
        /*
         * Filter only target dimensions, but with indices that are all outside the pass-through transform.
         * TransformSeparator should be able to give us a simple affine transform.
         */
        expected = Matrices.create(4, 8, new double[] {
            0, 1, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 0, 0, 1
        });
        s.clear();
        s.addTargetDimensions(1, 5, 7);
        s.addSourceDimensionRange(0, 7);
        result = s.separate();
        assertArrayEquals (new int[] {0, 1, 2, 3, 4, 5, 6}, s.getSourceDimensions());
        assertArrayEquals (new int[] {1, 5, 7}, s.getTargetDimensions());
        assertTransformEquals(expected, result);
        /*
         * Filter source dimensions. If we ask only for dimensions not in the pass-through transform,
         * then TransformSeparator should return an affine transform.
         */
        expected = new Matrix3(
            1, 0, 0,
            0, 1, 0,
            0, 0, 1
        );
        s.clear();
        s.addSourceDimensions(0, 6);
        result = s.separate();
        assertArrayEquals (new int[] {0, 6}, s.getSourceDimensions());
        assertArrayEquals (new int[] {0, 7}, s.getTargetDimensions());
        assertTransformEquals(expected, result);
        /*
         * Filter source dimensions, now with overlapping in the pass-through transform.
         * TransformSeparator is expected to create a new PassThroughTransform.
         */
        s.clear();
        s.addSourceDimensions(1, 2, 3, 4, 5);
        result = s.separate();
        assertArrayEquals(new int[] {1, 2, 3, 4, 5}, s.getSourceDimensions());
        assertArrayEquals(new int[] {1, 2, 3, 4, 5, 6}, s.getTargetDimensions());
        assertInstanceOf (PassThroughTransform.class, result);
        assertSame  (nonLinear, ((PassThroughTransform) result).subTransform);
        assertEquals(1, ((PassThroughTransform) result).firstAffectedCoordinate);
        assertEquals(2, ((PassThroughTransform) result).numTrailingCoordinates);
    }

    /**
     * Compares coordinate computed by a reference with coordinates computed by the transform to test.
     * We use this method when we cannot easily analyze the {@link MathTransform} created by the test
     * case, for example because it may have been rearranged in arbitrary ways for optimization purpose
     * (e.g. {@link PassThroughTransform#tryConcatenate(TransformJoiner)}).
     *
     * @param  tr1     first half of the transform to use as a reference.
     * @param  tr2     second half of the transform to use as a reference.
     * @param  test    the transform to test.
     * @param  random  random number generator for coordinate values.
     */
    private static void compare(final MathTransform tr1, final MathTransform tr2, final MathTransform test, final Random random)
            throws TransformException
    {
        DirectPosition source   = new GeneralDirectPosition(tr1.getSourceDimensions());
        DirectPosition step     = null;
        DirectPosition expected = null;
        DirectPosition actual   = null;
        for (int t=0; t<50; t++) {
            for (int i=source.getDimension(); --i>=0;) {
                source.setCoordinate(i, random.nextDouble());
            }
            step     = tr1 .transform(source,   step);
            expected = tr2 .transform(step, expected);
            actual   = test.transform(source, actual);
            assertEquals(expected, actual);
        }
    }

    /**
     * Tests separation of a concatenated transform containing a pass through transform.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     * @throws TransformException if an error occurred while transforming coordinates for comparison purpose.
     */
    @Test
    public void testConcatenatedPassThroughTransform() throws FactoryException, TransformException {
        final MathTransform linear       = MathTransforms.scale(4, 1, 1, 1, 1, 6);
        final MathTransform nonLinear    = new PseudoTransform(3, 2);
        final MathTransform passthrough  = MathTransforms.passThrough(2, nonLinear, 1);
        final MathTransform concatenated = new ConcatenatedTransform(linear, passthrough);  // Bypass optimizations.
        final TransformSeparator sep = new TransformSeparator(concatenated);
        sep.addSourceDimensionRange(0, 2);
        assertMatrixEquals(new Matrix3(4, 0, 0, 0, 1, 0, 0,  0, 1),
                           MathTransforms.getMatrix(sep.separate()),
                           STRICT, "Leading passthrough dimensions");
        sep.clear();
        sep.addSourceDimensionRange(5, 6);
        assertMatrixEquals(new Matrix2(6, 0, 0, 1),
                           MathTransforms.getMatrix(sep.separate()),
                           STRICT, "Trailing passthrough dimensions");
        sep.clear();
        sep.addSourceDimensionRange(2, 5);
        assertSame(nonLinear, sep.separate());

        sep.clear();
        sep.addSourceDimensionRange(1, 5);
        MathTransform mt = sep.separate();
        assertInstanceOf(PassThroughTransform.class, mt);
        final PassThroughTransform ps = ((PassThroughTransform) mt);
        assertEquals(1, ps.firstAffectedCoordinate);
        assertEquals(0, ps.numTrailingCoordinates);
        assertSame(nonLinear, ps.subTransform);
    }

    /**
     * Tests separation of a pass through transform containing another pass through transform.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     * @throws TransformException if an error occurred while transforming coordinates for comparison purpose.
     */
    @Test
    public void testNestedPassThroughTransform() throws FactoryException, TransformException {
        final MathTransform nonLinear    = new PseudoTransform(3, 2);
        final MathTransform passthrough1 = MathTransforms.passThrough(2, nonLinear, 1);
        final MathTransform concatenated = new ConcatenatedTransform(MathTransforms.scale(4, 3, 2, 1, 1, 6), passthrough1);
        final MathTransform passthrough2 = new PassThroughTransform(2, concatenated, 3);
        final TransformSeparator sep = new TransformSeparator(passthrough2);
        sep.addSourceDimensionRange(3, 7);
        MathTransform mt = sep.separate();
        var concat = assertInstanceOf(ConcatenatedTransform.class, mt);
        assertMatrixEquals(Matrices.create(5, 5, new double[] {
                    3, 0, 0, 0, 0,
                    0, 2, 0, 0, 0,
                    0, 0, 1, 0, 0,
                    0, 0, 0, 1, 0,
                    0, 0, 0, 0, 1
                }), MathTransforms.getMatrix(concat.transform1),
                STRICT, "Leading passthrough dimensions");

        mt = ((ConcatenatedTransform) mt).transform2;
        assertInstanceOf(PassThroughTransform.class, mt);
        final PassThroughTransform ps = ((PassThroughTransform) mt);
        assertEquals(1, ps.firstAffectedCoordinate);
        assertEquals(0, ps.numTrailingCoordinates);
        assertSame(nonLinear, ps.subTransform);
    }

    /**
     * Tests {@link TransformSeparator} with removal of unused source dimensions.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     */
    @Test
    public void testTrimSourceDimensions() throws FactoryException {
        MathTransform tr = MathTransforms.linear(Matrices.create(3, 4, new double[] {
            0,   0.5, 0,  -90,
            0.5, 0,   0, -180,
            0,   0,   0,    1}));
        /*
         * Verify that TransformSeparator does not trim anything if not requested so.
         */
        TransformSeparator s = new TransformSeparator(tr);
        s.addSourceDimensionRange(0, tr.getSourceDimensions());
        assertSame(tr, s.separate(), "No source dimensions should be trimmed if not requested.");
        assertArrayEquals(new int[] {0, 1, 2}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1   }, s.getTargetDimensions());
        /*
         * Trim the last dimension (most common case).
         */
        final Matrix expected = new Matrix3(
            0,   0.5, -90,
            0.5, 0,  -180,
            0,   0,     1);
        s.clear();
        MathTransform reduced = s.separate();
        assertNotEquals(tr, reduced);
        assertArrayEquals(new int[] {0, 1}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1}, s.getTargetDimensions());
        assertMatrixEquals(expected, MathTransforms.getMatrix(reduced), STRICT, "separate()");
        /*
         * Trim the first dimension.
         */
        tr = MathTransforms.linear(Matrices.create(3, 4, new double[] {
            0, 0,   0.5, -90,
            0, 0.5, 0,  -180,
            0, 0,   0,     1}));

        s = new TransformSeparator(tr);
        reduced = s.separate();
        assertNotEquals(tr, reduced);
        assertArrayEquals(new int[] {1, 2}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1}, s.getTargetDimensions());
        assertMatrixEquals(new Matrix3(0,   0.5, -90,
                                       0.5, 0,  -180,
                                       0,   0,     1),
                MathTransforms.getMatrix(reduced), STRICT, "separate()");
    }
}
