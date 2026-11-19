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
 * Header Message Name: Symbol Table Message
 * <p>
 * Header Message Type: 0x0011
 * <p>
 * Length: Fixed
 * <p>
 * Status: Required for “old style” groups; may not be repeated.
 * <p>
 * Description:
 * Each “old style” group has a v1 B-tree and a local heap for storing symbol
 * table entries, which are located with this message.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_stmgroup">IV.A.2.r. The Symbol Table Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SymbolTableMessage extends Message {

    /**
     * This value is the address of the v1 B-tree containing the symbol table
     * entries for the group.
     */
    public long v1BtreeAddress;
    /**
     * This value is the address of the local heap containing the link names
     * for the symbol table entries for the group.
     */
    public long localHeapAddress;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public SymbolTableMessage() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        v1BtreeAddress = channel.readOffset();
        localHeapAddress = channel.readOffset();
    }
}
