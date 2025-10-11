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
package org.apache.sis.metadata;

import java.util.Map;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.NilObject;
import org.apache.sis.util.SimpleInternationalString;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests the {@link NilReasonMap} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class NilReasonMapTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public NilReasonMapTest() {
    }

    /**
     * Creates a metadata instance to use for the tests.
     * This metadata has a mandatory property, which is the citation title.
     *
     * @param  title  a value for the mandatory property, of {@code null} if none.
     */
    private static DefaultCitation metadata(final String title) {
        final var citation = new DefaultCitation(title);
        citation.getAlternateTitles().add(new SimpleInternationalString("Another title"));
        return citation;
    }

    /**
     * Tests a map with no
     */
    @Test
    public void testCitation() {
        final DefaultCitation citation = metadata("A title");
        final Map<String,NilReason> map = MetadataStandard.ISO_19115.asNilReasonMap(citation, Citation.class, KeyNamePolicy.UML_IDENTIFIER);
        assertTrue(map.isEmpty());
        /*
         * Sets the edition date to a nil value.
         */
        assertNull (map.get        ("editionDate"));
        assertFalse(map.containsKey("editionDate"));
        assertNull (map.put        ("editionDate", NilReason.TEMPLATE));
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
        assertEquals("editionDate",      assertSingleton(map.keySet()));
        assertEquals(NilReason.TEMPLATE, assertSingleton(map.values()));
        assertEquals(NilReason.TEMPLATE, map.put("editionDate", null));
        /*
         * Set the title to nil value instead of edition date.
         */
        assertTrue  (map.isEmpty());
        assertFalse (map.containsKey("title"));
        assertNull  (map.put("title", NilReason.MISSING));
        assertTrue  (map.containsKey("title"));
        assertEquals(NilReason.MISSING, map.get("title"));
        assertEquals(1, map.size());
        assertEquals("title",           assertSingleton(map.keySet()));
        assertEquals(NilReason.MISSING, assertSingleton(map.values()));
        assertInstanceOf(NilObject.class, citation.getTitle());
        /*
         * Even if we clear the citation title, because that property is mandatory,
         * it should still be present in the map of nil reasons.
         */
        assertEquals(NilReason.MISSING, map.put("title", null));
        assertNull  (citation.getTitle());
        assertTrue  (map.containsKey("title"));
        assertEquals(1, map.size());
        assertEquals("title", assertSingleton(map.keySet()));
        assertNull  (         assertSingleton(map.values()));
    }
}
