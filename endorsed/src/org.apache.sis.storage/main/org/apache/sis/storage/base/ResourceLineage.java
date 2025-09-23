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
package org.apache.sis.storage.base;

import java.util.Collection;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.identification.Resolution;
import org.opengis.metadata.identification.Identification;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.InternationalString;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.lineage.DefaultSource;
import org.apache.sis.metadata.iso.maintenance.DefaultScope;
import static org.apache.sis.util.internal.shared.CollectionsExt.nonNull;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.MetadataScope;


/**
 * Metadata about a resource which is a single source of another resource.
 * This is an experimental class which may be revisited in any future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ResourceLineage {
    /**
     * Description of the level of the source data, or {@code null} if none.
     * Current implementation uses the first non-null title of a citation.
     */
    private InternationalString description;

    /**
     * Recommended reference to be used for the source data, or {@code null}.
     * Current implementation uses the first citation provided by {@link Metadata#getIdentificationInfo()}.
     */
    private Citation sourceCitation;

    /**
     * The type and extent of the source, or {@code null} if none.
     * Current implementation uses the resource scope declared in source metadata,
     * together with the source extents.
     */
    private DefaultScope scope;

    /**
     * She spatial reference system used by the source data, or {@code null} if none.
     * Current implementation uses the first reference system declared by metadata.
     */
    private ReferenceSystem referenceSystem;

    /**
     * Spatial resolution expressed as a scale factor, an angle or a level of detail.
     * Current implementation uses the first resolution found in identification information.
     */
    private Resolution resolution;

    /**
     * Returns {@code false} if this object has at least one non-null value.
     */
    final boolean isEmpty() {
        return description == null && sourceCitation == null && scope == null
                && referenceSystem == null && resolution == null;
    }

    /**
     * Collects information about a source of the derived resource for which to provide lineage.
     *
     * @param  source  metadata of a source of the derived resource for which to provide lineage.
     */
    ResourceLineage(final Metadata source) {
        referenceSystem = CollectionsExt.first(source.getReferenceSystemInfo());
        for (final Identification info : nonNull(source.getIdentificationInfo())) {
            final Citation citation = info.getCitation();
            if (citation != null) {
                if (sourceCitation == null) {
                    sourceCitation = citation;
                }
                if (description == null) {
                    description = citation.getTitle();
                }
            }
            if (resolution == null) {
                for (final Resolution candidate : nonNull(info.getSpatialResolutions())) {
                    if (candidate != null) {
                        resolution = candidate;
                    }
                }
            }
        }
        final ScopeCode level = getScopeLevel(source);
        final Collection<? extends Extent> extents = Extents.fromIdentificationInfo(source);
        if (level != null || !extents.isEmpty()) {
            scope = new DefaultScope(level);
            scope.setExtents(extents);
        }
    }

    /**
     * Returns the type (coverage, feature, …) of the source to be stored in the "level" attribute of the scope.
     *
     * @return scope level (coverage, feature, …), or {@code null} if none.
     */
    private static ScopeCode getScopeLevel(final Metadata source) {
        ScopeCode level = null;
        for (final MetadataScope ms : nonNull(source.getMetadataScopes())) {
            final ScopeCode c = ms.getResourceScope();
            if (c != null) {
                if (level == null) {
                    level = c;
                } else if (!level.equals(c)) {
                    level = null;
                    break;
                }
            }
        }
        return level;
    }

    /**
     * Creates an ISO 19115 metadata object from the information collected in this class.
     */
    final DefaultSource build() {
        final var source = new DefaultSource();
        source.setDescription(description);
        source.setSourceCitation(sourceCitation);
        source.setScope(scope);
        source.setSourceReferenceSystem(referenceSystem);
        source.setSourceSpatialResolution(resolution);
        return source;
    }
}
