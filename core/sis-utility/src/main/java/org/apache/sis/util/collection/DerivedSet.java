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
import java.io.Serializable;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.SetOfUnknownSize;


/**
 * A set whose values are derived <cite>on-the-fly</cite> from an other set.
 * Conversions are performed when needed by two converters:
 *
 * <ul>
 *   <li>The {@linkplain #iterator() iterator} obtain the derived values using the {@linkplain #converter}.</li>
 *   <li>Queries ({@link #contains contains}) and write operations ({@link #add add}, {@link #remove remove})
 *       obtain the storage values using the {@link Invertible#inverse} converter.</li>
 * </ul>
 *
 * <div class="section">Constraints</div>
 * <ul>
 *   <li>This set does not support {@code null} values, since {@code null} is used as a
 *       sentinel value when no mapping from {@linkplain #storage} to {@code this} exists.</li>
 *   <li>Instances of this class are serializable if their underlying {@linkplain #storage} set
 *       and the {@linkplain #converter} are serializable.</li>
 *   <li>This class performs no synchronization by itself. Nevertheless instances of this class
 *       may be thread-safe (depending on the sub-class implementation) if the underlying
 *       {@linkplain #storage} set (including its iterator) and the {@linkplain #converter}
 *       are thread-safe.</li>
 * </ul>
 *
 * <div class="section">Performance considerations</div>
 * This class does not cache any value, since the {@linkplain #storage} set is presumed modifiable.
 * If the storage set is known to be immutable, then sub-classes may consider to cache some values,
 * especially the result of the {@link #size()} method.
 *
 * @param <S> The type of elements in the storage set.
 * @param <E> The type of elements in this set.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
class DerivedSet<S,E> extends SetOfUnknownSize<E> implements CheckedContainer<E>, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6309535868745970619L;

    /**
     * The storage set whose values are derived from.
     */
    protected final Set<S> storage;

    /**
     * The converter from the storage to the derived type.
     */
    protected final ObjectConverter<S,E> converter;

    /**
     * Creates a new derived set from the specified storage set.
     *
     * @param storage   The set which actually store the elements.
     * @param converter The converter from the type in the storage set to the type in the derived set.
     */
    static <S,E> Set<E> create(final Set<S> storage, final ObjectConverter<S,E> converter) {
        final Set<FunctionProperty> properties = converter.properties();
        if (properties.contains(FunctionProperty.INVERTIBLE)) {
            if (FunctionProperty.isBijective(properties)) {
                return new Bijective<S,E>(storage, converter);
            }
            return new Invertible<S,E>(storage, converter);
        }
        return new DerivedSet<S,E>(storage, converter);
    }

    /**
     * Creates a new derived set from the specified storage set.
     *
     * @param storage   The set which actually store the elements.
     * @param converter The type of elements in this derived set.
     */
    private DerivedSet(final Set<S> storage, final ObjectConverter<S,E> converter) {
        this.storage   = storage;
        this.converter = converter;
    }

    /**
     * Returns the derived element type.
     */
    @Override
    public final Class<E> getElementType() {
        return converter.getTargetClass();
    }

    /**
     * Returns an iterator over the elements contained in this set.
     * The iterator will invokes the {@link #baseToDerived(Object)} method for each element.
     *
     * @return an iterator over the elements contained in this set.
     */
    @Override
    public final Iterator<E> iterator() {
        return new DerivedIterator<S,E>(storage.iterator(), converter);
    }

    /**
     * Returns the number of elements in this set. The default implementation counts
     * the number of elements returned by the {@link #iterator() iterator}.
     * Subclasses are encouraged to cache this value if they know that the
     * {@linkplain #storage} set is immutable.
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
        return !storage.isEmpty() || !iterator().hasNext();
    }

    /**
     * Ensures that this set contains the specified element.
     * This method first checks if the given element is non-null,
     * then delegates to the {@link #storage} set like below:
     *
     * {@preformat java
     *     return storage.add(inverse.apply(element));
     * }
     *
     * @param  element element whose presence in this set is to be ensured.
     * @return {@code true} if the set changed as a result of the call.
     * @throws UnsupportedOperationException if the {@linkplain #storage} set doesn't
     *         supports the {@code add} operation.
     */
    @Override
    public boolean add(final E element) throws UnsupportedOperationException {
        return add(element, converter.inverse().apply(element));
    }

    /**
     * Implementation of the {@link #add(Object)} method adding the given converted value
     * to the storage set. The {@code original} value is used only for formatting an error
     * message in case of failure.
     */
    final boolean add(final E original, final S element) {
        if (element == null) {
            throw new UnconvertibleObjectException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "element", original));
        }
        return storage.add(element);
    }

    /**
     * A {@link DerivedSet} for invertible converters. Availability of the inverse conversion
     * allows us to delegate the {@link #contains(Object)} and {@linkplain #remove(Object)}
     * operations to the {@linkplain #storage} set instead than iterating over all elements.
     *
     * @param <S> The type of elements in the storage set.
     * @param <E> The type of elements in this set.
     */
    private static class Invertible<S,E> extends DerivedSet<S,E> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5336633027232952482L;

        /**
         * The converter from the derived to the storage type.
         */
        private final ObjectConverter<E,S> inverse;

        /**
         * Creates a new derived set from the specified storage set.
         *
         * @param storage   The set which actually store the elements.
         * @param converter The type of elements in this derived set.
         */
        Invertible(final Set<S> storage, final ObjectConverter<S,E> converter) {
            super(storage, converter);
            inverse = converter.inverse();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final boolean add(final E element) throws UnsupportedOperationException {
            return add(element, inverse.apply(element));
        }

        /**
         * Returns {@code true} if this set contains the specified element.
         * This method first checks if the given element is an instance of {@link #getElementType()},
         * then delegates to the {@link #storage} set like below:
         *
         * {@preformat java
         *     return storage.contains(inverse.apply(element));
         * }
         *
         * @param  element object to be checked for containment in this set.
         * @return {@code true} if this set contains the specified element.
         */
        @Override
        public final boolean contains(final Object element) {
            final Class<E> type = getElementType();
            return type.isInstance(element) && storage.contains(inverse.apply(type.cast(element)));
        }

        /**
         * Removes a single instance of the specified element from this set.
         * This method first checks if the given element is an instance of {@link #getElementType},
         * then delegates to the {@link #storage} set like below:
         *
         * {@preformat java
         *     return storage.remove(inverse.apply(element));
         * }
         *
         * @param  element element to be removed from this set, if present.
         * @return {@code true} if the set contained the specified element.
         * @throws UnsupportedOperationException if the {@linkplain #storage} set doesn't
         *         supports the {@code remove} operation.
         */
        @Override
        public final boolean remove(final Object element) throws UnsupportedOperationException {
            final Class<E> type = getElementType();
            return type.isInstance(element) && storage.remove(inverse.apply(type.cast(element)));
        }
    }

    /**
     * A {@link DerivedSet} for converters that are both invertible and bijective.
     * The bijection allows us to query the {@linkplain #storage} set size directly
     * instead than iterating over all elements.
     *
     * @param <S> The type of elements in the storage set.
     * @param <E> The type of elements in this set.
     */
    private static final class Bijective<S,E> extends Invertible<S,E> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -7601944988804380342L;

        /**
         * Creates a new derived set from the specified storage set.
         *
         * @param storage   The set which actually store the elements.
         * @param converter The type of elements in this derived set.
         */
        Bijective(final Set<S> storage, final ObjectConverter<S,E> converter) {
            super(storage, converter);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            return storage.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty() {
            return storage.isEmpty();
        }
    }
}
