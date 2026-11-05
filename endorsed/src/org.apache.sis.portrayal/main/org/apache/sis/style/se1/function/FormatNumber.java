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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * Formatting of numbers for making them human readable, typically in labels.
 * The pattern syntax and semantic are those of {@link DecimalFormat}:
 *
 * <ul>
 *   <li>{@code #}: digit, omitted if irrelevant (leading or trailing zero),</li>
 *   <li>{@code 0}: digit,</li>
 *   <li>{@code .}: decimal point,</li>
 *   <li>{@code ,}: grouping separator,</li>
 *   <li>{@code E}: exponent indicator,</li>
 *   <li>{@code -}: minus sign,</li>
 *   <li>{@code '}: apostrophe for quoting.</li>
 * </ul>
 *
 * As in XSLT, the characters for the decimal point and the grouping separator can be redefined by
 * {@link #getDecimalPoint()} and {@link #getGroupingSeparator()}, which default to {@code "."} and {@code ","}
 * respectively. The redefined characters are used both in the patterns and in the formatted numbers.
 * For example, the pattern {@code "#.##0,00"} with {@code ","} as decimal point and {@code "."} as grouping
 * separator formats 1234.5 as {@code "1.234,50"}. If there is no negative pattern, {@code "-"} is prefixed
 * to the pattern for negative numbers. The numeric value is converted to a number if necessary. Values which are missing or
 * not convertible to numbers give null.
 *
 * <p>This function is available by the name {@code "FormatNumber"} in Apache SIS filter factories,
 * with the numeric value and the pattern as parameters, optionally followed by the negative pattern
 * (an empty string for none), the decimal point and the grouping separator.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
public final class FormatNumber<R> extends SymbologyFunction<R,String> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1823904652018341729L;

    /**
     * The name of this function.
     */
    static final String NAME = "FormatNumber";

    /**
     * The number to format.
     */
    private final Expression<R,?> numericValue;

    /**
     * The pattern for positive numbers.
     */
    private final Expression<R,?> pattern;

    /**
     * The pattern for negative numbers, or {@code null} if none.
     */
    private final Expression<R,?> negativePattern;

    /**
     * The decimal point, or {@code null} for {@code "."}.
     */
    private final Expression<R,?> decimalPoint;

    /**
     * The grouping separator, or {@code null} for {@code ","}.
     */
    private final Expression<R,?> groupingSeparator;

    /**
     * Creates a new number formatting function.
     *
     * @param  numericValue       the number to format.
     * @param  pattern            the pattern for positive numbers.
     * @param  negativePattern    the pattern for negative numbers, or {@code null} if none.
     * @param  decimalPoint       the decimal point, or {@code null} for {@code "."}.
     * @param  groupingSeparator  the grouping separator, or {@code null} for {@code ","}.
     */
    public FormatNumber(final Expression<R,?> numericValue, final Expression<R,?> pattern,
                        final Expression<R,?> negativePattern,
                        final Expression<R,?> decimalPoint, final Expression<R,?> groupingSeparator)
    {
        super(NAME, String.class);
        this.numericValue      = Objects.requireNonNull(numericValue, "numericValue");
        this.pattern           = Objects.requireNonNull(pattern, "pattern");
        this.negativePattern   = negativePattern;
        this.decimalPoint      = decimalPoint;
        this.groupingSeparator = groupingSeparator;
    }

    /**
     * Returns the number to format.
     *
     * @return the numeric value.
     */
    public Expression<R,?> getNumericValue() {
        return numericValue;
    }

    /**
     * Returns the pattern for positive numbers.
     *
     * @return the pattern.
     */
    public Expression<R,?> getPattern() {
        return pattern;
    }

    /**
     * Returns the pattern for negative numbers.
     *
     * @return the negative pattern, or empty for prefixing the pattern with {@code "-"}.
     */
    public Optional<Expression<R,?>> getNegativePattern() {
        return Optional.ofNullable(negativePattern);
    }

    /**
     * Returns the decimal point.
     *
     * @return the decimal point, or empty for {@code "."}.
     */
    public Optional<Expression<R,?>> getDecimalPoint() {
        return Optional.ofNullable(decimalPoint);
    }

    /**
     * Returns the grouping separator.
     *
     * @return the grouping separator, or empty for {@code ","}.
     */
    public Optional<Expression<R,?>> getGroupingSeparator() {
        return Optional.ofNullable(groupingSeparator);
    }

    /**
     * Returns the parameters of this function: the numeric value and the pattern, followed by the
     * negative pattern (an empty string for none), the decimal point and the grouping separator if specified.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        final List<Expression<R,?>> parameters = new ArrayList<>(5);
        parameters.add(numericValue);
        parameters.add(pattern);
        if (negativePattern != null || decimalPoint != null || groupingSeparator != null) {
            parameters.add((negativePattern != null) ? negativePattern : new Keyword<>(""));
        }
        if (decimalPoint != null || groupingSeparator != null) {
            parameters.add((decimalPoint != null) ? decimalPoint : new Keyword<>("."));
        }
        if (groupingSeparator != null) {
            parameters.add(groupingSeparator);
        }
        return List.copyOf(parameters);
    }

    /**
     * Formats the numeric value evaluated on the given resource.
     *
     * @param  input  the resource on which to evaluate the expressions.
     * @return the formatted number, or {@code null} if the value is missing, not a number or the pattern is invalid.
     */
    @Override
    public String apply(final R input) {
        final double value = number(numericValue.apply(input));
        final String positive = string(pattern.apply(input));
        if (Double.isNaN(value) || positive == null) {
            return null;
        }
        String p = positive;
        if (negativePattern != null) {
            final String negative = string(negativePattern.apply(input));
            if (negative != null && !negative.isEmpty()) {
                p = positive + ';' + negative;
            }
        }
        final var symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        symbols.setDecimalSeparator(character(decimalPoint, input, '.'));
        symbols.setGroupingSeparator(character(groupingSeparator, input, ','));
        try {
            final var format = new DecimalFormat("", symbols);
            format.applyLocalizedPattern(p);
            return format.format(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Evaluates the given optional expression as a single character.
     */
    private char character(final Expression<R,?> exp, final R input, final char fallback) {
        final String s = (exp != null) ? string(exp.apply(input)) : null;
        return (s != null && !s.isEmpty()) ? s.charAt(0) : fallback;
    }

    @Override
    Object[] properties() {
        return new Object[] {numericValue, pattern, negativePattern, decimalPoint, groupingSeparator};
    }
}
