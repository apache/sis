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
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Locates the chunks of a dataset.
 * <p>
 * Implementations exist for the version 1 B-tree of the classic format and for
 * the five chunk indexing types of the version 4 data layout message.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface ChunkIndex {

    /**
     * Returns the chunk at the given scaled coordinates, or null when not allocated.
     * The dataset fill value applies to unallocated chunks.
     *
     * @param channel file channel
     * @param scaled chunk grid coordinates, see {@link ChunkGrid}
     * @return the chunk at the given scaled coordinates, or null when not allocated
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the index structures are invalid or unsupported.
     */
    Chunk getChunk(HDF5DataInput channel, long[] scaled) throws IOException, DataStoreException;

    /**
     * Returns all allocated chunks of the dataset.
     * May be costly; {@link #getChunk(HDF5DataInput, long[])} is the way to read a sub extent.
     *
     * @param channel file channel
     * @return all allocated chunks of the dataset
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the index structures are invalid or unsupported.
     */
    List<Chunk> getChunks(HDF5DataInput channel) throws IOException, DataStoreException;

    /**
     * Returns whether a bit of a page bitmap is set, meaning the page is present in the file.
     * Follows the {@code H5VM_bit_get} convention used by the fixed and extensible arrays :
     * bits are numbered from the most significant bit of each byte.
     *
     * @param bitmap bitmap bytes
     * @param index bit index
     * @return whether a bit of a page bitmap is set, meaning the page is present in the file
     */
    static boolean isBitSet(byte[] bitmap, long index) {
        return (bitmap[(int) (index / 8)] >> (7 - (index % 8)) & 0x01) != 0;
    }
}
