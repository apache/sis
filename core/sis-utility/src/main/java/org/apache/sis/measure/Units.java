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

import javax.measure.Dimension;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.ParserException;
import javax.measure.Quantity;
import javax.measure.quantity.*;
import javax.measure.quantity.Angle;                // Because of name collision with Angle in this SIS package.
import org.opengis.geometry.DirectPosition;         // For javadoc
import org.opengis.referencing.cs.AxisDirection;    // For javadoc

import org.apache.sis.util.Static;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Constants;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static org.apache.sis.measure.SexagesimalConverter.EPS;


/**
 * Provides constants for various Units of Measurement together with static methods working on {@link Unit} instances.
 * Unit names and definitions in this class follow the definitions provided in the EPSG geodetic dataset
 * (when the unit exists in that dataset),
 * except “year” which has been renamed “{@linkplain #TROPICAL_YEAR tropical year}”.
 * This class focuses on the most commonly used units in the geospatial domain:
 * angular units ({@linkplain #DEGREE degree}, {@linkplain #ARC_SECOND arc-second}, …),
 * linear units ({@linkplain #KILOMETRE kilometre}, {@linkplain #NAUTICAL_MILE nautical mile}, …) and
 * temporal units ({@linkplain #DAY day}, {@linkplain #TROPICAL_YEAR year}, …),
 * but some other kind of units are also provided for completeness.
 * The main quantities are listed below, together with some related units:
 *
 * <table class="sis">
 *   <caption>Some quantities and related units</caption>
 *   <tr><th colspan="2">Quantity type</th><th>System unit</th><th>Some conventional units</th></tr>
 *
 *   <tr><td style="padding-top:15px" colspan="4"><b>Fundamental:</b></td></tr>
 *   <tr><td>{@link Length}</td>            <td>(L)</td> <td>{@link #METRE}</td>    <td>{@link #CENTIMETRE}, {@link #KILOMETRE}, {@link #NAUTICAL_MILE}, {@link #STATUTE_MILE}, {@link #FOOT}</td></tr>
 *   <tr><td>{@link Mass}</td>              <td>(M)</td> <td>{@link #KILOGRAM}</td> <td></td></tr>
 *   <tr><td>{@link Time}</td>              <td>(T)</td> <td>{@link #SECOND}</td>   <td>{@link #MILLISECOND}, {@link #MINUTE}, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}</td></tr>
 *   <tr><td>{@link ElectricCurrent}</td>   <td>(I)</td> <td>{@link #AMPERE}</td>   <td></td></tr>
 *   <tr><td>{@link Temperature}</td>       <td>(Θ)</td> <td>{@link #KELVIN}</td>   <td>{@link #CELSIUS}, {@link #FAHRENHEIT}</td></tr>
 *   <tr><td>{@link AmountOfSubstance}</td> <td>(N)</td> <td>{@link #MOLE}</td>     <td></td></tr>
 *   <tr><td>{@link LuminousIntensity}</td> <td>(J)</td> <td>{@link #CANDELA}</td>  <td></td></tr>
 *
 *   <tr><td style="padding-top:15px" colspan="4"><b>Dimensionless:</b></td></tr>
 *   <tr><td>{@link Angle}</td>      <td></td> <td>{@link #RADIAN}</td>    <td>{@link #DEGREE}, {@link #ARC_MINUTE}, {@link #ARC_SECOND}, {@link #GRAD}</td></tr>
 *   <tr><td>{@link SolidAngle}</td> <td></td> <td>{@link #STERADIAN}</td> <td></td></tr>
 *
 *   <tr><td style="padding-top:15px" colspan="4"><b>Derived:</b></td></tr>
 *   <tr><td>{@link Area}</td>     <td>(A)</td> <td>{@link #SQUARE_METRE}</td>      <td>{@link #HECTARE}</td></tr>
 *   <tr><td>{@link Volume}</td>   <td>(V)</td> <td>{@link #CUBIC_METRE}</td>       <td></td></tr>
 *   <tr><td>{@link Speed}</td>    <td>(ν)</td> <td>{@link #METRES_PER_SECOND}</td> <td>{@link #KILOMETRES_PER_HOUR}</td></tr>
 *   <tr><td>{@link Pressure}</td> <td></td>    <td>{@link #PASCAL}</td>            <td>{@link #HECTOPASCAL}, {@link #DECIBAR}, {@link #BAR}, {@link #ATMOSPHERE}</td></tr>
 * </table>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
public final class Units extends Static {
    /**
     * Unit of measurement defined as 10<sup>-9</sup> metres (1 nm). This unit is often used in
     * {@linkplain org.apache.sis.metadata.iso.content.DefaultBand#getBoundUnits() wavelength measurements}.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE}
     * and the unlocalized name is "nanometre".
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI units:</td>         <td style="word-spacing:1em"><u>{@code NANOMETRE}</u>, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table></div>
     *
     * @see org.apache.sis.metadata.iso.content.DefaultBand#getBoundUnits()
     *
     * @since 0.8
     */
    public static final Unit<Length> NANOMETRE;

    /**
     * Unit of measurement defined as 0.001 metres (1 mm).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “millimetre” and the identifier is EPSG:1025.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, <u>{@code MILLIMETRE}</u>, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Length> MILLIMETRE;

    /**
     * Unit of measurement defined as 0.01 metres (1 cm).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “centimetre” and the identifier is EPSG:1033.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, <u>{@code CENTIMETRE}</u>, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Length> CENTIMETRE;

    /**
     * The SI base unit for distances (m).
     * The unlocalized name is “metre” and the identifier is EPSG:9001.
     * This is the base of all other {@linkplain #isLinear(Unit) linear} units.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <u><b>{@code METRE}</b></u>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Length> METRE;

    /**
     * Unit of measurement defined as 1000 metres (1 km).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “kilometre” and the identifier is EPSG:9036.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, <u>{@code KILOMETRE}</u>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #KILOMETRES_PER_HOUR}.</td></tr>
     * </table></div>
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
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, <u>{@code NAUTICAL_MILE}</u>.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #KILOMETRES_PER_HOUR}.</td></tr>
     * </table></div>
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
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, <u>{@code STATUTE_MILE}</u>, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #KILOMETRES_PER_HOUR}.</td></tr>
     * </table></div>
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
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, <u>{@code US_SURVEY_FOOT}</u>, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Length> US_SURVEY_FOOT;

    /**
     * Unit of measurement defined as 0.3047972654 metres.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “Clarke’s foot” and the identifier is EPSG:9005.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, <u>{@code CLARKE_FOOT}</u>, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Length> CLARKE_FOOT;

    /**
     * Unit of measurement defined as exactly 0.3048 metres (1 ft).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “foot” and the identifier is EPSG:9002.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, <u>{@code FOOT}</u>, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Length> FOOT;

    /**
     * Unit of measurement defined as 2.54 centimetres (1 in).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE}
     * and the unlocalized name is “inch”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #POINT}, <u>{@code INCH}</u>, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table></div>
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
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI length units:</td>  <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em"><u>{@code POINT}</u>, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Length> POINT;

    /**
     * The SI derived unit for area (m²).
     * The unlocalized name is “square metre”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI area units:</td> <td style="word-spacing:1em"><u><b>{@code SQUARE_METRE}</b></u>, {@link #HECTARE}.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #CUBIC_METRE}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Area> SQUARE_METRE;

    /**
     * Unit of measurement defined as 10,000 square metres (1 ha).
     * One hectare is exactly equals to one hectometre (1 hm²).
     * While not an SI unit, the hectare is often used in the measurement of land.
     * The unlocalized name is “hectare”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI area units:</td> <td style="word-spacing:1em"><b>{@link #SQUARE_METRE}</b>, <u>{@code HECTARE}</u>.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #CUBIC_METRE}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Area> HECTARE;

    /**
     * The SI derived unit for volume (m³).
     * The unlocalized name is “cubic metre”.
     *
     * @since 0.8
     */
    public static final Unit<Volume> CUBIC_METRE;

    /**
     * The SI unit for solid angles (sr).
     * The unlocalized name is “steradian”.
     *
     * @since 0.8
     */
    public static final Unit<SolidAngle> STERADIAN;

    /**
     * Unit of measurement defined as 10<sup>-6</sup> radians (1 µrad).
     * The distance of one microradian of latitude on Earth is approximatively 2 millimetres.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “microradian” and the identifier is EPSG:9109.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI angle units:</td>   <td style="word-spacing:1em"><u>{@code MICRORADIAN}</u>, <b>{@link #RADIAN}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #DEGREE}, {@link #ARC_MINUTE}, {@link #ARC_SECOND}, {@link #GRAD}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Angle> MICRORADIAN;

    /**
     * The SI unit for plane angles (rad).
     * There is 2π radians in a circle.
     * The unlocalized name is “radian” and the identifier is EPSG:9101.
     * This is the base of all other {@linkplain #isAngular(Unit) angular} units.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI angle units:</td>   <td style="word-spacing:1em">{@link #MICRORADIAN}, <u><b>{@code RADIAN}</b></u>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #DEGREE}, {@link #ARC_MINUTE}, {@link #ARC_SECOND}, {@link #GRAD}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Angle> RADIAN;

    /**
     * Unit of measurement defined as π/180 radians (1°).
     * There is 360° in a circle.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “degree” and the identifier is EPSG:9102.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI angle units:</td>   <td style="word-spacing:1em">{@link #MICRORADIAN}, <b>{@link #RADIAN}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em"><u>{@code DEGREE}</u>, {@link #ARC_MINUTE}, {@link #ARC_SECOND}, {@link #GRAD}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table></div>
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
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI angle units:</td>   <td style="word-spacing:1em">{@link #MICRORADIAN}, <b>{@link #RADIAN}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #DEGREE}, <u>{@code ARC_MINUTE}</u>, {@link #ARC_SECOND}, {@link #GRAD}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table></div>
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
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI angle units:</td>   <td style="word-spacing:1em">{@link #MICRORADIAN}, <b>{@link #RADIAN}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #DEGREE}, {@link #ARC_MINUTE}, <u>{@code ARC_SECOND}</u>, {@link #GRAD}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Angle> ARC_SECOND;

    /**
     * Unit of measurement defined as π/200 radians (1 grad).
     * There is 400 grads in a circle.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “grad”, but the “gon” alias is also accepted.
     * The identifier is EPSG:9105.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI angle units:</td>   <td style="word-spacing:1em">{@link #MICRORADIAN}, <b>{@link #RADIAN}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #DEGREE}, {@link #ARC_MINUTE}, {@link #ARC_SECOND}, <u>{@code GRAD}</u>.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Angle> GRAD;

    /**
     * Unit of measurement defined as 10<sup>-3</sup> seconds (1 ms).
     * This unit is useful for inter-operability with various methods from the standard Java library.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “millisecond”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI time units:</td>    <td style="word-spacing:1em"><u>{@code MILLISECOND}</u>, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #METRES_PER_SECOND}, {@link #HERTZ}.</td></tr>
     * </table></div>
     *
     * @since 0.3
     *
     * @see java.util.concurrent.TimeUnit#MILLISECONDS
     */
    public static final Unit<Time> MILLISECOND;

    /**
     * The SI base unit for durations (s).
     * The unlocalized name is “second” and the identifier is EPSG:1040.
     * This is the base of all other {@linkplain #isTemporal(Unit) temporal} units.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI time units:</td>    <td style="word-spacing:1em">{@link #MILLISECOND}, <u><b>{@link #SECOND}</b></u>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #METRES_PER_SECOND}, {@link #HERTZ}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Time> SECOND;

    /**
     * Unit of measurement defined as 60 seconds (1 min).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “minute”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI time units:</td>    <td style="word-spacing:1em">{@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em"><u>{@code MINUTE}</u>, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #METRES_PER_SECOND}, {@link #HERTZ}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Time> MINUTE;

    /**
     * Unit of measurement defined as 60×60 seconds (1 h).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “hour”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI time units:</td>    <td style="word-spacing:1em">{@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #MINUTE}, <u>{@code HOUR}</u>, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}, {@link #HERTZ}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Time> HOUR;

    /**
     * Unit of measurement defined as 24×60×60 seconds (1 d).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “day”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI time units:</td>    <td style="word-spacing:1em">{@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, <u>{@code DAY}</u>, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}, {@link #HERTZ}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Time> DAY;

    /**
     * Unit of measurement defined as 7 days (1 wk).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “week”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI time units:</td>    <td style="word-spacing:1em">{@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, {@link #DAY}, <u>{@link WEEK}</u>, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}, {@link #HERTZ}.</td></tr>
     * </table></div>
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
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI time units:</td>    <td style="word-spacing:1em">{@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, <u>{@code TROPICAL_YEAR}</u>.</td></tr>
     *   <tr><td>Derived units:</td>    <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}, {@link #HERTZ}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Time> TROPICAL_YEAR;

    /**
     * The SI derived unit for frequency (Hz).
     * One hertz is equal to one cycle per {@linkplain #SECOND second}.
     * The unlocalized name is “hertz”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td><td style="word-spacing:0.5em">{@link #SECOND}<sup>-1</sup></td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Frequency> HERTZ;

    /**
     * The SI derived unit for speed (m/s).
     * The unlocalized name is “metres per second” and the identifier is EPSG:1026.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI speed units:</td>   <td style="word-spacing:1em"><u><b>{@code METRES_PER_SECOND}</b></u>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}.</td></tr>
     *   <tr><td>Components:</td>       <td style="word-spacing:0.5em">{@link #METRE} ∕ {@link #SECOND}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Speed> METRES_PER_SECOND;

    /**
     * Unit of measurement defined as 60/1000 metres per second (1 km/h).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRES_PER_SECOND}
     * and the unlocalized name is “kilometres per hour”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI speed units:</td>   <td style="word-spacing:1em"><b>{@link #METRES_PER_SECOND}</b>.</td></tr>
     *   <tr><td>In other systems:</td> <td style="word-spacing:1em"><u>{@code KILOMETRES_PER_HOUR}</u>.</td></tr>
     *   <tr><td>Components:</td>       <td style="word-spacing:0.5em">{@link #KILOMETRE} ∕ {@link #HOUR}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Speed> KILOMETRES_PER_HOUR;

    /**
     * The SI derived unit for pressure (Pa).
     * One pascal is equal to 1 N/m².
     * Pressures are often used in {@linkplain org.apache.sis.referencing.crs.DefaultParametricCRS parametric CRS}
     * for height measurements on a vertical axis.
     * The unlocalized name is “pascal”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI pressure units:</td> <td style="word-spacing:1em"><u><b>{@code PASCAL}</b></u>, {@link #HECTOPASCAL}.</td></tr>
     *   <tr><td>In other systems:</td>  <td style="word-spacing:1em">{@link #DECIBAR}, {@link #BAR}, {@link #ATMOSPHERE}.</td></tr>
     *   <tr><td>Components:</td>        <td style="word-spacing:0.5em">{@link #NEWTON} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Pressure> PASCAL;

    /**
     * Unit of measurement defined as 100 pascals (1 hPa).
     * The hectopascal is the international unit for measuring atmospheric or barometric pressure
     * and is exactly equal to one millibar.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #PASCAL}
     * and the unlocalized name is “hectopascal”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI pressure units:</td> <td style="word-spacing:1em"><b>{@link #PASCAL}</b>, <u>{@code HECTOPASCAL}</u>.</td></tr>
     *   <tr><td>In other systems:</td>  <td style="word-spacing:1em">{@link #DECIBAR}, {@link #BAR}, {@link #ATMOSPHERE}.</td></tr>
     *   <tr><td>Components:</td>        <td style="word-spacing:0.5em">{@link #NEWTON} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Pressure> HECTOPASCAL;

    /**
     * Unit of measurement defined as 10000 pascals (1 dbar).
     * This unit is used in oceanography as there is an approximate numerical equivalence
     * between pressure changes in decibars and depth changes in metres underwater.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #PASCAL}
     * and the unlocalized name is “decibar”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI pressure units:</td> <td style="word-spacing:1em"><b>{@link #PASCAL}</b>, {@link #HECTOPASCAL}.</td></tr>
     *   <tr><td>In other systems:</td>  <td style="word-spacing:1em"><u>{@code DECIBAR}</u>, {@link #BAR}, {@link #ATMOSPHERE}.</td></tr>
     *   <tr><td>Components:</td>        <td style="word-spacing:0.5em">{@link #NEWTON} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Pressure> DECIBAR;

    /**
     * Unit of measurement defined as 100000 pascals (1 bar).
     * One bar is slightly less than the average atmospheric pressure on Earth at sea level.
     * One millibar is exactly equal to one {@linkplain #HECTOPASCAL hectopascal}.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #PASCAL}
     * and the unlocalized name is “bar”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI pressure units:</td> <td style="word-spacing:1em"><b>{@link #PASCAL}</b>, {@link #HECTOPASCAL}.</td></tr>
     *   <tr><td>In other systems:</td>  <td style="word-spacing:1em">{@link #DECIBAR}, <u>{@code BAR}</u>, {@link #ATMOSPHERE}.</td></tr>
     *   <tr><td>Components:</td>        <td style="word-spacing:0.5em">{@link #NEWTON} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Pressure> BAR;

    /**
     * Unit of measurement defined as 101325 pascals (1 atm).
     * One atmosphere reflects the pressure at the mean sea level for countries around 49°N of latitude.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #PASCAL}
     * and the unlocalized name is “atmosphere”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI pressure units:</td> <td style="word-spacing:1em"><b>{@link #PASCAL}</b>, {@link #HECTOPASCAL}.</td></tr>
     *   <tr><td>In other systems:</td>  <td style="word-spacing:1em">{@link #DECIBAR}, {@link #BAR}, <u>{@code ATMOSPHERE}</u>.</td></tr>
     *   <tr><td>Components:</td>        <td style="word-spacing:0.5em">{@link #NEWTON} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Pressure> ATMOSPHERE;

    /**
     * The SI derived unit for force (N).
     * One newton is the force required to give a mass of 1 kg an acceleration of 1 m/s².
     * The unlocalized name is “newton”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #KILOGRAM} ⋅ {@link #METRES_PER_SECOND} ∕ {@link #SECOND}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #PASCAL}, {@link #JOULE}, {@link #WATT}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Force> NEWTON;

    /**
     * The SI derived unit for energy (J).
     * The unlocalized name is “joule”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #NEWTON} ⋅ {@link #METRE}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #WATT}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Energy> JOULE;

    /**
     * The SI derived unit for power (W).
     * One watt is equal to one joule per second.
     * The unlocalized name is “watt”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #JOULE} ∕ {@link #SECOND}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #VOLT}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Power> WATT;

    /**
     * The SI derived unit for electric potential difference (V).
     * The unlocalized name is “volt”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #WATT} ∕ {@link #AMPERE}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #FARAD}, {@link #OHM}, {@link #SIEMENS}, {@link #WEBER}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<ElectricPotential> VOLT;

    /**
     * The SI base unit for electric current (A).
     * The unlocalized name is “ampere”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #COULOMB}, {@link #VOLT}, {@link #OHM}, {@link #SIEMENS}, {@link #HENRY}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<ElectricCurrent> AMPERE;

    /**
     * The SI derived unit for electric charge (C).
     * One coulomb is the charge transfered by a current of one ampere during one second.
     * The unlocalized name is “coulomb”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #AMPERE} ⋅ {@link #SECOND}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #FARAD}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<ElectricCharge> COULOMB;

    /**
     * The SI derived unit for electric capacitance (F).
     * The unlocalized name is “farad”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #COULOMB} ∕ {@link #VOLT}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<ElectricCapacitance> FARAD;

    /**
     * The SI derived unit for electric resistance (Ω).
     * This is the inverse of electric conductance.
     * The unlocalized name is “ohm”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #VOLT} ∕ {@link #AMPERE}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #SIEMENS}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<ElectricResistance> OHM;

    /**
     * The SI derived unit for electric conductance (S).
     * This is the inverse of electric resistance.
     * The unlocalized name is “siemens”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #AMPERE} ∕ {@link #VOLT}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #OHM}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<ElectricConductance> SIEMENS;

    /**
     * The SI derived unit for magnetic flux (Wb).
     * The unlocalized name is “weber”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #VOLT} ⋅ {@link #SECOND}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #TESLA}, {@link #HENRY}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<MagneticFlux> WEBER;

    /**
     * The SI derived unit for magnetic flux density (T).
     * The unlocalized name is “tesla”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #WEBER} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<MagneticFluxDensity> TESLA;

    /**
     * The SI derived unit for inductance (H).
     * The unlocalized name is “henry”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #WEBER} ∕ {@link #AMPERE}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<ElectricInductance> HENRY;

    /**
     * The SI base unit for thermodynamic temperature (K).
     * The unlocalized name is “kelvin”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI temperature units:</td> <td style="word-spacing:1em"><u><b>{@code KELVIN}</b></u>.</td></tr>
     *   <tr><td>In other systems:</td>     <td style="word-spacing:1em">{@link #CELSIUS}, {@link #FAHRENHEIT}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Temperature> KELVIN;

    /**
     * Unit of measurement defined as the temperature in Kelvin minus 273.15.
     * The symbol is °C and the unlocalized name is “Celsius”.
     * Note that this is the only SI unit with an upper-case letter in its name.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI temperature units:</td> <td style="word-spacing:1em"><b>{@link #KELVIN}</b>.</td></tr>
     *   <tr><td>In other systems:</td>     <td style="word-spacing:1em"><u>{@code CELSIUS}</u>, {@link #FAHRENHEIT}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Temperature> CELSIUS;

    /**
     * Unit of measurement defined as 1.8 degree Celsius plus 32.
     * The symbol is °F and the unlocalized name is “Fahrenheit”
     * (note the upper-case "F" letter).
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>SI temperature units:</td> <td style="word-spacing:1em"><b>{@link #KELVIN}</b>.</td></tr>
     *   <tr><td>In other systems:</td>     <td style="word-spacing:1em">{@link #CELSIUS}, <u>{@code FAHRENHEIT}</u>.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Temperature> FAHRENHEIT;

    /**
     * The SI base unit for luminous intensity (cd).
     * The unlocalized name is “candela”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #LUMEN}, {@link #LUX}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<LuminousIntensity> CANDELA;

    /**
     * The SI derived unit for luminous flux (lm).
     * The unlocalized name is “lumen”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #CANDELA} ⋅ {@link #STERADIAN}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #LUX}.</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<LuminousFlux> LUMEN;

    /**
     * The SI derived unit for illuminance (lx).
     * The unlocalized name is “lux”.
     *
     * <div class="note"><p class="simpleTagLabel" style="margin-bottom:0">Related units:</p>
     * <table class="compact" summary="Related units" style="margin-left:30px; line-height:1.25">
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #LUX} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table></div>
     *
     * @since 0.8
     */
    public static final Unit<Illuminance> LUX;

    /**
     * The SI base unit for mass (kg).
     * The unlocalized name is “kilogram”.
     *
     * @since 0.8
     */
    public static final Unit<Mass> KILOGRAM;

    /**
     * The SI base unit for amount of substance (mol).
     * The unlocalized name is “mole”.
     *
     * @since 0.8
     */
    public static final Unit<AmountOfSubstance> MOLE;

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
     *
     * @since 0.3
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
     * This unity should not be confused with {@link #POINT}, which is approximatively equal to 1/72 of inch.
     *
     * @see #POINT
     */
    public static final Unit<Dimensionless> PIXEL;

    /**
     * Sets to {@code true} by the static initializer after the initialization has been completed.
     * This is a safety against unexpected changes in the {@link UnitRegistry#HARD_CODED} map.
     *
     * <p>We use here a "lazy final initialization" pattern. We rely on the fact that this field is
     * initialized to {@code true} only at the end of the following static initializer. All methods
     * invoked in the static initializer will see the default value, which is {@code false}, until
     * the initializer fully completed. While apparently dangerous, this behavior is actually documented
     * in <a href="http://docs.oracle.com/javase/specs/jls/se8/html/jls-12.html#jls-12.4.1">section 12.4.1
     * of Java language specification</a>:</p>
     *
     * <blockquote>The fact that initialization code is unrestricted allows examples to be constructed where
     * the value of a class variable can be observed when it still has its initial default value, before its
     * initializing expression is evaluated, but such examples are rare in practice. (…snip…) The full power
     * of the Java programming language is available in these initializers; programmers must exercise some care.
     * This power places an extra burden on code generators, but this burden would arise in any case because
     * the Java programming language is concurrent.</blockquote>
     */
    static final boolean initialized;
    static {
        final UnitDimension length        = new UnitDimension('L');
        final UnitDimension mass          = new UnitDimension('M');
        final UnitDimension time          = new UnitDimension('T');
        final UnitDimension current       = new UnitDimension('I');
        final UnitDimension temperature   = new UnitDimension('Θ');
        final UnitDimension amount        = new UnitDimension('N');
        final UnitDimension luminous      = new UnitDimension('J');
        final UnitDimension area          = length.pow(2);
        final UnitDimension speed         = length.divide(time);
        final UnitDimension force         = mass.multiply(speed).divide(time);
        final UnitDimension energy        = force.multiply(length);
        final UnitDimension power         = energy.divide(time);
        final UnitDimension charge        = current.multiply(time);
        final UnitDimension potential     = power.divide(current);
        final UnitDimension magneticFlux  = potential.multiply(time);
        final UnitDimension pressure      = force.divide(area);
        final UnitDimension dimensionless = UnitDimension.NONE;
        /*
         * Base, derived or alternate units that we need to reuse more than once in this static initializer.
         */
        final SystemUnit<Length>        m   = add(Length.class,        length,        "m",   UnitRegistry.SI, Constants.EPSG_METRE);
        final SystemUnit<Area>          m2  = add(Area.class,          area,          "m²",  UnitRegistry.SI, (short) 0);
        final SystemUnit<Time>          s   = add(Time.class,          time,          "s",   UnitRegistry.SI, (short) 1040);
        final SystemUnit<Temperature>   K   = add(Temperature.class,   temperature,   "K",   UnitRegistry.SI, (short) 0);
        final SystemUnit<Speed>         mps = add(Speed.class,         speed,         "m∕s", UnitRegistry.SI, (short) 1026);
        final SystemUnit<Pressure>      Pa  = add(Pressure.class,      pressure,      "Pa",  UnitRegistry.SI, (short) 0);
        final SystemUnit<Angle>         rad = add(Angle.class,         dimensionless, "rad", UnitRegistry.SI, (short) 9101);
        final SystemUnit<Dimensionless> one = add(Dimensionless.class, dimensionless, "",    UnitRegistry.SI, (short) 9201);
        /*
         * All SI prefix to be used below, with additional converters to be used more than once.
         */
        final LinearConverter nano  = LinearConverter.forPrefix('n');
        final LinearConverter micro = LinearConverter.forPrefix('µ');
        final LinearConverter milli = LinearConverter.forPrefix('m');
        final LinearConverter centi = LinearConverter.forPrefix('c');
        final LinearConverter hecto = LinearConverter.forPrefix('h');
        final LinearConverter kilo  = LinearConverter.forPrefix('k');
        final LinearConverter ten4  = LinearConverter.scale(10000, 1);
        /*
         * All Unit<Angle>.
         * 20 is the greatest common denominator between 180 and 200. The intend is to have arguments as small
         * as possible in the call to the scale(double, double) method, while keeping the right side integer.
         * Staying closer to zero during conversions helo to reduce rounding errors.
         */
        rad.related(4);
        RADIAN      = rad;
        GRAD        = add(rad, LinearConverter.scale(Math.PI / 20, 200       / 20), "grad", UnitRegistry.OTHER,    (short) 9105);
        DEGREE      = add(rad, LinearConverter.scale(Math.PI / 20, 180       / 20), "°",    UnitRegistry.ACCEPTED, Constants.EPSG_PARAM_DEGREES);
        ARC_MINUTE  = add(rad, LinearConverter.scale(Math.PI / 20, 180*60    / 20), "′",    UnitRegistry.ACCEPTED, (short) 9103);
        ARC_SECOND  = add(rad, LinearConverter.scale(Math.PI / 20, 180*60*60 / 20), "″",    UnitRegistry.ACCEPTED, (short) 9104);
        MICRORADIAN = add(rad, micro,                                               "µrad", UnitRegistry.SI,       (short) 9109);
        /*
         * All Unit<Length>.
         */
        m.related(7);
        METRE          = m;
        NANOMETRE      = add(m, nano,                                     "nm",    UnitRegistry.SI,       (short) 0);
        MILLIMETRE     = add(m, milli,                                    "mm",    UnitRegistry.SI,       (short) 1025);
        CENTIMETRE     = add(m, centi,                                    "cm",    UnitRegistry.SI,       (short) 1033);
        KILOMETRE      = add(m, kilo,                                     "km",    UnitRegistry.SI,       (short) 9036);
        NAUTICAL_MILE  = add(m, LinearConverter.scale(   1852,        1), "M",     UnitRegistry.OTHER,    (short) 9030);
        STATUTE_MILE   = add(m, LinearConverter.scale(1609344,      100), "mi",    UnitRegistry.IMPERIAL, (short) 9093);
        US_SURVEY_FOOT = add(m, LinearConverter.scale(   1200,     3937), "ftUS",  UnitRegistry.OTHER,    (short) 9003);
        CLARKE_FOOT    = add(m, LinearConverter.scale(3047972654d, 1E10), "ftCla", UnitRegistry.OTHER,    (short) 9005);
        FOOT           = add(m, LinearConverter.scale(   3048,    10000), "ft",    UnitRegistry.IMPERIAL, (short) 9002);
        INCH           = add(m, LinearConverter.scale(    254,    10000), "in",    UnitRegistry.IMPERIAL, (short) 0);
        POINT          = add(m, LinearConverter.scale( 996264, 72000000), "pt",    UnitRegistry.OTHER,    (short) 0);
        /*
         * All Unit<Time>.
         */
        s.related(5);
        SECOND         = s;
        MILLISECOND    = add(s, milli, "ms", UnitRegistry.SI, (short) 0);
        MINUTE         = add(s, LinearConverter.scale(         60,      1), "min", UnitRegistry.ACCEPTED, (short) 0);
        HOUR           = add(s, LinearConverter.scale(      60*60,      1), "h",   UnitRegistry.ACCEPTED, (short) 0);
        DAY            = add(s, LinearConverter.scale(   24*60*60,      1), "d",   UnitRegistry.ACCEPTED, (short) 0);
        WEEK           = add(s, LinearConverter.scale( 7*24*60*60,      1), "wk",  UnitRegistry.OTHER,    (short) 0);
        TROPICAL_YEAR  = add(s, LinearConverter.scale(31556925445.0, 1000), "a",   UnitRegistry.OTHER,    (short) 1029);
        /*
         * All Unit<Speed>.
         */
        mps.related(1);
        METRES_PER_SECOND   = mps;
        KILOMETRES_PER_HOUR = add(mps, LinearConverter.scale(6, 100), "km∕h", UnitRegistry.ACCEPTED, (short) 0);
        /*
         * All Unit<Pressure>.
         */
        Pa.related(3);
        PASCAL      = Pa;
        HECTOPASCAL = add(Pa,  hecto,                            "hPa",  UnitRegistry.SI,    (short) 0);
        DECIBAR     = add(Pa,  ten4,                             "dbar", UnitRegistry.OTHER, (short) 0);
        BAR         = add(Pa,  LinearConverter.scale(100000, 1), "bar",  UnitRegistry.OTHER, (short) 0);
        ATMOSPHERE  = add(Pa,  LinearConverter.scale(101325, 1), "atm",  UnitRegistry.OTHER, (short) 0);
        /*
         * All Unit<Temperature>.
         */
        K.related(1);
        KELVIN     = K;
        CELSIUS    = add(K, LinearConverter.offset(  27315, 100), "°C", UnitRegistry.SI,    (short) 0);
        FAHRENHEIT = add(K, new LinearConverter(100, 45967, 180), "°F", UnitRegistry.OTHER, (short) 0);
        /*
         * Electricity and magnetism.
         */
        AMPERE  = add(ElectricCurrent.class,     current,                      "A",  UnitRegistry.SI, (short) 0);
        COULOMB = add(ElectricCharge.class,      charge,                       "C",  UnitRegistry.SI, (short) 0);
        VOLT    = add(ElectricPotential.class,   potential,                    "V",  UnitRegistry.SI, (short) 0);
        FARAD   = add(ElectricCapacitance.class, charge.divide(potential),     "F",  UnitRegistry.SI, (short) 0);
        SIEMENS = add(ElectricConductance.class, current.divide(potential),    "S",  UnitRegistry.SI, (short) 0);
        OHM     = add(ElectricResistance.class,  potential.divide(current),    "Ω",  UnitRegistry.SI, (short) 0);
        WEBER   = add(MagneticFlux.class,        magneticFlux,                 "Wb", UnitRegistry.SI, (short) 0);
        TESLA   = add(MagneticFluxDensity.class, magneticFlux.divide(area),    "T",  UnitRegistry.SI, (short) 0);
        HENRY   = add(ElectricInductance.class,  magneticFlux.divide(current), "H",  UnitRegistry.SI, (short) 0);
        /*
         * Other units.
         */
        SQUARE_METRE = m2;
        HECTARE      = add(m2,  ten4,                                      "ha",  UnitRegistry.ACCEPTED, (short) 0);
        CUBIC_METRE  = add(Volume.class,            length.pow(3),         "m³",  UnitRegistry.SI,       (short) 0);
        HERTZ        = add(Frequency.class,         time.pow(-1),          "Hz",  UnitRegistry.SI,       (short) 0);
        KILOGRAM     = add(Mass.class,              mass,                  "kg",  UnitRegistry.SI,       (short) 0);
        NEWTON       = add(Force.class,             force,                 "N",   UnitRegistry.SI,       (short) 0);
        JOULE        = add(Energy.class,            energy,                "J",   UnitRegistry.SI,       (short) 0);
        WATT         = add(Power.class,             power,                 "W",   UnitRegistry.SI,       (short) 0);
        LUX          = add(Illuminance.class,       luminous.divide(area), "lx",  UnitRegistry.SI,       (short) 0);
        LUMEN        = add(LuminousFlux.class,      luminous,              "lm",  UnitRegistry.SI,       (short) 0);
        CANDELA      = add(LuminousIntensity.class, luminous,              "cd",  UnitRegistry.SI,       (short) 0);    // Must be after Lumen.
        MOLE         = add(AmountOfSubstance.class, amount,                "mol", UnitRegistry.SI,       (short) 0);
        STERADIAN    = add(SolidAngle.class,        dimensionless,         "sr",  UnitRegistry.SI,       (short) 0);
        /*
         * All Unit<Dimensionless>.
         */
        PERCENT = add(one, centi,                         "%",     UnitRegistry.OTHER, (short) 0);
        PPM     = add(one, micro,                         "ppm",   UnitRegistry.OTHER, (short) 9202);
        PSU     = add(Dimensionless.class, dimensionless, "psu",   UnitRegistry.OTHER, (short) 0);
        SIGMA   = add(Dimensionless.class, dimensionless, "sigma", UnitRegistry.OTHER, (short) 0);
        PIXEL   = add(Dimensionless.class, dimensionless, "px",    UnitRegistry.OTHER, (short) 0);
        UNITY   = UnitRegistry.init(one);  // Must be last in order to take precedence over all other units associated to UnitDimension.NONE.

        UnitRegistry.alias(UNITY,       Short.valueOf((short) 9203));
        UnitRegistry.alias(DEGREE,      Short.valueOf(Constants.EPSG_AXIS_DEGREES));
        UnitRegistry.alias(ARC_MINUTE,  "'");
        UnitRegistry.alias(ARC_SECOND, "\"");
        UnitRegistry.alias(KELVIN,      "K");       // Ordinary "K" letter (not the dedicated Unicode character).
        UnitRegistry.alias(CELSIUS,     "℃");
        UnitRegistry.alias(CELSIUS,   "Cel");
        UnitRegistry.alias(FAHRENHEIT,  "℉");
        UnitRegistry.alias(GRAD,      "gon");
        UnitRegistry.alias(HECTARE,   "hm²");
        UnitRegistry.alias(UNITY,       "1");

        initialized = true;
    }

    /**
     * Invoked by {@code Units} static class initializer for registering SI base and derived units.
     * This method shall be invoked in a single thread by the {@code Units} class initializer only.
     */
    private static <Q extends Quantity<Q>> SystemUnit<Q> add(Class<Q> quantity, UnitDimension dimension, String symbol, byte scope, short epsg) {
        return UnitRegistry.init(new SystemUnit<>(quantity, dimension, symbol, scope, epsg));
    }

    /**
     * Invoked by {@code Units} static class initializer for registering SI conventional units.
     * This method shall be invoked in a single thread by the {@code Units} class initializer only.
     */
    private static <Q extends Quantity<Q>> ConventionalUnit<Q> add(SystemUnit<Q> target, UnitConverter toTarget, String symbol, byte scope, short epsg) {
        final ConventionalUnit<Q> unit = UnitRegistry.init(new ConventionalUnit<>(target, toTarget, symbol, scope, epsg));
        final ConventionalUnit<Q>[] related = target.related;
        if (related != null && unit.scope != UnitRegistry.SI) {
            // Search first empty slot. This algorithm is inefficient, but the length of those arrays is small (<= 6).
            int i = 0;
            while (related[i] != null) i++;
            related[i] = unit;
        }
        return unit;
    }

    /**
     * Returns the system unit for the given dimension, or {@code null} if none.
     * Note that this method can not distinguish the different kinds of dimensionless units.
     * If the symbol or the quantity type is known, use {@link #get(String)} or {@link #get(Class)} instead.
     *
     * <p><b>Implementation note:</b> this method must be defined in this {@code Units} class
     * in order to force a class initialization before use.</p>
     */
    static SystemUnit<?> get(final Dimension dim) {
        return (SystemUnit<?>) UnitRegistry.get(dim);
    }

    /**
     * Returns the system unit for the given quantity, or {@code null} if none.
     *
     * <p><b>Implementation note:</b> this method must be defined in this {@code Units} class
     * in order to force a class initialization before use.</p>
     */
    @SuppressWarnings("unchecked")
    static <Q extends Quantity<Q>> SystemUnit<Q> get(final Class<Q> type) {
        return (SystemUnit<Q>) UnitRegistry.get(type);
    }

    /**
     * Returns the system unit for the given symbol, or {@code null} if none.
     * This method does not perform any parsing (prefix, exponents, <i>etc</i>).
     * It is only for getting one of the pre-defined constants, for example after deserialization.
     *
     * <p><b>Implementation note:</b> this method must be defined in this {@code Units} class
     * in order to force a class initialization before use.</p>
     */
    @SuppressWarnings("unchecked")
    static Unit<?> get(final String symbol) {
        return (Unit<?>) UnitRegistry.get(symbol);
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
     * Multiplies the given unit by the given ratio. For example multiplying {@link #CENTIMETRE} by 254/100 gives
     * {@link #INCH}. Invoking this method is equivalent to invoking <code>{@linkplain Unit#multiply(double)
     * Unit.multiply}(numerator / denominator)</code> except that the use of a ration of integer values help
     * Apache SIS to improve accuracy when more than one arithmetic operation are chained.
     *
     * @param  <Q>          the quantity measured by the unit.
     * @param  unit         the unit to multiply.
     * @param  numerator    the numerator of the multiplication factor.
     * @param  denominator  the denominator of the multiplication factor.
     * @return the unit multiplied by the given factor.
     *
     * @since 0.8
     */
    public static <Q extends Quantity<Q>> Unit<Q> multiply(Unit<Q> unit, double numerator, double denominator) {
        return unit.transform(LinearConverter.scale(numerator, denominator));
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
     *
     * @deprecated Replaced by Apache SIS implementation of {@link Unit#multiply(double)}.
     */
    @Deprecated
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
        return unit;
    }

    /**
     * Returns the factor by which to multiply the standard unit in order to get the given unit.
     * The "standard" unit is usually the SI unit on which the given unit is based, as given by
     * {@link Unit#getSystemUnit()}.
     *
     * <div class="note"><b>Example:</b>
     * if the given unit is {@link #KILOMETRE}, then this method returns 1000 since a measurement in kilometres
     * must be multiplied by 1000 in order to give the equivalent measurement in the "standard" units
     * (here {@link #METRE}).</div>
     *
     * If the given unit is {@code null} or if the conversion to the "standard" unit can not be expressed
     * by a single multiplication factor, then this method returns {@link Double#NaN}.
     *
     * @param  <Q>   the quantity measured by the unit, or {@code null}.
     * @param  unit  the unit for which we want the multiplication factor to standard unit, or {@code null}.
     * @return the factor by which to multiply a measurement in the given unit in order to get an equivalent
     *         measurement in the standard unit, or NaN if the conversion can not be expressed by a scale factor.
     */
    public static <Q extends Quantity<Q>> double toStandardUnit(final Unit<Q> unit) {
        if (unit != null) {
            final UnitConverter converter = unit.getConverterTo(unit.getSystemUnit());
            if (converter.isLinear() && converter.convert(0) == 0) {
                // Above check for converter(0) is a paranoiac check since
                // JSR-363 said that a "linear" converter has no offset.
                return converter.convert(1);
            }
        }
        return Double.NaN;
    }

    /**
     * Returns the coefficients of the given converter expressed as a polynomial equation.
     * This method returns the first of the following choices that apply:
     *
     * <ul>
     *   <li>If the given converter {@linkplain UnitConverter#isIdentity() is identity}, returns an empty array.</li>
     *   <li>If the given converter shifts the values without scaling them (for example the conversion from Kelvin to
     *       Celsius degrees), returns an array of length 1 containing only the offset.</li>
     *   <li>If the given converter scales the values (optionally in addition to shifting them), returns an array of
     *       length 2 containing the offset and scale factor, in that order.</li>
     * </ul>
     *
     * This method returns {@code null} if it can not get the polynomial equation coefficients from the given converter.
     *
     * @param  converter  the converter from which to get the coefficients of the polynomial equation, or {@code null}.
     * @return the polynomial equation coefficients (may be any length, including zero), or {@code null} if the given
     *         converter is {@code null} or if this method can not get the coefficients.
     *
     * @since 0.8
     */
    @SuppressWarnings("fallthrough")
    public static Number[] coefficients(final UnitConverter converter) {
        if (converter != null) {
            if (converter instanceof AbstractConverter) {
                return ((AbstractConverter) converter).coefficients();
            }
            if (converter.isIdentity()) {
                return new Number[0];
            }
            if (converter.isLinear()) {
                final double offset = converter.convert(0);  // Should be zero as per JSR-363 specification, but we are paranoiac.
                final double scale  = converter.convert(1) - offset;
                final Number[] c = new Number[(scale != 1) ? 2 : (offset != 0) ? 1 : 0];
                switch (c.length) {
                    case 2: c[1] = scale;       // Fall through
                    case 1: c[0] = offset;
                    case 0: break;
                }
                return c;
            }
        }
        return null;
    }

    /**
     * Returns the derivative of the given converter at the given value,
     * or {@code NaN} if this method can not compute it.
     *
     * @param  converter  the converter for which we want the derivative at a given point, or {@code null}.
     * @param  value      the point at which to compute the derivative.
     * @return the derivative at the given point, or {@code NaN} if unknown.
     *
     * @see org.apache.sis.referencing.operation.transform.AbstractMathTransform#derivative(DirectPosition)
     */
    public static double derivative(final UnitConverter converter, final double value) {
        return AbstractConverter.derivative(converter, value);
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
        /*
         * The switch for the SexagesimalConverter cases are needed since we did not put those units
         * in the UnitRegistry map for reducing a little bit class loading in the common cases where
         * those units are not needed. Other cases are redundant with the UnitRegistry check, but we
         * add them opportunistically as a shortcut since those units are frequently used.
         */
        switch (code) {
            case Constants.EPSG_PARAM_DEGREES:  // Fall through
            case Constants.EPSG_AXIS_DEGREES:   return DEGREE;
            case Constants.EPSG_METRE:          return METRE;

            case 9107: // Fall through
            case 9108: return SexagesimalConverter.DMS_SCALED;
            case 9110: return SexagesimalConverter.DMS;
            case 9111: return SexagesimalConverter.DM;
            case 9203: // Fall through
            case 9201: return UNITY;
            default: {
                return (code > 0 && code <= Short.MAX_VALUE) ? (Unit<?>) UnitRegistry.get((short) code) : null;
            }
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
     *
     * @see org.apache.sis.referencing.cs.CoordinateSystems#getEpsgCode(Unit, AxisDirection...)
     */
    public static Integer getEpsgCode(Unit<?> unit, final boolean inAxis) {
        if (unit != null) {
            if (!(unit instanceof AbstractUnit<?>)) {
                unit = get(unit.getSymbol());
                if (!(unit instanceof AbstractUnit<?>)) {
                    return null;
                }
            }
            short code = ((AbstractUnit<?>) unit).epsg;
            if (code != 0) {
                if (inAxis && code == Constants.EPSG_PARAM_DEGREES) {
                    code = Constants.EPSG_AXIS_DEGREES;
                }
                return Integer.valueOf(code);
            }
        }
        return null;
    }
}
