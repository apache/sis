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
package org.apache.sis.setup;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.time.ZoneId;
import java.nio.ByteBuffer;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import static java.util.logging.Logger.getLogger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Modules;


/**
 * Keys in a map of options for configuring various services
 * ({@link org.apache.sis.storage.DataStore}, <i>etc</i>).
 * {@code OptionKey}s are used for aspects that usually do not need to be configured, except in a few specialized cases.
 * For example, most data file formats read by SIS do not require the user to specify the character encoding, because
 * the encoding is often given in the file header or in the format specification. However if SIS needs to read plain
 * text files <em>and</em> the default platform encoding is not suitable, then the user can specify the desired encoding
 * explicitly using the {@link #ENCODING} option.
 *
 * <p>All options are <em>hints</em> and may be silently ignored. For example, most {@code DataStore}s will ignore the
 * {@code ENCODING} option if irrelevant to their format, or if the encoding is specified in the data file header.</p>
 *
 * <p>Options are <em>transitive</em>: if a service uses others services for its internal working, the given options
 * may also be given to those dependencies, at implementation choice.</p>
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
 * @version 1.5
 *
 * @param <T>  the type of option values.
 *
 * @since 0.3
 */
public class OptionKey<T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7580514229639750246L;

    /**
     * The locale to use for locale-sensitive data. This option determines the language to use for writing
     * {@link org.apache.sis.util.AbstractInternationalString international strings} when the target
     * storage support only one language. It may also control number and date patterns in some file formats
     * like Comma Separated Values (CSV). However, most data formats will ignore this locale.
     *
     * <p>This option is <strong>not</strong> for the locale of logging or warning messages. Messages
     * locale is rather controlled by {@link org.apache.sis.storage.DataStore#setLocale(Locale)}.</p>
     *
     * @see org.apache.sis.xml.XML#LOCALE
     *
     * @since 0.8
     */
    public static final OptionKey<Locale> LOCALE = new OptionKey<>("LOCALE", Locale.class);

    /**
     * The timezone to use when parsing or formatting dates and times without explicit timezone.
     * If this option is not provided, then the default value is format specific.
     * That default is often, but not necessarily, the {@linkplain ZoneId#systemDefault() platform default}.
     *
     * @see org.apache.sis.xml.XML#TIMEZONE
     *
     * @since 0.8
     */
    public static final OptionKey<ZoneId> TIMEZONE = new OptionKey<>("TIMEZONE", ZoneId.class);

    /**
     * The character encoding of document content.
     * This option can be used when the file to read or write does not describe itself its encoding.
     * For example, this option can be used when reading plain text files, but is ignored when
     * reading XML files having a {@code <?xml version="1.0" encoding="…"?>} declaration.
     *
     * <p>If this option is not provided, then the default value is format specific.
     * That default is often, but not necessarily, the {@linkplain Charset#defaultCharset() platform default}.</p>
     *
     * @see jakarta.xml.bind.Marshaller#JAXB_ENCODING
     *
     * @since 0.4
     */
    public static final OptionKey<Charset> ENCODING = new OptionKey<>("ENCODING", Charset.class);

    /**
     * The encoding of a URL (<strong>not</strong> the encoding of the document content).
     * This option may be used when converting a {@link String} or a {@link java.net.URL}
     * to a {@link java.net.URI} or a {@link java.io.File}. The following rules apply:
     *
     * <ul>
     *   <li>URI are always encoded in UTF-8. Consequently, this option is ignored for URI.</li>
     *   <li>URL are often encoded in UTF-8, but not necessarily. Other encodings are possible
     *       (while not recommended), or some URL may not be encoded at all.</li>
     * </ul>
     *
     * If this option is not provided, then the URL is assumed <strong>not</strong> encoded.
     *
     * <p><b>Example:</b> Given the {@code "file:Map%20with%20spaces.png"} URL, then:</p>
     * <ul>
     *   <li>If the URL encoding option is set to {@code "UTF-8"} or {@code "ISO-8859-1"}, then:<ul>
     *     <li>the encoded URI will be {@code "file:Map%20with%20spaces.png"};</li>
     *     <li>the decoded URI or the file will be {@code "file:Map with spaces.png"}.</li>
     *   </ul></li>
     *   <li>If the URL encoding option is set to {@code null} or is not provided, then:<ul>
     *     <li>the encoded URI will be {@code "file:Map%2520with%2520spaces.png"},
     *         i.e. the percent sign will be encoded as {@code "%25"};</li>
     *     <li>the decoded URI or the file will be {@code "file:Map%20with%20spaces.png"}.</li>
     *   </ul></li>
     * </ul>
     *
     * This option has not effect on URI encoding, which is always UTF-8.
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
     * The byte buffer to use for input/output operations. Some {@link org.apache.sis.storage.DataStore}
     * implementations allow a byte buffer to be specified, thus allowing users to choose the buffer
     * {@linkplain ByteBuffer#capacity() capacity}, whether the buffer {@linkplain ByteBuffer#isDirect()
     * is direct}, or to recycle existing buffers.
     *
     * <p>It is user's responsibility to ensure that:</p>
     * <ul>
     *   <li>The buffer does not contains any valuable data, as it will be {@linkplain ByteBuffer#clear() cleared}.</li>
     *   <li>The same buffer is not used concurrently by two different {@code DataStore} instances.</li>
     * </ul>
     *
     * @deprecated This option forces unconditional allocation of byte buffer, even if the data store does not use it.
     * It should be replaced by a {@link java.util.function.Supplier} or {@link java.util.function.Function}, but the
     * exact form has not been determined yet.
     */
    @Deprecated(since="1.5")
    // TODO: provide replacement in DataOptionKey, because this option is specific to data stores.
    public static final OptionKey<ByteBuffer> BYTE_BUFFER = new OptionKey<>("BYTE_BUFFER", ByteBuffer.class);

    /**
     * The coordinate reference system (CRS) of data to use if not explicitly defined.
     * This option can be used when the file to read does not describe itself the data CRS.
     * For example, this option can be used when reading ASCII Grid without CRS information,
     * but is ignored if the ASCII Grid file is accompanied by a {@code *.prj} file giving the CRS.
     *
     * @since 1.5
     */
    public static final OptionKey<CoordinateReferenceSystem> DEFAULT_CRS =
            new OptionKey<>("DEFAULT_CRS", CoordinateReferenceSystem.class);

    /**
     * The library to use for creating geometric objects at reading time.
     * Some libraries are the Java Topology Suite (JTS), ESRI geometry API and Java2D.
     * If this option is not specified, then a default library will be selected among
     * the libraries available in the runtime environment.
     *
     * @since 0.8
     */
    public static final OptionKey<GeometryLibrary> GEOMETRY_LIBRARY = new OptionKey<>("GEOMETRY_LIBRARY", GeometryLibrary.class);

    /**
     * The number of spaces to use for indentation when formatting text files in WKT or XML formats.
     * A value of {@value org.apache.sis.io.wkt.WKTFormat#SINGLE_LINE} means to format the whole WKT
     * or XML document on a single line without line feeds or indentation.
     *
     * <p>If this option is not provided, then the most typical default value used in Apache SIS is 2.
     * Such small indentation value is used because XML documents defined by OGC standards tend to be
     * verbose.</p>
     *
     * @see org.apache.sis.io.wkt.WKTFormat#SINGLE_LINE
     * @see jakarta.xml.bind.Marshaller#JAXB_FORMATTED_OUTPUT
     *
     * @since 0.8
     */
    public static final OptionKey<Integer> INDENTATION = new OptionKey<>("INDENTATION", Integer.class);

    /*
     * Note: we do not provide a LINE_SEPARATOR option for now because we cannot control the line separator
     * in JDK's JAXB implementation, and Apache SIS provides an org.apache.sis.io.LineAppender alternative.
     */

    /**
     * The name of this key. For {@code OptionKey} instances, it shall be the name of the static constants.
     * For subclasses of {@code OptionKey}, there is no restriction.
     */
    private final String name;

    /**
     * The type of values.
     */
    private final Class<T> type;

    /**
     * Creates a new key of the given name for values of the given type.
     *
     * @param name  the key name.
     * @param type  the type of values.
     */
    protected OptionKey(final String name, final Class<T> type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Returns the name of this option key.
     *
     * @return the name of this option key.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of values associated to this option key.
     *
     * @return the type of values.
     */
    public final Class<T> getElementType() {
        return type;
    }

    /**
     * Returns the option value in the given map for this key, or {@code null} if none.
     * This is a convenience method for implementers, which can be used as below:
     *
     * {@snippet lang="java" :
     *     public <T> T getOption(final OptionKey<T> key) {
     *         return key.getValueFrom(options);
     *     }
     *     }
     *
     * @param  options  the map where to search for the value, or {@code null} if not yet created.
     * @return the current value in the map for the this option, or {@code null} if none.
     */
    public T getValueFrom(final Map<OptionKey<?>,?> options) {
        return (options != null) ? type.cast(options.get(this)) : null;
    }

    /**
     * Sets a value for this option key in the given map, or in a new map if the given map is {@code null}.
     * This is a convenience method for implementers, which can be used as below:
     *
     * {@snippet lang="java" :
     *     public <T> void setOption(final OptionKey<T> key, final T value) {
     *         options = key.setValueInto(options, value);
     *     }
     *     }
     *
     * @param  options  the map where to set the value, or {@code null} if not yet created.
     * @param  value    the new value for the given option, or {@code null} for removing the value.
     * @return the given map of options, or a new map if the given map was null. The returned value
     *         may be null if the given map and the given value are both null.
     */
    public Map<OptionKey<?>,Object> setValueInto(Map<OptionKey<?>,Object> options, final T value) {
        if (value != null) {
            if (options == null) {
                options = new HashMap<>();
            }
            options.put(this, value);
        } else if (options != null) {
            options.remove(this);
        }
        return options;
    }

    /**
     * Returns {@code true} if the given object is an instance of the same class having the same name and type.
     *
     * @param object  the object to compare with this {@code OptionKey} for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            final OptionKey<?> that = (OptionKey<?>) object;
            return name.equals(that.name) && type == that.type;
        }
        return false;
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return name.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this option key.
     * The default implementation returns the value of {@link #getName()}.
     */
    @Override
    public String toString() {
        return getName();
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
            return OptionKey.class.getField(name).get(null);
        } catch (ReflectiveOperationException e) {
            /*
             * This may happen if we are deserializing a stream produced by a more recent SIS library
             * than the one running in this JVM. This class should be robust to this situation, since
             * we override the `equals` and `hashCode` methods. This option is likely to be ignored,
             * but options are expected to be optional.
             */
            Logging.recoverableException(getLogger(Modules.UTILITIES), OptionKey.class, "readResolve", e);
            return this;
        }
    }
}
