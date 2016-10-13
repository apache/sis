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
package org.apache.sis.internal.util;

import java.util.Map;
import java.util.HashMap;
import javax.measure.Unit;
import javax.measure.Quantity;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Workaround;


/**
 * Workaround for JSR-275 issues.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@Workaround(library="JSR-275", version="0.9.3")
public final class PatchedUnitFormat {
    /**
     * The symbols for some units defined by Apache SIS. We store here the symbols that we were not able
     * to set in the units created by {@link org.apache.sis.measure.SexagesimalConverter} because of
     * <a href="http://kenai.com/jira/browse/JSR_275-41">JSR-275 bug</a>.
     *
     * <p>We do not bother making this map unmodifiable. This is okay if this map is never modified
     * after this field has been assigned a value.</p>
     */
    private static volatile Map<Unit<?>,String> SYMBOLS;

    /**
     * Invoked by {@code SexagesimalConverter} static class initializer for declaring the SIS units
     * that JSR-275 0.9.3 can not format by itself. This method should not be invoked in any other
     * circumstance, otherwise an {@link IllegalStateException} will be thrown.
     *
     * @param entries The (unit, symbol) pairs.
     */
    public static void init(final Object... entries) {
        final Map<Unit<?>,String> symbols = new HashMap<>(8);
        for (int i=0; i<entries.length; i++) {
            final String uom;
            if (symbols.put((Unit<?>) entries[i], uom = (String) entries[++i]) != null) {
                throw new IllegalArgumentException(uom);   // Duplicated unit.
            }
        }
        if (SYMBOLS != null) {  // Check on a best-effort basis only (ignoring race conditions).
            throw new IllegalStateException();
        }
        SYMBOLS = symbols;
    }

    /**
     * Do not allow instantiation of this class.
     */
    private PatchedUnitFormat() {
    }

    /**
     * If the given unit is one of the unit that can not be formatted without ambiguity in WKT format,
     * return a proposed replacement. Otherwise returns {@code unit} unchanged.
     *
     * @param  <Q> The unit dimension.
     * @param  unit The unit to test.
     * @return The replacement to format, or {@code unit} if not needed.
     */
    @SuppressWarnings("unchecked")
    public static <Q extends Quantity<Q>> Unit<Q> toFormattable(Unit<Q> unit) {
        final Map<Unit<?>,String> symbols = SYMBOLS;
        if (symbols != null && symbols.containsKey(unit)) {
            assert Units.isAngular(unit);
            unit = (Unit<Q>) Units.DEGREE;
        }
        return unit;
    }

    /**
     * Returns the string representation of the given unit, or {@code null} if none.
     * This method is used as a workaround for a bug in JSR-275, which sometime throws
     * an exception in the {@link Unit#toString()} method.
     *
     * @param  unit The unit for which to get a string representation, or {@code null}.
     * @return The string representation of the given string (may be an empty string), or {@code null}.
     */
    @Workaround(library="JSR-275", version="0.9.3")
    public static String toString(final Unit<?> unit) {
        if (unit != null) {
            final Map<Unit<?>,String> symbols = SYMBOLS;
            if (symbols != null) {
                final String symbol = symbols.get(unit);
                if (symbol != null) {
                    return symbol;
                }
            }
            try {
                String text = unit.toString();
                if (text.equals("deg")) {
                    text = "°";
                }
                return text;
            } catch (IllegalArgumentException e) {
                // Workaround for JSR-275 implementation bug.
                // Do nothing, we will return null below.
            }
        }
        return null;
    }

    /**
     * Returns the "special-case" symbol of the given unit, or {@code null} if none.
     *
     * @param  unit  the unit to format.
     * @return the "special-case" symbol of given unit, or {@code null}.
     */
    @Workaround(library="JSR-275", version="0.9.3")
    public static String getSymbol(final Object unit) {
        final Map<Unit<?>,String> symbols = SYMBOLS;
        return (symbols != null) ? symbols.get(unit) : null;
    }
}
