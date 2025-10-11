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
package org.apache.sis.xml.test;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;
import java.time.ZoneId;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.cat.CodeListUID;
import org.apache.sis.util.Version;

// Test dependencies
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.apache.sis.test.LoggingWatcher;


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
 *
 * @see DocumentComparator
 */
@Execution(ExecutionMode.SAME_THREAD)
public abstract class TestCase extends org.apache.sis.test.TestCase {
    /**
     * Base class of (un)marshalling tests that may emit logs.
     */
    @ResourceLock(value=LoggingWatcher.LOCK, mode=ResourceAccessMode.READ)
    public static abstract class WithLogs extends TestCase {
        /**
         * A JUnit extension for listening to log events.
         */
        @RegisterExtension
        public final LoggingWatcher loggings;

        /**
         * Creates a new test case which will listen to logs emitted by the given logger.
         *
         * @param  logger  the logger to listen to.
         */
        protected WithLogs(final Logger logger) {
            loggings = new LoggingWatcher(logger);
        }

        /**
         * Creates a new test case which will listen to logs emitted by the logger of the given name.
         *
         * @param logger  name of the logger to listen.
         */
        protected WithLogs(final String logger) {
            loggings = new LoggingWatcher(logger);
        }
    }

    /**
     * A dummy URL to not try to load. This URL is handled in a special way by the unmarshallers created
     * by {@link #getMarshallerPool()}: they will not try to download the document at this address.
     */
    protected static final String DUMMY_URL = "http://test.net";

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
                assertNull(properties.put(XML.RESOLVER, new TestReferenceResolver(DUMMY_URL)));
                defaultPool = new MarshallerPool(properties);
            }
            return defaultPool;
        }
    }

    /**
     * Initializes the {@link #context} to the given locale and timezone.
     *
     * @param  marshal   {@code true} for setting the {@link Context#MARSHALLING} flag.
     * @param  locale    the locale, or {@code null} for the default.
     * @param  timezone  the timezone, or {@code null} for the default.
     * @throws JAXBException if an error occurred while initializing the context.
     *
     * @see #clearContext()
     */
    protected final void createContext(final boolean marshal, final Locale locale, final String timezone) throws JAXBException {
        context = new Context(marshal ? Context.MARSHALLING : 0, getMarshallerPool(), locale,
                (timezone != null) ? ZoneId.of(timezone) : null, null, null, null, null, null, null, null);
    }

    /**
     * Resets {@link #context} to {@code null} and clears the {@link ThreadLocal} which was holding that context.
     * This method is automatically invoked by JUnit after each test, but can also be invoked explicitly before
     * to create a new context. It is safe to invoke this method more than once.
     */
    @AfterEach
    public final void clearContext() {
        assertSame(context, Context.current(), "Unexpected context. Is this method invoked from the right thread?");
        if (context != null) {
            context.finish();
            context = null;
        }
    }

    /**
     * Parses two <abbr>XML</abbr> trees as <abbr>DOM</abbr> documents and compares the nodes.
     * The inputs given to this method can be any of the following types:
     *
     * <ul>
     *   <li>{@link org.w3c.dom.Node}: used directly without further processing.</li>
     *   <li>{@link java.io.InputStream}: the stream is parsed as an XML document, then closed.</li>
     *   <li>{@link java.nio.file.Path}, {@link java.io.File}, {@link java.net.URL} or {@link java.net.URI}:
     *       the stream is opened, parsed as an XML document, then closed.</li>
     *   <li>{@link String}: The string content is parsed directly as a XML document.</li>
     * </ul>
     *
     * The comparison will ignore comments and the optional attributes given in arguments.
     *
     * <h4>Ignored attributes substitution</h4>
     * For convenience, this method replaces some well known prefixes in the {@code ignoredAttributes}
     * array by their full namespace URLs. For example, this method replaces{@code "xsi:schemaLocation"}
     * by {@code "http://www.w3.org/2001/XMLSchema-instance:schemaLocation"}.
     * If such substitution is not desired, consider using {@link DocumentComparator} directly instead.
     *
     * <p>The current substitution map is as below (may be expanded in any future SIS version):</p>
     *
     * <table class="sis">
     *   <caption>Predefined prefix mapping</caption>
     *   <tr><th>Prefix</th> <th>URL</th></tr>
     *   <tr><td>xmlns</td>  <td>{@code "http://www.w3.org/2000/xmlns"}</td></tr>
     *   <tr><td>xlink</td>  <td>{@value Namespaces#XLINK}</td></tr>
     *   <tr><td>xsi</td>    <td>{@value Namespaces#XSI}</td></tr>
     *   <tr><td>gml</td>    <td>{@value Namespaces#GML}</td></tr>
     *   <tr><td>gco</td>    <td>{@value Namespaces#GCO}</td></tr>
     *   <tr><td>gmd</td>    <td>{@value LegacyNamespaces#GMD}</td></tr>
     *   <tr><td>gmx</td>    <td>{@value LegacyNamespaces#GMX}</td></tr>
     *   <tr><td>gmi</td>    <td>{@value LegacyNamespaces#GMI}</td></tr>
     * </table>
     *
     * <p>For example, in order to ignore the namespace, type and schema location declaration,
     * the following strings can be given to the {@code ignoredAttributes} argument:</p>
     *
     * {@snippet :
     *   "xmlns:*", "xsi:schemaLocation", "xsi:type"
     *   }
     *
     * @param  expected           the expected XML document.
     * @param  actual             the XML document to compare.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     *
     * @see DocumentComparator
     */
    public static void assertXmlEquals(final Object expected, final Object actual, final String... ignoredAttributes) {
        assertXmlEquals(expected, actual, 0, null, ignoredAttributes);
    }

    /**
     * Parses two <abbr>XML</abbr> trees as <abbr>DOM</abbr> documents, and compares the nodes with the
     * given tolerance threshold for numerical values. The inputs given to this method can be any of the
     * types documented {@linkplain #assertXmlEquals(Object, Object, String[]) above}. This method will
     * ignore comments and the optional attributes given in arguments as documented in the above method.
     *
     * @param  expected           the expected XML document.
     * @param  actual             the XML document to compare.
     * @param  tolerance          the tolerance threshold for comparison of numerical values.
     * @param  ignoredNodes       the fully-qualified names of the nodes to ignore, or {@code null} if none.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     *
     * @see DocumentComparator
     */
    public static void assertXmlEquals(final Object expected, final Object actual,
            final double tolerance, final String[] ignoredNodes, final String[] ignoredAttributes)
    {
        final DocumentComparator comparator;
        try {
            comparator = new DocumentComparator(expected, actual);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            /*
             * We don't throw directly those exceptions since failing to parse the XML file can
             * be considered as part of test failures and the JUnit exception for such failures
             * is AssertionError. Having no checked exception in "assert" methods allow us to
             * declare the checked exceptions only for the library code being tested.
             */
            throw new AssertionError(e);
        }
        comparator.tolerance = tolerance;
        comparator.ignoreComments = true;
        if (ignoredNodes != null) {
            for (final String node : ignoredNodes) {
                comparator.ignoredNodes.add(DocumentComparator.substitutePrefix(node));
            }
        }
        if (ignoredAttributes != null) {
            for (final String attribute : ignoredAttributes) {
                comparator.ignoredAttributes.add(DocumentComparator.substitutePrefix(attribute));
            }
        }
        comparator.compare();
    }

    /**
     * Marshals the given object and ensures that the result is equal to the given string.
     * This convenience method uses a default set of attributes to ignore.
     *
     * @param  expected  the expected XML.
     * @param  object    the object to marshal.
     * @throws JAXBException if an error occurred during marshalling.
     */
    protected final void assertMarshalEquals(final String expected, final Object object) throws JAXBException {
        assertXmlEquals(expected, marshal(object), "xmlns:*");
    }

    /**
     * Marshals the given object and ensures that the result is equal to the content of the given stream.
     * The stream should be opened by a call to {@link Class#getResourceAsStream(String)} from the module
     * that contains the resource, and the stream will be closed by this method.
     *
     * @param  expected           a stream opened on an XML document with the expected content.
     * @param  object             the object to marshal.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     * @throws JAXBException if an error occurred during marshalling.
     *
     * @see #unmarshalFile(Class, String)
     */
    protected final void assertMarshalEqualsFile(final InputStream expected, final Object object,
            final String... ignoredAttributes) throws JAXBException
    {
        assertNotNull(expected, "Test resource is not found or not accessible.");
        try (expected) {
            assertXmlEquals(expected, marshal(object), ignoredAttributes);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Marshals the given object and ensures that the result is equal to the content of the given stream.
     * The stream should be opened by a call to {@link Class#getResourceAsStream(String)} from the module
     * that contains the resource, and the stream will be closed by this method.
     *
     * @param  expected           a stream opened on an XML document with the expected content.
     * @param  object             the object to marshal.
     * @param  metadataVersion    whether to marshal legacy 19139:2007 or newer ISO 19115-3 document. Can be {@code null}.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     * @throws JAXBException if an error occurred during marshalling.
     */
    protected final void assertMarshalEqualsFile(final InputStream expected, final Object object,
            final Version metadataVersion, final String... ignoredAttributes) throws JAXBException
    {
        assertNotNull(expected, "Test resource is not found or not accessible.");
        try (expected) {
            assertXmlEquals(expected, marshal(object, metadataVersion), ignoredAttributes);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Marshals the given object and ensures that the result is equal to the content of the given stream,
     * within a tolerance threshold for numerical values.
     * The stream should be opened by a call to {@link Class#getResourceAsStream(String)} from the module
     * that contains the resource, and the stream will be closed by this method.
     *
     * @param  expected           a stream opened on an XML document with the expected content.
     * @param  object             the object to marshal.
     * @param  metadataVersion    whether to marshal legacy 19139:2007 or newer ISO 19115-3 document. Can be {@code null}.
     * @param  tolerance          the tolerance threshold for comparison of numerical values.
     * @param  ignoredNodes       the fully-qualified names of the nodes to ignore, or {@code null} if none.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     * @throws JAXBException if an error occurred during marshalling.
     *
     * @see #unmarshalFile(Class, String)
     */
    protected final void assertMarshalEqualsFile(final InputStream expected, final Object object, final Version metadataVersion,
            final double tolerance, final String[] ignoredNodes, final String[] ignoredAttributes) throws JAXBException
    {
        assertNotNull(expected, "Test resource is not found or not accessible.");
        try (expected) {
            assertXmlEquals(expected, marshal(object, metadataVersion), tolerance, ignoredNodes, ignoredAttributes);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
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
        assertNotNull(marshaller, "marshaller");
        assertNotNull(object, "object");
        if (buffer == null) {
            buffer = new StringWriter();
        }
        buffer.getBuffer().setLength(0);
        marshaller.marshal(object, buffer);
        return buffer.toString();
    }

    /**
     * Unmarshals the content of the given test file using the {@linkplain #getMarshallerPool() test marshaller pool}.
     * The stream should be opened by a call to {@link Class#getResourceAsStream(String)} from the module
     * that contains the resource, and the stream will be closed by this method.
     *
     * @param  <T>    compile-time type of {@code type} argument.
     * @param  type   the expected type of the unmarshalled object.
     * @param  input  a stream opened on the document to unmarshall.
     * @return the object unmarshalled from the given file.
     * @throws JAXBException if an error occurred during unmarshalling.
     *
     * @see #assertMarshalEqualsFile(String, Object, String...)
     */
    protected final <T> T unmarshalFile(final Class<T> type, final InputStream input) throws JAXBException {
        assertNotNull(input, "Test resource is not found or not accessible.");
        try (input) {
            final MarshallerPool pool = getMarshallerPool();
            final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
            final Object object = unmarshaller.unmarshal(input);
            pool.recycle(unmarshaller);
            return type.cast(object);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
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
        assertInstanceOf(type, object, "unmarshal");
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
        assertNotNull(unmarshaller, "unmarshaller");
        assertNotNull(xml, "xml");
        return unmarshaller.unmarshal(new StringReader(xml));
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
