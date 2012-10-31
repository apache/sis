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

import java.util.Set;
import java.util.Iterator;
import java.util.AbstractSet;
import java.io.Serializable;
import org.apache.sis.util.Decorator;
import org.apache.sis.util.ArgumentChecks;


/**
 * A set whose values are derived <cite>on-the-fly</cite> from an other set.
 * Conversions are performed when needed by two methods:
 *
 * <ul>
 *   <li>The {@linkplain #iterator() iterator} obtain the derived values by calls to the
 *       {@link #baseToDerived(Object)} method.</li>
 *   <li>Queries ({@link #contains contains}) and write operations ({@link #add add},
 *       {@link #remove remove}) obtain the storage values by calls to the
 *       {@link #derivedToBase(Object)} method.</li>
 * </ul>
 *
 * {@section Constraints}
 * <ul>
 *   <li>This set does not support {@code null} values, since {@code null} is used as a
 *       sentinel value when no mapping from {@linkplain #base} to {@code this} exists.</li>
 *   <li>Instances of this class are serializable if their underlying {@linkplain #base} set
 *       is serializable.</li>
 *   <li>This class performs no synchronization by itself. Nevertheless instances of this class
 *       may be thread-safe (depending on the sub-class implementation) if the underlying
 *       {@linkplain #base} set (including its iterator) is thread-safe.</li>
 * </ul>
 *
 * {@section Performance considerations}
 * This class does not cache any value, since the {@linkplain #base} set is presumed modifiable.
 * If the base set is known to be immutable, then sub-classes may consider to cache some values,
 * especially the result of the {@link #size()} method.
 *
 * @param <B> The type of elements in the backing set.
 * @param <E> The type of elements in this set.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 */
@Decorator(Set.class)
public abstract class DerivedSet<B,E> extends AbstractSet<E> implements CheckedContainer<E>, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4662336508586424581L;

    /**
     * The base set whose values are derived from.
     *
     * @see #baseToDerived(Object)
     * @see #derivedToBase(Object)
     */
    protected final Set<B> base;

    /**
     * The derived type.
     */
    private final Class<E> derivedType;

    /**
     * Creates a new derived set from the specified base set.
     *
     * @param base The base set.
     * @param derivedType The type of elements in this derived set.
     */
    public DerivedSet(final Set<B> base, final Class<E> derivedType) {
        ArgumentChecks.ensureNonNull("base",        this.base        = base);
        ArgumentChecks.ensureNonNull("derivedType", this.derivedType = derivedType);
    }

    /**
     * Returns the derived element type.
     */
    @Override
    public Class<E> getElementType() {
        return derivedType;
    }

    /**
     * Transforms a value in the {@linkplain #base} set to a value in this set.
     * If there is no mapping in the derived set for the specified element,
     * then this method returns {@code null}.
     *
     * @param  element A value in the {@linkplain #base} set.
     * @return The value that this view should contains instead of {@code element}, or {@code null}.
     */
    protected abstract E baseToDerived(final B element);

    /**
     * Transforms a value in this set to a value in the {@linkplain #base} set.
     *
     * @param  element A value in this set.
     * @return The value stored in the {@linkplain #base} set.
     */
    protected abstract B derivedToBase(final E element);

    /**
     * Returns an iterator over the elements contained in this set.
     * The iterator will invokes the {@link #baseToDerived(Object)} method for each element.
     *
     * @return an iterator over the elements contained in this set.
     */
    @Override
    public Iterator<E> iterator() {
        return new Iter(base.iterator());
    }

    /**
     * Returns the number of elements in this set. The default implementation counts
     * the number of elements returned by the {@link #iterator() iterator}.
     * Subclasses are encouraged to cache this value if they know that the
     * {@linkplain #base} set is immutable.
     *
     * @return the number of elements in this set.
     */
    @Override
    public int size() {
        int count = 0;
        for (final Iterator<E> it=iterator(); it.hasNext();) {
            it.next();
            count++;
        }
        return count;
    }

    /**
     * Returns {@code true} if this set contains no elements.
     *
     * @return {@code true} if this set contains no elements.
     */
    @Override
    public boolean isEmpty() {
        return !base.isEmpty() || !iterator().hasNext();
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     * This method first checks if the given element is an instance of {@link #derivedType},
     * then delegates to the {@link #base} set like below:
     *
     * {@preformat java
     *     return base.contains(derivedToBase(element));
     * }
     *
     * @param  element object to be checked for containment in this set.
     * @return {@code true} if this set contains the specified element.
     */
    @Override
    public boolean contains(final Object element) {
        return derivedType.isInstance(element) && base.contains(derivedToBase(derivedType.cast(element)));
    }

    /**
     * Ensures that this set contains the specified element.
     * This method first checks if the given element is non-null,
     * then delegates to the {@link #base} set like below:
     *
     * {@preformat java
     *     return base.add(derivedToBase(element));
     * }
     *
     * @param  element element whose presence in this set is to be ensured.
     * @return {@code true} if the set changed as a result of the call.
     * @throws UnsupportedOperationException if the {@linkplain #base} set doesn't
     *         supports the {@code add} operation.
     */
    @Override
    public boolean add(final E element) throws UnsupportedOperationException {
        ArgumentChecks.ensureNonNull("element", element);
        return base.add(derivedToBase(element));
    }

    /**
     * Removes a single instance of the specified element from this set.
     * This method first checks if the given element is an instance of {@link #derivedType},
     * then delegates to the {@link #base} set like below:
     *
     * {@preformat java
     *     return base.remove(derivedToBase(element));
     * }
     *
     * @param  element element to be removed from this set, if present.
     * @return {@code true} if the set contained the specified element.
     * @throws UnsupportedOperationException if the {@linkplain #base} set doesn't
     *         supports the {@code remove} operation.
     */
    @Override
    public boolean remove(final Object element) throws UnsupportedOperationException {
        return derivedType.isInstance(element) && base.remove(derivedToBase(derivedType.cast(element)));
    }

    /**
     * Iterates through the elements in the set.
     */
    @Decorator(Iterator.class)
    private final class Iter implements Iterator<E> {
        /**
         * The iterator from the {@linkplain DerivedSet#base} set.
         */
        private final Iterator<B> iterator;

        /**
         * The next element to be returned, or {@code null}.
         */
        private transient E next;

        /**
         * The iterator from the {@linkplain DerivedSet#base} set.
         */
        public Iter(final Iterator<B> iterator) {
            this.iterator = iterator;
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
                next = baseToDerived(iterator.next());
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
                value = baseToDerived(iterator.next());
            }
            return value;
        }

        /**
         * Removes from the underlying set the last element returned by the iterator.
         *
         * @throws UnsupportedOperationException if the {@linkplain #base} set doesn't
         *         supports the {@code remove} operation.
         */
        @Override
        public void remove() {
            iterator.remove();
        }
    }
}
