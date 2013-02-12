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
package org.apache.sis.measure;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.Format;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;


/**
 * Parses and formats {@linkplain Range ranges} of the given type. The kind of ranges created
 * by the {@code parse} method is determined by the class of range components:
 *
 * <ul>
 *   <li>If the components type is assignable to {@link Date}, then the {@code parse} method
 *       will create {@link DateRange} objects.</li>
 *   <li>If the components type is assignable to {@link Number}, then the {@code parse} method
 *       will create {@link MeasurementRange} objects if the text to parse contains a
 *       {@linkplain Unit unit} of measure, or {@link NumberRange} otherwise.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.06)
 * @version 0.3
 * @module
 *
 * @see Range
 * @see DateRange
 * @see NumberRange
 * @see MeasurementRange
 */
public class RangeFormat extends Format {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6700474540675919894L;

    /**
     * The constant value for {@link FieldPosition} which designate the minimal value.
     * This constant can be combined with one of the {@code *_FIELD} constants defined
     * in {@link NumberFormat} or {@link DateFormat} classes for fetching the position
     * of a formatted field. For example in order to get the position where the fraction
     * digits of the {@linkplain Range#getMinValue() minimal value} begin, use:
     *
     * {@preformat java
     *     FieldPosition pos = new FieldPosition(NumberFormat.FRACTION_FIELD | RangeFormat.MIN_VALUE_FIELD);
     *     rangeFormat.format(range, buffer, pos);
     *     int beginIndex = pos.getBeginIndex();
     * }
     */
    public static final int MIN_VALUE_FIELD = 0;
    // Note: the implementation in this class requires that MIN_VALUE_FIELD is 0.

    /**
     * The constant value for {@link FieldPosition} which designate the maximal value.
     * This constant can be combined with one of the {@code *_FIELD} constants defined
     * in {@link NumberFormat} or {@link DateFormat} classes for fetching the position
     * of a formatted field. For example in order to get the position where the fraction
     * digits of the {@linkplain Range#getMaxValue() maximal value} begin, use:
     *
     * {@preformat java
     *     FieldPosition pos = new FieldPosition(NumberFormat.FRACTION_FIELD | RangeFormat.MAX_VALUE_FIELD);
     *     rangeFormat.format(range, buffer, pos);
     *     int beginIndex = pos.getBeginIndex();
     * }
     */
    public static final int MAX_VALUE_FIELD = 0x40000000;
    // Note: do not use the sign bit, since the JDK uses -1 for "no field ID".
    // The maximal value used by the formats (as of JDK 1.6) is 17.

    /**
     * The constant value for {@link FieldPosition} which designate the units of measurement.
     * This field can <strong>not</strong> be combined with other field masks.
     */
    public static final int UNIT_FIELD = 0x20000000;

    /**
     * The symbols used for parsing and formatting a range.
     */
    private RangeSymbols symbols;

    /**
     * Symbols used by this format, inferred from {@link DecimalFormatSymbols}.
     */
    private final char minusSign;

    /**
     * Symbols used by this format, inferred from {@link DecimalFormatSymbols}.
     */
    private final String infinity;

    /**
     * The type of the range components. Valid types are {@link Number}, {@link Angle},
     * {@link Date} or a subclass of those types. This value determines the kind of range
     * to be created by the parse method:
     *
     * <ul>
     *   <li>{@link NumberRange} if the element class is assignable to {@link Number}.</li>
     *   <li>{@link DateRange}   if the element class is assignable to {@link Date}.</li>
     * </ul>
     */
    protected final Class<?> elementType;

    /**
     * The format to use for parsing and formatting the range components.
     * The format is determined from the {@linkplain #elementType element type}:
     *
     * <ul>
     *   <li>{@link AngleFormat}  if the element class is assignable to {@link Angle}.</li>
     *   <li>{@link NumberFormat} if the element class is assignable to {@link Number}.</li>
     *   <li>{@link DateFormat}   if the element class is assignable to {@link Date}.</li>
     * </ul>
     */
    protected final Format elementFormat;

    /**
     * The format for units of measurement, or {@code null} if none. This is non-null if and
     * only if {@link #elementType} is assignable to {@link Number} but not to {@link Angle}.
     */
    protected final UnitFormat unitFormat;

    /**
     * Constructs a new {@code RangeFormat} for the default locale.
     *
     * @return A range format in the default locale.
     */
    public static RangeFormat getInstance() {
        return new RangeFormat();
    }

    /**
     * Constructs a new {@code RangeFormat} for the specified locale.
     *
     * @param  locale The locale.
     * @return A range format in the given locale.
     */
    public static RangeFormat getInstance(final Locale locale) {
        return new RangeFormat(locale);
    }

    /**
     * Creates a new format for parsing and formatting {@linkplain NumberRange number ranges}
     * using the {@linkplain Locale#getDefault() default locale}.
     */
    public RangeFormat() {
        this(Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Creates a new format for parsing and formatting {@linkplain NumberRange number ranges}
     * using the given locale.
     *
     * @param  locale The locale for parsing and formatting range components.
     */
    public RangeFormat(final Locale locale) {
        this(locale, Number.class);
    }

    /**
     * Creates a new format for parsing and formatting {@linkplain DateRange date ranges}
     * using the given locale and timezone.
     *
     * @param locale   The locale for parsing and formatting range components.
     * @param timezone The timezone for the date to be formatted.
     */
    public RangeFormat(final Locale locale, final TimeZone timezone) {
        this(locale, Date.class);
        ((DateFormat) elementFormat).setTimeZone(timezone);
    }

    /**
     * Creates a new format for parsing and formatting {@linkplain Range ranges} of
     * the given element class using the given locale. The element class is typically
     * {@code Date.class} or some subclass of {@code Number.class}.
     *
     * @param  locale The locale for parsing and formatting range components.
     * @param  elementType The type of range components.
     * @throws IllegalArgumentException If the given type is not recognized by this constructor.
     */
    public RangeFormat(final Locale locale, final Class<?> elementType) throws IllegalArgumentException {
        this.elementType = elementType;
        if (Angle.class.isAssignableFrom(elementType)) {
            elementFormat = AngleFormat.getInstance(locale);
            unitFormat    = null;
        } else if (Number.class.isAssignableFrom(elementType)) {
            elementFormat = NumberFormat.getNumberInstance(locale);
            unitFormat    = UnitFormat.getInstance(locale);
        } else if (Date.class.isAssignableFrom(elementType)) {
            elementFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
            unitFormat    = null;
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, elementType));
        }
        final DecimalFormatSymbols ds;
        if (elementFormat instanceof DecimalFormat) {
            ds = ((DecimalFormat) elementFormat).getDecimalFormatSymbols();
        } else {
            ds = DecimalFormatSymbols.getInstance(locale);
        }
        minusSign = ds.getMinusSign();
        infinity  = ds.getInfinity();
        symbols   = new RangeSymbols();
    }

    /**
     * Returns the symbols used for parsing and formatting ranges.
     *
     * @return The symbols used by this format.
     */
    public RangeSymbols getSymbols() {
        return symbols.clone();
    }

    /**
     * Sets the symbols to use for parsing and formatting ranges.
     *
     * @param symbols The new symbols to use for this format.
     */
    public void setSymbols(final RangeSymbols symbols) {
        this.symbols = symbols.clone();
    }

    /**
     * Returns the pattern used by {@link #elementFormat} for formatting the minimum and
     * maximum values. If the element format does not use pattern, returns {@code null}.
     *
     * @param  localized {@code true} for returning the localized pattern, or {@code false}
     *         for the unlocalized one.
     * @return The pattern, or {@code null} if the {@link #elementFormat} doesn't use pattern.
     */
    public String getElementPattern(final boolean localized) {
        final Format format = elementFormat;
        if (format instanceof DecimalFormat) {
            final DecimalFormat df = (DecimalFormat) format;
            return localized ? df.toLocalizedPattern() : df.toPattern();
        }
        if (format instanceof SimpleDateFormat) {
            final SimpleDateFormat df = (SimpleDateFormat) format;
            return localized ? df.toLocalizedPattern() : df.toPattern();
        }
        if (format instanceof AngleFormat) {
            return ((AngleFormat) format).toPattern();
        }
        return null;
    }

    /**
     * Sets the pattern to be used by {@link #elementFormat} for formatting the minimum and
     * maximum values.
     *
     * @param  pattern The new pattern.
     * @param  localized {@code true} if the given pattern is localized.
     * @throws IllegalStateException If the {@link #elementFormat} does not use pattern.
     */
    public void setElementPattern(final String pattern, final boolean localized) {
        final Format format = elementFormat;
        if (format instanceof DecimalFormat) {
            final DecimalFormat df = (DecimalFormat) format;
            if (localized) {
                df.applyLocalizedPattern(pattern);
            } else {
                df.applyPattern(pattern);
            }
        } else if (format instanceof SimpleDateFormat) {
            final SimpleDateFormat df = (SimpleDateFormat) format;
            if (localized) {
                df.applyLocalizedPattern(pattern);
            } else {
                df.applyPattern(pattern);
            }
        } else if (format instanceof AngleFormat) {
            ((AngleFormat) format).applyPattern(pattern);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Formats a {@link Range} and appends the resulting text to a given string buffer. The default
     * implementation formats the range using the same rules than {@link Range#toString()}, except
     * that the values (numbers, angles or dates) are formatted using the {@link Format} object
     * appropriate for the locale given at construction time.
     *
     * @param  range      The {@link Range} object to format.
     * @param  toAppendTo Where the text is to be appended.
     * @param  pos        Identifies a field in the formatted text.
     * @return The string buffer passed in as {@code toAppendTo}, with formatted text appended.
     * @throws IllegalArgumentException If this formatter can not format the given object.
     */
    @Override
    public StringBuffer format(final Object range, final StringBuffer toAppendTo, final FieldPosition pos) {
        if (!(range instanceof Range<?>)) {
            final String message;
            if (range == null) {
                message = Errors.format(Errors.Keys.NullArgument_1, "range");
            } else {
                message = Errors.format(Errors.Keys.IllegalArgumentClass_3, "range", Range.class, range.getClass());
            }
            throw new IllegalArgumentException(message);
        }
        /*
         * Special case for an empty range. This is typically formatted as "[]". The field
         * position is unconditionally set to the empty substring inside the brackets.
         */
        final Range<?> r = (Range<?>) range;
        final RangeSymbols s = symbols;
        if (r.isEmpty()) {
            toAppendTo.append(s.openInclusive);
            final int p = toAppendTo.length();
            pos.setBeginIndex(p); // First index, inclusive.
            pos.setEndIndex  (p); // Last index, exclusive
            return toAppendTo.append(s.closeInclusive);
        }
        /*
         * Prepares the FieldPosition for the minimal and the maximal values. We need to
         * ensure that those two FieldPositions have their MAX_VALUE_FIELD bit cleared.
         * We opportunistically reuse the FieldPosition provided by the user if suitable
         * (this approach assumes that MIN_VALUE_FIELD is zero).
         */
        final FieldPosition minPos, maxPos;
        final int fieldID = pos.getField();
        if ((fieldID & MAX_VALUE_FIELD) == 0) {
            minPos = pos; // User is interested in minimal value.
            maxPos = new FieldPosition(fieldID);
        } else {
            minPos = new FieldPosition(fieldID & ~MAX_VALUE_FIELD);
            maxPos = minPos; // Will overwrite the value of minPos.
        }
        final Comparable<?> minValue = r.getMinValue();
        final Comparable<?> maxValue = r.getMaxValue();
        if (minValue != null && minValue.equals(maxValue)) {
            /*
             * Special case: minimal and maximal values are the same.  Formats only the minimal
             * value. If the user asked for the position of the maximal value, then the indexes
             * of the minimal value (which is also the maximal value) will be copied at the end
             * of this method (this work because maxPos == minPos in such case).
             */
            elementFormat.format(minValue, toAppendTo, minPos);
        } else {
            /*
             * General case: format the minimal and maximal values between brackets.
             * Units of measurement are added in the range is actually a MeasurementRange.
             */
            toAppendTo.append(r.isMinIncluded() ? s.openInclusive : s.openExclusive);
            if (minValue == null) {
                toAppendTo.append(minusSign);
                minPos.setBeginIndex(toAppendTo.length());
                toAppendTo.append(infinity);
                minPos.setEndIndex(toAppendTo.length());
            } else {
                elementFormat.format(minValue, toAppendTo, minPos);
            }
            toAppendTo.append(' ').append(s.separator).append(' ');
            if (maxValue == null) {
                maxPos.setBeginIndex(toAppendTo.length());
                toAppendTo.append(infinity);
                maxPos.setEndIndex(toAppendTo.length());
            } else {
                elementFormat.format(maxValue, toAppendTo, maxPos);
            }
            toAppendTo.append(r.isMaxIncluded() ? s.closeInclusive : s.closeExclusive);
        }
        /*
         * If the user asked for the position of the minimal value, then 'pos' is already defined
         * correctly because 'minPos == pos'. If the user asked for the position of the maximal
         * value, then we need to copy the indexes from the 'maxPos' instance.
         */
        if (pos != minPos) {
            pos.setBeginIndex(maxPos.getBeginIndex());
            pos.setEndIndex  (maxPos.getEndIndex());
        }
        /*
         * Formats the unit, if there is any. Note that the above lines processed UNIT_FIELD as
         * if it was MIN_VALUE_FIELD with some code not recognized by the formatter, so we need
         * to overwrite those indexes below in such case.
         */
        final boolean isUnitField = (pos.getField() == UNIT_FIELD);
        if (unitFormat != null && range instanceof MeasurementRange<?>) {
            final Unit<?> units = ((MeasurementRange<?>) range).getUnits();
            if (units != null) {
                toAppendTo.append(' ');
                if (isUnitField) {
                    pos.setBeginIndex(toAppendTo.length());
                }
                unitFormat.format(units, toAppendTo, pos);
                if (isUnitField) {
                    pos.setEndIndex(toAppendTo.length());
                }
                return toAppendTo;
            }
        }
        if (isUnitField) {
            final int length = toAppendTo.length();
            pos.setBeginIndex(length);
            pos.setEndIndex  (length);
        }
        return toAppendTo;
    }

    /**
     * Parses text from a string to produce a range. The default implementation delegates to
     * {@link #parse(String)} with no additional work.
     *
     * @param  source The text, part of which should be parsed.
     * @return A range parsed from the string, or {@code null} in case of error.
     * @throws ParseException If the given string can not be fully parsed.
     */
    @Override
    public Object parseObject(final String source) throws ParseException {
        return parse(source);
    }

    /**
     * Parses text from a string to produce a range. The default implementation delegates to
     * {@link #parse(String, ParsePosition)} with no additional work.
     *
     * @param  source The text, part of which should be parsed.
     * @param  pos    Index and error index information as described above.
     * @return A range parsed from the string, or {@code null} in case of error.
     */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        return parse(source, pos);
    }

    /**
     * Parses text from the given string to produce a range. This method use the full string.
     * If there is some unparsed characters after the parsed range, then this method thrown an
     * exception.
     *
     * @param  source The text to parse.
     * @return The parsed range (never {@code null}).
     * @throws ParseException If the given string can not be fully parsed.
     */
    public Range<?> parse(final String source) throws ParseException {
        final ParsePosition pos = new ParsePosition(0);
        UnconvertibleObjectException failure = null;
        try {
            final Range<?> range = tryParse(source, pos);
            if (range != null) {
                return range;
            }
        } catch (UnconvertibleObjectException e) {
            failure = e;
        }
        final int errorIndex = pos.getErrorIndex();
        final ParseException e = new ParseException(Errors.format(Errors.Keys.UnparsableStringForClass_3,
                elementType, source, CharSequences.token(source, errorIndex)), errorIndex);
        e.initCause(failure);
        throw e;
    }

    /**
     * Parses text from a string to produce a range. The method attempts to parse text starting
     * at the index given by {@code pos}. If parsing succeeds, then the index of {@code pos} is
     * updated to the index after the last character used, and the parsed range is returned. If
     * an error occurs, then the index of {@code pos} is not changed, the error index of {@code pos}
     * is set to the index of the character where the error occurred, and {@code null} is returned.
     *
     * @param  source The text, part of which should be parsed.
     * @param  pos    Index and error index information as described above.
     * @return A range parsed from the string, or {@code null} in case of error.
     */
    public Range<?> parse(final String source, final ParsePosition pos) {
        final int origin = pos.getIndex();
        Range<?> range;
        try {
            // Remainder: tryParse may return null.
            range = tryParse(source, pos);
        } catch (UnconvertibleObjectException e) {
            // Ignore - the error will be reported through the error index.
            range = null;
        }
        if (range != null) {
            pos.setErrorIndex(-1);
        } else {
            pos.setIndex(origin);
        }
        return range;
    }

    /**
     * Tries to parse the given text. In case of success, the error index is undetermined and
     * need to be reset to -1.  In case of failure (including an exception being thrown), the
     * parse index is undetermined and need to be reset to its initial value.
     */
    private Range<?> tryParse(final String source, final ParsePosition pos)
            throws UnconvertibleObjectException
    {
        final int length = source.length();
        int index = pos.getIndex();
        /*
         * Skip leading whitespace and find the first non-blank character.  It is usually
         * an opening bracket, except if minimal and maximal values are the same in which
         * case the brackets may be omitted.
         */
        char c;
        do if (index >= length) {
            pos.setErrorIndex(length);
            return null;
        } while ((Character.isWhitespace(c = source.charAt(index++))));
        /*
         * Get the minimal and maximal values, and whatever they are inclusive or exclusive.
         */
        final RangeSymbols s = symbols;
        final Object minValue, maxValue;
        final boolean isMinIncluded, isMaxIncluded;
        if (!s.isOpen(c)) {
            /*
             * No bracket. Assume that we have a single value for the range.
             */
            pos.setIndex(index - 1);
            final Object value = elementFormat.parseObject(source, pos);
            if (value == null) {
                return null;
            }
            pos.setErrorIndex(index - 1); // In case of failure during the conversion.
            minValue = maxValue = convert(value);
            isMinIncluded = isMaxIncluded = true;
            index = pos.getIndex();
        } else {
            /*
             * We found an opening bracket. Skip the whitespaces. If the next
             * character is a closing bracket, then we have an empty range.
             */
            isMinIncluded = (c == s.openInclusive);
            do if (index >= length) {
                pos.setErrorIndex(length);
                return null;
            } while ((Character.isWhitespace(c = source.charAt(index++))));
            if (s.isClose(c)) {
                pos.setIndex(index);
                pos.setErrorIndex(index - 1); // In case of failure during the conversion.
                minValue = maxValue = convert(0);
                isMaxIncluded = false;
            } else {
                /*
                 * At this point, we have determined that the range is non-empty and there
                 * is at least one value to parse. First, parse the minimal value. If we
                 * fail to parse, check if it was the infinity value (note that infinity
                 * should have been parsed successfully if the format is DecimalFormat).
                 */
                pos.setIndex(index - 1);
                Object value = elementFormat.parseObject(source, pos);
                if (value == null) {
                    if (c != minusSign) {
                        index--;
                    }
                    if (!source.regionMatches(index, infinity, 0, infinity.length())) {
                        return null;
                    }
                    pos.setIndex(index += infinity.length());
                }
                pos.setErrorIndex(index - 1); // In case of failure during the conversion.
                minValue = convert(value);
                /*
                 * Parsing of minimal value succeed and its type is valid. Now look for the
                 * separator. If it is not present, then assume that we have a single value
                 * for the range. The default RangeFormat implementation does not format
                 * brackets in such case (see the "No bracket" case above), but we make the
                 * parser tolerant to the case where the brackets are present.
                 */
                index = pos.getIndex();
                do if (index >= length) {
                    pos.setErrorIndex(length);
                    return null;
                } while ((Character.isWhitespace(c = source.charAt(index++))));
                final String separator = s.separator;
                if (source.regionMatches(index-1, separator, 0, separator.length())) {
                    index += separator.length() - 1;
                    do if (index >= length) {
                        pos.setErrorIndex(length);
                        return null;
                    } while ((Character.isWhitespace(c = source.charAt(index++))));
                    pos.setIndex(index - 1);
                    value = elementFormat.parseObject(source, pos);
                    if (value == null) {
                        if (!source.regionMatches(--index, infinity, 0, infinity.length())) {
                            return null;
                        }
                        pos.setIndex(index += infinity.length());
                    }
                    pos.setErrorIndex(index - 1); // In case of failure during the conversion.
                    maxValue = convert(value);
                    /*
                     * Skip one last time the whitespaces. The check for the closing bracket
                     * (which is mandatory) is performed outside the "if" block since it is
                     * common to the two "if ... else" cases.
                     */
                    index = pos.getIndex();
                    do if (index >= length) {
                        pos.setErrorIndex(length);
                        return null;
                    } while ((Character.isWhitespace(c = source.charAt(index++))));
                } else {
                    maxValue = minValue;
                }
                if (!s.isClose(c)) {
                    pos.setErrorIndex(index - 1);
                    return null;
                }
                isMaxIncluded = (c == s.closeInclusive);
            }
            pos.setIndex(index);
        }
        /*
         * Parses the unit, if any. The units are always optional: if we can not parse
         * them, then we will consider that the parsing stopped before the units.
         */
        Unit<?> unit = null;
        if (unitFormat != null) {
            while (index < length) {
                if (Character.isWhitespace(source.charAt(index))) {
                    index++;
                    continue;
                }
                // At this point we found a character that could be
                // the beginning of a unit symbol. Try to parse that.
                pos.setIndex(index);
// TODO: Uncomment when we have upgrated JSR-275 dependency.
//              unit = unitFormat.parse(source, pos);
                break;
            }
        }
        /*
         * At this point, all required informations are available. Now build the range.
         * In the special case were the target type is the generic Number type instead
         * than a more specialized type, the finest suitable type will be determined.
         */
        if (Number.class.isAssignableFrom(elementType)) {
            @SuppressWarnings({"unchecked","rawtypes"})
            Class<? extends Number> type = (Class) elementType;
            Number min = (Number) minValue;
            Number max = (Number) maxValue;
            if (type == Number.class) {
                type = Numbers.widestClass(Numbers.narrowestClass(min), Numbers.narrowestClass(max));
                min  = Numbers.cast(min, type);
                max  = Numbers.cast(max, type);
            }
            if (min.doubleValue() == Double.NEGATIVE_INFINITY) min = null;
            if (max.doubleValue() == Double.POSITIVE_INFINITY) max = null;
            if (unit != null) {
                @SuppressWarnings({"unchecked","rawtypes"})
                final MeasurementRange<?> range = new MeasurementRange(type, min, isMinIncluded, max, isMaxIncluded, unit);
                return range;
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            final NumberRange<?> range = new NumberRange(type, min, isMinIncluded, max, isMaxIncluded);
            return range;
        } else if (Date.class.isAssignableFrom(elementType)) {
            final Date min = (Date) minValue;
            final Date max = (Date) maxValue;
            return new DateRange(min, isMinIncluded, max, isMaxIncluded);
        } else {
            @SuppressWarnings({"unchecked","rawtypes"})
            final Class<? extends Comparable<?>> type = (Class) elementType;
            final Comparable<?> min = (Comparable<?>) minValue;
            final Comparable<?> max = (Comparable<?>) maxValue;
            @SuppressWarnings({"unchecked","rawtypes"})
            final Range<?> range = new Range(type, min, isMinIncluded, max, isMaxIncluded);
            return range;
        }
    }

    /**
     * Converts the given value to the a {@link #elementType} type.
     */
    @SuppressWarnings("unchecked")
    private Object convert(final Object value) throws UnconvertibleObjectException {
        if (value == null || elementType.isInstance(value)) {
            return value;
        }
        if (value instanceof Number && Number.class.isAssignableFrom(elementType)) {
            return Numbers.cast((Number) value, (Class<? extends Number>) elementType);
        }
        throw new UnconvertibleObjectException(Errors.format(
                Errors.Keys.IllegalClass_2, elementType, value.getClass()));
    }
}
