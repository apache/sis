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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.SortedMap;
import javax.measure.Unit;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.internal.netcdf.Axis;
import org.apache.sis.internal.netcdf.Grid;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Dimension;
import org.apache.sis.internal.netcdf.Resources;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ArraysExt;
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
     * <div class="note">"GeoX" and "GeoY" stands for projected coordinates, not geocentric coordinates
     * (<a href="https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/reference/CoordinateAttributes.html#AxisTypes">source</a>).
     * </div>
     *
     * @see #getAxisType(String)
     */
    private static final Map<String,Character> AXIS_TYPES = new HashMap<>(26);
    static {
        addAxisTypes('λ', "longitude", "lon", "long");
        addAxisTypes('φ', "latitude",  "lat");
        addAxisTypes('H', "pressure", "height", "altitude", "barometric_altitude", "elevation", "elev", "geoz");
        addAxisTypes('D', "depth", "depth_below_geoid");
        addAxisTypes('E', "geox", "projection_x_coordinate");
        addAxisTypes('N', "geoy", "projection_y_coordinate");
        addAxisTypes('t', "t", "time", "runtime");
        addAxisTypes('x', "x");
        addAxisTypes('y', "y");
        addAxisTypes('z', "z");
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
     * Returns {@code this} if the dimensions in this grid appear in the same order than in the given array,
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
     * It should be equal to the size of the array returned by {@link #getAxes(Decoder)},
     * but caller should be robust to inconsistencies.
     */
    @Override
    public int getTargetDimensions() {
        return range.length;
    }

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
     * is associated. See {@link org.apache.sis.internal.netcdf.ucar.GridWrapper#getAxes(Decoder)} for a
     * closer look on the relationship between this algorithm and the UCAR library.
     *
     * <p>In this method, the words "domain" and "range" are used in the netCDF sense: they are the input
     * (domain) and output (range) of the function that convert grid indices to geodetic coordinates.</p>
     *
     * <p>The domain of all axes is often the same than the domain of the variable, but not necessarily.
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
         * amount of disk seek operations. Data loading may happen in this method through Axis constructor.
         */
        final SortedMap<VariableInfo,Integer> variables = new TreeMap<>();
        for (int i=0; i<range.length; i++) {
            final VariableInfo v = range[i];
            if (variables.put(v, i) != null) {
                throw new DataStoreContentException(Resources.format(Resources.Keys.DuplicatedAxis_2, getFilename(), v.getName()));
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
            /*
             * In Apache SIS implementation, the abbreviation determines the axis type. If a "_coordinateaxistype" attribute
             * exists, il will have precedence over all other heuristic rules in this method because it is the most specific
             * information about axis type. Otherwise the "standard_name" attribute is our first fallback since valid values
             * are standardized to "longitude" and "latitude" among others.
             */
            char abbreviation = getAxisType(axis.getAxisType());
            if (abbreviation == 0) {
                abbreviation = getAxisType(axis.getAttributeAsString(CF.STANDARD_NAME));
                /*
                 * If the abbreviation is still unknown, look at the "long_name", "description" or "title" attribute. Those
                 * attributes are not standardized, so they are less reliable than "standard_name". But they are still more
                 * reliable that the variable name since the long name may be "Longitude" or "Latitude" while the variable
                 * name is only "x" or "y".
                 */
                if (abbreviation == 0) {
                    abbreviation = getAxisType(axis.getDescription());
                    if (abbreviation == 0) {
                        /*
                         * Actually the "degree_east" and "degree_north" units of measurement are the most reliable way to
                         * identify geographic system, but we nevertheless check them almost last because the direction is
                         * already verified by Axis constructor. By checking the variable attributes first, we give a chance
                         * to Axis constructor to report a warning if there is an inconsistency.
                         */
                        if (Units.isAngular(axis.getUnit())) {
                            final AxisDirection direction = AxisDirections.absolute(Axis.direction(axis.getUnitsString()));
                            if (AxisDirection.EAST.equals(direction)) {
                                abbreviation = 'λ';
                            } else if (AxisDirection.NORTH.equals(direction)) {
                                abbreviation = 'φ';
                            }
                        }
                        /*
                         * We test the variable name last because that name is more at risk of being an uninformative "x" or "y" name.
                         * If even the variable name is not sufficient, we use some easy to recognize units.
                         */
                        if (abbreviation == 0) {
                            abbreviation = getAxisType(axis.getName());
                            if (abbreviation == 0) {
                                final Unit<?> unit = axis.getUnit();
                                if (Units.isTemporal(unit)) {
                                    abbreviation = 't';
                                } else if (Units.isPressure(unit)) {
                                    abbreviation = 'z';
                                }
                            }
                        }
                    }
                }
            }
            /*
             * Get the grid dimensions (part of the "domain" in UCAR terminology) used for computing
             * the ordinate values along the current axis. There is exactly 1 such grid dimension in
             * straightforward netCDF files. However some more complex files may have 2 dimensions.
             */
            int i = 0;
            final DimensionInfo[] axisDomain = axis.dimensions;
            final int[] indices = new int[axisDomain.length];
            final int[] sizes   = new int[axisDomain.length];
            for (final DimensionInfo dimension : axisDomain) {
                for (int sourceDim = 0; sourceDim < domain.length; sourceDim++) {
                    if (domain[sourceDim] == dimension) {
                        indices[i] = sourceDim;
                        sizes[i++] = dimension.length;
                        break;
                    }
                }
            }
            axes[targetDim] = new Axis(abbreviation, axis.getAttributeAsString(CF.POSITIVE),
                                       ArraysExt.resize(indices, i), ArraysExt.resize(sizes, i), axis);
        }
        return axes;
    }

    /**
     * Returns a hash code for this grid. A map of {@code GridInfo} is used by
     * {@link ChannelDecoder#getGrids()} for sharing existing instances.
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
