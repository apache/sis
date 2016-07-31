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
package org.apache.sis.feature.builder;

import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;
import org.opengis.util.GenericName;
import org.apache.sis.util.resources.Errors;


/**
 * Wraps an existing operation. This package can not create new operations, except for a few special cases.
 * The user need to specify fully formed objects.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class OperationWrapper extends PropertyTypeBuilder {
    /**
     * The wrapped operation.
     */
    private final Operation operation;

    /**
     * Creates a new wrapper for the given operation.
     */
    OperationWrapper(final FeatureTypeBuilder owner, final Operation operation) {
        super(owner, operation);
        this.operation = operation;
        minimumOccurs = 1;
        maximumOccurs = 1;
    }

    /**
     * Returns the wrapped operation.
     */
    @Override
    PropertyType create() {
        return operation;
    }

    /**
     * Do not allow a change of cardinality.
     */
    @Override
    public PropertyTypeBuilder setCardinality(final int minimumOccurs, final int maximumOccurs) {
        if (minimumOccurs != 1 || maximumOccurs != 1) {
            throw readOnly();
        }
        return this;
    }

    /**
     * Do not allow modifications.
     */
    @Override public TypeBuilder setName       (GenericName name)         {throw readOnly();}
    @Override public TypeBuilder setDefinition (CharSequence definition)  {throw readOnly();}
    @Override public TypeBuilder setDesignation(CharSequence designation) {throw readOnly();}
    @Override public TypeBuilder setDescription(CharSequence description) {throw readOnly();}

    /**
     * Returns the exception to be thrown for read-only wrapper.
     */
    private UnsupportedOperationException readOnly() {
        throw new UnsupportedOperationException(errors().getString(Errors.Keys.UnmodifiableObject_1, PropertyTypeBuilder.class));
    }
}
