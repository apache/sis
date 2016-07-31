/*
 * Copyright 2016 haonguyen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.earthobservation;

import java.io.File;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.metadata.Metadata;

import static org.apache.sis.test.Assert.assertMultilinesEquals;
import static org.apache.sis.test.TestUtilities.formatNameAndValue;


/**
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class ModisReaderTest extends TestCase {
    /**
     * Tests {@link ModisReader#read()}.
     *
     * @throws IOException if an error occurred while reading the test file.
     * @throws DataStoreException if a property value can not be parsed as a
     * number or a date.
     */
    @Test
    public void testRead() throws Exception {
        // TODO
        final Metadata actual;
        File in = new File(ModisReader.class.getResource("Modis.xml").toURI());
         actual = new ModisReader(in).read();

        final String text = formatNameAndValue(DefaultMetadata.castOrCopy(actual).asTreeTable());
        assertMultilinesEquals(
                "Metadata\n"
                + "  ├─Language…………………………………………………………………………… en\n"
                + "  ├─Identification info\n"
                + "  │   ├─Citation\n"
                + "  │   │   ├─Title……………………………………………………………… MOD09Q1.A2010009.h08v07.005.2010027023253.hdf.xml\n"
                + "  │   │   └─Date\n"
                + "  │   │       ├─Date……………………………………………………… 2010-01-26 19:32:53\n"
                + "  │   │       └─Date type………………………………………… Publication\n"
                + "  │   ├─Point of contact (1 of 2)\n"
                + "  │   │   ├─Role………………………………………………………………… Originator\n"
                + "  │   │   └─Party\n"
                + "  │   │       └─Name……………………………………………………… EDC\n"
                + "  │   ├─Point of contact (2 of 2)\n"
                + "  │   │   ├─Role………………………………………………………………… Author\n"
                + "  │   │   └─Party\n"
                + "  │   │       └─Name……………………………………………………… EDC\n"
                + "  │   ├─Descriptive keywords\n"
                + "  │   │   └─Keyword………………………………………………………… Land/Water Mask > Cloud Mask > Atmospheric > Land Cover > Snow Cover\n"
                + "  │   ├─Extent\n"
                + "  │   │   └─Geographic element\n"
                + "  │   │       ├─West bound longitude…………… 101°33′13.2641574084″W\n"
                + "  │   │       ├─East bound longitude…………… 91°02′44.0791018375″W\n"
                + "  │   │       ├─South bound latitude…………… 9°58′17.31652206111″N\n"
                + "  │   │       ├─North bound latitude…………… 19°59′59.999993534″N\n"
                + "  │   │       └─Extent type code……………………… true\n"
                + "  │   └─Associated resource\n"
                + "  │       └─Name\n"
                + "  │           └─Title…………………………………………………… EDC\n"
                + "  ├─Distribution info\n"
                + "  │   └─Distribution format\n"
                + "  │       └─Format specification citation\n"
                + "  │           └─Alternate title………………………… MOD09Q1\n"
                + "  ├─Date info\n"
                + "  │   ├─Date…………………………………………………………………………… 2010-01-26 19:32:53\n"
                + "  │   └─Date type……………………………………………………………… Creation\n"
                + "  ├─Metadata scope\n"
                + "  │   └─Resource scope………………………………………………… Terra\n"
                + "  ├─Metadata identifier\n"
                + "  │   └─Code…………………………………………………………………………… MOD09Q1.A2010009.h08v07.005.2010027023253.hdf\n"
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
