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
package org.apache.sis.test;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.XML;
import org.junit.After;

import static org.opengis.test.Assert.*;


/**
 * Base class of XML (un)marshalling tests. SIS (un)marshalling process can be partially controlled
 * by a {@link Context}, which defines (among other) the locale and timezone. Some tests will need
 * to fix the context to a particular locale and timezone before to execute the test.
 *
 * <p>The {@link #context} field can be initialized by subclasses either explicitely or by invoking
 * a {@code createContext(…)} convenience method. The {@link #clearContext()} method will be invoked
 * after each test for clearing the SIS internal {@link ThreadLocal} which was holding that context.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see XMLComparator
 */
public abstract strictfp class XMLTestCase extends TestCase {
    /**
     * A poll of configured {@link Marshaller} and {@link Unmarshaller} binded to the default set of classes.
     * The locale is set to {@link Locale#UK} (the language of ISO standards) and the timezone is arbitrarily
     * set to CET (Central European Time). We intentionally use a timezone different than UTC in order to have
     * an error of one or two hours if a code fails to take timezone offset in account.
     *
     * <p>This field is initially {@code null} and created when first needed.</p>
     */
    private static MarshallerPool defaultPool;

    /**
     * The context containing locale, timezone, GML version and other information.
     * The context is initially {@code null} and can be created either explicitely,
     * or by invoking the {@link #createContext(boolean, Locale, String)} convenience method.
     *
     * @see #createContext(boolean, Locale, String)
     * @see #clearContext()
     */
    protected Context context;

    /**
     * A buffer for {@link #marshal(Marshaller, Object)}, created only when first needed.
     */
    private StringWriter buffer;

    /**
     * Creates a new test case.
     */
    protected XMLTestCase() {
    }

    /**
     * Returns the default XML (un)marshaller pool potentially shared by test methods in all sub-classes.
     * The (un)marshallers locale is set to {@link Locale#UK} (the language of ISO standards) and their
     * timezone is arbitrarily set to CET (<cite>Central European Time</cite>).
     *
     * {@note We intentionally use a timezone different than UTC in order to have an error of one or two hours
     *        if a code fails to take timezone offset in account.}
     *
     * @return The shared (un)marshaller pool.
     * @throws JAXBException If an error occurred while creating the JAXB marshaller.
     */
    protected static synchronized MarshallerPool getMarshallerPool() throws JAXBException {
        if (defaultPool == null) {
            final Map<String,Object> properties = new HashMap<String,Object>(4);
            assertNull(properties.put(XML.LOCALE, Locale.UK));
            assertNull(properties.put(XML.TIMEZONE, "CET"));
            defaultPool = new MarshallerPool(properties);
        }
        return defaultPool;
    }

    /**
     * Initializes the {@link #context} to the given locale and timezone.
     *
     * @param marshal  {@code true} for setting the {@link Context#MARSHALLING} flag.
     * @param locale   The locale, or {@code null} for the default.
     * @param timezone The timezone, or {@code null} for the default.
     *
     * @see #clearContext()
     */
    protected final void createContext(final boolean marshal, final Locale locale, final String timezone) {
        context = new Context(marshal ? Context.MARSHALLING : 0, locale,
                (timezone != null) ? TimeZone.getTimeZone(timezone) : null, null, null, null, null, null);
    }

    /**
     * Resets {@link #context} to {@code null} and clears the {@link ThreadLocal} which was holding that context.
     * This method is automatically invoked by JUnit after each test, but can also be invoked explicitely before
     * to create a new context. It is safe to invoke this method more than once.
     */
    @After
    public final void clearContext() {
        assertSame("Unexpected context. Is this method invoked from the right thread?", context, Context.current());
        if (context != null) {
            context.finish();
            context = null;
        }
    }

    /**
     * Marshals the given object using the {@linkplain #getMarshallerPool() test marshaller pool}.
     *
     * @param  object The object to marshal.
     * @return The marshalled object.
     * @throws JAXBException If an error occurred while marshalling the object.
     */
    protected final String marshal(final Object object) throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        final String xml = marshal(marshaller, object);
        pool.recycle(marshaller);
        return xml;
    }

    /**
     * Marshals the given object using the given marshaler.
     *
     * @param  marshaller The marshaller to use.
     * @param  object The object to marshal.
     * @return The marshalled object.
     * @throws JAXBException If an error occurred while marshalling the object.
     */
    protected final String marshal(final Marshaller marshaller, final Object object) throws JAXBException {
        ArgumentChecks.ensureNonNull("marshaller", marshaller);
        ArgumentChecks.ensureNonNull("object", object);
        if (buffer == null) {
            buffer = new StringWriter();
        }
        buffer.getBuffer().setLength(0);
        marshaller.marshal(object, buffer);
        return buffer.toString();
    }

    /**
     * Unmarshals the given object using the {@linkplain #getMarshallerPool() test marshaller pool}.
     *
     * @param  <T>  Compile-time type of {@code type} argument.
     * @param  type The expected type of the unmarshalled object.
     * @param  xml  The XML representation of the object to unmarshal.
     * @return The unmarshalled object.
     * @throws JAXBException If an error occurred while unmarshalling the XML.
     */
    protected final <T> T unmarshal(final Class<T> type, final String xml) throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final Object object = unmarshal(unmarshaller, xml);
        pool.recycle(unmarshaller);
        assertInstanceOf("unmarshal", type, object);
        return type.cast(object);
    }

    /**
     * Unmarshals the given XML using the given unmarshaler.
     *
     * @param  unmarshaller The unmarshaller to use.
     * @param  xml The XML representation of the object to unmarshal.
     * @return The unmarshalled object.
     * @throws JAXBException If an error occurred while unmarshalling the XML.
     */
    protected final Object unmarshal(final Unmarshaller unmarshaller, final String xml) throws JAXBException {
        ArgumentChecks.ensureNonNull("unmarshaller", unmarshaller);
        ArgumentChecks.ensureNonNull("xml", xml);
        return unmarshaller.unmarshal(new StringReader(xml));
    }
}
