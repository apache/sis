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

/**
 * Geopackage extension.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class GpkgExtension {

    protected final GpkgStore store;

    public GpkgExtension(GpkgStore store) {
        this.store = store;
    }

    /**
     * @return true if extension is installed in the database
     * @throws DataStoreException if extension detection failed
     */
    public abstract boolean isInstalled() throws DataStoreException;

    /**
     * Install or declare the extension in the database.
     *
     * @throws DataStoreException if an error occured while installing the extension
     */
    public abstract void install() throws DataStoreException;

}
