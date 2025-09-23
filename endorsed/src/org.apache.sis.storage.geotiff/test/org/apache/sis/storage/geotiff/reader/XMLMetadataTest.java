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
package org.apache.sis.storage.geotiff.reader;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMultilinesEquals;


/**
 * Tests the {@link XMLMetadata} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class XMLMetadataTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public XMLMetadataTest() {
    }

    /**
     * A GDAL metadata. The format is specific to the GDAL project.
     */
    private static final String GDAL_METADATA =
            "<GDALMetadata>\n" +
            "  <Item name=\"TITLE\">My image</Item>\n" +
            "  <Item name=\"SCALE\" sample=\"-3\">0.015</Item>" +
            "  <Item name=\"acquisitionStartDate\">2018-02-28T03:48:00Z</Item>\n" +
            "  <Item name=\"acquisitionEndDate\">2018-02-28T04:04:00Z</Item>\n" +
            "  <Foo>bar</Foo>\n" +
            "</GDALMetadata>\n";

    /**
     * A DGIWG metadata in ISO 19115 format.
     */
    private static final String GEO_METADATA =
            "<gmd:MD_Metadata\n" +
            "  xmlns:gmd = \"" + LegacyNamespaces.GMD + "\"\n"  +
            "  xmlns:gml = \"" + Namespaces.GML + "\"\n>" +
            "  <gmd:identificationInfo>\n" +
            "    <gmd:MD_DataIdentification>\n" +
            "      <gmd:extent>\n" +
            "        <gmd:EX_Extent>\n" +
            "          <gmd:temporalElement>\n" +
            "            <gmd:EX_TemporalExtent>\n" +
            "              <gmd:extent>\n" +
            "                <gml:TimePeriod>\n" +
            "                  <gml:description>Acquisition period</gml:description>\n" +
            "                  <gml:beginPosition>2018-02-28T03:04:00Z</gml:beginPosition>\n" +
            "                  <gml:endPosition>2018-02-28T04:48:00Z</gml:endPosition>\n" +
            "                </gml:TimePeriod>\n" +
            "              </gmd:extent>\n" +
            "            </gmd:EX_TemporalExtent>\n" +
            "          </gmd:temporalElement>\n" +
            "        </gmd:EX_Extent>\n" +
            "      </gmd:extent>\n" +
            "    </gmd:MD_DataIdentification>\n" +
            "  </gmd:identificationInfo>\n" +
            "</gmd:MD_Metadata>\n";

    /**
     * Tests parsing of GDAL metadata and formatting as a tree table.
     * THe XML document is like below:
     *
     * {@snippet lang="xml" :
     *   <GDALMetadata>
     *     <Item name="SCALE" sample="-3">0.015</Item>
     *     <Item name="acquisitionStartDate">2018-02-28T04:48:00</Item>
     *     <Item name="acquisitionEndDate">2018-02-28T03:04:00</Item>
     *   </GDALMetadata>
     *   }
     *
     * The tree table output is expected to <em>not</em> contains the "Item" word,
     * because this is redundancy repeated for all nodes. Instead, "Item" should
     * be replaced by the value of the "name" attribute.
     */
    @Test
    public void testTreeGDAL() {
        XMLMetadata xml = new XMLMetadata(GDAL_METADATA, true);
        assertSame(GDAL_METADATA, xml.toString());
        assertFalse(xml.isEmpty());
        DefaultTreeTable     table = new DefaultTreeTable(TableColumn.NAME, TableColumn.VALUE);
        DefaultTreeTable.Node root = (DefaultTreeTable.Node ) table.getRoot();
        DefaultTreeTable.Node node = new XMLMetadata.Root(xml, root, "Test");
        assertEquals("Test", node.getValue(TableColumn.NAME));
        root.setValue(TableColumn.NAME, "Root");
        assertMultilinesEquals(
                "Root\n" +
                "  └─Test\n" +
                "      └─GDALMetadata\n" +
                "          ├─TITLE…………………………………………… My image\n" +
                "          ├─SCALE…………………………………………… 0.015\n" +
                "          │   └─sample……………………………… -3\n" +
                "          ├─acquisitionStartDate…… 2018-02-28T03:48:00Z\n" +
                "          ├─acquisitionEndDate………… 2018-02-28T04:04:00Z\n" +
                "          └─Foo………………………………………………… bar\n",
                table.toString());
    }

    /**
     * Tests parsing GDAL metadata and conversion to ISO 19115 metadata.
     *
     * @throws Exception if an error occurred during XML parsing.
     */
    @Test
    public void testMetadataGDAL() throws Exception {
        XMLMetadata xml = new XMLMetadata(GDAL_METADATA, true);
        MetadataBuilder builder = new MetadataBuilder();
        assertNull(xml.appendTo(builder));
        DefaultMetadata metadata = builder.build();
        assertMultilinesEquals(
                "Metadata\n" +
                "  └─Identification info\n" +
                "      ├─Citation……………………………… My image\n" +
                "      └─Extent\n" +
                "          └─Temporal element\n" +
                "              └─Extent……………… 2018-02-28T03:48:00Z/2018-02-28T04:04:00Z\n",
                metadata.toString());
    }

    /**
     * Tests parsing DGIWG metadata and conversion to ISO 19115 metadata.
     *
     * @throws Exception if an error occurred during XML parsing.
     */
    @Test
    public void testGeoMetadata() throws Exception {
        XMLMetadata xml = new XMLMetadata(GEO_METADATA, false);
        MetadataBuilder builder = new MetadataBuilder();
        assertNull(xml.appendTo(builder));
        DefaultMetadata metadata = builder.build();
        assertMultilinesEquals(
                "Metadata\n" +
                "  └─Identification info\n" +
                "      └─Extent\n" +
                "          └─Temporal element\n" +
                "              └─Extent……………… 2018-02-28T03:04Z/2018-02-28T04:48Z\n",
                metadata.toString());
    }
}
