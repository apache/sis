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

import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.Affine;

// Test imports
import org.junit.Test;
import org.apache.sis.test.DependsOn;

import static org.junit.Assert.*;


/**
 * Tests the {@link CopyTransform} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
@DependsOn({
    CoordinateDomainTest.class,
    ProjectiveTransformTest.class
})
public final strictfp class CopyTransformTest extends MathTransformTestCase {
    /**
     * Generates random ordinates with approximatively 5% of NaN values in the array.
     */
    private double[] generateRandomCoordinates() {
        return generateRandomCoordinates(CoordinateDomain.GEOGRAPHIC, 0.05f);
    }

    /**
     * Replaces the current {@link CopyTransform} by an instance of {@link ProjectiveTransform}.
     */
    private void makeProjectiveTransform() {
        transform = new ProjectiveTransform(((CopyTransform) transform).getMatrix());
    }

    /**
     * Initializes the {@link #transform} field to a {@link CopyTransform} instance created
     * from the given argument. Then verifies that the matrix is consistent with the transform.
     */
    private void create(final int srcDim, final int... indices) {
        transform = new CopyTransform(srcDim, indices);
        assertEquals(transform, CopyTransform.create(((LinearTransform) transform).getMatrix()));
        validate();
    }

    /**
     * Tests an identity transform.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testIdentity() throws TransformException {
        create(3, 0, 1, 2);
        verifyParameters(Affine.getProvider(3, 3, true).getParameters(), null);
        verifyIsIdentity(true);

        final double[] source = generateRandomCoordinates();
        final double[] target = source.clone();
        verifyTransform(source, target);

        makeProjectiveTransform();
        verifyTransform(source, target);
    }

    /**
     * Tests transform from 3D to 3D.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void test3D() throws TransformException {
        create(3, 2, 1, 0);
        verifyIsIdentity(false);

        final double[] source = generateRandomCoordinates();
        final double[] target = new double[source.length];
        for (int i=0; i<source.length; i++) {
            final int r = i % 3;
            final int b = i - r;
            target[b + (2-r)] = source[i];
        }
        verifyTransform(source, target);

        makeProjectiveTransform();
        verifyTransform(source, target);
    }

    /**
     * Tests transform from 3D to 2D.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void test3Dto2D() throws TransformException {
        isInverseTransformSupported = false;
        create(3, 0, 1);
        verifyIsIdentity(false);

        final double[] source = generateRandomCoordinates();
        final double[] target = new double[source.length * 2/3];
        for (int i=0,j=0; i<source.length; i++) {
            target[j++] = source[i++];
            target[j++] = source[i++];
            // Skip one i (in the for loop).
        }
        verifyTransform(source, target);

        makeProjectiveTransform();
        verifyTransform(source, target);
    }
}
