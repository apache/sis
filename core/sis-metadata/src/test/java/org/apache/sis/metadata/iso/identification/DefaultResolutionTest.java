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
import org.apache.sis.xml.XML;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.Schemas;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultResolution}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.7
 * @since   0.3
 * @module
 */
@DependsOn(DefaultRepresentativeFractionTest.class)
public final strictfp class DefaultResolutionTest extends TestCase {
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
     * {@preformat xml
     *   <gmd:MD_Resolution>
     *     <gmd:distance>
     *       <gco:Distance uom=\"http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])\">1000.0</gco:Distance>
     *     </gmd:distance>
     *   </gmd:MD_Resolution>
     * }
     *
     * @throws JAXBException if an error occurred while marshalling the element.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultResolution resolution = new DefaultResolution();
        resolution.setDistance(1000.0);
        final String xml = XML.marshal(resolution);
        assertTrue("<gmd:distance> element is missing. If this test fails randomly, "
                + "see DefaultResolutionTest.testXML() javadoc for more information", xml.contains("distance"));
        /*
         * Following test is done as a matter of principle, but should not be a problem.
         * The real issue is the <gmd:distance> which happen to be randomly missing for
         * an unknown reason.
         */
        assertXmlEquals(
                "<gmd:MD_Resolution xmlns:gmd=\"" + LegacyNamespaces.GMD + '"' +
                                  " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
                "  <gmd:distance>\n" +
                "    <gco:Distance uom=\"" + Schemas.METADATA_ROOT_LEGACY + Schemas.UOM_PATH + "#xpointer(//*[@gml:id='m'])\">1000.0</gco:Distance>\n" +
                "  </gmd:distance>\n" +
                "</gmd:MD_Resolution>", xml, "xmlns:*");
        /*
         * Unmarshal the element back to a Java object, as a safety.
         * Should not be a problem neither.
         */
        assertEquals(resolution, XML.unmarshal(xml));
    }
}
