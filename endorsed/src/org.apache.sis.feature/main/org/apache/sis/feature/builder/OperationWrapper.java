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

import java.util.Objects;
import org.opengis.util.GenericName;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.PropertyType;


/**
 * Wraps an existing operation. This package cannot create new operations, except for a few special cases.
 * The user need to specify fully formed objects.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class OperationWrapper extends PropertyTypeBuilder {
    /**
     * The wrapped operation.
     */
    private final PropertyType operation;

    /**
     * Creates a new wrapper for the given operation.
     */
    OperationWrapper(final FeatureTypeBuilder owner, final PropertyType operation) {
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
    public PropertyType build() {
        return operation;
    }

    /**
     * Do not allow a change of multiplicity.
     */
    @Override public PropertyTypeBuilder setMinimumOccurs(int occurs) {return readOnly(1, occurs);}
    @Override public PropertyTypeBuilder setMaximumOccurs(int occurs) {return readOnly(1, occurs);}

    /**
     * Do not allow modifications.
     */
    @Override public PropertyTypeBuilder setName       (GenericName name)         {return readOnly(str(getName()),   str(name));}
    @Override public PropertyTypeBuilder setDefinition (CharSequence definition)  {return readOnly(getDefinition(),  definition);}
    @Override public PropertyTypeBuilder setDesignation(CharSequence designation) {return readOnly(getDesignation(), designation);}
    @Override public PropertyTypeBuilder setDescription(CharSequence description) {return readOnly(getDescription(), description);}
    @Override public PropertyTypeBuilder setDeprecated (boolean deprecated)       {return readOnly(isDeprecated(),   deprecated);}

    /**
     * Returns the string representation of the given name, or {@code null} if the name is null.
     * This is used for relaxing the comparison of names before to throw an exception.
     */
    private static String str(final GenericName name) {
        return (name != null) ? name.toString() : null;
    }

    /**
     * Throws {@link UnsupportedOperationException} if the given value is different than the expected value.
     */
    private OperationWrapper readOnly(final Object expected, final Object actual) {
        if (Objects.equals(expected, actual)) {
            return this;
        }
        throw new UnsupportedOperationException(errors().getString(Errors.Keys.UnmodifiableObject_1, PropertyTypeBuilder.class));
    }
}
