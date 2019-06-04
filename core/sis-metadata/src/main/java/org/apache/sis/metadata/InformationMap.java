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
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.ExtendedElementInformation;


/**
 * Map of information for a given implementation class. This map is read-only.
 * All values in this map are instances of {@link PropertyInformation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 *
 * @see PropertyInformation
 * @see MetadataStandard#asInformationMap(Class, KeyNamePolicy)
 *
 * @since 0.3
 * @module
 */
final class InformationMap extends PropertyMap<ExtendedElementInformation> {
    /**
     * The standard which define the {@link PropertyAccessor#type} interface.
     */
    private final Citation standard;

    /**
     * Creates an information map for the specified accessor.
     *
     * @param standard   the standard which define the {@code accessor.type} interface.
     * @param accessor   the accessor to use for the metadata.
     * @param keyPolicy  determines the string representation of keys in the map.
     */
    InformationMap(final Citation standard, final PropertyAccessor accessor, final KeyNamePolicy keyPolicy) {
        super(accessor, keyPolicy);
        this.standard = standard;
    }

    /**
     * Returns the information to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     */
    @Override
    public ExtendedElementInformation get(final Object key) {
        if (key instanceof String) {
            return accessor.information(standard, accessor.indexOf((String) key, false));
        }
        return null;
    }

    /**
     * Returns an iterator over the entries contained in this map.
     */
    @Override
    final Iterator<Map.Entry<String,ExtendedElementInformation>> iterator() {
        return new Iter() {
            @Override
            public Map.Entry<String,ExtendedElementInformation> next() {
                final ExtendedElementInformation value = accessor.information(standard, index);
                if (value == null) {
                    // PropertyAccessor.information(int) never return null if the index is valid.
                    throw new NoSuchElementException();
                }
                return new SimpleImmutableEntry<>(accessor.name(index++, keyPolicy), value);
            }
        };
    }
}
