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
package org.apache.sis.storage.hdf.heap;

import java.io.IOException;
import org.apache.sis.storage.hdf.base.IOStructure;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * The format for the ID used to locate an object in the global heap.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_globalheap">III.E. Disk Format: Level 1E - Global Heap</a>
 * @author Johann Sorel (Geomatys)
 */
public final class GlobalHeapId extends IOStructure {

    /**
     * This field is the address of the global heap collection where the data
     * object is stored.
     */
    public long collectionAddress;
    /**
     * This field is the index of the data object within the global heap collection.
     */
    public int objectIndex;

    /** Creates an empty global heap ID, filled by {@link #read(HDF5DataInput)}. */
    public GlobalHeapId() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        collectionAddress = channel.readOffset();
        objectIndex = channel.readInt();
    }
}
