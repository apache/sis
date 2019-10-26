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
package org.apache.sis.internal.storage.query;

import java.util.List;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.storage.AbstractGridResource;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * The result of {@link CoverageQuery#execute(GridCoverageResource)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CoverageSubset extends AbstractGridResource {
    /**
     * The coverage resource instances to filter.
     */
    private final GridCoverageResource source;

    /**
     * The query for filtering the source coverage.
     */
    private final CoverageQuery query;

    /**
     * Creates a new coverage resource by filtering the given coverage using the given query.
     */
    CoverageSubset(final GridCoverageResource source, final CoverageQuery query) {
        super(source instanceof StoreListeners ? (StoreListeners) source : null);
        this.source = source;
        this.query  = query;
    }

    /**
     * Returns the valid extent of grid coordinates clipped to the area specified in the query.
     *
     * @return extent of grid coordinates clipped to the query.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store,
     *         or if the grid geometry can not be clipped to the query area.
     */
    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        GridGeometry domain = source.getGridGeometry();
        final GridGeometry sub = query.getDomain();
        if (sub != null) try {
            domain = domain.derive().subgrid(sub).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            final String msg = Resources.forLocale(getLocale()).getString(Resources.Keys.CanNotIntersectDataWithQuery);
            final Exception cause = getReferencingCause(e);
            if (cause != null) {
                throw new DataStoreReferencingException(msg, cause);
            } else {
                throw new DataStoreException(msg, e);
            }
        }
        return domain;
    }

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     * The returned list should never be empty.
     *
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        final List<SampleDimension> dimensions = source.getSampleDimensions();
        if (query.getRangeCount() == 0) {
            return dimensions;
        }
        final int[] range = query.getRange();
        final SampleDimension[] subset = new SampleDimension[range.length];
        for (int i=0; i<range.length; i++) {
            final int j = range[i];
            try {
                subset[i] = dimensions.get(j);
            } catch (IndexOutOfBoundsException e) {
                throw new DataStoreException(invalidRange(j), e);
            }
        }
        return UnmodifiableArrayList.wrap(subset);
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {
        if (domain == null) {
            domain = query.getDomain();
        }
        range = query.subrange(this, range);
        return source.read(domain, range);
    }

    /**
     * Creates an exception message for an invalid range index.
     */
    final String invalidRange(final int index) {
        return Resources.forLocale(getLocale()).getString(
                Resources.Keys.InvalidSampleDimensionIndex_2, query.getRangeCount() - 1, index);
    }
}
