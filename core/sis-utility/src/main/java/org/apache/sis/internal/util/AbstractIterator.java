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
import java.util.NoSuchElementException;


/**
 * Base class for iterators that prepare the next element in advance.
 * The {@link #next} field is initially {@code null} and is reset to {@code null} after each call to {@link #next()}.
 * The {@link #hasNext()} method shall set the {@code #next} field to a non-null value if there is more elements to
 * return.
 *
 * @param <E> The type of elements to be returned by the iterator.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class AbstractIterator<E> implements Iterator<E> {
    /**
     * The next value to be returned by {@link #next()}, or {@code null} if not yet determined.
     * This field should be set by a non-null value by {@link #hasNext()}, unless there is no more elements.
     */
    protected E next;

    /**
     * For subclass constructors.
     */
    protected AbstractIterator() {
    }

    /**
     * Returns the next element in this iteration.
     *
     * @return The next element.
     */
    @Override
    public E next() {
        E value = next;
        if (value == null) {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            value = next;
        }
        next = null;
        return value;
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
