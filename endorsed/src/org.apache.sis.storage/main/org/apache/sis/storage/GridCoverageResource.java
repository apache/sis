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
import java.util.Objects;
import java.util.Optional;
import org.opengis.geometry.Envelope;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.util.ArraysExt;


/**
 * Access to data values in a <var>n</var>-dimensional grid.
 * A coverage is a kind of function with the following properties:
 *
 * <ul class="verbose">
 *   <li>The function input is a position valid in the coverage <i>domain</i>. In the particular case of
 *       {@link GridCoverage}, the domain is described by a {@linkplain #getGridGeometry() grid geometry}.</li>
 *   <li>The function output is a record of values in the coverage <i>range</i>. In the particular case of
 *       {@link GridCoverage}, the range is described by a list of {@linkplain #getSampleDimensions() sample dimensions}.</li>
 * </ul>
 *
 * A coverage resource may be a member of {@link Aggregate} if a single file can provide many rasters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.0
 */
public interface GridCoverageResource extends DataSet {
    /**
     * Returns the spatiotemporal extent of this resource in its most natural coordinate reference system.
     * The default implementation fetches this information from the {@linkplain #getGridGeometry() grid geometry},
     * if presents.
     *
     * @return the spatiotemporal resource extent. May be absent if none or too costly to compute.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     *
     * @see GridGeometry#getEnvelope()
     */
    @Override
    default Optional<Envelope> getEnvelope() throws DataStoreException {
        final GridGeometry gg = getGridGeometry();
        if (gg != null && gg.isDefined(GridGeometry.ENVELOPE)) {
            return Optional.of(gg.getEnvelope());
        }
        return Optional.empty();
    }

    /**
     * Returns the valid extent of grid coordinates together with the conversion from those grid
     * coordinates to real world coordinates. A grid geometry contains the following information:
     *
     * <ul class="verbose">
     *   <li>The minimum and maximum grid coordinates as integers (the <i>Grid Extent</i>).
     *       The minimum coordinates are typically (0,0, …, 0) but not necessarily.</li>
     *   <li>The minimum and maximum "real world" coordinates (the <i>Envelope</i>).
     *       Those coordinates are typically, but not necessarily, latitudes and longitudes
     *       or projected coordinates, together with altitudes and dates.</li>
     *   <li>A description of the datum and axes of above "real world" coordinates
     *       (the <i>Coordinate Reference System</i>).</li>
     *   <li>The conversion from grid coordinates to "real world" coordinates. This conversion is often,
     *       but not necessarily, a linear relationship. Axis order or direction may be changed by the conversion.
     *       For example, row indices may be increasing toward down while latitude coordinates are increasing toward up.</li>
     *   <li>An <em>estimation</em> of grid resolution for each "real world" axis.</li>
     * </ul>
     *
     * The grid returned by this method <em>should</em> be equal to the grid returned by
     * <code>{@linkplain #read(GridGeometry, int...) read}(null).{@linkplain GridCoverage#getGridGeometry() getGridGeometry()}</code>.
     * However, the grid geometry returned by this method is allowed to be only approximate if computing accurate information would be
     * prohibitively expensive, or if the grid geometry depends on the exact argument value given to the {@code read(…)} method.
     * At least, the {@linkplain GridGeometry#getDimension() number of dimensions} should match.
     *
     * @return extent of grid coordinates together with their mapping to "real world" coordinates.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     *
     * @see GridCoverage#getGridGeometry()
     */
    GridGeometry getGridGeometry() throws DataStoreException;

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     * Sample dimensions contain the following information:
     *
     * <ul class="verbose">
     *   <li>The range of valid <i>sample values</i>, typically but not necessarily as positive integers.</li>
     *   <li>A <i>transfer function</i> for converting sample values to real values, for example measurements
     *       of a geophysics phenomenon. The transfer function is typically defined by a scale factor and an offset,
     *       but is not restricted to such linear equations.</li>
     *   <li>The units of measurement of "real world" values after their conversions from sample values.</li>
     *   <li>The sample values reserved for missing values.</li>
     * </ul>
     *
     * The returned list should never be empty. If the coverage is an image to be used only for visualization purposes
     * (i.e. the image does not contain any classification data or any measurement of physical phenomenon), then list
     * size should be equal to the {@linkplain java.awt.image.SampleModel#getNumBands() number of bands} in the image
     * and sample dimension names may be "Red", "Green" and "Blue" for instance. Those sample dimensions do not need
     * to contain any {@linkplain SampleDimension#getCategories() category}.
     *
     * <p>The list returned by this method <em>should</em> be equal to the list returned by
     * <code>{@linkplain #read(GridGeometry, int...) read}(null).{@linkplain GridCoverage#getSampleDimensions() getSampleDimensions()}</code>.
     * However, the sample dimensions returned by this method is allowed to be only approximate if computing accurate information
     * would be prohibitively expensive, or if the sample dimensions depend on the {@code domain} argument (area of interest)
     * given to the {@code read(…)} method. At least, the number of sample dimensions should match.</p>
     *
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     *
     * @see GridCoverage#getSampleDimensions()
     */
    List<SampleDimension> getSampleDimensions() throws DataStoreException;

    /**
     * Returns the preferred resolutions (in units of CRS axes) for read operations in this data store.
     * If the storage supports pyramid, then the list should contain the resolution at each pyramid level
     * ordered from finest (smallest numbers) to coarsest (largest numbers) resolution.
     * Otherwise the list contains a single element which is the {@linkplain #getGridGeometry() grid geometry}
     * resolution, or an empty list if no resolution is applicable to the coverage (e.g. because non-constant).
     *
     * <p>Each element shall be an array with a length equals to the number of CRS dimensions.
     * In each array, value at index <var>i</var> is the cell size along CRS dimension <var>i</var>
     * in units of the CRS axis <var>i</var>.</p>
     *
     * <p>Note that arguments given to {@link #subset(CoverageQuery) subset(…)} or {@link #read read(…)} methods
     * are <em>not</em> constrained to the resolutions returned by this method. Those resolutions are only hints
     * about resolution values where read operations are likely to be more efficient.</p>
     *
     * @return preferred resolutions for read operations in this data store, or an empty array if none.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     *
     * @see GridGeometry#getResolution(boolean)
     *
     * @since 1.2
     */
    default List<double[]> getResolutions() throws DataStoreException {
        final GridGeometry gg = getGridGeometry();
        if (gg != null && gg.isDefined(GridGeometry.RESOLUTION)) {      // Should never be null but we are paranoiac.
            final double[] resolution = gg.getResolution(false);
            if (!ArraysExt.allEquals(resolution, Double.NaN)) {
                return List.of(resolution);
            }
        }
        return List.of();
    }

    /**
     * Requests a subset of the coverage.
     * The filtering can be applied in two aspects:
     *
     * <ul>
     *   <li>The returned {@code GridCoverageResource} may contain a smaller domain (grid extent).</li>
     *   <li>The returned {@code GridCoverageResource} may contain a smaller range (less sample dimensions).</li>
     * </ul>
     *
     * <p>The returned subset may be a <em>view</em> of this set, i.e. changes in this {@code GridCoverageResource}
     * may be reflected immediately on the returned subset (and conversely), but not necessarily.
     * However, the returned subset may not have the same capabilities as this {@link GridCoverageResource}.
     * In particular, write operations may become unsupported after complex queries.</p>
     *
     * <h4>Default implementation</h4>
     * The default implementation delegates to {@link CoverageQuery#execute(GridCoverageResource)} if the given
     * query is an instance of {@code CoverageQuery}, or throws {@link UnsupportedQueryException} otherwise.
     *
     * @param  query  definition of domain (grid extent) and range (sample dimensions) filtering applied at reading time.
     * @return resulting coverage resource (never {@code null}).
     * @throws UnsupportedQueryException if the given query is not valid for this {@code GridCoverageResource}.
     *         This includes query validation errors.
     * @throws DataStoreException if another error occurred while processing the query.
     *
     * @see FeatureSet#subset(Query)
     * @see CoverageQuery#execute(GridCoverageResource)
     *
     * @since 1.1
     */
    default GridCoverageResource subset(final Query query) throws UnsupportedQueryException, DataStoreException {
        if (Objects.requireNonNull(query) instanceof CoverageQuery) try {
            return ((CoverageQuery) query).execute(this);
        } catch (RuntimeException e) {
            throw new UnsupportedQueryException(e);
        } else {
            throw new UnsupportedQueryException();
        }
    }

    /**
     * Loads a subset of the grid coverage represented by this resource. If a non-null grid geometry is specified,
     * then this method will try to return a grid coverage matching the given grid geometry on a best-effort basis;
     * the coverage actually returned may have a different resolution, cover a different area in a different CRS,
     * <i>etc</i>. The general contract is that the returned coverage should not contain less data than a coverage
     * matching exactly the given geometry, ignoring the areas outside the resource domain.
     *
     * <p>The returned coverage shall contain the exact set of sample dimensions specified by the {@code range} argument,
     * in the specified order (the "best-effort basis" flexibility applies only to the grid geometry, not to the ranges).
     * All {@code ranges} values shall be between 0 inclusive and <code>{@linkplain #getSampleDimensions()}.size()</code>
     * exclusive, without duplicated values.</p>
     *
     * <p>While this method name suggests an immediate reading, some implementations may defer the actual reading
     * at a later stage.</p>
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DisjointExtentException if the given domain does not intersect the resource extent.
     * @throws IllegalArgumentException if the given domain or ranges are invalid for another reason.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException;

    /**
     * Returns an indication about when the "physical" loading of raster data will happen.
     * This is the strategy actually applied by this resource implementation, not necessarily
     * the strategy given in the last call to {@link #setLoadingStrategy(RasterLoadingStrategy)
     * setLoadingStrategy(…)}.
     *
     * <p>The default strategy is to load raster data at {@link #read read(…)} method invocation time.</p>
     *
     * @return current raster data loading strategy for this resource.
     * @throws DataStoreException if an error occurred while fetching data store configuration.
     *
     * @since 1.1
     */
    default RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
        return RasterLoadingStrategy.AT_READ_TIME;
    }

    /**
     * Sets the preferred strategy about when to do the "physical" loading of raster data.
     * Implementations are free to ignore this parameter or to replace the given strategy
     * by the closest alternative that this resource can support.
     *
     * @param  strategy  the desired strategy for loading raster data.
     * @return {@code true} if the given strategy has been accepted, or {@code false}
     *         if this implementation replaced the given strategy by an alternative.
     * @throws DataStoreException if an error occurred while setting data store configuration.
     *
     * @since 1.1
     */
    default boolean setLoadingStrategy(final RasterLoadingStrategy strategy) throws DataStoreException {
        return Objects.requireNonNull(strategy) == getLoadingStrategy();
    }
}
