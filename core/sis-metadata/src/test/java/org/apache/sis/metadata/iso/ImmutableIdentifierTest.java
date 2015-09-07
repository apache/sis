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
package org.apache.sis.metadata.iso;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import javax.xml.bind.JAXBException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.opengis.test.Validators;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;
import static org.opengis.referencing.ReferenceIdentifier.*;


/**
 * Tests {@link ImmutableIdentifier}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
@DependsOn({
    DefaultIdentifierTest.class,
    org.apache.sis.metadata.iso.citation.CitationsTest.class
})
public final strictfp class ImmutableIdentifierTest extends TestCase {
    /**
     * Returns the properties map to be used in argument to test methods.
     */
    private static Map<String,Object> properties() {
        final Map<String,Object> properties = new HashMap<String,Object>();
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
        final ImmutableIdentifier identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals     (CODE_KEY,            "This is a code",         identifier.getCode());
        assertNull       (CODESPACE_KEY,                                 identifier.getCodeSpace());
        assertTitleEquals(AUTHORITY_KEY,       "This is an authority",   identifier.getAuthority());
        assertEquals     (VERSION_KEY,         "This is a version",      identifier.getVersion());
        assertEquals     ("description",       "There is a description", identifier.getDescription().toString(Locale.ENGLISH));
        assertEquals     ("description_fr",    "Voici une description",  identifier.getDescription().toString(Locale.FRENCH));
        assertEquals     ("description_fr_CA", "Pareil",                 identifier.getDescription().toString(Locale.CANADA_FRENCH));
        assertEquals     ("description_fr_BE", "Voici une description",  identifier.getDescription().toString(new Locale("fr", "BE")));
    }

    /**
     * Tests the constructor with the {@code "description"} attribute as a {@link SimpleInternationalString}.
     */
    @Test
    @DependsOnMethod("testConstructorWithStringValues")
    public void testConstructorWithInternationalString() {
        final Map<String,Object> properties = properties();
        assertNotNull(properties.put("description", new SimpleInternationalString("Overwritten description")));
        final ImmutableIdentifier identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals     (CODE_KEY,            "This is a code",          identifier.getCode());
        assertNull       (CODESPACE_KEY,                                  identifier.getCodeSpace());
        assertTitleEquals(AUTHORITY_KEY,       "This is an authority",    identifier.getAuthority());
        assertEquals     (VERSION_KEY,         "This is a version",       identifier.getVersion());
        assertEquals     ("description",       "Overwritten description", identifier.getDescription().toString(Locale.ENGLISH));
        assertEquals     ("description_fr",    "Voici une description",   identifier.getDescription().toString(Locale.FRENCH));
        assertEquals     ("description_fr_CA", "Pareil",                  identifier.getDescription().toString(Locale.CANADA_FRENCH));
    }

    /**
     * Tests the constructor with the {@code "authority"} attribute as a {@link DefaultCitation}.
     */
    @Test
    @DependsOnMethod("testConstructorWithStringValues")
    public void testConstructorWithCitation() {
        final Map<String,Object> properties = properties();
        assertNotNull(properties.put(AUTHORITY_KEY, new DefaultCitation("An other authority")));
        final ImmutableIdentifier identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals     (CODE_KEY,            "This is a code",         identifier.getCode());
        assertNull       (CODESPACE_KEY,                                 identifier.getCodeSpace());
        assertTitleEquals(AUTHORITY_KEY,       "An other authority",     identifier.getAuthority());
        assertEquals     (VERSION_KEY,         "This is a version",      identifier.getVersion());
        assertEquals     ("description",       "There is a description", identifier.getDescription().toString(Locale.ENGLISH));
        assertEquals     ("description_fr",    "Voici une description",  identifier.getDescription().toString(Locale.FRENCH));
        assertEquals     ("description_fr_CA", "Pareil",                 identifier.getDescription().toString(Locale.CANADA_FRENCH));
    }

    /**
     * Tests the constructor with the {@code "authority"} attribute as one of the pre-defined constants.
     *
     * @see Citations#fromName(String)
     */
    @Test
    @DependsOnMethod("testConstructorWithStringValues")
    public void testPredefinedCitation() {
        final Map<String,Object> properties = properties();
        assertNotNull(properties.put(AUTHORITY_KEY, Constants.EPSG));
        final ImmutableIdentifier identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals(CODE_KEY,            "This is a code",         identifier.getCode());
        assertSame  (AUTHORITY_KEY,       Citations.EPSG,           identifier.getAuthority());
        assertEquals(CODESPACE_KEY,       Constants.EPSG,           identifier.getCodeSpace()); // Inferred from authority.
        assertEquals(VERSION_KEY,         "This is a version",      identifier.getVersion());
        assertEquals("description",       "There is a description", identifier.getDescription().toString(Locale.ENGLISH));
        assertEquals("description_fr",    "Voici une description",  identifier.getDescription().toString(Locale.FRENCH));
        assertEquals("description_fr_CA", "Pareil",                 identifier.getDescription().toString(Locale.CANADA_FRENCH));
    }

    /**
     * Tests the constructor with an argument of the wrong type.
     */
    @Test
    @DependsOnMethod("testConstructorWithStringValues")
    public void testConstructorWithWrongType() {
        final Map<String,Object> properties = properties();
        assertNotNull(properties.put(AUTHORITY_KEY, Locale.CANADA));
        try {
            final ImmutableIdentifier identifier = new ImmutableIdentifier(properties);
            fail(identifier.toString());
        } catch (IllegalArgumentException e) {
            // This is the expected exception
            final String message = e.getMessage();
            assertTrue(message, message.contains(AUTHORITY_KEY));
        }
    }

    /**
     * Test XML marshalling.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testMarshal() throws JAXBException {
        final ImmutableIdentifier identifier = new ImmutableIdentifier(new DefaultCitation("EPSG"), null, "4326");
        new DefaultIdentifierTest().testMarshal("RS_Identifier", identifier);
    }

    /**
     * Tests WKT formatting.
     */
    @Test
    public void testWKT() {
        ImmutableIdentifier id = new ImmutableIdentifier(Citations.EPSG, "EPSG", "4326", "8.2", null);
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
