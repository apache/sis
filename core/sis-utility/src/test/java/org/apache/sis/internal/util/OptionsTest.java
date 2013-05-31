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
package org.apache.sis.internal.util;

import java.util.Map;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link Options} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class OptionsTest extends TestCase {
    /**
     * Tests the {@link Options#set(Map, OptionKey, Object)} method
     * followed by {@link Options#get(Map, OptionKey)}.
     */
    @Test
    public void testSetAndGet() {
        assertNull(Options.get(null, OptionKey.URL_ENCODING));
        assertNull(Options.set(null, OptionKey.URL_ENCODING, null));

        final Map<OptionKey<?>,Object> options = Options.set(null, OptionKey.URL_ENCODING, "UTF-8");
        assertEquals("UTF-8", getSingleton(options.values()));
        assertEquals("UTF-8", Options.get(options, OptionKey.URL_ENCODING));

        assertSame(options, Options.set(options, OptionKey.URL_ENCODING, "ISO-8859-1"));
        assertEquals("ISO-8859-1", getSingleton(options.values()));
        assertEquals("ISO-8859-1", Options.get(options, OptionKey.URL_ENCODING));
    }

    /**
     * Tests the {@link Options#list(Map, String, StringBuilder)} method.
     */
    @Test
    @DependsOnMethod("testSetAndGet")
    public void testList() {
        final Map<OptionKey<?>,Object> options = Options.set(null, OptionKey.URL_ENCODING, "UTF-8");

        final StringBuilder buffer = new StringBuilder();
        Options.list(options, "", buffer);
        assertMultilinesEquals(
            "options={\n" +
            "    URL_ENCODING = UTF-8\n" +
            "}", buffer.toString());
    }
}
