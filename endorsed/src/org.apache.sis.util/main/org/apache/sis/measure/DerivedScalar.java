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
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.UnitConverter;
import org.apache.sis.util.ArgumentChecks;


/**
 * A quantity related to a scalar by an arbitrary (not necessarily linear) conversion.
 * For example, a temperature in Celsius degrees is related to a temperature in Kelvin
 * by applying an offset.
 *
 * <p>The {@link Scalar} parent class is restricted to cases where the relationship with system unit
 * is a scale factor. This {@code DerivedScalar} subclass allow the relationship to be more generic.
 * It is a design similar to {@link org.opengis.referencing.crs.DerivedCRS}</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <Q>  the type of quantity implemented by this scalar.
 */
class DerivedScalar<Q extends Quantity<Q>> extends Scalar<Q> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3729159568163676568L;

    /**
     * The value specified by the user, in unit of {@link #derivedUnit}.
     * Could be computed form super-class value, but nevertheless stored
     * for avoiding rounding errors.
     */
    private final double derivedValue;

    /**
     * The unit of measurement specified by the user. The relationship between this unit
     * and its system unit (stored in super-class) is something more complex than a scale
     * factor, otherwise we would not need this {@code DerivedScalar}.
     */
    @SuppressWarnings("serial")             // Most SIS implementations are serializable.
    private final Unit<Q> derivedUnit;

    /**
     * Converter from the system unit to the unit of this quantity.
     */
    @SuppressWarnings("serial")             // Most SIS implementations are serializable.
    private final UnitConverter fromSystem;

    /**
     * Creates a new scalar for the given value.
     *
     * @param toSystem  converter from {@code unit} to the system unit.
     */
    DerivedScalar(final double value, final Unit<Q> unit, final Unit<Q> systemUnit, final UnitConverter toSystem) {
        super(toSystem.convert(value), systemUnit);
        derivedValue = value;
        derivedUnit  = unit;
        fromSystem   = toSystem.inverse();
    }

    /**
     * Creates a new scalar resulting from an arithmetic operation performed on the given scalar.
     * The arithmetic operation result is in the same unit as the original scalar.
     *
     * @param  value  the arithmetic result in system unit.
     */
    DerivedScalar(final DerivedScalar<Q> origin, final double value) {
        super(value, origin.getSystemUnit());
        derivedUnit  = origin.derivedUnit;
        fromSystem   = origin.fromSystem;
        derivedValue = fromSystem.convert(value);
    }

    /**
     * Creates a new quantity of same type as this quantity but with a different value.
     * The unit of measurement shall be the same as the system unit of this quantity.
     * Implementation in subclasses should be like below:
     *
     * {@snippet lang="java" :
     *     assert newUnit == getSystemUnit() : newUnit;
     *     return new MyDerivedScalar(this, newValue);
     *     }
     */
    @Override
    Quantity<Q> create(double newValue, Unit<Q> newUnit) {
        assert newUnit == getSystemUnit() : newUnit;
        return new DerivedScalar<>(this, newValue);
    }

    /**
     * Returns the system unit of measurement.
     */
    final Unit<Q> getSystemUnit() {
        return super.getUnit();
    }

    /**
     * Returns the unit of measurement specified at construction time.
     */
    @Override
    public final Unit<Q> getUnit() {
        return derivedUnit;
    }

    /**
     * Returns the value specified at construction time.
     */
    @Override
    public final double doubleValue() {
        return derivedValue;
    }

    /**
     * Returns the value cast to a single-precision floating point number.
     */
    @Override
    public final float floatValue() {
        return (float) derivedValue;
    }

    /**
     * Returns the value rounded to nearest integer. {@link Double#NaN} are cast to 0 and values out of
     * {@code long} range are clamped to minimal or maximal representable numbers of {@code long} type.
     */
    @Override
    public final long longValue() {
        return Math.round(derivedValue);
    }

    /**
     * Converts this quantity to another unit of measurement.
     */
    @Override
    public final Quantity<Q> to(final Unit<Q> newUnit) {
        if (newUnit == derivedUnit) {
            return this;
        }
        ArgumentChecks.ensureNonNull("unit", newUnit);      // "unit" is the parameter name used in public API.
        /*
         * Do not invoke `this.create(double, Unit)` because the contract in this subclass
         * restricts the above method to cases where the given unit is the system unit.
         * Furthermore, we need to let `Quantities.create(…)` re-evaluate whether we need
         * a `DerivedScalar` instance or whether `Scalar` would be sufficient.
         */
        return Quantities.create(derivedUnit.getConverterTo(newUnit).convert(derivedValue), newUnit);
    }


    /**
     * A temperature in Celsius degrees or any other units having an offset compared to Kelvin.
     */
    static final class TemperatureMeasurement extends DerivedScalar<javax.measure.quantity.Temperature>
            implements javax.measure.quantity.Temperature
    {
        private static final long serialVersionUID = -3901877967613695897L;

        /** Constructor for {@link Quantities} factory only. */
        TemperatureMeasurement(double value, Unit<javax.measure.quantity.Temperature> unit,
                    Unit<javax.measure.quantity.Temperature> systemUnit, UnitConverter toSystem)
        {
            super(value, unit, systemUnit, toSystem);
        }

        /** Constructor for {@code create(…)} implementation only. */
        private TemperatureMeasurement(TemperatureMeasurement origin, double value) {
            super(origin, value);
        }

        @Override
        Quantity<javax.measure.quantity.Temperature> create(double newValue, Unit<javax.measure.quantity.Temperature> newUnit) {
            assert newUnit == getSystemUnit() : newUnit;
            return new TemperatureMeasurement(this, newValue);
        }
    }

    /**
     * Fallback used when no {@link DerivedScalar} implementation is available for a given quantity type.
     * This is basically a copy of {@link ScalarFallback} implementation adapted to {@code DerivedScalar}.
     *
     * @param <Q>  the type of quantity implemented by this scalar.
     */
    @SuppressWarnings("serial")
    static final class Fallback<Q extends Quantity<Q>> extends DerivedScalar<Q> implements InvocationHandler {
        /**
         * The type implemented by proxy instances.
         *
         * @see ScalarFallback#type
         */
        private final Class<Q> type;

        /**
         * Constructor for {@link Quantities} factory only.
         */
        private Fallback(final double value, final Unit<Q> unit, final Unit<Q> systemUnit,
                         final UnitConverter toSystem, final Class<Q> type)
        {
            super(value, unit, systemUnit, toSystem);
            this.type = type;
        }

        /**
         * Constructor for {@code create(…)} implementation only.
         */
        private Fallback(final Fallback<Q> origin, final double value) {
            super(origin, value);
            type = origin.type;
        }

        /**
         * Creates a new quantity of the same type as this quantity but a different value and/or unit.
         *
         * @see ScalarFallback#create(double, Unit)
         */
        @Override
        @SuppressWarnings("unchecked")
        Quantity<Q> create(final double newValue, final Unit<Q> newUnit) {
            assert newUnit == getSystemUnit() : newUnit;
            final Fallback<Q> quantity = new Fallback<>(this, newValue);
            return (Q) Proxy.newProxyInstance(Scalar.class.getClassLoader(), new Class<?>[] {type}, quantity);
        }

        /**
         * Creates a new {@link Fallback} instance implementing the given quantity type.
         *
         * @see ScalarFallback#factory(double, Unit, Class)
         */
        @SuppressWarnings("unchecked")
        static <Q extends Quantity<Q>> Q factory(final double value, final Unit<Q> unit,
                final Unit<Q> systemUnit, final UnitConverter toSystem, final Class<Q> type)
        {
            final Fallback<Q> quantity = new Fallback<>(value, unit, systemUnit, toSystem, type);
            return (Q) Proxy.newProxyInstance(Scalar.class.getClassLoader(), new Class<?>[] {type}, quantity);
        }

        /**
         * Invoked when a method of the {@link Quantity} interface is invoked.
         *
         * @see ScalarFallback#invoke(Object, Method, Object[])
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws ReflectiveOperationException {
            return method.invoke(this, args);
        }
    }
}
