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
package org.apache.sis.internal.metadata;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.sis.util.ArgumentChecks;


/**
 * An adapter for collections of a legacy type replaced by an other collection.
 * This adapter is used for properties which have been deprecated after updating an ISO standard.
 *
 * @param <L> The legacy type.
 * @param <N> The new type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public abstract class LegacyProperties<L,N> extends AbstractCollection<L> {
    /**
     * The collection where to store the elements.
     */
    final Collection<N> elements;

    /**
     * Creates a new adapter.
     *
     * @param elements The collection where to store the elements.
     */
    protected LegacyProperties(final Collection<N> elements) {
        this.elements = elements;
    }

    /**
     * Wraps a legacy value in its new type.
     *
     * @param  value The legacy value.
     * @return The new type.
     */
    protected abstract N wrap(final L value);

    /**
     * Extracts a legacy value from the new type.
     *
     * @param  value The new type.
     * @return The legacy value, or {@code null}.
     */
    protected abstract L unwrap(final N value);

    /**
     * Returns {@code true} if this collection is empty.
     *
     * @return {@code true} if this collection is empty.
     */
    @Override
    public final boolean isEmpty() {
        return !iterator().hasNext();
    }

    /**
     * Counts the number of non-null elements.
     *
     * @return Number of non-null elements.
     */
    @Override
    public final int size() {
        int count = 0;
        final Iterator<L> it = iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }

    /**
     * Adds a new element.
     *
     * @param  element The element to add.
     * @return {@code true} if the element has been added.
     */
    @Override
    public final boolean add(final L element) {
        ArgumentChecks.ensureNonNull("element", element);
        return elements.add(wrap(element));
    }

    /**
     * Returns an iterator over the legacy elements.
     *
     * @return Iterator over the legacy elements.
     */
    @Override
    public final Iterator<L> iterator() {
        final Iterator<N> it = elements.iterator();
        return new Iterator<L>() {
            /** The next value to return, or {@code null} if not yet verified. */
            private L next;

            /** Returns {@code true} if there is more elements to iterator. */
            @Override
            public final boolean hasNext() {
                if (next != null) {
                    return true;
                }
                while (it.hasNext()) {
                    next = unwrap(it.next());
                    if (next != null) {
                        return true;
                    }
                }
                return false;
            }

            /** Returns the next element. */
            @Override
            public final L next() {
                L n = next;
                if (n == null) {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    n = next;
                }
                next = null;
                return n;
            }

            /** Removes the last element returned by {@link #next()}. */
            @Override
            public final void remove() {
                it.remove();
            }
        };
    }
}
