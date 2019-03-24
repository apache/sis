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
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;


/**
 * Access to data values in a <var>n</var>-dimensional grid.
 * A coverage is a kind of function with the following properties:
 *
 * <ul class="verbose">
 *   <li>The function input is a position valid in the coverage <cite>domain</cite>. In the particular case of
 *       {@link GridCoverage}, the domain is described by a {@linkplain #getGridGeometry() grid geometry}.</li>
 *   <li>The function output is a record of values in the coverage <cite>range</cite>. In the particular case of
 *       {@link GridCoverage}, the range is described by a list of {@linkplain #getSampleDimensions() sample dimensions}.</li>
 * </ul>
 *
 * A coverage resource may be a member of {@link Aggregate} if a single file can provide many rasters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface GridCoverageResource extends DataSet {
    /**
     * Returns the valid extent of grid coordinates together with the conversion from those grid
     * coordinates to real world coordinates. A grid geometry contains the following information:
     *
     * <ul class="verbose">
     *   <li>The minimum and maximum grid coordinates as integers (the <cite>Grid Extent</cite>).
     *       The minimum coordinates are typically (0,0, …, 0) but not necessarily.</li>
     *   <li>The minimum and maximum "real world" coordinates (the <cite>Envelope</cite>).
     *       Those coordinates are typically, but not necessarily, latitudes and longitudes
     *       or projected coordinates, together with altitudes and dates.</li>
     *   <li>A description of the datum and axes of above "real world" coordinates
     *       (the <cite>Coordinate Reference System</cite>).</li>
     *   <li>The conversion from grid coordinates to "real world" coordinates. This conversion is often,
     *       but not necessarily, a linear relationship. Axis order or direction may be changed by the conversion.
     *       For example row indices may be increasing toward down while latitude coordinates are increasing toward up.</li>
     *   <li>An <em>estimation</em> of grid resolution for each "real world" axis.</li>
     * </ul>
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
     *   <li>The range of valid <cite>sample values</cite>, typically but not necessarily as positive integers.</li>
     *   <li>A <cite>transfer function</cite> for converting sample values to real values, for example measurements
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
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     *
     * @see GridCoverage#getSampleDimensions()
     */
    List<SampleDimension> getSampleDimensions() throws DataStoreException;

    /**
     * Loads a subset of the grid coverage represented by this resource. If a non-null grid geometry is specified,
     * then this method will try to return a grid coverage matching the given grid geometry on a best-effort basis;
     * the coverage actually returned may have a different resolution, cover a different area in a different CRS,
     * <i>etc</i>. The general contract is that the returned coverage should not contain less data than a coverage
     * matching exactly the given geometry.
     *
     * <p>The returned coverage shall contain the exact set of sample dimensions specified by the {@code range} argument,
     * in the specified order (the "best-effort basis" flexibility applies only to the grid geometry, not to the range).
     * All {@code range} values shall be between 0 inclusive and <code>{@linkplain #getSampleDimensions()}.size()</code>
     * exclusive, without duplicated values.</p>
     *
     * <p>While this method name suggests an immediate reading, some implementations may defer the actual reading
     * at a later stage.</p>
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    GridCoverage read(GridGeometry domain, int... range) throws DataStoreException;
}
