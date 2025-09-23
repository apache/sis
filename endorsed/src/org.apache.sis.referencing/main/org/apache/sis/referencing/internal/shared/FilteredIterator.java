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
package org.apache.sis.referencing.internal.shared;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * An iterator based on another iterator with some elements excluded.
 * The exclusion is implemented by looking ahead what is next element.
 * This iterator can also convert the element type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <S> the type of elements returned by the source iterator.
 * @param <T> the type of elements returned by this iterator.
 */
public final class FilteredIterator<S,T> implements Iterator<T> {
    /**
     * The source iterator.
     */
    private final Iterator<S> source;

    /**
     * Converter from elements of the source iterator to elements of this iterator.
     * If the mapper returns {@code null}, then that element will be skipped.
     */
    private final Function<S,T> mapper;

    /**
     * The next element to return, or {@code null} if unknown.
     */
    private T next;

    /**
     * Creates a new filtered iterator.
     * Elements will be converted using the given mapper.
     * If the mapper returns {@code null} for a given element, then that element will be skipped.
     *
     * @param  source  the source iterator.
     * @param  mapper  converter from elements of the source iterator to elements of this iterator.
     */
    public FilteredIterator(final Iterator<S> source, final Function<S,T> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    /**
     * Returns {@code true} if the iteration has more non-null elements.
     */
    @Override
    public boolean hasNext() {
        while (next == null) {
            if (source.hasNext()) {
                next = mapper.apply(source.next());
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the next non-null element in the iteration.
     *
     * @throws NoSuchElementException if the iteration has no more elements.
     */
    @Override
    public T next() {
        T element = next;
        next = null;
        while (element == null) {
            // We will let the source throws NoSuchElementException.
            element = mapper.apply(source.next());
        }
        return element;
    }

    /**
     * Performs the given action for each remaining element.
     */
    @Override
    public void forEachRemaining(final Consumer<? super T> action) {
        T element = next;
        if (element != null) {
            next = null;
            action.accept(element);
        }
        while (source.hasNext()) {
            element = mapper.apply(source.next());
            if (element != null) {
                action.accept(element);
            }
        }
    }
}
