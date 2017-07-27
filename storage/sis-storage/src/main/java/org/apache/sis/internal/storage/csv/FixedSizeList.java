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
package org.apache.sis.internal.storage.csv;

import java.util.AbstractList;


/**
 * Wraps an array of fixed size. Attempts to add a new element fail when the list reached its maximal capacity.
 * Clearing the array does <strong>not</strong> set the references to {@code null}, since we need to remember
 * the values of previous passes. This implementation is designed for compliance with the part of Moving Features
 * specification saying that "if the value equals the previous value, the text for the value can be omitted".
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class FixedSizeList extends AbstractList<Object> {
    /**
     * The array where to store the values.
     * Elements in this array are usually not null even if this list is empty.
     */
    private final Object[] values;

    /**
     * Number of elements added in the {@link #values} array.
     */
    private int size;

    /**
     * Creates a new list wrapping the given array of values.
     */
    FixedSizeList(final Object[] values) {
        this.values = values;
    }

    /**
     * Returns the number of elements added in the values array.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Resets the list size to zero, but do <strong>not</strong> reset the references to {@code null}.
     */
    @Override
    public void clear() {
        size = 0;
    }

    /**
     * Returns the value at the given index without check of index validity. We do not bother
     * verifying that {@literal index < size} because this list is used only for internal purpose.
     */
    @Override
    public Object get(final int index) {
        return values[index];
    }

    /**
     * Adds the given value if we have not yet reached the maximal capacity of this list.
     */
    @Override
    public boolean add(final Object value) {
        if (size < values.length) {
            values[size++] = value;
            return true;
        }
        return false;
    }
}
