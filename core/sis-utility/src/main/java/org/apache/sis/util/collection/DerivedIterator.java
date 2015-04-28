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
import org.apache.sis.util.ObjectConverter;


/**
 * An iterator which performs conversions on the fly using the given converter.
 * If a value is converted into a null value, then this iterator skips that value.
 * Consequently this iterator can not returns null value.
 *
 * @param <S> The type of elements in the storage collection.
 * @param <E> The type of elements in this set.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class DerivedIterator<S,E> implements Iterator<E> {
    /**
     * The original iterator to wrap.
     */
    private final Iterator<S> iterator;

    /**
     * The converter from the original values to the converted values.
     */
    private final ObjectConverter<S,E> converter;

    /**
     * The next element to be returned, or {@code null}.
     */
    private transient E next;

    /**
     * Creates a new iterator wrapping the given original iterator and converting the
     * values using the given converter.
     */
    DerivedIterator(final Iterator<S> iterator, ObjectConverter<S,E> converter) {
        this.iterator  = iterator;
        this.converter = converter;
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     */
    @Override
    public boolean hasNext() {
        while (next == null) {
            if (!iterator.hasNext()) {
                return false;
            }
            next = converter.apply(iterator.next());
        }
        return true;
    }

    /**
     * Returns the next element in the iteration.
     */
    @Override
    public E next() {
        E value = next;
        next = null;
        while (value == null) {
            value = converter.apply(iterator.next());
        }
        return value;
    }

    /**
     * Removes from the underlying set the last element returned by the iterator.
     *
     * @throws UnsupportedOperationException if the underlying collection doesn't supports the {@code remove} operation.
     */
    @Override
    public void remove() {
        iterator.remove();
    }
}
