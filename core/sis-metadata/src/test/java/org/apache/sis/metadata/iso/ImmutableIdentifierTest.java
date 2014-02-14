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
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.util.iso.SimpleInternationalString;
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
 * @since   0.3 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
@DependsOn(DefaultIdentifierTest.class)
public final strictfp class ImmutableIdentifierTest extends TestCase {
    /**
     * Returns the properties map to be used in argument to test methods.
     */
    private static Map<String,Object> properties() {
        final Map<String,Object> properties = new HashMap<>();
        assertNull(properties.put(CODE_KEY,        "This is a code"));
        assertNull(properties.put(AUTHORITY_KEY,   "This is an authority"));
        assertNull(properties.put(VERSION_KEY,     "This is a version"));
        assertNull(properties.put("dummy",         "Doesn't matter"));
        assertNull(properties.put("remarks",       "There is remarks"));
        assertNull(properties.put("remarks_fr",    "Voici des remarques"));
        assertNull(properties.put("remarks_fr_CA", "Pareil"));
        return properties;
    }

    /**
     * Tests the constructor with only {@link String} values, including localized remarks.
     */
    @Test
    public void testConstructorWithStringValues() {
        final Map<String,Object> properties = properties();
        final ImmutableIdentifier identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals(CODE_KEY,        "This is a code",       identifier.getCode());
        assertNull  (CODESPACE_KEY,                           identifier.getCodeSpace());
        assertEquals(AUTHORITY_KEY,   "This is an authority", identifier.getAuthority().getTitle().toString());
        assertEquals(VERSION_KEY,     "This is a version",    identifier.getVersion());
        assertEquals("remarks",       "There is remarks",     identifier.getRemarks().toString(Locale.ENGLISH));
        assertEquals("remarks_fr",    "Voici des remarques",  identifier.getRemarks().toString(Locale.FRENCH));
        assertEquals("remarks_fr_CA", "Pareil",               identifier.getRemarks().toString(Locale.CANADA_FRENCH));
        assertEquals("remarks_fr_BE", "Voici des remarques",  identifier.getRemarks().toString(new Locale("fr", "BE")));
    }

    /**
     * Tests the constructor with the {@code "remarks"} attribute as a {@link SimpleInternationalString}.
     */
    @Test
    @DependsOnMethod("testConstructorWithStringValues")
    public void testConstructorWithInternationalString() {
        final Map<String,Object> properties = properties();
        assertNotNull(properties.put("remarks", new SimpleInternationalString("Overwritten remarks")));
        final ImmutableIdentifier identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals(CODE_KEY,        "This is a code",       identifier.getCode());
        assertNull  (CODESPACE_KEY,                           identifier.getCodeSpace());
        assertEquals(AUTHORITY_KEY,   "This is an authority", identifier.getAuthority().getTitle().toString());
        assertEquals(VERSION_KEY,     "This is a version",    identifier.getVersion());
        assertEquals("remarks",       "Overwritten remarks",  identifier.getRemarks().toString(Locale.ENGLISH));
        assertEquals("remarks_fr",    "Voici des remarques",  identifier.getRemarks().toString(Locale.FRENCH));
        assertEquals("remarks_fr_CA", "Pareil",               identifier.getRemarks().toString(Locale.CANADA_FRENCH));
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

        assertEquals(CODE_KEY,        "This is a code",       identifier.getCode());
        assertNull  (CODESPACE_KEY,                           identifier.getCodeSpace());
        assertEquals(AUTHORITY_KEY,   "An other authority",   identifier.getAuthority().getTitle().toString());
        assertEquals(VERSION_KEY,     "This is a version",    identifier.getVersion());
        assertEquals("remarks",       "There is remarks",     identifier.getRemarks().toString(Locale.ENGLISH));
        assertEquals("remarks_fr",    "Voici des remarques",  identifier.getRemarks().toString(Locale.FRENCH));
        assertEquals("remarks_fr_CA", "Pareil",               identifier.getRemarks().toString(Locale.CANADA_FRENCH));
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
        assertNotNull(properties.put(AUTHORITY_KEY, "EPSG"));
        final ImmutableIdentifier identifier = new ImmutableIdentifier(properties);
        Validators.validate(identifier);

        assertEquals(CODE_KEY,        "This is a code",       identifier.getCode());
        assertSame  (AUTHORITY_KEY,   Citations.EPSG,         identifier.getAuthority());
        assertEquals(CODESPACE_KEY,   "EPSG",                 identifier.getCodeSpace()); // Inferred from authority.
        assertEquals(VERSION_KEY,     "This is a version",    identifier.getVersion());
        assertEquals("remarks",       "There is remarks",     identifier.getRemarks().toString(Locale.ENGLISH));
        assertEquals("remarks_fr",    "Voici des remarques",  identifier.getRemarks().toString(Locale.FRENCH));
        assertEquals("remarks_fr_CA", "Pareil",               identifier.getRemarks().toString(Locale.CANADA_FRENCH));
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
        final ImmutableIdentifier id = new ImmutableIdentifier(HardCodedCitations.OGP, "EPSG", "4326", "8.2", null);
        assertWktEquals(Convention.WKT2, "Id[“EPSG”, 4326, “8.2”, “OGP”]", id);
        assertWktEquals(Convention.WKT1, "AUTHORITY[“EPSG”, “4326”]", id);
    }
}
