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
import java.util.AbstractMap;
import java.util.Date;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Series;
import org.opengis.util.InternationalString;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link TypeMap} class on instances created by
 * {@link MetadataStandard#asTypeMap(Class, KeyNamePolicy, TypeValuePolicy)}.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(PropertyAccessorTest.class)
public final strictfp class TypeMapTest extends TestCase {
    /**
     * Tests the {@link MetadataStandard#asType(Class, KeyNamePolicy, TypeValuePolicy)} implementation.
     * The properties used in this test are listed in {@link PropertyAccessorTest#testConstructor()}.
     *
     * @see PropertyAccessorTest#testConstructor()
     */
    @Test
    public void testEntrySet() {
        final Map<String,Class<?>> map = MetadataStandard.ISO_19115.asTypeMap(
                Citation.class, KeyNamePolicy.UML_IDENTIFIER, TypeValuePolicy.ELEMENT_TYPE);
        assertArrayEquals(new Object[] {
            new AbstractMap.SimpleEntry<>("title",                 InternationalString.class),
            new AbstractMap.SimpleEntry<>("alternateTitle",        InternationalString.class),
            new AbstractMap.SimpleEntry<>("date",                  CitationDate.class),
            new AbstractMap.SimpleEntry<>("edition",               InternationalString.class),
            new AbstractMap.SimpleEntry<>("editionDate",           Date.class),
            new AbstractMap.SimpleEntry<>("identifier",            Identifier.class),
            new AbstractMap.SimpleEntry<>("citedResponsibleParty", ResponsibleParty.class),
            new AbstractMap.SimpleEntry<>("presentationForm",      PresentationForm.class),
            new AbstractMap.SimpleEntry<>("series",                Series.class),
            new AbstractMap.SimpleEntry<>("otherCitationDetails",  InternationalString.class),
            new AbstractMap.SimpleEntry<>("collectiveTitle",       InternationalString.class),
            new AbstractMap.SimpleEntry<>("ISBN",                  String.class),
            new AbstractMap.SimpleEntry<>("ISSN",                  String.class)
        }, map.entrySet().toArray());

        assertEquals(InternationalString.class, map.get("alternateTitle"));
        assertNull("Shall not exists.", map.get("dummy"));
    }
}
