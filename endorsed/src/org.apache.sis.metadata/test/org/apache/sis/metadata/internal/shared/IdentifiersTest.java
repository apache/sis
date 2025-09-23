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
package org.apache.sis.metadata.internal.shared;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import org.opengis.metadata.Identifier;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Tests {@link Identifiers}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @versio 1.0
 */
public final class IdentifiersTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public IdentifiersTest() {
    }

    /**
     * Creates a citation with the given title and the given identifiers.
     */
    private static DefaultCitation citation(final String title, final Identifier... identifiers) {
        DefaultCitation cit =  new DefaultCitation(title);
        cit.setIdentifiers(Arrays.asList(identifiers));
        return cit;
    }

    /**
     * Creates an identifier with a code space.
     */
    private static ReferenceIdentifier identifier(final String codeSpace, final String code) {
        return new Id(codeSpace, code);
    }

    @SuppressWarnings("serial")
    private static final class Id extends DefaultIdentifier implements ReferenceIdentifier {
        Id(String codeSpace, String code) {
            super(codeSpace, code, null);
        }
    }

    /**
     * Tests {@link Identifiers#hasCommonIdentifier(Iterable, Iterable)}.
     */
    @Test
    public void testHasCommonIdentifier() {
        final List<ReferenceIdentifier> id1 = new ArrayList<>(3);
        final List<ReferenceIdentifier> id2 = new ArrayList<>(2);
        assertNull(Identifiers.hasCommonIdentifier(id1, id2));
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
        assertNull(Identifiers.hasCommonIdentifier(id1, id2));
        /*
         * EPSG:9841 is a legacy (now deprecated) code for "Mercator (1SP)".
         * We could have declared it as a deprecated code in the above list,
         * but for the sake of this test we do not.
         */
        id2.add(identifier("EPSG", "9841"));
        assertEquals(Boolean.FALSE, Identifiers.hasCommonIdentifier(id1, id2));
        id2.add(identifier("EPSG", "9804"));
        assertEquals(Boolean.TRUE, Identifiers.hasCommonIdentifier(id1, id2));
    }

    /**
     * Tests {@link Identifiers#getIdentifier(Citation, boolean)}.
     */
    @Test
    public void testGetIdentifier() {
        DefaultCitation citation = new DefaultCitation(" Not an identifier ");
        assertEquals("Not an identifier", Identifiers.getIdentifier(citation, false));
        assertNull(Identifiers.getIdentifier(citation, true));

        citation = new DefaultCitation(" ValidIdentifier ");
        assertEquals("ValidIdentifier", Identifiers.getIdentifier(citation, false));
        assertEquals("ValidIdentifier", Identifiers.getIdentifier(citation, true));
        /*
         * Following test uses '-' in the first identifier, which is an invalid Unicode identifier part.
         * Consequently, the identifier that we get depends on whether we ask for strict Unicode or not.
         */
        citation = citation("Web Map Server", identifier("OGC", "06-042"), identifier("ISO", "19128"));
        assertEquals("OGC:06-042", Identifiers.getIdentifier(citation, false));
        assertEquals("ISO_19128",  Identifiers.getIdentifier(citation, true));
    }
}
