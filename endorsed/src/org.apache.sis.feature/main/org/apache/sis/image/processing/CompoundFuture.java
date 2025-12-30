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
package org.apache.sis.image.processing;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.feature.internal.Resources;


/**
 * The result of multiple asynchronous computations.
 * This {@code Future} is considered completed when all components are completed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CompoundFuture<R> implements Future<R> {
    /**
     * The elements making this computation.
     */
    private final Future<R>[] components;

    /**
     * Creates a new future with the given components.
     */
    private CompoundFuture(final Future<R>[] components) {
        this.components = components;
    }

    /**
     * Returns a future waiting for all given tasks to complete.
     * If the array length is 1, then this method returns directly its singleton element.
     *
     * @param  <R>         type if result computed by tasks.
     * @param  components  the sub-tasks to execute. This array is not cloned; do not modify.
     * @return a future containing all given tasks.
     */
    public static <R> Future<R> create(final Future<R>[] components) {
        switch (components.length) {
            case 0: return null;
            case 1: return components[0];
        }
        return new CompoundFuture<>(components);
    }

    /**
     * Attempts to cancel execution of this task. After this method return, subsequent calls
     * to {@link #isCancelled()} return {@code true} if this method returned {@code true}.
     *
     * <h4>Departure from specification</h4>
     * {@code Future} specification requires that after this method returns, subsequent calls
     * to {@link #isDone()} return {@code true}. This is not guaranteed in this implementation.
     *
     * @param  mayInterruptIfRunning  whether the thread executing tasks should be interrupted.
     * @return {@code true} if at least one component task could be interrupted.
     */
    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        boolean canceled = false;
        for (final Future<R> c : components) {
            canceled |= c.cancel(mayInterruptIfRunning);
        }
        return canceled;
    }

    /**
     * Returns {@code true} if this task was cancelled before it completed normally.
     * This task is considered cancelled if at least one component has been cancelled.
     *
     * @return {@code true} if at least one component task was cancelled before it completed.
     */
    @Override
    public boolean isCancelled() {
        for (final Future<R> c : components) {
            if (c.isCancelled()) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this task completed.
     * Completion may be due to normal termination, an exception, or cancellation.
     *
     * @return {@code true} if all component tasks completed.
     */
    @Override
    public boolean isDone() {
        for (final Future<R> c : components) {
            if (!c.isDone()) return false;
        }
        return true;
    }

    /**
     * Waits if necessary for all computations to complete, and then retrieves the result.
     * If all task components return either {@code null} or the same {@code <R>} value,
     * then that result is returned. Otherwise the various {@code <R>} values are given
     * to {@link #merge(Collection)} for obtaining a single result.
     *
     * @return the computed result.
     * @throws CancellationException if at least one computation was cancelled.
     * @throws ExecutionException if at least one computation threw an exception.
     * @throws InterruptedException if the current thread was interrupted while waiting.
     */
    @Override
    public R get() throws InterruptedException, ExecutionException {
        try {
            return get(0, true);
        } catch (TimeoutException e) {
            // Should never happen because we specified `noTimeOut = true`
            throw new AssertionError(e);
        }
    }

    /**
     * Same as {@link #get()} but with a timeout. The given timeout is the total timeout;
     * each component task may have a smaller timeout for keeping the total equal to the
     * given value.
     *
     * @param  timeout  the maximum time to wait.
     * @param  unit     the time unit of the timeout argument.
     * @throws CancellationException if at least one computation was cancelled.
     * @throws ExecutionException if at least one computation threw an exception.
     * @throws InterruptedException if the current thread was interrupted while waiting.
     * @throws TimeoutException if the wait timed out.
     */
    @Override
    public R get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException
    {
        return get(System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeout, unit), false);
    }

    /**
     * Implementation of public {@code get(…)} methods.
     * The timeout given to this method, if not ignored, is an absolute timeout.
     *
     * @param  timeout    {@link System#nanoTime()} value when to stop waiting.
     * @param  noTimeOut  {@code true} if {@code timeout} should be ignored.
     */
    private R get(final long timeout, final boolean noTimeOut)
            throws InterruptedException, ExecutionException, TimeoutException
    {
        R singleton = null;
        Set<R> results = null;
        for (final Future<R> c : components) {
            final R r = noTimeOut ? c.get() : c.get(Math.max(0, timeout - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (r != null) {
                if (singleton == null) {
                    singleton = r;
                } else if (r != singleton) {
                    if (results == null) {
                        results = new HashSet<>();
                        results.add(singleton);
                    }
                    results.add(r);
                }
            }
        }
        if (results != null) {
            singleton = merge(results);
        }
        return singleton;
    }

    /**
     * Invoked by {@code get(…)} if there is more than one non-null instance.
     * The default implementation throws an exception.
     *
     * @param  results  all non-null instances found.
     * @return the unique instance to return.
     */
    protected R merge(final Set<R> results) {
        final R singleton = Containers.peekIfSingleton(results);
        if (singleton != null) {
            return singleton;
        }
        throw new IllegalStateException(Resources.format(Resources.Keys.NotASingleton_1, "get()"));
    }
}
