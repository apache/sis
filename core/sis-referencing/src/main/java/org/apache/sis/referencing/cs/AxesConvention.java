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

import org.opengis.referencing.cs.AxisDirection; // For javadoc
import org.opengis.referencing.cs.CoordinateSystem;


/**
 * High-level characteristics about the axes of a coordinate system.
 * This enumeration provides a convenient way to identify some common axes conventions like
 * (<var>latitude</var>, <var>longitude</var>) versus (<var>longitude</var>, <var>latitude</var>) axis order,
 * or [-180 … +180]° versus [0 … 360]° longitude range.
 *
 * <p>Enumeration values are inferred from the properties of given {@link CoordinateSystem} instances.
 * This enumeration does not add new information and does not aim to cover all possible conventions – it is
 * only a convenience for identifying some common patterns.</p>
 *
 * {@section Axis order}
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
 * with older OGC specifications or other softwares, CRS forced to longitude first axis order can be created using the
 * {@link #NORMALIZED} enumeration value.</p>
 *
 * {@section Range of longitude values}
 * Most geographic CRS have a longitude axis defined in the [-180 … +180]° range. All map projections in Apache SIS are
 * designed to work in that range. This is also the range of {@link Math} trigonometric functions like {@code atan2(y,x)}.
 * However some data use the [0 … 360]° range instead. A geographic CRS can be shifted to that range of longitude values
 * using the {@link #POSITIVE_RANGE} enumeration value. The choice of longitude range will impact not only some
 * coordinate conversions, but also the methods that verify the <cite>domain of validity</cite>
 * (e.g. {@link org.apache.sis.geometry.GeneralEnvelope#normalize()}).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.20)
 * @version 0.4
 * @module
 *
 * @see AbstractCS#forConvention(AxesConvention)
 */
public enum AxesConvention {
    /**
     * Axes order, direction and units are forced to commonly used pre-defined values.
     * This enum identifies the following changes to apply on a coordinate system:
     *
     * <ul>
     *   <li>Directions opposites to the following ones are replaced by their "forward" counterpart
     *       (e.g. {@code SOUTH} → {@code NORTH}):
     *     <ul>
     *       <li>{@link AxisDirection#EAST EAST}, {@link AxisDirection#NORTH NORTH},
     *           {@link AxisDirection#UP UP}, {@link AxisDirection#FUTURE FUTURE} —
     *           commonly used directions for (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>) coordinates.</li>
     *       <li>{@link AxisDirection#DISPLAY_RIGHT DISPLAY_RIGHT}, {@link AxisDirection#DISPLAY_DOWN DISPLAY_DOWN} —
     *           commonly used (<var>x</var>, <var>y</var>) directions for screen devices.</li>
     *       <li>{@link AxisDirection#ROW_POSITIVE ROW_POSITIVE},
     *           {@link AxisDirection#COLUMN_POSITIVE COLUMN_POSITIVE} — indices in grids or matrices.</li>
     *     </ul>
     *   </li>
     *   <li>Axes with the new directions are reordered for a <cite>right-handed</cite> coordinate system.</li>
     *   <li>Angular units are set to {@link javax.measure.unit.NonSI#DEGREE_ANGLE}.</li>
     *   <li>Linear units are set to {@link javax.measure.unit.SI#METRE}.</li>
     *   <li>Temporal units are set to {@link javax.measure.unit.NonSI#DAY}.</li>
     * </ul>
     *
     * {@note The rules for normalized coordinate systems may be adjusted in future SIS versions based on experience
     *        gained. For more predictable results, consider using the <code>RIGHT_HANDED</code> enum instead.}
     */
    NORMALIZED,

    /**
     * Axes are reordered for a <cite>right-handed</cite> coordinate system. Directions, ranges and units are unchanged.
     * This enum is often used for deriving a coordinate system with the (<var>longitude</var>, <var>latitude</var>) or
     * (<var>x</var>,<var>y</var>) axis order. While it works in many cases, note that a right-handed coordinate system
     * does not guarantee that longitude or <var>x</var> axis will be first in every cases. The most notable exception
     * is the (North, West) case.
     *
     * {@note We do not provide a "<cite>longitude or <var>x</var> axis first</cite>" enumeration value because
     *        such criterion is hard to apply to inter-cardinal directions and has no meaning for map projections
     *        over a pole, while the right-handed rule can apply everywhere.}
     *
     * {@example The following table lists some axis orientations in the first column, and
     *           how those axes are reordered in a right-handed coordinate system (second column):
     * <ul>
     *   <table class="sis">
     *     <tr><th>Left-handed</th>   <th>Right-handed</th>  <th>Remarks</th></tr>
     *     <tr><td>(North, East)</td> <td>(East, North)</td> <td>This is the most common case.</td></tr>
     *     <tr><td>(West, North)</td> <td>(North, West)</td> <td>This right-handed system has latitude first.</td></tr>
     *     <tr><td>(South, West)</td> <td>(West, South)</td> <td>Used for the mapping of southern Africa.</td></tr>
     *     <tr><td>(South along 0°,<br>South along 90° West)</td>
     *         <td>(South along 90° West,<br>South along 0°)</td> <td>Can be used for the mapping of North pole.</td></tr>
     *   </table>
     * </ul>}
     *
     * @see org.apache.sis.referencing.cs.CoordinateSystems#angle(AxisDirection, AxisDirection)
     */
    RIGHT_HANDED,

    /**
     * Axes having a <cite>wraparound</cite>
     * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getRangeMeaning() range meaning}
     * are shifted to their ranges of positive values. The unit and range period are unchanged.
     *
     * <p>The most frequent usage of this enum is for shifting longitude values from the [-180 … +180]° range
     * to the [0 … 360]° range. However this enum could also be used with climatological calendars if their
     * time axis has a wrapround range meaning.</p>
     *
     * @see org.opengis.referencing.cs.RangeMeaning#WRAPAROUND
     */
    POSITIVE_RANGE
}
