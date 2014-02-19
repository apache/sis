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
package org.apache.sis.parameter;

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Static;


/**
 * Static methods working on parameters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
public final class Parameters extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Parameters() {
    }

    /**
     * Casts the given parameter descriptor to the given type.
     * An exception is thrown immediately if the parameter does not have the expected value class.
     *
     * @param  <T> The expected value class.
     * @param  descriptor The descriptor to cast, or {@code null}.
     * @param  type The expected value class.
     * @return The descriptor casted to the given type, or {@code null} if the given descriptor was null.
     * @throws ClassCastException if the given descriptor doesn't have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <T> ParameterDescriptor<T> cast(final ParameterDescriptor<?> descriptor, final Class<T> type)
            throws ClassCastException
    {
        if (descriptor != null) {
            final Class<?> actual = descriptor.getValueClass();
            // We require a strict equality - not type.isAssignableFrom(actual) - because in
            // the later case we could have (to be strict) to return a <? extends T> type.
            if (!type.equals(actual)) {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalParameterType_2,
                        descriptor.getName().getCode(), actual));
            }
        }
        return (ParameterDescriptor<T>) descriptor;
    }

    /**
     * Casts the given parameter value to the given type.
     * An exception is thrown immediately if the parameter does not have the expected value class.
     *
     * @param  <T>   The expected value class.
     * @param  value The value to cast, or {@code null}.
     * @param  type  The expected value class.
     * @return The value casted to the given type, or {@code null} if the given value was null.
     * @throws ClassCastException if the given value doesn't have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <T> ParameterValue<T> cast(final ParameterValue<?> value, final Class<T> type)
            throws ClassCastException
    {
        if (value != null) {
            final ParameterDescriptor<?> descriptor = value.getDescriptor();
            final Class<?> actual = descriptor.getValueClass();
            if (!type.equals(actual)) { // Same comment than cast(ParameterDescriptor)...
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalParameterType_2,
                        descriptor.getName().getCode(), actual));
            }
        }
        return (ParameterValue<T>) value;
    }
}
