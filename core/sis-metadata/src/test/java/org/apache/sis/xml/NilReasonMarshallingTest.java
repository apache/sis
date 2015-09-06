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
package org.apache.sis.xml;

import javax.xml.bind.JAXBException;
import org.opengis.metadata.content.Band;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.quality.ConformanceResult;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the XML marshalling of object having {@code nilReason} attribute.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-149">GEOTK-149</a>
 */
public final strictfp class NilReasonMarshallingTest extends XMLTestCase {
    /**
     * Tests a simple case for a missing data.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testMissing() throws JAXBException {
        final String expected =
                "<gmd:CI_Citation xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gmd:title>\n" +
                "    <gco:CharacterString>A title</gco:CharacterString>\n" +
                "  </gmd:title>\n" +
                "  <gmd:series gco:nilReason=\"missing\"/>\n" +
                "</gmd:CI_Citation>";

        final Citation citation = (Citation) XML.unmarshal(expected);
        assertTitleEquals("citation", "A title", citation);

        final Series series = citation.getSeries();
        assertInstanceOf("Should have instantiated a proxy.", NilObject.class, series);

        final NilReason reason = ((NilObject) series).getNilReason();
        assertSame("nilReason", NilReason.MISSING, reason);
        assertNull("NilReason.explanation", reason.getOtherExplanation());
        assertNull("NilReason.URI",         reason.getURI());

        assertEquals("Series[missing]", series.toString());
        assertNull("All attributes are expected to be null.", series.getName());

        final String actual = XML.marshal(citation);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(citation, XML.unmarshal(actual));
    }

    /**
     * Tests a missing boolean value. The {@link Boolean}, {@link Integer}, {@link Double} and {@link String}
     * values are implemented as special cases in {@link NilReason}, because they are final classes on which
     * we have no control.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    @DependsOnMethod("testMissing")
    public void testMissingBoolean() throws JAXBException {
        final String expected =
                "<gmd:DQ_ConformanceResult xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                         " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gmd:explanation>\n" +
                "    <gco:CharacterString>An explanation</gco:CharacterString>\n" +
                "  </gmd:explanation>\n" +
                "  <gmd:pass gco:nilReason=\"missing\"/>\n" +
                "</gmd:DQ_ConformanceResult>";

        final ConformanceResult result = (ConformanceResult) XML.unmarshal(expected);
        assertEquals("explanation", "An explanation", result.getExplanation().toString());

        final Boolean pass = result.pass();
        assertNotNull("Expected a sentinel value.", pass);
        assertEquals ("Nil value shall be false.",  Boolean.FALSE, pass);
        assertNotSame("Expected a sentinel value.", Boolean.FALSE, pass);
        assertSame("nilReason", NilReason.MISSING, NilReason.forObject(pass));

        final String actual = XML.marshal(result);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(result, XML.unmarshal(actual));
    }

    /**
     * Tests a missing integer value. The {@link Boolean}, {@link Integer}, {@link Double} and {@link String}
     * values are implemented as special cases in {@link NilReason}, because they are final classes on which
     * we have no control.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    @DependsOnMethod("testMissing")
    @SuppressWarnings("UnnecessaryBoxing")
    public void testMissingInteger() throws JAXBException {
        final String expected =
                "<gmd:MD_Dimension xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                 " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n" +
                "</gmd:MD_Dimension>";

        final Dimension result = (Dimension) XML.unmarshal(expected);

        final Integer size = result.getDimensionSize();
        assertNotNull("Expected a sentinel value.", size);
        assertEquals ("Nil value shall be 0.",      Integer.valueOf(0), size);
        assertNotSame("Expected a sentinel value.", Integer.valueOf(0), size);
        assertSame("nilReason", NilReason.UNKNOWN, NilReason.forObject(size));

        final String actual = XML.marshal(result);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(result, XML.unmarshal(actual));
    }

    /**
     * Tests a missing double value.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    @DependsOnMethod("testMissing")
    public void testMissingDouble() throws JAXBException {
        final String expected =
                "<gmd:MD_Band xmlns:gmd=\"" + Namespaces.GMD + '"' +
                            " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gmd:minValue gco:nilReason=\"unknown\"/>\n" +
                "  <gmd:peakResponse gco:nilReason=\"missing\"/>\n" +
                "</gmd:MD_Band>";

        final Band result = (Band) XML.unmarshal(expected);

        final Double minValue = result.getMinValue();
        assertNotNull("Expected a sentinel value.", minValue);
        assertTrue("Nil value shall be NaN.", minValue.isNaN());
        assertSame("nilReason", NilReason.UNKNOWN, NilReason.forObject(minValue));

        final Double peakResponse = result.getMinValue();
        assertNotNull("Expected a sentinel value.", peakResponse);
        assertTrue("Nil value shall be NaN.", peakResponse.isNaN());
        assertSame("nilReason", NilReason.UNKNOWN, NilReason.forObject(peakResponse));

        final String actual = XML.marshal(result);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(result, XML.unmarshal(actual));
    }

    /**
     * Tests a case where the nil reason is specified by an other reason.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    @DependsOnMethod("testMissing")
    public void testOther() throws JAXBException {
        final String expected =
                "<gmd:CI_Citation xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gmd:title>\n" +
                "    <gco:CharacterString>A title</gco:CharacterString>\n" +
                "  </gmd:title>\n" +
                "  <gmd:series gco:nilReason=\"other:myReason\"/>\n" +
                "</gmd:CI_Citation>";

        final Citation citation = (Citation) XML.unmarshal(expected);
        assertTitleEquals("citation", "A title", citation);

        final Series series = citation.getSeries();
        assertInstanceOf("Should have instantiated a proxy.", NilObject.class, series);

        final NilReason reason = ((NilObject) series).getNilReason();
        assertEquals("NilReason.explanation", "myReason", reason.getOtherExplanation());
        assertNull("NilReason.URI", reason.getURI());

        assertEquals("Series[other:myReason]", series.toString());
        assertNull("All attributes are expected to be null.", series.getName());

        final String actual = XML.marshal(citation);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(citation, XML.unmarshal(actual));
    }

    /**
     * Tests a case where the nil reason is specified by a URI.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    @DependsOnMethod("testMissing")
    public void testURI() throws JAXBException {
        final String expected =
                "<gmd:CI_Citation xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gmd:title>\n" +
                "    <gco:CharacterString>A title</gco:CharacterString>\n" +
                "  </gmd:title>\n" +
                "  <gmd:series gco:nilReason=\"http://www.myreason.org\"/>\n" +
                "</gmd:CI_Citation>";

        final Citation citation = (Citation) XML.unmarshal(expected);
        assertTitleEquals("citation", "A title", citation);

        final Series series = citation.getSeries();
        assertInstanceOf("Should have instantiated a proxy.", NilObject.class, series);

        final NilReason reason = ((NilObject) series).getNilReason();
        assertNull("NilReason.explanation", reason.getOtherExplanation());
        assertEquals("NilReason.URI", "http://www.myreason.org", String.valueOf(reason.getURI()));

        assertEquals("Series[http://www.myreason.org]", series.toString());
        assertNull("All attributes are expected to be null.", series.getName());

        final String actual = XML.marshal(citation);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(citation, XML.unmarshal(actual));
    }
}
