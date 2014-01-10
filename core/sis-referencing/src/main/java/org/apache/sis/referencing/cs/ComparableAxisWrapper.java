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

import java.util.Arrays;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.internal.referencing.AxisDirections;


/**
 * Wraps a {@link CoordinateSystemAxis} for comparison purpose. The sorting order tries to favor
 * a right-handed system. Compass directions like North and East are first. Vertical or temporal
 * directions like Up or Down are last.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.4)
 * @version 0.4
 * @module
 */
final class ComparableAxisWrapper implements Comparable<ComparableAxisWrapper> {
    /**
     * The wrapped axis.
     */
    private final CoordinateSystemAxis axis;

    /**
     * The direction along meridian, or {@code null} if none.
     */
    private final DirectionAlongMeridian meridian;

    /**
     * Creates a new wrapper for the given axis.
     */
    private ComparableAxisWrapper(final CoordinateSystemAxis axis) {
        this.axis = axis;
        meridian = DirectionAlongMeridian.parse(axis.getDirection());
    }

    /**
     * Compares with the specified object. See class javadoc for a description of the sorting order.
     */
    @Override
    public int compareTo(final ComparableAxisWrapper that) {
        final AxisDirection d1 = this.axis.getDirection();
        final AxisDirection d2 = that.axis.getDirection();
        final int compass = AxisDirections.angleForCompass(d2, d1);
        if (compass != Integer.MIN_VALUE) {
            return compass;
        }
        if (AxisDirections.isCompass(d1)) {
            assert !AxisDirections.isCompass(d2) : d2;
            return -1;
        }
        if (AxisDirections.isCompass(d2)) {
            assert !AxisDirections.isCompass(d1) : d1;
            return +1;
        }
        if (meridian != null) {
            if (that.meridian != null) {
                return meridian.compareTo(that.meridian);
            }
            return -1;
        } else if (that.meridian != null) {
            return +1;
        }
        return 0;
    }

    /**
     * Sorts the specified axis in an attempt to create a right-handed system.
     * The sorting is performed in place. This method returns {@code true} if
     * at least one axis moved.
     */
    public static boolean sort(final CoordinateSystemAxis[] axis) {
        final ComparableAxisWrapper[] wrappers = new ComparableAxisWrapper[axis.length];
        for (int i=0; i<axis.length; i++) {
            wrappers[i] = new ComparableAxisWrapper(axis[i]);
        }
        Arrays.sort(wrappers);
        boolean changed = false;
        for (int i=0; i<axis.length; i++) {
            final CoordinateSystemAxis a = wrappers[i].axis;
            changed |= (axis[i] != a);
            axis[i] = a;
        }
        return changed;
    }
}
