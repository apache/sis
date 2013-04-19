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
package org.apache.sis.metadata.iso.citation;

import java.util.Arrays;
import java.util.Collection;
import org.opengis.metadata.Identifier;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DefaultCitation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.19)
 * @version 0.3
 * @module
 */
public final strictfp class DefaultCitationTest extends TestCase {
    /**
     * Tests the identifier map, which handles ISBN and ISSN codes in a special way.
     */
    @Test
    public void testIdentifierMap() {
        final DefaultCitation citation = new DefaultCitation();
        final Collection<Identifier> identifiers = citation.getIdentifiers();
        final IdentifierMap identifierMap = citation.getIdentifierMap();
        assertTrue("Expected an initially empty set of identifiers.", identifiers.isEmpty());
        /*
         * Set the ISBN code, and ensure that the the ISBN is reflected in the identifier map.
         */
        citation.setISBN("MyISBN");
        assertEquals("MyISBN", citation.getISBN());
        assertEquals("ISBN code shall be included in the set of identifiers.", 1, identifiers.size());
        assertEquals("{ISBN=“MyISBN”}", identifierMap.toString());
        /*
         * Set the identifiers with a list containing ISBN and ISSN codes.
         * The ISBN code shall be ignored because and ISBN property was already set.
         * The ISSN code shall be retained because it is a new code.
         */
        assertNull("ISSN shall be initially null.", citation.getISSN());
        citation.setIdentifiers(Arrays.asList(
                new DefaultIdentifier(HardCodedCitations.OGC,  "MyOGC"),
                new DefaultIdentifier(HardCodedCitations.EPSG, "MyEPSG"),
                new DefaultIdentifier(Citations.ISBN, "MyIgnored"),
                new DefaultIdentifier(Citations.ISSN, "MyISSN")));

        assertEquals("The ISBN value shall not have been overwritten.",    "MyISBN", citation.getISBN());
        assertEquals("The ISSN value shall have been added, because new.", "MyISSN", citation.getISSN());
        assertEquals("{OGC=“MyOGC”, EPSG=“MyEPSG”, ISSN=“MyISSN”, ISBN=“MyISBN”}", identifierMap.toString());
    }
}
