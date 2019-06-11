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
package org.apache.sis.geometry;

/*
 * Do not add dependency to java.awt.Rectangle2D in this class, because not every platforms
 * support Java2D (e.g. Android),  or applications that do not need it may want to avoid to
 * force installation of the Java2D module (e.g. JavaFX/SWT).
 */
import java.util.Arrays;

import static org.apache.sis.util.ArgumentChecks.ensureValidIndex;
import static org.apache.sis.util.ArgumentChecks.ensureValidIndexRange;


/**
 * A view over a sub-set of the dimensions of a {@link GeneralEnvelope}.
 * This class doesn't keep any reference to the original envelope;
 * only the internal array is shared.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 * @module
 */
final class SubEnvelope extends GeneralEnvelope {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7241242611077979466L;

    /**
     * The index of the first valid coordinate value of the lower corner in the {@link #coordinates} array.
     *
     * @see ArrayEnvelope#beginIndex()
     */
    private final int beginIndex;

    /**
     * The index after the last valid coordinate value of the lower corner  in the {@link #coordinates} array.
     *
     * @see ArrayEnvelope#endIndex()
     */
    private final int endIndex;

    /**
     * Creates a new envelope over a portion of the given array. This constructor stores the given
     * reference directly; it does <strong>not</strong> clone the given array. This is the desired
     * behavior for allowing the {@code SubEnvelope} view to be "live".
     *
     * @param coordinates   the array of coordinate values to store directly (not cloned).
     * @param beginIndex    the index of the first valid coordinate value of the lower corner in the coordinates array.
     * @param endIndex      the index after the last valid coordinate value of the lower corner in the coordinates array.
     */
    SubEnvelope(final double[] coordinates, final int beginIndex, final int endIndex) {
        super(coordinates);
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    /**
     * Returns the index of the first valid coordinate value of the lower corner in the coordinates array.
     * This information is used by super-class methods.
     */
    @Override
    final int beginIndex() {
        return beginIndex;
    }

    /**
     * Returns the index after the last valid coordinate value of the lower corner in the coordinates array.
     * This information is used by super-class methods.
     */
    @Override
    final int endIndex() {
        return endIndex;
    }

    /**
     * Returns the number of dimensions in the portion of the array used by this {@code SubEnvelope}.
     */
    @Override
    public int getDimension() {
        return endIndex - beginIndex;
    }

    /**
     * Must be overridden, since the super-class method does not handle the index range
     * for performance reasons.
     */
    @Override
    public double getLower(final int dimension) throws IndexOutOfBoundsException {
        ensureValidIndex(endIndex, dimension);
        return coordinates[dimension + beginIndex];
    }

    /**
     * Must be overridden, since the super-class method does not handle the index range
     * for performance reasons.
     */
    @Override
    public double getUpper(final int dimension) throws IndexOutOfBoundsException {
        ensureValidIndex(endIndex, dimension);
        return coordinates[dimension + beginIndex + (coordinates.length >>> 1)];
    }

    /**
     * Must be overridden, since the super-class method processes the full array as a whole.
     */
    @Override
    public void setRange(int dimension, final double lower, final double upper)
            throws IndexOutOfBoundsException
    {
        ensureValidIndex(endIndex, dimension);
        /*
         * The check performed here shall be identical to the super-class method, which is itself
         * identical to ArrayEnvelope.verifyRanges(crs, coordinates) except that there is no loop.
         */
        if (lower > upper && crs != null && !isWrapAround(crs, dimension)) {
            throw new IllegalArgumentException(illegalRange(crs, dimension, lower, upper));
        }
        dimension += beginIndex;
        coordinates[dimension + (coordinates.length >>> 1)] = upper;
        coordinates[dimension] = lower;
    }

    /**
     * Must be overridden, since the super-class method processes the full array as a whole.
     */
    @Override
    public void setEnvelope(final double... corners) {
        final int dimension = getDimension();
        verifyArrayLength(dimension, corners);
        verifyRanges(crs, corners);
        final int d = coordinates.length >>> 1;
        System.arraycopy(corners, 0,         coordinates, beginIndex,     dimension);
        System.arraycopy(corners, dimension, coordinates, beginIndex + d, dimension);
    }

    /**
     * Must be overridden, since the super-class method processes the full array as a whole.
     */
    @Override
    public boolean isAllNaN() {
        final int d = coordinates.length >>> 1;
        for (int i=beginIndex; i<endIndex; i++) {
            if (!Double.isNaN(coordinates[i]) || !Double.isNaN(coordinates[i+d])) {
                return false;
            }
        }
        assert isEmpty() : this;
        return true;
    }

    /**
     * Must be overridden, since the super-class method processes the full array as a whole.
     */
    @Override
    public void setToNaN() {
        final int d = coordinates.length >>> 1;
        Arrays.fill(coordinates, beginIndex,   endIndex,   Double.NaN);
        Arrays.fill(coordinates, beginIndex+d, endIndex+d, Double.NaN);
        assert isAllNaN() : this;
    }

    /**
     * Must be overridden, since the super-class method processes the full array as a whole.
     */
    @Override
    public int hashCode() {
        return hashCodeByAPI();
    }

    /**
     * Must be overridden, since the super-class method processes the full array as a whole.
     */
    @Override
    public boolean equals(final Object object) {
        return equalsByAPI(object);
    }

    /**
     * Must be overridden, since the super-class method does not handle the index range
     * for performance reasons.
     */
    @Override
    public GeneralEnvelope subEnvelope(final int b, final int e) throws IndexOutOfBoundsException {
        ensureValidIndexRange(endIndex - beginIndex, b, e);
        return new SubEnvelope(coordinates, b + beginIndex, e + beginIndex);
    }

    /**
     * If the user wants a clone, copy only the relevant part of the coordinates array.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public GeneralEnvelope clone() {
        final int d = coordinates.length >>> 1;
        final int dimension = endIndex - beginIndex;
        final GeneralEnvelope copy = new GeneralEnvelope(endIndex - beginIndex);
        System.arraycopy(coordinates, beginIndex,     copy.coordinates, 0,         dimension);
        System.arraycopy(coordinates, beginIndex + d, copy.coordinates, dimension, dimension);
        copy.crs = crs;
        return copy;
    }
}
