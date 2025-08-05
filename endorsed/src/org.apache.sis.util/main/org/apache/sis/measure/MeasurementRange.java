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
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;


/**
 * A range of numbers associated with a unit of measurement. All operations performed by this
 * class ({@linkplain #union union}, {@linkplain #intersect intersection}, <i>etc.</i>) are
 * performed in the unit of measurement of {@code this} range object - values of the range
 * object given in argument are converted if needed before an operation is applied.
 *
 * <p>Other methods defined in this class:</p>
 * <ul>
 *   <li>Convenience {@code create(…)} static methods for every floating point primitive types.
 *       Usage of {@code MeasurementRange} with integer types is possible, but no convenience
 *       method is provided for integers because they are usually not representative of the
 *       nature of physical measurements.</li>
 *   <li>{@link #unit()} for getting the unit of measurement associated to this range.</li>
 *   <li>{@link #convertTo(Unit)} for converting the unit of measurement.</li>
 *   <li>{@link #castTo(Class)} for casting the range values to another type.</li>
 * </ul>
 *
 * <h2>Null unit of measurement</h2>
 * The unit of measurement should not be null, otherwise a {@link NumberRange} should be used
 * instead of {@code MeasurementRange}. Nevertheless this class is tolerant to {@code null}
 * units in order to support situations where a unit of measurement <em>should</em> be specified,
 * but for some reason is unavailable. If the unit of measurement become known at a later stage,
 * it can be specified by a call to {@link #convertTo(Unit)}.
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus inherently thread-safe.
 * Subclasses may or may not be immutable, at implementation choice. But implementers are
 * encouraged to make sure that subclasses remain immutable for more predictable behavior.
 *
 * @author  Martin Desruisseaux (IRD)
 * @version 0.6
 *
 * @param <E>  the type of range elements as a subclass of {@link Number}.
 *
 * @see RangeFormat
 * @see org.apache.sis.util.collection.RangeSet
 *
 * @since 0.3
 */
public class MeasurementRange<E extends Number & Comparable<? super E>> extends NumberRange<E> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3532903747339978756L;

    /**
     * The unit of measurement, or {@code null} if unknown.
     *
     * @see #unit()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final Unit<?> unit;

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
     * @param  unit           the unit of measurement, or {@code null} if unknown.
     * @return the new range of numeric values for the given endpoints and unit of measurement.
     * @throws IllegalArgumentException if {@link Float#isNaN(float)} is {@code true} for a given value.
     */
    public static MeasurementRange<Float> create(float minValue, boolean isMinIncluded,
                                                 float maxValue, boolean isMaxIncluded, Unit<?> unit)
    {
        final Float min = valueOf("minValue", minValue, Float.NEGATIVE_INFINITY);
        final Float max = valueOf("maxValue", maxValue, Float.POSITIVE_INFINITY);
        // No need to test isCacheable(Number) because the type is known and valueOf(…) disallows NaN values.
        return unique(new MeasurementRange<>(Float.class, min, isMinIncluded, Objects.equals(min, max) ? min : max, isMaxIncluded, unit));
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
     * @param  unit           the unit of measurement, or {@code null} if unknown.
     * @return the new range of numeric values for the given endpoints and unit of measurement.
     * @throws IllegalArgumentException if {@link Double#isNaN(double)} is {@code true} for a given value.
     */
    public static MeasurementRange<Double> create(double minValue, boolean isMinIncluded,
                                                  double maxValue, boolean isMaxIncluded, Unit<?> unit)
    {
        final Double min = valueOf("minValue", minValue, Double.NEGATIVE_INFINITY);
        final Double max = valueOf("maxValue", maxValue, Double.POSITIVE_INFINITY);
        // No need to test isCacheable(Number) because the type is known and valueOf(…) disallows NaN values.
        return unique(new MeasurementRange<>(Double.class, min, isMinIncluded, Objects.equals(min, max) ? min : max, isMaxIncluded, unit));
    }

    /**
     * Constructs a range of {@code double} values greater than the given value.
     * The {@code minValue} is often zero for creating a range of strictly positive values.
     * This method may return a shared instance, at implementation choice.
     *
     * @param  minValue  the minimal value (exclusive), or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  unit      the unit of measurement, or {@code null} if unknown.
     * @return the new range of numeric values greater than the given value.
     * @throws IllegalArgumentException if {@link Double#isNaN(double)} is {@code true} for the given value.
     *
     * @since 0.6
     */
    public static MeasurementRange<Double> createGreaterThan(final double minValue, final Unit<?> unit) {
        // No need to test isCacheable(Number) because the type is known and valueOf(…) disallows NaN values.
        return unique(new MeasurementRange<>(Double.class,
                valueOf("minValue", minValue, Double.NEGATIVE_INFINITY), false, null, false, unit));
    }

    /**
     * Constructs a range using the smallest type of {@link Number} that can hold the given values.
     * This method performs the same work as {@link NumberRange#createBestFit NumberRange.createBestFit(…)}
     * with an additional {@code unit} argument.
     *
     * <p>This method may return a shared instance, at implementation choice.</p>
     *
     * @param  minValue       the minimal value, or {@code null} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       the maximal value, or {@code null} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @param  unit           the unit of measurement, or {@code null} if unknown.
     * @return the new range, or {@code null} if both {@code minValue} and {@code maxValue} are {@code null}.
     *
     * @see NumberRange#createBestFit(Number, boolean, Number, boolean)
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    public static MeasurementRange<?> createBestFit(final Number minValue, final boolean isMinIncluded,
                                                    final Number maxValue, final boolean isMaxIncluded, final Unit<?> unit)
    {
        final Class<? extends Number> type;
        type = Numbers.widestClass(Numbers.narrowestClass(minValue),
                                   Numbers.narrowestClass(maxValue));
        if (type == null) {
            return null;
        }
        Number min = Numbers.cast(minValue, type);
        Number max = Numbers.cast(maxValue, type);
        final boolean isCacheable = isCacheable(min) && isCacheable(max);
        if (isCacheable && Objects.equals(min, max)) {
            max = min;      // Share the same instance.
        }
        MeasurementRange range = new MeasurementRange(type, min, isMinIncluded, max, isMaxIncluded, unit);
        if (isCacheable) {
            range = unique(range);
        }
        return range;
    }

    /**
     * Constructs a range with the same values as the specified range and the given unit.
     * This is a copy constructor, with the addition of a unit of measurement.
     *
     * @param  range  the range to copy. The elements must be {@link Number} instances.
     * @param  unit   the unit of measurement, or {@code null} if unknown.
     */
    public MeasurementRange(final Range<E> range, final Unit<?> unit) {
        super(range);
        this.unit = unit;
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
     * @param  unit   the unit of measurement, or {@code null} if unknown.
     * @throws IllegalArgumentException if the given type is not one of the primitive wrappers for numeric types.
     */
    public MeasurementRange(final Class<E> type, final ValueRange range, final Unit<?> unit) throws IllegalArgumentException {
        super(type, range);
        this.unit = unit;
    }

    /**
     * Constructs a range of {@link Number} objects.
     *
     * @param type           the element type, usually one of {@link Float} or {@link Double}.
     * @param minValue       the minimal value, or {@code null} if none.
     * @param isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param maxValue       the maximal value, or {@code null} if none.
     * @param isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @param unit           the unit of measurement, or {@code null} if unknown.
     */
    public MeasurementRange(final Class<E> type,
                            final E minValue, final boolean isMinIncluded,
                            final E maxValue, final boolean isMaxIncluded,
                            final Unit<?> unit)
    {
        super(type, minValue, isMinIncluded, maxValue, isMaxIncluded);
        this.unit = unit;
    }

    /**
     * Constructs a range with the same values as the specified range, cast to the specified type.
     *
     * @param  type   the element type, usually one of {@link Byte}, {@link Short},
     *                {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  range  the range to copy. The elements must be {@link Number} instances.
     * @param  unit   the unit of measurement, or {@code null} if unknown.
     */
    private MeasurementRange(final Class<E> type, final Range<? extends Number> range, final Unit<?> unit) {
        super(type, range);
        this.unit = unit;
    }

    /**
     * Creates a new range using the same element type and the same unit as this range.
     */
    @Override
    Range<E> create(final E minValue, final boolean isMinIncluded,
                    final E maxValue, final boolean isMaxIncluded)
    {
        return new MeasurementRange<>(elementType, minValue, isMinIncluded, maxValue, isMaxIncluded, unit);
    }

    /**
     * Returns the unit of measurement, or {@code null} if unknown.
     * In principle the unit should never be null, otherwise a {@link NumberRange} should have been used
     * instead of {@code MeasurementRange}. Nevertheless this method may return {@code null} if a unit
     * <em>should</em> exist but for some reason is unavailable.
     *
     * @return the unit of measurement, or {@code null}.
     */
    @Override
    public Unit<?> unit() {
        return unit;
    }

    /**
     * Converts this range to the specified unit. If this measurement range has null unit,
     * then the specified target unit are simply assigned to the returned range with no
     * other changes.
     *
     * @param  targetUnit  the target unit, or {@code null} for keeping the unit unchanged.
     * @return the converted range, or {@code this} if no conversion is needed.
     * @throws IncommensurableException if the target unit are not compatible with this {@linkplain #unit() range unit}.
     */
    public MeasurementRange<E> convertTo(final Unit<?> targetUnit) throws IncommensurableException {
        return convertAndCast(elementType, targetUnit);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <N extends Number & Comparable<? super N>> MeasurementRange<N> castTo(final Class<N> type) {
        if (elementType == type) {
            return (MeasurementRange<N>) this;
        } else {
            return new MeasurementRange<>(type, this, unit);
        }
    }

    /**
     * If the given range is an instance of {@code MeasurementRange}, converts that
     * range to the unit of this range. Otherwise returns the given range unchanged.
     *
     * @param  range  the range to convert.
     * @return the converted range.
     * @throws IllegalArgumentException if the given target unit is not compatible with the unit of this range.
     */
    private <N extends E> Range<N> convert(final Range<N> range) throws IllegalArgumentException {
        if (range instanceof MeasurementRange<?>) try {
            return ((MeasurementRange<N>) range).convertAndCast(range.elementType, unit);
        } catch (IncommensurableException e) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleUnits_2,
                    ((MeasurementRange<?>) range).unit, unit), e);
        }
        return range;
    }

    /**
     * Casts the specified range to the specified type. If this class is associated to a unit of
     * measurement, then this method convert the {@code range} unit to the same unit as this
     * instance.
     *
     * @param  type  the class to cast to. Must be one of {@link Byte}, {@link Short},
     *               {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @return the cast range, or {@code range} if no cast is needed.
     */
    @Override
    <N extends Number & Comparable<? super N>>
    NumberRange<N> convertAndCast(final NumberRange<?> range, final Class<N> type)
            throws IllegalArgumentException
    {
        if (range instanceof MeasurementRange<?>) try {
            return ((MeasurementRange<?>) range).convertAndCast(type, unit);
        } catch (IncommensurableException e) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleUnits_2,
                    ((MeasurementRange<?>) range).unit, unit), e);
        }
        return new MeasurementRange<>(type, range, unit);
    }

    /**
     * Casts this range to the specified type and converts to the specified unit.
     * This method is invoked on the {@code other} instance in expressions like
     * {@code this.operation(other)}.
     *
     * @param  type  the class to cast to. Must be one of {@link Byte}, {@link Short},
     *               {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  targetUnit the target unit, or {@code null} for no change.
     * @return the cast range, or {@code this}.
     * @throws IncommensurableException if the given target unit is not compatible with the unit of this range.
     */
    @SuppressWarnings("unchecked")
    private <N extends Number & Comparable<? super N>> MeasurementRange<N>
            convertAndCast(final Class<N> type, Unit<?> targetUnit) throws IncommensurableException
    {
        if (targetUnit == null || targetUnit.equals(unit)) {
            if (elementType == type) {
                return (MeasurementRange<N>) this;
            }
            targetUnit = unit;
        } else if (unit != null) {
            final UnitConverter converter = unit.getConverterToAny(targetUnit);
            if (!converter.isIdentity()) {
                boolean minInc = isMinIncluded;
                boolean maxInc = isMaxIncluded;
                double minimum = converter.convert(getMinDouble());
                double maximum = converter.convert(getMaxDouble());
                if (minimum > maximum) {
                    final double  td = minimum; minimum = maximum; maximum = td;
                    final boolean tb = minInc;  minInc  = maxInc;  maxInc  = tb;
                }
                if (Numbers.isInteger(type)) {
                    minInc &= (minimum == (minimum = Math.floor(minimum)));
                    maxInc &= (maximum == (maximum = Math.ceil (maximum)));
                }
                return new MeasurementRange<>(type,
                        Numbers.cast(minimum, type), minInc,
                        Numbers.cast(maximum, type), maxInc, targetUnit);
            }
        }
        return new MeasurementRange<>(type, this, targetUnit);
    }

    /**
     * Returns an initially empty array of the given length.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"}) // Generic array creation.
    final Range<E>[] newArray(final int length) {
        return new MeasurementRange[length];
    }

    /**
     * {@inheritDoc}
     * If the given range is an instance of {@code MeasurementRange}, then this method converts
     * the value of the other range to the unit of measurement of this range before to perform
     * the operation.
     *
     * @return {@inheritDoc}
     * @throws IllegalArgumentException if the given range is an instance of
     *         {@code MeasurementRange} using incommensurable unit of measurement.
     */
    @Override
    public boolean contains(final Range<? extends E> range) throws IllegalArgumentException {
        return super.contains(convert(range));
    }

    /**
     * {@inheritDoc}
     * If the given range is an instance of {@code MeasurementRange}, then this method converts
     * the value of the other range to the unit of measurement of this range before to perform
     * the operation.
     *
     * @return {@inheritDoc}
     * @throws IllegalArgumentException if the given range is an instance of
     *         {@code MeasurementRange} using incommensurable unit of measurement.
     */
    @Override
    public boolean intersects(final Range<? extends E> range) throws IllegalArgumentException {
        return super.intersects(convert(range));
    }

    /**
     * {@inheritDoc}
     * If the given range is an instance of {@code MeasurementRange}, then this method converts
     * the value of the other range to the unit of measurement of this range before to perform
     * the operation.
     *
     * @return {@inheritDoc}
     * @throws IllegalArgumentException if the given range is an instance of
     *         {@code MeasurementRange} using incommensurable unit of measurement.
     */
    @Override
    public Range<E> intersect(final Range<E> range) throws IllegalArgumentException {
        return super.intersect(convert(range));
    }

    /**
     * {@inheritDoc}
     * If the given range is an instance of {@code MeasurementRange}, then this method converts
     * the value of the other range to the unit of measurement of this range before to perform
     * the operation.
     *
     * @return {@inheritDoc}
     * @throws IllegalArgumentException if the given range is an instance of
     *         {@code MeasurementRange} using incommensurable unit of measurement.
     */
    @Override
    public Range<E> union(final Range<E> range) throws IllegalArgumentException {
        return super.union(convert(range));
    }

    /**
     * {@inheritDoc}
     * If the given range is an instance of {@code MeasurementRange}, then this method converts
     * the value of the other range to the unit of measurement of this range before to perform
     * the operation.
     *
     * @return {@inheritDoc}
     * @throws IllegalArgumentException if the given range is an instance of
     *         {@code MeasurementRange} using incommensurable unit of measurement.
     */
    @Override
    public Range<E>[] subtract(final Range<E> range) throws IllegalArgumentException {
        return super.subtract(convert(range));
    }

    /**
     * Compares this measurement range with the specified object for equality. Two {@code MeasurementRange} instances
     * are considered equal if they met all conditions {@linkplain Range#equals(Object) documented in the parent class}
     * and their {@link #unit()} are equal in the sense of {@link Objects#equals(Object, Object)}.
     * Note that this comparison does not distinguish the various {@link Float#NaN} or {@link Double#NaN} bit patterns.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        return super.equals(object) && Objects.equals(unit, ((MeasurementRange<?>) object).unit);
    }

    /**
     * Returns a hash code value for this measurement range.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hashCode(unit);
    }
}
