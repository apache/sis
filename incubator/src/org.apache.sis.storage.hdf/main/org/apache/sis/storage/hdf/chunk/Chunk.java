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
package org.apache.sis.storage.hdf.chunk;

import java.io.IOException;
import java.util.Arrays;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * A block of raw data, whichever way the layout stores it.
 * <p>
 * Compact and contiguous layouts produce a single chunk covering the whole
 * dataset; chunked layouts produce one per chunk of the chunk grid.
 *
 * @author Johann Sorel (Geomatys)
 */
public class Chunk {

    /**
     * Address of the raw data in the file.
     */
    public long address;
    /**
     * Size of the chunk in the file, in bytes, filters applied.
     */
    public long size;
    /**
     * A 32 bits field indicating which filters have been skipped for this chunk.
     * Each filter has an index in the pipeline, starting at 0 with the first
     * filter to apply; if that filter is skipped, the bit of its index is set.
     */
    public long filterMask;
    /**
     * Extent covered by this chunk, in dataset element coordinates.
     */
    public GridExtent offset;
    /**
     * Size of the chunk once filters are reversed, or -1 when unknown.
     */
    public long uncompressedSize = -1;
    /**
     * Whether the filter pipeline applies to this chunk. Compact and contiguous
     * raw data is never filtered.
     */
    public boolean filtered;

    /** Creates an empty chunk, filled by the chunk index reading it. */
    public Chunk() {
    }

    /**
     * Returns whether the given filter was skipped for this chunk and must not be reversed.
     * Tests the filter bit in the {@linkplain #filterMask filter mask}.
     *
     * @param filterIndex index of a filter in the pipeline
     * @return whether the given filter was skipped for this chunk and must not be reversed
     */
    public boolean isFilterSkipped(int filterIndex) {
        return (filterMask & (1L << filterIndex)) != 0;
    }

    /**
     * Read one element of a fixed or extensible array index.
     * <p>
     * Both indexes encode their elements the same way : an address alone when the
     * dataset has no filter pipeline, an address followed by the stored chunk size
     * and the filter mask when it has one.
     *
     * @param channel channel positioned on the element
     * @param clientId 0 for unfiltered chunks, 1 for filtered chunks
     * @param entrySize size of one element in bytes, from the array header
     * @param chunkByteSize size of a decoded chunk, used when the chunk is not filtered
     * @return the chunk, or null when this element has not been allocated
     * @throws IOException if the element size is inconsistent or if an error
     *         occurred while reading the file.
     */
    public static Chunk readIndexElement(HDF5DataInput channel, int clientId, int entrySize, long chunkByteSize) throws IOException {
        final long address = channel.readOffset();
        final boolean filtered = (clientId == 1);
        if (channel.isUndefinedOffset(address)) {
            //consume the remainder of the element so the caller stays aligned
            if (filtered) {
                channel.skipFully(entrySize - channel.getOffsetSize());
            }
            return null;
        }
        final Chunk chunk = new Chunk();
        chunk.address = address;
        chunk.filtered = filtered;
        if (filtered) {
            /*
            The width of the chunk size field is whatever is left in the element
            once the address and the 4 bytes filter mask are accounted for.
            */
            final int sizeWidth = entrySize - channel.getOffsetSize() - 4;
            if (sizeWidth < 1 || sizeWidth > 8) {
                throw new IOException("Inconsistent array index element size " + entrySize
                        + " for an offset size of " + channel.getOffsetSize());
            }
            chunk.size = channel.readUnsignedInteger(sizeWidth);
            chunk.filterMask = channel.readUnsignedInt();
        } else {
            chunk.size = chunkByteSize;
            chunk.uncompressedSize = chunkByteSize;
        }
        return chunk;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Chunk{");
        sb.append("size:").append(size);
        sb.append(",filterMask:").append(filterMask);
        sb.append(",address:").append(address);
        if (offset != null) {
            sb.append(",offset:").append(Arrays.toString(offset.getLow().getCoordinateValues()));
        }
        sb.append("}");
        return sb.toString();
    }
}
