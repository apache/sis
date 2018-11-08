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
package org.apache.sis.internal.netcdf;

import java.awt.image.DataBuffer;
import org.apache.sis.util.Numbers;


/**
 * The netCDF type of data. Number of bits and endianness are same as in the Java language
 * except {@link #CHAR}, which is defined as an unsigned 8-bits value. This enumeration is
 * related to the netCDF standard as below:
 *
 * <ul>
 *   <li>The netCDF numerical code is the {@link #ordinal()}.</li>
 *   <li>The CDL reserved word is the {@link #name()} is lower case,
 *       except for {@link #UNKNOWN} which is not a valid CDL type.</li>
 * </ul>
 *
 * The unsigned data types are not defined in netCDF classical version. However those data types
 * can be inferred from their signed counterpart if the later have a {@code "_Unsigned = true"}
 * attribute associated to the variable.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public enum DataType {
    /**
     * The enumeration for unknown data type. This is not a valid netCDF type.
     */
    UNKNOWN(Numbers.OTHER, false, false, (byte) 0, DataBuffer.TYPE_UNDEFINED),

    /**
     * 8 bits signed integer (netCDF type 1).
     * Can be made unsigned by assigning the “_Unsigned” attribute to a netCDF variable.
     */
    BYTE(Numbers.BYTE, true, false, (byte) 7, DataBuffer.TYPE_BYTE),

    /**
     * Character type as unsigned 8 bits (netCDF type 2).
     * Encoding can be specified by assigning the “_Encoding” attribute to a netCDF variable.
     */
    CHAR(Numbers.BYTE, false, true, (byte) 2, DataBuffer.TYPE_UNDEFINED),        // NOT Numbers.CHARACTER

    /**
     * 16 bits signed integer (netCDF type 3).
     */
    SHORT(Numbers.SHORT, true, false, (byte) 8, DataBuffer.TYPE_SHORT),

    /**
     * 32 bits signed integer (netCDF type 4).
     * This is also called "long", but that name is deprecated.
     */
    INT(Numbers.INTEGER, true, false, (byte) 9, DataBuffer.TYPE_INT),

    /**
     * 32 bits floating point number (netCDF type 5)
     * This is also called "real".
     */
    FLOAT(Numbers.FLOAT, false, false, (byte) 5, DataBuffer.TYPE_FLOAT),

    /**
     * 64 bits floating point number (netCDF type 6).
     */
    DOUBLE(Numbers.DOUBLE, false, false, (byte) 6, DataBuffer.TYPE_DOUBLE),

    /**
     * 8 bits unsigned integer (netCDF type 7).
     * Not available in netCDF classic format.
     */
    UBYTE(Numbers.BYTE, true, true, (byte) 1, DataBuffer.TYPE_BYTE),

    /**
     * 16 bits unsigned integer (netCDF type 8).
     * Not available in netCDF classic format.
     */
    USHORT(Numbers.SHORT, true, true, (byte) 3, DataBuffer.TYPE_USHORT),

    /**
     * 43 bits unsigned integer (netCDF type 9).
     * Not available in netCDF classic format.
     */
    UINT(Numbers.INTEGER, true, true, (byte) 4, DataBuffer.TYPE_INT),

    /**
     * 64 bits signed integer (netCDF type 10).
     * Not available in netCDF classic format.
     */
    INT64(Numbers.LONG, true, false, (byte) 11, DataBuffer.TYPE_UNDEFINED),

    /**
     * 64 bits unsigned integer (netCDF type 11).
     * Not available in netCDF classic format.
     */
    UINT64(Numbers.LONG, true, true, (byte) 10, DataBuffer.TYPE_UNDEFINED),

    /**
     * Character string (netCDF type 12).
     * Not available in netCDF classic format.
     */
    STRING(Numbers.OTHER, false, false, (byte) 12, DataBuffer.TYPE_UNDEFINED);

    /**
     * Mapping from the netCDF data type to the enumeration used by our {@link Numbers} class.
     */
    public final byte number;

    /**
     * {@code true} for data type that are signed or unsigned integers.
     */
    public final boolean isInteger;

    /**
     * {@code false} for signed data type (the default), or {@code true} for unsigned data type.
     * The OGC netCDF standard version 1.0 does not define unsigned data types. However some data
     * providers attach an {@code "_Unsigned = true"} attribute to the variable.
     */
    public final boolean isUnsigned;

    /**
     * The netCDF code of the data type of opposite sign convention.
     * For example for the {@link #BYTE} data type, this is the netCDF code of {@link #UBYTE}.
     */
    private final byte opposite;

    /**
     * The {@link DataBuffer} constant which most closely represents the "raw" internal data of the variable.
     * This is the value to be returned by {@link java.awt.image.SampleModel#getDataType()} for Java2D rasters
     * created from a variable data. If the variable data type can not be mapped to a Java2D data type, then
     * the raster data type is {@link DataBuffer#TYPE_UNDEFINED}.
     */
    public final int rasterDataType;

    /**
     * Creates a new enumeration.
     */
    private DataType(final byte number, final boolean isInteger, final boolean isUnsigned,
            final byte opposite, final int rasterDataType)
    {
        this.number         = number;
        this.isInteger      = isInteger;
        this.isUnsigned     = isUnsigned;
        this.opposite       = opposite;
        this.rasterDataType = rasterDataType;
    }

    /**
     * Returns the number of bytes for this data type, or 0 if unknown.
     *
     * @return number of bytes for this data type, or 0 if unknown.
     */
    public final int size() {
        switch (number) {
            case Numbers.BYTE:    return Byte.BYTES;
            case Numbers.SHORT:   return Short.BYTES;
            case Numbers.INTEGER: // Same as float
            case Numbers.FLOAT:   return Float.BYTES;
            case Numbers.LONG:    // Same as double
            case Numbers.DOUBLE:  return Double.BYTES;
            default:              return 0;
        }
    }

    /**
     * Returns the signed or unsigned variant of this data type.
     * If this data type does not have the requested variant, then this method returns {@code this}.
     *
     * @param  u  {@code true} for the unsigned variant, or {@code false} for the signed variant.
     * @return the signed or unsigned variant of this data type.
     */
    public final DataType unsigned(final boolean u) {
        return (u == isUnsigned) ? this : valueOf(opposite);
    }

    /**
     * An array of all supported netCDF data types ordered in such a way that
     * {@code VALUES[codeNetCDF - 1]} is the enumeration value for a given netCDF code.
     */
    private static final DataType[] VALUES = values();

    /**
     * Returns the enumeration value for the given netCDF code, or {@link #UNKNOWN} if the given code is unknown.
     *
     * @param  code  the netCDF code.
     * @return enumeration value for the give netCDF code.
     */
    public static DataType valueOf(final int code) {
        return (code >= 0 && code < VALUES.length) ? VALUES[code] : UNKNOWN;
    }
}
