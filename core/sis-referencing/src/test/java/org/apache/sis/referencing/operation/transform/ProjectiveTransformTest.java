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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.parameter.Parameterized;

// Test imports
import org.opengis.test.Validators;
import org.apache.sis.test.TestRunner;
import org.apache.sis.test.DependsOn;
import org.junit.runner.RunWith;
import org.junit.After;
import org.junit.Test;
import org.opengis.test.Assert;
import static org.opengis.test.Assert.*;

// Branch-dependent imports
import org.opengis.test.referencing.AffineTransformTest;


/**
 * Tests the {@link ProjectiveTransform} class by inheriting the tests defined in GeoAPI conformance module.
 * We use the {@link java.awt.geom.AffineTransform} class as a reference, so we need to avoid NaN values.
 * Note that {@link CopyTransformTest} will use {@code ProjectiveTransform} as a reference,
 * this time with NaN values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.5
 * @module
 */
@RunWith(TestRunner.class)
@DependsOn({AbstractMathTransformTest.class, ScaleTransformTest.class})
public strictfp class ProjectiveTransformTest extends AffineTransformTest {
    /**
     * Tolerance factor for strict comparisons.
     */
    private static final double STRICT = 0;

    /**
     * For {@link LinearTransformTest} constructor only.
     */
    ProjectiveTransformTest(final MathTransformFactory factory) {
        super(factory);
    }

    /**
     * Creates a new test suite.
     */
    public ProjectiveTransformTest() {
        super(new MathTransformFactoryBase() {
            @Override
            public MathTransform createAffineTransform(final Matrix matrix) {
                final ProjectiveTransform pt;
                if (matrix.getNumRow() == 3 && matrix.getNumCol() == 3) {
                    pt = new ProjectiveTransform2D(matrix);
                } else {
                    pt = new ProjectiveTransform(matrix);
                }
                MathTransform tr = pt.optimize();
                if (tr != pt) {
                    /*
                     * Opportunistically tests `ScaleTransform` together with `ProjectiveTransform`.
                     * We take `ScaleTransform` as a reference implementation because it is simpler.
                     */
                    tr = new TransformResultComparator(tr, pt, STRICT);
                }
                return tr;
            }
        });
    }

    /**
     * Returns the transform without {@link TransformResultComparator} wrapper.
     * The transform is the one computed by {@link ProjectiveTransform#optimize()}.
     */
    private MathTransform getOptimizedTransform() {
        MathTransform tr = transform;
        if (tr instanceof TransformResultComparator) {
            tr = ((TransformResultComparator) tr).reference;
        }
        return tr;
    }

    /*
     * Inherit all the tests from GeoAPI:
     *    - testIdentity1D()
     *    - testIdentity2D()
     *    - testIdentity3D()
     *    - testAxisSwapping2D()
     *    - testSouthOrientated2D()
     *    - testTranslatation2D()
     *    - testUniformScale2D()
     *    - testGenericScale2D()
     *    - testRotation2D()
     *    - testGeneral()
     *    - testDimensionReduction()
     */

    /**
     * Tests {@link ProjectiveTransform#optimize()}. In particular this method verifies that a non-square matrix
     * that looks like diagonal is not confused with a real diagonal matrix.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if a coordinate conversion failed.
     *
     * @since 0.7
     */
    @Test
    public void testOptimize() throws FactoryException, TransformException {
        matrix = Matrices.create(5, 4, new double[] {
            2, 0, 0, 0,
            0, 3, 0, 0,
            0, 0, 4, 0,
            0, 0, 0, 5,
            0, 0, 0, 1
        });
        transform = mtFactory.createAffineTransform(matrix);
        assertInstanceOf("Non-diagonal matrix shall not be handled by ScaleTransform.", ProjectiveTransform.class, transform);
        verifyConsistency(1, 2, 3,   -3, -2, -1);
        /*
         * Remove the "problematic" row. The new transform should now be optimizable.
         */
        matrix = ((MatrixSIS) matrix).removeRows(3, 4);
        transform = mtFactory.createAffineTransform(matrix);
        assertInstanceOf("Diagonal matrix should be handled by a specialized class.", ScaleTransform.class, getOptimizedTransform());
        verifyConsistency(1, 2, 3,   -3, -2, -1);
    }

    /**
     * Tests {@link ProjectiveTransform#optimize()} when the matrix defines a constant value.
     * Older SIS versions wrongly optimized this case as a translation.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if a coordinate conversion failed.
     *
     * @since 1.1
     */
    @Test
    public void testOptimizeConstant() throws FactoryException, TransformException {
        matrix = new Matrix2(0, 10, 0, 1);
        transform = mtFactory.createAffineTransform(matrix);
        Assert.assertMatrixEquals("Transform shall use the given matrix unmodified.",
                matrix, ((LinearTransform) transform).getMatrix(), STRICT);
        verifyConsistency(1, 2, 3,   -3, -2, -1);
    }

    /**
     * {@code true} if {@link #ensureImplementRightInterface()} should skip its check for a transform
     * of the given dimension. {@code ProjectiveTransformTest} needs to skip the case for dimension 1
     * because there is no {@code ProjectiveTransform1D} class. However {@link LinearTransformTest}
     * can check for all dimensions.
     */
    boolean skipInterfaceCheckForDimension(final int dimension) {
        return dimension == 1;
    }

    /**
     * Executed after every test in order to ensure that the {@linkplain #transform transform}
     * implements the {@link MathTransform1D} or {@link MathTransform2D} interface as needed.
     * In addition, all Apache SIS classes for linear transforms shall implement
     * {@link LinearTransform} and {@link Parameterized} interfaces.
     */
    @After
    public final void ensureImplementRightInterface() {
        /*
         * `TransformResultComparator.tested` is the `ProjectiveTransform` before call to `optimize()`.
         * This is okay because `ProjectiveTransform.optimize()` is not expected to take in account all
         * possible types, so the test would fail if checking its result. More complete optimizations
         * are tested in the `LinearTransformTest` subclass.
         */
        if (transform instanceof TransformResultComparator) {
            transform = ((TransformResultComparator) transform).tested;
        }
        /*
         * Below is a copy of MathTransformTestCase.validate(), with minor modifications
         * due to the fact that this class does not extend MathTransformTestCase.
         */
        assertNotNull("The `transform` field shall be assigned a value.", transform);
        Validators.validate(transform);
        final int dimension = transform.getSourceDimensions();
        if (transform.getTargetDimensions() == dimension && !skipInterfaceCheckForDimension(dimension)) {
            assertEquals("MathTransform1D", dimension == 1, (transform instanceof MathTransform1D));
            assertEquals("MathTransform2D", dimension == 2, (transform instanceof MathTransform2D));
        } else {
            assertFalse("MathTransform1D", transform instanceof MathTransform1D);
            assertFalse("MathTransform2D", transform instanceof MathTransform2D);
        }
        assertInstanceOf("Parameterized", Parameterized.class, transform);
        /*
         * End of MathTransformTestCase.validate(). Remaining is specific to LinearTransform implementations.
         */
        assertInstanceOf("Not a LinearTransform.", LinearTransform.class, transform);
        final Matrix tm = ((LinearTransform) transform).getMatrix();
        assertTrue("The matrix declared by the MathTransform is not equal to the one given at creation time.",
                Matrices.equals(matrix, tm, tolerance, false));

        assertSame("ParameterDescriptor",
                Affine.getProvider(transform.getSourceDimensions(), transform.getTargetDimensions(), true).getParameters(),
                ((Parameterized) transform).getParameterDescriptors());
    }
}
