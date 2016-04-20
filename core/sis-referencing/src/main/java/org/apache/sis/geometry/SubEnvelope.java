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
 * @since   0.3
 * @version 0.3
 * @module
 */
final class SubEnvelope extends GeneralEnvelope {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7241242611077979466L;

    /**
     * The index of the first valid ordinate value of the lower corner in the {@link #ordinates} array.
     *
     * @see ArrayEnvelope#beginIndex()
     */
    private final int beginIndex;

    /**
     * The index after the last valid ordinate value of the lower corner  in the {@link #ordinates} array.
     *
     * @see ArrayEnvelope#endIndex()
     */
    private final int endIndex;

    /**
     * Creates a new envelope over a portion of the given array. This constructor stores the given
     * reference directly; it does <strong>not</strong> clone the given array. This is the desired
     * behavior for allowing the {@code SubEnvelope} view to be "live".
     *
     * @param ordinates  The array of ordinate values to store directly (not cloned).
     * @param beginIndex The index of the first valid ordinate value of the lower corner in the ordinates array.
     * @param endIndex   The index after the last valid ordinate value of the lower corner in the ordinates array.
     */
    SubEnvelope(final double[] ordinates, final int beginIndex, final int endIndex) {
        super(ordinates);
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    /**
     * Returns the index of the first valid ordinate value of the lower corner in the ordinates array.
     * This information is used by super-class methods.
     */
    @Override
    final int beginIndex() {
        return beginIndex;
    }

    /**
     * Returns the index after the last valid ordinate value of the lower corner in the ordinates array.
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
        return ordinates[dimension + beginIndex];
    }

    /**
     * Must be overridden, since the super-class method does not handle the index range
     * for performance reasons.
     */
    @Override
    public double getUpper(final int dimension) throws IndexOutOfBoundsException {
        ensureValidIndex(endIndex, dimension);
        return ordinates[dimension + beginIndex + (ordinates.length >>> 1)];
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
         * identical to ArrayEnvelope.verifyRanges(crs, ordinates) except that there is no loop.
         */
        if (lower > upper && crs != null && !isWrapAround(crs, dimension)) {
            throw new IllegalArgumentException(illegalRange(crs, dimension, lower, upper));
        }
        dimension += beginIndex;
        ordinates[dimension + (ordinates.length >>> 1)] = upper;
        ordinates[dimension] = lower;
    }

    /**
     * Must be overridden, since the super-class method processes the full array as a whole.
     */
    @Override
    public void setEnvelope(final double... corners) {
        final int dimension = getDimension();
        verifyArrayLength(dimension, corners);
        verifyRanges(crs, corners);
        final int d = ordinates.length >>> 1;
        System.arraycopy(corners, 0,         ordinates, beginIndex,     dimension);
        System.arraycopy(corners, dimension, ordinates, beginIndex + d, dimension);
    }

    /**
     * Must be overridden, since the super-class method processes the full array as a whole.
     */
    @Override
    public boolean isAllNaN() {
        final int d = ordinates.length >>> 1;
        for (int i=beginIndex; i<endIndex; i++) {
            if (!Double.isNaN(ordinates[i]) || !Double.isNaN(ordinates[i+d])) {
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
        final int d = ordinates.length >>> 1;
        Arrays.fill(ordinates, beginIndex,   endIndex,   Double.NaN);
        Arrays.fill(ordinates, beginIndex+d, endIndex+d, Double.NaN);
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
        return new SubEnvelope(ordinates, b + beginIndex, e + beginIndex);
    }

    /**
     * If the user wants a clone, copy only the relevant part of the ordinates array.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public GeneralEnvelope clone() {
        final int d = ordinates.length >>> 1;
        final int dimension = endIndex - beginIndex;
        final GeneralEnvelope copy = new GeneralEnvelope(endIndex - beginIndex);
        System.arraycopy(ordinates, beginIndex,     copy.ordinates, 0,         dimension);
        System.arraycopy(ordinates, beginIndex + d, copy.ordinates, dimension, dimension);
        copy.crs = crs;
        return copy;
    }
}
