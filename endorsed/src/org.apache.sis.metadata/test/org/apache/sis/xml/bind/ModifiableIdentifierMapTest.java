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

import java.net.URI;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import static java.util.UUID.fromString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.XLink;
import static org.apache.sis.xml.IdentifierSpace.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests {@link ModifiableIdentifierMap}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ModifiableIdentifierMapTest extends TestCase {
    /**
     * The HREF string to replace by {@link XLink#toString()}.
     */
    private static final String TO_REPLACE = "xlink:href=“";

    /**
     * Asserts that the content of the given map is equal to the given content, represented as a string.
     * This method replaces the {@code xlink:href} value by the {@link XLink#toString()} value before to
     * compare with the map content. This is needed because the "special case rules" cause the {@code "href"}
     * identifier to be replaced by {@code "xlink:href"}.
     *
     * @param  expected  the expected content.
     * @param  map       the map to compare with the expected content.
     */
    private static void assertMapEquals(String expected, final Map<Citation,String> map) {
        final int start = expected.indexOf(TO_REPLACE);
        if (start >= 0) {
            final int end = start + TO_REPLACE.length();
            final int close = expected.indexOf('”', end);
            final StringBuilder buffer = new StringBuilder(expected);
            buffer.replace(close, close+1, "\"]");
            buffer.replace(start, end, "xlink=XLink[href=\"");
            expected = buffer.toString();
        }
        assertEquals(expected, map.toString());
    }

    /**
     * Creates a new test case.
     */
    public ModifiableIdentifierMapTest() {
    }

    /**
     * Tests read and write operations on an {@link IdentifierMapAdapter}, using a well-formed
     * identifier collection (no null values, no duplicated authorities).
     *
     * <p>This test does not use the {@link IdentifierMap}-specific API.</p>
     */
    @Test
    public void testGetAndPut() {
        final List<Identifier> identifiers = new ArrayList<>();
        final Map<Citation,String> map = new ModifiableIdentifierMap(identifiers);
        assertTrue  (map.isEmpty());    // Newly created map shall be empty.
        assertEquals(0, map.size());
        /*
         * Add two entries, then verify the map content.
         */
        assertTrue(identifiers.add(new IdentifierMapEntry(ID,   "myID")));
        assertTrue(identifiers.add(new IdentifierMapEntry(UUID, "myUUID")));
        assertFalse (map.isEmpty());                            // After add, map shall not be empty.
        assertEquals(2, map.size());
        assertEquals(2, identifiers.size());
        assertTrue  (map.containsKey(ID));                      // Shall contain the entry we added.
        assertTrue  (map.containsKey(UUID));
        assertFalse (map.containsKey(HREF));                    // Shall not contain entry we didn't added.
        assertTrue  (map.containsValue("myID"));                // Shall contain the entry we added.
        assertTrue  (map.containsValue("myUUID"));
        assertFalse (map.containsValue("myHREF"));              // Shall not contain entry we didn't added.
        assertEquals("myID",   map.get(ID));                    // Shall contain the entry we added.
        assertEquals("myUUID", map.get(UUID));
        assertNull  (          map.get(HREF));                  // Shall not contain entry we didn't added.
        assertMapEquals("{gml:id=“myID”, gco:uuid=“myUUID”}", map);
        /*
         * Alter one entry (no new entry added).
         */
        assertEquals("myUUID", map.put(UUID, "myNewUUID"));     // Shall get the old value.
        assertFalse (map.containsValue("myUUID"));              // Shall not contain anymore the old value.
        assertTrue  (map.containsValue("myNewUUID"));           // Shall contain the new value.
        assertMapEquals("{gml:id=“myID”, gco:uuid=“myNewUUID”}", map);
        assertEquals(2, map.size());                            // Map size shall be unchanged.
        assertEquals(2, identifiers.size());
        /*
         * Add a third identifier.
         */
        assertNull  (map.put(HREF, "myHREF"));                  // Shall not contain entry we didn't added.
        assertTrue  (map.containsValue("myHREF"));              // Shall contain the entry we added.
        assertTrue  (map.containsKey(HREF));
        assertMapEquals("{gml:id=“myID”, gco:uuid=“myNewUUID”, xlink:href=“myHREF”}", map);
        assertEquals(3, map.size());                            // Map size shall be updated.
        assertEquals(3, identifiers.size());
        /*
         * Remove an identifier using the Map.remove(…) API.
         */
        assertEquals("myNewUUID", map.remove(UUID));            // Shall get the old value.
        assertFalse (map.containsValue("myNewUUID"));           // Shall not contain the entry we removed.
        assertFalse (map.containsKey(UUID));
        assertMapEquals("{gml:id=“myID”, xlink:href=“myHREF”}", map);
        assertEquals(2, map.size());                            // Map size shall be updated.
        assertEquals(2, identifiers.size());
        /*
         * Remove an identifier using the Set.remove(…) API on values.
         */
        assertTrue  (map.values().remove("XLink[href=\"myHREF\"]"));
        assertFalse (map.containsValue("myHREF"));              // Shall not contain the entry we removed.
        assertFalse (map.containsKey(HREF));
        assertMapEquals("{gml:id=“myID”}", map);
        assertEquals(1, map.size());                            // Map size shall be updated.
        assertEquals(1, identifiers.size());
        /*
         * Remove an identifier using the Set.remove(…) API on keys.
         */
        assertTrue  (map.keySet().remove(ID));
        assertFalse (map.containsValue("myID"));                // Shall not contain the entry we removed.
        assertFalse (map.containsKey(ID));
        assertMapEquals("{}", map);
        assertEquals(0, map.size());                            // Map size shall be updated.
        assertEquals(0, identifiers.size());
    }

    /**
     * Tests write operations on an {@link IdentifierMap} using specific API.
     */
    @Test
    public void testPutSpecialized() {
        final List<Identifier> identifiers = new ArrayList<>();
        final IdentifierMap map = new ModifiableIdentifierMap(identifiers);
        final String myID = "myID";
        final java.util.UUID myUUID = fromString("a1eb6e53-93db-4942-84a6-d9e7fb9db2c7");
        final URI myURI = URI.create("http://mylink");

        assertNull(map.putSpecialized(ID,   myID));
        assertNull(map.putSpecialized(UUID, myUUID));
        assertNull(map.putSpecialized(HREF, myURI));
        assertMapEquals("{gml:id=“myID”,"
                + " gco:uuid=“a1eb6e53-93db-4942-84a6-d9e7fb9db2c7”,"
                + " xlink:href=“http://mylink”}", map);

        assertSame(myID,   map.getSpecialized(ID));
        assertSame(myUUID, map.getSpecialized(UUID));
        assertSame(myURI,  map.getSpecialized(HREF));
        assertEquals("myID",                                 map.get(ID));
        assertEquals("a1eb6e53-93db-4942-84a6-d9e7fb9db2c7", map.get(UUID));
        assertEquals("http://mylink",                        map.get(HREF));
    }

    /**
     * Tests read operations on an {@link IdentifierMap} using specific API.
     */
    @Test
    public void testGetSpecialized() {
        final List<Identifier> identifiers = new ArrayList<>();
        final IdentifierMap map = new ModifiableIdentifierMap(identifiers);

        assertNull(map.put(ID,   "myID"));
        assertNull(map.put(UUID, "a1eb6e53-93db-4942-84a6-d9e7fb9db2c7"));
        assertNull(map.put(HREF, "http://mylink"));
        assertMapEquals("{gml:id=“myID”,"
                + " gco:uuid=“a1eb6e53-93db-4942-84a6-d9e7fb9db2c7”,"
                + " xlink:href=“http://mylink”}", map);

        assertEquals("myID",                                             map.get           (ID));
        assertEquals("a1eb6e53-93db-4942-84a6-d9e7fb9db2c7",             map.get           (UUID));
        assertEquals("http://mylink",                                    map.get           (HREF));
        assertEquals("myID",                                             map.getSpecialized(ID));
        assertEquals(URI.create("http://mylink"),                        map.getSpecialized(HREF));
        assertEquals(fromString("a1eb6e53-93db-4942-84a6-d9e7fb9db2c7"), map.getSpecialized(UUID));
    }

    /**
     * Tests the handling of duplicated authorities.
     */
    @Test
    public void testDuplicatedAuthorities() {
        final List<Identifier> identifiers = new ArrayList<>();
        assertTrue(identifiers.add(new IdentifierMapEntry(ID,   "myID1")));
        assertTrue(identifiers.add(new IdentifierMapEntry(UUID, "myUUID")));
        assertTrue(identifiers.add(new IdentifierMapEntry(ID,   "myID2")));

        final IdentifierMap map = new ModifiableIdentifierMap(identifiers);
        assertEquals(2, map.size());            // Duplicated authorities shall be filtered.
        assertEquals(3, identifiers.size());    // Duplicated authorities shall still exist.
        assertEquals("myID1",  map.get(ID));
        assertEquals("myUUID", map.get(UUID));

        final Iterator<Citation> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        assertSame(ID, it.next());
        it.remove();
        assertTrue(it.hasNext());
        assertSame(UUID, it.next());
        assertFalse(it.hasNext());              // Duplicated authority shall have been removed.

        assertEquals(1, identifiers.size());
        assertEquals(1, map.size());
    }

    /**
     * Tests explicitly the special handling of {@code href} values.
     */
    @Test
    public void testHRefSubstitution() {
        final List<Identifier> identifiers = new ArrayList<>();
        final IdentifierMap map = new ModifiableIdentifierMap(identifiers);
        assertNull(map.put(HREF, "myHREF"));
        assertEquals("myHREF", map.get(HREF));          // Shall contain the entry we added.

        // Check the XLink object
        final XLink link = map.getSpecialized(XLINK);
        assertEquals("myHREF", String.valueOf(link.getHRef()), "Added href shall be stored as XLink attribute.");
        assertEquals(link.toString(), assertSingleton(identifiers).getCode(), "Identifier list shall contain the XLink.");

        // Modify the XLink object directly
        link.setHRef(URI.create("myNewHREF"));
        assertEquals("myNewHREF", map.get(HREF), "Change in XLink shall be reflected in href.");
    }

    /**
     * Tests with UUIDs.
     */
    @Test
    public void testUUIDs() {
        final List<Identifier> identifiers = new ArrayList<>();
        final IdentifierMap map = new ModifiableIdentifierMap(identifiers);
        final java.util.UUID id1 = fromString("434f3107-c6d2-4c8c-bb25-553f68641c5c");
        final java.util.UUID id2 = fromString("42924124-032a-4dfe-b06e-113e3cb81cf0");

        // Add first UUID.
        assertNull(map.putSpecialized(UUID, id1));

        // Replace UUID by a new one.
        assertSame(id1, map.putSpecialized(UUID, id2));
    }
}
