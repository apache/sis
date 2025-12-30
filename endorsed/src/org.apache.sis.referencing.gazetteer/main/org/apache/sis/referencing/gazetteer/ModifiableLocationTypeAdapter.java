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
import java.util.Map;
import java.util.IdentityHashMap;
import org.opengis.util.InternationalString;
import org.apache.sis.util.collection.Containers;


/**
 * Workaround for the lack of {@code LocationType} interface in GeoAPI 3.0.
 * This workaround will be removed if a future GeoAPI version publish that interface,
 * or if {@link AbstractLocationType} is made public.
 */
final class ModifiableLocationTypeAdapter extends ModifiableLocationType {
    /**
     * The reference system of the original type.
     * This is the only information not stored in {@link ModifiableLocationType}.
     */
    private final ReferencingByIdentifiers referenceSystem;

    /**
     * Copies all information from the given type.
     */
    private ModifiableLocationTypeAdapter(final AbstractLocationType type,
            final Map<AbstractLocationType,ModifiableLocationTypeAdapter> previous)
    {
        super(type.getName());
        setTheme(type.getTheme());
        setDefinition(type.getDefinition());
        setTerritoryOfUse(type.getTerritoryOfUse());
        setOwner(type.getOwner());
        for (final InternationalString s : type.getIdentifications()) {
            addIdentification(s);
        }
        referenceSystem = type.getReferenceSystem();
        for (final AbstractLocationType c : type.getChildren()) {
            ModifiableLocationTypeAdapter p = previous.get(c);
            if (p == null) {
                p = new ModifiableLocationTypeAdapter(c, previous);
                previous.put(c, p);
            }
            p.addParent(this);
        }
    }

    /**
     * Returns type type as-is if it is already an instance of {@code ModifiableLocationType},
     * or returns a copy otherwise.
     */
    static ModifiableLocationType copy(final AbstractLocationType type) {
        if (type instanceof ModifiableLocationType) {
            return (ModifiableLocationType) type;
        } else {
            return new ModifiableLocationTypeAdapter(type,
                    new IdentityHashMap<AbstractLocationType,ModifiableLocationTypeAdapter>());
        }
    }

    /**
     * Copies a list of location types.
     */
    static List<ModifiableLocationType> copy(final List<? extends AbstractLocationType> types) {
        final Map<AbstractLocationType,ModifiableLocationTypeAdapter> previous = new IdentityHashMap<>();
        final ModifiableLocationType[] nt = new ModifiableLocationType[types.size()];
        for (int i=0; i<nt.length; i++) {
            final AbstractLocationType c = types.get(i);
            ModifiableLocationTypeAdapter p = previous.get(c);
            if (p == null) {
                p = new ModifiableLocationTypeAdapter(c, previous);
                previous.put(c, p);
            }
            nt[i] = p;
        }
        return Containers.viewAsUnmodifiableList(nt);
    }

    /**
     * Returns the reference system of the type given to the constructor.
     */
    @Override
    public ReferencingByIdentifiers getReferenceSystem() {
        return referenceSystem;
    }
}
