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

import java.util.List;
import java.util.Locale;
import java.lang.reflect.Field;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.simple.CitationConstant;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.metadata.iso.citation.Citations.*;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link Citations}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public final strictfp class CitationsTest extends TestCase {
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
        assertSame(GEOTIFF,          fromName("GeoTIFF"));
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
        assertEquals("SIS",         getIdentifier(SIS));
        assertEquals("OGC",         getIdentifier(OGC));
        assertEquals("IOGP",        getIdentifier(IOGP));
        assertEquals("EPSG",        getIdentifier(EPSG));
        assertEquals("ESRI",        getIdentifier(ESRI));
        assertEquals("NetCDF",      getIdentifier(NETCDF));
        assertEquals("GeoTIFF",     getIdentifier(GEOTIFF));
        assertEquals("MapInfo",     getIdentifier(MAP_INFO));
        assertEquals("ISBN",        getIdentifier(ISBN));
        assertEquals("ISSN",        getIdentifier(ISSN));
        assertEquals("Proj.4",      getIdentifier(PROJ4));  // Not a valid Unicode identifier.
        assertEquals("S-57",        getIdentifier(S57));    // Not a valid Unicode identifier.
        assertEquals("ISO:19115-1", getIdentifier(ISO_19115.get(0)));  // The ':' separator is not usual in ISO references
        assertEquals("ISO:19115-2", getIdentifier(ISO_19115.get(1)));  // and could be changed in future SIS versions.
        assertEquals("OGC:WMS",     getIdentifier(WMS));
        assertIdentifierEquals("OGC:06-042", null, "OGC", null, "06-042",
                (ReferenceIdentifier) ((List<? extends Identifier>) WMS.getIdentifiers()).get(1));
        assertIdentifierEquals("ISO:19128", null, "ISO", "2005", "19128",
                (ReferenceIdentifier) ((List<? extends Identifier>) WMS.getIdentifiers()).get(2));
    }

    /**
     * Tests {@link Citations#getUnicodeIdentifier(Citation)} on the constants declared in the {@link Citations} class.
     * All values shall be valid Unicode identifiers or {@code null}.
     */
    @Test
    @DependsOnMethod("testGetIdentifier")
    public void testGetUnicodeIdentifier() {
        assertEquals("SIS",         getUnicodeIdentifier(SIS));
        assertEquals("OGC",         getUnicodeIdentifier(OGC));
        assertEquals("IOGP",        getUnicodeIdentifier(IOGP));
        assertEquals("EPSG",        getUnicodeIdentifier(EPSG));
        assertEquals("ESRI",        getUnicodeIdentifier(ESRI));
        assertEquals("NetCDF",      getUnicodeIdentifier(NETCDF));
        assertEquals("GeoTIFF",     getUnicodeIdentifier(GEOTIFF));
        assertEquals("MapInfo",     getUnicodeIdentifier(MAP_INFO));
        assertEquals("ISBN",        getUnicodeIdentifier(ISBN));
        assertEquals("ISSN",        getUnicodeIdentifier(ISSN));
        assertNull  ("Proj4",       getUnicodeIdentifier(PROJ4));      // Not yet publicly declared as an identifier.
        assertNull  ("S57",         getUnicodeIdentifier(S57));        // Not yet publicly declared as an identifier.
        assertEquals("OGC_WMS",     getUnicodeIdentifier(WMS));
        assertNull  ("ISO_19115-1", getUnicodeIdentifier(ISO_19115.get(0)));  // Not a valid Unicode identifier.
        assertNull  ("ISO_19115-2", getUnicodeIdentifier(ISO_19115.get(1)));
    }

    /**
     * Tests {@link org.apache.sis.internal.util.Citations#getCodeSpace(Citation)} on the constants
     * declared in the {@link Citations} class.
     */
    @Test
    @DependsOnMethod("testGetUnicodeIdentifier")
    public void testGetCodeSpace() {
        assertEquals("SIS",         org.apache.sis.internal.util.Citations.getCodeSpace(SIS));
        assertEquals("OGC",         org.apache.sis.internal.util.Citations.getCodeSpace(WMS));
        assertEquals("OGC",         org.apache.sis.internal.util.Citations.getCodeSpace(OGC));
        assertEquals("IOGP",        org.apache.sis.internal.util.Citations.getCodeSpace(IOGP));
        assertEquals("EPSG",        org.apache.sis.internal.util.Citations.getCodeSpace(EPSG));
        assertEquals("ESRI",        org.apache.sis.internal.util.Citations.getCodeSpace(ESRI));
        assertEquals("NetCDF",      org.apache.sis.internal.util.Citations.getCodeSpace(NETCDF));
        assertEquals("GeoTIFF",     org.apache.sis.internal.util.Citations.getCodeSpace(GEOTIFF));
        assertEquals("MapInfo",     org.apache.sis.internal.util.Citations.getCodeSpace(MAP_INFO));
        assertEquals("ISBN",        org.apache.sis.internal.util.Citations.getCodeSpace(ISBN));
        assertEquals("ISSN",        org.apache.sis.internal.util.Citations.getCodeSpace(ISSN));
        assertEquals("Proj4",       org.apache.sis.internal.util.Citations.getCodeSpace(PROJ4));
        assertEquals("S57",         org.apache.sis.internal.util.Citations.getCodeSpace(S57));
        assertNull  ("ISO_19115-1", org.apache.sis.internal.util.Citations.getCodeSpace(ISO_19115.get(0)));
        assertNull  ("ISO_19115-2", org.apache.sis.internal.util.Citations.getCodeSpace(ISO_19115.get(1)));
    }

    /**
     * Tests {@code getTitle()} on some {@code Citation} constants.
     */
    @Test
    public void testGetTitles() {
        assertTitleEquals("SIS",     "Apache Spatial Information System",    SIS);
        assertTitleEquals("WMS",     "Web Map Server",                       WMS);
        assertTitleEquals("OGC",     "Identifiers in OGC namespace",         OGC);
        assertTitleEquals("EPSG",    "EPSG Geodetic Parameter Dataset",      EPSG);
        assertTitleEquals("ISBN",    "International Standard Book Number",   ISBN);
        assertTitleEquals("ISSN",    "International Standard Serial Number", ISSN);
        assertTitleEquals("GEOTIFF", "GeoTIFF",                              GEOTIFF);
        assertTitleEquals("NETCDF",  "NetCDF",                               NETCDF);
        assertTitleEquals("PROJ4",   "Proj.4",                               PROJ4);
        assertTitleEquals("S57",     "S-57",                                 S57);
        assertTitleEquals("ISO_19115", "Geographic Information — Metadata Part 1: Fundamentals", ISO_19115.get(0));
        assertTitleEquals("ISO_19115", "Geographic Information — Metadata Part 2: Extensions for imagery and gridded data", ISO_19115.get(1));
        assertEquals     ("ISO_19128", "Geographic Information — Web map server interface", getSingleton(WMS.getAlternateTitles()).toString());
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
        return getSingleton(citation.getCitedResponsibleParties()).getOrganisationName().toString(Locale.US);
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
        assertEquals("EPSG", getUnicodeIdentifier(EPSG));
        assertEquals("IOGP", ((ReferenceIdentifier) identifier).getCodeSpace());
        assertEquals("EPSG", identifier.getCode());
    }

    /**
     * Test serialization.
     *
     * @throws IllegalAccessException should never happen since we asked only for public fields.
     */
    @Test
    @DependsOnMethod("testFromName")
    public void testSerialization() throws IllegalAccessException {
        for (final Field field : Citations.class.getFields()) {
            if (CitationConstant.class.isAssignableFrom(field.getType())) {
                final Object c = field.get(null);
                assertSame(field.getName(), c, assertSerializedEquals(c));
            }
        }
    }
}
