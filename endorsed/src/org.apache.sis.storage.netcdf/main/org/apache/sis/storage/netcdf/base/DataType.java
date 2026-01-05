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
package org.apache.sis.storage.netcdf.base;

import java.awt.image.DataBuffer;
import org.apache.sis.math.NumberType;
import org.apache.sis.math.Vector;


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
 * The unsigned data types are not defined in netCDF classical version. However, those data types
 * can be inferred from their signed counterpart if the latter have a {@code "_Unsigned = true"}
 * attribute associated to the variable.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum DataType {
    /**
     * The enumeration for unknown data type. This is not a valid netCDF type.
     */
    UNKNOWN(NumberType.VOID, Object.class, false, false, (byte) 0, null),

    /**
     * 8 bits signed integer (netCDF type 1).
     * Can be made unsigned by assigning the “_Unsigned” attribute to a netCDF variable.
     */
    BYTE(NumberType.BYTE, Byte.class, true, false, (byte) 7, org.apache.sis.image.DataType.BYTE),

    /**
     * Character type as unsigned 8 bits (netCDF type 2).
     * Encoding can be specified by assigning the “_Encoding” attribute to a netCDF variable.
     */
    CHAR(NumberType.BYTE, Character.class, false, true, (byte) 2, null),   // NOT Numbers.CHARACTER

    /**
     * 16 bits signed integer (netCDF type 3).
     */
    SHORT(NumberType.SHORT, Short.class, true, false, (byte) 8, org.apache.sis.image.DataType.SHORT),

    /**
     * 32 bits signed integer (netCDF type 4).
     * This is also called "long", but that name is deprecated.
     */
    INT(NumberType.INTEGER, Integer.class, true, false, (byte) 9, org.apache.sis.image.DataType.INT),

    /**
     * 32 bits floating point number (netCDF type 5)
     * This is also called "real".
     */
    FLOAT(NumberType.FLOAT, Float.class, false, false, (byte) 5, org.apache.sis.image.DataType.FLOAT),

    /**
     * 64 bits floating point number (netCDF type 6).
     */
    DOUBLE(NumberType.DOUBLE, Double.class, false, false, (byte) 6, org.apache.sis.image.DataType.DOUBLE),

    /**
     * 8 bits unsigned integer (netCDF type 7).
     * Not available in netCDF classic format.
     */
    UBYTE(NumberType.BYTE, Short.class, true, true, (byte) 1, org.apache.sis.image.DataType.BYTE),

    /**
     * 16 bits unsigned integer (netCDF type 8).
     * Not available in netCDF classic format.
     */
    USHORT(NumberType.SHORT, Integer.class, true, true, (byte) 3, org.apache.sis.image.DataType.USHORT),

    /**
     * 32 bits unsigned integer (netCDF type 9).
     * Not available in netCDF classic format.
     */
    UINT(NumberType.INTEGER, Long.class, true, true, (byte) 4, org.apache.sis.image.DataType.UINT),

    /**
     * 64 bits signed integer (netCDF type 10).
     * Not available in netCDF classic format.
     */
    INT64(NumberType.LONG, Long.class, true, false, (byte) 11, null),

    /**
     * 64 bits unsigned integer (netCDF type 11).
     * Not available in netCDF classic format.
     */
    UINT64(NumberType.LONG, Number.class, true, true, (byte) 10, null),

    /**
     * Character string (netCDF type 12).
     * Not available in netCDF classic format.
     */
    STRING(NumberType.VOID, String.class, false, false, (byte) 12, null);

    /**
     * Mapping from the netCDF data type to the enumeration used by Apache <abbr>SIS</abbr>.
     */
    public final NumberType number;

    /**
     * {@code true} for data type that are signed or unsigned integers.
     */
    public final boolean isInteger;

    /**
     * {@code false} for signed data type (the default), or {@code true} for unsigned data type.
     * The OGC netCDF standard version 1.0 does not define unsigned data types. However, some data
     * providers attach an {@code "_Unsigned = true"} attribute to the variable.
     */
    public final boolean isUnsigned;

    /**
     * The netCDF code of the data type of opposite sign convention.
     * For example, for the {@link #BYTE} data type, this is the netCDF code of {@link #UBYTE}.
     */
    private final byte opposite;

    /**
     * Wrapper of {@link DataBuffer} constant which most closely represents the "raw" internal data of the variable.
     * This wraps the value to be returned by {@link java.awt.image.SampleModel#getDataType()} for Java2D rasters
     * created from a variable data. If the variable data type cannot be mapped to a Java2D data type, then the
     * raster data type is {@code null}.
     */
    public final org.apache.sis.image.DataType rasterDataType;

    /**
     * The smallest Java wrapper class that can hold the values. Values are always signed. If {@link #isUnsigned}
     * is {@code true}, then a wider type is used for holding the large unsigned values. For example, the 16 bits
     * signed integer type is used for holding 8 bits unsigned integers.
     */
    private final Class<?> classe;

    /**
     * Creates a new enumeration value.
     */
    private DataType(final NumberType number, final Class<?> classe, final boolean isInteger, final boolean isUnsigned,
                     final byte opposite, final org.apache.sis.image.DataType rasterDataType)
    {
        this.number         = number;
        this.classe         = classe;
        this.isInteger      = isInteger;
        this.isUnsigned     = isUnsigned;
        this.opposite       = opposite;
        this.rasterDataType = rasterDataType;
    }

    /**
     * Returns the Java class to use for storing the values.
     *
     * @param  vector  {@code true} for a vector object, or {@code false} for a scalar object.
     */
    final Class<?> getClass(final boolean vector) {
        if (vector) {
            if (classe == Character.class) {
                return String.class;
            } else if (Number.class.isAssignableFrom(classe)) {
                return Vector.class;
            } else {
                return Object.class;
            }
        } else {
            return classe;
        }
    }

    /**
     * Returns the number of bytes for this data type, or 0 if unknown.
     *
     * @return number of bytes for this data type, or 0 if unknown.
     */
    public final int size() {
        switch (number) {
            case BYTE:    return Byte.BYTES;
            case SHORT:   return Short.BYTES;
            case INTEGER: // Same as float
            case FLOAT:   return Float.BYTES;
            case LONG:    // Same as double
            case DOUBLE:  return Double.BYTES;
            default:      return 0;
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
