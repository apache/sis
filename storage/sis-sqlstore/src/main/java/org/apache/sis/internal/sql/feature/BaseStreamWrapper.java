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
package org.apache.sis.internal.sql.feature;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.BaseStream;


/**
 * Delegates operations to an underlying stream provided by {@link #createSource()}.
 * Allows for custom logic decoration. See {@link StreamWrapper} for further details.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 *
 * @param  <T>  type of values contained in the stream, as defined in {@link BaseStream}.
 * @param  <S>  type of stream implementation, as defined in {@link BaseStream}.
 *
 * @since 1.1
 * @module
 */
abstract class BaseStreamWrapper<T, S extends BaseStream<T, S>> implements BaseStream<T, S> {
    /**
     * The stream decorated by this class. Created when first needed.
     *
     * @see #getSource()
     */
    private S source;

    /**
     * Whether the stream has been closed.
     */
    private boolean closed;

    /**
     * For sub-classes constructors.
     */
    protected BaseStreamWrapper() {
    }

    /**
     * Gets previously created wrapped stream, or creates it if never done.
     *
     * @return the stream containing actual data.
     * @throws IllegalStateException if this stream is {@linkplain #close() closed}.
     */
    protected final S getSource() {
        if (closed) {
            throw new IllegalStateException("Stream is closed.");
        }
        if (source == null) {
            source = createSource();
        }
        return source;
    }

    /**
     * Creates underlying stream on which to delegate operations. Sub-classes should never call this method directly.
     * Instead, use {@link #getSource()} for ensuring that decorated stream is created only once and potentially
     * re-used multiple times.
     *
     * @return a new, non-consumed stream to delegate actions to.
     */
    protected abstract S createSource();

    /**
     * Returns an iterator for the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public Iterator<T> iterator() {
        return getSource().iterator();
    }

    /**
     * Returns a spliterator for the elements of this stream.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public Spliterator<T> spliterator() {
        return getSource().spliterator();
    }

    /**
     * Returns whether stream terminal operation would execute in parallel.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     */
    @Override
    public boolean isParallel() {
        return getSource().isParallel();
    }

    /**
     * Returns an equivalent stream that is sequential.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public S sequential() {
        return getSource().sequential();
    }

    /**
     * Returns an equivalent stream that is parallel.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public S parallel() {
        return getSource().parallel();
    }

    /**
     * Returns an equivalent stream that is unordered.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public S unordered() {
        return getSource().unordered();
    }

    /**
     * Returns an equivalent stream with an additional close handler.
     * Default implementation delegates to the {@linkplain #getSource() source} stream.
     * Note that in default implementation, the returned stream is not wrapped anymore.
     */
    @Override
    public S onClose(Runnable closeHandler) {
        return getSource().onClose(closeHandler);
    }

    /**
     * Closes the source stream and marks this stream as closed.
     */
    @Override
    public void close() {
        closed = true;
        if (source != null) {
            source.close();
            source = null;
        }
    }
}
