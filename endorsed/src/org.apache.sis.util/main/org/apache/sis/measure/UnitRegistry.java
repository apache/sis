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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.io.Serializable;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.Dimension;
import javax.measure.spi.SystemOfUnits;
import javax.measure.format.MeasurementParseException;
import org.apache.sis.math.Fraction;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.logging.Logging;


/**
 * Lookup mechanism for finding a units from its quantity, dimension or symbol.
 * This class opportunistically implements {@link SystemOfUnits}, but Apache SIS
 * rather uses the static methods directly since we define all units in terms of SI.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class UnitRegistry implements SystemOfUnits, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -84557361079506390L;

    /**
     * A bitmask specifying that the unit symbol can be combined with a SI prefix.
     * This is usually combined only with {@link #SI}, not {@link #ACCEPTED} except
     * the litre unit (cL, mL, etc) and bel (for creating the decibel unit).
     * The gal unit can also be prefixed, but this unit is deprecated by ISO 80000-3:2006.
     */
    static final byte PREFIXABLE = 1;

    /**
     * Identifies units defined by the SI system.
     * All {@link SystemUnit} instances with this code can have a SI prefix.
     */
    static final byte SI = 2;

    /**
     * Identifies units defined outside the SI system but accepted for use with SI.
     */
    static final byte ACCEPTED = 4;

    /**
     * Identifies units defined by the centimeter–gram–second (CGS) system.
     */
    static final byte CGS = 8;

    /**
     * Identifies units defined for use in British imperial system.
     */
    static final byte IMPERIAL = 16;

    /**
     * Identifies units defined in another system than the above.
     */
    static final byte OTHER = 32;

    /**
     * All {@link UnitDimension}, {@link SystemUnit} or {@link ConventionalUnit} that are hard-coded in Apache SIS.
     * This map is populated by {@link Units} static initializer and shall not be modified after initialization,
     * in order to avoid the need for synchronization. Key and value types are restricted to the following pairs:
     *
     * <table class="sis">
     *   <caption>Key and value types</caption>
     *   <tr><th>Key type</th>                            <th>Value type</th>            <th>Description</th></tr>
     *   <tr><td>{@code Map<UnitDimension,Fraction>}</td> <td>{@link UnitDimension}</td> <td>Key is the base dimensions with their powers</td></tr>
     *   <tr><td>{@link UnitDimension}</td>               <td>{@link SystemUnit}</td>    <td>Key is the dimension of base or derived units.</td></tr>
     *   <tr><td>{@code Class<Quantity>}</td>             <td>{@link SystemUnit}</td>    <td>Key is the quantity type of base of derived units.</td></tr>
     *   <tr><td>{@link String}</td>                      <td>{@link AbstractUnit}</td>  <td>Key is the unit symbol.</td></tr>
     *   <tr><td>{@link Short}</td>                       <td>{@link AbstractUnit}</td>  <td>Key is the EPSG code.</td></tr>
     * </table>
     *
     * <h4>Dimension order</h4>
     * The search for an existing unit in this map is <strong>not</strong> sensitive to dimension order.
     * N⋅m is considered equivalent to m⋅N, and both of them are associated to the symbol "J" (Joule).
     * We ignore dimension order because it has no incidence on the unit symbol shown to user.
     */
    private static final Map<Object, Object> HARD_CODED = new HashMap<>(256);

    /**
     * Units defined by the user. Accesses to this map implies synchronization.
     * Values are stored by weak references and garbage collected when no longer used.
     * Key and value types are the same as the one described in {@link #HARD_CODED}.
     *
     * <h4>Dimension order</h4>
     * Contrarily to {@link #HARD_CODED}, the user-specified map of units is sensitive to dimension order.
     * kg∕(m⋅s³) is not considered the same as kg∕(s³⋅m) for formatting purpose (but still considered the
     * same for unit conversions purpose). This distinction is applied because the unit may have no label
     * associated to it. The only label may be the list of dimensions, so we try to show them in the same
     * order as specified by the users when they constructed their units.
     *
     * <h4>Implementation note</h4>
     * We separate hard-coded values from user-defined values because the number of hard-coded values is relatively
     * large, using weak references for them is useless, and most applications will not define any custom values so
     * the user-defined map will typically stay empty. This separation avoids synchronization of hard-coded values.
     * Furthermore the two maps have a different policy on whether to consider dimension order as significant.
     */
    private static final WeakValueHashMap<Object, Object> USER_DEFINED = new WeakValueHashMap<>(Object.class,
            UnitRegistry::hashCodeOrdered,
            UnitRegistry::equalsOrdered);

    /**
     * Returns a hash code value for the specified key, taking dimension order in account.
     * This is used for the policy of keys in the {@link #USER_DEFINED} map.
     *
     * @param  key  a key of one of the types defined in {@link #HARD_CODED}.
     * @return hash code value for the given key. Never null.
     */
    private static int hashCodeOrdered(final Object key) {
        if (key instanceof UnitDimension) return ((UnitDimension) key).hashCodeOrdered();
        if (key instanceof Map<?,?>) return UnitDimension.hashCodeOrdered((Map<?,?>) key);
        return key.hashCode();
    }

    /**
     * Compares the given keys, taking dimension order in account.
     * Shall be consistent with {@link #hashCodeOrdered(Object)}.
     *
     * @param  key    a key of one of the types defined in {@link #HARD_CODED}.
     * @param  other  an object to compare with the key. Never null.
     * @return whether the given object are equal.
     */
    private static boolean equalsOrdered(final Object key, final Object other) {
        if (key instanceof UnitDimension) {
            if (other instanceof UnitDimension) {
                return ((UnitDimension) key).equalsOrdered((UnitDimension) other);
            }
        } else if (key instanceof Map<?,?>) {
            if (other instanceof Map<?,?>) {
                return UnitDimension.equalsOrdered((Map<?,?>) key, (Map<?,?>) other);
            }
        }
        return key.equals(other);
    }

    /**
     * Adds the given {@code components}, {@code dim} pair in the map of hard-coded values.
     * This method shall be invoked in a single thread by the {@code Units} class initializer only (indirectly).
     */
    static void init(final Map<UnitDimension,Fraction> components, final UnitDimension dim) {
        assert !Units.initialized : dim.symbol;         // This assertion happens during Units initialization, but it is okay.
        if (HARD_CODED.put(components, dim) != null) {
            throw new AssertionError(dim.symbol);       // Shall not map the same dimension twice.
        }
    }

    /**
     * Invoked by {@link Units} static class initializer for registering SI base and derived units.
     * This method shall be invoked in a single thread by the {@code Units} class initializer only.
     */
    static <Q extends Quantity<Q>> SystemUnit<Q> init(final SystemUnit<Q> unit) {
        assert !Units.initialized : unit;        // This assertion happens during Units initialization, but it is okay.
        final String symbol = unit.getSymbol();
        int existed;
        /* Unconditional */ existed  = (HARD_CODED.put(unit.dimension, unit) == null) ? 0 : 1;
        /* Unconditional */ existed |= (HARD_CODED.put(unit.quantity,  unit) == null) ? 0 : 2;
        if (symbol != null) existed |= (HARD_CODED.put(symbol,         unit) == null) ? 0 : 4;
        if (unit.epsg != 0) existed |= (HARD_CODED.put(unit.epsg,      unit) == null) ? 0 : 8;
        /*
         * Key collision on dimension and quantity tolerated for dimensionless units only, with an
         * an exception for "candela" because "lumen" is candela divided by a dimensionless unit.
         * Another exception is "Hz" because it come after rad/s, which has the same dimension.
         */
        assert filter(existed, unit, symbol) == 0 : unit;
        return unit;
    }

    /**
     * Clears the {@code existed} bits for the cases where we allow dimension or quantity type collisions.
     * This method is invoked for assertions only.
     */
    private static int filter(int existed, final SystemUnit<?> unit, final String s) {
        if ("cd".equals(s) ||
            "Hz".equals(s) || "Bq".equals(s)) existed &= ~(1    );      // Accepts dimension collisions only;
        if (unit.dimension.isDimensionless()) existed &= ~(1 | 2);      // Accepts dimension and quantity collisions.
        return Strings.isNullOrEmpty(s) ? 0 : existed;
    }

    /**
     * Invoked by {@link Units} static class initializer for registering SI conventional units.
     * This method shall be invoked in a single thread by the {@code Units} class initializer only.
     */
    static <Q extends Quantity<Q>> ConventionalUnit<Q> init(final ConventionalUnit<Q> unit) {
        assert !Units.initialized : unit;        // This assertion happens during Units initialization, but it is okay.
        if (HARD_CODED.put(unit.getSymbol(), unit) == null) {
            if (unit.epsg == 0 || HARD_CODED.put(unit.epsg, unit) == null) {
                return unit;
            }
        }
        throw new AssertionError(unit);      // Shall not map the same unit twice.
    }

    /**
     * Adds an alias for the given unit. The given alias shall be either an instance of {@link String}
     * (for a symbol alias) or an instance of {@link Short} (for an EPSG code alias).
     */
    static void alias(final Unit<?> unit, final Comparable<?> alias) {
        assert !Units.initialized : unit;        // This assertion happens during Units initialization, but it is okay.
        if (HARD_CODED.put(alias, unit) != null) {
            throw new AssertionError(unit);      // Shall not map the same alias twice.
        }
    }

    /**
     * Adds the given {@code key}, {@code value} pair in the map of user-defined values, provided that no value
     * is currently associated to the given key. This method shall be invoked only after the {@link Units} class
     * has been fully initialized.
     */
    static Object putIfAbsent(final Object key, final Object value) {
        assert Units.initialized : value;
        Object previous = HARD_CODED.get(key);
        if (previous == null) {
            previous = USER_DEFINED.putIfAbsent(key, value);
        }
        return previous;
    }

    /**
     * Returns the value associated to the given key, or {@code null} if none.
     * This method can be invoked at anytime (at {@link Units} class initialization time or not).
     */
    static Object get(final Object key) {
        Object value = HARD_CODED.get(key);     // Treated as immutable, no synchronization needed.
        if (value == null) {
            value = USER_DEFINED.get(key);      // Implies a synchronization lock.
        }
        return value;
    }

    /**
     * Name of this system of units.
     */
    final String name;

    /**
     * The bitmask for units to include. Can be any combination of {@link #SI}, {@link #ACCEPTED},
     * {@link #IMPERIAL} or {@link #OTHER} bits.
     */
    private final int includes;

    /**
     * The value returned by {@link #getUnits()}, created when first needed.
     */
    private transient Set<Unit<?>> units;

    /**
     * Creates a new unit system.
     */
    UnitRegistry(final String name, final int includes) {
        this.name     = name;
        this.includes = includes;
    }

    /**
     * Returns the name of this system of units.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the default unit for the specified quantity, or {@code null} if none.
     */
    @Override
    public <Q extends Quantity<Q>> Unit<Q> getUnit(final Class<Q> type) {
        return Units.get(type);
    }

    /**
     * Returns a unit with the given string representation,
     * or {@code null} if none is found in this unit system.
     *
     * @param  symbols  the string representation of a unit.
     * @return the unit with the given string representation,
     *         or {@code null} if the give symbols cannot be parsed.
     */
    @Override
    public Unit<?> getUnit(final String symbols) {
        try {
            return Units.valueOf(symbols);
        } catch (MeasurementParseException e) {
            Logging.ignorableException(AbstractUnit.LOGGER, UnitRegistry.class, "getUnit", e);
            return null;
        }
    }

    /**
     * Returns a read only view over the units explicitly defined by this system.
     * This include the base and derived units which are assigned a special name and symbol.
     * This set does not include new units created by arithmetic or other operations.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<Unit<?>> getUnits() {
        if (Units.initialized) {                    // Force Units class initialization.
            synchronized (this) {
                if (units == null) {
                    units = new HashSet<>();
                    for (final Object value : HARD_CODED.values()) {
                        if (value instanceof AbstractUnit<?>) {
                            final AbstractUnit<?> unit = (AbstractUnit<?>) value;
                            if ((unit.scope & includes) != 0) {
                                units.add(unit);
                            }
                        }
                    }
                    units = Collections.unmodifiableSet(units);
                }
            }
        }
        return units;
    }

    /**
     * Returns the units defined in this system having the specified dimension, or an empty set if none.
     */
    @Override
    public Set<Unit<?>> getUnits(final Dimension dimension) {
        ArgumentChecks.ensureNonNull("dimension", dimension);
        final var filtered = new HashSet<Unit<?>>();
        for (final Unit<?> unit : getUnits()) {
            if (dimension.equals(unit.getDimension())) {
                filtered.add(unit);
            }
        }
        return filtered;
    }
}
