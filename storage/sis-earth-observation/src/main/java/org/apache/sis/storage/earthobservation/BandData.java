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
package org.apache.sis.storage.earthobservation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.internal.storage.GridResourceWrapper;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.DefaultMetadata;


/**
 * A band in a Landsat data set. Each band is represented by a separated GeoTIFF file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class BandData extends GridResourceWrapper {
    /**
     * The data store that contains this band.
     * Also the object on which to perform synchronization locks.
     */
    private final LandsatStore parent;

    /**
     * The band for which this instance provides data.
     */
    private final Band band;

    /**
     * Identifier of the band for which this instance provides data.
     *
     * @see #getIdentifier()
     */
    private final LocalName identifier;

    /**
     * Filename of the file to read for band data.
     * This is relative to {@link LandsatStore#directory}.
     */
    private final String filename;

    /**
     * The metadata, created when first needed.
     */
    private Metadata metadata;

    /**
     * Creates a new resource for the band identified by the given identifier.
     */
    BandData(final LandsatStore parent, final Band band, final LocalName identifier, final String filename) {
        this.parent     = parent;
        this.band       = band;
        this.identifier = identifier;
        this.filename   = filename;
    }

    /**
     * Creates the GeoTIFF reader and get the first image from it.
     */
    @Override
    protected GridCoverageResource createSource() throws DataStoreException {
        final Path file;
        if (parent.directory != null) {
            file = parent.directory.resolve(filename);
        } else {
            file = Paths.get(filename);
        }
        return new GeoTiffStore(parent, parent.getProvider(), new StorageConnector(file), true).components().get(0);
    }

    /**
     * Returns the resource persistent identifier. The name is the {@link Band#name()}
     * and the scope (namespace) is the name of the directory that contains this band.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(identifier);
    }

    /**
     * Returns information about this resource.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            /*
             * The GeoTIFF store implementation is known to create instances of `DefaultMetadata`,
             * and this instance is kept modifiable because the `hidden` argument specified to the
             * `GeoTiffStore` constructor was `true`. This is an undocumented feature that may be
             * modified in future SIS versions.
             */
            final DefaultMetadata md = (DefaultMetadata) source().getMetadata();
            for (final Identification id : md.getIdentificationInfo()) {
                final DefaultCitation c = (DefaultCitation) id.getCitation();
                if (c != null) {
                    c.setTitle(band.name);
                    break;
                }
            }
            md.transitionTo(DefaultMetadata.State.FINAL);
            metadata = md;
        }
        return metadata;
    }
}
