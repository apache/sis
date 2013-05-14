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
     * {@section Elements order}
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
     * Constructs a new axis associated to no grid dimension.
     * Actually this should never happen, but we try to be safe.
     *
     * @param attributeNames The attributes to use for fetching dimension information, or {@code null} if unknown.
     */
    public Axis(final AttributeNames.Dimension attributeNames) {
        this.attributeNames   = attributeNames;
        this.sourceDimensions = ArraysExt.EMPTY_INT;
        this.sourceSizes      = ArraysExt.EMPTY_INT;
    }

    /**
     * Constructs a new axis associated to a single grid dimension.
     *
     * @param attributeNames The attributes to use for fetching dimension information, or {@code null} if unknown.
     * @param sourceDim      The index of the grid dimension associated to this axis.
     * @param sourceSize     The number of cell elements along that axis.
     */
    public Axis(final AttributeNames.Dimension attributeNames, final int sourceDim, final int sourceSize) {
        this.attributeNames   = attributeNames;
        this.sourceDimensions = new int[] {sourceDim};
        this.sourceSizes      = new int[] {sourceSize};
    }

    /**
     * Constructs a new axis associated to exactly two grid dimensions.
     * This constructor will detects by itself which grid dimension varies fastest.
     *
     * @param attributeNames The attributes to use for fetching dimension information, or {@code null} if unknown.
     * @param sourceDim1     The index of a first grid dimension associated to this axis.
     * @param sourceSize1    The number of cell elements along the first grid dimension.
     * @param sourceDim2     The index of a second grid dimension associated to this axis.
     * @param sourceSize2    The number of cell elements along the second grid dimension.
     * @param toTarget       The conversion from grid coordinates to geodetic coordinates.
     */
    public Axis(final AttributeNames.Dimension attributeNames,
                final int sourceDim1, final int sourceSize1,
                final int sourceDim2, final int sourceSize2,
                final GridGeometry toTarget)
    {
        this.attributeNames = attributeNames;
        final int mid1 = sourceSize1 / 2;
        final int mid2 = sourceSize2 / 2;
        final double d1 = (toTarget.coordinateForCurrentAxis(0, mid2) - toTarget.coordinateForCurrentAxis(sourceSize1-1, mid2)) / sourceSize1;
        final double d2 = (toTarget.coordinateForCurrentAxis(mid1, 0) - toTarget.coordinateForCurrentAxis(mid1, sourceSize2-1)) / sourceSize2;
        if (Math.abs(d2) > Math.abs(d1)) {
            sourceDimensions  = new int[] {sourceDim2,  sourceDim1};
            sourceSizes       = new int[] {sourceSize2, sourceSize1};
        } else {
            sourceDimensions = new int[] {sourceDim1,  sourceDim2};
            sourceSizes      = new int[] {sourceSize1, sourceSize2};
        }
    }

    /**
     * Constructs a new axis associated to an arbitrary number of grid dimension.
     * Current implementation does not try to identify the "main" dimension
     * (we may try to improve that in a future SIS version).
     *
     * @param attributeNames   The attributes to use for fetching dimension information, or {@code null} if unknown.
     * @param sourceDimensions The index of the grid dimension associated to this axis.
     * @param sourceSizes      The number of cell elements along that axis.
     */
    public Axis(final AttributeNames.Dimension attributeNames, final int[] sourceDimensions, final int[] sourceSizes) {
        this.attributeNames   = attributeNames;
        this.sourceDimensions = sourceDimensions;
        this.sourceSizes      = sourceSizes;
    }
}
