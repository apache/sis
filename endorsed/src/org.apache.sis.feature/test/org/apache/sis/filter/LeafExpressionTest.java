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
package org.apache.sis.filter;

import org.apache.sis.feature.builder.FeatureTypeBuilder;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory;


/**
 * Tests {@link LeafExpression}.
 *
 * @author  Johann Sorel (Geomatys)
 */
@SuppressWarnings("exports")
public final class LeafExpressionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory<Feature, ?, ?> factory;

    /**
     * Creates a new test case.
     */
    public LeafExpressionTest() {
        factory = DefaultFilterFactory.forFeatures();
    }

    /**
     * Tests creation and serialization of "ValueReference".
     */
    @Test
    public void testReferenceSerialization() {
        var filter = factory.property("some_property", String.class);
        assertEquals("some_property", filter.getXPath());
        assertSerializedEquals(filter);
    }

    /**
     * Tests creation and serialization of "Literal".
     */
    @Test
    public void testLiteralSerialization() {
        final var e1 = factory.literal(true);
        final var e2 = factory.literal("a text string");
        final var e3 = factory.literal('x');
        final var e4 = factory.literal(122);
        final var e5 = factory.literal(45.56);

        assertEquals(Boolean.TRUE,    e1.getValue());
        assertEquals("a text string", e2.getValue());
        assertEquals('x',             e3.getValue());
        assertEquals(122,             e4.getValue());
        assertEquals(45.56,           e5.getValue());

        assertSerializedEquals(e1);
        assertSerializedEquals(e2);
        assertSerializedEquals(e3);
        assertSerializedEquals(e4);
        assertSerializedEquals(e5);
    }

    /**
     * Tests evaluation of "ValueReference", including with type conversion.
     */
    @Test
    public void testReferenceEvaluation() {
        final var builder = new FeatureTypeBuilder();
        builder.addAttribute(String.class).setName("some_property");
        final var instance = builder.setName("Test").build().newInstance();

        var reference = factory.property("some_property");
        assertEquals(Feature.class, reference.getResourceClass());
        assertNull(reference.apply(instance));
        assertNull(reference.apply(null));

        instance.setPropertyValue("some_property", "road");
        assertEquals("road", reference.apply(instance));

        reference = factory.property("some_property", String.class);
        assertEquals("road", reference.apply(instance));

        instance.setPropertyValue("some_property", "45.1");
        assertEquals("45.1", reference.apply(instance));

        reference = factory.property("some_property", Double.class);
        assertEquals(45.1, reference.apply(instance));
    }

    /**
     * Tests evaluation of "Literal".
     */
    @Test
    public void testLiteralEvaluation() {
        final var literal = factory.literal(12.45);
        assertEquals(12.45, literal.apply(null));
    }
}
