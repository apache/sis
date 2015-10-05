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

import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.collection.WeakHashSet;


/**
 * A range of numbers capable of widening conversions when performing range operations.
 * {@code NumberRange} has no unit of measurement. For a range of physical measurements
 * with unit of measure, see {@link MeasurementRange}.
 *
 * <p>{@code NumberRange} has some capability to convert different number types before to
 * perform operations. In order to provide both this flexibility and the safety of generic
 * types, most operations in this class are defined in two versions:</p>
 * <ul>
 *   <li>Methods inherited from the {@code Range} parent class
 *      ({@link #contains(Range) contains}, {@link #intersect(Range) intersect},
 *       {@link #intersects(Range) intersects}, {@link #union(Range) union} and
 *       {@link #subtract(Range) subtract}) requires argument or range elements
 *       of type {@code <E>}. No type conversion is performed.</li>
 *
 *   <li>Methods defined in this class with the {@code Any} suffix
 *      ({@link #containsAny(NumberRange) containsAny}, {@link #intersectAny(NumberRange) intersectAny},
 *       {@link #intersectsAny(NumberRange) intersectsAny}, {@link #unionAny(NumberRange) unionAny} and
 *       {@link #subtractAny(NumberRange) subtractAny}) are more lenient on the argument or range element
 *       type {@code <E>}. Widening conversions are performed as needed.</li>
 * </ul>
 *
 * The methods from the parent class are preferable when the ranges are known to contain elements
 * of the same type, since they avoid the cost of type checks and conversions. The method in this
 * class are convenient when the parameterized type is unknown ({@code <?>}).
 *
 * <p>Other methods defined in this class:</p>
 * <ul>
 *   <li>Convenience {@code create(…)} static methods for every numeric primitive types.</li>
 *   <li>{@link #castTo(Class)} for casting the range values to an other type.</li>
 * </ul>
 *
 * <div class="section">Relationship with standards</div>
 * {@code NumberRange} is the SIS class closest to the
 * <a href="http://en.wikipedia.org/wiki/Interval_%28mathematics%29">mathematical definition of interval</a>.
 * It is closely related, while not identical, to the ISO 19123 (<cite>Coverage geometry and functions</cite>)
 * definition of "ranges". At the difference of the parent {@link Range} class, which can be used only with
 * {@linkplain org.opengis.coverage.DiscreteCoverage discrete coverages}, the {@code NumberRange} class can
 * also be used with {@linkplain org.opengis.coverage.ContinuousCoverage continuous coverages}.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class and the {@link MeasurementRange} subclasses are immutable, and thus inherently thread-safe.
 * Other subclasses may or may not be immutable, at implementation choice. But implementors are encouraged
 * to make sure that all subclasses remain immutable for more predictable behavior.
 *
 * <div class="section">Shared instances</div>
 * <i><b>Note:</b> following is implementation details provided for information purpose.
 * The caching policy may change in any SIS version.</i>
 *
 * <p>All {@code create} static methods may return a shared instance. Those methods are preferred
 * to the constructors when the range is expected to have a long lifetime, typically as instance
 * given to {@linkplain org.apache.sis.parameter.DefaultParameterDescriptor parameter descriptor}.
 * Other methods do not check for shared instances, since the created object is often temporary.</p>
 *
 * @param <E> The type of range elements as a subclass of {@link Number}.
 *
 * @author  Martin Desruisseaux (IRD)
 * @author  Jody Garnett (for parameterized type inspiration)
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see RangeFormat
 * @see org.apache.sis.util.collection.RangeSet
 * @see <a href="http://en.wikipedia.org/wiki/Interval_%28mathematics%29">Wikipedia: Interval</a>
 */
public class NumberRange<E extends Number & Comparable<? super E>> extends Range<E> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3198281191274903617L;

    /**
     * The pool of ranges created by the {@code create(…)} methods.
     */
    @SuppressWarnings("unchecked")
    private static final WeakHashSet<NumberRange<?>> POOL = new WeakHashSet<NumberRange<?>>((Class) NumberRange.class);

    /**
     * Returns a unique instance of the given range, except if the range is empty.
     *
     * <div class="note"><b>Rational:</b>
     * we exclude empty ranges because the {@link Range#equals(Object)} consider them as equal.
     * Consequently if empty ranges were included in the pool, this method would return in some
     * occasions an empty range with different values than the given {@code range} argument.
     * </div>
     */
    static <E extends Number & Comparable<? super E>, T extends NumberRange<E>> T unique(T range) {
        if (!range.isEmpty()) {
            range = POOL.unique(range);
        }
        return range;
    }

    /**
     * Constructs a range of {@code byte} values.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       The minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given endpoints.
     */
    public static NumberRange<Byte> create(final byte minValue, final boolean isMinIncluded,
                                           final byte maxValue, final boolean isMaxIncluded)
    {
        return unique(new NumberRange<Byte>(Byte.class,
                Byte.valueOf(minValue), isMinIncluded,
                Byte.valueOf(maxValue), isMaxIncluded));
    }

    /**
     * Constructs a range of {@code short} values.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       The minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given endpoints.
     */
    public static NumberRange<Short> create(final short minValue, final boolean isMinIncluded,
                                            final short maxValue, final boolean isMaxIncluded)
    {
        return unique(new NumberRange<Short>(Short.class,
                Short.valueOf(minValue), isMinIncluded,
                Short.valueOf(maxValue), isMaxIncluded));
    }

    /**
     * Constructs a range of {@code int} values.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       The minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given endpoints.
     *
     * @see #createLeftBounded(int, boolean)
     */
    public static NumberRange<Integer> create(final int minValue, final boolean isMinIncluded,
                                              final int maxValue, final boolean isMaxIncluded)
    {
        return unique(new NumberRange<Integer>(Integer.class,
                Integer.valueOf(minValue), isMinIncluded,
                Integer.valueOf(maxValue), isMaxIncluded));
    }

    /**
     * Constructs a range of {@code long} values.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       The minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given endpoints.
     */
    public static NumberRange<Long> create(final long minValue, final boolean isMinIncluded,
                                           final long maxValue, final boolean isMaxIncluded)
    {
        return unique(new NumberRange<Long>(Long.class,
                Long.valueOf(minValue), isMinIncluded,
                Long.valueOf(maxValue), isMaxIncluded));
    }

    /**
     * Constructs a range of {@code float} values.
     * The values can not be {@link Float#NaN}.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       The minimal value, or {@link Float#NEGATIVE_INFINITY} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value, or {@link Float#POSITIVE_INFINITY} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given endpoints.
     */
    public static NumberRange<Float> create(final float minValue, final boolean isMinIncluded,
                                            final float maxValue, final boolean isMaxIncluded)
    {
        return unique(new NumberRange<Float>(Float.class,
                valueOf("minValue", minValue, Float.NEGATIVE_INFINITY), isMinIncluded,
                valueOf("maxValue", maxValue, Float.POSITIVE_INFINITY), isMaxIncluded));
    }

    /**
     * Returns the {@code Float} wrapper of the given primitive {@code float},
     * or {@code null} if it equals to the infinity value.
     */
    static Float valueOf(final String name, final float value, final float infinity) {
        if (Float.isNaN(value)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotANumber_1, name));
        }
        return (value != infinity) ? value : null;
    }

    /**
     * Constructs a range of {@code double} values.
     * The values can not be {@link Double#NaN}.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       The minimal value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values for the given endpoints.
     */
    public static NumberRange<Double> create(final double minValue, final boolean isMinIncluded,
                                             final double maxValue, final boolean isMaxIncluded)
    {
        return unique(new NumberRange<Double>(Double.class,
                valueOf("minValue", minValue, Double.NEGATIVE_INFINITY), isMinIncluded,
                valueOf("maxValue", maxValue, Double.POSITIVE_INFINITY), isMaxIncluded));
    }

    /**
     * Returns the {@code Double} wrapper of the given primitive {@code double},
     * or {@code null} if it equals to the infinity value.
     */
    static Double valueOf(final String name, final double value, final double infinity) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotANumber_1, name));
        }
        return (value != infinity) ? Numerics.valueOf(value) : null;
    }

    /**
     * Constructs a range of {@code int} values without upper bound.
     * This method may return a shared instance, at implementation choice.
     *
     * <div class="note"><b>Note:</b> for creating left-bounded ranges of floating point values,
     * use one of the {@code create(…)} methods with a {@code POSITIVE_INFINITY} constant.
     * We do not provide variants for other integer types because this method is typically invoked for
     * defining the {@linkplain org.apache.sis.feature.DefaultFeatureType cardinality of an attribute}.</div>
     *
     * @param  minValue       The minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @return The new range of numeric values from {@code minValue} to positive infinity.
     *
     * @see #create(int, boolean, int, boolean)
     *
     * @since 0.5
     */
    public static NumberRange<Integer> createLeftBounded(final int minValue, final boolean isMinIncluded) {
        return unique(new NumberRange<Integer>(Integer.class, Integer.valueOf(minValue), isMinIncluded, null, false));
    }

    /**
     * Constructs a range using the smallest type of {@link Number} that can hold the
     * given values. The given numbers don't need to be of the same type since they will
     * be {@linkplain Numbers#cast(Number, Class) casted} as needed. More specifically
     * this method returns:
     *
     * <ul>
     *   <li>{@code NumberRange<Byte>} if the given values are integers between
     *       {@value java.lang.Byte#MIN_VALUE} and {@value java.lang.Byte#MAX_VALUE} inclusive.</li>
     *   <li>{@code NumberRange<Short>} if the given values are integers between
     *       {@value java.lang.Short#MIN_VALUE} and {@value java.lang.Short#MAX_VALUE} inclusive.</li>
     *   <li>{@code NumberRange<Integer>} if the given values are integers between
     *       {@value java.lang.Integer#MIN_VALUE} and {@value java.lang.Integer#MAX_VALUE} inclusive.</li>
     *   <li>{@code NumberRange<Long>} if the given values are integers in the range of {@code long} values.</li>
     *   <li>{@code NumberRange<Float>} if the given values can be casted to {@code float} values without data lost.</li>
     *   <li>{@code NumberRange<Double>} If none of the above types is suitable.</li>
     * </ul>
     *
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       The minimal value, or {@code null} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value, or {@code null} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return The new range, or {@code null} if both {@code minValue} and {@code maxValue} are {@code null}.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    public static NumberRange<?> createBestFit(final Number minValue, final boolean isMinIncluded,
                                               final Number maxValue, final boolean isMaxIncluded)
    {
        final Class<? extends Number> type = Numbers.widestClass(
                Numbers.narrowestClass(minValue), Numbers.narrowestClass(maxValue));
        return (type == null) ? null : unique(new NumberRange(type,
                Numbers.cast(minValue, type), isMinIncluded,
                Numbers.cast(maxValue, type), isMaxIncluded));
    }

    /**
     * Returns the specified {@link Range} as a {@code NumberRange} object. If the specified
     * range is already an instance of {@code NumberRange}, then it is returned unchanged.
     * Otherwise a new number range is created using the {@linkplain #NumberRange(Range)
     * copy constructor}.
     *
     * @param  <N> The type of elements in the given range.
     * @param  range The range to cast or copy.
     * @return The same range than {@code range} as a {@code NumberRange} object.
     */
    public static <N extends Number & Comparable<? super N>> NumberRange<N> castOrCopy(final Range<N> range) {
        if (range instanceof NumberRange<?>) {
            return (NumberRange<N>) range;
        }
        // The constructor will ensure that the range element type is a subclass of Number.
        // Do not invoke unique(NumberRange) because the returned range is often temporary.
        return new NumberRange<N>(range);
    }

    /**
     * Constructs a range with the same type and the same values than the specified range.
     * This is a copy constructor.
     *
     * @param range The range to copy. The elements must be {@link Number} instances.
     */
    public NumberRange(final Range<E> range) {
        super(range);
    }

    /**
     * Constructs a range of the given type with values from the given annotation.
     * This constructor does not verify if the given type is wide enough for the values of
     * the given annotation, because those information are usually static. If nevertheless
     * the given type is not wide enough, then the values are truncated in the same way
     * than the Java language casts primitive types.
     *
     * @param  type  The element type, restricted to one of {@link Byte}, {@link Short},
     *               {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  range The range of values.
     * @throws IllegalArgumentException If the given type is not one of the primitive
     *         wrappers for numeric types.
     */
    public NumberRange(final Class<E> type, final ValueRange range) throws IllegalArgumentException {
        super(type, Numbers.cast(valueOf("minimum", range.minimum(), Double.NEGATIVE_INFINITY), type), range.isMinIncluded(),
                    Numbers.cast(valueOf("maximum", range.maximum(), Double.POSITIVE_INFINITY), type), range.isMaxIncluded());
    }

    /**
     * Constructs a range of {@link Number} objects.
     *
     * @param type           The element type, usually one of {@link Byte}, {@link Short},
     *                       {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param minValue       The minimal value, or {@code null} if none.
     * @param isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param maxValue       The maximal value, or {@code null} if none.
     * @param isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     */
    public NumberRange(final Class<E> type,
                       final E minValue, final boolean isMinIncluded,
                       final E maxValue, final boolean isMaxIncluded)
    {
        super(type, minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Constructs a range with the same values than the specified range,
     * casted to the specified type.
     *
     * @param  type  The element type, usually one of {@link Byte}, {@link Short},
     *               {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  range The range to copy. The elements must be {@link Number} instances.
     * @throws IllegalArgumentException If the given type is not one of the primitive
     *         wrappers for numeric types.
     */
    NumberRange(final Class<E> type, final Range<? extends Number> range)
            throws IllegalArgumentException
    {
        super(type, Numbers.cast(range.minValue, type), range.isMinIncluded,
                    Numbers.cast(range.maxValue, type), range.isMaxIncluded);
    }

    /**
     * Creates a new range using the same element type than this range. This method will
     * be overridden by subclasses in order to create a range of a more specific type.
     */
    @Override
    Range<E> create(final E minValue, final boolean isMinIncluded,
                    final E maxValue, final boolean isMaxIncluded)
    {
        return new NumberRange<E>(elementType, minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Casts the specified range to the specified type.  If this class is associated to a unit of
     * measurement, then this method converts the {@code range} unit to the same unit than this
     * instance. This method is overridden by {@link MeasurementRange} only in the way described
     * above.
     *
     * @param  type The class to cast to. Must be one of {@link Byte}, {@link Short},
     *              {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @return The casted range, or {@code range} if no cast is needed.
     * @throws IllegalArgumentException If the given type is not one of the primitive
     *         wrappers for numeric types.
     */
    @SuppressWarnings("unchecked")
    <N extends Number & Comparable<? super N>>
    NumberRange<N> convertAndCast(final NumberRange<?> range, final Class<N> type)
            throws IllegalArgumentException
    {
        if (range.elementType == type) {
            return (NumberRange<N>) range;
        }
        return new NumberRange<N>(type, range);
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
    @SuppressWarnings("unchecked")
    public <N extends Number & Comparable<? super N>> NumberRange<N> castTo(final Class<N> type)
            throws IllegalArgumentException
    {
        if (elementType == type) {
            return (NumberRange<N>) this;
        }
        return new NumberRange<N>(type, this);
    }

    /**
     * Returns an initially empty array of the given length.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"}) // Generic array creation.
    Range<E>[] newArray(final int length) {
        return new NumberRange[length];
    }

    /**
     * Returns the {@linkplain #getMinValue() minimum value} as a {@code double}.
     * If this range is unbounded, then {@link Double#NEGATIVE_INFINITY} is returned.
     *
     * @return The minimum value.
     */
    @SuppressWarnings("unchecked")
    public double getMinDouble() {
        final Number value = (Number) getMinValue();
        return (value != null) ? value.doubleValue() : Double.NEGATIVE_INFINITY;
    }

    /**
     * Returns the {@linkplain #getMinDouble() minimum value} with the specified inclusive or
     * exclusive state. If this range is unbounded, then {@link Double#NEGATIVE_INFINITY} is
     * returned.
     *
     * @param  inclusive {@code true} for the minimum value inclusive,
     *         or {@code false} for the minimum value exclusive.
     * @return The minimum value, inclusive or exclusive as requested.
     */
    public double getMinDouble(final boolean inclusive) {
        double value = getMinDouble();
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
    public double getMaxDouble() {
        final Number value = (Number) getMaxValue();
        return (value != null) ? value.doubleValue() : Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the {@linkplain #getMaxDouble() maximum value} with the specified inclusive or
     * exclusive state. If this range is unbounded, then {@link Double#POSITIVE_INFINITY} is
     * returned.
     *
     * @param  inclusive {@code true} for the maximum value inclusive,
     *         or {@code false} for the maximum value exclusive.
     * @return The maximum value, inclusive or exclusive as requested.
     */
    public double getMaxDouble(final boolean inclusive) {
        double value = getMaxDouble();
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

    /**
     * Returns {@code true} if this range contains the given value.
     * This method converts {@code this} or the given argument to the widest numeric type,
     * then performs the same work than {@link #contains(Comparable)}.
     *
     * @param  value The value to check for inclusion in this range.
     * @return {@code true} if the given value is included in this range.
     * @throws IllegalArgumentException if the given range can not be converted to a valid type
     *         through widening conversion.
     */
    public boolean containsAny(Number value) throws IllegalArgumentException {
        if (value == null) {
            return false;
        }
        final Class<? extends Number> type = Numbers.widestClass(elementType, value.getClass());
        value = Numbers.cast(value, type);
        if (minValue != null) {
            @SuppressWarnings("unchecked")
            final int c = ((Comparable) Numbers.cast(minValue, type)).compareTo(value);
            if (isMinIncluded ? (c > 0) : (c >= 0)) {
                return false;
            }
        }
        if (maxValue != null) {
            @SuppressWarnings("unchecked")
            final int c = ((Comparable) Numbers.cast(maxValue, type)).compareTo(value);
            if (isMaxIncluded ? (c < 0) : (c <= 0)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the supplied range is fully contained within this range.
     * This method converts {@code this} or the given argument to the widest numeric type,
     * then delegates to {@link #contains(Range)}.
     *
     * @param  range The range to check for inclusion in this range.
     * @return {@code true} if the given range is included in this range.
     * @throws IllegalArgumentException if the given range can not be converted to a valid type
     *         through widening conversion, or if the units of measurement are not convertible.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public boolean containsAny(final NumberRange<?> range) throws IllegalArgumentException {
        /*
         * The type bounds is actually <? extends Number & Comparable> but I'm unable to express
         * it as local variable as of Java 7. So we have to bypass the compiler check, but those
         * casts are actually safes.
         */
        final Class type = Numbers.widestClass(elementType, range.elementType);
        return castTo(type).contains(convertAndCast(range, type));
    }

    /**
     * Returns {@code true} if the supplied range is fully contained within this range.
     * This method converts {@code this} or the given argument to the widest numeric type,
     * then delegates to {@link #intersects(Range)}.
     *
     * @param  range The range to check for inclusion in this range.
     * @return {@code true} if the given range is included in this range.
     * @throws IllegalArgumentException if the given range can not be converted to a valid type
     *         through widening conversion, or if the units of measurement are not convertible.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public boolean intersectsAny(final NumberRange<?> range) throws IllegalArgumentException {
        final Class type = Numbers.widestClass(elementType, range.elementType);
        return castTo(type).intersects(convertAndCast(range, type));
    }

    /**
     * Returns the union of this range with the given range.
     * This method converts {@code this} or the given argument to the widest numeric type,
     * then delegates to {@link #intersect(Range)}.
     *
     * @param  range The range to add to this range.
     * @return The union of this range with the given range.
     * @throws IllegalArgumentException if the given range can not be converted to a valid type
     *         through widening conversion, or if the units of measurement are not convertible.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public NumberRange<?> intersectAny(final NumberRange<?> range) throws IllegalArgumentException {
        Class type = Numbers.widestClass(elementType, range.elementType);
        final NumberRange<?> intersect = castOrCopy(castTo(type).intersect(convertAndCast(range, type)));
        /*
         * Use a finer type capable to holds the result (since the intersection
         * may have reduced the range), but not finer than the finest type of
         * the ranges used in the intersection calculation.
         */
        type = Numbers.narrowestClass(elementType, range.elementType);
        type = Numbers.widestClass(type, Numbers.narrowestClass((Number) intersect.minValue));
        type = Numbers.widestClass(type, Numbers.narrowestClass((Number) intersect.maxValue));
        return intersect.castTo(type);
    }

    /**
     * Returns the union of this range with the given range.
     * This method converts {@code this} or the given argument to the widest numeric type,
     * then delegates to {@link #union(Range)}.
     *
     * @param  range The range to add to this range.
     * @return The union of this range with the given range.
     * @throws IllegalArgumentException if the given range can not be converted to a valid type
     *         through widening conversion, or if the units of measurement are not convertible.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public NumberRange<?> unionAny(final NumberRange<?> range) throws IllegalArgumentException {
        final Class type = Numbers.widestClass(elementType, range.elementType);
        return castOrCopy(castTo(type).union(convertAndCast(range, type)));
    }

    /**
     * Returns the range of values that are in this range but not in the given range.
     * This method converts {@code this} or the given argument to the widest numeric type,
     * then delegates to {@link #subtract(Range)}.
     *
     * @param  range The range to subtract.
     * @return This range without the given range, as an array of length 0, 1 or 2.
     * @throws IllegalArgumentException if the given range can not be converted to a valid type
     *         through widening conversion, or if the units of measurement are not convertible.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public NumberRange<?>[] subtractAny(final NumberRange<?> range) throws IllegalArgumentException {
        final Class type = Numbers.widestClass(elementType, range.elementType);
        return (NumberRange[]) castTo(type).subtract(convertAndCast(range, type));
    }
}
