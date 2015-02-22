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
package org.apache.sis.internal.referencing.provider;

import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A reference identifier for EPSG or GeoTiff codes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class IdentifierCode extends ImmutableIdentifier {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 357222258307746767L;

    /**
     * If this identifier is deprecated, the identifier that supersede this one.
     * Otherwise {@code 0}.
     */
    final int supersededBy;

    /**
     * Creates a new identifier for the given authority.
     *
     * @param authority Organization for definition and maintenance of the code space or code.
     * @param code Identifier code from the authority.
     */
    IdentifierCode(final Citation authority, final int code) {
        this(authority, code, 0);
    }

    /**
     * Creates a deprecated identifier for the given authority.
     *
     * @param authority Organization for definition and maintenance of the code space or code.
     * @param code Identifier code from the authority.
     * @param supersededBy The code that replace this one.
     */
    IdentifierCode(final Citation authority, final int code, final int supersededBy) {
        super(authority, codespace(authority), Integer.toString(code), null, remarks(supersededBy));
        this.supersededBy = supersededBy;
    }

    /**
     * Returns the code space for the given authority.
     */
    private static String codespace(final Citation authority) {
        return authority.getIdentifiers().iterator().next().getCode();
    }

    /**
     * formats a "Superseded by" international string.
     */
    private static InternationalString remarks(final int supersededBy) {
        if (supersededBy == 0) {
            return null;
        }
        return Vocabulary.formatInternational(Vocabulary.Keys.SupersededBy_1, supersededBy);
    }

    /**
     * Returns {@code true} if this code is deprecated.
     */
    @Override
    public boolean isDeprecated() {
        return supersededBy != 0;
    }
}
