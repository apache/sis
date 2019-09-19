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
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.Dimension;
import javax.measure.UnitConverter;
import javax.measure.UnconvertibleException;
import javax.measure.IncommensurableException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Characters;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.math.Fraction;


/**
 * A unit of measure which is related to a base or derived unit through a conversion formula.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
final class ConventionalUnit<Q extends Quantity<Q>> extends AbstractUnit<Q> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6963634855104019466L;

    /**
     * The base, derived or alternate units to which this {@code ConventionalUnit} is related.
     * This is called "preferred unit" in GML. This is usually an instance of {@link SystemUnit},
     * but may also be another {@link ConventionalUnit} in some rare cases where this conventional
     * unit can be prefixed like a SI units (e.g. litre: L, cl, mL, µL).
     */
    private final AbstractUnit<Q> target;

    /**
     * The conversion from this unit to the {@linkplain #target} unit.
     */
    final UnitConverter toTarget;

    /**
     * Creates a new unit having the given symbol and EPSG code.
     *
     * @param  target    the base or derived units to which this {@code ConventionalUnit} is related.
     * @param  toTarget  the conversion from this unit to the {@code target} unit.
     * @param  symbol    the unit symbol, or {@code null} if this unit has no specific symbol.
     * @param  scope     {@link UnitRegistry#SI}, {@link UnitRegistry#ACCEPTED}, other constants or 0 if unknown.
     * @param  epsg      the EPSG code, or 0 if this unit has no EPSG code.
     */
    ConventionalUnit(final AbstractUnit<Q> target, final UnitConverter toTarget, final String symbol, final byte scope, final short epsg) {
        super(symbol, scope, epsg);
        this.target   = target;
        this.toTarget = toTarget;
    }

    /**
     * Creates a new unit with default name and symbol for the given converter.
     *
     * @param  target    the base or derived units to which the new unit will be related.
     * @param  toTarget  the conversion from the new unit to the {@code target} unit.
     */
    static <Q extends Quantity<Q>> AbstractUnit<Q> create(final AbstractUnit<Q> target, final UnitConverter toTarget) {
        if (toTarget.isIdentity()) {
            return target;
        }
        /*
         * Verifies if an instance already exists for the given converter.
         * The 'related' array is populated only by the Units class static initializer.
         * The SystemUnitTest.verifyRelatedUnits() method verified that the array does
         * not contain null element and that all 'toTarget' are instances of LinearConverter.
         */
        final ConventionalUnit<Q>[] related = target.related();
        if (related != null && toTarget instanceof LinearConverter) {
            final LinearConverter c = (LinearConverter) toTarget;
            for (final ConventionalUnit<Q> existing : related) {
                if (c.equivalent((LinearConverter) existing.toTarget)) {        // Cast is safe - see above comment.
                    return existing;
                }
            }
        }
        /*
         * If the unit is a SI unit, try to create the SI symbol by the concatenation of the SI prefix
         * with the system unit symbol. The unit symbol are used later as a key for searching existing
         * unit instances.
         */
        String symbol = null;
        if (target.isPrefixable()) {
            final String ts = target.getSymbol();
            if (ts != null && !ts.isEmpty()) {
                double scale = AbstractConverter.scale(toTarget);
                if (!Double.isNaN(scale)) {
                    final char prefix = Prefixes.symbol(scale, power(ts));
                    if (prefix != 0) {
                        symbol = Prefixes.concat(prefix, ts);
                    }
                }
            }
        }
        /*
         * Create the unit, but we may discard it later if an equivalent unit already exists in the cache.
         * The use of the cache is not only for sharing instances, but also because existing instances may
         * have more information.  For example instances provided by Units static constants may contain an
         * EPSG code, or even an alternative symbol (e.g. “hm²” will be replaced by “ha” for hectare).
         */
        ConventionalUnit<Q> unit = new ConventionalUnit<>(target, toTarget, symbol, (byte) 0, (short) 0);
        if (symbol != null) {
            unit = unit.unique(symbol);
        }
        return unit;
    }

    /**
     * Returns a unique instance of this unit if available, or store this unit in the map of existing unit otherwise.
     *
     * @param  symbol  the symbol of this unit, which must be non-null.
     */
    @SuppressWarnings("unchecked")
    final ConventionalUnit<Q> unique(final String symbol) {
        final Object existing = UnitRegistry.putIfAbsent(symbol, this);
        if (existing instanceof ConventionalUnit<?>) {
            final ConventionalUnit<?> c = (ConventionalUnit<?>) existing;
            if (target.equals(c.target)) {
                final boolean equivalent;
                if (toTarget instanceof LinearConverter && c.toTarget instanceof LinearConverter) {
                    equivalent = ((LinearConverter) toTarget).equivalent((LinearConverter) c.toTarget);
                } else {
                    equivalent = toTarget.equals(c.toTarget);   // Fallback for unknown implementations.
                }
                if (equivalent) {
                    return (ConventionalUnit<Q>) c;
                }
            }
        }
        return this;
    }

    /**
     * Raises the given symbol to the given power. If the given symbol already contains an exponent,
     * it will be combined with the given power.
     *
     * @param  symbol  the symbol to raise to a power.
     * @param  n       the power to which to raise the given symbol.
     * @param  root    {@code true} for raising to 1/n instead of n.
     */
    private static String pow(final String symbol, final int n, final boolean root) {
        if (symbol != null) {
            final int length = symbol.length();
            int power = 1, i = 0;
            while (i < length) {
                final int c = symbol.codePointAt(i);
                i += Character.charCount(c);
                if (!isSymbolChar(c)) {
                    if (!Characters.isSuperScript(c) || i + Character.charCount(c) < length) {
                        return null;                // Character is not an exponent or is not the last character.
                    }
                    power = Characters.toNormalScript(c) - '0';
                }
            }
            if (power >= 0 && power <= 9) {
                final boolean isValid;
                if (root) {
                    isValid = (power % n) == 0;
                    power /= n;
                } else {
                    power *= n;
                    isValid = (power >= 0 && power <= 9);
                }
                if (isValid) {
                    return symbol.substring(0, i) + Characters.toSuperScript((char) (power + '0'));
                }
            }
        }
        return null;
    }

    /**
     * Returns the positive power after the given unit symbol, or 0 in case of doubt.
     * For example this method returns 1 for “m” and 2 for “m²”. We parse the unit symbol instead
     * than the {@link SystemUnit#dimension} because we can not extract easily the power from the
     * product of dimensions (e.g. what is the M⋅L²∕T³ power?) Furthermore the power will be used
     * for choosing a symbol prefix, so we want it to be consistent with the symbol more than the
     * internal representation.
     *
     * <p>If the unit is itself a product of other units, then this method returns the power of
     * the first unit. For example the power of “m/s²” is 1. This means that the “k” prefix in
     * “km/s²” apply only to the “m” unit.</p>
     */
    static int power(final String symbol) {
        final int length = symbol.length();
        int i = 0, c;
        do {
            if (i >= length) return 1;              // Single symbol (no product, no exponent).
            c = symbol.codePointAt(i);
            i += Character.charCount(c);
        } while (isSymbolChar(c));
        /*
         * At this point we found the first character which is not part of a unit symbol.
         * We may have found the exponent as in “m²”, or we may have found an arithmetic
         * operator like the “/” in “m/s²”. In any cases we stop here because we want the
         * exponent of the first symbol, not the “²” in “m/s²”.
         */
        final int p = Characters.toNormalScript(c) - '0';
        if (p >= 0 && p <= 9) {
            if (i < length) {
                c = symbol.codePointAt(i);
                if (isSymbolChar(c)) {
                    // Exponent is immediately followed by a another unit symbol character.
                    // We would have expected something else, like an arithmetic operator.
                    return 0;
                }
                c = Characters.toNormalScript(c);
                if (c >= '0' && c <= '9') {
                    // Exponent on two digits. We do not expect so high power after unit symbol.
                    return 0;
                }
            }
            return p;
        }
        return 1;
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
        return target.getDimension();
    }

    /**
     * Returns the unscaled system unit from which this unit is derived.
     */
    @Override
    public SystemUnit<Q> getSystemUnit() {
        return target.getSystemUnit();
    }

    /**
     * Returns the base units and their exponent whose product is the system unit,
     * or {@code null} if the system unit is a base unit (not a product of existing units).
     *
     * @return the base units and their exponent making up the system unit.
     */
    @Override
    public Map<SystemUnit<?>, Integer> getBaseUnits() {
        return target.getBaseUnits();
    }

    /**
     * Returns the base units used by Apache SIS implementations.
     * Contrarily to {@link #getBaseUnits()}, this method never returns {@code null}.
     */
    @Override
    final Map<SystemUnit<?>, Fraction> getBaseSystemUnits() {
        return target.getBaseSystemUnits();
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
        final Unit<T> alternate = target.asType(type);
        if (target.equals(alternate)) {
            return (Unit<T>) this;
        }
        return alternate.transform(toTarget);
    }

    /**
     * Returns a converter of numeric values from this unit to another unit of same type.
     *
     * @param  that  the unit of same type to which to convert the numeric values.
     * @return the converter from this unit to {@code that} unit.
     * @throws UnconvertibleException if the converter can not be constructed.
     */
    @Override
    public UnitConverter getConverterTo(final Unit<Q> that) throws UnconvertibleException {
        if (that == this) {
            return IdentityConverter.INSTANCE;
        }
        ArgumentChecks.ensureNonNull("that", that);
        UnitConverter c = toTarget;
        if (target != that) {                           // Optimization for a common case.
            final Unit<Q> step = that.getSystemUnit();
            if (target != step && !target.isCompatible(step)) {
                // Should never occur unless parameterized type has been compromised.
                throw new UnconvertibleException(incompatible(that));
            }
            c = target.getConverterTo(step).concatenate(c);         // Usually leave 'c' unchanged.
            c =   step.getConverterTo(that).concatenate(c);
        }
        return c;
    }

    /**
     * Returns a converter from this unit to the specified unit of unknown type.
     * This method can be used when the quantity type of the specified unit is unknown at compile-time
     * or when dimensional analysis allows for conversion between units of different type.
     *
     * @param  that  the unit to which to convert the numeric values.
     * @return the converter from this unit to {@code that} unit.
     * @throws IncommensurableException if this unit is not {@linkplain #isCompatible(Unit) compatible} with {@code that} unit.
     *
     * @see #isCompatible(Unit)
     */
    @Override
    public UnitConverter getConverterToAny(final Unit<?> that) throws IncommensurableException {
        if (that == this) {
            return IdentityConverter.INSTANCE;
        }
        ArgumentChecks.ensureNonNull("that", that);
        UnitConverter c = toTarget;
        if (target != that) {                           // Optimization for a common case.
            final Unit<?> step = that.getSystemUnit();
            if (target != step && !target.isCompatible(step)) {
                throw new IncommensurableException(incompatible(that));
            }
            c = target.getConverterToAny(step).concatenate(c);      // Usually leave 'c' unchanged.
            c =   step.getConverterToAny(that).concatenate(c);
        }
        return c;
    }

    /**
     * Returns a new unit identical to this unit except for the symbol, which is set to the given value.
     * This is used by {@link UnitFormat} mostly; we do not provide public API for setting a unit symbol
     * on a conventional unit.
     */
    final ConventionalUnit<Q> forSymbol(final String symbol) {
        if (symbol.equals(getSymbol())) {
            return this;
        }
        return new ConventionalUnit<>(target, toTarget, symbol, scope, epsg);
    }

    /**
     * Unsupported operation for conventional units, as required by JSR-363 specification.
     *
     * @param  symbol  the new symbol for the alternate unit.
     * @return the alternate unit.
     * @throws UnsupportedOperationException always thrown since this unit is not an unscaled standard unit.
     *
     * @see SystemUnit#alternate(String)
     */
    @Override
    public Unit<Q> alternate(final String symbol) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.NonSystemUnit_1, this));
    }

    /**
     * Ensures that the scale of measurement of this units is a ratio scale.
     */
    private void ensureRatioScale() {
        if (!toTarget.isLinear()) {
            throw new IllegalStateException(Errors.format(Errors.Keys.NonRatioUnit_1, this));
        }
    }

    /**
     * Returns the product of this unit with the one specified.
     *
     * @param  multiplier  the unit multiplier.
     * @return {@code this} × {@code multiplier}
     */
    @Override
    public Unit<?> multiply(final Unit<?> multiplier) {
        if (multiplier == this) return pow(2);                      // For formating e.g. "mi²".
        ensureRatioScale();
        return inferSymbol(target.multiply(multiplier).transform(toTarget), MULTIPLY, multiplier);
    }

    /**
     * Returns the quotient of this unit with the one specified.
     *
     * @param  divisor  the unit divisor.
     * @return {@code this} ∕ {@code divisor}
     */
    @Override
    public Unit<?> divide(final Unit<?> divisor) {
        ensureRatioScale();
        return inferSymbol(target.divide(divisor).transform(toTarget), DIVIDE, divisor);
    }

    /**
     * Returns a unit equals to this unit raised to an exponent.
     *
     * @param  n  the exponent.
     * @return the result of raising this unit to the exponent.
     */
    @Override
    public Unit<?> pow(final int n) {
        ensureRatioScale();
        return inferSymbol(applyConversion(target.pow(n), n, false), 'ⁿ', null);
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
        ensureRatioScale();
        return applyConversion(target.root(n), n, true);
    }

    /**
     * Applies the {@link #toTarget} conversion factor on the result of raising the system unit to the given power.
     * This method shall be invoked only if {@link #ensureRatioScale()} succeed (this is not verified).
     * This method tries to build a unit symbol made from the current unit raised to the given power.
     * This is not needed for SI units since {@link #create(AbstractUnit, UnitConverter)} can infer
     * the symbol automatically (including its prefix), but this is useful for non SI units like "mi²"
     *
     * @param  result  the result of {@link SystemUnit#pow(int)} or {@link SystemUnit#root(int)}.
     * @param  n       the power by which the {@link #target} has been raised for producing {@code result}.
     * @param  root    {@code true} if the power is 1/n instead of n.
     */
    private Unit<?> applyConversion(final Unit<?> result, final int n, final boolean root) {
        if (result == target) return this;
        final LinearConverter operation = LinearConverter.pow(toTarget, n, root);
        if (result instanceof SystemUnit<?>) {
            final String symbol = pow(getSymbol(), n, root);
            if (symbol != null) {
                return new ConventionalUnit<>((SystemUnit<?>) result, operation, symbol, (byte) 0, (short) 0).unique(symbol);
            }
        }
        return result.transform(operation);
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
        if (!isPrefixable()) {
            base = target;
            operation = toTarget.concatenate(operation);
        }
        return create(base, operation);
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
            final ConventionalUnit<?> that = (ConventionalUnit<?>) other;
            return Utilities.deepEquals(target,   that.target,   mode) &&
                   Utilities.deepEquals(toTarget, that.toTarget, mode);
        }
        return false;
    }

    /**
     * Returns a hash code value for this unit.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 37 * (target.hashCode() + 31 * toTarget.hashCode());
    }
}
