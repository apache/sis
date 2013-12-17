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

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.internal.jaxb.XmlUtilities;
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
     * A poll of configured {@link Marshaller} and {@link Unmarshaller}, created when first needed.
     */
    private static MarshallerPool pool;

    /**
     * Set the marshalling context to a fixed locale and timezone before to create the
     * JAXB wrappers for temporal objects.
     */
    private void createContext() {
        createContext(true, Locale.FRANCE, "CET");
    }

    /**
     * Creates the XML (un)marshaller pool to be shared by all test methods.
     * The (un)marshallers locale and timezone will be set to fixed values.
     *
     * @throws JAXBException If an error occurred while creating the pool.
     *
     * @see #disposeMarshallerPool()
     */
    @BeforeClass
    public static void createMarshallerPool() throws JAXBException {
        final Map<String,Object> properties = new HashMap<>(4);
        assertNull(properties.put(XML.LOCALE, Locale.FRANCE));
        assertNull(properties.put(XML.TIMEZONE, "CET"));
        pool = new MarshallerPool(JAXBContext.newInstance(TimeInstant.class, TimePeriod.class), properties);
    }

    /**
     * Invoked by JUnit after the execution of every tests in order to dispose
     * the {@link MarshallerPool} instance used internally by this class.
     */
    @AfterClass
    public static void disposeMarshallerPool() {
        pool = null;
    }

    /**
     * Tests time instant. The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException If an error occurred while marshalling.
     * @throws DatatypeConfigurationException Should never happen.
     */
    @Test
    public void testTimeInstant() throws JAXBException, DatatypeConfigurationException {
        createContext();
        final Marshaller   marshaller   = pool.acquireMarshaller();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final TimeInstant  instant      = new TimeInstant();
        instant.timePosition = XmlUtilities.toXML(context, date("1992-01-01 00:00:00"));
        final String actual = marshal(marshaller, instant);
        assertXmlEquals(
                "<gml:TimeInstant xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:timePosition>1992-01-01T01:00:00.000+01:00</gml:timePosition>\n" +
                "</gml:TimeInstant>\n", actual, "xmlns:*");

        final TimeInstant test = (TimeInstant) unmarshal(unmarshaller, actual);
        assertEquals("1992-01-01 00:00:00", format(XmlUtilities.toDate(context, test.timePosition)));

        pool.recycle(marshaller);
        pool.recycle(unmarshaller);
    }

    /**
     * Tests a time period using the GML 2 syntax.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException If an error occurred while marshalling.
     */
    @Test
    public void testPeriodGML2() throws JAXBException {
        createContext();
        final TimePeriodBound begin = new TimePeriodBound.GML2(new DummyInstant(date("1992-01-01 00:00:00")));
        final TimePeriodBound end   = new TimePeriodBound.GML2(new DummyInstant(date("2007-12-31 00:00:00")));
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
     * @param expected The expected string.
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
        final TimePeriod test = (TimePeriod) unmarshal(unmarshaller, actual);
        if (verifyValues) {
            assertEquals("1992-01-01 00:00:00", format(XmlUtilities.toDate(context, test.begin.calendar())));
            assertEquals("2007-12-31 00:00:00", format(XmlUtilities.toDate(context, test.end  .calendar())));
        }
        pool.recycle(marshaller);
        pool.recycle(unmarshaller);
    }

    /**
     * Tests a time period using the GML 3 syntax.
     * The test is executed using an arbitrary locale and timezone.
     *
     * @throws JAXBException If an error occurred while marshalling.
     */
    @Test
    public void testPeriodGML3() throws JAXBException {
        createContext();
        final TimePeriodBound begin = new TimePeriodBound.GML3(new DummyInstant(date("1992-01-01 00:00:00")), "before");
        final TimePeriodBound end   = new TimePeriodBound.GML3(new DummyInstant(date("2007-12-31 00:00:00")), "after");
        testPeriod(begin, end,
                "<gml:TimePeriod xmlns:gml=\"" + Namespaces.GML + "\">\n" +
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
        createContext();
        final TimePeriodBound begin = new TimePeriodBound.GML3(new DummyInstant(date("1992-01-01 23:00:00")), "before");
        final TimePeriodBound end   = new TimePeriodBound.GML3(new DummyInstant(date("2007-12-30 23:00:00")), "after");
        testPeriod(begin, end,
                "<gml:TimePeriod xmlns:gml=\"" + Namespaces.GML + "\">\n" +
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
        createContext();
        final TimePeriodBound begin = new TimePeriodBound.GML3(null, "before");
        final TimePeriodBound end   = new TimePeriodBound.GML3(new DummyInstant(date("2007-12-30 23:00:00")), "after");
        testPeriod(begin, end,
                "<gml:TimePeriod xmlns:gml=\"" + Namespaces.GML + "\">\n" +
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
        createContext();
        final TimePeriodBound begin = new TimePeriodBound.GML3(new DummyInstant(date("1992-01-01 23:00:00")), "before");
        final TimePeriodBound end   = new TimePeriodBound.GML3(null, "after");
        testPeriod(begin, end,
                "<gml:TimePeriod xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:beginPosition>1992-01-02</gml:beginPosition>\n" +
                "  <gml:endPosition indeterminatePosition=\"after\"/>\n" +
                "</gml:TimePeriod>\n", false);
    }
}
