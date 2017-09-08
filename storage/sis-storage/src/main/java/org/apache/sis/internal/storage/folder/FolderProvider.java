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
package org.apache.sis.internal.storage.folder;

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * The provider of {@link FolderStore} instances.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class FolderProvider extends DataStoreProvider {

    public static final String NAME = "folder";

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR = URIDataStore.Provider.descriptor(NAME);

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        final URI uri = connector.getStorageAs(URI.class);
        try {
            //check path is valid
            final Path path = Paths.get(uri);
            if (Files.isDirectory(path)) {
                return new ProbeResult(true, null, null);
            }
        } catch (FileSystemNotFoundException ex) {
            //nothing we can do, may happen a lot
        }
       return new ProbeResult(false, null, null);
    }

    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        final URI uri = connector.getStorageAs(URI.class);
        return new FolderStore(uri);
    }

    @Override
    public DataStore open(ParameterValueGroup parameters) throws DataStoreException {
        return new FolderStore(parameters);
    }

}
