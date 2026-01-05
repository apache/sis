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
package org.apache.sis.math;

import java.math.BigDecimal;
import java.math.BigInteger;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link NumberType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class NumberTypeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public NumberTypeTest() {
    }

    /**
     * Tests {@link NumberType#forNumberClasses(Class...)}.
     */
    @Test
    public void testForNumberClasses() {
        assertEquals(NumberType.NULL,   NumberType.forNumberClasses());
        assertEquals(NumberType.FLOAT,  NumberType.forNumberClasses(Float.class));
        assertEquals(NumberType.FLOAT,  NumberType.forNumberClasses(Float.TYPE, Long.class));
        assertEquals(NumberType.DOUBLE, NumberType.forNumberClasses(Float.class, Double.TYPE, Integer.class));
    }

    /**
     * Tests {@link NumberType#isInteger(Class)}.
     */
    @Test
    public void testIsInteger() {
        assertTrue (NumberType.isInteger(Byte      .TYPE));
        assertTrue (NumberType.isInteger(Short     .TYPE));
        assertTrue (NumberType.isInteger(Integer   .TYPE));
        assertTrue (NumberType.isInteger(Long      .TYPE));
        assertFalse(NumberType.isInteger(Float     .TYPE));
        assertFalse(NumberType.isInteger(Double    .TYPE));
        assertTrue (NumberType.isInteger(Byte      .class));
        assertTrue (NumberType.isInteger(Short     .class));
        assertTrue (NumberType.isInteger(Integer   .class));
        assertTrue (NumberType.isInteger(Long      .class));
        assertFalse(NumberType.isInteger(Float     .class));
        assertFalse(NumberType.isInteger(Double    .class));
        assertFalse(NumberType.isInteger(String    .class));
        assertFalse(NumberType.isInteger(Character .class));
        assertFalse(NumberType.isInteger(Fraction  .class));
        assertTrue (NumberType.isInteger(BigInteger.class));
        assertFalse(NumberType.isInteger(BigDecimal.class));
    }

    /**
     * Tests {@link NumberType#isFractional(Class)}.
     */
    @Test
    public void testIsFloat() {
        assertFalse(NumberType.isFractional(Byte      .TYPE));
        assertFalse(NumberType.isFractional(Short     .TYPE));
        assertFalse(NumberType.isFractional(Integer   .TYPE));
        assertFalse(NumberType.isFractional(Long      .TYPE));
        assertTrue (NumberType.isFractional(Float     .TYPE));
        assertTrue (NumberType.isFractional(Double    .TYPE));
        assertFalse(NumberType.isFractional(Byte      .class));
        assertFalse(NumberType.isFractional(Short     .class));
        assertFalse(NumberType.isFractional(Integer   .class));
        assertFalse(NumberType.isFractional(Long      .class));
        assertTrue (NumberType.isFractional(Float     .class));
        assertTrue (NumberType.isFractional(Double    .class));
        assertFalse(NumberType.isFractional(String    .class));
        assertFalse(NumberType.isFractional(Character .class));
        assertTrue (NumberType.isFractional(Fraction  .class));
        assertFalse(NumberType.isFractional(BigInteger.class));
        assertTrue (NumberType.isFractional(BigDecimal.class));
    }

    /**
     * Tests {@link NumberType#size()}.
     */
    @Test
    public void testSize() {
        assertEquals(Byte   .SIZE, NumberType.forNumberClass(Byte   .class).size().orElseThrow());
        assertEquals(Short  .SIZE, NumberType.forNumberClass(Short  .class).size().orElseThrow());
        assertEquals(Integer.SIZE, NumberType.forNumberClass(Integer.class).size().orElseThrow());
        assertEquals(Long.   SIZE, NumberType.forNumberClass(Long   .class).size().orElseThrow());
        assertEquals(Float  .SIZE, NumberType.forNumberClass(Float  .class).size().orElseThrow());
        assertEquals(Double .SIZE, NumberType.forNumberClass(Double .class).size().orElseThrow());
    }

    /**
     * Tests {@link NumberType#primitiveToWrapper(Class)}.
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
        assertSame(String.class, NumberType.primitiveToWrapper(String.class));
    }

    /**
     * Asserts that calls to {@link NumberType#primitiveToWrapper(Class)} produce the expected wrapper.
     * The {@code <N>} parameter type is for making sure that e.g. {@link Integer#TYPE} has the same
     * type declaration as {@code Integer.class} despite being different {@link Class} instances.
     */
    private static <N> void verifyPrimitiveToWrapper(final Class<N> wrapper, final Class<N> primitive) {
        assertNotSame(wrapper, primitive);
        assertSame   (wrapper, NumberType.primitiveToWrapper(primitive));
        assertSame   (wrapper, NumberType.primitiveToWrapper(wrapper));
    }

    /**
     * Tests {@link NumberType#wrapperToPrimitive(Class)}.
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
        assertSame(String.class, NumberType.wrapperToPrimitive(String.class));
    }

    /**
     * Asserts that calls to {@link NumberType#wrapperToPrimitive(Class)} produce the expected wrapper.
     * The {@code <N>} parameter type is for making sure that e.g. {@link Integer#TYPE} has the same
     * type declaration as {@code Integer.class} despite being different {@link Class} instances.
     */
    private static <N> void verifyWrapperToPrimitive(final Class<N> primitive, final Class<N> wrapper) {
        assertNotSame(primitive, wrapper);
        assertSame   (primitive, NumberType.wrapperToPrimitive(wrapper));
        assertSame   (primitive, NumberType.wrapperToPrimitive(primitive));
    }

    /**
     * Tests {@link NumberType#isConversionLossless(NumberType)}.
     * Opportunistically tests also related methods.
     */
    @Test
    public void testIsConversionLossless() {
        assertTrue (NumberType.FLOAT  .isConversionLossless(NumberType.DOUBLE));
        assertFalse(NumberType.DOUBLE .isConversionLossless(NumberType.FLOAT));
        assertFalse(NumberType.INTEGER.isConversionLossless(NumberType.FLOAT));
        assertTrue (NumberType.INTEGER.isConversionLossless(NumberType.DOUBLE));
        assertFalse(NumberType.LONG   .isConversionLossless(NumberType.DOUBLE));
        assertTrue (NumberType.LONG   .isConversionLossless(NumberType.BIG_DECIMAL));
        final NumberType[] values = NumberType.values();
        for (int i=0; i<values.length; i++) {
            final NumberType value = values[i];
            final String label = value.name();
            assertTrue(value.isConversionLossless(value), label);
            for (int j=0; j<i; j++) {
                final NumberType other = values[j];
                assertFalse(value.isConversionLossless(other), label);
                assertFalse(value.isNarrowerThan(other), label);
                assertFalse(other.isWiderThan(value), label);
            }
            for (int j=i; ++j < values.length;) {
                final NumberType other = values[j];
                if (value.isConversionLossless(other)) {
                    assertTrue(other.isWiderThan(value), label);
                    assertTrue(value.isNarrowerThan(other), label);
                }
            }
        }
    }
}
