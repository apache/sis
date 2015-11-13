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

import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.parameter.DefaultParameterDescriptorGroupTest;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.toRadians;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link ContextualParameters}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(DefaultParameterDescriptorGroupTest.class)
public final strictfp class ContextualParametersTest extends TestCase {
    /**
     * Creates an instance to use for testing purpose.
     */
    private static ContextualParameters create(final int srcDim, final int dstDim) {
        return new ContextualParameters(new DefaultOperationMethod(
                Collections.singletonMap(DefaultOperationMethod.NAME_KEY, "Test method"),
                srcDim, dstDim, DefaultParameterDescriptorGroupTest.M1_M1_O1_O2));
    }

    /**
     * Tests {@link ContextualParameters#parameter(String)} and {@link ContextualParameters#values()}.
     */
    @Test
    public void testParameters() {
        final ContextualParameters p = create(1, 1);
        assertTrue("values().isEmpty()",       p.values().isEmpty());
        assertTrue("normalize.isIdentity()",   p.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION).isIdentity());
        assertTrue("denormalize.isIdentity()", p.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION).isIdentity());
        assertTrue("normalize.isIdentity()",   p.getMatrix(ContextualParameters.MatrixRole.INVERSE_NORMALIZATION).isIdentity());
        assertTrue("denormalize.isIdentity()", p.getMatrix(ContextualParameters.MatrixRole.INVERSE_DENORMALIZATION).isIdentity());

        final ParameterValue<?> p1 = p.parameter("Mandatory 1");
        final ParameterValue<?> p2 = p.parameter("Mandatory 2");
        try {
            p.parameter("Mandatory 3");
            fail("Shall not find a non-existent parameter.");
        } catch (ParameterNotFoundException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("Mandatory 3"));
        }

        assertNotSame(p1, p2);
        assertSame(p1, p.parameter("Mandatory 1"));
        assertSame(p2, p.parameter("Mandatory 2"));
        assertEquals("values().size()", 2, p.values().size());
        assertArrayEquals("values.toArray()", new ParameterValue<?>[] {p1, p2}, p.values().toArray());
    }

    /**
     * Tests {@link ContextualParameters#completeTransform(MathTransformFactory, MathTransform)}
     * with identity normalization / denormalization transform. The complete transform should be
     * equals to the kernel (often the same instance, but not necessarily because of caching).
     *
     * @throws FactoryException Should never happen.
     */
    @Test
    public void testSameTransform() throws FactoryException {
        final ContextualParameters p = create(1, 1);
        p.parameter("Mandatory 1").setValue(4);
        final MathTransform kernel = MathTransforms.linear(3, 4);
        assertEquals(kernel, p.completeTransform(DefaultMathTransformFactoryTest.factory(), kernel));
        try {
            p.parameter("Mandatory 1").setValue(10);
            fail("Shall not be allowed to modify an immutable instance.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("ParameterValue"));
        }
    }

    /**
     * Tests {@link ContextualParameters#completeTransform(MathTransformFactory, MathTransform)}
     * with non-identity normalization transforms.
     *
     * @throws FactoryException Should never happen.
     */
    @Test
    @DependsOnMethod("testSameTransform")
    public void testCompleteTransform() throws FactoryException {
        final ContextualParameters p = create(2, 2);
        final Matrix normalize   = p.normalizeGeographicInputs(12);
        final Matrix denormalize = p.denormalizeGeographicOutputs(18);
        final Matrix product     = MathTransforms.getMatrix(p.completeTransform(
                DefaultMathTransformFactoryTest.factory(), MathTransforms.identity(2)));

        assertMatrixEquals("normalize", new Matrix3(
                PI/180,  0,       toRadians(-12),
                0,       PI/180,  0,
                0,       0,       1), normalize, 1E-16);

        assertMatrixEquals("denormalize", new Matrix3(
                180/PI,  0,       18,
                0,       180/PI,  0,
                0,       0,       1), denormalize, STRICT);

        assertMatrixEquals("product", new Matrix3(
                1, 0, 6,
                0, 1, 0,
                0, 0, 1), product, STRICT);
    }
}
