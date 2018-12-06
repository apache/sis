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


/**
 * Access to data values in a <var>n</var>-dimensional grid.
 * A coverage resource may be a member of {@link Aggregate} if a single file can provide many images.
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
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    List<SampleDimension> getSampleDimensions() throws DataStoreException;
}
