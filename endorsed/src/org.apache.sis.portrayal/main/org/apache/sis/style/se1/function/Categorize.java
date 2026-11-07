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
import java.util.List;
import java.util.function.Function;
import org.opengis.filter.Expression;


/**
 * Transformation of continuous values to distinct values, for example colors.
 * The continuous values are divided in intervals by thresholds in ascending order,
 * and each interval is mapped to a value. With <var>n</var> thresholds, there is
 * <var>n</var>+1 intervals and therefore <var>n</var>+1 values. The first interval
 * ranges from −∞ to the first threshold and the last one from the last threshold to +∞.
 * Whether a lookup value equal to a threshold belongs to the preceding or succeeding interval
 * is specified by {@link #getThresholdsBelongTo()}.
 *
 * <p>The lookup value and the thresholds shall be of a uniform and orderable type. They are compared
 * as numbers if all thresholds are convertible to numbers, or as {@link Comparable} objects otherwise.
 * Lookup values which are missing or not comparable to the thresholds are mapped to the fallback value.</p>
 *
 * <p>In a {@link org.apache.sis.style.se1.ColorMap}, the lookup value is the raster data ({@code "Rasterdata"})
 * and the values are colors. This function is available by the name {@code "Categorize"} in Apache SIS
 * filter factories. The parameters are the lookup value followed by the values and thresholds in alternance,
 * starting and ending with a value, optionally followed by the {@code "succeeding"} or {@code "preceding"} keyword.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param <V>  the type of values computed by the function, for example {@link java.awt.Color}.
 */
public final class Categorize<R,V> extends AbstractLookupFunction<R,V> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4384102719263386413L;

    /**
     * The name of this function.
     */
    static final String NAME = "Categorize";

    /**
     * Whether values equal to a threshold belong to the interval before or after that threshold.
     */
    public enum ThresholdsBelongTo {
        /** A value equal to a threshold belongs to the interval after that threshold. This is the default. */
        SUCCEEDING,

        /** A value equal to a threshold belongs to the interval before that threshold. */
        PRECEDING
    }

    /**
     * The value of each interval, one more than the number of thresholds.
     */
    private final List<Expression<R, ? extends V>> values;

    /**
     * The thresholds between intervals, in ascending order.
     */
    private final List<Expression<R,?>> thresholds;

    /**
     * Whether values equal to thresholds belong to the preceding or succeeding interval.
     */
    private final ThresholdsBelongTo thresholdsBelongTo;

    /**
     * Creates a new categorization function.
     *
     * @param  lookupValue         the value to categorize, or {@code null} for {@code "Rasterdata"}.
     * @param  values              the value of each interval, one more than the number of thresholds.
     * @param  thresholds          the thresholds between intervals, in ascending order.
     * @param  thresholdsBelongTo  whether thresholds belong to the preceding or succeeding interval,
     *                             or {@code null} for the default value ({@code SUCCEEDING}).
     * @param  fallbackValue       value to use when the lookup value cannot be categorized, or {@code null} if none.
     * @throws IllegalArgumentException if the number of values is not the number of thresholds plus one.
     */
    public Categorize(final Expression<R,?> lookupValue,
                      final List<? extends Expression<R, ? extends V>> values,
                      final List<? extends Expression<R,?>> thresholds,
                      final ThresholdsBelongTo thresholdsBelongTo,
                      final Expression<R, ? extends V> fallbackValue)
    {
        super(NAME, Object.class, lookupValue, fallbackValue);
        this.values     = List.copyOf(values);
        this.thresholds = List.copyOf(thresholds);
        if (this.values.size() != this.thresholds.size() + 1) {
            throw new IllegalArgumentException("Expected " + (this.thresholds.size() + 1) + " values for "
                    + this.thresholds.size() + " thresholds, but got " + this.values.size() + '.');
        }
        this.thresholdsBelongTo = (thresholdsBelongTo != null) ? thresholdsBelongTo : ThresholdsBelongTo.SUCCEEDING;
    }

    /**
     * Returns the value of each interval, in the order of intervals.
     *
     * @return the values of intervals (never empty).
     */
    public List<Expression<R, ? extends V>> getValues() {
        return values;
    }

    /**
     * Returns the thresholds between intervals, in ascending order.
     *
     * @return the thresholds (may be empty).
     */
    public List<Expression<R,?>> getThresholds() {
        return thresholds;
    }

    /**
     * Returns whether values equal to thresholds belong to the preceding or succeeding interval.
     *
     * @return whether thresholds belong to the preceding or succeeding interval.
     */
    public ThresholdsBelongTo getThresholdsBelongTo() {
        return thresholdsBelongTo;
    }

    /**
     * Returns the parameters of this function: the lookup value, then the values and thresholds in alternance,
     * then the {@code "preceding"} keyword if thresholds belong to the preceding interval.
     * The fallback value is not included.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        final List<Expression<R,?>> parameters = new ArrayList<>(values.size() * 2 + 1);
        parameters.add(lookupValue);
        for (int i = 0; i < values.size(); i++) {
            if (i != 0) parameters.add(thresholds.get(i - 1));
            parameters.add(values.get(i));
        }
        if (thresholdsBelongTo != ThresholdsBelongTo.SUCCEEDING) {
            parameters.add(new Keyword<>(keyword(thresholdsBelongTo)));
        }
        return List.copyOf(parameters);
    }

    @Override
    Object[] properties() {
        return new Object[] {lookupValue, values, thresholds, thresholdsBelongTo, fallbackValue};
    }

    /**
     * Returns a function categorizing arbitrary lookup values, with thresholds and values evaluated
     * once on the given resource.
     *
     * @param  input  the resource on which to evaluate thresholds and values, may be null for literals.
     * @return a function categorizing lookup values.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Function<Object,V> bind(final R input) {
        final int n = thresholds.size();
        final Object[] bounds  = new Object[n];
        final double[] numbers = new double[n];
        boolean numeric = true;
        for (int i = 0; i < n; i++) {
            bounds[i]  = thresholds.get(i).apply(input);
            numbers[i] = number(bounds[i]);
            numeric &= !Double.isNaN(numbers[i]);
        }
        final List<V> results = new ArrayList<>(values.size());
        for (Expression<R, ? extends V> e : values) {
            results.add(e.apply(input));
        }
        final V fallback = fallback(input);
        final boolean succeeding = (thresholdsBelongTo == ThresholdsBelongTo.SUCCEEDING);
        if (numeric) {
            return (value) -> {
                final double x = number(value);
                if (Double.isNaN(x)) return fallback;
                int i = 0;
                while (i < n && (x > numbers[i] || (succeeding && x == numbers[i]))) i++;
                return results.get(i);
            };
        }
        return (value) -> {
            if (!(value instanceof Comparable)) return fallback;
            int i = 0;
            try {
                int c;
                while (i < n && ((c = ((Comparable<Object>) value).compareTo(bounds[i])) > 0 || (succeeding && c == 0))) i++;
            } catch (RuntimeException e) {      // ClassCastException or NullPointerException.
                return fallback;
            }
            return results.get(i);
        };
    }

    /**
     * Returns an equivalent function computing values of the given type.
     * The values and the fallback value are converted, including colors encoded as strings.
     *
     * @param  <N>     compile-time value of {@code target} type.
     * @param  target  desired type of expression results.
     * @return function computing values of the given type.
     */
    @Override
    public <N> Expression<R,N> toValueType(final Class<N> target) {
        final List<Expression<R,N>> converted = new ArrayList<>(values.size());
        for (Expression<R, ? extends V> e : values) {
            converted.add(convert(e, target));
        }
        return new Categorize<>(lookupValue, converted, thresholds, thresholdsBelongTo,
                (fallbackValue != null) ? convert(fallbackValue, target) : null);
    }
}
