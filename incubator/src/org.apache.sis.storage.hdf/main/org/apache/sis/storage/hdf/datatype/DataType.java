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
import org.apache.sis.storage.DataStoreException;
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
public abstract class DataType {

    /** Datatype class of {@link FixedPoint}. */
    public static final int CLASSE_FIXED_POINT = 0;
    /** Datatype class of {@link FloatingPoint}. */
    public static final int CLASSE_FLOATING_POINT = 1;
    /** Datatype class of {@link Time}. */
    public static final int CLASSE_TIME = 2;
    /** Datatype class of {@link Chars} (fixed length strings). */
    public static final int CLASSE_STRING = 3;
    /** Datatype class of {@link BitField}. */
    public static final int CLASSE_BIT_FIELD = 4;
    /** Datatype class of {@link Opaque}. */
    public static final int CLASSE_OPAQUE = 5;
    /** Datatype class of {@link Compound}. */
    public static final int CLASSE_COMPOUND = 6;
    /** Datatype class of {@link Reference}. */
    public static final int CLASSE_REFERENCE = 7;
    /** Datatype class of {@link Enumerated}. */
    public static final int CLASSE_ENUMERATED = 8;
    /** Datatype class of {@link VariableLength}. */
    public static final int CLASSE_VARIABLE_LENGTH = 9;
    /** Datatype class of {@link Array}. */
    public static final int CLASSE_ARRAY = 10;

    /** Size of a datatype element in bytes. */
    protected final int byteSize;

    /**
     * Creates a datatype.
     *
     * @param byteSize size of an element in bytes.
     */
    public DataType(int byteSize) {
        this.byteSize = byteSize;
    }

    /**
     * Returns the Java class of a decoded value, also the component type of value arrays.
     *
     * @return the Java class of a decoded value, also the component type of value arrays
     */
    public abstract Class getValueClass();

    /**
     * Returns the size of a datatype element in bytes.
     *
     * @return the size of a datatype element in bytes
     */
    public int getByteSize() {
        return byteSize;
    }

    /**
     * Reads a single value or a multi-dimensional array of values, the last
     * dimension changing fastest.
     *
     * @param input input to read from.
     * @param dimensions array dimension sizes, {@code null} or empty for a scalar value.
     * @param compoundindexes compound members to read, ignored by other types.
     * @return the value, or an array of {@link #getValueClass()} elements.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a value can not be decoded.
     */
    public Object readData(HDF5DataInput input, int[] dimensions, int ... compoundindexes) throws IOException, DataStoreException {
        if (dimensions == null || dimensions.length == 0) {
            return readData(input, compoundindexes);
        }
        final Object array = java.lang.reflect.Array.newInstance(getValueClass(), dimensions);
        fill(input, array, dimensions, 0, compoundindexes);
        return array;
    }

    /**
     * Fills an array of any rank, the last dimension changing fastest.
     *
     * @param input input to read from.
     * @param array array or sub-array to fill.
     * @param dimensions dimension sizes of the full array.
     * @param depth dimension index of {@code array}.
     * @param compoundindexes compound members to read.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a value can not be decoded.
     */
    private void fill(HDF5DataInput input, Object array, int[] dimensions, int depth,
            int ... compoundindexes) throws IOException, DataStoreException {
        final int size = dimensions[depth];
        if (depth == dimensions.length - 1) {
            for (int i = 0; i < size; i++) {
                java.lang.reflect.Array.set(array, i, readData(input, compoundindexes));
            }
        } else {
            for (int i = 0; i < size; i++) {
                fill(input, java.lang.reflect.Array.get(array, i), dimensions, depth + 1, compoundindexes);
            }
        }
    }

    /**
     * Reads a strip of consecutive values.
     *
     * @param input input to read from.
     * @param size number of values.
     * @param compoundindexes compound members to read, ignored by other types.
     * @return an array of {@link #getValueClass()} elements.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a value can not be decoded.
     */
    public Object readData(HDF5DataInput input, int size, int ... compoundindexes) throws IOException, DataStoreException {
        Object array = java.lang.reflect.Array.newInstance(getValueClass(), size);
        for (int x = 0; x < size; x++) {
            java.lang.reflect.Array.set(array, x, readData(input, compoundindexes));
        }
        return array;
    }

    /**
     * Reads a single value at the current position, leaving the input after it.
     *
     * @param input input to read from.
     * @param compoundindexes compound members to read, ignored by other types.
     * @return a {@link #getValueClass()} instance, or its wrapper for primitives.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the value can not be decoded.
     */
    public abstract Object readData(HDF5DataInput input, int ... compoundindexes) throws IOException, DataStoreException;

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    /**
     * Reads a datatype description, from a datatype message or nested in another datatype.
     *
     * @param channel input positioned on the datatype description.
     * @return the datatype.
     * @throws IOException if an I/O error occurred or the datatype class is unknown.
     * @throws DataStoreException if the datatype description is invalid or unsupported.
     */
    public static DataType readMessageType(HDF5DataInput channel) throws IOException, DataStoreException {

        /*
        The version of the datatype message and the datatype’s class information
        are packed together in this field. The version number is packed in the
        top 4 bits of the field and the class is contained in the bottom 4 bits.

        The version number information is used for changes in the format of the
        datatype message and is described here:
        <ul>
        <li>0 : Never used</li>
        <li>1 : Used by early versions of the library to encode compound
        datatypes with explicit array fields. See the compound datatype
        description below for further details.</li>
        <li>2 : Used when an array datatype needs to be encoded.</li>
        <li>3 : Used when a VAX byte-ordered type needs to be encoded. Packs
        various other datatype classes more efficiently also.</li>
        <li>4 : Used to encode the revised reference datatype. </li>
        </ul>

        The class of the datatype determines the format for the class bit field
        and properties portion of the datatype message, which are described below.
        The following classes are currently defined:
        <ul>
        <li>0 : Fixed-Point</li>
        <li>1 : Floating-Point</li>
        <li>2 : Time</li>
        <li>3 : String</li>
        <li>4 : Bit field</li>
        <li>5 : Opaque</li>
        <li>6 : Compound</li>
        <li>7 : Reference</li>
        <li>8 : Enumerated</li>
        <li>9 : Variable-Length</li>
        <li>10 : Array</li>
        </ul>
        */
        final int classAndVersion = channel.readUnsignedByte();
        final int classe = classAndVersion & 0b1111;
        final int version = (classAndVersion & 0b11110000) >> 4;
        /*
        The information in these bit fields is specific to each datatype class
        and is described below. All bits not defined for a datatype class are
        set to zero.
        */
        final int classBitFields = channel.readUnsignedInt24();
        /*
        The size of a datatype element in bytes.
        */
        final int byteSize = channel.readInt();

        final DataType dataType = switch (classe) {
            case CLASSE_FIXED_POINT     -> new FixedPoint(byteSize, classBitFields, channel);
            case CLASSE_FLOATING_POINT  -> new FloatingPoint(byteSize, classBitFields, channel);
            case CLASSE_TIME            -> new Time(byteSize, classBitFields, channel);
            case CLASSE_STRING          -> new Chars(byteSize, classBitFields, channel);
            case CLASSE_BIT_FIELD       -> new BitField(byteSize, classBitFields, channel);
            case CLASSE_OPAQUE          -> new Opaque(byteSize, classBitFields, channel);
            case CLASSE_COMPOUND        -> new Compound(byteSize, version, classBitFields, channel);
            case CLASSE_REFERENCE       -> new Reference(byteSize, version, classBitFields, channel);
            case CLASSE_ENUMERATED      -> new Enumerated(byteSize, version, classBitFields, channel);
            case CLASSE_VARIABLE_LENGTH -> new VariableLength(byteSize, classBitFields, channel);
            case CLASSE_ARRAY           -> new Array(byteSize, version, classBitFields, channel);
            default                     -> throw new IOException("Unexpected classe " + classe);
        };
        return dataType;
    }

}
