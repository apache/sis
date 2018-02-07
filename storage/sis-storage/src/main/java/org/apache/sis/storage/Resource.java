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
package org.apache.sis.storage;

import java.util.Collections;
import java.util.Set;
import org.opengis.metadata.Metadata;


/**
 * Provides access to geospatial data in a {@code DataStore}. The ISO 19115 specification defines resource as
 * an <cite>“identifiable asset or means that fulfills a requirement”</cite>. For example a resource can be a
 * coverage of Sea Surface Temperature, or a coverage of water salinity, or the set of all buoys in a harbor,
 * or an aggregation of all the above. A resource is not necessarily digital; it can be a paper document or an
 * organization, in which case only metadata are provided. If the resource is digital, then {@code Resource}s
 * should be instances of sub-types like {@link Aggregate} or {@link FeatureSet}.
 *
 * <p>{@code DataStore}s are themselves closeable resources.
 * If the data store contains resources for many feature types or coverages, then the data store will be an
 * instance of {@link Aggregate}. The {@linkplain Aggregate#components() components} of an aggregate can be
 * themselves other aggregates, thus forming a tree.</p>
 *
 * <div class="note"><b>Relationship with ISO 19115:</b>
 * this type is closely related to the {@code DS_Resource} type defined by ISO 19115.
 * The Apache SIS type differs from the ISO type by being more closely related to data extraction,
 * as can been seen from the checked {@link DataStoreException} thrown by most methods.
 * Convenience methods for frequently requested information – for example {@link DataSet#getEnvelope()} – were added.
 * The sub-types performing the actual data extraction – for example {@link FeatureSet} – are specific to Apache SIS.
 * </div>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 *
 * @see Aggregate#components()
 *
 * @since 0.8
 * @module
 */
public interface Resource {
    /**
     * Returns information about this resource.
     * If this resource is an {@link Aggregate}, then the metadata may enumerate characteristics
     * (spatio-temporal extents, feature types, range dimensions, <i>etc.</i>) of all
     * {@linkplain Aggregate#components() components} in the aggregate, or summarize them (for example by omitting
     * {@linkplain org.apache.sis.metadata.iso.extent.DefaultExtent extents} that are fully included in larger extents).
     * If this resource is a {@link DataSet}, then the metadata shall contain only the information that apply to that
     * particular dataset, optionally with a reference to the parent metadata (see below).
     *
     * <p>Some relationships between metadata and resources are:</p>
     * <ul class="verbose">
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getParentMetadata() parentMetadata} /
     *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getTitle() title}:<br>
     *       a human-readable caption for {@link DataStore#getMetadata()} (if not redundant with this metadata).</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     *       {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getCitation() citation} /
     *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getTitle() title}:<br>
     *       a human-readable designation for this resource.</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     *       {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getAssociatedResources() associatedResource} /
     *       {@link org.apache.sis.metadata.iso.identification.DefaultAssociatedResource#getName() name} /
     *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getTitle() title}:<br>
     *       a human-readable designation for parent, children or other related resources.
     *       May be omitted if too expensive to compute.</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getMetadataScopes() metadataScope} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadataScope#getResourceScope() resourceScope}:<br>
     *       {@link org.opengis.metadata.maintenance.ScopeCode#DATASET} if the resource is a {@link DataSet}, or
     *       {@link org.opengis.metadata.maintenance.ScopeCode#SERVICE} if the resource is a web service, or
     *       {@link org.opengis.metadata.maintenance.ScopeCode#SERIES} or
     *       {@link org.opengis.metadata.maintenance.ScopeCode#INITIATIVE}
     *       if the resource is an {@link Aggregate} other than a transfer aggregate.</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getContentInfo() contentInfo} /
     *       {@link org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription#getFeatureTypeInfo() featureType} /
     *       {@link org.apache.sis.metadata.iso.content.DefaultFeatureTypeInfo#getFeatureTypeName() featureTypeName}:<br>
     *       names of feature types included in this resource. Example: “bridge”, “road”, “river”. <i>etc.</i></li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getContentInfo() contentInfo} /
     *       {@link org.apache.sis.metadata.iso.content.DefaultCoverageDescription#getAttributeGroups() attributeGroup} /
     *       {@link org.apache.sis.metadata.iso.content.DefaultAttributeGroup#getAttributes() attribute} /
     *       {@link org.apache.sis.metadata.iso.content.DefaultRangeDimension#getSequenceIdentifier() sequenceIdentifier}:<br>
     *       sample dimension names (or band numbers in simpler cases) of coverages or rasters included in this resource.</li>
     * </ul>
     *
     * @return information about this resource. Should not be {@code null}.
     * @throws DataStoreException if an error occurred while reading the metadata.
     *
     * @see DataStore#getMetadata()
     */
    Metadata getMetadata() throws DataStoreException;

    /**
     * Returns the capabilities of this resource.
     * Resources may have different capabilites based on the type and context.<br>
     * Example : {@link Capability#WRITABLE} can be found on {@link Aggregate} or
     * {@link FeatureSet} to indicate the resource support writing operations.
     *
     * <p>The default implementation returns an empty Set.</p>
     *
     * @return Set of {@link Capability}, never null, unmodifiable, can be empty.
     */
    default Set<Capability> getCapabilities() {
        return Collections.EMPTY_SET;
    }

}
