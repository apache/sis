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
import java.util.Set;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;


/**
 * The base class of {@link Map} views of metadata properties.
 * The map keys are fixed to the {@link String} type and will be the property names.
 * The map values depend on the actual {@code PropertyMap} subclasses; they may be
 * property values, property classes or property information.
 *
 * @param <V> The type of values in the map.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see ValueMap
 * @see NameMap
 * @see TypeMap
 * @see InformationMap
 */
abstract class PropertyMap<V> extends AbstractMap<String,V> {
    /**
     * The accessor to use for accessing the property names, types or values.
     */
    final PropertyAccessor accessor;

    /**
     * Determines the string representation of keys in the map.
     */
    final KeyNamePolicy keyPolicy;

    /**
     * A view of the mappings contained in this map.
     */
    transient Set<Map.Entry<String,V>> entrySet;

    /**
     * Creates a new map backed by the given accessor.
     */
    PropertyMap(final PropertyAccessor accessor, final KeyNamePolicy keyPolicy) {
        this.accessor  = accessor;
        this.keyPolicy = keyPolicy;
    }

    /**
     * Returns the number of elements in this map.
     * The default implementation returns {@link PropertyAccessor#count()}, which is okay only if
     * all metadata defined by the standard are included in the map. Subclasses shall override
     * this method if their map contain only a subset of all possible metadata elements.
     */
    @Override
    public int size() {
        return accessor.count();
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     * The default implementation is okay only if all metadata defined by the standard are included
     * in the map. Subclasses shall override this method if their map contain only a subset of all
     * possible metadata elements.
     */
    @Override
    public boolean containsKey(final Object key) {
        return (key instanceof String) && accessor.indexOf((String) key, false) >= 0;
    }

    /**
     * Returns a view of the mappings contained in this map. Subclasses shall override this method
     * if they define a different entries set class than the default {@link Entries} inner class.
     */
    @Override
    public Set<Map.Entry<String,V>> entrySet() {
        if (entrySet == null) {
            entrySet = new Entries();
        }
        return entrySet;
    }

    /**
     * Returns an iterator over the entries in this map.
     */
    abstract Iterator<Map.Entry<String,V>> iterator();




    /**
     * The iterator over the elements contained in a {@link Entries} set.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    abstract class Iter implements Iterator<Map.Entry<String,V>> {
        /**
         * Index of the next element to return.
         */
        int index;

        /**
         * Creates a new iterator.
         */
        Iter() {
        }

        /**
         * Returns {@code true} if there is more elements to return.
         */
        @Override
        public final boolean hasNext() {
            return index < accessor.count();
        }

        /**
         * Assumes that the underlying map is unmodifiable.
         */
        @Override
        public final void remove() {
            throw new UnsupportedOperationException();
        }
    }




    /**
     * Base class of views of the entries contained in the map.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    class Entries extends AbstractSet<Map.Entry<String,V>> {
        /**
         * Creates a new entries set.
         */
        Entries() {
        }

        /**
         * Returns true if this collection contains no elements.
         */
        @Override
        public final boolean isEmpty() {
            return PropertyMap.this.isEmpty();
        }

        /**
         * Returns the number of elements in this collection.
         */
        @Override
        public final int size() {
            return PropertyMap.this.size();
        }

        /**
         * Returns an iterator over the elements contained in this collection.
         */
        @Override
        public final Iterator<Map.Entry<String,V>> iterator() {
            return PropertyMap.this.iterator();
        }
    }
}
