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
import java.util.concurrent.atomic.AtomicInteger;
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
        return new Thread(Threads.SIS, r, "Application worker #" + incrementAndGet());
    }

    /**
     * Executes the given task in a background thread.
     *
     * @param  task  the task to execute.
     */
    public static void execute(final Runnable task) {
        EXECUTOR.execute(task);
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
            Logging.recoverableException(Logging.getLogger(Modules.APPLICATION), BackgroundThreads.class, "stop", e);
        }
        ResourceLoader.closeAll();
    }
}
