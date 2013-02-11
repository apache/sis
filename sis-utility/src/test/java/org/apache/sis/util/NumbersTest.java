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
package org.apache.sis.util;

import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;
import static org.apache.sis.util.Numbers.*;


/**
 * Tests the {@link Numbers} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class NumbersTest extends TestCase {
    /**
     * Tests {@link Numbers#isInteger(Class)}.
     */
    @Test
    public void testIsInteger() {
        assertTrue (isInteger(Byte     .TYPE));
        assertTrue (isInteger(Short    .TYPE));
        assertTrue (isInteger(Integer  .TYPE));
        assertTrue (isInteger(Long     .TYPE));
        assertFalse(isInteger(Float    .TYPE));
        assertFalse(isInteger(Double   .TYPE));
        assertTrue (isInteger(Byte     .class));
        assertTrue (isInteger(Short    .class));
        assertTrue (isInteger(Integer  .class));
        assertTrue (isInteger(Long     .class));
        assertFalse(isInteger(Float    .class));
        assertFalse(isInteger(Double   .class));
        assertFalse(isInteger(String   .class));
        assertFalse(isInteger(Character.class));
    }

    /**
     * Tests {@link Numbers#isFloat(Class)}.
     */
    @Test
    public void testIsFloat() {
        assertFalse(isFloat(Byte     .TYPE));
        assertFalse(isFloat(Short    .TYPE));
        assertFalse(isFloat(Integer  .TYPE));
        assertFalse(isFloat(Long     .TYPE));
        assertTrue (isFloat(Float    .TYPE));
        assertTrue (isFloat(Double   .TYPE));
        assertFalse(isFloat(Byte     .class));
        assertFalse(isFloat(Short    .class));
        assertFalse(isFloat(Integer  .class));
        assertFalse(isFloat(Long     .class));
        assertTrue (isFloat(Float    .class));
        assertTrue (isFloat(Double   .class));
        assertFalse(isFloat(String   .class));
        assertFalse(isFloat(Character.class));
    }

    /**
     * Tests {@link Numbers#primitiveBitCount(Class)}.
     */
    @Test
    public void testPrimitiveBitCount() {
        assertEquals(Byte   .SIZE, primitiveBitCount(Byte   .class));
        assertEquals(Short  .SIZE, primitiveBitCount(Short  .class));
        assertEquals(Integer.SIZE, primitiveBitCount(Integer.class));
        assertEquals(Long.   SIZE, primitiveBitCount(Long   .class));
        assertEquals(Float  .SIZE, primitiveBitCount(Float  .class));
        assertEquals(Double .SIZE, primitiveBitCount(Double .class));
    }

    /**
     * Tests {@link Numbers#primitiveToWrapper(Class)}.
     */
    @Test
    public void testPrimitiveToWrapper() {
        assertEquals(Byte   .class, primitiveToWrapper(Byte   .TYPE));
        assertEquals(Short  .class, primitiveToWrapper(Short  .TYPE));
        assertEquals(Integer.class, primitiveToWrapper(Integer.TYPE));
        assertEquals(Long   .class, primitiveToWrapper(Long   .TYPE));
        assertEquals(Float  .class, primitiveToWrapper(Float  .TYPE));
        assertEquals(Double .class, primitiveToWrapper(Double .TYPE));
        assertEquals(Byte   .class, primitiveToWrapper(Byte   .class));
        assertEquals(Short  .class, primitiveToWrapper(Short  .class));
        assertEquals(Integer.class, primitiveToWrapper(Integer.class));
        assertEquals(Long   .class, primitiveToWrapper(Long   .class));
        assertEquals(Float  .class, primitiveToWrapper(Float  .class));
        assertEquals(Double .class, primitiveToWrapper(Double .class));
    }

    /**
     * Tests {@link Numbers#wrapperToPrimitive(Class)}.
     */
    @Test
    public void testWrapperToPrimitive() {
        assertEquals(Byte   .TYPE, wrapperToPrimitive(Byte   .TYPE));
        assertEquals(Short  .TYPE, wrapperToPrimitive(Short  .TYPE));
        assertEquals(Integer.TYPE, wrapperToPrimitive(Integer.TYPE));
        assertEquals(Long   .TYPE, wrapperToPrimitive(Long   .TYPE));
        assertEquals(Float  .TYPE, wrapperToPrimitive(Float  .TYPE));
        assertEquals(Double .TYPE, wrapperToPrimitive(Double .TYPE));
        assertEquals(Byte   .TYPE, wrapperToPrimitive(Byte   .class));
        assertEquals(Short  .TYPE, wrapperToPrimitive(Short  .class));
        assertEquals(Integer.TYPE, wrapperToPrimitive(Integer.class));
        assertEquals(Long   .TYPE, wrapperToPrimitive(Long   .class));
        assertEquals(Float  .TYPE, wrapperToPrimitive(Float  .class));
        assertEquals(Double .TYPE, wrapperToPrimitive(Double .class));
    }

    /**
     * Tests {@link Numbers#widestClass(Class, Class)}.
     */
    @Test
    public void testWidestClass() {
        assertEquals(Byte   .class, widestClass(Byte.class,    Byte.class));
        assertEquals(Integer.class, widestClass(Byte.class,    Integer.class));
        assertEquals(Integer.class, widestClass(Integer.class, Byte.class));
        assertEquals(Float  .class, widestClass(Integer.class, Float.class));
    }

    /**
     * Tests {@link Numbers#narrowestClass(Class, Class)}.
     */
    @Test
    public void testNarrowestClass() {
        assertEquals(Byte   .class, narrowestClass(Byte.class,    Byte.class));
        assertEquals(Byte   .class, narrowestClass(Byte.class,    Integer.class));
        assertEquals(Byte   .class, narrowestClass(Integer.class, Byte.class));
        assertEquals(Integer.class, narrowestClass(Integer.class, Float.class));
    }

    /**
     * Tests {@link Numbers#narrowestClass(double)}.
     */
    @Test
    public void testNarrowestClassForValue() {
        assertEquals(Byte   .class, narrowestClass(    10.0));
        assertEquals(Short  .class, narrowestClass(  1000.0));
        assertEquals(Integer.class, narrowestClass(100000.0));
        assertEquals(Float  .class, narrowestClass(    10.5));
    }

    /**
     * Tests {@link Numbers#narrowestNumber(double)}.
     */
    @Test
    public void testNarrowestNumber() {
        assertEquals(Byte   .valueOf((byte)    10), narrowestNumber(    10.0));
        assertEquals(Short  .valueOf((short) 1000), narrowestNumber(  1000.0));
        assertEquals(Integer.valueOf(      100000), narrowestNumber(100000.0));
        assertEquals(Float  .valueOf(       10.5f), narrowestNumber(    10.5));
    }

    /**
     * Tests {@link Numbers#cast(Number, Class)}.
     */
    @Test
    public void testCast() {
        assertEquals(Byte .valueOf((byte)  10), cast(Integer.valueOf(10), Byte .class));
        assertEquals(Float.valueOf((float) 10), cast(Integer.valueOf(10), Float.class));
    }
}
