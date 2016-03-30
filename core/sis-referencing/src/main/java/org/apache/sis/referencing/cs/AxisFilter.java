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

import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import javax.measure.unit.Unit;


/**
 * Modifications to apply on the axes of a coordinate system in order to produce a new coordinate system.
 * {@code AxisFilter} can specify the axes to exclude in the new coordinate system, or specify different
 * units and directions associated to the axes.
 *
 * <div class="note"><b>Terminology note</b>
 * the word <cite>“filter”</cite> is understood here as <cite>“a computer program or subroutine to process a stream,
 * producing another stream”</cite> (<a href="http://en.wikipedia.org/wiki/Filter_%28software%29">Wikipedia</a>).
 * </div>
 *
 * <p>Note that filtering one or more axes may result in a change of coordinate system type.
 * For example excluding the <var>z</var> axis of a {@linkplain DefaultCylindricalCS cylindrical} coordinate system
 * results in a {@linkplain DefaultPolarCS polar} coordinate system.</p>
 *
 * <div class="section">Limitations</div>
 * This interface is not for changing axis order.
 * For changing axis order in addition to axis directions or units, see {@link AxesConvention}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 *
 * @see CoordinateSystems#replaceAxes(CoordinateSystem, AxisFilter)
 */
public interface AxisFilter {
    /**
     * Returns {@code true} if the given axis shall be included in the new coordinate system.
     *
     * @param  axis The axis to test.
     * @return {@code true} if the given axis shall be included in the new coordinate system.
     */
    boolean accept(CoordinateSystemAxis axis);

    /**
     * Returns a replacement for the given axis direction.
     *
     * <div class="note"><b>Example:</b>
     * for forcing the direction of the <var>z</var> axis toward up while leaving other axes unchanged,
     * one can write:
     *
     * {@preformat java
     *     &#64;Override
     *     public getDirectionReplacement(CoordinateSystemAxis axis, AxisDirection direction) {
     *         if (direction == AxisDirection.DOWN) {
     *             direction = AxisDirection.UP;
     *         }
     *         return direction;
     *     }
     * }
     * </div>
     *
     * @param  axis The axis for which to change axis direction, if desired.
     * @param  direction The original axis direction.
     * @return The new axis direction, or {@code direction} if there is no change.
     *
     * @since 0.7
     */
    AxisDirection getDirectionReplacement(CoordinateSystemAxis axis, AxisDirection direction);

    /**
     * @deprecated Use {@link #getDirectionReplacement(CoordinateSystemAxis, AxisDirection)} instead.
     *
     * @param  direction The original axis direction.
     * @return The new axis direction, or {@code direction} if there is no change.
     */
    @Deprecated
    AxisDirection getDirectionReplacement(AxisDirection direction);

    /**
     * Returns a replacement for the given axis unit.
     *
     * <div class="note"><b>Example:</b>
     * for replacing all angular units of a coordinate system to degrees (regardless what the original
     * angular units were) while leaving other kinds of units unchanged, one can write:
     *
     * {@preformat java
     *     &#64;Override
     *     public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit) {
     *         if (Units.isAngular(unit)) {
     *             unit = NonSI.DEGREE_ANGLE;
     *         }
     *         return unit;
     *     }
     * }
     * </div>
     *
     * @param  axis The axis for which to change unit, if desired.
     * @param  unit The original axis unit.
     * @return The new axis unit, or {@code unit} if there is no change.
     *
     * @since 0.7
     */
    Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit);

    /**
     * @deprecated Use {@link #getUnitReplacement(CoordinateSystemAxis, Unit)} instead.
     *
     * @param  unit The original axis unit.
     * @return The new axis unit, or {@code unit} if there is no change.
     */
    @Deprecated
    Unit<?> getUnitReplacement(Unit<?> unit);
}
