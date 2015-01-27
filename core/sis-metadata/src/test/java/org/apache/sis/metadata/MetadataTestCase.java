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
package org.apache.sis.metadata;

import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.Collection;
import org.opengis.util.CodeList;
import org.opengis.metadata.identification.CharacterSet;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.test.AnnotationsTestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Base class for tests done on metadata objects using reflection. This base class tests JAXB annotations
 * as described in the {@linkplain AnnotationsTestCase parent class}, and tests additional aspects like:
 *
 * <ul>
 *   <li>All {@link AbstractMetadata} instance shall be initially {@linkplain AbstractMetadata#isEmpty() empty}.</li>
 *   <li>All getter methods shall returns a null singleton or an empty collection, never a null collection.</li>
 *   <li>After a call to a setter method, the getter method shall return a value equals to the given value.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(PropertyAccessorTest.class)
public abstract strictfp class MetadataTestCase extends AnnotationsTestCase {
    /**
     * The standard implemented by the metadata objects to test.
     */
    private final MetadataStandard standard;

    /**
     * Random generator, or {@code null} if not needed.
     */
    private Random random;

    /**
     * Creates a new test suite for the given types.
     *
     * @param standard The standard implemented by the metadata objects to test.
     * @param types The GeoAPI interfaces, {@link CodeList} or {@link Enum} types to test.
     */
    protected MetadataTestCase(final MetadataStandard standard, final Class<?>... types) {
        super(types);
        this.standard = standard;
    }

    /**
     * Returns the SIS implementation for the given GeoAPI interface.
     *
     * @return {@inheritDoc}
     */
    @Override
    protected <T> Class<? extends T> getImplementation(final Class<T> type) {
        assertTrue(type.getName(), standard.isMetadata(type));
        final Class<? extends T> impl = standard.getImplementation(type);
        assertNotNull(type.getName(), impl);
        return impl;
    }

    /**
     * Return {@code true} if the given property is expected to be writable.
     * If this method returns {@code false}, then {@link #testPropertyValues()}
     * will not try to write a value for that property.
     *
     * <p>The default implementation returns {@code true}.</p>
     *
     * @param  impl     The implementation class.
     * @param  property The name of the property to test.
     * @return {@code true} if the given property is writable.
     */
    protected boolean isWritable(final Class<?> impl, final String property) {
        return true;
    }

    /**
     * Returns a dummy value of the given type. The default implementation returns values for
     * {@link CharSequence}, {@link Number}, {@link Date}, {@link Locale}, {@link CodeList},
     * {@link Enum} and types in the {@link #types} list.
     *
     * <p>The returned value may be of an other type than the given one if the
     * {@code PropertyAccessor} converter method know how to convert that type.</p>
     *
     * @param  property The name of the property for which to create a value.
     * @param  type The type of value to create.
     * @return The value of the given {@code type}, or of a type convertible to the given type.
     */
    protected Object valueFor(final String property, final Class<?> type) {
        if (CharSequence.class.isAssignableFrom(type)) {
            return "Dummy value for " + property + '.';
        }
        switch (Numbers.getEnumConstant(type)) {
            case Numbers.DOUBLE:  return         random.nextDouble() * 90;
            case Numbers.FLOAT:   return         random.nextFloat()  * 90f;
            case Numbers.LONG:    return (long)  random.nextInt(1000000) + 1;
            case Numbers.INTEGER: return         random.nextInt(  10000) + 1;
            case Numbers.SHORT:   return (short) random.nextInt(   1000) + 1;
            case Numbers.BYTE:    return (byte)  random.nextInt(    100) + 1;
            case Numbers.BOOLEAN: return         random.nextBoolean();
        }
        if (Date.class.isAssignableFrom(type)) {
            return new Date(random.nextInt() * 1000L);
        }
        if (CodeList.class.isAssignableFrom(type)) try {
            if (type == CharacterSet.class) {
                // DefaultMetadata convert CharacterSet into Charset,
                // but not all character sets are supported.
                return CharacterSet.ISO_8859_1;
            }
            if (type == CodeList.class) {
                return null;
            }
            final CodeList[] codes = (CodeList[]) type.getMethod("values", (Class[]) null).invoke(null, (Object[]) null);
            return codes[random.nextInt(codes.length)];
        } catch (Exception e) { // (ReflectiveOperationException) on JDK7 branch.
            fail(e.toString());
        }
        if (Locale.class.isAssignableFrom(type)) {
            switch (random.nextInt(4)) {
                case 0: return Locale.ROOT;
                case 1: return Locale.ENGLISH;
                case 2: return Locale.FRENCH;
                case 3: return Locale.JAPANESE;
            }
        }
        if (ArraysExt.containsIdentity(types, type)) {
            final Class<?> impl = getImplementation(type);
            if (impl != null) try {
                return impl.getConstructor((Class<?>[]) null).newInstance((Object[]) null);
            } catch (Exception e) { // (ReflectiveOperationException) on JDK7 branch.
                fail(e.toString());
            }
        }
        return null;
    }

    /**
     * Normalizes the type of the given value before comparison. In particular, {@code InternationalString}
     * instances are converted to {@code String} instances.
     */
    private static Object normalizeType(Object value) {
        if (value instanceof CharSequence) {
            value = value.toString();
        }
        return value;
    }

    /**
     * Validates the given newly constructed metadata. The default implementation ensures that
     * {@link AbstractMetadata#isEmpty()} returns {@code true}.
     *
     * @param metadata The metadata to validate.
     */
    protected void validate(final AbstractMetadata metadata) {
        assertTrue("AbstractMetadata.isEmpty()", metadata.isEmpty());
    }

    /**
     * For every properties in every non-{@code Codelist} types listed in the {@link #types} array,
     * test the property values. This method performs the tests documented in class javadoc.
     */
    @Test
    public void testPropertyValues() {
        random = TestUtilities.createRandomNumberGenerator();
        for (final Class<?> type : types) {
            if (!CodeList.class.isAssignableFrom(type)) {
                final Class<?> impl = getImplementation(type);
                if (impl != null) {
                    assertTrue(type.isAssignableFrom(impl));
                    testPropertyValues(new PropertyAccessor(standard.getCitation(), type, impl));
                }
            }
        }
        done();
    }

    /**
     * Implementation of {@link #testPropertyValues()} for a single class.
     */
    private void testPropertyValues(final PropertyAccessor accessor) {
        /*
         * Try to instantiate the implementation. Every implementation should
         * have a no-args constructor, and their instantiation should never fail.
         */
        testingClass = accessor.implementation.getCanonicalName();
        final Object instance;
        try {
            instance = accessor.implementation.getConstructor((Class<?>[]) null).newInstance((Object[]) null);
        } catch (Exception e) { // (ReflectiveOperationException) on JDK7 branch.
            fail(e.toString());
            return;
        }
        if (instance instanceof AbstractMetadata) {
            validate((AbstractMetadata) instance);
        }
        /*
         * Iterate over all properties defined in the interface,
         * and checks for the existences of a setter method.
         */
        final int count = accessor.count();
        for (int i=0; i<count; i++) {
            testingMethod = accessor.name(i, KeyNamePolicy.METHOD_NAME);
            if (skipTest(accessor.implementation, testingMethod)) {
                continue;
            }
            final String property = accessor.name(i, KeyNamePolicy.JAVABEANS_PROPERTY);
            assertNotNull("Missing method name.", testingMethod);
            assertNotNull("Missing property name.", property);
            assertEquals("Wrong property index.", i, accessor.indexOf(property, true));
            /*
             * Get the property type. In the special case where the property type
             * is a collection, this is the type of elements in that collection.
             */
            final Class<?> propertyType = Numbers.primitiveToWrapper(accessor.type(i, TypeValuePolicy.PROPERTY_TYPE));
            final Class<?>  elementType = Numbers.primitiveToWrapper(accessor.type(i, TypeValuePolicy.ELEMENT_TYPE));
            assertNotNull(testingMethod, propertyType);
            assertNotNull(testingMethod, elementType);
            final boolean isCollection = Collection.class.isAssignableFrom(propertyType);
            assertFalse("Element type can not be Collection.", Collection.class.isAssignableFrom(elementType));
            assertEquals("Property and element types shall be the same if and only if not a collection.",
                         !isCollection, propertyType == elementType);
            /*
             * Try to get a value.
             */
            Object value = accessor.get(i, instance);
            if (value == null) {
                assertFalse("Null values are not allowed to be collections.", isCollection);
            } else {
                assertInstanceOf("Wrong property type.", propertyType, value);
                if (value instanceof CheckedContainer<?>) {
                    assertTrue("Wrong element type in collection.",
                            elementType.isAssignableFrom(((CheckedContainer<?>) value).getElementType()));
                }
                if (isCollection) {
                    assertTrue("Collections shall be initially empty.", ((Collection<?>) value).isEmpty());
                    value = CollectionsExt.modifiableCopy((Collection<?>) value); // Protect from changes.
                }
            }
            /*
             * Try to write a value.
             */
            final boolean isWritable = isWritable(accessor.implementation, property);
            if (isWritable != accessor.isWritable(i)) {
                fail("Non writable property: " + accessor + '.' + property);
            }
            if (isWritable) {
                final Object newValue = valueFor(property, elementType);
                final Object oldValue = accessor.set(i, instance, newValue, PropertyAccessor.RETURN_PREVIOUS);
                assertEquals("PropertyAccessor.set(…) shall return the value previously returned by get(…).", value, oldValue);
                value = accessor.get(i, instance);
                if (isCollection) {
                    if (newValue == null) {
                        assertTrue("We didn't generated a random value for this type, consequently the "
                                + "collection should still empty.", ((Collection<?>) value).isEmpty());
                        value = null;
                    } else {
                        value = TestUtilities.getSingleton((Collection<?>) value);
                    }
                }
                assertEquals("PropertyAccessor.get(…) shall return the value that we have just set.",
                        normalizeType(newValue), normalizeType(value));
            }
        }
    }

    /**
     * Returns {@code true} if test for the given property should be skipped.
     * Reasons for skipping a test are:
     *
     * <ul>
     *   <li>Class which is a union (those classes behave differently than non-union classes).</li>
     *   <li>Method which is the delegate of many legacy ISO 19115:2003 methods.
     *       Having a property that can be modified by many other properties confuse the tests.</li>
     * </ul>
     */
    @SuppressWarnings("deprecation")
    private static boolean skipTest(final Class<?> implementation, final String method) {
        return implementation == org.apache.sis.metadata.iso.maintenance.DefaultScopeDescription.class ||
              (implementation == org.apache.sis.metadata.iso.DefaultMetadata.class &&
               method.equals("getLocales")) || // Fail when 'locale' value equals 'language'.
              (implementation == org.apache.sis.metadata.iso.DefaultMetadata.class &&
               method.equals("getDataSetUri")) ||
              (implementation == org.apache.sis.metadata.iso.citation.DefaultContact.class &&
               method.equals("getPhone")) || // Deprecated method replaced by 'getPhones()'.
              (implementation == org.apache.sis.metadata.iso.lineage.DefaultSource.class &&
               method.equals("getScaleDenominator")) || // Deprecated method replaced by 'getSourceSpatialResolution()'.
              (implementation == org.apache.sis.metadata.iso.citation.DefaultResponsibleParty.class &&
               method.equals("getParties"));
    }
}
