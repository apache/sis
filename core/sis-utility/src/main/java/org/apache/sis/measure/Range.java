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

import java.util.Formatter;
import java.util.Formattable;
import java.util.FormattableFlags;
import java.io.Serializable;
import javax.measure.unit.Unit;
import org.apache.sis.internal.util.Utilities;
import org.apache.sis.internal.util.PatchedUnitFormat;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.Numbers;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A set of minimum and maximum values of a certain class, allowing
 * a user to determine if a value of the same class is contained inside the range.
 * The minimum and maximum values do not have to be included in the range, and
 * can be null.  If the minimum or maximum values are null, the range is said to
 * be unbounded on that endpoint. If both the minimum and maximum are null,
 * the range is completely unbounded and all values of that class are contained
 * within the range. Null values are always considered <em>exclusive</em>,
 * since iterations over the values will never reach the infinite endpoint.
 *
 * <p>The minimal and maximal values (the <cite>endpoints</cite>) may be inclusive or exclusive.
 * Numeric ranges where both endpoints are inclusive are called <cite>closed intervals</cite>
 * and are represented by square brackets, for example "{@code [0 … 255]}".
 * Numeric ranges where both endpoints are exclusive are called <cite>open intervals</cite>
 * and are represented by parenthesis, for example "{@code (0 … 256)}".</p>
 *
 * <div class="section">Type and value of range elements</div>
 * To be a member of a {@code Range}, the {@code <E>} type defining the range must implement the
 * {@link Comparable} interface. All argument values given to the methods of this class shall be
 * or contain instances of that {@code <E>} type. The type is enforced by parameterized type,
 * but some subclasses may put additional constraints. For example {@link MeasurementRange} will
 * additionally checks the units of measurement. Consequently every methods defined in this class
 * may throw an {@link IllegalArgumentException} if a given argument does not meet some constraint
 * beyond the type.
 *
 * <div class="section">Relationship with ISO 19123 definition of range</div>
 * The ISO 19123 standard (<cite>Coverage geometry and functions</cite>) defines the range as the set
 * (either finite or {@linkplain org.opengis.geometry.TransfiniteSet transfinite}) of feature attribute
 * values associated by a function (the {@linkplain org.opengis.coverage.Coverage coverage}) with the
 * elements of the coverage domain. In other words, if we see a coverage as a function, then a range
 * is the set of possible return values.
 *
 * <p>The characteristics of the spatial domain are defined by the ISO 19123 standard whereas the
 * characteristics of the attribute range are not part of that standard. In Apache SIS, those
 * characteristics are described by the {@link org.apache.sis.coverage.SampleDimension} class,
 * which may contain one or many {@code Range} instances. Consequently this {@code Range} class
 * is closely related, but not identical, to the ISO 19123 definition or range.</p>
 *
 * <p>Ranges are not necessarily numeric. Numeric and non-numeric ranges can be associated to
 * {@linkplain org.opengis.coverage.DiscreteCoverage discrete coverages}, while typically only
 * numeric ranges can be associated to {@linkplain org.opengis.coverage.ContinuousCoverage
 * continuous coverages}.</p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class and the {@link NumberRange} / {@link MeasurementRange} subclasses are immutable,
 * and thus inherently thread-safe. Other subclasses may or may not be immutable, at implementation choice.
 * But implementors are encouraged to make sure that all subclasses remain immutable for more predictable behavior.
 *
 * @param <E> The type of range elements, typically a {@link Number} subclass or {@link java.util.Date}.
 *
 * @author  Joe White
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Jody Garnett (for parameterized type inspiration)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see RangeFormat
 * @see org.apache.sis.util.collection.RangeSet
 */
public class Range<E extends Comparable<? super E>> implements CheckedContainer<E>, Formattable, Emptiable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 603508245068333284L;

    /**
     * The base type of elements in this range.
     *
     * @see #getElementType()
     */
    final Class<E> elementType;

    /**
     * The minimal and maximal values.
     */
    final E minValue, maxValue;

    /**
     * Whether the minimal or maximum value is included.
     */
    final boolean isMinIncluded, isMaxIncluded;

    /**
     * Constructs a range with the same type and the same values than the specified range.
     * This is a copy constructor.
     *
     * @param range The range to copy.
     */
    public Range(final Range<E> range) {
        elementType   = range.elementType;
        minValue      = range.minValue;
        isMinIncluded = range.isMinIncluded;
        maxValue      = range.maxValue;
        isMaxIncluded = range.isMaxIncluded;
        assert validate() : elementType;
    }

    /**
     * Creates a new range bounded by the given endpoint values.
     *
     * <div class="note"><b>Assertion:</b>
     * This constructor verifies the {@code minValue} and {@code maxValue} arguments type if Java assertions
     * are enabled. This verification is not performed in normal execution because theoretically unnecessary
     * unless Java generic types have been tricked.</div>
     *
     * @param elementType    The base type of the range elements.
     * @param minValue       The minimal value, or {@code null} if none.
     * @param isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param maxValue       The maximal value, or {@code null} if none.
     * @param isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     */
    public Range(final Class<E> elementType,
            final E minValue, final boolean isMinIncluded,
            final E maxValue, final boolean isMaxIncluded)
    {
        ArgumentChecks.ensureNonNull("elementType", elementType);
        /*
         * The 'isMin/Maxincluded' flags must be forced to 'false' if 'minValue' or 'maxValue'
         * are null. This is required for proper working of algorithms implemented in this class.
         */
        this.elementType   = elementType;
        this.minValue      = minValue;
        this.isMinIncluded = isMinIncluded && (minValue != null);
        this.maxValue      = maxValue;
        this.isMaxIncluded = isMaxIncluded && (maxValue != null);
        assert validate() : elementType;
    }

    /**
     * Creates a new range using the same element type than this range. This method will
     * be overridden by subclasses in order to create a range of a more specific type.
     *
     * <div class="note"><b>API note:</b>
     * This method is invoked by all operations (union, intersection, <i>etc.</i>) that may create a new range.
     * But despite this fact, the return type of those methods are nailed down to {@code Range} (i.e. subclasses
     * shall not override the above-cited operations with covariant return type) because those operations may return
     * the given argument directly, and we have no guarantees on the type of those arguments.</div>
     */
    Range<E> create(final E minValue, final boolean isMinIncluded,
                    final E maxValue, final boolean isMaxIncluded)
    {
        return new Range<E>(elementType, minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Returns an initially empty array of {@code getClass()} type and of the given length.
     * This method is overridden by subclasses in order to create arrays of more specific type.
     * This method is invoked by the {@link #subtract(Range)} method. It is okay to use the new
     * array only if the ranges to store in that array are only {@code this} or new ranges created
     * by the {@link #create(Comparable, boolean, Comparable, boolean)} method - otherwise we may
     * get an {@link ArrayStoreException}.
     */
    @SuppressWarnings({"unchecked","rawtypes"}) // Generic array creation.
    Range<E>[] newArray(final int length) {
        return new Range[length];
    }

    /**
     * To be overridden by {@link MeasurementRange} only.
     *
     * @return The unit of measurement, or {@code null}.
     */
    Unit<?> unit() {
        return null;
    }

    /**
     * Invoked by the constructors in order to ensure that the argument are of valid types.
     * This check is performed only when assertions are enabled. This test is not needed in
     * normal execution if the users do not bypass the checks performed by generic types.
     */
    private boolean validate() {
        ArgumentChecks.ensureCanCast("minValue", elementType, minValue);
        ArgumentChecks.ensureCanCast("maxValue", elementType, maxValue);
        return Comparable.class.isAssignableFrom(elementType);
    }

    /**
     * Returns the base type of elements in this range.
     * This is the type specified at construction time.
     */
    @Override
    public Class<E> getElementType() {
        return elementType;
    }

    /**
     * Returns the minimal value, or {@code null} if this range has no lower limit.
     * If non-null, the returned value is either inclusive or exclusive depending on
     * the boolean returned by {@link #isMinIncluded()}.
     *
     * @return The minimal value, or {@code null} if this range is unbounded on the lower side.
     */
    public E getMinValue() {
        return minValue;
    }

    /**
     * Returns {@code true} if the {@linkplain #getMinValue() minimal value} is inclusive,
     * or {@code false} if exclusive. Note that {@code null} values are always considered
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
    public E getMaxValue() {
        return maxValue;
    }

    /**
     * Returns {@code true} if the {@linkplain #getMaxValue() maximal value} is inclusive,
     * or {@code false} if exclusive. Note that {@code null} values are always considered
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
     * <div class="note"><b>API note:</b>
     * This method is final because often used by the internal implementation.
     * Making the method final ensures that the other methods behave consistently.</div>
     *
     * @return {@code true} if this range is empty.
     */
    @Override
    public final boolean isEmpty() {
        if (minValue == null || maxValue == null) {
            return false; // Unbounded: can not be empty.
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
     */
    public boolean contains(final E value) {
        if (value == null) {
            return false;
        }
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
     * @throws IllegalArgumentException is the given range is incompatible,
     *         for example because of incommensurable units of measurement.
     */
    public boolean contains(final Range<? extends E> range) {
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
     * @throws IllegalArgumentException is the given range is incompatible,
     *         for example because of incommensurable units of measurement.
     */
    public boolean intersects(final Range<? extends E> range) {
        return (compareMinTo(range.maxValue, range.isMaxIncluded ? 0 : +1) <= 0) &&
               (compareMaxTo(range.minValue, range.isMinIncluded ? 0 : -1) >= 0);
    }

    /**
     * Returns the intersection between this range and the given range.
     *
     * @param  range The range to intersect.
     * @return The intersection of this range with the given range.
     * @throws IllegalArgumentException is the given range is incompatible,
     *         for example because of incommensurable units of measurement.
     */
    public Range<E> intersect(final Range<E> range) {
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
        final Range<E> intersect, min, max;
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
     * @throws IllegalArgumentException is the given range is incompatible,
     *         for example because of incommensurable units of measurement.
     */
    public Range<E> union(final Range<E> range) {
        final Range<E> union, min, max;
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
     * @return This range without the given range, as an array of length 0, 1 or 2.
     * @throws IllegalArgumentException is the given range is incompatible,
     *         for example because of incommensurable units of measurement.
     */
    public Range<E>[] subtract(final Range<E> range) {
        /*
         * Implementation note: never store the 'range' argument value in the array
         * returned by 'newArray(int)', otherwise we may get an ArrayStoreException.
         */
        final Range<E> subtract;
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
                    final Range<E>[] array = newArray(2);
                    array[0] = create(minValue, isMinIncluded, range.minValue, !range.isMinIncluded);
                    array[1] = create(range.maxValue, !range.isMaxIncluded, maxValue, isMaxIncluded);
                    return array;
                }
                subtract = create(minValue, isMinIncluded, range.minValue, !range.isMinIncluded);
            }
        }
        assert contains(subtract) : subtract;
        assert !subtract.intersects(range) : subtract;
        final Range<E>[] array = newArray(1);
        array[0] = subtract;
        return array;
    }

    /**
     * Compares the {@linkplain #getMinValue() minimum value} of this range with the given endpoint of
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
     * @param  value    An endpoint value of the other range to be compared to the minimal value of this range.
     * @param  position The position of {@code value} relative to the inclusive values of the other range.
     * @return Position (-, + or 0) of the inclusive values of this range compared to the other range.
     *
     * @see #contains(Range)
     */
    private int compareMinTo(final E value, int position) {
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
     * Compares the {@linkplain #getMaxValue() maximum value} of this range with the given endpoint
     * of another range. See the comment in {@link #compareMinTo(Comparable, int)} for more details.
     */
    private int compareMaxTo(final E value, int position) {
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

    /**
     * Compares this range with the given object for equality.
     * Two ranges are considered equal if they have the same {@link #getElementType() element type} and:
     *
     * <ul>
     *   <li>are both {@linkplain #isEmpty() empty}, or</li>
     *   <li>have equal {@linkplain #getMinValue() minimum} and {@linkplain #getMaxValue() maximum} values
     *       with equal inclusive/exclusive flags.</li>
     * </ul>
     *
     * Note that subclasses may add other requirements, for example on units of measurement.
     *
     * @param  object The object to compare with this range for equality.
     * @return {@code true} if the given object is equal to this range.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            final Range<?> other = (Range<?>) object;
            if (Objects.equals(elementType, other.elementType)) {
                if (isEmpty()) {
                    return other.isEmpty();
                }
                return Objects.equals(minValue, other.minValue) &&
                       Objects.equals(maxValue, other.maxValue) &&
                       isMinIncluded == other.isMinIncluded &&
                       isMaxIncluded == other.isMaxIncluded;
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for this range.
     */
    @Override
    public int hashCode() {
        int hash = elementType.hashCode();
        if (!isEmpty()) {
            hash = 13 * hash + Objects.hashCode(minValue);
            hash = 13 * hash + Objects.hashCode(maxValue);
            hash += isMinIncluded ?   17 :   37;
            hash += isMaxIncluded ? 1231 : 1237;
        }
        return hash ^ (int) serialVersionUID;
    }

    /**
     * Returns {@code true} if the given number is formatted with only one character.
     * We will use less space if the minimum and maximum values are formatted using
     * only one digit. This method assumes that we have verified that the element type
     * is an integer type before to invoke this method.
     */
    @SuppressWarnings("unchecked")
    private static boolean isCompact(final Comparable<?> value, final boolean ifNull) {
        if (value == null) {
            return ifNull;
        }
        final long n = ((Number) value).longValue();
        return n >= 0 && n < 10;
    }

    /**
     * Returns a unlocalized string representation of this range. This method complies to the format
     * described in the <a href="http://en.wikipedia.org/wiki/ISO_31-11">ISO 31-11</a> standard,
     * except that the minimal and maximal values are separated by the "{@code …}" character
     * instead than coma. More specifically, the string representation is defined as below:
     *
     * <ul>
     *   <li>If the range {@linkplain #isEmpty() is empty}, then this method returns "{@code {}}".</li>
     *   <li>Otherwise if the minimal value is equals to the maximal value, then the string
     *       representation of that value is returned inside braces as in "{@code {value}}".</li>
     *   <li>Otherwise the string representation of the minimal and maximal values are formatted
     *       like "{@code [min … max]}" for inclusive endpoints or "{@code (min … max)}" for exclusive
     *       endpoints, or a mix of both styles. The "{@code ∞}" symbol is used in place of
     *       {@code min} or {@code max} for unbounded ranges.</li>
     * </ul>
     *
     * If this range is a {@link MeasurementRange}, then the {@linkplain Unit unit of measurement}
     * is appended to the above string representation except for empty ranges.
     *
     * @see RangeFormat
     * @see <a href="http://en.wikipedia.org/wiki/ISO_31-11">Wikipedia: ISO 31-11</a>
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        final StringBuilder buffer = new StringBuilder(20);
        if (minValue != null && minValue.equals(maxValue)) {
            buffer.append('{').append(minValue).append('}');
        } else {
            buffer.append(isMinIncluded ? '[' : '(');
            if (minValue == null) {
                buffer.append("−∞");
            } else {
                buffer.append(minValue);
            }
            // Compact representation for integers, more space for real numbers.
            if (Numbers.isInteger(elementType) && isCompact(minValue, false) && isCompact(maxValue, true)) {
                buffer.append('…');
            } else {
                buffer.append(" … ");
            }
            if (maxValue == null) {
                buffer.append('∞');
            } else {
                buffer.append(maxValue);
            }
            buffer.append(isMaxIncluded ? ']' : ')');
        }
        final Unit<?> unit = unit();
        if (unit != null) {
            final String symbol = PatchedUnitFormat.toString(unit);
            if (!symbol.isEmpty()) {
                if (Character.isLetterOrDigit(symbol.codePointAt(0))) {
                    buffer.append(' ');
                }
                buffer.append(symbol);
            }
        }
        return buffer.toString();
    }

    /**
     * Formats this range using the provider formatter. This method is invoked when an
     * {@code Range} object is formatted using the {@code "%s"} conversion specifier of
     * {@link Formatter}. Users don't need to invoke this method explicitely.
     *
     * <p>If the alternate flags is present (as in {@code "%#s"}), then the range will
     * be formatted using the {@linkplain RangeFormat#isAlternateForm() alternate form}
     * for exclusive bounds.</p>
     *
     * @param formatter The formatter in which to format this angle.
     * @param flags     {@link FormattableFlags#LEFT_JUSTIFY} for left alignment, or 0 for right alignment.
     * @param width     Minimal number of characters to write, padding with {@code ' '} if necessary.
     * @param precision Maximal number of characters to write, or -1 if no limit.
     */
    @Override
    public void formatTo(final Formatter formatter, final int flags, final int width, int precision) {
        final String value;
        if (precision == 0) {
            value = "";
        } else {
            final RangeFormat format = new RangeFormat(formatter.locale(), elementType);
            format.setAlternateForm((flags & FormattableFlags.ALTERNATE) != 0);
            value = format.format(this, new StringBuffer(), null).toString();
        }
        Utilities.formatTo(formatter, flags, width, precision, value);
    }
}
