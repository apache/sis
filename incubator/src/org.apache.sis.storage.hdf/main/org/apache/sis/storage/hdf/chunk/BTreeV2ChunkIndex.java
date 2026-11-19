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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.btree.BTreeV2;
import org.apache.sis.storage.hdf.btree.BTreeV2_Type10_DatasetChunks;
import org.apache.sis.storage.hdf.btree.BTreeV2_Type11_FilteredDatasetChunks;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Chunk index backed by a version 2 B-tree, used by datasets having more than one
 * unlimited dimension.
 * <p>
 * Records hold the scaled position of the chunk, so the tree is walked once and
 * the records kept in a map rather than searched for each read.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixc">Appendix C: Types of Indexes for Dataset Chunks</a>
 * @author Johann Sorel (Geomatys)
 */
public final class BTreeV2ChunkIndex implements ChunkIndex {

    /** Records of chunks of a dataset with no filter. */
    private static final int RECORD_UNFILTERED = 10;
    /** Records of chunks of a dataset with a filter pipeline. */
    private static final int RECORD_FILTERED = 11;

    private final long btreeAddress;
    private final ChunkGrid grid;
    private final long chunkByteSize;

    private Map<List<Long>,Chunk> chunks;

    /**
     * Creates an index over a version 2 B-tree, read lazily.
     *
     * @param btreeAddress address of the version 2 B-tree header
     * @param grid chunk grid of the dataset
     * @param chunkByteSize size in bytes of a full chunk, used as the stored size of unfiltered chunks
     */
    public BTreeV2ChunkIndex(long btreeAddress, ChunkGrid grid, long chunkByteSize) {
        this.btreeAddress = btreeAddress;
        this.grid = grid;
        this.chunkByteSize = chunkByteSize;
    }

    private synchronized Map<List<Long>,Chunk> load(HDF5DataInput channel) throws IOException, DataStoreException {
        if (chunks != null) return chunks;
        final Map<List<Long>,Chunk> map = new HashMap<>();
        channel.mark();
        try {
            channel.seek(btreeAddress);
            final BTreeV2 btree = new BTreeV2();
            btree.read(channel);
            final int rank = grid.getDimension();
            final int sizeWidth = BTreeV2_Type11_FilteredDatasetChunks.chunkSizeWidth(
                    btree.getRecordSize(), channel.getOffsetSize(), rank);

            for (byte[] record : btree.readRecords(channel)) {
                final Chunk chunk = new Chunk();
                final long[] scaled;
                try (HDF5DataInput sub = channel.subChannel(record)) {
                    switch (btree.getType()) {
                        case RECORD_UNFILTERED: {
                            final BTreeV2_Type10_DatasetChunks rec = new BTreeV2_Type10_DatasetChunks();
                            rec.read(sub, rank);
                            chunk.address = rec.getAddress();
                            chunk.size = chunkByteSize;
                            chunk.uncompressedSize = chunkByteSize;
                            chunk.filtered = false;
                            scaled = rec.getDimensionScaledOffsets();
                            break;
                        }
                        case RECORD_FILTERED: {
                            final BTreeV2_Type11_FilteredDatasetChunks rec = new BTreeV2_Type11_FilteredDatasetChunks();
                            rec.read(sub, rank, sizeWidth);
                            chunk.address = rec.getAddress();
                            chunk.size = rec.getChunkSize();
                            chunk.filterMask = rec.getFilterMask();
                            chunk.filtered = true;
                            scaled = rec.getDimensionScaledOffsets();
                            break;
                        }
                        default:
                            throw new IOException("Unexpected version 2 B-tree type "
                                    + btree.getType() + " for a dataset chunk index");
                    }
                }
                chunk.offset = grid.extentOf(scaled);
                map.put(key(scaled), chunk);
            }
        } finally {
            channel.reset();
        }
        chunks = map;
        return chunks;
    }

    private static List<Long> key(long[] scaled) {
        final Long[] boxed = new Long[scaled.length];
        for (int i = 0; i < scaled.length; i++) {
            boxed[i] = scaled[i];
        }
        return Arrays.asList(boxed);
    }

    @Override
    public Chunk getChunk(HDF5DataInput channel, long[] scaled) throws IOException, DataStoreException {
        return load(channel).get(key(scaled));
    }

    @Override
    public List<Chunk> getChunks(HDF5DataInput channel) throws IOException, DataStoreException {
        return new ArrayList<>(load(channel).values());
    }
}
