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
import org.opengis.feature.Feature;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;


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
class AttributeView<V> extends PropertyView<V> implements Attribute<V> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3617999929561826634L;

    /**
     * The type of this attribute. Must be one of the properties listed in the {@link #feature}.
     */
    final AttributeType<V> type;

    /**
     * Creates a new attribute which will delegate its work to the given feature.
     */
    private AttributeView(final Feature feature, final AttributeType<V> type) {
        super(feature, type.getName().toString());
        this.type = type;
    }

    /**
     * Creates a new attribute which will delegate its work to the given feature.
     *
     * @param feature  the feature from which to read and where to write the attribute value.
     * @param type     the type of this attribute. Must be one of the properties listed in the
     *                 {@link #feature} (this is not verified by this constructor).
     */
    static <V> Attribute<V> create(final Feature feature, final AttributeType<V> type) {
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

    /**
     * Returns the type specified at construction time.
     */
    @Override
    public final AttributeType<V> getType() {
        return type;
    }

    /**
     * Returns the class of values.
     */
    @Override
    final Class<V> getValueClass() {
        return type.getValueClass();
    }

    /**
     * Returns an empty map since this simple view does not support characteristics.
     */
    @Override
    public final Map<String,Attribute<?>> characteristics() {
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
        Singleton(final Feature feature, final AttributeType<V> type) {
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
         * {@link Feature#setPropertyValue(String, Object)} implementation
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
            return singletonOrEmpty(getValue());
        }
    }
}
