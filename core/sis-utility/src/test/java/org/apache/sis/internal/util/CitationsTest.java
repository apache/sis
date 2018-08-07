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
package org.apache.sis.internal.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the internal {@link Citations} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.6
 * @module
 */
public final strictfp class CitationsTest extends TestCase {
    /**
     * Creates a citation with the given title and the given identifiers.
     */
     private static CitationMock citation(final String title, final Identifier... identifiers) {
        CitationMock cit =  new CitationMock(title, null, null);
        cit.identifiers = Arrays.asList(identifiers);
        return cit;
    }

    /**
     * Creates an identifier with a code space.
     */
     private static Identifier identifier(final String codeSpace, final String code) {
        return new CitationMock(null, codeSpace, code);
    }

    /**
     * Tests {@link Citations#hasCommonIdentifier(Iterable, Iterable)}.
     */
    @Test
    public void testHasCommonIdentifier() {
        final List<Identifier> id1 = new ArrayList<>(3);
        final List<Identifier> id2 = new ArrayList<>(2);
        assertNull(Citations.hasCommonIdentifier(id1, id2));
        /*
         * Add codes for two Operation Methods which are implemented in Apache SIS by the same class:
         *
         *  - EPSG:9804  —  "Mercator (variant A)" (formerly known as "Mercator (1SP)").
         *  - EPSG:1026  —  "Mercator (Spherical)"
         *  - GeoTIFF:7  —  "CT_Mercator"
         */
        id1.add(identifier("EPSG", "9804"));
        id1.add(identifier("EPSG", "1026"));
        id1.add(identifier("GeoTIFF", "7"));
        assertNull(Citations.hasCommonIdentifier(id1, id2));
        /*
         * EPSG:9841 is a legacy (now deprecated) code for "Mercator (1SP)".
         * We could have declared it as a deprecated code in the above list,
         * but for the sake of this test we do not.
         */
        id2.add(identifier("EPSG", "9841"));
        assertEquals(Boolean.FALSE, Citations.hasCommonIdentifier(id1, id2));
        id2.add(identifier("EPSG", "9804"));
        assertEquals(Boolean.TRUE, Citations.hasCommonIdentifier(id1, id2));
    }

    /**
     * Tests {@link Citations#getIdentifier(Citation, boolean)}.
     */
    @Test
    public void testGetIdentifier() {
        CitationMock citation = new CitationMock(" Not an identifier ", null, null);
        assertEquals("Not an identifier", Citations.getIdentifier(citation, false));
        assertNull(Citations.getIdentifier(citation, true));

        citation = new CitationMock(" ValidIdentifier ", null, null);
        assertEquals("ValidIdentifier", Citations.getIdentifier(citation, false));
        assertEquals("ValidIdentifier", Citations.getIdentifier(citation, true));
        /*
         * Following test uses '-' in the first identifier, which is an invalid Unicode identifier part.
         * Consequently the identifier that we get depends on whether we ask for strict Unicode or not.
         */
        citation = citation("Web Map Server", identifier("OGC", "06-042"), identifier("ISO", "19128"));
        assertEquals("OGC:06-042", Citations.getIdentifier(citation, false));
        assertEquals("ISO_19128",  Citations.getIdentifier(citation, true));
    }

    /**
     * Tests {@link Citations#identifierMatches(Citation, Identifier, CharSequence)}.
     */
    @Test
    public void testIdentifierMatches() {
        final Identifier ogc = identifier("OGC", "06-042");
        final Identifier iso = identifier("ISO", "19128");
        final Citation citation = citation("Web Map Server", ogc, iso, identifier("Foo", "06-042"));
        assertTrue ("With full identifier",  Citations.identifierMatches(citation, ogc, ogc.getCode()));
        assertTrue ("With full identifier",  Citations.identifierMatches(citation, iso, iso.getCode()));
        assertFalse("With wrong code",       Citations.identifierMatches(citation, identifier("ISO", "19115"), "19115"));
        assertFalse("With wrong code space", Citations.identifierMatches(citation, identifier("Foo", "19128"), "19128"));
        assertFalse("With wrong code",       Citations.identifierMatches(citation, null, "Foo"));
        assertTrue ("Without identifier",    Citations.identifierMatches(citation, null, "19128"));
        assertTrue ("With parsing",          Citations.identifierMatches(citation, null, "ISO:19128"));
        assertFalse("With wrong code space", Citations.identifierMatches(citation, null, "Foo:19128"));
    }
}
