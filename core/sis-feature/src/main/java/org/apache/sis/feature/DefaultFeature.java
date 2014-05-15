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
import java.util.ConcurrentModificationException;
import java.io.Serializable;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;

// Related to JDK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * An instance of a {@linkplain DefaultFeatureType feature type} containing values for a real-world phenomena.
 * Each feature instance can provide values for the following properties:
 *
 * <ul>
 *   <li>{@linkplain DefaultAttribute   Attributes}</li>
 *   <li>{@linkplain DefaultAssociation Associations to other features}</li>
 *   <li>{@linkplain DefaultOperation   Operations}</li>
 * </ul>
 *
 * {@section Simple features}
 * A feature is said “simple” if it complies to the following conditions:
 * <ul>
 *   <li>the feature allows only attributes and operations (no associations),</li>
 *   <li>the cardinality of all attributes is constrained to [1 … 1].</li>
 * </ul>
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
 *
 * @see DefaultFeatureType
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
     *
     * Conceptually, values in this map are {@link Property} instances. However at first we will store
     * only the property <em>values</em>, and build the full {@code Property} objects only if they are
     * requested. The intend is to reduce the amount of allocated objects as much as possible, because
     * typical SIS applications may create a very large amount of features.
     *
     * @see #propertiesAreInstantiated
     */
    private final Map<String, Object> properties;

    /**
     * {@code true} if the values in the {@link #properties} map are {@link Property} instances,
     * or {@code false} if the map contains only the "raw" property values.
     *
     * <p>This field is initially {@code false}, and will be set to {@code true} only if at least one
     * {@code Property} instance has been requested. In such case, all property values will have been
     * wrapped into their appropriate {@code Property} instance.</p>
     */
    private boolean propertiesAreInstantiated;

    /**
     * Creates a new feature of the given type.
     *
     * @param type Information about the feature (name, characteristics, <i>etc.</i>).
     */
    public DefaultFeature(final DefaultFeatureType type) {
        ArgumentChecks.ensureNonNull("type", type);
        this.type = type;
        properties = new HashMap<String,Object>(Math.min(16, Containers.hashMapCapacity(type.getInstanceSize())));
    }

    /**
     * Returns information about the feature (name, characteristics, <i>etc.</i>).
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.FeatureType}. This change is pending GeoAPI revision.</div>
     *
     * @return Information about the feature.
     */
    public DefaultFeatureType getType() {
        return type;
    }

    /**
     * Returns the type for the property of the given name.
     *
     * @param  name The property name.
     * @return The type for the property of the given name (never {@code null}).
     * @throws IllegalArgumentException If the given argument is not a property name of this feature.
     */
    private PropertyType getPropertyType(final String name) throws IllegalArgumentException {
        final PropertyType pt = type.getProperty(name);
        if (pt != null) {
            return pt;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, type.getName(), name));
    }

    /**
     * Returns the property (attribute, operation or association) of the given name.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.Property}. This change is pending GeoAPI revision.</div>
     *
     * @param  name The property name.
     * @return The property of the given name.
     * @throws IllegalArgumentException If the given argument is not a property name of this feature.
     */
    public Object getProperty(final String name) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("name", name);
        /*
         * Wraps values in Property objects for all entries in the properties map,
         * if not already done. This operation is execute at most once per feature.
         */
        if (!propertiesAreInstantiated) {
            propertiesAreInstantiated = true;
            if (!properties.isEmpty()) { // The map is typically empty when this method is first invoked.
                for (final Map.Entry<String, Object> entry : properties.entrySet()) {
                    final String key      = entry.getKey();
                    final Object value    = entry.getValue();
                    final PropertyType pt = getPropertyType(key);
                    final Property property;
                    if (pt instanceof DefaultAttributeType<?>) {
                        property = new DefaultAttribute((DefaultAttributeType<?>) pt, value);
                    } else {
                        throw new UnsupportedOperationException(); // TODO
                    }
                    if (entry.setValue(property) != value) {
                        throw new ConcurrentModificationException(key);
                    }
                }
            }
        }
        return getPropertyInstance(name);
    }

    /**
     * Implementation of {@link #getProperty(String)} invoked when we know that the {@link #properties}
     * map contains {@code Property} instances (as opposed to their value).
     */
    private Property getPropertyInstance(final String name) throws IllegalArgumentException {
        Property property = (Property) properties.get(name);
        if (property == null) {
            final PropertyType pt = getPropertyType(name);
            if (pt instanceof DefaultAttributeType<?>) {
                property = new DefaultAttribute((DefaultAttributeType<?>) pt);
            } else {
                throw new UnsupportedOperationException(); // TODO
            }
            replace(name, null, property);
        }
        return property;
    }

    /**
     * Returns the value for the property of the given name.
     * This convenience method is equivalent to the following code,
     * except that this method is potentially more efficient:
     *
     * {@preformat java
     *     return getProperty(name).getValue();
     * }
     *
     * @param  name The property name.
     * @return The value for the given property, or {@code null} if none.
     * @throws IllegalArgumentException If the given argument is not a property name of this feature.
     *
     * @see DefaultAttribute#getValue()
     */
    public Object getPropertyValue(final String name) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("name", name);
        final Object element = properties.get(name);
        if (element != null) {
            if (!propertiesAreInstantiated) {
                return element;
            } else {
                return ((DefaultAttribute<?>) element).getValue();
            }
        } else if (properties.containsKey(name)) {
            return null; // Null has been explicitely set.
        } else {
            final PropertyType pt = getPropertyType(name);
            if (pt instanceof DefaultAttributeType<?>) {
                return ((DefaultAttributeType<?>) pt).getDefaultValue();
            } else {
                throw new UnsupportedOperationException(); // TODO
            }
        }
    }

    /**
     * Sets the value for the property of the given name.
     *
     * {@section Validation}
     * The amount of validation performed by this method is implementation dependent.
     * Usually, only the most basic constraints are verified. This is so for performance reasons
     * and also because some rules may be temporarily broken while constructing a feature.
     * A more exhaustive verification can be performed by invoking the {@link #validate()} method.
     *
     * @param  name  The attribute name.
     * @param  value The new value for the given attribute (may be {@code null}).
     * @throws ClassCastException If the value is not assignable to the expected value class.
     * @throws IllegalArgumentException If the given value can not be assigned for an other reason.
     *
     * @see DefaultAttribute#setValue(Object)
     */
    public void setPropertyValue(final String name, final Object value) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("name", name);
        if (!propertiesAreInstantiated) {
            final Object previous = properties.put(name, value);
            /*
             * Slight optimisation: if we replaced a previous value of the same class, then we can skip the
             * checks for name and type validity since those checks has been done previously. But if we add
             * a new value or a value of a different type, then we need to check the name and type validity.
             */
            if (previous == null || (value != null && previous.getClass() != value.getClass())) {
                final PropertyType pt = type.getProperty(name);
                if (!isValidAttributeValue(pt, value)) {
                    replace(name, value, previous); // Restore the previous value.
                    if (pt == null) {
                        throw new IllegalArgumentException(Errors.format(
                                Errors.Keys.PropertyNotFound_2, type.getName(), name));
                    } else {
                        throw new ClassCastException(Errors.format(
                                Errors.Keys.IllegalPropertyClass_2, name, value.getClass()));
                    }
                }
            }
        } else {
            final Property property = getPropertyInstance(name);
            if (property instanceof DefaultAttribute<?>) {
                setAttributeValue((DefaultAttribute<?>) property, value);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Sets a value in the {@link #properties} map.
     *
     * @param name     The name of the property to set.
     * @param oldValue The old value, used for verification purpose.
     * @param newValue The new value.
     */
    private void replace(final String name, final Object oldValue, final Object newValue) {
        if (properties.put(name, newValue) != oldValue) {
            throw new ConcurrentModificationException(name);
        }
    }

    /**
     * Returns {@code true} if the given type is an attribute type, and if the given value
     * is valid for that attribute type.
     */
    private static boolean isValidAttributeValue(final PropertyType type, final Object value) {
        if (!(type instanceof DefaultAttributeType<?>)) {
            return false;
        }
        if (value == null) {
            return true;
        }
        return ((DefaultAttributeType<?>) type).getValueClass().isInstance(value);
    }

    /**
     * Sets the attribute value after verification of its type. This method is invoked only for checking
     * that we are not violating the Java parameterized type contract. For a more exhaustive validation,
     * use {@link Validator} instead.
     */
    @SuppressWarnings("unchecked")
    private static <T> void setAttributeValue(final DefaultAttribute<T> attribute, final Object value) {
        if (value != null) {
            final DefaultAttributeType<T> pt = attribute.getType();
            if (!pt.getValueClass().isInstance(value)) {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalPropertyClass_2,
                        pt.getName(), value.getClass()));
            }
        }
        ((DefaultAttribute) attribute).setValue(value);
    }

    /**
     * Verifies if all current properties met the constraints defined by the feature type.
     * This method returns {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality#getReports()
     * reports} for all constraint violations found, if any.
     *
     * <div class="note"><b>Example:</b> given a feature with an attribute named “population”.
     * If this attribute is mandatory ([1 … 1] cardinality) but no value has been assigned to it,
     * then this {@code validate()} method will return the following data quality report:
     *
     * {@preformat text
     *   Data quality
     *     ├─Scope
     *     │   └─Level………………………………………………… Feature
     *     └─Report
     *         ├─Measure identification
     *         │   └─Code………………………………………… population
     *         ├─Evaluation method type…… Direct internal
     *         └─Result
     *             ├─Explanation……………………… Missing value for “population” property.
     *             └─Pass………………………………………… false
     * }
     * </div>
     *
     * This feature is valid if this method does not report any
     * {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult conformance result} having a
     * {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult#pass() pass} value of {@code false}.
     *
     * @return Reports on all constraint violations found.
     *
     * @see DefaultAttribute#validate()
     * @see DefaultAssociation#validate()
     */
    /*
     * API NOTE: this method is final for now because if we allowed users to override it, users would
     * expect their method to be invoked by DefaultAssociation.validate(). But this is not yet the case.
     */
    public final DataQuality validate() {
        final Validator v = new Validator(ScopeCode.FEATURE);
        for (final Map.Entry<String, Object> entry : properties.entrySet()) {
            v.validateAny(getPropertyType(entry.getKey()), entry.getValue());
        }
        return v.quality;
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
        if (obj != null && obj.getClass() == getClass()) {
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
        final String lineSeparator = JDK7.lineSeparator();
        for (final Map.Entry<String,Object> entry : properties.entrySet()) {
            final PropertyType pt;
            Object element = entry.getValue();
            if (propertiesAreInstantiated) {
                pt = ((DefaultAttribute<?>) element).getType();
                element = ((DefaultAttribute<?>) element).getValue();
            } else {
                pt = type.getProperty(entry.getKey());
            }
            sb.append(pt.getName()).append(": ").append(element).append(lineSeparator);
        }
        return sb.toString();
    }
}
