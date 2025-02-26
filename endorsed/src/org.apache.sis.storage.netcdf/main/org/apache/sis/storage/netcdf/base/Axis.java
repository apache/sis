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
package org.apache.sis.storage.netcdf.base;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.OptionalLong;
import java.io.IOException;
import ucar.nc2.constants.CDM;      // String constants are copied by the compiler with no UCAR reference left.
import ucar.nc2.constants.CF;       // idem
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.metadata.content.TransferFunctionType;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.referencing.operation.builder.LocalizationGridException;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.netcdf.internal.Resources;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.iso.Types;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;
import org.apache.sis.math.Vector;
import org.apache.sis.pending.jdk.JDK18;


/**
 * Information about a coordinate system axes. In netCDF files, all axes can be related to 1 or more dimensions
 * of the grid domain. Those grid domain dimensions are specified by the {@link #gridDimensionIndices} array.
 * Whether the array length is 1 or 2 depends on whether the wrapped netCDF axis is an instance of
 * {@link ucar.nc2.dataset.CoordinateAxis1D} or {@link ucar.nc2.dataset.CoordinateAxis2D} respectively.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see Grid#getAxes(Decoder)
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
     *
     * <p>A given {@link Grid} should not have two {@code Axis} instances with equal {@code gridDimensionIndices}.
     * When {@code gridDimensionIndices.length} ≥ 2 we may have two {@code Axis} instances with the same indices
     * in their {@code gridDimensionIndices} arrays, but those indices should be in different order.</p>
     *
     * <p>The array length should be equal to {@link Variable#getNumDimensions()}. However, this {@code Axis} class
     * is tolerant to situations where the array length is shorter, which may happen if some grid dimensions where
     * not recognized or cannot be handled for whatever reason that {@link Grid} decided.</p>
     *
     * <p>This field is {@code null} if this {@code Axis} instance is not built for a {@link Grid}.
     * In particular, this field has no meaning for CRS of geometries in a {@link FeatureSet}.
     * See {@link #Axis(Variable)} for a list of methods than cannot be used in such case.</p>
     *
     * @see #getNumDimensions()
     * @see #getMainDirection()
     */
    final int[] gridDimensionIndices;

    /**
     * The number of cell elements along the source grid dimensions, as unsigned integers. The length of this
     * array shall be equal to the {@link #gridDimensionIndices} length. For each element, {@code gridSizes[i]}
     * shall be equal to the number of grid cells in the grid dimension at index {@code gridDimensionIndices[i]}.
     *
     * <p>This array should contain the same information as {@code coordinates.getShape()} but potentially in
     * a different order and with potentially one element (not necessarily the first one) set to a lower value
     * in order to avoid trailing {@link Float#NaN} values.</p>
     *
     * <p>Note that while we defined those values as unsigned for consistency with {@link Variable} dimensions,
     * not all operations in this {@code Axis} class support values greater than the signed integer range.</p>
     *
     * <p>This array is {@code null} if {@link #gridDimensionIndices} is {@code null}, i.e. if this axis is not
     * used for building a {@link Grid}. See {@link #Axis(Variable)} for a list of methods than cannot be used.</p>
     *
     * @see #getMainSize()
     * @see #getSizeProduct(int)
     * @see Variable#getGridDimensions()
     */
    private final int[] gridSizes;

    /**
     * Values of coordinates on this axis for given grid indices. This variables is often one-dimensional,
     * but can also be two-dimensional. Coordinate values should be read with the {@link #read()} method
     * in this {@code Axis} class instead of {@link Variable#read()} for trimming trailing NaN values.
     */
    final Variable coordinates;

    /**
     * Creates an axis for a {@link FeatureSet}. This constructor leaves the {@link #gridDimensionIndices}
     * and {@link #gridSizes} array to {@code null}, which forbids the use of following methods:
     *
     * <ul>
     *   <li>{@link #mainDimensionFirst(Axis[], int)}</li>
     *   <li>{@link #trySetTransform(Matrix, int, int, List)}</li>
     *   <li>{@link #createLocalizationGrid(Axis)}</li>
     *   <li>{@link #getSizeProduct(int)} (private method)</li>
     * </ul>
     *
     * All above methods should be used by {@link Grid} only.
     */
    Axis(final Variable coordinates) {
        this.coordinates = coordinates;
        abbreviation = AxisType.abbreviation(coordinates);
        final AxisDirection dir = direction(coordinates.getUnitsString());
        direction = (dir != null) ? dir : AxisDirections.fromAbbreviation(abbreviation);
        gridDimensionIndices = null;
        gridSizes = null;
    }

    /**
     * Constructs a new axis associated to an arbitrary number of grid dimensions. The given arrays are stored
     * as-in (not cloned) and their content may be modified after construction by {@link Grid#getAxes(Decoder)}.
     *
     * @param  abbreviation          axis abbreviation, also identifying its type. This is a controlled vocabulary.
     * @param  direction             direction of positive values ("up" or "down"), or {@code null} if unknown.
     * @param  gridDimensionIndices  indices of grid dimension associated to this axis, initially in netCDF order.
     * @param  gridSizes             number of cell elements along above grid dimensions, as unsigned integers.
     * @param  dimension             number of valid elements in {@code gridDimensionIndices} and {@code gridSizes}.
     * @param  coordinates           coordinates of the localization grid used by this axis.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    public Axis(final char abbreviation, final String direction, int[] gridDimensionIndices, int[] gridSizes,
                int dimension, final Variable coordinates) throws IOException, DataStoreException
    {
        gridDimensionIndices = ArraysExt.resize(gridDimensionIndices, dimension);
        gridSizes = ArraysExt.resize(gridSizes, dimension);
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
        AxisDirection dir = Types.forCodeName(AxisDirection.class, direction, null);
        AxisDirection check = AxisDirections.fromAbbreviation(abbreviation);
        final boolean isSigned = (dir != null);     // Whether `dir` takes in account the direction of positive values.
        boolean isConsistent = true;
        if (dir == null) {
            dir = check;
        } else if (check != null) {
            isConsistent = AxisDirections.isColinear(dir, check);
        }
        if (isConsistent) {
            check = direction(coordinates.getUnitsString());
            if (dir == null) {
                dir = check;
            } else if (check != null) {
                isConsistent = AxisDirections.isColinear(dir, check);
            }
        }
        if (!isConsistent) {
            coordinates.warning(Grid.class, "getAxes", null,        // Caller of this constructor.
                    Resources.Keys.AmbiguousAxisDirection_4, coordinates.getFilename(), coordinates.getName(), dir, check);
            if (isSigned) {
                if (AxisDirections.isOpposite(dir)) {
                    check = AxisDirections.opposite(check);         // Apply the sign of `dir` on `check`.
                }
                dir = check;
            }
        }
        this.direction            = dir;
        this.abbreviation         = abbreviation;
        this.gridDimensionIndices = gridDimensionIndices;
        this.gridSizes            = gridSizes;
        this.coordinates          = coordinates;
        /*
         * If the variable for localization grid declares a fill value, maybe the last rows are all NaN.
         * We need to trim them from this axis, otherwise it will confuse the grid geometry calculation.
         * Following operation must be done before mainDimensionFirst(…) is invoked, otherwise the order
         * of elements in `gridSizes` would not be okay anymore.
         */
        if (coordinates.getAttributeType(CDM.FILL_VALUE) != null) {
            final int page = getSizeProduct(1);            // Must exclude first dimension from computation.
            final Vector data = coordinates.read();
            int n = data.size();
            while (--n >= 0 && data.isNaN(n)) {}
            final int nr = JDK18.ceilDiv(++n, page);
            assert nr <= gridSizes[0] : nr;
            gridSizes[0] = nr;
            assert getSizeProduct(0) == n : n;
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
                final String direction = unit.substring(s+1);
                if (direction.length() == 1) {
                    switch (Character.toUpperCase(direction.charAt(0))) {
                        case 'E': return AxisDirection.EAST;
                        case 'N': return AxisDirection.NORTH;
                    }
                }
                return Types.forCodeName(AxisDirection.class, direction, null);
            }
        }
        return null;
    }

    /**
     * Swaps the two first source dimensions if needed for making the fastest varying dimension first.
     * This is a helper method for {@link Grid#getAxes(Decoder)} invoked after all axes of a grid have been created.
     * This method needs to know other axes in order to avoid collision.
     *
     * @param  axes   previously created axes.
     * @param  count  number of elements to consider in the {@code axes} array.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     *
     * @see #getMainDirection()
     */
    final void mainDimensionFirst(final Axis[] axes, final int count) throws IOException, DataStoreException {
        final int d0 = gridDimensionIndices[0];
        final int d1 = gridDimensionIndices[1];
        boolean s = false;
        for (int i=0; i<count; i++) {
            final int[] other = axes[i].gridDimensionIndices;
            if (other.length != 0) {
                final int first = other[0];
                if  (first == d1) return;           // Swapping would cause a collision.
                s = (first == d0);
                if (s) break;                       // Need swapping for avoiding collision.
            }
        }
        /*
         * Compute increments along x and y axes at the given location. We will compare averages of those increments in
         * order to determine which axis increases faster. Note that we do not use formula like  (last - first) / count
         * because such formula does not work if the coordinates to not increase or decrease monotonically.
         *
         * ┌────────────────┬────────────────┐
         * │(0)             │(1)             │(2)
         * │                │                │
         * ├────────────────┼────────────────┤
         * │(3)             │(4)             │(5)
         * │                │                │
         * └────────────────┴────────────────┘
         *  (6)              (7)              (8)
         */
        if (!s) {
            final int[] x = sampleIndices(gridSizes[0]);
            final int[] y = sampleIndices(gridSizes[1]);
            double xInc = 0, yInc = 0;
            for (int c=x.length * y.length; --c >= 0;) {
                final int i = x[c % y.length];
                final int j = y[c / y.length];
                final double vo = coordinates.coordinateForAxis(i, j);
                xInc += coordinates.coordinateForAxis(i+1, j) - vo;
                yInc += coordinates.coordinateForAxis(i, j+1) - vo;
            }
            if (!(Math.abs(yInc) > Math.abs(xInc))) {
                return;
            }
        }
        ArraysExt.swap(gridSizes,            0, 1);
        ArraysExt.swap(gridDimensionIndices, 0, 1);
    }

    /**
     * Returns indices of sample values to use in a dimension of the given length.
     * Current implementation returns indices at beginning, middle and end.
     *
     * @param  length  the dimension length.
     * @return indices to use for subsampling in a dimension of the given length.
     */
    private static int[] sampleIndices(final int length) {
        final int up;
        if (length >= 0) {
            if (length <= 1) return ArraysExt.EMPTY_INT;
            if (length <= 4) return ArraysExt.range(0, length - 1);
            up = length - 2;
        } else {
            up = Integer.MAX_VALUE - 1;                 // For unsigned integers, < 0 means overflow.
        }
        return new int[] {0, length >>> 1, up};
    }

    /**
     * Returns the fastest varying dimension of this "two-dimensional" axis.
     *
     * <ul>
     *   <li>If this method returns 0, then axis coordinates vary mostly in columns.
     *       Or to be more accurate, in first grid dimension (in netCDF order) associated to this axis.</li>
     *   <li>If this method returns 1, then axis coordinates vary mostly in rows.
     *       Or to be more accurate, in second grid dimension (in netCDF order) associated to this axis.</li>
     * </ul>
     *
     * If a grid has <var>n</var> dimensions but we copy in an array of length 2 the dimensions used by this
     * {@code Axis} instance, while preserving the dimension order as declared in the netCDF file, then the
     * value returned by this method is the index of the "main" dimension in this array of length 2.
     *
     * @return 0 or 1, depending on whether coordinates vary mostly on columns or on rows respectively.
     *
     * @see #getMainSize()
     */
    private int getMainDirection() {
        return (getNumDimensions() < 2 || gridDimensionIndices[0] <= gridDimensionIndices[1]) ? 0 : 1;
    }

    /**
     * Returns the number of dimension of the localization grid used by this axis.
     * This method returns 2 if this axis is backed by a localization grid having 2 or more dimensions.
     * In the netCDF UCAR library, such axes are handled by a {@link ucar.nc2.dataset.CoordinateAxis2D}.
     *
     * @return number of dimension of the localization grid used by this axis.
     */
    final int getNumDimensions() {
        return (gridDimensionIndices != null) ? gridDimensionIndices.length : coordinates.getNumDimensions();
    }

    /**
     * Returns the product of all {@link #gridSizes} values starting at the given index.
     * The product of all sizes given by {@code getSizeProduct(0)} shall be the length of
     * the vector returned by {@link #read()}.
     *
     * @param  i  index of the first size to include in the product.
     * @return the product of all {@link #gridSizes} values starting at the given index.
     * @throws ArithmeticException if the product cannot be represented as a signed 32 bits integer.
     */
    private int getSizeProduct(int i) {
        int length = 1;
        while (i < gridSizes.length) {
            length = Math.multiplyExact(length, getSize(i++));
        }
        return length;
    }

    /**
     * Returns the {@link #gridSizes} value at the given index, making sure it is representable as a
     * signed integer value. This method is invoked by operations not designed for unsigned integers.
     *
     * @param  i  index of the desired dimension, in the same order as {@link #gridDimensionIndices}.
     * @throws ArithmeticException if the size cannot be represented as a signed 32 bits integer.
     */
    private int getSize(final int i) {
        final int n = gridSizes[i];
        if (n >= 0) return n;
        throw new ArithmeticException(coordinates.errors().getString(Errors.Keys.IntegerOverflow_1, Integer.SIZE));
    }

    /**
     * Returns the number of cells in the first dimension of the localization grid used by this axis.
     * If the localization grid has more than one dimension ({@link #getNumDimensions()} {@literal > 1}),
     * then all additional dimensions are ignored. The first dimension should be the main one.
     *
     * @return number of cells in the first (main) dimension of the localization grid.
     */
    public final OptionalLong getMainSize() {
        final int m = getMainDirection();
        if (gridSizes != null && gridSizes.length > m) {
            return OptionalLong.of(Integer.toUnsignedLong(gridSizes[m]));
        }
        final List<Dimension> dimensions = coordinates.getGridDimensions();
        if (dimensions.size() > m) {
            return OptionalLong.of(dimensions.get(m).length());
        }
        return OptionalLong.empty();
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
     * This is used for testing if a predefined axis can be used instead of invoking
     * {@link #toISO(CSFactory, int, boolean)}.
     */
    final boolean isSameUnitAndDirection(final CoordinateSystemAxis axis) {
        if (!axis.getDirection().equals(direction)) {
            return false;
        }
        final Unit<?> unit = getUnit();                         // Null to be interpreted as system unit.
        return (unit == null) || axis.getUnit().equals(unit);
    }

    /**
     * Returns {@code true} if this axis is likely to have a "wraparound" range. The main case is the longitude
     * axis with a [-180 … +180]° range or a [0 … 360]° range, where the next value after +180° may be -180°.
     */
    final boolean isWraparound() {
        if (abbreviation == 0) {
            return AxisDirections.absolute(direction) == AxisDirection.EAST && Units.isAngular(getUnit());
        } else {
            return abbreviation == 'λ';
        }
    }

    /**
     * Returns the range of this wraparound axis, or {@link Double#NaN} if this axis is not a wraparound axis.
     */
    @SuppressWarnings("fallthrough")
    private double wraparoundRange() {
        if (isWraparound()) {
            double period = Longitude.MAX_VALUE - Longitude.MIN_VALUE;
            final Unit<?> unit = getUnit();
            if (unit != null) try {
                period = unit.getConverterToAny(Units.DEGREE).convert(period);
            } catch (IncommensurableException e) {
                warning(e, Errors.Keys.InconsistentUnitsForCS_1, unit);
                return Double.NaN;
            }
            return period;
        }
        return Double.NaN;
    }

    /**
     * Returns {@code true} if coordinates in this axis seem to map cell corner instead of cell center.
     * A {@code false} value does not necessarily means that the axis maps cell center; it can be unknown.
     * This method assumes a geographic CRS.
     *
     * <p>From CF-Convention: <q>If bounds are not provided, an application might reasonably assume the
     * grid points to be at the centers of the cells, but we do not require that in this standard.</q>
     * We nevertheless tries to guess by checking if the "cell center" convention would result in coordinates
     * outside the range of longitude or latitude values.</p>
     */
    final boolean isCellCorner() throws IOException, DataStoreException {
        double min;
        boolean wraparound;
        switch (abbreviation) {
            case 'λ': min = Longitude.MIN_VALUE; wraparound = true;  break;
            case 'φ': min =  Latitude.MIN_VALUE; wraparound = false; break;
            default: return false;
        }
        final Vector data = read();
        final int size = data.size();
        if (size != 0) {
            Unit<?> unit = getUnit();
            if (unit == null) {
                unit = Units.DEGREE;
            }
            try {
                final UnitConverter uc = unit.getConverterToAny(Units.DEGREE);
                if (wraparound && uc.convert(data.doubleValue(size - 1)) > Longitude.MAX_VALUE) {
                    min = 0;            // Replace [-180 … +180]° longitude range by [0 … 360]°.
                }
                return uc.convert(data.doubleValue(0)) == min;
            } catch (IncommensurableException e) {
                warning(e, Errors.Keys.InconsistentUnitsForCS_1, unit);
            }
        }
        return false;
    }

    /**
     * Creates an ISO 19111 axis from the information stored in this netCDF axis.
     *
     * @param  factory  the factory to use for creating the coordinate system axis.
     * @param  order    0 if creating the first axis, 1 if creating the second axis, <i>etc</i>.
     * @param  grid     {@code true} if building a CRS for a grid, or {@code false} for features.
     * @return the ISO axis.
     */
    @SuppressWarnings("fallthrough")
    final CoordinateSystemAxis toISO(final CSFactory factory, final int order, final boolean grid)
            throws DataStoreException, FactoryException, IOException
    {
        /*
         * The axis name is stored without namespace, because the variable name in a netCDF file can be anything;
         * this is not controlled vocabulary. However, the standard name, if any, is stored with "NetCDF" namespace
         * because this is controlled vocabulary.
         */
        final String name = getName();
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(CoordinateSystemAxis.NAME_KEY, name);                        // Intentionally no namespace.
        final List<GenericName> aliases = new ArrayList<>(2);
        final String standardName = coordinates.getAttributeAsString(CF.STANDARD_NAME);
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
        final String alt = coordinates.getAttributeAsString(CDM.LONG_NAME);
        if (alt != null && !similar(alt, name)) {
            properties.put(org.apache.sis.referencing.ImmutableIdentifier.DESCRIPTION_KEY, alt);   // Description associated to primary name.
            if (!similar(alt, standardName)) {
                aliases.add(new NamedIdentifier(null, alt));                        // Additional alias.
            }
        }
        if (!aliases.isEmpty()) {
            properties.put(CoordinateSystemAxis.ALIAS_KEY, aliases.toArray(GenericName[]::new));
        }
        /*
         * Axis abbreviation, direction and unit of measurement are mandatory. If any of them is null,
         * creation of CoordinateSystemAxis is likely to fail with an InvalidGeodeticParameterException.
         * We provide default values for the most well-identified axes.
         *
         * The default values are SI base units except degrees, which is the usual angular units for netCDF files.
         * Providing default units is a little bit dangerous, but we cannot create CRS otherwise. Note that wrong
         * defaults become harmless if the CRS is overwritten by GridMapping attributes in Variable.getGridGeometry().
         */
        Unit<?> unit = getUnit();
        if (unit == null) {
            switch (abbreviation) {
                case 'λ': case 'φ':                                 // Geodetic longitude and latitude.
                case 'θ': case 'Ω': unit = Units.DEGREE; break;     // Spherical longitude and latitude.
                case 'r': case 'D':                                 // Depth and radius.
                case 'H': case 'h':                                 // Gravity-related and ellipsoidal height.
                case 'E': case 'N': unit = Units.METRE;  break;     // Projected easting and northing.
                case 't':           unit = Units.SECOND; break;     // Time.
                case 'x': case 'y': {
                    if (grid) {
                        final Vector values = read();
                        final Number increment = values.increment(0);
                        if (increment != null && increment.doubleValue() == 1) {
                            // Do not test values.doubleValue(0) since different conventions exit (0-based, 1-based, etc).
                            unit = Units.PIXEL;
                            break;
                        }
                    }
                    // Else fallthrough.
                }
                default: unit = Units.UNITY; break;
            }
        }
        AxisDirection dir = direction;
        if (dir == null) {
            if (Units.isTemporal(unit)) {
                dir = AxisDirection.FUTURE;
            } else if (Units.isPressure(unit)) {
                dir = AxisDirection.UP;
            } else switch (order) {
                case 0:  dir = AxisDirection.COLUMN_POSITIVE; break;
                case 1:  dir = AxisDirection.ROW_POSITIVE;    break;
                default: dir = AxisDirections.UNSPECIFIED;    break;
            }
        }
        final String abbr;
        if (abbreviation != 0) {
            abbr = Character.toString(abbreviation).intern();
        } else if (dir != null && unit != null) {
            abbr = AxisDirections.suggestAbbreviation(name, dir, unit);
        } else {
            abbr = "A" + (order + 1);
        }
        return factory.createCoordinateSystemAxis(properties, abbr, dir, unit);
    }

    /**
     * Sets the scale and offset coefficients in the given "grid to CRS" transform if possible.
     * Source and target dimensions used by this method are in "natural" order (reverse of netCDF order).
     * Setting the coefficient is possible only if values in this variable are regular,
     * i.e. the difference between two consecutive values is constant.
     *
     * <p>If this method returns {@code true}, then the {@code nonLinears} list is left unchanged.
     * If this method returns {@code false}, then a non-linear transform or {@code null} has been
     * added to the {@code nonLinears} list. A {@code null} element means that the caller will need
     * to construct himself a transform backed by a localization grid.</p>
     *
     * @param  gridToCRS   the matrix in which to set scale and offset coefficient.
     * @param  lastSrcDim  number of source dimensions (grid dimensions) - 1. Used for conversion from netCDF to "natural" order.
     * @param  tgtDim      the target dimension, which is a dimension of the CRS. Identifies the matrix row of scale factor to set.
     * @param  nonLinears  where to add a non-linear transform if we cannot compute a linear one. {@code null} may be added.
     * @return whether this method successfully set the scale and offset coefficients.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     */
    final boolean trySetTransform(final Matrix gridToCRS, final int lastSrcDim, final int tgtDim,
            final List<MathTransform> nonLinears) throws IOException, DataStoreException
    {
        switch (getNumDimensions()) {
            /*
             * Variable is a scalar, which is represented by an array of length 1.
             * There is no source dimension, only a target dimension fixed to a constant value.
             * Consequently, the scale factor does not exist in the matrix, only the translation.
             */
            case 0: {
                final Vector data = read();
                if (!data.isEmpty()) {
                    gridToCRS.setElement(tgtDim, gridToCRS.getNumCol() - 1, data.doubleValue(0));
                }
                return true;
            }
            /*
             * Normal case where the axis has only one dimension.
             */
            case 1: {
                final Vector data = read();
                final int srcDim = lastSrcDim - gridDimensionIndices[0];    // Convert from netCDF to "natural" order.
                if (coordinates.trySetTransform(gridToCRS, srcDim, tgtDim, data)) {
                    return true;
                } else {
                    nonLinears.add(MathTransforms.interpolate(null, data.doubleValues()));
                    return false;
                }
            }
            /*
             * In netCDF files, axes are sometimes associated to two-dimensional localization grids.
             * If this is the case, then the following block checks if we can reduce those grids to
             * one-dimensional vector. For example, the following localisation grids:
             *
             *    10 10 10 10                  10 12 15 20
             *    12 12 12 12        or        10 12 15 20
             *    15 15 15 15                  10 12 15 20
             *    20 20 20 20                  10 12 15 20
             *
             * can be reduced to a one-dimensional {10 12 15 20} vector (orientation matter however).
             * We detect those cases by the call to data.repetitions(gridSizes). In above examples,
             * we would get {4} for the case illustrated on left side, and {1,4} for the right side.
             * The array length tells us if the variation is horizontal or vertical, and the product
             * of all numbers gives us the variation width. That width must match the grid width,
             * which is the size of the last dimension in netCDF order.
             *
             * Note: following block is currently restricted to the two-dimensional case, but it could
             * be generalized to n-dimensional case if we resolve the default case in the switch statement.
             */
            case 2: {
                Vector data = read();
                final int[] repetitions = data.repetitions(gridSizes);      // Detects repetitions as illustrated above.
                long repetitionLength = 1;
                for (int r : repetitions) {
                    repetitionLength = Math.multiplyExact(repetitionLength, r);
                }
                final int ri = getMainDirection();
                for (int i=0; i<=1; i++) {
                    final int width  = getSize(ri ^ i    );
                    final int height = getSize(ri ^ i ^ 1);
                    if (repetitionLength % width == 0) {        // Repetition length shall be grid width (or a divisor).
                        final int length, step;
                        if (repetitions.length >= 2) {
                            length = height;
                            step   = width;
                        } else {
                            length = width;
                            step   = 1;
                        }
                        data = data.subSampling(0, step, length);
                        if (coordinates.trySetTransform(gridToCRS, lastSrcDim - i, tgtDim, data)) {
                            return true;
                        } else {
                            nonLinears.add(MathTransforms.interpolate(null, data.doubleValues()));
                            return false;
                        }
                    }
                }
            }
            /*
             * Localization grid of 3 dimensions or more are theoretically possible, but uncommon.
             * Such grids are not yet supported. Note that we can read three or more dimensional data;
             * it is only the localization grid which is limited to one or two dimensions.
             */
        }
        nonLinears.add(null);
        return false;
    }

    /**
     * Tries to create a two-dimensional localization grid using this axis and the given axis.
     * This method is invoked as a fallback when {@link #trySetTransform(Matrix, int, int, List)}
     * could not set coefficients in the matrix of an affine transform.
     *
     * <p>The <em>source</em> dimensions (pixel indices) are insensitive to variables order: invoking {@code A.f(B)}
     * or {@code B.f(A)} are equivalent. However, the <em>target</em> dimensions ("real world" coordinates) depend on
     * the order: values of this variable will be stored in the first target dimension of the localization grid, and
     * values of the other variable will be in the second target dimension.</p>
     *
     * @param  other  the other axis to use for creating a localization grid.
     * @return the localization grid, or {@code null} if none can be built.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     * @throws TransformException if an unexpected error occurred during application of a linearizer.
     */
    final GridCacheValue createLocalizationGrid(final Axis other)
            throws IOException, FactoryException, TransformException, DataStoreException
    {
        if (getNumDimensions() != 2 || other.getNumDimensions() != 2) {
            return null;
        }
        final int xd =  this.gridDimensionIndices[0];
        final int yd =  this.gridDimensionIndices[1];
        final int xo = other.gridDimensionIndices[0];
        final int yo = other.gridDimensionIndices[1];
        if ((xo != xd | yo != yd) & (xo != yd | yo != xd)) {
            return null;
        }
        /*
         * Found two axes for the same set of dimensions, which implies that they have the same
         * shape (width and height) unless the two axes ignored a different number of NaN values.
         * Negative width and height means that their actual values overflow the `int` capacity,
         * which we cannot process here.
         */
        final int ri = (xd <= yd) ? 0 : 1;          // Take in account that mainDimensionFirst(…) may have reordered values.
        final int ro = (xo <= yo) ? 0 : 1;
        final int width  = getSize(ri ^ 1);         // Fastest varying is right-most dimension (when in netCDF order).
        final int height = getSize(ri    );         // Slowest varying is left-most dimension (when in netCDF order).
        if (other.gridSizes[ro ^ 1] != width ||
            other.gridSizes[ro    ] != height)
        {
            warning(null, Errors.Keys.MismatchedGridGeometry_2, getName(), other.getName());
            return null;
        }
        /*
         * First, verify if the localization grid has already been created previously. It happens if the netCDF file
         * contains data with different number of dimensions. For example, a file may have a variable with (longitude,
         * latitude) and another variable with (longitude, latitude, depth) dimensions, with both variables using the
         * same localization grid for the (longitude, latitude) part.
         */
        final Decoder decoder = coordinates.decoder;
        final GridCacheKey keyLocal = new GridCacheKey(width, height, this, other);
        GridCacheValue tr = keyLocal.cached(decoder);
        if (tr != null) {
            return tr;
        }
        /*
         * If there is no localization grid in the cache locale to the netCDF decoder, try again in the global cache.
         * This check is more expensive since it computes MD5 sum of all coordinate values.
         */
        final long time = System.nanoTime();
        final Vector vx =  this.read();
        final Vector vy = other.read();
        final Set<Linearizer> linearizers = decoder.convention().linearizers(decoder);
        final GridCacheKey.Global keyGlobal = new GridCacheKey.Global(keyLocal, vx, vy, linearizers);
        final Cache.Handler<GridCacheValue> handler = keyGlobal.lock();
        try {
            tr = handler.peek();
            if (tr == null) {
                final LocalizationGridBuilder grid = new LocalizationGridBuilder(width, height);
                grid.setControlPoints(vx, vy);
                /*
                 * At this point we finished to set values in the localization grid, but did not computed the transform yet.
                 * Before to use the grid for calculation, we need to repair discontinuities sometimes found with longitudes.
                 * If the grid crosses the anti-meridian, some values may suddenly jump from +180° to -180° or conversely.
                 * Even when not crossing the anti-meridian, we still observe apparently random 360° jumps in some files,
                 * especially close to poles. The methods invoked below try to make the longitude grid more continuous.
                 * The "ri" or "ro" argument specifies which dimension varies slowest, i.e. which dimension have values
                 * that do not change much when increasing longitudes. This is usually 1 (the rows).
                 */
                double period;
                if (!Double.isNaN(period = wraparoundRange())) {
                    grid.resolveWraparoundAxis(0, ri, period);
                }
                if (!Double.isNaN(period = other.wraparoundRange())) {
                    grid.resolveWraparoundAxis(1, ro, period);
                }
                /*
                 * Forward coordinate conversions are straightforward interpolations in the localization grid.
                 * But inverse conversions are more difficult to perform as they require iterations. They will
                 * converge better if the grid is close to linear.
                 */
                final MathTransformFactory factory = decoder.getMathTransformFactory();
                if (!linearizers.isEmpty()) {
                    // Current version does not need the factory, but future version may use it.
                    Linearizer.setCandidatesOnGrid(new Axis[] {this, other}, linearizers, grid);
                }
                /*
                 * There is usually a one-to-one relationship between localization grid cells and image pixels.
                 * Consequently, an accuracy set to a fraction of cell should be enough.
                 *
                 * TODO: take in account the case where GridAdjustment.dataToGridIndices() returns a value
                 * smaller than 1. For now we set the desired precision to a value 10 times smaller in order
                 * to take in account the case where dataToGridIndices() returns 0.1.
                 */
                grid.setDesiredPrecision(0.001);
                tr = new GridCacheValue(linearizers, grid, factory);
                tr = keyLocal.cache(decoder, tr);
            }
        } catch (LocalizationGridException ex) {
            /*
             * Complete the exception with a possible failure cause before to propagate the exception.
             * Example: "The grid spans more than 180° of longitude", which may cause transform errors.
             * The possible causes are known only by the linearizer, which is why we could not set it
             * at `LocalizationGridException` construction time.
             */
            for (final Linearizer linearizer : linearizers) {
                final CharSequence reason = linearizer.getPotentialCause();
                if (reason != null) {
                    ex.setPotentialCause(reason);
                    break;          // Take the cause of the linearizer that had the highest priority.
                }
            }
            throw ex;
        } finally {
            handler.putAndUnlock(tr);
        }
        decoder.performance(Grid.class, "getGridGeometry", Resources.Keys.ComputeLocalizationGrid_2, time);
        return tr;
    }

    /**
     * Reports a non-fatal error that occurred while constructing the grid geometry. This method is invoked
     * by methods that are themselves invoked (indirectly) by {@link Grid#getGridGeometry(Decoder)}, which
     * is invoked by {@link Variable#getGridGeometry()}. We pretend that the warning come from the latter
     * since it is a bit closer to a public API.
     *
     * @param  exception  the exception that occurred, or {@code null} if none.
     * @param  key        one or {@link Errors.Keys} constants.
     * @param  arguments  values to be formatted in the {@link java.text.MessageFormat} pattern.
     *
     * @see Variable#warning(Class, String, short, Object...)
     */
    private void warning(final Exception exception, final short key, final Object... arguments) {
        coordinates.error(Variable.class, "getGridGeometry", exception, key, arguments);
    }

    /**
     * Returns the coordinates in the localization grid, excluding some trailing NaN values if any.
     * This method typically returns a cached vector if the coordinates have already been read.
     *
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     */
    final Vector read() throws IOException, DataStoreException {
        final TransferFunction tr = coordinates.getTransferFunction();
        if (tr.getType() == TransferFunctionType.LINEAR) {
            Vector data = coordinates.read();
            if (gridSizes != null) {
                data = data.subList(0, getSizeProduct(0));              // Trim trailing NaN values.
            }
            data = data.transform(tr.getScale(), tr.getOffset());       // Apply scale and offset attributes, if any.
            return data;
        } else {
            throw new DataStoreException(coordinates.decoder.resources()
                    .getString(Resources.Keys.CanNotUseAxis_1, getName()));
        }
    }

    /**
     * Compares this axis with the given object for equality.
     *
     * @param  other  the other object to compare with this axis.
     * @return whether the other object describes the same axis as this object.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof Axis) {
            final Axis that = (Axis) other;
            return that.abbreviation == abbreviation && that.direction == direction
                    && Arrays.equals(that.gridDimensionIndices, gridDimensionIndices)
                    && Arrays.equals(that.gridSizes, gridSizes)
                    && coordinates.equals(that.coordinates);
        }
        return false;
    }

    /**
     * Returns a hash code value for this axis. This method uses only properties that are quick to compute.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return abbreviation + Arrays.hashCode(gridDimensionIndices) + Arrays.hashCode(gridSizes);
    }
}
