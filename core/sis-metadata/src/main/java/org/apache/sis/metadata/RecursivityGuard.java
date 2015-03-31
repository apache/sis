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
import java.util.IdentityHashMap;


/**
 * A guard against infinite recursivity in {@link AbstractMetadata#hashCode()},
 * {@link AbstractMetadata#isEmpty()} and {@link ModifiableMetadata#prune()} methods.
 * {@link AbstractMetadata#equals(Object)} uses an other implementation in {@link ObjectPair}.
 *
 * <div class="section">The problem</div>
 * Cyclic associations can exist in ISO 19115 metadata. For example {@code Instrument} know the platform
 * it is mounted on, and the {@code Platform} know its list of instrument. Consequently walking down the
 * metadata tree can cause infinite recursivity, unless we keep trace of previously visited metadata objects
 * in order to avoid visiting them again. We use an {@link IdentityHashMap} for that purpose, since the
 * recursivity problem exists only when revisiting the exact same instance. Furthermore, {@code HashMap}
 * would not suit since it invokes {@code equals(Object)} and {@code hashCode()}, which are precisely
 * the methods that we want to avoid invoking twice.
 *
 * @param <V> The kind of values to store in the maps.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see #HASH_CODES
 * @see ObjectPair#CURRENT
 * @see Pruner#MAPS
 */
final class RecursivityGuard<V> extends ThreadLocal<Map<Object,V>> {
    /**
     * The recursivity guard to use during {@code hashCode()} computations.
     * The values have no meaning for this map; only the keys matter.
     */
    static final RecursivityGuard<Object> HASH_CODES = new RecursivityGuard<Object>();

    /**
     * Creates a new thread-local map.
     */
    RecursivityGuard() {
    }

    /**
     * Creates an initially empty hash map when first needed, before any recursive invocation.
     */
    @Override
    protected Map<Object,V> initialValue() {
        return new IdentityHashMap<Object,V>();
    }
}
