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
package org.apache.sis.referencing.gazetteer;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Optional;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.referencing.gazetteer.internal.Resources;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.iso.DefaultNameSpace;


/**
 * A factory of reference systems by identifiers implemented by the SIS library.
 * Current implementation can instantiate shared instances of
 * {@link MilitaryGridReferenceSystem} and
 * {@link GeohashReferenceSystem}.
 *
 * MGRS and Geohash are not really "gazetteers", but we handle them in this class
 * for having a unique framework for referencing by identifiers.
 * Real gazetteers may be added in a future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 */
public class GazetteerFactory extends AbstractFactory {
    /**
     * Creates a new factory.
     */
    public GazetteerFactory() {
    }

    /**
     * Returns the name of referencing systems known to this factory.
     *
     * @return names of known reference systems.
     */
    public Set<String> getSupportedNames() {
        /*
         * In current implementation of `org.apache.sis.gui` module, the order in this set determines the
         * order of menu items. So we want a "nice" order and a `Set` implementation that preserve it.
         */
        return new LinkedHashSet<>(List.of(
                MilitaryGridReferenceSystem.IDENTIFIER,
                GeohashReferenceSystem.IDENTIFIER));
    }

    /**
     * Returns a shared instance of the reference system identified by the given name.
     * The current implementation recognizes the following names (case-sensitive):
     *
     * <table class="sis">
     *   <caption>Supported reference systems by identifiers</caption>
     *   <tr><th>Name</th>    <th>Reference system class</th></tr>
     *   <tr><td>MGRS</td>    <td>{@link MilitaryGridReferenceSystem}</td></tr>
     *   <tr><td>Geohash</td> <td>{@link GeohashReferenceSystem}</td></tr>
     * </table>
     *
     * @param  name  name of the reference system to obtain.
     * @return shared instance of the reference system for the given name.
     * @throws GazetteerException if the reference system cannot be obtained.
     */
    public ReferencingByIdentifiers forName(final String name) throws GazetteerException {
        return forNameIfKnown(name).orElseThrow(() ->
                new GazetteerException(Resources.format(Resources.Keys.ReferenceSystemNotFound_1, name)));
    }

    /**
     * Optionally returns a shared instance of the reference system identified by the given name.
     * This method performs the same work as {@link #forName(String)} but without throwing an
     * exception if the given name is unknown.
     *
     * @param  name  name of the reference system to obtain.
     * @return shared instance of the reference system for the given name.
     * @throws GazetteerException if the reference system cannot be obtained.
     */
    public Optional<ReferencingByIdentifiers> forNameIfKnown(String name) throws GazetteerException {
        final int s = name.lastIndexOf(DefaultNameSpace.DEFAULT_SEPARATOR);
        if (s < 0 || name.substring(0, s).trim().equalsIgnoreCase(Constants.SIS)) {
            name = name.substring(s + 1).trim();
            switch (name) {
                case MilitaryGridReferenceSystem.IDENTIFIER: return Optional.of(MilitaryGridReferenceSystem.getInstance());
                case GeohashReferenceSystem     .IDENTIFIER: return Optional.of(GeohashReferenceSystem     .getInstance());
            }
        }
        return Optional.empty();
    }
}
