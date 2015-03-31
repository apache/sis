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
import java.util.HashMap;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.acquisition.EnvironmentalRecord;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.util.AbstractMap.SimpleEntry;
import static org.apache.sis.metadata.KeyNamePolicy.*;


/**
 * Tests the {@link NameMap} class on instances created by
 * {@link MetadataStandard#asNameMap(Class, KeyNamePolicy, KeyNamePolicy)}.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(PropertyAccessorTest.class)
public final strictfp class NameMapTest extends TestCase {
    /**
     * Tests {@code NameMap.entrySet()} for an exact match (including iteration order).
     * The properties used in this test are listed in {@link PropertyAccessorTest#testConstructor()}.
     *
     * @see PropertyAccessorTest#testConstructor()
     */
    @Test
    public void testEntrySet() {
        final Map<String,String> map = MetadataStandard.ISO_19115.asNameMap(
                Citation.class, KeyNamePolicy.UML_IDENTIFIER, KeyNamePolicy.JAVABEANS_PROPERTY);
        assertArrayEquals(new Object[] {
            new SimpleEntry<String,String>("title",                 "title"),
            new SimpleEntry<String,String>("alternateTitle",        "alternateTitles"),
            new SimpleEntry<String,String>("date",                  "dates"),
            new SimpleEntry<String,String>("edition",               "edition"),
            new SimpleEntry<String,String>("editionDate",           "editionDate"),
            new SimpleEntry<String,String>("identifier",            "identifiers"),
            new SimpleEntry<String,String>("citedResponsibleParty", "citedResponsibleParties"),
            new SimpleEntry<String,String>("presentationForm",      "presentationForms"),
            new SimpleEntry<String,String>("series",                "series"),
            new SimpleEntry<String,String>("otherCitationDetails",  "otherCitationDetails"),
//          new SimpleEntry<String,String>("collectiveTitle",       "collectiveTitle"),  -- deprecated as of ISO 19115:2014
            new SimpleEntry<String,String>("ISBN",                  "ISBN"),
            new SimpleEntry<String,String>("ISSN",                  "ISSN"),
            new SimpleEntry<String,String>("graphic",               "graphics"),
            new SimpleEntry<String,String>("onlineResource",        "onlineResources")
        }, map.entrySet().toArray());

        assertEquals("alternateTitles", map.get("alternateTitle"));
        assertNull("Shall not exists.", map.get("dummy"));
    }

    /**
     * Tests the formatting of sentences.
     */
    @Test
    public void testSentences() {
        final Map<String,String> map, expected = new HashMap<String,String>();
        map = MetadataStandard.ISO_19115.asNameMap(EnvironmentalRecord.class, JAVABEANS_PROPERTY, SENTENCE);
        assertNull(expected.put("averageAirTemperature",    "Average air temperature"));
        assertNull(expected.put("maxAltitude",              "Max altitude"));
        assertNull(expected.put("maxRelativeHumidity",      "Max relative humidity"));
        assertNull(expected.put("meteorologicalConditions", "Meteorological conditions"));
        assertEquals(expected, map);
    }

    /**
     * Ensures that the string are interned. Note that the library will not break if strings are not interned;
     * it would just consume more memory than needed. We want to intern those strings because they usually
     * match method names or field names, which are already interned by the JVM.
     *
     * <div class="section">Explicit calls to {@code String.intern()}</div>
     * I though that annotation strings were interned like any other constants, but it does not
     * seem to be the case as of JDK7. To check if a future JDK release still needs explicit
     * call to {@link String#intern()}, try to remove the "{@code .intern()}" part in the
     * {@link PropertyAccessor#name(int, KeyNamePolicy)} method and run this test again.
     */
    @Test
    public void testStringIntern() {
        String name;
        Map<String,String> map;
        /*
         * Tests explicit intern.
         */
        map = MetadataStandard.ISO_19115.asNameMap(EnvironmentalRecord.class, SENTENCE, JAVABEANS_PROPERTY);
        name = map.get("Average air temperature");
        assertEquals("averageAirTemperature", name);
        assertSame  ("averageAirTemperature", name);
        /*
         * Tests implicit intern.
         */
        map = MetadataStandard.ISO_19115.asNameMap(EnvironmentalRecord.class, SENTENCE, METHOD_NAME);
        name = map.get("Average air temperature");
        assertEquals("getAverageAirTemperature", name);
        assertSame  ("getAverageAirTemperature", name);
        /*
         * Tests an other intern.
         */
        map = MetadataStandard.ISO_19115.asNameMap(EnvironmentalRecord.class, SENTENCE, UML_IDENTIFIER);
        name = map.get("Average air temperature");
        assertEquals("averageAirTemperature", name);
        assertSame  ("averageAirTemperature", name);
    }
}
