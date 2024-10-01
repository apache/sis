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
package org.apache.sis.storage.gdal;

import java.util.List;
import java.util.Optional;
import org.apache.sis.storage.DataStoreException;
import org.opengis.parameter.ParameterValueGroup;


/**
 * A data store which has been identified as a sub-dataset.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Subdataset extends GDALStore {
    /**
     * Creates a new data store as a child of the given store.
     *
     * @param  parent  the parent data store.
     * @param  url     <abbr>URL</abbr> for <var>GDAL</var> of the data store to open.
     * @param  driver  name of the driver to use.
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    Subdataset(final GDALStore parent, final String url, final String driver) throws DataStoreException {
        super(parent, url, driver);
    }

    /**
     * Returns an empty value, as this data store is opened only indirectly.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.empty();
    }

    /**
     * Does nothing. We do not support at this time aggregates of aggregates,
     * because we do not know if they happen in practice and we want to avoid
     * the risk of infinite recursion.
     */
    @Override
    List<Subdataset> groupBySubset(final GDAL gdal) {
        return null;
    }

    /**
     * Does nothing since this resource should be closed by the parent store.
     * If the user does not close the parent store, this store will be closed
     * by the garbage collector.
     */
    @Override
    public void close() {
    }
}
