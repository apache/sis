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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ConcurrentModificationException;
import java.io.Serializable;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.measure.NumberRange;

// Related to JDK7
import java.util.Objects;


/**
 * An instance of a {@linkplain DefaultFeatureType feature type} containing values for a real-world phenomena.
 *
 * {@section Usage in multi-thread environment}
 * {@code DefaultFeature} are <strong>not</strong> thread-safe.
 * Synchronization, if needed, shall be done externally by the caller.
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
     * Information about the feature (name, characteristics, <i>etc.</i>).
     */
    private final DefaultFeatureType type;

    /**
     * The properties (attributes, operations, feature associations) of this feature.
     * Each value can be one of the following types (from most generic to most specific):
     *
     * <ul>
     *   <li>A {@link MultiValues}, which is a list of {@code Attribute}.</li>
     *   <li>An {@code Attribute} in the common case of [0…1] cardinality.</li>
     *   <li>An object in the common case of [0…1] cardinality when only the value
     *       (not the {@code Attribute} object) is requested.</li>
     * </ul>
     *
     * The intend is to reduce the amount of allocate objects as much as possible,
     * because typical SIS applications may create a very large amount of features.
     */
    private final Map<String, Object> properties;

    /**
     * Creates a new features.
     *
     * @param type Information about the feature (name, characteristics, <i>etc.</i>).
     */
    public DefaultFeature(final DefaultFeatureType type) {
        ArgumentChecks.ensureNonNull("type", type);
        this.type = type;
        properties = new HashMap<>(Math.min(16, Containers.hashMapCapacity(type.getCharacteristics().size())));
    }

    /**
     * Returns information about the feature (name, characteristics, <i>etc.</i>).
     *
     * @return Information about the feature.
     */
    public DefaultFeatureType getType() {
        return type;
    }

    /**
     * Returns {@code true} if the given cardinality is for a singleton property.
     */
    private static boolean isSingleton(final NumberRange<Integer> cardinality) {
        switch (cardinality.getMaxValue()) {
            case 0:
            case 1:  return true;
            case 2:  return !cardinality.isMaxIncluded();
            default: return false;
        }
    }

    /**
     * Returns all properties (attributes, operations or associations) of the given name.
     * The returned list is <em>live</em>: change in that list will be immediately reflected
     * in this {@code DefaultFeature}, and conversely.
     *
     * <div class="warning">In a future SIS version, the type of list elements may be changed
     * to {@code org.opengis.feature.Property}. This change is pending GeoAPI revision.</div>
     *
     * @param  name The property name.
     * @return All properties of the given name, or an empty list if none.
     * @throws IllegalArgumentException If the given argument is not a property name of this feature.
     */
    public List<DefaultAttribute<?>> properties(final String name) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("name", name);
        final DefaultAttributeType<?> at = type.getProperty(name);
        if (at == null) {
            throw new IllegalArgumentException(propertyNotFound(name));
        }
        /*
         * If the majority of cases, the feature allows at most one attribute for the given name.
         * In order to save a little bit of space (because SIS applications may have a very large
         * amount of features), we will not store the list in this DefaultFeature. Instead, we use
         * a temporary object which will read and write the Attribute instance directly in the map.
         */
        if (isSingleton(at.getCardinality())) {
            return new SingletonValue(at, properties, name);
        }
        /*
         * If the property allow more than one feature, then we need a real List implementation.
         */
        final Object element = properties.get(name);
        if (element == null) {
            // TODO: create MultiValues list here.
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the value(s) of all attribute of the given name.
     * This convenience method combines a call to {@link #properties(String)} followed by calls to
     * {@link DefaultAttribute#getValue()} for each attribute, but may potentially be more efficient.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If the {@linkplain DefaultAttributeType#getCardinality() attribute cardinality} allows
     *       more than one occurrence, then this method will always return a {@link List}.
     *       That list may be empty but never {@code null}.</li>
     *   <li>Otherwise (if the attribute cardinality allows at most one occurrence),
     *       this method returns either the singleton value or {@code null}.</li>
     * </ul>
     *
     * @param  name The attribute name.
     * @return The value or list of values for the given attribute(s), or {@code null} if none.
     * @throws IllegalArgumentException If the given argument is not an attribute name of this feature.
     *
     * @see DefaultAttribute#getValue()
     */
    public Object getAttributeValue(final String name) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("name", name);
        final Object element = properties.get(name);
        /*
         * If there is no value for the given name, first ensure that the name is valid,
         * then return the default value without storing any object in this DefaultFeature.
         */
        if (element == null) {
            final DefaultAttributeType<?> at = type.getProperty(name);
            if (at == null) {
                throw new IllegalArgumentException(propertyNotFound(name));
            }
            if (isSingleton(at.getCardinality())) {
                return at.getDefaultValue();
            }
            // TODO
        }
        // TODO: test MultiValues list here.
        if (element instanceof DefaultAttribute<?>) {
            return ((DefaultAttribute<?>) element).getValue();
        }
        return element;
    }

    /**
     * Sets the value of the attribute of the given name.
     *
     * {@section Validation}
     * The amount of validation performed by this method is implementation dependent.
     * The current {@code DefaultFeature} implementation performs only minimal validations.
     * A more exhaustive verification can be performed by invoking the {@link #validate()} method.
     *
     * @param  name  The attribute name.
     * @param  value The new value for the given attribute (may be {@code null}).
     * @throws IllegalArgumentException If the given argument is not an attribute name of this feature.
     * @throws RuntimeException If this method performs validation and the given value does not meet the conditions.
     *         <span style="color:firebrick">This exception will be changed to {@code IllegalAttributeException} in
     *         a future SIS version.</span>
     *
     * @see DefaultAttribute#setValue(Object)
     */
    public void setAttributeValue(final String name, final Object value) throws IllegalArgumentException {
        Object element = properties.get(name);
        if (element == null) {
            final DefaultAttributeType<?> at = type.getProperty(name);
            if (at == null) {
                throw new IllegalArgumentException(propertyNotFound(name));
            }
            if (Objects.equals(value, at.getDefaultValue())) {
                return; // Avoid creating the attribute if not necessary.
            }
            final DefaultAttribute<?> attribute = new DefaultAttribute<>(at);
            setAttributeValue(attribute, value);
            if (properties.put(name, attribute) != null) {
                throw new ConcurrentModificationException();
            }
        } else {
            // TODO: check for MultiValues list here.
            if (element instanceof DefaultAttribute<?>) {
                setAttributeValue((DefaultAttribute<?>) element, value);
            } else {
                if (properties.put(name, value) != element) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    /**
     * Sets the attribute value after verification of its type.
     */
    @SuppressWarnings("unchecked")
    private static <T> void setAttributeValue(final DefaultAttribute<T> attribute, final Object value) {
        if (value != null) {
            final DefaultAttributeType<T> at = attribute.getType();
            if (!at.getValueClass().isInstance(value)) {
                throw new RuntimeException( // TODO: use IllegalAttributeException after GeoAPI revision.
                        Errors.format(Errors.Keys.IllegalPropertyClass_2, at.getName(), value.getClass()));
            }
        }
        ((DefaultAttribute) attribute).setValue(value);
    }

    /**
     * Returns the error message for a property not found.
     */
    private String propertyNotFound(final String name) {
        return Errors.format(Errors.Keys.PropertyNotFound_2, type.getName(), name);
    }

    /**
     * Ensures that all current property values comply with the constraints defined by the feature type.
     * This method will implicitly invokes {@link DefaultAttribute#validate()} for all attribute values.
     *
     * @throws RuntimeException If the current attribute value violates a constraint.
     *         <span style="color:firebrick">This exception will be changed to {@code IllegalAttributeException}
     *         in a future SIS version.</span>
     *
     * @see DefaultAttribute#validate()
     */
    public void validate() {
        // TODO
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
        for (final Map.Entry<String,Object> entry : properties.entrySet()) {
            final DefaultAttributeType<?> at;
            Object element = entry.getValue();
            // TODO: check for MultiValues list here.
            if (element instanceof DefaultAttribute<?>) {
                at = ((DefaultAttribute<?>) element).getType();
                element = ((DefaultAttribute<?>) element).getValue();
            } else {
                at = type.getProperty(entry.getKey());
            }
            sb.append(at.getName()).append(": ").append(element).append(lineSeparator);
        }
        return sb.toString();
    }
}
