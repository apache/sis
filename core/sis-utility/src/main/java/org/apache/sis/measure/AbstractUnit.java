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

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.MissingResourceException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import javax.measure.Unit;
import javax.measure.Quantity;
import org.apache.sis.math.Fraction;
import org.apache.sis.util.Characters;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;


/**
 * Base class of all unit implementations. There is conceptually 4 kinds of units,
 * but some of them are implemented by the same class:
 *
 * <ul>
 *   <li><b>Base units</b> are the 6 or 7 SI units used as building blocks for all other units.
 *       The base units are metre, second, kilogram, Kelvin degrees, Ampere and Candela,
 *       sometime with the addition of mole.</li>
 *   <li><b>Derived units</b> are products of base units raised to some power.
 *       For example "m/s" is a derived units.</li>
 *   <li><b>Alternate units</b> are dimensionless units handled as if they had a dimension.
 *       An example is angular degrees.</li>
 *   <li><b>Conventional units</b> are units multiplied or shifted by a constant value compared to a base,
 *       derived or alternate unit. For example "km" is a unit equals to 1000 metres, and"°C" is a unit
 *       shifted by 237.15 degrees compared to the Kelvin unit.</li>
 * </ul>
 *
 * In Apache SIS implementation, base and derived units are represented by the same class: {@link SystemUnit}.
 * All unit instances shall be immutable and thread-safe.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @version 1.0
 *
 * @param <Q>  the kind of quantity to be measured using this units.
 *
 * @since 0.8
 * @module
 */
abstract class AbstractUnit<Q extends Quantity<Q>> implements Unit<Q>, LenientComparable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5559950920796714303L;

    /**
     * The multiplication and division symbols used for Unicode representation.
     * Also used for internal representation of {@link #symbol}.
     */
    static final char MULTIPLY = '⋅', DIVIDE = '∕';

    /**
     * The unit symbol, or {@code null} if this unit has no specific symbol. If {@code null},
     * then the {@link #toString()} method is responsible for creating a representation on the fly.
     * If non-null, this symbol should complies with the {@link UnitFormat.Style#SYMBOL} formatting
     * (<strong>not</strong> the UCUM format). In particular, this symbol uses Unicode characters
     * for arithmetic operators and superscripts, as in “m/s²”. However this symbol should never
     * contains the unit conversion terms. For example “km” is okay, but “1000⋅m” is not.
     * The intent of those rules is to make easier to analyze the symbol in methods like
     * {@link ConventionalUnit#power(String)}.
     *
     * <p>Users can override this symbol by call to {@link UnitFormat#label(Unit, String)},
     * but such overriding applies only to the target {@code UnitFormat} instance.</p>
     *
     * <p>The value assigned to this field is also used by {@link #getName()} for fetching a localized name
     * from the resource bundle.</p>
     *
     * @see #getSymbol()
     * @see SystemUnit#alternate(String)
     */
    private final String symbol;

    /**
     * A code that identifies whether this unit is part of SI system, or outside SI but accepted for use with SI.
     * Value can be {@link UnitRegistry#SI}, {@link UnitRegistry#ACCEPTED}, other constants or 0 if unknown.
     *
     * <p>This information may be approximate since we can not always guess correctly whether the result of
     * an operation is part of SI or not. Values given to the field may be adjusted in any future version.</p>
     *
     * <p>This information is not serialized because {@link #readResolve()} will replace the deserialized instance
     * by a hard-coded instance with appropriate value, if possible.</p>
     *
     * @see #equals(short, short)
     */
    final transient byte scope;

    /**
     * The EPSG code, or 0 if this unit has no EPSG code.
     *
     * <p>This information is not serialized because {@link #readResolve()} will replace the deserialized instance
     * by a hard-coded instance with appropriate value, if possible.</p>
     *
     * @see #equals(short, short)
     */
    final transient short epsg;

    /**
     * Creates a new unit having the given symbol and EPSG code.
     *
     * @param  symbol  the unit symbol, or {@code null} if this unit has no specific symbol.
     * @param  scope   {@link UnitRegistry#SI}, {@link UnitRegistry#ACCEPTED}, other constants or 0 if unknown.
     * @param  epsg    the EPSG code, or 0 if this unit has no EPSG code.
     */
    AbstractUnit(final String symbol, final byte scope, final short epsg) {
        this.symbol = symbol;
        this.scope  = scope;
        this.epsg   = epsg;
    }

    /**
     * Returns {@code true} if the use of SI prefixes is allowed for the given unit.
     */
    static boolean isPrefixable(final Unit<?> unit) {
        return (unit instanceof AbstractUnit<?>) && ((AbstractUnit<?>) unit).isPrefixable();
    }

    /**
     * Returns {@code true} if the use of SI prefixes is allowed for this unit.
     */
    final boolean isPrefixable() {
        return (scope & UnitRegistry.PREFIXABLE) != 0;
    }

    /**
     * Returns the symbol (if any) of this unit. A unit may have no symbol, in which case
     * the {@link #toString()} method is responsible for creating a string representation.
     *
     * <div class="note"><b>Example:</b>
     * {@link Units#METRE} has the {@code "m"} symbol and the same string representation.
     * But {@link Units#METRES_PER_SECOND} has no symbol; it has only the {@code "m/s"}
     * string representation.</div>
     *
     * @return the unit symbol, or {@code null} if this unit has no specific symbol.
     *
     * @see #toString()
     * @see UnitFormat
     */
    @Override
    public final String getSymbol() {
        return symbol;
    }

    /**
     * Returns the name (if any) of this unit. For example {@link Units#METRE} has the "m" symbol and the "metre" name.
     * If this unit exists in the EPSG database, then this method should return the name as specified in the database.
     *
     * @return the unit name, or {@code null} if this unit has no specific name.
     *
     * @see UnitFormat#format(Unit, Appendable)
     */
    @Override
    public final String getName() {
        if (symbol != null) try {
            return UnitFormat.getBundle(Locale.getDefault()).getString(symbol);
        } catch (MissingResourceException e) {
            // Ignore as per this method contract.
        }
        return null;
    }

    /**
     * Returns the unscaled system unit from which this unit is derived.
     * System units are either base units, {@linkplain #alternate(String) alternate}
     * units or product of rational powers of system units.
     *
     * @return the system unit this unit is derived from, or {@code this} if this unit is a system unit.
     */
    @Override
    public abstract SystemUnit<Q> getSystemUnit();

    /**
     * Returns the base units and their exponent whose product is the system unit,
     * or {@code null} if the system unit is a base unit (not a product of existing units).
     *
     * @return the base units and their exponent making up the system unit.
     */
    @Override
    public abstract Map<SystemUnit<?>, Integer> getBaseUnits();

    /**
     * Returns the base units used by Apache SIS implementations.
     * Contrarily to {@link #getBaseUnits()}, this method never returns {@code null}.
     */
    abstract Map<SystemUnit<?>, Fraction> getBaseSystemUnits();

    /**
     * Indicates if this unit is compatible with the given unit.
     * This implementation delegates to:
     *
     * {@preformat java
     *   return getDimension().equals(that.getDimension());
     * }
     *
     * @param  that the other unit to compare for compatibility.
     * @return {@code true} if the given unit is compatible with this unit.
     *
     * @see #getDimension()
     */
    @Override
    public final boolean isCompatible(final Unit<?> that) {
        ArgumentChecks.ensureNonNull("that", that);
        return getDimension().equals(that.getDimension());
    }

    /**
     * Returns the error message for an incompatible unit.
     */
    final String incompatible(final Unit<?> that) {
        return Errors.format(Errors.Keys.IncompatibleUnits_2, this, that);
    }

    /**
     * Returns the result of setting the origin of the scale of measurement to the given value.
     * For example {@code CELSIUS = KELVIN.shift(273.15)} returns a unit where 0°C is equals to 273.15 K.
     *
     * @param  offset  the value to add when converting from the new unit to this unit.
     * @return this unit offset by the specified value, or {@code this} if the given offset is zero.
     */
    @Override
    public final Unit<Q> shift(final double offset) {
        return transform(LinearConverter.offset(offset, 1));
    }

    /**
     * Returns the result of multiplying this unit by the specified factor.
     * For example {@code KILOMETRE = METRE.multiply(1000)} returns a unit where 1 km is equals to 1000 m.
     *
     * @param  multiplier  the scale factor when converting from the new unit to this unit.
     * @return this unit scaled by the specified multiplier.
     */
    @Override
    public final Unit<Q> multiply(double multiplier) {
        final double divisor = inverse(multiplier);
        if (divisor != 1) multiplier = 1;
        return transform(LinearConverter.scale(multiplier, divisor));
    }

    /**
     * Returns the result of dividing this unit by an approximate divisor.
     * For example {@code GRAM = KILOGRAM.divide(1000)} returns a unit where 1 g is equals to 0.001 kg.
     *
     * @param  divisor  the inverse of the scale factor when converting from the new unit to this unit.
     * @return this unit divided by the specified divisor.
     */
    @Override
    public final Unit<Q> divide(double divisor) {
        final double multiplier = inverse(divisor);
        if (multiplier != 1) divisor = 1;
        return transform(LinearConverter.scale(multiplier, divisor));
    }

    /**
     * If the inverse of the given multiplier is an integer, returns that inverse. Otherwise returns 1.
     * This method is used for replacing e.g. {@code multiply(0.001)} calls by {@code divide(1000)} calls.
     * The later allows more accurate operations because of the way {@link LinearConverter} is implemented.
     */
    private static double inverse(final double multiplier) {
        if (Math.abs(multiplier) < 1) {
            final double inverse = 1 / multiplier;
            final double r = Math.rint(inverse);
            if (AbstractConverter.epsilonEquals(inverse, r)) {
                return r;
            }
        }
        return 1;
    }

    /**
     * Returns the inverse of this unit.
     *
     * @return 1 / {@code this}
     */
    @Override
    public final Unit<?> inverse() {
        return pow(-1);
    }

    /**
     * Returns units for the same quantity but with scale factors that are not the SI one, or {@code null} if none.
     * This method returns a direct reference to the internal field; caller shall not modify.
     */
    ConventionalUnit<Q>[] related() {
        return null;
    }

    /**
     * Compares this unit with the given object for equality.
     *
     * @param  other  the other object to compare with this unit, or {@code null}.
     * @return {@code true} if the given object is equal to this unit.
     */
    @Override
    public final boolean equals(final Object other) {
        return equals(other, ComparisonMode.STRICT);
    }

    /**
     * Compares this unit with the given object for equality,
     * optionally ignoring metadata and rounding errors.
     */
    @Override
    public boolean equals(final Object other, final ComparisonMode mode) {
        if (other == null || other.getClass() != getClass()) {
            return false;
        }
        if (mode.isIgnoringMetadata()) {
            return true;
        }
        final AbstractUnit<?> that = (AbstractUnit<?>) other;
        return equals(epsg, that.epsg) && equals(scope, that.scope) && Objects.equals(symbol, that.symbol);
    }

    /**
     * Compares the given values only if both of them are non-zero. If at least one value is zero (meaning "unknown"),
     * assume that values are equal. We do that because deserialized {@code AbstractUnit} instances have {@link #epsg}
     * and {@link #scope} fields initialized to 0, and we need to ignore those values when comparing the deserialized
     * instances with instances hard-coded in {@link Units} class. We should not have inconsistencies because there is
     * no public way to set those fields; they can only be defined in the {@code Units} hard-coded constants.
     */
    private static boolean equals(final short a, final short b) {
        return (a == 0) || (b == 0) || (a == b);
    }

    /**
     * Returns a hash code value for this unit.
     */
    @Override
    public int hashCode() {
        // Do not use EPSG code or scope because they are not serialized.
        return 31 * Objects.hashCode(symbol);
    }

    /**
     * Returns the unlocalized string representation of this unit, either as a single symbol
     * or a product of symbols or scale factors, eventually with an offset.
     *
     * @see #getSymbol()
     */
    @Override
    public final String toString() {
        if (symbol != null) {
            return symbol;
        } else {
            return UnitFormat.INSTANCE.format(this);
        }
    }

    /**
     * Returns {@code true} if the given Unicode code point is a valid character for a unit symbol.
     * Current implementation accepts letters, subscripts and the degree sign, but the set of legal
     * characters may be expanded in any future SIS version (however it should never allow spaces).
     * The goal is to avoid confusion with exponents and to detect where a unit symbol ends.
     *
     * <p>Space characters must be excluded from the set of legal characters because allowing them
     * would make harder for {@link UnitFormat} to detect correctly where a unit symbol ends.</p>
     *
     * <p>Note that some units defined in the {@link Units} class break this rule. In particular,
     * some of those units contains superscripts or division sign. But the hard-coded symbols in
     * that class are known to be consistent with SI usage or with {@link UnitFormat} work.</p>
     */
    static boolean isSymbolChar(final int c) {
        return Character.isLetter(c) || Characters.isSubScript(c) || "°'′’\"″%‰‱-_".indexOf(c) >= 0;
    }

    /**
     * Invoked on deserialization for returning a unique instance of {@code AbstractUnit} if possible.
     */
    final Object readResolve() throws ObjectStreamException {
        if (symbol != null && Units.initialized) {              // Force Units class initialization.
            final Object existing = UnitRegistry.putIfAbsent(symbol, this);
            if (equals(existing)) {
                return (Unit<?>) existing;
            }
        }
        return this;
    }
}
