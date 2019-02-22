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
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import ucar.nc2.Dimension;
import ucar.nc2.VariableIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;
import org.apache.sis.internal.netcdf.Axis;
import org.apache.sis.internal.netcdf.Grid;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArraysExt;


/**
 * Information about netCDF coordinate system, which include information about grid geometries.
 * In OGC/ISO specifications, the coordinate system and the grid geometries are distinct entities.
 * However the UCAR model takes a different point of view where the coordinate system holds some
 * of the grid geometry information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
final class GridWrapper extends Grid {
    /**
     * The netCDF coordinate system to wrap.
     */
    private final CoordinateSystem netcdfCS;

    /**
     * Dimensions of the grid in netCDF order (reverse of "natural" order).
     * This is the same content than {@code netcdfCS.getDomain()} but potentially in a different order.
     * Reordering is needed when the order of dimensions in a variable does not match the order of dimensions in the grid.
     * There is no such mismatch with Apache SIS implementation of netCDF reader, but those mismatches sometime happen with
     * the wrappers around UCAR library where the following methods may return lists with elements in different order:
     *
     * <ul>
     *   <li>{@link ucar.nc2.Variable#getDimensions()}</li>
     *   <li>{@link ucar.nc2.dataset.CoordinateSystem#getDomain()}</li>
     * </ul>
     *
     * @see #getDimensions()
     */
    private final List<Dimension> domain;

    /**
     * Other {@code GridWrapper} using the same UCAR coordinate systems but with {@link #domain} dimensions in a different order.
     * We keep previously created {@code GridWrapper} instances in order to keep the cached value in {@link #geometry} field,
     * since its computation may be costly.
     */
    private final Map<List<Dimension>, GridWrapper> reordered;

    /**
     * Creates a new grid geometry for the given netCDF coordinate system.
     *
     * @param  cs  the netCDF coordinate system.
     */
    GridWrapper(final CoordinateSystem cs) {
        netcdfCS  = cs;
        domain    = cs.getDomain();
        reordered = new HashMap<>();
    }

    /**
     * Creates a new grid geometry with the same coordinate system than the given parent.
     */
    private GridWrapper(final GridWrapper parent, final List<Dimension> dimensions) {
        netcdfCS  = parent.netcdfCS;
        domain    = dimensions;
        reordered = parent.reordered;
        assert netcdfCS.getDomain().containsAll(dimensions);
    }

    /**
     * Returns a localization grid having the same dimensions than this grid but in a different order.
     * This method is invoked by {@link VariableWrapper#getGrid()} when the localization grids created
     * by {@link Decoder} subclasses are not sufficient and must be tailored for a particular variable.
     * Returns {@code null} the the given dimensions are not members of this grid.
     */
    @Override
    protected Grid derive(final org.apache.sis.internal.netcdf.Dimension[] dimensions) {
        return derive(UnmodifiableArrayList.wrap(DimensionWrapper.unwrap(dimensions)));
    }

    /**
     * Returns a localization grid wrapping the same coordinate system than this grid but with dimensions in different order.
     * This is the implementation of {@link #derive(org.apache.sis.internal.netcdf.Dimension[])} after the Apache SIS objects
     * have been unwrapped into UCAR objects. Returns {@code null} the the given dimensions are not members of this grid.
     *
     * @param  dimensions  the dimensions of this grid but potentially in a different order.
     * @return localization grid with given dimension order (may be {@code this}), or {@code null}.
     */
    private GridWrapper derive(final List<Dimension> dimensions) {
        if (domain.equals(dimensions)) {
            return this;
        }
        return reordered.computeIfAbsent(dimensions, k -> {
            // Want same set of dimensions in different order.
            if (domain.size() == k.size() && domain.containsAll(k)) {
                return new GridWrapper(this, k);
            }
            return null;
        });
    }

    /**
     * Returns the grid to use for the given variable. This method is needed because the order of dimensions declared
     * in the {@link CoordinateSystem} may not be the same order than the dimensions of the given variable.
     *
     * @param  variable  the variable for which to get its grid.
     * @param  systems   the coordinate systems of the given variable.
     * @return grid for the given variable, or {@code null} if none.
     */
    final GridWrapper forVariable(final VariableIF variable, final List<CoordinateSystem> systems, final String[] axisNames) {
        if (systems.contains(netcdfCS) && containsAllNamedAxes(axisNames)) {
            return derive(variable.getDimensions());
        }
        return null;
    }

    /**
     * Returns a name for this grid geometry, for information purpose only.
     */
    @Override
    public String getName() {
        return netcdfCS.getName();
    }

    /**
     * Returns the number of dimensions of source coordinates in the <cite>"grid to CRS"</cite> conversion.
     * This is the number of dimensions of the <em>grid</em>.
     */
    @Override
    public int getSourceDimensions() {
        return netcdfCS.getRankDomain();
    }

    /**
     * Returns the number of dimensions of target coordinates in the <cite>"grid to CRS"</cite> conversion.
     * This is the number of dimensions of the <em>coordinate reference system</em>.
     * It should be equal to the size of the array returned by {@link #getAxes(Decoder)},
     * but caller should be robust to inconsistencies.
     */
    @Override
    public int getTargetDimensions() {
        return netcdfCS.getRankRange();
    }

    /**
     * Returns the dimensions of this grid, in netCDF (reverse of "natural") order.
     */
    @Override
    protected List<org.apache.sis.internal.netcdf.Dimension> getDimensions() {
        return DimensionWrapper.wrap(domain);
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
                for (final CoordinateAxis axis : netcdfCS.getCoordinateAxes()) {
                    if (name.equalsIgnoreCase(axis.getShortName())) {
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
     * is associated.
     *
     * <p>In this method, the words "domain" and "range" are used in the netCDF sense: they are the input
     * (domain) and output (range) of the function that convert grid indices to geodetic coordinates.</p>
     *
     * <p>The domain of all axes (or the {@linkplain CoordinateSystem#getDomain() coordinate system domain})
     * is often the same than the domain of the variable, but not necessarily.
     * In particular, the relationship is not straightforward when the coordinate system contains instances
     * of {@link CoordinateAxis2D}.</p>
     *
     * @param  decoder  the decoder of the netCDF file from which to create axes.
     * @return the CRS axes, in "natural" order (reverse of netCDF order).
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    @Override
    protected Axis[] createAxes(final Decoder decoder) throws IOException, DataStoreException {
        final List<CoordinateAxis> range = netcdfCS.getCoordinateAxes();
        /*
         * In this method, 'sourceDim' and 'targetDim' are relative to "grid to CRS" conversion.
         * So 'sourceDim' is the grid (domain) dimension and 'targetDim' is the CRS (range) dimension.
         */
        int targetDim = range.size();
        final Axis[] axes = new Axis[targetDim];
        while (--targetDim >= 0) {
            final CoordinateAxis axis = range.get(targetDim);
            /*
             * The AttributeNames are for ISO 19115 metadata. They are not used for locating grid cells
             * on Earth, but we nevertheless get them now for making MetadataReader work easier.
             */
            char abbreviation = 0;
            final AxisType type = axis.getAxisType();
            if (type != null) switch (type) {
                case GeoX:            abbreviation = 'x'; break;
                case GeoY:            abbreviation = 'y'; break;
                case GeoZ:            abbreviation = 'z'; break;
                case Lon:             abbreviation = 'λ'; break;
                case Lat:             abbreviation = 'φ'; break;
                case Pressure:        // Fallthrough: consider as Height
                case Height:          abbreviation = 'H'; break;
                case RunTime:         // Fallthrough: consider as Time
                case Time:            abbreviation = 't'; break;
                case RadialAzimuth:   abbreviation = 'θ'; break;    // Spherical longitude
                case RadialElevation: abbreviation = 'Ω'; break;    // Spherical latitude
                case RadialDistance:  abbreviation = 'r'; break;    // Geocentric radius
            }
            /*
             * Get the grid dimensions (part of the "domain" in UCAR terminology) used for computing
             * the ordinate values along the current axis. There is exactly 1 such grid dimension in
             * straightforward netCDF files. However some more complex files may have 2 dimensions.
             */
            int i = 0;
            final List<Dimension> axisDomain = axis.getDimensions();
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
                 * then there is maybe a problem with the netCDF file. However for the purpose of this
                 * package, we can proceed as if the dimension does not exist ('i' not incremented).
                 */
            }
            axes[targetDim] = new Axis(abbreviation, axis.getPositive(),
                    ArraysExt.resize(indices, i), ArraysExt.resize(sizes, i),
                    ((DecoderWrapper) decoder).getWrapperFor(axis));
        }
        /*
         * We want axes in "natural" order. But the netCDF UCAR library sometime provides axes already
         * in that order and sometime in reverse order (netCDF order). I'm not aware of a reliable way
         * to determine whether axis order provided by UCAR library needs to be reverted since I don't
         * know what determines that order (the file format? the presence of "coordinates" attribute?).
         * For now we compare axis order with dimension order, and if the axis contains all dimensions
         * in the same order we presume that this is the "netCDF" order (as opposed to a "coordinates"
         * attribute value order).
         */
        for (int i = Math.min(domain.size(), range.size()); --i >= 0;) {
            final List<Dimension> dimensions = range.get(i).getDimensions();
            if (dimensions.size() != 1 || !dimensions.get(0).equals(domain.get(i))) {
                return axes;
            }
        }
        ArraysExt.reverse(axes);
        return axes;
    }
}
