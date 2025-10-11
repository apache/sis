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
package org.apache.sis.metadata.iso.content;

import jakarta.xml.bind.JAXBException;
import org.apache.sis.util.Version;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ISO_NAMESPACE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.metadata.content.PolarizationOrientation;


/**
 * Tests {@link DefaultBand}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultBandTest extends TestCase {
    /**
     * The XML fragment used for testing.
     */
    private static final String XML =
              "<mrc:MI_Band xmlns:mrc=\"" + ISO_NAMESPACE + "19115/-3/mrc/1.0\" xmlns:gco=\"" + ISO_NAMESPACE +  "19115/-3/gco/1.0\">\n"
            + "  <mrc:numberOfValues>\n"
            + "    <gco:Integer>1000</gco:Integer>\n"       // Only in 2014 schema.
            + "  </mrc:numberOfValues>\n"
            + "  <mrc:nominalSpatialResolution>\n"
            + "    <gco:Real>10.0</gco:Real>\n"
            + "  </mrc:nominalSpatialResolution>\n"
            + "  <mrc:detectedPolarisation>\n"
            + "    <mrc:MI_PolarisationOrientationCode "    // Spell with "s" in 2014 schema.
            +       "codeList=\"" + ISO_NAMESPACE + "19115/resources/Codelist/cat/codelists.xml#MI_PolarisationOrientationCode\" codeListValue=\"vertical\">"
            +       "Vertical"
            +     "</mrc:MI_PolarisationOrientationCode>\n"
            + "  </mrc:detectedPolarisation>\n"
            + "</mrc:MI_Band>\n";

    /**
     * XML fragment using legacy schema. This XML contains an {@link PolarisationOrientation} code list,
     * which was spell with a "z" in the 2003 version of ISO 19115 standard. This XML is used for testing
     * that the legacy spelling is handled when (un)marshalling a legacy document.
     */
    private static final String XML_LEGACY =
              "<gmi:MI_Band xmlns:gmi=\"" + ISO_NAMESPACE + "19115/-2/gmi/1.0\" xmlns:gco=\"http://www.isotc211.org/2005/gco\">\n"
            + "  <gmi:nominalSpatialResolution>\n"
            + "    <gco:Real>10.0</gco:Real>\n"
            + "  </gmi:nominalSpatialResolution>\n"
            + "  <gmi:detectedPolarisation>\n"
            + "    <gmi:MI_PolarizationOrientationCode "    // Spell with "z" in 2003 schema.
            +       "codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MI_PolarisationOrientationCode\" codeListValue=\"vertical\">" +
                    "Vertical"
            +     "</gmi:MI_PolarizationOrientationCode>\n"
            + "  </gmi:detectedPolarisation>\n"
            + "</gmi:MI_Band>\n";

    /**
     * Creates a new test case.
     */
    public DefaultBandTest() {
    }

    /**
     * Tests marshalling a small metadata containing a {@link PolarisationOrientation}.
     *
     * @throws JAXBException if an error occurred during XML marshalling.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        marshal(VERSION_2014, XML);
    }

    /**
     * Tests marshalling a small metadata in legacy XML schema.
     *
     * @throws JAXBException if an error occurred during XML marshalling.
     */
    @Test
    public void testMarshallingLegacy() throws JAXBException {
        marshal(VERSION_2007, XML_LEGACY);
    }

    /**
     * Implementation of marshalling tests.
     *
     * @param  version   version of the XML schema to test.
     * @param  expected  expected XML fragment.
     */
    private void marshal(final Version version, final String expected) throws JAXBException {
        final DefaultBand band = new DefaultBand();
        band.setNumberOfValues(1000);
        band.setNominalSpatialResolution(10d);
        band.setDetectedPolarization(PolarizationOrientation.VERTICAL);
        final String actual = marshal(band, version);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests unmarshalling a small metadata containing a {@link PolarisationOrientation}.
     *
     * @throws JAXBException if an error occurred during XML unmarshalling.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        unmarshal(XML, 1000);
    }

    /**
     * Tests unmarshalling a small metadata in legacy XML schema.
     *
     * @throws JAXBException if an error occurred during XML unmarshalling.
     */
    @Test
    public void testUnmarshallingLegacy() throws JAXBException {
        unmarshal(XML_LEGACY, null);
    }

    /**
     * Implementation of unmarshalling tests.
     *
     * @param  xml             XML fragment to test.
     * @param  numberOfValues  expected values of {@code numberOfValues} property.
     */
    private void unmarshal(final String xml, final Integer numberOfValues) throws JAXBException {
        final DefaultBand band = unmarshal(DefaultBand.class, xml);
        assertEquals(Double.valueOf(10), band.getNominalSpatialResolution());
        assertEquals(PolarizationOrientation.VERTICAL, band.getDetectedPolarization());
        assertEquals(numberOfValues, band.getNumberOfValues());
    }
}
