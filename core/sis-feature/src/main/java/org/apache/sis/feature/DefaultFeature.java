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

import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;


/**
 * An instance of {@linkplain DefaultFeatureType feature type} containing values for a real-world phenomena.
 *
 * @author  Travis L. Pinney
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class DefaultFeature implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6594295132544357870L;

    /**
     * Information about the feature.
     */
    private final DefaultFeatureType type;

    /**
     * The properties (attributes, operations, feature associations) of this feature.
     */
    private final Map<String, DefaultAttribute<?>> properties;

    /**
     * Creates a new features.
     *
     * @param type Information about the feature.
     */
    public DefaultFeature(final DefaultFeatureType type) {
        ArgumentChecks.ensureNonNull("type", type);
        this.type = type;
        properties = new HashMap<>(Math.min(16, Containers.hashMapCapacity(type.getCharacteristics().size())));
    }

    /**
     * Returns the value of the attribute of the given name.
     *
     * @param  name The attribute name.
     * @return The value for the given attribute, or {@code null} if none.
     */
    public Object getAttributeValue(final String name) {
        final DefaultAttribute<?> attribute = properties.get(name);
        return (attribute != null) ? attribute.getValue() : null;
    }

    /**
     * Sets the value of the attribute of the given name.
     *
     * @param name  The attribute name.
     * @param value The new value for the given attribute (may be {@code null}).
     */
    @SuppressWarnings("unchecked")
    public void setAttributeValue(final String name, final Object value) {
        DefaultAttribute<?> attribute = properties.get(name);
        if (attribute == null) {
            if (value == null) {
                return;
            }
            final DefaultAttributeType<?> at = type.getProperty(name);
            if (at == null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, type.getName(), name));
            }
            attribute = new DefaultAttribute<>(at);
        }
        ArgumentChecks.ensureCanCast(name, attribute.getType().getValueClass(), value);
        ((DefaultAttribute) attribute).setValue(value);
    }

    /**
     * Returns a hash code value for this feature.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return type.hashCode() + 37 * properties.hashCode();
    }

    /**
     * Compares this feature with the given object for equality.
     *
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            final DefaultFeature that = (DefaultFeature) obj;
            return type.equals(that.type) &&
                   properties.equals(that.properties);
        }
        return false;
    }

    /**
     * Returns a string representation of this feature.
     * The returned string is for debugging purpose and may change in any future SIS version.
     *
     * @return A string representation of this feature for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String lineSeparator = System.lineSeparator();
        for (final DefaultAttribute<?> attribute : properties.values()) {
            sb.append(attribute.getType().getName()).append(": ").append(attribute.getValue()).append(lineSeparator);
        }
        return sb.toString();
    }
}
