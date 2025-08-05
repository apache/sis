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
package org.apache.sis.math;

import java.io.Serializable;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.AbstractList;
import java.util.RandomAccess;
import java.util.StringJoiner;
import java.util.Optional;
import java.util.Objects;
import java.util.function.IntSupplier;
import static java.util.logging.Logger.getLogger;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.system.Loggers;


/**
 * A vector of real numbers. An instance of {@code Vector} can be a wrapper around an array of Java primitive type
 * (typically {@code float[]} or {@code double[]}), or it may be a function calculating values on the fly.
 * Often the two above-cited cases are used together, for example in a time series where:
 *
 * <ul>
 *   <li><var>x[i]</var> is a linear function of <var>i</var>
 *       (e.g. the sampling time of measurements performed at a fixed time interval)</li>
 *   <li><var>y[i]</var> is the measurement of a phenomenon at time <var>x[i]</var>.</li>
 * </ul>
 *
 * <h2>Instantiation</h2>
 * Instances of {@code Vector} are usually created by calls to the {@link #create(Object, boolean)} static method.
 * The supplied array is not cloned – changes to the primitive array are reflected in the vector, and vice-versa.
 * Vectors can be a view over a subsection of the given array, or can provide a view of the elements in reverse order,
 * <i>etc</i>. The example below creates a view over a subsection:
 *
 * {@snippet lang="java" :
 *     float[] array = new float[100];
 *     Vector v = Vector.create(array, false).subList(20, 40)
 *     // At this point, v.doubleValue(0) is equivalent to (double) array[20].
 *     }
 *
 * <h2>Usage</h2>
 * The methods that are most often used after {@code Vector} creation are {@link #size()} and {@link #doubleValue(int)}
 * or {@link #intValue(int)}. Those methods make abstraction of the underlying data type. For example if the vector is
 * backed by an array of type {@code int[]}, then calls to {@code doubleValue(index)} will:
 *
 * <ul>
 *   <li>Convert the {@code int[index]} value to a {@code double} value.</li>
 *   <li>If {@link #isUnsigned()} is {@code true}, apply the necessary bitmask before conversion.</li>
 * </ul>
 *
 * Widening conversions (for example from {@code short} to {@code long}) are always allowed.
 * Narrowing conversions are allowed if the result can be represented at least approximately by the target type.
 * For example, conversions from {@code double} to {@code float} are always allowed (values that are too large for
 * the {@code float} type are represented by positive of negative infinity), but conversions from {@code long} to
 * {@code short} are allowed only if the value is between {@link Short#MIN_VALUE} and {@link Short#MAX_VALUE} inclusive.
 *
 * <h2>Comparison with other API</h2>
 * The above functionalities look like similar functionalities provided by {@link java.nio.ByteBuffer}
 * in standard Java, but they actually serve different purposes. The {@code ByteBuffer} getter methods
 * (for example {@code getShort(int)}, {@code getLong(int)}, <i>etc.</i>) allow to decode a sequence of
 * bytes in a way determined by the type of the value to decode (2 bytes for a {@code short}, 8 bytes
 * for a {@code long}, <i>etc.</i>) – the type of the stored value must be known before to read it.
 * By contrast, this {@code Vector} class is used in situations where <em>the decoding has already been done</em>
 * by the code that <em>create</em> a {@code Vector} object, but the data type may not be known
 * by the code that will <em>use</em> the {@code Vector} object.
 * For example, a method performing a numerical calculation may want to see the data as {@code double} values
 * without concern about whether the data were really stored as {@code double} or as {@code float} values.
 * For the situations where a {@link Buffer} is needed, inter-operability is provided by the {@link #buffer()}
 * method and by accepting buffer in the {@link #create(Object, boolean)} method.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @version 1.4
 *
 * @see org.apache.sis.util.collection.IntegerList
 *
 * @since 0.8
 */
public abstract class Vector extends AbstractList<Number> implements RandomAccess {
    /**
     * Wraps the given object in a vector. The argument should be one of the following:
     *
     * <ul>
     *   <li>An array of a primitive type, like {@code float[]}.</li>
     *   <li>A {@code Number[]} array.</li>
     *   <li>A {@code String[]} array (not recommended, but happen with some file formats).</li>
     *   <li>A {@code Vector}, in which case it is returned unchanged.</li>
     *   <li>A {@link Buffer} backed by a Java array. Wrap the part between buffer's position and limit.</li>
     *   <li>The {@code null} value, in which case {@code null} is returned.</li>
     * </ul>
     *
     * The given argument is not cloned.
     * Consequently, changes in the underlying array are reflected in this vector, and vice-versa.
     *
     * <h4>Unsigned integers</h4>
     * Java has no primitive support for unsigned integers. But some file formats use unsigned integers,
     * which can be simulated in Java by the use of bit masks or methods like {@link Integer#toUnsignedLong(int)}.
     * This {@code Vector} class applies automatically those masks (unless otherwise noticed in method Javadoc)
     * if the {@code isUnsigned} argument is {@code true}.
     * That argument applies only to {@code byte[]}, {@code short[]}, {@code int[]} or {@code long[]} arrays
     * and is ignored for all other kind of arrays.
     *
     * @param  array       the object to wrap in a vector, or {@code null}.
     * @param  isUnsigned  {@code true} if integer types should be interpreted as unsigned integers.
     * @return the given object wrapped in a vector, or {@code null} if the argument was {@code null}.
     * @throws IllegalArgumentException if the type of the given object is not recognized by the method.
     */
    public static Vector create(final Object array, final boolean isUnsigned) throws IllegalArgumentException {
        if (array == null) {
            return null;
        }
        if (array.getClass().isArray()) {
            return ArrayVector.newInstance(array, isUnsigned);
        }
        if (array instanceof Vector) {
            return (Vector) array;
        }
        if (array instanceof Buffer) {
            final Buffer buffer = (Buffer) array;
            if (buffer.hasArray()) {
                final int offset = buffer.arrayOffset();
                return ArrayVector.newInstance(buffer.array(), isUnsigned)
                        .subList(offset + buffer.position(), offset + buffer.limit());
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentClass_2, "array", array.getClass()));
    }

    /**
     * Wraps the given array of floating point values. This method does not clone the array:
     * changes in the given array will be reflected in the returned vector and vice-versa.
     * This method is equivalent to
     * <code>{@link #create(Object, boolean) create}(array, false)</code> but potentially faster.
     *
     * @param  array  the array of floating point values to wrap in a vector, or {@code null}.
     * @return the given array wrapped in a vector, or {@code null} if the argument was {@code null}.
     *
     * @since 1.0
     */
    public static Vector create(final double[] array) {
        /*
         * NOTE: we do not use variable-length argument (double...) because doing so may force us to
         * declare `create` methods for all other primitive types,  otherwise some developers may be
         * surprised that `create(0, 1, 2, 3)` converts the integer values to doubles. We do not yet
         * provide explicit `create(...)` methods for other primitive types because it is not needed
         * by Apache SIS and it would lost a feature of current API, which is to force developers to
         * think whether their integers are signed or unsigned.
         */
        return (array != null) ? new ArrayVector.Doubles(array) : null;
    }

    /**
     * Wraps the given {@code float[]} array in a vector that preserve the string representations in base 10.
     * For example, the 0.1 {@code float} value cast to {@code double} normally produces 0.10000000149011612
     * because of the way IEEE 754 arithmetic represents numbers (in base 2 instead of base 10). But the
     * vector returned by this method will convert the 0.1 {@code float} value into the 0.1 {@code double} value.
     * Note that despite the appearance, this is <strong>not</strong> more accurate than the normal cast,
     * because base 10 is not more privileged in nature than base 2.
     *
     * <h4>When to use</h4>
     * This method can be used when there is good reasons to think that the {@code float} numbers were parsed
     * from decimal representations, for example an ASCII file. There is usually no reason to use this method
     * if the values are the result of some numerical computations.
     *
     * @param  array  the array of floating point values to wrap in a vector, or {@code null}.
     * @return the given array wrapped in a vector, or {@code null} if the argument was {@code null}.
     *
     * @see DecimalFunctions#floatToDouble(float)
     */
    public static Vector createForDecimal(final float[] array) {
        return (array != null) ? new ArrayVector.Decimal(array) : null;
    }

    /**
     * Creates a sequence of numbers in a given range of values using the given increment.
     * The range of values will be {@code first} inclusive to {@code (first + increment*length)} exclusive.
     * Note that the value given by the {@code first} argument is equivalent to a "lowest" or "minimum" value
     * only if the given increment is positive.
     *
     * <p>The {@linkplain #getElementType() element type} will be inferred from the type of the given
     * {@code Number} instances. If will typically be {@code Integer.class} for the [100:1:120] range
     * and {@code Double.class} for the [0:0.1:1] range.</p>
     *
     * @param  first      the first value, inclusive.
     * @param  increment  the difference between the values at two adjacent indexes.
     * @param  length     the length of the desired vector.
     * @return the given sequence as a vector.
     */
    public static Vector createSequence(final Number first, final Number increment, final int length) {
        Class<? extends Number> type;
        type = Numbers.widestClass(first, increment);
        type = Numbers.widestClass(type,
               Numbers.narrowestClass(first.doubleValue() + increment.doubleValue() * (length-1)));
        return createSequence(type, first, increment, length);
    }

    /**
     * Creates a sequence of the given type.
     */
    static Vector createSequence(final Class<? extends Number> type, final Number first, final Number increment, final int length) {
        final int t = Numbers.getEnumConstant(type);
        if (t >= Numbers.BYTE && t <= Numbers.LONG) {
            // Use the long type if possible because not all long values can be represented as double.
            return new SequenceVector.Longs(type, first, increment, length);
        } else if (t == Numbers.FLOAT) {
            return new SequenceVector.Floats(type, first, increment, length);
        } else {
            return new SequenceVector.Doubles(type, first, increment, length);
        }
    }

    /**
     * For subclasses constructor.
     */
    protected Vector() {
    }

    /**
     * Returns the type of elements in this vector. If this vector is backed by an array of a primitive type,
     * then this method returns the <em>wrapper</em> class, not the primitive type. For example if this vector
     * is backed by an array of type {@code float[]}, then this method returns {@code Float.class},
     * not {@link Float#TYPE}.
     *
     * <p>The information returned by this method is only indicative; it is not guaranteed to specify accurately
     * this kind of objects returned by the {@link #get(int)} method. There is various situations where the types
     * may not match:</p>
     *
     * <ul>
     *   <li>If this vector {@linkplain #isUnsigned() is unsigned}, then the values returned by {@code get(int)}
     *       will be instances of a type wider than the type used by this vector for storing the values.</li>
     *   <li>If this vector has been {@linkplain #createForDecimal(float[]) created for decimal numbers},
     *       then the values returned by {@code get(int)} will use double-precision even if this vector
     *       stores the values as single-precision floating point numbers.</li>
     *   <li>If this vector {@linkplain #compress(double) has been compressed}, then the type returned by this
     *       method does not describe accurately the range of values that this vector can store.</li>
     * </ul>
     *
     * <p>Users of the {@link #doubleValue(int)} method do not need to care about this information since
     * {@code Vector} will perform automatically the type conversion. Users of other methods may want to
     * verify this information for avoiding {@link ArithmeticException}.</p>
     *
     * @return the type of elements in this vector.
     *
     * @see org.apache.sis.util.collection.CheckedContainer#getElementType()
     */
    public abstract Class<? extends Number> getElementType();

    /**
     * Returns {@code true} if values in this vector can be cast to single-precision floating point numbers
     * ({@code float}) without precision lost. In case of doubt, this method conservatively returns {@code false}.
     * This information is inferred from the {@linkplain #getElementType() element type} only.
     * This method does not check the actual values contained in this vector.
     *
     * @return whether values in this vector can be cast to {@code float} primitive type.
     *
     * @see #floatValue(int)
     * @see #floatValues()
     *
     * @since 1.1
     */
    public boolean isSinglePrecision() {
        final byte type = Numbers.getEnumConstant(getElementType());
        return (type == Numbers.FLOAT) || (type >= Numbers.BYTE && type <= Numbers.SHORT);
    }

    /**
     * Returns an estimation of the number of bits used by each value in this vector.
     * This is an estimation only and should be used only as a hint.
     */
    private int getBitCount() {
        try {
            return Numbers.primitiveBitCount(getElementType());
        } catch (IllegalArgumentException e) {
            return Integer.SIZE;                    // Assume references compressed on 32 bits.
        }
    }

    /**
     * Returns {@code true} if this vector contains only integer values.
     * This method may iterate over all values for performing this verification.
     *
     * @return {@code true} if this vector contains only integer values.
     */
    public boolean isInteger() {
        if (!Numbers.isInteger(getElementType())) {
            for (int i=size(); --i >= 0;) {
                if (!Numerics.isInteger(doubleValue(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if integer values shall be interpreted as unsigned values.
     * This method may return {@code true} for data stored in {@code byte[]}, {@code short[]}, {@code int[]}
     * or {@code long[]} arrays, but never for data stored in {@code float[]} and {@code double[]} arrays.
     * The default implementation returns {@code false}.
     *
     * <p>Unless otherwise noticed in Javadoc, users do not need to care about this information since
     * {@code Vector} methods will perform automatically the operations needed for unsigned integers.</p>
     *
     * @return {@code true} if the integer values shall be interpreted as unsigned values.
     */
    public boolean isUnsigned() {
        return false;
    }

    /**
     * Returns the number of elements in this vector.
     *
     * @return the number of elements in this vector.
     */
    @Override
    public abstract int size();

    /**
     * Returns {@code true} if this vector is empty or contains only {@code NaN} values.
     *
     * @return whether this vector is empty or contains only {@code NaN} values.
     *
     * @since 1.1
     */
    public boolean isEmptyOrNaN() {
        final int n = size();
        for (int i=0; i<n; i++) {
            if (!isNaN(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the value at the given index is {@code null} or {@code NaN}.
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return {@code true} if the value at the given index is {@code NaN}.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public abstract boolean isNaN(int index);

    /**
     * Returns the value at the given index as a {@code double}.
     * This is the safest method since all primitive types supported by {@code Vector}
     * are convertible to the {@code double} type.
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NullPointerException if the value is {@code null} (never happen if this vector wraps an array of primitive type).
     * @throws NumberFormatException if the value is stored as a {@code String} and cannot be parsed.
     *
     * @see #doubleValues()
     */
    public abstract double doubleValue(int index);

    /**
     * Returns the value at the given index as a {@code float}.
     * This method may result in a lost of precision if this vector
     * stores or computes its values with the {@code double} type.
     *
     * <p>The default implementation delegates to {@link #doubleValue(int)} and cast the result to {@code float}.</p>
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NullPointerException if the value is {@code null} (never happen if this vector wraps an array of primitive type).
     * @throws NumberFormatException if the value is stored as a {@code String} and cannot be parsed.
     *
     * @see #floatValues()
     * @see #isSinglePrecision()
     */
    public float floatValue(int index) {
        return (float) doubleValue(index);
    }

    /**
     * Returns the value at the given index as a {@code long}.
     * If this vector uses floating point values, the value is rounded to the nearest integer.
     *
     * <p>The default implementation delegates to {@link #doubleValue(int)} and verifies
     * if the result can be rounded to a {@code long} with an error not greater than 0.5.
     * Subclasses that store or compute their values with an integer type should override this method.</p>
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NullPointerException if the value is {@code null} (never happen if this vector wraps an array of primitive type).
     * @throws NumberFormatException if the value is stored as a {@code String} and cannot be parsed.
     * @throws ArithmeticException if the value is too large for the capacity of the {@code long} type.
     */
    public long longValue(final int index) {
        final double value = doubleValue(index);
        final long result = Math.round(value);
        if (Math.abs(result - value) <= 0.5) {
            return result;
        }
        throw canNotConvert(index, long.class);
    }

    /**
     * Returns the value at the given index as an {@code int}.
     * If this vector uses floating point values, the value is rounded to the nearest integer.
     *
     * <p>The default implementation delegates to {@link #longValue(int)} and verifies if the result
     * fits in the {@code int} type. Subclasses that store or compute their values with the {@code int},
     * {@code short} or {@code byte} type should override this method.</p>
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NullPointerException if the value is {@code null} (never happen if this vector wraps an array of primitive type).
     * @throws NumberFormatException if the value is stored as a {@code String} and cannot be parsed.
     * @throws ArithmeticException if the value is too large for the capacity of the {@code int} type.
     */
    public int intValue(final int index) {
        final long value = longValue(index);
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return (int) value;
        }
        throw canNotConvert(index, int.class);
    }

    /**
     * Returns the value at the given index as a {@code short}.
     * If this vector uses floating point values, the value is rounded to the nearest integer.
     *
     * <p>The default implementation delegates to {@link #longValue(int)} and verifies if the result
     * fits in the {@code short} type. Subclasses that store or compute their values with the {@code short}
     * or {@code byte} type should override this method.</p>
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NullPointerException if the value is {@code null} (never happen if this vector wraps an array of primitive type).
     * @throws NumberFormatException if the value is stored as a {@code String} and cannot be parsed.
     * @throws ArithmeticException if the value is too large for the capacity of the {@code short} type.
     */
    public short shortValue(final int index) {
        final long value = longValue(index);
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return (short) value;
        }
        throw canNotConvert(index, short.class);
    }

    /**
     * Returns the value at the given index as a {@code byte}.
     * If this vector uses floating point values, the value is rounded to the nearest integer.
     *
     * <p>The default implementation delegates to {@link #longValue(int)} and verifies if the result
     * fits in the {@code byte} type. Subclasses that store or compute their values with the {@code byte}
     * type should override this method.</p>
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NullPointerException if the value is {@code null} (never happen if this vector wraps an array of primitive type).
     * @throws NumberFormatException if the value is stored as a {@code String} and cannot be parsed.
     * @throws ArithmeticException if the value is too large for the capacity of the {@code byte} type.
     */
    public byte byteValue(final int index) {
        final long value = longValue(index);
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return (byte) value;
        }
        throw canNotConvert(index, byte.class);
    }

    /**
     * Returns the exception to be thrown when the component type in the backing array can
     * not be converted to the requested type through an identity or widening conversion.
     */
    private ArithmeticException canNotConvert(final int index, final Class<?> target) {
        return new ArithmeticException(Errors.format(Errors.Keys.CanNotConvertValue_2, stringValue(index), target));
    }

    /**
     * Returns a string representation of the value at the given index.
     * Invoking this method is generally equivalent to invoking
     * <code>String.valueOf({@linkplain #get(int) get}(index))</code>
     * except if the values are {@linkplain #isUnsigned() unsigned integers}.
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return a string representation of the value at the given index (may be {@code null}).
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     *
     * @see #toString()
     */
    public abstract String stringValue(int index);

    /**
     * Returns the number at the given index, or {@code null} if none.
     * The object returned by this method is usually an instance of the class returned by {@link #getElementType()},
     * but may also be an instance of a wider type if this is necessary for representing the values. For example
     * if {@link #getElementType()} returns {@code Byte.class} but {@link #isUnsigned()} returns {@code true},
     * then this method will rather return instances of {@link Short} because that type is the smallest Java
     * primitive type capable to hold byte values in the [0 … 255] range. But the elements are still stored
     * internally as {@code byte}, and the vector cannot accept values outside the [0 … 255] range even if
     * they are valid {@link Short} values.
     *
     * <p>The class of returned objects should be stable. For example, this method should not use different types
     * for different range of values. This stability is recommended but not guaranteed because {@code Vector}
     * can also wrap arbitrary {@code Number[]} arrays.</p>
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return the value at the given index (may be {@code null}).
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NumberFormatException if the value is stored as a {@code String} and cannot be parsed.
     */
    @Override
    public abstract Number get(int index);

    /**
     * Sets the number at the given index.
     * The given number should be an instance of the same type as the number returned by {@link #get(int)}.
     * If not, the stored value may lost precision as a result of the cast.
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @param  value  the value to set at the given index.
     * @return the value previously stored at the given index.
     * @throws UnsupportedOperationException if this vector is read-only.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NumberFormatException if the previous value was stored as a {@code String} and cannot be parsed.
     * @throws IllegalArgumentException if this vector uses some {@linkplain #compress(double) compression} technic
     *         and the given value is out of range for that compression.
     */
    @Override
    public abstract Number set(int index, Number value);

    /**
     * Sets a range of elements to the given number. Invoking this method is equivalent
     * to invoking {@link #set(int, Number)} in a loop, but potentially much more efficient.
     *
     * @param  fromIndex  index of the first element (inclusive) to be filled with the specified value.
     * @param  toIndex    index of the last element (exclusive) to be filled with the specified value.
     * @param  value      the value to be stored in elements of the vector.
     *
     * @since 1.1
     */
    public void fill(int fromIndex, final int toIndex, final Number value) {
        // Subclasses override with more efficient implementations.
        Objects.checkFromToIndex(fromIndex, toIndex, size());
        while (fromIndex < toIndex) set(fromIndex++, value);
    }

    /**
     * Returns the index of the first value which is equal (if {@code equality} is true)
     * or different (if {@code equality} is false) to the value at the {@code toSearch} index.
     * Subclasses should override if they can provide a more efficient implementation.
     *
     * @param  toSearch   index of the value to search.
     * @param  index      index of the first value where to start the search.
     * @param  equality   whether we search the first equal value, or the first different value.
     * @return index of the value found, or the vector size if the value has not been found.
     */
    int indexOf(final int toSearch, int index, final boolean equality) {
        final Number first = get(toSearch);
        final int size = size();
        while (index < size && first.equals(get(index)) != equality) index++;
        return index;
    }

    /**
     * Detects repetition patterns in the values contained in this vector. The repetitions detected by this method are
     * patterns that at repeated at a regular interval on the whole vector; this method does not search for repetitions
     * occurring at irregular intervals. This method returns an array of typically 0, 1 or 2 elements where zero element
     * means that no repetition has been found, one element describes a repetition (see the example below), and two elements
     * describes a repetition of the repetitions (examples below). More elements (deeper recursion) are theoretically
     * possible but not yet implemented.
     *
     * <p>If the values in this vector are of the form (<var>x</var>, <var>x</var>, …, <var>x</var>, <var>y</var>, <var>y</var>,
     * …, <var>y</var>, <var>z</var>, <var>z</var>, …, <var>z</var>, …), then the first integer in the returned array is the
     * number of consecutive <var>x</var> values before the <var>y</var> values. That number of occurrences must be the same
     * than the number of consecutive <var>y</var> values before the <var>z</var> values, the number of consecutive <var>z</var>
     * values before the next values, and so on until the end of the vector.</p>
     *
     * <h4>Examples</h4>
     * In the following vector, each value is repeated 3 times. So the array returned by this method would be {@code {4}},
     * meaning that the first number appears 4 times, followed by a new number appearing 4 times, followed by a new number
     * appearing 4 times, and so on until the end of the vector.
     *
     * <pre class="text">
     *    10, 10, 10, 10,
     *    12, 12, 12, 12,
     *    15, 15, 15, 15</pre>
     *
     * <h4>Repetitions of repetitions</h4>
     * For the next level (the second integer in the returned array), this method represents above repetitions by single entities
     * then reapplies the same repetition detection. This method processes has if the (<var>x</var>, <var>x</var>, …, <var>x</var>,
     * <var>y</var>, <var>y</var>, …, <var>y</var>, <var>z</var>, <var>z</var>, …, <var>z</var>, …) vector was replaced by a new
     * (<b>x</b>, <b>y</b>, <b>z</b>, …) vector, then the same detection algorithm was applied recursively.
     *
     * <h5>Examples</h5>
     * In the following vector, each value is repeated 2 times, then the sequence of 12 values is itself repeated 2 times.
     * So the array returned by this method would be {@code {3,4}}, meaning that the first number appears 3 times, followed
     * by a new number appearing 3 times, <i>etc.</i> until we counted 4 groups of 3 numbers. Then the whole sequence is
     * repeated until the end of the vector.
     *
     * <pre class="text">
     *    10, 10, 10,  12, 12, 12,  15, 15, 15,  18, 18, 18,
     *    10, 10, 10,  12, 12, 12,  15, 15, 15,  18, 18, 18,
     *    10, 10, 10,  12, 12, 12,  15, 15, 15,  18, 18, 18</pre>
     *
     * <h4>Use cases</h4>
     * This method is useful for analyzing the localization grid provided by some files (for example in netCDF format).
     * Those grids sometimes have constant longitude for the same column index, or constant latitude for the same row index.
     * This method can detect such regularity, which allows more efficient handling of the <i>grid to CRS</i> transform.
     *
     * @param  candidates  probable values, or {@code null} or an empty array if unknown. If non-empty, those values will be used
     *         for narrowing the search, which may improve performances. There is no guarantee that the values returned by this
     *         method will be among the given candidates.
     * @return the number of times that entities (numbers, or group of numbers) appears consecutively with identical values.
     *         If no such repetition is found, an empty array.
     *
     * @since 1.0
     *
     * @see #repeat(boolean, int)
     */
    public int[] repetitions(int... candidates) {
        if (candidates != null && candidates.length == 0) {
            candidates = null;
        }
        final int size = size();
        if (size >= 2) {
            /*
             * For the first level of repetitions, we rely on a method to be overridden by subclasses
             * for detecting the length of consecutive identical numbers. We could have used the more
             * generic algorithm based on `equals(int, int, Vector, int)` instead, but this approach
             * is faster.
             */
            int r0 = 0;
            for (int i=0; i < size-1; i += r0) {
                final int p = r0;
                r0 = indexOf(i, i+1, false) - i;
                if (r0 <= 1 || (p % r0) != 0) {
                    r0 = 1;
                    break;
                }
            }
            /*
             * At this point r0 is the number of identical consecutive numbers in vectors like (x,x,x, y,y,y, z,z,z)
             * and shall not be modified anymore for the rest of this method. This is the first integer value in the
             * array to be returned. Following algorithm applies to deeper levels.
             *
             * The `skip` variable is an optimization. Code below would work with skip = 0 all the times, but this is
             * very slow when r0 = 1 because equals(…) is invoked for all values.  Computing an number of values that
             * we can skip in the special case where r0 = 1 increases the speed a lot.
             */
            int candidateIndex = 0;
            final int skip = (r0 == 1) ? indexOf(0, 1, false) : 0;
            int r = 0;
search:     for (;;) {
                if (candidates != null) {
                    do {
                        if (candidateIndex >= candidates.length) {
                            r = size;                                       // Sentinel value for repetition not found.
                            break search;
                        }
                        final int n = candidates[candidateIndex++];
                        ArgumentChecks.ensureStrictlyPositive("candidates", n);
                        r = Math.multiplyExact(r0, n);
                    } while (r <= 0 || r >= size);
                } else {
                    r += r0;
                    if (skip != 0) {
                        /*
                         * Optimization for reducing the number of method calls when r0 = 1: the default algorithm is to
                         * search for a position multiple of `r0` where all values since the beginning of the vector are
                         * repeated. But if `r0` is 1, the default algorithms perform a costly check at every positions.
                         * To avoid that, we use `indexOf` for searching the index of the next position where a match may
                         * exist (we don't care anymore about multiples of r0 since r0 is 1). If the first expected values
                         * are constants, we use `indexOf` again for checking efficiently those constants.
                         */
                        r = indexOf(0, r, true);
                        if (skip != 1) {
                            if (r + skip >= size) break;
                            final int end = indexOf(r, r+1, false);
                            if (end - r != skip) {
                                r = end - 1;
                                continue;
                            }
                        }
                    }
                    if (r >= size) break;
                }
                final int base = Math.min(r0, size - r);
                if (base < skip || equals(skip, base, this, r + skip)) {
                    /*
                     * Found a possible repetition of length r. Verify if this repetition pattern is observed
                     * until the end of the vector. If not, we will search for the next possible repetition.
                     */
                    for (int i=r; i<size; i += r) {
                        if (!equals(0, Math.min(r, size - i), this, i)) {
                            continue search;
                        }
                    }
                    break;      // At this point we verified that the repetition is observed until the vector end.
                }
            }
            if (r < size) {
                return new int[] {r0, r / r0};
            } else if (r0 != 1) {
                return new int[] {r0};
            }
        }
        return ArraysExt.EMPTY_INT;
    }

    /**
     * Returns {@code a-b} as a signed value, throwing an exception if the result overflows a {@code long}.
     * The given values will be interpreted as unsigned values if this vector {@linkplain #isUnsigned() is unsigned}.
     *
     * @param  a  the first value, unsigned if {@link #isUnsigned()} return {@code true}.
     * @param  b  the value to subtract from {@code a}, unsigned if {@link #isUnsigned()} return {@code true}.
     * @return the difference, always signed.
     * @throws ArithmeticException if the difference is too large.
     *
     * @see Math#subtractExact(long, long)
     */
    final long subtract(final long a, final long b) {
        final long inc = a - b;
        // The sign of the difference shall be the same as the sign of Long.compare(…).
        if ((((isUnsigned() ? Long.compareUnsigned(a, b) : Long.compare(a, b)) ^ inc) & Long.MIN_VALUE) != 0) {
            throw new ArithmeticException(Errors.format(Errors.Keys.IntegerOverflow_1, Long.SIZE));
        }
        return inc;
    }

    /**
     * Returns the increment between all consecutive values if this increment is constant, or {@code null} otherwise.
     * If the returned value is non-null, then the following condition shall hold for all values of <var>i</var> in
     * the [0 … {@link #size()} - 1] range:
     *
     * <blockquote><code>{@linkplain Math#abs(double) abs}({@linkplain #doubleValue(int) doubleValue}(<var>i</var>)
     * - (doubleValue(0) + increment*<var>i</var>)) ≤ tolerance</code></blockquote>
     *
     * The tolerance threshold can be zero if exact matches are desired.
     * The return value (if non-null) is always a signed value,
     * even if this vector {@linkplain #isUnsigned() is unsigned}.
     *
     * @param  tolerance  the tolerance threshold for verifying if the increment is constant.
     * @return the increment as a signed value, or {@code null} if the increment is not constant.
     */
    @SuppressWarnings("fallthrough")
    public Number increment(final double tolerance) {
        ArgumentChecks.ensurePositive("tolerance", tolerance);
        int i = size();
        if (i >= 2) try {
            final int type = Numbers.getEnumConstant(getElementType());
            /*
             * For integer types, verify if the increment is constant. We do not use the "first + inc*i"
             * formula because some `long` values cannot be represented accurately as `double` values.
             * The result will be converted to the same type as the vector element type if possible,
             * or the next wider type if the increment is an unsigned value too big for the element type.
             */
            if (type >= Numbers.BYTE && type <= Numbers.LONG && tolerance < 0.5) {
                long p;
                final long inc = subtract(longValue(--i), p = longValue(--i));
                while (i != 0) {
                    if (p - (p = longValue(--i)) != inc) {
                        return null;
                    }
                }
                switch (type) {
                    case Numbers.BYTE:    if (inc >= Byte   .MIN_VALUE && inc <= Byte   .MAX_VALUE) return (byte)  inc;  // else fallthrough
                    case Numbers.SHORT:   if (inc >= Short  .MIN_VALUE && inc <= Short  .MAX_VALUE) return (short) inc;  // else fallthrough
                    case Numbers.INTEGER: if (inc >= Integer.MIN_VALUE && inc <= Integer.MAX_VALUE) return (int)   inc;  // else fallthrough
                    default: return inc;
                }
            }
            /*
             * For floating point types, we must use the same formula as the one used by SequenceVector:
             *
             *     doubleValue(i)  =  first + increment*i
             *
             * The intent is that if tolerance = 0 and this method returns a non-null value, then replacing
             * this vector by an instance of SequenceVector should produce exactly the same double values,
             * in the limit of the accuracy allowed by the floating point values.
             */
            final double first = doubleValue(0);
            double inc = (doubleValue(--i) - first) / i;                              // First estimation of increment.
            if (type == Numbers.DOUBLE || type == Numbers.BIG_DECIMAL) {
                final int pz = Math.max(0, Math.min(i, (int) Math.rint(-first / inc)));   // Presumed index of value zero.
                if (doubleValue(pz) == 0) {
                    final Number value = (pz == i) ? get(pz-1) : get(pz+1);     // Value adjacent to zero.
                    if (value != null && !(value instanceof Float)) {           // Float type is not accurate enough.
                        inc = value.doubleValue();                              // Presumed less subject to rounding errors.
                        if (pz == i) inc = -inc;
                    }
                }
            }
            if (type == Numbers.FLOAT) {
                while (i >= 1) {
                    final float  value = floatValue(i);
                    final double delta = Math.abs(first + inc*i-- - value);
                    final double accur = Math.ulp(value);
                    if (!((accur > tolerance) ? (delta < accur) : (delta <= tolerance))) {  // Use `!` for catching NaN.
                        return null;
                    }
                }
                final float f = (float) inc;
                if (f == inc) return f;                            // Use the java.lang.Float wrapper class if possible.
            } else {
                while (i >= 1) {
                    final double delta = Math.abs(first + inc*i - doubleValue(i--));
                    if (!(delta <= tolerance)) {                   // Use `!` for catching NaN.
                        return null;
                    }
                }
            }
            return inc;
        } catch (ArithmeticException e) {
            warning("increment", e);
        }
        return null;
    }

    /**
     * Returns the minimal and maximal values found in this vector.
     *
     * @return minimal and maximal values found in this vector.
     */
    public NumberRange<?> range() {
        return range(null, size());
    }

    /**
     * Computes the range of values at the indices provided by the given supplier.
     * The default implementation iterates over all {@code double} values, but
     * subclasses should override with a more efficient implementation if possible.
     *
     * @param  indices  supplier of indices of the values to examine for computing the range,
     *                  or {@code null} for the 0, 1, 2, … <var>n</var>-1 sequence.
     * @param  n        number of indices to get from the supplier.
     * @return the range of all values at the given indices.
     */
    NumberRange<?> range(final IntSupplier indices, int n) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        while (--n >= 0) {
            final double value = doubleValue((indices != null) ? indices.getAsInt() : n);
            if (value < min) min = value;
            if (value > max) max = value;
        }
        return NumberRange.create(min, true, max, true);
    }

    /**
     * Returns a view which contain the values of this vector in the given index range.
     * The returned view will contain the values from index {@code lower} inclusive to
     * {@code upper} exclusive.
     *
     * <h4>Implementation note</h4>
     * This method delegates its work
     * <code>{@linkplain #subSampling(int,int,int) subSampling}(lower, 1, upper - lower)</code>.
     * This method is declared final in order to force subclasses to override {@code subSampling(…)} instead.
     *
     * @param  lower  index of the first value to be included in the returned view.
     * @param  upper  index after the last value to be included in the returned view.
     * @return a view of this vector containing values in the given index range.
     * @throws IndexOutOfBoundsException if an index is outside the [0 … {@linkplain #size() size}-1] range.
     */
    @Override
    public final Vector subList(final int lower, final int upper) {
        return subSampling(lower, 1, upper - lower);
    }

    /**
     * Returns a view which contain the values of this vector in a given index range.
     * The returned view will contain the values from index {@code first} inclusive to
     * {@code (first + step*length)} exclusive with index incremented by the given {@code step} value,
     * which can be negative. More specifically the index <var>i</var> in the returned vector will maps
     * the element at index <code>(first + step*<var>i</var>)</code> in this vector.
     *
     * <p>This method does not copy the values. Consequently, any modification to the
     * values of this vector will be reflected in the returned view and vice-versa.</p>
     *
     * @param  first   index of the first value in this vector to be included in the returned view.
     * @param  step    the index increment between values in this vector to be included in the returned view.
     *                 Can be positive, zero or negative.
     * @param  length  the length of the view to be returned. Cannot be greater than
     *                 the length of this vector, except if the {@code step} is zero.
     * @return a view of this vector containing values in the given index range.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first + step*(length-1)}
     *         is outside the [0 … {@linkplain #size() size}-1] range.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Vector subSampling(final int first, final int step, final int length) {
        final int size = size();
        if (step == 1 && first == 0 && length == size) {
            return this;
        }
        final long last = first + step * (length - 1L);
        if (first < 0 || first >= size || last < 0 || last >= size || length < 0) {
            final short key;
            final Object arg1, arg2;
            if (step == 1) {
                key  = Errors.Keys.IllegalRange_2;
                arg1 = first;
                arg2 = last;
            } else {
                key  = Errors.Keys.IllegalArgumentValue_2;
                arg1 = "range";
                arg2 = "[" + first + ':' + step + ':' + last + ']';
            }
            throw new IndexOutOfBoundsException(Errors.format(key, arg1, arg2));
        }
        return createSubSampling(first, step, length);
    }

    /**
     * Implementation of {@link #subSampling(int,int,int)} to be overridden by subclasses.
     * Argument validity must have been verified by the caller.
     */
    Vector createSubSampling(final int first, final int step, final int length) {
        return new SubSampling(first, step, length);
    }

    /**
     * A view over another vector in a range of index.
     */
    private final class SubSampling extends Vector implements Serializable {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 7641036842053528486L;

        /** Index of the first element in the enclosing vector. */
        final int first;

        /** The index increment. May be negative but not zero. */
        final int step;

        /** The length of this vector. */
        final int length;

        /** Creates a new view over the given range. */
        SubSampling(final int first, final int step, final int length) {
            this.first  = first;
            this.step   = step;
            this.length = length;
        }

        /** Returns the backing vector. */
        @Override Vector backingVector() {
            return Vector.this;
        }

        /** Returns the index where to look for the value in the enclosing vector. */
        final int toBacking(final int index) {
            return Objects.checkIndex(index, length) * step + first;
        }

        /** Returns the index where to look for the value in the enclosing vector. */
        @Override int[] toBacking(final int[] index) {
            final int[] ni = new int[index.length];
            for (int j=0; j<ni.length; j++) {
                ni[j] = toBacking(index[j]);
            }
            return ni;
        }

        /** Returns the type of elements in this vector. */
        @Override public Class<? extends Number> getElementType() {
            return Vector.this.getElementType();
        }

        /** Returns whether values are convertible to {@code float} type. */
        @Override public boolean isSinglePrecision() {
            return Vector.this.isSinglePrecision();
        }

        /** Returns the length of this subvector. */
        @Override public int size() {
            return length;
        }

        /** Delegates to the enclosing vector. */
        @Override public boolean isUnsigned()               {return Vector.this.isUnsigned();}
        @Override public boolean isNaN      (int index)     {return Vector.this.isNaN      (toBacking(index));}
        @Override public double  doubleValue(int index)     {return Vector.this.doubleValue(toBacking(index));}
        @Override public float   floatValue (int index)     {return Vector.this.floatValue (toBacking(index));}
        @Override public long    longValue  (int index)     {return Vector.this.longValue  (toBacking(index));}
        @Override public int     intValue   (int index)     {return Vector.this.intValue   (toBacking(index));}
        @Override public short   shortValue (int index)     {return Vector.this.shortValue (toBacking(index));}
        @Override public byte    byteValue  (int index)     {return Vector.this.byteValue  (toBacking(index));}
        @Override public String  stringValue(int index)     {return Vector.this.stringValue(toBacking(index));}
        @Override public Number  get        (int index)     {return Vector.this.get        (toBacking(index));}

        /** Delegates to the enclosing vector. */
        @Override public Number set(final int index, final Number v) {
            final Number old = Vector.this.set(toBacking(index), v);
            modCount++;
            return old;
        }

        /** Delegates to the enclosing vector. */
        @Override Vector createTransform(final double scale, final double offset) {
            return Vector.this.transform(scale, offset).subSampling(first, step, length);
        }

        /** Delegates to the enclosing vector. */
        @Override Vector createSubSampling(int first, int step, final int length) {
            first = toBacking(first);
            step *= this.step;
            return Vector.this.subSampling(first, step, length);
        }

        /** Delegates to the enclosing vector if possible. */
        @Override Vector createConcatenate(final Vector toAppend) {
            if (toAppend instanceof SubSampling && toAppend.backingVector() == Vector.this) {
                final SubSampling other = (SubSampling) toAppend;
                if (other.step == step && other.first == first + step*length) {
                    return Vector.this.subSampling(first, step, length + other.length);
                }
            }
            return super.createConcatenate(toAppend);
        }

        /** Delegates to the enclosing vector */
        @Override NumberRange<?> range(final IntSupplier indices, final int n) {
            if (indices != null) {
                return Vector.this.range(() -> toBacking(indices.getAsInt()), n);
            }
            IntSupplier supplier = null;
            if (first != 0 || step != 1) {
                supplier = new IntSupplier() {
                    private int index = first;
                    @Override public int getAsInt() {
                        final int i = index;
                        index += step;
                        return i;
                    }
                };
            }
            return Vector.this.range(supplier, n);
        }

        /**
         * If the vector cannot be compressed, copies data in a new vector in order to have a smaller vector
         * than the one backing this view. This is advantageous only on the assumption that user do not keep
         * a reference to the original vector after {@link Vector#compress(double)} call.
         */
        @Override
        public Vector compress(final double tolerance) {
            final Vector c = super.compress(tolerance);
            return (c != this) ? c : copy();
        }

        /**
         * Returns a buffer over the sub-section represented by this {@code SubSampling} instance.
         */
        @Override
        public Optional<Buffer> buffer() {
            if (step == 1) {
                Vector.this.buffer().map((b) -> b.position(first).limit(first + length).slice());
            }
            return super.buffer();
        }
    }

    /**
     * If this vector is a view over a subset of another vector, returns the backing vector.
     * Otherwise returns {@code this}. If this method is overridden, it should be together
     * with the {@link #toBacking(int[])} method. This method shall not be overridden when
     * the view transform the values.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    Vector backingVector() {
        return this;
    }

    /**
     * Converts an array of indexes used by this vector to the indexes used by the backing vector.
     * If there is no such backing vector, then returns a clone of the given array.
     * This method must also check index validity.
     *
     * <p>Only subclasses that are views over a subset of this vector will override this method.</p>
     *
     * @param  indices  the indexes given by the user.
     * @return the indexes to use. Must be a new array in order to protect it from user changes.
     * @throws IndexOutOfBoundsException if at least one index is out of bounds.
     */
    int[] toBacking(int[] indices) {
        indices = indices.clone();
        final int length = size();
        for (int i : indices) {
            Objects.checkIndex(i, length);
        }
        return indices;
    }

    /**
     * Returns a view which contains the values of this vector at the given indexes.
     * This method does not copy the values, consequently any modification to the
     * values of this vector will be reflected in the returned view and vice-versa.
     *
     * <p>The indexes do not need to be in any particular order. The same index can be repeated
     * more than once. Thus it is possible to create a vector larger than the original vector.</p>
     *
     * @param  indices  indexes of the values to be returned.
     * @return a view of this vector containing values at the given indexes.
     * @throws IndexOutOfBoundsException if at least one index is out of bounds.
     */
    public Vector pick(int... indices) {
        indices = toBacking(indices);
        final int first, step;
        switch (indices.length) {
            case 0: {
                first = 0;
                step  = 1;
                break;
            }
            case 1: {
                first = indices[0];
                step  = 1;
                break;
            }
            default: {
                int limit;
                first = indices[0];
                limit = indices[1];
                step  = limit - first;
                for (int i=2; i<indices.length; i++) {
                    final int current = indices[i];
                    if (current - limit != step) {
                        return backingVector().new Pick(indices);
                    }
                    limit = current;
                }
                break;
            }
        }
        return subSampling(first, step, indices.length);
    }

    /**
     * A view over another vector at pre-selected indexes.
     */
    private final class Pick extends Vector implements Serializable {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 6574040261355090760L;

        /** The pre-selected indexes. */
        private final int[] indices;

        /**
         * Creates a new view over the values at the given indexes. This constructor
         * does not clone the array; it is caller responsibility to clone it if needed.
         */
        Pick(int[] indices) {
            this.indices = indices;
        }

        /** Returns the backing vector. */
        @Override Vector backingVector() {
            return Vector.this;
        }

        /** Returns the indexes where to look for the value in the enclosing vector. */
        @Override int[] toBacking(final int[] i) {
            final int[] ni = new int[i.length];
            for (int j=0; j<ni.length; j++) {
                ni[j] = indices[i[j]];
            }
            return ni;
        }

        /** Returns the type of elements in this vector. */
        @Override public Class<? extends Number> getElementType() {
            return Vector.this.getElementType();
        }

        /** Returns whether values are convertible to {@code float} type. */
        @Override public boolean isSinglePrecision() {
            return Vector.this.isSinglePrecision();
        }

        /** Delegates to the enclosing vector. */
        @Override public int      size()                {return indices.length;}
        @Override public boolean  isUnsigned()          {return Vector.this.isUnsigned();}
        @Override public boolean  isNaN      (int i)    {return Vector.this.isNaN      (indices[i]);}
        @Override public double   doubleValue(int i)    {return Vector.this.doubleValue(indices[i]);}
        @Override public float    floatValue (int i)    {return Vector.this.floatValue (indices[i]);}
        @Override public long     longValue  (int i)    {return Vector.this.longValue  (indices[i]);}
        @Override public int      intValue   (int i)    {return Vector.this.intValue   (indices[i]);}
        @Override public short    shortValue (int i)    {return Vector.this.shortValue (indices[i]);}
        @Override public byte     byteValue  (int i)    {return Vector.this.byteValue  (indices[i]);}
        @Override public String   stringValue(int i)    {return Vector.this.stringValue(indices[i]);}
        @Override public Number   get        (int i)    {return Vector.this.get        (indices[i]);}

        /** Delegates to the enclosing vector. */
        @Override public Number set(final int i, final Number v) {
            final Number old = Vector.this.set(indices[i], v);
            modCount++;
            return old;
        }

        /** Delegates to the enclosing vector. */
        @Override Vector createTransform(final double scale, final double offset) {
            return Vector.this.transform(scale, offset).pick(indices);
        }

        /** Delegates to the enclosing vector. */
        @Override Vector createSubSampling(int first, final int step, final int length) {
            final int[] ni = new int[length];
            if (step == 1) {
                System.arraycopy(indices, first, ni, 0, length);
            } else for (int j=0; j<length; j++) {
                ni[j] = indices[first];
                first += step;
            }
            return Vector.this.pick(ni);
        }

        /** Concatenates the indexes if possible. */
        @Override Vector createConcatenate(final Vector toAppend) {
            if (toAppend instanceof Pick && toAppend.backingVector() == Vector.this) {
                final int[] other = ((Pick) toAppend).indices;
                final int[] c = Arrays.copyOf(indices, indices.length + other.length);
                System.arraycopy(other, 0, c, indices.length, other.length);
                return Vector.this.pick(c);
            }
            return super.createConcatenate(toAppend);
        }

        /** Delegates to the enclosing vector. */
        @Override NumberRange<?> range(final IntSupplier supplier, final int n) {
            if (supplier != null) {
                return Vector.this.range(() -> indices[supplier.getAsInt()], n);
            } else {
                return Vector.this.range(new IntSupplier() {
                    private int index;
                    @Override public int getAsInt() {
                        return indices[index++];
                    }
                }, n);
            }
        }

        /**
         * If the vector cannot be compressed, copies data in a new vector in order to have a smaller vector
         * than the one backing this view. This is advantageous only on the assumption that user do not keep
         * a reference to the original vector after {@link Vector#compress(double)} call.
         */
        @Override
        public Vector compress(final double tolerance) {
            final Vector c = super.compress(tolerance);
            return (c != this) ? c : copy();
        }
    }

    /**
     * Returns the concatenation of this vector with the given one. Indexes in the [0 … {@link #size() size} - 1]
     * range will map to this vector, while indexes in the [{@code size} … {@code size} + {@code toAppend.size}]
     * range while map to the given vector.
     *
     * @param  toAppend  the vector to concatenate at the end of this vector.
     * @return the concatenation of this vector with the given vector.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Vector concatenate(final Vector toAppend) {
        if (toAppend.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return toAppend;
        }
        return createConcatenate(toAppend);
    }

    /**
     * Implementation of {@link #concatenate(Vector)} after argument has been validated.
     * Provides a more convenient (but non-essential) overriding point for subclasses.
     */
    Vector createConcatenate(final Vector toAppend) {
        return new ConcatenatedVector(this, toAppend);
    }

    /**
     * Returns a vector whose value is the content of this vector repeated <var>count</var> times.
     * The content can be repeated in two different ways:
     *
     * <ul>
     *   <li>If {@code eachValue} is {@code true}, then each value is repeated {@code count} times
     *       before to move to the next value.</li>
     *   <li>If {@code eachValue} is {@code false}, then whole vector is repeated {@code count} times.</li>
     * </ul>
     *
     * This method returns an empty vector if {@code count} is zero and returns {@code this} if {@code count} is one.
     * For other positive {@code count} values, this method returns an unmodifiable view of this vector:
     * changes in this vector are reflected in the repeated vector.
     *
     * <h4>Examples</h4>
     * if {@code vec} contains {@code {1, 2, 3}}, then:
     * <ul>
     *   <li>{@code vec.repeat(true,  4)} returns {@code {1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3}}.</li>
     *   <li>{@code vec.repeat(false, 4)} returns {@code {1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3}}.</li>
     * </ul>
     *
     * @param  eachValue  whether to apply the repetition on each value ({@code true}) or on the whole vector ({@code false}).
     * @param  count      number of repetitions as a positive number (including zero).
     * @return this vector repeated <var>count</var> time.
     *
     * @since 1.0
     *
     * @see #repetitions(int...)
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Vector repeat(final boolean eachValue, final int count) {
        switch (count) {
            case 0: return subList(0, 0);
            case 1: return this;
        }
        ArgumentChecks.ensurePositive("count", count);
        final int size = size();
        return new RepeatedVector(this, eachValue ? count : 1, size, Math.multiplyExact(size, count));
    }

    /**
     * Returns a view which contains the values of this vector in reverse order.
     *
     * <h4>Implementation note</h4>
     * This method delegates its work
     * to <code>{@linkplain #subSampling(int,int,int) subSampling}(size-1, -1, {@linkplain #size() size})</code>.
     * This method is declared final in order to force subclasses to override {@code subSampling(…)} instead.
     *
     * @return the vector values in reverse order.
     */
    public final Vector reverse() {
        final int length = size();
        return (length > 1) ? subSampling(length-1, -1, length) : this;
    }

    /**
     * Returns a view of this vector with all values transformed by the given linear equation.
     * If {@code scale} = 1 and {@code offset} = 0, then this method returns {@code this}.
     * Otherwise this method returns a vector where each value at index <var>i</var> is computed
     * by {@code doubleValue(i)} × {@code scale} + {@code offset}.
     * The values are computed on-the-fly; they are not copied.
     *
     * @param  scale   the scale factor to apply on each value, or 1 if none.
     * @param  offset  the offset to apply on each value, or 0 if none.
     * @return a vector with values computed by {@code doubleValue(i)} × {@code scale} + {@code offset}.
     * @throws IllegalArgumentException if an argument is NaN or infinite.
     *
     * @since 1.0
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Vector transform(final double scale, final double offset) {
        if (scale == 1 && offset == 0) {
            return this;
        }
        ArgumentChecks.ensureFinite("scale",  scale);
        ArgumentChecks.ensureFinite("offset", offset);
        if (scale == 0) {
            return new SequenceVector.Doubles(Double.class, 0, 0, size());
        }
        return createTransform(scale, offset);
    }

    /**
     * Implementation of {@link #createTransform(double, double)} after arguments have been validated.
     * Provides a more convenient (but non-essential) overriding point for subclasses.
     */
    Vector createTransform(final double scale, final double offset) {
        return new LinearlyDerivedVector(this, scale, offset);
    }

    /**
     * Returns a vector with the same data as this vector but encoded in a more compact way,
     * or {@code this} if this method cannot do better than current {@code Vector} instance.
     * Examples:
     *
     * <ul>
     *   <li>Vector is backed by an {@code int[]} array while values could be stored as {@code short} values.</li>
     *   <li>Vector contains increasing or decreasing values with a constant delta between consecutive values.</li>
     * </ul>
     *
     * The returned vector may or may not be backed by the array given to the {@link #create(Object, boolean)} method.
     * Since the returned array may be a copy of {@code this} array, caller should not retain reference to {@code this}
     * or reference to the backing array after this method call (otherwise an unnecessary duplication of data may exist
     * in memory).
     *
     * <h4>When to use</h4>
     * It is usually not worth to compress small arrays. Performance-critical arrays may not be compressed neither.
     * This method is best suited for vectors that may potentially be large and for which the cost of fetching
     * values in that vector is small compared to the calculation performed with the values.
     *
     * @param  tolerance  maximal difference allowed between original and compressed vectors (can be zero).
     * @return a more compact vector with the same data as this vector, or {@code this}.
     */
    @SuppressWarnings({"fallthrough", "ReturnOfCollectionOrArrayField"})
    public Vector compress(final double tolerance) {
        final int length = size();
        final Number inc = increment(tolerance);
        if (inc != null) {
            return createSequence(getElementType(), get(0), inc, length);
        }
        /*
         * Verify if the vector contains only NaN values. This extra check is useful because `increment()`
         * returns null if the array contains NaN. Note that for array of integers, `isNaN(int)` is very
         * efficient and the loop will stop immediately after the first iteration.
         */
        int i = 0;
        do if (i >= length) {
            final Double NaN = Double.NaN;
            return createSequence(getElementType(), NaN, NaN, length);
        } while (isNaN(i++));
        /*
         * Verify if the vector contains repetitions. If yes, then we can keep only a subregion of this vector.
         * The thresholds below are arbitrary; they are used for deciding if it is worth to remove repetitions,
         * keeping in mind that RepeatedVector consumes about 8 words in memory in addition to the base vector.
         * Assuming that RepeatedVector divides the vector length by 2, we need at least 16 integers before to
         * compensate. Another threshold is to verify that we do not see a repetition because some values from
         * the vector beginning appears at the vector end. As an arbitrary threshold, the repetition at vector
         * end must be at least 1/4 of the vector size.
         */
        if (length > 20*Integer.SIZE / getBitCount()) {
            final int[] repetitions = repetitions(MathFunctions.divisors(length));
            switch (repetitions.length) {
                default: if (length - repetitions[1] < length/4) break;               // Otherwise fallthrough.
                case 1:  return new RepeatedVector(this, repetitions, tolerance);
                case 0:  break;
            }
        }
        /*
         * Try to copy the values in a more compact format.
         * We will use a vector backed by IntegerList in order to use only the number of bits needed,
         * unless that amount is exactly the number of bits of a primitive type (8, 16, 32 or 64) in
         * which case using one of the specialized classes in this ArrayVector is more performant.
         */
        final NumberRange<?> range = range();
        if (range != null && !range.isEmpty()) {
            final Number min = range.getMinValue();
            final Number max = range.getMaxValue();
            final boolean isInteger = (min.doubleValue() >= Long.MIN_VALUE &&
                                       max.doubleValue() <= Long.MAX_VALUE &&
                                       isInteger());                                // May scan the vector.
            Vector vec;
            if (isInteger) {
                vec = PackedVector.compress(this, min.longValue(), max.longValue());
                if (vec == null) {
                    vec = ArrayVector.compress(this, min.longValue(), max.longValue());
                }
            } else {
                vec = ArrayVector.compress(this, tolerance);
            }
            if (vec != null) {
                return vec;
            }
        }
        return this;
    }

    /**
     * Logs a warning about an exception that can be safely ignored.
     */
    final void warning(final String method, final RuntimeException e) {
        Logging.recoverableException(getLogger(Loggers.MATH), Vector.class, method, e);
    }

    /**
     * Returns the vector data as a {@code java.nio} buffer.
     * Data are not copied: changes in the buffer are reflected on this vector and vice-versa.
     * Date are provided in their "raw" form. For example, unsigned integers are given as plain {@code int} elements
     * and it is caller responsibility to use {@link Integer#toUnsignedLong(int)} if needed.
     *
     * @return the vector data as a buffer. Absent if this vector is not backed by an array or a buffer.
     *
     * @since 1.0
     */
    public Optional<Buffer> buffer() {
        return Optional.empty();
    }

    /**
     * Copies all values in an array of double precision floating point numbers.
     * This method is for inter-operability with APIs requiring an array of primitive type.
     *
     * <p>The default implementation invokes {@link #doubleValue(int)} for all indices from 0 inclusive
     * to {@link #size()} exclusive. Subclasses may override with more efficient implementation.</p>
     *
     * @return a copy of all floating point values in this vector.
     *
     * @see #doubleValue(int)
     */
    public double[] doubleValues() {
        final double[] array = new double[size()];
        for (int i=0; i<array.length; i++) {
            array[i] = doubleValue(i);
        }
        return array;
    }

    /**
     * Copies all values in an array of single precision floating point numbers.
     * This method is for inter-operability with APIs requiring an array of primitive type.
     *
     * <p>The default implementation invokes {@link #floatValue(int)} for all indices from 0 inclusive
     * to {@link #size()} exclusive. Subclasses may override with more efficient implementation.</p>
     *
     * @return a copy of all floating point values in this vector.
     *
     * @see #floatValue(int)
     * @see #isSinglePrecision()
     */
    public float[] floatValues() {
        final float[] array = new float[size()];
        for (int i=0; i<array.length; i++) {
            array[i] = floatValue(i);
        }
        return array;
    }

    /**
     * Returns a copy of this vector. The returned vector is writable, and changes in that vector has no effect
     * on this original vector. The copy is not a clone since it may not be an instance of the same class.
     *
     * @return a copy of this vector.
     */
    final Vector copy() {
        final Object array;
        final int size = size();
        switch (Numbers.getEnumConstant(getElementType())) {
            case Numbers.DOUBLE: {
                array = doubleValues();
                break;
            }
            case Numbers.FLOAT: {
                array = floatValues();
                if (size != 0 && get(0) instanceof Double) {
                    return createForDecimal((float[]) array);
                }
                break;
            }
            case Numbers.LONG: {
                final long[] data = new long[size];
                for (int i=0; i<size; i++) {
                    data[i] = longValue(i);
                }
                array = data;
                break;
            }
            case Numbers.INTEGER: {
                final int[] data = new int[size];
                for (int i=0; i<size; i++) {
                    data[i] = (int) longValue(i);           // For handling both signed and unsigned integers.
                }
                array = data;
                break;
            }
            case Numbers.SHORT: {
                final short[] data = new short[size];
                for (int i=0; i<size; i++) {
                    data[i] = (short) intValue(i);          // For handling both signed and unsigned integers.
                }
                array = data;
                break;
            }
            case Numbers.BYTE: {
                final byte[] data = new byte[size];
                for (int i=0; i<size; i++) {
                    data[i] = (byte) intValue(i);           // For handling both signed and unsigned integers.
                }
                array = data;
                break;
            }
            default: {
                array = toArray(new Number[size]);
                break;
            }
        }
        return ArrayVector.newInstance(array, isUnsigned());
    }

    /**
     * Returns a string representation of this vector.
     *
     * @return a string representation of this vector.
     *
     * @see #stringValue(int)
     */
    @Override
    public String toString() {
        final StringJoiner buffer = new StringJoiner(", ", "[", "]");
        final int length = size();
        for (int i=0; i<length; i++) {
            buffer.add(stringValue(i));
        }
        return buffer.toString();
    }

    /**
     * The prime number used in hash code computation. Must be the same as the prime number used
     * in {@link Arrays#hashCode(Object[])} computation. More generally, or {@link #hashCode()}
     * implementations must be the same as {@code hashCode(…)} implementations in {@link Arrays}.
     */
    static final int PRIME = 31;

    /**
     * Returns a hash code for the values in this vector. The hash code is computed as if this vector was converted
     * to an array of {@link Number}s, then the {@link Arrays#hashCode(Object[])} method invoked for that array.
     * This contract is defined for compatibility with {@link java.util.List#hashCode()} contract.
     *
     * @return a hash code value for the values in this vector.
     *
     * @since 1.0
     */
    @Override
    public int hashCode() {
        int hash = 1;
        final int size = size();
        for (int i=0; i<size; i++) {
            hash = PRIME * hash + get(i).hashCode();
        }
        return hash;
    }

    /**
     * Returns {@code true} if the given object is a vector containing the same values as this vector.
     * This method performs the comparison as if the two vectors where converted to arrays of {@link Number}s,
     * then the {@link Arrays#equals(Object[], Object[])} method invoked for those arrays.
     *
     * @param  object  the other object to compare with this vector.
     * @return {@code true} if the given object is a vector containing the same values as this vector.
     *
     * @since 1.0
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) return true;
        if (object instanceof Vector) {
            final Vector other = (Vector) object;
            final int size = size();
            if (size == other.size()) {
                return equals(0, size, other, 0);
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this vector in the given range is equal to the specified vector.
     * NaN values are considered equal to all other NaN values, and -0.0 is different than +0.0.
     *
     * @param  lower        index of the first value to compare in this vector, inclusive.
     * @param  upper        index after the last value to compare in this vector.
     * @param  other        the other vector to compare values with this vector. May be {@code this}.
     * @param  otherOffset  index of the first element to compare in the other vector.
     * @return whether values over the specified range of the two vectors are equal.
     * @throws IllegalArgumentException if {@code lower} is greater than {@code upper}.
     */
    boolean equals(int lower, final int upper, final Vector other, int otherOffset) {
        while (lower < upper) {
            if (!get(lower++).equals(other.get(otherOffset++))) {
                return false;
            }
        }
        return true;
    }
}
