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


/**
 * Information about the grid geometry and the conversion from grid coordinates to geodetic coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class GridGeometry {
    /**
     * Constructs a new grid geometry information.
     */
    protected GridGeometry() {
    }

    /**
     * Returns the number of dimensions in the grid.
     * This is the number of dimensions of source coordinates in the <cite>"grid to CRS"</cite> transform.
     *
     * @return Number of grid dimensions.
     */
    public abstract int getSourceDimensions();

    /**
     * Returns the number of dimensions in the coordinate reference system.
     * This is the number of dimensions of target coordinates in the <cite>"grid to CRS"</cite> transform.
     * It should also be equal to the size of the list returned by {@link #getAxes()}.
     *
     * @return Number of CRS dimensions.
     */
    public abstract int getTargetDimensions();

    /**
     * Returns the axes of the coordinate reference system. The size of this list is expected equals to the
     * value returned by {@link #getTargetDimensions()}, however the caller should be robust to inconsistencies.
     *
     * @return The CRS axes, in NetCDF order (reverse of "natural" order).
     */
    public abstract Axis[] getAxes();

    /**
     * Returns the coordinate for the given grid coordinate of an axis in the process of being constructed.
     * This is a callback method for {@link #getAxes()}. In the NetCDF UCAR API, this method maps directly
     * to {@link ucar.nc2.dataset.CoordinateAxis2D#getCoordValue(int, int)}.
     *
     * @param  j The fastest varying (right-most) index.
     * @param  i The slowest varying (left-most) index.
     * @return The coordinate at the given index, or {@link Double#NaN} if it can not be computed.
     */
    protected abstract double coordinateForCurrentAxis(final int j, final int i);
}
