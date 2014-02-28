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

import org.opengis.util.GenericName;
import org.apache.sis.internal.util.Numerics;

import static org.apache.sis.util.ArgumentChecks.*;


/**
 * Definition of an attribute in a feature type.
 *
 * <div class="note"><b>Note:</b>
 * Compared to the Java language, {@code FeatureType} is equivalent to {@link Class} and
 * {@code AttributeType} is equivalent to {@link java.lang.reflect.Field}.</div>
 *
 * <div class="warning"><b>Warning:</b>
 * This class is expected to implement a GeoAPI {@code AttributeType} interface in a future version.
 * When such interface will be available, most references to {@code DefaultAttributeType} in the API
 * will be replaced by references to the {@code AttributeType} interface.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public class DefaultAttributeType<T> extends AbstractIdentifiedType {
    /**
     * The class that describe the type of attribute values.
     *
     * @see #getValueClass()
     */
    private final Class<T> valueClass;

    /**
     * The default value for the attribute, or {@code null}.
     *
     * @see #getDefaultValue()
     */
    private final T defaultValue;

    /**
     * Creates an attribute type of the given name.
     *
     * @param name          The name of this attribute type.
     * @param valueClass    The type of attribute values.
     * @param defaultValue  The default value, or {@code null} if none.
     */
    public DefaultAttributeType(final GenericName name, final Class<T> valueClass, final T defaultValue) {
        super(name);
        ensureNonNull("valueClass",   valueClass);
        ensureCanCast("defaultValue", valueClass, defaultValue);
        this.valueClass   = valueClass;
        this.defaultValue = Numerics.cached(defaultValue);
    }

    /**
     * The type of attribute values.
     */
    public Class<T> getValueClass() {
        return valueClass;
    }

    /**
     * The minimum number of occurrences of the property within its containing entity.
     * This value is always an integer greater than or equal to zero.
     */
    public int getMinimumOccurs() {
        return 0;
    }

    /**
     * The maximum number of occurrences of the property within its containing entity.
     * This value is a positive integer. A value of {@link Integer#MAX_VALUE} means that
     * the maximum number of occurrences is unbounded.
     */
    public int getMaximumOccurs() {
        return 1;
    }

    /**
     * The default value for the attribute.
     * This value is used when an attribute is created and no value for it is specified.
     */
    public T getDefaultValue() {
        return defaultValue;
    }
}
