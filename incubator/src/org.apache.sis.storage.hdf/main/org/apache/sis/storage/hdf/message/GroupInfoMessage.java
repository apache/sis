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
package org.apache.sis.storage.hdf.message;

import java.io.IOException;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: Group Info
 * <p>
 * Header Message Type: 0x000A
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * This message stores information for the constants defining a “new style”
 * group’s behavior. Constant information will be stored in this message and
 * variable information will be stored in the Link Info message.
 * <p>
 * Note: the “estimated entry” information below is used when determining the
 * size of the object header for the group when it is created.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_groupinfo">IV.A.2.k. The Group Info Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class GroupInfoMessage extends Message {

    /**
     * This is the group information flag with the following definition:
     * <ul>
     *   <li>Bit 0 : If set, link phase change values are stored.</li>
     *   <li>Bit 1 : If set, the estimated entry information is non-default and is stored.</li>
     *   <li>Bits 2-7 : Reserved</li>
     * </ul>
     */
    private int flags;
    /**
     * This field is present if bit 0 of Flags is set.
     * <p>
     * This is the maximum number of links to store “compactly” (in the group’s
     * object header).
     */
    private int linkPhaseChangeMaximumCompactValue;
    /**
     * This field is present if bit 0 of Flags is set.
     * <p>
     * This is the minimum number of links to store “densely” (in the group’s
     * fractal heap). The fractal heap’s address is located in the Link Info
     * message.
     */
    private int linkPhaseChangeMinimumDenseValue;
    /**
     * This field is present if bit 1 of Flags is set.
     * <p>
     * This is the estimated number of entries in groups.
     * If this field is not present, the default value of 4 will be used for
     * the estimated number of group entries.
     */
    private int estimatedNumberOfEntries;
    /**
     * This field is present if bit 1 of Flags is set.
     * <p>
     * This is the estimated length of entry name.
     * If this field is not present, the default value of 8 will be used for
     * the estimated link name length of group entries.
     */
    private int estimatedLinkNameLengthofEntries;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public GroupInfoMessage() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number for this message. This document describes version 0.
        */
        channel.ensureVersion(0);
        flags = channel.readUnsignedByte();
        if ((flags & 1) != 0) {
            linkPhaseChangeMaximumCompactValue = channel.readUnsignedShort();
            linkPhaseChangeMinimumDenseValue = channel.readUnsignedShort();
        }
        if ((flags & 2) != 0) {
            estimatedNumberOfEntries = channel.readUnsignedShort();
            estimatedLinkNameLengthofEntries = channel.readUnsignedShort();
        }
    }
}
