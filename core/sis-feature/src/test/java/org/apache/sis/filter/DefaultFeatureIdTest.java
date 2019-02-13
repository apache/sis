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
import static org.apache.sis.test.Assert.assertSerializedEquals;
import org.apache.sis.test.TestCase;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterFactory2;

/**
 * Tests {@link DefaultFeatureId}.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class DefaultFeatureIdTest extends TestCase {
    /**
     * Test factory.
     */
    @Test
    public void testConstructor() {
        final FilterFactory2 factory = new DefaultFilterFactory();
        assertNotNull(factory.featureId("abc"));
        assertNotNull(factory.gmlObjectId("abc"));
    }

    /**
     * Tests evaluation.
     */
    @Test
    public void testEvaluate() {
        final DefaultFeatureId fid = new DefaultFeatureId("123");

        // a feature type with a string identifier
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("type1");
        ftb.addAttribute(String.class).setName("att").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        final FeatureType typeString = ftb.build();

        // a feature type with an integer identifier
        ftb.clear();
        ftb.setName("type2");
        ftb.addAttribute(Integer.class).setName("att").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        final FeatureType typeInt = ftb.build();

        // a feature type with no identifier
        ftb.clear();
        ftb.setName("type3");
        final FeatureType typeNone = ftb.build();

        final Feature feature1 = typeString.newInstance();
        feature1.setPropertyValue("att", "123");

        final Feature feature2 = typeInt.newInstance();
        feature2.setPropertyValue("att", 123);

        final Feature feature3 = typeNone.newInstance();

        Assert.assertTrue(fid.matches(feature1));
        Assert.assertTrue(fid.matches(feature2));
        Assert.assertFalse(fid.matches(feature3));
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialize() {
        assertSerializedEquals(new DefaultFeatureId("abc"));
    }

}
