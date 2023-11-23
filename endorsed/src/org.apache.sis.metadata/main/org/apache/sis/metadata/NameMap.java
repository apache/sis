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


/**
 * Map of property names for a given implementation class. This map is read-only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see MetadataStandard#asNameMap(Class, KeyNamePolicy, KeyNamePolicy)
 */
final class NameMap extends PropertyMap<String> {
    /**
     * Determines the string representation of values in this map.
     */
    private final KeyNamePolicy valuePolicy;

    /**
     * Creates a name map for the specified accessor.
     *
     * @param accessor     the accessor to use for the metadata.
     * @param keyPolicy    determines the string representation of keys in the map.
     * @param valuePolicy  determines the string representation of values in this map.
     */
    NameMap(final PropertyAccessor accessor, final KeyNamePolicy keyPolicy, final KeyNamePolicy valuePolicy) {
        super(accessor, keyPolicy);
        this.valuePolicy = valuePolicy;
    }

    /**
     * Returns the name for the property at the specified index.
     */
    @Override
    final String getReflectively(final int index) {
        return accessor.name(index, valuePolicy);
    }
}
