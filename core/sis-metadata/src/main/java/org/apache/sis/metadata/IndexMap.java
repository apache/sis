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
 * Map of property indices for a given implementation class. This map is read-only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see MetadataStandard#asIndexMap(Class, KeyNamePolicy)
 *
 * @since 0.8
 * @module
 */
final class IndexMap extends PropertyMap<Integer> {
    /**
     * Creates a name map for the specified accessor.
     *
     * @param accessor   the accessor to use for the metadata.
     * @param keyPolicy  determines the string representation of keys in the map.
     */
    IndexMap(final PropertyAccessor accessor, final KeyNamePolicy keyPolicy) {
        super(accessor, keyPolicy);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     */
    @Override
    public Integer get(final Object key) {
        if (key instanceof String) {
            final int i = accessor.indexOf((String) key, false);
            if (i >= 0) return i;
        }
        return null;
    }

    /**
     * Returns an iterator over the entries contained in this map.
     */
    @Override
    final Iterator<Map.Entry<String,Integer>> iterator() {
        return new Iter() {
            @Override
            public Map.Entry<String,Integer> next() {
                final int i = index++;
                final String name = accessor.name(i, keyPolicy);
                if (name != null) {
                    return new SimpleImmutableEntry<>(name, i);
                }
                throw new NoSuchElementException();
            }
        };
    }
}
