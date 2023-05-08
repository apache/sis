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
import java.util.Locale;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.lang.reflect.Field;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.internal.simple.CitationConstant;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.metadata.Assertions.assertTitleEquals;
import static org.apache.sis.metadata.iso.citation.Citations.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link Citations}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.6
 */
@DependsOn(org.apache.sis.metadata.sql.MetadataSourceTest.class)
public final class CitationsTest extends TestCase {
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
            assertTrue(name, citations.add(c));                             // Fail if duplicated instances.
        }
        for (final Field field : Citations.class.getFields()) {
            final String name  = field.getName();
            final Object value = field.get(null);
            if (Citation.class.isAssignableFrom(field.getType())) {
                assertTrue(name, citations.remove((Citation) value));       // Fail if that instance is missing.
            } else for (final Object element : (List<?>) value) {
                assertTrue(name, citations.remove((Citation) element));     // Fail if that instance is missing.
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
        assertSame(ESRI,             fromName("ESRI"));          // Handled in a way very similar to "OGC".
        assertSame(NETCDF,           fromName("NetCDF"));
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
                assertSame(name, field.get(null), Citations.fromName(name));
            }
        }
    }

    /**
     * Tests {@link Citations#getIdentifier(Citation)} on the constants declared in the {@link Citations} class.
     * The values do not need to be valid Unicode identifiers.
     */
    @Test
    public void testGetIdentifier() {
        assertEquals("Apache:SIS",  getIdentifier(SIS));
        assertEquals("OGC",         getIdentifier(OGC));
        assertEquals("IOGP",        getIdentifier(IOGP));
        assertEquals("EPSG",        getIdentifier(EPSG));
        assertEquals("ESRI:ArcGIS", getIdentifier(ESRI));
        assertEquals("NetCDF",      getIdentifier(NETCDF));
        assertEquals("GeoTIFF",     getIdentifier(GEOTIFF));
        assertEquals("ISBN",        getIdentifier(ISBN));
        assertEquals("ISSN",        getIdentifier(ISSN));
        assertEquals("OSGeo:PROJ",  getIdentifier(PROJ4));              // Not a valid Unicode identifier.
        assertEquals("IHO:S-57",    getIdentifier(S57));                // Not a valid Unicode identifier.
        assertEquals("ISO:19115-1", getIdentifier(ISO_19115.get(0)));   // The ':' separator is not usual in ISO references
        assertEquals("ISO:19115-2", getIdentifier(ISO_19115.get(1)));   // and could be changed in future SIS versions.
        assertEquals("OGC:WMS",     getIdentifier(WMS));
    }

    /**
     * Tests {@link Citations#toCodeSpace(Citation)} on the constants
     * declared in the {@link Citations} class.
     */
    @Test
    @DependsOnMethod("testGetIdentifier")
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
        assertNull  ("ISO_19115-1", toCodeSpace(ISO_19115.get(0)));
        assertNull  ("ISO_19115-2", toCodeSpace(ISO_19115.get(1)));
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
    @DependsOnMethod("testGetIdentifier")
    public void testToCodeSpace() {
        final SimpleCitation citation = new SimpleCitation(" Valid\u2060Id\u200Bentifier ");
        assertEquals("ValidIdentifier", Citations.toCodeSpace(citation));

        assertNull("Shall not be taken as a valid identifier.",
                Citations.toCodeSpace(new SimpleCitation("Proj.4")));
        assertEquals("Shall fallback on the the identifier space name.",
                "TheProj4Space", Citations.toCodeSpace(new Proj4()));
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
        assertTitleEquals("SIS",       "Apache Spatial Information System",                      SIS);
        assertTitleEquals("WMS",       "Web Map Server",                                         WMS);
        assertTitleEquals("OGC",       "OGC Naming Authority",                                   OGC);
        assertTitleEquals("EPSG",      "EPSG Geodetic Parameter Dataset",                        EPSG);
        assertTitleEquals("ISBN",      "International Standard Book Number",                     ISBN);
        assertTitleEquals("ISSN",      "International Standard Serial Number",                   ISSN);
        assertTitleEquals("GEOTIFF",   "GeoTIFF Coverage Encoding Profile",                      GEOTIFF);
        assertTitleEquals("NETCDF",    "NetCDF Classic and 64-bit Offset Format",                NETCDF);
        assertTitleEquals("PROJ4",     "PROJ coordinate transformation software library",        PROJ4);
        assertTitleEquals("S57",       "IHO transfer standard for digital hydrographic data",    S57);
        assertTitleEquals("ISO_19115", "Geographic Information — Metadata Part 1: Fundamentals", ISO_19115.get(0));
        assertTitleEquals("ISO_19115", "Geographic Information — Metadata Part 2: Extensions for imagery and gridded data", ISO_19115.get(1));
    }

    /**
     * Tests {@code getCitedResponsibleParties()} on some {@code Citation} constants.
     */
    @Test
    public void testGetCitedResponsibleParty() {
        assertEquals("Open Geospatial Consortium",                       getCitedResponsibleParty(OGC));
        assertEquals("International Organization for Standardization",   getCitedResponsibleParty(ISO_19115.get(0)));
        assertEquals("International Organization for Standardization",   getCitedResponsibleParty(ISO_19115.get(1)));
        assertEquals("International Association of Oil & Gas producers", getCitedResponsibleParty(EPSG));
        assertEquals("International Association of Oil & Gas producers", getCitedResponsibleParty(IOGP));
    }

    /**
     * Returns the responsible party for the given constant.
     */
    private static String getCitedResponsibleParty(final Citation citation) {
        return getSingleton(getSingleton(citation.getCitedResponsibleParties()).getParties()).getName().toString(Locale.US);
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
        final Identifier identifier = getSingleton(EPSG.getIdentifiers());
        assertEquals("EPSG", toCodeSpace(EPSG));
        assertEquals("IOGP", identifier.getCodeSpace());
        assertEquals("EPSG", identifier.getCode());
    }

    /**
     * Verifies that citation constants are unmodifiable.
     */
    @Test
    public void ensureUnmodifiable() {
        final Collection<? extends Identifier> identifiers = Citations.EPSG.getIdentifiers();
        assertNotNull(identifiers);
        try {
            identifiers.add(null);
            fail("Predefined metadata shall be unmodifiable.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
        }
    }

    /**
     * Test serialization.
     *
     * @throws IllegalAccessException should never happen since we asked only for public fields.
     */
    @Test
    @DependsOnMethod("testFromName")
    public void testSerialization() throws IllegalAccessException {
        for (final Field field : Citations.class.getDeclaredFields()) {
            if (CitationConstant.class.isAssignableFrom(field.getType())) {
                final Object c = field.get(null);
                assertSame(field.getName(), c, assertSerializedEquals(c));
            }
        }
    }

    /**
     * Tests {@link Citations#identifierMatches(Citation, Identifier, String)}.
     */
    @Test
    public void testIdentifierMatches() {
        final Identifier ogc = new DefaultIdentifier("OGC", "06-042", null);
        final Identifier iso = new DefaultIdentifier("ISO", "19128", null);
        final DefaultCitation citation = new DefaultCitation("Web Map Server");
        citation.setIdentifiers(List.of(ogc, iso, new DefaultIdentifier("Foo", "06-042", null)));
        assertTrue ("With full identifier",  Citations.identifierMatches(citation, ogc, ogc.getCode()));
        assertTrue ("With full identifier",  Citations.identifierMatches(citation, iso, iso.getCode()));
        assertFalse("With wrong code",       Citations.identifierMatches(citation, new DefaultIdentifier("ISO", "19115", null), "19115"));
        assertFalse("With wrong code space", Citations.identifierMatches(citation, new DefaultIdentifier("Foo", "19128", null), "19128"));
        assertFalse("With wrong code",       Citations.identifierMatches(citation, "Foo"));
        assertTrue ("Without identifier",    Citations.identifierMatches(citation, "19128"));
        assertTrue ("With parsing",          Citations.identifierMatches(citation, "ISO:19128"));
        assertFalse("With wrong code space", Citations.identifierMatches(citation, "Foo:19128"));
    }
}
