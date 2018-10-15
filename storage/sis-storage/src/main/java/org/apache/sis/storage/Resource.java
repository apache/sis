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

import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;


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
 * @version 1.0
 *
 * @see Aggregate#components()
 *
 * @since 0.8
 * @module
 */
public interface Resource {
    /**
     * Returns the resource persistent identifier.
     * This identifier can be used to uniquely identify a resource in the containing {@link DataStore}.
     * For this identifier to be reliable the following conditions must hold:
     *
     * <ul>
     *   <li>It shall be unique in the {@link DataStore} which contains it, if there is one.</li>
     *   <li>It's value shall not change after closing and reopening the {@link DataStore} on the same data.</li>
     *   <li>It should be consistent with the <code>{@linkplain #getMetadata()}/​identificationInfo/​citation/​identifier</code> value.</li>
     * </ul>
     *
     * If any of above conditions is not met, then this identifier should be {@code null}.
     * This case may happen when a resource is an intermediate result of an ongoing process
     * or is a temporary resource generated on-the-fly, for example a sensor event.
     *
     * @return a persistent identifier unique within the data store, or {@code null} if this resource has no such identifier.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     *
     * @see DataStore#getIdentifier()
     * @see DataStore#findResource(String)
     *
     * @since 1.0
     */
    GenericName getIdentifier() throws DataStoreException;

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
     *       {@code ScopeCode.INITIATIVE}
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
     * Registers a listener that is notified each time a change occurs in the resource content or structure.
     * The resource will call the {@link ChangeListener#changeOccured(ChangeEvent)}
     * method when a new event matching the {@code eventType} is produced.
     *
     * <p>Registering a listener for a given {@code eventType} also register the listener for all sub-types.
     * The same listener can be added multiple times for different even type.
     * Adding many times the same listener with the same even type has no effect:
     * the listener will only be called once per event.</p>
     *
     * @todo When adding a listener to an aggregate, should the listener be added to all components?
     *       In other words, should listeners in a tree node also listen to events from all children?
     *
     * <p>The resource is not required to keep a reference to the listener.
     * For example the resource may discard a listener if no event of the given type happen on this resource.</p>
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  listener   listener to notify about changes.
     * @param  eventType  type of {@linkplain ChangeEvent} to listen (can not be {@code null}).
     */
    <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType);

    /**
     * Unregisters a listener previously added to this resource for the given type of events.
     * The {@code eventType} must be the exact same class than the one given to the {@code addListener(…)} method.
     *
     * <div class="note"><b>Example:</b>
     * if the same listener has been added for {@code ChangeEvent} and {@code StructuralChangeEvent}, that listener
     * will be notified only once for all {@code ChangeEvent}s. If that listener is removed for {@code ChangeEvent},
     * then the listener will still receive {@code StructuralChangeEvent}s.</div>
     *
     * <p>Calling multiple times this method with the same listener and event type or a listener
     * which is unknown to this resource will have no effect and will not raise an exception.</p>
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  listener   listener to stop notifying about changes.
     * @param  eventType  type of {@linkplain ChangeEvent} which were listened (can not be {@code null}).
     */
    <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType);
}
