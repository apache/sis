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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import javax.measure.Unit;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.measure.Units;
import org.apache.sis.math.Vector;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;


/**
 * Information about a coordinate system axes. In netCDF files, all axes can be related to 1 or more dimensions
 * of the grid domain. Those grid domain dimensions are specified by the {@link #sourceDimensions} array.
 * Whether the array length is 1 or 2 depends on whether the wrapped netCDF axis is an instance of
 * {@link ucar.nc2.dataset.CoordinateAxis1D} or {@link ucar.nc2.dataset.CoordinateAxis2D} respectively.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see Grid#getAxes()
 *
 * @since 0.3
 * @module
 */
public final class Axis extends NamedElement {
    /**
     * The abbreviation, also used as a way to identify the axis type.
     * This is a controlled vocabulary: if any abbreviation is changed,
     * then we need to search for all usages in the code and update it.
     * Possible values are:
     * <ul>
     *   <li>λ for longitude</li>
     *   <li>φ for latitude</li>
     *   <li>t for time</li>
     *   <li>h for ellipsoidal height</li>
     *   <li>H for geoidal height</li>
     *   <li>D for depth</li>
     *   <li>E for easting</li>
     *   <li>N for northing</li>
     *   <li>θ for spherical longitude (azimuthal angle)</li>
     *   <li>Ω for spherical latitude (polar angle)</li>
     *   <li>r for geocentric radius</li>
     *   <li>x,y,z for axes that are labeled as such, without more information.</li>
     *   <li>zero for unknown axes.</li>
     * </ul>
     *
     * @see AxisDirections#fromAbbreviation(char)
     * @see CRSBuilder#dispatch(List, Axis)
     */
    public final char abbreviation;

    /**
     * The axis direction, or {@code null} if unknown.
     */
    final AxisDirection direction;

    /**
     * The indices of the grid dimension associated to this axis. The length of this array is often 1.
     * But if more than one grid dimension is associated to this axis (i.e. if the wrapped netCDF axis
     * is an instance of {@link ucar.nc2.dataset.CoordinateAxis2D}),  then the first value is the grid
     * dimension which seems most closely oriented toward this axis direction. We do that for allowing
     * {@code MetadataReader.addSpatialRepresentationInfo(…)} method to get the most appropriate value
     * for ISO 19115 {@code metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionSize}
     * metadata property.
     */
    final int[] sourceDimensions;

    /**
     * The number of cell elements along the source grid dimensions. The length of this array shall be
     * equals to the {@link #sourceDimensions} length. For each element, {@code sourceSizes[i]} shall
     * be equals to the number of grid cells in the grid dimension at index {@code sourceDimensions[i]}.
     */
    public final int[] sourceSizes;

    /**
     * Values of coordinates on this axis for given grid indices. This variables is often one-dimensional,
     * but can also be two-dimensional.
     */
    final Variable coordinates;

    /**
     * Constructs a new axis associated to an arbitrary number of grid dimension.
     * In the particular case where the number of dimensions is equals to 2, this constructor will detect
     * by itself which grid dimension varies fastest and reorder in-place the elements in the given arrays
     * (those array are modified, not cloned).
     *
     * @param  owner             provides callback for the conversion from grid coordinates to geodetic coordinates.
     * @param  axis              an implementation-dependent object representing the axis.
     * @param  abbreviation      axis abbreviation, also identifying its type. This is a controlled vocabulary.
     * @param  direction         direction of positive values ("up" or "down"), or {@code null} if unknown.
     * @param  sourceDimensions  the index of the grid dimension associated to this axis.
     * @param  sourceSizes       the number of cell elements along that axis.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    public Axis(final Grid owner, final Variable axis, char abbreviation, final String direction,
                final int[] sourceDimensions, final int[] sourceSizes) throws IOException, DataStoreException
    {
        /*
         * Try to get the axis direction from one of the following sources,
         * in preference order (unless an inconsistency is detected):
         *
         *   1) The "positive" attribute value, which can be "up" or "down".
         *   2) The abbreviation, which indirectly tells us the axis type inferred by UCAR library.
         *   3) The direction in unit of measurement formatted as "degrees east" or "degrees north".
         *
         * Choice #1 is preferred because it is the only one telling us the direction of increasing values.
         * However if we find an inconsistency between directions inferred in those different ways, then we
         * give precedence to choices #2 and #3 in that order. Choice #1 is not considered authoritative
         * because it applies (in principle) only to vertical axis.
         */
        AxisDirection dir = Types.forCodeName(AxisDirection.class, direction, false);
        AxisDirection check = AxisDirections.fromAbbreviation(abbreviation);
        final boolean isSigned = (dir != null);     // Whether 'dir' takes in account the direction of positive values.
        boolean isConsistent = true;
        if (dir == null) {
            dir = check;
        } else if (check != null) {
            isConsistent = AxisDirections.isColinear(dir, check);
        }
        if (isConsistent) {
            check = direction(axis.getUnitsString());
            if (dir == null) {
                dir = check;
            } else if (check != null) {
                isConsistent = AxisDirections.isColinear(dir, check);
            }
        }
        if (!isConsistent) {
            axis.warning(Grid.class, "getAxes",                 // Caller of this constructor.
                         Resources.Keys.AmbiguousAxisDirection_4, axis.getFilename(), axis.getName(), dir, check);
            if (isSigned) {
                if (AxisDirections.isOpposite(dir)) {
                    check = AxisDirections.opposite(check);         // Apply the sign of 'dir' on 'check'.
                }
                dir = check;
            }
        }
        this.direction        = dir;
        this.abbreviation     = abbreviation;
        this.sourceDimensions = sourceDimensions;
        this.sourceSizes      = sourceSizes;
        this.coordinates      = axis;
        if (sourceDimensions.length == 2) {
            final int up0  = sourceSizes[0];
            final int up1  = sourceSizes[1];
            final int mid0 = up0 / 2;
            final int mid1 = up1 / 2;
            final double inc0 = (owner.coordinateForAxis(axis,     0, mid1) -
                                 owner.coordinateForAxis(axis, up0-1, mid1)) / up0;
            final double inc1 = (owner.coordinateForAxis(axis, mid0,     0) -
                                 owner.coordinateForAxis(axis, mid0, up1-1)) / up1;
            if (Math.abs(inc1) > Math.abs(inc0)) {
                sourceSizes[0] = up1;
                sourceSizes[1] = up0;
                ArraysExt.swap(sourceDimensions, 0, 1);
            }
        }
    }

    /**
     * Returns the axis direction for the given unit of measurement, or {@code null} if unknown.
     * This method performs the second half of the work of parsing "degrees_east" or "degrees_west" units.
     *
     * @param  unit  the string representation of the netCDF unit, or {@code null}.
     * @return the axis direction, or {@code null} if unrecognized.
     */
    public static AxisDirection direction(final String unit) {
        if (unit != null) {
            int s = unit.indexOf('_');
            if (s < 0) {
                s = unit.indexOf(' ');
            }
            if (s > 0) {
                return Types.forCodeName(AxisDirection.class, unit.substring(s+1), false);
            }
        }
        return null;
    }

    /**
     * Returns the name of this axis.
     *
     * @return the name of this element.
     */
    @Override
    public final String getName() {
        return coordinates.getName().trim();
    }

    /**
     * Returns the unit of measurement of this axis, or {@code null} if unknown.
     *
     * @return the unit of measurement, or {@code null} if unknown.
     */
    public final Unit<?> getUnit() {
        return coordinates.getUnit();
    }

    /**
     * Returns {@code true} if the given axis specifies the same direction and unit of measurement than this axis.
     * This is used for testing is a predefined axis can be used instead than invoking {@link #toISO(CSFactory)}.
     */
    final boolean isSameUnitAndDirection(final CoordinateSystemAxis axis) {
        return axis.getDirection().equals(direction) && axis.getUnit().equals(getUnit());
    }

    /**
     * Creates an ISO 19111 axis from the information stored in this netCDF axis.
     *
     * @param  factory  the factory to use for creating the coordinate system axis.
     * @return the ISO axis.
     */
    final CoordinateSystemAxis toISO(final CSFactory factory) throws FactoryException {
        /*
         * The axis name is stored without namespace, because the variable name in a netCDF file can be anything;
         * this is not controlled vocabulary. However the standard name, if any, is stored with "NetCDF" namespace
         * because this is controlled vocabulary.
         */
        final String name = getName();
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(CoordinateSystemAxis.NAME_KEY, name);                        // Intentionally no namespace.
        final List<GenericName> aliases = new ArrayList<>(2);
        final String standardName = coordinates.getAttributeString(CF.STANDARD_NAME);
        if (standardName != null) {
            final NamedIdentifier std = new NamedIdentifier(Citations.NETCDF, standardName);
            if (standardName.equals(name)) {
                properties.put(CoordinateSystemAxis.NAME_KEY, std);                 // Store as primary name.
            } else {
                aliases.add(std);                                                   // Store as alias.
            }
        }
        /*
         * The long name is stored as an optional description of the primary name.
         * It is also stored as an alias if not redundant with other names.
         */
        final String alt = coordinates.getAttributeString(CDM.LONG_NAME);
        if (alt != null && !similar(alt, name)) {
            properties.put(org.opengis.metadata.Identifier.DESCRIPTION_KEY, alt);   // Description associated to primary name.
            if (!similar(alt, standardName)) {
                aliases.add(new NamedIdentifier(null, alt));                        // Additional alias.
            }
        }
        if (!aliases.isEmpty()) {
            properties.put(CoordinateSystemAxis.ALIAS_KEY, aliases.toArray(new GenericName[aliases.size()]));
        }
        /*
         * Axis abbreviation, direction and unit of measurement are mandatory. If any of them is null,
         * creation of CoordinateSystemAxis is likely to fail with an InvalidGeodeticParameterException.
         * We provide default values for the most well-accepted values and leave other values to null.
         * Those null values can be accepted if users specify their own factory.
         */
        Unit<?> unit = getUnit();
        if (unit == null) {
            switch (abbreviation) {
                /*
                 * TODO: consider moving those default values in a separated class,
                 * for example a netCDF-specific CSFactory, for allowing users to override.
                 */
                case 'λ': case 'φ': unit = Units.DEGREE; break;
            }
        }
        final String abbr;
        if (abbreviation != 0) {
            abbr = Character.toString(abbreviation).intern();
        } else if (direction != null && unit != null) {
            abbr = AxisDirections.suggestAbbreviation(name, direction, unit);
        } else {
            abbr = null;
        }
        return factory.createCoordinateSystemAxis(properties, abbr, direction, unit);
    }

    /**
     * Sets the scale and offset coefficients in the given "grid to CRS" transform if possible.
     * Source and target dimensions used by this method are in "natural" order (reverse of netCDF order).
     * Setting the coefficient is possible only if values in this variable are regular,
     * i.e. the difference between two consecutive values is constant.
     *
     * <p>If this method returns {@code true}, then the {@code nonLinears} list is left unchanged.
     * If this method returns {@code false}, then a non-linear transform or {@code null} has been
     * added to the {@code nonLinears} list.</p>
     *
     * @param  gridToCRS   the matrix in which to set scale and offset coefficient.
     * @param  srcEnd      number of source dimensions (grid dimensions) - 1. Identifies the last column in the matrix.
     * @param  tgtDim      the target dimension, which is a dimension of the CRS. Identifies the matrix row of scale factor.
     * @param  nonLinears  where to add a non-linear transform if we can not compute a linear one. {@code null} may be added.
     * @return whether this method successfully set the scale and offset coefficients.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     */
    final boolean trySetTransform(final Matrix gridToCRS, final int srcEnd, final int tgtDim,
            final List<MathTransform> nonLinears) throws IOException, DataStoreException
    {
        /*
         * Normal case where the axis has only one dimension.
         */
        if (sourceDimensions.length == 1) {
            final int srcDim = srcEnd - sourceDimensions[0];
            if (coordinates.trySetTransform(gridToCRS, srcDim, tgtDim, null)) {
                return true;
            } else {
                nonLinears.add(MathTransforms.interpolate(null, coordinates.read().doubleValues()));
                return false;
            }
        }
        /*
         * In netCDF files, axes are sometime associated to two-dimensional localization grids.
         * If this is the case, then the following block checks if we can reduce those grids to
         * one-dimensional vector. For example the following localisation grids:
         *
         *    10 10 10 10                  10 12 15 20
         *    12 12 12 12        or        10 12 15 20
         *    15 15 15 15                  10 12 15 20
         *    20 20 20 20                  10 12 15 20
         *
         * can be reduced to a one-dimensional {10 12 15 20} vector (orientation matter however).
         *
         * Note: following block is currently restricted to the two-dimensional case, but it could
         * be generalized to n-dimensional case if we resolve the default case in the switch statement.
         */
        if (sourceDimensions.length == 2) {
            Vector data = coordinates.read();
            if (!coordinates.readTriesToCompress() || data.getClass().getSimpleName().equals("RepeatedVector")) {
                final int[] repetitions = data.repetitions();       // Detects repetitions as illustrated above.
                if (repetitions.length != 0) {
                    for (int i=0; i<sourceDimensions.length; i++) {
                        final int srcDim = srcEnd - sourceDimensions[i];    // "Natural" order is reverse of netCDF order.
                        final int length = sourceSizes[i];
                        int step = 1;
                        for (int j=0; j<sourceDimensions.length; j++) {
                            int previous = srcEnd - sourceDimensions[j];
                            if (previous < srcDim) step *= sourceSizes[j];
                        }
                        final boolean condition;
                        switch (srcDim) {
                            case 0:  condition = repetitions.length > 1 && (repetitions[1] % length) == 0; break;
                            case 1:  condition =                           (repetitions[0] % step)   == 0; break;
                            default: throw new AssertionError();        // I don't know yet how to generalize to n dimensions.
                        }
                        if (condition) {                                // Repetition length shall be grid size (or a multiple).
                            data = data.subSampling(0, step, length);
                            if (coordinates.trySetTransform(gridToCRS, srcDim, tgtDim, data)) {
                                return true;
                            } else {
                                nonLinears.add(MathTransforms.interpolate(null, data.doubleValues()));
                                return false;
                            }
                        }
                    }
                }
            }
        }
        nonLinears.add(null);
        return false;
    }
}
