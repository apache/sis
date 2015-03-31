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
package org.apache.sis.math;

import java.util.Iterator;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.resources.Errors;


/**
 * A sequence of {@code DirectPosition}s which is a view over arrays of ordinate values.
 * Each dimension is stored in a separated array. For example this class can view three
 * arrays (x[], y[], and z[]) as a sequence of three-dimensional {@code DirectPosition}.
 *
 * <div class="section">Limitation</div>
 * This class is also its own iterator. All calls to {@link #iterator()} return the same iterator,
 * and all calls to {@link #next()} return the same {@code DirectPosition} instance. Consequently
 * this class is not suitable for normal use where many objects may iterate over the sequence in
 * same time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class CompoundDirectPositions implements DirectPosition, Iterable<DirectPosition>, Iterator<DirectPosition> {
    /**
     * The arrays of ordinate values, for example (x[], y[], z[]).
     */
    private final double[][] ordinates;

    /**
     * Length of all ordinate values minus one.
     */
    private final int last;

    /**
     * Index of the next element to be returned by {@link #next()}.
     */
    private int index;

    /**
     * Wraps the given array of ordinate values.
     */
    CompoundDirectPositions(final double[]... ordinates) {
        this.ordinates = ordinates;
        final int length = ordinates[0].length;
        for (int i=1; i<ordinates.length; i++) {
            if (ordinates[i].length != length) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedArrayLengths));
            }
        }
        last = length - 1;
    }

    /**
     * Starts a new iteration.
     *
     * @return Always {@code this}.
     */
    @Override
    public Iterator<DirectPosition> iterator() {
        index = -1;
        return this;
    }

    /**
     * Returns {@code true} if there is more position to return.
     */
    @Override
    public boolean hasNext() {
        return index < last;
    }

    /**
     * Sets this object to the next position and return it.
     *
     * @return Always {@code this}.
     */
    @Override
    public DirectPosition next() {
        index++;
        return this;
    }

    /**
     * Returns {@code this} since this object is already a direct position.
     *
     * @return Always {@code this}.
     */
    @Override
    public DirectPosition getDirectPosition() {
        return this;
    }

    /**
     * Returns {@code null} since there is no CRS associated to this object.
     *
     * @return Always {@code null}.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return null;
    }

    /**
     * Returns the number of dimensions.
     */
    @Override
    public int getDimension() {
        return ordinates.length;
    }

    /**
     * Return the ordinate value at the given dimension.
     */
    @Override
    public double getOrdinate(final int dimension) {
        return ordinates[dimension][index];
    }

    /**
     * Not needed.
     */
    @Override
    public double[] getCoordinate() {
        throw new UnsupportedOperationException();
    }

    /**
     * Not needed.
     */
    @Override
    public void setOrdinate(int dimension, double value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not needed.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
