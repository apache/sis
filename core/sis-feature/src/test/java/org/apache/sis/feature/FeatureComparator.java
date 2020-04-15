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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Deprecable;
import org.junit.Assert;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;
import org.opengis.util.GenericName;

/**
 * Tool to compare feature and feature types.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public class FeatureComparator {

    private final Object obj1;
    private final Object obj2;

    /**
     * The fully-qualified name of properties to ignore in comparisons. This
     * collection is initially empty. Users can add or remove elements in this
     * collection as they wish.
     *
     * <p>
     * The elements shall be names in the form {@code "namespace:name"}, or only
     * {@code "name"} if there is no namespace.</p>
     */
    public final Set<String> ignoredProperties = new HashSet<>();
    /**
     * The fully-qualified name of characteristics to ignore in comparisons.
     * This collection is initially empty. Users can add or remove elements in
     * this collection as they wish.
     *
     * <p>
     * The elements shall be names in the form {@code "namespace:name"}, or only
     * {@code "name"} if there is no namespace.</p>
     */
    public final Set<String> ignoredCharacteristics = new HashSet<>();

    public boolean ignoreDefinition = false;
    public boolean ignoreDesignation = false;
    public boolean ignoreDescription = false;

    public FeatureComparator(Feature expected, Feature result) {
        ArgumentChecks.ensureNonNull("expected", expected);
        ArgumentChecks.ensureNonNull("result", result);
        this.obj1 = expected;
        this.obj2 = result;
    }

    public FeatureComparator(FeatureType expected, FeatureType result) {
        ArgumentChecks.ensureNonNull("expected", expected);
        ArgumentChecks.ensureNonNull("result", result);
        this.obj1 = expected;
        this.obj2 = result;
    }

    /**
     * Compare the features or feature types specified at construction time.
     */
    public void compare() {
        final Path path = new Path();
        if (obj1 instanceof FeatureType) {
            compareFeatureType(path, (FeatureType) obj1, (FeatureType) obj2);
        } else {
            compareFeature(path, (Feature) obj1, (Feature) obj2);
        }
    }

    private void compareType(Path path, IdentifiedType expected, IdentifiedType result) {
        if (expected instanceof FeatureType) {
            compareFeatureType(path, (FeatureType) expected, (FeatureType) result);
        } else if (expected instanceof PropertyType) {
            comparePropertyType(path, (PropertyType) expected, (PropertyType) result);
        } else {
            Assert.fail(msg(path, "Unexpected type " + expected));
        }
    }

    private void compareFeatureType(Path path, FeatureType expected, FeatureType result) {
        compareIdentifiedType(path, expected, result);

        Assert.assertEquals(msg(path, "Abstract state differ"), expected.isAbstract(), result.isAbstract());
        Assert.assertEquals(msg(path, "Super types differ"), expected.getSuperTypes(), result.getSuperTypes());

        List<? extends PropertyType> expectedProperties = new ArrayList<>(expected.getProperties(false));
        List<? extends PropertyType> resultProperties = new ArrayList<>(result.getProperties(false));

        while (!expectedProperties.isEmpty()) {
            final PropertyType pte = expectedProperties.remove(0);
            if (ignoredProperties.contains(pte.getName().toString())) {
                continue;
            }
            Path sub = path.sub(pte.getName().toString());
            PropertyType ptr = find(sub, resultProperties, pte.getName());
            resultProperties.remove(ptr);
            comparePropertyType(sub, pte, ptr);
        }
        while (!resultProperties.isEmpty()) {
            final PropertyType pte = resultProperties.remove(0);
            if (ignoredProperties.contains(pte.getName().toString())) {
                continue;
            }
            Path sub = path.sub(pte.getName().toString());
            Assert.fail(msg(sub, "Result type contains a property not declared in expected type : " + pte.getName()));
        }

    }

    private void compareFeature(Path path, Feature expected, Feature result) {
        compareFeatureType(path, expected.getType(), result.getType());

        Collection<? extends PropertyType> properties = expected.getType().getProperties(true);
        for (PropertyType pte : properties) {
            if (ignoredProperties.contains(pte.getName().toString())) {
                continue;
            }
            Path sub = path.sub(pte.getName().toString());

            Object expectedValue = expected.getPropertyValue(pte.getName().toString());
            Object resultValue = result.getPropertyValue(pte.getName().toString());
            if (!(expectedValue instanceof Collection)) {
                expectedValue = (expectedValue == null) ? Collections.EMPTY_LIST : Arrays.asList(expectedValue);
            }
            if (!(resultValue instanceof Collection)) {
                resultValue = (resultValue == null) ? Collections.EMPTY_LIST : Arrays.asList(resultValue);
            }

            Collection<?> expectedCol = (Collection) expectedValue;
            Collection<?> resultCol = (Collection) resultValue;

            if (expectedCol.size() != resultCol.size()) {
                Assert.fail(msg(sub, "Number of values differ, expected " + expectedCol.size() + " but was " + resultCol.size()));
            }

            Iterator<?> expectedIte = expectedCol.iterator();
            Iterator<?> resultIte = resultCol.iterator();

            while (expectedIte.hasNext()) {
                Object subExpVal = expectedIte.next();
                Object subResVal = resultIte.next();

                if (subExpVal instanceof Feature) {
                    compareFeature(path, (Feature) subExpVal, (Feature) subResVal);
                } else {
                    Assert.assertEquals(subExpVal, subResVal);
                }
            }
        }
    }

    private void comparePropertyType(Path path, PropertyType expected, PropertyType result) {

        if (expected instanceof AttributeType) {
            if (!(result instanceof AttributeType)) {
                Assert.fail(msg(path, "Expected an AttributeType for name " + ((AttributeType) expected).getName() + " but found a " + result));
            }
            compareAttribute(path, (AttributeType) expected, (AttributeType) result);

        } else if (expected instanceof FeatureAssociationRole) {
            if (!(result instanceof FeatureAssociationRole)) {
                Assert.fail(msg(path, "Expected a FeatureAssociationRole for name " + ((AttributeType) expected).getName() + " but found a " + result));
            }
            compareFeatureAssociationRole(path, (FeatureAssociationRole) expected, (FeatureAssociationRole) result);

        } else if (expected instanceof Operation) {
            if (!(result instanceof Operation)) {
                Assert.fail(msg(path, "Expected an Operation for name " + ((AttributeType) expected).getName() + " but found a " + result));
            }
            compareOperation(path, (Operation) expected, (Operation) result);
        }
    }

    private void compareAttribute(Path path, AttributeType expected, AttributeType result) {
        compareIdentifiedType(path, expected, result);
        Assert.assertEquals(msg(path, "Value classe differ"), expected.getValueClass(), expected.getValueClass());
        Assert.assertEquals(msg(path, "Default value differ"), expected.getDefaultValue(), expected.getDefaultValue());

        Map<String, AttributeType<?>> expCharacteristics = expected.characteristics();
        Map<String, AttributeType<?>> resCharacteristics = result.characteristics();

        final List<String> expKeys = new ArrayList<>(expCharacteristics.keySet());
        final List<String> resKeys = new ArrayList<>(resCharacteristics.keySet());

        while (!expKeys.isEmpty()) {
            final String pte = expKeys.remove(0);
            if (ignoredCharacteristics.contains(pte)) {
                continue;
            }
            AttributeType<?> exp = expCharacteristics.get(pte);
            AttributeType<?> res = resCharacteristics.get(pte);
            resKeys.remove(pte);
            comparePropertyType(path.sub("characteristic(" + pte + ")"), exp, res);
        }
        while (!resKeys.isEmpty()) {
            final String pte = resKeys.remove(0);
            if (ignoredCharacteristics.contains(pte)) {
                continue;
            }
            Assert.fail(msg(path, "Result type contains a characteristic not declared in expected type : " + pte));
        }

    }

    private void compareFeatureAssociationRole(Path path, FeatureAssociationRole expected, FeatureAssociationRole result) {
        compareIdentifiedType(path, expected, result);

        Assert.assertEquals(msg(path, "Minimum occurences differ"), expected.getMinimumOccurs(), result.getMinimumOccurs());
        Assert.assertEquals(msg(path, "Maximum occurences differ"), expected.getMaximumOccurs(), result.getMaximumOccurs());
        compareFeatureType(path.sub("association-valuetype"), expected.getValueType(), result.getValueType());
    }

    private void compareOperation(Path path, Operation expected, Operation result) {
        compareIdentifiedType(path, expected, result);
        Assert.assertEquals(expected.getParameters(), result.getParameters());
        compareType(path.sub("operation-result(" + expected.getResult().getName().toString() + ")"), expected.getResult(), result.getResult());
    }

    private void compareIdentifiedType(Path path, IdentifiedType expected, IdentifiedType result) {
        Assert.assertEquals(msg(path, "Name differ"), expected.getName(), result.getName());
        if (!ignoreDefinition) {
            Assert.assertEquals(msg(path, "Definition differ"), expected.getDefinition(), result.getDefinition());
        }
        if (!ignoreDesignation) {
            Assert.assertEquals(msg(path, "Designation differ"), expected.getDesignation(), result.getDesignation());
        }
        if (!ignoreDescription) {
            Assert.assertEquals(msg(path, "Description differ"), expected.getDescription(), result.getDescription());
        }

        //check deprecable
        if (expected instanceof Deprecable) {
            if (result instanceof Deprecable) {
                boolean dep1 = ((Deprecable) expected).isDeprecated();
                boolean dep2 = ((Deprecable) result).isDeprecated();
                if (dep1 != dep2) {
                    Assert.fail(msg(path, "Deprecated state differ, " + dep1 + " in expected " + dep2 + " in result"));
                }
            }
        }
    }

    private static PropertyType find(Path path, Collection<? extends PropertyType> properties, GenericName name) {
        for (PropertyType pt : properties) {
            if (pt.getName().equals(name)) {
                return pt;
            }
        }
        Assert.fail(msg(path, "Property not found for name " + name));
        return null;
    }

    private static String msg(Path path, String errorMessage) {
        return path.toString() + " " + errorMessage;
    }

    private static class Path {

        private final List<String> segments = new ArrayList<>();

        public Path sub(String segment) {
            Path p = new Path();
            p.segments.addAll(segments);
            p.segments.add(segment);
            return p;
        }

        @Override
        public String toString() {
            final StringJoiner sj = new StringJoiner(" > ");
            segments.stream().forEach(sj::add);
            return "[" + sj.toString() + "]";
        }
    }
}
