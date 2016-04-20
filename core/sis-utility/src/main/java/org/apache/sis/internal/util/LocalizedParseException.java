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
package org.apache.sis.internal.util;

import java.util.Locale;
import java.text.ParsePosition;
import java.text.ParseException;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.Localized;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;


/**
 * A {@link ParseException} in which {@link #getLocalizedMessage()} returns the message in the
 * parser locale. This exception contains the error message in two languages:
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
 * @since   0.3
 * @version 0.7
 * @module
 */
public final class LocalizedParseException extends ParseException implements LocalizedException, Localized {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1467571540435486742L;

    /**
     * The locale to use for formatting the localized error message, or {@code null} for the default.
     */
    private final Locale locale;

    /**
     * The resources key as one of the {@code Errors.Keys} constant.
     */
    private final short key;

    /**
     * The arguments for the localization message.
     */
    private final Object[] arguments;

    /**
     * Constructs a {@code ParseException} with a message formatted from the given resource key
     * and message arguments. This is the most generic constructor.
     *
     * @param locale      The locale for {@link #getLocalizedMessage()}, or {@code null} for the default.
     * @param key         The resource key as one of the {@code Errors.Keys} constant.
     * @param arguments   The values to be given to {@link Errors#getString(short, Object)}.
     * @param errorOffset The position where the error is found while parsing.
     */
    public LocalizedParseException(final Locale locale, final short key, final Object[] arguments, final int errorOffset) {
        super(Errors.format(key, arguments), errorOffset);
        this.locale    = locale;
        this.arguments = arguments;
        this.key       = key;
    }

    /**
     * Constructs a {@code ParseException} with a message formatted from the given resource key
     * and unparsable string. This convenience constructor fetches the word starting at the error
     * index, and uses that word as the single argument associated to the resource key.
     *
     * @param locale      The locale for {@link #getLocalizedMessage()}, or {@code null} for the default.
     * @param key         The resource key as one of the {@code Errors.Keys} constant.
     * @param text        The full text that {@code Format} failed to parse.
     * @param errorOffset The position where the error is found while parsing.
     */
    public LocalizedParseException(final Locale locale, final short key, final CharSequence text, final int errorOffset) {
        this(locale, key, new Object[] {CharSequences.token(text, errorOffset)}, errorOffset);
    }

    /**
     * Creates a {@link ParseException} with a localized message built from the given parsing
     * information. This convenience constructor creates a message of the kind <cite>"Can not
     * parse string "text" as an object of type 'type'"</cite>.
     *
     * @param  locale The locale for {@link #getLocalizedMessage()}, or {@code null} for the default.
     * @param  type   The type of objects parsed by the {@link java.text.Format}.
     * @param  text   The full text that {@code Format} failed to parse.
     * @param  pos    Index of the {@linkplain ParsePosition#getIndex() first parsed character},
     *                together with the {@linkplain ParsePosition#getErrorIndex() error index}.
     *                Can be {@code null} if index and error index are zero.
     */
    public LocalizedParseException(final Locale locale, final Class<?> type, final CharSequence text, final ParsePosition pos) {
        this(locale, type, text, (pos != null) ? pos.getIndex() : 0, (pos != null) ? pos.getErrorIndex() : 0);
    }

    /**
     * Workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private LocalizedParseException(final Locale locale, final Class<?> type,
            final CharSequence text, final int offset, final int errorOffset)
    {
        this(locale, arguments(type, text, offset, Math.max(offset, errorOffset)), errorOffset);
    }

    /**
     * Workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private LocalizedParseException(final Locale locale, final Object[] arguments, final int errorOffset) {
        this(locale, key(arguments), arguments, errorOffset);
    }

    /**
     * Workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").
     *
     * @param  type        The type of objects parsed by the {@link java.text.Format}.
     * @param  text        The text that {@code Format} failed to parse.
     * @param  offset      Index of the first character to parse in {@code text}.
     * @param  errorOffset The position where the error is found while parsing.
     * @return The {@code arguments} value to give to the constructor.
     */
    @Workaround(library="JDK", version="1.7")
    private static Object[] arguments(final Class<?> type, CharSequence text, final int offset, final int errorOffset) {
        if (errorOffset >= text.length()) {
            return new Object[] {text};
        }
        text = text.subSequence(offset, text.length());
        final CharSequence erroneous = CharSequences.token(text, errorOffset);
        if (erroneous.length() == 0) {
            return new Object[] {type, text};
        }
        return new Object[] {type, text, erroneous};
    }

    /**
     * Workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static short key(final Object[] arguments) {
        final short key;
        switch (arguments.length) {
            case 1: key = Errors.Keys.UnexpectedEndOfString_1;    break;
            case 2: key = Errors.Keys.UnparsableStringForClass_2; break;
            case 3: key = Errors.Keys.UnparsableStringForClass_3; break;
            default: throw new AssertionError();
        }
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locale getLocale() {
        return (locale != null) ? locale : Locale.getDefault();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalizedMessage() {
        return Errors.getResources(locale).getString(key, arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalizedMessage(final Locale locale) {
        return Errors.getResources(locale).getString(key, arguments);
    }
}
