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
package org.apache.sis.util.logging;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import org.apache.sis.util.Configuration;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Logging levels for data processing with execution time measurements.
 * Those levels are used for events that would normally be logged at {@link Level#FINE},
 * but with the possibility to use a slightly higher level if execution time was long.
 * Different logging levels - {@link #SLOWNESS} and {@link #SLOWER} - are provided for logging
 * only the events taking more time than some thresholds. For example the console could log
 * only the slowest events, while a file could log all events considered slow.
 *
 * <p>Every levels defined in this class have a {@linkplain #intValue() value} between the
 * {@link Level#FINE} and {@link Level#CONFIG} values. Consequently performance logging are
 * disabled by default, and enabling them imply enabling configuration logging too. This is
 * done that way because the configuration typically have a significant impact on performance.</p>
 *
 * <h2>Enabling performance logging</h2>
 * Performance logging can be enabled in various ways. Among others:
 *
 * <ul>
 *   <li>The {@code $JAVA_HOME/lib/logging.properties} file can be edited in order to log
 *       messages at the {@code FINE} level, at least for the packages of interest.</li>
 *   <li>The {@link Logger#setLevel(Level)} can be invoked, together with
 *       {@link java.util.logging.Handler#setLevel(Level)} on all relevant logging targets
 *       (console or file, <i>etc.</i>).</li>
 *   <li>The {@link MonolineFormatter#install(Logger, Level)} convenience
 *       method can be invoked.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.3
 * @module
 */
public final class PerformanceLevel extends Level {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6547125008284983701L;

    /**
     * The level for logging relatively slow events. By default, only events having an execution
     * time equals or greater than 1 second are logged at this level. However this threshold can
     * be changed by a call to <code>SLOWNESS.{@linkplain #setMinDuration(long, TimeUnit)}</code>.
     *
     * @since 1.3
     */
    public static final PerformanceLevel SLOWNESS = new PerformanceLevel("SLOWNESS", Vocabulary.Keys.Slowness, 620, 1000_000_000L);

    /**
     * The level for logging only events slower than the ones logged at the {@link #SLOWNESS} level.
     * By default, only events having an execution time equals or greater than 10 seconds are
     * logged at this level. However this threshold can be changed by a call to
     * <code>SLOWER.{@linkplain #setMinDuration(long, TimeUnit)}</code>.
     */
    public static final PerformanceLevel SLOWER = new PerformanceLevel("SLOWER", Vocabulary.Keys.Slower, 630, 10_000_000_000L);

    /**
     * @deprecated Renamed {@link #SLOWNESS}.
     */
    @Deprecated
    public static final PerformanceLevel SLOW = SLOWNESS;

    /**
     * The minimal duration (in nanoseconds) for logging the record.
     */
    private volatile long minDuration;

    /**
     * The key for producing a localized name of this level.
     */
    private final short localization;

    /**
     * Constructs a new logging level for monitoring performance.
     *
     * @param name      the logging level name.
     * @param value     the level value.
     * @param duration  the minimal duration (in nanoseconds) for logging a record.
     */
    private PerformanceLevel(final String name, final short key, final int value, final long duration) {
        super(name, value);
        localization = key;
        minDuration = duration;
    }

    /**
     * Returns the level to use for logging an event of the given duration.
     * The method may return {@link Level#FINE}, {@link #SLOWNESS} or {@link #SLOWER}
     * depending on the duration.
     *
     * @param  duration  the event duration.
     * @param  unit      the unit of the given duration value.
     * @return the level to use for logging an event of the given duration.
     */
    public static Level forDuration(long duration, final TimeUnit unit) {
        duration = unit.toNanos(duration);
        if (duration < SLOWNESS.minDuration) {
            return Level.FINE;              // Most common case.
        }
        return (duration >= SLOWER.minDuration) ? SLOWER : SLOWNESS;
    }

    /**
     * Returns the minimal duration for logging an event at this level.
     *
     * @param  unit  the unit in which to express the minimal duration.
     * @return the minimal duration in the given unit.
     */
    public long getMinDuration(final TimeUnit unit) {
        return unit.convert(minDuration, TimeUnit.NANOSECONDS);
    }

    /**
     * Sets the minimal duration for logging an event at this level. Invoking this method
     * may have an indirect impact of other performance levels:
     *
     * <ul>
     *   <li>If the given duration is longer than the duration of slower levels, then the latter
     *       are also set to the given duration.</li>
     *   <li>If the given duration is shorter than the duration of faster levels, then the latter
     *       are also set to the given duration.</li>
     * </ul>
     *
     * @param  duration  the minimal duration.
     * @param  unit      the unit of the given duration value.
     * @throws IllegalArgumentException if the given duration is zero or negative.
     */
    @Configuration
    @SuppressWarnings("fallthrough")
    public void setMinDuration(long duration, final TimeUnit unit) throws IllegalArgumentException {
        ArgumentChecks.ensureStrictlyPositive("duration", duration);
        duration = unit.toNanos(duration);
        final int value = intValue();
        synchronized (PerformanceLevel.class) {
            if (value >= SLOWER.intValue() && duration < SLOWNESS.minDuration) {
                SLOWNESS.minDuration = duration;
            }
            minDuration = duration;
            if (value <= SLOWNESS.intValue() && duration > SLOWER.minDuration) {
                SLOWER.minDuration = duration;
            }
        }
    }

    /**
     * Return the name of this level for the current default locale.
     *
     * @return name of this level for the current locale.
     *
     * @since 1.2
     */
    @Override
    public String getLocalizedName() {
        return Vocabulary.format(localization);
    }
}
