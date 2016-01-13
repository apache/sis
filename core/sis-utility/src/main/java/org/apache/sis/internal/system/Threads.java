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

import org.apache.sis.util.Static;
import org.apache.sis.util.logging.Logging;


/**
 * Utilities methods for threads. This class declares in a single place every {@link ThreadGroup}
 * used in SIS. Their intend is to bring some order in debugger informations, by grouping the
 * threads created by SIS together under the same parent tree node.
 *
 * <div class="section">Note on dependencies</div>
 * This class shall not depend on {@link ReferenceQueueConsumer} or {@link DelayedExecutor},
 * because initialization of those classes create new threads. However it is okay to have
 * dependencies the other way around.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
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
            Logging.severeException(Logging.getLogger(Loggers.SYSTEM), thread.getClass(), "run", exception);
        }
    };



    /* -------------------------------------------------------------------------------------
     * Every non-final static variables below this point are initialized by other classes,
     * like DaemonThread or Executors - this class will never initialize those variables by
     * itself. All initialization shall be performed in a synchronized (Thread.class) block.
     * ------------------------------------------------------------------------------------- */

    /**
     * The tail of a chain of {@code DaemonThread}s created by the {@code sis-utility} module.
     * Other modules need to maintain their own chain, if any. See the {@link DaemonThread}
     * javadoc for more information.
     */
    static DaemonThread lastCreatedDaemon;

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
     * @throws InterruptedException If an other thread invoked {@link Thread#interrupt()} while
     *         we were waiting for the daemon threads to die.
     */
    static synchronized void shutdown(final long stopWaitingAt) throws InterruptedException {
        DaemonThread.killAll(lastCreatedDaemon, stopWaitingAt);
    }
}
