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
package org.apache.sis.measure;

import java.io.Serializable;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A set of minimum and maximum values of a certain class, allowing
 * a user to determine if a value of the same class is contained inside the range.
 * The minimum and maximum values do not have to be included in the range, and
 * can be null.  If the minimum or maximum values are null, the range is said to
 * be unbounded on that extreme. If both the minimum and maximum are null,
 * the range is completely unbounded and all values of that class are contained
 * within the range. Null values are always considered <em>exclusive</em>,
 * since iterations over the values will never reach the infinite bound.
 *
 * {@section Type of range elements}
 * To be a member of a {@code Range}, the {@code <T>} type defining the range must implement the
 * {@link Comparable} interface. Some methods like {@link #contains(Comparable)}, which would
 * normally expect an argument of type {@code T}, accept the base type {@code Comparable<?>} in
 * order to allow widening conversions by the {@link NumberRange} subclass. Passing an argument of
 * non-convertible type to any method will cause an {@link IllegalArgumentException} to be thrown.
 *
 * {@note This class should never throw <code>ClassCastException</code>, unless there is a bug
 *        in the <code>Range</code> class or subclasses implementation.}
 *
 * @param <T> The type of range elements, typically a {@link Number} subclass or {@link java.util.Date}.
 *
 * @author  Joe White
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see RangeFormat
 */
@Immutable
public class Range<T extends Comparable<? super T>> implements CheckedContainer<T>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5393896130562660517L;

    /**
     * The base type of elements in this range.
     *
     * @see #getElementType()
     */
    private final Class<T> elementType;

    /**
     * The minimal and maximal values.
     */
    final T minValue, maxValue;

    /**
     * Whether the minimal or maximum value is included.
     */
    private final boolean isMinIncluded, isMaxIncluded;

    /**
     * Creates a new range bounded by the given inclusive values.
     *
     * @param elementType  The class of the range elements.
     * @param minValue     The minimal value (inclusive), or {@code null} if none.
     * @param maxValue     The maximal value (inclusive), or {@code null} if none.
     */
    public Range(final Class<T> elementType, final T minValue, final T maxValue) {
        this(elementType, minValue, true, maxValue, true);
    }

    /**
     * Creates a new range bounded by the given values.
     *
     * @param elementType    The base type of the range elements.
     * @param minValue       The minimal value, or {@code null} if none.
     * @param isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param maxValue       The maximal value, or {@code null} if none.
     * @param isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     */
    public Range(final Class<T> elementType,
            final T minValue, final boolean isMinIncluded,
            final T maxValue, final boolean isMaxIncluded)
    {
        ensureNonNull("elementType", elementType);
        /*
         * The 'isMin/Maxincluded' flags must be forced to 'false' if 'minValue' or 'maxValue'
         * are null. This is required for proper working of algorithms implemented in this class.
         */
        this.elementType   = elementType;
        this.minValue      = minValue;
        this.isMinIncluded = isMinIncluded && (minValue != null);
        this.maxValue      = maxValue;
        this.isMaxIncluded = isMaxIncluded && (maxValue != null);
        ensureValidType();
        if (minValue != null) ensureCompatibleType(minValue.getClass());
        if (maxValue != null) ensureCompatibleType(maxValue.getClass());
    }

    /**
     * Creates a new range using the same element class than this range. This method will
     * be overridden by subclasses in order to create a range of a more specific type.
     */
    Range<T> create(final T minValue, final boolean isMinIncluded,
                    final T maxValue, final boolean isMaxIncluded)
    {
        return new Range<>(elementType, minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Returns an initially empty array of the given length. To be overridden
     * by subclasses in order to create arrays of more specific type.
     */
    @SuppressWarnings({"unchecked","rawtypes"}) // Generic array creation.
    Range<T>[] newArray(final int length) {
        return new Range[length];
    }

    /**
     * Ensures that the given range uses the same element class than this range,
     * then return the casted argument value.
     *
     * @param range The range to test for compatibility.
     */
    @SuppressWarnings("unchecked")
    private Range<? extends T> ensureCompatible(final Range<?> range) throws IllegalArgumentException {
        ensureNonNull("range", range);
        ensureCompatibleType(range.elementType);
        return (Range<? extends T>) range;
    }

    /**
     * Ensures that the given type is compatible with the type expected by this range.
     */
    private void ensureCompatibleType(final Class<?> type) throws IllegalArgumentException {
        if (!elementType.isAssignableFrom(type)) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalClass_2, elementType, type));
        }
    }

    /**
     * Ensures that {@link #elementType} is compatible with the type expected by this range class.
     * This method is invoked at construction time for validating the type argument. This method
     * is overridden by {@link NumberRange} and {@link DateRange} for more specific check.
     */
    void ensureValidType() throws IllegalArgumentException {
        if (!Comparable.class.isAssignableFrom(elementType)) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalClass_2, Comparable.class, elementType));
        }
    }

    /**
     * Returns the base type of elements in this range.
     * This is the type specified at construction time.
     */
    @Override
    public Class<T> getElementType() {
        return elementType;
    }

    /**
     * Returns the minimal value, or {@code null} if this range has no lower limit.
     * If non-null, the returned value is either inclusive or exclusive depending on
     * the boolean returned by {@link #isMinIncluded()}.
     *
     * @return The minimal value, or {@code null} if this range is unbounded on the lower side.
     */
    public T getMinValue() {
        return minValue;
    }

    /**
     * Returns {@code true} if the {@linkplain #getMinValue() minimal value} is inclusive,
     * or {@code false} is exclusive. Note that {@code null} values are always considered
     * exclusive.
     *
     * @return {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     */
    public boolean isMinIncluded() {
        return isMinIncluded;
    }

    /**
     * Returns the maximal value, or {@code null} if this range has no upper limit.
     * If non-null, the returned value is either inclusive or exclusive depending on
     * the boolean returned by {@link #isMaxIncluded()}.
     *
     * @return The maximal value, or {@code null} if this range is unbounded on the upper side.
     */
    public T getMaxValue() {
        return maxValue;
    }

    /**
     * Returns {@code true} if the {@linkplain #getMaxValue() maximal value} is inclusive,
     * or {@code false} is exclusive. Note that {@code null} values are always considered
     * exclusive.
     *
     * @return {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     */
    public boolean isMaxIncluded() {
        return isMaxIncluded;
    }

    /**
     * Returns {@code true} if this range is empty. A range is empty if the
     * {@linkplain #getMinValue() minimum value} is smaller than the
     * {@linkplain #getMaxValue() maximum value}, or if they are equal while
     * at least one of them is exclusive.
     *
     * @return {@code true} if this range is empty.
     */
    public boolean isEmpty() {
        if (minValue == null || maxValue == null) {
            return false; // Unbounded: can't be empty.
        }
        final int c = minValue.compareTo(maxValue);
        if (c < 0) {
            return false; // Minimum is smaller than maximum.
        }
        // If min and max are equal, then the range is empty if at least one of them is exclusive.
        return (c != 0) || !isMinIncluded || !isMaxIncluded;
    }

    /**
     * Returns {@code true} if this range contains the given value. A range never contains the
     * {@code null} value. This is consistent with the <a href="#skip-navbar_top">class javadoc</a>
     * stating that null {@linkplain #getMinValue() minimum} or {@linkplain #getMaxValue() maximum}
     * values are exclusive.
     *
     * @param  value The value to check for inclusion in this range.
     * @return {@code true} if the given value is included in this range.
     * @throws IllegalArgumentException is the given value can not be converted to a valid type
     *         through widening conversion.
     */
    @SuppressWarnings("unchecked")
    public boolean contains(final Comparable<?> value) throws IllegalArgumentException {
        if (value == null) {
            return false;
        }
        ensureCompatibleType(value.getClass());
        return containsNC((T) value);
    }

    /**
     * Implementation of {@link #contains(Comparable)} to be invoked directly by subclasses.
     * "NC" stands for "No Conversion" - this method does not try to convert the value to a
     * compatible type.
     *
     * @param value The value to test for inclusion. Can not be null.
     */
    final boolean containsNC(final T value) {
        /*
         * Implementation note: when testing for inclusion or intersection in a range
         * (or in a rectangle, cube, etc.), it is often easier to test when we do not
         * have inclusion than to test for inclusion. So we test when to return false
         * and if no such test pass, we can return true.
         *
         * We consistently use min/maxValue.compareTo(value) in this class rather than
         * the opposite argument order (namely value.compareTo(min/maxValue)) in the
         * hope to reduce the risk of inconsistent behavior if usera pass different
         * sub-classes for the 'value' argument with different implementations of the
         * 'compareTo' method. Intead than using those user implementations, we always
         * use the implementations provided by min/maxValue.
         */
        if (minValue != null) {
            final int c = minValue.compareTo(value);
            if (isMinIncluded ? (c > 0) : (c >= 0)) {
                return false;
            }
        }
        if (maxValue != null) {
            final int c = maxValue.compareTo(value);
            if (isMaxIncluded ? (c < 0) : (c <= 0)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the supplied range is fully contained within this range.
     *
     * @param  range The range to check for inclusion in this range.
     * @return {@code true} if the given range is included in this range.
     * @throws IllegalArgumentException is the bounds of the given range can not be converted to
     *         a valid type through widening conversion, or if the units of measurement are not
     *         convertible.
     */
    public boolean contains(final Range<?> range) throws IllegalArgumentException {
        return containsNC(ensureCompatible(range));
    }

    /**
     * Implementation of {@link #contains(Range)} to be invoked directly by subclasses.
     * "NC" stands for "No Conversion" - this method does not try to convert the bounds
     * to a compatible type.
     */
    final boolean containsNC(final Range<? extends T> range) {
        /*
         * We could implement this method as below:
         *
         *     return contains(range.minValue) && contains(range.maxValue);
         *
         * However the above code performs more comparisons than necessary,
         * since it implicitly performs the following redundant checks:
         *
         *     (range.minValue < maxValue) redundant with (range.maxValue < maxValue)
         *     (range.maxValue > minValue) redundant with (range.minValue > minValue)
         *
         * We can implement this method with less comparisons as below:
         *
         *     return minValue.compareTo(range.minValue) <= 0 &&
         *            maxValue.compareTo(range.maxValue) >= 0;
         *
         * However we still have a little bit of additional checks to perform for the
         * inclusion status of both ranges.  Since the same checks will be needed for
         * intersection methods,  we factor out the comparisons in 'compareMinTo' and
         * 'compareMaxTo' methods.
         */
        return (compareMinTo(range.minValue, range.isMinIncluded ? 0 : -1) <= 0) &&
               (compareMaxTo(range.maxValue, range.isMaxIncluded ? 0 : +1) >= 0);
    }

    /**
     * Returns {@code true} if this range intersects the given range.
     *
     * @param  range The range to check for intersection with this range.
     * @return {@code true} if the given range intersects this range.
     * @throws IllegalArgumentException is the given range can not be converted to a valid type
     *         through widening conversion, or if the units of measurement are not convertible.
     */
    public boolean intersects(final Range<?> range) throws IllegalArgumentException {
        return intersectsNC(ensureCompatible(range));
    }

    /**
     * Implementation of {@link #intersects(Range)} to be invoked directly by subclasses.
     * "NC" stands for "No Conversion" - this method does not try to convert the bounds
     * to a compatible type.
     */
    final boolean intersectsNC(final Range<? extends T> range) {
        return (compareMinTo(range.maxValue, range.isMaxIncluded ? 0 : +1) <= 0) &&
               (compareMaxTo(range.minValue, range.isMinIncluded ? 0 : -1) >= 0);
    }

    /**
     * Returns the intersection between this range and the given range.
     *
     * @param  range The range to intersect.
     * @return The intersection of this range with the given range.
     * @throws IllegalArgumentException is the given range can not be converted to a valid type
     *         through widening conversion, or if the units of measurement are not convertible.
     */
    public Range<?> intersect(final Range<?> range) throws IllegalArgumentException {
        return intersectNC(ensureCompatible(range));
    }

    /**
     * Implementation of {@link #intersect(Range)} to be invoked directly by subclasses.
     * "NC" stands for "No Conversion" - this method does not try to convert the bounds
     * to a compatible type.
     */
    final Range<? extends T> intersectNC(final Range<? extends T> range)
            throws IllegalArgumentException
    {
        /*
         * For two ranges [L₁ … H₁] and [L₂ … H₂], the intersection is given by
         * ([max(L₁, L₂) … min(H₁, H₂)]). Only two comparisons is needed.
         *
         * There is a small complication since we shall also handle the inclusive states.
         * so instead than extracting the minimal and maximal values directly, we will
         * find which range contains the highest minimal value, and which range contains
         * the smallest maximal value. If we find the same range in both case (which can
         * be either 'this' or 'range), return that range. Otherwise we need to create a
         * new one.
         */
        final Range<? extends T> intersect, min, max;
        min = compareMinTo(range.minValue, range.isMinIncluded ? 0 : -1) < 0 ? range : this;
        max = compareMaxTo(range.maxValue, range.isMaxIncluded ? 0 : +1) > 0 ? range : this;
        if (min == max) {
            intersect = min;
        } else {
            intersect = create(min.minValue, min.isMinIncluded, max.maxValue, max.isMaxIncluded);
        }
        assert intersect.isEmpty() == !intersects(range) : intersect;
        return intersect;
    }

    /**
     * Returns the union of this range with the given range.
     *
     * @param  range The range to add to this range.
     * @return The union of this range with the given range.
     * @throws IllegalArgumentException is the given range can not be converted to a valid type
     *         through widening conversion, or if the units of measurement are not convertible.
     */
    public Range<?> union(final Range<?> range) throws IllegalArgumentException {
        return unionNC(ensureCompatible(range));
    }

    /**
     * Implementation of {@link #union(Range)} to be invoked directly by subclasses.
     * "NC" stands for "No Cast" - this method do not try to cast the value to a compatible type.
     */
    final Range<?> unionNC(final Range<? extends T> range) throws IllegalArgumentException {
        final Range<? extends T> union, min, max;
        min = compareMinTo(range.minValue, range.isMinIncluded ? 0 : -1) > 0 ? range : this;
        max = compareMaxTo(range.maxValue, range.isMaxIncluded ? 0 : +1) < 0 ? range : this;
        if (min == max) {
            union = min;
        } else {
            union = create(min.minValue, min.isMinIncluded, max.maxValue, max.isMaxIncluded);
        }
        assert union.contains(min) : min;
        assert union.contains(max) : max;
        return union;
    }

    /**
     * Returns the range of values that are in this range but not in the given range.
     * This method returns an array of length 0, 1 or 2:
     *
     * <ul>
     *   <li>If the given range contains fully this range, returns an array of length 0.</li>
     *   <li>If the given range is in the middle of this range, then the subtraction results in
     *       two disjoint ranges which will be returned as two elements in the array.</li>
     *   <li>Otherwise returns an array of length 1.</li>
     * </ul>
     *
     * @param  range The range to subtract.
     * @return This range without the given range.
     * @throws IllegalArgumentException is the given range can not be converted to a valid type
     *         through widening conversion, or if the units of measurement are not convertible.
     */
    public Range<?>[] subtract(final Range<?> range) throws IllegalArgumentException {
        return subtractNC(ensureCompatible(range));
    }

    /**
     * Implementation of {@link #subtract(Range)} to be invoked directly by subclasses.
     * "NC" stands for "No Cast" - this method do not try to cast the value to a compatible type.
     */
    final Range<T>[] subtractNC(final Range<? extends T> range) throws IllegalArgumentException {
        final Range<T> subtract;
        if (!intersects(range)) {
            subtract = this;
        } else {
            final boolean clipMin = compareMinTo(range.minValue, range.isMinIncluded ? 0 : -1) >= 0;
            final boolean clipMax = compareMaxTo(range.maxValue, range.isMaxIncluded ? 0 : +1) <= 0;
            if (clipMin) {
                if (clipMax) {
                    // The given range contains fully this range.
                    assert range.contains(this) : range;
                    return newArray(0);
                }
                subtract = create(range.maxValue, !range.isMaxIncluded, maxValue, isMaxIncluded);
            } else {
                if (!clipMax) {
                    final Range<T>[] array = newArray(2);
                    array[0] = create(minValue, isMinIncluded, range.minValue, !range.isMinIncluded);
                    array[1] = create(range.maxValue, !range.isMaxIncluded, maxValue, isMaxIncluded);
                    return array;
                }
                subtract = create(minValue, isMinIncluded, range.minValue, !range.isMinIncluded);
            }
        }
        assert contains(subtract) : subtract;
        assert !subtract.intersects(range) : subtract;
        final Range<T>[] array = newArray(1);
        array[0] = subtract;
        return array;
    }

    /**
     * Compares the {@linkplain #getMinValue() minimum value} of this range with the given bound of
     * another range. Since the given value is either the minimal or maximal value of another range,
     * it may be inclusive or exclusive. The later is specified by {@code position} as below:
     *
     * <ul>
     *   <li> 0 if {@code value} is inclusive.</li>
     *   <li>-1 if {@code value} is exclusive and lower than the inclusive values of the other range.</li>
     *   <li>+1 if {@code value} is exclusive and higher than the inclusive values of the other range.</li>
     * </ul>
     *
     * Note that the non-zero position shall be exactly -1 or +1, not arbitrary negative or positive.
     *
     * @param  value    A bound value of the other range to be compared to the minimal value of this range.
     * @param  position The position of {@code value} relative to the inclusive values of the other range.
     * @return Position (-, + or 0) of the inclusive values of this range compared to the other range.
     *
     * @see #containsNC(Range)
     */
    final int compareMinTo(final T value, int position) {
        /*
         * Check for infinite values.  If the given value is infinite, it can be either positive or
         * negative infinity, which we can infer from the 'position' argument. Note that 'position'
         * can not be 0 in such case, since infinities are always exclusive in this class.
         */
        if (minValue == null) {
            return (value == null) ? 0 : -1;
        }
        if (value == null) {
            return -position;
        }
        /*
         * Compare the two finite values. If they are not equal, we are done regardless the
         * inclusion states, because the difference between included and excluded values is
         * considered smaller than any quantity we can represent.
         */
        final int c = minValue.compareTo(value);
        if (c != 0) {
            return c;
        }
        /*
         * The two values are equal. If the 'minValue' of this range is inclusive, then the given
         * 'value' is directly at the "right" place (the beginning of the interior of this range),
         * so the 'position' argument gives directly the position of the "true minValue" relative
         * to the interior of the other range.
         *
         * But if 'minValue' is exclusive, then the "true minValue" of this range is one position
         * to the right  (where "position" is a counter for an infinitely small quantity, similar
         * to 'dx' in calculus). The effect is to return 0 if the given 'value' is also exclusive
         * and lower than the interior of the other range (position == -1),  and a positive value
         * in all other cases.
         */
        if (!isMinIncluded) {
            position++;
        }
        return position;
    }

    /**
     * Compares the {@linkplain #getMaxValue() maximum value} of this range with the given bound of
     * another range. See the comment in {@link #compareMinTo(Comparable, int)} for more details.
     */
    final int compareMaxTo(final T value, int position) {
        if (maxValue == null) {
            return (value == null) ? 0 : +1;
        }
        if (value == null) {
            return -position;
        }
        final int c = maxValue.compareTo(value);
        if (c != 0) {
            return c;
        }
        if (!isMaxIncluded) {
            position--;
        }
        return position;
    }

    @Override
    public boolean equals(Object object)
    {
        //make sure it's not null
        if (object == null)
        {
            return false;
        }


        Range<?> value = (Range<?>) object;
        if (value == null)
        {
            return false;
        }

        boolean retVal = true;
        retVal &= this.elementType == value.getElementType();
        if (value.isEmpty() && this.isEmpty())
        {
            return retVal;
        }

        retVal &= this.maxValue == value.getMaxValue();
        retVal &= this.minValue == value.getMinValue();
        retVal &= this.isMaxIncluded == value.isMaxIncluded();
        retVal &= this.isMinIncluded == value.isMinIncluded();
        return retVal;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 13 * hash + (this.minValue != null ? this.minValue.hashCode() : 0);
        hash = 13 * hash + (this.maxValue != null ? this.maxValue.hashCode() : 0);
        hash = 13 * hash + (this.elementType != null ? this.elementType.hashCode() : 0);
        hash = 13 * hash + (this.isMinIncluded ? 1 : 0);
        hash = 13 * hash + (this.isMaxIncluded ? 1 : 0);
        return hash;
    }
}
