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
package org.apache.sis.storage.hdf.btree;

import java.io.IOException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.hdf.chunk.Chunk;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * For nodes of node type 1 (chunked raw data nodes), the key is formatted as follows:
 * <ul>
 * <li>Bytes 1-4: size of the chunk in bytes.</li>
 * <li>Bytes 4-8: filter mask.</li>
 * <li>(D + 1) 64-bit fields: the offset of the chunk within the dataset, where D
 * is the number of dimensions of the dataset. The last value is the offset within
 * the dataset datatype and should always be zero.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class BTreeV1Chunk extends Chunk {

    /** Creates an empty chunk key, filled by {@link #read(HDF5DataInput, int[])}. */
    public BTreeV1Chunk() {
    }

    /**
     * Reads the chunk key. The chunk address, which follows the key in the node, is not read.
     *
     * @param channel channel positioned on the key
     * @param chunkDimension chunk size in each dimension, used to compute the chunk extent
     * @throws IOException if an I/O error occurred or the last offset is not zero.
     */
    public void read(HDF5DataInput channel, int[] chunkDimension) throws IOException {
        size = channel.readUnsignedInt();
        filterMask = channel.readUnsignedInt();
        final long[] low = new long[chunkDimension.length];
        final long[] high = new long[chunkDimension.length];
        for (int i = 0; i < low.length; i++) {
            low[i] = channel.readLong();
            high[i] = low[i] + chunkDimension[i];
        }
        this.offset = new GridExtent(null, low, high, false);
        long last = channel.readLong();
        if (last != 0) {
            throw new IOException("Last dimension should be 0 but was " + last);
        }
    }
}
