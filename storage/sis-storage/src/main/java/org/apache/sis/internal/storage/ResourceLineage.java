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
package org.apache.sis.internal.storage;

import java.util.Arrays;
import java.util.Collection;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.MetadataScope;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.lineage.Source;
import org.opengis.metadata.maintenance.Scope;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Resolution;
import org.opengis.metadata.identification.Identification;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.lineage.DefaultLineage;
import org.apache.sis.metadata.iso.maintenance.DefaultScope;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;

import static org.apache.sis.internal.util.CollectionsExt.nonNull;


/**
 * Metadata about a resource which is a single source of another resource.
 * This is an experimental class which may be revisited in any future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
final class ResourceLineage implements Source {
    /**
     * The source of the derived resource described by the lineage.
     */
    public final Resource source;

    /**
     * Metadata of the source, or {@code null} if none.
     * All properties returned by this class are inferred from those metadata.
     */
    private final Metadata metadata;

    /**
     * Creates a new source wrapping the given resource.
     *
     * @param  source  the source of the derived resource described by the resource lineage.
     * @throws DataStoreException if an error occurred while fetching metadata from the source.
     */
    private ResourceLineage(final Resource source) throws DataStoreException {
        this.source = source;
        metadata = source.getMetadata();
    }

    /**
     * Writes metadata about the sources of a resource.
     *
     * @param  lineage  where to write lineage information.
     * @param  sources  the sources of the resource for which to describe the lineage.
     * @throws DataStoreException if an error occurred while fetching metadata from a resource.
     */
    static void setSources(final DefaultLineage lineage, final Resource... sources) throws DataStoreException {
        final ResourceLineage[] wrappers  = new ResourceLineage[sources.length];
        for (int i=0; i<wrappers.length; i++) {
            wrappers[i] = new ResourceLineage(sources[i]);
        }
        lineage.setSources(Arrays.asList(wrappers));
    }

    /**
     * Returns a description of the level of the source data.
     * Default implementation returns the title of the {@linkplain #getSourceCitation() source citation}.
     *
     * @return description of the level of the source data, or {@code null} if none.
     */
    @Override
    public InternationalString getDescription() {
        final Citation citation = getSourceCitation();
        return (citation != null) ? citation.getTitle() : null;
    }

    /**
     * Returns the recommended reference to be used for the source data.
     * Default implementation returns the first citation having a non-null title
     * among the citations provided by {@link Metadata#getIdentificationInfo()}.
     *
     * @return recommended reference to be used for the source data, or {@code null}.
     */
    @Override
    public Citation getSourceCitation() {
        if (metadata != null) {
            for (final Identification info : nonNull(metadata.getIdentificationInfo())) {
                final Citation citation = info.getCitation();
                if (citation != null) {
                    if (citation.getTitle() != null) {
                        return citation;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the type and extent of the source. Default implementation returns the resource scope
     * declared in source metadata, together with the {@linkplain #getSourceExtents() source extents}.
     *
     * @return type and extent of the source, or {@code null} if none.
     */
    @Override
    public Scope getScope() {
        return createScope(metadata);
    }

    /**
     * Creates a scope from the given resource metadata.
     */
    private static Scope createScope(final Metadata metadata) {
        if (metadata != null) {
            ScopeCode level = null;
            for (final MetadataScope ms : nonNull(metadata.getMetadataScopes())) {
                level = ms.getResourceScope();
                if (level != null) break;
            }
            final Collection<? extends Extent> extents = Extents.fromIdentificationInfo(metadata);
            if (level != null || !extents.isEmpty()) {
                final DefaultScope scope = new DefaultScope(level);
                scope.setExtents(extents);
                return scope;
            }
        }
        return null;
    }

    /**
     * Information about the spatial, vertical and temporal extent of the source data.
     * Default implementation returns all extents declared in {@link Metadata#getIdentificationInfo()}.
     *
     * @return information about the extent of the source data.
     *
     * @deprecated As of ISO 19115:2014, moved to {@link Scope#getExtents()}.
     */
    @Override
    @Deprecated
    public Collection<? extends Extent> getSourceExtents() {
        return Extents.fromIdentificationInfo(metadata);
    }

    /**
     * Returns the spatial reference system used by the source data.
     * Default implementation returns the first reference system declared by metadata.
     *
     * @return spatial reference system used by the source data, or {@code null}.
     */
    @Override
    public ReferenceSystem getSourceReferenceSystem() {
        return (metadata != null) ? CollectionsExt.first(metadata.getReferenceSystemInfo()) : null;
    }

    /**
     * Spatial resolution expressed as a scale factor, an angle or a level of detail.
     * Default implementation returns the first resolution found in identification information.
     *
     * @return spatial resolution, or {@code null} if none.
     */
    @Override
    public Resolution getSourceSpatialResolution() {
        if (metadata != null) {
            for (final Identification info : nonNull(metadata.getIdentificationInfo())) {
                for (final Resolution candidate : nonNull(info.getSpatialResolutions())) {
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }
}
