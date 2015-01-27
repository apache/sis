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
package org.apache.sis.internal.jaxb;

import java.util.AbstractMap;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;


/**
 * An entry in the {@link IdentifierMap}. This class implements both the
 * {@link Map.Entry} interface (for inclusion in the set to be returned
 * by {@link IdentifierMapAdapter#entrySet()}) and the {@link Identifier}
 * interface (for inclusion in the {@link IdentifierMapAdapter#identifiers}
 * collection).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class IdentifierMapEntry extends AbstractMap.SimpleEntry<Citation,String> implements Identifier {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5159620102001638970L;

    /**
     * Creates a new entry for the given authority and code.
     */
    IdentifierMapEntry(final Citation authority, final String code) {
        super(authority, code);
    }

    /**
     * Returns the identifier namespace, which is the key of this entry.
     */
    @Override
    public Citation getAuthority() {
        return getKey();
    }

    /**
     * Returns the identifier code, which is the value of this entry.
     */
    @Override
    public String getCode() {
        return getValue();
    }

    /**
     * Same than the above, but as an immutable entry. We use this implementation when the
     * entry has been created on-the-fly at iteration time rather than being stored in the
     * identifier collection.
     */
    static final class Immutable extends AbstractMap.SimpleImmutableEntry<Citation,String> implements Identifier {
        private static final long serialVersionUID = -6857931598565368465L;
        Immutable(Citation authority, String code) {super(authority, code);}
        @Override public Citation getAuthority()   {return getKey();}
        @Override public String   getCode()        {return getValue();}
    }
}
