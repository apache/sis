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
import java.awt.image.SampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.feature.internal.Resources;
import static org.apache.sis.util.privy.Numerics.MAX_INTEGER_CONVERTIBLE_TO_FLOAT;


/**
 * Identification of the primitive type used for storing sample values in an image.
 * This is a type-safe version of the {@code TYPE_*} constants defined in {@link DataBuffer},
 * except that this enumeration distinguishes signed and unsigned 32 bits integers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.1
 */
public enum DataType {
    /**
     * Unsigned 8-bits data.
     * Mapped to {@link DataBuffer#TYPE_BYTE} in Java2D <abbr>API</abbr>.
     */
    BYTE(DataBuffer.TYPE_BYTE, (byte) 0),

    /**
     * Unsigned 16-bits data.
     * Mapped to {@link DataBuffer#TYPE_USHORT} in Java2D <abbr>API</abbr>.
     */
    USHORT(DataBuffer.TYPE_USHORT, (short) 0),

    /**
     * Signed 16-bits data.
     * Mapped to {@link DataBuffer#TYPE_SHORT} in Java2D <abbr>API</abbr>.
     */
    SHORT(DataBuffer.TYPE_SHORT, (short) 0),

    /**
     * Signed 32-bits data.
     * Mapped to {@link DataBuffer#TYPE_INT} in Java2D <abbr>API</abbr>.
     * Note that the sign of the latter Java2D type is ambiguous.
     * See {@link #forDataBufferType(int)} for more information.
     *
     * <p>This type is selected by default for all {@link DataBuffer#TYPE_INT}
     * cases that cannot be resolved as an {@link #UINT} case.</p>
     */
    INT(DataBuffer.TYPE_INT, 0),

    /**
     * Unsigned 32-bits data.
     * Mapped to {@link DataBuffer#TYPE_INT} in Java2D <abbr>API</abbr>.
     * Note that the sign of the latter Java2D type is ambiguous.
     * See {@link #forDataBufferType(int)} for more information.
     *
     * <p>This case is selected when the data are used with any packed sample model:
     * {@link SinglePixelPackedSampleModel} or {@link MultiPixelPackedSampleModel}.</p>
     *
     * @since 1.5
     */
    UINT(DataBuffer.TYPE_INT, 0),

    /**
     * Single precision (32-bits) floating point data.
     * Mapped to {@link DataBuffer#TYPE_FLOAT} in Java2D <abbr>API</abbr>.
     */
    FLOAT(DataBuffer.TYPE_FLOAT, Float.NaN),

    /**
     * Double precision (64-bits) floating point data.
     * Mapped to {@link DataBuffer#TYPE_DOUBLE} in Java2D <abbr>API</abbr>.
     */
    DOUBLE(DataBuffer.TYPE_DOUBLE, Double.NaN);

    /**
     * The data buffer type as one of the {@code DataBuffer.TYPE_*} constants.
     */
    private final int dataType;

    /**
     * The default fill value, which is 0 for integer types and NaN for floating point types.
     * The class of this number is the wrapper type corresponding to the type of sample values.
     *
     * @see #fillValue()
     */
    private final Number fillValue;

    /**
     * Enumeration values for {@code DataBuffer.TYPE_*} constants.
     * The unsigned variant of {@code TYPE_INT} is discarded, keeping the signed variant.
     *
     * @see #forDataBufferType(int)
     */
    private static final DataType[] VALUES = ArraysExt.remove(values(), DataBuffer.TYPE_INT + 1, 1);
    // Initialization trick: ordinal values would be DataBuffer.TYPE_* values is UINT wasn't present.

    /**
     * Creates a new enumeration value.
     */
    private DataType(final int dataType, final Number fillValue) {
        this.dataType  = dataType;
        this.fillValue = fillValue;
    }

    /**
     * Returns the data type of the bands in the given image.
     * This is often the {@linkplain SampleModel#getDataType() storage data type}, but not necessarily.
     * See {@link #forBands(SampleModel)} for more information.
     *
     * @param  image  the image for which to get the band data type.
     * @return type of sample values in the given image (never {@code null}).
     * @throws RasterFormatException if the image does not use a recognized data type.
     */
    public static DataType forBands(final RenderedImage image) {
        return forBands(image.getSampleModel());
    }

    /**
     * Returns the data type of the bands managed by the given the sample model.
     * This is often the {@linkplain SampleModel#getDataType() storage data type}, but not necessarily.
     * For example, if an <abbr>ARGB</abbr> image uses a storage mode where the sample values in the
     * four bands are {@linkplain SinglePixelPackedSampleModel packed in a single 32-bits integer},
     * then this method returns {@link #BYTE} (the type of a single band) rather than {@link #INT}
     * (the type of a whole pixel).
     *
     * @param  sm  the sample model for which to get the band data type.
     * @return type of sample values in the bands managed by the given sample model (never {@code null}).
     * @throws RasterFormatException if the sample model does not use a recognized data type.
     *
     * @since 1.5
     */
    public static DataType forBands(final SampleModel sm) {
        final int type = sm.getDataType();
        if (type > DataBuffer.TYPE_BYTE && type <= DataBuffer.TYPE_INT) {
            int numBits = 0;
            for (int i=sm.getNumBands(); --i >= 0;) {
                numBits = Math.max(numBits, sm.getSampleSize(i));
            }
            if (isUnsigned(sm)) {
                return (numBits <= Byte.SIZE) ? BYTE :
                       (numBits <= Short.SIZE) ? USHORT : UINT;
            } else {
                return (numBits <= Short.SIZE) ? SHORT : INT;
            }
        }
        return forDataBufferType(type);
    }

    /**
     * Returns the smallest data type capable to store the given range of values.
     * If the given range uses a floating point type, there there is a choice:
     *
     * <ul>
     *   <li>If {@code asInteger} is {@code false}, then this method returns
     *       {@link #FLOAT} or {@link #DOUBLE} depending on the range type.</li>
     *   <li>Otherwise, this method treats the floating point values as if they
     *       were integers, with minimum value rounded toward negative infinity
     *       and maximum value rounded toward positive infinity.</li>
     * </ul>
     *
     * @param  range         the range of values.
     * @param  forceInteger  whether to handle floating point values as integers.
     * @return smallest data type for the given range of values.
     */
    public static DataType forRange(final NumberRange<?> range, final boolean forceInteger) {
        final byte nt = Numbers.getEnumConstant(range.getElementType());
        if (!forceInteger) {
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
         * Check most common types first. If the range could be both signed and unsigned,
         * give precedence to unsigned types because it works better with IndexColorModel.
         * If a bounds is NaN, fallback on TYPE_FLOAT.
         */
        final DataType type;
        if (min >= -0.5 && max < 0xFFFFFFFF + 0.5) {
            type = (max < 0xFF + 0.5) ? BYTE : (max < 0xFFFF + 0.5) ? USHORT : UINT;
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
            case Numbers.INTEGER: return unsigned ? UINT   : INT;
            case Numbers.FLOAT:   return FLOAT;
            case Numbers.DOUBLE:  return DOUBLE;
        }
        throw new RasterFormatException(Resources.format(Resources.Keys.UnknownDataType_1, type));
    }

    /**
     * Returns the enumeration value for the given {@link DataBuffer} constant.
     * This method is the converse of {@link #toDataBufferType()}.
     * It is provided for interoperability with Java2D.
     *
     * <h4>32 bit integers</h4>
     * The case of {@link DataBuffer#TYPE_INT} is ambiguous.
     * Whether this type is considered as signed or unsigned depends on the context:
     * <ul>
     *   <li>The sign is indistinguishable when using Java2D <abbr>API</abbr> working on integer values
     *     such as the {@link Raster#getSample(int, int, int) Raster.getSample(…)} method.</li>
     *   <li>When converted to floating point values by Java2D <abbr>API</abbr> such as
     *     {@link Raster#getSampleFloat(int, int, int) Raster.getSampleFloat(…)}, sample values are:
     *     <ul>
     *       <li>Unsigned when the raster uses {@link SinglePixelPackedSampleModel} or {@link MultiPixelPackedSampleModel}.</li>
     *       <li>Signed when the raster uses any other type of sample model.</li>
     *     </ul>
     *   </li>
     *   <li>The type is <em>unsigned</em> when converted to a color by {@link java.awt.image.ComponentColorModel}.</li>
     * </ul>
     *
     * For resolving above ambiguities, consider using {@link #forBands(SampleModel)} instead of this method.
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
     * Returns the enumeration value for the given number of bits.
     * Only the following combinations of argument values are accepted:
     *
     * <table class="sis">
     *   <caption>Valid combinations of argument values</caption>
     *   <tr><th>size</th>                  <th>real</th>           <th>signed</th></tr>
     *   <tr><td>1</td>                     <td>{@code false}</td>  <td>{@code false}</td></tr>
     *   <tr><td>2</td>                     <td>{@code false}</td>  <td>{@code false}</td></tr>
     *   <tr><td>4</td>                     <td>{@code false}</td>  <td>{@code false}</td></tr>
     *   <tr><td>{@value Byte#SIZE}</td>    <td>{@code false}</td>  <td>{@code false}</td></tr>
     *   <tr><td>{@value Short#SIZE}</td>   <td>{@code false}</td>  <td>{@code false} or {@code true}</td></tr>
     *   <tr><td>{@value Integer#SIZE}</td> <td>{@code false}</td>  <td>{@code false} or {@code true}</td></tr>
     *   <tr><td>{@value Float#SIZE}</td>   <td>{@code true}</td>   <td>ignored</td></tr>
     *   <tr><td>{@value Double#SIZE}</td>  <td>{@code true}</td>   <td>ignored</td></tr>
     * </table>
     *
     * @param  size    number of bits as a power of 2 between 1 and {@value Double#SIZE} inclusive.
     * @param  real    {@code true} for floating point numbers or {@code false} for integers.
     * @param  signed  {@code true} for signed numbers of {@code false} for unsigned numbers.
     * @return the data type (never {@code null}) for the given number of bits.
     * @throws RasterFormatException if the combination of argument values is invalid.
     *
     * @since 1.2
     */
    public static DataType forNumberOfBits(final int size, final boolean real, final boolean signed) {
        if (size < 1 || size > Double.SIZE || Integer.lowestOneBit(size) != size) {
            throw new RasterFormatException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "size", size));
        }
        String argument = "real";       // For reporting which argument is inconsistent with sample size.
        boolean value   =  real;
        if (real) {
            switch (size) {
                case Float.SIZE:  return FLOAT;
                case Double.SIZE: return DOUBLE;
            }
        } else if (size <= Integer.SIZE) {
            switch (size) {
                case Short.SIZE:   return signed ? SHORT : USHORT;
                case Integer.SIZE: return signed ? INT   : UINT;
                default: if (!signed) return BYTE; else break;
            }
            argument = "signed";
            value    =  signed;
        }
        throw new RasterFormatException(Resources.format(Resources.Keys.UnsupportedSampleType_3, size, argument, value));
    }

    /**
     * Returns the size in bits of this data type.
     *
     * @return size in bits of this data type.
     */
    public final int size() {
        return DataBuffer.getDataTypeSize(dataType);
    }

    /**
     * Returns the size in bytes of this data type.
     *
     * @return size in bytes of this data type, from 1 to 4 inclusive.
     *
     * @since 1.3
     */
    public final int bytes() {
        return size() >>> 3;        // `size()` is never smaller than 8.
    }

    /**
     * Returns whether this type is an integer type, signed or not.
     * Integer types are {@link #BYTE}, {@link #USHORT}, {@link #SHORT}, {@link #INT} and {@link #UINT}.
     *
     * @return {@code true} if this type is an integer type.
     */
    public final boolean isInteger() {
        return dataType <= DataBuffer.TYPE_INT;
    }

    /**
     * Returns {@code true} if the given sample model uses an integer type.
     * Returns {@code false} if the type is a floating point type or in case
     * of doubt (e.g. for {@link DataBuffer#TYPE_UNDEFINED}).
     *
     * @param  sm  the sample model, or {@code null}.
     * @return whether the given sample model uses an integer type.
     *
     * @since 1.5
     */
    public static boolean isInteger(final SampleModel sm) {
        return (sm != null) && ImageUtilities.isIntegerType(sm.getDataType());
    }

    /**
     * Returns whether this type is an unsigned integer type.
     * Unsigned types are {@link #BYTE}, {@link #USHORT} and {@link #UINT}.
     *
     * @return {@code true} if this type is an unsigned integer type.
     */
    public final boolean isUnsigned() {
        return dataType <= DataBuffer.TYPE_USHORT || this == UINT;
    }

    /**
     * Returns {@code true} if the type of sample values is an unsigned integer type.
     * Returns {@code false} if the type is a floating point type or in case of doubt
     * (e.g. for {@link DataBuffer#TYPE_UNDEFINED}).
     *
     * @param  sm  the sample model, or {@code null}.
     * @return whether the given sample model provides unsigned sample values.
     *
     * @since 1.5
     */
    public static boolean isUnsigned(final SampleModel sm) {
        if (sm != null) {
            final int dataType = sm.getDataType();
            if (dataType >= DataBuffer.TYPE_BYTE) {
                if (dataType <= DataBuffer.TYPE_USHORT) return true;
                if (dataType <= DataBuffer.TYPE_INT) {
                    /*
                     * Typical case: 4 bands (ARGB) stored in a single data element of type `int`.
                     * The javadoc of those classes explain how to unpack the sample values,
                     * and the result is always unsigned.
                     */
                    return (sm instanceof SinglePixelPackedSampleModel) ||
                           (sm instanceof MultiPixelPackedSampleModel);
                }
            }
        }
        return false;
    }

    /**
     * Returns the primitive (signed) variant of this data type.
     * This method returns the value that most closely maps to a Java primitive type of the same number of bits.
     * Since all Java primitive types are signed, this method returns the signed variant of this type except for
     * the special case of {@link #BYTE} (because {@code DataType} does not define signed variant of that type).
     *
     * <p>More specifically, this methods replaces {@link #UINT} by {@link #INT},
     * replaces {@link #USHORT} by {@link #SHORT}, and returns all other types unchanged.
     * The purpose of this method is to simplify the {@code switch} statements with cases
     * restricted to Java primitive types, such as mapping to specific <abbr>NIO</abbr>
     * {@link java.nio.Buffer} subclasses.</p>
     *
     * @return the data type that most closely corresponds to a Java primitive type.
     *
     * @since 1.5
     */
    public final DataType toPrimitive() {
        switch (dataType) {
            default: return this;
            case DataBuffer.TYPE_INT: return INT;
            case DataBuffer.TYPE_USHORT: return SHORT;
        }
    }

    /**
     * Returns the smallest floating point type capable to store all values of this type
     * without precision lost. This method returns:
     *
     * <ul>
     *   <li>{@link #DOUBLE} if this data type is {@link #DOUBLE}, {@link #INT} or {@link #UINT}.</li>
     *   <li>{@link #FLOAT} for all other types.</li>
     * </ul>
     *
     * The promotion of integer values to floating point values is sometimes necessary
     * when the image may contain {@link Float#NaN} values.
     *
     * @return the smallest of {@link #FLOAT} or {@link #DOUBLE} types
     *         which can store all values of this type without any lost.
     */
    public final DataType toFloat() {
        return (dataType < DataBuffer.TYPE_INT || dataType == DataBuffer.TYPE_FLOAT) ? FLOAT : DOUBLE;
    }

    /**
     * Returns the {@link DataBuffer} constant for this enumeration value.
     * This method is the converse of {@link #forDataBufferType(int)}.
     * It is provided for interoperability with Java2D.
     *
     * @return one of the {@link DataBuffer} constants.
     */
    public final int toDataBufferType() {
        return dataType;
    }

    /**
     * Returns the default fill value, which is 0 for integer types and NaN for floating point types.
     * The class of this number is the wrapper class corresponding to the type of sample values,
     * ignoring whether the type is signed or unsigned. For example, for {@link #USHORT},
     * the returned fill value is an instance of the {@link Short} class.
     *
     * @return 0 of NaN in an instance of the wrapper class of the sample values.
     *
     * @since 1.5
     */
    public final Number fillValue() {
        return fillValue;
    }
}
