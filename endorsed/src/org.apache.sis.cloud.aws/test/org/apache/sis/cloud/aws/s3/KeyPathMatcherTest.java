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
package org.apache.sis.cloud.aws.s3;

import java.util.Locale;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link KeyPathMatcher}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class KeyPathMatcherTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public KeyPathMatcherTest() {
    }

    /**
     * Tests a pattern using "glob" syntax.
     */
    @Test
    public void testGlob() {
        final KeyPathMatcher matcher = new KeyPathMatcher("glob:bar*foo/fuu/**/f?i", ClientFileSystem.DEFAULT_SEPARATOR);
        final ClientFileSystem fs = ClientFileSystemTest.create();
        assertTrue (matcher.matches(new KeyPath(fs, "bar_skip_foo/fuu/d1/d2/d3/f_i", false)));
        assertFalse(matcher.matches(new KeyPath(fs, "bar_sk/p_foo/fuu/d1/d2/d3/f_i", false)));
    }

    /**
     * Ensures that the localized resources can be read.
     * This is not really a {@code KeyPathMatcher} test, but is put here opportunistically.
     */
    @Test
    public void testResource() {
        assertEquals("Unexpected “foo” protocol.",
                Resources.forLocale(Locale.ENGLISH).getString(Resources.Keys.UnexpectedProtocol_1, "foo"));
        assertEquals("Le protocole « foo » est inattendu.",
                Resources.forLocale(Locale.FRENCH).getString(Resources.Keys.UnexpectedProtocol_1, "foo"));
    }
}
