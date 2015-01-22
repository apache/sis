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

import org.opengis.referencing.operation.Matrix;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link LinearTransformBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class LinearTransformBuilderTest extends TestCase {
    /**
     * Tests a very simple case where an exact answer is expected.
     */
    @Test
    public void testExact() {
        final LinearTransformBuilder builder = new LinearTransformBuilder();
        builder.setSourcePoints(
                new DirectPosition2D(1, 1),
                new DirectPosition2D(1, 2),
                new DirectPosition2D(2, 2));
        builder.setTargetPoints(
                new DirectPosition2D(3, 2),
                new DirectPosition2D(3, 5),
                new DirectPosition2D(5, 5));
        final Matrix m = builder.create().getMatrix();

        // First row (x)
        assertEquals( 2, m.getElement(0, 0), 0);
        assertEquals( 0, m.getElement(0, 1), 1E-20);
        assertEquals( 1, m.getElement(0, 2), 0);

        // Second row (y)
        assertEquals( 0, m.getElement(1, 0), 0);
        assertEquals( 3, m.getElement(1, 1), 0);
        assertEquals(-1, m.getElement(1, 2), 0);
    }
}
