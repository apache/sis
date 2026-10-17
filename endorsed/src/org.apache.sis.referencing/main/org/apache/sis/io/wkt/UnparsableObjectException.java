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
package org.apache.sis.io.wkt;

import java.util.Locale;
import java.text.ParseException;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Errors;


/**
 * Thrown when a <i>Well Known Text</i> (<abbr>WKT</abbr>) cannot be parsed.
 * This exception may contain the error message in two languages:
 *
 * <ul>
 *   <li>{@link #getMessage()} returns the message in the default locale.
 *       In a client-server architecture, this is typically the locale on the server side.</li>
 *   <li>{@link #getLocalizedMessage()} returns the message in the locale given in argument to the
 *       {@link WKTFormat#WKTFormat(java.util.Locale, java.util.TimeZone) WKTFormat} constructor.
 *       In a client-server architecture, it is presumably the locale on the client side.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.8
 * @since   0.8
 */
public class UnparsableObjectException extends ParseException implements Localized {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5949195205739826024L;

    /**
     * The effective locale of the localized message, or {@code null} if unspecified.
     * This is the locale of the message returned by {@link #getLocalizedMessage()}.
     * It is specified at construction time, then eventually replaced by the locale
     * of the resource used for building the message.
     *
     * @see #getLocale()
     */
    private Locale locale;

    /**
     * The resources key as one of the {@link Errors.Keys} constant, or 0 if none.
     *
     * <p>This field is not serialized because key values sometimes change between different SIS versions.
     * The deserialized value will be 0, which will cause this {@code UnparsableObjectException} to fallback
     * on {@code super.getMessage()}.</p>
     */
    private transient short key;

    /**
     * The parameters for the localization message.
     */
    private transient Object[] parameters;

    /**
     * Creates an exception with the specified details message.
     *
     * @param  message      the detail message in the default locale.
     * @param  errorOffset  the position where the error is found while parsing.
     */
    public UnparsableObjectException(final String message, final int errorOffset) {
        super(message, errorOffset);
    }

    /**
     * Creates an exception with a message formatted from the given resource key and message parameters.
     *
     * @param  locale       the locale for {@link #getLocalizedMessage()}, or {@code null} for the default.
     * @param  key          the resource key as one of the {@link Errors.Keys} constant.
     * @param  parameters   the values to be given to {@link Errors#getString(short, Object)}.
     * @param  errorOffset  the position where the error is found while parsing.
     */
    UnparsableObjectException(final Locale locale, final short key, final Object[] parameters, final int errorOffset) {
        this(Errors.forLocale(locale), key, parameters, errorOffset);
    }

    /**
     * Workaround while waiting for JEP 513: Flexible Constructor Bodies.
     */
    @Workaround(library="JDK", version="11", fixed="25")
    private UnparsableObjectException(final Errors resources, final short key, final Object[] parameters, final int errorOffset) {
        super(resources.getString(key, parameters), errorOffset);
        this.locale     = resources.getLocale();
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
        return (key != 0) ? Errors.format(key, parameters) : super.getMessage();
    }

    /**
     * Returns a localized version of the exception message, typically for final user.
     * This is often the same message as the one returned by {@link #getMessage()},
     * but may in some occasions be in a different language if {@code WKTFormat} has been
     * {@linkplain WKTFormat#WKTFormat(java.util.Locale, java.util.TimeZone) constructed}
     * with a different locale.
     *
     * @return the localized message of this exception.
     */
    @Override
    public String getLocalizedMessage() {
        return super.getMessage();
    }

    /**
     * Returns the locale of the localized exception message, or {@code null} if unspecified.
     * If non-null, this is the effective language of the string returned by {@link #getLocalizedMessage()}.
     * This locale depends on the {@linkplain WKTFormat#getLocale() locale configured on the WKT parser} and
     * the resources available in Apache <abbr>SIS</abbr>.
     *
     * @return the locale of the localized message, or {@code null} if not explicitly defined.
     *
     * @since 1.8
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Initializes the <i>cause</i> of this throwable to the specified value.
     *
     * @param  cause  the cause saved for later retrieval by the {@link #getCause()} method.
     * @return a reference to this {@code UnparsableObjectException} instance.
     */
    @Override
    public UnparsableObjectException initCause(final Throwable cause) {
        return (UnparsableObjectException) super.initCause(cause);
    }
}
