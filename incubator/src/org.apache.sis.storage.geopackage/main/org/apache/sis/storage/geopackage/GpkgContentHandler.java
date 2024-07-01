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

import org.apache.sis.storage.geopackage.privy.Record;
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;

/**
 * Entry point for content data support.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface GpkgContentHandler {

    /**
     * Get handler priority.
     *
     * @return higher values get listed first. default is 0.
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Test if content is supported.
     *
     * @param content geopackage content definition
     * @return true if handler can process the content
     */
    boolean canOpen(Record.Content content);

    /**
     * Open given database content record as a resource.
     *
     * @param store not null
     * @param content not null
     * @return created resource
     * @throws DataStoreException if an error occured while opening resource
     */
    GpkgContentResource open(GpkgStore store, Record.Content content) throws DataStoreException;

    /**
     * Test if this handler can import given data in geopackage.
     *
     * @param resource to import, not null
     * @return true if resource can be imported
     */
    boolean canAdd(Resource resource);

    /**
     *
     * @param store in import into, not null
     * @param content description of the resource, this record is not save yet.
     *         handler is responsible for calling {@linkplain GpkgStore#saveContentRecord(org.apache.sis.storage.geopackage.privy.Record.Content) }.
     * @param resource to import, not null
     * @return new resource in the geopackage, not null
     * @throws DataStoreException if an error occured while importing resource
     */
    GpkgContentResource add(GpkgStore store, Record.Content content, Resource resource) throws DataStoreException;

    /**
     * Delete the resource from database with any related stored datas.
     *
     * @param resource to delete
     * @throws DataStoreException if an error occured while deleting resource
     */
    void delete(GpkgContentResource resource) throws DataStoreException;

    /**
     * @return list of extensions for this content type. may be empty.
     */
    List<GpkgExtension> getExtensions(GpkgStore store);

}
