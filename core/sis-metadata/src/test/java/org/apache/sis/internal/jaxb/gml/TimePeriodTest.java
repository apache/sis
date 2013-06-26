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
package org.apache.sis.internal.jaxb.gml;

import java.util.Locale;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.sis.internal.jaxb.XmlUtilities;
import org.apache.sis.test.XMLTransformation;
import org.apache.sis.test.XMLTestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.test.TestUtilities.format;


/**
 * Tests the {@link TimePeriod} class. The XML fragments used in this test cases are derived from
 * <a href="http://toyoda-eizi.blogspot.fr/2011/02/examples-of-gml-fragment-in-iso.html">here</a>.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.19)
 * @version 0.3
 * @module
 */
public final strictfp class TimePeriodTest extends XMLTestCase {
    /**
     * The XML marshaller.
     */
    private static Marshaller marshaller;

    /**
     * The XML unmarshaller.
     */
    private static Unmarshaller unmarshaller;

    /**
     * A buffer where to marshal.
     */
    private final StringWriter buffer = new StringWriter();

    /**
     * Creates the XML marshaller to be shared by all test methods.
     *
     * @throws JAXBException If an error occurred while creating the marshaller.
     */
    @BeforeClass
    public static void createMarshallers() throws JAXBException {
        final JAXBContext context = JAXBContext.newInstance(TimeInstant.class, TimePeriod.class);
        marshaller   = context.createMarshaller();
        unmarshaller = context.createUnmarshaller();
    }

    /**
     * Allows the garbage collector to collect the marshaller and unmarshallers.
     */
    @AfterClass
    public static void disposeMarshallers() {
        marshaller   = null;
        unmarshaller = null;
    }

    /**
     * Creates a new time instant for the given date.
     */
    private static TimeInstant createTimeInstant(final String date) throws DatatypeConfigurationException {
        final TimeInstant instant = new TimeInstant();
        instant.timePosition = XmlUtilities.toXML(null, date(date));
        return instant;
    }

    /**
     * Tests time instant. The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException If an error occurred while marshalling.
     * @throws DatatypeConfigurationException Should never happen.
     */
    @Test
    public void testTimeInstant() throws JAXBException, DatatypeConfigurationException {
        createContext(false, Locale.FRANCE, "CET");
        String expected =
            "<gml:TimeInstant>\n" +
            "  <gml:timePosition>1992-01-01T01:00:00.000+01:00</gml:timePosition>\n" +
            "</gml:TimeInstant>\n";
        final TimeInstant instant = createTimeInstant("1992-01-01 00:00:00");
        marshaller.marshal(instant, buffer);
        final String actual = buffer.toString();
        expected = XMLTransformation.GML.optionallyRemovePrefix(expected, actual);
        assertXmlEquals(expected, actual, "xmlns:*", "xsi:schemaLocation");
        final TimeInstant test = (TimeInstant) unmarshaller.unmarshal(new StringReader(actual));
        assertEquals("1992-01-01 00:00:00", format(XmlUtilities.toDate(test.timePosition)));
    }

    /**
     * Tests a time period using the GML 2 syntax.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException If an error occurred while marshalling.
     */
    @Test
    public void testPeriodGML2() throws JAXBException {
        createContext(false, Locale.FRANCE, "CET");
        testPeriod(new TimePeriodBound.GML2(new DummyInstant(date("1992-01-01 00:00:00"))),
                   new TimePeriodBound.GML2(new DummyInstant(date("2007-12-31 00:00:00"))),
            "<gml:TimePeriod>\n" +
            "  <gml:begin>\n" +
            "    <gml:TimeInstant>\n" +
            "      <gml:timePosition>1992-01-01T01:00:00+01:00</gml:timePosition>\n" +
            "    </gml:TimeInstant>\n" +
            "  </gml:begin>\n" +
            "  <gml:end>\n" +
            "    <gml:TimeInstant>\n" +
            "      <gml:timePosition>2007-12-31T01:00:00+01:00</gml:timePosition>\n" +
            "    </gml:TimeInstant>\n" +
            "  </gml:end>\n" +
            "</gml:TimePeriod>\n", true);
    }

    /**
     * Tests a time period using GML2 or GML3 syntax. This method is used for the
     * implementation of {@link #testPeriodGML2()} and {@link #testPeriodGML3()}.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @param expected The expected string.
     */
    private void testPeriod(final TimePeriodBound begin, final TimePeriodBound end,
            String expected, final boolean verifyValues) throws JAXBException
    {
        createContext(false, Locale.FRANCE, "CET");
        final TimePeriod period = new TimePeriod();
        period.begin = begin;
        period.end   = end;
        marshaller.marshal(period, buffer);
        final String actual = buffer.toString();
        expected = XMLTransformation.GML.optionallyRemovePrefix(expected, actual);
        assertXmlEquals(expected, actual, "xmlns:*", "xsi:schemaLocation");
        final TimePeriod test = (TimePeriod) unmarshaller.unmarshal(new StringReader(actual));
        if (verifyValues) {
            assertEquals("1992-01-01 00:00:00", format(XmlUtilities.toDate(test.begin.calendar())));
            assertEquals("2007-12-31 00:00:00", format(XmlUtilities.toDate(test.end  .calendar())));
        }
    }

    /**
     * Tests a time period using the GML 3 syntax.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException If an error occurred while marshalling.
     */
    @Test
    public void testPeriodGML3() throws JAXBException {
        createContext(false, Locale.FRANCE, "CET");
        testPeriod(new TimePeriodBound.GML3(new DummyInstant(date("1992-01-01 00:00:00")), "before"),
                   new TimePeriodBound.GML3(new DummyInstant(date("2007-12-31 00:00:00")), "after"),
            "<gml:TimePeriod>\n" +
            "  <gml:beginPosition>1992-01-01T01:00:00+01:00</gml:beginPosition>\n" +
            "  <gml:endPosition>2007-12-31T01:00:00+01:00</gml:endPosition>\n" +
            "</gml:TimePeriod>\n", true);
    }

    /**
     * Same test than {@link #testPeriodGML3()}, but with simplified date format (omit the hours and timezone)
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException If an error occurred while marshalling.
     */
    @Test
    public void testSimplifiedPeriodGML3() throws JAXBException {
        createContext(false, Locale.FRANCE, "CET");
        testPeriod(new TimePeriodBound.GML3(new DummyInstant(date("1992-01-01 23:00:00")), "before"),
                   new TimePeriodBound.GML3(new DummyInstant(date("2007-12-30 23:00:00")), "after"),
            "<gml:TimePeriod>\n" +
            "  <gml:beginPosition>1992-01-02</gml:beginPosition>\n" +
            "  <gml:endPosition>2007-12-31</gml:endPosition>\n" +
            "</gml:TimePeriod>\n", false);
    }

    /**
     * Same test than {@link #testSimplifiedPeriodGML3()}, but without beginning boundary.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException If an error occurred while marshalling.
     */
    @Test
    public void testBeforePeriodGML3() throws JAXBException {
        createContext(false, Locale.FRANCE, "CET");
        testPeriod(new TimePeriodBound.GML3(null, "before"),
                   new TimePeriodBound.GML3(new DummyInstant(date("2007-12-30 23:00:00")), "after"),
            "<gml:TimePeriod>\n" +
            "  <gml:beginPosition indeterminatePosition=\"before\"/>\n" +
            "  <gml:endPosition>2007-12-31</gml:endPosition>\n" +
            "</gml:TimePeriod>\n", false);
    }

    /**
     * Same test than {@link #testSimplifiedPeriodGML3()}, but without end boundary.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException If an error occurred while marshalling.
     */
    @Test
    public void testAfterPeriodGML3() throws JAXBException {
        createContext(false, Locale.FRANCE, "CET");
        testPeriod(new TimePeriodBound.GML3(new DummyInstant(date("1992-01-01 23:00:00")), "before"),
                   new TimePeriodBound.GML3(null, "after"),
            "<gml:TimePeriod>\n" +
            "  <gml:beginPosition>1992-01-02</gml:beginPosition>\n" +
            "  <gml:endPosition indeterminatePosition=\"after\"/>\n" +
            "</gml:TimePeriod>\n", false);
    }
}
