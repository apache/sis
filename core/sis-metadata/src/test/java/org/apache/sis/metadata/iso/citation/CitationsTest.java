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

import java.util.Locale;
import java.lang.reflect.Field;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.simple.CitationConstant;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.metadata.iso.citation.Citations.*;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link Citations}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
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
        assertSame(ISO,       fromName("ISO"));
        assertSame(ISO_19115, fromName("ISO 19115"));
        assertSame(OGC,       fromName(Constants.OGC));
        assertSame(EPSG,      fromName(Constants.EPSG));  // This one is most important.
        assertSame(OGP,       fromName(Constants.IOGP));  // For parsing GML "codeSpace" attribute.
        assertSame(OGP,       fromName("OGP"));           // Sometime seen in GML instead of "IOGP".
        assertSame(SIS,       fromName(Constants.SIS));
        assertSame(ESRI,      fromName("ESRI"));
        assertSame(ORACLE,    fromName("Oracle"));
        assertSame(NETCDF,    fromName("NetCDF"));
        assertSame(GEOTIFF,   fromName("GeoTIFF"));
        assertSame(PROJ4,     fromName("Proj.4"));
        assertSame(PROJ4,     fromName("Proj4"));
        assertSame(MAP_INFO,  fromName("MapInfo"));
        assertSame(S57,       fromName("S-57"));
        assertSame(S57,       fromName("S57"));
        assertSame(ISBN,      fromName("ISBN"));
        assertSame(ISSN,      fromName("ISSN"));
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
        assertEquals("ISO",        getIdentifier(ISO));
        assertEquals("ISO:19115",  getIdentifier(ISO_19115));
        assertEquals("OGC",        getIdentifier(OGC));
        assertEquals("OGP",        getIdentifier(OGP));
        assertEquals("EPSG",       getIdentifier(EPSG));
        assertEquals("SIS",        getIdentifier(SIS));
        assertEquals("ESRI",       getIdentifier(ESRI));
        assertEquals("Oracle",     getIdentifier(ORACLE));
        assertEquals("NetCDF",     getIdentifier(NETCDF));
        assertEquals("GeoTIFF",    getIdentifier(GEOTIFF));
        assertEquals("MapInfo",    getIdentifier(MAP_INFO));
        assertEquals("ISBN",       getIdentifier(ISBN));
        assertEquals("ISSN",       getIdentifier(ISSN));
        assertEquals("Proj.4",     getIdentifier(PROJ4));  // Not a valid Unicode identifier.
        assertEquals("S-57",       getIdentifier(S57));    // Not a valid Unicode identifier.
    }

    /**
     * Tests {@link Citations#getUnicodeIdentifier(Citation)} on the constants declared in the {@link Citations} class.
     * All values shall be valid Unicode identifiers or {@code null}.
     */
    @Test
    @DependsOnMethod("testGetIdentifier")
    public void testGetUnicodeIdentifier() {
        assertEquals("ISO",        getUnicodeIdentifier(ISO));
        assertEquals("ISO_19115",  getUnicodeIdentifier(ISO_19115));
        assertEquals("OGC",        getUnicodeIdentifier(OGC));
        assertEquals("OGP",        getUnicodeIdentifier(OGP));
        assertEquals("EPSG",       getUnicodeIdentifier(EPSG));
        assertEquals("SIS",        getUnicodeIdentifier(SIS));
        assertEquals("ESRI",       getUnicodeIdentifier(ESRI));
        assertEquals("Oracle",     getUnicodeIdentifier(ORACLE));
        assertEquals("NetCDF",     getUnicodeIdentifier(NETCDF));
        assertEquals("GeoTIFF",    getUnicodeIdentifier(GEOTIFF));
        assertEquals("MapInfo",    getUnicodeIdentifier(MAP_INFO));
        assertEquals("ISBN",       getUnicodeIdentifier(ISBN));
        assertEquals("ISSN",       getUnicodeIdentifier(ISSN));
        assertNull  ("Proj4",      getUnicodeIdentifier(PROJ4));      // Not yet publicly declared as an identifier.
        assertNull  ("S57",        getUnicodeIdentifier(S57));        // Not yet publicly declared as an identifier.
    }

    /**
     * Tests {@link org.apache.sis.internal.util.Citations#getCodeSpace(Citation)} on the constants
     * declared in the {@link Citations} class.
     */
    @Test
    @DependsOnMethod("testGetUnicodeIdentifier")
    public void testGetCodeSpace() {
        assertEquals("ISO",        org.apache.sis.internal.util.Citations.getCodeSpace(ISO));
        assertEquals("ISO_19115",  org.apache.sis.internal.util.Citations.getCodeSpace(ISO_19115));
        assertEquals("OGC",        org.apache.sis.internal.util.Citations.getCodeSpace(OGC));
        assertEquals("OGP",        org.apache.sis.internal.util.Citations.getCodeSpace(OGP));
        assertEquals("EPSG",       org.apache.sis.internal.util.Citations.getCodeSpace(EPSG));
        assertEquals("SIS",        org.apache.sis.internal.util.Citations.getCodeSpace(SIS));
        assertEquals("ESRI",       org.apache.sis.internal.util.Citations.getCodeSpace(ESRI));
        assertEquals("Oracle",     org.apache.sis.internal.util.Citations.getCodeSpace(ORACLE));
        assertEquals("NetCDF",     org.apache.sis.internal.util.Citations.getCodeSpace(NETCDF));
        assertEquals("GeoTIFF",    org.apache.sis.internal.util.Citations.getCodeSpace(GEOTIFF));
        assertEquals("MapInfo",    org.apache.sis.internal.util.Citations.getCodeSpace(MAP_INFO));
        assertEquals("ISBN",       org.apache.sis.internal.util.Citations.getCodeSpace(ISBN));
        assertEquals("ISSN",       org.apache.sis.internal.util.Citations.getCodeSpace(ISSN));
        assertEquals("Proj4",      org.apache.sis.internal.util.Citations.getCodeSpace(PROJ4));
        assertEquals("S57",        org.apache.sis.internal.util.Citations.getCodeSpace(S57));
    }

    /**
     * Tests {@code getTitle()} on some {@code Citation} constants.
     */
    @Test
    public void testGetTitles() {
        assertEquals("ISO 19115 Geographic Information — Metadata",       ISO_19115.getTitle().toString(Locale.US));
        assertEquals("Identifier in OGC namespace",                       OGC      .getTitle().toString(Locale.US));
        assertEquals("EPSG Geodetic Parameter Dataset",                   EPSG     .getTitle().toString(Locale.US));
        assertEquals("Apache Spatial Information System",                 SIS      .getTitle().toString(Locale.US));
        assertEquals("International Standard Book Number",                ISBN     .getTitle().toString(Locale.US));
        assertEquals("International Standard Serial Number",              ISSN     .getTitle().toString(Locale.US));
        assertEquals("GeoTIFF",                                           GEOTIFF  .getTitle().toString(Locale.US));
        assertEquals("NetCDF",                                            NETCDF   .getTitle().toString(Locale.US));
        assertEquals("Proj.4",                                            PROJ4    .getTitle().toString(Locale.US));
        assertEquals("S-57",                                              S57      .getTitle().toString(Locale.US));
    }

    /**
     * Tests {@code getCitedResponsibleParties()} on some {@code Citation} constants.
     */
    @Test
    public void testGetCitedResponsibleParty() {
        assertEquals("Open Geospatial Consortium",                       getCitedResponsibleParty(OGC));
        assertEquals("International Organization for Standardization",   getCitedResponsibleParty(ISO_19115));
        assertEquals("International Association of Oil & Gas producers", getCitedResponsibleParty(EPSG));
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
        assertEquals("EPSG", getUnicodeIdentifier(EPSG));
        assertEquals("IOGP", identifier.getCodeSpace());
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
