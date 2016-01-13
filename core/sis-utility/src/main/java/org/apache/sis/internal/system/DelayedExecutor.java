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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.BlockingQueue;
import org.apache.sis.util.logging.Logging;


/**
 * A thread executing short tasks after some (potentially zero nanosecond) delay.
 * This class should be reserved to internal SIS usage without user's code.
 * In practice some user code may be indirectly executed through SIS tasks invoking overrideable methods.
 * But all submitted tasks shall be very quick, since there is only one thread shared by everyone.
 *
 * <p>The methods for use in this class are:</p>
 * <ul>
 *   <li>{@link #schedule(Runnable, long)}</li>
 * </ul>
 *
 * <div class="section">Comparison with {@code java.util.concurrent}</div>
 * We tried to use {@link java.util.concurrent.ScheduledThreadPoolExecutor} in a previous SIS version,
 * but its "fixed-sized pool" design forces us to use only one thread if we do not want to waste resources
 * (profiling shows that even a single thread has very low activity), which reduces the interest of that class.
 * Combination of {@code ThreadPoolExecutor} super-class with {@code DelayedQueue} were not successful neither.
 *
 * <p>Given that it:</p>
 * <ul>
 *   <li>it seems difficult to configure {@code (Scheduled)ThreadPoolExecutor} in such a way
 *       that two or more threads are created only when really needed,</li>
 *   <li>using those executor services seems an overkill when the pool size is fixed to one thread,</li>
 *   <li>our profiling has show very low activity for that single thread anyway,</li>
 *   <li>we do not need cancellation and shutdown services for house keeping tasks (this is a daemon thread),</li>
 * </ul>
 * a more lightweight solution seems acceptable here. Pseudo-benchmarking using the
 * {@code CacheTest.stress()} tests suggests that the lightweight solution is faster.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-76">SIS-76</a>
 */
public final class DelayedExecutor extends DaemonThread {
    /**
     * Schedules the given short task for later execution in a daemon thread.
     * The task will be executed after the delay specified by {@link DelayedRunnable#getDelay(TimeUnit)}
     * The task must completes quickly, because we will typically use only one thread for all submitted tasks.
     * Completion of the task shall not be critical, since the JVM is allowed to shutdown before task completion.
     *
     * @param task The task to schedule for later execution.
     */
    public static void schedule(final DelayedRunnable task) {
        QUEUE.add(task);
    }

    /**
     * List of delayed tasks to execute.
     */
    private static final BlockingQueue<DelayedRunnable> QUEUE = new DelayQueue<DelayedRunnable>();

    /**
     * Creates the singleton instance of the {@code DelayedExecutor} thread.
     */
    static {
        synchronized (Threads.class) {
            final DelayedExecutor thread;
            Threads.lastCreatedDaemon = thread = new DelayedExecutor(Threads.lastCreatedDaemon);
            // Call to Thread.start() must be outside the constructor
            // (Reference: Goetz et al.: "Java Concurrency in Practice").
            thread.start();
        }
        if (Supervisor.ENABLED) {
            Supervisor.register();
        }
    }

    /**
     * Constructs a new thread as a daemon thread. This thread will be sleeping most of the time.
     * It will run only only a few nanoseconds every time a new {@link DelayedRunnable} is taken.
     *
     * <div class="note"><b>Note:</b>
     * We give to this thread a priority higher than the normal one since this thread shall
     * execute only tasks to be completed very shortly. Quick execution of those tasks is at
     * the benefit of the rest of the system, since they make more resources available sooner.</div>
     */
    private DelayedExecutor(final DaemonThread lastCreatedDaemon) {
        super("DelayedExecutor", lastCreatedDaemon);
        setPriority(Thread.NORM_PRIORITY + 1);
    }

    /**
     * Loop to be run during the virtual machine lifetime.
     * Public as an implementation side-effect; <strong>do not invoke explicitly!</strong>
     */
    @Override
    public final void run() {
        /*
         * The reference queue should never be null. However some strange cases have been
         * observed at shutdown time. If the field become null, assume that a shutdown is
         * under way and let the thread terminate.
         */
        BlockingQueue<DelayedRunnable> queue;
        while ((queue = QUEUE) != null) {
            try {
                final DelayedRunnable task = queue.take();
                if (task != null) {
                    task.run();
                    continue;
                }
            } catch (InterruptedException exception) {
                // Probably the 'killAll' method has been invoked.
                // We need to test 'isKillRequested()' below.
            } catch (Throwable exception) {
                Logging.unexpectedException(Logging.getLogger(Loggers.SYSTEM), getClass(), "run", exception);
            }
            if (isKillRequested()) {
                queue.clear();
                break;
            }
        }
        // Do not log anything at this point, since the loggers may be shutdown now.
    }

    /**
     * Returns {@code true} if this thread seems to be stalled. This method checks the head
     * of the queue. If the delay for that head has expired and the head is not removed in
     * the next 5 seconds, then we will presume that the thread is stalled or dead.
     *
     * @return {@inheritDoc}
     */
    @Override
    protected boolean isStalled() {
        final DelayedRunnable waiting = QUEUE.peek();
        if (waiting != null && waiting.getDelay(TimeUnit.NANOSECONDS) <= 0) try {
            for (int i=0; i<50; i++) {
                if (!isAlive()) break;
                Thread.sleep(100);
                if (QUEUE.peek() != waiting) {
                    return false;
                }
            }
            return true;
        } catch (InterruptedException e) {
            // Someone doesn't want to let us wait. Since we didn't had the time to
            // determine if the thread is stalled, conservatively return 'false'.
        }
        return false;
    }
}
