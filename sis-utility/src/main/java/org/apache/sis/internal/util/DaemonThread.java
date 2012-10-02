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


/**
 * Base class for all daemon threads in the SIS library. This class provides a
 * {@link #isKillRequested()} flag which shall be tested by the daemon threads.
 * It is okay to test this flag only when catching {@link InterruptedException},
 * as below:
 *
 * {@preformat java
 *     while (someCondition) {
 *         try {
 *             someObject.wait();
 *         } catch (InterruptedException e) {
 *             if (isKillRequested()) {
 *                 break; // Exit the loop for stopping the thread.
 *             }
 *         }
 *     }
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.09)
 * @version 0.3
 * @module
 */
public class DaemonThread extends Thread {
    /**
     * Set to {@code true} when the {@link #kill()} method has been invoked.
     */
    private volatile boolean killRequested;

    /**
     * Creates a new daemon thread. This constructor sets the daemon flag to {@code true}.
     *
     * @param group The thread group.
     * @param name  The thread name.
     */
    protected DaemonThread(final ThreadGroup group, final String name) {
        super(group, name);
        setDaemon(true);
    }

    /**
     * Returns {@code true} if {@link #kill()} has been invoked.
     *
     * @return {@code true} if {@link #kill()} has been invoked.
     */
    protected final boolean isKillRequested() {
        return killRequested;
    }

    /**
     * Kills all the given threads (ignoring null arguments),
     * and waits for the threads to die before to return.
     *
     * @param  stopWaitingAt Value of {@link System#nanoTime()} when to stop waiting.
     *         This is used for preventing the shutdown to block an indefinite amount of time.
     * @param  threads The threads to kill. Null arguments are silently ignored.
     * @throws InterruptedException If an other thread invoked {@link #interrupt()} while
     *         we were waiting for the daemon threads to die.
     */
    static void kill(final long stopWaitingAt, final DaemonThread... threads)
            throws InterruptedException
    {
        for (final DaemonThread thread : threads) {
            if (thread != null) {
                thread.killRequested = true;
                thread.interrupt();
            }
        }
        for (final DaemonThread thread : threads) {
            if (thread != null) {
                final long delay = stopWaitingAt - System.nanoTime();
                if (delay <= 0) break;
                thread.join(delay);
            }
        }
    }
}
