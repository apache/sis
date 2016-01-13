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
package org.apache.sis.internal.system;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * A task to be submitted to {@link DelayedExecutor} for later execution.
 *
 * <div class="section">Future evolution</div>
 * This interface may be removed in a future SIS version if we choose to use a library-wide executor
 * instead of {@code DelayedExecutor}. See <a href="https://issues.apache.org/jira/browse/SIS-76">SIS-76</a>
 * for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class DelayedRunnable implements Delayed, Runnable {
    /**
     * Time of execution of this task, in nanoseconds provided by {@link System#nanoTime()}.
     * In the particular case of the {@link Immediate} subclass, the meaning of this field is
     * modified: it is rather an ordinal value used for preserving task order.
     *
     * <div class="note"><b>Note:</b>
     * we use {@link System#nanoTime()} instead than {@link System#currentTimeMillis()} because
     * the later is not guaranteed to be monotonic: {@code currentTimeMillis} may change abruptly
     * for example if the user adjusts the clock of his operating system.
     * </div>
     */
    final long timestamp;

    /**
     * Creates a new task to be executed at the given time.
     * It is user's responsibility to add the {@link System#nanoTime()} value
     * to the delay he wants to wait.
     *
     * @param timestamp Time of execution of this task, in nanoseconds relative to {@link System#nanoTime()}.
     */
    protected DelayedRunnable(final long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the delay to wait before to execute this task,
     * or {@code 0} if this task shall be executed immediately.
     */
    @Override
    public long getDelay(final TimeUnit unit) {
        return unit.convert(timestamp - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    /**
     * Compares this task with the given delayed object for ordering.
     * The {@code other} object shall be an instance of {@link DelayedRunnable}.
     * This restriction should be okay since the {@link DelayedExecutor} queue
     * accepts only {@code DelayedRunnable} instances.
     *
     * @param other The other delayed object to compare with this delayed task.
     * @return -1 if the other task should happen before this one, +1 if it should happen after, or 0.
     */
    @Override
    public int compareTo(final Delayed other) {
        if (other instanceof Immediate) {
            return +1;                      // "Immediate" tasks always have precedence over delayed ones.
        }
        return Long.signum(timestamp - ((DelayedRunnable) other).timestamp);
    }

    /**
     * A "delayed" task which is actually executed as soon as possible.
     * The delay is fixed to 0 seconds, however those tasks are still
     * ordered in a "first created, first executed" basis.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    public static abstract class Immediate extends DelayedRunnable {
        /**
         * A counter for ordering the tasks in a "first created, first executed" basis.
         */
        private static final AtomicLong COUNTER = new AtomicLong(Long.MIN_VALUE);

        /**
         * Creates a new immediate task.
         */
        protected Immediate() {
            super(COUNTER.getAndIncrement());
        }

        /**
         * Returns the delay, which is fixed to 0 in every cases.
         *
         * @param  unit The unit of the value to return (ignored).
         * @return The delay, which is fixed to 0.
         */
        @Override
        public final long getDelay(final TimeUnit unit) {
            return 0L;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public final int compareTo(final Delayed other) {
            if (other instanceof Immediate) {
                if (timestamp > ((Immediate) other).timestamp) return +1;
                // Should never be equal.
            }
            return -1;
        }
    }
}
