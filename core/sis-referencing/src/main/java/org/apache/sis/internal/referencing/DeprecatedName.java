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

import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.Identifier;
import org.apache.sis.referencing.NamedIdentifier;


/**
 * A name which is deprecated (when associated to a given object) in the EPSG database.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class DeprecatedName extends NamedIdentifier {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1792369861343798471L;

    /**
     * Creates a new deprecated name with the same authority, code, version and remarks than the identifier.
     *
     * @param identifier The identifier.
     */
    public DeprecatedName(final Identifier identifier) {
        super(identifier);
    }

    /**
     * Creates a new deprecated name for the given code.
     *
     * @param authority The authority, or {@code null} if not available.
     * @param code      The code.
     */
    public DeprecatedName(final Citation authority, final String code) {
        super(authority, code);
    }

    /**
     * Returns {@code true} since this name is deprecated.
     *
     * @return {@code true}.
     */
    @Override
    public boolean isDeprecated() {
        return true;
    }
}
