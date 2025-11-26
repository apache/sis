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
package org.apache.sis.storage.base;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.io.Serializable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;

// Specific to the main branch:
import org.apache.sis.pending.geoapi.filter.SortBy;
import org.apache.sis.pending.geoapi.filter.SortProperty;
import org.apache.sis.pending.geoapi.filter.ValueReference;


/**
 * Comparator sorting features using an array of {@link SortProperty} elements applied in order.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (typically {@code Feature}) to sort.
 */
public final class SortByComparator<R> implements SortBy<R>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7964849249532212389L;

    /**
     * The sort order specified to the constructor.
     *
     * @see #getSortProperties()
     */
    @SuppressWarnings("serial")                     // Most SIS implementations are serializable.
    private final SortProperty<R>[] properties;

    /**
     * Creates a new comparator for the given sort expression.
     * This is shortcut for the common case where there is a single expression.
     *
     * @param  p  the property to wrap in a {@link SortBy} comparator.
     */
    @SuppressWarnings({"unchecked","rawtypes"})             // Generic array creation.
    private SortByComparator(final SortProperty<R> p) {
        ArgumentChecks.ensureNonNullElement("properties", 0, p);
        properties = new SortProperty[] {p};
    }

    /**
     * Creates a new comparator with the values of given map.
     * The map is used for removing duplicated expressions.
     */
    @SuppressWarnings({"unchecked","rawtypes"})             // Generic array creation.
    private SortByComparator(final Map<?, SortProperty<R>> merged) {
        properties = merged.values().toArray(SortProperty[]::new);
    }

    /**
     * Creates a new comparator for the given sort expressions.
     *
     * @param  <R>         the type of resources (typically {@code Feature}) to sort.
     * @param  properties  the sort order.
     * @return the comparator, or {@code null} if the given array is empty.
     */
    public static <R> SortByComparator<R> create(final SortProperty<R>[] properties) {
        switch (properties.length) {
            case 0: return null;
            case 1: return new SortByComparator<>(properties[0]);
        }
        final Map<ValueReference<R,?>, SortProperty<R>> merged = new LinkedHashMap<>();
        addAll(Arrays.asList(properties), merged);
        return new SortByComparator<>(merged);
    }

    /**
     * Creates a new comparator as the concatenation of the two given comparators.
     * The first comparator is used first, and if two resources are equal then the
     * second comparator is used.
     *
     * @param  <R>         the type of resources (typically {@code Feature}) to sort.
     * @param  sort        the first "sort by" to use, or {@code null} if none.
     * @param  comparator  the second "sort by" to use.
     * @return concatenation of the two comparators.
     */
    @SuppressWarnings("unchecked")
    public static <R> SortBy<? super R> concatenate(final SortBy<? super R> sort, final Comparator<? super R> comparator) {
        final SortBy<? super R> other;
        if (comparator instanceof SortBy<?>) {
            other = (SortBy<? super R>) comparator;
        } else if (comparator instanceof SortProperty<?>) {
            other = new SortByComparator<>((SortProperty<? super R>) comparator);
        } else {
            return null;
        }
        if (sort == null) {
            return other;
        }
        /*
         * The (SortBy<R>) casts are unsafe — they should be (SortBy<? super R>) — but are okay in this context
         * because we create a `SortByComparator` which will only "push" instances of R to the comparators.
         * There is no code that "pull" instances of R from the comparators, consequently no code that may
         * be surprised to get an instance of a super type of R instead of R.
         */
        return concatenate((SortBy<R>) sort, (SortBy<R>) other);
    }

    /**
     * Creates a new comparator as the concatenation of the two given comparators.
     * The first comparator is used first, and if two resources are equal then the
     * second comparator is used.
     *
     * @param  <R>  the type of resources (typically {@code Feature}) to sort.
     * @param  s1   the first "sort by" to use.
     * @param  s2   the second "sort by" to use.
     * @return concatenation of the two comparators.
     */
    public static <R> SortBy<R> concatenate(final SortBy<R> s1, final SortBy<R> s2) {
        final Map<ValueReference<R,?>, SortProperty<R>> merged = new LinkedHashMap<>();
        addAll(s1.getSortProperties(), merged);
        addAll(s2.getSortProperties(), merged);
        return new SortByComparator<>(merged);
    }

    /**
     * Adds all elements of the {@code properties} list into the {@code merged} map.
     * If two {@code SortProperty} instances use the same {@link ValueReference},
     * then only the first occurrence is retained.
     */
    private static <R> void addAll(final List<SortProperty<R>> properties,
            final Map<ValueReference<R,?>, SortProperty<R>> merged)
    {
        final int size = properties.size();
        for (int i=0; i<size; i++) {
            final SortProperty<R> p = properties.get(i);
            ArgumentChecks.ensureNonNullElement("properties", i, p);
            merged.putIfAbsent(p.getValueReference(), p);
        }
    }

    /**
     * Returns the properties whose values are used for sorting.
     * The list shall have a minimum of one element.
     */
    @Override
    public List<SortProperty<R>> getSortProperties() {
        return UnmodifiableArrayList.wrap(properties);
    }

    /**
     * Compares two resources for order. Returns a negative number if {@code r1} should be sorted before {@code r2},
     * a positive number if {@code r2} should be after {@code r1}, or 0 if both resources are equal.
     * The ordering of null resources or null property values is unspecified.
     */
    @Override
    public int compare(final R r1, final R r2) {
        for (final SortProperty<R> p : properties) {
            final int c = p.compare(r1, r2);
            if (c != 0) return c;
        }
        return 0;
    }

    /**
     * Returns a comparator as the concatenation of this comparator with the given one.
     * This comparator is used first, and if two resources are equal then the other comparator is used.
     *
     * @param  other  the other comparator to be used when this comparator considers two resources as equal.
     * @return concatenation of this comparator with the given one.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Comparator<R> thenComparing(final Comparator<? super R> other) {
        if (other instanceof SortBy<?>) {
            /*
             * The (SortBy<R>) cast is unsafe — it should be (SortBy<? super R>) — but it is okay in this context
             * because we create a `SortByComparator` which will only "push" instances of R to the `other` comparator.
             * There is no code that "pull" instances of R from the `other` comparator, consequently no code that may
             * be surprised to get an instance of a super type of R instead of R.
             */
            return concatenate(this, (SortBy<R>) other);
        }
        return SortBy.super.thenComparing(other);
    }
}
