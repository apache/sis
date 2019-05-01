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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import org.apache.sis.util.ArgumentChecks;


/**
 * A view over the low or high grid envelope coordinates.
 * This is not a general-purpose grid coordinates since it assumes a {@link GridExtent} coordinates layout.
 *
 * <div class="note"><b>Upcoming API generalization:</b>
 * this class may implement the {@code GridCoordinates} interface in a future Apache SIS version.
 * This is pending GeoAPI update.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class GridCoordinatesView {
    /**
     * A reference to the coordinate array of the enclosing grid envelope.
     */
    private final long[] ordinates;

    /**
     * Index of the first value in the {@link #ordinates} array.
     * This is 0 for low values, or {@link #getDimension()} for high values.
     */
    private final int offset;

    /**
     * Creates a new view over the low or high coordinates.
     */
    GridCoordinatesView(final long[] ordinates, final int offset) {
        this.ordinates = ordinates;
        this.offset = offset;
    }

    /**
     * Returns the number of dimension.
     */
    public final int getDimension() {
        return ordinates.length >>> 1;
    }

    /**
     * Returns all coordinate values.
     */
    public final long[] getCoordinateValues() {
        return Arrays.copyOfRange(ordinates, offset, offset + getDimension());
    }

    /**
     * Returns the coordinate value for the specified dimension.
     */
    public final long getCoordinateValue(final int index) {
        ArgumentChecks.ensureValidIndex(getDimension(), index);
        return ordinates[offset + index];
    }

    /**
     * Do not allow modification of grid coordinates since they are backed by {@link GridExtent}.
     */
//  public void setCoordinateValue(final int index, long value) {
//      throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "GridCoordinates"));
//  }

    /**
     * Returns a string representation of this grid coordinates for debugging purpose.
     */
    @Override
    public final String toString() {
        return "GridCoordinates".concat(Arrays.toString(getCoordinateValues()));
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public final int hashCode() {
        long code = -3;                             // Arbitrary seed for differentiating from Arrays.hashCode(long[]).
        final int end = offset + getDimension();
        for (int i=offset; i<end; i++) {
            code = 31 * code + ordinates[i];
        }
        return Long.hashCode(code);
    }

    /**
     * Compares this grid coordinates with the specified object for equality.
     *
     * @param  object  the object to compares with this grid coordinates.
     * @return {@code true} if the given object is equal to this grid coordinates.
     */
    @Override
    public final boolean equals(final Object object) {
        if (object == this) {                           // Slight optimization.
            return true;
        }
        /*
         * We do not require the exact same class because we want to accept
         * immutable grid coordinates as equal to mutable grid coordinates.
         */
        if (object instanceof GridCoordinatesView) {
            final GridCoordinatesView that = (GridCoordinatesView) object;
            final int dimension = getDimension();
            if (dimension == that.getDimension()) {
                // TODO: use Arrays.equals(...) with JDK9 instead.
                for (int i=0; i<dimension; i++) {
                    if (ordinates[offset + i] != that.ordinates[that.offset + i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
