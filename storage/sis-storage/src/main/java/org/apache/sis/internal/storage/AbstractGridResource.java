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
package org.apache.sis.internal.storage;

import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.WarningListeners;


/**
 * Base class for implementations of {@link GridCoverageResource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public abstract class AbstractGridResource extends AbstractResource implements GridCoverageResource {
    /**
     * Creates a new resource.
     *
     * @param listeners  the set of registered warning listeners for the data store, or {@code null} if none.
     */
    protected AbstractGridResource(final WarningListeners<DataStore> listeners) {
        super(listeners);
    }

    /**
     * Creates a new resource with the same warning listeners than the given resource,
     * or {@code null} if the listeners are unknown.
     *
     * @param resource  the resources from which to get the listeners, or {@code null} if none.
     */
    protected AbstractGridResource(final Resource resource) {
        super(resource);
    }

    /**
     * Returns the grid geometry envelope, or {@code null} if unknown.
     * This implementation fetches the envelope from the grid geometry instead than from metadata.
     *
     * @return the grid geometry envelope, or {@code null}.
     * @throws DataStoreException if an error occurred while computing the grid geometry.
     */
    @Override
    public Envelope getEnvelope() throws DataStoreException {
        final GridGeometry gg = getGridGeometry();
        if (gg != null && gg.isDefined(GridGeometry.ENVELOPE)) {
            return gg.getEnvelope();
        }
        return null;
    }

    /**
     * Invoked the first time that {@link #getMetadata()} is invoked. The default implementation populates
     * metadata based on information provided by {@link #getIdentifier()} and {@link #getGridGeometry()}.
     * Subclasses should override if they can provide more information.
     *
     * @param  metadata  the builder where to set metadata properties.
     * @throws DataStoreException if an error occurred while reading metadata from the data store.
     */
    @Override
    protected void createMetadata(final MetadataBuilder metadata) throws DataStoreException {
        super.createMetadata(metadata);
        metadata.addSpatialRepresentation(null, getGridGeometry(), false);
        for (final SampleDimension band : getSampleDimensions()) {
            metadata.addNewBand(band);
        }
    }

    /**
     * Verifies the validity of the grid geometry specified by the user
     * for a {@link #read(GridGeometry, int...)} operation.
     *
     * @param  domain  the user-specified grid geometry, or {@code null}.
     * @return the grid geometry to use for reading.
     * @throws DataStoreException if an error occurred while validating the grid geometry.
     */
    protected final GridGeometry validateReadArgument(GridGeometry domain) throws DataStoreException {
        final GridGeometry stored = getGridGeometry();
        if (domain == null) {
            return stored;
        }
        final int dimension = stored.getDimension();
        final int ad = domain.getDimension();
        if (ad != stored.getDimension()) {
            throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3, "domain", dimension, ad));
        }
        if (domain.isDefined(GridGeometry.GRID_TO_CRS) &&
                !stored.getGridToCRS(PixelInCell.CELL_CENTER).equals(domain.getGridToCRS(PixelInCell.CELL_CENTER)))
        {
            throw new IllegalArgumentException("Mismatched grid to CRS transform.");
        }
        return domain;
    }
}
