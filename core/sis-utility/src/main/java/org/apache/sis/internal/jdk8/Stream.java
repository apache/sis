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
package org.apache.sis.internal.jdk8;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * Placeholder for the {@link java.util.stream.Stream} interface.
 *
 * @param <T> type of values on which to iterate.
 */
public class Stream<T> {
    /**
     * The iterator wrapped by this stream.
     */
    private final Spliterator<T> it;

    /**
     * Wraps the given iterator.
     */
    Stream(final Spliterator<T> it) {
        this.it = it;
    }

    /**
     * Returns an iterator for this stream elements.
     *
     * @return iterator for this stream elements.
     */
    public Iterator<T> iterator() {
        return new Iter();
    }

    /**
     * Implementation of the iterator returned by {@link Stream#iterator()}.
     */
    private final class Iter implements Iterator<T>, Consumer<T> {
        /** 1 = has next; 2 = iteration finished; 0 = undetermined. */
        private byte status;

        /** Element to be returned by {@link #next()}. May be null. */
        private T next;

        /** For fetching the next element from the split iterator. */
        @Override public void accept(final T t) {
            next   = t;
            status = 1;
        }

        /** Verifies if there is another element that can be returned. */
        @Override public boolean hasNext() {
            if (status == 0) {
                if (!it.tryAdvance(this)) {
                    status = 2;
                }
            }
            return (status == 1);
        }

        /** Returns the next element. */
        @Override public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final T value = next;
            next = null;
            status = 0;
            return value;
        }

        /** Unsupported operation. */
        @Override public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
