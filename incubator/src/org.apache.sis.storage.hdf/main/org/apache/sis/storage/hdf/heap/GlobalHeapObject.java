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
 * An object of a global heap collection: a header (index, reference count, size)
 * followed by the object data, of which only the file position is kept.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_globalheap">III.E. Disk Format: Level 1E - Global Heap</a>
 * @author Johann Sorel (Geomatys)
 */
public final class GlobalHeapObject extends IOStructure {

    /**
     * Each object has a unique identification number within a collection.
     * The identification numbers are chosen so that new objects have the
     * smallest value possible with the exception that the identifier 0 always
     * refers to the object which represents all free space within the collection.
     */
    public int heapObjectIndex;
    /**
     * All heap objects have a reference count field. An object which is
     * referenced from some other part of the file will have a positive reference
     * count. The reference count for Object 0 is always zero.
     */
    public int referenceCount;
    /**
     * This is the size of the object data stored for the object. The actual
     * storage space allocated for the object data is rounded up to a
     * multiple of eight.
     */
    public long objectSize;
    /**
     * Object data position in the channel.
     * <p>
     * The object data is treated as a one-dimensional array of bytes to be
     * interpreted by the caller.
     */
    public long streamDataPosition;

    /** Creates an empty heap object, filled by {@link #read(HDF5DataInput)}. */
    public GlobalHeapObject() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        heapObjectIndex = channel.readUnsignedShort();
        referenceCount = channel.readUnsignedShort();
        channel.skipFully(4);
        objectSize = channel.readLength();
        streamDataPosition = channel.getStreamPosition();
        if (heapObjectIndex != 0) {
            //object 0 is a marker which indicates the remaining space in the heap.
            channel.skipFully(Math.toIntExact(objectSize));
        }
    }

}
