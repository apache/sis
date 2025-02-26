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
package org.apache.sis.io.stream;

import java.util.Arrays;
import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.multiplyExact;
import static java.lang.Math.incrementExact;
import static java.lang.Math.toIntExact;
import org.apache.sis.io.TableAppender;
import static org.apache.sis.pending.jdk.JDK18.ceilDiv;


/**
 * A sub-area in a <var>n</var>-dimensional hyper-rectangle, optionally with subsampling.
 * The size of the hyper-rectangle is given by the {@code sourceSize} argument at construction time,
 * where {@code sourceSize.length} is the number of dimensions
 * and {@code sourceSize[i]} is the number of values along dimension <var>i</var>.
 * For each dimension, the index ranges from 0 inclusive to {@code sourceSize[i]} exclusive.
 *
 * <p>This class assumes that the values are stored in a sequence (array or uncompressed file)
 * where index at dimension 0 varies fastest, followed by index at dimension 1, <i>etc</i>.</p>
 *
 * <p>This class has no knowledge of data size. The same {@code Region} instance can be used
 * for reading {@code byte} and {@code float} arrays for instance.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Region {
    /**
     * The size after reading only the sub-region at the given subsampling.
     * The length of this array is the hyper-rectangle dimension.
     *
     * @see #getTargetSize(int)
     * @see #targetLength(int)
     */
    private final int[] targetSize;

    /**
     * Position of the first value to read.
     * This position is zero if the value of all {@code regionLower} elements is zero.
     *
     * @see #getStartByteOffset(long)
     */
    final long startAt;

    /**
     * Number of values to skip after having read values.
     *
     * <ol>
     *   <li>{@code skips[0]} is the number of values to skip after each single value on the same line.</li>
     *   <li>{@code skips[1]} is the number of values to skip after having read the last value in a line.</li>
     *   <li>{@code skips[2]} is the number of values to skip after having read the last value in a plane.</li>
     *   <li>{@code skips[3]} is the number of values to skip after having read the last value in a cube.</li>
     *   <li><i>etc.</i></li>
     * </ol>
     *
     * Values are determined by the {@code subsampling} argument given at construction time.
     * The length of this array is the hyper-rectangle dimension plus one.
     */
    private final long[] skips;

    /**
     * Additional values to add to {@link #skips}, but in bytes instead of as a number of values.
     * This is the only field in this {@link Region} class to be expressed in byte units.
     * This offset is rarely provided.
     *
     * @see #setAdditionalByteOffset(int, long)
     */
    private long[] skipBytes;

    /**
     * Total length of the region.
     * This is the product of all values in the {@code sourceSize} argument given to the constructor.
     */
    public final long length;

    /**
     * Creates a new region. It is caller's responsibility to ensure that:
     * <ul>
     *   <li>all arrays have the same length</li>
     *   <li>{@code sourceSize[i] > 0} for all <var>i</var></li>
     *   <li>{@code regionLower[i] >= 0} for all <var>i</var></li>
     *   <li>{@code regionLower[i] < regionUpper[i] <= sourceSize[i]} for all <var>i</var></li>
     *   <li>{@code subsampling[i] > 0} for all <var>i</var></li>
     *   <li>The total length of data to read does not exceed {@link Integer#MAX_VALUE}.</li>
     * </ul>
     *
     * @param  sourceSize   the number of elements along each dimension.
     * @param  regionLower  indices of the first value to read or write along each dimension.
     * @param  regionUpper  indices after the last value to read or write along each dimension.
     * @param  subsampling  subsampling along each dimension. Shall be greater than zero.
     * @throws ArithmeticException if the size of the region to read exceeds {@link Integer#MAX_VALUE},
     *                             or the total hyper-cube size exceeds {@link Long#MAX_VALUE}.
     */
    public Region(final long[] sourceSize, final long[] regionLower, final long[] regionUpper, final long[] subsampling) {
        final int dimension = sourceSize.length;
        targetSize = new int[dimension];
        skips = new long[dimension + 1];
        long position = 0;
        long stride   = 1;
        long skip     = 0;
        for (int i=0; i<dimension;) {
            final long step  = subsampling[i];
            final long lower = regionLower[i];
            final long count = ceilDiv(subtractExact(regionUpper[i], lower), step);
            final long upper = addExact(lower, incrementExact(multiplyExact(count-1, step)));
            final long span  = sourceSize[i];
            assert (count > 0) && (lower >= 0) && (upper > lower) && (upper <= span) : i;

            targetSize[i] = toIntExact(count);
            position      = addExact(position, multiplyExact(stride, lower));
            skip          = addExact(skip,     multiplyExact(stride, span - (upper - lower)));
            skips[i]      = addExact(skips[i], multiplyExact(stride, step - 1));
            stride        = multiplyExact(stride, span);
            skips[++i]    = skip;
        }
        startAt = position;
        length  = stride;
    }

    /**
     * Sets an additional offset between values at two consecutive indices in the given dimension of the hyper-cube.
     * The strides are computed automatically at construction time, but this method can be invoked in some rare cases
     * where those values need to be modified (example: for adapting to the layout of netCDF "unlimited" variable).
     *
     * <p>This method is the only one in this {@link Region} class to use a count of bytes
     * instead of a count of sample values.</p>
     *
     * <h4>Example</h4>
     * In a cube of dimension 10×10×10, the number of values between indices (0,0,1) and (0,0,2) is 100.
     * If the values type is {@code float}, invoking {@code setAdditionalByteOffset(1, 12)} will increase
     * this value to 103 (computed as 100 + 12/{@value Float#BYTES}).
     * {@link HyperRectangleReader} will still read only the requested 100 values,
     * but will skip 3 more values when moving from plane 1 to plane 2.
     *
     * @param  dimension  dimension for which to increase the stride.
     * @param  skip       additional number of <strong>bytes</strong> to skip after we finished reading
     *                    a block of data in the specified dimension.
     */
    public void setAdditionalByteOffset(final int dimension, final long skip) {
        if (skipBytes == null) {
            skipBytes = new long[getDimension()];
        }
        skipBytes[dimension] = skip;
    }

    /**
     * Returns the number of dimension.
     *
     * @return the hyper-rectangle dimension.
     */
    public final int getDimension() {
        return targetSize.length;
    }

    /**
     * Number of dimensions for which we can collapse the read operations in a single operation because their
     * data are contiguous. This is the index of the first non-zero element in the {@link #skips} array.
     *
     * @return number of dimensions which can be transferred in a single I/O operation.
     */
    final int contiguousDataDimension() {
        final int dimension = skips.length - 1;
        int i;
        for (i=0; i<dimension; i++) {
            if (skips[i] != 0) break;
        }
        if (skipBytes != null) {
            final int n = Math.min(i, skipBytes.length);
            for (int j=0; j<n; j++) {
                if (skipBytes[j] != 0) {
                    return j;
                }
            }
        }
        return i;
    }

    /**
     * Returns the offset in bytes where reading should start.
     * Offset is relative to the first sample value of the hyper-cube.
     *
     * @param  sampleSize   size of sample values, in bytes. The value is usually between {@value Byte#BYTES} and
     *         {@value Long#BYTES}, but the type is nevertheless a {@code long} for implementation convenience.
     * @return offset in bytes relative to the first sample value.
     * @throws ArithmeticException if the offset overflows the 64-bits integer capacity.
     */
    public final long getStartByteOffset(long sampleSize) {
        if (skipBytes != null) {
            // This additional offset is in bytes.
            sampleSize = addExact(sampleSize, skipBytes[0]);
        }
        return Math.multiplyExact(startAt, sampleSize);
    }

    /**
     * Computes stride in number of bytes for the given dimension. Caller must provide a base stride value,
     * which is the stride that we would have in absence of subregion and additional bytes to skip.
     * This method adds to that given base the number of bytes to skip.
     *
     * @param  dimension    the dimension for which to compute the stride.
     * @param  base         stride that we would have in absence of subregion and bytes to skip.
     * @param  sampleSize   size of sample values, in bytes.
     * @return stride in bytes.
     * @throws ArithmeticException if the number of bytes exceed the {@code long} capacity.
     */
    final long stride(final int dimension, final int base, final int sampleSize) {
        long stride = multiplyExact(addExact(skips[dimension], base), sampleSize);
        if (skipBytes != null) {
            // This additional offset is in bytes.
            stride = addExact(stride, skipBytes[dimension]);
        }
        return stride;
    }

    /**
     * {@return the number of values to skip after having read values in the given dimension}.
     */
    final long getSkip(final int dimension) {
        return skips[dimension];
    }

    /**
     * Returns the total number of values to be read from the sub-region while applying the subsampling.
     * This method takes in account only the given number of dimensions.
     *
     * @param  dimension  number of dimensions to use.
     * @return number of values to read. Always greater than zero.
     * @throws ArithmeticException if the size is too large.
     */
    final int targetLength(final int dimension) {
        long size = 1;
        for (int i=0; i<dimension; i++) {
            size *= targetSize[i];
        }
        return toIntExact(size);
    }

    /**
     * Returns the size after reading only the sub-region in the given dimension.
     * This size takes in account the sub-sampling specified at construction time.
     *
     * @param  dimension  the dimension for which to get the target size.
     * @return expected number of elements in the given dimension.
     */
    public final int getTargetSize(final int dimension) {
        return targetSize[dimension];
    }

    /**
     * Compares this region with the given object for equality.
     *
     * @return the object to compare with this region.
     * @return whether this region and the given object are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Region) {
            final var r = (Region) obj;
            return length == r.length && startAt == r.startAt
                    && Arrays.equals(targetSize, r.targetSize)
                    && Arrays.equals(skips,      r.skips)
                    && Arrays.equals(skipBytes,  r.skipBytes);
        }
        return false;
    }

    /**
     * Returns a hash code value for this region.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(targetSize) + 7*Arrays.hashCode(skips) + 37*Arrays.hashCode(skipBytes)
                + Long.hashCode(startAt);   // Rarely different than zero.
    }

    /**
     * Returns a string representation of this region for debugging purpose.
     *
     * @return a string representation of this region.
     */
    @Override
    public String toString() {
        final var table = new TableAppender(" ");
        table.setCellAlignment(TableAppender.ALIGN_RIGHT);
        table.append("size").nextColumn();
        table.append("skip").nextLine();
        for (int i=0; i<targetSize.length; i++) {
            table.append(String.valueOf(targetSize[i])).nextColumn();
            table.append(String.valueOf(skips[i])).nextLine();
        }
        return table.toString();
    }
}
