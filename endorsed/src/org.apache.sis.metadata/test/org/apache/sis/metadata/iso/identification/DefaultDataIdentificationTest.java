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
package org.apache.sis.metadata.iso.identification;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.constraint.DefaultConstraints;
import org.apache.sis.metadata.iso.extent.Extents;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertTitleEquals;
import static org.apache.sis.metadata.TreeTableViewTest.assertMetadataTreeEquals;


/**
 * Tests {@link DefaultDataIdentification}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultDataIdentificationTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultDataIdentificationTest() {
    }

    /**
     * Creates the instance to test.
     */
    private static DefaultDataIdentification create() {
        /*
         * Citation
         *   ├─Title……………………………………………………………………………… Sea Surface Temperature Analysis Model
         *   ├─Date
         *   │   ├─Date……………………………………………………………………… Sep 22, 2005 00:00:00 AM
         *   │   └─Date type………………………………………………………… Creation
         *   └─Identifier
         *       └─Code……………………………………………………………………… NCEP/SST/Global_5x2p5deg/SST_Global_5x2p5deg_20050922_0000.nc
         */
        final var citation = new DefaultCitation("Sea Surface Temperature Analysis Model");
        citation.setDates(Set.of(new DefaultCitationDate(LocalDate.of(2005, 9, 22), DateType.CREATION)));
        citation.setIdentifiers(Set.of(new DefaultIdentifier("SST_Global.nc")));
        /*
         * Descriptive keywords
         *   ├─Keyword………………………………………………………………………… EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature
         *   ├─Type………………………………………………………………………………… Theme
         *   └─Thesaurus name
         *       └─Title…………………………………………………………………… GCMD Science Keywords
         */
        final var keywords = new DefaultKeywords("EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature");
        keywords.setType(KeywordType.THEME);
        keywords.setThesaurusName(new DefaultCitation("GCMD Science Keywords"));
        /*
         * Identification info
         *  ├─(above objects)
         *  ├─Abstract………………………………………………………………………………… Global 5.0 x 2.5 degree model data
         *  ├─Descriptive keywords
         *  │   ├─Keyword………………………………………………………………………… EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature
         *  │   ├─Type………………………………………………………………………………… Theme
         *  │   └─Thesaurus name
         *  │       └─Title…………………………………………………………………… GCMD Science Keywords
         *  ├─Resource constraints
         *  │   └─Use limitation……………………………………………………… Freely available
         *  ├─Spatial representation type……………………………… Grid
         *  ├─Locale (1 of 2)……………………………………………………………… en_US
         *  │   └─Character set………………………………………………………… US-ASCII
         *  ├─Locale (2 of 2)……………………………………………………………… fr
         *  │   └─Character set………………………………………………………… ISO-8859-1
         *  └─Extent
         *      └─Geographic element
         *          ├─West bound longitude…………………………… 180°W
         *          ├─East bound longitude…………………………… 180°E
         *          ├─South bound latitude…………………………… 90°S
         *          ├─North bound latitude…………………………… 90°N
         *          └─Extent type code……………………………………… true
         */
        final var info = new DefaultDataIdentification(citation,
                "Global 5.0 x 2.5 degree model data", null, null);
        info.setSpatialRepresentationTypes(Set.of(SpatialRepresentationType.GRID));
        info.setDescriptiveKeywords(Set.of(keywords));
        info.setResourceConstraints(Set.of(new DefaultConstraints("Freely available")));
        info.getLocalesAndCharsets().put(Locale.US,     StandardCharsets.US_ASCII);
        info.getLocalesAndCharsets().put(Locale.FRENCH, StandardCharsets.ISO_8859_1);
        info.setExtents(Set.of(Extents.WORLD));
        return info;
    }

    /**
     * Tests {@link DefaultDataIdentification#toString()}.
     * This is an integration tests for (among others):
     *
     * <ul>
     *   <li>Property order</li>
     *   <li>Date formatting</li>
     *   <li>Angle formatting</li>
     * </ul>
     */
    @Test
    public void testToString() {
        assertMetadataTreeEquals(
                "Data identification\n" +
                "  ├─Citation………………………………………………………… Sea Surface Temperature Analysis Model\n" +
                "  │   ├─Date………………………………………………………… 2005 Sep 22\n" +
                "  │   │   └─Date type………………………………… Creation\n" +
                "  │   └─Identifier………………………………………… SST_Global.nc\n" +
                "  ├─Abstract………………………………………………………… Global 5.0 x 2.5 degree model data\n" +
                "  ├─Spatial representation type……… Grid\n" +
                "  ├─Extent……………………………………………………………… World\n" +
                "  │   └─Geographic element\n" +
                "  │       ├─West bound longitude…… 180°W\n" +
                "  │       ├─East bound longitude…… 180°E\n" +
                "  │       ├─South bound latitude…… 90°S\n" +
                "  │       ├─North bound latitude…… 90°N\n" +
                "  │       └─Extent type code……………… True\n" +
                "  ├─Descriptive keywords\n" +
                "  │   ├─Keyword………………………………………………… EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature\n" +
                "  │   ├─Type………………………………………………………… Theme\n" +
                "  │   └─Thesaurus name……………………………… GCMD Science Keywords\n" +
                "  ├─Resource constraints\n" +
                "  │   └─Use limitation……………………………… Freely available\n" +
                "  ├─Locale (1 of 2)……………………………………… en_US\n" +
                "  │   └─Character set………………………………… US-ASCII\n" +
                "  └─Locale (2 of 2)……………………………………… fr\n" +
                "      └─Character set………………………………… ISO-8859-1\n",
            create().asTreeTable());
    }

    /**
     * Tests {@link DefaultDataIdentification#asMap()}, in particular on the {@code "language"} property.
     * This property still use the UML identifier of ISO 19115:2003.
     */
    @Test
    public void testValueMap() {
        final DefaultDataIdentification info = create();
        final Map<String,Object> map = info.asMap();
        assertEquals("Global 5.0 x 2.5 degree model data", map.get("abstract").toString());
        assertTitleEquals("Sea Surface Temperature Analysis Model",
                assertInstanceOf(Citation.class, map.get("citation")), "citation");
        assertEquals(Set.of(SpatialRepresentationType.GRID), map.get("spatialRepresentationType"));

        final Locale[] locales = {Locale.US, Locale.FRENCH};
        assertArrayEquals(locales, ((Collection<?>) map.get("language")).toArray());
        assertArrayEquals(locales, ((Collection<?>) map.get("languages")).toArray());
        assertArrayEquals(locales, ((Collection<?>) map.get("getLanguages")).toArray());
    }
}
