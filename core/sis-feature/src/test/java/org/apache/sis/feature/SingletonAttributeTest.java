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

import java.util.Locale;
import java.util.HashMap;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.DomainConsistency;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link SingletonAttribute}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn({
    DefaultAttributeTypeTest.class,
    PropertySingletonTest.class
})
public final strictfp class SingletonAttributeTest extends TestCase {
    /**
     * Creates an attribute for the city name.
     * This attribute has a default value.
     */
    static SingletonAttribute<String> city() {
        return new SingletonAttribute<String>(DefaultAttributeTypeTest.city());
    }

    /**
     * Creates an attribute for a city population value.
     * This attribute has no default value.
     */
    static SingletonAttribute<Integer> population() {
        return new SingletonAttribute<Integer>(DefaultAttributeTypeTest.population(new HashMap<String,Object>(4)));
    }

    /**
     * Creates an attribute type for a parliament name.
     * This applies only to features of type "Capital".
     */
    static SingletonAttribute<String> parliament() {
        return new SingletonAttribute<String>(DefaultAttributeTypeTest.parliament());
    }

    /**
     * Tests getting and setting an attribute value.
     */
    @Test
    public void testValue() {
        final AbstractAttribute<Integer> attribute = population();
        assertNull("value",  attribute.getValue());
        assertTrue("values", attribute.getValues().isEmpty());

        final Integer value = 1000;
        attribute.setValue(value);
        assertEquals     ("value",                 value,  attribute.getValue());
        assertArrayEquals("values", new Integer[] {value}, attribute.getValues().toArray());

        attribute.setValue(null);
        assertNull("value",  attribute.getValue());
        assertTrue("values", attribute.getValues().isEmpty());
    }

    /**
     * Tests {@link SingletonAttribute#quality()}.
     */
    @Test
    @DependsOnMethod("testValue")
    @SuppressWarnings("unchecked")
    public void testQuality() {
        final AbstractAttribute<Integer> attribute = population();
        DataQuality quality = attribute.quality();
        assertEquals("scope.level", ScopeCode.ATTRIBUTE, quality.getScope().getLevel());
        assertDomainConsistencyEquals("population", "Missing value for “population” property.",
                (DomainConsistency) getSingleton(quality.getReports()));
        /*
         * Intentionally store a value of the wrong type, and test again.
         */
        ((AbstractAttribute) attribute).setValue(4.5f);
        quality = attribute.quality();
        assertEquals("scope.level", ScopeCode.ATTRIBUTE, quality.getScope().getLevel());
        assertDomainConsistencyEquals("population", "Expected an instance of ‘Integer’ for the “population” property, but got an instance of ‘Float’.",
                (DomainConsistency) getSingleton(quality.getReports()));
    }

    /**
     * Verifies that the given element reports a validation failure with the given explanation.
     *
     * @param propertyName The name of the property that failed validation.
     * @param explanation  The expected explanation.
     * @param consistency  The report element to test.
     */
    private static void assertDomainConsistencyEquals(final String propertyName, final String explanation,
            final DomainConsistency consistency)
    {
        assertEquals("report.measureIdentification", propertyName, consistency.getMeasureIdentification().getCode());
        final ConformanceResult result = (ConformanceResult) getSingleton(consistency.getResults());
        assertFalse ("report.result.pass", result.pass());
        assertEquals("report.result.explanation", explanation, result.getExplanation().toString(Locale.US));
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
        assertFalse("equals(null)", a1.equals(null));
        testEquals(a1, a2);
    }

    /**
     * Implementation of {@link #testEquals()} used also by {@link #testClone()}.
     */
    static void testEquals(final AbstractAttribute<Integer> a1, final AbstractAttribute<Integer> a2) {
        assertTrue  ("equals",   a1.equals(a2));
        assertEquals("hashCode", a1.hashCode(), a2.hashCode());
        a2.setValue(1000);
        assertFalse("equals",   a1.equals(a2));
        assertFalse("hashCode", a1.hashCode() == a2.hashCode());
    }

    /**
     * Tests {@link SingletonAttribute#clone()}.
     *
     * @throws CloneNotSupportedException Should never happen.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testClone() throws CloneNotSupportedException {
        final SingletonAttribute<Integer> a1 = population();
        final  AbstractAttribute<Integer> a2 = a1.clone();
        assertNotSame(a1, a2);
        testEquals(a1, a2);
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testSerialization() {
        final AbstractAttribute<String> attribute = city();
        attribute.setValue("Atlantide");
        assertNotSame(attribute, assertSerializedEquals(attribute));
    }

    /**
     * Tests {@link SingletonAttribute#toString()}.
     */
    @Test
    @DependsOnMethod("testValue")
    public void testToString() {
        final AbstractAttribute<String> city = city();
        assertEquals("Attribute[“city” : String] = Utopia", city.toString());
        city.setValue("Dystopia");
        assertEquals("Attribute[“city” : String] = Dystopia", city.toString());
    }
}
