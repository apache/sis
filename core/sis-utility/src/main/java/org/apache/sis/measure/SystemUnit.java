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
import java.util.Objects;
import java.io.Serializable;
import java.io.ObjectStreamException;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.Dimension;
import javax.measure.UnitConverter;
import javax.measure.UnconvertibleException;
import javax.measure.IncommensurableException;
import javax.measure.spi.QuantityFactory;
import org.apache.sis.util.Characters;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.converter.SurjectiveConverter;
import org.apache.sis.math.Fraction;


/**
 * Implementation of base, alternate and derived units (see {@link AbstractUnit} for a description of unit kinds).
 * A {@code SystemUnit} is a base or alternate unit if associated to a base {@link UnitDimension}, or is a derived
 * units otherwise. No other type is allowed since {@code SystemUnit} is always a combination of fundamental units
 * without scale factor or offset.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @version 1.1
 *
 * @param <Q>  the kind of quantity to be measured using this units.
 *
 * @since 0.8
 * @module
 */
final class SystemUnit<Q extends Quantity<Q>> extends AbstractUnit<Q> implements QuantityFactory<Q> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4097466138698631914L;

    /**
     * The non-empty symbol for {@link Units#UNITY}.
     */
    static final String ONE = "1";

    /**
     * The type of quantity that uses this unit, or {@code null} if unknown.
     * This field should be null only when this unit is the result of an arithmetic
     * operation and that result can not be mapped to a known {@link Quantity} subtype.
     */
    final Class<Q> quantity;

    /**
     * The dimension of this unit of measurement. Can not be null.
     */
    final UnitDimension dimension;

    /**
     * The factory to use for creating quantities, or {@code null} if none.
     * This field does not need to be serialized because {@link AbstractUnit#readResolve()}
     * replaces deserialized instances by corresponding {@link Units} hard-coded instances.
     */
    final transient ScalarFactory<Q> factory;

    /**
     * Units for the same quantity but with scale factors that are not the SI one.
     * This is initialized by {@link Units} only and shall not change anymore after.
     * All units in this array shall use an instance of {@link LinearConverter}.
     *
     * @see #related(int)
     */
    private transient ConventionalUnit<Q>[] related;

    /**
     * Creates a new unit having the given symbol and EPSG code.
     *
     * @param  quantity   the type of quantity that uses this unit, or {@code null} if unknown.
     * @param  dimension  the unit dimension.
     * @param  symbol     the unit symbol, or {@code null} if this unit has no specific symbol.
     * @param  scope      {@link UnitRegistry#SI}, {@link UnitRegistry#ACCEPTED}, other constants or 0 if unknown.
     * @param  epsg       the EPSG code, or 0 if this unit has no EPSG code.
     * @param  factory    the factory to use for creating quantities, or {@code null} if none.
     */
    SystemUnit(final Class<Q> quantity, final UnitDimension dimension, final String symbol,
            final byte scope, final short epsg, final ScalarFactory<Q> factory)
    {
        super(symbol, scope, epsg);
        this.quantity  = quantity;
        this.dimension = dimension;
        this.factory   = factory;
    }

    /**
     * Returns a unit of the given dimension with default name and symbol.
     * This method is invoked for creating the result of arithmetic operations.
     * If there is no predefined unit for the given dimension, then the new unit may be allocated a symbol derived from
     * this unit's symbol. A new symbol is created only if this unit symbol and the {@code other} unit symbol are simple
     * (for example "m" but not "m²", or "N" but not "N/m").
     *
     * @param  operation  symbol to write after the symbol of this unit for generating the new unit symbol, or 0
     *                    for not inferring new symbol. Ignored if the condition documented in javadoc does not hold.
     * @param  other      other units to append after the operation symbol, or {@code null} if none or should be ignored.
     *                    Ignored if the condition documented in javadoc does not hold.
     */
    private SystemUnit<?> create(final UnitDimension newDimension, final char operation, final Unit<?> other) {
        /*
         * Check if we are computing the inverse of the other unit. We are computing inverse
         * if this unit is unity ("1") and is used as the numerator of a division. The unity
         * symbol is an empty string but we will format it as "1".
         */
        String ts = getSymbol();
        final boolean inverse = (ts != null && ts.isEmpty() && operation == DIVIDE);
        /*
         * Check if the SystemUnit to create is known to Units, provided that no dimensionless units
         * is involved. If a dimensionless unit is involved, we will try to build a symbol before to
         * check if the unit is known to Units. The reason is that there is many dimensionless units
         * with different symbols (rad, sr, psu, …).
         */
        final boolean deferred = newDimension.isDimensionless() || (!inverse && dimension.isDimensionless()) ||
                (other != null && UnitDimension.isDimensionless(other.getDimension()));
        if (!deferred) {
            final SystemUnit<?> result = Units.get(newDimension);
            if (result != null) return result;
        }
        /*
         * Try to create a unit symbol as the concatenation of the symbols of the two units,
         * with the operation symbol between them. If we can not, `symbol` will stay null.
         */
        String symbol = null;
        if (operation != 0) {
            symbol = inferSymbol(operation, other);
        }
        /*
         * Performs now the check that we did not performed at the
         * beginning of this method if any component was unitless.
         * The difference is that we take unit symbol in account.
         */
        if (deferred) {
            final SystemUnit<?> result = Units.get(newDimension);
            if (result != null && result.sameSymbol(symbol)) {
                return result;
            }
        }
        if (newDimension == dimension && sameSymbol(symbol)) {
            return this;
        }
        return new SystemUnit<>(null, newDimension, symbol, (byte) 0, (short) 0, null);
    }

    /**
     * Returns {@code true} if the given symbol is null or equals to the symbol of this unit.
     */
    private boolean sameSymbol(final String symbol) {
        return (symbol == null) || symbol.equals(getSymbol());
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
     * Returns the base units used by Apache SIS implementations.
     * Contrarily to {@link #getBaseUnits()}, this method never returns {@code null}.
     */
    @Override
    final Map<SystemUnit<?>, Fraction> getBaseSystemUnits() {
        return ObjectConverters.derivedKeys(dimension.components, DimToUnit.INSTANCE, Fraction.class);
    }

    /**
     * The converter for replacing the keys in the {@link SystemUnit#getBaseUnits()} map from {@link UnitDimension}
     * instances to {@link SystemUnit} instances. We apply conversions on the fly instead of extracting the data in
     * a new map once for all because the copy may fail if an entry contains a rational instead of an integer power.
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

    /**
     * Casts this unit to a parameterized unit of specified nature or throw a {@code ClassCastException}
     * if the dimension of the specified quantity and this unit's dimension do not match.
     *
     * @param  <T>   the type of the quantity measured by the unit.
     * @param  type  the quantity class identifying the nature of the unit.
     * @return this unit parameterized with the specified type.
     * @throws ClassCastException if the dimension of this unit is different from the specified quantity dimension.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Quantity<T>> Unit<T> asType(final Class<T> type) throws ClassCastException {
        ArgumentChecks.ensureNonNull("type", type);
        if (type == quantity) {
            if (getSymbol() == null) {
                // If this unit has no symbol, opportunistically supply a symbol if we find it.
                final SystemUnit<T> unit = Units.get(type);
                if (unit != null) {
                    return unit;
                }
            }
            return (Unit<T>) this;
        }
        /*
         * Verifies what are the expected dimensions of the given type by searching for the corresponding unit.
         * If we find that unit, returns it on the assumption that its symbol is right while the symbol of this
         * unit may no longer be right for the given type.  If we can not find a pre-defined units, then create
         * a new unit with the requested type but no symbol since we do not know yet what the symbol should be
         * for the new quantity.
         */
        SystemUnit<T> unit = Units.get(type);
        if (unit == null) {
            unit = new SystemUnit<>(type, dimension, null, (byte) 0, (short) 0, null);  // Intentionally no symbol.
        }
        if (!dimension.equals(unit.dimension)) {
            throw new ClassCastException(Errors.format(Errors.Keys.IncompatibleUnitDimension_5, new Object[] {
                    this, (quantity != null) ? quantity.getSimpleName() : "?", dimension,
                    type.getSimpleName(), unit.dimension}));
        }
        return unit;
    }

    /**
     * Returns {@code true} if this unit is equal to the given unit ignoring name, symbol and EPSG code.
     * This method should always returns {@code true} if parameterized type has not been compromised with
     * raw types or unchecked casts.
     *
     * @param  other  the other unit, which must be a system unit.
     */
    final boolean equalsIgnoreMetadata(final Unit<Q> other) {
        if (quantity != null && other instanceof SystemUnit<?>) {
            /*
             * For SIS implementation, we just need to compare the quantity class, if known.
             * Two units for the same quantity implies that they are also for the same dimension.
             */
            final Class<?> c = ((SystemUnit<Q>) other).quantity;
            if (c != null) {
                return (quantity == c);
            }
        }
        /*
         * For foreigner implementations, comparing the units dimension is better than nothing.
         * But this check is not as reliable as comparing the quantity classes.
         */
        assert other == other.getSystemUnit() : other;
        return dimension.equals(other.getDimension());
    }

    /**
     * Returns a converter of numeric values from this unit to another unit of same type.
     *
     * @param  unit  the unit of same type to which to convert the numeric values.
     * @return the converter from this unit to {@code that} unit.
     * @throws UnconvertibleException if the converter can not be constructed.
     */
    @Override
    public UnitConverter getConverterTo(final Unit<Q> unit) throws UnconvertibleException {
        ArgumentChecks.ensureNonNull("unit", unit);
        final Unit<Q> step = unit.getSystemUnit();
        if (step != this && !equalsIgnoreMetadata(step)) {
            // Should never occur unless parameterized type has been compromised.
            throw new UnconvertibleException(incompatible(unit));
        }
        if (step == unit) {
            return IdentityConverter.INSTANCE;
        }
        /*
         * At this point we know that the given units is not a system unit. Ask the conversion
         * FROM the given units (before to inverse it) instead of TO the given units because
         * in Apache SIS implementation, the former returns directly ConventionalUnit.toTarget
         * while the latter implies a recursive call to this method.
         */
        return unit.getConverterTo(step).inverse();
    }

    /**
     * Returns a converter from this unit to the specified unit of unknown type.
     * This method can be used when the quantity type of the specified unit is unknown at compile-time
     * or when dimensional analysis allows for conversion between units of different type.
     *
     * @param  unit  the unit to which to convert the numeric values.
     * @return the converter from this unit to {@code that} unit.
     * @throws IncommensurableException if this unit is not {@linkplain #isCompatible(Unit) compatible} with {@code that} unit.
     *
     * @see #isCompatible(Unit)
     */
    @Override
    public UnitConverter getConverterToAny(final Unit<?> unit) throws IncommensurableException {
        ArgumentChecks.ensureNonNull("unit", unit);
        final Unit<?> step = unit.getSystemUnit();
        if (step != this && !isCompatible(step)) {
            throw new IncommensurableException(incompatible(unit));
        }
        if (step == unit) {
            return IdentityConverter.INSTANCE;
        }
        // Same remark than in getConverterTo(Unit).
        return unit.getConverterToAny(step).inverse();
    }

    /**
     * Returns a system unit equivalent to this unscaled standard unit but used in expressions
     * to distinguish between quantities of a different nature but of the same dimensions.
     *
     * <p>The most important alternate unit in Apache SIS is {@link Units#RADIAN}, defined as below:</p>
     *
     * {@preformat java
     *   Unit<Angle> RADIAN = ONE.alternate("rad").asType(Angle.class);
     * }
     *
     * @param  symbol  the new symbol for the alternate unit.
     * @return the alternate unit.
     * @throws IllegalArgumentException if the specified symbol is already associated to a different unit.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Unit<Q> alternate(final String symbol) {
        ArgumentChecks.ensureNonEmpty("symbol", symbol);
        final int c = invalidCharForSymbol(symbol, 0, false);
        if (c >= 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalCharacter_2,
                    "symbol", String.valueOf(Character.toChars(c))));
        }
        if (symbol.equals(getSymbol())) {
            return this;
        }
        final SystemUnit<Q> alt = new SystemUnit<>(quantity, dimension, symbol, (byte) 0, (short) 0, factory);
        if (quantity != null) {
            /*
             * Use the cache only if this unit has a non-null quantity type. Do not use the cache even
             * in read-only mode when `quantity` is null because we would be unable to guarantee that
             * the parameterized type <Q> is correct.
             */
            final Object existing = UnitRegistry.putIfAbsent(symbol, alt);
            if (existing != null) {
                if (existing instanceof SystemUnit<?>) {
                    final SystemUnit<?> unit = (SystemUnit<?>) existing;
                    if (quantity.equals(unit.quantity) && dimension.equals(unit.dimension)) {
                        return (SystemUnit<Q>) unit;
                    }
                }
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, symbol));
            }
            /*
             * This method may be invoked for a new quantity, after a call to `asType(Class)`.
             * Try to register the new unit for that Quantity. But if another unit is already
             * registered for that Quantity, this is not necessarily an error.
             */
            UnitRegistry.putIfAbsent(quantity, alt);
        }
        return alt;
    }

    /**
     * Returns the product of this unit with the one specified.
     *
     * @param  multiplier  the unit multiplier.
     * @return {@code this} × {@code multiplier}
     */
    @Override
    public Unit<?> multiply(final Unit<?> multiplier) {
        ArgumentChecks.ensureNonNull("multiplier", multiplier);
        if (multiplier == this) return pow(2);                      // For formating e.g. "K²" instead of "K⋅K".
        return product(multiplier, false);
    }

    /**
     * Returns the quotient of this unit with the one specified.
     *
     * @param  divisor  the unit divisor.
     * @return {@code this} ∕ {@code divisor}
     */
    @Override
    public Unit<?> divide(final Unit<?> divisor) {
        ArgumentChecks.ensureNonNull("divisor", divisor);
        return product(divisor, true);
    }

    /**
     * Implementation of {@link #multiply(Unit)} and {@link #divide(Unit)} methods.
     *
     * @param  inverse  wether to use the inverse of {@code other}.
     */
    private <T extends Quantity<T>> Unit<?> product(final Unit<T> other, final boolean inverse) {
        final Unit<T> intermediate = other.getSystemUnit();
        final Dimension dim = intermediate.getDimension();
        final UnitDimension newDimension;
        final char operation;
        if (inverse) {
            operation = DIVIDE;
            newDimension = dimension.divide(dim);
        } else {
            operation = MULTIPLY;
            newDimension = dimension.multiply(dim);
        }
        final boolean transformed = (intermediate != other);
        Unit<?> result = create(newDimension, operation, transformed ? null : other);
        if (transformed) {
            UnitConverter c = other.getConverterTo(intermediate);
            if (!c.isLinear()) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NonRatioUnit_1, other));
            }
            if (!c.isIdentity()) {
                if (inverse) c = c.inverse();
                result = result.transform(c);
                /*
                 * If the system unit product is an Apache SIS implementation, try to infer a unit symbol
                 * to be given to our customized `transform` method. Otherwise fallback on standard API.
                 */
                result = inferSymbol(result, operation, other);
            }
        }
        return result;
    }

    /**
     * Returns a unit equals to this unit raised to an exponent.
     *
     * @param  n  the exponent.
     * @return the result of raising this unit to the exponent.
     */
    @Override
    public Unit<?> pow(final int n) {
        switch (n) {
            case 0: return Units.UNITY;
            case 1: return this;
            default: {
                final char p = (n >= 0 && n <= 9) ? Characters.toSuperScript((char) ('0' + n)) : 0;
                return create(dimension.pow(n), p, null);
            }
        }
    }

    /**
     * Returns a unit equals to the given root of this unit.
     *
     * @param  n  the root's order.
     * @return the result of taking the given root of this unit.
     * @throws ArithmeticException if {@code n == 0}.
     */
    @Override
    public Unit<?> root(final int n) {
        return create(dimension.root(n), (char) 0, null);
    }

    /**
     * Returns the unit derived from this unit using the specified converter.
     *
     * @param  operation  the converter from the transformed unit to this unit.
     * @return the unit after the specified transformation.
     */
    @Override
    public Unit<Q> transform(UnitConverter operation) {
        ArgumentChecks.ensureNonNull("operation", operation);
        AbstractUnit<Q> base = this;
        final ConventionalUnit<Q> pseudo = Prefixes.pseudoSystemUnit(this);
        if (pseudo != null) {
            /*
             * Special case for Units.KILOGRAM, to be replaced by Units.GRAM so a prefix can be computed.
             * The kilogram may appear in an expression like "kg/m", which we want to replace by "g/m".
             *
             * Note: we could argue that this block should be UnitFormat's work rather than SystemUnit.
             * We perform this work here because this unit symbol may be different than what UnitFormat
             * would infer, for example because of symbols recorded by sequence of Unit.multiply(Unit)
             * and Unit.divide(Unit) operations.
             */
            operation = operation.concatenate(pseudo.toTarget.inverse());
            base = pseudo;
        }
        return ConventionalUnit.create(base, operation);
    }

    /**
     * Invoked by {@link Units} initializer before to fill the {@link #related} array.
     * We define this method only for isolating the generic array creation.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    final void related(final int n) {
        if (related != null) {
            throw new IllegalStateException();
        }
        related = new ConventionalUnit[n];
    }

    /**
     * Returns units for the same quantity but with scale factors that are not the SI one, or {@code null} if none.
     * This method returns a direct reference to the internal field; caller shall not modify.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final ConventionalUnit<Q>[] related() {
        return related;
    }

    /**
     * Compares this unit with the given object for equality,
     * optionally ignoring metadata and rounding errors.
     *
     * @param  other  the other object to compare with this unit, or {@code null}.
     * @return {@code true} if the given object is equal to this unit.
     */
    @Override
    public boolean equals(final Object other, final ComparisonMode mode) {
        if (other == this) {
            return true;
        }
        if (super.equals(other, mode)) {
            final SystemUnit<?> that = (SystemUnit<?>) other;
            return Objects.equals(quantity, that.quantity) && dimension.equals(that.dimension);
        }
        return false;
    }

    /**
     * Returns a hash code value for this unit.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 37 * dimension.hashCode();
    }

    /**
     * Creates a quantity for the given value and unit of measurement.
     */
    @Override
    public Quantity<Q> create(final Number value, final Unit<Q> unit) {
        ArgumentChecks.ensureNonNull("value", value);
        ArgumentChecks.ensureNonNull("unit", unit);
        final double v = AbstractConverter.doubleValue(value);
        if (factory != null) {
            return factory.create(v, unit);
        } else if (quantity != null) {
            return ScalarFallback.factory(v, unit, quantity);
        } else {
            return new Scalar<>(v, unit);
        }
    }
}
