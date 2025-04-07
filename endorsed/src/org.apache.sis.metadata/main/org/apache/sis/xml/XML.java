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

import java.time.ZoneId;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Filter;
import java.util.logging.LogRecord;             // For javadoc
import java.net.URISyntaxException;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.util.Static;
import org.apache.sis.util.Version;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.system.Modules;
import org.apache.sis.system.SystemListener;
import org.apache.sis.xml.privy.URISource;
import org.apache.sis.xml.bind.TypeRegistration;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Provides convenience methods for marshalling and unmarshalling SIS objects.
 * Marshalling operations use the standard versions listed below
 * (for marshalling a document in a different version, see {@link MarshallerPool}).
 * Unmarshalling detects the version automatically.
 *
 * <table class="sis">
 *   <caption>Versions of standards applied at marshalling time</caption>
 *   <tr><th>Topic</th>       <th>SIS 0.3 to 0.8</th>  <th>SIS 1.0</th>          <th>Remarks</th></tr>
 *   <tr><td>Metadata</td>    <td>ISO 19139:2007</td>  <td>ISO 19115-3:2016</td> <td></td></tr>
 *   <tr><td>Referencing</td> <td>ISO 19136:2007</td>  <td>ISO 19136:2007</td>   <td>Same as GML 3.2</td></tr>
 * </table>
 *
 * This class defines also some property keys that can be given to the {@link Marshaller}
 * and {@link Unmarshaller} instances created by {@link MarshallerPool}:
 *
 * <table class="sis">
 *   <caption>Supported (un)marshaller properties</caption>
 *   <tr><th>Key</th>                         <th>Value type</th>                <th>Purpose</th></tr>
 *   <tr><td>{@link #LOCALE}</td>             <td>{@link Locale}</td>            <td>for specifying the locale to use for international strings and code lists.</td></tr>
 *   <tr><td>{@link #TIMEZONE}</td>           <td>{@link TimeZone}</td>          <td>for specifying the timezone to use for dates and times.</td></tr>
 *   <tr><td>{@link #SCHEMAS}</td>            <td>{@link Map}</td>               <td>for specifying the root URL of metadata schemas to use.</td></tr>
 *   <tr><td>{@link #GML_VERSION}</td>        <td>{@link Version}</td>           <td>for specifying the GML version of the document to be (un)marshalled.</td></tr>
 *   <tr><td>{@link #METADATA_VERSION}</td>   <td>{@link Version}</td>           <td>for specifying the metadata version of the document to be (un)marshalled.</td></tr>
 *   <tr><td>{@link #RESOLVER}</td>           <td>{@link ReferenceResolver}</td> <td>for replacing {@code xlink} or {@code uuidref} attributes by the actual object to use.</td></tr>
 *   <tr><td>{@link #CONVERTER}</td>          <td>{@link ValueConverter}</td>    <td>for controlling the conversion of URL, UUID, Units or similar objects.</td></tr>
 *   <tr><td>{@link #STRING_SUBSTITUTES}</td> <td>{@code String[]}</td>          <td>for specifying which code lists to replace by simpler {@code <gco:CharacterString>} elements.</td></tr>
 *   <tr><td>{@link #WARNING_FILTER}</td>     <td>{@link Filter}</td>            <td>for being notified about non-fatal warnings.</td></tr>
 * </table>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.5
 * @since   0.3
 */
public final class XML extends Static {
    /**
     * Specifies the locale to use for marshalling
     * {@link org.opengis.util.InternationalString} and {@link org.opengis.util.CodeList}
     * instances. The value for this property shall be an instance of {@link Locale} or a
     * {@link CharSequence} recognized by {@link org.apache.sis.util.Locales#parse(String)}.
     *
     * <p>This property is mostly for marshallers. However, this property can also be used at
     * unmarshalling time, for example if a {@code <lan:PT_FreeText>} element containing
     * many localized strings need to be represented in a Java {@link String} object. In
     * such case, the unmarshaller will try to pickup a string in the language specified
     * by this property.</p>
     *
     * <h4>Default behavior</h4>
     * If this property is never set, then (un)marshalling will try to use "unlocalized" strings -
     * typically some programmatic strings like {@linkplain org.opengis.annotation.UML#identifier()
     * UML identifiers}. While such identifiers often look like English words, they are not
     * considered as the {@linkplain Locale#ENGLISH English} localization.
     * The algorithm attempting to find a "unlocalized" string is defined in the
     * {@link org.apache.sis.util.DefaultInternationalString#toString(Locale)} javadoc.
     *
     * <h4>Special case</h4>
     * If the object to be marshalled is an instance of
     * {@link org.apache.sis.metadata.iso.DefaultMetadata}, then the value given to its
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#setLanguage(Locale) setLanguage(Locale)}
     * method will have precedence over this property. This behavior is compliant with INSPIRE rules.
     *
     * @see org.apache.sis.setup.OptionKey#LOCALE
     * @see Marshaller#setProperty(String, Object)
     * @see org.apache.sis.metadata.iso.DefaultMetadata#setLanguage(Locale)
     */
    public static final String LOCALE = "org.apache.sis.xml.locale";

    /**
     * Specifies the timezone to use for marshalling dates and times.
     * The value for this property shall be an instance of {@link ZoneId}, {@link TimeZone}, or a
     * {@link CharSequence} recognized by {@link ZoneId#of(String)} or {@link TimeZone#getTimeZone(String)}.
     *
     * <h4>Default behavior</h4>
     * If this property is never set, then (un)marshalling will use the
     * {@linkplain TimeZone#getDefault() default timezone}.
     *
     * @see org.apache.sis.setup.OptionKey#TIMEZONE
     */
    public static final String TIMEZONE = "org.apache.sis.xml.timezone";

    /**
     * Specifies the root URLs of some schemas.
     * This property modifies only the URL strings; it does not change the structure of
     * marshalled XML documents (for content structure, see {@link #METADATA_VERSION}).
     * The value for this property shall be an instance of {@link Map Map&lt;String,String&gt;}.
     * This property controls the URLs to be used when marshalling the following elements:
     *
     * <ul>
     *   <li>The value of the {@code codeList} attribute when marshalling subclasses of
     *       {@link org.opengis.util.CodeList}.</li>
     *   <li>The value of the {@code uom} attribute when marshalling measures
     *       (for example {@code <gco:Distance>}).</li>
     * </ul>
     *
     * <h4>Examples</h4>
     * URLs in code lists and is units of measurement may appear as below.
     * The underlined fragment is the part that can be replaced by {@code SCHEMAS} values:
     * <ul>
     *   <li><code><u>http://standards.iso.org/iso/19115/</u>resources/Codelist/cat/codelists.xml#LanguageCode</code></li>
     *   <li><code><u>http://www.isotc211.org/2005/</u>resources/Codelist/gmxCodelists.xml#LanguageCode</code></li>
     *   <li><code><u>http://www.isotc211.org/2005/</u>resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])</code></li>
     * </ul>
     *
     * <h4>Implementation note</h4>
     * The currently recognized keys are listed below.
     * The entries to be used depend on the {@linkplain #METADATA_VERSION metadata version} to be marshalled.
     * For example, the {@code "cat"} entry is used when marshalling ISO 19115-3:2016 document, while the
     * {@code "gmd"} and {@code "gmi"} entries are used when marshalling ISO 19139:2007 documents.
     * The following table gives some typical URLs, with the default URL in bold characters:
     *
     * <table class="sis">
     *   <caption>Supported root URLs</caption>
     *   <tr>
     *     <th>Map key</th>
     *     <th>Typical values (choose only one)</th>
     *   </tr><tr>
     *     <td><b>cat</b></td>
     *     <td><b>http://standards.iso.org/iso/19115/</b></td>
     *   </tr><tr>
     *     <td class="hsep"><b>gmd</b></td>
     *     <td class="hsep">
     *         <b>http://www.isotc211.org/2005/</b><br>
     *            http://schemas.opengis.net/iso/19139/20070417/<br>
     *            http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/</td>
     *   </tr>
     * </table>
     *
     * Additional keys, if any, are ignored. Future SIS versions may recognize more keys.
     */
    public static final String SCHEMAS = "org.apache.sis.xml.schemas";
    // If more keys are documented, update the Pooled.SCHEMAS_KEY array.

    /**
     * Specifies the GML version of the document to be marshalled or unmarshalled.
     * The GML version may affect the set of XML elements to be marshalled and their namespaces.
     * Note that GML 3.2 is identical to ISO 19136:2007.
     *
     * The value can be {@link String} or {@link Version} object.
     * If no version is specified, then the most recent supported GML version is assumed.
     *
     * <h4>Supported GML versions</h4>
     * Apache SIS currently supports GML 3.2.1 by default. SIS can read and write GML 3.2
     * if this property is set to "3.2". It is also possible to set this property to "3.1",
     * but the marshalled XML is not GML 3.1.1 conformant because of the differences between the two schemas.
     * See <a href="http://issues.apache.org/jira/browse/SIS-160">SIS-160: Need XSLT between GML 3.1 and 3.2</a>
     * for information about the status of GML 3.1.1 support.
     *
     * <h4>Compatibility note</h4>
     * Newer GML versions typically have more elements, but not always. For example, in {@code <gml:VerticalDatum>},
     * the {@code <gml:verticalDatumType>} property presents in GML 3.0 and 3.1 has been removed in GML 3.2.
     */
    public static final String GML_VERSION = "org.apache.sis.gml.version";

    /**
     * Specifies the metadata version of the document to be marshalled or unmarshalled.
     * The metadata version may affect the set of XML elements to be marshalled and their namespaces.
     * The value can be {@link String} or {@link Version} object.
     * If no version is specified, then the most recent supported metadata version is assumed.
     *
     * <p>The metadata version may be ignored when the metadata to marshal is inside a GML element.
     * For example, the {@code <gml:domainOfValidity>} element inside a coordinate reference system
     * is always marshalled using ISO 19139:2007 if the enclosing element uses GML 3.2 schema.</p>
     *
     * <h4>Supported metadata versions</h4>
     * Apache SIS currently supports ISO 19115-3:2016 by default. This version can be explicitly
     * set with value "2014" or above (because the abstract model was defined in ISO 19115-1:2014).
     * SIS can write legacy ISO 19139:2007 documents if this property is set to a value less than "2014".
     * Both versions can be read without the need to specify this property.
     *
     * @since 1.0
     */
    public static final String METADATA_VERSION = "org.apache.sis.xml.version.metadata";

    /**
     * Specifies whether the unmarshalling process should accept any metadata or GML supported version
     * if the user did not specified an explicit version. The value can be a {@link Boolean} instance,
     * or {@code "true"} or {@code "false"} as a {@link String}. If this value is not specified, then
     * the default is {@code true} for all {@code XML.unmarshal} methods and {@code false} otherwise.
     *
     * <p>Metadata and Geographic Markup Language have slightly different XML encoding depending on the
     * OGC/ISO version in use. Often the namespaces are different, but not only. Internally, Apache SIS
     * supports only the schema versions documented in this {@linkplain XML class javadoc}, for example
     * the ISO 19115-3:2016 version of metadata schema.  For unmarshalling a document encoded according
     * an older metadata schema (e.g. ISO 19139:2007), a transformation is applied on-the-fly.  However
     * this transformation may sometimes produce undesirable results or make debugging more difficult.
     * For this reason {@link MarshallerPool} applies the transformation only if explicitly requested,
     * either by setting a {@link #METADATA_VERSION} or {@link #GML_VERSION} explicitly, or by setting
     * this {@code LENIENT_UNMARSHAL} property to {@code true} if the version to unmarshal is not known
     * in advance.</p>
     *
     * @since 1.0
     */
    public static final String LENIENT_UNMARSHAL = "org.apache.sis.xml.lenient";

    /**
     * Allows client code to replace {@code xlink} or {@code uuidref} attributes by the actual objects to use.
     * The value for this property shall be an instance of {@link ReferenceResolver}.
     *
     * <p>If a property in a XML document is defined only by {@code xlink} or {@code uuidref} attributes,
     * without any concrete definition, then the default behavior is as below:</p>
     *
     * <ul>
     *   <li>If the reference is of the form {@code xlink:href="#foo"} and an object with the {@code gml:id="foo"}
     *       attribute was previously found in the same XML document, then that object will be used.</li>
     *   <li>Otherwise, if {@code xlink:href} references an external document, that document is unmarshalled.
     *       The URI resolution can be controlled with an {@link javax.xml.transform.URIResolver} specified
     *       at construction time.</li>
     *   <li>Otherwise, an empty element containing only the values of the above-cited attributes is created.</li>
     * </ul>
     *
     * Applications can sometimes do better by using some domain-specific knowledge, for example by searching in a
     * database. Users can define their search algorithm by subclassing {@link ReferenceResolver} and configuring
     * a unmarshaller as below:
     *
     * {@snippet lang="java" :
     *     ReferenceResolver  myResolver = ...;
     *     Map<String,Object> properties = new HashMap<>();
     *     properties.put(XML.RESOLVER, myResolver);
     *     Object obj = XML.unmarshal(source, properties);
     *     }
     *
     * @see Unmarshaller#setProperty(String, Object)
     * @see ReferenceResolver
     */
    public static final String RESOLVER = "org.apache.sis.xml.resolver";

    /**
     * Controls the behaviors of the (un)marshalling process when an element cannot be processed,
     * or alter the element values. The value for this property shall be an instance of {@link ValueConverter}.
     *
     * <p>If an element in a XML document cannot be parsed (for example if a {@linkplain java.net.URL}
     * string is not valid), the default behavior is to throw an exception which cause the
     * (un)marshalling of the entire document to fail. This default behavior can be customized by
     * invoking {@link Marshaller#setProperty(String, Object)} with this {@code CONVERTER} property
     * key and a custom {@link ValueConverter} instance. {@code ValueConverter} can also be used
     * for replacing an erroneous URL by a fixed URL. See the {@link ValueConverter} javadoc for
     * more details.</p>
     *
     * <h4>Example</h4>
     * The following example collects the failures in a list without stopping the (un)marshalling process.
     *
     * {@snippet lang="java" :
     *     class WarningCollector extends ValueConverter {
     *         // The warnings collected during (un)marshalling.
     *         List<String> messages = new ArrayList<String>();
     *
     *         // Override the default implementation in order to
     *         // collect the warnings and allow the process to continue.
     *         @Override
     *         protected <T> boolean exceptionOccured(MarshalContext context,
     *                 T value, Class<T> sourceType, Class<T> targetType, Exception e)
     *         {
     *             mesages.add(e.getLocalizedMessage());
     *             return true;
     *         }
     *     }
     *
     *     // Unmarshal a XML string, trapping some kind of errors.
     *     // Not all errors are trapped - see the ValueConverter
     *     // javadoc for more details.
     *     WarningCollector myWarningList = new WarningCollector();
     *     Map<String,Object> properties = new HashMap<>();
     *     properties.put(XML.CONVERTER, myWarningList);
     *     Object obj = XML.unmarshal(source, properties);
     *     if (!myWarningList.isEmpty()) {
     *         // Report here the warnings to the user.
     *     }
     * }
     *
     * @see Unmarshaller#setProperty(String, Object)
     * @see ValueConverter
     */
    public static final String CONVERTER = "org.apache.sis.xml.converter";

    /**
     * Allows marshallers to substitute some code lists by the simpler {@code <gco:CharacterString>} element.
     * The value for this property shall be a {@code String[]} array of any of the following values:
     *
     * <ul>
     *   <li>"{@code language}" for substituting {@code <lan:LanguageCode>} elements</li>
     *   <li>"{@code country}"  for substituting {@code <lan:Country>} elements</li>
     *   <li>"{@code filename}" for substituting {@code <gcx:FileName>} elements</li>
     *   <li>"{@code mimetype}" for substituting {@code <gcx:MimeFileType>} elements</li>
     * </ul>
     *
     * <h4>Example</h4>
     * INSPIRE compliant language code shall be formatted like below (details may vary):
     *
     * {@snippet lang="xml" :
     *   <gmd:language>
     *     <gmd:LanguageCode
     *         codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#LanguageCode"
     *         codeListValue="fra">French</gmd:LanguageCode>
     *   </gmd:language>
     * }
     *
     * However if this property contains the "{@code language}" value, then the marshaller will format
     * the language code like below (which is legal according OGC schemas, but is not INSPIRE compliant):
     *
     * {@snippet lang="xml" :
     *   <lan:language>
     *     <gco:CharacterString>fra</gco:CharacterString>
     *   </lan:language>
     * }
     */
    public static final String STRING_SUBSTITUTES = "org.apache.sis.xml.stringSubstitutes";

    /**
     * Specifies a listener to be notified when a non-fatal error occurred during the (un)marshalling.
     * The value for this property shall be an instance of {@link Filter}.
     *
     * <p>By default, warnings that occur during the (un)marshalling process are logged. However, if a
     * property is set for this key, then the {@link Filter#isLoggable(LogRecord)} method will be invoked.
     * If that method returns {@code false}, then the warning will not be logged by the (un)marshaller.</p>
     *
     * @since 1.0
     */
    public static final String WARNING_FILTER = "org.apache.sis.xml.warningFilter";

    /**
     * The pool of marshallers and unmarshallers used by this class.
     * The field name uses the uppercase convention because this field is almost constant:
     * this field is initially null, then created by {@link #getPool()} when first needed.
     * Once created the field value usually doesn't change. However, the field may be reset
     * to {@code null} when modules are loaded or unloaded by a container such as OSGi,
     * because the set of classes returned by {@link TypeRegistration#load(boolean)} may have changed.
     *
     * @see #getPool()
     */
    private static volatile MarshallerPool POOL;

    /**
     * Registers a listener for module path changes. In such case, a new pool will need to
     * be created because the {@code JAXBContext} may be different.
     */
    static {
        SystemListener.add(new SystemListener(Modules.UTILITIES) {
            @Override protected void classpathChanged() {
                POOL = null;
            }
        });
    }

    /**
     * Do not allow instantiation on this class.
     */
    private XML() {
    }

    /**
     * Returns the default (un)marshaller pool used by all methods in this class.
     *
     * <h4>Implementation note</h4>
     * Current implementation uses the double-check idiom. This is usually a deprecated practice
     * (the recommended alterative is to use static class initialization), but in this particular
     * case the field may be reset to {@code null} if modules are loaded or unloaded by a container,
     * so static class initialization would be a little bit too rigid.
     */
    @SuppressWarnings("DoubleCheckedLocking")
    private static MarshallerPool getPool() throws JAXBException {
        MarshallerPool pool = POOL;
        if (pool == null) {
            synchronized (XML.class) {
                pool = POOL;                            // Double-check idiom: see javadoc.
                if (pool == null) {
                    POOL = pool = new MarshallerPool(Map.of(LENIENT_UNMARSHAL, Boolean.TRUE));
                }
            }
        }
        return pool;
    }

    /**
     * Marshal the given object into a string.
     *
     * @param  object  the root of content tree to be marshalled.
     * @return the XML representation of the given object.
     * @throws JAXBException if an error occurred during the marshalling.
     */
    public static String marshal(final Object object) throws JAXBException {
        ensureNonNull("object", object);
        final StringWriter output = new StringWriter();
        final MarshallerPool pool = getPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.marshal(object, output);
        pool.recycle(marshaller);
        return output.toString();
    }

    /**
     * Marshal the given object into a stream.
     *
     * @param  object  the root of content tree to be marshalled.
     * @param  output  the stream where to write.
     * @throws JAXBException if an error occurred during the marshalling.
     */
    public static void marshal(final Object object, final OutputStream output) throws JAXBException {
        ensureNonNull("object", object);
        ensureNonNull("output", output);
        final MarshallerPool pool = getPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.marshal(object, output);
        pool.recycle(marshaller);
    }

    /**
     * Marshal the given object into a file.
     *
     * @param  object  the root of content tree to be marshalled.
     * @param  output  the file to be written.
     * @throws JAXBException if an error occurred during the marshalling.
     */
    public static void marshal(final Object object, final File output) throws JAXBException {
        ensureNonNull("object", object);
        ensureNonNull("output", output);
        final MarshallerPool pool = getPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.marshal(object, output);
        pool.recycle(marshaller);
    }

    /**
     * Marshal the given object into a path.
     *
     * @param  object  the root of content tree to be marshalled.
     * @param  output  the file to be written.
     * @throws JAXBException if an error occurred during the marshalling.
     */
    public static void marshal(final Object object, final Path output) throws JAXBException {
        ensureNonNull("object", object);
        ensureNonNull("output", output);
        try (OutputStream out = Files.newOutputStream(output, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            final MarshallerPool pool = getPool();
            final Marshaller marshaller = pool.acquireMarshaller();
            marshaller.marshal(object, out);
            pool.recycle(marshaller);
        } catch (IOException e) {
            throw new JAXBException(Errors.format(Errors.Keys.CanNotOpen_1, output), e);
        }
    }

    /**
     * Marshal the given object to a stream, DOM or other destinations.
     * This is the most flexible marshalling method provided in this {@code XML} class.
     * The destination is specified by the {@code output} argument implementation, for example
     * {@link javax.xml.transform.stream.StreamResult} for writing to a file or output stream.
     * The optional {@code properties} map can contain any key documented in this {@code XML} class,
     * together with the keys documented in the <i>supported properties</i> section of the
     * {@link Marshaller} class.
     *
     * @param  object      the root of content tree to be marshalled.
     * @param  output      the file to be written.
     * @param  properties  an optional map of properties to give to the marshaller, or {@code null} if none.
     * @throws JAXBException if a property has an illegal value, or if an error occurred during the marshalling.
     *
     * @since 0.4
     */
    public static void marshal(final Object object, final Result output, final Map<String,?> properties) throws JAXBException {
        ensureNonNull("object", object);
        ensureNonNull("output", output);
        final MarshallerPool pool = getPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        if (properties != null) {
            for (final Map.Entry<String,?> entry : properties.entrySet()) {
                marshaller.setProperty(entry.getKey(), entry.getValue());
            }
        }
        /*
         * STAX results are not handled by JAXB. We have to handle those cases ourselves.
         * This workaround should be removed if a future JDK version handles those cases.
         */
        if (output instanceof StAXResult) {
            @Workaround(library = "JDK", version = "1.8")
            final XMLStreamWriter writer = ((StAXResult) output).getXMLStreamWriter();
            if (writer != null) {
                marshaller.marshal(object, writer);
            } else {
                marshaller.marshal(object, ((StAXResult) output).getXMLEventWriter());
            }
        } else {
            marshaller.marshal(object, output);
        }
        pool.recycle(marshaller);
    }

    /**
     * Unmarshal an object from the given string.
     * Note that the given argument is the XML document itself,
     * <strong>not</strong> a URL to a XML document.
     *
     * @param  xml  the XML representation of an object.
     * @return the object unmarshalled from the given input.
     * @throws JAXBException if an error occurred during the unmarshalling.
     */
    public static Object unmarshal(final String xml) throws JAXBException {
        ensureNonNull("input", xml);
        final StringReader in = new StringReader(xml);
        final MarshallerPool pool = getPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final Object object = unmarshaller.unmarshal(in);
        pool.recycle(unmarshaller);
        return object;
    }

    /**
     * Unmarshal an object from the given stream.
     *
     * @param  input  the stream from which to read a XML representation.
     * @return the object unmarshalled from the given input.
     * @throws JAXBException if an error occurred during the unmarshalling.
     */
    public static Object unmarshal(final InputStream input) throws JAXBException {
        ensureNonNull("input", input);
        final MarshallerPool pool = getPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final Object object = unmarshaller.unmarshal(input);
        pool.recycle(unmarshaller);
        return object;
    }

    /**
     * Unmarshal an object from the given URL.
     *
     * @param  input  the URL from which to read a XML representation.
     * @return the object unmarshalled from the given input.
     * @throws JAXBException if an error occurred during the unmarshalling.
     */
    public static Object unmarshal(final URL input) throws JAXBException {
        ensureNonNull("input", input);
        final MarshallerPool pool = getPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final Object object = unmarshaller.unmarshal(input);
        pool.recycle(unmarshaller);
        return object;
    }

    /**
     * Unmarshal an object from the given file.
     *
     * @param  input  the file from which to read a XML representation.
     * @return the object unmarshalled from the given input.
     * @throws JAXBException if an error occurred during the unmarshalling.
     */
    public static Object unmarshal(final File input) throws JAXBException {
        ensureNonNull("input", input);
        final MarshallerPool pool = getPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final Object object = unmarshaller.unmarshal(input);
        pool.recycle(unmarshaller);
        return object;
    }

    /**
     * Unmarshal an object from the given path.
     *
     * @param  input  the path from which to read a XML representation.
     * @return the object unmarshalled from the given input.
     * @throws JAXBException if an error occurred during the unmarshalling.
     */
    public static Object unmarshal(final Path input) throws JAXBException {
        ensureNonNull("input", input);
        final Object object;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(input, StandardOpenOption.READ))) {
            object = unmarshal(URISource.create(in, input.toUri()), null);
        } catch (URISyntaxException | IOException e) {
            throw new JAXBException(Errors.format(Errors.Keys.CanNotRead_1, input), e);
        }
        return object;
    }

    /**
     * Unmarshal an object from the given stream, DOM or other sources.
     * Together with the {@linkplain #unmarshal(Source, Class, Map) Unmarshal by Declared Type} variant,
     * this is the most flexible unmarshalling method provided in this {@code XML} class.
     * The source is specified by the {@code input} argument implementation, for example
     * {@link javax.xml.transform.stream.StreamSource} for reading from a file or input stream.
     * The optional {@code properties} map can contain any key documented in this {@code XML} class,
     * together with the keys documented in the <i>supported properties</i> section of the
     * {@link Unmarshaller} class.
     *
     * @param  input       the file from which to read a XML representation.
     * @param  properties  an optional map of properties to give to the unmarshaller, or {@code null} if none.
     * @return the object unmarshalled from the given input.
     * @throws JAXBException if a property has an illegal value, or if an error occurred during the unmarshalling.
     *
     * @since 0.4
     */
    public static Object unmarshal(final Source input, final Map<String,?> properties) throws JAXBException {
        ensureNonNull("input", input);
        final MarshallerPool pool = getPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller(properties);
        final Object object;
        /*
         * STAX sources are not handled by jakarta.xml.bind.helpers.AbstractUnmarshallerImpl implementation.
         * We have to handle those cases ourselves. This workaround should be removed if a future JDK version handles
         * those cases.
         */
        if (input instanceof StAXSource) {
            @Workaround(library = "JDK", version = "1.8")
            final XMLStreamReader reader = ((StAXSource) input).getXMLStreamReader();
            if (reader != null) {
                object = unmarshaller.unmarshal(reader);
            } else {
                object = unmarshaller.unmarshal(((StAXSource) input).getXMLEventReader());
            }
        } else {
            object = unmarshaller.unmarshal(input);
        }
        pool.recycle(unmarshaller);
        return object;
    }

    /**
     * Unmarshal an object from the given stream, DOM or other sources.
     * Together with the {@linkplain #unmarshal(Source, Map) Unmarshal Global Root Element} variant,
     * this is the most flexible unmarshalling method provided in this {@code XML} class.
     * The source is specified by the {@code input} argument implementation, for example
     * {@link javax.xml.transform.stream.StreamSource} for reading from a file or input stream.
     * The optional {@code properties} map can contain any key documented in this {@code XML} class,
     * together with the keys documented in the <i>supported properties</i> section of the
     * {@link Unmarshaller} class.
     *
     * @param  <T>           compile-time value of the {@code declaredType} argument.
     * @param  input         the file from which to read a XML representation.
     * @param  declaredType  the JAXB mapped class of the object to unmarshal.
     * @param  properties    an optional map of properties to give to the unmarshaller, or {@code null} if none.
     * @return the object unmarshalled from the given input, wrapped in a JAXB element.
     * @throws JAXBException if a property has an illegal value, or if an error occurred during the unmarshalling.
     *
     * @since 0.8
     */
    public static <T> JAXBElement<T> unmarshal(final Source input, final Class<T> declaredType, final Map<String,?> properties)
            throws JAXBException
    {
        ensureNonNull("input", input);
        ensureNonNull("declaredType", declaredType);
        final MarshallerPool pool = getPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller(properties);
        final JAXBElement<T> element;
        if (input instanceof StAXSource) {                  // Same workaround as the one documented in above method.
            @Workaround(library = "JDK", version = "1.8")
            final XMLStreamReader reader = ((StAXSource) input).getXMLStreamReader();
            if (reader != null) {
                element = unmarshaller.unmarshal(reader, declaredType);
            } else {
                element = unmarshaller.unmarshal(((StAXSource) input).getXMLEventReader(), declaredType);
            }
        } else {
            element = unmarshaller.unmarshal(input, declaredType);
        }
        pool.recycle(unmarshaller);
        return element;
    }
}
