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

import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.Context;
import static org.apache.sis.xml.bind.gml.MeasureTest.UOM_URL;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;


/**
 * Tests {@link DefaultResolution}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
public final class DefaultResolutionTest extends TestCase.WithLogs {
    /**
     * Creates a new test case.
     */
    public DefaultResolutionTest() {
        super(Context.LOGGER);
    }

    /**
     * Tests the {@link DefaultResolution#DefaultResolution(RepresentativeFraction)} constructor.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-195">SIS-195</a>
     */
    @Test
    public void testConstructor() {
        final var scale = new DefaultRepresentativeFraction();
        scale.setDenominator(100);
        final var metadata = new DefaultResolution(scale);
        assertSame(scale, metadata.getEquivalentScale());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the various setter methods. Since they are exclusive properties,
     * we expect any new property to replace the old one.
     */
    @Test
    public void testSetExclusiveProperties() {
        final var metadata = new DefaultResolution();
        final var scale = new DefaultRepresentativeFraction();
        scale.setDenominator(100);

        metadata.setDistance(2.0);
        assertEquals(Double.valueOf(2.0), metadata.getDistance());
        assertNull(metadata.getEquivalentScale());
        loggings.assertNoUnexpectedLog();

        metadata.setEquivalentScale(scale);
        assertSame(scale, metadata.getEquivalentScale());
        assertNull(metadata.getDistance());
        loggings.assertNextLogContains("distance", "equivalentScale");
        loggings.assertNoUnexpectedLog();

        metadata.setDistance(null); // Expected to be a no-op.
        assertSame(scale, metadata.getEquivalentScale());
        assertNull(metadata.getDistance());

        metadata.setEquivalentScale(null);
        assertNull(metadata.getEquivalentScale());
        assertNull(metadata.getDistance());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests XML (un)marshalling of a resolution element. This test creates a {@link DefaultResolution}
     * instance which is expected to be marshalled as below (ignoring namespace declarations):
     *
     * {@snippet lang="xml" :
     *   <mri:MD_Resolution>
     *     <mri:distance>
     *       <gco:Distance uom="http://www.isotc211.org/2005/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])">1000.0</gco:Distance>
     *     </mri:distance>
     *   </mri:MD_Resolution>
     *   }
     *
     * @throws JAXBException if an error occurred while marshalling the element.
     */
    @Test
    public void testXML() throws JAXBException {
        final var resolution = new DefaultResolution();
        resolution.setDistance(1000.0);
        final String xml = marshal(resolution);
        assertXmlEquals(
                "<mri:MD_Resolution xmlns:mri=\"" + Namespaces.MRI + '"' +
                                  " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <mri:distance>\n" +
                "    <gco:Distance uom=\"" + UOM_URL + "#xpointer(//*[@gml:id='m'])\">1000.0</gco:Distance>\n" +
                "  </mri:distance>\n" +
                "</mri:MD_Resolution>", xml, "xmlns:*");

        assertEquals(resolution, unmarshal(DefaultResolution.class, xml));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests XML (un)marshalling of a resolution element using legacy XML schema.
     * This test creates a {@link DefaultResolution} instance which is expected to be marshalled as below
     * (ignoring namespace declarations):
     *
     * {@snippet lang="xml" :
     *   <gmd:MD_Resolution>
     *     <gmd:distance>
     *       <gco:Distance uom="http://www.isotc211.org/2005/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])">1000.0</gco:Distance>
     *     </gmd:distance>
     *   </gmd:MD_Resolution>
     *   }
     *
     * @throws JAXBException if an error occurred while marshalling the element.
     */
    @Test
    public void testLegacyXML() throws JAXBException {
        final var resolution = new DefaultResolution();
        resolution.setDistance(1000.0);
        final String xml = marshal(resolution, VERSION_2007);
        assertXmlEquals(
                "<gmd:MD_Resolution xmlns:gmd=\"" + LegacyNamespaces.GMD + '"' +
                                  " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
                "  <gmd:distance>\n" +
                "    <gco:Distance uom=\"" + UOM_URL + "#xpointer(//*[@gml:id='m'])\">1000.0</gco:Distance>\n" +
                "  </gmd:distance>\n" +
                "</gmd:MD_Resolution>", xml, "xmlns:*");

        assertEquals(resolution, unmarshal(DefaultResolution.class, xml));
        loggings.assertNoUnexpectedLog();
    }
}
