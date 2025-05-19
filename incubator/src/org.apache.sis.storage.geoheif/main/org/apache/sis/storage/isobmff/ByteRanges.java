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
package org.apache.sis.storage.isobmff;

import java.util.Arrays;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.UnsupportedEncodingException;


/**
 * Ranges of bytes to read from a <abbr>HEIF</abbr> file.
 * Instances are given to {@link Reader#resolve(long, long, ByteRanges)} for
 * converting values relative to a box item to values relative to the stream.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public class ByteRanges implements Comparable<ByteRanges> {
    /**
     * Interface implemented by the {@link TreeNode} subclasses which read bytes only when requested.
     * Contrarily to the constructors of {@link Box} subclasses which read all the payload immediately,
     * the method provided by this interface allow access to a subset of a potentially large sequence of bytes.
     *
     * @author Martin Desruisseaux (Geomatys)
     */
    public interface Reader {
        /**
         * Converts an offset relative to the data of this item to offsets relative to the origin of the input stream.
         * The subset is specified by the {@code offset} and {@code length} arguments, where an {@code offset} of zero
         * is for the first byte stored by this item. Implementation will typically add to {@code offset} the position
         * in the file of the first byte.
         *
         * @param  offset  offset of the first byte to read relative to the data stored is this item.
         * @param  length  maximum number of bytes to read, starting at the offset, or a negative value for reading all.
         * @param  addTo   where to add the ranges of bytes to read as offsets relatives to the beginning of the file.
         * @throws UnsupportedEncodingException if this item uses an unsupported construction method.
         * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
         * @throws DataStoreException if another logical error occurred.
         * @throws ArithmeticException if an integer overflow occurred.
         *
         * @see #addRange(long, long)
         */
        void resolve(long offset, long length, ByteRanges addTo) throws DataStoreException;
    }

    /**
     * Ranges of bytes to read, relative to the file and in the order they were added.
     * Consecutive ranges are merged in a single range, but no other simplification is performed.
     * Offsets at even indexes are starting point (inclusive) and offsets at odd indexes are end point (exclusive).
     */
    private long[] offsets;

    /**
     * Creates an initially empty set of byte ranges.
     */
    public ByteRanges() {
    }

    /**
     * Adds a range of byte offsets relative to the beginning of the <abbr>HEIF</abbr> file.
     * this method is invoked by {@link Reader#resolve(long, long, ByteRanges)} implementations
     * for specifying all range of bytes to read. This method is usually invoked exactly once.
     * It may be invoked more often if the bytes are spread over many regions of the file.
     *
     * @param start  offset of the first byte to read, inclusive.
     * @param end    offset after the last byte to read.
     */
    public void addRange(final long start, final long end) {
        if (start < end) {
            if (offsets == null) {
                offsets = new long[] {start, end};
            } else {
                int i = offsets.length - 1;
                if (offsets[i] == start) {
                    offsets[i] = end;
                } else {
                    offsets = Arrays.copyOf(offsets, ++i + 2);
                    offsets[i] = start;
                    offsets[i+1] = end;
                }
            }
        }
    }

    /**
     * Returns the offset of the first byte to read, or 0 if unknown.
     * Note that this is not necessarily the smallest offset, because nothing prevent the
     * ranges to be declared out-of-order. However, out-of-order ranges are assumed rare.
     */
    public final long offset() {
        return (offsets != null) ? offsets[0] : 0;      // A non-null array should be non-empty.
    }

    /**
     * Compares this set of ranges with the given ranges for the order in which they appear in the file.
     * In the current implementation, the ordering is based on the offset of the first byte to read.
     * It may be revised in a future version if useful.
     *
     * @param  other  the other range of bytes to compare with this instance.
     * @return negative is this range of bytes is before {@code other}, positive if after, 0 if same offset.
     */
    @Override
    public final int compareTo(final ByteRanges other) {
        return Long.compare(offset(), other.offset());
    }

    /**
     * Notifies the given input about the range of bytes which will be requested. This method should be
     * invoked for the {@code ByteRanges} associated to all tiles to read before the actual reading starts.
     * This notification is only a hint. It can be used for preparing a <abbr>HTTP</abbr> range request.
     *
     * @param  input  the input stream to notify about the ranges of bytes that will be requested.
     */
    public final void notify(final ChannelDataInput input) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final long[] offsets = this.offsets;
        if (offsets != null) {
            for (int i=0; i < offsets.length;) {
                // Note: FileCacheByteChannel uses a RangeSet for merging efficiently those ranges.
                input.rangeOfInterest(offsets[i++], offsets[i++]);
            }
        }
    }

    /**
     * Returns a stream of bytes for the given request as if all bytes were stored in one sequence.
     * If all data are stored in the same extent, then this method returns {@code input} directly.
     * Otherwise, this method returns a view over the given input as if all extents were consecutive.
     *
     * @todo The view is not yet implemented.
     *
     * @param  input    the channel opened on the <abbr>HEIF</abbr> file.
     * @param  request  the region to read, as adjusted by {@link #resolve(long, long, ByteRanges)}.
     * @return the input to use. By default, this is {@code input} directly.
     * @throws DataStoreException if the input cannot be created.
     */
    public final ChannelDataInput viewAsConsecutiveBytes(ChannelDataInput input) throws DataStoreException {
        if (offsets != null && offsets.length == 2) {
            return input;
        }
        /*
         * There is at least two extents to read. Create a channel
         * which will read each extent as if they were consecutive.
         * TODO: not yet implemented.
         */
        throw new UnsupportedEncodingException("Not supported yet");
    }
}
