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

import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.BaseStream;
import javax.validation.constraints.NotNull;

/**
 * Delegates operations to an underlying stream provided by {@link #createDecoratedStream()}. Allows for custom logic
 * decoration. See {@link StreamDecoration} for further details.
 *
 * @param <T> Type of values contained in the stream, as defined in {@link BaseStream}
 * @param <S> Type of stream implementation, as defined in {@link BaseStream}
 *
 *  @since 1.0
 *
 * @author Alexis Manin (Geomatys)
 */
public abstract class BaseStreamDecoration<T, S extends BaseStream<T, S>> implements BaseStream<T, S> {

    private S decorated;

    private boolean closed;

    /**
     * Get previously created wrapped stream, or create it if never done.
     * @return The stream containing actual data.
     */

    protected final @NotNull S getOrCreate() {
        if (closed) throw new IllegalStateException("Stream has already been closed.");
        if (decorated == null) {
            decorated = createDecoratedStream();
        }

        return decorated;
    }

    /**
     * Operation that creates underlying stream to delegate operations to as a last resort. Note that sub-classes should
     * never call this method. Instead, please use {@link #getOrCreate()}, ensuring that decorated stream is created
     * only once, then potentially re-used multiple times.
     *
     * @return A new, non-consumed stream to delegate actions to.
     */
    protected abstract @NotNull S createDecoratedStream();

    @Override
    public void close() {
        closed = true;
        if (decorated != null) decorated.close();
    }

    @Override
    public Iterator<T> iterator() {
        return getOrCreate().iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return getOrCreate().spliterator();
    }

    @Override
    public boolean isParallel() {
        return getOrCreate().isParallel();
    }

    @Override
    public S sequential() {
        return getOrCreate().sequential();
    }

    @Override
    public S parallel() {
        return getOrCreate().parallel();
    }

    @Override
    public S unordered() {
        return getOrCreate().unordered();
    }

    @Override
    public S onClose(Runnable closeHandler) {
        return getOrCreate().onClose(closeHandler);
    }
}
