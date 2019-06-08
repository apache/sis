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
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory2;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultObjectId}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class DefaultObjectIdTest extends TestCase {
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
     * Creates 3 features for testing purpose. Features are (in that order):
     *
     * <ol>
     *   <li>A feature type with an identifier as a string.</li>
     *   <li>A feature type with an integer identifier.</li>
     *   <li>A feature type with no identifier.</li>
     * </ol>
     */
    private static Feature[] features() {
        final Feature[] features = new Feature[3];
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("type1");
        ftb.addAttribute(String.class).setName("att").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        Feature f = ftb.build().newInstance();
        f.setPropertyValue("att", "123");
        features[0] = f;

        ftb.clear();
        ftb.setName("type2");
        ftb.addAttribute(Integer.class).setName("att").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        f = ftb.build().newInstance();
        f.setPropertyValue("att", 123);
        features[1] = f;

        ftb.clear();
        ftb.setName("type3");
        f = ftb.build().newInstance();
        features[2] = f;

        return features;
    }

    /**
     * Tests evaluation.
     */
    @Test
    public void testEvaluate() {
        final DefaultObjectId fid = new DefaultObjectId("123");
        final Feature[] features = features();
        assertTrue (fid.matches(features[0]));
        assertTrue (fid.matches(features[1]));
        assertFalse(fid.matches(features[2]));
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialize() {
        assertSerializedEquals(new DefaultObjectId("abc"));
    }
}
