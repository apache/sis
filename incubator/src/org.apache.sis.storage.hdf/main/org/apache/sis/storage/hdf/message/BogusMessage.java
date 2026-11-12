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
 * Header Message Name: Bogus
 * <p>
 * Header Message Type: 0x0009
 * <p>
 * Length: 4 bytes
 * <p>
 * Status: For testing only; should never be stored in a valid file.
 * <p>
 * Description:
 * This message is used for testing the HDF5 Library’s response to an “unknown”
 * message type and should never be encountered in a valid HDF5 file.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_bogus">IV.A.2.j. The Bogus Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class BogusMessage extends Message {

    /** This value should always be: 0xdeadbeef. */
    private int bogusValue;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public BogusMessage() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        bogusValue = channel.readInt();
    }
}
