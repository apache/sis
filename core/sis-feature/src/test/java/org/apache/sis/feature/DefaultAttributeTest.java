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
 * Tests {@link DefaultAttribute}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(DefaultAttributeTypeTest.class)
public final strictfp class DefaultAttributeTest extends TestCase {
    /**
     * Creates an attribute for the city name.
     * This attribute has a default value.
     */
    static DefaultAttribute<String> city() {
        return new DefaultAttribute<>(DefaultAttributeTypeTest.city(new HashMap<>(4)));
    }

    /**
     * Creates an attribute for a singleton value.
     * This attribute has no default value.
     */
    static DefaultAttribute<Integer> population() {
        return new DefaultAttribute<>(DefaultAttributeTypeTest.population(new HashMap<>(4)));
    }

    /**
     * Creates an attribute for a singleton value.
     * This attribute has no default value.
     */
    static DefaultAttribute<String> parliament() {
        return new DefaultAttribute<>(DefaultAttributeTypeTest.parliament());
    }

    /**
     * Tests getting and setting an attribute value.
     */
    @Test
    public void testValue() {
        final DefaultAttribute<Integer> attribute = population();
        assertNull("value", attribute.getValue());
        attribute.setValue(1000);
        assertEquals("value", Integer.valueOf(1000), attribute.getValue());
        attribute.setValue(null);
        assertNull("value", attribute.getValue());
    }

    /**
     * Tests {@link DefaultAttribute#quality()}.
     */
    @Test
    @DependsOnMethod("testValue")
    @SuppressWarnings("unchecked")
    public void testQuality() {
        final DefaultAttribute<Integer> attribute = population();
        DataQuality quality = attribute.quality();
        assertEquals("scope.level", ScopeCode.ATTRIBUTE, quality.getScope().getLevel());
        assertDomainConsistencyEquals("population", "Missing value for “population” property.",
                (DomainConsistency) getSingleton(quality.getReports()));
        /*
         * Intentionally store a value of the wrong type, and test again.
         */
        ((DefaultAttribute) attribute).setValue(4.5f);
        quality = attribute.quality();
        assertEquals("scope.level", ScopeCode.ATTRIBUTE, quality.getScope().getLevel());
        assertDomainConsistencyEquals("population", "Property “population” does not accept instances of ‘Float’.",
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
    public void testEquals() {
        final DefaultAttribute<Integer> a1 = population();
        final DefaultAttribute<Integer> a2 = population();
        assertFalse("equals(null)", a1.equals(null));
        testEquals(a1, a2);
    }

    /**
     * Implementation of {@link #testEquals()} used also by {@link #testClone()}.
     */
    private static void testEquals(final DefaultAttribute<Integer> a1, final DefaultAttribute<Integer> a2) {
        assertTrue  ("equals",   a1.equals(a2));
        assertEquals("hashCode", a1.hashCode(), a2.hashCode());
        a2.setValue(1000);
        assertFalse("equals",   a1.equals(a2));
        assertFalse("hashCode", a1.hashCode() == a2.hashCode());
    }

    /**
     * Tests {@link DefaultAttribute#clone()}.
     *
     * @throws CloneNotSupportedException Should never happen.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testClone() throws CloneNotSupportedException {
        final DefaultAttribute<Integer> a1 = population();
        final DefaultAttribute<Integer> a2 = a1.clone();
        assertNotSame(a1, a2);
        testEquals(a1, a2);
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testSerialization() {
        final DefaultAttribute<String> attribute = city();
        assertNotSame(attribute, assertSerializedEquals(attribute));
    }

    /**
     * Tests {@link DefaultAttribute#toString()}.
     */
    @Test
    @DependsOnMethod("testValue")
    public void testToString() {
        final DefaultAttribute<String> city = city();
        assertEquals("Attribute[“city” : String] = Utopia", city.toString());
        city.setValue("Dystopia");
        assertEquals("Attribute[“city” : String] = Dystopia", city.toString());
    }
}
