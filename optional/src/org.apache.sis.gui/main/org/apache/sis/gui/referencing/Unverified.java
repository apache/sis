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
package org.apache.sis.gui.referencing;

import org.opengis.util.FactoryException;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;


/**
 * Wrapper for a {@link ReferenceSystem} which has not yet been compared with authoritative definitions.
 * Those wrappers are created when {@link ReferenceSystem} instances have been specified to {@code setPreferred(…)}
 * or {@code addAlternatives(…)} methods with {@code replaceByAuthoritativeDefinition} argument set to {@code true}.
 *
 * @see RecentReferenceSystems#setPreferred(boolean, ReferenceSystem)
 * @see RecentReferenceSystems#addAlternatives(boolean, ReferenceSystem...)
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Unverified {
    /**
     * The reference system to verify.
     */
    private final ReferenceSystem system;

    /**
     * Flags the given reference system as unverified.
     */
    Unverified(final ReferenceSystem system) {
        this.system = system;
    }

    /**
     * Returns the verified (if possible) reference system.
     *
     * @param  finder  the finder to use.
     * @return the resolved reference system.
     * @throws FactoryException if an error occurred while resolving the reference system.
     */
    ReferenceSystem find(final IdentifiedObjectFinder finder) throws FactoryException {
        if (finder != null && finder.findSingleton(system) instanceof ReferenceSystem replacement) {
            return replacement;
        }
        return system;
    }
}
