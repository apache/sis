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

import org.apache.sis.util.logging.Logging;


/**
 * A central place where to manage SIS shutdown process.
 * For now this class is not yet registered as a shutdown hock,
 * but it will be in a future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class Shutdown {
    /**
     * Do not allow instantiation of this class.
     */
    private Shutdown() {
    }

    /**
     * Shutdowns the {@code sis-utility} threads and unregister the supervisor MBean.
     *
     * @param  caller The class invoking this method, to be used only for logging purpose,
     *         or {@code null} if the logging system is not available anymore (i.e. the JVM
     *         itself is shutting down).
     */
    public static void stop(final Class<?> caller) {
        /*
         * Following is usually fast, but may potentially take a little while.
         * If an other thread invoked Thread.interrupt() while we were waiting
         * for the threads to terminate, maybe not all threads have terminated
         * but continue the shutdown process anyway.
         */
        try {
            Threads.shutdown(System.nanoTime() + 4000);
        } catch (InterruptedException e) {
            if (caller != null) {
                Logging.unexpectedException(caller, "stop", e);
            }
        }
    }
}
