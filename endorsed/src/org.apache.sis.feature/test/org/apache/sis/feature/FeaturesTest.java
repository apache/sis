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

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link Features}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class FeaturesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public FeaturesTest() {
    }

    /**
     * Tests {@code Features.cast(AttributeType, Class)}.
     */
    @Test
    public void testCastAttributeType() {
        final DefaultAttributeType<String> parliament = DefaultAttributeTypeTest.parliament();
        assertSame(parliament, Features.cast(parliament, String.class));

        var e = assertThrows(ClassCastException.class, () -> Features.cast(parliament, CharSequence.class));
        assertMessageContains(e, "parliament", "String", "CharSequence");
    }

    /**
     * Tests {@code cast(Attribute, Class)}.
     */
    @Test
    public void testCastAttributeInstance() {
        final AbstractAttribute<String> parliament = SingletonAttributeTest.parliament();
        assertSame(parliament, Features.cast(parliament, String.class));

        var e = assertThrows(ClassCastException.class, () -> Features.cast(parliament, CharSequence.class));
        assertMessageContains(e, "parliament", "String", "CharSequence");
    }

    /**
     * Tests {@code validate(Feature)}.
     */
    @Test
    public void testValidate() {
        final var feature = DefaultFeatureTypeTest.city().newInstance();
        /*
         * Feature is invalid because of missing property “population”.
         * Validation should raise an exception.
         */
        var e = assertThrows(IllegalArgumentException.class, () -> Features.validate(feature));
        String message = e.getMessage();
        assertTrue(message.contains("city") || message.contains("population"), message);

        // Should pass validation.
        feature.setPropertyValue("city", "Utopia");
        feature.setPropertyValue("population", 10);
        Features.validate(feature);
    }
}
