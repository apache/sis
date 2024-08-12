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
package org.apache.sis.storage.geopackage;

import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.sql.DataAccess;


/**
 * A callback for creating custom resources from the content of a SQL spatial database.
 * This handler needs to be implemented only for resources that are not supported by
 * the default implementation of {@link GpkgStore}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public interface ContentHandler {
    /**
     * Returns whether this handler can create a resource for the given content.
     * The returned value should be cached by {@link GpkgStore}.
     *
     * @param  content  Geopackage content definition.
     * @return true if this handler can process the content.
     */
    boolean canOpen(Content content);

    /**
     * Opens as a resource the content specified by the given record.
     * This method does not need to cache the resource, as caching is done by {@link GpkgStore}.
     *
     * @param  dao      the data access object to use for low-level operations.
     * @param  content  a description of the desired resource.
     * @return the resource created by this handler.
     * @throws DataStoreException if an error occurred while opening resource.
     */
    Resource open(DataAccess dao, Content content) throws DataStoreException;

    /**
     * If this handle can add the given resource, adds it and returns the row to add in the content table.
     * OTherwise returns {@code null}.
     *
     * <h4>Default implementation</h4>
     * The default implementation returns {@code null}.
     * This is suitable to read-only Geopackage files.
     *
     * @param  dao       the data access object to use for low-level operations.
     * @param  resource  the resource to write.
     * @return the row to add in the content table, or {@code null} if the given resource cannot be written by this handler.
     * @throws DataStoreException if an error occurred while writing the resource.
     *
     * @see GpkgStore#add(Resource)
     */
    default Content addIfSupported(DataAccess dao, Resource resource) throws DataStoreException {
        return null;
    }

    /**
     * If the given resource is managed by this handler, removes it.
     * Otherwise, does nothing and returns {@code false}.
     * If the resource is removed, then the dependent data that are no longer used should also be deleted.
     *
     * <h4>Default implementation</h4>
     * The default implementation returns {@code false}.
     *
     * @param  resource  the resource to delete.
     * @return whether the given resource is managed by this handler.
     * @throws DataStoreException if an error occurred while deleting resource.
     *
     * @see GpkgStore#remove(Resource)
     */
    default boolean remove(DataAccess dao, Content content, Resource resource) throws DataStoreException {
        return false;
    }
}
