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
import org.opengis.util.InternationalString;
import org.apache.sis.util.LocalizedException;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.util.Workaround;


/**
 * Thrown when a {@link DataStore} can not complete a read or write operation.
 *
 * <h2>Localization</h2>
 * The {@link #getMessage()} and {@link #getLocalizedMessage()} methods return the same message,
 * but sometime in different languages. The general policy is that {@link #getMessage()} returns
 * the message in the JVM {@linkplain Locale#getDefault() default locale} while {@link #getLocalizedMessage()}
 * returns the message in the locale specified by the last call to {@link DataStore#setLocale(Locale)}.
 * In a client-server architecture, the former is typically the locale of the system administrator
 * while the latter is presumably the locale of the client connected to the server.
 * However this policy is applied on a <em>best-effort</em> basis only.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
public class DataStoreException extends Exception implements LocalizedException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1778987176103191950L;

    /**
     * The resources key as one of the {@link Resources.Keys} constant, or 0 if none.
     *
     * <p>This field is not serialized because key values sometime change between different SIS versions.
     * The deserialized value will be 0, which will cause this {@code DataStoreException} to fallback on
     * {@code super.getMessage()}.</p>
     */
    private transient short key;

    /**
     * The parameters for the localization message, or {@code null} if none.
     */
    private transient Object[] parameters;

    /**
     * Creates an exception with no cause and no details message.
     */
    public DataStoreException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message in the default locale.
     */
    public DataStoreException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause and no details message.
     *
     * @param cause  the cause for this exception.
     */
    public DataStoreException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message in the default locale.
     * @param cause    the cause for this exception.
     */
    public DataStoreException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a localized exception with a message saying that the given store can not be processed.
     * Location in the file where the error occurred while be fetched from the given {@code store}
     * argument if possible, for example by invoking the {@link java.io.LineNumberReader#getLineNumber()}
     * or {@link javax.xml.stream.XMLStreamReader#getLocation()} method.
     * If the given {@code store} argument is not one of the recognized types, then it is ignored.
     *
     * <p>Examples of messages created by this constructor:</p>
     * <ul>
     *   <li>Can not read <var>“Foo”</var> as a file in the <var>Bar</var> format.</li>
     *   <li>Can not read after column 10 or line 100 of <var>“Foo”</var> as part of a file in the <var>Bar</var> format.</li>
     * </ul>
     *
     * @param locale    the locale of the message to be returned by {@link #getLocalizedMessage()}, or {@code null}.
     * @param format    short name or abbreviation of the data format (e.g. "CSV", "GML", "WKT", <i>etc</i>).
     * @param filename  name of the file or data store where the error occurred.
     * @param store     the input or output object from which to get the current position, or {@code null} if none.
     *                  This can be a {@link java.io.LineNumberReader} or {@link javax.xml.stream.XMLStreamReader}
     *                  for example.
     *
     * @since 0.8
     */
    public DataStoreException(final Locale locale, final String format, final String filename, final Object store) {
        this(locale, IOUtilities.errorMessageParameters(format, filename, store));
    }

    /**
     * Workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private DataStoreException(final Locale locale, final Object[] params) {
        this(locale, IOUtilities.errorMessageKey(params), params);
    }

    /**
     * Creates a new exception which will format a localized message in the given locale.
     *
     * @param locale      the locale for the message to be returned by {@link #getLocalizedMessage()}.
     * @param key         one of {@link Resources.Keys} constants.
     * @param parameters  parameters to use for formatting the messages.
     */
    DataStoreException(final Locale locale, final short key, final Object... parameters) {
        super(Resources.forLocale(locale).getString(key, parameters));
        this.key        = key;
        this.parameters = parameters;
    }

    /**
     * Returns the exception message in the default locale, typically for system administrator.
     *
     * @return the message of this exception.
     */
    @Override
    public String getMessage() {
        return (key != 0) ? Resources.format(key, parameters) : super.getMessage();
    }

    /**
     * Returns a localized version of the exception message, typically for final user.
     * This is often the same message than the one returned by {@link #getMessage()},
     * but may in some occasions be different if {@link DataStore#setLocale(Locale)}
     * has been invoked with a different locale.
     *
     * <h4>{@code getMessage()} versus {@code getLocalizedMessage()}</h4>
     * When {@code getMessage()} and {@code getLocalizedMessage()} are not equivalent, the Apache SIS policy
     * is that {@code getMessage()} returns the message in the {@linkplain Locale#getDefault() default locale}
     * while {@code getLocalizedMessage()} returns the message in a locale that depends on the context in which
     * the exception has been thrown.
     *
     * In a client-server architecture, the former is often the locale on the <em>server</em> side while the latter
     * is the locale on the <em>client</em> side if that information has been provided to the {@link DataStore}.
     *
     * @return the localized message of this exception.
     *
     * @see DataStore#setLocale(Locale)
     *
     * @since 0.8
     */
    @Override
    public String getLocalizedMessage() {
        return super.getMessage();
    }

    /**
     * If this exception is capable to return the message in various locales, returns that message.
     * Otherwise returns {@code null}.
     *
     * @return the exception message, or {@code null} if this exception can not produce international message.
     *
     * @since 0.8
     */
    @Override
    public InternationalString getInternationalMessage() {
        return (key != 0) ? Resources.formatInternational(key, parameters) : null;
    }

    /**
     * Initializes the <i>cause</i> of this throwable to the specified value.
     *
     * @param  cause  the cause saved for later retrieval by the {@link #getCause()} method.
     * @return a reference to this {@code DataStoreException} instance.
     *
     * @since 0.8
     */
    @Override
    public DataStoreException initCause(final Throwable cause) {
        return (DataStoreException) super.initCause(cause);
    }
}
