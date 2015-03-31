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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.AbstractSet;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Range;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.Numbers.*;


/**
 * An ordered set of disjoint ranges where overlapping ranges are merged.
 * All {@code add} and {@code remove} operations defined in this class interact with the existing
 * ranges, merging or splitting previously added ranges in order to ensure that every ranges in a
 * {@code RangeSet} are always disjoint. More specifically:
 *
 * <ul>
 *   <li>When a range is {@linkplain #add(Range) added}, {@code RangeSet} first looks for existing
 *       ranges overlapping the specified range. If overlapping ranges are found, then those ranges
 *       are merged as of {@link Range#union(Range)}. Consequently, adding ranges may in some
 *       circumstances <strong>reduce</strong> the {@linkplain #size() size} of this set.</li>
 *   <li>Conversely, when a range is {@linkplain #remove(Object) removed}, {@code RangeSet} first
 *       looks if that range is in the middle of an existing range. If such range is found, then
 *       the enclosing range is splitted as of {@link Range#subtract(Range)}. Consequently, removing
 *       ranges may in some circumstances <strong>increase</strong> the size of this set.</li>
 * </ul>
 *
 * <div class="section">Inclusive or exclusive endpoints</div>
 * {@code RangeSet} requires that {@link Range#isMinIncluded()} and {@link Range#isMaxIncluded()}
 * return the same values for all instances added to this set. Those values need to be specified
 * at construction time. If a user needs to store mixed kind of ranges, then he needs to subclass
 * this {@code RangeSet} class and override the {@link #add(Range)}, {@link #remove(Object)} and
 * {@link #newRange(Comparable, Comparable)} methods.
 *
 * <div class="note"><b>Note:</b>
 * Current implementation does not yet support open intervals. The ranges shall be either closed intervals,
 * or half-open. This limitation exists because supporting open intervals implies that the internal array
 * shall support duplicated values.</div>
 *
 * <div class="section">Extensions to <code>SortedSet</code> API</div>
 * This class contains some methods not found in standard {@link SortedSet} API.
 * Some of those methods look like {@link java.util.List} API, in that they work
 * with the index of a {@code Range} instance in the sequence of ranges returned
 * by the iterator.
 *
 * <ul>
 *   <li>{@link #indexOfRange(Comparable)} returns the index of the range containing
 *       the given value (if any).</li>
 *   <li>{@link #getMinDouble(int)} and {@link #getMaxDouble(int)} return the endpoint values
 *       in the range at the given index as a {@code double} without the cost of creating a
 *       {@link Number} instance.</li>
 *   <li>{@link #getMinLong(int)} and {@link #getMaxLong(int)} are equivalent to the above
 *       methods for the {@code long} primitive type, used mostly for {@link java.util.Date}
 *       values (see implementation note below).</li>
 *   <li>{@link #intersect(Range)} provides a more convenient way than {@code subSet(…)},
 *       {@code headSet(…)} and {@code tailSet(…)} for creating views over subsets of a
 *       {@code RangeSet}.</li>
 *   <li>{@link #trimToSize()} frees unused space.</li>
 * </ul>
 *
 * <div class="section">Implementation note</div>
 * For efficiency reasons, this set stores the range values in a Java array of primitive type if
 * possible. The {@code Range} instances given in argument to the {@link #add(Range)} method are
 * not retained by this class. Ranges are recreated during iterations by calls to the
 * {@link #newRange(Comparable, Comparable)} method. Subclasses can override that method if they
 * need to customize the range objects to be created.
 *
 * <p>While it is possible to create {@code RangeSet<Date>} instances, it is more efficient to
 * use {@code RangeSet<Long>} with millisecond values because {@code RangeSet} will internally
 * use {@code long[]} arrays in the later case.</p>
 *
 * @param <E> The type of range elements.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see Range
 */
public class RangeSet<E extends Comparable<? super E>> extends AbstractSet<Range<E>>
        implements CheckedContainer<Range<E>>, SortedSet<Range<E>>, Cloneable, Serializable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7493555225994855486L;

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
     * @since   0.3
     * @version 0.3
     * @module
     */
    private static final class Compare<E extends Comparable<? super E>>
            implements Comparator<Range<E>>, Serializable
    {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 8688450091923783564L;

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
        Object readResolve() {
            return INSTANCE;
        }
    };

    /**
     * The type of elements in the ranges. If the element are numbers,
     * then the value is the wrapper type (not the primitive type).
     *
     * @see Range#getElementType()
     */
    protected final Class<E> elementType;

    /**
     * The primitive type, as one of {@code DOUBLE}, {@code FLOAT}, {@code LONG}, {@code INTEGER},
     * {@code SHORT}, {@code BYTE}, {@code CHARACTER} enumeration. If the {@link #elementType} is
     * not the wrapper of a primitive type, then this field value is {@code OTHER}.
     */
    private final byte elementCode;

    /**
     * {@code true} if the minimal values of ranges in this set are inclusive, or {@code false}
     * if exclusive. This value is specified at construction time and enforced when ranges are
     * added or removed.
     *
     * @see Range#isMinIncluded()
     */
    protected final boolean isMinIncluded;

    /**
     * {@code true} if the maximal values of ranges in this set are inclusive, or {@code false}
     * if exclusive. This value is specified at construction time and enforced when ranges are
     * added or removed.
     *
     * @see Range#isMaxIncluded()
     */
    protected final boolean isMaxIncluded;

    /**
     * The array of ranges. It may be either an array of Java primitive type like {@code int[]} or
     * {@code float[]}, or an array of {@link Comparable} elements. All elements at even indices
     * are minimal values, and all elements at odd indices are maximal values. Elements in this
     * array must be strictly increasing without duplicated values.
     *
     * <div class="note"><b>Note:</b>
     * The restriction against duplicated values will need to be removed in a future version
     * if we want to support open intervals. All binary searches in this class will need to
     * take in account the possibility for duplicated values.</div>
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
     * Client code should use the static {@link #create(Class, boolean, boolean)} method instead.
     *
     * @param elementType   The type of the range elements.
     * @param isMinIncluded {@code true} if the minimal values are inclusive, or {@code false} if exclusive.
     * @param isMaxIncluded {@code true} if the maximal values are inclusive, or {@code false} if exclusive.
     */
    protected RangeSet(final Class<E> elementType, final boolean isMinIncluded, final boolean isMaxIncluded) {
        ArgumentChecks.ensureNonNull("elementType", elementType);
        // Following assertion may fail only if the user bypass the parameterized type checks.
        assert Comparable.class.isAssignableFrom(elementType) : elementType;
        this.elementType   = elementType;
        this.elementCode   = getEnumConstant(elementType);
        this.isMinIncluded = isMinIncluded;
        this.isMaxIncluded = isMaxIncluded;
        if (!isMinIncluded && !isMaxIncluded) {
            // We do not localize this error message because it may disaspear
            // in a future SIS version if we decide to support closed intervals.
            throw new IllegalArgumentException("Open intervals are not yet supported.");
        }
    }

    /**
     * Constructs an initially empty set of ranges.
     *
     * @param  <E>           The type of range elements.
     * @param  elementType   The type of the range elements.
     * @param  isMinIncluded {@code true} if the minimal values are inclusive, or {@code false} if exclusive.
     * @param  isMaxIncluded {@code true} if the maximal values are inclusive, or {@code false} if exclusive.
     * @return A new range set for range elements of the given type.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <E extends Comparable<? super E>> RangeSet<E> create(final Class<E> elementType,
            final boolean isMinIncluded, final boolean isMaxIncluded)
    {
        ArgumentChecks.ensureNonNull("elementType", elementType);
        if (Number.class.isAssignableFrom(elementType)) {
            return new Numeric(elementType, isMinIncluded, isMaxIncluded);
        }
        return new RangeSet<E>(elementType, isMinIncluded, isMaxIncluded);
    }

    /**
     * Returns the type of elements in this <em>collection</em>, which is always {@code Range}.
     * This is not the type of minimal and maximal values in range objects.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final Class<Range<E>> getElementType() {
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
        if (array == null) {
            return true;
        }
        final boolean strict = isMinIncluded | isMaxIncluded;
        switch (elementCode) {
            case DOUBLE:    return ArraysExt.isSorted((double[]) array, strict);
            case FLOAT:     return ArraysExt.isSorted((float[])  array, strict);
            case LONG:      return ArraysExt.isSorted((long[])   array, strict);
            case INTEGER:   return ArraysExt.isSorted((int[])    array, strict);
            case SHORT:     return ArraysExt.isSorted((short[])  array, strict);
            case BYTE:      return ArraysExt.isSorted((byte[])   array, strict);
            case CHARACTER: return ArraysExt.isSorted((char[])   array, strict);
            default:        return ArraysExt.isSorted((E[])      array, strict);
        }
    }

    /**
     * Returns the index of {@code value} in {@link #array}. This method delegates to
     * one of {@code Arrays.binarySearch} methods, depending on element primary type.
     *
     * @param value The value to search.
     * @param lower Index of the first value to examine.
     * @param upper Index after the last value to examine.
     */
    final int binarySearch(final E value, final int lower, final int upper) {
        switch (elementCode) {
            // The convolved casts below are for working around a JDK6 compiler error which does not occur with the JDK7 compiler.
            case DOUBLE:    return Arrays.binarySearch((double[]) array, lower, upper, ((Double)    ((Comparable) value)).doubleValue());
            case FLOAT:     return Arrays.binarySearch((float []) array, lower, upper, ((Float)     ((Comparable) value)).floatValue ());
            case LONG:      return Arrays.binarySearch((long  []) array, lower, upper, ((Long)      ((Comparable) value)).longValue  ());
            case INTEGER:   return Arrays.binarySearch((int   []) array, lower, upper, ((Integer)   ((Comparable) value)).intValue   ());
            case SHORT:     return Arrays.binarySearch((short []) array, lower, upper, ((Short)     ((Comparable) value)).shortValue ());
            case BYTE:      return Arrays.binarySearch((byte  []) array, lower, upper, ((Byte)      ((Comparable) value)).byteValue  ());
            case CHARACTER: return Arrays.binarySearch((char  []) array, lower, upper, ((Character) ((Comparable) value)).charValue  ());
            default:        return Arrays.binarySearch((Object[]) array, lower, upper,             value);
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
     * Adds a range to this set. If the specified range overlaps existing ranges,
     * then the existing ranges will be merged as of {@link Range#union(Range)}.
     * In other words, invoking this method may <strong>reduce</strong> the
     * {@linkplain #size() size} of this set.
     *
     * <p>The default implementation does nothing if the given range {@linkplain Range#isEmpty() is
     * empty}. Otherwise this method ensures that the {@code isMinIncluded} and {@code isMaxIncluded}
     * match the ones given to the constructor of this {@code RangeSet}, then delegates to
     * {@link #add(Comparable, Comparable)}.</p>
     *
     * @param  range The range to add.
     * @return {@code true} if this set changed as a result of this method call.
     * @throws IllegalArgumentException If the {@code isMinIncluded} or {@code isMaxIncluded}
     *         property doesn't match the one given at this {@code RangeSet} constructor.
     */
    @Override
    public boolean add(final Range<E> range) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("range", range);
        if (range.isEmpty()) {
            return false;
        }
        if (range.isMinIncluded() != isMinIncluded || range.isMaxIncluded() != isMaxIncluded) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "range", range));
        }
        return add(range.getMinValue(), range.getMaxValue());
    }

    /**
     * Adds a range of values to this set. If the specified range overlaps existing ranges, then
     * the existing ranges will be merged. This may result in smaller {@linkplain #size() size}
     * of this set.
     *
     * @param  minValue The minimal value.
     * @param  maxValue The maximal value.
     * @return {@code true} if this set changed as a result of this method call.
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
        int i0 = binarySearch(minValue, 0, length);
        int i1 = binarySearch(maxValue, (i0 >= 0) ? i0 : ~i0, length);
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
        assert getValue(i0).compareTo(maxValue) <= 0;
        assert getValue(i1).compareTo(minValue) >= 0;
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
     * <p>The {@code isMinIncluded} and {@code isMaxIncluded} properties of the given range
     * shall be the complement of the ones given to the constructor of this {@code RangeSet}:</p>
     * <table class="sis">
     *   <caption>Expected bounds inclusion</caption>
     *   <tr><th>{@code add(…)} values</th> <th>{@code remove(…)} values</th></tr>
     *   <tr><td>{@code [min … max]}</td>   <td>{@code (min … max)}</td></tr>
     *   <tr><td>{@code (min … max)}</td>   <td>{@code [min … max]}</td></tr>
     *   <tr><td>{@code [min … max)}</td>   <td>{@code (min … max]}</td></tr>
     *   <tr><td>{@code (min … max]}</td>   <td>{@code [min … max)}</td></tr>
     * </table>
     *
     * <p>The default implementation does nothing if the given object is {@code null}, or is not an
     * instance of {@code Range}, or {@linkplain Range#isEmpty() is empty}, or its element type is
     * not equals to the element type of the ranges of this set. Otherwise this method ensures that
     * the {@code isMinIncluded} and {@code isMaxIncluded} are consistent with the ones given to the
     * constructor of this {@code RangeSet}, then delegates to {@link #remove(Comparable, Comparable)}.</p>
     *
     * @param  object The range to remove.
     * @return {@code true} if this set changed as a result of this method call.
     * @throws IllegalArgumentException If the {@code isMinIncluded} or {@code isMaxIncluded}
     *         property is not the complement of the one given at this {@code RangeSet} constructor.
     */
    @Override
    public boolean remove(final Object object) {
        if (object instanceof Range<?>) {
            @SuppressWarnings("unchecked") // Type will actally be checked on the line after.
            final Range<E> range = (Range<E>) object;
            if (range.getElementType() == elementType) {
                if (range.isMinIncluded() == isMaxIncluded || range.isMaxIncluded() == isMinIncluded) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.IllegalArgumentValue_2, "object", range));
                }
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
     * @param  minValue The minimal value.
     * @param  maxValue The maximal value.
     * @return {@code true} if this set changed as a result of this method call.
     * @throws IllegalArgumentException if {@code minValue} is greater than {@code maxValue}.
     */
    public boolean remove(final E minValue, final E maxValue) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("minValue", minValue);
        ArgumentChecks.ensureNonNull("maxValue", maxValue);
        if (length == 0) return false; // Nothing to do if no data.
        ensureOrdered(minValue, maxValue);

        // Search insertion index.
        int i0 = binarySearch(minValue, 0, length);
        int i1 = binarySearch(maxValue, (i0 >= 0) ? i0 : ~i0, length);
        if (i0 < 0) i0 = ~i0;
        if (i1 < 0) i1 = ~i1;
        if ((i0 & 1) == 0) {
            if ((i1 & 1) == 0) {
                /*
                 * i0 & i1 are even.
                 * Case where min and max value are outside any existing range.
                 *
                 *   index :      A0    B0       A1       B1        An      Bn     A(n+1)   B(n+1)
                 *   range :      ███████        ██████████   ◾◾◾   ██████████     ██████████
                 *                          |-----------------------------------|
                 *   values :            minValue (i0)                      maxValue (i1)
                 *
                 * In this case delete all ranges between minValue and maxValue ([(A1, B1); (An, Bn)]).
                 */
                removeAt(i0, i1);
            } else {
                /*
                 * i0 is even and i1 is odd.
                 * Case where minValue is outside any existing range and maxValue is inside a specific range.
                 *
                 *   index :      A0    B0       A1       B1        An      Bn     A(n+1)   B(n+1)
                 *   range :      ███████        ██████████   ◾◾◾   ██████████     ██████████
                 *                          |----------------------------|
                 *   values :            minValue (i0)               maxValue (i1)
                 *
                 * In this case :
                 * - delete all ranges between minValue and maxValue ([(A1, B1); (A(n-1), B(n-1))]).
                 * - and replace range (An; Bn) by new range (MaxValue; Bn).
                 *
                 * Result :
                 * index :      A0    B0       i1  Bn     A(n+1)   B(n+1)
                 * range :      ███████        █████      ██████████  ◾◾◾
                 */
                removeAt(i0, i1 & ~1); // equivalent to (i0, i1 - 1)
                Array.set(array, i0, maxValue);
            }
        } else {
            if ((i1 & 1) == 0) {
                /*
                 * i0 is odd and i1 is even.
                 * Case where minValue is inside a specific range and maxValue is outside any range.
                 *
                 *  index :      A0    B0     A1       B1        An      Bn        A(n+1)   B(n+1)
                 *  range :      ███████      ██████████   ◾◾◾   ██████████        ██████████
                 *                                 |----------------------------|
                 *  values :            minValue (i0)               maxValue (i1)
                 *
                 * In this case :
                 *  - delete all ranges between minValue and maxValue ([(A2, B2); (An, Bn)]).
                 *  - and replace range (A1; B1) by new range (A1; i0).
                 *
                 * Result :
                 *  index :      A0    B0       A1  i0     A(n+1)   B(n+1)
                 *  range :      ███████        █████      ██████████   ◾◾◾
                 */
                removeAt(i0 + 1, i1);
                Array.set(array, i0, minValue);
            } else {
                /*
                 * i0 and i1 are odd.
                 * Case where minValue and maxValue are inside any specific range.
                 *
                 *  index :      A0    B0     A1       B1         An      Bn       A(n+1)   B(n+1)
                 *  range :      ███████      ██████████   ◾◾◾    ██████████       ██████████
                 *                                 |-------------------|
                 *  values :            minValue (i0)               maxValue (i1)
                 *
                 * In this case :
                 *  - delete all ranges between minValue and maxValue ([(A2, B2); (A(n-1), B(n-1))]).
                 *  - and replace range (A1; B1) by new range (A1; i0).
                 *
                 * Result :
                 *  index  :      A0    B0       A1  i0    i1  Bn     A(n+1)   B(n+1)
                 *  range  :      ███████        █████  ◾◾◾    █████      ██████████
                 *
                 * A particularity case exist if i0 equal i1, which means minValue
                 * and maxValue are inside the same specific range.
                 *
                 *  index  :      A0    B0     A1                  B1         An      Bn
                 *  range  :      ███████      █████████████████████   ◾◾◾    ██████████
                 *                                |-------------|
                 *  values :            minValue (i0)      maxValue (i1)
                 * In this case total range number will be increase by one.
                 *
                 * Result  :
                 *  index  :      A0    B0       A1  i0    i1  B1     An   Bn
                 *  range  :      ███████        █████     █████   ◾◾◾   █████
                 */
                if (i0 == i1) {
                    // Above-cited special case
                    insertAt(i1 + 1, maxValue, getValue(i1));
                    Array.set(array, i0, minValue);
                } else {
                    final int di = i1 - i0;
                    assert di >= 2 : di;
                    if (di > 2) {
                        removeAt(i0 + 1, i1 & ~1); // equivalent to (i0 + 1, i1 - 1)
                    }
                    Array.set(array, i0,     minValue);
                    Array.set(array, i0 + 1, maxValue);
                }
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the given object is an instance of {@link Range} compatible
     * with this set and contained inside one of the range elements of this set.
     * If this method returns {@code true}, then:
     *
     * <ul>
     *   <li>Invoking {@link #add(Range)} is guaranteed to have no effect.</li>
     *   <li>Invoking {@link #remove(Object)} is guaranteed to modify this set.</li>
     * </ul>
     *
     * Conversely, if this method returns {@code false}, then:
     *
     * <ul>
     *   <li>Invoking {@link #add(Range)} is guaranteed to modify this set.</li>
     *   <li>Invoking {@link #remove(Object)} may or may not modify this set.
     *       The consequence of invoking {@code remove(…)} is undetermined because it
     *       depends on whether the given range is outside every ranges in this set,
     *       or if it overlaps with at least one range.</li>
     * </ul>
     *
     * The default implementation checks the type of the given object, then delegates to
     * <code>{@linkplain #contains(Range, boolean) contains}(object, false)</code>.
     *
     * @param  object The object to check for inclusion in this set.
     * @return {@code true} if the given object is contained in this set.
     */
    @Override
    public boolean contains(final Object object) {
        if (object instanceof Range<?>) {
            @SuppressWarnings("unchecked") // We are going to check just the line after.
            final Range<E> range = (Range<E>) object;
            if (range.getElementType() == elementType) {
                return contains(range, false);
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     *
     * <ul>
     *   <li>If the {@code exact} argument is {@code true}, then this method searches
     *       for an exact match (i.e. this method doesn't check if the given range is
     *       contained in a larger range).</li>
     *   <li>If the {@code exact} argument is {@code false}, then this method
     *       behaves as documented in the {@link #contains(Object)} method.</li>
     * </ul>
     *
     * @param  range The range to check for inclusion in this set.
     * @param  exact {@code true} for searching for an exact match,
     *         or {@code false} for searching for inclusion in any range.
     * @return {@code true} if the given object is contained in this set.
     */
    public boolean contains(final Range<E> range, final boolean exact) {
        ArgumentChecks.ensureNonNull("range", range);
        if (exact) {
            if (range.isMinIncluded() && !range.isMaxIncluded()) {
                final int index = binarySearch(range.getMinValue(), 0, length);
                if (index >= 0 && (index & 1) == 0) {
                    return getValue(index+1).compareTo(range.getMaxValue()) == 0;
                }
            }
        } else if (!range.isEmpty()) {
            int lower = binarySearch(range.getMinValue(), 0, length);
            if (lower < 0) {
                lower = ~lower;
                if ((lower & 1) == 0) {
                    // The lower endpoint of the given range falls between
                    // two ranges of this set.
                    return false;
                }
            } else if ((lower & 1) == 0) {
                // Lower endpoint of the given range matches exactly
                // the lower endpoint of a range in this set.
                if (!isMinIncluded && range.isMinIncluded()) {
                    return false;
                }
            }
            /*
             * At this point, the lower endpoint has been determined to be included
             * in a range of this set. Now check the upper endpoint.
             */
            int upper = binarySearch(range.getMaxValue(), lower, length);
            if (upper < 0) {
                upper = ~upper;
                if ((upper & 1) == 0) {
                    // The upper endpoint of the given range falls between
                    // two ranges of this set, or is after all ranges.
                    return false;
                }
            } else if ((upper & 1) != 0) {
                // Upper endpoint of the given range matches exactly
                // the upper endpoint of a range in this set.
                if (!isMaxIncluded && range.isMaxIncluded()) {
                    return false;
                }
            }
            return (upper - lower) <= 1;
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
        return getRange(0);
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
        return getRange(length - 2);
    }

    /**
     * Returns a view of the portion of this range set which is the intersection of
     * this {@code RangeSet} with the given range. Changes in this {@code RangeSet}
     * will be reflected in the returned view, and conversely.
     *
     * @param  subRange The range to intersect with this {@code RangeSet}.
     * @return A view of the specified range within this range set.
     */
    public SortedSet<Range<E>> intersect(final Range<E> subRange) {
        ArgumentChecks.ensureNonNull("subRange", subRange);
        return new SubSet(subRange);
    }

    /**
     * Returns a view of the portion of this sorted set whose elements range
     * from {@code lower}, inclusive, to {@code upper}, exclusive.
     * The default implementation is equivalent to the following pseudo-code
     * (omitting argument checks):
     *
     * {@preformat java
     *   return intersect(new Range<E>(elementType,
     *           lower.minValue,  lower.isMinIncluded,
     *           upper.minValue, !upper.isMinIncluded));
     * }
     *
     * <div class="note"><b>API note:</b>
     * This method takes the minimal value of the {@code upper} argument instead
     * than the maximal value because the upper endpoint is exclusive.</div>
     *
     * @param  lower Low endpoint (inclusive) of the sub set.
     * @param  upper High endpoint (exclusive) of the sub set.
     * @return A view of the specified range within this sorted set.
     *
     * @see #intersect(Range)
     */
    @Override
    public SortedSet<Range<E>> subSet(final Range<E> lower, final Range<E> upper) {
        ArgumentChecks.ensureNonNull("lower", lower);
        ArgumentChecks.ensureNonNull("upper", upper);
        final E maxValue = upper.getMinValue();
        if (maxValue == null) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "upper", upper));
        }
        return intersect(new Range<E>(elementType,
                lower.getMinValue(),  lower.isMinIncluded(),
                maxValue, !upper.isMinIncluded()));
    }

    /**
     * Returns a view of the portion of this sorted set whose elements are
     * strictly less than {@code upper}.
     * The default implementation is equivalent to the same pseudo-code than the one
     * documented in the {@link #subSet(Range, Range)} method, except that the lower
     * endpoint is {@code null}.
     *
     * @param  upper High endpoint (exclusive) of the headSet.
     * @return A view of the specified initial range of this sorted set.
     *
     * @see #intersect(Range)
     */
    @Override
    public SortedSet<Range<E>> headSet(final Range<E> upper) {
        ArgumentChecks.ensureNonNull("upper", upper);
        final E maxValue = upper.getMinValue();
        if (maxValue == null) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "upper", upper));
        }
        return intersect(new Range<E>(elementType, null, false, maxValue, !upper.isMinIncluded()));
    }

    /**
     * Returns a view of the portion of this sorted set whose elements are
     * greater than or equal to {@code lower}.
     * The default implementation is equivalent to the same pseudo-code than the one
     * documented in the {@link #subSet(Range, Range)} method, except that the upper
     * endpoint is {@code null}.
     *
     * @param  lower Low endpoint (inclusive) of the tailSet.
     * @return A view of the specified final range of this sorted set.
     *
     * @see #intersect(Range)
     */
    @Override
    public SortedSet<Range<E>> tailSet(final Range<E> lower) {
        ArgumentChecks.ensureNonNull("lower", lower);
        return intersect(new Range<E>(elementType, lower.getMinValue(), lower.isMinIncluded(), null, false));
    }

    /**
     * A view over a subset of {@link RangeSet}.
     * Instances of this class are created by the {@link RangeSet#intersect(Range)} method.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     *
     * @see RangeSet#intersect(Range)
     */
    private final class SubSet extends AbstractSet<Range<E>> implements SortedSet<Range<E>>, Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 3093791428299754372L;

        /**
         * The minimal and maximal values of this subset,
         */
        private Range<E> subRange;

        /**
         * Index of {@link #minValue} and {@link #maxValue} in the array of the enclosing
         * {@code RangeSet}. Those indices need to be recomputed every time the enclosing
         * {@code RangeSet} has been modified.
         *
         * @see #updateBounds()
         */
        private transient int lower, upper;

        /**
         * Modification count of the enclosing {@link RangeSet}, used for detecting changes.
         */
        private transient int modCount;

        /**
         * Creates a new subset of the enclosing {@code RangeSet} between the given values.
         *
         * @param subRange The range to intersect with the enclosing {@code RangeSet}.
         */
        SubSet(final Range<E> subRange) {
            this.subRange = subRange;
            if (subRange.isEmpty()) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.IllegalArgumentValue_2, "subRange", subRange));
            }
            modCount = RangeSet.this.modCount - 1;
        }

        /**
         * Recomputes the {@link #lower} and {@link #upper} indices if they are outdated.
         * If the indices are already up to date, then this method does nothing.
         */
        private void updateBounds() {
            if (modCount != RangeSet.this.modCount) {
                int lower = 0;
                int upper = length;
                final E minValue = subRange.getMinValue();
                final E maxValue = subRange.getMaxValue();
                if (minValue != null) {
                    lower = binarySearch(minValue, 0, upper);
                    if (lower < 0) {
                        lower = ~lower;
                    }
                    lower &= ~1; // Force the index to even value.
                }
                if (maxValue != null) {
                    upper = binarySearch(maxValue, lower, upper);
                    if (upper < 0) {
                        upper = ~upper;
                    }
                    /*
                     * If 'upper' is even (i.e. is the index of a minimal value), keep that index
                     * unchanged because this value is exclusive.  But if 'upper' is odd (i.e. is
                     * the index of a maximal value), move to the minimal value of the next range.
                     */
                    upper = (upper + 1) & ~1;
                }
                this.lower = lower;
                this.upper = upper;
                modCount = RangeSet.this.modCount;
            }
        }

        /**
         * Returns the comparator, which is the same than the enclosing class.
         */
        @Override
        public Comparator<Range<E>> comparator() {
            return RangeSet.this.comparator();
        }

        /**
         * Clears this subset by removing all elements in the range given to the constructor.
         */
        @Override
        public void clear() {
            RangeSet.this.remove(subRange);
        }

        /**
         * Returns the number of ranges in this subset.
         */
        @Override
        public int size() {
            updateBounds();
            return (upper - lower) >> 1;
        }

        /**
         * Adds a new range in the enclosing {@code RangeSet}, and updates this subset
         * in order to contain that range.
         */
        @Override
        public boolean add(final Range<E> range) {
            final boolean changed = RangeSet.this.add(range);
            /*
             * Update the minimal and maximal values if this sub-set has been expanded as
             * a result of this method call. Note that we don't need to remember that the
             * indices need to be recomputed, because RangeSet.this.modCount has already
             * been increased by the enclosing class.
             */
            subRange = subRange.union(range);
            return changed;
        }

        /**
         * Removes the given range or part of it from the enclosing {@code RangeSet}.
         * Before to perform the removal, this method intersects the given range with
         * the range of this subset.
         */
        @Override
        public boolean remove(Object object) {
            if (object instanceof Range<?>) {
                @SuppressWarnings("unchecked") // Type will actally be checked on the line after.
                final Range<E> range = (Range<E>) object;
                if (range.getElementType() == elementType) {
                    object = subRange.intersect(range);
                }
            }
            return RangeSet.this.remove(object);
        }

        /**
         * Tests if this subset contains the given range. Before to delegates to the enclosing
         * class, this method filter-out the ranges that are not contained in the range given
         * to the constructor of this subset.
         */
        @Override
        public boolean contains(final Object object) {
            if (object instanceof Range<?>) {
                @SuppressWarnings("unchecked") // Type will actally be checked on the line after.
                final Range<E> range = (Range<E>) object;
                if (range.getElementType() == elementType) {
                    if (!subRange.contains(range)) {
                        return false;
                    }
                }
            }
            return RangeSet.this.contains(object);
        }

        /**
         * Returns the first range in this subset,
         * intersected with the range given to the constructor.
         */
        @Override
        public Range<E> first() {
            updateBounds();
            if (lower == upper) {
                throw new NoSuchElementException();
            }
            return subRange.intersect(getRange(lower));
        }

        /**
         * Returns the last range in this subset,
         * intersected with the range given to the constructor.
         */
        @Override
        public Range<E> last() {
            updateBounds();
            if (lower == upper) {
                throw new NoSuchElementException();
            }
            return subRange.intersect(getRange(upper - 2));
        }

        /**
         * Delegates subset creation to the enclosing class.
         * The new subset will not be bigger than this subset.
         */
        @Override
        public SortedSet<Range<E>> subSet(Range<E> fromElement, Range<E> toElement) {
            fromElement = subRange.intersect(fromElement);
            toElement   = subRange.intersect(toElement);
            return RangeSet.this.subSet(fromElement, toElement);
        }

        /**
         * Delegates subset creation to the enclosing class.
         * The new subset will not be bigger than this subset.
         */
        @Override
        public SortedSet<Range<E>> headSet(Range<E> toElement) {
            toElement = subRange.intersect(toElement);
            return RangeSet.this.headSet(toElement);
        }

        /**
         * Delegates subset creation to the enclosing class.
         * The new subset will not be bigger than this subset.
         */
        @Override
        public SortedSet<Range<E>> tailSet(Range<E> fromElement) {
            fromElement = subRange.intersect(fromElement);
            return RangeSet.this.tailSet(fromElement);
        }

        /**
         * Returns an iterator over the elements in this subset.
         */
        @Override
        public Iterator<Range<E>> iterator() {
            updateBounds();
            return new SubIter(subRange, lower, upper);
        }
    }

    /**
     * The iterator returned by {@link SubSet#iterator()}. This iterator is similar
     * to the one returned by {@link RangeSet#iterator()}, except that:
     *
     * <ul>
     *   <li>The iteration is restricted to a sub-region of the {@link RangeSet#array}.</li>
     *   <li>The first and last ranges returned by the iterator are intercepted with
     *       the range of the subset (other ranges should not need to be intercepted).</li>
     *   <li>The range removed by {@link #remove()} is intercepted with the range of the
     *       subset.</li>
     * </ul>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    private final class SubIter extends Iter {
        /**
         * A copy of the {@link SubSet#subRange} field value at the time of iterator creation.
         */
        private final Range<E> subRange;

        /**
         * Index of the first element in the {@link RangeSet#array} where to iterate.
         */
        private final int lower;

        /**
         * Creates a new iterator for the given portion of the {@link RangeSet#array}.
         */
        SubIter(final Range<E> subRange, final int lower, final int upper) {
            super(upper);
            this.subRange = subRange;
            this.lower = lower;
            position = lower;
        }

        /**
         * Returns {@code true} if the iterator position is at the first or at the last range.
         * This method is accurate only when invoked after {@code super.next()}.
         */
        private boolean isFirstOrLast() {
            return position <= lower+2 || position >= upper;
        }

        /**
         * Returns the next element in the iteration.
         */
        @Override
        public Range<E> next() {
            Range<E> range = super.next();
            if (isFirstOrLast()) {
                range = subRange.intersect(range);
            }
            return range;
        }

        /**
         * Removes from the underlying collection the last element returned by the iterator.
         */
        @Override
        public void remove() {
            if (isFirstOrLast()) {
                // Default implementation is faster.
                super.remove();
                return;
            }
            if (!canRemove) {
                throw new IllegalStateException();
            }
            if (RangeSet.this.modCount != this.modCount) {
                throw new ConcurrentModificationException();
            }
            RangeSet.this.remove(subRange.intersect(getRange(position - 2)));
            canRemove = false;
        }
    }

    /**
     * Returns an iterator over the elements in this set of ranges.
     * All elements are {@link Range} objects.
     */
    @Override
    public Iterator<Range<E>> iterator() {
        return new Iter(length);
    }

    /**
     * The iterator returned by {@link RangeSet#iterator()}.
     * All elements are {@link Range} objects.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    private class Iter implements Iterator<Range<E>> {
        /**
         * Modification count at construction time.
         */
        int modCount;

        /**
         * Index after the last element in the {@link RangeSet#array} where to iterate.
         */
        final int upper;

        /**
         * Current position in {@link RangeSet#array}.
         */
        int position;

        /**
         * {@code true} if the {@link #remove()} method can be invoked.
         */
        boolean canRemove;

        /**
         * Creates a new iterator for the given portion of the {@link RangeSet#array}.
         */
        Iter(final int upper) {
            this.upper = upper;
            this.modCount = RangeSet.this.modCount;
        }

        /**
         * Returns {@code true} if the iteration has more elements.
         */
        @Override
        public final boolean hasNext() {
            return position < upper;
        }

        /**
         * Returns the next element in the iteration.
         */
        @Override
        public Range<E> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final Range<E> range = getRange(position);
            if (RangeSet.this.modCount != this.modCount) {
                // Check it last, in case a change occurred
                // while we were creating the range.
                throw new ConcurrentModificationException();
            }
            position += 2;
            canRemove = true;
            return range;
        }

        /**
         * Removes from the underlying collection the last element returned by the iterator.
         */
        @Override
        public void remove() {
            if (!canRemove) {
                throw new IllegalStateException();
            }
            if (RangeSet.this.modCount != this.modCount) {
                throw new ConcurrentModificationException();
            }
            removeAt(position-2, position);
            this.modCount = RangeSet.this.modCount;
            canRemove = false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    ////    List-like API - not usual Set API, but provided for efficiency.    ////
    ///////////////////////////////////////////////////////////////////////////////

    /**
     * If the specified value is inside a range, returns the index of this range.
     * Otherwise, returns {@code -1}.
     *
     * @param  value The value to search.
     * @return The index of the range which contains this value, or -1 if there is no such range.
     */
    public int indexOfRange(final E value) {
        int index = binarySearch(value, 0, length);
        if (index < 0) {
            // Found an insertion point. Make sure that the insertion
            // point is inside a range (i.e. before the maximum value).
            index = ~index; // Tild sign, not minus.
            if ((index & 1) == 0) {
                return -1;
            }
        } else if (!((index & 1) == 0 ? isMinIncluded : isMaxIncluded)) {
            // The value is equals to an excluded endpoint.
            return -1;
        }
        index /= 2; // Round toward 0 (odd index are maximum values).
        return index;
    }

    /**
     * Returns a {@linkplain Range#getMinValue() range minimum value} as a {@code long}.
     * The {@code index} can be any value from 0 inclusive to the set {@link #size() size}
     * exclusive. The returned values always increase with {@code index}.
     * Widening conversions are performed as needed.
     *
     * @param  index The range index, from 0 inclusive to {@link #size() size} exclusive.
     * @return The minimum value for the range at the specified index, inclusive.
     * @throws IndexOutOfBoundsException if {@code index} is out of bounds.
     * @throws ClassCastException if range elements are not convertible to {@code long}.
     */
    public long getMinLong(int index) throws IndexOutOfBoundsException, ClassCastException {
        if ((index *= 2) >= length) {
            throw new IndexOutOfBoundsException();
        }
        return Array.getLong(array, index);
    }

    /**
     * Returns a {@linkplain Range#getMinValue() range minimum value} as a {@code double}.
     * The {@code index} can be any value from 0 inclusive to the set {@link #size() size}
     * exclusive. The returned values always increase with {@code index}.
     * Widening conversions are performed as needed.
     *
     * @param  index The range index, from 0 inclusive to {@link #size() size} exclusive.
     * @return The minimum value for the range at the specified index, inclusive.
     * @throws IndexOutOfBoundsException if {@code index} is out of bounds.
     * @throws ClassCastException if range elements are not convertible to numbers.
     *
     * @see org.apache.sis.measure.NumberRange#getMinDouble()
     */
    public double getMinDouble(int index) throws IndexOutOfBoundsException, ClassCastException {
        if ((index *= 2) >= length) {
            throw new IndexOutOfBoundsException();
        }
        return Array.getDouble(array, index);
    }

    /**
     * Returns a {@linkplain Range#getMaxValue() range maximum value} as a {@code long}.
     * The {@code index} can be any value from 0 inclusive to the set {@link #size() size}
     * exclusive. The returned values always increase with {@code index}.
     * Widening conversions are performed as needed.
     *
     * @param  index The range index, from 0 inclusive to {@link #size() size} exclusive.
     * @return The maximum value for the range at the specified index, inclusive.
     * @throws IndexOutOfBoundsException if {@code index} is out of bounds.
     * @throws ClassCastException if range elements are not convertible to {@code long}.
     */
    public long getMaxLong(int index) throws IndexOutOfBoundsException, ClassCastException {
        if ((index *= 2) >= length) {
            throw new IndexOutOfBoundsException();
        }
        return Array.getLong(array, index + 1);
    }

    /**
     * Returns a {@linkplain Range#getMaxValue() range maximum value} as a {@code double}.
     * The {@code index} can be any value from 0 inclusive to the set's {@link #size size}
     * exclusive. The returned values always increase with {@code index}.
     * Widening conversions are performed as needed.
     *
     * @param  index The range index, from 0 inclusive to {@link #size size} exclusive.
     * @return The maximum value for the range at the specified index, exclusive.
     * @throws IndexOutOfBoundsException if {@code index} is out of bounds.
     * @throws ClassCastException if range elements are not convertible to numbers.
     *
     * @see org.apache.sis.measure.NumberRange#getMaxDouble()
     */
    public double getMaxDouble(int index) throws IndexOutOfBoundsException, ClassCastException {
        if ((index *= 2) >= length) {
            throw new IndexOutOfBoundsException();
        }
        return Array.getDouble(array, index + 1);
    }

    /**
     * Returns the value at the specified index. Even index are lower endpoints, while odd index
     * are upper endpoints. The index validity must have been checked before this method is invoked.
     */
    final E getValue(final int index) {
        assert (index >= 0) && (index < length) : index;
        return elementType.cast(Array.get(array, index));
    }

    /**
     * Returns the range at the given array index. The given index is relative to
     * the interval {@link #array}, which is twice the index of range elements.
     *
     * @param index The range index, from 0 inclusive to {@link #length} exclusive.
     */
    final Range<E> getRange(final int index) {
        return newRange(getValue(index), getValue(index+1));
    }

    /**
     * Returns a new {@link Range} object initialized with the given values.
     *
     * @param  lower The lower value, inclusive.
     * @param  upper The upper value, exclusive.
     * @return The new range for the given values.
     */
    protected Range<E> newRange(final E lower, final E upper) {
        return new Range<E>(elementType, lower, isMinIncluded, upper, isMaxIncluded);
    }

    /**
     * A {@link RangeSet} implementation for {@link NumberRange} elements.
     *
     * @see RangeSet#create(Class, boolean, boolean)
     */
    private static final class Numeric<E extends Number & Comparable<? super E>> extends RangeSet<E> {
        private static final long serialVersionUID = 5603640102714482527L;

        Numeric(final Class<E> elementType, final boolean isMinIncluded, final boolean isMaxIncluded) {
            super(elementType, isMinIncluded, isMaxIncluded);
        }

        @Override
        protected Range<E> newRange(final E lower, final E upper) {
            return new NumberRange<E>(elementType, lower, isMinIncluded, upper, isMaxIncluded);
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
            /*
             * Following code should produce a result identical to super.equals(Object)
             * without the cost of creating potentially large number of Range elements.
             */
            final RangeSet<?> that = (RangeSet<?>) object;
            if (length        != that.length        ||
                elementType   != that.elementType   ||
                isMinIncluded != that.isMinIncluded ||
                isMaxIncluded != that.isMaxIncluded)
            {
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
     *
     * @param  out The output stream where to serialize this range set.
     * @throws IOException If an I/O error occurred while writing.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        trimToSize();
        out.defaultWriteObject();
    }

    /**
     * Invoked after deserialization. Initializes the transient fields.
     *
     * @param  in The input stream from which to deserialize a range set.
     * @throws IOException If an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException If the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (array != null) {
            length = Array.getLength(array);
            assert isSorted();
        }
    }
}
