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
package org.apache.sis.converter;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link ArrayConverter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ArrayConverterTest extends TestCase {
    /**
     * Creates an array converter from {@link Integer} to {@link Double}.
     * The types need to be specified because we want to test wrapper and primitive types.
     */
    private static <S,T> ArrayConverter<S,T> create(final Class<S> sourceClass, final Class<T> targetClass) {
        return new ArrayConverter<>(sourceClass, targetClass, new NumberConverter<>(Integer.class, Double.class));
    }

    /**
     * Creates a new test case.
     */
    public ArrayConverterTest() {
    }

    /**
     * Tests conversions between wrapper classes.
     */
    @Test
    public void testWrapperTypes() {
        final ArrayConverter<Integer[], Double[]> converter = create(Integer[].class, Double[].class);
        final Integer[] source   = {4, 8, -6};
        final Double [] expected = {4.0, 8.0, -6.0};
        final Double [] actual   = converter.apply(source);
        assertArrayEquals(expected, actual);
    }

    /**
     * Tests conversions between wrapper classes.
     */
    @Test
    public void testPrimitiveTypes() {
        final ArrayConverter<int[], double[]> converter = create(int[].class, double[].class);
        final int[]    source   = {4, 8, -6};
        final double[] expected = {4.0, 8.0, -6.0};
        final double[] actual   = converter.apply(source);
        assertArrayEquals(expected, actual);
    }
}
