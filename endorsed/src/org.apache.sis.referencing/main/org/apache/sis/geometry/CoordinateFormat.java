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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.Quantity;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Time;
import javax.measure.quantity.Length;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.LocalizedParseException;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.QuantityFormat;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.io.CompoundFormat;
import org.apache.sis.pending.jdk.JDK23;


/**
 * Formats spatiotemporal coordinates using number, angle and date formats inferred from the coordinate system.
 * The format for each coordinate is inferred from the
 * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getUnit() coordinate system units}
 * using the following rules:
 *
 * <ul>
 *   <li>Coordinate values in angular units are formatted as angles using {@link AngleFormat}.</li>
 *   <li>Coordinate values in temporal units are formatted as dates using {@link DateFormat}.</li>
 *   <li>Other values are formatted as numbers using {@link NumberFormat} followed by the unit symbol
 *       formatted by {@link org.apache.sis.measure.UnitFormat}.</li>
 * </ul>
 *
 * The format can be controlled by invoking the {@link #applyPattern(Class, String)} public method,
 * or by overriding the {@link #createFormat(Class)} protected method.
 *
 * <h2>Coordinate reference system</h2>
 * {@code CoordinateFormat} uses the {@link DirectPosition#getCoordinateReferenceSystem()} value for determining
 * how to format each coordinate value. If the position does not specify a coordinate reference system, then the
 * {@linkplain #setDefaultCRS(CoordinateReferenceSystem) default CRS} is assumed. If no default CRS has been
 * specified, then all coordinates are formatted as decimal numbers.
 *
 * <p>{@code CoordinateFormat} does <strong>not</strong> transform the given coordinates in a unique CRS.
 * If the coordinates need to be formatted in a specific CRS, then the caller should
 * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform#transform(DirectPosition, DirectPosition)
 * transform the position} before to format it.</p>
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 1.4
 *
 * @see AngleFormat
 * @see org.apache.sis.measure.UnitFormat
 * @see GeneralDirectPosition
 *
 * @since 0.8
 */
public class CoordinateFormat extends CompoundFormat<DirectPosition> {
    /**
     * The logger for units of measurement.
     */
    private static final Logger LOGGER = Logger.getLogger(Loggers.MEASURE);

    /**
     * Serial number for cross-version compatibility.
     */
    private static final long serialVersionUID = 6633388113040644304L;

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
     * Units of measurement which are allowed to be automatically scaled to a larger unit.
     * For example if the unit of measurement of an axis is meter but the precision is 1000 metres,
     * then {@code CoordinateFormat} will automatically uses kilometres units instead of metres.
     */
    private static final Set<Unit<?>> SCALABLES = Set.of(Units.METRE, Units.PASCAL);

    /**
     * The separator between each coordinate values to be formatted.
     * The default value is a EM space space (U+2003).
     *
     * @see #getSeparator()
     * @see #setSeparator(String)
     */
    private String separator;

    /**
     * The separator without spaces, or an empty string if the separator contains only white spaces.
     * This is used at parsing time only.
     */
    private transient String parseSeparator;

    /**
     * The desired ground precision, or {@code null} if unspecified.
     * This precision may not apply to all axes. The "ground axes" dimensions
     * are identified by the bits set in the {@link #groundDimensions} bitmask.
     *
     * @see #groundDimensions
     * @see #setGroundPrecision(Quantity)
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private Quantity<?> groundPrecision;

    /**
     * The declared accuracy on ground, or {@code null} if unspecified. The accuracy applies to the same axes
     * than {@link #groundPrecision}. But contrarily to {@code groundPrecision}, the accuracy does not change
     * the number of fraction digits used by {@link NumberFormat}. Instead, it causes a text such as "± 30 m"
     * to be appended after the coordinates.
     *
     * @see #accuracyText
     * @see #groundDimensions
     * @see #accuracyThreshold
     * @see #setGroundAccuracy(Quantity)
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private Quantity<?> groundAccuracy;

    /**
     * Value of {@link #desiredPrecisions} which cause {@link #accuracyText} to be shown.
     * For each dimension identified by {@link #groundDimensions}, if the corresponding
     * value in {@link #desiredPrecisions} is equal or smaller to this threshold, then
     * {@link #accuracyText} will be appended after the formatted coordinates.
     *
     * @see #desiredPrecisions
     * @see #isAccuracyVisible
     */
    private transient double accuracyThreshold;

    /**
     * The dimensions on which {@link #groundPrecision} applies, specified as a bitmask.
     * This bitmask is computed by {@link #applyGroundPrecision(CoordinateReferenceSystem)}
     * when first needed. The current heuristic rules are:
     * <ul>
     *   <li>All axes having a {@link AxisDirections#isCompass(AxisDirection) compass direction}
     *       if at least one of those axes uses an unit of measurement compatible with the unit
     *       of {@link #groundPrecision} (possibly after conversion between linear and angular
     *       units on a sphere).</li>
     *   <li>Otherwise all axes using compatible units, regardless their direction and without
     *       conversion between linear and angular units.</li>
     * </ul>
     */
    private transient long groundDimensions;

    /**
     * The desired precisions for each coordinate, or {@code null} if unspecified.
     * The unit of measurement is given by {@link CoordinateSystemAxis#getUnit()}.
     * The length of this array does not need to be equal to the number of dimensions;
     * extraneous values are ignored and missing values are assumed equal to 0.
     * A value of 0 means to use the default precision for that dimension.
     *
     * <p>Note that this is the precision specified by the user, which may differ from
     * the precision returned by {@link #getPrecisions()}.</p>
     *
     * @see #setPrecisions(double...)
     * @see #getPrecisions()
     */
    private double[] desiredPrecisions;

    /**
     * Whether this {@code CoordinateFormat} instance has been configured for the precision and accuracy
     * specified by {@link #groundPrecision}, {@link #desiredPrecisions} and {@link #groundAccuracy}.
     * We use a field separated from {@link #lastCRS} because precision and accuracy threshold need
     * to be set only for formatting, not for parsing.
     *
     * @see #setPrecisions(double...)
     * @see #setGroundPrecision(Quantity)
     * @see #setGroundAccuracy(Quantity)
     * @see #configure(CoordinateReferenceSystem)
     */
    private transient boolean isPrecisionApplied;

    /**
     * Whether to append the accuracy after coordinate values. This flag is {@code true}
     * if {@link #accuracyText} is non-null and one of the following conditions is true:
     *
     * <ul>
     *   <li>{@link #desiredPrecisions} is null, in which case the accuracy is unconditionally shown.</li>
     *   <li>At least one {@link #desiredPrecisions} value is below {@link #accuracyThreshold}.</li>
     * </ul>
     *
     * This flag is valid only if {@link #isPrecisionApplied} is {@code true}.
     */
    private transient boolean isAccuracyVisible;

    /**
     * The coordinate reference system to assume if no CRS is attached to the position to format.
     * May be {@code null}.
     *
     * @see #setDefaultCRS(CoordinateReferenceSystem)
     */
    @SuppressWarnings("serial")                         // Most SIS implementations are serializable.
    private CoordinateReferenceSystem defaultCRS;

    /**
     * The coordinate reference system of the last {@link DirectPosition} that we parsed or formatted.
     * This is used for determining if we need to recompute all other transient fields in this class.
     *
     * @see #createFormats(CoordinateReferenceSystem)
     */
    private transient CoordinateReferenceSystem lastCRS;

    /**
     * Constants for the {@link #types} array.
     */
    private static final byte LONGITUDE=1, LATITUDE=2, ANGLE=3, DATE=4, TIME=5, INDEX=6;

    /**
     * The type for each value in the {@code formats} array, or {@code null} if not yet computed.
     * Types are: 0=number, 1=longitude, 2=latitude, 3=other angle, 4=date, 5=elapsed time, 6=index.
     *
     * <p>This array is created by {@link #createFormats(CoordinateReferenceSystem)}, which is invoked before
     * parsing or formatting in a different CRS than last operation, and stay unmodified after creation.</p>
     *
     * @see #createFormats(CoordinateReferenceSystem)
     */
    private transient byte[] types;

    /**
     * The format instances given by {@link #getFormat(Class)}, to use by default when we have
     * not been able to configure the precision. This is the same array as {@link #formats},
     * unless {@link #setPrecisions(double...)} has been invoked.
     * Values at different indices may reference the same {@link Format} instance.
     *
     * @see #createFormats(CoordinateReferenceSystem)
     */
    private transient Format[] sharedFormats;

    /**
     * The formats to use for formatting each coordinate value, or {@code null} if not yet computed.
     * The length of this array should be equal to the number of dimensions in {@link #lastCRS}.
     * Values at different indices may reference the same {@link Format} instance.
     *
     * @see #createFormats(CoordinateReferenceSystem)
     */
    private transient Format[] formats;

    /**
     * The units for each dimension to be formatted as a number with an unit of measurement.
     * We do not store {@link Unit} instances for dimensions to be formatted as angles or dates
     * because those quantities are formatted with specialized {@link Format} instances working
     * in fixed units; no unit symbol should appear after dates or DD°MM′SS″ angles.
     *
     * <p>We use this {@code units} array at parsing time for converting numbers from the units
     * of measurement in the parsed text to units expected by this {@code CoordinateFormat}.
     * Whether an element is non-null determines whether an unit symbol is allowed to appear
     * in the text to parse for the corresponding dimension.</p>
     *
     * <p>All non-null elements in this array are {@link CoordinateSystemAxis#getUnit()} return values.
     * This array is created by {@link #createFormats(CoordinateReferenceSystem)}, which is invoked before
     * parsing or formatting in a different CRS than last operation, and stay unmodified after creation.</p>
     *
     * @see #unitSymbolsUnscaled
     */
    private transient Unit<?>[] units;

    /**
     * Conversions from arbitrary units to the unit used by formatter, or {@code null} if none.
     * For example if coordinate at dimension <var>i</var> is formatted as an angle, then {@code toFormatUnit[i]}
     * is the conversion from angular axis units to decimal degrees before those degrees are formatted as DD°MM′SS″
     * with {@link AngleFormat}. Note that in this case, {@code units[i] == null} for telling that no unit symbol
     * should appear after the coordinate formatted in dimension <var>i</var> (because degree, minute and second
     * symbols are handled by {@link AngleFormat} instead).
     *
     * <p>In addition to conversions required by formatters expecting values in fixed units of measurement,
     * {@code toFormatUnit[i]} may also be non-null for some coordinates formatted as numbers if a different
     * unit of measurement is desired. For example, the converter may be non-null if some coordinates in metres
     * should be shown in kilometres. In those cases, {@code units[i] != null}.</p>
     *
     * <p>This array is used in slightly different ways at parsing time and formatting time. At formatting time,
     * coordinate values and unconditionally converted using all converters and the {@link #units} array is ignored.
     * At parsing time, {@code toFormatUnit[i]} converters are used only in dimensions <var>i</var> where the parser
     * requires a fixed unit which is implicit in the text ({@code units[i] == null}). For other dimensions accepting
     * various units ({@code units[i] != null}), the converter to use is determined by the unit of measurement written
     * in the text.</p>
     *
     * <p>This array is created by {@link #createFormats(CoordinateReferenceSystem)}, which is invoked before
     * parsing or formatting in a different CRS than last operation. It may be modified after creation as a
     * result of {@link #setPrecisions(double...)} calls, for example for replacing a "m" unit by "km".</p>
     *
     * @see #setConverter(int, int, UnitConverter)
     */
    private transient UnitConverter[] toFormatUnit;

    /**
     * Units symbols to append after coordinate values for each dimension, including leading space.
     * This is used only for coordinates to be formatted as ordinary numbers with {@link NumberFormat}.
     * This array is non-null only if at least one dimension needs to format its coordinates that way.
     *
     * <p>Units symbols may be followed by axis {@linkplain #directionSymbols direction symbols} used
     * for axes on the ground ("E", "N", "SW", <i>etc.</i>) so the complete symbol may be for example
     * "km E". Those direction symbols are stored in a separated array; they are not part of elements
     * of this {@code unitSymbols} array.</p>
     *
     * <p>This array is created by {@link #createFormats(CoordinateReferenceSystem)}, which is invoked before
     * parsing or formatting in a different CRS than last operation. It may be modified after creation as a
     * result of {@link #setPrecisions(double...)} calls, for example for replacing a "m" unit by "km".</p>
     */
    private transient String[] unitSymbols;

    /**
     * Same as {@link #unitSymbols} but without the changes applied by {@link #setPrecisions(double...)}.
     * This array is created by {@link #createFormats(CoordinateReferenceSystem)}, which is invoked before
     * parsing or formatting in a different CRS than last operation, and stay unmodified after creation.
     *
     * @see #units
     */
    private transient String[] unitSymbolsUnscaled;

    /**
     * Directions symbols ("E", "N", "SW", <i>etc.</i>) to append after coordinate values for some dimensions,
     * including leading space. This is used only for some coordinates formatted with {@link NumberFormat}.
     * This array is non-null only if at least one dimension needs to format its coordinates that way.
     * The length of this array is twice the number of dimensions. The array contains this tuple:
     *
     * <ol>
     *   <li>Symbol of axis direction (at even indices)</li>
     *   <li>Symbol in the direction opposite to axis direction (at odd indices)</li>
     * </ol>
     *
     * <p>This array is created by {@link #createFormats(CoordinateReferenceSystem)}, which is invoked before
     * parsing or formatting in a different CRS than last operation, and stay unmodified after creation.</p>
     */
    private transient String[] directionSymbols;

    /**
     * Text to append to the coordinate values for giving an indication about accuracy, or {@code null} if none.
     * Example: " ± 1 m" (note the leading space). This is determined by the {@link #groundAccuracy} value.
     * If {@link #desiredPrecisions} array is non-null, then accuracy is shown only if a precision is smaller.
     *
     * @see #groundAccuracy
     * @see #accuracyThreshold
     * @see #setGroundAccuracy(Quantity)
     */
    private transient String accuracyText;

    /**
     * Flags the coordinate values that need to be inverted before to be formatted.
     * This is needed for example if the axis is oriented toward past instead of future,
     * or toward west instead of east.
     *
     * @see #negate(int)
     */
    private transient long negate;

    /**
     * The time epochs. Non-null only if at least one coordinate is to be formatted as a date.
     *
     * <p>This array is created by {@link #createFormats(CoordinateReferenceSystem)}, which is invoked before
     * parsing or formatting in a different CRS than last operation, and stay unmodified after creation.</p>
     */
    private transient Instant[] epochs;

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
        separator = "\u2003";       // EM space.
        parseSeparator = "";
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
        parseSeparator = separator.strip();
    }

    /**
     * Returns the coordinate reference system to use if no CRS is explicitly associated to a given {@code DirectPosition}.
     * This CRS determines the type of format to use for each coordinate (number, angle or date) and the number of fraction
     * digits to use for achieving a {@linkplain #setGroundPrecision(Quantity) specified precision on ground}.
     *
     * @return the default coordinate reference system, or {@code null} if none.
     */
    public CoordinateReferenceSystem getDefaultCRS() {
        return defaultCRS;
    }

    /**
     * Sets the coordinate reference system to use if no CRS is explicitly associated to a given {@code DirectPosition}.
     * This CRS is only a default; positions given in another CRS are <strong>not</strong> automatically transformed to
     * that CRS before formatting.
     *
     * @param  crs  the default coordinate reference system, or {@code null} if none.
     */
    public void setDefaultCRS(final CoordinateReferenceSystem crs) {
        isPrecisionApplied &= (crs == defaultCRS);
        defaultCRS = crs;
    }

    /**
     * Computes the values of transient fields from the given CRS. The {@link #lastCRS} field is set to the given CRS
     * for allowing callers to check if this method needs to be invoked again (this method does not check by itself).
     * This method does not configure the formats for precisions specified by {@link #setPrecisions(double...)} and
     * related methods; that work is done by {@link #configure(CoordinateReferenceSystem)} at formatting time
     * (it is not needed at parsing time).
     *
     * @param  crs  the CRS for which to create the {@link Format} instances.
     *
     * @see #configure(CoordinateReferenceSystem)
     */
    private void createFormats(final CoordinateReferenceSystem crs) {
        types               = null;
        formats             = null;
        sharedFormats       = null;
        units               = null;
        toFormatUnit        = null;
        unitSymbols         = null;
        unitSymbolsUnscaled = null;
        directionSymbols    = null;
        epochs              = null;
        negate              = 0L;
        lastCRS             = crs;
        isPrecisionApplied  = false;
        /*
         * If no CRS were specified, we will format everything as numbers. Working with null CRS
         * is sometimes useful because null CRS are allowed in DirectPosition according ISO 19107.
         * Note that the caller may have replaced `crs` by `defaultCRS` if the CRS was null.
         */
        if (crs == null) {
            return;
        }
        final CoordinateSystem cs = crs.getCoordinateSystem();
        if (cs == null) {
            return;                                    // Paranoiac check (should never be null).
        }
        /*
         * Otherwise (if a CRS is given), infer the format subclasses from the axes.
         * Prepare also related information such as the unit of measurement and the
         * axis direction ("E", "N", etc.) that may need to be formatted.
         * The loop handles the following cases:
         *
         *    - case 0: no axis         — use default NumberFormat
         *    - case 1: angular unit    — use AngleFormat
         *    - case 2: temporal unit   — use DateFormat unless no TemporalCRS is found
         *    - case 3: grid direction  — use NumberFormat configured for integers.
         *    - case 4: all other unit  — use NumberFormat + UnitFormat + [axis direction]
         */
        final int      dimension = cs.getDimension();
        final byte[]   types     = new byte  [dimension];
        final Format[] formats   = new Format[dimension];
        for (int i=0; i<dimension; i++) {
            final CoordinateSystemAxis axis = cs.getAxis(i);
            if (axis == null) {                                               // Paranoiac check.
                formats[i] = getDefaultFormat();
                continue;
            }
            final AxisDirection direction = axis.getDirection();
            final Unit<?> unit = axis.getUnit();
            if (Units.isAngular(unit)) {
                /*
                 * CASE 1: Formatter for angular units. Target unit is DEGREE_ANGLE.
                 * Type is LONGITUDE, LATITUDE or ANGLE depending on axis direction.
                 */
                byte type = ANGLE;
                if      (direction == AxisDirection.NORTH) {type = LATITUDE;}
                else if (direction == AxisDirection.EAST)  {type = LONGITUDE;}
                else if (direction == AxisDirection.SOUTH) {type = LATITUDE;  negate(i);}
                else if (direction == AxisDirection.WEST)  {type = LONGITUDE; negate(i);}
                types  [i] = type;
                formats[i] = getFormat(Angle.class);
                setConverter(dimension, i, unit.asType(javax.measure.quantity.Angle.class).getConverterTo(Units.DEGREE));
                continue;
            } else if (Units.isTemporal(unit)) {
                /*
                 * CASE 2: Formatter for temporal units. Target unit is MILLISECONDS.
                 * Type is DATE.
                 */
                final CoordinateReferenceSystem t = CRS.getComponentAt(crs, i, i+1);
                if (t instanceof TemporalCRS) {
                    if (epochs == null) {
                        epochs = new Instant[dimension];
                    }
                    types  [i] = DATE;
                    formats[i] = getFormat(Date.class);
                    epochs [i] = TemporalDate.toInstant(DefaultTemporalCRS.castOrCopy((TemporalCRS) t).getOrigin(), null);
                    setConverter(dimension, i, unit.asType(Time.class).getConverterTo(Units.SECOND));
                    if (direction == AxisDirection.PAST) {
                        negate(i);
                    }
                    continue;
                }
                types[i] = TIME;
                // Fallthrough: format as number (cannot compute epoch because no TemporalCRS found).
            } else if (AxisDirections.isGrid(direction) && (unit == null || Units.PIXEL.isCompatible(unit))) {
                /*
                 * CASE 3: Formatter for grid cell indices. Target unit is unity of pixels.
                 * Type is INDEX, a flag meaning to not set minimum/maximum fraction digits.
                 */
                types[i] = INDEX;
            }
            /*
             * CASE 4: Formatter for all other units. Do NOT set types[i] since it may have been set to
             * a non-zero value by previous case. If not, the default value (zero) is the one we want.
             */
            formats[i] = getFormat(types[i] == INDEX ? Long.class : Number.class);
            if (unit != null) {
                if (units == null) {
                    units = new Unit<?>[dimension];
                }
                units[i] = unit;
                final String symbol = getFormat(Unit.class).format(unit);
                if (!symbol.isEmpty()) {
                    if (unitSymbols == null) {
                        unitSymbols = unitSymbolsUnscaled = new String[dimension];
                    }
                    unitSymbols[i] = QuantityFormat.SEPARATOR + symbol;
                }
            }
            if (AxisDirections.isCompass(direction)) {
                if (directionSymbols == null) {
                    directionSymbols = new String[dimension * 2];
                }
                directionSymbols[i*2]     = symbol(direction);
                directionSymbols[i*2 + 1] = symbol(AxisDirections.opposite(direction));
            }
        }
        this.types    = types;
        this.formats  = formats;        // Assign only on success because no element can be null.
        sharedFormats = formats;        // `getFormatClone(int)` will separate arrays later if needed.
    }

    /**
     * Returns the symbol ("E", "N", "SW", <i>etc.</i>) for given axis direction.
     */
    private static String symbol(final AxisDirection direction) {
        // Following cast uses our knowledge of `camelCaseToAcronym` implementation.
        String id = direction.identifier();
        if (id == null) id = direction.name();
        return ((StringBuilder) CharSequences.camelCaseToAcronym(id))
                .insert(0, Characters.NO_BREAK_SPACE).toString();
    }

    /**
     * Returns a clone of the format at the specified dimension. Format instances are cloned only when first needed.
     * The clones are needed when we want to change the format pattern (number of fraction digits, <i>etc.</i>) for
     * only one dimension, without impacting other dimensions that may use the same format.
     */
    private Format getFormatClone(final int dim) {
        if (formats == sharedFormats) {
            formats = formats.clone();
        }
        Format format = formats[dim];
        if (format == sharedFormats[dim]) {
            formats[dim] = format = (Format) format.clone();
        }
        return format;
    }

    /**
     * The default format to use when no CRS or no axis information is available. The coordinate type
     * could be anything (a date, an angle, …), but since we have no information we assume a number.
     * This method is defined for making clearer when such fallback is used.
     */
    private Format getDefaultFormat() {
        return getFormat(Number.class);
    }

    /**
     * Sets at the given index a conversion from CRS units to units used by this formatter.
     *
     * @param  dimension  number of dimensions of the coordinate system.
     * @param  i          index of the dimension for which to set the converter.
     * @param  c          the converter to set at the given dimension.
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
     * Replaces the "m" or "Pa" units of measurement in the given dimension by "km" or "kPa" or other units.
     * This is invoked for modifying the format created by {@link #createFormats(CoordinateReferenceSystem)}
     * according the value given to {@link #setPrecisions(double...)}.
     *
     * <h4>Limitation</h4>
     * Current implementation assumes that there is only one scale factor allowed by {@code CoordinateFormat},
     * which is 1000. If a future SIS version allows different scale factors, then we would need to make the
     * {@code if (toFormatUnit[i] == null)} check more accurate in {@link #applyPrecision(int)} method.
     *
     * @param  i      index of the dimension for which to change the unit.
     * @param  unit   value of {@code units[dimension]}.
     * @parma  scale  scale factor to apply on the unit.
     */
    private <Q extends Quantity<Q>> void scaleUnit(final int i, final Unit<Q> unit) {
        if (toFormatUnit == null) {
            toFormatUnit = new UnitConverter[formats.length];
        }
        if (toFormatUnit[i] == null) {
            final Unit<Q> target = unit.multiply(1000);
            toFormatUnit[i] = unit.getConverterTo(target);
            if (unitSymbols == unitSymbolsUnscaled) {
                unitSymbols = unitSymbols.clone();
            }
            unitSymbols[i] = QuantityFormat.SEPARATOR + getFormat(Unit.class).format(target);
        } else {
            // Dimension already scaled, assuming we allow only one scale factor.
        }
    }

    /**
     * Remembers that coordinate values at the given dimension will need to have their sign reverted.
     */
    private void negate(final int dimension) {
        if (dimension >= Long.SIZE) {
            throw new ArithmeticException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, dimension + 1));
        }
        negate |= (1L << dimension);
    }

    /**
     * Returns {@code true} if the value at the given dimension needs to have its sign reversed.
     */
    private boolean isNegative(final int dimension) {
        return (negate & Numerics.bitmask(dimension)) != 0;
    }

    /**
     * Returns the precisions at which coordinate values are formatted in each dimension.
     * For example if coordinates in dimension <var>i</var> are formatted with two fraction digits,
     * then the precision reported in {@code precisions[i]} will be 0.01. If the precision cannot
     * be determined for some dimensions, the corresponding values in the returned array will be 0.
     *
     * <p>The values returned by this method are not necessarily equal to the values specified in the last
     * call to {@link #setPrecisions(double...)}. For example if a precision of 0.03 has been requested for
     * a dimension whose coordinates are formatted as decimal numbers, then the actual precision returned
     * by this method for that dimension will be 0.01.</p>
     *
     * @return precision of coordinate values in each dimension (may contain 0 values for unknown precisions).
     *
     * @see AngleFormat#getPrecision()
     * @see DecimalFormat#getMaximumFractionDigits()
     *
     * @since 1.1
     */
    public double[] getPrecisions() {
        configure(defaultCRS);
        Format[] cf = formats;
        if (cf == null) {
            cf = new Format[DEFAULT_DIMENSION];
            Arrays.fill(cf, getDefaultFormat());
        }
        final double[] precisions = new double[cf.length];
        for (int i=0; i<precisions.length; i++) {
            final Format f = cf[i];
            if (f instanceof DecimalFormat) {
                /*
                 * Intentionally check the DecimalFormat subtype, not the more generic NumberFormat type,
                 * because the calculation below assumes base 10 and assumes that fraction digits are for
                 * fractions of 1 (by contrast, CompactNumberFormat may apply fraction to larger values).
                 */
                precisions[i] = MathFunctions.pow10(-((DecimalFormat) f).getMaximumFractionDigits());
            } else if (f instanceof AngleFormat) {
                precisions[i] = ((AngleFormat) f).getPrecision();
            }
        }
        return precisions;
    }

    /**
     * Sets the desired precisions at which to format coordinate values in each dimension.
     * For example if {@code precisions[i]} is 0.05, then coordinates in dimension <var>i</var>
     * will be shown with two fraction digits when formatted as decimal numbers, or with "D°MM"
     * pattern when formatted as angles.
     *
     * <p>This precision does not have a direct relationship to the precision on the ground.
     * For example, a precision of 0.01 could be one centimeter or 10 meters, depending if
     * the units of measurement in that dimension is meter or kilometer.
     * For a precision related to the ground, use {@link #setGroundPrecision(Quantity)} instead.</p>
     *
     * <p>If any value in the given array is 0 or {@link Double#NaN}, then there is a choice:
     * if {@link #setGroundPrecision(Quantity)} has been invoked, the precision specified to that
     * method will apply (if possible). Otherwise an implementation-specific default precision is used.
     * A typical use case is to use {@link #setGroundPrecision(Quantity)} for specifying an horizontal
     * precision in "real world" units and to use this {@code setPrecisions(double...)} method for adjusting
     * the precision of the vertical axis only.</p>
     *
     * @param  precisions  desired precision at which to format coordinate values in each dimension
     *                     (may have 0 or {@link Double#NaN} values for unspecified precisions in some
     *                     of those dimensions), or {@code null} for restoring the default values.
     *
     * @see AngleFormat#setPrecision(double, boolean)
     * @see DecimalFormat#setMaximumFractionDigits(int)
     *
     * @since 1.1
     */
    public void setPrecisions(final double... precisions) {
        /*
         * Implementation note: this method configures (indirectly through calls to `applyPrecision(int)`)
         * the formats given in the `formats` array but does not touch the `sharedFormats` array. This is
         * the opposite of `setGroundPrecision(…)` which performs a more global change that affect formats
         * in the `sharedFormats` array.
         */
        if (precisions == null) {
            desiredPrecisions = null;
            formats = sharedFormats;        // `getFormatClone(int)` will separate arrays later if needed.
        } else {
            if (desiredPrecisions == null || desiredPrecisions.length != precisions.length) {
                desiredPrecisions = new double[precisions.length];
                // Initial zero values mean "unspecified".
            }
            isPrecisionApplied &= (formats != null);
            for (int i=0; i<precisions.length; i++) {
                double p = Math.abs(precisions[i]);
                if (!(p < Double.POSITIVE_INFINITY)) p = 0;                 // Use ! for replacing NaN.
                if (desiredPrecisions[i] != (desiredPrecisions[i] = p)) {
                    // Precision changed. Keep format up to date.
                    if (isPrecisionApplied) {
                        applyPrecision(i);
                    }
                }
            }
        }
        updateAccuracyVisibility();
    }

    /**
     * Sets the pattern of the format for the specified dimension according the desired precision.
     * The format to configure is {@code formats[dim]} and the pattern will be constructed from the
     * {@code desiredPrecisions[dim]} value. Caller must ensure that the given dimension is valid
     * for both {@link #formats} and {@link #desiredPrecisions} arrays.
     */
    private void applyPrecision(final int dim) {
        final double precision = desiredPrecisions[dim];
        if (precision > 0) {
            final Format format = formats[dim];                 // Will be cloned below if needed.
            /*
             * Intentionally check the DecimalFormat subtype, not the more generic NumberFormat type,
             * because the calculation below assumes base 10 and assumes that fraction digits are for
             * fractions of 1 (by contrast, CompactNumberFormat may apply fraction to larger values).
             */
            if (format instanceof DecimalFormat && (types == null || types[dim] != INDEX)) {
                int digits = DecimalFunctions.fractionDigitsForDelta(precision, false);
                if (unitSymbols != null) {
                    /*
                     * The `units` array cannot be null if `unitSymbols` is non-null since unit symbols
                     * are inferred from Unit instances. For now we scale only a small set of known units,
                     * but more general scaling may be added in a future version.
                     */
                    final Unit<?> unit = units[dim];
                    if (SCALABLES.contains(unit)) {
                        if (precision >= 10) {          // If precision < 1000, we will use 1 or 2 fraction digits.
                            digits += 3;                // Because `scaleUnit(…)` scales by a factor 1000.
                            scaleUnit(dim, unit);
                        } else if (toFormatUnit != null) {
                            toFormatUnit[dim] = null;
                            unitSymbols[dim] = unitSymbolsUnscaled[dim];
                        }
                    }
                }
                digits = Math.max(digits, 0);
                final DecimalFormat nf = (DecimalFormat) getFormatClone(dim);
                nf.setMinimumFractionDigits(digits);
                nf.setMaximumFractionDigits(digits);
            } else if (format instanceof AngleFormat) {
                ((AngleFormat) getFormatClone(dim)).setPrecision(precision, true);
            }
        }
    }

    /**
     * Computes the values of transient fields from the given CRS and configure the format precisions.
     * This method updates the {@link #lastCRS} and {@link #isPrecisionApplied} fields.
     * This method does nothing if above-cited fields are already up to date.
     *
     * @param  crs  the CRS for which to create and configure the {@link Format} instances.
     *
     * @see #createFormats(CoordinateReferenceSystem)
     */
    private void configure(final CoordinateReferenceSystem crs) {
        if (lastCRS != crs) {
            createFormats(crs);                 // This method sets the `lastCRS` field.
        }
        if (!isPrecisionApplied) {
            if (groundPrecision != null) {
                applyGroundPrecision(crs);
            }
            if (desiredPrecisions != null) {
                if (sharedFormats == null) {
                    formats = sharedFormats = new Format[desiredPrecisions.length];
                    Arrays.fill(formats, getDefaultFormat());
                    types = new byte[formats.length];
                }
                final int n = Math.min(desiredPrecisions.length, formats.length);
                for (int i=0; i<n; i++) {
                    applyPrecision(i);          // Will clone Format instances if needed.
                }
            }
            applyGroundAccuracy(crs);
            updateAccuracyVisibility();
            isPrecisionApplied = true;
        }
    }

    /**
     * Adjusts the number of fraction digits to show in coordinates for achieving the given precision.
     * The {@link NumberFormat} and {@link AngleFormat} are configured for coordinates expressed in the
     * coordinate reference system of the position to format.
     *
     * The given resolution will be converted to the units used by coordinate system axes. For example if a 10 metres
     * resolution is specified but the {@linkplain #getDefaultCRS() default CRS} axes use kilometres, then this method
     * converts the resolution to 0.01 kilometre and uses that value for inferring that coordinates should be formatted
     * with 2 fraction digits. If the resolution is specified in an angular units such as degrees, this method uses the
     * {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius() ellipsoid authalic radius} for
     * computing an equivalent resolution in linear units. For example if the ellipsoid of default CRS is WGS84,
     * then this method considers a resolution of 1 second of angle as equivalent to a resolution of about 31 meters.
     * Conversions work also in the opposite direction (from linear to angular units) and are also used for choosing
     * which angle fields (degrees, minutes or seconds) to show.
     *
     * <p>If both {@link #setPrecisions(double...)} and {@code setGroundPrecision(Quantity)} are used,
     * then the values specified with {@code setPrecisions(…)} have precedence and this ground precision
     * is used only as a fallback. A typical use case is to specify the ground precision for horizontal
     * dimensions, then to specify a different precision <var>dz</var> for the vertical axis only with
     * {@code setPrecisions(NaN, NaN, dz)}.</p>
     *
     * @param  precision  the desired precision together with its linear or angular unit.
     *
     * @see DecimalFormat#setMaximumFractionDigits(int)
     * @see AngleFormat#setPrecision(double, boolean)
     * @see Quantities#create(double, Unit)
     *
     * @since 1.1
     */
    public void setGroundPrecision(final Quantity<?> precision) {
        groundPrecision = Objects.requireNonNull(precision);
        if (isPrecisionApplied) {
            applyGroundPrecision(lastCRS);
        }
    }

    /**
     * Specifies an uncertainty to append as "± <var>accuracy</var>" after the coordinate values.
     * If no {@linkplain #setPrecisions(double...) precisions} have been specified, the accuracy
     * will be always shown. But if precisions have been specified, then the accuracy will be
     * shown only if equals or greater than the precision.
     *
     * @param  accuracy  the accuracy to append after the coordinate values, or {@code null} if none.
     *
     * @see #getGroundAccuracy()
     * @see #getGroundAccuracyText()
     * @see Quantities#create(double, Unit)
     *
     * @since 1.1
     */
    public void setGroundAccuracy(final Quantity<?> accuracy) {
        accuracyText = null;
        groundAccuracy = accuracy;
        if (accuracy != null) {
            final NumberFormat nf = NumberFormat.getInstance(getLocale(Locale.Category.FORMAT));
            final QuantityFormat f = new QuantityFormat(nf, (UnitFormat) getFormat(Unit.class));
            if (buffer == null) buffer = new StringBuffer();
            buffer.setLength(0);
            accuracyText = f.format(accuracy, buffer.append("\u2003\u00B1\u00A0"), dummy).toString();
        }
        if (isPrecisionApplied) {
            applyGroundAccuracy(lastCRS);
            updateAccuracyVisibility();
        }
    }

    /**
     * Configures the formats for {@link #groundPrecision} value. Contrarily to {@link #applyPrecision(int)},
     * this method modifies the default formats provided by {@link #getFormat(Class)}. They are the formats
     * stored in the {@link #sharedFormats} array. Those formats are used as fallback when the {@link #formats}
     * array does not provide more specific format.
     *
     * <p>It is caller responsibility to ensure that {@link #groundPrecision} is non-null before to invoke this
     * method.</p>
     *
     * @param  crs  the target CRS in the conversion from ground units to CRS units.
     */
    private void applyGroundPrecision(final CoordinateReferenceSystem crs) {
        /*
         * If the given resolution is linear (for example in metres), compute an equivalent resolution in degrees
         * assuming a sphere of radius computed from the CRS.  Conversely if the resolution is angular (typically
         * in degrees), computes an equivalent linear resolution. For all other kind of units, do nothing.
         */
        final Resolution specified = new Resolution(groundPrecision);
        Resolution derived;
        IncommensurableException error;
        try {
            derived = specified.derived(crs);
            error   = null;
        } catch (IncommensurableException e) {      // Should not happen. If happen anyway, use `specified` only.
            derived = null;
            error   = e;
        }
        /*
         * We now have the requested resolution in both linear and angular units. Convert those resolutions
         * to the unit actually used by CRS axes.  If the units are not the same for all axes, use the unit
         * which result in the smallest resolution value after conversion. Current implementation considers
         * only compass directions (East, North, South-East, etc.) but we may revisit in the future.
         */
        groundDimensions     = 0L;
        boolean useSpecified = false;
        boolean useDerived   = false;
        final CoordinateSystem cs;
        if (crs != null && (cs = crs.getCoordinateSystem()) != null) {
            final int dimension = cs.getDimension();
            /*
             * The following loop will be executed exactly one or two times. The first execution checks
             * only axes having compass direction (East, North, South-East, etc.) and compatible units.
             * If no such axis is found, second execution checks all axes regardless their direction.
             */
            for (boolean useAllAxes = false; ; useAllAxes = true) {
                for (int i=0; i<dimension; i++) {
                    final CoordinateSystemAxis axis = cs.getAxis(i);
                    if (axis == null) continue;                         // Paranoiac check (should never be null).
                    final AxisDirection direction = axis.getDirection();
                    if (useAllAxes || AxisDirections.isCompass(direction)) {
                        specified.findMaxValue(axis);
                        final Unit<?> axisUnit = axis.getUnit();
                        if (axisUnit != null) try {
                            boolean done;
                            useSpecified |= (done = specified.findMinResolution(axisUnit, useSpecified));
                            if (!done && derived != null) {
                                useDerived |= (done = derived.findMinResolution(axisUnit, useDerived));
                            }
                            if (done) {
                                groundDimensions |= Numerics.bitmask(i);
                            }
                        } catch (IncommensurableException e) {
                            if (error == null) error = e;       // Should not happen. If happen anyway, skip axis.
                            else error.addSuppressed(e);
                        }
                    }
                }
                if (useSpecified | useDerived) {
                    break;
                }
                if (useAllAxes) {
                    useSpecified = true;
                    derived      = null;
                    break;
                }
            }
        }
        if (useSpecified) specified.setPrecision(this);
        if (useDerived)   derived  .setPrecision(this);
        if (error != null) {
            unexpectedException("setGroundPrecision", error);
        }
    }

    /**
     * Updates the {@link #accuracyThreshold} for the current {@link #groundAccuracy} value
     * (which may be null) and the given coordinate reference system.
     */
    private void applyGroundAccuracy(final CoordinateReferenceSystem crs) {
        long dimensions = groundDimensions;
abort:  if (dimensions != 0 && groundAccuracy != null) try {
            final Resolution specified = new Resolution(groundAccuracy);
            final Resolution derived   = specified.derived(crs);            // May be null.
            final CoordinateSystem cs  = crs.getCoordinateSystem();
            accuracyThreshold = 0;
            do {
                final int i = Long.numberOfTrailingZeros(dimensions);
                final Unit<?> unit = cs.getAxis(i).getUnit();
                final double accuracy;
                if (unit.isCompatible(specified.unit)) {
                    accuracy = specified.resolution(unit);
                } else if (derived != null && unit.isCompatible(derived.unit)) {
                    accuracy = derived.resolution(unit);
                } else {
                    break abort;
                }
                if (accuracy > accuracyThreshold) {
                    accuracyThreshold = accuracy;
                }
                dimensions &= ~(1L << i);
            } while (dimensions != 0);
            return;
        } catch (IncommensurableException e) {
            // Should not happen because `groundDimensions` bits were set only on successful axes.
            unexpectedException("setGroundAccuracy", e);
        }
        accuracyThreshold = Double.POSITIVE_INFINITY;
    }

    /**
     * Updates the {@link #isAccuracyVisible} flag according current values of {@link #accuracyText},
     * {@link #accuracyThreshold} and {@link #desiredPrecisions}.
     */
    private void updateAccuracyVisibility() {
        isAccuracyVisible = (accuracyText != null);
        if (isAccuracyVisible && desiredPrecisions != null) {
            long dimensions = groundDimensions & (Numerics.bitmask(desiredPrecisions.length) - 1);
            if (dimensions != 0) {
                isAccuracyVisible = false;
                do {
                    final int i = Long.numberOfTrailingZeros(dimensions);
                    final double precision = desiredPrecisions[i];
                    if (precision > 0 && precision <= accuracyThreshold) {
                        isAccuracyVisible = true;
                        break;
                    }
                    dimensions &= ~(1L << i);
                } while (dimensions != 0);
            }
        }
    }

    /**
     * Desired resolution in a given units, together with methods for converting to the units of a coordinate system axis.
     * This is a helper class for {@link CoordinateFormat#setGroundPrecision(Quantity)} implementation. An execution of
     * that method typically creates two instances of this {@code Resolution} class: one for the resolution in metres
     * and another one for the resolution in degrees.
     */
    private static final class Resolution {
        /** Maximal absolute value that we may format, regardless unit of measurement. */
        private double magnitude;

        /** The desired resolution in the unit of measurement given by {@link #unit}. */
        private double resolution;

        /** Unit of measurement of {@link #resolution}. */
        private Unit<?> unit;

        /** Whether {@link #unit} is an angular unit. */
        final boolean isAngular;

        /** Creates a new instance initialized to the given precision. */
        Resolution(final Quantity<?> groundPrecision) {
            resolution = Math.abs(groundPrecision.getValue().doubleValue());
            unit       = groundPrecision.getUnit();
            isAngular  = Units.isAngular(unit);
        }

        /**
         * Creates a new instance derived from the given angular or linear resolution.
         * This constructor computes an angular resolution from a linear one, or conversely.
         * If is caller responsibility to ensure that the specified resolution is either linear or angular.
         *
         * @param  specified  the linear or angular resolution specified by the user.
         * @param  radius     authalic radius of CRS ellipsoid.
         * @param  axisUnit   {@code radius} unit of measurement, which is also ellipsoid axes unit.
         * @throws IncommensurableException should not happen if {@code specified} is either linear or angular.
         */
        private Resolution(final Resolution specified, final double radius, final Unit<Length> axisUnit)
                throws IncommensurableException
        {
            isAngular = !specified.isAngular;
            if (isAngular) {
                // Angular resolution in radians  =  linear resolution  /  radius
                resolution = Math.toDegrees(specified.resolution(axisUnit) / radius);
                unit       = Units.DEGREE;
            } else {
                // Linear resolution  =  angular resolution in radians  ×  radius.
                resolution = specified.resolution(Units.RADIAN) * radius;
                unit       = axisUnit;
            }
        }

        /**
         * If this resolution is in metres, returns equivalent resolution in degrees. Or conversely if this resolution
         * is in degrees, returns an equivalent resolution in metres. Other linear and angular units are accepted too;
         * they will be converted as needed.
         *
         * @param  crs  the CRS for which to derive an equivalent resolution, or {@code null} if none.
         * @return the derived resolution, or {@code null} if none.
         * @throws IncommensurableException should never happen since this method verifies unit compatibility.
         */
        Resolution derived(final CoordinateReferenceSystem crs) throws IncommensurableException {
            if (isAngular || Units.isLinear(unit)) {
                final Ellipsoid ellipsoid = DatumOrEnsemble.getEllipsoid(crs).orElse(null);
                final double radius = Formulas.getAuthalicRadius(ellipsoid);
                if (radius > 0) {                                       // Indirectly filter null ellipsoid.
                    Unit<Length> axisUnit = ellipsoid.getAxisUnit();
                    if (axisUnit != null) {                             // Paranoiac check (should never be null).
                        return new Resolution(this, radius, axisUnit);
                    }
                }
            }
            return null;
        }

        /**
         * Returns the resolution converted to the specified unit as an absolute value.
         *
         * @throws IncommensurableException if the specified unit is not compatible with {@link #unit}.
         */
        private double resolution(final Unit<?> target) throws IncommensurableException {
            return Math.abs(unit.getConverterToAny(target).convert(resolution));
        }

        /**
         * Adjusts the resolution units for the given coordinate system axis. This methods select the units which
         * result in the smallest absolute value of {@link #resolution}.
         *
         * @param  axisUnit     {@link CoordinateSystemAxis#getUnit()}.
         * @param  hasPrevious  whether this method has been successfully applied on another axis before.
         * @return whether the given axis unit is compatible with the expected unit.
         * @throws IncommensurableException should never happen since this method verifies unit compatibility.
         */
        boolean findMinResolution(final Unit<?> axisUnit, final boolean hasPrevious) throws IncommensurableException {
            if (!axisUnit.isCompatible(unit)) {
                return false;
            }
            final double r = resolution(axisUnit);
            if (!hasPrevious || r < resolution) {
                resolution = r;                         // To units producing the smallest value.
                unit = axisUnit;
            }
            return true;
        }

        /**
         * Adjusts the maximal magnitude value, ignoring unit conversion. We do not apply unit conversion because
         * the axis minimum and maximum values are already in the units of the coordinates that will be formatted.
         * Even if different axes use different units, we want the largest value that {@link NumberFormat} may see.
         */
        final void findMaxValue(final CoordinateSystemAxis axis) {
            final double maxValue = Math.max(Math.abs(axis.getMinimumValue()),
                                             Math.abs(axis.getMaximumValue()));
            if (maxValue > magnitude) {
                magnitude = maxValue;
            }
        }

        /**
         * Configures the {@link NumberFormat} or {@link AngleFormat} for a number of fraction digits
         * sufficient for the given resolution. This method configures the shared formats returned by
         * {@link #getFormat(Class)}. They are the formats stored in the {@link #sharedFormats} array.
         */
        void setPrecision(final CoordinateFormat owner) {
            if (Units.isTemporal(unit)) {
                return;                         // Setting temporal resolution is not yet implemented.
            }
            final Format format = owner.getFormat(isAngular ? Angle.class : Number.class);
            if (format instanceof DecimalFormat) {
                /*
                 * Intentionally check the DecimalFormat subtype, not the more generic NumberFormat type,
                 * because the calculation below assumes base 10 and assumes that fraction digits are for
                 * fractions of 1 (by contrast, CompactNumberFormat may apply fraction to larger values).
                 */
                if (resolution == 0) resolution = 1E-6;                     // Arbitrary value.
                final int p = Math.max(0, DecimalFunctions.fractionDigitsForDelta(resolution, true));
                final int m = Math.max(0, DecimalFunctions.fractionDigitsForDelta(Math.ulp(magnitude), false));
                ((DecimalFormat) format).setMinimumFractionDigits(Math.min(p, m));
                ((DecimalFormat) format).setMaximumFractionDigits(p);
            } else if (format instanceof AngleFormat) {
                ((AngleFormat) format).setPrecision(resolution, true);
            }
        }
    }

    /**
     * Returns the current ground accuracy value, or {@code null} if none.
     * This is the value given to the last call to {@link #setGroundAccuracy(Quantity)}.
     *
     * @return the current ground accuracy value, or {@code null} if none.
     *
     * @see #setGroundAccuracy(Quantity)
     */
    public Quantity<?> getGroundAccuracy() {
        return groundAccuracy;
    }

    /**
     * Returns the textual representation of the current ground accuracy.
     * Example: " ± 3 m" (note the leading space).
     *
     * @return textual representation of current ground accuracy.
     *
     * @see #setGroundAccuracy(Quantity)
     */
    public Optional<String> getGroundAccuracyText() {
        return Optional.ofNullable(accuracyText);
    }

    /**
     * Returns the pattern for number, angle or date fields. The given {@code valueType} should be
     * {@code Number.class}, {@code Angle.class}, {@code Date.class} or a sub-type of the above.
     * This method may return {@code null} if the underlying format cannot provide a pattern.
     *
     * <table class="sis">
     *   <caption>Pattern availability for type of value</caption>
     *   <tr><th>Value type</th>     <th>Base format class</th>    <th>Format with pattern</th></tr>
     *   <tr><td>{@link Number}</td> <td>{@link NumberFormat}</td> <td>{@link DecimalFormat}</td></tr>
     *   <tr><td>{@link Angle}</td>  <td>{@link AngleFormat}</td>  <td>{@link AngleFormat}</td></tr>
     *   <tr><td>{@link Date}</td>   <td>{@link DateFormat}</td>   <td>{@link SimpleDateFormat}</td></tr>
     * </table>
     *
     * @param  valueType  the base type of coordinate values to parse and format:
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
     *     {@link DecimalFormat} class. This pattern may be used for any coordinate to be formatted as plain number,
     *     for example in {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian coordinate system}.</li>
     *   <li>If {@code valueType} is {@code Angle.class}, then the pattern syntax shall be as described in the
     *     {@link AngleFormat} class. This pattern may be used for any coordinate to be formatted as latitude or longitude,
     *     for example in {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal coordinate system}.</li>
     *   <li>If {@code valueType} is {@code Date.class}, then the pattern syntax shall be as described in the
     *     {@link SimpleDateFormat} class. This pattern may be used for any coordinate to be formatted as date and time,
     *     for example in {@linkplain org.apache.sis.referencing.cs.DefaultTimeCS time coordinate system}.</li>
     * </ul>
     *
     * @param  valueType  the base type of coordinate values to parse and format:
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
        formats = sharedFormats;            // For forcing an update of `formats` when needed.
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
     * Creates a new format to use for parsing and formatting values of the given type.
     * This method is invoked by {@link #getFormat(Class)} the first time that a format
     * is needed for the given type.
     *
     * <p>See {@linkplain CompoundFormat#createFormat(Class) super-class} for a description of recognized types.
     * This method override uses the short date pattern instead of the (longer) default one.</p>
     *
     * @param  valueType  the base type of values to parse or format.
     * @return the format to use for parsing of formatting values of the given type, or {@code null} if none.
     */
    @Override
    protected Format createFormat(final Class<?> valueType) {
        if (valueType == Date.class) {
            final Locale locale = super.getLocale();
            if (!Locale.ROOT.equals(locale)) {
                final DateFormat format;
                format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
                format.setTimeZone(getTimeZone());
                return format;
            }
        }
        return super.createFormat(valueType);
    }

    /**
     * Formats the given coordinate.
     * The type of each coordinate value (number, angle or date) is determined by the CRS of the given
     * position if such CRS is defined, or from the {@linkplain #getDefaultCRS() default CRS} otherwise.
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
     * The type of each coordinate value (number, angle or date) is determined by the CRS of the given
     * position if such CRS is defined, or from the {@linkplain #getDefaultCRS() default CRS} otherwise.
     *
     * @param  position    the coordinate to format.
     * @param  toAppendTo  where the text is to be appended.
     * @throws IOException if an error occurred while writing to the given appendable.
     * @throws ArithmeticException if a date value exceed the capacity of {@code long} type.
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
        /*
         * Configure the formatters for the desired precision, which can potentially change for each point.
         * Note that the formatters may not have been created if the CRS is null (because `createFormats(…)`
         * does not know which format to use), in which case generic number formats will be used.
         */
        configure(crs);
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
         * The format to use for each coordinate has been computed by `configure`. The format array length
         * should match the number of dimensions in the given position assuming that the DirectPosition is
         * consistent with its CRS. If there is no CRS, or if the DirectPosition dimension is (illegally)
         * greater than the CRS dimension, then we will format the coordinate as a plain number.
         */
        final int dimension = position.getDimension();
        for (int i=0; i < dimension; i++) {
            double value = position.getOrdinate(i);
            final Object valueObject;
            final String unit, direction;
            final Format f;
            if (formats != null && i < formats.length) {    // The < check is a safety against illegal DirectPosition.
                f = formats[i];
                unit = (unitSymbols != null) ? unitSymbols[i] : null;
                if (directionSymbols == null) {
                    direction = null;
                } else if (value < 0) {
                    value = -value;
                    direction = directionSymbols[i*2 + 1];
                } else {
                    direction = directionSymbols[i*2];
                }
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
                    default:        valueObject = Double.valueOf(value); break;
                    case INDEX:     valueObject = Math.round    (value); break;
                    case LONGITUDE: valueObject = new Longitude (value); break;
                    case LATITUDE:  valueObject = new Latitude  (value); break;
                    case ANGLE:     valueObject = new Angle     (value); break;
                    case DATE: {
                        if (Double.isFinite(value)) {
                            valueObject = TemporalDate.toDate(TemporalDate.addSeconds(epochs[i], value));
                        } else {
                            if (i != 0) toAppendTo.append(separator);
                            toAppendTo.append(String.valueOf(value));
                            continue;
                        }
                        break;
                    }
                }
            } else {
                valueObject = value;
                f = getDefaultFormat();
                unit = direction = null;
            }
            /*
             * At this point we got the value to format together with the Format instance to use.
             */
            if (i != 0) {
                toAppendTo.append(separator);
            }
            if (f.format(valueObject, destination, dummy) != toAppendTo) {
                toAppendTo.append(destination);
                destination.setLength(0);
            }
            if (unit      != null) toAppendTo.append(unit);
            if (direction != null) toAppendTo.append(direction);
        }
        /*
         * Finished to format the all coordinate values. Appends the accuracy if
         * there is one and if the precision is at least as small as the accuracy.
         */
        if (isAccuracyVisible) {
            toAppendTo.append(accuracyText);
        }
    }

    /**
     * Parses a coordinate from the given character sequence.
     * This method presumes that the coordinate reference system is the {@linkplain #getDefaultCRS() default CRS}.
     * The parsing begins at the {@linkplain ParsePosition#getIndex() index} given by the {@code pos} argument.
     * If parsing succeeds, then the {@code pos} index is updated to the index after the last coordinate value and
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
         * The Format instances to be used for each coordinate values is determined by the default CRS.
         * If no such CRS has been specified, then we will parse everything as plain numbers.
         */
        if (lastCRS != defaultCRS) {
            createFormats(defaultCRS);
        }
        final double[] coordinates;
        Format format;
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Format[] formats = this.formats;
        if (formats != null) {
            format      = null;
            coordinates = new double[formats.length];
        } else {
            format      = getDefaultFormat();
            coordinates = new double[DEFAULT_DIMENSION];
        }
        /*
         * For each coordinate value except the first one, we need to skip the separator.
         * If we do not find the separator, we may consider that we reached the coordinate
         * end ahead of time. We currently allow that only for coordinate without CRS.
         */
        for (int i=0; i < coordinates.length; i++) {
skipSep:    if (i != 0) {
                final int end = subPos.getIndex();          // End of previous coordinate.
                int index = offset + end;
                while (index < length) {
                    if (parseSeparator.isEmpty()) {
                        final int next = CharSequences.skipLeadingWhitespaces(text, index, length);
                        if (next > index) {
                            subPos.setIndex(next - offset);
                            break skipSep;
                        }
                    } else {
                        if (CharSequences.regionMatches(text, index, parseSeparator)) {
                            subPos.setIndex(index + parseSeparator.length() - offset);
                            break skipSep;
                        }
                    }
                    final int c = Character.codePointAt(text, index);
                    if (!Character.isSpaceChar(c)) break;
                    index += Character.charCount(c);
                }
                /*
                 * No separator found. If no CRS was specified (in which case we don't know how many coordinates
                 * were expected), then stop parsing and return whatever number of coordinates we got. Otherwise
                 * (another coordinate was expected) consider we have a too short string or unexpected characters.
                 */
                if (formats == null) {
                    pos.setIndex(index);
                    return new GeneralDirectPosition(Arrays.copyOf(coordinates, i));
                }
                pos.setIndex(start);
                pos.setErrorIndex(index);
                final CharSequence previous = text.subSequence(start, end);
                final CharSequence found = CharSequences.token(text, index);
                final short key;
                final CharSequence[] args;
                if (found.length() != 0) {
                    key = Errors.Keys.UnexpectedCharactersAfter_2;
                    args = new CharSequence[] {previous, found};
                } else {
                    key = Errors.Keys.UnexpectedEndOfString_1;
                    args = new CharSequence[] {previous};
                }
                throw new LocalizedParseException(getLocale(), key, args, index);
            }
            /*
             * At this point `subPos` is set to the beginning of the next coordinate to parse in `asString`.
             * Parse the value as a number, angle or date, as determined from the coordinate system axis.
             */
            if (formats != null) {
                format = formats[i];
            }
            @SuppressWarnings("null")       // `format` was initially null only if `formats` is non-null.
            final Object object = format.parseObject(asString, subPos);
            if (object == null) {
                /*
                 * If we failed to parse, build an error message with the type that was expected for that coordinate.
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
                        case INDEX:     type = Long.class;      break;
                    }
                }
                pos.setIndex(start);
                if (subPos != pos) {
                    pos.setErrorIndex(offset + subPos.getErrorIndex());
                }
                throw new LocalizedParseException(getLocale(), type, text, pos);
            }
            /*
             * The value part (number, angle or date) has been parsed successfully.
             * Get the numerical value. The unit of measurement may not be the same
             * than the one expected by the CRS (we will convert later).
             */
            double value;
            if (object instanceof Angle) {
                value = ((Angle) object).degrees();
            } else if (object instanceof Date) {
                final Duration d = JDK23.until(epochs[i], ((Date) object).toInstant());
                value = d.getSeconds() + (d.getNano() / (double) Constants.NANOS_PER_SECOND);
            } else {
                value = ((Number) object).doubleValue();
            }
            /*
             * The value sign may need to be adjusted if the value is followed by a direction symbol
             * such as "N", "E" or "SW". Get the symbols that are allowed for current coordinate.
             * We will check for their presence after the unit symbol, or immediately after the value
             * if there is no unit symbol.
             */
            String direction = null;
            String opposite  = null;
            if (directionSymbols != null) {
                direction = directionSymbols[i*2    ];
                opposite  = directionSymbols[i*2 + 1];
            }
            /*
             * The unit written after the coordinate value may not be the same as the unit declared
             * in the CRS axis, so we have to parse the unit and convert the value before to apply the
             * change of sign.
             */
            final Unit<?> target;
            UnitConverter toCRS = null;
parseUnit:  if (units != null && (target = units[i]) != null) {
                final int base = subPos.getIndex();
                int index = base;                       // Will become start index of unit symbol.
                /*
                 * Skip whitespaces using Character.isSpaceChar(…), not Character.isWhitespace(…),
                 * because we need to skip also the non-breaking space (Characters.NO_BREAK_SPACE).
                 * If we cannot parse the unit after those spaces, we will revert to the original
                 * position + spaces skipped (absence of unit will not be considered an error).
                 */
                int c;
                for (;;) {
                    if (index >= asString.length()) {
                        break parseUnit;                // Found only spaces until end of string.
                    }
                    c = asString.codePointAt(index);
                    if (!Character.isSpaceChar(c)) break;
                    index += Character.charCount(c);
                }
                /*
                 * Now the `index` should be positioned on the first character of the unit symbol.
                 * Before to parse the unit, verify if a direction symbol is found after the unit.
                 * We need to do this check because unit symbol and direction symbol are separated
                 * by a no-break space, which causes `UnitFormat` to try to parse them together as
                 * a unique unit symbol.
                 */
                int stopAt = index;                     // Will become stop index of unit symbol.
                int nextAt = -1;                        // Will become start index of next coordinate.
checkDirection: if (direction != null) {
                    do {
                        stopAt += Character.charCount(c);
                        if (stopAt >= asString.length()) {
                            break checkDirection;
                        }
                        c = asString.codePointAt(stopAt);
                    } while (!Character.isSpaceChar(c));
                    /*
                     * Found the first space character, which may be a no-break space.
                     * Check for direction symbol here. This strategy is based on the
                     * fact that the direction symbol starts with a no-break space.
                     */
                    if (asString.regionMatches(true, stopAt, direction, 0, direction.length())) {
                        nextAt = stopAt + direction.length();
                    } else if (asString.regionMatches(true, stopAt, opposite, 0, opposite.length())) {
                        nextAt = stopAt + opposite.length();
                        value = -value;
                    }
                }
                /*
                 * Parse the unit symbol now. The `nextAt` value determines whether a direction symbol
                 * has been found, in which case we need to exclude the direction from the text parsed
                 * by `UnitFormat`.
                 */
                final Format f = getFormat(Unit.class);
                final Object unit;
                try {
                    if (nextAt < 0) {
                        subPos.setIndex(index);
                        unit = f.parseObject(asString, subPos);     // Let `UnitFormat` decide where to stop parsing.
                    } else {
                        unit = f.parseObject(asString.substring(index, stopAt));
                        subPos.setIndex(nextAt);
                        direction = opposite = null;
                    }
                    if (unit == null) {
                        subPos.setIndex(base);
                        subPos.setErrorIndex(-1);
                    } else {
                        toCRS = ((Unit<?>) unit).getConverterToAny(target);
                    }
                } catch (ParseException | IncommensurableException e) {
                    index += offset;
                    pos.setIndex(start);
                    pos.setErrorIndex(index);
                    if (e instanceof ParseException) {
                        throw (ParseException) e;
                    }
                    throw (ParseException) new ParseException(e.getMessage(), index).initCause(e);
                }
            } else {
                /*
                 * If we reach this point, the format at dimension `i` uses an implicit unit of measurement
                 * such as degrees for `AngleFormat` or milliseconds for `DateFormat`. Only for those cases
                 * (identified by `units[i] == null`), use the conversion declared in `toFormatUnit` array.
                 */
                if (toFormatUnit != null) {
                    toCRS = toFormatUnit[i];
                    if (toCRS != null) {
                        toCRS = toCRS.inverse();
                    }
                }
            }
            /*
             * At this point either the unit of measurement has been parsed, or there is no unit.
             * If the direction symbol ("E", "N", "SW", etc.) has not been found before, check now.
             */
            if (direction != null) {
                int index = subPos.getIndex();
                if (asString.regionMatches(true, index, direction, 0, direction.length())) {
                    index += direction.length();
                } else if (asString.regionMatches(true, index, opposite, 0, opposite.length())) {
                    index += opposite.length();
                    value = -value;
                }
                subPos.setIndex(index);
            }
            /*
             * The conversions and sign reversal applied below shall be in reverse order
             * than the operations applied by the `format(…)` method.
             */
            if (toCRS != null) {
                value = toCRS.convert(value);
            }
            if (isNegative(i)) {
                value = -value;
            }
            coordinates[i] = value;
        }
        /*
         * If accuracy information is appended after the coordinates (e.g. " ± 3 km"), skip that text.
         */
        if (accuracyText != null) {
            final int index = subPos.getIndex();
            final int lg = accuracyText.length();
            if (asString.regionMatches(true, index, accuracyText, 0, lg)) {
                subPos.setIndex(index + lg);
            }
        }
        final GeneralDirectPosition position = new GeneralDirectPosition(coordinates);
        position.setCoordinateReferenceSystem(defaultCRS);
        return position;
    }

    /**
     * Invoked when an unexpected error occurred but continuation is still possible.
     * This method is invoked in the context of units of measurement.
     *
     * @param  method  the public method to report as the source of the log record.
     * @param  error   the error that occurred.
     */
    private static void unexpectedException(final String method, final Exception error) {
        Logging.unexpectedException(LOGGER, CoordinateFormat.class, method, error);
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
        clone.createFormats(null);
        if (desiredPrecisions != null) {
            clone.desiredPrecisions = desiredPrecisions.clone();
        }
        return clone;
    }

    /**
     * Invoked on deserialization for restoring some transient fields.
     *
     * @param  in  the input stream from which to deserialize a coordinate format
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the module path.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        parseSeparator = separator.strip();
    }
}
