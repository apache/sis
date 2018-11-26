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

import java.util.Iterator;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.measure.Units;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.opengis.test.Assert.*;


/**
 * Tests {@link TransformSeparator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.7
 * @module
 */
@DependsOn({
    PassThroughTransformTest.class,
    ConcatenatedTransformTest.class
})
public final strictfp class TransformSeparatorTest extends TestCase {
    /**
     * Verifies the argument checks performed by the {@code add} methods.
     */
    @Test
    public void testArgumentChecks() {
        final TransformSeparator s = new TransformSeparator(MathTransforms.identity(8));
        try {
            s.getSourceDimensions();
            fail("Shall not return unspecified dimensions.");
        } catch (IllegalStateException e) {
            // This is the expected exception.
        }
        try {
            s.getTargetDimensions();
            fail("Shall not return unspecified dimensions.");
        } catch (IllegalStateException e) {
            // This is the expected exception.
        }
        s.addSourceDimensionRange(1, 4);
        s.addTargetDimensions(0, 3, 4);
        assertArrayEquals("sourceDimensions", new int[] {1, 2, 3}, s.getSourceDimensions());
        assertArrayEquals("targetDimensions", new int[] {0, 3, 4}, s.getTargetDimensions());
        try {
            s.addSourceDimensions(3, 4, 5);
            fail("Shall not accept non-increasing value.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("dimensions[0]"));
        }
        try {
            s.addTargetDimensionRange(3, 5);
            fail("Shall not accept non-increasing value.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("lower"));
        }
        s.addSourceDimensions(4, 6);
        s.addTargetDimensionRange(6, 8);
        assertArrayEquals("sourceDimensions", new int[] {1, 2, 3, 4, 6}, s.getSourceDimensions());
        assertArrayEquals("targetDimensions", new int[] {0, 3, 4, 6, 7}, s.getTargetDimensions());
        try {
            s.addSourceDimensions(8);
            fail("Shall not accept value out of range.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("dimensions[0]"));
        }
        try {
            s.addTargetDimensions(3, 8);
            fail("Shall not accept value out of range.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("dimensions[0]"));
        }
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
        final TransformSeparator s = new TransformSeparator(MathTransforms.linear(matrix));
        /*
         * Trivial case: no dimension specified, we should get the transform unchanged.
         */
        assertSame("transform", s.transform, s.separate());
        assertArrayEquals("sourceDimensions", new int[] {0, 1, 2}, s.getSourceDimensions());
        assertArrayEquals("targetDimensions", new int[] {0, 1, 2}, s.getTargetDimensions());
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
        assertMatrixEquals("transform", matrix, ((LinearTransform) s.separate()).getMatrix(), STRICT);
        assertArrayEquals("sourceDimensions", new int[] {0, 1, 2}, s.getSourceDimensions());
        assertArrayEquals("targetDimensions", new int[] {0, 2},    s.getTargetDimensions());
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
        assertMatrixEquals("transform", matrix, ((LinearTransform) s.separate()).getMatrix(), STRICT);
        assertArrayEquals("sourceDimensions", new int[] {1, 2}, s.getSourceDimensions());
        assertArrayEquals("targetDimensions", new int[] {1},    s.getTargetDimensions());
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
        assertMatrixEquals("transform", matrix, ((LinearTransform) s.separate()).getMatrix(), STRICT);
        assertArrayEquals("sourceDimensions", new int[] {0, 2}, s.getSourceDimensions());
        assertArrayEquals("targetDimensions", new int[] {0},    s.getTargetDimensions());
        /*
         * Try again, but with the addition of a target dimension that TransformSeparator can not keep.
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
        TransformSeparator s = new TransformSeparator(MathTransforms.linear(matrix));
        s.addSourceDimensions(1);
        assertMatrixEquals("transform", new Matrix2(
               NaN, 6,
                 0, 1
        ), ((LinearTransform) s.separate()).getMatrix(), STRICT);
        assertArrayEquals(new int[] {2}, s.getTargetDimensions());
    }

    /**
     * Tests separation of a concatenated transform.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     */
    @Test
    @DependsOnMethod("testLinearTransform")
    public void testConcatenatedTransform() throws FactoryException {
        final MathTransformFactory factory = DefaultFactories.forBuildin(MathTransformFactory.class);
        final TransformSeparator s = new TransformSeparator(EllipsoidToCentricTransform.createGeodeticConversion(
                factory, HardCodedDatum.WGS84.getEllipsoid(), false), factory);

        s.addSourceDimensions(0, 1);
        s.addTargetDimensions(0, 1);
        final Iterator<MathTransform> it = MathTransforms.getSteps(s.separate()).iterator();
        assertInstanceOf("normalize",   LinearTransform.class,             it.next());
        assertInstanceOf("transform",   EllipsoidToCentricTransform.class, it.next());
        assertInstanceOf("denormalize", LinearTransform.class,             it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Tests separation of a pass through transform.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     */
    @Test
    @DependsOnMethod("testLinearTransform")
    public void testPassThroughTransform() throws FactoryException {
        final MathTransform nonLinear = new EllipsoidToCentricTransform(6378137, 6356752.314245179,
                Units.METRE, false, EllipsoidToCentricTransform.TargetType.CARTESIAN);
        final TransformSeparator s = new TransformSeparator(MathTransforms.passThrough(2, nonLinear, 3));
        /*
         * Trivial case: no dimension specified, we should get the transform unchanged.
         */
        assertSame("transform", s.transform, s.separate());
        assertArrayEquals("sourceDimensions", new int[] {0, 1, 2, 3, 4, 5, 6},    s.getSourceDimensions());
        assertArrayEquals("targetDimensions", new int[] {0, 1, 2, 3, 4, 5, 6, 7}, s.getTargetDimensions());
        /*
         * Filter only target dimensions. If the requested indices overlap the pass-through transform,
         * TransformSeparator will just concatenate a matrix after the transform for dropping dimensions.
         */
        Matrix matrix = Matrices.create(4, 9, new double[] {
            0, 1, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 1, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 1
        });
        s.clear();
        s.addTargetDimensions(1, 2, 7);
        MathTransform r = s.separate();
        assertArrayEquals("sourceDimensions", new int[] {0, 1, 2, 3, 4, 5, 6}, s.getSourceDimensions());
        assertArrayEquals("targetDimensions", new int[] {1, 2, 7}, s.getTargetDimensions());
        assertInstanceOf ("separate()", ConcatenatedTransform.class, r);
        assertSame(s.transform, ((ConcatenatedTransform) r).transform1);
        assertMatrixEquals("separate().transform2", matrix,
                ((LinearTransform) (((ConcatenatedTransform) r).transform2)).getMatrix(), STRICT);
        /*
         * Filter only target dimensions, but with indices that are all outside the pass-through transform.
         * TransformSeparator should be able to give us a simple affine transform.
         */
        matrix = Matrices.create(4, 8, new double[] {
            0, 1, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 0, 0, 1
        });
        s.clear();
        s.addTargetDimensions(1, 5, 7);
        r = s.separate();
        assertArrayEquals ("sourceDimensions", new int[] {0, 1, 2, 3, 4, 5, 6}, s.getSourceDimensions());
        assertArrayEquals ("targetDimensions", new int[] {1, 5, 7}, s.getTargetDimensions());
        assertInstanceOf  ("separate()", LinearTransform.class, r);
        assertMatrixEquals("separate().transform2", matrix, ((LinearTransform) r).getMatrix(), STRICT);
        /*
         * Filter source dimensions. If we ask only for dimensions not in the pass-through transform,
         * then TransformSeparator should return an affine transform.
         */
        matrix = new Matrix3(
            1, 0, 0,
            0, 1, 0,
            0, 0, 1
        );
        s.clear();
        s.addSourceDimensions(0, 6);
        r = s.separate();
        assertArrayEquals ("sourceDimensions", new int[] {0, 6}, s.getSourceDimensions());
        assertArrayEquals ("targetDimensions", new int[] {0, 7}, s.getTargetDimensions());
        assertInstanceOf  ("separate()", LinearTransform.class, r);
        assertMatrixEquals("separate().transform2", matrix, ((LinearTransform) r).getMatrix(), STRICT);
        /*
         * Filter source dimensions, now with overlapping in the pass-through transform.
         * TransformSeparator is expected to create a new PassThroughTransform.
         */
        s.clear();
        s.addSourceDimensions(1, 2, 3, 4, 5);
        r = s.separate();
        assertArrayEquals("sourceDimensions", new int[] {1, 2, 3, 4, 5}, s.getSourceDimensions());
        assertArrayEquals("targetDimensions", new int[] {1, 2, 3, 4, 5, 6}, s.getTargetDimensions());
        assertInstanceOf ("separate()", PassThroughTransform.class, r);
        assertSame  ("subTransform",  nonLinear, ((PassThroughTransform) r).subTransform);
        assertEquals("firstAffectedOrdinate", 1, ((PassThroughTransform) r).firstAffectedOrdinate);
        assertEquals("numTrailingOrdinates",  2, ((PassThroughTransform) r).numTrailingOrdinates);
    }

    /**
     * Tests {@link TransformSeparator#getTrimSourceDimensions()}.
     *
     * @throws FactoryException if an error occurred while creating a new transform.
     */
    @Test
    @DependsOnMethod("testLinearTransform")
    public void testGetTrimSourceDimensions() throws FactoryException {
        MathTransform tr = MathTransforms.linear(Matrices.create(3, 4, new double[] {
            0,   0.5, 0,  -90,
            0.5, 0,   0, -180,
            0,   0,   0,    1}));
        /*
         * Verify that TransformSeparator does not trim anything if not requested so.
         */
        TransformSeparator s = new TransformSeparator(tr);
        assertFalse("trimSourceDimensions", s.getTrimSourceDimensions());
        assertSame("No source dimensions should be trimmed if not requested.", tr, s.separate());
        assertArrayEquals(new int[] {0, 1, 2}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1   }, s.getTargetDimensions());
        /*
         * Trim the last dimension (most common case).
         */
        final Matrix expected = new Matrix3(
            0,   0.5, -90,
            0.5, 0,  -180,
            0,   0,     1);
        s.setTrimSourceDimensions(true);
        assertTrue("trimSourceDimensions", s.getTrimSourceDimensions());
        MathTransform reduced = s.separate();
        assertNotEquals("separate()", tr, reduced);
        assertArrayEquals(new int[] {0, 1}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1}, s.getTargetDimensions());
        assertMatrixEquals("separate()", expected, MathTransforms.getMatrix(reduced), STRICT);
        /*
         * Trim the first dimension.
         */
        tr = MathTransforms.linear(Matrices.create(3, 4, new double[] {
            0, 0,   0.5, -90,
            0, 0.5, 0,  -180,
            0, 0,   0,     1}));

        s = new TransformSeparator(tr);
        s.setTrimSourceDimensions(true);
        reduced = s.separate();
        assertNotEquals("separate()", tr, reduced);
        assertArrayEquals(new int[] {1, 2}, s.getSourceDimensions());
        assertArrayEquals(new int[] {0, 1}, s.getTargetDimensions());
        assertMatrixEquals("separate()", new Matrix3(
            0,   0.5, -90,
            0.5, 0,  -180,
            0,   0,     1), MathTransforms.getMatrix(reduced), STRICT);
    }
}
