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
import javax.measure.Quantity;
import javax.measure.spi.QuantityFactory;


/**
 * Default factory when {@link SystemUnit} cannot be used directly.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @version 1.4
 * @since   1.4
 */
class DefaultQuantityFactory<Q extends Quantity<Q>> implements QuantityFactory<Q> {
    /**
     * Creates a new factory.
     */
    DefaultQuantityFactory() {
    }

    /**
     * Unconditionally returns {@code null} because this factory
     * should be used only when the type is not for a system unit.
     */
    @Override
    public final Unit<Q> getSystemUnit() {
        return null;
    }

    /**
     * Creates a quantity for the given number stated in the specified unit.
     *
     * @param  value  the numeric value stated in the specified unit.
     * @param  unit   the unit of the value.
     * @return the requested quantity.
     */
    @Override
    public Quantity<Q> create(final Number value, final Unit<Q> unit) {
        return new Scalar<>(AbstractConverter.doubleValue(value), unit);
    }

    /**
     * Creates a quantity for the given number stated in the specified unit and scale.
     *
     * @param  value  the numeric value stated in the specified unit.
     * @param  unit   the unit of the value.
     * @param  scale  the {@code ABSOLUTE} / {@code RELATIVE} scale of the quantity to create.
     * @return the requested quantity.
     */
    @Override
    public final Quantity<Q> create(final Number value, final Unit<Q> unit, final Quantity.Scale scale) {
        if (Objects.requireNonNull(scale) != Scalar.SCALE) {
            throw new UnsupportedOperationException("Relative scale is not yet supported.");
        }
        return create(value, unit);
    }
}
