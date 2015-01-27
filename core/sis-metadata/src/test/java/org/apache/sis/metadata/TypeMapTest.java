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
import java.util.Collection;
import java.util.Date;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicDescription;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.extent.AbstractGeographicExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicDescription;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.util.AbstractMap.SimpleEntry;


/**
 * Tests the {@link TypeMap} class on instances created by
 * {@link MetadataStandard#asTypeMap(Class, KeyNamePolicy, TypeValuePolicy)}.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(PropertyAccessorTest.class)
public final strictfp class TypeMapTest extends TestCase {
    /**
     * Tests {@code TypeMap.entrySet()} for an exact match (including iteration order).
     * The properties used in this test are listed in {@link PropertyAccessorTest#testConstructor()}.
     *
     * @see PropertyAccessorTest#testConstructor()
     */
    @Test
    public void testEntrySet() {
        final Map<String,Class<?>> map = MetadataStandard.ISO_19115.asTypeMap(
                Citation.class, KeyNamePolicy.UML_IDENTIFIER, TypeValuePolicy.ELEMENT_TYPE);
        assertArrayEquals(new Object[] {
            new SimpleEntry<String,Class<?>>("title",                 InternationalString.class),
            new SimpleEntry<String,Class<?>>("alternateTitle",        InternationalString.class),
            new SimpleEntry<String,Class<?>>("date",                  CitationDate.class),
            new SimpleEntry<String,Class<?>>("edition",               InternationalString.class),
            new SimpleEntry<String,Class<?>>("editionDate",           Date.class),
            new SimpleEntry<String,Class<?>>("identifier",            Identifier.class),
            new SimpleEntry<String,Class<?>>("citedResponsibleParty", ResponsibleParty.class),
            new SimpleEntry<String,Class<?>>("presentationForm",      PresentationForm.class),
            new SimpleEntry<String,Class<?>>("series",                Series.class),
            new SimpleEntry<String,Class<?>>("otherCitationDetails",  InternationalString.class),
//          new SimpleEntry<String,Class<?>>("collectiveTitle",       InternationalString.class),  -- deprecated as of ISO 19115:2014
            new SimpleEntry<String,Class<?>>("ISBN",                  String.class),
            new SimpleEntry<String,Class<?>>("ISSN",                  String.class),
            new SimpleEntry<String,Class<?>>("graphic",               BrowseGraphic.class),
            new SimpleEntry<String,Class<?>>("onlineResource",        OnlineResource.class)
        }, map.entrySet().toArray());

        assertEquals(InternationalString.class, map.get("alternateTitle"));
        assertNull("Shall not exists.", map.get("dummy"));
    }

    /**
     * Tests {@link TypeMap#get(Object)} on a well known metadata type for various {@link TypeValuePolicy}.
     */
    @Test
    public void testGet() {
        final MetadataStandard standard = MetadataStandard.ISO_19115;
        final KeyNamePolicy keyPolicy = KeyNamePolicy.JAVABEANS_PROPERTY;
        Map<String, Class<?>> types;

        types = standard.asTypeMap(DefaultCitation.class, keyPolicy, TypeValuePolicy.PROPERTY_TYPE);
        assertEquals(InternationalString.class, types.get("title"));
        assertEquals(Collection.class,          types.get("alternateTitles"));

        types = standard.asTypeMap(DefaultCitation.class, keyPolicy, TypeValuePolicy.ELEMENT_TYPE);
        assertEquals(InternationalString.class, types.get("title"));
        assertEquals(InternationalString.class, types.get("alternateTitles"));

        types = standard.asTypeMap(DefaultCitation.class, keyPolicy, TypeValuePolicy.DECLARING_INTERFACE);
        assertEquals(Citation.class, types.get("title"));
        assertEquals(Citation.class, types.get("alternateTitles"));

        types = standard.asTypeMap(DefaultCitation.class, keyPolicy, TypeValuePolicy.DECLARING_CLASS);
        assertEquals(DefaultCitation.class, types.get("title"));
        assertEquals(DefaultCitation.class, types.get("alternateTitles"));

        /*
         * Tests declaring classes/interfaces again, now with metadata having a class hierarchy.
         */
        types = standard.asTypeMap(DefaultGeographicDescription.class, keyPolicy, TypeValuePolicy.DECLARING_INTERFACE);
        assertEquals(GeographicDescription.class, types.get("geographicIdentifier"));
        assertEquals(GeographicExtent.class,      types.get("inclusion"));

        types = standard.asTypeMap(DefaultGeographicDescription.class, keyPolicy, TypeValuePolicy.DECLARING_CLASS);
        assertEquals(DefaultGeographicDescription.class, types.get("geographicIdentifier"));
        assertEquals(AbstractGeographicExtent.class,     types.get("inclusion"));
    }
}
