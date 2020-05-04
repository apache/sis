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
package org.apache.sis.internal.gui;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.Threads;
import org.apache.sis.util.logging.Logging;


/**
 * Provides the thread pool for JavaFX application. This thread pool is different than the pool used by
 * the {@link org.apache.sis.internal.system.CommonExecutor} shared by the rest of Apache SIS library.
 * Contrarily to {@code CommonExecutor}, this {@code BackgroundThreads} class always allocates threads
 * to new tasks immediately (no queuing of tasks), no matter if all processors are already busy or not.
 * The intent is to have quicker responsiveness to user actions, even at the cost of lower throughput.
 * Another difference is that the threads used by this class are not daemon threads in order to not stop
 * for example in the middle of a write operation.
 *
 * <p>This class extends {@link AtomicInteger} for opportunistic reason.
 * Users should not rely on this implementation details.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@SuppressWarnings("serial")                         // Not intended to be serialized.
public final class BackgroundThreads extends AtomicInteger implements ThreadFactory {
    /**
     * The executor for background tasks. This is actually an {@link ExecutorService} instance,
     * but only the {@link Executor} method should be used according JavaFX documentation.
     */
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new BackgroundThreads());

    /**
     * For the singleton {@link #EXECUTOR}.
     */
    private BackgroundThreads() {
    }

    /**
     * Creates a new thread. This method is invoked by {@link #EXECUTOR}
     * when needed and does not need to be invoked explicitly.
     *
     * @param  r  the runnable to assign to the thread.
     * @return a thread for the executor.
     */
    @Override
    public Thread newThread(final Runnable r) {
        final Thread t = new Thread(Threads.SIS, r, "Application worker #" + incrementAndGet());
        t.setPriority(Thread.NORM_PRIORITY - 1);      // Let JavaFX thread have higher priority.
        return t;
    }

    /**
     * Executes the given task in a background thread. This method can be invoked from any thread.
     * If the current thread is not the {@linkplain Platform#isFxApplicationThread() JavaFX thread},
     * then this method assumes that current thread is already a background thread and execute the
     * given task in that thread.
     *
     * @param  task  the task to execute.
     */
    public static void execute(final Runnable task) {
        if (Platform.isFxApplicationThread()) {
            EXECUTOR.execute(task);
        } else {
            task.run();
            /*
             * The given task is almost always a `javafx.concurrent.Task` which needs to do some work
             * in JavaFX thread after the background thread finished. Because this method is normally
             * invoked from JavaFX thread, the caller does not expect its JavaFX properties to change
             * concurrently. For avoiding unexpected behavior, we wait for the given task to complete
             * the work that it may be doing in JavaFX thread. We rely on the fact that `task.run()`
             * has already done its `Platform.runLater(…)` calls at this point, and the call that we
             * are doing below is guaranteed to be executed after the calls done by `task.run()`.
             * The timeout is low because the tasks on JavaFX threads are supposed to be very short.
             */
            final CountDownLatch c = new CountDownLatch(1);
            Platform.runLater(c::countDown);
            try {
                c.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrupted("execute", e);
            }
        }
    }

    /**
     * Invoked at application shutdown time for stopping the executor threads after they completed their tasks.
     * This method returns soon but the background threads may continue for some time if they did not finished
     * their task yet.
     *
     * @throws Exception if an error occurred while closing at least one data store.
     */
    public static void stop() throws Exception {
        EXECUTOR.shutdown();
        try {
            EXECUTOR.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            /*
             * Someone does not want to wait for termination.
             * Closes the data stores now even if some of them may still be in use.
             */
            interrupted("stop", e);
        }
        ResourceLoader.closeAll();
    }

    /**
     * Invoked when waiting for an operation to complete and the wait has been interrupted.
     * It should not happen, but if it happens anyway we just log a warning and continue.
     * Note that this is not very different than continuing after the timeout elapsed.
     * In both cases the risk is that some data structure may be in inconsistent state.
     *
     * @param  method  the method which has been interrupted.
     * @param  e       the exception that interrupted the waiting process.
     */
    private static void interrupted(final String method, final InterruptedException e) {
        Logging.unexpectedException(Logging.getLogger(Modules.APPLICATION), BackgroundThreads.class, method, e);
    }
}
