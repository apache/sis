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
package org.apache.sis.feature;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.sis.util.collection.CheckedContainer;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;


/**
 * Tests some default method implementations provided in {@link AbstractFeature}.
 * This test also has the side-effect of testing {@link Validator} with attribute
 * instances that are not {@link AbstractAttribute}.
 *
 * <p>This class inherits all tests defined in {@link FeatureTestCase}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class AbstractFeatureTest extends FeatureTestCase {
    /**
     * Creates a new test case.
     */
    public AbstractFeatureTest() {
    }

    /**
     * A feature implementation on top of {@link AbstractFeature}. This class has more code than strictly necessary
     * since we need to reproduce some of the verifications performed by the Apache SIS supported implementations
     * in order to get the tests to pass.
     */
    @SuppressWarnings("serial")
    private static final class CustomFeature extends AbstractFeature implements Cloneable {
        /**
         * All property values. For this test we use a {@code java.util.Map}. However, users who want to provide
         * their own {@link AbstractFeature} implementation should consider to use plain Java fields instead.
         * If a feature backed by a {@code java.util.Map} is really wanted, then {@link SparseFeature} should
         * be considered.
         */
        private HashMap<String, Object> values = new HashMap<>();

        /**
         * Creates a new feature of the given type. This constructor adds immediately the default values into
         * the {@link #values} map and a modifiable list (even if empty) for all properties that are collections.
         */
        CustomFeature(final DefaultFeatureType type) {
            super(type);
            for (final PropertyType property : type.getProperties(true)) {
                if (property instanceof AttributeType<?> attribute) {
                    Object value = attribute.getDefaultValue();
                    if (isMultiValued(property)) {
                        value = new ArrayList<>(PropertyView.singletonOrEmpty(value));
                    }
                    if (value != null) {
                        values.put(property.getName().toString(), value);
                    }
                }
            }
        }

        /**
         * Returns {@code true} if the given property can contains more than one value.
         */
        private static boolean isMultiValued(final PropertyType pt) {
            return (pt instanceof AttributeType<?>) && (((AttributeType<?>) pt).getMaximumOccurs() > 1);
        }

        /**
         * Returns the value for the property of the given name, or {@code null} if none.
         * This method does not verify if it should return the default value or a collection.
         * In this simple implementation, those verifications are done at construction time.
         *
         * <p>A robust implementation would verify that the given name is legal.
         * For this test, be skip that verification.</p>
         */
        @Override
        public Object getPropertyValue(final String name) {
            return values.get(name);
        }

        /**
         * Sets the value for the property of the given name. In order to allow the tests to pass,
         * we need to reproduce in this method some of the verifications performed by the
         * {@link SingletonAttribute} and {@link MultiValuedAttribute} implementations.
         */
        @Override
        public void setPropertyValue(final String name, Object value) {
            final var property = getType().getProperty(name);
            final boolean isMultiValued = isMultiValued(property);
            if (isMultiValued && !(value instanceof Collection<?>)) {
                value = new ArrayList<>(PropertyView.singletonOrEmpty(value));
            }
            if (value != null) {
                final Class<?> base;
                if (property instanceof AttributeType<?> attribute) {
                    base = attribute.getValueClass();
                } else {
                    base = FeatureType.class;
                }
                for (final Object element : (isMultiValued ? (Iterable<?>) value : PropertyView.singletonOrEmpty(value))) {
                    if (!base.isInstance(element)) {
                        throw new ClassCastException("Cannot cast " + value.getClass() + " to " + base + " in " + name + '.');
                    }
                }
            }
            values.put(name, value);
        }

        /**
         * Returns a close of this feature.
         */
        @Override
        @SuppressWarnings("unchecked")
        public CustomFeature clone() {
            final CustomFeature c;
            try {
                c = (CustomFeature) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
            c.values = (HashMap<String, Object>) c.values.clone();
            return c;
        }
    }

    /**
     * Relax the requirements about {@link AbstractFeature#getProperty(String)} to return the exact same instance.
     */
    @Override
    boolean assertSameProperty(final String name, final Property expected, final boolean modified) {
        final var actual = feature.getProperty(name);
        if ((expected instanceof PropertyView) == (actual instanceof PropertyView)) {
            assertEquals(expected, actual, name);
        }
        assertSame(expected.getName(), actual.getName(), "name");
        if (!modified) {
            assertSame(expected.getValue(), actual.getValue(), "value");
        }
        return actual == expected;
    }

    /**
     * Creates a new feature for the given type.
     */
    @Override
    final AbstractFeature createFeature(final DefaultFeatureType type) {
        return new CustomFeature(type);
    }

    /**
     * Clones the {@link #feature} instance.
     */
    @Override
    final AbstractFeature cloneFeature() throws CloneNotSupportedException {
        return ((CustomFeature) feature).clone();
    }

    // Inherit all tests from the super-class.

    /**
     * Tests {@link AbstractFeature#castOrCopyAsCheckedList(Collection, Class)}.
     */
    @Test
    public void testCastOrCopyAsCheckedList() {
        final var fruits = List.of("Apple", "Orange", "Raisin");
        final var asStrings = AbstractFeature.castOrCopyAsCheckedList(fruits, String.class);
        assertEquals (String.class, assertInstanceOf(CheckedContainer.class, asStrings).getElementType());
        assertNotSame(fruits, asStrings);       // Should have created a new instance.
        assertEquals (fruits, asStrings);       // Should contain the same data.
        assertSame   (asStrings, AbstractFeature.castOrCopyAsCheckedList(asStrings, String.class));

        final var asChars = AbstractFeature.castOrCopyAsCheckedList(asStrings, CharSequence.class);
        assertEquals (CharSequence.class, assertInstanceOf(CheckedContainer.class, asChars).getElementType());
        assertNotSame(asStrings, asChars);      // Should have created a new instance.
        assertEquals (asStrings, asChars);      // Should contain the same data.
        assertEquals (fruits,    asChars);      // Should contain the same data.

        var e = assertThrows(ClassCastException.class,
                () -> AbstractFeature.castOrCopyAsCheckedList(asChars, Integer.class),
                "Should not be allowed to cast String to Integer.");
        assertMessageContains(e, "String", "Integer");
    }
}
