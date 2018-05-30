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
import java.util.Objects;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.io.Serializable;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;
import org.apache.sis.internal.feature.Resources;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.MultiValuedPropertyException;


/**
 * An attribute or association implementation which delegate its work to the parent feature.
 * This class is used for default implementation of {@link AbstractFeature#getProperty(String)}.
 *
 * <p><strong>This implementation is inefficient!</strong>
 * This class is for making easier to begin with a custom {@link AbstractFeature} implementation,
 * but developers are encouraged to provide their own {@link AbstractFeature#getProperty(String)}
 * implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
abstract class PropertyView<V> extends Field<V> implements Property, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5605415150581699255L;

    /**
     * The feature from which to read and where to write the attribute or association value.
     */
    final Feature feature;

    /**
     * The string representation of the property name. This is the value to be given in calls to
     * {@link Feature#getPropertyValue(String)} and {@link Feature#setPropertyValue(String, Object)}.
     */
    final String name;

    /**
     * Creates a new property which will delegate its work to the given feature.
     *
     * @param feature  the feature from which to read and where to write the property value.
     * @param name     the string representation of the property name.
     */
    PropertyView(final Feature feature, final String name) {
        this.feature = feature;
        this.name = name;
    }

    /**
     * Creates a new property which will delegate its work to the given feature.
     *
     * @param feature  the feature from which to read and where to write the property value.
     * @param type     the type of the property. Must be one of the properties listed in the
     *                 {@code feature} (this is not verified by this constructor).
     */
    static Property create(final Feature feature, final PropertyType type) {
        if (type instanceof AttributeType<?>) {
            return AttributeView.create(feature, (AttributeType<?>) type);
        } else if (type instanceof FeatureAssociationRole) {
            return AssociationView.create(feature, (FeatureAssociationRole) type);
        } else if (type instanceof Operation) {
            return ((Operation) type).apply(feature, null);
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownType_1, Classes.getClass(type)));
        }
    }

    /**
     * Returns the class of values.
     */
    abstract Class<V> getValueClass();

    /**
     * Returns the singleton value. This default implementation assumes that the property is multi-valued
     * (single-valued properties shall override this method), but we nevertheless provide a fallback for
     * non-{@code Iterable} values as a safety against implementations that are not strictly compliant
     * to our {@link Feature#getPropertyValue(String)} method contract. Then this method verifies that
     * the value is a collection containing zero or one element and returns that element or {@code null}.
     */
    @Override
    public V getValue() throws MultiValuedPropertyException {
        Object value = feature.getPropertyValue(name);
        if (value instanceof Iterable<?>) {
            final Iterator<?> it = ((Iterable<?>) value).iterator();
            if (!it.hasNext()) {
                return null;
            }
            value = it.next();
            if (it.hasNext()) {
                throw new MultiValuedPropertyException(Resources.format(Resources.Keys.NotASingleton_1, name));
            }
        }
        return getValueClass().cast(value);
    }

    /**
     * Sets the values of the given attribute. This default implementation assumes that the property
     * is multi-valued (single-valued properties shall override this method) and that the
     * {@link Feature#setPropertyValue(String, Object)} implementation will verify the argument type.
     */
    @Override
    public void setValue(final V value) {
        feature.setPropertyValue(name, singletonOrEmpty(value));
    }

    /**
     * Returns the given value as a singleton if non-null, or returns an empty list otherwise.
     *
     * @param  <V>      the element type.
     * @param  element  the element to returns in a collection if non-null.
     * @return a collection containing the given element if non-null, or an empty collection otherwise.
     */
    static <V> List<V> singletonOrEmpty(final V element) {
        return (element != null) ? Collections.singletonList(element) : Collections.emptyList();
    }

    /**
     * Returns the values as a collection. This method tries to verify that the collection
     * contains elements of the expected type, but this verification is not always possible.
     * Consequently this method may, sometime, be actually unsafe.
     */
    @Override
    @SuppressWarnings("unchecked")              // Actually not 100% safe, but we have done our best.
    public Collection<V> getValues() {
        final Object values = feature.getPropertyValue(name);
        if (values instanceof Collection<?>) {
            if (values instanceof CheckedContainer<?>) {
                final Class<?> expected = getValueClass();
                final Class<?> actual = ((CheckedContainer<?>) values).getElementType();
                if (expected != actual) {       // Really exact match, not Class.isAssignableFrom(Class).
                    throw new ClassCastException(Errors.format(Errors.Keys.UnexpectedTypeForReference_3, name, expected, actual));
                }
            }
            return (Collection<V>) values;
        } else {
            return singletonOrEmpty(getValueClass().cast(values));
        }
    }

    /**
     * Sets the values of the given attribute. This method assumes that the
     * {@link Feature#setPropertyValue(String, Object)} implementation will
     * verify the argument type.
     */
    @Override
    public final void setValues(final Collection<? extends V> values) {
        feature.setPropertyValue(name, values);
    }

    /**
     * Returns a hash code value for this property.
     */
    @Override
    public final int hashCode() {
        return Objects.hashCode(name) ^ System.identityHashCode(feature);
    }

    /**
     * Compares this attribute with the given object for equality.
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj.getClass() == getClass()) {
            final PropertyView<?> that = (PropertyView<?>) obj;
            return feature == that.feature && Objects.equals(name, that.name);
        }
        return false;
    }

    /**
     * Returns a string representation of this property for debugging purposes.
     */
    @Override
    public final String toString() {
        return FieldType.toString(false, getClass().getSimpleName(), getName(),
                Classes.getShortName(getValueClass()), getValues().iterator()).toString();
    }
}
