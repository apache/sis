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

import java.util.List;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CheckedContainer;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


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
 * @param <E> The type of elements in the list.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.4
 * @module
 *
 * @see Collections#checkedList(List, Class)
 */
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
     * @param type The element type (can not be null).
     */
    public CheckedArrayList(final Class<E> type) {
        super();
        this.type = type;
        ensureNonNull("type", type);
    }

    /**
     * Constructs a list of the specified type and initial capacity.
     *
     * @param type The element type (should not be null).
     * @param capacity The initial capacity.
     */
    public CheckedArrayList(final Class<E> type, final int capacity) {
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
     *       to thrown an exception with that message. We let the caller throw the exception for reducing the
     *       stack trace depth, so the first element on the stack trace is the public {@code add(E)} method.</li>
     * </ul>
     *
     * @param  collection   The collection in which the user attempted to add an invalid element.
     * @param  element      The element that the user attempted to add (may be {@code null}).
     * @param  expectedType The type of elements that the collection expected.
     * @return The message to give to the exception to be thrown, or {@code null} if no message shall be thrown.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-139">SIS-139</a>
     * @see <a href="https://issues.apache.org/jira/browse/SIS-157">SIS-157</a>
     */
    public static String illegalElement(final Collection<?> collection, final Object element, final Class<?> expectedType) {
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
        final Context context = Context.current();
        if (context != null) {
            Context.warningOccured(context, collection.getClass(), "add", Errors.class, key, arguments);
            return null;
        } else {
            return Errors.format(key, arguments);
        }
    }

    /**
     * Ensures that the given element is non-null and assignable to the type
     * specified at construction time.
     *
     * @param  element the object to check, or {@code null}.
     * @return {@code true} if the instance is valid, {@code false} if it shall be ignored.
     * @throws IllegalArgumentException if the specified element can not be added to this list.
     */
    private boolean ensureValid(final E element) throws IllegalArgumentException {
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
            throw new NullArgumentException(message);
        } else {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Ensures that all elements of the given collection can be added to this list.
     *
     * @param  collection the collection to check, or {@code null}.
     * @return The potentially filtered collection of elements to add.
     * @throws IllegalArgumentException if at least one element can not be added to this list.
     */
    @SuppressWarnings("unchecked")
    private List<E> ensureValidCollection(final Collection<? extends E> collection) throws IllegalArgumentException {
        int count = 0;
        final Object[] array = collection.toArray();
        for (final Object element : array) {
            if (ensureValid((E) element)) {
                array[count++] = element;
            }
        }
        // Not-so-unsafe cast: we verified in the above loop that all elements are instance of E.
        // The array itself may not be an instance of E[], but this is not important for Mediator.
        return new Mediator<E>(ArraysExt.resize((E[]) array, count));
    }

    /**
     * A wrapper around the given array for use by {@link #addAll(Collection)} only.  This wrapper violates
     * some {@link List} method contracts, so it must really be used only as a temporary object for passing
     * the array to {@code AbstractList.addAll(…)} implementation. In particular {@link #toArray()} returns
     * directly the internal array, because this is the method to be invoked by {@code addAll(…)}  (this is
     * actually the only important method for this wrapper).
     */
    private static final class Mediator<E> extends AbstractList<E> {
        private final E[] array;
        Mediator(final E[] array)           {this.array = array;}
        @Override public int size()         {return array.length;}
        @Override public E   get(int index) {return array[index];}
        @Override public E[] toArray()      {return array;} // See class javadoc.
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param  index   index of element to replace.
     * @param  element element to be stored at the specified position.
     * @return the element previously at the specified position.
     * @throws IndexOutOfBoundsException if index out of range.
     * @throws IllegalArgumentException if the specified element is not of the expected type.
     */
    @Override
    public E set(final int index, final E element) throws IllegalArgumentException {
        if (ensureValid(element)) {
            return super.set(index, element);
        }
        return get(index);
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param  element element to be appended to this list.
     * @return always {@code true}.
     * @throws IllegalArgumentException if the specified element is not of the expected type.
     */
    @Override
    public boolean add(final E element) throws IllegalArgumentException {
        if (ensureValid(element)) {
            return super.add(element);
        }
        return false;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     *
     * @param  index index at which the specified element is to be inserted.
     * @param  element element to be inserted.
     * @throws IndexOutOfBoundsException if index out of range.
     * @throws IllegalArgumentException if the specified element is not of the expected type.
     */
    @Override
    public void add(final int index, final E element) throws IllegalArgumentException {
        if (ensureValid(element)) {
            super.add(index, element);
        }
    }

    /**
     * Appends all of the elements in the specified collection to the end of this list,
     * in the order that they are returned by the specified Collection's Iterator.
     *
     * @param  collection the elements to be inserted into this list.
     * @return {@code true} if this list changed as a result of the call.
     * @throws IllegalArgumentException if at least one element is not of the expected type.
     */
    @Override
    public boolean addAll(final Collection<? extends E> collection) throws IllegalArgumentException {
        return super.addAll(ensureValidCollection(collection));
    }

    /**
     * Inserts all of the elements in the specified collection into this list,
     * starting at the specified position.
     *
     * @param  index index at which to insert first element fromm the specified collection.
     * @param  collection elements to be inserted into this list.
     * @return {@code true} if this list changed as a result of the call.
     * @throws IllegalArgumentException if at least one element is not of the expected type.
     */
    @Override
    public boolean addAll(final int index, final Collection<? extends E> collection) throws IllegalArgumentException {
        return super.addAll(index, ensureValidCollection(collection));
    }
}
