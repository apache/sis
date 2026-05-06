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
package org.apache.sis.storage;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.function.Supplier;
import java.time.ZoneId;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.io.ObjectStreamException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.modifier.CoverageModifier;
import org.apache.sis.feature.FoliationRepresentation;
import org.apache.sis.system.Modules;


/**
 * Keys in a map of options for configuring data stores.
 * {@code OptionKey}s are used for aspects that usually do not need to be configured, except in a few specialized cases.
 * For example, most data file formats read by <abbr>SIS</abbr> do not require the user to specify the character encoding,
 * because the encoding is often given in the file header or in the format specification. However, if <abbr>SIS</abbr>
 * needs to read plain text files <em>and</em> the default platform encoding is not suitable,
 * then the user can specify the desired encoding explicitly using the {@link #ENCODING} option.
 *
 * <p>All options are <em>hints</em> and may be silently ignored. For example, most {@link DataStore}s will ignore the
 * {@code ENCODING} option if irrelevant to their format, or if the encoding is specified in the data file header.</p>
 *
 * <p>Options are <em>transitive</em>: if a data store uses others data stores for its internal working,
 * the given options may also be given to those dependencies, at implementation choice.</p>
 *
 * <h2>Defining new options</h2>
 * Developers who wish to define their own options can define static constants in a subclass,
 * as in the following example:
 *
 * {@snippet lang="java" :
 *     public final class MyOptionKey<T> extends OptionKey<T> {
 *         public static final OptionKey<String> MY_OPTION = new MyOptionKey<>("MY_OPTION", String.class);
 *
 *         private MyOptionKey(final String name, final Class<T> type) {
 *             super(name, type);
 *         }
 *     }
 *     }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.7
 *
 * @param <T>  the type of option values.
 *
 * @since 1.7
 */
public class OptionKey<T> extends org.apache.sis.setup.OptionKey<T> {
    /**
     * The locale to use for locale-sensitive data. This option determines the language to use for writing
     * {@link org.opengis.util.InternationalString international strings} when the target storage supports
     * only one language. It may also control number and date patterns in some file formats such as
     * Comma Separated Values (<abbr>CSV</abbr>). However, most data formats will ignore this locale.
     *
     * <p>This option is <strong>not</strong> for the locale of logging or warning messages.
     * Messages locale is rather controlled by {@link DataStore#setLocale(Locale)}.</p>
     *
     * @see org.apache.sis.xml.XML#LOCALE
     * @see DataStore#setLocale(Locale)
     */
    public static final OptionKey<Locale> LOCALE = new OptionKey<>("LOCALE", Locale.class);

    /**
     * The timezone to use when parsing or formatting dates and times without explicit timezone.
     * If this option is not provided, then the default value is format specific.
     * That default is often, but not necessarily, the {@linkplain ZoneId#systemDefault() platform default}.
     *
     * @see org.apache.sis.xml.XML#TIMEZONE
     */
    public static final OptionKey<ZoneId> TIMEZONE = new OptionKey<>("TIMEZONE", ZoneId.class);

    /**
     * The number of spaces to use for indentation when formatting texts in <abbr>WKT</abbr> or <abbr>XML</abbr>.
     * A value of {@value org.apache.sis.io.wkt.WKTFormat#SINGLE_LINE} means to format the whole <abbr>WKT</abbr>
     * or <abbr>XML</abbr> document on a single line without line feeds or indentation.
     *
     * <p>If this option is not provided, then the most typical default value used in Apache <abbr>SIS</abbr> is 2.
     * Such small indentation value is used because <abbr>XML</abbr> documents defined by <abbr>OGC</abbr> standards
     * tend to be verbose.</p>
     *
     * @see org.apache.sis.io.wkt.WKTFormat#SINGLE_LINE
     * @see jakarta.xml.bind.Marshaller#JAXB_FORMATTED_OUTPUT
     */
    public static final OptionKey<Integer> INDENTATION = new OptionKey<>("INDENTATION", Integer.class);

    /**
     * The character encoding of document content.
     * This option can be used when the file to read or write does not describe itself its encoding.
     * For example, this option can be used when reading plain text files, but is ignored when
     * reading <abbr>XML</abbr> files having a {@code <?xml version="1.0" encoding="…"?>} declaration.
     *
     * <p>If this option is not provided, then the default value is format specific.
     * That default is often, but not necessarily, the {@linkplain Charset#defaultCharset() platform default}.</p>
     *
     * @see jakarta.xml.bind.Marshaller#JAXB_ENCODING
     */
    public static final OptionKey<Charset> ENCODING = new OptionKey<>("ENCODING", Charset.class);

    /**
     * The encoding of a <abbr>URL</abbr> (<em>not</em> the encoding of the document content).
     * This option may be used when converting a {@link String} or a {@link java.net.URL}
     * to a {@link java.net.URI} or a {@link java.io.File}. The following rules apply:
     *
     * <ul>
     *   <li><abbr>URI</abbr> are always encoded in <abbr>UTF</abbr>-8.
     *       Therefore, this option is ignored for <abbr>URI</abbr>.</li>
     *   <li><abbr>URL</abbr> are often encoded in <abbr>UTF</abbr>-8, but not necessarily.
     *       Other encoding are possible (while not recommended), or some <abbr>URL</abbr> may not be encoded at all.</li>
     * </ul>
     *
     * If this option is not provided, then the <abbr>URL</abbr> is assumed <em>not</em> encoded.
     *
     * <p><b>Example:</b> Given the {@code "file:Map%20with%20spaces.png"} URL, then:</p>
     * <ul>
     *   <li>If the <abbr>URL</abbr> encoding option is set to {@code "UTF-8"} or {@code "ISO-8859-1"}, then:<ul>
     *     <li>the encoded <abbr>URI</abbr> will be {@code "file:Map%20with%20spaces.png"};</li>
     *     <li>the decoded <abbr>URI</abbr> or the file will be {@code "file:Map with spaces.png"}.</li>
     *   </ul></li>
     *   <li>If the <abbr>URL</abbr> encoding option is set to {@code null} or is not provided, then:<ul>
     *     <li>the encoded <abbr>URI</abbr> will be {@code "file:Map%2520with%2520spaces.png"},
     *         i.e. the percent sign will be encoded as {@code "%25"};</li>
     *     <li>the decoded <abbr>URI</abbr> or the file will be {@code "file:Map%20with%20spaces.png"}.</li>
     *   </ul></li>
     * </ul>
     *
     * @see java.net.URLDecoder
     */
    public static final OptionKey<String> URL_ENCODING = new OptionKey<>("URL_ENCODING", String.class);

    /**
     * Whether a storage object shall be opened in read, write, append or other modes.
     * The main options that can be provided are:
     *
     * <table class="sis">
     *   <caption>Supported open options</caption>
     *   <tr><th>Value</th>                             <th>Meaning</th></tr>
     *   <tr><td>{@link StandardOpenOption#READ}</td>   <td>Open for reading data from the storage object.</td></tr>
     *   <tr><td>{@link StandardOpenOption#WRITE}</td>  <td>Open for modifying existing data in the storage object.</td></tr>
     *   <tr><td>{@link StandardOpenOption#APPEND}</td> <td>Open for appending new data in the storage object.</td></tr>
     *   <tr><td>{@link StandardOpenOption#CREATE}</td> <td>Creates a new storage object (file or database) if it does not exist.</td></tr>
     * </table>
     */
    public static final OptionKey<OpenOption[]> OPEN_OPTIONS = new OptionKey<>("OPEN_OPTIONS", OpenOption[].class);

    /**
     * Provider of the byte buffer to use for input/output operations. Some {@link DataStore} implementations allow
     * a buffer to be specified, thus allowing users to choose the buffer {@linkplain ByteBuffer#capacity() capacity},
     * whether the buffer {@linkplain ByteBuffer#isDirect() is direct}, or to recycle existing buffers.
     *
     * <p>It is user's responsibility to ensure that:</p>
     * <ul>
     *   <li>The buffer does not contains any valuable data, as it will be {@linkplain ByteBuffer#clear() cleared}.</li>
     *   <li>The same buffer is not used concurrently by two different {@code DataStore} instances.</li>
     * </ul>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final OptionKey<Supplier<ByteBuffer>> BYTE_BUFFER = new OptionKey("BYTE_BUFFER", Supplier.class);

    /**
     * Path to an auxiliary file containing metadata encoded in an <abbr>ISO</abbr> 19115-3 <abbr>XML</abbr> document.
     * The given path, if not absolute, is relative to the path of the main storage file.
     * If the file exists, it is parsed and its content is merged or appended after the
     * metadata read by the storage. If the file does not exist, then it is ignored.
     *
     * <h4>Wildcard</h4>
     * It the {@code '*'} character is present in the path, then it is replaced by the name of the
     * main file without its extension. For example if the main file is {@code "city-center.tiff"},
     * then {@code "*.xml"} will become {@code "city-center.xml"}.
     */
    public static final OptionKey<Path> METADATA_PATH = new OptionKey<>("METADATA_PATH", Path.class);

    /**
     * The coordinate reference system (<abbr>CRS</abbr>) of data to use if not explicitly defined.
     * This option can be used when the file to read does not describe itself the data <abbr>CRS</abbr>.
     * For example, this option can be used when reading <abbr>ASCII</abbr> Grid without <abbr>CRS</abbr>
     * information, but is ignored if the <abbr>ASCII</abbr> Grid file is accompanied by a {@code *.prj}
     * file giving the <abbr>CRS</abbr>.
     */
    public static final OptionKey<CoordinateReferenceSystem> DEFAULT_CRS =
            new OptionKey<>("DEFAULT_CRS", CoordinateReferenceSystem.class);

    /**
     * Whether to assemble trajectory fragments (distinct <abbr>CSV</abbr> lines) into a single {@code Feature}
     * instance forming a foliation. This is ignored if the file does not seem to contain moving features.
     */
    public static final OptionKey<FoliationRepresentation> FOLIATION_REPRESENTATION =
            new OptionKey<>("FOLIATION_REPRESENTATION", FoliationRepresentation.class);

    /**
     * The library to use for creating geometric objects at reading time. Some available libraries are
     * the Java Topology Suite (<abbr>JTS</abbr>), <abbr>ESRI</abbr> geometry <abbr>API</abbr> and Java2D.
     * If this option is not specified, then a default library will be selected among the libraries available
     * in the runtime environment.
     */
    public static final OptionKey<GeometryLibrary> GEOMETRY_LIBRARY =
            new OptionKey<>("GEOMETRY_LIBRARY", GeometryLibrary.class);

    /**
     * Callback methods invoked for modifying some aspects of the grid coverages created by resources.
     */
    public static final OptionKey<CoverageModifier> COVERAGE_MODIFIER =
            new OptionKey<>("COVERAGE_MODIFIER", CoverageModifier.class);

    /**
     * The listeners to declare as the parent of the data store listeners.
     * This option can be used when the {@link DataStore} to open is itself
     * a child of an {@link Aggregate}.
     */
    public static final OptionKey<StoreListeners> PARENT_LISTENERS =
            new OptionKey<>("PARENT_LISTENERS", StoreListeners.class);

    // Temporary hack for transition from deprecated class.
    static {
        org.apache.sis.setup.OptionKey.LOCALE       = LOCALE;
        org.apache.sis.setup.OptionKey.TIMEZONE     = TIMEZONE;
        org.apache.sis.setup.OptionKey.ENCODING     = ENCODING;
        org.apache.sis.setup.OptionKey.URL_ENCODING = URL_ENCODING;
        org.apache.sis.setup.OptionKey.OPEN_OPTIONS = OPEN_OPTIONS;
        org.apache.sis.setup.OptionKey.DEFAULT_CRS  = DEFAULT_CRS;
        org.apache.sis.setup.OptionKey.INDENTATION  = INDENTATION;
        org.apache.sis.setup.OptionKey.GEOMETRY_LIBRARY = GEOMETRY_LIBRARY;
    }

    /*
     * Note: we do not provide a LINE_SEPARATOR option for now because we cannot control the line separator
     * in JDK's JAXB implementation, and Apache SIS provides an org.apache.sis.io.LineAppender alternative.
     */

    /**
     * Creates a new key of the given name for values of the given type.
     *
     * @param name  the key name.
     * @param type  the type of values.
     */
    protected OptionKey(final String name, final Class<T> type) {
        super(name, type);
    }

    /**
     * Resolves this option key on deserialization. This method is invoked
     * only for instance of the exact {@code OptionKey} class, not subclasses.
     *
     * @return the unique {@code OptionKey} instance.
     * @throws ObjectStreamException required by specification but should never be thrown.
     */
    private Object readResolve() throws ObjectStreamException {
        try {
            return OptionKey.class.getField(super.getName()).get(null);
        } catch (ReflectiveOperationException e) {
            /*
             * This may happen if we are deserializing a stream produced by a more recent SIS library
             * than the one running in this JVM. This class should be robust to this situation, since
             * we override the `equals` and `hashCode` methods. This option is likely to be ignored,
             * but options are expected to be optional.
             */
            Logging.recoverableException(Logger.getLogger(Modules.STORAGE), OptionKey.class, "readResolve", e);
            return this;
        }
    }
}
