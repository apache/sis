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
package org.apache.sis.internal.metadata;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.apache.sis.metadata.AbstractMetadata;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.util.AbstractIterator;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.ArgumentChecks;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * An adapter for collections of a legacy type replaced by an other collection.
 * This adapter is used for implementation of deprecated methods in the {@link org.apache.sis.metadata.iso}
 * sub-packages, usually when the deprecation is the result of upgrading from an older to a newer ISO standard.
 *
 * @param <L> The legacy type.
 * @param <N> The new type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public abstract class LegacyPropertyAdapter<L,N> extends AbstractCollection<L> {
    /**
     * The collection where to store the elements.
     */
    protected final Collection<N> elements;

    /**
     * For logging warning only once per collection usage.
     */
    private transient boolean warningOccurred;

    /**
     * Creates a new adapter.
     *
     * @param elements The collection where to store the elements (may be {@code null}).
     */
    protected LegacyPropertyAdapter(final Collection<N> elements) {
        this.elements = elements;
    }

    /**
     * Wraps a legacy value in its new type.
     *
     * @param  value The legacy value.
     * @return The new type.
     */
    protected abstract N wrap(final L value);

    /**
     * Extracts a legacy value from the new type.
     *
     * @param  container The new type.
     * @return The legacy value, or {@code null}.
     */
    protected abstract L unwrap(final N container);

    /**
     * Update a new value with the given legacy value.
     *
     * @param  container The new value to be used as a container for the old value.
     * @param  value     The value to update in the container.
     * @return Whether this method has been able to perform the update.
     */
    protected abstract boolean update(final N container, final L value);



    // ┌───────────────────────────────────────────────────────────────────────────────────────────────────────┐
    // │                                  Convenience methods for subclasses                                   │
    // └───────────────────────────────────────────────────────────────────────────────────────────────────────┘



    /**
     * Returns {@code this} if the collection given at construction time was non-null, or {@code null} otherwise.
     * The later case may happen at marshalling time.
     *
     * @return {@code this} or {@code null}.
     */
    public final LegacyPropertyAdapter<L,N> validOrNull() {
        return (elements != null) ? this : null;
    }

    /**
     * Sets the values from the given collection.
     *
     * @param newValues The values to set (may be {@code null}).
     */
    public final void setValues(Collection<? extends L> newValues) {
        if (newValues == null) {
            newValues = Collections.emptySet();
        }
        final Iterator<? extends L> it = newValues.iterator();
        final Iterator<N> toUpdate = elements.iterator();
        boolean hasNext = it.hasNext();
        L next = hasNext ? it.next() : null;
        while (toUpdate.hasNext()) {
            final N container = toUpdate.next();
            if (update(container, next)) {
                hasNext = it.hasNext();
                next = hasNext ? it.next() : null;
                if (isEmpty(container)) {
                    toUpdate.remove();
                }
            }
        }
        if (hasNext) {
            elements.add(wrap(next));
            while (it.hasNext()) {
                elements.add(wrap(it.next()));
            }
        }
    }

    /**
     * Returns the singleton value of the given collection, or {@code null} if the given collection is null or empty.
     * If the given collection contains more than one element, then a warning is emitted.
     *
     * @param  <L>           The kind of legacy values to be returned.
     * @param  values        The collection from which to get the value.
     * @param  valueClass    The value class, used in case of warning only.
     * @param  caller        Either {@code this} or {@code null}.
     * @param  callerClass   The caller class, used in case of warning only.
     * @param  callerMethod  The caller method, used in case of warning only.
     * @return The first value, or {@code null} if none.
     */
    public static <L> L getSingleton(final Collection<? extends L> values, final Class<L> valueClass,
            final LegacyPropertyAdapter<L,?> caller, final Class<?> callerClass, final String callerMethod)
    {
        if (values != null) {
            final Iterator<? extends L> it = values.iterator();
            if (it.hasNext()) {
                final L value = it.next();
                if (it.hasNext()) {
                    if (caller != null) {
                        if (caller.warningOccurred) {
                            return value; // Skip the warning.
                        }
                        caller.warningOccurred = true;
                    }
                    warnIgnoredExtraneous(valueClass, callerClass, callerMethod);
                }
                return value;
            }
        }
        return null;
    }

    /**
     * Emit a warning about extraneous ignored values.
     *
     * @param  valueClass    The value class (usually a GeoAPI interface).
     * @param  callerClass   The caller class (usually an Apache SIS implementation of a GeoAPI interface).
     * @param  callerMethod  The caller method (usually the name of a getter method).
     */
    public static void warnIgnoredExtraneous(final Class<?> valueClass,
            final Class<?> callerClass, final String callerMethod)
    {
        Context.warningOccured(Context.current(), callerClass, callerMethod,
                Messages.class, Messages.Keys.IgnoredPropertiesAfterFirst_1, valueClass);
    }

    /**
     * Returns the given value as an empty or singleton collection.
     *
     * @param  <L>   The type of the old value.
     * @param  value The value, or {@code null} if none.
     * @return The given value as a collection.
     */
    public static <L> Collection<L> asCollection(final L value) {
        return (value != null) ? Collections.singleton(value) : Collections.<L>emptySet();
    }

    /**
     * Returns {@code true} if the given metadata is empty.
     */
    static boolean isEmpty(final Object container) {
        return (container instanceof AbstractMetadata) && ((AbstractMetadata) container).isEmpty();
    }



    // ┌───────────────────────────────────────────────────────────────────────────────────────────────────────┐
    // │                                 Methods from the Collection interface                                 │
    // └───────────────────────────────────────────────────────────────────────────────────────────────────────┘



    /**
     * Returns {@code true} if this collection is empty.
     *
     * @return {@code true} if this collection is empty.
     */
    @Override
    public final boolean isEmpty() {
        return !iterator().hasNext();
    }

    /**
     * Counts the number of non-null elements.
     *
     * @return Number of non-null elements.
     */
    @Override
    public final int size() {
        int count = 0;
        final Iterator<L> it = iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }

    /**
     * Adds a new element.
     *
     * @param  value The element to add.
     * @return {@code true} if the element has been added.
     */
    @Override
    public boolean add(final L value) {
        ArgumentChecks.ensureNonNull("value", value);
        return elements.add(wrap(value));
    }

    /**
     * Returns an iterator over the legacy elements.
     *
     * @return Iterator over the legacy elements.
     */
    @Override
    public final Iterator<L> iterator() {
        final Iterator<N> it = elements.iterator();
        return new AbstractIterator<L>() {
            /**
             * The container of the next value to return.
             */
            private N container;

            /**
             * Returns {@code true} if there is more elements to iterate.
             * This method prefetches and stores the next value.
             */
            @Override
            public final boolean hasNext() {
                if (next != null) {
                    return true;
                }
                while (it.hasNext()) {
                    container = it.next();
                    next = unwrap(container);
                    if (next != null) {
                        return true;
                    }
                }
                container = null;
                return false;
            }

            /**
             * Removes the last element returned by {@link #next()}.
             */
            @Override
            public final void remove() {
                if (container == null) {
                    throw new IllegalStateException();
                }
                if (!update(container, null)) {
                    throw new UnsupportedOperationException();
                }
                if (isEmpty(container)) {
                    it.remove();
                }
            }
        };
    }

    /**
     * Compares this collection with the given object for equality. This method performs comparisons only with
     * instances of {@code LegacyPropertyAdapter}, and returns {@code false} for all other kinds of collection.
     * We do <strong>not</strong> compare with arbitrary collection implementations.
     *
     * <p><b>Rational:</b> {@link Collection#equals(Object)} contract explicitely forbids comparisons with
     * {@code List} and {@code Set}. The rational explained in {@code Collection} javadoc applies also to
     * other kind of {@code Collection} implementations: we can not enforce {@code Collection.equals(Object)}
     * to be symmetric in such cases.</p>
     *
     * @param  other The other object to compare with this collection, or {@code null}.
     * @return {@code true} if the objects are equal, or {@code false} otherwise.
     */
    @Override
    public final boolean equals(final Object other) {
        if (!(other instanceof LegacyPropertyAdapter<?,?>)) {
            return false;
        }
        final Iterator<?> ot = ((LegacyPropertyAdapter<?,?>) other).iterator();
        final Iterator<L> it = iterator();
        while (it.hasNext()) {
            if (!ot.hasNext() || !Objects.equals(it.next(), ot.next())) {
                return false;
            }
        }
        return !ot.hasNext();
    }

    /**
     * Returns a hash code value for this collection.
     *
     * @return A hash code value calculated from the content of this collection.
     */
    @Override
    public final int hashCode() {
        int code = 0;
        for (final L element : this) {
            code = code*31 + Objects.hashCode(element);
        }
        return code;
    }
}
