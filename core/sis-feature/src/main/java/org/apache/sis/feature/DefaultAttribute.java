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
public class DefaultAttribute<T> implements Serializable {
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
     *
     * @see DefaultFeature#getAttributeValue(String)
     */
    public T getValue() {
        return value;
    }

    /**
     * Sets the attribute value.
     *
     * {@section Validation}
     * The amount of validation performed by this method is implementation dependent.
     * The current {@code DefaultAttribute} implementation performs only very cheap (if any) validations.
     * A more exhaustive verification can be performed by invoking the {@link #validate()} method.
     *
     * @param  value The new value.
     * @throws RuntimeException If this method performs validation and the given value does not meet the conditions.
     *         <em>This exception will be changed to {@code IllegalAttributeException} in a future SIS version.</em>
     *
     * @see DefaultFeature#setAttributeValue(String, Object)
     */
    public void setValue(final T value) {
        this.value = value;
    }

    /**
     * Ensures that the current attribute value complies with the constraints defined by the attribute type.
     * This method can be invoked explicitly on a single attribute, or may be invoked implicitly by a call to
     * {@link DefaultFeature#validate()}.
     *
     * @throws RuntimeException If the current attribute value violates a constraint.
     *         <em>This exception will be changed to {@code IllegalAttributeException} in a future SIS version.</em>
     *
     * @see DefaultFeature#validate()
     */
    public void validate() {
        // TODO: future SIS implementation shall check constraints here.
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
        if (obj != null && obj.getClass() == getClass()) {
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
