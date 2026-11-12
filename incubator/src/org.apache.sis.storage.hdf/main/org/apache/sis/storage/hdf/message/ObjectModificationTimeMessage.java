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
 * Header Message Name: Object Modification Time
 * <p>
 * Header Message Type: 0x0012
 * <p>
 * Length: Fixed
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * The object modification time is a timestamp which indicates the time of the
 * last modification of an object. The time is updated when any object header
 * message changes according to the system clock where the change was posted.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_mod">IV.A.2.s. The Object Modification Time Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class ObjectModificationTimeMessage extends Message {

    /**
     * A 32-bit unsigned integer value that stores the number of seconds since
     * 0 hours, 0 minutes, 0 seconds, January 1, 1970, Coordinated Universal
     * Time.
     */
    private int secondsAfterUnixEpoch;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public ObjectModificationTimeMessage() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number is used for changes in the format of Object
        Modification Time and is described here:
        0 : Never used.
        1 : Used by Version 1.6.1 and after of the library to encode time. In
        this version, the time is the seconds after Epoch.
         */
        channel.ensureVersion(1);
        channel.skipFully(3);
        secondsAfterUnixEpoch = channel.readInt();
    }
}
