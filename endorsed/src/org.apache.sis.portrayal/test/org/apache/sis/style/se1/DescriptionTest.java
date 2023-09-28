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
package org.apache.sis.style.se1;

import org.apache.sis.util.SimpleInternationalString;

// Test dependencies
import org.junit.Test;


/**
 * Tests for {@link Description}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class DescriptionTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public DescriptionTest() {
    }

    /**
     * Test of {@code Title} property.
     */
    @Test
    public void testTitle() {
        var i18n = new SimpleInternationalString("A random title");
        final var cdt = factory.createDescription();
        assertEmpty(cdt.getTitle());
        cdt.setTitle(i18n);
        assertOptionalEquals(i18n, cdt.getTitle());
    }

    /**
     * Test of {@code Abstract} property.
     */
    @Test
    public void testAbstract() {
        var i18n = new SimpleInternationalString("A random abstract");
        final var cdt = factory.createDescription();
        assertEmpty(cdt.getAbstract());
        cdt.setAbstract(i18n);
        assertOptionalEquals(i18n, cdt.getAbstract());
    }
}
