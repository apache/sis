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
package org.apache.sis.storage.gdal;

import java.awt.image.DataBuffer;


/**
 * Type of data loaded by <abbr>GDAL</abbr>.
 * This enumeration mirrors the C/C++ {@code GDALDataType} enumeration in the GDAL's API.
 * The {@linkplain #ordinal() ordinal} values shall be equal to the GDAL enumeration values.
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
enum DataType {
    /**
     * Unknown or unspecified type.
     * In <abbr>GDAL</abbr>: {@code GDT_Unknown} = 0.
     */
    UNKNOWN((short) DataBuffer.TYPE_UNDEFINED, (short) 0),

    /**
     * 8 bits unsigned integer.
     * In <abbr>GDAL</abbr>: {@code GDT_Byte} = 1.
     */
    UNSIGNED_BYTE((short) DataBuffer.TYPE_BYTE, (short) Byte.SIZE),

    /**
     * 16 bits unsigned integer.
     * In <abbr>GDAL</abbr>: {@code GDT_UInt16} = 2.
     */
    UNSIGNED_SHORT((short) DataBuffer.TYPE_USHORT, (short) Short.SIZE),

    /**
     * 16 bits signed integer.
     * In <abbr>GDAL</abbr>: {@code GDT_Int16} = 3.
     */
    SHORT((short) DataBuffer.TYPE_SHORT, (short) Short.SIZE),

    /**
     * 32 bits unsigned integer.
     * In <abbr>GDAL</abbr>: {@code GDT_UInt32} = 4.
     * Note that both this value and {@link #INT} can be assigned to the Java2D {@code TYPE_INT}.
     */
    UNSIGNED_INT((short) DataBuffer.TYPE_INT, (short) Integer.SIZE),

    /**
     * 32 bits signed integer.
     * In <abbr>GDAL</abbr>: {@code GDT_Int32} = 5.
     * Note that both this value and {@link #UNSIGNED_INT} can be assigned to the Java2D {@code TYPE_INT}.
     */
    INT((short) DataBuffer.TYPE_INT, (short) Integer.SIZE),

    /**
     * 32 bit floating point value.
     * In <abbr>GDAL</abbr>: {@code GDT_Float32} = 6.
     */
    FLOAT((short) DataBuffer.TYPE_FLOAT, (short) Float.SIZE),

    /**
     * 64 bits floating point value.
     * In <abbr>GDAL</abbr>: {@code GDT_Float64} = 7.
     */
    DOUBLE((short) DataBuffer.TYPE_DOUBLE, (short) Double.SIZE),

    /**
     * Two 16 bits integers for a complex number.
     * In <abbr>GDAL</abbr>: {@code GDT_CInt16} = 8.
     * This is undefined in Java 2D image <abbr>API</abbr>.
     */
    COMPLEX_SHORT((short) DataBuffer.TYPE_UNDEFINED, (short) (2 * Short.SIZE)),

    /**
     * Two 32 bits integers for a complex number.
     * In <abbr>GDAL</abbr>: {@code GDT_CInt32} = 9.
     * This is undefined in Java 2D image <abbr>API</abbr>.
     */
    COMPLEX_INT((short) DataBuffer.TYPE_UNDEFINED, (short) (2 * Integer.SIZE)),

    /**
     * Two 32 bits floating point values for a complex number.
     * In <abbr>GDAL</abbr>: {@code GDT_CFloat32} = 10.
     * This is undefined in Java 2D image <abbr>API</abbr>.
     */
    COMPLEX_FLOAT((short) DataBuffer.TYPE_UNDEFINED, (short) (2 * Float.SIZE)),

    /**
     * Two 64 bits floating point values for a complex number.
     * In <abbr>GDAL</abbr>: {@code GDT_CFloat64} = 11.
     * This is undefined in Java 2D image <abbr>API</abbr>.
     */
    COMPLEX_DOUBLE((short) DataBuffer.TYPE_UNDEFINED, (short) (2 * Double.SIZE)),

    /**
     * 64 bits unsigned integer.
     * In <abbr>GDAL</abbr>: {@code GDT_UInt64} = 12.
     * This is undefined in Java 2D image <abbr>API</abbr>.
     */
    UNSIGNED_LONG((short) DataBuffer.TYPE_UNDEFINED, (short) Long.SIZE),

    /**
     * 64 bits signed integer.
     * In <abbr>GDAL</abbr>: {@code GDT_Int64} = 13.
     * This is undefined in Java 2D image <abbr>API</abbr>.
     */
    LONG((short) DataBuffer.TYPE_UNDEFINED, (short) Long.SIZE),

    /**
     * 8 bits signed integer.
     * In <abbr>GDAL</abbr>: {@code GDT_Int8} = 14.
     * This is undefined in Java 2D image <abbr>API</abbr>.
     *
     * @see #UNSIGNED_BYTE
     */
    BYTE((short) DataBuffer.TYPE_UNDEFINED, (short) (Byte.SIZE));

    /**
     * The {@code DataBuffer.TYPE_*} constant equivalent to this <abbr>GDAL</abbr> data type.
     * This is {@link DataBuffer#TYPE_UNDEFINED} if there is no direct Java 2D equivalence.
     */
    final short imageType;

    /**
     * Size in bits of this data type, or 0 if undefined.
     */
    final short numBits;

    /**
     * Creates a new enumeration value.
     *
     * @param  imageType  the {@code DataBuffer.TYPE_*} constant equivalent to this <abbr>GDAL</abbr> data type.
     * @param  numBits    size in bits of this data type, or 0 if undefined.
     */
    private DataType(final short imageType, final short numBits) {
        this.imageType = imageType;
        this.numBits   = numBits;
    }

    /**
     * All values with, for each element, the index equals to the ordinal value.
     */
    private static final DataType[] VALUES = values();

    /**
     * Returns the enumeration constant for the given <abbr>GDAL</abbr> enumeration value.
     *
     * @param  ordinal  the <abbr>GDAL</abbr> enumeration value.
     * @return constant equivalent to the given value, or {@link #UNKNOWN} if the given value is not recognized.
     */
    public static DataType valueOf(final int ordinal) {
        return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : UNKNOWN;
    }

    /**
     * Gets the <abbr>GDAL</abbr> data type corresponding to the given {@code DataBuffer.TYPE_*} value.
     * In case of ambiguity, this enumeration instance is used for resolving the type.
     *
     * @param  type  a {@code DataBuffer.TYPE_*} value.
     * @return the equivalent <abbr>GDAL</abbr> value, or {@link #UNKNOWN} if the given type is not recognized.
     *
     * @see org.apache.sis.image.DataType#forDataBufferType(int)
     */
    public DataType forDataBufferType(final int type) {
        if (type == imageType) {
            // For resolving the `INT` versus `UNSIGNED_INT` ambiguity.
            return this;
        }
        switch (type) {
            case DataBuffer.TYPE_BYTE:   return UNSIGNED_BYTE;
            case DataBuffer.TYPE_USHORT: return UNSIGNED_SHORT;
            case DataBuffer.TYPE_SHORT:  return SHORT;
            case DataBuffer.TYPE_INT:    return INT;
            case DataBuffer.TYPE_FLOAT:  return FLOAT;
            case DataBuffer.TYPE_DOUBLE: return DOUBLE;
            default: return UNKNOWN;
        }
    }
}
