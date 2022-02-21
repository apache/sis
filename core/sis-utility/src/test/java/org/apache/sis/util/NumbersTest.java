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

import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.sis.math.Fraction;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.util.Numbers.*;


/**
 * Tests the {@link Numbers} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.3
 * @module
 */
public final strictfp class NumbersTest extends TestCase {
    /**
     * Tests {@link Numbers#isInteger(Class)}.
     */
    @Test
    public void testIsInteger() {
        assertTrue (isInteger(Byte      .TYPE));
        assertTrue (isInteger(Short     .TYPE));
        assertTrue (isInteger(Integer   .TYPE));
        assertTrue (isInteger(Long      .TYPE));
        assertFalse(isInteger(Float     .TYPE));
        assertFalse(isInteger(Double    .TYPE));
        assertTrue (isInteger(Byte      .class));
        assertTrue (isInteger(Short     .class));
        assertTrue (isInteger(Integer   .class));
        assertTrue (isInteger(Long      .class));
        assertFalse(isInteger(Float     .class));
        assertFalse(isInteger(Double    .class));
        assertFalse(isInteger(String    .class));
        assertFalse(isInteger(Character .class));
        assertFalse(isInteger(Fraction  .class));
        assertTrue (isInteger(BigInteger.class));
        assertFalse(isInteger(BigDecimal.class));
    }

    /**
     * Tests {@link Numbers#isFloat(Class)}.
     */
    @Test
    public void testIsFloat() {
        assertFalse(isFloat(Byte      .TYPE));
        assertFalse(isFloat(Short     .TYPE));
        assertFalse(isFloat(Integer   .TYPE));
        assertFalse(isFloat(Long      .TYPE));
        assertTrue (isFloat(Float     .TYPE));
        assertTrue (isFloat(Double    .TYPE));
        assertFalse(isFloat(Byte      .class));
        assertFalse(isFloat(Short     .class));
        assertFalse(isFloat(Integer   .class));
        assertFalse(isFloat(Long      .class));
        assertTrue (isFloat(Float     .class));
        assertTrue (isFloat(Double    .class));
        assertFalse(isFloat(String    .class));
        assertFalse(isFloat(Character .class));
        assertTrue (isFloat(Fraction  .class));
        assertFalse(isFloat(BigInteger.class));
        assertTrue (isFloat(BigDecimal.class));
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
        verifyPrimitiveToWrapper(Byte     .class,  Byte     .TYPE);
        verifyPrimitiveToWrapper(Short    .class,  Short    .TYPE);
        verifyPrimitiveToWrapper(Integer  .class,  Integer  .TYPE);
        verifyPrimitiveToWrapper(Long     .class,  Long     .TYPE);
        verifyPrimitiveToWrapper(Float    .class,  Float    .TYPE);
        verifyPrimitiveToWrapper(Double   .class,  Double   .TYPE);
        verifyPrimitiveToWrapper(Character.class,  Character.TYPE);
        verifyPrimitiveToWrapper(Boolean  .class,  Boolean  .TYPE);
        verifyPrimitiveToWrapper(Void     .class,  Void     .TYPE);
        assertSame(String.class, primitiveToWrapper(String.class));
    }

    /**
     * Asserts that calls to {@link Numbers#primitiveToWrapper(Class)} produces the expected wrapper.
     * The {@code <N>} parameter type is for making sure that e.g. {@link Integer#TYPE} has the same
     * type declaration than {@code Integer.class} despite being different {@link Class} instances.
     */
    private static <N> void verifyPrimitiveToWrapper(final Class<N> wrapper, final Class<N> primitive) {
        assertNotSame(wrapper, primitive);
        assertSame   (wrapper, primitiveToWrapper(primitive));
        assertSame   (wrapper, primitiveToWrapper(wrapper));
    }

    /**
     * Tests {@link Numbers#wrapperToPrimitive(Class)}.
     */
    @Test
    public void testWrapperToPrimitive() {
        verifyWrapperToPrimitive(Byte     .TYPE,  Byte     .class);
        verifyWrapperToPrimitive(Short    .TYPE,  Short    .class);
        verifyWrapperToPrimitive(Integer  .TYPE,  Integer  .class);
        verifyWrapperToPrimitive(Long     .TYPE,  Long     .class);
        verifyWrapperToPrimitive(Float    .TYPE,  Float    .class);
        verifyWrapperToPrimitive(Double   .TYPE,  Double   .class);
        verifyWrapperToPrimitive(Character.TYPE,  Character.class);
        verifyWrapperToPrimitive(Boolean  .TYPE,  Boolean  .class);
        verifyWrapperToPrimitive(Void     .TYPE,  Void     .class);
        assertSame(String.class, wrapperToPrimitive(String.class));
    }

    /**
     * Asserts that calls to {@link Numbers#wrapperToPrimitive(Class)} produces the expected wrapper.
     * The {@code <N>} parameter type is for making sure that e.g. {@link Integer#TYPE} has the same
     * type declaration than {@code Integer.class} despite being different {@link Class} instances.
     */
    private static <N> void verifyWrapperToPrimitive(final Class<N> primitive, final Class<N> wrapper) {
        assertNotSame(primitive, wrapper);
        assertSame   (primitive, wrapperToPrimitive(wrapper));
        assertSame   (primitive, wrapperToPrimitive(primitive));
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
     * Tests {@link Numbers#narrowestClass(Number)}.
     */
    @Test
    public void testNarrowestClassForValue() {
        assertEquals(Byte   .class, narrowestClass(    127.0));
        assertEquals(Short  .class, narrowestClass(    128.0));
        assertEquals(Integer.class, narrowestClass( 100000.0));
        assertEquals(Float  .class, narrowestClass(     10.5));
        assertEquals(Byte   .class, narrowestClass(   -128  ));
        assertEquals(Short  .class, narrowestClass(   -129  ));
        assertEquals(Integer.class, narrowestClass(-100000  ));
        assertEquals(Integer.class, narrowestClass((double) (1L << 30)));
        assertEquals(Float  .class, narrowestClass((double) (1L << 40)));
        assertEquals(Double .class, narrowestClass(StrictMath.PI));
    }

    /**
     * Tests {@link Numbers#narrowestNumber(Number)}.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testNarrowestNumber() {
        assertEquals(Byte   .valueOf((byte)   127),  narrowestNumber(    127.0));
        assertEquals(Short  .valueOf((short)  128),  narrowestNumber(    128.0));
        assertEquals(Integer.valueOf(      100000),  narrowestNumber( 100000.0));
        assertEquals(Float  .valueOf(       10.5f),  narrowestNumber(     10.5));
        assertEquals(Byte   .valueOf((byte)  -128),  narrowestNumber(   -128  ));
        assertEquals(Short  .valueOf((short) -129),  narrowestNumber(   -129  ));
        assertEquals(Integer.valueOf(     -100000),  narrowestNumber(-100000  ));
        assertEquals(Integer.valueOf(1  << 30),      narrowestNumber((double) (1L << 30)));
        assertEquals(Float  .valueOf(1L << 40),      narrowestNumber((double) (1L << 40)));
        assertEquals(Double .valueOf(StrictMath.PI), narrowestNumber(StrictMath.PI));
    }

    /**
     * Tests {@link Numbers#cast(Number, Class)}.
     */
    @Test
    public void testCast() {
        final Integer value = 10;
        assertEquals(Byte   .valueOf((byte)   10), cast(value, Byte   .class));
        assertEquals(Short  .valueOf((short)  10), cast(value, Short  .class));
        assertSame  (value,                        cast(value, Integer.class));
        assertEquals(Long   .valueOf((long)   10), cast(value, Long   .class));
        assertEquals(Float  .valueOf((float)  10), cast(value, Float  .class));
        assertEquals(Double .valueOf((double) 10), cast(value, Double .class));
    }

    /**
     * Tests {@link Numbers#wrap(double, Class)}.
     */
    @Test
    public void testWrap() {
        final double value = 10;
        assertEquals(Byte   .valueOf((byte)   10), wrap(value, Byte   .class));
        assertEquals(Short  .valueOf((short)  10), wrap(value, Short  .class));
        assertEquals(Integer.valueOf(         10), wrap(value, Integer.class));
        assertEquals(Long   .valueOf((long)   10), wrap(value, Long   .class));
        assertEquals(Float  .valueOf((float)  10), wrap(value, Float  .class));
        assertEquals(Double .valueOf((double) 10), wrap(value, Double .class));
        try {
            final Integer n = wrap(4.5, Integer.class);
            fail("Expected an exception but got " + n);
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("Integer"));
        }
    }
}
