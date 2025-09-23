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

import java.util.Objects;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.math.MathFunctions;
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
 *   <li>{@link #castTo(Class)} for casting the range values to another type.</li>
 *   <li>{@link #transform(MathTransform1D)} for applying an arbitrary conversion.</li>
 * </ul>
 *
 * <h2>Relationship with standards</h2>
 * {@code NumberRange} is the SIS class closest to the
 * <a href="https://en.wikipedia.org/wiki/Interval_%28mathematics%29">mathematical definition of interval</a>.
 * It is closely related, while not identical, to the ISO 19123 (<cite>Coverage geometry and functions</cite>)
 * definition of "ranges". At the difference of the parent {@link Range} class, which can be used only with
 * discrete coverages, the {@code NumberRange} class can
 * also be used with continuous coverages.
 *
 * <h2>Immutability and thread safety</h2>
 * This class and the {@link MeasurementRange} subclasses are immutable, and thus inherently thread-safe.
 * Other subclasses may or may not be immutable, at implementation choice. But implementers are encouraged
 * to make sure that all subclasses remain immutable for more predictable behavior.
 *
 * <h2>Shared instances</h2>
 * <i><b>Note:</b> following is implementation details provided for information purpose.
 * The caching policy may change in any SIS version.</i>
 *
 * <p>All {@code create} static methods may return a shared instance. Those methods are preferred
 * to the constructors when the range is expected to have a long lifetime, typically as instance
 * given to {@linkplain org.apache.sis.parameter.DefaultParameterDescriptor parameter descriptor}.
 * Other methods do not check for shared instances, since the created object is often temporary.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Jody Garnett (for parameterized type inspiration)
 * @version 1.4
 *
 * @param <E>  the type of range elements as a subclass of {@link Number}.
 *
 * @see RangeFormat
 * @see org.apache.sis.util.collection.RangeSet
 * @see <a href="https://en.wikipedia.org/wiki/Interval_%28mathematics%29">Wikipedia: Interval</a>
 *
 * @since 0.3
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
    private static final WeakHashSet<NumberRange<?>> POOL = new WeakHashSet<>((Class) NumberRange.class);

    /**
     * Returns a unique instance of the given range, except if the range is empty.
     *
     * <h4>Rational</h4>
     * We exclude empty ranges because the {@link Range#equals(Object)} consider them as equal.
     * Consequently, if empty ranges were included in the pool, this method would return in some
     * occasions an empty range with different values than the given {@code range} argument.
     *
     * We use this method only for caching range of wrapper of primitive types ({@link Byte},
     * {@link Short}, <i>etc.</i>) because those types are known to be immutable.
     */
    static <E extends Number & Comparable<? super E>, T extends NumberRange<E>> T unique(T range) {
        if (!range.isEmpty()) {
            range = POOL.unique(range);
        }
        return range;
    }

    /**
     * Returns {@code true} if the given value is valid for a range to be cached by {@link #union(Range)}.
     * A range can be cached if the {@link Number} values are null or instances of a standard Java class
     * known to be immutable, and the wrapped values are not NaN except the canonical {@link Double#NaN}
     * or {@link Float#NaN} values. This check is necessary because {@link #equals(Object)} considers all
     * {@code NaN} values as equal.
     */
    static boolean isCacheable(final Number n) {
        if (n == null) {
            return true;
        } else if (n instanceof Double) {
            final double value = (Double) n;
            return !Double.isNaN(value) || Double.doubleToRawLongBits(value) == 0x7FF8000000000000L;
        } else if (n instanceof Float) {
            final float value = (Float) n;
            return !Float.isNaN(value) || Float.floatToRawIntBits(value) == 0x7FC00000;
        } else {
            return Numbers.getEnumConstant(n.getClass()) != Numbers.OTHER;
        }
    }

    /**
     * Constructs a range containing a single value of the given type.
     * The given value is used as the minimum and maximum values, inclusive.
     *
     * @param  <N>     compile-time value of {@code type}.
     * @param  type    the element type, usually one of {@link Byte}, {@link Short},
     *                 {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  value   the value, or {@code null} for creating an unbounded range.
     * @return a range containing the given value as its inclusive minimum and maximum.
     *
     * @since 1.0
     */
    public static <N extends Number & Comparable<? super N>> NumberRange<N> create(final Class<N> type, final N value) {
        NumberRange<N> range = new NumberRange<>(type, value, true, value, true);
        if (isCacheable(value)) {
            range = unique(range);
        }
        return range;
    }

    /**
     * Constructs a range of {@code byte} values.
     * If the minimum is greater than the maximum, then the range {@linkplain #isEmpty() is empty}.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       the minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       the maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return the new range of numeric values for the given endpoints.
     */
    public static NumberRange<Byte> create(final byte minValue, final boolean isMinIncluded,
                                           final byte maxValue, final boolean isMaxIncluded)
    {
        // No need to check for equality because all bytes values are cached by Byte.valueOf(…).
        return unique(new NumberRange<>(Byte.class,
                Byte.valueOf(minValue), isMinIncluded,
                Byte.valueOf(maxValue), isMaxIncluded));
    }

    /**
     * Constructs a range of {@code short} values.
     * If the minimum is greater than the maximum, then the range {@linkplain #isEmpty() is empty}.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       the minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       the maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return the new range of numeric values for the given endpoints.
     */
    public static NumberRange<Short> create(final short minValue, final boolean isMinIncluded,
                                            final short maxValue, final boolean isMaxIncluded)
    {
        final Short min = minValue;
        final Short max = (minValue == maxValue) ? min : maxValue;
        return unique(new NumberRange<>(Short.class, min, isMinIncluded, max, isMaxIncluded));
    }

    /**
     * Constructs a range of {@code int} values.
     * If the minimum is greater than the maximum, then the range {@linkplain #isEmpty() is empty}.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       the minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       the maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return the new range of numeric values for the given endpoints.
     *
     * @see #createLeftBounded(int, boolean)
     */
    public static NumberRange<Integer> create(final int minValue, final boolean isMinIncluded,
                                              final int maxValue, final boolean isMaxIncluded)
    {
        final Integer min = minValue;
        final Integer max = (minValue == maxValue) ? min : maxValue;
        return unique(new NumberRange<>(Integer.class, min, isMinIncluded, max, isMaxIncluded));
    }

    /**
     * Constructs a range of {@code long} values.
     * If the minimum is greater than the maximum, then the range {@linkplain #isEmpty() is empty}.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       the minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       the maximal value.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return the new range of numeric values for the given endpoints.
     */
    public static NumberRange<Long> create(final long minValue, final boolean isMinIncluded,
                                           final long maxValue, final boolean isMaxIncluded)
    {
        final Long min = minValue;
        final Long max = (minValue == maxValue) ? min : maxValue;
        return unique(new NumberRange<>(Long.class, min, isMinIncluded, max, isMaxIncluded));
    }

    /**
     * Constructs a range of {@code float} values.
     * The minimum and maximum values cannot be NaN but can be infinite.
     * If the minimum is greater than the maximum, then the range {@linkplain #isEmpty() is empty}.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       the minimal value, or {@link Float#NEGATIVE_INFINITY} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       the maximal value, or {@link Float#POSITIVE_INFINITY} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return the new range of numeric values for the given endpoints.
     * @throws IllegalArgumentException if {@link Float#isNaN(float)} is {@code true} for a given value.
     */
    public static NumberRange<Float> create(final float minValue, final boolean isMinIncluded,
                                            final float maxValue, final boolean isMaxIncluded)
    {
        final Float min = valueOf("minValue", minValue, Float.NEGATIVE_INFINITY);
        final Float max = valueOf("maxValue", maxValue, Float.POSITIVE_INFINITY);
        // No need to test isCacheable(Number) because the type is known and valueOf(…) disallows NaN values.
        return unique(new NumberRange<>(Float.class, min, isMinIncluded, Objects.equals(min, max) ? min : max, isMaxIncluded));
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
     * The minimum and maximum values cannot be NaN but can be infinite.
     * If the minimum is greater than the maximum, then the range {@linkplain #isEmpty() is empty}.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       the minimal value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       the maximal value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return the new range of numeric values for the given endpoints.
     * @throws IllegalArgumentException if {@link Double#isNaN(double)} is {@code true} for a given value.
     */
    public static NumberRange<Double> create(final double minValue, final boolean isMinIncluded,
                                             final double maxValue, final boolean isMaxIncluded)
    {
        final Double min = valueOf("minValue", minValue, Double.NEGATIVE_INFINITY);
        final Double max = valueOf("maxValue", maxValue, Double.POSITIVE_INFINITY);
        // No need to test isCacheable(Number) because the type is known and valueOf(…) disallows NaN values.
        return unique(new NumberRange<>(Double.class, min, isMinIncluded, Objects.equals(min, max) ? min : max, isMaxIncluded));
    }

    /**
     * Returns the {@code Double} wrapper of the given primitive {@code double},
     * or {@code null} if it equals to the infinity value.
     */
    static Double valueOf(final String name, final double value, final double infinity) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotANumber_1, name));
        }
        return (value != infinity) ? value : null;
    }

    /**
     * Constructs a range using the smallest type of {@link Number} that can hold the given values.
     * The given numbers do not need to be of the same type since they will
     * be {@linkplain Numbers#cast(Number, Class) cast} as needed.
     * More specifically this method returns:
     *
     * <ul>
     *   <li>{@code NumberRange<Byte>} if the given values are integers between
     *       {@value java.lang.Byte#MIN_VALUE} and {@value java.lang.Byte#MAX_VALUE} inclusive.</li>
     *   <li>{@code NumberRange<Short>} if the given values are integers between
     *       {@value java.lang.Short#MIN_VALUE} and {@value java.lang.Short#MAX_VALUE} inclusive.</li>
     *   <li>{@code NumberRange<Integer>} if the given values are integers between
     *       {@value java.lang.Integer#MIN_VALUE} and {@value java.lang.Integer#MAX_VALUE} inclusive.</li>
     *   <li>{@code NumberRange<Long>} if the given values are integers between
     *       {@value java.lang.Long#MIN_VALUE} and {@value java.lang.Long#MAX_VALUE} inclusive.</li>
     *   <li>{@code NumberRange<Float>} if the given values can be cast to {@code float} values without data lost.</li>
     *   <li>{@code NumberRange<Double>} if none of the above types is suitable.</li>
     * </ul>
     *
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue       the minimal value, or {@code null} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       the maximal value, or {@code null} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return the new range, or {@code null} if both {@code minValue} and {@code maxValue} are {@code null}.
     * @throws Illegal­Argument­Exception if the given numbers are not primitive wrappers for numeric types.
     */
    public static NumberRange<?> createBestFit(final Number minValue, final boolean isMinIncluded,
                                               final Number maxValue, final boolean isMaxIncluded)
    {
        return createBestFit(false, minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Constructs a range using the smallest integer type or floating point type that can hold the given values.
     * If {@code asFloat} is {@code false}, then the returned range can use any wrapper type and this method behaves
     * as described in {@linkplain #createBestFit(java.lang.Number, boolean, java.lang.Number, boolean) above method}.
     * If {@code asFloat} is {@code true}, then the returned range is restricted to {@link Float} and {@link Double}
     * number types; integer types are cast to one of the floating point types.
     *
     * @param  asFloat        whether to restrict the returned range to floating point types.
     * @param  minValue       the minimal value, or {@code null} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       the maximal value, or {@code null} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @return the new range, or {@code null} if both {@code minValue} and {@code maxValue} are {@code null}.
     * @throws Illegal­Argument­Exception if the given numbers are not primitive wrappers for numeric types.
     *
     * @since 1.2
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    public static NumberRange<?> createBestFit(final boolean asFloat,
            final Number minValue, final boolean isMinIncluded,
            final Number maxValue, final boolean isMaxIncluded)
    {
        final Class<? extends Number> type;
        if (asFloat) {
            if (minValue == null && maxValue == null) {
                return null;
            }
            // Types supported below should be consistent with the comment in next block.
            type = (isFloat(minValue) && isFloat(maxValue)) ? Float.class : Double.class;
        } else {
            /*
             * The `narrowestClass(…)` method currently returns only wrappers of primitive types.
             * The `Fraction`, `BigInteger` or `BigDecimal` types accepted by `widestClass(…)` are lost.
             * We could support those additional classes as well (by improving `narrowestClass(…)` and
             * updating above Javadoc), but we do not yet have a need for them.
             */
            type = Numbers.widestClass(Numbers.narrowestClass(minValue),
                                       Numbers.narrowestClass(maxValue));
            if (type == null) {
                return null;
            }
        }
        Number min = Numbers.cast(minValue, type);
        Number max = Numbers.cast(maxValue, type);
        final boolean isCacheable = isCacheable(min) && isCacheable(max);
        if (isCacheable && Objects.equals(min, max)) {
            max = min;      // Share the same instance.
        }
        NumberRange range = new NumberRange(type, min, isMinIncluded, max, isMaxIncluded);
        if (isCacheable) {
            range = unique(range);
        }
        return range;
    }

    /**
     * Returns {@code true} if the given value can be cast to the {@code Float} type.
     */
    private static boolean isFloat(final Number value) {
        return (value == null) ||
                Double.doubleToRawLongBits(value.floatValue()) == Double.doubleToRawLongBits(value.doubleValue());
    }

    /**
     * Constructs a range of {@code int} values without upper bound.
     * This method may return a shared instance, at implementation choice.
     *
     * <h4>API note</h4>
     * For creating left-bounded ranges of floating point values,
     * use one of the {@code create(…)} methods with a {@code POSITIVE_INFINITY} constant.
     * We do not provide variants for other integer types because this method is typically invoked for
     * defining the {@linkplain org.apache.sis.feature.DefaultFeatureType multiplicity of an attribute}.
     *
     * @param  minValue       the minimal value.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @return the new range of numeric values from {@code minValue} to positive infinity.
     *
     * @see #create(int, boolean, int, boolean)
     *
     * @since 0.5
     */
    public static NumberRange<Integer> createLeftBounded(final int minValue, final boolean isMinIncluded) {
        // Use POOL.unique(…) directly because we do not need the check for Range.isEmpty() here.
        return POOL.unique(new NumberRange<>(Integer.class, Integer.valueOf(minValue), isMinIncluded, null, false));
    }

    /**
     * Returns the specified {@link Range} as a {@code NumberRange} object.
     * If the specified range is already an instance of {@code NumberRange}, then it is returned unchanged.
     * Otherwise a new number range is created using the {@linkplain #NumberRange(Range) copy constructor}.
     *
     * @param  <N>    the type of elements in the given range.
     * @param  range  the range to cast or copy.
     * @return the same range as {@code range} as a {@code NumberRange} object.
     */
    public static <N extends Number & Comparable<? super N>> NumberRange<N> castOrCopy(final Range<N> range) {
        if (range instanceof NumberRange<?>) {
            return (NumberRange<N>) range;
        }
        // The constructor will ensure that the range element type is a subclass of Number.
        // Do not invoke unique(NumberRange) because the returned range is often temporary.
        return new NumberRange<>(range);
    }

    /**
     * Constructs a range with the same type and the same values as the specified range.
     * This is a copy constructor.
     *
     * @param range the range to copy. The elements must be {@link Number} instances.
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
     * @param  type   the element type, restricted to one of {@link Byte}, {@link Short},
     *                {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  range  the range of values.
     * @throws IllegalArgumentException if the given type is not one of the primitive wrappers for numeric types.
     */
    public NumberRange(final Class<E> type, final ValueRange range) throws IllegalArgumentException {
        super(type, Numbers.cast(valueOf("minimum", range.minimum(), Double.NEGATIVE_INFINITY), type), range.isMinIncluded(),
                    Numbers.cast(valueOf("maximum", range.maximum(), Double.POSITIVE_INFINITY), type), range.isMaxIncluded());
    }

    /**
     * Constructs a range of {@link Number} objects.
     *
     * @param  type           the element type, usually one of {@link Byte}, {@link Short},
     *                        {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  minValue       the minimal value, or {@code null} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       the maximal value, or {@code null} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     */
    public NumberRange(final Class<E> type,
                       final E minValue, final boolean isMinIncluded,
                       final E maxValue, final boolean isMaxIncluded)
    {
        super(type, minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Constructs a range with the same values as the specified range, cast to the specified type.
     *
     * @param  type   the element type, usually one of {@link Byte}, {@link Short},
     *                {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  range  the range to copy. The elements must be {@link Number} instances.
     * @throws IllegalArgumentException if the given type is not one of the primitive wrappers for numeric types.
     */
    NumberRange(final Class<E> type, final Range<? extends Number> range)
            throws IllegalArgumentException
    {
        super(type, Numbers.cast(range.minValue, type), range.isMinIncluded,
                    Numbers.cast(range.maxValue, type), range.isMaxIncluded);
    }

    /**
     * Creates a new range using the same element type as this range. This method will
     * be overridden by subclasses in order to create a range of a more specific type.
     */
    @Override
    Range<E> create(final E minValue, final boolean isMinIncluded,
                    final E maxValue, final boolean isMaxIncluded)
    {
        return new NumberRange<>(elementType, minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Casts the specified range to the specified type.  If this class is associated to a unit of measurement,
     * then this method converts the {@code range} unit to the same unit as this instance.
     * This method is overridden by {@link MeasurementRange} only in the way described above.
     *
     * @param  type  the class to cast to. Must be one of {@link Byte}, {@link Short},
     *               {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @return the cast range, or {@code range} if no cast is needed.
     * @throws IllegalArgumentException if the given type is not one of the primitive wrappers for numeric types.
     */
    @SuppressWarnings("unchecked")
    <N extends Number & Comparable<? super N>>
    NumberRange<N> convertAndCast(final NumberRange<?> range, final Class<N> type) throws IllegalArgumentException {
        if (range.elementType == type) {
            return (NumberRange<N>) range;
        }
        return new NumberRange<>(type, range);
    }

    /**
     * Casts this range to the specified type. If the cast from this range type to the given
     * type is a narrowing conversion, then the cast is performed according the rules of the
     * Java language: the high-order bytes are silently dropped.
     *
     * @param  <N>   the class to cast to.
     * @param  type  the class to cast to. Must be one of {@link Byte}, {@link Short},
     *               {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @return the cast range, or {@code this} if this range already uses the specified type.
     * @throws IllegalArgumentException if the given type is not one of the primitive wrappers for numeric types.
     */
    @SuppressWarnings("unchecked")
    public <N extends Number & Comparable<? super N>> NumberRange<N> castTo(final Class<N> type) throws IllegalArgumentException {
        if (elementType == type) {
            return (NumberRange<N>) this;
        }
        return new NumberRange<>(type, this);
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
     * @return the minimum value.
     */
    @SuppressWarnings("unchecked")
    public double getMinDouble() {
        final Number value = getMinValue();
        return (value != null) ? value.doubleValue() : Double.NEGATIVE_INFINITY;
    }

    /**
     * Returns the {@linkplain #getMinDouble() minimum value} with the specified inclusive or exclusive state.
     * If this range is unbounded, then {@link Double#NEGATIVE_INFINITY} is returned.
     *
     * @param  inclusive  {@code true} for the minimum value inclusive, or
     *                    {@code false} for the minimum value exclusive.
     * @return the minimum value, inclusive or exclusive as requested.
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
     * @return the maximum value.
     */
    @SuppressWarnings("unchecked")
    public double getMaxDouble() {
        final Number value = getMaxValue();
        return (value != null) ? value.doubleValue() : Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the {@linkplain #getMaxDouble() maximum value} with the specified inclusive or exclusive state.
     * If this range is unbounded, then {@link Double#POSITIVE_INFINITY} is returned.
     *
     * @param  inclusive  {@code true} for the maximum value inclusive, or
     *                    {@code false} for the maximum value exclusive.
     * @return the maximum value, inclusive or exclusive as requested.
     */
    public double getMaxDouble(final boolean inclusive) {
        double value = getMaxDouble();
        if (inclusive != isMaxIncluded()) {
            value = next(getElementType(), value, !inclusive);
        }
        return value;
    }

    /**
     * Computes the average of minimum and maximum values. If numbers are integers, the average is computed using
     * inclusive values (e.g. equivalent to <code>{@linkplain #getMinDouble(boolean) getMinDouble}(true)</code>).
     * Otherwise the minimum and maximum values are used as-is
     * (because making them inclusive is considered an infinitely small change).
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If one bound is infinite, the return value is the same infinity.</li>
     *   <li>If the two bounds are infinite, the return value is {@link Double#NaN}.</li>
     *   <li>If the range {@linkplain #isEmpty() is empty}, the return value is {@link Double#NaN}.</li>
     *   <li>If a bound {@linkplain Double#isNaN(double) is NaN}, the return value is the same NaN
     *       (reminder: {@linkplain MathFunctions#toNanFloat(int) multiple NaN values} are possible).</li>
     * </ul>
     *
     * @return (<var>minimum</var> + <var>maximum</var>) / 2 computed using inclusive values.
     *
     * @since 1.1
     */
    public double getMedian() {
        return medianOrSpan(true);
    }

    /**
     * Computes the difference between minimum and maximum values. If numbers are integers, the difference is computed
     * using inclusive values (e.g. using <code>{@linkplain #getMinDouble(boolean) getMinDouble}(true)</code>).
     * Otherwise the minimum and maximum values are used as-is
     * (because making them inclusive is considered an infinitely small change).
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If the range {@linkplain #isEmpty() is empty}, the return value is 0.</li>
     *   <li>If at least one bound is infinite, the return value is {@link Double#POSITIVE_INFINITY}.</li>
     *   <li>If a bound {@linkplain Double#isNaN(double) is NaN}, the return value is the same NaN
     *       (reminder: {@linkplain MathFunctions#toNanFloat(int) multiple NaN values} are possible).</li>
     * </ul>
     *
     * @return (<var>maximum</var> − <var>minimum</var>) computed using inclusive values.
     *
     * @since 1.1
     */
    public double getSpan() {
        return medianOrSpan(false);
    }

    /**
     * Implementation of {@link #getMedian()} and {@link #getSpan()}.
     */
    private double medianOrSpan(final boolean median) {
        if (minValue == null) {
            if (median) {
                return (maxValue == null) ? Double.NaN : Double.NEGATIVE_INFINITY;
            }
            return Double.POSITIVE_INFINITY;
        } else if (maxValue == null) {
            return Double.POSITIVE_INFINITY;
        }
        if (Numbers.isInteger(getElementType())) {
            long min = minValue.longValue();
            long max = maxValue.longValue();
            if (!isMinIncluded) min++;
            if (!isMaxIncluded) max--;
            if (min <= max) {
                return median ? MathFunctions.average(min, max)
                              : Numerics.toUnsignedDouble(max - min);
            }
        } else {
            final double min = minValue.doubleValue();
            final double max = maxValue.doubleValue();
            if (min <= max) {
                return median ? (min + max) * 0.5 : (max - min);
            } else if (Double.isNaN(min) && (isMinIncluded || !Double.isNaN(max))) {
                return min;     // Same NaN with precedence to inclusive bound if the 2 bounds are NaN.
            } else if (Double.isNaN(max)) {
                return max;
            }
        }
        return median ? Double.NaN : 0;
    }

    /**
     * Returns the next value for the given type.
     *
     * @param  type   the element type.
     * @param  value  the value to increment or decrement.
     * @param  up     {@code true} for incrementing, or {@code false} for decrementing.
     * @return the adjacent value.
     */
    private static double next(final Class<?> type, double value, final boolean up) {
        if (Numbers.isInteger(type)) {
            if (up) value++; else value--;
        } else if (type.equals(Float.class)) {
            final float fv = (float) value;
            value = up ? Math.nextUp(fv) : Math.nextDown(fv);
        } else if (type.equals(Double.class)) {
            value = up ? Math.nextUp(value) : Math.nextDown(value);
        } else {
            // Thrown IllegalStateException instead of IllegalArgumentException because
            // the `type` argument given to this method come from a NumberRange field.
            throw new IllegalStateException(Errors.format(Errors.Keys.NotAPrimitiveWrapper_1, type));
        }
        return value;
    }

    /**
     * Returns {@code true} if this range contains the given value.
     * This method converts {@code this} or the given argument to the widest numeric type,
     * then performs the same work as {@link #contains(Comparable)}.
     *
     * @param  value  the value to check for inclusion in this range, or {@code null}.
     * @return {@code true} if the given value is non-null and included in this range.
     * @throws IllegalArgumentException if the given range cannot be converted to a valid type
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
     * @param  range  the range to check for inclusion in this range.
     * @return {@code true} if the given range is included in this range.
     * @throws IllegalArgumentException if the given range cannot be converted to a valid type
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
     * Returns {@code true} if the supplied range intersects this range.
     * This method converts {@code this} or the given argument to the widest numeric type,
     * then delegates to {@link #intersects(Range)}.
     *
     * @param  range  the range to check for intersection with this range.
     * @return {@code true} if the given range intersects this range.
     * @throws IllegalArgumentException if the given range cannot be converted to a valid type
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
     * @param  range  the range to add to this range.
     * @return the intersection of this range with the given range.
     * @throws IllegalArgumentException if the given range cannot be converted to a valid type
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
     * @param  range  the range to add to this range.
     * @return the union of this range with the given range.
     * @throws IllegalArgumentException if the given range cannot be converted to a valid type
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
     * @param  range  the range to subtract.
     * @return this range without the given range, as an array of length 0, 1 or 2.
     * @throws IllegalArgumentException if the given range cannot be converted to a valid type
     *         through widening conversion, or if the units of measurement are not convertible.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public NumberRange<?>[] subtractAny(final NumberRange<?> range) throws IllegalArgumentException {
        final Class type = Numbers.widestClass(elementType, range.elementType);
        return (NumberRange[]) castTo(type).subtract(convertAndCast(range, type));
    }

    /**
     * Returns this range converted using the given converter.
     *
     * @param  converter  the converter to apply.
     * @return the converted range, or {@code this} if the result is the same as this range.
     * @throws TransformException if an error occurred during the conversion.
     *
     * @since 1.3
     */
    public NumberRange<?> transform(final MathTransform1D converter) throws TransformException {
        final double lower = getMinDouble();
        final double upper = getMaxDouble();
        final double min   = converter.transform(lower);
        final double max   = converter.transform(upper);
        /*
         * Use `doubleToLongBits` instead of `doubleToLongRawBits` for preserving the NaN values
         * used by the original range. Different NaN values may be used for different types of
         * "no data" values we usually want to keep them unchanged by the converter.
         */
        if (Double.doubleToLongBits(min) != Double.doubleToLongBits(lower) ||
            Double.doubleToLongBits(max) != Double.doubleToLongBits(upper))
        {
            if (min > max) {
                return create(max, isMaxIncluded, min, isMinIncluded);
            } else {
                return create(min, isMinIncluded, max, isMaxIncluded);
            }
        }
        return this;
    }
}
