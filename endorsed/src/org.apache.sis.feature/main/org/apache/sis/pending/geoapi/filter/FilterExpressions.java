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
package org.apache.sis.pending.geoapi.filter;

// Specific to the main branch:
import java.util.List;
import java.util.Objects;
import java.util.AbstractList;
import java.util.Locale;
import org.opengis.util.ScopedName;
import org.apache.sis.filter.Filter;
import org.apache.sis.filter.Expression;
import org.apache.sis.util.iso.Names;


/**
 * Placeholder for GeoAPI 3.1 interfaces (not yet released).
 * Shall not be visible in public API, as it will be deleted after next GeoAPI release.
 */
@SuppressWarnings("doclint:missing")
final class FilterExpressions<R> extends AbstractList<Expression<R,?>> {
    private final List<Filter<R>> filters;

    FilterExpressions(final List<Filter<R>> filters) {
        this.filters = Objects.requireNonNull(filters);
    }

    @Override
    public boolean isEmpty() {
        return filters.isEmpty();
    }

    @Override
    public int size() {
        return filters.size();
    }

    @Override
    public Expression<R,?> get(final int index) {
        return new Element<>(filters.get(index));
    }

    private static final class Element<R> implements Expression<R,Boolean> {
        private final Filter<R> filter;

        Element(final Filter<R> filter) {
            this.filter = Objects.requireNonNull(filter);
        }

        @Override
        public ScopedName getFunctionName() {
            final Enum<?> type = filter.getOperatorType();
            final String identifier = type.name().toLowerCase(Locale.US);
            if (identifier != null) {
                return Names.createScopedName(Name.STANDARD, null, identifier);
            } else {
                return Names.createScopedName(Name.EXTENSION, null, type.name());
            }
        }

        @Override
        public Class<? super R> getResourceClass() {
            return filter.getResourceClass();
        }

        @Override
        public List<Expression<R,?>> getParameters() {
            return filter.getExpressions();
        }

        @Override
        public Boolean apply(final R input) {
            return filter.test(input);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <N> Expression<R,N> toValueType(final Class<N> type) {
            if (type.isAssignableFrom(Boolean.class)) return (Expression<R,N>) this;
            else throw new ClassCastException();
        }

        @Override
        public int hashCode() {
            return ~filter.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return (obj instanceof Element) && filter.equals(((Element) obj).filter);
        }

        @Override
        public String toString() {
            return "Expression[" + filter.toString() + ']';
        }
    }
}
