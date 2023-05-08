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

import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;


/**
 * Tests {@link IdentifierFilter}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
public final class IdentifierFilterTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory<Feature,Object,?> factory;

    /**
     * Creates a new test case.
     */
    public IdentifierFilterTest() {
        factory = DefaultFilterFactory.forFeatures();
    }

    /**
     * Tests construction and serialization.
     */
    @Test
    public void testSerialize() {
        assertSerializedEquals(factory.resourceId("abc"));
    }

    /**
     * Tests on features of diffferent types. Test data are:
     * <ul>
     *   <li>A feature type with an identifier as a string.</li>
     *   <li>A feature type with an integer identifier.</li>
     *   <li>A feature type with no identifier.</li>
     * </ul>
     */
    @Test
    public void testEvaluate() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.addAttribute(String.class).setName("att").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        final Feature f1 = ftb.setName("Test 1").build().newInstance();
        f1.setPropertyValue("att", "123");

        ftb.clear().addAttribute(Integer.class).setName("att").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        final Feature f2 = ftb.setName("Test 2").build().newInstance();
        f2.setPropertyValue("att", 123);

        final Feature f3 = ftb.clear().setName("Test 3").build().newInstance();

        final Filter<Feature> id = factory.resourceId("123");
        assertEquals(Feature.class, id.getResourceClass());
        assertTrue (id.test(f1));
        assertTrue (id.test(f2));
        assertFalse(id.test(f3));
    }

    /**
     * Tests evaluation of two identifiers combined by a OR logical operator.
     */
    @Test
    public void testEvaluateCombined() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.addAttribute(String.class).setName("att").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        final FeatureType type = ftb.setName("Test").build();

        final Feature f1 = type.newInstance(); f1.setPropertyValue("att", "123");
        final Feature f2 = type.newInstance(); f2.setPropertyValue("att", "abc");
        final Feature f3 = type.newInstance(); f3.setPropertyValue("att", "abc123");

        final Filter<Feature> id = factory.or(
                factory.resourceId("abc"),
                factory.resourceId("123"));

        assertEquals(Feature.class, id.getResourceClass());
        assertTrue (id.test(f1));
        assertTrue (id.test(f2));
        assertFalse(id.test(f3));
    }
}
