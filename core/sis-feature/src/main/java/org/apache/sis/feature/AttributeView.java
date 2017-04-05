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
import java.util.Collection;
import java.util.Collections;
import org.opengis.util.GenericName;

// Branch-dependent imports
import java.util.Objects;


/**
 * An attribute implementation which delegate its work to the parent feature.
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
class AttributeView<V> extends AbstractAttribute<V> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3617999929561826634L;

    /**
     * The feature from which to read and where to write the attribute or association value.
     */
    final AbstractFeature feature;

    /**
     * The string representation of the property name. This is the value to be given in calls to
     * {@code Feature.getPropertyValue(String)} and {@code Feature.setPropertyValue(String, Object)}.
     */
    final String name;

    /**
     * Creates a new attribute which will delegate its work to the given feature.
     */
    private AttributeView(final AbstractFeature feature, final DefaultAttributeType<V> type) {
        super(type);
        this.feature = feature;
        this.name = type.getName().toString();
    }

    /**
     * Creates a new attribute which will delegate its work to the given feature.
     *
     * @param feature  the feature from which to read and where to write the attribute value.
     * @param type     the type of this attribute. Must be one of the properties listed in the
     *                 {@link #feature} (this is not verified by this constructor).
     */
    static <V> AbstractAttribute<V> create(final AbstractFeature feature, final DefaultAttributeType<V> type) {
        if (isSingleton(type.getMaximumOccurs())) {
            return new Singleton<>(feature, type);
        } else {
            return new AttributeView<>(feature, type);
        }
    }

    /**
     * Returns the name of the type specified at construction time.
     */
    @Override
    public final GenericName getName() {
        return type.getName();
    }

    @Override
    public V getValue() {
        return type.getValueClass().cast(PropertyView.getValue(feature, name));
    }

    @Override
    public void setValue(final V value) {
        PropertyView.setValue(feature, name, value);
    }

    @Override
    public Collection<V> getValues() {
        return PropertyView.getValues(feature, name, type.getValueClass());
    }

    @Override
    public final void setValues(final Collection<? extends V> values) {
        PropertyView.setValues(feature, name, values);
    }

    /**
     * Returns an empty map since this simple view does not support characteristics.
     */
    @Override
    public final Map<String,AbstractAttribute<?>> characteristics() {
        return Collections.emptyMap();
    }

    /**
     * Specialization of {@code AttributeView} when the amount of values can be only zero or one.
     * This implementation takes shortcuts for the {@code getValue()} and {@code getValues()} methods.
     * This specialization is provided because it is the most common case.
     */
    private static final class Singleton<V> extends AttributeView<V> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -808239726590009163L;

        /**
         * Creates a new attribute which will delegate its work to the given feature.
         */
        Singleton(final AbstractFeature feature, final DefaultAttributeType<V> type) {
            super(feature, type);
        }

        /**
         * Returns the single value, or {@code null} if none.
         */
        @Override
        public V getValue() {
            return this.type.getValueClass().cast(this.feature.getPropertyValue(this.name));
        }

        /**
         * Sets the value of this attribute. This method assumes that the
         * {@code Feature.setPropertyValue(String, Object)} implementation
         * will verify the argument type.
         */
        @Override
        public void setValue(final V value) {
            this.feature.setPropertyValue(this.name, value);
        }

        /**
         * Wraps the property value in a set.
         */
        @Override
        public Collection<V> getValues() {
            return PropertyView.singletonOrEmpty(getValue());
        }
    }

    @Override
    public final int hashCode() {
        return PropertyView.hashCode(feature, name);
    }

    @Override
    public final boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj.getClass() == getClass()) {
            final AttributeView<?> that = (AttributeView<?>) obj;
            return feature == that.feature && Objects.equals(name, that.name);
        }
        return false;
    }

    @Override
    public final String toString() {
        return PropertyView.toString(getClass(), type.getValueClass(), getName(), getValues());
    }
}
