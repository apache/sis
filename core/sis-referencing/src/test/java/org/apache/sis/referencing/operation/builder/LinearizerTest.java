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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.HardCodedConversions;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Linearizer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@DependsOn(LocalizationGridBuilderTest.class)
public final strictfp class LinearizerTest extends TestCase {
    /**
     * Tests {@link LinearTransformBuilder#approximate(MathTransform, Envelope)} on a transform created by
     * {@link LocalizationGridBuilder}. We verify that the {@link Linearizer#approximate(MathTransform,
     * Envelope)} short path is executed.
     *
     * @throws TransformException if an error occurred while computing test points.
     * @throws FactoryException if an error occurred while computing the localization grid.
     *
     * @see LinearTransformBuilderTest#testSetPointsFromTransform()
     */
    @Test
    public void testApproximate() throws TransformException, FactoryException {
        // Same set of points than `LinearTransformBuilderTest.testSetPointsFromTransform()`.
        final LinearTransformBuilder points = new LinearTransformBuilder(3, 5);
        points.setControlPoints(HardCodedConversions.mercator().getConversionFromBase().getMathTransform());

        // Non-linear transform producing the same values than the set of points;
        final LocalizationGridBuilder builder = new LocalizationGridBuilder(points);
        final MathTransform transform = builder.create(null);
        assertFalse(transform instanceof LinearTransform);

        // Linear approximation by Least Square Root method.
        final LinearTransform linear = LinearTransformBuilder.approximate(transform, new Envelope2D(null, 0, 0, 3, 5));
        org.opengis.test.Assert.assertMatrixEquals("linear",
                new Matrix3(111319, 0,   0,
                            0, 110662, -62,
                            0, 0, 1), linear.getMatrix(), 0.5);

        assertSame("Should have extracted the existing instance instead of computing a new one.", points.create(null), linear);
    }
}
