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
package org.apache.sis.storage.geoheif;

import java.util.Collection;
import java.util.Optional;
import org.opengis.util.GenericName;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.collection.Containers;


/**
 * An aggregation of other resources.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class Group extends AbstractResource implements Aggregate {
    /**
     * Name of this group.
     */
    private final GenericName name;

    /**
     * The components of this group.
     */
    private final GridCoverageResource[] components;

    /**
     * Creates a new group of grid coverage resources.
     *
     * @param store       the parent of this aggregate.
     * @param name        the name of this aggregate.
     * @param components  the child resources.
     */
    Group(final GeoHeifStore store, final GenericName name, final GridCoverageResource[] components) {
        super(store);
        this.name = name;
        this.components = components;
    }

    /**
     * Returns the name of this aggregate.
     */
    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.of(name);
    }

    /**
     * Returns all components of this aggregate.
     */
    @Override
    public Collection<GridCoverageResource> components() {
        return Containers.viewAsUnmodifiableList(components);
    }
}
