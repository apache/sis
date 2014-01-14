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

import static java.util.Collections.singletonMap;
import org.apache.sis.util.CharSequences;


/**
 * Derives an coordinate system from an existing one for {@link AxesConvention}.
 * The main usage for this class is to reorder the axes in some fixed order like
 * (<var>x</var>, <var>y</var>, <var>z</var>) or (<var>longitude</var>, <var>latitude</var>).
 *
 * <p>This class implements {@link Comparable} for opportunist reasons.
 * This should be considered as an implementation details.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
final class Normalizer implements Comparable<Normalizer> {
    /**
     * The axis to be compared by {@link #compareTo(Normalizer)}.
     */
    private final CoordinateSystemAxis axis;

    /**
     * The direction along meridian, or {@code null} if none. This is inferred from {@link #axis}
     * at construction time in order to compute it only once before to sort an array of axes.
     */
    private final DirectionAlongMeridian meridian;

    /**
     * For internal usage by {@link #sort(CoordinateSystemAxis[])} only.
     */
    private Normalizer(final CoordinateSystemAxis axis) {
        this.axis = axis;
        meridian = DirectionAlongMeridian.parse(axis.getDirection());
    }

    /**
     * Compares two axis for an order that try to favor right-handed coordinate systems.
     * Compass directions like North and East are first. Vertical directions like Up or Down are next.
     */
    @Override
    public int compareTo(final Normalizer that) {
        final AxisDirection d1 = this.axis.getDirection();
        final AxisDirection d2 = that.axis.getDirection();
        final int compass = AxisDirections.angleForCompass(d2, d1);
        if (compass != Integer.MIN_VALUE) {
            return compass;
        }
        if (meridian != null) {
            if (that.meridian != null) {
                return meridian.compareTo(that.meridian);
            }
            return -1;
        } else if (that.meridian != null) {
            return +1;
        }
        return d1.ordinal() - d2.ordinal();
    }

    /**
     * Sorts the specified axis in an attempt to create a right-handed system.
     * The sorting is performed in place. This method returns {@code true} if
     * at least one axis moved as result of this method call.
     */
    static boolean sort(final CoordinateSystemAxis[] axis) {
        final Normalizer[] wrappers = new Normalizer[axis.length];
        for (int i=0; i<axis.length; i++) {
            wrappers[i] = new Normalizer(axis[i]);
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

    /**
     * Reorder the axes in an attempt to get a right-handed system.
     * If no axis change is needed, then this method returns {@code cs} unchanged.
     */
    static AbstractCS normalize(final AbstractCS cs) {
        final int dimension = cs.getDimension();
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[dimension];
        for (int i=0; i<dimension; i++) {
            axes[i] = cs.getAxis(i);
        }
        /*
         * Sorts the axis in an attempt to create a right-handed system
         * and creates a new Coordinate System if at least one axis changed.
         */
        if (!sort(axes)) {
            return cs;
        }
        final StringBuilder buffer = (StringBuilder) CharSequences.camelCaseToSentence(cs.getInterface().getSimpleName());
        return cs.createSameType(singletonMap(AbstractCS.NAME_KEY, DefaultCompoundCS.nameFor(buffer, axes)), axes);
    }
}
