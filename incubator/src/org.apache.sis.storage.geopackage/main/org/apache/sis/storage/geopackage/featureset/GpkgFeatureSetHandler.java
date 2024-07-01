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
package org.apache.sis.storage.geopackage.featureset;

import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.geopackage.Gpkg;
import org.apache.sis.storage.geopackage.GpkgContentHandler;
import org.apache.sis.storage.geopackage.GpkgContentResource;
import org.apache.sis.storage.geopackage.GpkgStore;
import org.apache.sis.storage.geopackage.GpkgExtension;
import org.apache.sis.storage.geopackage.privy.Record;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GpkgFeatureSetHandler implements GpkgContentHandler {

    @Override
    public boolean canOpen(Record.Content content) {
        return Gpkg.DATATYPE_FEATURES.equalsIgnoreCase(content.dataType)
            || Gpkg.DATATYPE_ATTRIBUTES.equalsIgnoreCase(content.dataType);
    }

    @Override
    public boolean canAdd(Resource resource) {
        //TODO
        return false;
    }

    @Override
    public GpkgContentResource open(GpkgStore store, Record.Content content) {
        return new GpkgFeatureSet(this, store, content);
    }

    @Override
    public GpkgContentResource add(GpkgStore store, Record.Content content, Resource resource) throws DataStoreException {
        throw new DataStoreException("Not supported yet.");
    }

    @Override
    public void delete(GpkgContentResource resource) throws DataStoreException{
        throw new DataStoreException("Not supported yet.");
    }

    @Override
    public List<GpkgExtension> getExtensions(GpkgStore store) {
        return List.of();
    }
}
