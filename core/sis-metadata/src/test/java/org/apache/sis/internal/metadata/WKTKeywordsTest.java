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
package org.apache.sis.internal.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link WKTKeywords} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class WKTKeywordsTest extends TestCase {
    /**
     * Ensures that all constants are equal to the name of the field that declare it.
     * The intend is to avoid misleading constant names when reading code.
     *
     * <p>This test is not strictly necessary. We are just checking an arbitrary convention here, not a requirement.
     * If a developer change the constant values without changing the constant names (for example in order to use the
     * abridged WKT 2 keyword names instead that their long name), this is okay — just ignore this test.</p>
     *
     * @throws ReflectiveOperationException should never happen.
     */
    @Test
    public void verifyConstantValues() throws Exception {
        for (final Field field : WKTKeywords.class.getDeclaredFields()) {
            final String name = field.getName();
            final int modifiers = field.getModifiers();
            assertTrue(name, Modifier.isPublic(modifiers));
            assertTrue(name, Modifier.isStatic(modifiers));
            assertTrue(name, Modifier.isFinal (modifiers));
            assertEquals("As a policy of WKTKeywords, constants value should be equal to field name.", name, field.get(null));
        }
    }
}
