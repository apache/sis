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
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;


/**
 * Modifications to apply on the axes of a coordinate system in order to produce a new coordinate system.
 * {@code AxisFilter} can specify the axes to exclude in the new coordinate system, or specify different
 * units and directions associated to the axes.
 *
 * <div class="note"><b>Terminology note:</b>
 * the word <q>filter</q> is understood here as <q>a computer program or subroutine to process a stream,
 * producing another stream</q> (<a href="https://en.wikipedia.org/wiki/Filter_%28software%29">Wikipedia</a>).
 * </div>
 *
 * <p>Note that filtering one or more axes may result in a change of coordinate system type.
 * For example, excluding the <var>z</var> axis of a {@linkplain DefaultCylindricalCS cylindrical} coordinate system
 * results in a {@linkplain DefaultPolarCS polar} coordinate system.</p>
 *
 * <h2>Default implementation</h2>
 * All methods in this interface have a default implementation equivalent to <i>no-operation</i>.
 * Implementers need to override only the methods for the aspects to change.
 *
 * <h2>Limitations</h2>
 * This interface is not for changing axis order.
 * For changing axis order in addition to axis directions or units, see {@link AxesConvention}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.7
 *
 * @see CoordinateSystems#replaceAxes(CoordinateSystem, AxisFilter)
 *
 * @since 0.6
 */
public interface AxisFilter {
    /**
     * Returns {@code true} if the given axis shall be included in the new coordinate system.
     * The default implementation unconditionally returns {@code true}.
     *
     * @param  axis  the axis to test.
     * @return {@code true} if the given axis shall be included in the new coordinate system.
     */
    default boolean accept(CoordinateSystemAxis axis) {
        return true;
    }

    /**
     * Returns a replacement for the given axis direction.
     * The default implementation unconditionally returns the given {@code direction} unchanged.
     *
     * <h4>Example</h4>
     * For forcing the direction of the <var>z</var> axis toward up while leaving other axes unchanged,
     * one can write:
     *
     * {@snippet lang="java" :
     *     @Override
     *     public getDirectionReplacement(CoordinateSystemAxis axis, AxisDirection direction) {
     *         if (direction == AxisDirection.DOWN) {
     *             direction = AxisDirection.UP;
     *         }
     *         return direction;
     *     }
     * }
     *
     * @param  axis       the axis for which to change axis direction, if desired.
     * @param  direction  the original axis direction.
     * @return the new axis direction, or {@code direction} if there is no change.
     *
     * @since 0.7
     */
    default AxisDirection getDirectionReplacement(CoordinateSystemAxis axis, AxisDirection direction) {
        return direction;
    }

    /**
     * Returns a replacement for the given axis unit.
     * The default implementation unconditionally returns the given {@code unit} unchanged.
     *
     * <h4>Example</h4>
     * For replacing all angular units of a coordinate system to degrees (regardless what the original
     * angular units were) while leaving other kinds of units unchanged, one can write:
     *
     * {@snippet lang="java" :
     *     @Override
     *     public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit) {
     *         if (Units.isAngular(unit)) {
     *             unit = Units.DEGREE;
     *         }
     *         return unit;
     *     }
     * }
     *
     * @param  axis  the axis for which to change unit, if desired.
     * @param  unit  the original axis unit.
     * @return the new axis unit, or {@code unit} if there is no change.
     *
     * @since 0.7
     */
    default Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit) {
        return unit;
    }
}
