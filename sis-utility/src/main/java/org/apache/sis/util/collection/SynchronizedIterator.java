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

import java.util.Iterator;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;


/**
 * An iterator synchronized on the given lock. The functionality is equivalent to the one provided
 * by {@link java.util.Collections#synchronizedSet} iterator, except that the synchronization is
 * performed on an arbitrary lock.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 *
 * @see java.util.Collections#synchronizedSet
 */
@ThreadSafe
final class SynchronizedIterator<E> implements Iterator<E> {
    /**
     * The wrapped iterator.
     */
    @GuardedBy("lock")
    private final Iterator<E> iterator;

    /**
     * The lock.
     */
    private final Object lock;

    /**
     * Creates a new iterator.
     */
    SynchronizedIterator(final Iterator<E> iterator, final Object lock) {
        this.iterator = iterator;
        this.lock = lock;
    }

    /**
     * Returns {@code true} if there is more elements to iterate over.
     */
    @Override
    public boolean hasNext() {
        synchronized (lock) {
            return iterator.hasNext();
        }
    }

    /**
     * Returns the next element in iterator order.
     */
    @Override
    public E next() {
        synchronized (lock) {
            return iterator.next();
        }
    }

    /**
     * Removes the last iterated element.
     */
    @Override
    public void remove() {
        synchronized (lock) {
            iterator.remove();
        }
    }
}
