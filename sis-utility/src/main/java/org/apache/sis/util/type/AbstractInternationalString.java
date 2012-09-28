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
package org.apache.sis.util.type;

import java.util.Locale;
import java.util.Formatter;
import java.util.Formattable;
import java.util.FormattableFlags;
import org.apache.sis.util.CharSequences;
import org.opengis.util.InternationalString;


/**
 * Base class for {@linkplain String string} that has been internationalized into several
 * {@linkplain Locale locales}. The {@link InternationalString} interface is used as a replacement
 * for the {@link String} class whenever an attribute needs to be internationalization capable.
 * The default value (as returned by {@link #toString()} and other {@link CharSequence} methods)
 * is the string in the current {@linkplain Locale#getDefault() system-wide default locale}.
 * <p>
 * The {@linkplain Comparable natural ordering} is defined by the value returned by
 * {@link #toString()}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
public abstract class AbstractInternationalString implements InternationalString, Formattable {
    /**
     * The string in the {@linkplain Locale#getDefault() system default} locale, or {@code null}
     * if this string has not yet been determined. This is the default string returned by
     * {@link #toString()} and others methods from the {@link CharSequence} interface.
     * <P>
     * This field is not serialized because serialization is often used for data transmission
     * between a server and a client, and the client may not use the same locale than the server.
     * We want the locale to be examined again on the client side.
     * <P>
     * This field is read and written by {@link SimpleInternationalString}.
     */
    transient String defaultValue;

    /**
     * Constructs an international string.
     */
    public AbstractInternationalString() {
    }

    /**
     * Returns the length of the string in the {@linkplain Locale#getDefault() default locale}.
     * This is the length of the string returned by {@link #toString()}.
     *
     * @return Length of the string in the default locale.
     */
    @Override
    public int length() {
        if (defaultValue == null) {
            defaultValue = toString();
            if (defaultValue == null) {
                return 0;
            }
        }
        return defaultValue.length();
    }

    /**
     * Returns the character of the string in the {@linkplain Locale#getDefault() default locale}
     * at the specified index. This is a character of the string returned by {@link #toString()}.
     *
     * @param  index The index of the character.
     * @return The character at the specified index.
     * @throws IndexOutOfBoundsException if the specified index is out of bounds.
     */
    @Override
    public char charAt(final int index) throws IndexOutOfBoundsException {
        if (defaultValue == null) {
            defaultValue = toString();
            if (defaultValue == null) {
                throw new StringIndexOutOfBoundsException();
            }
        }
        return defaultValue.charAt(index);
    }

    /**
     * Returns a subsequence of the string in the {@linkplain Locale#getDefault() default locale}.
     * The subsequence is a {@link String} object starting with the character value at the specified
     * index and ending with the character value at index {@code end - 1}.
     *
     * @param   start The start index, inclusive.
     * @param   end   The end index, exclusive.
     * @return  The specified subsequence.
     * @throws  IndexOutOfBoundsException if {@code start} or {@code end} is out of range.
     */
    @Override
    public CharSequence subSequence(final int start, final int end) {
        if (defaultValue == null) {
            defaultValue = toString();
            if (defaultValue == null) {
                if (start == 0 && end == 0) {
                    return "";
                }
                throw new StringIndexOutOfBoundsException();
            }
        }
        return defaultValue.substring(start, end);
    }

    /**
     * Returns this string in the given locale. If no string is available in the given locale,
     * then some default locale is used. The default locale is implementation-dependent.
     * It may or may not be the {@linkplain Locale#getDefault() system default}).
     *
     * @param  locale The desired locale for the string to be returned,
     *         or {@code null} for a string in the implementation default locale.
     * @return The string in the given locale if available, or in the default locale otherwise.
     */
    @Override
    public abstract String toString(final Locale locale);

    /**
     * Returns this string in the default locale. Invoking this method is equivalent to invoking
     * <code>{@linkplain #toString(Locale) toString}({@linkplain Locale#getDefault()})</code>.
     * <p>
     * All methods from {@link CharSequence} operate on this string.
     * This string is also used as the criterion for {@linkplain Comparable natural ordering}.
     *
     * @return The string in the default locale.
     */
    @Override
    public String toString() {
        if (defaultValue == null) {
            defaultValue = toString(Locale.getDefault());
            if (defaultValue == null) {
                return "";
            }
        }
        return defaultValue;
    }

    /**
     * Formats this international string using the given formatter.
     * This method appends the string obtained by:
     *
     * <blockquote><code>
     * {@linkplain #toString(Locale) toString}(formatter.{@linkplain Formatter#locale()})
     * </code></blockquote>
     *
     * @param formatter The formatter to use for formatting this string.
     * @param flags     A bitmask of {@link FormattableFlags} values.
     * @param width     The minimum number of characters, or -1 if none.
     * @param precision The maximum number of characters (before expanding to the {@code width}),
     *                  or -1 for no restriction.
     */
    @Override
    public void formatTo(final Formatter formatter, final int flags, int width, final int precision) {
        final Locale locale = formatter.locale();
        String value = toString(locale);
        if ((flags & FormattableFlags.UPPERCASE) != 0) {
            value = value.toUpperCase(locale);
        }
        if (precision >= 0 && precision < value.length()) {
            value = value.substring(0, precision);
        }
        final Object[] args = new Object[] {value, value};
        width -= value.length();
        String format = "%s";
        if (width >= 0) {
            format = "%s%s";
            args[(flags & FormattableFlags.LEFT_JUSTIFY) != 0 ? 1 : 0] = CharSequences.spaces(width);
        }
        formatter.format(format, args);
    }

    /**
     * Compares this string with the specified object for order. This method compares
     * the string in the {@linkplain Locale#getDefault() default locale}, as returned
     * by {@link #toString()}.
     *
     * @param  object The string to compare with this string.
     * @return A negative number if this string is before the given string, a positive
     *         number if after, or 0 if equals.
     */
    @Override
    public int compareTo(final InternationalString object) {
        return toString().compareTo(object.toString());
    }
}
