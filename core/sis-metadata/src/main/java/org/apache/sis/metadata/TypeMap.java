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
package org.apache.sis.metadata;

import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * Map of property valuePolicy for a given implementation class. This map is read-only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see MetadataStandard#asTypeMap(Class, KeyNamePolicy, TypeValuePolicy)
 */
final class TypeMap extends PropertyMap<Class<?>> {
    /**
     * The kind of values in this map.
     */
    final TypeValuePolicy valuePolicy;

    /**
     * Creates a type map for the specified accessor.
     *
     * @param accessor    The accessor to use for the metadata.
     * @param keyPolicy   Determines the string representation of keys in the map..
     * @param valuePolicy The kind of values in this map.
     */
    TypeMap(final PropertyAccessor accessor, final KeyNamePolicy keyPolicy, final TypeValuePolicy valuePolicy) {
        super(accessor, keyPolicy);
        this.valuePolicy = valuePolicy;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     */
    @Override
    public Class<?> get(final Object key) {
        if (key instanceof String) {
            return accessor.type(accessor.indexOf((String) key, false), valuePolicy);
        }
        return null;
    }

    /**
     * Returns an iterator over the entries contained in this map.
     */
    @Override
    final Iterator<Map.Entry<String,Class<?>>> iterator() {
        return new Iter() {
            @Override
            public Map.Entry<String,Class<?>> next() {
                if (index >= accessor.count()) {
                    throw new NoSuchElementException();
                }
                final Class<?> value = accessor.type(index, valuePolicy);
                return new SimpleImmutableEntry<String,Class<?>>(accessor.name(index++, keyPolicy), value);
            }
        };
    }
}
