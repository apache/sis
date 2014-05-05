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
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


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
     * Tests attribute comparison.
     */
    @Test
    public void testEquals() {
        final DefaultAttribute<Integer> a1 = population();
        final DefaultAttribute<Integer> a2 = population();
        assertFalse ("equals",   a1.equals(null));
        assertTrue  ("equals",   a1.equals(a2));
        assertEquals("hashCode", a1.hashCode(), a2.hashCode());
        a2.setValue(1000);
        assertFalse("equals",   a1.equals(a2));
        assertFalse("hashCode", a1.hashCode() == a2.hashCode());
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testSerialization() {
        final DefaultAttribute<String> attribute = city();
        assertSerializedEquals(attribute);
    }
}
