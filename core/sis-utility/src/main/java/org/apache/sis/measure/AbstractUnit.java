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
 * @param  <Q>  the kind of quantity to be measured using this units.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
abstract class AbstractUnit<Q extends Quantity<Q>> implements Unit<Q>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5559950920796714303L;

    /**
     * The unit symbol, or {@code null} if this unit has no specific symbol. If {@code null},
     * then the {@link #toString()} method is responsible for creating a representation on the fly.
     * If non-null, this symbol should complies with the {@link UnitFormat.Style#SYMBOL} formatting
     * (<strong>not</strong> the UCUM format). In particular, this symbol uses Unicode characters
     * for arithmetic operators and superscripts, as in “m/s²”. However this symbol should never
     * contains the unit conversion terms. For example “km” is okay, but “1000⋅m” is not.
     * The intend of those rules is to make easier to analyze the symbol in methods like
     * {@link ConventionalUnit#power(String)}.
     *
     * <p>Users can override this symbol by call to {@link UnitFormat#label(Unit, String)},
     * but such overriding applies only to the target {@code UnitFormat} instance.</p>
     *
     * <p>The value given assigned to this field is also used by {@link #getName()}
     * for fetching a localized name from the resource bundle.</p>
     *
     * @see #getSymbol()
     * @see SystemUnit#alternate(String)
     */
    private final String symbol;

    /**
     * A code that identifies whether this unit is part of SI system, or outside SI but accepted for use with SI.
     * Value can be {@link UnitRegistry#SI}, {@link UnitRegistry#ACCEPTED}, other constants or 0 if unknown.
     *
     * <p>This information may be approximative since we can not always guess correctly whether the result of
     * an operation is part of SI or not. Values given to the field may be adjusted in any future version.</p>
     */
    final byte scope;

    /**
     * The EPSG code, or 0 if this unit has no EPSG code.
     */
    final short epsg;

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
        try {
            return UnitFormat.getBundle(Locale.getDefault()).getString(symbol);
        } catch (MissingResourceException e) {
            return null;
        }
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
    public final Unit<Q> multiply(final double multiplier) {
        final double r = inverse(multiplier);
        if (!Double.isNaN(r)) {
            return divide(r);
        }
        return transform(LinearConverter.scale(multiplier, 1));
    }

    /**
     * Returns the result of dividing this unit by an approximate divisor.
     * For example {@code GRAM = KILOGRAM.divide(1000)} returns a unit where 1 g is equals to 0.001 kg.
     *
     * @param  divisor  the inverse of the scale factor when converting from the new unit to this unit.
     * @return this unit divided by the specified divisor.
     */
    @Override
    public final Unit<Q> divide(final double divisor) {
        final double r = inverse(divisor);
        if (!Double.isNaN(r)) {
            return multiply(r);
        }
        return transform(LinearConverter.scale(1, divisor));
    }

    /**
     * If the inverse of the given multiplier is an integer, returns that inverse. Otherwise returns NaN.
     * This method is used for replacing e.g. {@code multiply(0.001)} calls by {@code divide(1000)} calls.
     * The later allows more accurate operations because of the way {@link LinearConverter} is implemented.
     */
    private static double inverse(final double multiplier) {
        if (Math.abs(multiplier) < 1) {
            final double inverse = 1 / multiplier;
            final double r = Math.rint(inverse);
            if (epsilonEquals(inverse, r)) {
                return r;
            }
        }
        return Double.NaN;
    }

    /**
     * Returns {@code true} if the given floating point numbers are considered equal.
     * The tolerance factor used in this method is arbitrary and may change in any future version.
     */
    static boolean epsilonEquals(final double expected, final double actual) {
        return Math.abs(expected - actual) <= Math.scalb(Math.ulp(expected), 4);
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
     * Compares this unit with the given object for equality.
     *
     * @param  other  the other object to compares with this unit, or {@code null}.
     * @return {@code true} if the given object is equals to this unit.
     */
    @Override
    public boolean equals(final Object other) {
        if (other != null && other.getClass() == getClass()) {
            final AbstractUnit<?> that = (AbstractUnit<?>) other;
            return epsg == that.epsg && scope == that.scope && Objects.equals(symbol, that.symbol);
        }
        return false;
    }

    /**
     * Returns a hash code value for this unit.
     */
    @Override
    public int hashCode() {
        return epsg + 31 * Objects.hashCode(symbol);
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
     * Current implementation accepts only letters, subscripts and the degree sign, but the set of
     * legal characters may be expanded in any future SIS version.
     * The most important goal is to avoid confusion with exponents.
     *
     * <p>Note that some units defined in the {@link Units} class break this rule. But the hard-coded
     * symbols in that class are known to be consistent with SI usage or with {@link UnitFormat} work.</p>
     */
    static boolean isSymbolChar(final int c) {
        return Character.isLetter(c) || Characters.isSubScript(c) || c == '°';
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
