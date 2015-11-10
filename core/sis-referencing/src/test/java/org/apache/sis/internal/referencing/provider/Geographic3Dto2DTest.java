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
package org.apache.sis.internal.referencing.provider;

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the {@link Geographic3Dto2D} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(AffineTest.class)
public final strictfp class Geographic3Dto2DTest extends TestCase {
    /**
     * Tests {@link Geographic3Dto2D#createMathTransform(ParameterValueGroup)}.
     *
     * @throws FactoryException should never happen.
     * @throws NoninvertibleTransformException should never happen.
     */
    @Test
    public void testCreateMathTransform() throws FactoryException, NoninvertibleTransformException {
        final Geographic3Dto2D provider = new Geographic3Dto2D();
        final MathTransform mt = provider.createMathTransform(null, null);
        assertSame("Expected cached instance.", mt, provider.createMathTransform(null, null));
        /*
         * Verify the full matrix. Note that the longitude offset is expected to be in degrees.
         * This conversion from grad to degrees is specific to Apache SIS and may be revised in
         * future version. See org.apache.sis.referencing.operation package javadoc.
         */
        assertInstanceOf("Shall be an affine transform.", LinearTransform.class, mt);
        assertMatrixEquals("Expected a Geographic 3D to 2D conversion.", Matrices.create(3, 4, new double[] {
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 0, 1}), ((LinearTransform) mt).getMatrix(), STRICT);
        assertMatrixEquals("Expected a Geographic 2D to 3D conversion.", Matrices.create(4, 3, new double[] {
                1, 0, 0,
                0, 1, 0,
                0, 0, 0,
                0, 0, 1}), ((LinearTransform) mt.inverse()).getMatrix(), STRICT);
    }
}
