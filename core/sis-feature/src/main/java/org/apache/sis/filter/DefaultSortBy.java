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

// Branch-dependent imports
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.filter.expression.PropertyName;


/**
 * Defines a sort order based on a property and ascending/descending order.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class DefaultSortBy implements SortBy, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5434026034835575812L;

    /**
     * The property on which to apply sorting.
     */
    private final PropertyName property;

    /**
     * The desired order: {@code ASCENDING} or {@code DESCENDING}.
     */
    private final SortOrder order;

    /**
     * Creates a new {@code SortBy} filter.
     * It is caller responsibility to ensure that no argument is null.
     *
     * @param property  property on which to apply sorting.
     * @param order     the desired order: {@code ASCENDING} or {@code DESCENDING}.
     */
    DefaultSortBy(final PropertyName property, final SortOrder order) {
        this.property = property;
        this.order    = order;
    }

    /**
     * Returns the property to sort by.
     */
    @Override
    public PropertyName getPropertyName() {
        return property;
    }

    /**
     * Returns the sort order: {@code ASCENDING} or {@code DESCENDING}.
     */
    @Override
    public SortOrder getSortOrder() {
        return order;
    }

    /**
     * Computes a hash code value for this filter.
     */
    @Override
    public int hashCode() {
        return property.hashCode() + 41 * order.hashCode();
    }

    /**
     * Compares this filter with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultSortBy) {
            final DefaultSortBy other = (DefaultSortBy) obj;
            return property.equals(other.property)
                   && order.equals(other.order);
        }
        return false;
    }
}
