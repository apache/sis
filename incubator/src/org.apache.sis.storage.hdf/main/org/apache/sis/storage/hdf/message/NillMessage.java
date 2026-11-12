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
 * Header Message Name: NIL
 * <p>
 * Header Message Type: 0x0000
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may be repeated.
 * <p>
 * Description:
 * The NIL message is used to indicate a message which is to be ignored when
 * reading the header messages for a data object. [Possibly one which has
 * been deleted for some reason.]
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_nil">IV.A.2.a. The NIL Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class NillMessage extends Message {

    /** Creates a NIL message, which has no content. */
    public NillMessage() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
    }
}
