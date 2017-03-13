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
 * Placeholder for the {@link java.util.Spliterators} class.
 */
public final class Spliterators {
    /**
     * Do not allow instantiation of this class.
     */
    private Spliterators() {
    }

    /**
     * Wraps the given spliterator in an iterator.
     *
     * @param  <T>  type of elements returned by the iterator.
     * @param  spliterator  the spliterator to wrap.
     * @return the iterator for the given spliterator.
     */
    public static <T> Iterator<T> iterator(final Spliterator<? extends T> spliterator) {
        final class Iter implements Iterator<T>, Consumer<T> {
            private boolean hasNext;
            private T next;

            @Override
            public void accept(T element) {
                next = element;
            }

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    hasNext = spliterator.tryAdvance(this);
                }
                return hasNext;
            }

            @Override
            public T next() {
                if (hasNext()) {
                    hasNext = false;
                    return next;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
        return new Iter();
    }
}
