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
package org.apache.sis.util.collection;

import java.util.concurrent.Callable;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.parallel.Isolated;
import org.apache.sis.test.TestCase;


/**
 * Base class of tests that may depend on the garbage collector activity.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Isolated("Depends on garbage collector activity")
abstract class TestCaseWithGC extends TestCase {
    /**
     * Whether the tests that may depend on the garbage collector activity are allowed.
     * Those tests are a little bit dangerous since they may randomly fail on a server
     * too busy for running the garbage collector as fast as expected.
     */
    static final boolean GC_DEPENDENT_TESTS_ENABLED = true;

    /**
     * Maximal time that {@code waitFoo()} methods can wait, in milliseconds.
     *
     * @see #waitForBlockedState(Thread)
     * @see #waitForGarbageCollection(Callable)
     */
    private static final int MAXIMAL_WAIT_TIME = 1000;

    /**
     * Creates a new test case.
     */
    protected TestCaseWithGC() {
    }

    /**
     * Waits up to one second for the garbage collector to do its work.
     * This method can be invoked only if {@link #GC_DEPENDENT_TESTS_ENABLED} is {@code true}.
     *
     * <p>Note that this method does not throw any exception if the given condition has not been
     * reached before the timeout. Instead, it is the caller responsibility to test the return value.
     * This method is designed that way because the caller can usually produce a more accurate error
     * message about which value has not been garbage collected as expected.</p>
     *
     * @param  stopCondition  a condition which return {@code true} if this method can stop waiting,
     *         or {@code false} if it needs to ask again for garbage collection.
     * @return {@code true} if the given condition has been met, or {@code false} if we waited up
     *         to the timeout without meeting the given condition.
     * @throws InterruptedException if this thread has been interrupted while waiting.
     */
    static boolean waitForGarbageCollection(final Callable<Boolean> stopCondition) throws InterruptedException {
        assertTrue(GC_DEPENDENT_TESTS_ENABLED, "GC-dependent tests not allowed in this run.");
        int retry = MAXIMAL_WAIT_TIME / 50;     // 50 shall be the same number as in the call to Thread.sleep.
        boolean stop;
        do {
            if (--retry == 0) {
                return false;
            }
            Thread.sleep(50);
            System.gc();
            try {
                stop = stopCondition.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        } while (!stop);
        return true;
    }

    /**
     * Waits up to one second for the given thread to reach the blocked or the waiting state.
     *
     * @param  thread  the thread to wait for blocked or waiting state.
     * @throws IllegalThreadStateException if the thread has terminated its execution,
     *         or has not reached the waiting or blocked state before the timeout.
     * @throws InterruptedException if this thread has been interrupted while waiting.
     *
     * @see java.lang.Thread.State#BLOCKED
     * @see java.lang.Thread.State#WAITING
     */
    static void waitForBlockedState(final Thread thread) throws IllegalThreadStateException, InterruptedException {
        int retry = MAXIMAL_WAIT_TIME / 5;      // 5 shall be the same number as in the call to Thread.sleep.
        do {
            Thread.sleep(5);
            switch (thread.getState()) {
                case WAITING:
                case BLOCKED: return;
                case TERMINATED: throw new IllegalThreadStateException("The thread has completed execution.");
            }
        } while (--retry != 0);
        throw new IllegalThreadStateException("The thread is not in a blocked or waiting state.");
    }
}
