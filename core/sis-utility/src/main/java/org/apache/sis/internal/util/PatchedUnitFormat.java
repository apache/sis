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
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.measure.quantity.Quantity;
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
public final class PatchedUnitFormat extends Format {
    /**
     * For cross-version compatibility (even if this class is hopefully temporary).
     */
    private static final long serialVersionUID = -3064428584419360693L;

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
        final Map<Unit<?>,String> symbols = new HashMap<Unit<?>,String>(8);
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
     * The {@link UnitFormat} to patch.
     */
    private final UnitFormat format;

    /**
     * {@code true} for formatting the unit names using US spelling.
     * Example: "meter" instead of "metre".
     */
    public boolean isLocaleUS;

    /**
     * Creates a new {@code PatchedUnitFormat} instance wrapping the given format.
     *
     * @param format the format to wrap.
     */
    public PatchedUnitFormat(final UnitFormat format) {
        this.format = format;
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
    public static <Q extends Quantity> Unit<Q> toFormattable(Unit<Q> unit) {
        final Map<Unit<?>,String> symbols = SYMBOLS;
        if (symbols != null && symbols.containsKey(unit)) {
            assert Units.isAngular(unit);
            unit = (Unit<Q>) NonSI.DEGREE_ANGLE;
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
     * Formats the given unit.
     *
     * @param  unit The unit to format.
     * @param  toAppendTo where to append to unit.
     * @param  pos Ignored.
     * @return The given {@code toAppendTo} argument.
     */
    @Override
    public StringBuffer format(final Object unit, final StringBuffer toAppendTo, final FieldPosition pos) {
        final Map<Unit<?>,String> symbols = SYMBOLS;
        if (symbols != null) {
            final String symbol = symbols.get(unit);
            if (symbol != null) {
                return toAppendTo.append(symbol);
            }
        }
        /*
         * Following are specific to the WKT format, which is currently the only user of this method.
         * If we invoke this method for other purposes, then we would need to provide more control on
         * what kind of formatting is desired.
         */
        if (Unit.ONE.equals(unit)) {
            return toAppendTo.append("unity");
        } else if (NonSI.DEGREE_ANGLE.equals(unit)) {
            return toAppendTo.append("degree");
        } else if (SI.METRE.equals(unit)) {
            return toAppendTo.append(isLocaleUS ? "meter" : "metre");
        } else if (NonSI.FOOT_SURVEY_US.equals(unit)) {
            return toAppendTo.append("US survey foot");
        } else if (Units.PPM.equals(unit)) {
            return toAppendTo.append("parts per million");
        }
        return format.format(unit, toAppendTo, pos);
    }

    /**
     * Delegates to the wrapped {@link UnitFormat}.
     *
     * @return The parsed unit, or {@code null}.
     */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        return format.parseObject(source, pos);
    }
}
