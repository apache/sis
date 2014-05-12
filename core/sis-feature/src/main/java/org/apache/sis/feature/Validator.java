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
package org.apache.sis.feature;

import org.apache.sis.util.resources.Errors;


/**
 * Provides validation methods to be shared by different implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class Validator {
    /**
     * Do not allow (for now) instantiation of this class.
     */
    private Validator() {
    }

    /**
     * Ensures that the give element is an instance of the expected type.
     * The caller shall ensure that the element is non-null before to invoke this method.
     */
    static void ensureValidType(final AbstractIdentifiedType type, final DefaultAttribute<?> element) {
        if (element.getType() != type) {
            // TODO: replace RuntimeException by IllegalAttributeException after GeoAPI review.
            throw new RuntimeException(Errors.format(Errors.Keys.MismatchedPropertyType_1, type.getName()));
        }
    }

    /**
     * Ensures that the given value is valid for the given attribute type.
     * This method delegates to one of the {@code ensureValidValue(…)} methods depending of the value type.
     */
    static void ensureValid(final PropertyType type, final Object value) {
        if (type instanceof DefaultAttributeType<?>) {
            ensureValidValue((DefaultAttributeType<?>) type, value);
        }
        if (type instanceof DefaultAssociationRole) {
            ensureValidValue((DefaultAssociationRole) type, (DefaultFeature) value);
        }
    }

    /**
     * Ensures that the given value is valid for the given attribute type.
     */
    static void ensureValidValue(final DefaultAttributeType<?> type, final Object value) {
        if (value == null) {
            return;
        }
        /*
         * In theory, the following check is unnecessary since the type was constrained by the Attribute.setValue(T)
         * method signature. However in practice the call to Attribute.setValue(…) is sometime done after type erasure,
         * so we are better to check.
         */
        if (!type.getValueClass().isInstance(value)) {
            throw new RuntimeException( // TODO: IllegalAttributeException, pending GeoAPI revision.
                    Errors.format(Errors.Keys.IllegalPropertyClass_2, type.getName(), value.getClass()));
        }
    }

    /**
     * Ensures that the given value is valid for the given association role.
     */
    static void ensureValidValue(final DefaultAssociationRole role, final DefaultFeature value) {
        if (value == null) {
            return;
        }
        final DefaultFeatureType type = value.getType();
        if (!role.getValueType().isAssignableFrom(type)) {
            throw new RuntimeException( // TODO: IllegalAttributeException, pending GeoAPI revision.
                    Errors.format(Errors.Keys.IllegalPropertyClass_2, role.getName(), type.getName()));
        }
    }
}
