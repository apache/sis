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
package org.apache.sis.geometries.math;

import java.awt.image.RasterFormatException;
import static org.apache.sis.util.privy.Numerics.MAX_INTEGER_CONVERTIBLE_TO_FLOAT;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Numbers;


/**
 * This class is a clone of Apache SIS org.apache.sis.image.DataType.
 * But without image type restrictions.
 *
 * Normalized values definitions can be found at :
 * https://www.khronos.org/opengl/wiki/Normalized_Integer
 * https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_mesh_quantization/README.md
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public enum DataType {

    /**
     * Signed 8-bits data.
     */
    BYTE(0),

    /**
     * Unsigned 8-bits data.
     */
    UBYTE(1),

    /**
     * Signed 16-bits data.
     */
    SHORT(2),

    /**
     * Unsigned 16-bits data.
     */
    USHORT(3),

    /**
     * Signed 32-bits data.
     */
    INT(4),

    /**
     * Unsigned 32-bits data.
     */
    UINT(5),

    /**
     * Signed 64-bits data.
     */
    LONG(6),

    /**
     * Single precision (32-bits) floating point data.
     */
    FLOAT(7),

    /**
     * Double precision (64-bits) floating point data.
     */
    DOUBLE(8),

    /**
     * Signed 8-bits data interpreted as a decimal in range [-1..1]
     */
    NORMALIZED_BYTE(9),

    /**
     * Unsigned 8-bits data interpreted as a decimal in range [0..1]
     */
    NORMALIZED_UBYTE(10),

    /**
     * Signed 16-bits data interpreted as a decimal in range [-1..1]
     */
    NORMALIZED_SHORT(11),

    /**
     * Unsigned 16-bits data interpreted as a decimal in range [0..1]
     */
    NORMALIZED_USHORT(12);

    private final int order;

    /**
     * Creates a new enumeration.
     */
    private DataType(int order) {
        this.order = order;
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
         * give precedence to unsigned values.
         * If a bounds is NaN, fallback on TYPE_FLOAT.
         */
        final DataType type;
        if (min >= -0.5 && max < 0xFF + 0.5) {
            type = UBYTE;
        } else if (min >= Byte.MIN_VALUE - 0.5 && max < 0xFF + 0.5) {
            type = BYTE;
        } else if (min >= -0.5 && max < 0xFFFF + 0.5) {
            type = USHORT;
        } else if (min >= Short.MIN_VALUE - 0.5 && max < Short.MAX_VALUE + 0.5) {
            type = SHORT;
        } else if (min >= - 0.5 && max < 4294967295l + 0.5) {
            type = UINT;
        } else if (min >= Integer.MIN_VALUE - 0.5 && max < Integer.MAX_VALUE + 0.5) {
            type = INT;
        } else if (min >= Long.MIN_VALUE - 0.5 && max < Long.MAX_VALUE + 0.5) {
            type = LONG;
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
            case Numbers.BYTE:    return unsigned ? UBYTE : BYTE;
            case Numbers.SHORT:   return unsigned ? USHORT : SHORT;
            case Numbers.INTEGER: return unsigned ? UINT : INT;
            case Numbers.LONG:    return LONG;
            case Numbers.FLOAT:   return FLOAT;
            case Numbers.DOUBLE:  return DOUBLE;
        }
        throw new RasterFormatException("Unknown data type " + type.getSimpleName());
    }

    /**
     * Returns the size in bits of this data type.
     *
     * @return size in bits of this data type.
     */
    public int size() {
        switch (this) {
            case BYTE :
            case UBYTE :
                return 8;
            case SHORT :
            case USHORT :
                return 16;
            case INT :
            case UINT :
            case FLOAT :
                return 32;
            case LONG :
            case DOUBLE :
                return 64;
            default :
                throw new IllegalStateException("Unexpected type " + this.name());
        }
    }

    /**
     * Returns whether this type is an unsigned integer type.
     * Unsigned types are {@link #UBYTE}, {@link #USHORT} and {@link #UINT}.
     *
     * @return {@code true} if this type is an unsigned integer type.
     */
    public boolean isUnsigned() {
        switch (this) {
            case UBYTE :
            case USHORT :
            case UINT :
                return true;
            default :
                return false;
        }
    }

    /**
     * Returns whether this type is an integer type, signed or not.
     *
     * @return {@code true} if this type is an integer type.
     */
    public boolean isInteger() {
        switch (this) {
            case BYTE :
            case UBYTE :
            case SHORT :
            case USHORT :
            case INT :
            case UINT :
            case LONG :
                return true;
            default :
                return false;
        }
    }

    /**
     * Returns the smallest floating point type capable to store all values of this type
     * without precision lost. This method returns:
     *
     * <ul>
     *   <li>{@link #DOUBLE} if this data type is {@link #DOUBLE}, {@link #INT}, {@link #UINT} or {@link #LONG}.</li>
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
        switch (this) {
            case INT :
            case UINT :
            case LONG :
            case DOUBLE :
                return DOUBLE;
            default :
                return FLOAT;
        }
    }

    /**
     * Get the widest datatype which may contain both types.
     */
    public static DataType largest(DataType type1, DataType type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        if (type1.order > type2.order) {
            DataType t = type1;
            type1 = type2;
            type2 = t;
        }

        return switch (type1) {
            case BYTE ->
                    switch (type2) {
                    case UBYTE -> SHORT;
                    case SHORT -> SHORT;
                    case USHORT -> INT;
                    case INT -> INT;
                    case UINT -> LONG;
                    case LONG -> LONG;
                    case FLOAT -> FLOAT;
                    case DOUBLE -> DOUBLE;
                    case NORMALIZED_BYTE -> FLOAT;
                    case NORMALIZED_UBYTE -> FLOAT;
                    case NORMALIZED_SHORT -> FLOAT;
                    case NORMALIZED_USHORT -> FLOAT;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case UBYTE ->
                    switch (type2) {
                    case SHORT -> SHORT;
                    case USHORT -> USHORT;
                    case INT -> INT;
                    case UINT -> UINT;
                    case LONG -> LONG;
                    case FLOAT -> FLOAT;
                    case DOUBLE -> DOUBLE;
                    case NORMALIZED_BYTE -> FLOAT;
                    case NORMALIZED_UBYTE -> FLOAT;
                    case NORMALIZED_SHORT -> FLOAT;
                    case NORMALIZED_USHORT -> FLOAT;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case SHORT ->
                    switch (type2) {
                    case USHORT -> INT;
                    case INT -> INT;
                    case UINT -> LONG;
                    case LONG -> LONG;
                    case FLOAT -> FLOAT;
                    case DOUBLE -> DOUBLE;
                    case NORMALIZED_BYTE -> FLOAT;
                    case NORMALIZED_UBYTE -> FLOAT;
                    case NORMALIZED_SHORT -> FLOAT;
                    case NORMALIZED_USHORT -> FLOAT;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case USHORT ->
                    switch (type2) {
                    case INT -> INT;
                    case UINT -> UINT;
                    case LONG -> LONG;
                    case FLOAT -> FLOAT;
                    case DOUBLE -> DOUBLE;
                    case NORMALIZED_BYTE -> FLOAT;
                    case NORMALIZED_UBYTE -> FLOAT;
                    case NORMALIZED_SHORT -> FLOAT;
                    case NORMALIZED_USHORT -> FLOAT;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case INT ->
                    switch (type2) {
                    case UINT -> LONG;
                    case LONG -> LONG;
                    case FLOAT -> FLOAT;
                    case DOUBLE -> DOUBLE;
                    case NORMALIZED_BYTE -> FLOAT;
                    case NORMALIZED_UBYTE -> FLOAT;
                    case NORMALIZED_SHORT -> FLOAT;
                    case NORMALIZED_USHORT -> FLOAT;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case UINT ->
                    switch (type2) {
                    case LONG -> LONG;
                    case FLOAT -> FLOAT;
                    case DOUBLE -> DOUBLE;
                    case NORMALIZED_BYTE -> FLOAT;
                    case NORMALIZED_UBYTE -> FLOAT;
                    case NORMALIZED_SHORT -> FLOAT;
                    case NORMALIZED_USHORT -> FLOAT;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case LONG ->
                    switch (type2) {
                    case FLOAT -> DOUBLE;
                    case DOUBLE -> DOUBLE;
                    case NORMALIZED_BYTE -> DOUBLE;
                    case NORMALIZED_UBYTE -> DOUBLE;
                    case NORMALIZED_SHORT -> DOUBLE;
                    case NORMALIZED_USHORT -> DOUBLE;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case FLOAT ->
                    switch (type2) {
                    case DOUBLE -> DOUBLE;
                    case NORMALIZED_BYTE -> FLOAT;
                    case NORMALIZED_UBYTE -> FLOAT;
                    case NORMALIZED_SHORT -> FLOAT;
                    case NORMALIZED_USHORT -> FLOAT;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case DOUBLE ->
                    switch (type2) {
                    case NORMALIZED_BYTE -> DOUBLE;
                    case NORMALIZED_UBYTE -> DOUBLE;
                    case NORMALIZED_SHORT -> DOUBLE;
                    case NORMALIZED_USHORT -> DOUBLE;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case NORMALIZED_BYTE ->
                    switch (type2) {
                    case NORMALIZED_UBYTE -> NORMALIZED_SHORT;
                    case NORMALIZED_SHORT -> NORMALIZED_SHORT;
                    case NORMALIZED_USHORT -> FLOAT;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case NORMALIZED_UBYTE ->
                    switch (type2) {
                    case NORMALIZED_SHORT -> NORMALIZED_SHORT;
                    case NORMALIZED_USHORT -> NORMALIZED_USHORT;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            case NORMALIZED_SHORT ->
                    switch (type2) {
                    case NORMALIZED_USHORT -> FLOAT;
                    default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
                    };
            default -> throw new IllegalArgumentException("Unexpected types " + type1 + " " + type2);
        };

    }
}
