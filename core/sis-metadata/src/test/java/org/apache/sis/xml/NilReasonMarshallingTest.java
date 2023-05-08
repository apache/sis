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
import org.apache.sis.test.xml.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.metadata.Assertions.assertTitleEquals;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Tests the XML marshalling of object having {@code nilReason} attribute.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 */
public final class NilReasonMarshallingTest extends TestCase {
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
        assertTitleEquals("citation", "A title", citation);

        final Series series = citation.getSeries();
        assertInstanceOf("Should have instantiated a proxy.", NilObject.class, series);

        final NilReason reason = ((NilObject) series).getNilReason();
        assertSame("nilReason", NilReason.MISSING, reason);
        assertNull("NilReason.explanation", reason.getOtherExplanation());
        assertNull("NilReason.URI",         reason.getURI());

        assertEquals("Series[missing]", series.toString());
        assertNull("All attributes are expected to be null.", series.getName());

        final String actual = marshal(citation);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(citation, unmarshal(Citation.class, actual));
    }

    /**
     * Tests a missing boolean value. The {@link Boolean}, {@link Integer}, {@link Double} and {@link String}
     * values are implemented as special cases in {@link NilReason}, because they are final classes on which
     * we have no control.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    @DependsOnMethod("testMissing")
    public void testMissingBoolean() throws JAXBException {
        final String expected =
                "<mdq:DQ_ConformanceResult xmlns:mdq=\"" + Namespaces.MDQ + '"' +
                                         " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <mdq:explanation>\n" +
                "    <gco:CharacterString>An explanation</gco:CharacterString>\n" +
                "  </mdq:explanation>\n" +
                "  <mdq:pass gco:nilReason=\"missing\"/>\n" +
                "</mdq:DQ_ConformanceResult>";

        final ConformanceResult result = unmarshal(ConformanceResult.class, expected);
        assertEquals("explanation", "An explanation", result.getExplanation().toString());

        final Boolean pass = result.pass();
        assertNotNull("Expected a sentinel value.", pass);
        assertEquals ("Nil value shall be false.",  Boolean.FALSE, pass);
        assertNotSame("Expected a sentinel value.", Boolean.FALSE, pass);
        assertSame("nilReason", NilReason.MISSING, NilReason.forObject(pass));

        final String actual = marshal(result);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(result, unmarshal(ConformanceResult.class, actual));
    }

    /**
     * Tests a missing integer value. The {@link Boolean}, {@link Integer}, {@link Double} and {@link String}
     * values are implemented as special cases in {@link NilReason}, because they are final classes on which
     * we have no control.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    @DependsOnMethod("testMissing")
    @SuppressWarnings("UnnecessaryBoxing")
    public void testMissingInteger() throws JAXBException {
        final String expected =
                "<msr:MD_Dimension xmlns:msr=\"" + Namespaces.MSR + '"' +
                                 " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <msr:dimensionSize gco:nilReason=\"unknown\"/>\n" +
                "</msr:MD_Dimension>";

        final Dimension result = unmarshal(Dimension.class, expected);

        final Integer size = result.getDimensionSize();
        assertNotNull("Expected a sentinel value.", size);
        assertEquals ("Nil value shall be 0.",      Integer.valueOf(0), size);
        assertNotSame("Expected a sentinel value.", Integer.valueOf(0), size);
        assertSame("nilReason", NilReason.UNKNOWN, NilReason.forObject(size));

        final String actual = marshal(result);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(result, unmarshal(Dimension.class, actual));
    }

    /**
     * Tests a missing double value.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    @DependsOnMethod("testMissing")
    public void testMissingDouble() throws JAXBException {
        final String expected =
                "<mrc:MD_Band xmlns:mrc=\"" + Namespaces.MRC + '"' +
                            " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <mrc:minValue gco:nilReason=\"unknown\"/>\n" +
                "  <mrc:peakResponse gco:nilReason=\"missing\"/>\n" +
                "</mrc:MD_Band>";

        final Band result = unmarshal(Band.class, expected);

        final Double minValue = result.getMinValue();
        assertNotNull("Expected a sentinel value.", minValue);
        assertTrue("Nil value shall be NaN.", minValue.isNaN());
        assertSame("nilReason", NilReason.UNKNOWN, NilReason.forObject(minValue));

        final Double peakResponse = result.getMinValue();
        assertNotNull("Expected a sentinel value.", peakResponse);
        assertTrue("Nil value shall be NaN.", peakResponse.isNaN());
        assertSame("nilReason", NilReason.UNKNOWN, NilReason.forObject(peakResponse));

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
    @DependsOnMethod("testMissing")
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
        assertTitleEquals("citation", "A title", citation);

        final Series series = citation.getSeries();
        assertInstanceOf("Should have instantiated a proxy.", NilObject.class, series);

        final NilReason reason = ((NilObject) series).getNilReason();
        assertEquals("NilReason.explanation", "myReason", reason.getOtherExplanation());
        assertNull("NilReason.URI", reason.getURI());

        assertEquals("Series[other:myReason]", series.toString());
        assertNull("All attributes are expected to be null.", series.getName());

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
    @DependsOnMethod("testMissing")
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
        assertTitleEquals("citation", "A title", citation);

        final Series series = citation.getSeries();
        assertInstanceOf("Should have instantiated a proxy.", NilObject.class, series);

        final NilReason reason = ((NilObject) series).getNilReason();
        assertNull("NilReason.explanation", reason.getOtherExplanation());
        assertEquals("NilReason.URI", "http://www.myreason.org", String.valueOf(reason.getURI()));

        assertEquals("Series[http://www.myreason.org]", series.toString());
        assertNull("All attributes are expected to be null.", series.getName());

        final String actual = marshal(citation);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(citation, unmarshal(Citation.class, actual));
    }
}
