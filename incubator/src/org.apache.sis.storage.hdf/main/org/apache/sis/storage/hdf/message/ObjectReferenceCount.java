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
 * Header Message Name: Object Reference Count
 * <p>
 * Header Message Type: 0x0016
 * <p>
 * Length: Fixed
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * This message stores the number of hard links (in groups or objects) pointing
 * to an object: in other words, its reference count.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_refcount">IV.A.2.w. The Object Reference Count Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class ObjectReferenceCount extends Message {

    /**
     * The unsigned 32-bit integer is the reference count for the object. This
     * message is only present in “version 2” (or later) object headers, and if
     * not present those object header versions, the reference count for the
     * object is assumed to be 1.
     */
    private int referenceCount;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public ObjectReferenceCount() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number for this message. This document describes version 0.
        */
        channel.ensureVersion(0);
        referenceCount = channel.readInt();
    }
}
