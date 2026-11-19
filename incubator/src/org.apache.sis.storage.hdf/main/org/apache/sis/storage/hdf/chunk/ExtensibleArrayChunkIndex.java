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
 * Extensible array chunk index, used by datasets having exactly one unlimited
 * dimension.
 * <p>
 * The array is made of a header, an index block, and a growing set of super
 * blocks and data blocks. Almost none of the block sizes are stored in the file :
 * they are derived from the creation parameters held by the data layout message,
 * following the same computation as {@code H5EA__hdr_init}.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_appendixc_extarr">VII.D. The Extensible Array Index</a>
 * @author Johann Sorel (Geomatys)
 */
public final class ExtensibleArrayChunkIndex implements ChunkIndex {

    /** The ASCII character string “EAHD” indicates the header of an extensible array. */
    public static final byte[] HEADER_SIGNATURE = "EAHD".getBytes(StandardCharsets.US_ASCII);
    /** The ASCII character string “EAIB” indicates an extensible array index block. */
    public static final byte[] INDEX_BLOCK_SIGNATURE = "EAIB".getBytes(StandardCharsets.US_ASCII);
    /** The ASCII character string “EASB” indicates an extensible array super block. */
    public static final byte[] SUPER_BLOCK_SIGNATURE = "EASB".getBytes(StandardCharsets.US_ASCII);
    /** The ASCII character string “EADB” indicates an extensible array data block. */
    public static final byte[] DATA_BLOCK_SIGNATURE = "EADB".getBytes(StandardCharsets.US_ASCII);

    /**
     * Geometry of one super block, all of it derived rather than stored.
     *
     * @param dataBlockCount number of data blocks of this super block
     * @param elementsPerDataBlock number of elements held by each of them
     * @param startIndex index of the first element covered by this super block,
     *        counted after the elements held by the index block
     * @param startDataBlock index of its first data block among all data blocks
     */
    private record SuperBlockInfo(long dataBlockCount, long elementsPerDataBlock,
            long startIndex, long startDataBlock) {}

    private final long headerAddress;
    private final ChunkGrid grid;
    private final long chunkByteSize;

    //header fields
    private int clientId;
    private int elementSize;
    private int maxNumberOfElementsBits;
    private int indexBlockElements;
    private int dataBlockMinimumElements;
    private int superBlockMinimumDataPointers;
    private int maxDataBlockPageElementsBits;
    private long maxIndexSet;
    private long indexBlockAddress;

    //derived
    private int elementOffsetSize;
    private int superBlockCount;
    private int firstSuperBlockIndex;
    private int indexBlockDataAddressCount;
    private int indexBlockSuperAddressCount;
    private long dataBlockPageElements;
    private SuperBlockInfo[] superBlocks;

    //index block content
    private Chunk[] indexBlockChunks;
    private long[] dataBlockAddresses;
    private long[] superBlockAddresses;

    private boolean loaded;

    /**
     * Creates an index over an extensible array, read lazily.
     *
     * @param headerAddress file address of the extensible array header
     * @param grid chunk grid of the dataset
     * @param chunkByteSize size in bytes of a full chunk, used as the stored size of unfiltered chunks
     */
    public ExtensibleArrayChunkIndex(long headerAddress, ChunkGrid grid, long chunkByteSize) {
        this.headerAddress = headerAddress;
        this.grid = grid;
        this.chunkByteSize = chunkByteSize;
    }

    private static int log2Exact(int value) {
        return Integer.numberOfTrailingZeros(value);
    }

    private static int log2Floor(long value) {
        return value <= 1 ? 0 : 63 - Long.numberOfLeadingZeros(value);
    }

    private synchronized void load(HDF5DataInput channel) throws IOException {
        if (loaded) return;
        channel.mark();
        try {
            readHeader(channel);
            derive();
            readIndexBlock(channel);
            loaded = true;
        } finally {
            channel.reset();
        }
    }

    private void readHeader(HDF5DataInput channel) throws IOException {
        channel.seek(headerAddress);
        channel.ensureSignature(HEADER_SIGNATURE);
        channel.ensureVersion(0);
        clientId = channel.readUnsignedByte();
        elementSize = channel.readUnsignedByte();
        maxNumberOfElementsBits = channel.readUnsignedByte();
        indexBlockElements = channel.readUnsignedByte();
        dataBlockMinimumElements = channel.readUnsignedByte();
        superBlockMinimumDataPointers = channel.readUnsignedByte();
        maxDataBlockPageElementsBits = channel.readUnsignedByte();
        final long numberOfSuperBlocks = channel.readLength();
        final long sizeOfSuperBlocks = channel.readLength();
        final long numberOfDataBlocks = channel.readLength();
        final long sizeOfDataBlocks = channel.readLength();
        maxIndexSet = channel.readLength();
        final long numberOfElements = channel.readLength();
        indexBlockAddress = channel.readOffset();
        final int checksum = channel.readInt();
        if (clientId != 0 && clientId != 1) {
            throw new IOException("Unexpected extensible array client id " + clientId);
        }
    }

    /**
     * Compute the block geometry from the creation parameters, as {@code H5EA__hdr_init} does.
     */
    private void derive() {
        elementOffsetSize = (maxNumberOfElementsBits + 7) / 8;
        superBlockCount = 1 + (maxNumberOfElementsBits - log2Exact(dataBlockMinimumElements));
        firstSuperBlockIndex = 2 * log2Exact(superBlockMinimumDataPointers);
        indexBlockDataAddressCount = 2 * (superBlockMinimumDataPointers - 1);
        indexBlockSuperAddressCount = superBlockCount - firstSuperBlockIndex;
        dataBlockPageElements = 1L << maxDataBlockPageElementsBits;

        superBlocks = new SuperBlockInfo[superBlockCount];
        long startIndex = 0;
        long startDataBlock = 0;
        for (int u = 0; u < superBlockCount; u++) {
            final long dataBlockCount = 1L << (u / 2);
            final long elementsPerDataBlock = (1L << ((u + 1) / 2)) * dataBlockMinimumElements;
            superBlocks[u] = new SuperBlockInfo(dataBlockCount, elementsPerDataBlock, startIndex, startDataBlock);
            startIndex += dataBlockCount * elementsPerDataBlock;
            startDataBlock += dataBlockCount;
        }
    }

    private void readIndexBlock(HDF5DataInput channel) throws IOException {
        indexBlockChunks = new Chunk[indexBlockElements];
        dataBlockAddresses = new long[indexBlockDataAddressCount];
        superBlockAddresses = new long[indexBlockSuperAddressCount];
        if (channel.isUndefinedOffset(indexBlockAddress)) {
            return;
        }
        channel.seek(indexBlockAddress);
        channel.ensureSignature(INDEX_BLOCK_SIGNATURE);
        channel.ensureVersion(0);
        final int blockClientId = channel.readUnsignedByte();
        if (blockClientId != clientId) {
            throw new IOException("Extensible array index block client id " + blockClientId
                    + " does not match header client id " + clientId);
        }
        channel.readOffset(); //header address
        for (int i = 0; i < indexBlockElements; i++) {
            indexBlockChunks[i] = readElement(channel, i);
        }
        for (int i = 0; i < indexBlockDataAddressCount; i++) {
            dataBlockAddresses[i] = channel.readOffset();
        }
        for (int i = 0; i < indexBlockSuperAddressCount; i++) {
            superBlockAddresses[i] = channel.readOffset();
        }
        final int checksum = channel.readInt();
    }

    private Chunk readElement(HDF5DataInput channel, long elementIndex) throws IOException {
        final Chunk chunk = Chunk.readIndexElement(channel, clientId, elementSize, chunkByteSize);
        if (chunk != null) {
            chunk.offset = grid.extentOf(grid.scaledOf(elementIndex));
        }
        return chunk;
    }

    /**
     * Resolve one element of the array, following {@code H5EA__lookup_elmt}.
     *
     * @param channel file channel
     * @param index element index
     * @return the chunk, or {@code null} if not allocated
     * @throws IOException if an I/O error occurred.
     */
    private Chunk lookup(HDF5DataInput channel, long index) throws IOException {
        if (index < indexBlockElements) {
            return indexBlockChunks[(int) index];
        }
        /*
        The super block holding this element is found from the index taken
        relative to the elements stored in the index block.
        */
        final long relative = index - indexBlockElements;
        final int superIndex = log2Floor((relative / dataBlockMinimumElements) + 1);
        if (superIndex >= superBlockCount) return null;
        final SuperBlockInfo info = superBlocks[superIndex];
        final long inSuperBlock = index - (indexBlockElements + info.startIndex());

        final long dataBlockAddress;
        if (superIndex < firstSuperBlockIndex) {
            /*
            The data blocks of the first super blocks are addressed directly from
            the index block.
            */
            final long dataBlockIndex = info.startDataBlock() + inSuperBlock / info.elementsPerDataBlock();
            if (dataBlockIndex >= dataBlockAddresses.length) return null;
            dataBlockAddress = dataBlockAddresses[(int) dataBlockIndex];
        } else {
            final int superAddressIndex = superIndex - firstSuperBlockIndex;
            if (superAddressIndex >= superBlockAddresses.length) return null;
            final long superBlockAddress = superBlockAddresses[superAddressIndex];
            if (channel.isUndefinedOffset(superBlockAddress)) return null;
            dataBlockAddress = readSuperBlockDataAddress(channel, superBlockAddress, info,
                    inSuperBlock / info.elementsPerDataBlock());
        }
        if (channel.isUndefinedOffset(dataBlockAddress)) return null;
        return readDataBlockElement(channel, dataBlockAddress, info.elementsPerDataBlock(),
                inSuperBlock % info.elementsPerDataBlock(), index);
    }

    private long readSuperBlockDataAddress(HDF5DataInput channel, long address,
            SuperBlockInfo info, long dataBlockIndex) throws IOException {
        channel.seek(address);
        channel.ensureSignature(SUPER_BLOCK_SIGNATURE);
        channel.ensureVersion(0);
        channel.readUnsignedByte(); //client id
        channel.readOffset();       //header address
        channel.skipFully(elementOffsetSize); //block offset
        if (info.elementsPerDataBlock() > dataBlockPageElements) {
            /*
            When the data blocks of this super block are paged, their page bitmaps
            are stored here rather than in the data blocks themselves.
            */
            final long pages = (info.elementsPerDataBlock() + dataBlockPageElements - 1) / dataBlockPageElements;
            channel.skipFully(info.dataBlockCount() * ((pages + 7) / 8));
        }
        channel.skipFully(dataBlockIndex * channel.getOffsetSize());
        return channel.readOffset();
    }

    private Chunk readDataBlockElement(HDF5DataInput channel, long address, long elementCount,
            long within, long elementIndex) throws IOException {
        channel.seek(address);
        channel.ensureSignature(DATA_BLOCK_SIGNATURE);
        channel.ensureVersion(0);
        channel.readUnsignedByte(); //client id
        channel.readOffset();       //header address
        channel.skipFully(elementOffsetSize); //block offset

        if (elementCount > dataBlockPageElements) {
            /*
            Paged data block : the pages follow each other, each one ending with
            its own checksum. The page bitmap lives in the owning super block.
            */
            final long page = within / dataBlockPageElements;
            channel.skipFully(page * (dataBlockPageElements * elementSize + 4));
            within %= dataBlockPageElements;
        }
        channel.skipFully(within * elementSize);
        return readElement(channel, elementIndex);
    }

    @Override
    public Chunk getChunk(HDF5DataInput channel, long[] scaled) throws IOException {
        load(channel);
        final long index = grid.linearIndex(scaled);
        if (index < 0 || index >= maxIndexSet) return null;
        channel.mark();
        try {
            return lookup(channel, index);
        } finally {
            channel.reset();
        }
    }

    @Override
    public List<Chunk> getChunks(HDF5DataInput channel) throws IOException {
        load(channel);
        final List<Chunk> chunks = new ArrayList<>();
        channel.mark();
        try {
            for (long i = 0; i < maxIndexSet; i++) {
                final Chunk chunk = lookup(channel, i);
                if (chunk != null) chunks.add(chunk);
            }
        } finally {
            channel.reset();
        }
        return chunks;
    }
}
