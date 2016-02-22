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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.lang.reflect.Array;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.CheckedContainer;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


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
 * except that this class uses one less level of indirection (which may be significant since
 * unmodifiable lists are extensively used in SIS) and implements the {@link CheckedContainer}
 * interface.
 *
 * <div class="section">WARNING! Type safety hole</div>
 * The {@link #getElementType()} return type is {@code Class<E>}, but its implementation actually
 * returns {@code Class<? extends E>}. This contract violation is possible because Java arrays are
 * covariant (at the contrary of collections). In order to avoid such contract violation, callers
 * <strong>must</strong> ensure that the type of array elements in exactly {@code E}, not a subtype
 * of {@code E}. This class has no way to verify that condition. This class is not in the public API
 * for this reason.
 *
 * <p>Note that the public API, {@link org.apache.sis.util.collection.Containers#unmodifiableList(Object[])},
 * returns {@code List<? extends E>}, which is okay.</p>
 *
 * @param <E> The type of elements in the list.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class UnmodifiableArrayList<E> extends AbstractList<E> implements CheckedContainer<E>, Serializable {
    /**
     * For compatibility with different versions.
     */
    private static final long serialVersionUID = 13882164775184042L;

    /**
     * The wrapped array.
     */
    final E[] array;

    /**
     * Creates a new instance wrapping the given array. A direct reference to the given array is
     * retained (i.e. the array is <strong>not</strong> cloned). Consequently the given array
     * shall not be modified after construction if this list is intended to be immutable.
     *
     * <p>This constructor is for sub-classing only. Users should invoke the {@link #wrap(Object[])}
     * static method instead.</p>
     *
     * <p>The argument type is intentionally {@code E[]} instead than {@code E...} in order to force
     * the caller to instantiate the array explicitely, in order to make sure that the array type is
     * the intended one.</p>
     *
     * <div class="section">WARNING! Type safety hole</div>
     * Callers <strong>must</strong> ensure that the type of array elements in exactly {@code E},
     * not a subtype of {@code E}. See class javadoc for more information.
     *
     * @param array The array to wrap.
     */
    protected UnmodifiableArrayList(final E[] array) {                          // NOT "E..." - see javadoc.
        this.array = Objects.requireNonNull(array);
    }

    /**
     * Creates a new instance wrapping the given array. A direct reference to the given array is
     * retained (i.e. the array is <strong>not</strong> cloned). Consequently the given array
     * shall not be modified after construction if the returned list is intended to be immutable.
     *
     * <div class="section">WARNING! Type safety hole</div>
     * Callers <strong>must</strong> ensure that the type of array elements in exactly {@code E},
     * not a subtype of {@code E}. If the caller is okay with {@code List<? extends E>}, then (s)he
     * should use {@link org.apache.sis.util.collection.Containers#unmodifiableList(Object[])} instead.
     * See class javadoc for more information.
     *
     * <p>The argument type is intentionally {@code E[]} instead than {@code E...} in order to force
     * the caller to instantiate the array explicitely, in order to make sure that the array type is
     * the intended one.</p>
     *
     * @param  <E> The type of elements in the list.
     * @param  array The array to wrap, or {@code null} if none.
     * @return The given array wrapped in an unmodifiable list, or {@code null} if the given
     *         array was null.
     */
    public static <E> UnmodifiableArrayList<E> wrap(final E[] array) {          // NOT "E..." - see javadoc.
        return (array != null) ? new UnmodifiableArrayList<E>(array) : null;
    }

    /**
     * Creates a new instance wrapping a subregion of the given array. A direct reference to the
     * given array is retained (i.e. the array is <strong>not</strong> cloned). Consequently the
     * specified sub-region of the given array shall not be modified after construction if the
     * returned list is intended to be immutable.
     *
     * <p>This method does not check the validity of the given index.
     * The check must be done by the caller.</p>
     *
     * <div class="section">WARNING! Type safety hole</div>
     * Callers <strong>must</strong> ensure that the type of array elements in exactly {@code E},
     * not a subtype of {@code E}. If the caller is okay with {@code List<? extends E>}, then (s)he
     * should use {@link org.apache.sis.util.collection.Containers#unmodifiableList(Object[])} instead.
     * See class javadoc for more information.
     *
     * @param  <E>   The type of elements in the list.
     * @param  array The array to wrap.
     * @param  lower Low endpoint (inclusive) of the sublist.
     * @param  upper High endpoint (exclusive) of the sublist.
     * @return The given array wrapped in an unmodifiable list.
     */
    public static <E> UnmodifiableArrayList<E> wrap(final E[] array, final int lower, final int upper) {
        if (lower == 0 && upper == array.length) {
            return new UnmodifiableArrayList<E>(array);
        }
        return new UnmodifiableArrayList.SubList<E>(array, lower, upper - lower);
    }

    /**
     * Returns the element type of the wrapped array. The default implementation returns
     * <code>array.getClass().{@linkplain Class#getComponentType() getComponentType()}</code>.
     *
     * @return The type of elements in the list.
     */
    @Override
    public Class<E> getElementType() {
        // No @SuppressWarnings because this cast is really unsafe. See class javadoc.
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
     *
     * @return The size of this list.
     */
    @Override
    public int size() {
        return array.length;
    }

    /**
     * Returns the size of the array backing this list. This is the length of the array
     * given to the constructor. It is equal to {@link #size()} except if this instance
     * is a {@linkplain #subList(int,int) sublist}, in which case the value returned by
     * this method is greater than {@code size()}.
     *
     * <p>This method can be used as a hint for choosing a {@code UnmodifiableArrayList}
     * instance to keep when there is a choice between many equal instances. Note that a
     * greater value is not necessarily more memory consuming, since the backing array
     * may be shared by many sublists.</p>
     *
     * @return The length of the backing array.
     */
    public final int arraySize() {
        return array.length;
    }

    /**
     * Returns the element at the specified index.
     *
     * @param  index The index of the element to get.
     * @return The element at the given index.
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
     * @see #wrap(Object[], int, int)
     */
    @Override
    public UnmodifiableArrayList<E> subList(final int lower, final int upper)
            throws IndexOutOfBoundsException
    {
        ArgumentChecks.ensureValidIndexRange(size(), lower, upper);
        return new SubList<E>(array, lower + lower(), upper - lower);
    }

    /**
     * A view over a portion of {@link UnmodifiableArrayList}.
     *
     * @param <E> The type of elements in the list.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    private static final class SubList<E> extends UnmodifiableArrayList<E> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 33065197642139916L;

        /**
         * Index of the first element and size of this list.
         */
        private final int lower, size;

        /**
         * Creates a new sublist.
         *
         * <p><b>WARNING! Type safety hole</b></p>
         * Callers <strong>must</strong> ensure that the type of array elements in exactly {@code E},
         * not a subtype of {@code E}. See {@link UnmodifiableArrayList} class javadoc for more information.
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

        /**
         * Returns a copy of the backing array section viewed by this sublist.
         */
        @Override
        public E[] toArray() {
            return Arrays.copyOfRange(array, lower, lower + size);
        }
    }

    /**
     * Returns a copy of the backing array. Note that the array type is {@code E[]} rather than {@code Object[]}.
     * This is not what {@code ArrayList} does, but is not forbidden by {@link java.util.List#toArray()} javadoc
     * neither.
     *
     * @return A copy of the wrapped array.
     */
    @Override
    public E[] toArray() {
        return array.clone();
    }

    /**
     * Copies the backing array in the given one if the list fits in the given array.
     * If the list does not fit in the given array, returns the collection in a new array.
     *
     * @param  <T>   The type of array element.
     * @param  dest  The array where to copy the elements if the list can fits in the array.
     * @return The given array, or a newly created array if this list is larger than the given array.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] dest) {
        final int size = size();
        if (dest.length != size) {
            if (dest.length < size) {
                /*
                 * We are cheating here since the array component may not be assignable to T.
                 * But if this assumption is wrong, then the call to System.arraycopy(…) later
                 * will thrown an ArrayStoreException, which is the exception type required by
                 * the Collection.toArray(T[]) javadoc.
                 */
                dest = (T[]) Array.newInstance(dest.getClass().getComponentType(), size);
            } else {
                dest[size] = null; // Required by Collection.toArray(T[]) javadoc.
            }
        }
        System.arraycopy(array, lower(), dest, 0, size);
        return dest;
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
