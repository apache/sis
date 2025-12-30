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
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Format;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.OffsetDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.RangeFormat;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.measure.QuantityFormat;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.MetadataServices;
import org.apache.sis.util.internal.shared.LocalizedParseException;
import static org.apache.sis.util.internal.shared.Constants.UTC;


/**
 * Base class of {@link Format} implementations which delegate part of their work to other
 * {@code Format} instances. {@code CompoundFormat} subclasses typically work on relatively
 * large blocks of data, for example a metadata tree or a <i>Well Known Text</i> (WKT).
 * Those blocks of data usually contain smaller elements like numbers and dates, whose parsing
 * and formatting can be delegated to {@link NumberFormat} and {@link DateFormat} respectively.
 * Subclasses can obtain instances of those formats by call to {@link #getFormat(Class)} where
 * the argument is the type of the value to parse or format.
 * {@code CompoundFormat} supports at least the following value types, but subclasses may add more types:
 *
 * <table class="sis">
 *   <caption>Supported value types</caption>
 *   <tr><th>Value type</th>              <th>Format type</th>                                      <th>Remarks</th></tr>
 *   <tr><td>{@link DirectPosition}</td>  <td>{@link org.apache.sis.geometry.CoordinateFormat}</td> <td>Requires {@code org.apache.sis.referencing} module.</td></tr>
 *   <tr><td>{@link Angle}</td>           <td>{@link AngleFormat}</td>                              <td></td></tr>
 *   <tr><td>{@link Date}</td>            <td>{@link DateFormat}</td>                               <td>Timezone specified by {@link #getTimeZone()}.</td></tr>
 *   <tr><td>{@link Number}</td>          <td>{@link NumberFormat}</td>                             <td></td></tr>
 *   <tr><td>{@link Unit}</td>            <td>{@link UnitFormat}</td>                               <td></td></tr>
 *   <tr><td>{@link Range}</td>           <td>{@link RangeFormat}</td>                              <td></td></tr>
 *   <tr><td>{@link Class}</td>           <td>(internal)</td>                                       <td></td></tr>
 * </table>
 *
 * <h2>Sources and destinations</h2>
 * Since {@code CompoundFormat} may work on larger texts than the usual {@code Format} classes,
 * it defines {@code parse} and {@code format} methods working with arbitrary {@link CharSequence}
 * and {@link Appendable} instances. The standard {@code Format} methods redirect to the above-cited
 * methods.
 *
 * <h2>Sub-classing</h2>
 * The abstract methods to be defined by subclasses are:
 * <ul>
 *   <li>{@link #getValueType()}</li>
 *   <li>{@link #format(Object, Appendable)}</li>
 *   <li>{@link #parse(CharSequence, ParsePosition)}</li>
 * </ul>
 *
 * <h2>Comparison with other API</h2>
 * In the standard {@link Format} class, the {@code parse} methods either accept a {@link ParsePosition} argument
 * and returns {@code null} on error, or does not take position argument and throws a {@link ParseException} on error.
 * In this {@code CompoundFormat} class, the {@code parse} method both takes a {@code ParsePosition} argument and
 * throws a {@code ParseException} on error. This allows both substring parsing and more accurate exception message
 * in case of error.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @param <T>  the base type of objects parsed and formatted by this class.
 *
 * @since 0.3
 */
public abstract class CompoundFormat<T> extends Format implements Localized {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -689151528653024968L;

    /**
     * The locale given at construction time, or {@link Locale#ROOT} (never {@code null}) for
     * unlocalized format. See {@link #getLocale()} for more information on {@code ROOT} locale.
     *
     * @see #getLocale()
     */
    private final Locale locale;

    /**
     * The timezone given at construction time, or {@code null} for UTC.
     *
     * @see #getTimeZone()
     */
    private final TimeZone timezone;

    /**
     * The formats for smaller unit of information, created when first needed.
     * {@code null} is used as a sentinel value meaning "no format".
     */
    private transient Map<Class<?>, Format> formats;

    /**
     * Creates a new format for the given locale. The given locale can be {@code null} or
     * {@link Locale#ROOT} if this format shall parse and format "unlocalized" strings.
     * See {@link #getLocale()} for more information about the {@code ROOT} locale.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone, or {@code null} for UTC.
     */
    protected CompoundFormat(final Locale locale, final TimeZone timezone) {
        this.locale   = (locale != null) ? locale : Locale.ROOT;
        this.timezone = timezone;
    }

    /**
     * Returns the locale used by this format. The returned value may be {@link Locale#ROOT}
     * if this format does not apply any localization. The definition of "unlocalized string"
     * is implementation-dependent, but some typical examples are:
     *
     * <ul>
     *   <li>Format {@link Number} instances using {@code toString()} instead of {@code NumberFormat}.</li>
     *   <li>Format {@link Date} instances using the ISO pattern instead of the English one.</li>
     * </ul>
     *
     * @return the locale of this {@code Format}, or {@code Locale.ROOT} for unlocalized format.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the locale for the given category. Subclasses may override this method in order to assign
     * different roles to the different locale categories. A typical (but not mandatory) mapping is:
     *
     * <ul>
     *   <li>{@link java.util.Locale.Category#FORMAT} specifies the locale to use for numbers, dates and angles formatting.</li>
     *   <li>{@link java.util.Locale.Category#DISPLAY} specifies the locale to use for {@link org.opengis.util.CodeList} labels
     *       and {@link org.opengis.util.InternationalString} contents.</li>
     * </ul>
     *
     * For subclasses that do not override this method, the default implementation returns {@link #getLocale()}.
     *
     * <h4>Example</h4>
     * The ISO 19162 (<cite>Well Known Text</cite>) standard requires a number format similar to the one defined by
     * {@code Locale.ROOT} while it allows informative texts (remarks, <i>etc.</i>) to be formatted according the
     * user's locale. Consequently, {@code WKTFormat} fixes (usually) the locale for {@code Category.FORMAT} to
     * {@code Locale.ROOT} and let {@code Category.DISPLAY} be any locale.
     *
     * @param  category  the category for which a locale is desired.
     * @return the locale for the given category (never {@code null}).
     *
     * @since 0.4
     */
    public Locale getLocale(final Locale.Category category) {
        ArgumentChecks.ensureNonNull("category", category);
        return getLocale();
    }

    /**
     * Returns the timezone used by this format.
     *
     * @return the timezone used for this format, or UTC for unlocalized format.
     */
    public TimeZone getTimeZone() {
        return (timezone != null) ? (TimeZone) timezone.clone() : TimeZone.getTimeZone(UTC);
    }

    /**
     * Returns the base type of values parsed and formatted by this {@code Format} instance.
     * The returned type may be a subclass of {@code <T>} if the format is configured in a way
     * that restrict the kind value to be parsed.
     *
     * <h4>Examples</h4>
     * <ul>
     *   <li>{@code StatisticsFormat} unconditionally returns {@code Statistics.class}.</li>
     *   <li>{@code TreeTableFormat} unconditionally returns {@code TreeTable.class}.</li>
     * </ul>
     *
     * @return the base type of values parsed and formatted by this {@code Format} instance.
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
     *   <li>One of the following actions is taken (at implementation choice):
     *     <ul>
     *       <li>this method returns {@code null}, or</li>
     *       <li>a {@code ParseException} is thrown with an {@linkplain ParseException#getErrorOffset() error offset}
     *           set to the index of the first unparsable character.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * if a {@code ParseException} is thrown, its error offset is usually the same as the {@code ParsePosition}
     * error index, but implementations are free to adopt a slightly different policy. For example
     * if parsing of the {@code "30.0 40,0"} coordinate fails on the coma in the last number, then the {@code pos}
     * {@linkplain ParsePosition#getErrorIndex() error index} may be set to 5 (the beginning of the {@code "40.0"}
     * character sequence) or to 7 (the coma position), depending on the implementation.</div>
     *
     * Most implementations never return {@code null}. However, some implementations may choose to return {@code null}
     * if they can determine that the given text is not a supported format and reserve {@code ParseException} for the
     * cases where the text seems to be the expected format but contains a malformed element.
     *
     * @param  text  the character sequence for the object to parse.
     * @param  pos   the position where to start the parsing.
     *               On return, the position where the parsing stopped or where an error occurred.
     * @return the parsed object, or {@code null} if the text is not recognized.
     * @throws ParseException if an error occurred while parsing the object.
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
     *
     * @param  text  the string representation of the object to parse.
     * @param  pos   the position where to start the parsing.
     * @return the parsed object, or {@code null} if the given string cannot be parsed.
     */
    @Override
    public T parseObject(final String text, final ParsePosition pos) {
        try {
            return parse(text, pos);
        } catch (ParseException e) {
            if (pos.getErrorIndex() < 0) {
                pos.setErrorIndex(e.getErrorOffset());
            }
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
     * <h4>Whitespaces</h4>
     * The usual SIS policy, as documented in the {@link org.apache.sis.util.CharSequences} class, is to test for
     * whitespaces using the {@code Character.isWhitespace(…)} method. The combination of {@code isSpaceChar(…)}
     * and {@code isISOControl(…)} done in this {@code parseObject(…)} method is more permissive since it encompasses
     * all whitespace characters, plus non-breaking spaces and non-white ISO controls.
     *
     * @param  text  the string representation of the object to parse.
     * @return the parsed object.
     * @throws ParseException if an error occurred while parsing the object.
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
        throw new LocalizedParseException(getLocale(Locale.Category.DISPLAY), getValueType(), text, pos);
    }

    /**
     * Writes a textual representation of the given object in the given stream or buffer.
     *
     * @param  object      the object to format.
     * @param  toAppendTo  where to format the object.
     * @throws IOException if an error occurred while writing to the given appendable.
     */
    public abstract void format(T object, Appendable toAppendTo) throws IOException;

    /**
     * Writes a textual representation of the given object in the given buffer.
     * This method delegates the work to {@link #format(Object, Appendable)}.
     * If an {@link IOException} occurs (for example, because {@code format(…)} performs
     * some I/O operations on other objects than the given {@link StringBuilder}),
     * the exception is wrapped in {@link UncheckedIOException}.
     *
     * @param  object      the object to format.
     * @param  toAppendTo  where to format the object.
     *
     * @since 1.6
     */
    public void format(T object, StringBuilder toAppendTo) {
        try {
            format(object, (Appendable) toAppendTo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a textual representation of the specified object in the given buffer.
     * This method delegates the work to {@link #format(Object, Appendable)}.
     * If an {@link IOException} occurs (for example, because {@code format(…)} performs
     * some I/O operations on other objects than the given {@link StringBuffer}),
     * the exception is wrapped in {@link UncheckedIOException}.
     *
     * @param  object      the object to format.
     * @param  toAppendTo  where to format the object.
     * @param  pos         ignored in current implementation.
     * @return the given buffer, returned for convenience.
     */
    @Override
    public StringBuffer format(final Object object, final StringBuffer toAppendTo, final FieldPosition pos) {
        final Class<? extends T> valueType = getValueType();
        try {
            format(valueType.cast(object), toAppendTo);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalClass_2, valueType, Classes.getClass(object)), e);
        } catch (IOException e) {
            /*
             * Should never happen when writing into a StringBuffer, unless the error
             * results from another operation than writting in the given buffer.
             */
            throw new UncheckedIOException(e);
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
     *   <li>If no format is found for a concrete class, search again for
     *       {@linkplain Classes#getAllInterfaces(Class) all implemented interfaces}.</li>
     *   <li>If no format can be created, return {@code null}.</li>
     * </ol>
     *
     * See {@link #createFormat(Class)} for the list of value types recognized by the default
     * {@code CompoundFormat} implementation.
     *
     * @param  valueType  the base type of values to parse or format, or {@code null} if unknown.
     * @return the format to use for parsing and formatting values of the given type or any parent type,
     *         or {@code null} if none.
     */
    protected Format getFormat(final Class<?> valueType) {
        if (formats == null) {
            formats = new IdentityHashMap<>(4);
        }
        Format format = formats.get(valueType);
        if (format == null && !formats.containsKey(valueType)) {
            format = createFormat(valueType);
            if (format == null) {
                /*
                 * We tried the given class directly. If it didn't worked, try the interfaces before
                 * to try the parent class. The reason is that we may have for example:
                 *
                 *     interface Length extends Quantity;                   // From JSR-385.
                 *
                 *     class MyLength extends Number implements Length;
                 *
                 * If we were looking for parent classes first, we would get a formatter for Number.
                 * But instead we want a formatter for Quantity, which is a (Number + Unit) tuple.
                 * Note that looking for directly declared interfaces first is not sufficient;
                 * we need to look for parent of Length so we can find Quantity before Number.
                 */
                final Class<?>[] interfaces = Classes.getAllInterfaces(valueType);
                Class<?> type = null;
                for (int i=0; ; i++) {
                    if (i < interfaces.length) {
                        type = interfaces[i];               // Try interfaces first.
                    } else {
                        if (i == interfaces.length) {       // Try parent classes after we tried all interfaces.
                            type = valueType;
                        }
                        type = type.getSuperclass();
                        if (type == null) break;            // No format found - stop the search with format = null.
                    }
                    format = formats.get(type);
                    if (format != null) break;              // Intentionally no formats.containsKey(type) check here.
                    format = createFormat(type);
                    if (format != null) {
                        formats.put(type, format);
                        break;
                    }
                }
            }
            formats.put(valueType, format);                 // Store result even null.
        }
        return format;
    }

    /**
     * Creates a new format to use for parsing and formatting values of the given type.
     * This method is invoked by {@link #getFormat(Class)} the first time that a format
     * is needed for the given type.
     * The class given in argument can be any of the classes listed in the "Value type" column below:
     *
     * <table class="sis">
     *   <caption>Supported value types</caption>
     *   <tr><th>Value type</th>              <th>Format type</th></tr>
     *   <tr><td>{@link DirectPosition}</td>  <td>{@link org.apache.sis.geometry.CoordinateFormat}</td></tr>
     *   <tr><td>{@link Angle}</td>           <td>{@link AngleFormat}</td></tr>
     *   <tr><td>{@link Date}</td>            <td>{@link DateFormat}</td></tr>
     *   <tr><td>{@link TemporalAccessor}</td><td>{@link DateTimeFormatter}</td></tr>
     *   <tr><td>{@link Number}</td>          <td>{@link NumberFormat}</td></tr>
     *   <tr><td>{@link Unit}</td>            <td>{@link UnitFormat}</td></tr>
     *   <tr><td>{@link Quantity}</td>        <td>{@link QuantityFormat}</td></tr>
     *   <tr><td>{@link Range}</td>           <td>{@link RangeFormat}</td></tr>
     *   <tr><td>{@link Class}</td>           <td>(internal)</td></tr>
     * </table>
     *
     * Subclasses can override this method for adding more types, or for configuring the
     * newly created {@link Format} instances. Note that implementations shall check the
     * type using the {@code expected == type} comparator, not
     * <code>expected.{@linkplain Class#isAssignableFrom(Class) isAssignableFrom}(type)</code>,
     * because the check for parent types is done by the {@link #getFormat(Class)} method.
     * This approach allows subclasses to create specialized formats for different value
     * sub-types. For example, a subclass may choose to format {@link Double} values differently
     * than other types of number.
     *
     * @param  valueType  the base type of values to parse or format.
     * @return the format to use for parsing of formatting values of the given type, or {@code null} if none.
     */
    protected Format createFormat(final Class<?> valueType) {
        /*
         * The first case below is an apparent exception to the `expected == type` rule
         * documented in this method javadoc. But actually it is not, since the call to
         * DefaultFormat.getInstance(…) will indirectly perform this kind of comparison.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Locale locale = getLocale(Locale.Category.FORMAT);
        if (Number.class.isAssignableFrom(valueType)) {
            if (Locale.ROOT.equals(locale)) {
                return DefaultFormat.getInstance(valueType);
            } else if (valueType == Number.class) {
                return NumberFormat.getInstance(locale);
            } else if (Numbers.isInteger(valueType)) {
                return NumberFormat.getIntegerInstance(locale);
            }
        } else if (TemporalAccessor.class.isAssignableFrom(valueType)) {
            final DateTimeFormatter format;
            if (valueType == ChronoLocalDateTime.class || valueType == ChronoZonedDateTime.class || valueType == OffsetDateTime.class) {
                format = DateTimeFormatter.ofLocalizedDateTime(getTemporalStyle(true), getTemporalStyle(false));
            } else if (valueType == ChronoLocalDate.class) {
                format = DateTimeFormatter.ofLocalizedDate(getTemporalStyle(true));
            } else if (valueType == LocalTime.class || valueType == OffsetTime.class) {
                format = DateTimeFormatter.ofLocalizedTime(getTemporalStyle(false));
            } else if (valueType == Instant.class) {
                format = DateTimeFormatter.ISO_INSTANT;
            } else {
                return null;
            }
            return format.withLocale(locale).toFormat();
        } else if (valueType == Date.class) {
            final DateFormat format;
            if (Locale.ROOT.equals(locale)) {
                format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
            } else {
                format = DateFormat.getDateTimeInstance(getLegacyStyle(true), getLegacyStyle(false), locale);
            }
            format.setTimeZone(getTimeZone());
            return format;
        } else if (valueType == Angle.class) {
            return AngleFormat.getInstance(locale);
        } else if (valueType == Unit.class) {
            return new UnitFormat(locale);
        } else if (valueType == Quantity.class) {
            return new QuantityFormat(locale);
        } else if (valueType == Range.class) {
            return new RangeFormat(locale);
        } else if (valueType == DirectPosition.class) {
            return MetadataServices.getInstance().createCoordinateFormat(locale, getTimeZone());
        } else if (valueType == Class.class) {
            return ClassFormat.INSTANCE;
        } else {
            final Class<?>[] interfaces = valueType.getInterfaces();
            if (ArraysExt.contains(interfaces, IdentifiedObject.class)) {
                return new IdentifiedObjectFormat(locale);
            }
        }
        return null;
    }

    /**
     * Returns the style to use for formatting dates or times.
     * This is invoked by the default implementation of {@link #createFormat(Class)} when
     * the formatter to create is a {@link DateFormat} or a {@link DateTimeFormatter}.
     *
     * @param  dates  {@code true} for the date style, or {@code false} for the time (hours) style.
     * @return the date or time style to use.
     * @since 1.5
     */
    protected FormatStyle getTemporalStyle(final boolean dates) {
        return FormatStyle.MEDIUM;
    }

    /**
     * Returns the style to use for formatting dates or times using legacy formatter.
     *
     * @param  dates  {@code true} for the date style, or {@code false} for the time (hours) style.
     * @return the date or time style to use.
     */
    private int getLegacyStyle(final boolean dates) {
        switch (getTemporalStyle(dates)) {
            case FULL:   return DateFormat.FULL;
            case LONG:   return DateFormat.LONG;
            case MEDIUM: return DateFormat.MEDIUM;
            case SHORT:  return DateFormat.SHORT;
            default:     return DateFormat.DEFAULT;
        }
    }

    /**
     * Returns a clone of this format.
     *
     * @return a clone of this format.
     */
    @Override
    public CompoundFormat<T> clone() {
        @SuppressWarnings("unchecked")
        final CompoundFormat<T> clone = (CompoundFormat<T>) super.clone();
        if (clone.formats != null) {
            clone.formats = new IdentityHashMap<>(clone.formats);
            for (final Map.Entry<Class<?>,Format> entry : clone.formats.entrySet()) {
                entry.setValue((Format) entry.getValue().clone());
            }
        }
        return clone;
    }

    /*
     * Do not override equals(Object) and hashCode(). They are unlikely to be needed since we
     * do not expect CompoundFormats to be used as keys in HashMap, especially since they are
     * mutable. Furthermore, it is difficult to check for equality since the values in the
     * `formats` map are created only when needed and we don't know how subclasses will
     * configure them.
     */
}
