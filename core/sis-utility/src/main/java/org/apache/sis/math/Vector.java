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
import java.util.Arrays;
import java.util.AbstractList;
import java.util.RandomAccess;
import java.util.function.IntSupplier;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Numerics;

import static org.apache.sis.util.ArgumentChecks.ensureValidIndex;


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
 * <div class="section">Instantiation</div>
 * Instances of {@code Vector} are usually created by calls to the {@link #create(Object, boolean)} static method.
 * The supplied array is not cloned – changes to the primitive array are reflected in the vector, and vis-versa.
 * Vectors can be a view over a subsection of the given array, or can provide a view of the elements in reverse order,
 * <i>etc</i>. The example below creates a view over a subsection:
 *
 * {@preformat java
 *     float[] array = new float[100];
 *     Vector v = Vector.create(array, false).subList(20, 40)
 *     // At this point, v.doubleValue(0) is equivalent to (double) array[20].
 * }
 *
 * <div class="section">Usage</div>
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
 * Narrowing conversions are allowed if the result can be represented at least approximatively by the target type.
 * For example conversions from {@code double} to {@code float} are always allowed (values that are too large for
 * the {@code float} type are represented by positive of negative infinity), but conversions from {@code long} to
 * {@code short} are allowed only if the value is between {@link Short#MIN_VALUE} and {@link Short#MAX_VALUE} inclusive.
 *
 * <div class="note"><b>Comparison with other API:</b>
 * the above functionalities look like similar functionalities provided by {@link java.nio.ByteBuffer}
 * in standard Java, but they actually serve different purposes. The {@code ByteBuffer} getter methods
 * (for example {@code getShort(int)}, {@code getLong(int)}, <i>etc.</i>) allow to decode a sequence of
 * bytes in a way determined by the type of the value to decode (2 bytes for a {@code short}, 8 bytes
 * for a {@code long}, <i>etc.</i>) – the type of the stored value must be known before to read it.
 * By contrast, this {@code Vector} class is used in situations where <em>the decoding has already been done</em>
 * by the code that <em>create</em> a {@code Vector} object, but the data type may not be known
 * by the code that will <em>use</em> the {@code Vector} object.
 * For example a method performing a numerical calculation may want to see the data as {@code double} values
 * without concern about whether the data were really stored as {@code double} or as {@code float} values.</div>
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see org.apache.sis.util.collection.IntegerList
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
     *   <li>The {@code null} value, in which case {@code null} is returned.</li>
     * </ul>
     *
     * The given argument is not cloned.
     * Consequently changes in the underlying array are reflected in this vector, and vis-versa.
     *
     * <div class="section">Unsigned integers</div>
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
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentClass_2, "array", array.getClass()));
    }

    /**
     * Wraps the given {@code float[]} array in a vector that preserve the string representations in base 10.
     * For example the 0.1 {@code float} value casted to {@code double} normally produces 0.10000000149011612
     * because of the way IEEE 754 arithmetic represents numbers (in base 2 instead than base 10). But the
     * vector returned by this method will convert the 0.1 {@code float} value into the 0.1 {@code double} value.
     * Note that despite the appearance, this is <strong>not</strong> more accurate than the normal cast,
     * because base 10 is not more privileged in nature than base 2.
     *
     * <div class="section">When to use</div>
     * This method can be used when there is good reasons to think that the {@code float} numbers were parsed
     * from decimal representations, for example an ASCII file. There is usually no reason to use this method
     * if the values are the result of some numerical computations.
     *
     * @param  array  the object to wrap in a vector, or {@code null}.
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
        final int t = Numbers.getEnumConstant(type);
        if (t >= Numbers.BYTE && t <= Numbers.LONG) {
            // Use the long type if possible because not all long values can be represented as double.
            return new SequenceVector.Longs(first, increment, length);
        } else {
            return new SequenceVector.Doubles(first, increment, length);
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
     * Returns {@code true} if this vector contains only integer values.
     * This method may iterate over all values for performing this verification.
     *
     * @return {@code true} if this vector contains only integer values.
     */
    public boolean isInteger() {
        if (!Numbers.isInteger(getElementType())) {
            for (int i=size(); --i >= 0;) {
                final double v = doubleValue(i);
                if (v != Math.floor(v)) {
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
     *
     * <p>Unless otherwise noticed in Javadoc, users do not need to care about this information since
     * {@code Vector} methods will perform automatically the operations needed for unsigned integers.</p>
     *
     * @return {@code true} if the integer values shall be interpreted as unsigned values.
     */
    public abstract boolean isUnsigned();

    /**
     * Returns the number of elements in this vector.
     *
     * @return the number of elements in this vector.
     */
    @Override
    public abstract int size();

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
     * @throws NumberFormatException if the value is stored as a {@code String} and can not be parsed.
     */
    public abstract double doubleValue(int index);

    /**
     * Returns the value at the given index as a {@code float}.
     * This method may result in a lost of precision if this vector
     * stores or computes its values with the {@code double} type.
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NullPointerException if the value is {@code null} (never happen if this vector wraps an array of primitive type).
     * @throws NumberFormatException if the value is stored as a {@code String} and can not be parsed.
     */
    public abstract float floatValue(int index);

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
     * @throws NumberFormatException if the value is stored as a {@code String} and can not be parsed.
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
     * @throws NumberFormatException if the value is stored as a {@code String} and can not be parsed.
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
     * @throws NumberFormatException if the value is stored as a {@code String} and can not be parsed.
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
     * @throws NumberFormatException if the value is stored as a {@code String} and can not be parsed.
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
     */
    public abstract String stringValue(int index);

    /**
     * Returns the number at the given index, or {@code null} if none.
     * The object returned by this method is usually an instance of the class returned by {@link #getElementType()},
     * but may also be an instance of a wider type if this is necessary for representing the values.
     *
     * <div class="note"><b>Example:</b>
     * if {@link #getElementType()} returns {@code Byte.class} but {@link #isUnsigned()} returns {@code true},
     * then this method may return instances of {@link Short} since that type is the smallest Java primitive
     * type capable to hold byte values in the [128 … 255] range.</div>
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @return the value at the given index (may be {@code null}).
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NumberFormatException if the value is stored as a {@code String} and can not be parsed.
     */
    @Override
    public abstract Number get(int index);

    /**
     * Sets the number at the given index.
     * The given number should be an instance of the same type than the number returned by {@link #get(int)}.
     *
     * @param  index  the index in the [0 … {@linkplain #size() size}-1] range.
     * @param  value  the value to set at the given index.
     * @return the value previously stored at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NumberFormatException if the previous value was stored as a {@code String} and can not be parsed.
     * @throws ClassCastException if the given value can not be converted to the type expected by this vector.
     * @throws ArrayStoreException if the given value can not be stored in this vector.
     */
    @Override
    public abstract Number set(int index, Number value);

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
        // The sign of the difference shall be the same than the sign of Long.compare(…).
        if ((((isUnsigned() ? Long.compareUnsigned(a, b) : Long.compare(a, b)) ^ inc) & Long.MIN_VALUE) != 0) {
            throw new ArithmeticException();
        }
        return inc;
    }

    /**
     * Returns the increment between all consecutive values if this increment is constant, or {@code null} otherwise.
     * If the returned value is non-null, then the following condition shall hold for all values of <var>i</var> in
     * the [0 … {@link #size()} - 1] range:
     *
     * <blockquote><code>{@linkplain Math#abs(double) abs}({@linkplain #doubleValue(int) doubleValue}(<var>i</var>)
     * - (doubleValue(0) + increment*<var>i</var>)) &lt;= tolerance</code></blockquote>
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
             * formula because some 'long' values can not be represented accurately as 'double' values.
             * The result will be converted to the same type than the vector element type if possible,
             * or the next wider type if the increment is an unsigned value too big for the element type.
             */
            if (type >= Numbers.BYTE && type <= Numbers.LONG && tolerance < 1) {
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
             * For floating point types, we must use the same formula than the one used by SequenceVector:
             *
             *     doubleValue(i)  =  first + increment*i
             *
             * The intend is that if tolerance = 0 and this method returns a non-null value, then replacing
             * this vector by an instance of SequenceVector should produce exactely the same double values.
             */
            if (type >= Numbers.FLOAT && type <= Numbers.DOUBLE) {
                final double first = doubleValue(0);
                final double inc = (doubleValue(--i) - first) / i;
                while (i >= 1) {
                    if (!(Math.abs(first + inc*i - doubleValue(i--)) <= tolerance)) {       // Use '!' for catching NaN.
                        return null;
                    }
                }
                if (type == Numbers.FLOAT) {
                    final float f = (float) inc;
                    if (f == inc) return f;             // Use the java.lang.Float wrapper class if possible.
                }
                return inc;
            }
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
     * <div class="note"><b>Implementation note:</b> this method delegates its work
     * <code>{@linkplain #subSampling(int,int,int) subSampling}(lower, 1, upper - lower)</code>.
     * This method is declared final in order to force subclasses to override {@code subSampling(…)} instead.</div>
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
     * <p>This method does not copy the values. Consequently any modification to the
     * values of this vector will be reflected in the returned view and vis-versa.</p>
     *
     * @param  first   index of the first value to be included in the returned view.
     * @param  step    the index increment in this vector between two consecutive values
     *                 in the returned vector. Can be positive, zero or negative.
     * @param  length  the length of the vector to be returned. Can not be greater than
     *                 the length of this vector, except if the {@code step} is zero.
     * @return a view of this vector containing values in the given index range.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first + step*(length-1)}
     *         is outside the [0 … {@linkplain #size() size}-1] range.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Vector subSampling(final int first, final int step, final int length) {
        if (step == 1 && first == 0 && length == size()) {
            return this;
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
        protected SubSampling(final int first, final int step, final int length) {
            ensureValid(first, step, length);
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
            ensureValidIndex(length, index);
            return index*step + first;
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
    }

    /**
     * If this vector is a view over another vector, returns the backing vector.
     * Otherwise returns {@code this}. If this method is overridden, it should be
     * together with the {@link #toBacking(int[])} method.
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
     * <p>Only subclasses that are views of this vector will override this method.</p>
     *
     * @param  indices  the indexes given by the user.
     * @return the indexes to use. Must be a new array in order to protect it from user changes.
     * @throws IndexOutOfBoundsException if at least one index is out of bounds.
     */
    int[] toBacking(int[] indices) {
        indices = indices.clone();
        final int length = size();
        for (int i : indices) {
            ensureValidIndex(length, i);
        }
        return indices;
    }

    /**
     * Returns a view which contains the values of this vector at the given indexes.
     * This method does not copy the values, consequently any modification to the
     * values of this vector will be reflected in the returned view and vis-versa.
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
     * A view over an other vector at pre-selected indexes.
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
        @Override Vector createSubSampling(int first, final int step, final int length) {
            ensureValid(first, step, length);
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
    }

    /**
     * Ensures that the range created from the given parameters is valid.
     */
    static void ensureValid(final int first, final int step, final int length) {
        if (length < 0) {
            final short key;
            final Object arg1, arg2;
            if (step == 1) {
                key  = Errors.Keys.IllegalRange_2;
                arg1 = first;
                arg2 = first + length;
            } else {
                key  = Errors.Keys.IllegalArgumentValue_2;
                arg1 = "range";
                arg2 = "[" + first + ':' + step + ':' + (first + step*length) + ']';
            }
            throw new IllegalArgumentException(Errors.format(key, arg1, arg2));
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
     * Implementation of {@link #concatenate(Vector)} to be overridden by subclasses.
     */
    Vector createConcatenate(final Vector toAppend) {
        return new ConcatenatedVector(this, toAppend);
    }

    /**
     * Returns a view which contains the values of this vector in reverse order.
     *
     * <div class="note"><b>Implementation note:</b> this method delegates its work
     * to <code>{@linkplain #subSampling(int,int,int) subSampling}(size-1, -1, {@linkplain #size() size})</code>.
     * This method is declared final in order to force subclasses to override {@code subSampling(…)} instead.</div>
     *
     * @return the vector values in reverse order.
     */
    public final Vector reverse() {
        final int length = size();
        return (length > 1) ? subSampling(length-1, -1, length) : this;
    }

    /**
     * Returns a vector with the same data than this vector but encoded in a more compact way,
     * or {@code this} if this method can not do better than current {@code Vector} instance.
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
     * <div class="section">When to use</div>
     * It is usually not worth to compress small arrays. Performance-critical arrays may not be compressed neither.
     * This method is best suited for arrays that may potentially be large and for which the cost of reading that
     * array is small compared to the calculation performed with the values.
     *
     * @param  tolerance  maximal difference allowed between original and compressed vectors (can be zero).
     * @return a more compact vector with the same data than this vector, or {@code this}.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Vector compress(final double tolerance) {
        final int length = size();
        final Number inc = increment(tolerance);
        if (inc != null) {
            return createSequence(get(0), inc, length);
        }
        /*
         * Verify if the vector contains only NaN values. This extra check is useful because 'increment()'
         * returns null if the array contains NaN. Note that for array of integers, 'isNaN(int)' is very
         * efficient and the loop will stop immediately after the first iteration.
         */
        for (int i=0; i<length; i++) {
            if (!isNaN(i)) return this;
        }
        final Double NaN = Numerics.valueOf(Double.NaN);
        return new SequenceVector.Doubles(NaN, NaN, length);
    }

    /**
     * Logs a warning about an exception that can be safely ignored.
     */
    final void warning(final String method, final RuntimeException e) {
        Logging.recoverableException(Logging.getLogger(Loggers.MATH), Vector.class, method, e);
    }

    /**
     * Returns a string representation of this vector.
     *
     * @return a string representation of this vector.
     */
    @Override
    public String toString() {
        final int length = size();
        if (length == 0) {
            return "[]";
        }
        final StringBuilder buffer = new StringBuilder();
        String separator = "[";
        for (int i=0; i<length; i++) {
            buffer.append(separator).append(stringValue(i));
            separator = ", ";
        }
        return buffer.append(']').toString();
    }
}
