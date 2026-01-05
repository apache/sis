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
package org.apache.sis.storage.geotiff.reader;

import java.util.Arrays;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.imageio.plugins.tiff.TIFFTag;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.Fraction;
import org.apache.sis.math.Vector;
import org.apache.sis.util.resources.Errors;


/**
 * The types of values in a TIFF header. Provides also some support for reading a value of a given type.
 *
 * <p><b>Note on naming convention:</b>
 * the values in this enumeration are not necessarily named as the TIFF type names.
 * This enumeration rather match the Java primitive type names.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum Type {
    /**
     * An 8-bits byte that may contain anything, depending on the definition of the field.
     * <ul>
     *   <li>TIFF name: {@code UNDEFINED}</li>
     *   <li>TIFF code: 7</li>
     * </ul>
     */
    UNDEFINED(TIFFTag.TIFF_UNDEFINED, Byte.BYTES, false) {
        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            throw new UnsupportedOperationException(name());
        }

        /** Unknown value (used for reporting native metadata only). */
        @Override public Object readAsObject(final ChannelDataInput input, final long count) throws IOException {
            return null;
        }
    },

    /**
     * An 8-bits signed (twos-complement) integer.
     * <ul>
     *   <li>TIFF name: {@code SBYTE}</li>
     *   <li>TIFF code: 6</li>
     * </ul>
     */
    BYTE(TIFFTag.TIFF_SBYTE, Byte.BYTES, false) {
        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readByte();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readByte(), count);
            }
            return value;
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return input.readBytes(count);
        }
    },

    /**
     * A 8-bits unsigned integer.
     * <ul>
     *   <li>TIFF name: {@code BYTE}</li>
     *   <li>TIFF code: 1</li>
     * </ul>
     */
    UBYTE(TIFFTag.TIFF_BYTE, Byte.BYTES, true) {
        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readUnsignedByte();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readUnsignedByte(), count);
            }
            return value;
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return input.readBytes(count);
        }
    },

    /**
     * A 16-bits (2-bytes) signed (twos-complement) integer.
     * <ul>
     *   <li>TIFF name: {@code SSHORT}</li>
     *   <li>TIFF code: 8</li>
     * </ul>
     */
    SHORT(TIFFTag.TIFF_SSHORT, Short.BYTES, false) {
        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readShort();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readShort(), count);
            }
            return value;
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return input.readShorts(count);
        }
    },

    /**
     * A 16-bits (2-bytes) unsigned integer.
     * <ul>
     *   <li>TIFF name: {@code SHORT}</li>
     *   <li>TIFF code: 3</li>
     * </ul>
     */
    USHORT(TIFFTag.TIFF_SHORT, Short.BYTES, true) {
        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readUnsignedShort();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readUnsignedShort(), count);
            }
            return value;
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return input.readShorts(count);
        }
    },

    /**
     * A 32-bits (4-bytes) signed (twos-complement) integer.
     * <ul>
     *   <li>TIFF name: {@code SLONG}</li>
     *   <li>TIFF code: 9</li>
     * </ul>
     */
    INT(TIFFTag.TIFF_SLONG, Integer.BYTES, false) {
        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readInt();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readInt(), count);
            }
            return value;
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return input.readInts(count);
        }
    },

    /**
     * 32-bits (4-bytes) unsigned integer.
     * <ul>
     *   <li>TIFF name: {@code LONG}</li>
     *   <li>TIFF code: 4</li>
     * </ul>
     */
    UINT(TIFFTag.TIFF_LONG, Integer.BYTES, true) {
        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readUnsignedInt();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readUnsignedInt(), count);
            }
            return value;
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return input.readInts(count);
        }
    },

    /**
     * A 64-bits (8-bytes) signed (twos-complement) integer.
     * <ul>
     *   <li>TIFF code: 17</li>
     * </ul>
     */
    LONG(17, Long.BYTES, false) {
        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readLong();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readLong(), count);
            }
            return value;
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return input.readLongs(count);
        }
    },

    /**
     * A 64-bits (8-bytes) unsigned integer.
     * <ul>
     *   <li>TIFF code: 16</li>
     * </ul>
     */
    ULONG(16, Long.BYTES, true) {
        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readLong();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readLong(), count);
            }
            if (value >= 0) {
                return value;
            }
            throw new ArithmeticException(canNotConvert(Long.toUnsignedString(value)));
        }

        @Override public double readAsDouble(final ChannelDataInput input, final long count) throws IOException {
            return Numerics.toUnsignedDouble(readAsLong(input, count));
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return input.readLongs(count);
        }
    },

    /**
     * Single precision (4-bytes) IEEE format.
     * <ul>
     *   <li>TIFF name: {@code FLOAT}</li>
     *   <li>TIFF code: 11</li>
     * </ul>
     */
    FLOAT(TIFFTag.TIFF_FLOAT, Float.BYTES, false) {
        private float readAsFloat(final ChannelDataInput input, final long count) throws IOException {
            final float value = input.readFloat();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readFloat(), count);
            }
            return value;
        }

        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            final float value = readAsFloat(input, count);
            final long r = (long) value;
            if (r == value) {
                return r;
            }
            throw new ArithmeticException(canNotConvert(Float.toString(value)));
        }

        @Override public double readAsDouble(final ChannelDataInput input, final long count) throws IOException {
            return DecimalFunctions.floatToDouble(readAsFloat(input, count));
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return input.readFloats(count);
        }
    },

    /**
     * Double precision (8-bytes) IEEE format.
     * <ul>
     *   <li>TIFF name: {@code DOUBLE}</li>
     *   <li>TIFF code: 12</li>
     * </ul>
     */
    DOUBLE(TIFFTag.TIFF_DOUBLE, Double.BYTES, false) {
        @Override public double readAsDouble(final ChannelDataInput input, final long count) throws IOException {
            final double value = input.readDouble();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readDouble(), count);
            }
            return value;
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return input.readDoubles(count);
        }
    },

    /**
     * Two signed integers: the first represents the numerator of a fraction; the second, the denominator.
     * <ul>
     *   <li>TIFF name: {@code SRATIONAL}</li>
     *   <li>TIFF code: 10</li>
     * </ul>
     */
    RATIONAL(TIFFTag.TIFF_SRATIONAL, (2*Integer.BYTES), false) {
        @Override public double readAsDouble(final ChannelDataInput input, final long count) throws IOException {
            return readFraction(input, count).doubleValue();
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return readFractions(input, count);
        }

        @Override public Object readAsObject(final ChannelDataInput input, final long count) throws IOException {
            return (count == 1) ? readFraction(input, count) : super.readAsObject(input, count);
        }
    },

    /**
     * Two unsigned integers: the first represents the numerator of a fraction; the second, the denominator.
     * <ul>
     *   <li>TIFF name: {@code RATIONAL}</li>
     *   <li>TIFF code: 5</li>
     * </ul>
     */
    URATIONAL(TIFFTag.TIFF_RATIONAL, (2*Integer.BYTES), true) {
        @Override public double readAsDouble(final ChannelDataInput input, final long count) throws IOException {
            return readFraction(input, count).doubleValue();
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return readFractions(input, count);
        }

        @Override public Object readAsObject(final ChannelDataInput input, final long count) throws IOException {
            return (count == 1) ? readFraction(input, count) : super.readAsObject(input, count);
        }
    },

    /**
     * 8-bits byte that contains a 7-bit ASCII code. In a string of ASCII characters, the last byte must be NUL
     * (binary zero). The string length (including the NUL byte) is the {@code count} field before the string.
     * NUL bytes may also appear in the middle of the string for separating its content into multi-strings.
     * <ul>
     *   <li>TIFF name: {@code ASCII}</li>
     *   <li>TIFF code: 2</li>
     * </ul>
     */
    ASCII(TIFFTag.TIFF_ASCII, Byte.BYTES, false) {
        @Override public String[] readAsStrings(final ChannelDataInput input, final long length, final Charset charset) throws IOException {
            final byte[] chars = input.readBytes(Math.toIntExact(length));
            String[] lines = new String[1];                     // We will usually have exactly one string.
            int count = 0, lower = 0;
            for (int i=0; i<chars.length; i++) {
                if (chars[i] == 0) {
                    if (count >= lines.length) {
                        lines = Arrays.copyOf(lines, 2*count);
                    }
                    lines[count++] = new String(chars, lower, i-lower, charset);
                }
            }
            return ArraysExt.resize(lines, count);
        }

        /** Returns the singleton string, or {@code null} if none. */
        private String readAsString(final ChannelDataInput input, final long count, final boolean mandatory) throws IOException {
            final String[] lines = readAsStrings(input, count, StandardCharsets.US_ASCII);
            if (lines.length != 0) {
                final String value = lines[0];
                int i = 1;
                do if (i >= lines.length) return value;
                while (value.equals(lines[i++]));
            } else if (!mandatory) {
                return null;
            }
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, 1, lines.length));
        }

        @Override public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
            return Long.parseLong(readAsString(input, count, true));
        }

        @Override public double readAsDouble(final ChannelDataInput input, final long count) throws IOException {
            String text = readAsString(input, count, false);
            if (text == null || (text = text.trim()).isEmpty() || text.equalsIgnoreCase("NaN")) {
                return Double.NaN;
            }
            return Double.parseDouble(text);
        }

        @Override public Object readAsArray(final ChannelDataInput input, final int count) throws IOException {
            return readAsStrings(input, count, StandardCharsets.US_ASCII);
        }

        @Override public Object readAsObject(final ChannelDataInput input, final long count) throws IOException {
            return readAsString(input, count, false);
        }
    };

    /**
     * The TIFF numerical code for this type.
     */
    final int code;

    /**
     * The size of this type, in number of bytes.
     */
    public final int size;

    /**
     * Whether this type is an unsigned integer.
     */
    final boolean isUnsigned;

    /**
     * Creates a new enumeration
     */
    private Type(final int code, final int size, final boolean isUnsigned) {
        this.code = code;
        this.size = size;
        this.isUnsigned = isUnsigned;
    }

    /**
     * All types known to this enumeration in an array where the index is the GeoTIFF numerical code.
     */
    private static final Type[] FROM_CODES = new Type[19];
    static {
        for (final Type type : values()) {
            FROM_CODES[type.code] = type;
        }
        FROM_CODES[13] = UINT;      // IFD type.
        FROM_CODES[18] = ULONG;     // IFD8 type.
    }

    /**
     * Returns the type for the given GeoTIFF code, or {@code null} if the given type is unknown.
     *
     * @param  code  the GeoTIFF numerical code.
     * @return the enumeration value that represent the given type, or {@code null} if unknown.
     */
    public static Type valueOf(final int code) {
        return (code >= 0 && code < FROM_CODES.length) ? FROM_CODES[code] : null;
    }

    /**
     * Reads the next rational number. This is either a single value or a member of a vector.
     */
    private Number nextFraction(final ChannelDataInput input) throws IOException {
        if (isUnsigned) {
            return Numerics.fraction(input.readUnsignedInt(), input.readUnsignedInt());
        } else {
            return new Fraction(input.readInt(), input.readInt());
        }
    }

    /**
     * Reads an array of rational numbers.
     * This is a helper method for {@link #RATIONAL} and {@link #URATIONAL} types.
     */
    final Number[] readFractions(final ChannelDataInput input, int count) throws IOException {
        final Number[] values = new Number[count];
        for (int i=0; i<count; i++) {
            values[i] = nextFraction(input);
        }
        return values;
    }

    /**
     * Reads a rational number, making sure that the value is unique if repeated.
     * This is a helper method for {@link #RATIONAL} and {@link #URATIONAL} types.
     */
    final Number readFraction(final ChannelDataInput input, final long count) throws IOException {
        final Number value = nextFraction(input);
        for (long i=1; i<count; i++) {
            ensureSingleton(value.doubleValue(), nextFraction(input).doubleValue(), count);
        }
        return value;
    }

    /**
     * Invoked by {@code read(…)} method implementations for verifying that the {@code count} argument value is 1.
     * All read methods other than {@code readAsArray(…)} expect exactly one value, except methods in {@link #ASCII}
     * enumeration value which are treated differently.
     *
     * <p>While exactly one value is expected, we are tolerant to longer arrays provided that all values are the
     * same. This is seen in practice where a value expected to apply to the image is repeated for each band.</p>
     *
     * @param  previous  the previous value.
     * @param  actual    the actual value.
     * @param  count     the number of values to read.
     * @throws IllegalArgumentException if {@code count} does not have the expected value.
     */
    private static void ensureSingleton(final long previous, final long actual, final long count) {
        if (previous != actual) {
            // Even if the methods did not expected an array in argument, we are conceptually reading an array.
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, 1, count));
        }
    }

    /**
     * Same as {@link #ensureSingleton(long, long, long)} but with floating-point values.
     */
    private static void ensureSingleton(final double previous, final double actual, final long count) {
        if (Double.doubleToLongBits(previous) != Double.doubleToLongBits(actual)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, 1, count));
        }
    }

    /**
     * Formats an error message for a value that cannot be converted.
     */
    final String canNotConvert(final String value) {
        return Errors.format(Errors.Keys.CanNotConvertValue_2, value, name());
    }

    /**
     * Reads a single value and returns it as a signed {@code short} type, performing conversion if needed.
     * This method should be invoked when the caller expects a single value.
     *
     * @param  input  the input from where to read the value.
     * @param  count  the number of values (normally exactly 1).
     * @return the value as a {@code short}.
     * @throws IOException if an error occurred while reading the stream.
     * @throws NumberFormatException if the value was stored in ASCII and cannot be parsed.
     * @throws ArithmeticException if the value cannot be represented in the Java signed {@code short} type.
     * @throws IllegalArgumentException if the value is not a singleton.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public final short readAsShort(final ChannelDataInput input, final long count) throws IOException {
        final long value = readAsLong(input, count);
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return (short) value;
        }
        throw new ArithmeticException(canNotConvert(Long.toString(value)));
    }

    /**
     * Reads a single value and returns it as a signed {@code int} type, performing conversion if needed.
     * This method should be invoked when the caller expects a single value.
     *
     * @param  input  the input from where to read the value.
     * @param  count  the number of values (normally exactly 1).
     * @return the value as an {@code int}.
     * @throws IOException if an error occurred while reading the stream.
     * @throws NumberFormatException if the value was stored in ASCII and cannot be parsed.
     * @throws ArithmeticException if the value cannot be represented in the Java signed {@code int} type.
     * @throws IllegalArgumentException if the value is not a singleton.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public final int readAsInt(final ChannelDataInput input, final long count) throws IOException {
        final long value = readAsLong(input, count);
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return (int) value;
        }
        throw new ArithmeticException(canNotConvert(Long.toString(value)));
    }

    /**
     * Reads a single value which is expected to be positive. A negative value may be an encoding error in the
     * big TIFF file, or if it was really the intended value then something greater than what we can support.
     *
     * @throws IOException if an error occurred while reading the stream.
     * @throws NumberFormatException if the value was stored in ASCII and cannot be parsed.
     * @throws ArithmeticException if the value cannot be represented in the Java signed {@code long} type.
     * @throws IllegalArgumentException if the value is not a singleton.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public final long readAsUnsignedLong(final ChannelDataInput input, final long count) throws IOException {
        final long value = readAsLong(input, count);
        if (value >= 0) {
            return value;
        }
        throw new ArithmeticException(canNotConvert(Long.toUnsignedString(value)));
    }

    /**
     * Reads a single value and returns it as a {@code long} type, performing conversion if needed.
     * This method should be invoked when the caller expect a single value.
     *
     * <p>If the value is an ASCII type, then this method will parse the text as a number.
     * This support of ASCII type is required for supporting the encoding used in the tags
     * added by GDAL.</p>
     *
     * @param  input  the input from where to read the value.
     * @param  count  the number of values (normally exactly 1).
     * @return the value as a {@code long}.
     * @throws IOException if an error occurred while reading the stream.
     * @throws NumberFormatException if the value was stored in ASCII and cannot be parsed.
     * @throws ArithmeticException if the value cannot be represented in the Java signed {@code long} type.
     * @throws IllegalArgumentException if the value is not a singleton.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public long readAsLong(final ChannelDataInput input, final long count) throws IOException {
        // All enum MUST override one of `readAsLong(…)` or `readAsDouble(…)` methods.
        final double value = readAsDouble(input, count);
        final long r = (long) value;
        if (r == value) {
            return r;
        }
        throw new ArithmeticException(canNotConvert(Double.toString(value)));
    }

    /**
     * Reads a single value and returns it as a {@code double} type, performing conversion if needed.
     * This method should be invoked when the caller expect a single value.
     *
     * <p>If the value is an ASCII type, then this method will parse the text as a number.
     * This support of ASCII type is required for supporting the encoding used in the tags
     * added by GDAL.</p>
     *
     * @param  input  the input from where to read the value.
     * @param  count  the number of values (normally exactly 1).
     * @return the value as a {@code double}.
     * @throws IOException if an error occurred while reading the stream.
     * @throws NumberFormatException if the value was stored in ASCII and cannot be parsed.
     * @throws IllegalArgumentException if the value is not a singleton.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public double readAsDouble(ChannelDataInput input, long count) throws IOException {
        // All enum MUST override one of `readAsLong(…)` or `readAsDouble(…)` methods.
        return readAsLong(input, count);
    }

    /**
     * Reads the value as strings. There is usually exactly one string, but an arbitrary amount is allowed.
     * The default implementation assumes that the vector contains numerical data, which is the case of all
     * types except {@link #ASCII}. This method is overridden for handling any text in the {@code ASCII} case.
     *
     * @param  input    the input from where to read the value.
     * @param  length   the string length, including the final NUL byte.
     * @param  charset  the character encoding (normally US ASCII).
     * @return the value as a string.
     * @throws IOException if an error occurred while reading the stream.
     * @throws ArithmeticException if the given length is too large.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public String[] readAsStrings(final ChannelDataInput input, final long length, final Charset charset) throws IOException {
        final String[] s = new String[Math.toIntExact(length)];
        for (int i=0; i<s.length; i++) {
            final double value = readAsDouble(input, 1);
            final long r = (long) value;
            s[i] = (r == value) ? String.valueOf(r) : String.valueOf(value);
        }
        return s;
    }

    /**
     * Returns the value as a {@link Vector}, a {@link Number} (only for fractions) or a {@link String} instance.
     * This method should be overridden by all enumeration values that do no override
     * {@link #readAsArray(ChannelDataInput, int)}.
     *
     * @param  input  the input from where to read the values.
     * @param  count  the number of values.
     * @return the value as a Java array or a {@link String}, or {@code null} if undefined.
     * @throws IOException if an error occurred while reading the stream.
     */
    public Object readAsObject(ChannelDataInput input, long count) throws IOException {
        return readAsVector(input, count);
    }

    /**
     * Reads an arbitrary number of values as a Java array.
     * This is usually (but not necessarily) an array of primitive type.
     * It may be unsigned values packed in their signed counterpart.
     *
     * @param  input  the input from where to read the values.
     * @param  count  the number of values.
     * @return the value as a Java array. May be an empty array.
     * @throws IOException if an error occurred while reading the stream.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public Object readAsArray(ChannelDataInput input, int count) throws IOException {
        throw new UnsupportedOperationException(name());
    }

    /**
     * Reads an arbitrary number of values as a wrapper around a Java array of primitive type.
     * This wrapper provide a more convenient way to access array elements than the object
     * returned by {@link #readAsArray(ChannelDataInput, int)}.
     *
     * @param  input  the input from where to read the values.
     * @param  count  the number of values.
     * @return the value as a wrapper around a Java array of primitive type.
     * @throws IOException if an error occurred while reading the stream.
     * @throws ArithmeticException if the given count is too large.
     * @throws NumberFormatException if the value was stored in ASCII and cannot be parsed.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public final Vector readAsVector(final ChannelDataInput input, final long count) throws IOException {
        return Vector.create(readAsArray(input, Math.toIntExact(count)), isUnsigned);
    }
}
