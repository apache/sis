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
package org.apache.sis.filter.visitor;

import java.util.Map;
import org.opengis.filter.Expression;
import org.opengis.filter.ValueReference;


/**
 * Dummy implementation of property value reference.
 * The features handled by this implementation are property-value maps.
 *
 * @author  Johann Sorel (Geomatys)
 *
 * @param  <V>  type of values returned by this expression.
 */
final class ValueReferenceMock<V> implements ValueReference<Map<String,?>, V> {
    /**
     * Name of the property for which to get values.
     */
    private final String xpath;

    /**
     * Expected type of values.
     */
    private final Class<V> type;

    /**
     * Creates a new dummy reference.
     *
     * @param xpath  name of the property for which to get values.
     * @param type   type of values returned by this expression.
     */
    ValueReferenceMock(final String xpath, final Class<V> type) {
        this.xpath = xpath;
        this.type  = type;
    }

    /**
     * Returns the type of resources accepted by this mock.
     */
    @Override
    public Class<Map> getResourceClass() {
        return Map.class;
    }

    /**
     * Returns the name of the property for which to get values.
     */
    @Override
    public String getXPath() {
        return xpath;
    }

    /**
     * Returns the value of the referenced property in the given feature.
     */
    @Override
    public V apply(final Map<String,?> feature) {
        return type.cast(feature.get(xpath));
    }

    /**
     * Returns a reference to the same value but cast to a different type.
     */
    @Override
    public <N> Expression<Map<String,?>, N> toValueType(final Class<N> target) {
        return new ValueReferenceMock<>(xpath, target);
    }
}
