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

import java.util.Map;
import java.io.Serializable;
import java.io.ObjectStreamException;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.Dimension;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.internal.converter.SurjectiveConverter;


/**
 * Implementation of base, alternate and derived units (see {@link AbstractUnit} for a description of unit kinds).
 * A {@code SystemUnit} is a base or alternate unit if associated to a base {@link UnitDimension}, or is a derived
 * units otherwise. No other type is allowed since {@code SystemUnit} is always a combination of fundamental units
 * without scale factor or offset.
 *
 * @param  <Q>  the kind of quantity to be measured using this units.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
abstract class SystemUnit<Q extends Quantity<Q>> extends AbstractUnit<Q> {
    /**
     * The type of quantity that uses this unit.
     */
    final Class<Quantity<Q>> quantity;

    /**
     * The dimension of this unit of measurement.
     */
    final UnitDimension dimension;

    /**
     * Creates a new unit having the given symbol and EPSG code.
     *
     * @param  dimension  the unit dimension.
     * @param  name       the unit name,   or {@code null} if this unit has no specific name.
     * @param  symbol     the unit symbol, or {@code null} if this unit has no specific symbol.
     * @param  epsg       the EPSG code,   or 0 if this unit has no EPSG code.
     */
    SystemUnit(final Class<Quantity<Q>> quantity, final UnitDimension dimension,
            final String name, final String symbol, final short epsg)
    {
        super(name, symbol, epsg);
        this.quantity  = quantity;
        this.dimension = dimension;
    }

    /**
     * Returns the dimension of this unit.
     * Two units {@code u1} and {@code u2} are {@linkplain #isCompatible(Unit) compatible}
     * if and only if {@code u1.getDimension().equals(u2.getDimension())}.
     *
     * @return the dimension of this unit.
     *
     * @see #isCompatible(Unit)
     */
    @Override
    public Dimension getDimension() {
        return dimension;
    }

    /**
     * Returns the unscaled system unit from which this unit is derived.
     * Since this unit is already a base, alternate or derived unit, this method returns {@code true}.
     *
     * @return {@code this}
     */
    @Override
    public SystemUnit<Q> getSystemUnit() {
        return this;
    }

    /**
     * Returns the base units and their exponent whose product is this unit,
     * or {@code null} if this unit is a base unit (not a product of existing units).
     *
     * @return the base units and their exponent making up this unit.
     */
    @Override
    public Map<SystemUnit<?>, Integer> getBaseUnits() {
        final Map<UnitDimension,Integer> dim = dimension.getBaseDimensions();
        if (dim == null) {
            return null;            // This unit is associated to a base dimension.
        }
        return ObjectConverters.derivedKeys(dim, DimToUnit.INSTANCE, Integer.class);
    }

    /**
     * The converter for replacing the keys in the {@link SystemUnit#getBaseUnits()} map from {@link UnitDimension}
     * instances to {@link SystemUnit} instances. We apply conversions on the fly instead than extracting the data in
     * a new map once for all because the copy may fail if an entry contains a rational instead than an integer power.
     * With on-the-fly conversions, the operation will not fail if the user never ask for that particular value.
     */
    private static final class DimToUnit extends SurjectiveConverter<UnitDimension, SystemUnit<?>> implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 7545067577687885675L;

        /**
         * The unique instance used by {@link SystemUnit#getBaseUnits()}.
         */
        static final DimToUnit INSTANCE = new DimToUnit();

        /**
         * Constructor for the singleton {@link #INSTANCE}.
         */
        private DimToUnit() {
        }

        /**
         * Returns the type of key values in the map returned by {@link UnitDimension#getBaseDimensions()}.
         */
        @Override
        public Class<UnitDimension> getSourceClass() {
            return UnitDimension.class;
        }

        /**
         * Returns the type of key values in the map to be returned by {@link SystemUnit#getBaseUnits()}.
         */
        @Override
        @SuppressWarnings("unchecked")
        public Class<SystemUnit<?>> getTargetClass() {
            return (Class) SystemUnit.class;
        }

        /**
         * Returns the unit associated to the given dimension, or {@code null} if none.
         */
        @Override
        public SystemUnit<?> apply(final UnitDimension dim) {
            return Units.get(dim);
        }

        /**
         * Invoked on deserialization for replacing the deserialized instance by the unique instance.
         */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
