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
package org.apache.sis.util.internal.shared;

import java.util.Locale;
import java.text.ParsePosition;
import java.text.ParseException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.LocalizedException;
import org.apache.sis.util.resources.Errors;


/**
 * A {@link ParseException} in which {@link #getLocalizedMessage()} returns the message in the parser locale.
 * This exception contains the error message in two languages:
 *
 * <ul>
 *   <li>{@link ParseException#getMessage()} returns the message in the default locale.</li>
 *   <li>{@link ParseException#getLocalizedMessage()} returns the message in the locale given
 *       in argument to the constructor.</li>
 * </ul>
 *
 * This locale given to the constructor is usually the {@link java.text.Format} locale,
 * which is presumed to be the end-user locale.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LocalizedParseException extends ParseException implements LocalizedException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1467571540435486742L;

    /**
     * The resources key as one of the {@link Errors.Keys} constant, or 0 if none.
     *
     * <p>This field is not serialized because key values sometimes change between different SIS versions.
     * The deserialized value will be 0, which will cause this {@code LocalizedParseException} to fallback
     * on {@code super.getMessage()}.</p>
     */
    private final transient short key;

    /**
     * The parameters for the localization message.
     */
    private final transient Object[] parameters;

    /**
     * Constructs a {@code ParseException} with a message formatted from the given resource key and message parameters.
     * This is the most generic constructor.
     *
     * @param  locale       the locale for {@link #getLocalizedMessage()}, or {@code null} for the default.
     * @param  key          the resource key as one of the {@code Errors.Keys} constant.
     * @param  parameters   the values to be given to {@link Errors#getString(short, Object)}.
     * @param  errorOffset  the position where the error is found while parsing.
     */
    public LocalizedParseException(final Locale locale, final short key, final Object[] parameters, final int errorOffset) {
        super(Errors.forLocale(locale).getString(key, parameters), errorOffset);
        this.parameters = parameters;
        this.key        = key;
    }

    /**
     * Creates a {@link ParseException} with a localized message built from the given parsing information.
     * This convenience constructor creates a message of the kind <q>Cannot parse string "text" as an
     * object of type 'type'</q>.
     *
     * @param  locale  the locale for {@link #getLocalizedMessage()}, or {@code null} for the default.
     * @param  type    the type of objects parsed by the {@link java.text.Format}.
     * @param  text    the full text that {@code Format} failed to parse.
     * @param  pos     index of the {@linkplain ParsePosition#getIndex() first parsed character},
     *                 together with the {@linkplain ParsePosition#getErrorIndex() error index}.
     *                 Can be {@code null} if index and error index are zero.
     */
    public LocalizedParseException(final Locale locale, final Class<?> type, final CharSequence text, final ParsePosition pos) {
        this(locale, type, text, (pos != null) ? pos.getIndex() : 0, (pos != null) ? pos.getErrorIndex() : 0);
    }

    /**
     * Workaround while waiting for JEP 447: Statements before super(…).
     */
    @Workaround(library="JDK", version="1.8")
    private LocalizedParseException(final Locale locale, final Class<?> type,
            final CharSequence text, final int offset, final int errorOffset)
    {
        this(locale, parameters(type, text, offset, Math.max(offset, errorOffset)), errorOffset);
    }

    /**
     * Workaround while waiting for JEP 447: Statements before super(…).
     */
    @Workaround(library="JDK", version="1.8")
    private LocalizedParseException(final Locale locale, final Object[] parameters, final int errorOffset) {
        this(locale, key(parameters), parameters, errorOffset);
    }

    /**
     * Workaround while waiting for JEP 447: Statements before super(…).
     *
     * @param  type         the type of objects parsed by the {@link java.text.Format}.
     * @param  text         the text that {@code Format} failed to parse.
     * @param  offset       index of the first character to parse in {@code text}.
     * @param  errorOffset  the position where the error is found while parsing.
     * @return the {@code parameters} value to give to the constructor.
     */
    @Workaround(library="JDK", version="1.8")
    private static Object[] parameters(final Class<?> type, CharSequence text, final int offset, final int errorOffset) {
        if (errorOffset >= text.length()) {
            return new Object[] {text};
        }
        final CharSequence erroneous = CharSequences.token(text, errorOffset);
        text = text.subSequence(offset, text.length());
        if (erroneous.length() == 0) {
            return new Object[] {type, text};
        }
        return new Object[] {type, text, erroneous};
    }

    /**
     * Workaround while waiting for JEP 447: Statements before super(…).
     */
    @Workaround(library="JDK", version="1.8")
    private static short key(final Object[] parameters) {
        final short key;
        switch (parameters.length) {
            case 1: key = Errors.Keys.UnexpectedEndOfString_1;    break;
            case 2: key = Errors.Keys.UnparsableStringForClass_2; break;
            case 3: key = Errors.Keys.UnparsableStringForClass_3; break;
            default: throw new AssertionError();
        }
        return key;
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
     *
     * @return the localized message of this exception.
     */
    @Override
    public String getLocalizedMessage() {
        return super.getMessage();
    }

    /**
     * If this exception is capable to return the message in various locales, returns that message.
     * Otherwise returns {@code null}.
     *
     * @return the exception message, or {@code null} if this exception cannot produce international message.
     */
    @Override
    public InternationalString getInternationalMessage() {
        return (key != 0) ? Errors.formatInternational(key, parameters) : null;
    }

    /**
     * Initializes the <i>cause</i> of this throwable to the specified value.
     *
     * @param  cause  the cause saved for later retrieval by the {@link #getCause()} method.
     * @return a reference to this {@code LocalizedParseException} instance.
     */
    @Override
    public LocalizedParseException initCause(final Throwable cause) {
        return (LocalizedParseException) super.initCause(cause);
    }
}
