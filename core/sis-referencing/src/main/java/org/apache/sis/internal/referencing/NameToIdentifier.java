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

import org.opengis.util.NameSpace;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Does the unobvious mapping between {@link ReferenceIdentifier} properties and {@link GenericName} ones.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class NameToIdentifier implements ReferenceIdentifier {
    /**
     * The name from which to infer the identifier attributes.
     */
    private final GenericName name;

    /**
     * Infers the attributes from the given name.
     *
     * @param name The name from which to infer the identifier properties.
     */
    public NameToIdentifier(final GenericName name) {
        ensureNonNull("name", name);
        this.name = name;
    }

    /**
     * Infers the authority from the scope.
     */
    @Override
    public Citation getAuthority() {
        final NameSpace scope = name.scope();
        if (scope == null || scope.isGlobal()) {
            return null;
        }
        return Citations.fromName(scope.name().tip().toString());
    }

    /**
     * Takes everything except the tip as the code space.
     *
     * @param name The name from which to get the code space.
     * @return The code space, or {@code null} if none.
     */
    public static String getCodeSpace(final GenericName name) {
        if (name instanceof ScopedName) {
            return ((ScopedName) name).path().toString();
        }
        if (name.depth() == 2) {
            // May happen on GenericName implementation that do not implement the ScopedName interface.
            // The most importance case is org.apache.sis.referencing.NamedIdentifier.
            return name.head().toString();
        }
        return null;
    }

    /**
     * Takes everything except the tip as the code space.
     */
    @Override
    public String getCodeSpace() {
        return getCodeSpace(name);
    }

    /**
     * Takes the last element as the code.
     */
    @Override
    public String getCode() {
        return name.tip().toString();
    }

    /**
     * Names are not versioned.
     */
    @Override
    public String getVersion() {
        return null;
    }
}
