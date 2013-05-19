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

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Related to JDK8
import org.apache.sis.internal.jdk8.Function;


/**
 * Tests the {@link CollectionsExt} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class CollectionsExtTest extends TestCase {
    /**
     * Tests {@link CollectionsExt#toCaseInsensitiveNameMap(Collection, Function, Locale)}.
     */
    @Test
    public void testToCaseInsensitiveNameMap() {
        final Function<String,String> nameFunction = ObjectConverters.identity(String.class);
        final Map<String,String> expected = new HashMap<String,String>();
        assertNull(expected.put("AA", "AA"));
        assertNull(expected.put("Aa", "Aa")); // No mapping for "aa", because of ambiguity between "AA" and "Aa".
        assertNull(expected.put("BB", "BB"));
        assertNull(expected.put("bb", "bb"));
        assertNull(expected.put("CC", "CC"));
        assertNull(expected.put("cc", "CC")); // Automatically added.

        final List<String> elements = Arrays.asList("AA", "Aa", "BB", "bb", "CC");
        for (int i=0; i<10; i++) {
            Collections.shuffle(elements);
            assertMapEquals(expected, CollectionsExt.toCaseInsensitiveNameMap(elements, nameFunction, Locale.ROOT));
        }
    }
}
