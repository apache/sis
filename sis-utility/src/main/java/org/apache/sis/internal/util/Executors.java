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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RejectedExecutionException;
import org.apache.sis.util.Static;


/**
 * Library-wide executors for Apache SIS internal tasks only (not arbitrary user tasks).
 * This class extends {@link ThreadFactory} and {@link RejectedExecutionHandler} for opportunist
 * reasons. Developers shall ignore this implementation detail, which may change at any time.
 * The methods for use in this class are:
 *
 * <ul>
 *   <li>{@link #executeDaemonTask(Runnable)}</li>
 *   <li>{@link #createThreadFactory(String)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
public final class Executors extends Static implements ThreadFactory, RejectedExecutionHandler {
    /**
     * The library-wide executor for short tasks. This executor is not for heavy calculations.
     * A single thread should be sufficient for those tasks. However we use a thread pool for
     * scalability in case a unusually large amount of tasks are submitted simultaneously.
     *
     * <p>The threads created by this class are daemon threads in order to let user application
     * terminate even if the user do not invoke {@link Threads#shutdown(long)}. Consequently,
     * tasks given to this constructor shall not be critical tasks, unless the caller thread
     * has some way to wait for its submitted tasks to complete.</p>
     *
     * <p>Queue and thread creation politic:</p>
     * <ul>
     *   <li>Initialized with no thread.</li>
     *   <li>Create one thread when a task if first submitted.</li>
     *   <li>All other tasks are enqueued, up to 10 enqueued tasks.</li>
     *   <li>If there is more than 10 enqueued tasks, create more threads up to the number of processors.</li>
     *   <li>All other tasks submission will block until it can be enqueued.</li>
     * </ul>
     */
    private static final ExecutorService DAEMON_TASKS;
    static {
        final Executors handlers = new Executors(true, "Pooled thread #");
        final ThreadPoolExecutor ex = new ThreadPoolExecutor(1,
                Runtime.getRuntime().availableProcessors(),
                5L, TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>(10),
                handlers, handlers);
        ex.allowCoreThreadTimeOut(true);
        DAEMON_TASKS = ex;
        synchronized (Threads.class) {
            Threads.executor = ex; // Used for shutdown only.
        }
    }

    /**
     * {@code true} if the threads to be created should be daemon threads.
     * This will also determine the thread group and the thread priority.
     */
    private final boolean isDaemon;

    /**
     * The prefix to put at the beginning of thread names, before thread number.
     */
    private final String namePrefix;

    /**
     * Number of threads created up to date by this thread factory.
     * This will be used together with {@link #namePrefix} for creating the thread name.
     */
    private int threadCount;

    /**
     * Creates a new thread factory, for internal use only.
     *
     * @param isDaemon   {@code true} if the threads to be created should be daemon threads.
     * @param namePrefix The prefix to put at the beginning of thread names, before thread number.
     */
    private Executors(final boolean isDaemon, final String prefix) {
        this.isDaemon = isDaemon;
        this.namePrefix = prefix;
    }

    /**
     * Executes the given short task in a daemon thread. This method shall be invoked for
     * Apache SIS tasks only, <strong>not</strong> for arbitrary user task. The task must
     * completes quickly, because we will typically use only one thread for all submitted
     * tasks (while the number of thread will automatically increase in case of heavy load).
     * Completion of the task shall not be critical, since the JVM is allowed to shutdown
     * before task completion.
     *
     * @param task The quick task to execute in a daemon thread.
     */
    public static void executeDaemonTask(final Runnable task) {
        DAEMON_TASKS.execute(task);
    }

    /**
     * Creates a factory for worker threads created by the SIS library. This factory will
     * creates ordinary (non-daemon) threads in the "Apache SIS" thread group, using the
     * given prefix for the thread names.
     *
     * @param  prefix The prefix to put in front of thread names, before the thread number.
     * @return The thread factory.
     */
    public static ThreadFactory createThreadFactory(final String prefix) {
        return new Executors(false, prefix);
    }

    /**
     * Invoked when a new thread needs to be created. This method is public as an
     * implementation side-effect and should not be invoked directly.
     *
     * @param  task The task to execute.
     * @return A new thread running the given task.
     *
     * @see #createThreadFactory(String)
     */
    @Override
    public Thread newThread(final Runnable task) {
        final int n;
        synchronized (this) {
            n = ++threadCount;
        }
        final String name = namePrefix + n;
        final Thread thread = new Thread(isDaemon ? Threads.DAEMONS : Threads.SIS, task, name);
        thread.setDaemon(isDaemon);
        if (isDaemon) {
            /*
             * Daemon threads created by the Apache SIS library are expected to complete quickly.
             * They are often (but not always) used for cleaning work, in which case a quick run
             * may be benificial to the rest of the application by releasing resources.  This is
             * a similar policy to the ReferenceQueueConsumer one.  Note that no user task shall
             * be submitted in daemon thread.
             */
            thread.setPriority(Thread.NORM_PRIORITY + 1);
        }
        return thread;
    }

    /**
     * Invoked when a task can not be accepted because the queue is full and the maximal number
     * of threads has been reached. This method blocks until a slot is made available.
     *
     * @param  task The task to execute.
     * @param  executor The executor that invoked this method.
     * @throws RejectedExecutionException If the caller thread has been interrupted while waiting.
     */
    @Override
    public void rejectedExecution(final Runnable task, final ThreadPoolExecutor executor) {
        try {
            executor.getQueue().put(task);
        } catch (InterruptedException e) {
            throw new RejectedExecutionException(e);
        }
    }
}
