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
package org.apache.sis.referencing;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import org.opengis.test.Validators;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.test.mock.VerticalCRSMock;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.referencing.ReferenceSystem.*;
import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.MetadataAssert.assertWktEquals;


/**
 * Tests the {@link AbstractReferenceSystem} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@DependsOn(AbstractIdentifiedObjectTest.class)
public final strictfp class AbstractReferenceSystemTest extends TestCase {
    /**
     * Tests {@link AbstractReferenceSystem}.
     */
    @Test
    public void testCreateFromMap() {
        final Map<String,Object> properties = new HashMap<String,Object>();
        assertNull(properties.put("name",       "This is a name"));
        assertNull(properties.put("scope",      "This is a scope"));
        assertNull(properties.put("scope_fr",   "Valide dans ce domaine"));
        assertNull(properties.put("remarks",    "There is remarks"));
        assertNull(properties.put("remarks_fr", "Voici des remarques"));

        final AbstractReferenceSystem reference = new AbstractReferenceSystem(properties);
        Validators.validate(reference);

        assertEquals("name",       "This is a name",         reference.getName()   .getCode());
        assertEquals("scope",      "This is a scope",        reference.getScope()  .toString(Locale.ROOT));
        assertEquals("scope_fr",   "Valide dans ce domaine", reference.getScope()  .toString(Locale.FRENCH));
        assertEquals("remarks",    "There is remarks",       reference.getRemarks().toString(Locale.ENGLISH));
        assertEquals("remarks_fr", "Voici des remarques",    reference.getRemarks().toString(Locale.FRENCH));
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testCreateFromMap")
    public void testSerialization() {
        final Map<String,Object> properties = new HashMap<String,Object>(8);
        assertNull(properties.put("code",       "4326"));
        assertNull(properties.put("codeSpace",  "EPSG"));
        assertNull(properties.put("scope",      "This is a scope"));
        assertNull(properties.put("remarks",    "There is remarks"));
        assertNull(properties.put("remarks_fr", "Voici des remarques"));

        final AbstractReferenceSystem object = new AbstractReferenceSystem(properties);
        Validators.validate(object);

        assertNotSame(object, assertSerializedEquals(object));
    }

    /**
     * Tests WKT formatting with a name that contains the quote character and optional information.
     * We test that the closing quote character is doubled and the optional information properly formatted.
     */
    @Test
    @DependsOnMethod("testCreateFromMap")
    public void testWKT() {
        final Map<String,Object> properties = new HashMap<String,Object>(8);
        assertNull(properties.put(NAME_KEY, "My “object”."));
        assertNull(properties.put(SCOPE_KEY, "Large scale topographic mapping and cadastre."));
        assertNull(properties.put(REMARKS_KEY, "注です。"));
        assertNull(properties.put(IDENTIFIERS_KEY, new ImmutableIdentifier(
                Citations.EPSG, "EPSG", "4326", "8.2", null)));
        assertNull(properties.put(DOMAIN_OF_VALIDITY_KEY, new DefaultExtent("Netherlands offshore.",
                new DefaultGeographicBoundingBox(2.54, 6.40, 51.43, 55.77),
                new DefaultVerticalExtent(10, 1000, VerticalCRSMock.DEPTH),
                new DefaultTemporalExtent()))); // TODO: needs sis-temporal module for testing that one.
        final AbstractReferenceSystem object = new AbstractReferenceSystem(properties);

        assertTrue(object.toString(Convention.WKT1).startsWith(
                "ReferenceSystem[\"My “object”.\", AUTHORITY[\"EPSG\", \"4326\"]]"));

        assertWktEquals(Convention.WKT1,
                "ReferenceSystem[“My \"object\".”, AUTHORITY[“EPSG”, “4326”]]",
                object);

        assertWktEquals(Convention.WKT2,
                "ReferenceSystem[“My \"object\".”,\n" +     // Quotes replaced
                "  SCOPE[“Large scale topographic mapping and cadastre.”],\n" +
                "  AREA[“Netherlands offshore.”],\n" +
                "  BBOX[51.43, 2.54, 55.77, 6.40],\n" +
                "  VERTICALEXTENT[-1000, -10, LENGTHUNIT[“metre”, 1]],\n" +
                "  ID[“EPSG”, 4326, “8.2”, URI[“urn:ogc:def:referenceSystem:EPSG:8.2:4326”]],\n" +
                "  REMARK[“注です。”]]",
                object);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ReferenceSystem[“My \"object\".”,\n" +
                "  Scope[“Large scale topographic mapping and cadastre.”],\n" +
                "  Area[“Netherlands offshore.”],\n" +
                "  BBox[51.43, 2.54, 55.77, 6.40],\n" +
                "  VerticalExtent[-1000, -10],\n" +
                "  Id[“EPSG”, 4326, “8.2”, URI[“urn:ogc:def:referenceSystem:EPSG:8.2:4326”]],\n" +
                "  Remark[“注です。”]]",
                object);

        assertWktEquals(Convention.INTERNAL,
                "ReferenceSystem[“My “object””.”,\n" +  // Quote doubled
                "  Scope[“Large scale topographic mapping and cadastre.”],\n" +
                "  Area[“Netherlands offshore.”],\n" +
                "  BBox[51.43, 2.54, 55.77, 6.40],\n" +
                "  VerticalExtent[-1000, -10],\n" +
                "  Id[“EPSG”, 4326, “8.2”],\n" +
                "  Remark[“注です。”]]",
                object);
    }
}
