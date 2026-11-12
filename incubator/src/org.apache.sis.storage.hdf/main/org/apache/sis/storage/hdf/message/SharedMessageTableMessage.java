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
 * Header Message Name: Shared Message Table
 * <p>
 * Header Message Type: 0x000F
 * <p>
 * Length: Fixed
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description: This message is used to locate the table of shared
 * object header message (SOHM) indexes. Each index consists of information to
 * find the shared messages from either the heap or object header. This message
 * is only found in the superblock extension.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_shared">IV.A.2.p. The Shared Message Table Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SharedMessageTableMessage extends Message {

    /**
     * This field is the address of the master table for shared object header
     * message indexes.
     */
    private long sharedObjectHeaderMessageTableAddress;
    /** This field is the number of indices in the master table. */
    private int numberOfIndices;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public SharedMessageTableMessage() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number for this message. This document describes version 0.
        */
        channel.ensureVersion(0);
        sharedObjectHeaderMessageTableAddress = channel.readOffset();
        numberOfIndices = channel.readUnsignedByte();
    }
}
