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
package org.apache.sis.referencing.cs;

import javax.measure.Unit;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;                 // For javadoc
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.measure.Units;


/**
 * High-level characteristics about the axes of a coordinate system.
 * This enumeration provides a convenient way to identify some common axes conventions like
 * axis order or range of longitude values. Apache SIS Coordinate System objects can be made
 * compliant to a given convention by calls to their {@code forConvention(AxesConvention)} method.
 *
 * <p>The following table summarizes the coordinate system aspects that may be modified by each enum value,
 * with an example of change applied by the enum. Blank cells mean that the property is not changed by the
 * enum value.</p>
 *
 * <table class="sis">
 *   <caption>Coordinate system properties changed by enum values</caption>
 *   <tr>
 *     <th             rowspan="2">Property</th>
 *     <th class="sep" rowspan="2">Example</th>
 *     <th class="sep" colspan="4">Modified by:</th>
 *   </tr><tr>
 *     <th class="sep">{@linkplain #NORMALIZED Normalized}</th>
 *     <th>{@linkplain #DISPLAY_ORIENTED Display<br>oriented}</th>
 *     <th>{@linkplain #RIGHT_HANDED     Right<br>handed}</th>
 *     <th>{@linkplain #POSITIVE_RANGE   Positive<br>range}</th>
 *   </tr><tr>
 *     <td>Axis order</td>
 *     <td> class="sep"(<var>longitude</var>, <var>latitude</var>)</td>
 *     <td class="sep" style="text-align:center">✔</td>
 *     <td style="text-align:center">✔</td>
 *     <td style="text-align:center">✔</td>
 *     <td></td>
 *   </tr><tr>
 *     <td>Axis direction</td>
 *     <td> class="sep"({@linkplain AxisDirection#EAST east}, {@linkplain AxisDirection#NORTH north})</td>
 *     <td class="sep" style="text-align:center">✔</td>
 *     <td style="text-align:center">✔</td>
 *     <td></td>
 *     <td></td>
 *   </tr><tr>
 *     <td>Unit of measurement</td>
 *     <td> class="sep"Angular degrees &amp; metres</td>
 *     <td class="sep" style="text-align:center">✔</td>
 *     <td></td>
 *     <td></td>
 *     <td></td>
 *   </tr><tr>
 *     <td>Range of values</td>
 *     <td> class="sep"[0 … 360]° of longitude</td>
 *     <td> class="sep"</td>
 *     <td></td>
 *     <td></td>
 *     <td style="text-align:center">✔</td>
 *   </tr>
 * </table>
 *
 * <h2>Note on enumeration order</h2>
 * The enumeration values are sorted from most conservative to most aggressive.
 * The fist enumeration values make no change or change only the range of values.
 * Next enumeration values may change axis order but not the units of measurement.
 * The last enumeration values may change both axis order an units of measurement.
 *
 * <h2>Note on axis order</h2>
 * The axis order is specified by the authority (typically a national agency) defining the Coordinate Reference System
 * (CRS). The order depends on the CRS type and the country defining the CRS. In the case of geographic CRS, the
 * (<var>latitude</var>, <var>longitude</var>) axis order is widely used by geographers and pilots for centuries.
 * However, software developers tend to consistently use the (<var>x</var>,<var>y</var>) order for every kinds of CRS.
 * Those different practices resulted in contradictory definitions of axis order for almost every CRS of kind
 * {@code GeographicCRS}, for some {@code ProjectedCRS} in the South hemisphere (South Africa, Australia, <i>etc.</i>)
 * and for some polar projections among others.
 *
 * <p>Recent OGC standards mandate the use of axis order as defined by the authority. Oldest OGC standards used the
 * (<var>x</var>,<var>y</var>) axis order instead, ignoring any authority specification. Many software products still use
 * the old (<var>x</var>,<var>y</var>) axis order, because it is easier to implement. Apache SIS supports both conventions.
 * By default, SIS creates CRS with axis order as defined by the authority. Those CRS are created by calls to the
 * {@link org.apache.sis.referencing.CRS#forCode(String)} method. The actual axis order can be verified after the CRS
 * creation with {@code System.out.println(crs)}. If (<var>x</var>,<var>y</var>) axis order is wanted for compatibility
 * with older OGC specifications or other software products, CRS forced to "longitude first" axis order can be created
 * using the {@link #DISPLAY_ORIENTED} or {@link #NORMALIZED} enumeration value.</p>
 *
 * <h2>Note on range of longitude values</h2>
 * Most geographic CRS have a longitude axis defined in the [-180 … +180]° range. All map projections in Apache SIS are
 * designed to work in that range. This is also the range of {@link Math} trigonometric functions like {@code atan2(y,x)}.
 * However, some data use the [0 … 360]° range instead. A geographic CRS can be shifted to that range of longitude values
 * using the {@link #POSITIVE_RANGE} enumeration value. The choice of longitude range will impact not only some
 * coordinate conversions, but also the methods that verify the <i>domain of validity</i>
 * (e.g. {@link org.apache.sis.geometry.GeneralEnvelope#normalize()}).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see AbstractCS#forConvention(AxesConvention)
 * @see org.apache.sis.referencing.crs.AbstractCRS#forConvention(AxesConvention)
 *
 * @since 0.4
 */
public enum AxesConvention implements AxisFilter {
    /**
     * The axis order as they were specified in the original coordinate system.
     * The first time that a {@code forConvention(…)} method is invoked on a new coordinate system (CS),
     * a reference to that original CS is associated to this enumeration value and can be retrieved from
     * any derived object.
     *
     * @since 1.5
     */
    ORIGINAL,

    /**
     * Axes having a <i>wraparound</i> range meaning are shifted to their ranges of positive values.
     * The units of measurement and range period (difference between
     * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getMaximumValue() maximum} and
     * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getMinimumValue() minimum value})
     * are unchanged.
     *
     * <h4>Usage</h4>
     * The most frequent usage of this enum is for shifting longitude values from the [-180 … +180]° range
     * to the [0 … 360]° range. However, this enum could also be used with climatological calendars if their
     * time axis has a wrapround range meaning.
     *
     * <p>Note that conversions from an coordinate system using the [-180 … +180]° range to a coordinate system
     * using the [0 … 360]° range may not be affine. For example, the data in the West hemisphere ([-180 … 0]°)
     * may need to move on the right side of the East hemisphere ([180 … 360]°).
     * Some geometries may need to be separated in two parts, and others may need to be merged.</p>
     *
     * @see org.opengis.referencing.cs.CoordinateSystemAxis#getRangeMeaning()
     * @see org.opengis.referencing.cs.RangeMeaning#WRAPAROUND
     */
    POSITIVE_RANGE,

    /**
     * Axes are ordered for a <i>right-handed</i> coordinate system. Axis directions, ranges or coordinate values
     * and units of measurement are unchanged. In the two-dimensional case, the handedness is defined from the point of
     * view of an observer above the plane of the system.
     *
     * <p>Note that a right-handed coordinate system does not guarantee that longitude or <var>x</var> axis
     * will be first in every cases. The most notable exception is the case of (West, North) orientations.
     * The following table lists that case, together with other common axis orientations.
     * The axes orientations implied by this {@code RIGHT_HANDED} enum is shown,
     * together with {@link #DISPLAY_ORIENTED} axes for reference:</p>
     *
     * <table class="sis">
     *   <caption>Examples of left-handed and right-handed coordinate systems</caption>
     *   <tr><th>Left-handed</th> <th>Right-handed</th> <th>Display oriented</th> <th>Remarks</th></tr>
     *   <tr><td>North, East</td> <td>East, North</td> <td>East, North</td> <td>This is the most common case.</td></tr>
     *   <tr><td>West, North</td> <td>North, West</td> <td>East, North</td> <td>This right-handed system has latitude first.</td></tr>
     *   <tr><td>South, West</td> <td>West, South</td> <td>East, North</td> <td>Used for the mapping of southern Africa.</td></tr>
     *   <tr><td>South along 0°,<br>South along 90° West</td>
     *       <td>South along 90° West,<br>South along 0°</td>
     *       <td>(Same as right-handed)</td>
     *       <td>Can be used for the mapping of North pole.</td></tr>
     * </table>
     *
     * @see org.apache.sis.referencing.cs.CoordinateSystems#angle(AxisDirection, AxisDirection)
     * @see <a href="https://en.wikipedia.org/wiki/Right_hand_rule">Right-hand rule on Wikipedia</a>
     */
    RIGHT_HANDED,

    /**
     * Axes are reordered and oriented toward directions commonly used for displaying purpose.
     * Units of measurement are unchanged. This convention can be used for deriving a coordinate system with the
     * (<var>longitude</var>, <var>latitude</var>) or (<var>x</var>,<var>y</var>) axis order.
     * A similar concept appears in the Web Map Services (WMS) 1.3 specification, quoted here:
     *
     * <blockquote><b>6.7.2 Map CS</b> —
     * The usual orientation of the Map CS shall be such that the <var>i</var> axis is parallel to the East-to-West axis
     * of the Layer CRS and increases Eastward, and the <var>j</var> axis is parallel to the North-to-South axis of the
     * Layer CRS and increases Southward. This orientation will not be possible in some cases, as (for example) in an
     * orthographic projection over the South Pole. The convention to be followed is that, wherever possible, East shall
     * be to the right edge and North shall be toward the upper edge of the Map CS.</blockquote>
     *
     * The above-cited (<var>i</var>, <var>j</var>) axes are mapped to <i>display right</i> and <i>display down</i>
     * directions respectively. Other kinds of axis are mapped to <i>east</i> and <i>north</i> directions when possible.
     * More specifically, Apache SIS tries to setup the following directions
     * (replacing a direction by its "forward" counterpart when necessary, e.g. {@code SOUTH} → {@code NORTH})
     * in the order shown below:
     *
     * <table class="sis">
     *   <caption>Axis directions and order</caption>
     *   <tr>
     *     <th>Preferred axis sequences</th>
     *     <th>Purpose</th>
     *   </tr><tr>
     *     <td>{@link AxisDirection#EAST EAST}, {@link AxisDirection#NORTH NORTH},
     *         {@link AxisDirection#UP UP}, {@link AxisDirection#FUTURE FUTURE}</td>
     *     <td>Commonly used (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>) directions for coordinates.</td>
     *   </tr><tr>
     *     <td>{@link AxisDirection#DISPLAY_RIGHT DISPLAY_RIGHT}, {@link AxisDirection#DISPLAY_DOWN DISPLAY_DOWN}</td>
     *     <td>Commonly used (<var>x</var>, <var>y</var>) directions for screen devices.</td>
     *   </tr><tr>
     *     <td>{@link AxisDirection#ROW_POSITIVE ROW_POSITIVE},
     *         {@link AxisDirection#COLUMN_POSITIVE COLUMN_POSITIVE}</td>
     *     <td>Indices in grids or matrices.</td>
     *   </tr>
     * </table>
     *
     * <h4>API notes</h4>
     * we do not provide a <q>longitude or <var>x</var> axis first</q> enumeration value because such criterion
     * is hard to apply to inter-cardinal directions and has no meaning for map projections over a pole.
     * The <i>display oriented</i> enumeration name applies to a wider range of cases,
     * but still have a loosely definition which may be adjusted in future Apache SIS versions.
     * If a more stable definition is needed, consider using {@link #RIGHT_HANDED} instead since
     * <i>right-handed</i> coordinate systems have a more precise meaning in Apache SIS.
     *
     * @since 1.0
     */
    DISPLAY_ORIENTED {
        @Override public AxisDirection getDirectionReplacement(CoordinateSystemAxis axis, AxisDirection direction) {
            return NORMALIZED.getDirectionReplacement(axis, direction);
        }
    },

    /**
     * Axes order, direction and units of measurement are forced to commonly used predefined values.
     * This convention implies the following changes on a coordinate system:
     *
     * <ul>
     *   <li>Axes are oriented and ordered as defined for {@link #DISPLAY_ORIENTED} coordinate systems.</li>
     *   <li>Known units are normalized (this list may be expanded in future SIS versions):
     *     <ul>
     *       <li>Angular units are set to {@link Units#DEGREE}.</li>
     *       <li>Linear units are set to {@link Units#METRE}.</li>
     *       <li>Temporal units are set to {@link Units#DAY}.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * This convention does not normalize longitude values to the [-180 … +180]° range and does not set the
     * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum#getPrimeMeridian() prime meridian} to Greenwich.
     * Those changes are not applied for avoiding discontinuity in conversions from the non-normalized CRS to the normalized CRS.
     *
     * <h4>Rational</h4>
     * The reason why we do not normalize the range and the prime meridian is because doing so
     * would cause the conversion between old and new coordinate systems to be non-affine for axes
     * having {@link org.opengis.referencing.cs.RangeMeaning#WRAPAROUND}. Furthermore, changing the
     * prime meridian would be a datum change rather than a coordinate system change, and datum
     * changes are more difficult to handle by coordinate operation factories.
     *
     * @see org.apache.sis.referencing.CommonCRS#normalizedGeographic()
     * @see CoordinateSystems#replaceAxes(CoordinateSystem, AxisFilter)
     */
    NORMALIZED {
        @Override
        public Unit<?> getUnitReplacement(final CoordinateSystemAxis axis, Unit<?> unit) {
            if (Units.isLinear(unit)) {
                unit = Units.METRE;
            } else if (Units.isAngular(unit)) {
                unit = Units.DEGREE;
            } else if (Units.isTemporal(unit)) {
                unit = Units.DAY;
            }
            return unit;
        }

        @Override
        public AxisDirection getDirectionReplacement(final CoordinateSystemAxis axis, final AxisDirection direction) {
            /*
             * For now we do not touch to inter-cardinal directions (e.g. "North-East")
             * because it is not clear which normalization policy would match common usage.
             */
            if (!AxisDirections.isIntercardinal(direction)) {
                /*
                 * Change the direction only if the axis allows negative values.
                 * If the axis accepts only positive values, then the direction
                 * is considered non-invertible.
                 */
                if (axis == null || axis.getMinimumValue() < 0) {
                    return AxisDirections.absolute(direction);
                }
            }
            return direction;
        }
    };

    /**
     * Returns the conventions that only change axis order.
     * Units of measurement and ranges of values are not modified.
     * The current implementation returns {@link #RIGHT_HANDED} and {@link #DISPLAY_ORIENTED}, in that order.
     *
     * @return the conventions that only change axis order.
     *
     * @since 1.5
     */
    public static AxesConvention[] valuesForOrder() {
        return new AxesConvention[] {RIGHT_HANDED, DISPLAY_ORIENTED};
    }
}
