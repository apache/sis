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
package org.apache.sis.internal.simple;

import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link SimpleIdentifier}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(org.apache.sis.internal.util.CitationsTest.class)
public final strictfp class SimpleIdentifierTest extends TestCase {
    /**
     * Tests {@link SimpleIdentifier#toString()}.
     */
    @Test
    public void testToString() {
        final SimpleCitation authority = new SimpleCitation("EPSG");
        assertEquals("SimpleIdentifier[“EPSG:4326”]", new SimpleIdentifier(authority, "4326", false).toString());
        assertEquals("SimpleIdentifier[“EPSG”]",      new SimpleIdentifier(authority,  null,  false).toString());
        assertEquals("SimpleIdentifier[“4326”]",      new SimpleIdentifier(null,      "4326", false).toString());
        assertEquals("SimpleIdentifier[]",            new SimpleIdentifier(null,       null,  false).toString());
    }

    /**
     * Tests {@link SimpleIdentifier#toWKT()}.
     */
    @Test
    public void testToWKT() {
        final SimpleCitation authority = new SimpleCitation("EPSG");
        assertEquals("Id[\"EPSG\", \"4326\"]", new SimpleIdentifier(authority, "4326", false).toWKT());
        assertEquals("Id[\"EPSG\", null]",     new SimpleIdentifier(authority,  null,  false).toWKT());
        assertEquals("Id[null, \"4326\"]",     new SimpleIdentifier(null,      "4326", false).toWKT());
        assertEquals("Id[null, null]",         new SimpleIdentifier(null,       null,  false).toWKT());
    }
}
