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
import org.apache.sis.util.resources.Errors;

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
 * Instances of {@code Vector} are created by calls to the {@link #create(Object)} static method.
 * The supplied array is not cloned – changes to the primitive array are reflected in the vector,
 * and vis-versa.
 *
 * Vectors can be created over a subrange of an array, provides a view of the elements in reverse
 * order, <i>etc</i>. The example below creates a view over a subrange:
 *
 * {@preformat java
 *     Vector v = Vector.create(array).subList(lower, upper)
 * }
 *
 * <div class="note"><b>Note:</b>
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
 * The methods that are most often used after {@code Vector} creation are {@link #size()} and
 * {@link #doubleValue(int)} or {@link #intValue(int)}.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public abstract class Vector extends AbstractList<Number> implements RandomAccess {
    /**
     * Wraps the given object in a vector. The argument should be one of the following:
     *
     * <ul>
     *   <li>An array of a primitive type, like {@code float[]}.</li>
     *   <li>A {@code Vector}, in which case it is returned unchanged.</li>
     *   <li>The {@code null} value, in which case {@code null} is returned.</li>
     * </ul>
     *
     * The given argument is not cloned.
     * Consequently changes in the underlying array are reflected in this vector, and vis-versa.
     *
     * @param  array       the object to wrap in a vector, or {@code null}.
     * @param  isUnsigned  {@code true} if integer types should be interpreted as unsigned integers.
     *         This argument is ignored if the given array is not a {@code byte[]}, {@code short[]},
     *         {@code int[]} or {@code long[]} array.
     * @return the given object wrapped in a vector, or {@code null} if the argument was {@code null}.
     * @throws IllegalArgumentException if the type of the given object is not recognized by the method.
     */
    public static Vector create(final Object array, final boolean isUnsigned) throws IllegalArgumentException {
        if (array instanceof double[]) {
            return new ArrayVector.Double((double[]) array);
        }
        if (array instanceof float[]) {
            return new ArrayVector.Float((float[]) array);
        }
        if (array instanceof long[]) {
            if (isUnsigned) {
                return new ArrayVector.UnsignedLong((long[]) array);
            } else {
                return new ArrayVector.Long((long[]) array);
            }
        }
        if (array instanceof int[]) {
            if (isUnsigned) {
                return new ArrayVector.UnsignedInteger((int[]) array);
            } else {
                return new ArrayVector.Integer((int[]) array);
            }
        }
        if (array instanceof short[]) {
            if (isUnsigned) {
                return new ArrayVector.UnsignedShort((short[]) array);
            } else {
                return new ArrayVector.Short((short[]) array);
            }
        }
        if (array instanceof byte[]) {
            if (isUnsigned) {
                return new ArrayVector.UnsignedByte((byte[]) array);
            } else {
                return new ArrayVector.Byte((byte[]) array);
            }
        }
        if (array == null || array instanceof Vector) {
            return (Vector) array;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalParameterType_2, "array", array.getClass()));
    }

    /**
     * Creates a sequence of numbers in a given range of values using the given increment.
     * The range of values will be {@code first} inclusive to {@code (first + increment*length)} exclusive.
     * Note that the value given by the {@code first} argument is equivalent to a "lowest" or "minimum" value
     * only if the given increment is positive.
     *
     * <p>The {@linkplain #getElementType() element type} will be the smallest type that can be used
     * for storing every values. For example it will be {@code Byte.class} for the range [100:1:120]
     * but will be {@code Double.class} for the range [0:0.1:1].</p>
     *
     * @param  first      the first value, inclusive.
     * @param  increment  the difference between the values at two adjacent indexes.
     * @param  length     the length of the desired vector.
     * @return the given sequence as a vector.
     */
    public static Vector createSequence(final double first, final double increment, final int length) {
        return new SequenceVector(first, increment, length);
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
     * @return the type of elements in this vector.
     */
    public abstract Class<? extends Number> getElementType();

    /**
     * Returns {@code true} if integer values shall be interpreted as unsigned values.
     * Java has no primitive type for unsigned integers, but {@code Vector} implementations
     * can simulate them by applying the necessary bit masks.
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
     * Returns {@code true} if the value at the given index is {@code NaN}.
     *
     * @param  index the index in the [0 … {@linkplain #size size}-1] range.
     * @return {@code true} if the value at the given index is {@code NaN}.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public abstract boolean isNaN(final int index) throws IndexOutOfBoundsException;

    /**
     * Returns the value at the given index as a {@code double}.
     *
     * @param  index the index in the [0 … {@linkplain #size size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to a
     *         {@code double} by an identity or widening conversion.
     */
    public abstract double doubleValue(final int index) throws IndexOutOfBoundsException, ClassCastException;

    /**
     * Returns the value at the given index as a {@code float}.
     * If this vector uses internally a wider type like {@code double}, then this method may
     * cast the value or throw an exception at implementation choice. The general guidelines
     * are:
     *
     * <ul>
     *   <li>If the value is read from a primitive array of a wider type (typically through a
     *       vector created by {@link #create(Object)}, throw an exception because we assume
     *       that the data provider thinks that the extra precision is needed.</li>
     *   <li>If the value is the result of a computation (typically through a vector created
     *       by {@link #createSequence}), cast the value because the calculation accuracy is
     *       often unknown to the vector - and not necessarily its job.</li>
     * </ul>
     *
     * For safety users should either call {@link #doubleValue(int)} in all cases, or call this
     * methods only if the type returned by {@link #getElementType()} has been verified.
     *
     * @param  index the index in the [0 … {@linkplain #size size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to a
     *         {@code float} by an identity or widening conversion.
     */
    public abstract float floatValue(final int index) throws IndexOutOfBoundsException, ClassCastException;

    /**
     * Returns the value at the given index as a {@code long}.
     * If this vector uses internally a wider type, then this method may cast the value
     * or throw an exception according the same guidelines than {@link #floatValue(int)}.
     *
     * @param  index the index in the [0 … {@linkplain #size size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to a
     *         {@code long} by an identity or widening conversion.
     */
    public abstract long longValue(final int index) throws IndexOutOfBoundsException, ClassCastException;

    /**
     * Returns the value at the given index as an {@code int}.
     * If this vector uses internally a wider type, then this method may cast the value
     * or throw an exception according the same guidelines than {@link #floatValue(int)}.
     *
     * @param  index the index in the [0 … {@linkplain #size size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to an
     *         {@code int} by an identity or widening conversion.
     */
    public abstract int intValue(final int index) throws IndexOutOfBoundsException, ClassCastException;

    /**
     * Returns the value at the given index as a {@code short}.
     * If this vector uses internally a wider type, then this method may cast the value
     * or throw an exception according the same guidelines than {@link #floatValue(int)}.
     *
     * @param  index the index in the [0 … {@linkplain #size size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to a
     *         {@code short} by an identity or widening conversion.
     */
    public abstract short shortValue(final int index) throws IndexOutOfBoundsException, ClassCastException;

    /**
     * Returns the value at the given index as a {@code byte}.
     * If this vector uses internally a wider type, then this method may cast the value
     * or throw an exception according the same guidelines than {@link #floatValue(int)}.
     *
     * @param  index the index in the [0 … {@linkplain #size size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to a
     *         {@code byte} by an identity or widening conversion.
     */
    public abstract byte byteValue(final int index) throws IndexOutOfBoundsException, ClassCastException;

    /**
     * Returns the number at the given index, or {@code null} if none.
     *
     * @param  index the index in the [0 … {@linkplain #size size}-1] range.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public abstract Number get(final int index) throws IndexOutOfBoundsException;

    /**
     * Sets the number at the given index.
     *
     * @param  index the index in the [0 … {@linkplain #size size}-1] range.
     * @param  value the value to set at the given index.
     * @return the value previously stored at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws ArrayStoreException if the given value can not be stored in this vector.
     */
    @Override
    public abstract Number set(final int index, final Number value)
            throws IndexOutOfBoundsException, ArrayStoreException;

    /**
     * If this vector is a view over an other vector, returns the backing vector.
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
     * @param  index the indexes given by the user.
     * @return the indexes to use. Must be a new array in order to protect it from user changes.
     * @throws IndexOutOfBoundsException if at least one index is out of bounds.
     */
    int[] toBacking(int[] index) throws IndexOutOfBoundsException {
        index = index.clone();
        final int length = size();
        for (int i : index) {
            ensureValidIndex(length, i);
        }
        return index;
    }

    /**
     * Returns a view which contains the values of this vector at the given indexes.
     * This method does not copy the values, consequently any modification to the
     * values of this vector will be reflected in the returned view and vis-versa.
     *
     * <p>The indexes don't need to be in any particular order. The same index can be repeated
     * more than once. Thus it is possible to create a vector larger than the original vector.</p>
     *
     * @param  index index of the values to be returned.
     * @return a view of this vector containing values at the given indexes.
     * @throws IndexOutOfBoundsException if at least one index is out of bounds.
     */
    public Vector view(int... index) throws IndexOutOfBoundsException {
        index = toBacking(index);
        final int first, step;
        switch (index.length) {
            case 0: {
                first = 0;
                step  = 1;
                break;
            }
            case 1: {
                first = index[0];
                step  = 1;
                break;
            }
            default: {
                int limit;
                first = index[0];
                limit = index[1];
                step  = limit - first;
                for (int i=2; i<index.length; i++) {
                    final int current = index[i];
                    if (current - limit != step) {
                        return backingVector().new View(index);
                    }
                    limit = current;
                }
                break;
            }
        }
        return subList(first, step, index.length);
    }

    /**
     * Returns a view which contains the values of this vector in reverse order. This method delegates its work
     * to <code>{@linkplain #subList(int,int,int) subList}(size-1, -1, {@linkplain #size() size})</code>.
     * It is declared final in order to force every subclasses to override the later method instead than this one.
     *
     * @return the vector values in reverse order.
     */
    public final Vector reverse() {
        final int length = size();
        return (length != 0) ? subList(length-1, -1, length) : this;
    }

    /**
     * Returns a view which contain the values of this vector in the given index range.
     * The returned view will contain the values from index {@code lower} inclusive to
     * {@code upper} exclusive.
     *
     * <p>This method delegates its work to <code>{@linkplain #subList(int,int,int) subList}(lower,
     * 1, upper-lower)</code>. It is declared final in order to force every subclasses to override
     * the later method instead than this one.</p>
     *
     * @param  lower index of the first value to be included in the returned view.
     * @param  upper index after the last value to be included in the returned view.
     * @return a view of this vector containing values in the given index range.
     * @throws IndexOutOfBoundsException If an index is outside the [0 … {@linkplain #size size}-1] range.
     */
    @Override
    public final Vector subList(final int lower, final int upper) throws IndexOutOfBoundsException {
        return subList(lower, 1, upper - lower);
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
     * @param  first  index of the first value to be included in the returned view.
     * @param  step   the index increment in this vector between two consecutive values
     *                in the returned vector. Can be positive, zero or negative.
     * @param  length the length of the vector to be returned. Can not be greater than
     *                the length of this vector, except if the {@code step} is zero.
     * @return a view of this vector containing values in the given index range.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first + step*(length-1)}
     *         is outside the [0 … {@linkplain #size size}-1] range.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Vector subList(final int first, final int step, final int length) throws IndexOutOfBoundsException {
        if (step == 1 && first == 0 && length == size()) {
            return this;
        }
        return createSubList(first, step, length);
    }

    /**
     * Implementation of {@link #subList(int,int,int)} to be overridden by subclasses.
     */
    Vector createSubList(final int first, final int step, final int length) {
        return new SubList(first, step, length);
    }

    /**
     * Returns the concatenation of this vector with the given one. Indexes in the [0 … {@linkplain #size size}-1]
     * range will map to this vector, while indexes in the [size … size + toAppend.size] range while map to the
     * given vector.
     *
     * @param  toAppend the vector to concatenate at the end of this vector.
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
     * A view over an other vector at pre-selected indexes.
     */
    private final class View extends Vector implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 6574040261355090760L;

        /**
         * The pre-selected indexes.
         */
        private final int[] index;

        /**
         * Creates a new view over the values at the given indexes. This constructor
         * does not clone the array; it is caller responsibility to clone it if needed.
         */
        public View(int[] index) {
            this.index = index;
        }

        /** Returns the backing vector. */
        @Override Vector backingVector() {
            return Vector.this;
        }

        /** Returns the indexes where to look for the value in the enclosing vector. */
        @Override int[] toBacking(final int[] i) throws IndexOutOfBoundsException {
            final int[] ni = new int[i.length];
            for (int j=0; j<ni.length; j++) {
                ni[j] = index[i[j]];
            }
            return ni;
        }

        /** Returns the type of elements in this vector. */
        @Override public Class<? extends Number> getElementType() {
            return Vector.this.getElementType();
        }

        /** Returns whether the type should be interpreted as an unsigned integer type. */
        @Override public boolean isUnsigned() {
            return Vector.this.isUnsigned();
        }

        /** Returns the length of this view. */
        @Override public int size() {
            return index.length;
        }

        /** Delegates to the enclosing vector. */
        @Override public boolean isNaN(final int i) {
            return Vector.this.isNaN(index[i]);
        }

        /** Delegates to the enclosing vector. */
        @Override public double doubleValue(final int i) {
            return Vector.this.doubleValue(index[i]);
        }

        /** Delegates to the enclosing vector. */
        @Override public float floatValue(final int i) {
            return Vector.this.floatValue(index[i]);
        }

        /** Delegates to the enclosing vector. */
        @Override public long longValue(final int i) {
            return Vector.this.longValue(index[i]);
        }

        /** Delegates to the enclosing vector. */
        @Override public int intValue(final int i) {
            return Vector.this.intValue(index[i]);
        }

        /** Delegates to the enclosing vector. */
        @Override public short shortValue(final int i) {
            return Vector.this.shortValue(index[i]);
        }

        /** Delegates to the enclosing vector. */
        @Override public byte byteValue(final int i) {
            return Vector.this.byteValue(index[i]);
        }

        /** Delegates to the enclosing vector. */
        @Override public Number get(final int i) {
            return Vector.this.get(index[i]);
        }

        /** Delegates to the enclosing vector. */
        @Override public Number set(final int i, final Number value) {
            return Vector.this.set(index[i], value);
        }

        /** Delegates to the enclosing vector. */
        @Override Vector createSubList(int first, final int step, final int length) {
            ensureValid(first, step, length);
            final int[] ni = new int[length];
            if (step == 1) {
                System.arraycopy(index, first, ni, 0, length);
            } else for (int j=0; j<length; j++) {
                ni[j] = index[first];
                first += step;
            }
            return Vector.this.view(ni);
        }

        /** Concatenates the indexes if possible. */
        @Override Vector createConcatenate(final Vector toAppend) {
            if (toAppend instanceof View && toAppend.backingVector() == Vector.this) {
                final int[] other = ((View) toAppend).index;
                final int[] c = Arrays.copyOf(index, index.length + other.length);
                System.arraycopy(other, 0, c, index.length, other.length);
                return Vector.this.view(c);
            }
            return super.createConcatenate(toAppend);
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
     * A view over an other vector in a range of index.
     */
    private final class SubList extends Vector implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 7641036842053528486L;

        /**
         * Index of the first element in the enclosing vector.
         */
        private final int first;

        /**
         * The index increment. May be negative but not zero.
         */
        private final int step;

        /**
         * The length of this vector.
         */
        private final int length;

        /**
         * Creates a new view over the given range.
         */
        protected SubList(final int first, final int step, final int length) {
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
        private int toBacking(final int index) throws IndexOutOfBoundsException {
            ensureValidIndex(length, index);
            return index*step + first;
        }

        /** Returns the index where to look for the value in the enclosing vector. */
        @Override int[] toBacking(final int[] index) throws IndexOutOfBoundsException {
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

        /** Returns whether the type should be interpreted as an unsigned integer type. */
        @Override public boolean isUnsigned() {
            return Vector.this.isUnsigned();
        }

        /** Returns the length of this subvector. */
        @Override public int size() {
            return length;
        }

        /** Delegates to the enclosing vector. */
        @Override public boolean isNaN(final int index) {
            return Vector.this.isNaN(toBacking(index));
        }

        /** Delegates to the enclosing vector. */
        @Override public double doubleValue(final int index) {
            return Vector.this.doubleValue(toBacking(index));
        }

        /** Delegates to the enclosing vector. */
        @Override public float floatValue(final int index) {
            return Vector.this.floatValue(toBacking(index));
        }

        /** Delegates to the enclosing vector. */
        @Override public long longValue(final int index) {
            return Vector.this.longValue(toBacking(index));
        }

        /** Delegates to the enclosing vector. */
        @Override public int intValue(final int index) {
            return Vector.this.intValue(toBacking(index));
        }

        /** Delegates to the enclosing vector. */
        @Override public short shortValue(final int index) {
            return Vector.this.shortValue(toBacking(index));
        }

        /** Delegates to the enclosing vector. */
        @Override public byte byteValue(final int index) {
            return Vector.this.byteValue(toBacking(index));
        }

        /** Delegates to the enclosing vector. */
        @Override public Number get(final int index) {
            return Vector.this.get(toBacking(index));
        }

        /** Delegates to the enclosing vector. */
        @Override public Number set(final int index, final Number value) {
            return Vector.this.set(toBacking(index), value);
        }

        /** Delegates to the enclosing vector. */
        @Override Vector createSubList(int first, int step, final int length) {
            first = toBacking(first);
            step *= this.step;
            return Vector.this.subList(first, step, length);
        }

        /** Delegates to the enclosing vector if possible. */
        @Override Vector createConcatenate(final Vector toAppend) {
            if (toAppend instanceof SubList && toAppend.backingVector() == Vector.this) {
                final SubList other = (SubList) toAppend;
                if (other.step == step && other.first == first + step*length) {
                    return Vector.this.createSubList(first, step, length + other.length);
                }
            }
            return super.createConcatenate(toAppend);
        }
    }
}
