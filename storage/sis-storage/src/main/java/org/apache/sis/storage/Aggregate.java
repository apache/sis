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

import java.util.Collection;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.storage.Resources;


/**
 * A collection of resources. An aggregate can have two or more components.
 * Each component can be another aggregate, thus forming a tree of resources.
 * Different kinds of aggregate may exist for various reasons, for example (adapted from ISO 19115):
 *
 * <ul class="verbose">
 *   <li><b>Series:</b> a generic collection of resources that share similar characteristics
 *       (theme, date, resolution, <i>etc.</i>). The exact definition is determined by the data provider.
 *       See {@link org.opengis.metadata.maintenance.ScopeCode#SERIES} for more examples.</li>
 *   <li><b>Sensor series:</b> a collection of resources observed by the same sensor.</li>
 *   <li><b>Platform series:</b> a collection of resources observed by sensors installed on the same platform.
 *       The {@linkplain #components() components} of a platform series are <cite>sensor series</cite>.
 *       Those components usually share the same geospatial geometry.</li>
 *   <li><b>Production series:</b> a collection of resources produced using the same process. Members of a production
 *       series share {@linkplain org.apache.sis.metadata.iso.DefaultMetadata#getResourceLineages() lineage} and
 *       {@linkplain org.apache.sis.metadata.iso.lineage.DefaultLineage#getProcessSteps() processing history}.</li>
 *   <li><b>Initiative:</b> a collection of resources related by their participation in a common initiative.</li>
 *   <li><b>Transfer aggregate:</b> a set of resources collected for the purpose of transfer.
 *       The {@linkplain #components() components} may be the results of an ad hoc query, for example on a Web Service.</li>
 * </ul>
 *
 * The same resource may be part of more than one aggregate. For example the same resource could be part of
 * a <cite>production series</cite> and a <cite>transfer aggregate</cite>. In Apache SIS implementation,
 * those two kinds of aggregate will usually be implemented by different {@link DataStore} instances.
 *
 * <div class="section">Metadata</div>
 * Aggregates should have {@link #getMetadata() metadata} /
 * {@link org.apache.sis.metadata.iso.DefaultMetadata#getMetadataScopes() metadataScope} /
 * {@link org.apache.sis.metadata.iso.DefaultMetadataScope#getResourceScope() resourceScope} sets to
 * {@link org.opengis.metadata.maintenance.ScopeCode#SERIES} or
 * {@link org.opengis.metadata.maintenance.ScopeCode#INITIATIVE} if applicable.
 * If not too expensive to compute, the names of all components should be listed as
 * {@linkplain org.apache.sis.metadata.iso.identification.AbstractIdentification#getAssociatedResources()
 * associated resources} with an {@link org.opengis.metadata.identification.AssociationType#IS_COMPOSED_OF} relation.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public interface Aggregate extends Resource {
    /**
     * Returns the children resources of this aggregate. The returned collection contains
     * the resources listed by their name in the following {@linkplain #getMetadata() metadata} elements.
     * The returned collection may contain more resources if the metadata are incomplete,
     * and the resources do not need to be in the same order:
     *
     * <blockquote><code><b>this</b>.metadata</code> /
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     * {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getAssociatedResources() associatedResource}
     * with {@link org.opengis.metadata.identification.AssociationType#IS_COMPOSED_OF}</blockquote>
     *
     * The name of each child resource in the returned collection is given by the following metadata element:
     *
     * <blockquote><code><b>child</b>.metadata</code> /
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     * {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getCitation() citation} /
     * {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getTitle() title}</blockquote>
     *
     * <div class="section">Lazy resource instantiation</div>
     * If the collection instantiates components only when first needed, and if a checked exception occurs
     * during invocation of a {@link Collection} or {@link java.util.Iterator} method, then the collection
     * or the iterator should wrap the exception in a {@link org.apache.sis.util.collection.BackingStoreException}.
     *
     * @return all children resources that are components of this aggregate. Never {@code null}.
     * @throws DataStoreException if an error occurred while fetching the components.
     */
    Collection<Resource> components() throws DataStoreException;

    /**
     * Adds a new {@code Resource} in this {@code Aggregate}.
     * The given {@link Resource} will be copied, and the <cite>effectively added</cite> resource returned.
     * The effectively added resource may differ from the given resource in many aspects.
     * The possible changes may include the followings but not only:
     * <ul>
     *  <li>types and properties names</li>
     *  <li>{@link CoordinateReferenceSystem}</li>
     *  <li>{@link Metadata}</li>
     * </ul>
     *
     * <div class="note"><b>Warning:</b>
     * copying informations between stores may produce differences in many aspects.
     * The range of changes depends both on the original {@link Resource} structure
     * and the target {@code Resource} structure. If the differences are too large,
     * then this {@code Aggregate} may throw an exception.
     * </div>
     *
     * <p>The default implementation throws {@link ReadOnlyStorageException}.</p>
     *
     * @param  resource  the resource to copy in this {@code Aggregate}.
     * @return the effectively added resource. May be {@code resource} itself if it has been added verbatim.
     * @throws ReadOnlyStorageException if this instance does not support write operations.
     * @throws DataStoreException if the given resource can not be stored in this {@code Aggregate} for another reason.
     */
    default Resource add(Resource resource) throws ReadOnlyStorageException, DataStoreException {
        throw new ReadOnlyStorageException(this, Resources.Keys.StoreIsReadOnly);
    }

    /**
     * Removes a {@code Resource} from this {@code Aggregate}.
     * This operation is destructive: the {@link Resource} and it's related data will be removed.
     *
     * <p>The default implementation throws {@link ReadOnlyStorageException}.</p>
     *
     * @param  resource  child resource to remove, should not be null.
     * @throws ReadOnlyStorageException if this instance does not support write operations.
     * @throws DataStoreException if the given resource could not be removed for another reason.
     */
    default void remove(Resource resource) throws ReadOnlyStorageException, DataStoreException {
        throw new ReadOnlyStorageException(this, Resources.Keys.StoreIsReadOnly);
    }
}
