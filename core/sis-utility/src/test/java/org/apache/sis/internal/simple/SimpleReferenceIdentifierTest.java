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

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link SimpleReferenceIdentifier}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class SimpleReferenceIdentifierTest extends TestCase {
    /**
     * Tests {@link SimpleReferenceIdentifier#toString()}.
     */
    @Test
    public void testToString() {
        final SimpleCitation authority = new SimpleCitation("EPSG");
        assertEquals("SimpleReferenceIdentifier[“EPSG:4326”]", new SimpleReferenceIdentifier(authority, "4326").toString());
        assertEquals("SimpleReferenceIdentifier[“EPSG”]",      new SimpleReferenceIdentifier(authority,  null ).toString());
        assertEquals("SimpleReferenceIdentifier[“4326”]",      new SimpleReferenceIdentifier(null,      "4326").toString());
        assertEquals("SimpleReferenceIdentifier[]",            new SimpleReferenceIdentifier(null,       null ).toString());
    }

    /**
     * Tests {@link SimpleReferenceIdentifier#toWKT()}.
     */
    @Test
    public void testToWKT() {
        final SimpleCitation authority = new SimpleCitation("EPSG");
        assertEquals("ID[\"EPSG\", \"4326\"]", new SimpleReferenceIdentifier(authority, "4326").toWKT());
        assertEquals("ID[\"EPSG\", null]",     new SimpleReferenceIdentifier(authority,  null ).toWKT());
        assertEquals("ID[null, \"4326\"]",     new SimpleReferenceIdentifier(null,      "4326").toWKT());
        assertEquals("ID[null, null]",         new SimpleReferenceIdentifier(null,       null ).toWKT());
    }
}
