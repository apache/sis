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
package org.apache.sis.xml.bind;

import java.util.Map;
import java.util.ArrayList;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import static org.apache.sis.xml.IdentifierSpace.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link IdentifierMapAdapter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class IdentifierMapAdapterTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public IdentifierMapAdapterTest() {
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSame(ID,   assertSerializedEquals(ID));
        assertSame(UUID, assertSerializedEquals(UUID));
        assertSame(HREF, assertSerializedEquals(HREF));

        final var identifiers = new ArrayList<Identifier>();
        final Map<Citation,String> map = new IdentifierMapAdapter(identifiers);
        assertTrue(identifiers.add(new IdentifierMapEntry(ID,   "myID")));
        assertTrue(identifiers.add(new IdentifierMapEntry(UUID, "myUUID")));

        final Map<Citation,String> copy = assertSerializedEquals(map);
        assertNotSame(map, copy);
        assertEquals(2, copy.size());
    }
}
