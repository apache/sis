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

import java.util.Collections;
import java.util.List;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Index of a dataset stored as a single block : compact and contiguous layouts,
 * and the Single Chunk indexing type of the version 4 data layout message.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SingleChunkIndex implements ChunkIndex {

    private final Chunk chunk;

    /**
     * Creates an index holding a single chunk.
     *
     * @param chunk the only chunk of the dataset, or null when storage has not
     *        been allocated.
     */
    public SingleChunkIndex(Chunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public Chunk getChunk(HDF5DataInput channel, long[] scaled) {
        for (long s : scaled) {
            if (s != 0) return null;
        }
        return chunk;
    }

    @Override
    public List<Chunk> getChunks(HDF5DataInput channel) {
        return chunk == null ? Collections.emptyList() : Collections.singletonList(chunk);
    }
}
