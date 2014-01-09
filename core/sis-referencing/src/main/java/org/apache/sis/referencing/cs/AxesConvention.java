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


/**
 * High-level characteristics about the axes of an ellipsoidal coordinate system.
 * This enumeration provides a convenient way to identify some common axes conventions like
 * (<var>latitude</var>, <var>longitude</var>) versus (<var>longitude</var>, <var>latitude</var>) order,
 * or [-180 … +180]° versus [0 … 360]° longitude range.
 *
 * <p>Enumeration values are inferred from the properties of given {@link EllipsoidalCS} instances.
 * This enumeration does not add new information and does not aim to cover all possible conventions – it is
 * only a convenience for identifying some common patterns.</p>
 *
 * {@section Range of longitude values}
 * This enumeration provides a way to specify whether the range of longitude values is expected to be positive
 * (typically [0 … 360]°) or if the range mixes positive and negative values (typically [-180 … +180]°).
 * This information usually has no impact on coordinate transformations. However, they have an impact on
 * methods that verify the <cite>domain of validity</cite>, for example
 * {@link org.apache.sis.geometry.GeneralEnvelope#normalize()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.20)
 * @version 0.4
 * @module
 */
public enum AxesConvention {
    /**
     * Axis order and ranges are as specified by the authority. For ellipsoidal coordinate systems defined by
     * EPSG database, this is often - but not always - (<var>latitude</var>, <var>longitude</var>) axis order
     * with longitude values in the [-180 … +180]° range.
     */
    AS_SPECIFIED,

    /**
     * Axes are reordered for a <cite>right-handed</cite> coordinate system. Axis orientations and ranges are unchanged.
     * This enum is often used for deriving a coordinate system with the (<var>longitude</var>, <var>latitude</var>)
     * or (<var>x</var>, <var>y</var>) axis order, but actually does not guarantee that longitude or <var>x</var> will
     * be first as in the (South, East) example below. Note that the "<var>x</var> first" criterion has no meaning for
     * map projections having their origin on a pole, while the right-handed rule applies everywhere.
     *
     * {@example The following table lists some axis orientations in the first column, and
     *           how those axes are reordered in a right-handed coordinate system (second column):
     * <ul>
     *   <table>
     *     <tr><th>Left-handed</th>   <th>Right-handed</th>  <th>Remarks</th></tr>
     *     <tr><td>(North, East)</td> <td>(East, North)</td> <td>This is the most common case.</td></tr>
     *     <tr><td>(East, South)</td> <td>(South, East)</td> <td>This right-handed system has latitude first.</td></tr>
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
     * Axes having a <cite>wraparound</cite> range meaning are shifted to their ranges of positive values.
     * The unit and range period are unchanged.
     *
     * <p>The most frequent usage of this enum is for shifting longitude values from [-180 … +180]° to the [0 … 360]°
     * range. However this enum could also be used with climatological calendars if their time axis has a wrapround
     * range meaning.</p>
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getRangeMeaning()
     * @see org.opengis.referencing.cs.RangeMeaning#WRAPAROUND
     */
    POSITIVE_RANGE
}
