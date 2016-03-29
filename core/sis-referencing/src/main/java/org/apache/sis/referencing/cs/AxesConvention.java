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

import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;                 // For javadoc
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.internal.metadata.AxisDirections;
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
 *     <th>Property</th>
 *     <th>Example</th>
 *     <th>{@linkplain #NORMALIZED              Normalized}</th>
 *     <th>{@linkplain #CONVENTIONALLY_ORIENTED Conventionally<br>oriented}</th>
 *     <th>{@linkplain #RIGHT_HANDED            Right<br>handed}</th>
 *     <th>{@linkplain #POSITIVE_RANGE          Positive<br>range}</th>
 *   </tr>
 *   <tr>
 *     <td>Axis order</td>
 *     <td>(<var>longitude</var>, <var>latitude</var>)</td>
 *     <td style="text-align:center">✔</td>
 *     <td style="text-align:center">✔</td>
 *     <td style="text-align:center">✔</td>
 *     <td></td>
 *   </tr>
 *   <tr>
 *     <td>Axis direction</td>
 *     <td>({@linkplain AxisDirection#EAST east}, {@linkplain AxisDirection#NORTH north})</td>
 *     <td style="text-align:center">✔</td>
 *     <td style="text-align:center">✔</td>
 *     <td></td>
 *     <td></td>
 *   </tr>
 *   <tr>
 *     <td>Unit of measurement</td>
 *     <td>Angular degrees &amp; metres</td>
 *     <td style="text-align:center">✔</td>
 *     <td></td>
 *     <td></td>
 *     <td></td>
 *   </tr>
 *   <tr>
 *     <td>Range of values</td>
 *     <td>[0 … 360]° of longitude</td>
 *     <td></td>
 *     <td></td>
 *     <td></td>
 *     <td style="text-align:center">✔</td>
 *   </tr>
 * </table>
 *
 * <div class="section">Discussion on axis order</div>
 * The axis order is specified by the authority (typically a national agency) defining the Coordinate Reference System
 * (CRS). The order depends on the CRS type and the country defining the CRS. In the case of geographic CRS, the
 * (<var>latitude</var>, <var>longitude</var>) axis order is widely used by geographers and pilotes for centuries.
 * However software developers tend to consistently use the (<var>x</var>,<var>y</var>) order for every kind of CRS.
 * Those different practices resulted in contradictory definitions of axis order for almost every CRS of kind
 * {@code GeographicCRS}, for some {@code ProjectedCRS} in the South hemisphere (South Africa, Australia, <i>etc.</i>)
 * and for some polar projections among others.
 *
 * <p>Recent OGC standards mandate the use of axis order as defined by the authority. Oldest OGC standards used the
 * (<var>x</var>,<var>y</var>) axis order instead, ignoring any authority specification. Many softwares still use the
 * old (<var>x</var>,<var>y</var>) axis order, because it is easier to implement. Apache SIS supports both conventions.
 * By default, SIS creates CRS with axis order as defined by the authority. Those CRS are created by calls to the
 * {@link org.apache.sis.referencing.CRS#forCode(String)} method. The actual axis order can be verified after the CRS
 * creation with {@code System.out.println(crs)}. If (<var>x</var>,<var>y</var>) axis order is wanted for compatibility
 * with older OGC specifications or other softwares, CRS forced to "longitude first" axis order can be created using the
 * {@link #CONVENTIONALLY_ORIENTED} or {@link #NORMALIZED} enumeration value.</p>
 *
 * <div class="section">Range of longitude values</div>
 * Most geographic CRS have a longitude axis defined in the [-180 … +180]° range. All map projections in Apache SIS are
 * designed to work in that range. This is also the range of {@link Math} trigonometric functions like {@code atan2(y,x)}.
 * However some data use the [0 … 360]° range instead. A geographic CRS can be shifted to that range of longitude values
 * using the {@link #POSITIVE_RANGE} enumeration value. The choice of longitude range will impact not only some
 * coordinate conversions, but also the methods that verify the <cite>domain of validity</cite>
 * (e.g. {@link org.apache.sis.geometry.GeneralEnvelope#normalize()}).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see AbstractCS#forConvention(AxesConvention)
 * @see org.apache.sis.referencing.crs.AbstractCRS#forConvention(AxesConvention)
 */
public enum AxesConvention implements AxisFilter {
    /**
     * Axes order, direction and units of measure are forced to commonly used pre-defined values.
     * This enum represents the following changes to apply on a coordinate system:
     *
     * <ul>
     *   <li>Axes are oriented and ordered as defined for {@link #CONVENTIONALLY_ORIENTED} coordinate systems.</li>
     *   <li>Known units are normalized (this list may be expanded in future SIS versions):
     *     <ul>
     *       <li>Angular units are set to {@link javax.measure.unit.NonSI#DEGREE_ANGLE}.</li>
     *       <li>Linear units are set to {@link javax.measure.unit.SI#METRE}.</li>
     *       <li>Temporal units are set to {@link javax.measure.unit.NonSI#DAY}.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * Current implementation does not normalize longitude values to the [-180 … +180]° range and does not set
     * the {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum#getPrimeMeridian() prime meridian}
     * to Greenwich. However those rules may be adjusted in future SIS versions based on experience gained.
     * For more predictable results, consider using {@link #CONVENTIONALLY_ORIENTED} or {@link #RIGHT_HANDED} instead.
     *
     * <div class="note"><b>Rational:</b>
     * The reason why we do not yet normalize the range and the prime meridian is because doing so
     * would cause the conversion between old and new coordinate systems to be non-affine for axes
     * having {@link org.opengis.referencing.cs.RangeMeaning#WRAPAROUND}. Furthermore changing the
     * prime meridian would be a datum change rather than a coordinate system change, and datum
     * changes are more difficult to handle by coordinate operation factories.
     * </div>
     *
     * @see org.apache.sis.referencing.CommonCRS#normalizedGeographic()
     * @see CoordinateSystems#replaceAxes(CoordinateSystem, AxisFilter)
     */
    NORMALIZED {
        @Override
        public boolean accept(final CoordinateSystemAxis axis) {
            return true;
        }

        @Override
        @Deprecated
        public Unit<?> getUnitReplacement(Unit<?> unit) {
            return getUnitReplacement(null, unit);
        }

        @Override
        public Unit<?> getUnitReplacement(final CoordinateSystemAxis axis, Unit<?> unit) {
            if (Units.isLinear(unit)) {
                unit = SI.METRE;
            } else if (Units.isAngular(unit)) {
                unit = NonSI.DEGREE_ANGLE;
            } else if (Units.isTemporal(unit)) {
                unit = NonSI.DAY;
            }
            return unit;
        }

        @Override
        @Deprecated
        public AxisDirection getDirectionReplacement(final AxisDirection direction) {
            return getDirectionReplacement(null, direction);
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
    },

    /**
     * Axes are oriented toward conventional directions and ordered for a {@linkplain #RIGHT_HANDED right-handed}
     * coordinate system. Units of measurement are unchanged.
     *
     * <p>More specifically, directions opposites to the following ones are replaced by their "forward" counterpart
     * (e.g. {@code SOUTH} → {@code NORTH}):</p>
     *
     * <table class="sis">
     *   <caption>Axis directions used by convention</caption>
     *   <tr>
     *     <th>Preferred {@link AxisDirection}</th>
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
     * Then, axes are ordered for {@link #RIGHT_HANDED} coordinate system.
     *
     * <div class="section">Usage</div>
     * This enum is often used for deriving a coordinate system with the (<var>longitude</var>, <var>latitude</var>) or
     * (<var>x</var>,<var>y</var>) axis order. We do not provide a <cite>"longitude or <var>x</var> axis first"</cite>
     * enumeration value because such criterion is hard to apply to inter-cardinal directions and has no meaning for
     * map projections over a pole, while the right-handed rule can apply everywhere.
     *
     * <p><cite>Right-handed</cite> coordinate systems have a precise meaning in Apache SIS.
     * However <cite>conventionally oriented</cite> coordinate systems have a looser definition.
     * A similar concept appears in the Web Map Services (WMS) 1.3 specification, quoted here:</p>
     *
     * <div class="note"><b>6.7.2 Map CS</b> —
     * The usual orientation of the Map CS shall be such that the <var>i</var> axis is parallel to the East-to-West axis
     * of the Layer CRS and increases Eastward, and the <var>j</var> axis is parallel to the North-to-South axis of the
     * Layer CRS and increases Southward. This orientation will not be possible in some cases, as (for example) in an
     * orthographic projection over the South Pole. The convention to be followed is that, wherever possible, East shall
     * be to the right edge and North shall be toward the upper edge of the Map CS.</div>
     *
     * @since 0.5
     */
    CONVENTIONALLY_ORIENTED {
        @Override
        public boolean accept(final CoordinateSystemAxis axis) {
            return true;
        }

        @Override
        @Deprecated
        public Unit<?> getUnitReplacement(final Unit<?> unit) {
            return unit;
        }

        @Override
        public Unit<?> getUnitReplacement(final CoordinateSystemAxis axis, final Unit<?> unit) {
            return unit;
        }

        @Override
        @Deprecated
        public AxisDirection getDirectionReplacement(final AxisDirection direction) {
            return getDirectionReplacement(null, direction);
        }

        @Override
        public AxisDirection getDirectionReplacement(CoordinateSystemAxis axis, AxisDirection direction) {
            return NORMALIZED.getDirectionReplacement(axis, direction);
        }
    },

    /**
     * Axes are ordered for a <cite>right-handed</cite> coordinate system. Axis directions, ranges or ordinate values
     * and units of measurement are unchanged. In the two-dimensional case, the handedness is defined from the point
     * of view of an observer above the plane of the system.
     *
     * <p>Note that a right-handed coordinate system does not guarantee that longitude or <var>x</var> axis
     * will be first in every cases. The most notable exception is the case of (West, North) orientations.
     * The following table lists that case, together with other common axis orientations.
     * The axes orientations implied by this {@code RIGHT_HANDED} enum is shown,
     * together with {@link #CONVENTIONALLY_ORIENTED} axes for reference:</p>
     *
     * <div class="note">
     * <table class="sis">
     *   <caption>Examples of left-handed and right-handed coordinate systems</caption>
     *   <tr><th>Left-handed</th> <th>Right-handed</th> <th>Conventionally oriented</th> <th>Remarks</th></tr>
     *   <tr><td>North, East</td> <td>East, North</td> <td>East, North</td> <td>This is the most common case.</td></tr>
     *   <tr><td>West, North</td> <td>North, West</td> <td>East, North</td> <td>This right-handed system has latitude first.</td></tr>
     *   <tr><td>South, West</td> <td>West, South</td> <td>East, North</td> <td>Used for the mapping of southern Africa.</td></tr>
     *   <tr><td>South along 0°,<br>South along 90° West</td>
     *       <td>South along 90° West,<br>South along 0°</td>
     *       <td>(Same as right-handed)</td>
     *       <td>Can be used for the mapping of North pole.</td></tr>
     * </table></div>
     *
     * @see org.apache.sis.referencing.cs.CoordinateSystems#angle(AxisDirection, AxisDirection)
     * @see <a href="http://en.wikipedia.org/wiki/Right_hand_rule">Right-hand rule on Wikipedia</a>
     */
    RIGHT_HANDED {
        @Override
        public boolean accept(final CoordinateSystemAxis axis) {
            return true;
        }

        @Override
        @Deprecated
        public Unit<?> getUnitReplacement(final Unit<?> unit) {
            return unit;
        }

        @Override
        public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, final Unit<?> unit) {
            return unit;
        }

        @Override
        @Deprecated
        public AxisDirection getDirectionReplacement(final AxisDirection direction) {
            return direction;
        }

        @Override
        public AxisDirection getDirectionReplacement(CoordinateSystemAxis axis, final AxisDirection direction) {
            return direction;
        }
    },

    /**
     * Axes having a <cite>wraparound</cite>
     * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getRangeMeaning() range meaning} are
     * shifted to their ranges of positive values. The units of measurement and range period (difference between
     * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getMaximumValue() maximum} and
     * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getMinimumValue() minimum value})
     * are unchanged.
     *
     * <p>Note that projecting geometry objects from the old to the new coordinate system may require
     * a non-affine conversion. Some geometries may need to be separated in two parts, and others may
     * need to be merged.</p>
     *
     * <div class="section">Usage</div>
     * The most frequent usage of this enum is for shifting longitude values from the [-180 … +180]° range
     * to the [0 … 360]° range. However this enum could also be used with climatological calendars if their
     * time axis has a wrapround range meaning.
     *
     * <p>Note that conversions from an coordinate system using the [-180 … +180]° range to a coordinate system
     * using the [0 … 360]° range may not be affine. For example the data in the West hemisphere ([-180 … 0]°)
     * may need to move on the right side of the East hemisphere ([180 … 360]°).</p>
     *
     * @see org.opengis.referencing.cs.RangeMeaning#WRAPAROUND
     */
    POSITIVE_RANGE {
        @Override
        public boolean accept(final CoordinateSystemAxis axis) {
            return true;
        }

        @Override
        @Deprecated
        public Unit<?> getUnitReplacement(final Unit<?> unit) {
            return unit;
        }

        @Override
        public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, final Unit<?> unit) {
            return unit;
        }

        @Override
        @Deprecated
        public AxisDirection getDirectionReplacement(final AxisDirection direction) {
            return direction;
        }

        @Override
        public AxisDirection getDirectionReplacement(CoordinateSystemAxis axis, final AxisDirection direction) {
            return direction;
        }
    }
}
