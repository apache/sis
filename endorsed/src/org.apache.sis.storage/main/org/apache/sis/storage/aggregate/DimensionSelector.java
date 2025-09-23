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
import org.apache.sis.util.internal.shared.Strings;


/**
 * A helper class for choosing the dimension on which to perform aggregation.
 * An instance is created for each dimension of the grid geometry of a group.
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
     * The largest extent size found among all slices.
     * Together with {@link #sumOfSize}, it provides a way to check is the size is constant.
     */
    private long maxSize;

    /**
     * Sum of grid extent size of each slice.
     * This is updated for each new slice added to this selector.
     */
    private BigInteger sumOfSize;

    /**
     * {@code true} if the increment between each slice is constant and equals to the extent size.
     * In such case, the slices are actually tiles of constant size in a regular tile matrix.
     * This is used for setting the value of {@link GroupByTransform#isMosaic}.
     *
     * <h4>Validity</h4>
     * This value is valid only after {@link #finish()} has been invoked, which is itself invoked
     * only after the {@link #positions} array has been completed with data from all slices.
     */
    boolean isMosaic;

    /**
     * {@code true} if all {@link #positions} values are the same.
     * For example, for a list of slices in the same geographic area but at different days <var>t</var>,
     * this flag will typically be {@code true} for the horizontal dimensions and {@code false} for the
     * temporal dimension.
     *
     * <h4>Validity</h4>
     * This value is valid only after {@link #finish()} has been invoked, which is itself invoked
     * only after the {@link #positions} array has been completed with data from all slices.
     */
    boolean isConstantPosition;

    /**
     * Average position increment in unit of the extent size.
     * Small values mean that the position barely changes compared to the slice size.
     * This is used for {@linkplain #compareTo choosing a preferred aggregation axis}.
     *
     * <h4>Validity</h4>
     * This value is valid only after {@link #finish()} has been invoked, which is itself invoked
     * only after the {@link #positions} array has been completed with data from all slices.
     */
    private double relativeIncrement;

    /**
     * Difference between minimal and maximal increment.
     * Small values suggest that the increment is more stable compared to large values.
     * This is used for {@linkplain #compareTo choosing a preferred aggregation axis}.
     *
     * <h4>Validity</h4>
     * This value is valid only after {@link #finish()} has been invoked, which is itself invoked
     * only after the {@link #positions} array has been completed with data from all slices.
     */
    private long incrementRange;

    /**
     * Prepares a new selector for a single dimension.
     *
     * @param  dimension   the dimension examined by this selector.
     * @param  sliceCount  number of slices.
     */
    DimensionSelector(final int dimension, final int sliceCount) {
        this.dimension = dimension;
        this.positions = new long[sliceCount];
        this.sumOfSize = BigInteger.ZERO;
    }

    /**
     * Sets the position and size of a single slice. The given position can be the low, mid or high grid coordinate,
     * or anything else, as long as the choice is kept consistent across calls to this method on the same instance.
     * The positions can be in any order, not necessarily increasing with the slice index.
     *
     * @param sliceIndex  index of the slice.
     * @param position    position of the slice from an arbitrary measurement process.
     * @param extentSize  size of the extent, in number of cells.
     */
    final void setSliceExtent(final int sliceIndex, final long position, final long extentSize) {
        positions[sliceIndex] = position;
        maxSize = Math.max(maxSize, extentSize);
        sumOfSize = sumOfSize.add(BigInteger.valueOf(extentSize));
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
        isMosaic = isConstantPosition = (maxInc == 0);
        if (minInc <= maxInc) {
            relativeIncrement = sumOfInc.doubleValue() / sumOfSize.doubleValue();
            incrementRange = maxInc - minInc;   // Cannot overflow because minInc >= 0.
            isMosaic = (incrementRange == 0) && (isConstantPosition || maxInc == maxSize);
        }
        if (isMosaic) {
            // Verify that all tiles have the same size.
            isMosaic = sumOfSize.equals(BigInteger.valueOf(maxSize).multiply(BigInteger.valueOf(positions.length)));
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
        return Strings.toString(getClass(),
                "dimension",          dimension,
                "isMosaic",           isMosaic,
                "isConstantPosition", isConstantPosition,
                "relativeIncrement",  relativeIncrement,
                "incrementRange",     incrementRange);
    }
}
