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
package org.apache.sis.referencing.report;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.io.File;
import java.io.IOException;

import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.test.report.AuthorityCodesReport;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Version;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Generates a list of supported Coordinate Reference Systems in the current directory.
 * This class is for manual execution after the EPSG database has been updated,
 * or the projection implementations changed.
 *
 * <p><b>WARNING:</b>
 * this class implements heuristic rules for nicer sorting (e.g. of CRS having numbers as Roman letters).
 * Those heuristic rules were determined specifically for the EPSG dataset expanded with WMS codes.
 * This class is not likely to produce good results for any other authorities, and many need to be updated
 * after any upgrade of the EPSG dataset.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class CoordinateReferenceSystems extends AuthorityCodesReport {
    /**
     * The titles of some sections in the list of CRS. We use sections for grouping related CRS together.
     * By default (if no mapping is specified below), the section titles will be the datum names.
     * But in some cases we use a slightly different title. Sometime the changes are only cosmetic
     * (e.g. "Reseau Geodesique Francais" → "Réseau Géodésique Français").
     * But sometime the changes have the effect of merging different datums under the same section.
     * We do that only when the datums are closely related, and the decision to merge or not is taken on a
     * case-by-case basis. For example we merge the "Arc 1950" and "Arc 1960" sections into a single "Arc" section,
     * since those sections were small and we do not want to scatter the HTML page with too many sections.
     * However we do not merge "NAD83" and "NAD83(HARN)" because those sections are already quite large,
     * and merging them will result in a too large section.
     *
     * <p>The decision to merge or not is arbitrary. Generally, we try to avoid small sections (less that 5 CRS)
     * but without merging together unrelated datums.</p>
     */
    private static final Map<String,String> SECTION_TITLES = new HashMap<>();
    static {
        rd("Arc 1950",                                                    "Arc");
        rd("Arc 1960",                                                    "Arc");
        rd("Ancienne Triangulation Francaise (Paris)",                    "Ancienne Triangulation Française");
        rd("Australian Geodetic Datum 1966",                              "Australian Geodetic Datum");
        rd("Australian Geodetic Datum 1984",                              "Australian Geodetic Datum");
        rd("Australian Height Datum (Tasmania)",                          "Australian Height Datum");
        rd("Azores Central Islands 1948",                                 "Azores Central Islands");
        rd("Azores Central Islands 1995",                                 "Azores Central Islands");
        rd("Azores Oriental Islands 1940",                                "Azores Oriental Islands");
        rd("Azores Oriental Islands 1995",                                "Azores Oriental Islands");
        rd("Baltic 1980",                                                 "Baltic");
        rd("Baltic 1982",                                                 "Baltic");
        rd("Baltic Sea",                                                  "Baltic");
        rd("Batavia (Jakarta)",                                           "Batavia");
        rd("Bermuda 1957",                                                "Bermuda");
        rd("Bermuda 2000",                                                "Bermuda");
        rd("Bogota 1975 (Bogota)",                                        "Bogota 1975");
        rd("Carthage (Paris)",                                            "Carthage");
        rd("Bern 1938",                                                   "Bern / CH1903");
        rd("CH1903",                                                      "Bern / CH1903");
        rd("CH1903+",                                                     "Bern / CH1903");
        rd("CH1903 (Bern)",                                               "Bern / CH1903");
        rd("Chatham Islands Datum 1971",                                  "Chatham Islands Datum");
        rd("Chatham Islands Datum 1979",                                  "Chatham Islands Datum");
        rd("Corrego Alegre 1961",                                         "Corrego Alegre");
        rd("Corrego Alegre 1970-72",                                      "Corrego Alegre");
        rd("Dealul Piscului 1930",                                        "Dealul Piscului");
        rd("Dealul Piscului 1970",                                        "Dealul Piscului");
        rd("Deutsches Haupthoehennetz 1985",                              "Deutsches Haupthoehennetz");
        rd("Deutsches Haupthoehennetz 1992",                              "Deutsches Haupthoehennetz");
        rd("Douala 1948",                                                 "Douala");
        rd("Dunedin 1958",                                                "Dunedin");
        rd("Dunedin-Bluff 1960",                                          "Dunedin");
        rd("EGM2008 geoid",                                               "EGM geoid");
        rd("EGM84 geoid",                                                 "EGM geoid");
        rd("EGM96 geoid",                                                 "EGM geoid");
        rd("Egypt 1907",                                                  "Egypt");
        rd("Egypt 1930",                                                  "Egypt");
        rd("Egypt Gulf of Suez S-650 TL",                                 "Egypt");
        rd("EPSG example  X",                                             "Seismic bin grid datum");     // 2 spaces before "X".
        rd("Estonia 1992",                                                "Estonia");
        rd("Estonia 1997",                                                "Estonia");
        rd("European Datum 1950",                                         "European Datum");
        rd("European Datum 1950(1977)",                                   "European Datum");
        rd("European Datum 1979",                                         "European Datum");
        rd("European Datum 1987",                                         "European Datum");
        rd("European Vertical Reference Frame 2000",                      "European Vertical Reference Frame");
        rd("European Vertical Reference Frame 2007",                      "European Vertical Reference Frame");
        rd("Fahud Height Datum",                                          "Fahud");
        rd("Fao 1979",                                                    "Fao");
        rd("Faroe Datum 1954",                                            "Faroe Islands");
        rd("Faroe Islands Vertical Reference 2009",                       "Faroe Islands");
        rd("fk89",                                                        "Faroe Islands");
        rd("Fiji 1956",                                                   "Fiji");
        rd("Fiji Geodetic Datum 1986",                                    "Fiji");
        rd("Greek (Athens)",                                              "Greek");
        rd("Greek Geodetic Reference System 1987",                        "Greek");
        rd("Guadeloupe 1948",                                             "Guadeloupe");
        rd("Guadeloupe 1951",                                             "Guadeloupe");
        rd("Guadeloupe 1988",                                             "Guadeloupe");
        rd("Gunung Segara (Jakarta)",                                     "Gunung Segara");
        rd("Guyane Francaise",                                            "Centre Spatial Guyanais 1967");   // For a deprecated CRS.
        rd("Hong Kong 1963",                                              "Hong Kong");
        rd("Hong Kong 1963(67)",                                          "Hong Kong");
        rd("Hong Kong 1980",                                              "Hong Kong");
        rd("Hong Kong Chart Datum",                                       "Hong Kong");
        rd("Hong Kong Principal Datum",                                   "Hong Kong");
        rd("Hungarian Datum 1909",                                        "Hungarian Datum");
        rd("Hungarian Datum 1972",                                        "Hungarian Datum");
        rd("IGN 1962 Kerguelen",                                          "IGN");
        rd("IGN 1966",                                                    "IGN");
        rd("IGN 1988 LS",                                                 "IGN");
        rd("IGN 1988 MG",                                                 "IGN");
        rd("IGN 1988 SB",                                                 "IGN");
        rd("IGN 1988 SM",                                                 "IGN");
        rd("IGN 1992 LD",                                                 "IGN");
        rd("IGN Astro 1960",                                              "IGN");
        rd("IGN53 Mare",                                                  "IGN");
        rd("IGN56 Lifou",                                                 "IGN");
        rd("IGN63 Hiva Oa",                                               "IGN");
        rd("IGN72 Grande Terre",                                          "IGN");
        rd("IGN72 Nuku Hiva",                                             "IGN");
        rd("IGN78 Corsica",                                               "IGN");
        rd("Indian 1954",                                                 "Indian");
        rd("Indian 1960",                                                 "Indian");
        rd("Indian 1975",                                                 "Indian");
        rd("International Great Lakes Datum 1955",                        "International Great Lakes Datum");
        rd("International Great Lakes Datum 1985",                        "International Great Lakes Datum");
        rd("International Terrestrial Reference Frame 1988",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 1989",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 1990",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 1991",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 1992",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 1993",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 1994",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 1996",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 1997",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 2000",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 2005",              "International Terrestrial Reference Frame");
        rd("International Terrestrial Reference Frame 2008",              "International Terrestrial Reference Frame");
        rd("Islands Net 1993",                                            "Islands Net");
        rd("Islands Net 2004",                                            "Islands Net");
        rd("Jamaica 1875",                                                "Jamaica");
        rd("Jamaica 1969",                                                "Jamaica");
        rd("Jamaica 2001",                                                "Jamaica");
        rd("Kalianpur 1880",                                              "Kalianpur");
        rd("Kalianpur 1937",                                              "Kalianpur");
        rd("Kalianpur 1962",                                              "Kalianpur");
        rd("Kalianpur 1975",                                              "Kalianpur");
        rd("Kertau (RSO)",                                                "Kertau");
        rd("Kertau 1968",                                                 "Kertau");
        rd("Korean Datum 1985",                                           "Korean Datum");
        rd("Korean Datum 1995",                                           "Korean Datum");
        rd("Kuwait Oil Company",                                          "Kuwait Oil Company / Kuwait Utility");
        rd("Kuwait PWD",                                                  "Kuwait Oil Company / Kuwait Utility");
        rd("Kuwait Utility",                                              "Kuwait Oil Company / Kuwait Utility");
        rd("Lao 1993",                                                    "Lao");
        rd("Lao National Datum 1997",                                     "Lao");
        rd("Lisbon 1890",                                                 "Lisbon");
        rd("Lisbon 1890 (Lisbon)",                                        "Lisbon");
        rd("Lisbon 1937",                                                 "Lisbon");
        rd("Lisbon 1937 (Lisbon)",                                        "Lisbon");
        rd("Cote d'Ivoire",                                               "Locodjo 1965");   // For a deprecated CRS.
        rd("Madeira 1936",                                                "Porto Santo");    // For a deprecated CRS.
        rd("Makassar (Jakarta)",                                          "Makassar");
        rd("Manoca 1962",                                                 "Manoca");
        rd("Martinique 1938",                                             "Martinique");
        rd("Martinique 1955",                                             "Martinique");
        rd("Martinique 1987",                                             "Martinique");
        rd("Maupiti 83",                                                  "Maupiti");
        rd("Maupiti SAU 2001",                                            "Maupiti");
        rd("Missao Hidrografico Angola y Sao Tome 1951",                  "Missao Hidrografico Angola y Sao Tome");
        rd("Mhast",                                                       "Missao Hidrografico Angola y Sao Tome");
        rd("Mhast (offshore)",                                            "Missao Hidrografico Angola y Sao Tome");
        rd("Mhast (onshore)",                                             "Missao Hidrografico Angola y Sao Tome");
        rd("Militar-Geographische Institut (Ferro)",                      "Militar-Geographische Institut");
        rd("Monte Mario (Rome)",                                          "Monte Mario");
        rd("Moorea 87",                                                   "Moorea");
        rd("Moorea SAU 1981",                                             "Moorea");
        rd("Nahrwan 1934",                                                "Nahrwan");
        rd("Nahrwan 1967",                                                "Nahrwan");
        rd("Naparima 1955",                                               "Naparima");
        rd("Naparima 1972",                                               "Naparima");
        rd("Nivellement General de la Corse 1948",                        "Nivellement Général Corse / France / Nouvelle-Calédonie / Polynésie Française / Luxembourd / Guyanais");
        rd("Nivellement General de la France - IGN69",                    "Nivellement Général Corse / France / Nouvelle-Calédonie / Polynésie Française / Luxembourd / Guyanais");
        rd("Nivellement General de la France - Lallemand",                "Nivellement Général Corse / France / Nouvelle-Calédonie / Polynésie Française / Luxembourd / Guyanais");
        rd("Nivellement General de Nouvelle Caledonie",                   "Nivellement Général Corse / France / Nouvelle-Calédonie / Polynésie Française / Luxembourd / Guyanais");
        rd("Nivellement General de Polynesie Francaise",                  "Nivellement Général Corse / France / Nouvelle-Calédonie / Polynésie Française / Luxembourd / Guyanais");
        rd("Nivellement General du Luxembourg",                           "Nivellement Général Corse / France / Nouvelle-Calédonie / Polynésie Française / Luxembourd / Guyanais");
        rd("Nivellement General Guyanais 1977",                           "Nivellement Général Corse / France / Nouvelle-Calédonie / Polynésie Française / Luxembourd / Guyanais");
        rd("NGO 1948 (Oslo)",                                             "NGO 1948");
        rd("NAD27 Michigan",                                              "North American Datum 1927 — Michigan");
        rd("NAD83 (High Accuracy Reference Network)",                     "North American Datum 1983 — High Accuracy Reference Network");
        rd("NAD83 (National Spatial Reference System 2007)",              "North American Datum 1983 — National Spatial Reference System 2007");
        rd("NAD83 Canadian Spatial Reference System",                     "North American Datum 1983 — Canadian Spatial Reference System");
        rd("Nord Sahara 1959 (Paris)",                                    "Nord Sahara 1959");
        rd("Nord de Guerre (Paris)",                                      "Ancienne Triangulation Française");
        rd("Nouvelle Triangulation Francaise",                            "Nouvelle Triangulation Française");
        rd("Nouvelle Triangulation Francaise (Paris)",                    "Nouvelle Triangulation Française");
        rd("Ordnance Datum Newlyn (Orkney Isles)",                        "Ordnance Datum Newlyn");
        rd("OSGB 1936",                                                   "OSGB");
        rd("OSGB 1970 (SN)",                                              "OSGB");
        rd("Padang 1884 (Jakarta)",                                       "Padang 1884");
        rd("PDO Height Datum 1993",                                       "PDO Survey / Height Datum 1993");
        rd("PDO Survey Datum 1993",                                       "PDO Survey / Height Datum 1993");
        rd("Pitcairn 1967",                                               "Pitcairn");
        rd("Pitcairn 2006",                                               "Pitcairn");
        rd("Porto Santo 1936",                                            "Porto Santo");
        rd("Porto Santo 1995",                                            "Porto Santo");
        rd("Posiciones Geodesicas Argentinas 1994",                       "Posiciones Geodesicas Argentinas");
        rd("Posiciones Geodesicas Argentinas 1998",                       "Posiciones Geodesicas Argentinas");
        rd("Posiciones Geodesicas Argentinas 2007",                       "Posiciones Geodesicas Argentinas");
        rd("Qatar 1948",                                                  "Qatar");
        rd("Qatar 1974",                                                  "Qatar");
        rd("Qatar National Datum 1995",                                   "Qatar");
        rd("Qornoq 1927",                                                 "Qornoq");
        rd("Reseau Geodesique Nouvelle Caledonie 1991",                   "Réseau Géodésique de Nouvelle-Calédonie");
        rd("Reseau Geodesique de Nouvelle Caledonie 91-93",               "Réseau Géodésique de Nouvelle-Calédonie");
        rd("Reseau National Belge 1950",                                  "Réseau National Belge");
        rd("Reseau National Belge 1950 (Brussels)",                       "Réseau National Belge");
        rd("Reseau National Belge 1972",                                  "Réseau National Belge");
        rd("Reunion 1947",                                                "Réunion");
        rd("Reunion 1989",                                                "Réunion");
        rd("Rikets hojdsystem 1900",                                      "Rikets hojdsystem");
        rd("Rikets hojdsystem 1970",                                      "Rikets hojdsystem");
        rd("Rikets hojdsystem 2000",                                      "Rikets hojdsystem");
        rd("Samboja",                                                     "Gunung Segara");      // For a deprecated CRS.
        rd("Segora",                                                      "Gunung Segara");      // For a deprecated CRS.
        rd("Sierra Leone 1968",                                           "Sierra Leone");
        rd("Sierra Leone Colony 1924",                                    "Sierra Leone");
        rd("SIRGAS-Chile",                                                "SIRGAS");
        rd("SIRGAS-ROU98",                                                "SIRGAS");
        rd("SIRGAS_ES2007.8",                                             "SIRGAS");
        rd("South American Datum 1969",                                   "South American Datum");
        rd("South American Datum 1969(96)",                               "South American Datum");
        rd("Sri Lanka Datum 1999",                                        "Sri Lanka");
        rd("Sri Lanka Vertical Datum",                                    "Sri Lanka");
        rd("Stockholm 1938 (Stockholm)",                                  "Stockholm 1938");
        rd("Sudan",                                                       "Adindan");            // For a deprecated CRS.
        rd("System Jednotne Trigonometricke Site Katastralni (Ferro)",    "System Jednotne Trigonometricke Site Katastralni");
        rd("System Jednotne Trigonometricke Site Katastralni/05",         "System Jednotne Trigonometricke Site Katastralni");
        rd("System Jednotne Trigonometricke Site Katastralni/05 (Ferro)", "System Jednotne Trigonometricke Site Katastralni");
        rd("Tahaa 54",                                                    "Tahaa");
        rd("Tahaa SAU 2001",                                              "Tahaa");
        rd("Tahiti 52",                                                   "Tahiti");
        rd("Tahiti 79",                                                   "Tahiti");
        rd("Taiwan Datum 1967",                                           "Taiwan Datum");
        rd("Taiwan Datum 1997",                                           "Taiwan Datum");
        rd("Tananarive 1925 (Paris)",                                     "Tananarive 1925");
        rd("Viti Levu 1912",                                              "Viti Levu");
        rd("Viti Levu 1916",                                              "Viti Levu");          // For a deprecated CRS.
        rd("Voirol 1875",                                                 "Voirol");
        rd("Voirol 1875 (Paris)",                                         "Voirol");
        rd("Voirol 1879",                                                 "Voirol");
        rd("Voirol 1879 (Paris)",                                         "Voirol");
        rd("WGS 72 Transit Broadcast Ephemeris",                          "World Geodetic System 1972 — Transit Broadcast Ephemeris");
        rd("Yellow Sea 1956",                                             "Yellow Sea");
        rd("Yellow Sea 1985",                                             "Yellow Sea");
    }

    /**
     * Shortcut for {@link #SECTION_TITLES} initialization.
     * {@code "rd"} stands for "rename datum".
     */
    private static void rd(final String datum, final String display) {
        assertNull(datum, SECTION_TITLES.put(datum, display));
    }

    /**
     * The keywords before which to cut the CRS names. The main intend here is to preserve the
     * "far west", "west", "central west", "central", "central east", "east", "far east" order.
     */
    private static final String[] CUT_BEFORE = {
        " far west",        // "MAGNA-SIRGAS / Colombia Far West zone"
        " far east",
        " west",            // "Bogota 1975 / Colombia West zone"
        " east",            // "Bogota 1975 / Colombia East Central zone"
        " central",         // "Korean 1985 / Central Belt" (between "East Belt" and "West Belt")
        " old central",     // "NAD Michigan / Michigan Old Central"
        " bogota zone",     // "Bogota 1975 / Colombia Bogota zone"
        // Do not declare "North" and "South" as it causes confusion with "WGS 84 / North Pole" and other cases.
    };

    /**
     * The keywords after which to cut the CRS names.
     *
     * Note: alphabetical sorting of Roman numbers work for zones from I to VIII inclusive.
     * If there is more zones (for example with "JGD2000 / Japan Plane Rectangular"), then
     * we need to cut before those numbers in order to use sorting by EPSG codes instead.
     *
     * Note 2: if alphabetical sorting is okay for Roman numbers, it is actually preferable
     * because it give better position of names with height like "zone II + NGF IGN69 height".
     */
    private static final String[] CUT_AFTER = {
        " cs ",                     // "JGD2000 / Japan Plane Rectangular CS IX"
        " tm",                      // "ETRS89 / TM35FIN(E,N)" — we want to not interleave them between "TM35" and "TM36".
        " dktm",                    // "ETRS89 / DKTM1 + DVR90 height"
        "-gk",                      // "ETRS89 / ETRS-GK19FIN"
//      " philippines zone ",       // "Luzon 1911 / Philippines zone IV"
//      " california zone ",        // "NAD27 / California zone V"
//      " ngo zone ",               // "NGO 1948 (Oslo) / NGO zone I"
//      " lambert zone ",           // "NTF (Paris) / Lambert zone II + NGF IGN69 height"
        "fiji 1956 / utm zone "     // Two zones: 60S and 1S with 60 before 1.
    };

    /**
     * The symbol to write in from of EPSG code of CRS having an axis order different
     * then the (longitude, latitude) one.
     */
    private static final char YX_ORDER = '\u21B7';

    /**
     * The factory which create CRS instances.
     */
    private final CRSAuthorityFactory factory;

    /**
     * Creates a new instance.
     */
    private CoordinateReferenceSystems() throws FactoryException {
        super(null);
        properties.setProperty("TITLE",           "Apache SIS™ Coordinate Reference System (CRS) codes");
        properties.setProperty("PRODUCT.NAME",    "Apache SIS™");
        properties.setProperty("PRODUCT.VERSION", getVersion());
        properties.setProperty("PRODUCT.URL",     "http://sis.apache.org");
        properties.setProperty("JAVADOC.GEOAPI",  "http://www.geoapi.org/snapshot/javadoc");
        properties.setProperty("FACTORY.NAME",    "EPSG");
        properties.setProperty("FACTORY.VERSION", "7.9");
        properties.setProperty("FACTORY.VERSION.SUFFIX", ", together with other sources");
        properties.setProperty("DESCRIPTION", "<p><b>Notation:</b></p>\n" +
                "<ul>\n" +
                "  <li>The " + YX_ORDER + " symbol in front of authority codes (${PERCENT.ANNOTATED} of them) identifies" +
                " left-handed coordinate systems (for example with <var>latitude</var> axis before <var>longitude</var>).</li>\n" +
                "  <li>The <del>codes with a strike</del> (${PERCENT.DEPRECATED} of them) identify deprecated CRS." +
                " In some cases, the remarks column indicates the replacement.</li>\n" +
                "</ul>");
        factory = org.apache.sis.referencing.CRS.getAuthorityFactory(null);
        add(factory);
    }

    /**
     * Generates the HTML report.
     *
     * @param  args Ignored.
     * @throws FactoryException If an error occurred while fetching the CRS.
     * @throws IOException If an error occurred while writing the HTML file.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(final String[] args) throws FactoryException, IOException {
        Locale.setDefault(Locale.US);   // We have to use this hack for now because exceptions are formatted in the current locale.
        final CoordinateReferenceSystems writer = new CoordinateReferenceSystems();
        final File file = writer.write(new File("CoordinateReferenceSystems.html"));
        System.out.println("Created " + file.getAbsolutePath());
    }

    /**
     * Returns the current Apache SIS version, with the {@code -SNAPSHOT} trailing part omitted.
     *
     * @return The current Apache SIS version.
     */
    private static String getVersion() {
        String version = Version.SIS.toString();
        final int snapshot = version.lastIndexOf('-');
        if (snapshot >= 2) {
            version = version.substring(0, snapshot);
        }
        return version;
    }

    /**
     * Creates the text to show in the "Remarks" column for the given CRS.
     */
    private String getRemark(final CoordinateReferenceSystem crs) {
        if (crs instanceof GeographicCRS) {
            return (crs.getCoordinateSystem().getDimension() == 3) ? "Geographic 3D" : "Geographic";
        }
        if (crs instanceof GeneralDerivedCRS) {
            final OperationMethod method = ((GeneralDerivedCRS) crs).getConversionFromBase().getMethod();
            return "<a href=\"CoordinateOperationMethods.html#"
                   + IdentifiedObjects.getIdentifier(method, Citations.EPSG).getCode()
                   + "\">" + method.getName().getCode().replace('_', ' ') + "</a>";
        }
        if (crs instanceof GeocentricCRS) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs instanceof CartesianCS) {
                return "Geocentric (Cartesian coordinate system)";
            } else if (cs instanceof SphericalCS) {
                return "Geocentric (spherical coordinate system)";
            }
            return "Geocentric";
        }
        if (crs instanceof VerticalCRS) {
            final VerticalDatumType type = ((VerticalCRS) crs).getDatum().getVerticalDatumType();
            return CharSequences.camelCaseToSentence(type.name().toLowerCase(getLocale())) + " height";
        }
        if (crs instanceof CompoundCRS) {
            final StringBuilder buffer = new StringBuilder();
            for (final CoordinateReferenceSystem component : ((CompoundCRS) crs).getComponents()) {
                if (buffer.length() != 0) {
                    buffer.append(" + ");
                }
                buffer.append(getRemark(component));
            }
            return buffer.toString();
        }
        if (crs instanceof EngineeringCRS) {
            return "Engineering (" + crs.getCoordinateSystem().getName().getCode() + ')';
        }
        return "";
    }

    /**
     * Invoked when a CRS has been successfully created. This method modifies the default
     * {@link org.opengis.test.report.AuthorityCodesReport.Row} attribute values created
     * by GeoAPI.
     *
     * @param  code    The authority code of the created object.
     * @param  object  The object created from the given authority code.
     * @return The created row, or {@code null} if the row should be ignored.
     */
    @Override
    protected Row createRow(final String code, final IdentifiedObject object) {
        if (code.equals("EPSG:5804")) {
            System.out.println("");
        }
        final Row row = super.createRow(code, object);
        final CoordinateReferenceSystem crs = (CoordinateReferenceSystem) object;
        final CoordinateReferenceSystem crsXY = AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
        if (!Utilities.deepEquals(crs.getCoordinateSystem(), crsXY.getCoordinateSystem(), ComparisonMode.IGNORE_METADATA)) {
            row.annotation = YX_ORDER;
        }
        row.remark = getRemark(crs);
        if (object instanceof Deprecable) {
            row.isDeprecated = ((Deprecable) object).isDeprecated();
        }
        /*
         * If the object is deprecated, try to find the reason.
         * Don't take the whole comment, because it may be pretty long.
         */
        if (row.isDeprecated) {
            InternationalString i18n = object.getRemarks();
            for (final Identifier id : object.getIdentifiers()) {
                if (id instanceof Deprecable && ((Deprecable) id).isDeprecated()) {
                    i18n = ((Deprecable) id).getRemarks();
                    break;
                }
            }
            if (i18n != null) {
                row.remark = i18n.toString(getLocale());
            }
        }
        ((ByName) row).setup(CRS.getSingleComponents(crs).get(0).getDatum());
        return row;
    }

    /**
     * Invoked when a CRS creation failed. This method modifies the default
     * {@link org.opengis.test.report.AuthorityCodesReport.Row} attribute values
     * created by GeoAPI.
     *
     * @param  code      The authority code of the object to create.
     * @param  exception The exception that occurred while creating the identified object.
     * @return The created row, or {@code null} if the row should be ignored.
     */
    @Override
    protected Row createRow(final String code, final FactoryException exception) {
        final Row row = super.createRow(code, exception);
        try {
            row.name = factory.getDescriptionText(code).toString(getLocale());
        } catch (FactoryException e) {
            Logging.unexpectedException(null, CoordinateReferenceSystems.class, "createRow", e);
        }
        if (code.startsWith("AUTO2:")) {
            // It is normal to be unable to instantiate an "AUTO" CRS,
            // because those authority codes need parameters.
            row.hasError = false;
            row.remark = "Projected";
            ((ByName) row).setup(CommonCRS.WGS84.datum());
        } else {
            String message = exception.getMessage();
            if (message.contains("Unable to format units in UCUM")) {
                // Simplify a very long and badly formatted message.
                message = "Unable to format units in UCUM";
            }
            row.remark = message;
            ((ByName) row).setup(null);
        }
        return row;
    }

    /**
     * Invoked by {@link AuthorityCodesReport} for creating a new row instance.
     *
     * @return The new row instance.
     */
    @Override
    protected Row newRow() {
        return new ByName();
    }

    /**
     * A row with an natural ordering that use the first part of the name before to use the authority code.
     * We use only the part of the name prior some keywords (e.g. {@code "zone"}).
     * For example if the following codes:
     *
     * {@preformat text
     *    EPSG:32609    WGS 84 / UTM zone 9N
     *    EPSG:32610    WGS 84 / UTM zone 10N
     * }
     *
     * We compare only the "WGS 84 / UTM" string, then the code. This is a reasonably easy way to keep a more
     * natural ordering ("9" sorted before "10", "UTM North" projections kept together and same for South).
     */
    private static final class ByName extends Row {
        /**
         * A string derived from the {@link #name} to use for sorting.
         */
        private String reducedName;

        /**
         * The datum name, or {@code null} if unknown.
         * If non-null, this is used for grouping CRS names by sections.
         */
        String section;

        /**
         * Creates a new row.
         */
        ByName() {
        }

        /**
         * Computes the {@link #reducedName} field value.
         */
        final void setup(final Datum datum) {
            if (datum != null) {
                section = datum.getName().getCode();
            } else {
                // Temporary patch (TODO: remove after we implemented the missing methods in SIS)
                if (name.startsWith("NSIDC EASE-Grid")) {
                    section = "Unspecified datum";
                } else if (code.equals("EPSG:2163")) {
                    section = "Unspecified datum";
                } else if (code.equals("EPSG:5818")) {
                    section = "Seismic bin grid datum";
                }
            }
            section = JDK8.getOrDefault(SECTION_TITLES, section, section);
            /*
             * Get a copy of the name in all lower case.
             */
            final StringBuilder b = new StringBuilder(name);
            for (int i=0; i<b.length(); i++) {
                b.setCharAt(i, Character.toLowerCase(b.charAt(i)));
            }
            /*
             * Cut the string to a shorter length if we find a keyword.
             * This will result in many string equals, which will then be sorted by EPSG codes.
             * This is useful when the EPSG codes give a better ordering than the alphabetic one
             * (for example with Roman numbers).
             */
            int s = 0;
            for (final String keyword : CUT_BEFORE) {
                int i = b.lastIndexOf(keyword);
                if (i > 0 && (s == 0 || i < s)) s = i;
            }
            for (final String keyword : CUT_AFTER) {
                int i = b.lastIndexOf(keyword);
                if (i >= 0) {
                    i += keyword.length();
                    if (i > s) s = i;
                }
            }
            if (s != 0) b.setLength(s);
            uniformizeZoneNumber(b);
            reducedName = b.toString();
        }

        /**
         * If the string ends with a number optionally followed by "N" or "S", replaces the hemisphere
         * symbol by a sign and makes sure that the number uses at least 3 digits (e.g. "2N" → "+002").
         */
        private static void uniformizeZoneNumber(final StringBuilder b) {
            if (b.indexOf("/") < 0) {
                /*
                 * Do not process names like "WGS 84". We want to process only names like "WGS 84 / UTM zone 2N",
                 * otherwise the replacement of "WGS 84" by "WGS 084" causes unexpected sorting.
                 */
                return;
            }
            int  i = b.length();
            char c = b.charAt(i - 1);
            if (c == ')') {
                // Ignore suffix like " (ftUS)".
                i = b.lastIndexOf(" (");
                if (i < 0) return;
                c = b.charAt(i - 1);
            }
            char sign;
            switch (c) {
                default:            sign =  0;       break;
                case 'e': case 'n': sign = '+'; i--; break;
                case 'w': case 's': sign = '-'; i--; break;
            }
            int upper = i;
            do {
                if (i == 0) return;
                c = b.charAt(--i);
            } while (c >= '0' && c <= '9');
            switch (upper - ++i) {
                case 2: b.insert(i,  '0'); upper++;  break;     // Found 2 digits.
                case 1: b.insert(i, "00"); upper+=2; break;     // Only one digit found.
                case 0: return;                                 // No digit.
            }
            if (sign != 0) {
                b.insert(i, sign);
                upper++;
            }
            b.setLength(upper);
        }

        /**
         * Compares this row with the given row for ordering by name.
         */
        @Override
        public int compareTo(final Row o) {
            int n = reducedName.compareTo(((ByName) o).reducedName);
            if (n == 0) {
                n = super.compareTo(o);
            }
            return n;
        }
    }

    /**
     * Sorts the rows, then inserts sections between CRS instances that use different datums.
     */
    @Override
    protected void sortRows() {
        super.sortRows();
        @SuppressWarnings("SuspiciousToArrayCall")
        final ByName[] data = rows.toArray(new ByName[rows.size()]);
        final Map<String,String> sections = new TreeMap<>();
        for (final ByName row : data) {
            final String section = row.section;
            if (section != null) {
                sections.put(CharSequences.toASCII(section).toString().toLowerCase(), section);
            }
        }
        rows.clear();
        /*
         * Recopy the rows, but section-by-section. We do this sorting here instead than in the Row.compareTo(Row)
         * method in order to preserve the alphabetical order of rows with unknown datum.
         * Algorithm below is inefficient, but this class should be rarely used anyway and only by site maintainer.
         */
        for (final String section : sections.values()) {
            final Row separator = new Row();
            separator.isSectionHeader = true;
            separator.name = section;
            rows.add(separator);
            boolean found = false;
            for (int i=0; i<data.length; i++) {
                final ByName row = data[i];
                if (row != null) {
                    if (row.section != null) {
                        found = section.equals(row.section);
                    }
                    if (found) {
                        rows.add(row);
                        data[i] = null;
                        found = true;
                    }
                }
            }
        }
        boolean found = false;
        for (final ByName row : data) {
            if (row != null) {
                if (!found) {
                    final Row separator = new Row();
                    separator.isSectionHeader = true;
                    separator.name = "Unknown";
                    rows.add(separator);
                }
                rows.add(row);
                found = true;
            }
        }
    }
}
