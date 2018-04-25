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

import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * Immutable SortBy.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class DefaultSortBy implements SortBy {

    private final PropertyName property;
    private final SortOrder order;

    /**
     *
     * @param property sort by property applied on.
     * @param order sorting order
     */
    public DefaultSortBy(PropertyName property, SortOrder order) {
        this.property = property;
        this.order = order;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyName getPropertyName() {
        return property;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public SortOrder getSortOrder() {
        return order;
    }

}
