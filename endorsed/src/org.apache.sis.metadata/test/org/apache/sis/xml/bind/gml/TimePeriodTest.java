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
package org.apache.sis.xml.bind.gml;

import java.util.HashMap;
import java.util.Locale;
import java.util.Date;
import java.time.Instant;
import java.time.LocalDate;
import javax.xml.datatype.DatatypeConfigurationException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.internal.shared.XmlUtilities;
import org.apache.sis.temporal.TemporalObjects;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;


/**
 * Tests the {@link TimePeriod} class. The XML fragments used in this test cases are derived from
 * <a href="http://toyoda-eizi.blogspot.fr/2011/02/examples-of-gml-fragment-in-iso.html">here</a>.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class TimePeriodTest extends TestCase {
    /**
     * A poll of configured {@link Marshaller} and {@link Unmarshaller}.
     */
    private final MarshallerPool pool;

    /**
     * Creates the XML (un)marshaller pool to be shared by all test methods.
     * The (un)marshallers locale and timezone will be set to fixed values.
     *
     * @throws JAXBException if an error occurred while creating the pool.
     */
    public TimePeriodTest() throws JAXBException {
        final var properties = new HashMap<String,Object>(4);
        assertNull(properties.put(XML.LOCALE, Locale.FRANCE));
        assertNull(properties.put(XML.TIMEZONE, "CET"));
        pool = new MarshallerPool(JAXBContext.newInstance(TimeInstant.class, TimePeriod.class), properties);
    }

    /**
     * Set the marshalling context to a fixed locale and timezone before to create the
     * JAXB wrappers for temporal objects.
     */
    private void createContext() throws JAXBException {
        createContext(true, Locale.FRANCE, "CET");
    }

    /**
     * Tests time instant. The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException if an error occurred while marshalling.
     * @throws DatatypeConfigurationException should never happen.
     */
    @Test
    public void testTimeInstant() throws JAXBException, DatatypeConfigurationException {
        createContext();
        final Marshaller   marshaller   = pool.acquireMarshaller();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final TimeInstant  instant      = new TimeInstant();
        instant.timePosition = XmlUtilities.toXML(context, Date.from(Instant.parse("1992-01-01T00:00:00Z")));
        final String actual = marshal(marshaller, instant);
        assertXmlEquals(
                "<gml:TimeInstant xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:timePosition>1992-01-01T01:00:00.000+01:00</gml:timePosition>\n" +
                "</gml:TimeInstant>\n", actual, "xmlns:*");

        final var test = (TimeInstant) unmarshal(unmarshaller, actual);
        assertEquals("1992-01-01T00:00:00Z", XmlUtilities.toDate(context, test.timePosition).toInstant().toString());

        pool.recycle(marshaller);
        pool.recycle(unmarshaller);
    }

    /**
     * Tests a time period using the GML 2 syntax.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException if an error occurred while marshalling.
     */
    @Test
    public void testPeriodGML2() throws JAXBException {
        createContext();
        final var begin = new TimePeriodBound.GML2(TemporalObjects.createInstant(Instant.parse("1992-01-01T00:00:00Z")));
        final var end   = new TimePeriodBound.GML2(TemporalObjects.createInstant(Instant.parse("2007-12-31T00:00:00Z")));
        testPeriod(begin, end,
                "<gml:TimePeriod xmlns:gml=\"" + Namespaces.GML + "\">\n" +
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
     * @param  expected  the expected string.
     */
    private void testPeriod(final TimePeriodBound begin, final TimePeriodBound end,
            final String expected, final boolean verifyValues) throws JAXBException
    {
        final Marshaller   marshaller   = pool.acquireMarshaller();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final TimePeriod   period       = new TimePeriod();
        period.begin = begin;
        period.end   = end;
        final String actual = marshal(marshaller, period);
        assertXmlEquals(expected, actual, "xmlns:*");
        final var test = (TimePeriod) unmarshal(unmarshaller, actual);
        if (verifyValues) {
            assertEquals("1992-01-01T00:00:00Z", XmlUtilities.toDate(context, test.begin.calendar()).toInstant().toString());
            assertEquals("2007-12-31T00:00:00Z", XmlUtilities.toDate(context, test.end  .calendar()).toInstant().toString());
        }
        pool.recycle(marshaller);
        pool.recycle(unmarshaller);
    }

    /**
     * Tests a time period using the GML 3 syntax.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException if an error occurred while marshalling.
     */
    @Test
    public void testPeriodGML3() throws JAXBException {
        createContext();
        final var begin = new TimePeriodBound.GML3(TemporalObjects.createInstant(Instant.parse("1992-01-01T00:00:00Z")), "before");
        final var end   = new TimePeriodBound.GML3(TemporalObjects.createInstant(Instant.parse("2007-12-31T00:00:00Z")), "after");
        testPeriod(begin, end,
                "<gml:TimePeriod xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:beginPosition>1992-01-01T01:00:00+01:00</gml:beginPosition>\n" +
                "  <gml:endPosition>2007-12-31T01:00:00+01:00</gml:endPosition>\n" +
                "</gml:TimePeriod>\n", true);
    }

    /**
     * Same test as {@link #testPeriodGML3()}, but with simplified date format (omit the hours and timezone)
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException if an error occurred while marshalling.
     */
    @Test
    public void testSimplifiedPeriodGML3() throws JAXBException {
        createContext();
        final var begin = new TimePeriodBound.GML3(TemporalObjects.createInstant(LocalDate.of(1992,  1,  2)), "before");
        final var end   = new TimePeriodBound.GML3(TemporalObjects.createInstant(LocalDate.of(2007, 12, 31)), "after");
        testPeriod(begin, end,
                "<gml:TimePeriod xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:beginPosition>1992-01-02</gml:beginPosition>\n" +
                "  <gml:endPosition>2007-12-31</gml:endPosition>\n" +
                "</gml:TimePeriod>\n", false);
    }

    /**
     * Same test as {@link #testSimplifiedPeriodGML3()}, but without beginning boundary.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException if an error occurred while marshalling.
     */
    @Test
    public void testBeforePeriodGML3() throws JAXBException {
        createContext();
        final var begin = new TimePeriodBound.GML3(null, "before");
        final var end   = new TimePeriodBound.GML3(TemporalObjects.createInstant(LocalDate.of(2007, 12, 31)), "after");
        testPeriod(begin, end,
                "<gml:TimePeriod xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:beginPosition indeterminatePosition=\"before\"/>\n" +
                "  <gml:endPosition>2007-12-31</gml:endPosition>\n" +
                "</gml:TimePeriod>\n", false);
    }

    /**
     * Same test as {@link #testSimplifiedPeriodGML3()}, but without end boundary.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException if an error occurred while marshalling.
     */
    @Test
    public void testAfterPeriodGML3() throws JAXBException {
        createContext();
        final var begin = new TimePeriodBound.GML3(TemporalObjects.createInstant(LocalDate.of(1992, 1, 2)), "before");
        final var end   = new TimePeriodBound.GML3(null, "after");
        testPeriod(begin, end,
                "<gml:TimePeriod xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:beginPosition>1992-01-02</gml:beginPosition>\n" +
                "  <gml:endPosition indeterminatePosition=\"after\"/>\n" +
                "</gml:TimePeriod>\n", false);
    }
}
