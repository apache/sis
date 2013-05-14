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
package org.apache.sis.internal.netcdf.ucar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import ucar.nc2.Dimension;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;

import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.netcdf.WarningProducer;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;
import static org.apache.sis.util.collection.Containers.hashMapCapacity;


/**
 * Information about NetCDF coordinate system.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.14)
 * @version 0.3
 * @module
 */
final class CRSBuilder extends WarningProducer {
    /**
     * The NetCDF coordinate system to wrap.
     */
    private final CoordinateSystem netcdfCS;

    /**
     * The coordinate axes in natural (reverse of NetCDF) order, or {@code null} for inferring
     * it from the {@link #netcdfCS}.
     */
    private List<CoordinateAxis> axes;

    /**
     * Creates a new CRS builder.
     *
     * @param parent Where to send the warnings, or {@code null} if none.
     * @param cs The NetCDF coordinate system, or {@code null} if none.
     */
    CRSBuilder(final WarningProducer parent, final CoordinateSystem cs) {
        super(parent);
        netcdfCS = cs;
    }

    /**
     * Ensures that the given value is defined.
     */
    private static void ensureDefined(final String name, final Object value) throws IllegalStateException {
        if (value == null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueForProperty_1, name));
        }
    }

    /**
     * Returns the NetCDF coordinate axes in natural (reverse of NetCDF) order. The returned list is
     * usually the NetCDF {@linkplain CoordinateSystem#getCoordinateAxes() coordinate axes} list in
     * the reverse order.
     *
     * <p>By default, the returned list is modifiable. Any changes in the content of this list will
     * be reflected in the wrappers to be {@linkplain #build() build}. This is useful if the caller
     * wants to modify the axis order, as in the example below:</p>
     *
     * {@preformat java
     *     builder.setCoordinateSystem(...);
     *     Collection.sort(builder.getCoordinateAxes(), new CoordinateAxis.AxisComparator());
     * }
     *
     * @return The NetCDF coordinate axis in natural order (reverse of NetCDF order),
     *         or {@code null} if unknown.
     *
     * @see ucar.nc2.dataset.CoordinateAxis.AxisComparator
     */
    private List<CoordinateAxis> getCoordinateAxes() {
        if (axes == null && netcdfCS != null) {
            Collections.reverse(axes = new ArrayList<>(netcdfCS.getCoordinateAxes()));
        }
        return axes;
    }

    /**
     * Returns the dimensions of all axes, together with an axis associated to each dimension.
     * If more than one axis use the same dimension, then the first axis has precedence.
     *
     * <p>The domain of all axes (or the {@linkplain CoordinateSystem#getDomain() coordinate system
     * domain}) is often the same than the {@linkplain #getDomain() domain of the variable}, but
     * not necessarily. In particular, the relationship is not straightforward when the coordinate
     * system contains instances of {@link CoordinateAxis2D}.</p>
     *
     * @return All axis dimensions associated to their originating axis.
     * @throws IllegalStateException If the {@linkplain #getCoordinateAxes() coordinate axes}
     *         are not defined.
     *
     * @see CoordinateAxis#getDimensions()
     */
    public Map<Dimension,CoordinateAxis> getAxesDomain() throws IllegalStateException {
        final List<CoordinateAxis> axes = getCoordinateAxes();
        ensureDefined("axes", axes);
        final Map<Dimension,CoordinateAxis> map = new LinkedHashMap<>(hashMapCapacity(axes.size()));
        /*
         * Stores all dimensions in the map, together with an arbitrary axis. If there is no
         * conflict, we are done. If there is conflicts, then the first one-dimensional axis
         * (if any) will have precedence over all other axes for that dimension. If the conflict
         * involves only axes having 2 or more dimensions, then we will defer their handling
         * to a later stage.
         */
        Map<Dimension, Set<CoordinateAxis>> conflicts = null;
        for (final CoordinateAxis axis : axes) {
            final int rank = axis.getRank();
            for (int i=rank; --i>=0;) {
                final Dimension dimension = axis.getDimension(i);
                final CoordinateAxis previous = map.put(dimension, axis);
                if (previous != null) {
                    final int pr = previous.getRank();
                    if (pr != 1 && rank != 1) {
                        /*
                         * Found a conflict (two axes using the same dimension) that can not be
                         * resolved before the loop completion. Remember this conflict in order
                         * to process it later.
                         */
                        if (conflicts == null) {
                            conflicts = new HashMap<>(4);
                        }
                        Set<CoordinateAxis> deferred = conflicts.get(dimension);
                        if (deferred == null) {
                            deferred = new LinkedHashSet<>(4);
                            conflicts.put(dimension, deferred);
                        }
                        deferred.add(previous);
                        deferred.add(axis);
                    } else {
                        /*
                         * The conflict can be resolved by giving precedence to a one-dimensional
                         * axis and discart the other.
                         */
                        if (pr == 1) {
                            map.put(dimension, previous);
                        }
                        if (conflicts != null) {
                            conflicts.remove(dimension);
                        }
                    }
                }
            }
        }
        /*
         * At this point the map is fully build, but some values may be inaccurate if conflicts
         * exist. In such cases, we will first checks if there is any axis that can be assigned
         * to only one dimension, because all other dimensions are not available anymore.
         */
redo:   while (!isNullOrEmpty(conflicts)) {
            for (final Map.Entry<Dimension,Set<CoordinateAxis>> entry : conflicts.entrySet()) {
                final Dimension dimension = entry.getKey();
otherAxis:      for (final CoordinateAxis axis : entry.getValue()) {
                    for (int i=axis.getRank(); --i>=0;) {
                        final Dimension candidate = axis.getDimension(i);
                        if (candidate != dimension && conflicts.containsKey(candidate)) {
                            // Axis can be assigned to 2 or more dimensions. Search an other one.
                            continue otherAxis;
                        }
                    }
                    /*
                     * If we reach this point, then this axis can be associated only
                     * to the current dimension; no other dimension are available.
                     */
                    conflicts.remove(dimension);
                    map.put(dimension, axis);
                    continue redo; // Maybe some axes prior to this one can now be processed.
                }
            }
            /*
             * If we reach this point, we have not been able to process any axis.
             * Pickup what seems the "main" dimension according an arbitrary rule.
             */
            for (final Set<CoordinateAxis> as : conflicts.values()) {
                for (final CoordinateAxis axis : as) {
                    final Dimension dimension = axis.getDimension(findMainDimensionOf(axis));
                    if (conflicts.remove(dimension) != null) {
                        map.put(dimension, axis);
                        for (final Set<CoordinateAxis> toClean : conflicts.values()) {
                            toClean.remove(axis);
                        }
                        continue redo; // Maybe some other axes can now be processed.
                    }
                }
            }
            /*
             * If we reach this point, there is no "main" dimension available. Such case should
             * never happen for two-dimensional axes, but could happen for axes having three or
             * more dimensions. Such axes do not exist in the NetCDF API at the time of writting,
             * but if they appear in a future version there is where we should complete the code.
             */
            throw new UnsupportedOperationException();
        }
        return map;
    }

    /**
     * Returns the index of what seems to be the "main" dimension of the given coordinate axis.
     * For {@link CoordinateAxis1D}, the returned index is trivially 0 in all cases.
     * For {@link CoordinateAxis2D}, the default implementation returns the index (0 or 1)
     * of the dimension for which the largest increment is found.
     *
     * <p>This method affects the sort performed by {@link #sortAxesAccordingDomain()}.</p>
     *
     * @param  axis The axis for which to get the index of the "main" dimension.
     * @return Index of the axis dimension having the largest increment.
     */
    private static int findMainDimensionOf(final CoordinateAxis axis) {
        int main = 0;
        if (axis instanceof CoordinateAxis2D) {
            final CoordinateAxis2D a2 = (CoordinateAxis2D) axis;
            final int    si = a2.getShape(0);
            final int    sj = a2.getShape(1);
            final double di = Math.abs(a2.getCoordValue(0, sj >>> 1) - a2.getCoordValue(si-1, sj >>> 1)) / si;
            final double dj = Math.abs(a2.getCoordValue(si >>> 1, 0) - a2.getCoordValue(si >>> 1, sj-1)) / sj;
            if (dj > di) {
                main = 1;
            }
        }
        return main;
    }
}
