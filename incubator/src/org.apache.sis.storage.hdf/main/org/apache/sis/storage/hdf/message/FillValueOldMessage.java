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
 * Header Message Name: Fill Value (old)
 * <p>
 * Header Message Type: 0x0004
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * The fill value message stores a single data value which is returned to the
 * application when an uninitialized data element is read from a dataset. The
 * fill value is interpreted with the same datatype as the dataset. If no fill
 * value message is present then a fill value of all zero bytes is assumed.
 * <p>
 * This fill value message is deprecated in favor of the “new” fill value message
 * (Message Type 0x0005) and is only written to the file for forward compatibility
 * with versions of the HDF5 Library before the 1.6.0 version. Additionally, it
 * only appears for datasets with a user-defined fill value (as opposed to the
 * library default fill value or an explicitly set “undefined” fill value).
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_ofvmessage">IV.A.2.e. The Data Storage - Fill Value (Old) Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FillValueOldMessage extends Message {

    /** This is the size of the Fill Value field in bytes. */
    private int size;
    /**
     * The fill value. The bytes of the fill value are interpreted using the same
     * datatype as for the dataset.
     */
    private byte[] fillValue;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public FillValueOldMessage() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        size = channel.readInt();
        fillValue = channel.readNBytes(size);
    }
}
