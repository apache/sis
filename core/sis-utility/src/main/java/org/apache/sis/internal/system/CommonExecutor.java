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

import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The executor shared by most of Apache SIS library for relatively "heavy" operations.
 * The operations should relatively long tasks, otherwise work-stealing algorithms may
 * provide better performances. For example it may be used when each computational unit
 * is an image tile, in which case the thread scheduling overhead is small compared to
 * the size of the computational task.
 *
 * <p>This thread pool is complementary to the {@link java.util.concurrent.ForkJoinPool}
 * used by {@link java.util.stream.Stream} API for example. The fork-join mechanism expects
 * computational tasks (blocking operations such as I/O are not recommended) of medium size
 * (between 100 and 10000 basic computational steps). By contrast, the tasks submitted to
 * this {@code CommonExecutor} may be long and involve blocking input/output operations.
 * We use a separated thread pool for avoiding the risk that long and/or blocked tasks
 * prevent the Java common pool from working.</p>
 *
 * <p>This class extends {@link AtomicInteger} for opportunistic reason.
 * Users should not rely on this implementation details.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see org.apache.sis.internal.gui.BackgroundThreads
 * @see java.util.concurrent.ForkJoinPool#commonPool()
 *
 * @since 1.1
 * @module
 */
@SuppressWarnings("serial")                     // Not intended to be serialized.
public final class CommonExecutor extends AtomicInteger implements ThreadFactory {
    /**
     * Maximal number of threads that {@link #INSTANCE} can execute.
     * If the number of tasks is greater than this parallelism value,
     * extraneous tasks will be queued.
     */
    public static final int PARALLELISM = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);

    /**
     * The executor for background tasks. The maximum number of threads is the number of processors minus 1.
     * The minus 1 is for increasing the chances that some CPU is still available for Java common thread pool
     * or for JavaFX/Swing thread. In addition the caller will often do part of the work in its own thread.
     * Threads are disposed after two minutes of inactivity.
     */
    private static final ThreadPoolExecutor INSTANCE;
    static {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(PARALLELISM, PARALLELISM, 2, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(1000000),         // Arbitrary limit against excessive queue expansion.
                new CommonExecutor());
        executor.allowCoreThreadTimeOut(true);
        INSTANCE = executor;
    }

    /**
     * Returns the executor service shared by SIS modules for most costly calculations.
     *
     * @return the executor service for SIS tasks to run in background.
     */
    public static ExecutorService instance() {
        return INSTANCE;
    }

    /**
     * If the given task has been scheduled for execution but its execution did not yet started,
     * removes it from the scheduled list. Otherwise does nothing. The given task should be one
     * of the following values:
     *
     * <ul>
     *   <li>The {@link Runnable} value given to {@link ExecutorService#execute(Runnable)}.</li>
     *   <li>The {@link Future} value returned by {@link ExecutorService#submit(Runnable)}.
     *       In that case, the {@code Future} wrapper created by {@link ThreadPoolExecutor}
     *       is actually an instance of {@link java.util.concurrent.RunnableFuture}.</li>
     * </ul>
     *
     * @param  task  the task to remove from the list of tasks to execute.
     * @return whether the given task has been removed.
     */
    public static boolean unschedule(final Future<?> task) {
        if (task instanceof Runnable) {
            return INSTANCE.remove((Runnable) task);
        }
        return false;
    }

    /**
     * For the singleton {@link #INSTANCE}.
     */
    private CommonExecutor() {
    }

    /**
     * Invoked by {@link #INSTANCE} for creating a new daemon thread. The thread will have a slightly
     * lower priority than normal threads so that short requests for CPU power (e.g. a user action in
     * the JavaFX/Swing thread) are processed more quickly. This is done on the assumption that tasks
     * executed by {@link #INSTANCE} are relatively long tasks, for which loosing momentously some CPU
     * power does not make a big difference.
     *
     * @param  r  the runnable to assign to the thread.
     * @return a thread for the executor.
     */
    @Override
    public Thread newThread(final Runnable r) {
        final Thread t = new Thread(Threads.SIS, r, "Background worker #" + incrementAndGet());
        t.setPriority(Thread.NORM_PRIORITY - 2);
        t.setDaemon(true);
        return t;
    }
}
