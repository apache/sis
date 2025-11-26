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
package org.apache.sis.storage.landsat;

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;


/**
 * An aggregate of {@link Band}.
 * Each aggregate is for one {@link BandGroupName}.
 *
 * @todo Future implementation should implement {@code GridCoverageResource}
 *       and provides an aggregated coverage view where each Landsat band is
 *       a sample dimension.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class BandGroup extends AbstractResource implements Aggregate, StoreResource {
    /**
     * The group of bands that this aggregate represents.
     */
    final BandGroupName group;

    /**
     * Name of the band group.
     * This is set by {@link LandsatStore} and should not be modified after that point.
     */
    GenericName identifier;

    /**
     * The array of images for each Landsat band.
     */
    private final Band[] components;

    /**
     * Creates a new aggregate for the specified group.
     * This constructor will copy only the resources for that group from the given array.
     */
    private BandGroup(final Resource parent, final BandGroupName group,
                      final Band[] resources, final int count)
    {
        super(parent);
        this.group = group;
        int n = 0;
        Band[] components = new Band[resources.length];
        for (int i=0; i<count; i++) {
            final Band r = resources[i];
            if (r.band.group == group) {
                components[n++] = r;
            }
        }
        this.components = ArraysExt.resize(components, n);
    }

    /**
     * Creates aggregates for the given bands.
     */
    static BandGroup[] group(final Resource parent, final Band[] resources, final int count) {
        final BandGroupName[] groups = BandGroupName.values();
        final BandGroup[] aggregates = new BandGroup[groups.length];
        int n = 0;
        for (final BandGroupName group : groups) {
            final BandGroup c = new BandGroup(parent, group, resources, count);
            if (c.components.length != 0) {
                aggregates[n++] = c;
            }
        }
        return ArraysExt.resize(aggregates, n);
    }

    /**
     * Returns the data store that created this resource.
     */
    @Override
    public DataStore getOriginator() {
        return (DataStore) listeners.getParent().get().getSource();
    }

    /**
     * Returns the resource persistent identifier if available.
     */
    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.of(identifier);
    }

    /**
     * Invoked in a synchronized block the first time that {@link #getMetadata()} is invoked.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final MetadataBuilder metadata = new MetadataBuilder();
        metadata.addTitle(group.title);             // Must be before `addDefaultMetadata(…)`.
        metadata.addDefaultMetadata(this, listeners);
        return metadata.build();
    }

    /**
     * Returns the resources for each Landsat band of this group.
     */
    @Override
    public Collection<Resource> components() {
        return UnmodifiableArrayList.wrap(components);
    }

    /**
     * Returns the paths to the files containing each band.
     */
    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        final Set<Path> paths = new HashSet<>();
        for (Band b : components) {
            b.getFileSet().map(FileSet::getPaths).ifPresent(paths::addAll);
        }
        return Optional.of(new FileSet(paths));
    }

    /**
     * Returns all bands in the given array of aggregates.
     */
    static final List<Band> bands(final BandGroup[] components) {
        final List<Band> bands = new ArrayList<>();
        if (components != null) {
            for (final BandGroup c : components) {
                Collections.addAll(bands, c.components);
            }
        }
        return bands;
    }
}
