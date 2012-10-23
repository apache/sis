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
package org.apache.sis.util;

import java.util.Locale;
import java.text.ParseException;
import java.text.ParsePosition;
import org.apache.sis.util.resources.Errors;


/**
 * A {@link ParseException} in which {@link #getLocalizedMessage()} returns the message in the
 * parser locale.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class LocalizedParseException extends ParseException implements LocalizedException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8240939095802384277L;

    /**
     * The locale to use for formatting the localized error message.
     */
    private final Locale locale;

    /**
     * The arguments for the localization message, as an array of length 1, 2 or 3.
     *
     * <ul>
     *   <li>The type of objects to be parsed, as a {@link Class}.
     *       Omitted if the error is "unexpected end of string".</li>
     *   <li>The text to be parsed, as a {@link String}.</li>
     *   <li>The characters that couldn't be parsed. Omitted if empty.</li>
     * </ul>
     */
    private final Object[] arguments;

    /**
     * Constructs a ParseException with the specified detail message and offset.
     *
     * @param locale      The locale for {@link #getLocalizedMessage()}.
     * @param arguments   The value of {@link #arguments(String, ParsePosition)}.
     * @param errorOffset The position where the error is found while parsing.
     */
    LocalizedParseException(final Locale locale, final Object[] arguments, final int errorOffset) {
        super(message(locale, arguments), errorOffset);
        this.locale    = locale;
        this.arguments = arguments;
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
    static Object[] arguments(final Class<?> type, String text, final int offset, final int errorOffset) {
        if (errorOffset >= text.length()) {
            return new Object[] {text};
        }
        final String erroneous = CharSequences.token(text, errorOffset).toString();
        text = text.substring(offset);
        if (erroneous.isEmpty()) {
            return new Object[] {type, text};
        }
        return new Object[] {type, text, erroneous};
    }

    /**
     * Formats the error message using the given locale and arguments.
     */
    private static String message(final Locale locale, final Object[] arguments) {
        final int key;
        switch (arguments.length) {
            case 1: key = Errors.Keys.UnexpectedEndOfString_1;    break;
            case 2: key = Errors.Keys.UnparsableStringForClass_2; break;
            case 3: key = Errors.Keys.UnparsableStringForClass_3; break;
            default: throw new AssertionError();
        }
        return Errors.getResources(locale).getString(key, arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalizedMessage() {
        return message(locale, arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage(final Locale locale) {
        return message(locale, arguments);
    }
}
