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
import jakarta.xml.bind.JAXBException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.metadata.simple.SimpleCitation;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.pending.jdk.JDK19;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertTitleEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.metadata.Identifier.*;


/**
 * Tests {@link ImmutableIdentifier}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ImmutableIdentifierTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ImmutableIdentifierTest() {
    }

    /**
     * Returns the properties map to be used in argument to test methods.
     */
    private static Map<String,Object> properties() {
        final var properties = new HashMap<String,Object>();
        assertNull(properties.put(CODE_KEY,            "This is a code"));
        assertNull(properties.put(AUTHORITY_KEY,       "This is an authority"));
        assertNull(properties.put(VERSION_KEY,         "This is a version"));
        assertNull(properties.put("dummy",             "Doesn't matter"));
        assertNull(properties.put("description",       "There is a description"));
        assertNull(properties.put("description_fr",    "Voici une description"));
        assertNull(properties.put("description_fr_CA", "Pareil"));
        return properties;
    }

    /**
     * Tests the constructor with only {@link String} values, including a localized description.
     */
    @Test
    public void testConstructorWithStringValues() {
        final Map<String,Object> properties = properties();
        final var identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals     ("This is a code",         identifier.getCode());
        assertNull       (                          identifier.getCodeSpace());
        assertTitleEquals("This is an authority",   identifier.getAuthority(), AUTHORITY_KEY);
        assertEquals     ("This is a version",      identifier.getVersion());
        assertEquals     ("There is a description", identifier.getDescription().toString(Locale.ENGLISH));
        assertEquals     ("Voici une description",  identifier.getDescription().toString(Locale.FRENCH));
        assertEquals     ("Pareil",                 identifier.getDescription().toString(Locale.CANADA_FRENCH));
        assertEquals     ("Voici une description",  identifier.getDescription().toString(JDK19.localeOf("fr", "BE")));
    }

    /**
     * Tests the constructor with the {@code "description"} attribute as a {@link SimpleInternationalString}.
     */
    @Test
    public void testConstructorWithInternationalString() {
        final Map<String,Object> properties = properties();
        assertNotNull(properties.put("description", new SimpleInternationalString("Overwritten description")));
        final var identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals     ("This is a code",          identifier.getCode());
        assertNull       (                           identifier.getCodeSpace());
        assertTitleEquals("This is an authority",    identifier.getAuthority(), AUTHORITY_KEY);
        assertEquals     ("This is a version",       identifier.getVersion());
        assertEquals     ("Overwritten description", identifier.getDescription().toString(Locale.ENGLISH));
        assertEquals     ("Voici une description",   identifier.getDescription().toString(Locale.FRENCH));
        assertEquals     ("Pareil",                  identifier.getDescription().toString(Locale.CANADA_FRENCH));
    }

    /**
     * Tests the constructor with the {@code "authority"} attribute as a {@link DefaultCitation}.
     */
    @Test
    public void testConstructorWithCitation() {
        final Map<String,Object> properties = properties();
        assertNotNull(properties.put(AUTHORITY_KEY, new DefaultCitation("Another authority")));
        final var identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals     ("This is a code",         identifier.getCode());
        assertNull       (                          identifier.getCodeSpace());
        assertTitleEquals("Another authority",      identifier.getAuthority(), AUTHORITY_KEY);
        assertEquals     ("This is a version",      identifier.getVersion());
        assertEquals     ("There is a description", identifier.getDescription().toString(Locale.ENGLISH));
        assertEquals     ("Voici une description",  identifier.getDescription().toString(Locale.FRENCH));
        assertEquals     ("Pareil",                 identifier.getDescription().toString(Locale.CANADA_FRENCH));
    }

    /**
     * Tests the constructor with the {@code "authority"} attribute as one of the predefined constants.
     *
     * @see Citations#fromName(String)
     */
    @Test
    public void testPredefinedCitation() {
        final Map<String,Object> properties = properties();
        assertNotNull(properties.put(AUTHORITY_KEY, Constants.EPSG));
        final var identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals("This is a code",         identifier.getCode());
        assertSame  (Citations.EPSG,           identifier.getAuthority());
        assertEquals(Constants.EPSG,           identifier.getCodeSpace());    // Inferred from authority.
        assertEquals("This is a version",      identifier.getVersion());
        assertEquals("There is a description", identifier.getDescription().toString(Locale.ENGLISH));
        assertEquals("Voici une description",  identifier.getDescription().toString(Locale.FRENCH));
        assertEquals("Pareil",                 identifier.getDescription().toString(Locale.CANADA_FRENCH));
    }

    /**
     * Tests the constructor with an argument of the wrong type.
     */
    @Test
    public void testConstructorWithWrongType() {
        final Map<String,Object> properties = properties();
        assertNotNull(properties.put(AUTHORITY_KEY, Locale.CANADA));
        var e = assertThrows(IllegalArgumentException.class, () -> new ImmutableIdentifier(properties));
        assertMessageContains(e, AUTHORITY_KEY);
    }

    /**
     * Test XML marshalling.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    @Disabled("To be replaced by GML")
    public void testMarshal() throws JAXBException {
        final var identifier = new ImmutableIdentifier(new DefaultCitation("EPSG"), null, "4326");
        assertXmlEquals(
                "<gmd:RS_Identifier xmlns:gmd=\"" + LegacyNamespaces.GMD + "\" " +
                               "xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
                "  <gmd:authority>\n" +
                "    <gmd:CI_Citation>\n" +
                "      <gmd:title>\n" +
                "        <gco:CharacterString>EPSG</gco:CharacterString>\n" +
                "      </gmd:title>\n" +
                "    </gmd:CI_Citation>\n" +
                "  </gmd:authority>\n" +
                "  <gmd:code>\n" +
                "    <gco:CharacterString>4326</gco:CharacterString>\n" +
                "  </gmd:code>\n" +
                "</gmd:RS_Identifier>",
                marshal(identifier, VERSION_2007), "xmlns:*");
    }

    /**
     * Tests WKT formatting.
     */
    @Test
    public void testWKT() {
        var id = new ImmutableIdentifier(Citations.EPSG, "EPSG", "4326", "8.2", null);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "Id[“EPSG”, 4326, “8.2”]", id);
        assertWktEquals(Convention.WKT2, "ID[“EPSG”, 4326, “8.2”]", id);
        assertWktEquals(Convention.WKT1, "AUTHORITY[“EPSG”, “4326”]", id);
        /*
         * Same identifier, but with an authority different than the EPSG one.
         * The Citation element should then be visible in WKT 2.
         */
        id = new ImmutableIdentifier(new SimpleCitation("IOGP"), "EPSG", "4326", "8.2", null);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "Id[“EPSG”, 4326, “8.2”, Citation[“IOGP”]]", id);
        assertWktEquals(Convention.WKT2, "ID[“EPSG”, 4326, “8.2”, CITATION[“IOGP”]]", id);
        assertWktEquals(Convention.WKT1, "AUTHORITY[“EPSG”, “4326”]", id);
    }
}
