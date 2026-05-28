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
package org.apache.sis.metadata;

import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;


/**
 * A key in the {@link MetadataStandard} internal cache.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CacheKey {
    /**
     * The metadata class (interface or implementation) for which a {@link PropertyAccessor} will be associated.
     * May be null if unknown, in which case {@link #isValid()} returns {@code false}.
     */
    final Class<?> type;

    /**
     * If the {@link #type} is the type of the value of a property, the type declared in the property signature.
     * This information allows to resolve ambiguities when a class implements more than one metadata interface.
     * Some examples are found in the {@link org.apache.sis.metadata.simple} package.
     *
     * <p><b>Invariant:</b> {@code propertyType.isAssignableFrom(type)}</p>
     *
     * <p>This field shall never be null. If there is no property type information,
     * then this field shall be set to {@code Object.class}.</p>
     */
    final Class<?> propertyType;

    /**
     * Creates a new key to use in the cache.
     * The {@code propertyType.isInstance(metadata)} condition should always be {@code true},
     * but this is not verified by this constructor. Instead, the validity can be verified
     * after constructor with {@link #isValid()}.
     *
     * @param  type          the metadata class, or {@code null}.
     * @param  propertyType  base class of the metadata object.
     */
    CacheKey(final Class<?> type, final Class<?> propertyType) {
        this.type = type;
        this.propertyType = propertyType;
    }

    /**
     * Returns whether instances of {@link #type} can can be values of a property of type {@link #propertyType}.
     */
    final boolean isValid() {
        return (type != null) && propertyType.isAssignableFrom(type);
    }

    /**
     * Returns a hash code value for this key.
     */
    @Override
    public int hashCode() {
        int code = propertyType.hashCode();
        if (type != null) code += 31 * type.hashCode();
        return code;
    }

    /**
     * Compares this key with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof CacheKey) {
            final var other = (CacheKey) obj;
            return (type == other.type) && (propertyType == other.propertyType);
        }
        return false;
    }

    /**
     * Returns a string representation for debugging purpose only.
     */
    @Override
    public String toString() {
        String name = Classes.getShortName(type);
        if (propertyType != Object.class) {
            name = name + " as " + Classes.getShortName(propertyType);
        }
        return name;
    }

    /**
     * Creates an error message for an unrecognized type.
     */
    final String unrecognized() {
        return Errors.format(Errors.Keys.UnknownType_1, type);
    }

    /**
     * Creates an error message for an invalid key.
     */
    final String invalid() {
        return Errors.format(Errors.Keys.IllegalArgumentClass_3, "type", propertyType, type);
    }
}
