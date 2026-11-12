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
 * Shared message record of an index node, for a message stored in an object header.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_sohm">III.I. Disk Format: Level 1I - Shared Object Header Message Table</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SharedMessageRecordStoredInObjectHeader extends IOStructure {

    /**
     * This has a value of 1 indicating that the message is stored in an object header.
     */
    private int messageLocation;
    /**
     * This is the hash value for the message.
     */
    private int hashValue;
    /**
     * This is the message type in the object header.
     */
    private int messageType;
    /**
     * This is the creation index of the message within the object header.
     */
    private int creationIndex;
    /**
     * This is the address of the object header where the message is located.
     */
    private long objectHeaderAddress;


    /** Creates an empty structure, filled by {@link #read(HDF5DataInput)}. */
    public SharedMessageRecordStoredInObjectHeader() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        messageLocation = channel.readUnsignedByte();
        hashValue = channel.readInt();
        channel.skipFully(1); //reserved
        messageType = channel.readUnsignedByte();
        //the creation index is two bytes, making this record 17 bytes long
        creationIndex = channel.readUnsignedShort();
        objectHeaderAddress = channel.readOffset();
    }
}
