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
import java.time.OffsetDateTime;
import java.net.URL;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.extent.Extent;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.NilObject;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.metadata.xml.TestUsingFile;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;


/**
 * Tests {@link DefaultExtent}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
@SuppressWarnings("exports")
public final class DefaultExtentTest extends TestUsingFile {
    /**
     * Creates a new test case.
     */
    public DefaultExtentTest() {
    }

    /**
     * Opens the stream to the XML file containing extent information.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     * @return stream opened on the XML document to use for testing purpose.
     */
    public static InputStream openTestFile(final Format format) {
        return format.openTestFile("Extent.xml");
    }

    /**
     * Returns the URL to an arbitrary test file.
     * This is an accessor for tests in other modules.
     *
     * @return URL to an arbitrary test file.
     */
    public static URL getTestFileURL() {
        return Format.XML2007.getURL("Extent.xml");
    }

    /**
     * Tests {@link DefaultExtent#intersect(Extent)}.
     */
    @Test
    public void testIntersect() {
        final var bounds1   = new DefaultGeographicBoundingBox(10, 20, 30, 40);
        final var bounds2   = new DefaultGeographicBoundingBox(16, 18, 31, 42);
        final var clip      = new DefaultGeographicBoundingBox(15, 25, 26, 32);
        final var expected1 = new DefaultGeographicBoundingBox(15, 20, 30, 32);
        final var expected2 = new DefaultGeographicBoundingBox(16, 18, 31, 32);
        final var e1 = new DefaultExtent("Somewhere", bounds1, null, null);
        final var e2 = new DefaultExtent("Somewhere", clip, null, null);
        e1.getGeographicElements().add(bounds2);
        e1.intersect(e2);
        assertEquals("Somewhere", e1.getDescription().toString());
        assertFalse(e1.getDescription() instanceof NilObject);
        assertArrayEquals(new DefaultGeographicBoundingBox[] {expected1, expected2},
                          e1.getGeographicElements().toArray());
        /*
         * Change the description and test again. That description should be considered missing
         * because we have a mismatch. Also change abounding box in such a way that there is no
         * intersection. That bounding box should be omitted.
         */
        bounds2.setBounds(8, 12, 33, 35);
        e1.setGeographicElements(List.of(bounds1, bounds2));
        e2.setDescription(new SimpleInternationalString("Somewhere else"));
        e1.intersect(e2);
        assertTrue(e1.getDescription() instanceof NilObject);
        assertArrayEquals(new DefaultGeographicBoundingBox[] {expected1},
                          e1.getGeographicElements().toArray());
    }

    /**
     * Tests the (un)marshalling of a {@code <gex:EX_Extent>} object.
     * This test opportunistically tests setting {@code "gml:id"} value.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testXML() throws JAXBException {
        roundtrip(Format.XML2016);
    }

    /**
     * Tests the (un)marshalling of a {@code <gmd:EX_Extent>} object using the legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testLegacyXML() throws JAXBException {
        roundtrip(Format.XML2007);
    }

    /**
     * Compares the marshalling and unmarshalling of a {@link DefaultExtent} with XML in the given file.
     */
    private void roundtrip(final Format format) throws JAXBException {
        final var bbox = new DefaultGeographicBoundingBox(-99, -79, 14.9844, 31);
        bbox.getIdentifierMap().put(IdentifierSpace.ID, "bbox");
        final var temporal = new DefaultTemporalExtent(
                OffsetDateTime.parse("2010-01-27T08:26:10-05:00"),
                OffsetDateTime.parse("2010-08-27T08:26:10-05:00"));
        final var extent = new DefaultExtent(null, bbox, null, temporal);
        assertMarshalEqualsFile(openTestFile(format), extent, format.schemaVersion, 0,
                new String[] {"gml:description"},                               // Ignored nodes.
                new String[] {"xmlns:*", "xsi:schemaLocation", "gml:id"});      // Ignored attributes.
        assertEqualsIgnoreMetadata(extent, unmarshalFile(DefaultExtent.class, openTestFile(format)));
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
