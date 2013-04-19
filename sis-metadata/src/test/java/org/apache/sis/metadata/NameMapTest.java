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
 * @since   0.3 (derived from geotk-3.04)
 * @version 0.3
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
            new SimpleEntry<>("title",                 "title"),
            new SimpleEntry<>("alternateTitle",        "alternateTitles"),
            new SimpleEntry<>("date",                  "dates"),
            new SimpleEntry<>("edition",               "edition"),
            new SimpleEntry<>("editionDate",           "editionDate"),
            new SimpleEntry<>("identifier",            "identifiers"),
            new SimpleEntry<>("citedResponsibleParty", "citedResponsibleParties"),
            new SimpleEntry<>("presentationForm",      "presentationForms"),
            new SimpleEntry<>("series",                "series"),
            new SimpleEntry<>("otherCitationDetails",  "otherCitationDetails"),
            new SimpleEntry<>("collectiveTitle",       "collectiveTitle"),
            new SimpleEntry<>("ISBN",                  "ISBN"),
            new SimpleEntry<>("ISSN",                  "ISSN")
        }, map.entrySet().toArray());

        assertEquals("alternateTitles", map.get("alternateTitle"));
        assertNull("Shall not exists.", map.get("dummy"));
    }

    /**
     * Tests the formatting of sentences.
     */
    @Test
    public void testSentences() {
        final Map<String,String> map, expected = new HashMap<>();
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
     * {@section Explicit calls to <code>String.intern()</code>}
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
