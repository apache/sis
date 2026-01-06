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
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.operation.provider.Affine;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.math.Fraction;

// Test dependencies
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.referencing.Assertions;
import org.apache.sis.test.FailureDetailsReporter;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.referencing.AffineTransformTest;


/**
 * Tests the {@link ProjectiveTransform} class by inheriting the tests defined in GeoAPI conformance module.
 * We use the {@link java.awt.geom.AffineTransform} class as a reference, so we need to avoid NaN values.
 * Note that {@link CopyTransformTest} will use {@code ProjectiveTransform} as a reference,
 * this time with NaN values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
@ExtendWith(FailureDetailsReporter.class)
public class ProjectiveTransformTest extends AffineTransformTest {
    /**
     * A math transform factory which delegates instantiations to the enclosing test class.
     * This is a workaround while waiting for JEP 447: Statements before super(…).
     */
    private static final class Proxy extends MathTransformFactoryBase {
        /** The enclosing test class. */
        ProjectiveTransformTest test;

        /** Delegates object creation to the enclosing test class. */
        @Override public MathTransform createAffineTransform(final Matrix matrix) {
            return test.createAffineTransform(matrix);
        }
    }

    /**
     * Creates a new test suite.
     */
    public ProjectiveTransformTest() {
        super(new Proxy());
        ((Proxy) mtFactory).test = this;
    }

    /**
     * Creates the transform to test.
     * This method is overridden by subclass testing a different kind of transform.
     *
     * @param  matrix  coefficients of the affine transform.
     * @return the transform to test.
     */
    MathTransform createAffineTransform(final Matrix matrix) {
        final ProjectiveTransform tested;
        if (matrix.getNumRow() == 3 && matrix.getNumCol() == 3) {
            tested = new ProjectiveTransform2D(matrix);
        } else {
            tested = new ProjectiveTransform(matrix);
        }
        final MathTransform reference = tested.optimize();
        if (tested != reference) {
            /*
             * Opportunistically tests `ScaleTransform` together with `ProjectiveTransform`.
             * We take `ScaleTransform` as a reference implementation because it is simpler.
             */
            return new TransformResultComparator(reference, tested, 1E-12);
        }
        return tested;
    }

    /**
     * Whether the post-test validation should skip its check for a transform of the given dimension.
     * {@code ProjectiveTransformTest} needs to skip the case for dimension 1 because there is no
     * {@code ProjectiveTransform1D} class. However, {@link LinearTransformTest} can do the check
     * for all dimensions.
     *
     * @see #ensureImplementRightInterface()
     */
    boolean skipInterfaceCheckForDimension(final int dimension) {
        return dimension == 1;
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
     * @throws FactoryException if the transform cannot be created.
     * @throws TransformException if a coordinate conversion failed.
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
        assertInstanceOf(ProjectiveTransform.class, transform, "Non-diagonal matrix shall not be handled by ScaleTransform.");
        verifyConsistency(1, 2, 3,   -3, -2, -1);
        /*
         * Remove the "problematic" row. The new transform should now be optimizable.
         */
        matrix = ((MatrixSIS) matrix).removeRows(3, 4);
        transform = mtFactory.createAffineTransform(matrix);
        assertInstanceOf(ScaleTransform.class, getOptimizedTransform(), "Diagonal matrix should be handled by a specialized class.");
        verifyConsistency(1, 2, 3,   -3, -2, -1);
    }

    /**
     * Tests {@link ProjectiveTransform#optimize()} when the matrix defines a constant value.
     * Older SIS versions wrongly optimized this case as a translation.
     *
     * @throws FactoryException if the transform cannot be created.
     * @throws TransformException if a coordinate conversion failed.
     */
    @Test
    public void testOptimizeConstant() throws FactoryException, TransformException {
        matrix = new Matrix2(0, 10, 0, 1);
        transform = mtFactory.createAffineTransform(matrix);
        Assertions.assertMatrixEquals(matrix, transform, "Transform shall use the given matrix unmodified.");
        verifyConsistency(1, 2, 3,   -3, -2, -1);
    }

    /**
     * Tests the concatenation of transforms that would result in rounding errors
     * if extended-precision matrix operations were not used.
     *
     * @throws FactoryException if the transform cannot be created.
     * @throws TransformException if a coordinate conversion failed.
     */
    @Test
    public void testRoundingErrors() throws FactoryException, TransformException {
        final var num = new Matrix4(); num.m00 =  2; num.m11 = 3.25; num.m22 = -17;
        final var den = new Matrix4(); den.m00 = 37; den.m11 = 1000; den.m22 = 127;

        // Add translation terms.
        num.m03 =  4*37; num.m13 = 17; num.m23 = -2*127;
        den.m03 = -3*37; den.m13 = 65; den.m23 =  7*127;

        transform = TransformResultComparator.concatenate(
                mtFactory.createAffineTransform(num),
                mtFactory.createAffineTransform(den).inverse(),
                mtFactory);
        matrix = ((LinearTransform) getOptimizedTransform()).getMatrix();
        /*
         * Verify matrix elements after inversion and concatenation.
         * The extended precision types should be used.
         */
        final ExtendedPrecisionMatrix m = (ExtendedPrecisionMatrix) matrix;
        assertEquals(new Fraction(2, 37),                 m.getElementOrNull(0,0));
        assertEquals(DoubleDouble.of(325).divide(100000), m.getElementOrNull(1,1));
        assertEquals(new Fraction(-17, 127),              m.getElementOrNull(2,2));
        assertNull  (                                     m.getElementOrNull(0,1));
        assertEquals(         0, m.getElement(0,1));
        assertEquals(  2d /  37, m.getElement(0,0), 1E-15);
        assertEquals(   0.00325, m.getElement(1,1), 1E-15);
        assertEquals(-17d / 127, m.getElement(2,2), 1E-15);
        /*
         * Test a transformation, expecting exact result.
         */
        verifyTransform(new double[] {2645.5,  19500, 2222.5},
                        new double[] {   150, 63.327, -306.5});
    }

    /**
     * Returns the transform without {@link TransformResultComparator} wrapper.
     * The transform is the one computed by {@link ProjectiveTransform#optimize()}.
     */
    private MathTransform getOptimizedTransform() {
        MathTransform tr = transform;
        while (tr instanceof TransformResultComparator) {
            tr = ((TransformResultComparator) tr).reference;
        }
        return tr;
    }

    /**
     * Executed after every test in order to ensure that the {@linkplain #transform transform}
     * implements the {@link MathTransform1D} or {@link MathTransform2D} interface as needed.
     * In addition, all Apache SIS classes for linear transforms shall implement
     * {@link LinearTransform} and {@link Parameterized} interfaces.
     */
    @AfterEach
    public final void ensureImplementRightInterface() {
        /*
         * `TransformResultComparator.tested` is the `ProjectiveTransform` before call to `optimize()`.
         * This is okay because `ProjectiveTransform.optimize()` is not expected to take in account all
         * possible types, so the test would fail if checking its result. More complete optimizations
         * are tested in the `LinearTransformTest` subclass.
         */
        while (transform instanceof TransformResultComparator) {
            transform = ((TransformResultComparator) transform).tested;
        }
        /*
         * Below is a copy of MathTransformTestCase.validate(), with minor modifications
         * due to the fact that this class does not extend MathTransformTestCase.
         */
        assertNotNull(transform, "The `transform` field shall be assigned a value.");
        Validators.validate(transform);
        final int dimension = transform.getSourceDimensions();
        if (transform.getTargetDimensions() == dimension && !skipInterfaceCheckForDimension(dimension)) {
            assertEquals(dimension == 1, (transform instanceof MathTransform1D));
            assertEquals(dimension == 2, (transform instanceof MathTransform2D));
        } else {
            assertFalse(transform instanceof MathTransform1D);
            assertFalse(transform instanceof MathTransform2D);
        }
        assertInstanceOf(Parameterized.class, transform);
        /*
         * End of MathTransformTestCase.validate(). Remaining is specific to LinearTransform implementations.
         */
        assertInstanceOf(LinearTransform.class, transform);
        final Matrix tm = ((LinearTransform) transform).getMatrix();
        assertTrue(Matrices.equals(matrix, tm, tolerance, false),
                "The matrix declared by the MathTransform is not equal to the one given at creation time.");

        assertSame(Affine.provider(transform.getSourceDimensions(), transform.getTargetDimensions(), true).getParameters(),
                   ((Parameterized) transform).getParameterDescriptors());
    }
}
