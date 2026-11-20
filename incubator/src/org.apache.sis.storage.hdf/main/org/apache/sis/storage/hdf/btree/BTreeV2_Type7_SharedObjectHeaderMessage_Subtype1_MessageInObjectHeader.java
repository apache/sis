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
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Record of a version 2 B-tree of type 7, used for indexing shared object header
 * messages, for a message stored in an object header (message location 1).
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_infra_btrees_v2">III.A.2. Disk Format: Level 1A2 - Version 2 B-trees</a>
 * @author Johann Sorel (Geomatys)
 */
public final class BTreeV2_Type7_SharedObjectHeaderMessage_Subtype1_MessageInObjectHeader {

    /**
     * This field Indicates the location where the message is stored:
     * <ul>
     *   <li>0 : Shared message is stored in shared message index heap.</li>
     *   <li>1 : Shared message is stored in object header.</li>
     * </ul>
     */
    private int messageLocation;
    /**
     * This field is hash value of the shared message. The hash value is the
     * Jenkins’ lookup3 checksum algorithm applied to the shared message.
     */
    private int hash;
    /**
     * The object header message type of the shared message.
     */
    private int messageType;
    /**
     * This field indicates that the shared message is the nth message of its
     * type in the specified object header.
     */
    private int objectHeaderIndex;
    /**
     * The address of the object header containing the shared message.
     */
    private long objectHeaderAddress;

    /** Creates an empty record, filled by {@link #read(HDF5DataInput)}. */
    public BTreeV2_Type7_SharedObjectHeaderMessage_Subtype1_MessageInObjectHeader() {
    }

    /**
     * Reads the record.
     *
     * @param channel channel positioned on the record
     * @throws IOException if an I/O error occurred.
     */
    public void read(HDF5DataInput channel) throws IOException {

        messageLocation = channel.readUnsignedByte();
        hash = channel.readInt();
        channel.skipFully(1); //reserved
        /*
        The message type is one byte and the object header index two, so that this
        record is the same 17 bytes as the subtype 0 record it shares a B-tree with.
        */
        messageType = channel.readUnsignedByte();
        objectHeaderIndex = channel.readUnsignedShort();
        objectHeaderAddress = channel.readOffset();
    }
}
