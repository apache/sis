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

import java.io.Serializable;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;

// Related to JDK7
import java.util.Objects;


/**
 * Holds the value of an attribute in a feature. The value can be an arbitrary Java object.
 * Constraints like the base Java class and domain of values are specified by the
 * {@linkplain DefaultAttributeType attribute type} associated to each attribute instance.
 *
 * @param <T> The type of attribute values.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class DefaultAttribute<T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8103788797446754561L;

    /**
     * Information about the attribute (base Java class, domain of values, <i>etc.</i>).
     */
    private final DefaultAttributeType<T> type;

    /**
     * The attribute value.
     */
    private T value;

    /**
     * Creates a new attribute of the given type.
     * The value is initialized to the {@linkplain DefaultAttributeType#getDefaultValue() default value}.
     *
     * @param type Information about the attribute (base Java class, domain of values, <i>etc.</i>).
     */
    public DefaultAttribute(final DefaultAttributeType<T> type) {
        ArgumentChecks.ensureNonNull("type", type);
        this.type = type;
        value = type.getDefaultValue();
    }

    /**
     * Returns information about the attribute (base Java class, domain of values, <i>etc.</i>).
     *
     * @return Information about the attribute.
     */
    public DefaultAttributeType<T> getType() {
        return type;
    }

    /**
     * Returns the attribute value.
     *
     * @return The attribute value (may be {@code null}).
     */
    public T getValue() {
        return value;
    }

    /**
     * Sets the attribute value.
     *
     * <div class="warning">Current implementation does not yet performed any validation.
     * However future Apache SIS version is likely to check argument validity here.</div>
     *
     * @param  value The new value.
     * @throws IllegalArgumentException If the given value is outside the attribute domain.
     */
    public void setValue(final T value) throws IllegalArgumentException {
        this.value = value;
    }

    /**
     * Returns a hash code value for this attribute type.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return type.hashCode() + Objects.hashCode(value);
    }

    /**
     * Compares this attribute with the given object for equality.
     *
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj.getClass() == getClass()) {
            final DefaultAttribute<?> that = (DefaultAttribute<?>) obj;
            return type.equals(that.type) &&
                   Objects.equals(value, that.value);
        }
        return false;
    }

    /**
     * Returns a string representation of this attribute.
     * The returned string is for debugging purpose and may change in any future SIS version.
     *
     * @return A string representation of this attribute for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return type.toString("Attribute").append(" = ").append(value).toString();
    }
}
