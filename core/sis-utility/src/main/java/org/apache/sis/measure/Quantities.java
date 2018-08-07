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

import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.UnitConverter;
import javax.measure.quantity.Time;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.format.ParserException;
import org.apache.sis.util.Static;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Provides static methods working on {@link Quantity} instances.
 * Apache SIS implementation of quantities has the following characteristics:
 *
 * <ul>
 *   <li>Values are stored with {@code double} precision.</li>
 *   <li>All quantities implement the specific subtype (e.g. {@link Length} instead of {@code Quantity<Length>}).</li>
 *   <li>Quantities are immutable, {@link Comparable} and {@link java.io.Serializable}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public final class Quantities extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Quantities() {
    }

    /**
     * Creates a quantity for the given value and unit of measurement symbol.
     * This is a convenience method that combines a call to {@link Units#valueOf(String)}
     * with {@link #create(double, Unit)}.
     *
     * @param  value  the quantity magnitude.
     * @param  unit   symbol of the unit of measurement associated to the given value.
     * @return a quantity of the given type for the given value and unit of measurement.
     * @throws ParserException if the given symbol can not be parsed.
     */
    public static Quantity<?> create(final double value, final String unit) {
        return create(value, Units.valueOf(unit));
    }

    /**
     * Creates a quantity for the given value and unit of measurement.
     *
     * @param  <Q>    the quantity type (e.g. {@link Length}, {@link Angle}, {@link Time}, <i>etc.</i>).
     * @param  value  the quantity magnitude.
     * @param  unit   the unit of measurement associated to the given value.
     * @return a quantity of the given type for the given value and unit of measurement.
     * @throws IllegalArgumentException if the given unit class is not a supported implementation.
     *
     * @see UnitServices#getQuantityFactory(Class)
     */
    public static <Q extends Quantity<Q>> Q create(final double value, final Unit<Q> unit) {
        ArgumentChecks.ensureNonNull("unit", unit);
        final Unit<Q> system = unit.getSystemUnit();
        if (system instanceof SystemUnit<?>) {
            final UnitConverter c = unit.getConverterTo(system);
            final ScalarFactory<Q> factory = ((SystemUnit<Q>) system).factory;
            if (c.isLinear()) {
                /*
                 * We require arithmetic operations (A + B, A * 2, etc.) to be performed as if all values were
                 * converted to system unit before calculation. This is mandatory for preserving arithmetic laws
                 * like associativity, commutativity, etc.  But in the special case were the unit of measurement
                 * if related to the system unit with only a scale factor (no offset), we get equivalent results
                 * even if we skip the conversion to system unit.  Since the vast majority of units fall in this
                 * category, it is worth to do this optimization.
                 *
                 * (Note: despite its name, above 'isLinear()' method actually has an 'isScale()' behavior).
                 */
                if (factory != null) {
                    return factory.create(value, unit);
                } else {
                    return ScalarFallback.factory(value, unit, ((SystemUnit<Q>) system).quantity);
                }
            }
            /*
             * If the given unit of measurement is derived from the system unit by a more complex formula
             * than a scale factor, then we need to perform arithmetic operations using the full path
             * (convert all values to system unit before calculation).
             */
            if (factory != null) {
                final Q quantity = factory.createDerived(value, unit, system, c);
                if (quantity != null) {
                    return quantity;
                }
            }
            return DerivedScalar.Fallback.factory(value, unit, system, c, ((SystemUnit<Q>) system).quantity);
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedImplementation_1, unit.getClass()));
        }
    }

    /**
     * Returns the given quantity as an instance of the specific {@code Quantity} subtype.
     * For example this method can be used for converting a {@code Quantity<Length>} to a {@link Length}.
     * If the given quantity already implements the specific interface, then it is returned as-is.
     *
     * @param  <Q>      the quantity type (e.g. {@link Length}, {@link Angle}, {@link Time}, <i>etc.</i>), or {@code null}.
     * @param  quantity the quantity to convert to the specific subtype.
     * @return the given quantity as a specific subtype (may be {@code quantity} itself), or {@code null} if the given quantity was null.
     * @throws IllegalArgumentException if the unit class associated to the given quantity is not a supported implementation.
     */
    @SuppressWarnings("unchecked")
    public static <Q extends Quantity<Q>> Q castOrCopy(final Quantity<Q> quantity) {
        if (quantity != null) {
            final Unit<Q> unit   = quantity.getUnit();
            final Unit<Q> system = unit.getSystemUnit();
            if (!(system instanceof SystemUnit<?>)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedImplementation_1, unit.getClass()));
            }
            final Class<Q> type = ((SystemUnit<Q>) system).quantity;
            if (!type.isInstance(quantity)) {
                final ScalarFactory<Q> factory = ((SystemUnit<Q>) system).factory;
                final double value = AbstractConverter.doubleValue(quantity.getValue());
                if (factory != null) {
                    return factory.create(value, unit);
                } else {
                    return ScalarFallback.factory(value, unit, type);
                }
            }
        }
        return (Q) quantity;
    }
}
