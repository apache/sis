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
package org.apache.sis.storage.geotiff;

import java.util.Arrays;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.Fraction;
import org.apache.sis.math.Vector;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;


/**
 * The types of values in a TIFF header. Provides also some support for reading a value of a given type.
 *
 * <p><b>Note on naming convention:</b>
 * the values in this enumeration are not necessarily named as the TIFF type names.
 * This enumeration rather match the Java primitive type names.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.8
 * @module
 */
enum Type {
    /**
     * An 8-bits byte that may contain anything, depending on the definition of the field.
     * <ul>
     *   <li>TIFF name: {@code UNDEFINED}</li>
     *   <li>TIFF code: 7</li>
     * </ul>
     */
    UNDEFINED(7, Byte.BYTES, false) {
        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            throw new UnsupportedOperationException(name());
        }

        /** Unknown value (used for reporting native metadata only). */
        @Override public Object readObject(final ChannelDataInput input, final long count) throws IOException {
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
    BYTE(6, Byte.BYTES, false) {
        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readByte();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readByte(), count);
            }
            return value;
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
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
    UBYTE(1, Byte.BYTES, true) {
        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readUnsignedByte();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readUnsignedByte(), count);
            }
            return value;
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
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
    SHORT(8, Short.BYTES, false) {
        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readShort();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readShort(), count);
            }
            return value;
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
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
    USHORT(3, Short.BYTES, true) {
        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readUnsignedShort();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readUnsignedShort(), count);
            }
            return value;
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
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
    INT(9, Integer.BYTES, false) {
        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readInt();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readInt(), count);
            }
            return value;
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
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
    UINT(4, Integer.BYTES, true) {
        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readUnsignedInt();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readUnsignedInt(), count);
            }
            return value;
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
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
        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readLong();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readLong(), count);
            }
            return value;
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
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
        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            final long value = input.readLong();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readLong(), count);
            }
            if (value >= 0) {
                return value;
            }
            throw new ArithmeticException(canNotConvert(Long.toUnsignedString(value)));
        }

        @Override public double readDouble(final ChannelDataInput input, final long count) throws IOException {
            return Numerics.toUnsignedDouble(readLong(input, count));
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
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
    FLOAT(11, Float.BYTES, false) {
        private float readFloat(final ChannelDataInput input, final long count) throws IOException {
            final float value = input.readFloat();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readFloat(), count);
            }
            return value;
        }

        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            final float value = readFloat(input, count);
            final long r = (long) value;
            if (r == value) {
                return r;
            }
            throw new ArithmeticException(canNotConvert(Float.toString(value)));
        }

        @Override public double readDouble(final ChannelDataInput input, final long count) throws IOException {
            return DecimalFunctions.floatToDouble(readFloat(input, count));
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
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
    DOUBLE(12, Double.BYTES, false) {
        @Override public double readDouble(final ChannelDataInput input, final long count) throws IOException {
            final double value = input.readDouble();
            for (long i=1; i<count; i++) {
                ensureSingleton(value, input.readDouble(), count);
            }
            return value;
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
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
    RATIONAL(10, (2*Integer.BYTES), false) {
        private Fraction readFraction(final ChannelDataInput input, final long count) throws IOException {
            final Fraction value = new Fraction(input.readInt(), input.readInt());
            for (long i=1; i<count; i++) {
                ensureSingleton(value.doubleValue(), input.readInt() / (double) input.readInt(), count);
            }
            return value;
        }

        @Override public double readDouble(final ChannelDataInput input, final long count) throws IOException {
            return readFraction(input, count).doubleValue();
        }

        /** Returns the value as a {@link Fraction}. */
        @Override public Object readObject(final ChannelDataInput input, final long count) throws IOException {
            return readFraction(input, count);
        }
    },

    /**
     * Two unsigned integers: the first represents the numerator of a fraction; the second, the denominator.
     * <ul>
     *   <li>TIFF name: {@code RATIONAL}</li>
     *   <li>TIFF code: 5</li>
     * </ul>
     */
    URATIONAL(5, (2*Integer.BYTES), true) {
        private Number readFraction(final ChannelDataInput input, final long count) throws IOException {
            final long n  = input.readUnsignedInt();
            final long d  = input.readUnsignedInt();
            final int  ni = (int) n;
            final int  di = (int) d;
            final Number value = (ni == n && di == d) ? new Fraction(ni, di) : Double.valueOf(n / (double) d);
            for (long i=1; i<count; i++) {
                ensureSingleton(value.doubleValue(), input.readUnsignedInt() / (double) input.readUnsignedInt(), count);
            }
            return value;
        }

        @Override public double readDouble(final ChannelDataInput input, final long count) throws IOException {
            return readFraction(input, count).doubleValue();
        }

        /** Returns the value as {@link Faction} if possible or {@link Double} otherwise. */
        @Override public Object readObject(final ChannelDataInput input, final long count) throws IOException {
            return readFraction(input, count);
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
    ASCII(2, Byte.BYTES, false) {
        @Override public String[] readString(final ChannelDataInput input, final long length, final Charset charset) throws IOException {
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
        private String readString(final ChannelDataInput input, final long count, final boolean mandatory) throws IOException {
            final String[] lines = readString(input, count, StandardCharsets.US_ASCII);
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

        @Override public long readLong(final ChannelDataInput input, final long count) throws IOException {
            return Long.parseLong(readString(input, count, true));
        }

        @Override public double readDouble(final ChannelDataInput input, final long count) throws IOException {
            String text = readString(input, count, false);
            if (text == null || (text = text.trim()).isEmpty() || text.equalsIgnoreCase("NaN")) {
                return Double.NaN;
            }
            return Double.parseDouble(text);
        }

        @Override public Object readArray(final ChannelDataInput input, final int count) throws IOException {
            return readString(input, count, StandardCharsets.US_ASCII);
        }

        @Override public Object readObject(final ChannelDataInput input, final long count) throws IOException {
            return readString(input, count, false);
        }
    };

    /**
     * The TIFF numerical code for this type.
     */
    final int code;

    /**
     * The size of this type, in number of bytes.
     */
    final int size;

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
    static Type valueOf(final int code) {
        return (code >= 0 && code < FROM_CODES.length) ? FROM_CODES[code] : null;
    }

    /**
     * Formats a rational number. This is a helper method for {@link #RATIONAL} and {@link #URATIONAL} types.
     */
    static String toString(final long numerator, final long denominator) {
        return new StringBuilder().append(numerator).append('/').append(denominator).toString();
    }

    /**
     * Invoked by {@code read(…)} method implementations for verifying that the {@code count} argument value is 1.
     * All read methods other than {@code readArray(…)} expect exactly one value, except methods in {@link #ASCII}
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
    static void ensureSingleton(final long previous, final long actual, final long count) {
        if (previous != actual) {
            // Even if the methods did not expected an array in argument, we are conceptually reading an array.
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, 1, count));
        }
    }

    /**
     * Same as {@link #ensureSingleton(long, long, long)} but with floating-point values.
     */
    static void ensureSingleton(final double previous, final double actual, final long count) {
        if (Double.doubleToLongBits(previous) != Double.doubleToLongBits(actual)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, 1, count));
        }
    }

    /**
     * Formats an error message for a value that can not be converted.
     */
    final String canNotConvert(final String value) {
        return Errors.format(Errors.Keys.CanNotConvertValue_2, value, name());
    }

    /**
     * Reads a single value and returns it as a signed {@code short} type, performing conversion if needed.
     * This method should be invoked when the caller expects a single value.
     *
     * @param  input  the input from where to read the value.
     * @param  count  the amount of values (normally exactly 1).
     * @return the value as a {@code short}.
     * @throws IOException if an error occurred while reading the stream.
     * @throws NumberFormatException if the value was stored in ASCII and can not be parsed.
     * @throws ArithmeticException if the value can not be represented in the Java signed {@code short} type.
     * @throws IllegalArgumentException if the value is not a singleton.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public final short readShort(final ChannelDataInput input, final long count) throws IOException {
        final long value = readLong(input, count);
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
     * @param  count  the amount of values (normally exactly 1).
     * @return the value as an {@code int}.
     * @throws IOException if an error occurred while reading the stream.
     * @throws NumberFormatException if the value was stored in ASCII and can not be parsed.
     * @throws ArithmeticException if the value can not be represented in the Java signed {@code int} type.
     * @throws IllegalArgumentException if the value is not a singleton.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public final int readInt(final ChannelDataInput input, final long count) throws IOException {
        final long value = readLong(input, count);
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
     * @throws NumberFormatException if the value was stored in ASCII and can not be parsed.
     * @throws ArithmeticException if the value can not be represented in the Java signed {@code long} type.
     * @throws IllegalArgumentException if the value is not a singleton.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public final long readUnsignedLong(final ChannelDataInput input, final long count) throws IOException {
        final long value = readLong(input, count);
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
     * @param  count  the amount of values (normally exactly 1).
     * @return the value as a {@code long}.
     * @throws IOException if an error occurred while reading the stream.
     * @throws NumberFormatException if the value was stored in ASCII and can not be parsed.
     * @throws ArithmeticException if the value can not be represented in the Java signed {@code long} type.
     * @throws IllegalArgumentException if the value is not a singleton.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public long readLong(final ChannelDataInput input, final long count) throws IOException {
        // All enum MUST override one of 'readLong' or 'readDouble' methods.
        final double value = readDouble(input, count);
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
     * @param  count  the amount of values (normally exactly 1).
     * @return the value as a {@code double}.
     * @throws IOException if an error occurred while reading the stream.
     * @throws NumberFormatException if the value was stored in ASCII and can not be parsed.
     * @throws IllegalArgumentException if the value is not a singleton.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public double readDouble(ChannelDataInput input, long count) throws IOException {
        // All enum MUST override one of 'readLong' or 'readDouble' methods.
        return readLong(input, count);
    }

    /**
     * Reads the value as strings. There is usually exactly one string, but an arbitrary amount is allowed.
     *
     * @param  input    the input from where to read the value.
     * @param  length   the string length, including the final NUL byte.
     * @param  charset  the character encoding (normally US ASCII).
     * @return the value as a string.
     * @throws IOException if an error occurred while reading the stream.
     * @throws ArithmeticException if the given length is too large.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public String[] readString(final ChannelDataInput input, final long length, final Charset charset) throws IOException {
        final String[] s = new String[Math.toIntExact(length)];
        for (int i=0; i<s.length; i++) {
            final double value = readDouble(input, 1);
            final long r = (long) value;
            s[i] = (r == value) ? String.valueOf(r) : String.valueOf(value);
        }
        return s;
    }

    /**
     * Returns the value as a {@link Vector}, a {@link Number} (only for fractions) or a {@link String} instance.
     * This method should be overridden by all enumeration values that do no override
     * {@link #readArray(ChannelDataInput, int)}.
     *
     * @param  input  the input from where to read the values.
     * @param  count  the amount of values.
     * @return the value as a Java array or a {@link String}, or {@code null} if undefined.
     * @throws IOException if an error occurred while reading the stream.
     */
    public Object readObject(ChannelDataInput input, long count) throws IOException {
        return readVector(input, count);
    }

    /**
     * Reads an arbitrary amount of values as a Java array.
     *
     * @param  input  the input from where to read the values.
     * @param  count  the amount of values.
     * @return the value as a Java array. May be an empty array.
     * @throws IOException if an error occurred while reading the stream.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public Object readArray(ChannelDataInput input, int count) throws IOException {
        throw new UnsupportedOperationException(name());
    }

    /**
     * Reads an arbitrary amount of values as a wrapper around a Java array of primitive type.
     * This wrapper provide a more convenient way to access array elements than the object
     * returned by {@link #readArray(ChannelDataInput, int)}.
     *
     * @param  input  the input from where to read the values.
     * @param  count  the amount of values.
     * @return the value as a wrapper around a Java array of primitive type.
     * @throws IOException if an error occurred while reading the stream.
     * @throws ArithmeticException if the given count is too large.
     * @throws NumberFormatException if the value was stored in ASCII and can not be parsed.
     * @throws UnsupportedOperationException if this type is {@link #UNDEFINED}.
     */
    public final Vector readVector(final ChannelDataInput input, final long count) throws IOException {
        return Vector.create(readArray(input, Math.toIntExact(count)), isUnsigned);
    }
}
