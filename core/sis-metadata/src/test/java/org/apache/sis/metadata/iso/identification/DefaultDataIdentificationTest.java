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

import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.constraint.DefaultConstraints;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultDataIdentification}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn({
    org.apache.sis.metadata.PropertyAccessorTest.class, // For properties order
    org.apache.sis.metadata.iso.citation.DefaultCitationTest.class,
    org.apache.sis.metadata.iso.citation.DefaultCitationDateTest.class,
    org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBoxTest.class,
    DefaultKeywordsTest.class
})
public final strictfp class DefaultDataIdentificationTest extends TestCase {
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
        final DefaultCitation citation = new DefaultCitation("Sea Surface Temperature Analysis Model");
        citation.setDates(singleton(new DefaultCitationDate(TestUtilities.date("2005-09-22 00:00:00"), DateType.CREATION)));
        citation.setIdentifiers(singleton(new DefaultIdentifier("SST_Global.nc")));
        /*
         * Descriptive keywords
         *   ├─Keyword………………………………………………………………………… EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature
         *   ├─Type………………………………………………………………………………… Theme
         *   └─Thesaurus name
         *       └─Title…………………………………………………………………… GCMD Science Keywords
         */
        final DefaultKeywords keywords = new DefaultKeywords(
                "EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature");
        keywords.setType(KeywordType.THEME);
        keywords.setThesaurusName(new DefaultCitation("GCMD Science Keywords"));
        /*
         * Identification info
         *  ├─(above objects)
         *  ├─Abstract………………………………………………………………………………… NCEP SST Global 5.0 x 2.5 degree model data
         *  ├─Descriptive keywords
         *  │   ├─Keyword………………………………………………………………………… EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature
         *  │   ├─Type………………………………………………………………………………… Theme
         *  │   └─Thesaurus name
         *  │       └─Title…………………………………………………………………… GCMD Science Keywords
         *  ├─Resource constraints
         *  │   └─Use limitation……………………………………………………… Freely available
         *  ├─Spatial representation type……………………………… Grid
         *  └─Extent
         *      └─Geographic element
         *          ├─West bound longitude…………………………… 180°W
         *          ├─East bound longitude…………………………… 180°E
         *          ├─South bound latitude…………………………… 90°S
         *          ├─North bound latitude…………………………… 90°N
         *          └─Extent type code……………………………………… true
         */
        final DefaultDataIdentification info = new DefaultDataIdentification(citation,
                "NCEP SST Global 5.0 x 2.5 degree model data", null, null);
        info.setSpatialRepresentationTypes(singleton(SpatialRepresentationType.GRID));
        info.setDescriptiveKeywords(singleton(keywords));
        info.setResourceConstraints(singleton(new DefaultConstraints("Freely available")));
        info.setExtents(singleton(Extents.WORLD));
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
        assertMultilinesEquals(
                "Data identification\n" +
                "  ├─Citation\n" +
                "  │   ├─Title……………………………………………………… Sea Surface Temperature Analysis Model\n" +
                "  │   ├─Date\n" +
                "  │   │   ├─Date……………………………………………… 2005-09-22 00:00:00\n" +
                "  │   │   └─Date type………………………………… Creation\n" +
                "  │   └─Identifier\n" +
                "  │       └─Code……………………………………………… SST_Global.nc\n" +
                "  ├─Abstract………………………………………………………… NCEP SST Global 5.0 x 2.5 degree model data\n" +
                "  ├─Descriptive keywords\n" +
                "  │   ├─Keyword………………………………………………… EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature\n" +
                "  │   ├─Type………………………………………………………… Theme\n" +
                "  │   └─Thesaurus name\n" +
                "  │       └─Title…………………………………………… GCMD Science Keywords\n" +
                "  ├─Resource constraints\n" +
                "  │   └─Use limitation……………………………… Freely available\n" +
                "  ├─Spatial representation type……… Grid\n" +
                "  └─Extent\n" +
                "      ├─Description……………………………………… World\n" +
                "      └─Geographic element\n" +
                "          ├─West bound longitude…… 180°W\n" +
                "          ├─East bound longitude…… 180°E\n" +
                "          ├─South bound latitude…… 90°S\n" +
                "          ├─North bound latitude…… 90°N\n" +
                "          └─Extent type code……………… true\n",
            TestUtilities.formatNameAndValue(create().asTreeTable()));
    }
}
