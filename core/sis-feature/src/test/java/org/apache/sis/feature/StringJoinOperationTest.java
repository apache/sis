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

import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.feature.PropertyType;


/**
 * Tests {@link StringJoinOperation}.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    AbstractOperationTest.class,
    DenseFeatureTest.class
})
public final strictfp class StringJoinOperationTest extends TestCase {
    /**
     * Creates a feature type with an string join operation.
     * The feature contains the following properties:
     *
     * <ul>
     *   <li>{@code name} as a {@link String}</li>
     *   <li>{@code age} as an {@link Integer}</li>
     *   <li>{@code summary} as string join of {@code name} and {@code age} attributes.</li>
     * </ul>
     *
     * @return The feature for a person.
     */
    private static DefaultFeatureType person() {
        final PropertyType nameType = new DefaultAttributeType<>(name("name"), String.class, 1, 1, null);
        final PropertyType ageType  = new DefaultAttributeType<>(name("age"), Integer.class, 1, 1, null);
        final PropertyType cmpType  = FeatureOperations.compound(name("concat"), "/", "prefix:", ":suffix", nameType, ageType);
        return new DefaultFeatureType(name("person"), false, null, nameType, ageType, cmpType);
    }

    /**
     * Creates the identification map to be given to attribute, operation and feature constructors.
     */
    private static Map<String,?> name(final String name) {
        return Collections.singletonMap(AbstractIdentifiedType.NAME_KEY, name);
    }

    /**
     * Tests {@code StringJoinOperation.Result.getValue()} on sparse and dense features.
     */
    @Test
    public void testGetValue() {
        final DefaultFeatureType person = person();
        testGetValue(new DenseFeature (person));
        testGetValue(new SparseFeature(person));
    }

    /**
     * Executes the {@link #testGetValue()} on the given feature, which is either sparse or dense.
     */
    private static void testGetValue(final AbstractFeature feature) {
        assertEquals("prefix:/:suffix", feature.getPropertyValue("concat"));

        feature.setPropertyValue("name", "marc");
        assertEquals("prefix:marc/:suffix", feature.getPropertyValue("concat"));

        feature.setPropertyValue("age", 21);
        assertEquals("prefix:marc/21:suffix", feature.getPropertyValue("concat"));
    }

    /**
     * Tests {@code StringJoinOperation.Result.setValue(String)} on sparse and dense features.
     */
    @Test
    @DependsOnMethod("testGetValue")
    public void testSetValue() {
        final DefaultFeatureType person = person();
        testSetValue(new DenseFeature (person));
        testSetValue(new SparseFeature(person));
    }

    /**
     * Executes the {@link #testSetValue()} on the given feature, which is either sparse or dense.
     */
    private static void testSetValue(final AbstractFeature feature) {
        feature.setPropertyValue("concat", "prefix:emile/37:suffix");
        assertEquals("name",   "emile", feature.getPropertyValue("name"));
        assertEquals("age",         37, feature.getPropertyValue("age"));
        assertEquals("concat", "prefix:emile/37:suffix", feature.getPropertyValue("concat"));
    }
}
