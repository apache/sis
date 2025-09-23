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

import java.util.Arrays;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.UnitConverter;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.math.MathFunctions;


/**
 * Utility methods related to the management of prefixes.
 * Current implementation handles only the International System of Unit (SI),
 * but this may be improved in future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Prefixes {
    /**
     * The SI “deca” prefix. This is the only SI prefix encoded on two letters instead of one.
     * It can be represented by the CJK compatibility character “㍲”, but use of those characters
     * is generally not recommended outside of Chinese, Japanese or Korean texts.
     */
    private static final String DECA = "da";

    /**
     * The SI prefixes in increasing order. The only two-letters prefix – “da” – is encoded using the JCK compatibility
     * character “㍲”. The Greek letter μ is repeated twice: the U+00B5 character for micro sign (this is the character
     * that Apache SIS uses in unit symbols) and the U+03BC character for the Greek small letter “mu” (the latter is the
     * character that appears when decomposing JCK compatibility characters with {@link java.text.Normalizer}).
     * Both characters have same appearance but different values.
     *
     * <p>For each prefix at index <var>i</var>, the multiplication factor is given by 10 raised to power {@code POWERS[i]}.</p>
     */
    private static final char[] PREFIXES = {'E','G','M','P','T','Y','Z','a','c','d','f','h','k','m','n','p','y','z','µ','μ','㍲'};
    private static final byte[] POWERS   = {18,  9,  6, 15, 12, 24, 21,-18, -2, -1,-15,  2,  3, -3, -9,-12,-24,-21, -6, -6,  1};

    /**
     * The SI prefixes from smallest to largest. Power of tens go from -24 to +24 inclusive with a step of 3,
     * except for the addition of -2, -1, +1, +2 and the omission of 0.
     *
     * @see #symbol(double, int)
     */
    private static final char[] ENUM = {'y','z','a','f','p','n','µ','m','c','d','㍲','h','k','M','G','T','P','E','Z','Y'};

    /**
     * The maximal power of 1000 for the prefixes in the {@link #ENUM} array. Note that 1000⁸ = 1E+24.
     */
    static final int MAX_POWER = 8;

    /**
     * The converters for SI prefixes, created when first needed.
     *
     * @see #converter(char)
     */
    private static final LinearConverter[] CONVERTERS = new LinearConverter[POWERS.length];

    /**
     * Do not allow instantiation of this class.
     */
    private Prefixes() {
    }

    /**
     * Returns the converter for the given SI prefix, or {@code null} if none.
     * Those converters are created when first needed and cached for reuse.
     */
    static LinearConverter converter(final char prefix) {
        final int i = Arrays.binarySearch(PREFIXES, prefix);
        if (i < 0) {
            return null;
        }
        synchronized (CONVERTERS) {
            LinearConverter c = CONVERTERS[i];
            if (c == null) {
                final int p = POWERS[i];
                final double numerator, denominator;
                if (p >= 0) {
                    numerator = MathFunctions.pow10(p);
                    denominator = 1;
                } else {
                    numerator = 1;
                    denominator = MathFunctions.pow10(-p);
                }
                c = LinearConverter.scale(numerator, denominator);
                CONVERTERS[i] = c;
            }
            return c;
        }
    }

    /**
     * Returns the SI prefix symbol for the given scale factor, or 0 if none.
     *
     * @param  scale  the scale factor.
     * @param  power  the unit power. For example if we are scaling m², then this is 2.
     * @return the prefix, or 0 if none.
     */
    static char symbol(double scale, final int power) {
        switch (power) {
            case 0:  return 0;
            case 1:  break;
            case 2:  scale = Math.sqrt(scale); break;
            case 3:  scale = Math.cbrt(scale); break;
            default: scale = Math.pow(scale, 1.0/power);
        }
        final int n = Numerics.toExp10(Math.getExponent(scale)) + 1;
        if (AbstractConverter.epsilonEquals(MathFunctions.pow10(n), scale)) {
            int i = Math.abs(n);
            switch (i) {
                case 0:  return 0;
                case 1:  // Fallthrough
                case 2:  break;
                default: {
                    if (i > (MAX_POWER*3) || (i % 3) != 0) {
                        return 0;
                    }
                    i = i/3 + 2;
                    break;
                }
            }
            return ENUM[n >= 0 ? (MAX_POWER+1) + i : (MAX_POWER+2) - i];
        }
        return 0;
    }

    /**
     * Returns the concatenation of the given prefix with the given unit symbol.
     */
    static String concat(final char prefix, final String unit) {
        return (prefix == '㍲') ? DECA + unit : prefix + unit;
    }

    /**
     * Returns the unit for the given symbol, taking the SI prefix in account. The given string is usually a single symbol
     * like "km", but may be an expression like "m³" or "m/s" if the given symbol is explicitly registered as an item that
     * {@link Units#get(String)} recognizes. This method does not perform any arithmetic operation on {@code Unit},
     * except a check for the exponent.
     *
     * @param  uom  a symbol compliant with the rules documented in {@link AbstractUnit#symbol}.
     * @return the unit for the given symbol, or {@code null} if no unit is found.
     */
    static Unit<?> getUnit(final String uom) {
        Unit<?> unit = Units.get(uom);
        if (unit == null && uom.length() >= 2) {
            int s = 1;
            char prefix = uom.charAt(0);
            if (prefix == 'd' && uom.charAt(1) == 'a') {
                prefix = '㍲';      // Converse of above 'concat(char, String)' method.
                s = 2;              // Skip "da", which we represent by '㍲'.
            }
            unit = Units.get(uom.substring(s));
            if (AbstractUnit.isPrefixable(unit)) {
                LinearConverter c = Prefixes.converter(prefix);
                if (c != null) {
                    String symbol = unit.getSymbol();
                    final int power = ConventionalUnit.power(symbol);
                    if (power != 0) {
                        if (power != 1) {
                            c = LinearConverter.pow(c, power, false);
                        }
                        symbol = Prefixes.concat(prefix, symbol);
                        return new ConventionalUnit<>((AbstractUnit<?>) unit, c, symbol.intern(), (byte) 0, (short) 0);
                    }
                }
            }
            unit = null;
        }
        return unit;
    }

    /**
     * If the given system unit should be replaced by pseudo-unit for the purpose of formatting,
     * returns that pseudo-unit. Otherwise returns {@code null}. This method is for handling the
     * Special case of {@link Units#KILOGRAM}, to be replaced by {@link Units#GRAM} so a prefix
     * can be computed. The kilogram may appear in an expression like "kg/m", which we want to
     * replace by "g/m". We do that by dividing the unit by 1000 (the converter for "milli" prefix).
     */
    @SuppressWarnings("unchecked")
    static <Q extends Quantity<Q>> ConventionalUnit<Q> pseudoSystemUnit(final SystemUnit<Q> unit) {
        if ((unit.scope & ~UnitRegistry.SI) == 0 && unit.dimension.numeratorIs('M')) {
            if (unit == Units.KILOGRAM) {
                return (ConventionalUnit<Q>) Units.GRAM;            // Optimization for a common case.
            } else {
                String symbol = unit.getSymbol();
                if (symbol != null && symbol.length() >= 3 && symbol.startsWith("kg")
                        && !AbstractUnit.isSymbolChar(symbol.codePointAt(2)))
                {
                    symbol = symbol.substring(1);
                    UnitConverter c = converter('m');
                    return new ConventionalUnit<>(unit, c, symbol, UnitRegistry.PREFIXABLE, (short) 0).unique(symbol);
                }
            }
        }
        return null;
    }
}
