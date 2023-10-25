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
package org.apache.sis.storage.coveragejson;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.coveragejson.binding.Coverage;
import org.apache.sis.storage.coveragejson.binding.CoverageCollection;
import org.apache.sis.storage.coveragejson.binding.CoverageJsonObject;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.URIDataStore;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.WritableAggregate;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;

/**
 * A data store backed by Coverage-JSON files.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CoverageJsonStore extends DataStore implements WritableAggregate {

    /**
     * The {@link CoverageJsonStoreProvider#LOCATION} parameter value, or {@code null} if none.
     * This is used for information purpose only, not for actual reading operations.
     *
     * @see #getOpenParameters()
     */
    private final URI location;

    /**
     * Same value than {@link #location} but as a path, or {@code null} if none.
     * Stored separately because conversion from path to URI back to path is not
     * looseness (relative paths become absolutes).
     */
    private final Path path;

    private boolean parsed;
    private final List<Resource> components = new ArrayList<>();

    CoverageJsonStore(CoverageJsonStoreProvider provider, StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        location = connector.getStorageAs(URI.class);
        path = connector.getStorageAs(Path.class);
    }

    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.ofNullable(URIDataStore.parameters(provider, location));
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addIdentifier(null, IOUtilities.filename(path), MetadataBuilder.Scope.ALL);
        return builder.buildAndFreeze();
    }

    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (!parsed) {
            parsed = true;
            if (Files.exists(path)) {
                try (Jsonb b = JsonbBuilder.create();
                     InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
                    final CoverageJsonObject obj = b.fromJson(in, CoverageJsonObject.class);

                    if (obj instanceof Coverage) {
                        final Coverage coverage = (Coverage) obj;
                        components.add(new CoverageResource(this, coverage));

                    } else if (obj instanceof CoverageCollection) {
                        final CoverageCollection col = (CoverageCollection) obj;
                        throw new UnsupportedOperationException("Coverage collection not supported yet.");
                    }

                } catch (Exception ex) {
                    throw new DataStoreException("Failed to parse coverage json object.", ex);
                }
            }
        }

        return Collections.unmodifiableList(components);
    }

    @Override
    public synchronized Resource add(Resource resource) throws DataStoreException {
        //ensure file is parsed
        components();

        if (resource instanceof GridCoverageResource) {
            final GridCoverageResource gcr = (GridCoverageResource) resource;
            final GridCoverage coverage = gcr.read(null);
            final Coverage binding = CoverageResource.gridCoverageToBinding(coverage);
            final CoverageResource jcr = new CoverageResource(this, binding);
            components.add(jcr);
            save();
            return jcr;
        }

        throw new DataStoreException("Only GridCoverage resource are supported");
    }

    @Override
    public synchronized void remove(Resource resource) throws DataStoreException {
        //ensure file is parsed
        components();

        for (int i = 0, n = components.size(); i < n ;i++) {
            if (components.get(i) == resource) {
                components.remove(i);
                save();
                return;
            }
        }
        throw new NoSuchDataException();
    }

    private synchronized void save() throws DataStoreException {
        //ensure file is parsed
        components();

        final int size = components.size();
        final String json;
        if (size == 1) {
            //single coverage
            final CoverageResource res = (CoverageResource) components.get(0);

            try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
                json = jsonb.toJson(res.getBinding());
            } catch (Exception ex) {
                throw new DataStoreException("Failed to create coverage json binding", ex);
            }
        } else {
            final CoverageCollection col = new CoverageCollection();
            col.coverages = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                final CoverageResource res = (CoverageResource) components.get(i);
                col.coverages.add(res.getBinding());
            }

            try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
                json = jsonb.toJson(col);
            } catch (Exception ex) {
                throw new DataStoreException("Failed to create coverage collection json binding", ex);
            }
        }

        try {
            Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new DataStoreException("Failed to save coverage-json", ex);
        }
    }

    @Override
    public void close() throws DataStoreException {
    }
}
