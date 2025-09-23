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
import javax.measure.Quantity;
import javax.measure.format.MeasurementParseException;
import javax.measure.quantity.*;
import javax.measure.quantity.Angle;                // Because of name collision with Angle in this SIS package.
import org.opengis.geometry.DirectPosition;         // For javadoc
import org.opengis.referencing.cs.AxisDirection;    // For javadoc
import org.apache.sis.util.Static;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Constants;
import static org.apache.sis.measure.UnitRegistry.SI;
import static org.apache.sis.measure.UnitRegistry.ACCEPTED;
import static org.apache.sis.measure.UnitRegistry.CGS;
import static org.apache.sis.measure.UnitRegistry.IMPERIAL;
import static org.apache.sis.measure.UnitRegistry.OTHER;
import static org.apache.sis.measure.UnitRegistry.PREFIXABLE;
import static org.apache.sis.util.internal.shared.Constants.SECONDS_PER_DAY;
import static org.apache.sis.util.internal.shared.Constants.MILLIS_PER_TROPICAL_YEAR;


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
 *   <tr><td>{@link Time}</td>              <td>(T)</td> <td>{@link #SECOND}</td>   <td>{@link #NANOSECOND}, {@link #MILLISECOND}, {@link #MINUTE}, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}</td></tr>
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
 * @author  Alexis Manin (Geomatys)
 * @version 1.5
 * @since   0.3
 */
public final class Units extends Static {
    /**
     * Unit of measurement defined as 10<sup>-9</sup> metres (1 nm). This unit is often used in
     * {@linkplain org.apache.sis.metadata.iso.content.DefaultBand#getBoundUnits() wavelength measurements}.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE}
     * and the unlocalized name is "nanometre".
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em"><u>{@code NANOMETRE}</u>, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, <u>{@code MILLIMETRE}</u>, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Length> MILLIMETRE;

    /**
     * Unit of measurement defined as 0.01 metres (1 cm).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “centimetre” and the identifier is EPSG:1033.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, <u>{@code CENTIMETRE}</u>, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Length> CENTIMETRE;

    /**
     * The SI base unit for distances (m).
     * The unlocalized name is “metre” and the identifier is EPSG:9001.
     * This is the base of all other {@linkplain #isLinear(Unit) linear} units.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <u><b>{@code METRE}</b></u>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Length> METRE;

    /**
     * Unit of measurement defined as 1000 metres (1 km).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “kilometre” and the identifier is EPSG:9036.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, <u>{@code KILOMETRE}</u>.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #KILOMETRES_PER_HOUR}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Length> KILOMETRE;

    /**
     * Unit of measurement defined as exactly 1852 metres (1 M).
     * This is approximately the distance between two parallels of latitude
     * separated by one {@linkplain #ARC_MINUTE arc-minute}.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “nautical mile” and the identifier is EPSG:9030.
     *
     * <p>There is no internationally agreed symbol for nautical mile. Apache SIS uses “M” in agreement with the
     * International Hydrographic Organization (IHO) and the International Bureau of Weights and Measures (BIPM).
     * But “NM” and “nmi” are also in use.</p>
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, <u>{@code NAUTICAL_MILE}</u>.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #KILOMETRES_PER_HOUR}.</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, <u>{@code STATUTE_MILE}</u>, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #KILOMETRES_PER_HOUR}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Length> STATUTE_MILE;

    /**
     * Unit of measurement approximately equals to 0.3048006096… metres.
     * The legal definition is exactly 12/39.37 metres.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “US survey foot” and the identifier is EPSG:9003.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, <u>{@code US_SURVEY_FOOT}</u>, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Length> US_SURVEY_FOOT;

    /**
     * Unit of measurement defined as 0.3047972654 metres.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “Clarke’s foot” and the identifier is EPSG:9005.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, {@link #FOOT}, <u>{@code CLARKE_FOOT}</u>, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Length> CLARKE_FOOT;

    /**
     * Unit of measurement defined as exactly 0.3048 metres (1 ft).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE},
     * the unlocalized name is “foot” and the identifier is EPSG:9002.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, {@link #INCH}, <u>{@code FOOT}</u>, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Length> FOOT;

    /**
     * Unit of measurement defined as 2.54 centimetres (1 in).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRE}
     * and the unlocalized name is “inch”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em">{@link #POINT}, <u>{@code INCH}</u>, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI length units:</td> <td style="word-spacing:1em">{@link #NANOMETRE}, {@link #MILLIMETRE}, {@link #CENTIMETRE}, <b>{@link #METRE}</b>, {@link #KILOMETRE}.</td></tr>
     *   <tr><td>Non-SI units:</td>    <td style="word-spacing:1em"><u>{@code POINT}</u>, {@link #INCH}, {@link #FOOT}, {@link #CLARKE_FOOT}, {@link #US_SURVEY_FOOT}, {@link #STATUTE_MILE}, {@link #NAUTICAL_MILE}.</td></tr>
     *   <tr><td>Derived units:</td>   <td style="word-spacing:1em">{@link #SQUARE_METRE}, {@link #CUBIC_METRE}, {@link #METRES_PER_SECOND}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Length> POINT;

    /**
     * The SI derived unit for area (m²).
     * The unlocalized name is “square metre”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI area units:</td> <td style="word-spacing:1em"><u><b>{@code SQUARE_METRE}</b></u>, {@link #HECTARE}.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #CUBIC_METRE}.</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI area units:</td> <td style="word-spacing:1em"><b>{@link #SQUARE_METRE}</b>, <u>{@code HECTARE}</u>.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #CUBIC_METRE}.</td></tr>
     * </table>
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
     * The unit for litre volume (L, l or ℓ).
     * The unlocalized name is “litre”.
     *
     * @since 0.8
     */
    public static final Unit<Volume> LITRE;

    /**
     * The SI unit for solid angles (sr).
     * The unlocalized name is “steradian”.
     *
     * @since 0.8
     */
    public static final Unit<SolidAngle> STERADIAN;

    /**
     * Unit of measurement defined as 10<sup>-6</sup> radians (1 µrad).
     * The distance of one microradian of latitude on Earth is approximately 2 millimetres.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “microradian” and the identifier is EPSG:9109.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI angle units:</td> <td style="word-spacing:1em"><u>{@code MICRORADIAN}</u>, <b>{@link #RADIAN}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>   <td style="word-spacing:1em">{@link #DEGREE}, {@link #ARC_MINUTE}, {@link #ARC_SECOND}, {@link #GRAD}.</td></tr>
     *   <tr><td>Derived units:</td>  <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI angle units:</td> <td style="word-spacing:1em">{@link #MICRORADIAN}, <u><b>{@code RADIAN}</b></u>.</td></tr>
     *   <tr><td>Non-SI units:</td>   <td style="word-spacing:1em">{@link #DEGREE}, {@link #ARC_MINUTE}, {@link #ARC_SECOND}, {@link #GRAD}.</td></tr>
     *   <tr><td>Derived units:</td>  <td style="word-spacing:1em">{@link #STERADIAN}, {@link #RADIANS_PER_SECOND}.</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI angle units:</td> <td style="word-spacing:1em">{@link #MICRORADIAN}, <b>{@link #RADIAN}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>   <td style="word-spacing:1em"><u>{@code DEGREE}</u>, {@link #ARC_MINUTE}, {@link #ARC_SECOND}, {@link #GRAD}.</td></tr>
     *   <tr><td>Derived units:</td>  <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Angle> DEGREE;

    /**
     * Unit of measurement defined as 1/60 degree (1′).
     * The distance of one arc-minute of latitude on Earth is approximately 1852 metres
     * (one {@linkplain #NAUTICAL_MILE nautical mile}).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “arc-minute” and the identifier is EPSG:9103.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI angle units:</td> <td style="word-spacing:1em">{@link #MICRORADIAN}, <b>{@link #RADIAN}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>   <td style="word-spacing:1em">{@link #DEGREE}, <u>{@code ARC_MINUTE}</u>, {@link #ARC_SECOND}, {@link #GRAD}.</td></tr>
     *   <tr><td>Derived units:</td>  <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Angle> ARC_MINUTE;

    /**
     * Unit of measurement defined as 1/(60×60) degree (1″).
     * The distance of one arc-second of latitude on Earth is approximately 31 metres.
     * This unit of measurement is used for rotation terms in
     * {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa-Wolf parameters}.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #RADIAN},
     * the unlocalized name is “arc-second” and the identifier is EPSG:9104.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI angle units:</td> <td style="word-spacing:1em">{@link #MICRORADIAN}, <b>{@link #RADIAN}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>   <td style="word-spacing:1em">{@link #DEGREE}, {@link #ARC_MINUTE}, <u>{@code ARC_SECOND}</u>, {@link #GRAD}.</td></tr>
     *   <tr><td>Derived units:</td>  <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI angle units:</td> <td style="word-spacing:1em">{@link #MICRORADIAN}, <b>{@link #RADIAN}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>   <td style="word-spacing:1em">{@link #DEGREE}, {@link #ARC_MINUTE}, {@link #ARC_SECOND}, <u>{@code GRAD}</u>.</td></tr>
     *   <tr><td>Derived units:</td>  <td style="word-spacing:1em">{@link #STERADIAN}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Angle> GRAD;

    /**
     * Unit of measurement defined as 10<sup>-9</sup> seconds (1 ms).
     * This unit is useful for inter-operability with various methods from the standard Java library.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “nanosecond”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI time units:</td> <td style="word-spacing:1em"><u>{@code NANOSECOND}</u>, {@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #METRES_PER_SECOND}, {@link #HERTZ}, {@link #BECQUEREL}.</td></tr>
     * </table>
     *
     * @see java.util.concurrent.TimeUnit#NANOSECONDS
     *
     * @since 1.5
     */
    public static final Unit<Time> NANOSECOND;

    /**
     * Unit of measurement defined as 10<sup>-3</sup> seconds (1 ms).
     * This unit is useful for inter-operability with various methods from the standard Java library.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “millisecond”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI time units:</td> <td style="word-spacing:1em"><u>{@link #NANOSECOND}, {@code MILLISECOND}</u>, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #METRES_PER_SECOND}, {@link #HERTZ}, {@link #BECQUEREL}.</td></tr>
     * </table>
     *
     * @see java.util.concurrent.TimeUnit#MILLISECONDS
     *
     * @since 0.3
     */
    public static final Unit<Time> MILLISECOND;

    /**
     * The SI base unit for durations (s).
     * The unlocalized name is “second” and the identifier is EPSG:1040.
     * This is the base of all other {@linkplain #isTemporal(Unit) temporal} units.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI time units:</td> <td style="word-spacing:1em">{@link #NANOSECOND}, {@link #MILLISECOND}, <u><b>{@link #SECOND}</b></u>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #METRES_PER_SECOND}, {@link #HERTZ}, {@link #BECQUEREL}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Time> SECOND;

    /**
     * Unit of measurement defined as 60 seconds (1 min).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “minute”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI time units:</td> <td style="word-spacing:1em">{@link #NANOSECOND}, {@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em"><u>{@code MINUTE}</u>, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #METRES_PER_SECOND}, {@link #HERTZ}, {@link #BECQUEREL}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Time> MINUTE;

    /**
     * Unit of measurement defined as 60×60 seconds (1 h).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “hour”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI time units:</td> <td style="word-spacing:1em">{@link #NANOSECOND}, {@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em">{@link #MINUTE}, <u>{@code HOUR}</u>, {@link #DAY}, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}, {@link #HERTZ}, {@link #BECQUEREL}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Time> HOUR;

    /**
     * Unit of measurement defined as 24×60×60 seconds (1 d).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “day”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI time units:</td> <td style="word-spacing:1em">{@link #NANOSECOND}, {@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, <u>{@code DAY}</u>, {@link #WEEK}, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}, {@link #HERTZ}, {@link #BECQUEREL}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Time> DAY;

    /**
     * Unit of measurement defined as 7 days (1 wk).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND}
     * and the unlocalized name is “week”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI time units:</td> <td style="word-spacing:1em">{@link #NANOSECOND}, {@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, {@link #DAY}, <u>{@link #WEEK}</u>, {@link #TROPICAL_YEAR}.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}, {@link #HERTZ}, {@link #BECQUEREL}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Time> WEEK;

    /**
     * Unit of measurement approximately equals to 365.24219 days (1 a).
     * This is defined by the International Union of Geological Sciences (IUGS) as exactly 31556925.445 seconds,
     * taken as the length of the tropical year in the year 2000.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #SECOND},
     * the unlocalized name is “year” and the identifier is EPSG:1029.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI time units:</td> <td style="word-spacing:1em">{@link #NANOSECOND}, {@link #MILLISECOND}, <b>{@link #SECOND}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em">{@link #MINUTE}, {@link #HOUR}, {@link #DAY}, {@link #WEEK}, <u>{@code TROPICAL_YEAR}</u>.</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}, {@link #HERTZ}, {@link #BECQUEREL}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Time> TROPICAL_YEAR;

    /**
     * The SI derived unit for frequency (Hz).
     * One hertz is equal to one cycle per {@linkplain #SECOND second}.
     * The unlocalized name is “hertz”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td><td style="word-spacing:0.5em">{@link #SECOND}<sup>-1</sup></td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Frequency> HERTZ;

    /**
     * The unit for angular velocity (rad/s).
     * The identifier is EPSG:1035.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #RADIAN} ∕ {@link #SECOND}</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<AngularVelocity> RADIANS_PER_SECOND;

    /**
     * The SI derived unit for speed (m/s).
     * The unlocalized name is “metres per second” and the identifier is EPSG:1026.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI speed units:</td> <td style="word-spacing:1em"><u><b>{@code METRES_PER_SECOND}</b></u>.</td></tr>
     *   <tr><td>Non-SI units:</td>   <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}, {@link #KNOT}.</td></tr>
     *   <tr><td>Components:</td>     <td style="word-spacing:0.5em">{@link #METRE} ∕ {@link #SECOND}</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Speed> METRES_PER_SECOND;

    /**
     * Unit of measurement defined as 1/3.6 metres per second (1 km/h).
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRES_PER_SECOND}
     * and the unlocalized name is “kilometres per hour”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI speed units:</td> <td style="word-spacing:1em"><b>{@link #METRES_PER_SECOND}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>   <td style="word-spacing:1em"><u>{@code KILOMETRES_PER_HOUR}</u>, {@link #KNOT}.</td></tr>
     *   <tr><td>Components:</td>     <td style="word-spacing:0.5em">{@link #KILOMETRE} ∕ {@link #HOUR}</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Speed> KILOMETRES_PER_HOUR;

    /**
     * Unit of measurement defined as 1.852 km/h.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRES_PER_SECOND}
     * and the unlocalized name is “knot”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI speed units:</td> <td style="word-spacing:1em"><b>{@link #METRES_PER_SECOND}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>   <td style="word-spacing:1em">{@link #KILOMETRES_PER_HOUR}, <u>{@code #KNOT}</u>.</td></tr>
     *   <tr><td>Components:</td>     <td style="word-spacing:0.5em">{@link #KILOMETRE} ∕ {@link #HOUR}</td></tr>
     * </table>
     *
     * @since 1.5
     */
    public static final Unit<Speed> KNOT;

    /**
     * The SI derived unit for acceleration (m/s²).
     * The unlocalized name is “metres per second squared”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI acceleration units:</td> <td style="word-spacing:1em"><u><b>{@code METRES_PER_SECOND_SQUARED}</b></u>.</td></tr>
     *   <tr><td>Non-SI units:</td>          <td style="word-spacing:1em">{@link #GAL}.</td></tr>
     *   <tr><td>Components:</td>            <td style="word-spacing:0.5em">{@link #METRES_PER_SECOND} ∕ {@link #SECOND}</td></tr>
     * </table>
     *
     * @since 1.2
     */
    public static final Unit<Acceleration> METRES_PER_SECOND_SQUARED;

    /**
     * Unit of measurement defined as 1/100 metres per second squared (1 cm/s²).
     * This is a CGS unit (not a SI unit) used in geodesy and geophysics to express acceleration due to gravity.
     * The {@linkplain ConventionalUnit#getSystemUnit() system unit} is {@link #METRES_PER_SECOND_SQUARED},
     * the symbol is "Gal" (upper-case first letter) and the unlocalized name is “gal” (lower-case letter).
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI acceleration units:</td> <td style="word-spacing:1em"><u><b>{@link #METRES_PER_SECOND_SQUARED}</b></u>.</td></tr>
     *   <tr><td>Non-SI units:</td>          <td style="word-spacing:1em">{@code GAL}.</td></tr>
     *   <tr><td>Components:</td>            <td style="word-spacing:0.5em">{@link #CENTIMETRE} ∕ {@link #SECOND}²</td></tr>
     * </table>
     *
     * @since 1.2
     */
    public static final Unit<Acceleration> GAL;

    /**
     * The SI derived unit for pressure (Pa).
     * One pascal is equal to 1 N/m².
     * Pressures are often used in {@linkplain org.apache.sis.referencing.crs.DefaultParametricCRS parametric CRS}
     * for height measurements on a vertical axis.
     * The unlocalized name is “pascal”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI pressure units:</td> <td style="word-spacing:1em"><u><b>{@code PASCAL}</b></u>, {@link #HECTOPASCAL}.</td></tr>
     *   <tr><td>Non-SI units:</td>      <td style="word-spacing:1em">{@link #DECIBAR}, {@link #BAR}, {@link #ATMOSPHERE}.</td></tr>
     *   <tr><td>Components:</td>        <td style="word-spacing:0.5em">{@link #NEWTON} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI pressure units:</td> <td style="word-spacing:1em"><b>{@link #PASCAL}</b>, <u>{@code HECTOPASCAL}</u>.</td></tr>
     *   <tr><td>Non-SI units:</td>      <td style="word-spacing:1em">{@link #DECIBAR}, {@link #BAR}, {@link #ATMOSPHERE}.</td></tr>
     *   <tr><td>Components:</td>        <td style="word-spacing:0.5em">{@link #NEWTON} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI pressure units:</td> <td style="word-spacing:1em"><b>{@link #PASCAL}</b>, {@link #HECTOPASCAL}.</td></tr>
     *   <tr><td>Non-SI units:</td>      <td style="word-spacing:1em"><u>{@code DECIBAR}</u>, {@link #BAR}, {@link #ATMOSPHERE}.</td></tr>
     *   <tr><td>Components:</td>        <td style="word-spacing:0.5em">{@link #NEWTON} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI pressure units:</td> <td style="word-spacing:1em"><b>{@link #PASCAL}</b>, {@link #HECTOPASCAL}.</td></tr>
     *   <tr><td>Non-SI units:</td>      <td style="word-spacing:1em">{@link #DECIBAR}, <u>{@code BAR}</u>, {@link #ATMOSPHERE}.</td></tr>
     *   <tr><td>Components:</td>        <td style="word-spacing:0.5em">{@link #NEWTON} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table>
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
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI pressure units:</td> <td style="word-spacing:1em"><b>{@link #PASCAL}</b>, {@link #HECTOPASCAL}.</td></tr>
     *   <tr><td>Non-SI units:</td>      <td style="word-spacing:1em">{@link #DECIBAR}, {@link #BAR}, <u>{@code ATMOSPHERE}</u>.</td></tr>
     *   <tr><td>Components:</td>        <td style="word-spacing:0.5em">{@link #NEWTON} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Pressure> ATMOSPHERE;

    /**
     * The SI derived unit for force (N).
     * One newton is the force required to give a mass of 1 kg an acceleration of 1 m/s².
     * The unlocalized name is “newton”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #KILOGRAM} ⋅ {@link #METRES_PER_SECOND} ∕ {@link #SECOND}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #PASCAL}, {@link #JOULE}, {@link #WATT}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Force> NEWTON;

    /**
     * The SI derived unit for energy (J).
     * The unlocalized name is “joule”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #NEWTON} ⋅ {@link #METRE}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #WATT}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Energy> JOULE;

    /**
     * The SI derived unit for power (W).
     * One watt is equal to one joule per second.
     * The unlocalized name is “watt”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #JOULE} ∕ {@link #SECOND}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #VOLT}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Power> WATT;

    /**
     * The SI derived unit for electric potential difference (V).
     * The unlocalized name is “volt”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #WATT} ∕ {@link #AMPERE}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #FARAD}, {@link #OHM}, {@link #SIEMENS}, {@link #WEBER}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<ElectricPotential> VOLT;

    /**
     * The SI base unit for electric current (A).
     * The unlocalized name is “ampere”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #COULOMB}, {@link #VOLT}, {@link #OHM}, {@link #SIEMENS}, {@link #HENRY}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<ElectricCurrent> AMPERE;

    /**
     * The SI derived unit for electric charge (C).
     * One coulomb is the charge transferred by a current of one ampere during one second.
     * The unlocalized name is “coulomb”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #AMPERE} ⋅ {@link #SECOND}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #FARAD}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<ElectricCharge> COULOMB;

    /**
     * The SI derived unit for electric capacitance (F).
     * The unlocalized name is “farad”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #COULOMB} ∕ {@link #VOLT}</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<ElectricCapacitance> FARAD;

    /**
     * The SI derived unit for electric resistance (Ω).
     * This is the inverse of electric conductance.
     * The unlocalized name is “ohm”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #VOLT} ∕ {@link #AMPERE}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #SIEMENS}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<ElectricResistance> OHM;

    /**
     * The SI derived unit for electric conductance (S).
     * This is the inverse of electric resistance.
     * The unlocalized name is “siemens”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #AMPERE} ∕ {@link #VOLT}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #OHM}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<ElectricConductance> SIEMENS;

    /**
     * The SI derived unit for magnetic flux (Wb).
     * The unlocalized name is “weber”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td>    <td style="word-spacing:0.5em">{@link #VOLT} ⋅ {@link #SECOND}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #TESLA}, {@link #HENRY}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<MagneticFlux> WEBER;

    /**
     * The SI derived unit for magnetic flux density (T).
     * The unlocalized name is “tesla”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #WEBER} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<MagneticFluxDensity> TESLA;

    /**
     * The SI derived unit for inductance (H).
     * The unlocalized name is “henry”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #WEBER} ∕ {@link #AMPERE}</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<ElectricInductance> HENRY;

    /**
     * The SI base unit for thermodynamic temperature (K).
     * The unlocalized name is “kelvin”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI temperature units:</td> <td style="word-spacing:1em"><u><b>{@code KELVIN}</b></u>.</td></tr>
     *   <tr><td>Non-SI units:</td>         <td style="word-spacing:1em">{@link #CELSIUS}, {@link #FAHRENHEIT}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Temperature> KELVIN;

    /**
     * Unit of measurement defined as the temperature in Kelvin minus 273.15.
     * The symbol is °C and the unlocalized name is “Celsius”.
     * Note that this is the only SI unit with an upper-case letter in its name.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI temperature units:</td> <td style="word-spacing:1em"><b>{@link #KELVIN}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>         <td style="word-spacing:1em"><u>{@code CELSIUS}</u>, {@link #FAHRENHEIT}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Temperature> CELSIUS;

    /**
     * Unit of measurement defined as 1.8 degree Celsius plus 32.
     * The symbol is °F and the unlocalized name is “Fahrenheit”
     * (note the upper-case "F" letter).
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI temperature units:</td> <td style="word-spacing:1em"><b>{@link #KELVIN}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>         <td style="word-spacing:1em">{@link #CELSIUS}, <u>{@code FAHRENHEIT}</u>.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Temperature> FAHRENHEIT;

    /**
     * The SI base unit for luminous intensity (cd).
     * The unlocalized name is “candela”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #LUMEN}, {@link #LUX}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<LuminousIntensity> CANDELA;

    /**
     * The SI derived unit for luminous flux (lm).
     * The unlocalized name is “lumen”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #CANDELA} ⋅ {@link #STERADIAN}</td></tr>
     *   <tr><td>Derived units:</td> <td style="word-spacing:1em">{@link #LUX}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<LuminousFlux> LUMEN;

    /**
     * The SI derived unit for illuminance (lx).
     * The unlocalized name is “lux”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td> <td style="word-spacing:0.5em">{@link #LUX} ∕ {@link #SQUARE_METRE}</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Illuminance> LUX;

    /**
     * A SI conventional unit for mass (g).
     * The unlocalized name is “gram”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI mass units:</td> <td style="word-spacing:1em"><u>{@code GRAM}</u>, <b>{@link #KILOGRAM}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em">{@link #TONNE}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Mass> GRAM;

    /**
     * The SI base unit for mass (kg).
     * The unlocalized name is “kilogram”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI mass units:</td> <td style="word-spacing:1em">{@link #GRAM}, <u><b>{@code KILOGRAM}</b></u>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em">{@link #TONNE}.</td></tr>
     * </table>
     *
     * @since 0.8
     */
    public static final Unit<Mass> KILOGRAM;

    /**
     * The SI convention unit for mass (t).
     * The unlocalized name is “tonne”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>SI mass units:</td> <td style="word-spacing:1em">{@link #GRAM}, <b>{@link #KILOGRAM}</b>.</td></tr>
     *   <tr><td>Non-SI units:</td>  <td style="word-spacing:1em"><u>{@code TONNE}</u>.</td></tr>
     * </table>
     *
     * @since 1.5
     */
    public static final Unit<Mass> TONNE;

    /**
     * The SI base unit for amount of substance (mol).
     * The unlocalized name is “mole”.
     *
     * @since 0.8
     */
    public static final Unit<AmountOfSubstance> MOLE;

    /**
     * The SI derived unit for radioactivity (Bq).
     * The unlocalized name is “becquerel”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td><td style="word-spacing:0.5em">{@link #SECOND}<sup>-1</sup></td></tr>
     *   <tr><td>Non-SI units:</td> <td style="word-spacing:1em">{@link #CURIE}.</td></tr>
     * </table>
     *
     * @since 1.4
     */
    public static final Unit<Radioactivity> BECQUEREL;

    /**
     * Unit of measurement defined as 3.7 × 10¹⁰ becquerel
     * The symbol is Ci and the unlocalized name is “curie”.
     *
     * <table class="compact" style="margin-left:30px; line-height:1.25">
     *   <caption>Related units</caption>
     *   <tr><td>Components:</td><td style="word-spacing:0.5em">{@link #SECOND}<sup>-1</sup></td></tr>
     *   <tr><td>SI radioactivity units:</td> <td style="word-spacing:1em"><b>{@link #BECQUEREL}</b>.</td></tr>
     * </table>
     *
     * @since 1.4
     */
    public static final Unit<Radioactivity> CURIE;

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
     * Sub-division of logarithm of ratio of the measured quantity to a reference quantity (dB).
     *
     * @since 1.0
     */
    public static final Unit<Dimensionless> DECIBEL;

    /**
     * Salinity measured using PSS-78. While this is a dimensionless measurement, the {@code "psu"} symbol
     * is sometimes added to PSS-78 measurement. However, this is officially discouraged.
     *
     * @since 0.8
     */
    public static final Unit<Salinity> PSU;

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
     * This unity should not be confused with {@link #POINT}, which is approximately equal to 1/72 of inch.
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
        final UnitDimension frequency     = time.pow(-1);
        final UnitDimension area          = length.pow(2);
        final UnitDimension speed         = length.divide(time);
        final UnitDimension acceleration  = speed.divide(time);
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
        final SystemUnit<Length>        m    = add(Length.class,        Scalar.Length::new,         length,        "m",    (byte) (SI | PREFIXABLE), Constants.EPSG_METRE);
        final SystemUnit<Area>          m2   = add(Area.class,          Scalar.Area::new,           area,          "m²",   (byte) (SI | PREFIXABLE), (short) 0);
        final SystemUnit<Volume>        m3   = add(Volume.class,        Scalar.Volume::new,         length.pow(3), "m³",   (byte) (SI | PREFIXABLE), (short) 0);
        final SystemUnit<Time>          s    = add(Time.class,          Scalar.Time::new,           time,          "s",    (byte) (SI | PREFIXABLE), (short) 1040);
        final SystemUnit<Temperature>   K    = add(Temperature.class,   Scalar.Temperature.FACTORY, temperature,   "K",    (byte) (SI | PREFIXABLE), (short) 0);
        final SystemUnit<Speed>         mps  = add(Speed.class,         Scalar.Speed::new,          speed,         "m∕s",  (byte) (SI | PREFIXABLE), (short) 1026);
        final SystemUnit<Acceleration>  mps2 = add(Acceleration.class,  Scalar.Acceleration::new,   acceleration,  "m∕s²", (byte) (SI | PREFIXABLE), (short) 0);
        final SystemUnit<Pressure>      Pa   = add(Pressure.class,      Scalar.Pressure::new,       pressure,      "Pa",   (byte) (SI | PREFIXABLE), (short) 0);
        final SystemUnit<Angle>         rad  = add(Angle.class,         Scalar.Angle::new,          dimensionless, "rad",  (byte) (SI | PREFIXABLE), (short) 9101);
        final SystemUnit<Dimensionless> one  = add(Dimensionless.class, Scalar.Dimensionless::new,  dimensionless, "",             SI,               (short) 9201);
        final SystemUnit<Mass>          kg   = add(Mass.class,          Scalar.Mass::new,           mass,          "kg",           SI,               (short) 0);
        /*
         * All SI prefix to be used below, with additional converters to be used more than once.
         */
        final LinearConverter nano  = Prefixes.converter('n');
        final LinearConverter micro = Prefixes.converter('µ');
        final LinearConverter milli = Prefixes.converter('m');
        final LinearConverter centi = Prefixes.converter('c');
        final LinearConverter hecto = Prefixes.converter('h');
        final LinearConverter kilo  = Prefixes.converter('k');
        final LinearConverter ten4  = LinearConverter.scale(10000, 1);
        /*
         * All Unit<Angle>.
         * 20 is the greatest common denominator between 180 and 200. The intent is to have arguments as small
         * as possible in the call to the scale(double, double) method, while keeping the right side integer.
         * Staying closer to zero during conversions helo to reduce rounding errors.
         */
        rad.related(4);
        RADIAN      = rad;
        GRAD        = add(rad, LinearConverter.scale(Math.PI / 20, 200       / 20), "grad", OTHER,    (short) 9105);
        DEGREE      = add(rad, LinearConverter.scale(Math.PI / 20, 180       / 20), "°",    ACCEPTED, Constants.EPSG_PARAM_DEGREES);
        ARC_MINUTE  = add(rad, LinearConverter.scale(Math.PI / 20, 180*60    / 20), "′",    ACCEPTED, (short) 9103);
        ARC_SECOND  = add(rad, LinearConverter.scale(Math.PI / 20, 180*60*60 / 20), "″",    ACCEPTED, (short) 9104);
        MICRORADIAN = add(rad, micro,                                               "µrad", SI,       (short) 9109);
        /*
         * All Unit<Length>.
         */
        m.related(7);
        METRE          = m;
        NANOMETRE      = add(m, nano,                                     "nm",    SI,       (short) 0);
        MILLIMETRE     = add(m, milli,                                    "mm",    SI,       (short) 1025);
        CENTIMETRE     = add(m, centi,                                    "cm",    SI,       (short) 1033);
        KILOMETRE      = add(m, kilo,                                     "km",    SI,       (short) 9036);
        NAUTICAL_MILE  = add(m, LinearConverter.scale(   1852,        1), "M",     OTHER,    (short) 9030);
        STATUTE_MILE   = add(m, LinearConverter.scale(1609344,     1000), "mi",    IMPERIAL, (short) 9093);
        US_SURVEY_FOOT = add(m, LinearConverter.scale(   1200,     3937), "ftUS",  OTHER,    (short) 9003);
        CLARKE_FOOT    = add(m, LinearConverter.scale(3047972654d, 1E10), "ftCla", OTHER,    (short) 9005);
        FOOT           = add(m, LinearConverter.scale(   3048,    10000), "ft",    IMPERIAL, (short) 9002);
        INCH           = add(m, LinearConverter.scale(    254,    10000), "in",    IMPERIAL, (short) 0);
        POINT          = add(m, LinearConverter.scale( 996264, 72000000), "pt",    OTHER,    (short) 0);
        /*
         * All Unit<Time>.
         */
        s.related(5);
        SECOND         = s;
        NANOSECOND     = add(s, nano,                                                  "ns",  SI,       (short) 0);
        MILLISECOND    = add(s, milli,                                                 "ms",  SI,       (short) 0);
        MINUTE         = add(s, LinearConverter.scale(                      60,    1), "min", ACCEPTED, (short) 0);
        HOUR           = add(s, LinearConverter.scale(                   60*60,    1), "h",   ACCEPTED, (short) 0);
        DAY            = add(s, LinearConverter.scale(         SECONDS_PER_DAY,    1), "d",   ACCEPTED, (short) 0);
        WEEK           = add(s, LinearConverter.scale(       7*SECONDS_PER_DAY,    1), "wk",  OTHER,    (short) 0);
        TROPICAL_YEAR  = add(s, LinearConverter.scale(MILLIS_PER_TROPICAL_YEAR, 1000), "a",   OTHER,    (short) 1029);
        /*
         * All Unit<Speed>, Unit<Acceleration>, Unit<AngularVelocity> and Unit<ScaleRateOfChange>.
         * The `unityPerSecond` unit is not added to the registry because it is specific to the EPSG database,
         * has no clear symbol and is easy to confuse with Hertz. We create that unit only for allowing us to
         * create the "ppm/a" units.
         */
        final SystemUnit<ScaleRateOfChange> unityPerSecond;
        unityPerSecond = new SystemUnit<>(ScaleRateOfChange.class, frequency, null, OTHER, (short) 1036, null);
        unityPerSecond.related(1);
        mps .related(2);
        mps2.related(1);
        METRES_PER_SECOND         = mps;
        METRES_PER_SECOND_SQUARED = mps2;
        KILOMETRES_PER_HOUR       = add(mps, LinearConverter.scale(10, 36),     "km∕h",  ACCEPTED, (short) 0);
        KNOT                      = add(mps, LinearConverter.scale(1852, 3600), "kn",    OTHER,    (short) 0);
        RADIANS_PER_SECOND        = add(AngularVelocity.class, null, frequency, "rad∕s", SI,       (short) 1035);
        GAL                       = add(mps2, centi, "Gal", (byte) (CGS | PREFIXABLE | ACCEPTED),  (short) 0);
        add(unityPerSecond, LinearConverter.scale(1, 31556925445E6), "ppm∕a", OTHER, (short) 1030);
        /*
         * All Unit<Pressure>.
         */
        Pa.related(3);
        PASCAL      = Pa;
        HECTOPASCAL = add(Pa,  hecto,                            "hPa",  SI,    (short) 0);
        DECIBAR     = add(Pa,  ten4,                             "dbar", OTHER, (short) 0);
        BAR         = add(Pa,  LinearConverter.scale(100000, 1), "bar",  OTHER, (short) 0);
        ATMOSPHERE  = add(Pa,  LinearConverter.scale(101325, 1), "atm",  OTHER, (short) 0);
        /*
         * All Unit<Temperature>.
         */
        K.related(2);
        KELVIN       = K;
        CELSIUS      = add(K, LinearConverter.offset(  27315, 100), "°C", SI,    (short) 0);
        FAHRENHEIT   = add(K, new LinearConverter(100, 45967, 180), "°F", OTHER, (short) 0);
        /*
         * Unit<Volume> and Unit<Mass>. Those units need to be handled in a special way because:
         * 1) The base unit of mass is "kg", not "g". This is handled by a hard-coded case in SystemUnit.
         * 2) The liter unit is not a SI units, but despite that is commonly used with SI prefixes.
         */
        SQUARE_METRE = m2;
        CUBIC_METRE  = m3;
        KILOGRAM     = kg;
        TONNE        = add(kg, kilo,  "t",         ACCEPTED,               (short) 0);
        HECTARE      = add(m2, ten4,  "ha",        ACCEPTED,               (short) 0);
        LITRE        = add(m3, milli, "L", (byte) (ACCEPTED | PREFIXABLE), (short) 0);
        GRAM         = add(kg, milli, "g", (byte) (ACCEPTED | PREFIXABLE), (short) 0);
        /*
         * Radioactivity must be defined after angular velocities, because it has the same dimensions.
         * The dimensions are also the same as frequency.
         */
        final SystemUnit<Radioactivity> Bq;
        Bq = add(Radioactivity.class, null, frequency, "Bq",  (byte) (SI | PREFIXABLE), (short) 0);
        Bq.related(1);
        BECQUEREL = Bq;
        CURIE     = add(Bq, LinearConverter.scale(3.7E10, 1), "Ci", OTHER, (short) 0);
        /*
         * Force, energy, electricity, magnetism and other units.
         * Frequency must be defined after angular velocities.
         */
        HERTZ      = add(Frequency.class,           Scalar.Frequency::new, frequency,                    "Hz",  (byte) (SI | PREFIXABLE), (short) 0);
        NEWTON     = add(Force.class,               Scalar.Force::new,     force,                        "N",   (byte) (SI | PREFIXABLE), (short) 0);
        JOULE      = add(Energy.class,              Scalar.Energy::new,    energy,                       "J",   (byte) (SI | PREFIXABLE), (short) 0);
        WATT       = add(Power.class,               Scalar.Power::new,     power,                        "W",   (byte) (SI | PREFIXABLE), (short) 0);
        AMPERE     = add(ElectricCurrent.class,     null,                  current,                      "A",   (byte) (SI | PREFIXABLE), (short) 0);
        COULOMB    = add(ElectricCharge.class,      null,                  charge,                       "C",   (byte) (SI | PREFIXABLE), (short) 0);
        VOLT       = add(ElectricPotential.class,   null,                  potential,                    "V",   (byte) (SI | PREFIXABLE), (short) 0);
        FARAD      = add(ElectricCapacitance.class, null,                  charge.divide(potential),     "F",   (byte) (SI | PREFIXABLE), (short) 0);
        SIEMENS    = add(ElectricConductance.class, null,                  current.divide(potential),    "S",   (byte) (SI | PREFIXABLE), (short) 0);
        OHM        = add(ElectricResistance.class,  null,                  potential.divide(current),    "Ω",   (byte) (SI | PREFIXABLE), (short) 0);
        WEBER      = add(MagneticFlux.class,        null,                  magneticFlux,                 "Wb",  (byte) (SI | PREFIXABLE), (short) 0);
        TESLA      = add(MagneticFluxDensity.class, null,                  magneticFlux.divide(area),    "T",   (byte) (SI | PREFIXABLE), (short) 0);
        HENRY      = add(ElectricInductance.class,  null,                  magneticFlux.divide(current), "H",   (byte) (SI | PREFIXABLE), (short) 0);
        LUX        = add(Illuminance.class,         null,                  luminous.divide(area),        "lx",  (byte) (SI | PREFIXABLE), (short) 0);
        LUMEN      = add(LuminousFlux.class,        null,                  luminous,                     "lm",  (byte) (SI | PREFIXABLE), (short) 0);
        CANDELA    = add(LuminousIntensity.class,   null,                  luminous,                     "cd",  (byte) (SI | PREFIXABLE), (short) 0);    // Must be after Lumen.
        MOLE       = add(AmountOfSubstance.class,   null,                  amount,                       "mol", (byte) (SI | PREFIXABLE), (short) 0);
        STERADIAN  = add(SolidAngle.class,          null,                  dimensionless,                "sr",  (byte) (SI | PREFIXABLE), (short) 0);
        /*
         * All Unit<Dimensionless>.
         */
        final SystemUnit<Salinity> sal;
        ConventionalUnit<Dimensionless> bel;
        SIGMA   = add(Dimensionless.class, Scalar.Dimensionless::new, dimensionless, "sigma", OTHER, (short) 0);
        PIXEL   = add(Dimensionless.class, Scalar.Dimensionless::new, dimensionless, "px",    OTHER, (short) 0);
        sal     = add(Salinity.class,      null,                      dimensionless, null,    OTHER, (short) 0);
        PSU     = add(sal, milli,                                                    "psu",   OTHER, (short) 0);
        PERCENT = add(one, centi,                                                    "%",     OTHER, (short) 0);
        PPM     = add(one, micro,                                                    "ppm",   OTHER, (short) 9202);
        bel     = add(one, PowerOf10.belToOne(), "B", (byte) (ACCEPTED | PREFIXABLE), (short) 0);
        DECIBEL = add(bel, Prefixes.converter('d'), "dB", ACCEPTED, (short) 0);
        UNITY   = UnitRegistry.init(one);  // Must be last in order to take precedence over all other units associated to UnitDimension.NONE.

        UnitRegistry.alias(UNITY,       Short.valueOf((short) 9203));
        UnitRegistry.alias(DEGREE,      Short.valueOf(Constants.EPSG_AXIS_DEGREES));
        UnitRegistry.alias(ARC_MINUTE,  "'");
        UnitRegistry.alias(ARC_SECOND, "\"");
        UnitRegistry.alias(KNOT,       "kt");       // Symbol used in aviation.
        UnitRegistry.alias(KELVIN,      "K");       // Ordinary "K" letter (not the dedicated Unicode character).
        UnitRegistry.alias(CELSIUS,     "℃");
        UnitRegistry.alias(CELSIUS,   "Cel");
        UnitRegistry.alias(FAHRENHEIT,  "℉");
        UnitRegistry.alias(GRAD,      "gon");
        UnitRegistry.alias(GAL,     "cm∕s²");
        UnitRegistry.alias(HECTARE,   "hm²");
        UnitRegistry.alias(LITRE,       "l");
        UnitRegistry.alias(LITRE,       "ℓ");
        UnitRegistry.alias(PSU,       "PSU");
        UnitRegistry.alias(UNITY, SystemUnit.ONE);

        initialized = true;
    }

    /**
     * Invoked by {@code Units} static class initializer for registering SI base and derived units.
     * This method shall be invoked in a single thread by the {@code Units} class initializer only.
     *
     * @param  quantity   the type of quantity that uses this unit (should not be null).
     * @param  factory    the factory to use for creating quantities, or {@code null} if none.
     * @param  dimension  the unit dimension.
     * @param  symbol     the unit symbol, or {@code null} if this unit has no specific symbol.
     * @param  scope      {@link UnitRegistry#SI}, {@link UnitRegistry#ACCEPTED}, other constants or 0 if unknown.
     * @param  epsg       the EPSG code, or 0 if this unit has no EPSG code.
     */
    private static <Q extends Quantity<Q>> SystemUnit<Q> add(Class<Q> quantity, ScalarFactory<Q> factory,
            UnitDimension dimension, String symbol, byte scope, short epsg)
    {
        return UnitRegistry.init(new SystemUnit<>(quantity, dimension, symbol, scope, epsg, factory));
    }

    /**
     * Invoked by {@code Units} static class initializer for registering SI conventional units.
     * This method shall be invoked in a single thread by the {@code Units} class initializer only.
     *
     * <p>The {@code target} argument should be an instance of {@link SystemUnit}.
     * The only exception is for creating the {@link #DECIBEL} unit base on the bel conventional unit.</p>
     *
     * <p>If the {@code target} unit holds a list of {@linkplain SystemUnit#related() related units}
     * (i.e. conventional units that cannot be computed easily by appending a SI prefix), then the new
     * conventional unit is added to that list of related units. For example, "foot" is related to "metre"
     * and "degree Celsius" is related to "Kelvin", but "kilometre" is not recorded as related to "metre"
     * because this relationship can be inferred automatically without the need of a {@code related} table.
     * The unrecorded units are all SI units related to {@code target} by a scale factor without offset.</p>
     */
    private static <Q extends Quantity<Q>> ConventionalUnit<Q> add(AbstractUnit<Q> target, UnitConverter toTarget, String symbol, byte scope, short epsg) {
        final ConventionalUnit<Q> unit = UnitRegistry.init(new ConventionalUnit<>(target, toTarget, symbol, scope, epsg));
        final ConventionalUnit<Q>[] related = target.related();
        if (related != null && (unit.scope != UnitRegistry.SI || !toTarget.isLinear())) {
            // Search first empty slot. This algorithm is inefficient, but the length of those arrays is small (<= 7).
            int i = 0;
            while (related[i] != null) i++;
            related[i] = unit;
        }
        return unit;
    }

    /**
     * Returns the system unit for the given dimension, or {@code null} if none.
     * Note that this method cannot distinguish the different kinds of dimensionless units.
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
     * It is only for getting one of the predefined constants, for example after deserialization.
     *
     * <p><b>Implementation note:</b> this method must be defined in this {@code Units} class
     * in order to force a class initialization before use.</p>
     *
     * @see Prefixes#getUnit(String)
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
     * Those units are sometimes used instead of linear units for altitude measurements.
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
     * Multiplies the given unit by the given ratio. For example, multiplying {@link #CENTIMETRE} by 254/100 gives
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
     * Creates an unit for values computed by the logarithm in base 10 of values in the given unit.
     * Conversions from the given unit to the returned unit is done by {@link Math#log10(double)}.
     *
     * <p><strong>The given unit should be dimensionless.</strong>
     * However, this method does not enforce this constraint in order to allow the creation of
     * units such as <a href="https://en.wikipedia.org/wiki/Decibel_watt">decibel watt</a>.
     * For example:</p>
     *
     * {@snippet lang="java" :
     * Unit<Power> dBW = Units.logarithm(Units.WATT).divide(10);
     * }
     *
     * @param  unit  the unit from which to convert.
     * @return an unit which is the logarithm in base 10 of the given unit.
     *
     * @since 1.5
     */
    public static <Q extends Quantity<Q>> Unit<Q> logarithm(final Unit<Q> unit) {
        return unit.transform(PowerOf10.INSTANCE);
    }

    /**
     * Returns the factor by which to multiply the standard unit in order to get the given unit.
     * The "standard" unit is usually the SI unit on which the given unit is based, as given by
     * {@link Unit#getSystemUnit()}.
     *
     * <p>If the given unit is {@code null} or if the conversion to the "standard" unit cannot be expressed
     * by a single multiplication factor, then this method returns {@link Double#NaN}.</p>
     *
     * <h4>Example</h4>
     * If the given unit is {@link #KILOMETRE}, then this method returns 1000 since a measurement in kilometres
     * must be multiplied by 1000 in order to give the equivalent measurement in the "standard" units
     * (here {@link #METRE}).
     *
     * @param  <Q>   the quantity measured by the unit, or {@code null}.
     * @param  unit  the unit for which we want the multiplication factor to standard unit, or {@code null}.
     * @return the factor by which to multiply a measurement in the given unit in order to get an equivalent
     *         measurement in the standard unit, or NaN if the conversion cannot be expressed by a scale factor.
     */
    public static <Q extends Quantity<Q>> double toStandardUnit(final Unit<Q> unit) {
        return AbstractConverter.scale(unit == null ? null : unit.getConverterTo(unit.getSystemUnit()));
    }

    /**
     * Creates a linear converter from the given scale and offset.
     *
     * @param  scale   the scale factor, or {@code null} if none (default value of 1).
     * @param  offset  the offset, or {@code null} if none (default value of 0).
     * @return a converter for the given scale and offset.
     *
     * @see org.apache.sis.referencing.operation.transform.MathTransforms#linear(double, double)
     *
     * @since 1.0
     */
    public static UnitConverter converter(final Number scale, final Number offset) {
        return LinearConverter.create(scale, offset);
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
     * This method returns {@code null} if it cannot get the polynomial equation coefficients from the given converter.
     *
     * @param  converter  the converter from which to get the coefficients of the polynomial equation, or {@code null}.
     * @return the polynomial equation coefficients (may be any length, including zero), or {@code null} if the given
     *         converter is {@code null} or if this method cannot get the coefficients.
     *
     * @since 0.8
     */
    @OptionalCandidate
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
                final double offset = converter.convert(0);  // Should be zero as per JSR-385 specification, but we are paranoiac.
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
     * or {@code NaN} if this method cannot compute it.
     *
     * @param  converter  the converter for which we want the derivative at a given point, or {@code null}.
     * @param  value      the point at which to compute the derivative.
     *                    Ignored (can be {@link Double#NaN}) if the conversion is linear.
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
     * <h4>Parsing authority codes</h4>
     * If the given {@code uom} arguments is of the form {@code "EPSG:####"}, {@code "urn:ogc:def:uom:EPSG:####"}
     * or {@code "http://www.opengis.net/def/uom/EPSG/0/####"} (ignoring case and whitespaces around separators),
     * then {@code "####"} is parsed as an integer and forwarded to the {@link #valueOfEPSG(int)} method.
     *
     * <h4>Note on netCDF unit symbols</h4>
     * In netCDF files, values of "unit" attribute are concatenations of an angular unit with an axis direction,
     * as in {@code "degrees_east"} or {@code "degrees_north"}. This {@code valueOf(…)} method ignores those suffixes
     * and unconditionally returns {@link #DEGREE} for all axis directions.
     *
     * @param  uom  the symbol to parse, or {@code null}.
     * @return the parsed symbol, or {@code null} if {@code uom} was null.
     * @throws MeasurementParseException if the given symbol cannot be parsed.
     *
     * @see UnitFormat#parse(CharSequence)
     */
    public static Unit<?> valueOf(String uom) throws MeasurementParseException {
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
     * which uses EPSG codes for identifying units.</p>
     *
     * <p>The currently recognized values are:</p>
     * <table class="sis">
     *   <caption>EPSG codes for units</caption>
     *   <tr>
     *     <td><table class="compact">
     *       <caption>Angular units</caption>
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
     *     <td class="sep"><table class="compact">
     *       <caption>Linear units</caption>
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
     *     <td class="sep"><table class="compact">
     *       <caption>Time units</caption>
     *       <tr><td style="width: 40px"><b>Code</b></td><td><b>Unit</b></td></tr>
     *       <tr><td>1029</td><td>year</td></tr>
     *       <tr><td>1040</td><td>second</td></tr>
     *     </table></td>
     *     <td class="sep"><table class="compact">
     *       <caption>Scale units</caption>
     *       <tr><td style="width: 40px"><b>Code</b></td><td><b>Unit</b></td></tr>
     *       <tr><td>9201</td><td>unity</td></tr>
     *       <tr><td>9202</td><td>parts per million</td></tr>
     *       <tr><td>9203</td><td>unity</td></tr>
     *     </table></td>
     *   </tr>
     * </table>
     *
     * <h4>Axis units special case</h4>
     * EPSG uses code 9102 (<cite>degree</cite>) for prime meridian and coordinate operation parameters,
     * and code 9122 (<cite>degree (supplier to define representation)</cite>) for coordinate system axes.
     * But Apache SIS considers those two codes as synonymous.
     *
     * @param  code  the EPSG code for a unit of measurement.
     * @return the unit, or {@code null} if the code is unrecognized.
     *
     * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createUnit(String)
     */
    @OptionalCandidate
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
     * @see org.apache.sis.referencing.cs.CoordinateSystems#getEpsgCode(Unit, AxisDirection...)
     *
     * @since 0.4
     */
    @OptionalCandidate
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
