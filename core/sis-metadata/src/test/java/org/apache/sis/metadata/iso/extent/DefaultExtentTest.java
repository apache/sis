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
package org.apache.sis.metadata.iso.extent;

import java.net.URL;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.xml.Namespaces;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.date;


/**
 * Tests {@link DefaultExtent}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
@DependsOn(DefaultGeographicBoundingBoxTest.class)
public final strictfp class DefaultExtentTest extends XMLTestCase {
    /**
     * Returns the URL to the XML file of the given name.
     * The argument shall be one of the files listed in the following directory:
     *
     * <ul>
     *   <li>{@code "core/sis-metadata/src/test/resources/org/apache/sis/metadata/iso/extent"}</li>
     * </ul>
     *
     * @param  filename The name of the XML file.
     * @return The URL to the given XML file.
     */
    public static URL getResource(final String filename) {
        final URL resource = DefaultExtentTest.class.getResource(filename);
        assertNotNull(filename, resource);
        return resource;
    }

    /**
     * Tests the (un)marshalling of a {@code <gmd:EX_Extent>} object.
     * This test opportunistically tests setting {@code "gml:id"} value.
     *
     * <p><b>XML test file:</b>
     * {@code "core/sis-metadata/src/test/resources/org/apache/sis/metadata/iso/extent/Extent.xml"}</p>
     *
     * @throws IOException   If an error occurred while reading the XML file.
     * @throws JAXBException If an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testXML() throws IOException, JAXBException {
        final DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox(-99, -79, 14.9844, 31);
        bbox.getIdentifierMap().put(IdentifierSpace.ID, "bbox");
        final DefaultTemporalExtent temporal = new DefaultTemporalExtent();
        if (PENDING_FUTURE_SIS_VERSION) {
            // This block needs sis-temporal module.
            temporal.setBounds(date("2010-01-27 13:26:10"), date("2010-08-27 13:26:10"));
        }
        final DefaultExtent extent = new DefaultExtent(null, bbox, null, temporal);
        /*
         * XML marshalling, and compare with the content of "ProcessStep.xml" file.
         */
        final String xml = marshal(extent);
        assertTrue(xml.startsWith("<?xml"));
        assertXmlEquals(getResource("Extent.xml"), xml, "xmlns:*", "xsi:schemaLocation");
        /*
         * Final comparison: ensure that we didn't lost any information.
         */
        assertEquals(extent, unmarshal(DefaultExtent.class, xml));
    }

    /**
     * Tests XML marshalling of the {@link Extents#WORLD} constant, which is a {@code DefaultExtent} instance.
     *
     * @throws JAXBException If an error occurred during the during marshalling / unmarshalling processes.
     *
     * @since 0.6
     */
    @Test
    public void testWorldConstant() throws JAXBException {
        final String xml = marshal(Extents.WORLD);
        assertXmlEquals("<gmd:EX_Extent" +
                " xmlns:gco=\"" + Namespaces.GCO + '"' +
                " xmlns:gmd=\"" + Namespaces.GMD + "\">\n" +
                "  <gmd:description>\n" +
                "    <gco:CharacterString>World</gco:CharacterString>\n" +
                "  </gmd:description>\n" +
                "  <gmd:geographicElement>\n" +
                "    <gmd:EX_GeographicBoundingBox>\n" +
                "      <gmd:extentTypeCode>    <gco:Boolean> true </gco:Boolean></gmd:extentTypeCode>\n" +
                "      <gmd:westBoundLongitude><gco:Decimal> -180 </gco:Decimal></gmd:westBoundLongitude>\n" +
                "      <gmd:eastBoundLongitude><gco:Decimal>  180 </gco:Decimal></gmd:eastBoundLongitude>\n" +
                "      <gmd:southBoundLatitude><gco:Decimal>  -90 </gco:Decimal></gmd:southBoundLatitude>\n" +
                "      <gmd:northBoundLatitude><gco:Decimal>   90 </gco:Decimal></gmd:northBoundLatitude>\n" +
                "    </gmd:EX_GeographicBoundingBox>\n" +
                "  </gmd:geographicElement>\n" +
                "</gmd:EX_Extent>",
                xml, "xmlns:*", "xsi:schemaLocation");
    }
}
