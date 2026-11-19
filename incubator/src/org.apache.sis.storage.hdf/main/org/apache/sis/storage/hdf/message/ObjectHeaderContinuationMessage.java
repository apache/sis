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
 * Header Message Name: Object Header Continuation
 * <p>
 * Header Message Type: 0x0010
 * <p>
 * Length: Fixed
 * <p>
 * Status: Optional; may be repeated.
 * <p>
 * Description:
 * The object header continuation is the location in the file of a block
 * containing more header messages for the current data object. This can be
 * used when header blocks become too large or are likely to change over time.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_continuation">IV.A.2.q. The Object Header Continuation Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class ObjectHeaderContinuationMessage extends Message {

    /**
     * This value is the address in the file where the header continuation
     * block is located.
     * <p>
     * The format of the header continuation block that this message points to
     * depends on the version of the object header that the message is contained
     * within.
     * <p>
     * Continuation blocks for version 1 object headers have no special
     * formatting information; they are merely a list of object header message
     * info sequences (type, size, flags, reserved bytes and data for each
     * message sequence). See the description of Version 1 Data Object Header
     * Prefix.
     * <p>
     * Continuation blocks for version 2 object headers do have special formatting
     * information (see the description of Version 2 Data Object Header Prefix).
     */
    public long offset;
    /**
     * This value is the length in bytes of the header continuation block in
     * the file.
     */
    public long length;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public ObjectHeaderContinuationMessage() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        offset = channel.readOffset();
        length = channel.readLength();
    }
}
