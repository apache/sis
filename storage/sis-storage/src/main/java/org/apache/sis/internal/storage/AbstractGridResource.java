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
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


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
     * Validate the {@code range} argument given to {@link #read(GridGeometry, int...)}.
     * This method verifies that all indices are between 0 and {@code numSampleDimensions},
     * but does not verify if there is duplicated indices since such duplication is allowed.
     *
     * @param  numSampleDimensions  number of sample dimensions.
     * @param  range  the {@code range} argument given by the user. May be null or empty.
     * @return the 0-based indices of ranges to use. May be a copy of the given {@code range} argument or a sequence
     *         from 0 to {@code numSampleDimensions} exclusive if {@code range} was {@code null} or empty.
     */
    protected final int[] validateRangeArgument(final int numSampleDimensions, int[] range) {
        ArgumentChecks.ensureStrictlyPositive("numSampleDimensions", numSampleDimensions);
        if (range == null || range.length == 0) {
            return ArraysExt.range(0, numSampleDimensions);
        }
        range = range.clone();
        for (int i=0; i<range.length; i++) {
            final int r = range[i];
            if (r < 0 || r >= numSampleDimensions) {
                throw new IllegalArgumentException(Resources.forLocale(getLocale()).getString(
                        Resources.Keys.InvalidSampleDimensionIndex_2, i, numSampleDimensions - 1));
            }
        }
        return range;
    }
}
