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
package org.apache.sis.referencing.crs;

import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.math.FunctionProperty;


/**
 * Provides a map without the {@code "conversion."} prefix in front of property keys.
 * The method to invoke is {@link #unprefix(Map)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class ConversionKeys implements ObjectConverter<String,String> {
    /**
     * The prefix to add or remove to the keys.
     */
    private static final String PREFIX = "conversion.";

    /**
     * The converter for adding or removing the {@link #PREFIX} in keys.
     */
    private static final ConversionKeys ADD = new ConversionKeys(true), REMOVE = new ConversionKeys(false);

    /**
     * {@code true} if this converter adds the prefix, or {@code false} if it removes it.
     */
    private final boolean add;

    /**
     * Creates a new converter which will add or remove the prefix.
     */
    private ConversionKeys(final boolean add) {
        this.add = add;
    }

    /**
     * Provides a map without the {@code "conversion."} prefix in the keys.
     *
     * @param <V> Type of values in the map.
     * @param properties The user-supplied properties.
     */
    @SuppressWarnings("unchecked")
    static <V> Map<String,V> unprefix(final Map<String,V> properties) {
        /*
         * The cast to (Class<V>) is not correct, but it not a problem in this particular case
         * because this converter will be used for a map which will be read only (not written)
         * by DefaultConversion. The returned map should never escape the SIS private space.
         */
        return ObjectConverters.derivedKeys(properties, REMOVE, (Class<V>) Object.class);
    }

    /**
     * Returns the type of keys in the user-supplied properties map.
     */
    @Override
    public Class<String> getSourceClass() {
        return String.class;
    }

    /**
     * Returns the type of keys in the derived properties.
     */
    @Override
    public Class<String> getTargetClass() {
        return String.class;
    }

    /**
     * Returns the manner in which source keys are mapped to target keys.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.INVERTIBLE, FunctionProperty.ORDER_PRESERVING,
                add ? FunctionProperty.INJECTIVE : FunctionProperty.SURJECTIVE);
    }

    /**
     * Adds or removes the prefix from the specified key.
     * In the removal case if the key does not begin with the prefix, then this method returns {@code null}.
     *
     * @param  key A key from the user-supplied properties map.
     * @return The key to show in the derived map.
     */
    @Override
    public String apply(final String key) {
        if (add) {
            return PREFIX + key;
        } else {
            return key.startsWith(PREFIX) ? key.substring(PREFIX.length()) : null;
        }
    }

    /**
     * Returns the inverse of this converter.
     */
    @Override
    public ObjectConverter<String,String> inverse() {
        return add ? REMOVE : ADD;
    }
}
