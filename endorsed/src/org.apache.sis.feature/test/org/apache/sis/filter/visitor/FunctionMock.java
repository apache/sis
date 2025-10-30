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
import java.util.List;
import org.opengis.util.ScopedName;
import org.opengis.filter.Expression;
import org.apache.sis.util.iso.Names;


/**
 * Dummy implementation of filter function.
 * The features handled by this implementation are property-value maps.
 * This class stores a function name and parameters but cannot do any operation.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class FunctionMock implements Expression<Map<String,?>, Object> {
    /**
     * The local part of the function name.
     */
    private final String name;

    /**
     * The function parameters.
     */
    private final List<Expression<Map<String,?>, ?>> parameters;

    /**
     * Creates a new dummy function.
     *
     * @param  name        the local part of the function name.
     * @param  parameters  the function parameters.
     */
    FunctionMock(final String name, final List<Expression<Map<String,?>, ?>> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    /**
     * Returns the function name.
     */
    @Override
    public ScopedName getFunctionName() {
        return Names.createScopedName(null, null, name);
    }

    /**
     * Returns the type of resources accepted by this mock.
     */
    @Override
    public Class<Map> getResourceClass() {
        return Map.class;
    }

    /**
     * Returns the function parameters.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Expression<Map<String,?>, ?>> getParameters() {
        return parameters;
    }

    /**
     * Unsupported operation.
     */
    @Override
    public Object apply(Map<String,?> feature) {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public <N> Expression<Map<String,?>, N> toValueType(Class<N> target) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
