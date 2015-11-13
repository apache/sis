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
import java.util.Arrays;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.DataQuality;
import org.apache.sis.internal.util.Cloner;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A feature in which most properties are expected to be provided. This implementation uses a plain array for
 * its internal storage of properties. This consumes less memory than {@link java.util.Map} when we know that
 * all (or almost all) elements in the array will be assigned a value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Marc le Bihan
 * @since   0.5
 * @version 0.6
 * @module
 *
 * @see SparseFeature
 * @see DefaultFeatureType
 */
final class DenseFeature extends AbstractFeature implements Cloneable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2041120433733230588L;

    /**
     * The map of property names to indices in the {@link #properties} array. This map is a reference to the
     * {@link DefaultFeatureType#indices} map (potentially shared by many feature instances) and shall not be
     * modified.
     */
    private final Map<String, Integer> indices;

    /**
     * The properties (attributes or feature associations) in this feature.
     *
     * Conceptually, values in this array are {@link Property} instances. However at first we will store only
     * the property <em>values</em>, and convert to an array of type {@code Property[]} only when at least one
     * property is requested. The intend is to reduce the amount of allocated objects as much as possible,
     * because typical SIS applications may create a very large amount of features.
     */
    private Object[] properties;

    /**
     * Creates a new feature of the given type.
     *
     * @param type Information about the feature (name, characteristics, <i>etc.</i>).
     */
    public DenseFeature(final DefaultFeatureType type) {
        super(type);
        indices = type.indices();
    }

    /**
     * Returns the index for the property of the given name, or {@link DefaultFeatureType#OPERATION_INDEX}
     * if the property is a parameterless operation.
     *
     * @param  name The property name.
     * @return The index for the property of the given name,
     *         or a negative value if the property is a parameterless operation.
     * @throws IllegalArgumentException if the given argument is not a property name of this feature.
     */
    private int getIndex(final String name) throws IllegalArgumentException {
        final Integer index = indices.get(name);
        if (index != null) {
            return index;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, getName(), name));
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
        final int index = getIndex(name);
        if (index < 0) {
            return getOperationResult(name);
        }
        /*
         * Are the Property instances currently initialized? If not, wrap the values we can find.
         * This is a all-or-nothing converion (we do not wrap only the requested property)
         * for avoiding the additional complexity of remembering which values were wrapped.
         */
        if (!(properties instanceof Property[])) {
            wrapValuesInProperties();
        }
        /*
         * Find the wanted property. If the property still have a null value, we create it from its type.
         */
        Property property = ((Property[]) properties)[index];
        if (property == null) {
            property = createProperty(name);
            properties[index] = property;
        }
        return property;
    }

    /**
     * Sets the property (attribute, operation or association).
     *
     * @param  property The property to set.
     * @throws IllegalArgumentException if the type of the given property is not one of the types
     *         known to this feature, or if the property can not be set or another reason.
     */
    @Override
    public void setProperty(final Object property) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("property", property);
        final String name = ((Property) property).getName().toString();
        verifyPropertyType(name, (Property) property);
        if (!(properties instanceof Property[])) {
            wrapValuesInProperties();
        }
        /*
         * Following index should never be OPERATION_INDEX (a negative value) because the call
         * to 'verifyPropertyType(name, property)' shall have rejected all Operation types.
         */
        properties[indices.get(name)] = property;
    }

    /**
     * Wraps values in {@code Property} objects for all elements in the {@link #properties} array.
     * This operation is executed at most once per feature.
     */
    private void wrapValuesInProperties() {
        final Property[] c = new Property[indices.size()];
        if (properties != null) {
            assert c.length == properties.length;
            for (final Map.Entry<String, Integer> entry : indices.entrySet()) {
                final int index = entry.getValue();
                if (index >= 0) {
                    final Object value = properties[index];
                    if (value != null) {
                        c[index] = createProperty(entry.getKey(), value);
                    }
                }
            }
        }
        properties = c;     // Store only on success.
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
        final int index = getIndex(name);
        if (index < 0) {
            return getOperationValue(name);
        }
        if (properties != null) {
            final Object element = properties[index];
            if (element != null) {
                if (!(properties instanceof Property[])) {
                    return element; // Most common case.
                } else if (element instanceof AbstractAttribute<?>) {
                    return getAttributeValue((AbstractAttribute<?>) element);
                } else if (element instanceof AbstractAssociation) {
                    return getAssociationValue((AbstractAssociation) element);
                } else {
                    throw unsupportedPropertyType(((Property) element).getName());
                }
            }
        }
        return getDefaultValue(name);
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
    public void setPropertyValue(final String name, Object value) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("name", name);
        final int index = getIndex(name);
        if (index < 0) {
            setOperationValue(name, value);
            return;
        }
        if (properties == null) {
            final int n = indices.size();
            properties = (value != null) ? new Object[n] : new Property[n];
        }
        if (!(properties instanceof Property[])) {
            if (value != null) {
                if (!canSkipVerification(properties[index], value)) {
                    value = verifyPropertyValue(name, value);
                }
                properties[index] = value;
                return;
            } else {
                wrapValuesInProperties();
            }
        }
        Property property = ((Property[]) properties)[index];
        if (property == null) {
            property = createProperty(name);
            properties[index] = property;
        }
        setPropertyValue(property, value);
    }

    /**
     * Verifies if all current properties met the constraints defined by the feature type. This method returns
     * {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality#getReports() reports} for all invalid
     * properties, if any.
     */
    @Override
    public DataQuality quality() {
        if (properties != null && !(properties instanceof Property[])) {
            final Validator v = new Validator(ScopeCode.FEATURE);
            for (final Map.Entry<String, Integer> entry : indices.entrySet()) {
                v.validateAny(type.getProperty(entry.getKey()), properties[entry.getValue()]);
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
    public DenseFeature clone() throws CloneNotSupportedException {
        final DenseFeature clone = (DenseFeature) super.clone();
        clone.properties = clone.properties.clone();
        if (clone.properties instanceof Property[]) {
            final Property[] p = (Property[]) clone.properties;
            final Cloner cloner = new Cloner();
            for (int i=0; i<p.length; i++) {
                final Property property = p[i];
                if (property instanceof Cloneable) {
                    p[i] = (Property) cloner.clone(property);
                }
            }
        }
        return clone;
    }

    /**
     * Returns a hash code value for this feature.
     * This implementation computes the hash code using only the property values, not the {@code Property} instances,
     * in order to keep the hash code value stable before and after the {@code properties} array is promoted from the
     * {@code Object[]} type to the {@code Property[]} type.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        int code = 1;
        if (properties != null) {
            if (properties instanceof Property[]) {
                for (final Property p : (Property[]) properties) {
                    code = 31 * code;
                    final Object value;
                    if (p instanceof AbstractAttribute<?>) {
                        value = getAttributeValue((AbstractAttribute<?>) p);
                    } else if (p instanceof AbstractAssociation) {
                        value = getAssociationValue((AbstractAssociation) p);
                    } else {
                        continue;
                    }
                    if (value != null) {
                        code += value.hashCode();
                    }
                }
            } else {
                code = Arrays.hashCode(properties);
            }
        }
        return type.hashCode() + code;
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
        if (obj instanceof DenseFeature) {
            final DenseFeature that = (DenseFeature) obj;
            if (type.equals(that.type)) {
                final boolean asProperties = (properties instanceof Property[]);
                if (asProperties != (that.properties instanceof Property[])) {
                    if (asProperties) {
                        that.wrapValuesInProperties();
                    } else {
                        wrapValuesInProperties();
                    }
                }
                return Arrays.equals(properties, that.properties);
            }
        }
        return false;
    }
}
