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
package org.apache.sis.referencing.factory;

import java.util.Iterator;
import java.util.Map;
import org.apache.sis.internal.util.AbstractMap;


/**
 * A map of authority codes filtered by their type.
 * This map is used for implementation of {@link CommonAuthorityFactory#getAuthorityCodes(Class)}.
 * Only keys in this map are useful; values are meaningless.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class FilteredCodes extends AbstractMap<String, Boolean> {
    /**
     * The codes known to the authority factory, associated with their CRS type.
     */
    final Map<String,Class<?>> codes;

    /**
     * The type of spatial reference objects for which the codes are desired.
     */
    final Class<?> type;

    /**
     * Creates a new filtered view over the given authority codes.
     */
    FilteredCodes(final Map<String,Class<?>> codes, final Class<?> type) {
        this.codes = codes;
        this.type = type;
    }

    /**
     * Returns an iterator over the entries to retain.
     * Values in the returned entries are meaningless; only the keys matter.
     */
    @Override
    protected EntryIterator<String,Boolean> entryIterator() {
        return new EntryIterator<String,Boolean>() {
            /** Iterator over the backing map. */
            private final Iterator<Map.Entry<String,Class<?>>> it = codes.entrySet().iterator();

            /** The next code to return. */
            private String code;

            /** Move the iterator position to the next code to return. */
            @Override protected boolean next() {
                while (it.hasNext()) {
                    final Map.Entry<String, Class<?>> entry = it.next();
                    if (type.isAssignableFrom(entry.getValue())) {
                        code = entry.getKey();
                        return true;
                    }
                }
                return false;
            }

            /** Returns the code at the current iterator position. */
            @Override protected String getKey() {
                return code;
            }

            /** Ignored, except that it must be non-null. */
            @Override protected Boolean getValue() {
                return Boolean.TRUE;
            }
        };
    }

    /**
     * Returns a non-null value if the given code is included in the set.
     */
    @Override
    public Boolean get(final Object key) {
        Class<?> t = codes.get(key);
        if (t == null && key instanceof String) {
            t = codes.get(CommonAuthorityFactory.reformat((String) key));
            if (t == null) {
                return null;
            }
        }
        return type.isAssignableFrom(t) ? Boolean.TRUE : null;
    }
}
