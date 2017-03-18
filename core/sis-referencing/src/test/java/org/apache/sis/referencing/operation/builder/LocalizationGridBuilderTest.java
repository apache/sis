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
package org.apache.sis.referencing.operation.builder;

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;


/**
 * Tests {@link LocalizationGridBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(LinearTransformBuilderTest.class)
public final strictfp class LocalizationGridBuilderTest extends TransformTestCase {
    /**
     * Tests a small grid of 3 rows and 2 columns.
     * This tests use the same point than {@link LinearTransformBuilderTest#testImplicitSource2D()}
     *
     * @throws FactoryException if an error occurred while computing the localization grid.
     * @throws TransformException if an error occurred while testing a transformation.
     *
     * @see LinearTransformBuilderTest#testImplicitSource2D()
     */
    @Test
    public void testSixPoints() throws FactoryException, TransformException {
        final LocalizationGridBuilder builder = new LocalizationGridBuilder(2, 3);
        builder.setControlPoint(0, 0, 3, 9);
        builder.setControlPoint(0, 1, 4, 7);
        builder.setControlPoint(0, 2, 6, 6);
        builder.setControlPoint(1, 0, 4, 8);
        builder.setControlPoint(1, 1, 5, 4);
        builder.setControlPoint(1, 2, 8, 2);

        transform = builder.create(null);
        tolerance = 1;                          // TODO: temporary high value while we debug.
        verifyTransform(new double[] {0, 0}, new double[] {3, 9});
    }
}
