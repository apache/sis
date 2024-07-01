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

import org.apache.sis.storage.Resource;
import org.apache.sis.storage.event.StoreListeners;

/**
 * A resource of a geopackage database.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface GpkgContentResource extends Resource{

    /**
     * Used by matrix and matrixset instances to forward events.
     *
     * @return store listeners, not null.
     */
    StoreListeners listeners();

    /**
     * Get resource geopackage datastore.
     *
     * @return geopackage datastore, not null
     */
    GpkgStore getStore();

    /**
     * Get matching database content record.
     *
     * @return database record, not null.
     */
    org.apache.sis.storage.geopackage.privy.Record.Content getRecord();

    /**
     * Get handler which created this resource.
     *
     * @return handler, not null
     */
    GpkgContentHandler getHandler();

}
