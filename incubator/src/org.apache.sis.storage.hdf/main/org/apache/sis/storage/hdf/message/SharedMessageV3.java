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
 * Shared message, version 3 : locates a message stored in another object header
 * or in the shared object header message heap.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_dataobject_hdr_msg">IV.A.2. Disk Format: Level 2A2 - Data Object Header Messages</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SharedMessageV3 extends Message {

    /**
     * The type of shared message location:
     * <ul>
     *   <li>0 : Message is not shared and is not shareable.</li>
     *   <li>1 : Message stored in file’s shared object header message heap (a shared message).</li>
     *   <li>2 : Message stored in another object’s header (a committed message).</li>
     *   <li>3 : Message stored is not shared, but is shareable.</li>
     * </ul>
     */
    private int type;
    /**
     * This field contains either a Size of Offsets-bytes address of the object
     * header containing the message to be shared, or an 8-byte fractal heap ID
     * for the message in the file’s shared object header message heap.
     */
    private Object location;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public SharedMessageV3() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number indicates changes in the format of shared object message
        and is described here:
        3 : Used by the library of version 1.8 and after. In this version,
        the Type field can indicate that the message is stored in the fractal heap.
         */
        channel.ensureVersion(3);
        type = channel.readUnsignedByte();
        if (type == 1) {
            location = channel.readOffset();
        } else {
            throw new IOException("TODO");
        }

    }
}
