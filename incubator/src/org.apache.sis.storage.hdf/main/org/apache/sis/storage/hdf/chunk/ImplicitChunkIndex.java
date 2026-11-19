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

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Implicit chunk index : the chunks are laid out contiguously in the file, in
 * row major order, so a chunk address is pure arithmetic and no index structure
 * is stored at all.
 * <p>
 * This index requires early allocation and forbids a filter pipeline, so every
 * chunk exists and none is filtered.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixc">Appendix C: Types of Indexes for Dataset Chunks</a>
 * @author Johann Sorel (Geomatys)
 */
public final class ImplicitChunkIndex implements ChunkIndex {

    private final long baseAddress;
    private final long chunkByteSize;
    private final ChunkGrid grid;

    /**
     * Creates an index of chunks stored contiguously from the given address.
     *
     * @param baseAddress file address of the first chunk
     * @param chunkByteSize size in bytes of one chunk
     * @param grid chunk grid of the dataset
     */
    public ImplicitChunkIndex(long baseAddress, long chunkByteSize, ChunkGrid grid) {
        this.baseAddress = baseAddress;
        this.chunkByteSize = chunkByteSize;
        this.grid = grid;
    }

    @Override
    public Chunk getChunk(HDF5DataInput channel, long[] scaled) {
        final long[] counts = grid.getChunkCounts();
        for (int i = 0; i < scaled.length; i++) {
            if (scaled[i] < 0 || scaled[i] >= counts[i]) return null;
        }
        final Chunk chunk = new Chunk();
        chunk.address = baseAddress + grid.linearIndex(scaled) * chunkByteSize;
        chunk.size = chunkByteSize;
        chunk.uncompressedSize = chunkByteSize;
        chunk.filtered = false;
        chunk.offset = grid.extentOf(scaled);
        return chunk;
    }

    @Override
    public List<Chunk> getChunks(HDF5DataInput channel) {
        final long[] counts = grid.getChunkCounts();
        long total = 1;
        for (long c : counts) {
            total *= c;
        }
        final List<Chunk> chunks = new ArrayList<>();
        for (long i = 0; i < total; i++) {
            chunks.add(getChunk(channel, grid.scaledOf(i)));
        }
        return chunks;
    }
}
