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
package org.apache.sis.storage;

import java.util.Arrays;
import java.util.List;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.GridClippingMode;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * The result of {@link CoverageQuery#execute(GridCoverageResource)}.
 * This implementation merges the domain and range specified by the query with
 * arguments of {@link GridCoverageResource#read(GridGeometry, int...)} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
final class CoverageSubset extends AbstractGridCoverageResource {
    /**
     * The coverage resource instance which provides the data.
     */
    private final GridCoverageResource source;

    /**
     * The domain and range to read from the {@linkplain #source} coverage.
     */
    private final CoverageQuery query;

    /**
     * Creates a new coverage resource by filtering the given coverage using the given query.
     * This given query is stored as-is (it is not cloned neither optimized).
     *
     * @param source  the coverage resource instances which provides the data.
     * @param query   the domain and range to read from the {@code source} coverage.
     */
    CoverageSubset(final GridCoverageResource source, final CoverageQuery query) {
        super(source instanceof AbstractResource ? ((AbstractResource) source).listeners : null, false);
        this.source = source;
        this.query  = query;
    }

    /**
     * Returns the valid extent of grid coordinates clipped to the area specified in the query.
     * It should be the geometry of the coverage that we get when invoking {@link #read read(…)}
     * with {@code null} arguments, but this is not guaranteed.
     * The returned grid geometry may be approximate.
     *
     * @return extent of grid coordinates clipped to the query.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store,
     *         or if the grid geometry can not be clipped to the query area.
     */
    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        /*
         * The grid should be fully contained in the resource specified at construction time,
         * including margin expansion (if any), because data may not exist outside that area.
         * This requirement is specified by `GridClippingMode.STRICT`.
         */
        return clip(source.getGridGeometry(), GridRoundingMode.NEAREST, GridClippingMode.STRICT);
    }

    /**
     * Clips the given domain to the area of interest specified by the query. If any grid geometry is null,
     * the other one is returned. The {@code domain} argument should be the domain to read as specified to
     * {@link #read(GridGeometry, int...)}, or the full {@code CoverageSubset} domain if no value were given
     * to the {@code read(…)} method.
     *
     * @param  domain    the domain requested in a read operation, or {@code null}.
     * @param  rounding  whether to clip to nearest box or an enclosing box.
     * @param  clipping  whether to clip the resulting extent to the specified {@code domain} extent.
     * @return intersection of the given grid geometry with the query domain.
     * @throws DataStoreException if the intersection can not be computed.
     */
    private GridGeometry clip(final GridGeometry domain, final GridRoundingMode rounding, final GridClippingMode clipping)
            throws DataStoreException
    {
        final GridGeometry areaOfInterest = query.getSelection();
        if (domain == null) return areaOfInterest;
        if (areaOfInterest == null) return domain;
        try {
            final GridDerivation derivation = domain.derive().rounding(rounding).clipping(clipping);
            final int expansion = query.getSourceDomainExpansion();
            if (expansion != 0) {
                final int[] margins = new int[domain.getDimension()];
                Arrays.fill(margins, expansion);
                derivation.margin(margins);
            }
            return derivation.subgrid(areaOfInterest).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            final String msg = Resources.forLocale(listeners.getLocale())
                    .getString(Resources.Keys.CanNotIntersectDataWithQuery_1, listeners.getSourceName());
            final Throwable cause = e.getCause();
            if (cause instanceof FactoryException || cause instanceof TransformException) {
                throw new DataStoreReferencingException(msg, cause);
            } else if (e instanceof DisjointExtentException) {
                throw new NoSuchDataException(msg, e);
            } else {
                throw new DataStoreException(msg, e);
            }
        }
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
        final int[] range = query.getProjection();
        if (range == null) {
            return dimensions;
        }
        final SampleDimension[] subset = new SampleDimension[range.length];
        for (int i=0; i<range.length; i++) {
            final int j = range[i];
            try {
                subset[i] = dimensions.get(j);
            } catch (IndexOutOfBoundsException e) {
                throw new DataStoreException(invalidRange(dimensions.size(), j), e);
            }
        }
        return UnmodifiableArrayList.wrap(subset);
    }

    /**
     * Returns an indication about when the "physical" loading of raster data will happen.
     * This information is fetched from the wrapped resource doing the actual real operations.
     */
    @Override
    public RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
        return source.getLoadingStrategy();
    }

    /**
     * Sets the preferred strategy about when to do the "physical" loading of raster data.
     * This information is forwarded to the wrapped resource doing the actual real operations.
     */
    @Override
    public boolean setLoadingStrategy(final RasterLoadingStrategy strategy) throws DataStoreException {
        return source.setLoadingStrategy(strategy);
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     * The domain to be read by the resource is computed as below:
     * <ul>
     *   <li>If the query specifies a {@link CoverageQuery#getSelection() domain},
     *       the given domain is intersected with the query domain.</li>
     *   <li>If the query specifies a {@linkplain CoverageQuery#getSourceDomainExpansion() domain expansion},
     *       the given domain is expanded by the amount specified in the query.</li>
     * </ul>
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {
        if (domain == null) {
            domain = source.getGridGeometry();
        }
        /*
         * Relax the clipping mode because we would need to clip not inside the
         * specified `domain` but inside the source domain, which may be larger.
         */
        domain = clip(domain, GridRoundingMode.ENCLOSING, GridClippingMode.BORDER_EXPANSION);
        final int[] qr = query.getProjection();
        if (range == null) {
            range = qr;
        } else if (qr != null) {
            final int[] sub = new int[range.length];
            for (int i=0; i<range.length; i++) {
                final int j = range[i];
                if (j >= 0 && j < qr.length) {
                    sub[i] = qr[j];
                } else {
                    throw new IllegalArgumentException(invalidRange(qr.length, j));
                }
            }
            range = sub;
        }
        return source.read(domain, range);
    }

    /**
     * Creates an exception message for an invalid range index.
     *
     * @param size   number of sample dimensions in source coverage.
     * @param index  the index which is out of bounds.
     */
    private String invalidRange(final int size, final int index) {
        return Resources.forLocale(listeners.getLocale()).getString(
                Resources.Keys.InvalidSampleDimensionIndex_2, size - 1, index);
    }
}
