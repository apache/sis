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
package org.apache.sis.internal.referencing;

import java.time.Instant;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.geometry.AbstractEnvelope;


/**
 * Convenience methods for accessing the temporal component of an object (envelope, grid geometry…).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class TemporalAccessor {
    /**
     * Dimension of the temporal component.
     */
    public final int dimension;

    /**
     * Converter from floating point numbers to dates.
     */
    public final DefaultTemporalCRS timeCRS;

    /**
     * Creates a new accessor for the given coordinate reference system.
     *
     * @param  dim  the dimension of the temporal component.
     * @param  crs  the coordinate reference system.
     */
    private TemporalAccessor(final int dim, final TemporalCRS crs) {
        dimension = dim;
        timeCRS = DefaultTemporalCRS.castOrCopy(crs);
    }

    /**
     * Creates a new temporal accessor for elements at the given dimensions.
     * This method search for a temporal component in the given CRS.
     *
     * @param  crs  the coordinate reference system which may contain a temporal component, or {@code null}.
     * @param  dim  offset to add to the dimension indices. This is usually zero.
     * @return the temporal accessor, or {@code null} if no temporal component has been found.
     */
    public static TemporalAccessor of(final CoordinateReferenceSystem crs, int dim) {
        if (crs instanceof TemporalCRS) {
            return new TemporalAccessor(dim, (TemporalCRS) crs);
        }
        if (crs instanceof CompoundCRS) {
            for (final CoordinateReferenceSystem component : ((CompoundCRS) crs).getComponents()) {
                final TemporalAccessor accessor = of(component, dim);
                if (accessor != null) {
                    return accessor;
                }
                dim += ReferencingUtilities.getDimension(component);
            }
        }
        return null;
    }

    /**
     * Returns the lower and upper values in the given envelope. It is caller's responsibility to ensure that
     * the envelope CRS is the same than the one used for creating this {@code TemporalAccessor}.
     *
     * @param  envelope  the envelope from which to get the start time end end time.
     * @return the start time and end time in an array of length 1 or 2, or an empty array if none.
     */
    @SuppressWarnings("fallthrough")
    public Instant[] getTimeRange(final AbstractEnvelope envelope) {
        Instant startTime = timeCRS.toInstant(envelope.getLower(dimension));
        Instant endTime   = timeCRS.toInstant(envelope.getUpper(dimension));
        if (startTime == null) {
            startTime = endTime;
            endTime = null;
        }
        Instant[] times = new Instant[(endTime != null) ? 2 : (startTime != null) ? 1 : 0];
        switch (times.length) {
            default: times[1] = endTime;        // Fall through.
            case 1:  times[0] = startTime;      // Fall through.
            case 0:  break;
        }
        return times;
    }
}
