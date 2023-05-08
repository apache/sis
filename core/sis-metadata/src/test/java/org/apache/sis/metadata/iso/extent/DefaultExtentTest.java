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

import java.util.List;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.extent.Extent;
import org.apache.sis.util.Version;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.NilObject;
import org.apache.sis.metadata.xml.TestUsingFile;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;
import static org.apache.sis.test.TestUtilities.date;


/**
 * Tests {@link DefaultExtent}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.3
 */
@DependsOn(DefaultGeographicBoundingBoxTest.class)
public final class DefaultExtentTest extends TestUsingFile {
    /**
     * An XML file containing extent information.
     */
    public static final String FILENAME = "Extent.xml";

    /**
     * Tests {@link DefaultExtent#intersect(Extent)}.
     */
    @Test
    public void testIntersect() {
        final DefaultGeographicBoundingBox bounds1   = new DefaultGeographicBoundingBox(10, 20, 30, 40);
        final DefaultGeographicBoundingBox bounds2   = new DefaultGeographicBoundingBox(16, 18, 31, 42);
        final DefaultGeographicBoundingBox clip      = new DefaultGeographicBoundingBox(15, 25, 26, 32);
        final DefaultGeographicBoundingBox expected1 = new DefaultGeographicBoundingBox(15, 20, 30, 32);
        final DefaultGeographicBoundingBox expected2 = new DefaultGeographicBoundingBox(16, 18, 31, 32);
        final DefaultExtent e1 = new DefaultExtent("Somewhere", bounds1, null, null);
        final DefaultExtent e2 = new DefaultExtent("Somewhere", clip, null, null);
        e1.getGeographicElements().add(bounds2);
        e1.intersect(e2);
        assertEquals("description", "Somewhere", e1.getDescription().toString());
        assertFalse("isNil(description)", e1.getDescription() instanceof NilObject);
        assertArrayEquals("geographicElements", new DefaultGeographicBoundingBox[] {
            expected1, expected2
        }, e1.getGeographicElements().toArray());
        /*
         * Change the description and test again. That description should be considered missing
         * because we have a mismatch. Also change abounding box in such a way that there is no
         * intersection. That bounding box should be omitted.
         */
        bounds2.setBounds(8, 12, 33, 35);
        e1.setGeographicElements(List.of(bounds1, bounds2));
        e2.setDescription(new SimpleInternationalString("Somewhere else"));
        e1.intersect(e2);
        assertTrue("isNil(description)", e1.getDescription() instanceof NilObject);
        assertArrayEquals("geographicElements", new DefaultGeographicBoundingBox[] {
            expected1
        }, e1.getGeographicElements().toArray());
    }

    /**
     * Tests the (un)marshalling of a {@code <gex:EX_Extent>} object.
     * This test opportunistically tests setting {@code "gml:id"} value.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testXML() throws JAXBException {
        roundtrip(XML2016+FILENAME, VERSION_2014);
    }

    /**
     * Tests the (un)marshalling of a {@code <gmd:EX_Extent>} object using the legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testLegacyXML() throws JAXBException {
        roundtrip(XML2007+FILENAME, VERSION_2007);
    }

    /**
     * Compares the marshalling and unmarshalling of a {@link DefaultExtent} with XML in the given file.
     */
    private void roundtrip(final String filename, final Version version) throws JAXBException {
        final DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox(-99, -79, 14.9844, 31);
        bbox.getIdentifierMap().put(IdentifierSpace.ID, "bbox");
        final DefaultTemporalExtent temporal = new DefaultTemporalExtent();
        if (PENDING_FUTURE_SIS_VERSION) {
            // This block needs a more complete sis-temporal module.
            temporal.setBounds(date("2010-01-27 13:26:10"), date("2010-08-27 13:26:10"));
        }
        final DefaultExtent extent = new DefaultExtent(null, bbox, null, temporal);
        assertMarshalEqualsFile(filename, extent, version, "xmlns:*", "xsi:schemaLocation");
        assertEquals(extent, unmarshalFile(DefaultExtent.class, filename));
    }

    /**
     * Tests XML marshalling of the {@link Extents#WORLD} constant, which is a {@code DefaultExtent} instance.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testWorldConstant() throws JAXBException {
        final String xml = marshal(Extents.WORLD);
        assertXmlEquals("<gex:EX_Extent" +
                " xmlns:gex=\"" + Namespaces.GEX + '"' +
                " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gex:description>\n" +
                "    <gco:CharacterString>World</gco:CharacterString>\n" +
                "  </gex:description>\n" +
                "  <gex:geographicElement>\n" +
                "    <gex:EX_GeographicBoundingBox>\n" +
                "      <gex:extentTypeCode>    <gco:Boolean> true </gco:Boolean></gex:extentTypeCode>\n" +
                "      <gex:westBoundLongitude><gco:Decimal> -180 </gco:Decimal></gex:westBoundLongitude>\n" +
                "      <gex:eastBoundLongitude><gco:Decimal>  180 </gco:Decimal></gex:eastBoundLongitude>\n" +
                "      <gex:southBoundLatitude><gco:Decimal>  -90 </gco:Decimal></gex:southBoundLatitude>\n" +
                "      <gex:northBoundLatitude><gco:Decimal>   90 </gco:Decimal></gex:northBoundLatitude>\n" +
                "    </gex:EX_GeographicBoundingBox>\n" +
                "  </gex:geographicElement>\n" +
                "</gex:EX_Extent>",
                xml, "xmlns:*", "xsi:schemaLocation");
    }
}
