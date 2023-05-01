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

import java.util.function.Predicate;
import org.apache.sis.internal.filter.Node;

// Branch-dependent imports
import org.opengis.filter.Filter;


/**
 * Base class of some (not all) nodes that are filters. This base class overrides {@link Predicate}
 * methods for building other {@link Filter} objects instead of default Java implementations that
 * Apache SIS cannot recognize.
 *
 * <p><b>Note:</b> this class duplicates the method definition in {@link Optimization.OnFilter}.
 * This duplication exists because not all filter implementations extends this class, and not all
 * implementations implement the {@link Optimization.OnFilter} interface.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 *
 * @since 1.1
 */
abstract class FilterNode<R> extends Node implements Filter<R> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1272149643938168189L;

    /**
     * Creates a new node.
     */
    FilterNode() {
    }

    /**
     * If the given predicate can be casted to a filter of the same parameterized type as the template,
     * returns {@code other} casted to that type. Otherwise returns {@code null}.
     *
     * @param  <R>       desired parameterized type.
     * @param  template  the filter from which to get the runtime value of {@code <R>}.
     * @param  other     the predicate to cast to a filter compatible with the target.
     * @return the casted predicate, or {@code null} if it cannot be casted.
     */
    @SuppressWarnings("unchecked")
    static <R> Filter<R> castOrNull(final Filter<R> template, final Predicate<? super R> other) {
        if (other instanceof Filter<?>) {
            final Class<?> type = template.getResourceClass();
            if (type != null) {
                final Class<?> to = ((Filter<?>) other).getResourceClass();
                if (to != null && type.isAssignableFrom(to)) {
                    return (Filter<R>) other;
                }
            }
        }
        return null;
    }

    /**
     * Returns the {@code AND} logical operation between this filter and the given predicate.
     * This method duplicates the {@link Optimization.OnFilter#and(Predicate)} method, but is
     * defined because not all subclasses implement the {@code Optimization} inner interface.
     */
    @Override
    public final Predicate<R> and(final Predicate<? super R> other) {
        final Filter<R> filter = castOrNull(this, other);
        if (filter != null) {
            return new LogicalFilter.And<>(this, filter);
        } else {
            return Filter.super.and(other);
        }
    }

    /**
     * Returns the {@code OR} logical operation between this filter and the given predicate.
     * This method duplicates the {@link Optimization.OnFilter#or(Predicate)} method, but is
     * defined because not all subclasses implement the {@code Optimization} inner interface.
     */
    @Override
    public final Predicate<R> or(final Predicate<? super R> other) {
        final Filter<R> filter = castOrNull(this, other);
        if (filter != null) {
            return new LogicalFilter.Or<>(this, filter);
        } else {
            return Filter.super.and(other);
        }
    }

    /**
     * Returns the logical negation of this filter.
     */
    @Override
    public final Predicate<R> negate() {
        return new LogicalFilter.Not<>(this);
    }
}
