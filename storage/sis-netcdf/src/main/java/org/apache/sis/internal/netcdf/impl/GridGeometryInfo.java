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
package org.apache.sis.internal.netcdf.impl;

import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.netcdf.Axis;
import org.apache.sis.internal.netcdf.GridGeometry;
import org.apache.sis.storage.netcdf.AttributeNames;


/**
 * Description of a grid geometry found in a NetCDF file.
 *
 * <p>In this class, the words "domain" and "range" are used in the NetCDF sense: they are the input
 * (domain) and output (range) of the function that convert grid indices to geodetic coordinates.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class GridGeometryInfo extends GridGeometry {
    /**
     * Mapping from values of the {@code "_CoordinateAxisType"} attribute to the
     * {@code AttributeNames.Dimension} constant.
     */
    private static final Object[] AXIS_TYPES = {
        "Lon",      AttributeNames.LONGITUDE,
        "Lat",      AttributeNames.LATITUDE,
        "Pressure", AttributeNames.VERTICAL,
        "Height",   AttributeNames.VERTICAL,
        "RunTime",  AttributeNames.TIME,
        "Time",     AttributeNames.TIME
    };

    /**
     * Describes the input values expected by the function converting grid indices to geodetic coordinates.
     * They are the dimensions of the grid (<strong>not</strong> the dimensions of the CRS).
     */
    private final Dimension[] domain;

    /**
     * Describes the output values calculated by the function converting grid indices to geodetic coordinates.
     * They are the coordinate values expressed in the CRS.
     */
    private final VariableInfo[] range;

    /**
     * Constructs a new grid geometry information.
     *
     * @param domain Describes the input values of the "grid to CRS" conversion.
     * @param range  The output values of the "grid to CRS" conversion.
     */
    GridGeometryInfo(final Dimension[] domain, final VariableInfo[] range) {
        this.domain = domain;
        this.range  = range;
    }

    /**
     * Returns the number of dimensions of source coordinates in the <cite>"grid to CRS"</cite> conversion.
     * This is the number of dimensions of the <em>grid</em>.
     */
    @Override
    public int getSourceDimensions() {
        return domain.length;
    }

    /**
     * Returns the number of dimensions of target coordinates in the <cite>"grid to CRS"</cite> conversion.
     * This is the number of dimensions of the <em>coordinate reference system</em>.
     */
    @Override
    public int getTargetDimensions() {
        return range.length;
    }

    /**
     * Returns all axes of the NetCDF coordinate system, together with the grid dimension to which the axis
     * is associated. See {@link org.apache.sis.internal.netcdf.ucar.GridGeometryWrapper#getAxes()} for more
     * information on the algorithm applied here, and relationship with the UCAR library.
     */
    @Override
    public Axis[] getAxes() {
        int targetDim = range.length;
        final Axis[] axes = new Axis[targetDim];
        while (--targetDim >= 0) {
            final VariableInfo axis = range[targetDim];
            final Dimension[] axisDomain = axis.dimensions;
            AttributeNames.Dimension attributeNames = null;
            final String type = axis.getAxisType();
            if (type != null) {
                for (int i=0; i<AXIS_TYPES.length; i+=2) {
                    if (type.equalsIgnoreCase((String) AXIS_TYPES[i])) {
                        attributeNames = (AttributeNames.Dimension) AXIS_TYPES[i+1];
                        break;
                    }
                }
            }
            int i = 0;
            final int[] indices = new int[axisDomain.length];
            final int[] sizes   = new int[axisDomain.length];
            for (final Dimension dimension : axisDomain) {
                for (int sourceDim=domain.length; --sourceDim>=0;) {
                    if (domain[sourceDim] == dimension) {
                        indices[i] = sourceDim;
                        sizes[i++] = dimension.length;
                        break;
                    }
                }
            }
            axes[targetDim] = new Axis(this, attributeNames,
                    ArraysExt.resize(indices, i),
                    ArraysExt.resize(sizes, i));
        }
        return axes;
    }

    @Override
    protected double coordinateForCurrentAxis(final int j, final int i) {
        throw new UnsupportedOperationException();
    }
}
