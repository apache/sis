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

import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;


/**
 * Static methods working on features.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class Features extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Features() {
    }

    /**
     * Casts the given attribute type to the given parameterized type.
     * An exception is thrown immediately if the given type does not have the expected
     * {@linkplain DefaultAttributeType#getValueClass() value class}.
     *
     * @param  <T>        The expected value class.
     * @param  type       The attribute type to cast, or {@code null}.
     * @param  valueClass The expected value class.
     * @return The attribute type casted to the given value class, or {@code null} if the given type was null.
     * @throws ClassCastException if the given attribute type does not have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <T> DefaultAttributeType<T> cast(final DefaultAttributeType<?> type, final Class<T> valueClass)
            throws ClassCastException
    {
        if (type != null) {
            final Class<?> actual = type.getValueClass();
            // We require a strict equality - not type.isAssignableFrom(actual) - because in
            // the later case we could have (to be strict) to return a <? extends T> type.
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Errors.format(Errors.Keys.MismatchedValueClass_3,
                        type.getName(), valueClass, actual));
            }
        }
        return (DefaultAttributeType<T>) type;
    }

    /**
     * Casts the given attribute instance to the given parameterized type.
     * An exception is thrown immediately if the given instance does not have the expected
     * {@linkplain DefaultAttributeType#getValueClass() value class}.
     *
     * @param  <T>        The expected value class.
     * @param  attribute  The attribute instance to cast, or {@code null}.
     * @param  valueClass The expected value class.
     * @return The attribute instance casted to the given value class, or {@code null} if the given instance was null.
     * @throws ClassCastException if the given attribute instance does not have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <T> DefaultAttribute<T> cast(final DefaultAttribute<?> attribute, final Class<T> valueClass)
            throws ClassCastException
    {
        if (attribute != null) {
            final Class<?> actual = attribute.getType().getValueClass();
            // We require a strict equality - not type.isAssignableFrom(actual) - because in
            // the later case we could have (to be strict) to return a <? extends T> type.
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Errors.format(Errors.Keys.MismatchedValueClass_3,
                        attribute.getName(), valueClass, actual));
            }
        }
        return (DefaultAttribute<T>) attribute;
    }
}
