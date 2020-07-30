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
package org.apache.sis.image;

import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.RasterFormatException;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.feature.Resources;

import static org.apache.sis.internal.util.Numerics.MAX_INTEGER_CONVERTIBLE_TO_FLOAT;


/**
 * Identification of the primitive type used for storing sample values in an image.
 * This is a type-safe version of the {@code TYPE_*} constants defined in {@link DataBuffer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public enum DataType {
    /*
     * Enumeration values must be declared in order of increasing `DataBuffer.TYPE_*` constant values,
     * without skiping values. This requirement allow us to get `FOO.ordinal() == DataBuffer.TYPE_FOO`.
     * This matching is verified by DataTypeTest.verifyOrdinalValues().
     */

    /**
     * Unsigned 8-bits data.
     */
    BYTE,

    /**
     * Unsigned 16-bits data.
     */
    USHORT,

    /**
     * Signed 16-bits data.
     */
    SHORT,

    /**
     * Signed 32-bits data.
     */
    INT,

    /**
     * Single precision (32-bits) floating point data.
     */
    FLOAT,

    /**
     * Double precision (64-bits) floating point data.
     */
    DOUBLE;

    /**
     * All enumeration values, cached for avoiding to recreate this array
     * on every {@link #forDataBufferType(int)} call.
     */
    private static final DataType[] VALUES = values();

    /**
     * Creates a new enumeration.
     */
    private DataType() {
    }

    /**
     * Returns the data type of the given image.
     *
     * @param  image  the image for which to get the type.
     * @return type of the given image (never {@code null}).
     * @throws RasterFormatException if the image does not use a recognized data type.
     */
    public static DataType of(final RenderedImage image) {
        return forDataBufferType(ImageUtilities.getDataType(image));
    }

    /**
     * Returns the data type of the given raster.
     *
     * @param  raster  the raster for which to get the type.
     * @return type of the given raster (never {@code null}).
     * @throws RasterFormatException if the raster does not use a recognized data type.
     */
    public static DataType of(final Raster raster) {
        return forDataBufferType(ImageUtilities.getDataType(raster));
    }

    /**
     * Returns the smallest data type capable to store the given range of values.
     * If the given range uses a floating point type, there there is a choice:
     *
     * <ul>
     *   <li>If {@code asInteger} is {@code false}, then this method returns
     *       {@link #FLOAT} or {@link #DOUBLE} depending on the range type.</li>
     *   <li>Otherwise this method treats the floating point values as if they
     *       were integers, with minimum value rounded toward negative infinity
     *       and maximum value rounded toward positive infinity.</li>
     * </ul>
     *
     * @param  range      the range of values.
     * @param  asInteger  whether to handle floating point values as integers.
     * @return smallest data type for the given range of values.
     */
    public static DataType forRange(final NumberRange<?> range, final boolean asInteger) {
        ArgumentChecks.ensureNonNull("range", range);
        final byte nt = Numbers.getEnumConstant(range.getElementType());
        if (!asInteger) {
            if (nt >= Numbers.DOUBLE)   return DOUBLE;
            if (nt >= Numbers.FRACTION) return FLOAT;
        }
        final double min = range.getMinDouble();
        final double max = range.getMaxDouble();
        if (nt < Numbers.BYTE || nt > Numbers.FLOAT || nt == Numbers.LONG) {
            /*
             * Value type is long, double, BigInteger, BigDecimal or unknown type.
             * If conversions to 32 bits integers would lost integer digits, or if
             * a bound is NaN, stick to the most conservative data buffer type.
             */
            if (!(min >= -MAX_INTEGER_CONVERTIBLE_TO_FLOAT - 0.5 &&
                  max <   MAX_INTEGER_CONVERTIBLE_TO_FLOAT + 0.5))
            {
                return DOUBLE;
            }
        }
        /*
         * Check most common types first. If the range could be both signed and unsigned short,
         * give precedence to unsigned short because it works better with IndexColorModel.
         * If a bounds is NaN, fallback on TYPE_FLOAT.
         */
        final DataType type;
        if (min >= -0.5 && max < 0xFFFF + 0.5) {
            type = (max < 0xFF + 0.5) ? BYTE : USHORT;
        } else if (min >= Short.MIN_VALUE - 0.5 && max < Short.MAX_VALUE + 0.5) {
            type = SHORT;
        } else if (min >= Integer.MIN_VALUE - 0.5 && max < Integer.MAX_VALUE + 0.5) {
            type = INT;
        } else {
            type = FLOAT;
        }
        return type;
    }

    /**
     * Returns the data type for the given primitive type. The given {@code type} should be a primitive
     * type such as {@link Short#TYPE}, but wrappers class such as {@code Short.class} are also accepted.
     *
     * @param  type      the primitive type or its wrapper class.
     * @param  unsigned  whether the type should be considered unsigned.
     * @return the data type (never {@code null}) for the given primitive type.
     * @throws RasterFormatException if the given type is not a recognized.
     */
    public static DataType forPrimitiveType(final Class<?> type, final boolean unsigned) {
        switch (Numbers.getEnumConstant(type)) {
            case Numbers.BYTE:    return unsigned ? BYTE   : SHORT;
            case Numbers.SHORT:   return unsigned ? USHORT : SHORT;
            case Numbers.INTEGER: if (unsigned) break; else return INT;
            case Numbers.FLOAT:   return FLOAT;
            case Numbers.DOUBLE:  return DOUBLE;
        }
        throw new RasterFormatException(Resources.format(Resources.Keys.UnknownDataType_1, type));
    }

    /**
     * Returns the enumeration value for the given {@link DataBuffer} constant.
     *
     * @param  type  one of {@code DataBuffer.TYPE_*} constants.
     * @return the data type (never {@code null}) for the given data buffer type.
     * @throws RasterFormatException if the given type is not a recognized {@code DataBuffer.TYPE_*} constant.
     */
    public static DataType forDataBufferType(final int type) {
        if (type >= 0 && type < VALUES.length) {
            return VALUES[type];
        } else {
            throw new RasterFormatException(Resources.format(Resources.Keys.UnknownDataType_1, type));
        }
    }

    /**
     * Returns the size in bits of this data type.
     *
     * @return size in bits of this data type.
     */
    public int size() {
        return DataBuffer.getDataTypeSize(ordinal());
    }

    /**
     * Returns whether this type is an unsigned integer type.
     * Unsigned types are {@link #BYTE} and {@link #USHORT}.
     *
     * @return {@code true} if this type is an unsigned integer type.
     */
    public boolean isUnsigned() {
        return ordinal() <= DataBuffer.TYPE_USHORT;
    }

    /**
     * Returns whether this type is an integer type, signed or not.
     * Integer types are {@link #BYTE}, {@link #USHORT}, {@link #SHORT} and {@link #INT}.
     *
     * @return {@code true} if this type is an integer type.
     */
    public boolean isInteger() {
        return ordinal() <= DataBuffer.TYPE_INT;
    }

    /**
     * Returns the smallest floating point type capable to store all values of this type
     * without precision lost. This method returns:
     *
     * <ul>
     *   <li>{@link #DOUBLE} if this data type is {@link #DOUBLE} or {@link #INT}.</li>
     *   <li>{@link #FLOAT} for all other types.</li>
     * </ul>
     *
     * The promotion of integer values to floating point values is sometime necessary
     * when the image may contain {@link Float#NaN} values.
     *
     * @return the smallest of {@link #FLOAT} or {@link #DOUBLE} types
     *         which can store all values of this type without any lost.
     */
    public DataType toFloat() {
        final int type = ordinal();
        return (type < DataBuffer.TYPE_INT || type == DataBuffer.TYPE_FLOAT) ? FLOAT : DOUBLE;
    }
}
