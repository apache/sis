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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * A container that ensures that all elements are assignable to a given base type.
 * Checked containers are usually {@link Collection}, but not always.
 *
 * <h2>Constraint</h2>
 * If a class implements both {@code CheckedContainer} and {@code Collection},
 * then the parameterized type shall be the same type. Example:
 *
 * {@snippet lang="java" :
 *     class MyList<E> extends AbstractList<E> implements CheckedContainer<E> { ... }
 *     }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 *
 * @param <E>  the base type of elements in the container.
 *
 * @since 0.3
 */
public interface CheckedContainer<E> {
    /**
     * Returns the base type of all elements in this container.
     *
     * @return the element type.
     */
    Class<E> getElementType();

    /**
     * Returns whether this container is modifiable, unmodifiable or immutable.
     *
     * @return the mutability status of this container.
     *
     * @since 1.6
     */
    Mutability getMutability();

    /**
     * Mutability status of a container.
     *
     * @since 1.6
     */
    enum Mutability {
        /**
         * The container is modifiable.
         * Invoking a setter method should not cause {@link UnsupportedOperationException} to be thrown.
         */
        MODIFIABLE,

        /**
         * The container supports only the removal of elements.
         * Attempts to add or modify an element will cause {@link UnsupportedOperationException} to be thrown.
         */
        REMOVE_ONLY,

        /**
         * The container is unmodifiable, but not necessarily immutable.
         * Attempts to add, remove or add an element will cause {@link UnsupportedOperationException} to be thrown.
         * However, the content of the collection way still change with time, for example if the collection is a view
         * and the underlying view changes.
         */
        UNMODIFIABLE,

        /**
         * The container is immutable. Its content does not change, and attempts to change
         * the content will cause {@link UnsupportedOperationException} to be thrown.
         */
        IMMUTABLE,

        /**
         * The mutability status of the container is unknown.
         */
        UNKNOWN;

        /**
         * Hard-coded implementation classes considered as modifiable. We require the exact classes without accepting
         * subclasses because the latter may override methods in a way that change the mutability of the collection.
         * This is set is not exhaustive. Its content is determined by Apache <abbr>SIS</abbr> needs.
         */
        private static final Set<Class<?>> MUTABLES = Set.of(ArrayDeque.class, ArrayList.class, HashSet.class, LinkedHashSet.class);

        /**
         * Returns the mutability status of the given collection.
         * If the given collection implements the {@link CheckedContainer} interface,
         * then this method returns the {@link #getMutability()} status.
         * Otherwise, this method tries to guess from the implementation class.
         * In case of doubt, this method returns {@link #UNKNOWN}.
         *
         * @param  collection  the collection for which to get the mutability status.
         * @return mutability status of the given collection.
         */
        public static Mutability of(final Collection<?> collection) {
            if (collection != null) {
                if (collection instanceof CheckedContainer<?>) {
                    return ((CheckedContainer<?>) collection).getMutability();
                }
                if (collection instanceof EnumSet<?> || MUTABLES.contains(collection.getClass())) {
                    return MODIFIABLE;
                }
            }
            return UNKNOWN;
        }
    }
}
