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
 * Header Message Name: B-tree ‘K’ Values
 * <p>
 * Header Message Type: 0x0013
 * <p>
 * Length: Fixed
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * This message retrieves non-default ‘K’ values for internal and leaf nodes of
 * a group or indexed storage v1 B-trees. This message is only found in the
 * superblock extension.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_btreek">IV.A.2.t. The B-tree &lsquo;K&rsquo; Values Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class BtreeKValuesMessage extends Message {

    /**
     * This is the node ‘K’ value for each internal node of an indexed storage
     * v1 B-tree. See the description of this field in version 0 and 1 of the
     * superblock as well the section on v1 B-trees.
     */
    private int indexStorageInternalNodeK;
    /**
     * This is the node ‘K’ value for each internal node of a group v1 B-tree.
     * See the description of this field in version 0 and 1 of the superblock
     * as well as the section on v1 B-trees.
     */
    private int groupInternalNodeK;
    /**
     * This is the node ‘K’ value for each leaf node of a group v1 B-tree. See
     * the description of this field in version 0 and 1 of the superblock as
     * well as the section on v1 B-trees.
     */
    private int groupLeafNodeK;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public BtreeKValuesMessage() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number for this message. This document describes version 0.
        */
        channel.ensureVersion(0);
        indexStorageInternalNodeK = channel.readUnsignedShort();
        groupInternalNodeK = channel.readUnsignedShort();
        groupLeafNodeK = channel.readUnsignedShort();
    }
}
