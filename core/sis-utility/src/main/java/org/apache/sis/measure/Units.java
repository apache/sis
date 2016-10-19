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

import java.util.Map;
import java.util.HashMap;
import javax.measure.Dimension;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.ParserException;
import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Area;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Force;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Power;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Time;
import javax.measure.quantity.Volume;

import org.apache.sis.util.Static;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Constants;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static org.apache.sis.measure.SexagesimalConverter.EPS;


/**
 * Provides constants for various Units of Measurement together with static methods working on {@link Unit} instances.
 * This class focuses on the most commonly used units in the geospatial domain:
 * angular units ({@linkplain #DEGREE degree}, {@linkplain #ARC_SECOND arc-second}, …),
 * linear units ({@linkplain #KILOMETRE kilometre}, {@linkplain #NAUTICAL_MILE nautical mile}, …) and
 * temporal units ({@linkplain #DAY day}, {@linkplain #TROPICAL_YEAR year}, …).
 * But some other kind of units are also provided for completeness.
 *
 * <p>All Units of Measurement are based on units from the International System (SI).
 * The fundamental units are listed below, together with some dimensionless units:</p>
 *
 * <table class="sis">
 *   <caption>SI fundamental units and dimensionless units</caption>
 *   <tr><th>Quantity type</th>       <th>Dimension symbol</th> <th>Base unit</th></tr>
 *   <tr><td>{@link Length}</td>            <td>L</td>          <td>{@link #METRE}</td></tr>
 *   <tr><td>{@link Mass}</td>              <td>M</td>          <td>{@link #KILOGRAM}</td></tr>
 *   <tr><td>{@link Time}</td>              <td>T</td>          <td>{@link #SECOND}</td></tr>
 *   <tr><td>{@link ElectricCurrent}</td>   <td>I</td>          <td>{@link #AMPERE}</td></tr>
 *   <tr><td>{@link Temperature}</td>       <td>Θ</td>          <td>{@link #KELVIN}</td></tr>
 *   <tr><td>{@link AmountOfSubstance}</td> <td>N</td>          <td>{@link #MOLE}</td></tr>
 *   <tr><td>{@link LuminousIntensity}</td> <td>J</td>          <td>{@link #CANDELA}</td></tr>
 *   <tr><td>{@link Angle}</td>             <td></td>           <td>{@link #RADIAN}</td></tr>
 * </table>
 *
 * Unit names and definitions in this class follow the definitions provided in the EPSG geodetic dataset
 * (when the unit exists in that dataset),
 * except “year” which has been renamed “{@linkplain #TROPICAL_YEAR tropical year}”.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
public final class Units extends Static {
    /**
     * The units for given {@link UnitDimension} or {@code Class<Quantity>} instances.
     * This map contains mostly SI units (no imperial units) with the addition of some alternative units.
     * This map must be unmodified after it has been populated.
     */
    private static final Map<Object, SystemUnit<?>> SYSTEM = new HashMap<>();

    /**
     * Returns the system unit for the given quantity, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    static <Q extends Quantity<Q>> SystemUnit<Q> get(final Class<Q> type) {
        return (SystemUnit<Q>) SYSTEM.get(type);
    }

    /**
     * Returns the system unit for the given dimension, or {@code null} if none.
     * Note that this method can not distinguish the different kinds of dimensionless units.
     * If the quantity type is known, use {@link #get(Class)} instead.
     */
    static SystemUnit<?> get(final Dimension dim) {
        return SYSTEM.get(dim);
    }

    /**
     * Unit of measurement defined as 10<sup>-9</sup> metres (1 nm). This unit is often used in
     * {@linkplain org.apache.sis.metadata.iso.content.DefaultBand#getBoundUnits() wavelength measurements}.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE}
     * and the unlocalized name is "nanometre".
     *
     * @see #CENTIMETRE
     * @see #MILLIMETRE
     *
     * @since 0.8
     */
    public static final Unit<Length> NANOMETRE;

    /**
     * Unit of measurement defined as 0.001 metres (1 mm).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “millimetre” and the identifier is EPSG:1025.
     *
     * @see #NANOMETRE
     * @see #CENTIMETRE
     * @see #KILOMETRE
     *
     * @since 0.8
     */
    public static final Unit<Length> MILLIMETRE;

    /**
     * Unit of measurement defined as 0.01 metres (1 cm).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “centimetre” and the identifier is EPSG:1033.
     *
     * @see #NANOMETRE
     * @see #MILLIMETRE
     * @see #KILOMETRE
     *
     * @since 0.8
     */
    public static final Unit<Length> CENTIMETRE;

    /**
     * The SI base unit for distances (m).
     * The unlocalized name is “metre” and the identifier is EPSG:9001.
     * This is the base of all other {@linkplain #isLinear(Unit) linear} units:
     *
     * {@link #NANOMETRE} (nm),
     * {@link #MILLIMETRE} (mm),
     * {@link #CENTIMETRE} (cm),
     * {@link #KILOMETRE} (km),
     * {@link #NAUTICAL_MILE} (M),
     * {@link #STATUTE_MILE} (mi),
     * {@link #US_SURVEY_FOOT},
     * {@link #FOOT} (ft),
     * {@link #INCH} (in) and
     * {@link #POINT} (pt)
     * among others.
     *
     * @see #SQUARE_METRE
     * @see #CUBIC_METRE
     *
     * @since 0.8
     */
    public static final Unit<Length> METRE;

    /**
     * Unit of measurement defined as 1000 metres (1 km).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “kilometre” and the identifier is EPSG:9036.
     *
     * @see #STATUTE_MILE
     * @see #NAUTICAL_MILE
     *
     * @since 0.8
     */
    public static final Unit<Length> KILOMETRE;

    /**
     * Unit of measurement defined as exactly 1852 metres (1 M).
     * This is approximatively the distance between two parallels of latitude
     * separated by one {@linkplain #ARC_MINUTE arc-minute}.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “nautical mile” and the identifier is EPSG:9030.
     *
     * <p>There is no internationally agreed symbol for nautical mile. Apache SIS uses “M” in agreement with the
     * International Hydrographic Organization (IHO) and the International Bureau of Weights and Measures (BIPM).
     * But “NM” and “nmi” are also in use.</p>
     *
     * @see #STATUTE_MILE
     * @see #KILOMETRE
     * @see #ARC_MINUTE
     *
     * @since 0.8
     */
    public static final Unit<Length> NAUTICAL_MILE;

    /**
     * Unit of measurement defined as exactly 1609.344 metres (1 mi).
     * This unit is often named “mile” without qualifier, but Apache SIS uses “statute mile”
     * for emphasing the difference with {@linkplain #NAUTICAL_MILE nautical mile}.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE}.
     * The unlocalized name is “statute mile” but is localized as "international mile" in the US
     * for avoiding confusion with the US survey mile.
     * The identifier is EPSG:9093.
     *
     * @see #KILOMETRE
     * @see #NAUTICAL_MILE
     * @see #FOOT
     *
     * @since 0.8
     */
    public static final Unit<Length> STATUTE_MILE;

    /**
     * Unit of measurement approximatively equals to 0.3048006096… metres.
     * The legal definition is exactly 12/39.37 metres.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “US survey foot” and the identifier is EPSG:9003.
     *
     * @see #FOOT
     *
     * @since 0.8
     */
    public static final Unit<Length> US_SURVEY_FOOT;

    /**
     * Unit of measurement defined as exactly 0.3048 metres (1 ft).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “foot” and the identifier is EPSG:9002.
     *
     * @see #US_SURVEY_FOOT
     * @see #STATUTE_MILE
     * @see #INCH
     *
     * @since 0.8
     */
    public static final Unit<Length> FOOT;

    /**
     * Unit of measurement defined as 2.54 centimetres (1 in).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE}
     * and the unlocalized name is “inch”.
     *
     * @see #CENTIMETRE
     * @see #POINT
     * @see #FOOT
     *
     * @since 0.8
     */
    public static final Unit<Length> INCH;

    /**
     * Unit of measurement defined as 0.013837 inch (1 pt).
     * This is commonly used to measure the height of a font.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE}
     * and the unlocalized name is “point”.
     *
     * @see #INCH
     * @see #MILLIMETRE
     * @see #PIXEL
     *
     * @since 0.8
     */
    public static final Unit<Length> POINT;

    /**
     * The SI unit for plane angles (rad).
     * There is 2π radians in a circle.
     * The unlocalized name is “radian” and the identifier is EPSG:9101.
     * This is the base of all other {@linkplain #isAngular(Unit) angular} units:
     *
     * {@link #GRAD} (grad),
     * {@link #DEGREE} (°),
     * {@link #ARC_MINUTE} (′),
     * {@link #ARC_SECOND} (″) and
     * {@link #MICRORADIAN} (µrad)
     * among others.
     *
     * @since 0.8
     */
    public static final Unit<Angle> RADIAN;

    /**
     * Unit of measurement defined as π/200 radians (1 grad).
     * There is 400 grads in a circle.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “grad” and the identifier is EPSG:9105.
     *
     * @see #DEGREE
     *
     * @since 0.8
     */
    public static final Unit<Angle> GRAD;

    /**
     * Unit of measurement defined as π/180 radians (1°).
     * There is 360° in a circle.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “degree” and the identifier is EPSG:9102.
     *
     * @see #ARC_MINUTE
     * @see #ARC_SECOND
     *
     * @since 0.8
     */
    public static final Unit<Angle> DEGREE;

    /**
     * Unit of measurement defined as 1/60 degree (1′).
     * The distance of one arc-minute of latitude on Earth is approximatively 1852 metres
     * (one {@linkplain #NAUTICAL_MILE nautical mile}).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “arc-minute” and the identifier is EPSG:9103.
     *
     * @see #DEGREE
     * @see #ARC_SECOND
     * @see #NAUTICAL_MILE
     *
     * @since 0.8
     */
    public static final Unit<Angle> ARC_MINUTE;

    /**
     * Unit of measurement defined as 1/(60×60) degree (1″).
     * The distance of one arc-second of latitude on Earth is approximatively 31 metres.
     * This unit of measurement is used for rotation terms in
     * {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa-Wolf parameters}.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “arc-second” and the identifier is EPSG:9104.
     *
     * @see #DEGREE
     * @see #ARC_MINUTE
     *
     * @since 0.8
     */
    public static final Unit<Angle> ARC_SECOND;

    /**
     * Unit of measurement defined as 10<sup>-6</sup> radians (1 µrad).
     * The distance of one microradian of latitude on Earth is approximatively 2 millimetres.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “microradian” and the identifier is EPSG:9109.
     *
     * @see #ARC_MINUTE
     * @see #ARC_SECOND
     *
     * @since 0.8
     */
    public static final Unit<Angle> MICRORADIAN;

    /**
     * Unit of measurement defined as 10<sup>-3</sup> seconds (1 ms).
     * This unit is useful for inter-operability with various methods from the standard Java library.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “millisecond”.
     *
     * @see java.util.concurrent.TimeUnit#MILLISECONDS
     */
    public static final Unit<Time> MILLISECOND;

    /**
     * The SI base unit for durations (s).
     * The unlocalized name is “second” and the identifier is EPSG:1040.
     * This is the base of all other {@linkplain #isTemporal(Unit) temporal} units:
     *
     * {@link #MILLISECOND} (ms),
     * {@link #MINUTE} (min),
     * {@link #HOUR} (h),
     * {@link #DAY} (d),
     * {@link #WEEK} (wk) and
     * {@link #TROPICAL_YEAR} (a)
     * among others.
     *
     * @see #HERTZ
     *
     * @since 0.8
     */
    public static final Unit<Time> SECOND;

    /**
     * Unit of measurement defined as 60 seconds (1 min).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “minute”.
     *
     * @see #SECOND
     * @see #HOUR
     * @see #DAY
     *
     * @since 0.8
     */
    public static final Unit<Time> MINUTE;

    /**
     * Unit of measurement defined as 60×60 seconds (1 h).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “hour”.
     *
     * @see #SECOND
     * @see #MINUTE
     * @see #DAY
     *
     * @since 0.8
     */
    public static final Unit<Time> HOUR;

    /**
     * Unit of measurement defined as 24×60×60 seconds (1 d).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “day”.
     *
     * @see #HOUR
     * @see #WEEK
     * @see #TROPICAL_YEAR
     *
     * @since 0.8
     */
    public static final Unit<Time> DAY;

    /**
     * Unit of measurement defined as 7 days (1 wk).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “week”.
     *
     * @see #DAY
     * @see #TROPICAL_YEAR
     *
     * @since 0.8
     */
    public static final Unit<Time> WEEK;

    /**
     * Unit of measurement approximatively equals to 365.24219 days (1 a).
     * This is defined by the International Union of Geological Sciences (IUGS) as exactly 31556925.445 seconds,
     * taken as the length of the tropical year in the the year 2000.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND},
     * the unlocalized name is “year” and the identifier is EPSG:1029.
     *
     * @see #DAY
     * @see #WEEK
     *
     * @since 0.8
     */
    public static final Unit<Time> TROPICAL_YEAR;

    /**
     * The SI derived unit for pressure (Pa).
     * One pascal is equal to 1 N/m².
     * Pressures are often used in {@linkplain org.apache.sis.referencing.crs.DefaultParametricCRS parametric CRS}
     * for height measurements on a vertical axis.
     * The unlocalized name is “pascal”.
     *
     * @see #NEWTON
     * @see #SQUARE_METRE
     * @see #HECTOPASCAL
     *
     * @since 0.8
     */
    public static final Unit<Pressure> PASCAL;

    /**
     * Unit of measurement defined as 100 pascals (1 hPa).
     * The hectopascal is the international unit for measuring atmospheric or barometric pressure.
     * One hectopascal is exactly equal to one millibar.
     * The unlocalized name is “hectopascal”.
     *
     * @see #PASCAL
     *
     * @since 0.8
     */
    public static final Unit<Pressure> HECTOPASCAL;

    /**
     * The SI derived unit for area (m²).
     * The unlocalized name is “square metre”.
     *
     * @since 0.8
     *
     * @see #METRE
     * @see #CUBIC_METRE
     */
    public static final Unit<Area> SQUARE_METRE;

    /**
     * The SI derived unit for volume (m³).
     * The unlocalized name is “cubic metre”.
     *
     * @since 0.8
     *
     * @see #METRE
     * @see #SQUARE_METRE
     */
    public static final Unit<Volume> CUBIC_METRE;

    /**
     * The SI derived unit for speed (m/s).
     * The unlocalized name is “metres per second” and the identifier is EPSG:1026.
     *
     * @see #METRE
     * @see #SECOND
     * @see #KILOMETRES_PER_HOUR
     *
     * @since 0.8
     */
    public static final Unit<Speed> METRES_PER_SECOND;

    /**
     * Unit of measurement defined as 60/1000 metres per second (1 km/h).
     * The unlocalized name is “kilometres per hour”.
     *
     * @see #KILOMETRE
     * @see #HOUR
     * @see #METRES_PER_SECOND
     *
     * @since 0.8
     */
    public static final Unit<Speed> KILOMETRES_PER_HOUR;

    /**
     * The SI base unit for mass (kg).
     * The unlocalized name is “kilogram”.
     *
     * @since 0.8
     */
    public static final Unit<Mass> KILOGRAM;

    /**
     * The SI derived unit for force (N).
     * One newton is the force required to give a mass of 1 kg an acceleration of 1 m/s².
     * The unlocalized name is “newton”.
     *
     * @since 0.8
     *
     * @see #KILOGRAM
     * @see #METRES_PER_SECOND
     */
    public static final Unit<Force> NEWTON;

    /**
     * The SI derived unit for energy (J).
     * The unlocalized name is “joule”.
     *
     * @since 0.8
     */
    public static final Unit<Energy> JOULE;

    /**
     * The SI derived unit for power (W).
     * One watt is equal to one joule per second.
     * The unlocalized name is “watt”.
     *
     * @since 0.8
     */
    public static final Unit<Power> WATT;

    /**
     * The SI base unit for thermodynamic temperature (K).
     * The unlocalized name is “kelvin”.
     *
     * @see #CELSIUS
     *
     * @since 0.8
     */
    public static final Unit<Temperature> KELVIN;

    /**
     * Unit of measurement defined as the temperature in Kelvin minus 273.15.
     * The symbol is ℃ and the unlocalized name is “celsius”.
     *
     * @see #KELVIN
     *
     * @since 0.8
     */
    public static final Unit<Temperature> CELSIUS;

    /**
     * The SI derived unit for frequency (Hz).
     * One hertz is equal to one cycle per second.
     * The unlocalized name is “hertz”.
     *
     * @since 0.8
     *
     * @see #SECOND
     */
    public static final Unit<Frequency> HERTZ;

    /**
     * The base dimensionless unit for scale measurements.
     * The unlocalized name is “unity” and the identifier is EPSG:9201.
     * This is the base of all other {@linkplain #isScale(Unit) scale} units:
     *
     * {@link #PERCENT} (%),
     * {@link #PPM} (ppm) and
     * {@link #PIXEL} (px)
     * among others.
     *
     * @since 0.8
     */
    public static final Unit<Dimensionless> UNITY;

    /**
     * Dimensionless unit for percentages (%).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #UNITY}
     * and the unlocalized name is “percentage”.
     *
     * @see #UNITY
     * @see #PPM
     *
     * @since 0.8
     */
    public static final Unit<Dimensionless> PERCENT;

    /**
     * Dimensionless unit for parts per million (ppm).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #UNITY},
     * the unlocalized name is “parts per million” and the identifier is EPSG:9202.
     *
     * @see #UNITY
     * @see #PERCENT
     */
    public static final Unit<Dimensionless> PPM;

    /**
     * Salinity measured using PSS-78. While this is a dimensionless measurement, the {@code "psu"} symbol
     * is sometime added to PSS-78 measurement. However this is officially discouraged.
     *
     * <p>If we make this field public in a future SIS version, we should consider introducing a
     * {@code Salinity} quantity type.</p>
     */
    static final Unit<Dimensionless> PSU;

    /**
     * Sigma-level, used in oceanography. This is a way to measure a depth as a fraction of the sea floor depth.
     *
     * <p>If we make this field public in a future SIS version, we should consider introducing a new quantity type.
     * The type to introduce has not yet been determined.</p>
     */
    static final Unit<Dimensionless> SIGMA;

    /**
     * Dimensionless unit for pixels (px).
     * The unlocalized name is “pixel”.
     *
     * @see #POINT
     */
    public static final Unit<Dimensionless> PIXEL;

    static {
        final UnitDimension length        = new UnitDimension('L');
        final UnitDimension mass          = new UnitDimension('M');
        final UnitDimension time          = new UnitDimension('T');
        final UnitDimension temperature   = new UnitDimension('Θ');
        final UnitDimension area          = length.pow(2);
        final UnitDimension speed         = length.divide(time);
        final UnitDimension force         = mass.multiply(speed).divide(time);
        final UnitDimension energy        = force.multiply(length);
        final UnitDimension pressure      = force.divide(area);
        final UnitDimension dimensionless = UnitDimension.NONE;
        /*
         * Base, derived or alternate units that we need to reuse more than once in this static initializer.
         */
        final SystemUnit<Length>        m   = new SystemUnit<>(Length.class,        length,        "m",   Constants.EPSG_METRE);
        final SystemUnit<Time>          s   = new SystemUnit<>(Time.class,          time,          "s",   (short) 1040);
        final SystemUnit<Temperature>   K   = new SystemUnit<>(Temperature.class,   temperature,   "K",   (short) 0);
        final SystemUnit<Speed>         mps = new SystemUnit<>(Speed.class,         speed,         "m∕s", (short) 1026);
        final SystemUnit<Pressure>      Pa  = new SystemUnit<>(Pressure.class,      pressure,      "Pa",  (short) 0);
        final SystemUnit<Angle>         rad = new SystemUnit<>(Angle.class,         dimensionless, "rad", (short) 9101);
        final SystemUnit<Dimensionless> one = new SystemUnit<>(Dimensionless.class, dimensionless, "",    (short) 9201);
        /*
         * All SI prefix to be used below.
         */
        final LinearConverter nano  = LinearConverter.scale(1, 1000000000);
        final LinearConverter micro = LinearConverter.scale(1,    1000000);
        final LinearConverter milli = LinearConverter.scale(1,       1000);
        final LinearConverter centi = LinearConverter.scale(1,        100);
        final LinearConverter hecto = LinearConverter.scale(100,        1);
        final LinearConverter kilo  = LinearConverter.scale(1000,       1);
        /*
         * All Unit<Angle>
         */
        RADIAN      = add(rad);
        GRAD        = new ConventionalUnit<>(rad, LinearConverter.create(Math.PI /  200, 0),     "grad", (short) 9105);
        DEGREE      = new ConventionalUnit<>(rad, LinearConverter.create(Math.PI /  180, 0),        "°", (short) 9102);
        ARC_MINUTE  = new ConventionalUnit<>(rad, LinearConverter.create(Math.PI / (180*60),    0), "′", (short) 9103);
        ARC_SECOND  = new ConventionalUnit<>(rad, LinearConverter.create(Math.PI / (180*60*60), 0), "″", (short) 9104);
        MICRORADIAN = new ConventionalUnit<>(rad, micro, "µrad", (short) 9109);
        /*
         * All Unit<Length>
         */
        METRE          = add(m);
        NANOMETRE      = new ConventionalUnit<>(m, nano,  "nm", (short) 0);
        MILLIMETRE     = new ConventionalUnit<>(m, milli, "mm", (short) 1025);
        CENTIMETRE     = new ConventionalUnit<>(m, centi, "cm", (short) 1033);
        KILOMETRE      = new ConventionalUnit<>(m, kilo,  "km", (short) 9036);
        NAUTICAL_MILE  = new ConventionalUnit<>(m, LinearConverter.scale(   1852,        1), "M",     (short) 9030);
        STATUTE_MILE   = new ConventionalUnit<>(m, LinearConverter.scale(1609344,      100), "mi",    (short) 9093);
        US_SURVEY_FOOT = new ConventionalUnit<>(m, LinearConverter.scale(   1200,     3937), "ft_US", (short) 9003);
        FOOT           = new ConventionalUnit<>(m, LinearConverter.scale(   3048,    10000), "ft",    (short) 9002);
        INCH           = new ConventionalUnit<>(m, LinearConverter.scale(    254,    10000), "in",    (short) 0);
        POINT          = new ConventionalUnit<>(m, LinearConverter.scale( 996264, 72000000), "pt",    (short) 0);
        /*
         * All Unit<Time>
         */
        SECOND         = add(s);
        MILLISECOND    = new ConventionalUnit<>(s, milli, "ms", (short) 0);
        MINUTE         = new ConventionalUnit<>(s, LinearConverter.scale(         60,      1), "min", (short) 0);
        HOUR           = new ConventionalUnit<>(s, LinearConverter.scale(      60*60,      1), "h",   (short) 0);
        DAY            = new ConventionalUnit<>(s, LinearConverter.scale(   24*60*60,      1), "d",   (short) 0);
        WEEK           = new ConventionalUnit<>(s, LinearConverter.scale( 7*24*60*60,      1), "wk",  (short) 0);
        TROPICAL_YEAR  = new ConventionalUnit<>(s, LinearConverter.scale(31556925445.0, 1000), "a",   (short) 1029);
        /*
         * Other units.
         */
        KELVIN              = add(K);
        PASCAL              = add(Pa);
        METRES_PER_SECOND   = add(mps);
        KILOGRAM            = add(new SystemUnit<>(Mass.class,      mass,                    "kg",   (short) 0));
        SQUARE_METRE        = add(new SystemUnit<>(Area.class,      area,                    "m²",   (short) 0));
        CUBIC_METRE         = add(new SystemUnit<>(Volume.class,    length.pow(3),           "m³",   (short) 0));
        NEWTON              = add(new SystemUnit<>(Force.class,     force,                   "N",    (short) 0));
        JOULE               = add(new SystemUnit<>(Energy.class,    energy,                  "J",    (short) 0));
        WATT                = add(new SystemUnit<>(Power.class,     energy.divide(time),     "W",    (short) 0));
        HERTZ               = add(new SystemUnit<>(Frequency.class, time.pow(-1),            "Hz",   (short) 0));
        HECTOPASCAL         = new ConventionalUnit<>(Pa, hecto,                              "hPa",  (short) 0);
        KILOMETRES_PER_HOUR = new ConventionalUnit<>(mps, LinearConverter.scale(6, 100),     "km∕h", (short) 0);
        CELSIUS             = new ConventionalUnit<>(K, LinearConverter.create(1, 273.15),   "℃",    (short) 0);
        /*
         * All Unit<Dimensionless>
         */
        PERCENT = new ConventionalUnit<>(one, centi, "%",   (short) 0);
        PPM     = new ConventionalUnit<>(one, micro, "ppm", (short) 9202);
        PSU     = new SystemUnit<>(Dimensionless.class, dimensionless, "psu",   (short) 0);
        SIGMA   = new SystemUnit<>(Dimensionless.class, dimensionless, "sigma", (short) 0);
        PIXEL   = new SystemUnit<>(Dimensionless.class, dimensionless, "px",    (short) 0);
        UNITY   = add(one);  // Must be last in order to take precedence over all other units associated to UnitDimension.NONE.
    }

    /**
     * Invoked by {@code Units} static class initializer for registering SI base and derived units.
     * We do not synchronize that method on the assumption that {@link #SYSTEM} map will be fully
     * populated in a single thread by the {@code Units} class initializer, then never modified.
     */
    private static <Q extends Quantity<Q>> SystemUnit<Q> add(final SystemUnit<Q> unit) {
        SYSTEM.put(unit.dimension, unit);
        if (SYSTEM.put(unit.quantity, unit) != null) {
            throw new AssertionError();                 // Shall not map the same dimension twice.
        }
        return unit;
    }

    /**
     * Do not allows instantiation of this class.
     */
    private Units() {
    }

    /**
     * Returns {@code true} if the given unit is a linear unit.
     * Linear units are convertible to {@link #DEGREE}.
     *
     * <p>Angular units are dimensionless, which may be a cause of confusion with other
     * dimensionless units like {@link #UNITY} or {@link #PPM}. This method take care
     * of differentiating angular units from other dimensionless units.</p>
     *
     * @param  unit  the unit to check (may be {@code null}).
     * @return {@code true} if the given unit is non-null and angular.
     *
     * @see #ensureAngular(Unit)
     */
    public static boolean isAngular(final Unit<?> unit) {
        return (unit != null) && unit.getSystemUnit().equals(RADIAN);
    }

    /**
     * Returns {@code true} if the given unit is a linear unit.
     * Linear units are convertible to {@link #METRE}.
     *
     * @param  unit  the unit to check (may be {@code null}).
     * @return {@code true} if the given unit is non-null and linear.
     *
     * @see #METRE
     * @see #ensureLinear(Unit)
     */
    public static boolean isLinear(final Unit<?> unit) {
        return (unit != null) && unit.getSystemUnit().equals(METRE);
    }

    /**
     * Returns {@code true} if the given unit is a pressure unit.
     * Pressure units are convertible to {@link #PASCAL}.
     * Those units are sometime used instead of linear units for altitude measurements.
     *
     * @param  unit  the unit to check (may be {@code null}).
     * @return {@code true} if the given unit is non-null and a pressure unit.
     */
    public static boolean isPressure(final Unit<?> unit) {
        return (unit != null) && unit.getSystemUnit().equals(PASCAL);
    }

    /**
     * Returns {@code true} if the given unit is a temporal unit.
     * Temporal units are convertible to {@link #SECOND}.
     *
     * @param  unit  the unit to check (may be {@code null}).
     * @return {@code true} if the given unit is non-null and temporal.
     *
     * @see #ensureTemporal(Unit)
     */
    public static boolean isTemporal(final Unit<?> unit) {
        return (unit != null) && unit.getSystemUnit().equals(SECOND);
    }

    /**
     * Returns {@code true} if the given unit is a dimensionless scale unit.
     * This include {@link #UNITY} and {@link #PPM}.
     *
     * @param  unit  the unit to check (may be {@code null}).
     * @return {@code true} if the given unit is non-null and a dimensionless scale.
     *
     * @see #UNITY
     * @see #ensureScale(Unit)
     */
    public static boolean isScale(final Unit<?> unit) {
        return (unit != null) && unit.getSystemUnit().equals(UNITY);
    }

    /**
     * Makes sure that the specified unit is either null or an angular unit.
     * This method is used for argument checks in constructors and setter methods.
     *
     * @param  unit  the unit to check, or {@code null} if none.
     * @return the given {@code unit} argument, which may be null.
     * @throws IllegalArgumentException if {@code unit} is non-null and not an angular unit.
     *
     * @see #isAngular(Unit)
     */
    @SuppressWarnings("unchecked")
    public static Unit<Angle> ensureAngular(final Unit<?> unit) throws IllegalArgumentException {
        if (unit != null && !isAngular(unit)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NonAngularUnit_1, unit));
        }
        return (Unit<Angle>) unit;
    }

    /**
     * Makes sure that the specified unit is either null or a linear unit.
     * This method is used for argument checks in constructors and setter methods.
     *
     * @param  unit  the unit to check, or {@code null} if none.
     * @return the given {@code unit} argument, which may be null.
     * @throws IllegalArgumentException if {@code unit} is non-null and not a linear unit.
     *
     * @see #isLinear(Unit)
     */
    @SuppressWarnings("unchecked")
    public static Unit<Length> ensureLinear(final Unit<?> unit) throws IllegalArgumentException {
        if (unit != null && !isLinear(unit)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NonLinearUnit_1, unit));
        }
        return (Unit<Length>) unit;
    }

    /**
     * Makes sure that the specified unit is either null or a temporal unit.
     * This method is used for argument checks in constructors and setter methods.
     *
     * @param  unit  the unit to check, or {@code null} if none.
     * @return the given {@code unit} argument, which may be null.
     * @throws IllegalArgumentException if {@code unit} is non-null and not a temporal unit.
     *
     * @see #isTemporal(Unit)
     */
    @SuppressWarnings("unchecked")
    public static Unit<Time> ensureTemporal(final Unit<?> unit) throws IllegalArgumentException {
        if (unit != null && !isTemporal(unit)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NonTemporalUnit_1, unit));
        }
        return (Unit<Time>) unit;
    }

    /**
     * Makes sure that the specified unit is either null or a scale unit.
     * This method is used for argument checks in constructors and setter methods.
     *
     * @param  unit  the unit to check, or {@code null} if none.
     * @return the given {@code unit} argument, which may be null.
     * @throws IllegalArgumentException if {@code unit} is non-null and not a scale unit.
     *
     * @see #isScale(Unit)
     */
    @SuppressWarnings("unchecked")
    public static Unit<Dimensionless> ensureScale(final Unit<?> unit) throws IllegalArgumentException {
        if (unit != null && !isScale(unit)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NonScaleUnit_1, unit));
        }
        return (Unit<Dimensionless>) unit;
    }

    /**
     * Multiplies the given unit by the given factor. For example multiplying {@link #METRE}
     * by 1000 gives {@link #KILOMETRE}. Invoking this method is equivalent to invoking
     * {@link Unit#multiply(double)} except for the following:
     *
     * <ul>
     *   <li>A small tolerance factor is applied for a few factors commonly used in GIS.
     *       For example {@code multiply(RADIANS, 0.0174532925199...)} will return {@link #DEGREE}
     *       even if the given numerical value is slightly different than {@linkplain Math#PI π}/180.
     *       The tolerance factor and the set of units handled especially may change in future SIS versions.</li>
     *   <li>This method tries to returns unique instances for some common units.</li>
     * </ul>
     *
     * @param  <Q>     the quantity measured by the unit.
     * @param  unit    the unit to multiply.
     * @param  factor  the multiplication factor.
     * @return the unit multiplied by the given factor.
     */
    @Workaround(library="JSR-275", version="0.9.3")
    @SuppressWarnings("unchecked")
    public static <Q extends Quantity<Q>> Unit<Q> multiply(Unit<Q> unit, final double factor) {
        if (RADIAN.equals(unit)) {
            if (abs(factor - (PI / 180)) <= (EPS * PI/180)) {
                return (Unit<Q>) DEGREE;
            }
            if (abs(factor - (PI / 200)) <= (EPS * PI/200)) {
                return (Unit<Q>) GRAD;
            }
        } else if (METRE.equals(unit)) {
            if (abs(factor - 0.3048) <= (EPS * 0.3048)) {
                return (Unit<Q>) FOOT;
            }
            if (abs(factor - (1200.0/3937)) <= (EPS * (1200.0/3937))) {
                return (Unit<Q>) US_SURVEY_FOOT;
            }
        }
        if (abs(factor - 1) > EPS) {
            unit = unit.multiply(factor);
        }
        return UnitsMap.canonicalize(unit);
    }

    /**
     * Returns the factor by which to multiply the standard unit in order to get the given unit.
     * The "standard" unit is usually the SI unit on which the given unit is based.
     *
     * <p><b>Example:</b> if the given unit is <var>kilometre</var>, then this method returns 1000
     * since a measurement in kilometres must be multiplied by 1000 in order to give the equivalent
     * measurement in the "standard" units (here <var>metres</var>).</p>
     *
     * @param  <Q>   the quantity measured by the unit.
     * @param  unit  the unit for which we want the multiplication factor to standard unit.
     * @return the factor by which to multiply a measurement in the given unit in order to
     *         get an equivalent measurement in the standard unit.
     */
    @Workaround(library="JSR-275", version="0.9.3")
    public static <Q extends Quantity<Q>> double toStandardUnit(final Unit<Q> unit) {
        return derivative(unit.getConverterTo(unit.getSystemUnit()), 0);
    }

    /**
     * Returns an estimation of the derivative of the given converter at the given value.
     * This method is a workaround for a method which existed in previous JSR-275 API but
     * have been removed in more recent releases.
     *
     * <p>Current implementation computes the derivative as below:</p>
     *
     * {@preformat java
     *     return converter.convert(value + 1) - converter.convert(value);
     * }
     *
     * The above is exact for linear converters, which is the case of the vast majority of unit converters in use.
     * It may not be exact for a few unusual converter like the one from sexagesimal degrees to decimal degrees.
     *
     * @param  converter  the converter for which we want the derivative at a given point.
     * @param  value      the point at which to compute the derivative.
     * @return the derivative at the given point.
     */
    @Workaround(library="JSR-275", version="0.9.3")
    public static double derivative(final UnitConverter converter, final double value) {
        return converter.convert(value + 1) - converter.convert(value);
    }

    /**
     * Parses the given symbol. Invoking this method is equivalent to invoking
     * {@link UnitFormat#parse(CharSequence)} on a shared locale-independent instance.
     * This method is capable to handle some symbols found during WKT parsing or in XML files.
     * The list of symbols supported by this method is implementation-dependent
     * and may change in future SIS versions.
     *
     * <div class="section">Parsing authority codes</div>
     * As a special case, if the given {@code uom} arguments is of the form {@code "EPSG:####"}
     * or {@code "urn:ogc:def:uom:EPSG:####"} (ignoring case and whitespaces), then {@code "####"}
     * is parsed as an integer and forwarded to the {@link #valueOfEPSG(int)} method.
     *
     * <div class="section">NetCDF unit symbols</div>
     * The attributes in NetCDF files often merge the axis direction with the angular unit,
     * as in {@code "degrees_east"} or {@code "degrees_north"}. This {@code valueOf} method
     * ignores those suffixes and unconditionally returns {@link #DEGREE} for all axis directions.
     * In particular, the units for {@code "degrees_west"} and {@code "degrees_east"}
     * do <strong>not</strong> have opposite sign.
     * It is caller responsibility to handle the direction of axes associated to NetCDF units.
     *
     * @param  uom  the symbol to parse, or {@code null}.
     * @return the parsed symbol, or {@code null} if {@code uom} was null.
     * @throws ParserException if the given symbol can not be parsed.
     *
     * @see UnitFormat#parse(CharSequence)
     */
    public static Unit<?> valueOf(String uom) throws ParserException {
        return (uom != null) ? UnitFormat.INSTANCE.parse(uom) : null;
    }

    /**
     * Returns a hard-coded unit from an EPSG code. The {@code code} argument given to this method shall
     * be a code identifying a record in the {@code "Unit of Measure"} table of the EPSG geodetic dataset.
     * If this method does not recognize the given code, then it returns {@code null}.
     *
     * <p>The list of units recognized by this method is not exhaustive. This method recognizes
     * the base units declared in the {@code TARGET_UOM_CODE} column of the above-cited table,
     * and some frequently-used units. The list of recognized units may be updated in any future
     * version of SIS.</p>
     *
     * <p>The {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess} class uses this method
     * for fetching the base units, and derives automatically other units from the information
     * found in the EPSG database. This method is also used by other classes not directly related
     * to the EPSG database, like {@link org.apache.sis.referencing.factory.CommonAuthorityFactory}
     * which uses EPSG code for identifying units.</p>
     *
     * <p>The currently recognized values are:</p>
     * <table class="sis">
     *   <caption>EPSG codes for units</caption>
     *   <tr>
     *     <th>Angular units</th>
     *     <th class="sep">Linear units</th>
     *     <th class="sep">Temporal units</th>
     *     <th class="sep">Scale units</th>
     *   </tr><tr>
     *     <td><table class="compact" summary="Angular units">
     *       <tr><td style="width: 40px"><b>Code</b></td><td><b>Unit</b></td></tr>
     *       <tr><td>9101</td><td>radian</td></tr>
     *       <tr><td>9102</td><td>decimal degree</td></tr>
     *       <tr><td>9103</td><td>minute</td></tr>
     *       <tr><td>9104</td><td>second</td></tr>
     *       <tr><td>9105</td><td>grad</td></tr>
     *       <tr><td>9107</td><td>degree-minute-second</td></tr>
     *       <tr><td>9108</td><td>degree-minute-second</td></tr>
     *       <tr><td>9109</td><td>microradian</td></tr>
     *       <tr><td>9110</td><td>sexagesimal degree-minute-second</td></tr>
     *       <tr><td>9111</td><td>sexagesimal degree-minute</td></tr>
     *       <tr><td>9122</td><td>decimal degree</td></tr>
     *     </table></td>
     *     <td class="sep"><table class="compact" summary="Linear units">
     *       <tr><td style="width: 40px"><b>Code</b></td><td><b>Unit</b></td></tr>
     *       <tr><td>1025</td><td>millimetre</td></tr>
     *       <tr><td>1033</td><td>centimetre</td></tr>
     *       <tr><td>9001</td><td>metre</td></tr>
     *       <tr><td>9002</td><td>foot</td></tr>
     *       <tr><td>9003</td><td>US survey foot</td></tr>
     *       <tr><td>9030</td><td>nautical mile</td></tr>
     *       <tr><td>9036</td><td>kilometre</td></tr>
     *       <tr><td>9093</td><td>statute mile</td></tr>
     *     </table></td>
     *     <td class="sep"><table class="compact" summary="Time units">
     *       <tr><td style="width: 40px"><b>Code</b></td><td><b>Unit</b></td></tr>
     *       <tr><td>1029</td><td>year</td></tr>
     *       <tr><td>1040</td><td>second</td></tr>
     *     </table></td>
     *     <td class="sep"><table class="compact" summary="Scale units">
     *       <tr><td style="width: 40px"><b>Code</b></td><td><b>Unit</b></td></tr>
     *       <tr><td>9201</td><td>unity</td></tr>
     *       <tr><td>9202</td><td>parts per million</td></tr>
     *       <tr><td>9203</td><td>unity</td></tr>
     *     </table></td>
     *   </tr>
     * </table>
     *
     * <div class="note"><b>Note:</b>
     * EPSG uses code 9102 (<cite>degree</cite>) for prime meridian and coordinate operation parameters,
     * and code 9122 (<cite>degree (supplier to define representation)</cite>) for coordinate system axes.
     * But Apache SIS considers those two codes as synonymous.</div>
     *
     * @param  code  the EPSG code for a unit of measurement.
     * @return the unit, or {@code null} if the code is unrecognized.
     *
     * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createUnit(String)
     */
    public static Unit<?> valueOfEPSG(final int code) {
        switch (code) {
            case Constants.EPSG_PARAM_DEGREES:  // Fall through
            case Constants.EPSG_AXIS_DEGREES:   return DEGREE;
            case Constants.EPSG_METRE:          return METRE;

            case 1029: return TROPICAL_YEAR;
            case 1040: return SECOND;
            case 9002: return FOOT;
            case 9003: return US_SURVEY_FOOT;
            case 9030: return NAUTICAL_MILE;
            case 9036: return KILOMETRE;
            case 9101: return RADIAN;
            case 9103: return ARC_MINUTE;
            case 9104: return ARC_SECOND;
            case 9105: return GRAD;
            case 9109: return MICRORADIAN;
            case 9107: // Fall through
            case 9108: return SexagesimalConverter.DMS_SCALED;
            case 9110: return SexagesimalConverter.DMS;
            case 9111: return SexagesimalConverter.DM;
            case 9203: // Fall through
            case 9201: return UNITY;
            case 9202: return PPM;
            default:   return null;
        }
    }

    /**
     * Returns the EPSG code of the given units, or {@code null} if unknown.
     * This method is the converse of {@link #valueOfEPSG(int)}.
     *
     * <p>The same unit may be represented by different EPSG codes depending on the context:</p>
     * <ul>
     *   <li>EPSG:9102 – <cite>degree</cite> – is used for prime meridian and coordinate operation parameters.</li>
     *   <li>EPSG:9122 – <cite>degree (supplier to define representation)</cite> – is used for coordinate system axes.</li>
     * </ul>
     *
     * When such choice exists, the code to return is determined by the {@code inAxis} argument,
     * which specifies whether the code will be used for axis definition or in other context.
     *
     * @param  unit   the unit for which to get the EPSG code.
     * @param  inAxis {@code true} for a unit used in Coordinate System Axis definition.
     * @return the EPSG code of the given units, or {@code null} if unknown.
     *
     * @since 0.4
     */
    public static Integer getEpsgCode(final Unit<?> unit, final boolean inAxis) {
        Integer code = UnitsMap.EPSG_CODES.get(unit);
        if (inAxis && code != null && code == Constants.EPSG_PARAM_DEGREES) {
            code = UnitsMap.EPSG_AXIS_DEGREES;
        }
        return code;
    }
}
