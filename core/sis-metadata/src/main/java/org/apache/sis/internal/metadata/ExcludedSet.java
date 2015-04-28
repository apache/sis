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

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.io.Serializable;
import org.apache.sis.util.resources.Errors;


/**
 * A unmodifiable empty set with a customized exception message thrown by the {@link #add(Object)}
 * method. This set is used only for mutually exclusive properties, when a collection can not have
 * elements because the other property is set.
 *
 * @param  <E> The type of elements that the collection would have if it was non-empty.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see Collections#emptySet()
 */
public final class ExcludedSet<E> extends AbstractSet<E> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5502982329328318554L;

    /**
     * The name of the mutually exclusive properties.
     */
    private final String name1, name2;

    /**
     * Creates a new empty set.
     *
     * @param name1 The name of the first mutually exclusive property.
     * @param name2 The name of the second mutually exclusive property.
     */
    public ExcludedSet(final String name1, final String name2) {
        this.name1 = name1;
        this.name2 = name2;
    }

    /**
     * Returns {@code true} since this set is always empty.
     *
     * @return {@code true}
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * Returns {@code 0} since this set is always empty.
     *
     * @return 0
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * Returns the empty iterator.
     *
     * @return Empty iterator.
     */
    @Override
    public Iterator<E> iterator() {
        return Collections.<E>emptySet().iterator();
    }

    /**
     * Unconditionally throws a {@link UnsupportedOperationException} with a message
     * saying which properties are mutually exclusive.
     *
     * @param  e Ignored.
     * @return Never return.
     */
    @Override
    public boolean add(final E e) {
        throw new UnsupportedOperationException(Errors.format(
                Errors.Keys.CanNotAddToExclusiveSet_2, name1, name2));
    }
}
