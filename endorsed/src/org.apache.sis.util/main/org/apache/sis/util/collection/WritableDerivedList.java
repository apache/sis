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

import java.util.List;
import java.util.function.Function;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;


/**
 * A list in which values are derived from another list using a given function.
 * The conversion is done on-the-fly every times that an element is read or stored.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <S>  type of elements in the source list.
 * @param  <E>  type of elements in this list.
 */
final class WritableDerivedList<S,E> extends DerivedList<S,E> implements CheckedContainer<E> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2285713232906970418L;

    /**
     * The function for deriving an element to store in the source list.
     */
    @SuppressWarnings("serial")
    private final Function<E,S> inverse;

    /**
     * Type of elements in this list.
     */
    private final Class<E> elementType;

    /**
     * Creates a new derived list from the specified storage list.
     *
     * @param  storage    the list which actually stores the elements.
     * @param  converter  the converter from the type in the storage list to the type in the derived list.
     */
    static <S,E> List<E> create(final List<S> storage, final ObjectConverter<S,E> converter) {
        if (converter.properties().contains(FunctionProperty.INVERTIBLE)) {
            return new WritableDerivedList<>(storage, converter);
        }
        return new DerivedList<>(storage, converter);
    }

    /**
     * Creates a new derived list wrapping the given list.
     *
     * @param  storage  the list of source elements.
     * @param  adapter  the function for deriving an element in this list from an element in the source list.
     */
    private WritableDerivedList(final List<S> storage, final ObjectConverter<S,E> adapter) {
        super(storage, adapter);
        this.inverse = adapter.inverse();
        elementType = adapter.getTargetClass();
    }

    /**
     * Creates a sub-list of the given parent list.
     */
    private WritableDerivedList(final WritableDerivedList<S,E> parent, final List<S> storage) {
        super(storage, parent.adapter);
        inverse = parent.inverse;
        elementType = parent.elementType;
    }

    /**
     * Returns the base type of all elements in this list.
     */
    @Override
    public Class<? extends E> getElementType() {
        return elementType;
    }

    /**
     * Returns whether this container is modifiable, unmodifiable or immutable.
     */
    @Override
    public Mutability getMutability() {
        return Mutability.MODIFIABLE;
    }

    /**
     * Adds the given element in the list.
     *
     * @param  element  element to add to this list.
     * @return whether the element has been added.
     */
    @Override
    public boolean add(final E element) {
        return storage.add(inverse.apply(element));
    }

    /**
     * Adds the given element at the given index in the list.
     *
     * @param  index    index where to add the element.
     * @param  element  element to add to this list.
     * @return whether the element has been added.
     */
    @Override
    public void add(final int index, final E element) {
        storage.add(index, inverse.apply(element));
    }

    /**
     * Modifies an element in this list.
     *
     * @param  index    index of the element to modify.
     * @param  element  the new element at the given index.
     * @return the old element at the given index.
     */
    @Override
    public E set(final int index, final E element) {
        return adapter.apply(storage.set(index, inverse.apply(element)));
    }

    /**
     * Returns a view of the portion of this list.
     */
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new WritableDerivedList<>(this, storage.subList(fromIndex, toIndex));
    }
}
