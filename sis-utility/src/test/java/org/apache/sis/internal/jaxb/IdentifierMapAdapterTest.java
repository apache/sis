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
import java.util.Collection;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.UUID.fromString;
import static org.apache.sis.test.Assert.*;
import static org.apache.sis.xml.IdentifierSpace.*;


/**
 * Tests {@link IdentifierMapAdapter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.18)
 * @version 0.3
 * @module
 */
public strictfp class IdentifierMapAdapterTest extends TestCase {
    /**
     * Creates the {@link IdentifierMapAdapter} instance to test for the given identifiers.
     * Subclasses will override this method.
     *
     * @param  identifiers The identifiers to wrap in an {@code IdentifierMapAdapter}.
     * @return The {@code IdentifierMapAdapter} to test.
     */
    IdentifierMapAdapter create(final Collection<Identifier> identifiers) {
        return new IdentifierMapAdapter(identifiers);
    }

    /**
     * Asserts that the content of the given map is equals to the given content, represented
     * as a string. Subclasses can override this method in order to alter the expected string.
     *
     * @param  expected The expected content.
     * @return The map to compare with the expected content.
     */
    void assertMapEquals(final String expected, final Map<Citation,String> map) {
        assertEquals(expected, map.toString());
    }

    /**
     * Returns a string representation of the given {@code href} value.
     * The default implementation returns the value unchanged.
     */
    String toHRefString(final String href) {
        return href;
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
        final Map<Citation,String> map = create(identifiers);
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        identifiers.add(new IdentifierMapEntry(ID,   "myID"));
        identifiers.add(new IdentifierMapEntry(UUID, "myUUID"));
        assertFalse (map.isEmpty());
        assertEquals(2, map.size());
        assertEquals(2, identifiers.size());
        assertTrue  (map.containsKey(ID));
        assertTrue  (map.containsKey(UUID));
        assertFalse (map.containsKey(HREF));
        assertEquals("myID",   map.get(ID));
        assertEquals("myUUID", map.get(UUID));
        assertNull  (          map.get(HREF));
        assertTrue  (map.containsValue("myID"));
        assertTrue  (map.containsValue("myUUID"));
        assertFalse (map.containsValue("myHREF"));
        assertMapEquals("{gml:id=“myID”, gco:uuid=“myUUID”}", map);

        assertEquals("myUUID", map.put(UUID, "myNewUUID"));
        assertFalse (map.containsValue("myUUID"));
        assertTrue  (map.containsValue("myNewUUID"));
        assertMapEquals("{gml:id=“myID”, gco:uuid=“myNewUUID”}", map);
        assertEquals(2, map.size());
        assertEquals(2, identifiers.size());

        assertNull  (map.put(HREF, "myHREF"));
        assertTrue  (map.containsValue("myHREF"));
        assertTrue  (map.containsKey(HREF));
        assertMapEquals("{gml:id=“myID”, gco:uuid=“myNewUUID”, xlink:href=“myHREF”}", map);
        assertEquals(3, map.size());
        assertEquals(3, identifiers.size());

        assertEquals("myNewUUID", map.remove(UUID));
        assertFalse (map.containsValue("myNewUUID"));
        assertFalse (map.containsKey(UUID));
        assertMapEquals("{gml:id=“myID”, xlink:href=“myHREF”}", map);
        assertEquals(2, map.size());
        assertEquals(2, identifiers.size());

        assertTrue  (map.values().remove(toHRefString("myHREF")));
        assertFalse (map.containsValue("myHREF"));
        assertFalse (map.containsKey(HREF));
        assertMapEquals("{gml:id=“myID”}", map);
        assertEquals(1, map.size());
        assertEquals(1, identifiers.size());

        assertTrue  (map.keySet().remove(ID));
        assertFalse (map.containsValue("myID"));
        assertFalse (map.containsKey(ID));
        assertMapEquals("{}", map);
        assertEquals(0, map.size());
        assertEquals(0, identifiers.size());
    }

    /**
     * Tests write operations on an {@link IdentifierMap} using specific API.
     */
    @Test
    public void testPutSpecialized() {
        final List<Identifier> identifiers = new ArrayList<>();
        final IdentifierMap map = create(identifiers);
        final String myID = "myID";
        final java.util.UUID myUUID = fromString("a1eb6e53-93db-4942-84a6-d9e7fb9db2c7");
        final URI myURI = URI.create("http://mylink");

        map.putSpecialized(ID,   myID);
        map.putSpecialized(UUID, myUUID);
        map.putSpecialized(HREF, myURI);
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
        final IdentifierMap map = create(identifiers);

        map.put(ID,   "myID");
        map.put(UUID, "a1eb6e53-93db-4942-84a6-d9e7fb9db2c7");
        map.put(HREF, "http://mylink");
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
        identifiers.add(new IdentifierMapEntry(ID,   "myID1"));
        identifiers.add(new IdentifierMapEntry(UUID, "myUUID"));
        identifiers.add(new IdentifierMapEntry(ID,   "myID2"));

        final IdentifierMap map = create(identifiers);
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
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSame(ID,   assertSerializedEquals(ID));
        assertSame(UUID, assertSerializedEquals(UUID));
        assertSame(HREF, assertSerializedEquals(HREF));

        final List<Identifier> identifiers = new ArrayList<>();
        final Map<Citation,String> map = create(identifiers);
        identifiers.add(new IdentifierMapEntry(ID,   "myID"));
        identifiers.add(new IdentifierMapEntry(UUID, "myUUID"));

        final Map<Citation,String> copy = assertSerializedEquals(map);
        assertNotSame(map, copy);
        assertEquals(2, copy.size());
    }
}
