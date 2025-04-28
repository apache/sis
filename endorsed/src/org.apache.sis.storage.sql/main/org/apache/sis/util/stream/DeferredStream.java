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
package org.apache.sis.util.stream;

import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * A stream which delay the creation of the source stream as long as possible.
 * This stream gives to subclasses an opportunity to collect more information before to create the source.
 * The source stream is implemented by a {@link Spliterator} created by {@link #createSourceIterator()}.
 * The call to that method is deferred until a terminal operation is invoked.
 *
 * <h2>Example</h2>
 * If a stream can count its elements efficiently (e.g. using a {@code COUNT} query on SQL databases),
 * a {@code DeferredStream} subclass can override the {@link #count()} method for running the count query
 * instead of counting elements of the stream manually.
 *
 * <p>Deferred streams are also useful with intermediate operations. For example, a subclass can override
 * the {@link #skip(long)} and {@link #limit(long)} methods for modifying the SQL query with addition of
 * {@code OFFSET} and {@code FETCH NEXT} clauses before the worker stream is created.</p>
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <T>  the type of objects contained in the stream, as specified in {@link Stream} interface.
 */
public abstract class DeferredStream<T> extends StreamWrapper<T> {
    /**
     * A proxy to the handler to run for releasing resources. This is registered as a stream close handler
     * immediately at stream creation time, but the actual work to do for releasing resource is set later,
     * at {@link #createSourceIterator()} creation time.
     */
    private final CloseHandler closeHandler;

    /**
     * Implementation of {@link #closeHandler}. This class allows to invoke {@link Stream#onClose(Runnable)}
     * early while specifying the actual close action later, at {@link #createSourceIterator()} invocation time.
     * This is necessary because it is too late to register a close handler on the stream when the worker
     * is created, because the terminal operation already started at that time.
     */
    private static final class CloseHandler implements Runnable {
        /**
         * Handler to run for releasing resources, or {@code null} if none.
         * This is reset to {@code null} after usage.
         *
         * @see #setCloseHandler(AutoCloseable)
         */
        AutoCloseable handler;

        /**
         * If there is a read or write lock to unlock, that lock. Otherwise {@code null}.
         * This is non-null only with databases that do not support concurrent transactions well.
         *
         * @see #lock(ReadWriteLock)
         */
        Lock lock;

        /**
         * Invoked by the stream for disposing the resources.
         */
        @Override
        public void run() {
            final AutoCloseable h = handler;
            final Lock c = lock;
            handler = null;
            lock    = null;
            try {
                if (h != null) try {
                    h.close();
                } catch (Exception e) {
                    throw cannotExecute(e);
                }
            } finally {
                if (c != null) {
                    c.unlock();
                }
            }
        }
    }

    /**
     * Creates a new deferred stream.
     *
     * @param characteristics  characteristics of the iterator to be created by {@link #createSourceIterator()}.
     * @param parallel         whether the stream is initially parallel.
     */
    protected DeferredStream(final int characteristics, final boolean parallel) {
        source = StreamSupport.stream(this::terminal, characteristics, parallel);
        source.onClose(closeHandler = new CloseHandler());
        toClose = source;
    }

    /**
     * Creates the worker iterator and marks this stream as not active anymore.
     * This method is invoked by {@link StreamSupport} when a terminal operation is invoked.
     */
    private Spliterator<T> terminal() {
        source = null;
        try {
            return createSourceIterator();
        } catch (Exception cause) {
            throw cannotExecute(cause);
            /*
             * The close handler will be invoked later assuming that the user created the stream in a
             * `try ... finally` block. We could invoke the close handler here as a safety, but we do
             * not do that in order to have more predictable and consistent behavior.
             */
        }
    }

    /**
     * Creates the iterator which will provide the actual data.
     * The {@linkplain Spliterator#characteristics() characteristics} of the returned iterator
     * must be the {@code characteristics} argument given to the {@code DeferredStream} constructor.
     *
     * <p>This method is invoked at most once, generally when a stream terminal operation is invoked.
     * After this method is invoked, this stream will not be active anymore.
     * The stream returned by the public methods should be used instead.</p>
     *
     * @return an iterator over the elements.
     * @throws Exception if the iterator cannot be created.
     */
    protected abstract Spliterator<T> createSourceIterator() throws Exception;

    /**
     * Registers a handler to run for releasing resources created by the worker.
     * This method can be invoked by the {@link #createSourceIterator()} implementation.
     * The specified handler will be executed exactly once, unless it is discarded
     * by a subsequent call to {@code setCloseHandler(…)}.
     *
     * <p>This method can be invoked many times, each invocation replacing the previous handler.
     * The following example uses JDBC connection and assumes that {@code MyIterator} implements
     * {@link AutoCloseable}, and that the resource disposal done by that method includes closing
     * the JDBC connection:</p>
     *
     * {@snippet lang="java" :
     *     @Override
     *     protected Spliterator<T> createSourceIterator() throws SQLException {
     *         Connection c = ...;
     *         setCloseHandler(c);
     *         MyIterator worker = ...;
     *         setCloseHandler(worker);
     *         return worker;
     *     }
     * }
     *
     * @param  handler  the action to execute for releasing resources, includes the case when
     *                  {@code createSourceIterator()} fails. Can be {@code null} for no action,
     */
    protected final void setCloseHandler(final AutoCloseable handler) {
        closeHandler.handler = handler;
    }

    /**
     * Sets the read lock.
     * Locks are used only with databases that do not support concurrent transactions well.
     *
     * @param  lock  the lock, or {@code null} if none.
     */
    protected final void lock(final ReadWriteLock lock) {
        if (lock != null) {
            (closeHandler.lock = lock.readLock()).lock();
        }
    }

    /**
     * Closes the connection (if any) and unlock.
     */
    protected final void unlock() {
        closeHandler.run();
    }

    /**
     * Creates an unchecked exception for an operation that cannot be executed because of the specified cause.
     *
     * @param  cause  the cause about why the operation cannot be executed.
     * @return the unchecked exception to throw. May be {@code cause} itself.
     */
    public static RuntimeException cannotExecute(final Exception cause) {
        final Exception unwrap = Exceptions.unwrap(cause);
        if (unwrap instanceof RuntimeException) {
            return (RuntimeException) unwrap;
        } else if (cause instanceof RuntimeException) {
            return (RuntimeException) cause;
        } else {
            return new BackingStoreException(cause.toString(), unwrap);
        }
    }
}
