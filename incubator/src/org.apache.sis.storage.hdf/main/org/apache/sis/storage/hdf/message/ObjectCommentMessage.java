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
import java.nio.charset.StandardCharsets;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: Object Comment
 * <p>
 * Header Message Type: 0x000D
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * The object comment is designed to be a short description of an object. An
 * object comment is a sequence of non-zero ({@code \0}) ASCII characters with no
 * other formatting included by the library.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_comment">IV.A.2.n. The Object Comment Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class ObjectCommentMessage extends Message {

    /** A null terminated ASCII character string. */
    private String comment;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public ObjectCommentMessage() {
    }

    /**
     * Returns the object comment, a short description of the object.
     *
     * @return the object comment, a short description of the object
     */
    public String getComment() {
        return comment;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        comment = channel.readNullTerminatedString(0, StandardCharsets.US_ASCII);
    }
}
