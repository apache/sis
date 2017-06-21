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
package org.apache.sis.internal.netcdf;

import java.io.IOException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.storage.netcdf.AttributeNames;
import org.apache.sis.storage.DataStoreException;


/**
 * Information about a coordinate system axes. In NetCDF files, all axes can be related to 1 or more dimensions
 * of the grid domain. Those grid domain dimensions are specified by the {@link #sourceDimensions} array.
 * Whether the array length is 1 or 2 depends on whether the wrapped NetCDF axis is an instance of
 * {@link ucar.nc2.dataset.CoordinateAxis1D} or {@link ucar.nc2.dataset.CoordinateAxis2D} respectively.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see GridGeometry#getAxes()
 *
 * @since 0.3
 * @module
 */
public final class Axis {
    /**
     * The attributes to use for fetching dimension (in ISO-19115 sense) information, or {@code null} if unknown.
     */
    public final AttributeNames.Dimension attributeNames;

    /**
     * The indices of the grid dimension associated to this axis. The length of this array is often 1.
     * But if more than one grid dimension is associated to this axis (i.e. if the wrapped NetCDF axis
     * is an instance of {@link ucar.nc2.dataset.CoordinateAxis2D}),  then the first value is the grid
     * dimension which seems most closely oriented toward this axis direction. We do that for allowing
     * {@code MetadataReader.addSpatialRepresentationInfo(…)} method to get the most appropriate value
     * for ISO 19115 {@code metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionSize}
     * metadata property.
     */
    public final int[] sourceDimensions;

    /**
     * The number of cell elements along the source grid dimensions. The length of this array shall be
     * equals to the {@link #sourceDimensions} length. For each element, {@code sourceSizes[i]} shall
     * be equals to the number of grid cells in the grid dimension at index {@code sourceDimensions[i]}.
     */
    public final int[] sourceSizes;

    /**
     * Constructs a new axis associated to an arbitrary number of grid dimension.
     * In the particular case where the number of dimensions is equals to 2, this constructor will detect
     * by itself which grid dimension varies fastest and reorder in-place the elements in the given arrays
     * (those array are modified, not cloned).
     *
     * @param  owner             provides callback for the conversion from grid coordinates to geodetic coordinates.
     * @param  axis              an implementation-dependent object representing the two-dimensional axis, or {@code null} if none.
     * @param  attributeNames    the attributes to use for fetching dimension information, or {@code null} if unknown.
     * @param  sourceDimensions  the index of the grid dimension associated to this axis.
     * @param  sourceSizes       the number of cell elements along that axis.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     */
    public Axis(final GridGeometry owner, final Object axis, final AttributeNames.Dimension attributeNames,
            final int[] sourceDimensions, final int[] sourceSizes) throws IOException, DataStoreException
    {
        this.attributeNames   = attributeNames;
        this.sourceDimensions = sourceDimensions;
        this.sourceSizes      = sourceSizes;
        if (sourceDimensions.length == 2) {
            final int up0  = sourceSizes[0];
            final int up1  = sourceSizes[1];
            final int mid0 = up0 / 2;
            final int mid1 = up1 / 2;
            final double inc0 = (owner.coordinateForAxis(axis,     0, mid1) -
                                 owner.coordinateForAxis(axis, up0-1, mid1)) / up0;
            final double inc1 = (owner.coordinateForAxis(axis, mid0,     0) -
                                 owner.coordinateForAxis(axis, mid0, up1-1)) / up1;
            if (Math.abs(inc1) > Math.abs(inc0)) {
                sourceSizes[0] = up1;
                sourceSizes[1] = up0;
                ArraysExt.swap(sourceDimensions, 0, 1);
            }
        }
    }
}
