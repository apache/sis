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
package org.apache.sis.util.internal.shared;

import java.util.List;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CheckedContainer;


/**
 * A {@linkplain Collections#checkedList(List, Class) checked} {@link ArrayList}.
 * The type checks are performed at run-time in addition to the compile-time checks.
 *
 * <p>Using this class is similar to wrapping an {@link ArrayList} using the methods provided
 * in the standard {@link Collections} class, except for the following differences:</p>
 *
 * <ul>
 *   <li>Avoid one level of indirection.</li>
 *   <li>Does not accept null elements.</li>
 * </ul>
 *
 * The checks are performed only on a <em>best effort</em> basis. In current implementation,
 * holes are known to exist in use cases like {@code sublist(…).set(…)} or when using the list iterator.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <E>  the type of elements in the list.
 *
 * @see Collections#checkedList(List, Class)
 */
@SuppressWarnings("CloneableImplementsClone")           // ArrayList.clone() is sufficient.
public final class CheckedArrayList<E> extends ArrayList<E> implements CheckedContainer<E> {
    /**
     * Serial version UID for compatibility with different versions.
     */
    private static final long serialVersionUID = -8265578982723471814L;

    /**
     * The element type.
     */
    private final Class<E> type;

    /**
     * Constructs a list of the specified type.
     *
     * @param type  the element type (cannot be null).
     */
    public CheckedArrayList(final Class<E> type) {
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Constructs a list of the specified type and initial capacity.
     *
     * @param type      the element type (should not be null).
     * @param capacity  the initial capacity.
     */
    public CheckedArrayList(final Class<E> type, final int capacity) {
        super(capacity);
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Returns the given collection as a {@code CheckedArrayList} instance of the given element type.
     *
     * @param  <E>         the element type.
     * @param  collection  the collection or {@code null}.
     * @param  type        the element type.
     * @return the given collection as a {@code CheckedArrayList}, or {@code null} if the given collection was null.
     * @throws ClassCastException if an element is not of the expected type.
     */
    @SuppressWarnings("unchecked")
    public static <E> CheckedArrayList<E> castOrCopy(final Collection<?> collection, final Class<E> type) {
        if (collection == null) {
            return null;
        }
        if (collection instanceof CheckedArrayList<?> && ((CheckedArrayList<?>) collection).type == type) {
            return (CheckedArrayList<E>) collection;
        } else {
            final CheckedArrayList<E> list = new CheckedArrayList<>(type, collection.size());
            list.addAll((Collection) collection);               // addAll will perform the type checks.
            return list;
        }
    }

    /**
     * Returns the element type given at construction time.
     */
    @Override
    public Class<E> getElementType() {
        return type;
    }

    /**
     * Indicates that this collection is modifiable.
     *
     * @return {@link Mutability#MODIFIABLE}.
     */
    @Override
    public Mutability getMutability() {
        return Mutability.MODIFIABLE;
    }

    /**
     * Invoked when an illegal element has been given to the {@code add(E)} method.
     * The element may be illegal either because null or because of invalid type.
     * This method will perform only one of the following actions:
     *
     * <ul>
     *   <li>If a unmarshalling process is under way, then this method logs a warning and returns {@code null}.
     *       The {@code add(E)} caller method shall return {@code false} without throwing exception. This is a
     *       violation of {@link Collection#add(Object)} contract, but is required for unmarshalling of empty
     *       XML elements (see SIS-139 and SIS-157).</li>
     *   <li>If no unmarshalling process is under way, then this method returns a {@code String} containing the
     *       error message to give to the exception to be thrown. The {@code add(E)} caller method is responsible
     *       to throw an exception with that message. We let the caller throws the exception for reducing the
     *       stack trace depth, so the first element on the stack trace is the public {@code add(E)} method.</li>
     * </ul>
     *
     * @param  collection    the collection in which the user attempted to add an invalid element.
     * @param  element       the element that the user attempted to add (may be {@code null}).
     * @param  expectedType  the type of elements that the collection expected.
     * @return the message to give to the exception to be thrown, or {@code null} if no message shall be thrown.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-139">SIS-139</a>
     * @see <a href="https://issues.apache.org/jira/browse/SIS-157">SIS-157</a>
     */
    public static String illegalElement(final Collection<?> collection, final Object element, final Class<?> expectedType) {
        /*
         * Ignore if the current thread is in process of unmarshalling a XML document.
         * This happen when an XML element is empty (e.g. "<cit:contact/>"), in which
         * case JAXB tries to add a null element.
         */
        if (MetadataServices.getInstance().isUnmarshalling()) {
            return null;
        }
        final short key;
        final Object[] arguments;
        if (element == null) {
            key = Errors.Keys.NullCollectionElement_1;
            arguments = new Object[] {
                Classes.getShortClassName(collection) + '<' + Classes.getShortName(expectedType) + '>'
            };
        } else {
            key = Errors.Keys.IllegalArgumentClass_3;
            arguments = new Object[] {"element", expectedType, element.getClass()};
        }
        return Errors.format(key, arguments);
    }

    /**
     * Ensures that the given element is non-null and assignable to the type specified at construction time.
     *
     * @param  element  the object to check, or {@code null}.
     * @return {@code true} if the instance is valid, {@code false} if it shall be ignored.
     * @throws NullPointerException if the given element is {@code null}.
     * @throws ClassCastException if the given element is not of the expected type.
     */
    private boolean ensureValid(final E element) {
        if (type.isInstance(element)) {
            return true;
        }
        final String message = illegalElement(this, element, type);
        if (message == null) {
            /*
             * If a unmarshalling process is under way, silently discard null element.
             * This case happen when a XML element for a collection contains no child.
             * See https://issues.apache.org/jira/browse/SIS-139
             */
            return false;
        }
        if (element == null) {
            throw new NullPointerException(message);
        } else {
            throw new ClassCastException(message);
        }
    }

    /**
     * Ensures that all elements of the given collection can be added to this list.
     *
     * @param  collection  the collection to check, or {@code null}.
     * @return the potentially filtered collection of elements to add.
     * @throws NullPointerException if an element is {@code null}.
     * @throws ClassCastException if an element is not of the expected type.
     */
    @SuppressWarnings("unchecked")
    private List<E> ensureValidCollection(final Collection<? extends E> collection) {
        int count = 0;
        final Object[] array = collection.toArray();
        for (final Object element : array) {
            if (ensureValid((E) element)) {
                array[count++] = element;
            }
        }
        // Not-so-unsafe cast: we verified in the above loop that all elements are instance of E.
        // The array itself may not be an instance of E[], but this is not important for Mediator.
        return new Mediator<>(ArraysExt.resize((E[]) array, count));
    }

    /**
     * A wrapper around the given array for use by {@link CheckedArrayList#addAll(Collection)} only.
     * This wrapper violates some {@link List} method contracts, so it shall really be used only as
     * a temporary object for passing array to {@code ArrayList.addAll(…)} methods.
     * In particular {@link #toArray()} returns directly the internal array, because this is the method to be
     * invoked by {@code ArrayList.addAll(…)} (this is actually the only important method in this wrapper).
     *
     * @param  <E>  the type or list elements.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private static final class Mediator<E> extends AbstractList<E> {
        private final E[] array;
        Mediator(final E[] array)           {this.array = array;}
        @Override public int size()         {return array.length;}
        @Override public E   get(int index) {return array[index];}
        @Override public E[] toArray()      {return array;}                 // See class javadoc.
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param  index    index of element to replace.
     * @param  element  element to be stored at the specified position.
     * @return the element previously at the specified position.
     * @throws IndexOutOfBoundsException if index out of range.
     * @throws NullPointerException if the given element is {@code null}.
     * @throws ClassCastException if the given element is not of the expected type.
     */
    @Override
    public E set(final int index, final E element) {
        if (ensureValid(element)) {
            return super.set(index, element);
        }
        return get(index);
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param  element  element to be appended to this list.
     * @return always {@code true}.
     * @throws NullPointerException if the given element is {@code null}.
     * @throws ClassCastException if the given element is not of the expected type.
     */
    @Override
    public boolean add(final E element) {
        if (ensureValid(element)) {
            return super.add(element);
        }
        return false;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     *
     * @param  index  index at which the specified element is to be inserted.
     * @param  element  element to be inserted.
     * @throws IndexOutOfBoundsException if index out of range.
     * @throws NullPointerException if the given element is {@code null}.
     * @throws ClassCastException if the given element is not of the expected type.
     */
    @Override
    public void add(final int index, final E element) {
        if (ensureValid(element)) {
            super.add(index, element);
        }
    }

    /**
     * Appends all of the elements in the specified collection to the end of this list,
     * in the order that they are returned by the specified Collection's Iterator.
     *
     * @param  collection  the elements to be inserted into this list.
     * @return {@code true} if this list changed as a result of the call.
     * @throws NullPointerException if an element is {@code null}.
     * @throws ClassCastException if an element is not of the expected type.
     */
    @Override
    public boolean addAll(final Collection<? extends E> collection) {
        return super.addAll(ensureValidCollection(collection));
    }

    /**
     * Inserts all of the elements in the specified collection into this list,
     * starting at the specified position.
     *
     * @param  index  index at which to insert first element fromm the specified collection.
     * @param  collection  elements to be inserted into this list.
     * @return {@code true} if this list changed as a result of the call.
     * @throws NullPointerException if an element is {@code null}.
     * @throws ClassCastException if an element is not of the expected type.
     */
    @Override
    public boolean addAll(final int index, final Collection<? extends E> collection) {
        return super.addAll(index, ensureValidCollection(collection));
    }

    /**
     * Returns a checked sublist.
     *
     * <h4>Limitations</h4>
     * Current implementation checks only the type.
     * It does not prevent the insertion of {@code null} values.
     *
     * @param  fromIndex  index of the first element.
     * @param  toIndex    index after the last element.
     * @return the sublist in the given index range.
     */
    @Override
    public List<E> subList(final int fromIndex, final int toIndex) {
        return Collections.checkedList(super.subList(fromIndex, toIndex), type);
    }
}
