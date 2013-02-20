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
package org.apache.sis.util.collection;

import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.AbstractSet;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import net.jcip.annotations.NotThreadSafe;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Range;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.Numbers.*;


/**
 * An ordered set of ranges where overlapping ranges are merged.
 * When a range is {@linkplain #add(Range) added}, {@code RangeSet} first looks for an existing
 * range overlapping the specified range. If overlapping ranges are found, ranges are merged as
 * of {@link Range#union(Range)}. Consequently, adding ranges may in some circumstances
 * <strong>reduce</strong> the {@linkplain #size() size} of this set.
 * Conversely, {@linkplain #remove(Object) removing} ranges may <strong>increase</strong>
 * the size of this set as a result of executing the {@link Range#subtract(Range)} operation.
 *
 * <p>For efficiency reasons, this set stores the range values in a Java array of primitive type if
 * possible. The instances given in argument to the {@link #add(Range)} method are not retained by
 * this class. Ranges are recreated during iterations by calls to the {@link #newRange(Comparable,
 * Comparable)} method. Subclasses can override that method if they need to customize the range
 * objects to be created.</p>
 *
 * {@section Current limitation}
 * The current implementation can store only ranges having inclusive minimal value and exclusive
 * maximal value. If a user needs to store other kind of ranges, then he needs to subclass this
 * {@code RangeSet} class and override the {@link #add(Range)}, {@link #remove(Object)} and
 * {@link newRange(Comparable, Comparable)} methods.
 *
 * @param <E> The type of range elements.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 *
 * @see Range
 */
@NotThreadSafe
public class RangeSet<E extends Comparable<? super E>> extends AbstractSet<Range<E>>
        implements CheckedContainer<Range<E>>, SortedSet<Range<E>>, Cloneable, Serializable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6085227672036239981L;

    /**
     * The range comparator returned by {@link RangeSet#comparator()}. This comparator
     * is defined for compliance with the {@link SortedSet} contract, but is not not
     * used by the {@code RangeSet} implementation.
     *
     * <p>This comparator can order non-ambiguous ranges: the minimum and maximum values
     * of one range shall both be smaller, equal or greater than the values of the other
     * range. In case of ambiguity (when a range in included in the other range), this
     * comparator throws an exception. Such ambiguities should not happen in sequences
     * of ranges created by {@code RangeSet}.</p>
     *
     * @param <E> The type of range elements.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3 (derived from geotk-2.0)
     * @version 0.3
     * @module
     */
    private static final class Compare<E extends Comparable<? super E>>
            implements Comparator<Range<E>>, Serializable
    {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -3710903977144041225L;

        /**
         * The singleton instance, as a raw type in order to allow
         * {@link RangeSet#comparator()} to return the same instance for all types.
         */
        @SuppressWarnings("rawtypes")
        static final Compare INSTANCE = new Compare();

        /**
         * Constructor for the singleton instance only.
         */
        private Compare() {
        }

        /**
         * Compares the given range instance.
         * See class-javadoc for more information.
         */
        @Override
        public int compare(final Range<E> r1, final Range<E> r2) {
            int cmin = r1.getMinValue().compareTo(r2.getMinValue());
            int cmax = r1.getMaxValue().compareTo(r2.getMaxValue());
            if (cmin == 0) {
                final boolean included = r1.isMinIncluded();
                if (r2.isMinIncluded() != included) {
                    cmin = included ? -1 : +1;
                }
            }
            if (cmax == 0) {
                final boolean included = r1.isMaxIncluded();
                if (r2.isMaxIncluded() != included) {
                    cmax = included ? +1 : -1;
                }
            }
            if (cmin == cmax) return cmax; // Easy case: min and max are both greater, smaller or eq.
            if (cmin == 0)    return cmax; // Easy case: only max value differ.
            if (cmax == 0)    return cmin; // Easy case: only min value differ.
            // One range is included in the other.
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UndefinedOrderingForElements_2, r1, r2));
        }

        /**
         * Returns the singleton instance on deserialization.
         */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    };

    /**
     * The type of elements in the ranges. If the element are numbers,
     * then the value is the wrapper type (not the primitive type).
     *
     * @see #getElementType()
     */
    final Class<E> elementType;

    /**
     * The primitive type, as one of {@code DOUBLE}, {@code FLOAT}, {@code LONG}, {@code INTEGER},
     * {@code SHORT}, {@code BYTE}, {@code CHARACTER} enumeration. If the {@link #elementType} is
     * not the wrapper of a primitive type, then this field value is {@code OTHER}.
     */
    private final byte elementCode;

    /**
     * The array of ranges. It may be either an array of Java primitive type like {@code int[]} or
     * {@code float[]}, or an array of {@link Comparable} elements. All elements at even indices
     * are minimal values, and all elements at odd indices are maximal values. Elements in this
     * array must be strictly increasing without duplicated values.
     */
    private Object array;

    /**
     * The length of valid elements in the {@linkplain #array}. Since the array contains both
     * the minimum and maximum values, its length is twice the number of ranges in this set.
     *
     * @see #size()
     */
    private transient int length;

    /**
     * The amount of modifications applied on the range {@linkplain #array}.
     * Used for checking concurrent modifications.
     */
    private transient int modCount;

    /**
     * Constructs an initially empty set of ranges.
     * This constructor is provided for sub-classing only.
     * Client code should use the static {@link #create(Class)} method instead.
     *
     * @param elementType The type of the range elements.
     */
    protected RangeSet(final Class<E> elementType) {
        ArgumentChecks.ensureNonNull("elementType", elementType);
        // Following assertion may fail only if the user bypass the parameterized type checks.
        assert Comparable.class.isAssignableFrom(elementType) : elementType;
        this.elementType = elementType;
        elementCode = getEnumConstant(elementType);
    }

    /**
     * Constructs an initially empty set of ranges.
     *
     * @param  <E> The type of range elements.
     * @param  elementType The type of the range elements.
     * @return A new range set for range elements of the given type.
     */
    @SuppressWarnings("unchecked")
    public static <E extends Comparable<? super E>> RangeSet<E> create(final Class<E> elementType) {
        ArgumentChecks.ensureNonNull("elementType", elementType);
        if (Number.class.isAssignableFrom(elementType)) {
            return new Numeric(elementType);
        }
        return new RangeSet<>(elementType);
    }

    /**
     * Returns the type of elements in this collection.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<Range<E>> getElementType() {
        return (Class) Range.class;
    }

    /**
     * Returns the comparator associated with this sorted set.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"}) // Because we share the same static instance.
    public Comparator<Range<E>> comparator() {
        return Compare.INSTANCE;
    }

    /**
     * Removes all elements from this set of ranges.
     */
    @Override
    public void clear() {
        if (array instanceof Object[]) {
            Arrays.fill((Object[]) array, 0, length, null); // Let GC do its job.
        }
        length = 0;
        modCount++;
    }

    /**
     * Returns the number of ranges in this set.
     */
    @Override
    public int size() {
        assert (length & 1) == 0; // Length must be even.
        return length >>> 1;
    }

    /**
     * Unconditionally copies the internal array in a new array having just the required length.
     */
    private void reallocate() {
        if (length == 0) {
            array = null;
        } else {
            final Object oldArray = array;
            array = Array.newInstance(oldArray.getClass().getComponentType(), length);
            System.arraycopy(oldArray, 0, array, 0, length);
        }
    }

    /**
     * Trims this set to the minimal amount of memory required for holding its data.
     * This method may be invoked after all elements have been {@linkplain #add(Range) added}
     * in order to free unused memory.
     */
    public final void trimToSize() {
        // This method is final because equals(Object) and other methods rely on this behavior.
        if (array != null && Array.getLength(array) != length) {
            reallocate(); // Will set the array to null if length == 0.
            assert isSorted();
        }
    }

    /**
     * Inserts two values at the given index. The underlying {@linkplain #array} shall be
     * non-null before this method is invoked. This method increases the array size as needed.
     *
     * @param lower The index where to insert the values.
     * @param minValue The first value to insert.
     * @param maxValue The second value to insert.
     */
    private void insertAt(final int lower, final E minValue, final E maxValue) {
        final Object oldArray = array;
        final int capacity = Array.getLength(oldArray);
        if (length + 2 > capacity) {
            array = Array.newInstance(oldArray.getClass().getComponentType(), 2*Math.max(capacity, 8));
            System.arraycopy(oldArray, 0, array, 0, lower);
        }
        System.arraycopy(oldArray, lower, array, lower+2, length-lower);
        Array.set(array, lower,   minValue);
        Array.set(array, lower+1, maxValue);
        length += 2;
        modCount++;
    }

    /**
     * Removes the values in the given range. The underlying {@linkplain #array} shall be
     * non-null before this method is invoked.
     *
     * @param lower First value to remove, inclusive.
     * @param upper Last value to remove, exclusive.
     */
    private void removeAt(final int lower, final int upper) {
        final int oldLength = length;
        System.arraycopy(array, upper, array, lower, oldLength - upper);
        length -= (upper - lower);
        if (array instanceof Object[]) {
            // Clear references so the garbage collector can do its job.
            Arrays.fill((Object[]) array, length, oldLength, null);
        }
        modCount++;
    }

    /**
     * Returns {@code true} if the element in the array are sorted.
     * This method is used for assertions only. The array shall be
     * trimmed to size before this method is invoked.
     */
    @SuppressWarnings("unchecked")
    private boolean isSorted() {
        switch (elementCode) {
            case DOUBLE:    return ArraysExt.isSorted((double[]) array, true);
            case FLOAT:     return ArraysExt.isSorted((float[])  array, true);
            case LONG:      return ArraysExt.isSorted((long[])   array, true);
            case INTEGER:   return ArraysExt.isSorted((int[])    array, true);
            case SHORT:     return ArraysExt.isSorted((short[])  array, true);
            case BYTE:      return ArraysExt.isSorted((byte[])   array, true);
            case CHARACTER: return ArraysExt.isSorted((char[])   array, true);
            default:        return ArraysExt.isSorted((E[])      array, true);
        }
    }

    /**
     * Returns the index of {@code value} in {@link #array}. This method delegates to
     * one of {@code Arrays.binarySearch} methods, depending on element primary type.
     *
     * @param value The value to search.
     * @param n The length of the array to consider.
     */
    private int binarySearch(final E value, final int n) {
        switch (elementCode) {
            case DOUBLE:    return Arrays.binarySearch((double[]) array, 0, n, ((Double)    value).doubleValue());
            case FLOAT:     return Arrays.binarySearch((float []) array, 0, n, ((Float)     value).floatValue ());
            case LONG:      return Arrays.binarySearch((long  []) array, 0, n, ((Long)      value).longValue  ());
            case INTEGER:   return Arrays.binarySearch((int   []) array, 0, n, ((Integer)   value).intValue   ());
            case SHORT:     return Arrays.binarySearch((short []) array, 0, n, ((Short)     value).shortValue ());
            case BYTE:      return Arrays.binarySearch((byte  []) array, 0, n, ((Byte)      value).byteValue  ());
            case CHARACTER: return Arrays.binarySearch((char  []) array, 0, n, ((Character) value).charValue  ());
            default:        return Arrays.binarySearch((Object[]) array, 0, n,              value);
        }
    }

    /**
     * Ensures that the given minimum value is not greater than the maximum value.
     * This method is used for argument checks.
     */
    private static <E extends Comparable<? super E>> void ensureOrdered(final E minValue, final E maxValue) {
        if (minValue.compareTo(maxValue) > 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, minValue, maxValue));
        }
    }

    /**
     * Ensures that the given range has supported <cite>include</cite> and
     * <cite>exclude</cite> attributes.
     */
    private void ensureSupported(final Range<E> range) throws IllegalArgumentException {
        if (!range.isMinIncluded() || range.isMaxIncluded()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2,
                    range.getMinValue(), range.getMaxValue()));
        }
    }

    /**
     * Adds a range to this set. If the specified range overlaps existing ranges,
     * then the existing ranges will be merged as of {@link Range#union(Range)}.
     * In other words, invoking this method may <strong>reduce</strong> the
     * {@linkplain #size() size} of this set.
     *
     * <p>The default implementation does nothing if the given range {@linkplain Range#isEmpty()
     * is empty}, or delegates to {@link #add(Comparable, Comparable)} otherwise.</p>
     *
     * @param  range The range to add.
     * @return {@code true} if this set changed (either in size or in values) as a result of this method call.
     * @throws IllegalArgumentException If the given range uses unsupported <cite>include</cite> or
     *         <cite>exclude</cite> attributes.
     */
    @Override
    public boolean add(final Range<E> range) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("range", range);
        if (range.isEmpty()) {
            return false;
        }
        ensureSupported(range);
        return add(range.getMinValue(), range.getMaxValue());
    }

    /**
     * Adds a range of values to this set. If the specified range overlaps existing ranges, then
     * the existing ranges will be merged. This may result in smaller {@linkplain #size() size}
     * of this set.
     *
     * @param  minValue The minimal value, inclusive.
     * @param  maxValue The maximal value, exclusive.
     * @return {@code true} if this set changed (either in size or in values) as a result of this method call.
     * @throws IllegalArgumentException if {@code minValue} is greater than {@code maxValue}.
     */
    public boolean add(final E minValue, final E maxValue) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("minValue", minValue);
        ArgumentChecks.ensureNonNull("maxValue", maxValue);
        /*
         * If this set is initially empty, just store the given range directly.
         */
        if (array == null) {
            ensureOrdered(minValue, maxValue);
            Class<?> type = elementType;
            if (type != Boolean.class) { // Because there is no Arrays.binarySearch(boolean[], …) method.
                type = wrapperToPrimitive(type);
            }
            array = Array.newInstance(type, 8);
            Array.set(array, 0, minValue);
            Array.set(array, 1, maxValue);
            length = 2;
            modCount++;
            return true;
        }
        final int modCountChk = modCount;
        int i1 = binarySearch(maxValue, length);
        int i0 = binarySearch(minValue, (i1 >= 0) ? i1 + 1 : ~i1);
        if (i0 < 0) {
            i0 = ~i0; // Really tild operator, not minus sign.
            if ((i0 & 1) == 0) {
                /*
                 * If the "insertion point" is outside any existing range, if the maximum value
                 * is not present neither in this set (i1 < 0) and if its insertion point is the
                 * same, then insert the new range in the space between two existing ranges.
                 */
                if (i0 == ~i1) { // Includes the (i0 == length) case.
                    ensureOrdered(minValue, maxValue);
                    insertAt(i0, minValue, maxValue);
                    return true;
                }
                /*
                 * Expand an existing range in order to contains the new minimal value.
                 *
                 *   index:      0     1        2        3          4        5
                 *   range:      ███████    ◾◾◾◾██████████          ██████████
                 *   minValue:              │ (insertion point i0 == 2)
                 */
                Array.set(array, i0, minValue);
                modCount++;
            }
        }
        /*
         * If the minimal value "insertion point" is an odd index, this means that the value is
         * inside an existing range. In such case, just move to the beginning of the existing range.
         *
         *   index:      0     1        2        3          4        5
         *   range:      ███████        ██████████          ██████████
         *   minValue:                       │ (insertion point i0 == 3)
         *   moving i0:                 ├────┘
         */
        i0 &= ~1; // Equivalent to executing i0-- only when i0 is odd.
        if (i1 < 0) {
            i1 = ~i1; // Really tild operator, not minus sign.
            if ((i1 & 1) == 0) {
                /*
                 * If the "insertion point" is outside any existing range, expand the previous
                 * range in order to contain the new maximal value. Note that we known that the
                 * given range overlaps the previous range, otherwise the (i0 == ~i1) block above
                 * would have been executed.
                 *
                 *   index:      0     1        2        3          4        5
                 *   range:      ███████        ██████████◾◾◾◾      ██████████
                 *   minValue:                               │ (insertion point i1 == 4)
                 */
                Array.set(array, --i1, maxValue);
                modCount++;
            }
            // At this point, i1 is guaranteed to be odd.
        }
        /*
         * Ensure that the index is odd. This means that if the maximum value is the begining of an
         * existing range, then we move to the end of that range. Otherwise the index is unchanged.
         */
        i1 |= 1; // Equivalent to executing i1++ only if i1 is even.
        /*
         * At this point, the index of the [minValue … maxValue] range is now [i0 … i1].
         * Remove everything between i0 and i1, excluding i0 and i1 themselves.
         */
        assert get(i0).compareTo(maxValue) <= 0;
        assert get(i1).compareTo(minValue) >= 0;
        final int n = i1 - (++i0);
        if (n != 0) {
            removeAt(i0, i1);
        }
        assert (Array.getLength(array) >= length) && (length & 1) == 0 : length;
        return modCountChk != modCount;
    }

    /**
     * Removes a range from this set. If the specified range is inside an existing range, then the
     * existing range may be splitted in two smaller ranges as of {@link Range#subtract(Range)}.
     * In other words, invoking this method may <strong>increase</strong> the
     * {@linkplain #size() size} of this set.
     *
     * <p>The default implementation does nothing if the given range {@linkplain Range#isEmpty()
     * is empty}, or delegates to {@link #remove(Comparable, Comparable)} otherwise.</p>
     *
     * @param  object The range to remove.
     * @return {@code true} if this set changed (either in size or in values) as a result of this method call.
     * @throws IllegalArgumentException If the given range uses unsupported <cite>include</cite> or
     *         <cite>exclude</cite> attributes.
     */
    @Override
    public boolean remove(final Object object) {
        if (object instanceof Range<?>) {
            @SuppressWarnings("unchecked") // Type will actally be checked on the line after.
            final Range<E> range = (Range<E>) object;
            if (range.getElementType() == elementType) {
                ensureSupported(range);
                return remove(range.getMinValue(), range.getMaxValue());
            }
        }
        return false;
    }

    /**
     * Removes a range of values to this set. If the specified range in inside an existing ranges,
     * then the existing range may be splitted in two smaller ranges. This may result in greater
     * {@linkplain #size() size} of this set.
     *
     * @param  minValue The minimal value, inclusive.
     * @param  maxValue The maximal value, exclusive.
     * @return {@code true} if this set changed (either in size or in values) as a result of this method call.
     * @throws IllegalArgumentException if {@code minValue} is greater than {@code maxValue}.
     */
    public boolean remove(final E minValue, final E maxValue) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Returns the value at the specified index. Even index are lower bounds, while odd index
     * are upper bounds. The index validity must have been checked before this method is invoked.
     */
    final E get(final int index) {
        assert (index >= 0) && (index < length) : index;
        return elementType.cast(Array.get(array, index));
    }

    /**
     * Returns a {@linkplain Range#getMinValue() range minimum value} as a {@code double}.
     * The {@code index} can be any value from 0 inclusive to the set {@link #size() size}
     * exclusive. The returned values always increase with {@code index}.
     *
     * @param  index The range index, from 0 inclusive to {@link #size() size} exclusive.
     * @return The minimum value for the range at the specified index.
     * @throws IndexOutOfBoundsException if {@code index} is out of bounds.
     * @throws ClassCastException if range elements are not convertible to numbers.
     *
     * @see org.apache.sis.measure.NumberRange#getMinDouble()
     */
    public final double getMinDouble(int index)
            throws IndexOutOfBoundsException, ClassCastException
    {
        if ((index *= 2) >= length) {
            throw new IndexOutOfBoundsException();
        }
        return Array.getDouble(array, index);
    }

    /**
     * Returns a {@linkplain Range#getMaxValue() range maximum value} as a {@code double}.
     * The {@code index} can be any value from 0 inclusive to the set's {@link #size size}
     * exclusive. The returned values always increase with {@code index}.
     *
     * @param  index The range index, from 0 inclusive to {@link #size size} exclusive.
     * @return The maximum value for the range at the specified index.
     * @throws IndexOutOfBoundsException if {@code index} is out of bounds.
     * @throws ClassCastException if range elements are not convertible to numbers.
     *
     * @see org.apache.sis.measure.NumberRange#getMaxDouble()
     */
    public final double getMaxDouble(int index)
            throws IndexOutOfBoundsException, ClassCastException
    {
        if ((index *= 2) >= length) {
            throw new IndexOutOfBoundsException();
        }
        return Array.getDouble(array, index + 1);
    }

    /**
     * If the specified value is inside a range, returns the index of this range.
     * Otherwise, returns {@code -1}.
     *
     * @param  value The value to search.
     * @return The index of the range which contains this value, or -1 if there is no such range.
     */
    public int indexOfRange(final E value) {
        int index = binarySearch(value, length);
        if (index < 0) {
            // Found an insertion point. Make sure that the insertion
            // point is inside a range (i.e. before the maximum value).
            index = ~index; // Tild sign, not minus.
            if ((index & 1) == 0) {
                return -1;
            }
        }
        index /= 2; // Round toward 0 (odd index are maximum values).
        return index;
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     * This method searches for an exact match, i.e. this method doesn't
     * check if the given range is contained in a larger range.
     *
     * @param object The object to compare to this set.
     * @return {@code true} if the given object is contained in this set.
     */
    @Override
    public boolean contains(final Object object) {
        if (object instanceof Range<?>) {
            @SuppressWarnings("unchecked") // We are going to check just the line after.
            final Range<E> range = (Range<E>) object;
            if (range.getElementType() == elementType) {
                if (range.isMinIncluded() && !range.isMaxIncluded()) {
                    final int index = binarySearch(range.getMinValue(), length);
                    if (index >= 0 && (index & 1) == 0) {
                        final int c = get(index+1).compareTo(range.getMaxValue());
                        return c == 0;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the first (lowest) range currently in this sorted set.
     *
     * @throws NoSuchElementException if this set is empty.
     */
    @Override
    public Range<E> first() throws NoSuchElementException {
        if (length == 0) {
            throw new NoSuchElementException();
        }
        return newRange(get(0), get(1));
    }

    /**
     * Returns the last (highest) range currently in this sorted set.
     *
     * @throws NoSuchElementException if the set is empty.
     */
    @Override
    public Range<E> last() throws NoSuchElementException {
        if (length == 0) {
            throw new NoSuchElementException();
        }
        return newRange(get(length-2), get(length-1));
    }

    /**
     * Returns a view of the portion of this sorted set whose elements range
     * from {@code lower}, inclusive, to {@code upper}, exclusive.
     *
     * @param  lower Low endpoint (inclusive) of the sub set.
     * @param  upper High endpoint (exclusive) of the sub set.
     * @return A view of the specified range within this sorted set.
     */
    @Override
    public SortedSet<Range<E>> subSet(final Range<E> lower, final Range<E> upper) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Returns a view of the portion of this sorted set whose elements are
     * strictly less than {@code upper}.
     *
     * @param  upper High endpoint (exclusive) of the headSet.
     * @return A view of the specified initial range of this sorted set.
     */
    @Override
    public SortedSet<Range<E>> headSet(final Range<E> upper) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Returns a view of the portion of this sorted set whose elements are
     * greater than or equal to {@code lower}.
     *
     * @param  lower Low endpoint (inclusive) of the tailSet.
     * @return A view of the specified final range of this sorted set.
     */
    @Override
    public SortedSet<Range<E>> tailSet(final Range<E> lower) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Returns an iterator over the elements in this set of ranges.
     * All elements are {@link Range} objects.
     */
    @Override
    public Iterator<Range<E>> iterator() {
        return new Iter();
    }


    /**
     * An iterator for iterating through ranges in a {@link RangeSet}.
     * All elements are {@link Range} objects.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3 (derived from geotk-2.0)
     * @version 0.3
     * @module
     */
    private final class Iter implements Iterator<Range<E>> {
        /**
         * Modification count at construction time.
         */
        private int modCount = RangeSet.this.modCount;

        /**
         * The array length.
         */
        private final int length = RangeSet.this.length;

        /**
         * Current position in {@link RangeSet#array}.
         */
        private int position;

        /**
         * Returns {@code true} if the iteration has more elements.
         */
        @Override
        public boolean hasNext() {
            return position < length;
        }

        /**
         * Returns the next element in the iteration.
         */
        @Override
        public Range<E> next() {
            if (hasNext()) {
                final E lower = get(position++);
                final E upper = get(position++);
                if (RangeSet.this.modCount != modCount) {
                    // Check it last, in case a change occurred
                    // while we was constructing the element.
                    throw new ConcurrentModificationException();
                }
                return newRange(lower, upper);
            }
            throw new NoSuchElementException();
        }

        /**
         * Removes from the underlying collection the
         * last element returned by the iterator.
         */
        @Override
        public void remove() {
            if (position != 0) {
                if (RangeSet.this.modCount == modCount) {
                    removeAt(position-2, position);
                    modCount = RangeSet.this.modCount;
                } else {
                    throw new ConcurrentModificationException();
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Returns a new {@link Range} object initialized with the given values.
     *
     * @param  lower The lower value, inclusive.
     * @param  upper The upper value, exclusive.
     * @return The new range for the given values.
     */
    protected Range<E> newRange(final E lower, final E upper) {
        return new Range<>(elementType, lower, true, upper, false);
    }

    /**
     * A {@link RangeSet} implementation for {@link NumberRange} elements.
     *
     * @see RangeSet#create(Class)
     */
    private static final class Numeric<E extends Number & Comparable<? super E>> extends RangeSet<E> {
        private static final long serialVersionUID = 934107071458551753L;

        Numeric(final Class<E> elementType) {
            super(elementType);
        }

        @Override
        protected Range<E> newRange(final E lower, final E upper) {
            return new NumberRange<>(elementType, lower, true, upper, false);
        }
    }

    /*
     * Do not override hash code - or if we do, we shall make sure than the
     * hash code value is computed as documented in the Set interface.
     */

    /**
     * Compares the specified object with this set of ranges for equality.
     *
     * @param object The object to compare with this range.
     * @return {@code true} if the given object is equal to this range.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof RangeSet<?>) {
            final RangeSet<?> that = (RangeSet<?>) object;
            if (length != that.length || elementType != that.elementType) {
                return false;
            }
            this.trimToSize();
            that.trimToSize();
            final Object a1 = this.array;
            final Object a2 = that.array;
            switch (elementCode) {
                case DOUBLE:    return Arrays.equals((double[]) a1, (double[]) a2);
                case FLOAT:     return Arrays.equals((float []) a1, ( float[]) a2);
                case LONG:      return Arrays.equals((long  []) a1, (  long[]) a2);
                case INTEGER:   return Arrays.equals((int   []) a1, (   int[]) a2);
                case SHORT:     return Arrays.equals((short []) a1, ( short[]) a2);
                case BYTE:      return Arrays.equals((byte  []) a1, (  byte[]) a2);
                case CHARACTER: return Arrays.equals((char  []) a1, (  char[]) a2);
                default:        return Arrays.equals((Object[]) a1, (Object[]) a2);
            }
        }
        return super.equals(object); // Allow comparison with other Set implementations.
    }

    /**
     * Returns a clone of this range set.
     *
     * @return A clone of this range set.
     */
    @Override
    @SuppressWarnings("unchecked")
    public RangeSet<E> clone() {
        final RangeSet<E> set;
        try {
            set = (RangeSet<E>) super.clone();
        } catch (CloneNotSupportedException exception) {
            // Should not happen, since we are cloneable.
            throw new AssertionError(exception);
        }
        set.reallocate();
        return set;
    }

    /**
     * Invoked before serialization. Trims the internal array to the minimal size
     * in order to reduce the size of the object to be serialized.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        trimToSize();
        out.defaultWriteObject();
    }

    /**
     * Invoked after deserialization. Initializes the transient fields.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (array != null) {
            length = Array.getLength(array);
            assert isSorted();
        }
    }
}
