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
import org.apache.sis.referencing.operation.matrix.Matrices;

// Test imports
import org.junit.Test;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;

import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests the {@link CopyTransform} class.
 * Also opportunistically tests consistency with {@link ProjectiveTransform}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
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
        assertIsIdentity(transform);
        assertParameterEquals(Affine.getProvider(3, 3, true).getParameters(), null);

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
    public void testConstantDimension() throws TransformException {
        create(3, 2, 1, 0);
        assertIsNotIdentity(transform);

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
    @DependsOnMethod("testConstantDimension")
    public void testDimensionReduction() throws TransformException {
        isInverseTransformSupported = false;
        create(3, 0, 1);
        assertIsNotIdentity(transform);

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

    /**
     * Tests a transform with more output dimensions than input dimensions.
     * The extra dimension has values set to 0. This kind of transform happen
     * in the inverse of <cite>"Geographic 3D to 2D conversion"</cite> (EPSG:9659).
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testDimensionReduction")
    public void testDimensionAugmentation() throws TransformException {
        transform = new ProjectiveTransform(Matrices.create(4, 3, new double[] {
                0, 1, 0,
                1, 0, 0,
                0, 0, 0,
                0, 0, 1}));

        assertInstanceOf("inverse", CopyTransform.class, transform.inverse());
        verifyTransform(new double[] {2,3,    6,0,    2, Double.NaN},
                        new double[] {3,2,0,  0,6,0,  Double.NaN, 2, 0});
    }
}
