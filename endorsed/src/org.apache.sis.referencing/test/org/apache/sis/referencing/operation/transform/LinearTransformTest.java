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
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests various implementation of the {@link LinearTransform} interface by inheriting the tests defined
 * in GeoAPI conformance module. The transforms are created by {@link MathTransforms#linear(Matrix)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LinearTransformTest extends ProjectiveTransformTest {
    /**
     * Creates a new test suite.
     */
    public LinearTransformTest() {
    }

    /**
     * Creates the math transform to test.
     */
    @Override
    MathTransform createAffineTransform(final Matrix matrix) {
        return MathTransforms.linear(matrix);
    }

    /**
     * Do not skip any interface check - all of them shall be correct in this test case.
     */
    @Override
    boolean skipInterfaceCheckForDimension(final int dimension) {
        return false;
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testIdentity1D() throws FactoryException, TransformException {
        super.testIdentity1D();
        assertInstanceOf(IdentityTransform1D.class, transform, "Unexpected implementation.");
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testIdentity2D() throws FactoryException, TransformException {
        super.testIdentity2D();
        assertInstanceOf(AffineTransform2D.class, transform, "Unexpected implementation.");
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testIdentity3D() throws FactoryException, TransformException {
        super.testIdentity3D();
        assertInstanceOf(IdentityTransform.class, transform, "Unexpected implementation.");
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testAxisSwapping2D() throws FactoryException, TransformException {
        super.testAxisSwapping2D();
        assertInstanceOf(AffineTransform2D.class, transform, "Unexpected implementation.");
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testSouthOrientated2D() throws FactoryException, TransformException {
        super.testSouthOrientated2D();
        assertInstanceOf(AffineTransform2D.class, transform, "Unexpected implementation.");
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testTranslatation2D() throws FactoryException, TransformException {
        super.testTranslatation2D();
        assertInstanceOf(AffineTransform2D.class, transform, "Unexpected implementation.");
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testUniformScale2D() throws FactoryException, TransformException {
        super.testUniformScale2D();
        assertInstanceOf(AffineTransform2D.class, transform, "Unexpected implementation.");
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testGenericScale2D() throws FactoryException, TransformException {
        super.testGenericScale2D();
        assertInstanceOf(AffineTransform2D.class, transform, "Unexpected implementation.");
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testRotation2D() throws FactoryException, TransformException {
        super.testRotation2D();
        assertInstanceOf(AffineTransform2D.class, transform, "Unexpected implementation.");
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testGeneral() throws FactoryException, TransformException {
        super.testGeneral();
        assertInstanceOf(AffineTransform2D.class, transform, "Unexpected implementation.");
    }

    /**
     * Runs the GeoAPI tests, then perform implementation-specific checks.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testDimensionReduction() throws FactoryException, TransformException {
        super.testDimensionReduction();
        assertInstanceOf(ProjectiveTransform.class, transform, "Unexpected implementation.");
    }
}
