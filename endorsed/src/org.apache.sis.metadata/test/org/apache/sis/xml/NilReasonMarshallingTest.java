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

import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.content.Band;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.citation.Citation;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.test.Assertions.assertTitleEquals;


/**
 * Tests the XML marshalling of object having {@code nilReason} attribute.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class NilReasonMarshallingTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public NilReasonMarshallingTest() {
    }

    /**
     * Tests a simple case for a missing data.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testMissing() throws JAXBException {
        final String expected =
                "<cit:CI_Citation xmlns:cit=\"" + Namespaces.CIT + '"' +
                                " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <cit:title>\n" +
                "    <gco:CharacterString>A title</gco:CharacterString>\n" +
                "  </cit:title>\n" +
                "  <cit:series gco:nilReason=\"missing\"/>\n" +
                "</cit:CI_Citation>";

        final Citation citation = unmarshal(Citation.class, expected);
        assertTitleEquals("A title", citation, "citation");

        final Series series = citation.getSeries();
        final NilObject nil = assertInstanceOf(NilObject.class, series, "Should have instantiated a proxy.");
        final NilReason reason = nil.getNilReason();
        assertSame(NilReason.MISSING, reason, "nilReason");
        assertNull(reason.getOtherExplanation(), "NilReason.explanation");
        assertNull(reason.getURI(), "NilReason.URI");

        assertEquals("Series[missing]", series.toString());
        assertNull(series.getName(), "All attributes are expected to be null.");

        final String actual = marshal(citation);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(citation, unmarshal(Citation.class, actual));
    }

    /**
     * Tests a missing double value.
     * The {@link Float}, {@link Double} and {@link String} values are implemented as special
     * cases in {@link NilReason}, because they are final classes on which we have no control.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testMissingDouble() throws JAXBException {
        final String expected =
                "<mrc:MD_Band xmlns:mrc=\"" + Namespaces.MRC + '"' +
                            " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <mrc:minValue gco:nilReason=\"unknown\"/>\n" +
                "  <mrc:peakResponse gco:nilReason=\"missing\"/>\n" +
                "</mrc:MD_Band>";

        final Band result = unmarshal(Band.class, expected);

        final Double minValue = result.getMinValue();
        assertNotNull(minValue, "Expected a sentinel value.");
        assertTrue(minValue.isNaN(), "Nil value shall be NaN.");
        assertSame(NilReason.UNKNOWN, NilReason.forObject(minValue), "nilReason");

        final Double peakResponse = result.getMinValue();
        assertNotNull(peakResponse, "Expected a sentinel value.");
        assertTrue(peakResponse.isNaN(), "Nil value shall be NaN.");
        assertSame(NilReason.UNKNOWN, NilReason.forObject(peakResponse), "nilReason");

        final String actual = marshal(result);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(result, unmarshal(Band.class, actual));
    }

    /**
     * Tests a case where the nil reason is specified by another reason.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testOther() throws JAXBException {
        final String expected =
                "<cit:CI_Citation xmlns:cit=\"" + Namespaces.CIT + '"' +
                                " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <cit:title>\n" +
                "    <gco:CharacterString>A title</gco:CharacterString>\n" +
                "  </cit:title>\n" +
                "  <cit:series gco:nilReason=\"other:myReason\"/>\n" +
                "</cit:CI_Citation>";

        final Citation citation = unmarshal(Citation.class, expected);
        assertTitleEquals("A title", citation, "citation");

        final Series series = citation.getSeries();
        assertInstanceOf(NilObject.class, series, "Should have instantiated a proxy.");

        final NilReason reason = ((NilObject) series).getNilReason();
        assertEquals("myReason", reason.getOtherExplanation(), "NilReason.explanation");
        assertNull(reason.getURI(), "NilReason.URI");

        assertEquals("Series[other:myReason]", series.toString());
        assertNull(series.getName(), "All attributes are expected to be null.");

        final String actual = marshal(citation);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(citation, unmarshal(Citation.class, actual));
    }

    /**
     * Tests a case where the nil reason is specified by a URI.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testURI() throws JAXBException {
        final String expected =
                "<cit:CI_Citation xmlns:cit=\"" + Namespaces.CIT + '"' +
                                " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <cit:title>\n" +
                "    <gco:CharacterString>A title</gco:CharacterString>\n" +
                "  </cit:title>\n" +
                "  <cit:series gco:nilReason=\"http://www.myreason.org\"/>\n" +
                "</cit:CI_Citation>";

        final Citation citation = unmarshal(Citation.class, expected);
        assertTitleEquals("A title", citation, "citation");

        final Series series = citation.getSeries();
        assertInstanceOf(NilObject.class, series, "Should have instantiated a proxy.");

        final NilReason reason = ((NilObject) series).getNilReason();
        assertNull(reason.getOtherExplanation(), "NilReason.explanation");
        assertEquals("http://www.myreason.org", String.valueOf(reason.getURI()), "NilReason.URI");

        assertEquals("Series[http://www.myreason.org]", series.toString());
        assertNull(series.getName(), "All attributes are expected to be null.");

        final String actual = marshal(citation);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(citation, unmarshal(Citation.class, actual));
    }
}
