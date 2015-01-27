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

import java.util.Deque;
import java.util.ArrayDeque;
import java.io.Serializable;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link ConverterRegistry} implementation. Every tests in this class uses their own
 * instance of {@link ConverterRegistry}, so they are not affected by whatever new converter may
 * be added or removed from the system-wide registry.
 *
 * <p>This test shall not perform any conversion neither serialization. It shall only ensures
 * that the converters are properly registered. This is because some {@link SystemConverter}
 * methods may query back {@link SystemRegistry#INSTANCE} while we want to keep the tests
 * isolated.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn({
    StringConverterTest.class, FallbackConverterTest.class,
    org.apache.sis.util.collection.TreeTableFormatTest.class
})
public final strictfp class ConverterRegistryTest extends TestCase {
    /**
     * The registry being tested.
     */
    private final ConverterRegistry registry = new ConverterRegistry();

    /**
     * All converters registered in a test case. Only the converter type and properties
     * will be verified; no conversion or serialization shall be attempted.
     */
    private final Deque<ObjectConverter<?,?>> converters = new ArrayDeque<ObjectConverter<?,?>>();

    /**
     * Registers a converter to test.
     */
    private void register(final ObjectConverter<?,?> converter) {
        assertNotNull("Missing ObjectConverter", converter);
        converters.add(converter);
        registry.register(converter);
    }

    /**
     * Ensures that all converters tested so far are still registered.
     */
    private void assertAllConvertersAreRegistered() {
        for (final ObjectConverter<?,?> converter : converters) {
            assertSame(converter, registry.find(converter.getSourceClass(), converter.getTargetClass()));
        }
    }

    /**
     * Ensures that the current converters is also registered for the given target class.
     * The given target may not be the same than the {@link ObjectConverter#getTargetClass()}.
     *
     * @param targetClass The target class to ensure that the converter is registered for.
     */
    private void assertSameConverterForTarget(final Class<?> targetClass) {
        final ObjectConverter<?,?> converter = converters.peekLast();
        final Class<?> sourceClass = converter.getSourceClass();
        final ObjectConverter<?,?> actual;
        try {
            actual = registry.find(sourceClass, targetClass);
        } catch (UnconvertibleObjectException e) {
            fail("Converter ‘" + converter + "‛ was expected to be applicable to ‘" +
                    targetClass.getSimpleName() + "‛ but got " + e);
            return;
        }
        assertSame("Same converter shall be applicable to other target.", converter, actual);
    }

    /**
     * Ensures that there is no converter for the given target.
     *
     * @param targetClass The target which should not have any registered converter.
     */
    private void assertNoConverterForTarget(final Class<?> targetClass) {
        final ObjectConverter<?,?> converter = converters.peekLast();
        final Class<?> sourceClass = converter.getSourceClass();
        final ObjectConverter<?,?> actual;
        try {
            actual = registry.find(sourceClass, targetClass);
        } catch (UnconvertibleObjectException e) {
            // This is the expected exception
            final String message = e.getMessage();
            assertTrue(message, message.contains(sourceClass.getSimpleName()));
            assertTrue(message, message.contains(targetClass.getSimpleName()));
            return;
        }
        fail("Expected no converter from ‘" + sourceClass.getSimpleName() + "‛ to ‘" +
                targetClass.getSimpleName() + "‛ but got " + actual);
    }

    /**
     * Ensures that the converter for the given target is an {@link IdentityConverter}.
     *
     * @param targetClass The target for which an identity converter should be obtained.
     */
    private void assertIdentityForTarget(final Class<?> targetClass) {
        final ObjectConverter<?,?> converter = converters.peekLast();
        final Class<?> sourceClass = converter.getSourceClass();
        final ObjectConverter<?,?> actual = registry.find(sourceClass, targetClass);
        final String message = sourceClass.getSimpleName() + " ← " + targetClass.getSimpleName();
        assertInstanceOf(message, IdentityConverter.class, actual);
        assertSame(message, sourceClass, actual.getSourceClass());
        assertSame(message, targetClass, actual.getTargetClass());
    }

    /**
     * Asserts that the converter to the given target is a fallback having the given string
     * representation.
     *
     * @param expected The expected string representation of the fallback.
     */
    private void assertFallbackEquals(final Class<?> target, final String expected) {
        ObjectConverter<?,?> converter = converters.peekLast();
        final Class<?> sourceClass = converter.getSourceClass();
        converter = registry.find(sourceClass, target);
        assertMultilinesEquals(expected, converter.toString());
    }

    /**
     * Tests registration of converters from {@link String} to miscellaneous objects.
     * This method tests the addition of many converters because we want to observe
     * how {@linkplain #registry} grow in reaction to those additions.
     *
     * <p>This test compares the string representations for convenience.  In theory those string
     * representations are not committed API, so if the {@code FallbackConverter} implementation
     * change, it is okay to update this test accordingly.</p>
     */
    @Test
    public void testStringToMiscellaneous() {
        assertAllConvertersAreRegistered();
        register(new StringConverter.Short());
        assertSameConverterForTarget(Short       .class);
        assertSameConverterForTarget(Number      .class);
        assertIdentityForTarget     (Object      .class);
        assertNoConverterForTarget  (Cloneable   .class);
        assertIdentityForTarget     (Comparable  .class);
        assertIdentityForTarget     (Serializable.class);
        assertMultilinesEquals("After StringConverter.Short",
            "ConverterRegistry\n" +
            "  ├─Short         ← String\n" +
            "  ├─Number        ← String\n" +  // Same instance than above, applied to Number target.
            "  │   └─Short     ← String\n" +
            "  ├─Object        ← String\n" +
            "  ├─Comparable    ← String\n" +
            "  └─Serializable  ← String\n", registry.toString());
        /*
         * Adds String ← Long
         * Expected side-effect: creation of FallbackConverter[String ← Number]
         */
        assertAllConvertersAreRegistered();
        assertNoConverterForTarget(Long.class);
        register(new StringConverter.Long());
        assertSameConverterForTarget(Long        .class);
        assertIdentityForTarget     (Object      .class);
        assertNoConverterForTarget  (Cloneable   .class);
        assertIdentityForTarget     (Comparable  .class);
        assertIdentityForTarget     (Serializable.class);
        assertFallbackEquals        (Number      .class,
                "Number    ← String\n" +
                "  ├─Short ← String\n" +
                "  └─Long  ← String\n");
        assertMultilinesEquals("After StringConverter.Long",
            "ConverterRegistry\n" +
            "  ├─Short         ← String\n" +
            "  ├─Object        ← String\n" +
            "  ├─Comparable    ← String\n" +
            "  ├─Serializable  ← String\n" +
            "  ├─Long          ← String\n" +
            "  └─Number        ← String\n" + // The FallbackConverter, which replaced the previous.
            "      ├─Short     ← String\n" +
            "      └─Long      ← String\n", registry.toString());
        /*
         * Adds String ← Boolean
         * Expected side-effect: none since Boolean is not a Number
         */
        assertAllConvertersAreRegistered();
        assertNoConverterForTarget(Boolean.class);
        register(new StringConverter.Boolean());
        assertSameConverterForTarget(Boolean     .class);
        assertIdentityForTarget     (Object      .class);
        assertNoConverterForTarget  (Cloneable   .class);
        assertIdentityForTarget     (Comparable  .class);
        assertIdentityForTarget     (Serializable.class);
        assertMultilinesEquals("After StringConverter.Boolean",
            "ConverterRegistry\n" +
            "  ├─Short         ← String\n" +
            "  ├─Object        ← String\n" +
            "  ├─Comparable    ← String\n" +
            "  ├─Serializable  ← String\n" +
            "  ├─Long          ← String\n" +
            "  ├─Number        ← String\n" +
            "  │   ├─Short     ← String\n" +
            "  │   └─Long      ← String\n" +
            "  └─Boolean       ← String\n", registry.toString());
        /*
         * Adds String ← Number
         * Expected side-effect: replacement of the FallbackConverter
         */
        assertAllConvertersAreRegistered();
        register(new StringConverter.Number());
        assertSameConverterForTarget(Number      .class);
        assertIdentityForTarget     (Object      .class);
        assertNoConverterForTarget  (Cloneable   .class);
        assertIdentityForTarget     (Comparable  .class);
        assertIdentityForTarget     (Serializable.class);
        assertMultilinesEquals("After StringConverter.Number",
            "ConverterRegistry\n" +
            "  ├─Short         ← String\n" +
            "  ├─Object        ← String\n" +
            "  ├─Comparable    ← String\n" +
            "  ├─Serializable  ← String\n" +
            "  ├─Long          ← String\n" +
            "  ├─Boolean       ← String\n" +
            "  └─Number        ← String\n", registry.toString()); // Replaced the FallbackConverter.
        /*
         * Adds String ← Float
         * Expected side-effect: none
         */
        assertAllConvertersAreRegistered();
        assertNoConverterForTarget(Float.class);
        register(new StringConverter.Float());
        assertSameConverterForTarget(Float       .class);
        assertIdentityForTarget     (Object      .class);
        assertNoConverterForTarget  (Cloneable   .class);
        assertIdentityForTarget     (Comparable  .class);
        assertIdentityForTarget     (Serializable.class);
        assertMultilinesEquals("After StringConverter.Float",
            "ConverterRegistry\n" +
            "  ├─Short         ← String\n" +
            "  ├─Object        ← String\n" +
            "  ├─Comparable    ← String\n" +
            "  ├─Serializable  ← String\n" +
            "  ├─Long          ← String\n" +
            "  ├─Boolean       ← String\n" +
            "  ├─Number        ← String\n" +
            "  └─Float         ← String\n", registry.toString());
        /*
         * Final check.
         */
        assertAllConvertersAreRegistered();
    }

    /**
     * Tests registration of converters between {@link Number} and miscellaneous objects.
     * This method tests the addition of many converters because we want to observe
     * how {@linkplain #registry} grow in reaction to those additions.
     *
     * <p>This test compares the string representations for convenience.  In theory those string
     * representations are not committed API, so if the {@code FallbackConverter} implementation
     * change, it is okay to update this test accordingly.</p>
     */
    @Test
    @DependsOnMethod("testStringToMiscellaneous")
    public void testNumberToMiscellaneous() {
        assertAllConvertersAreRegistered();
        register(new StringConverter.Number().inverse());
        assertSameConverterForTarget(String      .class);
        assertIdentityForTarget     (Object      .class);
        assertNoConverterForTarget  (Cloneable   .class);
        assertNoConverterForTarget  (Comparable  .class);
        assertIdentityForTarget     (Serializable.class);
        assertSameConverterForTarget(CharSequence.class);
        assertMultilinesEquals("After ObjectToString.Number",
            "ConverterRegistry\n" +
            "  ├─String        ← Number\n" +
            "  ├─CharSequence  ← Number\n" +
            "  │   └─String    ← Number\n" +
            "  ├─Object        ← Number\n" +
            "  └─Serializable  ← Number\n", registry.toString());
        /*
         * Adds String ← Number
         * Expected side-effect: none
         */
        assertAllConvertersAreRegistered();
        register(new StringConverter.Number());
        assertSameConverterForTarget(Number.class);
        assertMultilinesEquals("After StringConverter.Number",
            "ConverterRegistry\n" +
            "  ├─String        ← Number\n" +
            "  ├─CharSequence  ← Number\n" +
            "  │   └─String    ← Number\n" +
            "  ├─Object        ← Number\n" +
            "  ├─Serializable  ← Number\n" +
            "  └─Number        ← String\n", registry.toString());
        /*
         * Adds Number ← Float
         * Expected side-effect: none
         */
        assertAllConvertersAreRegistered();
        register(new NumberConverter<Number,Float>(Number.class, Float.class));
        assertSameConverterForTarget(Float.class);
        assertMultilinesEquals("After NumberConverter.Float",
            "ConverterRegistry\n" +
            "  ├─String        ← Number\n" +
            "  ├─CharSequence  ← Number\n" +
            "  │   └─String    ← Number\n" +
            "  ├─Object        ← Number\n" +
            "  ├─Serializable  ← Number\n" +
            "  ├─Number        ← String\n" +
            "  └─Float         ← Number\n", registry.toString());
        /*
         * Final check.
         */
        assertAllConvertersAreRegistered();
    }

    /**
     * Tests automatic creation of a converter for an array of values.
     */
    @Test
    public void testArrayOfWrapperTypes() {
        register(new NumberConverter<Float,Double>(Float.class, Double.class));
        final ObjectConverter<?,?> converter = registry.find(Float[].class, Double[].class);
        assertInstanceOf("Array conversions", ArrayConverter.class, converter);
        assertEquals(Float [].class, converter.getSourceClass());
        assertEquals(Double[].class, converter.getTargetClass());
        assertSame("Converter shall be cached.", converter, registry.find(Float[].class, Double[].class));
    }

    /**
     * Tests automatic creation of a converter for an array of values.
     */
    @Test
    @DependsOnMethod("testArrayOfWrapperTypes")
    public void testArrayOfPrimitiveTypes() {
        register(new NumberConverter<Float,Double>(Float.class, Double.class));
        final ObjectConverter<?,?> converter = registry.find(float[].class, double[].class);
        assertInstanceOf("Array conversions", ArrayConverter.class, converter);
        assertEquals(float [].class, converter.getSourceClass());
        assertEquals(double[].class, converter.getTargetClass());
        assertSame("Converter shall be cached.", converter, registry.find(float[].class, double[].class));
    }
}
