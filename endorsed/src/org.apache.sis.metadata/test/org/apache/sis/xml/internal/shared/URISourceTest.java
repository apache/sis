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
package org.apache.sis.xml.internal.shared;

import java.net.URI;
import java.net.URISyntaxException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;


/**
 * Tests {@link URISource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class URISourceTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public URISourceTest() {
    }

    /**
     * Tests {@link URISource#isChildOfBase()}, used for access control.
     *
     * @throws URISyntaxException if a test URI is invalid.
     */
    @Test
    public void testIsChildOfBase() throws URISyntaxException {
        final var base = new URI("http://foo.com/dir/dummy.txt");
        URISource source = new URISource(base, new URI("sibling.txt"));
        assertEquals(new URI("http://foo.com/dir/sibling.txt"), source.document);
        assertNull(source.fragment);
        assertTrue(source.isChildOfBase());

        source = new URISource(base, new URI("../dir/more/child.txt"));
        assertEquals(new URI("http://foo.com/dir/more/child.txt"), source.document);
        assertNull(source.fragment);
        assertTrue(source.isChildOfBase());

        source = new URISource(base, new URI("../other/child.txt"));
        assertEquals(new URI("http://foo.com/other/child.txt"), source.document);
        assertNull(source.fragment);
        assertFalse(source.isChildOfBase());

        source = new URISource(base, new URI("http://other.com/dir/sibling.txt"));
        assertEquals(new URI("http://other.com/dir/sibling.txt"), source.document);
        assertNull(source.fragment);
        assertFalse(source.isChildOfBase());
    }
}
