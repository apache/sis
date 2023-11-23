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
import java.util.stream.Collectors;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.internal.CollectionsExt;

// Test dependencies
import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.AbstractOperation;


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
    private final AbstractFeature expectedInstance;

    /**
     * The expected feature type.
     */
    private final DefaultFeatureType expectedType;

    /**
     * The actual feature, or {@code null} if comparing only feature type.
     */
    private final AbstractFeature actualInstance;

    /**
     * The actual feature type.
     */
    private final DefaultFeatureType actualType;

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
     * @see AbstractIdentifiedType#getDefinition()
     */
    public boolean ignoreDefinition;

    /**
     * Configuration option for ignoring the <em>designation</em> metadata associated
     * to feature types and property types. The default value is {@code false}.
     *
     * @see #compare()
     * @see AbstractIdentifiedType#getDesignation()
     */
    public boolean ignoreDesignation;

    /**
     * Configuration option for ignoring the <em>description</em> metadata associated
     * to feature types and property types. The default value is {@code false}.
     *
     * @see #compare()
     * @see AbstractIdentifiedType#getDescription()
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
    public FeatureComparator(final AbstractFeature expected, final AbstractFeature actual) {
        ArgumentChecks.ensureNonNull("expected", expected);
        ArgumentChecks.ensureNonNull("actual", actual);
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
    public FeatureComparator(final DefaultFeatureType expected, final DefaultFeatureType actual) {
        ArgumentChecks.ensureNonNull("expected", expected);
        ArgumentChecks.ensureNonNull("actual",   actual);
        expectedInstance = null;
        expectedType     = expected;
        actualInstance   = null;
        actualType       = actual;
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
    private void compareType(final AbstractIdentifiedType expected, final AbstractIdentifiedType actual) {
        boolean recognized = false;
        if (expected instanceof DefaultFeatureType) {
            assertInstanceOf(path(), DefaultFeatureType.class, actual);
            compareFeatureType((DefaultFeatureType) expected, (DefaultFeatureType) actual);
            recognized = true;
        }
        if (expected instanceof AbstractIdentifiedType) {
            assertInstanceOf(path(), AbstractIdentifiedType.class, actual);
            comparePropertyType(expected, actual);
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
    private void compareFeatureType(final DefaultFeatureType expected, final DefaultFeatureType actual) {
        compareIdentifiedType(expected, actual);

        // TODO: put messages in lambda functionw with JUnit 5.
        assertEquals(path() + "Abstract state differ", expected.isAbstract(), actual.isAbstract());
        assertEquals(path() + "Super types differ", expected.getSuperTypes(), actual.getSuperTypes());
        /*
         * Compare all properties that are not ignored.
         * Properties are removed from the `actualProperties` list as we found them.
         */
        final List<AbstractIdentifiedType> actualProperties = new ArrayList<>(actual.getProperties(false));
        actualProperties.removeIf(this::isIgnored);
        for (final AbstractIdentifiedType pte : expected.getProperties(false)) {
            if (!isIgnored(pte)) {
                final String tip = push(pte.getName().toString());
                AbstractIdentifiedType pta = findAndRemove(actualProperties, pte.getName());
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
            for (final AbstractIdentifiedType pta : actualProperties) {
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
    private void compareFeature(final AbstractFeature expected, final AbstractFeature actual) {
        compareFeatureType(expected.getType(), actual.getType());
        for (final AbstractIdentifiedType p : expected.getType().getProperties(true)) {
            if (isIgnored(p)) {
                continue;
            }
            final String tip = push(p.getName().toString());
            final Collection<?> expectedValues = asCollection(expected.getPropertyValue(tip));
            final Collection<?> actualValues   = asCollection(actual.getPropertyValue(tip));
            assertEquals(path() + "Number of values differ", expectedValues.size(), actualValues.size());
            final Iterator<?> expectedIter = expectedValues.iterator();
            final Iterator<?> actualIter = actualValues.iterator();
            while (expectedIter.hasNext()) {
                final Object expectedElement = expectedIter.next();
                final Object actualElement = actualIter.next();
                if (expectedElement instanceof AbstractFeature) {
                    compareFeature((AbstractFeature) expectedElement, (AbstractFeature) actualElement);
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
    private void comparePropertyType(final AbstractIdentifiedType expected, final AbstractIdentifiedType actual) {
        if (expected instanceof DefaultAttributeType) {
            assertInstanceOf(path(), DefaultAttributeType.class, actual);
            compareAttribute((DefaultAttributeType) expected, (DefaultAttributeType) actual);
        }
        if (expected instanceof DefaultAssociationRole) {
            assertInstanceOf(path(), DefaultAssociationRole.class, actual);
            compareFeatureAssociationRole((DefaultAssociationRole) expected, (DefaultAssociationRole) actual);
        }
        if (expected instanceof AbstractOperation) {
            assertInstanceOf(path(), AbstractOperation.class, actual);
            compareOperation((AbstractOperation) expected, (AbstractOperation) actual);
        }
    }

    /**
     * Compares two attribute types.
     *
     * @param  expected  the expected property.
     * @param  actual    the actual property.
     * @throws AssertionError if the actual property is not equal to the expected property.
     */
    private void compareAttribute(final DefaultAttributeType<?> expected, final DefaultAttributeType<?> actual) {
        compareIdentifiedType(expected, actual);
        assertEquals(path() + "Value classe differ",  expected.getValueClass(),   expected.getValueClass());
        assertEquals(path() + "Default value differ", expected.getDefaultValue(), expected.getDefaultValue());

        final Map<String, DefaultAttributeType<?>> expectedChrs = expected.characteristics();
        final Map<String, DefaultAttributeType<?>> actualChrs = actual.characteristics();
        final List<String> actualChrNames = new ArrayList<>(actualChrs.keySet());
        actualChrNames.removeIf((p) -> ignoredCharacteristics.contains(p));

        for (final Map.Entry<String, DefaultAttributeType<?>> entry : expectedChrs.entrySet()) {
            final String p = entry.getKey();
            if (!ignoredCharacteristics.contains(p)) {
                final DefaultAttributeType<?> expectedChr = entry.getValue();
                final DefaultAttributeType<?> actualChr = actualChrs.get(p);
                final String tip = push("characteristic(" + p + ')');
                assertNotNull(path(), actualChr);
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
    private void compareFeatureAssociationRole(final DefaultAssociationRole expected, final DefaultAssociationRole actual) {
        compareIdentifiedType(expected, actual);
        assertEquals(path() + "Minimum occurences differ", expected.getMinimumOccurs(), actual.getMinimumOccurs());
        assertEquals(path() + "Maximum occurences differ", expected.getMaximumOccurs(), actual.getMaximumOccurs());
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
    private void compareOperation(final AbstractOperation expected, final AbstractOperation actual) {
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
    private void compareIdentifiedType(final AbstractIdentifiedType expected, final AbstractIdentifiedType actual) {
        assertEquals(path() + "Name differ", expected.getName(), actual.getName());
        if (!ignoreDefinition) {
            assertEquals(path() + "Definition differ", expected.getDefinition(), actual.getDefinition());
        }
        if (!ignoreDesignation) {
            assertEquals(path() + "Designation differ", expected.getDesignation(), actual.getDesignation());
        }
        if (!ignoreDescription) {
            assertEquals(path() + "Description differ", expected.getDescription(), actual.getDescription());
        }
        if (expected instanceof Deprecable && actual instanceof Deprecable) {
            assertEquals(path() + "Deprecated state differ",
                    ((Deprecable) expected).isDeprecated(),
                    ((Deprecable) actual).isDeprecated());
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
    private boolean isIgnored(final AbstractIdentifiedType property) {
        return ignoredProperties.contains(property.getName().toString());
    }

    /**
     * Searches for a property of the given name in the given list and remove the property from that list.
     * If the property is not found, then this method fails.
     */
    private AbstractIdentifiedType findAndRemove(final Collection<AbstractIdentifiedType> properties, final GenericName name) {
        final Iterator<AbstractIdentifiedType> it = properties.iterator();
        while (it.hasNext()) {
            final AbstractIdentifiedType pt = it.next();
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
        if (value instanceof Collection<?>) {
            return (Collection<?>) value;
        } else {
            return CollectionsExt.singletonOrEmpty(value);
        }
    }
}
