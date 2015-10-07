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
package org.apache.sis.internal.jaxb;

import java.net.URI;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.xml.XLink;
import org.junit.Test;

import static java.util.UUID.fromString;
import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.xml.IdentifierSpace.*;


/**
 * Tests {@link ModifiableIdentifierMap}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(IdentifierMapAdapterTest.class)
public final strictfp class ModifiableIdentifierMapTest extends TestCase {
    /**
     * The HREF string to replace by {@link XLink#toString()}.
     */
    private static final String TO_REPLACE = "xlink:href=“";

    /**
     * Asserts that the content of the given map is equals to the given content, represented as a string.
     * This method replaces the {@code xlink:href} value by the {@link XLink#toString()} value before to
     * compare with the map content. This is needed because the "special case rules" cause the {@code "href"}
     * identifier to be replaced by {@code "xlink:href"}.
     *
     * @param  expected The expected content.
     * @return The map to compare with the expected content.
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
     * Tests read and write operations on an {@link IdentifierMapAdapter}, using a well-formed
     * identifier collection (no null values, no duplicated authorities).
     *
     * <p>This test does not use the {@link IdentifierMap}-specific API.</p>
     */
    @Test
    public void testGetAndPut() {
        final List<Identifier> identifiers = new ArrayList<Identifier>();
        final Map<Citation,String> map = new ModifiableIdentifierMap(identifiers);
        assertTrue  ("Newly created map shall be empty.", map.isEmpty());
        assertEquals("Newly created map shall be empty.", 0, map.size());
        /*
         * Add two entries, then verify the map content.
         */
        assertTrue(identifiers.add(new IdentifierMapEntry(ID,   "myID")));
        assertTrue(identifiers.add(new IdentifierMapEntry(UUID, "myUUID")));
        assertFalse ("After add, map shall not be empty.", map.isEmpty());
        assertEquals("After add, map shall not be empty.", 2, map.size());
        assertEquals("After add, map shall not be empty.", 2, identifiers.size());
        assertTrue  ("Shall contain the entry we added.",        map.containsKey(ID));
        assertTrue  ("Shall contain the entry we added.",        map.containsKey(UUID));
        assertFalse ("Shall not contain entry we didn't added.", map.containsKey(HREF));
        assertTrue  ("Shall contain the entry we added.",        map.containsValue("myID"));
        assertTrue  ("Shall contain the entry we added.",        map.containsValue("myUUID"));
        assertFalse ("Shall not contain entry we didn't added.", map.containsValue("myHREF"));
        assertEquals("Shall contain the entry we added.",        "myID",   map.get(ID));
        assertEquals("Shall contain the entry we added.",        "myUUID", map.get(UUID));
        assertNull  ("Shall not contain entry we didn't added.",           map.get(HREF));
        assertMapEquals("{gml:id=“myID”, gco:uuid=“myUUID”}", map);
        /*
         * Alter one entry (no new entry added).
         */
        assertEquals("Shall get the old value.",       "myUUID", map.put(UUID, "myNewUUID"));
        assertFalse ("Shall not contain anymore the old value.", map.containsValue("myUUID"));
        assertTrue  ("Shall contain the new value.",             map.containsValue("myNewUUID"));
        assertMapEquals("{gml:id=“myID”, gco:uuid=“myNewUUID”}", map);
        assertEquals("Map size shall be unchanged.", 2, map.size());
        assertEquals("Map size shall be unchanged.", 2, identifiers.size());
        /*
         * Add a third identifier.
         */
        assertNull  ("Shall not contain entry we didn't added.", map.put(HREF, "myHREF"));
        assertTrue  ("Shall contain the entry we added.",        map.containsValue("myHREF"));
        assertTrue  ("Shall contain the entry we added.",        map.containsKey(HREF));
        assertMapEquals("{gml:id=“myID”, gco:uuid=“myNewUUID”, xlink:href=“myHREF”}", map);
        assertEquals("Map size shall be updated.", 3, map.size());
        assertEquals("Map size shall be updated.", 3, identifiers.size());
        /*
         * Remove an identifier using the Map.remove(…) API.
         */
        assertEquals("Shall get the old value.",   "myNewUUID", map.remove(UUID));
        assertFalse ("Shall not contain the entry we removed.", map.containsValue("myNewUUID"));
        assertFalse ("Shall not contain the entry we removed.", map.containsKey(UUID));
        assertMapEquals("{gml:id=“myID”, xlink:href=“myHREF”}", map);
        assertEquals("Map size shall be updated.", 2, map.size());
        assertEquals("Map size shall be updated.", 2, identifiers.size());
        /*
         * Remove an identifier using the Set.remove(…) API on values.
         */
        assertTrue  (map.values().remove("XLink[href=\"myHREF\"]"));
        assertFalse ("Shall not contain the entry we removed.", map.containsValue("myHREF"));
        assertFalse ("Shall not contain the entry we removed.", map.containsKey(HREF));
        assertMapEquals("{gml:id=“myID”}", map);
        assertEquals("Map size shall be updated.", 1, map.size());
        assertEquals("Map size shall be updated.", 1, identifiers.size());
        /*
         * Remove an identifier using the Set.remove(…) API on keys.
         */
        assertTrue  (map.keySet().remove(ID));
        assertFalse ("Shall not contain the entry we removed.", map.containsValue("myID"));
        assertFalse ("Shall not contain the entry we removed.", map.containsKey(ID));
        assertMapEquals("{}", map);
        assertEquals("Map size shall be updated.", 0, map.size());
        assertEquals("Map size shall be updated.", 0, identifiers.size());
    }

    /**
     * Tests write operations on an {@link IdentifierMap} using specific API.
     */
    @Test
    public void testPutSpecialized() {
        final List<Identifier> identifiers = new ArrayList<Identifier>();
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
        final List<Identifier> identifiers = new ArrayList<Identifier>();
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
        final List<Identifier> identifiers = new ArrayList<Identifier>();
        assertTrue(identifiers.add(new IdentifierMapEntry(ID,   "myID1")));
        assertTrue(identifiers.add(new IdentifierMapEntry(UUID, "myUUID")));
        assertTrue(identifiers.add(new IdentifierMapEntry(ID,   "myID2")));

        final IdentifierMap map = new ModifiableIdentifierMap(identifiers);
        assertEquals("Duplicated authorities shall be filtered.", 2, map.size());
        assertEquals("Duplicated authorities shall still exist.", 3, identifiers.size());
        assertEquals("myID1",  map.get(ID));
        assertEquals("myUUID", map.get(UUID));

        final Iterator<Citation> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        assertSame(ID, it.next());
        it.remove();
        assertTrue(it.hasNext());
        assertSame(UUID, it.next());
        assertFalse("Duplicated authority shall have been removed.", it.hasNext());

        assertEquals(1, identifiers.size());
        assertEquals(1, map.size());
    }

    /**
     * Tests explicitely the special handling of {@code href} values.
     */
    @Test
    public void testHRefSubstitution() {
        final List<Identifier> identifiers = new ArrayList<Identifier>();
        final IdentifierMap map = new ModifiableIdentifierMap(identifiers);
        assertNull(map.put(HREF, "myHREF"));
        assertEquals("Shall contain the entry we added.", "myHREF", map.get(HREF));

        // Check the XLink object
        final XLink link = map.getSpecialized(XLINK);
        assertEquals("Added href shall be stored as XLink attribute.", "myHREF", String.valueOf(link.getHRef()));
        assertEquals("Identifier list shall contain the XLink.", link.toString(), getSingleton(identifiers).getCode());

        // Modidfy the XLink object directly
        link.setHRef(URI.create("myNewHREF"));
        assertEquals("Change in XLink shall be reflected in href.", "myNewHREF", map.get(HREF));
    }

    /**
     * Tests with UUIDs.
     */
    @Test
    public void testUUIDs() {
        final List<Identifier> identifiers = new ArrayList<Identifier>();
        final IdentifierMap map = new ModifiableIdentifierMap(identifiers);
        final java.util.UUID id1 = fromString("434f3107-c6d2-4c8c-bb25-553f68641c5c");
        final java.util.UUID id2 = fromString("42924124-032a-4dfe-b06e-113e3cb81cf0");

        // Add first UUID.
        assertNull(map.putSpecialized(UUID, id1));

        // Replace UUID by a new one.
        assertSame(id1, map.putSpecialized(UUID, id2));
    }
}
