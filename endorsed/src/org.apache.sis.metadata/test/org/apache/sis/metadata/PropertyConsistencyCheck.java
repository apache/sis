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
import java.util.Map;
import java.lang.reflect.Method;
import org.opengis.util.CodeList;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.metadata.internal.Dependencies;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.xml.test.AnnotationConsistencyCheck;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.ControlledVocabulary;


/**
 * Base class for tests done on metadata objects using reflection. This base class tests JAXB annotations
 * as described in the {@link AnnotationConsistencyCheck parent class}, and tests additional aspects like:
 *
 * <ul>
 *   <li>All {@link AbstractMetadata} instance shall be initially {@linkplain AbstractMetadata#isEmpty() empty}.</li>
 *   <li>All getter methods shall returns a null singleton or an empty collection, never a null collection.</li>
 *   <li>After a call to a setter method, the getter method shall return a value equals to the given value.</li>
 * </ul>
 *
 * This base class is defined in this {@code org.apache.sis.metadata} package because it needs to access
 * package-private classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class PropertyConsistencyCheck extends AnnotationConsistencyCheck {
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
     * @param  standard  the standard implemented by the metadata objects to test.
     * @param  types     the GeoAPI interfaces, {@link CodeList} or {@link Enum} types to test.
     */
    protected PropertyConsistencyCheck(final MetadataStandard standard, final Class<?>... types) {
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
        assertTrue(standard.isMetadata(type), type.getName());
        final Class<? extends T> impl = standard.getImplementation(type);
        assertNotNull(impl, type.getName());
        return impl;
    }

    /**
     * Return {@code true} if the given property is expected to be writable.
     * If this method returns {@code false}, then {@link #testPropertyValues()}
     * will not try to write a value for that property.
     *
     * <p>The default implementation returns {@code true}.</p>
     *
     * @param  impl      the implementation class.
     * @param  property  the name of the property to test.
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
     * <p>The returned value may be of another type than the given one if the
     * {@code PropertyAccessor} converter method know how to convert that type.</p>
     *
     * @param  property  the name of the property for which to create a value.
     * @param  type      the type of value to create.
     * @return the value of the given {@code type}, or of a type convertible to the given type.
     */
    protected Object sampleValueFor(final String property, final Class<?> type) {
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
        if (ControlledVocabulary.class.isAssignableFrom(type)) try {
            if (type == CodeList.class) {
                return null;
            }
            final ControlledVocabulary[] codes = (ControlledVocabulary[]) type.getMethod("values", (Class[]) null).invoke(null, (Object[]) null);
            return codes[random.nextInt(codes.length)];
        } catch (ReflectiveOperationException e) {
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
            } catch (ReflectiveOperationException e) {
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
     * @param  metadata  the metadata to validate.
     */
    protected void validate(final AbstractMetadata metadata) {
        assertTrue(metadata.isEmpty(), "AbstractMetadata.isEmpty()");
    }

    /**
     * For every properties in every non-{@code Codelist} types listed in the {@link #types} array,
     * tests the property values. This method verifies that all {@link AbstractMetadata} instances
     * are initially {@linkplain AbstractMetadata#isEmpty() empty}, all getter methods return a null
     * singleton or an empty collection, and tests setting a random value.
     */
    @Test
    public void testPropertyValues() {
        random = TestUtilities.createRandomNumberGenerator();
        for (final Class<?> type : types) {
            if (!ControlledVocabulary.class.isAssignableFrom(type)) {
                final Class<?> impl = getImplementation(type);
                if (impl != null) {
                    assertTrue(type.isAssignableFrom(impl), "Not an implementation of expected interface.");
                    testPropertyValues(new PropertyAccessor(type, impl, impl));
                }
            }
        }
    }

    /**
     * Implementation of {@link #testPropertyValues()} for a single class.
     */
    private void testPropertyValues(final PropertyAccessor accessor) {
        /*
         * Try to instantiate the implementation. Every implementation should
         * have a no-args constructor, and their instantiation should never fail.
         */
        testingMethod = null;
        testingClass = accessor.implementation.getCanonicalName();
        final Object instance;
        try {
            instance = accessor.implementation.getConstructor((Class<?>[]) null).newInstance((Object[]) null);
        } catch (ReflectiveOperationException e) {
            fail(e.toString());
            return;
        }
        if (instance instanceof AbstractMetadata md) {
            validate(md);
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
            assertNotNull(testingMethod, "Missing method name.");
            assertNotNull(property, "Missing property name.");
            assertEquals(i, accessor.indexOf(property, true), "Wrong property index.");
            /*
             * Get the property type. In the special case where the property type
             * is a collection, this is the type of elements in that collection.
             */
            final Class<?> propertyType = Numbers.primitiveToWrapper(accessor.type(i, TypeValuePolicy.PROPERTY_TYPE));
            final Class<?>  elementType = Numbers.primitiveToWrapper(accessor.type(i, TypeValuePolicy.ELEMENT_TYPE));
            assertNotNull(propertyType, testingMethod);
            assertNotNull(elementType, testingMethod);
            final boolean isMap        =        Map.class.isAssignableFrom(propertyType);
            final boolean isCollection = Collection.class.isAssignableFrom(propertyType);
            assertFalse(Collection.class.isAssignableFrom(elementType), "Element type cannot be Collection.");
            assertEquals(!(isMap | isCollection), propertyType == elementType,
                    "Property and element types shall be the same if and only if not a collection.");
            /*
             * Try to get a value.
             */
            Object value = accessor.get(i, instance);
            if (value == null) {
                assertFalse(isMap | isCollection, "Null values are not allowed to be collections.");
            } else {
                assertTrue(propertyType.isInstance(value), "Wrong property type.");
                if (value instanceof CheckedContainer<?> c) {
                    assertTrue(elementType.isAssignableFrom(c.getElementType()), "Wrong element type in collection.");
                }
                if (isMap) {
                    assertTrue(((Map<?,?>) value).isEmpty(), "Collections shall be initially empty.");
                    value = CollectionsExt.modifiableCopy((Map<?,?>) value);                          // Protect from changes.
                } else if (isCollection) {
                    assertTrue(((Collection<?>) value).isEmpty(), "Collections shall be initially empty.");
                    value = CollectionsExt.modifiableCopy((Collection<?>) value);                     // Protect from changes.
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
                if (isMap) {
                    continue;
                }
                final Object newValue = sampleValueFor(property, elementType);
                final Object oldValue = accessor.set(i, instance, newValue, PropertyAccessor.RETURN_PREVIOUS);
                assertEquals(value, oldValue, "PropertyAccessor.set(…) shall return the value previously returned by get(…).");
                value = accessor.get(i, instance);
                if (isCollection) {
                    if (newValue == null) {
                        assertTrue(((Collection<?>) value).isEmpty(), "We did not generated a random value"
                                + " for this type, consequently the collection should still empty.");
                        value = null;
                    } else {
                        value = TestUtilities.getSingleton((Collection<?>) value);
                    }
                }
                assertEquals(normalizeType(newValue), normalizeType(value),
                        "PropertyAccessor.get(…) shall return the value that we have just set.");
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
              (implementation == org.apache.sis.metadata.iso.citation.DefaultResponsibleParty.class &&
               method.equals("getParties"));
    }

    /**
     * Verifies the {@link TitleProperty} annotations. This method verifies that the property exist,
     * is a singleton, and is not another metadata object. The property should also be mandatory,
     * but this method does not verify that restriction since there is some exceptions.
     */
    @Test
    public void testTitlePropertyAnnotation() {
        for (final Class<?> type : types) {
            final Class<?> impl = standard.getImplementation(type);
            if (impl != null) {
                final TitleProperty an = impl.getAnnotation(TitleProperty.class);
                if (an != null) {
                    final String name = an.name();
                    final String message = impl.getSimpleName() + '.' + name;
                    final PropertyAccessor accessor = new PropertyAccessor(type, impl, impl);

                    // Property shall exist.
                    final int index = accessor.indexOf(name, false);
                    assertTrue(index >= 0, message);

                    // Property cannot be a metadata.
                    final Class<?> elementType = accessor.type(index, TypeValuePolicy.ELEMENT_TYPE);
                    assertFalse(standard.isMetadata(elementType), message);

                    // Property shall be a singleton.
                    assertSame(elementType, accessor.type(index, TypeValuePolicy.PROPERTY_TYPE), message);
                }
            }
        }
    }

    /**
     * Verifies the {@link Dependencies} annotations. This method verifies that the annotation is applied on
     * deprecated getter methods, that the referenced properties exist and the getter methods of referenced
     * properties are not deprecated.
     *
     * @throws NoSuchMethodException if {@link PropertyAccessor} references a non-existent method (would be a bug).
     */
    @Test
    public void testDependenciesAnnotation() throws NoSuchMethodException {
        for (final Class<?> type : types) {
            final Class<?> impl = standard.getImplementation(type);
            if (impl != null) {
                Map<String,String> names = null;
                for (final Method method : impl.getDeclaredMethods()) {
                    final Dependencies dep = method.getAnnotation(Dependencies.class);
                    if (dep != null) {
                        final String name = method.getName();
                        if (names == null) {
                            names = standard.asNameMap(type, KeyNamePolicy.JAVABEANS_PROPERTY, KeyNamePolicy.METHOD_NAME);
                        }
                        /*
                         * Currently, @Dependencies is applied only on deprecated getter methods.
                         * However, this policy may change in future Apache SIS versions.
                         */
                        assertTrue(name.startsWith("get"), name);
                        assertTrue(method.isAnnotationPresent(Deprecated.class), name);
                        /*
                         * All dependencies shall be non-deprecated methods. Combined with above
                         * restriction about @Dependencies applied only on deprected methods, this
                         * ensure that there is no cycle.
                         */
                        for (final String ref : dep.value()) {
                            // Verify that the dependency is a property name.
                            assertEquals(names.get(ref), ref, name);

                            // Verify that the referenced method is non-deprecated.
                            assertFalse(impl.getMethod(names.get(ref)).isAnnotationPresent(Deprecated.class), name);
                        }
                    }
                }
            }
        }
    }
}
