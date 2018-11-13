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
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.ArraysExt;
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
 * @see GridGeometry#getAxes()
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
     */
    public final char abbreviation;

    /**
     * The axis direction, or {@code null} if unknown.
     */
    private final AxisDirection direction;

    /**
     * The indices of the grid dimension associated to this axis. The length of this array is often 1.
     * But if more than one grid dimension is associated to this axis (i.e. if the wrapped netCDF axis
     * is an instance of {@link ucar.nc2.dataset.CoordinateAxis2D}),  then the first value is the grid
     * dimension which seems most closely oriented toward this axis direction. We do that for allowing
     * {@code MetadataReader.addSpatialRepresentationInfo(…)} method to get the most appropriate value
     * for ISO 19115 {@code metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionSize}
     * metadata property.
     */
    public final int[] sourceDimensions;

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
    private final Variable coordinates;

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
    public Axis(final GridGeometry owner, final Variable axis, char abbreviation, final String direction,
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
            axis.warning(owner.getClass(), "getAxes",               // Caller of this constructor.
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
     * Creates ISO 19111 axes from the information stored in given netCDF axes.
     *
     * @param  axes     the axes to convert to ISO data structure.
     * @param  factory  the factory to use for creating the coordinate system axis.
     * @return the ISO axes.
     */
    static CoordinateSystemAxis[] toISO(final List<Axis> axes, final CSFactory factory) throws FactoryException {
        final CoordinateSystemAxis[] iso = new CoordinateSystemAxis[axes.size()];
        for (int i=0; i<iso.length; i++) {
            iso[i] = axes.get(i).toISO(factory);
        }
        return iso;
    }

    /**
     * Creates an ISO 19111 axis from the information stored in this netCDF axis.
     *
     * @param  factory  the factory to use for creating the coordinate system axis.
     * @return the ISO axis.
     */
    private CoordinateSystemAxis toISO(final CSFactory factory) throws FactoryException {
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
         * Axis abbreviation, direction and unit of measurement are mandatory.
         * If any of them is null, creation of CoordinateSystemAxis is likely
         * to fail with an InvalidGeodeticParameterException. But we let the
         * factory to choose, in case users specify their own factory.
         */
        final Unit<?> unit = coordinates.getUnit();
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
}
