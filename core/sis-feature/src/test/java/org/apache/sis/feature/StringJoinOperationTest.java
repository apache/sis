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


/**
 * Tests {@link StringJoinOperation}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(LinkOperationTest.class)
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
     * The operation uses {@code "<<:"} and {@code ":>>"} as prefix and suffix respectively
     * avoid avoiding confusion if a code spelled the variable name (e.g. {@code prefix})
     * instead of using it.
     *
     * @return The feature for a person.
     */
    private static DefaultFeatureType person() {
        final AbstractIdentifiedType nameType = new DefaultAttributeType<String> (name("name"), String.class, 1, 1, null);
        final AbstractIdentifiedType ageType  = new DefaultAttributeType<Integer>(name("age"), Integer.class, 1, 1, null);
        final AbstractIdentifiedType cmpType  = FeatureOperations.compound(name("concat"), "/", "<<:", ":>>", nameType, ageType);
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
     * This test does not use the {@code '\'} escape character.
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
        assertEquals("<<:/:>>", feature.getPropertyValue("concat"));

        feature.setPropertyValue("name", "marc");
        assertEquals("<<:marc/:>>", feature.getPropertyValue("concat"));

        feature.setPropertyValue("age", 21);
        assertEquals("<<:marc/21:>>", feature.getPropertyValue("concat"));
    }

    /**
     * Tests {@code StringJoinOperation.Result.setValue(String)} on sparse and dense features.
     * This test does not use the {@code '\'} escape character.
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
        feature.setPropertyValue("concat", "<<:emile/37:>>");
        assertEquals("name",   "emile", feature.getPropertyValue("name"));
        assertEquals("age",         37, feature.getPropertyValue("age"));
        assertEquals("concat", "<<:emile/37:>>", feature.getPropertyValue("concat"));
    }

    /**
     * Tests {@code getValue()} and {@code setValue(String)} with values that contains the {@code '\'}
     * escape character.
     */
    @Test
    @DependsOnMethod({"testGetValue", "testSetValue"})
    public void testEscapeCharacter() {
        final DenseFeature feature = new DenseFeature(person());
        feature.setPropertyValue("name", "marc/emile\\julie");
        feature.setPropertyValue("age", 30);
        assertEquals("<<:marc\\/emile\\\\julie/30:>>", feature.getPropertyValue("concat"));

        feature.setPropertyValue("concat", "<<:emile\\\\julie\\/marc/:>>");
        assertEquals("name", "emile\\julie/marc", feature.getPropertyValue("name"));
        assertNull  ("age", feature.getPropertyValue("age"));
    }

    /**
     * Verifies that proper exceptions are thrown in case of illegal argument.
     */
    @Test
    public void testIllegalArgument() {
        final DenseFeature feature = new DenseFeature(person());
        try {
            feature.setPropertyValue("concat", "((:marc/21:>>");
            fail("Should fail because of mismatched prefix.");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("<<:"));
            assertTrue(message, message.contains("(("));
        }
        try {
            feature.setPropertyValue("concat", "<<:marc/21:))");
            fail("Should fail because of mismatched suffix.");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains(":>>"));
            assertTrue(message, message.contains("))"));
        }
        try {
            feature.setPropertyValue("concat", "<<:marc/21/julie:>>");
            fail("Should fail because of too many components.");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("<<:marc/21/julie:>>"));
        }
        try {
            feature.setPropertyValue("concat", "<<:marc/julie:>>");
            fail("Should fail because of unparsable number.");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("julie"));
            assertTrue(message, message.contains("age"));
        }
    }
}
