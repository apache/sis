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
import org.opengis.coverage.grid.GridCoordinates;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.jdk9.JDK9;


/**
 * A view over the low or high grid envelope coordinates.
 * This is not a general-purpose grid coordinates since it assumes a {@link GridExtent} coordinates layout.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
final class GridCoordinatesView implements GridCoordinates {
    /**
     * A reference to the coordinate array of the enclosing grid envelope.
     */
    private final long[] coordinates;

    /**
     * Index of the first value in the {@link #coordinates} array.
     * This is 0 for low values, or {@link #getDimension()} for high values.
     */
    private final int offset;

    /**
     * Creates a new view over the low or high coordinates.
     */
    GridCoordinatesView(final long[] coordinates, final int offset) {
        this.coordinates = coordinates;
        this.offset = offset;
    }

    /**
     * Returns the number of dimension.
     */
    @Override
    public final int getDimension() {
        return coordinates.length >>> 1;
    }

    /**
     * Returns all coordinate values.
     */
    @Override
    public final long[] getCoordinateValues() {
        return Arrays.copyOfRange(coordinates, offset, offset + getDimension());
    }

    /**
     * Returns the coordinate value for the specified dimension.
     */
    @Override
    public final long getCoordinateValue(final int index) {
        ArgumentChecks.ensureValidIndex(getDimension(), index);
        return coordinates[offset + index];
    }

    /**
     * Do not allow modification of grid coordinates since they are backed by {@link GridExtent}.
     */
    @Override
    public void setCoordinateValue(final int index, long value) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "GridCoordinates"));
    }

    /**
     * Returns a string representation of this grid coordinates for debugging purpose.
     */
    @Override
    public final String toString() {
        final StringBuilder buffer = new StringBuilder("GridCoordinates[");
        final int dimension = getDimension();
        for (int i=0; i<dimension; i++) {
            if (i != 0) buffer.append(' ');
            buffer.append(coordinates[i + offset]);
        }
        return buffer.append(']').toString();
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public final int hashCode() {
        long code = -3;                             // Arbitrary seed for differentiating from Arrays.hashCode(long[]).
        final int end = offset + getDimension();
        for (int i=offset; i<end; i++) {
            code = 31 * code + coordinates[i];
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
            return JDK9.equals(this.coordinates, this.offset, this.offset + this.getDimension(),
                               that.coordinates, that.offset, that.offset + that.getDimension());
        }
        return false;
    }
}
