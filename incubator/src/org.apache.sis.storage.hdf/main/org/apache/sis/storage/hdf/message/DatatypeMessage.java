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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.datatype.DataType;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: Datatype
 * <p>
 * Header Message Type: 0x0003
 * <p>
 * Length: Variable
 * <p>
 * Status: Required for dataset or committed datatype (formerly named datatype)
 * objects; may not be repeated.
 * <p>
 * Description: The datatype message defines the datatype for each element of a
 * dataset or a common datatype for sharing between multiple datasets. A datatype
 * can describe an atomic type like a fixed- or floating-point type or more
 * complex types like a C struct (compound datatype), array (array datatype),
 * or C++ vector (variable-length datatype).
 * <p>
 * Datatype messages that are part of a dataset object do not describe how
 * elements are related to one another; the dataspace message is used for that
 * purpose. Datatype messages that are part of a committed datatype (formerly
 * named datatype) message describe a common datatype that can be shared by
 * multiple datasets in the file.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class DatatypeMessage extends Message {

    private DataType dataType;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public DatatypeMessage() {
    }

    /**
     * Returns the datatype of each element of the dataset or committed datatype.
     *
     * @return the datatype of each element of the dataset or committed datatype
     */
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException, DataStoreException {
        dataType = DataType.readMessageType(channel);
    }

}
