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
package org.apache.sis.storage.geotiff;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.ParseException;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.Metadata;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.assertMultilinesEquals;
import static org.apache.sis.test.TestUtilities.formatNameAndValue;
import org.opengis.util.FactoryException;

/**
 * Tests {@link LandsatReader}.
 *
 * @author Thi Phuong Hao Nguyen (VNSC)
 * @since 0.8
 * @version 0.8
 * @module
 */
public class LandsatReaderTest extends TestCase {

    /**
     * Tests {@link LandsatReader#read()}.
     *
     * @throws IOException if an error occurred while reading the test file.
     * @throws DataStoreException if a property value can not be parsed as a
     * number or a date.
     */
    @Test
    public void testRead() throws IOException, DataStoreException, ParseException, FactoryException, JAXBException {
        // TODO 
        final Metadata reade;
        try (BufferedReader in = new BufferedReader(new FileReader("/home/haonguyen/data/LC81230522014071LGN00_MTL.txt"))) {
            reade = new LandsatReader(in).read();
        }
        System.out.println("The Metadata of LC81230522014071LGN00_MTL.txt is:");
        System.out.println(reade);
        compareToExpected(reade);
    }

    static void compareToExpected(final Metadata actual) {
        {
            final String text = formatNameAndValue(DefaultMetadata.castOrCopy(actual).asTreeTable()); 
        assertMultilinesEquals(
                    "Metadata\n"
                    + "  ├─Language…………………………………………………………………………… en\n"
                    + "  ├─Identification info\n"
                    + "  │   ├─Citation                         \n"
                    + "  │   │   ├─Title……………………………………………………………… LC81230522014071LGN00_MTL.txt\n"
                    + "  │   │   └─Date                         \n"
                    + "  │   │       ├─Date……………………………………………………… 2014-03-12 06:06:35\n"
                    + "  │   │       └─Date type………………………………………… Publication\n"
                    + "  │   ├─Point of contact (1 of 3)\n"
                    + "  │   │   ├─Role………………………………………………………………… Originator\n"
                    + "  │   │   └─Party\n"
                    + "  │   │       └─Name……………………………………………………… Image courtesy of the U.S. Geological Survey\n"
                    + "  │   ├─Point of contact (2 of 3)\n"
                    + "  │   │   ├─Role………………………………………………………………… Publisher\n"
                    + "  │   │   └─Party\n"
                    + "  │   │       └─Name……………………………………………………… Image courtesy of the U.S. Geological Survey\n"
                    + "  │   ├─Point of contact (3 of 3)\n"
                    + "  │   │   ├─Role………………………………………………………………… Author\n"
                    + "  │   │   └─Party\n"
                    + "  │   │       └─Name……………………………………………………… Image courtesy of the U.S. Geological Survey\n"
                    + "  │   ├─Descriptive keywords\n"
                    + "  │   │   └─Keyword………………………………………………………… GLS2000\n"
                    + "  │   └─Extent\n"
                    + "  │       └─Geographic element\n"
                    + "  │           ├─West bound longitude…………… 108°20′10.464″E\n"
                    + "  │           ├─East bound longitude…………… 110°26′39.66″E\n"
                    + "  │           ├─South bound latitude…………… 10°29′59.604″N\n"
                    + "  │           ├─North bound latitude…………… 12°37′25.716″N\n"
                    + "  │           └─Extent type code……………………… true\n"
                    + "  ├─Content info\n"
                    + "  │   ├─Illumination elevation angle…………… 58.80866057\n"
                    + "  │   ├─Illumination azimuth angle………………… 116.88701534\n"
                    + "  │   ├─Cloud cover percentage…………………………… 8.34\n"
                    + "  │   └─Attribute group\n"
                    + "  │       ├─Content type…………………………………………… Physical measurement\n"
                    + "  │       ├─Attribute (1 of 11)\n"
                    + "  │       │   ├─Peak response……………………………… 433.0\n"
                    + "  │       │   ├─Bound units…………………………………… nm\n"
                    + "  │       │   └─Description…………………………………… Coastal Aerosol\n"
                    + "  │       ├─Attribute (2 of 11)\n"
                    + "  │       │   ├─Peak response……………………………… 482.0\n"
                    + "  │       │   ├─Bound units…………………………………… nm\n"
                    + "  │       │   └─Description…………………………………… Blue\n"
                    + "  │       ├─Attribute (3 of 11)\n"
                    + "  │       │   ├─Peak response……………………………… 562.0\n"
                    + "  │       │   ├─Bound units…………………………………… nm\n"
                    + "  │       │   └─Description…………………………………… Green\n"
                    + "  │       ├─Attribute (4 of 11)\n"
                    + "  │       │   ├─Peak response……………………………… 655.0\n"
                    + "  │       │   ├─Bound units…………………………………… nm\n"
                    + "  │       │   └─Description…………………………………… Red\n"
                    + "  │       ├─Attribute (5 of 11)\n"
                    + "  │       │   ├─Peak response……………………………… 865.0\n"
                    + "  │       │   ├─Bound units…………………………………… nm\n"
                    + "  │       │   └─Description…………………………………… Near-Infrared\n"
                    + "  │       ├─Attribute (6 of 11)\n"
                    + "  │       │   ├─Peak response……………………………… 1610.0\n"
                    + "  │       │   ├─Bound units…………………………………… nm\n"
                    + "  │       │   └─Description…………………………………… Short Wavelength Infrared (SWIR) 1\n"
                    + "  │       ├─Attribute (7 of 11)\n"
                    + "  │       │   ├─Peak response……………………………… 2200.0\n"
                    + "  │       │   ├─Bound units…………………………………… nm\n"
                    + "  │       │   └─Description…………………………………… Short Wavelength Infrared (SWIR) 2\n"
                    + "  │       ├─Attribute (8 of 11)\n"
                    + "  │       │   ├─Peak response……………………………… 590.0\n"
                    + "  │       │   ├─Bound units…………………………………… nm\n"
                    + "  │       │   └─Description…………………………………… Panchromatic\n"
                    + "  │       ├─Attribute (9 of 11)\n"
                    + "  │       │   ├─Peak response……………………………… 1375.0\n"
                    + "  │       │   ├─Bound units…………………………………… nm\n"
                    + "  │       │   └─Description…………………………………… Cirrus\n"
                    + "  │       ├─Attribute (10 of 11)\n"
                    + "  │       │   ├─Peak response……………………………… 10800.0\n"
                    + "  │       │   ├─Bound units…………………………………… nm\n"
                    + "  │       │   └─Description…………………………………… Thermal Infrared Sensor (TIRS) 1\n"
                    + "  │       └─Attribute (11 of 11)\n"
                    + "  │           ├─Peak response……………………………… 12000.0\n"
                    + "  │           ├─Bound units…………………………………… nm\n"
                    + "  │           └─Description…………………………………… Thermal Infrared Sensor (TIRS) 2\n"
                    + "  ├─Distribution info\n"
                    + "  │   └─Distribution format\n"
                    + "  │       └─Format specification citation\n"
                    + "  │           └─Alternate title………………………… GEOTIFF\n"
                    + "  ├─Acquisition information\n"
                    + "  │   ├─Operation\n"
                    + "  │   │   ├─Status…………………………………………………………… Completed\n"
                    + "  │   │   ├─Type………………………………………………………………… Real\n"
                    + "  │   │   └─Significant event\n"
                    + "  │   │       └─Time……………………………………………………… 2014-03-12 03:02:01\n"
                    + "  │   └─Platform\n"
                    + "  │       ├─Identifier\n"
                    + "  │       │   └─Code……………………………………………………… LANDSAT_8\n"
                    + "  │       └─Instrument\n"
                    + "  │           └─Identifier\n"
                    + "  │               └─Code…………………………………………… OLI_TIRS\n"
                    + "  ├─Date info\n"
                    + "  │   ├─Date…………………………………………………………………………… 2014-03-12 06:06:35\n"
                    + "  │   └─Date type……………………………………………………………… Creation\n"
                    + "  ├─Metadata scope\n"  
                    + "  │   └─Resource scope………………………………………………… L1T\n"
                    + "  ├─Metadata identifier\n"
                    + "  │   └─Code…………………………………………………………………………… LC81230522014071LGN00\n"
                    + "  ├─Metadata standard (1 of 2)\n"
                    + "  │   ├─Title………………………………………………………………………… Geographic Information — Metadata Part 1: Fundamentals\n"
                    + "  │   ├─Cited responsible party\n"
                    + "  │   │   ├─Party\n"
                    + "  │   │   │   └─Name……………………………………………………… International Organization for Standardization\n"
                    + "  │   │   └─Role………………………………………………………………… Principal investigator\n"
                    + "  │   ├─Edition…………………………………………………………………… ISO 19115-1:2014(E)\n"
                    + "  │   ├─Identifier\n"
                    + "  │   │   ├─Code………………………………………………………………… 19115-1\n"
                    + "  │   │   ├─Code space………………………………………………… ISO\n"
                    + "  │   │   └─Version………………………………………………………… 2014(E)\n"
                    + "  │   └─Presentation form………………………………………… Document digital\n"
                    + "  └─Metadata standard (2 of 2)\n"
                    + "      ├─Title………………………………………………………………………… Geographic Information — Metadata Part 2: Extensions for imagery and gridded data\n"
                    + "      ├─Cited responsible party\n"
                    + "      │   ├─Party\n"
                    + "      │   │   └─Name……………………………………………………… International Organization for Standardization\n"
                    + "      │   └─Role………………………………………………………………… Principal investigator\n"
                    + "      ├─Edition…………………………………………………………………… ISO 19115-2:2009(E)\n"
                    + "      ├─Identifier\n"
                    + "      │   ├─Code………………………………………………………………… 19115-2\n"
                    + "      │   ├─Code space………………………………………………… ISO\n"
                    + "      │   └─Version………………………………………………………… 2009(E)\n"
                    + "      └─Presentation form………………………………………… Document digital\n", text);
        }
    }
}
