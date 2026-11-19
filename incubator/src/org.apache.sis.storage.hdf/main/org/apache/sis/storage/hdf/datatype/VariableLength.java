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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.heap.GlobalHeapId;
import org.apache.sis.storage.hdf.heap.GlobalHeapObject;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Variable-length datatype : a sequence of a base type or a string, whose
 * content is stored in a global heap. Sequences are decoded as arrays of the
 * base type and strings as {@link String} with their padding removed.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class VariableLength extends DataType {

    private static final int PADDING_NULL_TERMINATED = 0;
    private static final int PADDING_NULL_PAD = 1;
    private static final int PADDING_SPACE_PAD = 2;

    /**
     * Type. This four-bit value contains the type of variable-length datatype described. The values defined are:
     * <ul>
     *   <li>0 : Sequence: A variable-length sequence of any datatype.
     *   Variable-length sequences do not have padding or character set
     *   information.</li>
     *   <li>1 : String: A variable-length sequence of characters. Variable-length
     *   strings have padding and character set information.</li>
     *   <li>2-15 : Reserved</li>
     * </ul>
     */
    public final int type;
    /**
     * Padding type. (variable-length string only) This four-bit value
     * determines the type of padding used for variable-length strings.
     * The values are the same as for the string padding type, as follows:
     * <ul>
     *   <li>0 : Null terminate: A zero byte marks the end of a string and is
     *   guaranteed to be present after converting a long string to a short
     *   string. When converting a short string to a long string, the value is
     *   padded with additional null characters as necessary.</li>
     *   <li>1 : Null pad: Null characters are added to the end of the value during
     *   conversion from a short string to a longer string. Conversion from a
     *   long string to a shorter string simply truncates the value.</li>
     *   <li>2 : Space pad: Space characters are added to the end of the value
     *   during conversion from a short string to a longer string. Conversion
     *   from a long string to a shorter string simply truncates the value.
     *   This is the Fortran representation of the string.</li>
     *   <li>3-15 : Reserved</li>
     * </ul>
     * This value is set to zero for variable-length sequences.
     */
    public final int paddingType;
    /**
     * Character Set. (variable-length string only) This four-bit value
     * specifies the character set to be used for encoding the string:
     * <ul>
     *   <li>0 : ASCII character set encoding</li>
     *   <li>1 : UTF-8 character set encoding</li>
     *   <li>2-15 : Reserved</li>
     * </ul>
     * This value is set to zero for variable-length sequences.
     */
    public final int characterSet;
    /**
     * Each variable-length type is based on some parent type. The information
     * for that parent type is described recursively by this field.
     */
    public final DataType baseType;

    /**
     * Reads the datatype properties, which are the base type.
     *
     * @param byteSize element size in bytes, which is the size of the heap reference.
     * @param classBitFields class bit fields (type, padding, character set).
     * @param channel input positioned on the datatype properties.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the base datatype is invalid or unsupported.
     */
    public VariableLength(int byteSize, int classBitFields, HDF5DataInput channel) throws IOException, DataStoreException {
        super(byteSize);
        type = classBitFields & 0b1111;
        paddingType = (classBitFields & 0b11110000) >> 4;
        characterSet = (classBitFields & 0b111100000000) >> 8;
        baseType =  DataType.readMessageType(channel);
    }

    @Override
    public Class getValueClass() {
        if (type == 0) {
            //a sequence decodes to an array of the base type
            return java.lang.reflect.Array.newInstance(baseType.getValueClass(), 0).getClass();
        } else {
            return String.class;
        }
    }

    /**
     * Apply the padding rule of a fixed or variable length string.
     *
     * @param value raw characters
     * @param paddingType 0 null terminated, 1 null padded, 2 space padded
     * @return the trimmed string
     */
    static String trim(String value, int paddingType) {
        switch (paddingType) {
            case PADDING_NULL_TERMINATED: {
                final int end = value.indexOf('\0');
                return end < 0 ? value : value.substring(0, end);
            }
            case PADDING_NULL_PAD: {
                int end = value.length();
                while (end > 0 && value.charAt(end - 1) == '\0') end--;
                return value.substring(0, end);
            }
            case PADDING_SPACE_PAD: {
                int end = value.length();
                while (end > 0 && value.charAt(end - 1) == ' ') end--;
                return value.substring(0, end);
            }
            default:
                return value;
        }
    }

    @Override
    public Object readData(HDF5DataInput channel, int ... compoundindexes) throws IOException, DataStoreException {

        /*
        Data is not here, it is in a global heap
        this is noted in the global heap section : III.E. Disk Format: Level 1E - Global Heap
        The HDF5 Library creates global heap collections as needed, so there
        may be multiple collections throughout the file. The set of all of
        them is abstractly called the “global heap”, although they do not
        actually link to each other, and there is no global place in the file
        where you can discover all of the collections. The collections are
        found simply by finding a reference to one through another object in
        the file. For example, data of variable-length datatype elements is
        stored in the global heap and is accessed via a global heap ID. The
        format for global heap IDs is described at the end of this section.
        */

        /*
        An element is the number of items of the value, then the global heap
        identifier of the collection holding them.
        */
        final long length = channel.readUnsignedInt();

        final GlobalHeapId globalheapId = new GlobalHeapId();
        globalheapId.read(channel);
        if (globalheapId.collectionAddress <= 0) {
            //no data or corrupted ? specification do no say.
            return null;
        }

        final GlobalHeapObject globalHeapObject = channel.getGlobalHeapObject(globalheapId);
        if (globalHeapObject == null) {
            channel.getGlobalHeapObject(globalheapId);
            //throw new IOException("global heap object not found " + globalheapId);
            return null;
        }

        /*
        The heap object lives in the file, at a position the heap reported. When
        this value is being decoded from a buffer extracted from a heap, such as a
        densely stored attribute, that position must be seeked on the file channel
        rather than on the buffer.
        */
        final HDF5DataInput fileChannel = channel.getFileChannel();

        if (type == 0) {
            /*
            A sequence of the base type. The element gives how many items there
            are; reading a single one would only be correct by accident.
            */
            final int count = Math.toIntExact(length);
            final Object values = java.lang.reflect.Array.newInstance(baseType.getValueClass(), count);
            try {
                fileChannel.mark();
                fileChannel.seek(globalHeapObject.streamDataPosition);
                for (int i = 0; i < count; i++) {
                    java.lang.reflect.Array.set(values, i, baseType.readData(fileChannel));
                }
            } finally {
                fileChannel.reset();
            }
            return values;

        } else if (type == 1) {
            /*
            A string of the given length, the padding rule then telling which of
            those characters are significant.
            */
            try {
                fileChannel.mark();
                fileChannel.seek(globalHeapObject.streamDataPosition);
                final int count = Math.toIntExact(Math.min(length, globalHeapObject.objectSize));
                final byte[] datas = fileChannel.readNBytes(count);
                final String value = new String(datas,
                        characterSet == 0 ? StandardCharsets.US_ASCII : StandardCharsets.UTF_8);
                return trim(value, paddingType);
            } finally {
                fileChannel.reset();
            }

        } else {
            throw new IOException("Unexpected type " + type);
        }
    }
}
