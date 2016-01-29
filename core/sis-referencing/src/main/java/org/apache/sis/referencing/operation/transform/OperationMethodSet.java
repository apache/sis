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
package org.apache.sis.referencing.operation.transform;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.util.SetOfUnknownSize;
import org.apache.sis.referencing.operation.DefaultOperationMethod;


/**
 * An immutable and thread-safe set containing the operation methods given by an {@link Iterable}.
 * Initial iteration is synchronized on the given {@code Iterable} and the result is cached.
 *
 * <div class="section">Rational</div>
 * We use this class instead than copying the {@link OperationMethod} instances in a {@link java.util.HashSet}
 * in order to allow deferred {@code OperationMethod} instantiation, for example in the usual case where the
 * iterable is a {@link java.util.ServiceLoader}: we do not invoke {@link Iterator#next()} before needed.
 *
 * <div class="section">Limitations</div>
 * The usual {@link Set} methods like {@code contains(Object)} are inefficient as they may require a traversal
 * of all elements in this set.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
final class OperationMethodSet extends SetOfUnknownSize<OperationMethod> {
    /**
     * The operation type we are looking for.
     */
    private final Class<? extends SingleOperation> type;

    /**
     * The {@link DefaultMathTransformFactory#methods} used for fetching the initial methods.
     * We need this reference for locking purpose.
     */
    private final Iterable<? extends OperationMethod> methods;

    /**
     * Iterator over {@link #methods} elements. All usage of this iterator must be synchronized on {@link #methods}.
     * Will be set to {@code null} when the iteration is over.
     */
    private Iterator<? extends OperationMethod> methodIterator;

    /**
     * The methods returned by the first iteration.
     */
    private final List<OperationMethod> cachedMethods;

    /**
     * Constructs a set wrapping the given iterable.
     * The caller musts holds the lock on {@code methods} when invoking this constructor.
     *
     * @param type The type of coordinate operation for which to retain methods.
     * @param methods The {@link DefaultMathTransformFactory#methods} used for fetching the initial methods.
     */
    OperationMethodSet(final Class<? extends SingleOperation> type,
            final Iterable<? extends OperationMethod> methods)
    {
        this.type = type;
        this.methods = methods;
        cachedMethods = new ArrayList<OperationMethod>();
        reset();
    }

    /**
     * Invoked on construction time, or when the service loader has been reloaded.
     * The caller musts holds the lock on {@code methods} when invoking this method.
     */
    final synchronized void reset() {
        assert Thread.holdsLock(methods);
        cachedMethods.clear();
        methodIterator = methods.iterator();
        if (!methodIterator.hasNext()) {
            methodIterator = null;
        }
    }

    /**
     * Transfers the next element from {@link #methodIterator} to {@link #cachedMethods}.
     *
     * @return {@code true} if the transfer has been done, or {@code false} if the next
     *         method has been skipped because its operation type is not the expected one
     *         or because the element has already been added in a previous transfer.
     */
    private boolean transfer() {
        assert Thread.holdsLock(this);
        final OperationMethod method;
        synchronized (methods) {
            method = methodIterator.next();
            if (!methodIterator.hasNext()) {
                methodIterator = null;
            }
        }
        // Maintenance note: following check shall be consistent with the one in 'contains(Object)'.
        if (method instanceof DefaultOperationMethod) {
            final Class<? extends SingleOperation> c = ((DefaultOperationMethod) method).getOperationType();
            if (c != null && !type.isAssignableFrom(c)) {
                return false;
            }
        }
        /*
         * ServiceLoader guarantees that the iteration does not contain duplicated elements.
         * The Set contract provides similar guarantee. For other types (which should be very
         * uncommon), we check for duplicated elements as a safety.
         *
         * Note that in the vast majority of cases, 'methods' is an instance of ServiceLoader
         * and its "instanceof" check should be very fast since ServiceLoader is a final class.
         */
        if (!(methods instanceof ServiceLoader || methods instanceof Set<?>)) {
            if (cachedMethods.contains(method)) {
                return false;
            }
        }
        return cachedMethods.add(method);
    }

    /**
     * Returns {@code true} if this set is empty.
     */
    @Override
    public synchronized boolean isEmpty() {
        if (!cachedMethods.isEmpty()) {
            return false;
        }
        while (methodIterator != null) {
            if (transfer()) return false;
        }
        return true;
    }

    /**
     * Returns the number of elements in this set.
     */
    @Override
    public synchronized int size() {
        while (methodIterator != null) {
            transfer();
        }
        return cachedMethods.size();
    }

    /**
     * Returns {@code true} if the {@link #size()} method is cheap.
     */
    @Override
    protected synchronized boolean isSizeKnown() {
        return methodIterator == null;
    }

    /**
     * Returns {@code true} if {@link #next(int)} can return an operation method at the given index.
     */
    final synchronized boolean hasNext(final int index) {
        if (index >= cachedMethods.size()) {
            do if (methodIterator == null) {
                return false;
            } while (!transfer());
        }
        return true;
    }

    /**
     * Returns the operation method at the given index. In case of index out of bounds, this method throws a
     * {@link NoSuchElementException} instead than an {@link IndexOutOfBoundsException} because this method
     * is designed for being invoked by {@link Iter#next()}.
     */
    final synchronized OperationMethod next(final int index) {
        if (index >= cachedMethods.size()) {
            do if (methodIterator == null) {
                throw new NoSuchElementException();
            } while (!transfer());
        }
        return cachedMethods.get(index);
    }

    /**
     * Returns an iterator over the elements contained in this set.
     */
    @Override
    public Iterator<OperationMethod> iterator() {
        return new Iterator<OperationMethod>() {
            /** Index of the next element to be returned. */
            private int cursor;

            @Override
            public boolean hasNext() {
                return OperationMethodSet.this.hasNext(cursor);
            }

            @Override
            public OperationMethod next() {
                return OperationMethodSet.this.next(cursor++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns {@code true} if this set contains the given object.
     * This implementation overrides the default one with a quick check allowing us to filter
     * {@code OperationMethod} instances of the wrong type before to iterate over the elements.
     */
    @Override
    public boolean contains(final Object object) {
        // Maintenance note: following check shall be consistent with the one in 'transfer()'.
        if (object instanceof DefaultOperationMethod) {
            final Class<? extends SingleOperation> c = ((DefaultOperationMethod) object).getOperationType();
            if (c != null && !type.isAssignableFrom(c)) {
                return false;
            }
        } else if (!(object instanceof OperationMethod)) {
            return false;
        }
        /*
         * NOTE: we could optimize this method here with the following code, which would be
         * much more efficient than the default implementation if 'methods' is a HashSet:
         *
         *   if (methods instanceof Collection<?>) {
         *       synchronized (methods) {
         *           return ((Collection<?>) methods).contains(object);
         *       }
         *   }
         *
         * However we don't do that because it would bring 2 issues:
         *
         *   1) There is no guarantee that implementation of the 'methods' collection uses the 'equals(Object)'
         *      method. For example TreeSet rather uses 'compareTo(Object)'. Since the OperationMethodSet class
         *      uses 'equals', there is a risk of inconsistency.
         *
         *   2) The 'synchronized (methods)' statement introduces a risk of deadlock if some implementations of
         *      'OperationMethod.equals(Object)' invokes DefaultMathTransformFactory methods. Of course no such
         *      callbacks should exist (and Apache SIS don't do that), but the OperationMethods can be supplied
         *      by users and Murphy's law said that if something can go wrong, it will go wrong.
         *
         * Since there is no evidence at this time that we need an efficient OperationMethodSet.contains(Object)
         * implementation, we keep for now the slowest but more conservative approach inherited from AbstractSet.
         * However this choice may be revisited in any future SIS version if necessary.
         */
        return super.contains(object);
    }
}
