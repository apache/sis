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
package org.apache.sis.referencing.internal.shared;

import java.time.Instant;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.measure.Range;


/**
 * Convenience methods for accessing the temporal component of an object (envelope, grid geometry…).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TemporalAccessor {
    /**
     * Empty array of instants.
     */
    public static final Instant[] EMPTY = new Instant[0];

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
     * This method searches for a temporal component in the given CRS.
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
                dim += CRS.getDimensionOrZero(component);
            }
        }
        return null;
    }

    /**
     * Returns the lower and upper values in the given envelope. It is caller's responsibility to ensure
     * that the envelope CRS is the same as the one used for creating this {@code TemporalAccessor}.
     *
     * @param  envelope  the envelope from which to get the start time end end time.
     * @return the start time and end time in an array of length 1 or 2, or an empty array if none.
     */
    @SuppressWarnings({"fallthrough", "ReturnOfCollectionOrArrayField"})
    public Instant[] getTimeBounds(final AbstractEnvelope envelope) {
        Instant startTime = timeCRS.toInstant(envelope.getLower(dimension));
        Instant endTime   = timeCRS.toInstant(envelope.getUpper(dimension));
        if (startTime == null) {
            if (endTime == null) {
                return EMPTY;
            }
            startTime = endTime;
            endTime = null;
        }
        final var times = new Instant[(endTime != null) ? 2 : 1];
        switch (times.length) {
            default: times[1] = endTime;        // Fall through.
            case 1:  times[0] = startTime;      // Fall through.
            case 0:  break;
        }
        return times;
    }

    /**
     * Returns the temporal range of given envelope. It is caller's responsibility to ensure that
     * the envelope CRS is the same as the one used for creating this {@code TemporalAccessor}.
     *
     * @param  envelope  the envelope from which to get the start time end end time.
     * @return the start time and end time.
     *
     * @see org.apache.sis.geometry.Envelopes#toTimeRange(Envelope)
     */
    public Range<Instant> getTimeRange(final Envelope envelope) {
        return new Range<>(Instant.class,
                timeCRS.toInstant(envelope.getMinimum(dimension)), true,
                timeCRS.toInstant(envelope.getMaximum(dimension)), true);
    }

    /**
     * Copies the temporal extent from an envelope to a metadata object.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target temporal extent.
     */
    public void setTemporalExtent(final Envelope envelope, final DefaultTemporalExtent target) {
        target.setBounds(timeCRS.toInstant(envelope.getMinimum(dimension)),
                         timeCRS.toInstant(envelope.getMaximum(dimension)));
    }
}
