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
package org.apache.sis.internal.referencing;

import java.util.Map;
import java.util.HashMap;
import org.apache.sis.internal.util.AbstractMap;


/**
 * A map which first looks for values in a user-supplied map, then looks in a default map if no value where found
 * in the user-supplied one. This map is for {@link org.apache.sis.referencing.factory.GeodeticObjectFactory} and
 * other SIS factories internal usage only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public class MergedProperties extends AbstractMap<String,Object> {
    /**
     * The user-supplied properties.
     */
    private final Map<String,?> properties;

    /**
     * Fallback for values not found in {@link #properties}.
     */
    private final Map<String,?> defaultProperties;

    /**
     * A map containing the merge of user and default properties, or {@code null} if not yet created.
     * This map is normally never needed. It may be created only if the user creates his own subclass
     * of {@code GeodeticObjectFactory} or other factories and iterates on this map or asks for its size.
     */
    private transient Map<String,Object> merge;

    /**
     * Creates a new map which will merge the given properties on the fly.
     *
     * @param properties The user-supplied properties.
     * @param defaultProperties Fallback for values not found in {@code properties}.
     */
    public MergedProperties(final Map<String,?> properties, final Map<String,?> defaultProperties) {
        this.properties = properties;
        this.defaultProperties = defaultProperties;
    }

    /**
     * Returns an iterator over the user-supplied properties together with
     * the default properties which were not specified in the user's ones.
     *
     * @return Iterator over merged properties.
     */
    @Override
    protected EntryIterator<String,Object> entryIterator() {
        if (merge == null) {
            merge = new HashMap<String,Object>(defaultProperties);
            merge.putAll(properties);
            merge.remove(null);
        }
        return new IteratorAdapter<String,Object>(merge);    // That iterator will skip null values.
    }

    /**
     * Returns the value for the given key by first looking in the user-supplied map,
     * then by looking in the default properties if no value were specified in the user map.
     * If there is no default value, invokes {@link #invisibleEntry(Object)} in last resort.
     *
     * @param  key The key for which to get the value.
     * @return The value associated to the given key, or {@code null} if none.
     */
    @Override
    public Object get(final Object key) {
        Object value = properties.get(key);
        if (value == null && !properties.containsKey(key)) {
            value = defaultProperties.get(key);
            if (value == null) {
                value = invisibleEntry(key);
            }
        }
        return value;
    }

    /**
     * Returns the value for an "invisible" entry if no user-supplied values were found for that key.
     * This is used only for "secret" keys used for SIS internal purpose (not for public API).
     *
     * <div class="note"><b>Example:</b>
     * {@link org.apache.sis.referencing.factory.GeodeticObjectFactory} handles the {@code "mtFactory"} key in a special
     * way since this is normally not needed for CRS, CS and datum objects, except when creating the SIS implementation
     * of derived or projected CRS (because of the way we implemented derived CRS). But this is somewhat specific to
     * SIS, so we do no want to expose this implementation details.</div>
     *
     * @param  key The key for which to get the value.
     * @return The value associated to the given key, or {@code null} if none.
     */
    protected Object invisibleEntry(final Object key) {
        return null;
    }
}
