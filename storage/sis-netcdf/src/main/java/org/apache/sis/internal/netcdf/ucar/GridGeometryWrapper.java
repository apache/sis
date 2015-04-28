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
package org.apache.sis.internal.netcdf.ucar;

import java.util.List;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;
import org.apache.sis.internal.netcdf.Axis;
import org.apache.sis.internal.netcdf.GridGeometry;
import org.apache.sis.storage.netcdf.AttributeNames;
import org.apache.sis.util.ArraysExt;


/**
 * Information about NetCDF coordinate system, which include information about grid geometries.
 * In OGC/ISO specifications, the coordinate system and the grid geometries are distinct entities.
 * However the UCAR model takes a different point of view where the coordinate system holds some
 * of the grid geometry information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class GridGeometryWrapper extends GridGeometry {
    /**
     * The NetCDF coordinate system to wrap.
     */
    private final CoordinateSystem netcdfCS;

    /**
     * A temporary variable for {@link #coordinateForCurrentAxis(int, int)}.
     */
    private transient CoordinateAxis2D axis2D;

    /**
     * Creates a new grid geometry for the given NetCDF coordinate system.
     *
     * @param cs The NetCDF coordinate system, or {@code null} if none.
     */
    GridGeometryWrapper(final CoordinateSystem cs) {
        netcdfCS = cs;
    }

    /**
     * Returns the number of dimensions in the grid.
     */
    @Override
    public int getSourceDimensions() {
        return netcdfCS.getRankDomain();
    }

    /**
     * Returns the number of dimensions in the coordinate reference system.
     */
    @Override
    public int getTargetDimensions() {
        return netcdfCS.getRankRange();
    }

    /**
     * Returns all axes of the NetCDF coordinate system, together with the grid dimension to which the axis
     * is associated.
     *
     * <p>In this method, the words "domain" and "range" are used in the NetCDF sense: they are the input
     * (domain) and output (range) of the function that convert grid indices to geodetic coordinates.</p>
     *
     * <p>The domain of all axes (or the {@linkplain CoordinateSystem#getDomain() coordinate system domain})
     * is often the same than the {@linkplain #getDomain() domain of the variable}, but not necessarily.
     * In particular, the relationship is not straightforward when the coordinate system contains instances
     * of {@link CoordinateAxis2D}.</p>
     */
    @Override
    public Axis[] getAxes() {
        final List<Dimension> domain = netcdfCS.getDomain();
        final List<CoordinateAxis> range = netcdfCS.getCoordinateAxes();
        int targetDim = range.size();
        final Axis[] axes = new Axis[targetDim];
        /*
         * NetCDF files declare axes in reverse order, so we iterate in the 'netcdfAxes'
         * list in reverse order for adding to the 'axes' list in "natural" order.
         */
        while (--targetDim >= 0) {
            final CoordinateAxis  axis = range.get(targetDim);
            final List<Dimension> axisDomain = axis.getDimensions();
            AttributeNames.Dimension attributeNames = null;
            final AxisType type = axis.getAxisType();
            if (type != null) switch (type) {
                case Lon:      attributeNames = AttributeNames.LONGITUDE; break;
                case Lat:      attributeNames = AttributeNames.LATITUDE; break;
                case Pressure: // Fallthrough: consider as Height
                case Height:   attributeNames = AttributeNames.VERTICAL; break;
                case RunTime:  // Fallthrough: consider as Time
                case Time:     attributeNames = AttributeNames.TIME; break;
            }
            /*
             * Get the grid dimensions (part of the "domain" in UCAR terminology) used for computing
             * the ordinate values along the current axis. There is exactly 1 such grid dimension in
             * straightforward NetCDF files. However some more complex files may have 2 dimensions.
             */
            int i = 0;
            final int[] indices = new int[axisDomain.size()];
            final int[] sizes   = new int[indices.length];
            for (final Dimension dimension : axisDomain) {
                final int sourceDim = domain.lastIndexOf(dimension);
                if (sourceDim >= 0) {
                    indices[i] = sourceDim;
                    sizes[i++] = dimension.getLength();
                }
                /*
                 * If the axis dimension has not been found in the coordinate system (sourceDim < 0),
                 * then there is maybe a problem with the NetCDF file. However for the purpose of this
                 * package, we can proceed as if the dimension does not exist.
                 */
            }
            axis2D = (axis instanceof CoordinateAxis2D) ? (CoordinateAxis2D) axis : null;
            axes[targetDim] = new Axis(this, attributeNames,
                    ArraysExt.resize(indices, i),
                    ArraysExt.resize(sizes, i));
        }
        axis2D = null;
        return axes;
    }

    /**
     * Returns the coordinate for the given grid coordinate of an axis in the process of being constructed.
     * This is a callback method for {@link #getAxes()}.
     */
    @Override
    protected double coordinateForCurrentAxis(final int j, final int i) {
        return (axis2D != null) ? axis2D.getCoordValue(j, i) : Double.NaN;
    }
}
