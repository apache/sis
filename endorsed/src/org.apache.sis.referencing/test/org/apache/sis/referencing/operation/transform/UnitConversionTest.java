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

import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.measure.Units;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link UnitConversion}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
public final class UnitConversionTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public UnitConversionTest() {
    }

    /**
     * Tests a linear conversion.
     */
    @Test
    public void testLinear() {
        final MathTransform tr = MathTransforms.convert(Units.KILOMETRE.getConverterTo(Units.METRE));
        final var linear = (LinearTransform1D) tr;
        assertEquals(1000, linear.scale,  STRICT);
        assertEquals(   0, linear.offset, STRICT);
    }

    /**
     * Tests a non-linear conversion.
     *
     * @throws TransformException if a test value cannot be transformed.
     */
    @Test
    public void testLogarithmic() throws TransformException {
        final MathTransform tr = MathTransforms.convert(Units.UNITY.getConverterTo(Units.DECIBEL));
        final var wrapper = (UnitConversion) tr;
        assertEquals(20, wrapper.transform(10), STRICT);
        assertEquals(10, wrapper.inverse().transform(20), STRICT);
    }
}
