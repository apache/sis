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

import java.lang.reflect.Proxy;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.UnitConverter;
import javax.measure.UnconvertibleException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.logging.Logging;


/**
 * A quantity representable by position on a scale or line, having only magnitude.
 * {@code Scalar} are represented by a single floating point number and a unit of measurement.
 * This is the base class for some commonly used quantity types like length, angle and dimensionless quantities.
 * Instances of this class are unmodifiable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 *
 * @param <Q>  the type of quantity implemented by this scalar.
 */
class Scalar<Q extends Quantity<Q>> extends Number implements Quantity<Q>, Comparable<Q> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -381805117700594712L;

    /**
     * The scale of this quantity. Currently only absolute scale is supported.
     */
    static final Scale SCALE = Scale.ABSOLUTE;

    /**
     * The numerical value of this quantity.
     */
    private final double value;

    /**
     * The unit of measurement associated to the value.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final Unit<Q> unit;

    /**
     * Creates a new scalar for the given value.
     * Callers should ensure that the unit argument is non-null.
     */
    Scalar(final double value, final Unit<Q> unit) {
        this.value = value;
        this.unit  = unit;
    }

    /**
     * Creates a new quantity of same type as this quantity but with a different value and/or unit.
     * This method performs the same work as {@link Quantities#create(double, Unit)}, but without
     * the need to check for the Apache SIS specific {@link SystemUnit} implementation.
     *
     * <p>This method is invoked (indirectly) in only two situations:</p>
     * <ul>
     *   <li>Arithmetic operations that do not change the unit of measurement (addition, subtraction),
     *       in which case the given {@code newUnit} is the same that the unit of this quantity.</li>
     *   <li>Conversion to a new compatible unit by {@link #to(Unit)}, provided that the conversion
     *       is only a scale factor.</li>
     * </ul>
     *
     * {@link DerivedScalar} relies on the fact that there are no other situations where this method
     * is invoked. If this assumption become not true anymore in a future SIS version, then we need
     * to revisit {@code DerivedScalar}.
     *
     * @see Quantities#create(double, Unit)
     */
    Quantity<Q> create(double newValue, Unit<Q> newUnit) {
        return new Scalar<>(newValue, newUnit);
    }

    /**
     * Returns a quantity quantity of same type as this quantity but with a different value and/or unit.
     * If the new value and unit are the same as this quantity, then {@code this} instance is returned.
     * Positive and negative zeros are considered two different values.
     */
    private Quantity<?> of(final double newValue, final Unit<?> newUnit) {
        if (unit != newUnit || Double.doubleToRawLongBits(value) != Double.doubleToRawLongBits(newValue)) {
            return Quantities.create(newValue, newUnit);
        }
        return this;
    }

    /**
     * Returns a quantity with the same units as this quantity. If the new value is the same
     * than current value, then {@code this} instance is returned. Positive and negative zeros
     * are considered two different values.
     */
    private Quantity<Q> of(final double newValue) {
        if (Double.doubleToRawLongBits(value) != Double.doubleToRawLongBits(newValue)) {
            return create(newValue, unit);
        }
        return this;
    }

    /**
     * Returns the scale of this quantity, which can be absolute or relative.
     *
     * @return whether this quantity uses absolute or relative scale.
     */
    @Override
    public final Scale getScale() {
        return SCALE;
    }

    /**
     * Returns the unit of measurement specified at construction time.
     * The method shall not return {@code null}.
     */
    @Override
    public Unit<Q> getUnit() {
        return unit;
    }

    /**
     * Returns the value as a number, which is this instance itself.
     */
    @Override
    public final Number getValue() {
        return this;
    }

    /**
     * Returns the value specified at construction time.
     */
    @Override
    public double doubleValue() {
        return value;
    }

    /**
     * Returns the value cast to a single-precision floating point number.
     */
    @Override
    public float floatValue() {
        return (float) value;
    }

    /**
     * Returns the value rounded to nearest integer. {@link Double#NaN} are cast to 0 and values out of
     * {@code long} range are clamped to minimal or maximal representable numbers of {@code long} type.
     */
    @Override
    public long longValue() {
        return Math.round(value);
    }

    /**
     * Returns the value rounded to nearest integer. {@link Double#NaN} are cast to 0 and values out of
     * {@code int} range are clamped to minimal or maximal representable numbers of {@code int} type.
     */
    @Override
    public final int intValue() {
        return Numerics.clamp(longValue());
    }

    /**
     * Returns the value rounded to nearest integer. {@link Double#NaN} are cast to 0 and values out of
     * {@code short} range are clamped to minimal or maximal representable numbers of {@code short} type.
     */
    @Override
    public final short shortValue() {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, longValue()));
    }

    /**
     * Returns the value rounded to nearest integer. {@link Double#NaN} are cast to 0 and values out of
     * {@code byte} range are clamped to minimal or maximal representable numbers of {@code byte} type.
     */
    @Override
    public final byte byteValue() {
        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, longValue()));
    }

    /**
     * Returns the value of the given quantity converted to the same units of measurement than this quantity.
     */
    private double doubleValue(final Quantity<Q> other) {
        double otherValue = other.getValue().doubleValue();
        final Unit<Q> otherUnit = other.getUnit();
        if (otherUnit != unit) {
            otherValue = otherUnit.getConverterTo(unit).convert(otherValue);
        }
        return otherValue;
    }

    /**
     * Compares the numerical value of this quantity with the value of another quantity of the same type.
     * The comparison is performed with {@code double} precision in the units of measurement of this quantity.
     */
    @Override
    public final int compareTo(final Q other) {
        return Double.compare(value, doubleValue(other));
    }

    /**
     * Converts this quantity to another unit of measurement.
     * This default implementation is valid only if the unit of this quantity is a system unit,
     * or convertible to the system unit with only a scale factor. If this assumption does not
     * hold anymore (as in {@link DerivedScalar} subclass), then this method needs to be overridden.
     */
    @Override
    public Quantity<Q> to(final Unit<Q> newUnit) {
        if (newUnit == unit) {
            return this;
        }
        ArgumentChecks.ensureNonNull("unit", newUnit);      // "unit" is the parameter name used in public API.
        assert unit.getConverterTo(unit.getSystemUnit()).isLinear() : unit;              // See method javadoc.
        final UnitConverter c = unit.getConverterTo(newUnit);
        final double newValue = c.convert(value);
        if (c.isLinear()) {                                 // Despite method name, this is actually "is scale".
            /*
             * Conversion from this quantity to system unit was a scale factor (see assumption documented
             * in this method javadoc) and given conversion is also a scale factor. Consequently, conversion
             * from the new quantity unit to system unit will still be a scale factor, in which case this
             * `Scalar` class is still appropriate.
             */
            return create(newValue, newUnit);
        } else {
            /*
             * Re-evaluate if we need to create a DerivedScalar subclass.
             */
            return Quantities.create(newValue, newUnit);
        }
    }

    /**
     * Returns the sum of this {@code Quantity} with another quantity.
     * The result is given in units of this quantity.
     */
    @Override
    public final Quantity<Q> add(final Quantity<Q> other) {
        return of(value + doubleValue(other));
    }

    /**
     * Returns the difference between this {@code Quantity} and the given quantity.
     * The result is given in units of this quantity.
     */
    @Override
    public final Quantity<Q> subtract(final Quantity<Q> other) {
        return of(value - doubleValue(other));
    }

    /**
     * Returns this quantity scaled by the given number.
     */
    @Override
    public final Quantity<Q> multiply(final Number scale) {
        return of(value * scale.doubleValue());
    }

    /**
     * Returns this quantity divided by the given number.
     */
    @Override
    public final Quantity<Q> divide(final Number divisor) {
        return of(value / divisor.doubleValue());
    }

    /**
     * Returns this quantity multiplied by the given quantity.
     */
    @Override
    public final Quantity<?> multiply(final Quantity<?> other) {
        return of(value * other.getValue().doubleValue(), unit.multiply(other.getUnit()));
    }

    /**
     * Returns this quantity divided by the given quantity.
     */
    @Override
    public final Quantity<?> divide(final Quantity<?> other) {
        return of(value / other.getValue().doubleValue(), unit.divide(other.getUnit()));
    }

    /**
     * Returns the reciprocal of this quantity.
     */
    @Override
    public final Quantity<?> inverse() {
        return of(1 / value, unit.inverse());
    }

    /**
     * Returns a quantity whose value is {@code −getValue()}.
     */
    @Override
    public final Quantity<Q> negate() {
        return of(-value);
    }

    /**
     * Ensures that this quantity is of the given type.
     */
    @Override
    public final <T extends Quantity<T>> Quantity<T> asType(final Class<T> type) throws ClassCastException {
        return type.cast(this);
    }

    /**
     * Compares this quantity with the given quantity, doing the conversion of unit if necessary.
     *
     * @param  that  the quantity to be compared with this instance.
     * @return {@code true} if the two quantities are equivalent.
     */
    @Override
    public final boolean isEquivalentTo(final Quantity<Q> that) {
        final Unit<Q> otherUnit = that.getUnit();
        try {
            final Number r = otherUnit.getConverterTo(unit).convert(that.getValue());
            if (r instanceof Float) {
                return Math.abs(value - r.doubleValue()) < Math.ulp(r.floatValue());
            } else {
                return value == r.doubleValue();
            }
        } catch (UnconvertibleException e) {
            Logging.ignorableException(AbstractUnit.LOGGER, Scalar.class, "isEquivalentTo", e);
            // Non-convertible quantities are not equivalent.
        }
        return false;
    }

    /**
     * Returns {@code true} if the given object is another {@code Scalar} with the same value and same unit
     * of measurement.
     */
    @Override
    public final boolean equals(Object other) {
        if (!(other instanceof Scalar<?>)) {
            if (!(other instanceof Proxy)) {
                return false;
            }
            try {
                other = Proxy.getInvocationHandler(other);
            } catch (IllegalArgumentException | SecurityException e) {
                return false;
            }
            if (!(other instanceof Scalar<?>)) {
                return false;
            }
        }
        /*
         * We require the Scalar implementation rather than accepting arbitrary Quantity<?> instance
         * for making sure that we obey to Object.equals(Object) contract (e.g. symmetric, transitive,
         * etc.). But we invoke the getter methods instead of accessing directly the fields because
         * DerivedScalar override them.
         */
        final Scalar<?> that = (Scalar<?>) other;
        return Double.doubleToLongBits(doubleValue()) == Double.doubleToLongBits(that.doubleValue())
                && getUnit().equals(that.getUnit());
    }

    /**
     * Returns a hash code value for this quantity. This method computes the code from values returned by
     * {@link #doubleValue()} and {@link #getUnit()} methods, which may be overridden by sub-classes.
     */
    @Override
    public final int hashCode() {
        return Double.hashCode(doubleValue()) ^ getUnit().hashCode();
    }

    /**
     * Returns the quantity value followed by its units of measurement. This method uses the values returned
     * by {@link #doubleValue()} and {@link #getUnit()} methods, which may be overridden by sub-classes.
     */
    @Override
    public final String toString() {
        final StringBuilder buffer = new StringBuilder().append(doubleValue());
        StringBuilders.trimFractionalPart(buffer);
        final String symbol = getUnit().toString();
        if (symbol != null && !symbol.isEmpty()) {
            buffer.append(QuantityFormat.SEPARATOR).append(symbol);
        }
        return buffer.toString();
    }


    /*
     * Following inner classes are straightforward implementations for some commonly used quantity types.
     * We do not need to provide an implementation for every types, because we have a proxy mechanism as
     * a fallback for less frequently used types.
     */


    static final class Dimensionless extends Scalar<javax.measure.quantity.Dimensionless>
                                     implements     javax.measure.quantity.Dimensionless
    {
        private static final long serialVersionUID = -7783945219314403648L;
        Dimensionless(double value, Unit<javax.measure.quantity.Dimensionless> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Dimensionless> create(double value, Unit<javax.measure.quantity.Dimensionless> unit) {
            return new Dimensionless(value, unit);
        }
    }

    static final class Angle extends Scalar<javax.measure.quantity.Angle>
                             implements     javax.measure.quantity.Angle
    {
        private static final long serialVersionUID = -1706116845342397826L;
        Angle(double value, Unit<javax.measure.quantity.Angle> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Angle> create(double value, Unit<javax.measure.quantity.Angle> unit) {
            return new Angle(value, unit);
        }
    }

    static final class Length extends Scalar<javax.measure.quantity.Length>
                              implements     javax.measure.quantity.Length
    {
        private static final long serialVersionUID = 6664029554501181657L;
        Length(double value, Unit<javax.measure.quantity.Length> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Length> create(double value, Unit<javax.measure.quantity.Length> unit) {
            return new Length(value, unit);
        }
    }

    static final class Area extends Scalar<javax.measure.quantity.Area>
                            implements     javax.measure.quantity.Area
    {
        private static final long serialVersionUID = -9127932093170175175L;
        Area(double value, Unit<javax.measure.quantity.Area> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Area> create(double value, Unit<javax.measure.quantity.Area> unit) {
            return new Area(value, unit);
        }
    }

    static final class Volume extends Scalar<javax.measure.quantity.Volume>
                              implements     javax.measure.quantity.Volume
    {
        private static final long serialVersionUID = -1505528008598251420L;
        Volume(double value, Unit<javax.measure.quantity.Volume> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Volume> create(double value, Unit<javax.measure.quantity.Volume> unit) {
            return new Volume(value, unit);
        }
    }

    static final class Time extends Scalar<javax.measure.quantity.Time>
                            implements     javax.measure.quantity.Time
    {
        private static final long serialVersionUID = 3992130757485565027L;
        Time(double value, Unit<javax.measure.quantity.Time> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Time> create(double value, Unit<javax.measure.quantity.Time> unit) {
            return new Time(value, unit);
        }
    }

    static final class Frequency extends Scalar<javax.measure.quantity.Frequency>
                                 implements     javax.measure.quantity.Frequency
    {
        private static final long serialVersionUID = -2038564695278895642L;
        Frequency(double value, Unit<javax.measure.quantity.Frequency> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Frequency> create(double value, Unit<javax.measure.quantity.Frequency> unit) {
            return new Frequency(value, unit);
        }
    }

    static final class Speed extends Scalar<javax.measure.quantity.Speed>
                             implements     javax.measure.quantity.Speed
    {
        private static final long serialVersionUID = 4086187563299428546L;
        Speed(double value, Unit<javax.measure.quantity.Speed> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Speed> create(double value, Unit<javax.measure.quantity.Speed> unit) {
            return new Speed(value, unit);
        }
    }

    static final class Acceleration extends Scalar<javax.measure.quantity.Acceleration>
                                    implements     javax.measure.quantity.Acceleration
    {
        private static final long serialVersionUID = 8041442665100572880L;
        Acceleration(double value, Unit<javax.measure.quantity.Acceleration> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Acceleration> create(double value, Unit<javax.measure.quantity.Acceleration> unit) {
            return new Acceleration(value, unit);
        }
    }

    static final class Mass extends Scalar<javax.measure.quantity.Mass>
                            implements     javax.measure.quantity.Mass
    {
        private static final long serialVersionUID = -3348515590324141647L;
        Mass(double value, Unit<javax.measure.quantity.Mass> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Mass> create(double value, Unit<javax.measure.quantity.Mass> unit) {
            return new Mass(value, unit);
        }
    }

    static final class Force extends Scalar<javax.measure.quantity.Force>
                             implements     javax.measure.quantity.Force
    {
        private static final long serialVersionUID = -4988289861436247522L;
        Force(double value, Unit<javax.measure.quantity.Force> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Force> create(double value, Unit<javax.measure.quantity.Force> unit) {
            return new Force(value, unit);
        }
    }

    static final class Energy extends Scalar<javax.measure.quantity.Energy>
                              implements     javax.measure.quantity.Energy
    {
        private static final long serialVersionUID = 857370990868536857L;
        Energy(double value, Unit<javax.measure.quantity.Energy> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Energy> create(double value, Unit<javax.measure.quantity.Energy> unit) {
            return new Energy(value, unit);
        }
    }

    static final class Power extends Scalar<javax.measure.quantity.Power>
                             implements     javax.measure.quantity.Power
    {
        private static final long serialVersionUID = -5751533351918725110L;
        Power(double value, Unit<javax.measure.quantity.Power> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Power> create(double value, Unit<javax.measure.quantity.Power> unit) {
            return new Power(value, unit);
        }
    }

    static final class Pressure extends Scalar<javax.measure.quantity.Pressure>
                                implements     javax.measure.quantity.Pressure
    {
        private static final long serialVersionUID = -8647834252032382587L;
        Pressure(double value, Unit<javax.measure.quantity.Pressure> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Pressure> create(double value, Unit<javax.measure.quantity.Pressure> unit) {
            return new Pressure(value, unit);
        }
    }

    static final class Temperature extends Scalar<javax.measure.quantity.Temperature>
                                   implements     javax.measure.quantity.Temperature
    {
        static final ScalarFactory<javax.measure.quantity.Temperature> FACTORY = new ScalarFactory<javax.measure.quantity.Temperature>() {
            @Override public javax.measure.quantity.Temperature create(double value, Unit<javax.measure.quantity.Temperature> unit) {
                return new Temperature(value, unit);
            }

            @Override public javax.measure.quantity.Temperature createDerived(double value, Unit<javax.measure.quantity.Temperature> unit,
                    Unit<javax.measure.quantity.Temperature> systemUnit, UnitConverter toSystem)
            {
                return new DerivedScalar.TemperatureMeasurement(value, unit, systemUnit, toSystem);
            }
        };

        private static final long serialVersionUID = -6391507887931973739L;
        Temperature(double value, Unit<javax.measure.quantity.Temperature> unit) {super(value, unit);}

        @Override
        Quantity<javax.measure.quantity.Temperature> create(double value, Unit<javax.measure.quantity.Temperature> unit) {
            return new Temperature(value, unit);
        }
    }
}
