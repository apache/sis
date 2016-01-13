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
package org.apache.sis.math;

import java.io.Serializable;
import org.opengis.util.InternationalString;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.Types;

import static java.lang.Math.*;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.lang.Double.doubleToLongBits;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.apache.sis.internal.jdk8.LongConsumer;
import org.apache.sis.internal.jdk8.DoubleConsumer;


/**
 * Holds some statistics derived from a series of sample values.
 * Given a series of <var>y₀</var>, <var>y₁</var>, <var>y₂</var>, <var>y₃</var>, <i>etc…</i> samples,
 * this class computes the {@linkplain #minimum() minimum}, {@linkplain #maximum() maximum},
 * {@linkplain #mean() mean}, {@linkplain #rms() root mean square} and
 * {@linkplain #standardDeviation(boolean) standard deviation} of the given samples.
 *
 * <p>In addition to the statistics on the sample values, this class can optionally compute
 * statistics on the differences between consecutive sample values, i.e. the statistics on
 * <var>y₁</var>-<var>y₀</var>, <var>y₂</var>-<var>y₁</var>, <var>y₃</var>-<var>y₂</var>, <i>etc…</i>,
 * Those statistics can be fetched by a call to {@link #differences()}.
 * They are useful for verifying if the interval between sample values is approximatively constant.</p>
 *
 * <p>If the samples are (at least conceptually) the result of some <var>y</var>=<var>f</var>(<var>x</var>)
 * function for <var>x</var> values increasing or decreasing at a constant interval Δ<var>x</var>,
 * then one can get the statistics on the <cite>discrete derivatives</cite> by a call to
 * <code>differences().{@linkplain #scale(double) scale}(1/Δx)</code>.</p>
 *
 * <p>Statistics are computed on the fly using the
 * <a href="http://en.wikipedia.org/wiki/Kahan_summation_algorithm">Kahan summation algorithm</a>
 * for reducing the numerical errors; the sample values are never stored in memory.</p>
 *
 * <p>An instance of {@code Statistics} is initially empty: the {@linkplain #count() count} of
 * values is set to zero, and all above-cited statistical values are set to {@link Double#NaN NaN}.
 * The statistics are updated every time an {@link #accept(double)} method is invoked with a non-NaN
 * value.</p>
 *
 * <div class="section">Examples</div>
 * The following examples assume that a <var>y</var>=<var>f</var>(<var>x</var>) function
 * is defined. A simple usage is:
 *
 * {@preformat java
 *     Statistics stats = new Statistics("y");
 *     for (int i=0; i<numberOfValues; i++) {
 *         stats.accept(f(i));
 *     }
 *     System.out.println(stats);
 * }
 *
 * Following example computes the statistics on the first and second derivatives
 * in addition to the statistics on the sample values:
 *
 * {@preformat java
 *     final double x₀ = ...; // Put here the x value at i=0
 *     final double Δx = ...; // Put here the interval between x values
 *     Statistics stats = Statistics.forSeries("y", "∂y/∂x", "∂²y/∂x²");
 *     for (int i=0; i<numberOfValues; i++) {
 *         stats.accept(f(x₀ + i*Δx));
 *     }
 *     stats.differences().scale(1/Δx);
 *     System.out.println(stats);
 * }
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class Statistics implements DoubleConsumer, LongConsumer, Cloneable, Serializable {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 8495118253884975477L;

    /**
     * The name of the phenomenon for which this object is collecting statistics.
     * If non-null, then this name will be shown as column header in the table formatted
     * by {@link StatisticsFormat}.
     *
     * @see #name()
     */
    private final InternationalString name;

    /**
     * The minimal value given to the {@link #accept(double)} method.
     */
    private double minimum = NaN;

    /**
     * The maximal value given to the {@link #accept(double)} method.
     */
    private double maximum = NaN;

    /**
     * The sum of all values given to the {@link #accept(double)} method.
     */
    private double sum;

    /**
     * The sum of square of all values given to the {@link #accept(double)} method.
     */
    private double squareSum;

    /**
     * The low-order bits in last update of {@link #sum}.
     * This is used for the Kahan summation algorithm.
     */
    private transient double lowBits;

    /**
     * The low-order bits in last update of {@link #squareSum}.
     * This is used for the Kahan summation algorithm.
     */
    private transient double squareLowBits;

    /**
     * Number of non-NaN values given to the {@link #accept(double)} method.
     */
    private int count;

    /**
     * Number of NaN values given to the {@link #accept(double)} method.
     * Those value are ignored in the computation of all above values.
     */
    private int countNaN;

    /**
     * Constructs an initially empty set of statistics.
     * The {@linkplain #count()} and the {@link #sum()} are initialized to zero
     * and all other statistical values are initialized to {@link Double#NaN}.
     *
     * <p>Instances created by this constructor do not compute differences between sample values.
     * If differences or discrete derivatives are wanted, use the {@link #forSeries forSeries(…)}
     * method instead.</p>
     *
     * @param name The phenomenon for which this object is collecting statistics, or {@code null}
     *             if none. If non-null, then this name will be shown as column header in the table
     *             formatted by {@link StatisticsFormat}.
     */
    public Statistics(final CharSequence name) {
        this.name = Types.toInternationalString(name);
    }

    /**
     * Constructs a new {@code Statistics} object which will also compute finite differences
     * up to the given order. If the values to be given to the {@code accept(…)} methods are
     * the <var>y</var> values of some <var>y</var>=<var>f</var>(<var>x</var>) function for
     * <var>x</var> values increasing or decreasing at a constant interval Δ<var>x</var>,
     * then the finite differences are proportional to discrete derivatives.
     *
     * <p>The {@code Statistics} object created by this method know nothing about the Δ<var>x</var>
     * interval. In order to get the discrete derivatives, the following method needs to be invoked
     * <em>after</em> all sample values have been added:</p>
     *
     * {@preformat java
     *     statistics.differences().scale(1/Δx);
     * }
     *
     * The maximal "derivative" order is determined by the length of the {@code differenceNames} array:
     *
     * <ul>
     *   <li>0 if no differences are needed (equivalent to direct instantiation of a new
     *       {@code Statistics} object).</li>
     *   <li>1 for computing the statistics on the differences between consecutive samples
     *       (proportional to the statistics on the first discrete derivatives) in addition
     *       to the sample statistics.</li>
     *   <li>2 for computing also the statistics on the differences between consecutive differences
     *       (proportional to the statistics on the second discrete derivatives) in addition to the
     *       above.</li>
     *   <li><i>etc</i>.</li>
     * </ul>
     *
     *
     *
     * @param  name  The phenomenon for which this object is collecting statistics, or {@code null}
     *               if none. If non-null, then this name will be shown as column header in the table
     *               formatted by {@link StatisticsFormat}.
     * @param  differenceNames The names of the statistics on differences.
     *         The given array can not be null, but can contain null elements.
     * @return The newly constructed, initially empty, set of statistics.
     *
     * @see #differences()
     */
    public static Statistics forSeries(final CharSequence name, final CharSequence... differenceNames) {
        ArgumentChecks.ensureNonNull("differenceNames", differenceNames);
        Statistics stats = null;
        for (int i=differenceNames.length; --i >= -1;) {
            final CharSequence n = (i >= 0) ? differenceNames[i] : name;
            stats = (stats == null) ? new Statistics(n) : new WithDelta(n, stats);
        }
        return stats;
    }

    /**
     * Returns the name of the phenomenon for which this object is collecting statistics.
     * If non-null, then this name will be shown as column header in the table formatted
     * by {@link StatisticsFormat}.
     *
     * @return The phenomenon for which this object is collecting statistics, or {@code null} if none.
     */
    public InternationalString name() {
        return name;
    }

    /**
     * Resets this object state as if it was just created.
     * The {@linkplain #count()} and the {@link #sum()} are set to zero
     * and all other statistical values are set to {@link Double#NaN}.
     */
    public void reset() {
        minimum       = NaN;
        maximum       = NaN;
        sum           = 0;
        squareSum     = 0;
        lowBits       = 0;
        squareLowBits = 0;
        count         = 0;
        countNaN      = 0;
    }

    /**
     * Updates statistics for the specified floating-point sample value.
     * {@link Double#NaN NaN} values increment the {@linkplain #countNaN() NaN count},
     * but are otherwise ignored.
     *
     * @param sample The sample value (may be NaN).
     *
     * @see #accept(long)
     * @see #combine(Statistics)
     */
    @Override
    public void accept(final double sample) {
        if (isNaN(sample)) {
            countNaN++;
        } else {
            real(sample);
        }
    }

    /**
     * Implementation of {@link #accept(double)} for real (non-NaN) numbers.
     */
    private void real(double sample) {
        // Two next lines use !(a >= b) instead than
        // (a < b) in order to take NaN in account.
        if (!(minimum <= sample)) minimum = sample;
        if (!(maximum >= sample)) maximum = sample;

        // According algebraic laws, lowBits should always been zero. But it is
        // not when using floating points with limited precision. Do not simplify!
        double y = sample + lowBits;
        lowBits = y + (sum - (sum += y));

        sample *= sample;
        y = sample + squareLowBits;
        squareLowBits = y + (squareSum - (squareSum += y));

        count++;
    }

    /**
     * Updates statistics for the specified integer sample value.
     * For very large integer values (greater than 2<sup>52</sup> in magnitude),
     * this method may be more accurate than the {@link #accept(double)} version.
     *
     * @param sample The sample value.
     *
     * @see #accept(double)
     * @see #combine(Statistics)
     */
    @Override
    public void accept(final long sample) {
        real(sample);
    }

    /**
     * Updates statistics with all samples from the specified {@code stats}.
     * Invoking this method is equivalent (except for rounding errors) to invoking
     * {@link #accept(double) accept(…)} for all samples that were added to {@code stats}.
     *
     * @param stats The statistics to be added to {@code this}.
     */
    public void combine(final Statistics stats) {
        ArgumentChecks.ensureNonNull("stats", stats);

        // "if (a < b)" is equivalent to "if (!isNaN(a) && a < b)".
        if (isNaN(minimum) || stats.minimum < minimum) minimum = stats.minimum;
        if (isNaN(maximum) || stats.maximum > maximum) maximum = stats.maximum;

        double y = stats.sum + lowBits;
        lowBits = y + (sum - (sum += y)) + stats.lowBits;

        y = stats.squareSum + squareLowBits;
        squareLowBits = y + (squareSum - (squareSum += y)) + stats.squareLowBits;

        count += stats.count;
        countNaN += max(stats.countNaN, 0);
    }

    /**
     * Multiplies the statistics by the given factor. The given scale factory is also applied
     * recursively on the {@linkplain #differences() differences} statistics, if any.
     * Invoking this method transforms the statistics as if every values given to the
     * {@code accept(…)} had been first multiplied by the given factor.
     *
     * <p>This method is useful for computing discrete derivatives from the differences between
     * sample values. See {@link #differences()} or {@link #forSeries forSeries(…)} for more
     * information.</p>
     *
     * @param factor The factor by which to multiply the statistics.
     */
    public void scale(double factor) {
        ArgumentChecks.ensureFinite("factor", factor);
        minimum       *= factor;
        maximum       *= factor;
        sum           *= factor;
        lowBits       *= factor;
        factor        *= factor;
        squareSum     *= factor;
        squareLowBits *= factor;
    }

    /**
     * For {@link WithDelta} usage only.
     */
    void decrementCountNaN() {
        countNaN--;
    }

    /**
     * Returns the number of {@link Double#NaN NaN} samples.
     * {@code NaN} samples are ignored in all other statistical computation.
     * This method count them for information purpose only.
     *
     * @return The number of NaN values.
     */
    public int countNaN() {
        return max(countNaN, 0); // The Delta subclass initializes countNaN to -1.
    }

    /**
     * Returns the number of samples, excluding {@link Double#NaN NaN} values.
     *
     * @return The number of sample values, excluding NaN.
     */
    public int count() {
        return count;
    }

    /**
     * Returns the minimum sample value, or {@link Double#NaN NaN} if none.
     *
     * @return The minimum sample value, or NaN if none.
     */
    public double minimum() {
        return minimum;
    }

    /**
     * Returns the maximum sample value, or {@link Double#NaN NaN} if none.
     *
     * @return The maximum sample value, or NaN if none.
     */
    public double maximum() {
        return maximum;
    }

    /**
     * Equivalents to <code>{@link #maximum() maximum} - {@link #minimum() minimum}</code>.
     * If no samples were added, then returns {@link Double#NaN NaN}.
     *
     * @return The span of sample values, or NaN if none.
     */
    public double span() {
        return maximum - minimum;
    }

    /**
     * Returns the sum, or 0 if none.
     *
     * @return The sum, or 0 if none.
     */
    public double sum() {
        return sum;
    }

    /**
     * Returns the mean value, or {@link Double#NaN NaN} if none.
     *
     * @return The mean value, or NaN if none.
     */
    public double mean() {
        return sum / count;
    }

    /**
     * Returns the root mean square, or {@link Double#NaN NaN} if none.
     *
     * @return The root mean square, or NaN if none.
     */
    public double rms() {
        return sqrt(squareSum / count);
    }

    /**
     * Returns the standard deviation. If the sample values given to the {@code accept(…)}
     * methods have a uniform distribution, then the returned value should be close to
     * <code>sqrt({@linkplain #span() span}² / 12)</code>. If they have a
     * Gaussian distribution (which is the most common case), then the returned value
     * is related to the <a href="http://en.wikipedia.org/wiki/Error_function">error
     * function</a>.
     *
     * <p>As a reminder, the table below gives the probability for a sample value to be
     * inside the {@linkplain #mean() mean} ± <var>n</var> × <var>deviation range</var>,
     * assuming that the distribution is Gaussian (first column) or assuming that the
     * distribution is uniform (second column).</p>
     *
     * <table class="sis" summary="Propability values for some standard deviations.">
     *   <tr><th>n</th><th>Gaussian</th><th>uniform</th>
     *   <tr><td>0.5</td><td>69.1%</td><td>28.9%</td></tr>
     *   <tr><td>1.0</td><td>84.2%</td><td>57.7%</td></tr>
     *   <tr><td>1.5</td><td>93.3%</td><td>86.6%</td></tr>
     *   <tr><td>2.0</td><td>97.7%</td><td>100%</td></tr>
     *   <tr><td>3.0</td><td>99.9%</td><td>100%</td></tr>
     * </table>
     *
     * @param allPopulation
     *          {@code true} if sample values given to {@code accept(…)} methods were the totality
     *          of the population under study, or {@code false} if they were only a sampling.
     * @return  The standard deviation.
     */
    public double standardDeviation(final boolean allPopulation) {
        return sqrt((squareSum - sum*sum/count) / (allPopulation ? count : count-1));
    }

    /**
     * Returns the statistics on the differences between sample values, or {@code null} if none.
     * For example if the sample values given to the {@code accept(…)} methods were <var>y₀</var>,
     * <var>y₁</var>, <var>y₂</var> and <var>y₃</var>, then this method returns statistics on
     * <var>y₁</var>-<var>y₀</var>, <var>y₂</var>-<var>y₁</var> and <var>y₃</var>-<var>y₂</var>.
     *
     * <p>The differences between sample values are related to the discrete derivatives as below,
     * where Δ<var>x</var> is the constant interval between the <var>x</var> values of the
     * <var>y</var>=<var>f</var>(<var>x</var>) function:</p>
     *
     * {@preformat java
     *     Statistics derivative = statistics.differences();
     *     derivative.scale(1/Δx); // Shall be invoked only once.
     *     Statistics secondDerivative = derivative.differences();
     *     // Do not invoke scale(1/Δx) again.
     * }
     *
     * This method returns a non-null value only if this {@code Statistics} instance has been created by a
     * call to the {@link #forSeries forSeries(…)} method with a non-empty {@code differenceNames} array.
     * More generally, calls to this method can be chained up to {@code differenceNames.length} times for
     * fetching second or higher order derivatives, as in the above example.
     *
     * @return The statistics on the differences between consecutive sample values,
     *         or {@code null} if not calculated by this object.
     *
     * @see #forSeries(CharSequence, CharSequence[])
     * @see #scale(double)
     */
    public Statistics differences() {
        return null;
    }

    /**
     * Returns a string representation of this statistics. This string will span
     * multiple lines, one for each statistical value. For example:
     *
     * {@preformat text
     *     Number of values:     8726
     *     Minimum value:       6.853
     *     Maximum value:       8.259
     *     Mean value:          7.421
     *     Root Mean Square:    7.846
     *     Standard deviation:  6.489
     * }
     *
     * @return A string representation of this statistics object.
     *
     * @see StatisticsFormat
     */
    @Override
    public String toString() {
        return StatisticsFormat.getInstance().format(this);
    }

    /**
     * Returns a clone of this statistics.
     *
     * @return A clone of this statistics.
     */
    @Override
    public Statistics clone() {
        try {
            return (Statistics) super.clone();
        } catch (CloneNotSupportedException exception) {
            // Should not happen since we are cloneable
            throw new AssertionError(exception);
        }
    }

    /**
     * Returns a hash code value for this statistics.
     */
    @Override
    public int hashCode() {
        final long code = (doubleToLongBits(minimum) +
                     31 * (doubleToLongBits(maximum) +
                     31 * (doubleToLongBits(sum) +
                     31 * (doubleToLongBits(squareSum)))));
        return (int) code ^ (int) (code >>> 32) ^ count;
    }

    /**
     * Compares this statistics with the specified object for equality.
     *
     * @param  object The object to compare with.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && getClass() == object.getClass()) {
            final Statistics cast = (Statistics) object;
            return Objects.equals(name, cast.name)
                    && count == cast.count && countNaN == cast.countNaN
                    && doubleToLongBits(minimum)   == doubleToLongBits(cast.minimum)
                    && doubleToLongBits(maximum)   == doubleToLongBits(cast.maximum)
                    && doubleToLongBits(sum)       == doubleToLongBits(cast.sum)
                    && doubleToLongBits(squareSum) == doubleToLongBits(cast.squareSum);
        }
        return false;
    }




    /**
     * Holds some statistics about the difference between consecutive sample values.
     * Given a series of <var>s₀</var>, <var>s₁</var>, <var>s₂</var>, <var>s₃</var>,
     * <i>etc…</i> samples, this class computes statistics for <var>s₁</var>-<var>s₀</var>,
     * <var>s₂</var>-<var>s₁</var>, <var>s₃</var>-<var>s₂</var>, <i>etc…</i>
     * which are stored in a {@link #delta} statistics object.
     *
     * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    private static final class WithDelta extends Statistics {
        /**
         * Serial number for compatibility with different versions.
         */
        private static final long serialVersionUID = -5149634417399815874L;

        /**
         * Statistics about the differences between consecutive sample values.
         * Consider this field as final; it is modified only by the {@link #clone()} method.
         */
        private Statistics delta;

        /**
         * Last value given to an {@link #accept(double)} method as
         * a {@code double}, or {@link Double#NaN} if none.
         */
        private double last = NaN;

        /**
         * Last value given to an {@link #accept(long)}
         * method as a {@code long}, or 0 if none.
         */
        private long lastAsLong;

        /**
         * Constructs an initially empty set of statistics using the specified object for
         * {@link #delta} statistics. This constructor allows chaining different kind of
         * statistics objects. For example, one could write:
         *
         * {@preformat java
         *     new Statistics.Delta(new Statistics.Delta());
         * }
         *
         * which would compute statistics of sample values, statistics of difference between
         * consecutive sample values, and statistics of difference of difference between
         * consecutive sample values. Other kinds of {@link Statistics} object could be
         * chained as well.
         *
         * @param name  The phenomenon for which this object is collecting statistics, or {@code null}.
         * @param delta The object where to stores delta statistics.
         */
        WithDelta(final CharSequence name, final Statistics delta) {
            super(name);
            this.delta = delta;
            delta.decrementCountNaN(); // Do not count the first NaN, which will always be the first value.
        }

        /**
         * Resets this object state as if it was just created.
         */
        @Override
        public void reset() {
            super.reset();
            delta.reset();
            delta.decrementCountNaN(); // Do not count the first NaN, which will always be the first value.
            last       = NaN;
            lastAsLong = 0;
        }

        /**
         * Updates statistics for the specified sample value and its discrete derivatives.
         * The {@link #delta} statistics are updated with <code>sample - sample<sub>last</sub></code>
         * value, where <code>sample<sub>last</sub></code> is the value given to the previous call of
         * an {@code accept(…)} method.
         */
        @Override
        public void accept(final double sample) {
            super.accept(sample);
            delta.accept(sample - last);
            last       = sample;
            lastAsLong = (long) sample;
        }

        /**
         * Performs the same work than {@link #accept(double)}, but with greater precision for
         * very large integer values (greater than 2<sup>52</sup> in magnitude),
         */
        @Override
        public void accept(final long sample) {
            super.accept(sample);
            if (last == (double) lastAsLong) {
                // 'lastAsLong' may have more precision than 'last' since the cast to the
                // 'double' type may loose some digits. Invoke the 'delta.accept(long)' version.
                delta.accept(sample - lastAsLong);
            } else {
                // The sample value is either fractional, outside 'long' range,
                // infinity or NaN. Invoke the 'delta.accept(double)' version.
                delta.accept(sample - last);
            }
            last       = sample;
            lastAsLong = sample;
        }

        /**
         * Update statistics with all samples from the specified {@code stats}.
         *
         * @throws ClassCastException If {@code stats} is not an instance of
         *         {@code Statistics.Delta}.
         */
        @Override
        public void combine(final Statistics stats) throws ClassCastException {
            ArgumentChecks.ensureNonNull("stats", stats);
            delta.combine(stats.differences());
            super.combine(stats);
            if (stats instanceof WithDelta) {
                final WithDelta toAdd = (WithDelta) stats;
                last       = toAdd.last;
                lastAsLong = toAdd.lastAsLong;
            } else {
                last       = NaN;
                lastAsLong = 0;
            }
        }

        /**
         * Scales the statistics by the given factor.
         */
        @Override
        public void scale(final double factor) {
            super.scale(factor);
            delta.scale(factor);
        }

        /**
         * Decrements the count of NaN values by one. This method is invoked on construction
         * or on {@link #reset()} call, because the first discrete derivative always have one
         * less value than the original one, the second derivative two less values, etc.
         */
        @Override
        final void decrementCountNaN() {
            super.decrementCountNaN();
            delta.decrementCountNaN();
        }

        /**
         * Returns the statistics about difference between consecutive values.
         */
        @Override
        public Statistics differences() {
            return delta;
        }

        /**
         * Returns a clone of this statistics.
         */
        @Override
        public Statistics clone() {
            final WithDelta copy = (WithDelta) super.clone();
            copy.delta = copy.delta.clone();
            return copy;
        }

        /**
         * Tests this statistics with the specified object for equality.
         */
        @Override
        public boolean equals(final Object obj) {
            return super.equals(obj) && delta.equals(((WithDelta) obj).delta);
        }

        /**
         * Returns a hash code value for this statistics.
         */
        @Override
        public int hashCode() {
            return super.hashCode() + 31*delta.hashCode();
        }
    }
}
