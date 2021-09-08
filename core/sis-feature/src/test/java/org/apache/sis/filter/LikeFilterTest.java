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

import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Literal;


/**
 * Tests {@link LikeFilter} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class LikeFilterTest extends TestCase {
    /**
     * Tests {@link LikeFilter#isMetaCharacter(char)}.
     */
    @Test
    public void testIsMetaCharacter() {
        assertTrue (LikeFilter.isMetaCharacter('.'));
        assertTrue (LikeFilter.isMetaCharacter('*'));
        assertTrue (LikeFilter.isMetaCharacter('?'));
        assertTrue (LikeFilter.isMetaCharacter('('));
        assertTrue (LikeFilter.isMetaCharacter(')'));
        assertTrue (LikeFilter.isMetaCharacter('['));
        assertTrue (LikeFilter.isMetaCharacter(']'));
        assertTrue (LikeFilter.isMetaCharacter('{'));
        assertTrue (LikeFilter.isMetaCharacter('}'));
        assertTrue (LikeFilter.isMetaCharacter('\\'));
        assertTrue (LikeFilter.isMetaCharacter('^'));
        assertTrue (LikeFilter.isMetaCharacter('$'));
        assertTrue (LikeFilter.isMetaCharacter('|'));
        assertTrue (LikeFilter.isMetaCharacter('+'));
        assertFalse(LikeFilter.isMetaCharacter('&'));
        assertFalse(LikeFilter.isMetaCharacter('-'));
    }

    /**
     * Tests "Like" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testLike() {
        final FilterFactory<Feature,Object,?> factory = DefaultFilterFactory.forFeatures();
        final Literal<Feature, String> literal = factory.literal("Apache SIS");

        assertTrue (factory.like(literal, "Apache%").test(null));
        assertFalse(factory.like(literal, "Oracle%").test(null));

        // A character is missing, should not match.
        assertFalse(factory.like(literal, "Apache%IS_%").test(null));
        assertTrue (factory.like(literal, "Apache%I_"  ).test(null));

        // Test case insensitive match.
        assertTrue(factory.like(literal, "apache sis", '%', '_', '\\', false).test(null));

        // Test character escape.
        assertTrue(factory.like(factory.literal("*Apache* SIS"), "!*Apache!* S.S", '*', '.', '!', true).test(null));
    }
}
