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

import java.io.Serializable;
import java.util.AbstractList;
import org.apache.sis.util.ArgumentChecks;

// Related to JDK7
import java.util.Objects;


/**
 * An unmodifiable view of an array. Invoking
 *
 * {@preformat java
 *     List<?> list = UnmodifiableArrayList.wrap(array);
 * }
 *
 * is equivalent to
 *
 * {@preformat java
 *     List<?> list = Collections.unmodifiableList(Arrays.asList(array));
 * }
 *
 * except that this class uses one less level of indirection. Despite that advantage being minor,
 * this class is defined because extensively used in the SIS library.
 *
 * @param <E> The type of elements in the list.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
public class UnmodifiableArrayList<E> extends AbstractList<E> implements CheckedContainer<E>, Serializable {
    /**
     * For compatibility with different versions.
     */
    private static final long serialVersionUID = -3605810209653785967L;

    /**
     * The wrapped array.
     */
    private final E[] array;

    /**
     * Creates a new instance wrapping the given array. A direct reference to the given array is
     * retained (i.e. the array is <strong>not</strong> cloned). Consequently the given array
     * shall not be modified after construction if this list is intended to be immutable.
     *
     * <p>This constructor is for sub-classing only. Users should invoke the {@link #wrap(E[])}
     * static method instead.</p>
     *
     * @param array The array to wrap.
     */
    @SafeVarargs
    protected UnmodifiableArrayList(final E... array) {
        ArgumentChecks.ensureNonNull("array", array);
        this.array = array;
    }

    /**
     * Creates a new instance wrapping the given array. A direct reference to the given array is
     * retained (i.e. the array is <strong>not</strong> cloned). Consequently the given array
     * shall not be modified after construction if the returned list is intended to be immutable.
     *
     * @param  <E> The type of elements in the list.
     * @param  array The array to wrap, or {@code null} if none.
     * @return The given array wrapped in an unmodifiable list, or {@code null} if the given
     *         array was null.
     */
    @SafeVarargs
    public static <E> UnmodifiableArrayList<E> wrap(final E... array) {
        return (array != null) ? new UnmodifiableArrayList<>(array) : null;
    }

    /**
     * Creates a new instance wrapping a subregion of the given array. A direct reference to the
     * given array is retained (i.e. the array is <strong>not</strong> cloned). Consequently the
     * specified sub-region of the given array shall not be modified after construction if the
     * returned list is intended to be immutable.
     *
     * @param  <E>   The type of elements in the list.
     * @param  array The array to wrap.
     * @param  lower Low endpoint (inclusive) of the sublist.
     * @param  upper High endpoint (exclusive) of the sublist.
     * @return The given array wrapped in an unmodifiable list.
     * @throws IndexOutOfBoundsException If the lower or upper value are out of bounds.
     */
    public static <E> UnmodifiableArrayList<E> wrap(final E[] array, final int lower, final int upper)
            throws IndexOutOfBoundsException
    {
        ArgumentChecks.ensureNonNull("array", array);
        ArgumentChecks.ensureValidIndexRange(array.length, lower, upper);
        if (lower == 0 && upper == array.length) {
            return new UnmodifiableArrayList<>(array);
        }
        return new UnmodifiableArrayList.SubList<>(array, lower, upper - lower);
    }

    /**
     * Returns the element type of the wrapped array. The default implementation returns
     * <code>array.getClass().{@linkplain Class#getComponentType() getComponentType()}</code>.
     *
     * @return The type of elements in the list.
     */
    @Override
    @SuppressWarnings("unchecked") // Safe if this instance was created safely with wrap(E[]).
    public Class<E> getElementType() {
        return (Class<E>) array.getClass().getComponentType();
    }

    /**
     * Returns the index of the first valid element.
     * To be overridden by {@link SubList} only.
     */
    int lower() {
        return 0;
    }

    /**
     * Returns the list size.
     */
    @Override
    public int size() {
        return array.length;
    }

    /**
     * Returns the element at the specified index.
     */
    @Override
    public E get(final int index) {
        return array[index];
    }

    /**
     * Returns the index in this list of the first occurrence of the specified element,
     * or -1 if the list does not contain the element.
     *
     * @param object The element to search for.
     * @return The index of the first occurrence of the given object, or {@code -1}.
     */
    @Override
    public int indexOf(final Object object) {
        final int lower = lower();
        final int upper = lower + size();
        if (object == null) {
            for (int i=lower; i<upper; i++) {
                if (array[i] == null) {
                    return i - lower;
                }
            }
        } else {
            for (int i=lower; i<upper; i++) {
                if (object.equals(array[i])) {
                    return i - lower;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the index in this list of the last occurrence of the specified element,
     * or -1 if the list does not contain the element.
     *
     * @param object The element to search for.
     * @return The index of the last occurrence of the given object, or {@code -1}.
     */
    @Override
    public int lastIndexOf(final Object object) {
        final int lower = lower();
        int i = lower + size();
        if (object == null) {
            while (--i >= lower) {
                if (array[i] == null) {
                    break;
                }
            }
        } else {
            while (--i >= lower) {
                if (object.equals(array[i])) {
                    break;
                }
            }
        }
        return i - lower;
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     *
     * @param object The element to check for existence.
     * @return {@code true} if this collection contains the given element.
     */
    @Override
    public boolean contains(final Object object) {
        final int lower = lower();
        int i = lower + size();
        if (object == null) {
            while (--i >= lower) {
                if (array[i] == null) {
                    return true;
                }
            }
        } else {
            while (--i >= lower) {
                if (object.equals(array[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a view of the portion of this list between the specified
     * {@code lower}, inclusive, and {@code upper}, exclusive.
     *
     * @param  lower Low endpoint (inclusive) of the sublist.
     * @param  upper High endpoint (exclusive) of the sublist.
     * @return A view of the specified range within this list.
     * @throws IndexOutOfBoundsException If the lower or upper value are out of bounds.
     *
     * @see #wrap(E[], int, int)
     */
    @Override
    public UnmodifiableArrayList<E> subList(final int lower, final int upper)
            throws IndexOutOfBoundsException
    {
        ArgumentChecks.ensureValidIndexRange(size(), lower, upper);
        return new SubList<>(array, lower + lower(), upper - lower);
    }

    /**
     * A view over a portion of {@link UnmodifiableArrayList}.
     *
     * @param <E> The type of elements in the list.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3 (derived from geotk-3.00)
     * @version 0.3
     * @module
     */
    private static final class SubList<E> extends UnmodifiableArrayList<E> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -6297280390649627532L;

        /**
         * Index of the first element and size of this list.
         */
        private final int lower, size;

        /**
         * Creates a new sublist.
         */
        SubList(final E[] array, final int lower, final int size) {
            super(array);
            this.lower = lower;
            this.size  = size;
        }

        /**
         * Returns the index of the first element.
         */
        @Override
        int lower() {
            return lower;
        }

        /**
         * Returns the size of this list.
         */
        @Override
        public int size() {
            return size;
        }

        /**
         * Returns the element at the given index.
         */
        @Override
        public E get(final int index) {
            ArgumentChecks.ensureValidIndex(size, index);
            return super.get(index + lower);
        }
    }

    /**
     * Compares this list with the given object for equality.
     *
     * @param  object The object to compare with this list.
     * @return {@code true} if the given object is equal to this list.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != this) {
            if (!(object instanceof UnmodifiableArrayList<?>)) {
                return super.equals(object);
            }
            final UnmodifiableArrayList<?> that = (UnmodifiableArrayList<?>) object;
            int size  = this.size();
            if (size != that.size()) {
                return false;
            }
            int i = this.lower();
            int j = that.lower();
            while (--size >= 0) {
                if (!Objects.equals(this.array[i++], that.array[j++])) {
                    return false;
                }
            }
        }
        return true;
    }
}
