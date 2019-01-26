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

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.SortedMap;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.netcdf.Axis;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.Grid;
import org.apache.sis.internal.netcdf.Resources;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.measure.Units;
import ucar.nc2.constants.CF;


/**
 * Description of a grid geometry found in a netCDF file.
 *
 * <p>In this class, the words "domain" and "range" are used in the netCDF sense: they are the input
 * (domain) and output (range) of the function that convert grid indices to geodetic coordinates.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
final class GridInfo extends Grid {
    /**
     * Mapping from values of the {@code "_CoordinateAxisType"} attribute or axis name to the abbreviation.
     * Keys are lower cases and values are controlled vocabulary documented in {@link Axis#abbreviation}.
     *
     * @see #getAxisType(String)
     */
    private static final Map<String,Character> AXIS_TYPES = new HashMap<>(26);
    static {
        addAxisTypes('λ', "longitude", "lon", "long");
        addAxisTypes('φ', "latitude",  "lat");
        addAxisTypes('H', "pressure", "height", "altitude", "elevation", "elev");
        addAxisTypes('D', "depth");
        addAxisTypes('t', "t", "time", "runtime");
        addAxisTypes('x', "x", "geox");
        addAxisTypes('y', "y", "geoy");
        addAxisTypes('z', "z", "geoz");
    }

    /**
     * Adds a sequence of axis types or variable names for the given abbreviation.
     */
    private static void addAxisTypes(final char abbreviation, final String... names) {
        final Character c = abbreviation;
        for (final String name : names) {
            AXIS_TYPES.put(name, c);
        }
    }

    /**
     * Describes the input values expected by the function converting grid indices to geodetic coordinates.
     * They are the dimensions of the grid (<strong>not</strong> the dimensions of the CRS).
     *
     * @see #getShape()
     */
    private final Dimension[] domain;

    /**
     * Describes the output values calculated by the function converting grid indices to geodetic coordinates.
     * They are the coordinate values expressed in the CRS.
     */
    private final VariableInfo[] range;

    /**
     * Whether axes should be sorted instead than relying on the order found in netCDF file.
     */
    private final boolean sortAxes;

    /**
     * Constructs a new grid geometry information.
     * The {@code domain} and {@code range} arrays often have the same length, but not necessarily.
     *
     * @param  domain    describes the input values of the "grid to CRS" conversion.
     * @param  range     the output values of the "grid to CRS" conversion.
     * @param  sortAxes  whether axes should be sorted instead than relying on the order found in netCDF file.
     */
    GridInfo(final Dimension[] domain, final VariableInfo[] range, final boolean sortAxes) {
        this.domain   = domain;
        this.range    = range;
        this.sortAxes = sortAxes;
    }

    /**
     * Returns the name of the netCDF file containing this grid geometry, or {@code null} if unknown.
     */
    private String getFilename() {
        for (final VariableInfo info : range) {
            final String filename = info.getFilename();
            if (filename != null) return filename;
        }
        return null;
    }

    /**
     * Returns a name for this grid geometry, for information purpose only.
     */
    @Override
    public String getName() {
        return listNames(range, range.length, " ");
    }

    /**
     * Returns the axis type for an axis of the given name, or 0 if unknown.
     * If non-zero, then the returned code is one of the controlled vocabulary
     * documented in {@link Axis#abbreviation}.
     */
    private static char getAxisType(final String name) {
        if (name != null) {
            final Character abbreviation = AXIS_TYPES.get(name.toLowerCase(Locale.US));
            if (abbreviation != null) {
                return abbreviation;
            }
        }
        return 0;
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
     * It should be equal to the size of the array returned by {@link #getAxes()},
     * but caller should be robust to inconsistencies.
     */
    @Override
    public int getTargetDimensions() {
        return range.length;
    }

    /**
     * Returns the number of cells along each source dimension, in "natural" order.
     *
     * @return number of cells along each source dimension, in "natural" (opposite of netCDF) order.
     */
    @Override
    protected long[] getShape() {
        final int    dim  = domain.length;
        final long[] size = new long[dim];
        for (int i=0; i<dim; i++) {
            size[(dim-1) - i] = Integer.toUnsignedLong(domain[i].length);
        }
        return size;
    }

    /**
     * Returns all axes of the netCDF coordinate system, together with the grid dimension to which the axis
     * is associated. See {@link org.apache.sis.internal.netcdf.ucar.GridWrapper#getAxes()} for a closer look
     * on the relationship between this algorithm and the UCAR library.
     *
     * <p>In this method, the words "domain" and "range" are used in the netCDF sense: they are the input
     * (domain) and output (range) of the function that convert grid indices to geodetic coordinates.</p>
     *
     * <p>The domain of all axes is often the same than the domain of the variable, but not necessarily.
     * In particular, the relationship is not straightforward when the coordinate system contains
     * "two-dimensional axes" (in {@link ucar.nc2.dataset.CoordinateAxis2D} sense).</p>
     *
     * @return the CRS axes, in netCDF order (reverse of "natural" order).
     * @throws DataStoreException if a logical error occurred.
     */
    @Override
    protected Axis[] createAxes() throws DataStoreException {
        /*
         * Process the variables in the order the appear in the sequence of bytes that make the netCDF files.
         * This is often the same order than the indices, but not necessarily. The intent is to reduce the
         * amount of disk seek operations. Data loading may happen in this method through Axis constructor.
         */
        final SortedMap<VariableInfo,Integer> variables = new TreeMap<>();
        for (int i=0; i<range.length; i++) {
            final VariableInfo v = range[i];
            if (variables.put(v, i) != null) {
                throw new DataStoreContentException(Resources.format(Resources.Keys.DuplicatedReference_2, getFilename(), v.getName()));
            }
        }
        /*
         * In this method, 'sourceDim' and 'targetDim' are relative to "grid to CRS" conversion.
         * So 'sourceDim' is the grid (domain) dimension and 'targetDim' is the CRS (range) dimension.
         */
        final Axis[] axes = new Axis[range.length];
        for (final SortedMap.Entry<VariableInfo,Integer> entry : variables.entrySet()) {
            final int targetDim = entry.getValue();
            final VariableInfo axis = entry.getKey();
            char abbreviation = getAxisType(axis.getAxisType());
            if (abbreviation == 0) {
                abbreviation = getAxisType(axis.getName());
                if (abbreviation == 0) {
                    if (Units.isTemporal(axis.getUnit())) {
                        abbreviation = 't';
                    }
                }
            }
            /*
             * Get the grid dimensions (part of the "domain" in UCAR terminology) used for computing
             * the ordinate values along the current axis. There is exactly 1 such grid dimension in
             * straightforward netCDF files. However some more complex files may have 2 dimensions.
             */
            int i = 0;
            final Dimension[] axisDomain = axis.dimensions;
            final int[] indices = new int[axisDomain.length];
            final int[] sizes   = new int[axisDomain.length];
            for (final Dimension dimension : axisDomain) {
                for (int sourceDim = domain.length; --sourceDim >= 0;) {
                    if (domain[sourceDim] == dimension) {
                        indices[i] = sourceDim;
                        sizes[i++] = dimension.length;
                        break;
                    }
                }
            }
            axes[targetDim] = new Axis(axis, abbreviation, axis.getAttributeAsString(CF.POSITIVE),
                                       ArraysExt.resize(indices, i), ArraysExt.resize(sizes, i));
        }
        if (sortAxes) {
            Arrays.sort(axes);
        }
        return axes;
    }

    /**
     * Returns a coordinate for the given two-dimensional grid coordinate axis.
     * This is (indirectly) a callback method for {@link #getAxes()}.
     *
     * @throws ArithmeticException if the axis size exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    @Override
    protected double coordinateForAxis(final Variable axis, final int j, final int i) throws IOException, DataStoreException {
        final VariableInfo v = (VariableInfo) axis;
        final int n = v.dimensions[0].length;
        return v.read().doubleValue(j + n*i);
    }

    /**
     * Returns a hash code for this grid. A map of {@code GridInfo} is used by
     * {@link ChannelDecoder#getGridGeometries()} for sharing existing instances.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(domain) ^ Arrays.hashCode(range);
    }

    /**
     * Compares the grid with the given object for equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof GridInfo) {
            final GridInfo that = (GridInfo) other;
            return Arrays.equals(domain, that.domain) &&
                   Arrays.equals(range,  that.range);
        }
        return false;
    }
}
