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
package org.apache.sis.internal.stream;

import java.util.stream.BaseStream;
import org.apache.sis.util.ArgumentChecks;


/**
 * Base class of all stream wrappers defined in this package.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <T>  type of values contained in the stream, as defined in {@link BaseStream}.
 * @param  <S>  type of stream interface, as defined in {@link BaseStream}.
 *
 * @since 1.1
 * @module
 */
public abstract class BaseStreamWrapper<T, S extends BaseStream<T,S>> implements BaseStream<T,S> {
    /**
     * The stream to close, or {@code null} if none. This is set at construction time
     * (typically to the same stream than the source) and should not be modified after
     * that point.
     */
    S toClose;

    /**
     * For sub-classes constructors.
     */
    BaseStreamWrapper() {
    }

    /**
     * Creates a new wrapper for the given stream.
     *
     * @param  source  the stream to wrap.
     */
    protected BaseStreamWrapper(final S source) {
        ArgumentChecks.ensureNonNull("source", source);
        toClose = source;
    }

    /**
     * Verifies that this stream is still the active one, then returns the source of this stream.
     *
     * @return the stream containing actual data.
     * @throws IllegalStateException if this stream is no longer the active stream on which to apply operations.
     */
    abstract S source();

    /**
     * Returns the exception to throw when an operation is invoked on an inactive stream.
     */
    final IllegalStateException inactive() {
        return new IllegalStateException("This stream is no longer active.");
    }

    /**
     * Returns whether stream terminal operation would execute in parallel.
     *
     * @return whether this stream works in parallel.
     */
    @Override
    public boolean isParallel() {
        return source().isParallel();
    }

    /**
     * Closes this stream. This method can be invoked on this stream even if it is not anymore
     * the active one because this is the stream referenced in a {@code try ... finally} block.
     */
    @Override
    public void close() {
        final S s = toClose;
        if (s != null) {
            toClose = null;     // Clears first in case of failure.
            s.close();
        }
    }
}
