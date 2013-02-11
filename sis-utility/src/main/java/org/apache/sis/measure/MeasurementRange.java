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

import javax.measure.unit.Unit;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.ConversionException;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;

// Related to JDK7
import java.util.Objects;


/**
 * A range of numbers associated with a unit of measurement. Unit conversions are applied as
 * needed by {@linkplain #union union} and {@linkplain #intersect intersection} operations.
 *
 * @param <T> The type of range elements as a subclass of {@link Number}.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@Immutable
public class MeasurementRange<T extends Number & Comparable<? super T>> extends NumberRange<T> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3980319420337513745L;

    /**
     * The units of measurement, or {@code null} if unknown.
     */
    private final Unit<?> units;

    /**
     * Constructs an inclusive range of {@code float} values.
     *
     * @param  minValue The minimal value, inclusive, or {@link Float#NEGATIVE_INFINITY} if none..
     * @param  maxValue The maximal value, <strong>inclusive</strong>, or {@link Float#POSITIVE_INFINITY} if none.
     * @param  units    The units of measurement, or {@code null} if unknown.
     * @return The new range of numeric values for the given bounds and unit of measurement.
     */
    public static MeasurementRange<Float> create(float minValue, float maxValue, Unit<?> units) {
        return new MeasurementRange<>(Float.class,
                valueOf("minValue", minValue, Float.NEGATIVE_INFINITY),
                valueOf("maxValue", maxValue, Float.POSITIVE_INFINITY), units);
    }

    /**
     * Constructs a range of {@code float} values.
     *
     * @param  minValue       The minimal value, or {@link Float#NEGATIVE_INFINITY} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value, or {@link Float#POSITIVE_INFINITY} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @param  units          The units of measurement, or {@code null} if unknown.
     * @return The new range of numeric values for the given bounds and unit of measurement.
     */
    public static MeasurementRange<Float> create(float minValue, boolean isMinIncluded,
                                                 float maxValue, boolean isMaxIncluded, Unit<?> units)
    {
        return new MeasurementRange<>(Float.class,
                valueOf("minValue", minValue, Float.NEGATIVE_INFINITY), isMinIncluded,
                valueOf("maxValue", maxValue, Float.POSITIVE_INFINITY), isMaxIncluded, units);
    }

    /**
     * Constructs an inclusive range of {@code double} values.
     *
     * @param  minValue The minimal value, inclusive, or {@link Double#NEGATIVE_INFINITY} if none..
     * @param  maxValue The maximal value, <strong>inclusive</strong>, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  units    The units of measurement, or {@code null} if unknown.
     * @return The new range of numeric values for the given bounds and unit of measurement.
     */
    public static MeasurementRange<Double> create(double minValue, double maxValue, Unit<?> units) {
        return new MeasurementRange<>(Double.class,
                valueOf("minValue", minValue, Double.NEGATIVE_INFINITY),
                valueOf("maxValue", maxValue, Double.POSITIVE_INFINITY), units);
    }

    /**
     * Constructs a range of {@code double} values.
     *
     * @param  minValue       The minimal value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param  maxValue       The maximal value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     * @param  units          The units of measurement, or {@code null} if unknown.
     * @return The new range of numeric values for the given bounds and unit of measurement.
     */
    public static MeasurementRange<Double> create(double minValue, boolean isMinIncluded,
                                                  double maxValue, boolean isMaxIncluded, Unit<?> units)
    {
        return new MeasurementRange<>(Double.class,
                valueOf("minValue", minValue, Double.NEGATIVE_INFINITY), isMinIncluded,
                valueOf("maxValue", maxValue, Double.POSITIVE_INFINITY), isMaxIncluded, units);
    }

    /**
     * Constructs a range using the smallest type of {@link Number} that can hold the given values.
     * This method performs the same work than {@link NumberRange#createBestFit
     * NumberRange.createBestFit(…)} with an additional {@code units} argument.
     *
     * @param  minimum        The minimum value, or {@code null} for negative infinity.
     * @param  isMinIncluded  Defines whether the minimum value is included in the range.
     * @param  maximum        The maximum value, or {@code null} for positive infinity.
     * @param  isMaxIncluded  Defines whether the maximum value is included in the range.
     * @param  units          The units of measurement, or {@code null} if unknown.
     * @return The new range, or {@code null} if both {@code minimum} and {@code maximum}
     *         are {@code null}.
     *
     * @see NumberRange#createBestFit(Number, boolean, Number, boolean)
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    public static MeasurementRange<?> createBestFit(final Number minimum, final boolean isMinIncluded,
            final Number maximum, final boolean isMaxIncluded, final Unit<?> units)
    {
        final Class<? extends Number> type = Numbers.widestClass(
                Numbers.narrowestClass(minimum), Numbers.narrowestClass(maximum));
        return (type == null) ? null :
            new MeasurementRange(type, Numbers.cast(minimum, type), isMinIncluded,
                                       Numbers.cast(maximum, type), isMaxIncluded, units);
    }

    /**
     * Constructs a range with the same values than the specified range and the given units.
     * This is a copy constructor, with the addition of a unit of measurement.
     *
     * @param range The range to copy. The elements must be {@link Number} instances.
     * @param units The units of measurement, or {@code null} if unknown.
     */
    public MeasurementRange(final Range<T> range, final Unit<?> units) {
        super(range);
        this.units = units;
    }

    /**
     * Constructs a range of {@link Number} objects.
     *
     * @param type          The element class, usually one of {@link Byte}, {@link Short},
     *                      {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param minimum       The minimum value.
     * @param maximum       The maximum value.
     * @param units         The units of measurement, or {@code null} if unknown.
     */
    public MeasurementRange(final Class<T> type, final T minimum, final T maximum, final Unit<?> units) {
        super(type, minimum, maximum);
        this.units = units;
    }

    /**
     * Constructs a range of {@link Number} objects.
     *
     * @param type          The element class, usually one of {@link Byte}, {@link Short},
     *                      {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param minimum       The minimum value.
     * @param isMinIncluded Defines whether the minimum value is included in the Range.
     * @param maximum       The maximum value.
     * @param isMaxIncluded Defines whether the maximum value is included in the Range.
     * @param units         The units of measurement, or {@code null} if unknown.
     */
    public MeasurementRange(final Class<T> type,
                            final T minimum, final boolean isMinIncluded,
                            final T maximum, final boolean isMaxIncluded,
                            final Unit<?> units)
    {
        super(type, minimum, isMinIncluded, maximum, isMaxIncluded);
        this.units = units;
    }

    /**
     * Constructs a range with the same values than the specified range,
     * casted to the specified type.
     *
     * @param type The element class, usually one of {@link Byte}, {@link Short},
     *             {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param range The range to copy. The elements must be {@link Number} instances.
     * @param units   The units of measurement, or {@code null} if unknown.
     */
    private MeasurementRange(Class<T> type, Range<? extends Number> range, final Unit<?> units) {
        super(type, range);
        this.units = units;
    }

    /**
     * Creates a new range using the same element class than this range.
     */
    @Override
    MeasurementRange<T> create(final T minValue, final boolean isMinIncluded,
                               final T maxValue, final boolean isMaxIncluded)
    {
        return new MeasurementRange<>(elementType, minValue, isMinIncluded, maxValue, isMaxIncluded, units);
    }

    /**
     * Returns the units of measurement, or {@code null} if unknown.
     *
     * @return The units of measurement, or {@code null}.
     */
    @Override
    public Unit<?> getUnits() {
        return units;
    }

    /**
     * Converts this range to the specified units. If this measurement range has null units,
     * then the specified target units are simply assigned to the returned range with no
     * other changes.
     *
     * @param  targetUnits the target units, or {@code null} for keeping the units unchanged.
     * @return The converted range, or {@code this} if no conversion is needed.
     * @throws ConversionException if the target units are not compatible with
     *         this {@linkplain #getUnits range units}.
     */
    public MeasurementRange<T> convertTo(final Unit<?> targetUnits) throws ConversionException {
        return convertAndCast(elementType, targetUnits);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number & Comparable<? super N>> MeasurementRange<N> castTo(Class<N> type) {
        return convertAndCast(this, type);
    }

    /**
     * Casts the specified range to the specified type. If this class is associated to a unit of
     * measurement, then this method convert the {@code range} units to the same units than this
     * instance.
     *
     * @param type The class to cast to. Must be one of {@link Byte}, {@link Short},
     *             {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @return The casted range, or {@code range} if no cast is needed.
     */
    @Override
    <N extends Number & Comparable<? super N>>
    MeasurementRange<N> convertAndCast(final Range<? extends Number> range, final Class<N> type)
            throws IllegalArgumentException
    {
        if (range instanceof MeasurementRange<?>) {
            final MeasurementRange<?> casted = (MeasurementRange<?>) range;
            try {
                return casted.convertAndCast(type, units);
            } catch (ConversionException e) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.IncompatibleUnits_2, casted.units, units), e);
            }
        }
        return new MeasurementRange<>(type, range, units);
    }

    /**
     * Casts this range to the specified type and converts to the specified units.
     *
     * @param  type The class to cast to. Must be one of {@link Byte}, {@link Short},
     *             {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  targetUnit the target units, or {@code null} for no change.
     * @return The casted range, or {@code this}.
     * @throws ConversionException if the target units are not compatible with
     *         this {@linkplain #getUnits range units}.
     */
    @SuppressWarnings("unchecked")
    private <N extends Number & Comparable<? super N>> MeasurementRange<N>
            convertAndCast(final Class<N> type, final Unit<?> targetUnits) throws ConversionException
    {
        if (targetUnits == null || targetUnits.equals(units)) {
            if (type.equals(elementType)) {
                return (MeasurementRange<N>) this;
            } else {
                return new MeasurementRange<>(type, this, units);
            }
        }
        if (units == null) {
            return new MeasurementRange<>(type, this, targetUnits);
        }
        final UnitConverter converter = units.getConverterToAny(targetUnits);
        if (converter.equals(UnitConverter.IDENTITY)) {
            return new MeasurementRange<>(type, this, targetUnits);
        }
        boolean isMinIncluded = isMinIncluded();
        boolean isMaxIncluded = isMaxIncluded();
        Double minimum = converter.convert(getMinimum());
        Double maximum = converter.convert(getMaximum());
        if (minimum.compareTo(maximum) > 0) {
            final Double td = minimum;
            minimum = maximum;
            maximum = td;
            final boolean tb = isMinIncluded;
            isMinIncluded = isMaxIncluded;
            isMaxIncluded = tb;
        }
        return new MeasurementRange<>(type,
                Numbers.cast(minimum, type), isMinIncluded,
                Numbers.cast(maximum, type), isMaxIncluded, targetUnits);
    }

    /**
     * Returns an initially empty array of the given length.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"}) // Generic array creation.
    MeasurementRange<T>[] newArray(final int length) {
        return new MeasurementRange[length];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MeasurementRange<?> union(final Range<?> range) throws IllegalArgumentException {
        return (MeasurementRange<?>) super.union(range);
        // Should never throw ClassCastException because super.union(Range) invokes create(...),
        // which is overridden in this class with MeasurementRange return type.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MeasurementRange<?> intersect(final Range<?> range) throws IllegalArgumentException {
        return (MeasurementRange<?>) super.intersect(range);
        // Should never throw ClassCastException because super.intersect(Range) invokes
        // convertAndCast(...),  which is overridden in this class with MeasurementRange
        // return type.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MeasurementRange<?>[] subtract(final Range<?> range) throws IllegalArgumentException {
        return (MeasurementRange<?>[]) super.subtract(range);
        // Should never throw ClassCastException because super.subtract(Range) invokes newArray(int)
        // and create(...), which are overridden in this class with MeasurementRange return type.
    }

    /**
     * Compares this range with the specified object for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            if (object instanceof MeasurementRange<?>) {
                final MeasurementRange<?> that = (MeasurementRange<?>) object;
                return Objects.equals(this.units, that.units);
            }
            return true;
        }
        return false;
    }
}
