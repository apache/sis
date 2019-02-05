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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;

/**
 * Binary logic filter AND.
 * All children filters must be true to pass.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
final class DefaultAnd implements And, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8726983485969293693L;

    private final List<Filter> filters;

    public DefaultAnd(List<Filter> filters) {
        if (filters.size() < 2) throw new IllegalArgumentException("At least two filters are requiered");
        this.filters = Collections.unmodifiableList(filters);
    }

    /**
     * {@inheritDoc}
     * @return list of at least two {@code Filter}.
     */
    @Override
    public List<Filter> getChildren() {
        return filters;
    }

    @Override
    public boolean evaluate(Object object) {
        for (Filter filter : filters) {
            if (!filter.evaluate(object)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultAnd other = (DefaultAnd) obj;
        return Objects.equals(this.filters, other.filters);
    }

    @Override
    public int hashCode() {
        return 43 * filters.hashCode();
    }

    @Override
    public String toString() {
        return AbstractExpression.toStringTree("And", filters);
    }

}
