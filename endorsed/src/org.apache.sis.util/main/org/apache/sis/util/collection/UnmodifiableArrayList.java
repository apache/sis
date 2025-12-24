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
import java.util.Arrays;
import java.util.Objects;
import java.util.RandomAccess;
import java.lang.reflect.Array;


/**
 * An unmodifiable view of an array. Invoking
 *
 * {@snippet lang="java" :
 *     List<?> list = Containers.viewAsUnmodifiableList(array);
 *     }
 *
 * is equivalent to
 *
 * {@snippet lang="java" :
 *     List<?> list = Collections.unmodifiableList(Arrays.asList(array));
 *     }
 *
 * except that this class uses one less level of indirection (which may be significant because
 * unmodifiable lists are extensively used in SIS) and implements the {@link CheckedContainer}
 * interface.
 *
 * @author  Martin Desruisseaux (IRD)
 *
 * @param <E>  the type of elements in the list.
 */
@SuppressWarnings("EqualsAndHashcode")
class UnmodifiableArrayList<E> extends AbstractList<E> implements RandomAccess, CheckedContainer<E>, Serializable {
    /**
     * For compatibility with different versions.
     */
    private static final long serialVersionUID = 13882164775184042L;

    /**
     * The wrapped array.
     */
    @SuppressWarnings("serial")         // Not statically typed as Serializable.
    protected final E[] array;

    /**
     * Creates a new instance wrapping the given array. A direct reference to the given array is
     * retained (i.e. the array is <strong>not</strong> cloned). Consequently, the given array
     * shall not be modified after construction if this list is intended to be immutable.
     *
     * <p>This constructor is for sub-classing only. Users should invoke the {@link #wrap(Object[])}
     * static method instead.</p>
     *
     * <p>The argument type is intentionally {@code E[]} instead of {@code E...} in order to force
     * the caller to instantiate the array explicitly, in order to make sure that the array type is
     * the intended one.</p>
     *
     * @param array the array to wrap.
     */
    protected UnmodifiableArrayList(final E[] array) {      // NOT "E..." - see javadoc.
        this.array = Objects.requireNonNull(array);
    }

    /**
     * Returns the element type of the wrapped array. The default implementation returns
     * <code>array.getClass().{@linkplain Class#getComponentType() getComponentType()}</code>.
     * Because arrays in the Java language are covariant (at the contrary of collections),
     * the returned element type has to be {@code <? extends E>} instead of {@code <E>}.
     *
     * @return the type of elements in the list.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final Class<? extends E> getElementType() {
        return (Class<? extends E>) array.getClass().getComponentType();
    }

    /**
     * Indicates that this collection is unmodifiable.
     *
     * @return {@link Mutability#UNMODIFIABLE}.
     */
    @Override
    public final Mutability getMutability() {
        return Mutability.UNMODIFIABLE;
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
     * @return the size of this list.
     */
    @Override
    public int size() {
        return array.length;
    }

    /**
     * Returns the element at the specified index.
     *
     * @param  index  the index of the element to get.
     * @return the element at the given index.
     */
    @Override
    public E get(final int index) {
        return array[index];
    }

    /**
     * Returns the index in this list of the first occurrence of the specified element,
     * or -1 if the list does not contain the element.
     *
     * @param  object  the element to search for.
     * @return the index of the first occurrence of the given object, or {@code -1}.
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
     * @param  object  the element to search for.
     * @return the index of the last occurrence of the given object, or {@code -1}.
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
     * @param  object  the element to check for existence.
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
     * @param  lower  low endpoint (inclusive) of the sublist.
     * @param  upper  high endpoint (exclusive) of the sublist.
     * @return a view of the specified range within this list.
     * @throws IndexOutOfBoundsException if the lower or upper value are out of bounds.
     *
     * @see #wrap(Object[], int, int)
     */
    @Override
    public UnmodifiableArrayList<E> subList(final int lower, final int upper)
            throws IndexOutOfBoundsException
    {
        Objects.checkFromToIndex(lower, upper, size());
        return new SubList<>(array, lower + lower(), upper - lower);
    }

    /**
     * A view over a portion of {@link UnmodifiableArrayList}.
     *
     * @param <E>  the type of elements in the list.
     */
    static final class SubList<E> extends UnmodifiableArrayList<E> {
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
            return super.get(Objects.checkIndex(index, size) + lower);
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
     * @return a copy of the wrapped array.
     */
    @Override
    public E[] toArray() {
        return array.clone();
    }

    /**
     * Copies the backing array in the given one if the list fits in the given array.
     * If the list does not fit in the given array, returns the collection in a new array.
     *
     * @param  <T>   the type of array element.
     * @param  dest  the array where to copy the elements if the list can fits in the array.
     * @return the given array, or a newly created array if this list is larger than the given array.
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
                 * will throw an ArrayStoreException, which is the exception type required by
                 * the Collection.toArray(T[]) javadoc.
                 */
                dest = (T[]) Array.newInstance(dest.getClass().getComponentType(), size);
            } else {
                dest[size] = null;              // Required by Collection.toArray(T[]) javadoc.
            }
        }
        System.arraycopy(array, lower(), dest, 0, size);
        return dest;
    }

    /**
     * Compares this list with the given object for equality.
     *
     * @param  object  the object to compare with this list.
     * @return {@code true} if the given object is equal to this list.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != this) {
            if (!(object instanceof UnmodifiableArrayList<?>)) {
                return super.equals(object);
            }
            final var that = (UnmodifiableArrayList<?>) object;
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
