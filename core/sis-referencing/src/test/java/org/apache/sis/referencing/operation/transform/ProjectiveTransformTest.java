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

import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.parameter.Parameterized;

// Test imports
import org.opengis.test.Validators;
import org.apache.sis.test.DependsOn;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.After;
import static org.opengis.test.Assert.*;

// Branch-dependent imports
import org.junit.Test;
import org.junit.Ignore;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.test.referencing.TransformTestCase;


/**
 * Tests the {@link ProjectiveTransform} class by inheriting the tests defined in GeoAPI conformance module.
 * We use the {@link AffineTransform2D} class as a reference, so we need to avoid NaN values.
 * Note that {@link CopyTransformTest} will use {@code ProjectiveTransform} as a reference,
 * this time with NaN values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 */
@RunWith(JUnit4.class)
@DependsOn({AbstractMathTransformTest.class, ScaleTransformTest.class})
public strictfp class ProjectiveTransformTest extends TransformTestCase {
    /**
     * The factory to use for creating linear transforms.
     */
    private final MathTransformFactory mtFactory;

    /**
     * The matrix for the tested transform.
     */
    private Matrix matrix;

    /**
     * For {@link LinearTransformTest} constructor only.
     */
    ProjectiveTransformTest(final MathTransformFactory factory) {
        mtFactory = factory;
    }

    /**
     * Creates a new test suite.
     */
    public ProjectiveTransformTest() {
        this(new MathTransformFactoryBase() {
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
                     * Opportunistically tests ScaledTransform together with ProjectiveTransform.
                     * We takes ScaledTransform as a reference implementation since it is simpler.
                     */
                    tr = new TransformResultComparator(tr, pt, 0);
                }
                return tr;
            }
        });
    }

    /*
     * GeoAPI 3.1 defines the following tests. However since those tests are not available
     * in GeoAPI 3.0, we put empty placeholder. For running the real test, see for example
     * the JDK6 branch of Apache SIS.
     *
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

    static final String MESSAGE = "This test is not available in GeoAPI 3.0. "
            + "See Apache SIS JDK6, JDK7 or JDK8 branch for the actual tests.";

    @Test
    @Ignore(MESSAGE)
    public void testIdentity1D() throws FactoryException, TransformException {
    }

    @Test
    @Ignore(MESSAGE)
    public void testIdentity2D() throws FactoryException, TransformException {
    }

    @Test
    @Ignore(MESSAGE)
    public void testIdentity3D() throws FactoryException, TransformException {
    }

    @Test
    @Ignore(MESSAGE)
    public void testAxisSwapping2D() throws FactoryException, TransformException {
    }

    @Test
    @Ignore(MESSAGE)
    public void testSouthOrientated2D() throws FactoryException, TransformException {
    }

    @Test
    @Ignore(MESSAGE)
    public void testTranslatation2D() throws FactoryException, TransformException {
    }

    @Test
    @Ignore(MESSAGE)
    public void testUniformScale2D() throws FactoryException, TransformException {
    }

    @Test
    @Ignore(MESSAGE)
    public void testGenericScale2D() throws FactoryException, TransformException {
    }

    @Test
    @Ignore(MESSAGE)
    public void testRotation2D() throws FactoryException, TransformException {
    }

    @Test
    @Ignore(MESSAGE)
    public void testGeneral() throws FactoryException, TransformException {
    }

    @Test
    @Ignore(MESSAGE)
    public void testDimensionReduction() throws FactoryException, TransformException {
    }

    /**
     * Tests {@link ProjectiveTransform#optimize()}. In particular this method verifies that a non-square matrix
     * that looks like diagonal is not confused with a real diagonal matrix.
     *
     * @throws TransformException if a coordinate conversion failed.
     *
     * @since 0.7
     */
    @Test
    public void testOptimize() throws TransformException {
        matrix = Matrices.create(5, 4, new double[] {
            2, 0, 0, 0,
            0, 3, 0, 0,
            0, 0, 4, 0,
            0, 0, 0, 5,
            0, 0, 0, 1
        });
        transform = new ProjectiveTransform(matrix).optimize();
        assertInstanceOf("Non-diagonal matrix shall not be handled by ScaleTransform.", ProjectiveTransform.class, transform);
        verifyConsistency(new float[] {1, 2, 3,   -3, -2, -1});
        /*
         * Remove the "problematic" row. The new transform should now be optimizable.
         */
        matrix = ((MatrixSIS) matrix).removeRows(3, 4);
        transform = new ProjectiveTransform(matrix).optimize();
        assertInstanceOf("Diagonal matrix should be handled by a specialized class.", ScaleTransform.class, transform);
        verifyConsistency(new float[] {1, 2, 3,   -3, -2, -1});
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
        if (transform instanceof TransformResultComparator) {
            transform = ((TransformResultComparator) transform).tested;
        }
        /*
         * Below is a copy of MathTransformTestCase.validate(), with minor modifications
         * due to the fact that this class does not extend MathTransformTestCase.
         */
        assertNotNull("The 'transform' field shall be assigned a value.", transform);
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
