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
import java.util.Set;
import java.util.List;
import java.util.Collection;
import static java.util.AbstractMap.SimpleEntry;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import static org.opengis.metadata.citation.PresentationForm.DOCUMENT_HARDCOPY;
import org.apache.sis.xml.NilReason;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultIndividual;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSingleton;

// Specific to the geoapi-4.0 branch:
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;


/**
 * Tests the {@link ValueMap} class on instances created by
 * {@link MetadataStandard#asValueMap(Object, Class, KeyNamePolicy, ValueExistencePolicy)}.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see MetadataStandardTest#testValueMap()
 */
@SuppressWarnings("exports")
public final class ValueMapTest extends TestCase {
    /**
     * The citation instance created by {@link #createCitation()}.
     */
    private DefaultCitation citation;

    /**
     * The title of the metadata instance created by {@link #createCitation()}.
     */
    private InternationalString title;

    /**
     * The author of the metadata instance created by {@link #createCitation()}.
     */
    private DefaultResponsibility author;

    /**
     * Creates a new test case.
     */
    public ValueMapTest() {
    }

    /**
     * Creates the metadata instance to be used for testing purpose.
     * This method creates the following metadata
     * (ignoring identifiers, which will be inferred from the ISBN value):
     *
     * <pre class="text">
     *   Citation
     *     ├─Title…………………………………………………… Undercurrent
     *     ├─Edition……………………………………………… &lt;nil:unknown&gt;
     *     ├─Cited Responsible Parties
     *     │   └─Individual Name……………… Testsuya Toyoda
     *     └─ISBN……………………………………………………… 9782505004509</pre>
     *
     * The citation instance is stored in the {@link #citation} field.
     * The title and author instances are stored in the {@link #title} and {@link #author} fields.
     *
     * @return the map view of the citation create by this method.
     */
    private Map<String,Object> createCitation() {
        title    = new SimpleInternationalString("Undercurrent");
        author   = new DefaultResponsibility();
        citation = new DefaultCitation(title);
        author.setParties(Set.of(new DefaultIndividual("Testsuya Toyoda", null, null)));
        citation.setCitedResponsibleParties(Set.of(author));
        citation.setISBN("9782505004509");
        citation.setEdition(NilReason.UNKNOWN.createNilObject(InternationalString.class));
        return MetadataStandard.ISO_19115.asValueMap(citation, null, KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
    }

    /**
     * Tests the {@link ValueMap#get(Object)} method.
     */
    @Test
    public void testGet() {
        final Map<String,Object> map = createCitation();
        assertEquals("Undercurrent",  map.get("title").toString());
        assertEquals(List.of(author), map.get("citedResponsibleParties"));
        assertEquals("9782505004509", map.get("ISBN"));
        assertNull(map.get("edition"), "NilObject shall be excluded.");
        /*
         * The ISBN shall also be visible as an identifier.
         */
        final Object identifiers = map.get("identifiers");
        assertInstanceOf(Collection.class, identifiers);
        final Object identifier = assertSingleton((Collection<?>) identifiers);
        assertEquals("9782505004509", assertInstanceOf(Identifier.class, identifier).getCode());
    }

    /**
     * Tests the {@link ValueMap#entrySet()} method.
     * The expected metadata is:
     *
     * <pre class="text">
     *   Citation
     *     ├─Title…………………………………………………… Undercurrent
     *     ├─Identifiers…………………………………… 9782505004509
     *     ├─Cited Responsible Parties
     *     │   └─Individual Name……………… Testsuya Toyoda
     *     └─ISBN……………………………………………………… 9782505004509</pre>
     *
     * Note that this test is intentionally sensitive to iteration order.
     * That order shall be fixed by the {@code XmlType} annotation.
     */
    @Test
    public void testEntrySet() {
        final Map<String,Object> map = createCitation();
        assertEquals(1, citation.getIdentifiers().size());
        assertArrayEquals(new SimpleEntry<?,?>[] {
            new SimpleEntry<>("title",                   title),
            new SimpleEntry<>("identifiers",             citation.getIdentifiers()),
            new SimpleEntry<>("citedResponsibleParties", List.of(author)),
            new SimpleEntry<>("ISBN",                    "9782505004509")
        }, map.entrySet().toArray());
    }

    /**
     * Tests the {@link ValueMap#entrySet()} method for the same metadata as {@link #testEntrySet()},
     * but asking for all non-null and non-nil entries including the empty collections.
     */
    @Test
    public void testEntrySetForNonNil() {
        final Map<String,Object> map = createCitation();
        final Map<String,Object> all = MetadataStandard.ISO_19115.asValueMap(citation,
                null, KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_NIL);
        assertFalse(map.containsKey("alternateTitles"), "Null values shall be excluded.");
        assertTrue (all.containsKey("alternateTitles"), "Null values shall be included.");
        assertFalse(map.containsKey("edition"), "Nil objects shall be excluded.");
        assertFalse(all.containsKey("edition"), "Nil objects shall be excluded.");
        assertTrue (all.entrySet().containsAll(map.entrySet()), "'all' shall be a larger map than 'map'.");
        assertFalse(map.entrySet().containsAll(all.entrySet()), "'all' shall be a larger map than 'map'.");
        assertArrayEquals(new SimpleEntry<?,?>[] {
            new SimpleEntry<>("title",                   title),
            new SimpleEntry<>("alternateTitles",         List.of()),
            new SimpleEntry<>("dates",                   List.of()),
            new SimpleEntry<>("identifiers",             citation.getIdentifiers()),
            new SimpleEntry<>("citedResponsibleParties", List.of(author)),
            new SimpleEntry<>("presentationForms",       Set.of()),
            new SimpleEntry<>("otherCitationDetails",    List.of()),
            new SimpleEntry<>("ISBN",                    "9782505004509"),
            new SimpleEntry<>("onlineResources",         List.of()),
            new SimpleEntry<>("graphics",                List.of())
        }, all.entrySet().toArray());
    }

    /**
     * Tests the {@link ValueMap#entrySet()} method for the same metadata as {@link #testEntrySet()},
     * but asking for all non-null entries including nil objects and the empty collections.
     */
    @Test
    public void testEntrySetForNonNull() {
        final Map<String,Object> map = createCitation();
        final Map<String,Object> all = MetadataStandard.ISO_19115.asValueMap(citation,
                null, KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_NULL);
        assertFalse(map.containsKey("alternateTitles"), "Null values shall be excluded.");
        assertTrue (all.containsKey("alternateTitles"), "Null values shall be included.");
        assertFalse(map.containsKey("edition"), "Nil objects shall be excluded.");
        assertTrue (all.containsKey("edition"), "Nil objects shall be included.");
        assertTrue (all.entrySet().containsAll(map.entrySet()), "'all' shall be a larger map than 'map'.");
        assertFalse(map.entrySet().containsAll(all.entrySet()), "'all' shall be a larger map than 'map'.");
        assertArrayEquals(new SimpleEntry<?,?>[] {
            new SimpleEntry<>("title",                   title),
            new SimpleEntry<>("alternateTitles",         List.of()),
            new SimpleEntry<>("dates",                   List.of()),
            new SimpleEntry<>("edition",                 NilReason.UNKNOWN.createNilObject(InternationalString.class)),
            new SimpleEntry<>("identifiers",             citation.getIdentifiers()),
            new SimpleEntry<>("citedResponsibleParties", List.of(author)),
            new SimpleEntry<>("presentationForms",       Set.of()),
            new SimpleEntry<>("otherCitationDetails",    List.of()),
            new SimpleEntry<>("ISBN",                    "9782505004509"),
            new SimpleEntry<>("onlineResources",         List.of()),
            new SimpleEntry<>("graphics",                List.of())
        }, all.entrySet().toArray());
    }

    /**
     * Tests the {@link ValueMap#entrySet()} method for the same metadata as {@link #testEntrySet()},
     * but asking for all entries including null and empty values.
     */
    @Test
    public void testEntrySetForAll() {
        final Map<String,Object> map = createCitation();
        final Map<String,Object> all = MetadataStandard.ISO_19115.asValueMap(citation,
                null, KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.ALL);
        assertFalse(map.containsKey("alternateTitles"), "Null values shall be excluded.");
        assertTrue (all.containsKey("alternateTitles"), "Null values shall be included.");
        assertTrue (all.entrySet().containsAll(map.entrySet()), "'all' shall be a larger map than 'map'.");
        assertFalse(map.entrySet().containsAll(all.entrySet()), "'all' shall be a larger map than 'map'.");
        assertArrayEquals(new SimpleEntry<?,?>[] {
            new SimpleEntry<>("title",                   title),
            new SimpleEntry<>("alternateTitles",         List.of()),
            new SimpleEntry<>("dates",                   List.of()),
            new SimpleEntry<>("edition",                 NilReason.UNKNOWN.createNilObject(InternationalString.class)),
            new SimpleEntry<>("editionDate",             null),
            new SimpleEntry<>("identifiers",             citation.getIdentifiers()),
            new SimpleEntry<>("citedResponsibleParties", List.of(author)),
            new SimpleEntry<>("presentationForms",       Set.of()),
            new SimpleEntry<>("series",                  null),
            new SimpleEntry<>("otherCitationDetails",    List.of()),
//          new SimpleEntry<>("collectiveTitle",         null),  -- deprecated as of ISO 19115:2014.
            new SimpleEntry<>("ISBN",                    "9782505004509"),
            new SimpleEntry<>("ISSN",                    null),
            new SimpleEntry<>("onlineResources",         List.of()),
            new SimpleEntry<>("graphics",                List.of())
        }, all.entrySet().toArray());
    }

    /**
     * Tests the {@link ValueMap#put(String,Object)} and {@link ValueMap#remove(Object)} methods.
     * Note that this test is intentionally sensitive to iteration order.
     * That order shall be fixed by the {@code XmlType} annotation.
     */
    @Test
    public void testPutAndRemove() {
        final Map<String,Object> map = createCitation();
        /*
         * Remove the ISBN value. Result shall be:
         *
         * Citation
         *   ├─Title…………………………………………………… Undercurrent
         *   └─Cited Responsible Parties
         *       └─Individual Name……………… Testsuya Toyoda
         */
        assertEquals("9782505004509", map.remove("ISBN"));
        assertNull(citation.getISBN(), "ISBN shall have been removed.");
        assertTrue(citation.getIdentifiers().isEmpty(), "ISBN shall have been removed.");
        assertArrayEquals(new SimpleEntry<?,?>[] {
            new SimpleEntry<>("title", title),
            new SimpleEntry<>("citedResponsibleParties", List.of(author))
        }, map.entrySet().toArray());
        /*
         * Add a value. Result shall be:
         *
         * Citation
         *   ├─Title…………………………………………………… Undercurrent
         *   ├─Cited Responsible Parties
         *   │   └─Individual Name……………… Testsuya Toyoda
         *   └─Presentation Forms………………… document hardcopy
         */
        assertNull(map.put("presentationForm", DOCUMENT_HARDCOPY));
        assertEquals(DOCUMENT_HARDCOPY, assertSingleton(citation.getPresentationForms()));
        assertArrayEquals(new SimpleEntry<?,?>[] {
            new SimpleEntry<>("title", title),
            new SimpleEntry<>("citedResponsibleParties", List.of(author)),
            new SimpleEntry<>("presentationForms", Set.of(DOCUMENT_HARDCOPY))
        }, map.entrySet().toArray());
        /*
         * Add back the ISBN value. Result shall be:
         *
         * Citation
         *   ├─Title…………………………………………………… Undercurrent
         *   ├─Identifiers…………………………………… 9782505004509
         *   ├─Cited Responsible Parties
         *   │   └─Individual Name……………… Testsuya Toyoda
         *   ├─Presentation Forms………………… document hardcopy
         *   └─ISBN……………………………………………………… 9782505004509
         */
        assertNull(map.put("ISBN", "9782505004509"));
        assertEquals("9782505004509", citation.getISBN());
        assertEquals(1, citation.getIdentifiers().size(), "ISBN shall appears in the identifier list.");
        assertArrayEquals(new SimpleEntry<?,?>[] {
            new SimpleEntry<>("title",                   title),
            new SimpleEntry<>("identifiers",             citation.getIdentifiers()),
            new SimpleEntry<>("citedResponsibleParties", List.of(author)),
            new SimpleEntry<>("presentationForms",       Set.of(DOCUMENT_HARDCOPY)),
            new SimpleEntry<>("ISBN",                    "9782505004509")
        }, map.entrySet().toArray());
    }

    /**
     * Tests {@link ValueMap#putIfAbsent(String, Object)}.
     */
    @Test
    public void testPutIfAbsent() {
        final Map<String, Object> cmap = createCitation();
        assertEquals("Undercurrent", String.valueOf(cmap.putIfAbsent("title", "A new title")));
        assertEquals("Undercurrent", String.valueOf(cmap.get("title")));
        assertEquals("Undercurrent", String.valueOf(cmap.remove("title")));
        assertNull(cmap.putIfAbsent("title", "A new title"));
        assertEquals("A new title",  String.valueOf(cmap.get("title")));
    }
}
