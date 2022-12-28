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
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.internal.geoapi.filter.Literal;
import org.apache.sis.internal.geoapi.filter.ValueReference;


/**
 * Tests {@link LeafExpression}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final class LeafExpressionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final DefaultFilterFactory<AbstractFeature,Object,?> factory;

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
        final Expression<AbstractFeature, String> filter = factory.property("some_property", String.class);
        assertEquals("some_property", ((ValueReference<?,?>) filter).getXPath());
        assertSerializedEquals(filter);
    }

    /**
     * Tests creation and serialization of "Literal".
     */
    @Test
    public void testLiteralSerialization() {
        final Literal<?,?> f1 = (Literal<?,?>) factory.literal(true);
        final Literal<?,?> f2 = (Literal<?,?>) factory.literal("a text string");
        final Literal<?,?> f3 = (Literal<?,?>) factory.literal('x');
        final Literal<?,?> f4 = (Literal<?,?>) factory.literal(122);
        final Literal<?,?> f5 = (Literal<?,?>) factory.literal(45.56);

        assertEquals(Boolean.TRUE,    f1.getValue());
        assertEquals("a text string", f2.getValue());
        assertEquals('x',             f3.getValue());
        assertEquals(122,             f4.getValue());
        assertEquals(45.56,           f5.getValue());

        assertSerializedEquals(f1);
        assertSerializedEquals(f2);
        assertSerializedEquals(f3);
        assertSerializedEquals(f4);
        assertSerializedEquals(f5);
    }

    /**
     * Tests evaluation of "ValueReference", including with type conversion.
     */
    @Test
    public void testReferenceEvaluation() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.addAttribute(String.class).setName("some_property");
        final AbstractFeature f = ftb.setName("Test").build().newInstance();

        Expression<AbstractFeature,?> ref = factory.property("some_property");
        assertNull(ref.apply(f));
        assertNull(ref.apply(null));

        f.setPropertyValue("some_property", "road");
        assertEquals("road", ref.apply(f));

        ref = factory.property("some_property", String.class);
        assertEquals("road", ref.apply(f));

        f.setPropertyValue("some_property", "45.1");
        assertEquals("45.1", ref.apply(f));

        ref = factory.property("some_property", Double.class);
        assertEquals(45.1, ref.apply(f));
    }

    /**
     * Tests evaluation of "Literal".
     */
    @Test
    public void testLiteralEvaluation() {
        final Expression<AbstractFeature,?> literal = factory.literal(12.45);
        assertEquals(12.45, literal.apply(null));
    }
}
