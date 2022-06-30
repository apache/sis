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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.FutureTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import org.apache.sis.gui.DataViewer;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.Threads;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.Exceptions;

import static java.util.logging.Logger.getLogger;


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
 * @version 1.3
 * @since   1.1
 * @module
 */
@SuppressWarnings("serial")                         // Not intended to be serialized.
public final class BackgroundThreads extends AtomicInteger implements ThreadFactory {
    /**
     * The {@code mayInterruptIfRunning} argument value to give to calls to
     * {@link java.util.concurrent.Future#cancel(boolean)} if the background task
     * may be doing I/O operations on a {@link java.nio.channels.InterruptibleChannel}.
     * Interruption must be disabled for avoiding the channel to be closed.
     *
     * <p>Note that the default value of {@link javafx.concurrent.Task#cancel()} is {@code true}.
     * So task doing I/O operations should be cancelled with {@code cancel(NO_INTERRUPT_DURING_IO)}.
     * This flag is defined mostly for tracking places in the code where tasks doing I/O operations
     * may be interrupted.</p>
     */
    public static final boolean NO_INTERRUPT_DURING_IO = false;

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
     * Runs the given task in JavaFX thread and wait for completion before to return.
     * This method should <em>not</em> be invoked from JavaFX application thread.
     * This method can be used for showing a dialog box during a background operation.
     *
     * @param  <V>   type of result that will be returned.
     * @param  task  the task to execute in JavaFX thread.
     * @return the task result, or {@code null} if an error occurred.
     */
    public static <V> V runAndWaitDialog(final Callable<V> task) {
        final FutureTask<V> f = new FutureTask<>(task);
        Platform.runLater(f);
        try {
            return f.get();
        } catch (ExecutionException e) {
            ExceptionReporter.show(DataViewer.getCurrentStage(), null, null, e);
        } catch (InterruptedException e) {
            interrupted("runAndWait", e);
        }
        return null;
    }

    /**
     * Runs the given task in JavaFX thread and wait for completion before to return.
     * This method should <em>not</em> be invoked from JavaFX application thread.
     *
     * @param  <V>   type of result that will be returned.
     * @param  task  the task to execute in JavaFX thread.
     * @return the task result.
     * @throws Exception if the task threw an exception.
     */
    public static <V> V runAndWait(final Callable<V> task) throws Exception {
        final FutureTask<V> f = new FutureTask<>(task);
        Platform.runLater(f);
        try {
            return f.get();
        } catch (ExecutionException e) {
            throw Exceptions.unwrap(e);
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
        DataStoreOpener.closeAll();
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
        Logging.unexpectedException(getLogger(Modules.APPLICATION), BackgroundThreads.class, method, e);
    }
}
