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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.BlockingQueue;
import org.apache.sis.util.logging.Logging;


/**
 * A thread executing short tasks after some (potentially zero nanosecond) delay.
 * This thread is reserved to internal SIS usage - no user code shall be executed here.
 * All submitted tasks shall be very quick, since there is only one thread shared by everyone.
 *
 * {@note In practice some user code may be indirectly executed, since some SIS tasks invoke
 * overrideable methods. We may need to revisit the <code>DelayedExecutor</code> design in a
 * future version if the above happens to be a problem. For example we may allow the user to
 * specify an application-wide scheduled executor and delegate the tasks to that executor.}
 *
 * The methods for use in this class are:
 * <ul>
 *   <li>{@link #executeDaemonTask(DelayedRunnable)}</li>
 *   <li>{@link #schedule(DelayedRunnable)}</li>
 * </ul>
 *
 * {@section Comparison with <code>java.util.concurrent</code>}
 * We tried to use {@link java.util.concurrent.ScheduledThreadPoolExecutor} in a previous version,
 * but it seems more suitable to heavier tasks in applications controlling their own executor. For
 * example {@code ScheduledThreadPoolExecutor} acts as a fixed-sized pool, thus forcing us to use
 * only one thread if we don't want to waste resources (profiling shows that even a single thread
 * has very low activity). The {@code ThreadPoolExecutor} super-class is more flexible but still
 * have a quite aggressive policy on threads creation, and doesn't handle delayed tasks by itself.
 * We could combine both worlds with a {@code ThreadPoolExecutor} using a {@code DelayedQueue},
 * but it forces us to declare a core pool size of 0 otherwise {@code ThreadPoolExecutor} tries
 * to execute the tasks immediately without queuing them. Combined with the {@code DelayedQueue}
 * characteristics (being an unbounded queue), this result in {@code ThreadPoolExecutor} never
 * creating more than one thread (because it waits for the queue to reject a task before to create
 * more threads than the pool size).
 *
 * <p>Given that it seems difficult to configure {@code (Scheduled)ThreadPoolExecutor} in such
 * a way that two or more threads are created only when really needed, given that using those
 * thread pools seems an overkill when the pool size is fixed to one thread, given that our
 * profiling has show very low activity for that single thread anyway, and given that we do
 * not need cancellation and shutdown services for house keeping tasks (this is a daemon thread),
 * a more lightweight solution seems acceptable here. Pseudo-benchmarking using the
 * {@code CacheTest.stress()} tests suggests that the lightweight solution is faster.</p>
 *
 * {@section Future evolution}
 * We may remove (again) this class in a future SIS evolution if we happen to need an executor anyway.
 * However it may be better to wait and see what are the executor needs. Setting up an executor implies
 * choosing many arbitrary parameter values like the number of core threads, maximum threads, idle time,
 * queue capacity, etc. Furthermore some platforms (e.g. MacOS) provide OS-specific implementations
 * integrating well in their environment. We may want to let the user provides the executor of his
 * choice, or we way want to have more profiling data for choosing an appropriate executor. But we
 * may need to find some way to give priority to SIS tasks, since most of them are for releasing
 * resources - in which case quick execution probably help the system to run faster.
 * However before to switch from the lightweight solution to a more heavy solution,
 * micro-benchmarking is desirable. The {@code CacheTest.stress()} tests can be used
 * in first approximation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-76">SIS-76</a>
 */
public final class DelayedExecutor extends DaemonThread {
    /**
     * Executes the given short task in a daemon thread. This method shall be invoked for
     * Apache SIS tasks only, <strong>not</strong> for arbitrary user task. The task must
     * completes quickly, because we will typically use only one thread for all submitted
     * tasks. Completion of the task shall not be critical, since the JVM is allowed to
     * shutdown before task completion.
     *
     * {@section Future evolution}
     * If {@code DelayedExecutor} is removed in a future SIS version in favor of JDK6 executors,
     * then the method signature will probably be {@code Executors.execute(Runnable)}.
     *
     * @param task The task to execute.
     */
    public static void executeDaemonTask(final DelayedRunnable task) {
        QUEUE.add(task);
    }

    /**
     * Schedules the given short task for later execution in a daemon thread.
     * The task will be executed after the delay specified by {@link DelayedRunnable#getDelay}
     * The task must completes quickly, because we will typically use only one thread for all
     * submitted tasks. Completion of the task shall not be critical, since the JVM is allowed
     * to shutdown before task completion.
     *
     * {@section Future evolution}
     * If {@code DelayedExecutor} is removed in a future SIS version in favor of JDK6 executors,
     * then the method signature will probably be {@code Executors.schedule(Runnable, long, TimeUnit)}.
     *
     * @param task The task to schedule for later execution.
     */
    public static void schedule(final DelayedRunnable task) {
        // For now the implementation is identical to 'execute'. However it may become
        // different if we choose to use a library-wide executor in a future SIS version.
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
     * {@note We give to this thread a priority higher than the normal one since this thread shall
     *        execute only tasks to be completed very shortly. Quick execution of those tasks is at
     *        the benefit of the rest of the system, since they make more resources available sooner.}
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
                Logging.unexpectedException(getClass(), "run", exception);
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
