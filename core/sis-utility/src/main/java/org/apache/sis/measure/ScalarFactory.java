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


/**
 * Creates quantities for the given value and unit of measurement.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @param <Q>  the type of quantities created by this factory.
 *
 * @since 0.8
 * @module
 */
interface ScalarFactory<Q extends Quantity<Q>> {
    /**
     * Creates a new quantity for the given value and unit of measurement.
     * This method is invoked only when the given unit is convertible to
     * system unit with a scale factor (no offset). This is the case of
     * most units of measurement.
     *
     * @param  value  the value of the quantity to create.
     * @param  unit   the unit of measurement associated to the given value.
     * @return a quantity with the given value and unit of measurement.
     */
    Q create(double value, Unit<Q> unit);

    /**
     * Creates a new quantity for the given value and unit of measurement
     * which is not convertible to system unit by a scale factor. Example
     * of such quantities is a temperature measurement in Celsius degrees,
     * since conversion to Kelvin implies an offset.
     *
     * @param  value       the value of the quantity to create.
     * @param  unit        the unit of measurement associated to the given value.
     * @param  systemUnit  {@link Unit#getSystemUnit()}, opportunistically provided because already known by the caller.
     * @param  toSystem    {@code unit.getConverterTo(systemUnit)}, provided because already known by the caller.
     * @return a quantity with the given value and unit of measurement, or
     *         {@code null} if no pre-defined implementation is available.
     */
    default Q createDerived(double value, Unit<Q> unit, Unit<Q> systemUnit, UnitConverter toSystem) {
        return null;
    }
}
