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
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.DataQuality;
import org.apache.sis.internal.util.Cloner;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A feature in which only a small fraction of properties are expected to be provided. This implementation uses
 * a {@link Map} for its internal storage of properties. This consumes less memory than a plain array when we
 * know that the array may be long and likely to be full of {@code null} values.
 *
 * @author  Travis L. Pinney
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 *
 * @see DenseFeature
 * @see DefaultFeatureType
 */
final class SparseFeature extends AbstractFeature implements Cloneable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2954323576287152427L;

    /**
     * A {@link #valuesKind} flag meaning that the {@link #properties} map contains raw values.
     */
    private static final byte VALUES = 0; // Must be zero, because we want it to be 'valuesKind' default value.

    /**
     * A {@link #valuesKind} flag meaning that the {@link #properties} map contains {@link Property} instances.
     */
    private static final byte PROPERTIES = 1;

    /**
     * A {@link #valuesKind} flag meaning that the {@link #properties} map is invalid.
     */
    private static final byte CORRUPTED = 2;

    /**
     * The map of property names to keys in the {@link #properties} map. This map is a reference to the
     * {@link DefaultFeatureType#indices} map (potentially shared by many feature instances) and shall
     * not be modified.
     *
     * <p>We use those indices as {@link #properties} keys instead than using directly the property names
     * in order to resolve aliases.</p>
     */
    private final Map<String, Integer> indices;

    /**
     * The properties (attributes or feature associations) in this feature.
     *
     * Conceptually, values in this map are {@link Property} instances. However at first we will store
     * only the property <em>values</em>, and build the full {@code Property} objects only if they are
     * requested. The intend is to reduce the amount of allocated objects as much as possible, because
     * typical SIS applications may create a very large amount of features.
     *
     * @see #valuesKind
     */
    private HashMap<Integer, Object> properties;

    /**
     * {@link #PROPERTIES} if the values in the {@link #properties} map are {@link Property} instances,
     * or {@link #VALUES} if the map contains only the "raw" property values.
     *
     * <p>This field is initially {@code VALUES}, and will be set to {@code PROPERTIES} only if at least
     * one {@code Property} instance has been requested. In such case, all property values will have been
     * wrapped into their appropriate {@code Property} instance.</p>
     */
    private byte valuesKind;

    /**
     * Creates a new feature of the given type.
     *
     * @param type Information about the feature (name, characteristics, <i>etc.</i>).
     */
    public SparseFeature(final DefaultFeatureType type) {
        super(type);
        indices = type.indices();
        properties = new HashMap<Integer, Object>();
    }

    /**
     * Returns the index for the property of the given name, or {@link DefaultFeatureType#OPERATION_INDEX}
     * if the property is a parameterless operation.
     *
     * @param  name The property name.
     * @return The index for the property of the given name,
     *         or a negative value if the property is a parameterless operation.
     * @throws IllegalArgumentException If the given argument is not a property name of this feature.
     */
    private int getIndex(final String name) throws IllegalArgumentException {
        final Integer index = indices.get(name);
        if (index != null) {
            return index;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, getName(), name));
    }

    /**
     * Returns the property name at the given index.
     * Current implementation is inefficient, but this method should rarely be invoked.
     */
    private String nameOf(final Integer index) {
        for (final Map.Entry<String, Integer> entry : indices.entrySet()) {
            if (index.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        // Should never reach this point.
        throw new AssertionError(index);
    }

    /**
     * Ensures that the {@link #properties} map contains {@link Property} instances instead than
     * property values. The conversion, if needed, will be performed at most once per feature.
     */
    private void requireMapOfProperties() {
        if (valuesKind != PROPERTIES) {
            if (!properties.isEmpty()) { // The map is typically empty when this method is first invoked.
                if (valuesKind != VALUES) {
                    throw new CorruptedObjectException(getName());
                }
                valuesKind = CORRUPTED;
                for (final Map.Entry<Integer, Object> entry : properties.entrySet()) {
                    final String key = nameOf(entry.getKey());
                    final Object value = entry.getValue();
                    if (entry.setValue(createProperty(key, value)) != value) {
                        throw new ConcurrentModificationException(key);
                    }
                }
            }
            valuesKind = PROPERTIES; // Set only on success.
        }
    }

    /**
     * Returns the property (attribute, operation or association) of the given name.
     *
     * @param  name The property name.
     * @return The property of the given name.
     * @throws IllegalArgumentException If the given argument is not a property name of this feature.
     */
    @Override
    public Object getProperty(final String name) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("name", name);
        requireMapOfProperties();
        return getPropertyInstance(name);
    }

    /**
     * Implementation of {@link #getProperty(String)} invoked when we know that the {@link #properties}
     * map contains {@code Property} instances (as opposed to their value).
     */
    private Property getPropertyInstance(final String name) throws IllegalArgumentException {
        assert valuesKind == PROPERTIES : valuesKind;
        final Integer index = getIndex(name);
        if (index < 0) {
            return (Property) getOperationResult(name);
        }
        Property property = (Property) properties.get(index);
        if (property == null) {
            property = createProperty(name);
            replace(index, null, property);
        }
        return property;
    }

    /**
     * Sets the property (attribute, operation or association).
     *
     * @param  property The property to set.
     * @throws IllegalArgumentException if the type of the given property is not one of the types
     *         known to this feature, or if the property can not be set for another reason.
     */
    @Override
    public void setProperty(final Object property) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("property", property);
        final String name = ((Property) property).getName().toString();
        verifyPropertyType(name, (Property) property);
        requireMapOfProperties();
        /*
         * Following index should never be OPERATION_INDEX (a negative value) because the call
         * to 'verifyPropertyType(name, property)' shall have rejected all Operation types.
         */
        properties.put(indices.get(name), property);
    }

    /**
     * Returns the value for the property of the given name.
     *
     * @param  name The property name.
     * @return The value for the given property, or {@code null} if none.
     * @throws IllegalArgumentException If the given argument is not an attribute or association name of this feature.
     */
    @Override
    public Object getPropertyValue(final String name) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("name", name);
        final Integer index = getIndex(name);
        if (index < 0) {
            return getOperationValue(name);
        }
        final Object element = properties.get(index);
        if (element != null) {
            if (valuesKind == VALUES) {
                return element; // Most common case.
            } else if (element instanceof AbstractAttribute<?>) {
                return getAttributeValue((AbstractAttribute<?>) element);
            } else if (element instanceof AbstractAssociation) {
                return getAssociationValue((AbstractAssociation) element);
            } else if (valuesKind == PROPERTIES) {
                throw unsupportedPropertyType(((Property) element).getName());
            } else {
                throw new CorruptedObjectException(getName());
            }
        } else if (properties.containsKey(index)) {
            return null; // Null has been explicitely set.
        } else {
            return getDefaultValue(name);
        }
    }

    /**
     * Sets the value for the property of the given name.
     *
     * @param  name  The attribute name.
     * @param  value The new value for the given attribute (may be {@code null}).
     * @throws ClassCastException If the value is not assignable to the expected value class.
     * @throws IllegalArgumentException If the given value can not be assigned for another reason.
     */
    @Override
    public void setPropertyValue(final String name, final Object value) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("name", name);
        final Integer index = getIndex(name);
        if (index < 0) {
            setOperationValue(name, value);
            return;
        }
        if (valuesKind == VALUES) {
            final Object previous = properties.put(index, value);
            /*
             * Slight optimization:  if we replaced a previous value of the same class, then we can skip the
             * checks for name and type validity since those checks have been done previously. But if we add
             * a new value or a value of a different type, then we need to check the name and type validity.
             */
            if (!canSkipVerification(previous, value)) {
                Object toStore = previous; // This initial value will restore the previous value if the check fail.
                try {
                    toStore = verifyPropertyValue(name, value);
                } finally {
                    if (toStore != value) {
                        replace(index, value, toStore);
                    }
                }
            }
        } else if (valuesKind == PROPERTIES) {
            setPropertyValue(getPropertyInstance(name), value);
        } else {
            throw new CorruptedObjectException(getName());
        }
    }

    /**
     * Sets a value in the {@link #properties} map.
     *
     * @param index    The key of the property to set.
     * @param oldValue The old value, used for verification purpose.
     * @param newValue The new value.
     */
    private void replace(final Integer index, final Object oldValue, final Object newValue) {
        if (properties.put(index, newValue) != oldValue) {
            throw new ConcurrentModificationException(nameOf(index));
        }
    }

    /**
     * Verifies if all current properties met the constraints defined by the feature type. This method returns
     * {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality#getReports() reports} for all invalid
     * properties, if any.
     */
    @Override
    public DataQuality quality() {
        if (valuesKind == VALUES) {
            final Validator v = new Validator(ScopeCode.FEATURE);
            for (final Map.Entry<String, Integer> entry : indices.entrySet()) {
                v.validateAny(type.getProperty(entry.getKey()), properties.get(entry.getValue()));
            }
            return v.quality;
        }
        /*
         * Slower path when there is a possibility that user overridden the Property.quality() methods.
         */
        return super.quality();
    }

    /**
     * Returns a copy of this feature
     * This method clones also all {@linkplain Cloneable cloneable} property instances in this feature,
     * but not necessarily property values. Whether the property values are cloned or not (i.e. whether
     * the clone operation is <cite>deep</cite> or <cite>shallow</cite>) depends on the behavior or
     * property {@code clone()} methods.
     *
     * @return A clone of this attribute.
     * @throws CloneNotSupportedException if this feature can not be cloned, typically because
     *         {@code clone()} on a property instance failed.
     */
    @Override
    @SuppressWarnings("unchecked")
    public SparseFeature clone() throws CloneNotSupportedException {
        final SparseFeature clone = (SparseFeature) super.clone();
        clone.properties = (HashMap<Integer,Object>) clone.properties.clone();
        switch (clone.valuesKind) {
            default:        throw new AssertionError(clone.valuesKind);
            case CORRUPTED: throw new CorruptedObjectException(clone.getName());
            case VALUES:    break; // Nothing to do.
            case PROPERTIES: {
                final Cloner cloner = new Cloner();
                for (final Map.Entry<Integer,Object> entry : clone.properties.entrySet()) {
                    final Property property = (Property) entry.getValue();
                    if (property instanceof Cloneable) {
                        entry.setValue(cloner.clone(property));
                    }
                }
                break;
            }
        }
        return clone;
    }

    /**
     * Returns a hash code value for this feature.
     * This implementation computes the hash code using only the property values, not the {@code Property} instances,
     * in order to keep the hash code value stable before and after the {@code properties} map is (conceptually)
     * promoted from the {@code Map<Integer,Object>} type to the {@code Map<Integer,Property>} type.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        int code = type.hashCode() * 37;
        if (valuesKind == PROPERTIES) {
            for (final Map.Entry<Integer,Object> entry : properties.entrySet()) {
                final Object p = entry.getValue();
                final Object value;
                if (p instanceof AbstractAttribute<?>) {
                    value = getAttributeValue((AbstractAttribute<?>) p);
                } else if (p instanceof AbstractAssociation) {
                    value = getAssociationValue((AbstractAssociation) p);
                } else {
                    value = null;
                }
                code += Objects.hashCode(entry.getKey()) ^ Objects.hashCode(value);
            }
        } else {
            code += properties.hashCode();
        }
        return code;
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
        if (obj instanceof SparseFeature) {
            final SparseFeature that = (SparseFeature) obj;
            if (type.equals(that.type)) {
                final boolean asProperties = (valuesKind == PROPERTIES);
                if (asProperties != (that.valuesKind == PROPERTIES)) {
                    if (asProperties) {
                        that.requireMapOfProperties();
                    } else {
                        requireMapOfProperties();
                    }
                }
                return properties.equals(that.properties);
            }
        }
        return false;
    }
}
