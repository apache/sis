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

import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.LogRecord; // For javadoc
import java.net.URL;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import org.apache.sis.util.Static;
import org.apache.sis.util.Version;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.jaxb.TypeRegistration;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Provides convenience methods for marshalling and unmarshalling SIS objects.
 * This class defines also some property keys that can be given to the {@link Marshaller}
 * and {@link Unmarshaller} instances created by {@link PooledMarshaller}:
 *
 * <table class="sis">
 *   <caption>Supported (un)marshaller properties</caption>
 *   <tr><th>Key</th>                         <th>Value type</th>                <th>Purpose</th></tr>
 *   <tr><td>{@link #LOCALE}</td>             <td>{@link Locale}</td>            <td>for specifying the locale to use for international strings and code lists.</td></tr>
 *   <tr><td>{@link #TIMEZONE}</td>           <td>{@link TimeZone}</td>          <td>for specifying the timezone to use for dates and times.</td></tr>
 *   <tr><td>{@link #SCHEMAS}</td>            <td>{@link Map}</td>               <td>for specifying the root URL of metadata schemas to use.</td></tr>
 *   <tr><td>{@link #DEFAULT_NAMESPACE}</td>  <td>{@link String}</td>            <td>for specifying the default namespace of the XML document to write.</td></tr>
 *   <tr><td>{@link #GML_VERSION}</td>        <td>{@link Version}</td>           <td>for specifying the GML version to the document be (un)marshalled.</td></tr>
 *   <tr><td>{@link #RESOLVER}</td>           <td>{@link ReferenceResolver}</td> <td>for replacing {@code xlink} or {@code uuidref} attributes by the actual object to use.</td></tr>
 *   <tr><td>{@link #CONVERTER}</td>          <td>{@link ValueConverter}</td>    <td>for controlling the conversion of URL, UUID, Units or similar objects.</td></tr>
 *   <tr><td>{@link #STRING_SUBSTITUTES}</td> <td>{@code String[]}</td>          <td>for specifying which code lists to replace by simpler {@code <gco:CharacterString>} elements.</td></tr>
 *   <tr><td>{@link #WARNING_LISTENER}</td>   <td>{@link WarningListener}</td>   <td>for being notified about non-fatal warnings.</td></tr>
 * </table>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final class XML extends Static {
    /**
     * Specifies the locale to use for marshalling
     * {@link org.opengis.util.InternationalString} and {@link org.opengis.util.CodeList}
     * instances. The value for this property shall be an instance of {@link Locale} or a
     * {@link CharSequence} recognized by {@link org.apache.sis.util.Locales#parse(String)}.
     *
     * <p>This property is mostly for marshallers. However this property can also be used at
     * unmarshalling time, for example if a {@code <gmd:PT_FreeText>} element containing
     * many localized strings need to be represented in a Java {@link String} object. In
     * such case, the unmarshaller will try to pickup a string in the language specified
     * by this property.</p>
     *
     * <div class="section">Default behavior</div>
     * If this property is never set, then (un)marshalling will try to use "unlocalized" strings -
     * typically some programmatic strings like {@linkplain org.opengis.annotation.UML#identifier()
     * UML identifiers}. While such identifiers often look like English words, they are not
     * considered as the {@linkplain Locale#ENGLISH English} localization.
     * The algorithm attempting to find a "unlocalized" string is defined in the
     * {@link org.apache.sis.util.iso.DefaultInternationalString#toString(Locale)} javadoc.
     *
     * <div class="section">Special case</div>
     * If the object to be marshalled is an instance of
     * {@link org.apache.sis.metadata.iso.DefaultMetadata}, then the value given to its
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#setLanguage(Locale) setLanguage(Locale)}
     * method will have precedence over this property. This behavior is compliant with INSPIRE rules.
     *
     * @see Marshaller#setProperty(String, Object)
     * @see org.apache.sis.metadata.iso.DefaultMetadata#setLanguage(Locale)
     */
    public static final String LOCALE = "org.apache.sis.xml.locale";

    /**
     * Specifies the timezone to use for marshalling dates and times.
     * The value for this property shall be an instance of {@link TimeZone}
     * or a {@link CharSequence} recognized by {@link TimeZone#getTimeZone(String)}.
     *
     * <div class="section">Default behavior</div>
     * If this property is never set, then (un)marshalling will use the
     * {@linkplain TimeZone#getDefault() default timezone}.
     */
    public static final String TIMEZONE = "org.apache.sis.xml.timezone";

    /**
     * Specifies the root URL of schemas. The value for this property shall
     * be an instance of {@link Map Map&lt;String,String&gt;}.
     * This property controls the URL to be used when marshalling the following elements:
     *
     * <ul>
     *   <li>The value of the {@code codeList} attribute when marshalling subclasses of
     *       {@link org.opengis.util.CodeList} in ISO 19139 compliant XML document.</li>
     *   <li>The value of the {@code uom} attribute when marshalling measures (for example
     *       {@code <gco:Distance>}) in ISO 19139 compliant XML document.</li>
     * </ul>
     *
     * As of SIS 0.3, only one {@code Map} key is recognized: {@code "gmd"}, which stands
     * for the ISO 19139 schemas. Additional keys, if any, are ignored. Future SIS versions
     * may recognize more keys.
     *
     * <div class="section">Valid values</div>
     * <table class="sis">
     *   <caption>Supported schemas</caption>
     *   <tr><th>Map key</th> <th>Typical values (choose only one)</th></tr>
     *   <tr><td><b>gmd</b></td><td>
     *     http://schemas.opengis.net/iso/19139/20070417/<br>
     *     http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/<br>
     *     http://eden.ign.fr/xsd/fra/20060922/
     *   </td></tr>
     * </table>
     */
    public static final String SCHEMAS = "org.apache.sis.xml.schemas";
    // If more keys are documented, update the Pooled.SCHEMAS_KEY array.

    /**
     * Specifies the default namespace of the XML document to write.
     * An example of value for this key is {@code "http://www.isotc211.org/2005/gmd"}.
     *
     * <div class="section">Current limitation</div>
     * In current SIS implementation, this property is honored only by the {@link MarshallerPool} constructors.
     * Specifying this property to {@link javax.xml.bind.Marshaller#setProperty(String, Object)} is too late.
     * This limitation may be fixed in a future SIS version.
     */
    public static final String DEFAULT_NAMESPACE = "org.apache.sis.xml.defaultNamespace";

    /**
     * Specifies the GML version of the document to be marshalled or unmarshalled.
     * The GML version may affect the set of XML elements to be marshalled and their namespaces.
     *
     * <div class="note"><b>Compatibility note:</b>
     * Newer versions typically have more elements, but not always. For example in {@code <gml:VerticalDatum>},
     * the {@code <gml:verticalDatumType>} property presents in GML 3.0 and 3.1 has been removed in GML 3.2.</div>
     *
     * The value can be {@link String} or {@link Version} objects.
     * If no version is specified, then the most recent GML version is assumed.
     *
     * <div class="section">Supported GML versions</div>
     * Apache SIS currently supports GML 3.2.1 by default. SIS can read and write GML 3.2
     * if this property is set to "3.2". It is also possible to set this property to "3.1",
     * but the marshalled XML is not GML 3.1.1 conformant because of the differences between the two schemas.
     * See <a href="http://issues.apache.org/jira/browse/SIS-160">SIS-160: Need XSLT between GML 3.1 and 3.2</a>
     * for information about the status of GML 3.1.1 support.
     */
    public static final String GML_VERSION = "org.apache.sis.gml.version";

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
     *   <li>Otherwise an empty element containing only the values of the above-cited attributes is created.</li>
     * </ul>
     *
     * Applications can sometime do better by using some domain-specific knowledge, for example by searching in a
     * database. Users can define their search algorithm by subclassing {@link ReferenceResolver} and configuring
     * a unmarshaller as below:
     *
     * {@preformat java
     *     ReferenceResolver  myResolver = ...;
     *     Map<String,Object> properties = new HashMap<>();
     *     properties.put(XML.RESOLVER, myResolver);
     *     Object obj = XML.unmarshal(source, properties);
     * }
     *
     * @see Unmarshaller#setProperty(String, Object)
     * @see ReferenceResolver
     */
    public static final String RESOLVER = "org.apache.sis.xml.resolver";

    /**
     * Controls the behaviors of the (un)marshalling process when an element can not be processed,
     * or alter the element values. The value for this property shall be an instance of {@link ValueConverter}.
     *
     * <p>If an element in a XML document can not be parsed (for example if a {@linkplain java.net.URL}
     * string is not valid), the default behavior is to throw an exception which cause the
     * (un)marshalling of the entire document to fail. This default behavior can be customized by
     * invoking {@link Marshaller#setProperty(String, Object)} with this {@code CONVERTER} property
     * key and a custom {@link ValueConverter} instance. {@code ValueConverter} can also be used
     * for replacing an erroneous URL by a fixed URL. See the {@link ValueConverter} javadoc for
     * more details.</p>
     *
     * <div class="note"><b>Example:</b>
     * the following example collects the failures in a list without stopping the (un)marshalling process.
     *
     * {@preformat java
     *     class WarningCollector extends ValueConverter {
     *         // The warnings collected during (un)marshalling.
     *         List<String> messages = new ArrayList<String>();
     *
     *         // Override the default implementation in order to
     *         // collect the warnings and allow the process to continue.
     *         &#64;Override
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
     * </div>
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
     *   <li>"{@code language}" for substituting {@code <gmd:LanguageCode>} elements</li>
     *   <li>"{@code country}"  for substituting {@code <gmd:Country>} elements</li>
     *   <li>"{@code filename}" for substituting {@code <gmx:FileName>} elements</li>
     *   <li>"{@code mimetype}" for substituting {@code <gmx:MimeFileType>} elements</li>
     * </ul>
     *
     * <div class="note"><b>Example:</b>
     * INSPIRE compliant language code shall be formatted like below (details may vary):
     *
     * {@preformat xml
     *   <gmd:language>
     *     <gmd:LanguageCode
     *         codeList="http://schemas.opengis.net/iso/19139/20070417/resources/Codelist/gmxCodelists.xml#LanguageCode"
     *         codeListValue="fra">French</gmd:LanguageCode>
     *   </gmd:language>
     * }
     *
     * However if this property contains the "{@code language}" value, then the marshaller will format
     * the language code like below (which is legal according OGC schemas, but is not INSPIRE compliant):
     *
     * {@preformat xml
     *   <gmd:language>
     *     <gco:CharacterString>fra</gco:CharacterString>
     *   </gmd:language>
     * }
     * </div>
     */
    public static final String STRING_SUBSTITUTES = "org.apache.sis.xml.stringSubstitutes";

    /**
     * Specifies a listener to be notified when a non-fatal error occurred during the (un)marshalling.
     * The value for this property shall be an instance of {@code WarningListener<Object>}.
     *
     * <p>By default, warnings that occur during the (un)marshalling process are logged. However if a
     * property is set for this key, then the {@link WarningListener#warningOccured(Object, LogRecord)}
     * method will be invoked and the warning will <em>not</em> be logged by the (un)marshaller.</p>
     *
     * @see WarningListener
     */
    public static final String WARNING_LISTENER = "org.apache.sis.xml.warningListener";

    /**
     * The pool of marshallers and unmarshallers used by this class.
     * The field name uses the uppercase convention because this field is almost constant:
     * this field is initially null, then created by {@link #getPool()} when first needed.
     * Once created the field value usually doesn't change. However the field may be reset
     * to {@code null} in an OSGi context when modules are loaded or unloaded, because the
     * set of classes returned by {@link TypeRegistration#defaultClassesToBeBound()} may
     * have changed.
     *
     * @see #getPool()
     */
    private static volatile MarshallerPool POOL;

    /**
     * Registers a listener for classpath changes. In such case, a new pool will need to
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
     * <div class="note"><b>Implementation note:</b>
     * Current implementation uses the double-check idiom. This is usually a deprecated practice
     * (the recommended alterative is to use static class initialization), but in this particular
     * case the field may be reset to {@code null} if OSGi modules are loaded or unloaded, so static
     * class initialization would be a little bit too rigid.</div>
     */
    @SuppressWarnings("DoubleCheckedLocking")
    private static MarshallerPool getPool() throws JAXBException {
        MarshallerPool pool = POOL;
        if (pool == null) {
            synchronized (XML.class) {
                pool = POOL; // Double-check idiom: see javadoc.
                if (pool == null) {
                    POOL = pool = new MarshallerPool(null);
                }
            }
        }
        return pool;
    }

    /**
     * Marshall the given object into a string.
     *
     * @param  object The root of content tree to be marshalled.
     * @return The XML representation of the given object.
     * @throws JAXBException If an error occurred during the marshalling.
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
     * Marshall the given object into a stream.
     *
     * @param  object The root of content tree to be marshalled.
     * @param  output The stream where to write.
     * @throws JAXBException If an error occurred during the marshalling.
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
     * Marshall the given object into a file.
     *
     * @param  object The root of content tree to be marshalled.
     * @param  output The file to be written.
     * @throws JAXBException If an error occurred during the marshalling.
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
     * Marshall the given object to a stream, DOM or other destinations.
     * This is the most flexible marshalling method provided in this {@code XML} class.
     * The destination is specified by the {@code output} argument implementation, for example
     * {@link javax.xml.transform.stream.StreamResult} for writing to a file or output stream.
     * The optional {@code properties} map can contain any key documented in this {@code XML} class,
     * together with the keys documented in the <cite>supported properties</cite> section of the the
     * {@link Marshaller} class.
     *
     * @param  object The root of content tree to be marshalled.
     * @param  output The file to be written.
     * @param  properties An optional map of properties to give to the marshaller, or {@code null} if none.
     * @throws JAXBException If a property has an illegal value, or if an error occurred during the marshalling.
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
        marshaller.marshal(object, output);
        pool.recycle(marshaller);
    }

    /**
     * Unmarshall an object from the given string.
     * Note that the given argument is the XML document itself,
     * <strong>not</strong> a URL to a XML document.
     *
     * @param  xml The XML representation of an object.
     * @return The object unmarshalled from the given input.
     * @throws JAXBException If an error occurred during the unmarshalling.
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
     * Unmarshall an object from the given stream.
     *
     * @param  input The stream from which to read a XML representation.
     * @return The object unmarshalled from the given input.
     * @throws JAXBException If an error occurred during the unmarshalling.
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
     * Unmarshall an object from the given URL.
     *
     * @param  input The URL from which to read a XML representation.
     * @return The object unmarshalled from the given input.
     * @throws JAXBException If an error occurred during the unmarshalling.
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
     * Unmarshall an object from the given file.
     *
     * @param  input The file from which to read a XML representation.
     * @return The object unmarshalled from the given input.
     * @throws JAXBException If an error occurred during the unmarshalling.
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
     * Unmarshall an object from the given stream, DOM or other sources.
     * This is the most flexible unmarshalling method provided in this {@code XML} class.
     * The source is specified by the {@code input} argument implementation, for example
     * {@link javax.xml.transform.stream.StreamSource} for reading from a file or input stream.
     * The optional {@code properties} map can contain any key documented in this {@code XML} class,
     * together with the keys documented in the <cite>supported properties</cite> section of the the
     * {@link Unmarshaller} class.
     *
     * @param  input The file from which to read a XML representation.
     * @param  properties An optional map of properties to give to the unmarshaller, or {@code null} if none.
     * @return The object unmarshalled from the given input.
     * @throws JAXBException If a property has an illegal value, or if an error occurred during the unmarshalling.
     *
     * @since 0.4
     */
    public static Object unmarshal(final Source input, final Map<String,?> properties) throws JAXBException {
        ensureNonNull("input", input);
        final MarshallerPool pool = getPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        if (properties != null) {
            for (final Map.Entry<String,?> entry : properties.entrySet()) {
                unmarshaller.setProperty(entry.getKey(), entry.getValue());
            }
        }
        final Object object = unmarshaller.unmarshal(input);
        pool.recycle(unmarshaller);
        return object;
    }
}
