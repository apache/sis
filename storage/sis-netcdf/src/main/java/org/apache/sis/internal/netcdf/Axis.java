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

import org.apache.sis.util.ArraysExt;
import org.apache.sis.storage.netcdf.AttributeNames;


/**
 * Information about a coordinate system axes. In NetCDF files, all axes can be related to 1 or more dimensions
 * of the grid domain. Those grid domain dimensions are specified by the {@link #sourceDimensions} array.
 * Whether the array length is 1 or 2 depends on whether the wrapped NetCDF axis is an instance of
 * {@link ucar.nc2.dataset.CoordinateAxis1D} or {@link ucar.nc2.dataset.CoordinateAxis2D} respectively.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see GridGeometry#getAxes()
 */
public final class Axis {
    /**
     * The attributes to use for fetching dimension (in ISO-19115 sense) information, or {@code null} if unknown.
     */
    public final AttributeNames.Dimension attributeNames;

    /**
     * The indices of the grid dimension associated to this axis. Values in this array are sorted with more
     * "significant" (defined below) dimensions first - this is not necessarily increasing order.
     *
     * <div class="section">Elements order</div>
     * The length of this array is often 1. But if more than one grid dimension is associated to this axis
     * (i.e. if the wrapped NetCDF axis is an instance of {@link ucar.nc2.dataset.CoordinateAxis2D}), then
     * the first index is what seems the most significant grid dimension (i.e. the dimension which seems
     * to be varying fastest along this axis).
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
     * In the particular case where the number of dimensions is equals to 2, this
     * constructor will detects by itself which grid dimension varies fastest.
     *
     * @param owner            Provides callback for the conversion from grid coordinates to geodetic coordinates.
     * @param attributeNames   The attributes to use for fetching dimension information, or {@code null} if unknown.
     * @param sourceDimensions The index of the grid dimension associated to this axis.
     * @param sourceSizes      The number of cell elements along that axis.
     */
    public Axis(final GridGeometry owner, final AttributeNames.Dimension attributeNames,
            final int[] sourceDimensions, final int[] sourceSizes)
    {
        this.attributeNames   = attributeNames;
        this.sourceDimensions = sourceDimensions;
        this.sourceSizes      = sourceSizes;
        if (sourceDimensions.length == 2) {
            final int up0  = sourceSizes[0];
            final int up1  = sourceSizes[1];
            final int mid0 = up0 / 2;
            final int mid1 = up1 / 2;
            final double d1 = (owner.coordinateForCurrentAxis(0, mid1) - owner.coordinateForCurrentAxis(up0-1, mid1)) / up0;
            final double d2 = (owner.coordinateForCurrentAxis(mid0, 0) - owner.coordinateForCurrentAxis(mid0, up1-1)) / up1;
            if (Math.abs(d2) > Math.abs(d1)) {
                sourceSizes[0] = up1;
                sourceSizes[1] = up0;
                ArraysExt.swap(sourceDimensions, 0, 1);
            }
        }
    }
}
