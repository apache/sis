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

import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Citations} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class CitationsTest extends TestCase {
    /**
     * Tests {@link Citations#getIdentifier(Citation, boolean)}.
     */
    @Test
    public void testGetIdentifier() {
        SimpleCitation citation = new SimpleCitation(" Not an identifier ");
        assertEquals("Not an identifier", Citations.getIdentifier(citation, false));
        assertNull(Citations.getIdentifier(citation, true));

        citation = new SimpleCitation(" ValidIdentifier ");
        assertEquals("ValidIdentifier", Citations.getIdentifier(citation, false));
        assertEquals("ValidIdentifier", Citations.getIdentifier(citation, true));
    }

    /**
     * Tests {@link Citations#getUnicodeIdentifier(Citation)} with some ignorable characters.
     * Ignorable character used in this test are:
     *
     * <ul>
     *   <li>200B: zero width space</li>
     *   <li>2060: word joiner</li>
     * </ul>
     */
    @Test
    @DependsOnMethod("testGetIdentifier")
    public void testGetUnicodeIdentifier() {
        final SimpleCitation citation = new SimpleCitation(" Valid\u2060Id\u200Bentifier ");
        assertEquals("ValidIdentifier", Citations.getUnicodeIdentifier(citation));
    }
}
