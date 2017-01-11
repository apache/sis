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


/**
 * A {@code Spliterator} backed by an iterator.
 */
final class IteratorSpliterator<T> implements Spliterator<T> {
    /**
     * The iterator from which to get the data.
     */
    private final Iterator<T> it;

    /**
     * Creates a new {@code Spliterator} for the given iterator.
     */
    IteratorSpliterator(final Iterator<T> it) {
        this.it = it;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        final boolean hasNext = it.hasNext();
        if (hasNext) {
            action.accept(it.next());
        }
        return hasNext;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public void forEachRemaining(Consumer<? super T> action) {
        while (tryAdvance(action));
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return -1;
    }

    @Override
    public int characteristics() {
        return 0;
    }
}
