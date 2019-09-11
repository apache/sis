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


/**
 * An {@link Aggregate} with writing capabilities. {@code WritableAggregate} inherits the reading capabilities from its
 * parent and adds the capabilities to {@linkplain #add(Resource) add} or {@linkplain #remove(Resource) remove} resources.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface WritableAggregate extends Aggregate {
    /**
     * Adds a new {@code Resource} in this {@code Aggregate}.
     * The given {@link Resource} will be copied, and the <cite>effectively added</cite> resource returned.
     * The effectively added resource may differ from the given resource in many aspects.
     * The possible changes may include the followings but not only:
     *
     * <ul>
     *  <li>types and properties names</li>
     *  <li>{@link org.opengis.referencing.crs.CoordinateReferenceSystem}</li>
     *  <li>{@link org.opengis.metadata.Metadata}</li>
     * </ul>
     *
     * <div class="note"><b>Warning:</b>
     * copying information between stores may produce differences in many aspects.
     * The range of changes depends both on the original {@link Resource} structure
     * and the target {@code Resource} structure. If the differences are too large,
     * then this {@code Aggregate} may throw an exception.
     * </div>
     *
     * @param  resource  the resource to copy in this {@code Aggregate}.
     * @return the effectively added resource. May be {@code resource} itself if it has been added verbatim.
     * @throws DataStoreException if the given resource can not be stored in this {@code Aggregate}.
     */
    Resource add(Resource resource) throws DataStoreException;

    /**
     * Removes a {@code Resource} from this {@code Aggregate}.
     * The given resource should be one of the instances returned by {@link #components()}.
     * This operation is destructive in two aspects:
     *
     * <ul>
     *   <li>The {@link Resource} and it's data will be deleted from the {@link DataStore}.</li>
     *   <li>The given resource may become invalid and should not be used anymore after this method call.</li>
     * </ul>
     *
     * @param  resource  child resource to remove from this {@code Aggregate}.
     * @throws DataStoreException if the given resource could not be removed.
     */
    void remove(Resource resource) throws DataStoreException;
}
