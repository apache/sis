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
package org.apache.sis.storage.hdf.datatype;

import org.apache.sis.storage.hdf.internal.Constants;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Array datatype : a fixed size, multi dimensional array of another datatype.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class Array extends DataType {

    /** Number of dimensions of the array. */
    public final int dimensionality;
    /** Size of each dimension, the first one being the slowest changing. */
    public final int[] dimensionSizes;
    /** Datatype of the array elements. */
    public final DataType baseType;

    /**
     * Reads the datatype properties.
     *
     * @param byteSize size in bytes of a whole array.
     * @param version datatype message version (2 or more).
     * @param classBitFields class bit fields, unused.
     * @param channel input positioned on the datatype properties.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the base datatype is invalid or unsupported.
     */
    public Array(int byteSize, int version, int classBitFields, HDF5DataInput channel) throws IOException, DataStoreException {
        super(byteSize);
        dimensionality = channel.readUnsignedByte();
        if (version == 2) {
            channel.skipFully(3); //reserved
        }
        dimensionSizes = channel.readInts(dimensionality);
        if (version == 2) {
            /*
            Permutation indices were never implemented by the HDF5 library, they
            are always the identity. Read them and warn rather than reorder.
            */
            final int[] permutation = channel.readInts(dimensionality);
            for (int i = 0; i < permutation.length; i++) {
                if (permutation[i] != i) {
                    Constants.LOGGER.log(Level.WARNING, "Array datatype declares a non identity"
                            + " permutation, which is not supported; dimensions are left in order.");
                    break;
                }
            }
        }
        baseType = DataType.readMessageType(channel);
    }

    /**
     * Returns the number of elements, product of all dimension sizes.
     *
     * @return the number of elements, product of all dimension sizes
     */
    public int getElementCount() {
        int count = 1;
        for (int d : dimensionSizes) {
            count *= d;
        }
        return count;
    }

    @Override
    public Class getValueClass() {
        return java.lang.reflect.Array.newInstance(baseType.getValueClass(),
                new int[dimensionality]).getClass();
    }

    @Override
    public Object readData(HDF5DataInput input, int ... compoundindexes) throws IOException, DataStoreException {
        return baseType.readData(input, dimensionSizes, compoundindexes);
    }
}
