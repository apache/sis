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
package org.apache.sis.test.xml;

import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.internal.jaxb.cat.CodeListUID;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Version;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.XML;
import org.junit.After;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;


/**
 * Base class of tests which contain some XML (un)marshalling.
 * The subclasses do not need to be fully dedicated to XML.
 *
 * <p>SIS (un)marshalling process can be partially controlled by a {@link Context}, which defines (among other)
 * the locale and timezone. Some tests will need to fix the context to a particular locale and timezone before
 * to execute the test.</p>
 *
 * <p>The {@link #context} field can be initialized by subclasses either explicitly or by invoking
 * a {@code createContext(…)} convenience method. The {@link #clearContext()} method will be invoked
 * after each test for clearing the SIS internal {@link ThreadLocal} which was holding that context.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.1
 *
 * @see DocumentComparator
 *
 * @since 0.3
 */
public abstract class TestCase extends org.apache.sis.test.TestCase {
    /**
     * Miscellaneous version constants used for ISO standards.
     */
    protected static final Version VERSION_2007 = LegacyNamespaces.VERSION_2007,
                                   VERSION_2014 = LegacyNamespaces.VERSION_2014;

    /**
     * The timezone used for the tests. We intentionally use a timezone different than UTC in order
     * to have an error of one or two hours if a code fails to take timezone offset in account.
     */
    private static final String TIMEZONE = "CET";

    /**
     * Date parser and formatter using the {@code "yyyy-MM-dd HH:mm:ss"} pattern
     * and the time zone of the XML (un)marshallers used for the tests.
     */
    private static final DateFormat dateFormat;
    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
        dateFormat.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
        dateFormat.setLenient(false);
    };

    /**
     * A poll of configured {@link Marshaller} and {@link Unmarshaller} binded to the default set of classes.
     * The locale is set to {@link Locale#UK} (the language of ISO standards) and the timezone is arbitrarily
     * set to CET (Central European Time). We intentionally use a timezone different than UTC in order to have
     * an error of one or two hours if a code fails to take timezone offset in account.
     *
     * <p>This field is initially {@code null} and created when first needed.</p>
     *
     * @see #getMarshallerPool()
     */
    private static MarshallerPool defaultPool;

    /**
     * The context containing locale, timezone, GML version and other information.
     * The context is initially {@code null} and can be created either explicitly,
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
    protected TestCase() {
    }

    /**
     * Returns the XML (un)marshaller for the tests in this class. The default implementation
     * returns a XML (un)marshaller pool potentially shared by test methods in all sub-classes.
     * The (un)marshallers locale is set to {@link Locale#UK} (the language of ISO standards)
     * and their timezone is arbitrarily set to CET (<cite>Central European Time</cite>).
     *
     * We intentionally use a timezone different than UTC in order to have an error of one or two hours
     * if a code fails to take timezone offset in account.
     *
     * @return the shared (un)marshaller pool.
     * @throws JAXBException if an error occurred while creating the JAXB marshaller.
     */
    protected MarshallerPool getMarshallerPool() throws JAXBException {
        synchronized (TestCase.class) {
            if (defaultPool == null) {
                final Map<String,Object> properties = new HashMap<>(4);
                assertNull(properties.put(XML.LOCALE, Locale.UK));
                assertNull(properties.put(XML.TIMEZONE, TIMEZONE));
                assertNull(properties.put(XML.LENIENT_UNMARSHAL, Boolean.TRUE));
                defaultPool = new MarshallerPool(properties);
            }
            return defaultPool;
        }
    }

    /**
     * Initializes the {@link #context} to the given locale and timezone.
     *
     * @param marshal   {@code true} for setting the {@link Context#MARSHALLING} flag.
     * @param locale    the locale, or {@code null} for the default.
     * @param timezone  the timezone, or {@code null} for the default.
     *
     * @see #clearContext()
     */
    protected final void createContext(final boolean marshal, final Locale locale, final String timezone) {
        context = new Context(marshal ? Context.MARSHALLING : 0, locale,
                (timezone != null) ? TimeZone.getTimeZone(timezone) : null, null, null, null, null, null, null);
    }

    /**
     * Resets {@link #context} to {@code null} and clears the {@link ThreadLocal} which was holding that context.
     * This method is automatically invoked by JUnit after each test, but can also be invoked explicitly before
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
     * Returns the URL to the XML file of the given name.
     * The file shall be in the same package than a subclass of {@code this}.
     * This method begins the search in the package of {@link #getClass()}.
     * If the resource is not found in that package, then this method searches in the parent classes.
     * The intent is to allow some test classes to be overridden in different modules.
     *
     * @param  filename  the name of the XML file.
     * @return the URL to the given XML file.
     */
    private URL getResource(final String filename) {
        Class<?> c = getClass();
        do {
            final URL resource = c.getResource(filename);
            if (resource != null) return resource;
            c = c.getSuperclass();
        } while (!c.equals(TestCase.class));
        throw new AssertionError("Test resource not found: " + filename);
    }

    /**
     * Marshals the given object and ensure that the result is equal to the content of the given file.
     *
     * @param  filename           the name of the XML file in the package of the final subclass of {@code this}.
     * @param  object             the object to marshal.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     * @throws JAXBException if an error occurred during marshalling.
     *
     * @see #unmarshalFile(Class, String)
     */
    protected final void assertMarshalEqualsFile(final String filename, final Object object,
            final String... ignoredAttributes) throws JAXBException
    {
        assertXmlEquals(getResource(filename), marshal(object), ignoredAttributes);
    }

    /**
     * Marshals the given object and ensure that the result is equal to the content of the given file.
     *
     * @param  filename           the name of the XML file in the package of the final subclass of {@code this}.
     * @param  object             the object to marshal.
     * @param  metadataVersion    whether to marshal legacy 19139:2007 or newer ISO 19115-3 document. Can be {@code null}.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     * @throws JAXBException if an error occurred during marshalling.
     *
     * @since 1.0
     */
    protected final void assertMarshalEqualsFile(final String filename, final Object object,
            final Version metadataVersion, final String... ignoredAttributes) throws JAXBException
    {
        assertXmlEquals(getResource(filename), marshal(object, metadataVersion), ignoredAttributes);
    }

    /**
     * Marshals the given object and ensure that the result is equal to the content of the given file,
     * within a tolerance threshold for numerical values.
     *
     * @param  filename           the name of the XML file in the package of the final subclass of {@code this}.
     * @param  object             the object to marshal.
     * @param  metadataVersion    whether to marshal legacy 19139:2007 or newer ISO 19115-3 document. Can be {@code null}.
     * @param  tolerance          the tolerance threshold for comparison of numerical values.
     * @param  ignoredNodes       the fully-qualified names of the nodes to ignore, or {@code null} if none.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     * @throws JAXBException if an error occurred during marshalling.
     *
     * @see #unmarshalFile(Class, String)
     *
     * @since 1.0
     */
    protected final void assertMarshalEqualsFile(final String filename, final Object object, final Version metadataVersion,
            final double tolerance, final String[] ignoredNodes, final String[] ignoredAttributes) throws JAXBException
    {
        assertXmlEquals(getResource(filename), marshal(object, metadataVersion), tolerance, ignoredNodes, ignoredAttributes);
    }

    /**
     * Marshals the given object using the {@linkplain #getMarshallerPool() test marshaller pool}.
     * The default XML schema is used (usually the most recent one).
     *
     * @param  object  the object to marshal.
     * @return the marshalled object.
     * @throws JAXBException if an error occurred while marshalling the object.
     *
     * @see #unmarshal(Class, String)
     */
    protected final String marshal(final Object object) throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        final String xml = marshal(marshaller, object);
        pool.recycle(marshaller);
        return xml;
    }

    /**
     * Marshals the given object using the {@linkplain #getMarshallerPool() test marshaller pool}.
     * The XML schema identified by the given version is used.
     *
     * @param  object           the object to marshal.
     * @param  metadataVersion  whether to marshal legacy 19139:2007 or newer ISO 19115-3 document. Can be {@code null}.
     * @return the marshalled object.
     * @throws JAXBException if an error occurred while marshalling the object.
     *
     * @since 1.0
     */
    protected final String marshal(final Object object, final Version metadataVersion) throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.METADATA_VERSION, metadataVersion);
        final String xml = marshal(marshaller, object);
        pool.recycle(marshaller);
        return xml;
    }

    /**
     * Marshals the given object using the given marshaler.
     *
     * @param  marshaller  the marshaller to use.
     * @param  object      the object to marshal.
     * @return the marshalled object.
     * @throws JAXBException if an error occurred while marshalling the object.
     *
     * @see #unmarshal(Unmarshaller, String)
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
     * Unmarshals the content of the given test file using the {@linkplain #getMarshallerPool() test marshaller pool}.
     * The resource is obtained by a call to {@code getClass().getResource(filename)}, which implies that the file
     * shall be in the same package than the subclass of {@code this}.
     *
     * @param  <T>       compile-time type of {@code type} argument.
     * @param  type      the expected type of the unmarshalled object.
     * @param  filename  the name of the XML file in the package of the final subclass of {@code this}.
     * @return the object unmarshalled from the given file.
     * @throws JAXBException if an error occurred during unmarshalling.
     *
     * @see #assertMarshalEqualsFile(String, Object, String...)
     */
    protected final <T> T unmarshalFile(final Class<T> type, final String filename) throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final Object object = unmarshaller.unmarshal(getResource(filename));
        pool.recycle(unmarshaller);
        assertInstanceOf(filename, type, object);
        return type.cast(object);
    }

    /**
     * Unmarshals the given object using the {@linkplain #getMarshallerPool() test marshaller pool}.
     *
     * @param  <T>   compile-time type of {@code type} argument.
     * @param  type  the expected type of the unmarshalled object.
     * @param  xml   the XML representation of the object to unmarshal.
     * @return the unmarshalled object.
     * @throws JAXBException if an error occurred while unmarshalling the XML.
     *
     * @see #marshal(Object)
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
     * Unmarshals the given XML using the given unmarshaller.
     *
     * @param  unmarshaller  the unmarshaller to use.
     * @param  xml           the XML representation of the object to unmarshal.
     * @return the unmarshalled object.
     * @throws JAXBException if an error occurred while unmarshalling the XML.
     *
     * @see #marshal(Marshaller, Object)
     */
    protected final Object unmarshal(final Unmarshaller unmarshaller, final String xml) throws JAXBException {
        ArgumentChecks.ensureNonNull("unmarshaller", unmarshaller);
        ArgumentChecks.ensureNonNull("xml", xml);
        return unmarshaller.unmarshal(new StringReader(xml));
    }

    /**
     * Parses the date for the given string using the {@code "yyyy-MM-dd HH:mm:ss"} pattern
     * and the time zone of the XML (un)marshallers used for the tests.
     *
     * @param  date  the date as a {@link String}.
     * @return the date as a {@link Date}.
     */
    protected static Date xmlDate(final String date) {
        ArgumentChecks.ensureNonNull("date", date);
        try {
            synchronized (dateFormat) {
                return dateFormat.parse(date);
            }
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * The string substitutions to perform for downgrading an ISO 19115-3 document to ISO 19139:2007.
     * Values at even indices are strings to search, and values at odd indices are replacements.
     */
    private static final String[] TO_LEGACY_XML = {
        Namespaces.CIT, LegacyNamespaces.GMD, "cit",  "gmd",
        Namespaces.MCC, LegacyNamespaces.GMD, "mcc",  "gmd",
        Namespaces.MRI, LegacyNamespaces.GMD, "mri",  "gmd",
        Namespaces.GCO, LegacyNamespaces.GCO,      // "gco"
        Namespaces.GCX, LegacyNamespaces.GMX, "gcx",  "gmx",
        CodeListUID.METADATA_ROOT,  CodeListUID.METADATA_ROOT_LEGACY,           // For code lists.
        CodeListUID.CODELISTS_PATH, CodeListUID.CODELISTS_PATH_LEGACY
    };

    /**
     * Performs a simple ISO 19115-3 to ISO 19139:2007 translations using only search-and-replaces.
     * For example, this method replaces {@code "cit"} prefix by {@code "gmd"} and the corresponding
     * {@value Namespaces#CIT} namespace by {@value LegacyNamespaces#GMD}. However, this method does
     * not perform any more complex translations like attributes refactored in other classes.  If a
     * more complex translation is required, the test case should provide the legacy XML verbatim
     * in a separated string.
     *
     * @param  xml  an XML compliant with ISO 19115-3.
     * @return an XML compliant with ISO 19139:2007.
     *
     * @since 1.0
     */
    protected static String toLegacyXML(final String xml) {
        final StringBuilder buffer = new StringBuilder(xml);
        for (int c=0; c < TO_LEGACY_XML.length;) {
            final String toSearch  = TO_LEGACY_XML[c++];
            final String replaceBy = TO_LEGACY_XML[c++];
            final int length = toSearch.length();
            int i = buffer.length();
            while ((i = buffer.lastIndexOf(toSearch, i)) >= 0) {
                /*
                 * Following may throw a StringIndexOutOfBoundsException if 'toSearch' is located at the
                 * beginning (i == 0) or end (end == buffer.length()) of the buffer. However, those cases
                 * should never happen in Apache SIS test cases since it would be invalid XML.
                 */
                if (!Character.isUnicodeIdentifierPart(buffer.codePointBefore(i))) {
                    final int end = i + length;
                    if (!Character.isUnicodeIdentifierPart(buffer.codePointAt(end))) {
                        buffer.replace(i, end, replaceBy);
                    }
                }
                i -= length;
            }
        }
        return buffer.toString();
    }
}
