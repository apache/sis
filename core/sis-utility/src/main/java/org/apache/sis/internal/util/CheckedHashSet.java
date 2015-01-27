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
package org.apache.sis.internal.util;

import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashSet;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.collection.CheckedContainer;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A {@linkplain Collections#checkedSet(Set, Class) checked} {@link LinkedHashSet}.
 * The type checks are performed at run-time in addition to the compile-time checks.
 *
 * <p>Using this class is similar to wrapping a {@link LinkedHashSet} using the methods provided
 * in the standard {@link Collections} class, except for the following differences:</p>
 *
 * <ul>
 *   <li>Avoid one level of indirection.</li>
 *   <li>Does not accept null elements.</li>
 * </ul>
 *
 * @param <E> The type of elements in the set.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see Collections#checkedSet(Set, Class)
 */
public final class CheckedHashSet<E> extends LinkedHashSet<E> implements CheckedContainer<E> {
    /**
     * Serial version UID for compatibility with different versions.
     */
    private static final long serialVersionUID = 1999408533884863599L;

    /**
     * The element type.
     */
    private final Class<E> type;

    /**
     * Constructs a set of the specified type.
     *
     * @param type The element type (can not be null).
     */
    public CheckedHashSet(final Class<E> type) {
        super();
        this.type = type;
        ensureNonNull("type", type);
    }

    /**
     * Constructs a set of the specified type and initial capacity.
     *
     * @param type The element type (should not be null).
     * @param capacity The initial capacity.
     */
    public CheckedHashSet(final Class<E> type, final int capacity) {
        super(capacity);
        this.type = type;
        ensureNonNull("type", type);
    }

    /**
     * Returns the element type given at construction time.
     */
    @Override
    public Class<E> getElementType() {
        return type;
    }

    /**
     * Adds the specified element to this set if it is not already present.
     *
     * @param  element element to be added to this set.
     * @return {@code true} if the set did not already contain the specified element.
     * @throws IllegalArgumentException if the specified element is not of the expected type.
     */
    @Override
    public boolean add(final E element) throws IllegalArgumentException {
        if (type.isInstance(element)) {
            return super.add(element);
        }
        final String message = CheckedArrayList.illegalElement(this, element, type);
        if (message == null) {
            /*
             * If a unmarshalling process is under way, silently discard null element.
             * This case happen when a XML element for a collection contains no child.
             * See https://issues.apache.org/jira/browse/SIS-139
             */
            return false;
        }
        if (element == null) {
            throw new NullArgumentException(message);
        } else {
            throw new IllegalArgumentException(message);
        }
    }

    /*
     * No need to override 'addAll', since it is implemented on top of 'add'.
     */
}
