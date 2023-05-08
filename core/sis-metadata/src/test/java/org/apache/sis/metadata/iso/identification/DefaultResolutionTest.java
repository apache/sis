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

import javax.xml.bind.JAXBException;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.xml.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;
import static org.apache.sis.internal.jaxb.gml.MeasureTest.UOM_URL;


/**
 * Tests {@link DefaultResolution}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.1
 * @since   0.3
 */
@DependsOn(DefaultRepresentativeFractionTest.class)
public final class DefaultResolutionTest extends TestCase {
    /**
     * A JUnit {@link Rule} for listening to log events. This field is public because JUnit requires us to
     * do so, but should be considered as an implementation details (it should have been a private field).
     */
    @Rule
    public final LoggingWatcher loggings = new LoggingWatcher(Context.LOGGER);

    /**
     * Verifies that no unexpected warning has been emitted in any test defined in this class.
     */
    @After
    public void assertNoUnexpectedLog() {
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the {@link DefaultResolution#DefaultResolution(RepresentativeFraction)} constructor.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-195">SIS-195</a>
     *
     * @since 0.6
     */
    @Test
    public void testConstructor() {
        final DefaultRepresentativeFraction scale = new DefaultRepresentativeFraction();
        scale.setDenominator(100);
        final DefaultResolution metadata = new DefaultResolution(scale);
        assertSame(scale, metadata.getEquivalentScale());
    }

    /**
     * Tests the various setter methods. Since they are exclusive properties,
     * we expect any new property to replace the old one.
     */
    @Test
    public void testSetExclusiveProperties() {
        final DefaultResolution metadata = new DefaultResolution();
        final DefaultRepresentativeFraction scale = new DefaultRepresentativeFraction();
        scale.setDenominator(100);

        metadata.setDistance(2.0);
        assertEquals("distance", Double.valueOf(2.0), metadata.getDistance());
        assertNull("equivalentScale", metadata.getEquivalentScale());
        loggings.assertNoUnexpectedLog();

        metadata.setEquivalentScale(scale);
        assertSame("equivalentScale", scale, metadata.getEquivalentScale());
        assertNull("distance", metadata.getDistance());
        loggings.assertNextLogContains("distance", "equivalentScale");
        loggings.assertNoUnexpectedLog();

        metadata.setDistance(null); // Expected to be a no-op.
        assertSame("equivalentScale", scale, metadata.getEquivalentScale());
        assertNull("distance", metadata.getDistance());

        metadata.setEquivalentScale(null);
        assertNull("equivalentScale", metadata.getEquivalentScale());
        assertNull("distance", metadata.getDistance());
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
        final DefaultResolution resolution = new DefaultResolution();
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
        final DefaultResolution resolution = new DefaultResolution();
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
    }
}
