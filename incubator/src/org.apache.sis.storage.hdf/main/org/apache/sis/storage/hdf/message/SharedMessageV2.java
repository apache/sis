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
 * Shared message, version 2 : locates a message stored in another object header.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_dataobject_hdr_msg">IV.A.2. Disk Format: Level 2A2 - Data Object Header Messages</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SharedMessageV2 extends Message {

    /**
     * The type of shared message location:
     * <ul>
     *   <li>0 : Message stored in another object’s header (a committed message).</li>
     * </ul>
     */
    private int type;
    /** The address of the object header containing the message to be shared. */
    private long address;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public SharedMessageV2() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number is used when there are changes in the format of a
        shared object message and is described here:
        2 : Used by the library of version 1.6.1 and after.
         */
        channel.ensureVersion(2);
        type = channel.readUnsignedByte();
        address = channel.readOffset();
    }
}
