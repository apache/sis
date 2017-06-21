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
package org.apache.sis.storage.netcdf;

import java.io.IOException;
import ucar.nc2.dataset.NetcdfDataset;
import org.opengis.metadata.Metadata;
import org.apache.sis.internal.netcdf.TestCase;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.IOTestCase;
import org.apache.sis.internal.netcdf.ucar.DecoderWrapper;
import org.apache.sis.internal.netcdf.impl.ChannelDecoderTest;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.formatNameAndValue;


/**
 * Tests {@link MetadataReader}. This tests uses the SIS embedded implementation and the UCAR library
 * for reading NetCDF attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
@DependsOn({
    ChannelDecoderTest.class,
    org.apache.sis.internal.netcdf.impl.VariableInfoTest.class
})
public final strictfp class MetadataReaderTest extends IOTestCase {
    /**
     * Tests {@link MetadataReader#split(String)}.
     */
    @Test
    public void testSplit() {
        assertArrayEquals(new String[] {"John Doe", "Foo \" Bar", "Jane Lee", "L J Smith, Jr."},
                MetadataReader.split("John Doe, \"Foo \" Bar\" ,Jane Lee,\"L J Smith, Jr.\"").toArray());
    }

    /**
     * Reads the metadata using the NetCDF decoder embedded with SIS,
     * and compares its string representation with the expected one.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testEmbedded() throws IOException, DataStoreException {
        final Metadata metadata;
        try (Decoder input = ChannelDecoderTest.createChannelDecoder(NCEP)) {
            metadata = new MetadataReader(input).read();
        }
        compareToExpected(metadata);
    }

    /**
     * Reads the metadata using the UCAR library and compares
     * its string representation with the expected one.
     *
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testUCAR() throws IOException, DataStoreException {
        final Metadata metadata;
        try (Decoder input = new DecoderWrapper(TestCase.LISTENERS, new NetcdfDataset(open(NCEP)))) {
            metadata = new MetadataReader(input).read();
        }
        compareToExpected(metadata);
    }

    /**
     * Compares the string representation of the given metadata object with the expected one.
     * The given metadata shall have been created from the {@link #NCEP} dataset.
     */
    static void compareToExpected(final Metadata actual) {
        final String text = formatNameAndValue(DefaultMetadata.castOrCopy(actual).asTreeTable());
        assertMultilinesEquals(
            "Metadata\n" +
            "  ├─Contact\n" +
            "  │   ├─Individual…………………………………………………………………………… NOAA/NWS/NCEP\n" +     // TODO: actually we can not distinguish individual from organization.
            "  │   └─Role…………………………………………………………………………………………… Point of contact\n" +
            "  ├─Spatial representation info\n" +
            "  │   ├─Number of dimensions………………………………………………… 3\n" +
            "  │   ├─Axis dimension properties (1 of 3)…………… Column\n" +
            "  │   │   └─Dimension size……………………………………………………… 73\n" +
            "  │   ├─Axis dimension properties (2 of 3)…………… Row\n" +
            "  │   │   └─Dimension size……………………………………………………… 73\n" +
            "  │   ├─Axis dimension properties (3 of 3)…………… Time\n" +
            "  │   │   └─Dimension size……………………………………………………… 1\n" +
            "  │   ├─Cell geometry…………………………………………………………………… Area\n" +
            "  │   └─Transformation parameter availability…… false\n" +
            "  ├─Identification info\n" +
            "  │   ├─Citation………………………………………………………………………………… Sea Surface Temperature Analysis Model\n" +
            "  │   │   ├─Date………………………………………………………………………………… 2005-09-22 00:00:00\n" +
            "  │   │   │   └─Date type………………………………………………………… Creation\n" +
            "  │   │   ├─Identifier………………………………………………………………… NCEP/SST/Global_5x2p5deg/SST_Global_5x2p5deg_20050922_0000.nc\n" +
            "  │   │   │   └─Authority………………………………………………………… edu.ucar.unidata\n" +
            "  │   │   └─Cited responsible party\n" +
            "  │   │       ├─Individual……………………………………………………… NOAA/NWS/NCEP\n" +  // TODO: actually we can not distinguish individual from organization.
            "  │   │       └─Role……………………………………………………………………… Originator\n" +
            "  │   ├─Abstract………………………………………………………………………………… NCEP SST Global 5.0 x 2.5 degree model data\n" +
            "  │   ├─Point of contact\n" +
            "  │   │   ├─Individual………………………………………………………………… NOAA/NWS/NCEP\n" +
            "  │   │   └─Role………………………………………………………………………………… Point of contact\n" +
            "  │   ├─Descriptive keywords\n" +
            "  │   │   ├─Keyword………………………………………………………………………… EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature\n" +
            "  │   │   ├─Type………………………………………………………………………………… Theme\n" +
            "  │   │   └─Thesaurus name……………………………………………………… GCMD Science Keywords\n" +
            "  │   ├─Resource constraints\n" +
            "  │   │   └─Use limitation……………………………………………………… Freely available\n" +
            "  │   ├─Spatial representation type……………………………… Grid\n" +
            "  │   └─Extent\n" +
            "  │       ├─Geographic element\n" +
            "  │       │   ├─West bound longitude…………………………… 180°W\n" +
            "  │       │   ├─East bound longitude…………………………… 180°E\n" +
            "  │       │   ├─South bound latitude…………………………… 90°S\n" +
            "  │       │   ├─North bound latitude…………………………… 90°N\n" +
            "  │       │   └─Extent type code……………………………………… true\n" +
            "  │       └─Vertical element\n" +
            "  │           ├─Minimum value……………………………………………… 0.0\n" +
            "  │           └─Maximum value……………………………………………… 0.0\n" +
            "  ├─Content info\n" +
            "  │   └─Attribute group\n" +
            "  │       └─Attribute…………………………………………………………………… SST\n" +
            "  │           ├─Units…………………………………………………………………… K\n" +
            "  │           ├─Description…………………………………………………… Sea temperature\n" +
            "  │           └─Name……………………………………………………………………… sea_water_temperature\n" +
            "  ├─Data quality info\n" +
            "  │   ├─Scope\n" +
            "  │   │   └─Level……………………………………………………………………………… Dataset\n" +
            "  │   └─Lineage\n" +
            "  │       └─Statement…………………………………………………………………… 2003-04-07 12:12:50 - created by gribtocdl" +
            "              2005-09-26T21:50:00 - edavis - add attributes for dataset discovery\n" +
            "  ├─Metadata scope\n" +
            "  │   └─Resource scope………………………………………………………………… Dataset\n" +
            "  ├─Metadata identifier……………………………………………………………… NCEP/SST/Global_5x2p5deg/SST_Global_5x2p5deg_20050922_0000.nc\n" +
            "  │   └─Authority……………………………………………………………………………… edu.ucar.unidata\n" +
            "  ├─Metadata standard (1 of 2)…………………………………………… Geographic Information — Metadata Part 1: Fundamentals\n" +
            "  │   ├─Edition…………………………………………………………………………………… ISO 19115-1:2014(E)\n" +
            "  │   ├─Identifier…………………………………………………………………………… 19115-1\n" +
            "  │   │   ├─Code space………………………………………………………………… ISO\n" +
            "  │   │   └─Version………………………………………………………………………… 2014(E)\n" +
            "  │   ├─Cited responsible party\n" +
            "  │   │   ├─Organisation…………………………………………………………… International Organization for Standardization\n" +
            "  │   │   └─Role………………………………………………………………………………… Principal investigator\n" +
            "  │   └─Presentation form………………………………………………………… Document digital\n" +
            "  └─Metadata standard (2 of 2)…………………………………………… Geographic Information — Metadata Part 2: Extensions for imagery and gridded data\n" +
            "      ├─Edition…………………………………………………………………………………… ISO 19115-2:2009(E)\n" +
            "      ├─Identifier…………………………………………………………………………… 19115-2\n" +
            "      │   ├─Code space………………………………………………………………… ISO\n" +
            "      │   └─Version………………………………………………………………………… 2009(E)\n" +
            "      ├─Cited responsible party\n" +
            "      │   ├─Organisation…………………………………………………………… International Organization for Standardization\n" +
            "      │   └─Role………………………………………………………………………………… Principal investigator\n" +
            "      └─Presentation form………………………………………………………… Document digital\n", text);
    }
}
