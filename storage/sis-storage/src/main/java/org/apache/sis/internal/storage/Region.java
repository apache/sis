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
package org.apache.sis.internal.storage;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * A sub-area in a <var>n</var>-dimensional hyper-rectangle, optionally with sub-sampling.
 * The size of the hyper-rectangle is given by the {@code size} argument at construction time,
 * where {@code size.length} is the number of dimensions and {@code size[i]} is the number of
 * values along dimension <var>i</var>. For each dimension, the index ranges from 0 inclusive
 * to {@code size[i]} exclusive.
 *
 * <p>This class assumes that the values are stored in a sequence (array or uncompressed file)
 * where index at dimension 0 varies fastest, followed by index at dimension 1, <i>etc</i>.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class Region {
    /**
     * Total length of the sequence of values, ignoring sub-area and sub-sampling.
     */
    final long sourceLength;

    /**
     * The size after reading only the sub-region at the given sub-sampling.
     * The length of this array is the hyper-rectangle dimension.
     *
     * @see #targetLength(int)
     */
    final int[] targetSize;

    /**
     * Position of the first value to read.
     * This position is zero if the value of all {@code regionLower} elements is zero.
     */
    final long startAt;

    /**
     * Number of values to skip after having read a values.
     *
     * <ol>
     *   <li>{@code skips[0]} is the number of values to skip after each single value on the same line.</li>
     *   <li>{@code skips[1]} is the number of values to skip after having read the last value in a line.</li>
     *   <li>{@code skips[2]} is the number of values to skip after having read the last value in a plane.</li>
     *   <li>{@code skips[3]} is the number of values to skip after having read the last value in a cube.</li>
     *   <li><i>etc.</i></li>
     * </ol>
     *
     * The length of this array is the hyper-rectangle dimension plus one.
     */
    final long[] skips;

    /**
     * Number of dimensions for which we can collapse the read operations in a single operation because their
     * data are contiguous. This is the index of the first non-zero element in the {@link #skips} array.
     */
    final int contiguousDataDimension;

    /**
     * Creates a new region. It is caller's responsibility to ensure that:
     * <ul>
     *   <li>all arrays have the same length</li>
     *   <li>{@code size[i] > 0} for all <var>i</var></li>
     *   <li>{@code regionLower[i] >= 0} for all <var>i</var></li>
     *   <li>{@code regionLower[i] < regionUpper[i] <= size[i]} for all <var>i</var></li>
     *   <li>{@code subsamplings[i] > 0} for all <var>i</var></li>
     * </ul>
     *
     * @param size         The number of elements along each dimension.
     * @param regionLower  Index of the first value to read or write along each dimension.
     * @param regionUpper  Index after the last value to read or write along each dimension.
     * @param subsamplings Sub-sampling along each dimension. Shall be greater than zero.
     */
    public Region(final long[] size, final long[] regionLower, final long[] regionUpper, final int[] subsamplings) {
        final int dimension = size.length;
        targetSize = new int[dimension];
        skips = new long[dimension + 1];
        long position = 0;
        long stride   = 1;
        long skip     = 0;
        for (int i=0; i<dimension;) {
            final int  step  =  subsamplings[i];
            final long lower =  regionLower [i];
            final long count = (regionUpper [i] - lower + (step-1)) / step;      // (upper-lower)/step rounded toward up, provided all values are positive.
            final long upper = lower + ((count-1) * step + 1);
            final long span  = size[i];
            assert (count > 0) && (lower >= 0) && (upper > lower) && (upper <= span) : i;
            targetSize[i] = JDK8.toIntExact(count);

            position += stride * lower;
            skip     += stride * (span - (upper - lower));
            skips[i] += stride * (step - 1);
            stride   *= span;
            skips[++i] = skip;
        }
        startAt = position;
        sourceLength = stride;
        int i;
        for (i=0; i<dimension; i++) {
            if (skips[i] != 0) break;
        }
        contiguousDataDimension = i;
    }

    /**
     * Returns the number of dimension.
     *
     * @return The hyper-rectangle dimension.
     */
    public final int getDimension() {
        return targetSize.length;
    }

    /**
     * Returns the total number of values to be read from the sub-region while applying the sub-sampling.
     * This method takes in account only the given number of dimensions.
     */
    final int targetLength(final int dimension) {
        long length = 1;
        for (int i=0; i<dimension; i++) {
            length *= targetSize[i];
        }
        return JDK8.toIntExact(length);
    }
}
