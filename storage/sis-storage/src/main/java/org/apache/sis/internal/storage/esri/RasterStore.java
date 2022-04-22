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
package org.apache.sis.internal.storage.esri;

import java.util.Optional;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.storage.PRJDataStore;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.metadata.sql.MetadataStoreException;


/**
 * Base class for the implementation of ASCII Grid or raw binary store.
 * This base class manages the reading of following auxiliary files:
 *
 * <ul>
 *   <li>{@code *.stx} for statistics about bands.</li>
 *   <li>{@code *.clr} for the image color map.</li>
 *   <li>{@code *.prj} for the CRS definition.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
abstract class RasterStore extends PRJDataStore implements GridCoverageResource {
    /**
     * Keyword for the number of rows in the image.
     */
    static final String NROWS = "NROWS";

    /**
     * Keyword for the number of columns in the image.
     */
    static final String NCOLS = "NCOLS";

    /**
     * The image size together with the "grid to CRS" transform.
     * This is also used as a flag for checking whether the
     * {@code "*.prj"} file and the header have been read.
     */
    GridGeometry gridGeometry;

    /**
     * The metadata object, or {@code null} if not yet created.
     */
    Metadata metadata;

    /**
     * Creates a new raster store from the given file, URL or stream.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (file, URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    RasterStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        listeners.useWarningEventsOnly();
    }

    /**
     * Builds metadata and assigns the result to the {@link #metadata} field.
     *
     * @param  formatName  name of the raster format.
     * @param  formatKey   key of format description in the {@code SpatialMetadata} database.
     * @throws DataStoreException if an error occurred during the parsing process.
     */
    final void createMetadata(final String formatName, final String formatKey) throws DataStoreException {
        final GridGeometry gridGeometry = getGridGeometry();        // May cause parsing of header.
        final MetadataBuilder builder = new MetadataBuilder();
        try {
            builder.setPredefinedFormat(formatKey);
        } catch (MetadataStoreException e) {
            builder.addFormatName(formatName);
            listeners.warning(e);
        }
        builder.addResourceScope(ScopeCode.COVERAGE, null);
        builder.addEncoding(encoding, MetadataBuilder.Scope.METADATA);
        builder.addSpatialRepresentation(null, gridGeometry, true);
        try {
            builder.addExtent(gridGeometry.getEnvelope());
        } catch (TransformException e) {
            throw new DataStoreReferencingException(getLocale(), formatName, getDisplayName(), null).initCause(e);
        }
        /*
         * Do not add the sample dimensions because in current version computing the sample dimensions without
         * statistics requires loading the full image. Even if `GridCoverage.getSampleDimensions()` exists and
         * could be used opportunistically, we do not use it in order to keep a deterministic behavior
         * (we do not want the metadata to vary depending on the order in which methods are invoked).
         */
        addTitleOrIdentifier(builder);
        builder.setISOStandards(false);
        metadata = builder.buildAndFreeze();
    }

    /**
     * Returns the spatiotemporal extent of the raster file.
     *
     * @return the spatiotemporal resource extent.
     * @throws DataStoreException if an error occurred while computing the envelope.
     * @hidden
     */
    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return Optional.ofNullable(getGridGeometry().getEnvelope());
    }
}
