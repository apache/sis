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

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Objects;
import java.text.NumberFormat;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.format.UnitFormat;
import javax.measure.format.QuantityFormat;
import javax.measure.spi.QuantityFactory;
import javax.measure.spi.ServiceProvider;
import javax.measure.spi.SystemOfUnits;
import javax.measure.spi.SystemOfUnitsService;
import javax.measure.spi.FormatService;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.util.logging.Logging;


/**
 * The central point from which all unit services (parsing, formatting, listing, <i>etc</i>) can be obtained.
 * Apache SIS does not use this class (SIS rather uses {@link Units} predefined constants and {@link UnitFormat}
 * directly since they are designed specifically for SIS needs).
 * This class is provided for allowing other applications to discover Apache SIS implementation of JSR-385
 * without direct dependency. A {@code UnitServices} instance can be obtained by call to {@link #current()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.8
 */
public class UnitServices extends ServiceProvider implements SystemOfUnitsService, FormatService {
    /**
     * All system of units known to this provider.
     * The last element in the array is the default system with all units known to SIS.
     */
    private final UnitRegistry[] systems;

    /**
     * Creates a new service provider. {@code UnitServices} should not be instantiated
     * directly unless the caller wants specifically the Apache SIS implementation.
     * For obtaining the default implementation, use {@link #current()} instead.
     */
    public UnitServices() {
        systems = new UnitRegistry[] {
            new UnitRegistry("SI",            UnitRegistry.SI),
            new UnitRegistry("SI + accepted", UnitRegistry.SI | UnitRegistry.ACCEPTED),
            new UnitRegistry("Imperial",      UnitRegistry.IMPERIAL),
            new UnitRegistry("CGS",           UnitRegistry.CGS),
            new UnitRegistry("SI + other",    -1)                       // Must be last.
        };
    }

    /**
     * Returns the default system of units used by Apache SIS.
     * This include the International System of Units (SI) together with some imperial units and other units.
     * This system includes at least all the constants defined in the {@link Units} class.
     *
     * @return the system of units used by Apache SIS.
     */
    @Override
    public SystemOfUnits getSystemOfUnits() {
        return systems[systems.length - 1];
    }

    /**
     * Returns the system of units having the specified name, or {@code null} if none.
     * The argument can be any name in the following table:
     *
     * <table class="sis">
     *   <caption>Available system of units</caption>
     *   <tr><th>Name</th>          <th>Examples</th></tr>
     *   <tr><td>SI</td>            <td>m, km, m³, s, m∕s, K, °C, hPa, rad, µrad</td></tr>
     *   <tr><td>SI + accepted</td> <td>s, min, h, m∕s, km∕h, °, ′, ″, ha</td></tr>
     *   <tr><td>Imperial</td>      <td>in, ft, mi (statute mile)</td></tr>
     *   <tr><td>SI + other</td>    <td>m, m∕s, km∕h, ft, mi, M (nautical mile)</td></tr>
     * </table>
     *
     * The search for name is case-insensitive and ignore whitespaces.
     *
     * @param  name  the name of the desired system of units.
     * @return the system of units for the given name, or {@code null} if none.
     */
    @Override
    public SystemOfUnits getSystemOfUnits(final String name) {
        ArgumentChecks.ensureNonEmpty("name", name);
        for (final UnitRegistry s : systems) {
            if (CharSequences.equalsFiltered(s.name, name, Characters.Filter.UNICODE_IDENTIFIER, true)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns the list of all available systems of units.
     *
     * @return list of available systems of units.
     */
    @Override
    public Collection<SystemOfUnits> getAvailableSystemsOfUnits() {
        return UnmodifiableArrayList.wrap(systems);
    }

    /**
     * Returns the service to obtain a {@link SystemOfUnits} instances.
     * The default implementation returns {@code this} since this {@code UnitServices} class
     * implements directly all relevant interfaces. The methods related to system of units are:
     *
     * <ul>
     *   <li>{@link #getSystemOfUnits()}</li>
     *   <li>{@link #getSystemOfUnits(String)}</li>
     *   <li>{@link #getAvailableSystemsOfUnits()}</li>
     * </ul>
     *
     * @return the service to obtain a {@link SystemOfUnits}, or {@code null} if none.
     */
    @Override
    public SystemOfUnitsService getSystemOfUnitsService() {
        return this;
    }

    /**
     * Returns an unit format instance for human-readable unit symbols in the default locale.
     * The format style is {@link org.apache.sis.measure.UnitFormat.Style#SYMBOL}.
     * This style requires support for Unicode characters;
     * for example square metres are formatted as “m²”, not “m2”.
     *
     * @return a {@link org.apache.sis.measure.UnitFormat} instance for unit symbols.
     */
    @Override
    public UnitFormat getUnitFormat() {
        return new org.apache.sis.measure.UnitFormat(Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Returns the unit format for the given name. The argument can be the name of
     * any value in the {@link org.apache.sis.measure.UnitFormat.Style} enumeration.
     * The argument can be any name in the following table:
     *
     * <table class="sis">
     *   <caption>Available unit format name</caption>
     *   <tr><th>Name</th>      <th>Examples</th></tr>
     *   <tr><td>SYMBOL</td>    <td>km, m³, m∕s, N⋅m, K, °C, hPa, rad, µrad</td></tr>
     *   <tr><td>UCUM</td>      <td>km, m3, m/s, N.m</td></tr>
     *   <tr><td>NAME</td>      <td>kilometre, cubic metre, metres per second</td></tr>
     * </table>
     *
     * The {@code "NAME"} format is locale-sensitive. The format locale can be modified by a call
     * to {@link org.apache.sis.measure.UnitFormat#setLocale(Locale)} on the returned object.
     *
     * @param  name the name of the desired format.
     * @return the corresponding unit format, or {@code null} if none.
     */
    @Override
    public UnitFormat getUnitFormat(String name) {
        final Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        name = name.toUpperCase(locale).trim();
        final org.apache.sis.measure.UnitFormat.Style style;
        try {
            style = org.apache.sis.measure.UnitFormat.Style.valueOf(name);
        } catch (IllegalArgumentException e) {
            // JSR-385 specification mandate that we return null.
            Logging.recoverableException(AbstractUnit.LOGGER, UnitServices.class, "getUnitFormat", e);
            return null;
        }
        org.apache.sis.measure.UnitFormat f = new org.apache.sis.measure.UnitFormat(locale);
        f.setStyle(style);
        return f;
    }

    /**
     * Returns the unit format having the specified name or {@code null} if none.
     * The variant is an optional argument for requesting e.g. ASCII-only format,
     * or for choosing case-sensitive versus case-insensitive variants.
     * In current implementation the variant argument is ignored.
     *
     * @param  name     the name of the desired format.
     * @param  variant  indicates a variation of a unit format.
     * @return the corresponding unit format, or {@code null} if none.
     * @since  1.4
     */
    @Override
    public UnitFormat getUnitFormat(final String name, final String variant) {
        return getUnitFormat(name);
    }

    /**
     * Returns a quantity format for the default locale.
     *
     * @return a {@link org.apache.sis.measure.QuantityFormat} instance for quantities.
     * @since  1.4
     */
    @Override
    public QuantityFormat getQuantityFormat() {
        return new org.apache.sis.measure.QuantityFormat(Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Returns the quantity format having the specified name or {@code null} if none.
     * The names accepted by this method are those documented in {@link #getUnitFormat(String)}.
     *
     * @param  name  the name of the format.
     * @return the corresponding quantity format, or {@code null} if none.
     * @since  1.4
     */
    @Override
    public QuantityFormat getQuantityFormat(final String name) {
        final UnitFormat unitFormat = getUnitFormat(name);
        if (unitFormat instanceof org.apache.sis.measure.UnitFormat) {
            return new org.apache.sis.measure.QuantityFormat(NumberFormat.getInstance(),
                        (org.apache.sis.measure.UnitFormat) unitFormat);
        }
        return null;
    }

    /**
     * Returns a list of available format names. The default implementation returns the names
     * of all values in the {@link org.apache.sis.measure.UnitFormat.Style} enumeration.
     *
     * @param  type  the type of formats (for units or for quantities).
     * @return list of available formats.
     */
    @Override
    public Set<String> getAvailableFormatNames(final FormatType type) {
        final Set<String> names = new HashSet<>(4);
        for (final Enum<?> e : org.apache.sis.measure.UnitFormat.Style.values()) {
            names.add(e.name());
        }
        return names;
    }

    /**
     * Returns the service to obtain a {@link UnitFormat} instances.
     * The default implementation returns {@code this} since this {@code UnitServices} class
     * implements directly all relevant interfaces. The methods related to unit formats are:
     *
     * <ul>
     *   <li>{@link #getUnitFormat()}</li>
     *   <li>{@link #getUnitFormat(String)}</li>
     *   <li>{@link #getAvailableFormatNames(FormatType)}</li>
     * </ul>
     *
     * @return the service to obtain a {@link UnitFormat}, or {@code null} if none.
     */
    @Override
    public FormatService getFormatService() {
        return this;
    }

    /**
     * Return a factory for the given {@code Quantity} type. In the particular case of Apache SIS implementation,
     * {@link Quantities#create(double, Unit)} provides a more direct way to instantiate quantities.
     *
     * @param  <Q>   compile-time value of the {@code type} argument.
     * @param  type  type of the desired the quantity.
     * @return the service to obtain {@link Quantity} instances.
     *
     * @see Quantities#create(double, Unit)
     */
    @Override
    public <Q extends Quantity<Q>> QuantityFactory<Q> getQuantityFactory(final Class<Q> type) {
        QuantityFactory<Q> factory = Units.get(Objects.requireNonNull(type));
        if (factory == null) {
            if (type != null) {
                factory = new DefaultQuantityFactory<Q>() {
                    @Override
                    public Quantity<Q> create(final Number value, final Unit<Q> unit) {
                        return ScalarFallback.factory(AbstractConverter.doubleValue(value), unit, type);
                    }
                };
            } else {
                factory = new DefaultQuantityFactory<>();
            }
        }
        return factory;
    }
}
