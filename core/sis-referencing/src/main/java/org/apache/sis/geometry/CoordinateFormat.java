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
package org.apache.sis.geometry;

import java.text.Format;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.io.IOException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Time;
import javax.measure.IncommensurableException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.apache.sis.internal.util.LocalizedParseException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.io.CompoundFormat;

// Branch-dependent imports
import java.io.UncheckedIOException;


/**
 * Formats spatiotemporal coordinates using number, angle and date formats inferred from the coordinate system.
 * The format for each ordinate is inferred from the
 * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getUnit() coordinate system units}
 * using the following rules:
 *
 * <ul>
 *   <li>Ordinate values in angular units are formated as angles using {@link AngleFormat}.</li>
 *   <li>Ordinate values in temporal units are formated as dates using {@link DateFormat}.</li>
 *   <li>Other values are formatted as numbers using {@link NumberFormat} followed by the unit symbol
 *       formatted by {@link org.apache.sis.measure.UnitFormat}.</li>
 * </ul>
 *
 * The format can be controlled by invoking the {@link #applyPattern(Class, String)} public method,
 * or by overriding the {@link #createFormat(Class)} protected method.
 *
 * <p>This format does <strong>not</strong> transform the given coordinates in a unique CRS.
 * If the coordinates need to be formatted in a specific CRS, then the caller should
 * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform#transform(DirectPosition, DirectPosition)
 * transform the position} before to format it.</p>
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 0.8
 *
 * @see AngleFormat
 * @see org.apache.sis.measure.UnitFormat
 * @see GeneralDirectPosition
 *
 * @since 0.8
 * @module
 */
public class CoordinateFormat extends CompoundFormat<DirectPosition> {
    /**
     * Serial number for cross-version compatibility.
     */
    private static final long serialVersionUID = 8324486673169133932L;

    /**
     * Maximal number of characters to convert to {@link String} if the text to parse is not a string instance.
     * This is an arbitrary limit that may change (or be removed) in any future SIS version.
     */
    private static final int READ_AHEAD_LIMIT = 256;

    /**
     * Maximal number of dimensions to use when parsing a coordinate without {@link #defaultCRS}.
     * This is an arbitrary limit that may change (or be removed) in any future SIS version.
     * To avoid this limitation, users are encouraged to specify a default CRS.
     */
    private static final int DEFAULT_DIMENSION = 4;

    /**
     * The separator between each coordinate values to be formatted.
     * The default value is a space.
     */
    private String separator;

    /**
     * The separator without spaces, used at parsing time.
     */
    private String parseSeparator;

    /**
     * The coordinate reference system to assume if no CRS is attached to the position to format.
     * May be {@code null}.
     */
    private CoordinateReferenceSystem defaultCRS;

    /**
     * The coordinate reference system of the last {@link DirectPosition} that we formatted.
     * This is used for determining if we need to recompute all other transient fields in this class.
     */
    private transient CoordinateReferenceSystem lastCRS;

    /**
     * Constants for the {@link #types} array.
     */
    private static final byte LONGITUDE=1, LATITUDE=2, ANGLE=3, DATE=4, TIME=5;

    /**
     * The type for each value in the {@code formats} array, or {@code null} if not yet computed.
     * Types are: 0=number, 1=longitude, 2=latitude, 3=other angle, 4=date, 5=elapsed time.
     */
    private transient byte[] types;

    /**
     * The formats to use for formatting, or {@code null} if not yet computed.
     * This array length should be equal to the {@link #lastCRS} dimension.
     */
    private transient Format[] formats;

    /**
     * The units for each dimension to be formatted as number.
     * We do not store this information for dimensions to be formatted as angle or date.
     */
    private transient Unit<?>[] units;

    /**
     * Conversions from arbitrary units to the unit used by formatter, or {@code null} if none.
     * For example in the case of dates, this is the conversions from temporal axis units to milliseconds.
     */
    private transient UnitConverter[] toFormatUnit;

    /**
     * Units symbols. Used only for ordinate to be formatted as ordinary numbers.
     * Non-null only if at least one ordinate is to be formatted that way.
     */
    private transient String[] unitSymbols;

    /**
     * Flags the ordinate values that need to be inverted before to be formatted.
     * This is needed for example if the axis is oriented toward past instead than future,
     * or toward west instead than east.
     *
     * @see #negate(int)
     */
    private transient int negate;

    /**
     * The time epochs. Non-null only if the at least on ordinate is to be formatted as a date.
     */
    private transient long[] epochs;

    /**
     * Dummy field position.
     */
    private transient FieldPosition dummy;

    /**
     * Temporary buffer to use if the {@code toAppendTo} argument given to {@link #format(DirectPosition, Appendable)}
     * is not an instance of {@code StringBuffer}.
     */
    private transient StringBuffer buffer;

    /**
     * Constructs a new coordinate format with default locale and timezone.
     */
    public CoordinateFormat() {
        this(Locale.getDefault(Locale.Category.FORMAT), TimeZone.getDefault());
    }

    /**
     * Constructs a new coordinate format for the specified locale and timezone.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone, or {@code null} for UTC.
     */
    public CoordinateFormat(final Locale locale, final TimeZone timezone) {
        super(locale, timezone);
        parseSeparator = separator = " ";
    }

    /**
     * Returns the separator between each coordinate (number, angle or date).
     * The default value is a single space.
     *
     * @return the current coordinate separator.
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Sets the separator between each coordinate.
     * The default value is a single space.
     *
     * @param  separator  the new coordinate separator.
     */
    public void setSeparator(final String separator) {
        ArgumentChecks.ensureNonEmpty("separator", separator);
        this.separator = separator;
        parseSeparator = CharSequences.trimWhitespaces(separator);
        if (parseSeparator.isEmpty()) {
            parseSeparator = separator;
        }
    }

    /**
     * Returns the coordinate reference system to use if no CRS is explicitely associated to a given {@code DirectPosition}.
     *
     * @return the default coordinate reference system, or {@code null} if none.
     */
    public CoordinateReferenceSystem getDefaultCRS() {
        return defaultCRS;
    }

    /**
     * Sets the coordinate reference system to use if no CRS is explicitely associated to a given {@code DirectPosition}.
     * This CRS is only a default; positions given in another CRS are <strong>not</strong> automatically transformed to
     * that CRS before formatting.
     *
     * @param  crs  the default coordinate reference system, or {@code null} if none.
     */
    public void setDefaultCRS(final CoordinateReferenceSystem crs) {
        defaultCRS = crs;
    }

    /**
     * Computes the value of transient fields from the given CRS.
     */
    private void initialize(final CoordinateReferenceSystem crs) {
        types        = null;
        formats      = null;
        units        = null;
        toFormatUnit = null;
        unitSymbols  = null;
        epochs       = null;
        negate       = 0;
        lastCRS      = crs;
        if (crs == null) {
            return;
        }
        /*
         * If no CRS were specified, we will format everything as numbers. Working with null CRS
         * is sometime useful because null CRS are allowed in DirectPosition according ISO 19107.
         * Otherwise (if a CRS is given), infer the format subclasses from the axes.
         */
        final CoordinateSystem cs = crs.getCoordinateSystem();
        final int dimension = cs.getDimension();
        final byte[]   types   = new byte  [dimension];
        final Format[] formats = new Format[dimension];
        for (int i=0; i<dimension; i++) {
            final CoordinateSystemAxis axis = cs.getAxis(i);
            final Unit<?> unit = axis.getUnit();
            /*
             * Formatter for angular units. Target unit is DEGREE_ANGLE.
             * Type is LONGITUDE, LATITUDE or ANGLE depending on axis direction.
             */
            if (Units.isAngular(unit)) {
                byte type = ANGLE;
                final AxisDirection dir = axis.getDirection();
                if      (AxisDirection.NORTH.equals(dir)) {type = LATITUDE;}
                else if (AxisDirection.EAST .equals(dir)) {type = LONGITUDE;}
                else if (AxisDirection.SOUTH.equals(dir)) {type = LATITUDE;  negate(i);}
                else if (AxisDirection.WEST .equals(dir)) {type = LONGITUDE; negate(i);}
                types  [i] = type;
                formats[i] = getFormat(Angle.class);
                setConverter(dimension, i, unit.asType(javax.measure.quantity.Angle.class).getConverterTo(Units.DEGREE));
                continue;
            }
            /*
             * Formatter for temporal units. Target unit is MILLISECONDS.
             * Type is DATE.
             */
            if (Units.isTemporal(unit)) {
                final CoordinateReferenceSystem t = CRS.getComponentAt(crs, i, i+1);
                if (t instanceof TemporalCRS) {
                    if (epochs == null) {
                        epochs = new long[dimension];
                    }
                    types  [i] = DATE;
                    formats[i] = getFormat(Date.class);
                    epochs [i] = ((TemporalCRS) t).getDatum().getOrigin().getTime();
                    setConverter(dimension, i, unit.asType(Time.class).getConverterTo(Units.MILLISECOND));
                    if (AxisDirection.PAST.equals(axis.getDirection())) {
                        negate(i);
                    }
                    continue;
                }
                types[i] = TIME;
                // Fallthrough: formatted as number.
            }
            /*
             * Formatter for all other units. Do NOT set types[i] since it may have been set
             * to a non-zero value by previous case. If not, the default value (zero) is the
             * one we want.
             */
            formats[i] = getFormat(Number.class);
            if (unit != null) {
                if (units == null) {
                    units = new Unit<?>[dimension];
                }
                units[i] = unit;
                final String symbol = getFormat(Unit.class).format(unit);
                if (!symbol.isEmpty()) {
                    if (unitSymbols == null) {
                        unitSymbols = new String[dimension];
                    }
                    unitSymbols[i] = symbol;
                }
            }
        }
        this.types   = types;           // Assign only on success.
        this.formats = formats;
    }

    /**
     * Sets the unit converter at the given index.
     */
    private void setConverter(final int dimension, final int i, final UnitConverter c) {
        if (!c.isIdentity()) {
            if (toFormatUnit == null) {
                toFormatUnit = new UnitConverter[dimension];
            }
            toFormatUnit[i] = c;
        }
    }

    /**
     * Remembers that ordinate values at the given dimension will need to have their sign reverted.
     */
    private void negate(final int dimension) {
        if (dimension >= Integer.SIZE) {
            throw new ArithmeticException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, dimension));
        }
        negate |= (1 << dimension);
    }

    /**
     * Returns {@code true} if the value at the given dimension needs to have its sign reversed.
     */
    private boolean isNegative(final int dimension) {
        return (dimension < Integer.SIZE) && (negate & (1 << dimension)) != 0;
    }

    /**
     * Returns the pattern for number, angle or date fields. The given {@code valueType} should be
     * {@code Number.class}, {@code Angle.class}, {@code Date.class} or a sub-type of the above.
     * This method may return {@code null} if the underlying format can not provide a pattern.
     *
     * <table class="sis">
     *   <caption>Pattern availability for type of value</caption>
     *   <tr><th>Value type</th>     <th>Base format class</th>    <th>Format with pattern</th></tr>
     *   <tr><td>{@link Number}</td> <td>{@link NumberFormat}</td> <td>{@link DecimalFormat}</td></tr>
     *   <tr><td>{@link Angle}</td>  <td>{@link AngleFormat}</td>  <td>{@link AngleFormat}</td></tr>
     *   <tr><td>{@link Date}</td>   <td>{@link DateFormat}</td>   <td>{@link SimpleDateFormat}</td></tr>
     * </table>
     *
     * @param  valueType  the base type of ordinate values to parse and format:
     *                    {@code Number.class}, {@code Angle.class} or {@code Date.class}.
     * @return the pattern for fields of the given type, or {@code null} if not applicable.
     *
     * @see #getFormat(Class)
     */
    public String getPattern(final Class<?> valueType) {
        final Format format = getFormat(valueType);
        if (format instanceof AngleFormat) {
            return ((AngleFormat) format).toPattern();
        } else if (format instanceof DecimalFormat) {
            return ((DecimalFormat) format).toPattern();
        } else if (format instanceof SimpleDateFormat) {
            return ((SimpleDateFormat) format).toPattern();
        } else {
            return null;
        }
    }

    /**
     * Sets the pattern for number, angle or date fields.
     * The pattern syntax depends on the {@code valueType} argument:
     *
     * <ul>
     *   <li>If {@code valueType} is {@code Number.class}, then the pattern syntax shall be as described in the
     *     {@link DecimalFormat} class. This pattern may be used for any ordinate to be formatted as plain number,
     *     for example in {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian coordinate system}.</li>
     *   <li>If {@code valueType} is {@code Angle.class}, then the pattern syntax shall be as described in the
     *     {@link AngleFormat} class. This pattern may be used for any ordinate to be formatted as latitude or longitude,
     *     for example in {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal coordinate system}.</li>
     *   <li>If {@code valueType} is {@code Date.class}, then the pattern syntax shall be as described in the
     *     {@link SimpleDateFormat} class. This pattern may be used for any ordinate to be formatted as date and time,
     *     for example in {@linkplain org.apache.sis.referencing.cs.DefaultTimeCS time coordinate system}.</li>
     * </ul>
     *
     * @param  valueType  the base type of ordinate values to parse and format:
     *                    {@code Number.class}, {@code Angle.class} or {@code Date.class}.
     * @param  pattern    the pattern as specified in {@link DecimalFormat}, {@link AngleFormat}
     *                    or {@link SimpleDateFormat} javadoc.
     * @return {@code true} if the pattern has been applied, or {@code false} if {@code valueType} does not
     *         specify a known type or if the format associated to that type does not support patterns.
     * @throws IllegalArgumentException if the given pattern is invalid.
     */
    public boolean applyPattern(final Class<?> valueType, final String pattern) {
        ArgumentChecks.ensureNonNull("pattern", pattern);
        final Format format = getFormat(valueType);
        if (format instanceof DecimalFormat) {
            ((DecimalFormat) format).applyPattern(pattern);
        } else if (format instanceof SimpleDateFormat) {
            ((SimpleDateFormat) format).applyPattern(pattern);
        } else if (format instanceof AngleFormat) {
            ((AngleFormat) format).applyPattern(pattern);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Returns the base type of values parsed and formatted by this {@code Format} instance.
     *
     * @return {@code DirectPosition.class}.
     */
    @Override
    public final Class<DirectPosition> getValueType() {
        return DirectPosition.class;
    }

    /**
     * Formats the given coordinate.
     *
     * @param  position  the coordinate to format.
     * @return the formatted position.
     */
    public String format(final DirectPosition position) {
        if (buffer == null) {
            buffer = new StringBuffer();
        }
        buffer.setLength(0);
        try {
            format(position, buffer);
        } catch (IOException e) {
            /*
             * Should never happen when writing into a StringBuffer, unless the user override the
             * format(…) method. We do not rethrow an AssertionError because of this possibility.
             */
            throw new UncheckedIOException(e);
        }
        return buffer.toString();
    }

    /**
     * Formats the given coordinate and appends the resulting text to the given stream or buffer.
     *
     * @param  position    the coordinate to format.
     * @param  toAppendTo  where the text is to be appended.
     * @throws IOException if an error occurred while writing to the given appendable.
     */
    @Override
    @SuppressWarnings("UnnecessaryBoxing")
    public void format(final DirectPosition position, final Appendable toAppendTo) throws IOException {
        ArgumentChecks.ensureNonNull("position",   position);
        ArgumentChecks.ensureNonNull("toAppendTo", toAppendTo);
        CoordinateReferenceSystem crs = position.getCoordinateReferenceSystem();
        if (crs == null) {
            crs = defaultCRS;                           // May still be null.
        }
        if (crs != lastCRS) {
            initialize(crs);
        }
        /*
         * Standard java.text.Format API can only write into a StringBuffer. If the given Appendable is not a
         * StringBuffer, then we will need to format in a temporary buffer before to copy to the Appendable.
         */
        final StringBuffer destination;
        if (toAppendTo instanceof StringBuffer) {
            destination = (StringBuffer) toAppendTo;
        } else {
            if (buffer == null) {
                buffer = new StringBuffer();
            }
            destination = buffer;
            destination.setLength(0);
        }
        if (dummy == null) {
            dummy = new FieldPosition(0);
        }
        /*
         * The format to use for each ordinate has been computed by 'initialize'.  The format array length
         * should match the number of dimensions in the given position if the DirectPosition is consistent
         * with its CRS, but we will nevertheless verify has a paranoiac check.  If there is no CRS, or if
         * the DirectPosition dimension is (illegally) greater than the CRS dimension, then we will format
         * the ordinate as a number.
         */
        final int dimension = position.getDimension();
        for (int i=0; i < dimension; i++) {
            double value = position.getOrdinate(i);
            final Object object;
            final Format f;
            if (formats != null && i < formats.length) {
                f = formats[i];
                if (isNegative(i)) {
                    value = -value;
                }
                if (toFormatUnit != null) {
                    final UnitConverter c = toFormatUnit[i];
                    if (c != null) {
                        value = c.convert(value);
                    }
                }
                switch (types[i]) {
                    default:        object = Double.valueOf(value); break;
                    case LONGITUDE: object = new Longitude (value); break;
                    case LATITUDE:  object = new Latitude  (value); break;
                    case ANGLE:     object = new Angle     (value); break;
                    case DATE:      object = new Date(Math.round(value) + epochs[i]); break;
                }
            } else {
                object = value;
                f = getFormat(Number.class);
            }
            /*
             * At this point we got the value to format together with the Format instance to use.
             */
            if (i != 0) {
                toAppendTo.append(separator);
            }
            if (f.format(object, destination, dummy) != toAppendTo) {
                toAppendTo.append(destination);
                destination.setLength(0);
            }
            if (unitSymbols != null && i < unitSymbols.length) {
                final String symbol = unitSymbols[i];
                if (symbol != null) {
                    toAppendTo.append(Characters.NO_BREAK_SPACE).append(symbol);
                }
            }
        }
    }

    /**
     * Parses a coordinate from the given character sequence.
     * This method presumes that the coordinate reference system is the {@linkplain #getDefaultCRS() default CRS}.
     * The parsing begins at the {@linkplain ParsePosition#getIndex() index} given by the {@code pos} argument.
     * If parsing succeeds, then the {@code pos} index is updated to the index after the last ordinate value and
     * the parsed coordinate is returned. Otherwise (if parsing fails), the {@code pos} index is left unchanged,
     * the {@code pos} {@linkplain ParsePosition#getErrorIndex() error index} is set to the index of the first
     * unparsable character and an exception is thrown with a similar {@linkplain ParseException#getErrorOffset()
     * error index}.
     *
     * @param  text  the character sequence for the coordinate to parse.
     * @param  pos   the index where to start the parsing.
     * @return the parsed coordinate (never {@code null}).
     * @throws ParseException if an error occurred while parsing the coordinate.
     */
    @Override
    public DirectPosition parse(final CharSequence text, final ParsePosition pos) throws ParseException {
        ArgumentChecks.ensureNonNull("text", text);
        ArgumentChecks.ensureNonNull("pos",  pos);
        final int start  = pos.getIndex();
        final int length = text.length();
        /*
         * The NumberFormat, DateFormat and AngleFormat work only on String values, not on CharSequence.
         * If the given text is not a String, we will convert an arbitrarily small section of the given
         * text. Note that this will require to adjust the ParsePosition indices.
         */
        final int offset;
        final String asString;
        final ParsePosition subPos;
        if (text instanceof String) {
            offset   = 0;
            subPos   = pos;
            asString = (String) text;
        } else {
            offset   = start;
            subPos   = new ParsePosition(0);
            asString = text.subSequence(start, Math.min(start + READ_AHEAD_LIMIT, length)).toString();
        }
        /*
         * The Format instances to be used for each ordinate values is determined by the default CRS.
         * If no such CRS has been specified, then we will parse everything as plain numbers.
         */
        if (lastCRS != defaultCRS) {
            initialize(defaultCRS);
        }
        final double[] ordinates;
        Format format;
        final Format[] formats = this.formats;
        if (formats != null) {
            format    = null;
            ordinates = new double[formats.length];
        } else {
            format    = getFormat(Number.class);
            ordinates = new double[DEFAULT_DIMENSION];
        }
        /*
         * For each ordinate value except the first one, we need to skip the separator.
         * If we do not find the separator, we may consider that we reached the coordinate
         * end ahead of time. We currently allow that only for coordinate without CRS.
         */
        for (int i=0; i < ordinates.length; i++) {
            if (i != 0) {
                final int end = subPos.getIndex();
                int index = offset + end;
                while (!CharSequences.regionMatches(text, index, parseSeparator)) {
                    if (index < length) {
                        final int c = Character.codePointAt(text, index);
                        if (Character.isSpaceChar(c)) {
                            index += Character.charCount(c);
                            continue;
                        }
                    }
                    if (formats == null) {
                        pos.setIndex(index);
                        return new GeneralDirectPosition(Arrays.copyOf(ordinates, i));
                    }
                    pos.setIndex(start);
                    pos.setErrorIndex(index);
                    throw new LocalizedParseException(getLocale(), Errors.Keys.UnexpectedCharactersAfter_2,
                            new CharSequence[] {text.subSequence(start, end), CharSequences.token(text, index)}, index);
                }
                subPos.setIndex(index + parseSeparator.length() - offset);
            }
            /*
             * At this point 'subPos' is set to the beginning of the next ordinate to parse in 'asString'.
             * Parse the value as a number, angle or date, as determined from the coordinate system axis.
             */
            if (formats != null) {
                format = formats[i];
            }
            @SuppressWarnings("null")
            final Object object = format.parseObject(asString, subPos);
            if (object == null) {
                /*
                 * If we failed to parse, build an error message with the type that was expected for that ordinate.
                 * If the given CharSequence was not a String, we may need to update the error index since we tried
                 * to parse only a substring.
                 */
                Class<?> type = Number.class;
                if (types != null) {
                    switch (types[i]) {
                        case LONGITUDE: type = Longitude.class; break;
                        case LATITUDE:  type = Latitude.class;  break;
                        case ANGLE:     type = Angle.class;     break;
                        case DATE:      type = Date.class;      break;
                    }
                }
                pos.setIndex(start);
                if (subPos != pos) {
                    pos.setErrorIndex(offset + subPos.getErrorIndex());
                }
                throw new LocalizedParseException(getLocale(), type, text, pos);
            }
            double value;
            if (object instanceof Angle) {
                value = ((Angle) object).degrees();
            } else if (object instanceof Date) {
                value = ((Date) object).getTime() - epochs[i];
            } else {
                value = ((Number) object).doubleValue();
            }
            /*
             * The conversions and sign reversal applied below shall be in exact reverse order than
             * in the 'format(…)' method. However we have one additional step compared to format(…):
             * the unit written after the ordinate value may not be the same than the unit declared
             * in the CRS axis, so we have to parse the unit and convert the value before to apply
             * the reverse of 'format(…)' steps.
             */
            if (units != null) {
                final Unit<?> target = units[i];
                if (target != null) {
                    final int base = subPos.getIndex();
                    int index = base;
                    /*
                     * Skip whitespaces using Character.isSpaceChar(…), not Character.isWhitespace(…),
                     * because we need to skip also the non-breaking space (Characters.NO_BREAK_SPACE).
                     * If we can not parse the unit after those spaces, we will revert to the original
                     * position (absence of unit will not be considered an error).
                     */
                    while (index < asString.length()) {
                        final int c = asString.codePointAt(index);
                        if (Character.isSpaceChar(c)) {
                            index += Character.charCount(c);
                            continue;
                        }
                        subPos.setIndex(index);
                        final Object unit = getFormat(Unit.class).parseObject(asString, subPos);
                        if (unit == null) {
                            subPos.setIndex(base);
                            subPos.setErrorIndex(-1);
                        } else try {
                            value = ((Unit<?>) unit).getConverterToAny(target).convert(value);
                        } catch (IncommensurableException e) {
                            index += offset;
                            pos.setIndex(start);
                            pos.setErrorIndex(index);
                            throw (ParseException) new ParseException(e.getMessage(), index).initCause(e);
                        }
                        break;
                    }
                }
            }
            if (toFormatUnit != null) {
                final UnitConverter c = toFormatUnit[i];
                if (c != null) {
                    value = c.inverse().convert(value);
                }
            }
            if (isNegative(i)) {
                value = -value;
            }
            ordinates[i] = value;
        }
        final GeneralDirectPosition position = new GeneralDirectPosition(ordinates);
        position.setCoordinateReferenceSystem(defaultCRS);
        return position;
    }

    /**
     * Returns a clone of this format.
     *
     * @return a clone of this format.
     */
    @Override
    public CoordinateFormat clone() {
        final CoordinateFormat clone = (CoordinateFormat) super.clone();
        clone.dummy  = null;
        clone.buffer = null;
        Format[] cf = clone.formats;
        if (cf != null) {
            clone.formats = cf = cf.clone();
            for (int i=0; i < cf.length; i++) {
                cf[i] = (Format) cf[i].clone();
            }
        }
        return clone;
    }
}
