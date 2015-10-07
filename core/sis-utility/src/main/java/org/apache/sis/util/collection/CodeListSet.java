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

import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import org.opengis.util.CodeList;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.internal.util.CheckedArrayList;


/**
 * A specialized {@code Set} implementation for use with {@link CodeList} values.
 * All elements in a {@code CodeListSet} are of the same {@code CodeList} class,
 * which must be final. Iterators traverse the elements in the order in which the
 * code list constants are declared.
 *
 * <div class="section">Implementation note</div>
 * {@code CodeListSet} is implemented internally by bit vectors for compact and efficient storage.
 * All bulk operations ({@code addAll}, {@code removeAll}, {@code containsAll}) are very quick if
 * their argument is also a {@code CodeListSet} instance.
 *
 * <div class="section">Usage example</div>
 * The following example creates a set of {@link org.opengis.referencing.cs.AxisDirection}s
 * for a (<var>x</var>,<var>y</var>,<var>z</var>) coordinate system:
 *
 * {@preformat java
 *   CodeListSet<AxisDirection> codes = new CodeListSet<>(AxisDirection.class);
 *   Collections.addAll(codes, AxisDirection.EAST, AxisDirection.NORTH, AxisDirection.UP),
 * }
 *
 * @param <E> The type of code list elements in the set.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see java.util.EnumSet
 */
public class CodeListSet<E extends CodeList<E>> extends AbstractSet<E>
        implements CheckedContainer<E>, Cloneable, Serializable
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6328082298556260980L;

    /**
     * A pool of code list arrays. When many {@code CodeListSet} instances are for the
     * same code list type, this allows those instances to share the same arrays.
     */
    @SuppressWarnings("rawtypes")
    private static final WeakHashSet<CodeList[]> POOL = new WeakHashSet<CodeList[]>(CodeList[].class);

    /**
     * The type of code list elements.
     *
     * @see #getElementType()
     */
    private final Class<E> elementType;

    /**
     * A bitmask of code list values present in this map.
     */
    private long values;

    /**
     * The bit set for supplementary values beyond the {@code values} mask, or {@code null}
     * if none. This is very rarely needed, but we need this field in case a code list has
     * more than 64 elements.
     *
     * <div class="note"><b>Implementation note:</b>
     * The standard {@link java.util.EnumSet} class uses different implementations depending on whether
     * the enumeration contains more or less than 64 elements. We can not apply the same strategy for
     * {@code CodeListSet}, because new code list elements can be created at runtime. Consequently this
     * implementation needs to be able to growth its capacity.</div>
     */
    private BitSet supplementary;

    /**
     * All possible code list elements, fetched when first needed.
     * Note that this array may need to be fetched more than once,
     * because code list elements can be dynamically added.
     *
     * @see #valueOf(int)
     */
    private transient E[] codes;

    /**
     * Creates an initially empty set for code lists of the given type.
     * The given {@code CodeList} type shall be final.
     *
     * @param  elementType The type of code list elements to be included in this set.
     * @throws IllegalArgumentException If the given class is not final.
     */
    public CodeListSet(final Class<E> elementType) throws IllegalArgumentException {
        if (!Modifier.isFinal(elementType.getModifiers())) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ClassNotFinal_1, elementType));
        }
        this.elementType = elementType;
    }

    /**
     * Creates set for code lists of the given type. If the {@code fill} argument is {@code false},
     * then the new set will be initially empty. Otherwise the new set will be filled with all code
     * list elements of the given type that are known at construction time. Note that if new code
     * list elements are created after the invocation of this {@code CodeListSet} constructor, then
     * those new elements will <em>not</em> be in this set.
     *
     * @param  elementType The type of code list elements to be included in this set.
     * @param  fill {@code true} for filling the set with all known elements of the given type,
     *         or {@code false} for leaving the set empty.
     * @throws IllegalArgumentException If the given class is not final.
     */
    public CodeListSet(final Class<E> elementType, final boolean fill) throws IllegalArgumentException {
        this(elementType);
        if (fill) {
            codes = POOL.unique(Types.getCodeValues(elementType));
            int n = codes.length;
            if (n < Long.SIZE) {
                values = (1L << n) - 1;
            } else {
                values = -1;
                if ((n -= Long.SIZE) != 0) {
                    supplementary = new BitSet(n);
                    supplementary.set(0, n);
                }
            }
        }
    }

    /**
     * Returns the type of code list elements in this set.
     *
     * @return The type of code list elements in this set.
     */
    @Override
    public Class<E> getElementType() {
        return elementType;
    }

    /**
     * Returns the code list for the given ordinal value. This methods depends
     * only on the code list type; it does not depend on the content of this set.
     */
    final E valueOf(final int ordinal) {
        E[] array = codes;
        if (array == null || ordinal >= array.length) {
            codes = array = POOL.unique(Types.getCodeValues(elementType));
        }
        return array[ordinal];
    }

    /**
     * Removes all elements from this set.
     */
    @Override
    public void clear() {
        values = 0;
        final BitSet s = supplementary;
        if (s != null) {
            s.clear();
        }
    }

    /**
     * Returns {@code true} if this set does not contains any element.
     *
     * @return {@code true} if this set is empty.
     */
    @Override
    public boolean isEmpty() {
        final BitSet s;
        return values == 0 && ((s = supplementary) == null || s.isEmpty());
    }

    /**
     * Returns the number of elements in this set.
     *
     * @return The number of elements in this set.
     */
    @Override
    public int size() {
        int n = Long.bitCount(values);
        final BitSet s = supplementary;
        if (s != null) {
            n += s.cardinality();
        }
        return n;
    }

    /**
     * Adds the specified code list element in this set.
     *
     * @param  element The code list element to add in this set.
     * @return {@code true} if this set has been modified as a consequence of this method call.
     */
    @Override
    public boolean add(final E element) {
        if (element == null) {
            final String message = CheckedArrayList.illegalElement(this, element, elementType);
            if (message == null) {
                /*
                 * If a unmarshalling process is under way, silently discard null element.
                 * This case happen when a codeListValue attribute in a XML file is empty.
                 * See https://issues.apache.org/jira/browse/SIS-157
                 */
                return false;
            }
            throw new NullArgumentException(message);
        }
        int ordinal = element.ordinal();
        if (ordinal < Long.SIZE) {
            return values != (values |= (1L << ordinal));
        }
        /*
         * The above code is suffisient in the vast majority of cases. In the rare cases where
         * there is more than 64 elements, create a BitSet for storing the supplementary values.
         */
        BitSet s = supplementary;
        if (s == null) {
            supplementary = s = new BitSet();
        }
        if (s.get(ordinal -= Long.SIZE)) {
            return false;
        }
        s.set(ordinal);
        return true;
    }

    /**
     * Removes the specified code list element from this set.
     * This methods does nothing if the given argument is {@code null} or is
     * not an instance of the code list class specified at construction time.
     *
     * @param  object The code list element to remove from this set.
     * @return {@code true} if this set has been modified as a consequence of this method call.
     */
    @Override
    public boolean remove(final Object object) {
        if (elementType.isInstance(object)) {
            return clear(((CodeList<?>) object).ordinal());
        }
        return false;
    }

    /**
     * Clears the bit at the given ordinal value. This method is invoked by
     * {@link #remove(Object)} or by {@link Iter#remove()}.
     */
    final boolean clear(int ordinal) {
        if (ordinal < Long.SIZE) {
            return values != (values &= ~(1L << ordinal));
        }
        // Rare cases where there is more than 64 elements.
        final BitSet s = supplementary;
        if (s != null && s.get(ordinal -= Long.SIZE)) {
            s.clear(ordinal);
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this set contains the given element.
     * This methods returns {@code false} if the given argument is {@code null} or
     * is not an instance of the code list class specified at construction time.
     *
     * @param  object The element to test for presence in this set.
     * @return {@code true} if the given object is contained in this set.
     */
    @Override
    public boolean contains(final Object object) {
        if (elementType.isInstance(object)) {
            int ordinal = ((CodeList<?>) object).ordinal();
            if (ordinal < Long.SIZE) {
                return (values & (1L << ordinal)) != 0;
            }
            // Rare cases where there is more than 64 elements.
            final BitSet s = supplementary;
            if (s != null) {
                return s.get(ordinal - Long.SIZE);
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this set contains all the elements of the given collection.
     *
     * @param  c The collection to be checked for containment in this set.
     * @return {@code true} if this set contains all elements of the given collection.
     */
    @Override
    public boolean containsAll(final Collection<?> c) {
        if (c instanceof CodeListSet) {
            final CodeListSet<?> o = (CodeListSet<?>) c;
            if (elementType == o.elementType) {
                if (values == (values | o.values)) {
                    /*
                     * Code below this point checks for the rare cases
                     * where there is more than 64 code list elements.
                     */
                    final BitSet s = supplementary;
                    final BitSet os = o.supplementary;
                    if (( s == null ||  s.isEmpty()) &&
                        (os == null || os.isEmpty()))
                    {
                        return true;
                    }
                    if (s != null && os != null) {
                        final BitSet tmp = (BitSet) os.clone();
                        tmp.andNot(s);
                        return tmp.isEmpty();
                    }
                }
            }
            return false;
        }
        return super.containsAll(c);
    }

    /**
     * Adds all elements of the given collection to this set.
     *
     * @param  c The collection containing elements to be added to this set.
     * @return {@code true} if this set changed as a result of this method call.
     */
    @Override
    public boolean addAll(final Collection<? extends E> c) throws IllegalArgumentException {
        if (c instanceof CodeListSet) {
            final CodeListSet<?> o = (CodeListSet<?>) c;
            // Following assertion should be ensured by parameterized types.
            assert elementType.isAssignableFrom(o.elementType);
            boolean changed = (values != (values |= o.values));
            /*
             * Code below this point is for the rare cases
             * where there is more than 64 code list elements.
             */
            final BitSet os = o.supplementary;
            if (os != null) {
                final BitSet s = supplementary;
                if (s == null) {
                    if (!os.isEmpty()) {
                        supplementary = (BitSet) os.clone();
                        changed = true;
                    }
                } else if (changed) {
                    // Avoid the cost of computing cardinality.
                    s.or(os);
                } else {
                    final int cardinality = s.cardinality();
                    s.or(os);
                    changed = (cardinality != s.cardinality());
                }
            }
            return changed;
        }
        return super.addAll(c);
    }

    /**
     * Returns the bitmask to use for a bulk operation with an other set of code lists.
     */
    private long mask(final CodeListSet<?> other) {
        return (elementType == other.elementType) ? other.values : 0;
    }

    /**
     * Adds all elements of the given collection from this set.
     *
     * @param  c The collection containing elements to be removed from this set.
     * @return {@code true} if this set changed as a result of this method call.
     */
    @Override
    public boolean removeAll(final Collection<?> c) {
        if (c instanceof CodeListSet) {
            boolean changed = (values != (values &= ~mask((CodeListSet<?>) c)));
            /*
             * Code below this point is for the rare cases
             * where there is more than 64 code list elements.
             */
            final BitSet s = supplementary;
            if (s != null) {
                final BitSet os = ((CodeListSet<?>) c).supplementary;
                if (os != null) {
                    if (changed) {
                        // Avoid the cost of computing cardinality.
                        s.andNot(os);
                    } else {
                        final int cardinality = s.cardinality();
                        s.andNot(os);
                        changed = (cardinality != s.cardinality());
                    }
                }
            }
            return changed;
        }
        return super.removeAll(c);
    }

    /**
     * Retains only the elements of the given collection in this set.
     *
     * @param  c The collection containing elements to retain in this set.
     * @return {@code true} if this set changed as a result of this method call.
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        if (c instanceof CodeListSet) {
            boolean changed = (values != (values &= mask((CodeListSet<?>) c)));
            /*
             * Code below this point is for the rare cases
             * where there is more than 64 code list elements.
             */
            final BitSet s = supplementary;
            if (s != null) {
                final BitSet os = ((CodeListSet<?>) c).supplementary;
                if (os == null) {
                    changed |= !s.isEmpty();
                    s.clear();
                } else if (changed) {
                    // Avoid the cost of computing cardinality.
                    s.and(os);
                } else {
                    final int cardinality = s.cardinality();
                    s.and(os);
                    changed = (cardinality != s.cardinality());
                }
            }
            return changed;
        }
        return super.retainAll(c);
    }

    /**
     * Returns an iterator over the elements in this set.
     * The instance returned by this implementation will iterate over a snapshot of this
     * {@code CodeListSet} content at the time this method has been invoked. Changes in
     * this {@code CodeListSet} made after this method call will not affect the values
     * returned by the iterator.
     *
     * @return An iterator over the elements in this set.
     */
    @Override
    public Iterator<E> iterator() {
        return new Iter(values, supplementary);
    }

    /**
     * The iterator returned by {@link CodeListSet#iterator()}.
     */
    private final class Iter implements Iterator<E> {
        /**
         * Initialized to {@link CodeListSet#values}, then the bits are cleared as we
         * progress in the iteration. This value become 0 when the iteration is done.
         */
        private long remaining;

        /**
         * Initialized to a clone of {@link CodeListSet#supplementary}, then the bits are cleared
         * as we progress in the iteration. The bit set become empty when the iteration is done.
         */
        private final BitSet more;

        /**
         * Ordinal value of the last element returned by {@link #next()}, or -1 if none.
         */
        private int last;

        /**
         * Creates a new iterator initialized to the given values.
         */
        Iter(final long values, final BitSet supplementary) {
            remaining = values;
            more = (supplementary != null) ? (BitSet) supplementary.clone() : null;
            last = -1;
        }

        /**
         * Returns {@code true} if there is more elements to return.
         */
        @Override
        public boolean hasNext() {
            return remaining != 0 || (more != null && !more.isEmpty());
        }

        /**
         * Returns the next element.
         */
        @Override
        public E next() {
            int ordinal = Long.numberOfTrailingZeros(remaining);
            if (ordinal >= Long.SIZE) {
                // Rare case when we have more than 64 elements.
                if (more == null || (ordinal = more.nextSetBit(0)) < 0) {
                    throw new NoSuchElementException();
                }
                more.clear(ordinal);
                ordinal += Long.SIZE;
            }
            last = ordinal;
            remaining &= ~(1L << ordinal);
            return valueOf(ordinal);
        }

        /**
         * Removes the last element returned by this iterator.
         */
        @Override
        public void remove() {
            if (last < 0) {
                throw new IllegalStateException();
            }
            clear(last);
            last = -1;
        }
    }

    /**
     * Returns a new set of the same class containing the same elements than this set.
     *
     * @return A clone of this set.
     */
    @Override
    @SuppressWarnings("unchecked")
    public CodeListSet<E> clone() {
        final CodeListSet<E> clone;
        try {
            clone = (CodeListSet<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e); // Should never happen, since we are cloneable.
        }
        final BitSet s = supplementary;
        if (s != null) {
            clone.supplementary = (BitSet) s.clone();
        }
        return clone;
    }
}
