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
package org.apache.sis.storage.hdf.base;

import java.io.IOException;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Shared message record of an index node, for a message stored in the fractal heap of the index.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_sohm">III.I. Disk Format: Level 1I - Shared Object Header Message Table</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SharedMessageRecordStoredInFractalHeap extends IOStructure {

    /**
     * This has a value of 0 indicating that the message is stored in the heap.
     */
    private int messageLocation;
    /**
     * This is the hash value for the message.
     */
    private int hashValue;
    /**
     * This is the number of times the message is used in the file.
     */
    private int referenceCount;
    /**
     * This is an 8-byte fractal heap ID for the message as stored in the fractal heap for the index.
     */
    public Object fractalHeapId;


    /** Creates an empty structure, filled by {@link #read(HDF5DataInput)}. */
    public SharedMessageRecordStoredInFractalHeap() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        messageLocation = channel.readUnsignedByte();
        hashValue = channel.readInt();
        referenceCount = channel.readInt();
        /*
        fractalHeapId
        */
        throw new IOException("TODO");
    }
}
