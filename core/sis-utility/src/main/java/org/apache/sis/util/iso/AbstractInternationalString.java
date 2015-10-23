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
package org.apache.sis.util.iso;

import java.util.Locale;
import java.util.Formatter;
import java.util.Formattable;
import java.util.FormattableFlags;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.Utilities;
import org.apache.sis.util.CharSequences;


/**
 * Base class for character strings that has been internationalized into several locales.
 * The {@link InternationalString} interface is used instead of the {@link String} class
 * whenever an attribute needs to be internationalization capable.
 *
 * <p>The default value (as returned by {@link #toString()} and other {@link CharSequence} methods)
 * is the string in the current {@linkplain Locale#getDefault() system-wide default locale}.
 * The {@linkplain Comparable natural ordering} is defined by the value returned by {@link #toString()}.</p>
 *
 * <div class="section">Substituting a free text by a code list</div>
 * The ISO standard allows to substitute some character strings in the <cite>"free text"</cite> domain
 * by a {@link org.opengis.util.CodeList} value. This can be done with:
 *
 * <ul>
 *   <li>{@link Types#getCodeTitle(CodeList)} for getting the {@link InternationalString}
 *       instance to store in a metadata property.</li>
 *   <li>{@link Types#forCodeTitle(CharSequence)} for retrieving the {@link org.opengis.util.CodeList}
 *       previously stored as an {@code InternationalString}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class AbstractInternationalString implements InternationalString, Formattable {
    /**
     * The string in the {@linkplain Locale#getDefault() system default} locale, or {@code null}
     * if this string has not yet been determined. This is the default string returned by
     * {@link #toString()} and others methods from the {@link CharSequence} interface.
     *
     * <div class="section">Thread safety</div>
     * For thread safety this field shall either be read and written in a synchronized block,
     * or be fixed at construction time and never changed after than point. All other usages
     * are prohibited.
     *
     * <div class="section">Serialization</div>
     * This field is not serialized because serialization is often used for data transmission
     * between a server and a client, and the client may not use the same locale than the server.
     * We want the locale to be examined again on the client side.
     */
    transient String defaultValue;

    /**
     * Constructs an international string.
     */
    protected AbstractInternationalString() {
    }

    /**
     * Returns the length of the string in the {@linkplain Locale#getDefault() default locale}.
     * This is the length of the string returned by {@link #toString()}.
     *
     * @return Length of the string in the default locale.
     */
    @Override
    public int length() {
        return CharSequences.length(toString());
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
        return toString().charAt(index);
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
        return toString().substring(start, end);
    }

    /**
     * Returns this string in the given locale. If no string is available in the given locale,
     * then some fallback locale is used. The fallback locale is implementation-dependent, and
     * is not necessarily the same than the default locale used by the {@link #toString()} method.
     *
     * <div class="section">Handling of <code>Locale.ROOT</code> argument value</div>
     * {@link Locale#ROOT} can be given to this method for requesting a "unlocalized" string,
     * typically some programmatic values like enumerations or identifiers. While identifiers
     * often look like English words, {@code Locale.ROOT} is not considered synonymous to
     * {@link Locale#ENGLISH} because the values may differ in the way numbers and dates are
     * formatted (e.g. using the ISO 8601 standard for dates instead than English conventions).
     *
     * <div class="section">Handling of <code>null</code> argument value</div>
     * The {@code Locale.ROOT} constant is new in Java 6. Some other libraries designed for Java 5
     * use the {@code null} value for "unlocalized" strings. Apache SIS accepts {@code null} value
     * for inter-operability with those libraries. However the behavior is implementation dependent:
     * some subclasses will take {@code null} as a synonymous of the system default locale, while
     * other subclasses will take {@code null} as a synonymous of the root locale. In order to
     * ensure determinist behavior, client code are encouraged to specify only non-null values.
     *
     * @param  locale The desired locale for the string to be returned.
     * @return The string in the given locale if available, or in an
     *         implementation-dependent fallback locale otherwise.
     *
     * @see Locale#getDefault()
     * @see Locale#ROOT
     */
    @Override
    public abstract String toString(Locale locale);

    /**
     * Returns this string in the default locale. Invoking this method is equivalent to invoking
     * <code>{@linkplain #toString(Locale) toString}({@linkplain Locale#getDefault()})</code>.
     *
     * <p>All methods from {@link CharSequence} operate on this string.
     * This string is also used as the criterion for {@linkplain Comparable natural ordering}.</p>
     *
     * @return The string in the default locale.
     */
    @Override
    public synchronized String toString() {
        String text = defaultValue;
        if (text == null) {
            text = toString(Locale.getDefault());
            if (text == null) {
                return "";
            }
            defaultValue = text;
        }
        return text;
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
    public void formatTo(final Formatter formatter, final int flags, final int width, final int precision) {
        Utilities.formatTo(formatter, flags, width, precision, toString(formatter.locale()));
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
