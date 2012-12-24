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
package org.apache.sis.io;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;
import java.io.IOException;
import java.text.Format;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import net.jcip.annotations.NotThreadSafe;
import org.opengis.util.InternationalString;

import org.apache.sis.measure.Angle;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.internal.util.LocalizedParseException;


/**
 * Base class of {@link Format} implementations which delegate part of their work to other
 * {@code Format} instances. {@code CompoundFormat} subclasses typically work on relatively
 * large blocks of data, for example a metadata tree or a <cite>Well Known Text</cite> (WKT).
 * Those blocks of data usually contain smaller elements like numbers and dates, whose parsing
 * and formatting can be delegated to {@link NumberFormat} and {@link DateFormat} respectively.
 *
 * <p>Since {@code CompoundFormat} may work on larger texts than the usual {@code Format} classes,
 * it defines {@code parse} and {@code format} methods working with arbitrary {@link CharSequence}
 * and {@link Appendable} instances. The standard {@code Format} methods redirect to the above-cited
 * methods.</p>
 *
 * <p>The abstract methods to be defined by subclasses are:</p>
 *
 * <ul>
 *   <li>{@link #getValueType()} returns the {@code <T>} class or a subclass.</li>
 *   <li>{@link #parse(CharSequence, ParsePosition)} may throws {@code ParseException}.</li>
 *   <li>{@link #format(Object, Appendable)} may throws {@code IOException}.</li>
 * </ul>
 *
 * {@note In the standard <code>Format</code> class, the <code>parse</code> methods either accept
 *        a <code>ParsePosition</code> argument and returns <code>null</code> on error,
 *        or does not take position argument and throws a <code>ParseException</code> on error.
 *        In this <code>CompoundFormat</code> class, the <code>parse</code> method both takes a
 *        <code>ParsePosition</code> argument and throws a <code>ParseException</code> on error.
 *        This allows both substring parsing and more accurate exception message in case of error.}
 *
 * @param <T> The base type of objects parsed and formatted by this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@NotThreadSafe
public abstract class CompoundFormat<T> extends Format implements Localized {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7094915750367581487L;

    /**
     * The locale given at construction time, or {@code null} for unlocalized format.
     * See {@link #getLocale()} for more information on {@code null} locale.
     */
    protected final Locale locale;

    /**
     * The timezone given at construction time, or {@code null} for UTC.
     */
    protected final TimeZone timezone;

    /**
     * The formats for smaller unit of information.
     * Will be created only when first needed.
     */
    private transient Map<Class<?>,Format> formats;

    /**
     * Creates a new format for the given locale. The given locale can be {@code null} if this
     * format shall parse and format "unlocalized" strings. See {@link #getLocale()} for more
     * information on {@code null} locale.
     *
     * @param locale   The locale, or {@code null} for unlocalized format.
     * @param timezone The timezone, or {@code null} for UTC.
     */
    protected CompoundFormat(final Locale locale, final TimeZone timezone) {
        this.locale   = locale;
        this.timezone = timezone;
    }

    /**
     * Returns the locale given at construction time. The returned locale may be {@code null}
     * if this format does not apply any localization. The definition of "unlocalized string"
     * is implementation-dependent, but some typical examples are:
     *
     * <ul>
     *   <li>Format {@link Number}s using {@code toString()} instead than {@code NumberFormat}.</li>
     *   <li>Format {@link InternationalString}s using {@code toString(null)}. This has the desired
     *       behavior at least with the {@linkplain org.apache.sis.util.iso.DefaultInternationalString#toString(Locale)
     *       SIS implementation}.</li>
     * </ul>
     *
     * @return The locale used for this format, or {@code null} for unlocalized format.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the base type of values parsed and formatted by this {@code Format} instance.
     * The returned type may be a subclass of {@code <T>} if the format is configured in a way
     * that restrict the kind value to be parsed.
     *
     * @return The base type of values parsed and formatted by this {@code Format} instance.
     */
    public abstract Class<? extends T> getValueType();

    /**
     * Creates an object from the given character sequence.
     * The parsing begins at the index given by the {@code pos} argument.
     * If parsing succeeds, then:
     *
     * <ul>
     *   <li>The {@code pos} {@linkplain ParsePosition#getIndex() index} is updated to the index
     *       after the last successfully parsed character.</li>
     *   <li>The parsed object is returned.</li>
     * </ul>
     *
     * If parsing fails, then:
     *
     * <ul>
     *   <li>The {@code pos} index is left unchanged</li>
     *   <li>The {@code pos} {@linkplain ParsePosition#getErrorIndex() error index}
     *       is set to the beginning of the unparsable character sequence.</li>
     *   <li>A {@code ParseException} is thrown with an
     *       {@linkplain ParseException#getErrorOffset() error offset} relative to the above-cited
     *       {@code pos} error index. Consequently the exact error location is <var>{@code pos}
     *       error index</var> + <var>{@code ParseException} error offset</var>.</li>
     * </ul>
     *
     * <blockquote><font size="-1"><b>Example:</b>
     * If parsing of the {@code "30.0 40,0"} coordinate fails on the coma in the last number,
     * then the {@code pos} error index will be set to 5 (the beginning of the {@code "40.0"}
     * character sequence) while the {@code ParseException} error offset will be set to 2
     * (the coma position relative the the beginning of the {@code "40.0"} character sequence).
     * </font></blockquote>
     *
     * This error offset policy is a consequence of the compound nature of {@code CompoundFormat},
     * since the exception may have been produced by a call to {@link Format#parseObject(String)}.
     *
     * @param  text The character sequence for the object to parse.
     * @param  pos  The position where to start the parsing.
     * @return The parsed object.
     * @throws ParseException If an error occurred while parsing the object.
     */
    public abstract T parse(CharSequence text, ParsePosition pos) throws ParseException;

    /**
     * Creates an object from the given string representation, or returns {@code null} if an error
     * occurred while parsing. The parsing begins at the index given by the {@code pos} argument.
     * If parsing succeeds, then:
     *
     * <ul>
     *   <li>The {@code pos} {@linkplain ParsePosition#getIndex() index} is updated to the index
     *       after the last successfully parsed character.</li>
     *   <li>The parsed object is returned.</li>
     * </ul>
     *
     * If parsing fails, then:
     *
     * <ul>
     *   <li>The {@code pos} index is left unchanged</li>
     *   <li>The {@code pos} {@linkplain ParsePosition#getErrorIndex() error index}
     *       is set to the index of the character where the error occurred.</li>
     *   <li>{@code null} is returned.</li>
     * </ul>
     *
     * The default implementation delegates to {@link #parse(CharSequence, ParsePosition)}.
     * In case of failure, the {@linkplain ParseException exception error offset} is added
     * to the {@code pos} error index.
     *
     * @param  text The string representation of the object to parse.
     * @param  pos  The position where to start the parsing.
     * @return The parsed object, or {@code null} if the given string can not be parsed.
     */
    @Override
    public T parseObject(final String text, final ParsePosition pos) {
        try {
            return parse(text, pos);
        } catch (ParseException e) {
            pos.setErrorIndex(Math.max(pos.getIndex(), pos.getErrorIndex()) + e.getErrorOffset());
            return null;
        }
    }

    /**
     * Creates an object from the given string representation.
     * The default implementation delegates to {@link #parse(CharSequence, ParsePosition)}
     * and ensures that the given string has been fully used, ignoring trailing
     * {@linkplain Character#isSpaceChar(int) spaces} and
     * {@linkplain Character#isISOControl(int) ISO control characters}.
     *
     * {@note The usual SIS policy, as documented in the <code>CharSequences</code> class,
     * is to test for whitespaces using the <code>Characters.isWhitespace(…)</code> method.
     * The combination of <code>isSpaceChar(…)</code> and <code>isISOControl(…)</code> done
     * in this <code>parseObject(…)</code> method is more permissive since it encompasses
     * all whitespace characters, plus non-breaking spaces and non-white ISO controls.}
     *
     * @param  text The string representation of the object to parse.
     * @return The parsed object.
     * @throws ParseException If an error occurred while parsing the object.
     */
    @Override
    public T parseObject(final String text) throws ParseException {
        final ParsePosition pos = new ParsePosition(0);
        final T value = parse(text, pos);
        if (value != null) {
            final int length = text.length();
            int c, n=0, i=pos.getIndex();
            do {
                if ((i += n) >= length) {
                    return value;
                }
                c = text.codePointAt(i);
                n = Character.charCount(c);
            } while (Character.isSpaceChar(c) || Character.isISOControl(c));
            pos.setErrorIndex(i);
        }
        throw new LocalizedParseException(locale, getValueType(), text, pos);
    }

    /**
     * Writes a textual representation of the given object in the given stream or buffer.
     *
     * @param  object      The object to format.
     * @param  toAppendTo  Where to format the object.
     * @throws IOException If an error occurred while writing in the given appender.
     */
    public abstract void format(T object, Appendable toAppendTo) throws IOException;

    /**
     * Writes a textual representation of the specified object in the given buffer.
     * This method delegates its work to {@link #format(Object, Appendable)}, but
     * without propagating {@link IOException}. The I/O exception should never
     * occur since we are writing in a {@link StringBuffer}.
     *
     * {@note Strictly speaking, an <code>IOException</code> could still occur if a subclass
     * overrides the above <code>format</code> method and performs some I/O operation outside
     * the given <code>StringBuffer</code>. However this is not the intended usage of this
     * class and implementors should avoid such unexpected I/O operation.}
     *
     * @param  object      The object to format.
     * @param  toAppendTo  Where to format the object.
     * @param  pos         Ignored in current implementation.
     * @return             The given buffer, returned for convenience.
     */
    @Override
    public StringBuffer format(final Object object, final StringBuffer toAppendTo, final FieldPosition pos) {
        final Class<? extends T> valueType = getValueType();
        ArgumentChecks.ensureCanCast("tree", valueType, object);
        try {
            format(valueType.cast(object), toAppendTo);
        } catch (IOException e) {
            /*
             * Should never happen when writing into a StringBuffer, unless the user
             * override the format(Object, Appendable) method. We do not rethrown an
             * AssertionError because of this possibility.
             */
            throw new BackingStoreException(e);
        }
        return toAppendTo;
    }

    /**
     * Returns the format to use for parsing and formatting values of the given type.
     * This method applies the following algorithm:
     *
     * <ol>
     *   <li>If a format is cached for the given type, return that format.</li>
     *   <li>Otherwise if a format can be {@linkplain #createFormat(Class) created}
     *       for the given type, cache the newly created format and return it.</li>
     *   <li>Otherwise do again the same checks for the {@linkplain Class#getSuperclass() super class}.</li>
     *   <li>If no format can be created, returns {@code null}.</li>
     * </ol>
     *
     * See {@link #createFormat(Class)} for the list of value types recognized by the default
     * {@code CompoundFormat} implementation.
     *
     * @param  valueType The base type of values to parse or format.
     * @return The format to use for parsing and formatting values of the given type or any
     *         parent type, or {@code null} if none.
     */
    protected Format getFormat(final Class<?> valueType) {
        Format format = null;
        Map<Class<?>,Format> formats = this.formats;
        for (Class<?> type=valueType; type!=null; type=type.getSuperclass()) {
            if (formats != null) {
                format = formats.get(type);
                if (format != null) {
                    if (type != valueType) {
                        formats.put(valueType, format);
                    }
                    break;
                }
            }
            format = createFormat(type);
            if (format != null) {
                if (formats == null) {
                    this.formats = formats = new HashMap<Class<?>,Format>(4);
                }
                formats.put(type, format);
                break;
            }
        }
        return format;
    }

    /**
     * Creates a new format to use for parsing and formatting values of the given type.
     * This method is invoked by {@link #getFormat(Class)} the first time that a format
     * is needed for the given type.
     *
     * <p>The default implementation creates the following formats:</p>
     *
     * <table class="sis">
     *   <tr><th>Value type</th>     <th>Format</th></tr>
     *   <tr><td>{@link Angle}</td>  <td>{@link AngleFormat}</td></tr>
     *   <tr><td>{@link Date}</td>   <td>{@link DateFormat}</td></tr>
     *   <tr><td>{@link Number}</td> <td>{@link NumberFormat}</td></tr>
     * </table>
     *
     * Subclasses can override this method for adding more types, or for configuring the
     * newly created {@link Format} instances. Note that implementations shall check the
     * type using the {@code expected == type} comparator, not
     * <code>expected.{@linkplain Class#isAssignableFrom(Class) isAssignableFrom}(type)</code>,
     * because the check for parent types is done by the {@link #getFormat(Class)} method.
     * This approach allows subclasses to create specialized formats for different value
     * sub-types. For example a subclass may choose to format {@link Double} values differently
     * than other types of number.
     *
     * @param  valueType The base type of values to parse or format.
     * @return The format to use for parsing of formatting values of the given type,
     *         or {@code null} if none.
     */
    protected Format createFormat(final Class<?> valueType) {
        /*
         * The first case below is an apparent exception to the 'expected == type' rule
         * documented in this method javadoc. But actually it is not, since the call to
         * DefaultFormat.getInstance(…) will indirectly perform this kind of comparison.
         */
        if (Number.class.isAssignableFrom(valueType)) {
            if (locale == null) {
                return DefaultFormat.getInstance(valueType);
            } else if (valueType == Number.class) {
                return NumberFormat.getInstance(locale);
            }
        } else if (valueType == Date.class) {
            final DateFormat format;
            if (locale != null) {
                format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
            } else {
                format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
            }
            format.setTimeZone(timezone != null ? timezone : TimeZone.getTimeZone("UTC"));
            return format;
        } else if (valueType == Angle.class) {
            return AngleFormat.getInstance(locale != null ? locale : Locale.US);
        }
        return null;
    }

    /**
     * Returns a clone of this format.
     */
    @Override
    public CompoundFormat<T> clone() {
        @SuppressWarnings("unchecked")
        final CompoundFormat<T> clone = (CompoundFormat<T>) super.clone();
        if (clone.formats != null) {
            clone.formats = new HashMap<Class<?>,Format>(clone.formats);
            for (final Map.Entry<Class<?>,Format> entry : clone.formats.entrySet()) {
                entry.setValue((Format) entry.getValue().clone());
            }
        }
        return clone;
    }

    /*
     * Do not override equals(Object) and hashCode(). They are unlikely to be needed since we
     * do not expect CompoundFormats to be used as keys in HashMap, especially since they are
     * mutable. Furthermore it is difficult to check for equality since the values in the
     * 'formats' map are created only when needed and we don't know how subclasses will
     * configure them.
     */
}
