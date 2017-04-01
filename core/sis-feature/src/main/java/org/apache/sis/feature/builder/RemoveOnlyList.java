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
package org.apache.sis.feature.builder;

import java.util.AbstractList;
import java.util.List;


/**
 * Wraps another list in a new list allowing only read and remove operations.
 * Addition of new values are not allowed through this {@code RemoveOnlyList}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class RemoveOnlyList<E extends TypeBuilder> extends AbstractList<E> {
    /**
     * The original list to wrap.
     */
    private final List<E> elements;

    /**
     * Creates a new list wrapping the given list.
     */
    RemoveOnlyList(final List<E> elements) {
        this.elements = elements;
    }

    /**
     * Returns the number of elements in this list.
     */
    @Override
    public int size() {
        return elements.size();
    }

    /**
     * Returns the element at the given index.
     */
    @Override
    public E get(int index) {
        return elements.get(index);
    }

    /**
     * Removes the element at the given index. The removed element is flagged as not usable anymore.
     */
    @Override
    public E remove(int index) {
        final E element = elements.get(index);
        if (element != null) {
            element.remove();
        }
        return element;
    }
}
