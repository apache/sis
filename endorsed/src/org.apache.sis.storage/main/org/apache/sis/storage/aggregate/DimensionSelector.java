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
package org.apache.sis.storage.aggregate;

import java.util.Arrays;
import java.math.BigInteger;
import org.apache.sis.util.privy.Strings;


/**
 * A helper class for choosing the dimension on which to perform aggregation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DimensionSelector implements Comparable<DimensionSelector> {
    /**
     * The dimension examined by this selector.
     */
    final int dimension;

    /**
     * Grid coordinate in a single dimension of the grid extent, one value for each slice.
     * It may be the grid low, mid or high value, it does not matter for this class as long
     * as they are consistent.
     */
    private final long[] positions;

    /**
     * Sum of grid extent size of each slice.
     * This is updated for each new slice added to this selector.
     */
    private BigInteger sumOfSize;

    /**
     * Increment in unit of the extent size. This calculation is based on mean values only.
     * It is computed after the {@link #positions} array has been completed with data from all slices.
     */
    private double relativeIncrement;

    /**
     * Difference between minimal and maximal increment.
     * This is computed after the {@link #positions} array has been completed with data from all slices.
     */
    private long incrementRange;

    /**
     * {@code true} if all {@link #positions} values are the same.
     * This field is valid only after {@link #finish()} call.
     */
    boolean isConstantPosition;

    /**
     * Prepares a new selector for a single dimension.
     *
     * @param  dim  the dimension examined by this selector.
     * @param  n    number of slices.
     */
    DimensionSelector(final int dim, final int n) {
        dimension = dim;
        positions = new long[n];
        sumOfSize = BigInteger.ZERO;
    }

    /**
     * Sets the extent of a single slice.
     *
     * @param i     index of the slice.
     * @param pos   position of the slice. Could be low, mid or high index, as long as the choice is kept consistent.
     * @param size  size of the extent, in number of cells.
     */
    final void setSliceExtent(final int i, final long pos, final long size) {
        positions[i] = pos;
        sumOfSize = sumOfSize.add(BigInteger.valueOf(size));
    }

    /**
     * Computes the {@linkplain #increment} between slices after all positions have been specified.
     * This method is invoked in parallel (on different instances) for each dimension.
     */
    final void finish() {
        Arrays.sort(positions);     // Not `parallelSort(…)` because this method is already invoked in parallel.
        long maxInc = 0;
        long minInc = Long.MAX_VALUE;
        BigInteger sumOfInc = BigInteger.ZERO;
        long previous = positions[0];
        for (int i=1; i<positions.length; i++) {
            final long p = positions[i];
            final long d = p - previous;
            if (d != 0) {
                if (d < minInc) minInc = d;
                if (d > maxInc) maxInc = d;
                sumOfInc = sumOfInc.add(BigInteger.valueOf(d));
                previous = p;
            }
        }
        isConstantPosition = (maxInc == 0);
        if (minInc <= maxInc) {
            relativeIncrement = sumOfInc.doubleValue() / sumOfSize.doubleValue();
            incrementRange = maxInc - minInc;   // Cannot overflow because minInc >= 0.
            /*
             * TODO: we may have a mosaic if `incrementRange == 0 && maxInc == size`.
             *       Or maybe we could accept `maxInc <= minSize`.
             */
        }
    }

    /**
     * Compares for order of "probability" that a dimension is the one to aggregate.
     * After using this comparator, dimensions that are more likely to be the ones to
     * aggregate are sorted last. Because the order is defined that way, sorting the
     * {@code DimensionSelector} array will have no effect in the most typical cases
     * where the dimensions to aggregate are the last ones.
     */
    @Override
    public int compareTo(final DimensionSelector other) {
        int c = Boolean.compare(other.isConstantPosition, isConstantPosition);      // Non-constant sorted last.
        if (c == 0) {
            c = Double.compare(relativeIncrement, other.relativeIncrement);         // Largest values sorted last.
            if (c == 0) {
                c = Long.compare(other.incrementRange, incrementRange);             // Smallest values sorted last.
            }
        }
        return c;
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "dimension", dimension, "relativeIncrement", relativeIncrement);
    }
}
