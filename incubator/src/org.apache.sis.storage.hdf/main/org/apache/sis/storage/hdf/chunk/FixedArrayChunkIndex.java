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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Fixed array chunk index, used by datasets whose dimensions are all fixed : the
 * number of chunks is known once and for all, so their addresses are held in a
 * flat array addressed by the linear chunk index.
 * <p>
 * The array is made of a header and a single data block. Large data blocks are
 * split in pages, and a bitmap in the data block tells which pages exist; absent
 * pages occupy no space at all in the file.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_appendixc_fixedarr">VII.C. The Fixed Array Index</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FixedArrayChunkIndex implements ChunkIndex {

    /**
     * The ASCII character string “FAHD” indicates the header of a fixed array.
     */
    public static final byte[] HEADER_SIGNATURE = "FAHD".getBytes(StandardCharsets.US_ASCII);
    /**
     * The ASCII character string “FADB” indicates a fixed array data block.
     */
    public static final byte[] DATA_BLOCK_SIGNATURE = "FADB".getBytes(StandardCharsets.US_ASCII);

    private final long headerAddress;
    private final ChunkGrid grid;
    private final long chunkByteSize;

    //header fields
    private int clientId;
    private int entrySize;
    private int pageBits;
    private long maxNumberOfEntries;
    private long dataBlockAddress;

    private Chunk[] elements;

    /**
     * Creates an index over a fixed array, read lazily.
     *
     * @param headerAddress file address of the fixed array header
     * @param grid chunk grid of the dataset
     * @param chunkByteSize size in bytes of a full chunk, used as the stored size of unfiltered chunks
     */
    public FixedArrayChunkIndex(long headerAddress, ChunkGrid grid, long chunkByteSize) {
        this.headerAddress = headerAddress;
        this.grid = grid;
        this.chunkByteSize = chunkByteSize;
    }

    private synchronized Chunk[] load(HDF5DataInput channel) throws IOException {
        if (elements == null) {
            channel.mark();
            try {
                readHeader(channel);
                elements = new Chunk[Math.toIntExact(maxNumberOfEntries)];
                if (channel.isDefinedOffset(dataBlockAddress)) {
                    channel.seek(dataBlockAddress);
                    readDataBlock(channel);
                }
            } finally {
                channel.reset();
            }
        }
        return elements;
    }

    private void readHeader(HDF5DataInput channel) throws IOException {
        channel.seek(headerAddress);
        channel.ensureSignature(HEADER_SIGNATURE);
        channel.ensureVersion(0);
        /*
        The client id is the kind of data held by the array : 0 for the addresses
        of unfiltered chunks, 1 for the addresses, sizes and filter masks of
        filtered chunks.
        */
        clientId = channel.readUnsignedByte();
        entrySize = channel.readUnsignedByte();
        pageBits = channel.readUnsignedByte();
        maxNumberOfEntries = channel.readLength();
        dataBlockAddress = channel.readOffset();
        final int checksum = channel.readInt();
        if (clientId != 0 && clientId != 1) {
            throw new IOException("Unexpected fixed array client id " + clientId);
        }
    }

    private void readDataBlock(HDF5DataInput channel) throws IOException {
        channel.ensureSignature(DATA_BLOCK_SIGNATURE);
        channel.ensureVersion(0);
        final int blockClientId = channel.readUnsignedByte();
        if (blockClientId != clientId) {
            throw new IOException("Fixed array data block client id " + blockClientId
                    + " does not match header client id " + clientId);
        }
        final long ownerAddress = channel.readOffset();
        if (ownerAddress != headerAddress) {
            throw new IOException("Fixed array data block points at header " + ownerAddress
                    + " instead of " + headerAddress);
        }

        final long pageSize = 1L << pageBits;
        final long pageCount = (maxNumberOfEntries + pageSize - 1) / pageSize;

        if (pageCount <= 1) {
            //a single page, the elements follow directly and there is no bitmap
            readElements(channel, 0, Math.toIntExact(maxNumberOfEntries));
            final int checksum = channel.readInt();
            return;
        }

        /*
        Paged block : a bitmap tells which pages were written. A page whose bit is
        clear takes no room in the file, so pages must be walked in order rather
        than addressed by a fixed stride.
        */
        final byte[] pageBitmap = channel.readNBytes(Math.toIntExact((pageCount + 7) / 8));
        final int checksum = channel.readInt();
        for (long page = 0; page < pageCount; page++) {
            if (!ChunkIndex.isBitSet(pageBitmap, page)) continue;
            final long first = page * pageSize;
            final int count = Math.toIntExact(Math.min(pageSize, maxNumberOfEntries - first));
            readElements(channel, first, count);
            final int pageChecksum = channel.readInt();
        }
    }

    private void readElements(HDF5DataInput channel, long firstIndex, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            final Chunk chunk = Chunk.readIndexElement(channel, clientId, entrySize, chunkByteSize);
            final int index = Math.toIntExact(firstIndex + i);
            if (chunk != null) {
                chunk.offset = grid.extentOf(grid.scaledOf(index));
                elements[index] = chunk;
            }
        }
    }

    @Override
    public Chunk getChunk(HDF5DataInput channel, long[] scaled) throws IOException {
        final Chunk[] all = load(channel);
        final long index = grid.linearIndex(scaled);
        if (index < 0 || index >= all.length) return null;
        return all[(int) index];
    }

    @Override
    public List<Chunk> getChunks(HDF5DataInput channel) throws IOException {
        final List<Chunk> chunks = new ArrayList<>();
        for (Chunk chunk : load(channel)) {
            if (chunk != null) chunks.add(chunk);
        }
        return chunks;
    }
}
