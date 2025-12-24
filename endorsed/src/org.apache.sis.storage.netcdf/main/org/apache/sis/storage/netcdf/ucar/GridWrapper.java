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
package org.apache.sis.storage.netcdf.ucar;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.netcdf.base.Axis;
import org.apache.sis.storage.netcdf.base.Grid;
import org.apache.sis.storage.netcdf.base.Decoder;
import org.apache.sis.storage.netcdf.base.Variable;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.Containers;


/**
 * Information about netCDF coordinate system, which include information about grid geometries.
 * In OGC/ISO specifications, the coordinate system and the grid geometries are distinct entities.
 * However, the UCAR model takes a different point of view where the coordinate system holds some
 * of the grid geometry information.
 *
 * <p>{@code GridWrapper} instances do not contain data; they are only about the geometry of grids.
 * Many netCDF variables may be associated to the same {@code GridWrapper} instance.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GridWrapper extends Grid {
    /**
     * The netCDF coordinate system to wrap.
     */
    private final CoordinateSystem netcdfCS;

    /**
     * Dimensions of the grid in netCDF order (reverse of "natural" order).
     * This is the same content as {@code netcdfCS.getDomain()} but potentially in a different order.
     * Reordering is needed when the order of dimensions in a variable does not match the order of dimensions in the grid.
     * There is no such mismatch with Apache SIS implementation of netCDF reader, but those mismatches sometimes happen with
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
        reordered = new HashMap<>(4);               // Will typically contain 0 or 1 entry.
        domain    = new ArrayList<>(cs.getDomain());
        /*
         * We need dimensions in netCDF order as declared in variables. But the netCDF UCAR library sometimes provides
         * axes in a different order, I'm not sure why. Maybe it uses the order in which dimensions are declared in
         * the global header (not the order declared on variables). We apply a standard order below. It is okay
         * if that order is not appropriate for a particular variable because the order will be reajusted by
         * `forDimensions(…)` method. We use the order which is most likely to be the order used by variables.
         */
        final Map<Dimension,AxisType> types = new HashMap<>();
        for (final CoordinateAxis axis : cs.getCoordinateAxes()) {
            final AxisType type = axis.getAxisType();
            for (final Dimension dim : axis.getDimensions()) {
                if (types.putIfAbsent(dim, type) == null) break;
            }
        }
        domain.sort((d1, d2) -> {
            final AxisType t1 = types.get(d1);
            final AxisType t2 = types.get(d2);
            if (t1 == t2)   return 0;
            if (t1 == null) return +1;
            if (t2 == null) return -1;
            return t1.axisOrder() - t2.axisOrder();
        });
    }

    /**
     * Creates a new grid geometry with the same coordinate system as the given parent
     * but dimensions in a different order. This is used for building coordinate systems
     * with axis order matching the order of dimensions in variables.
     */
    private GridWrapper(final GridWrapper parent, final List<Dimension> dimensions) {
        netcdfCS  = parent.netcdfCS;
        reordered = parent.reordered;
        domain    = dimensions;
        assert netcdfCS.getDomain().containsAll(dimensions);
    }

    /**
     * Returns a localization grid having the same dimensions as this grid but in a different order.
     * This method is invoked by {@link VariableWrapper#findGrid} when the localization grids created
     * by {@link DecoderWrapper} are not sufficient and must be tailored for a particular variable.
     * Returns {@code null} if this grid contains a dimension not in the given list.
     */
    @Override
    protected Grid forDimensions(final org.apache.sis.storage.netcdf.base.Dimension[] dimensions) {
        return forDimensions(Containers.viewAsUnmodifiableList(DimensionWrapper.unwrap(dimensions)));
    }

    /**
     * Implementation of {@link #forDimensions(org.apache.sis.storage.netcdf.base.Dimension[])}
     * after the Apache SIS objects have been unwrapped into UCAR objects.
     *
     * @param  dimensions  the desired dimensions, in order. May contain more dimensions than this grid.
     * @return localization grid with the exact same set of dimensions than this grid (no more and no less),
     *         but in the order specified by the given array (ignoring dimensions not in this grid).
     *         May be {@code this} or {@code null}.
     */
    private GridWrapper forDimensions(List<Dimension> dimensions) {
        if (dimensions.size() > domain.size()) {
            dimensions = new ArrayList<>(dimensions);
            dimensions.retainAll(domain);
        }
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
     * in the {@link CoordinateSystem} may not be the same order as the dimensions of the given variable.
     *
     * @param  variable  the variable for which to get its grid.
     * @param  systems   the coordinate systems of the given variable.
     * @return grid for the given variable, or {@code null} if none.
     */
    final GridWrapper forVariable(final ucar.nc2.Variable variable, final List<CoordinateSystem> systems, final String[] axisNames) {
        if (systems.contains(netcdfCS) && containsAllNamedAxes(axisNames)) {
            return forDimensions(variable.getDimensions());
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
     * Returns the number of dimensions of source coordinates in the <q>grid to CRS</q> conversion.
     * This is the number of dimensions of the <em>grid</em>.
     * It should be equal to the size of {@link #getDimensions()} list.
     */
    @Override
    public int getSourceDimensions() {
        return netcdfCS.getRankDomain();
    }

    /*
     * A `getTargetDimensions()` method would be like below, but is
     * excluded because `getAxes(…).length` is the authoritative value:
     *
     *     @Override
     *     public int getTargetDimensions() {
     *         return netcdfCS.getRankRange();
     *     }
     */

    /**
     * Returns the dimensions of this grid, in netCDF (reverse of "natural") order.
     */
    @Override
    protected List<org.apache.sis.storage.netcdf.base.Dimension> getDimensions() {
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
     * is often the same as the domain of the variable, but not necessarily.
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
         * In this method, `sourceDim` and `targetDim` are relative to "grid to CRS" conversion.
         * So `sourceDim` is the grid (domain) dimension and `targetDim` is the CRS (range) dimension.
         */
        int axisCount = 0;
        int targetDim = range.size();
        final Axis[] axes = new Axis[targetDim];
        while (--targetDim >= 0) {
            final CoordinateAxis axis = range.get(targetDim);
            final Variable wrapper = ((DecoderWrapper) decoder).getWrapperFor(axis);
            /*
             * The AttributeNames are for ISO 19115 metadata. They are not used for locating grid cells
             * on Earth, but we nevertheless get them now for making MetadataReader work easier.
             */
            char abbreviation = 0;
            final AxisType type = axis.getAxisType();
            if (type != null) switch (type) {
                case GeoX:            abbreviation = netcdfCS.isGeoXY() ? 'E' : 'x'; break;
                case GeoY:            abbreviation = netcdfCS.isGeoXY() ? 'N' : 'y'; break;
                case GeoZ:            abbreviation = netcdfCS.isGeoXY() ? 'H' : 'z'; break;
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
            if (abbreviation == 0) {
                abbreviation = org.apache.sis.storage.netcdf.base.AxisType.abbreviation(wrapper, true);
            }
            /*
             * Get the grid dimensions (part of the "domain" in UCAR terminology) used for computing
             * the coordinate values along the current axis. There is exactly 1 such grid dimension in
             * straightforward netCDF files. However, some more complex files may have 2 dimensions.
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
                 * package, we can proceed as if the dimension does not exist (`i` not incremented).
                 */
            }
            if (i != 0) {   // Variables with 0 dimensions sometimes happen.
                axes[axisCount++] = new Axis(abbreviation, axis.getPositive(), indices, sizes, i, wrapper);
            }
        }
        return ArraysExt.resize(axes, axisCount);
    }
}
