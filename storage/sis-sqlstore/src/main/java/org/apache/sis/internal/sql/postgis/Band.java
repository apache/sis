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
package org.apache.sis.internal.sql.postgis;

import java.awt.image.DataBuffer;
import java.awt.image.RasterFormatException;
import org.apache.sis.util.resources.Errors;


/**
 * Information about a single band in PostGIS WKB raster.
 * Used during reading and writing processes.
 *
 * <p>This class is also the place where WKB constants are defined.
 * Some constants are defined directly in an array, or indirectly as index in an array.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class Band {
    /**
     * If this bit is set, data is to be found on the file system,
     * through the path specified in {@code RASTERDATA}.
     */
    private static final int IS_OFFLINE = 1 << 7;

    /**
     * If this bit is set, the stored nodata value is a true nodata value.
     * Otherwise the value stored as a nodata value should be ignored.
     */
    private static final int HAS_NODATA = 1 << 6;

    /**
     * If this bit is set, all the values of the band are expected to be nodata values.
     * This is a dirty flag.
     */
    private static final int IS_NODATA = 1 << 5;

    /**
     * Mask for the type of pixel values.
     * This mask is applied on {@link #header}.
     */
    private static final int PIXEL_TYPE = 0x0F;

    /**
     * The header byte, which is a bit pattern decomposed by the {@link #PIXEL_TYPE},
     * {@link #IS_OFFLINE}, {@link #HAS_NODATA} and {@link #IS_NODATA} masks.
     */
    final int header;

    /**
     * The "no data" value, or {@code null} if none.
     */
    Number noDataValue;

    /**
     * The raster pixel values as a {@code byte[]}, {@code short[]},
     * {@code int[]}, {@code float[]} or {@code double[]} array.
     */
    Object data;

    /**
     * Creates an initially empty band.
     *
     * @param  header  the byte header of the band.
     */
    Band(final int header) {
        this.header = header;
    }

    /**
     * Creates a band of the given type with the given no-data value.
     *
     * @param  pixelType    WKB code for the type of pixel values.
     * @param  noDataValue  the "no data" value, or {@code null} if none.
     */
    Band(int pixelType, final Number noDataValue) {
        if (noDataValue != null) {
            pixelType |= HAS_NODATA;
        }
        this.header = pixelType;
        this.noDataValue = noDataValue;
    }

    /**
     * Returns a code for the type of pixel values.
     */
    private int getPixelType() {
        return header & PIXEL_TYPE;
    }

    /**
     * Returns {@code true} if data is to be found on the file system,
     * through the path specified in {@code RASTERDATA}.
     */
    final boolean isOffline() {
        return (header & IS_OFFLINE) != 0;
    }

    /**
     * If {@code true}, the stored nodata value is a true nodata value.
     * Otherwise the value stored as a nodata value should be ignored.
     */
    final boolean hasNodata() {
        return (header & HAS_NODATA) != 0;
    }

    /**
     * If {@code true}, all the values of the band are expected to be nodata values.
     * This is a dirty flag.
     */
    final boolean isNodata() {
        return (header & IS_NODATA) != 0;
    }

    /**
     * Returns {@code true} if the data type of this band is an unsigned type.
     */
    final boolean isUnsigned() {
        final int dataType = getDataBufferType();
        return dataType == DataBuffer.TYPE_BYTE || dataType == DataBuffer.TYPE_USHORT ||
               dataType == (DataBuffer.TYPE_INT | OPPOSITE_SIGN);
    }

    /**
     * A bitmask applied to {@link DataBuffer#TYPE_BYTE} or {@link DataBuffer#TYPE_INT}
     * for meaning that the actual data type has a sign opposite to the data buffer type.
     * We use a mask for making easy to ignore this difference when desired.
     *
     * <h4>Limitations</h4>
     * We do not have good support for those types yet.
     * For now, we handle them using their standard counterparts and ignore the sign difference,
     * except if the user asked for a {@code GridCoverage} in which case we raise an exception
     * (because the {@code evaluate(…)} methods would return wrong values).
     * If we need a real support of those types, we should create custom
     * {@code DataBufferSignedByte} and {@code DataBufferUInt} classes.
     */
    static final int OPPOSITE_SIGN = 8;

    /**
     * Type of Java2D buffer for each WKB data type. In the case of {@link DataBuffer#TYPE_BYTE},
     * sample values may be packed in 1, 2 or 4 bits (the {@link DataBuffer} type does not tell).
     * For all integer types except {@code short}, we may have a sign mismatch.
     *
     * @see #BUFFER_TO_PIXEL_TYPE
     */
    @SuppressWarnings("PointlessBitwiseExpression")
    private static final byte[] PIXEL_TO_BUFFER_TYPE = {
        DataBuffer.TYPE_BYTE,                   // Pixel type  0: 1-bit boolean
        DataBuffer.TYPE_BYTE,                   // Pixel type  1: 2-bit unsigned integer
        DataBuffer.TYPE_BYTE,                   // Pixel type  2: 4-bit unsigned integer
        DataBuffer.TYPE_BYTE | OPPOSITE_SIGN,   // Pixel type  3: 8-bit signed integer
        DataBuffer.TYPE_BYTE,                   // Pixel type  4: 8-bit unsigned integer
        DataBuffer.TYPE_SHORT,                  // Pixel type  5: 16-bit signed integer
        DataBuffer.TYPE_USHORT,                 // Pixel type  6: 16-bit unsigned integer
        DataBuffer.TYPE_INT,                    // Pixel type  7: 32-bit signed integer
        DataBuffer.TYPE_INT | OPPOSITE_SIGN,    // Pixel type  8: 32-bit unsigned integer
        DataBuffer.TYPE_UNDEFINED,              // Pixel type  9: unasigned
        DataBuffer.TYPE_FLOAT,                  // Pixel type 10: 32-bit float
        DataBuffer.TYPE_DOUBLE                  // Pixel type 11: 64-bit float
    };

    /**
     * Returns the type of Java2D buffer for this band. In the case of {@link DataBuffer#TYPE_BYTE},
     * sample values may be packed in 1, 2 or 4 bits (the {@link DataBuffer} type does not tell).
     * For all integer types except {@code short}, we may have a sign mismatch.
     *
     * @return buffer type, or {@link DataBuffer#TYPE_UNDEFINED} if the band type is unknown.
     */
    final int getDataBufferType() {
        final int type = getPixelType();
        return (type < PIXEL_TO_BUFFER_TYPE.length) ? PIXEL_TO_BUFFER_TYPE[type] : DataBuffer.TYPE_UNDEFINED;
    }

    /**
     * Maps {@link DataBuffer} types to WKB pixel types.
     * This array is the converse of {@link #PIXEL_TO_BUFFER_TYPE}.
     */
    private static final byte[] BUFFER_TO_PIXEL_TYPE = new byte[8];
    static {
        BUFFER_TO_PIXEL_TYPE[DataBuffer.TYPE_BYTE]   =  4;   //  8-bit unsigned integer
        BUFFER_TO_PIXEL_TYPE[DataBuffer.TYPE_USHORT] =  6;   // 16-bit unsigned integer
        BUFFER_TO_PIXEL_TYPE[DataBuffer.TYPE_SHORT]  =  5;   // 16-bit signed integer
        BUFFER_TO_PIXEL_TYPE[DataBuffer.TYPE_INT]    =  7;   // 32-bit signed integer
        BUFFER_TO_PIXEL_TYPE[DataBuffer.TYPE_FLOAT]  = 10;   // 32-bit float
        BUFFER_TO_PIXEL_TYPE[DataBuffer.TYPE_DOUBLE] = 11;   // 64-bit float
    }

    /**
     * Returns the WKB pixel type for the given {@link DataBuffer} constant.
     *
     * @param  dataType  one of {@link DataBuffer} {@code TYPE_*} constants.
     * @return WKB type for the given data type.
     * @throws RasterFormatException if the given type is invalid.
     */
    static int bufferToPixelType(final int dataType) {
        if (dataType >= 0 && dataType < BUFFER_TO_PIXEL_TYPE.length) {
            return BUFFER_TO_PIXEL_TYPE[dataType];
        }
        throw new RasterFormatException(Errors.format(Errors.Keys.UnsupportedType_1, dataType));
    }

    /**
     * Number of bits for each WKB data type.
     * Indices in the array are WKB pixel types.
     *
     * @see #SIZE_TO_PIXEL_TYPE
     */
    private static final byte[] PIXEL_TYPE_TO_SIZE = {
        1,              // Pixel type  0: 1-bit boolean
        2,              // Pixel type  1: 2-bit unsigned integer
        4,              // Pixel type  2: 4-bit unsigned integer
        Byte.SIZE,      // Pixel type  3: 8-bit signed integer
        Byte.SIZE,      // Pixel type  4: 8-bit unsigned integer
        Short.SIZE,     // Pixel type  5: 16-bit signed integer
        Short.SIZE,     // Pixel type  6: 16-bit unsigned integer
        Integer.SIZE,   // Pixel type  7: 32-bit signed integer
        Integer.SIZE,   // Pixel type  8: 32-bit unsigned integer
        0,              // Pixel type  9: unasigned
        Float.SIZE,     // Pixel type 10: 32-bit float
        Double.SIZE     // Pixel type 11: 64-bit float
    };

    /**
     * Returns the number of bits per sample value in this band.
     *
     * @return number of bits.
     * @throws RasterFormatException if the pixel type is unknown.
     */
    final int getDataTypeSize() {
        final int type = getPixelType();
        if (type < PIXEL_TYPE_TO_SIZE.length) {
            int size = PIXEL_TYPE_TO_SIZE[type];
            if (size != 0) return size;
        }
        throw new RasterFormatException(Errors.format(Errors.Keys.UnsupportedType_1, type));
    }

    /**
     * Maps sample sizes (in bits) to WKB pixel types.
     * Indices in the array are the logarithm in base 2 of the pixel size.
     * This array is the converse of {@link #PIXEL_TYPE_TO_SIZE}.
     */
    private static final byte[] SIZE_TO_PIXEL_TYPE = {
        0,     //  1-bit boolean
        1,     //  2-bit unsigned integer
        2,     //  4-bit unsigned integer
        4,     //  8-bit unsigned integer
        6,     //  16-bit unsigned integer
        8      //  32-bit unsigned integer
    };

    /**
     * Returns the WKB pixel type for an unsigned integer capable to store the given number of bits.
     *
     * @param  size  number of bits.
     * @return WKB type for the given size.
     * @throws RasterFormatException if the given size is invalid.
     */
    static int sizeToPixelType(final int size) {
        if (size > 0) {
            int p = (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(size);
            if ((1 << p) != size) p++;
            if (p >= 0 && p < SIZE_TO_PIXEL_TYPE.length) {
                return SIZE_TO_PIXEL_TYPE[p];
            }
        }
        throw new RasterFormatException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "size", size));
    }

    /**
     * Replaces all "no data" value in the given array by the {@link Float#NaN} value.
     * The {@link #noDataValue} field is set to {@code null} for telling that there is
     * no longer a sentinel value (other than NaN) after this method call.
     */
    final float[] toNaN(final float[] array) {
        if (noDataValue != null) {
            final float r = noDataValue.floatValue();
            if (!Float.isNaN(r)) {
                for (int i=0; i<array.length; i++) {
                    if (array[i] == r) {
                        array[i] = Float.NaN;
                    }
                }
            }
            noDataValue = null;
        }
        return array;
    }

    /**
     * Replaces all "no data" value in the given array by the {@link Double#NaN} value.
     * The {@link #noDataValue} field is set to {@code null} for telling that there is
     * no longer a sentinel value (other than NaN) after this method call.
     */
    final double[] toNaN(final double[] array) {
        if (noDataValue != null) {
            final double r = noDataValue.doubleValue();
            if (!Double.isNaN(r)) {
                for (int i=0; i<array.length; i++) {
                    if (array[i] == r) {
                        array[i] = Double.NaN;
                    }
                }
            }
            noDataValue = null;
        }
        return array;
    }

    /**
     * Returns a string representation of this WKB raster band for debugging purpose.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WKB Raster Band:");
        sb.append("\n- pixel type: ").append(getPixelType());
        sb.append("\n- is offline: ").append(isOffline());
        sb.append("\n- has nodata: ").append(hasNodata());
        sb.append("\n- is no data: ").append(isNodata());
        sb.append("\n- nodata val: ").append(noDataValue);
        return sb.toString();
    }
}
