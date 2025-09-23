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
package org.apache.sis.feature.test;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Deque;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opengis.util.GenericName;
import org.apache.sis.util.internal.shared.CollectionsExt;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.apache.sis.util.Deprecable;


/**
 * Comparator of feature instances and feature types.
 * Can be used in test suite for comparing an actual feature against its expected value.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class FeatureComparator {
    /**
     * The expected feature, or {@code null} if comparing only feature type.
     */
    private final Feature expectedInstance;

    /**
     * The expected feature type.
     */
    private final FeatureType expectedType;

    /**
     * The actual feature, or {@code null} if comparing only feature type.
     */
    private final Feature actualInstance;

    /**
     * The actual feature type.
     */
    private final FeatureType actualType;

    /**
     * The fully-qualified name of properties to ignore in comparisons.
     * This collection is initially empty.
     * Users can add or remove elements in this collection as they wish.
     *
     * <p>The elements shall be names in the form {@code "namespace:name"},
     * or only {@code "name"} if there is no namespace.</p>
     *
     * @see #compare()
     */
    public final Set<String> ignoredProperties = new HashSet<>();

    /**
     * The fully-qualified name of characteristics to ignore in comparisons.
     * This collection is initially empty.
     * Users can add or remove elements in this collection as they wish.
     *
     * <p>The elements shall be names in the form {@code "namespace:name"},
     * or only {@code "name"} if there is no namespace.</p>
     *
     * @see #compare()
     */
    public final Set<String> ignoredCharacteristics = new HashSet<>();

    /**
     * Configuration option for ignoring the <em>definition</em> metadata associated
     * to feature types and property types. The default value is {@code false}.
     *
     * @see #compare()
     * @see IdentifiedType#getDefinition()
     */
    public boolean ignoreDefinition;

    /**
     * Configuration option for ignoring the <em>designation</em> metadata associated
     * to feature types and property types. The default value is {@code false}.
     *
     * @see #compare()
     * @see IdentifiedType#getDesignation()
     */
    public boolean ignoreDesignation;

    /**
     * Configuration option for ignoring the <em>description</em> metadata associated
     * to feature types and property types. The default value is {@code false}.
     *
     * @see #compare()
     * @see IdentifiedType#getDescription()
     */
    public boolean ignoreDescription;

    /**
     * Path to the property being compared. Used in case of test failure.
     */
    private final Deque<String> path = new ArrayDeque<>();

    /**
     * Creates a new comparator for the given feature instances.
     *
     * @param  expected  the expected feature instance.
     * @param  actual    the actual feature instance.
     */
    public FeatureComparator(final Feature expected, final Feature actual) {
        expectedInstance = expected;
        expectedType     = expected.getType();
        actualInstance   = actual;
        actualType       = actual.getType();
    }

    /**
     * Creates a new comparator for the given feature types.
     *
     * @param  expected  the expected feature type.
     * @param  actual    the actual feature type.
     */
    public FeatureComparator(final FeatureType expected, final FeatureType actual) {
        expectedInstance = null;
        expectedType     = Objects.requireNonNull(expected);
        actualInstance   = null;
        actualType       = Objects.requireNonNull(actual);
    }

    /**
     * Compares the feature instances or feature types specified at construction time.
     * If there is any aspect to ignore during comparisons, then the {@link #ignoredProperties},
     * {@link #ignoredCharacteristics}, {@link #ignoreDefinition}, {@link #ignoreDesignation} or
     * {@link #ignoreDescription} flags should be set before to invoke this method.
     *
     * @throws AssertionError if the test fails.
     */
    public void compare() {
        if (expectedInstance != null) {
            compareFeature(expectedInstance, actualInstance);
        } else {
            compareFeatureType(expectedType, actualType);
        }
    }

    /**
     * Compares two feature types or two property types.
     *
     * @param  expected  the expected type.
     * @param  actual    the actual type.
     * @throws AssertionError if the actual type is not equal to the expected type.
     */
    private void compareType(final IdentifiedType expected, final IdentifiedType actual) {
        boolean recognized = false;
        if (expected instanceof FeatureType type) {
            var c = assertInstanceOf(FeatureType.class, actual, this::path);
            compareFeatureType(type, c);
            recognized = true;
        }
        if (expected instanceof PropertyType type) {
            var c = assertInstanceOf(PropertyType.class, actual, this::path);
            comparePropertyType(type, c);
            recognized = true;
        }
        if (!recognized) {
            fail(path() + "Unexpected type " + expected);
        }
    }

    /**
     * Compares two feature types. This comparison implies the comparisons of all non-ignored properties.
     *
     * @param  expected  the expected type.
     * @param  actual    the actual type.
     * @throws AssertionError if the actual type is not equal to the expected type.
     */
    private void compareFeatureType(final FeatureType expected, final FeatureType actual) {
        compareIdentifiedType(expected, actual);

        assertEquals(expected.isAbstract(),    actual.isAbstract(),    () -> path() + "Abstract state differ.");
        assertEquals(expected.getSuperTypes(), actual.getSuperTypes(), () -> path() + "Super types differ.");
        /*
         * Compare all properties that are not ignored.
         * Properties are removed from the `actualProperties` list as we found them.
         */
        final List<PropertyType> actualProperties = new ArrayList<>(actual.getProperties(false));
        actualProperties.removeIf(this::isIgnored);
        for (final PropertyType pte : expected.getProperties(false)) {
            if (!isIgnored(pte)) {
                final String tip = push(pte.getName().toString());
                PropertyType pta = findAndRemove(actualProperties, pte.getName());
                comparePropertyType(pte, pta);
                pull(tip);
            }
        }
        /*
         * Any remaining property in the `actualProperties` list is unexpected.
         */
        if (!actualProperties.isEmpty()) {
            final StringBuilder b = new StringBuilder(path())
                    .append("Actual type contains a property not declared in expected type:")
                    .append(System.lineSeparator());
            for (final PropertyType pta : actualProperties) {
                b.append("  ").append(pta.getName()).append(System.lineSeparator());
            }
            fail(b.toString());
        }
    }

    /**
     * Compares two feature instances. This comparison implies the comparison of feature types
     * and the comparisons of all non-ignored properties.
     *
     * @param  expected  the expected instance.
     * @param  actual    the actual instance.
     * @throws AssertionError if the actual instance is not equal to the expected instance.
     */
    private void compareFeature(final Feature expected, final Feature actual) {
        compareFeatureType(expected.getType(), actual.getType());
        for (final PropertyType p : expected.getType().getProperties(true)) {
            if (isIgnored(p)) {
                continue;
            }
            final String tip = push(p.getName().toString());
            final Collection<?> expectedValues = asCollection(expected.getPropertyValue(tip));
            final Collection<?> actualValues   = asCollection(actual.getPropertyValue(tip));
            assertEquals(expectedValues.size(), actualValues.size(), () -> path() + "Number of values differ.");
            final Iterator<?> expectedIter = expectedValues.iterator();
            final Iterator<?> actualIter = actualValues.iterator();
            while (expectedIter.hasNext()) {
                final Object expectedElement = expectedIter.next();
                final Object actualElement = actualIter.next();
                if (expectedElement instanceof Feature instance) {
                    compareFeature(instance, (Feature) actualElement);
                } else {
                    assertEquals(expectedElement, actualElement);
                }
            }
            pull(tip);
        }
    }

    /**
     * Compares two property types. This method dispatches the comparison to a more specialized method.
     *
     * @param  expected  the expected property.
     * @param  actual    the actual property.
     * @throws AssertionError if the actual property is not equal to the expected property.
     */
    private void comparePropertyType(final PropertyType expected, final PropertyType actual) {
        if (expected instanceof AttributeType<?> type) {
            var c = assertInstanceOf(AttributeType.class, actual, this::path);
            compareAttribute(type, c);
        }
        if (expected instanceof FeatureAssociationRole role) {
            var c = assertInstanceOf(FeatureAssociationRole.class, actual, this::path);
            compareFeatureAssociationRole(role, c);
        }
        if (expected instanceof Operation op) {
            var c = assertInstanceOf(Operation.class, actual, this::path);
            compareOperation(op, c);
        }
    }

    /**
     * Compares two attribute types.
     *
     * @param  expected  the expected property.
     * @param  actual    the actual property.
     * @throws AssertionError if the actual property is not equal to the expected property.
     */
    private void compareAttribute(final AttributeType<?> expected, final AttributeType<?> actual) {
        compareIdentifiedType(expected, actual);
        assertEquals(expected.getValueClass(),   expected.getValueClass(),   () -> path() + "Value classe differ.");
        assertEquals(expected.getDefaultValue(), expected.getDefaultValue(), () -> path() + "Default value differ.");

        final Map<String, AttributeType<?>> expectedChrs = expected.characteristics();
        final Map<String, AttributeType<?>> actualChrs = actual.characteristics();
        final List<String> actualChrNames = new ArrayList<>(actualChrs.keySet());
        actualChrNames.removeIf((p) -> ignoredCharacteristics.contains(p));

        for (final Map.Entry<String, AttributeType<?>> entry : expectedChrs.entrySet()) {
            final String p = entry.getKey();
            if (!ignoredCharacteristics.contains(p)) {
                final AttributeType<?> expectedChr = entry.getValue();
                final AttributeType<?> actualChr = actualChrs.get(p);
                final String tip = push("characteristic(" + p + ')');
                assertNotNull(actualChr, this::path);
                assertTrue(actualChrNames.remove(p));
                comparePropertyType(expectedChr, actualChr);
                pull(tip);
            }
        }
        /*
         * Any remaining characteristics in the `actualChrNames` list is unexpected.
         */
        if (!actualChrNames.isEmpty()) {
            final StringBuilder b = new StringBuilder(path())
                    .append("Result type contains a characteristic not declared in expected type:")
                    .append(System.lineSeparator());
            for (final String c : actualChrNames) {
                b.append("  ").append(c).append(System.lineSeparator());
            }
            fail(b.toString());
        }
    }

    /**
     * Compares two associations.
     *
     * @param  expected  the expected property.
     * @param  actual    the actual property.
     * @throws AssertionError if the actual property is not equal to the expected property.
     */
    private void compareFeatureAssociationRole(final FeatureAssociationRole expected, final FeatureAssociationRole actual) {
        compareIdentifiedType(expected, actual);
        assertEquals(expected.getMinimumOccurs(), actual.getMinimumOccurs(), () -> path() + "Minimum occurences differ.");
        assertEquals(expected.getMaximumOccurs(), actual.getMaximumOccurs(), () -> path() + "Maximum occurences differ.");
        final String tip = push("association-valuetype");
        compareFeatureType(expected.getValueType(), actual.getValueType());
        pull(tip);
    }

    /**
     * Compares two operations.
     *
     * @param  expected  the expected property.
     * @param  actual    the actual property.
     * @throws AssertionError if the actual property is not equal to the expected property.
     */
    private void compareOperation(final Operation expected, final Operation actual) {
        compareIdentifiedType(expected, actual);
        assertEquals(expected.getParameters(), actual.getParameters());
        final String tip = push("operation-actual(" + expected.getResult().getName() + ')');
        compareType(expected.getResult(), actual.getResult());
        pull(tip);
    }

    /**
     * Comparisons common to feature type, property type, association roles and operations.
     *
     * @param  expected  the expected type.
     * @param  actual    the actual type.
     * @throws AssertionError if the actual type is not equal to the expected type.
     */
    private void compareIdentifiedType(final IdentifiedType expected, final IdentifiedType actual) {
        assertEquals(expected.getName(), actual.getName(), () -> path() + "Name differ.");
        if (!ignoreDefinition) {
            assertEquals(expected.getDefinition(), actual.getDefinition(), () -> path() + "Definition differ.");
        }
        if (!ignoreDesignation) {
            assertEquals(expected.getDesignation(), actual.getDesignation(), () -> path() + "Designation differ.");
        }
        if (!ignoreDescription) {
            assertEquals(expected.getDescription(), actual.getDescription(), () -> path() + "Description differ.");
        }
        if (expected instanceof Deprecable de && actual instanceof Deprecable da) {
            assertEquals(de.isDeprecated(), da.isDeprecated(), () -> path() + "Deprecated state differ.");
        }
    }

    /**
     * Adds the given name to the path to the property being compared.
     * This is used for formatting error messages in case of error.
     */
    private String push(final String label) {
        path.addLast(label);
        return label;
    }

    /**
     * Removes the given name from the path to the property being compared.
     * This is the converse of {@link #push(String)} and shall be invoked
     * when the comparison of a property finished.
     */
    private void pull(final String tip) {
        assertSame(tip, path.removeLast());
    }

    /**
     * Returns a string representation of current {@link #path} value.
     */
    private String path() {
        return path.stream().collect(Collectors.joining(" > ", "[", "]: "));
    }

    /**
     * Returns {@code true} if the given property should be ignored in feature comparisons.
     */
    private boolean isIgnored(final PropertyType property) {
        return ignoredProperties.contains(property.getName().toString());
    }

    /**
     * Searches for a property of the given name in the given list and remove the property from that list.
     * If the property is not found, then this method fails.
     */
    private PropertyType findAndRemove(final Collection<PropertyType> properties, final GenericName name) {
        final Iterator<PropertyType> it = properties.iterator();
        while (it.hasNext()) {
            final PropertyType pt = it.next();
            if (pt.getName().equals(name)) {
                it.remove();
                return pt;
            }
        }
        fail(path() + "Property not found for name " + name);
        return null;
    }

    /**
     * Returns the given value as a collection. If the value is null, an empty collection is returned.
     * If the value is already a collection, then it is returned as-is. Otherwise the value is wrapped
     * in a singleton collection.
     */
    private static Collection<?> asCollection(final Object value) {
        if (value instanceof Collection<?> c) {
            return c;
        } else {
            return CollectionsExt.singletonOrEmpty(value);
        }
    }
}
