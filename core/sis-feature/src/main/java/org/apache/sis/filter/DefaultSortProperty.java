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
package org.apache.sis.filter;

import java.util.Iterator;
import java.util.Collections;
import java.io.Serializable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.opengis.filter.SortOrder;
import org.opengis.filter.SortProperty;
import org.opengis.filter.ValueReference;


/**
 * Defines a sort order based on a property and ascending/descending order.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <R>  the type of resources (typically {@code Feature}) to sort.
 *
 * @since 1.1
 * @module
 */
final class DefaultSortProperty<R> implements SortProperty<R>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6065805794498223206L;

    /**
     * The property on which to apply sorting.
     */
    private final ValueReference<? super R, ?> property;

    /**
     * Whether the sorting order is {@code ASCENDING} or {@code DESCENDING}.
     */
    private final boolean descending;

    /**
     * Creates a new {@code SortProperty} instance.
     * It is caller responsibility to ensure that no argument is null.
     *
     * @param property  property on which to apply sorting.
     * @param order     the desired order: {@code ASCENDING} or {@code DESCENDING}.
     */
    DefaultSortProperty(final ValueReference<? super R, ?> property, final SortOrder order) {
        ArgumentChecks.ensureNonNull("property", property);
        ArgumentChecks.ensureNonNull("order",    order);
        this.property = property;
        descending = SortOrder.DESCENDING.equals(order);
    }

    /**
     * Returns the property to sort by.
     */
    @Override
    public ValueReference<? super R, ?> getValueReference() {
        return property;
    }

    /**
     * Returns the sort order: {@code ASCENDING} or {@code DESCENDING}.
     */
    @Override
    public SortOrder getSortOrder() {
        return descending ? SortOrder.DESCENDING : SortOrder.ASCENDING;
    }

    /**
     * Compares two resources for order. Returns a negative number if {@code r1} should be sorted before {@code r2},
     * a positive number if {@code r2} should be after {@code r1}, or 0 if both resources are equal.
     * The ordering of null resources or null property values is unspecified and may change in any future version.
     *
     * @param  r1  the first resource to compare.
     * @param  r2  the second resource to compare.
     * @return negative if the first resource is before the second, positive for the converse, or 0 if equal.
     * @throws ClassCastException if the types of {@linkplain ValueReference#apply(Object) property values}
     *         prevent them from being compared by this comparator.
     */
    @Override
    @SuppressWarnings({"rawtypes","unchecked"})
    public int compare(R r1, R r2) {
        if (descending) {
            final R rt = r1;
            r1 = r2;
            r2 = rt;
        }
        if (r1 == null) return +1;
        if (r2 == null) return -1;
        final Object o1 = property.apply(r1);
        final Object o2 = property.apply(r2);
        if (o1 == null) return +1;
        if (o2 == null) return -1;
        /*
         * Following code uses raw types and unsafe casts, but actually it should be okay.
         * If a cast is invalid, a `ClassCastException` will be thrown almost immediately,
         * either in this method or in `Containers.compare(…)` loop. Because this exception
         * is part of the `Comparator.compare(…)` method contract, we are compliant.
         */
        if (o1 instanceof Comparable<?> && o2 instanceof Comparable<?>) {
            return ((Comparable) o1).compareTo(o2);
        }
        if (o1 instanceof Iterable<?>) {
            return Containers.compare(((Iterable) o1).iterator(), iterator(o2));
        }
        if (o2 instanceof Iterable<?>) {
            return Containers.compare(iterator(o1), ((Iterable) o2).iterator());
        }
        throw new ClassCastException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                property.getXPath(), Comparable.class, (o1 instanceof Comparable<?> ? o2 : o1).getClass()));
    }

    /**
     * Returns an iterator for the given object. Intentionally raw return type for allowing unchecked casts
     * in {@link #compare(Object, Object)}. It is not as unsafe as it looks since {@link ClassCastException}
     * should happen soon if the type is incorrect and that exception is part of method contract.
     */
    @SuppressWarnings("rawtypes")
    private static Iterator iterator(final Object o) {
        return (o instanceof Iterable<?>) ? ((Iterable<?>) o).iterator() : Collections.singleton(o).iterator();
    }

    /**
     * Computes a hash code value for this filter.
     */
    @Override
    public int hashCode() {
        return property.hashCode() + Boolean.hashCode(descending);
    }

    /**
     * Compares this filter with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultSortProperty<?>) {
            final DefaultSortProperty<?> other = (DefaultSortProperty<?>) obj;
            return descending == other.descending && property.equals(other.property);
        }
        return false;
    }
}
