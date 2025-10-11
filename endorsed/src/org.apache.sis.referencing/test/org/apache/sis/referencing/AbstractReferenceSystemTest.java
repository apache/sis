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

import java.util.HashMap;
import java.util.Locale;
import java.time.LocalDate;
import org.opengis.util.InternationalString;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.mock.VerticalCRSMock;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.referencing.Assertions.assertRemarksEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.referencing.IdentifiedObject.*;
import static org.opengis.referencing.ObjectDomain.*;


/**
 * Tests the {@link AbstractReferenceSystem} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings("exports")
public final class AbstractReferenceSystemTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AbstractReferenceSystemTest() {
    }

    /**
     * Tests {@link AbstractReferenceSystem}.
     */
    @Test
    public void testCreateFromMap() {
        final var properties = new HashMap<String,Object>();
        assertNull(properties.put("name",       "This is a name"));
        assertNull(properties.put("scope",      "This is a scope"));
        assertNull(properties.put("scope_fr",   "Valide dans ce domaine"));
        assertNull(properties.put("remarks",    "There is remarks"));
        assertNull(properties.put("remarks_fr", "Voici des remarques"));

        final var reference = new AbstractReferenceSystem(properties);
        Validators.validate(reference);

        final InternationalString scope = assertSingleton(reference.getDomains()).getScope();

        assertEquals("This is a name",         reference.getName().getCode());
        assertEquals("This is a scope",        scope.toString(Locale.ROOT));
        assertEquals("Valide dans ce domaine", scope.toString(Locale.FRENCH));
        assertRemarksEquals("There is remarks",    reference, Locale.ENGLISH);
        assertRemarksEquals("Voici des remarques", reference, Locale.FRENCH);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final var properties = new HashMap<String,Object>(8);
        assertNull(properties.put("code",       "4326"));
        assertNull(properties.put("codeSpace",  "EPSG"));
        assertNull(properties.put("scope",      "This is a scope"));
        assertNull(properties.put("remarks",    "There is remarks"));
        assertNull(properties.put("remarks_fr", "Voici des remarques"));

        final var object = new AbstractReferenceSystem(properties);
        Validators.validate(object);

        assertNotSame(object, assertSerializedEquals(object));
    }

    /**
     * Tests WKT formatting with a name that contains the quote character and optional information.
     * We test that the closing quote character is doubled and the optional information properly formatted.
     */
    @Test
    public void testWKT() {
        final var properties = new HashMap<String,Object>(8);
        assertNull(properties.put(NAME_KEY, "My “object”."));
        assertNull(properties.put(SCOPE_KEY, "Large scale topographic mapping and cadastre."));
        assertNull(properties.put(REMARKS_KEY, "注です。"));
        assertNull(properties.put(IDENTIFIERS_KEY, new ImmutableIdentifier(
                Citations.EPSG, "EPSG", "4326", "8.2", null)));

        assertNull(properties.put(DOMAIN_OF_VALIDITY_KEY, new DefaultExtent("Netherlands offshore.",
                new DefaultGeographicBoundingBox(2.54, 6.40, 51.43, 55.77),
                new DefaultVerticalExtent(10, 1000, VerticalCRSMock.DEPTH),
                new DefaultTemporalExtent(LocalDate.of(2010, 4, 5),
                                          LocalDate.of(2010, 9, 8)))));

        final var object = new AbstractReferenceSystem(properties);

        assertTrue(object.toString(Convention.WKT1).startsWith(
                "ReferenceSystem[\"My “object”.\", AUTHORITY[\"EPSG\", \"4326\"]]"));

        assertWktEquals(Convention.WKT1,
                "ReferenceSystem[“My \"object\".”, AUTHORITY[“EPSG”, “4326”]]",
                object);

        assertWktEquals(Convention.WKT2_2015,
                "ReferenceSystem[“My \"object\".”,\n" +     // Quotes replaced
                "  SCOPE[“Large scale topographic mapping and cadastre.”],\n" +
                "  AREA[“Netherlands offshore.”],\n" +
                "  BBOX[51.43, 2.54, 55.77, 6.40],\n" +
                "  VERTICALEXTENT[-1000, -10, LENGTHUNIT[“metre”, 1]],\n" +
                "  TIMEEXTENT[2010-04-05, 2010-09-08],\n" +
                "  ID[“EPSG”, 4326, “8.2”, URI[“urn:ogc:def:referenceSystem:EPSG:8.2:4326”]],\n" +
                "  REMARK[“注です。”]]",
                object);

        assertWktEquals(Convention.WKT2_2019,
                "ReferenceSystem[“My \"object\".”,\n" +     // Quotes replaced
                "  USAGE[\n" +
                "    SCOPE[“Large scale topographic mapping and cadastre.”],\n" +
                "    AREA[“Netherlands offshore.”],\n" +
                "    BBOX[51.43, 2.54, 55.77, 6.40],\n" +
                "    VERTICALEXTENT[-1000, -10, LENGTHUNIT[“metre”, 1]],\n" +
                "    TIMEEXTENT[2010-04-05, 2010-09-08]],\n" +
                "  ID[“EPSG”, 4326, “8.2”, URI[“urn:ogc:def:referenceSystem:EPSG:8.2:4326”]],\n" +
                "  REMARK[“注です。”]]",
                object);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ReferenceSystem[“My \"object\".”,\n" +
                "  Usage[\n" +
                "    Scope[“Large scale topographic mapping and cadastre.”],\n" +
                "    Area[“Netherlands offshore.”],\n" +
                "    BBox[51.43, 2.54, 55.77, 6.40],\n" +
                "    VerticalExtent[-1000, -10],\n" +
                "    TimeExtent[2010-04-05, 2010-09-08]],\n" +
                "  Id[“EPSG”, 4326, “8.2”, URI[“urn:ogc:def:referenceSystem:EPSG:8.2:4326”]],\n" +
                "  Remark[“注です。”]]",
                object);

        assertWktEquals(Convention.INTERNAL,
                "ReferenceSystem[“My “object””.”,\n" +  // Quote doubled
                "  Usage[\n" +
                "    Scope[“Large scale topographic mapping and cadastre.”],\n" +
                "    Area[“Netherlands offshore.”],\n" +
                "    BBox[51.43, 2.54, 55.77, 6.40],\n" +
                "    VerticalExtent[-1000, -10],\n" +
                "    TimeExtent[2010-04-05, 2010-09-08]],\n" +
                "  Id[“EPSG”, 4326, “8.2”],\n" +
                "  Remark[“注です。”]]",
                object);
    }
}
