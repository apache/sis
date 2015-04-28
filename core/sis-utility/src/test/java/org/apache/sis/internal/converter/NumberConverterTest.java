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
package org.apache.sis.internal.converter;

import java.math.BigInteger;
import java.math.BigDecimal;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the various {@link NumberConverter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(SystemRegistryTest.class)
public final strictfp class NumberConverterTest extends TestCase {
    /**
     * Creates a {@link NumberConverter} for the given source and target classes.
     * We have to use the {@link ConverterRegistry} instead than instantiating the
     * converters directly because some tests are going to verify that the converter
     * has been properly cached.
     */
    private static <S extends Number, T> ObjectConverter<S,T> create(
            final Class<S> sourceClass, final Class<T> targetClass)
    {
        final ObjectConverter<S,T> converter = SystemRegistry.INSTANCE.findExact(sourceClass, targetClass);
        assertInstanceOf("ConverterRegistry.find(" + sourceClass.getSimpleName() + ", " + targetClass.getSimpleName() + ')',
                (targetClass == Comparable.class) ? NumberConverter.Comparable.class : NumberConverter.class, converter);
        return converter;
    }

    /**
     * Asserts that conversion of the given {@code source} value produces the given {@code target} value.
     * The conversion is not expected to be invertible. This method is used for testing rounding behavior.
     */
    private static <S extends Number, T extends Number> void runConversion(
            final ObjectConverter<S,T> c, final S source, final T target, final S inverse)
            throws UnconvertibleObjectException
    {
        assertFalse(source.equals(inverse));
        assertEquals("Forward conversion.", target,  c.apply(source));
        assertEquals("Inverse conversion.", inverse, c.inverse().apply(target));
    }

    /**
     * Asserts that conversion of the given {@code source} value produces
     * the given {@code target} value, and tests the inverse conversion.
     */
    private static <S extends Number, T extends Number> void runInvertibleConversion(
            final ObjectConverter<S,T> c, final S source, final T target)
            throws UnconvertibleObjectException
    {
        assertEquals("Forward conversion.", target, c.apply(source));
        assertEquals("Inverse conversion.", source, c.inverse().apply(target));
        assertSame("Inconsistent inverse.", c, c.inverse().inverse());
        assertTrue("Invertible converters shall declare this capability.",
                c.properties().contains(FunctionProperty.INVERTIBLE));
    }

    /**
     * Tries to convert a value which is expected to fail.
     */
    private static <S extends Number> void tryUnconvertibleValue(final ObjectConverter<S,?> c, final S source) {
        try {
            c.apply(source);
            fail("Should not accept the value.");
        } catch (UnconvertibleObjectException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains(c.getTargetClass().getSimpleName()));
        }
    }

    /**
     * Tests conversions to {@link Byte} values.
     */
    @Test
    public void testByte() {
        final ObjectConverter<Integer, Byte> c = create(Integer.class, Byte.class);
        runInvertibleConversion(c, Integer.valueOf(-8), Byte.valueOf((byte) -8));
        runInvertibleConversion(c, Integer.valueOf(Byte.MIN_VALUE), Byte.valueOf(Byte.MIN_VALUE));
        runInvertibleConversion(c, Integer.valueOf(Byte.MAX_VALUE), Byte.valueOf(Byte.MAX_VALUE));
        tryUnconvertibleValue  (c, Integer.valueOf(Byte.MIN_VALUE - 1));
        tryUnconvertibleValue  (c, Integer.valueOf(Byte.MAX_VALUE + 1));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link Short} values.
     */
    @Test
    public void testShort() {
        final ObjectConverter<Integer, Short> c = create(Integer.class, Short.class);
        runInvertibleConversion(c, Integer.valueOf(-8), Short.valueOf((short) -8));
        runInvertibleConversion(c, Integer.valueOf(Short.MIN_VALUE), Short.valueOf(Short.MIN_VALUE));
        runInvertibleConversion(c, Integer.valueOf(Short.MAX_VALUE), Short.valueOf(Short.MAX_VALUE));
        tryUnconvertibleValue  (c, Integer.valueOf(Short.MIN_VALUE - 1));
        tryUnconvertibleValue  (c, Integer.valueOf(Short.MAX_VALUE + 1));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link Integer} values.
     */
    @Test
    public void testInteger() {
        final ObjectConverter<Float, Integer> c = create(Float.class, Integer.class);
        runInvertibleConversion(c, Float.valueOf(-8),    Integer.valueOf(-8));
        runConversion          (c, Float.valueOf(2.25f), Integer.valueOf(2), Float.valueOf(2f));
        runConversion          (c, Float.valueOf(2.75f), Integer.valueOf(3), Float.valueOf(3f));
        // Can not easily tests the values around Integer.MIN/MAX_VALUE because of rounding errors in float.
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link Long} values.
     */
    @Test
    public void testLong() {
        final ObjectConverter<Float, Long> c = create(Float.class, Long.class);
        runInvertibleConversion(c, Float.valueOf(-8), Long.valueOf(-8));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link Float} values.
     */
    @Test
    public void testFloat() {
        final ObjectConverter<Double, Float> c = create(Double.class, Float.class);
        runInvertibleConversion(c, Double.valueOf(2.5), Float.valueOf(2.5f));
        runConversion          (c, Double.valueOf(0.1), Float.valueOf(0.1f), Double.valueOf(0.1f));
        tryUnconvertibleValue  (c, Double.valueOf(1E+40));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link Double} values.
     */
    @Test
    public void testDouble() {
        final ObjectConverter<BigDecimal, Double> c = create(BigDecimal.class, Double.class);
        runInvertibleConversion(c, BigDecimal.valueOf(2.5), Double.valueOf(2.5));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link BigInteger} values.
     */
    @Test
    public void testBigInteger() {
        final ObjectConverter<Double, BigInteger> c = create(Double.class, BigInteger.class);
        runInvertibleConversion(c, Double.valueOf(1000), BigInteger.valueOf(1000));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link BigDecimal} values.
     */
    @Test
    public void testBigDecimal() {
        final ObjectConverter<Double, BigDecimal> c = create(Double.class, BigDecimal.class);
        runInvertibleConversion(c, Double.valueOf(2.5), BigDecimal.valueOf(2.5));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversion of a value having more digits than what the {@code double} type can hold.
     */
    @Test
    public void testLargeValue() {
        final long longValue = 1000000000000000010L;
        final double doubleValue = longValue;
        assertTrue(StrictMath.ulp(doubleValue) > 10); // Need to have more digits than 'double' capacity.
        runConversion(create(BigDecimal.class, Double.class),
                BigDecimal.valueOf(longValue), Double.valueOf(doubleValue), BigDecimal.valueOf(doubleValue));

        final ObjectConverter<BigDecimal, Long> c = create(BigDecimal.class, Long.class);
        final BigDecimal value = BigDecimal.valueOf(longValue);
        runInvertibleConversion(c, value, Long.valueOf(longValue));
        tryUnconvertibleValue(c, value.multiply(BigDecimal.valueOf(10)));
    }

    /**
     * Tests conversions to comparable objects. Should returns the object unchanged
     * since all {@link Number} subclasses are comparable.
     */
    @Test
    public void testComparable() {
        @SuppressWarnings("unchecked")
        final ObjectConverter<Number,Comparable<?>> c = create(Number.class, (Class) Comparable.class);
        final Integer value = 8;
        assertSame(value, c.apply(value));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }
}
