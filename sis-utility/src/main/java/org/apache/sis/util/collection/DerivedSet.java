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
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.math.FunctionProperty;


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
 * {@section Constraints}
 * <ul>
 *   <li>This set does not support {@code null} values, since {@code null} is used as a
 *       sentinel value when no mapping from {@linkplain #base} to {@code this} exists.</li>
 *   <li>Instances of this class are serializable if their underlying {@linkplain #base} set
 *       and the {@linkplain #converter} are serializable.</li>
 *   <li>This class performs no synchronization by itself. Nevertheless instances of this class
 *       may be thread-safe (depending on the sub-class implementation) if the underlying
 *       {@linkplain #base} set (including its iterator) and the {@linkplain #converter}
 *       are thread-safe.</li>
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
class DerivedSet<B,E> extends AbstractSet<E> implements CheckedContainer<E>, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4662336508586424581L;

    /**
     * The base set whose values are derived from.
     */
    protected final Set<B> base;

    /**
     * The converter from the base to the derived type.
     */
    protected final ObjectConverter<B,E> converter;

    /**
     * Creates a new derived set from the specified base set.
     *
     * @param base      The base set.
     * @param converter The converter from the type in the base set to the type in the derived set.
     */
    static <B,E> Set<E> create(final Set<B> base, final ObjectConverter<B,E> converter) {
        final Set<FunctionProperty> properties = converter.properties();
        if (properties.contains(FunctionProperty.INVERTIBLE)) {
            if (FunctionProperty.isBijective(properties)) {
                return new Bijective<>(base, converter);
            }
            return new Invertible<>(base, converter);
        }
        return new DerivedSet<>(base, converter);
    }

    /**
     * Creates a new derived set from the specified base set.
     *
     * @param base The base set.
     * @param converter The type of elements in this derived set.
     */
    private DerivedSet(final Set<B> base, final ObjectConverter<B,E> converter) {
        this.base      = base;
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
        return new DerivedIterator<>(base.iterator(), converter);
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
     * Ensures that this set contains the specified element.
     * This method first checks if the given element is non-null,
     * then delegates to the {@link #base} set like below:
     *
     * {@preformat java
     *     return base.add(inverse.convert(element));
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
        return base.add(converter.inverse().convert(element));
    }

    /**
     * A {@link DerivedSet} for invertible converters. Availability of the inverse conversion
     * allows us to delegate the {@link #contains(Object)} and {@linkplain #remove(Object)}
     * operations to the {@linkplain #base} set instead than iterating over all elements.
     *
     * @param <B> The type of elements in the backing set.
     * @param <E> The type of elements in this set.
     */
    private static class Invertible<B,E> extends DerivedSet<B,E> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 5957167307119709856L;

        /**
         * The converter from the derived to the base type.
         */
        private final ObjectConverter<E,B> inverse;

        /**
         * Creates a new derived set from the specified base set.
         *
         * @param base The base set.
         * @param converter The type of elements in this derived set.
         */
        Invertible(final Set<B> base, final ObjectConverter<B,E> converter) {
            super(base, converter);
            inverse = converter.inverse();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final boolean add(final E element) throws UnsupportedOperationException {
            ArgumentChecks.ensureNonNull("element", element);
            return base.add(inverse.convert(element));
        }

        /**
         * Returns {@code true} if this set contains the specified element.
         * This method first checks if the given element is an instance of {@link #getElementType()},
         * then delegates to the {@link #base} set like below:
         *
         * {@preformat java
         *     return base.contains(inverse.convert(element));
         * }
         *
         * @param  element object to be checked for containment in this set.
         * @return {@code true} if this set contains the specified element.
         */
        @Override
        public final boolean contains(final Object element) {
            final Class<? extends E> type = getElementType();
            return type.isInstance(element) && base.contains(inverse.convert(type.cast(element)));
        }

        /**
         * Removes a single instance of the specified element from this set.
         * This method first checks if the given element is an instance of {@link #getElementType},
         * then delegates to the {@link #base} set like below:
         *
         * {@preformat java
         *     return base.remove(inverse.convert(element));
         * }
         *
         * @param  element element to be removed from this set, if present.
         * @return {@code true} if the set contained the specified element.
         * @throws UnsupportedOperationException if the {@linkplain #base} set doesn't
         *         supports the {@code remove} operation.
         */
        @Override
        public final boolean remove(final Object element) throws UnsupportedOperationException {
            final Class<? extends E> type = getElementType();
            return type.isInstance(element) && base.remove(inverse.convert(type.cast(element)));
        }
    }

    /**
     * A {@link DerivedSet} for converters that are both invertible and bijective.
     * The bijection allows us to query the {@linkplai #base} set size directly
     * instead than iterating over all elements.
     *
     * @param <B> The type of elements in the backing set.
     * @param <E> The type of elements in this set.
     */
    private static final class Bijective<B,E> extends Invertible<B,E> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -7601944988804380342L;

        /**
         * Creates a new derived set from the specified base set.
         *
         * @param base The base set.
         * @param converter The type of elements in this derived set.
         */
        Bijective(final Set<B> base, final ObjectConverter<B,E> converter) {
            super(base, converter);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            return base.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty() {
            return base.isEmpty();
        }
    }
}
