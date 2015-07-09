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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.logging.Logging;


/**
 * A thread processing all {@link Reference} instances enqueued in a {@link ReferenceQueue}.
 * This is the central place where <em>every</em> weak references produced by the SIS library
 * are consumed. This thread will invoke the {@link Disposable#dispose()} method for each
 * references enqueued by the garbage collector. Those references <strong>must</strong>
 * implement the {@link Disposable} interface.
 *
 * Example:
 *
 * {@preformat java
 *     final class MyReference extends WeakReference<MyType> implements Disposable {
 *         MyReference(MyType referent) {
 *             super(referent, ReferenceQueueConsumer.QUEUE);
 *         }
 *
 *         &#64;Override
 *         public void dispose() {
 *             // Perform here some cleaning work that must be done when the referent has
 *             // been garbage-collected. Remember that get() returns null from this point.
 *         }
 *     }
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class ReferenceQueueConsumer extends DaemonThread {
    /**
     * List of references collected by the garbage collector. This reference shall be given to
     * {@link Reference} constructors as documented in the class javadoc. Those {@code Reference}
     * sub-classes <strong>must</strong> implement the {@link Disposable} interface.
     */
    public static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<Object>();

    /**
     * Creates the singleton instance of the {@code ReferenceQueueConsumer} thread.
     */
    static {
        synchronized (Threads.class) {
            final ReferenceQueueConsumer thread;
            Threads.lastCreatedDaemon = thread = new ReferenceQueueConsumer(Threads.lastCreatedDaemon);
            // Call to Thread.start() must be outside the constructor
            // (Reference: Goetz et al.: "Java Concurrency in Practice").
            thread.start();
        }
        if (Supervisor.ENABLED) {
            Supervisor.register();
        }
    }

    /**
     * Constructs a new thread as a daemon thread. This thread will be sleeping most of the time.
     * It will run only only a few nanoseconds every time a new {@link Reference} is enqueued.
     *
     * <div class="note"><b>Note:</b>
     * We give to this thread a priority higher than the normal one since this thread shall
     * execute only tasks to be completed very shortly. Quick execution of those tasks is at
     * the benefit of the rest of the system, since they make more resources available sooner.</div>
     */
    private ReferenceQueueConsumer(final DaemonThread lastCreatedDaemon) {
        super("ReferenceQueueConsumer", lastCreatedDaemon);
        setPriority(Thread.MAX_PRIORITY - 2);
    }

    /**
     * Loop to be run during the virtual machine lifetime.
     * Public as an implementation side-effect; <strong>do not invoke explicitly!</strong>
     */
    @Override
    public final void run() {
        /*
         * The reference queue should never be null. However some strange cases have been
         * observed at shutdown time. If the field become null, assume that a shutdown is
         * under way and let the thread terminate.
         */
        ReferenceQueue<Object> queue;
        while ((queue = QUEUE) != null) {
            try {
                /*
                 * Block until a reference is enqueued. The reference should never be null
                 * when using the method without timeout (it could be null if we specified
                 * a timeout). If the remove() method behaves as if a timeout occurred, we
                 * may be in the middle of a shutdown. Continue anyway as long as we didn't
                 * received the kill event.
                 */
                final Reference<?> ref = queue.remove();
                if (ref != null) {
                    /*
                     * If the reference does not implement the Disposeable interface, we want
                     * the ClassCastException to be logged in the "catch" block since it would
                     * be a programming error that we want to know about.
                     */
                    ((Disposable) ref).dispose();
                    continue;
                }
            } catch (InterruptedException exception) {
                // Probably the 'killAll' method has been invoked.
                // We need to test 'isKillRequested()' below.
            } catch (Throwable exception) {
                Logging.unexpectedException(Logging.getLogger(Loggers.SYSTEM), getClass(), "run", exception);
            }
            if (isKillRequested()) {
                break;
            }
        }
        // Do not log anything at this point, since the loggers may be shutdown now.
    }
}
