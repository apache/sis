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
package org.apache.sis.storage.netcdf.classic;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import ucar.nc2.constants.CF;       // String constants are copied by the compiler with no UCAR reference left.
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.netcdf.base.Axis;
import org.apache.sis.storage.netcdf.base.AxisType;
import org.apache.sis.storage.netcdf.base.Grid;
import org.apache.sis.storage.netcdf.base.Decoder;
import org.apache.sis.storage.netcdf.base.Dimension;
import org.apache.sis.storage.netcdf.internal.Resources;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;


/**
 * Description of a grid geometry found in a netCDF file.
 *
 * <p>In this class, the words "domain" and "range" are used in the netCDF sense: they are the input
 * (domain) and output (range) of the function that convert grid indices to geodetic coordinates.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
final class GridInfo extends Grid {
    /**
     * Describes the input values expected by the function converting grid indices to geodetic coordinates.
     * They are the dimensions of the grid (<strong>not</strong> the dimensions of the CRS).
     * Dimensions are listed in the order they appear in netCDF file (reverse of "natural" order).
     *
     * @see #getDimensions()
     * @see VariableInfo#dimensions
     */
    private final DimensionInfo[] domain;

    /**
     * Describes the output values calculated by the function converting grid indices to geodetic coordinates.
     * They are the coordinate values expressed in the CRS. Order should be the order to be declared in the CRS.
     * This is often, but not necessarily, the reverse order than the {@link #domain} dimension.
     */
    private final VariableInfo[] range;

    /**
     * Constructs a new grid geometry information.
     * The {@code domain} and {@code range} arrays often have the same length, but not necessarily.
     *
     * @param  domain  describes the input values of the "grid to CRS" conversion, in netCDF order.
     * @param  range   the output values of the "grid to CRS" conversion, in CRS order as much as possible.
     */
    GridInfo(final DimensionInfo[] domain, final VariableInfo[] range) {
        this.domain = domain;
        this.range  = range;
    }

    /**
     * Returns {@code this} if the dimensions in this grid appear in the same order as in the given array,
     * or {@code null} otherwise. Current implementation does not apply the dimension reordering documented
     * in parent class because reordering should not be needed for this SIS implementation of netCDF reader.
     * Reordering is more needed for the implementation based on UCAR library.
     */
    @Override
    protected Grid forDimensions(final Dimension[] dimensions) {
        int i = 0;
        for (Dimension required : domain) {
            do if (i >= dimensions.length) return null;
            while (!required.equals(dimensions[i++]));
        }
        return this;
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
     * Returns the number of dimensions of source coordinates in the <q>grid to CRS</q> conversion.
     * This is the number of dimensions of the <em>grid</em>.
     */
    @Override
    public int getSourceDimensions() {
        return domain.length;
    }

    /*
     * A `getTargetDimensions()` method would be like below, but is
     * excluded because `getAxes(…).length` is the authoritative value:
     *
     *     @Override
     *     public int getTargetDimensions() {
     *         return range.length;
     *     }
     */

    /**
     * Returns the dimensions of this grid, in netCDF (reverse of "natural") order.
     */
    @Override
    protected List<Dimension> getDimensions() {
        return UnmodifiableArrayList.wrap(domain);
    }

    /**
     * Returns {@code true} if this grid contains all axes of the specified names, ignoring case.
     * If the given array is null, then no filtering is applied and this method returns {@code true}.
     * If the grid contains more axes than the named ones, then the additional axes are ignored.
     */
    @Override
    protected boolean containsAllNamedAxes(final String[] axisNames) {
        if (axisNames != null) {
next:       for (final String name : axisNames) {
                for (final VariableInfo axis : range) {
                    if (name.equalsIgnoreCase(axis.getName())) {
                        continue next;
                    }
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Returns all axes of the netCDF coordinate system, together with the grid dimension to which the axis
     * is associated. See {@link org.apache.sis.storage.netcdf.ucar.GridWrapper#getAxes(Decoder)} for a
     * closer look on the relationship between this algorithm and the UCAR library.
     *
     * <p>In this method, the words "domain" and "range" are used in the netCDF sense: they are the input
     * (domain) and output (range) of the function that convert grid indices to geodetic coordinates.</p>
     *
     * <p>The domain of all axes is often the same as the domain of the variable, but not necessarily.
     * In particular, the relationship is not straightforward when the coordinate system contains
     * "two-dimensional axes" (in {@link ucar.nc2.dataset.CoordinateAxis2D} sense).</p>
     *
     * @param  decoder  the decoder of the netCDF file from which to create axes.
     * @return the CRS axes, in "natural" order (reverse of netCDF order).
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    @Override
    protected Axis[] createAxes(final Decoder decoder) throws IOException, DataStoreException {
        /*
         * Process the variables in the order the appear in the sequence of bytes that make the netCDF files.
         * This is often the reverse order of range indices, but not necessarily. The intent is to reduce the
         * number of disk seek operations. Data loading may happen in this method through Axis constructor.
         */
        final var variables = new TreeMap<VariableInfo,Integer>();
        for (int i=0; i<range.length; i++) {
            final VariableInfo v = range[i];
            if (variables.put(v, i) != null) {
                throw new DataStoreContentException(decoder.resources().getString(
                        Resources.Keys.DuplicatedAxis_2, getFilename(), v.getName()));
            }
        }
        /*
         * In this method, `sourceDim` and `targetDim` are relative to "grid to CRS" conversion.
         * So `sourceDim` is the grid (domain) dimension and `targetDim` is the CRS (range) dimension.
         */
        final Axis[] axes = new Axis[range.length];
        for (final Map.Entry<VariableInfo,Integer> entry : variables.entrySet()) {
            final int targetDim = entry.getValue();
            final VariableInfo axis = entry.getKey();
            /*
             * Get the grid dimensions (part of the "domain" in UCAR terminology) used for computing
             * the coordinate values along the current axis. There is exactly 1 such grid dimension in
             * straightforward netCDF files. However, some more complex files may have 2 dimensions.
             *
             * An axis may have two-dimensions but be conceptually one-dimensional if the coordinate values
             * are given as character strings. While rare, this is sometime observed in practice for dates.
             * In such case, the dimension used for indexing the characters in each value should not appear
             * in the `domain` field of this grid. Therefore, that artificial dimension should be discarded
             * by the loop below. This is okay because either the strings are automatically parsed as numbers
             * by `org.apache.sis.math.ArrayVector.ASCII`, or either that variable have already been converted
             * to a one-dimensional vector of numbers by `VariableTransformer`.
             */
            int i = 0;
            final DimensionInfo[] axisDomain = axis.dimensions;
            final int[] indices = new int[axisDomain.length];
            final int[] sizes   = new int[axisDomain.length];
            for (final DimensionInfo dimension : axisDomain) {
                for (int sourceDim = 0; sourceDim < domain.length; sourceDim++) {
                    if (domain[sourceDim] == dimension) {
                        indices[i] = sourceDim;
                        sizes[i++] = dimension.length;              // Handled as unsigned intengers.
                        break;
                    }
                }
            }
            final char abbreviation = AxisType.abbreviation(axis, true);
            axes[targetDim] = new Axis(abbreviation, axis.getAttributeAsString(CF.POSITIVE), indices, sizes, i, axis);
        }
        return axes;
    }

    /**
     * Returns a hash code for this grid. A map of {@code GridInfo} is used by
     * {@link ChannelDecoder#getGridCandidates()} for sharing existing instances.
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
