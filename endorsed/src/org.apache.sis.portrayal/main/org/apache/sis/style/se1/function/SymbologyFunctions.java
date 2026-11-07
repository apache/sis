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
package org.apache.sis.style.se1.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.opengis.util.LocalName;
import org.opengis.util.TypeName;
import org.apache.sis.filter.FunctionRegister;
import org.apache.sis.util.iso.Names;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.capability.AvailableFunction;


/**
 * Provider of the functions defined by Symbology Encoding: the transformation functions
 * {@code "Categorize"}, {@code "Interpolate"} and {@code "Recode"}, and the formatting functions
 * {@code "FormatNumber"}, {@code "FormatDate"}, {@code "Substring"}, {@code "Concatenate"},
 * {@code "ChangeCase"}, {@code "Trim"}, {@code "StringPosition"} and {@code "StringLength"}.
 * This provider is discovered by {@link java.util.ServiceLoader} and makes those functions available
 * through {@link org.opengis.filter.FilterFactory#function(String, Expression...)}.
 *
 * @author  Johann Sorel (Geomatys)
 *
 * @see Categorize
 * @see Interpolate
 * @see Recode
 * @see FormatNumber
 * @see FormatDate
 * @see Substring
 * @see Concatenate
 * @see ChangeCase
 * @see Trim
 * @see StringPosition
 * @see StringLength
 */
public final class SymbologyFunctions implements FunctionRegister {
    /**
     * Creates a new provider. This constructor is invoked by {@link java.util.ServiceLoader}.
     */
    public SymbologyFunctions() {
    }

    /**
     * Returns the authority of the functions, which is {@code "SE"} for Symbology Encoding.
     *
     * @return {@code "SE"}.
     */
    @Override
    public String getAuthority() {
        return "SE";
    }

    /**
     * Names of all functions provided by this register.
     */
    private static final Set<String> NAMES = Set.of(
            Categorize.NAME,
            Interpolate.NAME,
            Recode.NAME,
            FormatNumber.NAME,
            FormatDate.NAME,
            Substring.NAME,
            Concatenate.NAME,
            ChangeCase.NAME,
            Trim.NAME,
            StringPosition.NAME,
            StringLength.NAME);

    /**
     * Returns the names of the functions provided by this register.
     *
     * @return the names of all Symbology Encoding functions.
     */
    @Override
    public Set<String> getNames() {
        return NAMES;
    }

    /**
     * Describes the function of the given name.
     *
     * @param  name  name of a Symbology Encoding function.
     * @return description of the function.
     * @throws IllegalArgumentException if the given name is not recognized.
     */
    @Override
    public AvailableFunction describe(final String name) {
        final String canonical = canonical(name);
        return new AvailableFunction() {
            @Override public LocalName getName()       {return Names.createLocalName(null, null, canonical);}
            @Override public TypeName  getReturnType() {return Names.createTypeName(Object.class);}
        };
    }

    /**
     * Returns the canonical name of the given function name.
     */
    private static String canonical(final String name) {
        for (String n : NAMES) {
            if (n.equalsIgnoreCase(name)) return n;
        }
        throw new IllegalArgumentException("Unknown function: " + name);
    }

    /**
     * Creates a function of the given name.
     *
     * <ul>
     *   <li>{@code Categorize(lookup, value, threshold, value, …, value [, "succeeding"|"preceding"])}</li>
     *   <li>{@code Interpolate(lookup, data, value, data, value, … [, mode] [, method])} where mode is
     *       {@code "linear"}, {@code "cosine"} or {@code "cubic"} and method is {@code "color"} or {@code "numeric"}.</li>
     *   <li>{@code Recode(lookup, data, value, data, value, …)}</li>
     *   <li>{@code FormatNumber(value, pattern [, negativePattern [, decimalPoint [, groupingSeparator]]])}</li>
     *   <li>{@code FormatDate(value [, pattern])}</li>
     *   <li>{@code Substring(string [, position [, length]])}</li>
     *   <li>{@code Concatenate(string, string, …)}</li>
     *   <li>{@code ChangeCase(string [, "toUpper"|"toLower"])}</li>
     *   <li>{@code Trim(string [, "leading"|"trailing"|"both" [, stripOffChar]])}</li>
     *   <li>{@code StringPosition(lookupString, string [, "frontToBack"|"backToFront"])}</li>
     *   <li>{@code StringLength(string)}</li>
     * </ul>
     *
     * @param  <R>         the type of resources used as inputs.
     * @param  name        name of a Symbology Encoding function.
     * @param  parameters  the function parameters.
     * @return the function.
     * @throws IllegalArgumentException if the name is not recognized or the parameters are invalid.
     */
    @Override
    public <R> Expression<R,?> create(final String name, final Expression<R,?>[] parameters) {
        final String fn = canonical(name);
        final int n = parameters.length;
        verify(fn, n != 0, "at least one parameter");
        final List<Expression<R,?>> args = new ArrayList<>(Arrays.asList(parameters));
        switch (fn) {
            case Categorize.NAME: {
                final var belongTo = trailing(args, Categorize.ThresholdsBelongTo.values());
                verify(fn, (args.size() & 1) == 0, "values and thresholds in alternance, starting and ending with a value");
                final var values     = new ArrayList<Expression<R,?>>();
                final var thresholds = new ArrayList<Expression<R,?>>();
                for (int i = 1; i < args.size(); i++) {
                    ((i & 1) != 0 ? values : thresholds).add(args.get(i));
                }
                return new Categorize<R,Object>(args.get(0), values, thresholds, belongTo, null);
            }
            case Interpolate.NAME: {
                var method = trailing(args, Interpolate.Method.values());
                final var mode = trailing(args, Interpolate.Mode.values());
                if (method == null) method = trailing(args, Interpolate.Method.values());
                verify(fn, (args.size() & 1) != 0, "data and values in alternance");
                final var points = new ArrayList<Interpolate.InterpolationPoint<R>>();
                for (int i = 1; i < args.size(); i += 2) {
                    points.add(new Interpolate.InterpolationPoint<>(SymbologyFunction.convert(args.get(i), Number.class), args.get(i + 1)));
                }
                return new Interpolate<>(args.get(0), points, mode, method, null);
            }
            case Recode.NAME: {
                verify(fn, (n & 1) != 0, "data and values in alternance");
                final var items = new ArrayList<Recode.MapItem<R>>();
                for (int i = 1; i < n; i += 2) {
                    items.add(new Recode.MapItem<>(parameters[i], parameters[i + 1]));
                }
                return new Recode<>(parameters[0], items, null);
            }
            case FormatNumber.NAME: {
                count(fn, n, 2, 5);
                return new FormatNumber<>(parameters[0], parameters[1], optional(parameters, 2), optional(parameters, 3), optional(parameters, 4));
            }
            case FormatDate.NAME: {
                count(fn, n, 1, 2);
                return new FormatDate<>(parameters[0], optional(parameters, 1));
            }
            case Substring.NAME: {
                count(fn, n, 1, 3);
                return new Substring<>(parameters[0], optional(parameters, 1), optional(parameters, 2));
            }
            case Concatenate.NAME: {
                return new Concatenate<>(args);
            }
            case ChangeCase.NAME: {
                count(fn, n, 1, 2);
                return new ChangeCase<>(parameters[0], (n > 1) ? keyword(fn, parameters[1], ChangeCase.Direction.values()) : null);
            }
            case Trim.NAME: {
                count(fn, n, 1, 3);
                return new Trim<>(parameters[0], (n > 1) ? keyword(fn, parameters[1], Trim.StripOffPosition.values()) : null,
                                  optional(parameters, 2));
            }
            case StringPosition.NAME: {
                count(fn, n, 2, 3);
                return new StringPosition<>(parameters[0], parameters[1],
                        (n > 2) ? keyword(fn, parameters[2], StringPosition.SearchDirection.values()) : null);
            }
            default: {
                count(fn, n, 1, 1);
                return new StringLength<>(parameters[0]);
            }
        }
    }

    /**
     * Throws an exception if the parameters of the given function are invalid.
     *
     * @param  fn        name of the function.
     * @param  valid     whether the parameters are valid.
     * @param  expected  description of the expected parameters.
     */
    private static void verify(final String fn, final boolean valid, final String expected) {
        if (!valid) {
            throw new IllegalArgumentException("The " + fn + " function expects " + expected + '.');
        }
    }

    /**
     * Verifies that the number of parameters is in the given range.
     */
    private static void count(final String fn, final int n, final int min, final int max) {
        verify(fn, n >= min && n <= max, (min == max ? String.valueOf(min) : min + " to " + max) + " parameters, but got " + n);
    }

    /**
     * Returns the parameter at the given index, or {@code null} if the index is out of bounds.
     */
    private static <R> Expression<R,?> optional(final Expression<R,?>[] parameters, final int index) {
        return (index < parameters.length) ? parameters[index] : null;
    }

    /**
     * Returns the enumeration value designated by the given parameter.
     *
     * @throws IllegalArgumentException if the parameter is not a literal matching one of the given values.
     */
    private static <E extends Enum<E>> E keyword(final String fn, final Expression<?,?> parameter, final E[] values) {
        final E value = match(parameter, values);
        verify(fn, value != null, "one of " + Arrays.toString(values) + " instead of " + parameter);
        return value;
    }

    /**
     * Removes and returns the last parameter if it is a literal matching one of the given values.
     * Otherwise returns {@code null} without modifying the list. The first parameter is never removed.
     */
    private static <E extends Enum<E>> E trailing(final List<? extends Expression<?,?>> args, final E[] values) {
        final E value = (args.size() > 1) ? match(args.get(args.size() - 1), values) : null;
        if (value != null) {
            args.remove(args.size() - 1);
        }
        return value;
    }

    /**
     * Returns the enumeration value having the Symbology Encoding keyword given by the parameter, ignoring case.
     * Returns {@code null} if the parameter is not a literal matching one of the given values.
     */
    private static <E extends Enum<E>> E match(final Expression<?,?> parameter, final E[] values) {
        if (parameter instanceof Literal) {
            final Object value = ((Literal<?,?>) parameter).getValue();
            if (value instanceof CharSequence) {
                final String text = value.toString().trim();
                for (final E e : values) {
                    if (SymbologyFunction.keyword(e).equalsIgnoreCase(text)) return e;
                }
            }
        }
        return null;
    }
}
