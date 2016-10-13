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
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
public final class Units extends Static {
    /**
     * The SI base unit for distances (symbol “m”).
     *
     * @see #NANOMETRE
     * @see #MILLIMETRE
     * @see #CENTIMETRE
     * @see #KILOMETRE
     * @see #NAUTICAL_MILE
     * @see #MILE
     * @see #FOOT
     * @see #FOOT_SURVEY_US
     * @see #INCH
     * @see #PIXEL
     *
     * @since 0.8
     */
    public static final Unit<Length> METRE = tec.units.ri.unit.Units.METRE;

    /**
     * Unit of measurement defined as 10<sup>-9</sup> metres.
     * This unit is often used in wavelength measurements.
     *
     * @see #METRE
     * @see #CENTIMETRE
     * @see #MILLIMETRE
     *
     * @since 0.8
     */
    public static final Unit<Length> NANOMETRE = METRE.divide(1E9);

    /**
     * Unit of measurement defined as 0.001 metres.
     *
     * @see #METRE
     * @see #NANOMETRE
     * @see #CENTIMETRE
     * @see #KILOMETRE
     *
     * @since 0.8
     */
    public static final Unit<Length> MILLIMETRE = METRE.divide(1000);

    /**
     * Unit of measurement defined as 0.01 metres.
     *
     * @see #METRE
     * @see #NANOMETRE
     * @see #MILLIMETRE
     * @see #KILOMETRE
     *
     * @since 0.8
     */
    public static final Unit<Length> CENTIMETRE = METRE.divide(100);

    /**
     * Unit of measurement defined as 1000 metres.
     *
     * @see #METRE
     * @see #NAUTICAL_MILE
     *
     * @since 0.8
     */
    public static final Unit<Length> KILOMETRE = METRE.multiply(1000);

    /**
     * Unit of measurement defined as 1852 metres.
     * This is approximatively the distance between two parallels of latitude separated by one arc-minute.
     *
     * @see #METRE
     * @see #KILOMETRE
     * @see #MILE
     *
     * @since 0.8
     */
    public static final Unit<Length> NAUTICAL_MILE = METRE.multiply(1852);

    /**
     * Unit of measurement defined as 1609.344 metres.
     *
     * @see #METRE
     * @see #KILOMETRE
     * @see #NAUTICAL_MILE
     *
     * @since 0.8
     */
    public static final Unit<Length> MILE = METRE.multiply(1609.344);

    /**
     * Unit of measurement defined as 0.3048 metres.
     *
     * @see #METRE
     * @see #FOOT_SURVEY_US
     *
     * @since 0.8
     */
    public static final Unit<Length> FOOT = METRE.multiply(0.3048);

    /**
     * Unit of measurement defined as 12/39.37 metres.
     * This is approximatively 0.3048006096… metres.
     *
     * @see #METRE
     * @see #FOOT
     *
     * @since 0.8
     */
    public static final Unit<Length> FOOT_SURVEY_US = METRE.multiply(12 / 39.37);

    /**
     * Unit of measurement defined as 2.54 centimetres.
     *
     * @since 0.8
     */
    public static final Unit<Length> INCH = METRE.multiply(2.54 / 100);

    /**
     * Unit of measurement defined as 0.013837 inch.
     * This is commonly used to measure the height of a font.
     *
     * @since 0.8
     */
    public static final Unit<Length> POINT = METRE.multiply(0.996264 / 72);

    /**
     * The SI unit for plane angles.
     * There is 2π radians in a circle.
     *
     * @see #DEGREE
     * @see #GRAD
     * @see #ARC_MINUTE
     * @see #ARC_SECOND
     * @see #MICRORADIAN
     *
     * @since 0.8
     */
    public static final Unit<Angle> RADIAN = tec.units.ri.unit.Units.RADIAN;

    /**
     * Unit of measurement defined as π/180 radians.
     * There is 360° in a circle.
     *
     * @see #RADIAN
     * @see #ARC_MINUTE
     * @see #ARC_SECOND
     *
     * @since 0.8
     */
    public static final Unit<Angle> DEGREE = RADIAN.multiply(Math.PI/180);

    /**
     * Unit of measurement defined as π/200 radians.
     * There is 400 grads in a circle.
     *
     * @see #RADIAN
     * @see #DEGREE
     *
     * @since 0.8
     */
    public static final Unit<Angle> GRAD = RADIAN.multiply(Math.PI/200);

    /**
     * Unit of measurement defined as 1/60 degree.
     *
     * @see #RADIAN
     * @see #DEGREE
     *
     * @since 0.8
     */
    public static final Unit<Angle> ARC_MINUTE = RADIAN.multiply(Math.PI / (180*60));

    /**
     * Unit of measurement defined as 1/(60×60) degree.
     *
     * @see #RADIAN
     * @see #DEGREE
     *
     * @since 0.8
     */
    public static final Unit<Angle> ARC_SECOND = RADIAN.multiply(Math.PI / (180*60*60));

    /**
     * Unit of measurement defined as 10<sup>-6</sup> radians.
     *
     * @see #RADIAN
     *
     * @since 0.8
     */
    public static final Unit<Angle> MICRORADIAN = RADIAN.divide(1E+6);

    /**
     * The SI base unit for durations (symbol “s”).
     *
     * @see #MILLISECOND
     * @see #MINUTE
     * @see #HOUR
     * @see #DAY
     * @see #WEEK
     * @see #YEAR
     *
     * @since 0.8
     */
    public static final Unit<Time> SECOND = tec.units.ri.unit.Units.SECOND;

    /**
     * Unit of measurement defined as 10<sup>-3</sup> seconds.
     * Useful for conversions from and to {@link java.util.Date} objects.
     *
     * @see #SECOND
     */
    public static final Unit<Time> MILLISECOND = SECOND.divide(1000);

    /**
     * Unit of measurement defined as 60 seconds.
     *
     * @see #SECOND
     * @see #HOUR
     *
     * @since 0.8
     */
    public static final Unit<Time> MINUTE = tec.units.ri.unit.Units.MINUTE;

    /**
     * Unit of measurement defined as 60×60 seconds.
     *
     * @see #SECOND
     * @see #MINUTE
     *
     * @since 0.8
     */
    public static final Unit<Time> HOUR = tec.units.ri.unit.Units.HOUR;

    /**
     * Unit of measurement defined as 24×60×60 seconds.
     *
     * @see #SECOND
     * @see #WEEK
     *
     * @since 0.8
     */
    public static final Unit<Time> DAY = tec.units.ri.unit.Units.DAY;

    /**
     * Unit of measurement defined as 7 days.
     *
     * @see #SECOND
     * @see #DAY
     * @see #YEAR
     *
     * @since 0.8
     */
    public static final Unit<Time> WEEK = tec.units.ri.unit.Units.WEEK;

    /**
     * The EPSG:1029 definition of year.
     *
     * @see #SECOND
     * @see #WEEK
     * @see #DAY
     *
     * @since 0.8
     */
    private static final Unit<Time> YEAR = SECOND.divide(31556925.445);

    /**
     * The SI unit for frequency (symbol “Hz”).
     * A unit of frequency equal to one cycle per second.
     *
     * @since 0.8
     */
    public static final Unit<Frequency> HERTZ = tec.units.ri.unit.Units.HERTZ;

    /**
     * The SI unit for pressure (symbol “Pa”).
     * One pascal is equal to 1 N/m².
     * Pressures are often used in {@linkplain org.apache.sis.referencing.crs.DefaultParametricCRS parametric CRS}
     * for height measurements.
     *
     * @see #HECTOPASCAL
     *
     * @since 0.8
     */
    public static final Unit<Pressure> PASCAL = tec.units.ri.unit.Units.PASCAL;

    /**
     * Unit of measurement defined as 100 pascals.
     *
     * @see #PASCAL
     *
     * @since 0.8
     */
    public static final Unit<Pressure> HECTOPASCAL = PASCAL.multiply(100);

    /**
     * The SI base unit for mass (symbol “kg”).
     *
     * @since 0.8
     */
    public static final Unit<Mass> KILOGRAM = tec.units.ri.unit.Units.KILOGRAM;

    /**
     * The SI unit for force (symbol “N”).
     * One newton is the force required to give a mass of 1 kg an acceleration of 1 m/s².
     *
     * @since 0.8
     */
    public static final Unit<Force> NEWTON = tec.units.ri.unit.Units.NEWTON;

    /**
     * The SI unit for energy (symbol “J”).
     *
     * @since 0.8
     */
    public static final Unit<Energy> JOULE = tec.units.ri.unit.Units.JOULE;

    /**
     * The SI unit for power (symbol “W”).
     * One watt is equal to one joule per second.
     *
     * @since 0.8
     */
    public static final Unit<Power> WATT = tec.units.ri.unit.Units.WATT;

    /**
     * The SI base unit for thermodynamic temperature (symbol “K”).
     *
     * @see #CELSIUS
     *
     * @since 0.8
     */
    public static final Unit<Temperature> KELVIN = tec.units.ri.unit.Units.KELVIN;

    /**
     * Unit of measurement defined as the temperature in Kelvin minus 273.15.
     *
     * @see #KELVIN
     *
     * @since 0.8
     */
    public static final Unit<Temperature> CELSIUS = tec.units.ri.unit.Units.CELSIUS;

    /**
     * Derived unit of measurement for speed (symbol “m/s”).
     *
     * @see #KILOMETRES_PER_HOUR
     *
     * @since 0.8
     */
    public static final Unit<Speed> METRES_PER_SECOND = tec.units.ri.unit.Units.METRES_PER_SECOND;

    /**
     * Derived unit of measurement for speed (symbol “km/h”).
     *
     * @see #METRES_PER_SECOND
     *
     * @since 0.8
     */
    public static final Unit<Speed> KILOMETRES_PER_HOUR = tec.units.ri.unit.Units.KILOMETRES_PER_HOUR;

    /**
     * Derived unit of measurement for area (symbol “m²”).
     *
     * @since 0.8
     */
    public static final Unit<Area> SQUARE_METRE = tec.units.ri.unit.Units.SQUARE_METRE;

    /**
     * Derived unit of measurement for area (symbol “m²”).
     *
     * @since 0.8
     */
    public static final Unit<Volume> CUBIC_METRE = tec.units.ri.unit.Units.CUBIC_METRE;

    /**
     * Dimensionless unit for scale measurements.
     *
     * @see #PERCENT
     * @see #PPM
     *
     * @since 0.8
     */
    public static final Unit<Dimensionless> ONE = tec.units.ri.AbstractUnit.ONE;

    /**
     * Dimensionless unit for percentages.
     *
     * @see #ONE
     * @see #PPM
     *
     * @since 0.8
     */
    public static final Unit<Dimensionless> PERCENT = tec.units.ri.unit.Units.PERCENT;

    /**
     * Dimensionless unit for parts per million.
     *
     * @see #ONE
     * @see #PERCENT
     */
    public static final Unit<Dimensionless> PPM = ONE.divide(1E+6);

    /**
     * Salinity measured using PSS-78. While this is a dimensionless measurement, the {@code "psu"} symbol
     * is sometime added to PSS-78 measurement. However this is officially discouraged.
     */
    static final Unit<Dimensionless> PSU = ONE.alternate("psu");

    /**
     * Sigma-level, used in oceanography. This is a way to measure a depth as a fraction of the sea floor depth.
     */
    static final Unit<Dimensionless> SIGMA = ONE.alternate("sigma");

    /**
     * Dimensionless unit for pixels.
     */
    public static final Unit<Dimensionless> PIXEL = ONE.alternate("pixel");

    static {
        final javax.measure.format.UnitFormat format = tec.units.ri.format.SimpleUnitFormat.getInstance();
        format.label(METRE,  "m");
        format.label(FOOT,  "ft");
        format.label(DEGREE, "°");
        format.label(GRAD, "grad");
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
     * dimensionless units like {@link #ONE} or {@link #PPM}. This method take care
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
     * This include {@link Unit#ONE} and {@link #PPM}.
     *
     * @param  unit  the unit to check (may be {@code null}).
     * @return {@code true} if the given unit is non-null and a dimensionless scale.
     *
     * @see #ensureScale(Unit)
     */
    public static boolean isScale(final Unit<?> unit) {
        return (unit != null) && unit.getSystemUnit().equals(ONE);
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
                return (Unit<Q>) FOOT_SURVEY_US;
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
     *       <tr><td>9001</td><td>metre</td></tr>
     *       <tr><td>9002</td><td>foot</td></tr>
     *       <tr><td>9003</td><td>US survey foot</td></tr>
     *       <tr><td>9030</td><td>nautical mile</td></tr>
     *       <tr><td>9036</td><td>kilometre</td></tr>
     *     </table></td>
     *     <td class="sep"><table class="compact" summary="Time units">
     *       <tr><td style="width: 40px"><b>Code</b></td><td><b>Unit</b></td></tr>
     *       <tr><td>1029</td><td>year</td></tr>
     *       <tr><td>1040</td><td>second</td></tr>
     *     </table></td>
     *     <td class="sep"><table class="compact" summary="Scale units">
     *       <tr><td style="width: 40px"><b>Code</b></td><td><b>Unit</b></td></tr>
     *       <tr><td>9201</td><td>one</td></tr>
     *       <tr><td>9202</td><td>part per million</td></tr>
     *       <tr><td>9203</td><td>one</td></tr>
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

            case 1029: return YEAR;
            case 1040: return SECOND;
            case 9002: return FOOT;
            case 9003: return FOOT_SURVEY_US;
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
            case 9201: return ONE;
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
