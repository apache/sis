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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.sis.internal.storage.AbstractResource;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.ReadOnlyStorageException;
import org.apache.sis.storage.Resource;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;

/**
 * Folder aggregation of resources.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
class FolderAggregate extends AbstractResource implements Aggregate {

    /**
     * Used to sort resources by type and name
     */
    private static final Comparator<Resource> RESOURCE_COMPARATOR = new Comparator<Resource>() {
        @Override
        public int compare(Resource o1, Resource o2) {
            if (o1 instanceof FolderAggregate && !(o2 instanceof FolderAggregate)) {
                return -1;
            } else if (o2 instanceof FolderAggregate) {
                return +1;
            }
            try {
                String m1 = getIdentifier(o1.getMetadata());
                if (m1==null) m1 = "";
                String m2 = getIdentifier(o2.getMetadata());
                if (m2==null) m2 = "";
                return m1.compareTo(m2);

            } catch (DataStoreException ex) {
                return 0;
            }
        }
    };

    private final FolderStore store;
    private final FolderAggregate parent;
    private final Path path;
    private final Metadata metadata;
    private List<Resource> resources;

    FolderAggregate(FolderStore store, FolderAggregate parent, Path path) {
        super(store, null);
        this.store = store;
        this.parent = parent;
        this.path = path;

        final MetadataBuilder mb = new MetadataBuilder();
        mb.addIdentifier(null, path.getFileName().toString(), MetadataBuilder.Scope.ALL);
        metadata =  mb.build(true);
    }


    @Override
    public Metadata getMetadata() throws DataStoreException {
        return metadata;
    }

    @Override
    public synchronized Collection<Resource> components() throws DataStoreException {
        if (resources==null) {
            resources = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                final Iterator<Path> ite = stream.iterator();
                while (ite.hasNext()) {
                    final Path candidate = ite.next();
                    if (Files.isDirectory(candidate)) {
                        resources.add(new FolderAggregate(store, this, candidate));
                    } else {
                        //test it
                        try {
                            final DataStore store = DataStores.open(candidate);
                            resources.add(store);
                        } catch (DataStoreException ex) {
                            //not a known type file, skip it
                        }
                    }
                }
            } catch (IOException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }
            Collections.sort(resources, RESOURCE_COMPARATOR);
            resources = Collections.unmodifiableList(resources);
        }
        return resources;
    }

    @Override
    public synchronized Resource add(Resource resource) throws DataStoreException, ReadOnlyStorageException {
        //TODO : we should try to copy the resource files if it based on a filesystem,
        //but the API do not provide thoses informations
        return Aggregate.super.add(resource);
    }

    @Override
    public synchronized void remove(Resource resource) throws DataStoreException, ReadOnlyStorageException {
        Aggregate.super.remove(resource);
    }

    synchronized void close() throws DataStoreException {
        if (resources != null) {
            //close all children resources
            for (Resource r : resources) {
                if (r instanceof DataStore) {
                    ((DataStore) r).close();
                } else if (r instanceof FolderAggregate) {
                    ((FolderAggregate) r).close();
                }
            }
        }
    }

    private static String getIdentifier(Metadata metadata) {
        final Collection<? extends Identification> identifications = metadata.getIdentificationInfo();
        for (Identification identification : identifications) {
            final Citation citation = identification.getCitation();
            if (citation != null) {
                for (Identifier identifier : citation.getIdentifiers()) {
                    return identifier.getCode();
                }
            }
        }
        return null;
    }

}
