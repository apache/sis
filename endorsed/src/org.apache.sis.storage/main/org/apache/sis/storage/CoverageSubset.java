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

import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.GridClippingMode;
import org.apache.sis.coverage.grid.DimensionalityReduction;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.coverage.internal.shared.RangeArgument;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.pending.jdk.JDK16;


/**
 * The result of {@link CoverageQuery#execute(GridCoverageResource)}.
 * This implementation merges the domain and range specified by the query with
 * arguments of {@link GridCoverageResource#read(GridGeometry, int...)} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CoverageSubset extends AbstractGridCoverageResource {
    /**
     * Name for this coverage subset, or {@code null} if none.
     */
    private final GenericName name;

    /**
     * The coverage resource instance which provides the data.
     */
    private final GridCoverageResource source;

    /**
     * The domain of this resource, computed when first requested.
     * Sometime but not always the same as {@link #areaOfInterest}.
     * If {@link #reduction} is non-null, then this is the reduced grid geometry.
     *
     * @see #getGridGeometry()
     */
    private GridGeometry gridGeometry;

    /**
     * The domain specified in the query, potentially with dimensions added for a slice.
     * The number of dimensions should be the same as {@linkplain #source} grid geometry.
     * May be {@code null} if no area of interest has been specified.
     */
    private final GridGeometry areaOfInterest;

    /**
     * The dimensionality reduction to apply on the coverages, or {@code null} if none.
     */
    private final DimensionalityReduction reduction;

    /**
     * Number of additional cells to read on each border of the source grid coverage.
     */
    private final int sourceDomainExpansion;

    /**
     * 0-based indices of sample dimensions to read, or {@code null} for reading them all.
     */
    private final int[] selectedRange;

    /**
     * Creates a new coverage resource by filtering the given coverage using the given query.
     *
     * @param  name    name for this coverage subset, or {@code null} if none.
     * @param  source  the coverage resource instances which provides the data.
     * @param  query   the domain and range to read from the {@code source} coverage.
     * @throws DataStoreException if an error occurred while reading from the source resource.
     */
    CoverageSubset(final GenericName name, final GridCoverageResource source, final CoverageQuery query) throws DataStoreException {
        super(source);
        this.name   = name;
        this.source = source;
        reduction   = query.getAxisSelection(source);
        GridGeometry selection = query.getSelection();
        if (selection != null && reduction != null && reduction.isReduced(selection)) {
            selection = reduction.reverse(selection);
        }
        areaOfInterest        = selection;
        selectedRange         = query.getProjection(source);
        sourceDomainExpansion = query.getSourceDomainExpansion();
    }

    /**
     * Returns the name for this resource if specified.
     */
    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.ofNullable(name);
    }

    /**
     * Creates metadata about this subset.
     * It includes information about the complete feature set.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addDefaultMetadata(this, listeners);
        builder.addLineage(Resources.formatInternational(Resources.Keys.UnfilteredData));
        builder.addProcessDescription(Resources.formatInternational(Resources.Keys.SubsetQuery_1, StoreUtilities.getLabel(source)));
        builder.addSource(source.getMetadata());
        return builder.build();
    }

    /**
     * Returns the preferred resolutions (in units of CRS axes) for read operations in this data store.
     * In absence of dimensionality reduction, this is the same resolution as the source resource.
     *
     * @return preferred resolutions for read operations in this data store, or an empty array if none.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public List<double[]> getResolutions() throws DataStoreException {
        List<double[]> resolutions = source.getResolutions();
        if (reduction != null) {
            JDK16.toList(resolutions.stream()
                    .map((resolution) -> reduction.apply(new DirectPositionView.Double(resolution)).getCoordinates()));
        }
        return resolutions;
    }

    /**
     * Returns the valid extent of grid coordinates clipped to the area specified in the query.
     * It should be the geometry of the coverage that we get when invoking {@link #read read(…)}
     * with {@code null} arguments, but this is not guaranteed.
     * The returned grid geometry may be approximate.
     *
     * @return extent of grid coordinates clipped to the query.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store,
     *         or if the grid geometry cannot be clipped to the query area.
     */
    @Override
    public synchronized GridGeometry getGridGeometry() throws DataStoreException {
        GridGeometry domain = gridGeometry;
        if (domain == null) {
            /*
             * The grid should be fully contained in the resource specified at construction time,
             * including margin expansion (if any), because data may not exist outside that area.
             * This requirement is specified by `GridClippingMode.STRICT`.
             */
            domain = clip(source.getGridGeometry(), GridRoundingMode.NEAREST, GridClippingMode.STRICT);
            if (reduction != null) {
                domain = reduction.apply(domain);
            }
            gridGeometry = domain;      // Set only on success.
        }
        return domain;
    }

    /**
     * Clips the given domain to the area of interest specified by the query. If any grid geometry is null,
     * the other one is returned. The {@code domain} argument should have the number of dimensions expected
     * by the {@linkplain #source}, using {@link #reduction} if necessary for changing the dimensionality.
     *
     * @param  domain    the domain requested in a read operation, or {@code null}.
     * @param  rounding  whether to clip to nearest box or an enclosing box.
     * @param  clipping  whether to clip the resulting extent to the specified {@code domain} extent.
     * @return intersection of the given grid geometry with the query domain.
     * @throws DataStoreException if the intersection cannot be computed.
     */
    private GridGeometry clip(final GridGeometry domain, final GridRoundingMode rounding, final GridClippingMode clipping)
            throws DataStoreException
    {
        if (domain == null) return areaOfInterest;
        if (areaOfInterest == null) return domain;
        try {
            final GridDerivation derivation = domain.derive().rounding(rounding).clipping(clipping);
            if (sourceDomainExpansion != 0) {
                final int[] margins = new int[domain.getDimension()];
                Arrays.fill(margins, sourceDomainExpansion);
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
        if (selectedRange == null) {
            return dimensions;
        }
        final var subset = new SampleDimension[selectedRange.length];
        for (int i=0; i < selectedRange.length; i++) {
            final int j = selectedRange[i];
            try {
                subset[i] = dimensions.get(j);
            } catch (IndexOutOfBoundsException e) {
                throw new DataStoreException(e);
            }
        }
        return List.of(subset);
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
     *   <li>If the query specifies a {@linkplain CoverageQuery#getAxisSelection() dimensionality reduction},
     *       the given domain is inflated to the number of dimensions of the source.</li>
     *   <li>If the query specifies an {@linkplain CoverageQuery#getSelection() area of interest},
     *       the given domain is intersected with the query selection.</li>
     *   <li>If the query specifies a {@linkplain CoverageQuery#getSourceDomainExpansion() domain expansion},
     *       the given domain is expanded by the amount specified in the query.</li>
     * </ul>
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
        /*
         * Need to convert to the full number of dimensions first, even if the domain is null,
         * because this operation may return a domain with slice coordinates.
         */
        if (reduction != null) {
            domain = reduction.reverse(domain);
        }
        if (domain == null) {
            domain = source.getGridGeometry();
        }
        /*
         * Relax the clipping mode because we would need to clip not inside the
         * specified `domain` but inside the source domain, which may be larger.
         */
        domain = clip(domain, GridRoundingMode.ENCLOSING, GridClippingMode.BORDER_EXPANSION);
        if (selectedRange != null) {
            final var validation = RangeArgument.validate(selectedRange.length, ranges, listeners);
            ranges = new int[validation.getNumBands()];
            for (int i=0; i<ranges.length; i++) {
                ranges[validation.getTargetIndex(i)] = selectedRange[validation.getSourceIndex(i)];
            }
        }
        /*
         * Read the coverage will all dimensions, then provide a view
         * with a reduced number of dimensions if that was requested.
         */
        GridCoverage coverage = source.read(domain, ranges);
        if (reduction != null) {
            coverage = reduction.apply(coverage);
        }
        return coverage;
    }
}
