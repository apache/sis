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

import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.lang.reflect.Field;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.internal.CitationConstant;
import org.apache.sis.metadata.simple.SimpleCitation;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.xml.IdentifierSpace;
import static org.apache.sis.metadata.iso.citation.Citations.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertPartyNameEquals;
import static org.apache.sis.test.Assertions.assertTitleEquals;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Tests {@link Citations}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class CitationsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CitationsTest() {
    }

    /**
     * Verifies that {@link Citations#values()} is complete by comparing with the list
     * of constants given by reflection.
     *
     * @throws IllegalAccessException should never happen since we asked only for public fields.
     */
    @Test
    public void verifyValues() throws IllegalAccessException {
        final Set<Citation> citations = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final Citation c : Citations.values()) {
            final String name = ((CitationConstant) c).title;
            assertTrue(citations.add(c), name);                             // Fail if duplicated instances.
        }
        for (final Field field : Citations.class.getFields()) {
            final String name  = field.getName();
            final Object value = field.get(null);
            if (Citation.class.isAssignableFrom(field.getType())) {
                assertTrue(citations.remove((Citation) value), name);       // Fail if that instance is missing.
            } else for (final Object element : (List<?>) value) {
                assertTrue(citations.remove((Citation) element), name);     // Fail if that instance is missing.
            }
        }
        assertTrue(citations.isEmpty());
    }

    /**
     * Tests {@link Citations#fromName(String)}.
     *
     * @throws IllegalAccessException should never happen since we asked only for public fields.
     */
    @Test
    public void testFromName() throws IllegalAccessException {
        assertSame(SIS,              fromName(Constants.SIS));
        assertSame(OGC,              fromName(Constants.OGC));   // Success of this test is important for remaining of SIS.
        assertSame(EPSG,             fromName(Constants.EPSG));  // Success of this test is important for remaining of SIS.
        assertSame(IOGP,             fromName(Constants.IOGP));
        assertSame(IOGP,             fromName("OGP"));
        assertSame(ESRI,             fromName(Constants.ESRI));  // Handled in a way very similar to "OGC".
        assertSame(NETCDF,           fromName(Constants.NETCDF));
        assertSame(GEOTIFF,          fromName(Constants.GEOTIFF));
        assertSame(PROJ4,            fromName("Proj.4"));
        assertSame(PROJ4,            fromName("Proj4"));
        assertSame(MAP_INFO,         fromName("MapInfo"));
        assertSame(S57,              fromName("S-57"));
        assertSame(S57,              fromName("S57"));
        assertSame(ISBN,             fromName("ISBN"));
        assertSame(ISSN,             fromName("ISSN"));
        assertSame(ISO_19115.get(0), fromName("ISO 19115-1"));
        assertSame(ISO_19115.get(1), fromName("ISO 19115-2"));
        assertSame(WMS,              fromName("WMS"));
        assertSame(WMS,              fromName(Constants.CRS));
        /*
         * Verify again, but using reflection for making sure that the field names
         * are consistent and that we did not forgot any citation constant.
         */
        for (final Field field : Citations.class.getFields()) {
            if (Citation.class.isAssignableFrom(field.getType())) {
                final String name = field.getName();
                assertSame(field.get(null), Citations.fromName(name), name);
            }
        }
    }

    /**
     * Tests {@link Citations#getIdentifier(Citation)} on the constants declared in the {@link Citations} class.
     * The values do not need to be valid Unicode identifiers.
     */
    @Test
    public void testGetIdentifier() {
        assertEquals("SIS",         getIdentifier(SIS));
        assertEquals("OGC",         getIdentifier(OGC));
        assertEquals("IOGP",        getIdentifier(IOGP));
        assertEquals("EPSG",        getIdentifier(EPSG));
        assertEquals("ArcGIS",      getIdentifier(ESRI));
        assertEquals("NetCDF",      getIdentifier(NETCDF));
        assertEquals("GeoTIFF",     getIdentifier(GEOTIFF));
        assertEquals("ISBN",        getIdentifier(ISBN));
        assertEquals("ISSN",        getIdentifier(ISSN));
        assertEquals("PROJ",        getIdentifier(PROJ4));              // Not a valid Unicode identifier.
        assertEquals("S-57",        getIdentifier(S57));                // Not a valid Unicode identifier.
        assertEquals("19115-1",     getIdentifier(ISO_19115.get(0)));   // The ':' separator is not usual in ISO references
        assertEquals("19115-2",     getIdentifier(ISO_19115.get(1)));   // and could be changed in future SIS versions.
        assertEquals("WMS",         getIdentifier(WMS));
    }

    /**
     * Tests {@link Citations#toCodeSpace(Citation)} on the constants
     * declared in the {@link Citations} class.
     */
    @Test
    public void testToCodeSpaceFromConstant() {
        assertEquals("SIS",         toCodeSpace(SIS));
        assertEquals("OGC",         toCodeSpace(WMS));
        assertEquals("OGC",         toCodeSpace(OGC));
        assertEquals("IOGP",        toCodeSpace(IOGP));
        assertEquals("EPSG",        toCodeSpace(EPSG));
        assertEquals("ESRI",        toCodeSpace(ESRI));
        assertEquals("NetCDF",      toCodeSpace(NETCDF));
        assertEquals("GeoTIFF",     toCodeSpace(GEOTIFF));
        assertEquals("MapInfo",     toCodeSpace(MAP_INFO));
        assertEquals("ISBN",        toCodeSpace(ISBN));
        assertEquals("ISSN",        toCodeSpace(ISSN));
        assertEquals("Proj4",       toCodeSpace(PROJ4));
        assertEquals("S57",         toCodeSpace(S57));
        assertNull  (               toCodeSpace(ISO_19115.get(0)));
        assertNull  (               toCodeSpace(ISO_19115.get(1)));
    }

    /**
     * Tests {@link Citations#toCodeSpace(Citation)} with some ignorable characters.
     * Ignorable character used in this test are:
     *
     * <ul>
     *   <li>200B: zero width space</li>
     *   <li>2060: word joiner</li>
     * </ul>
     */
    @Test
    public void testToCodeSpace() {
        final var citation = new SimpleCitation(" Valid\u2060Id\u200Bentifier ");
        assertEquals("ValidIdentifier", Citations.toCodeSpace(citation));

        assertNull(Citations.toCodeSpace(new SimpleCitation("Proj.4")),
                   "Shall not be taken as a valid identifier.");
        assertEquals("TheProj4Space", Citations.toCodeSpace(new Proj4()),
                     "Shall fallback on the identifier space name.");
    }

    /**
     * A citation which is also an {@link IdentifierSpace}, for {@link #testToCodeSpace()} purpose.
     */
    @SuppressWarnings("serial")
    private static final class Proj4 extends SimpleCitation implements IdentifierSpace<Integer> {
        Proj4() {
            super("Proj.4");
        }

        @Override
        public String getName() {
            return "TheProj4Space";         // Intentionally a very different name than "Proj4".
        }
    }

    /**
     * Tests {@code getTitle()} on some {@code Citation} constants.
     */
    @Test
    public void testGetTitles() {
        assertTitleEquals("Apache Spatial Information System",                      SIS, "SIS");
        assertTitleEquals("Web Map Server",                                         WMS, "WMS");
        assertTitleEquals("OGC Naming Authority",                                   OGC, "OGC");
        assertTitleEquals("EPSG Geodetic Parameter Dataset",                        EPSG, "EPSG");
        assertTitleEquals("International Standard Book Number",                     ISBN, "ISBN");
        assertTitleEquals("International Standard Serial Number",                   ISSN, "ISSN");
        assertTitleEquals("GeoTIFF Coverage Encoding Profile",                      GEOTIFF, "GEOTIFF");
        assertTitleEquals("NetCDF Classic and 64-bit Offset Format",                NETCDF, "NETCDF");
        assertTitleEquals("PROJ coordinate transformation software library",        PROJ4, "PROJ4");
        assertTitleEquals("IHO transfer standard for digital hydrographic data",    S57, "S57");
        assertTitleEquals("Geographic Information — Metadata Part 1: Fundamentals", ISO_19115.get(0), "ISO_19115");
        assertTitleEquals("Geographic Information — Metadata Part 2: Extensions for imagery and gridded data", ISO_19115.get(1), "ISO_19115");
    }

    /**
     * Tests {@code getCitedResponsibleParties()} on some {@code Citation} constants.
     */
    @Test
    @org.junit.jupiter.api.Disabled("Requires GeoAPI 3.1.")
    public void testGetCitedResponsibleParty() {
        assertPartyNameEquals("Open Geospatial Consortium",                       OGC, "OGC");
        assertPartyNameEquals("International Organization for Standardization",   ISO_19115.get(0), "ISO_19115");
        assertPartyNameEquals("International Organization for Standardization",   ISO_19115.get(1), "ISO_19115");
        assertPartyNameEquals("International Association of Oil & Gas producers", EPSG, "EPSG");
        assertPartyNameEquals("International Association of Oil & Gas producers", IOGP, "IOGP");
    }

    /**
     * Special tests dedicated to the {@link Citations#EPSG} constant. This is maybe the most important
     * citation declared in the {@link Citations} class, since it is declared as the authority of almost
     * all Coordinate Reference System (CRS) objects typically used by SIS.
     *
     * <p>Apache SIS identifies the EPSG authority with {@link Identifier} {@code "IOGP:EPSG"}.</p>
     */
    @Test
    public void testEPSG() {
        final Identifier identifier = assertSingleton(EPSG.getIdentifiers());
        assertEquals("EPSG", toCodeSpace(EPSG));
//      assertEquals("IOGP", identifier.getCodeSpace());
        assertEquals("EPSG", identifier.getCode());
    }

    /**
     * Verifies that citation constants are unmodifiable.
     */
    @Test
    public void ensureUnmodifiable() {
        final Collection<? extends Identifier> identifiers = Citations.EPSG.getIdentifiers();
        assertNotNull(identifiers);
        var e = assertThrows(UnsupportedOperationException.class, () -> identifiers.add(null),
                             "Predefined metadata shall be unmodifiable.");
        assertNotNull(e);
    }

    /**
     * Test serialization.
     *
     * @throws IllegalAccessException should never happen since we asked only for public fields.
     */
    @Test
    public void testSerialization() throws IllegalAccessException {
        for (final Field field : Citations.class.getDeclaredFields()) {
            if (CitationConstant.class.isAssignableFrom(field.getType())) {
                final Object c = field.get(null);
                assertSame(c, assertSerializedEquals(c), field.getName());
            }
        }
    }

    /**
     * Tests {@link Citations#identifierMatches(Citation, Identifier, String)}.
     */
    @Test
    public void testIdentifierMatches() {
        final var ogc = new Id("OGC", "06-042");
        final var iso = new Id("ISO", "19128");
        final var citation = new DefaultCitation("Web Map Server");
        citation.setIdentifiers(List.of(ogc, iso, new DefaultIdentifier("Foo", "06-042", null)));
        assertTrue (/* With full identifier */ Citations.identifierMatches(citation, ogc, ogc.getCode()));
        assertTrue (/* With full identifier */ Citations.identifierMatches(citation, iso, iso.getCode()));
        assertFalse(/* With wrong code      */ Citations.identifierMatches(citation, new Id("ISO", "19115"), "19115"));
        assertFalse(/* With wrong codespace */ Citations.identifierMatches(citation, new Id("Foo", "19128"), "19128"));
        assertFalse(/* With wrong code      */ Citations.identifierMatches(citation, "Foo"));
        assertTrue (/* Without identifier   */ Citations.identifierMatches(citation, "19128"));
        assertTrue (/* With parsing         */ Citations.identifierMatches(citation, "ISO:19128"));
        assertFalse(/* With wrong codespace */ Citations.identifierMatches(citation, "Foo:19128"));
    }

    @SuppressWarnings("serial")
    private static final class Id extends DefaultIdentifier implements ReferenceIdentifier {
        Id(String codeSpace, String code) {
            super(codeSpace, code, null);
        }
    }
}
