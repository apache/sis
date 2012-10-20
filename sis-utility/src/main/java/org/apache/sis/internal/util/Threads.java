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
import java.util.concurrent.ExecutorService;
import org.apache.sis.util.Static;
import org.apache.sis.util.logging.Logging;


/**
 * Utilities methods for threads. This class declares in a single place every {@link ThreadGroup}
 * used in SIS. Their intend is to bring some order in debugger informations, by grouping the
 * threads created by SIS together under the same parent tree node.
 *
 * {@section Note on dependencies}
 * This class shall not depend on {@link Executors} or {@link ReferenceQueueConsumer}, because
 * initialization of those classes create new threads or threaded executor. But it is okay to
 * have dependencies the other way around.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
final class Threads extends Static {
    /**
     * The parent of every threads declared in this class. This parent will be declared as close
     * as possible to the root of all thread groups (i.e. not as an application thread subgroup).
     * The intend is to separate the library thread groups from the user application thread groups.
     */
    static final ThreadGroup SIS;
    static {
        ThreadGroup parent = Thread.currentThread().getThreadGroup();
        try {
            ThreadGroup candidate;
            while ((candidate = parent.getParent()) != null) {
                parent = candidate;
            }
        } catch (SecurityException e) {
            // If we are not allowed to get the parent, stop there.
            // We went up in the tree as much as we were allowed to.
        }
        SIS = new ThreadGroup(parent, "Apache SIS");
    }

    /**
     * The sub-group for daemon threads, usually for resources disposal.
     */
    static final ThreadGroup DAEMONS = new ThreadGroup(SIS, "Daemons") {
        @Override public void uncaughtException(final Thread thread, final Throwable exception) {
            Logging.severeException(Logging.getLogger("org.apache.sis"), thread.getClass(), "run", exception);
        }
    };

    /**
     * The tail of a chain of {@code DaemonThread}s created by the {@code sis-utility} module.
     * Other modules need to maintain their own chain, if any. See the {@link DaemonThread}
     * javadoc for more information.
     */
    static DaemonThread lastCreatedDaemon;

    /**
     * Executor to shutdown. This is a copy of the {@link Executors#DAEMON_TASKS} field,
     * copied here only when the {@link Executors} class is loaded and initialized. We
     * do that way for avoiding dependency from {@code Threads} to {@code Executors}.
     */
    static ExecutorService executor;

    /**
     * Do not allows instantiation of this class.
     */
    private Threads() {
    }

    /**
     * Sends a kill signal to all daemon threads created by the {@code sis-utility} module,
     * and waits for the threads to die before to return.
     *
     * <p><strong>This method is for internal use by Apache SIS shutdown hooks only.</strong>
     * Users should never invoke this method explicitely.</p>
     *
     * @param  stopWaitingAt A {@link System#nanoTime()} value telling when to stop waiting.
     *         This is used for preventing shutdown process to block an indefinite amount of time.
     * @throws InterruptedException If an other thread invoked {@link #interrupt()} while
     *         we were waiting for the daemon threads to die.
     */
    static synchronized void shutdown(final long stopWaitingAt) throws InterruptedException {
        if (executor != null) {
            executor.shutdown();
            /*
             * Wait for work completion. In theory this is not necessary since the daemon
             * tasks are only house-cleaning work. We nevertheless wait for their completion
             * as a safety. There tasks are supposed to be short.
             */
            final long delay = stopWaitingAt - System.nanoTime();
            if (delay > 0) {
                executor.awaitTermination(delay, TimeUnit.NANOSECONDS);
                // Even if the tasks didn't completed, continue without waiting for them.
                // We can not log at this point, since the logging framework may be shutdown.
            }
        }
        DaemonThread.killAll(lastCreatedDaemon, stopWaitingAt);
    }
}
