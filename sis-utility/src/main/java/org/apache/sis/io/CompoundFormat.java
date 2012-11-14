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
 * Those blocks of data usually contain smaller information units like numbers and dates,
 * whose parsing and formatting can be delegated to {@link NumberFormat} and {@link DateFormat}
 * respectively.
 *
 * <p>Since this subclasses may work on larger texts than the usual {@code Format} classes,
 * they will work with {@link CharSequence} and {@link Appendable} as much as possible.
 * The abstract methods to be defined by subclasses are:</p>
 *
 * <ul>
 *   <li>{@link #getValueType()} : return the {@code <T>} class</li>
 *   <li>{@link #parse(CharSequence, ParsePosition)}</li>
 *   <li>{@link #format(Object, Appendable)} throws {@link IOException}</li>
 * </ul>
 *
 * @param <T> The type of objects parsed and formatted by this class.
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
     *
     * @see #getLocale()
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
     *       behavior at least with {@linkplain org.apache.sis.util.Type.DefaultInternationalString
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
     * Returns the type of values formatted by this {@code Format} instance.
     *
     * @return The type of values formatted by this {@code Format} instance.
     */
    public abstract Class<T> getValueType();

    /**
     * Creates an object from the given character sequence, or returns {@code null} if an error
     * occurred while parsing the characters.
     *
     * @param  text The character sequence for the object to parse.
     * @param  pos  The position where to start the parsing.
     * @return The parsed object, or {@code null} if the given character sequence can not be parsed.
     */
    public abstract T parse(CharSequence text, ParsePosition pos);

    /**
     * Creates an object from the given string representation, or returns {@code null} if an error
     * occurred while parsing the string. The default implementation delegates to
     * {@link #parse(CharSequence, ParsePosition)}.
     *
     * @param  text The string representation of the object to parse.
     * @param  pos  The position where to start the parsing.
     * @return The parsed object, or {@code null} if the given string can not be parsed.
     */
    @Override
    public T parseObject(final String text, final ParsePosition pos) {
        return parse(text, pos);
    }

    /**
     * Creates an object from the given string representation.
     * The default implementation delegates to {@link #parseObject(String, ParsePosition)}.
     *
     * @param  text The string representation of the object to parse.
     * @return The parsed object.
     * @throws ParseException If an error occurred while parsing the tree.
     */
    @Override
    public T parseObject(final String text) throws ParseException {
        final ParsePosition pos = new ParsePosition(0);
        final T table = parseObject(text, pos);
        if (table != null) {
            return table;
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
     * {@note Strictly speaking, an <code>IOException</code> could still occur if the user
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
        final Class<T> valueType = getValueType();
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
     * <ul>
     *   <li>If a format is cached for the given type, return that format.</li>
     *   <li>Otherwise if a format can be {@linkplain #createFormat(Class) created}
     *       for the given type, cache the newly created format and return it.</li>
     *   <li>Otherwise do again the same checks for the {@linkplain Class#getSuperclass() super class}.</li>
     *   <li>If no format can be created, returns {@code null}.</li>
     * </ul>
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
                    this.formats = formats = new HashMap<>(4);
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
     * This approach allows sub-classes to create specialized formats for different value
     * sub-types. For example a sub-class may choose to format {@link Double} values differently
     * than other type of numbers.
     *
     * @param  valueType The base type of values to parse or format.
     * @return The format to use for parsing of formatting values of the given type,
     *         or {@code null} if none.
     */
    protected Format createFormat(final Class<?> valueType) {
        if (valueType == Number.class) {
            if (locale == null) return null;
            return NumberFormat.getInstance(locale);
        }
        if (valueType == Date.class) {
            final DateFormat format;
            if (locale != null) {
                format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
            } else {
                format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
            }
            format.setTimeZone(timezone != null ? timezone : TimeZone.getTimeZone("UTC"));
        }
        if (valueType == Angle.class) {
            if (locale == null) return null;
            return AngleFormat.getInstance(locale);
        }
        return null;
    }
}
