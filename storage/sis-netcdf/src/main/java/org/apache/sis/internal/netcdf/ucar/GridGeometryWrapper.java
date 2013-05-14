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
import java.util.ArrayList;
import java.util.Collections;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;
import org.apache.sis.internal.netcdf.Axis;
import org.apache.sis.internal.netcdf.GridGeometry;
import org.apache.sis.internal.netcdf.WarningProducer;
import org.apache.sis.storage.netcdf.AttributeNames;


/**
 * Information about NetCDF coordinate system, which include information about grid geometries.
 * In OGC/ISO specifications, the coordinate system and the grid geometries are distinct entities.
 * However the UCAR model takes a different point of view where the coordinate system holds some
 * of the grid geometry information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.14)
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
     * Creates a new CRS builder.
     *
     * @param parent Where to send the warnings, or {@code null} if none.
     * @param cs The NetCDF coordinate system, or {@code null} if none.
     */
    GridGeometryWrapper(final WarningProducer parent, final CoordinateSystem cs) {
        super(parent);
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
     * <p>The domain of all axes (or the {@linkplain CoordinateSystem#getDomain() coordinate system domain})
     * is often the same than the {@linkplain #getDomain() domain of the variable}, but not necessarily.
     * In particular, the relationship is not straightforward when the coordinate system contains instances
     * of {@link CoordinateAxis2D}.</p>
     */
    @Override
    @SuppressWarnings("fallthrough")
    public List<Axis> getAxes() {
        final List<Dimension> sourceDimensions = netcdfCS.getDomain();
        final List<CoordinateAxis> netcdfAxes = netcdfCS.getCoordinateAxes();
        final List<Axis> axes = new ArrayList<>(netcdfAxes.size());
        for (final CoordinateAxis netcdfAxis : netcdfAxes) {
            final List<Dimension> dimensions = netcdfAxis.getDimensions();
            AttributeNames.Dimension attributeNames = null;
            final AxisType type = netcdfAxis.getAxisType();
            if (type != null) switch (type) {
                case Lon:      attributeNames = AttributeNames.LONGITUDE; break;
                case Lat:      attributeNames = AttributeNames.LATITUDE; break;
                case Pressure: // Fallthrough: consider as Height
                case Height:   attributeNames = AttributeNames.VERTICAL; break;
                case RunTime:  // Fallthrough: consider as Time
                case Time:     attributeNames = AttributeNames.TIME; break;
            }
            final Axis axis;
            switch (dimensions.size()) {
                case 0: {
                    // Should never happen, but defined by paranoia.
                    axis = new Axis(attributeNames);
                    break;
                }
                case 1: {
                    // The most common case where one source axis == one target axis.
                    final Dimension dim = dimensions.get(0);
                    axis = new Axis(attributeNames,
                            sourceDimensions.indexOf(dim), dim.getLength());
                    break;
                }
                case 2: {
                    // An other case managed by the UCAR API.
                    if (netcdfAxis instanceof CoordinateAxis2D) {
                        axis2D = (CoordinateAxis2D) netcdfAxis;
                        final CoordinateAxis2D a2 = (CoordinateAxis2D) netcdfAxis;
                        final Dimension dim0 = dimensions.get(0);
                        final Dimension dim1 = dimensions.get(1);
                        axis = new Axis(attributeNames,
                                sourceDimensions.indexOf(dim0), dim0.getLength(),
                                sourceDimensions.indexOf(dim1), dim1.getLength(),
                                this);
                        break;
                    }
                    // Fallthrough: use the generic case as a fallback.
                }
                default: {
                    // Uncommon case.
                    final int[] indices = new int[dimensions.size()];
                    final int[] sizes   = new int[indices.length];
                    for (int i=indices.length; --i>=0;) {
                        final Dimension dimension = dimensions.get(i);
                        indices[i] = sourceDimensions.indexOf(dimension);
                        sizes[i] = dimension.getLength();
                    }
                    axis = new Axis(attributeNames, indices, sizes);
                    break;
                }
            }
        }
        /*
         * NetCDF files define the axes in reverse order.
         * Retores them in "natural" order.
         */
        Collections.reverse(axes);
        return axes;
    }

    /**
     * Returns the coordinate for the given grid coordinate of an axis in the process of being constructed.
     * This is a callback method for {@link #getAxes()}.
     */
    @Override
    protected double coordinateForCurrentAxis(final int j, final int i) {
        return axis2D.getCoordValue(j, i);
    }
}
