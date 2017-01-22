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
import org.opengis.util.GenericName;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.internal.feature.Resources;

// Branch-dependent imports


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
 * @since   0.8
 * @version 0.8
 * @module
 */
final class PropertyView {
    /**
     * Do not allow instantiation of this class.
     */
    private PropertyView() {
    }

    /**
     * Creates a new property which will delegate its work to the given feature.
     *
     * @param feature  the feature from which to read and where to write the property value.
     * @param type     the type of the property. Must be one of the properties listed in the
     *                 {@code feature} (this is not verified by this constructor).
     */
    static Property create(final AbstractFeature feature, final AbstractIdentifiedType type) {
        if (type instanceof DefaultAttributeType<?>) {
            return AttributeView.create(feature, (DefaultAttributeType<?>) type);
        } else if (type instanceof DefaultAssociationRole) {
            return AssociationView.create(feature, (DefaultAssociationRole) type);
        } else if (type instanceof AbstractOperation) {
            return (Property) ((AbstractOperation) type).apply(feature, null);
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownType_1, Classes.getClass(type)));
        }
    }

    /**
     * Returns the singleton value. This default implementation assumes that the property is multi-valued
     * (single-valued properties shall override this method), but we nevertheless provide a fallback for
     * non-{@code Iterable} values as a safety against implementations that are not strictly compliant
     * to our {@link Feature#getPropertyValue(String)} method contract. Then this method verifies that
     * the value is a collection containing zero or one element and returns that element or {@code null}.
     */
    static Object getValue(final AbstractFeature feature, final String name) {
        Object value = feature.getPropertyValue(name);
        if (value instanceof Iterable<?>) {
            final Iterator<?> it = ((Iterable<?>) value).iterator();
            if (!it.hasNext()) {
                return null;
            }
            value = it.next();
            if (it.hasNext()) {
                throw new IllegalStateException(Resources.format(Resources.Keys.NotASingleton_1, name));
            }
        }
        return value;
    }

    /**
     * Sets the values of the given attribute. This default implementation assumes that the property
     * is multi-valued (single-valued properties shall override this method) and that the
     * {@link Feature#setPropertyValue(String, Object)} implementation will verify the argument type.
     */
    static void setValue(final AbstractFeature feature, final String name, final Object value) {
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
        return (element != null) ? Collections.singletonList(element) : Collections.<V>emptyList();
    }

    /**
     * Returns the values as a collection. This method tries to verify that the collection
     * contains elements of the expected type, but this verification is not always possible.
     * Consequently this method may, sometime, be actually unsafe.
     */
    @SuppressWarnings("unchecked")              // Actually not 100% safe, but we have done our best.
    static <V> Collection<V> getValues(final AbstractFeature feature, final String name, final Class<V> expected) {
        final Object values = feature.getPropertyValue(name);
        if (values instanceof Collection<?>) {
            if (values instanceof CheckedContainer<?>) {
                final Class<?> actual = ((CheckedContainer<?>) values).getElementType();
                if (expected != actual) {       // Really exact match, not Class.isAssignableFrom(Class).
                    throw new ClassCastException(Errors.format(Errors.Keys.UnexpectedTypeForReference_3, name, expected, actual));
                }
            }
            return (Collection<V>) values;
        } else {
            return singletonOrEmpty(expected.cast(values));
        }
    }

    /**
     * Sets the values of the given attribute. This method assumes that the
     * {@link Feature#setPropertyValue(String, Object)} implementation will
     * verify the argument type.
     */
    static void setValues(final AbstractFeature feature, final String name, final Collection<?> values) {
        feature.setPropertyValue(name, values);
    }

    /**
     * Returns a hash code value for this property.
     */
    static int hashCode(final AbstractFeature feature, final String name) {
        return Objects.hashCode(name) ^ System.identityHashCode(feature);
    }

    /**
     * Returns a string representation of this property for debugging purposes.
     */
    @Debug
    static String toString(final Class<?> classe, final Class<?> valueClass, final GenericName name, final Collection<?> values) {
        return FieldType.toString(false, classe.getSimpleName(), name,
                Classes.getShortName(valueClass), values.iterator()).toString();
    }
}
