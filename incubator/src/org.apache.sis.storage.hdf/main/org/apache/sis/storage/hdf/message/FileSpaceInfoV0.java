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
 * Header Message Name: File Space Info
 * <p>
 * Header Message Type: 0x0017
 * <p>
 * Length: Fixed
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description: This message stores the file space management strategy and
 * the addresses of the free-space managers. This is version 0 of the message.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_fsinfo">IV.A.2.x. The File Space Info Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FileSpaceInfoV0 extends Message {

    /**
     * This is the file space strategy used to manage file space. There are
     * four types:
     * <ul>
     *   <li>1 : {@code H5F_FILE_SPACE_ALL_PERSIST}</li>
     *   <li>2 : {@code H5F_FILE_SPACE_ALL}</li>
     *   <li>3 : {@code H5F_FILE_SPACE_AGGR_VFD}</li>
     *   <li>4 : {@code H5F_FILE_SPACE_VFD}</li>
     * </ul>
     */
    private int strategy;
    /**
     * This is the smallest free-space section size that the free-space manager
     * will track.
     */
    private int threshold;
    /**
     * These are the six free-space manager addresses for the six file space
     * allocation types: {@code H5FD_MEM_SUPER}, {@code H5FD_MEM_BTREE},
     * {@code H5FD_MEM_DRAW}, {@code H5FD_MEM_GHEAP}, {@code H5FD_MEM_LHEAP}
     * and {@code H5FD_MEM_OHDR}.
     * <p>
     * Note that these six fields exist only if the value for the field
     * “Strategy” is {@code H5F_FILE_SPACE_ALL_PERSIST}.
     */
    private long freespaceManagerAddressForH5FD_MEM_SUPER;
    private long freespaceManagerAddressForH5FD_MEM_BTREE;
    private long freespaceManagerAddressForH5FD_MEM_DRAW;
    private long freespaceManagerAddressForH5FD_MEM_GHEAP;
    private long freespaceManagerAddressForH5FD_MEM_LHEAP;
    private long freespaceManagerAddressForH5FD_MEM_OHDR;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public FileSpaceInfoV0() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        /*
        This is version 0 of this message.
         */
        channel.ensureVersion(0);
        strategy = channel.readUnsignedByte();
        threshold = channel.readUnsignedShort();
        freespaceManagerAddressForH5FD_MEM_SUPER = channel.readOffset();
        freespaceManagerAddressForH5FD_MEM_BTREE = channel.readOffset();
        freespaceManagerAddressForH5FD_MEM_DRAW = channel.readOffset();
        freespaceManagerAddressForH5FD_MEM_GHEAP = channel.readOffset();
        freespaceManagerAddressForH5FD_MEM_LHEAP = channel.readOffset();
        freespaceManagerAddressForH5FD_MEM_OHDR = channel.readOffset();
    }
}
