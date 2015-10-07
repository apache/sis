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
package org.apache.sis.xml;

import java.util.Map;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;


/**
 * A map view of some or all identifiers in an {@linkplain IdentifiedObject identified object}.
 * Each {@linkplain java.util.Map.Entry map entry} is associated to an {@link Identifier} where
 * {@linkplain java.util.Map.Entry#getKey() key} is the {@linkplain Identifier#getAuthority()
 * identifier authority} and the {@linkplain java.util.Map.Entry#getValue() value} is the
 * {@linkplain Identifier#getCode() identifier code}.
 *
 * <p>Some XML identifiers are difficult to handle as {@link Identifier} objects. Those identifiers are
 * rather handled using specialized classes like {@link XLink}. This {@code IdentifierMap} interface
 * mirrors the standard {@link Map#get(Object) get} and {@link Map#put(Object, Object) put} methods
 * with specialized methods, in order to fetch and store identifiers as objects of the specialized
 * class.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see IdentifiedObject#getIdentifierMap()
 */
public interface IdentifierMap extends Map<Citation,String> {
    /**
     * Returns the identifier associated to the given namespace,
     * or {@code null} if this map contains no mapping of the
     * specialized type for the namespace.
     *
     * @param  <T> The identifier type.
     * @param  authority The namespace whose associated identifier is to be returned.
     * @return The identifier to which the given namespace is mapped, or
     *         {@code null} if this map contains no mapping for the namespace.
     */
    <T> T getSpecialized(IdentifierSpace<T> authority);

    /**
     * Associates the given identifier with the given namespace in this map
     * (optional operation). If the map previously contained a mapping for
     * the namespace, then the old value is replaced by the specified value.
     *
     * @param  <T> The identifier type.
     * @param  authority The namespace with which the given identifier is to be associated.
     * @param  value The identifier to be associated with the given namespace.
     * @return The previous identifier associated with {@code authority}, or {@code null}
     *         if there was no mapping of the specialized type for {@code authority}.
     * @throws UnsupportedOperationException If the identifier map is unmodifiable.
     */
    <T> T putSpecialized(IdentifierSpace<T> authority, T value) throws UnsupportedOperationException;
}
