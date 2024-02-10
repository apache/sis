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

import java.util.Arrays;
import java.util.HashMap;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link MultiValuedAttribute}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(SingletonAttributeTest.class)
public final class MultiValuedAttributeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MultiValuedAttributeTest() {
    }

    /**
     * Creates an attribute for a list of universities.
     * The multiplicity is [0 … ∞].
     */
    static MultiValuedAttribute<String> universities() {
        return new MultiValuedAttribute<>(DefaultAttributeTypeTest.universities());
    }

    /**
     * Creates an attribute for a city population value.
     * This attribute has no default value.
     */
    private static MultiValuedAttribute<Integer> population() {
        return new MultiValuedAttribute<>(DefaultAttributeTypeTest.population(new HashMap<>(4)));
    }

    /**
     * Tests getting and setting a single attribute value.
     */
    @Test
    public void testValue() {
        final AbstractAttribute<String> attribute = universities();
        assertNull(attribute.getValue());
        assertTrue(attribute.getValues().isEmpty());

        final String   value  = "University of arts";
        final String[] values = {value};

        attribute.setValue(value);
        assertEquals     (value,  attribute.getValue());
        assertArrayEquals(values, attribute.getValues().toArray());

        attribute.setValue(null);
        assertNull(attribute.getValue());
        assertTrue(attribute.getValues().isEmpty());

        attribute.setValues(Arrays.asList(values));
        assertEquals     (value,  attribute.getValue());
        assertArrayEquals(values, attribute.getValues().toArray());
    }

    /**
     * Tests getting and setting multiple attribute values.
     */
    @Test
    @DependsOnMethod("testValue")
    public void testValues() {
        final AbstractAttribute<String> attribute = universities();
        final String[] values = {
            "University of arts",
            "University of sciences",
            "University of international development"
        };
        attribute.setValues(Arrays.asList(values));
        assertArrayEquals(values, attribute.getValues().toArray());

        var e = assertThrows(IllegalStateException.class, () -> attribute.getValue(),
                             "getValue() shall not be allowed when there is more than one value.");
        assertMessageContains(e, "universities");
    }

    /**
     * Tests initialization to the default value.
     */
    @Test
    @DependsOnMethod("testValue")
    public void testDefaultValue() {
        final AbstractAttribute<String> attribute = new MultiValuedAttribute<>(DefaultAttributeTypeTest.city());
        assertEquals     (              "Utopia",  attribute.getValue());
        assertArrayEquals(new String[] {"Utopia"}, attribute.getValues().toArray());

        attribute.setValue("Atlantide");
        assertEquals     (              "Atlantide",  attribute.getValue());
        assertArrayEquals(new String[] {"Atlantide"}, attribute.getValues().toArray());

        attribute.setValue(null);
        assertNull(attribute.getValue());
        assertTrue(attribute.getValues().isEmpty());
    }

    /**
     * Tests attribute comparison.
     */
    @Test
    @DependsOnMethod("testValue")
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() {
        final AbstractAttribute<Integer> a1 = population();
        final AbstractAttribute<Integer> a2 = population();
        assertFalse(a1.equals(null));
        SingletonAttributeTest.testEquals(a1, a2);
    }

    /**
     * Tests {@link MultiValuedAttribute#clone()}.
     *
     * @throws CloneNotSupportedException should never happen.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testClone() throws CloneNotSupportedException {
        final MultiValuedAttribute<Integer> a1 = population();
        final    AbstractAttribute<Integer> a2 = a1.clone();
        assertNotSame(a1, a2);
        SingletonAttributeTest.testEquals(a1, a2);
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testSerialization() {
        final AbstractAttribute<String> attribute = universities();
        attribute.setValue("University of international development");
        assertNotSame(attribute, assertSerializedEquals(attribute));
    }
}
