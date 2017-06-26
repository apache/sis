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

import org.opengis.util.GenericName;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.apache.sis.feature.AbstractIdentifiedType;


/**
 * Wraps an existing operation. This package can not create new operations, except for a few special cases.
 * The user need to specify fully formed objects.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class OperationWrapper extends PropertyTypeBuilder {
    /**
     * The wrapped operation.
     */
    private final AbstractIdentifiedType operation;

    /**
     * Creates a new wrapper for the given operation.
     */
    OperationWrapper(final FeatureTypeBuilder owner, final AbstractIdentifiedType operation) {
        super(owner);
        this.operation = operation;
        minimumOccurs = 1;
        maximumOccurs = 1;
        initialize(operation);
    }

    /**
     * Returns the wrapped operation.
     */
    @Override
    public AbstractIdentifiedType build() {
        return operation;
    }

    /**
     * Do not allow a change of cardinality.
     */
    @Override public PropertyTypeBuilder setMinimumOccurs(int occurs) {if (occurs == 1) return this; throw readOnly();}
    @Override public PropertyTypeBuilder setMaximumOccurs(int occurs) {if (occurs == 1) return this; throw readOnly();}

    /**
     * Do not allow modifications.
     */
    @Override public TypeBuilder setName       (GenericName name)         {throw readOnly();}
    @Override public TypeBuilder setDefinition (CharSequence definition)  {throw readOnly();}
    @Override public TypeBuilder setDesignation(CharSequence designation) {throw readOnly();}
    @Override public TypeBuilder setDescription(CharSequence description) {throw readOnly();}
    @Override public TypeBuilder setDeprecated (boolean deprecated)       {throw readOnly();}

    /**
     * Returns the exception to be thrown for read-only wrapper.
     */
    private UnsupportedOperationException readOnly() {
        throw new UnsupportedOperationException(errors().getString(Errors.Keys.UnmodifiableObject_1, PropertyTypeBuilder.class));
    }
}
