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

import net.jcip.annotations.Immutable;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A range of numbers. {@linkplain #union Union} and {@linkplain #intersect intersection}
 * are computed as usual, except that widening conversions will be applied as needed.
 *
 * <p>{@code NumberRange} has no units. For a range of physical measurements with units of
 * measure, see {@link MeasurementRange}.</p>
 *
 * @param <T> The type of range elements as a subclass of {@link Number}.
 *
 * @author  Martin Desruisseaux (IRD)
 * @author  Jody Garnett (for parameterized type suggestion)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 *
 * @see RangeFormat
 */
@Immutable
public class NumberRange<T extends Number & Comparable<? super T>> extends Range<T> {
    //
    // IMPLEMENTATION NOTE: This class is full of @SuppressWarnings("unchecked") annotations.
    // Nevertheless we should never get ClassCastException - if we get some, this would be a
    // bug in this implementation. Users may get IllegalArgumentException however.
    //

    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -818167965963008231L;

    /**
     * Constructs an inclusive range of {@code byte} values.
     *
     * @param  minValue The minimal value, inclusive.
     * @param  maxValue The maximal value, <strong>inclusive</strong>.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Byte> create(final byte minValue, final byte maxValue) {
        return new NumberRange<>(Byte.class, Byte.valueOf(minValue), Byte.valueOf(maxValue));
    }

    /**
     * Constructs a range of {@code byte} values.
     *
     * @param  minValue       The minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Byte> create(final byte minValue, final boolean isMinIncluded,
                                           final byte maxValue, final boolean isMaxIncluded)
    {
        return new NumberRange<>(Byte.class,
                Byte.valueOf(minValue), isMinIncluded,
                Byte.valueOf(maxValue), isMaxIncluded);
    }

    /**
     * Constructs an inclusive range of {@code short} values.
     *
     * @param  minValue The minimal value, inclusive.
     * @param  maxValue The maximal value, <strong>inclusive</strong>.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Short> create(final short minValue, final short maxValue) {
        return new NumberRange<>(Short.class, Short.valueOf(minValue), Short.valueOf(maxValue));
    }

    /**
     * Constructs a range of {@code short} values.
     *
     * @param  minValue       The minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Short> create(final short minValue, final boolean isMinIncluded,
                                            final short maxValue, final boolean isMaxIncluded)
    {
        return new NumberRange<>(Short.class,
                Short.valueOf(minValue), isMinIncluded,
                Short.valueOf(maxValue), isMaxIncluded);
    }

    /**
     * Constructs an inclusive range of {@code int} values.
     *
     * @param  minValue The minimal value, inclusive.
     * @param  maxValue The maximal value, <strong>inclusive</strong>.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Integer> create(final int minValue, final int maxValue) {
        return new NumberRange<>(Integer.class, Integer.valueOf(minValue), Integer.valueOf(maxValue));
    }

    /**
     * Constructs a range of {@code int} values.
     *
     * @param  minValue       The minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Integer> create(final int minValue, final boolean isMinIncluded,
                                              final int maxValue, final boolean isMaxIncluded)
    {
        return new NumberRange<>(Integer.class,
                Integer.valueOf(minValue), isMinIncluded,
                Integer.valueOf(maxValue), isMaxIncluded);
    }

    /**
     * Constructs an inclusive range of {@code long} values.
     *
     * @param  minValue The minimal value, inclusive.
     * @param  maxValue The maximal value, <strong>inclusive</strong>.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Long> create(final long minValue, final long maxValue) {
        return new NumberRange<>(Long.class, Long.valueOf(minValue), Long.valueOf(maxValue));
    }

    /**
     * Constructs a range of {@code long} values.
     *
     * @param  minValue       The minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Long> create(final long minValue, final boolean isMinIncluded,
                                           final long maxValue, final boolean isMaxIncluded)
    {
        return new NumberRange<>(Long.class,
                Long.valueOf(minValue), isMinIncluded,
                Long.valueOf(maxValue), isMaxIncluded);
    }

    /**
     * Constructs an inclusive range of {@code float} values.
     * The values can not be {@link Float#NaN}.
     *
     * @param  minValue The minimal value, inclusive, or {@link Float#NEGATIVE_INFINITY} if none..
     * @param  maxValue The maximal value, <strong>inclusive</strong>, or {@link Float#POSITIVE_INFINITY} if none.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Float> create(final float minValue, final float maxValue) {
        return new NumberRange<>(Float.class,
                valueOf("minValue", minValue, Float.NEGATIVE_INFINITY),
                valueOf("maxValue", maxValue, Float.POSITIVE_INFINITY));
    }

    /**
     * Constructs a range of {@code float} values.
     * The values can not be {@link Float#NaN}.
     *
     * @param  minValue       The minimal value, or {@link Float#NEGATIVE_INFINITY} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value, or {@link Float#POSITIVE_INFINITY} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Float> create(final float minValue, final boolean isMinIncluded,
                                            final float maxValue, final boolean isMaxIncluded)
    {
        return new NumberRange<>(Float.class,
                valueOf("minValue", minValue, Float.NEGATIVE_INFINITY), isMinIncluded,
                valueOf("maxValue", maxValue, Float.POSITIVE_INFINITY), isMaxIncluded);
    }

    /**
     * Returns the {@code Float} wrapper of the given primitive {@code float},
     * or {@code null} if it equals to the infinity value.
     */
    static Float valueOf(final String name, final float value, final float infinity) {
        if (Float.isNaN(value)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotANumber_1, name));
        }
        return (value != infinity) ? Float.valueOf(value) : null;
    }

    /**
     * Constructs an inclusive range of {@code double} values.
     * The values can not be {@link Double#NaN}.
     *
     * @param  minValue The minimal value, inclusive, or {@link Double#NEGATIVE_INFINITY} if none..
     * @param  maxValue The maximal value, <strong>inclusive</strong>, or {@link Double#POSITIVE_INFINITY} if none.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Double> create(final double minValue, final double maxValue) {
        return new NumberRange<>(Double.class,
                valueOf("minValue", minValue, Double.NEGATIVE_INFINITY),
                valueOf("maxValue", maxValue, Double.POSITIVE_INFINITY));
    }

    /**
     * Constructs a range of {@code double} values.
     * The values can not be {@link Double#NaN}.
     *
     * @param  minValue       The minimal value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given bounds.
     */
    public static NumberRange<Double> create(final double minValue, final boolean isMinIncluded,
                                             final double maxValue, final boolean isMaxIncluded)
    {
        return new NumberRange<>(Double.class,
                valueOf("minValue", minValue, Double.NEGATIVE_INFINITY), isMinIncluded,
                valueOf("maxValue", maxValue, Double.POSITIVE_INFINITY), isMaxIncluded);
    }

    /**
     * Returns the {@code Double} wrapper of the given primitive {@code double},
     * or {@code null} if it equals to the infinity value.
     */
    static Double valueOf(final String name, final double value, final double infinity) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotANumber_1, name));
        }
        return (value != infinity) ? Double.valueOf(value) : null;
    }

    /**
     * Constructs a range using the smallest type of {@link Number} that can hold the
     * given values. The given numbers don't need to be of the same type since they will
     * be {@linkplain Numbers#cast(Number, Class) casted} as needed. More specifically:
     *
     * <ul>
     *   <li>If the values are between {@value java.lang.Byte#MIN_VALUE} and
     *       {@value java.lang.Byte#MAX_VALUE} inclusive, then the given values are converted
     *       to {@link Byte} objects and a {@code NumberRange} is created from them.</li>
     *   <li>Otherwise if the values are between {@value java.lang.Short#MIN_VALUE} and
     *       {@value java.lang.Short#MAX_VALUE} inclusive, then the given values are converted
     *       to {@link Short} objects and a {@code NumberRange} is created from them.</li>
     *   <li>Otherwise the {@link Integer} type is tested in the same way, then the
     *       {@link Long} type, and finally the {@link Float} type.</li>
     *   <li>If none of the above types is suitable, then the {@link Double} type is used.</li>
     * </ul>
     *
     * @param  minValue       The minimal value, or {@code null} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value, or {@code null} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range, or {@code null} if both {@code minValue} and {@code maxValue} are {@code null}.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    public static NumberRange<?> createBestFit(final Number minValue, final boolean isMinIncluded,
                                               final Number maxValue, final boolean isMaxIncluded)
    {
        final Class<? extends Number> type = Numbers.widestClass(
                Numbers.narrowestClass(minValue), Numbers.narrowestClass(maxValue));
        return (type == null) ? null :
            new NumberRange(type, Numbers.cast(minValue, type), isMinIncluded,
                                  Numbers.cast(maxValue, type), isMaxIncluded);
    }

    /**
     * Wraps the specified {@link Range} in a {@code NumberRange} object. If the specified
     * range is already an instance of {@code NumberRange}, then it is returned unchanged.
     * Otherwise a new number range is created using the {@linkplain #NumberRange(Range)
     * copy constructor}.
     *
     * @param  <N> The type of elements in the given range.
     * @param  range The range to wrap.
     * @return The same range than {@code range} as a {@code NumberRange} object.
     */
    public static <N extends Number & Comparable<? super N>> NumberRange<N> wrap(final Range<N> range) {
        if (range instanceof NumberRange<?>) {
            return (NumberRange<N>) range;
        }
        // The constructor will ensure that the range element class is a subclass of Number.
        return new NumberRange<>(range);
    }

    /**
     * Constructs a range with the same type and the same values than the specified range.
     * This is a copy constructor.
     *
     * @param range The range to copy. The elements must be {@link Number} instances.
     */
    public NumberRange(final Range<T> range) {
        super(range);
    }

    /**
     * Constructs an inclusive range of {@link Number} objects.
     *
     * @param  type     The element class, usually one of {@link Byte}, {@link Short},
     *                  {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  minValue The minimum value, inclusive, or {@code null} if none.
     * @param  maxValue The maximum value, <strong>inclusive</strong>, or {@code null} if none.
     */
    public NumberRange(final Class<T> type, final T minValue, final T maxValue) {
        super(type, minValue, maxValue);
    }

    /**
     * Constructs a range of {@link Number} objects.
     *
     * @param type           The element class, usually one of {@link Byte}, {@link Short},
     *                       {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param minValue       The minimal value, or {@code null} if none.
     * @param isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param maxValue       The maximal value, or {@code null} if none.
     * @param isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     */
    public NumberRange(final Class<T> type,
                       final T minValue, final boolean isMinIncluded,
                       final T maxValue, final boolean isMaxIncluded)
    {
        super(type, minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Constructs a range with the same values than the specified range,
     * casted to the specified type.
     *
     * @param  type  The element class, usually one of {@link Byte}, {@link Short},
     *               {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  range The range to copy. The elements must be {@link Number} instances.
     * @throws IllegalArgumentException If the given type is not one of the primitive
     *         wrappers for numeric types.
     */
    NumberRange(final Class<T> type, final Range<? extends Number> range)
            throws IllegalArgumentException
    {
        super(type, Numbers.cast(range.minValue, type), range.isMinIncluded,
                    Numbers.cast(range.maxValue, type), range.isMaxIncluded);
    }

    /**
     * Creates a new range using the same element class than this range. This method will
     * be overridden by subclasses in order to create a range of a more specific type.
     */
    @Override
    NumberRange<T> create(final T minValue, final boolean isMinIncluded,
                          final T maxValue, final boolean isMaxIncluded)
    {
        return new NumberRange<>(elementType, minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Ensures that {@link #elementType} is compatible with the type expected by this range class.
     * Invoked for argument checking by the super-class constructor.
     */
    @Override
    final void ensureValidType() throws IllegalArgumentException {
        ensureNumberClass(elementType);
        super.ensureValidType(); // Check that the type implements also Comparable.
    }

    /**
     * Ensures that the given class is {@link Number} or a subclass.
     */
    private static void ensureNumberClass(final Class<?> type) throws IllegalArgumentException {
        if (!Number.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalClass_2, Number.class, type));
        }
    }

    /**
     * Returns the type of minimum and maximum values.
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Number> getElementType(final Range<?> range) {
        ArgumentChecks.ensureNonNull("range", range);
        final Class<?> type = range.elementType;
        ensureNumberClass(type);
        /*
         * Safe because we checked in the above line. We could have used Class.asSubclass(Class)
         * instead but we want an IllegalArgumentException in case of failure rather than a
         * ClassCastException.
         */
        return (Class<? extends Number>) type;
    }

    /**
     * Casts the specified range to the specified type.  If this class is associated to a unit of
     * measurement, then this method converts the {@code range} units to the same units than this
     * instance.  This method is overridden by {@link MeasurementRange} only in the way described
     * above.
     *
     * @param  type The class to cast to. Must be one of {@link Byte}, {@link Short},
     *              {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @return The casted range, or {@code range} if no cast is needed.
     * @throws IllegalArgumentException If the given type is not one of the primitive
     *         wrappers for numeric types.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    <N extends Number & Comparable<? super N>>
    NumberRange<N> convertAndCast(final Range<? extends Number> range, final Class<N> type)
            throws IllegalArgumentException
    {
        if (type.equals(range.getElementType())) {
            // Safe because we checked in the line just above.
            return (NumberRange<N>) wrap((Range) range);
        }
        return new NumberRange<>(type, range);
    }

    /**
     * Casts this range to the specified type. If the cast from this range type to the given
     * type is a narrowing conversion, then the cast is performed according the rules of the
     * Java language: the high-order bytes are silently dropped.
     *
     * @param  <N>   The class to cast to.
     * @param  type  The class to cast to. Must be one of {@link Byte}, {@link Short},
     *               {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @return The casted range, or {@code this} if this range already uses the specified type.
     * @throws IllegalArgumentException If the given type is not one of the primitive
     *         wrappers for numeric types.
     */
    public <N extends Number & Comparable<? super N>> NumberRange<N> castTo(final Class<N> type)
            throws IllegalArgumentException
    {
        return convertAndCast(this, type);
    }

    /**
     * Returns an initially empty array of the given length.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"}) // Generic array creation.
    NumberRange<T>[] newArray(final int length) {
        return new NumberRange[length];
    }

    /**
     * Returns {@code true} if the specified value is within this range.
     * This method delegates to {@link #contains(Comparable)}.
     *
     * @param  value The value to check for inclusion.
     * @return {@code true} if the given value is within this range.
     * @throws IllegalArgumentException if the given value is not comparable.
     */
    public final boolean contains(final Number value) throws IllegalArgumentException {
        if (value != null && !(value instanceof Comparable<?>)) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.NotComparableClass_1, value.getClass()));
        }
        return contains((Comparable<?>) value);
    }

    /**
     * Returns {@code true} if the specified value is within this range.
     * The given value must be a subclass of {@link Number}.
     *
     * @throws IllegalArgumentException If the given type is not one of the primitive
     *         wrappers for numeric types.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public boolean contains(Comparable<?> value) throws IllegalArgumentException {
        if (value == null) {
            return false;
        }
        ArgumentChecks.ensureCanCast("value", Number.class, value);
        /*
         * Suppress warning because we checked the class in the line just above, so we are safe.
         * We could have used Class.cast(Object) but we want an IllegalArgumentException with a
         * localized message.
         */
        Number number = (Number) value;
        final Class<? extends Number> type = Numbers.widestClass(elementType, number.getClass());
        number = Numbers.cast(number, type);
        /*
         * The 'type' bounds should actually be <? extends Number & Comparable> since the method
         * signature expect a Comparable and we have additionally casted to a Number.  However I
         * have not found a way to express that safely in a local variable with Java 6.
         */
        return castTo((Class) type).containsNC((Comparable<?>) number);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public boolean contains(Range<?> range) throws IllegalArgumentException {
        final Class<? extends Number> type = Numbers.widestClass(elementType, getElementType(range));
        /*
         * The type bounds is actually <? extends Number & Comparable> but I'm unable to express
         * it as local variable as of Java 6. So we have to bypass the compiler check, but those
         * casts are actually safes - including the (Range) cast - because getElementType(range)
         * would have throw an exception otherwise.
         */
        range = convertAndCast((Range) range, (Class) type);
        return castTo((Class) type).containsNC(range);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public boolean intersects(Range<?> range) throws IllegalArgumentException {
        final Class<? extends Number> type = Numbers.widestClass(elementType, getElementType(range));
        range = convertAndCast((Range) range, (Class) type); // Same comment than contains(Range).
        return castTo((Class) type).intersectsNC(range);
    }

    /**
     * {@inheritDoc}
     * Widening conversions will be applied as needed.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public NumberRange<?> union(Range<?> range) throws IllegalArgumentException {
        final Class<? extends Number> type = Numbers.widestClass(elementType, getElementType(range));
        range = convertAndCast((Range) range, (Class) type); // Same comment than contains(Range).
        return (NumberRange) castTo((Class) type).unionNC(range);
    }

    /**
     * {@inheritDoc}
     * Widening conversions will be applied as needed.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public NumberRange<?> intersect(Range<?> range) throws IllegalArgumentException {
        final Class<? extends Number> rangeType = getElementType(range);
        Class<? extends Number> type = Numbers.widestClass(elementType, rangeType);
        range = castTo((Class) type).intersectNC(convertAndCast((Range) range, (Class) type));
        /*
         * Use a finer type capable to holds the result (since the intersection
         * may have reduced the range), but not finer than the finest type of
         * the ranges used in the intersection calculation.
         */
        type = Numbers.narrowestClass(elementType, rangeType);
        type = Numbers.widestClass(type, Numbers.narrowestClass((Number) range.minValue));
        type = Numbers.widestClass(type, Numbers.narrowestClass((Number) range.maxValue));
        return convertAndCast((Range) range, (Class) type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public NumberRange<?>[] subtract(Range<?> range) throws IllegalArgumentException {
        Class<? extends Number> type = Numbers.widestClass(elementType, getElementType(range));
        return (NumberRange[]) castTo((Class) type)
                .subtractNC(convertAndCast((Range) range, (Class) type));
    }

    /**
     * Returns the {@linkplain #getMinValue() minimum value} as a {@code double}.
     * If this range is unbounded, then {@link Double#NEGATIVE_INFINITY} is returned.
     *
     * @return The minimum value.
     */
    @SuppressWarnings("unchecked")
    public double getMinimum() {
        final Number value = (Number) getMinValue();
        return (value != null) ? value.doubleValue() : Double.NEGATIVE_INFINITY;
    }

    /**
     * Returns the {@linkplain #getMinimum() minimum value} with the specified inclusive or
     * exclusive state. If this range is unbounded, then {@link Double#NEGATIVE_INFINITY} is
     * returned.
     *
     * @param  inclusive {@code true} for the minimum value inclusive,
     *         or {@code false} for the minimum value exclusive.
     * @return The minimum value, inclusive or exclusive as requested.
     */
    public double getMinimum(final boolean inclusive) {
        double value = getMinimum();
        if (inclusive != isMinIncluded()) {
            value = next(getElementType(), value, inclusive);
        }
        return value;
    }

    /**
     * Returns the {@linkplain #getMaxValue() maximum value} as a {@code double}.
     * If this range is unbounded, then {@link Double#POSITIVE_INFINITY} is returned.
     *
     * @return The maximum value.
     */
    @SuppressWarnings("unchecked")
    public double getMaximum() {
        final Number value = (Number) getMaxValue();
        return (value != null) ? value.doubleValue() : Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the {@linkplain #getMaximum() maximum value} with the specified inclusive or
     * exclusive state. If this range is unbounded, then {@link Double#POSITIVE_INFINITY} is
     * returned.
     *
     * @param  inclusive {@code true} for the maximum value inclusive,
     *         or {@code false} for the maximum value exclusive.
     * @return The maximum value, inclusive or exclusive as requested.
     */
    public double getMaximum(final boolean inclusive) {
        double value = getMaximum();
        if (inclusive != isMaxIncluded()) {
            value = next(getElementType(), value, !inclusive);
        }
        return value;
    }

    /**
     * Returns the next value for the given type.
     *
     * @param  type  The element type.
     * @param  value The value to increment or decrement.
     * @param  up    {@code true} for incrementing, or {@code false} for decrementing.
     * @return The adjacent value.
     */
    private static double next(final Class<?> type, double value, final boolean up) {
        if (!up) {
            value = -value;
        }
        if (Numbers.isInteger(type)) {
            value++;
        } else if (type.equals(Float.class)) {
            value = Math.nextUp((float) value);
        } else if (type.equals(Double.class)) {
            value = Math.nextUp(value);
        } else {
            // Thrown IllegalStateException instead than IllegalArgumentException because
            // the 'type' argument given to this method come from a NumberRange field.
            throw new IllegalStateException(Errors.format(Errors.Keys.NotAPrimitiveWrapper_1, type));
        }
        if (!up) {
            value = -value;
        }
        return value;
    }
}
