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

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import org.apache.sis.util.logging.Logging;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A central place where to manage SIS shutdown process.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public final class Shutdown extends Thread {
    /**
     * Non-null if a shutdown hook is already registered. That shutdown hook is not necessarily {@link #hook}.
     * It may be an OSGi or Servlet shutdown hook instead, as notified by {@link #setContainer(String)}.
     */
    private static String container;

    /**
     * The shutdown hook to be registered to the JVM {@link Runtime}, created when first needed.
     */
    private static Shutdown hook;

    /**
     * The resources to dispose. Most recently added resources are last.
     */
    private static final List<Callable<?>> resources = new ArrayList<Callable<?>>();

    /**
     * Creates the thread to be executed at shutdown time.
     */
    private Shutdown() {
        super(Threads.SIS, "Shutdown");
    }

    /**
     * Invoked at JVM shutdown time.
     */
    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void run() {
        try {
            Shutdown.stop((Class<?>) null);
        } catch (Exception e) {
            /*
             * Too late for logging since we are in process of shutting down the Java Virtual Machine.
             * It is still possible to write the stack trace to System.err, but this is about all we can do.
             */
            e.printStackTrace();
        }
    }

    /**
     * Returns the value set by the last call to {@link #setContainer(String)}.
     *
     * @return Typically {@code "OSGi"}, {@code "Servlet"} or {@code null}.
     */
    public static String getContainer() {
        synchronized (resources) {
            return container;
        }
    }

    /**
     * Invoked if the Apache SIS library is executed from an environment that provide its own shutdown hook.
     * Example of such environments are OSG and servlet containers. In such case, the shutdown hook will not
     * be registered to the JVM {@link Runtime}.
     *
     * @param env A description of the container. Should contain version information if possible.
     *            Example: {@code "OSGi"} or {@code "JavaServer Web Dev Kit/1.0"}.
     */
    public static void setContainer(final String env) {
        Objects.requireNonNull(env);
        synchronized (resources) {
            removeShutdownHook();       // Should not be needed but we are paranoiac.
            container = env;
        }
    }

    /**
     * Registers a code to execute at JVM shutdown time. The resources will be disposed at
     * shutdown time in reverse order (most recently added resources will be disposed first).
     *
     * <p>The same resource shall not be added twice.</p>
     *
     * @param resource The resource disposal to register for execution at shutdown time.
     */
    public static void register(final Callable<?> resource) {
        synchronized (resources) {
            assert !resources.contains(resource);
            resources.add(resource);
            if (hook == null && container == null) {
                hook = new Shutdown();
                Runtime.getRuntime().addShutdownHook(hook);
            }
        }
    }

    /**
     * Removes the shutdown hook, if any.
     */
    private static void removeShutdownHook() {
        assert Thread.holdsLock(resources);
        if (hook != null) {
            Runtime.getRuntime().removeShutdownHook(hook);
            hook = null;
        }
    }

    /**
     * Unregisters a code from execution at JVM shutdown time.
     * This method uses identity comparison (it does not use {@link Object#equals(Object)}).
     *
     * @param resource The resource disposal to cancel execution.
     */
    public static void unregister(final Callable<?> resource) {
        synchronized (resources) {
            for (int i = resources.size(); --i>=0;) {       // Check most recently added resources first.
                if (resources.get(i) == resource) {
                    resources.remove(i);
                    break;
                }
            }
        }
    }

    /**
     * Unregisters the supervisor MBean, executes the disposal tasks and shutdowns the {@code sis-utility} threads.
     *
     * @param  caller The class invoking this method, to be used only for logging purpose, or {@code null}
     *         if the logging system is not available anymore (i.e. the JVM itself is shutting down).
     * @throws Exception if an error occurred during unregistration of the supervisor MBean
     *         or during a resource disposal.
     */
    public static void stop(final Class<?> caller) throws Exception {
        synchronized (resources) {
            container = "Shutdown";
            if (caller != null) {
                removeShutdownHook();
            }
        }
        /*
         * Unregister the MBean before to stop the threads, in order to avoid false alerts
         * in the superviror 'warnings()' method. Failure to unregister the MBean is worth
         * to report, but we will do that only after we completed the other shutdown steps.
         */
        Exception exception = null;
        if (Supervisor.ENABLED) try {
            Supervisor.unregister();
        } catch (Exception deferred) {
            exception = deferred;
        }
        /*
         * Dispose resources, if any, starting with most recently registered. The disposal code should not
         * invoke Shutdown.[un]register(Disposable), but we nevertheless make the loop robust to this case.
         */
        synchronized (resources) {
            int i;
            while ((i = resources.size()) != 0) try {       // In case run() modifies the resources list.
                resources.remove(i - 1).call();             // Dispose most recently added resources first.
            } catch (Exception e) {
                exception = e;
            }
        }
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
                Logging.unexpectedException(Logging.getLogger(Loggers.SYSTEM), caller, "stop", e);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }
}
