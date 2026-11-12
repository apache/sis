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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.internal.StringUtilities;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Enumeration datatype : a set of name and value pairs, the values being of
 * a base type, usually an integer. Values are decoded as those of the base type.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class Enumerated extends DataType {

    /**
     * Each enumeration type is based on some parent type, usually an integer.
     * The information for that parent type is described recursively by this field.
     */
    private final DataType baseType;
    /**
     * The name for each name/value pair. Each name is stored as a null
     * terminated ASCII string in a multiple of eight bytes. The names are in no
     * particular order.
     */
    private final String[] names;
    /**
     * The list of values in the same order as the names. The values are packed
     * (no inter-value padding) and the size of each value is determined by the
     * parent type.
     */
    private final Object[] values;

    /**
     * Reads the datatype properties : the base type, then the member names and values.
     *
     * @param byteSize element size in bytes.
     * @param version datatype message version.
     * @param classBitFields class bit fields (number of members).
     * @param channel input positioned on the datatype properties.
     * @throws IOException if an I/O error occurred or the version is unexpected.
     * @throws DataStoreException if the base datatype is invalid or unsupported.
     */
    public Enumerated(int byteSize, int version, int classBitFields, HDF5DataInput channel) throws IOException, DataStoreException {
        super(byteSize);
        final int numberOfMembers = classBitFields & 0b1111111111111111;

        names = new String[numberOfMembers];
        values = new Object[numberOfMembers];
        if (version == 1 || version == 2) {
            baseType = DataType.readMessageType(channel);
            for (int i = 0; i < numberOfMembers; i++) {
                names[i] = channel.readNullTerminatedString(8, StandardCharsets.US_ASCII);
            }
            for (int i = 0; i < numberOfMembers; i++) {
                values[i] = baseType.readData(channel);
            }
        } else if (version == 3) {
            baseType = DataType.readMessageType(channel);
            for (int i = 0; i < numberOfMembers; i++) {
                names[i] = channel.readNullTerminatedString(0, StandardCharsets.US_ASCII);
            }
            for (int i = 0; i < numberOfMembers; i++) {
                values[i] = baseType.readData(channel);
            }
        } else {
            throw new IOException("Unexpected enumerated version " + version);
        }
    }

    /**
     * Returns the datatype of the values, usually an integer type.
     *
     * @return the datatype of the values, usually an integer type
     */
    public DataType getBaseType() {
        return baseType;
    }

    /**
     * Returns the member names, in the order of {@link #getValues()}; not a copy.
     *
     * @return the member names, in the order of {@link #getValues()}; not a copy
     */
    public String[] getNames() {
        return names;
    }

    /**
     * Returns the member values, in the order of {@link #getNames()}; not a copy.
     *
     * @return the member values, in the order of {@link #getNames()}; not a copy
     */
    public Object[] getValues() {
        return values;
    }

    @Override
    public Class getValueClass() {
        return baseType.getValueClass();
    }

    /**
     * Returns the name declared for the given value, or {@code null} if none.
     * <p>
     * Values are decoded as those of the base type rather than as names, because
     * the name would change the class of the arrays {@code Dataset} returns.
     *
     * @param value a value decoded with the base type.
     * @return the name declared for the given value, or {@code null} if none
     */
    public String nameOf(Object value) {
        for (int i = 0; i < values.length; i++) {
            if (java.util.Objects.deepEquals(values[i], value)) {
                return names[i];
            }
        }
        return null;
    }

    /**
     * Returns a new map from member value to name, in declaration order.
     *
     * @return a new map from member value to name, in declaration order
     */
    public java.util.Map<Object,String> getMapping() {
        final java.util.Map<Object,String> mapping = new java.util.LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            mapping.put(values[i], names[i]);
        }
        return mapping;
    }

    @Override
    public Object readData(HDF5DataInput input, int ... compoundindexes) throws IOException, DataStoreException {
        return baseType.readData(input, compoundindexes);
    }

    @Override
    public Object readData(HDF5DataInput input, int size, int ... compoundindexes) throws IOException, DataStoreException {
        return baseType.readData(input, size, compoundindexes);
    }

    @Override
    public Object readData(HDF5DataInput input, int[] dimensions, int ... compoundindexes) throws IOException, DataStoreException {
        return baseType.readData(input, dimensions, compoundindexes);
    }

    @Override
    public String toString() {
        final List<String> lst = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            lst.add(values[i] + " : " + names[i]);
        }
        return StringUtilities.toStringTree(this.getClass().getSimpleName(), lst);
    }
}
